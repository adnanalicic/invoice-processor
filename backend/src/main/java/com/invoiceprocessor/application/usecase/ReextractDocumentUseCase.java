package com.invoiceprocessor.application.usecase;

import com.invoiceprocessor.application.port.out.DocumentRepository;
import com.invoiceprocessor.application.port.out.InvoiceExtractionRepository;
import com.invoiceprocessor.application.port.out.LlmInvoiceExtractor;
import com.invoiceprocessor.application.port.out.StackRepository;
import com.invoiceprocessor.domain.entity.Document;
import com.invoiceprocessor.domain.entity.ExtractionStatus;
import com.invoiceprocessor.domain.entity.Stack;
import com.invoiceprocessor.domain.service.InvoiceProcessingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReextractDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final InvoiceExtractionRepository extractionRepository;
    private final StackRepository stackRepository;
    private final LlmInvoiceExtractor llmExtractor;
    private final InvoiceProcessingService processingService;

    public ReextractDocumentUseCase(
            DocumentRepository documentRepository,
            InvoiceExtractionRepository extractionRepository,
            StackRepository stackRepository,
            LlmInvoiceExtractor llmExtractor,
            InvoiceProcessingService processingService) {
        this.documentRepository = documentRepository;
        this.extractionRepository = extractionRepository;
        this.stackRepository = stackRepository;
        this.llmExtractor = llmExtractor;
        this.processingService = processingService;
    }

    @Transactional
    public void execute(UUID documentId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        extractionRepository.deleteByDocumentId(documentId);

        document.setExtractionStatus(ExtractionStatus.EXTRACTING);
        documentRepository.save(document);

        InvoiceProcessingService.LlmExtractionResult llmResult = llmExtractor.extract(document);
        InvoiceProcessingService.ProcessingResult result = processingService.processDocumentWithLlmResult(document, llmResult);

        documentRepository.save(document);

        if (result.success() && result.extraction() != null) {
            extractionRepository.save(result.extraction());
        }

        Stack stack = stackRepository.findById(document.getStackId())
            .orElseThrow(() -> new RuntimeException("Stack not found: " + document.getStackId()));
        
        stack.setDocuments(documentRepository.findByStackId(stack.getId()));
        stack.updateStatusFromDocuments();
        stackRepository.save(stack);
    }
}
