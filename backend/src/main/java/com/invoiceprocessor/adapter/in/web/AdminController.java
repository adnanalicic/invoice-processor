package com.invoiceprocessor.adapter.in.web;

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

    public AdminController(
            GetIntegrationEndpointsUseCase getUseCase,
            UpsertIntegrationEndpointUseCase upsertUseCase) {
        this.getUseCase = getUseCase;
        this.upsertUseCase = upsertUseCase;
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

    public record UpsertRequest(
        String name,
        Map<String, String> settings
    ) {}
}
