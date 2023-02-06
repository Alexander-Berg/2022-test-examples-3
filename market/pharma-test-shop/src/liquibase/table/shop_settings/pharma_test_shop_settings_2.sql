--liquibase formatted sql
--changeset baktybekdosku:add_column_outlet_ids
alter table pharma_test_shop_settings add column outlet_ids varchar not null default '';
