#!/usr/bin/python
# -*- coding: utf-8 -*-

import unittest

from market.pylibrary.yatestwrap.yatestwrap import source_path

import market.pylibrary.database as database


class TestLoadDatasources(unittest.TestCase):
    def test(self):
        ds = database.load_datasources_from_config(source_path('market/idx/marketindexer/tests/datasources.conf'))
        self.assertEqual(len(ds), 3)
        self.assertEqual(ds['worker']['hosts'], ['aida'])
        self.assertEqual(ds['worker']['port'], 3306)
        self.assertEqual(ds['worker']['user'], 'market')
        self.assertEqual(ds['worker']['passwd'], '')
        self.assertEqual(ds['worker']['db'], 'marketindexer2')


if __name__ == '__main__':
    unittest.main()
