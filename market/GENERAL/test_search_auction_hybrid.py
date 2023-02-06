#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    ClickType,
    CpaCategory,
    CpaCategoryType,
    DeliveryBucket,
    DeliveryOption,
    GLParam,
    GLType,
    HyperCategory,
    MnPlace,
    Model,
    Offer,
    Region,
    RegionalDelivery,
    RegionalModel,
    Shop,
)
from core.testcase import TestCase, main
from core.matcher import Contains, NoKey

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
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

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
            DeliveryBucket(
                bucket_id=10234,
                fesh=234,
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=10235,
                fesh=235,
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[225]),
            Shop(fesh=2, priority_region=213, regions=[225]),
            Shop(fesh=3, priority_region=213, regions=[225]),
            Shop(fesh=4, priority_region=213, regions=[225]),
            Shop(fesh=5, priority_region=213, regions=[225]),
            Shop(fesh=6, priority_region=213, regions=[225]),
            Shop(fesh=7, priority_region=213, regions=[225]),
            Shop(fesh=8, priority_region=213, regions=[225]),
            Shop(fesh=9, priority_region=213, regions=[225]),
            Shop(fesh=10, priority_region=213, regions=[225]),
            Shop(fesh=11, priority_region=213, regions=[225]),
            Shop(fesh=12, priority_region=213, regions=[225]),
            Shop(fesh=13, priority_region=213, regions=[225]),
            Shop(fesh=14, priority_region=213, regions=[225]),
            Shop(fesh=123, priority_region=213),
            Shop(fesh=124, priority_region=213),
            Shop(fesh=234, priority_region=2, regions=[213]),
            Shop(fesh=235, priority_region=2, regions=[213]),
        ]

        cls.index.models += [
            Model(title="autobroker basic hirel model", ts=1, hid=102, hyperid=700),
            Model(title="autobroker basic lowrel model", ts=2, hid=102, hyperid=900),
            Model(title='formula calculation model', ts=3, hid=101, hyperid=500),
            Model(title='formula calculation model', ts=4, hid=90417, hyperid=600),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=700, rids=[213], offers=1, local_offers=1),
            RegionalModel(hyperid=900, rids=[213], offers=1, local_offers=1),
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=200,
                children=[
                    HyperCategory(hid=201),
                    HyperCategory(hid=202),
                    HyperCategory(hid=203),
                    HyperCategory(
                        hid=204,
                        children=[
                            HyperCategory(hid=241),
                        ],
                    ),
                    HyperCategory(
                        hid=205,
                        children=[
                            HyperCategory(hid=251),
                        ],
                    ),
                    HyperCategory(
                        hid=206,
                        children=[
                            HyperCategory(hid=261),
                        ],
                    ),
                    HyperCategory(hid=207),
                    HyperCategory(
                        hid=208,
                        children=[
                            HyperCategory(hid=281),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.cpa_categories += [
            CpaCategory(hid=101, regions=[225], cpa_type=CpaCategoryType.CPC_AND_CPA),
            CpaCategory(hid=102, regions=[225], cpa_type=CpaCategoryType.CPC_AND_CPA),
            CpaCategory(hid=103, regions=[225], cpa_type=CpaCategoryType.CPC_AND_CPA),
            CpaCategory(hid=200, regions=[225], cpa_type=CpaCategoryType.CPC_AND_CPA),
            CpaCategory(hid=201, regions=[213], cpa_type=CpaCategoryType.CPC_AND_CPA),
            CpaCategory(hid=202, regions=[213], cpa_type=CpaCategoryType.CPC_AND_CPA),
            CpaCategory(hid=203, regions=[213], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
            CpaCategory(hid=204, regions=[213], cpa_type=CpaCategoryType.CPC_AND_CPA),
            CpaCategory(hid=205, regions=[213], cpa_type=CpaCategoryType.CPC_AND_CPA),
            CpaCategory(hid=206, regions=[213], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
            CpaCategory(hid=207, regions=[2], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
            CpaCategory(hid=208, regions=[2], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
            CpaCategory(hid=90417, regions=[225], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
            CpaCategory(hid=90433, regions=[225], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
            CpaCategory(hid=8518088, regions=[225], cpa_type=CpaCategoryType.CPA_NON_GURU),
        ]

        cls.index.offers += [
            Offer(
                title='auction type offer',
                ts=200,
                fesh=1,
                hid=200,
                picture_flags=1,
                bid=30,
                price=8000,
                delivery_buckets=[10001],
            ),
            Offer(
                title='auction type offer',
                ts=201,
                fesh=2,
                hid=201,
                picture_flags=1,
                bid=30,
                price=8000,
                delivery_buckets=[10002],
            ),
            Offer(
                title='auction type offer',
                ts=202,
                fesh=3,
                hid=202,
                picture_flags=1,
                bid=30,
                price=8000,
                delivery_buckets=[10003],
            ),
            Offer(
                title='auction type offer',
                ts=203,
                fesh=4,
                hid=203,
                picture_flags=1,
                bid=30,
                price=8000,
                delivery_buckets=[10004],
            ),
            Offer(
                title='auction type offer',
                ts=204,
                fesh=5,
                hid=204,
                picture_flags=1,
                bid=30,
                price=8000,
                delivery_buckets=[10005],
            ),
            Offer(
                title='auction type offer',
                ts=205,
                fesh=6,
                hid=205,
                picture_flags=1,
                bid=30,
                price=8000,
                delivery_buckets=[10006],
            ),
            Offer(
                title='auction type offer',
                ts=206,
                fesh=7,
                hid=206,
                picture_flags=1,
                bid=30,
                price=8000,
                delivery_buckets=[10007],
            ),
            Offer(
                title='auction type offer',
                ts=207,
                fesh=8,
                hid=241,
                picture_flags=1,
                bid=30,
                price=8000,
                delivery_buckets=[10008],
            ),
            Offer(
                title='auction type offer',
                ts=208,
                fesh=9,
                hid=251,
                picture_flags=1,
                bid=30,
                price=8000,
                delivery_buckets=[10009],
            ),
            Offer(
                title='auction type offer',
                ts=209,
                fesh=10,
                hid=261,
                picture_flags=1,
                bid=30,
                price=8000,
                delivery_buckets=[10010],
            ),
            Offer(
                title='auction type offer',
                ts=210,
                fesh=11,
                hid=207,
                picture_flags=1,
                bid=30,
                price=8000,
                delivery_buckets=[10011],
            ),
            Offer(
                title='auction type offer',
                ts=211,
                fesh=12,
                hid=208,
                picture_flags=1,
                bid=30,
                price=8000,
                delivery_buckets=[10012],
            ),
            Offer(
                title='auction type offer',
                ts=212,
                fesh=13,
                hid=281,
                picture_flags=1,
                bid=30,
                price=8000,
                delivery_buckets=[10013],
            ),
            Offer(
                title='auction type offer',
                ts=213,
                fesh=14,
                hid=8518088,
                picture_flags=1,
                bid=30,
                price=8000,
                delivery_buckets=[10014],
            ),
        ]

        cls.index.offers += [
            Offer(
                title='formula calculation offer',
                fesh=1,
                hyperid=500,
                ts=5,
                hid=101,
                picture_flags=1,
                bid=15,
                price=8000,
            ),
            Offer(
                title='formula calculation offer',
                fesh=2,
                hyperid=500,
                ts=6,
                hid=101,
                picture_flags=1,
                bid=20,
                price=8000,
            ),
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
            Offer(
                title='formula calculation offer',
                fesh=5,
                hyperid=600,
                ts=9,
                hid=90417,
                picture_flags=1,
                bid=15,
                price=8000,
            ),
            Offer(
                title='formula calculation offer',
                fesh=6,
                hyperid=600,
                ts=10,
                hid=90417,
                picture_flags=1,
                bid=20,
                price=8000,
            ),
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
            Offer(title='visual offer', vclusterid=1000000001, ts=13, fesh=1, hid=104, bid=15, price=5000),
            Offer(
                title='visual offer',
                vclusterid=1000000001,
                ts=14,
                fesh=2,
                hid=104,
                bid=20,
                price=5000,
                delivery_buckets=[10002],
            ),
            Offer(
                title='visual offer',
                vclusterid=1000000002,
                ts=15,
                fesh=3,
                hid=90433,
                bid=10,
                price=5000,
                delivery_buckets=[10003],
            ),
            Offer(
                title='visual offer',
                vclusterid=1000000002,
                ts=16,
                fesh=4,
                hid=90433,
                bid=30,
                price=5000,
                delivery_buckets=[10004],
            ),
            Offer(
                title='visual offer',
                vclusterid=1000000003,
                ts=17,
                fesh=6,
                hid=104,
                bid=25,
                price=5000,
                delivery_buckets=[10006],
            ),
            Offer(
                title='visual offer',
                vclusterid=1000000004,
                ts=18,
                fesh=7,
                hid=104,
                bid=35,
                price=5000,
                delivery_buckets=[10007],
            ),
            Offer(
                title='visual offer',
                vclusterid=1000000005,
                ts=19,
                fesh=8,
                hid=90433,
                bid=40,
                price=5000,
                delivery_buckets=[10008],
            ),
            Offer(title='visual offer', vclusterid=1000000006, ts=20, fesh=5, hid=90433, bid=10, price=5000),
        ]

        cls.index.offers += [
            Offer(
                fesh=1,
                title="autobroker basic cpa offer 500 fee",
                ts=21,
                hid=90417,
                picture_flags=1,
                price=10000,
                bid=20,
                waremd5='xMpCOKC5I4INzFCab3WEmQ',
                delivery_buckets=[10001],
                randx=40,
            ),
            Offer(
                fesh=2,
                title="autobroker basic cpc offer 40 bid",
                ts=22,
                hid=102,
                picture_flags=1,
                price=10000,
                bid=40,
                waremd5='wgrU12_pd1mqJ6DJm_9nEA',
                delivery_buckets=[10002],
                randx=70,
            ),
            Offer(
                fesh=3,
                title="autobroker basic cpc offer 20 bid",
                ts=23,
                hid=102,
                picture_flags=1,
                price=10000,
                bid=20,
                waremd5='EUhIXt-nprRmCEEWR-cysw',
                delivery_buckets=[10003],
                randx=60,
            ),
            Offer(
                fesh=4,
                title="autobroker basic cpa offer 700 bid",
                ts=24,
                hid=90417,
                picture_flags=1,
                price=10000,
                bid=700,
                delivery_buckets=[10004],
                randx=50,
            ),
            Offer(
                fesh=5,
                title="autobroker basic hirel cpc offer",
                ts=25,
                hid=102,
                picture_flags=1,
                price=10000,
                bid=90,
                delivery_buckets=[10005],
                randx=90,
            ),
            Offer(
                fesh=6,
                title="autobroker basic lowrel cpa offer",
                ts=26,
                hid=90417,
                picture_flags=1,
                price=10000,
                bid=100,
                delivery_buckets=[10006],
                randx=20,
            ),
        ]

        cls.index.offers += [
            Offer(
                title="cast-iron cauldrons fee 700", fesh=123, hid=90417, picture_flags=1, bid=10, randx=70, price=10000
            ),
            # local head
            Offer(
                title="cast-iron cauldrons bid 60", fesh=123, hid=103, picture_flags=1, bid=60, randx=90, price=10000
            ),
            # local tail
            Offer(
                title="cast-iron cauldrons fee 550",
                fesh=234,
                hid=90417,
                picture_flags=1,
                bid=25,
                randx=30,
                price=10000,
                delivery_buckets=[10234],
            ),
            # non-local head
            Offer(
                title="cast-iron cauldrons bid 50",
                fesh=234,
                hid=103,
                picture_flags=1,
                bid=50,
                randx=50,
                price=10000,
                delivery_buckets=[10234],
            ),
            # non-local tail
            Offer(
                title="cast-iron cauldrons fee 450", fesh=124, hid=90417, picture_flags=1, bid=45, randx=60, price=10000
            ),
            # local head
            Offer(
                title="cast-iron cauldrons bid 40", fesh=124, hid=103, picture_flags=1, bid=40, randx=80, price=10000
            ),
            # local tail
            Offer(
                title="cast-iron cauldrons bid 35",
                fesh=235,
                hid=103,
                picture_flags=1,
                bid=35,
                randx=40,
                price=10000,
                delivery_buckets=[10235],
            ),
            # non-local head
            Offer(
                title="cast-iron cauldrons fee 305",
                fesh=235,
                hid=90417,
                picture_flags=1,
                bid=75,
                randx=20,
                price=10000,
                delivery_buckets=[10235],
            ),
            # non-local tail
        ]

        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.2)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.011)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 25).respond(0.19)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 26).respond(0.01)

    def test_auction_type_selection(self):
        cpc_ts = [200, 201, 202, 204, 205, 207, 208, 210, 211, 212]

        for region in ['213', '2', '54']:
            response = self.report.request_json(
                'place=prime&text=auction+type+offer&rids={0}&debug=da&debug-doc-count=100&numdoc=15'
                '&rearr-factors=market_force_search_auction=Hybrid'.format(region)
            )

            for ts in cpc_ts:
                self.assertFragmentIn(
                    response,
                    {
                        "properties": {"TS": str(ts), "BID": "30", "MIN_BID": "9", "DOCUMENT_AUCTION_TYPE": "CPC"},
                    },
                )

        # (ts, document_auction_type)
        results = [
            (7, 'CPC'),
            (8, 'CPC'),
            (3, 'MODEL_VENDOR'),
            (4, 'MODEL_VENDOR'),
            (5, None),
            (6, None),
            (9, None),
            (11, 'CPA'),
            (12, 'CPA'),  # почему cpc документы имеют cpa-аукцион?
        ]

        response = self.report.request_json(
            'place=prime&text=formula+calculation&debug-doc-count=60&debug=da&rids=213&rearr-factors=market_force_search_auction=Hybrid'
        )

        for i, auction in results:
            self.assertFragmentIn(
                response,
                {
                    "properties": {
                        "TS": str(i),
                        "DOCUMENT_AUCTION_TYPE": auction if auction else NoKey('DOCUMENT_AUCTION_TYPE'),
                    }
                },
            )

    def test_auction_formula_tweaks(self):
        # (ts, bid, min_bid)
        results = [(5, 15, 11), (6, 20, 11)]

        response = self.report.request_json(
            'place=prime&text=formula+calculation&rids=213&debug=da&rearr-factors=market_force_search_auction=Hybrid;'
            'market_tweak_search_auction_params=0.13,0.1,6;market_tweak_search_auction_cpa_params=0.43,0.3,4'
            ';market_disable_auction_for_offers_with_model=0&debug-doc-count=60'
        )
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(
                        "Using search auction with parameters:",
                        "cpaAlpha=0.43 cpaBeta=0.3 cpaGamma=4",
                        "cpcAlpha=0.13 cpcBeta=0.1 cpcGamma=6",
                        "auction type: 2",
                    )
                ]
            },
        )
        for i, cost, min_cost in results:
            mul = sigmoid(cost - min_cost, 0.13, 0.1, 6)
            self.assertFragmentIn(
                response,
                {
                    "properties": {"TS": str(i), "BID": str(cost), "MIN_BID": str(min_cost)},
                    "rank": [{"name": "CPM", "value": str(int(10000 * mul))}],
                },
            )

    def test_search_auction_countries(self):
        cpc_regions = ['20729', '163']
        no_auction_regions = ['144', '37179']

        for region in cpc_regions:
            response = self.report.request_json(
                'place=prime&text=formula+calculation&rids={0}&debug=da&rearr-factors=market_force_search_auction=Hybrid;'
                'market_tweak_search_auction_params=0.13,0.1,6;market_tweak_search_auction_cpa_params=0.43,0.3,4'.format(
                    region
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "logicTrace": [
                        Contains(
                            "Using search auction with parameters:",
                            "cpaAlpha=0.43 cpaBeta=0.3 cpaGamma=4",
                            "cpcAlpha=0.13 cpcBeta=0.1 cpcGamma=6",
                            "auction type: 0",
                        )
                    ]
                },
            )

        for region in no_auction_regions:
            response = self.report.request_json(
                'place=prime&text=formula+calculation&rids={0}&debug=da&rearr-factors=market_force_search_auction=Hybrid;'
                'market_tweak_search_auction_params=0.13,0.1,6;market_tweak_search_auction_cpa_params=0.43,0.3,4'.format(
                    region
                )
            )
            self.assertFragmentNotIn(response, {"logicTrace": [Contains("Using search auction")]})

    def test_auction_autobroker_basic_prime(self):
        response = self.report.request_json(
            'place=prime&rids=213&text=autobroker+basic&debug=da&show-urls=cpa,external&rearr-factors=market_force_search_auction=Hybrid'
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "autobroker basic hirel cpc offer"}},
                    {"titles": {"raw": "autobroker basic hirel model"}},
                    {
                        "titles": {"raw": "autobroker basic cpc offer 40 bid"},
                    },
                    {
                        "titles": {"raw": "autobroker basic cpc offer 20 bid"},
                    },
                    {"titles": {"raw": "autobroker basic cpa offer 700 bid"}},
                    {
                        "titles": {"raw": "autobroker basic cpa offer 500 fee"},
                    },
                    {"titles": {"raw": "autobroker basic lowrel model"}},
                    {"titles": {"raw": "autobroker basic lowrel cpa offer"}},
                ]
            },
            preserve_order=True,
        )

        self.show_log.expect(title="autobroker basic hirel cpc offer", position=1, click_price=51, min_bid=13, bid=90)
        self.show_log.expect(title="autobroker basic hirel model", position=2)
        self.show_log.expect(title="autobroker basic cpc offer 40 bid", position=3, click_price=21, min_bid=13, bid=40)
        self.show_log.expect(title="autobroker basic cpc offer 20 bid", position=4, click_price=20, min_bid=13, bid=20)
        self.show_log.expect(
            title="autobroker basic cpa offer 700 bid", position=5, click_price=21, min_bid=13, bid=700
        )
        self.show_log.expect(title="autobroker basic cpa offer 500 fee", position=6, click_price=13, min_bid=13, bid=20)
        self.show_log.expect(title="autobroker basic lowrel model", position=7)
        self.show_log.expect(title="autobroker basic lowrel cpa offer", position=8, click_price=13, min_bid=13, bid=100)

        self.click_log.expect(ClickType.EXTERNAL, cb=90, cp=51, min_bid=13, position=1)
        self.click_log.expect(ClickType.EXTERNAL, cb=40, cp=21, min_bid=13, position=3)
        self.click_log.expect(ClickType.EXTERNAL, cb=20, cp=20, min_bid=13, position=4)
        # self.click_log.expect(ClickType.EXTERNAL, cb=20, cp=20, min_bid=13, position=4)
        self.click_log.expect(ClickType.EXTERNAL, cb=20, cp=13, min_bid=13, position=6)
        self.click_log.expect(ClickType.EXTERNAL, cb=100, cp=13, min_bid=13, position=8)

    def test_auction_autobroker_cauldrons_prime(self):
        response = self.report.request_json(
            'place=prime&text=cast-iron cauldrons&rids=213&show-urls=cpa,external&rearr-factors=market_force_search_auction=Hybrid'
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "cast-iron cauldrons bid 60"}},
                    {"titles": {"raw": "cast-iron cauldrons bid 40"}},
                    {"titles": {"raw": "cast-iron cauldrons fee 700"}},
                    {"titles": {"raw": "cast-iron cauldrons fee 450"}},
                    {"titles": {"raw": "cast-iron cauldrons bid 50"}},
                    {"titles": {"raw": "cast-iron cauldrons bid 35"}},
                    {"titles": {"raw": "cast-iron cauldrons fee 550"}},
                    {"titles": {"raw": "cast-iron cauldrons fee 305"}},
                ]
            },
            preserve_order=True,
        )

        self.show_log.expect(title="cast-iron cauldrons bid 60", bid=60, click_price=41, min_bid=13, position=1)
        self.show_log.expect(title="cast-iron cauldrons bid 40", bid=40, click_price=13, min_bid=13, position=2)
        self.show_log.expect(title="cast-iron cauldrons fee 700", bid=13, click_price=13, min_bid=13, position=3)
        self.show_log.expect(title="cast-iron cauldrons fee 450", bid=45, click_price=13, min_bid=13, position=4)
        self.show_log.expect(title="cast-iron cauldrons bid 50", bid=50, click_price=36, min_bid=13, position=5)
        self.show_log.expect(title="cast-iron cauldrons bid 35", bid=35, click_price=13, min_bid=13, position=6)
        self.show_log.expect(title="cast-iron cauldrons fee 550", bid=25, click_price=25, min_bid=13, position=7)
        self.show_log.expect(title="cast-iron cauldrons fee 305", bid=75, click_price=13, min_bid=13, position=8)

        self.click_log.expect(ClickType.EXTERNAL, cb=60, cp=41, min_bid=13, position=1)
        self.click_log.expect(ClickType.EXTERNAL, cb=40, cp=13, min_bid=13, position=2)
        self.click_log.expect(ClickType.EXTERNAL, cb=13, cp=13, min_bid=13, position=3)
        self.click_log.expect(ClickType.EXTERNAL, cb=45, cp=13, min_bid=13, position=4)
        self.click_log.expect(ClickType.EXTERNAL, cb=50, cp=36, min_bid=13, position=5)
        self.click_log.expect(ClickType.EXTERNAL, cb=35, cp=13, min_bid=13, position=6)
        self.click_log.expect(ClickType.EXTERNAL, cb=25, cp=25, min_bid=13, position=7)
        self.click_log.expect(ClickType.EXTERNAL, cb=75, cp=13, min_bid=13, position=8)

    @classmethod
    def prepare_autobroker_in_filtering(cls):
        cls.index.models += [
            Model(title="gl-filtering hirel model", ts=1, hid=102, hyperid=1700),
            Model(title="gl-filtering lowrel model", ts=2, hid=102, hyperid=1900),
        ]

        cls.index.gltypes = [
            GLType(param_id=202, hid=102, gltype=GLType.ENUM, values=list(range(40, 51)), unit_name="ENUM"),
            GLType(param_id=204, hid=102, gltype=GLType.BOOL, unit_name="BOOL"),
            GLType(param_id=211, hid=102, gltype=GLType.NUMERIC, unit_name="NUMERIC"),
        ]

        cls.index.offers += [
            Offer(
                fesh=1,
                title="gl-filtering cpa offer 500 fee",
                ts=21,
                hid=90417,
                picture_flags=1,
                price=10000,
                bid=20,
                glparams=[GLParam(param_id=202, value=40)],
                randx=10,
            ),
            Offer(
                fesh=2,
                title="gl-filtering cpc offer 40 bid",
                ts=22,
                hid=102,
                picture_flags=1,
                price=10000,
                bid=40,
                glparams=[GLParam(param_id=204, value=1)],
            ),
            Offer(
                fesh=3,
                title="gl-filtering cpc offer 20 bid",
                ts=23,
                hid=102,
                picture_flags=1,
                price=10000,
                bid=20,
                glparams=[GLParam(param_id=211, value=0.5)],
            ),
            Offer(
                fesh=4,
                title="gl-filtering cpa offer 700 bid",
                ts=24,
                hid=90417,
                picture_flags=1,
                price=10000,
                bid=700,
                glparams=[GLParam(param_id=202, value=40)],
                randx=11,
            ),
            Offer(
                fesh=5,
                title="gl-filtering hirel cpc offer",
                ts=25,
                hid=102,
                picture_flags=1,
                price=10000,
                bid=90,
                glparams=[GLParam(param_id=204, value=1), GLParam(param_id=211, value=0.5)],
            ),
            Offer(
                fesh=6, title="gl-filtering lowrel cpa offer", ts=26, hid=90417, picture_flags=1, price=10000, bid=100
            ),
        ]

    def test_autobroker_glfiltering_enum_prime(self):
        """
        Проверяется, что в случае gl фильтрации (тип фильтра enum) автоброкер работает для показанных документов
        """
        response = self.report.request_json(
            'place=prime&rids=213&text=gl-filtering&hid=90417&debug=da&show-urls=cpa,external&glfilter=202:40&rearr-factors=market_force_search_auction=Hybrid'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "gl-filtering cpa offer 700 bid"}},
                    {"titles": {"raw": "gl-filtering cpa offer 500 fee"}},
                ]
            },
            preserve_order=True,
        )
        self.show_log.expect(title="gl-filtering cpa offer 700 bid", position=1, click_price=21, min_bid=13, bid=700)
        self.show_log.expect(title="gl-filtering cpa offer 500 fee", position=2, click_price=13, min_bid=13, bid=20)
        self.click_log.expect(ClickType.EXTERNAL, cb=700, cp=21, min_bid=13, position=1)
        self.click_log.expect(ClickType.EXTERNAL, cb=20, cp=13, min_bid=13, position=2)

    def test_autobroker_glfiltering_bool_prime(self):
        """
        Проверяется, что в случае gl фильтрации (тип фильтра bool) автоброкер работает для показанных документов
        """
        response = self.report.request_json(
            'place=prime&rids=213&text=gl-filtering&hid=102&debug=da&show-urls=cpa,external&glfilter=204:1&rearr-factors=market_force_search_auction=Hybrid'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "gl-filtering hirel cpc offer"}},
                    {"titles": {"raw": "gl-filtering cpc offer 40 bid"}},
                ]
            },
            preserve_order=True,
        )
        self.show_log.expect(title="gl-filtering hirel cpc offer", position=1, click_price=13, min_bid=13, bid=90)
        self.show_log.expect(title="gl-filtering cpc offer 40 bid", position=2, click_price=13, min_bid=13, bid=40)
        self.click_log.expect(ClickType.EXTERNAL, cb=90, cp=13, min_bid=13, position=1)
        self.click_log.expect(ClickType.EXTERNAL, cb=40, cp=13, min_bid=13, position=2)

    def test_autobroker_glfiltering_num_prime(self):
        """
        Проверяется, что в случае gl фильтрации (тип фильтра numeric) автоброкер работает для показанных документов
        """
        response = self.report.request_json(
            'place=prime&rids=213&text=gl-filtering&hid=102&debug=da&show-urls=cpa,external&glfilter=211:0,5&rearr-factors=market_force_search_auction=Hybrid'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "gl-filtering hirel cpc offer"}},
                    {"titles": {"raw": "gl-filtering cpc offer 20 bid"}},
                ]
            },
            preserve_order=True,
        )
        self.show_log.expect(title="gl-filtering hirel cpc offer", position=1, click_price=13, min_bid=13, bid=90)
        self.show_log.expect(title="gl-filtering cpc offer 20 bid", position=2, click_price=13, min_bid=13, bid=20)


if __name__ == '__main__':
    main()
