package com.invoiceprocessor.application.usecase;

import com.invoiceprocessor.application.port.out.DocumentRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CreateManualStackUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CreateManualStackUseCase.class);

    private final StackRepository stackRepository;
    private final DocumentRepository documentRepository;
    private final InvoiceExtractionRepository extractionRepository;
    private final LlmInvoiceExtractor llmExtractor;
    private final InvoiceProcessingService processingService;
    private final StorageService storageService;

    public CreateManualStackUseCase(
            StackRepository stackRepository,
            DocumentRepository documentRepository,
            InvoiceExtractionRepository extractionRepository,
            LlmInvoiceExtractor llmExtractor,
            InvoiceProcessingService processingService,
            StorageService storageService) {
        this.stackRepository = stackRepository;
        this.documentRepository = documentRepository;
        this.extractionRepository = extractionRepository;
        this.llmExtractor = llmExtractor;
        this.processingService = processingService;
        this.storageService = storageService;
    }

    @Transactional
    public UUID execute(CreateManualStackRequest request) {
        Stack stack = new Stack(request.from(), request.to(), request.subject());
        stack = stackRepository.save(stack);

        List<Document> documents = new ArrayList<>();

        if (request.body() != null && !request.body().trim().isEmpty()) {
            try {
                String key = generateS3Key(stack.getId(), "manual-email-body.txt");
                String contentLocation = storageService.uploadFile(
                    key,
                    new ByteArrayInputStream(request.body().getBytes(StandardCharsets.UTF_8)),
                    "text/plain"
                );
                Document emailBody = new Document(
                    stack.getId(),
                    DocumentType.EMAIL_BODY,
                    "email-body.txt",
                    contentLocation
                );
                documents.add(emailBody);
            } catch (Exception e) {
                logger.error("Failed to store manual email body for stack {}: {}", stack.getId(), e.getMessage(), e);
            }
        }

        if (request.attachments() != null) {
            for (AttachmentContent attachment : request.attachments()) {
                try {
                    String filename = attachment.filename() != null ? attachment.filename() : "attachment";
                    DocumentType docType = determineDocumentType(attachment.contentType(), filename);
                    String key = generateS3Key(stack.getId(), filename);
                    String contentType = attachment.contentType() != null
                        ? attachment.contentType()
                        : "application/octet-stream";
                    String contentLocation = storageService.uploadFile(
                        key,
                        new ByteArrayInputStream(attachment.content()),
                        contentType
                    );

                    Document document = new Document(
                        stack.getId(),
                        docType,
                        filename,
                        contentLocation
                    );
                    documents.add(document);
                } catch (Exception e) {
                    logger.error("Failed to process manual attachment {}: {}", attachment.filename(), e.getMessage(), e);
                }
            }
        }

        for (Document document : documents) {
            documentRepository.save(document);
            processDocument(document);
        }

        stack.setDocuments(documents);
        stack.updateStatusFromDocuments();
        stackRepository.save(stack);

        return stack.getId();
    }

    private void processDocument(Document document) {
        document.setExtractionStatus(ExtractionStatus.EXTRACTING);
        documentRepository.save(document);

        InvoiceProcessingService.LlmExtractionResult llmResult = llmExtractor.extract(document);
        InvoiceProcessingService.ProcessingResult result =
            processingService.processDocumentWithLlmResult(document, llmResult);

        documentRepository.save(document);

        extractionRepository.deleteByDocumentId(document.getId());
        if (result.success() && result.extraction() != null) {
            extractionRepository.save(result.extraction());
        }
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
            String lower = filename.toLowerCase();
            if (lower.endsWith(".pdf")) {
                return DocumentType.PDF_ATTACHMENT;
            } else if (lower.matches(".*\\.(jpg|jpeg|png|gif|bmp)$")) {
                return DocumentType.IMAGE_ATTACHMENT;
            }
        }

        return DocumentType.OTHER_ATTACHMENT;
    }

    private String generateS3Key(UUID stackId, String filename) {
        String safeName = filename == null ? "attachment" : filename.replaceAll("\\s+", "_");
        return "manual-stacks/" + stackId + "/" + System.currentTimeMillis() + "-" + safeName;
    }

    public record CreateManualStackRequest(
        String from,
        String to,
        String subject,
        String body,
        List<AttachmentContent> attachments
    ) {}

    public record AttachmentContent(
        String filename,
        String contentType,
        byte[] content
    ) {}
}
