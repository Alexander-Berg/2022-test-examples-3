#!/usr/bin/env python

# -*- coding: UTF-8 -*-

import os
import re
import sys
from pprint import pprint
from collections import defaultdict, Counter


results_errors = { 'parse' : 0 }


# Collect errors from logs
results_log_errors = dict()
for error in ['index', 'transaction', 'parse']:
    results_log_errors[error] = 0

# Collect history lag timings
results_lag_timings = list()
results_lag_queue_timings = list()
results_lag_logbroker_timings = list()


# Collect misc stat
results_stats = dict()
for stat in ['events_total']:
    results_stats[stat] = 0
#
lag_re = re.compile('^Event time lag: (\d+) s$')

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
    if 'rid' in parsed:
        rid = parsed['rid']
#    pprint(parsed)

    if level == 'ERROR':
        #error_matches = error_re.findall(parsed['message'])
        if message.startswith("Got error from listener"):
            results_log_errors['transaction'] += 1
        elif message.startswith("Event is not saved to index"):
            results_log_errors['index'] += 1
        elif message.startswith("Error while parsing event of type"):
            results_log_errors['parse'] += 1

    elif level == 'INFO':
#        print message 
        matches = lag_re.findall(message)
        if len (matches):
            b_lag = matches[0]
            if rid.startswith("lenta.process_log_line"):
                results_lag_queue_timings.append(b_lag)
            else:
                results_lag_logbroker_timings.append(b_lag)

            results_stats['events_total'] += 1
            results_lag_timings.append(b_lag)



for error, result in sorted (results_log_errors.items()):
    print("count_log_error_%s %d" % (error, result))

for error, value in sorted(results_errors.items()):
    print("error_%s %d" % (error, value))

print ("lag_timings %s" % (' '.join(results_lag_timings) ))

print ("lag_queue_timings %s" % (' '.join(results_lag_queue_timings) ))

print ("lag_logbroker_timings %s" % (' '.join(results_lag_logbroker_timings) ))


for stat, value in sorted(results_stats.items()):
    print ("stat_%s %d" % (stat, value))


sys.exit(0)

