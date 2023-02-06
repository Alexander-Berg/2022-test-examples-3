INSERT INTO partner_capacity (capacity_id, partner_id, location_from_id, location_to_id, delivery_type,
                              platform_client_id, value, capacity_type, day)
VALUES (1, 145, 1, 225, null, 1, 3, 'REGULAR', NULL),
       (2, 145, 1, 213, null, 1, 3, 'REGULAR', NULL);

INSERT INTO capacity_counter (capacity_id, day, parcel_count, is_day_off_created)
VALUES (1, '2019-01-01', 3, true),
       (2, '2019-01-01', 3, true);

INSERT INTO order_to_ship (id, partner_id, platform_client_id, location_from_id, location_to_id, delivery_type, shipment_day, processed, status)
VALUES ('1', 145, 1, 1, 213, 'DELIVERY', '2019-01-01', true, 'CREATED'),
       ('2', 145, 1, 1, 213, 'DELIVERY', '2019-01-01', true, 'CREATED'),
       ('3', 145, 1, 1, 213, 'DELIVERY', '2019-01-01', true, 'CREATED'),
       ('4', 145, 1, 1, 213, 'DELIVERY', '2019-01-01', FALSE, 'CREATED'),
       ('5', 146, 1, 1, 213, 'DELIVERY', '2019-01-01', true, 'CREATED'),
       ('6', 147, 1, 1, 213, 'DELIVERY', '2019-01-01', true, 'CREATED');

INSERT INTO capacity_counter_notification (capacity_counter_id, is_50_percent_notification_send,
                                           is_90_percent_notification_send)
VALUES (1, TRUE, TRUE),
       (2, TRUE, TRUE);
