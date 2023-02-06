#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, MarketSku, Shop
from core.testcase import TestCase, main


blue_offer_1 = BlueOffer(waremd5='BlueOffer1-----------w', fesh=777, price=460)  # без скидки
blue_offer_2 = BlueOffer(waremd5='BlueOffer2-----------w', fesh=777, price=460, price_old=600)  # скидка 23.(3)%
blue_offer_3 = BlueOffer(waremd5='BlueOffer3-----------w', fesh=777, price=600, price_old=1200)  # скидка 50%
blue_offer_4 = BlueOffer(waremd5='BlueOffer4-----------w', fesh=777, price=460, price_old=600)  # 23.(3)%
blue_offer_5 = BlueOffer(waremd5='BlueOffer5-----------w', fesh=777, price=600, price_old=1200)  # 50%


msku_1 = MarketSku(hyperid=1, hid=1, sku=11, blue_offers=[blue_offer_1])
msku_2 = MarketSku(hyperid=2, hid=1, sku=12, blue_offers=[blue_offer_2])
msku_3 = MarketSku(hyperid=3, hid=1, sku=13, blue_offers=[blue_offer_3])
msku_4 = MarketSku(hyperid=4, hid=2, sku=21, blue_offers=[blue_offer_4])
msku_5 = MarketSku(hyperid=5, hid=2, sku=22, blue_offers=[blue_offer_5])


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(fesh=777, priority_region=213),
        ]

        cls.index.mskus += [
            msku_1,
            msku_2,
            msku_3,
            msku_4,
            msku_5,
        ]

    def __test_helper(self, req, offers_before_filter, offers_in_result):
        response = self.report.request_json(req)
        offers_passed = len(offers_in_result)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalModels": offers_passed,
                    "totalOffersBeforeFilters": offers_before_filter,
                    "results": [
                        {
                            "offers": {
                                "items": [
                                    {
                                        "entity": "offer",
                                        "wareId": offer.waremd5,
                                    }
                                ]
                            }
                        }
                        for offer in offers_in_result
                    ],
                }
            },
        )

    def test_no_filter(self):
        '''
        Проверяет, что без фильтра есть все офера
        '''
        self.__test_helper(
            'place=prime&rgb=blue&hid=1',
            3,
            [
                blue_offer_1,
                blue_offer_2,
                blue_offer_3,
            ],
        )

    def test_abs_range_filter(self):
        self.__test_helper(
            'place=prime&rgb=blue&hid=1&discount-abs-from=1&discount-abs-to=10000',
            3,
            [
                # blue_offer_1 отфильтруется т.к. скидка 0 руб
                blue_offer_2,
                blue_offer_3,
            ],
        )

        self.__test_helper(
            'place=prime&rgb=blue&hid=1&discount-abs-from=1&discount-abs-to=2',
            3,
            [
                # все офферы отфильтруются, т.к. в диапазон [1…2] не попадает ни один оффер
            ],
        )

        self.__test_helper(
            'place=prime&rgb=blue&hid=1&discount-abs-from=100&discount-abs-to=200',
            3,
            [
                blue_offer_2,
            ],
        )

        self.__test_helper(
            'place=prime&rgb=blue&hid=1&discount-abs-from=100',
            3,
            [
                blue_offer_2,
                blue_offer_3,
            ],
        )

        self.__test_helper(
            'place=prime&rgb=blue&hid=1&discount-abs-from=400&discount-abs-to=600',
            3,
            [
                blue_offer_3,
            ],
        )

        self.__test_helper(
            'place=prime&rgb=blue&hid=1&discount-abs-to=600',
            3,
            [
                blue_offer_2,
                blue_offer_3,
            ],
        )

        self.__test_helper('place=prime&rgb=blue&hid=1&discount-abs-from=601&discount-abs-to=700', 3, [])

    def test_full_filter1(self):
        '''
        Проверяет, что в диапазоне скидки 0..100 есть все офера для hid 1, кроме офера без скидки
        '''
        self.__test_helper(
            'place=prime&rgb=blue&hid=1&discount-from=0&discount-to=100',
            3,
            [
                blue_offer_2,
                blue_offer_3,
            ],
        )

    def test_full_filter2(self):
        '''
        Проверяет, что в диапазоне скидки 0..100 есть все офера для hid 2
        '''
        self.__test_helper(
            'place=prime&rgb=blue&hid=2&discount-from=0&discount-to=100',
            2,
            [
                blue_offer_4,
                blue_offer_5,
            ],
        )

    def test_left_filter(self):
        '''
        Проверяет, что в диапазоне 0..22 нет скидок
        '''
        self.__test_helper('place=prime&rgb=blue&hid=2&discount-from=0&discount-to=22', 2, [])

    def test_mid_filter(self):
        '''
        Проверяет, что в диапазоне 24..49 одна скидка
        '''
        self.__test_helper(
            'place=prime&rgb=blue&hid=2&discount-from=22&discount-to=49',
            2,
            [
                blue_offer_4,
            ],
        )

    def test_left_filter_2(self):
        '''
        Проверяет, что в диапазоне 50..100 одна скидка
        '''
        self.__test_helper(
            'place=prime&rgb=blue&hid=2&discount-from=50&discount-to=100',
            2,
            [
                blue_offer_5,
            ],
        )

    def test_left_open_filter(self):
        '''
        Проверяет, что в диапазоне 24..* одна скидка
        '''
        self.__test_helper(
            'place=prime&rgb=blue&hid=2&discount-from=24',
            2,
            [
                blue_offer_5,
            ],
        )

    def test_right_open_filter(self):
        '''
        Проверяет, что в диапазоне *..40 одна скидка
        '''
        self.__test_helper(
            'place=prime&rgb=blue&hid=2&discount-to=40',
            2,
            [
                blue_offer_4,
            ],
        )

    def test_filter_output(self):
        response = self.report.request_json('place=prime&rgb=blue&hid=2&discount-from=0&discount-to=100')
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
        req_no_filters = 'place=prime&rgb=blue&hid=1'
        self.assertEqualJsonResponses(req_no_filters, 'place=prime&rgb=blue&hid=1&discount-from=-5&discount-to=22')
        self.assertEqualJsonResponses(req_no_filters, 'place=prime&rgb=blue&hid=1&discount-from=5&discount-to=777')


if __name__ == '__main__':
    main()
