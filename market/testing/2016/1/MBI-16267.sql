--liquibase formatted sql

--changeset tesseract:MBI-16267_testing endDelimiter:///
DELETE FROM market_api.api_client_limits where client_id = 6635 ///
DELETE FROM market_api.api_clients where id = 6635 ///

INSERT INTO market_api.api_clients (id, name, email, secret) VALUES (6635, 'Clothes mobile', 'email@example.com', 'J5ApdUiFU3jZJDxKHUw6GJeRfh1xI3') ///