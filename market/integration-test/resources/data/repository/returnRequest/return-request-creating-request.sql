INSERT
INTO pickup_point(pvz_market_id, logistic_point_id)
VALUES ('68710', 6871);

INSERT
INTO return_request(return_id,
                    barcode,
                    external_order_id,
                    buyer_name,
                    client_type,
                    request_date,
                    pickup_point_id,
                    state)
VALUES (7833,
        'VOZ_FF_7833',
        167802871,
        'Константин Вячеславович Воронцов',
        'CLIENT',
        '2021-02-13',
        NULL,
        'AWAITING_FOR_DATA'),

       (7832,
        'VOZ_FF_7832',
        167802870,
        'Константин Вячеславович Воронцов',
        'CLIENT',
        '2021-02-13',
        1,
        'CREATING_REQUESTS');

INSERT INTO return_request_item(return_request_id,
                                name,
                                return_type,
                                return_reason,
                                price,
                                count)
VALUES (2,
        'Красные лабутены 43го размера',
        'WITH_DISADVANTAGES',
        'bad quality',
        99.99,
        1);
