INSERT INTO partner (id, name, readable_name, status, type)
VALUES (1, 'CrossDock1', 'Some crossdock partner', 'active', 'SUPPLIER'),
       (2, 'CrossDock2', 'Another crossdock partner', 'active', 'SUPPLIER'),
       (3, 'Fulfillment1', 'Another crossdock partner', 'active', 'FULFILLMENT'),
       (4, 'CrossDock3', 'Another crossdock partner', 'active', 'SUPPLIER'),
       (5, 'CrossDock4', 'Another crossdock partner', 'active', 'SUPPLIER'),
       (6, 'CrossDock5', 'Another crossdock partner', 'active', 'SUPPLIER'),
       (7, 'CrossDock6', 'Another crossdock partner', 'active', 'SUPPLIER');


INSERT INTO platform_client (id, name)
VALUES (1, 'Beru'),
       (2, 'Bringly'),
       (3, 'Yandex Delivery');


INSERT INTO partner_capacity (id, partner_id, location_from, location_to, delivery_type, type,
                              counting_type, service_type, platform_client_id, day, value)
VALUES (10, 1, 255, 255, 'courier', 'regular', 'order', 'shipment', 1, null, 100),
       (11, 1, 255, 255, 'courier', 'regular', 'order', 'shipment', 2, null, 200),
       (12, 2, 255, 255, 'courier', 'regular', 'order', 'shipment', 1, null, 300),
       (13, 2, 255, 255, 'courier', 'regular', 'order', 'shipment', 1, null, 400),
       (14, 2, 255, 255, 'courier', 'regular', 'order', 'shipment', 2, null, 500),
       (15, 3, 255, 255, 'courier', 'regular', 'order', 'shipment', 1, null, 600),
       (16, 4, 255, 255, 'courier', 'regular', 'order', 'shipment', 1, null, 700),
       (17, 5, 255, 255, 'courier', 'regular', 'order', 'shipment', 1, null, 800),
       (18, 5, 255, 255, 'courier', 'regular', 'order', 'shipment', 2, null, 900),
       (19, 5, 255, 255, 'courier', 'regular', 'order', 'shipment', 3, null, 1000),
       (20, 6, 255, 255, 'courier', 'regular', 'order', 'delivery', 1, null, 1100),
       (21, 7, 255, 255, 'courier', 'regular', 'order', 'shipment', 1, null, 1200);


INSERT INTO partner_capacity_day_off (id, capacity_id, day)
VALUES (100, 10, '2020-10-10'),
       (101, 10, '2020-10-11'),
       (102, 10, '2020-10-12'),
       (103, 10, '2020-10-13'),
       (104, 10, '2020-10-14'),
       (105, 11, '2020-10-12'),
       (106, 11, '2020-10-13'),
       (107, 11, '2020-10-14'),
       (108, 11, '2020-10-15'),
       (109, 11, '2020-10-16'),

       (110, 12, '2020-10-10'),
       (111, 12, '2020-10-11'),
       (112, 12, '2020-10-11'),
       (113, 12, '2020-10-12'),
       (114, 12, '2020-10-13'),
       (115, 12, '2020-10-14'),
       (116, 12, '2020-10-15'),
       (117, 12, '2020-10-16'),
       (118, 13, '2020-10-14'),
       (119, 14, '2020-10-13'),
       (120, 14, '2020-10-14'),
       (121, 14, '2020-10-15'),

       (122, 15, '2020-10-11'),
       (123, 15, '2020-10-12'),
       (124, 15, '2020-10-13'),
       (125, 15, '2020-10-14'),
       (126, 15, '2020-10-15'),

       (127, 16, '2020-10-11'),
       (128, 16, '2020-10-12'),
       (129, 16, '2020-10-14'),
       (130, 16, '2020-10-14'),
       (131, 16, '2020-10-15'),

       (132, 17, '2020-10-11'),
       (133, 17, '2020-10-12'),
       (134, 17, '2020-10-13'),
       (135, 17, '2020-10-14'),
       (136, 17, '2020-10-15'),
       (137, 18, '2020-10-11'),
       (138, 18, '2020-10-12'),
       (139, 18, '2020-10-13'),
       (140, 18, '2020-10-14'),
       (141, 18, '2020-10-15'),
       (142, 19, '2020-10-11'),
       (143, 19, '2020-10-12'),
       (144, 19, '2020-10-13'),
       (145, 19, '2020-10-14'),
       (146, 19, '2020-10-15'),

       (147, 20, '2020-10-11'),
       (148, 20, '2020-10-12'),
       (149, 20, '2020-10-13'),
       (150, 20, '2020-10-14'),
       (151, 20, '2020-10-15'),

       (152, 21, '2020-10-11'),
       (153, 21, '2020-10-12'),
       (154, 21, '2020-10-13'),
       (155, 21, '2020-10-14'),
       (156, 21, '2020-10-15');
