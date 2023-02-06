#!/usr/bin/python

import urllib2
import base64
import json
import yaml
import sys
from collections import defaultdict
from pprint import pprint


def get_queue_stat(queue):
    stat = defaultdict(int)
#    pprint(queue)
    for meter in ['messages', 'messages_unacknowledged']:
        stat[meter] = queue[meter]
    if 'message_stats' in queue:
        for meter in ['ack_details', 'deliver_details', 'deliver_get_details', 'disk_reads_details', 'disk_writes_details', 'publish_details', 'redeliver_details']:
            if meter in queue['message_stats']:
                stat['message_stats_{}_rate'.format(meter)] = queue['message_stats'][meter]['rate']

    return stat


usage = "{} vhost config\n".format(sys.argv[0])

if len(sys.argv) < 3:
    sys.stderr.write(usage)
    sys.exit(0)

rabbit_vhost = sys.argv[1]
rabbit_config_file = sys.argv[2]

rabbit_config = yaml.safe_load(open(rabbit_config_file))['rabbitmq-access'][rabbit_vhost]

rabbit_mqueue_stat_request = urllib2.Request('http://localhost:15672/api/queues/' + rabbit_vhost)

base64string = base64.encodestring('%s:%s' % (rabbit_config['username'], rabbit_config['password'])).replace('\n', '')
rabbit_mqueue_stat_request.add_header("Authorization", "Basic %s" % base64string)
result = urllib2.urlopen(rabbit_mqueue_stat_request)

data = result.read()
queues = json.loads(data)

#pprint (queues)

aggr_conf = {'queue_index' : ['index_general', 'index_music', 'index_reindex'],
         'queue_photoslice' : ['photoslice_general'],
         'queue_minor' : ['minor_general'],
         'queue_sync' : ['xiva_browser'],
         'queue_default' : [],
         'total' : [] }


plain_queues = defaultdict(dict)

aggr_queues = defaultdict(int)
for ag_name in aggr_conf:
    aggr_queues[ag_name] = dict()
    for meter in ['messages', 'messages_unacknowledged']:
        aggr_queues[ag_name][meter] = 0



for queue in queues:
    queue_name = queue['name']
    if queue_name not in ['submit', 'started', 'completed', 'pgRejected', 'tasks-default'] and not queue_name.startswith('disk_') and not queue_name.startswith('minor_') \
        and not queue_name.startswith('photoslice_') and not queue_name.startswith('index_'):
        continue
    stat = get_queue_stat(queue)
    plain_queues[queue_name] = stat

#    print "{}_count_messages_{} {}".format(rabbit_vhost, queue['name'], queue['messages'])
#    print "{}_count_messages_unacknowledged_{} {}".format(rabbit_vhost, queue['name'], queue['messages_unacknowledged'])


    aggr_groups = ['total']
    if queue_name != 'submit':
        aggr_name = 'queue_default'
        for ag_name, ag_list in aggr_conf.iteritems():
            if queue['name'] in ag_list:
                aggr_name = ag_name
                break
        aggr_groups.append(aggr_name)

    for ag_name in aggr_groups:
        for meter in stat:
            if meter not in aggr_queues[ag_name]:
                aggr_queues[ag_name][meter] = 0
            aggr_queues[ag_name][meter] += stat[meter]



for meter, values in plain_queues.iteritems():
    for value_k, value_v in sorted (values.items()):
        if value_v > 0:
            print "{}_count-{}-{} {}".format(rabbit_vhost, meter, value_k, value_v)


for meter, values in aggr_queues.iteritems():
    for value_k, value_v in sorted (values.items()):
        if value_v > 0:
            print "{}_aggr_count_{}-{} {}".format(rabbit_vhost, meter, value_k, value_v)


