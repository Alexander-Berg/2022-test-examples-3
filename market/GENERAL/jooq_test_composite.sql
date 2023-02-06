--liquibase formatted sql

--changeset s-ermakov:DEEPMIND-519-jooq_test_composite
create table mbo_category.jooq_test_composite (
    supplier_id int,
    shop_sku text,
    description text,
    modified_date timestamp,
    PRIMARY KEY(supplier_id, shop_sku)
);
