#!/usr/bin/env python
# -*- coding: UTF-8 -*-

import os
import re

keys = [ 'yadrop.crash']

items = dict()

for key in keys:
        items[key] = 0

fin = os.popen('/usr/bin/timetail -t java -n 60 /var/log/yadrop/messages.log', 'r')

index = re.compile('.*\CRASH\ REPORT.*')

for line in fin:
        matches = index.findall(line)
        if len(matches):
                items['yadrop.crash'] += 1

for key, value in items.items():
    print("%s %s" % (key, value))
