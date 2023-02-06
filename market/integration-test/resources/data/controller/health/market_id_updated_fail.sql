INSERT INTO partner (id, market_id, name, readable_name, status, type, billing_client_id, rating, domain,
                     logo_url)
VALUES (1, 829721, 'FulfillmentService1', 'Fulfillment service 1', 'active', 'FULFILLMENT', 123, 1, 'first.ff.example.com', 'http://test.logo.first'),
       (2, 829722, 'DeliveryService1', 'Delivery service 1', 'active', 'DELIVERY', 123, 1, 'first.ds.example.com', 'http://test.logo.second'),
       (3, 829723, 'FulfillmentService2', 'Fulfillment service 2', 'inactive', 'FULFILLMENT', 1234, 12, 'second.ff.example.com', 'http://test.logo.third'),
       (4, 829724, 'DeliveryService2', 'Delivery service 2', 'frozen', 'DELIVERY', 1234, 12, 'second.ds.example.com', 'http://test.logo.fourth');

INSERT INTO partner_market_id_status(partner_id, status, created, updated)
VALUES (1, 'ERROR', '2019-10-21T18:09:53.202612', '2019-10-21T18:09:53.202612'),
       (2, 'OK', '2019-10-21T19:09:53.202612', '2019-10-21T19:09:53.202612'),
       (3, 'ERROR', '2019-10-21T20:09:53.202612', '2019-10-21T20:09:53.202612'),
       (4, 'EMPTY_OGRN', '2019-10-21T20:09:53.202612', '2019-10-21T20:09:53.202612');