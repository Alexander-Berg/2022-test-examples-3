--liquibase formatted sql

--changeset apershukov:add_offsets_table
CREATE TABLE offsets
(
    consumer_id VARCHAR NOT NULL,
    partition   VARCHAR NOT NULL,
    value       BIGINT  NOT NULL,
    PRIMARY KEY (consumer_id, partition)
);
