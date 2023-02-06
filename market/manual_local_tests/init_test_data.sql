INSERT INTO permissions(uid, role)
VALUES (1120000000000000, 'global_params_writer'),
       (1120000000000000, 'global_wh_writer'),
       (1120000000000000, 'wh_0_adder'),
       (1120000000000000, 'wh_0_editor'),
       (1120000000000000, 'wh_1_adder'),
       (1120000000000000, 'wh_1_editor'),
       (1120000000000000, 'wh_3_adder'),
       (1120000000000000, 'wh_3_editor'),
       (1120000000000000, 'wh_4_adder'),
       (1120000000000000, 'wh_4_editor')
ON CONFLICT DO NOTHING;

ALTER TABLE warehouse ALTER COLUMN conductor_group_id SET DEFAULT 930081;  -- 930081 is market_wms_workstations_darkstore_ekb (not used now)
ALTER SEQUENCE warehouse_id_seq RESTART WITH 100;
INSERT INTO warehouse(id, ws_net_prefix, pr_net_prefix, tmp_net_prefix, timezone,
                      name, city, short_name, ws_zombie, pr_zombie)
VALUES (1, '2a02:6b0:0::/56', '2a02:5a0:0::/56', '2a02:7c0:0::/56', 'Europe/Moscow',
        'default_warehouse', 'default_city', 'defwh', 'zomb-adfox-3', 'zomb-market-affiliat'),
       (2, '2a02:6b1:0::/56', '2a02:5a1:0::/56', '2a02:7c1:0::/56', 'Europe/Astrakhan',
        'test2', 'test', 'tst2', 'zomb-adfox-2', 'zomb-auto-import'),
       (3, '2a02:6b2:0::/56', '2a02:5a2:0::/56', '2a02:7c2:0::/56', 'Asia/Yekaterinburg',
        'test3', 'test', 'tst3', 'zomb-adfox-3', 'zomb-adfox-2'),
       (4, '2a02:6b3:0::/56', '2a02:5a3:0::/56', '2a02:7c3:0::/56', 'Asia/Omsk',
        'test4', 'no_city', 'tst4', 'zomb-adfox-3', 'zomb-adfox-2'),
       (5, '2a02:6b4:0::/56', '2a02:5a4:0::/56', '2a02:7c4:0::/56', 'Asia/Tomsk',
        'test5', 'no_city', 'tst5', 'zomb-adfox-3', 'zomb-adfox-2')
ON CONFLICT DO NOTHING;
UPDATE warehouse SET conductor_group_id = 930081;  -- 930081 is market_wms_workstations_darkstore_ekb (not used now)

ALTER SEQUENCE type_workstation_id_seq RESTART WITH 100;
INSERT INTO type_workstation (id, name, short_name, description)
VALUES (1, 'default_type', 'def', 'Just default type_workstation'),
       (2, 'test1', 'tst1', 'test1'),
       (3, 'test2', 'tst2', 'test2'),
       (4, 'test3', 'tst3', 'test3')
ON CONFLICT DO NOTHING;

ALTER SEQUENCE model_workstation_id_seq RESTART WITH 100;
INSERT INTO model_workstation (id, name, vendor, description)
VALUES (1, 'default_model_workstation', 'default vendor', 'Just default model_workstation'),
       (2, 'test1', 'test', 'test1'),
       (3, 'test2', 'test', 'test2'),
       (4, 'test3', 'test', 'test3')
ON CONFLICT DO NOTHING;

ALTER SEQUENCE model_printer_id_seq RESTART WITH 100;
INSERT INTO model_printer (id, name, short_name, vendor, type_wms, description)
VALUES (1, 'default_model_printer', 'def', 'default vendor', 245, 'Just default model_printer'),
       (2, 'test1', 'tst1', 'test', 245, 'test1'),
       (3, 'test2', 'tst2', 'test', 245, 'test2'),
       (4, 'test3', 'tst3', 'test', 245, 'test3')
ON CONFLICT DO NOTHING;

ALTER SEQUENCE model_scanner_id_seq RESTART WITH 100;
INSERT INTO model_scanner (id, name, vendor, description)
VALUES (1, 'default_model_scanner', 'default vendor', 'Just default model_scanner'),
       (2, 'test1', 'test', 'test1'),
       (3, 'test2', 'test', 'test2'),
       (4, 'test3', 'test', 'test3')
ON CONFLICT DO NOTHING;

ALTER SEQUENCE cert_8021x_id_seq RESTART WITH 100;
INSERT INTO cert_8021x (id, cert_id, state, yav_secret_id, yav_secret_version, download_before)
VALUES (1, 'cert01', 'issued', 'yav_secret_id01', 'yav_secret_version01', '2030-01-01 00:00:00'),
       (2, 'cert02', 'issued', 'yav_secret_id02', 'yav_secret_version02', '2030-01-01 00:00:00'),
       (3, 'cert03', 'issued', 'yav_secret_id03', 'yav_secret_version03', '2030-01-01 00:00:00'),
       (4, 'cert04', 'revoked', 'yav_secret_id04', 'yav_secret_version04', '2030-01-01 00:00:00'),
       (5, NULL, 'empty', NULL, NULL, '1970-01-01 00:00:00'),
       (6, 'cert06', 'issued', 'yav_secret_id06', 'yav_secret_version06', '2030-01-01 00:00:00'),
       (7, 'cert07', 'issued', 'yav_secret_id07', 'yav_secret_version07', '2030-01-01 00:00:00'),
       (8, 'cert08', 'revoked', 'yav_secret_id08', 'yav_secret_version08', '2030-01-01 00:00:00'),
       (9, NULL, 'empty', NULL, NULL, '1970-01-01 00:00:00'),
       (10, 'cert10', 'issued', 'yav_secret_id10', 'yav_secret_version10', '2030-01-01 00:00:00'),
       (11, 'cert11', 'issued', 'yav_secret_id11', 'yav_secret_version11', '2030-01-01 00:00:00'),
       (12, 'cert12', 'revoked', 'yav_secret_id12', 'yav_secret_version12', '2030-01-01 00:00:00'),
       (13, 'cert13', 'issued', 'yav_secret_id13', 'yav_secret_version13', '2030-01-01 00:00:00'),
       (14, 'cert14', 'issued', 'yav_secret_id14', 'yav_secret_version15', '2030-01-01 00:00:00'),
       (15, 'cert15', 'issued', 'yav_secret_id16', 'yav_secret_version16', '2030-01-01 00:00:00'),
       (16, NULL, 'empty', NULL, NULL, '1970-01-01 00:00:00'),
       (17, NULL, 'empty', NULL, NULL, '1970-01-01 00:00:00'),
       (18, NULL, 'empty', NULL, NULL, '1970-01-01 00:00:00'),
       (19, 'cert19', 'revoked', 'yav_secret_id19', 'yav_secret_version19', '2030-01-01 00:00:00'),
       (20, 'cert20', 'issued', 'yav_secret_id20', 'yav_secret_version20', '2030-01-01 00:00:00'),
       (21, NULL, 'empty', NULL, NULL, '1970-01-01 00:00:00'),
       (22, 'cert22', 'issued', 'yav_secret_id22', 'yav_secret_version22', '2030-01-01 00:00:00')
ON CONFLICT DO NOTHING;

ALTER SEQUENCE workstation_id_seq RESTART WITH 100;
INSERT INTO workstation (id, position, serial_number, inventory_number,
                         wireless, mac_address, cert_8021_x_id,
                         warehouse_id, type_id, status, model_workstation_id)
VALUES (1, 1, 'default_serial_workstation', 'tdefwsinv', FALSE, 'AC:1F:6B:4A:EF:0A', 1, 1, 1, 'active', 1),
       (2, 2, 'test_1ws_serial', 't1wsinv', FALSE, 'EC:1F:7B:40:EF:1A', 1, 1, 1, 'not_active', 1),
       (3, 3, 'test_2ws_serial', 't2wsinv', FALSE, 'BC:1F:7B:40:EF:2A', 2, 2, 2, 'active', 2),
       (4, 4, 'test_3ws_serial', 't3wsinv', TRUE, 'BC:1F:7B:40:EF:3A', 3, 3, 3, 'active', 3),
       (5, 5, 'test_4ws_serial', 't4wsinv', FALSE, 'BC:1F:7B:40:EF:4A', 4, 1, 1, 'active', 1),
       (6, 6, 'test_5ws_serial', 't5wsinv', FALSE, 'BC:1F:7B:40:EF:5A', 5, 1, 1, 'active', 1),
       (7, 6, 'test_6ws_serial', 't6wsinv', TRUE, 'BC:1F:7B:40:EF:6A', 6, 1, 1, 'active', 1),
       (8, 6, 'test_7ws_serial', 't7wsinv', FALSE, 'BC:1F:7B:40:EF:7A', 7, 1, 1, 'active', 1),
       (9, 6, 'test_8ws_serial', 't8wsinv', TRUE, 'BC:1F:7B:40:EF:8A', 8, 1, 1, 'active', 1),
       (10, 6, 'test_9ws_serial', 't9wsinv', FALSE, 'BC:1F:7B:40:EF:9A', 9, 1, 1, 'active', 1)
ON CONFLICT DO NOTHING;

ALTER SEQUENCE printer_id_seq RESTART WITH 100;
INSERT INTO printer (id, serial_number, inventory_number, wireless, mac_address, ip,
                     cert_8021_x_id, warehouse_id, workstation_id, model_printer_id)
VALUES (1, 'default_serial_printer', 'tdefpinv', FALSE, 'BC:1F:7B:40:EF:21', NULL, 10, 1, 1, 1),
       (2, 'test_1p_serial', 't1pinv', FALSE, 'BC:1F:7B:40:EF:22', NULL, 11, 1, 1, 1),
       (2, 'test_2p_serial', 't2pinv', TRUE, 'BC:1F:7B:40:EF:23', '10.10.10.11', 12, 2, 2, 2),
       (4, 'test_3p_serial', 't3pinv', FALSE, 'BC:1F:7B:40:EF:24', NULL, 13, 3, 3, 3),
       (5, 'test_4p_serial', 't4pinv', TRUE, 'BC:1F:7B:40:EF:25', NULL, 14, 1, 4, 1),
       (6, 'test_5p_serial', 't5pinv', FALSE, 'BC:1F:7B:40:EF:26', NULL, 15, 1, 5, 1),
       (7, 'test_6p_serial', 't6pinv', TRUE, 'BC:1F:7B:40:EF:27', '2a02:6b8:0:5e0d:2c0:ebff:fe11:a7ca', 16, 1, 1, 1),
       (8, 'test_7p_serial', 't7pinv', FALSE, 'BC:1F:7B:40:EF:28', NULL, 17, 1, 2, 1)
ON CONFLICT DO NOTHING;
INSERT INTO printer (id, serial_number, inventory_number, wireless, mac_address, label_size_wms, dns_cname, ip,
                     cert_8021_x_id, warehouse_id, workstation_id, model_printer_id)
VALUES (9, 'test_9p_serial', 't9pinv', FALSE, 'BC:1F:7B:40:EF:29', 7, 'test-9p-serial', '10.10.10.10', 18, 3, 3, 3),
       (10, 'test_10p_serial', 't10pinv', TRUE, 'BC:1F:7B:40:EF:30', 7, 'test-10p-cname', NULL, 19, 1, 4, 1),
       (11, 'test_11p_serial', 't11pinv', FALSE, 'BC:1F:7B:40:EF:31', 3, 'test-11p-cname',
        '2a02:6b8:0:5e0f:96c6:91ff:fea4:6999', 20, 1, 5, 1),
       (12, 'test_12p_serial', 't12pinv', TRUE, 'BC:1F:7B:40:EF:32', 6, 'test-12p-cname', NULL, 21, 1, NULL, 1),
       (13, 'test_13p_serial', 't13pinv', FALSE, 'BC:1F:7B:40:EF:33', 8, 'test-13p-cname',
        '2a02:6b8:0:5e0d:2c0:ebff:fe10:d16d', 22, 1, NULL, 1)
ON CONFLICT DO NOTHING;

ALTER SEQUENCE scanner_id_seq RESTART WITH 100;
INSERT INTO scanner (id, serial_number, inventory_number, warehouse_id, workstation_id, model_scanner_id)
VALUES (1, 'default_serial_scanner', 'ultinv', 1, 1, 1),
       (2, 'test_1s_serial', '_1sinv', 1, 1, 1),
       (3, 'test_2s_serial', '_2sinv', 2, 2, 2),
       (4, 'test_3s_serial', '_3sinv', 3, 3, 3),
       (5, 'test_4s_serial', '_4sinv', 1, 4, 1),
       (6, 'test_5s_serial', '_5sinv', 1, 5, 1),
       (7, 'test_6s_serial', '_6sinv', 1, NULL, 1),
       (8, 'test_7s_serial', '_7sinv', 1, NULL, 1)
ON CONFLICT DO NOTHING;

ALTER SEQUENCE work_page_id_seq RESTART WITH 100;
INSERT INTO work_page (id, url, type_id, warehouse_id)
VALUES (1, 'https://defaulturl.ru/default', 1, 1),
       (2, 'https://test1url.ru/test', 1, 2),
       (3, 'https://test2url.ru/test', 2, 2),
       (4, 'https://test3url.ru/test', 3, 3),
       (5, 'https://test4url.ru/test', 1, 2),
       (6, 'https://test5url.ru/test', 2, 3),
       (7, 'https://test6url.ru/test', 3, 1),
       (8, 'https://test7url.ru/test', 1, 2)
ON CONFLICT DO NOTHING;

ALTER SEQUENCE default_flag_id_seq RESTART WITH 100;
INSERT INTO default_flag (id, flag, type_id)
VALUES (1, 'flag1', 1),
       (2, 'flag2', 2),
       (3, 'flag3', 3),
       (4, 'flag4', 4),
       (5, 'flag5', 1),
       (6, 'flag6', 2),
       (7, 'flag7', 3),
       (8, 'flag8', 1)
ON CONFLICT DO NOTHING;

ALTER SEQUENCE configuration_flag_id_seq RESTART WITH 100;
INSERT INTO configuration_flag (id, flag, workstation_id)
VALUES (1, 'flag1', 1),
       (2, 'flag2', 2),
       (3, 'flag3', 3),
       (4, 'flag4', 1),
       (5, 'flag5', 2),
       (6, 'flag6', 3),
       (7, 'flag7', 1)
ON CONFLICT DO NOTHING;
