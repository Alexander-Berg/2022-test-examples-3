#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import datetime

from core.types import (
    BlueOffer,
    CreditGlobalRestrictions,
    CreditPlan,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
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
    Payment,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    Shop,
    Tax,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.types.delivery import OutletType, OutletWorkingTime, BlueDeliveryTariff
from core.testcase import TestCase, main
from core.combinator import DeliveryStats, make_offer_id
from core.matcher import Absent
from market.combinator.proto.grpc.combinator_pb2 import OfferDeliveryStats, PickupPointType
from google.protobuf.text_format import MessageToString


MARKET_OUTLET_THRESHOLD = 170


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
    VirtualBlue = Shop(
        fesh=3,
        datafeed_id=3,
        priority_region=213,
        tax_system=Tax.OSN,
        fulfillment_virtual=True,
        cpa=Shop.CPA_REAL,
        virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
        delivery_service_outlets=[2000],
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


class _Mskus:
    CheapAndFastMsku = MarketSku(
        title="КомбиВзрыв", hyperid=1, sku=1, blue_offers=[_Offers.CheapOffer, _Offers.FastOffer]
    )
    VagueMsku = MarketSku(title="Второй", hyperid=2, sku=2, blue_offers=[_Offers.VagueOffer])
    CorrectMsku = MarketSku(title="Comparison", hyperid=3, sku=3, blue_offers=[_Offers.CorrectOffer])
    FreeDeliveryMsku = MarketSku(title="Free Delivery", hyperid=4, sku=4, blue_offers=[_Offers.FreeDeliveryOffer])


COMBINATOR_SKU_TEST_DATA = (
    (
        _Offers.FastOffer,
        _Shops.FastShop,
        _Mskus.CheapAndFastMsku,
        DeliveryStats(cost=0, day_from=4, day_to=5),  # courier
        DeliveryStats(cost=10, day_from=2, day_to=3),  # pickup
        None,  # post
        DeliveryStats(cost=20, day_from=1, day_to=1),  # avia
        DeliveryStats(cost=20, day_from=2, day_to=2),  # market_pickup
        None,  # external_pickup
    ),
    (
        _Offers.CheapOffer,
        _Shops.CheapShop,
        _Mskus.CheapAndFastMsku,
        None,  # courier
        None,  # pickup
        DeliveryStats(cost=20, day_from=8, day_to=11),  # post
        DeliveryStats(cost=20, day_from=1, day_to=3),  # avia
        None,  # market_pickup
        DeliveryStats(cost=20, day_from=2, day_to=4),  # external_pickup
    ),
    (
        _Offers.VagueOffer,
        _Shops.CheapShop,
        _Mskus.VagueMsku,
        DeliveryStats(cost=0, day_from=2, day_to=4),  # courier
        None,  # pickup
        DeliveryStats(cost=40, day_from=3, day_to=7),  # post
        None,
        None,
        None,
    ),
    (
        _Offers.CorrectOffer,
        _Shops.FastShop,
        _Mskus.CorrectMsku,
        DeliveryStats(cost=5, day_from=1, day_to=2),  # courier
        DeliveryStats(cost=10, day_from=2, day_to=3),  # pickup
        DeliveryStats(cost=20, day_from=6, day_to=15),  # post
        DeliveryStats(cost=20, day_from=1, day_to=1),  # avia
        None,
        None,
    ),
    (
        _Offers.FreeDeliveryOffer,
        _Shops.FastShop,
        _Mskus.FreeDeliveryMsku,
        DeliveryStats(cost=5, day_from=1, day_to=2),  # courier
        DeliveryStats(cost=10, day_from=2, day_to=3),  # pickup
        DeliveryStats(cost=20, day_from=6, day_to=15),  # post
        None,
        DeliveryStats(cost=20, day_from=2, day_to=2),  # market_pickup
        DeliveryStats(cost=20, day_from=2, day_to=4),  # external_pickup
    ),
)


class T(TestCase):
    @classmethod
    def beforePrepare(cls):
        cls.settings.delivery_calendar_start_date = datetime.date(day=18, month=5, year=2020)

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
            _Mskus.CheapAndFastMsku,
            _Mskus.VagueMsku,
            _Mskus.CorrectMsku,
            _Mskus.FreeDeliveryMsku,
        ]
        cls.index.shops += [_Shops.FastShop, _Shops.CheapShop, _Shops.VirtualBlue]
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

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=800,
                dc_bucket_id=800,
                fesh=_Shops.VirtualBlue.fesh,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=rid,
                        options=[DeliveryOption(price=5, day_from=1, day_to=2)],
                        payment_methods=[Payment.PT_YANDEX],
                    )
                    for rid in (213, 10758, 192)
                ],
            )
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5000,
                dc_bucket_id=5000,
                fesh=_Shops.VirtualBlue.fesh,
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

        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=[BlueDeliveryTariff(user_price=0, large_size=0, for_plus=1), BlueDeliveryTariff(user_price=99)],
            regions=[],
            market_outlet_threshold=MARKET_OUTLET_THRESHOLD,
            exp_name='unified',
        )

    @classmethod
    def prepare_credit_plans(cls):
        cls.index.credit_plans_container.global_restrictions = CreditGlobalRestrictions(min_price=1, max_price=100000)
        cls.index.credit_plans_container.credit_plans = [
            CreditPlan(
                plan_id='ShutUpAndTakeMyMoney',
                bank='Combankator',
                term=12,
                rate=12.3,
                initial_payment_percent=0,
                max_price=30000,
            )
        ]

    @classmethod
    def prepare_sku_offers(cls):
        def stats(data):
            return DeliveryStats(*data) if data is not None else data

        def get_outlet_types(pickup, post):
            result = []
            if pickup:
                result.extend((OutletType.FOR_PICKUP, OutletType.FOR_POST_TERM))
            if post:
                result.append(OutletType.FOR_POST)
            return result

        for offer, shop, _, courier, pickup, post, avia, market_pickup, external_pickup in COMBINATOR_SKU_TEST_DATA:
            cls.combinator.add_offer_delivery(
                offer_ids=make_offer_id(offer, shop),
                courier_stats=stats(courier),
                pickup_stats=stats(pickup),
                post_stats=stats(post),
                avia_stats=stats(avia),
                market_pickup_stats=stats(market_pickup),
                external_pickup_stats=stats(external_pickup),
                outlet_types=get_outlet_types(pickup, post),
            )

    def __combinator_offer_responses(self, expected_data):
        def fill_stats(src, dst):
            def fill_date(days, dst):
                date = self.settings.delivery_calendar_start_date + datetime.timedelta(days=days)
                dst.day = date.day
                dst.month = date.month
                dst.year = date.year

            dst.cost = src.cost
            fill_date(src.day_from, dst.date_from)
            fill_date(src.day_to, dst.date_to)

        result = []
        for offer, shop, _, courier, pickup, post, _, _, _ in expected_data:
            expected = OfferDeliveryStats()
            expected.offer.shop_sku = offer.offerid
            expected.offer.shop_id = shop.fesh
            expected.offer.partner_id = shop.warehouse_id
            expected.offer.available_count = 1
            expected.offer.feed_id = shop.datafeed_id

            if courier:
                fill_stats(courier, expected.courier_stats)
            if pickup:
                fill_stats(pickup, expected.pickup_stats)
                expected.pickup_point_types.extend([PickupPointType.SERVICE_POINT])
            if post:
                fill_stats(post, expected.post_stats)
                expected.pickup_point_types.extend([PickupPointType.POST_OFFICE])
            if pickup:
                expected.pickup_point_types.extend([PickupPointType.PARCEL_LOCKER])

            expected_string = MessageToString(expected, as_one_line=True, indent=0)
            result.append(
                {"logicTrace": [r"\[ME\].*? CalculateDelivery\(\): Combinator response:.*?" + expected_string]}
            )

        return result

    def __check_combinator_response(self, expected_data, response, enable_combinator):
        for expected in self.__combinator_offer_responses(expected_data):
            if enable_combinator:
                self.assertFragmentIn(response, expected, use_regex=True)
            else:
                self.assertFragmentNotIn(response, expected, use_regex=True)

    def test_sku_multi_offer_request(self):
        rearr_factors = "&rearr-factors=market_cart_multi_offer=1"
        request_template = "place=sku_offers&rgb=blue&rids=213&market-sku=1&debug=1{rearr}"
        request = request_template.format(rearr=rearr_factors)
        for offer_data in COMBINATOR_SKU_TEST_DATA[:2]:
            request += "&offerid={}".format(offer_data[0].waremd5)
        response = self.report.request_json(request)
        self.__check_combinator_response(COMBINATOR_SKU_TEST_DATA[:2], response, True)

    def test_multi_sku_offers_request(self):
        request = "place=sku_offers&rgb=blue&rids=213&rearr-factors=market_cart_multi_offer=1&debug=1&" + '&'.join(
            ('market-sku={}'.format(i) for i in range(1, 5))
        )
        for offer_data in COMBINATOR_SKU_TEST_DATA:
            request += "&offerid={}".format(offer_data[0].waremd5)
        response = self.report.request_json(request)
        self.__check_combinator_response(COMBINATOR_SKU_TEST_DATA, response, True)

    def test_avia_courier_delivery(self):
        """
        Проверяется, что на КМ корректно приходят опции авиадоставки
        """
        request = "place=sku_offers&rgb=blue&rids=213&market-sku={}&offerid={}&rearr-factors=avia_delivery={}"

        for enable_avia in [1, 0]:
            for offer, _, msku, courier, _, _, avia, _, _ in COMBINATOR_SKU_TEST_DATA:
                response = self.report.request_json(request.format(msku.sku, offer.waremd5, enable_avia))
                options = []
                if courier is not None:
                    options.append(
                        {
                            "price": {
                                "currency": "RUR",
                                "value": "99",
                            },
                            "dayFrom": courier.day_from,
                            "dayTo": courier.day_to,
                            "serviceId": "99",
                            "partnerType": "market_delivery",
                        }
                    )
                if avia is not None and enable_avia:
                    options.append(
                        {
                            "price": {
                                "currency": "RUR",
                                "value": "20",
                            },
                            "dayFrom": avia.day_from,
                            "dayTo": avia.day_to,
                            "serviceId": "99",
                            "partnerType": "market_delivery",
                        }
                    )
                self.assertFragmentIn(
                    response,
                    {
                        "search": {
                            "results": [
                                {
                                    "entity": "sku",
                                    "offers": {
                                        "items": [
                                            {
                                                "delivery": {
                                                    "options": options if options else [],
                                                }
                                            }
                                        ]
                                    },
                                }
                            ]
                        }
                    },
                    allow_different_len=False,
                )

    def test_market_pickup_delivery(self):
        """
        Проверяется, что на КМ корректно обрабатывается статистика для маркетных и внешних аутлетов
        """
        request = "place=sku_offers&rgb=blue&rids=213&market-sku={}&offerid={}&pickup-options=grouped&rearr-factors=use_market_pickup_stats={}"

        for use_market_stats in [1, 0]:
            for offer, _, msku, _, pickup, _, _, market_stats, external_stats in COMBINATOR_SKU_TEST_DATA:
                response = self.report.request_json(request.format(msku.sku, offer.waremd5, use_market_stats))
                options = []
                if pickup is not None and not use_market_stats:
                    options.append(
                        {
                            "price": {
                                "currency": "RUR",
                                "value": "99",
                            },
                            "dayFrom": pickup.day_from,
                            "dayTo": pickup.day_to,
                        }
                    )
                for stats, price in [
                    (market_stats, '0' if offer.price > MARKET_OUTLET_THRESHOLD else '99'),
                    (external_stats, '99'),
                ]:
                    if stats is not None and use_market_stats:
                        options.append(
                            {
                                "price": {
                                    "currency": "RUR",
                                    "value": price,
                                },
                                "dayFrom": stats.day_from,
                                "dayTo": stats.day_to,
                            }
                        )
                self.assertFragmentIn(
                    response,
                    {
                        "search": {
                            "results": [
                                {
                                    "entity": "sku",
                                    "offers": {
                                        "items": [
                                            {
                                                "delivery": {
                                                    "pickupOptions": options if options else Absent(),
                                                    "hasPickup": bool(options),
                                                }
                                            }
                                        ]
                                    },
                                }
                            ]
                        }
                    },
                    allow_different_len=False,
                )


if __name__ == '__main__':
    main()
