INSERT INTO partner_schedule (partner_from_id, partner_to_id, type, day, time_from, time_to)
VALUES (145, 106, 'SHIPMENT', 1, '21:00:00', '21:00:00'),
       (145, 106, 'SHIPMENT', 2, '21:00:00', '21:00:00'),
       (145, 106, 'SHIPMENT', 3, '21:00:00', '21:00:00'),
       (145, 106, 'REGISTER', 1, '17:00:00', '18:00:00'),
       (145, 106, 'REGISTER', 2, '17:00:00', '18:00:00'),
       (145, 106, 'REGISTER', 3, '17:00:00', '18:00:00');

INSERT INTO partner_relation (id, partner_from_id, partner_to_id, enabled, partner_from_type, partner_to_type)
VALUES (1, 145, 106, True, null, null),
       (3, 48349, 4, True, 'DROPSHIP', 'SORTING_CENTER');
