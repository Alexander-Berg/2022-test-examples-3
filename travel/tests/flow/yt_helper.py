# -*- coding: utf-8 -*-

import logging

import yaml

import yatest.common as common
import yt.wrapper as yt


class YtHelper(object):
    yt_root = '//home/travel/tests/cpa/flow'

    def __init__(self, yt_client, yt_proxy):
        self.yt_client = yt_client
        self.yt_proxy = yt_proxy
        tables_config_path = common.source_path('travel/cpa/tools3/tables_config.yaml')
        with open(tables_config_path) as f:
            tables_config = yaml.load(f.read(), yaml.SafeLoader)['tables']
        self.tables_config = {t['path']: t for t in tables_config}

    def create_tables(self):
        for table_config in self.tables_config.values():
            self._create_table(table_config)

    def write_snapshots(self, snapshots, table_name):
        table_path = yt.ypath_join(self.yt_root, table_name)
        table_fields = self._get_table_fields(table_name)
        data = [self._get_snapshot_data(s, table_fields) for s in snapshots]
        logging.info('Writing to %s: %s', table_name, data)
        self.yt_client.insert_rows(table_path, data)

    def write_purgatory_items(self, purgatory_items, table_name):
        table_path = yt.ypath_join(self.yt_root, table_name)
        logging.info('Writing to %s: %s', table_name, purgatory_items)
        self.yt_client.insert_rows(table_path, purgatory_items)

    def read_tables(self):
        tables_data = dict()
        for table_name in self.tables_config.keys():
            tables_data[table_name] = self._read_table(table_name)
        return tables_data

    def _create_table(self, table_config):
        table_path = yt.ypath_join(self.yt_root, table_config['path'])
        logging.info('Processing %s', table_path)

        attributes = table_config['attributes'].copy()
        attributes.pop('min_data_ttl', None)
        attributes.pop('max_data_ttl', None)
        schema = table_config['schema']
        attributes['schema'] = schema

        if self.yt_client.exists(table_path):
            logging.info('Table already exists')
            return

        logging.info('Creating table')
        self.yt_client.create('table', table_path, recursive=True, attributes=attributes)
        logging.info('Mounting table')
        self.yt_client.mount_table(table_path, sync=True)

    def _get_table_fields(self, table_name):
        table_config = self.tables_config[table_name]
        return [row['name'] for row in table_config['schema']]

    @staticmethod
    def _get_snapshot_data(snapshot, table_fields):
        snapshot_data = dict()
        snapshot_dict = snapshot.as_dict(add_data=True)
        for field in table_fields:
            snapshot_data[field] = snapshot_dict[field]
        return snapshot_data

    def _read_table(self, table_name):
        table_fields = ', '.join(self._get_table_fields(table_name))
        table_path = yt.ypath_join(self.yt_root, table_name)
        return list(self.yt_client.select_rows('{} from [{}]'.format(table_fields, table_path)))
