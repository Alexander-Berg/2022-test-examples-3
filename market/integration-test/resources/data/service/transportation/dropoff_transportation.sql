INSERT INTO logistics_point
(id, type, partner_id, external_id, address_id, contact_id, schedule_id, active, calendar_id, is_market_branded)
VALUES (100, 'PICKUP_POINT', 4, '04', 1, 1, NULL, TRUE, 2, true),
       (110, 'PICKUP_POINT', 4, '05', 1, 1, NULL, TRUE, 2, false);

INSERT INTO logistic_segments
(id, partner_id, logistic_point_id, partner_relation_id, location_id, name, partner_route_id, type,
 delivery_interval_id)
VALUES (10, 4, 100, NULL, 213, 'dropoff segment', NULL, 'warehouse'::LOGISTIC_SEGMENT_TYPE, NULL),
       (11, 1, NULL, NULL, 213, 'from dropoff movement', NULL, 'movement'::LOGISTIC_SEGMENT_TYPE, NULL),
       (12, 1, NULL, NULL, 213, 'to dropoff movement', NULL, 'movement'::LOGISTIC_SEGMENT_TYPE, NULL),
       (13, 4, 110, NULL, 213, 'not branded dropoff segment', NULL, 'warehouse'::LOGISTIC_SEGMENT_TYPE, NULL),
       (14, 4, 110, NULL, 213, 'pickup point segment', NULL, 'pickup'::LOGISTIC_SEGMENT_TYPE, NULL),
       (15, 1, NULL, NULL, 213, 'from not branded dropoff movement', NULL, 'movement'::LOGISTIC_SEGMENT_TYPE, NULL),
       (16, 1, NULL, NULL, 213, 'to pickup movement', NULL, 'movement'::LOGISTIC_SEGMENT_TYPE, NULL);

INSERT INTO logistic_edges
(id, from_segment_id, to_segment_id)
VALUES (10, 10, 11),  (11, 11, 3), -- дропофф - СЦ
       (12, 1, 12), (13, 12, 10), -- дропшип - дропофф
       (14, 1, 16), (15, 16, 14), -- небрендированный дропофф - СЦ
       (16, 13, 15), (17, 15, 3); -- дропшип - ПВЗ

INSERT INTO logistic_segments_services
(id, segment_id, status, code, duration, price, schedule, calendar, delivery_type)
VALUES (13, 11, 'active', 3, 24, 100, 101, NULL, NULL),
       (14, 12, 'active', 3, 24, 100, 101, NULL, NULL),
       (15, 15, 'active', 3, 24, 100, 101, NULL, NULL),
       (16, 16, 'active', 3, 24, 100, 101, NULL, NULL);
