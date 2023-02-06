# coding: utf-8

import os
import yatest.common

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.yatf.resources.yt_table_resource import YtTableResource


class YtPrepareMarketStreamsTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        super(YtPrepareMarketStreamsTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'prepare_market_streams_for_offers'

    def execute(self,
                yt_stuff,
                recent_market_path,
                offers_dir,
                result_path,
                parts_count,
                path=None):

        if path is None:
            relative_path = os.path.join('market', 'idx', 'streams',
                                         'src', 'prepare_market_streams',
                                         'src', 'prepare_market_streams')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        proxy = yt_stuff.get_server()

        cmd = [
            path,
            '--proxy', proxy,
            '--recent-market-streams-path', recent_market_path,
            '--offers-dir', offers_dir,
            '--result-path', result_path,
            '--parts-count', str(parts_count)
        ]

        self.exec_result = self.try_execute_under_gdb(cmd)

        self.outputs.update(
            {
                "result_path": YtTableResource(yt_stuff,
                                               result_path,
                                               load=True),
            }
        )
