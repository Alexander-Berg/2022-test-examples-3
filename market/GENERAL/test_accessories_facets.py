#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import HyperCategory, Model, Offer, Shop
from core.testcase import TestCase, main
from core.types.autogen import Const


class T(TestCase):
    """
    Набор тестов для выдачи дополнительных фильтров метода офферных аксессуаров
    MARKETOUT-11473
    """

    @classmethod
    def prepare(cls):
        """
        Данные для простых тестов выдачи набора категорийных фильтров для офферных
        аксуссуаров (источник - "формула"):
        * Несколько категорий
        * Модель и оффер с двумя аксессуарами из разных категорий для теста выдачи нескольких фильтров
            по запросу из одного оффера
        * Две модели и оферы с аксессуарами из разных категорий для теста выдачи нескольких фильтров
            по запросу из нескольких офферов
        """

        cls.index.hypertree += [
            HyperCategory(hid=101, name="Category #101"),
            HyperCategory(hid=102, name="Category #102"),
            HyperCategory(hid=103, name="Category #103"),
        ]

        cls.index.models += [
            Model(hyperid=1, hid=101, title='Model #1', accessories=[2, 3]),
            Model(hyperid=2, hid=102, title='Model #2', accessories=[4]),
            Model(hyperid=3, hid=103, title='Model #3', accessories=[5]),
            Model(hyperid=4, hid=102, title='Model #4', accessories=[]),
            Model(hyperid=5, hid=103, title='Model #5', accessories=[]),
        ]

        cls.index.offers += [
            Offer(hyperid=1, hid=101, price=1000, cpa=Offer.CPA_REAL, fesh=1001, waremd5="BH8EPLtKmdLQhLUasgaOnA"),
            Offer(hyperid=2, hid=102, price=2000, cpa=Offer.CPA_REAL, fesh=1001, waremd5="bpQ3a9LXZAl_Kz34vaOpSg"),
            Offer(hyperid=3, hid=103, price=3000, cpa=Offer.CPA_REAL, fesh=1001, waremd5="KXGI8T3GP_pqjgdd7HfoHQ"),
            Offer(hyperid=4, hid=102, price=4000, cpa=Offer.CPA_REAL, fesh=1001, waremd5="yRgmzyBD4j8r4rkCby6Iuw"),
            Offer(hyperid=5, hid=103, price=5000, cpa=Offer.CPA_REAL, fesh=1001, waremd5="xzFUFhFuAvI1sVcwDnxXPQ"),
        ]

        cls.index.shops += [
            Shop(fesh=1001, name="Shop #1001", home_region=Const.ROOT_COUNTRY, cpa=Shop.CPA_REAL),
        ]

    def test_single_offer_basket(self):
        """
        Тест вывода фильтров по категориям для простой корзины
        Проверяем состав и полноту выходных записей для фильтра по категориям
        при запросе аксессуаров для корзины из одного оффера.
        Ожидаем два фильтра, соответствующих двум категориям результирующего списка аксессуаров
        """
        response = self.report.request_json(
            "place=accessories&pp=143&fesh=1001&hyperid=1&offerid=BH8EPLtKmdLQhLUasgaOnA&price=1000"
        )
        self.assertFragmentIn(
            response,
            {
                "groups": [
                    {
                        "title": "Category #102",
                        "resources": {
                            "garsons": [
                                {
                                    "params": {
                                        "hid": [102],
                                        "hyperid": [1],
                                        "price": [1000],
                                        "fesh": [1001],
                                        "offerid": ["BH8EPLtKmdLQhLUasgaOnA"],
                                    }
                                }
                            ]
                        },
                    },
                    {
                        "title": "Category #103",
                        "resources": {
                            "garsons": [
                                {
                                    "params": {
                                        "hid": [103],
                                        "hyperid": [1],
                                        "price": [1000],
                                        "fesh": [1001],
                                        "offerid": ["BH8EPLtKmdLQhLUasgaOnA"],
                                    }
                                }
                            ]
                        },
                    },
                ]
            },
            preserve_order=False,
        )

    def test_multioffer_basket(self):
        """
        Тест вывода фильтров по категориям
        Проверяем состав и полноту выходных записей для фильтра по категориям
        при запросе аксессуаров для корзины из нескольких офферов.
        Ожидаем два фильтра, соответствующих двум категориям результирующего списка аксессуаров
        """
        response = self.report.request_json(
            "place=accessories&pp=143&fesh=1001,1001&hyperid=2,3&offerid=bpQ3a9LXZAl_Kz34vaOpSg,KXGI8T3GP_pqjgdd7HfoHQ&price=2000,3000"
        )
        self.assertFragmentIn(
            response,
            {
                "groups": [
                    {
                        "title": "Category #102",
                        "resources": {
                            "garsons": [
                                {
                                    "params": {
                                        "hid": [102],
                                        "hyperid": [2, 3],
                                        "price": [2000, 3000],
                                        "fesh": [1001, 1001],
                                        "offerid": ["bpQ3a9LXZAl_Kz34vaOpSg", "KXGI8T3GP_pqjgdd7HfoHQ"],
                                    }
                                }
                            ]
                        },
                    },
                    {
                        "title": "Category #103",
                        "resources": {
                            "garsons": [
                                {
                                    "params": {
                                        "hid": [103],
                                        "hyperid": [2, 3],
                                        "price": [2000, 3000],
                                        "fesh": [1001, 1001],
                                        "offerid": ["bpQ3a9LXZAl_Kz34vaOpSg", "KXGI8T3GP_pqjgdd7HfoHQ"],
                                    }
                                }
                            ]
                        },
                    },
                ]
            },
            preserve_order=False,
        )


if __name__ == '__main__':
    main()
