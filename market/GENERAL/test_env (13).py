# coding: utf-8

import os
import yatest.common

from yatest.common import network
from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.test_envs.base_env import BaseEnv


def _STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'marketindexer', 'yatf',
        'resources',
        'stubs',
    )


class MarketIndexer(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def init_stubs(self):
        self._STUBS = BaseEnv.merge_stubs(
            {
                name: FileResource(os.path.join(_STUBS_DIR(), filename))
                for name, filename in {
                    'datasources_conf': 'datasources.conf',
                    'zookeeper_conf': 'zookeeper.conf',
                    'common_ini': 'common.ini',
                    'local_ini': 'local.ini',
                }.items()
            },
        )

    def export_os_env_variables(self):
        os.environ['DS_CONFIG_PATH'] = self.resources['datasources_conf'].path
        os.environ['ZK_CONFIG_PATH'] = self.resources['zookeeper_conf'].path
        os.environ['IC_CONFIG_PATH'] = self.resources['common_ini'].path
        os.environ['IL_CONFIG_PATH'] = self.resources['local_ini'].path

    def __init__(self, yt_stuff, **resources):
        self.init_stubs()
        super(MarketIndexer, self).__init__(**resources)
        self._yt_stuff = yt_stuff
        self.yt_test_folder = get_yt_prefix()
        # init os env variables
        self.export_os_env_variables()
        self._pm = None

    @property
    def description(self):
        return 'marketindexer-env'

    def execute(self, clt_command_args_list):
        relative_bin_path = os.path.join(
            'market', 'idx', 'marketindexer', 'bin', 'mindexer_clt', 'mindexer_clt',
        )
        absolute_bin_path = yatest.common.binary_path(relative_bin_path)
        args = [
            absolute_bin_path,
        ] + clt_command_args_list

        self.exec_result = self.try_execute_under_gdb(
            args,
            cwd=self.output_dir,
            check_exit_code=True,
        )

    def get_table_resource_data(self, path):
        return YtTableResource(
            self._yt_stuff,
            path,
            load=True
        ).data

    def __enter__(self):
        self._pm = network.PortManager()
        super(MarketIndexer, self).__enter__()
        self.resources['common_ini'].init_after_resourses(self)
        return self

    def __exit__(self, *args):
        if self._pm:
            self._pm.release()

    @property
    def port_manager(self):
        return self._pm

    @property
    def working_dir(self):
        return self.resources['common_ini'].test_work_dir
