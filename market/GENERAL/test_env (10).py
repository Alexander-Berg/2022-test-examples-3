import os

from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.feeds.qparser.bin.executor.qparser import QParser
from market.idx.datacamp.proto.api.UpdateTask_pb2 import PartnerStats
from market.idx.datacamp.proto.errors.Explanation_pb2 import ExplanationBatch

from market.pylibrary.pbufsn_utils import read_pbufsn


class QParserTestEnv(BaseEnv):
    def __init__(self, qparser_bin, yt_server=None, **resources):

        super(QParserTestEnv, self).__init__(**resources)

        if 'feed_cfg' in self.resources:
            self.resources['feed_cfg'].update_options({
                'trace': {
                    'enable_offers_trace': True,
                    'offers_trace_dir': self.output_dir
                },
                'fp_metadata': {
                    'enabled': True,
                    'filename': 'test_fp_meta',
                },
            })
            if 'logbroker' in self.resources['feed_cfg'].options:
                self.resources['feed_cfg'].options['logbroker']['bind_business_id_to_partition'] = True
                self.resources['feed_cfg'].options['logbroker']['writers_count'] = 1
                self.resources['feed_cfg'].options['logbroker']['enable_stocks_deduplication_by_ts'] = True
        if yt_server:
            assert 'basic_table' in self.resources
            assert 'service_table' in self.resources
            assert 'actual_service_table' in self.resources
            yt_options = {
                'sas_proxy': yt_server.get_server(),
                'vla_proxy': yt_server.get_server(),
                'meta_proxy': yt_server.get_yt_client().config["proxy"]["url"],
                'token_path': yt_server.get_yt_client().config["token"] or "NO_TOKEN",
                'basic_offers_table': self.resources['basic_table'].table_path,
                'service_offers_table': self.resources['service_table'].table_path,
                'actual_service_offers_table': self.resources['actual_service_table'].table_path,
                'max_inflight': 1,
                'full_deduplication': True
            }
            if 'service_search_table' in self.resources:
                yt_options['search_service_offers_table'] = self.resources['service_search_table'].table_path
            if 'actual_service_search_table' in self.resources:
                yt_options['search_actual_service_offers_table'] = self.resources['actual_service_search_table'].table_path
            self.resources['feed_cfg'].update_options({'yt': yt_options})

        explanation_log_filename = self.feed_cfg.options.get('explanation_log', {}).get('filename')
        self.feed_errors_file = os.path.join(self.output_dir, explanation_log_filename) if explanation_log_filename else None

        parsing_stats_filename = self.feed_cfg.options.get('partner_stats', {}).get('filename')
        self.partner_stats_file = os.path.join(self.output_dir, parsing_stats_filename) if parsing_stats_filename else None

        self.qparser_bin = qparser_bin

    @property
    def feed_cfg(self):
        return self.resources['feed_cfg']

    @property
    def description(self):
        return 'qparser'

    @property
    def feed_parsing_errors(self):
        fatal_feed_errors = None
        if self.feed_errors_file and os.path.exists(self.feed_errors_file):
            explanation_batch = list(read_pbufsn(self.feed_errors_file, ExplanationBatch, 'EXPM'))
            if explanation_batch:
                fatal_feed_errors = explanation_batch[0].explanation
        return fatal_feed_errors

    @property
    def parsing_stats(self):
        stats = None
        if self.partner_stats_file and os.path.exists(self.partner_stats_file):
            stats = list(read_pbufsn(self.partner_stats_file, PartnerStats, 'PRST'))
            if not stats:
                stats = None  # [] -> None
        return stats

    @property
    def partner_parsing_stats(self):
        if not self.parsing_stats:
            return None
        return self.parsing_stats[0]

    @property
    def feed_archive_link(self):
        return ''

    def execute(self):
        qparser = QParser(bin_path=self.qparser_bin, config_paths=[self.feed_cfg.path])
        retcode = qparser.run(
            feed_path=self.feed_cfg.feed_path,
            feed_info=self.feed_cfg.feed_info,
            output_dir=self.output_dir,
            feed_parsing_task_filepath=self.resources['feed_parsing_task_filepath'].path,
            shopdat_path=self.resources['shops_dat'].path if 'shops_dat' in self.resources else None
        )
        return retcode
