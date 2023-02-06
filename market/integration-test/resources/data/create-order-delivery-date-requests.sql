INSERT INTO godd.request_condition(id,
                                   description,
                                   from_date_offset,
                                   to_date_offset,
                                   from_time_offset,
                                   to_time_offset,
                                   request_time)
VALUES (1, '', 0, 0, 0, 0, null);

INSERT INTO godd.delivery_request_condition(delivery_service_id,
                                            request_condition_id)
VALUES (2, 1);

INSERT INTO godd.order_request(id,
                               order_id,
                               request_condition_id,
                               create_time,
                               process_time,
                               status)
VALUES (1, 123, 1, now(), '2020-07-14 10:59:00', 'NEW'),
       (2, 123, 1, now(), '2020-07-14 11:01:00', 'NEW'),
       (3, 123, 1, now(), '2020-07-14 12:00:00', 'SENT'),
       (4, 123, 1, now(), '2020-07-14 12:00:00', 'RECEIVED'),
       (5, 123, 1, now(), '2020-07-14 12:00:00', 'NEW');

ALTER sequence godd.order_request_id_seq RESTART WITH 6;
