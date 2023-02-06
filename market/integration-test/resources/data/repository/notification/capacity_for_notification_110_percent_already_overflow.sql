INSERT INTO partner_capacity (capacity_id, partner_id, location_from_id, location_to_id, delivery_type,
                              platform_client_id, value, capacity_type, day, counting_type, service_type)
VALUES (1, 145, 1, 225, NULL, 1, 100, 'REGULAR', NULL, 'ORDER', 'INBOUND'),
       (2, 145, 1, 213, NULL, 1, 100, 'REGULAR', NULL, 'ORDER', 'SHIPMENT');

INSERT INTO capacity_counter (capacity_id, day, parcel_count)
VALUES (1, '2019-01-01', 111);

INSERT INTO capacity_counter_notification (capacity_counter_id, is_50_percent_notification_send,
                                           is_90_percent_notification_send, is_100_percent_notification_send)
VALUES (1, TRUE, TRUE, TRUE);
