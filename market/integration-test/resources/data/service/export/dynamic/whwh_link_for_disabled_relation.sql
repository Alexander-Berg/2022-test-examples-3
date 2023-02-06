INSERT INTO partner (id, name, status, type, billing_client_id, rating, location_id, tracking_type)
VALUES (1, 'Dropship service', 'active', 'DROPSHIP', 124, 1, 255, NULL),
       (2, 'Sorting center', 'active', 'SORTING_CENTER', 124, 5, 255, 'status1'),
       (3, 'Sorting center 2', 'active', 'SORTING_CENTER', 124, 5, 255, NULL),
       (4, 'Delivery service', 'active', 'DELIVERY', 124, 5, 255, 'status2');

INSERT INTO partner_relation (id, from_partner, to_partner, handling_time, enabled, return_partner, shipment_type)
VALUES (1, 1, 2, 0, TRUE, 2, 'WITHDRAW'),
       (2, 1, 3, 0, FALSE, 3, 'WITHDRAW'),
       (3, 1, 4, 0, FALSE, 3, 'WITHDRAW');

INSERT INTO platform_client (id, name)
VALUES (1, 'Beru');

INSERT INTO platform_client_partners (partner_id, platform_client_id, id, status)
VALUES (1, 1, 1, 'ACTIVE'),
       (2, 1, 2, 'ACTIVE'),
       (3, 1, 3, 'ACTIVE');

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
       (10, 5, '10:00', '20:00'),
       (10, 6, '10:00', '20:00'),
       (10, 7, '10:00', '20:00');

INSERT INTO logistics_point (id, address_id, external_id, type, active, partner_id, schedule_id)
VALUES (1, 1, 'warehouse1', 'WAREHOUSE', TRUE, 1, 10),
       (2, 1, 'warehouse2', 'WAREHOUSE', TRUE, 2, 10),
       (3, 1, 'warehouse3', 'WAREHOUSE', TRUE, 3, 10),
       (4, 1, 'warehouse4', 'WAREHOUSE', TRUE, 4, 10);
