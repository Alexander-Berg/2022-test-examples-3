#!/usr/bin/env python
# -*- coding: UTF-8 -*-

import os
import re
import sys

codes_keys = [ 
'scanfile',
'infected',
'unknown',
'others'
]


codes = dict()
for c_key in codes_keys:
    codes[c_key] = 0


index_re = re.compile('(Client request SCANFILE|Threat found Identity|Client request BYE|Client request SSSP/1.0|Client request QUERY SERVER|Client terminated connection early)')

for line in sys.stdin:
    line = line.strip()
    matches = index_re.findall(line)

    if len(matches):
        code = matches[0]

	if code == 'Client request SCANFILE':
            codes['scanfile'] += 1
	elif code == 'Threat found Identity':
            codes['infected'] += 1
	else:
            codes['others'] += 1
    else:
         codes['unknown'] += 1

for status, value in sorted(codes.items()):
    print("sophos_total_%s %d" % (status, value))

