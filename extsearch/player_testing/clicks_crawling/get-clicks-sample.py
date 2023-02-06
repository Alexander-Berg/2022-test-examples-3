#!/usr/bin/python

import argparse
from collections import defaultdict
import json
import yt.wrapper as yt
import logging
from datetime import datetime, timedelta


# for less sampling_rate there are significant performance issues
# 0.0005 ratio is currently about 40'000 rows
MIN_SAMPLING_RATE = 0.0005


def get_recent_table(clicks_directory):
    days_ago = 1
    while not yt.exists(
        yt.ypath_join(clicks_directory, datetime.strftime(datetime.now() - timedelta(days_ago), '%Y%m%d'))
    ):
        days_ago += 1

    return yt.ypath_join(clicks_directory, datetime.strftime(datetime.now() - timedelta(days_ago), '%Y%m%d'))


def read_urls_sample(table_path, sample_max_size):
    row_count = int(yt.row_count(table_path))
    logging.info('Table {} has {} rows'.format(table_path, row_count))

    if row_count == 0:
        logging.info('Table {} is empty'.format(table_path))
        return []

    sampling_rate = 1.0

    if sample_max_size / row_count < 1 - 1e-3:
        sampling_rate = sample_max_size / row_count

    if sampling_rate < MIN_SAMPLING_RATE:
        sampling_rate = MIN_SAMPLING_RATE
        logging.info('Sampling rate is too low, setting to {}'.format(MIN_SAMPLING_RATE))
    else:
        logging.info('Sampling rate is set to {}'.format(sampling_rate))

    url_sample = defaultdict()

    for row in yt.read_table(table_path, table_reader={'sampling_rate': sampling_rate}):
        url_sample[row['Url']] = row['PlayerId']
        if len(url_sample) == sample_max_size:
            break

    logging.info('Read {} rows'.format(len(url_sample)))

    return url_sample


def write_tsv(file_path, url_sample):
    with open(file_path, 'w') as f:
        for url in url_sample:
            f.write(url + '\n')


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)

    ap = argparse.ArgumentParser()
    ap.add_argument('--clicks-directory', required=True)
    ap.add_argument('--sample-max-size', required=True)
    ap.add_argument('--url-sample', required=True)
    ap.add_argument('--url-player-id-mapping', required=True)
    args = ap.parse_args()

    url_sample = read_urls_sample(get_recent_table(args.clicks_directory), int(args.sample_max_size))

    write_tsv(args.url_sample, url_sample)
    json.dump(url_sample, open(args.url_player_id_mapping, 'w'), indent=1)
