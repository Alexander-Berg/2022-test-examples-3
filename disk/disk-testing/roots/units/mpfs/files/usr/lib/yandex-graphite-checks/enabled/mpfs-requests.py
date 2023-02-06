#!/usr/bin/env python
# -*- coding: UTF-8 -*-

import os
import subprocess, sys, time, re, socket
import urllib2

keys = [
'mpfs_requests_5xx',
'mpfs_requests_4xx',
'mpfs_msearch-proxy_non200'
]

#keys = [ 'mpfs_xmpp_5xx',
#'mpfs_xiva_5xx',
#'mpfs_mail_5xx',
#'mpfs_uploader_5xx',
#'mpfs_webdav_5xx',
#'mpfs_passport_5xx',
#'mpfs_social_5xx',
#'mpfs_narod_5xx',
#'mpfs_abook_5xx',
#'mpfs_clck_5xx'
#]

items = dict()

for key in keys:
    items[key] = 0

fin = os.popen('/usr/bin/mymtail.sh /var/log/mpfs/requests.log mpfs', 'r')

index_re = re.compile(' client "(http://.*)" (\d+) \d+ \d+ (\d+\.\d+)$')

#2012-12-03 12:02:51,187 [4880] 4880_4060 client "http://msearch-proxy.mail.yandex.net/api/chemodan_search?service=mail&uid=27829560&sort=ctime&offset=240&amt=40&order=desc&newstyle=1&tree=0" 500 32 0 6.022
for line in fin:
    matches = index_re.findall(line)
    if len(matches):
        client = matches[0][0]
        code = int(matches[0][1])
        if code >= 500:
            items['mpfs_requests_5xx'] += 1
	if code >= 400:
            items['mpfs_requests_4xx'] += 1
        if client.find('msearch') > 0 and code != 200:
            items['mpfs_msearch-proxy_non200'] += 1
#            if client.find('uploader') >= 0 and client.find('disk.yandex.net'):
#                items['mpfs_uploader_5xx'] += 1
#
#            elif client.find('api.yadisk.cc') >= 0:
#                items['mpfs_clck_5xx'] += 1
#
#            elif client.find('xiva') >= 0 and client.find('mail.yandex.net') >= 0:
#                items['mpfs_xiva_5xx'] += 1
#
#            elif client.find('push') >= 0 and client.find('online.yandex.net') >= 0:
#                items['mpfs_xmpp_5xx'] += 1
#
#            elif client.find('xmpp.yandex.ru') >= 0:
#                items['mpfs_xmpp_5xx'] += 1
#
#            elif client.find('') >= 0:
#                items['mpfs_mail_5xx'] += 1
#
#            elif client.find('') >= 0:
#                items['mpfs_narod_5xx'] += 1
#
#            elif client.find('') >= 0:
#                items['mpfs_abook_5xx'] += 1
#            else:
#                items['mpfs_requests_5xx'] += 1

for code, val in items.items():
        print("%s %s" % (code, val))
