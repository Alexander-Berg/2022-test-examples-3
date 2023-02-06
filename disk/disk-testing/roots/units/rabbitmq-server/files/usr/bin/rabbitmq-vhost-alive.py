#!/usr/bin/python

import urllib2
import base64
import json
import yaml
import sys
from collections import defaultdict
from pprint import pprint


usage = "{} vhost config\n".format(sys.argv[0])

if len(sys.argv) < 3:
    sys.stderr.write(usage)
    sys.exit(0)

rabbit_vhost = sys.argv[1]
rabbit_config_file = sys.argv[2]

rabbit_config = yaml.safe_load(open(rabbit_config_file))['rabbitmq-access'][rabbit_vhost]

rabbit_mqueue_stat_request = urllib2.Request('http://localhost:15672/api/aliveness-test/' + rabbit_vhost)

base64string = base64.encodestring('%s:%s' % (rabbit_config['username'], rabbit_config['password'])).replace('\n', '') 
rabbit_mqueue_stat_request.add_header("Authorization", "Basic %s" % base64string)
result = urllib2.urlopen(rabbit_mqueue_stat_request)

data = result.read()
json_data = json.loads(data)
#pprint(json_data)

if json_data['status'] == 'ok':
    print "0; OK, aliveness-test/mqueue-vhost"
else:
    print "2; CRIT, aliveness-test/mqueue-vhost"
    
sys.exit(0)

