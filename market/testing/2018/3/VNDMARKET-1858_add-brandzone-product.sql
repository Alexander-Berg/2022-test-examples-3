--liquibase formatted sql
--changeset zaharov-i:VNDMARKET-1858_add-brandzone-product-testing
INSERT INTO VENDORS.PRODUCT (ID, NAME, BALANCE_ID)
    VALUES (3, 'Бренд-зона', 60509037);
