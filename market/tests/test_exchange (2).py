# -*- coding: utf-8 -*-

import unittest

from market.pylibrary.yatestwrap.yatestwrap import source_path

from market.idx.snippets.src import exchange


class Test(unittest.TestCase):
    def setUp(self):
        self.exchange = exchange.load(source_path('market/idx/snippets/test-data/input/currencies.xml'))

    def test(self):
        # курс рубль - рубль всегда 1
        self.assertAlmostEqual(self.exchange.get_rate('CBRF', 'RUR', 'RUR'), 1)
        # можно получить курс как по имени банка, так и по имени валюты
        self.assertAlmostEqual(self.exchange.get_rate('CBRF', 'USD', 'RUR'), self.exchange.get_rate('RUR', 'USD', 'RUR'))
        # алиас
        self.assertAlmostEqual(self.exchange.get_rate('CBRF', 'USD', 'RUR'), self.exchange.get_rate('CBRF', 'USD', 'RUB'))
