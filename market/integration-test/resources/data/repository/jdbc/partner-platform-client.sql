INSERT INTO partner(id, name, status, type)
VALUES (123, 'Partner 123', 'active', 'FULFILLMENT'),
       (124, 'Partner 124', 'active', 'DELIVERY'),
       (100, 'RETAIL partner', 'active', 'RETAIL');

INSERT INTO platform_client(id, name)
VALUES (1, 'Беру'),
       (2, 'Брингли');

INSERT INTO platform_client_partners(partner_id, platform_client_id, id, status)
VALUES (123, 1, 1, 'ACTIVE'),
       (124, 1, 2, 'INACTIVE'),
       (123, 2, 3, 'FROZEN'),
       (124, 2, 4, 'TESTING'),
       (100, 1, 5, 'ACTIVE');
