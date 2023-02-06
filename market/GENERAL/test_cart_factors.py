#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, DynamicWarehouseInfo, DynamicWarehouseToWarehouseInfo, MarketSku, Offer, Region, Shop
from core.testcase import TestCase, main
from core.matcher import NoKey, Round


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(rid=213),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=13, home_region=213),
            DynamicWarehouseInfo(id=1, home_region=213),
            DynamicWarehouseInfo(id=2, home_region=213),
            DynamicWarehouseToWarehouseInfo(
                warehouse_from=13,
                warehouse_to=1,
            ),
        ]

        cls.index.shops += [
            Shop(
                fesh=13,
                datafeed_id=13,
                priority_region=213,
                name='virtual_shop',
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                warehouse_id=13,
            ),
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='1P Kolomna kalach',
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                is_supplier=True,
                fulfillment_program=True,
                cpa=Shop.CPA_REAL,
                warehouse_id=1,
            ),
            Shop(
                fesh=2,
                datafeed_id=2,
                priority_region=213,
                name='3P Kolomna pastila',
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                is_supplier=True,
                fulfillment_program=True,
                warehouse_id=2,
            ),
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                title='Kolomna kalach sku',
                sku=1,
                blue_offers=[
                    BlueOffer(
                        title='Коломна Калач',
                        price=100,
                        feedid=1,
                        weight=10,
                        waremd5='Kalach____1P_________g',
                        vendor_id=1,
                    ),
                ],
            ),
            MarketSku(
                hyperid=1,
                title='Kolomna pastila sku',
                sku=2,
                blue_offers=[
                    BlueOffer(
                        title='Коломна пастила',
                        price=200,
                        feedid=2,
                        weight=10,
                        waremd5='Pastila___3P_________g',
                        vendor_id=1,
                    )
                ],
            ),
        ]

        cls.index.offers += [
            Offer(fesh=2, title="Половина рульки на троих", cpa=Offer.CPA_REAL, is_cpc=False),
        ]

    def test_vendors_in_cart(self):
        response = self.report.request_json(
            "place=prime&text=пастила&debug=da&pp=18&&rearr-factors=market_promo_cart_discount_enable_any_rgb=1"
            "&rearr-factors=market_metadoc_search=no"
        )
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "wareId": "Pastila___3P_________g",
                    "factors": {
                        "DOCUMENTS_OF_THIS_VENDOR_IN_CART": NoKey("DOCUMENTS_OF_THIS_VENDOR_IN_CART"),
                    },
                }
            },
        )

        response = self.report.request_json(
            "place=prime&cart=Kalach____1P_________g&text=калач&debug=da&pp=18&rearr-factors=market_promo_cart_discount_enable_any_rgb=1"
            "&rearr-factors=market_metadoc_search=no"
        )
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "wareId": "Kalach____1P_________g",
                    "factors": {"DOCUMENTS_OF_THIS_VENDOR_IN_CART": "1"},
                }
            },
        )

        response = self.report.request_json(
            "place=prime&cart=Pastila___3P_________g&cart=Kalach____1P_________g&text=калач&debug=da&pp=18&rearr-factors=market_promo_cart_discount_enable_any_rgb=1"
            "&rearr-factors=market_metadoc_search=no"
        )
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "wareId": "Kalach____1P_________g",
                    "factors": {"DOCUMENTS_OF_THIS_VENDOR_IN_CART": "2"},
                }
            },
        )

    def test_price_factors(self):
        response = self.report.request_json(
            "place=prime&text=пастила&debug=da&pp=18&&rearr-factors=market_promo_cart_discount_enable_any_rgb=1"
            "&rearr-factors=market_metadoc_search=no"
        )
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "wareId": "Pastila___3P_________g",
                    "factors": {
                        "PRICE_SEGMENT_IN_CART": NoKey("PRICE_SEGMENT_IN_CART"),
                        "THRIFT_IN_CART": NoKey("THRIFT_IN_CART"),
                    },
                }
            },
        )

        response = self.report.request_json(
            "place=prime&cart=Kalach____1P_________g&text=калач&debug=da&pp=18&rearr-factors=market_promo_cart_discount_enable_any_rgb=1"
            "&rearr-factors=market_metadoc_search=no"
        )
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "wareId": "Kalach____1P_________g",
                    "factors": {"THRIFT_IN_CART": Round(1), "PRICE_SEGMENT_IN_CART": Round(1)},
                }
            },
        )

        response = self.report.request_json(
            "place=prime&cart=Pastila___3P_________g&cart=Kalach____1P_________g&text=калач&debug=da&pp=18&rearr-factors=market_promo_cart_discount_enable_any_rgb=1"
            "&rearr-factors=market_metadoc_search=no"
        )
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "wareId": "Kalach____1P_________g",
                    "factors": {"PRICE_SEGMENT_IN_CART": Round(0.707106), "THRIFT_IN_CART": Round(0.666666)},
                }
            },
        )


if __name__ == '__main__':
    main()
