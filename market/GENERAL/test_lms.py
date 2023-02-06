#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryCalcFeedInfo,
    DeliveryOption,
    DeliveryServicePriorityAndRegionInfo,
    DeliveryServiceRegionToRegionInfo,
    DynamicCapacityDaysOff,
    DynamicCapacityInfo,
    DynamicDaysSet,
    DynamicDeliveryConditionsByRegionInfo,
    DynamicDeliveryConditionsMetaInfo,
    DynamicDeliveryConditionsReferenceInfo,
    DynamicDeliveryConditionsSplitInfo,
    DynamicDeliveryServiceInfo,
    DynamicDeliveryThresholdsInfo,
    DynamicSkuOffer,
    DynamicTimeIntervalsSet,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    DynamicWeightBoundInfo,
    DynamicWeightRangeInfo,
    GpsCoord,
    MarketSku,
    MnPlace,
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
from core.matcher import Absent, NotEmpty
from core.logs import ErrorCodes
import copy
import urllib

USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"

weight_bounds = [
    DynamicWeightBoundInfo(id=1, weight=5, including=False),
    DynamicWeightBoundInfo(id=2, weight=5, including=True),
    DynamicWeightBoundInfo(id=3, weight=10, including=False),
    DynamicWeightBoundInfo(id=4, weight=10, including=True),
    DynamicWeightBoundInfo(id=5, weight=7, including=False),
    DynamicWeightBoundInfo(id=6, weight=7, including=True),
]

weight_ranges = [
    DynamicWeightRangeInfo(id=100, weight_lb_id=None, weight_ub_id=5),
    DynamicWeightRangeInfo(id=101, weight_lb_id=5, weight_ub_id=None),
    DynamicWeightRangeInfo(id=102, weight_lb_id=6, weight_ub_id=None),
    DynamicWeightRangeInfo(id=103, weight_lb_id=2, weight_ub_id=2),
    DynamicWeightRangeInfo(id=104, weight_lb_id=2, weight_ub_id=6),
    DynamicWeightRangeInfo(id=105, weight_lb_id=None, weight_ub_id=6),
    DynamicWeightRangeInfo(id=106, weight_lb_id=None, weight_ub_id=None),
]

warehouse_1111 = DynamicWarehouseInfo(id=1111, home_region=213, holidays_days_set_key=7)

region_213_warehouses_priority = DynamicWarehousesPriorityInRegion(region=213, warehouses=[1111, 147, 3333, 145, 164])
region_2_warehouses_priority = DynamicWarehousesPriorityInRegion(region=2, warehouses=[2222, 1111, 164])
region_5_warehouses_priority = DynamicWarehousesPriorityInRegion(region=5, warehouses=[164])
region_39_warehouses_priority = DynamicWarehousesPriorityInRegion(region=39, warehouses=[147, 1111, 164])

NORTH_CAUCASIAN_FEDERAL_DISTRICT = 102444
NORTH_CAUCASIAN_FEDERAL_DISTRICT_CHILD = 10244411
SIBERIAN_FEDERAL_DISTRICT = 59
URAL_FEDERAL_DISTRICT = 52
FAR_EASTERN_FEDERAL_DISTRICT = 73

NO_COMBINATOR_FLAG = '&combinator=0'


def get_warehouse_and_delivery_service(
    warehouse_id,
    service_id,
    add_date_switch_hour_for_region_213=True,
    is_active=True,
    shipment_holidays_days_set_key=6,
    date_switch_hour=1,
    capacity_by_region=None,
    priorities_and_regions=None,
    operation_time=0,
):
    if capacity_by_region is None:
        capacity_by_region = []
    if priorities_and_regions is None:
        priorities_and_regions = []
    date_switch_hours = [
        DateSwitchTimeAndRegionInfo(date_switch_hour=date_switch_hour, region_to=2),
        DateSwitchTimeAndRegionInfo(date_switch_hour=date_switch_hour, region_to=3),
        DateSwitchTimeAndRegionInfo(date_switch_hour=date_switch_hour, region_to=33),
        DateSwitchTimeAndRegionInfo(date_switch_hour=date_switch_hour, region_to=44),
        DateSwitchTimeAndRegionInfo(date_switch_hour=date_switch_hour, region_to=39),
        DateSwitchTimeAndRegionInfo(date_switch_hour=date_switch_hour, region_to=NORTH_CAUCASIAN_FEDERAL_DISTRICT),
        DateSwitchTimeAndRegionInfo(date_switch_hour=date_switch_hour, region_to=SIBERIAN_FEDERAL_DISTRICT),
        DateSwitchTimeAndRegionInfo(date_switch_hour=date_switch_hour, region_to=URAL_FEDERAL_DISTRICT),
        DateSwitchTimeAndRegionInfo(date_switch_hour=date_switch_hour, region_to=FAR_EASTERN_FEDERAL_DISTRICT),
        DateSwitchTimeAndRegionInfo(date_switch_hour=date_switch_hour, region_to=100),
    ]
    if add_date_switch_hour_for_region_213:
        date_switch_hours += [DateSwitchTimeAndRegionInfo(date_switch_hour=20, region_to=213)]
    return DynamicWarehouseAndDeliveryServiceInfo(
        warehouse_id=warehouse_id,
        delivery_service_id=service_id,
        operation_time=operation_time,
        date_switch_time_infos=date_switch_hours,
        shipment_holidays_days_set_key=shipment_holidays_days_set_key,
        is_active=is_active,
        capacity_by_region=capacity_by_region,
        priorities_and_regions=priorities_and_regions,
    )


warehouse1111_delivery_service258 = get_warehouse_and_delivery_service(1111, 258)
warehouse1111_delivery_service259 = get_warehouse_and_delivery_service(1111, 259)
warehouse1111_delivery_service260 = get_warehouse_and_delivery_service(1111, 260)
warehouse4444_delivery_service444 = get_warehouse_and_delivery_service(
    4444,
    444,
    shipment_holidays_days_set_key=8,
    operation_time=5,
)
warehouse2222_delivery_service258 = get_warehouse_and_delivery_service(
    2222,
    258,
    capacity_by_region=[
        DynamicCapacityInfo(
            region_to=100,
            capacity_days_off=[
                DynamicCapacityDaysOff(delivery_type=DynamicCapacityDaysOff.DT_COURIER, days_key=6),
                DynamicCapacityDaysOff(delivery_type=DynamicCapacityDaysOff.DT_PICKUP, days_key=6),
                DynamicCapacityDaysOff(delivery_type=DynamicCapacityDaysOff.DT_POST, days_key=6),
            ],
        ),
        DynamicCapacityInfo(
            region_to=300,
            capacity_days_off=[
                DynamicCapacityDaysOff(delivery_type=DynamicCapacityDaysOff.DT_COURIER, days_key=4),
                DynamicCapacityDaysOff(delivery_type=DynamicCapacityDaysOff.DT_PICKUP, days_key=0),
                DynamicCapacityDaysOff(delivery_type=DynamicCapacityDaysOff.DT_POST, days_key=4),
            ],
        ),
    ],
)
warehouse147_delivery_service257 = get_warehouse_and_delivery_service(147, 257)
warehouse147_delivery_service258 = get_warehouse_and_delivery_service(147, 258)
warehouse3333_delivery_service358 = get_warehouse_and_delivery_service(3333, 358)
warehouse145_delivery_service358 = get_warehouse_and_delivery_service(145, 358)

warehouse1111_delivery_service157 = DynamicWarehouseAndDeliveryServiceInfo(
    warehouse_id=1111,
    delivery_service_id=157,
    date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
    inbound_time=TimeInfo(0, 0),
    transfer_time=TimeInfo(0, 0),
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
warehouse1111_delivery_service103 = DynamicWarehouseAndDeliveryServiceInfo(
    warehouse_id=1111,
    delivery_service_id=103,
    date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
)
warehouse1111_delivery_service158 = DynamicWarehouseAndDeliveryServiceInfo(
    warehouse_id=1111,
    delivery_service_id=158,
    date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
)


class T(TestCase):
    @classmethod
    def prepare_blue_delivery_price(cls):
        '''
        Отключаем цену доставки синих оферов от пользователя, т.к. в этом тесте исторически много завязок на цену в тарифе
        https://st.yandex-team.ru/MARKETOUT-34206
        '''
        cls.settings.blue_delivery_price_enabled = False

    @classmethod
    def prepare(cls):
        cls.settings.blue_market_free_delivery_threshold = 3500
        cls.settings.blue_market_prime_free_delivery_threshold = 3500
        cls.settings.blue_market_yandex_plus_free_delivery_threshold = 3500
        cls.settings.nordstream_autogenerate = False
        cls.settings.default_search_experiment_flags += ['market_nordstream=0']
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']

        cls.index.regiontree += [
            Region(
                rid=14838,
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=1,
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[
                            Region(
                                rid=213,
                                tz_offset=10680,
                                children=[Region(rid=3, tz_offset=10800), Region(rid=4, tz_offset=10800)],
                            ),
                            Region(
                                rid=2,
                                tz_offset=10800,
                                children=[
                                    Region(rid=5, tz_offset=10800),
                                ],
                            ),
                        ],
                    ),
                    Region(
                        rid=11029,
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[
                            Region(rid=39, children=[Region(rid=123)]),
                        ],
                    ),
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
            Region(
                rid=26,
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=11030,
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[
                            Region(rid=100, children=[Region(rid=200), Region(rid=300)]),
                            Region(rid=4444, children=[Region(rid=44, children=[Region(rid=444), Region(rid=445)])]),
                        ],
                    )
                ],
            ),
        ]

        cls.settings.lms_autogenerate = False

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
            Shop(fesh=15, datafeed_id=15, priority_region=213, currency=Currency.RUR, blue='REAL', warehouse_id=171),
            Shop(fesh=16, datafeed_id=16, priority_region=213, currency=Currency.RUR, blue='REAL', warehouse_id=172),
            Shop(fesh=17, datafeed_id=17, priority_region=213, currency=Currency.RUR, blue='REAL', warehouse_id=172),
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
                    4001,
                    4002,
                    4003,
                    4004,
                    4005,
                    4006,
                ],
            ),
            Shop(
                fesh=44,
                datafeed_id=44,
                priority_region=213,
                name='blue_shop_with_delivery_holiday',
                currency=Currency.RUR,
                blue='REAL',
                warehouse_id=4444,
            ),
            Shop(
                fesh=33,
                datafeed_id=33,
                warehouse_id=1113,
                name='crossdock_shop',
                priority_region=213,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                fulfillment_program=True,
                direct_shipping=False,
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
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=15, day_from=10, day_to=11)]),
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=15, day_from=10, day_to=11)]),
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
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=15, day_from=10, day_to=11)]),
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
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=9, day_to=10)]),
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
                    RegionalDelivery(rid=100, options=[DeliveryOption(price=3, day_from=4, day_to=5)]),
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
            DeliveryBucket(
                bucket_id=808,
                fesh=1,
                carriers=[558],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=33, options=[DeliveryOption(price=3, day_from=4, day_to=5)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=809,
                fesh=1,
                carriers=[658],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=44, options=[DeliveryOption(price=3, day_from=4, day_to=5)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=810,
                dc_bucket_id=8100,
                fesh=1,
                carriers=[258],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=12345, options=[DeliveryOption(price=3, day_from=4, day_to=5)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=811,
                dc_bucket_id=8101,
                fesh=1,
                carriers=[260],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=39, options=[DeliveryOption(price=3, day_from=4, day_to=5)]),
                ],
            ),
            #      | ShopPrice
            #      |
            #      |
            #      |___________________b1
            #   12 |__________d3       |
            #   11 |__________|________|____c2
            #   10 |__________|g6      |    |
            #    9 |____e4____|___a0   |    |
            #    8 |____|_____|___|____|____f5
            #    7 |    |     |   |    |    |
            #      |    |     |   |    |    |
            #      |    |     |   |    |    |
            #      |    |     |   |    |    |
            #      |    |     |   |    |    |
            #      |    |     |   |    |    |
            #      |____|_____|___|____|____|____ time
            #           2     5   7    9    11
            #
            DeliveryBucket(
                bucket_id=1000,
                fesh=1,
                carriers=[700],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    # a
                    RegionalDelivery(rid=44, options=[DeliveryOption(shop_delivery_price=8, day_from=7, day_to=7)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=1001,
                fesh=1,
                carriers=[701],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    # b
                    RegionalDelivery(rid=44, options=[DeliveryOption(shop_delivery_price=12, day_from=9, day_to=9)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=1002,
                fesh=1,
                carriers=[702],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    # c
                    RegionalDelivery(rid=44, options=[DeliveryOption(shop_delivery_price=10, day_from=11, day_to=11)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=1003,
                fesh=1,
                carriers=[703],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    # d
                    RegionalDelivery(rid=44, options=[DeliveryOption(shop_delivery_price=11, day_from=5, day_to=5)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=1004,
                fesh=1,
                carriers=[704],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    # e
                    RegionalDelivery(rid=44, options=[DeliveryOption(shop_delivery_price=8, day_from=2, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=1005,
                fesh=1,
                carriers=[705],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    # f
                    RegionalDelivery(rid=44, options=[DeliveryOption(shop_delivery_price=7, day_from=11, day_to=11)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=1006,
                fesh=1,
                carriers=[706],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    # g
                    RegionalDelivery(rid=44, options=[DeliveryOption(shop_delivery_price=9, day_from=5, day_to=5)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=1007,
                fesh=1,
                carriers=[707],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    # h
                    RegionalDelivery(rid=44, options=[DeliveryOption(shop_delivery_price=9, day_from=5, day_to=5)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=1008,
                fesh=1,
                carriers=[708],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    # h
                    RegionalDelivery(rid=44, options=[DeliveryOption(shop_delivery_price=10, day_from=5, day_to=5)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=444,
                dc_bucket_id=1044,
                fesh=1,
                carriers=[444],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=15, day_from=1, day_to=2)]),
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
            PickupBucket(
                bucket_id=9001,
                dc_bucket_id=90010,
                fesh=1,
                carriers=[258],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                options=[
                    PickupOption(outlet_id=4001, price=15),
                    PickupOption(outlet_id=4002, price=15),
                    PickupOption(outlet_id=4003, price=15),
                ],
            ),
            PickupBucket(
                bucket_id=9002,
                dc_bucket_id=90020,
                fesh=1,
                carriers=[258],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                options=[
                    PickupOption(outlet_id=4004, price=15),
                    PickupOption(outlet_id=4005, price=15),
                    PickupOption(outlet_id=4006, price=15),
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
            weight=10000, width=260, height=250, length=250, warehouse_id=1111
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
        cls.delivery_calc.on_request_offer_buckets(weight=4, width=40, height=40, length=40, warehouse_id=4444).respond(
            [1044], [], []
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
                point_id=4001,
                delivery_service_id=258,
                region=100,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=258, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=15
                ),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=4002,
                delivery_service_id=258,
                region=200,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=258, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=15
                ),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=4003,
                delivery_service_id=258,
                region=300,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=258, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=15
                ),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=4004,
                delivery_service_id=258,
                region=100,
                point_type=Outlet.FOR_POST,
                delivery_option=OutletDeliveryOption(
                    shipper_id=258, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=15
                ),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=4005,
                delivery_service_id=258,
                region=200,
                point_type=Outlet.FOR_POST,
                delivery_option=OutletDeliveryOption(
                    shipper_id=258, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=15
                ),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=4006,
                delivery_service_id=258,
                region=300,
                point_type=Outlet.FOR_POST,
                delivery_option=OutletDeliveryOption(
                    shipper_id=258, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=15
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

        default_dimensions = OfferDimensions(
            length=20,
            width=30,
            height=10,
        )

        cls.index.mskus += [
            MarketSku(
                title="blue offer for delivery with holiday",
                hyperid=440,
                sku=440440,
                waremd5="Sku44WithHolidaydel__g",
                blue_offers=[
                    BlueOffer(
                        price=4444,
                        vat=Vat.VAT_10,
                        feedid=44,
                        offerid="offer_44",
                        waremd5="Sku44WithHolidayDeli_g",
                        weight=4,
                        dimensions=OfferDimensions(40, 40, 40),
                        is_fulfillment=True,
                    )
                ],
                delivery_buckets=[444],
            ),
            MarketSku(
                title="blue offer sku1",
                hyperid=1,
                sku=1,
                waremd5="Sku1-wdDXWsIiLVm1goleg",
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid="blue.offer.1.1",
                        waremd5="Sku1Price5-IiLVm1Goleg",
                        weight=5,
                        dimensions=default_dimensions,
                    ),
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=72,
                        offerid="blue.offer.1x.1",
                        waremd5="Sku1xPrice5-iLVm1Goleg",
                        weight=5,
                        dimensions=default_dimensions,
                    ),
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=33,
                        offerid="blue.offer.33.1",
                        waremd5="Sku33Price5-iLVm1Goleg",
                        weight=5,
                        dimensions=default_dimensions,
                    ),
                ],
                delivery_buckets=[801, 802, 803],
            ),
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
                        dimensions=default_dimensions,
                        has_delivery_options=False,  # no local delivery
                    ),
                    BlueOffer(
                        price=7,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid="blue.offer.2.2",
                        waremd5="Sku2Price7-IiLVm1Goleg",
                        weight=5,
                        dimensions=default_dimensions,
                    ),
                    BlueOffer(
                        price=8,
                        vat=Vat.VAT_10,
                        feedid=5,
                        offerid="blue.offer.2.3",
                        waremd5="Sku2Price8-IiLVm1Goleg",
                        weight=5,
                        dimensions=default_dimensions,
                    ),
                    BlueOffer(
                        price=9,
                        vat=Vat.VAT_10,
                        feedid=7,
                        offerid="blue.offer.2.4",
                        waremd5="Sku2Price9-IiLVm1Goleg",
                        weight=5,
                        dimensions=default_dimensions,
                    ),
                ],
                delivery_buckets=[804, 810, 811],
                pickup_buckets=[901, 902, 903, 9001],
                post_buckets=[201, 202, 9002],
            ),
            MarketSku(
                title="blue offer sku3",
                hyperid=3,
                sku=3,
                waremd5="Sku3-wdDXWsIiLVm1goleg",
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=4,
                        offerid="blue.offer.3.1",
                        waremd5="Sku3Price5-IiLVm1Goleg",
                        weight=5,
                        dimensions=default_dimensions,
                    ),
                    BlueOffer(
                        price=9,
                        vat=Vat.VAT_10,
                        feedid=7,
                        offerid="blue.offer.3.4",
                        waremd5="Sku3Price9-IiLVm1Goleg",
                        weight=5,
                        dimensions=default_dimensions,
                    ),
                    BlueOffer(
                        price=10,
                        vat=Vat.VAT_10,
                        feedid=72,
                        offerid="blue.offer.3.5",
                        waremd5="Sku3Pric10-IiLVm1Goleg",
                        weight=5,
                        dimensions=default_dimensions,
                    ),
                ],
                delivery_buckets=[804],
                pickup_buckets=[901, 902, 903],
            ),
            MarketSku(
                title="blue offer sku4",
                hyperid=4,
                sku=4,
                waremd5="Sku4-wdDXWsIiLVm1goleg",
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=70,
                        offerid="blue.offer.4",
                        waremd5="Sku4Price5-IiLVm1Goleg",
                        weight=5,
                        dimensions=default_dimensions,
                    ),
                    BlueOffer(
                        price=9,
                        vat=Vat.VAT_10,
                        feedid=71,
                        offerid="blue.offer.4",
                        waremd5="Sku4Price9-IiLVm1Goleg",
                        weight=5,
                        dimensions=default_dimensions,
                    ),
                ],
                delivery_buckets=[804],
                pickup_buckets=[901, 902, 903],
            ),
            MarketSku(
                title="blue offer sku5",
                hyperid=5,
                sku=5,
                waremd5="Sku5-wdDXWsIiLVm1goleg",
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=21,
                        offerid="blue.offer.5.1",
                        waremd5="Sku5Price5-IiLVm1Goleg",
                        weight=5,
                        dimensions=default_dimensions,
                    ),
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=22,
                        offerid="blue.offer.5.1",
                        waremd5="Sku5Pric22-IiLVm1Goleg",
                        weight=5,
                        dimensions=default_dimensions,
                    ),
                ],
                delivery_buckets=[807],
            ),
            MarketSku(
                title="dropshipped offer",
                hyperid=15,
                sku=1515,
                waremd5="Sku1515-Dropshippinggg",
                blue_offers=[
                    BlueOffer(
                        price=5151,
                        vat=Vat.VAT_10,
                        feedid=777,
                        offerid="drop.offer",
                        waremd5="Sku1515-Price5151____g",
                        weight=5,
                        dimensions=default_dimensions,
                        is_fulfillment=False,
                    )
                ],
            ),
            MarketSku(
                hyperid=150,
                sku=150150,
                waremd5="Sku15PriceN-IiLVm1Gole",
                blue_offers=[
                    BlueOffer(
                        price=5151,
                        vat=Vat.VAT_10,
                        feedid=15,
                        offerid="offer_15",
                        waremd5="Sku1500-Price5151____g",
                        weight=5,
                        dimensions=default_dimensions,
                        is_fulfillment=True,
                    )
                ],
                delivery_buckets=[808],
            ),
            MarketSku(
                hyperid=160,
                sku=160160,
                waremd5="Sku16PriceN-IiLVm1Gole",
                blue_offers=[
                    BlueOffer(
                        price=5151,
                        vat=Vat.VAT_10,
                        feedid=16,
                        offerid="offer_16",
                        waremd5="Sku1600-Price5151____g",
                        weight=5,
                        dimensions=default_dimensions,
                        is_fulfillment=True,
                    )
                ],
                delivery_buckets=[809],
            ),
            MarketSku(
                hyperid=170,
                sku=170170,
                waremd5="Sku17PriceN-IiLVm1Gole",
                blue_offers=[
                    BlueOffer(
                        price=5151,
                        vat=Vat.VAT_10,
                        feedid=17,
                        offerid="offer_17",
                        waremd5="Sku1700-Price5151____g",
                        weight=8,
                        blue_weight=8,
                        dimensions=default_dimensions,
                        blue_dimensions=default_dimensions,
                        is_fulfillment=True,
                        delivery_buckets=[1000, 1003],
                    ),
                    BlueOffer(
                        price=5151,
                        vat=Vat.VAT_10,
                        feedid=17,
                        offerid="offer_17.1",
                        waremd5="Sku1700-Price5151___1g",
                        weight=8,
                        blue_weight=8,
                        dimensions=default_dimensions,
                        blue_dimensions=default_dimensions,
                        is_fulfillment=True,
                        delivery_buckets=[1000, 1004],
                    ),
                    BlueOffer(
                        price=5151,
                        vat=Vat.VAT_10,
                        feedid=17,
                        offerid="offer_17.2",
                        waremd5="Sku1700-Price5151___2g",
                        weight=8,
                        blue_weight=8,
                        dimensions=default_dimensions,
                        blue_dimensions=default_dimensions,
                        is_fulfillment=True,
                        delivery_buckets=[1000, 1003],
                    ),
                    BlueOffer(
                        price=5151,
                        vat=Vat.VAT_10,
                        feedid=17,
                        offerid="offer_17.3",
                        waremd5="Sku1700-Price5151___3g",
                        weight=8,
                        blue_weight=8,
                        dimensions=default_dimensions,
                        blue_dimensions=default_dimensions,
                        is_fulfillment=True,
                        delivery_buckets=[1003, 1006],
                    ),
                    BlueOffer(
                        price=5151,
                        vat=Vat.VAT_10,
                        feedid=17,
                        offerid="offer_17.4",
                        waremd5="Sku1700-Price5151___4g",
                        weight=8,
                        blue_weight=8,
                        dimensions=default_dimensions,
                        blue_dimensions=default_dimensions,
                        is_fulfillment=True,
                        delivery_buckets=[1004, 1003, 1005, 1001],
                    ),
                    BlueOffer(
                        price=5151,
                        vat=Vat.VAT_10,
                        feedid=17,
                        offerid="offer_17.5",
                        waremd5="Sku1700-Price5151___5g",
                        weight=8,
                        blue_weight=8,
                        dimensions=default_dimensions,
                        blue_dimensions=default_dimensions,
                        is_fulfillment=True,
                        delivery_buckets=[1003, 1005, 1001],
                    ),
                    BlueOffer(
                        price=5151,
                        vat=Vat.VAT_10,
                        feedid=17,
                        offerid="offer_17.6",
                        waremd5="Sku1700-Price5151___6g",
                        weight=8,
                        blue_weight=8,
                        dimensions=default_dimensions,
                        blue_dimensions=default_dimensions,
                        is_fulfillment=True,
                        delivery_buckets=[1005, 1001],
                    ),
                    BlueOffer(
                        price=5151,
                        vat=Vat.VAT_10,
                        feedid=17,
                        offerid="offer_17.7",
                        waremd5="Sku1700-Price5151___7g",
                        weight=8,
                        blue_weight=8,
                        dimensions=default_dimensions,
                        blue_dimensions=default_dimensions,
                        is_fulfillment=True,
                        delivery_buckets=[1001, 1002],
                    ),
                    BlueOffer(
                        price=5151,
                        vat=Vat.VAT_10,
                        feedid=17,
                        offerid="offer_17.8",
                        waremd5="Sku1700-Price5151___8g",
                        weight=8,
                        blue_weight=8,
                        dimensions=default_dimensions,
                        blue_dimensions=default_dimensions,
                        is_fulfillment=True,
                        delivery_buckets=[1004, 1006],
                    ),
                    BlueOffer(
                        price=5151,
                        vat=Vat.VAT_10,
                        feedid=17,
                        offerid="offer_17_testdlvry",
                        waremd5="Sku1700-TestDlvry____g",
                        weight=8,
                        blue_weight=8,
                        dimensions=default_dimensions,
                        blue_dimensions=default_dimensions,
                        is_fulfillment=True,
                        delivery_buckets=[1007, 1008],
                    ),
                ],
            ),
        ]

        # Добавляем динамик объекты для СД
        cls.dynamic.lms += [
            DynamicDeliveryServiceInfo(id=ds_id, name=ds_name, rating=ds_rating)
            for ds_id, ds_name, ds_rating in [
                (101, "c_101", 4),
                (102, "c_102", 3),
                (157, "c_157", 1),
                (158, "c_158", 2),
                (257, "c_257", None),
                (360, "c_360", 1),
            ]
        ]

        cls.dynamic.lms += [
            warehouse_1111,
            DynamicWarehouseInfo(id=1113, home_region=213),
            DynamicWarehouseInfo(id=2222, home_region=2, holidays_days_set_key=7),
            DynamicWarehouseInfo(id=3333, home_region=2, holidays_days_set_key=4),
            DynamicWarehouseInfo(id=4444, home_region=213, holidays_days_set_key=7),
            DynamicWarehouseInfo(id=147, home_region=39, holidays_days_set_key=4),
            DynamicWarehouseInfo(id=145, home_region=213, holidays_days_set_key=4),
            DynamicWarehouseInfo(id=164, home_region=213, holidays_days_set_key=5),
            DynamicWarehouseInfo(id=171, home_region=33, holidays_days_set_key=5),
            DynamicWarehouseInfo(id=172, home_region=44, holidays_days_set_key=7),
            DynamicDeliveryServiceInfo(id=103, rating=2),
            DynamicDeliveryServiceInfo(id=99, rating=2),
            DynamicDeliveryServiceInfo(
                id=258,
                rating=2,
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(region_from=213, region_to=213, days_key=3),
                    DeliveryServiceRegionToRegionInfo(region_from=213, region_to=39, days_key=3),
                    DeliveryServiceRegionToRegionInfo(region_from=1, region_to=39, days_key=3),
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
            DynamicDeliveryServiceInfo(
                id=260,
                rating=2,
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=39, region_to=225, days_key=3)],
            ),
            DynamicDeliveryServiceInfo(id=358, rating=2),
            DynamicDeliveryServiceInfo(
                id=444,
                rating=1,
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(region_from=213, region_to=213, days_key=8),
                ],
                time_intervals=[
                    TimeIntervalsForRegion(
                        region=213, intervals=[TimeIntervalsForDaysInfo(intervals_key=1, days_key=0)]
                    ),
                ],
            ),
            DynamicDeliveryServiceInfo(id=558, rating=2),
            DynamicDeliveryServiceInfo(id=658, rating=2),
            DynamicDeliveryServiceInfo(id=700, rating=2),
            DynamicDeliveryServiceInfo(id=701, rating=2),
            DynamicDeliveryServiceInfo(id=702, rating=2),
            DynamicDeliveryServiceInfo(id=703, rating=2),
            DynamicDeliveryServiceInfo(id=704, rating=2),
            DynamicDeliveryServiceInfo(id=705, rating=2),
            DynamicDeliveryServiceInfo(id=706, rating=2),
            DynamicDeliveryServiceInfo(id=707, rating=2),
            DynamicDeliveryServiceInfo(id=708, rating=2),
            warehouse1111_delivery_service258,
            warehouse2222_delivery_service258,
            warehouse147_delivery_service257,
            warehouse147_delivery_service258,
            warehouse3333_delivery_service358,
            warehouse145_delivery_service358,
            warehouse1111_delivery_service157,
            warehouse1111_delivery_service158,
            warehouse1111_delivery_service257,
            warehouse1111_delivery_service102,
            warehouse1111_delivery_service103,
            warehouse1111_delivery_service260,
            warehouse4444_delivery_service444,
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
            get_warehouse_and_delivery_service(171, 558),
            get_warehouse_and_delivery_service(172, 658),
            get_warehouse_and_delivery_service(172, 700, shipment_holidays_days_set_key=7, date_switch_hour=10),
            get_warehouse_and_delivery_service(172, 701, shipment_holidays_days_set_key=7, date_switch_hour=10),
            get_warehouse_and_delivery_service(
                172,
                702,
                shipment_holidays_days_set_key=7,
                date_switch_hour=10,
                priorities_and_regions=[DeliveryServicePriorityAndRegionInfo(200, 44)],
            ),
            get_warehouse_and_delivery_service(172, 703, shipment_holidays_days_set_key=7, date_switch_hour=10),
            get_warehouse_and_delivery_service(172, 704, shipment_holidays_days_set_key=7, date_switch_hour=10),
            get_warehouse_and_delivery_service(172, 705, shipment_holidays_days_set_key=7, date_switch_hour=10),
            get_warehouse_and_delivery_service(172, 706, shipment_holidays_days_set_key=7, date_switch_hour=10),
            get_warehouse_and_delivery_service(
                172,
                707,
                shipment_holidays_days_set_key=7,
                date_switch_hour=10,
                priorities_and_regions=[DeliveryServicePriorityAndRegionInfo(200, 44)],
            ),
            get_warehouse_and_delivery_service(
                172,
                708,
                shipment_holidays_days_set_key=7,
                date_switch_hour=10,
                priorities_and_regions=[DeliveryServicePriorityAndRegionInfo(201, 44)],
            ),
            DynamicWarehouseToWarehouseInfo(
                warehouse_from=1113,
                warehouse_to=1111,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=19, region_to=225)],
            ),
            DynamicWarehouseToWarehouseInfo(warehouse_from=1111, warehouse_to=1111),
            DynamicWarehouseToWarehouseInfo(warehouse_from=2222, warehouse_to=2222),
            DynamicWarehouseToWarehouseInfo(warehouse_from=3333, warehouse_to=3333),
            DynamicWarehouseToWarehouseInfo(warehouse_from=4444, warehouse_to=4444),
            DynamicWarehouseToWarehouseInfo(warehouse_from=147, warehouse_to=147),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
            DynamicWarehouseToWarehouseInfo(warehouse_from=164, warehouse_to=164),
            DynamicWarehouseToWarehouseInfo(warehouse_from=171, warehouse_to=171),
            DynamicWarehouseToWarehouseInfo(warehouse_from=172, warehouse_to=172),
            region_213_warehouses_priority,
            region_2_warehouses_priority,
            region_5_warehouses_priority,
            region_39_warehouses_priority,
            DynamicWarehousesPriorityInRegion(region=33, warehouses=[171]),
            DynamicWarehousesPriorityInRegion(region=44, warehouses=[172]),
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
            DynamicDaysSet(key=7, days=[]),
            DynamicDaysSet(key=8, days=[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 16]),
        ]

        cls.index.lms = copy.deepcopy(cls.dynamic.lms)
        cls.index.lms -= [warehouse1111_delivery_service258]
        cls.index.lms += [warehouse1111_delivery_service259]

    def check_lms_delivery_service_courier(self, region, service_id):
        request = (
            "place=actual_delivery&offers-list=Sku1Price5-IiLVm1Goleg:1&rids={}".format(region) + NO_COMBINATOR_FLAG
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'delivery': {
                            'options': [
                                {
                                    'serviceId': service_id,
                                }
                            ]
                        }
                    }
                ]
            },
        )

    def check_lms_delivery_service_pickup(self, region, service_id_1, service_id_2):
        request = "place=actual_delivery&offers-list=Sku2Price5-IiLVm1Goleg:1&rids={}&force-use-delivery-calc=1&pickup-options=grouped".format(
            region
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'delivery': {
                            'pickupOptions': [
                                {
                                    'serviceId': service_id_1,
                                },
                                {
                                    'serviceId': service_id_2,
                                },
                            ]
                        }
                    }
                ]
            },
            preserve_order=True,
        )

    def test_lms_rating(self):
        '''
        Проверяем управление рейтингами СД через динамик LMS
        '''

        # Вначале служба 158 имеет больший рейтинг из индекса
        self.check_lms_delivery_service_courier(2, "158")

        # Добавляем динамик объект для службы 157
        self.dynamic.lms += [
            DynamicDeliveryServiceInfo(157, rating=3),
        ]

        # Теперь 157 служба имеет более высокий рейтинг и доставляет офер
        self.check_lms_delivery_service_courier(2, "157")

    def test_lms_regional_priority(self):
        '''
        Проверяем управление региональными приоритетами СД через динамик LMS
        '''

        # Вначале у 257 службы лучшие условия доставки
        self.check_lms_delivery_service_courier(213, "257")
        self.check_lms_delivery_service_pickup(213, 257, 157)

        # Добавляем динамик объекты для СД с приоритетами для региона (они важнее цен и рейтингов)
        self.dynamic.lms -= [warehouse1111_delivery_service157, warehouse1111_delivery_service257]
        self.dynamic.lms += [
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=1111,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
                priorities_and_regions=[DeliveryServicePriorityAndRegionInfo(2, 213)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=1111,
                delivery_service_id=257,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
                priorities_and_regions=[DeliveryServicePriorityAndRegionInfo(1, 213)],
            ),
        ]

        # Теперь 157 служба имеет больший приоритет и доставляет офер
        self.check_lms_delivery_service_courier(213, "157")
        self.check_lms_delivery_service_pickup(213, 157, 257)

        # Удаляем динамик объекты для СД
        self.dynamic.lms -= [warehouse1111_delivery_service257]

        # Добавляем новые динамик объекты для СД с одинаковыми приоритетами
        self.dynamic.lms += [
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=1111,
                delivery_service_id=257,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
                priorities_and_regions=[DeliveryServicePriorityAndRegionInfo(2, 213)],
            )
        ]

        # Теперь 257 служба доставляет офер (меньшая цена)
        self.check_lms_delivery_service_courier(213, "257")
        self.check_lms_delivery_service_pickup(213, 257, 157)

        # Удаляем динамик объекты для СД
        self.dynamic.lms -= [warehouse1111_delivery_service157]

        # Добавляем новые динамик объекты с разными приоритетами
        self.dynamic.lms += [
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=1111,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
                priorities_and_regions=[DeliveryServicePriorityAndRegionInfo(3, 213)],
            ),
        ]

        # Теперь 157 служба имеет больший приоритет в дочернем регионе
        self.check_lms_delivery_service_courier(4, "157")
        self.check_lms_delivery_service_pickup(4, 157, 257)

    def check_lms_core(self, rids):
        request = 'place=actual_delivery&offers-list=Sku1Price5-IiLVm1Goleg:1&rids={}&force-use-delivery-calc=1'.format(
            rids
        )
        return self.report.request_json(request)

    def check_lms_delivery(self, rids, day_from, day_to, order_before, shipment_day, service_id):
        response = self.check_lms_core(rids)
        self.assertFragmentIn(
            response,
            {
                'entity': 'deliveryGroup',
                'delivery': {
                    'options': [
                        {
                            "dayFrom": day_from,
                            "dayTo": day_to,
                            "orderBefore": order_before,
                            "isDefault": True,
                            "serviceId": service_id,
                            "shipmentDay": shipment_day,
                        }
                    ]
                },
            },
        )

    def test_lms(self):
        """Проверяется, что время доставки, заданное календарями из LMS вычисляется корректно"""
        self.check_lms_delivery(213, 4, 5, "20", 0, "258")

        """Проверяется связка warehouse-delivery_service"""
        """    если убрать связку, то служба меняется на другую (257)"""
        self.dynamic.lms -= [warehouse1111_delivery_service258]
        self.check_lms_delivery(213, 9, 10, "23", 0, "257")
        """    если вернуть связку, то опция службы 258 возвращается"""
        self.dynamic.lms += [warehouse1111_delivery_service258]
        self.check_lms_delivery(213, 4, 5, "20", 0, "258")

        """Проверяется откат на исходный lms файл в report_data в случае невозможности загрузки динамика"""
        self.dynamic.remove_lms_dynamic = True
        """    вместо службы 258 (из динамика) выбирается служба 259 (из report_data)"""
        """    При первом запросе происходит откат на исходный lms файл в report_data"""
        """    При втором ничего не меняется: используется все тот же файл из report_data"""
        for _ in range(0, 1):
            self.check_lms_delivery(213, 4, 5, "20", 0, "259")
        NO_LMS_DYNAMIC_ERROR_CODE = 9270
        """    и генерируется ошибка в error.log"""
        self.error_log.expect(code=NO_LMS_DYNAMIC_ERROR_CODE).times(1)
        self.base_logs_storage.error_log.expect(code=NO_LMS_DYNAMIC_ERROR_CODE).times(1)

        """После возвращения динамика под репорт снова выбирается служба 258"""
        self.dynamic.remove_lms_dynamic = False
        self.check_lms_delivery(213, 4, 5, "20", 0, "258")

    def test_date_switch_hour(self):
        """Проверяется, что с другим часом перескока для региона 2 (по отношению к региону 213) вычисляются другие сроки доставки."""
        for rids in [2, 3]:
            self.check_lms_delivery(rids, 8, 9, Absent(), 4, "258")
        """Проверяется, что для региона 4 (дочернего по отношению к 213) берется час перескока для 213 региона"""
        self.check_lms_delivery(4, 4, 5, "20", 0, "258")

    def test_warehouse_id(self):
        """Проверяется вывод иденитфикатора склада (warehouseId) для поставщика"""
        response = self.report.request_json(
            'place=actual_delivery&offers-list=Sku1Price5-IiLVm1Goleg:1&rids=213&force-use-delivery-calc=1'
        )
        self.assertFragmentIn(
            response, {'entity': 'offer', 'supplier': {'entity': 'shop', 'id': 3, 'warehouseId': 1111}}
        )
        """Проверяется фильтр оферов по предпочтительному складу"""
        """Сначала в place=sku_offers выводится офер от магазина с дефолтным складом 1111"""
        response = self.report.request_json(
            'place=sku_offers&market-sku=2&show-urls=direct&rids=2&offerid=Sku2Price5-IiLVm1Goleg'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'supplier': {'entity': 'shop', 'id': 3, 'warehouseId': 1111},
                'wareId': 'Sku2Price5-IiLVm1Goleg',
            },
        )
        """Здесь вводится предпочтительный склад 2222 и проверяется, что с этого склада нашелся другой офер (от другого магазина и с другим wareId)"""
        response = self.report.request_json(
            'place=sku_offers&market-sku=2&show-urls=direct&rids=2&offerid=Sku2Price5-IiLVm1Goleg&warehouse-id=2222'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'supplier': {'entity': 'shop', 'id': 10, 'warehouseId': 2222},
                'wareId': 'Sku2Price8-IiLVm1Goleg',
            },
        )
        """Со склада 3333 никакой магазин не поставляет этот товар -- оферов нет"""
        response = self.report.request_json(
            'place=sku_offers&market-sku=2&show-urls=direct&rids=213&offerid=Sku2Price5-IiLVm1Goleg&warehouse-id=3333'
        )
        self.assertFragmentNotIn(response, {'entity': 'offer', 'supplier': {'entity': 'shop'}})
        """Альтернативное написание флага (через подчеркивание)"""
        response = self.report.request_json(
            'place=sku_offers&market-sku=2&show-urls=direct&rids=2&offerid=Sku2Price5-IiLVm1Goleg&warehouse_id=2222'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'supplier': {'entity': 'shop', 'id': 10, 'warehouseId': 2222},
                'wareId': 'Sku2Price8-IiLVm1Goleg',
            },
        )

        """
        Проверяется, что если в &warehouse-id передается не определенный в LMS склад,
        то в error.log производится логгирование ошибки CGI_UNKNOWN_WAREHOUSE_ID
        """
        request = (
            'place=sku_offers'
            '&market-sku=2'
            '&show-urls=direct'
            '&rids=213'
            '&offerid=Sku2Price5-IiLVm1Goleg'
            '&warehouse-id=666'
        )
        self.report.request_json(request)
        self.error_log.expect(code=ErrorCodes.CGI_UNKNOWN_WAREHOUSE_ID)

    def check_with_particular_offers(self, response, waremd5_arr):
        self.assertFragmentIn(
            response,
            {"results": [{'entity': 'offer', 'wareId': wareId} for wareId in waremd5_arr]},
            allow_different_len=False,
        )

    def test_multi_warehouses(self):
        """Проверяется, что для разных регионов выбираются товары с соответствующих приоритетных складов"""

        """    place=offerinfo: возвращаются все запрошенные офферы без приоритизации по warehouseId, но учитывая возможность доставки из этого склада в регион пользователя"""
        request = 'place=offerinfo&rgb=blue&rids={}&show-urls=encrypted&regset=2'
        separate_offers = 'Sku2Price5-IiLVm1Goleg,Sku2Price9-IiLVm1Goleg'
        for region, offers_in_region, separate_result in [
            (
                213,
                ['Sku2Price5-IiLVm1Goleg', 'Sku2Price7-IiLVm1Goleg'],
                ['Sku2Price5-IiLVm1Goleg'],
            ),  # Нашлись только оферы со склада 1111
            (
                2,
                [
                    'Sku2Price5-IiLVm1Goleg',
                    'Sku2Price7-IiLVm1Goleg',
                    'Sku2Price8-IiLVm1Goleg',
                    'Sku2Price9-IiLVm1Goleg',
                ],
                ['Sku2Price5-IiLVm1Goleg', 'Sku2Price9-IiLVm1Goleg'],
            ),  # Нашлись оферы обоих складов
            (5, [], []),  # Оба склада запрещены к доставке в этот регион
        ]:
            # по msku
            response = self.report.request_json(request.format(region) + '&market-sku=2')
            self.check_with_particular_offers(response, offers_in_region)
            # по offerid: правила выбора офферов по warehouseId игнорируются
            response = self.report.request_json(request.format(region) + '&offerid=' + separate_offers)
            self.check_with_particular_offers(response, separate_result)

        """    place=sku_offers"""
        """    Проверяется, что:"""
        """        1) Для msku=2 правила выбора офферов по warehouseId игнорируются, т.к. офферы заданы явно"""
        """        2) Для msku=3 выбиратеся оффер из соответствующего warehouseId"""
        request = 'place=sku_offers&market-sku=2,3&rgb=blue&rids={}&offerid={}&regset=2'
        for region, msku2_wareId, msku2_warehouse, msku3_wareId, msku3_warehouseId in (
            (2, 'Sku2Price8-IiLVm1Goleg', 2222, 'Sku3Price9-IiLVm1Goleg', 2222),  # В регионе 2 доступны оба склада
            (2, 'Sku2Price5-IiLVm1Goleg', 1111, 'Sku3Price9-IiLVm1Goleg', 2222),
            (
                213,
                'Sku2Price5-IiLVm1Goleg',
                1111,
                'Sku3Price5-IiLVm1Goleg',
                1111,
            ),  # В регионе 213 доступен только склад 1111
        ):
            response = self.report.request_json(request.format(region, msku2_wareId))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": "2",
                            "offers": {
                                "items": [
                                    {
                                        'entity': 'offer',
                                        'wareId': msku2_wareId,
                                        'supplier': {'warehouseId': msku2_warehouse},
                                    }
                                ]
                            },
                        },
                        {
                            "entity": "sku",
                            "id": "3",
                            "offers": {
                                "items": [
                                    {
                                        'entity': 'offer',
                                        'wareId': msku3_wareId,
                                        'supplier': {'warehouseId': msku3_warehouseId},
                                    }
                                ]
                            },
                        },
                    ]
                },
                allow_different_len=False,
            )

        # Склады 1111 и 2222 не доступны в 5 регионе. У СКУ нет оферов
        for msku2_offer in ('Sku2Price8-IiLVm1Goleg', 'Sku2Price5-IiLVm1Goleg'):
            response = self.report.request_json(request.format(5, msku2_offer))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {"entity": "sku", "id": "2", "offers": {"items": Absent()}},
                        {
                            "entity": "sku",
                            "id": "3",
                            "offers": {
                                "items": Absent(),
                            },
                        },
                    ]
                },
                allow_different_len=False,
            )

        """    place 'prime' """
        for region, wareId, regional_delimiter in [
            (2, 'Sku2Price8-IiLVm1Goleg', []),
            (213, 'Sku2Price5-IiLVm1Goleg', []),
        ]:
            response = self.report.request_json(
                'place=prime&rids={}&hyperid=2&rearr-factors=market_metadoc_search=no'.format(region)
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            'entity': 'offer',
                            'wareId': wareId,
                        },
                    ]
                    + regional_delimiter
                },
                allow_different_len=False,
            )

        """    place 'actual_delivery' """
        request = 'place=actual_delivery&offers-list=Sku1Price5-IiLVm1Goleg:1,Sku2Price8-IiLVm1Goleg:1&rids={}&regset=2'
        # В регионе 2 доступны оба офера
        response = self.report.request_json(request.format(2))
        self.assertFragmentIn(
            response,
            {
                "offers": [
                    {
                        'entity': 'offer',
                        'wareId': 'Sku1Price5-IiLVm1Goleg',
                    },
                    {
                        'entity': 'offer',
                        'wareId': 'Sku2Price8-IiLVm1Goleg',
                    },
                ]
            },
            allow_different_len=False,
        )

        # В регионе 213 офер со склада 2222 не доступен
        response = self.report.request_json(request.format(213))
        self.assertFragmentIn(
            response,
            {"offerProblems": [{"wareId": "Sku2Price8-IiLVm1Goleg", "problems": ["NONEXISTENT_OFFER"]}]},
            allow_different_len=False,
        )
        self.error_log.expect(code=ErrorCodes.ACD_NOT_ALL_THE_SAME_WAREHOUSE_ID)

        # В регионе 5 оба офера не доступны
        response = self.report.request_json(request.format(5))
        self.assertFragmentIn(
            response,
            {
                "offerProblems": [
                    {"wareId": "Sku1Price5-IiLVm1Goleg", "problems": ["NONEXISTENT_OFFER"]},
                    {"wareId": "Sku2Price8-IiLVm1Goleg", "problems": ["NONEXISTENT_OFFER"]},
                ]
            },
            allow_different_len=False,
        )

    def test_multi_warehouses_with_dynamic(self):
        """Проверяется, что фильтрация синих офферов зависит от склада"""
        dynamic_offer_1 = DynamicSkuOffer(shop_id=11, sku='blue.offer.4', warehouse_id=1111)
        dynamic_offer_2 = DynamicSkuOffer(shop_id=11, sku='blue.offer.4', warehouse_id=2222)
        dynamic_offer_3 = DynamicSkuOffer(shop_id=11, sku='blue.offer.4')  # offer from default warehouse
        for filters, ware_ids in [
            ([dynamic_offer_1], ['Sku4Price9-IiLVm1Goleg']),
            ([dynamic_offer_2], ['Sku4Price5-IiLVm1Goleg']),
            ([dynamic_offer_3], ['Sku4Price9-IiLVm1Goleg', 'Sku4Price5-IiLVm1Goleg']),
        ]:
            self.dynamic.disabled_sku_offers = filters
            response = self.report.request_json(
                'place=offerinfo&rgb=blue&rids=2&show-urls=encrypted&regset=1&offerid=Sku4Price5-IiLVm1Goleg,Sku4Price9-IiLVm1Goleg'
            )
            self.check_with_particular_offers(response, ware_ids)

        """проверяются preorder офферы"""
        request = 'place=actual_delivery&offers-list=Sku4Price5-IiLVm1Goleg:1,Sku4Price9-IiLVm1Goleg:1&rids=2'
        for filters, ware_ids, preorder in [
            ([dynamic_offer_1], ['Sku4Price5-IiLVm1Goleg'], [True, Absent()]),
            ([dynamic_offer_2], ['Sku4Price9-IiLVm1Goleg'], [Absent(), True]),
            ([dynamic_offer_3], [], [Absent(), Absent()]),
        ]:
            self.dynamic.preorder_sku_offers = filters

            if len(ware_ids):
                """без флага show-preorder=1"""
                response = self.report.request_json(request)
                self.assertFragmentIn(
                    response,
                    {
                        "total": 0,
                        "offerProblems": [{"wareId": ware_ids[0], "problems": ["NONEXISTENT_OFFER"]}],
                        "results": [],
                    },
                    allow_different_len=False,
                )

            """с флагом show-preorder=1"""
            response = self.report.request_json(request + '&show-preorder=1')
            self.assertFragmentIn(
                response,
                {
                    "offers": [
                        {'entity': 'offer', 'wareId': 'Sku4Price5-IiLVm1Goleg', 'isPreorder': preorder[0]},
                        {'entity': 'offer', 'wareId': 'Sku4Price9-IiLVm1Goleg', 'isPreorder': preorder[1]},
                    ]
                },
                allow_different_len=False,
            )
            self.error_log.expect(code=ErrorCodes.ACD_NOT_ALL_THE_SAME_WAREHOUSE_ID)

    def check_warehouse_and_delivery_service_relations(
        self, old_relations, new_relations, check_switched_off_relations=False
    ):
        request_actual_delivery = (
            'place=actual_delivery'
            '&offers-list=Sku2Price5-IiLVm1Goleg:1'
            '&rids=213'
            '&force-use-delivery-calc=1'
            '&pickup-options=grouped'
            '&debug-all-courier-options=1'
        )
        request_sku_offers = 'place=sku_offers&market-sku=2&rgb=blue&rids=213&pickup-options=grouped'

        def check_actual_delivery(request):
            disable_post_as_pickup_rearr = '&rearr-factors=market_use_post_as_pickup=0'
            pickup_options = [{"serviceId": 157}, {"serviceId": 257}]
            post_options = [
                {"serviceId": 102, "dayFrom": 3, "dayTo": 7},
                {"serviceId": 103, "dayFrom": 3, "dayTo": 4},
            ]
            for disable_post_as_pickup in ['', disable_post_as_pickup_rearr]:
                response = self.report.request_json(request + disable_post_as_pickup)
                self.assertFragmentIn(
                    response,
                    {
                        "entity": "deliveryGroup",
                        "delivery": {
                            "options": [
                                {"serviceId": "258"},
                                {"serviceId": "258"},
                                {"serviceId": "258"},
                                {"serviceId": "258"},
                                {"serviceId": "258"},
                                {"serviceId": "258"},
                                {"serviceId": "157"},
                                {"serviceId": "257"},
                            ],
                            "pickupOptions": pickup_options
                            if disable_post_as_pickup
                            else pickup_options + post_options,
                            "postOptions": post_options if disable_post_as_pickup else Absent(),
                            "postStats": {"minDays": 3, "maxDays": 7},
                        },
                    },
                    allow_different_len=False,
                )

        def check_sku_offers(request):
            """Проверяются только pickupOptions, т.к. во всех плейсах, кроме actual_delivery эти опции вычисляются на основе deliveryServiceFlags, но не бакетов"""
            """Курьерка и почта вычисляется как и везде, поэтому не проверяется"""
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "pickupOptions": [
                            {"serviceId": 157},
                            {"serviceId": 257},
                        ],
                    }
                },
                allow_different_len=False,
            )

        """Проверяется для place=actual_delivery, что для курьерки, самовывоза, почты подбираются службы, которые имеют связки с соответствующим warehouse"""
        """    Сначала выводятся все службы, т.к. все связки есть"""
        check_actual_delivery(request_actual_delivery)
        check_sku_offers(request_sku_offers)

        """    Опции служб, для которых удаляются связки, скрыты"""
        self.dynamic.lms -= old_relations
        self.dynamic.lms += new_relations

        disable_post_as_pickup_rearr = '&rearr-factors=market_use_post_as_pickup=0'
        pickup_options = [{"serviceId": 157}]
        post_options = [{"serviceId": 103}]
        for disable_post_as_pickup in ['', disable_post_as_pickup_rearr]:
            response = self.report.request_json(request_actual_delivery + disable_post_as_pickup)
            self.assertFragmentIn(
                response,
                {
                    "entity": "deliveryGroup",
                    "delivery": {
                        "options": [
                            {"serviceId": "157"},
                        ],
                        "pickupOptions": pickup_options if disable_post_as_pickup else pickup_options + post_options,
                        "postOptions": post_options if disable_post_as_pickup else Absent(),
                        "postStats": {"minDays": 3, "maxDays": 4},
                    },
                },
                allow_different_len=False,
            )

        response = self.report.request_json(request_sku_offers)
        self.assertFragmentIn(
            response,
            {
                "delivery": {
                    "pickupOptions": [{"serviceId": 157}],
                }
            },
            allow_different_len=False,
        )

        """    После восстановления исходных связей получаем исходный результат"""
        if check_switched_off_relations:
            """Если связки есть, но они не активны, то их можно активировать через &allow-disabled-lms-relations=1"""
            check_actual_delivery(request_actual_delivery + '&allow-disabled-lms-relations=1')
            check_sku_offers(request_sku_offers + '&allow-disabled-lms-relations=1')

        self.dynamic.lms -= new_relations
        self.dynamic.lms += old_relations

        check_actual_delivery(request_actual_delivery)
        check_sku_offers(request_sku_offers)

    def test_warehouse_and_delivery_service_relations(self):
        relations = [
            warehouse1111_delivery_service258,
            warehouse1111_delivery_service102,
            warehouse1111_delivery_service257,
        ]

        """Проверяется наличие/отсутствие связок warehouse-delivery_service"""
        self.check_warehouse_and_delivery_service_relations(relations, [])

        """Проверяется, что если для региона (в данном случае 213) в связке не задан час перескока, то связка откидывается"""
        no_date_switch_hour_relations = [
            get_warehouse_and_delivery_service(1111, 258, False),
            get_warehouse_and_delivery_service(1111, 102, False),
            get_warehouse_and_delivery_service(1111, 257, False),
        ]
        self.check_warehouse_and_delivery_service_relations(relations, no_date_switch_hour_relations)

        """Проверяется, что если связка выключена, то она не учитывается по умолчанию. Если задан &allow-disabled-lms-relations=1, то связка учитывается не смотря на то, что выключена."""
        switched_off_relations = [
            get_warehouse_and_delivery_service(warehouse_id=1111, service_id=258, is_active=False),
            get_warehouse_and_delivery_service(warehouse_id=1111, service_id=102, is_active=False),
            get_warehouse_and_delivery_service(warehouse_id=1111, service_id=257, is_active=False),
        ]
        self.check_warehouse_and_delivery_service_relations(relations, switched_off_relations, True)

    def test_warehouse_id_for_actual_delivery(self):
        def check_warehouse(offer_id, service_id):
            response = self.report.request_json(
                'place=actual_delivery&offers-list={}:1&rids=213&force-use-delivery-calc=1'.format(offer_id)
            )
            self.assertFragmentIn(
                response,
                {
                    "weight": "5",
                    "dimensions": ["10", "20", "30"],
                    "entity": "deliveryGroup",
                    "delivery": {"options": [{"serviceId": service_id}]},
                },
            )

        """Проверяется, что warehouse_id учитывается при выборе bucket_id от калькулятора доставки"""
        check_warehouse('Sku5Price5-IiLVm1Goleg', '358')
        check_warehouse('Sku1Price5-IiLVm1Goleg', '258')

    def check_shipment_day_shift(self, ware_md5):
        self.dynamic.lms -= [warehouse_1111]

        warehouse_1111_new = DynamicWarehouseInfo(id=1111, home_region=213, holidays_days_set_key=5)
        self.dynamic.lms += [warehouse_1111_new]

        """Проверяется, что при заданном &preferable-courier-delivery-service берутся опции только определенной СД"""
        request = (
            'place=actual_delivery&offers-list={}:1&force-use-delivery-calc=1&rids=213&pickup-options=grouped'.format(
                ware_md5
            )
        )

        disable_post_as_pickup_rearr = '&rearr-factors=market_use_post_as_pickup=0'
        for disable_post_as_pickup in ['', disable_post_as_pickup_rearr]:
            response = self.report.request_json(request + '&debug-all-courier-options=1' + disable_post_as_pickup)
            self.assertFragmentIn(
                response,
                {
                    "entity": "deliveryGroup",
                    "delivery": {
                        "options": [
                            {"serviceId": "258"},
                            {"serviceId": "258"},
                            {"serviceId": "258"},
                            {"serviceId": "258"},
                            {"serviceId": "258"},
                            {"serviceId": "258"},
                            {"serviceId": "157"},
                            {"serviceId": "257"},
                        ],
                        "pickupOptions": NotEmpty(),
                        "postOptions": NotEmpty() if disable_post_as_pickup else Absent(),
                        "postStats": NotEmpty(),
                    },
                },
                allow_different_len=False,
            )

            """Затем проверяется, что с этим флагом выбирается только опции 258 СД"""
            request_service = request + '&preferable-courier-delivery-service=258' + disable_post_as_pickup
            response = self.report.request_json(request_service)
            self.assertFragmentIn(
                response,
                {
                    "entity": "deliveryGroup",
                    "delivery": {
                        "options": [
                            {"serviceId": "258", "dayFrom": 11, "dayTo": 12},
                            {"serviceId": "258", "dayFrom": 12, "dayTo": 12},
                            {"serviceId": "258", "dayFrom": 13, "dayTo": 13},
                            {"serviceId": "258", "dayFrom": 14, "dayTo": 14},
                            {"serviceId": "258", "dayFrom": 15, "dayTo": 15},
                            {"serviceId": "258", "dayFrom": 16, "dayTo": 16},
                        ],
                        "pickupOptions": NotEmpty(),
                        "postOptions": NotEmpty() if disable_post_as_pickup else Absent(),
                        "postStats": NotEmpty(),
                    },
                },
                allow_different_len=False,
            )

            """    Проверяется, что при заданном preferable-courier-delivery-day вычисляется shipmentDay, сдвинутый вперед. При этом не выводятся pickup- и post- опции"""

            def getOption(dayFrom, dayTo, shipmentDay):
                return [{"serviceId": "258", "dayFrom": dayFrom, "dayTo": dayTo, "shipmentDay": shipmentDay}]

            for day, options in [
                ("11", Absent()),
                ("12", getOption(11, 12, 7)),
                (
                    "13",
                    getOption(13, 13, 7),
                ),  # в DirectionCalendar для 258 службы 13 день -- выходной, поэтому shipmentDay остается прежним (7)
                ("14", getOption(12, 14, 8)),
                (
                    "15",
                    getOption(15, 15, 8),
                ),  # в DirectionCalendar для 258 службы 15 день -- выходной , поэтому shipmentDay остается прежним (8)
                ("16", getOption(14, 16, 9)),
            ]:
                response = self.report.request_json(request_service + '&preferable-courier-delivery-day=' + day)
                self.assertFragmentIn(
                    response,
                    {
                        "entity": "deliveryGroup",
                        "delivery": {
                            "options": options,
                            "pickupOptions": Absent(),
                            "postOptions": Absent(),
                            "postStats": Absent(),
                        },
                    },
                )

        """    возвращаем динамик обратно"""
        self.dynamic.lms -= [warehouse_1111_new]

        self.dynamic.lms += [warehouse_1111]

    def test_shipment_day_shift(self):
        """
        Проверяется, что дата отгрузки сдвигается вперед при выборе предпочтительного дня доставки
        На обычном FF офере со склада 1111
        """
        self.check_shipment_day_shift("Sku1Price5-IiLVm1Goleg")

    def test_shipment_day_shift_crossdock(self):
        """
        Проверяется, что дата отгрузки сдвигается вперед при выборе предпочтительного дня доставки
        На crossdock офере со склада 1113
        В результатах те же сроки, что и для FF офера, потому что у склада 1113 нет своих выходных,
        катоф раньше (19:00), сроки handling + transfer + inbound == 0
        """
        self.check_shipment_day_shift("Sku33Price5-iLVm1Goleg")

    @classmethod
    def prepare_test_offer_warehouse(cls):
        cls.index.delivery_calc_feed_info += [
            DeliveryCalcFeedInfo(feed_id=1111, generation_id=1, warehouse_id=1111),
            DeliveryCalcFeedInfo(feed_id=3333, generation_id=1, warehouse_id=3333),
            DeliveryCalcFeedInfo(feed_id=145, generation_id=1, warehouse_id=145),
        ]

    def test_offer_warehouse(self):
        """Проверяется работа параметра wh параметрического оффера."""
        request = 'place=actual_delivery&rids=213&offers-list=DuE098x_rinQLZn3KKrELw:1;w:5;d:10x20x30;p:1;wh:'
        for req_warehouse, warehouse, serviceId in [("1111", 1111, "258"), ("3333", 3333, "358")]:
            response = self.report.request_json(request + req_warehouse + NO_COMBINATOR_FLAG)
            self.assertFragmentIn(
                response,
                {
                    "entity": "deliveryGroup",
                    "delivery": {"options": [{"serviceId": serviceId}]},
                    "fakeOffers": [{"entity": "fakeOffer", "warehouseId": warehouse}],
                },
            )

    def test_date_switch_time(self):
        """Проверяется работа времени перескока (задается часами и минутами)"""
        self.dynamic.lms -= [warehouse1111_delivery_service257]

        def getWarehouse1111AndDeliveryService257Relation(hour, min, packTimeLms):
            return DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=1111,
                delivery_service_id=257,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(
                        date_switch_hour=23,
                        region_to=225,
                        date_switch_time=TimeInfo(hour, min),
                        packaging_time=packTimeLms,
                    )
                ],
            )

        request = (
            'place=actual_delivery&rids=213&offers-list=DuE098x_rinQLZn3KKrELw:1;w:5;d:10x20x30;p:1;wh:1111'
            '&pickup-options=grouped&preferable-courier-delivery-service=257' + NO_COMBINATOR_FLAG
        )

        """    Текущее время 2:58 (rid=213, tz_offset=10680)"""
        for hour, min, shipDay, dayFrom, dayTo, resHour, resMin, pResHour, pResMin, packTimeLMS, packTimeRep in [
            (2, 59, 0, 9, 10, "2", "59", 2, 59, TimeInfo(25, 36), "PT28H35M"),  # время перескока 2:59
            (2, 57, 1, 10, 11, Absent(), Absent(), 24, Absent(), TimeInfo(25, 36), "PT28H33M"),  # время перескока 2:57
            (2, 57, 1, 10, 11, Absent(), Absent(), 24, Absent(), TimeInfo(25), "PT27H57M"),
            (2, 57, 1, 10, 11, Absent(), Absent(), 24, Absent(), TimeInfo(25, 63), "PT29H0M"),
            (2, 57, 1, 10, 11, Absent(), Absent(), 24, Absent(), None, None),
        ]:
            currRel = getWarehouse1111AndDeliveryService257Relation(hour, min, packTimeLMS)
            self.dynamic.lms += [currRel]

            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "entity": "deliveryGroup",
                    "delivery": {
                        "options": [
                            {
                                "shipmentDay": shipDay,
                                "dayFrom": dayFrom,
                                "dayTo": dayTo,
                                "orderBefore": resHour,
                                "orderBeforeMin": resMin,
                                "serviceId": "257",
                                "packagingTime": packTimeRep if packTimeRep is not None else Absent(),
                            }
                        ],
                        "pickupOptions": [  # проверяется, что и для pickup опций минуты перескока выводятся
                            {
                                "serviceId": 257,
                                "orderBefore": pResHour,
                                "orderBeforeMin": pResMin,
                                "packagingTime": packTimeRep if packTimeRep is not None else Absent(),
                            }
                        ],
                    },
                },
            )

            self.dynamic.lms -= [currRel]

        """возвращаем исходный динамик обратно"""
        self.dynamic.lms += [warehouse1111_delivery_service257]

    def test_offers_from_different_warehouses(self):
        """Проверяется, что для офферов из разных warehouse не вычисляются условия доставки"""
        response = self.report.request_json(
            'place=actual_delivery&offers-list=Sku1xPrice5-iLVm1Goleg:1,Sku5Price5-IiLVm1Goleg:1&rids=213&force-use-delivery-calc=1&pickup-options=grouped'
        )
        self.assertFragmentIn(
            response,
            {
                "commonProblems": ["NOT_ALL_THE_SAME_WAREHOUSE_ID"],
                "results": [
                    {
                        "entity": "deliveryGroup",
                        "delivery": {
                            "isAvailable": False,
                            "hasPickup": False,
                            "hasPost": False,
                            "options": Absent(),
                            "pickupOptions": Absent(),
                            "postOptions": Absent(),
                            "postStats": Absent(),
                        },
                    }
                ],
            },
        )
        self.error_log.expect(code=ErrorCodes.ACD_NOT_ALL_THE_SAME_WAREHOUSE_ID)

    def test_offer_without_fulfillment(self):
        """Проверяется, что правильно работает проверка связей в LMS для предложений, исполняемых поставщиком"""
        dropship_offer_id = 'Sku1515-Price5151____g'
        request = 'place=offerinfo&rgb=blue&rids={}&show-urls=encrypted&regset=1'

        for region in [213, 2, 5]:
            response = self.report.request_json(request.format(region) + '&market-sku=1515')
            self.check_with_particular_offers(response, [dropship_offer_id])

            response = self.report.request_json(request.format(region) + '&offerid=' + dropship_offer_id)
            self.check_with_particular_offers(response, [dropship_offer_id])

    @classmethod
    def prepare_for_priority_warehouses(cls):
        """
        Приоритет складов для регионов
        213, warehouses=[1111, 147]
        2, warehouses=[2222, 1111]
        39, warehouses=[147, 1111],
        """

        cls.index.mskus += [
            MarketSku(
                sku=1001,
                hyperid=1001,
                hid=100,
                fesh=1,
                delivery_buckets=[804],
                blue_offers=[
                    BlueOffer(ts=10011, feedid=70),  # fesh=11, warehouse 1111 - priority in moscow
                ],
            ),
            MarketSku(
                sku=1002,
                hyperid=1002,
                hid=100,
                fesh=1,
                delivery_buckets=[804],
                blue_offers=[
                    BlueOffer(ts=10021, feedid=5),  # fesh=10 warehouse 2222 - priority in piter
                ],
            ),
            MarketSku(
                sku=1003,
                hyperid=1003,
                hid=100,
                fesh=1,
                delivery_buckets=[804],
                blue_offers=[
                    BlueOffer(ts=10031, feedid=72),  # fesh=12, warehouse 147 - priority in rostov
                ],
            ),
            # в зависимости от региона эта мскушка будет получать разные buybox офферы
            MarketSku(
                sku=1004,
                hyperid=1004,
                hid=100,
                fesh=1,
                delivery_buckets=[804],
                blue_offers=[
                    BlueOffer(ts=10041, feedid=70),  # fesh=11, warehouse 1111 - priority in moscow
                    BlueOffer(ts=10042, feedid=72),  # fesh=12, warehouse 147 - priority in rostov
                    BlueOffer(ts=10043, feedid=5),  # fesh=10 warehouse 2222 - priority in piter
                ],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10011).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10021).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10031).respond(0.3)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10041).respond(0.2)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10042).respond(0.2)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10043).respond(0.2)

    def test_boost_priority_warehouses(self):
        """Проверяем что на синем маркете более приоритетные склады бустятся на безтекстовой выдаче"""

        def rank(is_local=None, cpm=None):
            """Ранжирование при флаге market_search_boost_priority_warehouses=1 содержит элемент WAREHOUSE_PRIORITY
            поднимающий мскушки с офферами из более приоритетных регионов
            """
            r = [{"name": "HAS_PICTURE"}, {"name": "DELIVERY_TYPE"}, {"name": "IS_MODEL"}] + [
                {"name": "CPM", "value": str(cpm) if cpm is not None else NotEmpty()},
                {"name": "MODEL_TYPE"},
                {"name": "POPULARITY"},
                {"name": "ONSTOCK"},
                {"name": "RANDX"},
            ]
            return r

        def model(id, warehouse, is_local=None, cpm=None):
            """Модель содержит байбоксовый оффер из warehouse и в зависимости от warehouse_priority он бустится или нет"""
            return {
                "entity": "product",
                'id': id,
                'offers': {'items': [{'supplier': {'warehouseId': warehouse}}]},
                'debug': {'rank': rank(is_local, cpm)},
            }

        """

        Приоритет складов для регионов
        213, warehouses=[1111, 147]
        2, warehouses=[2222, 1111]
        39, warehouses=[147, 1111],

        при флаге market_search_boost_priority_warehouses=1
        вверху идут скушки имеющие байбокс-оффер из более локального склада
        байбокс внутри мскушки выбирается из более приоритетного склада (см 1004)
        """

        """
        при флаге market_search_boost_regional_warehouses=0 (по умолчанию)
        вверху идут скушки с более релевантными байбокс-офферами
        байбокс внутри мскушки по-прежнему выбирается из более приоритетного склада (см 1004)
        """

        response = self.report.request_json('place=prime&hid=100&rids=213&local-offers-first=0&rgb=blue&debug=da')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    model(id=1001, warehouse=1111, cpm=50000),
                    model(id=1003, warehouse=147, cpm=30000),
                    model(id=1004, warehouse=1111, cpm=20000),
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json('place=prime&hid=100&rids=2&local-offers-first=0&rgb=blue&debug=da')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    model(id=1001, warehouse=1111, cpm=50000),
                    model(id=1002, warehouse=2222, cpm=40000),
                    model(id=1004, warehouse=2222, cpm=20000),
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=prime&hid=100&rids=39&local-offers-first=0&rgb=blue&debug=da'
            '&rearr-factors=market_search_boost_regional_warehouses=4;market_search_boost_regional_warehouses_in=0,1;market_blue_buybox_by_gmv_ue_with_delivery=0;market_blue_buybox_no_warehouse_priority=0'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    model(id=1001, warehouse=1111, cpm=50000),
                    model(id=1003, warehouse=147, cpm=30000),
                    model(id=1004, warehouse=147, cpm=20000),
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_offerinfo_from_rostov(self):
        """
        Приоритет складов для регионов. offerinfo должен показать офер со склада 147 в регионах 213 и 39, но не показывать в регионе 2, т.к. в этом регионе нет связки с этим складом
        213, warehouses=[1111, 147]
        2, warehouses=[2222, 1111]
        39, warehouses=[147, 1111],
        Если регион не был передан, то фильтрация отключена
        """
        for rid in [213, 39]:
            response = self.report.request_json(
                'place=offerinfo&rgb=blue&regset=2&rids={}&feed_shoffer_id=72-blue.offer.1x.1'.format(rid)
            )
            self.assertFragmentIn(response, {"feed": {"id": "72", "offerId": "blue.offer.1x.1"}})

        # В регионе 2 офферы этого склада скрываются
        response = self.report.request_json(
            'place=offerinfo&rgb=blue&regset=2&rids=2&feed_shoffer_id=72-blue.offer.3.5'
        )
        self.assertFragmentIn(response, {"total": 0})

    def test_sku_offers_from_rostov_without_rids(self):
        """
        Проверяем, что офер не фильтруется по складу, если регион пользователя не был передан.
        Потребовалось, т.к. корзина ходит без региона
        """
        response = self.report.request_json('place=sku_offers&market-sku=1&rgb=blue&offerid=Sku1xPrice5-iLVm1Goleg')
        self.assertFragmentIn(
            response,
            {
                "offers": {
                    "items": [
                        {
                            "wareId": "Sku1xPrice5-iLVm1Goleg",
                            "supplier": {
                                "warehouseId": 147,
                            },
                        }
                    ]
                }
            },
        )

    def test_delivery_conditions_weight_ranges(self):
        weight_ranges_error = 'Several ranges are intersected'

        conditions = [
            DynamicDeliveryConditionsReferenceInfo(
                id=300, weight_range_id=100, delivery_thresholds_id=200
            ),  # (-inf, 7)
            DynamicDeliveryConditionsReferenceInfo(
                id=301, weight_range_id=101, delivery_thresholds_id=200
            ),  # (7, +inf)
            DynamicDeliveryConditionsReferenceInfo(
                id=302, weight_range_id=102, delivery_thresholds_id=200
            ),  # [7, +inf)
            DynamicDeliveryConditionsReferenceInfo(id=303, weight_range_id=103, delivery_thresholds_id=200),  # [5, 5]
            DynamicDeliveryConditionsReferenceInfo(id=304, weight_range_id=104, delivery_thresholds_id=200),  # [5, 7]
            DynamicDeliveryConditionsReferenceInfo(
                id=305, weight_range_id=105, delivery_thresholds_id=200
            ),  # (-inf, 7]
            DynamicDeliveryConditionsReferenceInfo(
                id=306, weight_range_id=106, delivery_thresholds_id=200
            ),  # (-inf, +inf]
        ]

        def getDeliveryConditionsMetaInfo(condition_ids):
            return DynamicDeliveryConditionsMetaInfo(
                id=125,
                weight_bounds=weight_bounds,
                weight_ranges=weight_ranges,
                thresholds=[DynamicDeliveryThresholdsInfo(id=200)],
                conditions=conditions,
                conditions_by_regions=[
                    DynamicDeliveryConditionsByRegionInfo(
                        id=400, region_id=1, delivery_condition_reference_ids=condition_ids
                    )
                ],
                condition_splits=[],
            )

        """Проверяется, что НЕпересекающиеся весовые диапазоны не вызывают ошибку, а пересекающиеся -- вызывают"""
        errors_counter = 0
        for conditions_meta, num_errors in [
            (getDeliveryConditionsMetaInfo([300, 301]), 0),
            (getDeliveryConditionsMetaInfo([300, 302]), 0),
            (getDeliveryConditionsMetaInfo([303, 302]), 0),
            (getDeliveryConditionsMetaInfo([304, 301]), 0),
            (getDeliveryConditionsMetaInfo([304, 302]), 1),
            (getDeliveryConditionsMetaInfo([305, 302]), 1),
            (getDeliveryConditionsMetaInfo([303, 304]), 1),
            (getDeliveryConditionsMetaInfo([304, 302, 302]), 2),
            (getDeliveryConditionsMetaInfo([306, 301]), 1),
            (getDeliveryConditionsMetaInfo([306, 302]), 1),
            (getDeliveryConditionsMetaInfo([306, 303]), 1),
            (getDeliveryConditionsMetaInfo([306, 306]), 1),
        ]:
            self.dynamic.lms += [conditions_meta]

            self.report.request_json('place=sku_offers&market-sku=1&rgb=blue&offerid=Sku1xPrice5-iLVm1Goleg')
            errors_counter += num_errors

            self.dynamic.lms -= [conditions_meta]
        self.error_log.expect(weight_ranges_error).times(errors_counter)
        self.base_logs_storage.error_log.expect(weight_ranges_error).times(errors_counter)

    def test_delivery_conditions(self):
        def getDeliveryConditionsMetaInfo(price, days, is_default):
            return DynamicDeliveryConditionsMetaInfo(
                id=125,
                weight_bounds=weight_bounds,
                weight_ranges=weight_ranges,
                thresholds=[DynamicDeliveryThresholdsInfo(id=200, price=price, days=days)],
                conditions=[
                    DynamicDeliveryConditionsReferenceInfo(id=302, weight_range_id=102, delivery_thresholds_id=200)
                ],  # [7, +inf)
                conditions_by_regions=[
                    DynamicDeliveryConditionsByRegionInfo(id=400, region_id=44, delivery_condition_reference_ids=[302])
                ],
                condition_splits=[DynamicDeliveryConditionsSplitInfo(id=500, delivery_conditions_by_region_ids=[400])],
                default_condition_split_id=(500 if is_default else None),
            )

        '''
        Распределятор работает для всех плейсов (проверяем на actual_delivery, sku_offers, offerinfo).
        При исспользовании rearr-factors=market_options_selector=*** - можно управлять выдачей
        '''
        request_actual_delivery = (
            "base=desktop-prestable.bluemarket.fslb.beru.ru&place=actual_delivery&offers-list={}:1&rids=44"
            + NO_COMBINATOR_FLAG
        )
        request_sku_offers = (
            "base=desktop-prestable.bluemarket.fslb.beru.ru&place=sku_offers&market-sku=170170&rids=44&offerid={}"
        )
        request_offerinfo = "base=desktop-prestable.bluemarket.fslb.beru.ru&place=offerinfo&rids=44&regset=2&offerid={}"

        """Проверяется, что для заданных порогов выбираются правильные опции: по времени, по цене для магазина, по времени и по цене для магазина"""
        """Проверка продуктового рейтинга не проводится в случае заданных thresholds"""
        for waremd5, price_threshold, days_threshold, delivery_service_id, is_default, is_exp in [
            ("Sku1700-Price5151____g", None, 4, "703", False, True),
            ("Sku1700-Price5151____g", None, 6, "703", False, True),
            ("Sku1700-Price5151____g", None, 7, "700", False, True),
            ("Sku1700-Price5151____g", None, 8, "700", False, True),
            ("Sku1700-Price5151___1g", None, 8, "704", False, True),  # same price
            ("Sku1700-Price5151___1g", None, 1, "704", False, True),  # same price
            ("Sku1700-Price5151___2g", 7, None, "700", False, True),
            ("Sku1700-Price5151___2g", 8, None, "700", False, True),
            ("Sku1700-Price5151___2g", 9, None, "700", False, True),
            ("Sku1700-Price5151___2g", 12, None, "703", False, True),
            ("Sku1700-Price5151___3g", 7, None, "706", False, True),  # same time
            ("Sku1700-Price5151___3g", 12, None, "706", False, True),  # same time
            ("Sku1700-Price5151___4g", 8, 9, "704", False, True),  # select quadrant
            ("Sku1700-Price5151___5g", 8, 9, "703", False, True),  # select quadrant
            ("Sku1700-Price5151___6g", 9, 8, "705", False, True),  # select quadrant
            (
                "Sku1700-Price5151___7g",
                9,
                8,
                "701",
                False,
                True,
            ),  # select point with best time when distances from (days_threshold, price_threshold) are equal
            (
                "Sku1700-Price5151___8g",
                7,
                1,
                "704",
                False,
                True,
            ),  # select nearest point to (days_threshold, price_threshold)
            # test default condition selection:
            ("Sku1700-Price5151___2g", None, 8, "700", True, False),  # select default condition
            ("Sku1700-Price5151___2g", None, 8, "703", False, False),  # select best time (no default condition)
        ]:
            conditions_meta = getDeliveryConditionsMetaInfo(
                price=price_threshold, days=days_threshold, is_default=is_default
            )
            self.dynamic.lms += [conditions_meta]

            experiment = '&rearr-factors=market_options_selector=500' if is_exp else ''

            response = self.report.request_json(
                request_actual_delivery.format(waremd5) + experiment + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
            )
            self.assertFragmentIn(
                response, {"entity": "deliveryGroup", "delivery": {"options": [{"serviceId": delivery_service_id}]}}
            )

            response = self.report.request_json(
                request_sku_offers.format(waremd5) + experiment + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
            )
            self.assertFragmentIn(
                response,
                {"items": [{"entity": "offer", "delivery": {"options": [{"serviceId": delivery_service_id}]}}]},
            )

            self.dynamic.lms -= [conditions_meta]

        waremd5 = "Sku1700-Price5151____g"
        delivery_service_id = "700"
        conditions_meta = getDeliveryConditionsMetaInfo(price=None, days=7, is_default=True)
        self.dynamic.lms += [conditions_meta]
        experiment = '&rearr-factors=market_options_selector=500'
        response = self.report.request_json(
            request_actual_delivery.format(waremd5) + experiment + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )
        self.assertFragmentIn(
            response, {"entity": "deliveryGroup", "delivery": {"options": [{"serviceId": delivery_service_id}]}}
        )
        response = self.report.request_json(request_offerinfo.format(waremd5) + USE_DEPRECATED_DIRECT_SHIPPING_FLOW)
        self.assertFragmentIn(
            response, {"entity": "offer", "delivery": {"options": [{"serviceId": delivery_service_id}]}}
        )
        self.dynamic.lms -= [conditions_meta]

    def test_special_logic(self):
        """
        Проверяем, что для некоторых специальных регионов, переданных в market_special_delivery_logic_regions
        исользуется специальная логика выбора опции доставки
        MARKETOUT-29085
            Приоритет СД в данном регионе
            Время доставки
            Цена для магазина
        """

        def getDeliveryConditionsMetaInfo(price, days, is_default):
            return DynamicDeliveryConditionsMetaInfo(
                id=125,
                weight_bounds=weight_bounds,
                weight_ranges=weight_ranges,
                thresholds=[DynamicDeliveryThresholdsInfo(id=200, price=price, days=days)],
                conditions=[
                    DynamicDeliveryConditionsReferenceInfo(id=302, weight_range_id=102, delivery_thresholds_id=200)
                ],  # [7, +inf)
                conditions_by_regions=[
                    DynamicDeliveryConditionsByRegionInfo(id=400, region_id=44, delivery_condition_reference_ids=[302])
                ],
                condition_splits=[DynamicDeliveryConditionsSplitInfo(id=500, delivery_conditions_by_region_ids=[400])],
                default_condition_split_id=(500 if is_default else None),
            )

        for (price_threshold, days_threshold, service) in [
            # для первого квадрата (https://wiki.yandex-team.ru/users/andr-savel/optionsselector/)
            # включается "специальная" логика, переключаемся на лучший приоритет
            (999999, 999999, 708),
            # для остальных логика та же
            (1, 1, 707),
            (1, 999999, 707),
            (999999, 1, 707),
        ]:
            conditions_meta = getDeliveryConditionsMetaInfo(price=price_threshold, days=days_threshold, is_default=True)
            self.dynamic.lms += [conditions_meta]
            waremd5 = "Sku1700-TestDlvry____g"
            request_sku_offers = (
                "base=desktop-prestable.bluemarket.fslb.beru.ru&place=offerinfo&rids=44&regset=2&offerid={}"
            )
            response = self.report.request_json(request_sku_offers.format(waremd5))
            self.assertFragmentIn(
                response,
                {"search": {"results": [{"entity": "offer", "delivery": {"options": [{"serviceId": str(service)}]}}]}},
            )
            self.dynamic.lms -= [conditions_meta]

    def test_shop_price(self):
        """Проверяется вывод цены доставки для магазина"""
        response = self.report.request_json(
            'base=desktop-prestable.bluemarket.fslb.beru.ru&place=actual_delivery&offers-list=Sku1700-Price5151____g:1&rids=44'
            + NO_COMBINATOR_FLAG
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "delivery": {"options": [{"priceForShop": {"currency": "RUR", "value": "11"}, "serviceId": "703"}]},
            },
        )

    @staticmethod
    def get_response_part_for_shipment_days(disable_post_as_pickup, courier, pickup, post):
        pickup_options = [{"serviceId": 258, "shipmentDay": pickup}]
        post_options = [{"serviceId": 258, "shipmentDay": post}]
        return {
            "entity": "deliveryGroup",
            "delivery": {
                "options": [{"serviceId": "258", "shipmentDay": courier}],
                "pickupOptions": pickup_options if disable_post_as_pickup else pickup_options + post_options,
                "postOptions": post_options if disable_post_as_pickup else Absent(),
            },
        }

    def test_capacity(self):
        request = (
            'place=actual_delivery&offers-list=Sku2Price8-IiLVm1Goleg:1&pickup-options=raw'
            + NO_COMBINATOR_FLAG
            + '&rids='
        )

        disable_post_as_pickup_rearr = '&rearr-factors=market_use_post_as_pickup=0'
        for disable_post_as_pickup in ['', disable_post_as_pickup_rearr]:
            """Проверяется, что для региона 100 и для дочернего региона 200 применяется один и тот же календарь (из региона 100)"""
            for rid in ["100", "200"]:
                response = self.report.request_json(request + rid + disable_post_as_pickup)
                self.assertFragmentIn(
                    response, self.get_response_part_for_shipment_days(disable_post_as_pickup, 4, 4, 4)
                )

            """А для региона 300 используется свой календарь. Для DT_PICKUP используется календарь, отличный от DT_COURIER и DT_POST"""
            response = self.report.request_json(request + "300" + disable_post_as_pickup)
            self.assertFragmentIn(response, self.get_response_part_for_shipment_days(disable_post_as_pickup, 7, 12, 7))

    def check_shipment_day_calculation(self, expected_shipment_days, debug_delivery_datetime=None):
        request = (
            "place=actual_delivery&offers-list=Sku2Price8-IiLVm1Goleg:1&pickup-options=raw&rids=300"
            + NO_COMBINATOR_FLAG
        )
        if debug_delivery_datetime:
            request += "&rearr-factors=debug_delivery_datetime=" + urllib.quote(str(debug_delivery_datetime))

        disable_post_as_pickup_rearr = '&rearr-factors=market_use_post_as_pickup=0'
        for disable_post_as_pickup in ['', disable_post_as_pickup_rearr]:
            response = self.report.request_json(request + disable_post_as_pickup)
            self.assertFragmentIn(
                response, self.get_response_part_for_shipment_days(disable_post_as_pickup, *expected_shipment_days)
            )

    def test_debug_delivery_timestamp(self):
        """
        Проверяем, что перенос текущей даты изменяет вычисленную дату отгрузки
        Покрываем дату старта LMS календарей и +30 дней. Эти даты далеко от всех дейофов, поэтому срок отгрузки 0/1 день
        """

        self.check_shipment_day_calculation((7, 12, 7))  # current day = 1985-06-24T09:30+03:00
        self.check_shipment_day_calculation((6, 11, 6), "1985-06-25T00:30+03:00")  # next day before cut-off
        self.check_shipment_day_calculation((6, 11, 6), "1985-06-24T21:30Z")  # next day before cut-off in UTC timezone
        self.check_shipment_day_calculation((0, 0, 0), "1985-06-17T00:30+03:00")  # LMS start day before cut-off
        self.check_shipment_day_calculation((1, 1, 1), "1985-06-17T01:30+03:00")  # LMS start day after cut-off
        self.check_shipment_day_calculation(
            (0, 0, 0), "1985-07-16T00:30+03:00"
        )  # LMS start day + 19 days before cut-off
        self.check_shipment_day_calculation(
            (1, 1, 1), "1985-07-16T01:30+03:00"
        )  # LMS start day + 19 days after cut-off
        self.check_shipment_day_calculation(
            (0, 0, 0), 487805400
        )  # 1985-06-17T00:30+03:00 before cut-off as a timestamp

    def check_filtered_buckets_trace(self, request, regset):
        relations = [warehouse1111_delivery_service258, warehouse1111_delivery_service157]
        self.dynamic.lms -= relations

        self.dynamic.lms += [
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=1111,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=123)],
            )
        ]

        if regset is not None:
            request += '&regset={}'.format(regset)
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                "traces": [
                    {
                        "document": "Sku2Price5-IiLVm1Goleg",
                        "type": "OFFER_BY_WARE_MD5",
                        "warehouse_id": 1111,
                        "valid_buckets": {
                            "COURIER.MMAP": Absent(),
                            "PICKUP.MMAP": [
                                {
                                    "bucket_id (report)": 903,
                                    "bucket_id (delivery calc)": 8,
                                    "delivery_service_id": 257,
                                }
                            ],
                            "POST.MMAP": [
                                {
                                    "bucket_id (report)": 201,
                                    "bucket_id (delivery calc)": 51,
                                    "delivery_service_id": 102,
                                },
                                {
                                    "bucket_id (report)": 202,
                                    "bucket_id (delivery calc)": 52,
                                    "delivery_service_id": 103,
                                },
                            ],
                        },
                        "filtered_buckets": {
                            "COURIER.MMAP": [
                                {
                                    "bucket_id (report)": 804,
                                    "bucket_id (delivery calc)": 4,
                                    "delivery_service_id": 258,
                                    "reasons": ["NO_WAREHOUSE_AND_DELIVERY_SERVICE_RELATION"],
                                },
                                {
                                    "bucket_id (report)": 810,
                                    "bucket_id (delivery calc)": 8100,
                                    "delivery_service_id": 258,
                                    "reasons": ["NO_OPTION_GROUP_FOR_USER_REGION"]
                                    if regset == 1
                                    else [
                                        "NO_WAREHOUSE_AND_DELIVERY_SERVICE_RELATION",
                                        "NO_OPTION_GROUP_FOR_USER_REGION",
                                    ],
                                },
                                {
                                    "bucket_id (report)": 811,
                                    "bucket_id (delivery calc)": 8101,
                                    "delivery_service_id": 260,
                                    "reasons": ["NO_OPTION_GROUP_FOR_USER_REGION"],
                                },
                            ],
                            "PICKUP.MMAP": [
                                {
                                    "bucket_id (report)": 901,
                                    "bucket_id (delivery calc)": 6,
                                    "delivery_service_id": 157,
                                    "reasons": [
                                        "NO_DATE_SWITCH_TIME_FOR_WAREHOUSE_AND_DELIVERY_SERVICE_RELATION_IN_USER_REGION"
                                    ],
                                },
                                {
                                    "bucket_id (report)": 902,
                                    "bucket_id (delivery calc)": 7,
                                    "delivery_service_id": 158,
                                    "reasons": ["NO_ALLOWED_OUTLET_TYPES_IN_USER_REGION"],
                                },
                                {
                                    "bucket_id (report)": 9001,
                                    "bucket_id (delivery calc)": 90010,
                                    "delivery_service_id": 258,
                                    "reasons": ["NO_WAREHOUSE_AND_DELIVERY_SERVICE_RELATION"],
                                },
                            ],
                            "POST.MMAP": [
                                {
                                    "bucket_id (report)": 9002,
                                    "bucket_id (delivery calc)": 90020,
                                    "delivery_service_id": 258,
                                    "reasons": ["NO_WAREHOUSE_AND_DELIVERY_SERVICE_RELATION"],
                                }
                            ],
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

        self.dynamic.lms -= [warehouse1111_delivery_service157]

        self.dynamic.lms += relations

    def check_filtered_buckets_trace_no_delivery(self, request, regset):
        relations = [
            warehouse1111_delivery_service258,
            warehouse1111_delivery_service157,
            warehouse1111_delivery_service102,
            warehouse1111_delivery_service103,
            warehouse1111_delivery_service257,
            warehouse1111_delivery_service158,
        ]
        self.dynamic.lms -= relations

        if regset is not None:
            request += '&regset={}'.format(regset)
        response = self.report.request_json(request)

        accept_doc_filtered_reason = 'DELIVERY_BLUE' if regset != 1 else Absent()
        passed_accept_doc = regset == 1

        self.assertFragmentIn(
            response,
            {
                "traces": [
                    {
                        "document": "Sku2Price5-IiLVm1Goleg",
                        "type": "OFFER_BY_WARE_MD5",
                        "in_accept_doc": True,
                        "passed_accept_doc": passed_accept_doc,
                        "accept_doc_filtered_reason": accept_doc_filtered_reason,
                        "warehouse_id": 1111,
                        "valid_buckets": {
                            "COURIER.MMAP": Absent(),
                            "PICKUP.MMAP": Absent(),
                            "POST.MMAP": Absent(),
                        },
                        "filtered_buckets": {
                            "COURIER.MMAP": [
                                {
                                    "bucket_id (report)": 804,
                                    "bucket_id (delivery calc)": 4,
                                    "delivery_service_id": 258,
                                    "reasons": ["NO_WAREHOUSE_AND_DELIVERY_SERVICE_RELATION"],
                                },
                                {
                                    "bucket_id (report)": 810,
                                    "bucket_id (delivery calc)": 8100,
                                    "delivery_service_id": 258,
                                    "reasons": ["NO_OPTION_GROUP_FOR_USER_REGION"]
                                    if regset == 1
                                    else ["NO_WAREHOUSE_AND_DELIVERY_SERVICE_RELATION"],
                                },
                                {
                                    "bucket_id (report)": 811,
                                    "bucket_id (delivery calc)": 8101,
                                    "delivery_service_id": 260,
                                    "reasons": ["NO_OPTION_GROUP_FOR_USER_REGION"],
                                },
                            ],
                            "PICKUP.MMAP": [
                                {
                                    "bucket_id (report)": 901,
                                    "bucket_id (delivery calc)": 6,
                                    "delivery_service_id": 157,
                                    "reasons": ["NO_WAREHOUSE_AND_DELIVERY_SERVICE_RELATION"],
                                },
                                {
                                    "bucket_id (report)": 902,
                                    "bucket_id (delivery calc)": 7,
                                    "delivery_service_id": 158,
                                    "reasons": ["NO_WAREHOUSE_AND_DELIVERY_SERVICE_RELATION"],
                                },
                                {
                                    "bucket_id (report)": 903,
                                    "bucket_id (delivery calc)": 8,
                                    "delivery_service_id": 257,
                                    "reasons": ["NO_WAREHOUSE_AND_DELIVERY_SERVICE_RELATION"],
                                },
                                {
                                    "bucket_id (report)": 9001,
                                    "bucket_id (delivery calc)": 90010,
                                    "delivery_service_id": 258,
                                    "reasons": ["NO_WAREHOUSE_AND_DELIVERY_SERVICE_RELATION"],
                                },
                            ],
                            "POST.MMAP": [
                                {
                                    "bucket_id (report)": 201,
                                    "bucket_id (delivery calc)": 51,
                                    "delivery_service_id": 102,
                                    "reasons": ["NO_WAREHOUSE_AND_DELIVERY_SERVICE_RELATION"],
                                },
                                {
                                    "bucket_id (report)": 202,
                                    "bucket_id (delivery calc)": 52,
                                    "delivery_service_id": 103,
                                    "reasons": ["NO_WAREHOUSE_AND_DELIVERY_SERVICE_RELATION"],
                                },
                                {
                                    "bucket_id (report)": 9002,
                                    "bucket_id (delivery calc)": 90020,
                                    "delivery_service_id": 258,
                                    "reasons": ["NO_WAREHOUSE_AND_DELIVERY_SERVICE_RELATION"],
                                },
                            ],
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

        self.dynamic.lms += relations

    def test_filtered_buckets_trace(self):
        """Проверяется:
        -- что valid_buckets это результат all_buckets - filtered_buckets
        -- причины фильтрации"""

        request = 'place=prime&offerid=Sku2Price5-IiLVm1Goleg&rids=213&rearr-factors=market_documents_search_trace=Sku2Price5-IiLVm1Goleg&debug=1&rgb=blue&pp=18&rearr-factors=market_nordstream=0'
        for regset in (None, 1, 2):
            self.check_filtered_buckets_trace(request, regset)
            self.check_filtered_buckets_trace_no_delivery(request, regset)

    def test_delivery_shipment_holidays(self):
        """Проверяется:
        -- как работают выходные для курьерской службы"""

        request = (
            'place=actual_delivery&offers-list=Sku44WithHolidayDeli_g:1&rids=213&rgb=blue&pp=18&rearr-factors=market_blue_delivery_date_without_holiday={}'
            + NO_COMBINATOR_FLAG
        )

        response = self.report.request_json(request.format(1))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "dayFrom": 19,
                                    "dayTo": 20,
                                }
                            ],
                        },
                    }
                ],
            },
            allow_different_len=False,
        )

        response = self.report.request_json(request.format(0))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "dayFrom": 20,
                                    "dayTo": 21,
                                }
                            ],
                        },
                    }
                ],
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
