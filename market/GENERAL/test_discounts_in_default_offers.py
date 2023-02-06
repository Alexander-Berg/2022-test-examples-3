#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import HyperCategory, HyperCategoryType, MnPlace, Model, Offer, Shop


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.001)

        # MARKETOUT-18173
        # готовим оффера так, чтобы выбирался сначала без скидки, а потом со скидкой

        # категории
        cls.index.hypertree += [
            HyperCategory(hid=101, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=102, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=201, output_type=HyperCategoryType.SIMPLE),
        ]

        # магазин
        cls.index.shops += [
            Shop(fesh=1001, priority_region=213),
            Shop(fesh=1002, priority_region=213),
            Shop(fesh=1003, priority_region=213),
            Shop(fesh=1004, priority_region=213),
            Shop(fesh=1005, priority_region=213),
            Shop(fesh=1006, priority_region=213),
            Shop(fesh=1007, priority_region=213),
            Shop(fesh=1008, priority_region=213),
            Shop(fesh=1009, priority_region=213),
        ]

        # моделька
        cls.index.models += [
            Model(hyperid=1817301, hid=101),
            Model(hyperid=1817302, hid=102),
        ]

        # оффера
        cls.index.offers += [
            Offer(hyperid=1817301, ts=100001, fesh=1001, price=1200),
            Offer(
                hyperid=1817301, ts=100002, fesh=1002, price=1500, price_old=2000, price_history=2000
            ),  # оффер со скидкой
            Offer(hyperid=1817301, ts=100003, fesh=1003, price=2000, waremd5="aaaaaaaaaaaaaaaaaaaaaQ"),
            Offer(hyperid=1817301, ts=100004, fesh=1004, price=1900),
            Offer(hyperid=1817301, ts=100005, fesh=1005, price=1948),
            Offer(hyperid=1817301, ts=100006, fesh=1006, price=1923),
            Offer(hyperid=1817301, ts=100007, fesh=1007, price=1821),
            Offer(hyperid=1817301, ts=100008, fesh=1008, price=1755),
            Offer(hyperid=1817301, ts=100009, fesh=1009, price=2013),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100001).respond(0.009)  # оффер за 1200 выберется как дефолтный
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100002).respond(
            0.007
        )  # оффер со скидкой за 1500 не выберется как дефолтный

        cls.index.offers += [
            Offer(
                hyperid=1817302, ts=120001, fesh=1010, price=1100, price_old=1300, price_history=2000
            ),  # оффер со скидкой
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 120001).respond(
            0.008
        )  # оффер со скидкой как дефолтный для модели 1817302

    def test_default_offer_is_with_discount(self):
        """
        Эмуляция карусели на скидочном хабе:

        При запросе с фильтром по скидкам дефолтным становится оффер со скидкой (от магазина 1002), проверяем это.
        """
        response = self.report.request_json(
            'place=prime&hid=101,201&allow-collapsing=1&filter-discount-only=1&how=discount_p&use-default-offers=1&onstock=1&relax-filters=1'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "product",
                "type": "model",
                "id": 1817301,
                "offers": {
                    "items": [
                        {
                            "entity": "offer",
                            "shop": {
                                "id": 1002,
                            },
                            "benefit": {
                                "type": "default",
                            },
                            "prices": {
                                "value": "1500",
                                "discount": {
                                    "oldMin": "2000",
                                    "percent": 25,
                                },
                            },
                        }
                    ]
                },
            },
        )

    def test_default_offer_for_model_is_not_with_discount(self):
        """
        А при переходе с карусели на карточку модели, мы видим совершенно другой оффер в качестве дефолтного

        Зафиксируем сей печальный факт
        """
        # именно такой запрос сейчас шлётся на фронте при заходе на морду КМ
        response = self.report.request_json(
            'place=productoffers&hyperid=1817301&page=1&numdoc=6&onstock=1&hid=101&offers-set=defaultList,listCpa&grhow=shop&pp=6&relax-filters=1&rgb=green_with_blue&do-waremd5=aaaaaaaaaaaaaaaaaaaaaQ'
        )

        # в качестве дефолтного нет больше офферов при waremd5 опции
        # в качестве героя выберется оффер с wareid
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "shop": {
                    "id": 1003,
                },
                "benefit": {
                    "type": "waremd5",
                },
            },
        )

        # и проверяем, что НЕ выберется оффер со скидкой от магазина 1002
        self.assertFragmentNotIn(
            response,
            {
                "entity": "offer",
                "shop": {
                    "id": 1002,
                },
                "benefit": {
                    "type": "cheapest",
                },
            },
        )

        # и проверяем что оффер со скидкой от магазина 1002 всё же попал в топ-6
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "shop": {
                    "id": 1002,
                },
            },
        )


if __name__ == '__main__':
    main()
