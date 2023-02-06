#!/usr/bin/env python
# -*- coding: UTF-8 -*-

import os
import re
import sys
import subprocess
import shlex
import httplib
import pprint
from collections import defaultdict
import json


meters_urls_json = {
    'thread_pool_total_count' : ['GET', '/z/manager/bean/dataJetty/method/getThreadPoolTotalCount/invoke.json'],
    'thread_pool_idle_count' : ['GET', '/z/manager/bean/dataJetty/method/getThreadPoolIdleCount/invoke.json'],
}
meters_urls_plain = {
    'http_max_threads' : ['GET', '/properties?property=http.maxThreads']
 }

sensors_urls_main = {'queue_incomplete_requests_count' : ['GET', '/sensor/queueSensors.incompleteRequestsCount()'],
                'queue_incomplete_requests_waiting_for_user_count' : ['GET', '/sensor/queueSensors.incompleteRequestsWaitingForUserCount()'],
                'queue_incomplete_and_uploaded_locally' : ['GET', '/sensor/queueSensors.incompleteAndUploadedLocally()'],
                'queue_incomplete_and_uploaded_locally_and_not_uploaded_to_mulca' : ['GET', '/sensor/queueSensors.incompleteAndUploadedLocallyAndNotUploadedToMulca'],
                'queue_incomplete_and_uploaded_to_mulca' : ['GET', '/sensor/queueSensors.incompleteAndUploadedToMulca()'],
}

sensors_urls_ep = dict()
sensors_urls_ufs = dict()

results_sensors = defaultdict(int)

conn = httplib.HTTPConnection('127.0.0.1', 32451)

# get imports:
conn.request('GET', '/sensor/externalResourceSensors.allServices()')
resp = conn.getresponse()
for srv in resp.read().strip()[1:-1].split(", "):
    sensors_urls_ufs['queue_incomplete_ufs_' + srv.lower()] = ['GET', '/sensor/queueSensors.uploadFromServiceIncompleteCount(%s)' % srv ]

# get exports:
conn.request('GET', '/sensor/externalResourceSensors.exportServices()')
resp = conn.getresponse()
for srv in resp.read().strip()[1:-1].split(", "):
    sensors_urls_ep['queue_incomplete_ep_' + srv.lower()] = ['GET', '/sensor/queueSensors.exportPhotosCountIncompleteCount(%s)' % srv ]


for sensor, req in sensors_urls_main.items():
    method, url =  req
    conn.request(method, url)
    resp = conn.getresponse()
    results_sensors[sensor]=int(resp.read().strip("\n"))


for sensor, req in sensors_urls_ufs.items():
    method, url =  req
    conn.request(method, url)
    resp = conn.getresponse()
    value = int(resp.read().strip("\n"))
    results_sensors[sensor]= value
    results_sensors['queue_incomplete_ufs_total'] += value


for sensor, req in sensors_urls_ep.items():
    method, url =  req
    conn.request(method, url)
    resp = conn.getresponse()
    value = int(resp.read().strip("\n"))
    results_sensors[sensor]= value
    results_sensors['queue_incomplete_ep_total'] += value

## threads
for meter in meters_urls_json:
    conn.request(meters_urls_json[meter][0], meters_urls_json[meter][1])
    data = json.load(conn.getresponse())
    results_sensors[meter] = int(data['invocationInfo']['result'])


for meter in meters_urls_plain:
    conn.request(meters_urls_plain[meter][0], meters_urls_plain[meter][1])
    data = conn.getresponse().read()
    results_sensors[meter] = int(data)


conn.close()

results_sensors['thread_free_count'] = results_sensors['http_max_threads'] - results_sensors['thread_pool_total_count'] + results_sensors['thread_pool_idle_count']

for sensor, value in sorted(results_sensors.items()):
    print("uploader_sensors_%s %d" % (sensor, value))

sys.exit(0)

