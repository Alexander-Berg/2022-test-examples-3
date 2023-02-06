INSERT INTO partner_external_params(partner_id, "type", active)
VALUES (1, 'CREATE_ORDER_FREEZE_ENABLED', TRUE),
       (2, 'CREATE_ORDER_FREEZE_ENABLED', FALSE);


INSERT INTO order_events_failover_counter (event_id, order_id, attempted_at, attempt_count, last_fail_cause, fixed,
                                           queued, last_fail_cause_type)
VALUES (1, 123, '2018-01-18 15:57:04', 6, 'Queued', false, false, 'FROZEN_SERVICE'), -- валидный
       (2, 123, '2018-01-18 15:57:04', 6, 'Queued', false, false, 'FROZEN_SERVICE'), -- валидный
       (3, 124, '2018-01-18 15:57:04', 3, 'Queued', false, false, 'FROZEN_SERVICE'), -- еще есть попытки
       (4, 125, '2018-01-18 15:57:04', 6, 'Queued', false, true, 'FROZEN_SERVICE'),  -- поставлен в очередь
       (5, 126, '2018-01-18 15:57:04', 6, 'Queued', true, false, 'FROZEN_SERVICE'),  -- исправлен
       (6, 127, '2018-01-18 15:57:04', 6, 'Queued', false, false, 'UNKNOWN'),        -- не та причина
       (7, 127, '2018-01-18 15:57:04', 6, 'Queued', false, false, 'FROZEN_SERVICE'), -- нет партнера
       (8, 127, '2018-01-18 15:57:04', 6, 'Queued', false, false, 'FROZEN_SERVICE'), -- есть другой партнер

       (9, 127, '2018-01-18 15:57:04', 6, 'Queued', false, false, 'FROZEN_SERVICE'); -- false -> true


INSERT INTO order_event_failover_entity(id, order_events_failover_counter_id, entity_type, entity_id)
VALUES (1, 1, 'PARTNER', 1),
       (2, 2, 'PARTNER', 1),
       (3, 3, 'PARTNER', 1),
       (4, 4, 'PARTNER', 1),
       (5, 5, 'PARTNER', 1),
       (6, 6, 'PARTNER', 1),
       (7, 8, 'PARTNER', 4),
       (8, 9, 'PARTNER', 2);
