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
    DeliveryServiceRegionToRegionInfo,
    DynamicCapacityDaysOff,
    DynamicCapacityInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicTimeIntervalsSet,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
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
from core.types.delivery import get_depth_days


ALL_DAYS = 0
NO_DAYS = 1
TODAY = 2
TOMORROW = 3
AFTER_TOMORROW = 4
NO_AVAILABLE_CAPACITY = 5
SINGLE_AVAILABLE_DAY = 6
TODAY_AND_AFTER_TOMORROW = 7
ALL_INTERVALS = 0


SKU_FULFILLMENT = 1
SKU_CROSSDOCK = 2
SKU_DROPSHIP = 3


def generate_capacity_days_off(when, region_to):
    return DynamicCapacityInfo(
        region_to,
        capacity_days_off=[
            DynamicCapacityDaysOff(
                delivery_type=DynamicCapacityDaysOff.DT_COURIER,
                days_key=when,
            ),
            DynamicCapacityDaysOff(
                delivery_type=DynamicCapacityDaysOff.DT_PICKUP,
                days_key=when,
            ),
            DynamicCapacityDaysOff(
                delivery_type=DynamicCapacityDaysOff.DT_POST,
                days_key=when,
            ),
        ],
    )


lms_environment = [
    DynamicDaysSet(
        key=ALL_DAYS,
        days=[0, 1, 2, 3, 4, 5, 6, 7],
    ),
    DynamicDaysSet(
        key=NO_DAYS,
        days=[],
    ),
    DynamicDaysSet(
        key=TODAY,
        days=[0],
    ),
    DynamicDaysSet(
        key=TOMORROW,
        days=[1],
    ),
    DynamicDaysSet(
        key=AFTER_TOMORROW,
        days=[2],
    ),
    DynamicDaysSet(
        key=NO_AVAILABLE_CAPACITY,
        days=list(range(get_depth_days() - 7)),  # current date is 7 days away from lms calendar start_date by default
    ),
    DynamicDaysSet(
        key=SINGLE_AVAILABLE_DAY,
        days=list(range(get_depth_days() - 8)),  # only one day available at the end of the calendar
    ),
    DynamicTimeIntervalsSet(
        key=ALL_INTERVALS,
        intervals=[
            TimeIntervalInfo(
                TimeInfo(10, 0),
                TimeInfo(22, 0),
            )
        ],
    ),
    DynamicDaysSet(
        key=TODAY_AND_AFTER_TOMORROW,
        days=[0, 2],
    ),
    DynamicWarehouseInfo(
        id=145,  # Fulfillment WH
        home_region=212,
        holidays_days_set_key=NO_DAYS,
    ),
    DynamicWarehouseInfo(
        id=200,  # Crossdock WH
        home_region=212,
        holidays_days_set_key=TODAY,  # Crossdock WH holidays merged with crossdock capacity days off
        # Crossdock shipment day = +1
    ),
    DynamicWarehouseInfo(
        id=172,  # Fulfillment WH for crossdock
        home_region=212,
        holidays_days_set_key=NO_DAYS,
    ),
    DynamicWarehouseInfo(
        id=321,  # Dropship warehouse
        home_region=212,
        holidays_days_set_key=TOMORROW,
    ),
    DynamicWarehousesPriorityInRegion(region=212, warehouses=[145, 200, 172]),
    DynamicDeliveryServiceInfo(
        id=157,
        region_to_region_info=[
            DeliveryServiceRegionToRegionInfo(
                region_from=212,
                region_to=212,
                days_key=NO_DAYS,  # holidays
            ),
        ],
        time_intervals=[
            TimeIntervalsForRegion(
                region=212,
                intervals=[
                    TimeIntervalsForDaysInfo(
                        intervals_key=ALL_INTERVALS,
                        days_key=ALL_DAYS,
                    )
                ],
            ),
        ],
    ),
    DynamicWarehouseAndDeliveryServiceInfo(
        warehouse_id=145,  # Fulfillment WH
        delivery_service_id=157,
        date_switch_time_infos=[
            DateSwitchTimeAndRegionInfo(
                date_switch_hour=19,
                region_to=212,
                date_switch_time=TimeInfo(19, 0),
                packaging_time=TimeInfo(3, 30),
            )
        ],
        shipment_holidays_days_set_key=TODAY,  # Fulfillment WH-DS holidays merged with fulfillment capacity days off
        # Fulfillment shipment day = +1
        capacity_by_region=[
            generate_capacity_days_off(TOMORROW, region_to=212),  # Fulfillment shipment day = +2
            generate_capacity_days_off(NO_AVAILABLE_CAPACITY, region_to=214),
            generate_capacity_days_off(SINGLE_AVAILABLE_DAY, region_to=215),
        ],
    ),
    DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
    DynamicWarehouseToWarehouseInfo(warehouse_from=172, warehouse_to=172),
    DynamicWarehouseToWarehouseInfo(warehouse_from=321, warehouse_to=321),
    DynamicWarehouseToWarehouseInfo(
        warehouse_from=200,  # Crossdock WH
        warehouse_to=172,
        date_switch_time_infos=[
            DateSwitchTimeAndRegionInfo(
                date_switch_hour=11,
                region_to=212,
                date_switch_time=TimeInfo(11, 0),
                packaging_time=TimeInfo(2, 10),
            )
        ],
        shipment_holidays_days_set_key=TOMORROW,  # Crossdock WH-WH holidays merged with crossdock capacity days off
        # Crossdock shipment day = +2
        capacity_by_region=[generate_capacity_days_off(AFTER_TOMORROW, region_to=212)],  # Crossdock shipment day = +3
    ),
    DynamicWarehouseAndDeliveryServiceInfo(
        warehouse_id=172,  # Fulfillment WH for crossdock
        delivery_service_id=157,
        date_switch_time_infos=[
            DateSwitchTimeAndRegionInfo(
                date_switch_hour=19,
                region_to=212,
                date_switch_time=TimeInfo(19, 0),
                packaging_time=TimeInfo(3, 30),
            )
        ],
    ),
    DynamicWarehouseAndDeliveryServiceInfo(
        warehouse_id=321,  # Fulfillment WH for crossdock
        delivery_service_id=157,
        date_switch_time_infos=[
            DateSwitchTimeAndRegionInfo(
                date_switch_hour=19,
                region_to=212,
                date_switch_time=TimeInfo(19, 0),
                packaging_time=TimeInfo(3, 30),
            )
        ],
        capacity_by_region=[generate_capacity_days_off(TODAY_AND_AFTER_TOMORROW, region_to=212)],
    ),
]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.lms_autogenerate = False
        cls.settings.nordstream_autogenerate = False
        cls.settings.default_search_experiment_flags += ['market_nordstream=0']

        cls.index.regiontree += [
            Region(rid=212, children=[Region(rid=214), Region(rid=215)]),
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
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
            ),
            Shop(
                fesh=32,
                datafeed_id=32,
                priority_region=212,
                name='crossdock',
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                fulfillment_program=True,
                direct_shipping=False,
                warehouse_id=200,
            ),
            Shop(
                fesh=42,
                datafeed_id=42,
                priority_region=212,
                name='dropship',
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                fulfillment_program=False,
                warehouse_id=321,
            ),
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
                        rid=212,
                        options=[
                            DeliveryOption(
                                price=15,
                                day_from=1,
                                day_to=2,
                                shop_delivery_price=10,
                            ),
                        ],
                    ),
                ],
            )
        ]

        cls.index.mskus += [
            MarketSku(
                title="blue offer sku{}".format(sku),
                sku=sku,
                hyperid=2,
                waremd5="Sku{}-wdDXWsIiLVm1goleg".format(sku),
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=feed,
                        offerid="blue.offer.{}.1".format(sku),
                        waremd5="Sku{}Price5-IiLVm1Goleg".format(sku),
                        weight=5,
                        dimensions=OfferDimensions(
                            length=20,
                            width=30,
                            height=10,
                        ),
                    ),
                ],
                delivery_buckets=[801],
            )
            for sku, feed in [
                (SKU_FULFILLMENT, 22),
                (SKU_CROSSDOCK, 32),
                (SKU_DROPSHIP, 42),
            ]
        ]

        cls.dynamic.lms = lms_environment
        cls.index.delivery_calc_feed_info.append(DeliveryCalcFeedInfo(feed_id=-1, warehouse_id=172))

    def check_holidays(self, sku, shipment_day, should_fail=False, region=212):
        """
        Check the courier delivery is calculated for the shipment_day
        """
        request = (
            "place=actual_delivery&rids={}&offers-list=Sku{}Price5-IiLVm1Goleg:1".format(region, sku) + "&combinator=0"
        )
        response = self.report.request_json(request)
        valid_assertion = self.assertFragmentIn if not should_fail else self.assertFragmentNotIn

        valid_assertion(
            response,
            {
                'entity': "deliveryGroup",
                'delivery': {
                    'options': [
                        {
                            'serviceId': "157",
                            'shipmentDay': shipment_day,
                            'isDefault': True,
                        }
                    ],
                },
            },
            preserve_order=True,
        )

    def test_lms_holidays_fulfillment(self):
        """
        Here the following days off merged:
        Fulfillment WH-DS holidays     = +1 day
        Fulfillment capacity days off  = +2 day
        """

        self.check_holidays(SKU_FULFILLMENT, 2)

    def test_lms_holidays_crossdock(self):
        """
        Here the following days off merged:
        Crossdock WH holidays        = +1 day
        Crossdock WH-WH holidays     = +2 day
        Crossdock capacity days off  = +3 day
        """

        self.check_holidays(SKU_CROSSDOCK, 3)

    def test_no_valid_capacity_days(self):
        """
        Test delivery to a region with no available capacity (214), and a single day (last one in the calendar), 215
        """
        # Capacity exceeded, no delivery available
        self.check_holidays(SKU_FULFILLMENT, 77, True, 214)
        # last calendar date is available
        self.check_holidays(SKU_FULFILLMENT, 76, False, 215)

    def test_dropship_holidays(self):
        """
        Test that drophip supplier's holidays are accounted for in delivery date calculation
        """
        # warehouse holiday on day 1 and exceeds DS capacity on days [0,2]
        self.check_holidays(SKU_DROPSHIP, 3, False, 212)


if __name__ == "__main__":
    main()
