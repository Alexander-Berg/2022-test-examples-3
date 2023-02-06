# coding: utf-8

import os
import yatest.common


from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv


class ErfMode(object):
    PREPARE = "prepare_web"
    JOIN = "join_web"
    JOIN_SKU_FEATURES = "join_sku_features"
    ENRICH = "enrich"
    BUILD = "build"
    PREPARE_DEMAND_FORECASTING = "prepare_demand_forecasting"
    JOIN_SKU_ERF = "join_sku_erf"
    JOIN_WEB = "join_web"


# /usr/lib/yandex/indexerf prepare_web --proxy arnold.yt.yandex.net --token-path /etc/datasources/yt-market-indexer
#                                      --shops-dat-path /indexer/market/last_complete/input/shops-utf8.dat.report.generated
#                                      --web-features //home/jupiter/export/20200908-000431/factors/erf
#                                      --dst-path //home/market/testing/indexer/static_features/herf/web_features/20200908-000431
#                                      --with-models
#                                      --with-blue
#                                      --with-cgi
def prepare_web_cmd(cmd, proxy, input, output, is_herf, merge_blue_urls_with_white=False):
    cmd.extend([
        '--proxy', proxy,
        '--shops-dat-path', input.shops_dat_file_path,
        '--web-features', input.web_features_path,
        '--dst-path', output.prepared_features_table_dir,
        '--with-blue',
        '--with-models',
        '--no-optimization'
    ])

    if merge_blue_urls_with_white:
        cmd.extend([
            '--merge-blue-urls-with-white'
        ])

    if is_herf:
        cmd.extend([
            '--is-herf'
        ])


# /usr/lib/yandex/indexerf  enrich --proxy arnold.yt.yandex.net --token-path /etc/datasources/yt-market-indexer
#                                  --features-path //home/market/production/indexer/gromoi/static_features/joined
#                                  --host-id-path //home/market/production/indexer/gibson/static_features/herf/offers/joined/20191124_2201/joined_ids
#                                  --docid-2-waremd5 //home/market/production/indexer/gibson/mi3/main/20191124_2201/genlog
#                                  --dst-path //home/market/production/indexer/gromoi/static_features/enriched
#                                  --parts-count 16 --pool market-production-indexer-white
#                                  --color-path //home/market/production/indexer/gibson/mi3/main/20191124_2201/erf/colorness
def enrich_cmd(cmd, proxy, input, output, is_herf):
    cmd.extend([
        '--proxy', proxy,
        '--features-path', input.yt_joined_erf_path,
        '--features-path', input.yt_joined_erf_path,
        '--blue-features-path', input.yt_blue_joined_erf_path,
        '--host-id-path', input.yt_herf_ids_path,
        '--sku-erf-id-path', input.yt_sku_erf_ids_path,
        '--docid-2-waremd5', input.yt_docid_path,
        '--color-path', input.yt_colorness_path,
        '--dst-path', output.yt_result_erf_path,
        '--elasticity-path', input.yt_elasticity_path,
        '--parts-count', str(output.parts_cnt),
        '--combine-chunks',
    ])

    if is_herf:
        cmd.extend([
            '--is-herf',
            '--do-copy',
        ])


# /usr/lib/yandex/indexerf build --proxy arnold.yt.yandex.net --token-path /etc/datasources/yt-market-indexer
#                                --static-features //home/market/production/indexer/gromoi/static_features/enriched/0
#                                --dst-path /var/lib/yandex/indexer/market/mif/offers/20191125_0605-0/workindex/erf
#                                --max-num-offers 14701650
def build_cmd(cmd, proxy, input, output, is_herf):
    cmd.extend([
        '--proxy', proxy,
        '--static-features', input.static_features,
        '--dst-path', output.indexerf_path,
        '--max-num-offers', str(output.num_docs),
    ])

    # build with remap mode
    if input.index_prefix is not None:
        cmd.extend([
            '--index-prefix', input.index_prefix,
            '--archive-path', input.archive_path,
        ])

    if is_herf:
        cmd.append('--is-herf')

    if output.is_sku:
        cmd.append('--is-sku-erf')

    # output erf structure of blue format
    if output.is_blue:
        cmd.append('--is-blue')
        cmd.append('--the-only-one-host')

    if output.is_fixed_host_id:
        cmd.append('--is-fixed-host-id')


def join_sku_features_cmd(cmd, proxy, input, output):
    cmd.extend([
        '--proxy', proxy,
        '--blue-offers-dir', input.yt_blue_offers_dir,
        '--blue-parts-count', str(input.yt_blue_parts_count),
        '--elasticity-path', input.yt_elasticity_path,
        '--elasticity-features-result-path', output.yt_result_erf_path,
        '--combine-chunks',
    ])


def prepare_demand_forecasting_cmd(cmd, proxy, input, output):
    cmd.extend([
        '--proxy', proxy,
        '--demand-forecasting-path', input.yt_demand_forecasting_path,
        '--demand-forecasting-features-result-path', output.yt_result_erf_path,
        '--target-date', input.target_date,
    ])


def join_sku_erf_cmd(cmd, proxy, input, output):
    cmd.extend([
        '--proxy', proxy,
        '--demand-forecasting-path', input.yt_demand_forecasting_path,
        '--blue-offers-sku', input.yt_blue_offers_sku_table,
        '--blue-parts-count', str(input.yt_blue_parts_count),
        '--sku-erf-dst-path', output.yt_result_erf_path,
        '--sku-erf-ids-path', output.yt_result_erf_ids_path,
        '--combine-chunks',
    ])


def join_web_cmd(cmd, proxy, input, output, is_herf):
    cmd.extend([
        '--proxy', proxy,
        '--filtered-features', input.yt_filtered_features_path,
        '--market-urls', input.yt_market_urls_path,
        '--dst-path', output.yt_dst_dir,
        '--parts-count', str(input.parts_count),
        '--combine-chunks',
    ])

    if input.is_blue:
        cmd.extend([
            '--is-blue'
        ])

    if is_herf:
        cmd.extend([
            '--is-herf'
        ])


class YtIndexErfTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        super(YtIndexErfTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'indexerf'

    def execute(self, erf_mode, yt_stuff, is_herf=False, path=None, merge_blue_urls_with_white=False):
        if path is None:
            relative_path = os.path.join('market', 'idx', 'generation', 'indexerf', 'src', 'indexerf')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        proxy = yt_stuff.get_server()
        output = self.resources['output']
        input = self.resources['input']

        cmd = [
            path,
            erf_mode,
        ]

        if erf_mode == ErfMode.ENRICH:
            enrich_cmd(cmd, proxy, input, output, is_herf)
        if erf_mode == ErfMode.BUILD:
            build_cmd(cmd, proxy, input, output, is_herf)
        if erf_mode == ErfMode.JOIN_SKU_FEATURES:
            join_sku_features_cmd(cmd, proxy, input, output)
        if erf_mode == ErfMode.PREPARE:
            prepare_web_cmd(cmd, proxy, input, output, is_herf, merge_blue_urls_with_white)
        if erf_mode == ErfMode.PREPARE_DEMAND_FORECASTING:
            prepare_demand_forecasting_cmd(cmd, proxy, input, output)
        if erf_mode == ErfMode.JOIN_SKU_ERF:
            join_sku_erf_cmd(cmd, proxy, input, output)
        if erf_mode == ErfMode.JOIN_WEB:
            join_web_cmd(cmd, proxy, input, output, is_herf)

        self.exec_result = self.try_execute_under_gdb(cmd)

        if erf_mode == ErfMode.BUILD:
            self.outputs.update(
                {
                    "indexerf_path": output.indexerf_path,
                    "index_dir": output.index_dir,
                    "index_name": output.index_name
                }
            )
        else:
            self.outputs.update(
                {
                    "result_tables": output.load_tables(yt_stuff)
                }
            )
            if erf_mode == ErfMode.JOIN_SKU_ERF:
                self.outputs.update(
                    {
                        "result_ids_tables": output.load_ids_tables(yt_stuff)
                    }
                )

    @property
    def result_tables(self):
        return self.outputs.get('result_tables')

    @property
    def result_ids_tables(self):
        return self.outputs.get('result_ids_tables')

    @property
    def indexerf_path(self):
        return self.outputs.get('indexerf_path')

    @property
    def index_dir(self):
        return self.outputs.get('index_dir')

    @property
    def index_name(self):
        return self.outputs.get('index_name')
