# coding: utf-8

import os
import yatest.common

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.yatf.resources.yt_table_resource import YtTableResource


class YtCustomModelStreamsTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        super(YtCustomModelStreamsTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'custom_model_streams'

    def execute(self, yt_stuff, model_path, yt_alias_streams_path, yt_title_streams_path, yt_marketing_description_streams_path, path=None):
        if path is None:
            relative_path = os.path.join('market', 'idx', 'streams',
                                         'src', 'prepare_model_streams',
                                         'src', 'prepare_model_streams')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        proxy = yt_stuff.get_server()

        cmd = [
            path,
            '--proxy', proxy,
            '--yt-models-path', model_path,
            '--yt-alias-streams-path', yt_alias_streams_path,
            '--yt-title-streams-path', yt_title_streams_path,
            '--yt-marketing-description-streams-path', yt_marketing_description_streams_path,
        ]

        self.exec_result = self.try_execute_under_gdb(cmd)

        self.outputs.update(
            {
                "aliases_table": YtTableResource(yt_stuff, yt_alias_streams_path, load=True),
                "titles_table": YtTableResource(yt_stuff, yt_title_streams_path, load=True),
                "marketing_descriptions_table": YtTableResource(yt_stuff, yt_marketing_description_streams_path, load=True),
            }
        )
