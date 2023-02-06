--liquibase formatted sql

--changeset apershukov:LILUCRM-1423_add-test-push-groups
CREATE TABLE test_device_groups (
  id VARCHAR PRIMARY KEY,
  name VARCHAR NOT NULL
);
