INSERT INTO schedule
VALUES (DEFAULT),
       (DEFAULT),
       (DEFAULT),
       (DEFAULT);

INSERT INTO schedule_day (schedule_id, "day", time_from, time_to)
VALUES (1, 1, '10:00', '18:00'),
       (1, 2, '12:00', '16:00'),
       (1, 1, '09:00', '18:00'),
       (1, 1, '10:00', '16:00'),
       (2, 3, '16:00', '18:00'),
       (2, 4, '06:00', '09:00');

INSERT INTO partner (id, market_id, name, readable_name, status, type, billing_client_id, rating, domain, logo_url)
VALUES (10, 829721, 'FulfillmentService1', 'Fulfillment service 1', 'active', 'FULFILLMENT', 123, 1,
        'first.ff.example.com', 'http://test.logo.first'),
       (14, 829722, 'FulfillmentService1', 'Fulfillment service 1', 'active', 'DELIVERY', 123, 1,
        'first.delivery.example.com', 'http://test.logo.delivery');

INSERT INTO schedule_day (schedule_id, "day", time_from, time_to)
VALUES (3, 2, '12:00', '16:30'),
       (3, 1, '14:00', '18:00'),
       (4, 5, '11:00', '19:00'),
       (4, 7, '06:30', '09:00');

INSERT INTO calendar(id)
VALUES (1),
       (2);

INSERT INTO delivery_interval (partner_id, schedule_id, location_id, calendar_id)
VALUES (10, 3, 10000, 1),
       (10, 4, 214, 2),
       (14, 1, 213, null),
       (14, null, 225, 2);
