INSERT INTO public.job_monitoring_config (job_name, max_delay_time, max_execution_time, max_failed_run)
VALUES ('test_job_name_2', 600000, 300000, 3);

INSERT INTO qrtz.log (id, job_name, job_group, job_finished_time, job_status, host_name)
VALUES (1, 'test_job_name_2', 'DEFAULT', '2022-03-22 14:24:00.135000', 'SOME_EXCEPTION', 'test-osx');
INSERT INTO qrtz.log (id, job_name, job_group, job_finished_time, job_status, host_name)
VALUES (2, 'another_job', 'DEFAULT', '2022-03-17 15:06:19.054000', 'OK', 'test-osx');
INSERT INTO qrtz.log (id, job_name, job_group, job_finished_time, job_status, host_name)
VALUES (3, 'test_job_name_2', 'DEFAULT', '2022-03-22 14:26:00.288000', 'SOME_EXCEPTION', 'test-osx');
INSERT INTO qrtz.log (id, job_name, job_group, job_finished_time, job_status, host_name)
VALUES (4, 'test_job_name_2', 'DEFAULT', '2022-03-22 14:24:00.594000', 'ANOTHER_EXCEPTION', 'test-osx');
