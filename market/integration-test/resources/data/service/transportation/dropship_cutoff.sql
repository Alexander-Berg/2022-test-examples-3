INSERT INTO partner
    (id, name, status, type, market_id)
VALUES (1, 'dropship', 'active', 'DROPSHIP', 20),
       (2, 'sorting_center', 'active', 'SORTING_CENTER', 30);

INSERT INTO address(id, location_id)
VALUES (10, 213),
       (11, 225);

INSERT INTO logistics_point(id, partner_id, active, type, address_id, external_id)
VALUES (101, 1, TRUE, 'WAREHOUSE', 10, 'dropship-warehouse-id'),
       (102, 2, TRUE, 'WAREHOUSE', 11, 'sorting-center-id'),
       (103, 1, FALSE, 'WAREHOUSE', 10, 'inactive-dropship-warehouse-id');

INSERT INTO logistic_segments (id, partner_id, logistic_point_id, name, type)
VALUES (1, 1, 101, 'dropship warehouse', 'warehouse'::LOGISTIC_SEGMENT_TYPE),
       (2, 1, NULL, 'dropship import', NULL),
       (3, 2, 102, 'sc warehouse', 'warehouse'::LOGISTIC_SEGMENT_TYPE),
       (4, 1, 103, 'inactive dropship warehouse', 'warehouse'::LOGISTIC_SEGMENT_TYPE);

INSERT INTO logistic_edges (id, from_segment_id, to_segment_id)
VALUES (1, 1, 2),
       (2, 2, 3);

INSERT INTO service_code(id, code, is_optional, name, type)
VALUES (1, 'TRANSPORT_MANAGER_MOVEMENT', TRUE, 'Учитывать ли сервис для Transport Manager', 'internal'),
       (2, 'PROCESSING', TRUE, 'Обработка', 'internal'),
       (3, 'CUTOFF', TRUE, 'Катофф', 'internal');

INSERT INTO schedule (id)
VALUES (1001),
       (1002);

INSERT INTO schedule_day (schedule_id, day, time_from, time_to)
VALUES (1001, 1, '10:00', '18:00'),
       (1002, 1, '18:00', '19:00');

INSERT INTO logistic_segments_services (segment_id, status, code, duration, schedule, price)
VALUES (1, 'active', 2, 24 * 60, NULL, 0),
       (4, 'active', 2, 24 * 60, NULL, 0),
       (1, 'active', 3, 0, 1001, 0),
       (4, 'active', 3, 0, 1001, 0),
       (2, 'active', 1, 0, 1002, 0),
       (4, 'active', 1, 0, 1002, 0);
