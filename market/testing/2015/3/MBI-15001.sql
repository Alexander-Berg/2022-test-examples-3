--liquibase formatted sql

--changeset tesseract:MBI-15001_5 endDelimiter:///
delete from market_api.api_client_limits where client_id = 101 and resource_name = 'offer/{}/url' ///
insert into market_api.api_client_limits (client_id, limit_type, resource_name, limit) values (101, 'METHOD', 'offer/{}/url', 40000) ///