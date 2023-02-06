INSERT INTO partner_capacity (capacity_id, partner_id, location_from_id, location_to_id, delivery_type,
                              platform_client_id, value, capacity_type, day, counting_type)
VALUES (1, 133, 1, 213,     NULL, 1, 1, 'REGULAR', NULL, 'ITEM'),
       (2, 133, 1, 20279,   NULL, 1, 1, 'REGULAR', NULL, 'ITEM'),
       (3, 133, 1, 216,     NULL, 1, 1, 'RESERVE', NULL, 'ITEM'),
       (4, 133, 1, 216,     NULL, 1, 1, 'RESERVE', '2019-06-20', 'ITEM'),
       (5, 133, 1, 117065,  NULL, 1, 4, 'REGULAR', NULL, 'ORDER'),
       (6, 666, 1, 225,     NULL, 1, 2, 'REGULAR', NULL, 'ITEM'),
       (7, 145, 1, 216,     NULL, 1, 5, 'REGULAR', NULL, 'ITEM');

INSERT INTO capacity_counter (id, capacity_id, day, parcel_count, is_day_off_created)
VALUES (1, 1, '2019-06-20', 43, true),
       (2, 2, '2019-06-20', 22, true),
       (3, 3, '2019-06-20', 1, true),
       (4, 4, '2019-06-20', 1, true),
       (5, 5, '2019-06-20', 14, true),
       (6, 6, '2019-06-20', 1, false),
       (7, 7, '2019-06-20', 10, true);

INSERT INTO order_to_ship (id, platform_client_id, partner_id, location_from_id, location_to_id,
                              delivery_type, shipment_day, status, processed)
VALUES ('1', 1, 133, 1, 117065, 'POST', '2019-06-20', 'CREATED', true),
       ('1', 1, 145, 1, 216, 'POST', '2019-06-20', 'CREATED', true),
       ('2', 1, 133, 1, 117065, 'POST', '2019-06-20', 'CREATED', true),
       ('3', 1, 133, 1, 216,    'POST', '2019-06-20', 'CREATED', true);

INSERT INTO order_to_ship_value(counting_type, value, order_to_ship_id, platform_client_id, partner_id, status)
VALUES ('ORDER', 1, '1', 1, 133, 'CREATED'),
       ('ITEM', 10, '1', 1, 133, 'CREATED'),
       ('ORDER', 1, '1', 1, 145, 'CREATED'),
       ('ITEM', 12, '1', 1, 145, 'CREATED'),
       ('ORDER', 1, '2', 1, 133, 'CREATED'),
       ('ITEM', 3, '2', 1, 133, 'CREATED'),
       ('ORDER', 1, '3', 1, 133, 'CREATED'),
       ('ITEM', 8, '3', 1, 133, 'CREATED');
