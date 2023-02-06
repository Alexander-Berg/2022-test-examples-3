#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    Elasticity,
    MarketSku,
    Region,
    RegionalDelivery,
    Shop,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.testcase import TestCase, main
from core.matcher import NotEmpty, Round


class T(TestCase):
    """Тесты на CPA факторы для формулы на базовом"""

    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(rid=213, region_type=Region.CITY),
            Region(rid=2, region_type=Region.CITY),
        ]

        cls.index.shops += [
            Shop(fesh=1, fulfillment_virtual=True, virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE),
            Shop(
                fesh=1111,
                datafeed_id=1111,
                priority_region=213,
                regions=[225],
                name="1P-Магазин 145 склад",
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
            ),
            Shop(
                fesh=31,
                datafeed_id=31,
                priority_region=213,
                regions=[225],
                name="3P поставщик Вася",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseInfo(id=147, home_region=213),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=147,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicDeliveryServiceInfo(id=157, rating=2),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145, 147]),
        ]

        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[213],
                warehouse_with_priority=[
                    WarehouseWithPriority(145, 0),
                    WarehouseWithPriority(147, 0),
                ],
            ),
            WarehousesPriorityInRegion(
                regions=[2],
                warehouse_with_priority=[
                    WarehouseWithPriority(145, 1),
                    WarehouseWithPriority(147, 0),
                ],
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1234,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=225, options=[DeliveryOption(price=15, shop_delivery_price=15, day_from=1, day_to=2)]
                    ),
                    RegionalDelivery(
                        rid=227, options=[DeliveryOption(price=48, shop_delivery_price=48, day_from=3, day_to=5)]
                    ),
                    RegionalDelivery(
                        rid=229, options=[DeliveryOption(price=58, shop_delivery_price=58, day_from=4, day_to=6)]
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=1235,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=224, options=[DeliveryOption(price=35, shop_delivery_price=35, day_from=2, day_to=4)]
                    ),
                    RegionalDelivery(
                        rid=225, options=[DeliveryOption(price=45, shop_delivery_price=45, day_from=3, day_to=5)]
                    ),
                    RegionalDelivery(
                        rid=226, options=[DeliveryOption(price=55, shop_delivery_price=55, day_from=4, day_to=6)]
                    ),
                ],
            ),
        ]

        sorted_elasticity = [
            Elasticity(price_variant=100, demand_mean=200),
            Elasticity(price_variant=200, demand_mean=80),
            Elasticity(price_variant=300, demand_mean=10),
        ]

        cls.index.mskus += [
            MarketSku(
                title="gmv ue rating",
                hyperid=2,
                sku=100013,
                hid=100,
                delivery_buckets=[1234],
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(price=119, feedid=31, offerid='shop_sku_gt_ref_min1', waremd5="BLUE-100013-FEED-1111Q"),
                    BlueOffer(price=122, feedid=31, offerid='shop_sku_gt_ref_min2', waremd5="BLUE-100013-FEED-3100Q"),
                ],
            ),
        ]

    def test_shop_operational_rating_factors(self):
        response = self.report.request_json(
            "place=prime&text=gmv&debug=da&pp=18"
            "&rearr-factors=market_metadoc_search=no;market_blue_buybox_disable_old_buybox_algo=0"
        )

        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "wareId": "BLUE-100013-FEED-1111Q",
                    "factors": {
                        "SHOP_OPERATIONAL_RATING_LATE_SHIP_RATE": Round(0.05),
                        "SHOP_OPERATIONAL_RATING_CANCELLATION_RATE": Round(0.011),
                        "SHOP_OPERATIONAL_RATING_RETURN_RATE": Round(0.001),
                    },
                }
            },
        )

    def test_gmv_eu_factors(self):
        response = self.report.request_json(
            "place=prime&text=gmv&rids=213&debug=da&pp=18" "&rearr-factors=market_metadoc_search=no"
        )

        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "wareId": "BLUE-100013-FEED-3100Q",
                    "factors": {
                        "GMV": NotEmpty(),
                    },
                }
            },
        )


if __name__ == '__main__':
    main()
