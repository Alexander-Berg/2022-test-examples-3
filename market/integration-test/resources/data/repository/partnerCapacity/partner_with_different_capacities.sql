INSERT INTO partner (id, name, readable_name, status, type)
VALUES (1, 'CrossDock1', 'Some crossdock partner', 'active', 'SUPPLIER');

INSERT INTO platform_client (id, name)
VALUES (1, 'Beru'),
       (2, 'Bringly');

INSERT INTO partner_capacity (id, partner_id, location_from, location_to, delivery_type, type,
                              counting_type, service_type, platform_client_id, day, value)
VALUES (10, 1, 255, 255, 'courier', 'regular', 'order', 'delivery', 1, null, 100),
       (11, 1, 255, 255, 'courier', 'regular', 'order', 'shipment', 1, '2020-10-10', 100),
       (12, 1, 255, 255, 'courier', 'regular', 'order', 'shipment', 1, null, 100),
       (13, 1, 255, 255, 'courier', 'regular', 'item', 'shipment', 1, null, 100),
       (14, 1, 256, 256, 'courier', 'regular', 'order', 'shipment', 1, null, 100),
       (15, 1, 255, 255, 'courier', 'regular', 'order', 'shipment', 1, null, 200),
       (16, 1, 255, 255, 'courier', 'reserve', 'order', 'shipment', 1, null, 200),
       (17, 1, 255, 255, 'courier', 'regular', 'order', 'shipment', 2, null, 100),
       (18, 1, 255, 255, null, 'regular', 'order', 'shipment', 1, null, 100);


INSERT INTO partner_capacity_day_off (id, capacity_id, day)
VALUES (10, 10, '2020-10-10'),
       (11, 11, '2020-10-10');
