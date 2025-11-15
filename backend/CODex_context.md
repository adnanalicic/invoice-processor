# Backend Service — Codex Context (2025-11-15)

## Role
- Central HTTP API and business logic for invoice processing.
- Owns the domain model (stacks, documents, extractions) and the full PostgreSQL schema.
- Exposes REST endpoints used by the Angular frontend and by the mail-import microservice.

## Key Tech
- Spring Boot 3 (Java 17), Spring Web, Spring Data JPA/Hibernate.
- PostgreSQL as the primary relational database.
- Flyway for schema migrations (`src/main/resources/db/migration`).
- MinIO / S3-compatible storage for email bodies and attachments.

## Integrations & Configuration Model
- Integration configuration is stored in the `integration_endpoints` table:
  - Columns: `id`, `name`, `type`, `settings_json`, `created_at`, `updated_at`.
  - `settings_json` is a JSON blob interpreted by various adapters (email, storage, etc.).
- Endpoint types (`EndpointType` enum):
  - `EMAIL_SOURCE` — IMAP inbox configurations used by mail-import.
    - **Multiple** rows allowed (one per inbox/folder).
  - `STORAGE_TARGET` — S3/MinIO endpoint used for file storage.
  - `OUTPUT_DESTINATION` — reserved for future downstream integrations.
- Database constraints:
  - Initial schema enforced a global `UNIQUE(type)`; migration `V3__Allow_multiple_email_sources.sql` relaxes this:
    - Multiple `EMAIL_SOURCE` rows are allowed.
    - A partial unique index keeps non-email types unique by `type`.

## Important Components
- `InvoiceProcessorApplication`
  - Main Spring Boot application class (no schedulers).

- `adapter.in.web.*`
  - REST controllers for external APIs:
    - Stacks, documents, and re-extraction operations.
    - Internal email-import endpoint (`EmailImportController`) used by mail-import.
    - Admin endpoints for integration configuration (`AdminController`).

- `application.usecase.*`
  - Use cases encapsulating business logic.
  - Examples:
    - `CreateManualStackUseCase` — manual uploads via the frontend.
    - `ImportEmailsUseCase` — legacy email import path (now typically driven via mail-import and the internal API).
    - `EmailSourceEndpointsUseCase` — CRUD for multiple `EMAIL_SOURCE` endpoints in `integration_endpoints`.

- `adapter.out.db.*`
  - JPA entities and repositories, including:
    - `IntegrationEndpointJpaEntity` / `IntegrationEndpointJpaRepository` / `IntegrationEndpointRepositoryImpl` for generic integration endpoints.
  - Acts as the persistence layer for all endpoint types.

- `adapter.out.storage.S3StorageService`
  - Implements `StorageService` using an S3-compatible backend (e.g. MinIO).
  - Loads its configuration from the `STORAGE_TARGET` endpoint in `integration_endpoints`:
    - Expects keys in `settings_json` such as `endpoint`, `accessKey`, `secretKey`, `bucket`, `region`, and `forcePathStyle`.
  - Responsible for bucket creation (on first use) and file upload.

- `adapter.in.web.DocumentController`
  - Handles document-related operations such as re-extraction and content retrieval.
  - Endpoints:
    - `POST /api/documents/{documentId}/reextract` — triggers re-extraction for a document.
    - `GET /api/documents/{documentId}/content` — streams the underlying file from storage (PDF/text) for inline viewing by the frontend.

## Admin API for Integrations
- `adapter.in.web.AdminController`
  - Base path: `/api/admin`.
  - Endpoints:
    - `GET /api/admin/endpoints`
      - Lists all integration endpoints with their types and settings.
    - `PUT /api/admin/endpoints/{type}`
      - Upserts a single endpoint per type (primarily used for `STORAGE_TARGET` and legacy single email config).
    - `GET /api/admin/email-sources`
      - Lists all `EMAIL_SOURCE` endpoints (multi-inbox configuration).
    - `POST /api/admin/email-sources`
      - Creates a new `EMAIL_SOURCE` endpoint (new inbox/folder).
    - `PUT /api/admin/email-sources/{id}`
      - Updates an existing `EMAIL_SOURCE` endpoint by id.
    - `DELETE /api/admin/email-sources/{id}`
      - Deletes an `EMAIL_SOURCE` endpoint by id.

## Mail-Import Integration
- Internal REST endpoint:
  - `POST /api/internal/email-import`
    - Controller: `adapter.in.web.EmailImportController`.
    - Request DTO: `EmailImportRequest` (from, to, subject, body, attachments).
    - Behaviour: creates a stack and associated documents, then triggers extraction.
- Mail-import microservice:
  - Uses `backend.api.base-url` (e.g. `http://backend:8080`) and calls `/api/internal/email-import`.
  - The backend treats emails forwarded by mail-import the same way as manually uploaded documents.

## Frontend Integration
- The Angular frontend calls backend APIs under `/api`:
  - Stack listing and details.
  - Document re-extraction.
  - Manual uploads.
  - Admin configuration for storage and email sources.
- Admin UI behaviour (including how multiple email inboxes are edited) is documented in `frontend/CODex_context.md` and implemented in Angular components such as `admin-page.component.ts`.
