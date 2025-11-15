package com.invoiceprocessor.adapter.out.llm;

import com.invoiceprocessor.domain.entity.Document;
import com.invoiceprocessor.domain.entity.DocumentType;
import com.invoiceprocessor.domain.entity.LlmClassification;
import com.invoiceprocessor.domain.service.InvoiceProcessingService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

class StubLlmInvoiceExtractorTest {

    private StubLlmInvoiceExtractor extractor = new StubLlmInvoiceExtractor();

    @Test
    void testExtract_FilenameContainsInvoice_ShouldClassifyAsInvoice() {
        Document document = new Document(
            UUID.randomUUID(),
            DocumentType.PDF_ATTACHMENT,
            "invoice-123.pdf",
            "dummy-location"
        );

        InvoiceProcessingService.LlmExtractionResult result = extractor.extract(document);

        assertEquals(LlmClassification.INVOICE, result.classification());
        assertNotNull(result.invoiceNumber());
        assertNotNull(result.invoiceDate());
        assertNotNull(result.supplierName());
        assertNotNull(result.totalAmount());
        assertNotNull(result.currency());
    }

    @Test
    void testExtract_ContentLocationContainsInvoice_ShouldClassifyAsInvoice() {
        Document document = new Document(
            UUID.randomUUID(),
            DocumentType.PDF_ATTACHMENT,
            "document.pdf",
            "invoice-reference-123"
        );

        InvoiceProcessingService.LlmExtractionResult result = extractor.extract(document);

        assertEquals(LlmClassification.INVOICE, result.classification());
    }

    @Test
    void testExtract_NoInvoiceInName_ShouldClassifyAsNotInvoice() {
        Document document = new Document(
            UUID.randomUUID(),
            DocumentType.PDF_ATTACHMENT,
            "report.pdf",
            "dummy-location"
        );

        InvoiceProcessingService.LlmExtractionResult result = extractor.extract(document);

        assertEquals(LlmClassification.NOT_INVOICE, result.classification());
        assertNull(result.invoiceNumber());
        assertNull(result.invoiceDate());
    }

    @Test
    void testExtract_CaseInsensitive_ShouldClassifyAsInvoice() {
        Document document = new Document(
            UUID.randomUUID(),
            DocumentType.PDF_ATTACHMENT,
            "INVOICE-123.PDF",
            "dummy-location"
        );

        InvoiceProcessingService.LlmExtractionResult result = extractor.extract(document);

        assertEquals(LlmClassification.INVOICE, result.classification());
    }
}
