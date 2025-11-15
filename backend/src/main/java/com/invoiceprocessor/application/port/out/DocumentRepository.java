package com.invoiceprocessor.application.port.out;

import com.invoiceprocessor.domain.entity.Document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository {
    Document save(Document document);
    Optional<Document> findById(UUID id);
    List<Document> findByStackId(UUID stackId);
}
