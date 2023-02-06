INSERT INTO partner (id, name, status, type)
VALUES (1, 'fulfillment', 'active', 'FULFILLMENT'),
       (2, 'delivery', 'active', 'DELIVERY'),
       (3, 'sorting_center', 'active', 'SORTING_CENTER'),
       (4, 'fulfillment1', 'active', 'FULFILLMENT');

INSERT INTO schedule (id)
VALUES (1),
       (2),
       (3),
       (4);

INSERT INTO calendar (id, parent_id)
VALUES (1, null),
       (2, 1),
       (3, null),
       (4, 1);

INSERT INTO delivery_interval (id, schedule_id, partner_id, location_id, calendar_id)
VALUES  (1, 1, 1, 225, null),
        (2, 2, 2, 213, 4),
        (3, 3, 3, 9, 3),
        (3, 4, 4, 9, 2);

INSERT INTO schedule_day (id, schedule_id, day, time_from, time_to)
VALUES (1, 1, 1, '10:00', '18:00'),
       (2, 1, 2, '11:00', '19:00'),
       (3, 2, 1, '12:00', '20:00'),
       (4, 2, 3, '13:00', '21:00'),
       (5, 3, 4, '14:00', '22:00'),
       (6, 3, 5, '15:00', '23:00'),
       (7, 4, 6, '15:00', '23:00');

INSERT INTO calendar_day (id, calendar_id, is_holiday, day)
VALUES (1, 1, true, '2018-10-18'),
       (2, 2, true, '2018-10-19'),
       (3, 3, true, '2018-10-20'),
