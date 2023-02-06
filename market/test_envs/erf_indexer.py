# coding: utf-8

import os
import yatest.common

from market.idx.generation.yatf.resources.erf_indexer.web_features import (
    Erf,
    Herf,
)
from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.test_envs.base_env import BaseEnv


def _STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'generation', 'yatf',
        'resources',
        'erf_indexer',
        'stubs'
    )


class IndexerfTestEnv(BaseEnv):

    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        self._STUBS = {
            name: FileResource(os.path.join(_STUBS_DIR(), filename))
            for name, filename in {

            }.items()
        }
        super(IndexerfTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'indexerf'

    def execute(self, index_dir, path=None):
        if path is None:
            relative_path = os.path.join(
                'market', 'tools',
                'indexerf_generator',
                'indexerf_generator',
            )
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        arc_path = os.path.join(index_dir, 'index')

        cmd = [
            path,
            '--arc-path', arc_path,
            '--web-features-path', self.resources['web_features'].path,
            '--output', self.output_dir
        ]
        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=False)

        self.outputs.update(
            {
                "erf": Erf(os.path.join(self.output_dir, 'erf')),
                "herf": Herf(os.path.join(self.output_dir, 'herf'))
            }
        )
