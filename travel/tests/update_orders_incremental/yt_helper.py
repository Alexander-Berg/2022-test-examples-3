# -*- coding: utf-8 -*-

import logging
from typing import Dict, Iterable

import yaml
import yatest.common as common
import yt.wrapper as yt

from travel.cpa.data_processing.lib.order_data_model import CATEGORY_CONFIGS
from travel.cpa.lib.lib_yt import get_table_schema

from data import TableData


class YtHelper(object):
    yt_root = '//home/travel/tests/cpa/flow'

    def __init__(self, yt_client, yt_proxy):
        logging.info('Creating yt helper')
        self.yt_client = yt_client
        self.yt_proxy = yt_proxy
        tables_config_path = common.source_path('travel/cpa/tools3/tables_config.yaml')
        with open(tables_config_path) as f:
            tables_config = yaml.load(f.read(), yaml.SafeLoader)
        self.tables_config = {t['path']: t for t in tables_config['tables']}
        self.order_table_config = tables_config['order_tables']

    def create_tables(self):
        logging.info('Creating tables from config')
        for table_config in self.tables_config.values():
            self._create_table(self.yt_root, table_config, table_config['schema'])
        for category, category_config in CATEGORY_CONFIGS.items():
            order_cls = category_config.order_with_decoded_label_cls or category_config.order_with_encoded_label_cls
            schema = order_cls().get_yt_schema()
            schema = get_table_schema(schema, sort_by=['partner_name', 'partner_order_id'])
            category_path = yt.ypath_join(self.yt_root, category)
            self._create_table(category_path, self.order_table_config, schema)

    def write_tables(self, tables: Dict[str, TableData]):
        for table_name, table_data in tables.items():
            table_path = yt.ypath_join(self.yt_root, table_name)
            logging.info(f'Writing to {table_name}')
            self.yt_client.insert_rows(table_path, table_data)

    def read_tables(self, tables: Iterable[str]):
        tables_data = dict()
        for table_name in tables:
            tables_data[table_name] = self._read_table(table_name)
        return tables_data

    def _create_table(self, root, table_config, schema):
        table_path = yt.ypath_join(root, table_config['path'])
        logging.info('Processing %s', table_path)

        attributes = table_config['attributes'].copy()
        attributes.pop('min_data_ttl', None)
        attributes.pop('max_data_ttl', None)
        attributes['schema'] = schema

        if self.yt_client.exists(table_path):
            logging.info('Table already exists')
            return

        logging.info('Creating table')
        self.yt_client.create('table', table_path, recursive=True, attributes=attributes)
        logging.info('Mounting table')
        self.yt_client.mount_table(table_path, sync=True)

    def _read_table(self, table_name):
        table_path = yt.ypath_join(self.yt_root, table_name)
        if self.yt_client.get(f'{table_path}/@dynamic'):
            return list(self.yt_client.select_rows(f'* from [{table_path}]'))
        else:
            return list(self.yt_client.read_table(table_path))
