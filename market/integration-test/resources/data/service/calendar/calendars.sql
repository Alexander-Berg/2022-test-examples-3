INSERT INTO calendar (id, parent_id)
VALUES (1, null),
       (2, 1),
       (3, 2),
       (4, 3);

INSERT INTO calendar_day (id, calendar_id, is_holiday, day)
VALUES (1, 1, false, '2018-10-01'),
       (2, 1, false, '2018-10-02'),
       (3, 2, true, '2018-10-01'),
       (4, 2, false, '2018-11-03'),
       (5, 4, true, '2018-10-07');