# coding: utf-8

import os

import yatest.common

from yt.wrapper import ypath_join

from market.idx.offers.yatf.utils.fixtures import corrected_offer
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import (
    DatacampOfferMetaInfoTable,
    PushPartnersMetaInfoTable,
)
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import (
    OffersRawTable,
    BlueOffersRawTable,
)
from market.idx.offers.yatf.resources.idx_prepare_offers.market_joined_normalized_table import MarketJoinedNormalizedTable
from market.idx.offers.yatf.resources.idx_prepare_offers.hosts_table import HostsTable
from market.idx.yatf.resources.command_arguments import CommandArguments
from market.idx.yatf.resources.delivery_buckets import DeliveryBucketsCSV
from market.idx.yatf.resources.delivery_feed_info import DeliveryFeedInfo
from market.idx.yatf.resources.feedlog import FeedLog
from market.idx.yatf.resources.feedsessions import FeedSessionsFile
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.resources.yt_token_resource import YtTokenResource
from market.idx.yatf.test_envs.base_env import BaseEnv


def _DATA_GETTER_GEOBASE_STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(), 'market', 'idx', 'yatf',
        'resources',
        'stubs',
        'getter',
        'geobase'
    )


def _DATA_GETTER_CURENCY_RATES_STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(), 'market', 'idx', 'yatf',
        'resources',
        'stubs',
        'getter',
        'currency_rates'
    )


class Or3FullMakerTestEnv(BaseEnv):
    def __init__(self, yt_stuff, generation, mi3_type, prev_generation='', shards=1, **resources):
        self._STUBS = {
            'geo': FileResource(os.path.join(_DATA_GETTER_GEOBASE_STUBS_DIR(), 'geo.c2p')),
            'currency_rates': FileResource(os.path.join(_DATA_GETTER_CURENCY_RATES_STUBS_DIR(), 'currency_rates.xml')),
        }
        super(Or3FullMakerTestEnv, self).__init__(**resources)
        self._generation = generation
        self._prev_generation = prev_generation
        self._yt_stuff = yt_stuff
        self._shards = shards
        self.yt_client = self._yt_stuff.get_yt_client()
        self.output_generation_dir = os.path.join(self.output_dir, generation, 'input')
        resources_stubs = {
            'config': Or3Config(),
            'yt_token': YtTokenResource(),
            'command-arguments': CommandArguments(),
        }
        self._mi3_type = mi3_type
        for name, val in resources_stubs.items():
            if name not in self.resources:
                self.resources[name] = val

    @property
    def yt_stuff(self):
        return self._yt_stuff

    @property
    def description(self):
        return 'full_maker'

    @property
    def generation(self):
        return self._generation

    @property
    def resource_dependencies(self):
        return {
            'config': 'yt_token'
        }

    def execute(self, path=None):
        if path is None:
            relative_path = os.path.join('market', 'idx', 'offers', 'bin',
                                         'full-maker', 'full-maker')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        cmd = [
            path,
            '--working-dir', self.output_dir,
            '--generation', self._generation,
            '--config', self.resources['config'].path,
            '--geo', self.resources['geo'].path,
            '--write-bucket-info-vectors',
            '--currency-rates-path', self.resources['currency_rates'].path,
            '--shards', str(self._shards),
        ]

        if self._prev_generation:
            cmd.extend([
                '--prev-generation', self._prev_generation,
            ])

        tovar_tree = self.resources.get('tovar_tree_pb')
        if tovar_tree:
            cmd += ['--tovar-tree-path', os.path.join(self.input_dir, tovar_tree.filename)]

        glue_config = self.resources.get('glue_config')
        if glue_config:
            cmd += ['--glue-config-path', os.path.join(self.input_dir, glue_config.path)]

        if 'eats_and_lavka_shops_dat' in self.resources:
            cmd.extend([
                '--eats-lavka-shopsdat-path', self.resources['eats_and_lavka_shops_dat'].path
            ])

        cmd.extend(self.resources['command-arguments'].argument_list())
        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=True
        )

        home_dir = self.resources['config'].options['yt']['home_dir']
        offers_raw_path = ypath_join(home_dir, 'mi3', self._mi3_type, self.generation,  'work', 'offers_raw')
        blue_offers_raw_path = ypath_join(home_dir, 'mi3', self._mi3_type, self.generation,  'work', 'blue_offers_raw')

        self.outputs['offers_raw'] = OffersRawTable(self.yt_stuff, offers_raw_path)
        self.outputs['blue_offers_raw'] = BlueOffersRawTable(self.yt_stuff, blue_offers_raw_path)
        for feedssessions_type in ('all', 'offers', 'promo', 'stored'):
            feedssessions_name = 'feedssessions.{}'.format(feedssessions_type)
            self.outputs[feedssessions_name] = FeedSessionsFile(feedssessions_name)
            self.outputs[feedssessions_name].load(os.path.join(
                self.output_generation_dir, feedssessions_name
            ))

        self.outputs['shopsdat_generated_path'] = os.path.join(self.output_generation_dir, 'shops-utf8.dat.report.generated')
        self.outputs['feedlog_main'] = FeedLog(os.path.join(self.output_generation_dir, 'feedlog.main.pbuf.sn'))
        self.outputs['delivery_feed_info'] = DeliveryFeedInfo(os.path.join(self.output_generation_dir, 'delivery', 'feed_info.pbuf.sn'))
        self.outputs['delivery_buckets'] = DeliveryBucketsCSV(os.path.join(self.output_generation_dir, 'delivery', 'delivery_buckets.csv'))

        datacamp_offer_meta_info_path = ypath_join(home_dir, 'mi3', self._mi3_type, self.generation, 'work', 'blue_datacamp_offer_meta_info')
        self.outputs['datacamp_offer_meta_info'] = DatacampOfferMetaInfoTable(self.yt_stuff, datacamp_offer_meta_info_path)

        push_partners_meta_info_path = ypath_join(home_dir, 'mi3', self._mi3_type, self.generation, 'work', 'blue_push_partners_meta_info')
        self.outputs['push_partners_meta_info'] = PushPartnersMetaInfoTable(self.yt_stuff, push_partners_meta_info_path)

        market_joined_normalized_table_path = ypath_join(home_dir, 'mi3', self._mi3_type, self.generation, 'market_joined_normalized')
        self.outputs['market_joined_normalized'] = MarketJoinedNormalizedTable(self.yt_stuff, market_joined_normalized_table_path)

        hosts_path = ypath_join(home_dir, 'mi3', self._mi3_type, self.generation, 'hosts')
        self.outputs['hosts'] = HostsTable(self.yt_stuff, hosts_path)

    @property
    def offers_raw(self):
        if not self.outputs['offers_raw'].data:
            self.outputs['offers_raw'].load()
        return self.outputs['offers_raw']

    @property
    def offers_raw_corrected(self):
        """ Offers from offers_raw with parsed 'offer' protobuf column """
        return [corrected_offer(row) for row in self.offers_raw.data]

    @property
    def blue_offers_raw(self):
        if not self.outputs['blue_offers_raw'].data:
            self.outputs['blue_offers_raw'].load()
        return self.outputs['blue_offers_raw']

    @property
    def blue_offers_raw_corrected(self):
        """ Offers from blue_offers_raw with parsed 'offer' protobuf column """
        return [corrected_offer(row) for row in self.blue_offers_raw.data]

    @property
    def shopsdat_generated_path(self):
        return self.outputs['shopsdat_generated_path']

    @property
    def feedlog_main(self):
        return self.outputs['feedlog_main']

    @property
    def delivery_feed_info(self):
        return self.outputs['delivery_feed_info']

    @property
    def delivery_buckets(self):
        return self.outputs['delivery_buckets']

    @property
    def market_joined_normalized(self):
        if not self.outputs['market_joined_normalized'].data:
            self.outputs['market_joined_normalized'].load()
        return self.outputs['market_joined_normalized']

    @property
    def hosts(self):
        if not self.outputs['hosts'].data:
            self.outputs['hosts'].load()
        return self.outputs['hosts']

    @property
    def datacamp_offer_meta_info_table(self):
        if not self.outputs['datacamp_offer_meta_info'].data:
            self.outputs['datacamp_offer_meta_info'].load()
        return self.outputs['datacamp_offer_meta_info']

    @property
    def push_partners_meta_info_table(self):
        if not self.outputs['push_partners_meta_info'].data:
            self.outputs['push_partners_meta_info'].load()
        return self.outputs['push_partners_meta_info']
