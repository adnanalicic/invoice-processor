package com.invoiceprocessor.application.usecase;

import com.invoiceprocessor.application.port.out.DocumentRepository;
import com.invoiceprocessor.application.port.out.EmailFetcher;
import com.invoiceprocessor.application.port.out.InvoiceExtractionRepository;
import com.invoiceprocessor.application.port.out.LlmInvoiceExtractor;
import com.invoiceprocessor.application.port.out.StackRepository;
import com.invoiceprocessor.application.port.out.StorageService;
import com.invoiceprocessor.domain.entity.Document;
import com.invoiceprocessor.domain.entity.DocumentType;
import com.invoiceprocessor.domain.entity.ExtractionStatus;
import com.invoiceprocessor.domain.entity.Stack;
import com.invoiceprocessor.domain.service.InvoiceProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ImportEmailsUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ImportEmailsUseCase.class);

    private final EmailFetcher emailFetcher;
    private final StackRepository stackRepository;
    private final DocumentRepository documentRepository;
    private final InvoiceExtractionRepository extractionRepository;
    private final LlmInvoiceExtractor llmExtractor;
    private final InvoiceProcessingService processingService;
    private final StorageService storageService;
    private final String emailFolder;

    public ImportEmailsUseCase(
            EmailFetcher emailFetcher,
            StackRepository stackRepository,
            DocumentRepository documentRepository,
            InvoiceExtractionRepository extractionRepository,
            LlmInvoiceExtractor llmExtractor,
            InvoiceProcessingService processingService,
            StorageService storageService,
            @Value("${email.import.folder:INBOX}") String emailFolder) {
        this.emailFetcher = emailFetcher;
        this.stackRepository = stackRepository;
        this.documentRepository = documentRepository;
        this.extractionRepository = extractionRepository;
        this.llmExtractor = llmExtractor;
        this.processingService = processingService;
        this.storageService = storageService;
        this.emailFolder = emailFolder;
    }

    @Transactional
    public ImportEmailsResponse execute() {
        logger.info("Starting email import from folder: {}", emailFolder);
        List<EmailFetcher.EmailMessage> emails;
        try {
            emails = emailFetcher.fetchUnreadEmails(emailFolder);
            logger.info("Found {} unread emails", emails.size());
        } catch (Exception e) {
            logger.error("Failed to fetch emails from folder {}: {}", emailFolder, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch emails: " + e.getMessage(), e);
        }
        
        List<UUID> createdStackIds = new ArrayList<>();
        int totalDocuments = 0;
        int errors = 0;

        for (EmailFetcher.EmailMessage email : emails) {
            try {
                logger.debug("Processing email: {} - {}", email.messageId(), email.subject());
                UUID stackId = processEmail(email);
                createdStackIds.add(stackId);
                
                // Count documents for this stack
                List<Document> documents = documentRepository.findByStackId(stackId);
                totalDocuments += documents.size();
                logger.info("Created stack {} with {} documents", stackId, documents.size());
                
                // Mark email as read after successful processing
                emailFetcher.markAsRead(email.messageId(), emailFolder);
            } catch (Exception e) {
                errors++;
                logger.error("Error processing email {}: {}", email.messageId(), e.getMessage(), e);
            }
        }

        logger.info("Email import completed: {} emails found, {} stacks created, {} documents created, {} errors", 
            emails.size(), createdStackIds.size(), totalDocuments, errors);

        return new ImportEmailsResponse(
            emails.size(),
            createdStackIds.size(),
            totalDocuments,
            errors
        );
    }

    private UUID processEmail(EmailFetcher.EmailMessage email) {
        // Create stack
        Stack stack = new Stack(email.from(), email.to(), email.subject());
        stack = stackRepository.save(stack);

        List<Document> documents = new ArrayList<>();

        // Create document for email body if present
        if (email.body() != null && !email.body().trim().isEmpty()) {
            try {
                String s3Key = generateS3Key(stack.getId(), "email-body.txt");
                String contentLocation = storageService.uploadFile(
                    s3Key,
                    new ByteArrayInputStream(email.body().getBytes(StandardCharsets.UTF_8)),
                    "text/plain"
                );

                Document emailBodyDoc = new Document(
                    stack.getId(),
                    DocumentType.EMAIL_BODY,
                    "email-body.txt",
                    contentLocation
                );
                documents.add(emailBodyDoc);
            } catch (Exception e) {
                logger.error("Error storing email body for stack {}: {}", stack.getId(), e.getMessage(), e);
            }
        }

        // Process attachments
        if (email.attachments() != null) {
            for (EmailFetcher.EmailAttachment attachment : email.attachments()) {
                try {
                    // Determine document type from content type
                    DocumentType docType = determineDocumentType(attachment.contentType(), attachment.filename());
                    
                    // Upload attachment to S3
                    String s3Key = generateS3Key(stack.getId(), attachment.filename());
                    String contentLocation = storageService.uploadFile(
                        s3Key,
                        attachment.content(),
                        attachment.contentType()
                    );

                    // Create document
                    Document doc = new Document(
                        stack.getId(),
                        docType,
                        attachment.filename(),
                        contentLocation
                    );
                    documents.add(doc);
                } catch (Exception e) {
                    logger.error("Error processing attachment {}: {}", attachment.filename(), e.getMessage(), e);
                }
            }
        }

        // Save and process documents
        for (Document document : documents) {
            documentRepository.save(document);
            processDocument(document);
        }

        // Update stack status
        stack.setDocuments(documents);
        stack.updateStatusFromDocuments();
        stackRepository.save(stack);

        return stack.getId();
    }

    private DocumentType determineDocumentType(String contentType, String filename) {
        if (contentType != null) {
            if (contentType.contains("pdf")) {
                return DocumentType.PDF_ATTACHMENT;
            } else if (contentType.startsWith("image/")) {
                return DocumentType.IMAGE_ATTACHMENT;
            }
        }
        
        if (filename != null) {
            String lowerFilename = filename.toLowerCase();
            if (lowerFilename.endsWith(".pdf")) {
                return DocumentType.PDF_ATTACHMENT;
            } else if (lowerFilename.matches(".*\\.(jpg|jpeg|png|gif|bmp)$")) {
                return DocumentType.IMAGE_ATTACHMENT;
            }
        }
        
        return DocumentType.OTHER_ATTACHMENT;
    }

    private String generateS3Key(UUID stackId, String filename) {
        return "stacks/" + stackId + "/" + System.currentTimeMillis() + "-" + filename;
    }

    private void processDocument(Document document) {
        document.setExtractionStatus(ExtractionStatus.EXTRACTING);
        documentRepository.save(document);

        InvoiceProcessingService.LlmExtractionResult llmResult = llmExtractor.extract(document);
        InvoiceProcessingService.ProcessingResult result = processingService.processDocumentWithLlmResult(document, llmResult);

        documentRepository.save(document);

        if (result.success() && result.extraction() != null) {
            extractionRepository.deleteByDocumentId(document.getId());
            extractionRepository.save(result.extraction());
        } else {
            extractionRepository.deleteByDocumentId(document.getId());
        }
    }

    public record ImportEmailsResponse(
        int emailsFound,
        int stacksCreated,
        int documentsCreated,
        int errors
    ) {}
}

