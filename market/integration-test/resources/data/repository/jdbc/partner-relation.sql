INSERT INTO partner(id, name, status, type)
VALUES (123, 'Partner 123', 'active', 'DELIVERY'),
       (124, 'Partner 124', 'active', 'FULFILLMENT'),
       (125, 'Partner 125', 'active', 'FULFILLMENT'),
       (100, 'RETAIL partner', 'active', 'RETAIL');

INSERT INTO platform_client(id, name)
VALUES (1, 'Беру'),
       (2, 'Брингли');

INSERT INTO platform_client_partners(partner_id, platform_client_id, id, status)
VALUES (124, 1, 1, 'active'),
       (100, 1, 2, 'active');

INSERT INTO schedule(id)
VALUES (1);
INSERT INTO schedule_day(id, schedule_id, day, time_from, time_to)
VALUES (1, 1, 1, '12:00:00', '13:00:00'),
       (2, 1, 3, '12:00:00', '13:00:00'),
       (3, 1, 5, '12:00:00', '13:00:00');
INSERT INTO address(id, location_id) VALUES (1, 1);
INSERT INTO logistics_point(id, external_id, active, type, schedule_id, partner_id, address_id)
VALUES (1, '1', true, 'WAREHOUSE', 1, 123, 1),
       (2, '2', true, 'WAREHOUSE', 1, 100, 1);

INSERT INTO partner_handling_time(id, partner_id, location_from, location_to, handling_time)
VALUES (1, 123, 2, 3, 3600000000000),
       (2, 124, 2, 3, 3600000000000),
       (3, 125, 2, 3, 3600000000000),
       (4, 100, 2, 3, 3600000000000);

---------------------------------------------------
INSERT INTO schedule(id)
VALUES (11),
       (12),
       (13);
INSERT INTO schedule_day(id, schedule_id, day, time_from, time_to)
VALUES (11, 12, 1, '12:00:00', '13:00:00'),
       (12, 12, 3, '12:00:00', '13:00:00'),
       (13, 12, 4, '12:00:00', '13:00:00'),
       (14, 13, 2, '12:00:00', '13:00:00'),
       (15, 13, 5, '12:00:00', '13:00:00'),
       (16, 13, 7, '12:00:00', '13:00:00');

INSERT INTO partner_relation(id,
                             enabled,
                             from_partner,
                             to_partner,
                             return_partner,
                             to_partner_logistics_point,
                             inbound_time,
                             transfer_time,
                             handling_time,
                             shipment_type,
                             register_schedule,
                             import_schedule,
                             intake_schedule)
VALUES (125, true, 124, 123, 124, 1, 3600000000000, 24 * 3600000000000, 1, 'IMPORT', 11, 12, 13),
       (126, true, 123, 124, 123, 1, null, null, 1, 'IMPORT', 11, 11, 11),
       (127, true, 125, 123, 125, 1, null, null, 1, 'IMPORT', 11, 11, 11),
       (100, true, 100, 123, 100, 1, 3600000000000, 24 * 3600000000000, 1, 'IMPORT', 11, 12, 13);

INSERT INTO partner_relation_product_rating(id, partner_relation_id, location_id, rating)
VALUES (1, 125, 1, 1),
       (2, 125, 2, 2);

INSERT INTO partner_relation_cutoff(id, partner_relation_id, location_id, cutoff_time, packaging_duration)
VALUES (1, 125, 1, '10:00', 3600000000000),
       (2, 125, 2, '11:00', 7200000000000);
