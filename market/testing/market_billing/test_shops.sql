--liquibase formatted sql

--changeset skiftcha:MBI-19214-table
CREATE TABLE market_billing.test_shops
(
  shop_id NUMBER NOT NULL
);