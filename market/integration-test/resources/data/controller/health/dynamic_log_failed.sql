INSERT INTO platform_client(id, name) VALUES
    (1, 'Beru'),
    (2, 'Bringly'),
    (3, 'Market'),
    (4, 'Yet another market');

INSERT INTO dynamic_log (id, platform_client_id, validated, status) VALUES
    (1, 1, '2018-06-28 15:00:00.056000', 'FAILED'),
    (2, 1, '2019-04-30 15:01:00.056000', 'OK'),
    (3, 2, '2019-05-28 15:02:00.056000', 'WARN'),
    (4, 3, '2019-05-28 15:03:02.056000', 'OK'),
    (5, 2, '2019-05-29 13:02:01.056000', 'FAILED'),
    (6, 1, '2019-05-29 13:02:02.056000', 'OK'),
    (7, 3, '2019-05-29 13:02:03.056000', 'FAILED'),
    (8, 4, '2019-05-29 13:02:04.056000', 'WARN');

INSERT INTO dynamic_fault (id, dynamic_log_id, entity_type, entity_id, status, reason) VALUES
    (1, 1, 'PARTNER_RELATION', 1, 'FAILED', 'FAILED_1'),
    (2, 3, 'PARTNER_RELATION', 1, 'WARN', 'WARN_1'),
    (3, 5, 'PARTNER_RELATION', 1, 'FAILED', 'FAILED_2'),
    (4, 5, 'PARTNER_RELATION', 2, 'WARN', 'WARN_2'),
    (5, 5, 'PARTNER_RELATION', 3, 'FAILED', 'FAILED_3'),
    (6, 7, 'PARTNER_RELATION', 3, 'FAILED', 'FAILED_4'),
    (7, 8, 'PARTNER_RELATION', 1, 'WARN', 'WARN_3');
