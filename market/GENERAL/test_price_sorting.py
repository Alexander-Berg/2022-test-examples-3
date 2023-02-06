#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    DeliveryBucket,
    DeliveryOption,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    MnPlace,
    Model,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Promo,
    PromoType,
    RegionalDelivery,
    RegionalModel,
    Shop,
    UngroupedModel,
    VCluster,
    VirtualModel,
)
from core.testcase import TestCase, main
from core.logs import ErrorCodes
from datetime import datetime


class T(TestCase):

    PRIORITY_FESH = 2201  # for region 66
    COUNTRY_FESH = 2202  # for region 66

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.offers += [
            Offer(hyperid=201, price=100, hid=2),
            Offer(hyperid=201, price=200, hid=2),
            Offer(hyperid=201, price=300, hid=2),
            Offer(hyperid=202, price=50, hid=2),
            Offer(hyperid=202, price=100, hid=2),
            Offer(hyperid=202, price=1000, hid=2),
            Offer(price=10, hid=2),
            Offer(price=10000, hid=2),
        ]

        cls.index.shops += [
            Shop(fesh=1, priority_region=225),
            Shop(fesh=2, priority_region=213, regions=[225]),
            Shop(fesh=3, priority_region=213, regions=[225], pickup_buckets=[5003]),
            Shop(fesh=4, priority_region=214, regions=[225], pickup_buckets=[5004]),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=4,
                fesh=4,
                carriers=[1],
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=5)]),
                ],
            )
        ]

        cls.index.outlets += [
            Outlet(fesh=3, region=213, point_type=Outlet.FOR_PICKUP, point_id=3),
            Outlet(fesh=4, region=213, point_type=Outlet.FOR_PICKUP, point_id=4),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5003,
                fesh=3,
                carriers=[99],
                options=[PickupOption(outlet_id=3)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5004,
                fesh=4,
                carriers=[99],
                options=[PickupOption(outlet_id=4)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        # hid=3 is non-leaf, hid=4 is leaf
        cls.index.hypertree += [
            HyperCategory(
                hid=3,
                children=[
                    HyperCategory(hid=4),
                ],
            )
        ]

        cls.index.offers += [
            Offer(price=100, ts=1, title="podshybnik offer 1", fesh=1),
            Offer(price=200, ts=2, title="podshybnik offer 2", fesh=1),
            Offer(price=300, ts=3, title="podshybnik offer 3", fesh=1),
            Offer(
                price=25, ts=10, vclusterid=1000000107, pricefrom=True, title="podshybnik offer for cluster 7", fesh=1
            ),
            Offer(
                price=123, ts=11, vclusterid=1000000108, pricefrom=True, title="podshybnik offer for cluster 8", fesh=1
            ),
            Offer(
                price=550, ts=12, vclusterid=1000000109, pricefrom=True, title="podshybnik offer for cluster 9", fesh=1
            ),
            Offer(price=1, ts=13, hid=3, title="nadzatylnik offer 1"),
            Offer(price=2, ts=14, hid=3, title="nadzatylnik offer 2"),
            Offer(price=1, ts=15, hid=4, title="nadzatylnik for children offer 1"),
            Offer(price=2, ts=16, hid=4, title="nadzatylnik for children offer 2"),
        ]

        cls.index.models += [
            Model(hyperid=4, ts=4, title="podshybnik model 4"),
            Model(hyperid=5, ts=5, title="podshybnik model 5"),
            Model(hyperid=6, ts=6, title="podshybnik model 6"),
        ]

        cls.index.offers += [
            Offer(fesh=1, hyperid=4, price=50),
            Offer(fesh=1, hyperid=5, price=250),
            Offer(fesh=1, hyperid=6, price=500),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=4, price_min=50, price_max=50, price_med=50, rids=[225], offers=1, onstock=1),
            RegionalModel(hyperid=5, price_min=250, price_max=250, price_med=250, rids=[225], offers=1, onstock=1),
            RegionalModel(hyperid=6, price_min=500, price_max=500, price_med=500, rids=[225], offers=1, onstock=1),
        ]

        cls.index.vclusters += [
            VCluster(vclusterid=1000000107, ts=7, title="podshybnik cluster 7"),
            VCluster(vclusterid=1000000108, ts=8, title="podshybnik cluster 8"),
            VCluster(vclusterid=1000000109, ts=9, title="podshybnik cluster 9"),
        ]

        cls.index.models += [
            Model(hyperid=7, ts=17, title="molotok model 1", hid=7),
            Model(hyperid=8, ts=18, title="molotok model 2", hid=7),
            Model(hyperid=9, ts=19, title="molotok model 3", hid=7),
        ]

        cls.index.vclusters += [
            VCluster(vclusterid=1000000110, ts=20, title="molotok cluster 1", hid=77),
            VCluster(vclusterid=1000000111, ts=21, title="molotok cluster 2", hid=77),
            VCluster(vclusterid=1000000112, ts=22, title="molotok cluster 3", hid=77),
        ]

        cls.index.offers += [
            Offer(ts=23, title="molotok offer 1", hid=7),
            Offer(ts=24, title="molotok offer 2", hid=7),
            Offer(ts=25, title="molotok offer 3", hid=7),
        ]

        cls.index.offers += [Offer(vclusterid=1000000110), Offer(vclusterid=1000000111), Offer(vclusterid=1000000112)]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.75)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5).respond(0.92)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 6).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 7).respond(0.49)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8).respond(0.91)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 9).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10).respond(0.49)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 11).respond(0.91)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 12).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 13).respond(0.14)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 14).respond(0.16)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 15).respond(0.14)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 16).respond(0.16)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 17).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 18).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 19).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 20).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 21).respond(None)  # invalid_mn_value
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 22).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 23).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 24).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 25).respond(None)  # invalid_mn_value

        cls.index.models += [Model(hyperid=10, hid=100)]

        cls.index.offers += [
            Offer(
                hyperid=10,
                ts=7001,
                price=110,
                fesh=2,
                title='Local delivery',
                has_delivery_options=True,
                pickup=False,
                delivery_options=[DeliveryOption(price=10)],
            ),
            Offer(
                hyperid=10, ts=7002, price=125, fesh=3, title='Local pickup', has_delivery_options=False, pickup=True
            ),
            Offer(
                hyperid=10,
                ts=7003,
                price=103,
                fesh=3,
                title='Local delivery + pickup',
                has_delivery_options=True,
                pickup=True,
                delivery_options=[DeliveryOption(price=20)],
            ),
            Offer(
                hyperid=10,
                ts=7004,
                price=120,
                fesh=4,
                title='Remote delivery',
                has_delivery_options=True,
                pickup=False,
                delivery_options=[DeliveryOption(price=0)],
                delivery_buckets=[4],
            ),
            Offer(
                hyperid=10, ts=7005, price=100, fesh=4, title='Remote pickup', has_delivery_options=False, pickup=True
            ),
            Offer(
                hyperid=10,
                ts=7006,
                price=113,
                fesh=4,
                title='Remote delivery + pickup',
                has_delivery_options=True,
                pickup=True,
                delivery_options=[DeliveryOption(price=5)],
            ),
            Offer(
                hyperid=10,
                ts=7007,
                price=105,
                fesh=4,
                title='Remote delivery without sis',
                has_delivery_options=True,
                pickup=False,
                delivery_options=[DeliveryOption(price=0)],
            ),
        ]

    def test_aprice_prime(self):
        response = self.report.request_json('place=prime&hid=2&how=aprice')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 202},
                    {"entity": "product", "id": 201},
                    {"entity": "offer", "prices": {"value": "10"}},
                ]
            },
            preserve_order=True,
        )

    def test_aprice_product_offers(self):
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json(
            'place=productoffers&hyperid=10&how=aprice&rids=213&deliveryincluded=0' + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "Local delivery + pickup"}, "prices": {"value": "103", "rawValue": "103"}},
                    {
                        "titles": {"raw": "Local delivery"},
                        "prices": {"value": "110", "rawValue": "110"},
                    },
                    {
                        "titles": {"raw": "Local pickup"},
                        "prices": {"value": "125", "rawValue": "125"},
                    },
                    {"entity": "regionalDelimiter"},
                    {
                        "titles": {"raw": "Remote pickup"},
                        "prices": {"value": "100", "rawValue": "100"},
                    },
                    {
                        "titles": {"raw": "Remote delivery without sis"},
                        "prices": {"value": "105", "rawValue": "105"},
                    },
                    {
                        "titles": {"raw": "Remote delivery + pickup"},
                        "prices": {"value": "113", "rawValue": "113"},
                    },
                    {
                        "titles": {"raw": "Remote delivery"},
                        "prices": {"value": "120", "rawValue": "120"},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # к офферам добавляется стоимость доставки (если у оффера вообще есть доставка и известна ее стоимость)
        # офферы без сроков и стоимости доставки пессимизируются
        response = self.report.request_json(
            'place=productoffers&hyperid=10&how=aprice&rids=213&deliveryincluded=1' + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "Local delivery"},
                        "prices": {"value": "120", "rawValue": "110"},
                    },
                    {"titles": {"raw": "Local delivery + pickup"}, "prices": {"value": "123", "rawValue": "103"}},
                    {
                        "titles": {"raw": "Local pickup"},
                        "prices": {"value": "125", "rawValue": "125"},
                    },
                    {"entity": "regionalDelimiter"},
                    {
                        "titles": {"raw": "Remote pickup"},
                        "prices": {"value": "100", "rawValue": "100"},
                    },
                    {
                        "titles": {"raw": "Remote delivery + pickup"},
                        "prices": {"value": "113", "rawValue": "113"},
                    },
                    {
                        "titles": {"raw": "Remote delivery"},
                        "prices": {"value": "125", "rawValue": "120"},
                    },
                    {
                        "titles": {"raw": "Remote delivery without sis"},
                        "prices": {"value": "105", "rawValue": "105"},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_dprice_product_offers(self):
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json(
            'place=productoffers&hyperid=10&how=dprice&rids=213&deliveryincluded=0' + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "Local pickup"},
                        "prices": {"value": "125", "rawValue": "125"},
                    },
                    {
                        "titles": {"raw": "Local delivery"},
                        "prices": {"value": "110", "rawValue": "110"},
                    },
                    {"titles": {"raw": "Local delivery + pickup"}, "prices": {"value": "103", "rawValue": "103"}},
                    {"entity": "regionalDelimiter"},
                    {
                        "titles": {"raw": "Remote delivery"},
                        "prices": {"value": "120", "rawValue": "120"},
                    },
                    {
                        "titles": {"raw": "Remote delivery + pickup"},
                        "prices": {"value": "113", "rawValue": "113"},
                    },
                    {
                        "titles": {"raw": "Remote delivery without sis"},
                        "prices": {"value": "105", "rawValue": "105"},
                    },
                    {
                        "titles": {"raw": "Remote pickup"},
                        "prices": {"value": "100", "rawValue": "100"},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # к офферам добавляется стоимость доставки (если у оффера вообще есть доставка и известна ее стоимость)
        # офферы без сроков и стоимости доставки (но с доставкой) - пессимизируются
        response = self.report.request_json(
            'place=productoffers&hyperid=10&how=dprice&rids=213&deliveryincluded=1' + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "Local pickup"},
                        "prices": {"value": "125", "rawValue": "125"},
                    },
                    {"titles": {"raw": "Local delivery + pickup"}, "prices": {"value": "123", "rawValue": "103"}},
                    {
                        "titles": {"raw": "Local delivery"},
                        "prices": {"value": "120", "rawValue": "110"},
                    },
                    {"entity": "regionalDelimiter"},
                    {
                        "titles": {"raw": "Remote delivery"},
                        "prices": {"value": "125", "rawValue": "120"},
                    },
                    {
                        "titles": {"raw": "Remote delivery + pickup"},
                        "prices": {"value": "113", "rawValue": "113"},
                    },
                    {
                        "titles": {"raw": "Remote pickup"},
                        "prices": {"value": "100", "rawValue": "100"},
                    },
                    {
                        "titles": {"raw": "Remote delivery without sis"},
                        "prices": {"value": "105", "rawValue": "105"},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_dprice_prime(self):
        response = self.report.request_json('place=prime&hid=2&how=dprice' '&pp=18')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 201},
                    {"entity": "product", "id": 202},
                    {"entity": "offer", "prices": {"value": "10000"}},
                    {"entity": "offer", "prices": {"value": "1000"}},
                ]
            },
            preserve_order=True,
        )

    def test_aprice_sort(self):
        # TODO after MARKETOUT-7170 will not be under the flag,
        # remove this test

        response = self.report.request_json('place=prime&how=aprice' '&text=podshybnik&rids=225' '&numdoc=12')

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "titles": {"raw": "podshybnik cluster 7"}, "prices": {"min": "25"}},
                    {"entity": "product", "titles": {"raw": "podshybnik model 4"}, "prices": {"min": "50"}},
                    {"entity": "product", "titles": {"raw": "podshybnik cluster 8"}, "prices": {"min": "123"}},
                    {"entity": "product", "titles": {"raw": "podshybnik model 5"}, "prices": {"min": "250"}},
                    {"entity": "product", "titles": {"raw": "podshybnik model 6"}, "prices": {"min": "500"}},
                    {"entity": "product", "titles": {"raw": "podshybnik cluster 9"}, "prices": {"min": "550"}},
                    {"entity": "offer", "titles": {"raw": "podshybnik offer for cluster 7"}, "prices": {"min": "25"}},
                    {"entity": "offer", "titles": {"raw": "podshybnik offer 1"}, "prices": {"value": "100"}},
                    {"entity": "offer", "titles": {"raw": "podshybnik offer for cluster 8"}, "prices": {"min": "123"}},
                    {"entity": "offer", "titles": {"raw": "podshybnik offer 2"}, "prices": {"value": "200"}},
                    {"entity": "offer", "titles": {"raw": "podshybnik offer 3"}, "prices": {"value": "300"}},
                    {"entity": "offer", "titles": {"raw": "podshybnik offer for cluster 9"}, "prices": {"min": "550"}},
                ]
            },
            preserve_order=True,
        )

    def test_dprice_sort(self):
        # TODO after MARKETOUT-7170 will not be under the flag,
        # remove this test

        response = self.report.request_json('place=prime&how=dprice&text=podshybnik&rids=225&numdoc=12')

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "titles": {"raw": "podshybnik cluster 9"},
                        "offers": {"count": 1},
                        "prices": {"min": "550"},
                    },
                    {"entity": "product", "titles": {"raw": "podshybnik model 6"}, "prices": {"max": "500"}},
                    {"entity": "product", "titles": {"raw": "podshybnik model 5"}, "prices": {"max": "250"}},
                    {"entity": "product", "titles": {"raw": "podshybnik cluster 8"}, "prices": {"min": "123"}},
                    {"entity": "product", "titles": {"raw": "podshybnik model 4"}, "prices": {"max": "50"}},
                    {"entity": "product", "titles": {"raw": "podshybnik cluster 7"}, "prices": {"min": "25"}},
                    {"entity": "offer", "titles": {"raw": "podshybnik offer for cluster 9"}, "prices": {"min": "550"}},
                    {"entity": "offer", "titles": {"raw": "podshybnik offer 3"}, "prices": {"value": "300"}},
                    {"entity": "offer", "titles": {"raw": "podshybnik offer 2"}, "prices": {"value": "200"}},
                    {"entity": "offer", "titles": {"raw": "podshybnik offer for cluster 8"}, "prices": {"min": "123"}},
                    {"entity": "offer", "titles": {"raw": "podshybnik offer 1"}, "prices": {"value": "100"}},
                    {"entity": "offer", "titles": {"raw": "podshybnik offer for cluster 7"}, "prices": {"min": "25"}},
                ]
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_price_sort_use_model_price_for_offer_experiments_prime_data(cls):
        """Создаем офферы с моделями с различными ценами.
        Оферы создаем с разными значениями pickup,
        чтобы при фильтрации офферов уходили дешевые (дорогие)
        офферы модели. Оферы в гуру-категории, чтобы применялся коллапсер.
        """
        cls.index.hypertree += [
            HyperCategory(hid=10, output_type=HyperCategoryType.GURU),
        ]

        cls.index.models += [
            Model(hid=10, hyperid=210, title='iphone model 1'),
            Model(hid=10, hyperid=211, title='iphone model 2'),
            Model(hid=10, hyperid=212, title='iphone model 3'),
            Model(hid=10, hyperid=213, title='iphone model 4'),
            Model(hid=10, hyperid=214, title='iphone model 5'),
            Model(hid=10, hyperid=215, title='iphone model 6'),
        ]

        cls.index.offers += [
            Offer(hid=10, hyperid=210, title='iphone offer 11', price=100),
            Offer(hid=10, hyperid=210, title='iphone offer 12', price=55, pickup=False),
            Offer(hid=10, hyperid=211, title='iphone offer 21', price=90),
            Offer(hid=10, hyperid=212, title='iphone offer 31', price=80),
            Offer(hid=10, hyperid=213, title='iphone offer 41', price=70),
            Offer(hid=10, hyperid=213, title='iphone offer 42', price=56, pickup=False),
            Offer(hid=10, hyperid=214, title='iphone offer 51', price=60),
            Offer(hid=10, hyperid=215, title='iphone offer 61', price=50),
        ]

    def test_aprice_sort_prime_relev(self):
        """Проверяем для place=prime, что при включенном коллапсере
        для how=aprice поля релевантности стоят в нужном порядке.
        """
        response = self.report.request_json(
            "place=prime&text=iphone&numdoc=3&page=1" "&allow-collapsing=1&how=aprice&debug=1"
        )
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "rank": [
                        {"name": "ONSTOCK"},
                        {"name": "DELIVERY_TYPE"},
                        {"name": "PRICE"},
                        {"name": "RANDX"},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_dprice_sort_prime_relev(self):
        """Проверяем для place=prime, что при включенном коллапсере
        для how=dprice поля релевантности стоят в нужном порядке.
        """
        response = self.report.request_json(
            "place=prime&text=iphone&numdoc=3&page=1" "&allow-collapsing=1&how=dprice&debug=1"
        )
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "rank": [
                        {"name": "ONSTOCK"},
                        {"name": "DELIVERY_TYPE"},
                        {"name": "PRICE"},
                        {"name": "RANDX"},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_quality_opinions_sort_prime_relev(self):
        """Проверяем, что для place=prime для сортровки how=quality_opinions,
        поля стоят в нужном порядке при включенном и выключенном схлопывании
        """

        for allow_collapsing in ['0', '1']:
            url = 'place=prime&text=iphone&numdoc=3&page=1&allow-collapsing={}&how=quality_opinions&debug=1'.format(
                allow_collapsing
            )

            response = self.report.request_json(url)
            self.assertFragmentIn(
                response,
                {
                    "debug": {
                        "rank": [
                            {"name": "DELIVERY_TYPE"},
                            {"name": "DOCUMENT_RATING_10"},
                            {"name": "OPINIONS_WITH_PREVALENCE"},
                            {"name": "ONSTOCK"},
                            {"name": "GURU_POPULARITY"},
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_quality_sort_prime_relev(self):
        """Проверяем, что для place=prime для сортровки how=quality,
        поля стоят в нужном порядке при включенном и выключенном схлопывании
        """

        for allow_collapsing in ['0', '1']:
            url = 'place=prime&text=iphone&numdoc=3&page=1&allow-collapsing={}&how=quality&debug=1'.format(
                allow_collapsing
            )

            response = self.report.request_json(url)
            self.assertFragmentIn(
                response,
                {
                    "debug": {
                        "rank": [
                            {"name": "DELIVERY_TYPE"},
                            {"name": "DOCUMENT_RATING"},
                            {"name": "ONSTOCK"},
                            {"name": "GURU_POPULARITY"},
                            {"name": "RANDX"},
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_aprice_sort_no_model_boost_experiment_prime_relev(self):
        """Проверяем для place=prime, что при включенном флаге market_use_model_boosting_on_price_sort
        убирается бустинг моделей для how=aprice при отключенном коллапсере.
        """
        url = "place=prime&text=iphone&numdoc=3&page=1&allow-collapsing=0&how=aprice&debug=1"

        # Проверяем, что модели бустятся по умолчанию
        response = self.report.request_json(url)
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "rank": [
                        {"name": "ONSTOCK"},
                        {"name": "IS_MODEL"},
                        {"name": "DELIVERY_TYPE"},
                        {"name": "PRICE"},
                        {"name": "RANDX"},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Проверяем, что убран бустинг моделей в эксперименте market_use_model_boosting_on_price_sort
        response = self.report.request_json(url + '&rearr-factors=market_use_model_boosting_on_price_sort=0')
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "rank": [
                        {"name": "ONSTOCK"},
                        {"name": "DELIVERY_TYPE"},
                        {"name": "PRICE"},
                        {"name": "RANDX"},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_dprice_sort_no_model_boost_experiment_prime_relev(self):
        """Проверяем для place=prime, что при включенном флаге market_use_model_boosting_on_price_sort
        убирается бустинг моделей для how=dprice при отключенном коллапсере.
        """
        url = "place=prime&text=iphone&numdoc=3&page=1&allow-collapsing=0&how=dprice&debug=1"

        # Проверяем, что модели бустятся по умолчанию
        response = self.report.request_json(url)
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "rank": [
                        {"name": "ONSTOCK"},
                        {"name": "IS_MODEL"},
                        {"name": "DELIVERY_TYPE"},
                        {"name": "PRICE"},
                        {"name": "RANDX"},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Проверяем, что убран бустинг моделей в эксперименте market_use_model_boosting_on_price_sort
        response = self.report.request_json(url + '&rearr-factors=market_use_model_boosting_on_price_sort=0')
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "rank": [
                        {"name": "ONSTOCK"},
                        {"name": "DELIVERY_TYPE"},
                        {"name": "PRICE"},
                        {"name": "RANDX"},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_zero_price(cls):
        cls.index.hypertree += [HyperCategory(hid=123, name="Продаем даром", output_type=HyperCategoryType.GURU)]

        cls.index.shops += [
            Shop(fesh=12301, priority_region=213, regions=[225]),
            Shop(fesh=12302, priority_region=213, regions=[225]),
            Shop(fesh=12303, priority_region=213, regions=[225]),
            Shop(fesh=12304, priority_region=2, regions=[225]),
            Shop(fesh=12305, priority_region=2, regions=[225]),
        ]

        cls.index.models += [
            Model(hid=123, hyperid=1020301, title='Хлам даром'),
            Model(hid=123, hyperid=1020302, title='Почти даром'),
            Model(hid=123, hyperid=1020303, title='Совсем даром но из Питера'),
            Model(hid=123, hyperid=1020304, title='Даром из Питера'),
        ]

        cls.index.offers += [
            Offer(hyperid=1020301, fesh=12301, title='Хлам даром за 100', price=100),
            Offer(hyperid=1020302, fesh=12302, title='Почти даром за 300', price=300),
            Offer(fesh=12303, title='Даром хрен знает что', price=200),
            Offer(hyperid=1020303, fesh=12304, title='Совсем даром из Питера за 30', price=30),
            Offer(hyperid=1020304, fesh=12305, title='Даром из Питера за 50', price=50),
        ]

    @classmethod
    def prepare_price_sort_respects_promocodes(cls):
        cls.index.offers += [
            Offer(price=100, hyperid=11),
            # real price is 200-120=80
            Offer(
                price=200,
                hyperid=11,
                promo=Promo(
                    promo_type=PromoType.PROMO_CODE,
                    start_date=datetime(1980, 1, 1),
                    end_date=datetime(2050, 1, 1),
                    key='f23qfbekwuanfejawbat',
                    url='http://my.url',
                    promo_code="my promo code",
                    discount_value=120,
                    discount_currency='RUR',
                ),
            ),
            Offer(price=300, hyperid=11),
            Offer(price=100, hid=5),
            # real price is 200*(1-0.55)=90
            Offer(
                price=200,
                hid=5,
                promo=Promo(
                    promo_type=PromoType.PROMO_CODE,
                    start_date=datetime(1980, 1, 1),
                    end_date=datetime(2050, 1, 1),
                    key='2943hro32g874',
                    url='http://my.url',
                    promo_code="my another promo code",
                    discount_value=55,
                ),
            ),
            Offer(price=300, hid=5),
        ]

    def test_price_sort_respects_promocodes(self):
        # aprice productoffers with hyperid
        response = self.report.request_json('place=productoffers&hyperid=11&how=aprice')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "prices": {"value": "200"},
                        "promos": [{"type": "promo-code", "discount": {"value": 120, "currency": "RUR"}}],
                    },
                    {
                        "prices": {"value": "100"},
                    },
                    {
                        "prices": {"value": "300"},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # aprice prime with hyperid
        response = self.report.request_json('place=prime&hyperid=11&how=aprice')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "prices": {"value": "200"},
                        "promos": [{"type": "promo-code", "discount": {"value": 120, "currency": "RUR"}}],
                    },
                    {
                        "prices": {"value": "100"},
                    },
                    {
                        "prices": {"value": "300"},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # aprice prime with hid
        response = self.report.request_json('place=prime&hid=5&how=aprice')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "prices": {"value": "200"},
                        "promos": [{"type": "promo-code", "discount": {"value": 55}}],
                    },
                    {
                        "prices": {"value": "100"},
                    },
                    {
                        "prices": {"value": "300"},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # dprice productoffers with hyperid
        response = self.report.request_json('place=productoffers&hyperid=11&how=dprice')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "prices": {"value": "300"},
                    },
                    {
                        "prices": {"value": "100"},
                    },
                    {
                        "prices": {"value": "200"},
                        "promos": [{"type": "promo-code", "discount": {"value": 120, "currency": "RUR"}}],
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # dprice prime with hyperid
        response = self.report.request_json('place=prime&hyperid=11&how=dprice')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "prices": {"value": "300"},
                    },
                    {
                        "prices": {"value": "100"},
                    },
                    {
                        "prices": {"value": "200"},
                        "promos": [{"type": "promo-code", "discount": {"value": 120, "currency": "RUR"}}],
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # dprice prime with hid
        response = self.report.request_json('place=prime&hid=5&how=dprice')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "prices": {"value": "300"},
                    },
                    {
                        "prices": {"value": "100"},
                    },
                    {
                        "prices": {"value": "200"},
                        "promos": [{"type": "promo-code", "discount": {"value": 55}}],
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_price_sort_ungrouped_docs_with_DO(cls):
        cls.index.models + [
            Model(
                hyperid=70101,
                hid=701,
                ungrouped_blue=[
                    UngroupedModel(
                        group_id=1,
                        title="ungrouped model with price=200",
                    ),
                    UngroupedModel(
                        group_id=2,
                        title="ungrouped model with price=400",
                    ),
                ],
            ),
        ]
        cls.index.mskus += [
            MarketSku(
                hid=701,
                hyperid=70101,
                sku=70102,
                title="sku with price=200",
                blue_offers=[BlueOffer(price=200)],
                ungrouped_model_blue=1,
            ),
            MarketSku(
                hid=701,
                hyperid=70101,
                sku=70103,
                title="sku with price=400",
                blue_offers=[BlueOffer(price=400)],
                ungrouped_model_blue=2,
            ),
        ]
        cls.index.offers += [
            Offer(hid=701, title="offer with price=100", price=100),
            Offer(hid=701, title="offer with price=300", price=300),
            Offer(hid=701, title="offer with price=500", price=500),
        ]

    def test_price_sort_ungrouped_docs_with_DO(self):
        """
        В индексе 2 рахлопнутых ску одной модели.
        Убеждаемся, что расхлопнутые ску сортируются по цене своих ДО.
        """
        response = self.report.request_json(
            "place=prime&hid=701&how=aprice" "&allow-ungrouping=1&allow-collapsing=1&use-default-offers=1"
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"titles": {"raw": "offer with price=100"}},
                        {"id": 70101, "offers": {"items": [{"titles": {"raw": "sku with price=200"}}]}},
                        {"titles": {"raw": "offer with price=300"}},
                        {"id": 70101, "offers": {"items": [{"titles": {"raw": "sku with price=400"}}]}},
                        {"titles": {"raw": "offer with price=500"}},
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_empty_models_pessimization_on_price_sort_in_dynstat_top(cls):
        cls.index.models += [
            Model(hid=101, hyperid=171, title="empty model with randx=1", randx=1),
            Model(hid=101, hyperid=172, title="empty model with randx=2", randx=2),
            Model(hid=101, hyperid=173, title="not empty model with min price 210"),
            Model(hid=101, hyperid=174, title="not empty model with min price 220"),
        ]
        cls.index.offers += [
            Offer(hyperid=173, title="model 173 offer with price 210", price=210),
            Offer(hyperid=174, title="model 174 offer with price 220", price=220),
            Offer(hid=101, title="offer with price 200", price=200),
            Offer(hid=101, title="offer with price 230", price=230),
        ]

    def test_empty_models_pessimization_on_price_sort_in_dynstat_top(self):
        """
        Проверяем, что пустые модели пессимизируются относительно моделей
        и офферов, попавших в топ с дин. статистиками
        """
        response = self.report.request_json("place=prime&hid=101&use-default-offers=1&allow-collapsing=1&how=aprice")
        self.assertFragmentIn(
            response,
            {
                "total": 6,
                "results": [
                    {"entity": "offer", "titles": {"raw": "offer with price 200"}},
                    {"id": 173, "titles": {"raw": "not empty model with min price 210"}},
                    {"id": 174, "titles": {"raw": "not empty model with min price 220"}},
                    {"entity": "offer", "titles": {"raw": "offer with price 230"}},
                    {"id": 172, "titles": {"raw": "empty model with randx=2"}},
                    {"id": 171, "titles": {"raw": "empty model with randx=1"}},
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json("place=prime&hid=101&use-default-offers=1&allow-collapsing=1&how=dprice")
        self.assertFragmentIn(
            response,
            {
                "total": 6,
                "results": [
                    {"entity": "offer", "titles": {"raw": "offer with price 230"}},
                    {"id": 174, "titles": {"raw": "not empty model with min price 220"}},
                    {"id": 173, "titles": {"raw": "not empty model with min price 210"}},
                    {"entity": "offer", "titles": {"raw": "offer with price 200"}},
                    {"id": 172, "titles": {"raw": "empty model with randx=2"}},
                    {"id": 171, "titles": {"raw": "empty model with randx=1"}},
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_empty_models_with_expired_stats_pessimization_on_price_sort(cls):
        cls.index.shops += [
            Shop(fesh=111, regions=[213]),
        ]
        cls.index.models += [
            Model(hid=111, hyperid=1171, title="empty model with expired min_price=100 randx=1", randx=1),
            Model(hid=111, hyperid=1172, title="empty model with expired min_price=100 randx=2", randx=2),
            Model(hid=111, hyperid=1173, title="not dynstat top model with min_price=210"),
            Model(hid=111, hyperid=1174, title="not dynstat top model with min_price=220"),
        ]
        # top60 состоит из моделей 1171 и 1172 и 58 офферов
        cls.index.offers += [
            Offer(hid=111, fesh=111, title="dynstat top offer with price=" + str(price), price=price)
            for price in range(100, 158)
        ]
        cls.index.offers += [
            Offer(hyperid=1173, fesh=111, title="model 1173 offer with price=210", price=210),
            Offer(hyperid=1174, fesh=111, title="model 1174 offer with price=220", price=220),
            Offer(hid=111, fesh=111, title="not dynstat top offer with price=300", price=300),
        ]
        cls.index.regional_models += [
            RegionalModel(hyperid=1171, price_min=100, offers=1, rids=[213]),
            RegionalModel(hyperid=1172, price_min=100, offers=1, rids=[213]),
        ]

    def test_empty_models_with_expired_stats_pessimization_on_price_sort(self):
        """
        https://st.yandex-team.ru/MARKETOUT-34607
        Проверяем, что пустые модели, попавшие в dynstatTop, пессимизируются относительно
        всех непустых документов.
        Пустая модель могла попасть в dynstatTop, если при формировании индекса она была непустой,
        а значит её региональные статистики тоже непустые. Но если вдруг магазин отключит офферы этой модели,
        то после запроса за дин. статистиками модель окажется пустой.
        """
        # Запрашиваю последнюю (2ю) страницу по 57 док-ов, чтобы захватить все доки,
        # не вошедшие в dynstatTop, и последний документ из dynstatTop.
        # Устанавливаю numdoc < 60, чтобы не переопределить кол-во доков для перезапроса статистик.
        response = self.report.request_json(
            "rids=213&place=prime&hid=111&allow-collapsing=1&how=aprice&numdoc=57&page=2"
            "&debug=da&local-offers-first=0"
        )
        self.assertFragmentIn(
            response,
            {
                "total": 63,
                "results": [
                    {"entity": "offer", "titles": {"raw": "dynstat top offer with price=157"}},
                    # ---- dynstat top with onstock=1 (58 docs) ----
                    {"id": 1173, "titles": {"raw": "not dynstat top model with min_price=210"}},
                    {"id": 1174, "titles": {"raw": "not dynstat top model with min_price=220"}},
                    {"entity": "offer", "titles": {"raw": "not dynstat top offer with price=300"}},
                    {"id": 1171, "titles": {"raw": "empty model with expired min_price=100 randx=1"}},
                    {"id": 1172, "titles": {"raw": "empty model with expired min_price=100 randx=2"}},
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )
        # Проверяем, что пустые модели попали в топ, а непустые не попали
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "1"}]}},
                    {"id": 1173, "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "0"}]}},
                    {"id": 1174, "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "0"}]}},
                    {"debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "0"}]}},
                    {"id": 1171, "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "1"}]}},
                    {"id": 1172, "debug": {"metaRank": [{"name": "IS_IN_DYNSTAT_TOP", "value": "1"}]}},
                ]
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_dynstat_top_price_sort_with_local_offers_first(cls):
        cls.index.shops += [
            Shop(fesh=cls.PRIORITY_FESH, priority_region=66),
            Shop(fesh=cls.COUNTRY_FESH, regions=[66]),
        ]

    @classmethod
    def prepare_dynstat_top_price_sort_with_local_offers_first__pessimize_not_top_priority_model(cls):
        cls.index.offers += [
            Offer(
                hid=220,
                price=price,
                fesh=cls.PRIORITY_FESH,
                title="search: dynstatTop offer / priority delivery / price=" + str(price),
            )
            for price in range(101, 160)
        ]
        cls.index.models += [
            Model(hyperid=2201, hid=220, title="search: dynstatTop model / priority delivery / price=301"),
            Model(hyperid=2202, hid=220, title="search: model / priority delivery / price=201"),
        ]
        cls.index.regional_models += [
            # Меняю модельные статистики, чтобы модель попапла в топ60
            RegionalModel(hyperid=2201, price_min=100, local_offers=1, offers=1, rids=[66]),
        ]
        cls.index.offers += [
            Offer(hyperid=2201, fesh=cls.PRIORITY_FESH, price=301),
            Offer(hyperid=2202, fesh=cls.PRIORITY_FESH, price=201),
        ]

    def test_dynstat_top_price_sort_with_local_offers_first__pessimize_not_top_priority_model(self):
        """
        https://st.yandex-team.ru/MARKETOUT-34607
        Среди 2х моделей с приоритетной доставкой, модель, не попавшая в dynstatTop, должна пессимизироваться.
        Даже если цена модели из dynstatTop после обновления статистик оказалась выше.
        """
        # Запрашиваю последнюю (2ю) страницу по 58 док-ов, чтобы захватить все доки,
        # не вошедшие в dynstatTop, и последние документы из dynstatTop.
        # Устанавливаю numdoc < 60, чтобы не переопределить кол-во доков для перезапроса статистик.
        response = self.report.request_json(
            "pp=18&place=prime&rids=66&hid=220&how=aprice&allow-collapsing=1&text=search"
            "&local-offers-first=1&page=2&numdoc=58"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "search: dynstatTop offer / priority delivery / price=159"}},
                    {"id": 2201, "titles": {"raw": "search: dynstatTop model / priority delivery / price=301"}},
                    {"id": 2202, "titles": {"raw": "search: model / priority delivery / price=201"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.assertFragmentNotIn(response, {"results": [{"entity": "regionalDelimiter"}]})

    @classmethod
    def prepare_dynstat_top_price_sort_with_local_offers_first__pessimize_not_priority_top_model(cls):
        cls.index.offers += [
            Offer(
                hid=221,
                price=price,
                fesh=cls.PRIORITY_FESH,
                title="search: dynstatTop offer / priority delivery / price=" + str(price),
            )
            for price in range(101, 160)
        ]
        cls.index.models += [
            Model(hyperid=2211, hid=221, title="search: dynstatTop model / country delivery / price=201"),
            Model(hyperid=2212, hid=221, title="search: model / priority delivery / price=301"),
        ]
        # Необходимо явно указать local_offers + rids, чтобы переопределить доставку в рег. статистиках
        cls.index.regional_models += [
            RegionalModel(hyperid=2211, price_min=100, local_offers=0, offers=1, rids=[66]),
        ]
        cls.index.offers += [
            Offer(hyperid=2211, fesh=cls.COUNTRY_FESH, price=201),
            Offer(hyperid=2212, fesh=cls.PRIORITY_FESH, price=301),
        ]

    def test_dynstat_top_price_sort_with_local_offers_first__pessimize_not_priority_top_model(self):
        """
        https://st.yandex-team.ru/MARKETOUT-34607
        Модель с приоритетной доставкой должна быть выше модели без приоритеной доставки, вошедшей в dynstatTop.
        Даже если цена модели из dynstatTop после обновления статистик оказалась ниже.
        """
        # Запрашиваю последнюю (2ю) страницу по 58 док-ов, чтобы захватить все доки,
        # не вошедшие в dynstatTop, и последние документы из dynstatTop.
        # Устанавливаю numdoc < 60, чтобы не переопределить кол-во доков для перезапроса статистик.
        response = self.report.request_json(
            "pp=18&place=prime&rids=66&hid=221&how=aprice&allow-collapsing=1&text=search"
            "&local-offers-first=1&page=2&numdoc=58"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "search: dynstatTop offer / priority delivery / price=159"}},
                    {"id": 2212, "titles": {"raw": "search: model / priority delivery / price=301"}},
                    {"entity": "regionalDelimiter"},
                    {"id": 2211, "titles": {"raw": "search: dynstatTop model / country delivery / price=201"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_dynstat_top_price_sort_with_local_offers_first__pessimize_not_top_country_model(cls):
        cls.index.offers += [
            Offer(
                hid=222,
                price=price,
                fesh=cls.PRIORITY_FESH,
                title="search: dynstatTop offer / priority delivery / price=" + str(price),
            )
            for price in range(101, 160)
        ]
        cls.index.models += [
            Model(hyperid=2221, hid=222, title="search: dynstatTop model / country delivery / price=301"),
            Model(hyperid=2222, hid=222, title="search: model / country delivery / price=201"),
        ]
        # Необходимо явно указать local_offers + rids, чтобы переопределить доставку в рег. статистиках
        cls.index.regional_models += [
            RegionalModel(hyperid=2221, price_min=100, local_offers=0, offers=1, rids=[66]),
            RegionalModel(hyperid=2222, price_min=201, local_offers=0, offers=1, rids=[66]),
        ]
        cls.index.offers += [
            Offer(hyperid=2221, fesh=cls.COUNTRY_FESH, price=301),
            Offer(hyperid=2222, fesh=cls.COUNTRY_FESH, price=201),
        ]

    def test_dynstat_top_price_sort_with_local_offers_first__pessimize_not_top_country_model(self):
        """
        https://st.yandex-team.ru/MARKETOUT-34607
        Среди 2х моделей с доставкой из других регионов, модель, не попавшая в dynstatTop,
        должна пессимизироваться.
        Даже если цена модели из dynstatTop после обновления статистик оказалась выше.
        """
        # Запрашиваю последнюю (2ю) страницу по 58 док-ов, чтобы захватить все доки,
        # не вошедшие в dynstatTop, и последние документы из dynstatTop.
        # Устанавливаю numdoc < 60, чтобы не переопределить кол-во доков для перезапроса статистик.
        response = self.report.request_json(
            "pp=18&place=prime&rids=66&hid=222&how=aprice&allow-collapsing=1&text=search"
            "&local-offers-first=1&page=2&numdoc=58"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "search: dynstatTop offer / priority delivery / price=159"}},
                    {"entity": "regionalDelimiter"},
                    {"id": 2221, "titles": {"raw": "search: dynstatTop model / country delivery / price=301"}},
                    {"id": 2222, "titles": {"raw": "search: model / country delivery / price=201"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_dynstat_top_price_sort_with_local_offers_first__pessimize_empty_models(cls):
        cls.index.offers += [
            Offer(
                hid=223,
                price=price,
                fesh=cls.PRIORITY_FESH,
                title="search: dynstatTop offer / priority delivery / price=" + str(price),
            )
            for price in range(101, 160)
        ]
        cls.index.models += [
            Model(hyperid=2231, hid=223, title="search: dynstatTop model / empty"),
            Model(hyperid=2232, hid=223, title="search: model / country delivery / price=201"),
            Model(hyperid=2233, hid=223, title="search: model / empty"),
        ]
        # Необходимо явно указать local_offers + rids, чтобы переопределить доставку в рег. статистиках
        cls.index.regional_models += [
            # Меняю модельные статистики, чтобы модель попапла в топ60
            RegionalModel(hyperid=2231, price_min=100, local_offers=1, offers=1, rids=[66]),
            RegionalModel(hyperid=2232, price_min=201, local_offers=0, offers=1, rids=[66]),
        ]
        cls.index.offers += [
            Offer(hyperid=2232, fesh=cls.COUNTRY_FESH, price=201),
        ]

    def test_dynstat_top_price_sort_with_local_offers_first__pessimize_empty_models(self):
        """
        https://st.yandex-team.ru/MARKETOUT-34607
        Пустые модели пессимизируются всегда.
        Среди пустых моделей пессимизируются модели, не попавшие в dynstatTop.
        """
        # Запрашиваю последнюю (2ю) страницу по 58 док-ов, чтобы захватить все доки,
        # не вошедшие в dynstatTop, и последние документы из dynstatTop.
        # Устанавливаю numdoc < 60, чтобы не переопределить кол-во доков для перезапроса статистик.
        response = self.report.request_json(
            "pp=18&place=prime&rids=66&hid=223&how=aprice&allow-collapsing=1&text=search"
            "&local-offers-first=1&page=2&numdoc=58"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "search: dynstatTop offer / priority delivery / price=159"}},
                    {"entity": "regionalDelimiter"},
                    {"id": 2232, "titles": {"raw": "search: model / country delivery / price=201"}},
                    {"id": 2231, "titles": {"raw": "search: dynstatTop model / empty"}},
                    {"id": 2233, "titles": {"raw": "search: model / empty"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_empty_models_pessimization_on_aprice_sort(cls):
        cls.index.offers += [
            Offer(hid=201, title="offer with price {}".format(100 + i), price=100 + i) for i in range(60)
        ]
        cls.index.models += [
            Model(hid=201, hyperid=271, title="empty model with randx=1", randx=1),
            Model(hid=201, hyperid=272, title="empty model with randx=2", randx=2),
            Model(hid=201, hyperid=274, title="not empty model with min price 200"),
        ]
        cls.index.offers += [
            Offer(hyperid=274, title="model 274 offer with price 200", price=200),
            Offer(hid=201, title="offer with price 300", price=300),
        ]

    def test_empty_models_pessimization_on_aprice_sort(self):
        """
        Проверяем, что пустые модели пессимизируются относительно моделей
        и офферов, не попавших в топ с дин. статистиками

        В индексе:
        - 60 офферов со ценами от 100 до 159 - чтобы создать топ документов для переранжирования
        - самый дорогой оффер с ценой 300 - чтобы убедиться, что непустые модели
          не пессимизируются относительно офферов
        - 2 модели без офферов - 2 модели, чтобы убедиться, что пустые модели оказались в конце неслучайно
        - 1 непустая модель с ценой 200
        """
        # Запрашиваю последнюю (2ю) страницу по 59 док-ов, чтобы захватить модели и часть офферов.
        # Устанавливаю numdoc < 60, чтобы не переопределить кол-во доков для перезапроса статистик
        response = self.report.request_json(
            "place=prime&hid=201&use-default-offers=1&allow-collapsing=1&numdoc=59&page=2&how=aprice"
        )
        self.assertFragmentIn(
            response,
            {
                "total": 64,
                "results": [
                    {"entity": "offer", "titles": {"raw": "offer with price 159"}},
                    {"id": 274, "titles": {"raw": "not empty model with min price 200"}},
                    {"entity": "offer", "titles": {"raw": "offer with price 300"}},
                    {"id": 272, "titles": {"raw": "empty model with randx=2"}},
                    {"id": 271, "titles": {"raw": "empty model with randx=1"}},
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_empty_models_pessimization_on_dprice_sort(cls):
        cls.index.offers += [
            Offer(hid=251, title="offer with price {}".format(700 + i), price=700 + i) for i in range(60)
        ]
        cls.index.models += [
            Model(hid=251, hyperid=231, title="empty model with randx=1", randx=1),
            Model(hid=251, hyperid=232, title="empty model with randx=2", randx=2),
            Model(hid=251, hyperid=234, title="not empty model with min price 550"),
        ]
        cls.index.offers += [
            Offer(hyperid=234, title="model 234 offer with price 550", price=550),
            Offer(hid=251, title="offer with price 250", price=250),
        ]

    def test_empty_models_pessimization_on_dprice_sort(self):
        """
        Проверяем, что пустые модели пессимизируются относительно моделей
        и офферов, не попавших в топ с дин. статистиками

        В индексе:
        - 60 офферов со ценами от 700 до 759 - чтобы создать топ документов для переранжирования
        - самый дешёвый оффер с ценой 250 - чтобы убедиться, что непустые модели
          не пессимизируются относительно офферов
        - 2 модели без офферов - 2 модели, чтобы убедиться, что пустые модели оказались в конце неслучайно
        - 1 непустая модель с ценой 550
        """
        # Запрашиваю последнюю (2ю) страницу по 59 док-ов, чтобы захватить модели и часть офферов.
        # Устанавливаю numdoc < 60, чтобы не переопределить кол-во доков для перезапроса статистик
        response = self.report.request_json(
            "place=prime&hid=251&use-default-offers=1&allow-collapsing=1&numdoc=59&page=2&how=dprice"
        )
        self.assertFragmentIn(
            response,
            {
                "total": 64,
                "results": [
                    {"entity": "offer", "titles": {"raw": "offer with price 700"}},
                    {"id": 234, "titles": {"raw": "not empty model with min price 550"}},
                    {"entity": "offer", "titles": {"raw": "offer with price 250"}},
                    {"id": 232, "titles": {"raw": "empty model with randx=2"}},
                    {"id": 231, "titles": {"raw": "empty model with randx=1"}},
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_sort_models_with_cutprice__good_state_is_not_set(cls):
        cls.index.models += [
            Model(hid=301, hyperid=300, title="model with cutprice DO price=300"),
            Model(hid=301, hyperid=301, title="model with DO price=400"),
            Model(hid=301, hyperid=302, title="model with DO price=200 and cutprice=500"),
        ]
        cls.index.offers += [
            Offer(hid=301, hyperid=300, price=300, is_cutprice=True),
            Offer(hid=301, hyperid=301, price=400),
            Offer(hid=301, hyperid=302, price=200),
            Offer(hid=301, hyperid=302, price=500, is_cutprice=True),
            Offer(hid=301, price=150, title="offer price=150"),
            Offer(hid=301, price=250, title="offer price=250"),
            Offer(hid=301, price=350, title="offer price=350"),
            Offer(hid=301, price=450, title="offer price=450"),
        ]

    def test_sort_models_with_cutprice__good_state_is_not_set(self):
        """
        Проверяем, что в сортировке по цене у моделей с уценёнными и обычными офферами
        цена для сортировки выбирается корректно
        Ожидается, что если good-state не указан, то:
        - модели только с уценкой сортируются по мин. цене уценки
        - модели с уценкой и обычными офферми сортируются по мин. цене обычных офферов
        - модели с обычными офферми сортируются по мин. цене обычных офферов

        Дополнительно добавляю несколько офферов, чтобы убедиться, что сортировка
        отработала корректно, а не случайно совпала с проверяемым порядком
        """
        response = self.report.request_json(
            "place=prime&show-cutprice=1&hid=301&use-default-offers=1&allow-collapsing=1&how=aprice"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "offer price=150"}},
                    {"titles": {"raw": "model with DO price=200 and cutprice=500"}},
                    {"titles": {"raw": "offer price=250"}},
                    {"titles": {"raw": "model with cutprice DO price=300"}},
                    {"titles": {"raw": "offer price=350"}},
                    {"titles": {"raw": "model with DO price=400"}},
                    {"titles": {"raw": "offer price=450"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_sort_models_with_cutprice__good_state_new(cls):
        cls.index.models += [
            Model(hid=311, hyperid=310, title="model with cutprice DO price=300"),
            Model(hid=311, hyperid=311, title="model with DO price=400"),
            Model(hid=311, hyperid=312, title="model with DO price=200 and cutprice=500"),
        ]
        cls.index.offers += [
            Offer(hid=311, hyperid=310, price=300, is_cutprice=True),
            Offer(hid=311, hyperid=311, price=400),
            Offer(hid=311, hyperid=312, price=200),
            Offer(hid=311, hyperid=312, price=500, is_cutprice=True),
            Offer(hid=311, price=150, title="offer price=150"),
            Offer(hid=311, price=250, title="offer price=250"),
            Offer(hid=311, price=350, title="offer price=350"),
            Offer(hid=311, price=450, title="offer price=450"),
        ]

    def test_sort_models_with_cutprice__good_state_new(self):
        """
        Проверяем, что для good-state=new у моделей используется цена обычного ДО (не уценённого)

        Дополнительно добавляю несколько офферов, чтобы убедиться, что сортировка
        отработала корректно, а не случайно совпала с проверяемым порядком
        """
        response = self.report.request_json(
            "good-state=new&place=prime&show-cutprice=1&hid=311&use-default-offers=1&allow-collapsing=1&how=aprice"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "offer price=150"}},
                    {"titles": {"raw": "model with DO price=200 and cutprice=500"}},
                    {"titles": {"raw": "offer price=250"}},
                    {"titles": {"raw": "offer price=350"}},
                    {"titles": {"raw": "model with DO price=400"}},
                    {"titles": {"raw": "offer price=450"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_sort_models_with_cutprice__good_state_cutprice(cls):
        cls.index.models += [
            Model(hid=321, hyperid=320, title="model with cutprice DO price=300"),
            Model(hid=321, hyperid=321, title="model with DO price=400"),
            Model(hid=321, hyperid=322, title="model with DO price=200 and cutprice=500"),
        ]
        cls.index.offers += [
            Offer(hid=321, hyperid=320, price=300, is_cutprice=True),
            Offer(hid=321, hyperid=321, price=400),
            Offer(hid=321, hyperid=322, price=200),
            Offer(hid=321, hyperid=322, price=500, is_cutprice=True),
            Offer(hid=321, price=250, title="cutprice offer price=250", is_cutprice=True),
            Offer(hid=321, price=350, title="cutprice offer price=350", is_cutprice=True),
            Offer(hid=321, price=450, title="cutprice offer price=450", is_cutprice=True),
            Offer(hid=321, price=550, title="cutprice offer price=550", is_cutprice=True),
        ]

    def test_sort_models_with_cutprice__good_state_cutprice(self):
        """
        Проверяем, что для good-state=cutprice у моделей используется цена уценённого ДО

        Дополнительно добавляю несколько офферов, чтобы убедиться, что сортировка
        отработала корректно, а не случайно совпала с проверяемым порядком
        """
        response = self.report.request_json(
            "good-state=cutprice&place=prime&show-cutprice=1&hid=321&use-default-offers=1"
            "&allow-collapsing=1&how=aprice"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "cutprice offer price=250"}},
                    {"titles": {"raw": "model with cutprice DO price=300"}},
                    {"titles": {"raw": "cutprice offer price=350"}},
                    {"titles": {"raw": "cutprice offer price=450"}},
                    {"titles": {"raw": "model with DO price=200 and cutprice=500"}},
                    {"titles": {"raw": "cutprice offer price=550"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_sort_cutprice_models_without_DO(cls):
        cls.index.offers += [Offer(hid=401, price=100, title="offer with price=100") for _ in range(60)]
        cls.index.models += [
            Model(hid=401, hyperid=400, title="model with min_cutprice=200 and min_price=220"),
            Model(hid=401, hyperid=401, title="model with min_cutprice=280 and min_price=240"),
            Model(hid=401, hyperid=402, title="model with min_price=210"),
            Model(hid=401, hyperid=405, title="model with min_price=270"),
            Model(hid=401, hyperid=406, title="model with min_cutprice=230"),
        ]
        cls.index.offers += [
            Offer(hyperid=400, price=200, is_cutprice=True),
            Offer(hyperid=400, price=220),
            Offer(hyperid=401, price=280, is_cutprice=True),
            Offer(hyperid=401, price=240),
            Offer(hyperid=402, price=210),
            Offer(hyperid=405, price=270),
            Offer(hyperid=406, price=230, is_cutprice=True),
            Offer(hid=401, price=300, title="most expensive offer with price=300"),
        ]

    def test_sort_cutprice_models_without_DO__show_cutprice_0(self):
        """
        Проверяем, что с show-cutprice=0 у моделей без обновленных статитсик
        учитывается только мин. цена новых офферов
        Параметр good-state на выдачу влиять не должен
        Модель 406, у которой есть только уценка, должна съехать вниз, тк в данном случае она не в продаже

        Чтобы инф-я о мин. цене не перетёрлась после запроса за ДО перед 2м этапом переранжирования:
        - Я создаю 60 уценённых офферов с мин. ценой, которые составят топ60
          документов после 1го этапа переранжирования для запроса за ДО
        """

        def check(good_state):
            # Запрашиваю последнюю страницу по 59 док-ов, чтобы захватить модели и часть офферов.
            request = (
                "place=prime&show-cutprice=0&hid=401&use-default-offers=1&allow-collapsing=1"
                "&how=aprice&numdoc=59&page=2&debug=da"
            )
            if good_state:
                request += "&good-state=" + good_state
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {"titles": {"raw": "offer with price=100"}},
                        {"titles": {"raw": "model with min_price=210"}, "id": 402},
                        {"titles": {"raw": "model with min_cutprice=200 and min_price=220"}, "id": 400},
                        {"titles": {"raw": "model with min_cutprice=280 and min_price=240"}, "id": 401},
                        {"titles": {"raw": "model with min_price=270"}, "id": 405},
                        {"titles": {"raw": "most expensive offer with price=300"}},
                        {"titles": {"raw": "model with min_cutprice=230"}, "id": 406},
                    ]
                },
                allow_different_len=False,
                preserve_order=True,
            )

        check(good_state=None)
        check(good_state="new")
        check(good_state="cutprice")

    def test_sort_cutprice_models_without_DO__show_cutprice_1(self):
        """
        Проверяем, что учитывается правильная цена у моделей с уценёнными и новыми офферами
        и без обновлённых статистик.
        Ожидается, что если good-state не указан, то:
        - модели только с уценкой сортируются по мин. цене уценки
        - модели с уценкой и обычными офферми сортируются по мин. цене обычных офферов
        - модели с обычными офферми сортируются по мин. цене обычных офферов

        Чтобы инф-я о мин. цене не перетёрлась после запроса за ДО перед 2м этапом переранжирования:
        - Я создаю 60 офферов с мин. ценой, которые составят топ60
          документов после 1го этапа переранжирования для запроса за ДО
        """
        # Запрашиваю последнюю страницу по 59 док-ов, чтобы захватить модели и часть офферов.
        response = self.report.request_json(
            "place=prime&show-cutprice=1&hid=401&use-default-offers=1&allow-collapsing=1&how=aprice&numdoc=59&page=2"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "offer with price=100"}},
                    {"titles": {"raw": "model with min_price=210"}, "id": 402},
                    {"titles": {"raw": "model with min_cutprice=200 and min_price=220"}, "id": 400},
                    {"titles": {"raw": "model with min_cutprice=230"}, "id": 406},
                    {"titles": {"raw": "model with min_cutprice=280 and min_price=240"}, "id": 401},
                    {"titles": {"raw": "model with min_price=270"}, "id": 405},
                    {"titles": {"raw": "most expensive offer with price=300"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_sort_cutprice_models_without_DO__good_state_new(self):
        """
        Проверяем, что для good-state=new у моделей без обновленных статитсик
        учитывается только мин. цена новых офферов

        Чтобы инф-я о мин. цене не перетёрлась после запроса за ДО перед 2м этапом переранжирования:
        - Я создаю 60 уценённых офферов с мин. ценой, которые составят топ60
          документов после 1го этапа переранжирования для запроса за ДО
        """
        # Запрашиваю последнюю страницу по 59 док-ов, чтобы захватить модели и часть офферов.
        # TODO хочется понять, почему тест падает, если убрать show-cutprice=1
        response = self.report.request_json(
            "good-state=new&place=prime&show-cutprice=1&hid=401&use-default-offers=1&allow-collapsing=1"
            "&how=aprice&numdoc=59&page=2"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "offer with price=100"}},
                    {"titles": {"raw": "model with min_price=210"}, "id": 402},
                    {"titles": {"raw": "model with min_cutprice=200 and min_price=220"}, "id": 400},
                    {"titles": {"raw": "model with min_cutprice=280 and min_price=240"}, "id": 401},
                    {"titles": {"raw": "model with min_price=270"}, "id": 405},
                    {"titles": {"raw": "most expensive offer with price=300"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_sort_cutprice_models_without_DO__good_state_cutprice(cls):
        cls.index.offers += [
            Offer(hid=501, price=100, title="cutprice offer with price=100", is_cutprice=True) for _ in range(60)
        ]
        cls.index.models += [
            Model(hid=501, hyperid=500, title="model with min_cutprice=200 and min_price=220"),
            Model(hid=501, hyperid=501, title="model with min_cutprice=250 and min_price=230"),
            Model(hid=501, hyperid=502, title="model with min_price=210"),
            Model(hid=501, hyperid=503, title="model with min_cutprice=240"),
        ]
        cls.index.offers += [
            Offer(hyperid=500, price=200, is_cutprice=True),
            Offer(hyperid=500, price=220),
            Offer(hyperid=501, price=250, is_cutprice=True),
            Offer(hyperid=501, price=230),
            Offer(hyperid=502, price=210),
            Offer(hyperid=503, price=240, is_cutprice=True),
            Offer(hid=501, price=300, title="most expensive cutprice offer with price=300", is_cutprice=True),
        ]

    def test_sort_cutprice_models_without_DO__good_state_cutprice(self):
        """
        Проверяем, что для good-state=cutprice у моделей без обновленных статитсик
        учитывается только мин. цена уценённых офферов

        Чтобы инф-я о мин. цене не перетёрлась после запроса за ДО перед 2м этапом переранжирования:
        - Я создаю 60 офферов с мин. ценой, которые составят топ60
          документов после 1го этапа переранжирования для запроса за ДО
        """
        # Запрашиваю последнюю страницу по 59 док-ов, чтобы захватить модели и часть офферов.
        response = self.report.request_json(
            "good-state=cutprice&place=prime&show-cutprice=1&hid=501&use-default-offers=1&allow-collapsing=1"
            "&how=aprice&numdoc=59&page=2"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "cutprice offer with price=100"}},
                    {"titles": {"raw": "model with min_cutprice=200 and min_price=220"}, "id": 500},
                    {"titles": {"raw": "model with min_cutprice=240"}, "id": 503},
                    {"titles": {"raw": "model with min_cutprice=250 and min_price=230"}, "id": 501},
                    {"titles": {"raw": "most expensive cutprice offer with price=300"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    class EmergencyConst:
        hid_a = 14

        model_a = 10100
        model_d = 10400

        sku_a_1 = model_a + 1
        sku_d_1 = model_d
        sku_d_2 = model_d + 2

        ungrouped_a_1 = sku_a_1

        ungrouped_d_1 = sku_d_1
        ungrouped_d_2 = sku_d_2

    @classmethod
    def prepare_sort_with_emergency_offers(cls):
        cls.index.models += [
            Model(
                hyperid=cls.EmergencyConst.model_a,
                title="Model A",
                hid=cls.EmergencyConst.hid_a,
                ts=11,
            ),
            Model(
                hyperid=cls.EmergencyConst.model_d,
                title="Model D",
                hid=cls.EmergencyConst.hid_a,
                ts=14,
                ungrouped_blue=[
                    UngroupedModel(group_id=cls.EmergencyConst.ungrouped_d_1, title="ungrouped d1", key='d_1'),
                    UngroupedModel(group_id=cls.EmergencyConst.ungrouped_d_2, title="ungrouped d2", key='d_2'),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title='SKU-A1',
                hyperid=cls.EmergencyConst.model_a,
                sku=cls.EmergencyConst.sku_a_1,
                waremd5=Offer.generate_waremd5('SKU-A1'),
                ungrouped_model_blue=cls.EmergencyConst.ungrouped_a_1,
                ts=1101,
                blue_offers=[
                    BlueOffer(
                        price=1400,
                        offerid='blue.offer.sku.a1',
                        waremd5=Offer.generate_waremd5('b_offer_SKU-A1_1'),
                        cpa=Offer.CPA_REAL,
                        ts=110101,
                    ),
                ],
            ),
            MarketSku(
                title='SKU-D1',
                hyperid=cls.EmergencyConst.model_d,
                sku=cls.EmergencyConst.sku_d_1,
                waremd5=Offer.generate_waremd5('SKU-D1'),
                ungrouped_model_blue=cls.EmergencyConst.ungrouped_d_1,
                ts=1401,
                blue_offers=[
                    BlueOffer(
                        price=1500,
                        offerid='blue.offer.sku.d1',
                        waremd5=Offer.generate_waremd5('b_offer_SKU-D1_1-X'),
                        cpa=Offer.CPA_REAL,
                        ts=140101,
                    ),
                ],
            ),
            MarketSku(
                title='SKU-D2',
                hyperid=cls.EmergencyConst.model_d,
                sku=cls.EmergencyConst.sku_d_2,
                waremd5=Offer.generate_waremd5('SKU-D2'),
                ungrouped_model_blue=cls.EmergencyConst.ungrouped_d_2,
                ts=1402,
                blue_offers=[
                    BlueOffer(
                        price=1250,
                        offerid='blue.offer.sku.d2',
                        waremd5=Offer.generate_waremd5('b_offer_SKU-D2_1'),
                        cpa=Offer.CPA_REAL,
                        ts=140201,
                    ),
                ],
            ),
        ]

        cls.index.virtual_models += [
            VirtualModel(virtual_model_id=80004),
        ]

        # Здесь данный оффер с виртуальной моделью приводит к тому, что для SKU sku_d_1 не нахидится ДО,
        # что приводит к некорректному вычислению ранга цены и неправильной сортировке.
        cls.index.offers += [
            Offer(
                title='white offer SKU-D1',
                ts=800401,
                price=1800,
                offerid='white.offer.sku.d1',
                waremd5=Offer.generate_waremd5('w_offer_SKU-D1_1'),
                cpa=Offer.CPA_REAL,
                virtual_model_id=80004,
                sku=cls.EmergencyConst.sku_d_1,
                ungrouped_model_blue=cls.EmergencyConst.ungrouped_d_1,
            )
        ]

    def make_product(self, wareId, price, priceRank):
        return {
            "entity": "product",
            "offers": {"items": [{"wareId": wareId, "prices": {"value": str(price)}}]},
            "debug": {
                "metaRank": [{"name": "PRICE", "value": str(priceRank)}],
            },
        }

    def test_sort_with_emergency_offers(self):
        """
        Проверяем корректность сортировки, если ДО не был найден.
        """

        base_request = "pp=18&place=prime&cpa=real&use-default-offers=1&allow-collapsing=1&how=aprice&hid={}".format(
            self.EmergencyConst.hid_a
        )

        # С выключенным флагом market_use_base_offer_price_with_model_stats metaRank для "b_offer_SKU-D1_1-X___g" считается некорректно.
        # Поэтому и возникает проблема с сортировкой.
        request = base_request + "&rearr-factors=market_use_base_offer_price_with_model_stats={}".format(0) + "&debug=1"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        self.make_product("b_offer_SKU-D1_1-X___g", 1500, 125001),
                    ]
                }
            },
        )

        # Поведение по умолчанию - порядок правильный и metaRank считается корректно
        request = base_request + "&debug=1"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        self.make_product("b_offer_SKU-D2_1_____g", 1250, 125001),
                        self.make_product("b_offer_SKU-A1_1_____g", 1400, 140001),
                        self.make_product("b_offer_SKU-D1_1-X___g", 1500, 150001),
                    ]
                }
            },
            preserve_order=True,
        )

        message = "Default offer don't found for ungrouped item (SKU, MODEL, ts): [(10400, 10400, 140101), ] - try old way (explicit)... Explicitly found: 0/1"
        self.error_log.expect(code=ErrorCodes.DEFAULT_OFFER_DONT_FOUND_FOR_UNGROUPED, message=message).times(2)


if __name__ == '__main__':
    main()
