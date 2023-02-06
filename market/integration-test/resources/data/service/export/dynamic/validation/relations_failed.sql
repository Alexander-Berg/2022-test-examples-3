INSERT INTO partner (id, name, readable_name, status, type, billing_client_id, rating, location_id)
VALUES (1, 'Fulfillment service 1',  'Fulfillment service 1', 'active', 'FULFILLMENT', 123, 1, 255),
       (2, 'Fulfillment service 2',  'Fulfillment service 2', 'active', 'FULFILLMENT', 123, 1, 213),
       (3, 'Fulfillment service 3',  'Fulfillment service 3', 'active', 'FULFILLMENT', 123, 1, 39),
       (4, 'Fulfillment service 4',  'Fulfillment service 4', 'active', 'FULFILLMENT', 123, 1, 42),
       (5, 'Delivery service 1', 'Delivery service 1', 'active', 'DELIVERY', 123, 1, null),
       (6, 'Delivery service 2', 'Delivery service 2', 'active', 'DELIVERY', 123, 1, null),
       (7, 'Delivery service 3', 'Delivery service 3', 'active', 'DELIVERY', 123, 1, null),
       (8, 'Delivery service 4', 'Delivery service 4', 'active', 'DELIVERY', 123, 1, null);

INSERT INTO partner_relation (id, from_partner, to_partner, handling_time, enabled, return_partner, shipment_type)
       -- OK --
VALUES (1, 1, 5, 0, TRUE, 1, 'WITHDRAW'),
       -- FAILED (handling_time == 666, enabled), must stop export dynamic --
       (2, 2, 6, 666, TRUE, 1, 'WITHDRAW'),
       -- WARN(handling_time == 666, disabled), must be ignored --
       (3, 3, 7, 666, FALSE, 1, 'WITHDRAW'),
       -- FAILED (handling_time == 666, enabled), must stop export dynamic --
       (4, 4, 8, 666, TRUE, 1, 'WITHDRAW');

INSERT INTO platform_client (id, name)
VALUES (1, 'Beru'), (2, 'Bringly');

INSERT INTO platform_client_partners (partner_id, platform_client_id, id, status)
VALUES (1, 1, 1, 'ACTIVE'),
       (2, 1, 2, 'ACTIVE'),
       (3, 1, 3, 'ACTIVE'),
       (4, 1, 4, 'ACTIVE');
