#!/usr/bin/python3

import argparse
import logging
import json
from urllib.parse import urlparse
import yt.wrapper as yt
import time


def get_host(url):
    return urlparse('http://' + url).netloc


def compare(snail_results, mapping):
    urls_with_diff = []
    hosts_to_check = set()

    for item in snail_results:
        if item['status'] != 'OK':
            continue
        if not 'players' in item or len(item['players']) == 0:
            continue
        for player_id in item['players']:
            if player_id not in mapping[item['url']]:
                hosts_to_check.add(get_host(item['url']))
                urls_with_diff.append({'Url': item['url'], 'PlayerId': player_id})

    return urls_with_diff, hosts_to_check


def send_to_checker(faulty_hosts, incoming_directory_path):
    table = incoming_directory_path + '/clicks-crawling.' + str(int(time.time()))
    yt.write_table(table, [{'Host': host} for host in faulty_hosts])
    logging.info('Stored faulty hosts to {}'.format(table))


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)

    ap = argparse.ArgumentParser()
    ap.add_argument('--snail-results', required=True)
    ap.add_argument('--url-player-id-mapping', required=True)
    ap.add_argument('--check-incoming-directory', required=True)
    ap.add_argument('--output', required=True)
    args = ap.parse_args()

    snail_results = json.load(open(args.snail_results, 'r'))
    mapping = json.load(open(args.url_player_id_mapping, 'r'))

    faulty_urls, faulty_hosts = compare(snail_results, mapping)

    json.dump(faulty_urls, open(args.output, 'w'), indent=True)
    send_to_checker(faulty_hosts, args.check_incoming_directory)

    logging.info('Found {} URLs with unexpected players'.format(len(faulty_urls)))
    logging.info('{} hosts sent to checking'.format(len(faulty_hosts)))
