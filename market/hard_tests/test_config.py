# -*- coding: utf-8 -*-
import unittest
import os

from market.idx.marketindexer.marketindexer import miconfig


class TestConfig(unittest.TestCase):
    """It is required to run with IC_CONFIG_PATH=/path/to/test/common.ini/file environment variable"""

    def test_miconfig(self):
        conf = miconfig.default()
        working_dir = conf.get('general', 'working_dir', 'default_bad_value')
        self.assertNotEqual(working_dir, 'default_bad_value')
        self.assertEqual(os.path.basename(working_dir), 'indexer_market')

    def test_no_such_value(self):
        conf = miconfig.default()
        no_such_value = conf.get('ka', 'ka', 'shka')
        self.assertEqual(no_such_value, 'shka')

if '__main__' == __name__:
    unittest.main()
