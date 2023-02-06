#!/usr/bin/env python
# -*- coding: utf-8 -*

import os
import sys
import json
import argparse
import yt.wrapper as yt

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc


def map(params, client):

    @yt.with_context
    def make_yabilet_snippet(row, context):
        if row.get('provider_permalink') == 'yabilet':
            snippet_text = '<yabiletId>{orig_id}</yabiletId>'.format(orig_id=row.get('original_id'))
            data = {'Url': str(row.get('company_permalink')),
                    params.get('snippet_name'): snippet_text,
                    '@table_index': 0}
            yield data

    client.run_map(make_yabilet_snippet,
                   params.get('pre_processing_out') or params.get('input_table'),
                   params.get('generating_out') or params.get('processing_out'),
                   format=yt.JsonFormat(control_attributes_mode='row_fields'))
    client.set_attribute(params.get('generating_out') or params.get('processing_out'),
                         'expiration_time',
                         misc.get_ttl(params.get('yt_ttl', 1)))
    client.set_attribute(params.get('ferryman_out'),
                         'expiration_time',
                         misc.get_ttl(params.get('yt_ttl', 1)))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Yabilet snippets mapper')
    parser.add_argument('--cluster', type=str, help='YT cluster')
    parser.add_argument('--parameters', type=str, help='Dict with job parameters')
    args = parser.parse_args()
    params = json.loads(args.parameters)
    yt_client = misc.get_client(os.environ['YT_TOKEN'],
                                os.environ['YT_POOL'],
                                args.cluster)
    map(params, yt_client)
