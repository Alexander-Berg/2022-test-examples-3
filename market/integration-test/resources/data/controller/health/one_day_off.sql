INSERT INTO partner (name, status, type, billing_client_id, rating)
VALUES ('Fulfillment service 1', 'active', 'FULFILLMENT', 123, 1),
       ('Delivery service 1', 'active', 'DELIVERY', 123, 1);

INSERT INTO partner_relation (from_partner, to_partner, enabled, return_partner)
VALUES (1, 2, TRUE, 1);

INSERT INTO partner_relation_day_off (partner_relation_id, day_off, created)
VALUES (1, '2018-10-29', '2018-10-29 12:00:00');
