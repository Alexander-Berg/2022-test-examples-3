#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    HyperCategory,
    HyperCategoryType,
    Model,
    NavCategory,
    Offer,
    Picture,
    YamarecCategoryPartition,
    YamarecPlace,
)
from core.testcase import TestCase, main
from core.bigb import BeruSkuOrderLastTimeCounter, SkuLastOrderEvent
from core.types.sku import MarketSku, BlueOffer


boost_flags = ["none", "boost-common", "boost-all"]
boost_coefs = ["1.0", "2.0", "4.0", "0.5"]
Model1 = 264711001
Model2 = 264711002
Model4 = 264711004
Model5 = 264711005
Model7 = 264711007

pic1 = Picture(width=100, height=100, group_id=1001, picture_id='pic1')
pic2 = Picture(width=100, height=100, group_id=1002, picture_id='pic2')
pic3 = Picture(width=100, height=100, group_id=1003, picture_id='pic3')
pic4 = Picture(width=100, height=100, group_id=1004, picture_id='pic4')
pic5 = Picture(width=100, height=100, group_id=1005, picture_id='pic5')
pic6 = Picture(width=100, height=100, group_id=1006, picture_id='pic6')


def create_rank(is_boosted, have_boost_flag, is_sorted, is_textless):
    result = [
        {'name': 'HAS_PICTURE'},
        {'name': 'DELIVERY_TYPE'},
        {'name': 'ONSTOCK'},
        {'name': 'RANDX'},
    ]
    if have_boost_flag:
        result.insert(
            0,
            {
                'name': 'BOOSTED_BOUGHT_SKU',
                'value': str(is_boosted),
            },
        )
    shift = int(have_boost_flag)
    if is_sorted:
        result.insert(2 + shift, {'name': 'IS_MODEL'})
        result.insert(3 + shift, {'name': 'GURU_POPULARITY'})
    else:
        result.insert(2 + shift, {'name': 'CPM'})
        result.insert(3 + shift, {'name': 'MODEL_TYPE'})
        result.insert(4 + shift, {'name': 'POPULARITY'})
    return result


def create_product_with_offer(
    id, is_boosted, have_boost_flag=1, is_sorted=0, is_textless=0, mult=1, absc=False, head=False
):
    if absc:
        result = {
            'entity': 'offer',
            'sku': str(id),
            'debug': {'rank': create_rank(is_boosted, have_boost_flag, is_sorted, is_textless)},
        }
    elif head:
        result = {
            'entity': 'offer',
            'sku': str(id),
            'debug': {
                'properties': {'BOOST_MULTIPLIER': str(mult)},
                'rank': create_rank(is_boosted, have_boost_flag, is_sorted, is_textless),
                'tech': {'docPriority': 'DocRangeNonLocalHead'},
            },
        }
    else:
        result = {
            'entity': 'offer',
            'sku': str(id),
            'debug': {
                'properties': {'BOOST_MULTIPLIER': str(mult)},
                'rank': create_rank(is_boosted, have_boost_flag, is_sorted, is_textless),
            },
        }
    return result


class _Offers(object):
    sku1_offer1 = BlueOffer(
        price=1,
        feedid=6,
        offerid='blue.offer.1.1',
        waremd5='Sku1Price5-IiLVm1Goleg',
        randx=6,
        cpa=Offer.CPA_REAL,
        picture=pic1,
    )
    sku2_offer1 = BlueOffer(
        price=5,
        feedid=4,
        offerid='blue.offer.2.1',
        waremd5='Sku2Price5-IiLVm1Goleg',
        randx=4,
        cpa=Offer.CPA_REAL,
        picture=pic2,
    )
    sku4_offer1 = BlueOffer(
        price=15,
        feedid=2,
        offerid='blue.offer.4.1',
        waremd5='Sku4Price6-IiLVm1Goleg',
        randx=1,
        cpa=Offer.CPA_REAL,
        picture=pic3,
    )
    sku4_offer2 = BlueOffer(
        price=14,
        feedid=9,
        offerid='blue.offer.4.2',
        waremd5='Sku4Price7-IiLVm1Goleg',
        randx=2,
        cpa=Offer.CPA_REAL,
        picture=pic4,
    )
    sku5_offer1 = BlueOffer(
        price=10,
        feedid=7,
        offerid='blue.offer.5.1',
        waremd5='Sku5Price6-IiLVm1Goleg',
        randx=3,
        cpa=Offer.CPA_REAL,
        picture=pic5,
    )
    sku7_offer1 = BlueOffer(
        price=4,
        feedid=10,
        offerid='blue.offer.7.1',
        waremd5='Sku7Price7-IiLVm1Goleg',
        randx=5,
        cpa=Offer.CPA_REAL,
        picture=pic6,
    )


class T(TestCase):
    @classmethod
    def prepare_boost(cls):

        cls.index.hypertree += [
            HyperCategory(
                hid=264711,
                children=[
                    HyperCategory(
                        hid=264712,
                        children=[
                            HyperCategory(hid=264714, output_type=HyperCategoryType.GURU),
                            HyperCategory(hid=264715, output_type=HyperCategoryType.GURU),
                        ],
                    ),
                    HyperCategory(
                        hid=264713,
                        children=[
                            HyperCategory(hid=264716, output_type=HyperCategoryType.GURU),
                            HyperCategory(hid=264717, output_type=HyperCategoryType.GURU),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.navtree += [
            NavCategory(
                nid=318,
                children=[
                    NavCategory(nid=264713, hid=264713),
                    NavCategory(nid=320, hid=111),
                    NavCategory(nid=321, hid=112),
                ],
            )
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.COMMONLY_PURCHASED_UNIVERSAL_CATEGORIES,
                kind=YamarecPlace.Type.CATEGORY,
                partitions=[
                    YamarecCategoryPartition(category_list=[264711, 264714], splits=['*']),
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.COMMONLY_PURCHASED_PERSONAL_CATEGORIES,
                kind=YamarecPlace.Type.CATEGORY,
                partitions=[
                    YamarecCategoryPartition(category_list=[264712], splits=['*']),
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=Model1, title='Яндекс.Поиск', hid=264711),
            Model(hyperid=Model2, title='Яндекс.Маркет', hid=264712),
            Model(hyperid=Model4, title='Яндекс.Такси', hid=264714),
            Model(hyperid=Model5, title='Яндекс.Еда', hid=264715),
            Model(hyperid=Model7, title='Яндекс.Таланты', hid=264717),
        ]

        counters = [
            BeruSkuOrderLastTimeCounter(
                sku_order_events=[
                    SkuLastOrderEvent(sku_id=264711001, timestamp=418419200),
                    SkuLastOrderEvent(sku_id=264711007, timestamp=488418200),
                    SkuLastOrderEvent(sku_id=264711002, timestamp=488419100),
                ]
            ),
        ]

        cls.bigb.on_request(yandexuid=26471001, client='merch-machine').respond(counters=counters)

        cls.index.mskus += [
            MarketSku(
                title="разработчик 4",
                hyperid=Model4,
                sku=264711004,
                waremd5='Sku4-wdDXWsIiLVm1goleg',
                blue_offers=[_Offers.sku4_offer1, _Offers.sku4_offer2],
                randx=1,
            ),
            MarketSku(
                title="разработчик 5",
                hyperid=Model5,
                sku=264711005,
                waremd5='Sku5-wdDXWsIiLVm1goleg',
                blue_offers=[_Offers.sku5_offer1],
                randx=2,
            ),
            MarketSku(
                title="разработчик 2",
                hyperid=Model2,
                sku=264711002,
                waremd5='Sku2-wdDXWsIiLVm1goleg',
                blue_offers=[_Offers.sku2_offer1],
                randx=3,
            ),
            MarketSku(
                title="разработчик 7",
                hyperid=Model7,
                sku=264711007,
                waremd5="Sku7-wdDXWsIiLVm1goleg",
                blue_offers=[_Offers.sku7_offer1],
                randx=4,
            ),
            MarketSku(
                title="разработчик 1",
                hyperid=Model1,
                sku=264711001,
                waremd5="Sku1-wdDXWsIiLVm1goleg",
                blue_offers=[_Offers.sku1_offer1],
                randx=5,
            ),
        ]

    def test_boost_offers_flag(self):
        """
        Проверяем:
            * На первых позициях должны быть все купленные товары в этой категории (при boost-all)
            * Аналогично но проверяем на принадлежность частотным подкатегориям заданной категории (при boost-common)
            * Отсутствие буста (none)
            * Документы не должны дублироваться
        """
        expected_result = [
            [Model1, Model7, Model2, Model5, Model4],  # none
            [Model1, Model2, Model7, Model5, Model4],  # boost-common
            [Model1, Model7, Model2, Model5, Model4],  # boost-all
        ]
        is_boosted = [
            [0, 0, 0, 0, 0],  # на none ничего не бустится
            [1, 1, 0, 0, 0],  # 2 и 1 модели бустятся тк они в коммон категории
            [1, 1, 1, 0, 0],  # бустятся 2, 7 и 1 модели
        ]
        for exp_res_num in range(3):
            request = (
                "place=prime&allow-collapsing=0&entities=offer&use-default-offers=1&hid=264711&"
                "rearr-factors=market_white_boost_bought_orders={};market_metadoc_search=no&yandexuid=26471001&debug=da"
            )
            # поскольку в коде зашиты категории для буста, то список надо передавать всегда
            request += "&rearr-factors=market_white_boost_bought_hids=264711,264712,264714,264715,264717;market_white_boost_bought_days=0"
            response = self.report.request_json(request.format(boost_flags[exp_res_num]))
            have_boost_flag = exp_res_num > 0
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 5,
                        "totalOffers": 5,
                        "results": [
                            create_product_with_offer(
                                expected_result[exp_res_num][0],
                                is_boosted[exp_res_num][0],
                                have_boost_flag,
                                is_sorted=0,
                                is_textless=1,
                            ),
                            create_product_with_offer(
                                expected_result[exp_res_num][1],
                                is_boosted[exp_res_num][1],
                                have_boost_flag,
                                is_sorted=0,
                                is_textless=1,
                            ),
                            create_product_with_offer(
                                expected_result[exp_res_num][2],
                                is_boosted[exp_res_num][2],
                                have_boost_flag,
                                is_sorted=0,
                                is_textless=1,
                            ),
                            create_product_with_offer(
                                expected_result[exp_res_num][3],
                                is_boosted[exp_res_num][3],
                                have_boost_flag,
                                is_sorted=0,
                                is_textless=1,
                            ),
                            create_product_with_offer(
                                expected_result[exp_res_num][4],
                                is_boosted[exp_res_num][4],
                                have_boost_flag,
                                is_sorted=0,
                                is_textless=1,
                            ),
                        ],
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )
            for page_num in range(4):
                request = (
                    "place=prime&allow-collapsing=0&entities=offer&use-default-offers=1&numdoc=1&page={}&hid=264711&"
                    "rearr-factors=market_white_boost_bought_orders={};market_metadoc_search=no&yandexuid=26471001&debug=da&"
                    "rearr-factors=market_white_boost_bought_hids=264711,264712,264714,264715,264717;market_white_boost_bought_days=0"
                )
                response = self.report.request_json(request.format(page_num + 1, boost_flags[exp_res_num]))
                self.assertFragmentIn(
                    response,
                    {
                        "search": {
                            "total": 5,
                            "totalOffers": 5,
                            "results": [
                                create_product_with_offer(
                                    expected_result[exp_res_num][page_num],
                                    is_boosted[exp_res_num][page_num],
                                    have_boost_flag,
                                    is_sorted=0,
                                    is_textless=1,
                                ),
                            ],
                        }
                    },
                    preserve_order=True,
                    allow_different_len=False,
                )

    def test_boost_absence(self):
        """
        При наличии пользовательской сортироки или текста в запросе буст не должен выполняться
        """
        not_boosted_order = [Model1, Model7, Model2, Model5, Model4]

        for flag in boost_flags:
            response = self.report.request_json(
                "place=prime&allow-collapsing=0&entities=offer&use-default-offers=1&how=guru_popularity&hid=264711&"
                "rearr-factors=market_white_boost_bought_orders={};market_metadoc_search=no&yandexuid=26471001&debug=da".format(
                    flag
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 5,
                        "totalOffers": 5,
                        "results": [
                            create_product_with_offer(model, 0, False, is_sorted=1, absc=True)
                            for model in not_boosted_order
                        ],
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )
            response = self.report.request_json(
                "place=prime&allow-collapsing=0&entities=offer&use-default-offers=1&text=разработчик&hid=264711&"
                "rearr-factors=market_white_boost_bought_orders={};market_metadoc_search=no&yandexuid=26471001&debug=da".format(
                    flag
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 5,
                        "totalOffers": 5,
                        "results": [
                            create_product_with_offer(model, 0, False, absc=True) for model in not_boosted_order
                        ],
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_date(self):
        expected_result = [
            [Model1, Model7, Model2, Model5, Model4],  # none
            [Model2, Model1, Model7, Model5, Model4],  # boost-common
            [Model7, Model2, Model1, Model5, Model4],  # boost-all
        ]
        is_boosted = [
            [0, 0, 0, 0, 0],  # на none ничего не бустится
            [1, 0, 0, 0, 0],  # 2 модель бустится тк она в коммон категории, а 1 модель не подходит по дате
            [1, 1, 0, 0, 0],  # 2 и 7 модель бустятся
        ]
        for exp_res_num in range(3):
            request = (
                "place=prime&allow-collapsing=0&entities=offer&use-default-offers=1&hid=264711&"
                "rearr-factors=market_white_boost_bought_orders={};market_white_boost_bought_days=5;market_metadoc_search=no&yandexuid=26471001&debug=da&"
                "rearr-factors=market_white_boost_bought_hids=264711,264712,264714,264715,264717"
            )
            response = self.report.request_json(request.format(boost_flags[exp_res_num]))
            have_boost_flag = exp_res_num > 0
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 5,
                        "totalOffers": 5,
                        "results": [
                            create_product_with_offer(
                                expected_result[exp_res_num][0],
                                is_boosted[exp_res_num][0],
                                have_boost_flag,
                                is_sorted=0,
                                is_textless=1,
                            ),
                            create_product_with_offer(
                                expected_result[exp_res_num][1],
                                is_boosted[exp_res_num][1],
                                have_boost_flag,
                                is_sorted=0,
                                is_textless=1,
                            ),
                            create_product_with_offer(
                                expected_result[exp_res_num][2],
                                is_boosted[exp_res_num][2],
                                have_boost_flag,
                                is_sorted=0,
                                is_textless=1,
                            ),
                            create_product_with_offer(
                                expected_result[exp_res_num][3],
                                is_boosted[exp_res_num][3],
                                have_boost_flag,
                                is_sorted=0,
                                is_textless=1,
                            ),
                            create_product_with_offer(
                                expected_result[exp_res_num][4],
                                is_boosted[exp_res_num][4],
                                have_boost_flag,
                                is_sorted=0,
                                is_textless=1,
                            ),
                        ],
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_hids(self):
        expected_result = [
            [Model1, Model7, Model2, Model5, Model4],  # none
            [Model2, Model1, Model7, Model5, Model4],  # boost-common
            [Model7, Model2, Model1, Model5, Model4],  # boost-all
        ]
        is_boosted = [
            [0, 0, 0, 0, 0],  # на none ничего не бустится
            [1, 0, 0, 0, 0],  # 2 модель бустится тк она в коммон категории, а 1 модель не подходит по дате
            [1, 1, 0, 0, 0],  # 2 и 7 модель бустятся
        ]
        for exp_res_num in range(3):
            response = self.report.request_json(
                "place=prime&allow-collapsing=0&entities=offer&use-default-offers=1&hid=264711&"
                "rearr-factors=market_white_boost_bought_orders={};market_white_boost_bought_hids=264712,264717;market_metadoc_search=no&yandexuid=26471001&debug=da".format(
                    boost_flags[exp_res_num]
                )
            )
            have_boost_flag = exp_res_num > 0
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 5,
                        "totalOffers": 5,
                        "results": [
                            create_product_with_offer(
                                expected_result[exp_res_num][0],
                                is_boosted[exp_res_num][0],
                                have_boost_flag,
                                is_sorted=0,
                                is_textless=1,
                            ),
                            create_product_with_offer(
                                expected_result[exp_res_num][1],
                                is_boosted[exp_res_num][1],
                                have_boost_flag,
                                is_sorted=0,
                                is_textless=1,
                            ),
                            create_product_with_offer(
                                expected_result[exp_res_num][2],
                                is_boosted[exp_res_num][2],
                                have_boost_flag,
                                is_sorted=0,
                                is_textless=1,
                            ),
                            create_product_with_offer(
                                expected_result[exp_res_num][3],
                                is_boosted[exp_res_num][3],
                                have_boost_flag,
                                is_sorted=0,
                                is_textless=1,
                            ),
                            create_product_with_offer(
                                expected_result[exp_res_num][4],
                                is_boosted[exp_res_num][4],
                                have_boost_flag,
                                is_sorted=0,
                                is_textless=1,
                            ),
                        ],
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_coef(self):
        expected_result = [
            [Model1, Model7, Model2, Model5, Model4],
            [Model1, Model7, Model2, Model5, Model4],
            [Model1, Model7, Model2, Model5, Model4],
            [Model5, Model4, Model1, Model7, Model2],  # проверяем, что буст с коэффициентом не безусловный
        ]
        mults = [[1, 1, 1, 1, 1], [2, 2, 2, 1, 1], [4, 4, 4, 1, 1], [1, 1, 0.5, 0.5, 0.5]]
        for exp_res_num in range(4):
            request = (
                "place=prime&allow-collapsing=0&entities=offer&use-default-offers=1&text=разработчик&hid=264711&yandexuid=26471001&debug=da&rearr-factors="
                "market_metadoc_search=no;"
                "market_white_boost_bought_coef={};"
                "market_white_boost_bought_days=0;"
                "market_white_boost_bought_hids=264711;"
            ).format(boost_coefs[exp_res_num])

            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 5,
                        "totalOffers": 5,
                        "results": [
                            create_product_with_offer(
                                expected_result[exp_res_num][i],
                                0,
                                False,
                                mult=mults[exp_res_num][i],
                            )
                            for i in range(5)
                        ],
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_boost_bought_meta_coef(self):
        request = (
            "place=prime&allow-collapsing=0&entities=offer&use-default-offers=1&text=разработчик&hid=264711&yandexuid=26471001&debug=da&rearr-factors="
            "market_white_boost_bought_coef=2.0;"
            "market_white_boost_bought_days=0;"
            "market_white_boost_bought_hids=264711;"
            "market_metadoc_search=no;"
            "market_textless_meta_formula_type=TESTALGO_trivial;"
            "market_new_cpm_iterator=0"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 5,
                    "totalOffers": 5,
                    "results": [
                        {"debug": {"metaProperties": {"BOOST_MULTIPLIER": "2"}}},
                        {"debug": {"metaProperties": {"BOOST_MULTIPLIER": "2"}}},
                        {"debug": {"metaProperties": {"BOOST_MULTIPLIER": "2"}}},
                        {"debug": {"metaProperties": {"BOOST_MULTIPLIER": "1"}}},
                        {"debug": {"metaProperties": {"BOOST_MULTIPLIER": "1"}}},
                    ],
                },
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_hids_and_subtrees_and_nids(self):
        """
        проверяем, что во флаге мы можем указывать хиды, в поддеревьях которых забустятся товары
        и так же с нидами
        """
        boost_hids_and_nids = [
            [[264711], [318]],  # 1 2 и 7
            [[264713], []],  # только 7 модель
            [[], [318, 319, 400]],  # только 7
            [[100500], [318, 319, 400]],  # только 7
            [[254714, 264713], [264713]],  # только 7
            [[264716, 264715], [319]],  # ничего
        ]
        expected_result = [
            [Model1, Model7, Model2, Model5, Model4],
            [Model7, Model1, Model2, Model5, Model4],
            [Model7, Model1, Model2, Model5, Model4],
            [Model7, Model1, Model2, Model5, Model4],
            [Model7, Model1, Model2, Model5, Model4],
            [Model1, Model7, Model2, Model5, Model4],
        ]
        is_boosted = [
            [1, 1, 1, 0, 0],
            [1, 0, 0, 0, 0],
            [1, 0, 0, 0, 0],
            [1, 0, 0, 0, 0],
            [1, 0, 0, 0, 0],
            [0, 0, 0, 0, 0],
        ]
        for exp_res_num in range(len(boost_hids_and_nids)):
            request = (
                "place=prime&allow-collapsing=0&entities=offer&use-default-offers=1&hid=264711&yandexuid=26471001&debug=da"
                "&rearr-factors=market_white_boost_bought_orders=boost-all;market_metadoc_search=no;"
                "market_white_boost_bought_hids={};"
                "market_white_boost_bought_nids={};"
                "market_white_boost_bought_days=0"
            ).format(
                ','.join(map(str, boost_hids_and_nids[exp_res_num][0])),
                ','.join(map(str, boost_hids_and_nids[exp_res_num][1])),
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 5,
                        "totalOffers": 5,
                        "results": [
                            create_product_with_offer(
                                expected_result[exp_res_num][0],
                                is_boosted[exp_res_num][0],
                            ),
                            create_product_with_offer(
                                expected_result[exp_res_num][1],
                                is_boosted[exp_res_num][1],
                            ),
                            create_product_with_offer(
                                expected_result[exp_res_num][2],
                                is_boosted[exp_res_num][2],
                            ),
                            create_product_with_offer(
                                expected_result[exp_res_num][3],
                                is_boosted[exp_res_num][3],
                            ),
                            create_product_with_offer(
                                expected_result[exp_res_num][4],
                                is_boosted[exp_res_num][4],
                            ),
                        ],
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )


if __name__ == '__main__':
    main()
