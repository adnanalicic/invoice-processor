package com.invoiceprocessor.application.usecase;

import com.invoiceprocessor.application.port.out.IntegrationEndpointRepository;
import com.invoiceprocessor.domain.entity.EndpointType;
import com.invoiceprocessor.domain.entity.IntegrationEndpoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EmailSourceEndpointsUseCase {

    private final IntegrationEndpointRepository repository;

    public EmailSourceEndpointsUseCase(IntegrationEndpointRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<IntegrationEndpoint> listEmailSources() {
        return repository.findAllByType(EndpointType.EMAIL_SOURCE);
    }

    @Transactional
    public IntegrationEndpoint createEmailSource(String name, Map<String, String> settings) {
        IntegrationEndpoint endpoint = new IntegrationEndpoint();
        endpoint.setId(UUID.randomUUID());
        endpoint.setName(name);
        endpoint.setType(EndpointType.EMAIL_SOURCE);
        endpoint.setSettings(settings);
        Instant now = Instant.now();
        endpoint.setCreatedAt(now);
        endpoint.setUpdatedAt(now);
        return repository.save(endpoint);
    }

    @Transactional
    public IntegrationEndpoint updateEmailSource(UUID id, String name, Map<String, String> settings) {
        IntegrationEndpoint endpoint = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Email source endpoint not found: " + id));

        endpoint.setName(name);
        endpoint.setSettings(settings);
        endpoint.setUpdatedAt(Instant.now());

        return repository.save(endpoint);
    }

    @Transactional
    public void deleteEmailSource(UUID id) {
        repository.deleteById(id);
    }
}

