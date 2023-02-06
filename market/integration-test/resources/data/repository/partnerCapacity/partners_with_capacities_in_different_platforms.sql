INSERT INTO partner (id, name, readable_name, status, type)
VALUES (1, 'CrossDock1', 'Some crossdock partner', 'active', 'SUPPLIER'),
       (2, 'CrossDock2', 'Another crossdock partner', 'active', 'SUPPLIER');

INSERT INTO platform_client (id, name)
VALUES (1, 'Beru'),
       (2, 'Bringly');

INSERT INTO partner_capacity (id, partner_id, location_from, location_to, delivery_type, type,
                              counting_type, service_type, platform_client_id, day, value)
VALUES (10, 1, 255, 255, 'courier', 'regular', 'order', 'shipment', 1, null, 100),
       (11, 1, 255, 255, 'courier', 'regular', 'order', 'shipment', 2, null, 200),
       (12, 2, 255, 255, 'courier', 'regular', 'order', 'shipment', 2, null, 300),
       (13, 2, 255, 255, 'courier', 'regular', 'order', 'shipment', 2, null, 400);

INSERT INTO partner_capacity_day_off (id, capacity_id, day)
VALUES (10, 10, '2020-10-10'),
       (11, 11, '2020-10-11'),
       (12, 12, '2020-10-10'),
       (13, 13, '2020-10-11');
