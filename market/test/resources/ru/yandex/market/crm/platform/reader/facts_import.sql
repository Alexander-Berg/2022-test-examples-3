--liquibase formatted sql

--changeset wanderer25:LILUCRM-1516 _add_facts_export_table
CREATE TABLE IF NOT EXISTS facts_import (
    import_id       VARCHAR PRIMARY KEY,
    import_context  JSONB NOT NULL,
    status_code     VARCHAR,
    status_message  VARCHAR
);
