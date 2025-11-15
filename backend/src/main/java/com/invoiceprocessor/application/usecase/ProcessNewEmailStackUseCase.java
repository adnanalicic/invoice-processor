package com.invoiceprocessor.application.usecase;

import com.invoiceprocessor.application.port.out.DocumentRepository;
import com.invoiceprocessor.application.port.out.InvoiceExtractionRepository;
import com.invoiceprocessor.application.port.out.LlmInvoiceExtractor;
import com.invoiceprocessor.application.port.out.StackRepository;
import com.invoiceprocessor.domain.entity.Document;
import com.invoiceprocessor.domain.entity.DocumentType;
import com.invoiceprocessor.domain.entity.ExtractionStatus;
import com.invoiceprocessor.domain.entity.Stack;
import com.invoiceprocessor.domain.service.InvoiceProcessingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ProcessNewEmailStackUseCase {

    private final StackRepository stackRepository;
    private final DocumentRepository documentRepository;
    private final InvoiceExtractionRepository extractionRepository;
    private final LlmInvoiceExtractor llmExtractor;
    private final InvoiceProcessingService processingService;

    public ProcessNewEmailStackUseCase(
            StackRepository stackRepository,
            DocumentRepository documentRepository,
            InvoiceExtractionRepository extractionRepository,
            LlmInvoiceExtractor llmExtractor,
            InvoiceProcessingService processingService) {
        this.stackRepository = stackRepository;
        this.documentRepository = documentRepository;
        this.extractionRepository = extractionRepository;
        this.llmExtractor = llmExtractor;
        this.processingService = processingService;
    }

    @Transactional
    public UUID execute(SimulateEmailRequest request) {
        Stack stack = new Stack(request.from(), request.to(), request.subject());
        stack = stackRepository.save(stack);

        List<Document> documents = new ArrayList<>();

        if (request.body() != null && !request.body().trim().isEmpty()) {
            Document emailBodyDoc = new Document(
                stack.getId(),
                DocumentType.EMAIL_BODY,
                null,
                "email-body-" + stack.getId()
            );
            documents.add(emailBodyDoc);
        }

        if (request.attachments() != null) {
            for (AttachmentInfo attachment : request.attachments()) {
                Document doc = new Document(
                    stack.getId(),
                    attachment.type(),
                    attachment.filename(),
                    attachment.contentReference()
                );
                documents.add(doc);
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
        InvoiceProcessingService.ProcessingResult result = processingService.processDocumentWithLlmResult(document, llmResult);

        documentRepository.save(document);

        if (result.success() && result.extraction() != null) {
            extractionRepository.deleteByDocumentId(document.getId());
            extractionRepository.save(result.extraction());
        } else {
            extractionRepository.deleteByDocumentId(document.getId());
        }
    }

    public record SimulateEmailRequest(
        String from,
        String to,
        String subject,
        String body,
        List<AttachmentInfo> attachments
    ) {}

    public record AttachmentInfo(
        String filename,
        DocumentType type,
        String contentReference
    ) {}
}
