package com.invoiceprocessor.domain.entity;

import java.util.UUID;

public class Document {
    private UUID id;
    private UUID stackId;
    private DocumentType type;
    private String filename;
    private String contentLocation;
    private LlmClassification llmClassification;
    private ExtractionStatus extractionStatus;

    public Document() {
        this.id = UUID.randomUUID();
        this.llmClassification = LlmClassification.UNKNOWN;
        this.extractionStatus = ExtractionStatus.NEW;
    }

    public Document(UUID stackId, DocumentType type, String filename, String contentLocation) {
        this();
        this.stackId = stackId;
        this.type = type;
        this.filename = filename;
        this.contentLocation = contentLocation;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getStackId() {
        return stackId;
    }

    public void setStackId(UUID stackId) {
        this.stackId = stackId;
    }

    public DocumentType getType() {
        return type;
    }

    public void setType(DocumentType type) {
        this.type = type;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentLocation() {
        return contentLocation;
    }

    public void setContentLocation(String contentLocation) {
        this.contentLocation = contentLocation;
    }

    public LlmClassification getLlmClassification() {
        return llmClassification;
    }

    public void setLlmClassification(LlmClassification llmClassification) {
        this.llmClassification = llmClassification;
    }

    public ExtractionStatus getExtractionStatus() {
        return extractionStatus;
    }

    public void setExtractionStatus(ExtractionStatus extractionStatus) {
        this.extractionStatus = extractionStatus;
    }
}
