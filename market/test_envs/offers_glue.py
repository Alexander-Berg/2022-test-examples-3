# coding: utf-8

import os
import yatest.common

from google.protobuf.json_format import ParseDict

from yt.wrapper.ypath import ypath_join, ypath_dirname

from market.proto.indexer import GenerationLog_pb2 as Record_pb

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv


class OffersGlueTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(
        self,
        yt_stuff,
        output_genlog_yt_path,
        input_glue_yt_paths=None,
        binary_path=None,
        merge_after_reduce=None,
        **resources
    ):
        super(OffersGlueTestEnv, self).__init__(**resources)

        self.yt_stuff = yt_stuff
        self.yt_client = yt_stuff.get_yt_client()

        self.yt_output_dir = output_genlog_yt_path
        self.yt_input_dir = ypath_dirname(resources['genlogs'].get_path())

        self.input_glue_yt_paths = input_glue_yt_paths or list()
        self.merge_after_reduce = merge_after_reduce

    @property
    def description(self):
        return 'offes_glue_app'

    def execute(self, binary_path=None):
        if binary_path is None:
            relative_path = os.path.join(
                'market', 'idx', 'offers', 'bin', 'offers-glue', 'offers-glue'
            )
            absolute_path = yatest.common.binary_path(relative_path)
            binary_path = absolute_path

        cmd = [
            binary_path,
            '--yt-proxy', self.yt_stuff.get_server(),
            '--yt-input-dir', self.yt_input_dir,
            '--yt-output-dir', self.yt_output_dir,
            '--glue-config-path', self.resources['glue_config'].path,
        ]

        for p in self.input_glue_yt_paths:
            cmd.append('--yt-glue-input-table={}'.format(p))

        if self.merge_after_reduce:
            cmd.append('--merge-after-offer-glue-reduce')

        self.exec_result = self.try_execute_under_gdb(cmd)

    @property
    def output_dir(self):
        return self.yt_output_dir

    def _get_all_tables(self, folder):
        tables = list()
        if self.yt_client.exists(folder):
            tables = list(self.yt_client.list(ypath_join(folder)))
        return tables

    @property
    def output_tables_list(self):
        return self._get_all_tables(self.output_dir)

    def _read_offers_from_each_table(self, folder, tables_list):
        data = []
        for table in tables_list:
            table_path = ypath_join(folder, table)
            if self.yt_client.exists(table_path):
                data.append(list(self.yt_client.read_table(table_path)))
        return data

    @property
    def output_tables_data(self):
        return self._read_offers_from_each_table(self.output_dir, self.output_tables_list)

    def _collect_offer_from_row(self, row):
        record = Record_pb.Record()
        ParseDict(row, record, ignore_unknown_fields=True)
        return record

    def _all_offers_from_folder(self, tables_data):
        offers_list = []
        for table in tables_data:
            for row in table:
                offers_list.append(self._collect_offer_from_row(row))
        return offers_list

    @property
    def genlog(self):
        tables = []
        for table in self.output_tables_data:
            if table != 'buybox':
                tables.append(table)
        return self._all_offers_from_folder(tables)
