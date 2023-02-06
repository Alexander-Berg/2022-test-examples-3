#!/usr/bin/python

import unittest
import common


class TestMshApiConfig(unittest.TestCase):

    def test_msh_api_generation(self):
        out = common.generate_config(
            'market-report',
            'template.cfg',
            'msh-api01ht.market.yandex.net.cfg',
            TARGET_HOST_NAME='msh-api01ht.market.yandex.net',
            ENV_TYPE='production')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertEqual(mr['WizClient'], 'market-main-report-api')
