INSERT INTO calendar (parent_id)
VALUES (null),
       (1),
       (null),
       (3),
       (null),
       (5);

INSERT INTO calendar_day (calendar_id, is_holiday, day)
VALUES (1, true, '2018-10-01'),
       (2, true, '2018-11-01'),
       (3, true, '2018-11-02'),
       (4, true, '2018-11-02'),
       (5, true, '2018-11-02'),
       (6, true, '2018-11-03');

UPDATE delivery_interval
SET calendar_id = CASE id
      WHEN 1 THEN 2
      WHEN 2 THEN 4
      WHEN 3 THEN 6
    END
WHERE id in (1, 2, 3);