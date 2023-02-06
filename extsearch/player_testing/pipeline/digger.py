#!/usr/bin/env python
import re
import argparse
import yt.wrapper as yt
from time import time
from random import random
from collections import defaultdict
from difflib import SequenceMatcher
from config import Config
from sys import exit
from util import get_host
import logging


def is_good_sample(config, vec):
    if len(set(vec)) < 0.75*config.sample_size:
        return False
    hosts = set()
    for item in vec:
        if item.startswith('/'):
            return False
        if not re.match('^[\-a-zA-Z0-9\/;\&=.?]+$', item):
            return False
        host = '.'.join(item.split('/')[0].split('.')[-2:])
        hosts.add(host)
    if len(hosts) > 1:
        return False
    host = list(hosts)[0]
    if host in config.known_hosts:
        return False
    for item in config.filtered_hosts:
        if host.find(item) != -1:
            return False
    return True


def calc_rank(config, host, row_cnt, sample, updated):
    page_host = host.split('/')[2]
    embed_host = sample[0].split('/')[0]
    matched = SequenceMatcher(a=page_host, b=embed_host).find_longest_match(0, len(page_host), 0, len(embed_host))
    f1 = float(matched.size) / min(len(page_host), len(embed_host))
    f2 = float(row_cnt) / config.max_input_rows
    f3 = (1.0 - (time() - updated) / (config.digging_days * 86400))
    return f1 + f2 + f3


def digger(config):
    def do_reduce(key, rows):
        if key['Source'] not in config.source_types:
            return
        row_cnt = 0
        sample = defaultdict(list)
        updated = 0
        for row in rows:
            row_cnt += 1
            if row_cnt >= config.max_input_rows:
                break
            ptype = row['playerCandidateType']
            if ptype not in config.player_types:
                continue
            updated = max(updated, row['updateTime'])
            sample[ptype].append(row['playerCandidateUrl'])
            if len(sample[ptype]) >= config.sample_size:
                break
        if updated < int(time()) - 86400 * config.digging_days:
            return
        for vec in sample.itervalues():
            if is_good_sample(config, vec):
                host = key['Host']
                rank = -1.0 * random() * calc_rank(config, host, row_cnt, vec, updated)
                yield {'Host': host, 'Sample': vec[:5], 'updateTime': updated, 'rank': rank}
                return
    return do_reduce


def empty_queue(incoming_dir):
    for table in yt.list(incoming_dir):
        if yt.get('{}/@row_count'.format(yt.ypath_join(incoming_dir, table))):
            return False
    return True


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    yt.initialize_python_job_processing()
    yt.config['pickling']['module_filter'] = lambda module: 'hashlib' not in getattr(module, '__name__', '') and 'hmac' != getattr(module, '__name__', '')
    ap = argparse.ArgumentParser()
    ap.add_argument('--input', default='//home/videoindex/players/candidate')
    ap.add_argument('--output', default='//home/videoindex/players/candidate.hosts')
    ap.add_argument('--force-dig', action='store_true', default=False)
    ap.add_argument('--dig-if-empty-queue', action='store_true', default=False)
    ap.add_argument('--incoming', required=True)
    args = ap.parse_args()
    if (not args.force_dig and not empty_queue(args.incoming)) or not args.dig_if_empty_queue:
        logging.info('nothing to do, exiting')
        exit(0)
    cfg = Config()
    with yt.TempTable() as tmp_tab:
        yt.run_reduce(digger(cfg), args.input, tmp_tab, reduce_by=['Host','Source'])
        yt.run_sort(tmp_tab, args.output, sort_by='rank')
        data = []
        for row in yt.read_table(yt.TablePath(args.output, end_index=cfg.digging_size)):
            logging.info('host {}'.format(row['Host']))
            data.append({'Host': get_host(row['Host'])})
        yt.write_table(yt.ypath_join(args.incoming, 'digger.{}'.format(int(time()))), data)
