#!/usr/bin/env python
import json
import argparse
import logging
import yt.wrapper as yt
from util import get_host
from config import Config


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    config = Config()
    ap = argparse.ArgumentParser()
    ap.add_argument('--incoming', required=True)
    ap.add_argument('--output', required=True, type=argparse.FileType('w'))
    args = ap.parse_args()
    hosts = {}
    for tab in yt.list(args.incoming):
        for row in yt.read_table(yt.ypath_join(args.incoming, tab)):
            try:
                host = get_host(row['Host'])
                hosts[host] = {
                    'Host': host,
                    'Priority': float(max(row.get('Priority'), 0)),
                    'Label': row.get('Requester'),
                    'ForceUpdate': bool(row.get('ForceUpdate'))
                }
            except Exception as e:
                logging.error(e)
                continue
    hosts = [item for item in hosts.itervalues()]
    hosts.sort(key=lambda item: -item['Priority'])
    json.dump(hosts[:config.host_limit], args.output, indent=1, sort_keys=True)
