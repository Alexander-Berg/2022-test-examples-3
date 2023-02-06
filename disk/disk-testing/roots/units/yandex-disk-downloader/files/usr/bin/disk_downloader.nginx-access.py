#!/usr/bin/env python
# -*- coding: UTF-8 -*-
import os
import re
import sys

codes_keys = [ 'code_1xx',
'code_2xx',
'code_3xx',
'code_4xx',
'code_5xx',
'code_unknown',
'cache_hit',
'cache_miss',
'code_410',
'code_400',
'code_404',
'code_403',
'code_416',
'count' ]

codes = dict()

for c_key in codes_keys:
    codes[c_key] = 0


logid = 'nginx-access.default';
if len (sys.argv) > 2:
    sys.exit ('USAGE: %s [LOGID]' % sys.argv[0])
elif len (sys.argv) == 2:
    logid = sys.argv[1]

fin = os.popen('/usr/bin/mymtail.sh /var/log/nginx/downloader/access.log %s' % (logid), 'r')

index_re = re.compile(' "GET /.* HTTP\/1\.\d" (\d+) "')

for line in fin:
    if line.find('proxy (MISS)') >= 0:
        codes['cache_miss'] += 1
    elif line.find('proxy (HIT)') >= 0:
        codes['cache_hit'] += 1

    if line.find('GET /ping') < 1:
        matches = index_re.findall(line)
	codes['count'] += 1

        if len(matches):
            code_ = int(matches[0])
            if code_ < 200:
                codes['code_1xx'] += 1
            elif code_ < 300:
                codes['code_2xx'] += 1
            elif code_ < 400:
                codes['code_3xx'] += 1
            elif code_ == 410:
                codes['code_410'] += 1
	    elif code_ == 400:
                codes['code_400'] += 1
	    elif code_ == 404:
                codes['code_404'] += 1
	    elif code_ == 416:
                codes['code_416'] += 1
	    elif code_ == 403:
                codes['code_403'] += 1
	    elif code_ < 500:
                codes['code_4xx'] += 1
            elif code_ < 600:
                codes['code_5xx'] += 1
            else:
                codes['code_unknown'] += 1


for code, value in sorted(codes.items()):
    print("nginx_total_%s %d" % (code, value))


