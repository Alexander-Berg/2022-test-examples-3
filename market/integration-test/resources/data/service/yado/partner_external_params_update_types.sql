INSERT INTO partner_external_param_type (id, key, description)
VALUES (111, 'UPDATE_RECIPIENT_ENABLED', 'параметр 1'),
       (112, 'UPDATE_ADDRESS_ENABLED', 'параметр 2'),
       (113, 'UPDATE_DELIVERY_DATE_ENABLED', 'параметр 3'),
       (114, 'DEFAULT_DROP_SHIP_DELIVERY', 'параметр 4'),
       (115, 'DEFAULT_DROP_SHIP_SORTING_CENTER', 'параметр 5'),
       (116, 'DEFAULT_SUPPLIER_FULFILLMENT', 'параметр 6');

INSERT INTO partner_external_param_value (partner_id, type_id, value)
VALUES (1, 111, '1'),
       (1, 112, '1'),
       (1, 113, '0'),
       (1, 114, '0'),
       (1, 115, '0'),
       (1, 116, '0');
