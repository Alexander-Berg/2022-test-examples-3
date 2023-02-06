#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Offer, Region, Shop, HyperCategory
from core.testcase import TestCase, main


class WareMd5:
    DIGITAL_GOOD = '29lEaAKkQll1XTjm0WPoIA'
    PLUS_GOOD = 'PLUS_AKkQll1XTjm0WPoIA'


class Constants:
    SUBSCRIPTIONS_HID = 17940630
    YANDEX_VENDOR = 15562112


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
            Region(rid=2, name='Санкт-Петербург'),
        ]
        cls.index.hypertree += [
            HyperCategory(hid=Constants.SUBSCRIPTIONS_HID, name='Онлайн-подписки и карты оплаты'),
        ]
        cls.index.shops += [
            #     DSBS магазин (для проверки цифровых товаров)
            Shop(fesh=1001, priority_region=213, cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
        ]
        cls.index.offers += [
            #     Цифровой товар
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=9001,
                fesh=1001,
                title="Digital",
                has_delivery_options=False,
                pickup=False,
                store=False,
                available=False,
                download=True,
                waremd5=WareMd5.DIGITAL_GOOD,
            ),
            #     Подписка от Яндекс Плюс
            Offer(
                cpa=Offer.CPA_REAL,
                hid=Constants.SUBSCRIPTIONS_HID,
                vendor_id=Constants.YANDEX_VENDOR,
                fesh=1001,
                title="Plus Subscription",
                has_delivery_options=False,
                pickup=False,
                store=False,
                available=False,
                download=True,
                waremd5=WareMd5.PLUS_GOOD,
            ),
        ]

    def test_digital_dsbs_goods(self):
        """
        Проверяем что цифровой dsbs товар есть на выдаче
        """
        for rids in [213, 2]:
            response = self.report.request_json("place=prime&fesh=1001&regset=2&rids={rids}".format(rids=rids))
            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "wareId": WareMd5.DIGITAL_GOOD,
                    "cpa": "real",
                    "delivery": {
                        "isAvailable": True,
                        "hasPickup": False,
                        "hasLocalStore": False,
                        "hasPost": False,
                        "isDownloadable": True,
                        "postAvailable": False,
                        "options": [],
                    },
                },
            )

        for rids in [213, 2]:
            response = self.report.request_json("place=prime&fesh=1001&regset=2&rids={rids}&rgb=blue".format(rids=rids))
            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "wareId": WareMd5.DIGITAL_GOOD,
                    "cpa": "real",
                    "delivery": {
                        "isAvailable": True,
                        "hasPickup": False,
                        "hasLocalStore": False,
                        "hasPost": False,
                        "isDownloadable": True,
                    },
                },
            )

    def test_hide_plus_subscription(self):
        """
        Проверяем, что по параметру hide_plus_subscriptions=1 скрываются подписки от Яндекс Плюс
        """
        request = "place=prime&hid={}&regset=2&rids=213&hide_plus_subscriptions={}"
        response = self.report.request_json(request.format(Constants.SUBSCRIPTIONS_HID, 0))
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "totalOffers": 1,
                "results": [
                    {
                        "entity": "offer",
                        "wareId": WareMd5.PLUS_GOOD,
                    }
                ],
            },
        )

        response = self.report.request_json(request.format(Constants.SUBSCRIPTIONS_HID, 1))
        self.assertFragmentIn(
            response,
            {"total": 0, "totalOffers": 0, "results": []},
        )


if __name__ == '__main__':
    main()
