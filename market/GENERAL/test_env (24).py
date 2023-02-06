# coding: utf-8

import os
import yatest.common


from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv


class StreamsConverterMode(object):
    JOIN_OFFERS_TEXTS_TO_MSKU = "join_offers_texts_to_sku"
    JOIN_ANN_DATA_STREAMS_TO_MSKU = "join_ann_data_streams_to_sku"
    PREPROCESS_METADOC_STREAMS = "preprocess_metadoc_streams"


def join_offers_texts_to_sku_cmd(cmd, proxy, input, output, category_tree):
    cmd.extend([
        '--proxy', proxy,
        '--src-offers-texts-table', input.offers_texts_table,
        '--offer-titles-stream-dst-table', output.offer_titles_stream_path,
        '--offer-texts-stream-dst-table', output.offer_texts_stream_path,
        '--category-tree-path', category_tree,
        '--tmp-path', input.tmp_path,
        '--msku-max-offers-count', str(input.msku_max_offers_count),
        '--collect-supplier-descriptions',
    ])


def join_ann_data_streams_to_sku_cmd(cmd, proxy, input, output):
    cmd.extend([
        '--proxy', proxy,
        '--src-ann-data-path', input.offers_ann_data_dir,
        '--src-msku-table', input.sharded_blue_urls_as_in_white_path,
        '--parts-count', str(input.offers_ann_data_parts_count),
        '--dst-table', output.dst_path,
        '--msku-max-offers-count', str(input.msku_max_offers_count),
    ])


def preprocess_metadoc_streams_cmd(cmd, proxy, input, output, nav_tree, nav_tree_all):
    cmd.extend([
        '--proxy', proxy,
        '--yt-genlog-path', input.genlog_path,
        '--parts-count', str(input.parts_count),
        '--dst-msku-table', output.msku_table_path,
        '--dst-no-msku-offers-table', output.no_msku_offers_table_path,
        '--msku-max-offers-count', str(input.msku_max_offers_count),
        '--collect-nids',
        '--nids-limit', str(input.nids_limit),
        '--cataloger-navigation-xml-path', nav_tree.path,
        '--cataloger-navigation-all-xml-path', nav_tree_all.path,
        '--max-text-size', str(input.max_text_size),
    ])


class StreamsConverterTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        super(StreamsConverterTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'streams_converter'

    def execute(self, mode, yt_stuff, path=None):
        if path is None:
            relative_path = os.path.join('market', 'idx', 'streams',
                                         'src', 'streams_converter',
                                         'src', 'streams_converter')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        proxy = yt_stuff.get_server()
        output = self.resources['output']
        input = self.resources['input']

        cmd = [path, mode]
        if mode == StreamsConverterMode.JOIN_OFFERS_TEXTS_TO_MSKU:
            join_offers_texts_to_sku_cmd(cmd, proxy, input, output, self.resources['tovar_tree_pb'].path)
        elif mode == StreamsConverterMode.JOIN_ANN_DATA_STREAMS_TO_MSKU:
            join_ann_data_streams_to_sku_cmd(cmd, proxy, input, output)
        elif mode == StreamsConverterMode.PREPROCESS_METADOC_STREAMS:
            preprocess_metadoc_streams_cmd(cmd, proxy, input, output, self.resources['cataloger_navigation_xml'],
                                           self.resources['cataloger_navigation_all_xml'])

        self.exec_result = self.try_execute_under_gdb(cmd)

        if mode == StreamsConverterMode.JOIN_OFFERS_TEXTS_TO_MSKU:
            self.outputs.update(
                {
                    "offer_titles_stream_table": output.load_offer_titles_stream_table(yt_stuff),
                    "offer_texts_stream_table": output.load_offer_texts_stream_table(yt_stuff),
                    "offer_titles_stream_data": output.load_offer_titles_stream_data(yt_stuff),
                    "offer_texts_stream_data": output.load_offer_texts_stream_data(yt_stuff),
                }
        )

        if mode == StreamsConverterMode.JOIN_ANN_DATA_STREAMS_TO_MSKU:
            self.outputs.update(
                {
                    "offer_web_stream_for_msku_table": output.load_offer_web_stream_for_msku_table(yt_stuff),
                    "offer_web_stream_for_msku_data": output.load_offer_web_stream_for_msku_data(yt_stuff),
                }
            )

        if mode == StreamsConverterMode.PREPROCESS_METADOC_STREAMS:
            self.outputs.update(
                {
                    "msku_table": output.load_msku_table(yt_stuff),
                    "no_msku_offers_table": output.load_no_msku_offers_table(yt_stuff),
                }
            )
