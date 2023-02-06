INSERT INTO schedule (id)
VALUES (10);

INSERT INTO schedule_day (schedule_id, day, time_from, time_to, is_main)
VALUES (10, 1, '10:00', '20:00', TRUE),
       (10, 2, '10:00', '20:00', TRUE),
       (10, 3, '10:00', '20:00', TRUE),
       (10, 4, '10:00', '20:00', TRUE),
       (10, 5, '10:00', '20:00', TRUE);

INSERT INTO partner (id, name, status, type, billing_client_id, rating, calendar_id, location_id, tracking_type)
VALUES (1, 'Dropship service', 'active', 'DROPSHIP', 124, 1, NULL, 255, NULL),
       (1006360, 'Delivery service', 'active', 'DELIVERY', 124, 4, NULL, 255, 'status2'),
       (2, 'Delivery service 2', 'active', 'DELIVERY', 124, 4, NULL, 255, 'status2'),
       (3, 'Sorting center', 'active', 'SORTING_CENTER', 124, 5, NULL, 255, NULL);

INSERT INTO partner_external_param_type (id, key)
VALUES (1, 'DROPSHIP_EXPRESS'),
       (2, 'DISABLE_ORDERS_AT_NON_WORKING_TIME');

INSERT INTO partner_external_param_value (id, partner_id, type_id, value)
VALUES (1, 1, 1, '1'),
       (2, 1, 2, '1');

INSERT INTO partner_relation (id, from_partner, to_partner, handling_time, enabled, return_partner, shipment_type,
                              intake_schedule)
VALUES (1, 1, 2, 0, FALSE, 1, 'WITHDRAW', 10),
       (2, 1, 1006360, 0, TRUE, 1, 'WITHDRAW', 10),
       (3, 3, 2, 0, TRUE, 3, 'WITHDRAW', 10);

INSERT INTO delivery_distributor_params (id, flag_id, location_id, min_weight, max_weight, strict_bounds_type,
                                         delivery_cost, delivery_duration)
VALUES (1, 1, 1, 1, 1, 'none', 1, 1),
       (2, 2, 1, 1, 1, 'none', 1, 1),
       (3, NULL, 1, 1, 1, 'none', 1, 1),
       (4, 3, 2, 3, 4, 'left', 100, 6),
       (5, 4, 2, 3, 4, 'left', 100, 6);

INSERT INTO platform_client (id, name)
VALUES (1, 'Beru');

INSERT INTO platform_client_partners (partner_id, platform_client_id, id, status)
VALUES (1, 1, 1, 'ACTIVE'),
       (2, 1, 2, 'ACTIVE'),
       (3, 1, 3, 'ACTIVE'),
       (1006360, 1, 4, 'ACTIVE');

INSERT INTO address (id, address_string)
VALUES (1, 'some address');

INSERT INTO logistics_point (id, address_id, external_id, type, active, partner_id, schedule_id)
VALUES (1, 1, 'warehouse1', 'WAREHOUSE', TRUE, 1, 10),
       (2, 1, 'pickup1', 'PICKUP_POINT', TRUE, 2, NULL),
       (3, 1, 'pickup2', 'PICKUP_POINT', TRUE, 1006360, NULL),
       (4, 1, 'warehouse2', 'WAREHOUSE', TRUE, 3, NULL);
