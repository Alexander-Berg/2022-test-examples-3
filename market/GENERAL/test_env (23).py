# coding: utf-8

import os
import yatest.common


from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.yatf.resources.yt_table_resource import YtTableResource


class YtCustomOfferStreamsTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        super(YtCustomOfferStreamsTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'custom_offer_streams'

    def execute(self,
                yt_stuff,
                offers_dir,
                result_path_title,
                parts_count,
                path=None):

        if path is None:
            relative_path = os.path.join('market', 'idx', 'streams',
                                         'src', 'prepare_offer_streams',
                                         'src', 'prepare_offer_streams')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        proxy = yt_stuff.get_server()

        cmd = [
            path,
            '--proxy', proxy,
            '--offers-dir', offers_dir,
            '--result-path-title', result_path_title,
            '--parts-count', str(parts_count)
        ]

        self.exec_result = self.try_execute_under_gdb(cmd)

        self.outputs.update(
            {
                "result_path_title": YtTableResource(yt_stuff,
                                                     result_path_title,
                                                     load=True),
            }
        )
