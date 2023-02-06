--liquibase formatted sql

--changeset tesseract:MBI-15357_1 endDelimiter:///
DELETE FROM market_api.api_client_limits
WHERE client_id IN (
  SELECT id
  FROM market_api.api_clients
  WHERE
       id = 1
    OR secret = '1'
) ///

DELETE FROM market_api.api_clients
WHERE
     id = 1
  OR secret = '1' ///


INSERT INTO market_api.api_clients (id, name, secret, url, tariff_id)
VALUES (1, 'Market test client', '1', 'test.market.yandex.ru', 1445341527) ///

INSERT INTO market_api.api_client_limits (client_id, limit_type, resource_name, limit)
SELECT DISTINCT 1, 'METHOD', c.resource_name, 100
FROM market_api.api_client_limits c
WHERE c.limit_type = 'METHOD' ///

INSERT INTO market_api.api_client_limits (client_id, limit_type, resource_name, limit)
VALUES (1, 'GLOBAL', 'GLOBAL', 100) ///

INSERT INTO market_api.api_client_limits (client_id, limit_type, resource_name, limit)
VALUES (1, 'GLOBAL', 'DAILY', 150000) ///