#!/usr/bin/env python
# -*- coding: utf-8 -*

import os
import sys
import json
import argparse
import yt.wrapper as yt

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc


def get_poster_url(event):
    try:
        poster = event['poster']
        return poster['origin_url']
    except (KeyError, TypeError):
        return None


def map(input_table, output_table, ferryman_table, client):

    @yt.with_context
    def make_photo_filter_snippet(row, context):
        if row.get('event.type.code') == 'cinema':
            poster_url = get_poster_url(row['event'])
            if poster_url:
                data = {'Url': 'events_{event_id}'.format(event_id=row.get('event.id')),
                        'filters_extra_img/1.x': poster_url,
                        '@table_index': 0}
                yield data

    client.run_map(make_photo_filter_snippet,
                   input_table,
                   output_table,
                   format=yt.JsonFormat(control_attributes_mode="row_fields"))
    client.set_attribute(output_table,
                         'expiration_time',
                         misc.get_ttl(params.get('yt_ttl', 1)))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Photo filter snippets mapper')
    parser.add_argument('--cluster', type=str, help='YT cluster')
    parser.add_argument('--parameters', type=str, help='Dict with job parameters')
    args = parser.parse_args()
    params = json.loads(args.parameters)
    yt_client = misc.get_client(os.environ['YT_TOKEN'],
                                os.environ['YT_POOL'],
                                args.cluster)
    map(params.get('input_table'),
        params.get('processing_out'),
        params.get('ferryman_out'),
        yt_client)
