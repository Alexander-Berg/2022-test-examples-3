# coding: utf-8

import logging
import os
import requests

from market.idx.yatf.common import get_binary_path

from market.library.common_proxy.yatf.test_envs.test_env import CommonProxyTestEnv

logger = logging.getLogger()


class DispatcherTestEnv(CommonProxyTestEnv):

    def __init__(self, **resources):
        super(DispatcherTestEnv, self).__init__('dispatcher', DispatcherTestEnv.dispatcher_bin(), **resources)

    @property
    def description(self):
        return 'dispatcher_env'

    def _get_processed_count(self, processor):
        url = 'http://localhost:{port}?command=get_info_server'.format(port=self.controller_port)
        result = requests.get(url).json()
        try:
            return int(result['result']['processors'][processor]['offers_count_count'])
        except KeyError as e:
            print(e)
            return 0

    @staticmethod
    def dispatcher_bin():
        return get_binary_path(os.path.join('market', 'idx', 'datacamp', 'dispatcher', 'bin', 'dispatcher'))

    @property
    def subscription_dispatcher_processed(self):
        return self._get_processed_count(self.config._subscription_dispatcher)
