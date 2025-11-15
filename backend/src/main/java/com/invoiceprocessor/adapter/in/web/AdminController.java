package com.invoiceprocessor.adapter.in.web;

import com.invoiceprocessor.application.usecase.EmailSourceEndpointsUseCase;
import com.invoiceprocessor.application.usecase.GetIntegrationEndpointsUseCase;
import com.invoiceprocessor.application.usecase.UpsertIntegrationEndpointUseCase;
import com.invoiceprocessor.domain.entity.EndpointType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final GetIntegrationEndpointsUseCase getUseCase;
    private final UpsertIntegrationEndpointUseCase upsertUseCase;
    private final EmailSourceEndpointsUseCase emailSourceUseCase;

    public AdminController(
            GetIntegrationEndpointsUseCase getUseCase,
            UpsertIntegrationEndpointUseCase upsertUseCase,
            EmailSourceEndpointsUseCase emailSourceUseCase) {
        this.getUseCase = getUseCase;
        this.upsertUseCase = upsertUseCase;
        this.emailSourceUseCase = emailSourceUseCase;
    }

    @GetMapping("/endpoints")
    public ResponseEntity<List<GetIntegrationEndpointsUseCase.IntegrationEndpointResponse>> getEndpoints() {
        return ResponseEntity.ok(getUseCase.execute());
    }

    @PutMapping("/endpoints/{type}")
    public ResponseEntity<GetIntegrationEndpointsUseCase.IntegrationEndpointResponse> upsertEndpoint(
            @PathVariable EndpointType type,
            @RequestBody UpsertRequest request) {
        var endpoint = upsertUseCase.execute(new UpsertIntegrationEndpointUseCase.UpsertIntegrationEndpointRequest(
            request.name(),
            type,
            request.settings()
        ));
        return ResponseEntity.ok(GetIntegrationEndpointsUseCase.IntegrationEndpointResponse.from(endpoint));
    }

    @GetMapping("/email-sources")
    public ResponseEntity<List<GetIntegrationEndpointsUseCase.IntegrationEndpointResponse>> getEmailSources() {
        var endpoints = emailSourceUseCase.listEmailSources().stream()
            .map(GetIntegrationEndpointsUseCase.IntegrationEndpointResponse::from)
            .toList();
        return ResponseEntity.ok(endpoints);
    }

    @PostMapping("/email-sources")
    public ResponseEntity<GetIntegrationEndpointsUseCase.IntegrationEndpointResponse> createEmailSource(
            @RequestBody EmailSourceRequest request) {
        var endpoint = emailSourceUseCase.createEmailSource(request.name(), request.settings());
        return ResponseEntity.ok(GetIntegrationEndpointsUseCase.IntegrationEndpointResponse.from(endpoint));
    }

    @PutMapping("/email-sources/{id}")
    public ResponseEntity<GetIntegrationEndpointsUseCase.IntegrationEndpointResponse> updateEmailSource(
            @PathVariable java.util.UUID id,
            @RequestBody EmailSourceRequest request) {
        var endpoint = emailSourceUseCase.updateEmailSource(id, request.name(), request.settings());
        return ResponseEntity.ok(GetIntegrationEndpointsUseCase.IntegrationEndpointResponse.from(endpoint));
    }

    @DeleteMapping("/email-sources/{id}")
    public ResponseEntity<Void> deleteEmailSource(@PathVariable java.util.UUID id) {
        emailSourceUseCase.deleteEmailSource(id);
        return ResponseEntity.noContent().build();
    }

    public record UpsertRequest(
        String name,
        Map<String, String> settings
    ) {}

    public record EmailSourceRequest(
        String name,
        Map<String, String> settings
    ) {}
}
