--liquibase formatted sql

--changeset evgeniy-popov:create_tmp_table
create temp table IF NOT EXISTS tmp
(
    id int not null primary key
);
