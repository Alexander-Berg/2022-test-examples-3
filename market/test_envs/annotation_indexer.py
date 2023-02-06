# coding: utf-8

import os
import yatest.common

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.test_envs.base_env import BaseEnv


def _STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'generation', 'yatf',
        'resources',
        'annotation_indexer',
        'stubs',
    )


class IndexannTestEnv(BaseEnv):

    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        self._STUBS.update({
            name: FileResource(os.path.join(_STUBS_DIR(), filename))
            for name, filename in {
                'indexdir': 'indexdir',
                'indexarc': 'indexarc',
                'indexaa': 'indexaa',
                'tovar_tree_pb': 'tovar-tree.pb',
                'catalog.navigation.xml': 'catalog.navigation.xml'
            }.items()
        })
        super(IndexannTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'indexann_generator'

    def execute(self, path=None):
        if path is None:
            relative_path = os.path.join(
                'market', 'tools',
                'indexann_generator',
                'indexann_generator'
            )
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        g = os.path.join(self.input_dir, 'index')
        categoires = os.path.join(
            self.input_dir,
            self.resources['tovar_tree_pb'].filename
        )

        cmd = [
            path,
            '-g', g,
            categoires
        ]
        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=False
        )
