# coding: utf-8

import os
import yatest.common

from yt.wrapper import ypath_join

from market.pylibrary.memoize.memoize import memoize

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.test_envs.base_env import BaseEnv

from market.tools.jump_table_dumper.yatf.resources.jump_table_flat import (
    JumpTableFlat
)


class JumpTableDumper(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        super(JumpTableDumper, self).__init__(**resources)
        self.yt_test_folder = get_yt_prefix()
        self.reduced_jump_table = ypath_join(self.yt_test_folder, 'reduced_jump_table')
        self.output_path = os.path.join(self.output_dir, 'jump_table.fb')
        if self.resources["use_64bit"]:
            self.output_path = self.output_path + '64'  # adjust extension for 64 bit (extension fb64 instead of fb)

    @property
    def description(self):
        return 'jump-table-dumper'

    def execute(self, yt_stuff):
        relative_bin_path = os.path.join(
            'market',
            'tools',
            'jump_table_dumper',
            'bin',
            'dumper',
            'jump-table-dumper',
        )
        absolute_bin_path = yatest.common.binary_path(relative_bin_path)
        input_reduced_jump_table = self.resources['input_reduced_jump_table']

        args = [
            absolute_bin_path,
            '--yt-proxy',                 yt_stuff.get_server(),
            '--input-reduced-jump-table', input_reduced_jump_table.table_path,
            '--output-file-name',         self.output_path
        ]

        if self.resources["use_64bit"]:
            args.append('--use-64-bit-flatbuffer')

        self.exec_result = self.try_execute_under_gdb(
            args,
            cwd=self.output_dir,
            check_exit_code=True
        )

        self.outputs.update({
            'jump_table_fb': JumpTableFlat(
                self.output_path,
            ),
        })

    @property
    @memoize()
    def jump_table_fb(self):
        return self.outputs['jump_table_fb'].load()
