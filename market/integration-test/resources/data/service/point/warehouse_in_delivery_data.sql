INSERT INTO partner
    (id, name, status, type, market_id)
VALUES (1, 'fulfillment', 'active', 'FULFILLMENT', 10),
       (2, 'delivery', 'active', 'DELIVERY', 20),
       (3, 'sorting_center', 'active', 'SORTING_CENTER', 30),
       (4, 'fulfillment1', 'active', 'FULFILLMENT', 40),
       (5, 'delivery2', 'active', 'DELIVERY', 50),
       (6, 'delivery3', 'active', 'DELIVERY', 60),
       (7, 'delivery4', 'active', 'DELIVERY', 70),
       (8, 'delivery5', 'inactive', 'DELIVERY', 80);

INSERT INTO private.settings_api
    (id, api_type, partner_id, token, format, version)
VALUES (1, 'delivery', 2, 'token', 'JSON', '1.0'),
       (2, 'delivery', 5, 'token2', 'JSON', '1.0'),
       (3, 'delivery', 6, 'token3', 'JSON', '1.0'),
       (4, 'delivery', 8, 'token5', 'JSON', '1.0');

INSERT INTO settings_method
    (id, settings_api_id, method, active, url)
VALUES (1, 1, 'putReferenceWarehouses', TRUE, 'testurl'),
       (2, 2, 'putReferenceWarehouses', TRUE, 'testurl2'),
       (3, 3, 'putReferenceWarehouses', FALSE, 'testurl3'),
       (4, 4, 'putReferenceWarehouses', TRUE, 'testurl3');

INSERT INTO address
    (id, post_code)
VALUES (1, '123123');

INSERT INTO contact
    (id, name)
VALUES (1, 'contact');

INSERT INTO schedule
    (id)
VALUES (1);

INSERT INTO schedule_day
    (id, schedule_id, day, time_from, time_to)
VALUES (1, 1, 1, '12:00', '14:00'),
       (2, 1, 2, '13:00', '15:00'),
       (3, 1, 3, '14:00', '16:00'),
       (4, 1, 4, '15:00', '17:00'),
       (5, 1, 5, '16:00', '18:00'),
       (6, 1, 6, '17:00', '19:00');

INSERT INTO logistics_point
    (id, type, partner_id, external_id, address_id, active)
VALUES (1, 'WAREHOUSE', 1, 1, 1, TRUE),
       (2, 'WAREHOUSE', 2, 1, 1, TRUE),
       (3, 'WAREHOUSE', NULL, 1, 1, TRUE),
       (4, 'WAREHOUSE', 4, 1, 1, TRUE),
       (5, 'PICKUP_POINT', 2, 1, 1, TRUE),
       (6, 'WAREHOUSE', NULL, 1, 1, FALSE);

INSERT INTO put_reference_warehouse_in_delivery_status
    (partner_id, warehouse_id, status)
VALUES (2, 1, 'CREATED');
