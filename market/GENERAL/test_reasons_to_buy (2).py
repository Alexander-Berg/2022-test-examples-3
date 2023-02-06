#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    Currency,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    MnPlace,
    Model,
    Offer,
    ReferenceShop,
    Shop,
    Tax,
    Vat,
    YamarecPlace,
    YamarecPlaceReasonsToBuy,
    YamarecSettingPartition,
)
from core.testcase import TestCase, main
from core.matcher import NoKey, ElementCount

YANDEX_STATION_ID = 1971204201


class T(TestCase):
    """
    Проверка вставки данных о причинах купить товар в модельную выдачу
    """

    @classmethod
    def prepare(cls):
        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()

        """
        Данные о причинах купить товар
        """
        cls.index.models += [
            Model(hyperid=1, hid=101, title="Good phone"),
            Model(hyperid=2, hid=101),
            Model(hyperid=3, hid=101, accessories=[1, YANDEX_STATION_ID], analogs=[1, YANDEX_STATION_ID]),
            Model(hyperid=YANDEX_STATION_ID, hid=101, title="Яндекс.Станция"),
        ]

        cls.index.shops += [
            Shop(fesh=1001, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
        ]

        cls.index.shops += [Shop(fesh=1777, fulfillment_virtual=True, virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE)]

        cls.index.offers += [
            Offer(hyperid=1, price=100, fesh=1001, cpa=Offer.CPA_REAL),
            Offer(hyperid=2, price=200, fesh=1001, cpa=Offer.CPA_REAL),
            Offer(hyperid=YANDEX_STATION_ID, title='Яндекс.Станция', price=9990, fesh=1001, cpa=Offer.CPA_REAL),
        ]

        cls.index.yamarec_places += [
            YamarecPlaceReasonsToBuy()
            .new_partition("split=1")
            .add(hyperid=1, reasons=[{"key1": "value1"}, {"key2": "value2"}])
            .add(hyperid=11, reasons=[{"key1": "value1"}, {"key2": "value2"}])  # valid json
            .add(hyperid=2, reasons=[])
            .add(  # valid empty json
                hyperid=YANDEX_STATION_ID,
                reasons=[
                    {"id": "viewed_n_times", "value": "100500"},
                    {"id": "bought_n_times", "value": "100"},
                    {"id": "compatible_with_alisa", "value": "1"},
                ],
            )
            .add(
                hyperid=1100,
                reasons=[{"id": "hype_goods", "value": "1"}],
            )
            .add(
                hyperid=1101,
                reasons=[
                    {
                        "id": "customers_choice",
                        "type": "consumerfactor",
                        "value": 0.95,
                    }
                ],
            )
            .add(
                hyperid=1102,
                reasons=[
                    {
                        "id": "best_by_factor",
                        "type": "consumerFactor",
                        "factor_id": "1",
                        "factor_priority": "1",
                        "factor_name": "factorName1",
                        "value": 0.85,
                    }
                ],
            )
            .add(
                hyperid=1103,
                reasons=[
                    {
                        "id": "positive_feedback",
                        "type": "consumerFactor",
                        "author_puid": "1001",
                        "text": "text3",
                        "value": 5.0,
                    },
                    {
                        "id": "best_by_factor",
                        "type": "consumerFactor",
                        "factor_id": "1",
                        "factor_priority": "1",
                        "factor_name": "factorName1",
                        "value": 0.9,
                    },
                    {
                        "id": "customers_choice",
                        "type": "consumerFactor",
                        "value": 0.95,
                    },
                ],
            )
            .add(
                hyperid=1104,
                reasons=[
                    {
                        "id": "positive_feedback",
                        "type": "consumerFactor",
                        "author_puid": "1001",
                        "text": "text1",
                        "value": 4.5,
                    }
                ],
            )
            .new_partition("split=2")
            .add(hyperid=1, reasons="")
            .new_partition("split=3")  # invalid json
            .add(
                hyperid=1101,
                reasons=[
                    {
                        "id": "positive_feedback",
                        "type": "consumerfactor",
                        "author_puid": "1001",
                        "text": "text1",
                        "anonymous": "true",  # filtered
                        "value": 5.0,
                        "value_threshold": "4.0",
                    },
                    {
                        "id": "best_by_factor",
                        "type": "consumerfactor",
                        "factor_id": "1",
                        "factor_priority": "1",
                        "factor_name": "factorname1",
                        "value": 0.7,  # filtered
                        "value_threshold": "0.8",
                    },
                    {  # passed
                        "id": "customers_choice",
                        "type": "consumerfactor",
                        "rating": "4.5",
                        "rating_threshold": "4.0",
                        "value": 0.95,
                        "recommenders_count": "100",
                        "share_threshold": "0.8",
                    },
                ],
            )
            .add(
                hyperid=1102,
                reasons=[
                    {
                        "id": "positive_feedback",
                        "type": "consumerFactor",
                        "author_puid": "1002",
                        "text": "text2",
                        "anonymous": "false",
                        "value": 3.5,  # filtered
                        "value_threshold": "4.0",
                    },
                    {  # passed
                        "id": "best_by_factor",
                        "type": "consumerFactor",
                        "factor_id": "1",
                        "factor_priority": "1",
                        "factor_name": "factorName1",
                        "value": 0.85,
                        "value_threshold": "0.8",
                    },
                    {
                        "id": "customers_choice",
                        "type": "consumerFactor",
                        "rating": "4.5",
                        "rating_threshold": "4.0",
                        "value": 0.75,  # filtered
                        "recommenders_count": "10",
                        "share_threshold": "0.8",
                    },
                ],
            )
            .add(
                hyperid=1103,
                reasons=[
                    {  # all passed
                        "id": "positive_feedback",
                        "type": "consumerFactor",
                        "author_puid": "1001",
                        "text": "text3",
                        "anonymous": "false",
                        "value": 5.0,
                        "value_threshold": "4.0",
                    },
                    {
                        "id": "best_by_factor",
                        "type": "consumerFactor",
                        "factor_id": "1",
                        "factor_priority": "1",
                        "factor_name": "factorName1",
                        "value": 0.9,
                        "value_threshold": "0.8",
                    },
                    {
                        "id": "customers_choice",
                        "type": "consumerFactor",
                        "rating": "4.5",
                        "rating_threshold": "4.0",
                        "value": 0.95,
                        "recommenders_count": "100",
                        "share_threshold": "0.8",
                    },
                ],
            )
            .add(
                hyperid=1104,
                reasons=[
                    {  # passed
                        "id": "positive_feedback",
                        "type": "consumerFactor",
                        "author_puid": "1001",
                        "text": "text1",
                        "anonymous": "false",
                        "value": 4.5,
                        "value_threshold": "4.0",
                    },
                    {
                        "id": "best_by_factor",
                        "type": "consumerFactor",
                        "factor_id": "1",
                        "factor_priority": "1",
                        "factor_name": "factorName1",
                        "value": 0.5,  # filtered
                        "value_threshold": "0.8",
                    },
                    {
                        "id": "customers_choice",
                        "type": "consumerFactor",
                        "rating": "2.0",
                        "rating_threshold": "4.0",
                        "value": 0.6666,  # filtered
                        "recommenders_count": "100",
                        "share_threshold": "0.8",
                    },
                ],
            )
            .new_partition("split=4")
            .add(hyperid=2088101, reasons=[{"key1_white": "value1"}, {"key2_white": "value2"}])
            .add(hyperid=20881011, reasons=[{"key1_white": "value1"}, {"key2_white": "value2"}])
            .add(hyperid=2088102, reasons=[])
            .add(hyperid=YANDEX_STATION_ID, reasons=[{"key1_white": "value1"}, {"key2_white": "value2"}])
            .new_partition("split=5")
            .add(  # valid empty json
                hyperid=YANDEX_STATION_ID,
                reasons=[
                    {"id": "viewed_n_times", "value": "100500"},
                    {"id": "bought_n_times", "value": "100500"},
                    {"id": "bestseller", "value": "100500"},
                ],
            )
            .new_partition("split=6")
            .add(  # valid empty json
                hyperid=1,
                reasons=[
                    {"id": "viewed_n_times", "value": "100500"},
                    {"id": "bought_n_times", "value": "100500"},
                    {"id": "bestseller", "value": "100500"},
                ],
            )
        ]

        cls.index.models += [
            Model(hyperid=1101, ts=1101, title="modelWithReasons 1"),
            Model(hyperid=1102, ts=1102, title="modelWithReasons 2"),
            Model(hyperid=1103, ts=1103, title="modelWithReasons 3"),
            Model(hyperid=1104, ts=1104, title="modelWithReasons 4"),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1101).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1102).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1103).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1104).respond(0.6)

        cls.index.offers += [
            Offer(hyperid=1100),
            Offer(hyperid=1101),
            Offer(hyperid=1102),
            Offer(hyperid=1103),
            Offer(hyperid=1104),
        ]

    @classmethod
    def prepare_popular_products(cls):
        """
        Конфигурация для получения хотя бы одной модели на выдаче popular_products
        """
        cls.index.models += [
            Model(hyperid=11, hid=111),
        ]
        cls.index.offers.append(Offer(hyperid=11))
        cls.index.hypertree += [
            HyperCategory(hid=111, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=101, output_type=HyperCategoryType.GURU),
        ]
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1111').respond(
            {
                'models': ['11', str(YANDEX_STATION_ID)],
            }
        )
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:1111', item_count=40, with_timestamps=True
        ).respond(
            {
                'models': ['11', str(YANDEX_STATION_ID)],
                'timestamps': ['1', '2'],
            }
        )

    def test_popular_products(self):
        response = self.report.request_json(
            "place=popular_products&yandexuid=1111&rearr-factors=split=1&rearr-factors=switch_popular_products_to_dj_no_nid_check=0"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 11, "reasonsToBuy": [{"key1": "value1"}, {"key2": "value2"}]},
                    {
                        "id": YANDEX_STATION_ID,
                        "reasonsToBuy": [
                            {"id": "viewed_n_times", "value": "100500"},
                            {"id": "compatible_with_alisa", "value": "1"},
                        ],
                    },
                ]
            },
            preserve_order=False,
        )

        response = self.report.request_json(
            "place=popular_products&yandexuid=1111&rearr-factors=split=2&rearr-factors=switch_popular_products_to_dj_no_nid_check=0"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 11, "reasonsToBuy": NoKey("reasonsToBuy")},
                    {"id": YANDEX_STATION_ID, "reasonsToBuy": NoKey("reasonsToBuy")},
                ]
            },
            preserve_order=False,
        )

    @classmethod
    def prepare_prime(cls):
        """
        Данные для непустой выдачи place=prime
        """
        cls.index.offers += [
            Offer(hyperid=1, price=100, title="good phone"),
        ]

    def test_prime(self):
        response = self.report.request_json("place=prime&rearr-factors=split=1&text=phone")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 1, "reasonsToBuy": [{"key1": "value1"}, {"key2": "value2"}]},
                ]
            },
            preserve_order=False,
        )

        response = self.report.request_json("place=prime&rearr-factors=split=1&text=Станция")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": YANDEX_STATION_ID, "reasonsToBuy": [{"id": "viewed_n_times", "value": "100500"}]},
                ]
            },
            preserve_order=False,
        )

        response = self.report.request_json("place=prime&rearr-factors=split=1&text=Станция&rgb=blue")

        print(response)

    def test_product_accessories(self):
        response = self.report.request_json(
            "place=product_accessories&hyperid=3&rearr-factors=split=1;market_disable_product_accessories=0"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 1, "reasonsToBuy": [{"key1": "value1"}, {"key2": "value2"}]},
                    {"id": YANDEX_STATION_ID, "reasonsToBuy": [{"id": "viewed_n_times", "value": "100500"}]},
                ]
            },
            preserve_order=False,
        )

    @classmethod
    def prepare_also_viewed(cls):
        """
        Конфигурация для получения непустой выдачи also_viewed
        """
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_PRODUCTS,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(params={'version': '66'}, splits=[{'split': '1'}]),
                    YamarecSettingPartition(params={'version': '66'}, splits=[{'split': '5'}]),
                    YamarecSettingPartition(params={'version': '66'}, splits=[{'split': '6'}]),
                ],
            ),
        ]
        cls.recommender.on_request_accessory_models(model_id=2, item_count=1000, version='66').respond(
            {'models': ['1', str(YANDEX_STATION_ID)]}
        )

    def test_also_viewed(self):
        response = self.report.request_json('place=also_viewed&rearr-factors=split=1&hyperid=2')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 1, "reasonsToBuy": [{"key1": "value1"}, {"key2": "value2"}]},
                    {"id": YANDEX_STATION_ID, "reasonsToBuy": [{"id": "viewed_n_times", "value": "100500"}]},
                ]
            },
            preserve_order=False,
        )

    def test_modelinfo(self):
        """
        Проверка причин в modelinfo
        """
        response = self.report.request_json("place=modelinfo&rearr-factors=split=1&text=phone&rids=213&hyperid=1")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 1, "reasonsToBuy": [{"key1": "value1"}, {"key2": "value2"}]},
                ]
            },
            preserve_order=False,
        )

        response = self.report.request_json("place=modelinfo&rearr-factors=split=1&rids=213&hyperid=1100")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 1100, "reasonsToBuy": [{"id": "hype_goods", "value": "1"}]},
                ]
            },
            preserve_order=False,
        )

        response = self.report.request_json(
            "place=modelinfo&rearr-factors=split=1&rids=213&hyperid={}".format(YANDEX_STATION_ID)
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": YANDEX_STATION_ID, "reasonsToBuy": [{"id": "viewed_n_times", "value": "100500"}]},
                ]
            },
            preserve_order=False,
        )

        response = self.report.request_json(
            "place=modelinfo"
            "&rearr-factors=split=1;market_reasons_to_buy_viewed_banned_models={banned}"
            "&rids=213&hyperid={hyperid}".format(
                hyperid=YANDEX_STATION_ID, banned=','.join(['1', '2', str(YANDEX_STATION_ID)])
            )
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": YANDEX_STATION_ID, "reasonsToBuy": []},
                ]
            },
            preserve_order=False,
        )

    @classmethod
    def prepare_blue_reasons(cls):
        """
        Данные о причинах купить товар на Синем
        """
        cls.index.models += [
            Model(hyperid=2088101, hid=2088100, title="Good camera"),
            Model(hyperid=2088102, hid=2088100),
            Model(hyperid=2088103, hid=2088100, analogs=[2088101, YANDEX_STATION_ID]),
        ]

        cls.index.shops += [
            Shop(
                fesh=2088111,
                datafeed_id=2088110,
                priority_region=213,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=2088101, price=100, title="good camera", fesh=2088111, cpa=Offer.CPA_REAL),
            Offer(hyperid=2088102, price=200, fesh=2088111, cpa=Offer.CPA_REAL),
        ]

        cls.index.reference_shops += [
            ReferenceShop(hid=2088100, fesh=2088111),
        ]

        cls.index.mskus += [
            MarketSku(
                title="Good camera",
                hyperid=2088101,
                sku=208810101,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=2088110),
                ],
            ),
            MarketSku(
                title="Яндекс Станция",
                hyperid=YANDEX_STATION_ID,
                sku=208810102,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=2088110),
                ],
            ),
        ]

        cls.index.yamarec_places += [
            YamarecPlaceReasonsToBuy(blue=True)
            .new_partition("split=4")
            .add(hyperid=2088101, reasons=[{"key1": "value1"}, {"key2": "value2"}])
            .add(hyperid=20881011, reasons=[{"key1": "value1"}, {"key2": "value2"}])
            .add(hyperid=2088102, reasons=[])
            .add(hyperid=YANDEX_STATION_ID, reasons=[{"key1": "value1"}, {"key2": "value2"}])
        ]

    def test_modelinfo_blue(self):
        """
        Проверка причин в modelinfo на Синем
        MARKETRECOM-3612 возвращаются белые причины независимо от rgb
        """
        response = self.report.request_json(
            "place=modelinfo&rgb=blue&rearr-factors=split=4&text=camera&rids=213&hyperid=2088101"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 2088101, "reasonsToBuy": [{"key1_white": "value1"}, {"key2_white": "value2"}]},
                ]
            },
            preserve_order=False,
        )

        response = self.report.request_json(
            "place=modelinfo&rearr-factors=split=4&text=camera&rids=213&hyperid=2088101"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 2088101, "reasonsToBuy": [{"key1_white": "value1"}, {"key2_white": "value2"}]},
                ]
            },
            preserve_order=False,
        )

        response = self.report.request_json(
            "place=modelinfo&rgb=blue&rearr-factors=split=4&rids=213&hyperid={}".format(YANDEX_STATION_ID)
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": YANDEX_STATION_ID, "reasonsToBuy": NoKey("reasonsToBuy")},
                ]
            },
            preserve_order=False,
        )

    def test_prime_blue(self):
        """
        Проверка причин в prime на Синем
        MARKETRECOM-3612 возвращаются белые причины независимо от rgb
        """
        response = self.report.request_json("place=prime&rgb=blue&rearr-factors=split=4&text=camera")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 2088101,
                        "reasonsToBuy": [{"key1_white": "value1"}, {"key2_white": "value2"}],
                    },
                ]
            },
            preserve_order=False,
        )

        response = self.report.request_json("place=prime&rgb=blue&rearr-factors=split=4&text=Станция")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": YANDEX_STATION_ID, "reasonsToBuy": NoKey("reasonsToBuy")},
                ]
            },
            preserve_order=False,
        )

    def test_reasons_to_buy_report_filtering(self):
        """Testing that reasons with reasonId in ["best_by_factor", "customers_choice", "positive_feedback"]
        are not filtered for parallel
        https://st.yandex-team.ru/MARKETOUT-31043
        """
        # Старый формат, без полей с порогами
        request = (
            'place=parallel&text=modelWithReasons&rearr-factors=split=1;market_implicit_model_wizard_author_info=0;'
        )
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "modelWithReasons 1"}}},
                                "reasonsToBuy": ElementCount(1),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "modelWithReasons 2"}}},
                                "reasonsToBuy": ElementCount(1),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "modelWithReasons 3"}}},
                                "reasonsToBuy": ElementCount(3),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "modelWithReasons 4"}}},
                                "reasonsToBuy": ElementCount(1),
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
        )

        # Новый формат, с включенной фильтрацией
        request = (
            'place=parallel&text=modelWithReasons&rearr-factors=split=3;market_implicit_model_wizard_author_info=0;'
        )
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "modelWithReasons 1"}}},
                                "reasonsToBuy": ElementCount(1),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "modelWithReasons 2"}}},
                                "reasonsToBuy": ElementCount(1),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "modelWithReasons 3"}}},
                                "reasonsToBuy": ElementCount(3),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "modelWithReasons 4"}}},
                                "reasonsToBuy": ElementCount(1),
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
        )

        # Новый формат, с отключением фильтрации
        request += "market_parallel_more_reasons_to_buy=1;"
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "modelWithReasons 1"}}},
                                "reasonsToBuy": ElementCount(3),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "modelWithReasons 2"}}},
                                "reasonsToBuy": ElementCount(3),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "modelWithReasons 3"}}},
                                "reasonsToBuy": ElementCount(3),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "modelWithReasons 4"}}},
                                "reasonsToBuy": ElementCount(3),
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
        )

    def test_reasons_to_buy_format(self):
        """Testing new format of some reasons to buy with threshods
        https://st.yandex-team.ru/MARKETOUT-31043
        """
        request = 'place=parallel&text=modelWithReasons&rearr-factors=split=3;market_parallel_more_reasons_to_buy=1;market_implicit_model_wizard_author_info=0;'
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "modelWithReasons 1"}}},
                                "reasonsToBuy": [
                                    {
                                        "id": "positive_feedback",
                                        "type": "consumerfactor",
                                        "author_puid": "1001",
                                        "text": "text1",
                                        "anonymous": "true",
                                        "value": 5,
                                        "value_threshold": "4.0",
                                    },
                                    {
                                        "id": "best_by_factor",
                                        "type": "consumerfactor",
                                        "factor_id": "1",
                                        "factor_priority": "1",
                                        "factor_name": "factorname1",
                                        "value": 0.7,
                                        "value_threshold": "0.8",
                                    },
                                    {
                                        "id": "customers_choice",
                                        "type": "consumerfactor",
                                        "rating": "4.5",
                                        "rating_threshold": "4.0",
                                        "value": 0.95,
                                        "recommenders_count": "100",
                                        "share_threshold": "0.8",
                                    },
                                ],
                            }
                        ]
                    }
                }
            },
        )

    def test_ban_views_and_purchases_reasons(self):
        def get_ban_reasons_to_buy_rearr(viewed_n_times=0, bought_n_times=0, bestseller=0):
            return (
                "ban_viewed_n_times_reason_to_buy={viewed_n_times};"
                "ban_bought_n_times_reason_to_buy={bought_n_times};"
                "ban_bestseller_reason_to_buy={bestseller}".format(
                    viewed_n_times=viewed_n_times,
                    bought_n_times=bought_n_times,
                    bestseller=bestseller,
                )
            )

        def do_test(req):
            flag_to_reason = {
                "&rearr-factors="
                + get_ban_reasons_to_buy_rearr(viewed_n_times=1): [
                    {"id": "bought_n_times", "value": "100500"},
                    {"id": "bestseller", "value": "100500"},
                ],
                "&rearr-factors="
                + get_ban_reasons_to_buy_rearr(bought_n_times=1): [
                    {"id": "viewed_n_times", "value": "100500"},
                    {"id": "bestseller", "value": "100500"},
                ],
                "&rearr-factors="
                + get_ban_reasons_to_buy_rearr(bestseller=1): [
                    {"id": "viewed_n_times", "value": "100500"},
                    {"id": "bought_n_times", "value": "100500"},
                ],
            }
            all_disabled = "&rearr-factors=" + get_ban_reasons_to_buy_rearr()
            response = self.report.request_json(req + all_disabled)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "id": 1,
                            "reasonsToBuy": [
                                {"id": "viewed_n_times", "value": "100500"},
                                {"id": "bought_n_times", "value": "100500"},
                                {"id": "bestseller", "value": "100500"},
                            ],
                        },
                    ]
                },
                preserve_order=False,
            )
            for flag, reasons in flag_to_reason.items():
                response = self.report.request_json(req + flag)
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {"id": 1, "reasonsToBuy": reasons},
                        ]
                    },
                    preserve_order=False,
                )

            # If no rearrs enabled, check that bestseller and bought_n_times are banned
            response = self.report.request_json(req)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "id": 1,
                            "reasonsToBuy": [
                                {"id": "viewed_n_times", "value": "100500"},
                            ],
                        },
                    ]
                },
                preserve_order=False,
            )

        # popular_products
        response = self.report.request_json(
            "place=popular_products&yandexuid=1111&rearr-factors=split=5&rearr-factors=switch_popular_products_to_dj_no_nid_check=0"
            + ';'
            + get_ban_reasons_to_buy_rearr()
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": YANDEX_STATION_ID, "reasonsToBuy": [{"id": "viewed_n_times", "value": "100500"}]},
                ]
            },
            preserve_order=False,
        )
        response = self.report.request_json(
            "place=popular_products&yandexuid=1111&rearr-factors=split=5&rearr-factors=switch_popular_products_to_dj_no_nid_check=0"
            "&rearr-factors=" + get_ban_reasons_to_buy_rearr(viewed_n_times=1)
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": YANDEX_STATION_ID, "reasonsToBuy": NoKey("reasonsToBuy")},
                ]
            },
            preserve_order=False,
        )

        # prime
        do_test("place=prime&rearr-factors=split=6&text=phone")

        # product_accessories
        do_test("place=product_accessories&hyperid=3&rearr-factors=split=6;market_disable_product_accessories=0")

        # also_viewed
        do_test('place=also_viewed&rearr-factors=split=6&hyperid=2')

        # model_info
        do_test("place=modelinfo&rearr-factors=split=6&rids=213&hyperid=1")


if __name__ == "__main__":
    main()
