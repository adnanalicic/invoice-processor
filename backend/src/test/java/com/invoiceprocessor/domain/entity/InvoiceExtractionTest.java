package com.invoiceprocessor.domain.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

class InvoiceExtractionTest {

    @Test
    void testIsValid_AllFieldsValid_ShouldReturnTrue() {
        InvoiceExtraction extraction = new InvoiceExtraction(
            UUID.randomUUID(),
            "INV-123",
            LocalDate.now(),
            "Supplier Inc.",
            new BigDecimal("1234.56"),
            "EUR"
        );

        assertTrue(extraction.isValid());
    }

    @Test
    void testIsValid_MissingInvoiceNumber_ShouldReturnFalse() {
        InvoiceExtraction extraction = new InvoiceExtraction(
            UUID.randomUUID(),
            null,
            LocalDate.now(),
            "Supplier Inc.",
            new BigDecimal("1234.56"),
            "EUR"
        );

        assertFalse(extraction.isValid());
    }

    @Test
    void testIsValid_EmptyInvoiceNumber_ShouldReturnFalse() {
        InvoiceExtraction extraction = new InvoiceExtraction(
            UUID.randomUUID(),
            "   ",
            LocalDate.now(),
            "Supplier Inc.",
            new BigDecimal("1234.56"),
            "EUR"
        );

        assertFalse(extraction.isValid());
    }

    @Test
    void testIsValid_MissingDate_ShouldReturnFalse() {
        InvoiceExtraction extraction = new InvoiceExtraction(
            UUID.randomUUID(),
            "INV-123",
            null,
            "Supplier Inc.",
            new BigDecimal("1234.56"),
            "EUR"
        );

        assertFalse(extraction.isValid());
    }

    @Test
    void testIsValid_ZeroAmount_ShouldReturnFalse() {
        InvoiceExtraction extraction = new InvoiceExtraction(
            UUID.randomUUID(),
            "INV-123",
            LocalDate.now(),
            "Supplier Inc.",
            BigDecimal.ZERO,
            "EUR"
        );

        assertFalse(extraction.isValid());
    }

    @Test
    void testIsValid_NegativeAmount_ShouldReturnFalse() {
        InvoiceExtraction extraction = new InvoiceExtraction(
            UUID.randomUUID(),
            "INV-123",
            LocalDate.now(),
            "Supplier Inc.",
            new BigDecimal("-100"),
            "EUR"
        );

        assertFalse(extraction.isValid());
    }
}
