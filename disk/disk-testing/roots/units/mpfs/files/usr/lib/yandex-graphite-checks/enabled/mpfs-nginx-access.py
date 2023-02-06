#!/usr/bin/env python
# -*- coding: UTF-8 -*-

import os
import subprocess, sys, time, re, socket

keys = [ 'web_code_mpfs_1xx',
'web_code_mpfs_2xx',
'web_code_mpfs_3xx',
'web_code_mpfs_4xx',
'web_code_mpfs_499',
'web_code_mpfs_400',
'web_code_mpfs_5xx',
'web_code_mpfs_unknown',
'web_code_answer_03xinf',
'mpfs_vdiff_200',
'mpfs_vdiff_404',
'mpfs_fdiff',
'mpfs_billing_5xx'
]

items = dict()

for key in keys:
    items[key] = 0

fin = os.popen('/usr/bin/mymtail.sh /var/log/nginx/mpfs/access.log mpfs', 'r')

index_re = re.compile(' "\w+ /.* HTTP\/1\.\d" (\d+) .* (\d+\.\d+)$')

for line in fin:
    matches = index_re.findall(line)

    if len(matches):
        code = int(matches[0][0])

        if code < 200:
            items['web_code_mpfs_1xx'] += 1
        elif code < 300:
            items['web_code_mpfs_2xx'] += 1
        elif code < 400:
            items['web_code_mpfs_3xx'] += 1
        elif code < 500:
            items['web_code_mpfs_4xx'] += 1
            if code == 499:
                items['web_code_mpfs_499'] += 1
            elif code == 400:
                items['web_code_mpfs_400'] += 1
        elif code < 600:
            items['web_code_mpfs_5xx'] += 1
        else:
            items['web_code_mpfs_unknown'] += 1

        time = float(matches[0][1])
        if time >= 0.3:
            items['web_code_answer_03xinf'] += 1

        if line.find('GET /desktop/diff') >= 0:
            if line.find('&version=') >= 0:
                if code == 200:
                    items['mpfs_vdiff_200'] += 1
                else:
                    items['mpfs_vdiff_404'] += 1
            else:
                items['mpfs_fdiff'] += 1

        if line.find('GET /billing') >= 0:
            if code > 499:
                items['mpfs_billing_5xx'] += 1


for code, val in items.items():
        print("%s %d" % (code, val))
