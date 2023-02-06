#!/usr/bin/python
import re
from collections import Counter
import pprint
import urllib2
import sys

#./logbroker_lag.py disk-event-loader ydisk-event-history-log
#http://$dc.logbroker.yandex.net/pull/offsets?client=disk-event-loader&log-type=ydisk-event-history-log&dc=$dc

lb_dc_list = ['myt', 'fol', 'sas', 'man', 'iva']
client = sys.argv[1]
log_type = sys.argv[2]


def http_get (url,retries,sleep):
    bdata = False
    for i in xrange(retries):
        try:
            http_req = urllib2.urlopen(url)
            if http_req.getcode() != 200 :
                bdata = False
            else:
                bdata = http_req.read()
                break
        except:
            bdata = False
    return bdata


dc_lags = {}
local_lags = []

for lb_query_dc in lb_dc_list:
    lags = Counter()
    url = 'http://{}.logbroker.yandex.net/pull/offsets?client={}&log-type={}'.format(lb_query_dc, client, log_type)
    lb_query_data = http_get(url, 2, 0)

    if lb_query_data:
        for line in lb_query_data.splitlines():
            (topic_partition, offsets, logstart, logsize, lag, owner) = line.split()
            dc_match = re.match('.+\.([a-z]+).*', topic_partition)
            dc = dc_match.group(1)
            if lag.isdigit():
                lags[dc] += int(lag)
        dc_lags[lb_query_dc] = lags
        local_lags.append(lags[lb_query_dc])

for (lb_dc, lags) in dc_lags.iteritems():
    for (dc, lag) in lags.iteritems():
       print('{}_{} {}'.format(lb_dc, dc, lag))

print('max_local_lag {}'.format(max(local_lags)))

