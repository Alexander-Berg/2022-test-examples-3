# coding: utf-8

import os
import yatest.common

from market.library.common_proxy.yatf.test_envs.test_env import CommonProxyTestEnv

LB_DUMPER_BIN = yatest.common.binary_path(os.path.join('market', 'amore', 'output', 'lbdumper_amore', 'bin', 'lbdumper'))


class LbDumperTestEnv(CommonProxyTestEnv):

    def __init__(self, yt_server, **resources):
        super(LbDumperTestEnv, self).__init__(
            app_name='lbdumper',
            bin_path=LB_DUMPER_BIN,
            **resources
        )
        self.yt_server = yt_server
        self.yt_client = yt_server.get_yt_client()
        self.yt_amore_info_table = self.resources.get('yt_amore_info_table', None)
        self.yt_blue_amore_info_table = self.resources.get('yt_blue_amore_info_table', None)

    @property
    def yt_amore_info_table_data(self):
        self.yt_amore_info_table.load()
        return self.yt_amore_info_table.data

    @property
    def yt_blue_amore_info_table_data(self):
        self.yt_blue_amore_info_table.load()
        return self.yt_blue_amore_info_table.data
