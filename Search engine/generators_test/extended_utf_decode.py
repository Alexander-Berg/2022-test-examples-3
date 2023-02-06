#!/usr/bin/env python
# -*- coding: utf-8 -*

import os
import sys
import json
import tarfile
import argparse
import yt.wrapper as yt

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc


def map(params, client):

    @yt.with_context
    def decode(row, context):
        value = json.loads(row.get(params.get('data_field')))
        data = {'key': str(row.get(params.get('permalink_field'))),
                'value': misc.format_snippet(params.get('snippet_name'),
                                             json.dumps(value, ensure_ascii=False)),
                '@table_index': 0}
        ferryman_data = {'Url': str(row.get(params.get('permalink_field'))),
                         params.get('snippet_name'): json.dumps(value, ensure_ascii=False),
                         '@table_index': 1}
        yield data
        yield ferryman_data

    client.run_map(decode,
                   params.get('pre_processing_out') or params.get('input_table'),
                   [params.get('generating_out') or params.get('processing_out'),
                    params.get('ferryman_out')],
                   format=yt.JsonFormat(control_attributes_mode="row_fields",
                                        attributes={'encode_utf8': False}))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Addrs snippets mapper')
    parser.add_argument('--cluster', type=str, help='YT cluster')
    parser.add_argument('--parameters',
                        type=str,
                        help='Dict with job parameters')
    args = parser.parse_args()
    yt_client = misc.get_client(os.environ['YT_TOKEN'],
                                os.environ.get('YT_POOL'),
                                args.cluster,
                                generation_stage=True)
    params = json.loads(args.parameters)
    map(params, yt_client)
