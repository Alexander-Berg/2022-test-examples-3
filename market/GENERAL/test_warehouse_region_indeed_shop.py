#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    MarketSku,
    OfferDimensions,
    Outlet,
    Region,
    RegionalDelivery,
    Shop,
    Vat,
)

from core.testcase import TestCase, main
from core.matcher import NotEmpty


class T(TestCase):
    """Тесты на замену информации о регионе магазина на инфоо по складу"""

    @classmethod
    def prepare(cls):
        cls.settings.lms_autogenerate = False

        cls.index.regiontree += [
            Region(
                rid=3,
                name='Центральный федеральный округ',
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=1,
                        name='Москва и Московская область',
                        region_type=Region.FEDERATIVE_SUBJECT,
                        latitude=55.815792,
                        longitude=37.380031,
                        population=45800000,
                        chief=213,
                        children=[
                            Region(
                                rid=213,
                                name='Москва',
                                latitude=55.815792,
                                longitude=37.380031,
                                population=15000000,
                                chief=213,
                            ),
                            Region(rid=10758, name='Химки', latitude=55.888796, longitude=37.430328, population=230000),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=1, fulfillment_virtual=True, virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE),
            Shop(
                fesh=33,
                datafeed_id=33,
                priority_region=10758,
                regions=[225],
                home_region=225,
                name='blue_shop_1',
                currency=Currency.RUR,
                blue='REAL',
                warehouse_id=1111,
                delivery_service_outlets=[222],
            ),
        ]

        cls.index.outlets += [
            Outlet(point_id=222, region=213, point_type=Outlet.FOR_POST_TERM, delivery_service_id=12345),
        ]

        cls.dynamic.lms += [
            DynamicDeliveryServiceInfo(id=1111, rating=2),
            DynamicWarehouseInfo(id=1111, home_region=10758, holidays_days_set_key=7),
            DynamicWarehousesPriorityInRegion(region=10758, warehouses=[1111]),
            # DynamicWarehouseAndDeliveryServiceInfo(warehouse_id=1111, delivery_service_id=12345),
            DynamicWarehouseToWarehouseInfo(
                warehouse_from=1111,
                warehouse_to=1111,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=19, region_to=10758)],
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=5,
                fesh=33,  # carriers=[12345],
                regional_options=[
                    RegionalDelivery(
                        rid=10758,
                        options=[
                            DeliveryOption(price=10000, day_from=15, day_to=15),
                            DeliveryOption(price=1000000, day_from=0, day_to=1),
                        ],
                    ),
                ],
            )
        ]

        cls.index.mskus += [
            MarketSku(
                title="Ручка из переговорки со странным колпачком SKU",
                hyperid=440,
                sku=440440,
                waremd5="Sku44WithHolidaydel__g",
                blue_offers=[
                    BlueOffer(
                        price=4444,
                        vat=Vat.VAT_10,
                        feedid=33,
                        offerid="offer_33",
                        waremd5="Sku44WithHolidayDeli_g",
                        weight=4,
                        dimensions=OfferDimensions(40, 40, 40),
                        is_fulfillment=True,
                        delivery_buckets=[5],
                    )
                ],
                delivery_buckets=[5],
            ),
        ]

    def test_warehouse_region_instead_shop_factors_on(self):
        # Выдать информацию о складе вместо информации о магазине
        response = self.report.request_json(
            "place=prime&text=колпачок&debug=da&rearr-factors=" "market_warehouse_region_instead_shop=1"
        )

        # флаг включен: показывает склад
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {
                        "SHOP_REGION_ID": "10758",
                        "SHOP_REGION_LATITUDE": "55.88879776",
                        "SHOP_REGION_LONGITUDE": "37.43032837",
                        "SHOP_REGION_POPULATION": "230000",
                    }
                }
            },
        )

    def test_warehouse_region_instead_shop_factors_off(self):
        response = self.report.request_json(
            "place=prime&text=колпачок&debug=da&rearr-factors=" "market_warehouse_region_instead_shop=0"
        )

        # флаг выключен: показывает магазинг (в данном случае виртуальный)
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "factors": {
                        "SHOP_REGION_ID": NotEmpty(),
                        "SHOP_REGION_POPULATION": "1234567",
                    }
                }
            },
        )


if __name__ == '__main__':
    main()
