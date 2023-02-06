#!/usr/bin/python

# Reads market-report access-log lines from stdin and prints all unique test_id's


import urlparse
import sys

ids = set()

for l in sys.stdin:
    parsed = urlparse.urlparse(l.split('\t')[1])
    params = urlparse.parse_qs(parsed.query)
    try:
        tb = params['test-buckets'][0]
    except KeyError:
        continue
    for test_data in tb.split(';'):
        tid, _, _ = test_data.split(',')
        ids.add(int(tid))

for tid in sorted(ids):
    print tid

