TRUNCATE order_events_failover_counter RESTART IDENTITY CASCADE;

INSERT INTO order_events_failover_counter(event_id,
                                          order_id,
                                          attempted_at,
                                          attempt_count,
                                          last_fail_cause,
                                          fixed,
                                          queued,
                                          ticket_creation_status)
VALUES
    -- too old
    (10, 1, '2020-11-22 15:00:00', 1, 'Network fail', false, false, 'NOT_CREATED'),
    (11, 1, '2020-11-22 15:01:00', 1, 'cause', false, true, 'N_A');
