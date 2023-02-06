INSERT INTO partner (id, name, status, type, billing_client_id, rating, tracking_type, location_id)
VALUES (1, 'Fulfillment partner', 'active', 'FULFILLMENT', 123, 1, null, 255),
       (2, 'Delivery service', 'active', 'DELIVERY', 123, 1, 'status1', null),
       (3, 'Delivery service1', 'active', 'DELIVERY', 123, 1, 'status1', null),
       (4, 'Fulfillment partner1', 'active', 'FULFILLMENT', 123, 1, null, 255),
       (5, 'Fulfillment partner2', 'active', 'FULFILLMENT', 123, 1, null, 255);


INSERT INTO private.settings_api (id, partner_id, token, format, version)
VALUES (1, 1, 'token', 'JSON', '1.0'),
       (2, 2, 'token2', 'JSON', '1.0'),
       (3, 3, 'token3', 'JSON', '1.0'),
       (4, 4, 'token4', 'JSON', '1.0'),
       (5, 5, 'token3', 'JSON', '1.0');

INSERT INTO settings_method (id, settings_api_id, method, active, url)
VALUES (1, 1, 'getReferenceTimetableCouriers', true, 'testurl'),
       (2, 2, 'getReferenceTimetableCouriers', true, 'testurl2'),
       (3, 4, 'getReferenceTimetableCouriers', false, 'testurl3'),
       (4, 3, 'getReferenceTimetableCouriers', true, 'testurl4'),
       (5, 5, 'some_method', true, 'testurl5'),
       (6, 5, 'another_method', true, 'testurl2');

INSERT INTO partner_delivery_interval_snapshots_requests(partner_id, update_time)
VALUES (1, '2019-02-14T19:09:53.202612'),
       (2, '2019-03-14T19:00:13.102612'),
       (3, '2012-07-10T00:00:13.202612'),
       (4, '2010-02-14T19:09:53.202612');

