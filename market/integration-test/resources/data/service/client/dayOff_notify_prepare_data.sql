INSERT INTO partner (id, market_id, name, readable_name, status, type, billing_client_id, rating, domain,
                     logo_url,
                     intake_schedule)
VALUES (1, 829721, 'FulfillmentService1', 'Fulfillment service 1', 'active', 'FULFILLMENT', 123, 1,
        'first.ff.example.com', 'http://test.logo.first', NULL),
       (2, 829722, 'DeliveryService1', 'Delivery Service 1', 'active', 'DELIVERY', 123, 1,
        'first.ds.example.com', 'http://test.ds.logo.first', NULL);

INSERT INTO platform_client (id, name)
VALUES (3901, 'Beru'),
       (3902, 'Bringly'),
       (3903, 'Yandex Delivery');

INSERT INTO partner_capacity (id, partner_id, location_from, location_to, delivery_type, type,
                              service_type, platform_client_id, day, value)
VALUES (1, 1, 1, 213, 'courier', 'regular', 'shipment', 3901, '2019-01-01', 100),
       (2, 1, 21, 22, 'pickup', 'regular', 'shipment', 3902, '2019-01-02', 200),
       (3, 1, 31, 32, 'post', 'reserve', 'shipment', 3903, '2019-01-03', 300),
       (4, 1, 41, 42, NULL, 'regular', 'shipment', 3903, '2019-01-04', 400),
       (5, 2, 41, 42, NULL, 'regular', 'delivery', 3903, NULL, 400);

INSERT INTO partner_capacity_day_off (capacity_id, day)
VALUES (1, '2019-05-01'),
       (1, '2019-05-02'),
       (1, '2019-05-03');

INSERT INTO partner_capacity_day_off (capacity_id, day)
VALUES (5, '2019-05-01'),
       (5, '2019-05-02'),
       (5, '2019-05-03');
