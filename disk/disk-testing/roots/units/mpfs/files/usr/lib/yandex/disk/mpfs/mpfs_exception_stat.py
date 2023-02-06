#!/usr/bin/python

import os
import re
import sys
from collections import Counter


exception_pattern = '([A-Z][A-Za-z0-9_]+):'

service_group = sys.argv[1]
results_count_exceptions = Counter()
results_aggr_count_exceptions = Counter()

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

    if 'message' not in parsed:
        continue

    if parsed['message'].startswith('Traceback'):
        exception_re_found = re.search(exception_pattern, parsed['message'])
        if exception_re_found:
            exception_name = exception_re_found.group(0).strip(':')
            results_count_exceptions[exception_name] +=  1
            results_aggr_count_exceptions['Total'] += 1


for exception_name, exception_value in results_count_exceptions.iteritems():
    print('exception_count_{} {}'.format(exception_name, exception_value))


for exception_name, exception_value in results_aggr_count_exceptions.iteritems():
    print('exception_aggr_count_{} {}'.format(exception_name, exception_value))



print('exception_group_count_{}_Total {}'.format(service_group, results_aggr_count_exceptions['Total']))
