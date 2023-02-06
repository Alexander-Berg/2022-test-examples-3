#!/usr/bin/env python

# -*- coding: UTF-8 -*-

import os
import re
import sys
import time
import urllib2
import json
#from pprint import pprint
from collections import defaultdict, Counter
import time

def http_get_json_cache(url, cache_file, cache_ttl = 300, retries = 1, sleep = 1):

    cache_need_update = True
    if os.path.exists(cache_file):
        ts_now = time.time()
        ts_cache = os.path.getmtime(cache_file)
        if (ts_now - ts_cache) < cache_ttl:
            cache_need_update = False
        
    if cache_need_update:
        json_data = http_get_json(url, retries, sleep)
        if json_data:
            cache_update(json.dumps(json_data), cache_file)

    cache_data = cache_read(cache_file)
    json_data = json.loads(cache_data)

    return json_data;
        
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


def http_get_json(url, retries, sleep):
    json_data = dict()
    for i in xrange(retries):
        try:
            bdata = http_get(url, retries, sleep)
            json_data = json.loads(bdata)
            if len(json_data) < 1:
                json_data = False
            else:
                break 
        except:
            json_data = False
    return json_data
    

def cache_update(data, cache_file):
    result = dict()
    try:
        cache_dir = os.path.dirname(cache_file)
        if not os.path.exists(cache_dir):
            os.mkdir(cache_dir)
        
        cache_fd = open(cache_file, "w")
        cache_fd.write(data)
        cache_fd.close()
    except:
        result = False

    return result


def cache_read(cache_file):
    try:
        cache_fd = open(cache_file, "r")
        result = cache_fd.read() 
        cache_fd.close()
    except:
        result = False
    
    return result



    

### get queller hosts

conductor_quellermap_url = 'http://c.yandex-team.ru/api-cached/groups2hosts/disk_queller?format=json'
conductor_quellermap_cache_file = "/var/cache/mworker-jobs-stat/quellermap.cache"
conductor_quellermap_cache_ttl = 300
conductor_quellermap_retries = 2
conductor_quellermap_delay = 1

quellermap = http_get_json_cache(url = conductor_quellermap_url, cache_file = conductor_quellermap_cache_file, cache_ttl = conductor_quellermap_cache_ttl, retries = conductor_quellermap_retries, sleep = conductor_quellermap_delay)


### get task on queue mapping from queller

queller_taskmap_cache_file = "/var/cache/mworker-jobs-stat/taskmap.cache"
queller_taskmap_retries = 1
queller_taskmap_sleep = 1

taskmap = dict()
for queller in quellermap:
    queller_taskmap_url = "http://{}:30811/z/celery-tasks.json".format(queller['fqdn'])
    taskmap = http_get_json_cache(url = queller_taskmap_url, cache_file = queller_taskmap_cache_file, retries = queller_taskmap_retries, sleep = queller_taskmap_sleep)
    if len(taskmap) > 0:
        break


task_queue = dict()
if len(taskmap) > 0:
    for match in taskmap['tasks']:
        task = match['id'].replace('.','-')
        queue = match['queue']
        task_queue[task] = queue
    

results_errors = { 'parse' : 0 }

# Collect errors from logs
results_log_errors = dict()
for error in ['parse']:
    results_log_errors[error] = 0


### task stats

# "status" field
results_count_task_status = defaultdict(Counter)
results_count_aggr_task_status = defaultdict(Counter)

# "task_status" field
results_count_task_tstatus = defaultdict(Counter)
results_count_aggr_task_tstatus = defaultdict(Counter)


# lifetime 
results_timings_task_lifetime = defaultdict(list)
results_timings_task_processed = defaultdict(list)

results_time_task_lifetime = defaultdict(int)
results_time_task_processed = defaultdict(int)

### queue status
results_count_queues_status = defaultdict(Counter)
results_count_queues_tstatus = defaultdict(Counter)

results_timings_queues_lifetime = defaultdict(list)
results_timings_queues_processed = defaultdict(list)

results_time_queues_lifetime = defaultdict(int)
results_time_queues_processed = defaultdict(int)

### operations stats

results_count_opertype_status = defaultdict(Counter)
results_count_opertype_tstatus = defaultdict(Counter)

results_count_opertype_title = defaultdict(Counter)

results_count_aggr_opertype_title = defaultdict(Counter)

results_timings_opertype_lifetime = defaultdict(list)
results_timings_opertype_processed = defaultdict(list)

#time_re = re.compile('Task [^ ]+ (?P<status>OK|FAIL|TEMP_FAIL) \(try [-\d]+\), name: (?P<task_name>[^ ]+) \((?P<oper_type>[\w_-]+), (?P<oper_subtype>[\w_-]+)\) \(processed: (?P<processed>\d+\.\d+) sec, lifetime: (?P<lifetime>\d+\.\d+) sec\)$')

time_re = re.compile(r'Task [^ ]+ (?P<status>OK|FAIL|TEMP_FAIL) \(try [-\d]+\), name: (?P<task_name>[^ ]+) \((?P<oper_type>[\w_-]+), (?P<oper_subtype>[\w_-]+)\) \(processed: (?P<processed>\d+\.\d+) sec, lifetime: (?P<lifetime>\d+\.\d+) sec\), task_status: (?P<task_status>[\w_-]+), oper_state: (?P<oper_state>[\w_-]+), oper_title: (?P<oper_title>[\w_-]+), oper_id: [^ ,]+')


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



    time_matches = re.search(time_re, parsed['message'])

    if time_matches:
        task_name = time_matches.group('task_name').replace('.','-')
        status = time_matches.group('status').lower()
        tstatus = time_matches.group('task_status').lower()
        processed = time_matches.group('processed')
        lifetime = time_matches.group('lifetime')
        oper_type = time_matches.group('oper_type')
        oper_title = time_matches.group('oper_title').lower()
        

        results_timings_task_lifetime[task_name].append(lifetime)
        results_timings_task_processed[task_name].append(processed)

        results_time_task_lifetime[task_name] += float(lifetime)
        results_time_task_processed[task_name] += float(processed)

        if oper_type != '-':
            results_count_opertype_status[oper_type][status] += 1
            results_count_opertype_tstatus[oper_type][tstatus] += 1
            results_count_opertype_title[oper_type][oper_title] += 1
            results_timings_opertype_lifetime[oper_type].append(lifetime)
            results_timings_opertype_processed[oper_type].append(processed)

            results_count_aggr_opertype_title[oper_type][oper_title] += 1
            
 
            
        
        results_count_task_status[task_name][status] += 1
        results_count_task_tstatus[task_name][tstatus] += 1

        results_count_aggr_task_status['total'][status] += 1
        results_count_aggr_task_tstatus['total'][tstatus] += 1


        # per queue
        if task_name in task_queue:
            queue_name = task_queue[task_name]
            results_timings_queues_lifetime[queue_name].append(lifetime)
            results_timings_queues_processed[queue_name].append(processed)

            results_time_queues_lifetime[queue_name] += float(lifetime)
            results_time_queues_processed[queue_name] += float(processed)
            results_count_queues_status[queue_name][status] += 1
            results_count_queues_tstatus[queue_name][tstatus] += 1



### TASK

# lifetime timings
for task_name, timings in sorted(results_timings_task_lifetime.iteritems()):
    if len(timings):
        print("mpfs_timings_lifetime_task_{} {}".format(task_name, ' '.join(timings) ))

# lifetime summary
for task_name, time in sorted (results_time_task_lifetime.iteritems()):
    print("mpfs_count_lifetime_time_task_{} {:0.2f}".format(task_name, time))

# processed time timings
for task_name, timings in sorted(results_timings_task_processed.iteritems()):
    if len(timings):
        print("mpfs_timings_processed_task_{} {}".format(task_name, ' '.join(timings) ))

# processed time summary
for task_name, time in sorted (results_time_task_processed.iteritems()):
    print("mpfs_count_processed_time_task_{} {:0.2f}".format(task_name, time))

#
for error, value in sorted(results_errors.items()):
    print("mpfs_error_{} {}".format(error, value))


# task status count
for task_name, result in sorted(results_count_task_status.iteritems()):
    for status_k, status_v in sorted (result.items()):
        print("mpfs_count_status_task_{}_{} {}".format(task_name, status_k, status_v))

# aggr task status count
for aggr_name, result in sorted(results_count_aggr_task_status.iteritems()):
    for status_k, status_v in sorted (result.items()):
        print("mpfs_count_aggr_task_status_{}_{} {}".format(aggr_name, status_k, status_v))


# task tstatus count
for task_name, result in sorted(results_count_task_tstatus.iteritems()):
    for tstatus_k, tstatus_v in sorted (result.items()):
        print("mpfs_count_tstatus_task_{}_{} {}".format(task_name, tstatus_k, tstatus_v))

# aggr tstatus count
for aggr_name, result in sorted(results_count_aggr_task_tstatus.iteritems()):
    for tstatus_k, tstatus_v in sorted (result.items()):
        print("mpfs_count_aggr_task_tstatus_{}_{} {}".format(aggr_name, tstatus_k, tstatus_v))


### QUEUES

# queue lifetime timings
for queue_name, timings in sorted(results_timings_queues_lifetime.iteritems()):
    if len(timings):
        print("mpfs_timings_lifetime_queue_{} {}".format(queue_name, ' '.join(timings) ))


# queue lifetime summary
for queue_name, time in sorted (results_time_queues_lifetime.iteritems()):
    print("mpfs_count_lifetime_time_queue_{} {:0.2f}".format(queue_name, time))


# queue processed timings
for queue_name, timings in sorted(results_timings_queues_processed.iteritems()):
    if len(timings):
        print("mpfs_timings_processed_queue_{} {}".format(queue_name, ' '.join(timings) ))


# queue processed summary
for queue_name, time in sorted (results_time_queues_processed.iteritems()):
    print("mpfs_count_processed_time_queue_{} {:0.2f}".format(queue_name, time))


# queue status count
for queue_name, result in sorted(results_count_queues_status.iteritems()):
    for status_k, status_v in sorted (result.items()):
        print("mpfs_count_status_queue_{}_{} {}".format(queue_name, status_k, status_v))




### OPERATIONS

for opertype, result in sorted(results_count_opertype_status.iteritems()):
    for status_k, status_v in sorted (result.items()):
        print("mpfs_count_status_opertype_{}_{} {}".format(opertype, status_k, status_v))

for opertype, result in sorted(results_count_opertype_tstatus.iteritems()):
    for tstatus_k, tstatus_v in sorted (result.items()):
        print("mpfs_count_tstatus_opertype_{}_{} {}".format(opertype, tstatus_k, tstatus_v))


for opertype, result in sorted(results_count_opertype_title.iteritems()):
    for title_k, title_v in sorted (result.items()):
        print("mpfs_count_title_opertype_{}_{} {}".format(opertype, title_k, title_v))


for aggr_opertype, result in sorted(results_count_aggr_opertype_title.iteritems()):
    for title_k, title_v in sorted (result.items()):
        print("mpfs_count_aggr_title_opertype_{}_{} {}".format(aggr_opertype, title_k, title_v))


# lifetime, processed timings

for opertype, timings in sorted(results_timings_opertype_lifetime.iteritems()):
    if len(timings):
        print("mpfs_timings_processed_opertype_{} {}".format(opertype, ' '.join(timings) ))

for opertype, timings in sorted(results_timings_opertype_processed.iteritems()):
    if len(timings):
        print("mpfs_timings_lifetime_opertype_{} {}".format(opertype, ' '.join(timings) ))



sys.exit(0)


