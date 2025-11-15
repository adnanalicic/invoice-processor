package com.invoiceprocessor.application.port.out;

import com.invoiceprocessor.domain.entity.Document;
import com.invoiceprocessor.domain.service.InvoiceProcessingService;

public interface LlmInvoiceExtractor {
    InvoiceProcessingService.LlmExtractionResult extract(Document document);
}
