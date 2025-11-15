package com.invoiceprocessor.adapter.out.db;

import com.invoiceprocessor.application.port.out.IntegrationEndpointRepository;
import com.invoiceprocessor.domain.entity.EndpointType;
import com.invoiceprocessor.domain.entity.IntegrationEndpoint;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class IntegrationEndpointRepositoryImpl implements IntegrationEndpointRepository {

    private final IntegrationEndpointJpaRepository jpaRepository;

    public IntegrationEndpointRepositoryImpl(IntegrationEndpointJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public IntegrationEndpoint save(IntegrationEndpoint endpoint) {
        IntegrationEndpointJpaEntity entity = IntegrationEndpointJpaEntity.fromDomain(endpoint);
        entity = jpaRepository.save(entity);
        return entity.toDomain();
    }

    @Override
    public Optional<IntegrationEndpoint> findById(java.util.UUID id) {
        return jpaRepository.findById(id).map(IntegrationEndpointJpaEntity::toDomain);
    }

    @Override
    public Optional<IntegrationEndpoint> findByType(EndpointType type) {
        return jpaRepository.findByType(type).map(IntegrationEndpointJpaEntity::toDomain);
    }

    @Override
    public List<IntegrationEndpoint> findAll() {
        return jpaRepository.findAll()
            .stream()
            .map(IntegrationEndpointJpaEntity::toDomain)
            .collect(Collectors.toList());
    }
}
