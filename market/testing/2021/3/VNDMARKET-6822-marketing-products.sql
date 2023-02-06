--liquibase formatted sql

--changeset mikelevin:VNDMARKET-6822-add-new-marketing-products

INSERT INTO VENDORS.PRODUCT (ID, NAME, BALANCE_ID)
VALUES (14, 'Маркетинговые услуги (ТВ)', 512812);
INSERT INTO VENDORS.PRODUCT (ID, NAME, BALANCE_ID)
VALUES (15, 'Маркетинговые услуги (Внешние площадки)', 512811);
