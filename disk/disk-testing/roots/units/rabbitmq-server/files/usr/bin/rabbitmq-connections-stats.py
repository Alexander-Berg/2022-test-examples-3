#!/usr/bin/python

import urllib2
import base64
import json
import yaml
import sys
from collections import defaultdict
from pprint import pprint

usage = "{} config_file\n".format(sys.argv[0])

if len(sys.argv) < 2:
    sys.stderr.write(usage)
    sys.exit(0)

rabbit_config_file = sys.argv[1]

rabbit_config = yaml.safe_load(open(rabbit_config_file))['rabbitmq-access'].values()[0]
rabbit_mqueue_stat_request = urllib2.Request('http://localhost:15672/api/connections')

base64string = base64.encodestring('%s:%s' % (rabbit_config['username'], rabbit_config['password'])).replace('\n', '')
rabbit_mqueue_stat_request.add_header("Authorization", "Basic %s" % base64string)
result = urllib2.urlopen(rabbit_mqueue_stat_request)

data = result.read()
json_data = json.loads(data)

results_clients = defaultdict(dict)
results_connections = defaultdict(int)

for node in json_data:
    # channels : 2
    # client_properties : {"product" : "ololo"}
    # state : 'running'
#    pprint(node)
    b_client = node['client_properties']['product'].lower()
    if 'platform' in node['client_properties']:
        b_client = node['client_properties']['platform'].lower() + "-" + b_client

#    print client
    for client in [b_client, 'total']:
        if client not in results_clients:
            results_clients[client]['channels'] = 0
            results_clients[client]['count'] = 0
        results_clients[client]['channels'] += node['channels']
        results_clients[client]['count'] += 1


    if node['ssl'] == False:
        results_connections['plain'] += 1
    elif node['ssl'] == True:
        results_connections['ssl'] += 1

    results_connections['total'] += 1



for meter, values in results_clients.iteritems():
    for value_k, value_v in sorted (values.items()):
        print "count_client_{}_{} {}".format(meter, value_k, value_v)


for meter,value in results_connections.iteritems():
    print "count_connections_{} {}".format(meter,value)
        
