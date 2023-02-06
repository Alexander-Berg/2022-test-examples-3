#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Currency, ExchangeRate, Offer, Shop


class T(TestCase):
    @classmethod
    def prepare(cls):
        pass

    # MARKETOUT-11520
    @classmethod
    def prepare_currency_convert(cls):
        cls.index.currencies += [
            Currency(
                name=Currency.USD,
                exchange_rates=[
                    ExchangeRate(to=Currency.RUR, rate=60.0),
                ],
            ),
            Currency(
                name=Currency.KZT,
                exchange_rates=[ExchangeRate(to=Currency.RUR, rate=0.20), ExchangeRate(to=Currency.USD, rate=0.003)],
            ),
        ]

    def test_invalid_user_cgi(self):
        """
        Запрос без параметров ведёт к ошибке
        """
        response = self.report.request_json('place=currency_convert')
        self.assertFragmentIn(response, {"error": {"code": "INVALID_USER_CGI"}})
        self.error_log.expect(code=3043)

        response = self.report.request_json('place=currency_convert&currency-from=RUB')
        self.assertFragmentIn(response, {"error": {"code": "INVALID_USER_CGI"}})
        self.error_log.expect(code=3043)

        response = self.report.request_json('place=currency_convert&currency-to=RUB')
        self.assertFragmentIn(response, {"error": {"code": "INVALID_USER_CGI"}})
        self.error_log.expect(code=3043)

        response = self.report.request_json('place=currency_convert&currency-from=RUB&currency-to=RUB')
        self.assertFragmentIn(response, {"error": {"code": "INVALID_USER_CGI"}})
        self.error_log.expect(code=3043)

        response = self.report.request_json('place=currency_convert&currency-from=RUB&currency-value=1')
        self.assertFragmentIn(response, {"error": {"code": "INVALID_USER_CGI"}})
        self.error_log.expect(code=3043)

        response = self.report.request_json('place=currency_convert&currency-to=RUB&currency-value=1')
        self.assertFragmentIn(response, {"error": {"code": "INVALID_USER_CGI"}})
        self.error_log.expect(code=3043)

        response = self.report.request_json('place=currency_convert&currency-value=1')
        self.assertFragmentIn(response, {"error": {"code": "INVALID_USER_CGI"}})
        self.error_log.expect(code=3043)

    def test_list_banks_1_is_not_invalid_cgi(self):
        """
        Запрос списка банков, но без currency-* параметров не приводит к ошибке
        """
        response = self.report.request_json('place=currency_convert&list-banks=1')
        self.assertFragmentNotIn(response, {"error": {"code": "INVALID_USER_CGI"}})

    def test_list_banks_0_is_invalid_cgi(self):
        """
        Запрос list-banks=0, но без currency-* параметров всё же приводит к ошибке
        """
        response = self.report.request_json('place=currency_convert&list-banks=0')
        self.assertFragmentIn(response, {"error": {"code": "INVALID_USER_CGI"}})
        self.error_log.expect(code=3043)

    def test_invalid_currency(self):
        response = self.report.request_json('place=currency_convert&currency-from=USD&currency-to=XYZ&currency-value=1')
        self.assertFragmentIn(response, {"error": {"code": "INVALID_USER_CGI"}})

        self.error_log.expect(code=3043)

    def test_empty_currency(self):
        response = self.report.request_json(
            'place=currency_convert&currency-from=USD&currency-to=&currency-value=1', strict=False
        )
        self.error_log.expect('Unknown destination currency')
        self.error_log.ignore(code=1001)
        self.assertEqual(500, response.code)

    def test_currency_convert_same_currency_integer(self):
        """
        Конвертация одной и той же валюты, целое значение
        """
        response = self.report.request_json('place=currency_convert&currency-from=RUB&currency-to=RUB&currency-value=1')
        self.assertFragmentIn(
            response,
            {
                "currencyFrom": "RUR",
                "currencyTo": "RUR",
                "value": "1",
                "convertedValue": "1",
                "renderedValue": "1",
                "renderedConvertedValue": "1",
                "bank": "BANK-RUR",
            },
        )

    def test_currency_convert_same_currency_float(self):
        """
        Конвертация одной и той же валюты, нецелое значение
        """
        response = self.report.request_json(
            'place=currency_convert&currency-from=RUB&currency-to=RUB&currency-value=1.23'
        )
        self.assertFragmentIn(
            response,
            {
                "currencyFrom": "RUR",
                "currencyTo": "RUR",
                "value": "1.23",
                "convertedValue": "1.23",
                "renderedValue": "1",
                "renderedConvertedValue": "1",
                "bank": "BANK-RUR",
            },
        )

    def test_currency_usd_to_rub_integer(self):
        """
        Конвертация USD в RUB, целое число
        """
        response = self.report.request_json(
            'place=currency_convert&currency-from=USD&currency-to=RUB&currency-value=100'
        )
        self.assertFragmentIn(
            response,
            {
                "currencyFrom": "USD",
                "currencyTo": "RUR",
                "value": "100",
                "convertedValue": "6000",
                "renderedValue": "100",
                "renderedConvertedValue": "6000",
                "bank": "BANK-RUR",
            },
        )

    def test_currency_usd_to_rub_float(self):
        """
        Конвертация USD в RUB, нецелое число
        """
        response = self.report.request_json(
            'place=currency_convert&currency-from=USD&currency-to=RUB&currency-value=100.12'
        )
        self.assertFragmentIn(
            response,
            {
                "currencyFrom": "USD",
                "currencyTo": "RUR",
                "value": "100.12",
                "convertedValue": "6007.2",
                "renderedValue": "100",
                "renderedConvertedValue": "6007",
                "bank": "BANK-RUR",
            },
        )

    def test_currency_usd_to_rub_high_precision(self):
        """
        Конвертация USD в RUB, нецелое число с большим числом знаков
        """
        response = self.report.request_json(
            'place=currency_convert&currency-from=USD&currency-to=RUB&currency-value=100.1234567'
        )
        self.assertFragmentIn(
            response,
            {
                "currencyFrom": "USD",
                "currencyTo": "RUR",
                "value": "100.1234567",
                "convertedValue": "6007.407402",
                "renderedValue": "100",
                "renderedConvertedValue": "6007",
                "bank": "BANK-RUR",
            },
        )

    def test_currency_usd_to_rub_very_high_precision(self):
        """
        Конвертация USD в RUB, нецелое число с очень большим числом знаков, учтутся только первые 7 с округлением
        """
        response = self.report.request_json(
            'place=currency_convert&currency-from=USD&currency-to=RUB&currency-value=100.123456789'
        )
        self.assertFragmentIn(
            response,
            {
                "currencyFrom": "USD",
                "currencyTo": "RUR",
                "value": "100.1234568",
                "convertedValue": "6007.407408",
                "renderedValue": "100",
                "renderedConvertedValue": "6007",
                "bank": "BANK-RUR",
            },
        )

    def test_list_banks(self):
        """
        Запрос только списка банков.
        Названия банков здесь - особенность формирования их названий в LITE.
        """
        response = self.report.request_json('place=currency_convert&list-banks=1')
        self.assertFragmentIn(response, {"banks": ["BANK-KZT", "BANK-RUR", "BANK-UE", "BANK-USD"]})

    def test_conversion_usd_rub_using_cross_rate_with_another_bank(self):
        """
        Конвертация из USD в RUB с использованием кросс-курса через KZT.
        Должен выдать USD->KZT->RUB: 1/0.003 * 0.20 = 66.6666667
        """
        response = self.report.request_json(
            'place=currency_convert&currency-from=USD&currency-to=RUB&currency-value=1&bank=BANK-KZT'
        )
        self.assertFragmentIn(
            response,
            {
                "currencyFrom": "USD",
                "currencyTo": "RUR",
                "value": "1",
                "convertedValue": "66.6666667",
                "renderedValue": "1",
                "renderedConvertedValue": "67",
            },
        )

    @classmethod
    def prepare_byn_support(cls):
        cls.index.currencies += [
            Currency(
                name=Currency.BYR,
                alias=Currency.BYN,
                rate_to_primary=10000.0,
                exchange_rates=[
                    ExchangeRate(to=Currency.RUR, rate=0.00308248),
                    ExchangeRate(to=Currency.USD, rate=0.0000533192),
                    ExchangeRate(to=Currency.KZT, rate=0.0167790),
                ],
            ),
        ]

        cls.index.shops += [Shop(fesh=1132801, currency=Currency.BYN)]

        cls.index.offers += [Offer(hyperid=1132801, price=10000)]

    def test_byn_to_byr(self):
        """
        1 BYN = 10000 BYR
        """
        response = self.report.request_json('place=currency_convert&currency-from=BYN&currency-to=BYR&currency-value=1')
        self.assertFragmentIn(
            response,
            {
                "currencyFrom": "BYN",
                "currencyTo": "BYR",
                "value": "1",
                "convertedValue": "10000",
                "renderedValue": "1",
                "renderedConvertedValue": "10000",
            },
        )

    def test_byr_to_byn(self):
        """
        1 BYR = 0.0001 BYN
        """
        response = self.report.request_json('place=currency_convert&currency-from=BYR&currency-to=BYN&currency-value=1')
        self.assertFragmentIn(
            response,
            {
                "currencyFrom": "BYR",
                "currencyTo": "BYN",
                "value": "1",
                "convertedValue": "0.0001",
                "renderedValue": "1",
                "renderedConvertedValue": "0",
            },
        )

    def test_price_in_byn(self):
        """
        Test BYN price
        324.41 = 10000 / 0.00308248 / 10000
        """
        response = self.report.request_json('place=productoffers&hyperid=1132801&currency=BYN')
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'model': {'id': 1132801},
                'prices': {
                    'currency': 'BYN',
                    'value': '324.41',
                },
            },
        )

    def test_price_in_byr(self):
        """
        Test BYR price
        3244141 = 10000 / 0.00308248
        """
        response = self.report.request_json('place=productoffers&hyperid=1132801&currency=BYR')
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'model': {'id': 1132801},
                'prices': {
                    'currency': 'BYR',
                    'value': '3244141',
                },
            },
        )

    def test_render_ruble_small(self):
        response = self.report.request_json(
            'place=currency_convert&currency-from=RUR&currency-to=RUR&currency-value=0.51'
        )
        self.assertFragmentIn(
            response,
            {
                "currencyFrom": "RUR",
                "currencyTo": "RUR",
                "value": "0.51",
                "convertedValue": "0.51",
                "renderedValue": "0.51",
                "renderedConvertedValue": "0.51",
            },
        )

    def test_render_ruble_large(self):
        response = self.report.request_json(
            'place=currency_convert&currency-from=RUR&currency-to=RUR&currency-value=1.51'
        )
        self.assertFragmentIn(
            response,
            {
                "currencyFrom": "RUR",
                "currencyTo": "RUR",
                "value": "1.51",
                "convertedValue": "1.51",
                "renderedValue": "2",
                "renderedConvertedValue": "2",
            },
        )

    def test_render_render_new_belarusian_ruble_small(self):
        response = self.report.request_json(
            'place=currency_convert&currency-from=BYN&currency-to=BYN&currency-value=0.51'
        )
        self.assertFragmentIn(
            response,
            {
                "currencyFrom": "BYN",
                "currencyTo": "BYN",
                "value": "0.51",
                "convertedValue": "0.51",
                "renderedValue": "0.51",
                "renderedConvertedValue": "0.51",
            },
        )

    def test_render_render_new_belarusian_ruble_large(self):
        response = self.report.request_json(
            'place=currency_convert&currency-from=BYN&currency-to=BYN&currency-value=1.51'
        )
        self.assertFragmentIn(
            response,
            {
                "currencyFrom": "BYN",
                "currencyTo": "BYN",
                "value": "1.51",
                "convertedValue": "1.51",
                "renderedValue": "1.51",
                "renderedConvertedValue": "1.51",
            },
        )


if __name__ == '__main__':
    main()
