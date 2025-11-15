package com.invoiceprocessor.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class InvoiceExtraction {
    private UUID id;
    private UUID documentId;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private String supplierName;
    private BigDecimal totalAmount;
    private String currency;

    public InvoiceExtraction() {
        this.id = UUID.randomUUID();
    }

    public InvoiceExtraction(UUID documentId, String invoiceNumber, LocalDate invoiceDate,
                           String supplierName, BigDecimal totalAmount, String currency) {
        this();
        this.documentId = documentId;
        this.invoiceNumber = invoiceNumber;
        this.invoiceDate = invoiceDate;
        this.supplierName = supplierName;
        this.totalAmount = totalAmount;
        this.currency = currency;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isValid() {
        return invoiceNumber != null && !invoiceNumber.trim().isEmpty()
            && invoiceDate != null
            && supplierName != null && !supplierName.trim().isEmpty()
            && totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0
            && currency != null && !currency.trim().isEmpty();
    }
}
