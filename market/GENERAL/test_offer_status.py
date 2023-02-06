#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import BlueOffer, Currency, MarketSku, Shop, Tax
import base64


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(
                fesh=100,
                datafeed_id=1000,
                priority_region=213,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=2,
                sku=2,
                blue_offers=[
                    BlueOffer(feedid=100, offerid='001', supplier_id=100),
                ],
            ),
        ]

    @staticmethod
    def get_feed_shoffer_64(feedshofferid):
        return "&feed_shoffer_id_base64={}".format(base64.urlsafe_b64encode(feedshofferid))

    def test_ok_offer(self):
        response = self.report.request_json('place=offer_status' + self.get_feed_shoffer_64('100-001'))
        self.assertFragmentIn(response, {'results': []}, allow_different_len=False)


if __name__ == '__main__':
    main()
