# coding: utf-8

import os
import yatest.common

from market.idx.offers.yatf.resources.offers_indexer.tracelog import TraceLog

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.yatf.resources.yt_token_resource import YtTokenResource


class IndexTracerTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        super(IndexTracerTestEnv, self).__init__(**resources)

        resources_stubs = {
            'yt_token': YtTokenResource(),
        }
        for name, val in resources_stubs.items():
            if name not in self.resources:
                self.resources[name] = val

    @property
    def description(self):
        return 'index-tracer'

    def execute(self, yt_stuff, input_process_log_yt_path, generation, completed_offers_for_trace_table_path, binary_path=None):
        if binary_path is None:
            relative_path = os.path.join('market', 'idx', 'generation', 'index-tracer', 'index-tracer')
            absolute_path = yatest.common.binary_path(relative_path)
            binary_path = absolute_path

        out_path = os.path.join(self.output_dir, 'offers-trace.log')

        cmd = [
            binary_path,
            '--yt-proxy', yt_stuff.get_server(),
            '--yt-token-path', self.resources['yt_token'].path,
            '--process-log-yt-table', input_process_log_yt_path,
            '--output-path', out_path,
            '--generation', generation,
            '--offers-completed-yt-table', completed_offers_for_trace_table_path,
        ]

        self.exec_result = self.try_execute_under_gdb(cmd)

        self.outputs.update({
            'tracelog': TraceLog(self.output_dir),
        })

    @property
    def tracelog(self):
        result = self.outputs['tracelog']
        return result.load()
