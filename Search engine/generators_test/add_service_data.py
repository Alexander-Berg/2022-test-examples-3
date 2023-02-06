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


def _get_columns(client, table_path):
    table = client.read_table(table_path, format=yt.JsonFormat())
    row = table.next()
    keys = row.keys()
    keys.remove('Url')
    return keys


def get_timestamp():
    return int(time.mktime(datetime.datetime.now().timetuple()))


def get_error_count(client, err_table):
    try:
        if client.exists(err_table):
            return int(client.get_attribute(err_table, 'row_count'))
        return 0
    except Exception:
        return 0


def set_fake_permalinks(client, table_path, snippet_name, ts_field_name, err_table, err_field_name):
    table_path = '//{path}'.format(path=table_path.split('//')[-1])
    if not client.exists(table_path):
        logging.info('Table %s does not exists' % table_path)
        return
    err_count = str(get_error_count(client, err_table))
    timestamp = str(get_timestamp())
    logging.info('Trying to set timestamp: %s' % timestamp)
    ts_row = {'Url': ts_field_name or 'timestamp_{snp_name}'.format(snp_name=snippet_name)}
    err_row = {'Url': err_field_name or 'errors_{snp_name}'.format(snp_name=snippet_name)}
    for key in _get_columns(client, table_path):
        ts_row.update({key: timestamp})
        err_row.update({key: err_count})
    client.write_table(yt.TablePath(table_path, append=True),
                       [err_row, ts_row],
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
    set_fake_permalinks(
        yt_client,
        params.get('processing_out') or params.get('generating_out'),
        params.get('snippet_name'),
        params.get('timestamp_field'),
        params.get('error_log'),
        params.get('error_count_field')
    )
