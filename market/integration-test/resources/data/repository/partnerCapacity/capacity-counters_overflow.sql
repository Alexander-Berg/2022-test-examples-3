INSERT INTO partner_capacity (capacity_id, partner_id, location_from_id, location_to_id, delivery_type,
                              platform_client_id, value, capacity_type, day)
VALUES (1, 2, 1, 117065, NULL, 1, 2, 'REGULAR', NULL),
       (2, 2, 1, 213, NULL, 1, 2, 'REGULAR', NULL),
       (3, 2, 1, 1, NULL, 1, 2, 'REGULAR', NULL),
       (4, 2, 1, 225, NULL, 1, 2, 'REGULAR', NULL);

INSERT INTO capacity_counter (capacity_id, day, parcel_count, max_allowed_parcel_count)
VALUES (1, '2019-01-01', 100, 200),
       (2, '2019-01-02', 150, 200),
       (3, '2019-01-03', 250, 200),
       (4, '2019-01-04', 300, 200);

