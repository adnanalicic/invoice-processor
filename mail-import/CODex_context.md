# Mail-Import Service — Codex Context (2025-11-15)

## Role
- Headless Spring Boot microservice that periodically reads emails from IMAP and forwards them to the backend over REST.
- Does **not** own business logic; only fetches + forwards.

## Key Tech
- Spring Boot 3 (Java 17) with scheduling enabled (`@EnableScheduling`).
- JPA/Hibernate + PostgreSQL (read-only usage) to pull IMAP config from `integration_endpoints`.
- Jakarta Mail (`jakarta.mail`) for IMAP.

## Important Components
- `EmailImportApplication` — main non-web Spring Boot app in `mail-import`.
- `application.port.out.EmailFetcher` — port for fetching emails.
- `adapter.out.email.ImapEmailFetcher` — IMAP implementation using `EMAIL_SOURCE` integration endpoint.
- `adapter.out.db.*` — minimal JPA layer for `integration_endpoints`.
- `emailimport.EmailImportScheduler` — scheduled job polling IMAP (`email.import.folder`, `email.import.poll-interval-ms`).
- `emailimport.EmailForwarder` — sends each email to backend REST API.

## Backend Communication
- Property: `backend.api.base-url` (default `http://localhost:8080`).
- Sends `EmailImportRequest` (from/to/subject/body/attachments) as JSON to:
  - `POST {backend.api.base-url}/api/internal/email-import`.
- Backend then creates stacks/documents via its own use cases.

## Configuration Expectations
- Mail-import and backend share the same PostgreSQL DB so that `integration_endpoints` is visible.
- IMAP credentials configured via backend admin API into `integration_endpoints` (`type = EMAIL_SOURCE`).
- Typical deployment: separate containers `backend` and `mail-import`, same DB, `mail-import` pointing at backend URL.

