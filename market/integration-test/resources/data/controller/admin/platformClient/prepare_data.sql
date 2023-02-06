TRUNCATE platform_client RESTART IDENTITY CASCADE;

INSERT INTO platform_client (name)
VALUES ('Beru'),
       ('Bringly'),
       ('Yandex Delivery'),
       ('Yandex Market');

INSERT INTO partner (name, status, type)
VALUES ('Test partner 1', 'active', 'FULFILLMENT'),
       ('Test partner 2', 'active', 'DELIVERY');

INSERT INTO platform_client_partners (platform_client_id, partner_id, status)
VALUES (1, 2, 'ACTIVE'),
       (3, 1, 'ACTIVE');
