#!/usr/bin/env python
#  -*- coding: utf-8 -*-

import os
import sys
import json
import argparse
import yt.wrapper as yt

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc


def create_shop_sprav_table(params, client):
    client.create('table',
                  params['temp_tables'].get('shop_sprav'),
                  attributes={'schema': [
                              {'name': 'address_en', 'type': 'string'},
                              {'name': 'address_ru', 'type': 'string'},
                              {'name': 'country_code', 'type': 'string'},
                              {'name': 'edadeal_id', 'type': 'string'},
                              {'name': 'edadeal_retailer_id', 'type': 'string'},
                              {'name': 'edadeal_retailer_website', 'type': 'string'},
                              {'name': 'is_active', 'type': 'boolean'},
                              {'name': 'lat', 'type': 'double'},
                              {'name': 'lng', 'type': 'double'},
                              {'name': 'name', 'type': 'string'},
                              {'name': 'short_address_en', 'type': 'string'},
                              {'name': 'short_address_ru', 'type': 'string'},
                              {'name': 'state', 'type': 'string'},
                              {'name': 'yandex_chain_id', 'type': 'int64'},
                              {'name': 'yandex_geoid', 'type': 'int64'},
                              {'name': 'yandex_oid', 'type': 'int64'},
                              {'name': 'yandex_rubric_id', 'type': 'int64'}
                             ]},
                  ignore_existing=True)
    raw_data = misc.download(params.get('input_table')[0])
    data = json.loads(raw_data)
    shops = []
    for row in data:
        row['lat'] = row['lat'] * 1.0
        row['lng'] = row['lng'] * 1.0
        shops.append(row)
    client.write_table(params['temp_tables'].get('shop_sprav'),
                       shops,
                       format=yt.JsonFormat(attributes={'encode_utf8': False}))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Prepare Edadeal snippets data')
    parser.add_argument('--cluster',type=str, help='YT cluster')
    parser.add_argument('--parameters', type=str, help='Dict with job parameters')
    args = parser.parse_args()
    yt_client = misc.get_client(os.environ['YT_TOKEN'], args.cluster)
    params = json.loads(args.parameters)
    create_shop_sprav_table(params, yt_client)
