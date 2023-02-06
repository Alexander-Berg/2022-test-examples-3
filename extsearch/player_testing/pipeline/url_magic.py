#!/usr/bin/env python
import json
import argparse
import logging
import host_status
from urlparse import urlparse
from config import Config
from host_status import HostStatus
from util import process_samples


def calc_metrics(sample, known_hosts):
    hosts = {}
    known = 0
    major_host_size = 0
    for url in sample:
        host = url.split('/')[0]
        if host in known_hosts:
            known += 1
        domain = '.'.join(host.split('.')[-2:])
        hosts[domain] = hosts.get(domain, 0) + 1
        major_host_size = max(major_host_size, hosts[domain])
    return {
        'major_host_rate': float(major_host_size) / len(sample) if len(sample) > 0 else 0.0,
        'known_host_rate': float(known) / len(sample) if len(sample) > 0 else 0.0,
        'dup_rate': 1.0 - float(len(set(sample))) / len(sample) if len(sample) > 0 else 0.0
    }


def url_magic(host_item, config):
    sample = host_item['Sample']
    metrics = calc_metrics(host_item['Sample'], config.known_hosts)
    if len(sample) < config.sample_size:
        host_item['Status'] = HostStatus.SMALL_SAMPLE
        return
    is_dirty = metrics['dup_rate'] > config.max_player_dup_rate
    is_dirty = is_dirty or metrics['known_host_rate'] > config.max_known_host_rate
    is_dirty = is_dirty or metrics['major_host_rate'] < config.min_major_host_rate
    if is_dirty:
        host_item['Status'] = HostStatus.DIRTY_SAMPLE
    metrics.update(host_item.get('Metrics', {}))
    host_item['Metrics'] = metrics


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    config = Config()
    ap = argparse.ArgumentParser()
    ap.add_argument('--input', required=True, type=argparse.FileType('r'))
    ap.add_argument('--output', required=True, type=argparse.FileType('w'))
    args = ap.parse_args()
    hosts = json.load(args.input)
    process_samples(hosts, url_magic, config=config)
    json.dump(hosts, args.output, indent=1, sort_keys=True)
