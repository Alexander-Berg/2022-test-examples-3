INSERT INTO shipment_order
(market_id, tracking_id, delivery_id, sorting_center_id, shipment_id, register_id)
VALUES
(1, 'TRACK-1', 2, 131, NULL, NULL),
(2, 'TRACK-2', 2, 131, NULL, NULL),
(3, 'TRACK-3', 2, 666, NULL, NULL);

INSERT INTO shipment
(id, external_id, delivery_id, sorting_center_id, shipment_date)
VALUES
(1, 'SC-1', 2, 131, '2018-01-01'),
(2, 'SC-1', 3, 4, '2018-01-01'),
(3, 'SC-1', 2, 131, '2018-01-02'),
(4, 'SC-1', 2, 666, '2018-01-04');

INSERT INTO legal_info
(id, partner_id, incorporation, ogrn, url, legal_form, legal_inn, phone)
VALUES
(1, 131, 'ООО ТЕСТ', 9876543210, 'http://test.com', 'ООО', '9876543', '+7 (123) 456-78-90');
