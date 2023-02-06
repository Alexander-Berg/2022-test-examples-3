#!/usr/bin/env python

# -*- coding: UTF-8 -*-

import os
import re
import sys
from pprint import pprint
from collections import defaultdict, Counter


results_errors = { 'parse' : 0 }


results_count_request_status = defaultdict(dict)
results_timings_request = defaultdict(list)

results_count_aggr_request_status = defaultdict(dict)
results_timings_aggr_request = defaultdict(list)


http_re = re.compile('HTTP (?P<r_type>[A-Z]+) https?:\/\/(?P<r_host>[^\/:]+)[\/:].* (?P<r_status>completed|failed)[^;]+; took (?P<r_took>\d+.\d+)$')


for line in sys.stdin:
    if line.strip() == '':
        continue

    parts = line.strip("\n").split("\t")
    parsed = {};
    for part in parts:
        eq = part.find('=')
        key = part[:eq]
        value = part[(eq+1):]
        parsed[key] = value        
    level = parsed['level']
    message = parsed['message']
#    pprint(parsed)

    if parsed['class'] == 'r.y.mi.io.http.apache.v4.ApacheHttpClient4Utils':
        request_matches = http_re.search(message)
        if request_matches:
            r_type = request_matches.group('r_type').lower()
            r_host = request_matches.group('r_host').replace('.', '_')
            r_status = request_matches.group('r_status')
            r_took = request_matches.group('r_took')


            if r_host not in results_count_request_status:
                results_count_request_status[r_host] = defaultdict(Counter)
                results_timings_request[r_host] = defaultdict(list)

                results_count_aggr_request_status[r_host] = defaultdict(Counter)
                results_timings_aggr_request[r_host] = defaultdict(list)

            results_timings_request[r_host][r_type].append(r_took)
            results_count_request_status[r_host][r_type][r_status] += 1 # calc total

            results_timings_aggr_request[r_host]['total'].append(r_took)
            results_count_aggr_request_status[r_host]['total'][r_status] += 1 



for host, results in sorted(results_count_request_status.iteritems()):
    for op, values in sorted (results.iteritems()):
        for val_k, val_v in sorted (values.iteritems()):
            print ("request_count_status_{}_{}_{} {}".format(host, op, val_k, val_v))

for host, results in sorted(results_count_aggr_request_status.iteritems()):
    for op, values in sorted (results.iteritems()):
        for val_k, val_v in sorted (values.iteritems()):
            print ("request_count_aggr_status_{}_{}_{} {}".format(host, op, val_k, val_v))

for host, results in sorted(results_timings_request.iteritems()):
    for request, timings in results.iteritems():
        if len(timings):
            print("request_timings_{}_{} {}".format(host, request, ' '.join(timings) ))

for host, results in sorted(results_timings_aggr_request.iteritems()):
    for request, timings in results.iteritems():
        if len(timings):
            print("request_timings_aggr_{}_{} {}".format(host, request, ' '.join(timings) ))



sys.exit(0)

