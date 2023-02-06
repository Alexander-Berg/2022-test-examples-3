INSERT INTO partner (id, market_id, name, readable_name, status, type, billing_client_id,
                     rating, domain, tracking_type, stock_sync_enabled, auto_switch_stock_sync_enabled)
VALUES (1, 1, 'Delivery service 1', 'ReadableName', 'active', 'DELIVERY', 123, 1, 'www.test-domain-1.com', 'status1', false, false);

INSERT INTO schedule (id)
VALUES (1001);

INSERT INTO calendar (id, parent_id)
VALUES (1001, 1);

INSERT INTO delivery_interval (id, schedule_id, partner_id, location_id, calendar_id)
VALUES  (1001, 1001, 1, 213, 1001);

INSERT INTO schedule_day (id, schedule_id, day, time_from, time_to)
VALUES (1001, 1001, 1, '10:00', '18:00'),
       (1002, 1001, 2, '11:00', '19:00');

INSERT INTO calendar_day (id, calendar_id, is_holiday, day)
VALUES (1001, 1001, true, '2018-10-18');
