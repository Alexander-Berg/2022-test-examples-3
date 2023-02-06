INSERT INTO partner_capacity (capacity_id, partner_id, location_from_id, location_to_id, delivery_type,
                              platform_client_id, value, capacity_type, day)
VALUES (1, 2, 1, 117065, NULL, 1, 3, 'REGULAR', NULL),
       (2, 2, 1, 20279, NULL, 1, 3, 'REGULAR', NULL);


INSERT INTO capacity_counter (capacity_id, day, parcel_count, max_allowed_parcel_count)
VALUES (1, '2019-01-01', 2, 3),
       (1, '2019-01-02', 3, 3),
       (2, '2019-01-01', 2, 3),
       (2, '2019-01-02', 3, 3),
       (2, '2019-01-03', 4, 3);


INSERT INTO order_to_ship(id, platform_client_id, partner_id, location_from_id, location_to_id, delivery_type,
                          shipment_day, status, processed)
VALUES
       -- capacity 1
       ('10', 1, 2, 1, 214, 'DELIVERY', '2019-01-01', 'CREATED', true),
       ('11', 1, 2, 1, 214, 'DELIVERY', '2019-01-01', 'CREATED', true),

       -- capacity 2
       ('1', 1, 2, 1, 20279, 'DELIVERY', '2019-01-01', 'CREATED', true),
       ('2', 1, 2, 1, 20279, 'DELIVERY', '2019-01-01', 'CREATED', true),

       ('3', 1, 2, 1, 20279, 'DELIVERY', '2019-01-02', 'CREATED', true),
       ('4', 1, 2, 1, 20279, 'DELIVERY', '2019-01-02', 'CREATED', true),
       ('5', 1, 2, 1, 20279, 'DELIVERY', '2019-01-02', 'CREATED', true),

       ('6', 1, 2, 1, 20279, 'DELIVERY', '2019-01-03', 'CREATED', true),
       ('7', 1, 2, 1, 20279, 'DELIVERY', '2019-01-03', 'CREATED', true),
       ('8', 1, 2, 1, 20279, 'DELIVERY', '2019-01-03', 'CREATED', true),
       ('9', 1, 2, 1, 20279, 'DELIVERY', '2019-01-03', 'CREATED', true),

       -- capacity 3 (will be imported from LMS on sync)
       ('21', 1, 2, 1, 216, 'DELIVERY', '2019-01-01', 'CREATED', true),
       ('22', 1, 2, 1, 216, 'DELIVERY', '2019-01-01', 'CREATED', true),
       ('23', 1, 2, 1, 216, 'DELIVERY', '2019-01-01', 'CREATED', true)

       ;