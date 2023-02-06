INSERT INTO partner_customer_info (name, phones, track_order_site, track_code_source)
VALUES ('PartnerOne', '{"+7-(912)-345-67-89", "+7-(912)-345-67-88"}', 'www.partner1-site.ru', 'ORDER_NO'),
       ('PartnerTwo', '{"+7-(912)-345-67-80", "+7-(912)-345-67-81"}', 'www.partner2-site.ru', 'DS_TRACK_CODE');

INSERT INTO partner_subtype (id, name, partner_type)
VALUES (1, 'Партнерская доставка (контрактная)', 'DELIVERY'),
       (2, 'Маркет Курьер', 'DELIVERY');

INSERT INTO partner (id, market_id, name, readable_name, status, type, billing_client_id, rating,
                     partner_customer_info_id, subtype_id)
VALUES (1001, 829721, 'DeliveryService1', 'Fulfillment service 1', 'active', 'DELIVERY', 123, 1, 1, 1),
       (1002, 829722, 'DeliveryService2', 'Delivery service 2', 'active', 'DELIVERY', 123, 1, 1, 2),
       (111, 829723, 'DeliveryService3', 'Delivery service 3', 'active', 'DELIVERY', 124, 1, 2, 1),
       (112, 829723, 'DeliveryService4', 'Delivery service 4', 'active', 'DELIVERY', 124, 1, null, 2);

INSERT INTO possible_order_change(partner_id, type, method, checkpoint_status_from, checkpoint_status_to)
VALUES (1001, 'DELIVERY_DATES', 'PARTNER_API', null, 10),
       (1001, 'RECIPIENT', 'PARTNER_API', null, 10),
       (1001, 'DELIVERY_DATES', 'PARTNER_SITE', null, 48),
       (1001, 'RECIPIENT', 'PARTNER_SITE', null, 48),
       (1001, 'DELIVERY_DATES', 'PARTNER_PHONE', null, 48),
       (1001, 'RECIPIENT', 'PARTNER_PHONE', null, 48),
       (1001, 'EXTEND_PICKUP_STORAGE_TIME', 'PARTNER_SITE', null, 48),
       (1001, 'EXTEND_PICKUP_STORAGE_TIME', 'PARTNER_PHONE', null, 48),
       (1001, 'ORDER_ITEMS', 'PARTNER_API', 113, 114),
       (111, 'DELIVERY_DATES', 'PARTNER_SITE', null, 48),
       (111, 'RECIPIENT', 'PARTNER_SITE', null, 48),
       (111, 'DELIVERY_DATES', 'PARTNER_PHONE', null, 48),
       (111, 'RECIPIENT', 'PARTNER_PHONE', null, 48),
       (111, 'EXTEND_PICKUP_STORAGE_TIME', 'PARTNER_SITE', null, 48),
       (111, 'EXTEND_PICKUP_STORAGE_TIME', 'PARTNER_PHONE', null, 48);
