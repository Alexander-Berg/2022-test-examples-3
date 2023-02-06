--liquibase formatted sql

--changeset saferif:MBI-16813 endDelimiter:;
UPDATE SHOPS_WEB.NN_ALIAS SET EMAIL = 'testmarket@yamoney.ru' WHERE ALIAS = 'MarketPaymentYaMoney';