#!/usr/bin/env python
# coding: utf-8
import json
import argparse
import logging
import host_status
from config import Config
from host_status import HostStatus
from eval_results import load_url_status,eval_results
from util import process_samples
import codecs


def probe_host(host_item, config, url_status):
    host_probe_item = host_item.copy()
    if 'Sample' in host_probe_item:
        host_probe_item['Sample'] = host_probe_item['Sample'][:config.probing_size]
    eval_results(host_probe_item, config, url_status)
    metrics = host_probe_item.get('Metrics', dict())
    status = host_probe_item['Status']
    probing_failed = status in [HostStatus.MIXED_MIME, HostStatus.HTTP_NOT_FOUND]
    probing_failed = probing_failed or (status == HostStatus.VDP and metrics.get('vdp_rate', 0.0) == 1.0)
    probing_failed = probing_failed or (status == HostStatus.POPUP and metrics.get('popup_rate', 0.0) == 1.0)
    if probing_failed:
        host_item['Status'] = HostStatus.BASIC_CHECK_FAILED
        host_item['Metrics'] = metrics
        metrics['probing_failed'] = True
        metrics['probing_status'] = status


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    config = Config()
    ap = argparse.ArgumentParser()
    ap.add_argument('--hosts', required=True, type=argparse.FileType('r'))
    ap.add_argument('--url-status', required=True, type=argparse.FileType('r'))
    ap.add_argument('--output', required=True, type=argparse.FileType('w'))
    args = ap.parse_args()
    url_status = load_url_status(args.url_status)
    hosts = json.load(args.hosts)
    process_samples(hosts, probe_host, config=config, url_status=url_status)
    json.dump(hosts, args.output, indent=1, sort_keys=True)
