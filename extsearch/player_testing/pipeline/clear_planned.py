#!/usr/bin/env python
import json
import argparse
import logging
import yt.wrapper as yt
from util import get_host
from config import Config
from time import time


def cleaner(blacklist):
    def mapper(row):
        try:
            if get_host(row['Host']) not in blacklist:
                yield row
        except Exception as e:
            pass
    return mapper


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    yt.initialize_python_job_processing()
    yt.config['pickling']['module_filter'] = lambda module: 'hashlib' not in getattr(module, '__name__', '') and 'hmac' != getattr(module, '__name__', '')
    config = Config()
    ap = argparse.ArgumentParser()
    ap.add_argument('--hosts', required=True, type=argparse.FileType('r'))
    ap.add_argument('--incoming', required=True)
    args = ap.parse_args()
    blacklist = set()
    for host in json.load(args.hosts):
        blacklist.add(host['Host'])
    with yt.Transaction():
        incoming = map(lambda t: yt.ypath_join(args.incoming, t), list(yt.list(args.incoming)))
        yt.run_map(cleaner(blacklist), incoming, yt.ypath_join(args.incoming, 'hosts.{}'.format(int(time()))))
        for tab in incoming:
            yt.remove(tab)
