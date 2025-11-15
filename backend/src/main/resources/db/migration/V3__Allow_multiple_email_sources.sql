-- Allow multiple EMAIL_SOURCE endpoints while keeping other types unique
ALTER TABLE integration_endpoints
    DROP CONSTRAINT IF EXISTS integration_endpoints_type_key;

CREATE UNIQUE INDEX IF NOT EXISTS integration_endpoints_type_unique_non_email_source
    ON integration_endpoints(type)
    WHERE type <> 'EMAIL_SOURCE';

