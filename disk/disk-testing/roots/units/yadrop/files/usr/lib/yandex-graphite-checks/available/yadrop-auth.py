#!/usr/bin/env python
# -*- coding: UTF-8 -*-

import os
import re

keys = [ 'yadrop.auth.blackbox.ok',
'yadrop.auth.blackbox.error',
'yadrop.auth.cache'
]

items = dict()

for key in keys:
	items[key] = 0

fin = os.popen('/usr/bin/mymtail.sh /var/log/yadrop/auth.log yadrop', 'r')

index1 = re.compile('ya_blackbox:\d+:info .* type=blackbox code=200')
index2 = re.compile('ya_blackbox:\d+:error .* type=blackbox')
index3 = re.compile('yadrop_auth:\d+:info .* type=auth_cache')

for line in fin:
	matches = index1.findall(line)
	if len(matches):
		items['yadrop.auth.blackbox.ok'] += 1

	matches = index2.findall(line)
	if len(matches):
		items['yadrop.auth.blackbox.error'] += 1

	matches = index3.findall(line)
	if len(matches):
		items['yadrop.auth.cache'] += 1

for key, value in items.items():
    print("%s %s" % (key, value))

