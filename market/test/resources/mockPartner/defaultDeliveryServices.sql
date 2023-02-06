INSERT INTO partner (id, type, name, url, token)
VALUES (198, 'DELIVERY', 'Маркет ПВЗ', NULL, NULL);

INSERT INTO partner (id, type, name, url, token)
VALUES (239, 'DELIVERY', 'Маркет Курьер', 'https://tpl-int.tst.vs.market.yandex.net/delivery/query-gateway', '');

INSERT INTO partner (id, type, name, url, token, latitude, longitude, start_time, end_time)
VALUES (1, 'MARKET_SORTING_CENTER', 'Маркет ПВЗ', NULL, NULL, 12.34, 56.78, '09:00', '18:00');

INSERT INTO partner (id, type, name, url, token, latitude, longitude)
VALUES (47819, 'MARKET_SORTING_CENTER', 'Маркет ПВЗ', NULL, NULL, 12.34, 56.78);

insert into partner_mapping
(delivery_service_id, sorting_center_id)
values
(198, 1),
(239, 47819)

