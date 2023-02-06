--queue (id, hostname, http_port, zk_port)

INSERT INTO queue VALUES (0, 'man1-6099-zoolooser-mailsearch-in-b03-18662.gencfg-c.yandex.net', 18663, 18662);
INSERT INTO queue VALUES (0, 'man2-1055-556-zoolooser-mailsearc-b03-18662.gencfg-c.yandex.net', 18663, 18662);
INSERT INTO queue VALUES (0, 'man2-1056-8cc-zoolooser-mailsearc-b03-18662.gencfg-c.yandex.net', 18663, 18662);
INSERT INTO queue VALUES (0, 'myt1-0091-zoolooser-mailsearch-in-b03-18662.gencfg-c.yandex.net', 18663, 18662);
INSERT INTO queue VALUES (0, 'myt1-0104-zoolooser-mailsearch-in-b03-18662.gencfg-c.yandex.net', 18663, 18662);
INSERT INTO queue VALUES (0, 'myt1-0174-zoolooser-mailsearch-in-b03-18662.gencfg-c.yandex.net', 18663, 18662);
INSERT INTO queue VALUES (0, 'sas2-5135-179-zoolooser-mailsearc-b03-18662.gencfg-c.yandex.net', 18663, 18662);
INSERT INTO queue VALUES (0, 'sas2-5142-711-zoolooser-mailsearc-b03-18662.gencfg-c.yandex.net', 18663, 18662);
INSERT INTO queue VALUES (0, 'sas2-5152-aec-zoolooser-mailsearc-b03-18662.gencfg-c.yandex.net', 18663, 18662);


--host_group (id, hostname, search_port, search_port_ng, index_port, dump_port, queue_id_port, freshness, dc)

INSERT INTO host_group VALUES (0, 'host1', 26763, 26764, 26767, 26769, 26767, true, 'man');
INSERT INTO host_group VALUES (0, 'host2', 18065, 18066, 18069, 18071, 18069, true, 'sas');
INSERT INTO host_group VALUES (0, 'host3', 26763, 26764, 26767, 26769, 26767, true, 'vla');

INSERT INTO host_group VALUES (1, 'host4', 26763, 26764, 26767, 26769, 26767, true, 'man');
INSERT INTO host_group VALUES (1, 'host5', 26763, 26764, 26767, 26769, 26767, true, 'sas');

INSERT INTO host_group VALUES (2, 'host1', 26763, 26764, 26765, 26769, 26765, true, 'man');
INSERT INTO host_group VALUES (2, 'host2', 18065, 18066, 18067, 18071, 18067, true, 'sas');
INSERT INTO host_group VALUES (2, 'host3', 26763, 26764, 26765, 26769, 26765, true, 'vla');

INSERT INTO host_group VALUES (3, 'host4', 26763, 26764, 26765, 26769, 26765, true, 'man');
INSERT INTO host_group VALUES (3, 'host5', 26763, 26764, 26765, 26769, 26765, true, 'sas');

INSERT INTO host_group VALUES (4, 'host6', 26763, 26764, 26767, 26769, 26767, true, 'vla');
INSERT INTO host_group VALUES (4, 'host7', 26763, 26764, 26767, 26769, 26767, true, 'sas');


--metashard (service, shard, label, queue_id, host_group_id, version)

--MAIL_CORP
INSERT INTO metashard VALUES('corp_change_log', 0, 0, 0, 4, 0);
INSERT INTO metashard VALUES('corp_change_log', 1, 0, 0, 4, 0);
INSERT INTO metashard VALUES('corp_change_log', 2, 0, 0, 4, 0);
INSERT INTO metashard VALUES('corp_change_log', 3, 0, 0, 4, 0);
INSERT INTO metashard VALUES('corp_change_log', 4, 0, 0, 4, 0);
INSERT INTO metashard VALUES('corp_change_log', 5, 0, 0, 4, 0);
INSERT INTO metashard VALUES('corp_change_log', 6, 0, 0, 4, 0);
INSERT INTO metashard VALUES('corp_change_log', 7, 0, 0, 4, 0);
INSERT INTO metashard VALUES('corp_change_log', 8, 0, 0, 4, 0);
INSERT INTO metashard VALUES('corp_change_log', 9, 0, 0, 4, 0);
INSERT INTO metashard VALUES('corp_change_log', 10, 0, 0, 4, 0);

INSERT INTO metashard VALUES('corp_change_log_offline', 0, 0, 0, 4, 3);
INSERT INTO metashard VALUES('corp_change_log_offline', 1, 0, 0, 4, 3);
INSERT INTO metashard VALUES('corp_change_log_offline', 2, 0, 0, 4, 3);
INSERT INTO metashard VALUES('corp_change_log_offline', 3, 0, 0, 4, 3);
INSERT INTO metashard VALUES('corp_change_log_offline', 4, 0, 0, 4, 3);
INSERT INTO metashard VALUES('corp_change_log_offline', 5, 0, 0, 4, 3);
INSERT INTO metashard VALUES('corp_change_log_offline', 6, 0, 0, 4, 3);
INSERT INTO metashard VALUES('corp_change_log_offline', 7, 0, 0, 4, 3);
INSERT INTO metashard VALUES('corp_change_log_offline', 8, 0, 0, 4, 3);
INSERT INTO metashard VALUES('corp_change_log_offline', 9, 0, 0, 4, 3);
INSERT INTO metashard VALUES('corp_change_log_offline', 10, 0, 0, 4, 3);

--MAIL_BP
INSERT INTO metashard VALUES('change_log', 0, 0, 0, 0, 0);
INSERT INTO metashard VALUES('change_log', 1, 0, 0, 0, 0);
INSERT INTO metashard VALUES('change_log', 2, 0, 0, 0, 0);
INSERT INTO metashard VALUES('change_log', 3, 0, 0, 0, 0);
INSERT INTO metashard VALUES('change_log', 4, 0, 0, 0, 0);
INSERT INTO metashard VALUES('change_log', 5, 0, 0, 0, 0);

INSERT INTO metashard VALUES('change_log', 6, 1, 0, 1, 7);
INSERT INTO metashard VALUES('change_log', 7, 1, 0, 1, 7);
INSERT INTO metashard VALUES('change_log', 8, 1, 0, 1, 7);
INSERT INTO metashard VALUES('change_log', 9, 1, 0, 1, 7);
INSERT INTO metashard VALUES('change_log', 10, 1, 0, 1, 7);

INSERT INTO metashard VALUES('change_log_offline', 0, 1, 0, 1, 0);
INSERT INTO metashard VALUES('change_log_offline', 1, 1, 0, 1, 0);
INSERT INTO metashard VALUES('change_log_offline', 2, 1, 0, 1, 0);
INSERT INTO metashard VALUES('change_log_offline', 3, 1, 0, 1, 0);
INSERT INTO metashard VALUES('change_log_offline', 4, 1, 0, 1, 0);
INSERT INTO metashard VALUES('change_log_offline', 5, 1, 0, 1, 0);

INSERT INTO metashard VALUES('change_log_offline', 6, 0, 0, 0, 0);
INSERT INTO metashard VALUES('change_log_offline', 7, 0, 0, 0, 0);
INSERT INTO metashard VALUES('change_log_offline', 8, 0, 0, 0, 0);
INSERT INTO metashard VALUES('change_log_offline', 9, 0, 0, 0, 0);
INSERT INTO metashard VALUES('change_log_offline', 10, 0, 0, 0, 0);

INSERT INTO metashard VALUES('subscriptions_prod_1', 0, 0, 0, 0, 0);
INSERT INTO metashard VALUES('subscriptions_prod_1', 1, 0, 0, 0, 0);
INSERT INTO metashard VALUES('subscriptions_prod_1', 2, 0, 0, 0, 0);
INSERT INTO metashard VALUES('subscriptions_prod_1', 3, 0, 0, 0, 0);
INSERT INTO metashard VALUES('subscriptions_prod_1', 4, 0, 0, 0, 0);
INSERT INTO metashard VALUES('subscriptions_prod_1', 5, 0, 0, 0, 0);

INSERT INTO metashard VALUES('subscriptions_prod_1', 6, 1, 0, 1, 0);
INSERT INTO metashard VALUES('subscriptions_prod_1', 7, 1, 0, 1, 0);
INSERT INTO metashard VALUES('subscriptions_prod_1', 8, 1, 0, 1, 0);
INSERT INTO metashard VALUES('subscriptions_prod_1', 9, 1, 0, 1, 0);
INSERT INTO metashard VALUES('subscriptions_prod_1', 10, 1, 0, 1, 0);

INSERT INTO metashard VALUES('subscriptions_prod_2', 0, 0, 0, 0, 0);
INSERT INTO metashard VALUES('subscriptions_prod_2', 1, 0, 0, 0, 0);
INSERT INTO metashard VALUES('subscriptions_prod_2', 2, 0, 0, 0, 0);
INSERT INTO metashard VALUES('subscriptions_prod_2', 3, 0, 0, 0, 0);
INSERT INTO metashard VALUES('subscriptions_prod_2', 4, 0, 0, 0, 0);
INSERT INTO metashard VALUES('subscriptions_prod_2', 5, 0, 0, 0, 0);

INSERT INTO metashard VALUES('subscriptions_prod_2', 6, 1, 0, 1, 0);
INSERT INTO metashard VALUES('subscriptions_prod_2', 7, 1, 0, 1, 0);
INSERT INTO metashard VALUES('subscriptions_prod_2', 8, 1, 0, 1, 0);
INSERT INTO metashard VALUES('subscriptions_prod_2', 9, 1, 0, 1, 0);
INSERT INTO metashard VALUES('subscriptions_prod_2', 10, 1, 0, 1, 0);

INSERT INTO metashard VALUES('iex', 0, 0, 0, 2, 0);
INSERT INTO metashard VALUES('iex', 1, 0, 0, 2, 0);
INSERT INTO metashard VALUES('iex', 2, 0, 0, 2, 0);
INSERT INTO metashard VALUES('iex', 3, 0, 0, 2, 0);
INSERT INTO metashard VALUES('iex', 4, 0, 0, 2, 0);
INSERT INTO metashard VALUES('iex', 5, 0, 0, 2, 0);

INSERT INTO metashard VALUES('iex', 6, 1, 0, 3, 0);
INSERT INTO metashard VALUES('iex', 7, 1, 0, 3, 0);
INSERT INTO metashard VALUES('iex', 8, 1, 0, 3, 0);
INSERT INTO metashard VALUES('iex', 9, 1, 0, 3, 0);
INSERT INTO metashard VALUES('iex', 10, 1, 0, 3, 0);


-- version = 7
SELECT NEXTVAL('metashard_versions');
SELECT NEXTVAL('metashard_versions');
SELECT NEXTVAL('metashard_versions');
SELECT NEXTVAL('metashard_versions');
SELECT NEXTVAL('metashard_versions');
SELECT NEXTVAL('metashard_versions');
SELECT NEXTVAL('metashard_versions');