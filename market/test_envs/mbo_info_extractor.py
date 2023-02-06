# -*- coding: utf-8 -*-

import os
import yatest.common

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.yatf.resources.mbo.contex_relations import (
    ContexRelationsPbsnResult
)


# Stub directory is the same as for model_indexer because it has the same inputs data
def _STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'models', 'yatf',
        'resources',
        'models_indexer',
        'stubs',
    )


class MboInfoExtractorTestEnv(BaseEnv):

    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(
            self,
            only_published=False,
            include_contex_proto=True,
            yt_input_dir=None,
            **resources
    ):
        self._STUBS = {
            name: FileResource(os.path.join(_STUBS_DIR(), filename))
            for name, filename in {
                'models': 'models_90592.pb',
                'parameters': 'parameters_90592.pb',
                'sku': 'sku_90592.pb',
            }.items()
            if name not in list(resources.keys())
        }
        super(MboInfoExtractorTestEnv, self).__init__(**resources)
        self.only_published=only_published
        self.include_contex_proto=include_contex_proto
        self.yt_input_dir = yt_input_dir

    @property
    def description(self):
        return 'mbo_info_extractor'

    def execute(self, yt_stuff=None, path=None):
        if path is None:
            relative_path = os.path.join(
                'market', 'idx', 'models', 'bin',
                'mbo-info-extractor',
                'mbo-info-extractor',
            )
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        cmd = [
            path,
            '--input-dir', self.input_dir,
            '--output-dir', self.output_dir,
            '--threads', '8',
        ]

        if self.only_published:
            cmd.extend(['--only-published'])

        if self.include_contex_proto:
            cmd.extend(['--include-contex-proto'])

        if yt_stuff is not None and self.yt_input_dir:
            cmd.extend([
                '--yt-mbo-input', self.yt_input_dir,
                '--yt-proxy', yt_stuff.get_server(),
            ])

        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=True,
        )

        self.outputs.update({
            'contex_experiments': FileResource(
                os.path.join(self.output_dir, 'contex_experiments.txt.gz')
            ),
            'model_group': FileResource(
                os.path.join(self.output_dir, 'model_group_csv')
            ),
            'model_group_for_beru_msku_card': FileResource(
                os.path.join(self.output_dir, 'model_group_for_beru_msku_card_csv')
            ),
            'model_ids': FileResource(
                os.path.join(self.output_dir, 'model_ids.gz')
            ),
            'param_allowed': FileResource(
                os.path.join(self.output_dir, 'param_allowed.csv')
            ),
        })

        if self.include_contex_proto:
            self.outputs.update({
                'contex_relations_pbsn': ContexRelationsPbsnResult(
                    os.path.join(self.output_dir, 'contex_relations.pbsn')
                )
            })
            self.outputs['contex_relations_pbsn'].load()
