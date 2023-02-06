# coding: utf-8

import os
import yatest.common

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.yatf.resources.resource import FileResource


def _STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'generation', 'yatf',
        'resources',
        'sentences_length_indexer',
        'stubs'
    )


class IndexsentTestEnv(BaseEnv):

    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        self._STUBS = {
            name: FileResource(os.path.join(_STUBS_DIR(), filename))
            for name, filename in {
                'indexaa': 'indexaa',
                'indexarc': 'indexarc',
                'indexinv': 'indexinv',
                'indexkey': 'indexkey',
            }.items()
        }
        super(IndexsentTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'indexsent'

    def execute(self, path=None):
        if path is None:
            relative_path = os.path.join(
                'yweb', 'robot',
                'write_sentence_lengths',
                'write_sentence_lengths',
            )
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        i = os.path.join(self.input_dir, 'index')

        cmd = [
            path,
            '-i', i,
            '-o', self.output_dir,
            '-g'
        ]
        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=False)
        self.outputs.update({
            'indexsent': FileResource(
                os.path.join(self.output_dir, 'indexsent')
            )
        })
