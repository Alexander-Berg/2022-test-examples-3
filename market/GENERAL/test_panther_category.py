#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    DeliveryBucket,
    HyperCategory,
    HyperCategoryType,
    NewShopRating,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    Shop,
)

from core.matcher import Contains


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.enable_category_panther = True
        cls.reqwizard.on_default_request().respond()

        cls.index.regiontree += [
            Region(
                rid=1,
                name='Москва и Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва'),
                ],
            )
        ]
        cls.index.shops += [
            Shop(
                fesh=1,
                priority_region=213,
                regions=[225],
                name='Московская пепячечная "Доставляем"',
                new_shop_rating=NewShopRating(new_rating_total=3.0),
            ),
            Shop(
                fesh=2,
                priority_region=213,
                regions=[225],
                name='Московская пепячечная "Доставляем"',
                new_shop_rating=NewShopRating(new_rating_total=4.0),
            ),
        ]

        cls.index.outlets += [
            Outlet(point_id=111, fesh=1, region=213),
            Outlet(point_id=222, fesh=2, region=213),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=1001,
                fesh=1,
                options=[PickupOption(outlet_id=111, price=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=1002,
                fesh=2,
                options=[PickupOption(outlet_id=222, price=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=8, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=9, output_type=HyperCategoryType.SIMPLE),
        ]

        cls.index.offers += [
            Offer(title='iphone black', bid=100, fesh=1, hid=8, randx=900, price=60, store=True, pickup_buckets=[1001]),
            Offer(
                title='iphone gold',
                bid=1,
                fesh=2,
                hid=8,
                randx=600,
                price=1000000,
                store=True,
                pickup_buckets=[1002],
            ),
            Offer(title='iphone bronze', fesh=1, hid=8, randx=300, price=1000, store=True, pickup_buckets=[1001]),
        ]

        cls.index.offers += [
            Offer(title='чехол для iphone', fesh=1, hid=9),
            Offer(title='чехол для iphone', fesh=2, hid=9),
            Offer(title='чехол для iphone', fesh=1, hid=9),
        ]

        cls.settings.ignore_qtree_decoding_failed_in_error_log = True

    def test_category_panther_enabled(self):
        '''
        Для бестекстовых запросов с категорией и регионом пантера включается
        '''
        response = self.report.request_json(
            'place=prime&hid=8&rids=225&debug=1&rearr-factors=market_use_category_panther_index=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                    }
                ]
            },
        )
        self.assertFragmentIn(response, {'logicTrace': [Contains("Using category panther index")]})

    def test_category_panther_disabled(self):
        '''
        При отсутствии региона пантера отключается
        '''
        response = self.report.request_json(
            'place=prime&hid=8&debug=1&rearr-factors=market_use_category_panther_index=1'
        )
        self.assertFragmentIn(response, {'logicTrace': [Contains("Disabling Panther")]})

    def test_category_panther_in_shop_collection_only(self):
        '''
        Пантера включена только для офферных коллекций
        '''
        response = self.report.request_json(
            'place=prime&hid=8&rids=225&debug=1&rearr-factors=market_use_category_panther_index=1'
        )
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "report": {
                        "context": {
                            "collections": {
                                "SHOP": {"pron": ["use_category_panther_index"]},
                            }
                        }
                    }
                }
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "debug": {
                    "report": {
                        "context": {
                            "collections": {
                                "BOOK": {"pron": ["use_category_panther_index"]},
                                "MODEL": {"pron": ["use_category_panther_index"]},
                                "*": {"pron": ["use_category_panther_index"]},
                            }
                        }
                    }
                }
            },
        )

    def test_category_panther_offer_top_limit(self):
        '''
        Для офферной коллекции топ=250
        '''
        response = self.report.request_json(
            'place=prime&hid=8&rids=225&debug=1&rearr-factors=market_use_category_panther_index=1'
        )
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "report": {
                        "context": {
                            "collections": {
                                "SHOP": {"pron": ["panther_top_size_=250"]},
                            }
                        }
                    }
                }
            },
        )

    def test_category_panther_offers(self):
        '''
        Проверяем, что на выдаче нужные офферы
        '''
        response = self.report.request_json(
            'place=prime&hid=8&rids=225&debug=1&rearr-factors=market_use_category_panther_index=1'
        )
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 3,
                "results": [
                    {
                        "titles": {"raw": "iphone black"},
                    },
                    {
                        "titles": {"raw": "iphone gold"},
                    },
                    {
                        "titles": {"raw": "iphone bronze"},
                    },
                ],
            },
        )
        response = self.report.request_json(
            'place=prime&hid=9&rids=225&debug=1&rearr-factors=market_use_category_panther_index=1'
        )
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 3,
                "results": [
                    {
                        "titles": {"raw": "чехол для iphone"},
                    },
                    {
                        "titles": {"raw": "чехол для iphone"},
                    },
                    {
                        "titles": {"raw": "чехол для iphone"},
                    },
                ],
            },
        )


if __name__ == '__main__':
    main()
