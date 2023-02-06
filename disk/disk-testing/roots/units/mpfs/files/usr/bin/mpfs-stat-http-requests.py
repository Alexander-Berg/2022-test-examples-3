#!/usr/bin/env python

# -*- coding: UTF-8 -*-

import os
import re
import sys
from collections import defaultdict, Counter

results_errors = { 'parse' : 0 }


results_count_codes = dict() 
results_aggr_count_codes = dict()

results_timings = defaultdict(list)

# 2016-01-22 20:32:34,950 [14603] rest-507fdc388405329194a9a166e3eeb8e0-api01h 14603_16997 client GET "http://ratelimiter.disk.yandex.net:1880/cloud_api_user/25799403" 200 0 0 0.003

index_re = re.compile('[A-Z]+ "https?:\/\/(?P<b_host>[^\/:]+).*" (?P<b_status>\d+) \d+ \d+ (?P<b_request_time>\d+\.\d+)$')
#index_re = 'client [A-Z]+ "https?://(?P<b_host>[^\/:]+)[^"]+" (?P<b_status>\d+) \d+ \d+ (?P<b_request_time>\d+\.\d+)'
aliases = { re.compile("^uploader\d+") : "disk_uploader",
            re.compile("^webdav\d+") : "disk_webdav",
            re.compile("^push\d+") : "disk_push",
            re.compile("^xmpp\d+") : "disk_xmpp" }

for line in sys.stdin:
#    print line

    if line.strip() == '': 
        continue

    parts = line.strip("\n").split("\t")
    parsed = {}; 
    for part in parts:
        eq = part.find('=')
        key = part[:eq]
        value = part[(eq+1):]
        parsed[key] = value 

    if 'module' not in parsed or parsed['module'] != 'client':
        continue


    line = line.strip()
    matches = index_re.search(parsed['message'])
#    matches = re.search(index_re, line)
#    pprint.pprint(matches.groups())
    if matches:
        b_host = matches.group('b_host')
        for alias_re, alias in aliases.iteritems():
            if alias_re.match(b_host):
                b_host = alias

        host = b_host.replace('.', '_')
        
        u_status = int(matches.group('b_status'))

        if host not in results_count_codes:
            results_count_codes[host] = Counter()
        

      
        results_timings[host].append(matches.group('b_request_time'))
        results_count_codes[host][u_status] += 1

    else:
        results_errors['parse'] += 1
#        print line
        continue

# aggregate
results_aggr_count_codes['total'] = Counter()
for host, result in sorted(results_count_codes.iteritems()):
    if host not in results_aggr_count_codes:
        results_aggr_count_codes[host] = Counter()

    for status_k, status_v in sorted (result.iteritems()):
        acode = str(status_k / 100) + 'xx'
        
        results_aggr_count_codes[host][acode] += status_v

        results_aggr_count_codes[host]['total'] += status_v
        results_aggr_count_codes['total']['total'] += status_v

    
# count_hosts_codes (codes per url)
for host, result in sorted(results_count_codes.iteritems()):
    for status_k, status_v in sorted (result.iteritems()):
        print("request_count_code_%s_%s %d" % (host, status_k, status_v))

print
# count_hosts_codes (codes per url)
for host, result in sorted(results_aggr_count_codes.iteritems()):
    for status_k, status_v in sorted (result.iteritems()):
        print("request_aggr_count_code_%s_%s %d" % (host, status_k, status_v))

print
for host, timings in sorted(results_timings.iteritems()):
    if len(timings):
        print("request_timings_%s %s" % (host, ' '.join(timings) ))


for error, value in sorted(results_errors.iteritems()):
    print("error_%s %d" % (error, value))



sys.exit(0)

