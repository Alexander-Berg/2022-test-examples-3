#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Model, Outlet, Region, Shop
from core.types.sku import MarketSku, BlueOffer


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(fesh=13, priority_region=213, fulfillment_virtual=True, virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE),
            Shop(fesh=777, datafeed_id=777, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
        ]

        cls.index.regiontree += [
            Region(rid=213, name='Москва', region_type=Region.FEDERATIVE_SUBJECT),
        ]
        cls.index.outlets += [
            Outlet(fesh=777, region=213),
        ]

        # оффер с ценой ниже предельной
        blue_offer_lt = BlueOffer(waremd5='BlueOffer-LT-Limit-log', price=10, feedid=777, offerid='shop_sku_lt_limit')
        # оффер с ценой равной предельной
        blue_offer_eq = BlueOffer(waremd5='BlueOffer-EQ-Limit-log', price=20, feedid=777, offerid='shop_sku_eq_limit')
        # оффер с ценой выше предельной
        blue_offer_gt = BlueOffer(waremd5='BlueOffer-GT-Limit-log', price=30, feedid=777, offerid='shop_sku_gt_limit')

        cls.index.mskus += [
            MarketSku(
                title='blue market sku1',
                hyperid=1,
                sku=110011,
                waremd5='MarketSku1-IiLVm1goleg',
                price_limit=20,
                blue_offers=[blue_offer_lt, blue_offer_eq, blue_offer_gt],
            ),
        ]

        cls.index.models += [
            Model(hyperid=1, hid=1, title='blue and green model'),
        ]

    def test_price_lt_limit(self):
        """Проверяем, что оффер с ценой ниже предельной показывается."""
        wareid = 'BlueOffer-LT-Limit-log'
        params = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}&rgb=BLUE'
        params += "&rearr-factors=market_price_limit_enabled=1"
        response = self.report.request_json(params.format(wareid))

        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': wareid,
                    "prices": {
                        'value': "10",
                    },
                }
            ],
        )

    def test_price_eq_limit(self):
        """Проверяем, что оффер с ценой равной предельной показывается."""
        wareid = 'BlueOffer-EQ-Limit-log'
        params = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}&rgb=BLUE'
        params += "&rearr-factors=market_price_limit_enabled=1"
        response = self.report.request_json(params.format(wareid))

        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': wareid,
                    "prices": {
                        'value': "20",
                    },
                }
            ],
        )

    def test_price_gt_limit_enabled_by_default(self):
        """Проверяем, что оффер с ценой выше предельной по умолчанию скрывается."""
        wareid = 'BlueOffer-GT-Limit-log'
        params = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}&rgb=BLUE'
        response = self.report.request_json(params.format(wareid))

        self.assertFragmentNotIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': wareid,
                }
            ],
        )

    def test_price_gt_limit_disabled(self):
        """Проверяем, что оффер с ценой выше предельной показывается,,
        если отключена функциональность сравнения.
        """
        wareid = 'BlueOffer-GT-Limit-log'
        params = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}&rgb=BLUE'
        params += "&rearr-factors=market_price_limit_enabled=0"
        response = self.report.request_json(params.format(wareid))

        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': wareid,
                    "prices": {
                        'value': "30",
                    },
                }
            ],
        )


if __name__ == '__main__':
    main()
