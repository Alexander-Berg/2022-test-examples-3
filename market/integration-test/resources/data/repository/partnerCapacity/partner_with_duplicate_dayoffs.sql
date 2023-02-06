INSERT INTO partner (id, name, readable_name, status, type)
VALUES (1, 'CrossDock1', 'Some crossdock partner', 'active', 'SUPPLIER');

INSERT INTO platform_client (id, name)
VALUES (1, 'Beru');

INSERT INTO partner_capacity (id, partner_id, location_from, location_to, delivery_type, type,
                              counting_type, service_type, platform_client_id, day, value)
VALUES (10, 1, 255, 255, 'courier', 'regular', 'order', 'shipment', 1, null, 100);


INSERT INTO partner_capacity_day_off (id, capacity_id, day)
VALUES (10, 10, '2020-10-10'),
       (11, 10, '2020-10-10');
