INSERT INTO public.shipment_queue
(id, shipment_id, shipment_external_id, order_id, created, started, finished, attempt_count, last_processor_name, status, message)
VALUES
  (1, 1, 1, 1, '2017-04-25', '2017-04-19', '2017-04-27', 5, '', 2, 'message'),
  (2, 1, 1, 1, '2017-04-25', '2017-04-20', '2017-04-25', 5, '', 1, 'message'),
  (11, 1, 1, 3, '2017-04-25', '2017-04-19', '2017-04-27', 5, '', 0, 'message'),
  (12, 1, 1, 3, '2017-04-25', '2017-04-19', '2017-04-27', 5, '', 1, 'message'),
  (13, 1, 1, 3, '2017-04-25', '2017-04-19', '2017-04-27', 5, '', 2, 'message');
