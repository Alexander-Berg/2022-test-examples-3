# -*- coding: utf-8 -*-

import unittest
import os

from market.pylibrary.yatestwrap.yatestwrap import source_path

from market.idx.datacamp.parser.lib.parser_engine.feed_processor import calculate_contents_hash


yml_dir = source_path('market/idx/datacamp/parser/tests/parser_engine/big_tests/yml')
# файлы, отличающиеся только датой в /yml_catalog[date]
yml_a1 = os.path.join(yml_dir, 'yml-a1.xml')  # hash: 3510927ad70ad07baee986d7c22991d1
yml_a2 = os.path.join(yml_dir, 'yml-a2.xml')  # hash: 3510927ad70ad07baee986d7c22991d1
# файл, отличающийся от a1 только последним офером
yml_b = os.path.join(yml_dir, 'yml-b.xml')  # hash: 2bb38a13529a779aa84a8f48eeed91f3
# разные не-yml файлы
csv_a = os.path.join(yml_dir, 'csv-a.csv')
csv_b = os.path.join(yml_dir, 'csv-b.csv')


class TestContentsHash(unittest.TestCase):
    def test_different_dates(self):
        self.assertEqual(calculate_contents_hash(yml_a1), calculate_contents_hash(yml_a2))

    def test_different_contents(self):
        self.assertNotEqual(calculate_contents_hash(yml_a1), calculate_contents_hash(yml_b))

    def test_non_yml(self):
        self.assertNotEqual(calculate_contents_hash(csv_a), calculate_contents_hash(csv_b))


if '__main__' == __name__:
    unittest.main()
