#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent, Contains, NoKey
from core.types import (
    BookingAvailability,
    BusinessCalendar,
    Const,
    Currency,
    DeliveryBucket,
    DeliveryCalendar,
    DeliveryOption,
    ExchangeRate,
    GpsCoord,
    Model,
    Offer,
    OfferDimensions,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Region,
    Shop,
)
from core.testcase import TestCase, main
from core.datasync import DataSyncYandexUserAddress
from core.types.delivery import OutletWorkingTime
from datetime import date, timedelta


HOME_GPS_COORD = GpsCoord(37.40, 55.5)
WORK_GPS_COORD = GpsCoord(37.35, 55.15)


def getOutletWorkingTimeWithSeveralIntervals(day):
    return [
        OutletWorkingTime(days_from=day, days_till=day, hours_from="10:00", hours_till="18:00"),
        OutletWorkingTime(days_from=day, days_till=day, hours_from="18:30", hours_till="19:00"),
        OutletWorkingTime(days_from=day, days_till=day, hours_from="20:00", hours_till="22:00"),
    ]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.settings.enable_testing_features = False
        # rids: [1, 100]
        # fesh: [101, 200]
        # outlet id: [201, 300]
        # hyperid: [301, 400]

        cls.index.regiontree += [
            Region(
                rid=213,
                name='Moscow',
                tz_offset=10800,
                region_type=Region.CITY,
                children=[Region(rid=1234, name='region1')],
            ),
            Region(rid=54, name='Yekaterinburg', tz_offset=18000),
            Region(rid=22, name='Kaliningrad', tz_offset=7200),
            Region(rid=202, name='New York', tz_offset=-18000),
        ]

        cls.index.currencies = [
            Currency(
                'KZT',
                exchange_rates=[
                    ExchangeRate(fr=Currency.RUR, rate=0.2),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=101, priority_region=213),
            Shop(fesh=102, priority_region=213),
            Shop(fesh=103, priority_region=213),
            Shop(fesh=104, priority_region=213),
            Shop(fesh=105, priority_region=213),
            Shop(fesh=106, priority_region=213),
            Shop(fesh=107, priority_region=213),
            Shop(fesh=108, priority_region=213),
            Shop(fesh=109, priority_region=213),
            Shop(fesh=110, priority_region=213),
            Shop(fesh=111, priority_region=213),
            Shop(fesh=112, priority_region=213),
        ]

        cls.index.business_calendars += [
            BusinessCalendar(region=225, holidays=[]),
        ]

        cls.index.delivery_calendars += [
            DeliveryCalendar(fesh=105, holidays=[0, 1, 2, 3, 4, 7]),
            DeliveryCalendar(fesh=110, holidays=[1]),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=201,
                fesh=101,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=3,
                    day_to=5,
                    order_before=6,
                    work_in_holiday=False,
                    price=500,
                    price_to=1000,
                    shipper_readable_id="self shipper",
                ),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=202,
                fesh=102,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=0,
                    day_to=2,
                    order_before=20,
                    work_in_holiday=False,
                    price=600,
                    price_to=1000,
                ),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=203,
                fesh=103,
                region=213,
                delivery_option=OutletDeliveryOption(
                    day_from=0,
                    day_to=2,
                    order_before=20,
                    work_in_holiday=False,
                    price=600,
                    price_to=1000,
                ),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=204,
                fesh=104,
                region=213,
                point_type=Outlet.FOR_STORE,
                delivery_option=OutletDeliveryOption(
                    day_from=0,
                    day_to=2,
                    order_before=20,
                    work_in_holiday=False,
                    price=600,
                    price_to=0,
                ),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=205,
                fesh=105,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=0,
                    day_to=2,
                    work_in_holiday=False,
                ),
                working_days=[i for i in [5, 6, 10]],
            ),
            Outlet(
                point_id=206,
                fesh=106,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=0,
                    day_to=2,
                    order_before=0,
                    work_in_holiday=False,
                ),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=207,
                fesh=107,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=0,
                    day_to=2,
                    order_before=0,
                    work_in_holiday=False,
                    unknown=True,
                ),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=208,
                fesh=108,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=209,
                fesh=109,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=0,
                    day_to=2,
                    order_before=0,
                    work_in_holiday=False,
                ),
                working_days=[i for i in range(2, 10)],
            ),
            Outlet(
                point_id=210,
                fesh=110,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=0,
                    day_to=2,
                    order_before=0,
                    work_in_holiday=False,
                ),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=211,
                fesh=111,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=0,
                    day_to=2,
                    order_before=None,
                    work_in_holiday=None,
                ),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=212,
                fesh=112,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=0,
                    day_to=2,
                    order_before=20,
                    work_in_holiday=False,
                    price=600,
                ),
                working_days=[i for i in range(10)],
            ),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=4000,
                fesh=112237,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                working_days=[0, 1, 2, 5],
                working_times=[
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.MONDAY,
                        days_till=OutletWorkingTime.MONDAY,
                        hours_from="10:00",
                        hours_till="18:00",
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.TUESDAY,
                        days_till=OutletWorkingTime.TUESDAY,
                        hours_from="10:00",
                        hours_till="18:00",
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.WEDNESDAY,
                        days_till=OutletWorkingTime.WEDNESDAY,
                        hours_from="10:00",
                        hours_till="18:00",
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.THURSDAY,
                        days_till=OutletWorkingTime.THURSDAY,
                        hours_from="10:00",
                        hours_till="18:00",
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.FRIDAY,
                        days_till=OutletWorkingTime.FRIDAY,
                        hours_from="10:00",
                        hours_till="18:00",
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.SATURDAY,
                        days_till=OutletWorkingTime.SATURDAY,
                        hours_from="10:00",
                        hours_till="18:00",
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.SUNDAY,
                        days_till=OutletWorkingTime.SUNDAY,
                        hours_from="10:00",
                        hours_till="18:00",
                    ),
                ],
            ),
            Outlet(
                point_id=4001,
                fesh=112237,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                working_days=[0, 1, 2, 5],
                working_times=getOutletWorkingTimeWithSeveralIntervals(OutletWorkingTime.MONDAY)
                + getOutletWorkingTimeWithSeveralIntervals(OutletWorkingTime.TUESDAY)
                + [
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.WEDNESDAY,
                        days_till=OutletWorkingTime.WEDNESDAY,
                        hours_from="10:00",
                        hours_till="18:00",
                    )
                ]
                + getOutletWorkingTimeWithSeveralIntervals(OutletWorkingTime.THURSDAY)
                + [
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.FRIDAY,
                        days_till=OutletWorkingTime.FRIDAY,
                        hours_from="10:00",
                        hours_till="18:00",
                    )
                ],
            ),
            Outlet(
                point_id=4002,
                fesh=112237,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                working_days=[0, 1, 2, 5],
                working_times=[
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.MONDAY,
                        days_till=OutletWorkingTime.MONDAY,
                        hours_from="10:00",
                        hours_till="18:00",
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.TUESDAY,
                        days_till=OutletWorkingTime.TUESDAY,
                        hours_from="10:00",
                        hours_till="18:00",
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.WEDNESDAY,
                        days_till=OutletWorkingTime.WEDNESDAY,
                        hours_from="10:00",
                        hours_till="18:00",
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.FRIDAY,
                        days_till=OutletWorkingTime.FRIDAY,
                        hours_from="10:00",
                        hours_till="18:00",
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.SATURDAY,
                        days_till=OutletWorkingTime.SATURDAY,
                        hours_from="10:00",
                        hours_till="18:00",
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.SUNDAY,
                        days_till=OutletWorkingTime.SUNDAY,
                        hours_from="10:00",
                        hours_till="18:00",
                    ),
                ],
            ),
            Outlet(
                point_id=4003,
                fesh=112237,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                working_days=[0, 1, 2, 5],
                working_times=getOutletWorkingTimeWithSeveralIntervals(OutletWorkingTime.MONDAY)
                + getOutletWorkingTimeWithSeveralIntervals(OutletWorkingTime.TUESDAY)
                + getOutletWorkingTimeWithSeveralIntervals(OutletWorkingTime.WEDNESDAY)
                + getOutletWorkingTimeWithSeveralIntervals(OutletWorkingTime.THURSDAY)
                + getOutletWorkingTimeWithSeveralIntervals(OutletWorkingTime.FRIDAY)
                + [
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.SATURDAY,
                        days_till=OutletWorkingTime.SATURDAY,
                        hours_from="10:00",
                        hours_till="18:00",
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.SUNDAY,
                        days_till=OutletWorkingTime.SUNDAY,
                        hours_from="10:00",
                        hours_till="18:00",
                    ),
                ],
            ),
            Outlet(
                fesh=5000,
                point_id=123456,
                region=213,
                email='aaa@bbb.ru',
                locality_name='Moscow',
                thoroughfare_name='Karl Marx av.',
                premise_number='1',
                block='2',
                km='3',
                estate='4',
                office_number='5',
                building='6',
                address_add='ABC',
                delivery_option=OutletDeliveryOption(
                    day_from=0,
                    day_to=2,
                    work_in_holiday=False,
                ),
            ),
            Outlet(
                point_id=123457,
                region=213,
                delivery_option=OutletDeliveryOption(
                    price=-1,
                    price_to=-1000,
                ),
            ),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=5000,
                region=213,
                delivery_service_id=1001,
                point_type=Outlet.FOR_PICKUP,
                working_days=[0, 1, 2, 3, 4, 6],
                working_times=[
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.TUESDAY,
                        days_till=OutletWorkingTime.TUESDAY,
                        hours_from="10:00",
                        hours_till="19:00",
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.WEDNESDAY,
                        days_till=OutletWorkingTime.WEDNESDAY,
                        hours_from="10:00",
                        hours_till="18:00",
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.FRIDAY,
                        days_till=OutletWorkingTime.FRIDAY,
                        hours_from="10:00",
                        hours_till="18:00",
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.SATURDAY,
                        days_till=OutletWorkingTime.SATURDAY,
                        hours_from="10:00",
                        hours_till="18:00",
                    ),
                ],
            )
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=101,
                carriers=[99],
                options=[PickupOption(outlet_id=201, day_from=3, day_to=5, price=500)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5002,
                fesh=102,
                carriers=[99],
                options=[PickupOption(outlet_id=202, day_from=0, day_to=2, price=600)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5003,
                fesh=103,
                carriers=[99],
                options=[PickupOption(outlet_id=203, day_from=0, day_to=2, price=600)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5004,
                fesh=104,
                carriers=[99],
                options=[PickupOption(outlet_id=204, day_from=0, day_to=2, price=600)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5005,
                fesh=105,
                carriers=[99],
                options=[PickupOption(outlet_id=205, day_from=0, day_to=2)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5006,
                fesh=106,
                carriers=[99],
                options=[PickupOption(outlet_id=206, day_from=0, day_to=2)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5007,
                fesh=107,
                carriers=[99],
                options=[PickupOption(outlet_id=207)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5008,
                fesh=108,
                carriers=[99],
                options=[PickupOption(outlet_id=208, price=0)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5009,
                fesh=109,
                carriers=[99],
                options=[PickupOption(outlet_id=209)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5010,
                fesh=110,
                carriers=[99],
                options=[PickupOption(outlet_id=210, day_from=0, day_to=2)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5011,
                fesh=111,
                carriers=[99],
                options=[PickupOption(outlet_id=211, day_from=0, day_to=2)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5012,
                fesh=112,
                carriers=[99],
                options=[PickupOption(outlet_id=212, day_from=0, day_to=2, price=600)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5013,
                fesh=135,
                carriers=[99],
                options=[PickupOption(outlet_id=253, day_from=0, day_to=1)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5014,
                fesh=136,
                carriers=[99],
                options=[PickupOption(outlet_id=254, day_from=0, day_to=1)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5015,
                carriers=[103],
                options=[PickupOption(outlet_id=213, day_from=0, day_to=2, price=600)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5016,
                fesh=114,
                carriers=[99],
                options=[PickupOption(outlet_id=214, day_from=0, day_to=2, price=600)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5017,
                fesh=115,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=215, day_from=3, day_to=5, price=500),
                    PickupOption(outlet_id=216, day_from=3, day_to=5, price=500),
                    PickupOption(outlet_id=217, day_from=3, day_to=5, price=500),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5018,
                carriers=[103],
                options=[PickupOption(outlet_id=218, day_from=3, day_to=5, price=500)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5019,
                carriers=[301],
                options=[PickupOption(outlet_id=300)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5020,
                fesh=123,
                carriers=[99],
                options=[PickupOption(outlet_id=302), PickupOption(outlet_id=303, day_from=5, day_to=10, price=700)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=6020,
                fesh=124,
                carriers=[99],
                options=[PickupOption(outlet_id=304, day_from=5, day_to=10, price=700)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5021,
                fesh=121,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=230, day_from=3, day_to=5, price=500),
                    PickupOption(outlet_id=231, day_from=3, day_to=5, price=500),
                    PickupOption(outlet_id=232, day_from=1, day_to=5, price=500),
                    PickupOption(outlet_id=233, day_from=1, day_to=5, price=500),
                    PickupOption(outlet_id=234, day_from=3, day_to=5, price=500),
                    PickupOption(outlet_id=235, day_from=3, day_to=5, price=500),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5022,
                fesh=122,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=245, day_from=3, day_to=5, price=500),
                    PickupOption(outlet_id=244, day_from=3, day_to=5, price=500),
                    PickupOption(outlet_id=243, day_from=3, day_to=5, price=500),
                    PickupOption(outlet_id=242, day_from=3, day_to=5, price=500),
                    PickupOption(outlet_id=241, day_from=3, day_to=5, price=500),
                    PickupOption(outlet_id=240, day_from=3, day_to=5, price=500),
                    PickupOption(outlet_id=239, day_from=3, day_to=5, price=500),
                    PickupOption(outlet_id=238, day_from=3, day_to=4, price=500),
                    PickupOption(outlet_id=237, day_from=1, day_to=5, price=500),
                    PickupOption(outlet_id=236, day_from=3, day_to=5, price=0),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5023,
                carriers=[123],
                options=[PickupOption(outlet_id=251, day_from=1, day_to=1, price=600)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5024,
                fesh=132,
                carriers=[99],
                options=[PickupOption(outlet_id=252)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5025,
                fesh=137,
                carriers=[99],
                options=[PickupOption(outlet_id=257)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5026,
                fesh=1563101,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=156310101, day_from=59, day_to=60, price=600),
                    PickupOption(outlet_id=156310102, day_from=60, day_to=61, price=600),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5027,
                fesh=75,
                carriers=[99],
                options=[PickupOption(outlet_id=175)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5028,
                fesh=120,
                carriers=[99],
                options=[PickupOption(outlet_id=228), PickupOption(outlet_id=229)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5029,
                fesh=1563102,
                carriers=[99],
                options=[PickupOption(outlet_id=156310103, day_from=61, day_to=70, price=600)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.models += [
            Model(hyperid=301),
            Model(hyperid=302),
            Model(hyperid=303),
            Model(hyperid=304),
            Model(hyperid=305),
            Model(hyperid=306),
            Model(hyperid=307),
            Model(hyperid=308),
            Model(hyperid=309),
            Model(hyperid=310),
        ]

        cls.index.offers += [
            Offer(fesh=101, hyperid=301, pickup_buckets=[5001]),
            Offer(fesh=102, hyperid=302, waremd5='aaaaaaaaaaaaaaaaaa_102', price=1000, pickup_buckets=[5002]),
            Offer(fesh=103, hyperid=302, waremd5='bbbbbbbbbbbbbbbbbb_103', price=1001, pickup_buckets=[5003]),
            Offer(fesh=104, hyperid=302, waremd5='cccccccccccccccccc_104', price=9999, pickup_buckets=[5004]),
            Offer(fesh=105, hyperid=303, pickup_buckets=[5005]),
            Offer(fesh=106, hyperid=304, pickup_buckets=[5006]),
            Offer(fesh=107, hyperid=305, pickup_buckets=[5007]),
            Offer(fesh=108, hyperid=306, pickup_buckets=[5008]),
            Offer(fesh=109, hyperid=307, pickup_buckets=[5009]),
            Offer(fesh=110, hyperid=308, pickup_buckets=[5010]),
            Offer(fesh=111, hyperid=309, pickup_buckets=[5011]),
            # report sets ONSTOCK flag if DeliveryOption with day_to <= 2
            Offer(
                fesh=112,
                hyperid=310,
                waremd5='aaaaaaaaaaaaaaaaaa_112',
                available=True,
                delivery_options=[DeliveryOption(day_from=0, day_to=2)],
                pickup_buckets=[5012],
            ),
            Offer(
                fesh=112,
                hyperid=310,
                waremd5='bbbbbbbbbbbbbbbbbb_112',
                available=False,
                delivery_options=[DeliveryOption(day_from=0, day_to=2)],
                pickup_buckets=[5012],
            ),
            # report clears ONSTOCK flag if DeliveryOption with day_to > 2
            Offer(
                fesh=112,
                hyperid=310,
                waremd5='cccccccccccccccccc_112',
                available=True,
                delivery_options=[DeliveryOption(day_from=0, day_to=3)],
                pickup_buckets=[5012],
            ),
            Offer(
                fesh=112,
                hyperid=310,
                waremd5='dddddddddddddddddd_112',
                available=False,
                delivery_options=[DeliveryOption(day_from=0, day_to=3)],
                pickup_buckets=[5012],
            ),
        ]

    def outlet_date(self, day):
        if self.settings.microseconds_for_disabled_random is not None:
            today = date.fromtimestamp(self.settings.microseconds_for_disabled_random / 10**6)
        else:
            today = Const.TODAY
        return today + timedelta(days=day)

    def test_working_days_format(self):
        response = self.report.request_json('place=geo&hyperid=301&rids=0')
        self.assertFragmentIn(
            response,
            {
                "workingDay": [
                    {
                        "date": str(self.outlet_date(day)),
                        "startTime": "00:00",
                        "endTime": "24:00",
                    }
                    for day in range(10)
                ],
            },
            preserve_order=True,
        )

    def test_compact_regions(self):
        response = self.report.request_json('place=geo&hyperid=301&rids=0&compact-regions=1')
        self.assertFragmentNotIn(response, {'delivery': {'shopPriorityRegion': {'name': 'Moscow'}}})

        self.assertFragmentIn(response, {'delivery': {'shopPriorityRegion': {'entity': 'region', 'id': 213}}})

        self.assertFragmentIn(
            response,
            {
                'regions': {
                    '213': {
                        'name': 'Moscow',
                        'lingua': {
                            'name': {
                                'genitive': 'Moscow',
                                'preposition': ' ',
                                'prepositional': 'Moscow',
                                'accusative': 'Moscow',
                            }
                        },
                        'type': 6,
                        'subtitle': 'Россия',
                    },
                    '225': {
                        'name': 'Россия',
                        'lingua': {
                            'name': {
                                'genitive': 'Россия',
                                'preposition': ' ',
                                'prepositional': 'Россия',
                                'accusative': 'Россия',
                            }
                        },
                        'type': 3,
                    },
                }
            },
        )

    def test_self_delivery_format(self):
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json('place=geo&hyperid=301&rids=0' + unified_off_flags)
        self.assertFragmentIn(
            response,
            {
                "selfDeliveryRule": {
                    "dayFrom": 3,
                    "dayTo": 5,
                    "orderBefore": 6,
                    "workInHoliday": False,
                    "currency": "RUR",
                    "cost": "500",
                    "priceTo": "1000",
                    "shipperHumanReadableId": "self shipper",
                },
            },
        )

    def test_outlet_types(self):
        # show delivery days and orderBefore only for DEPOT/MIXED outlets
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json(
            'place=geo&hyperid=302&rids=0&offerid=aaaaaaaaaaaaaaaaaa_102' + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": "202",
                    "type": "pickup",
                    "selfDeliveryRule": {
                        "dayFrom": 0,
                        "dayTo": 2,
                        "orderBefore": 20,
                        "workInHoliday": False,
                        "currency": "RUR",
                        "cost": "600",
                    },
                    "workingDay": [
                        {
                            "date": str(self.outlet_date(day)),
                            "startTime": "00:00",
                            "endTime": "24:00",
                        }
                        for day in range(10)
                    ],
                },
            },
        )

        response = self.report.request_json(
            'place=geo&hyperid=302&rids=0&offerid=bbbbbbbbbbbbbbbbbb_103' + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": "203",
                    "type": "mixed",
                    "selfDeliveryRule": {
                        "dayFrom": 0,
                        "dayTo": 2,
                        "orderBefore": 20,
                        "workInHoliday": False,
                        "currency": "RUR",
                        "cost": "0",
                    },
                    "workingDay": [
                        {
                            "date": str(self.outlet_date(day)),
                            "startTime": "00:00",
                            "endTime": "24:00",
                        }
                        for day in range(10)
                    ],
                },
            },
        )

        response = self.report.request_json(
            'place=geo&hyperid=302&rids=0&offerid=cccccccccccccccccc_104' + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": "204",
                    "type": "store",
                    "selfDeliveryRule": {
                        "dayFrom": NoKey("dayFrom"),
                        "dayTo": NoKey("dayTo"),
                        "orderBefore": NoKey("orderBefore"),
                        "workInHoliday": False,
                        "currency": "RUR",
                        "cost": "600",
                    },
                    "workingDay": [
                        {
                            "date": str(self.outlet_date(day)),
                            "startTime": "00:00",
                            "endTime": "24:00",
                        }
                        for day in range(10)
                    ],
                },
            },
        )

    def test_available_offer(self):
        # show delivery days and orderBefore only for AVAILABLE offers
        # (for any ONSTOCK flag)
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for available_offerid in ('aaaaaaaaaaaaaaaaaa_112', 'cccccccccccccccccc_112'):
            response = self.report.request_json(
                'place=geo&hyperid=310&rids=0&offerid={}'.format(available_offerid) + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    "outlet": {
                        "id": "212",
                        "type": "pickup",
                        "selfDeliveryRule": {
                            "dayFrom": 0,
                            "dayTo": 2,
                            "orderBefore": 20,
                            "workInHoliday": False,
                            "currency": "RUR",
                            "cost": "600",
                        },
                        "workingDay": [
                            {
                                "date": str(self.outlet_date(day)),
                                "startTime": "00:00",
                                "endTime": "24:00",
                            }
                            for day in range(10)
                        ],
                    },
                },
            )

        for unavailable_offerid in ('bbbbbbbbbbbbbbbbbb_112', 'dddddddddddddddddd_112'):
            response = self.report.request_json(
                'place=geo&hyperid=310&rids=0&offerid={}'.format(unavailable_offerid) + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    "outlet": {
                        "id": "212",
                        "type": "pickup",
                        "selfDeliveryRule": {
                            "dayFrom": NoKey("dayFrom"),
                            "dayTo": NoKey("dayTo"),
                            "orderBefore": NoKey("orderBefore"),
                            "workInHoliday": False,
                            "currency": "RUR",
                            "cost": "600",
                        },
                        "workingDay": [
                            {
                                "date": str(self.outlet_date(day)),
                                "startTime": "00:00",
                                "endTime": "24:00",
                            }
                            for day in range(10)
                        ],
                    },
                },
            )

    def test_currency(self):
        # 1 KZT = 5 RUR
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json('place=geo&hyperid=301&rids=0' + unified_off_flags)
        self.assertFragmentIn(
            response,
            {
                "selfDeliveryRule": {
                    "currency": "RUR",
                    "cost": "500",
                    "priceTo": "1000",
                },
            },
        )
        response = self.report.request_json('place=geo&hyperid=301&rids=0&currency=KZT' + unified_off_flags)
        self.assertFragmentIn(
            response,
            {
                "selfDeliveryRule": {
                    "currency": "KZT",
                    "cost": "100",
                    "priceTo": "200",
                },
            },
        )

    def test_cost(self):
        # price_to=1000 => free delivery if offer price > 1000
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json(
            'place=geo&hyperid=302&rids=0&offerid=aaaaaaaaaaaaaaaaaa_102' + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                "selfDeliveryRule": {
                    "currency": "RUR",
                    "cost": "600",
                    "priceTo": "1000",
                },
            },
        )
        response = self.report.request_json(
            'place=geo&hyperid=302&rids=0&offerid=bbbbbbbbbbbbbbbbbb_103' + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                "selfDeliveryRule": {
                    "currency": "RUR",
                    "cost": "0",
                    "priceTo": "1000",
                },
            },
        )

        # price_to=0 => no free delivery
        response = self.report.request_json(
            'place=geo&hyperid=302&rids=0&offerid=cccccccccccccccccc_104' + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                "selfDeliveryRule": {
                    "currency": "RUR",
                    "cost": "600",
                    "priceTo": NoKey("priceTo"),
                },
            },
        )

        # price or cost < 0
        response = self.report.request_xml('place=outlets&outlets=123457' + unified_off_flags)
        self.assertFragmentIn(
            response,
            '''
        <outlet>
            <SelfDeliveryRule>
                <Cost>0</Cost>
                <PriceTo>0</PriceTo>
                <CalcCurrency>RUR</CalcCurrency>
                <CalcCost>0</CalcCost>
                <CalcPriceTo>0</CalcPriceTo>
            </SelfDeliveryRule>
        </outlet>
        ''',
            allow_different_len=True,
        )

    @classmethod
    def prepare_order_before(cls):
        """Создаем магазины и офферы для проверки заказа после часа перескока в выходной/рабочий день"""
        cls.index.outlets += [
            Outlet(
                point_id=253,
                fesh=135,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(day_from=0, day_to=1, order_before=2, work_in_holiday=False),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=254,
                fesh=136,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(day_from=0, day_to=1, order_before=2, work_in_holiday=False),
                working_days=[i for i in range(10)],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=135, priority_region=213),
            Shop(fesh=136, priority_region=213),
        ]

        cls.index.offers += [
            Offer(fesh=135, hyperid=405, pickup_buckets=[5013]),
            Offer(fesh=136, hyperid=406, pickup_buckets=[5014]),
        ]

        cls.index.delivery_calendars += [
            DeliveryCalendar(fesh=135, holidays=[0, 1]),
            DeliveryCalendar(fesh=136, holidays=[1]),
        ]

    def check_day_from_to(self, hyperid, outlet_id):
        response = self.report.request_json(('place=geo&hyperid={0}&rids=213').format(hyperid))
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": outlet_id,
                    "selfDeliveryRule": {"dayFrom": 2, "dayTo": 3},
                },
            },
        )

    def test_order_before(self):
        # set orderBefore = 24
        # +1 день не прибавляется MARKETOUT-29814
        response = self.report.request_json('place=geo&hyperid=304&rids=0')
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": "206",
                    "selfDeliveryRule": {
                        "dayFrom": 0,
                        "dayTo": 2,
                        "orderBefore": 24,
                    },
                },
            },
        )

        # [0,2] (0 and 1 day is a shop holiday) -> [2,2],
        # set orderBefore = 24
        response = self.report.request_json('place=geo&hyperid=307&rids=0')
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": "209",
                    "selfDeliveryRule": {
                        "dayFrom": 2,
                        "dayTo": 2,
                        "orderBefore": 24,
                    },
                },
            },
        )

        # [0,2] -> (the 1st day is a delivery holiday) -> [0,3],
        # set orderBefore = 24
        response = self.report.request_json('place=geo&hyperid=308&rids=0')
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": "210",
                    "selfDeliveryRule": {
                        "dayFrom": 0,
                        "dayTo": 3,
                        "orderBefore": 24,
                    },
                },
            },
        )

        """Проверяется, что dayFrom и dayTo совпадают соответственно для аутлетов 253 и 254 в случае заказа в выходной и в будний день после часа перескока"""
        """    Заказ в выходной день"""
        self.check_day_from_to(405, "253")
        """    Заказ в будний день"""
        self.check_day_from_to(406, "254")

    def test_order_before_another_region(self):
        # order_before = 6
        response = self.report.request_json('place=geo&hyperid=301&rids=0&home-rids=213')
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": "201",
                    "selfDeliveryRule": {
                        "dayFrom": 3,
                        "dayTo": 5,
                        "orderBefore": 6,
                    },
                },
            },
        )
        response = self.report.request_json('place=geo&hyperid=301&rids=0&home-rids=54')
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": "201",
                    "selfDeliveryRule": {
                        "dayFrom": 3,
                        "dayTo": 5,
                        "orderBefore": 8,
                    },
                },
            },
        )
        response = self.report.request_json('place=geo&hyperid=301&rids=0&home-rids=22')
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": "201",
                    "selfDeliveryRule": {
                        "dayFrom": 3,
                        "dayTo": 5,
                        "orderBefore": 5,
                    },
                },
            },
        )
        response = self.report.request_json('place=geo&hyperid=301&rids=0&home-rids=202')
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": "201",
                    "selfDeliveryRule": {
                        "dayFrom": 3,
                        "dayTo": 5,
                        "orderBefore": 22,
                    },
                },
            },
        )

        # order_before = 24
        response = self.report.request_json('place=geo&hyperid=303&rids=0&home-rids=213')
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": "205",
                    "selfDeliveryRule": {
                        "dayFrom": 5,
                        "dayTo": 10,
                        "orderBefore": 24,
                    },
                },
            },
        )
        response = self.report.request_json('place=geo&hyperid=303&rids=0&home-rids=54')
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": "205",
                    "selfDeliveryRule": {
                        "dayFrom": 5,
                        "dayTo": 10,
                        "orderBefore": 24,
                    },
                },
            },
        )
        response = self.report.request_json('place=geo&hyperid=303&rids=0&home-rids=22')
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": "205",
                    "selfDeliveryRule": {
                        "dayFrom": 5,
                        "dayTo": 10,
                        "orderBefore": 24,
                    },
                },
            },
        )
        response = self.report.request_json('place=geo&hyperid=303&rids=0&home-rids=202')
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": "205",
                    "selfDeliveryRule": {
                        "dayFrom": 5,
                        "dayTo": 10,
                        "orderBefore": 24,
                    },
                },
            },
        )

    def test_unspecified_delivery_interval(self):
        response = self.report.request_json('place=geo&hyperid=304&rids=0')
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": "206",
                    "selfDeliveryRule": {
                        "dayFrom": 0,
                        "dayTo": 2,
                    },
                },
            },
        )
        response = self.report.request_json('place=geo&hyperid=305&rids=0')
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": "207",
                    "selfDeliveryRule": {
                        "dayFrom": NoKey("dayFrom"),
                        "dayTo": NoKey("dayTo"),
                    },
                },
            },
        )

    def test_calendar(self):
        response = self.report.request_json('place=geo&hyperid=303&rids=0')
        # day_from=0 -> 5 (0..4 - delivery holidays) -> 5 (outlet working day)
        # day_to=2 -> 8 (0..4, 7 - delivery holidays) -> 10 (8, 9 - outlet holidays)
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": "205",
                    "selfDeliveryRule": {
                        "dayFrom": 5,
                        "dayTo": 10,
                    },
                },
            },
        )

    def test_raw_delivery_options(self):
        response = self.report.request_json('place=geo&hyperid=303&rids=0&home-rids=54')
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": "205",
                    "selfDeliveryRule": {
                        "dayFrom": 5,
                        "dayTo": 10,
                        "orderBefore": 24,
                        "rawDayFrom": NoKey("rawDayFrom"),
                        "rawDayTo": NoKey("rawDayTo"),
                        "rawOrderBefore": NoKey("rawOrderBefore"),
                    },
                },
            },
        )

        response = self.report.request_json('place=geo&hyperid=303&rids=0&home-rids=54&raw_delivery_options=1')
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": "205",
                    "selfDeliveryRule": {
                        "dayFrom": 5,
                        "dayTo": 10,
                        "orderBefore": 24,
                        "rawDayFrom": 0,
                        "rawDayTo": 2,
                        "rawOrderBefore": 24,
                    },
                },
            },
        )

    def test_no_delivery(self):
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json('place=geo&hyperid=306&rids=0' + unified_off_flags)
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": "208",
                    "selfDeliveryRule": {
                        "dayFrom": NoKey("dayFrom"),
                        "dayTo": NoKey("dayTo"),
                        "orderBefore": NoKey("orderBefore"),
                        "workInHoliday": True,
                        "currency": "RUR",
                        "cost": "0",
                        "priceTo": NoKey("priceTo"),
                    },
                },
            },
        )

    def test_empty_delivery_params(self):
        response = self.report.request_json('place=geo&hyperid=309&rids=0')
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": "211",
                    "selfDeliveryRule": {
                        "dayFrom": 0,
                        "dayTo": 2,
                        "orderBefore": 24,
                        "workInHoliday": True,
                    },
                },
            },
        )

    # XML response
    @classmethod
    def prepare_postomat_delivery_data(cls):
        '''Создаем постамат с данными о доставке, привязываем его к магазину и оферу.'''
        cls.index.shops += [
            Shop(fesh=113, priority_region=213, delivery_service_outlets=[213], cpa=Shop.CPA_REAL),
            Shop(fesh=112238, priority_region=213, delivery_service_outlets=[5000], cpa=Shop.CPA_REAL),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=213,
                delivery_service_id=103,
                region=213,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(
                    day_from=0,
                    day_to=2,
                    order_before=20,
                    work_in_holiday=True,
                    price=600,
                ),
                working_days=[i for i in range(10)],
            ),
        ]

        cls.index.models += [
            Model(hyperid=311),
        ]

        cls.index.offers += [
            Offer(fesh=113, hyperid=311, post_term_delivery=True, cpa=Offer.CPA_REAL, pickup_buckets=[5015]),
        ]

    def test_postomat_delivery(self):
        '''place={geo}
        Проверяем, что данные по доставке есть в выдаче.
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json('place=geo&hyperid=311&rids=0' + unified_off_flags)
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": "213",
                    "type": "pickup",
                    "purpose": ["post_term"],
                    "selfDeliveryRule": {
                        "workInHoliday": True,
                        "currency": "RUR",
                        "cost": "600",
                    },
                },
            },
        )

    @classmethod
    def prepare_no_courier_delivery_data(cls):
        '''Создаем постамат с данными о доставке, привязываем его к магазину и оферу.'''
        cls.index.shops += [
            Shop(fesh=114, priority_region=213),
        ]

        cls.index.delivery_calendars += [
            DeliveryCalendar(fesh=114, holidays=[1]),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=214,
                fesh=114,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=0,
                    day_to=2,
                    order_before=20,
                    work_in_holiday=True,
                    price=600,
                ),
                working_days=[i for i in range(10)],
            ),
        ]

        cls.index.models += [
            Model(hyperid=312),
        ]

        cls.index.offers += [
            Offer(fesh=114, hyperid=312, has_delivery_options=False, pickup_buckets=[5016]),
        ]

    def test_no_courier_delivery_shop(self):
        '''place={geo}
        Проверяем, что в магазине без опций курьерской доставке у аутлетов отображаются СиС
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json('place=geo&hyperid=312&rids=0' + unified_off_flags)
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": "214",
                    "selfDeliveryRule": {
                        "dayFrom": 0,
                        "dayTo": 3,
                        "orderBefore": 20,
                        "workInHoliday": True,
                        "currency": "RUR",
                        "cost": "600",
                    },
                },
            },
        )

    @classmethod
    def prepare_pickup_options_data(cls):
        '''Создаем 3 аутлета pickup, store и mixed и
        службами доставки в них 99, 100 и 100 соответственно,
        1 постамат со службой доставки 103.
        Привязываем доставку к офферам из магазина.
        '''
        cls.index.shops += [
            Shop(fesh=115, priority_region=213, delivery_service_outlets=[218, 300], cpa=Shop.CPA_REAL),
            Shop(fesh=123, priority_region=213, delivery_service_outlets=[302, 303], cpa=Shop.CPA_REAL),
            Shop(fesh=124, priority_region=213, delivery_service_outlets=[304], cpa=Shop.CPA_REAL),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=215,
                fesh=115,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=3,
                    day_to=5,
                    order_before=16,
                    price=500,
                    price_to=1000,
                ),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=216,
                fesh=115,
                region=213,
                point_type=Outlet.FOR_STORE,
                delivery_option=OutletDeliveryOption(
                    day_from=3,
                    day_to=5,
                    order_before=16,
                    price=500,
                    price_to=1000,
                    shipper_id=100,
                ),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=217,
                fesh=115,
                region=213,
                point_type=Outlet.MIXED_TYPE,
                delivery_option=OutletDeliveryOption(
                    day_from=3,
                    day_to=5,
                    order_before=16,
                    price=500,
                    price_to=1000,
                    shipper_id=100,
                ),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=218,
                delivery_service_id=103,
                region=213,
                point_type=Outlet.FOR_POST_TERM,
                # set shipper_id manually (it's not set automatically by delivery_service_outlets)
                delivery_option=OutletDeliveryOption(
                    day_from=3,
                    day_to=5,
                    price=500,
                    price_to=1000,
                    shipper_id=103,
                ),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=300,
                delivery_service_id=301,
                region=54,
                point_type=Outlet.FOR_POST,
                delivery_option=OutletDeliveryOption(
                    day_from=3,
                    day_to=5,
                    price=500,
                    price_to=1000,
                    shipper_id=301,
                ),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=302,
                fesh=123,
                region=213,
                delivery_option=OutletDeliveryOption(day_from=1, day_to=3, price=70, price_to=100),
                point_type=Outlet.FOR_PICKUP,
                working_days=[i for i in range(31)],
            ),
            Outlet(
                point_id=303,
                fesh=123,
                region=2,
                delivery_option=OutletDeliveryOption(day_from=5, day_to=10, price=700, price_to=1000, shipper_id=301),
                point_type=Outlet.FOR_PICKUP,
                working_days=[i for i in range(31)],
            ),
            Outlet(
                point_id=304,
                fesh=124,
                region=213,
                delivery_option=OutletDeliveryOption(
                    day_from=5, day_to=10, price=700, price_to=1000, shipper_id=301, unknown=True
                ),
                point_type=Outlet.FOR_PICKUP,
                working_days=[i for i in range(31)],
            ),
        ]

        cls.index.models += [
            Model(hyperid=313),
            Model(hyperid=513),
            Model(hyperid=514),
        ]

        cls.index.offers += [
            Offer(
                fesh=115,
                hyperid=313,
                post_term_delivery=True,
                cpa=Offer.CPA_REAL,
                title="CPA REAL",
                pickup_buckets=[5017, 5018, 5019],
            ),
            Offer(
                fesh=115,
                hyperid=413,
                post_term_delivery=True,
                cpa=Offer.CPA_NO,
                title="CPA NO",
                pickup_buckets=[5017, 5018, 5019],
            ),
            Offer(
                fesh=115,
                hyperid=513,
                post_term_delivery=True,
                pickup_option=DeliveryOption(price=350, day_from=1, day_to=7, order_before=10),
                cpa=Offer.CPA_REAL,
                title="OFFER WITH PICKUP OPTIONS IN SHOP 115",
                pickup_buckets=[5017, 5018, 5019],
            ),
            Offer(
                fesh=123,
                hyperid=513,
                post_term_delivery=True,
                pickup_option=DeliveryOption(price=100, day_from=10, day_to=20, order_before=3),
                cpa=Offer.CPA_REAL,
                title="OFFER WITH PICKUP OPTIONS IN SHOP 123",
                pickup_buckets=[5020],
            ),
            Offer(
                fesh=124,
                hyperid=514,
                post_term_delivery=True,
                pickup_option=DeliveryOption(price=100, day_from=10, day_to=20, order_before=3),
                cpa=Offer.CPA_REAL,
                title="OFFER WITH PICKUP OPTIONS IN SHOP 124",
                pickup_buckets=[6020],
            ),
        ]

        cls.index.delivery_calendars += [
            DeliveryCalendar(fesh=124, holidays=[0, 1, 10, 11, 20, 21]),
        ]

    def test_outlets_purpose(self):
        """
        Проверяет, как разные типы ПВЗ отображаются в плэйсе outlets
        """
        sample = {
            '215': ['pickup'],
            '216': ['store'],
            '217': ['pickup', 'store'],
            '218': ['post_term'],
            '300': ['post'],
        }

        response = self.report.request_xml('place=outlets&outlets={}'.format(','.join(sample.keys())))
        for point_id, purpose in sample.items():
            purpose_text = '\n'.join(['<Purpose>{type}</Purpose>'.format(type=type) for type in purpose])

            self.assertFragmentIn(
                response,
                '''
                <outlet>
                    <PointId>{point_id}</PointId>
                    {purpose}
                </outlet>
            '''.format(
                    point_id=point_id, purpose=purpose_text
                ),
            )

    def test_pickup_options_format(self):
        '''place={prime, productoffers}
        Проверяем, что:
         - без флага pickup-options в выдаче нет "pickupOptions"
         - с флагом pickup-options=raw присутствует "pickupOptions" в нужном формате
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&hyperid=313&rids=213'.format(place) + unified_off_flags)
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "pickupOptions": NoKey("pickupOptions"),
                    },
                },
            )
            response = self.report.request_json(
                'place={}&hyperid=313&rids=213&pickup-options=raw'.format(place) + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "pickupOptions": [
                            {
                                "serviceId": 99,
                                "isMarketBranded": NoKey("isMarketBranded"),
                                "outlet": {
                                    "id": "215",
                                    "type": "pickup",
                                },
                                "price": {"currency": "RUR", "value": "500"},
                                "dayFrom": 3,
                                "dayTo": 5,
                                "orderBefore": 16,
                                "groupCount": NoKey("groupCount"),
                            },
                            {
                                "serviceId": 100,
                                "isMarketBranded": NoKey("isMarketBranded"),
                                "outlet": {
                                    "id": "216",
                                    "type": "store",
                                },
                                "price": {"currency": "RUR", "value": "500"},
                                "dayFrom": NoKey("dayFrom"),
                                "dayTo": NoKey("dayTo"),
                                "orderBefore": NoKey("orderBefore"),
                                "groupCount": NoKey("groupCount"),
                            },
                            {
                                "serviceId": 100,
                                "isMarketBranded": NoKey("isMarketBranded"),
                                "outlet": {
                                    "id": "217",
                                    "type": "mixed",
                                },
                                "price": {"currency": "RUR", "value": "500"},
                                "dayFrom": 3,
                                "dayTo": 5,
                                "orderBefore": 16,
                                "groupCount": NoKey("groupCount"),
                            },
                            {
                                "serviceId": 103,
                                "isMarketBranded": NoKey("isMarketBranded"),
                                "outlet": {"id": "218", "type": "pickup"},
                                "price": {"currency": "RUR", "value": "500"},
                                "groupCount": NoKey("groupCount"),
                            },
                        ],
                    },
                },
            )

    def test_pickup_options_format_cpa_no(self):
        '''place={prime, productoffers}
        Проверяем, что:
         - без флага pickup-options в выдаче нет "pickupOptions"
         - с флагом pickup-options=raw присутствует "pickupOptions" в нужном формате
         - служба не сра офера не отбрасывается
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&hyperid=413&rids=213'.format(place) + unified_off_flags)
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "pickupOptions": NoKey("pickupOptions"),
                    },
                },
            )
            response = self.report.request_json(
                'place={}&hyperid=413&rids=213&pickup-options=raw'.format(place) + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "pickupOptions": [
                            {
                                "serviceId": 99,
                                "isMarketBranded": NoKey("isMarketBranded"),
                                "outlet": {
                                    "id": "215",
                                    "type": "pickup",
                                },
                                "price": {"currency": "RUR", "value": "500"},
                                "dayFrom": 3,
                                "dayTo": 5,
                                "orderBefore": 16,
                                "groupCount": NoKey("groupCount"),
                            },
                        ],
                    },
                },
            )

            self.assertFragmentIn(response, {"serviceId": 103})

    def test_offer_with_self_pickup_options(self):
        '''У одного оффера из магазина 115 есть собственные опции самовывоза в домашнем регионе.
        И тогда они на выдаче заменяют опции всех аутлетов этого магазина в домашнем регионе.
        Но сроки заменяются только для аутлетов, имеющих параметры dayFrom/dayTo
        (например для аутлетов store их нет). Цена заменяется для всех домашних.
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers', 'geo']:
            response = self.report.request_json(
                'place={}&hyperid=513&rids=213&pickup-options=raw'.format(place) + unified_off_flags
            )

            self.assertFragmentIn(
                response,
                {
                    "titles": {
                        "raw": "OFFER WITH PICKUP OPTIONS IN SHOP 115",
                    },
                    "delivery": {
                        "pickupOptions": [
                            {
                                "serviceId": 99,
                                "isMarketBranded": NoKey("isMarketBranded"),
                                "outlet": {
                                    "id": "215",
                                    "type": "pickup",
                                },
                                "price": {"currency": "RUR", "value": "350"},
                                "dayFrom": 1,
                                "dayTo": 7,
                                "orderBefore": 10,
                            },
                            {
                                "serviceId": 100,
                                "outlet": {
                                    "id": "216",
                                    "type": "store",
                                },
                                "price": {"currency": "RUR", "value": "350"},
                                "dayFrom": NoKey("dayFrom"),
                                "dayTo": NoKey("dayTo"),
                                "orderBefore": NoKey("orderBefore"),
                            },
                            {
                                "serviceId": 100,
                                "isMarketBranded": NoKey("isMarketBranded"),
                                "outlet": {
                                    "id": "217",
                                    "type": "mixed",
                                },
                                "price": {"currency": "RUR", "value": "350"},
                                "dayFrom": 1,
                                "dayTo": 7,
                                "orderBefore": 10,
                            },
                            {
                                "serviceId": 103,
                                "isMarketBranded": NoKey("isMarketBranded"),
                                "outlet": {"id": "218", "type": "pickup"},
                                "price": {"currency": "RUR", "value": "350"},
                                "dayFrom": 1,
                                "dayTo": 7,
                                "orderBefore": 10,
                            },
                        ],
                    },
                },
            )

    def test_offer_with_self_pickup_options_home_region(self):
        '''У оффера из магазина 123 есть собственные опции самовывоза.
        Офферы магазина доставляются в два аутлета: один в регионе магазина, другой нет.
        Параметры самовывоза из оффера влияют только на аутлеты в домашнем регионе.

        Параллельно проверяется увеличение сроков доставки, если время в магазине >= orderBefore.
        В тестах текущее время прибито, и в Москве "сейчас" 3 утра. У офферной опции самовывоза
        dayFrom = 10, dayTo = 20. Из-за orderBefore = 3 эти сроки увеличиваются.
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response_moscow = self.report.request_json(
            'place=productoffers&hyperid=513&rids=213&pickup-options=raw' + unified_off_flags
        )
        self.assertFragmentIn(
            response_moscow,
            {
                "titles": {
                    "raw": "OFFER WITH PICKUP OPTIONS IN SHOP 123",
                },
                "delivery": {
                    "pickupOptions": [
                        {
                            "serviceId": 99,
                            "outlet": {
                                "id": "302",
                            },
                            "price": {"currency": "RUR", "value": "100"},
                            "dayFrom": 11,  # из данных оффера + 1
                            "dayTo": 21,
                            "orderBefore": 24,  # при "скачке" меняется на 24
                        },
                    ]
                },
            },
        )

        response_spb = self.report.request_json(
            'place=productoffers&hyperid=513&rids=2&pickup-options=raw' + unified_off_flags
        )
        self.assertFragmentIn(
            response_spb,
            {
                "titles": {
                    "raw": "OFFER WITH PICKUP OPTIONS IN SHOP 123",
                },
                "delivery": {
                    "pickupOptions": [
                        {
                            "serviceId": 301,
                            "outlet": {
                                "id": "303",
                            },
                            "price": {"currency": "RUR", "value": "700"},
                            "dayFrom": 5,  # из данных аутлета
                            "dayTo": 10,
                        },
                    ]
                },
            },
        )

    def test_pickup_available_if_offer_options_and_unknown_point(self):
        '''Теперь с учетом каникул'''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response_moscow = self.report.request_json(
            'place=productoffers&hyperid=514&rids=213&pickup-options=raw' + unified_off_flags
        )
        self.assertFragmentIn(
            response_moscow,
            {
                "titles": {
                    "raw": "OFFER WITH PICKUP OPTIONS IN SHOP 124",
                },
                "delivery": {
                    "pickupOptions": [
                        {
                            "outlet": {
                                "id": "304",
                            },
                            "price": {"currency": "RUR", "value": "100"},
                            "dayFrom": 14,  # из данных оффера + 1
                            "dayTo": 26,
                            "orderBefore": 24,  # при "скачке" меняется на 24
                        },
                    ]
                },
            },
        )

    def test_offer_with_self_pickup_options_kzt(self):
        '''Проверка параметра currency. Логика замены опций самовывоза остается прежней.'''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json(
            'place=productoffers&hyperid=513&rids=213&currency=KZT&pickup-options=raw' + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                "titles": {
                    "raw": "OFFER WITH PICKUP OPTIONS IN SHOP 123",
                },
                "delivery": {
                    "pickupOptions": [
                        {
                            "serviceId": 99,
                            "outlet": {
                                "id": "302",
                                "type": "pickup",
                            },
                            "price": {"currency": "KZT", "value": "20"},  # 100 * 0.2
                            "dayFrom": 11,
                            "dayTo": 21,
                        }
                    ],
                },
            },
        )

    def test_do_not_use_post_outlets(self):
        '''
        Что проверяем: оутлеты с типом POST не прикрепляются к оферу.
        Для магазина 115 заведен почтовый оутлет в 54 регионе, но его офер для модели 313 отбрасывается по доставке.
        '''
        response = self.report.request_json('place=prime&hyperid=313&rids=54&pickup-options=raw&debug=1')
        self.assertFragmentNotIn(response, {"entity": "offer"})

    def test_do_not_output_post_outlets(self):
        '''
        Что проверяем: что почтовые оутлеты не отдаются в place=outlets по списку служб доставки
        '''
        request = 'place=outlets&fesh=115&rids=54&deliveryServiceId=103,301'
        response = self.report.request_xml(request)
        self.assertFragmentNotIn(
            response,
            '''
        <outlet>
            <ServiceId>301</ServiceId>
        </outlet>
        ''',
        )

        # Но оутлеты других служб выводятся
        self.assertFragmentIn(
            response,
            '''
        <outlet>
            <ServiceId>103</ServiceId>
        </outlet>
        ''',
        )

    def test_show_post_outlets_by_id(self):
        '''
        Что проверяем: отображение почтовых оутлетов, если они были запрошены по идентификатору
        '''
        for market_color in ('', '&rgb=blue'):
            response = self.report.request_xml('place=outlets&outlets=300&rids=54' + market_color)
            self.assertFragmentIn(
                response,
                '''
            <outlet>
                <PointId>300</PointId>
                <ServiceId>301</ServiceId>
            </outlet>
            ''',
            )

    @classmethod
    def prepare_book_now_data(cls):
        '''Создаем по 2 аутлета всех типов в различных магазинах,
        создаем по 1 аутлету всех типов в одном магазине,
        создаем 2 аутлета из разных регионов в одном магазине.
        Привязываем аутлеты как book-now к офферам из магазинов.
        '''
        cls.index.shops += [
            Shop(fesh=116, priority_region=213),
            Shop(fesh=117, priority_region=213),
            Shop(fesh=118, priority_region=213),
            Shop(fesh=119, priority_region=213),
            Shop(fesh=120, priority_region=213),
        ]

        outlet_id = 219
        book_now_pickup_outlets = [
            Outlet(point_id=i, fesh=116, region=213, point_type=Outlet.FOR_PICKUP)
            for i in range(outlet_id, outlet_id + 2)
        ]
        outlet_id = 221
        book_now_mixed_outlets = [
            Outlet(point_id=i, fesh=117, region=213, point_type=Outlet.MIXED_TYPE)
            for i in range(outlet_id, outlet_id + 2)
        ]
        outlet_id = 223
        book_now_store_outlets = [
            Outlet(point_id=i, fesh=118, region=213, point_type=Outlet.FOR_STORE)
            for i in range(outlet_id, outlet_id + 2)
        ]
        outlet_id = 225
        book_now_any_outlets = [
            Outlet(point_id=i, fesh=119, region=213, point_type=t)
            for i, t in enumerate([Outlet.FOR_PICKUP, Outlet.MIXED_TYPE, Outlet.FOR_STORE], start=outlet_id)
        ]
        outlet_id = 228
        book_now_region_outlets = [
            Outlet(point_id=i, fesh=120, region=r, point_type=Outlet.FOR_STORE)
            for i, r in enumerate([52, 213], start=outlet_id)
        ]
        outlet_id = 230

        cls.index.outlets += (
            book_now_pickup_outlets
            + book_now_mixed_outlets
            + book_now_store_outlets
            + book_now_any_outlets
            + book_now_region_outlets
        )

        cls.index.models += [
            Model(hyperid=314),
            Model(hyperid=315),
            Model(hyperid=316),
            Model(hyperid=317),
            Model(hyperid=318),
        ]

        cls.index.offers += [
            Offer(
                fesh=116,
                hyperid=314,
                booking_availabilities=[
                    BookingAvailability(outlet_id=o.point_id, region_id=o.region_id, amount=5)
                    for o in book_now_pickup_outlets
                ],
            ),
            Offer(
                fesh=117,
                hyperid=315,
                booking_availabilities=[
                    BookingAvailability(outlet_id=o.point_id, region_id=o.region_id, amount=5)
                    for o in book_now_mixed_outlets
                ],
            ),
            Offer(
                fesh=118,
                hyperid=316,
                booking_availabilities=[
                    BookingAvailability(outlet_id=o.point_id, region_id=o.region_id, amount=5)
                    for o in book_now_store_outlets
                ],
            ),
            Offer(
                fesh=119,
                hyperid=317,
                title='book_now_amount_5',
                booking_availabilities=[
                    BookingAvailability(outlet_id=o.point_id, region_id=o.region_id, amount=5)
                    for o in book_now_any_outlets
                ],
            ),
            Offer(
                fesh=119,
                hyperid=317,
                title='book_now_amount_0',
                booking_availabilities=[
                    BookingAvailability(outlet_id=o.point_id, region_id=o.region_id, amount=0)
                    for o in book_now_any_outlets
                ],
            ),
            Offer(
                fesh=120,
                hyperid=318,
                booking_availabilities=[
                    BookingAvailability(outlet_id=o.point_id, region_id=o.region_id, amount=5)
                    for o in book_now_region_outlets
                ],
                pickup_buckets=[5028],
            ),
        ]

    def test_book_now_stores_count(self):
        '''place={prime, productoffers}
        Проверяем, что выводится нужное количество book_now-аутлетов:
         - 0 для pickup-аутлетов
         - 2 для mixed-аутлетов
         - 2 для store-аутлетов
         - 2 для всех типов аутлетов: pickup - нет, mixed и store - да (есть в наличии)
         - 0 для всех типов аутлетов (нет в наличии)
         - 1 для двух аутлетов из разных регионов (считается только аутлет из запрошенного региона)
        '''
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&hyperid=314&rids=213'.format(place))
            self.assertFragmentIn(response, {"shop": {"bookNowStoresCount": 0}})

            response = self.report.request_json('place={}&hyperid=315&rids=213'.format(place))
            self.assertFragmentIn(response, {"shop": {"bookNowStoresCount": 2}})

            response = self.report.request_json('place={}&hyperid=316&rids=213'.format(place))
            self.assertFragmentIn(response, {"shop": {"bookNowStoresCount": 2}})

            response = self.report.request_json('place={}&hyperid=317&rids=213'.format(place))
            self.assertFragmentIn(
                response,
                [
                    {
                        "shop": {"bookNowStoresCount": 2},
                        "titles": {"raw": "book_now_amount_5"},
                    },
                    {
                        "shop": {"bookNowStoresCount": 0},
                        "titles": {"raw": "book_now_amount_0"},
                    },
                ],
            )

            response = self.report.request_json('place={}&hyperid=318&rids=213'.format(place))
            self.assertFragmentIn(response, {"shop": {"bookNowStoresCount": 1}})

    def test_geo_shipping_urls(self):
        '''place={prime, productoffers}
        Проверяем, что при запросе урла geoShipping выводятся урлы
        pickupGeo, storeGeo, postomatGeo.
        Проверяем, что в этих урлах есть соответствующий offer-shipping.
        '''
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&hyperid=318&rids=213&show-urls=geoShipping'.format(place))
            self.assertFragmentIn(
                response,
                {
                    "urls": {
                        "pickupGeo": Contains("offer-shipping%3Dpickup"),
                        "storeGeo": Contains("offer-shipping%3Dstore"),
                        "postomatGeo": Contains("offer-shipping%3Dpostomat"),
                    }
                },
            )

    @classmethod
    def prepare_grouped_pickup_options_data(cls):
        '''Создаем 3 пары аутлетов (по опциям доставки)
        со всеми возможными комбинациями pickup, store и mixed.
        Создаем 10 аутлетов с различными параметрами
        стоимости, сроков, служб доставки.
        Привязываем доставку к офферам из магазина.
        '''

        cls.index.shops += [
            Shop(fesh=121, priority_region=213),
            Shop(fesh=122, priority_region=213),
        ]

        outlet_id = 230
        pickup_mixed_outlets = [
            Outlet(
                point_id=i,
                fesh=121,
                region=213,
                point_type=t,
                delivery_option=OutletDeliveryOption(
                    day_from=3,
                    day_to=5,
                    order_before=16,
                    price=500,
                    price_to=1000,
                ),
                working_days=[d for d in range(10)],
            )
            for i, t in enumerate([Outlet.FOR_PICKUP, Outlet.MIXED_TYPE], start=outlet_id)
        ]
        outlet_id = 232
        pickup_store_outlets = [
            Outlet(
                point_id=i,
                fesh=121,
                region=213,
                point_type=t,
                delivery_option=OutletDeliveryOption(
                    day_from=1,
                    day_to=5,
                    order_before=16,
                    price=500,
                    price_to=1000,
                ),
                working_days=[d for d in range(10)],
            )
            for i, t in enumerate([Outlet.FOR_PICKUP, Outlet.FOR_STORE], start=outlet_id)
        ]
        outlet_id = 234
        mixed_store_outlets = [
            Outlet(
                point_id=i,
                fesh=121,
                region=213,
                point_type=t,
                delivery_option=OutletDeliveryOption(
                    day_from=3,
                    day_to=5,
                    order_before=20,
                    price=500,
                    price_to=1000,
                ),
                working_days=[d for d in range(10)],
            )
            for i, t in enumerate([Outlet.MIXED_TYPE, Outlet.FOR_STORE], start=outlet_id)
        ]
        outlet_id = 236

        cls.index.outlets += pickup_mixed_outlets + pickup_store_outlets + mixed_store_outlets

        cls.index.outlets += [
            Outlet(
                point_id=245,
                fesh=122,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=3,
                    day_to=5,
                    order_before=16,
                    price=500,
                    shipper_id=101,
                ),
                working_days=[d for d in range(10)],
            ),
            Outlet(
                point_id=244,
                fesh=122,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=3,
                    day_to=5,
                    order_before=16,
                    price=500,
                    shipper_id=100,
                ),
                working_days=[d for d in range(10)],
            ),
            Outlet(
                point_id=243,
                fesh=122,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=3,
                    day_to=5,
                    order_before=16,
                    price=500,
                    shipper_id=98,
                ),
                working_days=[d for d in range(10)],
            ),
            Outlet(
                point_id=242,
                fesh=122,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=3,
                    day_to=5,
                    order_before=16,
                    price=500,
                    shipper_id=98,
                ),
                working_days=[d for d in range(10)],
            ),
            Outlet(
                point_id=241,
                fesh=122,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=3,
                    day_to=5,
                    order_before=16,
                    price=500,
                ),
                working_days=[d for d in range(10)],
            ),
            Outlet(
                point_id=240,
                fesh=122,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=3,
                    day_to=5,
                    order_before=16,
                    price=500,
                ),
                working_days=[d for d in range(10)],
            ),
            Outlet(
                point_id=239,
                fesh=122,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=3,
                    day_to=5,
                    order_before=20,
                    price=500,
                ),
                working_days=[d for d in range(10)],
            ),
            Outlet(
                point_id=238,
                fesh=122,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=3,
                    day_to=4,
                    order_before=16,
                    price=500,
                ),
                working_days=[d for d in range(10)],
            ),
            Outlet(
                point_id=237,
                fesh=122,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=1,
                    day_to=5,
                    order_before=16,
                    price=500,
                ),
                working_days=[d for d in range(10)],
            ),
            Outlet(
                point_id=236,
                fesh=122,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=3,
                    day_to=5,
                    order_before=16,
                    price=0,
                ),
                working_days=[d for d in range(10)],
            ),
        ]

        cls.index.models += [
            Model(hyperid=319),
            Model(hyperid=320),
        ]

        cls.index.offers += [
            Offer(fesh=121, hyperid=319, pickup_buckets=[5021]),
            Offer(fesh=122, hyperid=320, pickup_buckets=[5022]),
        ]

    def test_grouped_pickup_options_format(self):
        '''place={prime, productoffers}
        Проверяем, что с флагом pickup-options=grouped "pickupOptions"
        содержат агрегированные по цене и срокам аутлеты
        типа pickup и mixed (store опускаются).
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json(
                'place={}&hyperid=319&rids=213&pickup-options=grouped'.format(place) + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "pickupOptions": [
                            {
                                "serviceId": 99,
                                "isMarketBranded": NoKey("isMarketBranded"),
                                "outlet": NoKey("outlet"),
                                "type": NoKey("type"),
                                "price": {"currency": "RUR", "value": "500"},
                                "dayFrom": 1,
                                "dayTo": 5,
                                "orderBefore": 16,
                                "groupCount": 1,
                            },
                            {
                                "serviceId": 99,
                                "isMarketBranded": NoKey("isMarketBranded"),
                                "outlet": NoKey("outlet"),
                                "type": NoKey("type"),
                                "price": {"currency": "RUR", "value": "500"},
                                "dayFrom": 3,
                                "dayTo": 5,
                                "orderBefore": 20,
                                "groupCount": 1,
                            },
                            {
                                "serviceId": 99,
                                "isMarketBranded": NoKey("isMarketBranded"),
                                "outlet": NoKey("outlet"),
                                "type": NoKey("type"),
                                "price": {"currency": "RUR", "value": "500"},
                                "dayFrom": 3,
                                "dayTo": 5,
                                "orderBefore": 16,
                                "groupCount": 2,
                            },
                        ],
                    },
                },
                preserve_order=True,
            )

    def test_grouped_pickup_options_order(self):
        '''place={prime, productoffers}
        Проверяем, что с флагом pickup-options=grouped
        агрегированные опции в "pickupOptions" сортируются по:
        (price, dayFrom, dayTo, orderBefore, count).
        При одинаковых полях для разных сервисов первой идет 99 сервис,
        затем по возрастанию serviceId.
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json(
                'place={}&hyperid=320&rids=213&pickup-options=grouped'.format(place) + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "pickupOptions": [
                            {
                                "serviceId": 99,
                                "isMarketBranded": NoKey("isMarketBranded"),
                                "outlet": NoKey("outlet"),
                                "type": NoKey("type"),
                                "price": {"currency": "RUR", "value": "0"},
                                "dayFrom": 3,
                                "dayTo": 5,
                                "orderBefore": 16,
                                "groupCount": 1,
                            },
                            {
                                "serviceId": 99,
                                "isMarketBranded": NoKey("isMarketBranded"),
                                "outlet": NoKey("outlet"),
                                "type": NoKey("type"),
                                "price": {"currency": "RUR", "value": "500"},
                                "dayFrom": 1,
                                "dayTo": 5,
                                "orderBefore": 16,
                                "groupCount": 1,
                            },
                            {
                                "serviceId": 99,
                                "isMarketBranded": NoKey("isMarketBranded"),
                                "outlet": NoKey("outlet"),
                                "type": NoKey("type"),
                                "price": {"currency": "RUR", "value": "500"},
                                "dayFrom": 3,
                                "dayTo": 4,
                                "orderBefore": 16,
                                "groupCount": 1,
                            },
                            {
                                "serviceId": 99,
                                "isMarketBranded": NoKey("isMarketBranded"),
                                "outlet": NoKey("outlet"),
                                "type": NoKey("type"),
                                "price": {"currency": "RUR", "value": "500"},
                                "dayFrom": 3,
                                "dayTo": 5,
                                "orderBefore": 20,
                                "groupCount": 1,
                            },
                            {
                                "serviceId": 99,
                                "isMarketBranded": NoKey("isMarketBranded"),
                                "outlet": NoKey("outlet"),
                                "type": NoKey("type"),
                                "price": {"currency": "RUR", "value": "500"},
                                "dayFrom": 3,
                                "dayTo": 5,
                                "orderBefore": 16,
                                "groupCount": 2,
                            },
                            {
                                "serviceId": 98,
                                "isMarketBranded": NoKey("isMarketBranded"),
                                "outlet": NoKey("outlet"),
                                "type": NoKey("type"),
                                "price": {"currency": "RUR", "value": "500"},
                                "dayFrom": 3,
                                "dayTo": 5,
                                "orderBefore": 16,
                                "groupCount": 2,
                            },
                            {
                                "serviceId": 100,
                                "isMarketBranded": NoKey("isMarketBranded"),
                                "outlet": NoKey("outlet"),
                                "type": NoKey("type"),
                                "price": {"currency": "RUR", "value": "500"},
                                "dayFrom": 3,
                                "dayTo": 5,
                                "orderBefore": 16,
                                "groupCount": 1,
                            },
                            {
                                "serviceId": 101,
                                "isMarketBranded": NoKey("isMarketBranded"),
                                "outlet": NoKey("outlet"),
                                "type": NoKey("type"),
                                "price": {"currency": "RUR", "value": "500"},
                                "dayFrom": 3,
                                "dayTo": 5,
                                "orderBefore": 16,
                                "groupCount": 1,
                            },
                        ],
                    },
                },
                preserve_order=True,
            )

    @classmethod
    def prepare_best_pickup_option(cls):
        cls.index.models += [Model(hyperid=321)]

        cls.index.offers += [Offer(fesh=125, hyperid=321, pickup_buckets=[5032])]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5032,
                carriers=[192],
                options=[
                    PickupOption(outlet_id=262, day_from=2, day_to=2, price=800),
                    PickupOption(outlet_id=263, day_from=1, day_to=3, price=600),
                    PickupOption(outlet_id=264, day_from=0, day_to=2, price=700),
                    PickupOption(outlet_id=265, day_from=2, day_to=2, price=600),
                    PickupOption(outlet_id=266, day_from=3, day_to=4, price=900),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            )
        ]

        cls.index.outlets += [
            Outlet(
                point_id=262 + i,
                fesh=125,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                working_days=[d for d in range(30)],
                delivery_option=OutletDeliveryOption(day_from=1, day_to=1, order_before=16, price=500, shipper_id=101),
            )
            for i in range(5)
        ]

    def test_best_pickup_option(self):
        '''place={prime, productoffers}
        Проверяем, что с флагом pickup-options=best "pickupOptions"
        содержат единственный лучшый по цене и срокам аутлет
        '''

        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&hyperid=321&rids=213&pickup-options=best'.format(place))
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "pickupOptions": [{"price": {"currency": "RUR", "value": "600"}, "dayFrom": 2, "dayTo": 2}]
                    }
                },
                allow_different_len=False,
            )

    @classmethod
    def prepare_russian_post_delivery_data(cls):
        '''Создаем RUSSIAN-POST-аутлет с данными о доставке, привязываем его к магазину и оферу.'''
        cls.index.shops += [
            Shop(fesh=131, priority_region=213, delivery_service_outlets=[251], cpa=Shop.CPA_REAL),
            Shop(fesh=133, priority_region=213, delivery_service_outlets=[251], cpa=Shop.CPA_REAL),
            Shop(fesh=134, priority_region=213, delivery_service_outlets=[251], cpa=Shop.CPA_REAL),
            Shop(fesh=137, priority_region=213, delivery_service_outlets=[251], cpa=Shop.CPA_REAL),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=251,
                delivery_service_id=123,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=123,
                    day_from=1,
                    day_to=1,
                    order_before=20,
                    work_in_holiday=True,
                    price=600,
                ),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.1, 55.5),
                bool_props=["prop_2", "C", "prepayAllowed", "cashAllowed", "cardAllowed", "isMarketBranded"],
                post_code=123789,
                storage_period=12,
            ),
            Outlet(
                point_id=252252,
                delivery_service_id=123,
                region=213,
                point_type=Outlet.MIXED_TYPE,
                delivery_option=OutletDeliveryOption(
                    shipper_id=123,
                    day_from=1,
                    day_to=1,
                    order_before=20,
                    work_in_holiday=True,
                    price=600,
                ),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.1, 55.5),
                bool_props=["prop_2", "C", "prepayAllowed", "cashAllowed", "cardAllowed", "isMarketBranded"],
                post_code=123789,
                storage_period=12,
            ),
            Outlet(point_id=252, fesh=132, region=213, point_type=Outlet.FOR_PICKUP),
            Outlet(point_id=257, fesh=137, region=213, point_type=Outlet.FOR_PICKUP, gps_coord=GpsCoord(37.2, 55.5)),
            # Для красных магазинов
            Outlet(
                point_id=900251,
                delivery_service_id=123,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=123,
                    day_from=1,
                    day_to=1,
                    order_before=20,
                    work_in_holiday=True,
                    price=600,
                ),
                working_days=list(range(10)),
                gps_coord=GpsCoord(37.12, 55.49),
                bool_props=["prop_2", "C", "prepayAllowed", "cashAllowed", "cardAllowed"],
            ),
            Outlet(point_id=900252, fesh=900132, region=213, point_type=Outlet.FOR_PICKUP),
        ]

        cls.index.shipment_service_calendars += [
            DeliveryCalendar(
                fesh=131, calendar_id=123, date_switch_hour=20, holidays=[0, 1, 2, 3, 4, 5, 6, 14, 20, 21, 27, 28]
            ),
            DeliveryCalendar(
                fesh=133, calendar_id=123, date_switch_hour=2, holidays=[0, 1, 2, 3, 4, 5, 6, 14, 20, 21, 27, 28]
            ),
            DeliveryCalendar(fesh=134, calendar_id=123, date_switch_hour=2, holidays=[3, 4, 5, 6, 14, 20, 21, 27, 28]),
            DeliveryCalendar(
                fesh=137, calendar_id=123, date_switch_hour=20, holidays=[1, 2, 3, 4, 5, 6, 14, 20, 21, 27, 28]
            ),
        ]

        cls.index.models += [
            Model(hyperid=401),
        ]

        cls.index.offers += [
            Offer(fesh=131, hyperid=401, cpa=Offer.CPA_REAL, pickup_buckets=[5023]),
            Offer(fesh=132, hyperid=402, pickup_buckets=[5024]),
            Offer(fesh=133, hyperid=403, cpa=Offer.CPA_REAL, pickup_buckets=[5023]),
            Offer(fesh=134, hyperid=404, cpa=Offer.CPA_REAL, pickup_buckets=[5023]),
            Offer(
                fesh=137, hyperid=407, waremd5='jrS0wBvzN1goxUlML252hQ', cpa=Offer.CPA_REAL, pickup_buckets=[5023, 5025]
            ),
            Offer(
                fesh=137,
                hyperid=408,
                waremd5='l2saljLVb-gHEo8Ukl3maw',
                available=False,
                cpa=Offer.CPA_REAL,
                pickup_buckets=[5023, 5025],
            )
            # shops 131, 133, 134, 137 refer to russian_post outlets (serviceId = 123)
        ]

    def test_russian_post_delivery(self):
        '''place={geo}
        Проверяем, что на выдаче есть аутлет почты России.
        Дополнительно проверяется наличие поля serviceId для аутлетов почты России и собственных аутлетов.
        '''
        """add_param == '': проверка наличия аутлета почты России и его соответствующих полей"""
        """add_param == '&touch=1': в выдаче для тача в geo должен быть аутлет почты России как и для add_param == '' """
        for add_param in ['', '&touch=1']:
            response = self.report.request_json('place=geo&hyperid=401&rids=213' + add_param)
            #  [0-6] - holydays (7 days) + 1 day delivery
            self.assertFragmentIn(
                response,
                {
                    "shop": {
                        "entity": "shop",
                        "id": 131,
                        "outletsCount": 1,
                        "pickupStoresCount": 1,
                        "postomatStoresCount": 0,
                    },
                    "outlet": {
                        "id": "251",
                        "serviceId": 123,
                        "selfDeliveryRule": {
                            "shipmentDay": 7,
                            "shipmentBySupplier": Absent(),
                            "receptionByWarehouse": Absent(),
                            "dayFrom": 8,
                            "dayTo": 8,
                        },
                    },
                },
            )

            # check when &tile is set
            request = "place={0}&tile=617,322&zoom=10&hyperid=407&show-outlet=tiles&rids=213" + add_param

            response = self.report.request_json(request.format("geo"))
            self.assertFragmentIn(
                response,
                {
                    "entity": "tile",
                    "coord": {"x": 617, "y": 322, "zoom": 10},
                    "outlets": [
                        {
                            "entity": "outlet",
                            "id": "251",
                            "type": "pickup",
                            "gpsCoord": {"longitude": "37.1", "latitude": "55.5"},
                        },
                        {
                            "entity": "outlet",
                            "id": "257",
                            "type": "pickup",
                            "gpsCoord": {"longitude": "37.2", "latitude": "55.5"},
                        },
                    ],
                },
            )

            """Проверяется, что для &place=geo с заданными &geo_bounds и &touch=1 появляется аутлет почты России"""
            response = self.report.request_json(
                'place=geo&tile=617,322&zoom=10&hyperid=401&show-outlet=tiles&rids=213&pp=18&geo_bounds_rt=100,100&geo_bounds_lb=0,0'
                + add_param
            )
            self.assertFragmentIn(
                response,
                {
                    "entity": "tile",
                    "coord": {"x": 617, "y": 322, "zoom": 10},
                    "outlets": [
                        {
                            "entity": "outlet",
                            "id": "251",
                            "type": "pickup",
                            "gpsCoord": {"longitude": "37.1", "latitude": "55.5"},
                        }
                    ],
                },
            )

        response = self.report.request_json('place=geo&hyperid=401&rids=213')
        self.assertFragmentIn(response, {"outlet": {"id": "251", "serviceId": 123}})
        response = self.report.request_json('place=geo&hyperid=402&rids=213')
        self.assertFragmentIn(response, {"outlet": {"id": "252", "serviceId": 99}})

        # Белый
        response = self.report.request_xml('place=outlets&rids=213&outlets=251&fesh=131')
        self.assertFragmentIn(
            response,
            '''
            <outlet>
                <PointId>251</PointId>
                <SelfDeliveryRule>
                    <CalcInletShipmentDays>7</CalcInletShipmentDays>
                </SelfDeliveryRule>
                <ServiceId>123</ServiceId>
            </outlet>
            ''',
        )
        response = self.report.request_xml('place=outlets&rids=213&outlets=252&fesh=132')
        self.assertFragmentIn(
            response,
            '''
            <outlet>
                <PointId>252</PointId>
                <ServiceId>99</ServiceId>
            </outlet>
            ''',
        )

        """Проверяется, что serviceName=Russian Post появляется в availableServices и pickupOptions"""
        response = self.report.request_json('place=geo&hyperid=401&rids=213&pickup-options=grouped')
        self.assertFragmentIn(
            response, {"delivery": {"availableServices": [{"serviceId": 123}], "pickupOptions": [{"serviceId": 123}]}}
        )

        """Проверяем, что час перескока правильно работает, если текущий день выходной/рабочий"""
        """    Текущий день -- выходной. Эквивалентно тому, что заказ был сделан в 00:00 в первый рабочий день после текущего выходного дня."""
        response = self.report.request_json('place=geo&hyperid=403&rids=213')
        self.assertFragmentIn(
            response,
            {
                "shop": {"entity": "shop", "id": 133},
                "outlet": {
                    "id": "251",
                    "serviceId": 123,
                    "selfDeliveryRule": {
                        "shipmentDay": 7,
                        "shipmentBySupplier": Absent(),
                        "receptionByWarehouse": Absent(),
                        "dayFrom": 8,
                        "dayTo": 8,
                    },
                },
            },
        )
        """    Текущий день -- рабочий. Заказ после часа перескока эквивалентен заказу на следующий день до часа перескока."""
        response = self.report.request_json('place=geo&hyperid=404&rids=213')
        self.assertFragmentIn(
            response,
            {
                "shop": {"entity": "shop", "id": 134},
                "outlet": {
                    "id": "251",
                    "serviceId": 123,
                    "selfDeliveryRule": {
                        "shipmentDay": 1,
                        "shipmentBySupplier": Absent(),
                        "receptionByWarehouse": Absent(),
                        "dayFrom": 2,
                        "dayTo": 2,
                    },
                },
            },
        )

        """Проверяется, что день выдачи товара пользователю вычисляется но календарю аутлета, а не по календарю инлета"""
        """    Календарь инлета: сегодня -- последний рабочий день перед выходным, до часа перскока"""
        """    Календарь аутлута: сегодня и завтра -- рабочие дни"""
        """        В итоге dayFrom == dayTo == 1, но не 7, т.к. это посчитано по календарю аутлета"""
        response = self.report.request_json('place=geo&hyperid=407&rids=213')
        self.assertFragmentIn(
            response,
            {
                "shop": {"entity": "shop", "id": 137},
                "outlet": {
                    "id": "251",
                    "serviceId": 123,
                    "selfDeliveryRule": {
                        "shipmentDay": 0,
                        "shipmentBySupplier": Absent(),
                        "receptionByWarehouse": Absent(),
                        "dayFrom": 1,
                        "dayTo": 1,
                    },
                },
            },
        )

        """Проверяется &inlet-shipment-day, устанавливающий новую дату отгрузки (может выпадать на выходной день магазина, но не инлета)."""
        """Обратная ситуация (магазин -- рабочий), (инлет -- выходной) отсекается чекаутером."""
        """Раздельно эти кейсы проверить нельзя, т.к. shops_delivery_service_calendar.xml содержит смерженный календарь магазина и инлета."""
        for shipment_day in [2, 3]:
            # place=geo
            response = self.report.request_json(
                ('place=geo&hyperid=404&rids=213&inlet-shipment-day={0}').format(shipment_day)
            )
            self.assertFragmentIn(
                response,
                {
                    "shop": {"entity": "shop", "id": 134},
                    "outlet": {
                        "id": "251",
                        "serviceId": 123,
                        "selfDeliveryRule": {
                            "shipmentDay": shipment_day,
                            "shipmentBySupplier": Absent(),
                            "receptionByWarehouse": Absent(),
                            "dayFrom": shipment_day + 1,
                            "dayTo": shipment_day + 1,
                        },
                    },
                },
            )

            outlets_response = '''
                <outlet>
                    <PointId>{2}</PointId>
                    <SelfDeliveryRule>
                        <CalcInletShipmentDays>{0}</CalcInletShipmentDays>
                        <CalcMinDeliveryDays>{1}</CalcMinDeliveryDays>
                        <CalcMaxDeliveryDays>{1}</CalcMaxDeliveryDays>
                    </SelfDeliveryRule>
                    <ServiceId>123</ServiceId>
                </outlet>
            '''
            # place=outlets, white
            request = 'place=outlets&rids=213&outlets=251&fesh=134&inlet-shipment-day={}'.format(shipment_day)
            response = self.report.request_xml(request)
            self.assertFragmentIn(response, outlets_response.format(shipment_day, shipment_day + 1, 251))

        """Проверяется значение post-available для:"""
        for i in [['jrS0wBvzN1goxUlML252hQ', True], ['l2saljLVb-gHEo8Ukl3maw', False]]:
            """offerinfo"""
            response = self.report.request_json('regset=1&show-urls=encrypted&place=offerinfo&rids=213&offerid=' + i[0])
            self.assertFragmentIn(response, {"postAvailable": i[1]})
            """prime"""
            response = self.report.request_json('regset=1&show-urls=encrypted&place=prime&rids=213&offerid=' + i[0])
            self.assertFragmentIn(response, {"postAvailable": i[1]})

    @classmethod
    def prepare_outlets_with_coord(cls):
        cls.index.shops += [
            Shop(fesh=1101, priority_region=1, cpa=Shop.CPA_REAL),
            Shop(fesh=1102, priority_region=1, cpa=Shop.CPA_REAL),
            Shop(fesh=1103, priority_region=1, cpa=Shop.CPA_REAL),
            Shop(fesh=1104, priority_region=1, cpa=Shop.CPA_REAL),
        ]

        # Аутлеты на "карте"
        # Числа в скобках - координаты тайлов при zoom = 10
        #           37.0(617)     37.2(617)       37.4(618)      37.6(618)       37.8(619)
        # 55.8(321) |--------------|---------------|--------------|---------------
        #           |              |               |              |
        #           |              |               |              |
        #           |              |               |              |
        #           |              |               |              |
        # 55.6(322) |--------------|---------------|--------------|---------------
        #           |              |               |              |
        #           |     *(1221)  |    *(1222)    |*Home         |
        #           |              |               |              |
        #           |              |               |*(1241)       |
        # 55.4(323) |--------------|---------------|--------------|---------------
        #           |              |               |              |
        #           |     *(1232)  |      *(1233)  |              |
        #           |    *(1212)   |    *(1213)    |              |
        #           |              |               |              |
        # 55.2(324) |--------------|---------------|--------------|---------------
        #           |              |               |              |
        #           |     *(1231)  |     *Work     |              |
        #           |              |    *(1214)    |              |
        #           |  *(1211)     |               |              |
        # 55.0(325) |--------------|---------------|--------------|---------------

        cls.index.outlets += [
            Outlet(
                point_id=1211,
                fesh=1101,
                region=1,
                gps_coord=GpsCoord(37.1, 55.1),
                point_type=Outlet.FOR_STORE,
                delivery_option=OutletDeliveryOption(price=100),
            ),
            Outlet(
                point_id=1212,
                fesh=1101,
                region=1,
                gps_coord=GpsCoord(37.1, 55.3),
                delivery_option=OutletDeliveryOption(price=200),
            ),
            Outlet(
                point_id=1213,
                fesh=1101,
                region=1,
                gps_coord=GpsCoord(37.3, 55.3),
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(price=300),
            ),
            Outlet(
                point_id=1214,
                fesh=1101,
                region=1,
                gps_coord=GpsCoord(37.3, 55.1),
                point_type=Outlet.FOR_STORE,
                delivery_option=OutletDeliveryOption(price=400),
            ),
            Outlet(point_id=1221, fesh=1102, region=1, gps_coord=GpsCoord(37.1, 55.5), point_type=Outlet.FOR_STORE),
            Outlet(point_id=1222, fesh=1102, region=1, gps_coord=GpsCoord(37.3, 55.5), point_type=Outlet.FOR_PICKUP),
            Outlet(point_id=1231, fesh=1103, region=1, gps_coord=GpsCoord(37.12, 55.12), point_type=Outlet.FOR_STORE),
            Outlet(point_id=1232, fesh=1103, region=1, gps_coord=GpsCoord(37.12, 55.32), point_type=Outlet.FOR_PICKUP),
            Outlet(point_id=1233, fesh=1103, region=1, gps_coord=GpsCoord(37.32, 55.32)),
            Outlet(point_id=1241, fesh=1104, region=1, gps_coord=GpsCoord(37.42, 55.42)),
        ]

    @classmethod
    def prepare_attraction_points(cls):
        '''
        Подготавливаем данные для тестирования точек притяжения (адрес жительства и места работы)
        '''
        home_address = DataSyncYandexUserAddress(address_id='home', gps_coord=HOME_GPS_COORD)
        work_address = DataSyncYandexUserAddress(address_id='work', gps_coord=WORK_GPS_COORD)
        cls.datasync.on_request_yandex_user_address(12).respond([work_address, home_address])

    def test_ranking_place_outlets_with_attraction_points(self):
        outletsOrder = [
            # Эти две точки попали в область притяжения
            1222,  # Дом
            1214,  # Работа
            # Остальные точки идут в исходном порядке
            1211,
            1212,
            1213,
            1221,
            1231,
            1232,
            1233,
            1241,
        ]

        responseTemplate = """
            <outlets>
                {}
            </outlets>
        """

        outlets = ""
        for i in range(len(outletsOrder)):
            outlets += "<outlet><Rank>{}</Rank><PointId>{}</PointId></outlet>".format(i, outletsOrder[i])

        outlets_wo_attr_points = ""
        outletsOrderWoAttrPoints = sorted(outletsOrder)
        for i in range(len(outletsOrderWoAttrPoints)):
            outlets_wo_attr_points += "<outlet><Rank>{}</Rank><PointId>{}</PointId></outlet>".format(
                i, outletsOrderWoAttrPoints[i]
            )

        for market_color in ('', '&rgb=blue'):
            response = self.report.request_xml(
                'place=outlets&rids=1&outlets=1211,1212,1213,1214,1221,1222,1231,1232,1233,1241&puid=12&geo-attraction-distance=7000'
                + market_color
                + '&rearr-factors=market_outlets_disable_attraction_points=0'
            )
            self.assertFragmentIn(response, responseTemplate.format(outlets), preserve_order=True)
            response = self.report.request_xml(
                'place=outlets&rids=1&outlets=1211,1212,1213,1214,1221,1222,1231,1232,1233,1241&puid=12&geo-attraction-distance=7000'
                + market_color
                + '&rearr-factors=market_outlets_disable_attraction_points=1'
            )
            self.assertFragmentIn(response, responseTemplate.format(outlets_wo_attr_points), preserve_order=True)

    @classmethod
    def prepare_outlets_region(cls):
        cls.index.outlets += [
            Outlet(
                point_id=2220101,
                fesh=22201,
                region=213,
                point_type=Outlet.FOR_STORE,
                delivery_option=OutletDeliveryOption(price=100),
            ),
            Outlet(
                point_id=2220102,
                fesh=22201,
                region=1234,
                point_type=Outlet.FOR_STORE,
                delivery_option=OutletDeliveryOption(price=100),
            ),
        ]

    def test_outlets_region(self):
        all_outlets = '''
                <outlets>
                    <outlet>
                        <PointId>1211</PointId>
                    </outlet>
                    <outlet>
                        <PointId>2220101</PointId>
                    </outlet>
                    <outlet>
                        <PointId>2220102</PointId>
                    </outlet>
                </outlets>
        '''
        """Проверяется, что &rids фильтрует аутлеты для place=outlets, выбирая parent region для &rids"""
        for market_color in ('', '&rgb=blue'):
            request = 'place=outlets&outlets=1211,2220101,2220102' + market_color
            response = self.report.request_xml(request)
            """Показываются все аутлеты, т.к. регион не задан"""
            self.assertFragmentIn(response, all_outlets)

            """Показываются все аутлеты, т.к. parent_region=225, и туда входят регионы 213, 1234, 1"""
            response = self.report.request_xml(request + '&rids=1')
            self.assertFragmentIn(response, all_outlets)

            response = self.report.request_xml(request + '&rids=213')
            self.assertFragmentIn(response, all_outlets)

            """Показываются только аутлеты из родительского региона 213"""
            response = self.report.request_xml(request + '&rids=1234')
            self.assertFragmentIn(
                response,
                '''
                    <outlets>
                        <outlet>
                            <PointId>2220101</PointId>
                        </outlet>
                        <outlet>
                            <PointId>2220102</PointId>
                        </outlet>
                    </outlets>
            ''',
            )
            self.assertEqual(2, response.count("<outlet>"))

    @classmethod
    def prepare_outlets_with_long_delivery(cls):
        """Создаем два магазина: один - белый с 2-мя ПВЗ, доставка в один 60 дней ровно, доставка дольше 60 в другой
        Второй - красный, с одним ПВЗ и очень долгой доставкой
        """
        cls.index.shops += [
            Shop(fesh=1563101, priority_region=213, cpa=Shop.CPA_REAL),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=156310101,
                fesh=1563101,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=59,
                    day_to=60,
                    order_before=20,
                    work_in_holiday=True,
                    price=600,
                ),
                working_days=[i for i in range(70)],
                gps_coord=GpsCoord(37.1, 55.5),
            ),
            Outlet(
                point_id=156310102,
                fesh=1563101,
                region=2,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=60,
                    day_to=61,
                    order_before=20,
                    work_in_holiday=True,
                    price=600,
                ),
                working_days=[i for i in range(70)],
                gps_coord=GpsCoord(37.1, 75.5),
            ),
            Outlet(
                point_id=156310103,
                fesh=1563102,
                region=2,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=61,
                    day_to=70,
                    order_before=20,
                    work_in_holiday=True,
                    price=600,
                ),
                working_days=[i for i in range(90)],
                gps_coord=GpsCoord(37.1, 75.5),
            ),
        ]

        cls.index.offers += [
            Offer(fesh=1563101, hyperid=1563111, pickup_buckets=[5026]),
            Offer(fesh=1563102, hyperid=1563111, pickup_buckets=[5029]),
        ]

    def test_outlets_with_long_delivery(self):
        """Что тестируем: на белом сроки самовывоза до 60 дней включительно отображаются,
        а большие 60 дней скрываются ("на заказ" на фронте)
        """
        response = self.report.request_json('place=geo&fesh=1563101&rids=0')
        self.assertFragmentIn(
            response,
            [
                {"outlet": {"id": "156310101", "selfDeliveryRule": {"dayFrom": 59, "dayTo": 60, "orderBefore": 20}}},
                {
                    "outlet": {
                        "id": "156310102",
                        "selfDeliveryRule": {"dayFrom": Absent(), "dayTo": Absent(), "orderBefore": Absent()},
                    }
                },
            ],
        )

    def test_minify_outlets(self):
        request = 'place=outlets&rids=213&outlets=251&fesh=131'
        minify = '&minify-outlets=1'

        """Проверяется, что при заданном &minify-outlets=1 в выдаче остаются нужные поля, а ненужные пропадают."""
        response = self.report.request_xml(request + minify)
        self.assertFragmentIn(
            response,
            '''
            <outlet>
              <Rank>0</Rank>
              <PointId>251</PointId>
              <RegionId>213</RegionId>
              <ServiceId>123</ServiceId>
              <Purpose>pickup</Purpose>
              <SelfDeliveryRule>
                <CalcCurrency>RUR</CalcCurrency>
                <CalcCost>600</CalcCost>
                <CalcInletShipmentDays>7</CalcInletShipmentDays>
                <CalcMinDeliveryDays>8</CalcMinDeliveryDays>
                <CalcMaxDeliveryDays>8</CalcMaxDeliveryDays>
              </SelfDeliveryRule>
              <BooleanProperties>
                <cardAllowed description="property cardAllowed"/>
                <cashAllowed description="property cashAllowed"/>
                <prepayAllowed description="property prepayAllowed"/>
              </BooleanProperties>
            </outlet>
        ''',
            allow_different_len=False,
        )

        """То же самое, что и выше, но с mixed типом (поле purpose будет добавлено дважды)"""
        request_2 = 'place=outlets&rids=213&outlets=252252&fesh=131'
        response = self.report.request_xml(request_2 + minify)
        self.assertFragmentIn(
            response,
            '''
            <outlet>
              <Rank>0</Rank>
              <PointId>252252</PointId>
              <RegionId>213</RegionId>
              <ServiceId>123</ServiceId>
              <Purpose>pickup</Purpose>
              <Purpose>store</Purpose>
              <SelfDeliveryRule>
                <CalcCurrency>RUR</CalcCurrency>
                <CalcCost>600</CalcCost>
                <CalcInletShipmentDays>7</CalcInletShipmentDays>
                <CalcMinDeliveryDays>8</CalcMinDeliveryDays>
                <CalcMaxDeliveryDays>8</CalcMaxDeliveryDays>
              </SelfDeliveryRule>
              <BooleanProperties>
                <cardAllowed description="property cardAllowed"/>
                <cashAllowed description="property cashAllowed"/>
                <prepayAllowed description="property prepayAllowed"/>
              </BooleanProperties>
            </outlet>
        ''',
            allow_different_len=False,
        )

        """Проверяется возмоность вывода в вормате json для place=outlets"""
        response = self.report.request_json(
            request + minify + '&bsformat=2&rearr-factors=market_blue_show_storage_period=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "outlet",
                        "id": "251",
                        "name": "OUTLET-123-251",
                        "rank": 0,
                        "purpose": ["pickup"],
                        "gpsCoord": {"longitude": "37.1", "latitude": "55.5"},
                        "daily": True,
                        "around-the-clock": True,
                        "postCode": 123789,
                        "storagePeriod": 12,
                        "isMarketBranded": True,
                        "isMarketPartner": False,
                        "isMarketPostTerm": False,
                        # Поля, которых нет в коротком отображении
                        "type": Absent(),
                        "BooleanProperties": Absent(),
                        "serviceId": Absent(),
                        "email": Absent(),
                        "shop": Absent(),
                        "address": Absent(),
                        "workingTime": Absent(),
                        "workingDay": Absent(),
                        "selfDeliveryRule": Absent(),
                        "region": Absent(),
                    }
                ]
            },
        )

    def test_outlets_json(self):
        '''
        Проверяем полный формат ответа в json
        '''
        request = 'place=outlets&rids=213&outlets=251&fesh=131'
        response = self.report.request_json(request + '&bsformat=2')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "outlet",
                        "id": "251",
                        "name": "OUTLET-123-251",
                        "rank": 0,
                        "type": "pickup",
                        "purpose": ["pickup"],
                        "BooleanProperties": [
                            {"name": "C", "description": "property C"},
                            {"name": "cardAllowed", "description": "property cardAllowed"},
                            {"name": "cashAllowed", "description": "property cashAllowed"},
                            {"name": "prepayAllowed", "description": "property prepayAllowed"},
                            {"name": "prop_2", "description": "property prop_2"},
                        ],
                        "serviceId": 123,
                        "email": "",
                        "isMarketBranded": True,
                        "isMarketPartner": False,
                        "isMarketPostTerm": False,
                        "shop": {"id": 0},
                        "address": {
                            "fullAddress": "",
                            "country": "",
                            "region": "",
                            "locality": "",
                            "street": "",
                            "km": "",
                            "building": "",
                            "block": "",
                            "wing": "",
                            "estate": "",
                            "entrance": "",
                            "floor": "",
                            "room": "",
                            "office_number": "",
                            "note": "",
                        },
                        "workingTime": [
                            {"daysFrom": "7", "daysTo": "7", "hoursFrom": "00:00", "hoursTo": "24:00"},
                        ],
                        "workingDay": [
                            {"date": "1985-06-24", "startTime": "00:00", "endTime": "24:00"},
                        ],
                        "selfDeliveryRule": {
                            "shipmentDay": 7,
                            "shipmentBySupplier": Absent(),
                            "receptionByWarehouse": Absent(),
                            "dayFrom": 8,
                            "dayTo": 8,
                            "orderBefore": 20,
                            "workInHoliday": True,
                            "currency": "RUR",
                            "cost": "600",
                            "shipperHumanReadableId": "",
                        },
                        "region": {
                            "entity": "region",
                            "id": 213,
                            "name": "Moscow",
                            "lingua": {"name": {"genitive": "Moscow", "preposition": " ", "prepositional": "Moscow"}},
                        },
                        "gpsCoord": {"longitude": "37.1", "latitude": "55.5"},
                        "daily": True,
                        "around-the-clock": True,
                    }
                ]
            },
        )

    def test_outlet_address(self):
        """
        Проверяет поля адреса в аутлете
        """

        request = 'place=outlets&outlets=123456'
        response = self.report.request_xml(request)
        self.assertFragmentIn(
            response,
            '''
        <outlet>
            <Rank>0</Rank>
            <PointId>123456</PointId>
            <RegionId>213</RegionId>
            <SelfDeliveryRule>
                <MinDeliveryDays>0</MinDeliveryDays>
                <MaxDeliveryDays>2</MaxDeliveryDays>
                <ShipperId>99</ShipperId>
                <UnspecifiedDeliveryInterval>false</UnspecifiedDeliveryInterval>
                <WorkInHoliday>false</WorkInHoliday>
                <Cost>100</Cost>
                <PriceTo>0</PriceTo>
                <CalcCurrency>RUR</CalcCurrency>
                <CalcCost>100</CalcCost>
                <CalcPriceTo>0</CalcPriceTo>
            </SelfDeliveryRule>
            <PointType>MIXED</PointType>
            <Purpose>pickup</Purpose>
            <Purpose>store</Purpose>
            <PointName>OUTLET-5000-123456</PointName>
            <Email>aaa@bbb.ru</Email>
            <GpsCoord>41.920925,54.343961</GpsCoord>
            <IsMain>true</IsMain>
            <LocalityName>Moscow</LocalityName>
            <ThoroughfareName>Karl Marx av.</ThoroughfareName>
            <Km>3</Km>
            <PremiseNumber>1</PremiseNumber>
            <Building>6</Building>
            <Block>2</Block>
            <Estate>4</Estate>
            <OfficeNumber>5</OfficeNumber>
            <AddressAdd>ABC</AddressAdd>
            <ShopId>5000</ShopId>
            <ServiceId>99</ServiceId>
        </outlet>
        ''',
            allow_different_len=True,
        )

        """Проверяется возмоность вывода в вормате json для place=outlets"""
        response = self.report.request_json(request + '&bsformat=2')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "outlet",
                        "id": "123456",
                        "name": "OUTLET-5000-123456",
                        "rank": 0,
                        "type": "mixed",
                        "purpose": ["pickup", "store"],
                        "serviceId": 99,
                        "email": "aaa@bbb.ru",
                        "isMarketBranded": False,
                        "isMarketPartner": False,
                        "isMarketPostTerm": False,
                        "shop": {"id": 5000},
                        "address": {
                            "fullAddress": "Moscow, Karl Marx av., 3-й км, д. 1, корп. 2, стр. 6, влад. 4, офис 5",
                            "country": "",
                            "region": "",
                            "locality": "Moscow",
                            "street": "Karl Marx av.",
                            "km": "3",
                            "building": "1",
                            "block": "2",
                            "wing": "6",
                            "estate": "4",
                            "entrance": "",
                            "floor": "",
                            "room": "5",
                            "office_number": "5",
                            "note": "ABC",
                        },
                        "selfDeliveryRule": {
                            "workInHoliday": False,
                            "currency": "RUR",
                            "cost": "100",
                            "shipperHumanReadableId": "",
                        },
                        "region": {
                            "entity": "region",
                            "id": 213,
                            "name": "Moscow",
                            "lingua": {"name": {"genitive": "Moscow", "preposition": " ", "prepositional": "Moscow"}},
                        },
                        "gpsCoord": {"longitude": "41.920925", "latitude": "54.343961"},
                    }
                ]
            },
        )

    def generateDayWithSeveralIntervals(self, dayFrom, dayTo):
        return {
            "daysFrom": str(dayFrom),
            "daysTo": str(dayTo),
            "hoursFrom": "10:00",
            "hoursTo": "22:00",
            "breaks": [{"hoursFrom": "18:00", "hoursTo": "18:30"}, {"hoursFrom": "19:00", "hoursTo": "20:00"}],
        }

    def generateSimpleDay(self, dayFrom, dayTo):
        return {"daysFrom": str(dayFrom), "daysTo": str(dayTo), "hoursFrom": "10:00", "hoursTo": "18:00"}

    def test_outlet_working_time_grouping(self):
        base_request = 'place=outlets&rids=213&outlets={}&bsformat=2'
        request = base_request + '&compress-working-time=1'

        """Проверяется, что если расписание одинаково для каждого дня, то выводится одна группа дней"""
        response = self.report.request_json(request.format('4000'))
        self.assertFragmentIn(response, {"workingTime": [self.generateSimpleDay(1, 7)]}, allow_different_len=False)

        """Проверяется, что дни с понедельника по пятницу не группируются, если не все дни в этом интервале имеют одинаковые часы работы"""
        response = self.report.request_json(request.format('4001'))
        self.assertFragmentIn(
            response,
            {
                "workingTime": [
                    self.generateDayWithSeveralIntervals(1, 1),
                    self.generateDayWithSeveralIntervals(2, 2),
                    self.generateSimpleDay(3, 3),
                    self.generateDayWithSeveralIntervals(4, 4),
                    self.generateSimpleDay(5, 5),
                ]
            },
            allow_different_len=False,
        )

        """    от же случай, только для 4 рабочих и 2 выходных дней"""
        response = self.report.request_json(request.format('4002'))
        self.assertFragmentIn(
            response,
            {
                "workingTime": [
                    self.generateSimpleDay(1, 1),
                    self.generateSimpleDay(2, 2),
                    self.generateSimpleDay(3, 3),
                    self.generateSimpleDay(5, 5),
                    self.generateSimpleDay(6, 6),
                    self.generateSimpleDay(7, 7),
                ]
            },
            allow_different_len=False,
        )

        """Проверяется группировка для часов работы с несколькими интервалами в рамках одного дня. Выходные дни с одинаковыми интервалами работы не группируются."""
        response = self.report.request_json(request.format('4003'))
        self.assertFragmentIn(
            response,
            {
                "workingTime": [
                    self.generateDayWithSeveralIntervals(1, 5),
                    self.generateSimpleDay(6, 6),
                    self.generateSimpleDay(7, 7),
                ]
            },
            allow_different_len=False,
        )

        """Провряется, что группировки нет, если не задан флаг &compress-working-time=1"""
        response = self.report.request_json(base_request.format('4000'))
        self.assertFragmentIn(
            response, {"workingTime": [self.generateSimpleDay(i, i) for i in range(1, 8)]}, allow_different_len=False
        )

    @classmethod
    def prepare_pickup_option_region(cls):
        cls.index.shops += [
            Shop(fesh=75, priority_region=213),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=175,
                fesh=75,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=3,
                    day_to=5,
                    order_before=16,
                    price=500,
                    price_to=1000,
                ),
                working_days=[i for i in range(10)],
            ),
        ]

        cls.index.offers += [
            Offer(fesh=75, title="outlet_delivery_region", pickup_buckets=[5027]),
        ]

    def test_pickup_option_region(self):
        '''
        Что проверяем: вывод региона в опциях доставки в ПВЗ
        '''
        response = self.report.request_json('place=prime&fesh=75&pickup-options=raw&rids=213')
        self.assertFragmentIn(
            response,
            {
                "delivery": {
                    "pickupOptions": [
                        {
                            "outlet": {
                                "id": "175",
                            },
                            "region": {
                                "id": 213,
                            },
                        }
                    ],
                },
            },
        )

    def test_working_time_with_calendar_holiday(self):
        """Проверяется, что результирующие рабочие дни вычисляются на основе нерабочих дат и нерабочих дней недели"""
        request = 'place=outlets&outlets=5000&bsformat=2&fesh=112238'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "workingDay": [
                    {"date": "1985-06-25", "startTime": "10:00", "endTime": "19:00"},
                    {"date": "1985-06-26", "startTime": "10:00", "endTime": "18:00"},
                    {"date": "1985-06-28", "startTime": "10:00", "endTime": "18:00"},
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_delivery_service_outlet_code(cls):
        cls.index.outlets += [
            Outlet(
                point_id=5101,
                region=213,
                delivery_service_id=1212,
                delivery_service_outlet_code='BiggestOutlet',
                point_type=Outlet.FOR_PICKUP,
            )
        ]
        cls.index.shops += [
            Shop(fesh=1213, priority_region=213, delivery_service_outlets=[5101], cpa=Shop.CPA_REAL),
        ]

    def test_delivery_service_outlet_code(self):
        '''
        Проверяем наличие кода СД для оутлета на выдаче в формате json и xml
        '''
        request = 'place=outlets&outlets=5101,202&bsformat='
        # json
        response = self.report.request_json(request + '2')
        self.assertFragmentIn(
            response,
            [
                {
                    "id": "5101",
                    "deliveryServiceOutletCode": "BiggestOutlet",
                },
                {
                    "id": "202",
                    "deliveryServiceOutletCode": "",
                },
            ],
        )

        # xml
        response = self.report.request_xml(request)
        self.assertFragmentIn(
            response,
            '''
            <outlet>
                <PointId>5101</PointId>
                <DeliveryServiceOutletCode>BiggestOutlet</DeliveryServiceOutletCode>
            </outlet>
        ''',
        )

    @classmethod
    def prepare_white_program_pickup(cls):
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5030,
                fesh=1891,
                carriers=[192],
                options=[PickupOption(outlet_id=260, day_from=3, day_to=5, price=500)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_WHITE_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5031,
                fesh=1891,
                carriers=[99],
                options=[PickupOption(outlet_id=261, day_from=3, day_to=5, price=600)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]
        cls.index.outlets += [
            Outlet(point_id=260, fesh=1891, region=213, point_type=Outlet.FOR_PICKUP),
            Outlet(point_id=261, fesh=1891, region=213, point_type=Outlet.FOR_PICKUP),
        ]
        cls.index.shops += [Shop(fesh=1891, priority_region=213)]
        cls.index.offers += [
            Offer(fesh=1891, title='outlet_with_white_program_pickup_bucket', pickup_buckets=[5030, 5031])
        ]

    def test_white_program_pickup_with_exp(self):
        '''
        Проверяем, что на выдаче аутлеты из обоих бакетов
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json(
            'place=geo&text=outlet_with_white_program_pickup_bucket&rearr-factors=market_use_white_program_pickup_buckets=1&mega-points=true'
            + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                'total': 2,
                'totalOffers': 1,
                'shopOutlets': 2,
                'results': [
                    {
                        'entity': 'offer',
                        'titles': {'raw': 'outlet_with_white_program_pickup_bucket'},
                        'outlet': {'entity': 'outlet', 'id': '260', 'selfDeliveryRule': {'cost': '500'}},
                    },
                    {
                        'entity': 'offer',
                        'titles': {'raw': 'outlet_with_white_program_pickup_bucket'},
                        'outlet': {'entity': 'outlet', 'id': '261', 'selfDeliveryRule': {'cost': '600'}},
                    },
                ],
            },
        )

    def test_white_program_pickup_no_exp(self):
        '''
        Проверяем, что на выдаче аутлеты только из бакета с программой REGULAR_PROGRAM
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json(
            'place=geo&text=outlet_with_white_program_pickup_bucket&rearr-factors=market_use_white_program_pickup_buckets=0'
            + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                'total': 1,
                'shopOutlets': 1,
                'results': [
                    {
                        'entity': 'offer',
                        'titles': {'raw': 'outlet_with_white_program_pickup_bucket'},
                        'outlet': {'entity': 'outlet', 'id': '261', 'selfDeliveryRule': {'cost': '600'}},
                    }
                ],
            },
        )

        # Проверяем, что без mega-points=true не используется white program
        response = self.report.request_json(
            'place=geo&text=outlet_with_white_program_pickup_bucket&rearr-factors=market_use_white_program_pickup_buckets=1'
            + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                'total': 1,
                'shopOutlets': 1,
                'results': [
                    {
                        'entity': 'offer',
                        'titles': {'raw': 'outlet_with_white_program_pickup_bucket'},
                        'outlet': {'entity': 'outlet', 'id': '261', 'selfDeliveryRule': {'cost': '600'}},
                    }
                ],
            },
        )

    @classmethod
    def prepare_post_delivery_white(cls):
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=9030,
                fesh=1891,
                carriers=[213],
                options=[PickupOption(outlet_id=960, day_from=3, day_to=5, price=500)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_WHITE_PROGRAM,
            ),
            PickupBucket(
                bucket_id=9031,
                fesh=1891,
                carriers=[214],
                options=[PickupOption(outlet_id=961, day_from=3, day_to=5, price=600)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_WHITE_PROGRAM,
            ),
            PickupBucket(
                bucket_id=9032,
                fesh=1891,
                carriers=[215],
                options=[PickupOption(outlet_id=962, day_from=3, day_to=5, price=700)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_WHITE_PROGRAM,
            ),
        ]
        cls.index.outlets += [
            Outlet(point_id=960, region=213, point_type=Outlet.FOR_PICKUP, delivery_service_id=213),
            Outlet(point_id=961, region=213, point_type=Outlet.FOR_PICKUP, delivery_service_id=214),
            Outlet(point_id=962, region=213, point_type=Outlet.FOR_PICKUP, delivery_service_id=215),
        ]
        cls.index.shops += [Shop(fesh=1891, priority_region=213, delivery_service_outlets=[960, 961, 962])]
        cls.index.offers += [
            Offer(fesh=1891, title='outlet_with_post_delivery_white_bucket', pickup_buckets=[9030, 9031]),
            Offer(fesh=1891, title='outlet_without_post_delivery_white_bucket', pickup_buckets=[9032]),
        ]

    def test_post_delivery_white(self):
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json('place=geo&text=outlet_with_post_delivery_white_bucket' + unified_off_flags)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "outlet_with_post_delivery_white_bucket"},
                        "outlet": {
                            "entity": "outlet",
                            "id": "960",
                            "selfDeliveryRule": {"cost": "500"},
                            "serviceId": 213,
                        },
                        "shop": {
                            "outletsCount": 1,
                            "pickupStoresCount": 1,
                            "depotStoresCount": 1,
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "outlet_with_post_delivery_white_bucket"},
                        "outlet": {
                            "entity": "outlet",
                            "id": "961",
                            "selfDeliveryRule": {"cost": "600"},
                            "serviceId": 214,
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=geo&text=outlet_without_post_delivery_white_bucket' + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                "total": 0,
                "shopOutlets": 0,
            },
            allow_different_len=False,
        )
        self.assertFragmentNotIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "outlet_without_post_delivery_white_bucket"},
            },
        )

    @classmethod
    def prepare_storage_period(cls):
        cls.index.outlets += [
            Outlet(
                point_id=1999,
                region=213,
                delivery_service_id=1212,
                point_type=Outlet.FOR_PICKUP,
                storage_period=15,
            ),
            Outlet(
                point_id=2000,
                region=213,
                delivery_service_id=1212,
                point_type=Outlet.FOR_PICKUP,
                storage_period=0,
            ),
            Outlet(
                point_id=2001,
                region=213,
                fesh=300,
                point_type=Outlet.FOR_PICKUP,
                storage_period=16,
            ),
        ]
        cls.index.shops += [
            Shop(fesh=300, priority_region=213, delivery_service_outlets=[1999, 2000], cpa=Shop.CPA_REAL),
        ]

    def test_storage_period_exists(self):
        '''
        Проверяем наличие срока хранения для оутлета на выдаче в формате json и xml
        '''
        flag = "&rearr-factors=market_blue_show_storage_period=1"

        # Добавляем дефолтное значение срока хранения, переданное в эксп флаге
        flag = flag + "&rearr-factors=market_blue_default_storage_period=7"

        request = 'place=outlets&outlets={}{}&bsformat='

        # Запрос для не нулевого срока хранения
        req_storage_15 = (1999, 15, flag)

        # Запрос для нулевого срока хранения
        req_storage_0 = (2000, 0, flag)

        # Запрос к точке без срокаа хранения, но с дефолтным значением
        _ = (202, 7, flag)

        # Запрос для Click and collect
        req_storage_CC_flag = (2001, 16, flag)
        req_storage_CC_no_flag = (2001, 16, '')  # Срок хранения для C&C показывается всегда, вне зависимости от флага

        for (outlet_id, period, flag) in [req_storage_15, req_storage_0, req_storage_CC_flag, req_storage_CC_no_flag]:
            # json
            response = self.report.request_json(request.format(outlet_id, flag) + '2')
            self.assertFragmentIn(
                response,
                [
                    {
                        "id": str(outlet_id),
                        "storagePeriod": period,
                    }
                ],
            )
            # xml
            response = self.report.request_xml(request.format(outlet_id, flag))
            self.assertFragmentIn(
                response,
                '''
                <outlet>
                    <PointId>{}</PointId>
                    <StoragePeriod>{}</StoragePeriod>
                </outlet>
            '''.format(
                    outlet_id, period
                ),
            )

    def test_storage_period_not_exists(self):
        '''
        Проверяем отсутствие срока хранения для оутлета на выдаче в формате json и xml
        Оутлет не показывается, если не задан или если нельзя показывать
        '''
        flag = "&rearr-factors=market_blue_show_storage_period=1"

        request = 'place=outlets&outlets={}{}&bsformat='
        for req in [request.format(202, flag), request.format(1999, ''), request.format(2000, '')]:
            # json
            response = self.report.request_json(req + '2')
            self.assertFragmentNotIn(response, [{"storagePeriod"}])
            # xml
            response = self.report.request_xml(req)
            self.assertFragmentNotIn(
                response,
                '''
                StoragePeriod
            ''',
            )

    @classmethod
    def prepare_mbi_to_lms_aliases(cls):
        # Add outlet and its copy with alias_outlet_id
        cls.index.outlets += [
            Outlet(
                point_id=123321,
                region=213,
                delivery_service_id=1212,
                point_type=Outlet.FOR_PICKUP,
                email='e1@mail.com',
            ),
            Outlet(
                point_id=10000000975,
                mbi_alias_point_id=123321,
                region=213,
                delivery_service_id=1212,
                point_type=Outlet.FOR_PICKUP,
                email='e1@mail.com',
            ),
        ]

    def test_lms_to_mbi_aliases(self):
        '''
        Проверяем, что репорт корректно возвращает идентификаторы аутлетов с учётом
        алиасов mbi_outlet_id <---> lms_outlet_id
        '''
        # (outlet_id, expected_outlet_id, market_use_lms_outlets) triples.
        # Place=outlets returns the same outlet_id that was passed in request.
        OUTLETS_DATA = (
            (123321, 123321, 0),
            (10000000975, 10000000975, 1),
        )

        request_template = (
            'place=outlets&'
            'outlets={outlet_id}&'
            'bsformat={format_type}&'
            'rearr-factors=market_use_lms_outlets={flag_value}'
        )

        for outlet_id, expected_outlet_id, flag_value in OUTLETS_DATA:
            expected_email = 'e1@mail.com'
            # json
            request = request_template.format(outlet_id=outlet_id, format_type=2, flag_value=flag_value)
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                [
                    {
                        "id": str(expected_outlet_id),
                        "email": expected_email,
                    }
                ],
            )
            # xml
            request = request_template.format(outlet_id=outlet_id, format_type=0, flag_value=flag_value)
            response = self.report.request_xml(request)
            self.assertFragmentIn(
                response,
                '''
                    <outlet>
                        <PointId>{0}</PointId>
                        <Email>{1}</Email>
                    </outlet>
                '''.format(
                    expected_outlet_id, expected_email
                ),
            )

    def test_regular_program_deletes_white_in_pickup_options(self):
        """
        If regular program exists, market delivery white is not shown in pickupOptions, only in availableServices.
        Test that not_filter_white_program_delivery flag disables this functionality.
        """
        response = self.report.request_json(
            'place=prime&text=outlet_with_white_program_pickup_bucket&rids=213&pickup-options=raw&mega-points=1'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {'raw': 'outlet_with_white_program_pickup_bucket'},
                'delivery': {'pickupOptions': [{"partnerType": "regular"}]},
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=prime&text=outlet_with_white_program_pickup_bucket&rids=213&pickup-options=raw&rearr-factors=not_filter_white_program_delivery=1&mega-points=1'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {'raw': 'outlet_with_white_program_pickup_bucket'},
                'delivery': {'pickupOptions': [{"partnerType": "regular"}, {"partnerType": "market_delivery_white"}]},
            },
            allow_different_len=False,
            preserve_order=True,
        )

    MARKET_POST_TERM_ID_SHIFT = 50000

    @classmethod
    def prepare_market_branded(cls):
        for id in [1005483, 1005480, 1005558, 1005561, 1005720, 1005360, 1005354, 1005363]:
            cls.index.shops += [
                Shop(fesh=id, priority_region=213, delivery_service_outlets=[id], cpa=Shop.CPA_REAL),
            ]
            cls.index.outlets += [
                Outlet(
                    point_id=id,
                    delivery_service_id=id,
                    region=213,
                    point_type=Outlet.FOR_PICKUP,
                    working_days=[0, 1, 2, 3, 4, 6],
                    bool_props=["isMarketBranded", "isMarketPartner"] if (id & 1) else None,
                    working_times=[
                        OutletWorkingTime(
                            days_from=OutletWorkingTime.TUESDAY,
                            days_till=OutletWorkingTime.TUESDAY,
                            hours_from="10:00",
                            hours_till="19:00",
                        ),
                        OutletWorkingTime(
                            days_from=OutletWorkingTime.WEDNESDAY,
                            days_till=OutletWorkingTime.WEDNESDAY,
                            hours_from="10:00",
                            hours_till="18:00",
                        ),
                        OutletWorkingTime(
                            days_from=OutletWorkingTime.FRIDAY,
                            days_till=OutletWorkingTime.FRIDAY,
                            hours_from="10:00",
                            hours_till="18:00",
                        ),
                        OutletWorkingTime(
                            days_from=OutletWorkingTime.SATURDAY,
                            days_till=OutletWorkingTime.SATURDAY,
                            hours_from="10:00",
                            hours_till="18:00",
                        ),
                    ],
                ),
                Outlet(
                    point_id=id + cls.MARKET_POST_TERM_ID_SHIFT,
                    delivery_service_id=id,
                    region=213,
                    point_type=Outlet.FOR_POST_TERM,
                    working_days=[0, 1, 2, 3, 4, 6],
                    bool_props=["isMarketPostTerm"] if (id & 1) else None,
                    working_times=[
                        OutletWorkingTime(
                            days_from=OutletWorkingTime.TUESDAY,
                            days_till=OutletWorkingTime.TUESDAY,
                            hours_from="10:00",
                            hours_till="19:00",
                        ),
                        OutletWorkingTime(
                            days_from=OutletWorkingTime.WEDNESDAY,
                            days_till=OutletWorkingTime.WEDNESDAY,
                            hours_from="10:00",
                            hours_till="18:00",
                        ),
                        OutletWorkingTime(
                            days_from=OutletWorkingTime.FRIDAY,
                            days_till=OutletWorkingTime.FRIDAY,
                            hours_from="10:00",
                            hours_till="18:00",
                        ),
                        OutletWorkingTime(
                            days_from=OutletWorkingTime.SATURDAY,
                            days_till=OutletWorkingTime.SATURDAY,
                            hours_from="10:00",
                            hours_till="18:00",
                        ),
                    ],
                ),
            ]

    def test_market_branded(self):
        '''
        Проверяем выставление флага "isMarketBranded" только для брэндированных ПВЗ
        Сейчас это захардкоженный список СД
        '''
        request = 'place=outlets&outlets={}&bsformat=2&minify-outlets=1&rearr-factors=market_branded_outlets_from_lms=0'
        for id in [1005483, 1005480, 1005558, 1005561, 1005720, 1005360, 1005354, 1005363]:
            response = self.report.request_json(request.format(id))
            self.assertFragmentIn(response, {"entity": "outlet", "id": str(id), 'isMarketBranded': True})

        # Оутлет другой СД не брэндирован
        response = self.report.request_json(request.format(251))
        self.assertFragmentIn(response, {"entity": "outlet", "id": "251", 'isMarketBranded': False})

    def test_outlet_flags_from_lms(self):
        '''
        Проверяем выставление флагов "isMarketBranded", "isMarketPartner" и isTryingAvailable
        только для брэндированных, партнерских ПВЗ и с доступной примеркой у брендированных соответственно
        Данные о брэндированности и партнерстве хранятся в настройках оутлетов
        Признак примерки временно считаем если isMarketBranded и не isMarketPostTerm
        В настройках теста все нечетные ПВЗ брэндированы и являются партнерскими
        Так же брэндирован оутлет другой СД

        Также проверяем флаг маркетных постаматов "isMarketPostTerm"
        id постаматов смещены относительно ПВЗ на self.MARKET_POST_TERM_ID_SHIFT
        все маркетные постаматы по аналогии с ПВЗ - нечетные.
        '''
        request = 'place=outlets&outlets={}&bsformat=2&minify-outlets=1'
        for id in [1005483, 1005480, 1005558, 1005561, 1005720, 1005360, 1005354, 1005363]:
            is_market_branded = True if (id & 1) else False
            is_market_partner = True if (id & 1) else False
            is_market_postterm = False
            response = self.report.request_json(request.format(id))
            self.assertFragmentIn(
                response,
                {
                    "entity": "outlet",
                    "id": str(id),
                    'isMarketBranded': is_market_branded,
                    'isMarketPartner': is_market_partner,
                    'isMarketPostTerm': is_market_postterm,
                    'isTryingAvailable': is_market_branded and not is_market_postterm,
                },
            )

            is_market_branded = False
            is_market_partner = False
            is_market_postterm = True if ((id + self.MARKET_POST_TERM_ID_SHIFT) & 1) else False
            response = self.report.request_json(request.format(id + self.MARKET_POST_TERM_ID_SHIFT))
            self.assertFragmentIn(
                response,
                {
                    "entity": "outlet",
                    "id": str(id + self.MARKET_POST_TERM_ID_SHIFT),
                    'isMarketBranded': is_market_branded,
                    'isMarketPartner': is_market_partner,
                    'isMarketPostTerm': is_market_postterm,
                    'isTryingAvailable': is_market_branded and not is_market_postterm,
                },
            )

        is_market_branded = True
        is_market_partner = False
        is_market_postterm = False
        # Оутлет другой СД тоже брэндирован в настройках
        response = self.report.request_json(request.format(251))
        self.assertFragmentIn(
            response,
            {
                "entity": "outlet",
                "id": "251",
                'isMarketBranded': True,
                'isMarketPartner': False,
                'isMarketPostTerm': False,
                'isTryingAvailable': is_market_branded and not is_market_postterm,
            },
        )

    @classmethod
    def prepare_aggregation_by_market_branded_property(cls):
        cls.index.shops += [
            Shop(
                fesh=1123,
                priority_region=213,
                delivery_service_outlets=[2011, 2012, 2013],
            ),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=2011,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_service_id=333,
                delivery_option=OutletDeliveryOption(day_from=3, day_to=5, order_before=6, price=500, shipper_id=333),
                working_days=[i for i in range(10)],
                bool_props=["isMarketBranded"],
            ),
            Outlet(
                point_id=2012,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_service_id=333,
                delivery_option=OutletDeliveryOption(day_from=3, day_to=5, order_before=6, price=500, shipper_id=333),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=2013,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_service_id=333,
                delivery_option=OutletDeliveryOption(day_from=3, day_to=5, order_before=6, price=500, shipper_id=333),
                working_days=[i for i in range(10)],
                bool_props=["isMarketBranded"],
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5123,
                fesh=1123,
                carriers=[333],
                options=[
                    PickupOption(outlet_id=2011, day_from=3, day_to=5, price=500),
                    PickupOption(outlet_id=2012, day_from=3, day_to=5, price=500),
                    PickupOption(outlet_id=2013, day_from=3, day_to=5, price=500),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [
            Offer(
                fesh=1123,
                hyperid=100,
                pickup_buckets=[5123],
                waremd5='DuE098x_rinQLZn3KKrELw',
                weight=5,
                dimensions=OfferDimensions(length=10, width=20, height=30),
            ),
        ]

    def test_aggregation_by_market_branded_property(self):
        '''
        Проверяется, что в place=actual_delivery при аггрегации пикап бакетов учитывается
        свойство isMarketBranded аутлета
        '''
        request = (
            'place=actual_delivery'
            '&offers-list=DuE098x_rinQLZn3KKrELw:1'
            '&rids=213&pickup-options=grouped'
            '&pickup-options-extended-grouping=1'
            '&combinator=0'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "isMarketBranded": True,
                                    "outletIds": [2011, 2013],
                                },
                                {"outletIds": [2012], "isMarketBranded": Absent()},
                            ]
                        }
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_yandex_map_link(cls):
        cls.index.outlets += [
            Outlet(
                point_id=3000,
                region=213,
                delivery_service_id=1212,
                point_type=Outlet.FOR_PICKUP,
                yandex_map_link=15485476,
            ),
            Outlet(
                point_id=3001,
                region=213,
                delivery_service_id=1212,
                point_type=Outlet.FOR_PICKUP,
            ),
        ]
        cls.index.shops += [
            Shop(fesh=325, priority_region=213, delivery_service_outlets=[1999, 2000], cpa=Shop.CPA_REAL),
        ]

    def test_yandex_map_link(self):
        '''
        Проверяем корректную выдачу параметра yandexMapPermalink у аутлетов
        '''
        request = 'place=outlets&outlets=3000,3001&bsformat=2'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "id": "3000",
                        "yandexMapPermalink": 15485476,
                    },
                    {
                        "id": "3001",
                        "yandexMapPermalink": Absent(),
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_calendar_holiday(self):
        '''
        Проверяем что выдача содержит расписание праздников (должно работать без указания fesh)
        '''
        request = 'place=outlets&outlets=4000'
        response = self.report.request_json(request + '&bsformat=2')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "outlet",
                        "id": "4000",
                        "calendarHolidays": {
                            "dates": [
                                "1985-06-27",
                                "1985-06-28",
                            ],
                            "startDate": "1985-06-24",
                            "endDate": "1985-06-29",
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_xml(request)
        self.assertFragmentIn(
            response,
            '''
            <outlet>
                <PointId>{}</PointId>
                <CalendarHolidays>
                    <Date>1985-06-27</Date>
                    <Date>1985-06-28</Date>
                    <StartDate>1985-06-24</StartDate>
                    <EndDate>1985-06-29</EndDate>
                </CalendarHolidays>
            </outlet>
            '''.format(
                4000
            ),
        )


if __name__ == '__main__':
    main()
