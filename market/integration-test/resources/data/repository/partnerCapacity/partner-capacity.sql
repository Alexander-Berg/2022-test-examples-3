INSERT INTO partner_capacity (capacity_id, partner_id, location_from_id, location_to_id, delivery_type,
                              platform_client_id, value, capacity_type, day)
VALUES (1, 133, 1, 213, NULL, 1, 1, 'REGULAR', NULL),
       (2, 133, 1, 225, NULL, 1, 2, 'REGULAR', NULL),
       (3, 105, 1, 213, NULL, 1, 1, 'REGULAR', NULL),
       (4, 105, 1, 225, NULL, 1, 2, 'REGULAR', NULL);

-- Crossdock capacity
INSERT INTO partner_capacity (capacity_id, partner_id, location_from_id, location_to_id, delivery_type,
                              platform_client_id, value, capacity_type, counting_type, service_type, day)
VALUES (5, 105, 225, 225, NULL, 1, 1, 'REGULAR', 'ITEM', 'INBOUND', NULL),
       (6, 47778, 225, 225, NULL, 1, 1, 'REGULAR', 'ITEM', 'SHIPMENT', NULL);

INSERT INTO capacity_counter_notification (capacity_counter_id, is_50_percent_notification_send,
                                           is_90_percent_notification_send)
VALUES (1, TRUE, TRUE),
       (2, TRUE, TRUE),
       (3, TRUE, TRUE),
       (4, TRUE, TRUE);
