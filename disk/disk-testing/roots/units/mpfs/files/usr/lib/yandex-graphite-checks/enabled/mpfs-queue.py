#!/usr/bin/env python
# -*- coding: UTF-8 -*-

import os
import subprocess, sys, time, re, socket
import urllib2

keys = [ 'mpfs_queue' ]

items = dict()

for key in keys:
	items[key] = 0

url = 'http://127.0.0.1/queue/count'

count = 3

while count:    
	try:
		l = urllib2.urlopen(url)
		items['mpfs_queue'] = l.readline().strip()
		break
	except:
		count = count - 1
		pass

for code, val in items.items():
        print("%s %s" % (code, val))
