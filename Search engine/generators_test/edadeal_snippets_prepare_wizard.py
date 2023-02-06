#!/usr/bin/env python
#  -*- coding: utf-8 -*-

import os
import sys
import json
import argparse
import requests
import datetime
import yt.wrapper as yt

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc


def get_json_data(params):
    raw_data = misc.download(params.get('input_table')[1])
    return json.loads(raw_data)


def create_offers_table(params, json_data, client):
    offers = []
    for offer in json_data['offers']:
        dateStart = datetime.datetime.strptime(offer['dateStart'],
                                               '%Y-%m-%dT%H:%M:%SZ')
        dateEnd = datetime.datetime.strptime(offer['dateEnd'],
                                             '%Y-%m-%dT%H:%M:%SZ')
        if dateStart < datetime.datetime.now() < dateEnd:
            offers.append(offer)
    client.write_table(params['temp_tables'].get('offer'),
                       offers,
                       format=yt.JsonFormat(attributes={'encode_utf8': False}))


def create_shop_wizard_table(params, json_data, client):
    shops = []
    for shop in json_data['shops']:
        shops.append({
            'shop_edadeal_id': shop['edadealId'],
            'chain_id': shop['chainId'],
            'shop': shop,
            'catalog_cover_urls': shop['catalogCoverURLs']})
    client.write_table(params['temp_tables'].get('shop_wizard'),
                       shops,
                       format=yt.JsonFormat(attributes={'encode_utf8': False}))


def create_wizard_retailer_table(params, json_data, client):
    retailers = []
    for retailer in json_data['retailers']:
        retailer['chain_id'] = retailer['chainId']
        retailer['edadeal_retailer_id'] = retailer['id']
        retailers.append(retailer)
    client.write_table(params['temp_tables'].get('wizard_retailer'),
                       retailers,
                       format=yt.JsonFormat(attributes={'encode_utf8': False}))


def create_wizard_segment_table(params, json_data, client):
    segments = []
    for segment in json_data['segments']:
        segments.append({
            'segment_id': segment['id'],
            'parent_segment_id': segment.get('parentId'),
            'segment': segment})

    client.write_table(params['temp_tables'].get('wizard_segment'),
                       segments,
                       format=yt.JsonFormat(attributes={'encode_utf8': False}))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Prepare Edadeal snippets data')
    parser.add_argument('--cluster',type=str, help='YT cluster')
    parser.add_argument('--parameters', type=str, help='Dict with job parameters')
    args = parser.parse_args()
    yt_client = misc.get_client(os.environ['YT_TOKEN'], args.cluster)
    params = json.loads(args.parameters)
    data = get_json_data(params)
    create_offers_table(params, data, yt_client)
    create_shop_wizard_table(params, data, yt_client)
    create_wizard_retailer_table(params, data, yt_client)
    create_wizard_segment_table(params, data, yt_client)
