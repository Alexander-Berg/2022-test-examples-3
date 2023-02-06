# -*- coding: utf-8 -*-

import unittest

from market.pylibrary.yatestwrap.yatestwrap import source_path

from market.idx.snippets.src import exchange
from market.idx.snippets.src import price_expression
from market.idx.snippets.src.price_expression import convert_price, BadPriceExpression

exchange = exchange.load(source_path('market/idx/snippets/test-data/input/currencies.xml'))


def price(expression, currency_to):
    return price_expression.PriceExpression(expression, exchange).get_price_by_currency(currency_to, exchange)


class TestPriceExpression(unittest.TestCase):
    def test(self):
        self.assertAlmostEqual(price('123 1 0 RUR RUR', 'RUR'), 123)
        self.assertAlmostEqual(price('123 1 0 RUR RUR', 'BYN'), 123 * exchange.get_rate('BYN', 'RUR', 'BYN'))
        self.assertAlmostEqual(price('123 CBRF 0 USD RUR', 'RUR'), 123 * exchange.get_rate('RUR', 'USD', 'RUR'))
        self.assertAlmostEqual(price_expression.PriceExpression('150.99 NBRB 1.2 EUR UAH', exchange).price, 150.99)
        self.assertAlmostEqual(price_expression.PriceExpression('0 1 0 RUR RUR', exchange).price, 0)

    def test_error(self):
        self.assertRaises(BadPriceExpression, convert_price, 0, 'USD', 'RUR', exchange)
