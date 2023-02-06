#!/usr/bin/env python
# -*- coding: utf-8 -*

import os
import sys
import json
import time
import logging
import argparse
import datetime
import yt.wrapper as yt

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc


def get_timestamp():
    return int(time.mktime(datetime.datetime.now().timetuple()))


def _get_columns(client, table_path):
    table = client.read_table(table_path, format=yt.JsonFormat())
    row = table.next()
    keys = row.keys()
    keys.remove('Url')
    return keys


def set_fake_permalink(client, table_path, snippet_name, ts_field_name):
    table_path = '//{path}'.format(path=table_path.split('//')[-1])
    timestamp = str(get_timestamp())
    logging.info('Trying to set timestamp: %s' % timestamp)
    data = {'Url': ts_field_name or 'timestamp_{snp_name}'.format(snp_name=snippet_name)}
    for key in _get_columns(client, table_path):
        data.update({key: timestamp})
    client.write_table(yt.TablePath(table_path, append=True),
                       [data],
                       format=yt.JsonFormat(attributes={'encode_utf8': False}))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Adds monitoring timestamp to table')
    parser.add_argument('--cluster', type=str, help='YT cluster')
    parser.add_argument('--parameters',
                        type=str,
                        help='Dict with job parameters')
    args = parser.parse_args()
    params = json.loads(args.parameters)
    yt_client = misc.get_client(os.environ['YT_TOKEN'],
                                os.environ.get('YT_POOL'),
                                args.cluster)
    set_fake_permalink(yt_client,
                       params.get('processing_out') or params.get('generating_out'),
                       params.get('snippet_name'),
                       params.get('timestamp_field'))
