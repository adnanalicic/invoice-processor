package com.invoiceprocessor.adapter.out.db;

import com.invoiceprocessor.domain.entity.EndpointType;
import com.invoiceprocessor.domain.entity.IntegrationEndpoint;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "integration_endpoints")
@Getter
@Setter
@NoArgsConstructor
public class IntegrationEndpointJpaEntity {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EndpointType type;

    @Column(name = "settings_json", nullable = false, columnDefinition = "TEXT")
    private String settingsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static IntegrationEndpointJpaEntity fromDomain(IntegrationEndpoint endpoint) {
        IntegrationEndpointJpaEntity entity = new IntegrationEndpointJpaEntity();
        entity.setId(endpoint.getId());
        entity.setName(endpoint.getName());
        entity.setType(endpoint.getType());
        entity.setSettingsJson(writeSettings(endpoint.getSettings()));
        entity.setCreatedAt(endpoint.getCreatedAt());
        entity.setUpdatedAt(endpoint.getUpdatedAt());
        return entity;
    }

    public IntegrationEndpoint toDomain() {
        IntegrationEndpoint endpoint = new IntegrationEndpoint();
        endpoint.setId(this.id);
        endpoint.setName(this.name);
        endpoint.setType(this.type);
        endpoint.setSettings(readSettings(this.settingsJson));
        endpoint.setCreatedAt(this.createdAt);
        endpoint.setUpdatedAt(this.updatedAt);
        return endpoint;
    }

    private static String writeSettings(Map<String, String> settings) {
        try {
            return OBJECT_MAPPER.writeValueAsString(settings != null ? settings : Map.of());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize settings", e);
        }
    }

    private static Map<String, String> readSettings(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Map.of();
            }
            return OBJECT_MAPPER.readValue(json, OBJECT_MAPPER.getTypeFactory()
                .constructMapType(Map.class, String.class, String.class));
        } catch (Exception e) {
            return Map.of();
        }
    }
}
