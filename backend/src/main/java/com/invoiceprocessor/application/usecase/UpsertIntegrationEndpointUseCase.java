package com.invoiceprocessor.application.usecase;

import com.invoiceprocessor.application.port.out.IntegrationEndpointRepository;
import com.invoiceprocessor.domain.entity.EndpointType;
import com.invoiceprocessor.domain.entity.IntegrationEndpoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class UpsertIntegrationEndpointUseCase {

    private final IntegrationEndpointRepository repository;

    public UpsertIntegrationEndpointUseCase(IntegrationEndpointRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public IntegrationEndpoint execute(UpsertIntegrationEndpointRequest request) {
        IntegrationEndpoint endpoint = repository.findByType(request.type())
            .orElseGet(IntegrationEndpoint::new);

        if (endpoint.getId() == null) {
            endpoint.setId(UUID.randomUUID());
            endpoint.setCreatedAt(Instant.now());
        }
        endpoint.setUpdatedAt(Instant.now());
        endpoint.setName(request.name());
        endpoint.setType(request.type());
        endpoint.setSettings(request.settings());

        return repository.save(endpoint);
    }

    public record UpsertIntegrationEndpointRequest(
        String name,
        EndpointType type,
        Map<String, String> settings
    ) {}
}
