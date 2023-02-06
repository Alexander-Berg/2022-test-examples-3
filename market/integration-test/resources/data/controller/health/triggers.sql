INSERT INTO qrtz.job_details (job_name, job_group, description, job_class_name, is_durable, is_nonconcurrent, is_update_data, requests_recovery, sched_name, job_data) VALUES
('fullSyncExecutor', 'default', '', 'testtest', true, true, false,false, 'Sched', ''),
('korobyteSyncExecutor', 'default', '', 'testtest', true, true, false,false, 'Sched', '');

INSERT INTO qrtz.triggers (trigger_name, trigger_group, job_name, job_group, description, next_fire_time, prev_fire_time, priority, trigger_state, trigger_type, start_time, end_time, calendar_name, misfire_instr, job_data, sched_name) VALUES
('fullSyncExecutorTrigger', 'default', 'fullSyncExecutor', 'default', '', 1544707800000, 1544706900000, 0, 'BLOCKED', 'CRON', 1539236228000, 0, null, 2, '', 'Sched'),
('korobyteSyncExecutorTrigger', 'default', 'korobyteSyncExecutor', 'default', '', 1544707800000, 1544706900000, 0, 'BLOCKED', 'CRON', 1539236228000, 0, null, 2, '', 'Sched');
