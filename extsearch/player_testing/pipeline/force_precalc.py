#!/usr/bin/env python
# coding: utf-8
import json
import argparse
import logging
from host_status import HostStatus
import yt.wrapper as yt


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    ap = argparse.ArgumentParser()
    ap.add_argument('--input', required=True, type=argparse.FileType('r'))
    ap.add_argument('--recalc-table', default='//home/videoindex/full/docbase/reprecalc/hosts')
    args = ap.parse_args()
    recalc = set()
    hosts = json.load(args.input)
    for hostrec in hosts:
        for item in hostrec['Samples']:
            if item.get('Status') in [HostStatus.SEARCHABLE_HTML, HostStatus.SEARCHABLE_VIDEO]:
                recalc.add(hostrec['Host'])
    data = [{'Host': host, 'Comment': 'from autoplayer check'} for host in recalc]
    yt.retries.run_with_retries(lambda: yt.insert_rows(args.recalc_table, data))
