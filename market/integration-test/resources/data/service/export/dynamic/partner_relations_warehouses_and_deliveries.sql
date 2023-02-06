INSERT INTO partner (id, name, status, type, billing_client_id, rating, tracking_type, location_id)
VALUES (1, 'Fulfillment partner', 'active', 'FULFILLMENT', 123, 1, NULL, 255),
       (2, 'Delivery service', 'active', 'DELIVERY', 123, 1, 'status1', NULL),
       (3, 'Supplier partner', 'active', 'SUPPLIER', 124, 1, NULL, 255);

INSERT INTO partner_relation (id, from_partner, to_partner, handling_time, enabled, transfer_time, inbound_time,
                              return_partner, shipment_type)
VALUES (1, 1, 2, 0, TRUE, NULL, NULL, 1, 'WITHDRAW'),
       (2, 3, 1, 0, TRUE, 60000000000, 9000000000000, 1, 'WITHDRAW'),
       (3, 3, 2, 0, TRUE, NULL, NULL, 1, 'WITHDRAW');

INSERT INTO partner_relation_product_rating (id, partner_relation_id, location_id, rating)
VALUES (1, 1, 255, 12),
       (2, 2, 255, 12),
       (3, 3, 255, 12);

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
       (10, 5, '10:00', '20:00');

INSERT INTO logistics_point (id, address_id, external_id, type, active, partner_id, schedule_id)
VALUES (1, 1, 'warehouse1', 'WAREHOUSE', TRUE, 1, 10),
       (2, 1, 'warehouse2', 'WAREHOUSE', TRUE, 3, 10);
