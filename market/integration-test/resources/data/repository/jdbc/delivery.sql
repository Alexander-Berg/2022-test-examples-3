INSERT INTO platform_client(id, name)
VALUES (1, 'Беру'),
       (2, 'Брингли');

INSERT INTO partner(id, name, status, type)
VALUES (123, 'Partner 123', 'active', 'FULFILLMENT');

INSERT INTO partner(id, readable_name, name, tracking_type, rating, location_id, status, type)
VALUES (124, 'My delivery', 'My delivery name', 'tt1', 1, 1, 'active', 'DELIVERY');


INSERT INTO schedule(id)
VALUES
    -- delivery_interval
    (1),
    -- partner_route
    (2),
    -- logistics points
    (3),
    (4),
    (5),
    (6);
INSERT INTO schedule_day(id, schedule_id, day, time_from, time_to)
VALUES
    -- delivery_interval
    (1, 1, 2, '10:00', '20:00'),
    -- partner_route
    (2, 2, 1, '00:00', '23:59'),
    -- logistics point 1
    (3, 3, 1, '10:00', '20:00'),
    (4, 3, 2, '10:00', '20:00'),
    (5, 3, 5, '10:00', '20:00'),
    -- logistics point 3
    (6, 5, 1, '10:00', '20:00'),
    (7, 5, 2, '10:00', '20:00'),
    (8, 5, 5, '10:00', '20:00');

INSERT INTO address(id, location_id)
VALUES (1, 1),
       (2, 1),
       (3, 1),
       (4, 1);
INSERT INTO logistics_point(id, partner_id, external_id, external_hash,
                            active, type, schedule_id, address_id)
VALUES (1, 124, '1', '1', TRUE, 'WAREHOUSE', 3, 1),
       (2, 124, '2', '2', TRUE, 'WAREHOUSE', 4, 2),
       (3, 124, '3', '3', TRUE, 'PICKUP_POINT', 5, 3),
       (4, 124, '4', '4', FALSE, 'PICKUP_POINT', 6, 4);

INSERT INTO partner_route(id, partner_id, location_from, location_to, schedule_id)
VALUES (1, 124, 225, 1, 2);

INSERT INTO partner_capacity(id, partner_id, type, service_type, delivery_type, counting_type, location_from,
                             location_to, value, platform_client_id, day)
VALUES (1, 124, 'regular', 'delivery', 'courier', 'item', 225, 2, 10, 1, '2020-09-07'),
       (2, 124, 'regular', 'delivery', 'courier', 'item', 225, 2, 10, 1, '2020-09-06'),
       (3, 124, 'regular', 'delivery', 'courier', 'item', 225, 2, 10, 2, '2020-09-07'),
       (4, 124, 'regular', 'delivery', 'courier', 'item', 225, 2, 10, 2, '2020-09-06'),
       (5, 124, 'regular', 'delivery', 'courier', 'item', 225, 2, 10, 1, NULL);
INSERT INTO partner_capacity_day_off(id, capacity_id, day)
VALUES (1, 1, '2020-09-07'),
       (2, 2, '2020-09-06'),
       (3, 3, '2020-09-07'),
       (4, 4, '2020-09-06');

INSERT INTO calendar(id, parent_id)
VALUES (1, NULL);


INSERT INTO delivery_interval(id, partner_id, calendar_id, location_id, schedule_id)
VALUES (1, 124, 1, 2, 1);

INSERT INTO partner_handling_time(id, partner_id, handling_time, location_from, location_to)
VALUES (1, 124, 3600000000000, 2, 3);

INSERT INTO partner_external_param_type (id, key)
VALUES (1, 'IS_DROPOFF');

INSERT INTO partner_external_param_value (id, type_id, partner_id, value)
VALUES (1, 1, 124, '1');
