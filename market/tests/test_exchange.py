# -*- coding: utf-8 -*-

import unittest

from market.pylibrary.yatestwrap.yatestwrap import source_path

import market.idx.marketindexer.marketindexer.exchange as marketindexer_exchange


class Test(unittest.TestCase):
    def setUp(self):
        self.exchange = marketindexer_exchange.load(source_path('market/idx/marketindexer/tests/data/currencies.xml'))

    def test(self):
        # курс рубль - рубль всегда 1
        self.assertAlmostEqual(self.exchange.get_rate('CBRF', 'RUR', 'RUR'), 1)
        # можно получить курс как по имени банка, так и по имени валюты
        self.assertAlmostEqual(self.exchange.get_rate('CBRF', 'USD', 'RUR'), self.exchange.get_rate('RUR', 'USD', 'RUR'))
        # алиас
        self.assertAlmostEqual(self.exchange.get_rate('CBRF', 'USD', 'RUR'), self.exchange.get_rate('CBRF', 'USD', 'RUB'))

if __name__ == '__main__':
    unittest.main()
