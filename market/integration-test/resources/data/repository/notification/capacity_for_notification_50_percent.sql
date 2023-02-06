INSERT INTO partner_capacity (capacity_id, partner_id, location_from_id, location_to_id, delivery_type,
                              platform_client_id, value, capacity_type, day, counting_type, service_type)
VALUES (1, 145, 1, 225, NULL, 1, 100, 'REGULAR', NULL, 'ORDER', 'DELIVERY'),
       (2, 145, 1, 213, NULL, 1, 100, 'REGULAR', NULL, 'ITEM', 'SHIPMENT');

INSERT INTO capacity_counter (capacity_id, day, parcel_count)
VALUES (1, '2019-01-01', 51);
