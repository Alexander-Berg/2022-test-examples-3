#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Offer, Shop


class T(TestCase):
    @classmethod
    def prepare(cls):
        pass

    @classmethod
    def prepare_offers_for_hide_test_shops(cls):
        """
        12345 - обычный магазин
        48322 - тестовый магазин
        """
        cls.index.shops += [
            Shop(fesh=12345),
            Shop(fesh=48322),
        ]

        cls.index.offers += [
            Offer(hyperid=123, fesh=12345),
            Offer(hyperid=123, fesh=48322),
        ]

        cls.index.test_shops += [48322, 48333]

    def test_offer_in_not_test_shop(self):
        """
        Запрашиваем офферы для модели 123 с флагом hide-test-shops
        В результат должен попасть обычный магазин 12345
        """
        response = self.report.request_json('place=productoffers&hyperid=123&hide-test-shops=1')
        self.assertFragmentIn(response, {"entity": "offer", "model": {"id": 123}, "shop": {"id": 12345}})

    def test_hide_offer_in_test_shop(self):
        """
        Запрашиваем офферы для модели 123 с флагом hide-test-shops
        В результат не должен попасть тестовый магазин 48322
        """
        response = self.report.request_json('place=productoffers&hyperid=123&hide-test-shops=1')
        self.assertFragmentNotIn(response, {"entity": "offer", "model": {"id": 123}, "shop": {"id": 48322}})

    def test_show_offers(self):
        """
        Запрашиваем офферы для модели 123 без флага hide-test-shops
        В результат должны попасть оба магазина
        """
        response = self.report.request_json('place=productoffers&hyperid=123&hide-test-shops=0')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "model": {"id": 123}, "shop": {"id": 12345}},
                    {"entity": "offer", "model": {"id": 123}, "shop": {"id": 48322}},
                ]
            },
        )


if __name__ == '__main__':
    main()
