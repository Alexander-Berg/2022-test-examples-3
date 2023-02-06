# coding: utf-8

import os
import yatest.common

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv


# /usr/lib/yandex/produce_offer_status
#                           --yt-proxy arnold.yt.yandex.net
#                           --yt-token-path /etc/datasources/yt-market-indexer
#                           --yt-process-log //home/market/production/indexer/gibson/mi3/main/20210205_2202/process_log/process_log
#                           --yt-process-log //home/market/production/indexer/gibson/mi3/main/20210205_2202/blue_shard_other_genlog_out/process_log/process_log
#                           --yt-genlog //home/market/production/indexer/gibson/mi3/main/20210205_2202/genlog
#                           --parts-count 16
#                           --yt-genlog  //home/market/production/indexer/gibson/mi3/main/20210205_2202/genlog_blue_on_white
#                           --parts-count 8
#                           --yt-dropped-offers //home/market/production/indexer/gibson/out/dropped_offers/20210205_2202
#                           --shops-dat /indexer/market/20210205_2202/input/shops-utf8.dat.report.generated
#                           --dst-path //home/market/production/indexer/gibson/out/offers_status/20210205_2202
class YtOfferStatusTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, yt_stuff, **resources):
        super(YtOfferStatusTestEnv, self).__init__(**resources)
        self.yt_client = yt_stuff.get_yt_client()

    @property
    def description(self):
        return 'offer-status'

    def execute(self, yt_stuff, path=None):
        if path is None:
            relative_path = os.path.join('market', 'idx', 'generation', 'offer_status', 'src', 'produce_offer_status')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        proxy = yt_stuff.get_server()
        result_table = self.resources['output']
        input = self.resources['input']

        cmd = [
            path,
            '--yt-proxy', proxy,
            '--shops-dat', input.shops_dat_file_path,
            '--dst-path', result_table.get_path(),
        ]
        for process_log in input.process_log_paths:
            cmd.extend([
                '--yt-process-log', process_log,
            ])

        for dropped_offers in input.dropped_offers_paths:
            cmd.extend([
                '--yt-dropped-offers', dropped_offers,
            ])

        for genlog, parts_count in input.genlog_paths:
            cmd.extend([
                '--yt-genlog', genlog,
                '--parts-count', str(parts_count),
            ])

        self.exec_result = self.try_execute_under_gdb(cmd)

        result_table.load()
        self.outputs.update(
            {
                "result_table": result_table
            }
        )

    @property
    def result_table(self):
        return self.outputs.get('result_table')
