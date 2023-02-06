#!/usr/bin/env python

# -*- coding: UTF-8 -*-

import os
import re
import sys


results_errors = { 'parse' : 0 }

s_name = 'mpfs';

results_count_hosts_codes = dict() 
results_count_hosts_acodes = dict()

results_timings_hosts_all = dict()

# 2016-01-22 20:32:34,950 [14603] rest-507fdc388405329194a9a166e3eeb8e0-api01h 14603_16997 client GET "http://ratelimiter.disk.yandex.net:1880/cloud_api_user/25799403" 200 0 0 0.003

index_re = re.compile('[A-Z]+ "https?:\/\/(?P<b_host>[^\/:]+).*" (?P<b_status>\d+) \d+ \d+ (?P<b_request_time>\d+\.\d+)$')
#index_re = 'client [A-Z]+ "https?://(?P<b_host>[^\/:]+)[^"]+" (?P<b_status>\d+) \d+ \d+ (?P<b_request_time>\d+\.\d+)'
aliases = { re.compile("^uploader\d+") : "disk_uploader",
            re.compile("^webdav\d+") : "disk_webdav",
            re.compile("^push\d+") : "disk_push" }

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
        for alias_re, alias in aliases.items():
            if alias_re.match(b_host):
                b_host = alias

        u_host = b_host.replace('.', '_')
        
        u_status = int(matches.group('b_status'))

        if u_host not in results_count_hosts_codes:
            results_count_hosts_codes[u_host] = dict()
        if u_status not in results_count_hosts_codes[u_host]:
            results_count_hosts_codes[u_host][u_status] = 0
        
        if u_host not in results_timings_hosts_all:
            results_timings_hosts_all[u_host] = list()

      
        results_timings_hosts_all[u_host].append(matches.group('b_request_time'))
        results_count_hosts_codes[u_host][u_status] += 1

    else:
        results_errors['parse'] += 1
#        print line
        continue

# aggregate
results_count_hosts_acodes['total'] = dict()
results_count_hosts_acodes['total']['total'] = 0
for u_host, result in sorted(results_count_hosts_codes.items()):
    if u_host not in results_count_hosts_acodes:
        results_count_hosts_acodes[u_host] = dict()
        results_count_hosts_acodes[u_host]['total'] = 0
    for u_status_k, u_status_v in sorted (result.items()):

        acode = str(u_status_k / 100) + 'xx'
        
        if acode not in results_count_hosts_acodes[u_host]:
            results_count_hosts_acodes[u_host][acode] = 0 

        results_count_hosts_acodes[u_host][acode] += u_status_v

        results_count_hosts_acodes[u_host]['total'] += u_status_v
        results_count_hosts_acodes['total']['total'] += u_status_v

    
# count_hosts_codes (codes per url)
for u_host, result in sorted(results_count_hosts_codes.items()):
    for u_status_k, u_status_v in sorted (result.items()):
        print("%s_count_request_%s_code_%s %d" % (s_name, u_host, u_status_k, u_status_v))

print
# count_hosts_codes (codes per url)
for u_host, result in sorted(results_count_hosts_acodes.items()):
    for u_status_k, u_status_v in sorted (result.items()):
        print("%s_count_request_%s_acode_%s %d" % (s_name, u_host, u_status_k, u_status_v))

print
for u_host, timings in sorted(results_timings_hosts_all.items()):
    if len(timings):
        print("%s_timings_request_%s %s" % (s_name, u_host, ' '.join(timings) ))


for error, value in sorted(results_errors.items()):
    print("%s_error_%s %d" % (s_name, error, value))



sys.exit(0)

