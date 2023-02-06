INSERT INTO shipment_order
(market_id, tracking_id, delivery_id, sorting_center_id, shipment_id, register_id)
VALUES
(1, 'TRACK-1', 2, 131, NULL, NULL),
(2, 'TRACK-2', 2, 131, NULL, NULL);

INSERT INTO shipment
(id, external_id, delivery_id, sorting_center_id, shipment_date)
VALUES
(1, 'SC-1', 2, 131, '2018-01-01'),
(2, 'SC-1', 3, 4, '2018-01-01'),
(3, 'SC-1', 2, 131, '2018-01-02');

INSERT INTO register
(id, external_id, shipment_id, created, updated, status)
VALUES
(1, '', 1, '12018-01-01 22:10:52.688889', '12018-01-01 22:10:52.688889', 'CREATING'),
(2, '', 1, '12018-01-01 22:10:52.688889', '12018-01-01 22:10:52.688889', 'CREATING'),
(3, '', 2, '12018-01-01 22:10:52.688889', '12018-01-01 22:10:52.688889', 'CREATING'),
(4, '', 3, '12018-01-01 22:10:52.688889', '12018-01-01 22:10:52.688889', 'CREATING');