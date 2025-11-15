package com.invoiceprocessor.application.usecase;

import com.invoiceprocessor.application.port.out.IntegrationEndpointRepository;
import com.invoiceprocessor.domain.entity.EndpointType;
import com.invoiceprocessor.domain.entity.IntegrationEndpoint;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GetIntegrationEndpointsUseCase {

    private final IntegrationEndpointRepository repository;

    public GetIntegrationEndpointsUseCase(IntegrationEndpointRepository repository) {
        this.repository = repository;
    }

    public List<IntegrationEndpointResponse> execute() {
        return repository.findAll().stream()
            .map(IntegrationEndpointResponse::from)
            .toList();
    }

    public IntegrationEndpointResponse getByType(EndpointType type) {
        IntegrationEndpoint endpoint = repository.findByType(type)
            .orElseThrow(() -> new RuntimeException("Endpoint not found: " + type));
        return IntegrationEndpointResponse.from(endpoint);
    }

    public record IntegrationEndpointResponse(
        UUID id,
        String name,
        EndpointType type,
        Map<String, String> settings
    ) {
        public static IntegrationEndpointResponse from(IntegrationEndpoint endpoint) {
            return new IntegrationEndpointResponse(
                endpoint.getId(),
                endpoint.getName(),
                endpoint.getType(),
                endpoint.getSettings()
            );
        }
    }
}
