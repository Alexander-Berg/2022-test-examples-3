#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    DeliveryBucket,
    DeliveryOption,
    HyperCategory,
    HyperCategoryType,
    MinBidsCategory,
    Offer,
    Region,
    RegionalDelivery,
    Shop,
)
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                tz_offset=10800,
                children=[
                    Region(rid=213, name='Москва', tz_offset=10800),
                    Region(rid=10758, name='Химки', tz_offset=10800),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=1, priority_region=213),
        ]

    @classmethod
    def prepare_output_format(cls):
        """Создаем категорию
        Создаем оффер с определенными полями
        """
        cls.index.hypertree += [HyperCategory(hid=101)]

        cls.index.offers += [
            Offer(
                fesh=1,
                hyperid=11,
                hid=101,
                price=300,
                bid=100,
                title='output_format_test',
                waremd5='ZRK9Q9nKpuAsmQsKgmUtyg',
                delivery_options=[
                    DeliveryOption(price=200, day_from=1, day_to=1),
                ],
            ),
        ]

    def test_miprime_output_format(self):
        """Что тестируем: формат выдачи плейса miprime

        Задаем запрос к плейсу, проверяем формат выдачи
        """
        response = self.report.request_json('place=miprime&rids=213&hyperid=11')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'salesDetected': False,
                    'results': [
                        {
                            'entity': 'offer',
                            'categories': [{'id': 101}],
                            'titles': {'raw': 'output_format_test'},
                            'isRecommendedByVendor': False,
                            'shop': {'id': 1, 'homeRegion': {'id': 225, 'name': 'Россия'}},
                            'delivery': {
                                'shopPriorityRegion': {
                                    'id': 213,
                                },
                                'inStock': False,
                                'isAvailable': False,
                                'price': {'currency': 'RUR', 'value': '200', 'isDeliveryIncluded': False},
                            },
                            'wareId': 'ZRK9Q9nKpuAsmQsKgmUtyg',
                            'model': {'id': 11},
                            'prices': {'currency': 'RUR', 'value': '300', 'rawValue': '300'},
                            'seller': {'price': '300', 'currency': 'RUR', 'sellerToUserExchangeRate': 1},
                            'clickPrice': 100,
                            'bid': 100,
                            'cbid': 100,
                        }
                    ],
                }
            },
        )

    @classmethod
    def prepare_local_delivery_price(cls):
        """Создаем бакет для регионов 213 и 10758
        Создаем оффер с этим бакетом
        """

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=101,
                fesh=1,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=500, day_from=1, day_to=1),
                        ],
                    ),
                    RegionalDelivery(
                        rid=10758,
                        options=[
                            DeliveryOption(price=700, day_from=1, day_to=2),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(fesh=1, hyperid=10, title='local_delivery_price_test', delivery_buckets=[101]),
        ]

    def test_miprime_local_delivery_price(self):
        """Что тестируем: вывод цены локальной доставки в приоритетном и неприоритетном
        регионах в плейсе miprime

        Задаем запрос в разных регионах, проверяем, что в теге <price> внутри <delivery>
        выводится цена доставки в приоритетный регион магазина (213)
        """
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for rids in [213, 10758]:
            response = self.report.request_json('place=miprime&rids={}&hyperid=10'.format(rids) + unified_off_flags)
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 1,
                        'results': [
                            {
                                'entity': 'offer',
                                'delivery': {
                                    'shopPriorityRegion': {
                                        'entity': 'region',
                                        'id': 213,
                                    },
                                    'isAvailable': False,
                                    'price': {'currency': 'RUR', 'value': '500', 'isDeliveryIncluded': False},
                                },
                            }
                        ],
                    }
                },
            )

    @classmethod
    def prepare_miprime_filter_by_feed_category_ids(cls):
        cls.index.offers += [
            Offer(title='feed_category_id_1', fesh=1000, feedid=100, feed_category_id=120),
            Offer(title='feed_category_id_2', fesh=1000, feedid=100, feed_category_id=121),
            Offer(title='feed_category_id_3', fesh=1001, feedid=101, feed_category_id=120),
        ]

    def test_miprime_filter_by_feed_category_ids(self):
        """Тестируем фильтрацию по паре shcatid и feedid"""
        response = self.report.request_json('place=miprime&feedid={}&shcatid={}'.format(100, 120))
        self.assertFragmentIn(
            response,
            {'search': {'total': 1, 'results': [{'entity': 'offer', 'titles': {'raw': 'feed_category_id_1'}}]}},
        )

    @classmethod
    def prepare_miprime_filter_by_model_matching(cls):
        cls.index.offers += [Offer(title='matched', hyperid=210, fesh=100), Offer(title='not_matched', fesh=100)]

    def test_miprime_filter_by_model_matching(self):
        """Тестируем фильтрацию по model-matching"""
        response = self.report.request_json('place=miprime&fesh=100&model-matching=matched')
        self.assertFragmentIn(
            response, {'search': {'total': 1, 'results': [{'entity': 'offer', 'titles': {'raw': 'matched'}}]}}
        )

        response = self.report.request_json('place=miprime&fesh=100&model-matching=notmatched')
        self.assertFragmentIn(
            response, {'search': {'total': 1, 'results': [{'entity': 'offer', 'titles': {'raw': 'not_matched'}}]}}
        )

    @classmethod
    def prepare_original_offer_bids(cls):
        cls.index.hypertree += [HyperCategory(hid=5000, output_type=HyperCategoryType.GURU)]
        cls.index.shops += [Shop(fesh=5000, priority_region=213)]
        cls.index.offers += [
            Offer(hyperid=5001, hid=5000, fesh=5000, price=10000, bid=1, pull_to_min_bid=False),
            Offer(hyperid=5002, hid=5000, fesh=5000, price=10000, bid=1, pull_to_min_bid=True),
        ]

        cls.index.min_bids_category_stats += [
            MinBidsCategory(
                category_id=90401,
                geo_group_id=0,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.05,
            ),
            MinBidsCategory(
                category_id=90401,
                geo_group_id=1,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.05,
            ),
        ]

    def test_original_offer_bids(self):
        """MARKETOUT-20604
        Check if original shop bids are returned if it's forbidden to pull them up
        """

        response = self.report.request_json('place=miprime&rids=213&hyperid=5001&api=partner')
        self.assertFragmentIn(response, {"results": [{"entity": "offer", "cbid": 1, "bid": 1, "clickPrice": 13}]})

        response = self.report.request_json('place=miprime&rids=213&hyperid=5002&api=partner')
        self.assertFragmentIn(response, {"results": [{"entity": "offer", "cbid": 13, "bid": 13, "clickPrice": 13}]})


if __name__ == '__main__':
    main()
