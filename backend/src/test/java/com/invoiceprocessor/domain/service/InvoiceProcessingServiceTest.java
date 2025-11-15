package com.invoiceprocessor.domain.service;

import com.invoiceprocessor.domain.entity.Document;
import com.invoiceprocessor.domain.entity.DocumentType;
import com.invoiceprocessor.domain.entity.ExtractionStatus;
import com.invoiceprocessor.domain.entity.LlmClassification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

class InvoiceProcessingServiceTest {

    private InvoiceProcessingService service;
    private Document document;

    @BeforeEach
    void setUp() {
        service = new InvoiceProcessingService();
        document = new Document(UUID.randomUUID(), DocumentType.PDF_ATTACHMENT, "test.pdf", "test-location");
    }

    @Test
    void testProcessDocument_NotInvoice_ShouldSetNotApplicable() {
        InvoiceProcessingService.LlmExtractionResult llmResult = 
            new InvoiceProcessingService.LlmExtractionResult(
                LlmClassification.NOT_INVOICE,
                null, null, null, null, null
            );

        InvoiceProcessingService.ProcessingResult result = 
            service.processDocumentWithLlmResult(document, llmResult);

        assertTrue(result.success());
        assertEquals(ExtractionStatus.NOT_APPLICABLE, document.getExtractionStatus());
        assertEquals(LlmClassification.NOT_INVOICE, document.getLlmClassification());
        assertNull(result.extraction());
    }

    @Test
    void testProcessDocument_ValidInvoice_ShouldSetProcessed() {
        InvoiceProcessingService.LlmExtractionResult llmResult = 
            new InvoiceProcessingService.LlmExtractionResult(
                LlmClassification.INVOICE,
                "INV-123",
                LocalDate.now(),
                "Supplier Inc.",
                new BigDecimal("1234.56"),
                "EUR"
            );

        InvoiceProcessingService.ProcessingResult result = 
            service.processDocumentWithLlmResult(document, llmResult);

        assertTrue(result.success());
        assertEquals(ExtractionStatus.PROCESSED, document.getExtractionStatus());
        assertEquals(LlmClassification.INVOICE, document.getLlmClassification());
        assertNotNull(result.extraction());
        assertTrue(result.extraction().isValid());
    }

    @Test
    void testProcessDocument_InvalidInvoiceData_ShouldSetError() {
        InvoiceProcessingService.LlmExtractionResult llmResult = 
            new InvoiceProcessingService.LlmExtractionResult(
                LlmClassification.INVOICE,
                null, // Missing invoice number - invalid
                LocalDate.now(),
                "Supplier Inc.",
                new BigDecimal("1234.56"),
                "EUR"
            );

        InvoiceProcessingService.ProcessingResult result = 
            service.processDocumentWithLlmResult(document, llmResult);

        assertFalse(result.success());
        assertEquals(ExtractionStatus.ERROR, document.getExtractionStatus());
        assertEquals(LlmClassification.INVOICE, document.getLlmClassification());
    }

    @Test
    void testProcessDocument_NullResult_ShouldSetError() {
        InvoiceProcessingService.ProcessingResult result = 
            service.processDocumentWithLlmResult(document, null);

        assertFalse(result.success());
        assertEquals(ExtractionStatus.ERROR, document.getExtractionStatus());
    }
}
