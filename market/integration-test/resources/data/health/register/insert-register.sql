INSERT INTO shipment_order
(market_id, tracking_id, delivery_id, sorting_center_id, shipment_id, created, register_id)
VALUES
(1, 'TRACK-1', 2, 131, NULL, '2018-01-01 22:10:52.688889', NULL),
(2, 'TRACK-2', 2, 131, NULL, '2017-01-01 22:10:52.688889', NULL);

INSERT INTO shipment
(id, external_id, delivery_id, sorting_center_id, shipment_date)
VALUES
(1, 'SC-1', 2, 131, '2018-01-01'),
(2, 'SC-1', 3, 4, '2018-01-01'),
(3, 'SC-1', 2, 131, '2018-01-02');

INSERT INTO register
(id, external_id, shipment_id, created, updated, status)
VALUES
(1, '', 1, '2018-01-01 22:10:52.688889', '2018-01-01 22:10:52.688889', 'NEW'),
(2, '', 1, '2018-01-01 22:10:52.688889', '2018-01-01 22:10:52.688889', 'CREATING'),
(3, '', 2, '2018-01-01 22:10:52.688889', '2018-01-01 22:10:52.688889', 'CREATED'),
(4, '', 3, '2018-01-01 22:10:52.688889', '2018-01-01 22:10:52.688889', 'ERROR'),
(5, '', 1, '2018-01-01 20:10:52.688889', '2018-01-01 20:10:52.688889', 'CREATING');
