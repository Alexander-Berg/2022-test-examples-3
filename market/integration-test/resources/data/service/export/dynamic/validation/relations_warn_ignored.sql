INSERT INTO partner (id, name, readable_name, status, type, billing_client_id, rating, location_id)
VALUES (1, 'Fulfillment service 1', 'Fulfillment service 1', 'active', 'FULFILLMENT', 123, 1, 255),
       (2, 'Fulfillment service 2', 'Fulfillment service 2', 'active', 'FULFILLMENT', 123, 1, 213),
       (3, 'Delivery service 1', 'Delivery service 1', 'active', 'DELIVERY', 123, 1, NULL),
       (4, 'Delivery service 2', 'Delivery service 2', 'active', 'DELIVERY', 123, 1, NULL);

INSERT INTO regions (id, name, parent_id)
VALUES (255, 'Region1', 213),
       (213, 'Region2', NULL);

INSERT INTO partner_relation (id, from_partner, to_partner, handling_time, enabled, return_partner, shipment_type)
    -- OK --
VALUES (1, 1, 3, 0, TRUE, 1, 'WITHDRAW'),
       -- WARN (capacity == 666), must be ignored --
       (2, 1, 4, 666, FALSE, 1, 'WITHDRAW'),
       -- WARN (capacity == 666), must be ignored --
       (3, 2, 4, 666, FALSE, 1, 'WITHDRAW');

INSERT INTO platform_client (id, name)
VALUES (1, 'Beru'),
       (2, 'Bringly');

INSERT INTO platform_client_partners (partner_id, platform_client_id, id, status)
VALUES (1, 1, 1, 'ACTIVE'),
       (2, 1, 2, 'ACTIVE');

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
       (2, 1, 'warehouse2', 'WAREHOUSE', TRUE, 2, 10);
