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
    HandlingRegionToRegionInfo,
    MarketSku,
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
import copy


INITIAL_HOUR = 12  # Because we can only emulate current time change via region's timezone,
# we will assume current time is 12:00 for interval definitions.
# Time then can be adjusted around 12:00 from 07:00 till 17:00 (to avoid crossing the midnight)

ALL_INTERVALS = [
    (7, 0, 11, 0),  # interval from 07:00 to 11:00
    (11, 0, 14, 0),  # interval from 11:00 to 14:00
    (15, 0, 17, 0),  # interval from 15:00 to 17:00
    (10, 0, 16, 0),  # interval from 10:00 to 16:00
    (8, 0, 10, 0),  # interval from 08:00 to 10:00
    (12, 0, 15, 0),  # interval from 12:00 to 15:00
]

I_07_11 = 0
I_11_14 = 1
I_15_17 = 2
I_10_16 = 3
I_08_10 = 4
I_12_15 = 5

NORMAL_INTERVALS = [ALL_INTERVALS[i] for i in [I_07_11, I_11_14, I_15_17, I_10_16]]
OTHER_INTERVALS = [ALL_INTERVALS[i] for i in [I_08_10, I_12_15]]
NO_INTERVALS = []
INTERVALS_NOT_SET = None

ALL_DAYS = 0
NO_DAYS = 10
TODAY_ONLY = 11
TOMORROW_ONLY = 12

BASE_SERVICE_ID = 157
SDD_SERVICE = 0
NDD_SERVICE = 1
NDD_SERVICE_WITH_HANDLING_TIME = 2
NDD_SERVICE_WITH_OPERATION_TIME = 3
DELIVERY_SERVICES = [
    SDD_SERVICE,
    NDD_SERVICE,
    NDD_SERVICE_WITH_HANDLING_TIME,
    NDD_SERVICE_WITH_OPERATION_TIME,
]

BASE_BUCKET_ID = 801
DAY_FROM_DAY_TO_FOR_SERVICE_BUCKET = [
    (0, 0),
    (1, 1),
    (0, 0),
    (0, 0),
]
HANDLING_INFO_FOR_SERVICE = [
    None,
    None,
    [
        HandlingRegionToRegionInfo(
            region_from=212,
            region_to=212,
            handling_time=TimeInfo(24, 0),
        ),
    ],
    None,
]
OPERATION_TIME_FOR_SERVICE = [
    None,
    None,
    None,
    1,
]

constant_lms_environment = [
    DynamicDaysSet(
        key=ALL_DAYS,
        days=[0, 1, 2, 3, 4, 5, 6, 7],  # all days for a week ahead
    ),
    DynamicDaysSet(
        key=NO_DAYS,
        days=[],  # no days
    ),
    DynamicDaysSet(
        key=TODAY_ONLY,
        days=[0],  # today only
    ),
    DynamicDaysSet(
        key=TOMORROW_ONLY,
        days=[1],  # tomorrow only
    ),
]


def make_dynamic_lms_environment(
    current_hour,
    warehouse_holidays,
    delivery_service_holidays,
    cut_offs,
    delivery_intervals_for_days,
):
    """ """

    hours_diff = INITIAL_HOUR - current_hour

    dynamic_lms_environment = [
        DynamicWarehouseInfo(
            id=112,
            home_region=212,
            holidays_days_set_key=warehouse_holidays,
        ),
        DynamicWarehouseToWarehouseInfo(warehouse_from=112, warehouse_to=112),
    ]

    dynamic_lms_environment += [
        DynamicDeliveryServiceInfo(
            id=(BASE_SERVICE_ID + delivery_service),
            region_to_region_info=[
                DeliveryServiceRegionToRegionInfo(
                    region_from=212,
                    region_to=212,
                    days_key=delivery_service_holidays,  # holidays
                ),
            ],
            time_intervals=[
                TimeIntervalsForRegion(
                    region=212,
                    intervals=[
                        TimeIntervalsForDaysInfo(
                            intervals_key=delivery_interval_for_day[1], days_key=delivery_interval_for_day[1]
                        )
                        for delivery_interval_for_day in delivery_intervals_for_days
                    ],
                ),
            ]
            if delivery_intervals_for_days
            else None,
            handling_info=HANDLING_INFO_FOR_SERVICE[delivery_service],
        )
        for delivery_service in DELIVERY_SERVICES
    ]

    dynamic_lms_environment += [
        DynamicWarehouseAndDeliveryServiceInfo(
            warehouse_id=112,
            delivery_service_id=(BASE_SERVICE_ID + delivery_service),
            operation_time=OPERATION_TIME_FOR_SERVICE[delivery_service],
            date_switch_time_infos=[
                DateSwitchTimeAndRegionInfo(
                    date_switch_hour=cut_off[1] + hours_diff,
                    region_to=212,
                    date_switch_time=TimeInfo(
                        cut_off[1] + hours_diff,
                        cut_off[2],
                    ),
                    packaging_time=TimeInfo(
                        cut_off[3],
                        cut_off[4],
                    ),
                )
                for cut_off in cut_offs
                if cut_off[0] == delivery_service
            ],
        )
        for delivery_service in DELIVERY_SERVICES
    ]

    if delivery_intervals_for_days:
        dynamic_lms_environment += [
            DynamicTimeIntervalsSet(
                key=delivery_intervals_for_day[1],
                intervals=[
                    TimeIntervalInfo(
                        TimeInfo(
                            delivery_interval[0] + hours_diff,
                            delivery_interval[1],
                        ),
                        TimeInfo(
                            delivery_interval[2] + hours_diff,
                            delivery_interval[3],
                        ),
                    )
                    for delivery_interval in delivery_intervals_for_day[0]
                ],
            )
            for delivery_intervals_for_day in delivery_intervals_for_days
        ]

    return dynamic_lms_environment


class T(TestCase):
    @classmethod
    def prepare(cls):
        """ """

        cls.settings.lms_autogenerate = False

        cls.index.regiontree += [
            Region(
                rid=212,
                tz_offset=(INITIAL_HOUR * 3600),
            ),
        ]

        cls.index.shops += [
            Shop(
                fesh=12,
                datafeed_id=12,
                priority_region=212,
                name='virtual shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
            Shop(
                fesh=22,
                datafeed_id=22,
                priority_region=212,
                name='blue shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue='REAL',
                warehouse_id=112,
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=(BASE_BUCKET_ID + delivery_service),
                dc_bucket_id=(BASE_BUCKET_ID + delivery_service),
                fesh=12,
                carriers=[BASE_SERVICE_ID + delivery_service],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=212,
                        options=[
                            DeliveryOption(
                                price=15,
                                day_from=DAY_FROM_DAY_TO_FOR_SERVICE_BUCKET[delivery_service][0],
                                day_to=DAY_FROM_DAY_TO_FOR_SERVICE_BUCKET[delivery_service][1],
                                shop_delivery_price=10,
                            ),
                        ],
                    ),
                ],
            )
            for delivery_service in DELIVERY_SERVICES
        ]

        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1",
                sku=1,
                waremd5="Sku1-wdDXWsIiLVm1goleg",
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
                delivery_buckets=[(BASE_BUCKET_ID + delivery_service) for delivery_service in DELIVERY_SERVICES],
            ),
        ]

    def check_sdd(self, test_cases, preferable_courier_delivery_day=None):
        """
        Execute all test cases provided: make the necessary changes to LMS data and validate the actual_delivery results
        See complete definition of the test_cases data below
        """

        for test_case in test_cases:
            current_hour = test_case[0]
            warehouse_holidays = test_case[1]
            delivery_service_holidays = test_case[2]
            cut_offs = test_case[3]
            delivery_intervals_for_days = test_case[4]
            options = test_case[5]

            hours_diff = INITIAL_HOUR - current_hour

            self.dynamic.lms = constant_lms_environment
            self.dynamic.lms += make_dynamic_lms_environment(
                current_hour,
                warehouse_holidays,
                delivery_service_holidays,
                cut_offs,
                delivery_intervals_for_days,
            )

            request = "place=actual_delivery&rids=212&combinator=0&offers-list=Sku1Price5-IiLVm1Goleg:1"
            if preferable_courier_delivery_day is not None:
                request += "&preferable-courier-delivery-day={}".format(preferable_courier_delivery_day)

            response = self.report.request_json(request)

            self.assertFragmentIn(
                response,
                {
                    'entity': "deliveryGroup",
                    'delivery': {
                        'options': [
                            {
                                'serviceId': str(BASE_SERVICE_ID + option[0]),
                                'shipmentDay': option[1],
                                'dayFrom': option[2],
                                'dayTo': option[3],
                                'isDefault': option[4],
                                'orderBefore': str(option[5] + hours_diff) if option[5] is not None else Absent(),
                                'orderBeforeMin': str(option[6]) if option[6] is not None else Absent(),
                                'packagingTime': "PT"
                                + str(option[7] + hours_diff if option[7] != 0 else 0)
                                + "H"
                                + str(option[8])
                                + "M",
                                'timeIntervals': [
                                    {
                                        'from': str(ALL_INTERVALS[interval][0] + hours_diff).zfill(2)
                                        + ":"
                                        + str(ALL_INTERVALS[interval][1]).zfill(2),
                                        'to': str(ALL_INTERVALS[interval][2] + hours_diff).zfill(2)
                                        + ":"
                                        + str(ALL_INTERVALS[interval][3]).zfill(2),
                                        'isDefault': interval == option[9],
                                    }
                                    for interval in option[10]
                                ]
                                if option[10]
                                else Absent(),
                            }
                            for option in options
                        ],
                    },
                },
                preserve_order=True,
            )

    # Test case data structure
    # ########################
    #
    # current hour,
    # warehouse holidays,
    # delivery service holidays,
    #
    # cut-offs
    # [
    #     (
    #         delivery service for the cut-off,
    #         cut-off hour,
    #         cut-off minute,
    #         packaging duration hour,
    #         packaging duration minute,
    #     ),
    #     ...
    # ],
    #
    # intervals for days
    # [
    #     (
    #         key of the set of intervals,
    #         key of the set of days,
    #     ),
    #     ...
    # ],
    #
    # expected options
    # [
    #     (
    #         delivery service,
    #         shipment day,
    #         day from,
    #         day to,
    #         is default option,
    #         order before hour,
    #         order before minute,
    #         packaging time hour,
    #         packaging time minute,
    #         key of the default interval,
    #         [
    #             key of the interval,
    #             ...
    #         ],
    #     ),
    #     ...
    # ]

    @classmethod
    def prepare_sdd(cls):
        """
        This preparation is just for debugging, change the prepared_case for the LMS data initialization on startup
        """

        prepared_case = (
            13,
            NO_DAYS,
            NO_DAYS,
            [
                (SDD_SERVICE, 6, 0, 1, 0),
                (NDD_SERVICE, 12, 0, 4, 0),
            ],
            [
                (NORMAL_INTERVALS, TODAY_ONLY),
                (OTHER_INTERVALS, TOMORROW_ONLY),
            ],
        )

        current_hour = prepared_case[0]
        warehouse_holidays = prepared_case[1]
        delivery_service_holidays = prepared_case[2]
        cut_offs = prepared_case[3]
        delivery_intervals_for_days = prepared_case[4]

        cls.dynamic.lms = constant_lms_environment
        cls.dynamic.lms += make_dynamic_lms_environment(
            current_hour,
            warehouse_holidays,
            delivery_service_holidays,
            cut_offs,
            delivery_intervals_for_days,
        )

        cls.index.lms = copy.deepcopy(cls.dynamic.lms)

    def test_early_sdd_order(self):
        """
        Match the SDD cut-off, match all intervals
        so SDD option wins today with all intervals
        """

        test_cases = [
            #   current_hour,
            #   warehouse_holidays,
            #   service_holidays,
            #   [
            #       (service, cutoff_h, cutoff_m, packaging_h, packaging_m),
            #   ], [
            #       (intervals, days),
            #   ], [
            #       Expected options
            #       (service, shipment, day_from, day_to, is_default, cutoff_h, cutoff_m, packaging_h, packaging_m, default_interval, [interval1, interval2, ...]),
            #   ]
            (
                5,
                NO_DAYS,
                NO_DAYS,
                [
                    (SDD_SERVICE, 5, 30, 0, 30),
                    (NDD_SERVICE, 12, 0, 4, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (SDD_SERVICE, 0, 0, 0, True, 5, 30, 6, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                    (SDD_SERVICE, 0, 1, 1, False, 5, 30, 6, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                ],
            ),
        ]
        self.check_sdd(test_cases)

    def test_late_sdd_order(self):
        """
        Match the SDD cut-off, miss early intervals
        so SDD option wins today with late intervals
        This test has different results if SDD is disabled: interval filtering
        """

        test_cases = [
            #   current_hour,
            #   warehouse_holidays,
            #   service_holidays,
            #   [
            #       (service, cutoff_h, cutoff_m, packaging_h, packaging_m),
            #   ], [
            #       (intervals, days),
            #   ], [
            #       Expected options
            #       (service, shipment, day_from, day_to, is_default, cutoff_h, cutoff_m, packaging_h, packaging_m, default_interval, [interval1, interval2, ...]),
            #   ]
            (
                9,
                NO_DAYS,
                NO_DAYS,
                [
                    (SDD_SERVICE, 10, 0, 0, 30),
                    (NDD_SERVICE, 12, 0, 4, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (SDD_SERVICE, 0, 0, 0, True, 10, 0, 10, 30, I_11_14, [I_11_14, I_15_17]),
                    (SDD_SERVICE, 0, 1, 1, False, 10, 0, 10, 30, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                ],
            ),
        ]
        self.check_sdd(test_cases)

    def test_missed_sdd_order(self):
        """
        Match the SDD cut-off, miss all today intervals
        so NDD option wins with all intervals for tomorrow
        This test has different results if SDD is disabled: SDD has the same speed as NDD
        """

        test_cases = [
            #   current_hour,
            #   warehouse_holidays,
            #   service_holidays,
            #   [
            #       (service, cutoff_h, cutoff_m, packaging_h, packaging_m),
            #   ], [
            #       (intervals, days),
            #   ], [
            #       Expected options
            #       (service, shipment, day_from, day_to, is_default, cutoff_h, cutoff_m, packaging_h, packaging_m, default_interval, [interval1, interval2, ...]),
            #   ]
            (
                16,
                NO_DAYS,
                NO_DAYS,
                [
                    (SDD_SERVICE, 16, 30, 0, 30),
                    (NDD_SERVICE, 17, 0, 1, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (NDD_SERVICE, 0, 1, 1, True, 17, 0, 18, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                    (NDD_SERVICE, 0, 2, 2, False, 17, 0, 18, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                ],
            ),
        ]
        self.check_sdd(test_cases)

    def test_ndd_order(self):
        """
        Miss the SDD cut-off, but match the NDD cut-off
        so NDD option wins with all intervals for tomorrow
        """

        test_cases = [
            #   current_hour,
            #   warehouse_holidays,
            #   service_holidays,
            #   [
            #       (service, cutoff_h, cutoff_m, packaging_h, packaging_m),
            #   ], [
            #       (intervals, days),
            #   ], [
            #       Expected options
            #       (service, shipment, day_from, day_to, is_default, cutoff_h, cutoff_m, packaging_h, packaging_m, default_interval, [interval1, interval2, ...]),
            #   ]
            (
                7,
                NO_DAYS,
                NO_DAYS,
                [
                    (SDD_SERVICE, 6, 0, 1, 0),
                    (NDD_SERVICE, 12, 0, 4, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (NDD_SERVICE, 0, 1, 1, True, 12, 0, 16, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                    (NDD_SERVICE, 0, 2, 2, False, 12, 0, 16, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                ],
            ),
        ]
        self.check_sdd(test_cases)

    def test_missed_ndd_order(self):
        """
        Miss the SDD cut-off, miss the NDD cut-off
        so SDD option wins, with all intervals for tomorrow
        """

        test_cases = [
            #   current_hour,
            #   warehouse_holidays,
            #   service_holidays,
            #   [
            #       (service, cutoff_h, cutoff_m, packaging_h, packaging_m),
            #   ], [
            #       (intervals, days),
            #   ], [
            #       Expected options
            #       (service, shipment, day_from, day_to, is_default, cutoff_h, cutoff_m, packaging_h, packaging_m, default_interval, [interval1, interval2, ...]),
            #   ]
            (
                13,
                NO_DAYS,
                NO_DAYS,
                [
                    (SDD_SERVICE, 6, 0, 0, 30),
                    (NDD_SERVICE, 12, 0, 4, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (SDD_SERVICE, 1, 1, 1, True, None, None, 6, 30, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                    (SDD_SERVICE, 1, 2, 2, False, None, None, 6, 30, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                ],
            ),
        ]
        self.check_sdd(test_cases)

    def test_exact_cutoff_match(self):
        """
        ...
        """

        test_cases = [
            #   current_hour,
            #   warehouse_holidays,
            #   service_holidays,
            #   [
            #       (service, cutoff_h, cutoff_m, packaging_h, packaging_m),
            #   ], [
            #       (intervals, days),
            #   ], [
            #       Expected options
            #       (service, shipment, day_from, day_to, is_default, cutoff_h, cutoff_m, packaging_h, packaging_m, default_interval, [interval1, interval2, ...]),
            #   ]
            (
                12,
                NO_DAYS,
                NO_DAYS,
                [
                    (NDD_SERVICE, 12, 0, 3, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (NDD_SERVICE, 1, 2, 2, True, None, None, 15, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                    (NDD_SERVICE, 1, 3, 3, False, None, None, 15, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                ],
            ),
        ]
        self.check_sdd(test_cases)

        test_cases = [
            #   current_hour,
            #   warehouse_holidays,
            #   service_holidays,
            #   [
            #       (service, cutoff_h, cutoff_m, packaging_h, packaging_m),
            #   ], [
            #       (intervals, days),
            #   ], [
            #       Expected options
            #       (service, shipment, day_from, day_to, is_default, cutoff_h, cutoff_m, packaging_h, packaging_m, default_interval, [interval1, interval2, ...]),
            #   ]
            (
                10,
                NO_DAYS,
                NO_DAYS,
                [
                    (SDD_SERVICE, 10, 0, 1, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (SDD_SERVICE, 1, 1, 1, True, None, None, 11, 0, I_15_17, [I_15_17]),
                    (SDD_SERVICE, 1, 2, 2, False, None, None, 11, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                ],
            ),
        ]
        self.check_sdd(test_cases)

    def test_missed_ndd_order_different_intervals_tomorrow(self):
        """
        Miss the SDD cut-off, miss the NDD cut-off
        so SDD option wins with all different intervals for tomorrow only
        (and there is only one option for tomorrow because there are intervals for today and tomorrow only)
        """

        test_cases = [
            #   current_hour,
            #   warehouse_holidays,
            #   service_holidays,
            #   [
            #       (service, cutoff_h, cutoff_m, packaging_h, packaging_m),
            #   ], [
            #       (intervals, days),
            #   ], [
            #       Expected options
            #       (service, shipment, day_from, day_to, is_default, cutoff_h, cutoff_m, packaging_h, packaging_m, default_interval, [interval1, interval2, ...]),
            #   ]
            (
                13,
                NO_DAYS,
                NO_DAYS,
                [
                    (SDD_SERVICE, 6, 0, 1, 0),
                    (NDD_SERVICE, 12, 0, 4, 0),
                ],
                [
                    (NORMAL_INTERVALS, TODAY_ONLY),
                    (OTHER_INTERVALS, TOMORROW_ONLY),
                ],
                [
                    (SDD_SERVICE, 1, 1, 1, True, None, None, 7, 0, I_12_15, [I_12_15, I_08_10]),
                ],
            ),
        ]
        self.check_sdd(test_cases)

    def test_sdd_delivery_vacation_today(self):
        """
        Match the SDD cut-off, miss early intervals, delivery service vacation is today,
        so SDD option wins with all intervals for tomorrow
        """

        test_cases = [
            #   current_hour,
            #   warehouse_holidays,
            #   service_holidays,
            #   [
            #       (service, cutoff_h, cutoff_m, packaging_h, packaging_m),
            #   ], [
            #       (intervals, days),
            #   ], [
            #       Expected options
            #       (service, shipment, day_from, day_to, is_default, cutoff_h, cutoff_m, packaging_h, packaging_m, default_interval, [interval1, interval2, ...]),
            #   ]
            (
                9,
                NO_DAYS,
                TODAY_ONLY,
                [
                    (SDD_SERVICE, 10, 0, 0, 30),
                    (NDD_SERVICE, 12, 0, 4, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (SDD_SERVICE, 0, 1, 1, True, 10, 0, 10, 30, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                    (SDD_SERVICE, 0, 2, 2, False, 10, 0, 10, 30, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                ],
            ),
        ]
        self.check_sdd(test_cases)

    def test_sdd_warehouse_vacation_today(self):
        """
        Match the SDD cut-off, miss early intervals, warehouse vacation is today
        so SDD option wins with late intervals for tomorrow
        """

        test_cases = [
            #   current_hour,
            #   warehouse_holidays,
            #   service_holidays,
            #   [
            #       (service, cutoff_h, cutoff_m, packaging_h, packaging_m),
            #   ], [
            #       (intervals, days),
            #   ], [
            #       Expected options
            #       (service, shipment, day_from, day_to, is_default, cutoff_h, cutoff_m, packaging_h, packaging_m, default_interval, [interval1, interval2, ...]),
            #   ]
            (
                9,
                TODAY_ONLY,
                NO_DAYS,
                [
                    (SDD_SERVICE, 10, 0, 0, 30),
                    (NDD_SERVICE, 12, 0, 4, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (SDD_SERVICE, 1, 1, 1, True, 10, 0, 10, 30, I_11_14, [I_11_14, I_15_17]),
                    (SDD_SERVICE, 1, 2, 2, False, 10, 0, 10, 30, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                ],
            ),
        ]
        self.check_sdd(test_cases)

    def test_ndd_delivery_vacation_tomorrow(self):
        """
        Miss the SDD cut-off, match the NDD cut-off, delivery service vacation is tomorrow
        so NDD option wins with all intervals for the day after tomorrow
        """

        test_cases = [
            #   current_hour,
            #   warehouse_holidays,
            #   service_holidays,
            #   [
            #       (service, cutoff_h, cutoff_m, packaging_h, packaging_m),
            #   ], [
            #       (intervals, days),
            #   ], [
            #       Expected options
            #       (service, shipment, day_from, day_to, is_default, cutoff_h, cutoff_m, packaging_h, packaging_m, default_interval, [interval1, interval2, ...]),
            #   ]
            (
                7,
                NO_DAYS,
                TOMORROW_ONLY,
                [
                    (SDD_SERVICE, 6, 0, 1, 0),
                    (NDD_SERVICE, 12, 0, 4, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (NDD_SERVICE, 0, 2, 2, True, 12, 0, 16, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                    (NDD_SERVICE, 0, 3, 3, False, 12, 0, 16, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                ],
            ),
        ]
        self.check_sdd(test_cases)

    def test_ndd_warehouse_vacation_tomorrow(self):
        """
        Miss the SDD cut-off, match the NDD cut-off, warehouse vacation is tomorrow
        so NDD option wins with late intervals for the day after tomorrow
        """

        test_cases = [
            #   current_hour,
            #   warehouse_holidays,
            #   service_holidays,
            #   [
            #       (service, cutoff_h, cutoff_m, packaging_h, packaging_m),
            #   ], [
            #       (intervals, days),
            #   ], [
            #       Expected options
            #       (service, shipment, day_from, day_to, is_default, cutoff_h, cutoff_m, packaging_h, packaging_m, default_interval, [interval1, interval2, ...]),
            #   ]
            (
                7,
                NO_DAYS,
                TOMORROW_ONLY,
                [
                    (SDD_SERVICE, 6, 0, 1, 0),
                    (NDD_SERVICE, 12, 0, 4, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (NDD_SERVICE, 0, 2, 2, True, 12, 0, 16, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                    (NDD_SERVICE, 0, 3, 3, False, 12, 0, 16, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                ],
            ),
        ]
        self.check_sdd(test_cases)

    def test_sdd_warehouse_vacation_today_delivery_vacation_tomorrow(self):
        """
        Match the SDD cut-off, miss early intervals, warehouse vacation is today, delivery vacation is tomorrow
        so SDD option wins with all intervals for the day after tomorrow
        """

        test_cases = [
            #   current_hour,
            #   warehouse_holidays,
            #   service_holidays,
            #   [
            #       (service, cutoff_h, cutoff_m, packaging_h, packaging_m),
            #   ], [
            #       (intervals, days),
            #   ], [
            #       Expected options
            #       (service, shipment, day_from, day_to, is_default, cutoff_h, cutoff_m, packaging_h, packaging_m, default_interval, [interval1, interval2, ...]),
            #   ]
            (
                9,
                TODAY_ONLY,
                TOMORROW_ONLY,
                [
                    (SDD_SERVICE, 10, 0, 0, 30),
                    (NDD_SERVICE, 12, 0, 4, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (SDD_SERVICE, 1, 2, 2, True, 10, 0, 10, 30, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                    (SDD_SERVICE, 1, 3, 3, False, 10, 0, 10, 30, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                ],
            ),
        ]
        self.check_sdd(test_cases)

    def test_sdd_order_preferable_delivery_today(self):
        """
        place=actual_delivery is requested with additional flag preferable-courier-delivery-day= to re-calculate
        the proper shipment day based on the desired delivery day, selected by the user
        When asked for today's delivery, the shipment day must also be today.
        Early intervals are unavailable.
        """

        test_cases = [
            #   current_hour,
            #   warehouse_holidays,
            #   service_holidays,
            #   [
            #       (service, cutoff_h, cutoff_m, packaging_h, packaging_m),
            #   ], [
            #       (intervals, days),
            #   ], [
            #       Expected options
            #       (service, shipment, day_from, day_to, is_default, cutoff_h, cutoff_m, packaging_h, packaging_m, default_interval, [interval1, interval2, ...]),
            #   ]
            (
                10,
                NO_DAYS,
                NO_DAYS,
                [
                    (SDD_SERVICE, 11, 0, 3, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (SDD_SERVICE, 0, 0, 0, True, 11, 0, 14, 0, I_15_17, [I_15_17]),
                ],
            ),
        ]
        self.check_sdd(test_cases, preferable_courier_delivery_day=0)

    def test_sdd_order_preferable_delivery_tomorrow(self):
        """
        place=actual_delivery is requested with additional flag preferable-courier-delivery-day= to re-calculate
        the proper shipment day based on the desired delivery day, selected by the user
        When asked for tomorrow's delivery, and we match the cut-off, the shipment day must be today, because
        this gives the maximum available intervals for tomorrow
        If we don't match the cut-off, the shipment is only possible tomorrow, and early intervals are unavailable.
        """

        test_cases = [
            #   current_hour,
            #   warehouse_holidays,
            #   service_holidays,
            #   [
            #       (service, cutoff_h, cutoff_m, packaging_h, packaging_m),
            #   ], [
            #       (intervals, days),
            #   ], [
            #       Expected options
            #       (service, shipment, day_from, day_to, is_default, cutoff_h, cutoff_m, packaging_h, packaging_m, default_interval, [interval1, interval2, ...]),
            #   ]
            (
                10,
                NO_DAYS,
                NO_DAYS,
                [
                    (SDD_SERVICE, 11, 0, 3, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (SDD_SERVICE, 0, 1, 1, False, 11, 0, 14, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                ],
            ),
            (
                12,
                NO_DAYS,
                NO_DAYS,
                [
                    (SDD_SERVICE, 11, 0, 3, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (SDD_SERVICE, 1, 1, 1, True, None, None, 14, 0, I_15_17, [I_15_17]),
                ],
            ),
        ]
        self.check_sdd(test_cases, preferable_courier_delivery_day=1)

    def test_sdd_order_preferable_delivery_later(self):
        """
        place=actual_delivery is requested with additional flag preferable-courier-delivery-day= to re-calculate
        the proper shipment day based on the desired delivery day, selected by the user
        When asked for the delivery on the future day, the shipment day must be the day minus one, because
        this gives the maximum available intervals for the next day
        """

        test_cases = [
            #   current_hour,
            #   warehouse_holidays,
            #   service_holidays,
            #   [
            #       (service, cutoff_h, cutoff_m, packaging_h, packaging_m),
            #   ], [
            #       (intervals, days),
            #   ], [
            #       Expected options
            #       (service, shipment, day_from, day_to, is_default, cutoff_h, cutoff_m, packaging_h, packaging_m, default_interval, [interval1, interval2, ...]),
            #   ]
            (
                10,
                NO_DAYS,
                NO_DAYS,
                [
                    (SDD_SERVICE, 11, 0, 3, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (SDD_SERVICE, 3, 4, 4, False, 11, 0, 14, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                ],
            ),
            (
                12,
                NO_DAYS,
                NO_DAYS,
                [
                    (SDD_SERVICE, 11, 0, 3, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (SDD_SERVICE, 3, 4, 4, False, None, None, 14, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                ],
            ),
        ]
        self.check_sdd(test_cases, preferable_courier_delivery_day=4)

    def test_two_cutoffs_one_with_no_intervals_sdd_only(self):
        """
        Two cut-offs, the last one is late for today intervals (and the packaging is before of after the midnight).
        The special case is if there is the SDD tariff ONLY. In this case SDD options work as follows:
          - shipment day = 0 (today)
          - day from = 1, day to = 1 (tomorrow)
            - this contradicts the definition of the tariff for SDD, but the logic is INTENTIONALLY changed with an extra delay
          - packaging time is calculated since midnight of today (day = 0)
          - intervals for tomorrow are chosen after the packaging time
        """

        test_cases = [
            #   current_hour,
            #   warehouse_holidays,
            #   service_holidays,
            #   [
            #       (service, cutoff_h, cutoff_m, packaging_h, packaging_m),
            #   ], [
            #       (intervals, days),
            #   ], [
            #       Expected options
            #       (service, shipment, day_from, day_to, is_default, cutoff_h, cutoff_m, packaging_h, packaging_m, default_interval, [interval1, interval2, ...]),
            #   ]
            (
                10,
                NO_DAYS,
                NO_DAYS,
                [
                    (SDD_SERVICE, 11, 0, 3, 0),
                    (SDD_SERVICE, 16, 0, 6, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (SDD_SERVICE, 0, 0, 0, True, 11, 0, 14, 0, I_15_17, [I_15_17]),
                    (SDD_SERVICE, 0, 1, 1, False, 11, 0, 14, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                ],
            ),
            (
                12,
                NO_DAYS,
                NO_DAYS,
                [
                    (SDD_SERVICE, 11, 0, 3, 0),
                    (SDD_SERVICE, 16, 0, 6, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (SDD_SERVICE, 0, 1, 1, True, 16, 0, 22, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                    (SDD_SERVICE, 0, 2, 2, False, 16, 0, 22, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                ],
            ),
            (
                12,
                NO_DAYS,
                NO_DAYS,
                [
                    (SDD_SERVICE, 11, 0, 3, 0),
                    (SDD_SERVICE, 16, 0, 10, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (SDD_SERVICE, 0, 1, 1, True, 16, 0, 26, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                    (SDD_SERVICE, 0, 2, 2, False, 16, 0, 26, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                ],
            ),
            (
                12,
                NO_DAYS,
                NO_DAYS,
                [
                    (SDD_SERVICE, 11, 0, 3, 0),
                    (SDD_SERVICE, 16, 0, 16, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (SDD_SERVICE, 0, 1, 1, True, 16, 0, 32, 0, I_10_16, [I_10_16, I_11_14, I_15_17]),
                    (SDD_SERVICE, 0, 2, 2, False, 16, 0, 32, 0, I_10_16, [I_10_16, I_11_14, I_15_17]),
                ],
            ),
        ]
        self.check_sdd(test_cases)

    def test_two_cutoffs_one_with_no_intervals(self):
        """
        Same as above, but with both SDD and NDD options -- explicit regression
        """

        test_cases = [
            #   current_hour,
            #   warehouse_holidays,
            #   service_holidays,
            #   [
            #       (service, cutoff_h, cutoff_m, packaging_h, packaging_m),
            #   ], [
            #       (intervals, days),
            #   ], [
            #       Expected options
            #       (service, shipment, day_from, day_to, is_default, cutoff_h, cutoff_m, packaging_h, packaging_m, default_interval, [interval1, interval2, ...]),
            #   ]
            (
                10,
                NO_DAYS,
                NO_DAYS,
                [
                    (SDD_SERVICE, 11, 0, 3, 0),
                    (NDD_SERVICE, 16, 0, 6, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (SDD_SERVICE, 0, 0, 0, True, 11, 0, 14, 0, I_15_17, [I_15_17]),
                    (SDD_SERVICE, 0, 1, 1, False, 11, 0, 14, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                ],
            ),
            (
                12,
                NO_DAYS,
                NO_DAYS,
                [
                    (SDD_SERVICE, 11, 0, 3, 0),
                    (NDD_SERVICE, 16, 0, 6, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (NDD_SERVICE, 0, 1, 1, True, 16, 0, 22, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                    (NDD_SERVICE, 0, 2, 2, False, 16, 0, 22, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                ],
            ),
            (
                12,
                NO_DAYS,
                NO_DAYS,
                [
                    (SDD_SERVICE, 11, 0, 3, 0),
                    (NDD_SERVICE, 16, 0, 10, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (NDD_SERVICE, 0, 1, 1, True, 16, 0, 26, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                    (NDD_SERVICE, 0, 2, 2, False, 16, 0, 26, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                ],
            ),
            (
                12,
                NO_DAYS,
                NO_DAYS,
                [
                    (SDD_SERVICE, 11, 0, 3, 0),
                    (NDD_SERVICE, 16, 0, 16, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (NDD_SERVICE, 0, 1, 1, True, 16, 0, 32, 0, I_10_16, [I_10_16, I_11_14, I_15_17]),
                    (NDD_SERVICE, 0, 2, 2, False, 16, 0, 32, 0, I_10_16, [I_10_16, I_11_14, I_15_17]),
                ],
            ),
        ]
        self.check_sdd(test_cases)

    def test_sdd_intervals_not_set(self):
        """
        Test SDD сase when there are no delivery intervals at all for a DS
        Courier delivery doesn't and shouldn't work properly without intervals
        Below the result has only one option for the fastest delivery day
        The logic for 5 later days breaks because it depends on the intervals presence
        In theory SDD should also work for pickup without intervals, create tests here:
        https://st.yandex-team.ru/MARKETOUT-30461
        """

        test_cases = [
            #   current_hour,
            #   warehouse_holidays,
            #   service_holidays,
            #   [
            #       (service, cutoff_h, cutoff_m, packaging_h, packaging_m),
            #   ], [
            #       (intervals, days),
            #   ], [
            #       Expected options
            #       (service, shipment, day_from, day_to, is_default, cutoff_h, cutoff_m, packaging_h, packaging_m, default_interval, [interval1, interval2, ...]),
            #   ]
            (
                5,
                NO_DAYS,
                NO_DAYS,
                [
                    (SDD_SERVICE, 5, 30, 0, 30),
                ],
                INTERVALS_NOT_SET,
                [
                    (SDD_SERVICE, 0, 0, 0, True, 5, 30, 6, 0, None, None),
                ],
            ),
        ]
        self.check_sdd(test_cases)

    def test_sdd_no_intervals_set_for_today(self):
        """
        Test SDD сase when there are no delivery intervals for today
        Courier delivery doesn't and shouldn't work properly without intervals
        Below the result has only one option for the fastest delivery day
        The logic for 5 later days breaks because it depends on the intervals presence
        """

        test_cases = [
            #   current_hour,
            #   warehouse_holidays,
            #   service_holidays,
            #   [
            #       (service, cutoff_h, cutoff_m, packaging_h, packaging_m),
            #   ], [
            #       (intervals, days),
            #   ], [
            #       Expected options
            #       (service, shipment, day_from, day_to, is_default, cutoff_h, cutoff_m, packaging_h, packaging_m, default_interval, [interval1, interval2, ...]),
            #   ]
            (
                5,
                NO_DAYS,
                NO_DAYS,
                [
                    (SDD_SERVICE, 5, 30, 0, 30),
                ],
                [
                    (NO_INTERVALS, TODAY_ONLY),
                    (NORMAL_INTERVALS, TOMORROW_ONLY),
                ],
                [
                    (SDD_SERVICE, 0, 1, 1, True, 5, 30, 6, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                ],
            ),
            (
                5,
                NO_DAYS,
                NO_DAYS,
                [
                    (SDD_SERVICE, 5, 30, 0, 30),
                ],
                [
                    (NORMAL_INTERVALS, TOMORROW_ONLY),
                ],
                [
                    (SDD_SERVICE, 0, 1, 1, True, 5, 30, 6, 0, I_10_16, [I_10_16, I_07_11, I_11_14, I_15_17]),
                ],
            ),
        ]
        self.check_sdd(test_cases)

    def test_ndd_with_handling_time_or_operation_time(self):
        """
        New NDD case: when tariff day_from = 0 and day_to = 0, but LMS operation_time > 0
        and/or handling_time > 0 this is actually NOT SDD, but NDD case!
        Interval checking logic should be skipped. Testing it here.
        """

        test_cases = [
            #   current_hour,
            #   warehouse_holidays,
            #   service_holidays,
            #   [
            #       (service, cutoff_h, cutoff_m, packaging_h, packaging_m),
            #   ], [
            #       (intervals, days),
            #   ], [
            #       Expected options
            #       (service, shipment, day_from, day_to, is_default, cutoff_h, cutoff_m, packaging_h, packaging_m, default_interval, [interval1, interval2, ...]),
            #   ]
            (
                7,
                NO_DAYS,
                NO_DAYS,
                [
                    (NDD_SERVICE_WITH_HANDLING_TIME, 12, 0, 4, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (
                        NDD_SERVICE_WITH_HANDLING_TIME,
                        0,
                        1,
                        1,
                        True,
                        12,
                        0,
                        16,
                        0,
                        I_10_16,
                        [I_10_16, I_07_11, I_11_14, I_15_17],
                    ),
                    (
                        NDD_SERVICE_WITH_HANDLING_TIME,
                        0,
                        2,
                        2,
                        False,
                        12,
                        0,
                        16,
                        0,
                        I_10_16,
                        [I_10_16, I_07_11, I_11_14, I_15_17],
                    ),
                ],
            ),
            (
                7,
                NO_DAYS,
                NO_DAYS,
                [
                    (NDD_SERVICE_WITH_OPERATION_TIME, 12, 0, 4, 0),
                ],
                [
                    (NORMAL_INTERVALS, ALL_DAYS),
                ],
                [
                    (
                        NDD_SERVICE_WITH_OPERATION_TIME,
                        0,
                        1,
                        1,
                        True,
                        12,
                        0,
                        16,
                        0,
                        I_10_16,
                        [I_10_16, I_07_11, I_11_14, I_15_17],
                    ),
                    (
                        NDD_SERVICE_WITH_OPERATION_TIME,
                        0,
                        2,
                        2,
                        False,
                        12,
                        0,
                        16,
                        0,
                        I_10_16,
                        [I_10_16, I_07_11, I_11_14, I_15_17],
                    ),
                ],
            ),
        ]
        self.check_sdd(test_cases)


if __name__ == "__main__":
    main()
