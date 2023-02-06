INSERT INTO order_to_ship
(id, platform_client_id, partner_id, location_from_id, location_to_id, delivery_type, shipment_day, processed, status)
VALUES
('1',   1, 2, 1, 1, 'POST',     '2019-06-01', TRUE, 'CREATED'),
('2',   2, 3, 2, 1, 'PICKUP',   '2019-06-02', TRUE, 'CREATED'),
('3',   3, 4, 3, 1, 'DELIVERY', '2019-06-03', TRUE, 'CREATED'),
('1',   5, 8, 4, 1, 'DELIVERY', '2019-06-04', TRUE, 'CREATED');

INSERT INTO order_to_ship_value (counting_type, value, order_to_ship_id, platform_client_id, partner_id, status)
VALUES ('ORDER', 1, '1', 1, 2, 'CREATED'),
       ('ITEM', 3, '1', 1, 2, 'CREATED'),
       ('ORDER', 1, '2', 2, 3, 'CREATED'),
       ('ITEM', 5, '2', 2, 3, 'CREATED'),
       ('ORDER', 1, '3', 3, 4, 'CREATED'),
       ('ITEM', 7, '3', 3, 4, 'CREATED'),
       ('ORDER', 1, '1', 5, 8, 'CREATED'),
       ('ITEM', 42, '1', 5, 8, 'CREATED');
