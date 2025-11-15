package com.invoiceprocessor.adapter.out.db;

import com.invoiceprocessor.application.port.out.InvoiceExtractionRepository;
import com.invoiceprocessor.domain.entity.InvoiceExtraction;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class InvoiceExtractionRepositoryImpl implements InvoiceExtractionRepository {

    private final InvoiceExtractionJpaRepository jpaRepository;

    public InvoiceExtractionRepositoryImpl(InvoiceExtractionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public InvoiceExtraction save(InvoiceExtraction extraction) {
        InvoiceExtractionJpaEntity entity = InvoiceExtractionJpaEntity.fromDomain(extraction);
        entity = jpaRepository.save(entity);
        return entity.toDomain();
    }

    @Override
    public Optional<InvoiceExtraction> findByDocumentId(UUID documentId) {
        return jpaRepository.findByDocumentId(documentId)
            .map(InvoiceExtractionJpaEntity::toDomain);
    }

    @Override
    public void deleteByDocumentId(UUID documentId) {
        jpaRepository.deleteByDocumentId(documentId);
    }
}
