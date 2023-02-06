INSERT INTO calendar (id)
VALUES (1),
       (2),
       (3),
       (4);

INSERT INTO calendar_day (id, calendar_id, is_holiday, day)
VALUES (1, 1, true, '2018-10-01'),
       (2, 1, true, '2018-11-01'),
       (3, 1, false, '2018-11-02'),
       (4, 2, true, '2018-11-03'),
       (5, 4, true, '2018-10-03');

INSERT INTO location_calendar (location_id, calendar_id)
VALUES (225, 1),
       (213, 3),
       (200, 4);