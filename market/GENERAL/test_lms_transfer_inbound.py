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
    DynamicWarehousesPriorityInRegion,
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


RID_COUNTRY = 100
LOCAL_TZ = 10800  # +03:00
TZ_LABEL = "{}{:02}:{:02}".format(
    "+" if LOCAL_TZ > 0 else "-",
    abs(LOCAL_TZ) / 3600,
    (abs(LOCAL_TZ) % 3600) / 60,
)

FESH_START = 1000
BUCKET_ID_START = 1000
WH1_ID_START = 40000
WH2_ID_START = 50000
DS_ID_START = 60000


ALL_DAYS, NO_DAYS, DAY_0, DAY_1, DAY_2, DAY_0_1, DAY_0_1_2, DAY_1_2, DAY_2 = list(range(9))

DAYS_SETS = [
    (ALL_DAYS, list(range(30))),
    (NO_DAYS, []),
    (DAY_0, [0]),
    (DAY_1, [1]),
    (DAY_2, [2]),
    (DAY_0_1, [0, 1]),
    (DAY_0_1_2, [0, 1, 2]),
    (DAY_1_2, [1, 2]),
    (DAY_2, [2]),
]


ALL_INTERVALS = 0


TS_TODAY = "1985-06-24"
TS_TOMORROW = "1985-06-25"
TS_AFTER_TOMORROW = "1985-06-26"
TS_AFTER2_TOMORROW = "1985-06-27"

TS_NOW = TS_TODAY + "T12:00:00" + TZ_LABEL


TEST_CASES = [
    #         ------ @ WH1 -------, ---------------------- @ WH1-WH2 -----------------------------, ------ @ WH2 -------, ------------------------ expected ------------------------------
    # region, holidays, handling h, holidays, cut-off, packaging, operation, transfer h, inbound h, holidays, handling h,                  reception @ WH2       ..     shipment @supplier
    # Transfer ends today and inbound ends today
    (
        101,
        NO_DAYS,
        1,
        NO_DAYS,
        13,
        1,
        1,
        3,
        3,
        NO_DAYS,
        1,
        TS_TODAY + "T21:00:00",
        TS_TODAY + "T15:00:00",
        TS_TODAY + 'T21:00:00',
        TS_TODAY + 'T15:00:00',
    ),
    # Transfer ends today and inbound ends tomorrow
    (
        102,
        NO_DAYS,
        1,
        NO_DAYS,
        13,
        1,
        1,
        6,
        3,
        NO_DAYS,
        1,
        TS_TOMORROW + "T00:00:00",
        TS_TODAY + "T15:00:00",
        TS_AFTER_TOMORROW + 'T00:00:00',
        TS_TOMORROW + 'T15:00:00',
    ),
    # Transfer ends today (WH1-WH2 shipment holidays today — delays to tomorrow) and inbound ends the day after tomorrow
    (
        103,
        NO_DAYS,
        1,
        DAY_0,
        13,
        1,
        1,
        6,
        3,
        NO_DAYS,
        1,
        TS_AFTER_TOMORROW + "T00:00:00",
        TS_TOMORROW + "T15:00:00",
        TS_AFTER2_TOMORROW + 'T00:00:00',
        TS_AFTER_TOMORROW + 'T15:00:00',
    ),
    # Transfer ends today (WH1-WH2 shipment holidays tomorrow — doesn't affect) and inbound ends tomorrow
    (
        104,
        NO_DAYS,
        1,
        DAY_1,
        13,
        1,
        1,
        6,
        3,
        NO_DAYS,
        1,
        TS_TOMORROW + "T00:00:00",
        TS_TODAY + "T15:00:00",
        TS_TOMORROW + 'T00:00:00',
        TS_TODAY + 'T15:00:00',
    ),
    # Transfer ends today and inbound ends tomorrow (WH2 holidays tomorrow - delays to day after tomorrow)
    (
        105,
        NO_DAYS,
        1,
        NO_DAYS,
        13,
        1,
        1,
        6,
        3,
        DAY_1,
        1,
        TS_AFTER_TOMORROW + "T00:00:00",
        TS_TOMORROW + "T15:00:00",
        TS_AFTER2_TOMORROW + 'T00:00:00',
        TS_AFTER_TOMORROW + 'T15:00:00',
    ),
]


(
    RID,
    WH1_HOLIDAYS,
    HANDLING_WH1_HOURS,
    SHIPMENT_HOLIDAYS,
    CUTOFF_HOUR,
    PACKAGING_HOURS,
    OPERATION_TIME,
    TRANSFER_HOURS,
    INBOUND_HOURS,
    WH2_HOLIDAYS,
    HANDLING_WH2_HOURS,
    RECEPTION_BY_WH2,
    SHIPMENT_BY_SUPPLIER,
    RECEPTION_BY_WH2_ADJUSTED,
    SHIPMENT_BY_SUPPLIER_ADJUSTED,
) = list(range(15))


def create_lms_environment():
    return (
        [
            DynamicWarehouseInfo(
                id=WH1_ID_START + test_case[RID],
                home_region=test_case[RID],
                holidays_days_set_key=test_case[WH1_HOLIDAYS],
                handling_info=[
                    HandlingRegionToRegionInfo(
                        region_from=test_case[RID],
                        region_to=test_case[RID],
                        handling_time=TimeInfo(test_case[HANDLING_WH1_HOURS], 0),
                    ),
                ],
            )
            for test_case in TEST_CASES
        ]
        + [
            DynamicWarehouseToWarehouseInfo(
                warehouse_from=WH1_ID_START + test_case[RID],
                warehouse_to=WH2_ID_START + test_case[RID],
                shipment_holidays_days_set_key=test_case[SHIPMENT_HOLIDAYS],
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(
                        date_switch_hour=test_case[CUTOFF_HOUR],
                        region_to=test_case[RID],
                        date_switch_time=TimeInfo(test_case[CUTOFF_HOUR], 0),
                        packaging_time=TimeInfo(test_case[PACKAGING_HOURS], 0),
                    ),
                ],
                operation_time=test_case[OPERATION_TIME],
                transfer_time=TimeInfo(test_case[TRANSFER_HOURS], 0),
                inbound_time=TimeInfo(test_case[INBOUND_HOURS], 0),
            )
            for test_case in TEST_CASES
        ]
        + [
            DynamicWarehouseInfo(
                id=WH2_ID_START + test_case[RID],
                home_region=test_case[RID],
                holidays_days_set_key=test_case[WH2_HOLIDAYS],
                handling_info=[
                    HandlingRegionToRegionInfo(
                        region_from=test_case[RID],
                        region_to=test_case[RID],
                        handling_time=TimeInfo(test_case[HANDLING_WH2_HOURS], 0),
                    ),
                ],
            )
            for test_case in TEST_CASES
        ]
        + [
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=WH2_ID_START + test_case[RID],
                delivery_service_id=DS_ID_START + test_case[RID],
                operation_time=0,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(
                        date_switch_hour=23,
                        region_to=test_case[RID],
                        date_switch_time=TimeInfo(23, 0),
                        packaging_time=TimeInfo(0, 0),
                    ),
                ],
            )
            for test_case in TEST_CASES
        ]
        + [
            DynamicDeliveryServiceInfo(
                id=DS_ID_START + test_case[RID],
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(
                        region_from=test_case[RID],
                        region_to=test_case[RID],
                        days_key=NO_DAYS,  # holidays
                    ),
                ],
                time_intervals=[
                    TimeIntervalsForRegion(
                        region=test_case[RID],
                        intervals=[
                            TimeIntervalsForDaysInfo(
                                intervals_key=ALL_INTERVALS,
                                days_key=ALL_DAYS,
                            ),
                        ],
                    ),
                ],
            )
            for test_case in TEST_CASES
        ]
        + [
            DynamicWarehousesPriorityInRegion(
                region=test_case[RID],
                warehouses=[
                    WH1_ID_START + test_case[RID],
                    WH2_ID_START + test_case[RID],
                ],
            )
            for test_case in TEST_CASES
        ]
        + [
            DynamicDaysSet(
                key=days_set[0],
                days=days_set[1],
            )
            for days_set in DAYS_SETS
        ]
        + [
            DynamicTimeIntervalsSet(
                key=ALL_INTERVALS,
                intervals=[
                    TimeIntervalInfo(
                        TimeInfo(10, 0),
                        TimeInfo(18, 0),
                    ),
                ],
            ),
        ]
    )


REQUESTS = [
    "rgb=blue&place=actual_delivery&offers-list={ware_md5}:1&regset=2&combinator=0&rids={rid}&"
    "rearr-factors=debug_delivery_datetime={now_timestamp};market_blue_shipment_by_supplier={use_shipment_by_supplier}",
]


def offer_ware_md5(test_case):
    return "Sku1Price5-IiLVm1G{:0>3}g".format(test_case[RID])


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.lms_autogenerate = False
        cls.settings.nordstream_autogenerate = False
        cls.settings.default_search_experiment_flags += ['market_nordstream=0']

        cls.index.regiontree += [
            Region(
                rid=RID_COUNTRY,
                tz_offset=LOCAL_TZ,
                children=[
                    Region(
                        rid=test_case[RID],
                        tz_offset=LOCAL_TZ,
                    )
                    for test_case in TEST_CASES
                ],
            ),
        ]

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=RID_COUNTRY,
                name='virtual shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            )
        ] + [
            Shop(
                fesh=FESH_START + test_case[RID],
                datafeed_id=FESH_START + test_case[RID],
                priority_region=test_case[RID],
                name='blue shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                fulfillment_program=True,
                direct_shipping=False,
                warehouse_id=WH1_ID_START + test_case[RID],
            )
            for test_case in TEST_CASES
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=BUCKET_ID_START + test_case[RID],
                dc_bucket_id=BUCKET_ID_START + test_case[RID],
                fesh=FESH_START + test_case[RID],
                carriers=[DS_ID_START + test_case[RID]],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=test_case[RID],
                        options=[
                            DeliveryOption(
                                price=15,
                                day_from=1,
                                day_to=2,
                                shop_delivery_price=10,
                            ),
                        ],
                    )
                ],
            )
            for test_case in TEST_CASES
        ]

        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1",
                sku=1,
                hyperid=100,
                blue_offers=[
                    BlueOffer(
                        feedid=FESH_START + test_case[RID],
                        waremd5=offer_ware_md5(test_case),
                        price=5,
                        vat=Vat.VAT_10,
                        weight=5,
                        dimensions=OfferDimensions(
                            length=20,
                            width=30,
                            height=10,
                        ),
                    )
                    for test_case in TEST_CASES
                ],
                delivery_buckets=[BUCKET_ID_START + test_case[RID] for test_case in TEST_CASES],
            ),
        ]

        cls.dynamic.lms = create_lms_environment()

    def check_lms_transfer_inbound(
        self,
        request_template,
        test_case,
    ):
        rid = test_case[RID]
        reception_by_wh2 = test_case[RECEPTION_BY_WH2] + TZ_LABEL
        shipment_by_supplier = test_case[SHIPMENT_BY_SUPPLIER] + TZ_LABEL
        reception_by_wh2_adjusted = test_case[RECEPTION_BY_WH2_ADJUSTED] + TZ_LABEL
        shipment_by_supplier_adjusted = test_case[SHIPMENT_BY_SUPPLIER_ADJUSTED] + TZ_LABEL

        for use_shipment_by_supplier in [True, False]:
            for adjust_shipment_by_supplier in (None, 0, 1):
                request = request_template.format(
                    rid=rid,
                    ware_md5=offer_ware_md5(test_case),
                    now_timestamp=TS_NOW,
                    use_shipment_by_supplier=use_shipment_by_supplier,
                )
                if adjust_shipment_by_supplier is not None:
                    request += '&rearr-factors=market_blue_adjust_shipment_by_supplier={}'.format(
                        adjust_shipment_by_supplier
                    )
                response = self.report.request_json(request)

                rec_by_wh = Absent()
                ship_by_sup = Absent()
                if use_shipment_by_supplier:
                    if adjust_shipment_by_supplier == 0:
                        rec_by_wh = reception_by_wh2
                        ship_by_sup = shipment_by_supplier
                    else:
                        rec_by_wh = reception_by_wh2_adjusted
                        ship_by_sup = shipment_by_supplier_adjusted

                self.assertFragmentIn(
                    response,
                    {
                        'results': [
                            {
                                'entity': 'deliveryGroup',
                                'delivery': {
                                    'options': [
                                        {
                                            'receptionByWarehouse': rec_by_wh,
                                            'shipmentBySupplier': ship_by_sup,
                                        },
                                    ],
                                },
                            }
                        ],
                    },
                )

    def run_test_cases(
        self,
        requests,
        test_cases,
    ):
        for request in requests:
            for test_case in test_cases:
                self.check_lms_transfer_inbound(
                    request,
                    test_case,
                )

    def test_lms_transfer_inbound(self):
        """
        Testing combinations of:
        - transfer time / WH1-WH2 shipment holidays
        - inbound time / WH2 fullfilment holidays
        """

        self.dynamic.lms = create_lms_environment()
        self.run_test_cases(REQUESTS, TEST_CASES)


if __name__ == "__main__":
    main()
