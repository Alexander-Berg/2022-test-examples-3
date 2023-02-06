#!/usr/bin/env python
# -*- coding: UTF-8 -*-

import os
import subprocess, sys, time, re, socket

keys = ['mpfs_fcgi_error']

items = dict()

for key in keys:
    items[key] = 0

fin = os.popen('/usr/bin/mymtail.sh /var/log/mpfs/fcgi-error.log mpfs', 'r')

index_re = re.compile('^2')

for line in fin:
    matches = index_re.findall(line)

    if len(matches):
        items['mpfs_fcgi_error'] += 1

for code, val in items.items():
        print("%s %d" % (code, val))
