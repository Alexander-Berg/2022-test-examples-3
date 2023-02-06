#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    ClickType,
    CpaCategory,
    CpaCategoryType,
    DeliveryBucket,
    DeliveryOption,
    MnPlace,
    Model,
    Offer,
    Region,
    RegionalDelivery,
    RegionalModel,
    Shop,
)
from core.testcase import TestCase, main

import math


def sigmoid(x, alpha, beta, gamma):
    return 1.0 + alpha * (1.0 / (1.0 + gamma * math.exp(-beta * x)) - 1.0 / (1.0 + gamma))


def cpc_auction(bid, min_bid):
    alpha = 0.61
    beta = 0.01
    gamma = 2.742
    x = bid - min_bid
    return sigmoid(x, alpha, beta, gamma)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]

        cls.index.regiontree += [
            Region(rid=54, name='Екатеринбург', region_type=Region.CITY),
            Region(
                rid=149,
                name='Беларусь',
                region_type=Region.COUNTRY,
                children=[
                    Region(rid=20729, name='Бобруйск'),
                ],
            ),
            Region(
                rid=187,
                name='Украина',
                region_type=Region.COUNTRY,
                children=[
                    Region(rid=144, name='Львов'),
                ],
            ),
            Region(
                rid=159,
                name='Казахстан',
                region_type=Region.COUNTRY,
                children=[
                    Region(rid=163, name='Астана'),
                ],
            ),
            Region(
                rid=21227,
                name='Сомали',
                region_type=Region.COUNTRY,
                children=[
                    Region(rid=37179, name='Могадишо'),
                ],
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=10001,
                fesh=1,
                regional_options=[
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=54, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=10002,
                fesh=2,
                regional_options=[
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=54, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=10003,
                fesh=3,
                regional_options=[
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=54, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=10004,
                fesh=4,
                regional_options=[
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=54, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=10005,
                fesh=5,
                regional_options=[
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=54, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=10006,
                fesh=6,
                regional_options=[
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=54, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=10007,
                fesh=7,
                regional_options=[
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=54, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=10008,
                fesh=8,
                regional_options=[
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=54, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=10009,
                fesh=9,
                regional_options=[
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=54, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=10010,
                fesh=10,
                regional_options=[
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=54, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=10011,
                fesh=11,
                regional_options=[
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=54, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=10012,
                fesh=12,
                regional_options=[
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=54, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=10013,
                fesh=13,
                regional_options=[
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=54, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=10014,
                fesh=14,
                regional_options=[
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=54, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[225], cpc=Shop.CPC_NO),
            Shop(fesh=2, priority_region=213, regions=[225]),
            Shop(fesh=3, priority_region=213, regions=[225], cpc=Shop.CPC_NO),
            Shop(fesh=4, priority_region=213, regions=[225]),
            Shop(fesh=5, priority_region=213, regions=[225], cpc=Shop.CPC_NO),
            Shop(fesh=6, priority_region=213, regions=[225]),
            Shop(fesh=7, priority_region=213, regions=[225], cpc=Shop.CPC_NO),
            Shop(fesh=8, priority_region=213, regions=[225]),
        ]

        cls.index.models += [
            Model(title="autobroker basic hirel model", ts=1, hid=102, hyperid=700),
            Model(title="autobroker basic lowrel model", ts=2, hid=102, hyperid=900),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=700, rids=[213], offers=1, local_offers=1),
            RegionalModel(hyperid=900, rids=[213], offers=1, local_offers=1),
        ]

        cls.index.cpa_categories += [
            CpaCategory(hid=101, regions=[225], cpa_type=CpaCategoryType.CPC_AND_CPA),
            CpaCategory(hid=90417, regions=[225], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
        ]

        cls.index.offers += [
            Offer(title='formula calculation offer', fesh=1, ts=5, hid=101, picture_flags=1, bid=15, price=8000),
            Offer(title='formula calculation offer', fesh=2, ts=6, hid=101, picture_flags=1, bid=20, price=8000),
            Offer(
                title='formula calculation offer',
                fesh=3,
                ts=7,
                hid=101,
                picture_flags=1,
                bid=25,
                price=8000,
                delivery_buckets=[10003],
            ),
            Offer(
                title='formula calculation offer',
                fesh=4,
                ts=8,
                hid=101,
                picture_flags=1,
                bid=30,
                price=8000,
                delivery_buckets=[10004],
            ),
            Offer(title='formula calculation offer', fesh=5, ts=9, hid=90417, picture_flags=1, bid=15, price=8000),
            Offer(title='formula calculation offer', fesh=6, ts=10, hid=90417, picture_flags=1, bid=20, price=8000),
            Offer(
                title='formula calculation offer',
                fesh=7,
                ts=11,
                hid=90417,
                picture_flags=1,
                bid=25,
                price=8000,
                delivery_buckets=[10007],
            ),
            Offer(
                title='formula calculation offer',
                fesh=8,
                ts=12,
                hid=90417,
                picture_flags=1,
                bid=30,
                price=8000,
                delivery_buckets=[10008],
            ),
        ]

        cls.index.offers += [
            Offer(
                fesh=1,
                title="autobroker cpa cat cpa offer",
                ts=21,
                hid=90417,
                picture_flags=1,
                price=10000,
                bid=20,
                waremd5='xMpCOKC5I4INzFCab3WEmQ',
                delivery_buckets=[10001],
            ),
            Offer(
                fesh=2,
                title="autobroker cpc cat cpa cpc offer",
                ts=22,
                hid=101,
                picture_flags=1,
                price=10000,
                bid=40,
                waremd5='wgrU12_pd1mqJ6DJm_9nEA',
                delivery_buckets=[10002],
            ),
            Offer(
                fesh=3,
                title="autobroker cpc cat cpa offer",
                ts=23,
                hid=101,
                picture_flags=1,
                price=10000,
                bid=20,
                waremd5='EUhIXt-nprRmCEEWR-cysw',
                delivery_buckets=[10003],
            ),
            Offer(
                fesh=4,
                title="autobroker cpc cat cpc offer",
                ts=25,
                hid=101,
                picture_flags=1,
                price=10000,
                bid=90,
                delivery_buckets=[10005],
            ),
            Offer(
                fesh=6,
                title="autobroker cpa cat cpa cpc offer",
                ts=24,
                hid=90417,
                picture_flags=1,
                price=10000,
                bid=700,
                waremd5='8NnShtQIC16NUYI5F9BhGA',
                delivery_buckets=[10004],
            ),
            Offer(
                fesh=8,
                title="autobroker cpa cat cpc offer",
                ts=26,
                hid=90417,
                picture_flags=1,
                price=10000,
                bid=100,
                delivery_buckets=[10006],
            ),
        ]

        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.13)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.011)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 21).respond(0.119)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 22).respond(0.118)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 23).respond(0.117)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 24).respond(0.116)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 25).respond(0.115)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 26).respond(0.01)

    def test_auction_formula_calculation(self):
        # (ts, is_cpc_categ, is_cpc, fee/bid, min_fee/min_bid)
        results = [(6, True, True, 20, 9), (10, False, True, 20, 9)]

        response = self.report.request_json('place=prime&text=formula+calculation&debug-doc-count=60&debug=da&rids=213')

        for i, is_cpc_categ, is_cpc, cost, min_cost in results:
            if (is_cpc_categ and is_cpc) or (not is_cpc_categ):
                mul = cpc_auction(cost, min_cost)
                self.assertFragmentIn(
                    response,
                    {
                        "properties": {
                            "TS": str(i),
                            "BID": str(cost),
                            "MIN_BID": str(min_cost),
                            "DOCUMENT_AUCTION_TYPE": "CPC",
                        },
                        "rank": [{"name": "CPM", "value": str(int(10000 * mul))}],
                    },
                )

    def test_auction_autobroker(self):
        response = self.report.request_json('place=prime&rids=213&text=autobroker&debug=da&show-urls=cpa,external')

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "autobroker cpa cat cpa cpc offer"},
                    },
                    {"titles": {"raw": "autobroker basic hirel model"}},
                    {"titles": {"raw": "autobroker cpc cat cpc offer"}},
                    {
                        "titles": {"raw": "autobroker cpc cat cpa cpc offer"},
                    },
                    {
                        "titles": {"raw": "autobroker cpa cat cpc offer"},
                    },
                    {
                        "titles": {"raw": "autobroker basic lowrel model"},
                    },
                ]
            },
            preserve_order=True,
        )

        self.show_log.expect(title="autobroker basic hirel model", position=2)
        self.show_log.expect(title="autobroker cpc cat cpc offer", position=3, click_price=60, min_bid=13, bid=90)
        self.show_log.expect(title="autobroker cpc cat cpa cpc offer", position=4, click_price=13, min_bid=13, bid=40)
        self.show_log.expect(title="autobroker cpa cat cpa cpc offer", position=1, click_price=97, min_bid=13, bid=700)
        self.show_log.expect(title="autobroker cpa cat cpc offer", position=5, click_price=84, min_bid=13, bid=100)
        self.show_log.expect(title="autobroker basic lowrel model", position=6)

        self.click_log.expect(ClickType.EXTERNAL, cb=90, cp=60, min_bid=13, position=3)
        self.click_log.expect(ClickType.EXTERNAL, cb=40, cp=13, min_bid=13, position=4)
        self.click_log.expect(ClickType.EXTERNAL, cb=700, cp=97, min_bid=13, position=1)
        self.click_log.expect(ClickType.EXTERNAL, cb=100, cp=84, min_bid=13, position=5)


if __name__ == '__main__':
    main()
