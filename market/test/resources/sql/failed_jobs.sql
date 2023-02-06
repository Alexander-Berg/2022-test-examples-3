INSERT INTO jobs (job_id, params, submitted_at, finished_at, status, result)
VALUES ('job_1',
        '{"parameters":{"@type":"takeout_parameter","uid":"1","unixtime":123,"type":"WHITE_MARKET"}}',
        now() - INTERVAL '2 hours 15 minutes',
        now() - INTERVAL '15 minutes',
        'FAILED',
        '{"error":"error","files":[]}');

INSERT INTO jobs (job_id, params, submitted_at, finished_at, status, result)
VALUES ('job_2',
        '{"parameters":{"@type":"takeout_parameter","uid":"1","unixtime":124,"type":"WHITE_MARKET"}}',
        now() - INTERVAL '2 hours 15 minutes',
        now() - INTERVAL '1 hour 50 minutes',
        'SUCCESSFUL',
        '{"error":"null","files":[{"name":"takeout_2_WHITE_MARKET.json","url":"url","fileId":"fileId","fileKey":"fileKey","emptyContent":false}]}');
