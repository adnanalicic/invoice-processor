# Frontend — Codex Context (2025-11-15)

## Role
- Angular single-page application providing:
  - Operator UI for browsing and inspecting invoice stacks/documents.
  - Admin UI for configuring integration endpoints (email sources, storage target).

## Tech & Entry Points
- Angular application (standalone components) built against the backend’s REST API.
- All HTTP calls are centralized in `src/app/services/api.service.ts`:
  - Base URL: `/api` (typically proxied by a reverse proxy to the backend service).

## Key Components
- `src/app/services/api.service.ts`
  - Wraps backend endpoints for:
    - Stacks, documents, manual uploads, re-extraction.
    - Integration endpoints: `getIntegrationEndpoints`, `saveIntegrationEndpoint`.
    - Email sources (multi-inbox): `getEmailSources`, `createEmailSource`, `updateEmailSource`, `deleteEmailSource`.

- `src/app/components/admin/admin-page.component.ts`
  - Standalone component providing the admin UI for:
    - Configuring one `STORAGE_TARGET` endpoint (storage settings).
    - Configuring multiple `EMAIL_SOURCE` endpoints (email inboxes / folders).
  - Email configuration behaviour:
    - Displays a selector labelled **“Email folders”**.
    - Each entry corresponds to an `EMAIL_SOURCE` endpoint in `integration_endpoints`.
    - The label for each option is the endpoint’s `folder` setting (or `INBOX` by default).
    - Users can:
      - Select an existing folder (inbox) to edit its IMAP settings.
      - Create a **New folder...** entry, which maps to creating a new `EMAIL_SOURCE` endpoint.
      - Delete a selected folder, which deletes the corresponding endpoint via the `/api/admin/email-sources/{id}` API.
    - The form fields map directly to the `settings` JSON for `EMAIL_SOURCE`:
      - `host`, `port`, `username`, `password`, `folder`, `ssl`.

## Configuration Flow
- Storage target:
  - Admin configures endpoint via the “Storage Target Settings” form.
  - Form submission calls `saveIntegrationEndpoint('STORAGE_TARGET', {...})`.
  - Backend persists the configuration as a `STORAGE_TARGET` row in `integration_endpoints`.

- Email sources (multi-inbox):
  - Admin uses the email section to manage multiple inboxes.
  - On load:
    - Frontend calls `getIntegrationEndpoints()` to populate storage settings.
    - Calls `getEmailSources()` to load all `EMAIL_SOURCE` endpoints.
  - For each inbox:
    - The component populates the email form from the endpoint’s `settings`.
    - Save:
      - If an endpoint id is selected, uses `updateEmailSource(id, payload)`.
      - If none is selected, uses `createEmailSource(payload)` to create a new row.
    - Delete:
      - Uses `deleteEmailSource(id)` and refreshes the list.

## URLs & Deployment Notes
- Frontend is typically served on its own origin (e.g. `http://localhost:4200` in dev).
- All API calls are made to `/api/...`; in production, this is usually routed to the backend service.

