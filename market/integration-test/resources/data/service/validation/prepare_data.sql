INSERT INTO partner (id, market_id, name, readable_name, status, type, billing_client_id, rating, domain,
                     logo_url, intake_schedule, location_id)
VALUES (1, 829721, 'FulfillmentService1', 'Fulfillment service 1', 'active', 'FULFILLMENT', 123, 1,
        'first.ff.example.com', 'http://test.logo.first', NULL,  1),
       (2, 829722, 'DeliveryService1', 'Delivery service 1', 'active', 'DELIVERY', 123, 1, 'first.ds.example.com',
        'http://test.logo.second', NULL, null),
       (3, 829723, 'FulfillmentService2', 'Fulfillment service 2', 'inactive', 'FULFILLMENT', 1234, 12,
        'second.ff.example.com', 'http://test.logo.third', NULL, null),
       (4, 829724, 'DeliveryService2', 'Delivery service 2', 'frozen', 'DELIVERY', 1234, 12, 'second.ds.example.com',
        'http://test.logo.fourth', null, null),
       (5, 829725, 'SortingCenter', 'Sorting Center', 'active', 'SORTING_CENTER', 1234, 12, 'sc.example.com',
        'http://test.logo.fifth', NULL, null);

INSERT INTO schedule VALUES (DEFAULT), (DEFAULT);

INSERT INTO address (id, address_string, location_id)
VALUES (1, 'some address', 1),
       (2, 'another address', null);

INSERT INTO partner_route (id, schedule_id, location_from, location_to, partner_id)
VALUES (1, 1, 1, 225, 4),
       (2, 2, 2, 225, 2);

INSERT INTO logistics_point (id, market_id, address_id, external_id, type, active, partner_id,
                            schedule_id)
VALUES (1, 123, 1, 'warehouse1', 'WAREHOUSE', TRUE, 1, NULL),
       (2, 345, 2, 'warehouse2', 'WAREHOUSE', TRUE, 2, NULL),
       (3, 910, 2, 'pickup1', 'PICKUP_POINT', TRUE, 3, NULL);

INSERt INTO regions (id, name, parent_id)
VALUES (1, 'Москва', 2),
       (2, 'Россия', 3),
       (3, 'Евразия', null);
