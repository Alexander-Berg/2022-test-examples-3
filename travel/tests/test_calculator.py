import unittest
from travel.avia.analytics.price_changes.python.lib.answer_calculator import AnswerCalculator


class TestItem(unittest.TestCase):
    def test_calc(self):
        calc = AnswerCalculator(price_column='price')
        records = [
            {'unixtime': 1, 'price': 1},
            {'unixtime': 2, 'price': 2},
            {'unixtime': 3, 'price': 4},
        ]

        expectd = [
            {'unixtime': 1, 'price': 1, 'answer': 2},
            {'unixtime': 2, 'price': 2, 'answer': 1},
            {'unixtime': 3, 'price': 4},
        ]

        result = calc('', records)
        for x, y in zip(result, expectd):
            self.assertEqual(x, y)
