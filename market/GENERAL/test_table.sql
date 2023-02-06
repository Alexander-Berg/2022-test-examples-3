--liquibase formatted sql

--changeset v-lozhnikov:MARKETBOOTCAMP-252-test-table
CREATE TABLE IF NOT EXISTS test_table
(
    id   BIGINT NOT NULL PRIMARY KEY,
    name TEXT
);
