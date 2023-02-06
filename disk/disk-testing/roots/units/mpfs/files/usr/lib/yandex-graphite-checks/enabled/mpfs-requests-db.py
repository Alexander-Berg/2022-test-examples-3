#!/usr/bin/env python
# -*- coding: UTF-8 -*-

import os
import subprocess, sys, time, re, socket

items = dict()

fin = os.popen('/usr/bin/mymtail.sh /var/log/mpfs/requests.log mongo', 'r')
index_re = re.compile('logging (?:mongos|disk-unit-\d+)\.(?:[-_a-z]+\.)?([\w\.]+)\.(\w+)\(')

def increment(items, key):
    if key in items:
        items[key] += 1
    else:
        items[key] = 1

mongoless300 = 0
mongomore300 = 0

for line in fin:
    mongo = index_re.findall(line)
    if len(mongo) == 0:
        continue

    st = line.split(" ")
    st.reverse()
    if st[0] <= "0.3":
        mongoless300 += 1
    else:
        mongomore300 += 1

    (coll, op) = mongo[0]

    increment(items, coll)
    increment(items, ".".join((coll,op)))

for code, val in items.items():
        print("%s %s" % (code, val))
print "mpfs.mongoless300 %s" % mongoless300
print "mpfs.mongomore300 %s" % mongomore300
