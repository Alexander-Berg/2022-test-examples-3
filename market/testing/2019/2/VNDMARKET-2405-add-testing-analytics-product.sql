--liquibase formatted sql

--changeset rpanasenkov:VNDMARKET-2405-add-testing-analytics-product
UPDATE VENDORS.PRODUCT SET BALANCE_ID = 509758 WHERE ID = 4;