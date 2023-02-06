INSERT INTO public.job_monitoring_config (job_name, max_delay_time, max_execution_time, max_failed_run)
VALUES ('test_job_name_1', 600000, 300000, 2);

INSERT INTO qrtz.log (id, job_name, job_group, job_finished_time, job_status, host_name)
VALUES (1, 'test_job_name_1', 'DEFAULT', '2022-01-17 13:06:19.054000', 'SOME_EXCEPTION', 'test-osx');
INSERT INTO qrtz.log (id, job_name, job_group, job_finished_time, job_status, host_name)
VALUES (2, 'test_job_name_1', 'DEFAULT', '2022-03-17 15:06:19.054000', 'OK', 'test-osx');
