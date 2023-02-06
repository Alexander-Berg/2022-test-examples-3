# coding: utf-8

import os
import yatest.common

from yt.wrapper import ypath_join

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.yt_token_resource import YtTokenResource
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.generation.yatf.utils.fixtures import glue_config

from .snippet_diff_builder import create_genlog_table


def _STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'generation', 'yatf',
        'resources',
        'stubs',
    )


class MskuReducerMode(object):
    COLLECT_OFFERS_DATA_FOR_MSKU = "collect_offers_data_for_msku"
    PROPAGATE_MSKU_DATA_TO_OFFERS = "propagate_msku_data_to_offers"


class MskuReducerTestEnv(BaseEnv):

    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, yt_stuff, rows_feed, **resources):

        self._STUBS.update({
            name: FileResource(os.path.join(_STUBS_DIR(), filename))
            for name, filename in {
                'geobase_gz': 'geobase_full.gz'
            }.items()
        })
        self._STUBS.update({'glue_config': glue_config()})
        super(MskuReducerTestEnv, self).__init__(**resources)

        self.yt_stuff = yt_stuff
        self.yt_client = yt_stuff.get_yt_client()

        yt_prefix = get_yt_prefix()

        SHARD_TABLE = '0000'

        self.genlog_input_dir = ypath_join(yt_prefix, 'genlog')
        self.yt_input_table_path = create_genlog_table(yt_stuff, rows_feed, ypath_join(self.genlog_input_dir, SHARD_TABLE))

        self.dst_genlog_dir = ypath_join(yt_prefix, 'result')
        self.dst_genlog_table_path = ypath_join(self.dst_genlog_dir, SHARD_TABLE)

        self.parts_count = 1

        resources_stubs = {
            'yt_token': YtTokenResource(),
        }
        for name, val in resources_stubs.items():
            if name not in self.resources:
                self.resources[name] = val

    @property
    def description(self):
        return 'msku_reducer'

    def execute(self, mode, path=None, sane_regions_size=None, reduce_regions_for_fake_msku=False):
        SHARD_TABLE = '0000'

        if path is None:
            relative_path = os.path.join(
                'market', 'idx', 'generation', 'msku_reducer', 'msku_reducer'
            )
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        cmd = [
            path, mode,
            '--yt-proxy', self.yt_stuff.get_server(),
            '--yt-token-path', self.resources['yt_token'].path,

            '--common-yt-src-genlog-dir', self.genlog_input_dir,
            '--common-yt-dst-genlog-dir', self.dst_genlog_dir,
            '--common-parts-count', str(self.parts_count),
        ]

        if mode == MskuReducerMode.COLLECT_OFFERS_DATA_FOR_MSKU:
            self.dst_genlog_table_path = ypath_join(self.dst_genlog_dir, SHARD_TABLE + '_0')
            cmd.append('--reduce-regions-instead-set-earth')
            cmd.append('--add-more-literals')
            if self.resources['geobase_gz'].path:
                cmd.extend([
                    '--enable-regions',
                    '--geobase-gz-path', self.resources['geobase_gz'].path,
                    '--earth-for-msku-without-offer-regions',
                ])
            if sane_regions_size:
                cmd.extend(['--msku-sane-regions-size', str(sane_regions_size)])
            cmd.extend(['--glue-config-path', self.resources['glue_config'].path])
            if reduce_regions_for_fake_msku:
                cmd.append('--reduce-regions-for-fake-msku')
            cmd.append('--set-has-adv-bid-literal')

        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=True
        )

    def _read_rows_from(self, table):
        return [row for row in self.yt_client.read_table(table)]

    @property
    def result_genlog_table(self):
        return list(self.yt_client.read_table(self.dst_genlog_table_path))

    @property
    def result_genlog_data(self):
        result_data = {}
        for row in self._read_rows_from(self.dst_genlog_table_path):
            result_data[row['ware_md5']] = row
        return result_data
