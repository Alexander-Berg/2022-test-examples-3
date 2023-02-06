# coding: utf-8

import os
import yatest.common

from market.library.common_proxy.yatf.test_envs.test_env import CommonProxyTestEnv

LB_DUMPER_BIN = yatest.common.binary_path(os.path.join('market', 'idx', 'input', 'mdm_dumper', 'bin', 'lbdumper'))


class LbDumperTestEnv(CommonProxyTestEnv):

    def __init__(self, yt_server, **resources):
        super(LbDumperTestEnv, self).__init__(
            app_name='lbdumper',
            bin_path=LB_DUMPER_BIN,
            **resources
        )
        self.yt_server = yt_server
        self.yt_client = yt_server.get_yt_client()
        self.yt_mdm_info_table = self.resources.get('yt_mdm_info_table', None)

    @property
    def yt_mdm_info_table_data(self):
        self.yt_mdm_info_table.load()
        return self.yt_mdm_info_table.data
