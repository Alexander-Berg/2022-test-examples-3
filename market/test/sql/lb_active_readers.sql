--liquibase formatted sql

--changeset apershukov:add_active_instances_table
CREATE TABLE lb_active_readers
(
    dc           VARCHAR NOT NULL,
    last_load_ts TIMESTAMP,
    PRIMARY KEY (dc)
);
