import unittest

from parameterized import parameterized

from travel.avia.library.python.enum.currency import Currency


class CurrencyTestCase(unittest.TestCase):

    @parameterized.expand(
        [
            ['RUB', Currency.RUB],
            ['RUR', Currency.RUB],
            ['USD', Currency.USD],
            ['UAH', Currency.UAH],
            ['EUR', Currency.EUR],
            ['KZT', Currency.KZT],
            ['BYN', Currency.BYN],
            ['UZS', Currency.UZS],
        ]
    )
    def test_currency(self, code, expected):
        self.assertEqual(expected, Currency.from_str_with_correction(code))

    def test_currency_exception(self):
        with self.assertRaises(Exception) as context:
            Currency.from_str_with_correction('RRR')
        self.assertTrue('is not a valid Currency' in str(context.exception))
