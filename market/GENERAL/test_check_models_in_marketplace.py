#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import BlueOffer, Currency, MarketSku, Model, Offer, Region, Shop, Tax


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(rid=213),
            Region(rid=2),
        ]

        cls.index.models += [
            Model(hyperid=1),
            Model(hyperid=2),
            Model(hyperid=3),
            Model(hyperid=4),
            Model(hyperid=5),
            Model(hyperid=6),
        ]

        cls.index.shops += [
            Shop(
                fesh=1001,
                datafeed_id=1001,
                priority_region=213,
                currency=Currency.RUR,
                supplier_type=Shop.FIRST_PARTY,
                fulfillment_program=True,
                tax_system=Tax.OSN,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
            ),
            Shop(
                fesh=1002,
                datafeed_id=1002,
                priority_region=2,
                currency=Currency.RUR,
                supplier_type=Shop.FIRST_PARTY,
                fulfillment_program=True,
                tax_system=Tax.OSN,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
            ),
            Shop(
                fesh=1003,
                datafeed_id=1003,
                priority_region=213,
                regions=[213],
                currency=Currency.RUR,
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title='sku1',
                hyperid=1,
                sku=11,
                blue_offers=[
                    BlueOffer(feedid=1001),
                ],
            ),
            MarketSku(
                title='sku2',
                hyperid=2,
                sku=2,
                blue_offers=[],  # no offers
            ),
            MarketSku(
                title='sku3',
                hyperid=3,
                sku=33,
                blue_offers=[
                    BlueOffer(feedid=1001),
                ],
            ),
            MarketSku(
                title='sku5',
                hyperid=5,
                sku=55,
                blue_offers=[
                    BlueOffer(feedid=1002),
                ],
            ),
            MarketSku(title="sku6", hyperid=6, sku=66),
        ]

        cls.index.offers += [
            Offer(hyperid=1),
            Offer(hyperid=2),
            Offer(hyperid=4),
            Offer(
                title="dsbs offer",
                hyperid=6,
                fesh=1003,
                waremd5='Dsbs_________________g',
                price=100000,
                sku=66,
                cpa=Offer.CPA_REAL,
            ),
        ]

    def test_check_models_in_marketplace(self):
        response = self.report.request_json('place=check_models_in_marketplace&hyperid=1,2,3,4,5,6')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "modelId": 1,
                    },
                    {
                        "modelId": 3,
                    },
                    {
                        "modelId": 5,
                    },
                    {
                        "modelId": 6,
                    },
                ]
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
