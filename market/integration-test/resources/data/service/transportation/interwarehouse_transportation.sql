INSERT INTO partner
    (id, name, status, type, market_id)
VALUES (7, 'fulfillment2', 'active', 'FULFILLMENT', 30),
       (8, 'fulfillment3', 'active', 'FULFILLMENT', 30),
       (9, 'delivery', 'active', 'DELIVERY', 30);

INSERT INTO partner_relation(id, from_partner, to_partner, enabled, shipment_type, handling_time, return_partner)
VALUES (100, 7, 8, true, 'IMPORT', '2', 5),
       (101, 2, 7, false, 'IMPORT', '2', 2),
       (102, 2, 8, true, 'IMPORT', '2', 2);


INSERT INTO logistics_point
(id, type, partner_id, external_id, address_id, contact_id, schedule_id, active)
VALUES (60, 'WAREHOUSE', 7, '05', 1, 1, NULL, TRUE),
       (70, 'WAREHOUSE', 8, '06', 1, 1, NULL, TRUE),
       (80, 'WAREHOUSE', 8, '07', 1, 1, NULL, TRUE);

INSERT INTO partner_transport
(id, logistics_point_from, logistics_point_to, partner_id, duration, price, pallet_count)
VALUES (1, 60, 70, 9, '00:30:00', 1000, 20),
       (2, 20, 80, 9, '01:30:00', 2000, 33),
       (5, 60, 70, 9, '00:40:00', 800, 15),
       (6, 20, 60, 9, '00:45:00', 1200, 19);

INSERT INTO interwarehouse_schedule(logistics_point_from, logistics_point_to, "day", time_from, time_to, pallets, type, transport)
VALUES (60, 70, 1, '10:00', '15:00', 15, 'XDOC_TRANSPORT', 1),
       (60, 70, 2, '17:00', '19:00', 5, 'XDOC_TRANSPORT', null),
       (60, 70, 2, '12:00', '14:00', 5, 'XDOC_TRANSPORT', null),
       (60, 70, 2, '17:00', '19:00', 5, 'LINEHAUL', 5),
       (20, 60, 5, '16:00', '19:00', 19, 'LINEHAUL', 6),
       (20, 70, 4, '12:00', '15:00', 10, 'XDOC_TRANSPORT', null),
       (20, 80, 7, '13:00', '20:00', 10, 'XDOC_TRANSPORT', 2);

