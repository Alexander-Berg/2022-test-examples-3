INSERT INTO qrtz.fired_triggers (entry_id, job_name, trigger_name, instance_name, fired_time, trigger_group, priority, state, job_group, is_nonconcurrent, is_update_data, sched_time, requests_recovery, sched_name) VALUES
('entry_id', 'fullSyncExecutor', 'fullSyncExecutorTrigger', 'instance1', '1530620655038', 'DEFAULT', '0', 'EXECUTING', 'DEFAULT', true, false, '1530620650000', false, 'Sched'),
('entry_id1', 'korobyteSyncExecutor', 'korobyteSyncExecutorTrigger', 'instance2', '1530620655038', 'DEFAULT', '0', 'EXECUTING', 'DEFAULT', true, false, '1530620650000', false, 'Sched');

INSERT INTO qrtz.log (id, job_name, trigger_fire_time, job_finished_time, job_status) VALUES
(10002, 'unknownJob', '2018-07-03 15:00:00.056000', '2018-07-03 15:10:00.056000', 'OK');