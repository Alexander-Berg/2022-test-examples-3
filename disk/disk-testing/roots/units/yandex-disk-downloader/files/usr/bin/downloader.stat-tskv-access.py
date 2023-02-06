#!/usr/bin/env python

# -*- coding: UTF-8 -*-

import os
import re
import sys
import pprint
from collections import defaultdict


results_errors = { 'parse' : 0 }

s_name = 'downloader';

meters_upstreams = {
    'authorizer' : ['unix:/var/run/fastcgi-blackbox-authorizer.sock'],
    'uploader' : [ '[2a02:6b8:0:3400::2:9]:80', '141.8.146.9:80' ],
    'resizer' : ['[2a02:6b8::161]:80', '213.180.204.161:80', '93.158.134.161:80', '213.180.193.161:80', '213.180.205.193:80', '[2a02:6b8:0:3400::4:193]:80'],
    'mulcagate' : ['127.0.0.1:10010'],
}

upstreams_meters = dict();
for meter,upstreams in meters_upstreams.items():
    for upstream in upstreams:
        upstreams_meters[upstream] = meter

timings_upstreams_all = [ 'authorizer', 'uploader', 'resizer' ]
count_upstreams_codes = [ 'authorizer', 'uploader', 'resizer', 'mulcagate' ]


results_count_upstreams_codes = dict()
for mu_key in count_upstreams_codes:
    results_count_upstreams_codes[mu_key] = defaultdict(int)

results_timings_upstreams_all = defaultdict(list)

## grab urls:
meters_urls = { 
    'get_preview' : ['GET /preview'],
    'get_rpreview' : ['GET /rpreview'],
    'get_disk' : ['GET /disk'],
    'get_rdisk' : ['GET /rdisk'],
    'get_share' : ['GET /share'],
    'get_rshare' : ['GET /rshare'],
    'get_zip' : ['GET /zip'],
    'get_rzip' : ['GET /rzip']
}

urls_meters = dict();
for meter,urls in meters_urls.items():
    for url in urls:
        urls_meters[url] = meter
meters_urls['others'] = None

# 
timings_urls_all = ['get_preview', 'get_disk']
timings_urls_lt1mb = [ 'get_rdisk' ]
count_urls_codes = ['get_preview' , 'get_rpreview', 'get_disk', 'get_rdisk', 'get_share', 'get_rshare', 'get_zip', 'get_rzip' ]
count_urls_caches = ['get_preview' , 'get_rpreview', 'get_disk', 'get_rdisk', 'get_share', 'get_rshare', 'get_zip', 'get_rzip' ]

# codes
results_count_urls_codes = dict() 
for mu_key in count_urls_codes:
    results_count_urls_codes[mu_key] = defaultdict(int)

# caches
results_count_urls_caches = dict()
for mu_key in count_urls_caches:
    results_count_urls_caches[mu_key] = defaultdict(int)

# timings

results_timings_urls_all = defaultdict(list)

results_timings_urls_lt1mb = defaultdict(list)


#####

#index_re = re.compile(': \"([A-Z]+ \/[^\/?\"]*)?[\/?\"].* (\d{3}) (?:\d+\.\d+\.\d+\.\d+|[0-9a-fA-F:]+) \-?\d+ \d+ (\d+\.\d+)$')

url_re = re.compile('^/([^/]+)/[^\?]+\?uid=(\d+)&')
#url_re = re.compile('^/([^/]+)/')
#url_re = re.compile('/([^/]+)/')

for line in sys.stdin:
#    print line

    parts = line.strip("\n").split("\t")
    parsed = {};
    for part in parts:
        eq = part.find('=')
        key = part[:eq]
        value = part[(eq+1):]
        parsed[key] = value
        #pprint.pprint (parsed)

    u_cache = parsed.get('upstream_cache_status', '-');
    u_bytes_sent = int(parsed['bytes_sent'])

    url_matches = url_re.findall(parsed['request'])
    if len(url_matches):
        b_request = url_matches[0][0]
        b_url = parsed['method'] + ' /' + b_request
        if b_url in urls_meters:
            u_url = urls_meters[b_url]
        else:
            u_url = 'others'

        u_time = parsed['request_time']
        if u_url in timings_urls_all:
            results_timings_urls_all[u_url].append(u_time)
        elif (u_url in timings_urls_lt1mb) and (u_bytes_sent < 1048576):
            results_timings_urls_lt1mb[u_url].append(u_time)


        if u_url in count_urls_codes:
            u_code = int(parsed['status'])
            results_count_urls_codes[u_url][u_code] += 1

        if u_url in count_urls_caches:
            u_cache = parsed.get('upstream_cache_status', '-').lower();
            if u_cache in ['hit', 'miss']:
                results_count_urls_caches[u_url][u_cache] += 1


            l_upstream_addr = parsed['upstream_addr'].split(' : ')
            l_upstream_response_time = parsed['upstream_response_time'].split(' : ')
            l_upstream_status = parsed['upstream_status'].split(' : ')

            i = 0
            for b_upstream_addr in l_upstream_addr:
                if b_upstream_addr in upstreams_meters:
                    u_upstream_addr = upstreams_meters[b_upstream_addr]
                    
                    if u_upstream_addr in count_upstreams_codes and l_upstream_status[i].isdigit() :
                        u_upstream_status = int(l_upstream_status[i])
                        results_count_upstreams_codes[u_upstream_addr][u_upstream_status] += 1

                    if u_upstream_addr in timings_upstreams_all:
                        try:
                            float(l_upstream_response_time[i])
                            results_timings_upstreams_all[u_upstream_addr].append(l_upstream_response_time[i])
                        except:
                            pass
                i += 1


    else:
        results_errors['parse'] += 1
#        print (line)
        continue
#    print "%s %s %s" % (u_url, u_code, u_time )

    



# count_urls_codes (codes per url)
for u_url, result in sorted(results_count_urls_codes.items()):
    for u_code, value in sorted (result.items()):
        print("%s_count_request_%s_code_%s %d" % (s_name, u_url, u_code, value))


for u_url, result in sorted(results_count_urls_caches.items()):
    for u_cache, value in sorted (result.items()):
        print("%s_count_request_%s_cache_%s %d" % (s_name, u_url, u_cache, value))
     

for u_upstream, result in sorted(results_count_upstreams_codes.items()):
    for u_code, value in sorted (result.items()):
        print("%s_count_upstream_%s_code_%s %d" % (s_name, u_upstream, u_code, value))


for u_upstream, result in sorted(results_timings_upstreams_all.items()):
    if len(result):
        print("%s_timings_upstream_%s %s" % (s_name, u_upstream, ' '.join(result) ))     




for u_url, result in sorted(results_timings_urls_lt1mb.items()):
    if len(result):
        print("%s_timings_request_%s_lt1mb %s" % (s_name, u_url, ' '.join(result) ))


for u_url, result in sorted(results_timings_urls_all.items()):
    if len(result):
        print("%s_timings_request_%s %s" % (s_name, u_url, ' '.join(result) ))



for error, value in sorted(results_errors.items()):
    print("%s_error_%s %d" % (s_name, error, value))



sys.exit(0)

