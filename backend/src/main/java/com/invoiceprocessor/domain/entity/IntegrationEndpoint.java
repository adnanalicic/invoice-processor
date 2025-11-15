package com.invoiceprocessor.domain.entity;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IntegrationEndpoint {
    private UUID id;
    private String name;
    private EndpointType type;
    private Map<String, String> settings;
    private Instant createdAt;
    private Instant updatedAt;

    public IntegrationEndpoint() {
        this.id = UUID.randomUUID();
        this.settings = new HashMap<>();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public IntegrationEndpoint(String name, EndpointType type, Map<String, String> settings) {
        this();
        this.name = name;
        this.type = type;
        if (settings != null) {
            this.settings.putAll(settings);
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EndpointType getType() {
        return type;
    }

    public void setType(EndpointType type) {
        this.type = type;
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
