# coding: utf-8

import collections
import itertools
import os
import yatest.common

from hamcrest import (
    assert_that,
    equal_to,
)

from yt.wrapper import ypath_join

from market.idx.offers.yatf.resources.idx_prepare_offers.blue_offers_sku_table import BlueOffersSkuTable
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.offers.yatf.resources.idx_prepare_offers.custom_offer_streams_table import CustomOfferStreamsTable
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import (
    WareMd5DulicatesTable,
    WareMd5DulicatesWithMskuTable,
)
from market.idx.offers.yatf.resources.idx_prepare_offers.deleted_msku_table import DeletedMskuTable
from market.idx.offers.yatf.resources.idx_prepare_offers.filtered_offer_table import FilteredOfferTable
from market.idx.offers.yatf.resources.idx_prepare_offers.market_joined_normalized_table import MarketJoinedNormalizedTable
from market.idx.offers.yatf.resources.idx_prepare_offers.offers_search_texts_table import OffersSearchTextsTable
from market.idx.offers.yatf.resources.idx_prepare_offers.offers_with_descriptions_table import OffersWithDescriptionTable
from market.idx.offers.yatf.resources.idx_prepare_offers.or3offers_table import Or3OffersTable
from market.idx.offers.yatf.utils.fixtures import corrected_offer

from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.resources.yt_token_resource import YtTokenResource
from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.yatf.resources.resource import FileResource


def get_iterable(x):
    if isinstance(x, collections.Iterable):
        return x
    else:
        return x,


def _DATA_GETTER_CURENCY_RATES_STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(), 'market', 'idx', 'yatf',
        'resources',
        'stubs',
        'getter',
        'currency_rates'
    )


class Or3MainIdxTestEnv(BaseEnv):
    def __init__(
        self,
        yt_stuff,
        generation,
        mi3_type,
        shards,
        half_mode,
        blue_shards=1,
        use_hiding_rules=False,
        drop_msku_without_offers=None,
        enable_contex=None,
        one_table_mode=False,
        **resources
    ):
        self._STUBS = {
            'currency_rates': FileResource(os.path.join(_DATA_GETTER_CURENCY_RATES_STUBS_DIR(), 'currency_rates.xml')),
        }
        super(Or3MainIdxTestEnv, self).__init__(**resources)
        self._yt_stuff = yt_stuff
        self.yt_client = self._yt_stuff.get_yt_client()
        self.generation = generation
        self.mi3_type = mi3_type
        self.shards = shards
        self.half_mode = half_mode
        self.one_table_mode = one_table_mode
        self.blue_shards = blue_shards
        self.use_hiding_rules = use_hiding_rules
        self.drop_msku_without_offers = drop_msku_without_offers
        self.enable_contex = enable_contex

        resources_stubs = {
            'config': Or3Config(),
            'yt_token': YtTokenResource()
        }
        for name, val in resources_stubs.items():
            if name not in self.resources:
                self.resources[name] = val
        self.params_table = self.resources.get('params_table')
        self.shops_dat = self.resources.get('shops_dat')
        self.stop_word_hiding_rules_json = self.resources.get('stop_word_hiding_rules_json')
        self.tovar_tree_pb = self.resources.get('tovar_tree_pb')

        self.tables = self._create_tables()
        self.outputs = dict()

    @property
    def description(self):
        return 'idx_main'

    @property
    def binary_path(self):
        return os.path.join(
            'market', 'idx', 'offers', 'bin', 'main-idx', 'or3-main-idx'
        )

    @property
    def yt_stuff(self):
        return self._yt_stuff

    @property
    def home_dir(self):
        return self.resources['config'].options['yt']['home_dir']

    @property
    def resource_dependencies(self):
        return {
            'config': 'yt_token'
        }

    def _create_tables(self):
        tables = {
            'offers_shards': [
                Or3OffersTable(self.yt_stuff, path)
                for path in self.offers_shards_paths
            ],
            'blue_offers_shards': [
                Or3OffersTable(self.yt_stuff, path)
                for path in self.blue_offers_shards_paths
            ],
            'blue_offer_to_stock': YtTableResource(self.yt_stuff, self.blue_offer_to_stock_table_path),
            'blue_offers_search_texts': OffersSearchTextsTable(self.yt_stuff, self.blue_offers_search_texts_table_path),
            'blue_offers_sku': BlueOffersSkuTable(self.yt_stuff, self.blue_offers_sku_table_path),
            'blue_offers_urls': YtTableResource(self.yt_stuff, self.blue_offers_urls_table_path),
            'custom_offer_streams': CustomOfferStreamsTable(self.yt_stuff, self.custom_offer_streams_table_path),
            'deleted_msku': DeletedMskuTable(self.yt_stuff, self.deleted_msku_table_path),
            'mainidx_filtered_offer': FilteredOfferTable(self.yt_stuff, self.filtered_offer_table_path),
            'dropped_offers': Or3OffersTable(self.yt_stuff, self.dropped_offers_table_path),
            'market_joined_normalized': MarketJoinedNormalizedTable(self.yt_stuff, self.market_joined_normalized_table_path),
            'offer_to_stock': YtTableResource(self.yt_stuff, self.offer_to_stock_table_path),
            'offers_search_texts': OffersSearchTextsTable(self.yt_stuff, self.offers_search_texts_table_path),
            'offers_with_description': OffersWithDescriptionTable(self.yt_stuff, self.offers_with_descriptions_table_path),
            'sharded_blue_urls_as_in_white': YtTableResource(self.yt_stuff, self.sharded_blue_urls_as_in_white_table_path),
            'ware_md5_duplicates': WareMd5DulicatesTable(self.yt_stuff, self.ware_md5_duplicates_table_path),
            'ware_md5_duplicates_with_msku': WareMd5DulicatesWithMskuTable(self.yt_stuff, self.ware_md5_duplicates_with_msku_table_path),
            'blue_offer_to_pic': YtTableResource(self.yt_stuff, self.blue_offer_to_pic_table_path),
            'joined_blue_offer_to_pic': YtTableResource(self.yt_stuff, self.joined_blue_offer_to_pic_reduced_table_path),
        }
        return tables

    def _check_tables(self):
        if self.yt_client.exists(self.offers_shards_dir):
            shards = self.yt_client.list(self.offers_shards_dir, absolute=True)
            assert_that(
                shards,
                equal_to(self.offers_shards_paths),
                'All shard tables are presented'
            )

        if self.yt_client.exists(self.blue_offers_shards_dir):
            blue_shards = self.yt_client.list(self.blue_offers_shards_dir, absolute=True)
            assert_that(
                blue_shards,
                equal_to(self.blue_offers_shards_paths),
                'All blue shard tables are presented'
            )

    def _reload_tables(self):
        for table_group in self.tables.values():
            for table in get_iterable(table_group):
                if self.yt_client.exists(table.table_path):
                    table.load()

    @staticmethod
    def _get_corrected_offers(tables):
        return [
            [
                corrected_offer(x)
                for x in table
            ]
            for table in tables
        ]

    @staticmethod
    def _join_tables(tables):
        return list(
            itertools.chain.from_iterable(
                tables
            )
        )

    @classmethod
    def _get_offers_groupped_by_offer_id(cls, tables):
        return {
            x['offer_id']: x
            for x in cls._join_tables(tables)
        }

    def _update_output_data(self):
        """
        Для обычных таблиц возвращает контент в виде списка.
        Для шардированных таблиц офферов возвращает результаты в трёх разных видах:
        1. offers_shards, blue_offers_shards
            скорректированные оффера по шардам: [[],[],...]
        2. offers, blue_offers
            скорректированные оффера одним списком: [...]
        3. offers_by_offer_id, blue_offers_by_offer_id
            словарь скорректированных офферов по offer_id из {'offer_id': offer,...}
        """
        for name, table_group in self.tables.items():
            if isinstance(table_group, list):
                self.outputs[name] = [table.data for table in table_group if table.data is not None]
            else:
                self.outputs[name] = table_group.data

        self.outputs['offers_shards'] = self._get_corrected_offers(self.outputs['offers_shards'])
        self.outputs['blue_offers_shards'] = self._get_corrected_offers(self.outputs['blue_offers_shards'])

        self.outputs['offers'] = self._join_tables(self.outputs['offers_shards'])
        self.outputs['blue_offers'] = self._join_tables(self.outputs['blue_offers_shards'])

        self.outputs['offers_by_offer_id'] = self._get_offers_groupped_by_offer_id(self.outputs['offers_shards'])
        self.outputs['blue_offers_by_offer_id'] = self._get_offers_groupped_by_offer_id(self.outputs['blue_offers_shards'])

    def execute(self, path=None, generate_stream_tables=False, generate_blue_urls_table=False):
        self.execute_task(path, generate_stream_tables, generate_blue_urls_table, 'BuildFilteredMskuInfo')
        self.execute_task(path, generate_stream_tables, generate_blue_urls_table, 'MovePoSToSsd')
        self.execute_task(path, generate_stream_tables, generate_blue_urls_table, 'PrepareBids')
        self.execute_task(path, generate_stream_tables, generate_blue_urls_table)

        self._check_tables()
        self._reload_tables()
        self._update_output_data()

    def execute_task(self, path=None, generate_stream_tables=False, generate_blue_urls_table=False, subtask=None):
        if path is None:
            relative_path = self.binary_path
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        cmd = [
            path,
            '--generation', self.generation,
            '--mi3-type', self.mi3_type,
            '--shards', str(self.shards),
            '--config', self.resources['config'].path,
            '--filtered-msku-file-path', os.path.join(self.input_dir, 'filtered_msku_info'),
            '--currency-rates-path', self.resources['currency_rates'].path,
        ]

        if self.shops_dat:
            cmd += ['--shops-dat', os.path.join(self.input_dir, self.shops_dat.filename)]

        if self.blue_shards:
            cmd += ['--blue-shards', str(self.blue_shards)]
        if self.half_mode:
            cmd += ['--half']
        if self.one_table_mode:
            cmd += ['--one-table-mode']
        if generate_blue_urls_table:
            cmd += ['--yt-generate-blue-urls-table']

        if self.stop_word_hiding_rules_json:
            cmd.extend(
                [
                    '--stop-words-file-path',
                    os.path.join(self.input_dir, self.stop_word_hiding_rules_json.filename),
                    '--hiding-rules-file-path',
                    os.path.join(self.input_dir, 'hiding_rules.pbuf'),
                ]
            )

        if self.tovar_tree_pb:
            cmd += ['--category-file-path', os.path.join(self.input_dir, self.tovar_tree_pb.filename)]

        if self.use_hiding_rules:
            cmd += ['--use-hiding-rules']

        if generate_stream_tables:
            cmd += ['--yt-generate-streams-tables']

        if self.drop_msku_without_offers:
            cmd += ['--drop-msku-without-offers']

        if self.enable_contex:
            cmd += ['--enable-contex']

        if subtask is not None:
            cmd += ['--subtask', subtask]

        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=True
        )

    @property
    def offers_shards_dir(self):
        return ypath_join(self._generation_path, 'offers')

    @property
    def blue_offers_shards_dir(self):
        return ypath_join(self._generation_path, 'blue_offers_shards')

    @property
    def offers_shards_paths(self):
        paths = [
            ypath_join(self.offers_shards_dir, "%04d" % (shard,))
            for shard in range(self.shards)
        ]
        return paths

    @property
    def blue_offers_shards_paths(self):
        paths = [
            ypath_join(self.blue_offers_shards_dir, "%04d" % (shard,))
            for shard in range(self.blue_shards)
        ]
        return paths

    @property
    def _generation_path(self):
        return ypath_join(self.home_dir, 'mi3', self.mi3_type, self.generation)

    @property
    def _custom_streams_path(self):
        return ypath_join(self.home_dir, "streams", "offers", self.generation, "custom_streams")

    @property
    def dropped_offers_table_path(self):
        return ypath_join(self._generation_path, 'dropped_offers')

    @property
    def offers_with_descriptions_table_path(self):
        return ypath_join(self._custom_streams_path, 'descriptions_tmp', 'offers_with_descriptions')

    @property
    def custom_offer_streams_table_path(self):
        return ypath_join(self._custom_streams_path, 'titles')

    @property
    def blue_offers_sku_table_path(self):
        return ypath_join(self._generation_path, 'blue_offers_sku')

    @property
    def deleted_msku_table_path(self):
        return ypath_join(self._generation_path, 'deleted_msku')

    @property
    def filtered_offer_table_path(self):
        return ypath_join(self._generation_path, 'mainidx_filtered_offer')

    @property
    def offers_search_texts_table_path(self):
        return ypath_join(self._generation_path, 'offers_search_texts')

    @property
    def blue_offers_search_texts_table_path(self):
        return ypath_join(self._generation_path, 'blue_offers_search_texts')

    @property
    def blue_offers_urls_table_path(self):
        return ypath_join(self._generation_path, 'blue_offers_urls')

    @property
    def sharded_blue_urls_as_in_white_table_path(self):
        return ypath_join(self._generation_path, 'sharded_blue_urls_as_in_white')

    @property
    def ware_md5_duplicates_table_path(self):
        return ypath_join(self._generation_path, 'work', 'ware_md5_duplicates')

    @property
    def ware_md5_duplicates_with_msku_table_path(self):
        return ypath_join(self._generation_path, 'work', 'ware_md5_duplicates_with_msku')

    @property
    def market_joined_normalized_table_path(self):
        return ypath_join(self._generation_path, 'market_joined_normalized')

    @property
    def offer_to_stock_table_path(self):
        return ypath_join(self._generation_path, 'work', 'offer2stock_reduced')

    @property
    def blue_offer_to_stock_table_path(self):
        return ypath_join(self._generation_path, 'work', 'blue_offer2stock_reduced')

    @property
    def blue_offer_to_pic_table_path(self):
        return ypath_join(self._generation_path, 'work', 'blue_offer2pic')

    @property
    def joined_blue_offer_to_pic_reduced_table_path(self):
        return ypath_join(self._generation_path, 'work', 'sorted_blue_offer2pic_reduced')
