INSERT INTO jobs (job_id, params, submitted_at, finished_at, status, result)
VALUES ('job_1',
        '{"parameters":{"@type":"takeout_parameter","uid":"1","unixtime":123,"type":"WHITE_MARKET"}}',
        now() - INTERVAL '2 months 1 day',
        now() - INTERVAL '2 months 1 day',
        'SUCCESSFUL',
        '{"error":null,"files":[{"name":"takeout_1_WHITE_MARKET.json","url":"url","fileId":"fileId","fileKey":"fileKey","emptyContent":false}]}');

INSERT INTO jobs (job_id, params, submitted_at, finished_at, status, result)
VALUES ('job_2',
        '{"parameters":{"@type":"takeout_parameter","uid":"2","unixtime":123,"type":"WHITE_MARKET"}}',
        now() - INTERVAL '1 month',
        now() - INTERVAL '1 month',
        'SUCCESSFUL',
        '{"error":null,"files":[{"name":"takeout_2_WHITE_MARKET.json","url":"url","fileId":"fileId","fileKey":"fileKey","emptyContent":false}]}');

INSERT INTO jobs (job_id, params, submitted_at, finished_at, status, result)
VALUES ('job_3',
        '{"parameters":{"@type":"takeout_parameter","uid":"3","unixtime":123,"type":"WHITE_MARKET"}}',
        now() - INTERVAL '2 months 1 day',
        now() - INTERVAL '2 months 1 day',
        'CLEANED',
        '{"error":null,"files":[{"name":"takeout_3_WHITE_MARKET.json","url":"url","fileId":"fileId","fileKey":"fileKey","emptyContent":false}]}');
