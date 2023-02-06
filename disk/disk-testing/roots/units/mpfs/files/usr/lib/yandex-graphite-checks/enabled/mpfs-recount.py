#!/usr/bin/env python
# -*- coding: UTF-8 -*-

import os
import subprocess, sys, time, re, socket
import urllib2

keys = [ 'mpfs_recount' ]

items = dict()

for key in keys:
	items[key] = 0

url = 'http://mpfs.disk.yandex.net/service/recount_number'

count = 3

while count:    
	try:
		l = urllib2.urlopen(url)
		items['mpfs_recount'] = l.readline().strip()
		break
	except:
		count = count - 1
		pass

for code, val in items.items():
        print("%s %s" % (code, val))
