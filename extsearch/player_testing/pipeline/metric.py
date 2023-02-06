#!/usr/bin/env python
import yt.wrapper as yt
import argparse
import json
import logging
from datetime import datetime
from time import time


def calc_metric(table, ts):
    query = 'Host,Result FROM [{}] WHERE Timestamp<={}'.format(table, ts)
    success = set()
    failed = set()
    for row in yt.select_rows(query):
        host = row['Host']
        if row['Result']:
            success.add(host)
        else:
            failed.add(host)
    return len(success), len(failed)


def get_points(html_table, video_table, depth):
    now = int(time())
    now -= now % 86400
    points = []
    for i in range(depth)[::-1]:
        ts = now - i * 86400
        html = calc_metric(html_table, ts)
        video = calc_metric(video_table, ts)
        points.append({
            'fielddate': datetime.fromtimestamp(ts).strftime('%Y-%m-%d'),
            'iframe_player': html[0],
            'iframe_rejected': html[1],
            'html5_player': video[0],
            'html5_rejected': video[1]
        })
    return points


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    ap = argparse.ArgumentParser()
    ap.add_argument('--iframe-db', required=True)
    ap.add_argument('--html5-db', required=True)
    ap.add_argument('--depth', default=30, type=int)
    ap.add_argument('--as-dict', default=False, action='store_true')
    ap.add_argument('--output', type=argparse.FileType('w'))
    args = ap.parse_args()
    data = get_points(args.iframe_db, args.html5_db, args.depth)
    json.dump({'values': data} if args.as_dict else data, args.output, indent=1, sort_keys=True)
