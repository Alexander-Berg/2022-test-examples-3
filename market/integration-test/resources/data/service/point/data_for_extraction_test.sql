INSERT INTO partner
    (id, name, status, type, market_id)
VALUES (3, 'delivery', 'active', 'DELIVERY', 20),
       (4, 'delivery', 'active', 'DELIVERY', 30);

INSERT INTO address (id, location_id, country, latitude, longitude, settlement, post_code, street, house, housing,
                     building,
                     apartment, comment, address_string, short_address_string, region)
VALUES (1, 213, 'Россия', 56.948048, 24.107018, '', '1005', 'Уриекстес', '14а', '', '', '', 'SIA “ILIOR”',
        '1005, Рига, Уриекстес, 14а', 'Уриекстес, 14а', 'Москва и Московская область');

INSERT INTO contact
    (id, name)
VALUES (1, 'contact');

INSERT INTO schedule
    (id)
VALUES (1);

INSERT INTO schedule_day
    (id, schedule_id, day, time_from, time_to, is_main)
VALUES (1, 1, 1, '12:00', '14:00', TRUE),
       (2, 1, 2, '13:00', '15:00', TRUE),
       (3, 1, 2, '14:00', '16:00', TRUE);

INSERT INTO logistics_point
(id, type, partner_id, external_id, address_id, contact_id, schedule_id, active)
VALUES (1, 'WAREHOUSE', NULL, 1, 1, 1, 1, TRUE),
       (2, 'WAREHOUSE', NULL, 2, 1, 1, 1, TRUE),
       (3, 'WAREHOUSE', NULL, 3, 1, 1, 1, TRUE),
       (4, 'WAREHOUSE', NULL, 3, 1, NULL, 1, TRUE),
       (5, 'WAREHOUSE', NULL, 3, 1, 1, 1, TRUE),
       (6, 'WAREHOUSE', NULL, 3, 1, NULL, 1, TRUE),
       (7, 'WAREHOUSE', NULL, 3, 1, 1, 1, TRUE),
       (8, 'WAREHOUSE', NULL, 3, 1, 1, 1, TRUE),
       (9, 'WAREHOUSE', NULL, 3, 1, 1, 1, TRUE);

INSERT INTO put_reference_warehouse_in_delivery_status
    (partner_id, warehouse_id, status)
VALUES (3, 1, 'CREATED'),
       (3, 2, 'CREATED'),
       (3, 3, 'NEW'),
       (3, 4, 'NEW'),
       (3, 5, 'NEW'),
       (3, 6, 'NEW'),
       (3, 7, 'NEW'),
       (3, 8, 'ERROR'),
       (3, 9, 'NEW'),
       (4, 1, 'NEW'),
       (4, 2, 'NEW'),
       (4, 3, 'NEW'),
       (4, 4, 'PREPARED'),
       (4, 5, 'NEW'),
       (4, 6, 'NEW'),
       (4, 7, 'CREATING');

INSERT INTO phone
    (id, number, type, logistics_point_id)
VALUES (1, '999-999-99-91', 0, 1),
       (2, '999-999-99-92', 0, 2),
       (3, '999-999-99-93', 0, 3),
       (4, '999-999-99-94', 0, 4);
