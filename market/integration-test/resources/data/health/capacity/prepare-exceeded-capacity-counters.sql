INSERT INTO partner_capacity (capacity_id, partner_id, location_from_id, location_to_id, delivery_type,
                              platform_client_id, value, capacity_type, day)
VALUES (1, 133, 1, 213,     NULL, 1, 0, 'REGULAR', NULL),
       (2, 133, 1, 20279,   NULL, 1, 1, 'REGULAR', NULL),
       (3, 133, 1, 216,     NULL, 1, 1, 'RESERVE', NULL),
       (4, 133, 1, 216,     NULL, 1, 1, 'RESERVE', '2019-06-20'),
       (5, 133, 1, 117065,  NULL, 1, 4, 'REGULAR', NULL),
       (6, 666, 1, 225,     NULL, 1, 2, 'REGULAR', NULL),
       (7, 145, 1, 216,     NULL, 1, 10, 'REGULAR', NULL);

INSERT INTO capacity_counter (id, capacity_id, day, parcel_count)
VALUES (1, 1, '2019-06-20', 300),
       (2, 2, '2019-06-21', 2),
       (3, 3, '2019-06-20', 1),
       (4, 4, '2019-06-19', 1),
       (5, 5, '2019-06-20', 2),
       (6, 6, '2019-05-20', 1),
       (7, 7, '2018-06-20', 10);

INSERT INTO order_to_ship (id, platform_client_id, partner_id, location_from_id, location_to_id,
                           delivery_type, shipment_day, status, processed)
VALUES ('1', 1, 133, 1, 117065, 'POST', '2019-06-20', 'CREATED', true),
       ('1', 1, 145, 1, 216, 'POST', '2019-06-20', 'CREATED', true),
       ('2', 1, 133, 1, 117065, 'POST', '2019-06-20', 'CREATED', true),
       ('3', 1, 133, 1, 216,    'POST', '2019-06-20', 'CREATED', true);


