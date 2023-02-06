--liquibase formatted sql
--changeset katejud:pharma_test_shop_order
CREATE TABLE IF NOT EXISTS pharma_test_shop_order
(
    id   BIGINT NOT NULL PRIMARY KEY,
    status VARCHAR(30) NOT NULL ,
    substatus VARCHAR(30) NOT NULL ,
    delivery_type VARCHAR(30) NOT NULL ,
    json_data VARCHAR(10000) NOT NULL
);

--changeset katejud:add_column_shop_id
ALTER TABLE pharma_test_shop_order
    ADD COLUMN shop_id int8;
