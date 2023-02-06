#!/usr/bin/env python

import unittest
import os
import common

class TestParallelConfig(unittest.TestCase):

    def test_parallel_generation(self):
        out = common.generate_config('market-parallel-report', 'template.cfg', 'msh01ht.yandex.ru.cfg.no-two-stage')
        conf = common.parse_config(out)
        basesearch = conf.find_section('Collection', lambda sec: 'id="basesearch"' in sec.decl)
        self.assertTrue(basesearch is None)
        yandsearch = conf.find_section('Collection', lambda sec: 'id="yandsearch"' in sec.decl)
        local = yandsearch.find_section('SearchSource', lambda sec: 'basesearch16-0' in sec['CgiSearchPrefix'])
        self.assertTrue(local is not None)
        self.assertTrue('CgiSnippetPrefix' not in local)
        self.assertTrue(common.get_cardsearch(conf) is not None)

    def test_separate_models(self):
        out = common.generate_config('market-parallel-report', 'template.cfg', 'msh01ht.yandex.ru.cfg.no-two-stage')
        conf = common.parse_config(out)

        yandsearch = conf.find_section('Collection', lambda sec: 'id="yandsearch"' in sec.decl)
        searchsources = yandsearch.get_sections('SearchSource')
        shopsources = [s for s in searchsources if s['ServerDescr'] == 'SHOP']

        self.assertTrue(len(shopsources))

    def test_default_indexarchive_mode(self):
        out = common.generate_config('market-parallel-report', 'template.cfg', 'msh01ht.yandex.ru.cfg.no-two-stage')
        conf = common.parse_config(out)
        basesearch = common.get_basesearch(conf, '16-0')
        self.assertTrue(basesearch is not None)
        self.assertTrue('IndexArchiveMode' not in basesearch)

    def test_wizclient(self):
        out = common.generate_config('market-parallel-report', 'template.cfg', 'msh01ht.yandex.ru.cfg.no-two-stage',
                                     TARGET_HOST_NAME='msh-par01d.market.yandex.net')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertEqual(mr['WizClient'], 'market-parallel-report')

