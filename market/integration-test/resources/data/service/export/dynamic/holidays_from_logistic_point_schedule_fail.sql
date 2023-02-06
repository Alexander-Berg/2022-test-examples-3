INSERT INTO partner (id, name, status, type, billing_client_id, rating, location_id, tracking_type)
VALUES (1, 'Dropship service 1. Has calendar', 'active', 'DROPSHIP', 124, 1, 255, NULL),
       (2, 'Dropship service 2. Has no calendar', 'active', 'DROPSHIP', 124, 2, 255, 'status1'),
       (3, 'Dropship service 3. Has no calendar & 24/7 warehouse', 'active', 'DROPSHIP', 124, 3, 255, NULL),
       (4, 'Delivery service', 'active', 'DELIVERY', 124, 4, 255, 'status2'),
       (5, 'Sorting center', 'active', 'SORTING_CENTER', 124, 5, 255, NULL),
       (6, 'Dropship service 4. Has 2 warehouses', 'active', 'DROPSHIP', 124, 1, 255, NULL);

INSERT INTO partner_relation (id, from_partner, to_partner, handling_time, enabled, return_partner, shipment_type)
VALUES (1, 1, 5, 0, TRUE, 5, 'WITHDRAW'),
       (2, 2, 5, 0, TRUE, 5, 'WITHDRAW'),
       (3, 3, 5, 0, TRUE, 5, 'WITHDRAW'),
       (4, 5, 4, 0, TRUE, 5, 'WITHDRAW'),
       (5, 6, 5, 0, TRUE, 5, 'WITHDRAW');

INSERT INTO partner_relation_product_rating (id, partner_relation_id, location_id, rating)
VALUES (1, 1, 255, 12),
       (2, 2, 255, 12);

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
       (4, 1, 4, 'ACTIVE'),
       (5, 1, 5, 'ACTIVE'),
       (6, 1, 6, 'ACTIVE');

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
       (2, 1, 'warehouse2', 'WAREHOUSE', TRUE, 2, NULL),
       (3, 1, 'warehouse3', 'WAREHOUSE', TRUE, 3, NULL),
       (4, 1, 'warehouse4', 'WAREHOUSE', TRUE, 4, NULL),
       (5, 1, 'warehouse5', 'WAREHOUSE', TRUE, 5, NULL),
       (6, 1, 'pickup1', 'PICKUP_POINT', TRUE, 3, NULL),
       (7, 1, 'warehouse6', 'WAREHOUSE', TRUE, 6, 10),
       (8, 1, 'warehouse7', 'WAREHOUSE', TRUE, 6, 10);


