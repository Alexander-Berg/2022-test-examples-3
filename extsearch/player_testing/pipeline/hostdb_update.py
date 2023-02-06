#!/usr/bin/env python
# coding: utf-8
import json
import argparse
import logging
import host_status
import yt.wrapper as yt
from config import Config
from host_status import HostStatus
from util import process_samples
from time import time

class HostDBUpdater(object):

    @staticmethod
    def _calc_no_popup(metrics, config):
        return float(metrics.get('popup_rate', 0.0)) <= config.allowed_popup_rate

    def __init__(self, host_item, config, param):
        # misc
        self.db = param.video_db if host_item['Status'] == HostStatus.SEARCHABLE_VIDEO else param.html_db
        self.force = param.force or host_item['ForceUpdate']
        self.is_video = self.db == param.video_db
        self.has_prev = False
        self.can_update = True
        # DB columns
        self.host = host_item['Host']
        self.platform = config.platform
        self.player_source = host_item['PlayerSource']
        self.player_type = host_item['PlayerType']
        self.result = host_item['Status'] in [HostStatus.SEARCHABLE_VIDEO, HostStatus.SEARCHABLE_HTML]
        self.last_results = [self.result, self.result]
        self.status = 'SEARCHABLE' if self.result else host_item['Status']
        self.timestamp = int(time())
        self.created_timestamp = self.timestamp
        self.info = host_item.get('Metrics', {})
        self.no_popups = HostDBUpdater._calc_no_popup(self.info, config)
        self.sample = host_item.get('Sample', [])[:config.saved_sample_size]
        self.requester = host_item.get('Label')
        self.errors = []
        self._fetch_prev()

    def _fetch_prev(self):
        platform = '' if self.is_video else 'AND Platform="{}"'.format(self.platform)
        query = '* FROM [{}] WHERE Host="{}" {} AND PlayerSource="{}" AND PlayerType="{}"'.format(
            self.db, self.host, platform, self.player_source, self.player_type
        )
        for row in yt.select_rows(query):
            if row['Result'] and not self.result and not self.force:
                self.can_update = False
            self.created_timestamp = row['CreatedTimestamp']
            self.last_results[0] = row['Result']
            self.has_prev = True

    def _get_key(self):
        key = {
            'Host': self.host,
            'PlayerSource': self.player_source,
            'PlayerType': self.player_type
        }
        if not self.is_video:
            key['Platform'] = self.platform
        return key

    def _get_dict(self):
        data = self._get_key()
        data.update({
            'Result': self.result,
            'Status': self.status,
            'Timestamp': self.timestamp,
            'CreatedTimestamp': self.created_timestamp,
            'Info': self.info,
            'Sample': self.sample,
            'Errors': self.errors
        })
        if not self.is_video:
            data.update({
            'LastResults': self.last_results,
            'NoPopups': self.no_popups,
            'Requester': self.requester,
            })
        return data

    def update(self, dry_run):
        if not self.can_update:
            logging.info('Item {} update skipped'.format(json.dumps(self._get_key())))
            return
        if self.has_prev:
            logging.info('Deleting {} from [{}]'.format(self._get_key(), self.db))
            yt.retries.run_with_retries(lambda: yt.delete_rows(self.db, [self._get_key()]))
        logging.info('Inserting {} into {}'.format(self._get_dict(), self.db))
        if not dry_run:
            yt.retries.run_with_retries(lambda: yt.insert_rows(self.db, [self._get_dict()]))


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    config = Config()
    ap = argparse.ArgumentParser()
    ap.add_argument('--input', required=True, type=argparse.FileType('r'))
    ap.add_argument('--html-db', required=True)
    ap.add_argument('--video-db', required=True)
    ap.add_argument('--force', action='store_true')
    ap.add_argument('--dry-run', action='store_true')
    args = ap.parse_args()
    hosts = json.load(args.input)
    for host in hosts:
        for host_item in host.get('Samples', []):
            for key in ['Host', 'Label', 'ForceUpdate']:
                host_item[key] = host[key]
            updater = HostDBUpdater(host_item, config, args)
            updater.update(args.dry_run)
