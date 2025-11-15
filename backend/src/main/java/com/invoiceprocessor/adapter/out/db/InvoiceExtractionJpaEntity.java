package com.invoiceprocessor.adapter.out.db;

import com.invoiceprocessor.domain.entity.InvoiceExtraction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "invoice_extractions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceExtractionJpaEntity {
    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "document_id", columnDefinition = "UUID", nullable = false, unique = true)
    private UUID documentId;

    @Column(nullable = false)
    private String invoiceNumber;

    @Column(nullable = false)
    private LocalDate invoiceDate;

    @Column(nullable = false)
    private String supplierName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String currency;

    public static InvoiceExtractionJpaEntity fromDomain(InvoiceExtraction extraction) {
        InvoiceExtractionJpaEntity entity = new InvoiceExtractionJpaEntity();
        entity.setId(extraction.getId());
        entity.setDocumentId(extraction.getDocumentId());
        entity.setInvoiceNumber(extraction.getInvoiceNumber());
        entity.setInvoiceDate(extraction.getInvoiceDate());
        entity.setSupplierName(extraction.getSupplierName());
        entity.setTotalAmount(extraction.getTotalAmount());
        entity.setCurrency(extraction.getCurrency());
        return entity;
    }

    public InvoiceExtraction toDomain() {
        InvoiceExtraction extraction = new InvoiceExtraction();
        extraction.setId(this.id);
        extraction.setDocumentId(this.documentId);
        extraction.setInvoiceNumber(this.invoiceNumber);
        extraction.setInvoiceDate(this.invoiceDate);
        extraction.setSupplierName(this.supplierName);
        extraction.setTotalAmount(this.totalAmount);
        extraction.setCurrency(this.currency);
        return extraction;
    }
}
