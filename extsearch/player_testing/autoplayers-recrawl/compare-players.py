#!/usr/bin/python

import argparse
import logging
import json
from urlparse import urlparse


def compare(snail_results, mapping):
    urls_with_diff = []

    for item in snail_results:
        if item['status'] != 'OK':
            continue

        if not 'players' in item or len(item['players']) == 0:
            continue

        for player_id in item['players']:
            if player_id not in mapping[item['url']]:
                urls_with_diff.append(
                    {'Url': item['url'], 'PlayerId': player_id})

    return urls_with_diff


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)

    ap = argparse.ArgumentParser()
    ap.add_argument('--snail-results', required=True)
    ap.add_argument('--url-player-id-mapping', required=True)
    ap.add_argument('--output', required=True)
    args = ap.parse_args()

    snail_results = json.load(open(args.snail_results, 'r'))
    mapping = json.load(open(args.url_player_id_mapping, 'r'))

    json.dump(compare(snail_results, mapping), open(args.output, 'w'), indent=True)
