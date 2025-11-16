package com.invoiceprocessor.adapter.out.llm;

import com.invoiceprocessor.application.port.out.LlmInvoiceExtractor;
import com.invoiceprocessor.domain.entity.Document;
import com.invoiceprocessor.domain.entity.LlmClassification;
import com.invoiceprocessor.domain.service.InvoiceProcessingService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Stub implementation of LLMInvoiceExtractor.
 * 
 * This is a simple, deterministic stub for development/testing.
 * Replace this adapter with a real LLM integration when ready.
 * 
 * Behavior:
 * - If filename or contentLocation contains "invoice" (case-insensitive), 
 *   classify as INVOICE and return dummy but valid invoice data.
 * - Otherwise, classify as NOT_INVOICE.
 */
@Component
@Profile("stub-llm")
public class StubLlmInvoiceExtractor implements LlmInvoiceExtractor {

    @Override
    public InvoiceProcessingService.LlmExtractionResult extract(Document document) {
        String filename = document.getFilename() != null ? document.getFilename().toLowerCase() : "";
        String contentLocation = document.getContentLocation() != null 
            ? document.getContentLocation().toLowerCase() : "";

        boolean isInvoice = filename.contains("invoice") || contentLocation.contains("invoice");

        if (isInvoice) {
            return new InvoiceProcessingService.LlmExtractionResult(
                LlmClassification.INVOICE,
                "INV-" + document.getId().toString().substring(0, 8).toUpperCase(),
                LocalDate.now(),
                "Example Supplier Inc.",
                new BigDecimal("1234.56"),
                "EUR"
            );
        } else {
            return new InvoiceProcessingService.LlmExtractionResult(
                LlmClassification.NOT_INVOICE,
                null,
                null,
                null,
                null,
                null
            );
        }
    }
}
