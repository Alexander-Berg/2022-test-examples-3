#!/usr/bin/env python
# -*- coding: UTF-8 -*-

import os
import re
import sys

codes_keys = [ 'ok',
'timeout',
'skipped',
'error',
'infected',
'bad',
'unknown',
'count'
]


codes = dict()
for c_key in codes_keys:
    codes[c_key] = 0


logid = 'drwebd-log.default';
if len (sys.argv) > 2:
    sys.exit ('USAGE: %s [LOGID]' % sys.argv[0])
elif len (sys.argv) == 2:
    logid = sys.argv[1]

fin = os.popen('/usr/bin/mymtail.sh /var/log/drweb.log %s | grep "<drwebdc>"' % (logid), 'r')

index_re = re.compile('(Ok$|timeout!|skipped|compression ratio is too high|error!|Error reading file data|infected|advertising|intrusion|dangerous|joke)')

for line in fin:

    line = line.strip()
    matches = index_re.findall(line)

    if len(matches):
        code = matches[0]

        codes['count'] += 1

	if code == 'Ok':
            codes['ok'] += 1
	elif code == 'timeout!':
            codes['timeout'] += 1
	elif code in ['skipped', 'compression ratio is too high']:
            codes['skipped'] += 1
	elif code in ['error!', 'Error reading file data']:
            codes['error'] += 1
	elif code == 'infected':
            codes['infected'] += 1
	elif code in ['advertising', 'intrusion', 'dangerous', 'joke']:
	    codes['bad'] += 1
	else:
            codes['unknown'] += 1

for status, value in sorted(codes.items()):
    print("drweb_total_%s %d" % (status, value))

