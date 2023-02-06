#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, Currency, MarketSku, Shop, Tax
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(
                # Виртуальный магазин синего маркета
                fesh=1000,
                datafeed_id=1000,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title="сепулька 1",
                sku="1",
                hyperid=1001,
                has_gone=True,  # will be filtered out by has_gone (both msku & offer)
                blue_offers=[
                    BlueOffer(
                        feedid=1,
                        offerid=1,
                    )
                ],
            ),
            MarketSku(
                title="сепулька 2",
                sku="2",
                hyperid=1001,
                blue_offers=[
                    BlueOffer(
                        feedid=1,
                        offerid=2,  # will be filtered out by offer dynamic (only offer)
                    )
                ],
            ),
            MarketSku(
                title="сепулька 3",
                sku="3",
                hyperid=1001,
                blue_offers=[
                    BlueOffer(
                        feedid=1,
                        offerid=3,
                        price=2000,  # will be filtered out by buy box
                    ),
                    BlueOffer(
                        feedid=1,
                        offerid=4,
                        price=1000,
                    ),
                ],
            ),
            MarketSku(
                title="сепуления",  # will be skipped by search
                sku="5",
                hyperid=1001,
                blue_offers=[
                    BlueOffer(
                        feedid=1,
                        offerid=5,
                    )
                ],
            ),
        ]


if __name__ == '__main__':
    main()
