INSERT INTO scheduled_tasks_log (id, name, host, start_time, finish_time, status, fail_cause)
VALUES (1, 'First job', 'default host', 100, 200, 'OK', NULL),
       (2, 'Second job', 'default host', 110, 300, 'ERROR', 'some error description'),
       (3, 'Third job', 'default host', 120, 400, 'OK', NULL),
       (4, 'Fourth job', 'default host', 130, 500, 'ERROR', 'another error');
