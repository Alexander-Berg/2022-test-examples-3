INSERT INTO partner (id, name, status, type, billing_client_id, rating, location_id)
VALUES (1, 'Fulfillment service 1', 'active', 'FULFILLMENT', 123, 1, 255),
       (2, 'Delivery service 1', 'active', 'DELIVERY', 123, 1, NULL),
       (3, 'Fulfillment service 2', 'active', 'FULFILLMENT', 123, 1, 255),
       (4, 'Delivery service 2', 'active', 'DELIVERY', 123, 1, NULL),
       (5, 'Delivery service 3', 'active', 'DELIVERY', 123, 2, NULL),
       (6, 'Delivery service 4', 'active', 'DELIVERY', 123, 1, NULL),
       (7, 'Dropship service 4', 'active', 'DROPSHIP', 123, 1, 255),
       (8, 'Dropship service 5', 'active', 'DROPSHIP', 123, 1, 255),
       (9, 'SC service 1', 'active', 'SORTING_CENTER', 123, 1, 255),
       (10, 'RETAIL partner', 'active', 'RETAIL', 123, 1, 255),
       (11, 'Fulfillment service 3', 'testing', 'FULFILLMENT', 123, 1, NULL);

INSERT INTO partner_relation (id, from_partner, to_partner, handling_time, enabled, return_partner, shipment_type)
VALUES (1, 1, 2, 0, TRUE, 1, 'WITHDRAW'),
       (2, 1, 4, 0, FALSE, 1, 'WITHDRAW'),
       (3, 1, 5, 0, TRUE, 1, 'WITHDRAW'),
       (4, 3, 4, 0, TRUE, 1, 'WITHDRAW'),
       (5, 3, 6, 0, TRUE, 1, 'WITHDRAW'),
       (6, 7, 5, 0, TRUE, 1, 'WITHDRAW'),
       (7, 7, 6, 0, TRUE, 1, 'WITHDRAW'),
       (8, 8, 9, 0, TRUE, 9, 'WITHDRAW'),
       (9, 10, 9, 0, TRUE, 10, 'WITHDRAW');

INSERT INTO platform_client (id, name)
VALUES (1, 'Beru'),
       (2, 'Bringly');

INSERT INTO platform_client_partners (partner_id, platform_client_id, id, status)
VALUES (1, 1, 1, 'ACTIVE'),
       (3, 2, 2, 'ACTIVE'),
       (7, 1, 3, 'ACTIVE'),
       (8, 1, 4, 'ACTIVE'),
       (9, 1, 5, 'ACTIVE'),
       (10, 1, 6, 'ACTIVE'),
       (11, 1, 7, 'TESTING');

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
       (2, 1, 'warehouse2', 'WAREHOUSE', TRUE, 3, 10),
       (3, 1, 'warehouse3', 'WAREHOUSE', TRUE, 7, 10),
       (4, 1, 'warehouse4', 'WAREHOUSE', TRUE, 8, 10),
       (5, 1, 'warehouse5', 'WAREHOUSE', TRUE, 9, 10),
       (6, 1, 'retail warehouse', 'WAREHOUSE', TRUE, 10, 10);
