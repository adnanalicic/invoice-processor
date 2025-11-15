# Mail-Import Service — Codex Context (2025-11-15)

## Role
- Headless Spring Boot microservice that periodically reads emails from IMAP and forwards them to the backend over REST.
- Does **not** own business logic; only fetches and forwards emails along with attachments.

## Key Tech
- Spring Boot 3 (Java 17) with scheduling enabled (`@EnableScheduling`).
- Spring Data JPA + Hibernate, connecting to the same PostgreSQL database as the backend.
- Jakarta Mail (`jakarta.mail`) for IMAP and message parsing.

## Important Components
- `EmailImportApplication`
  - Main non-web Spring Boot app in `mail-import`.
  - Enables scheduling and wiring of the email import pipeline.

- `application.port.out.EmailFetcher`
  - Port for fetching emails from one or more mailboxes.
  - `fetchUnreadEmails(String folder)` returns a list of `EmailMessage` records.
    - `EmailMessage` includes: `endpointId`, `folder`, `messageId`, `from`, `to`, `subject`, `body`, and attachments.
  - `markAsRead(String endpointId, String messageId, String folder)` marks a single email as read in the appropriate mailbox/folder.

- `adapter.out.email.ImapEmailFetcher`
  - Concrete IMAP implementation of `EmailFetcher`.
  - Resolves IMAP configuration from `integration_endpoints` rows where `type = EMAIL_SOURCE`.
  - Behaviour:
    - Loads **all** `EMAIL_SOURCE` endpoints via the shared JPA repository.
    - For each endpoint:
      - Builds IMAP settings (`host`, `port`, `username`, `password`, `ssl`) from `settings_json`.
      - Uses a per-endpoint `folder` setting when present; falls back to the global `email.import.folder` when missing.
      - Connects via IMAP, searches for unread messages (`SEEN = false`), and emits `EmailMessage` instances with the endpoint id and folder attached.
    - If no `EMAIL_SOURCE` endpoints exist:
      - Falls back to legacy application properties for IMAP settings and the configured `email.import.folder`.
  - `markAsRead`:
    - Uses the `endpointId` and `folder` from `EmailMessage` to reconnect to the correct mailbox.
    - Looks up the `integration_endpoints` row by id, rebuilds the IMAP config, and marks the message as `SEEN`.

- `adapter.out.db.*`
  - Minimal JPA layer for the `integration_endpoints` table.
  - Mirrors the backend’s `IntegrationEndpoint` domain and repository abstractions.

- `emailimport.EmailImportScheduler`
  - Scheduled job that orchestrates fetching and forwarding.
  - Controlled via:
    - `email.import.folder` — default folder name if an endpoint does not specify `folder` in its settings.
    - `email.import.poll-interval-ms` — polling interval (fixed delay).
  - Flow:
    1) Calls `EmailFetcher.fetchUnreadEmails(defaultFolder)` to retrieve unread mails across all endpoints.
    2) For each `EmailMessage`, calls `EmailForwarder.forwardEmail(...)`.
    3) After successful forwarding, calls `EmailFetcher.markAsRead(endpointId, messageId, folder)` to mark messages as read.
  - Wrapped in a read-only transaction to avoid LOB/autocommit issues when reading configuration from PostgreSQL.

- `emailimport.EmailForwarder`
  - Responsible for sending each `EmailMessage` to the backend.
  - Uses `RestTemplate` to call the backend’s internal email-import API.
  - Converts attachments to byte arrays and builds an `EmailImportRequest` DTO.

## Backend Communication
- Property: `backend.api.base-url` (default `http://localhost:8080`).
- Sends `EmailImportRequest` (from/to/subject/body/attachments) as JSON to:
  - `POST {backend.api.base-url}/api/internal/email-import`
    - Backend creates stacks/documents and performs extraction on its side.

## Configuration Expectations
- Mail-import and backend **must** share the same PostgreSQL database:
  - Mail-import reads the `integration_endpoints` table that the backend owns.
- IMAP credentials are configured via the backend admin API into `integration_endpoints` as one or more `EMAIL_SOURCE` endpoints:
  - Each inbox is one row with type `EMAIL_SOURCE` and JSON settings including at least `host`, `port`, `username`, `password`, `ssl`, and typically `folder`.
- Typical deployment:
  - Separate containers: `backend` and `mail-import`, sharing a DB.
  - `mail-import` points at the backend via `backend.api.base-url` (e.g. `http://backend:8080`).

