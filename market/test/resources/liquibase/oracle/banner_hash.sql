--liquibase formatted sql

--changeset stani:test-ora
create table shops_web.banner_hash
(
  banner_id varchar(256) not null,
  hash_id bigint not null
);
