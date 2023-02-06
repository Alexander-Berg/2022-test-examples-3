# coding: utf-8

import os
import yatest.common

from yt.wrapper import ypath_join

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.test_envs.base_env import BaseEnv


class ImageFactorsCollectorMode(object):
    JOIN_OFFERS = "join_offers"
    JOIN_MSKU = "join_msku"
    ENRICH_WITH_GENLOG = "enrich_with_genlog"
    DUMP_IMAGE2TEXT_V10 = "dump_image2text_v10"
    CALCULATE_FACTORS_OVER_MSKU = "calculate_factors_over_msku"


def join_offers_cmd(cmd, proxy, options, categories):
    cmd.extend([
        '--yt-proxy', proxy,
        '--common-parts-count', str(options.parts_cnt),
        '--yt-input-offers-dir', options.input_offers_dir,
        '--yt-input-picrobot-success', options.input_factors_table_path,
        '--yt-result-table', options.output,
    ])
    if categories:
        cmd.extend(['--common-categories-to-calc-image-factors', categories])


def join_msku_cmd(cmd, proxy, options, categories):
    cmd.extend([
        '--yt-proxy', proxy,
        '--common-parts-count', str(options.parts_cnt),
        '--yt-input-offers-dir', options.input_offers_dir,
        '--yt-input-msku-factors', options.input_factors_table_path,
        '--yt-result-table', options.output,
    ])
    if categories:
        cmd.extend(['--common-categories-to-calc-image-factors', categories])


def calc_factors_over_msku_cmd(cmd, proxy, options):
    cmd.extend([
        '--yt-proxy', proxy,
        '--common-parts-count', str(options.parts_cnt),
        '--yt-input-offers-dir', options.input_offers_dir,
        '--yt-input-msku-factors', options.input_factors_table_path,
        '--yt-result-table', options.output,
    ])


def enrich_cmd(cmd, proxy, options):
    cmd.extend([
        '--yt-proxy', proxy,
        '--common-parts-count', str(options.parts_cnt),
        '--yt-input-offers-factors', options.input_factors_table_path,
        '--yt-input-msku-factors', options.input_factors_table2_path,
        '--yt-input-genlog-dir', options.input_offers_dir,
        '--yt-result-dir', options.output,
    ])


def dump_i2t_v10_cmd(cmd, proxy, options):
    cmd.extend([
        '--yt-proxy', proxy,
        '--yt-input-factors-table', options.input_factors_table_path,
        '--yt-input-genlog-table', options.input_genlog_table_path,
        '--output-dir', options.output_dir,
    ])


class ImageFactorsCollectorTestEnv(BaseEnv):

    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        super(ImageFactorsCollectorTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'image_factors_collector'

    def execute(self, mode, yt_stuff, categories=None, path=None):
        if path is None:
            relative_path = os.path.join('market', 'idx', 'generation', 'image_factors_collector', 'image_factors_collector')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        proxy = yt_stuff.get_server()
        cmd = [
            path,
            mode,
        ]

        options = self.resources['options']
        if mode == ImageFactorsCollectorMode.JOIN_OFFERS:
            join_offers_cmd(cmd, proxy, options, categories)
        if mode == ImageFactorsCollectorMode.JOIN_MSKU:
            join_msku_cmd(cmd, proxy, options, categories)
        if mode == ImageFactorsCollectorMode.CALCULATE_FACTORS_OVER_MSKU:
            calc_factors_over_msku_cmd(cmd, proxy, options)
        if mode == ImageFactorsCollectorMode.ENRICH_WITH_GENLOG:
            enrich_cmd(cmd, proxy, options)
        if mode == ImageFactorsCollectorMode.DUMP_IMAGE2TEXT_V10:
            dump_i2t_v10_cmd(cmd, proxy, options)

        if 'tovar_tree_pb' in self.resources:
            cmd.extend(['--common-categories-tree-path', self.resources['tovar_tree_pb'].path])

        self.exec_result = self.try_execute_under_gdb(cmd)

        if mode == ImageFactorsCollectorMode.JOIN_OFFERS or mode == ImageFactorsCollectorMode.JOIN_MSKU:
            self.outputs.update({
                "joined_factors_table": YtTableResource(yt_stuff, options.output, load=True),
            })
        if mode == ImageFactorsCollectorMode.CALCULATE_FACTORS_OVER_MSKU:
            self.outputs.update({
                "aggregated_factors_table": YtTableResource(yt_stuff, options.output, load=True),
            })
        if mode == ImageFactorsCollectorMode.ENRICH_WITH_GENLOG:
            self.outputs.update({
                "enriched_shard_tables": self._load_shards(yt_stuff, options.output),
            })
        if mode == ImageFactorsCollectorMode.DUMP_IMAGE2TEXT_V10:
            self.outputs.update({
                "image_i2t_v12_dssm_binary": os.path.join(options.output_dir, 'image_i2t_v12_dssm.binary'),
                "image_i2t_v12_dssm_index": os.path.join(options.output_dir, 'image_i2t_v12_dssm.index'),
                "index_dir": options.output_dir,
            })

    def _load_shards(self, yt_stuff, output_dir):
        tables = yt_stuff.get_yt_client().list(output_dir)
        return [YtTableResource(yt_stuff, ypath_join(output_dir, table), load=True) for table in tables]

    @property
    def index_dir(self):
        return self.outputs.get("index_dir")
