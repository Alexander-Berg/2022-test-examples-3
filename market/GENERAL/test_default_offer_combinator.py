#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import datetime

from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    GpsCoord,
    MarketSku,
    Model,
    OfferDimensions,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Region,
    Shop,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.types.delivery import OutletType, OutletWorkingTime
from core.testcase import TestCase, main
from core.combinator import DeliveryStats, make_offer_id
from core.matcher import Absent


class _Shops:
    FastShop = Shop(
        fesh=1,
        client_id=1,
        datafeed_id=1,
        priority_region=213,
        supplier_type=Shop.FIRST_PARTY,
        blue=Shop.BLUE_REAL,
        cpa=Shop.CPA_REAL,
        warehouse_id=111,
        fulfillment_program=True,
    )
    CheapShop = Shop(
        fesh=2,
        client_id=2,
        datafeed_id=2,
        priority_region=213,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        cpa=Shop.CPA_REAL,
        warehouse_id=222,
        fulfillment_program=False,
    )


class _Offers:
    CheapOffer = BlueOffer(
        offerid="Cheap",
        price=10,
        feedid=_Shops.CheapShop.datafeed_id,
        weight=1.2,
        waremd5='TestOfferCheap_______g',
        dimensions=OfferDimensions(length=10, width=20.1, height=30),
        supplier_id=_Shops.CheapShop.fesh,
        post_buckets=[5001],
    )
    FastOffer = BlueOffer(
        offerid="Fast",
        price=200,
        feedid=_Shops.FastShop.datafeed_id,
        weight=2.2,
        waremd5='TestOfferFast________g',
        dimensions=OfferDimensions(length=20, width=10, height=10.6),
        supplier_id=_Shops.FastShop.fesh,
        delivery_buckets=[800],
        pickup_buckets=[5000],
    )
    VagueOffer = BlueOffer(
        offerid="Vague",
        price=200,
        feedid=_Shops.CheapShop.datafeed_id,
        weight=0.01,
        waremd5='TestOfferVague_______g',
        dimensions=OfferDimensions(length=8.5, width=10, height=1),
        supplier_id=_Shops.CheapShop.fesh,
        post_buckets=[5001],
    )
    CorrectOffer = BlueOffer(
        offerid="Correct",
        price=152,
        feedid=_Shops.FastShop.datafeed_id,
        weight=1.3,
        waremd5='TestOfferCorrect_____g',
        dimensions=OfferDimensions(length=7.5, width=11, height=2),
        supplier_id=_Shops.FastShop.fesh,
        delivery_buckets=[800],
        pickup_buckets=[5000],
        post_buckets=[5001],
    )
    FreeDeliveryOffer = BlueOffer(
        offerid="FreeDelivery",
        price=2501,
        feedid=_Shops.FastShop.datafeed_id,
        weight=1.111,
        waremd5='TestOfferFreeDeliveryg',
        dimensions=OfferDimensions(length=11, width=12, height=13),
        supplier_id=_Shops.FastShop.fesh,
        delivery_buckets=[800],
        pickup_buckets=[5000],
        post_buckets=[5001],
    )


COMBINATOR_TEST_DATA = (
    (
        _Offers.FastOffer,
        _Shops.FastShop,
        DeliveryStats(cost=0, day_from=4, day_to=5),  # courier
        DeliveryStats(cost=10, day_from=2, day_to=3),  # pickup
        None,
    ),  # post
    (
        _Offers.CheapOffer,
        _Shops.CheapShop,
        None,  # courier
        None,  # pickup
        DeliveryStats(cost=20, day_from=8, day_to=11),
    ),
    (
        _Offers.VagueOffer,
        _Shops.CheapShop,
        DeliveryStats(cost=0, day_from=2, day_to=4),  # courier
        None,  # pickup
        DeliveryStats(cost=40, day_from=3, day_to=7),
    ),  # post
    (
        _Offers.CorrectOffer,
        _Shops.FastShop,
        DeliveryStats(cost=5, day_from=1, day_to=2),  # courier
        DeliveryStats(cost=10, day_from=2, day_to=3),  # pickup
        DeliveryStats(cost=20, day_from=6, day_to=15),
    ),  # post
    (
        _Offers.FreeDeliveryOffer,
        _Shops.FastShop,
        DeliveryStats(cost=5, day_from=1, day_to=2),  # courier
        DeliveryStats(cost=10, day_from=2, day_to=3),  # pickup
        DeliveryStats(cost=20, day_from=6, day_to=15),
    ),
)  # post


class T(TestCase):
    @classmethod
    def beforePrepare(cls):
        cls.settings.delivery_calendar_start_date = datetime.date(day=18, month=5, year=2020)
        cls.settings.logbroker_enabled = True
        cls.settings.init_combinator_topics = True

    @classmethod
    def prepare(cls):
        cls.settings.check_combinator_errors = True
        # Current date 18/05/2020 @ 00:26 MSK
        cls.settings.microseconds_for_disabled_random = 1589750813000000
        cls.settings.report_subrole = 'blue-main'
        cls.settings.blue_market_free_delivery_threshold = 2500
        cls.index.regiontree += [
            Region(
                rid=3,
                name='Центральный федеральный округ',
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=1,
                        name='Московская область',
                        region_type=Region.FEDERATIVE_SUBJECT,
                        tz_offset=10800,
                        children=[
                            Region(rid=213, name='Москва', tz_offset=10800),
                            Region(rid=10758, name='Химки', tz_offset=10800),
                        ],
                    ),
                    Region(rid=192, name='Владимир'),
                ],
            ),
        ]

        cls.index.models += [Model(hyperid=i, hid=i) for i in range(1, 4)]
        cls.index.mskus += [
            MarketSku(title="КомбиВзрыв", hyperid=1, sku=1, blue_offers=[_Offers.CheapOffer, _Offers.FastOffer]),
            MarketSku(title="Второй", hyperid=2, sku=2, blue_offers=[_Offers.VagueOffer]),
            MarketSku(title="Comparison", hyperid=3, sku=3, blue_offers=[_Offers.CorrectOffer]),
            MarketSku(title="Free Delivery", hyperid=4, sku=4, blue_offers=[_Offers.FreeDeliveryOffer]),
        ]
        cls.index.shops += [
            _Shops.FastShop,
            _Shops.CheapShop,
        ]
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[225],
                warehouse_with_priority=[
                    WarehouseWithPriority(_Shops.CheapShop.warehouse_id, 100),
                    WarehouseWithPriority(_Shops.FastShop.warehouse_id, 100),
                ],
            )
        ]
        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(_Shops.FastShop.warehouse_id, 213, holidays_days_set_key=1),
            DynamicWarehouseInfo(_Shops.CheapShop.warehouse_id, 213, holidays_days_set_key=1),
            DynamicDeliveryServiceInfo(
                157,
                "ds_157",
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=213, region_to=225, days_key=1)],
            ),
            DynamicDaysSet(key=1, days=[28]),
        ]
        cls.dynamic.lms += [
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=shop.warehouse_id,
                delivery_service_id=157,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=20, region_to=225)],
            )
            for shop in (_Shops.CheapShop, _Shops.FastShop)
        ]

        cls.index.outlets += [
            Outlet(
                point_id=2000,
                delivery_service_id=157,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                working_days=list(range(15)),
                working_times=[
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.MONDAY,
                        days_till=OutletWorkingTime.SUNDAY,
                        hours_from='09:00',
                        hours_till='20:00',
                    )
                ],
                gps_coord=GpsCoord(37.12, 55.32),
                delivery_option=OutletDeliveryOption(
                    shipper_id=157, day_from=2, day_to=3, work_in_holiday=True, price=10
                ),
            ),
            Outlet(
                point_id=2001,
                delivery_service_id=157,
                region=213,
                point_type=Outlet.FOR_POST,
                post_code=123123,
                delivery_option=OutletDeliveryOption(
                    shipper_id=157, day_from=6, day_to=10, order_before=2, work_in_holiday=True, price=150
                ),
                working_days=list(range(20)),
                gps_coord=GpsCoord(36.87, 55.11),
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5000,
                dc_bucket_id=5000,
                fesh=_Shops.FastShop.fesh,
                carriers=[157],
                options=[PickupOption(outlet_id=2000, day_from=2, day_to=3, price=10)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5001,
                dc_bucket_id=5001,
                fesh=_Shops.CheapShop.fesh,
                carriers=[157],
                options=[PickupOption(outlet_id=2001, day_from=6, day_to=15, price=20)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

    @classmethod
    def prepare_defaultoffer_combinator(cls):
        def stats(data):
            return DeliveryStats(*data) if data is not None else data

        def get_outlet_types(pickup, post):
            result = []
            if pickup:
                result.extend((OutletType.FOR_PICKUP, OutletType.FOR_POST_TERM))
            if post:
                result.append(OutletType.FOR_POST)
            return result

        for offer, shop, courier, pickup, post in COMBINATOR_TEST_DATA:
            cls.combinator.add_offer_delivery(
                offer_ids=make_offer_id(offer, shop),
                courier_stats=stats(courier),
                pickup_stats=stats(pickup),
                post_stats=stats(post),
                outlet_types=get_outlet_types(pickup, post),
            )

    def test_defaultoffer_combinator(self):
        response = self.report.request_json("place=defaultoffer&hyperid={}&combinator=1&rids=213".format(1))
        self.assertFragmentIn(
            response,
            {
                "postStats": {"maxDays": 11, "maxPrice": {"value": "99"}, "minDays": 8, "minPrice": {"value": "99"}},
            },
        )

        # no region in request, than don't go to combi
        response = self.report.request_json("place=defaultoffer&hyperid={}&combinator=1".format(1))
        self.assertFragmentIn(
            response,
            {
                "postStats": Absent(),
            },
        )

        response = self.report.request_json("place=defaultoffer&hyperid={}&combinator=1&rids=213".format(2))
        self.assertFragmentIn(
            response,
            {
                "postStats": {"maxDays": 7, "maxPrice": {"value": "99"}, "minDays": 3, "minPrice": {"value": "99"}},
            },
        )

        response = self.report.request_json("place=defaultoffer&hyperid={}&combinator=1&rids=213".format(3))
        self.assertFragmentIn(
            response,
            {
                "postStats": {"maxDays": 15, "maxPrice": {"value": "99"}, "minDays": 6, "minPrice": {"value": "99"}},
            },
        )

        response = self.report.request_json("place=defaultoffer&hyperid={}&combinator=1&rids=213".format(4))
        self.assertFragmentIn(
            response,
            {
                "postStats": {"maxDays": 15, "maxPrice": {"value": "99"}, "minDays": 6, "minPrice": {"value": "99"}},
            },
        )

    def test_productoffers_combinator(self):
        response = self.report.request_json("place=productoffers&hyperid=1&combinator=1&rids=213")
        self.assertFragmentIn(
            response,
            {
                "postStats": {"maxDays": 11, "maxPrice": {"value": "99"}, "minDays": 8, "minPrice": {"value": "99"}},
            },
        )


if __name__ == '__main__':
    main()
