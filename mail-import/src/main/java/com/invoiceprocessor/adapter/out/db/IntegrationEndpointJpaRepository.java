package com.invoiceprocessor.adapter.out.db;

import com.invoiceprocessor.domain.entity.EndpointType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IntegrationEndpointJpaRepository extends JpaRepository<IntegrationEndpointJpaEntity, UUID> {
    Optional<IntegrationEndpointJpaEntity> findByType(EndpointType type);
}

