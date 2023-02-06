#!/usr/bin/env python

# -*- coding: UTF-8 -*-

import sys
import re
#import pprint
from collections import defaultdict, Counter
import time


results_errors = Counter()

results_timings = defaultdict(dict)
results_aggr_timings = defaultdict(dict)

results_count_status = defaultdict(dict)
results_aggr_count_status = defaultdict(dict)

results_count_rpref = defaultdict(dict)
results_aggr_count_rpref = defaultdict(dict)

results_count_size = defaultdict(dict)
results_aggr_count_size = defaultdict(dict)

results_count_nret = defaultdict(dict)
results_aggr_count_nret = defaultdict(dict)



results_aggr_count_mongo300 = defaultdict(int)

# 2016-06-15 18:37:13,952 [2955] win-LUzY_1FK_IB1-1-webdav8g 2955_103339 logging disk-unit-11.user_data.user_data.find(({'_id': 'df88e90da6cc591c8a37e24024004e6e', 'uid': '390956727'},),     {'slave_okay': False}, read=PRIMARY).1 1 104 0.008


query_re = re.compile('(?P<q_shard>system|common|local|hardlinks|disk-unit-\d+|blockings|mongos)\.[\w_\-]+\.[\w_\-$]+\.(?P<q_op>[\w_\-]+)\(.* (?P<q_took>\d+\.\d+)$')
query_read_re = re.compile ('read=(?P<q_rpref>PRIMARY|SLAVE)\)\.\d+(?: socket_time: (?P<q_socket_time>\d+\.\d+))? (?P<q_nret>\d+) (?P<q_size>\d+) (?P<q_took>\d+\.\d+)$')

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

    if parsed['module'] != 'logging':
        continue

    query_matches_message = query_re.search(parsed['message'])
    if query_matches_message:
        query_matches = query_matches_message.groupdict()
        q_op = query_matches['q_op'].lower()
        shards = [query_matches['q_shard']]

        q_status = 'completed'
        q_took = query_matches['q_took']

        if shards[0].startswith('disk-unit'):
            shards.append('unit')

        meter = q_op

        if q_op in ['find', 'find_one', 'getMore']:
            q_type = 'read'
            query_detail_message = query_read_re.search(parsed['message'])
            if query_detail_message:
                query_detail = query_detail_message.groupdict()
            else:
                results_errors['parse_details'] += 1
                continue

            q_rpref = query_detail['q_rpref'].lower()
#            q_socket_time = query_detail['q_socket_time']
            q_nret = int(query_detail['q_nret'])
            q_size = int(query_detail['q_size'] )
        else:
            q_type =  'write'
            q_rpref = 'primary'
#            q_socket_time = 0
            q_nret = 0
            q_size = 0


        for q_shard in shards:
            if q_shard not in results_timings:
                results_timings[q_shard] = defaultdict(list)
                results_aggr_timings[q_shard] = defaultdict(list)
                results_aggr_count_nret[q_shard] = Counter()
                results_aggr_count_size[q_shard] = Counter()
                results_count_nret[q_shard] = Counter()
                results_count_size[q_shard] = Counter()

                for m in ['read', 'write', 'total']:
                    results_aggr_count_status[q_shard][m] = Counter()
                    results_aggr_count_rpref[q_shard][m] = Counter()

            if meter not in results_timings[q_shard]:
                results_count_status[q_shard][meter] = Counter()
                results_count_rpref[q_shard][meter] = Counter()

            results_timings[q_shard][meter].append(q_took)
            results_count_status[q_shard][meter][q_status] += 1
            results_count_nret[q_shard][meter] += q_nret
            results_count_size[q_shard][meter] += q_size
            results_count_rpref[q_shard][meter][q_rpref] += 1

            for meter in [q_type, 'total']:
                results_aggr_timings[q_shard][meter].append(q_took)
                results_aggr_count_status[q_shard][meter][q_status] += 1
                results_aggr_count_nret[q_shard][meter] += q_nret
                results_aggr_count_size[q_shard][meter] += q_size
                results_aggr_count_rpref[q_shard][meter][q_rpref] += 1


            if float(q_took) >= 0.3:
                results_aggr_count_mongo300[q_shard] += 1


    else:
        results_errors['parse_line'] += 1


# timings

for shard, results in sorted(results_timings.iteritems()):
    for query, timings in results.iteritems():
        if len(timings):
            print("query_timings_{}_{} {}".format(shard, query, ' '.join(timings) ))

for shard, results in sorted(results_aggr_timings.iteritems()):
    for query, timings in results.iteritems():
        if len(timings):
            print("query_aggr_timings_{}_{} {}".format(shard, query, ' '.join(timings) ))


# status

for shard, results in sorted(results_count_status.iteritems()):
    for op, values in sorted (results.iteritems()):
        for val_k, val_v in sorted (values.iteritems()):
            print ("query_count_status_{}_{}_{} {}".format(shard, op, val_k, val_v))


for shard, results in sorted(results_aggr_count_status.iteritems()):
    for op, values in sorted (results.iteritems()):
        for val_k, val_v in sorted (values.iteritems()):
            print ("query_aggr_count_status_{}_{}_{} {}".format(shard, op, val_k, val_v))


# rpref


for shard, results in sorted(results_count_rpref.iteritems()):
    for op, values in sorted (results.iteritems()):
        for val_k, val_v in sorted (values.iteritems()):
            print ("query_count_rpref_{}_{}_{} {}".format(shard, op, val_k, val_v))


for shard, results in sorted(results_aggr_count_rpref.iteritems()):
    for op, values in sorted (results.iteritems()):
        for val_k, val_v in sorted (values.iteritems()):
            print ("query_aggr_count_rpref_{}_{}_{} {}".format(shard, op, val_k, val_v))


# nret

for shard, results in sorted(results_count_nret.iteritems()):
    for val_k, val_v in sorted (results.iteritems()):
        print ("query_count_nret_{}_{} {}".format(shard, val_k, val_v))


for shard, results in sorted(results_aggr_count_nret.iteritems()):
    for val_k, val_v in sorted (results.iteritems()):
        print ("query_aggr_count_nret_{}_{} {}".format(shard, val_k, val_v))


# size

for shard, results in sorted(results_count_size.iteritems()):
    for val_k, val_v in sorted (results.iteritems()):
        print ("query_count_size_{}_{} {}".format(shard, val_k, val_v))


for shard, results in sorted(results_aggr_count_size.iteritems()):
    for val_k, val_v in sorted (results.iteritems()):
        print ("query_aggr_count_size_{}_{} {}".format(shard, val_k, val_v))


for shard, val_k in sorted(results_aggr_count_mongo300.iteritems()):
    print ("query_aggr_count_mongo300_{} {}".format(shard, val_k))

for error_k, error_v in sorted(results_errors.iteritems()):
    print ("error_{} {}".format(error_k, error_v))

