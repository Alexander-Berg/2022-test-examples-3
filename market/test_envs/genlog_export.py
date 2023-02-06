import os
import yatest.common
from yt.wrapper import ypath_join

from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.yt_token_resource import YtTokenResource


class GenlogExportTestEnv(BaseEnv):

    GENERATION = '20200527_1807'

    def __init__(self, **resources):
        super(GenlogExportTestEnv, self).__init__(**resources)
        self.resources['yt_token'] = YtTokenResource()

    def execute(self, yt_stuff, binary_path=None):
        input_genlog_yt_path = self.resources['genlog_table'].get_path()
        if binary_path is None:
            relative_path = os.path.join('market', 'idx', 'export', 'genlog-export', 'genlog-export')
            absolute_path = yatest.common.binary_path(relative_path)
            binary_path = absolute_path

        yt_output_table = ypath_join(get_yt_prefix(), self.GENERATION, 'genlog-export')
        cmd = [
            binary_path,
            '--yt-proxy', yt_stuff.get_server(),
            '--yt-output-table', yt_output_table,
            '--yt-token-path', self.resources['yt_token'].path,
            '--summary-path', '/dev/stdout',
            '--yt-input-table', input_genlog_yt_path
        ]

        self.exec_result = self.try_execute_under_gdb(cmd, check_exit_code=True)
