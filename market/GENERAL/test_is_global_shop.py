#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import DeliveryBucket, DeliveryOption, GlobalModel, Model, Offer, Region, RegionalDelivery, Shop
from core.testcase import TestCase, main

CI_RUSSIAN_POST = 1


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(fesh=1, is_global=False),
            Shop(fesh=2, is_global=True),
            Shop(fesh=3),  # shop is not global by default
        ]

        # randx нужен чтобы оферы на выдаче шли в определенном порядке.
        cls.index.offers += [
            Offer(hid=100, title="Offer from non-global shop", fesh=1, randx=300),
            Offer(hid=100, title="Offer from global shop", fesh=2, randx=200),
            Offer(hid=100, title="Offer from shop with no global tag", fesh=3, randx=100),
        ]

        cls.index.regiontree += [
            Region(rid=300, children=[Region(rid=301)]),
            Region(rid=399),
        ]

        cls.index.global_models += [
            GlobalModel(hyperid=201, rids=[300]),
            GlobalModel(hyperid=202, rids=[301]),
        ]

        cls.index.models += [
            Model(hyperid=200, hid=100, title="Non-global model"),
            Model(hyperid=201, hid=100, title="Global model"),
            Model(hyperid=202, hid=100, title="Global model for child region"),
        ]

    def test_is_global_shop_not_present(self):
        """
        Проверяем выдачу в случае отуствия параметра is-global-shop=1, должны
        вернуться все оферы с корректно выставленным флажком isGlobal.
        """
        response = self.report.request_json("place=prime&hid=100")
        self.assertEqual(3, response.count({"entity": "offer"}))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer from non-global shop"},
                        "shop": {
                            "id": 1,
                            "isGlobal": False,
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer from global shop"},
                        "shop": {
                            "id": 2,
                            "isGlobal": True,
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer from shop with no global tag"},
                        "shop": {
                            "id": 3,
                            "isGlobal": False,
                        },
                    },
                ]
            },
            preserve_order=True,
        )

    def test_is_global_shop_equals_true(self):
        """
        Проверяем что в случае наличия параметра is-global-shop=1 оферы
        упорядочены по признаку isGlobal, причем в начале идут глобальные оферы
        (isGlobal=True)
        """
        response = self.report.request_json("place=prime&hid=100&is-global-shop=1")
        self.assertEqual(3, response.count({"entity": "offer"}))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer from global shop"},
                        "shop": {
                            "id": 2,
                            "isGlobal": True,
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer from non-global shop"},
                        "shop": {
                            "id": 1,
                            "isGlobal": False,
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer from shop with no global tag"},
                        "shop": {
                            "id": 3,
                            "isGlobal": False,
                        },
                    },
                ]
            },
            preserve_order=True,
        )

    def test_model_filtering_when_is_global_shop_not_present(self):
        """
        Проверяем выдачу в случае отуствия параметра is-global-shop=1, должны
        вернуться все модели.
        """
        for region in [300, 301, 399]:
            response = self.report.request_json("place=prime&hid=100&rids={}".format(region))
            self.assertEqual(3, response.count({"entity": "product"}))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {"entity": "product", "titles": {"raw": "Non-global model"}},
                        {"entity": "product", "titles": {"raw": "Global model"}},
                        {"entity": "product", "titles": {"raw": "Global model for child region"}},
                    ]
                },
            )

    def test_model_filtering_when_is_global_shop_equals_true(self):
        """
        Проверяем что в случае наличия параметра is-global-shop=1 возвращаются
        только модели, имеющие глобальные предложения в указанных регионах.
        """
        response = self.report.request_json("place=prime&hid=100&rids=300&is-global-shop=1")
        self.assertEqual(1, response.count({"entity": "product"}))
        self.assertFragmentIn(response, {"results": [{"entity": "product", "titles": {"raw": "Global model"}}]})

        # Проверяем что для дочернего региона в выдаче есть модель
        # "Global model for child region" и модель для родительского региона.
        response = self.report.request_json("place=prime&hid=100&rids=301&is-global-shop=1")
        self.assertEqual(2, response.count({"entity": "product"}))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "titles": {"raw": "Global model"}},
                    {"entity": "product", "titles": {"raw": "Global model for child region"}},
                ]
            },
        )

        # В этом регионе для моделей нет глобальных предложений.
        response = self.report.request_json("place=prime&hid=100&rids=399&is-global-shop=1")
        self.assertEqual(0, response.count({"entity": "product"}))

    @classmethod
    def prepare_hide_global_cpa(cls):
        cls.index.shops += [
            Shop(fesh=10, is_global=False, priority_region=300, cpa=Shop.CPA_REAL),
            Shop(fesh=11, is_global=True, regions=[300], priority_region=350, cpa=Shop.CPA_REAL),
        ]
        cls.index.offers += [
            Offer(hid=101, title="Non-global CPA offer", fesh=10, cpa=Offer.CPA_REAL),
            Offer(hid=101, title="Global CPA offer", fesh=11, cpa=Offer.CPA_REAL),
            Offer(hid=101, title="Non-global CPC offer", fesh=10, cpa=Offer.CPA_NO),
            Offer(hid=101, title="Global CPC offer", fesh=11, cpa=Offer.CPA_NO),
        ]

    def test_hide_global_cpa(self):
        """
        Проверяем что при наличии параметра hide-global-cpa=1 глобальные CPA
        оферы не попадают в выдачу.
        """
        response = self.report.request_json("place=prime&hid=101&rids=300&hide-global-cpa=1")
        self.assertEqual(3, response.count({"entity": "offer"}))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "Non-global CPA offer"},
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "Non-global CPC offer"},
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "Global CPC offer"},
                    },
                ]
            },
        )
        # Проверим еще что при отсутствии флага hide-global-cpa в выдаче есть
        # все четыре офера.
        response = self.report.request_json("place=prime&hid=101&rids=300")
        self.assertEqual(4, response.count({"entity": "offer"}))

    @classmethod
    def prepare_filter_russian_post(cls):
        cls.index.shops += [
            Shop(fesh=1174901, is_global=False),
            Shop(fesh=1174902, is_global=True),
            Shop(fesh=1174903, is_global=True),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1174901,
                fesh=1174901,
                carriers=[CI_RUSSIAN_POST],
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=0, day_from=2, day_to=32, order_before=24)])
                ],
            ),
            DeliveryBucket(
                bucket_id=1174902,
                fesh=1174902,
                carriers=[CI_RUSSIAN_POST],
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=0, day_from=2, day_to=32, order_before=24)])
                ],
            ),
            DeliveryBucket(
                bucket_id=1174903,
                fesh=1174903,
                carriers=[CI_RUSSIAN_POST, 1234],
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=0, day_from=2, day_to=32, order_before=24)])
                ],
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=1174901, fesh=1174901, price=100, delivery_buckets=[1174901]),
            Offer(hyperid=1174901, fesh=1174902, price=200, delivery_buckets=[1174902]),
            Offer(hyperid=1174901, fesh=1174903, price=300, delivery_buckets=[1174903]),
        ]

    def test_filter_russian_post_is_global_shop_false(self):
        """
        Делаем запрос без is-global-shop (=0)
        Должно быть все три оффера
        """
        response = self.report.request_json('place=productoffers&rids=213&hyperid=1174901&is-global-shop=0')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "model": {"id": 1174901}, "shop": {"id": 1174901}},
                    {"entity": "offer", "model": {"id": 1174901}, "shop": {"id": 1174902}},
                    {"entity": "offer", "model": {"id": 1174901}, "shop": {"id": 1174903}},
                ]
            },
        )

    def test_filter_russian_post_is_global_shop_true(self):
        """
        Делаем запрос с is-global-shop (=1)
        Магазин 1174902 (is_global=1 + доставка Почтой РФ) должен пропасть
        Магазин 1174903 должен остаться, т.к. в нём есть доставка № 1234, которая не Почта РФ
        """
        response = self.report.request_json('place=productoffers&rids=213&hyperid=1174901&is-global-shop=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "model": {"id": 1174901}, "shop": {"id": 1174901}},
                    {"entity": "offer", "model": {"id": 1174901}, "shop": {"id": 1174903}},
                ]
            },
        )

        self.assertFragmentNotIn(response, {"shop": {"id": 1174902}})


if __name__ == "__main__":
    main()
