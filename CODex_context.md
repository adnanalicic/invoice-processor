# Invoice Processor — Architecture Overview (2025-11-15)

## System Overview
- Invoice processing platform composed of a Spring Boot backend, a headless mail-import microservice, and an Angular frontend.
- Primary responsibilities:
  - Backend: HTTP API, domain logic, persistence, storage integrations.
  - Mail-import: IMAP polling and forwarding of emails to the backend.
  - Frontend: Operator UI for monitoring stacks and configuring integrations.

## Services
- `backend` (Spring Boot 3, Java 17)
  - Owns the domain model (stacks, documents, extractions) and the PostgreSQL schema.
  - Exposes REST APIs consumed by the frontend and by mail-import.
  - Manages integration configuration (email sources, storage targets, etc.) in the database.

- `mail-import` (Spring Boot 3, Java 17)
  - Non-web service with scheduling enabled.
  - Periodically fetches emails from IMAP and forwards them to the backend.
  - Reads integration configuration from the same PostgreSQL database as the backend.

- `frontend` (Angular)
  - Single-page app served separately (typically via a static web server or reverse proxy).
  - Talks to the backend under the `/api` path.
  - Provides admin views for configuring integrations and operational views for working with stacks.

## Data & Infrastructure
- PostgreSQL
  - Shared relational database used by backend and mail-import.
  - Schema owned and migrated by the backend via Flyway (see `backend/src/main/resources/db/migration`).

- Object Storage (MinIO / S3-compatible)
  - Used by the backend for storing email bodies and attachments.
  - Configuration is stored as integration endpoints in the database; concrete details are documented in `backend/CODex_context.md`.

## Configuration Model
- All external integrations (email sources, storage targets, etc.) are modeled as rows in the `integration_endpoints` table.
- Backend exposes admin APIs for managing these endpoints; mail-import and other components consume them.
- Detailed semantics (endpoint types, settings keys, constraints) are described in:
  - `backend/CODex_context.md` — backend integration and domain details.
  - `mail-import/CODex_context.md` — mail-import scheduling and IMAP behaviour.
  - `frontend/CODex_context.md` — admin UI behaviour and how it maps to backend APIs.

## Where to Look for Details
- For backend behaviour (endpoints, use cases, storage, integration rules), see `backend/CODex_context.md`.
- For mail-import behaviour (IMAP polling, multi-inbox handling, error handling), see `mail-import/CODex_context.md`.
- For admin UI behaviour (how integrations are edited, including multiple email inboxes), see `frontend/CODex_context.md` and the Angular components under `frontend/src/app/components`.

