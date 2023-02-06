--liquibase formatted sql

--changeset vivg:LILUCRM-506-test_devices_table
CREATE TABLE test_devices (
    id_type  VARCHAR NOT NULL,
    id_value VARCHAR NOT NULL,
    name     VARCHAR,
    selected BOOLEAN NOT NULL,

    PRIMARY KEY (id_type, id_value)
);
