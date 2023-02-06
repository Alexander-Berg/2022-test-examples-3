#!/usr/bin/env python
# -*- coding: utf-8 -*

import os
import re
import sys
import json
import argparse
import yt.wrapper as yt

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc


def parse_src(row):
    rgx = re.compile(r'^(?:https?:\/\/)?(?:www\.)?([^\/]+)')
    url = rgx.search(row.get('url')).group(1).lower()
    if url != 'yandex.ru':
        yield {'url': url,
               'orig_url': row.get('url'),
               'value': {'org_name': row.get('org_name'),
                         'socket_url': row.get('socket_url'),
                         'org_id': row.get('org_id'),
                         'title': row.get('title'),
                         'wait_time': row.get('wait_time'),
                         'bot_id': row.get('bot_id')}}

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=('Prepate data for '
                                                  '"chat_xml/1.x" snippets'))
    parser.add_argument('--cluster', type=str, help='YT cluster')
    parser.add_argument('--parameters',
                        type=str,
                        help='Dict with job parameters')
    args = parser.parse_args()
    yt_client = misc.get_client(os.environ['YT_TOKEN'], args.cluster)
    params = json.loads(args.parameters)
    yt_client.run_map(parse_src,
                      params.get('input_table'),
                      params.get('pre_processing_out'),
                      format=yt.JsonFormat(attributes={"encode_utf8": False}))
