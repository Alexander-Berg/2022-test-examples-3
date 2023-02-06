DELETE FROM partner_capacity;

INSERT INTO partner_capacity
(capacity_id, partner_id, location_from_id, location_to_id, delivery_type, platform_client_id, value, capacity_type, day)
VALUES
(1, 1, 1, 1, 'DELIVERY', 1, 1000, 'REGULAR', null),
(2, 2, 2, 2, 'PICKUP', 2, 2000, 'RESERVE', '2019-06-13'),
(3, 3, 3, 3, 'POST', 3, 3000, 'REGULAR', '2020-01-01');
