# coding: utf-8

import os
import yatest.common

from yt.wrapper import ypath_join

from market.proto.msku.jump_table_filters_pb2 import JumpTableModelEntry

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix


class JumpTableReducer(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        super(JumpTableReducer, self).__init__(**resources)
        self.yt_test_folder = get_yt_prefix()
        self.reduced_jump_table = ypath_join(self.yt_test_folder, 'reduced_jump_table')

    @property
    def description(self):
        return 'jump-table-reducer'

    def execute(self, yt_stuff):
        relative_bin_path = os.path.join(
            'market',
            'tools',
            'jump_table_dumper',
            'bin',
            'reducer',
            'jump-table-reducer',
        )
        absolute_bin_path = yatest.common.binary_path(relative_bin_path)
        input_jump_table = self.resources['input_jump_table']

        args = [
            absolute_bin_path,
            '--yt-proxy',         yt_stuff.get_server(),
            '--input-jump-table', input_jump_table.table_path,
            '--reduced-jump-table', self.reduced_jump_table
        ]

        self.exec_result = self.try_execute_under_gdb(
            args,
            cwd=self.output_dir,
            check_exit_code=True
        )

        self.outputs.update({
            'reduced-jump-table': YtTableResource(
                yt_stuff,
                self.reduced_jump_table,
                load=True
            ),
        })
        self.prepare_reduced_table_data()

    def prepare_reduced_table_data(self):
        res = []
        for row in self.outputs['reduced-jump-table'].data:
            model_id = row['model_id']
            model_jump_table = JumpTableModelEntry()
            model_jump_table.ParseFromString(row['model_jump_table'])

            res.append({
                'model_id': model_id,
                'model_jump_table': model_jump_table,
            })
        self.outputs['reduced-jump-table'] = res

    @property
    def reduced_table(self):
        return self.outputs['reduced-jump-table']
