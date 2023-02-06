--liquibase formatted sql

--changeset s-myachenkov:test-ora
CREATE SCHEMA if not exists shops_web;
CREATE SCHEMA if not exists market_billing;
