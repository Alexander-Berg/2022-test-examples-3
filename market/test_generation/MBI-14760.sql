--liquibase formatted sql

--changeset tesseract:MBI-14760 endDelimiter:/
ALTER TABLE SHOPS_WEB.TEST_GENERATION ADD (
  TYPE NUMBER(1) DEFAULT 0
)
/