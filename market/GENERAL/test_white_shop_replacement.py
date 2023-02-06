#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    MarketSku,
    RegionalDelivery,
    Shop,
)


class T(TestCase):
    """магазин Беру у 3P офферов должен быть подменен на сматченый белый магазин"""

    @classmethod
    def prepare(cls):

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='ВиртуальныйМагазинНаБеру',
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
            Shop(
                fesh=1111,
                business_fesh=1,
                datafeed_id=1111,
                priority_region=213,
                regions=[225],
                name="ПоставщикБеру!",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
            Shop(fesh=11, business_fesh=1, name="ПоставщикБеруНаБелом!", priority_region=213, regions=[225]),
            Shop(
                fesh=2222,
                business_fesh=2,
                datafeed_id=2222,
                priority_region=213,
                regions=[225],
                name="РазмещаюсьНаБеру!",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
            Shop(fesh=21, business_fesh=2, name="РазмещаюсьНаБеру в Мск", priority_region=213, regions=[213]),
            Shop(fesh=22, business_fesh=2, name="РазмещаюсьНаБеру в Питере", priority_region=2, regions=[2]),
            Shop(
                fesh=3333,
                datafeed_id=3333,
                priority_region=213,
                regions=[225],
                name="РазмещаюсьТолькоНаБеру!",
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
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicDeliveryServiceInfo(id=157, rating=2),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145]),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=213000,
                carriers=[157],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=15, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=2000,
                carriers=[157],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=15, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=225000,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=225, options=[DeliveryOption(price=15, day_from=1, day_to=2)]),
                ],
            ),
        ]

        cls.index.mskus += [
            # оффер останется оффером от Беру, т.к. это 1P поставщик
            MarketSku(
                hyperid=1, hid=1, sku=1, delivery_buckets=[225000], blue_offers=[BlueOffer(price=2000, feedid=1111)]
            ),
            # оффер привяжется к магазинам 21 или 22 в зависимости от региона
            MarketSku(
                hyperid=1, hid=1, sku=2, delivery_buckets=[225000], blue_offers=[BlueOffer(price=2000, feedid=2222)]
            ),
            # оффер останется оффером от Беру т.к. этот 3P поставщик не имеет своего магазина на белом
            MarketSku(
                hyperid=1, hid=1, sku=3, delivery_buckets=[225000], blue_offers=[BlueOffer(price=2000, feedid=3333)]
            ),
        ]

    def test_loading(self):
        pass


if __name__ == '__main__':
    main()
