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
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicTimeIntervalsSet,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    GpsCoord,
    HandlingRegionToRegionInfo,
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
from core.matcher import Absent

USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"

TZ_OFFSET_12PM = 43200  # current user and shop time is 12:00 (12pm)

RID_COUNTRY = 100
OUTLET_START = 2000
POST_OUTLET_START = 3000

ALL_DAYS = list(range(30))
TODAY_ONLY = [0]
TOMORROW_ONLY = [1]
TODAY_AND_TOMORROW = [0, 1]

KEY_ALL_DAYS = 0
KEY_NO_DAYS = 1
KEY_TODAY_ONLY = 2
KEY_TOMORROW_ONLY = 3
KEY_TODAY_AND_TOMORROW = 4
KEY_ALL_INTERVALS = 0

MORNING_INTERVAL = (10, 14)
EVENING_INTERVAL = (18, 22)

ALL_DAY = [MORNING_INTERVAL, EVENING_INTERVAL]
EVENING = [EVENING_INTERVAL]

SDD = (0, 0)
NDD = (1, 1)

LEGACY_PACKAGING_HOURS = 1

NORMAL_TEST_CASES = [
    # cut-off h, handling h @ WH, handling h @ DS, tariff, deadline h, shipment d, delivery from d, intervals,   RID
    # Regression for legacy packaging time
    (13, None, None, SDD, 14, 0, 0, EVENING, 1001),
    (11, None, None, SDD, 12, 1, 1, EVENING, 1002),
    (13, None, None, NDD, 14, 0, 1, ALL_DAY, 1003),
    (11, None, None, NDD, 12, 1, 2, ALL_DAY, 1004),
    # Handling @ both WH and DS is in time for today shipping and dispatch, but intervals are good or missing
    (13, 2, 1, SDD, 14, 0, 0, EVENING, 1005),
    (13, 2, 6, SDD, 14, 0, 1, ALL_DAY, 1006),
    (13, 4, 5, SDD, 14, 0, 1, ALL_DAY, 1007),
    # Same as above, but cut-off is missing. Expected +1 day to shipment and delivery
    (11, 2, 1, SDD, 12, 1, 1, EVENING, 1008),
    (11, 2, 6, SDD, 12, 1, 2, ALL_DAY, 1009),
    (11, 4, 5, SDD, 12, 1, 2, ALL_DAY, 1010),
    # Handling @ WH is in time for today shipping, but handling @ DS postpones for tomorrow dispatch
    # Interval filtering doesn't work (it's NDD case: SDD tariff, but handling @ DS > 24h)
    (13, 8, 5, SDD, 14, 0, 1, ALL_DAY, 1011),
    (13, 8, 16, SDD, 14, 0, 1, ALL_DAY, 1012),
    (13, 8, 5, NDD, 14, 0, 2, ALL_DAY, 1013),
    (13, 8, 16, NDD, 14, 0, 2, ALL_DAY, 1014),
    # Same as above, but cut-off is missing. Expected +1 day to shipment and delivery
    # Interval filtering doesn't work (it's NDD case: SDD tariff, but handling @ DS > 24h)
    (11, 8, 5, SDD, 12, 1, 2, ALL_DAY, 1015),
    (11, 8, 16, SDD, 12, 1, 2, ALL_DAY, 1016),
    (11, 8, 5, NDD, 12, 1, 3, ALL_DAY, 1017),
    (11, 8, 16, NDD, 12, 1, 3, ALL_DAY, 1018),
    # Handling @ WH is in time for tomorrow shipping, and handling @ DS is in time for tomorrow dispatch
    (13, 18, 2, SDD, 14, 1, 1, ALL_DAY, 1023),
    (13, 18, 5, SDD, 14, 1, 1, EVENING, 1024),
    (13, 18, 2, NDD, 14, 1, 2, ALL_DAY, 1025),
    (13, 18, 5, NDD, 14, 1, 2, ALL_DAY, 1026),
    # Same as above, but cut-off is missing. Expected +1 day to shipment and delivery
    (11, 18, 2, SDD, 12, 2, 2, ALL_DAY, 1027),
    (11, 18, 6, SDD, 12, 2, 2, EVENING, 1028),
    (11, 18, 2, NDD, 12, 2, 3, ALL_DAY, 1029),
    (11, 18, 6, NDD, 12, 2, 3, ALL_DAY, 1030),
    # Handling @ WH is in time for tomorrow shipping, but handling @ DS postpones for after-tomorrow dispatch
    # Interval filtering doesn't work (it's NDD case: SDD tariff, but handling @ DS > 24h)
    (13, 18, 26, SDD, 14, 1, 2, ALL_DAY, 1031),
    (13, 18, 30, SDD, 14, 1, 2, ALL_DAY, 1032),
    (13, 18, 26, NDD, 14, 1, 3, ALL_DAY, 1033),
    (13, 18, 30, NDD, 14, 1, 3, ALL_DAY, 1034),
    # Unusually long handling time
    (13, 540, 2, NDD, 14, 23, 24, ALL_DAY, 1035),
]

WAREHOUSE_HOLIDAY_TODAY_TEST_CASES = [
    # cut-off h, handling h @ WH, handling h @ DS, tariff, deadline h, shipment d, delivery from d, intervals,   RID),
    (13, None, 0, NDD, 14, 1, 2, ALL_DAY, 1042),
    (13, 24, 0, NDD, 14, 2, 3, ALL_DAY, 1043),
    (11, None, 0, NDD, 12, 1, 2, ALL_DAY, 1044),
    (11, 24, 0, NDD, 12, 2, 3, ALL_DAY, 1045),
]

WAREHOUSE_HOLIDAY_TOMORROW_TEST_CASES = [
    # cut-off h, handling h @ WH, handling h @ DS, tariff, deadline h, shipment d, delivery from d, intervals,   RID),
    (13, None, 0, NDD, 14, 0, 1, ALL_DAY, 1046),
    (13, 24, 0, NDD, 14, 2, 3, ALL_DAY, 1047),
    (11, None, 0, NDD, 12, 2, 3, ALL_DAY, 1048),
    (11, 24, 0, NDD, 12, 3, 4, ALL_DAY, 1049),
]

WAREHOUSE_HOLIDAY_TODAY_AND_TOMORROW_TEST_CASES = [
    # cut-off h, handling h @ WH, handling h @ DS, tariff, deadline h, shipment d, delivery from d, intervals,   RID),
    (13, None, 0, NDD, 14, 2, 3, ALL_DAY, 1050),
    (13, 24, 0, NDD, 14, 3, 4, ALL_DAY, 1051),
    (11, None, 0, NDD, 12, 2, 3, ALL_DAY, 1052),
    (11, 24, 0, NDD, 12, 3, 4, ALL_DAY, 1053),
]

TEST_CASES = (
    NORMAL_TEST_CASES
    + WAREHOUSE_HOLIDAY_TODAY_TEST_CASES
    + WAREHOUSE_HOLIDAY_TOMORROW_TEST_CASES
    + WAREHOUSE_HOLIDAY_TODAY_AND_TOMORROW_TEST_CASES
)

(
    CUTOFF_HOUR,
    HANDLING_WH_HOURS,
    HANDLING_DS_HOURS,
    TARIFF,
    DEADLINE_HOUR,
    SHIPMENT_DAY,
    DELIVERY_FROM_DAY,
    INTERVALS,
    RID,
) = list(range(9))


def create_lms_environment(warehouse_holidays_key=KEY_NO_DAYS):
    return [
        DynamicDaysSet(
            key=KEY_ALL_DAYS,
            days=ALL_DAYS,
        ),
        DynamicDaysSet(
            key=KEY_NO_DAYS,
            days=[],
        ),
        DynamicDaysSet(
            key=KEY_TODAY_ONLY,
            days=TODAY_ONLY,
        ),
        DynamicDaysSet(
            key=KEY_TOMORROW_ONLY,
            days=TOMORROW_ONLY,
        ),
        DynamicDaysSet(
            key=KEY_TODAY_AND_TOMORROW,
            days=TODAY_AND_TOMORROW,
        ),
        DynamicTimeIntervalsSet(
            key=KEY_ALL_INTERVALS,
            intervals=[
                TimeIntervalInfo(
                    TimeInfo(interval[0], 0),
                    TimeInfo(interval[1], 0),
                )
                for interval in ALL_DAY
            ],
        ),
        DynamicWarehouseInfo(
            id=145,
            home_region=RID_COUNTRY,
            holidays_days_set_key=warehouse_holidays_key,
            handling_info=[
                HandlingRegionToRegionInfo(
                    region_from=RID_COUNTRY,
                    region_to=test_case[RID],
                    handling_time=TimeInfo(test_case[HANDLING_WH_HOURS], 0) if test_case[HANDLING_WH_HOURS] else None,
                )
                for test_case in TEST_CASES
            ],
        ),
        DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
        DynamicDeliveryServiceInfo(
            id=157,
            region_to_region_info=[
                DeliveryServiceRegionToRegionInfo(
                    region_from=RID_COUNTRY,
                    region_to=RID_COUNTRY,
                    days_key=KEY_NO_DAYS,  # holidays
                ),
            ],
            time_intervals=[
                TimeIntervalsForRegion(
                    region=RID_COUNTRY,
                    intervals=[
                        TimeIntervalsForDaysInfo(
                            intervals_key=KEY_ALL_INTERVALS,
                            days_key=KEY_ALL_DAYS,
                        )
                    ],
                ),
            ],
            handling_info=[
                HandlingRegionToRegionInfo(
                    region_from=RID_COUNTRY,
                    region_to=test_case[RID],
                    handling_time=TimeInfo(test_case[HANDLING_DS_HOURS], 0) if test_case[HANDLING_DS_HOURS] else None,
                )
                for test_case in TEST_CASES
            ],
        ),
        DynamicDeliveryServiceInfo(
            id=27,
            region_to_region_info=[
                DeliveryServiceRegionToRegionInfo(region_from=RID_COUNTRY, region_to=RID_COUNTRY, days_key=KEY_NO_DAYS),
            ],
            time_intervals=[
                TimeIntervalsForRegion(
                    region=RID_COUNTRY,
                    intervals=[TimeIntervalsForDaysInfo(intervals_key=KEY_ALL_INTERVALS, days_key=KEY_ALL_DAYS)],
                ),
            ],
            handling_info=[
                HandlingRegionToRegionInfo(
                    region_from=RID_COUNTRY, region_to=test_case[RID], handling_time=TimeInfo(48)
                )
                for test_case in TEST_CASES
            ],
        ),
    ] + [
        DynamicWarehouseAndDeliveryServiceInfo(
            warehouse_id=145,
            delivery_service_id=ds_id,
            operation_time=0,
            date_switch_time_infos=[
                DateSwitchTimeAndRegionInfo(
                    date_switch_hour=test_case[CUTOFF_HOUR],
                    region_to=test_case[RID],
                    date_switch_time=TimeInfo(test_case[CUTOFF_HOUR], 0),
                    packaging_time=TimeInfo(LEGACY_PACKAGING_HOURS, 0),
                )
                for test_case in TEST_CASES
            ],
        )
        for ds_id in (27, 157)
    ]


REQUESTS = [
    "place=actual_delivery&offers-list=Sku1Price5-IiLVm1Goleg:1",
    "place=prime&offerid=Sku1Price5-IiLVm1Goleg",
    "place=offerinfo&offerid=Sku1Price5-IiLVm1Goleg",
    "place=sku_offers&market-sku=1",
]


class T(TestCase):
    @classmethod
    def prepare(cls):
        """ """

        cls.settings.lms_autogenerate = False

        cls.index.regiontree += [
            Region(
                rid=RID_COUNTRY,
                tz_offset=TZ_OFFSET_12PM,
                children=[
                    Region(
                        rid=test_case[RID],
                        tz_offset=TZ_OFFSET_12PM,
                    )
                    for test_case in TEST_CASES
                ],
            ),
        ]

        cls.index.shops += [
            Shop(
                fesh=12,
                datafeed_id=12,
                priority_region=RID_COUNTRY,
                name='virtual shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                delivery_service_outlets=[(OUTLET_START + test_case[RID]) for test_case in TEST_CASES],
            ),
            Shop(
                fesh=22,
                datafeed_id=22,
                priority_region=RID_COUNTRY,
                name='blue shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue='REAL',
                warehouse_id=145,
            ),
        ]

        cls.index.outlets += [
            # Pickup outlets below
            Outlet(
                point_id=OUTLET_START + test_case[RID],
                delivery_service_id=157,
                region=test_case[RID],
                working_days=ALL_DAYS,
                delivery_option=OutletDeliveryOption(
                    shipper_id=157, day_from=test_case[TARIFF][0], day_to=test_case[TARIFF][1], price=400
                ),
                point_type=Outlet.FOR_POST_TERM,
                gps_coord=GpsCoord(37.7, 55.7),
            )
            for test_case in TEST_CASES
        ]

        cls.index.outlets += [
            # Post outlet
            Outlet(
                point_id=POST_OUTLET_START + test_case[RID],
                delivery_service_id=27,
                region=test_case[RID],
                working_days=ALL_DAYS,
                delivery_option=OutletDeliveryOption(
                    shipper_id=27, day_from=test_case[TARIFF][0], day_to=test_case[TARIFF][1], price=200
                ),
                point_type=Outlet.FOR_POST,
                gps_coord=GpsCoord(37.75, 55.65),
            )
            for test_case in TEST_CASES
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=801,
                dc_bucket_id=801,
                fesh=12,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=test_case[RID],
                        options=[
                            DeliveryOption(
                                price=15,
                                day_from=test_case[TARIFF][0],
                                day_to=test_case[TARIFF][1],
                                shop_delivery_price=10,
                            ),
                        ],
                    )
                    for test_case in TEST_CASES
                ],
            )
        ]

        cls.index.pickup_buckets += [
            # Pickup bucket
            PickupBucket(
                bucket_id=901,
                dc_bucket_id=901,
                fesh=12,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                options=[
                    PickupOption(
                        outlet_id=OUTLET_START + test_case[RID],
                        day_from=test_case[TARIFF][0],
                        day_to=test_case[TARIFF][1],
                    )
                    for test_case in TEST_CASES
                    if test_case[TARIFF] == NDD
                ],
            ),
            # Post bucket
            PickupBucket(
                bucket_id=1001,
                dc_bucket_id=1001,
                fesh=12,
                carriers=[27],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                options=[
                    PickupOption(
                        outlet_id=POST_OUTLET_START + test_case[RID],
                        day_from=test_case[TARIFF][0],
                        day_to=test_case[TARIFF][1],
                    )
                    for test_case in TEST_CASES
                    if test_case[TARIFF] == NDD
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1",
                sku=1,
                waremd5="Sku1-wdDXWsIiLVm1goleg",
                hyperid=100,
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=22,
                        offerid="blue.offer.1.1",
                        waremd5="Sku1Price5-IiLVm1Goleg",
                        weight=5,
                        dimensions=OfferDimensions(
                            length=20,
                            width=30,
                            height=10,
                        ),
                    ),
                ],
                delivery_buckets=[801],
                pickup_buckets=[901],
                post_buckets=[1001],
                post_term_delivery=True,
            ),
        ]

        cls.dynamic.lms = create_lms_environment()

    def check_lms_handling_time(
        self,
        request,
        test_case,
        with_preferable_courier_delivery,
    ):
        # difference between handling time of post vs pickup (in days)
        def post_handling_delay():
            pickup_handling_hours = test_case[CUTOFF_HOUR] + (test_case[HANDLING_WH_HOURS] or 0)
            post_handling_hours = pickup_handling_hours + 48  # handling is 2 days (fixed) for Post
            pickup_handling_hours += test_case[HANDLING_DS_HOURS] or 0
            return (post_handling_hours / 24) - (pickup_handling_hours / 24)

        tariff = test_case[TARIFF]
        deadline_hour = test_case[DEADLINE_HOUR]
        shipment_day = test_case[SHIPMENT_DAY]
        delivery_day_from = test_case[DELIVERY_FROM_DAY]
        intervals = test_case[INTERVALS]
        rid = test_case[RID]

        delivery_day_to = delivery_day_from + tariff[1] - tariff[0]
        actual_delivery = "actual_delivery" in request
        courier_options_for_5_days = actual_delivery and not with_preferable_courier_delivery
        pickup_options = tariff == NDD and not (actual_delivery and with_preferable_courier_delivery)
        if pickup_options:
            post_handling_delay_days = post_handling_delay()
            post_stats = {
                "minDays": delivery_day_from + post_handling_delay_days,
                "maxDays": delivery_day_to + post_handling_delay_days,
            }
        else:
            post_stats = Absent()

        request += "&pickup-options=grouped&pickup-options-extended-grouping=1&regset=2&rgb=blue&rids={}".format(rid)
        if with_preferable_courier_delivery:
            request += "&preferable-courier-delivery-day={}".format(delivery_day_from)
        if actual_delivery:
            request += "&combinator=0"
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                'delivery': {
                    'options': [
                        {
                            'shipmentDay': shipment_day,
                            'dayFrom': delivery_day_from,
                            'dayTo': delivery_day_to,
                            'packagingTime': "PT{}H0M".format(deadline_hour) if actual_delivery else Absent(),
                            'timeIntervals': [
                                {
                                    'from': "{:02d}:00".format(interval[0]),
                                    'to': "{:02d}:00".format(interval[1]),
                                }
                                for interval in intervals
                            ]
                            if actual_delivery
                            else Absent(),
                        },
                    ]
                    + ([{}] * 4 if courier_options_for_5_days else []),
                    'pickupOptions': [
                        {
                            'shipmentDay': shipment_day,
                            'dayFrom': delivery_day_from,
                            'dayTo': delivery_day_to,
                        },
                    ]
                    if pickup_options
                    else Absent(),
                    'postStats': post_stats,
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def run_test_cases(
        self,
        test_cases,
        with_preferable_courier_delivery=False,
    ):
        for request in REQUESTS:
            request += '&rearr-factors=market_nordstream_relevance=0'  # так как тест на лмс
            request += USE_DEPRECATED_DIRECT_SHIPPING_FLOW
            for test_case in test_cases:
                self.check_lms_handling_time(
                    request,
                    test_case,
                    with_preferable_courier_delivery,
                )

    def test_lms_handling_time(self):
        """
        Testing all normal test cases for handling time
        """

        self.dynamic.lms = create_lms_environment()
        self.run_test_cases(NORMAL_TEST_CASES)

    def test_lms_handling_time_with_warehouse_holidays(self):
        """
        Testing test cases with warehouse holidays on different days + handling time
        """

        self.dynamic.lms = create_lms_environment(warehouse_holidays_key=KEY_TODAY_ONLY)
        self.run_test_cases(WAREHOUSE_HOLIDAY_TODAY_TEST_CASES)
        self.run_test_cases(WAREHOUSE_HOLIDAY_TODAY_TEST_CASES, with_preferable_courier_delivery=True)

        self.dynamic.lms = create_lms_environment(warehouse_holidays_key=KEY_TOMORROW_ONLY)
        self.run_test_cases(WAREHOUSE_HOLIDAY_TOMORROW_TEST_CASES)
        self.run_test_cases(WAREHOUSE_HOLIDAY_TOMORROW_TEST_CASES, with_preferable_courier_delivery=True)

        self.dynamic.lms = create_lms_environment(warehouse_holidays_key=KEY_TODAY_AND_TOMORROW)
        self.run_test_cases(WAREHOUSE_HOLIDAY_TODAY_AND_TOMORROW_TEST_CASES)
        self.run_test_cases(WAREHOUSE_HOLIDAY_TODAY_AND_TOMORROW_TEST_CASES, with_preferable_courier_delivery=True)


if __name__ == "__main__":
    main()
