#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import random

from core.types import (
    Currency,
    HyperCategory,
    HyperCategoryType,
    Model,
    Region,
    Shop,
    Tax,
    Vat,
    YamarecCategoryPartition,
    YamarecPlace,
)
from core.testcase import TestCase, main
from core.types.sku import MarketSku, BlueOffer
from core.bigb import BeruModelOrderLastTimeCounter, BigBKeyword, ModelLastOrderEvent, WeightedValue
from core.matcher import Regex, Contains, NotEmpty, Empty  # noqa

DEFAULT_PROFILE = [
    BigBKeyword(
        id=BigBKeyword.GENDER,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.GENDER_MALE, weight=621947),
            WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=375515),
        ],
    ),
]


class _Offers(object):
    waremd5s = [
        'Sku1Price5-IiLVm1Goleg',
        'Sku2Price50-iLVm1Goleg',
        'Sku3Price45-iLVm1Goleg',
        'Sku4Price36-iLVm1Goleg',
        'Sku5Price15-iLVm1Goleg',
        'Sku5Price16-iLVm1Goleg',
    ]
    feed_ids = [2] * len(waremd5s)
    prices = [500, 50, 45, 36, 15, 16]
    shop_skus = ['Feed_{feedid}_sku{i}'.format(feedid=feedid, i=i + 1) for i, feedid in enumerate(feed_ids)]
    model_ids = list(range(1, len(waremd5s) + 1))
    sku_offers = [
        BlueOffer(price=price, vat=Vat.VAT_10, offerid=shop_sku, feedid=feedid, waremd5=waremd5)
        for feedid, waremd5, price, shop_sku in zip(feed_ids, waremd5s, prices, shop_skus)
    ]


class T(TestCase):
    @classmethod
    def prepare(cls):
        """
        Prepares index.
        Personal and universal categories are from
        recom-commonly-purchased-*-categories.tsv
        """
        cls.settings.rgb_blue_is_cpa = True

        personal_category_filter = [
            90606,
            7683677,
            7683675,
            13462769,
            91330,
            13337703,
            15720046,
            15720050,
            91331,
            15720051,
            15720045,
            91329,
            15720037,
            15720039,
            16147683,
            91382,
            15714713,
            15714708,
            15714698,
            91388,
            14698852,
            15557928,
            15714682,
            15714680,
            15714675,
            15714671,
            15720042,
            91419,
            91420,
            14621180,
            91422,
            91430,
            91397,
            15726404,
            15726402,
            91352,
            15726412,
            15726410,
            15726408,
            15719803,
            15719820,
            15719828,
            15719799,
            91427,
            91344,
            15714127,
            15714122,
            91342,
            15714129,
            91340,
            91343,
            14706137,
            15714542,
            15714113,
            15714106,
            15727944,
            15728039,
            91345,
            15727473,
            15727886,
            15727888,
            15727896,
            15727878,
            15727884,
            15727954,
            91339,
            15727967,
            91421,
            91408,
            15697700,
            13041400,
            15934091,
            16088924,
            15770939,
            4922657,
            15770934,
            13518990,
            14245094,
            12718081,
            15959385,
            15963644,
            13212408,
            15685457,
            15685787,
            13212400,
            12718223,
            15999360,
            15963668,
            15999143,
            12718332,
            12714755,
            12718255,
            12766642,
            12704208,
            15971367,
            12714763,
            12704139,
            15962102,
            818945,
            8480736,
            13277104,
            13277088,
            13277108,
            13277089,
            13276918,
            13276920,
            14995813,
            14995788,
            4748066,
            4748064,
            4748062,
            8480752,
            8510396,
            14996541,
            4748057,
            14996686,
            8480754,
            13276669,
            4748072,
            4748074,
            13276667,
            14996659,
            4748078,
            14994593,
            14990285,
            8480738,
            13244155,
            13239550,
            13240862,
            13239503,
            13239527,
            4854062,
            14993426,
            13239477,
            13239479,
            14993540,
            91184,
            14993483,
            15011042,
            8476101,
            8476102,
            8476110,
            8476103,
            8476097,
            8476098,
            8476539,
            8476100,
            8476099,
            13239041,
            13238924,
            14994948,
            8478954,
            14989778,
            4748058,
            13239135,
            14990252,
            13238994,
            13239089,
            15350596,
            6470214,
            8475961,
            13357269,
            13314796,
            15019493,
            91179,
            91180,
            13314795,
            13314823,
            14993676,
            13314841,
            8475955,
            14994526,
            14989707,
            4852774,
            4852773,
            13314855,
            14994695,
        ]

        universal_categories = [
            278374,
            13491643,
            91078,
            91335,
            91327,
            91423,
            15368134,
            16044621,
            16044387,
            16044466,
            16044416,
            15714731,
            91392,
            15726400,
            91332,
            818944,
            91346,
            15714135,
            15714102,
            16011677,
            16011796,
            16011704,
            15714105,
            982439,
            15720388,
            16099944,
            15697667,
            15697685,
            15697659,
            15697691,
            90689,
            13041431,
            13196790,
            13041429,
            15696738,
            13041430,
            90691,
            90688,
            13041456,
            90690,
            13041460,
            13041507,
            13041512,
            13041511,
            13041314,
            13041252,
            13277094,
            8480725,
            8480722,
            8480713,
            15927546,
            13243353,
            13239358,
            91183,
            91167,
            91176,
            16042844,
            14989652,
            91173,
            7693914,
            14995755,
            13334231,
            13314877,
            91174,
            15002303,
        ]

        universal_categories_1 = [202] + universal_categories[1:]

        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
            Region(rid=2, name='Санкт-Петербург'),
            Region(rid=64, name='Екатеринбург'),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=hid, output_type=HyperCategoryType.GURU)
            for hid in personal_category_filter
            if hid != 14994695 and hid != 90606 and hid != 15557928 and hid != 15714713
        ]

        cls.index.hypertree += [
            HyperCategory(hid=hid, output_type=HyperCategoryType.GURU)
            for hid in universal_categories
            if hid != 278374 and hid != 13491643 and hid != 91183 and hid != 91167
        ]
        cls.index.hypertree += [HyperCategory(201, output_type=HyperCategoryType.GURU)]
        cls.index.hypertree += [HyperCategory(202, output_type=HyperCategoryType.GURU)]
        cls.index.hypertree += [
            HyperCategory(
                hid=200,
                children=[
                    HyperCategory(
                        hid=301,
                        children=[
                            HyperCategory(90606, output_type=HyperCategoryType.GURU),
                            HyperCategory(14994695, output_type=HyperCategoryType.GURU),
                        ],
                    ),
                    HyperCategory(
                        hid=302,
                        children=[
                            HyperCategory(278374, output_type=HyperCategoryType.GURU),
                            HyperCategory(13491643, output_type=HyperCategoryType.GURU),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.COMMONLY_PURCHASED_UNIVERSAL_CATEGORIES,
                kind=YamarecPlace.Type.CATEGORY,
                partitions=[
                    YamarecCategoryPartition(category_list=universal_categories, splits=['*']),
                    YamarecCategoryPartition(category_list=universal_categories_1, splits=["2"]),
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.COMMONLY_PURCHASED_PERSONAL_CATEGORIES,
                kind=YamarecPlace.Type.CATEGORY,
                partitions=[
                    YamarecCategoryPartition(category_list=personal_category_filter, splits=['*']),
                ],
            ),
        ]

        # shops
        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='blue_shop_1',
                currency=Currency.RUR,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=2,
                datafeed_id=2,
                priority_region=213,
                name='blue_shop_1',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue='REAL',
                warehouse_id=145,
            ),
        ]

        # models with randomly selected ts
        random.seed(0)
        random_ts = list(range(1, len(_Offers.model_ids) + 1))
        random.shuffle(random_ts)

        categories = [
            90606,  # personal
            278374,  # univeral
            7683677,  # personal
            202,  # unknown
            13491643,  # universal
            91078,  # universal
        ]
        cls.index.models += [
            Model(hyperid=model_id, hid=category, ts=ts)
            for model_id, category, ts in zip(_Offers.model_ids, categories, random_ts)
        ]

        # market skus
        cls.index.mskus += [
            MarketSku(
                title='Blue offer {sku}'.format(sku=shop_sku),
                hyperid=hyperid,
                sku=hyperid,
                waremd5='Sku{i}-wdDXWsIiLVm1goleg'.format(i=hyperid),
                blue_offers=[sku_offer],
            )
            for hyperid, shop_sku, sku_offer in zip(_Offers.model_ids, _Offers.shop_skus, _Offers.sku_offers)
        ]
        for yuid in ['1001', '1002']:
            cls.recommender.on_request_models_of_interest(user_id='yandexuid:' + yuid).respond({'models': ['1', '3']})

            cls.bigb.on_request(yandexuid=yuid, client='merch-machine').respond(keywords=DEFAULT_PROFILE)

    def test_universal_and_personal(self):
        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                "place=commonly_purchased&pp=18&numdoc=100&{}&rearr-factors=split=1;turn_on_commonly_purchased=1&allow-collapsing=1&rids=213&yandexuid=1001&debug=1".format(
                    param
                )
            )

            # ignore unknown accessory categories
            self.error_log.ignore("Unknown category ID")
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 5,
                        "results": [
                            {"categories": [{"id": 90606}], "id": 1},
                            {"categories": [{"id": 7683677}], "id": 3},
                            {"categories": [{"id": 278374}], "id": 2},
                            {"categories": [{"id": 13491643}], "id": 5},
                            {"categories": [{"id": 91078}], "id": 6},
                        ],
                    }
                },
                preserve_order=True,
            )
            # personal categories cold start must *not* be here
            self.assertFragmentNotIn(response, {"logicTrace": [Regex(r"^\[ME\] .+ COLD START mode:.*")]})

    def test_replacement_in_universal_categories(self):
        for param in ['rgb=blue', 'cpa=real']:
            # test based on test_universal_and_personal
            response = self.report.request_json(
                "place=commonly_purchased&pp=18&numdoc=100&{}&rearr-factors=split=1;turn_on_commonly_purchased=1&allow-collapsing=1&rids=213&yandexuid=1002&debug=1".format(
                    param
                )
            )

            # ignore unknown accessory categories
            self.error_log.ignore("Unknown category ID")

            # category 278374 was replaced by category 202
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 5,
                        "results": [
                            {"categories": [{"id": 90606}], "id": 1},
                            {"categories": [{"id": 7683677}], "id": 3},
                            {"categories": [{"id": 202}], "id": 4},
                            {"categories": [{"id": 13491643}], "id": 5},
                            {"categories": [{"id": 91078}], "id": 6},
                        ],
                    }
                },
                preserve_order=True,
            )

            self.assertFragmentNotIn(response, {"categories": [{"id": 278374}], "id": 2})

    def test_hid_filter(self):
        """Проверка hid-фильтра"""

        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                "place=commonly_purchased&pp=18&numdoc=100&{}&rearr-factors=split=1;turn_on_commonly_purchased=1&allow-collapsing=1&rids=213&yandexuid=1001&debug=1&hid=200".format(
                    param
                )
            )
            self.error_log.ignore("Unknown category ID")
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 3,
                        "results": [
                            {"categories": [{"id": 90606}], "id": 1},
                            {"categories": [{"id": 278374}], "id": 2},
                            {"categories": [{"id": 13491643}], "id": 5},
                        ],
                    }
                },
                preserve_order=True,
            )

            # Только персональные
            response = self.report.request_json(
                "place=commonly_purchased&pp=18&numdoc=100&{}&rearr-factors=split=1;turn_on_commonly_purchased=1&allow-collapsing=1&rids=213&yandexuid=1001&debug=1&hid=301".format(
                    param
                )
            )
            # ignore unknown accessory categories
            self.error_log.ignore("Unknown category ID")
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 1,
                        "results": [
                            {"categories": [{"id": 90606}], "id": 1},
                        ],
                    }
                },
                preserve_order=True,
            )

            # Только универсальные
            response = self.report.request_json(
                "place=commonly_purchased&pp=18&numdoc=100&{}&rearr-factors=split=1;turn_on_commonly_purchased=1&allow-collapsing=1&rids=213&yandexuid=1001&debug=1&hid=302".format(
                    param
                )
            )
            # ignore unknown accessory categories
            self.error_log.ignore("Unknown category ID")
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 2,
                        "results": [
                            {"categories": [{"id": 278374}], "id": 2},
                            {"categories": [{"id": 13491643}], "id": 5},
                        ],
                    }
                },
                preserve_order=True,
            )

    @classmethod
    def prepare_combined_model_orders(cls):
        counters = [
            BeruModelOrderLastTimeCounter(
                model_order_events=[
                    ModelLastOrderEvent(model_id=2, timestamp=478419200),
                    ModelLastOrderEvent(model_id=2484601, timestamp=488418200),
                    ModelLastOrderEvent(model_id=2484602, timestamp=478418200),
                    ModelLastOrderEvent(model_id=2484603, timestamp=478417200),
                    ModelLastOrderEvent(model_id=2484604, timestamp=478416200),
                    ModelLastOrderEvent(model_id=2484605, timestamp=478415200),
                    ModelLastOrderEvent(model_id=2484606, timestamp=478414200),
                    ModelLastOrderEvent(model_id=2484607, timestamp=478413200),
                    ModelLastOrderEvent(model_id=2484608, timestamp=478413200),
                    ModelLastOrderEvent(model_id=2484609, timestamp=478413200),
                ]
            ),
        ]

        cls.bigb.on_request(yandexuid=24846001, client='merch-machine').respond(counters=counters)

        cls.index.hypertree += [
            HyperCategory(
                hid=400,
                children=[
                    HyperCategory(
                        hid=501,
                        children=[
                            HyperCategory(15557928, output_type=HyperCategoryType.GURU),
                            HyperCategory(15714713, output_type=HyperCategoryType.GURU),
                        ],
                    ),
                    HyperCategory(
                        hid=502,
                        children=[
                            HyperCategory(91183, output_type=HyperCategoryType.GURU),
                            HyperCategory(91167, output_type=HyperCategoryType.GURU),
                        ],
                    ),
                ],
            ),
        ]
        categories = [
            91382,  # personal
            91183,  # univeral
            15714713,  # personal
            201,  # unknown
            91167,  # universal
            91176,  # universal
            91388,  # personal
            15557928,  # personal
            15714708,  # personal
        ]
        model_ids_1 = range(2484601, 2484610)
        cls.index.models += [
            Model(hyperid=model_id, hid=category, ts=model_id) for model_id, category in zip(model_ids_1, categories)
        ]

        model_ids_2 = range(2484610, 2484620)
        cls.index.models += [
            Model(hyperid=model_id, hid=category, ts=model_id) for model_id, category in zip(model_ids_2, categories)
        ]

        model_ids_1_onstock = range(2484601, 2484609)
        cls.index.mskus += [
            MarketSku(
                title='Blue offer {sku}'.format(sku=shop_sku),
                hyperid=hyperid,
                sku=hyperid,
                blue_offers=[BlueOffer(price=50, vat=Vat.VAT_10, offerid=str(shop_sku))],
            )
            for hyperid, shop_sku in zip(model_ids_1_onstock, model_ids_1_onstock)
        ]

        model_ids_2_onstock = range(2484610, 2484619)
        cls.index.mskus += [
            MarketSku(
                title='Blue offer {sku}'.format(sku=shop_sku),
                hyperid=hyperid,
                sku=hyperid,
                blue_offers=[BlueOffer(price=50, vat=Vat.VAT_10, offerid=str(shop_sku))],
            )
            for hyperid, shop_sku in zip(model_ids_2_onstock, model_ids_2_onstock)
        ]

        cls.recommender.on_request_models_of_interest(user_id='yandexuid:24846001').respond({'models': ['1', '3']})

    def get_known_thumbnails(self):
        return [
            {
                'namespace': 'marketpic',
                'thumbnails': [
                    {"name": "50x50", "width": 50, "height": 50},
                    {"name": "55x70", "width": 55, "height": 70},
                    {"name": "60x80", "width": 60, "height": 80},
                    {"name": "74x100", "width": 74, "height": 100},
                    {"name": "75x75", "width": 75, "height": 75},
                    {"name": "90x120", "width": 90, "height": 120},
                    {"name": "100x100", "width": 100, "height": 100},
                    {"name": "120x160", "width": 120, "height": 160},
                    {"name": "150x150", "width": 150, "height": 150},
                    {"name": "180x240", "width": 180, "height": 240},
                    {"name": "190x250", "width": 190, "height": 250},
                    {"name": "200x200", "width": 200, "height": 200},
                    {"name": "240x320", "width": 240, "height": 320},
                    {"name": "300x300", "width": 300, "height": 300},
                    {"name": "300x400", "width": 300, "height": 400},
                    {"name": "600x600", "width": 600, "height": 600},
                    {"name": "600x800", "width": 600, "height": 800},
                    {"name": "900x1200", "width": 900, "height": 1200},
                    {"name": "x124_trim", "width": 166, "height": 124},
                    {"name": "x166_trim", "width": 248, "height": 166},
                    {"name": "x248_trim", "width": 332, "height": 248},
                    {"name": "x332_trim", "width": 496, "height": 332},
                ],
            },
            {
                'namespace': 'marketpic_scaled',
                'thumbnails': [
                    {"name": "50x50", "width": 50, "height": 50},
                    {"name": "55x70", "width": 55, "height": 70},
                    {"name": "60x80", "width": 60, "height": 80},
                    {"name": "74x100", "width": 74, "height": 100},
                    {"name": "75x75", "width": 75, "height": 75},
                    {"name": "90x120", "width": 90, "height": 120},
                    {"name": "100x100", "width": 100, "height": 100},
                    {"name": "120x160", "width": 120, "height": 160},
                    {"name": "150x150", "width": 150, "height": 150},
                    {"name": "180x240", "width": 180, "height": 240},
                    {"name": "190x250", "width": 190, "height": 250},
                    {"name": "200x200", "width": 200, "height": 200},
                    {"name": "240x320", "width": 240, "height": 320},
                    {"name": "300x300", "width": 300, "height": 300},
                    {"name": "300x400", "width": 300, "height": 400},
                    {"name": "600x600", "width": 600, "height": 600},
                    {"name": "600x800", "width": 600, "height": 800},
                    {"name": "900x1200", "width": 900, "height": 1200},
                    {"name": "x124_trim", "width": 166, "height": 124},
                    {"name": "x166_trim", "width": 248, "height": 166},
                    {"name": "x248_trim", "width": 332, "height": 248},
                    {"name": "x332_trim", "width": 496, "height": 332},
                ],
            },
            {
                'namespace': 'mpic',
                'thumbnails': [
                    {"name": "1hq", "width": 50, "height": 50},
                    {"name": "2hq", "width": 100, "height": 100},
                    {"name": "3hq", "width": 75, "height": 75},
                    {"name": "4hq", "width": 150, "height": 150},
                    {"name": "5hq", "width": 200, "height": 200},
                    {"name": "6hq", "width": 250, "height": 250},
                    {"name": "7hq", "width": 120, "height": 120},
                    {"name": "8hq", "width": 240, "height": 240},
                    {"name": "9hq", "width": 500, "height": 500},
                    {"name": "x124_trim", "width": 166, "height": 124},
                    {"name": "x166_trim", "width": 248, "height": 166},
                    {"name": "x248_trim", "width": 332, "height": 248},
                    {"name": "x332_trim", "width": 496, "height": 332},
                ],
            },
        ]

    def test_combined_model_orders(self):
        """Проверяем режим подмешивания заказов пользователя к результатам
        поиска по персональным категориям.
        При этом модель 2484601 отфильтровывается как заказанная менее 2-х недель назад
        Модель 2484604 отфильтровывается - она из нечастотной категории
        Модель 2484609 отфильтровывается - не в наличии
        """
        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                "place=commonly_purchased&pp=18&numdoc=10&{}&rearr-factors=split=1;turn_on_commonly_purchased=1&allow-collapsing=1&rids=213&yandexuid=24846001&debug=1&new-picture-format=1".format(
                    param
                )
            )
            # ignore unknown accessory categories
            self.error_log.ignore("Unknown category ID")
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 11,
                        "knownThumbnails": self.get_known_thumbnails(),
                        "results": [
                            {"categories": [{"id": 278374}], "id": 2},
                            {"categories": [{"id": 91183}], "id": 2484602},
                            {"categories": [{"id": 15714713}], "id": 2484603},
                            {"categories": [{"id": 91167}], "id": 2484605},
                            {"categories": [{"id": 91176}], "id": 2484606},
                            {"categories": [{"id": 91388}], "id": 2484607},
                            {"categories": [{"id": 15557928}], "id": 2484608},
                            {"categories": [{"id": 90606}], "id": 1},
                            {"categories": [{"id": 7683677}], "id": 3},
                            {"categories": [{"id": 13491643}], "id": 5},
                        ],
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )
            # personal categories cold start must *not* be here
            self.assertFragmentNotIn(response, {"logicTrace": [Regex(r"^\[ME\] .+ COLD START mode:.*")]})

    def test_combined_models_hid_filter(self):
        """Проверка hid-фильтра в режиме объединенного поиска"""

        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                "place=commonly_purchased&pp=18&numdoc=10&{}&rearr-factors=split=1;turn_on_commonly_purchased=1&allow-collapsing=1&rids=213&yandexuid=24846001&debug=1&hid=400".format(
                    param
                )
            )
            self.error_log.ignore("Unknown category ID")
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 6,
                        "results": [
                            {"categories": [{"id": 91183}], "id": 2484602},
                            {"categories": [{"id": 15714713}], "id": 2484603},
                            {"categories": [{"id": 91167}], "id": 2484605},
                            {"categories": [{"id": 15557928}], "id": 2484608},
                            {"categories": [{"id": 91183}], "id": 2484611},
                            {"categories": [{"id": 91167}], "id": 2484614},
                        ],
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

            # Только персональные
            response = self.report.request_json(
                "place=commonly_purchased&pp=18&numdoc=10&{}&rearr-factors=split=1;turn_on_commonly_purchased=1&allow-collapsing=1&rids=213&yandexuid=24846001&debug=1&hid=501".format(
                    param
                )
            )
            # ignore unknown accessory categories
            self.error_log.ignore("Unknown category ID")
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 2,
                        "results": [
                            {"categories": [{"id": 15714713}], "id": 2484603},
                            {"categories": [{"id": 15557928}], "id": 2484608},
                        ],
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

            # Только универсальные
            response = self.report.request_json(
                "place=commonly_purchased&pp=18&numdoc=10&{}&rearr-factors=split=1;turn_on_commonly_purchased=1&allow-collapsing=1&rids=213&yandexuid=24846001&debug=1&hid=502".format(
                    param
                )
            )
            # ignore unknown accessory categories
            self.error_log.ignore("Unknown category ID")
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 4,
                        "results": [
                            {"categories": [{"id": 91183}], "id": 2484602},
                            {"categories": [{"id": 91167}], "id": 2484605},
                            {"categories": [{"id": 91183}], "id": 2484611},
                            {"categories": [{"id": 91167}], "id": 2484614},
                        ],
                    }
                },
                preserve_order=True,
            )

    def test_reversed_experiment(self):
        """Проверяем, что в обратном эксперименте поведение прежнее"""
        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                "rearr-factors=market_commonly_purchased_use_orders_history=0&place=commonly_purchased&pp=18&numdoc=10&"
                "{}&rearr-factors=split=1;turn_on_commonly_purchased=1&allow-collapsing=1&rids=213&yandexuid=24846001&debug=1&new-picture-format=1".format(
                    param
                )
            )
            # ignore unknown accessory categories
            self.error_log.ignore("Unknown category ID")
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 5,
                        "knownThumbnails": self.get_known_thumbnails(),
                        "results": [
                            {"categories": [{"id": 90606}], "id": 1},
                            {"categories": [{"id": 7683677}], "id": 3},
                            {"categories": [{"id": 278374}], "id": 2},
                            {"categories": [{"id": 13491643}], "id": 5},
                            {"categories": [{"id": 91078}], "id": 6},
                        ],
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )
            # personal categories cold start must *not* be here
            self.assertFragmentNotIn(response, {"logicTrace": [Regex(r"^\[ME\] .+ COLD START mode:.*")]})

    def test_commonly_purchased_ordered_param(self):
        """Проверяем, что:
        - commonly-purchased-ordered=1 при наличии заказов = есть выдача
        - commonly-purchased-ordered=0 при наличии заказов = нет выдачи
        - commonly-purchased-ordered=1 при отсутствии заказов = нет выдачи
        - commonly-purchased-ordered=0 при отсутствии заказов = есть выдача
        """
        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                "commonly-purchased-ordered=1&yandexuid=24846001&place=commonly_purchased&pp=18&numdoc=10&{}&rearr-factors=split=1;turn_on_commonly_purchased=1&allow-collapsing=1&rids=213".format(
                    param
                )
            )
            # ignore unknown accessory categories
            self.error_log.ignore("Unknown category ID")
            self.assertFragmentIn(response, {"search": {"results": NotEmpty()}})

            response = self.report.request_json(
                "commonly-purchased-ordered=0&yandexuid=24846001&place=commonly_purchased&pp=18&numdoc=10&{}&rearr-factors=split=1;turn_on_commonly_purchased=1&allow-collapsing=1&rids=213".format(
                    param
                )
            )
            # ignore unknown accessory categories
            self.error_log.ignore("Unknown category ID")
            self.assertFragmentIn(response, {"search": {"results": []}})

            response = self.report.request_json(
                "commonly-purchased-ordered=1&yandexuid=1001&place=commonly_purchased&pp=18&numdoc=10&{}&rearr-factors=split=1;turn_on_commonly_purchased=1&allow-collapsing=1&rids=213".format(
                    param
                )
            )
            # ignore unknown accessory categories
            self.error_log.ignore("Unknown category ID")
            self.assertFragmentIn(response, {"search": {"results": []}})

            response = self.report.request_json(
                "commonly-purchased-ordered=0&yandexuid=1001&place=commonly_purchased&pp=18&numdoc=10&{}&rearr-factors=split=1;turn_on_commonly_purchased=1&allow-collapsing=1&rids=213".format(
                    param
                )
            )
            # ignore unknown accessory categories
            self.error_log.ignore("Unknown category ID")
            self.assertFragmentIn(response, {"search": {"results": NotEmpty()}})

    def test_textless_search_flag(self):
        """Проверяем, что в зависимости от флага меняется сортировка."""
        for param in ['rgb=blue', 'cpa=real']:
            request = "place=commonly_purchased&pp=18&numdoc=100&{}&rearr-factors=split=1;turn_on_commonly_purchased=1&allow-collapsing=1&rids=213&yandexuid=1001".format(
                param
            )
            request += "&rearr-factors=market_commonly_purchased_use_textless_search={use_textless_search}"

            response = self.report.request_json(request.format(use_textless_search=0) + "&debug=1")
            self.assertFragmentIn(response, {"logicTrace": [Contains("Sorting: SF_GURU_POPULARITY")]})
            response = self.report.request_json(request.format(use_textless_search=1) + "&debug=1")
            self.assertFragmentIn(response, {"logicTrace": [Contains("Sorting: SF_CPM")]})

    def test_empty_response_for_rgb_blue(self):
        """
        https://st.yandex-team.ru/MARKETRECOM-2507
        Проверяем, что с rgb=blue и без флага turn_on_commonly_purchased выдача пуста
        """
        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json("place=commonly_purchased&{}".format(param))
            self.assertFragmentIn(response, {"search": {"total": 0, "results": []}}, allow_different_len=False)

            response = self.report.request_json(
                "place=commonly_purchased&{}&rearr-factors=turn_on_commonly_purchased=0".format(param)
            )
            self.assertFragmentIn(response, {"search": {"total": 0, "results": []}}, allow_different_len=False)


if __name__ == '__main__':
    main()
