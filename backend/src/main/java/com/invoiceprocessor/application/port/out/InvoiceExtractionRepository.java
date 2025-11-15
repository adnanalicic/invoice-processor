package com.invoiceprocessor.application.port.out;

import com.invoiceprocessor.domain.entity.InvoiceExtraction;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceExtractionRepository {
    InvoiceExtraction save(InvoiceExtraction extraction);
    Optional<InvoiceExtraction> findByDocumentId(UUID documentId);
    void deleteByDocumentId(UUID documentId);
}
