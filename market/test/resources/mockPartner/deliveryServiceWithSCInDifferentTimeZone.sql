INSERT INTO partner_schedule (id)
VALUES (1);

INSERT INTO partner_schedule_interval (schedule_id, time_from, time_to, is_user_available)
VALUES (1, '09:00', '22:00', true),
       (1, '09:00', '14:00', true),
       (1, '14:00', '18:00', true),
       (1, '18:00', '22:00', true),
       (1, '09:00', '12:00', false),
       (1, '12:00', '14:00', false);

INSERT INTO partner (id, type, name, url, token, schedule_id)
VALUES (100500, 'DELIVERY', 'Беру доставка', NULL, NULL, 1);

INSERT INTO partner (id, type, name, url, token, latitude, longitude, zone_offset, start_time, end_time)
VALUES (100501, 'MARKET_SORTING_CENTER', 'Беру СЦ', NULL, NULL, 55.741526, 37.71698, 4, '09:00:00', '23:00:00');

insert into partner_mapping (delivery_service_id, sorting_center_id)
values (100500, 100501);
