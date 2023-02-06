# encoding: utf-8
from __future__ import unicode_literals
header = {
    'path': '/var/log/postgresql/postgresql-9.6-main.csv',
    'ident': 'dbaas',
    'offset': '3',
    'partition': '1',
    'seqno': '9921',
    'server': 'logship-dev01e.cmail.yandex.net',
    'sourceid': 'base64:eL5wm2zzRya7j7ZHsKS2Zg',
    'topic': 'rt3.sas--dbaas--dbaas-int-log',
}
data = u"""tskv	detail=	timestamp=1497607833	database_name=diskdb_test02	session_start_time=2017-06-16 13:09:36.009 MSK	session_id=5943ae60.5a223	command_tag=SELECT	ms=853	sql_state_code=00000	context=	error_severity=LOG	internal_query=	connection_from=localhost:52484	hostname=miscdb-test02f.mail.yandex.net	virtual_transaction_id=15/14278712	location=	message=duration: 0.275 ms  execute <unnamed>: SELECT pg_is_in_recovery() as is_in_recovery, ROUND(extract(epoch FROM clock_timestamp() - ts))::integer as replication_lag FROM repl_mon;	hint=	query=	internal_query_pos=	application_name=app - [2a02:6b8:b011:4001::7ee7:25a9]:51838	log_format=dbaas_int_log	origin=postgresql	process_id=369187	query_pos=	transaction_id=0	cluster=miscdb-test02	log_time=2017-06-16 13:10:33.853 MSK	user_name=sharpei	session_line_num=819
tskv	detail=	timestamp=1497607833	database_name=diskdb_test02	session_start_time=2017-06-16 13:09:36 MSK	session_id=5943ae60.5a223	command_tag=DISCARD ALL	ms=853	sql_state_code=00000	context=	error_severity=LOG	internal_query=	connection_from=localhost:52484	hostname=miscdb-test02f.mail.yandex.net	virtual_transaction_id=15/0	location=	message=Юникод duration: 0.027 ms  statement: DISCARD ALL;	hint=	query=\\	internal_query_pos=	application_name=	log_format=dbaas_int_log	origin=postgresql	process_id=369187	query_pos=	transaction_id=0	cluster=miscdb-test02	log_time=2017-06-16 13:10:33.853 MSK	user_name=sharpei	session_line_num=820
tskv	detail=	timestamp=1497607833	database_name=diskdb_test02	session_start_time=2017-06-16 13:09:36 MSK	session_id=5943ae60.5a223	command_tag=DISCARD ALL	ms=853	sql_state_code=00000	context=	error_severity=LOG	internal_query=	connection_from=localhost:52484	hostname=miscdb-test02f.mail.yandex.net	virtual_transaction_id=15/0	location=	message=duration: 0.027 ms  statement: DISCARD ALL;	hint=	query=	internal_query_pos=	application_name=	log_format=dbaas_int_log	origin=bogus	process_id=369187	query_pos=	transaction_id=0	cluster=miscdb-test02	log_time=2017-06-16 13:10:33.853 MSK	user_name=sharpei	session_line_num=820
"""
# expected = u"""miscdb-test02	00000	DISCARD ALL		Юникод duration: 0.027 ms  statement: DISCARD ALL;		miscdb-test02f.mail.yandex.net				localhost:52484	15/0	820	0	853	1497607833	369187	2017-06-16 13:10:33		2017-06-16 13:09:36	2017-06-16	diskdb_test02		5943ae60.5a223		sharpei		LOG"""
expected = u"""	miscdb-test02	DISCARD ALL	localhost:52484		diskdb_test02		LOG		miscdb-test02f.mail.yandex.net				2017-06-16	2017-06-16 13:10:33	Юникод duration: 0.027 ms  statement: DISCARD ALL;	853	369187			5943ae60.5a223	820	2017-06-16 13:09:36	00000	1497607833	0	sharpei	15/0"""
