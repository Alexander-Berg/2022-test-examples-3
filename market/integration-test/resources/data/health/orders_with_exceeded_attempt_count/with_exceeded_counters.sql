INSERT INTO order_events_failover_counter (event_id, order_id, attempted_at, attempt_count, last_fail_cause, fixed)
VALUES
      (1,	123,	'2018-01-18 15:57:04', 1,	'Queued',	false),
      (2,	124,	'2018-01-18 15:57:04', 3,	'Queued',	false),
      (3,	125,	'2018-01-18 15:57:04', 6,	'Queued',	false),
      (4,	126,	'2018-01-18 15:57:04', 5,	'Queued',	false),
      (5,	127,	'2018-01-18 15:57:04', 8,	'Queued',	false);
