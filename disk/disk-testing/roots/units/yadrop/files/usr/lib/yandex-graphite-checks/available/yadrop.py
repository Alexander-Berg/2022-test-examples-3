#!/usr/bin/env python
# -*- coding: UTF-8 -*-

import os
import re

keys = [ 'web.code.yadrop.1xx',
'web.code.yadrop.2xx',
'web.code.yadrop.3xx',
'web.code.yadrop.4xx',
'web.code.yadrop.5xx',
'web.code.yadrop.507',
'web.code.yadrop.unknown'
]

items = dict()

for key in keys:
	items[key] = 0

fin = os.popen('/usr/bin/mymtail.sh /var/log/yadrop/access.log yadrop', 'r')

index_re = re.compile(' (\d+) \d+\.\d+ (?:\d+|-) (?:\d+|-)$')

for line in fin:
	matches = index_re.findall(line)

	if len(matches):
		code = int(matches[0])

		if code < 200:
			items['web.code.yadrop.1xx'] += 1
		elif code < 300:
			items['web.code.yadrop.2xx'] += 1
		elif code < 400:
			items['web.code.yadrop.3xx'] += 1
		elif code < 500:
			items['web.code.yadrop.4xx'] += 1
		elif code < 600 and code != 507:
			items['web.code.yadrop.5xx'] += 1
		elif code == 507:
			items['web.code.yadrop.507'] += 1
		else:
			items['web.code.yadrop.unknown'] += 1
fin.close()

for key, value in items.items():
    print("%s %s" % (key, value))
