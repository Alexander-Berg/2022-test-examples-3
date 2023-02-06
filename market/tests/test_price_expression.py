# -*- coding: utf-8 -*-

import unittest

from market.pylibrary.yatestwrap.yatestwrap import source_path

import market.idx.marketindexer.marketindexer.exchange as marketindexer_exchange
from market.idx.marketindexer.marketindexer.price_expression import convert_price, BadPriceExpression, PriceExpression

exchange = marketindexer_exchange.load(source_path('market/idx/marketindexer/tests/data/currencies.xml'))


def price(expression, currency_to):
    return PriceExpression(expression, exchange).get_price_by_currency(currency_to, exchange)


class TestPriceExpression(unittest.TestCase):
    def test(self):
        self.assertAlmostEqual(price('123 1 0 RUR RUR', 'RUR'), 123)
        self.assertAlmostEqual(price('123 1 0 RUR RUR', 'BYN'), 123 * exchange.get_rate('BYN', 'RUR', 'BYN'))
        self.assertAlmostEqual(price('123 CBRF 0 USD RUR', 'RUR'), 123 * exchange.get_rate('RUR', 'USD', 'RUR'))
        self.assertAlmostEqual(PriceExpression('150.99 NBRB 1.2 EUR UAH', exchange).price, 150.99)
        self.assertAlmostEqual(PriceExpression('0 1 0 RUR RUR', exchange).price, 0)

    def test_error(self):
        self.assertRaises(BadPriceExpression, convert_price, 0, 'USD', 'RUR', exchange)


if __name__ == '__main__':
    unittest.main()
