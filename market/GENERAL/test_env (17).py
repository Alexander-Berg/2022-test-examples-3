# coding: utf-8

import os
import yatest.common


from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.yatf.resources.yt_table_resource import YtTableResource


class YtAdditionalModelStreamsTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        super(YtAdditionalModelStreamsTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'additional_model_streams'

    def execute(self, yt_stuff, yt_input_path, yt_micro_model_descr_streams_path, path=None):
        if path is None:
            relative_path = os.path.join('market', 'idx', 'streams',
                                         'src', 'prepare_additional_model_streams',
                                         'src', 'prepare_additional_model_streams')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        proxy = yt_stuff.get_server()

        cmd = [
            path,
            '--proxy', proxy,
            '--yt-input-path', yt_input_path,
            '--yt-micro-model-descr-streams-path', yt_micro_model_descr_streams_path,
        ]

        self.exec_result = self.try_execute_under_gdb(cmd)

        self.outputs.update(
            {
                "micro_model_descr_table": YtTableResource(yt_stuff, yt_micro_model_descr_streams_path, load=True),
            }
        )
