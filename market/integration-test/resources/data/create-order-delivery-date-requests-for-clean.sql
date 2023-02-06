INSERT INTO godd.request_condition(id,
                                   description,
                                   from_date_offset,
                                   to_date_offset,
                                   from_time_offset,
                                   to_time_offset,
                                   request_time)
VALUES (1, '', 0, 0, 0, 0, null);

INSERT INTO godd.order_request(id,
                               order_id,
                               request_condition_id,
                               create_time,
                               process_time,
                               status)
VALUES (1, 123, 1, now(), '2020-07-12 10:00:00', 'RECEIVED'),
       (2, 123, 1, now(), '2020-07-12 10:00:00', 'FAIL'),
       (3, 123, 1, now(), '2020-07-12 10:00:00', 'SENT'),
       (4, 123, 1, now(), '2020-07-13 10:00:00', 'RECEIVED'),
       (5, 123, 1, now(), '2020-07-13 10:00:00', 'FAIL'),
       (6, 123, 1, now(), '2020-07-13 10:00:00', 'SENT'),
       (7, 123, 1, now(), '2020-07-14 10:00:00', 'RECEIVED'),
       (8, 123, 1, now(), '2020-07-14 10:00:00', 'FAIL'),
       (9, 123, 1, now(), '2020-07-14 10:00:00', 'SENT'),
       (10, 123, 1, now(), '2020-07-14 10:00:00', 'NEW');

ALTER sequence godd.order_request_id_seq RESTART WITH 8;
