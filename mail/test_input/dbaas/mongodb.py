# encoding: utf-8
from __future__ import unicode_literals
header = {
    'path': '/var/log/mongodb/mongodb',
    'ident': 'dbaas',
    'offset': '3',
    'partition': '1',
    'seqno': '9921',
    'server': 'logship-dev01e.cmail.yandex.net',
    'sourceid': 'base64:eL5wm2zzRya7j7ZHsKS2Zg',
    'topic': 'rt3.sas--dbaas--dbaas-int-log',
}

data = u"""tskv	log_time=2018-01-16 20:50:01.727000 MSK\	context=conn56333	message=received client metadata from 127.0.0.1:51936 conn56333: { application: { name: "MongoDB Shell" }, driver: { name: "MongoDB Internal Client", version: "3.4.10" }, os: { type: "Linux", name: "Ubuntu", architecture: "x86_64", version: "14.04" } }	datetime=2018-01-16T20:50:01.727+0300	ms=726	origin=mongod	hostname=mongo-test02e.mail.yandex.net	component=NETWORK	log_format=dbaas_int_log	timestamp=1516125001	severity=I	cluster=ololo
tskv	log_time=2018-01-16 20:50:01.730000 MSK	context=conn56333	message=SCRAM-SHA-1 authentication failed for васян on базавасяна from client 127.0.0.1:51936 ; UserNotFound: Could not find user васян@базавасяна	datetime=2018-01-16T20:50:01.730+0300	ms=730	origin=mongod	hostname=mongo-test02e.mail.yandex.net	component=ACCESS	log_format=dbaas_int_log	timestamp=1516125001	severity=I	cluster=ololo
tskv	log_time=2018-01-16 20:50:01.731000 MSK	context=conn56333	message=end connection 127.0.0.1:51936 (7 connections now open)	datetime=2018-01-16T20:50:01.731+0300	ms=730	hostname=mongo-test02e.mail.yandex.net	component=-	log_format=dbaas_int_log	timestamp=1516125001	severity=I	cluster=ololo
"""

expected = u"""ololo	ACCESS	conn56333	mongo-test02e.mail.yandex.net	2018-01-16	2018-01-16 20:50:01	SCRAM-SHA-1 authentication failed for васян on базавасяна from client 127.0.0.1:51936 ; UserNotFound: Could not find user васян@базавасяна	730	I	1516125001"""
