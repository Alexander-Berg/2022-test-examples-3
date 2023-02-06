--liquibase formatted sql

--changeset pochemuto:MBO-20368-jooq_test_entity
create table mbo_category.jooq_test_entity (
    id          bigserial primary key,
    description text,
    modified_date timestamp
);

--changeset pochemuto:MBO-22424-add-date-range-test-fields
alter table mbo_category.jooq_test_entity add from_date date;
alter table mbo_category.jooq_test_entity add to_date date;

--changeset s-ermakov:MBO-25345-add-timestamp-entries
alter table mbo_category.jooq_test_entity add created_at timestamp;
alter table mbo_category.jooq_test_entity add created_at_tz timestamp with time zone;
