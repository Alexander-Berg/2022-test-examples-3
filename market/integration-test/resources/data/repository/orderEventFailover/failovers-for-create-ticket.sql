TRUNCATE order_events_failover_counter RESTART IDENTITY CASCADE;

INSERT INTO order_events_failover_counter(event_id,
                                          order_id,
                                          attempted_at,
                                          attempt_count,
                                          last_fail_cause,
                                          fixed,
                                          failure_order_event_action,
                                          queued,
                                          ticket_creation_status)
VALUES
    -- too old
    (10, 1, '2020-11-22 15:00:00', 6, 'cause', false, null, false, 'NOT_CREATED'),
    (11, 1, '2020-11-22 15:01:00', 1, 'cause', false, null, true, 'N_A'),
    -- fixed
    (20, 2, '2020-11-23 15:00:00', 6, 'cause', true, null, false, 'NOT_CREATED'),
    (21, 2, '2020-11-23 15:01:00', 1, 'cause', false, null, true, 'N_A'),
    -- already sent
    (30, 3, '2020-11-23 15:00:00', 6, 'cause', true, null, false, 'IN_PROGRESS'),
    (31, 3, '2020-11-23 15:01:00', 1, 'cause', false, null, true, 'N_A'),
    -- not enough attempts
    (40, 4, '2020-11-23 15:00:00', 3, 'cause', true, null, false, 'IN_PROGRESS'),
    (41, 4, '2020-11-23 15:01:00', 1, 'cause', false, null, true, 'N_A'),
    -- 1-st sutable
    (50, 5, '2020-11-23 15:00:00', 6, 'cause', false, null, false, 'NOT_CREATED'),
    (51, 5, '2020-11-23 15:01:00', 1, 'cause', false, null, true, 'N_A'),
    -- 2-nd sutable
    (60, 6, '2020-11-23 16:00:00', 6, 'cause', false, null, false, 'NOT_CREATED'),
    (61, 6, '2020-11-23 16:01:00', 1, 'cause', false, null, true, 'N_A');
