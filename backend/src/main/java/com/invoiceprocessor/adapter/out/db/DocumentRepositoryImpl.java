package com.invoiceprocessor.adapter.out.db;

import com.invoiceprocessor.application.port.out.DocumentRepository;
import com.invoiceprocessor.domain.entity.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class DocumentRepositoryImpl implements DocumentRepository {

    private final DocumentJpaRepository jpaRepository;

    public DocumentRepositoryImpl(DocumentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Document save(Document document) {
        DocumentJpaEntity entity = DocumentJpaEntity.fromDomain(document);
        entity = jpaRepository.save(entity);
        return entity.toDomain();
    }

    @Override
    public Optional<Document> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(DocumentJpaEntity::toDomain);
    }

    @Override
    public List<Document> findByStackId(UUID stackId) {
        return jpaRepository.findByStackId(stackId)
            .stream()
            .map(DocumentJpaEntity::toDomain)
            .collect(Collectors.toList());
    }
}
