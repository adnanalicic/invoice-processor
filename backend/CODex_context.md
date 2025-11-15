# Backend Service — Codex Context (2025-11-15)

## Role
- Central HTTP API and business logic for invoice processing.
- Owns domain model (stacks, documents, extractions) and persistence.
- Exposes REST endpoints used by frontend and by the mail-import microservice.

## Key Tech
- Spring Boot 3 (Java 17), JPA/Hibernate, PostgreSQL, MinIO (S3 compatible).
- Flyway migrations: `src/main/resources/db/migration`.

## Integrations
- Email / IMAP configuration stored in `integration_endpoints` table (`type = EMAIL_SOURCE`).
- S3 storage configuration stored in `integration_endpoints` (`type = STORAGE_TARGET`).
- MinIO requires `"forcePathStyle": "true"` and full endpoint URI.

## Important Components
- `InvoiceProcessorApplication` — main Spring Boot app (no schedulers).
- `adapter.in.web.*` — REST controllers (admin, stacks, documents, internal email-import).
- `application.usecase.*` — use cases such as `CreateManualStackUseCase`, `ImportEmailsUseCase` (now only used directly by backend, not scheduled).
- `adapter.out.storage.S3StorageService` — S3/MinIO storage via `STORAGE_TARGET` endpoint.
- `adapter.out.db.*` — JPA entities/repositories including `IntegrationEndpoint*`.

## Mail-Import Integration
- Internal REST endpoint: `POST /api/internal/email-import`.
  - Controller: `adapter.in.web.EmailImportController`.
  - Accepts `EmailImportRequest` (from/to/subject/body/attachments) and calls `CreateManualStackUseCase`.
- Mail-import microservice connects here via `backend.api.base-url` (e.g. `http://backend:8080`).

