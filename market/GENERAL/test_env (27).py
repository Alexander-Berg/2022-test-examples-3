# coding: utf-8
import os
import yatest.common

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv


class Pbsn2YtTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        super(Pbsn2YtTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'pnsn2yt'

    def execute(self, path=None):
        if path is None:
            relative_path = os.path.join('market', 'idx', 'tools', 'pbsn2yt', 'bin',
                                         'pbsn2yt')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        cmd = [
            path,
            '--input-format', self.resources['input'].format,
            '--proxy', self.resources['output'].yt_proxy,
            '--output-format', self.resources['output'].format,
            '--threads', '1',
            self.resources['input'].path,
            self.resources['output'].table_path
        ]
        if self.resources['output'].expand:
            cmd.insert(1, '--expand-by')
            cmd.insert(2, self.resources['output'].expand)

        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=False)

        self.outputs.update({
            'result': self.resources['output'].get_data()
        })

    @property
    def result(self):
        return self.outputs['result']
