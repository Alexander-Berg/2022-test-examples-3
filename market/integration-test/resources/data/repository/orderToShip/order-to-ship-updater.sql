INSERT INTO order_to_ship
(id, platform_client_id, partner_id, location_from_id, location_to_id, delivery_type, shipment_day, processed, status)
VALUES
('A1', 1, 1, 1, 1, 'DELIVERY', '2019-06-27', TRUE, 'CREATED'),
('B2', 2, 2, 2, 2, 'PICKUP', '2019-06-13', TRUE, 'CREATED'),
('C3', 3, 3, 3, 3, 'POST', '2020-01-01', TRUE, 'CREATED');

INSERT INTO order_to_ship_value(counting_type, value, order_to_ship_id, platform_client_id, partner_id, status)
VALUES ('ORDER', 1, 'A1', 1, 1, 'CREATED'),
       ('ITEM', 3, 'A1', 1, 1, 'CREATED'),
       ('ORDER', 1, 'B2', 2, 2, 'CREATED'),
       ('ITEM', 17, 'B2', 2, 2, 'CREATED'),
       ('ORDER', 1, 'C3', 3, 3, 'CREATED'),
       ('ITEM', 21, 'C3', 3, 3, 'CREATED');
