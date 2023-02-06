#!/usr/bin/env python
# coding: utf-8
import json
import argparse
import logging
import host_status
from config import Config
from host_status import HostStatus
import codecs

if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    config = Config()
    ap = argparse.ArgumentParser()
    ap.add_argument('--input', required=True, type=argparse.FileType('r'))
    ap.add_argument('--count', type=int, default=9999)
    ap.add_argument('--output', required=True)
    args = ap.parse_args()
    hosts = json.load(args.input)
    output=set()
    for hostrec in hosts:
        if 'Status' in hostrec or 'Samples' not in hostrec:
            continue
        for item in hostrec['Samples']:
            if 'Status' in item or 'Sample' not in item:
                continue
            for url in item['Sample'][:args.count]:
                output.add(url)
    with codecs.open(args.output, 'w', encoding='utf-8') as outfd:
        outfd.write('\n'.join(list(output)))
