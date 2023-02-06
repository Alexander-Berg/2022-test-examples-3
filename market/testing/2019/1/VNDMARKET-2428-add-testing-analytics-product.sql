--liquibase formatted sql

--changeset fbokovikov:VNDMARKET-2428-add-testing-analytics-product
insert into vendors.product(id, name, balance_id)
values (4, 'Маркет.Аналитика', 508643);