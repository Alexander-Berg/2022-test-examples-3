INSERT INTO partner (id, name, status, type, billing_client_id, rating, tracking_type, location_id)
VALUES (1, 'Fulfillment service 1', 'active', 'FULFILLMENT', 123, 1, NULL, 255),
       (2, 'Delivery service 1', 'active', 'DELIVERY', 123, 1, 'status1', NULL),
       (3, 'Dropship service 2', 'active', 'DROPSHIP', 123, 1, NULL, 255),
       (4, 'Delivery service 2', 'active', 'DELIVERY', 123, 1, 'status1', NULL),
       (5, 'Dropship service 3', 'active', 'DROPSHIP', 123, 1, NULL, 255),
       (6, 'Delivery service 3', 'inactive', 'DELIVERY', 123, 1, 'status1', NULL),
       (7, 'Dropship service 4', 'inactive', 'DROPSHIP', 123, 1, NULL, 255),
       (8, 'Delivery service 4', 'active', 'DELIVERY', 123, 1, 'status1', NULL),
       (9, 'Fulfillment service 5', 'active', 'FULFILLMENT', 123, 1, NULL, 255),
       (10, 'Delivery service 5', 'active', 'DELIVERY', 123, 1, 'status1', NULL);

INSERT INTO partner_relation (id, from_partner, to_partner, handling_time, enabled, return_partner, shipment_type)
VALUES (1, 1, 2, 0, TRUE, 1, 'WITHDRAW'),
       (2, 3, 4, 0, FALSE, 1, 'WITHDRAW'),
       (3, 5, 6, 0, TRUE, 1, 'WITHDRAW'),
       (4, 7, 8, 0, TRUE, 1, 'WITHDRAW'),
       (5, 9, 10, 0, TRUE, 1, 'WITHDRAW');

INSERT INTO delivery_distributor_params (id, flag_id, location_id, min_weight, max_weight, strict_bounds_type,
                                         delivery_cost, delivery_duration)
VALUES (1, 1, 1, 1, 1, 'none', 1, 1),
       (2, 2, 1, 1, 1, 'none', 1, 1),
       (3, NULL, 1, 1, 1, 'none', 1, 1),
       (4, 3, 2, 3, 4, 'left', 100, 6);

INSERT INTO platform_client (id, name)
VALUES (1, 'Beru'),
       (2, 'Bringly');

INSERT INTO platform_client_partners (partner_id, platform_client_id, id, status)
VALUES (1, 1, 1, 'ACTIVE'),
       (3, 1, 2, 'ACTIVE'),
       (5, 1, 3, 'ACTIVE'),
       (7, 1, 4, 'ACTIVE'),
       (9, 2, 5, 'ACTIVE');

INSERT INTO address (id, address_string)
VALUES (1, 'some address');

INSERT INTO schedule
    (id)
VALUES (10);

INSERT INTO schedule_day
    (schedule_id, day, time_from, time_to)
VALUES (10, 1, '10:00', '20:00'),
       (10, 2, '10:00', '20:00'),
       (10, 3, '10:00', '20:00'),
       (10, 4, '10:00', '20:00'),
       (10, 5, '10:00', '20:00');

INSERT INTO logistics_point (id, address_id, external_id, type, active, partner_id, schedule_id)
VALUES (1, 1, 'warehouse1', 'WAREHOUSE', TRUE, 1, 10),
       (2, 1, 'warehouse3', 'WAREHOUSE', TRUE, 3, 10),
       (3, 1, 'warehouse5', 'WAREHOUSE', TRUE, 5, 10),
       (4, 1, 'warehouse7', 'WAREHOUSE', TRUE, 7, 10),
       (5, 1, 'warehouse9', 'WAREHOUSE', TRUE, 9, 10);
