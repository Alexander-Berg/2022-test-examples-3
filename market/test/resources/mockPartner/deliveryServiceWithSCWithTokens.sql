INSERT INTO partner (id, type, name, url, token)
VALUES (100500, 'DELIVERY', 'Беру доставка', NULL, 'ds_token');

INSERT INTO partner (id, type, name, url, token, latitude, longitude, zone_offset, start_time, end_time)
VALUES (100501, 'MARKET_SORTING_CENTER', 'Беру СЦ', NULL, 'sc_token', 55.741526, 37.71698, 3, '09:00:00', '23:00:00');

insert into partner_mapping (delivery_service_id, sorting_center_id)
values (100500, 100501);
