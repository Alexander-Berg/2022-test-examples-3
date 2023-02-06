--liquibase formatted sql

--changeset zaharov-i:VNDMARKET-5623_marketplace_modelbid_product_test
UPDATE VENDORS.PRODUCT SET BALANCE_ID = 10000025 WHERE ID = 12;
