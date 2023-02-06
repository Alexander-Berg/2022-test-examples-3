INSERT INTO partner (name, status, type, billing_client_id, rating)
VALUES ('Fulfillment service 1', 'active', 'FULFILLMENT', 123, 1),
       ('Delivery service 1', 'active', 'DELIVERY', 123, 1),
       ('Fulfillment service 2', 'active', 'FULFILLMENT', 1234, 12),
       ('Delivery service 2', 'active', 'DELIVERY', 1234, 12);

INSERT INTO schedule (id)
VALUES (DEFAULT),
       (DEFAULT);

INSERT INTO schedule_day (schedule_id, "day", time_from, time_to)
VALUES (1, 1, '12:00', '13:00'),
       (1, 2, '13:00', '14:00'),
       (2, 3, '12:00', '15:00'),
       (2, 4, '13:00', '16:00');

INSERT INTO address (id, location_id, latitude, longitude, settlement, post_code, street, house, housing, building,
                     apartment, comment, address_string, short_address_string, region)
VALUES (1, 213, 56.948048, 24.107018, '', '1005', 'Уриекстес', '14а', '', '', '', 'SIA “ILIOR”',
        '1005, Рига, Уриекстес, 14а', 'Уриекстес, 14а', 'Москва и Московская область');

INSERT INTO logistics_point
(name, type, partner_id, external_id, address_id, contact_id, schedule_id, active, pickup_point_subtype)
VALUES ('Склад 1', 'WAREHOUSE', 1, 1, 1, NULL, 1, TRUE, NULL),
       ('Склад 2', 'WAREHOUSE', 1, 2, 1, NULL, 1, TRUE, NULL);

INSERT INTO logistics_point_gate
    (logistics_point_id, gate_number, enabled)
VALUES (1, 'gate11', TRUE),
       (1, 'gate12', TRUE),
       (2, 'gate21', FALSE),
       (2, 'gate22', FALSE);

