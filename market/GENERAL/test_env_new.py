# coding: utf-8

import logging
import os
import requests
import warnings

from market.idx.yatf.common import get_binary_path

from market.library.common_proxy.yatf.test_envs.test_env import CommonProxyTestEnv

from market.idx.datacamp.yatf.resources.sync_replica_provider_mock import SyncReplicaProviderMock
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampCategoriesTable,
    DataCampFreshBlueOffersTable,
    DataCampFreshWhiteOffersTable,
    DataCampOffersBlogTable,
    DataCampPartnersTable,
    DataCampPromoTable,
    DataCampServiceOffersTable,
    DataCampBasicSearchOffersTable,
    DataCampServiceSearchOffersTable,
    DataCampActualServiceSearchOffersTable,
    DataCampBusinessStatusTable,
    DataCampMskuTable,
)

logger = logging.getLogger()


class PiperTestEnv(CommonProxyTestEnv):
    def _load_table(self, table_name):
        if table_name not in self.resources:
            warnings.warn(
                'table resource by the name {} was not loaded at PiperTestEnv initialization'.format(table_name)
            )
        self.resources[table_name].load()
        return self.resources[table_name]

    def __init__(self, yt_server, log_broker_stuff, options=dict(), **resources):
        super(PiperTestEnv, self).__init__('piper', PiperTestEnv.piper_bin(), **resources)

        self.config = self.resources['piper_config']
        self.yt_server = yt_server
        self.log_broker_stuff = log_broker_stuff

        self._name_to_constructor = {
            'basic_offers': (lambda path: DataCampBasicOffersTable(yt_server, path)),
            'service_offers': (lambda path: DataCampServiceOffersTable(yt_server, path)),
            'actual_service_offers': (lambda path: DataCampServiceOffersTable(yt_server, path)),
            'partners': (lambda path: DataCampPartnersTable(yt_server, path, [])),
            'blog': (lambda path: DataCampOffersBlogTable(yt_server, path)),
            'categories': (lambda path: DataCampCategoriesTable(yt_server, path)),
            'promo': (lambda path: DataCampPromoTable(yt_server, path)),
            'fresh/white': (lambda path: DataCampFreshWhiteOffersTable(yt_server, path)),
            'fresh/blue': (lambda path: DataCampFreshBlueOffersTable(yt_server, path)),
            'basic_search_offers': (lambda path: DataCampBasicSearchOffersTable(yt_server, path)),
            'service_search_offers': (lambda path: DataCampServiceSearchOffersTable(yt_server, path)),
            'actual_service_search_offers': (lambda path: DataCampActualServiceSearchOffersTable(yt_server, path)),
            'business_status': (lambda path: DataCampBusinessStatusTable(yt_server, path)),
            'datacamp_msku': (lambda path: DataCampMskuTable(yt_server, path)),
        }

        if 'tables' in options:
            for table in options['tables']:
                if table in self._name_to_constructor:
                    self.resources[table] = self._name_to_constructor[table](self.config.yt_table_path(table))

        resources_stubs = {
            'sync_replica_provider': SyncReplicaProviderMock(self.yt_server),
        }

        for name, val in resources_stubs.items():
            if name not in self.resources:
                self.resources[name] = val

        self.datacamp_offers_storage = None

    @property
    def basic_offers_table(self):
        return self._load_table('basic_offers')

    @property
    def service_offers_table(self):
        return self._load_table('service_offers')

    @property
    def actual_service_offers_table(self):
        return self._load_table('actual_service_offers')

    @property
    def offers_table(self):
        return self._load_table('offers')

    @property
    def partners_table(self):
        return self._load_table('partners')

    @property
    def business_status_table(self):
        return self._load_table('business_status')

    @property
    def offers_blog_table(self):
        return self._load_table('blog')

    @property
    def categories_table(self):
        return self._load_table('categories')

    @property
    def promos_table(self):
        return self._load_table('promo')

    @property
    def datacamp_msku_table(self):
        return self._load_table('datacamp_msku')

    @property
    def fresh_white_offers_table(self):
        return self._load_table('fresh/white')

    @property
    def fresh_blue_offers_table(self):
        return self._load_table('fresh/blue')

    @property
    def basic_search_offers_table(self):
        return self._load_table('basic_search_offers')

    @property
    def service_search_offers_table(self):
        return self._load_table('service_search_offers')

    @property
    def actual_service_search_offers_table(self):
        return self._load_table('actual_service_search_offers')

    # The following is the old content of PiperTestEnv

    @property
    def description(self):
        return 'piper_env'

    def _get_processed_count(self, semantic_name):
        url = 'http://localhost:{port}?command=get_info_server'.format(port=self.controller_port)
        result = requests.get(url).json()
        try:
            processor = self.config.get_processor_by_semantic_name(semantic_name)
            return int(result['result']['processors'][processor]['offers_count_count'])
        except KeyError as e:
            print(e)
            return 0

    @staticmethod
    def piper_bin():
        return get_binary_path(os.path.join('market', 'idx', 'datacamp', 'controllers', 'piper', 'bin', 'piper'))

    @property
    def api_data_processed(self):
        return self._get_processed_count('api_data_processor')

    @property
    def united_offers_processed(self):
        return self._get_processed_count('united_updater')

    @property
    def fresh_offers_processed(self):
        return self._get_processed_count('fresh_offers_sender')

    @property
    def subscription_dispatcher_processed(self):
        return self._get_processed_count('subscription_dispatcher')

    @property
    def qoffers_processed(self):
        return self._get_processed_count('qoffers_processor')

    @property
    def promo_processed(self):
        return self._get_processed_count('promo_updater')

    @property
    def categories_processed(self):
        return self._get_processed_count('categories_storage_updater')

    @property
    def msku_processed(self):
        return self._get_processed_count('msku_updater')

    @property
    def quoter_api_processed(self):
        return self._get_processed_count('quoter_api_message')

    @property
    def quoter_datacamp_message_processed(self):
        return self._get_processed_count('quoter_datacamp_message')

    @property
    def quoter_external_message_processed(self):
        return self._get_processed_count('quoter_external_message')

    @property
    def quoter_united_offer_processed(self):
        return self._get_processed_count('quoter_united_offer')
