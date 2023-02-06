#!/usr/bin/env python
# -*- coding: utf-8 -*

import os
import sys
import json
import argparse

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Prepate data for "afisha_json/1.x" snippets')
    parser.add_argument('--cluster', type=str, help='YT cluster')
    parser.add_argument('--parameters',
                        type=str,
                        help='Dict with job parameters')
    args = parser.parse_args()
    yt_client = misc.get_client(os.environ['YT_TOKEN'],
                                os.environ.get('YT_POOL') or '',
                                args.cluster)
    params = json.loads(args.parameters)
    yt_client.copy(source_path=params.get('input_table'),
                   destination_path=params.get('pre_processing_out'),
                   force=True)
    yt_client.run_sort(params.get('pre_processing_out'), sort_by=['place.id'])
