#!/usr/bin/env python

# -*- coding: UTF-8 -*-

import os
import re
import sys

results_errors = { 'parse' : 0 }

meters_codes = [ 'code_1xx',
'code_2xx',
'code_3xx',
'code_4xx',
'code_5xx',
'code_unknown',
'all']


results_count_codes = dict()
for mc_key in meters_codes:
    results_count_codes[mc_key] = 0


meters_urls = {'get_zip-folder' : ['GET /zip-folder'],
    'post_upload-from-service' : ['POST /upload-from-service'],
    'get_zip-folder-public' : ['GET /zip-folder-public'],
    'get_regenerate-preview' : ['GET /regenerate-preview'],
    'put_patch-target' : ['PUT /patch-target'],
    'post_patch-url' : ['POST /patch-url'],
    'get_request-status' : ['GET /request-status'],
    'post_upload-url' : ['POST /upload-url'],
    'head_upload-target' : ['HEAD /upload-target'],
    'put_upload-target' : ['PUT /upload-target'],
    'get_generate-preview' : ['GET /generate-preview'],
    'loading-status' : ['GET /loading-status', 'POST /loading-status'],
}

urls_meters = dict();
for meter,urls in meters_urls.items():
    for url in urls:
        urls_meters[url] = meter

meters_urls['others'] = None
results_count_urls = dict() 
results_timings_urls = dict()
for mu_key in meters_urls:
    results_timings_urls[mu_key] = []
    results_count_urls[mu_key] = dict()
    for mc_key in meters_codes:
        results_count_urls[mu_key][mc_key] = 0


#####

index_re = re.compile(': \"([A-Z]+ \/[^\/?\"]*)?[\/?\"].* (\d{3}) (?:\d+\.\d+\.\d+\.\d+|[0-9a-fA-F:]+) \-?\d+ \d+ (\d+\.\d+)$')

for line in sys.stdin:

    line = line.strip()
    matches = index_re.findall(line)

    if len(matches):
        b_url = matches[0][0]
        u_code = int(matches[0][1])
        u_time = matches[0][2]

        if b_url in urls_meters:
            u_url = urls_meters[b_url]
            results_timings_urls[u_url].append(u_time)
	else:
	    u_url = 'others'

#        print "%s %s %s" % (u_url, u_code, u_time )

	if u_code < 200:
            results_count_urls[u_url]['code_1xx'] += 1
	elif u_code < 300:
            results_count_urls[u_url]['code_2xx'] += 1
	elif u_code < 400:
            results_count_urls[u_url]['code_3xx'] += 1
	elif u_code < 500:
            results_count_urls[u_url]['code_4xx'] += 1
	elif u_code < 600:
            results_count_urls[u_url]['code_5xx'] += 1
	else:
            results_count_urls[u_url]['code_unknown'] += 1

        results_count_urls[u_url]['all'] += 1


    else:
#        print line
	results_errors['parse'] += 1


for u_url, result in sorted(results_count_urls.items()):
    for u_code, value in sorted (result.items()):
	results_count_codes[u_code] += value
	print("uploader_count_request_%s_%s %d" % (u_url, u_code, value))
	
for u_code, value in sorted(results_count_codes.items()):
    print("uploader_count_request_total_%s %d" % (u_code, value))


for u_url, result in sorted(results_timings_urls.items()):
    print("uploader_timings_request_%s %s" % (u_url, ' '.join(result) ))


for error, value in sorted(results_errors.items()):
    print("uploader_count_access_error_%s %d" % (error, value))



sys.exit(0)

