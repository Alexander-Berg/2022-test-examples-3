--liquibase formatted sql

--changeset markin-nn:MARKETCHECKOUT-10025 runAlways=true context=testing
--comment: Защита от переналивки баланса в тестинге
delete
from service_product_cache;
