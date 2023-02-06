INSERT INTO godd.request_condition(id,
                                   description,
                                   from_date_offset,
                                   to_date_offset,
                                   from_time_offset,
                                   to_time_offset,
                                   request_time)
VALUES (1, '', 0, 0, 0, 0, null);

INSERT INTO godd.orders(id,
                        from_date,
                        to_date,
                        from_time,
                        to_time,
                        timezone_offset,
                        delivery_service_id,
                        delivery_type,
                        warehouse_id,
                        created)
VALUES (122,
        '2020-06-01',
        '2020-06-01',
        '10:00:00',
        '20:30:00',
        28800,
        2,
        'DELIVERY',
        0,
        '2020-06-01');

INSERT INTO godd.order_request(id,
                               order_id,
                               request_condition_id,
                               create_time,
                               process_time,
                               status)
VALUES (1, 122, 1, '2020-06-01 10:00:00', '2020-06-02 10:00:00', 'NEW');
