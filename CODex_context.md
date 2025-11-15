# Invoice Processor — Critical Context (2025-11-15)

## Architecture & Config
- Spring Boot backend (Java 17), Angular frontend, PostgreSQL, MinIO (S3-compatible).
- External integrations are configured via the `integration_endpoints` table (`EMAIL_SOURCE`, `STORAGE_TARGET`), not `application.yml`.

## Email Source (`ImapEmailFetcher`)
- Loads IMAP settings (`host`, `port`, `username`, `password`, `ssl`) from `integration_endpoints` where `type = EMAIL_SOURCE`.
- Falls back to legacy properties only if the DB row is missing.
- Email folder is configured via the `email.import.folder` property.

## Storage (`S3StorageService`)
- Uses `integration_endpoints` entry with `type = STORAGE_TARGET`; no property-based fallback.
- Required JSON keys in `settings_json`: `endpoint`, `accessKey`, `secretKey`, `bucket`, `region`; optional `forcePathStyle`.
- Missing or invalid storage config fails fast; storage bucket is created on first use.

## Manual Uploads
- `CreateManualStackUseCase` stores email body and attachments through `StorageService`.
- Manual uploads require a valid `STORAGE_TARGET` endpoint configuration.

## Operational Pitfalls
- Seed `integration_endpoints` via `/api/admin/endpoints/EMAIL_SOURCE` and `/api/admin/endpoints/STORAGE_TARGET` before using email or storage features.
- For MinIO, use a full endpoint URI (e.g. `http://localhost:9000`) and set `"forcePathStyle": "true"`.
