package com.invoiceprocessor.domain.service;

import com.invoiceprocessor.domain.entity.Document;
import com.invoiceprocessor.domain.entity.ExtractionStatus;
import com.invoiceprocessor.domain.entity.InvoiceExtraction;
import com.invoiceprocessor.domain.entity.LlmClassification;
import org.springframework.stereotype.Service;

@Service
public class InvoiceProcessingService {

    public ProcessingResult processDocumentWithLlmResult(Document document, LlmExtractionResult llmResult) {
        if (llmResult == null) {
            document.setExtractionStatus(ExtractionStatus.ERROR);
            return new ProcessingResult(false, null, "LLM result is null");
        }

        document.setLlmClassification(llmResult.classification());

        if (llmResult.classification() == LlmClassification.NOT_INVOICE) {
            document.setExtractionStatus(ExtractionStatus.NOT_APPLICABLE);
            return new ProcessingResult(true, null, "Document classified as NOT_INVOICE");
        }

        if (llmResult.classification() == LlmClassification.INVOICE) {
            InvoiceExtraction extraction = new InvoiceExtraction(
                document.getId(),
                llmResult.invoiceNumber(),
                llmResult.invoiceDate(),
                llmResult.supplierName(),
                llmResult.totalAmount(),
                llmResult.currency()
            );

            if (extraction.isValid()) {
                document.setExtractionStatus(ExtractionStatus.PROCESSED);
                return new ProcessingResult(true, extraction, "Invoice extraction successful");
            } else {
                document.setExtractionStatus(ExtractionStatus.ERROR);
                return new ProcessingResult(false, null, "Invoice extraction validation failed: missing required fields");
            }
        }

        document.setExtractionStatus(ExtractionStatus.ERROR);
        return new ProcessingResult(false, null, "Unknown classification: " + llmResult.classification());
    }

    public record LlmExtractionResult(
        LlmClassification classification,
        String invoiceNumber,
        java.time.LocalDate invoiceDate,
        String supplierName,
        java.math.BigDecimal totalAmount,
        String currency
    ) {}

    public record ProcessingResult(
        boolean success,
        InvoiceExtraction extraction,
        String message
    ) {}
}
