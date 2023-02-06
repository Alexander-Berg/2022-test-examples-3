INSERT INTO partner (name, status, type, billing_client_id, rating)
VALUES ('Fulfillment service 1', 'active', 'FULFILLMENT', 123, 1),
       ('Delivery service 1', 'active', 'DELIVERY', 123, 1),
       ('Delivery service 2', 'active', 'DELIVERY', 124, 3),
       ('Dropship service 1', 'active', 'DROPSHIP', 124, 3),
       ('Dropship service 2', 'active', 'DROPSHIP', 124, 3);

INSERT INTO service_code (code, is_optional)
VALUES ('CASH_SERVICE', FALSE),
       ('CHECK', FALSE),
       ('COMPLECT', FALSE);

INSERT INTO schedule
VALUES (DEFAULT),
       (DEFAULT);

INSERT INTO schedule_day (schedule_id, "day", time_from, time_to)
VALUES (1, 1, '10:00', '18:00'),
       (1, 2, '12:00', '16:00'),
       (2, 3, '16:00', '18:00'),
       (2, 4, '06:00', '09:00');

INSERT INTO address (location_id, country, latitude, longitude, settlement, post_code, street, house, housing, building,
                     apartment, comment, address_string, short_address_string, region)
VALUES (11474, 'Россия', 56.948048, 24.107018, '', '1005', 'Уриекстес', '14а', '', '', '', 'SIA “ILIOR”',
        '1005, Рига, Уриекстес, 14а', 'Уриекстес, 14а', 'region1'),
       (21651, 'Россия', 56.659840, 37.863199, '', '111024', 'Яничкин проезд', 7, '', '', '', 'терминал БД-6',
        '111024, Московская область, Котельники, Яничкин проезд, 7', 'Яничкин проезд, 7', 'region2'),
       (976, 'Россия', 56.146633, 101.610517, 'Братск', '665717', 'ул. Южная', '14', '10', '', '', '',
        'Братск, ул. Южная, д. 14 стр. 10', 'ул. Южная, д. 14 стр. 10', 'region3'),
       (213, 'Россия', 56.646633, 36.610517, 'Москва', '665717', 'ул. Южная', '14', '10', '', '', '',
        'Братск, ул. Южная, д. 14 стр. 10', 'ул. Южная, д. 14 стр. 10', 'region3'),
       (10675, 'Россия', 51.379909, 42.125175, 'Борисоглебск', '', 'Матросовская', '162', '', '', '', '',
        'Борисоглебск, Матросовская, д. 162', 'Матросовская, д. 162', 'region3');

INSERT INTO location_zone(id, location_id, name, description)
VALUES (1, 213, 'Внутри МКАД', 'Внутри МКАД'),
       (2, 213, 'От 0 до 10 км от МКАД', 'От 0 до 10 км от МКАД'),
       (3, 213, 'От 10 до 30 км от МКАД', 'От 10 до 30 км от МКАД');

INSERT INTO contact (name, surname, patronymic)
VALUES ('Григорий', 'Синьков', ''),
       ('Марат', 'Болатов', '');

INSERT INTO logistics_point (type, partner_id, market_id, contact_id, address_id, external_id, name, schedule_id,
                             active, cash_allowed, prepay_allowed, card_allowed, return_allowed, pickup_point_subtype,
                             location_zone_id)
VALUES ('PICKUP_POINT', 1, 1, 1, 1, 'R1', 'Склад Я.Маркета в Риге', 1, TRUE, FALSE, FALSE, FALSE, FALSE, 'TERMINAL', 1),
       ('PICKUP_POINT', 2, 2, 2, 2, 'main1', 'г.Котельники, Яничкин проезд, д.7', 2, TRUE, FALSE, FALSE, FALSE, FALSE,
        'PICKUP_POINT', 2),
       ('WAREHOUSE', 3, 1, 1, 3, 'R2', 'Какой-то адрес', 1, FALSE, FALSE, FALSE, FALSE, FALSE, NULL, 3),
       ('PICKUP_POINT', 3, 2, 2, 2, 'main2', 'г.Котельники, Яничкин проезд, д.7', 2, TRUE, FALSE, FALSE, FALSE, FALSE,
        'POST_OFFICE', NULL),
       ('WAREHOUSE', NULL, 2, 2, 4, 'main2', 'г.Котельники, Яничкин проезд, д.7', 2, TRUE, FALSE, FALSE, FALSE, FALSE,
        NULL, NULL),
        ('WAREHOUSE', 4, 1, 1, 5, 'R2', 'Какой-то адрес', 1, TRUE, FALSE, FALSE, FALSE, FALSE, NULL, NULL);


INSERT INTO phone (logistics_point_id, number, internal_number, comment, type)
VALUES (1, 79060312378, NULL, 'Всегда занят', 1),
       (1, 79060312378, NULL, NULL, 0),
       (2, 79254448812, '345', NULL, 0),
       (6, 71234567891, '345', NULL, 0);

INSERT INTO point_services(logistics_point_id, service_id)
VALUES (1, 1),
       (1, 2),
       (2, 1);

INSERT INTO regions (id, name, parent_id, type, path)
VALUES (10000, 'Земля', NULL, 15, '10000'),
       (10001, 'Евразия', 10000, 0, '10000.10001'),
       (225, 'Россия', 10001, 2, '10000.10001.225'),
       (59, 'Сибирский федеральный округ', 225, 3, '10000.10001.225.59'),
       (3, 'Центральный федеральный округ', 225, 3, '10000.10001.225.3'),
       (11266, 'Иркутская область', 59, 4, '10000.10001.225.59.11266'),
       (1, 'Москва и Московская область', 3, 4, '10000.10001.225.3.1'),
       (121076, 'Городской округ Братск', 11266, 9, '10000.10001.225.59.11266.121076'),
       (121006, 'Городской округ Котельники', 1, 9, '10000.10001.225.3.1.121006'),
       (976, 'Братск', 121076, 5, '10000.10001.225.59.11266.121076.976'),
       (21651, 'Котельники', 121006, 5, '10000.10001.225.3.1.121006.21651');

