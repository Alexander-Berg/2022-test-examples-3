INSERT INTO partner (id, name, status, type, billing_client_id, rating, tracking_type, location_id)
VALUES (1, 'Fulfillment service 1', 'active', 'FULFILLMENT', 123, 1, NULL, 255),
       (2, 'Delivery service 1', 'active', 'DELIVERY', 123, 1, 'status1', NULL),
       (3, 'Dropship service 1', 'active', 'DROPSHIP', 124, 1, NULL, 255),
       (4, 'Delivery service 2', 'active', 'DELIVERY', 124, 2, 'status2', NULL);

INSERT INTO partner_relation (id, from_partner, to_partner, handling_time, enabled, return_partner, shipment_type)
VALUES (1, 1, 2, 0, TRUE, 1, 'WITHDRAW'),
       (2, 3, 4, 0, TRUE, 1, 'WITHDRAW');

INSERT INTO partner_relation_product_rating (id, partner_relation_id, location_id, rating)
VALUES (1, 1, 255, 12),
       (2, 2, 255, 12);

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
       (2, 1, 2, 'ACTIVE'),
       (3, 2, 3, 'ACTIVE'),
       (4, 2, 4, 'ACTIVE');

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
       (2, 1, 'warehouse2', 'WAREHOUSE', TRUE, 2, 10),
       (3, 1, 'warehouse3', 'WAREHOUSE', TRUE, 3, 10),
       (4, 1, 'pickup1', 'PICKUP_POINT', TRUE, 3, 10);


