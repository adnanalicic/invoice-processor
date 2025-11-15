package com.invoiceprocessor.application.usecase;

import com.invoiceprocessor.application.port.out.DocumentRepository;
import com.invoiceprocessor.application.port.out.InvoiceExtractionRepository;
import com.invoiceprocessor.application.port.out.StackRepository;
import com.invoiceprocessor.domain.entity.Document;
import com.invoiceprocessor.domain.entity.InvoiceExtraction;
import com.invoiceprocessor.domain.entity.Stack;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GetStackDetailsUseCase {

    private final StackRepository stackRepository;
    private final DocumentRepository documentRepository;
    private final InvoiceExtractionRepository extractionRepository;

    public GetStackDetailsUseCase(
            StackRepository stackRepository,
            DocumentRepository documentRepository,
            InvoiceExtractionRepository extractionRepository) {
        this.stackRepository = stackRepository;
        this.documentRepository = documentRepository;
        this.extractionRepository = extractionRepository;
    }

    public StackDetailsResponse execute(UUID stackId) {
        Stack stack = stackRepository.findById(stackId)
            .orElseThrow(() -> new RuntimeException("Stack not found: " + stackId));

        List<Document> documents = documentRepository.findByStackId(stackId);

        List<DocumentDetails> documentDetails = documents.stream()
            .map(doc -> {
                Optional<InvoiceExtraction> extraction = extractionRepository.findByDocumentId(doc.getId());
                return new DocumentDetails(
                    doc.getId(),
                    doc.getType(),
                    doc.getFilename(),
                    doc.getLlmClassification(),
                    doc.getExtractionStatus(),
                    extraction.map(InvoiceDetails::from).orElse(null)
                );
            })
            .collect(Collectors.toList());

        return new StackDetailsResponse(
            stack.getId(),
            stack.getSubject(),
            stack.getFromAddress(),
            stack.getToAddress(),
            stack.getReceivedAt(),
            stack.getStatus(),
            documentDetails
        );
    }

    public record StackDetailsResponse(
        UUID id,
        String subject,
        String fromAddress,
        String toAddress,
        java.time.Instant receivedAt,
        com.invoiceprocessor.domain.entity.StackStatus status,
        List<DocumentDetails> documents
    ) {}

    public record DocumentDetails(
        UUID id,
        com.invoiceprocessor.domain.entity.DocumentType type,
        String filename,
        com.invoiceprocessor.domain.entity.LlmClassification classification,
        com.invoiceprocessor.domain.entity.ExtractionStatus extractionStatus,
        InvoiceDetails invoice
    ) {}

    public record InvoiceDetails(
        UUID id,
        String invoiceNumber,
        java.time.LocalDate invoiceDate,
        String supplierName,
        java.math.BigDecimal totalAmount,
        String currency
    ) {
        public static InvoiceDetails from(InvoiceExtraction extraction) {
            return new InvoiceDetails(
                extraction.getId(),
                extraction.getInvoiceNumber(),
                extraction.getInvoiceDate(),
                extraction.getSupplierName(),
                extraction.getTotalAmount(),
                extraction.getCurrency()
            );
        }
    }
}
