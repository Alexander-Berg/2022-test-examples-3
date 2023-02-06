insert into queue_tasks (id, queue_name, attempt, process_time, actor)
values (301, 'mail.queue', 0, '1970-01-11'::timestamp, '{"eventId":"101","orderId":"201"}'),
       (302, 'mail.queue', 0, '1970-01-12'::timestamp, '{"eventId":"102","orderId":"202"}'),
       (303, 'mail.queue', 0, '1970-01-13'::timestamp, '{"eventId":"103","orderId":"203"}'),
       (304, 'mail.queue', 0, '1970-01-14'::timestamp, '{"eventId":"104","orderId":"204"}'),
       (305, 'mail.queue', 0, '1970-01-15'::timestamp, '{"eventId":"105","orderId":"201"}'),
       (306, 'mail.queue', 0, '1970-01-16'::timestamp, '{"eventId":"106","orderId":"201"}');

insert into order_events_failover_counter (event_id, order_id, attempt_count, attempted_at, fixed, queued, failure_order_event_action, last_fail_cause, last_fail_cause_type)
values (101, 201, 6, '1970-01-01'::timestamp, false, false, 'SC_ORDER_CREATE', 'причина падения', 'INTERNAL_SERVER_ERROR'),
       (102, 202, 3, '1970-01-02'::timestamp, true, false, 'SC_ORDER_CREATE', null, 'FROZEN_SERVICE'),
       (103, 203, 0, '1970-01-03'::timestamp, false, true, 'SC_ORDER_CREATE', null, 'INTERNAL_SERVER_ERROR'),
       (104, 204, 6, '1970-01-04'::timestamp, false, false, 'FF_ORDER_CREATE', 'другая причина падения', 'FROZEN_SERVICE'),
       (105, 201, 6, '1970-01-05'::timestamp, false, false, 'SC_ORDER_CREATE', 'что-то сломалось', 'INTERNAL_SERVER_ERROR'),
       (106, 201, 0, '1970-01-06'::timestamp, false, false, 'SC_ORDER_CREATE', 'ПРИЧИНА ПАДЕНИЯ', 'INTERNAL_SERVER_ERROR');
