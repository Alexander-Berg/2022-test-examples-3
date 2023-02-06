--liquibase formatted sql

--changeset snoop:2015.3.20 endDelimiter:;
REVOKE DELETE ON shops_web.test_generation FROM SYSTEM;