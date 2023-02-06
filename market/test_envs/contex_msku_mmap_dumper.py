# coding: utf-8

import os

import yatest.common
from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.mmap_output import MmapOutput
from market.idx.yatf.test_envs.base_env import BaseEnv


class ContexMskuDumperTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, yt_stuff, **resources):
        self._yt_stuff = yt_stuff
        super(ContexMskuDumperTestEnv, self).__init__(**resources)

        self.output_path = os.path.join(self.output_dir, 'contex_msku.mmap')

    @property
    def description(self):
        return 'contex_msku_dumper'

    def execute(self, path=None):
        if path is None:
            relative_path = os.path.join(
                'market', 'idx', 'models', 'bin', 'contex_msku_mmap_dumper', 'src', 'contex_msku_mmap_dumper'
            )
            path = yatest.common.binary_path(relative_path)

        cmd = [
            path,
            '--yt-proxy', self._yt_stuff.get_server(),
            '--input-msku-contex-table', self.resources['msku'].table_path,
            '--output-contex-msku-path', self.output_path,
        ]

        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=False,
        )

        self.outputs.update({
            'result': MmapOutput(self.output_path)
        })

    @property
    def result(self):
        return self.outputs['result']
