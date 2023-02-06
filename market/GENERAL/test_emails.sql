--liquibase formatted sql

--changeset apershukov:LILUCRM-356-test_emails_table
CREATE TABLE test_emails (
    email VARCHAR PRIMARY KEY,
    selected BOOLEAN
);
