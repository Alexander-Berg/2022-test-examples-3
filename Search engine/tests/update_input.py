#!/usr/bin/env python
# -*- encoding: utf-8 -*-

import argparse
from random import shuffle
import yt.wrapper as yt
import json


def add_arguments(parser):
    parser.add_argument(
        '--input',
        default='//home/geosearch/social_networks/profiles/full/vk.com',
        help='Path to the source YT-table',
    )
    parser.add_argument(
        '--output',
        default='test.data',
        help='Path to the destination file',
    )
    parser.add_argument(
        '--proxy',
        default='hahn',
        help='YT proxy',
    )
    parser.add_argument(
        '--token',
        help='YT token',
    )
    parser.add_argument(
        '--size',
        type=int,
        default=1000,
        help='Size in lines of result file',
    )


def optimize_row(row):
    l = len(row['profile_json']['posts'])
    row['profile_json']['posts'] = row['profile_json']['posts'][:min(l, 3)]
    return row


parser = argparse.ArgumentParser(description='Make test input file with specified number of rows')
add_arguments(parser)
args = parser.parse_args()

yt.config['proxy']['url'] = args.proxy
if args.token:
    yt.config['token'] = args.token

max_lines = args.size
rows = []

row_count = yt.row_count(args.input)
cur_rows = 0
cur_pers = 0
for row in yt.read_table(args.input, format='<encode_utf8=%false>json'):
    if len(row['profile_json']['posts']) > 0:
        rows.append(optimize_row(row))
    cur_rows += 1
    if float(cur_rows) / float(row_count) >= float(cur_pers) / 100.0:
        print str(cur_pers) + '% processed (' + str(cur_rows) + ')'
        cur_pers += 1

shuffle(rows)

with open(args.output, "w") as fout:
    for i in range(max_lines):
        fout.write(json.dumps(rows[i], ensure_ascii=False).encode("utf-8") + '\n')
