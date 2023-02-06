BEGIN TRANSACTION;
INSERT INTO public.shipment_order (market_id, tracking_id, delivery_id, sorting_center_id, shipment_id, register_id)
VALUES (1, 'TRACK-1', 106, 131, NULL, NULL);
INSERT INTO public.shipment (id, external_id, delivery_id, sorting_center_id, shipment_date)
VALUES (1, 'SC-1', 106, 131, CURRENT_DATE);
INSERT INTO public.register_schedule (delivery_id, sorting_center_id, day, time_from, time_to)
VALUES (106, 131, 1, '00:00:00', '23:59:59');
INSERT INTO public.register_schedule (delivery_id, sorting_center_id, day, time_from, time_to)
VALUES (106, 131, 2, '00:00:00', '23:59:59');
INSERT INTO public.register_schedule (delivery_id, sorting_center_id, day, time_from, time_to)
VALUES (106, 131, 3, '00:00:00', '23:59:59');
INSERT INTO public.register_schedule (delivery_id, sorting_center_id, day, time_from, time_to)
VALUES (106, 131, 4, '00:00:00', '23:59:59');
INSERT INTO public.register_schedule (delivery_id, sorting_center_id, day, time_from, time_to)
VALUES (106, 131, 5, '00:00:00', '23:59:59');
INSERT INTO public.register_schedule (delivery_id, sorting_center_id, day, time_from, time_to)
VALUES (106, 131, 6, '00:00:00', '23:59:59');
INSERT INTO public.register_schedule (delivery_id, sorting_center_id, day, time_from, time_to)
VALUES (106, 131, 7, '00:00:00', '23:59:59');
COMMIT;