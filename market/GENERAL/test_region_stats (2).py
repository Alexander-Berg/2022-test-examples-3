#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Currency, ExchangeRate, Model, Offer, Region, RegionalModel, Shop
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.regiontree = [
            Region(rid=149, name='Беларусь'),
        ]

        cls.index.regional_models += [
            RegionalModel(
                hyperid=210,
                rids=[225],
                offers=10,
                price_min=1000,
                price_max=2000,
                price_med=1500,
                price_old_min=1101,
                max_discount=10,
                dccount=2,
            ),
        ]

        cls.index.currencies = [
            Currency(
                'RUR',
                exchange_rates=[
                    ExchangeRate(fr=Currency.BYR, rate=0.00378),
                    ExchangeRate(fr=Currency.KZT, rate=0.21),
                ],
            ),
            Currency(
                'BYR',
                exchange_rates=[
                    ExchangeRate(fr=Currency.RUR, rate=263.36),
                    ExchangeRate(fr=Currency.KZT, rate=59),
                ],
            ),
            Currency(
                'KZT',
                exchange_rates=[
                    ExchangeRate(fr=Currency.RUR, rate=5),
                    ExchangeRate(fr=Currency.BYR, rate=0.017),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=100, priority_region=225),
            Shop(fesh=101, priority_region=149, currency=Currency.BYR),
        ]

        cls.index.models += [
            Model(hyperid=210, hid=100),
            Model(hyperid=211, hid=101),
        ]

        cls.index.offers += [
            Offer(hyperid=210, fesh=100, price=1011),
            Offer(hyperid=210, fesh=100, price=2100),
            Offer(hyperid=211, fesh=100, price=1000),  # Price in RUR
            Offer(hyperid=211, fesh=101, price=200000, price_old=203000),  # Price in BYR
        ]

    def test_model_prices_RUR(self):
        response = self.report.request_json('place=prime&hid=100&rids=225')
        self.assertFragmentIn(
            response, {"type": "model", "prices": {"min": "1011", "max": "2100", "currency": "RUR", "avg": "1500"}}
        )

    def test_model_prices_BYR(self):
        response = self.report.request_json('place=prime&hid=100&rids=225&currency=BYR')
        self.assertFragmentIn(
            response,
            {"type": "model", "prices": {"min": "266257", "max": "553056", "currency": "BYR", "avg": "396825"}},
        )

    def test_model_prices_KZT(self):
        response = self.report.request_json('place=prime&hid=100&rids=225&currency=KZT')
        self.assertFragmentIn(
            response, {"type": "model", "prices": {"min": "5055", "max": "10500", "currency": "KZT", "avg": "7143"}}
        )

    def test_unknown_currency(self):
        response = self.report.request_xml('place=prime&hid=100&rids=225&currency=USD', strict=False)
        self.assertTrue(response.code == 500)
        self.assertFragmentIn(response, '<error code="1011">.* unsupported currency: USD</error>', use_regex=True)
        self.error_log.ignore('unsupported currency: USD')


if __name__ == '__main__':
    main()
