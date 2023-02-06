# coding: utf-8

import os
import yatest.common

from market.pylibrary.memoize.memoize import memoize

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv

from market.tools.jump_table_dumper.yatf.resources.jump_table_flat import (
    JumpTableFlat
)


class JumpTableLocalDumper(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        super(JumpTableLocalDumper, self).__init__(**resources)
        self.output_path = os.path.join(self.output_dir, 'jump_table_local.fb')
        if self.resources["use_64bit"]:
            self.output_path = self.output_path + '64'  # adjust extension for 64 bit (extension fb64 instead of fb)

    @property
    def description(self):
        return 'jump-table-local-dumper'

    def execute(self):
        relative_bin_path = os.path.join(
            'market',
            'tools',
            'jump_table_dumper',
            'bin',
            'local_dumper',
            'jump-table-dumper',
        )
        absolute_bin_path = yatest.common.binary_path(relative_bin_path)
        input_file = self.resources['input_json_file']

        args = [
            absolute_bin_path,
            '-i',       input_file.path,
            '-o',       self.output_path
        ]

        print("self.output_path 2 = {}".format(self.output_path))

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
