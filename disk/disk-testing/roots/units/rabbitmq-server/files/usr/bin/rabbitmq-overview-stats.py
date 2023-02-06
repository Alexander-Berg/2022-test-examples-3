#!/usr/bin/python

import urllib2
import base64
import json
import yaml
import sys
from collections import defaultdict
from pprint import pprint


usage = "{} vhost config\n".format(sys.argv[0])

if len(sys.argv) < 2:
    sys.stderr.write(usage)
    sys.exit(0)

rabbit_config_file = sys.argv[1]

rabbit_config = yaml.safe_load(open(rabbit_config_file))['rabbitmq-access'].values()[0]

rabbit_mqueue_stat_request = urllib2.Request('http://localhost:15672/api/overview')

base64string = base64.encodestring('%s:%s' % (rabbit_config['username'], rabbit_config['password'])).replace('\n', '')
rabbit_mqueue_stat_request.add_header("Authorization", "Basic %s" % base64string)
result = urllib2.urlopen(rabbit_mqueue_stat_request)

data = result.read()
json_data = json.loads(data)

#pprint (json_data)



for node_k,node_v in json_data['message_stats'].iteritems():
    if isinstance(node_v, dict) and 'rate' in node_v:
        print "overview_{}_rate {}".format(node_k,node_v['rate'])


for node_k,node_v in json_data['queue_totals'].iteritems():
    if isinstance(node_v, dict) and 'rate' in node_v:
        print "overview_{}_rate {}".format(node_k,node_v['rate'])
    else:
        print "overview_{} {}".format(node_k,node_v)


sys.exit(0)



