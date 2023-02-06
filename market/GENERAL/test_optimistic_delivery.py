#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    RegionalDelivery,
    Shop,
)
from core.testcase import TestCase, main
from core.types.offer import OfferDimensions
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax


USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"


class _Shops(object):
    blue_shop_1 = Shop(
        fesh=3,
        datafeed_id=3,
        priority_region=2,
        name='blue_shop_1',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.FIRST_PARTY,
        blue='REAL',
    )


class _Offers(object):
    sku1_offer1 = BlueOffer(
        price=5,
        vat=Vat.VAT_10,
        feedid=3,
        offerid='blue.offer.1.1',
        waremd5='Sku1Price5-IiLVm1Goleg',
        weight=5,
        dimensions=OfferDimensions(length=20, width=30, height=10),
    )
    sku2_offer1 = BlueOffer(
        price=55,
        vat=Vat.VAT_18,
        feedid=3,
        offerid='blue.offer.2.1',
        waremd5='Sku2Price55-iLVm1Goleg',
        weight=5,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        cargo_types=[300],
    )


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                cpa=Shop.CPA_REAL,
            ),
            _Shops.blue_shop_1,
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseInfo(id=123, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
            DynamicWarehouseToWarehouseInfo(warehouse_from=123, warehouse_to=123),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=111,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=123,
                delivery_service_id=111,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicDeliveryServiceInfo(id=111, rating=2),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145, 123]),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1234,
                carriers=[111],
                regional_options=[
                    RegionalDelivery(
                        rid=225, options=[DeliveryOption(price=20, day_from=2, day_to=2, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1",
                hyperid=1,
                delivery_buckets=[1234],
                sku=1,
                waremd5='Sku1-wdDXWsIiLVm1goleg',
                blue_offers=[_Offers.sku1_offer1],
            ),
            MarketSku(
                title="blue offer sku2",
                hyperid=1,
                delivery_buckets=[1234],
                sku=2,
                waremd5='Sku2-wdDXWsIiLVm1goleg',
                blue_offers=[_Offers.sku2_offer1],
            ),
        ]

    def test_optimistic_delivery(self):
        request = (
            'place=sku_offers&rgb=blue&rids=213&market-sku={}&rearr-factors=market_optimistic_delivery_for_moscow={}'
            + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )

        response = self.report.request_json(request.format(1, 0))
        self.assertFragmentIn(
            response,
            {
                "delivery": {
                    "options": [
                        {
                            "dayFrom": 2,
                            "dayTo": 3,
                        }
                    ]
                }
            },
        )

        response = self.report.request_json(request.format(1, 1))
        self.assertFragmentIn(
            response,
            {
                "delivery": {
                    "options": [
                        {
                            "dayFrom": 1,
                            "dayTo": 3,
                        }
                    ]
                }
            },
        )

        response = self.report.request_json(request.format(2, 0))
        self.assertFragmentIn(
            response,
            {
                "delivery": {
                    "options": [
                        {
                            "dayFrom": 2,
                            "dayTo": 3,
                        }
                    ]
                }
            },
        )

        response = self.report.request_json(request.format(2, 1))
        self.assertFragmentIn(
            response,
            {
                "delivery": {
                    "options": [
                        {
                            "dayFrom": 2,
                            "dayTo": 3,
                        }
                    ]
                }
            },
        )


if __name__ == '__main__':
    main()
