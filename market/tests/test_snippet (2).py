#!/usr/bin/python

import unittest
import os
import common

class TestSnippetReportConfig(unittest.TestCase):

    def test_generation(self):
        out = common.generate_config('market-snippet-report', 'market-snippet-report-template.cfg', 'msh-off01ht.cfg')
        conf = common.parse_config(out)

    def test_collection_for_checking(self):
        out = common.generate_config('market-snippet-report', 'market-snippet-report-template.cfg', 'msh-off01ht.cfg')
        self.assertTrue('<Collection id="basesearch0-for-checking" autostart="must">' in out)

    def test_model_snipppet_parts_present(self):
        out = common.generate_config('market-snippet-report', 'market-snippet-report-template.cfg', 'msh-off01ht.cfg')
        conf = common.parse_config(out)
        model_sections = conf.find_sections('Collection', lambda sec: 'id="basesearch-model-' in sec.decl)
        self.assertEqual(len(model_sections), 8)
        book_sections = conf.find_sections('Collection', lambda sec: 'id="basesearch-book-' in sec.decl)
        self.assertEqual(len(book_sections), 8)
