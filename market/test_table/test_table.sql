--liquibase formatted sql

--changeset inozemcevns:temp_test_table
CREATE TEMPORARY TABLE IF NOT EXISTS test_table
(
    id   BIGINT NOT NULL PRIMARY KEY,
    name TEXT
);
