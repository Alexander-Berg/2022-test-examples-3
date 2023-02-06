INSERT INTO partner_capacity (capacity_id, partner_id, location_from_id, location_to_id, delivery_type,
                              platform_client_id, value, capacity_type, day)
VALUES (1, 2, 1, 117065, NULL, 1, 2, 'REGULAR', NULL),
       (2, 2, 1, 213, NULL, 1, 2, 'REGULAR', NULL),
       (3, 2, 1, 1, NULL, 1, 2, 'REGULAR', NULL),
       (4, 2, 1, 225, NULL, 1, 2, 'REGULAR', NULL);

INSERT INTO capacity_counter (capacity_id, day, parcel_count, is_day_off_created)
VALUES (1, '2019-01-01', 2, false),
       (2, '2019-01-01', 2, false),
       (3, '2019-01-01', 2, false),
       (4, '2019-01-01', 2, false);

INSERT INTO order_to_ship(id, platform_client_id, partner_id, location_from_id, location_to_id, delivery_type,
                          shipment_day, status, processed)
VALUES ('1', 1, 2, 1, 20482, 'DELIVERY', '2019-01-01', 'CREATED', true),
       ('2', 1, 2, 1, 20482, 'DELIVERY', '2019-01-01', 'CREATED', true);
