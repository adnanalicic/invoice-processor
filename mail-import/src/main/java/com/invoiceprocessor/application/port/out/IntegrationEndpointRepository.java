package com.invoiceprocessor.application.port.out;

import com.invoiceprocessor.domain.entity.EndpointType;
import com.invoiceprocessor.domain.entity.IntegrationEndpoint;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IntegrationEndpointRepository {
    IntegrationEndpoint save(IntegrationEndpoint endpoint);
    Optional<IntegrationEndpoint> findById(UUID id);
    Optional<IntegrationEndpoint> findByType(EndpointType type);
    List<IntegrationEndpoint> findAllByType(EndpointType type);
    List<IntegrationEndpoint> findAll();
    void deleteById(UUID id);
}
