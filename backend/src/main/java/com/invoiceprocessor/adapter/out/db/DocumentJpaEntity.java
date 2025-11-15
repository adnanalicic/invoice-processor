package com.invoiceprocessor.adapter.out.db;

import com.invoiceprocessor.domain.entity.Document;
import com.invoiceprocessor.domain.entity.DocumentType;
import com.invoiceprocessor.domain.entity.ExtractionStatus;
import com.invoiceprocessor.domain.entity.LlmClassification;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentJpaEntity {
    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "stack_id", columnDefinition = "UUID", nullable = false)
    private UUID stackId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType type;

    private String filename;

    @Column(nullable = false)
    private String contentLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LlmClassification llmClassification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExtractionStatus extractionStatus;

    public static DocumentJpaEntity fromDomain(Document document) {
        DocumentJpaEntity entity = new DocumentJpaEntity();
        entity.setId(document.getId());
        entity.setStackId(document.getStackId());
        entity.setType(document.getType());
        entity.setFilename(document.getFilename());
        entity.setContentLocation(document.getContentLocation());
        entity.setLlmClassification(document.getLlmClassification());
        entity.setExtractionStatus(document.getExtractionStatus());
        return entity;
    }

    public Document toDomain() {
        Document document = new Document();
        document.setId(this.id);
        document.setStackId(this.stackId);
        document.setType(this.type);
        document.setFilename(this.filename);
        document.setContentLocation(this.contentLocation);
        document.setLlmClassification(this.llmClassification);
        document.setExtractionStatus(this.extractionStatus);
        return document;
    }
}
