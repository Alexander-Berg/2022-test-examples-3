--liquibase formatted sql

--changeSet nongi:ADVSHOP-434-destination-table-for-testing
CREATE SCHEMA if NOT EXISTS test;
CREATE TABLE if NOT EXISTS test.destination_table
(
    id         bigint    not null,
    first_name varchar   not null,
    synced_at timestamp not null,
    constraint pk_destination_table primary key (id)
);
