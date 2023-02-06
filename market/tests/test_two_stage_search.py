#!/usr/bin/env python

import unittest
import os
import common

class TestTwoStageSearch(unittest.TestCase):

    def test_generation(self):
        out = common.generate_config('market-report', 'template.cfg', 'msh01ht.yandex.ru.cfg')
        conf = common.parse_config(out)
        yandsearch = common.get_yandsearch(conf)
        self.assertTrue(yandsearch is not None)
        local = yandsearch.find_section('SearchSource', lambda sec: 'basesearch16-0' in sec['CgiSearchPrefix'])
        self.assertTrue(local is not None)
        self.assertTrue('CgiSnippetPrefix' in local)

        basesearch = common.get_basesearch(conf, '16-0')
        self.assertTrue(basesearch is not None)
        self.assertTrue('IndexArchiveMode' in basesearch)
        self.assertEqual(basesearch['IndexArchiveMode'], 'IndexOnly')
