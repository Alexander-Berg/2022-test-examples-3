-- dropship #1
UPDATE logistic_segments_services_meta_value
SET value = 'azaza'
WHERE service_id = 2;

-- dropship #2
UPDATE logistic_segments
SET type = 'linehaul'::logistic_segment_type
WHERE id = 5;

-- dropship #3
UPDATE logistic_segments_services
SET schedule = NULL
WHERE id = 6;
