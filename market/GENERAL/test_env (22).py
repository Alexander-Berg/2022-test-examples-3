# coding: utf-8

import os
import yatest.common


from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.yatf.resources.yt_table_resource import YtTableResource

from market.proto.indexer.GenerationLog_pb2 import Record
from market.proto.ir.UltraController_pb2 import EnrichedOffer

from hamcrest import equal_to, contains_inanyorder, has_length, all_of


class YtModelStreamsForOffersTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        super(YtModelStreamsForOffersTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'model_streams_for_offers'

    def execute(self,
                yt_stuff,
                offers_dir,
                parts_count,
                yt_title_streams_path,
                yt_alias_streams_path,
                yt_marketing_descr_streams_path,
                yt_micro_model_descr_streams_path,
                yt_cpa_queries_streams_path,
                yt_title_streams_result_path,
                yt_alias_streams_result_path,
                yt_marketing_descr_streams_result_path,
                yt_micro_model_descr_streams_result_path,
                yt_cpa_queries_result_path,
                path=None):
        if path is None:
            relative_path = os.path.join('market', 'idx', 'streams',
                                         'src', 'prepare_model_streams_for_offers',
                                         'src', 'prepare_model_streams_for_offers')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        proxy = yt_stuff.get_server()

        cmd = [
            path,
            '--proxy', proxy,
            '--offers-dir', offers_dir,
            '--yt-alias-streams-path', yt_alias_streams_path,
            '--yt-title-streams-path', yt_title_streams_path,
            '--yt-marketing-description-streams-path', yt_marketing_descr_streams_path,
            '--yt-micro-model-description-streams-path', yt_micro_model_descr_streams_path,
            '--yt-cpa-queries-streams-path', yt_cpa_queries_streams_path,
            '--yt-alias-streams-result-path', yt_alias_streams_result_path,
            '--yt-title-streams-result-path', yt_title_streams_result_path,
            '--yt-marketing-description-streams-result-path', yt_marketing_descr_streams_result_path,
            '--yt-micro-model-description-streams-result-path', yt_micro_model_descr_streams_result_path,
            '--yt-cpa-queries-result-path', yt_cpa_queries_result_path,
            '--parts-count', str(parts_count)
        ]

        self.exec_result = self.try_execute_under_gdb(cmd)

        self.outputs.update(
            {
                "aliases": YtTableResource(yt_stuff, yt_alias_streams_result_path, load=True),
                "titles": YtTableResource(yt_stuff, yt_title_streams_result_path, load=True),
                "marketing_descriptions": YtTableResource(yt_stuff, yt_marketing_descr_streams_result_path, load=True),
                "micro_model_descriptions": YtTableResource(yt_stuff, yt_micro_model_descr_streams_result_path, load=True),
                "cpa_queries": YtTableResource(yt_stuff, yt_cpa_queries_result_path, load=True),
            }
        )


class Offer:
    def __init__(self, ware_md5, hyperid=None, is_fake_msku_offer=None):
        self.record = Record()
        self.uc = EnrichedOffer()

        self.record.feed_id = 123
        self.record.offer_id = 'offer_id'

        if ware_md5 is not None:
            self.record.ware_md5 = ware_md5
        if is_fake_msku_offer is not None:
            self.record.is_fake_msku_offer = is_fake_msku_offer
        if hyperid is not None:
            self.uc.matched_id = hyperid

    def to_row(self):
        return {"genlog": self.record.SerializeToString(), "uc": self.uc.SerializeToString()}


def contains_only(items):
    """Проверяет что проверяемый массив и items содержат одни и те же элементы (без учета порядка)"""
    return all_of(
        has_length(equal_to(len(items))),
        contains_inanyorder(*[equal_to(item) for item in items])
        )
