--liquibase formatted sql

--changeSet nongi:ADVSHOP-434-source-table-for-testing
CREATE SCHEMA if NOT EXISTS test;
CREATE TABLE if NOT EXISTS test.source_table
(
    id         bigint    not null,
    first_name varchar,
    age        integer,
    constraint pk_source_table primary key (id)
);
