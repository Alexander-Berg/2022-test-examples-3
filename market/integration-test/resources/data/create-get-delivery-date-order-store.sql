INSERT INTO godd.delivery(id, max_requested_orders, enabled)
VALUES (2, 1000, true);

INSERT INTO godd.orders(id,
                        from_date,
                        to_date,
                        from_time,
                        to_time,
                        timezone_offset,
                        delivery_service_id,
                        delivery_type,
                        warehouse_id)
VALUES (123,
        '2020-07-14',
        '2020-07-15',
        '10:00:00',
        '20:30:00',
        28800,
        2,
        'DELIVERY',
        0);
