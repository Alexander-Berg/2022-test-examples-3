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
    HyperCategory,
    MarketSku,
    OfferDimensions,
    Outlet,
    Region,
    RegionalDelivery,
    Shop,
    Vat,
)
from core.svn_data import SvnData

from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare_market_report_data_from_access(cls):
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_svn_data = True
        cls.settings.market_access_settings.use_svn_data = True

    @classmethod
    def setup_market_access_resources(cls, access_server, shade_host_port):
        svn_data = SvnData(access_server=access_server, shade_host_port=shade_host_port, meta_paths=cls.meta_paths)

        # warehouse_id\tuser_region\thid\tmin_price
        svn_data.min_price_for_region_delivery += [
            [1111, 10759, 1802270, 301],
            [1111, 10760, 90401, 160],
        ]

        svn_data.create_version()

    @classmethod
    def prepare(cls):
        cls.index.hypertree += [
            HyperCategory(hid=1802270, children=[HyperCategory(hid=18022709)]),
            HyperCategory(hid=18022710),
            HyperCategory(hid=18022711),
        ]

        cls.settings.lms_autogenerate = False

        cls.index.regiontree += [
            Region(
                rid=3,
                name='Центральный федеральный округ',
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=1,
                        chief=213,
                        children=[
                            Region(
                                rid=213,
                                name='Москва',
                                chief=213,
                            ),
                            Region(rid=10758),
                        ],
                    ),
                ],
            ),
            Region(rid=10759),
            Region(rid=10760, children=[Region(rid=10761)]),
        ]

        cls.index.shops += [
            Shop(fesh=1, fulfillment_virtual=True, virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE),
            Shop(
                fesh=33,
                datafeed_id=33,
                # priority_region=10758,
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
            DynamicWarehouseInfo(id=1111, home_region=225, holidays_days_set_key=7),
            DynamicWarehousesPriorityInRegion(region=10759, warehouses=[1111]),
            DynamicWarehousesPriorityInRegion(region=10761, warehouses=[1111]),
            DynamicWarehouseToWarehouseInfo(
                warehouse_from=1111,
                warehouse_to=1111,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=19, region_to=10759),
                    DateSwitchTimeAndRegionInfo(date_switch_hour=19, region_to=10761),
                ],
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=5,
                fesh=33,  # carriers=[12345],
                regional_options=[
                    RegionalDelivery(
                        rid=10759,
                        options=[
                            DeliveryOption(price=10000, day_from=15, day_to=15),
                            DeliveryOption(price=1000000, day_from=0, day_to=1),
                        ],
                    ),
                    RegionalDelivery(
                        rid=10761,
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
                title="Ручка за 300",
                hyperid=440,
                sku=440440,
                waremd5="Sku44WithHolidaydel__g",
                hid=18022709,
                blue_offers=[
                    BlueOffer(
                        price=300,
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
            MarketSku(
                title="Ручка за 500",
                sku=440441,
                waremd5="Sku44WithHolidayde1__g",
                hid=18022710,
                blue_offers=[
                    BlueOffer(
                        price=500,
                        vat=Vat.VAT_10,
                        feedid=33,
                        offerid="offer_34",
                        waremd5="Sku44WithHolidayDe1i_g",
                        weight=4,
                        dimensions=OfferDimensions(40, 40, 40),
                        is_fulfillment=True,
                        delivery_buckets=[5],
                    )
                ],
                delivery_buckets=[5],
            ),
            MarketSku(
                title="Ручка за 150",
                sku=440442,
                waremd5="Sku34WithHolidayde1__g",
                hid=18022711,
                blue_offers=[
                    BlueOffer(
                        price=150,
                        vat=Vat.VAT_10,
                        feedid=33,
                        offerid="offer_35",
                        waremd5="Sku44Wi1hHolidayDe1i_g",
                        weight=4,
                        dimensions=OfferDimensions(40, 40, 40),
                        is_fulfillment=True,
                        delivery_buckets=[5],
                    )
                ],
                delivery_buckets=[5],
            ),
        ]

    def test_hide_offer_by_price_threshold_prime(self):
        response = self.report.request_json(
            "place=prime&text=ручка&rids=10759&rearr-factors=filter_by_price_threshold_for_delivery=0"
        )

        # флаг выключен: все товары найдутся
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"titles": {"raw": "Ручка за 300"}},
                        {"titles": {"raw": "Ручка за 150"}},
                        {"titles": {"raw": "Ручка за 500"}},
                    ]
                }
            },
        )

        response = self.report.request_json("place=prime&text=ручка&rids=10759&debug=da")

        # флаг включен: отфильтруются товары меньше чем за 300 для этого региона
        self.assertFragmentIn(
            response,
            {
                'debug': {'brief': {'filters': {'PRICE_THRESHOLD_FOR_REGIONAL_DELIVERY': 1}}},
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "search": {
                    "results": [
                        {"titles": {"raw": "Ручка за 300"}},
                    ]
                }
            },
        )

        # в файле с ограничениями есть условие для региона выше по дереву для всех категорий (ограничение 160)
        response = self.report.request_json("place=prime&text=ручка&rids=10761&debug=da")
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"titles": {"raw": "Ручка за 300"}},
                        {"titles": {"raw": "Ручка за 500"}},
                    ]
                }
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "search": {
                    "results": [
                        {"titles": {"raw": "Ручка за 150"}},
                    ]
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {'brief': {'filters': {'PRICE_THRESHOLD_FOR_REGIONAL_DELIVERY': 1}}},
            },
        )

    def test_hide_offer_by_price_threshold_productoffers(self):
        response = self.report.request_json(
            "place=productoffers&rids=10759&text=ручка&hyperid=440&rearr-factors=filter_by_price_threshold_for_delivery=0"
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"titles": {"raw": "Ручка за 300"}},
                    ]
                }
            },
        )

        response = self.report.request_json("place=productoffers&rids=10759&text=ручка&hyperid=440")
        self.assertFragmentNotIn(
            response,
            {
                "search": {
                    "results": [
                        {"titles": {"raw": "Ручка за 300"}},
                    ]
                }
            },
        )


if __name__ == '__main__':
    main()
