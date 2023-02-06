# -*- coding: utf-8 -*-

import six
import unittest

from market.pylibrary.yatestwrap.yatestwrap import source_path

from market.idx.snippets.src import geobase

DATA = '''
1\t0\t3
2\t0\t3
3\t1\t4
'''


class TestGeo(unittest.TestCase):
    def test_load(self):
        geo = geobase.load(six.StringIO(DATA))
        self.assertEqual(geo.get_country_region(3), 1)

    def test_load_real_data(self):
        geo = geobase.load(source_path('market/idx/snippets/test-data/input/geobase.txt'))
        self.assertEqual(geo.get_country_region(geobase.RUSSIA_RID), geobase.RUSSIA_RID)
        self.assertEqual(geo.get_country_region(213), geobase.RUSSIA_RID)
