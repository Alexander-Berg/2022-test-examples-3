# coding: utf-8

import os
import yatest.common

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.yatf.resources.yt_table_resource import YtTableResource


class YtDescriptionStreamsTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        super(YtDescriptionStreamsTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'description_streams'

    def execute(self,
                yt_stuff,
                rthub_path,
                genlog_table,
                descriptions_table,
                hosts_table,
                dont_clear_rthub=False,
                path=None):

        if path is None:
            relative_path = os.path.join('market', 'idx', 'streams',
                                         'src', 'prepare_description_streams',
                                         'src', 'prepare_description_streams')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        proxy = yt_stuff.get_server()

        cmd = [
            path,
            'preprocess',
            '--proxy', proxy,
            '--rthub-path', rthub_path,
            '--descriptions-table', descriptions_table,
            '--recent-genlog-table', genlog_table,
            '--hosts-table', hosts_table,
            '--working-path', '//home/test_descriptions'
        ]

        if dont_clear_rthub:
            cmd += ['--dont-clear-rthub']

        self.exec_result = self.try_execute_under_gdb(cmd)

        self.outputs.update(
            {
                "descriptions_table": YtTableResource(yt_stuff,
                                                      descriptions_table,
                                                      load=True),
                "hosts_table": YtTableResource(yt_stuff,
                                               hosts_table,
                                               load=True)
            }
        )

    def execute_synch_part(self,
                           yt_stuff,
                           descriptions_table,
                           working_path,
                           description_streams,
                           path=None):

        if path is None:
            relative_path = os.path.join('market', 'idx', 'streams',
                                         'src', 'prepare_description_streams',
                                         'src', 'prepare_description_streams')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        proxy = yt_stuff.get_server()

        cmd = [
            path,
            'produce',
            '--proxy', proxy,
            '--working-path', working_path,
            '--descriptions-table', descriptions_table,
            '--description-streams', description_streams,
        ]

        self.exec_result = self.try_execute_under_gdb(cmd)

        self.outputs.update(
            {
                "description_streams": YtTableResource(yt_stuff,
                                                       description_streams,
                                                       load=True),
            }
        )
