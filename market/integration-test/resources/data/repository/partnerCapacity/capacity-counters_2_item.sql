INSERT INTO partner_capacity (capacity_id, partner_id, location_from_id, location_to_id, delivery_type,
                              platform_client_id, value, capacity_type, day, counting_type)
VALUES (1, 2, 1, 117065, NULL, 1, 2, 'REGULAR', NULL, 'ITEM'),
       (2, 2, 1, 213, NULL, 1, 2, 'REGULAR', NULL, 'ITEM'),
       (3, 2, 1, 1, NULL, 1, 2, 'REGULAR', NULL, 'ITEM'),
       (4, 2, 1, 225, NULL, 1, 2, 'REGULAR', NULL, 'ITEM');

INSERT INTO capacity_counter (capacity_id, day, parcel_count)
VALUES (1, '2019-01-01', 2),
       (2, '2019-01-01', 2),
       (3, '2019-01-01', 2),
       (4, '2019-01-01', 2);

INSERT INTO order_to_ship(id, platform_client_id, partner_id, location_from_id, location_to_id, delivery_type,
                          shipment_day, status, processed)
VALUES ('1', 1, 2, 1, 20482, 'DELIVERY', '2019-01-01', 'CREATED', true),
       ('2', 1, 2, 1, 20482, 'DELIVERY', '2019-01-01', 'CREATED', true),
       ('3', 1, 2, 1, 20482, 'DELIVERY', '2019-01-01', 'CREATED', true),
       ('3', 1, 2, 1, 20482, 'DELIVERY', '2019-01-01', 'CANCELLED', true);

INSERT INTO order_to_ship_value(counting_type, value, order_to_ship_id, platform_client_id, partner_id, status)
VALUES ('ORDER', 1, '1', 1, 2, 'CREATED'),
       ('ITEM', 25, '1', 1, 2, 'CREATED'),
       ('ORDER', 1, '2', 1, 2, 'CREATED'),
       ('ITEM', 11, '2', 1, 2, 'CREATED'),
       ('ORDER', 1, '3', 1, 2, 'CREATED'),
       ('ITEM', 11, '3', 1, 2, 'CREATED');
