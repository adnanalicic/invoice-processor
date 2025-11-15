package com.invoiceprocessor.domain.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Stack {
    private UUID id;
    private Instant receivedAt;
    private String fromAddress;
    private String toAddress;
    private String subject;
    private StackStatus status;
    private List<Document> documents;

    public Stack() {
        this.id = UUID.randomUUID();
        this.receivedAt = Instant.now();
        this.status = StackStatus.RECEIVED;
        this.documents = new ArrayList<>();
    }

    public Stack(String fromAddress, String toAddress, String subject) {
        this();
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.subject = subject;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public StackStatus getStatus() {
        return status;
    }

    public void setStatus(StackStatus status) {
        this.status = status;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }

    public void addDocument(Document document) {
        this.documents.add(document);
    }

    public void updateStatusFromDocuments() {
        boolean hasError = documents.stream()
            .anyMatch(doc -> doc.getExtractionStatus() == ExtractionStatus.ERROR);
        
        if (hasError) {
            this.status = StackStatus.ERROR;
            return;
        }

        boolean allFinal = documents.stream()
            .allMatch(doc -> doc.getExtractionStatus() == ExtractionStatus.PROCESSED
                || doc.getExtractionStatus() == ExtractionStatus.NOT_APPLICABLE);
        
        if (allFinal && !documents.isEmpty()) {
            this.status = StackStatus.PROCESSED;
        } else if (!documents.isEmpty()) {
            this.status = StackStatus.PROCESSING;
        }
    }
}
