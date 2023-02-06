--liquibase formatted sql

--changeset apershukov:LILUCRM-1423_add-test-email-groups
CREATE TABLE test_email_groups (
  id VARCHAR PRIMARY KEY,
  name VARCHAR NOT NULL
);
