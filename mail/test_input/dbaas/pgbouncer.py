# encoding: utf-8
from __future__ import unicode_literals
header = {
    'path': '/var/log/postgresql/pgbouncer.log',
    'ident': 'dbaas',
    'offset': '3',
    'partition': '1',
    'seqno': '9921',
    'server': 'logship-dev01e.cmail.yandex.net',
    'sourceid': 'base64:eL5wm2zzRya7j7ZHsKS2Zg',
    'topic': 'rt3.sas--dbaas--dbaas-int-log',
}
data = r"""tskv	ms=460	session_id=C-0x55bb4d71cba0	db=disk_notifier_config	cluster=miscdb-test02	log_time=2017-06-19 14:27:13.460	pid=323862	source=[2a02:6b8:0:1630::247]:40208	level=LOG	text=login attempt: db\=disk_notifier_config user\=disk_notifier tls\=TLSv1.2/ECDHE-RSA-AES128-GCM-SHA256/ECDH\=prime256v1\n	log_format=dbaas_int_log	origin=pgbouncer	user=disk_notifier	hostname=miscdb-test02h.mail.yandex.net	timestamp=1497871633
tskv	ms=0.574	session_id=C-0x55bb4d672828	db=disk_notifier_config	cluster=miscdb-test02	log_time=2017-06-19 14:27:13.574	pid=323862	source=[2a02:6b8:0:1630::249]:49174	level=LOG	text=closing because: client close request (age\=5)\n	log_format=dbaas_int_log	origin=pgbouncer	user=disk_notifier	hostname=miscdb-test02h.mail.yandex.net	timestamp=1497871633
tskv	ms=666	session_id=C-0x55bb4d6178f0	db=postgres	cluster=miscdb-test02	log_time=2017-06-19 14:27:13.666	pid=323862	source=[2a02:6b8:0:1630::1c0]:49400	level=LOG	text=closing because: client close request (age\=5)\n	log_format=dbaas_int_log	origin=bogus	user=pgaas-proxy-auth-user	hostname=miscdb-test02h.mail.yandex.net	timestamp=1497871633
"""

expected = r"""miscdb-test02	disk_notifier_config	miscdb-test02h.mail.yandex.net	LOG	2017-06-19	2017-06-19 14:27:13	460	323862	C-0x55bb4d71cba0	[2a02:6b8:0:1630::247]:40208	login attempt: db=disk_notifier_config user=disk_notifier tls=TLSv1.2/ECDHE-RSA-AES128-GCM-SHA256/ECDH=prime256v1\\n	1497871633	disk_notifier"""
