#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicBlueFreeDeliveryThresholdByRegion,
    DynamicBlueFreeDeliveryThresholds,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicTimeIntervalsSet,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    GpsCoord,
    MarketSku,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    Shop,
    Tax,
    TimeInfo,
    TimeIntervalInfo,
    TimeIntervalsForDaysInfo,
    TimeIntervalsForRegion,
    Vat,
)
from core.types.offer import OfferDimensions
from core.types.delivery import BlueDeliveryTariff
from core.matcher import Absent
import copy

warehouse_1111 = DynamicWarehouseInfo(id=1111, home_region=213, holidays_days_set_key=4)

region_213_warehouses_priority = DynamicWarehousesPriorityInRegion(region=213, warehouses=[1111, 147])

NORTH_CAUCASIAN_FEDERAL_DISTRICT = 102444
NORTH_CAUCASIAN_FEDERAL_DISTRICT_CHILD = 10244411
SIBERIAN_FEDERAL_DISTRICT = 59
URAL_FEDERAL_DISTRICT = 52
FAR_EASTERN_FEDERAL_DISTRICT = 73


def get_warehouse_and_delivery_service(warehouse_id, service_id, add_date_switch_hour_for_region_213=True):
    date_switch_hours = [
        DateSwitchTimeAndRegionInfo(date_switch_hour=1, region_to=2),
        DateSwitchTimeAndRegionInfo(date_switch_hour=1, region_to=3),
        DateSwitchTimeAndRegionInfo(date_switch_hour=1, region_to=39),
        DateSwitchTimeAndRegionInfo(date_switch_hour=1, region_to=NORTH_CAUCASIAN_FEDERAL_DISTRICT),
        DateSwitchTimeAndRegionInfo(date_switch_hour=1, region_to=SIBERIAN_FEDERAL_DISTRICT),
        DateSwitchTimeAndRegionInfo(date_switch_hour=1, region_to=URAL_FEDERAL_DISTRICT),
        DateSwitchTimeAndRegionInfo(date_switch_hour=1, region_to=FAR_EASTERN_FEDERAL_DISTRICT),
    ]
    if add_date_switch_hour_for_region_213:
        date_switch_hours += [DateSwitchTimeAndRegionInfo(date_switch_hour=20, region_to=213)]
    return DynamicWarehouseAndDeliveryServiceInfo(
        warehouse_id=warehouse_id,
        delivery_service_id=service_id,
        operation_time=0,
        date_switch_time_infos=date_switch_hours,
        shipment_holidays_days_set_key=6,
    )


warehouse1111_delivery_service258 = get_warehouse_and_delivery_service(1111, 258)
warehouse1111_delivery_service259 = get_warehouse_and_delivery_service(1111, 259)

warehouse2222_delivery_service258 = get_warehouse_and_delivery_service(2222, 258)
warehouse147_delivery_service257 = get_warehouse_and_delivery_service(147, 257)
warehouse147_delivery_service258 = get_warehouse_and_delivery_service(147, 258)
warehouse3333_delivery_service358 = get_warehouse_and_delivery_service(3333, 358)
warehouse145_delivery_service358 = get_warehouse_and_delivery_service(145, 358)

warehouse1111_delivery_service157 = DynamicWarehouseAndDeliveryServiceInfo(
    warehouse_id=1111,
    delivery_service_id=157,
    date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
)
warehouse1111_delivery_service257 = DynamicWarehouseAndDeliveryServiceInfo(
    warehouse_id=1111,
    delivery_service_id=257,
    date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
)
warehouse1111_delivery_service102 = DynamicWarehouseAndDeliveryServiceInfo(
    warehouse_id=1111,
    delivery_service_id=102,
    date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
)


class T(TestCase):
    @classmethod
    def prepare_blue_delivery_price(cls):
        '''
        Тарифы доставки синих оферов
        '''
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=[BlueDeliveryTariff(user_price=99)],
            large_size_weight=10005,
        )

    @classmethod
    def prepare(cls):
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']

        cls.settings.blue_market_free_delivery_threshold = 3500
        cls.settings.blue_market_prime_free_delivery_threshold = 3500
        cls.settings.blue_market_yandex_plus_free_delivery_threshold = 3500

        cls.index.regiontree += [
            Region(rid=213, tz_offset=10680, children=[Region(rid=3, tz_offset=10800), Region(rid=4, tz_offset=10800)]),
            Region(
                rid=2,
                tz_offset=10800,
                children=[
                    Region(rid=5, tz_offset=10800),
                ],
            ),
            Region(
                rid=NORTH_CAUCASIAN_FEDERAL_DISTRICT,
                tz_offset=10800,
                children=[
                    Region(rid=NORTH_CAUCASIAN_FEDERAL_DISTRICT_CHILD, tz_offset=10800),
                ],
            ),
            Region(rid=SIBERIAN_FEDERAL_DISTRICT),
            Region(rid=URAL_FEDERAL_DISTRICT),
            Region(rid=FAR_EASTERN_FEDERAL_DISTRICT),
            Region(rid=39, children=[Region(rid=123)]),
        ]

        cls.settings.lms_autogenerate = False
        cls.settings.loyalty_enabled = True

        cls.index.shops += [
            Shop(
                fesh=3,
                datafeed_id=3,
                priority_region=213,
                name='blue_shop_1',
                currency=Currency.RUR,
                blue='REAL',
                warehouse_id=1111,
            ),
            Shop(
                fesh=3,
                datafeed_id=4,
                priority_region=213,
                name='blue_shop_4',
                currency=Currency.RUR,
                blue='REAL',
                warehouse_id=1111,
            ),
            Shop(
                fesh=10,
                datafeed_id=5,
                priority_region=213,
                name='blue_shop_10',
                currency=Currency.RUR,
                blue='REAL',
                warehouse_id=2222,
            ),
            Shop(
                fesh=10,
                datafeed_id=7,
                priority_region=213,
                name='blue_shop_20',
                currency=Currency.RUR,
                blue='REAL',
                warehouse_id=2222,
            ),
            Shop(fesh=11, datafeed_id=70, priority_region=213, currency=Currency.RUR, blue='REAL', warehouse_id=1111),
            Shop(fesh=11, datafeed_id=71, priority_region=213, currency=Currency.RUR, blue='REAL', warehouse_id=2222),
            Shop(fesh=12, datafeed_id=72, priority_region=213, currency=Currency.RUR, blue='REAL', warehouse_id=147),
            Shop(
                fesh=21,
                datafeed_id=21,
                priority_region=213,
                name='blue_shop_21',
                currency=Currency.RUR,
                blue='REAL',
                warehouse_id=3333,
            ),
            Shop(
                fesh=22,
                datafeed_id=22,
                priority_region=213,
                name='blue_shop_22',
                currency=Currency.RUR,
                blue='REAL',
                warehouse_id=145,
            ),
            Shop(
                fesh=777,
                datafeed_id=777,
                priority_region=213,
                name='dropshipper',
                currency=Currency.RUR,
                blue='REAL',
                fulfillment_program=False,
                warehouse_id=164,
            ),
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                cpa=Shop.CPA_REAL,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                delivery_service_outlets=[
                    2002,
                    2003,
                    3001,
                    3002,
                    3003,
                    3004,
                    3005,
                    3006,
                    3007,
                    3008,
                    3009,
                    3010,
                    2004,
                    2005,
                    2006,
                    2007,
                ],
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=801,
                dc_bucket_id=1,
                fesh=1,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=15, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=15, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=5, options=[DeliveryOption(price=3, day_from=4, day_to=5)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=802,
                dc_bucket_id=2,
                fesh=1,
                carriers=[158],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=15, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=5, options=[DeliveryOption(price=15, day_from=4, day_to=5)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=803,
                dc_bucket_id=3,
                fesh=1,
                carriers=[257],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=4, day_to=5)]),
                    RegionalDelivery(rid=39, options=[DeliveryOption(price=10, day_from=4, day_to=5)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=804,
                dc_bucket_id=4,
                fesh=1,
                carriers=[258],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=3, day_from=4, day_to=5)]),
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=3, day_from=4, day_to=5)]),
                    RegionalDelivery(rid=3, options=[DeliveryOption(price=3, day_from=4, day_to=5)]),
                    RegionalDelivery(rid=5, options=[DeliveryOption(price=3, day_from=4, day_to=5)]),
                    RegionalDelivery(rid=39, options=[DeliveryOption(price=3, day_from=4, day_to=5)]),
                    RegionalDelivery(
                        rid=NORTH_CAUCASIAN_FEDERAL_DISTRICT_CHILD,
                        options=[DeliveryOption(price=3, day_from=4, day_to=5)],
                    ),
                    RegionalDelivery(
                        rid=SIBERIAN_FEDERAL_DISTRICT, options=[DeliveryOption(price=3, day_from=4, day_to=5)]
                    ),
                    RegionalDelivery(
                        rid=URAL_FEDERAL_DISTRICT, options=[DeliveryOption(price=3, day_from=4, day_to=5)]
                    ),
                    RegionalDelivery(
                        rid=FAR_EASTERN_FEDERAL_DISTRICT, options=[DeliveryOption(price=3, day_from=4, day_to=5)]
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=805,
                dc_bucket_id=5,
                fesh=1,
                carriers=[259],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=3, day_from=4, day_to=5)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=807,
                dc_bucket_id=71,
                fesh=1,
                carriers=[358],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=3, day_from=4, day_to=5)]),
                ],
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=901,
                dc_bucket_id=6,
                fesh=1,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                options=[
                    PickupOption(outlet_id=3001, price=15),
                    PickupOption(outlet_id=3002, price=15),
                    PickupOption(outlet_id=3003, price=15),
                ],
            ),
            PickupBucket(
                bucket_id=902,
                dc_bucket_id=7,
                fesh=1,
                carriers=[158],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                options=[
                    PickupOption(outlet_id=3004, price=10),
                ],
            ),
            PickupBucket(
                bucket_id=903,
                dc_bucket_id=8,
                fesh=1,
                carriers=[257],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                options=[
                    PickupOption(outlet_id=3005, price=10),
                    PickupOption(outlet_id=3006, price=10),
                    PickupOption(outlet_id=3007, price=10),
                    PickupOption(outlet_id=3008, price=10),
                    PickupOption(outlet_id=3009, price=10),
                    PickupOption(outlet_id=3010, price=10),
                ],
            ),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=2002,
                delivery_service_id=102,
                region=213,
                point_type=Outlet.FOR_POST,
                post_code=111,
                delivery_option=OutletDeliveryOption(shipper_id=102),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=2003,
                delivery_service_id=103,
                region=213,
                point_type=Outlet.FOR_POST,
                post_code=222,
                delivery_option=OutletDeliveryOption(shipper_id=103),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=2004,
                delivery_service_id=103,
                region=NORTH_CAUCASIAN_FEDERAL_DISTRICT_CHILD,
                point_type=Outlet.FOR_POST,
                post_code=223,
                delivery_option=OutletDeliveryOption(shipper_id=103),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=2005,
                delivery_service_id=103,
                region=SIBERIAN_FEDERAL_DISTRICT,
                point_type=Outlet.FOR_POST,
                post_code=224,
                delivery_option=OutletDeliveryOption(shipper_id=103),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=2006,
                delivery_service_id=103,
                region=URAL_FEDERAL_DISTRICT,
                point_type=Outlet.FOR_POST,
                post_code=225,
                delivery_option=OutletDeliveryOption(shipper_id=103),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=2007,
                delivery_service_id=103,
                region=FAR_EASTERN_FEDERAL_DISTRICT,
                point_type=Outlet.FOR_POST,
                post_code=226,
                delivery_option=OutletDeliveryOption(shipper_id=103),
                working_days=[i for i in range(10)],
            ),
        ]

        # post buckets
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=201,
                dc_bucket_id=51,
                fesh=1,
                carriers=[102],
                options=[PickupOption(outlet_id=2002, day_from=3, day_to=7)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=202,
                dc_bucket_id=52,
                fesh=1,
                carriers=[103],
                options=[
                    PickupOption(outlet_id=2003, day_from=3, day_to=4),
                    PickupOption(outlet_id=2004, day_from=3, day_to=4),
                    PickupOption(outlet_id=2005, day_from=3, day_to=4),
                    PickupOption(outlet_id=2006, day_from=3, day_to=4),
                    PickupOption(outlet_id=2007, day_from=3, day_to=4),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.delivery_calc.on_request_offer_buckets(weight=5, width=10, height=20, length=30, warehouse_id=1111).respond(
            [1, 2, 3, 4, 5], [6, 7, 8], [51, 52]
        )
        cls.delivery_calc.on_request_offer_buckets(weight=5, width=10, height=20, length=30, warehouse_id=145).respond(
            [1, 2, 3, 4, 5], [6, 7, 8], [51, 52]
        )
        cls.delivery_calc.on_request_offer_buckets(
            weight=10000, width=255, height=255, length=244, warehouse_id=1111
        ).respond([1, 2, 3, 4, 5], [6, 7, 8], [51, 52])
        cls.delivery_calc.on_request_offer_buckets(
            weight=219.6665625, width=59.5, height=49.5, length=89.5, warehouse_id=1111
        ).respond([1, 2, 3, 4, 5], [6, 7, 8], [51, 52])
        cls.delivery_calc.on_request_offer_buckets(
            weight=115.0046875, width=51.6771946, height=51.6771946, length=51.6771946, warehouse_id=1111
        ).respond([1, 2, 3, 4, 5], [6, 7, 8], [51, 52])

        cls.delivery_calc.on_request_offer_buckets(weight=5, width=10, height=20, length=30, warehouse_id=2222).respond(
            [1, 2, 3, 4, 5], [6, 7, 8], []
        )
        cls.delivery_calc.on_request_offer_buckets(
            weight=10, width=30, height=20, length=30, warehouse_id=1111
        ).respond([1, 2, 3, 4, 5], [6, 7, 8], [])
        cls.delivery_calc.on_request_offer_buckets(
            weight=10, width=30, height=20, length=30, warehouse_id=2222
        ).respond([1, 2, 3, 4, 5], [6, 7, 8], [])
        cls.delivery_calc.on_request_offer_buckets(weight=5, width=10, height=20, length=30, warehouse_id=3333).respond(
            [71], [], []
        )

        cls.index.outlets += [
            Outlet(
                point_id=3001,
                delivery_service_id=157,
                region=2,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=157, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=15
                ),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=3002,
                delivery_service_id=157,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=157, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=15
                ),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=3003,
                delivery_service_id=157,
                region=4,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=157, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=15
                ),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=3004,
                delivery_service_id=158,
                region=2,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=158, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=10
                ),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=3005,
                delivery_service_id=257,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=257, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=10
                ),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=3006,
                delivery_service_id=257,
                region=4,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=257, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=10
                ),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=3007,
                delivery_service_id=257,
                region=NORTH_CAUCASIAN_FEDERAL_DISTRICT_CHILD,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=257, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=10
                ),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=3008,
                delivery_service_id=257,
                region=SIBERIAN_FEDERAL_DISTRICT,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=257, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=10
                ),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=3009,
                delivery_service_id=257,
                region=URAL_FEDERAL_DISTRICT,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=257, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=10
                ),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=3010,
                delivery_service_id=257,
                region=FAR_EASTERN_FEDERAL_DISTRICT,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=257, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=10
                ),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title="blue offer sku2",
                hyperid=2,
                sku=2,
                waremd5="Sku2-wdDXWsIiLVm1goleg",
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=4,
                        offerid="blue.offer.2.1",
                        waremd5="Sku2Price5-IiLVm1Goleg",
                        weight=5,
                        blue_weight=5,
                        dimensions=OfferDimensions(
                            length=20,
                            width=30,
                            height=10,
                        ),
                        blue_dimensions=OfferDimensions(
                            length=20,
                            width=30,
                            height=10,
                        ),
                    ),
                ],
                delivery_buckets=[804],
                pickup_buckets=[901, 902, 903],
                post_buckets=[201, 202],
            ),
        ]

        # Добавляем динамик объекты для СД
        cls.dynamic.lms += [
            DynamicDeliveryServiceInfo(id=101, name="c_101", rating=4),
            DynamicDeliveryServiceInfo(id=102, name="c_102", rating=3),
            DynamicDeliveryServiceInfo(id=103, name="c_103"),
            DynamicDeliveryServiceInfo(id=157, name="c_157", rating=1),
            DynamicDeliveryServiceInfo(id=158, name="c_158", rating=2),
            DynamicDeliveryServiceInfo(id=257, name="c_257"),
        ]

        cls.dynamic.lms += [
            warehouse_1111,
            DynamicWarehouseInfo(id=2222, home_region=2, holidays_days_set_key=4),
            DynamicWarehouseInfo(id=3333, home_region=2, holidays_days_set_key=4),
            DynamicWarehouseInfo(id=147, home_region=213, holidays_days_set_key=4),
            DynamicWarehouseInfo(id=145, home_region=213, holidays_days_set_key=4),
            DynamicWarehouseInfo(id=164, home_region=213, holidays_days_set_key=5),
            DynamicDeliveryServiceInfo(id=99, rating=2),
            DynamicDeliveryServiceInfo(
                id=258,
                rating=2,
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(region_from=213, region_to=213, days_key=3),
                    DeliveryServiceRegionToRegionInfo(region_from=213, region_to=39, days_key=3),
                ],
                time_intervals=[
                    TimeIntervalsForRegion(
                        region=213,
                        intervals=[
                            TimeIntervalsForDaysInfo(intervals_key=4, days_key=0),
                            TimeIntervalsForDaysInfo(intervals_key=0, days_key=1),
                            TimeIntervalsForDaysInfo(intervals_key=1, days_key=2),
                        ],
                    )
                ],
            ),
            DynamicDeliveryServiceInfo(
                id=259,
                rating=2,
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=213, region_to=213, days_key=3)],
            ),
            DynamicDeliveryServiceInfo(id=358, rating=2),
            warehouse1111_delivery_service258,
            warehouse2222_delivery_service258,
            warehouse147_delivery_service257,
            warehouse147_delivery_service258,
            warehouse3333_delivery_service358,
            warehouse145_delivery_service358,
            warehouse1111_delivery_service157,
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=1111,
                delivery_service_id=158,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            warehouse1111_delivery_service257,
            warehouse1111_delivery_service102,
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=1111,
                delivery_service_id=103,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=258,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=164,
                delivery_service_id=99,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=22, region_to=225)],
            ),
            region_213_warehouses_priority,
            DynamicWarehousesPriorityInRegion(region=2, warehouses=[2222, 1111]),
            DynamicWarehousesPriorityInRegion(region=39, warehouses=[147, 1111]),
            # time intervals sets
            DynamicTimeIntervalsSet(
                key=0,
                intervals=[
                    TimeIntervalInfo(TimeInfo(19, 15), TimeInfo(23, 45)),
                    TimeIntervalInfo(TimeInfo(10, 0), TimeInfo(18, 30)),
                ],
            ),
            DynamicTimeIntervalsSet(
                key=1,
                intervals=[
                    TimeIntervalInfo(TimeInfo(10, 0), TimeInfo(17, 30)),
                ],
            ),
            DynamicTimeIntervalsSet(
                key=2,
                intervals=[
                    TimeIntervalInfo(TimeInfo(11, 0), TimeInfo(17, 00)),
                ],
            ),
            DynamicTimeIntervalsSet(
                key=3,
                intervals=[
                    TimeIntervalInfo(TimeInfo(11, 0), TimeInfo(15, 10)),
                ],
            ),
            DynamicTimeIntervalsSet(
                key=4,
                intervals=[
                    TimeIntervalInfo(TimeInfo(9, 0), TimeInfo(14, 30)),
                ],
            ),
            # days sets
            DynamicDaysSet(key=0, days=[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 14, 15]),
            DynamicDaysSet(key=1, days=[12]),
            DynamicDaysSet(key=2, days=[16]),
            DynamicDaysSet(key=3, days=[13, 15]),
            DynamicDaysSet(key=4, days=[1, 2, 3, 4, 5, 6]),
            DynamicDaysSet(key=5, days=[0, 1, 2, 3, 4, 5, 6]),
            DynamicDaysSet(key=6, days=[1, 2, 3]),
        ]

        cls.index.lms = copy.deepcopy(cls.dynamic.lms)
        cls.index.lms -= [warehouse1111_delivery_service258]
        cls.index.lms += [warehouse1111_delivery_service259]

    def check_free_delivery_threshold_in_regions(
        self, num, total_price, threshold_1, remainder_1, threshold_2, remainder_2, is_null_price
    ):
        for region, threshold, remainder in [
            (NORTH_CAUCASIAN_FEDERAL_DISTRICT_CHILD, threshold_1, remainder_1),
            (SIBERIAN_FEDERAL_DISTRICT, threshold_1, remainder_1),
            (URAL_FEDERAL_DISTRICT, threshold_1, remainder_1),
            (FAR_EASTERN_FEDERAL_DISTRICT, threshold_2, remainder_2),
        ]:
            response = self.report.request_json(
                'place=actual_delivery&offers-list=Sku2Price5-IiLVm1Goleg:{}&rids={}&force-use-delivery-calc=1&pickup-options=grouped&rearr-factors=market_conf_loyalty_delivery_threshold_enabled=1'.format(  # noqa
                    num, region
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "offersTotalPrice": {"value": total_price},
                    "freeDeliveryThreshold": {"value": threshold},
                    "freeDeliveryRemainder": {"value": remainder},
                    "results": [
                        {
                            "entity": "deliveryGroup",
                            "delivery": {
                                "options": [{"price": {"value": "0" if is_null_price else "99"}, "serviceId": "258"}],
                                # данные pickup-опций не возвращаются при разбиении заказа на 2 коробки
                                # "pickupOptions": [
                                #     {"price": {"value": "0" if is_null_price else "99"}, "serviceId": 257}
                                # ],
                                # "postOptions": [{"price": {"value": "0" if is_null_price else "99"}, "serviceId": 103}],
                            },
                        }
                    ],
                },
                allow_different_len=False,
            )

    def check_thresholds_no_dynamic(self):
        self.check_free_delivery_threshold_in_regions(1, "5", "5000", "4995", "7000", "6995", False)
        self.check_free_delivery_threshold_in_regions(2000, "10000", "5000", "0", "7000", "0", True)

    def test_free_delivery_threshold_in_regions(self):
        """Проверяется, что если суммарная цена офферов достигает порога, заданного для региона, то цена доставки обнуляется"""

        self.check_thresholds_no_dynamic()

        """thresholds, заданные через динамик, работают"""
        dynamic_thresholds = DynamicBlueFreeDeliveryThresholds(
            currency="RUR",
            thresholds_by_region=[
                DynamicBlueFreeDeliveryThresholdByRegion(region=NORTH_CAUCASIAN_FEDERAL_DISTRICT_CHILD, threshold=5001),
                DynamicBlueFreeDeliveryThresholdByRegion(region=SIBERIAN_FEDERAL_DISTRICT, threshold=5001),
                DynamicBlueFreeDeliveryThresholdByRegion(region=URAL_FEDERAL_DISTRICT, threshold=5001),
                DynamicBlueFreeDeliveryThresholdByRegion(region=FAR_EASTERN_FEDERAL_DISTRICT, threshold=7001),
            ],
        )

        self.dynamic.loyalty += [dynamic_thresholds]
        self.check_free_delivery_threshold_in_regions(1, "5", "5001", "4996", "7001", "6996", False)
        self.check_free_delivery_threshold_in_regions(2000, "10000", "5001", "0", "7001", "0", True)

        # В регионе, который не задан в динамике нет порога бесплатной доставки
        response = self.report.request_json(
            'place=actual_delivery&offers-list=Sku2Price5-IiLVm1Goleg:{}&rids={}&force-use-delivery-calc=1&pickup-options=grouped&rearr-factors=market_conf_loyalty_delivery_threshold_enabled=1'.format(  # noqa
                1, 213
            )
        )
        self.assertFragmentIn(
            response,
            {
                "offersTotalPrice": {"value": "5"},
                "freeDeliveryThreshold": Absent(),
                "results": [
                    {
                        "entity": "deliveryGroup",
                        "delivery": {
                            "options": [{"price": {"value": "99"}}],
                            # при разбиении заказа на 2 коробки опции самовывоза не возвращаются
                            # "pickupOptions": [{"price": {"value": "99"}}],
                            # "postOptions": [{"price": {"value": "99"}}],
                        },
                    }
                ],
            },
        )

        self.dynamic.loyalty -= [dynamic_thresholds]

        """После удаления значений динамика используются исходные значения thresholds"""
        self.check_thresholds_no_dynamic()


if __name__ == '__main__':
    main()
