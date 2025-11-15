package com.invoiceprocessor.adapter.out.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceExtractionJpaRepository extends JpaRepository<InvoiceExtractionJpaEntity, UUID> {
    Optional<InvoiceExtractionJpaEntity> findByDocumentId(UUID documentId);
    void deleteByDocumentId(UUID documentId);
}
