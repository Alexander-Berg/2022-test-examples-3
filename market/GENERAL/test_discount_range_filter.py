#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, Offer, Shop
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.shops += [
            Shop(fesh=1001, priority_region=213),
        ]

        cls.index.models += [
            Model(hyperid=311, hid=1, title='search me one'),
            Model(hyperid=312, hid=2, title='search me two'),
        ]

        cls.index.offers += [
            Offer(offerid='100', hyperid=311, fesh=1001, price=460),  # no discount
            Offer(offerid='101', hyperid=311, fesh=1001, price=460, price_old=600),  # 24% discount
            Offer(offerid='102', hyperid=311, fesh=1001, price=600, price_old=1200),  # 50% discount
            Offer(offerid='201', hyperid=312, fesh=1001, price=460, price_old=600),  # 23% discount
            Offer(offerid='202', hyperid=312, fesh=1001, price=600, price_old=1200),  # 50% discount
        ]

    def test_no_filter(self):
        '''
        Проверяет, что без фильтра есть все офера
        '''
        response = self.report.request_json('place=prime&hid=1')
        self.assertFragmentIn(response, {"search": {"totalOffers": 3, "totalModels": 1, "totalOffersBeforeFilters": 3}})

    def test_abs_range_filter(self):
        response = self.report.request_json('place=prime&hid=1&discount-abs-from=1&discount-abs-to=10000')
        self.assertFragmentIn(response, {"search": {"totalOffers": 2, "totalOffersBeforeFilters": 3}})

        response = self.report.request_json('place=prime&hid=1&discount-abs-from=1&discount-abs-to=2')
        self.assertFragmentIn(response, {"search": {"totalOffers": 0, "totalOffersBeforeFilters": 3}})

        response = self.report.request_json('place=prime&hid=1&discount-abs-from=100&discount-abs-to=200')
        self.assertFragmentIn(response, {"search": {"totalOffers": 1, "totalOffersBeforeFilters": 3}})

        response = self.report.request_json('place=prime&hid=1&discount-abs-from=100')
        self.assertFragmentIn(response, {"search": {"totalOffers": 2, "totalOffersBeforeFilters": 3}})

        response = self.report.request_json('place=prime&hid=1&discount-abs-from=400&discount-abs-to=600')
        self.assertFragmentIn(response, {"search": {"totalOffers": 1, "totalOffersBeforeFilters": 3}})

        response = self.report.request_json('place=prime&hid=1&discount-abs-from=400&discount-abs-to=600')
        self.assertFragmentIn(response, {"search": {"totalOffers": 1, "totalOffersBeforeFilters": 3}})

        response = self.report.request_json('place=prime&hid=1&discount-abs-to=600')
        self.assertFragmentIn(response, {"search": {"totalOffers": 2, "totalOffersBeforeFilters": 3}})

        response = self.report.request_json('place=prime&hid=1&discount-abs-from=601&discount-abs-to=700')
        self.assertFragmentIn(response, {"search": {"totalOffers": 0, "totalOffersBeforeFilters": 3}})

    def test_full_filter1(self):
        '''
        Проверяет, что в диапазоне скидки 0..100 есть все офера для модели 311, кроме офера без скидки
        Также проверяет, что дефолтный офер модели (без скидки) не попадает в выдачу, а значит,
        не попадает и модель
        '''
        response = self.report.request_json('place=prime&hid=1&discount-from=0&discount-to=100')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalOffers": 2,
                    "totalModels": 0,
                    "totalOffersBeforeFilters": 3,
                    "maxDiscountPercent": 50,
                    "results": [
                        {"entity": "offer", "prices": {"discount": {"percent": 50}}},
                        {"entity": "offer", "prices": {"discount": {"percent": 23}}},
                    ],
                }
            },
        )

    def test_full_filter2(self):
        '''
        Проверяет, что в диапазоне скидки 0..100 есть все офера для модели 312
        Также проверяет, что модель попадает, т.к. дефолтный офер модели попадает в диапазон
        '''
        response = self.report.request_json('place=prime&hid=2&discount-from=0&discount-to=100')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalOffers": 2,
                    "totalModels": 1,
                    "totalOffersBeforeFilters": 2,
                    "results": [{"entity": "product", "titles": {"raw": "search me two"}}],
                }
            },
        )

    def test_left_filter(self):
        '''
        Проверяет, что в диапазоне 0..22 нет скидок
        '''
        response = self.report.request_json('place=prime&hid=2&discount-from=0&discount-to=22')
        self.assertFragmentIn(response, {"search": {"totalOffers": 0, "totalModels": 0, "totalOffersBeforeFilters": 2}})

    def test_mid_filter(self):
        '''
        Проверяет, что в диапазоне 24..49 одна скидка
        '''
        response = self.report.request_json('place=prime&hid=2&discount-from=22&discount-to=49')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalOffers": 1,
                    "totalOffersBeforeFilters": 2,
                    "results": [{"entity": "offer", "shop": {"feed": {"offerId": "201"}}}],
                },
            },
        )

    def test_left_filter_2(self):
        '''
        Проверяет, что в диапазоне 50..100 одна скидка
        '''
        response = self.report.request_json('place=prime&hid=2&discount-from=50&discount-to=100')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalOffers": 1,
                    "totalOffersBeforeFilters": 2,
                    "results": [{"entity": "offer", "shop": {"feed": {"offerId": "202"}}}],
                },
            },
        )

    def test_left_open_filter(self):
        '''
        Проверяет, что в диапазоне 24..* одна скидка
        '''
        response = self.report.request_json('place=prime&hid=2&discount-from=24')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalOffers": 1,
                    "totalOffersBeforeFilters": 2,
                    "results": [{"entity": "offer", "shop": {"feed": {"offerId": "202"}}}],
                }
            },
        )

    def test_right_open_filter(self):
        '''
        Проверяет, что в диапазоне *..40 одна скидка
        '''
        response = self.report.request_json('place=prime&hid=2&discount-to=40')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalOffers": 1,
                    "totalOffersBeforeFilters": 2,
                    "results": [{"entity": "offer", "shop": {"feed": {"offerId": "201"}}}],
                }
            },
        )

    def test_filter_output(self):
        response = self.report.request_json('place=prime&hid=2&discount-from=0&discount-to=100')
        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "id": "discount-range",
                        "type": "number",
                        "values": [
                            {"max": "100", "min": "0", "id": "chosen"},
                            {"max": "100", "min": "0", "id": "found"},
                        ],
                    }
                ]
            },
            preserve_order=True,
        )

    def test_bad_filter(self):
        '''
        Проверяет, что значения фильтра вне диапазона [0…100] приводят к несрабатыванию фильтра
        '''
        req_no_filters = 'place=prime&hid=1'
        self.assertEqualJsonResponses(req_no_filters, 'place=prime&hid=1&discount-from=-5&discount-to=22')
        self.assertEqualJsonResponses(req_no_filters, 'place=prime&hid=1&discount-from=5&discount-to=777')


if __name__ == '__main__':
    main()
