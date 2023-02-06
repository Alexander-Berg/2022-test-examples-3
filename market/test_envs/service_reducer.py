# coding: utf-8

import os
import yatest.common

from yt.wrapper import ypath_join

from market.idx.generation.yatf.utils.fixtures import glue_config
from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.yt_token_resource import YtTokenResource
from market.idx.yatf.test_envs.base_env import BaseEnv

from .snippet_diff_builder import create_genlog_table


class ServiceReducerTestEnv(BaseEnv):

    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, yt_stuff, rows_feed, write_service_offers_to_genlog, **resources):
        super(ServiceReducerTestEnv, self).__init__(**resources)

        self.write_service_offers_to_genlog = write_service_offers_to_genlog
        self.yt_stuff = yt_stuff
        self.yt_client = yt_stuff.get_yt_client()

        yt_prefix = get_yt_prefix()

        SHARD_TABLE = '0000'

        self.genlog_input_dir = ypath_join(yt_prefix, 'genlog')
        self.yt_input_table_path = create_genlog_table(yt_stuff, rows_feed, ypath_join(self.genlog_input_dir, SHARD_TABLE))

        self.collapse_genlog_dir = ypath_join(yt_prefix, 'collapse_genlog')
        self.filtered_genlog_dir = ypath_join(yt_prefix, 'filtered_genlog')
        self.relation_output_dir = ypath_join(yt_prefix, 'relations')
        self.genlog_output_dir = ypath_join(yt_prefix, 'business_genlog')

        self.expected_collapse_genlog_table_path = ypath_join(self.collapse_genlog_dir, SHARD_TABLE)
        self.expected_filtered_genlog_table_path = ypath_join(self.filtered_genlog_dir, SHARD_TABLE)
        self.expected_relation_output_table_path = ypath_join(self.relation_output_dir, SHARD_TABLE)
        self.expected_genlog_output_table_path = ypath_join(self.genlog_output_dir, SHARD_TABLE)

        resources_stubs = {
            'yt_token': YtTokenResource(),
            'glue_config': glue_config()
        }
        for name, val in resources_stubs.items():
            if name not in self.resources:
                self.resources[name] = val

    @property
    def description(self):
        return 'service_reducer'

    def execute(self, path=None, generation='20210715_000'):
        currency_rates_path = os.path.join(
            yatest.common.source_path(),
            'market', 'idx', 'yatf', 'resources', 'stubs', 'getter', 'mbi', 'currency_rates.xml'
        )

        if path is None:
            relative_path = os.path.join(
                'market', 'idx', 'generation', 'service_reducer', 'service_reducer'
            )
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        cmd = [
            path,
            '--yt-proxy', self.yt_stuff.get_server(),
            '--yt-token-path', self.resources['yt_token'].path,

            '--yt-genlog-input-dir', self.genlog_input_dir,
            '--yt-collapse-genlog-dir', self.collapse_genlog_dir,
            '--yt-filtered-genlog-dir', self.filtered_genlog_dir,
            '--yt-relation-output-dir', self.relation_output_dir,
            '--yt-genlog-output-dir', self.genlog_output_dir,

            '--currency-rates-path', currency_rates_path,
            '--generation', generation,
            '--msku-filtering-enabled',
            '--glue-config-path', self.resources['glue_config'].path,
        ]

        if self.write_service_offers_to_genlog:
            cmd.append('--write-service-offers-to-genlog')

        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=True
        )

    def _read_rows_from(self, table):
        return [row for row in self.yt_client.read_table(table)]

    @property
    def output_rows(self):
        return self._read_rows_from(self.expected_relation_output_table_path)

    @property
    def collapse_genlog_rows(self):
        return self._read_rows_from(self.expected_collapse_genlog_table_path)

    @property
    def filtered_genlog_rows(self):
        return self._read_rows_from(self.expected_filtered_genlog_table_path)

    @property
    def relation_output_rows(self):
        return self._read_rows_from(self.expected_relation_output_table_path)

    @property
    def genlog_output_rows(self):
        return self._read_rows_from(self.expected_genlog_output_table_path)
