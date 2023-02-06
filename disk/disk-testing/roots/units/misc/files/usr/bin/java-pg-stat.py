#!/usr/bin/env python

# -*- coding: UTF-8 -*-

import sys
import re
from pprint import pprint
from collections import defaultdict, Counter


results_errors = Counter()

# Collect errors from logs
results_log_errors = dict()
for error in ['parse']:
    results_log_errors[error] = 0


results_count_query_status = dict()
results_aggr_qtype_count_query_status = dict()

results_timings_query = dict()
results_aggr_qtype_timings_query = dict()


#tskv    tskv_format=ydisk-dataapi-log   logtime=2016-08-18 16:30:46,125 logtimezone=+0300       unixtime=1471527046     level=DEBUG     thread=qtp614785733-218 ycrid=rest-1483b026db9558e4f2290cd7a57dee0f-api06g      rid=YipQKbZq    class=r.y.ch.jdbc.q  message=Q: SELECT * FROM databases_093 WHERE (app = 'profile') AND (user_id = '43969629') AND (dbId = 'addresses'): completed at master apidb01d.disk.yandex.net/api_disk_data, rc=1; took 0.001


q_query_re = re.compile('(?:\(LONG\) )?Q: (?P<q_op>[A-Z]+) .*: (?P<q_status>completed|failed) at (?P<q_state>master|slave) (?P<q_host>(?P<q_cluster>[^\d]+)[^\/]+)\/(?P<q_db>[^,;]+)(?:, rc=(?P<q_rc>\d+))?; took (?P<q_took>\d+.\d+)')



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

    if parsed['class'] == 'r.y.ch.jdbc.q':
        q_query_matches = q_query_re.search(message)
        if q_query_matches:
            q_op = q_query_matches.group('q_op').lower()
            q_status = q_query_matches.group('q_status')
            q_state = q_query_matches.group('q_state')
            q_host = q_query_matches.group('q_host').replace('.', '_')
            q_took = q_query_matches.group('q_took')
            q_cluster = q_query_matches.group('q_cluster')

#            print line
#            print "{} {} {} {} {}".format(q_cluster, q_state, q_host, q_status, q_took)

            if q_op == 'select': # mb inaccuracy
                q_type = 'read'
            elif q_op in ['begin', 'commit', 'rollback']:
                q_type = 'transproc'
            else:
                q_type = 'write'



            for meter in [q_state, q_host, 'total']:
                if q_cluster not in results_count_query_status:
                    results_count_query_status[q_cluster] = dict()
                    results_aggr_qtype_count_query_status[q_cluster] = dict()
                    results_timings_query[q_cluster] = dict()
                    results_aggr_qtype_timings_query[q_cluster] = dict()


                if meter not in results_count_query_status[q_cluster]:
                    results_count_query_status[q_cluster][meter] = defaultdict(Counter)
                    results_aggr_qtype_count_query_status[q_cluster][meter] = defaultdict(Counter)
                    results_timings_query[q_cluster][meter] = defaultdict(list)
                    results_aggr_qtype_timings_query[q_cluster][meter] = defaultdict(list)


                results_count_query_status[q_cluster][meter][q_op][q_status] += 1
                results_aggr_qtype_count_query_status[q_cluster][meter][q_type][q_status] += 1

                results_timings_query[q_cluster][meter][q_op].append(q_took)
                results_aggr_qtype_timings_query[q_cluster][meter][q_type].append(q_took)

        else:
            results_errors['parse_line'] += 1
            
   

for q_cluster, q_cluster_stats in results_count_query_status.iteritems():
    for q_meter, q_meter_stats in q_cluster_stats.iteritems():
        for q_type, q_type_stats in q_meter_stats.iteritems():
            for q_status, q_status_v in q_type_stats.iteritems():
                print "query_status_{}_{}_{}_{} {}".format(q_cluster, q_meter, q_type, q_status, q_status_v)


for q_cluster, q_cluster_stats in results_aggr_qtype_count_query_status.iteritems():
    for q_meter, q_meter_stats in q_cluster_stats.iteritems():
        for q_type, q_type_stats in q_meter_stats.iteritems():
            for q_status, q_status_v in q_type_stats.iteritems():
                print "query_aggr_qtype_status_{}_{}_{}_{} {}".format(q_cluster, q_meter, q_type, q_status, q_status_v)



for q_cluster, q_cluster_stats in results_timings_query.iteritems():
    for q_meter, q_meter_stats in q_cluster_stats.iteritems():
        for q_type, q_type_stats in q_meter_stats.iteritems():
            print "query_timings_{}_{}_{} {}".format(q_cluster, q_meter, q_type, ' '.join(q_type_stats))



for q_cluster, q_cluster_stats in results_aggr_qtype_timings_query.iteritems():
    for q_meter, q_meter_stats in q_cluster_stats.iteritems():
        for q_type, q_type_stats in q_meter_stats.iteritems():
            print "query_aggr_qtype_timings_{}_{}_{} {}".format(q_cluster, q_meter, q_type, ' '.join(q_type_stats))




