#!/usr/bin/python

import unittest
import os
import common

class TestPpcshopConfig(unittest.TestCase):

    def test_ppcsshop_generation(self):
        out = common.generate_config('market-ppcshop-report', 'template.cfg', 'msh01ht.yandex.ru.cfg.no-two-stage')
        conf = common.parse_config(out)
        yandsearch = common.get_yandsearch(conf)
        local = yandsearch.find_section('SearchSource', lambda sec: 'basesearch16-0' in sec['CgiSearchPrefix'])
        self.assertTrue(local is not None)
        self.assertTrue(common.get_cardsearch(conf) is None)

    def test_no_snippets_no_two_stage_query(self):
        out = common.generate_config('market-ppcshop-report', 'template.cfg', 'msh01ht.yandex.ru.cfg')
        conf = common.parse_config(out)
        basesearch = conf.find_section('Collection', lambda sec: 'id="basesearch' in sec.decl)
        self.assertEqual(basesearch['IndexArchiveMode'], 'IndexOnly')
        yandsearch = common.get_yandsearch(conf)
        self.assertTrue(yandsearch is not None)
        self.assertTrue(not 'TwoStepQuery' in yandsearch['MetaSearchOptions'])
        local = yandsearch.find_section('SearchSource', lambda sec: 'basesearch16-0' in sec['CgiSearchPrefix'])
        self.assertTrue(local is not None)
        self.assertTrue(not 'CgiSnippetPrefix' in local)

    def test_wizclient(self):
        out = common.generate_config('market-ppcshop-report', 'template.cfg', 'msh01ht.yandex.ru.cfg',
                                     TARGET_HOST_NAME='mrate01d.market.yandex.net')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertEqual(mr['WizClient'], 'market-mrate')
