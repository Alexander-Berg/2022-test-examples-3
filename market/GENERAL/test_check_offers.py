#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    CategoryRestriction,
    Currency,
    HyperCategory,
    MarketSku,
    Offer,
    RegionalRestriction,
    Shop,
    Tax,
    Vat,
)
from core.testcase import TestCase, main
from core.matcher import Absent
from core.types.hypercategory import ADULT_CATEG_ID

import base64


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.hypertree += [
            HyperCategory(hid=ADULT_CATEG_ID),
        ]

        cls.index.category_restrictions += [
            CategoryRestriction(
                name='ask_18',
                hids=[ADULT_CATEG_ID],
                regional_restrictions=[RegionalRestriction(rids_with_subtree=[213])],
            )
        ]

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
            Shop(
                fesh=5,
                datafeed_id=5,
                priority_region=213,
                warehouse_id=145,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
            ),
        ]

        cls.index.offers += [
            Offer(feedid=100, offerid='001'),
            Offer(feedid=200, offerid='001', hyperid=1),
            Offer(feedid=300, offerid='A,B'),
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=2,
                sku=2,
                blue_offers=[
                    BlueOffer(feedid=400, offerid='001', supplier_id=100),
                ],
            ),
            MarketSku(
                hyperid=3,
                sku=3,
                hid=ADULT_CATEG_ID,
                blue_offers=[
                    BlueOffer(
                        price=20,
                        vat=Vat.VAT_18,
                        title="Intimator",
                        feedid=5,
                        adult=1,
                        offerid='Intimator',
                        waremd5='AdultBlueOffer_______g',
                    )
                ],
            ),
        ]

    def test_bulk(self):
        response_1 = self.report.request_json(
            'place=check_offers' '&feed_shoffer_id=100-001' '&feed_shoffer_id=200-001' '&feed_shoffer_id=300-001'
        )
        response_2 = self.report.request_json('place=check_offers' '&feed_shoffer_id=100-001,200-001,300-001')
        for response in (response_1, response_2):
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'feedId': 100,
                            'offerId': '001',
                            'modelId': Absent(),
                        },
                        {
                            'feedId': 200,
                            'offerId': '001',
                            'modelId': 1,
                        },
                    ]
                },
                allow_different_len=False,
            )

    def test_virtual_feed_id_offer_id(self):
        response = self.report.request_json('place=check_offers' '&feed_shoffer_id=1000-400.001')
        self.assertFragmentIn(
            response,
            {
                'feedId': 1000,
                'offerId': '400.001',
            },
        )

    def test_feed_shoffer_id_base64(self):
        response = self.report.request_json(
            'place=check_offers' '&feed_shoffer_id_base64={}'.format(base64.urlsafe_b64encode('300-A,B'))
        )
        self.assertFragmentIn(
            response,
            {
                'feedId': 300,
                'offerId': 'A,B',
            },
        )

    def test_check_adult_filter(self):
        response = self.report.request_json(
            'place=check_offers' '&feed_shoffer_id=5-Intimator' '&check-offers-filter=1'
        )
        self.assertFragmentIn(
            response,
            {
                "filterReasons": [
                    {
                        "wareId": "AdultBlueOffer_______g",
                        "reason": "ADULT",
                        "feedId": 5,
                        "offerId": "Intimator",
                        "supplierId": "",  # это supplier ogrn для белых маркетплейсов
                        "warehouseId": 145,
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()
