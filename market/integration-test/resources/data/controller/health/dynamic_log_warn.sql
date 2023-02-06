INSERT INTO platform_client(id, name) VALUES
    (1, 'Beru'),
    (2, 'Bringly');

INSERT INTO dynamic_log (id, platform_client_id, validated, status) VALUES
    (1, 1, '2018-06-28 15:00:00.056000', 'FAILED'),
    (2, 1, '2019-04-30 15:01:00.056000', 'OK'),
    (3, 2, '2019-05-28 15:02:00.056000', 'FAILED'),
    (4, 2, '2019-05-29 13:02:01.056000', 'WARN');

INSERT INTO dynamic_fault (id, dynamic_log_id, entity_type, entity_id, status, reason) VALUES
    (1, 1, 'PARTNER_RELATION', 1, 'FAILED', 'FAILED1'),
    (2, 3, 'PARTNER_RELATION', 2, 'FAILED', 'FAILED2'),
    (3, 4, 'PARTNER_RELATION', 3, 'WARN', 'WARN1'),
    (4, 4, 'PARTNER_RELATION', 4, 'WARN', 'WARN2');
