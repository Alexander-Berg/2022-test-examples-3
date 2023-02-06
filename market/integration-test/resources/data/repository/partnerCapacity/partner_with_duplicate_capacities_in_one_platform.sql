INSERT INTO partner (id, name, readable_name, status, type)
VALUES (1, 'CrossDock1', 'Some crossdock partner', 'active', 'SUPPLIER');

INSERT INTO platform_client (id, name)
VALUES (1, 'Beru');

INSERT INTO partner_capacity (id, partner_id, location_from, location_to, delivery_type, type,
                              counting_type, service_type, platform_client_id, day, value)
VALUES (10, 1, 255, 255, 'courier', 'regular', 'order', 'shipment', 1, null, 100),
       (11, 1, 255, 255, 'courier', 'regular', 'order', 'shipment', 1, null, 200),
       (12, 1, 260, 260, 'courier', 'regular', 'order', 'shipment', 1, null, 300),
       (13, 1, 255, 255, 'pickup', 'regular', 'order', 'shipment', 1, null, 400),
       (14, 1, 255, 255, 'courier', 'regular', 'item', 'shipment', 1, null, 500),
       (15, 1, 255, 255, 'courier', 'regular', 'order', 'shipment', 1, null, 600),
       (16, 1, 255, 255, 'courier', 'regular', 'order', 'shipment', 1, null, 700),
       (17, 1, 255, 255, 'courier', 'regular', 'order', 'shipment', 1, null, 800);


INSERT INTO partner_capacity_day_off (id, capacity_id, day)
VALUES (10, 10, '2020-10-10'),
       (11, 11, '2020-10-10'),
       (12, 12, '2020-10-10'),
       (13, 13, '2020-10-10'),
       (14, 14, '2020-10-10'),
       (15, 15, '2020-10-10'),
       (16, 17, '2020-10-11');
