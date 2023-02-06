-- tasks of failed job
INSERT INTO takeout_tasks (job_id, api_name, api_url, status, file_description, submitted_at, finished_at)
VALUES ('job_1',
        'mbo_cms_api',
        'url',
        'SUCCESSFUL',
        '{"name":"mbo_cms_api","url":"url","fileId":"fileId","fileKey":"key","emptyContent":false}',
        now() - INTERVAL '2 hours 15 minutes',
        now() - INTERVAL '10 minutes');

INSERT INTO takeout_tasks (job_id, api_name, api_url, status, file_description, submitted_at, finished_at)
VALUES ('job_1',
        'pers_utils',
        'url2',
        'SUCCESSFUL',
        '{"name":"pers_utils","url":"url2","fileId":"fileId2","fileKey":"key2","emptyContent":false}',
        now() - INTERVAL '2 hours 15 minutes',
        now() - INTERVAL '2 hours 10 minutes');

-- tasks of successful job
INSERT INTO takeout_tasks (job_id, api_name, api_url, status, file_description, submitted_at, finished_at)
VALUES ('job_2',
        'pers_qa',
        'url3',
        'SUCCESSFUL',
        '{"name":"pers_qa","url":"url3","fileId":"fileId3","fileKey":"key3","emptyContent":false}',
        now() - INTERVAL '2 hours 15 minutes',
        now() - INTERVAL '2 hours 10 minutes');

INSERT INTO takeout_tasks (job_id, api_name, api_url, status, file_description, submitted_at, finished_at)
VALUES ('job_2',
        'checkouter',
        'url4',
        'SUCCESSFUL',
        '{"name":"checkouter","url":"url4","fileId":"fileId4","fileKey":"key4","emptyContent":false}',
        now() - INTERVAL '2 hours 15 minutes',
        now() - INTERVAL '1 hour 55 minutes');

INSERT INTO takeout_tasks (job_id, api_name, api_url, status, file_description, submitted_at, finished_at)
VALUES ('job_2',
        'pers_grade',
        'url5',
        'SUCCESSFUL',
        '{"name":"pers_grade","url":"url5","fileId":"fileId5","fileKey":"key5","emptyContent":true}',
        now() - INTERVAL '2 hours 15 minutes',
        now() - INTERVAL '2 hours 10 minutes');
