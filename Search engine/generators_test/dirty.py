#!/usr/bin/env python
# -*- coding: utf-8 -*

import os
import sys
import json
import argparse
import yt.wrapper as yt
import xml.etree.ElementTree as ET

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Generates RBC snippets')
    parser.add_argument('--cluster',type=str, help='YT cluster')
    parser.add_argument('--parameters', type=str, help='Dict with job parameters')
    args = parser.parse_args()
    yt_client = misc.get_client(os.environ['YT_TOKEN'], args.cluster)
    #params = json.loads(args.parameters)

    #schema = [
        #{'name': 'dumper.rps', 'type': 'double'},
        #{'name': 'latency_0.95', 'type': 'double'},
        #{'name': 'latency_0.99', 'type': 'double'},
        #{'name': 'memory_rss', 'type': 'int64'},
        #{'name': 'memory_vsz', 'type': 'int64'}
    #]
    #table_path = '//home/geosearch-prod/addrs_base/perf'
    #yt_data = yt_client.read_table('//tmp/addrs_perf', format='json')
    #data = [d for d in yt_data]
    ##yt_client.remove(table_path, force=True)
    #yt_client.create_table(table_path, attributes={"schema": schema})
    #yt_client.write_table(table_path, data, format='json')
    yt_client.create('map_node', '//home/geosearch-prod/snippets/validation_errors')
