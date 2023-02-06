#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main
from core.types import BlueOffer, MarketSku, Offer, Shop

SKU_ID = 1
MODEL_ID = 100

BLUE_OFFER_SHOP_SKU = 'blue_offer_shop_sku'
WHITE_OFFER_ID = 'white_offer_id'

BLUE_SHOP_ID = 1
WHITE_SHOP_ID = 2

HID = 1000


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.mskus += [
            MarketSku(
                sku=SKU_ID,
                hyperid=MODEL_ID,
                hid=HID,
                blue_offers=[
                    BlueOffer(
                        offerid=BLUE_OFFER_SHOP_SKU,
                        feedid=BLUE_SHOP_ID,
                    )
                ],
            )
        ]
        cls.index.offers += [
            Offer(offerid=WHITE_OFFER_ID, sku=SKU_ID, fesh=WHITE_SHOP_ID, hyperid=MODEL_ID, hid=HID),
        ]
        cls.index.shops += [
            Shop(
                fesh=BLUE_SHOP_ID,
                datafeed_id=BLUE_SHOP_ID,
                fulfillment_program=True,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            )
        ]

    def test_total_documents(self):
        request = 'place=prime&hid={}&debug=da'.format(HID)
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {'TOTAL_DOCUMENTS_CPC': 1, 'TOTAL_DOCUMENTS_CPA': 1})


if __name__ == '__main__':
    main()
