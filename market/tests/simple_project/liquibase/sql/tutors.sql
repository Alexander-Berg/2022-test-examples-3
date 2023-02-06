--liquibase formatted sql

--changeset test-user:create-tutors-table
CREATE TABLE tutors(
  first_name VARCHAR(50) NOT NULL,
  last_name VARCHAR(50) NOT NULL
);
