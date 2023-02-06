--liquibase formatted sql

--changeset andreybosy:tmp_test-table
CREATE TABLE IF NOT EXISTS tmp_test
(
    id   BIGINT NOT NULL PRIMARY KEY,
    name TEXT
);
