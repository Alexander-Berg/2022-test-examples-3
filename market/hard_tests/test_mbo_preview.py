# -*- coding: utf-8 -*-

import os
import unittest

import context

from market.idx.pylibrary.curllib.curl import Dummy
from market.idx.marketindexer.marketindexer import miconfig
from market.idx.marketindexer.marketindexer.mbo_preview.defines import Category
from market.idx.marketindexer.marketindexer.mbo_preview.mbo_cache import MboStorage


class TestMboPreview(unittest.TestCase):
    def test_mbo_storage(self):
        indexer_dir = os.path.join(context.rootdir, 'indexer_dir')
        context.create_workdir_test_environment(indexer_dir)

        cfg = miconfig.default()
        storage = MboStorage(os.path.join(cfg.working_dir,
                                          'mbo-preview',
                                          'cache'),
                             'localhost',
                             curl=Dummy())
        storage.clear()
        storage.link_to(indexer_dir,
                        [Category(category_id=123, timestamp=1479488204),
                         Category(category_id=456, timestamp=1479488504),
                         Category(category_id=789, timestamp=1479488904)])

        self.assertTrue(os.path.exists(os.path.join(indexer_dir, 'models_123.pb')))
        self.assertTrue(os.path.exists(os.path.join(indexer_dir, 'models_456.pb')))
        self.assertTrue(os.path.exists(os.path.join(indexer_dir, 'models_789.pb')))
        self.assertTrue(os.path.exists(os.path.join(indexer_dir, 'parameters_123.pb')))
        self.assertTrue(os.path.exists(os.path.join(indexer_dir, 'parameters_456.pb')))
        self.assertTrue(os.path.exists(os.path.join(indexer_dir, 'parameters_789.pb')))


if '__main__' == __name__:
    unittest.main()
