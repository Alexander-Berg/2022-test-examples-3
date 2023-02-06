INSERT INTO calendar(id)
VALUES (1),
       (2);

INSERT INTO calendar_day(calendar_id, day, is_holiday)
VALUES (1, '2021-05-09', TRUE),
       (1, '2021-05-10', TRUE),
       (1, '2021-05-05', FALSE),
       (1, '2021-05-02', TRUE),
       (2, '2021-05-02', TRUE),
       (2, '2021-05-07', TRUE);

INSERT INTO partner
    (id, name, status, type, market_id)
VALUES (1, 'delivery', 'active', 'DELIVERY', 20),
       (2, 'fulfillment', 'active', 'FULFILLMENT', 30),
       (3, 'delivery', 'active', 'DELIVERY', 40),
       (4, 'delivery', 'active', 'DELIVERY', 50),
       (5, 'dropship', 'active', 'DELIVERY', 60),
       (6, 'delivery-3pl', 'active', 'DELIVERY', 70);

INSERT INTO schedule (id)
VALUES (101),
       (102),
       (103);

INSERT INTO schedule_day (id, schedule_id, "day", time_from, time_to)
VALUES (1, 101, 1, '10:00', '18:00'),
       (2, 101, 2, '10:00', '18:00'),
       (3, 101, 3, '10:00', '18:00'),
       (4, 101, 4, '10:00', '18:00'),
       (5, 101, 5, '10:00', '18:00'),
       (6, 102, 6, '15:00', '20:00'),
       (7, 102, 7, '15:00', '20:00'),
       (8, 103, 1, '10:00', '18:00'),
       (9, 103, 2, '10:00', '18:00'),
       (10, 103, 3, '10:00', '18:00'),
       (11, 103, 4, '10:00', '18:00'),
       (12, 103, 5, '10:00', '18:00'),
       (13, 103, 6, '10:00', '18:00'),
       (14, 103, 7, '10:00', '18:00');

INSERT INTO address
    (id, post_code)
VALUES (1, '123123');

INSERT INTO contact
    (id, name)
VALUES (1, 'contact');

INSERT INTO logistics_point
(id, type, partner_id, external_id, address_id, contact_id, schedule_id, active, calendar_id)
VALUES (10, 'WAREHOUSE', 1, '01', 1, 1, NULL, TRUE, 2),
       (20, 'WAREHOUSE', 2, '02', 1, 1, NULL, TRUE, 1),
       (30, 'WAREHOUSE', 3, '03', 1, 1, NULL, TRUE, NULL),
       (40, 'WAREHOUSE', 4, '04', 1, 1, NULL, TRUE, NULL),
       (50, 'WAREHOUSE', 5, '05', 1, 1, NULL, TRUE, NULL);

INSERT INTO logistic_segments
(id, partner_id, logistic_point_id, partner_relation_id, location_id, name, partner_route_id, type,
 delivery_interval_id)
VALUES (1, 1, 10, NULL, 213, 'partner1 warehouse segment (dropship #1)', NULL, 'warehouse'::LOGISTIC_SEGMENT_TYPE,
        NULL),
       (2, 1, NULL, NULL, 213, 'partner1 car segment (self-export)', NULL, NULL, NULL),
       (3, 2, 20, NULL, 213, 'partner2 warehouse segment (sorting center)', NULL, 'warehouse'::LOGISTIC_SEGMENT_TYPE,
        NULL),
       (4, 3, 30, NULL, 213, 'partner3 warehouse segment (dropship #2)', NULL, 'warehouse'::LOGISTIC_SEGMENT_TYPE,
        NULL),
       (5, 4, 40, NULL, 213, 'partner4 warehouse segment (dropship #3)', NULL, 'warehouse'::LOGISTIC_SEGMENT_TYPE,
        NULL),
       (6, 2, NULL, NULL, 213, 'partner2 car segment (sorting center withdraw)', NULL, NULL, NULL),
       (7, 1, NULL, NULL, 213, 'partner1 car return segment', NULL, 'return_movement'::LOGISTIC_SEGMENT_TYPE, NULL),
       (8, 5, 50, NULL, 213, 'partner5 warehouse segment (dropship #4)', NULL, 'warehouse'::LOGISTIC_SEGMENT_TYPE, NULL),
       (9, 6, NULL, NULL, 213, '3pl moving segment', NULL, NULL, NULL);

INSERT INTO logistic_edges
    (id, from_segment_id, to_segment_id)
VALUES (1, 1, 2), -- дропшип в свою машину
       (2, 2, 3), -- машина в СЦ
       (3, 4, 6), -- СЦ к дропшипу забором
       (4, 5, 6), -- СЦ к другому дропшипу забором
       (5, 6, 3), -- СЦ обратно на склад
       (6, 1, 7), -- СД в свою машину (возврат)
       (7, 7, 3), -- Машина СД к ФФ (возврат)
       (8, 8, 9), -- дропшип в 3pl машину
       (9, 9, 3); -- 3pl машина в СЦ

INSERT INTO service_code(id, code, is_optional, name, type)
VALUES (1, 'INBOUND', TRUE, 'Приёмка', 'inbound'),
       (2, 'SHIPMENT', TRUE, 'Отгрузка', 'outbound'),
       (3, 'TRANSPORT_MANAGER_MOVEMENT', TRUE, 'Учитывать ли сервис для Transport Manager', 'internal'),
       (4, 'LAST_MILE', TRUE, 'Последняя миля', 'internal');

INSERT INTO logistic_segments_services
(id, segment_id, status, code, duration, price, schedule, calendar, delivery_type)
VALUES (1, 1, 'active', 2, 24, 100, 101, NULL, NULL),
       (2, 2, 'active', 3, 24, 100, 101, NULL, NULL),
       (3, 3, 'active', 1, 24, 100, 103, NULL, NULL),
       (4, 4, 'active', 2, 24, 100, 102, NULL, NULL),
       (5, 5, 'active', 2, 24, 100, 102, NULL, NULL),
       (6, 6, 'active', 3, 24, 100, 102, NULL, NULL),
       (7, 6, 'active', 4, 24, 100, 102, NULL, NULL),
       (8, 2, 'active', 1, 24, 100, 102, NULL, NULL),
       (9, 2, 'active', 2, 24, 100, 102, NULL, NULL),
       (10, 7, 'active', 3, 24, 100, 102, NULL, NULL),
       (11, 8, 'active', 2, 24, 100, 101, NULL, NULL),
       (12, 9, 'active', 3, 24, 100, 101, NULL, NULL);

INSERT INTO logistic_segments_services_meta_key
    (id, "key", description)
VALUES (1, 'WEIGHT', 'Вес'),
       (2, 'VOLUME', 'Объём'),
       (3, 'ROUTING_ENABLED', 'Включена ли маршрутизация'),
       (4, 'ROUTING_DIMENSIONS_CLASS', 'Весогабариты '),
       (5, 'ROUTING_EXPECTED_VOLUME', 'Ожидаемый объем'),
       (6, 'ROUTING_EXCLUDED_FROM_LOCATION_GROUP', 'Исключен ли из группировки'),
       (7, 'ROUTING_LOCATION_GROUP_TAG', 'Тэг для группировки');

INSERT INTO logistic_segments_services_meta_value
    (id, service_id, key_id, value)
VALUES (1, 1, 1, '100'),
       (2, 1, 2, '1000'),
       (3, 2, 1, '200'),
       (4, 2, 2, '2000'),
       (5, 3, 1, '300'),
       (6, 3, 2, '3000'),
       (7, 12, 3, 'true'),
       (8, 12, 4, 'REGULAR_CARGO'),
       (9, 12, 5, '1.0'),
       (10, 12, 6, 'true'),
       (11, 12, 7, 'location-group-tag');



