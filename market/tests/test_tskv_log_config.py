#!/usr/bin/env python

import unittest
import os
import common

class TestMarketReportConfig(unittest.TestCase):

    def test_main_tskv_config(self):
        out = common.generate_config('market-report', 'template.cfg', 'msh01ht.market.yandex.net.cfg',
                                     TARGET_HOST_NAME='msh01ht.market.yandex.net', ENV_TYPE='production')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertEqual(mr['UseAccessLog'], '1')
        self.assertEqual(mr['UseTskvAccessLog'], '1')
        self.assertEqual(mr['AccessLog'], "/var/log/search/market-access.log")
        self.assertEqual(mr['AccessTskvLog'], "/var/log/search/market-access-tskv.log")

    def test_parallel_tskv_config(self):
        out = common.generate_config('market-parallel-report', 'template.cfg', 'msh01ht.market.yandex.net.cfg',
                                     TARGET_HOST_NAME='msh01ht.market.yandex.net.cfg', ENV_TYPE='production')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertEqual(mr['UseAccessLog'], '0')
        self.assertEqual(mr['UseTskvAccessLog'], '1')
        self.assertEqual(mr['AccessLog'], "/var/log/search/market-parallel-access.log")
        self.assertEqual(mr['AccessTskvLog'], "/var/log/search/market-parallel-access.log") # keep behavior, write logs to old file

    def test_other_tskv_config(self):
        out = common.generate_config('market-mbo-preview-report', 'template.cfg',
                                     'mbo-preview01et.supermarket.yandex.net.cfg',
                                     TARGET_HOST_NAME='mbo-preview01et.supermarket.yandex.net',
                                     ENV_TYPE='production')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertEqual(mr['UseAccessLog'], '1')
        self.assertEqual(mr['UseTskvAccessLog'], '0')
        self.assertEqual(mr['AccessLog'], "/var/log/search/market-mbo-preview-access.log")
        self.assertEqual(mr['AccessTskvLog'], "/var/log/search/market-mbo-preview-access-tskv.log")
