--liquibase formatted sql

--changeset test-user:create-students-table
CREATE TABLE student(
  first_name VARCHAR(50) NOT NULL,
  last_name VARCHAR(50) NOT NULL
);
