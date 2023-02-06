# encoding: utf-8
from __future__ import unicode_literals
header = {
    'path': '/var/log/auth.log',
    'ident': 'dbaas',
    'offset': '3',
    'partition': '1',
    'seqno': '9921',
    'server': 'logship-dev01e.cmail.yandex.net',
    'sourceid': 'base64:eL5wm2zzRya7j7ZHsKS2Zg',
    'topic': 'rt3.sas--dbaas--dbaas-int-log',
}
data = u"""
tskv	cluster=test	pid=	timestamp=1498027249	origin=syslog_auth	daemon=sudo	log_time=2017 Jun 21 09:40:49	log_format=dbaas_int_log	text=pam_unix(sudo:session): session closed for user root\n	hostname=arhipov
tskv	cluster=test	pid=	timestamp=1498027250	origin=hz	daemon=sudo	log_time=2017 Jun 21 09:40:50	log_format=dbaas_int_log	text=postgres : TTY\=unknown ; PWD\=/tmp ; USER\=root ; COMMAND\=/usr/bin/service postgresql status\n	hostname=arhipov
tskv	cluster=test	pid=	timestamp=1498027250	origin=syslog_auth	daemon=sudo	log_time=2017 Jun 21 09:40:50	log_format=dbaas_int_log	text=pam_unix(sudo:session): session opened for user root by (uid\=0)\n	hostname=arhipov
"""
expected = u"""
"""
