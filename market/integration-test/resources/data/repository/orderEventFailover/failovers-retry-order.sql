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
       -- fixed
    (10, 1, '2020-11-22 15:00:00', 6, 'cause', true, null, false, 'NOT_CREATED'),
       -- blocker
    (11, 1, '2020-11-22 15:00:00', 6, 'cause', false, null, false, 'NOT_CREATED'),
       -- queue tail
    (12, 1, '2020-11-22 15:01:00', 1, 'cause', false, null, true, 'N_A'),
    (13, 1, '2020-11-22 15:01:00', 1, 'cause', false, null, true, 'N_A'),
    (14, 1, '2020-11-22 15:01:00', 1, 'cause', false, null, true, 'N_A');
