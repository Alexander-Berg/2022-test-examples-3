#!/usr/bin/env python
import json
import argparse
import logging
import host_status
import yt.wrapper as yt
from urlparse import urlparse
from config import Config
from host_status import HostStatus
from collections import defaultdict


def fetch_sample(config, candidates_tab, host):
    item = []
    for source in config.source_types:
        sample = defaultdict(list)
        complete = set()
        row_cnt = 0
        for row in yt.read_table(yt.TablePath(candidates_tab, exact_key=[host, source])):
            row_cnt += 1
            player_type = row['playerCandidateType']
            if player_type not in config.player_types:
                continue
            if len(sample[player_type]) < config.sample_size:
                sample[player_type].append(row['playerCandidateUrl'])
            else:
                complete.add(player_type)
            if len(complete) == len(config.player_types) or row_cnt > config.max_input_rows:
                break
        if sample:
            for player_type, links in sample.iteritems():
                item.append({
                    'PlayerSource': source,
                    'PlayerType': player_type,
                    'Sample': links
                })
    return item


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    config = Config()
    ap = argparse.ArgumentParser()
    ap.add_argument('--candidates-tab', required=True)
    ap.add_argument('--hosts', required=True, type=argparse.FileType('r'))
    ap.add_argument('--output', required=True, type=argparse.FileType('w'))
    args = ap.parse_args()
    hosts = json.load(args.hosts)
    i = 0
    for item in hosts:
        logging.info('Sampling host #{}: {}'.format(i, item['Host']))
        dst='Samples'
        item[dst] = fetch_sample(config, args.candidates_tab, 'https://{}'.format(item['Host']))
        if not item[dst]:
            item[dst] = fetch_sample(config, args.candidates_tab, 'http://{}'.format(item['Host']))
            if not item[dst]:
                item['Status'] = HostStatus.EMPTY_SAMPLE
        i += 1
    json.dump(hosts, args.output, indent=1, sort_keys=True)
