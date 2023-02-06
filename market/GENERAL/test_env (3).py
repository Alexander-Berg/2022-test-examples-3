# coding: utf-8

import logging
import os
import requests

from market.idx.yatf.common import get_binary_path

from market.library.common_proxy.yatf.test_envs.test_env import CommonProxyTestEnv

logger = logging.getLogger()


class PiperTestEnv(CommonProxyTestEnv):

    def __init__(self, **resources):
        super(PiperTestEnv, self).__init__('piper', PiperTestEnv.piper_bin(), **resources)

    @property
    def description(self):
        return 'piper_env'

    def _get_processed_count(self, processor):
        url = 'http://localhost:{port}?command=get_info_server'.format(port=self.controller_port)
        result = requests.get(url).json()
        try:
            return int(result['result']['processors'][processor]['offers_count_count'])
        except KeyError as e:
            print(e)
            return 0

    @staticmethod
    def piper_bin():
        return get_binary_path(os.path.join('market', 'idx', 'datacamp', 'controllers', 'piper', 'bin', 'piper'))

    def api_data_processed(self):
        return self._get_processed_count(self.config.api_data_processor)

    def united_offers_processed(self):
        return self._get_processed_count(self.config.united_updater)

    def subscription_dispatcher_processed(self):
        return self._get_processed_count(self.config._subscription_dispatcher)

    def qoffers_processed(self):
        return self._get_processed_count(self.config.qoffers_processor)

    def promo_processed(self):
        return self._get_processed_count(self.config.promo_updater)

    def categories_processed(self):
        return self._get_processed_count(self.config.categories_storage_updater)

    def msku_processed(self):
        return self._get_processed_count(self.config.msku_updater)

    def quoter_api_processed(self):
        return self._get_processed_count(self.config.quoter_api_message)

    def quoter_datacamp_message_processed(self):
        return self._get_processed_count(self.config.quoter_datacamp_message)

    def quoter_external_message_processed(self):
        return self._get_processed_count(self.config.quoter_external_message)

    def quoter_united_offer_processed(self):
        return self._get_processed_count(self.config.quoter_united_offer)
