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

USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"

ALL_DAYS = 0
NO_DAYS = 1
ALL_INTERVALS = 0

lms_environment = [
    DynamicDaysSet(
        key=ALL_DAYS,
        days=[0, 1, 2, 3, 4, 5, 6, 7],
    ),
    DynamicDaysSet(
        key=NO_DAYS,
        days=[],
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
    DynamicWarehouseInfo(
        id=145,
        home_region=212,
        holidays_days_set_key=NO_DAYS,
    ),
    DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
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
        warehouse_id=145,
        delivery_service_id=157,
        operation_time=0,
        date_switch_time_infos=[
            DateSwitchTimeAndRegionInfo(
                date_switch_hour=19,
                region_to=212,
                date_switch_time=TimeInfo(19, 0),
                packaging_time=TimeInfo(3, 30),
            )
        ],
    ),
]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.lms_autogenerate = False

        cls.index.regiontree += [
            Region(
                rid=212,
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
                delivery_service_outlets=[9011, 9012, 9013],
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
                warehouse_id=145,
            ),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=9011,
                delivery_service_id=157,
                region=212,
                point_type=Outlet.FOR_POST_TERM,
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.7, 55.7),
                delivery_option=OutletDeliveryOption(shipper_id=157, day_from=3, day_to=5, price=400),
            ),
            Outlet(
                point_id=9012,
                delivery_service_id=157,
                region=212,
                point_type=Outlet.FOR_POST_TERM,
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.7, 55.7),
                delivery_option=OutletDeliveryOption(shipper_id=157, day_from=4, day_to=5, price=400),
            ),
            Outlet(
                point_id=9013,
                delivery_service_id=157,
                region=212,
                point_type=Outlet.FOR_POST_TERM,
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.7, 55.7),
                delivery_option=OutletDeliveryOption(shipper_id=157, day_from=4, day_to=6, price=400),
            ),
            Outlet(
                point_id=9020,
                delivery_service_id=157,
                region=212,
                point_type=Outlet.FOR_POST,
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.7, 55.7),
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

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=901,
                fesh=12,
                carriers=[157],
                options=[
                    PickupOption(outlet_id=9011, day_from=3, day_to=5, price=20),
                    PickupOption(outlet_id=9012, day_from=4, day_to=5, price=20),
                    PickupOption(outlet_id=9013, day_from=4, day_to=6, price=20),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=902,
                fesh=12,
                carriers=[157],
                options=[
                    PickupOption(outlet_id=9020, day_from=7, day_to=8, price=20),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
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
                post_buckets=[902],
                post_term_delivery=True,
            ),
        ]

        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=[
                BlueDeliveryTariff(user_price=99, large_size=0, price_to=3000),
                BlueDeliveryTariff(user_price=0, large_size=0),
                BlueDeliveryTariff(user_price=399, large_size=1),
            ],
            regions=[212],
            ya_plus_threshold=5,
            large_size_weight=20,
        )
        cls.index.blue_delivery_modifiers.set_default_modifier(tariffs=[BlueDeliveryTariff(user_price=99)])

        cls.dynamic.lms = lms_environment

    def test_shown_delivery_for_blue(self):
        """
        Проверяем, что для синего во всех основных плейсах с оферной выдачей в shows-log логируются сроки доставки
        из офера. В самовывозе берется максимальный диапазон по всем аутлетам.
        """

        color = "&rgb=blue"
        for utm, request in enumerate(
            [
                "place=prime&perks=yandex_plus&offerid=Sku1Price5-IiLVm1Goleg",
                "place=offerinfo&perks=yandex_plus&offerid=Sku1Price5-IiLVm1Goleg",
                "place=sku_offers&perks=yandex_plus&market-sku=1",
                "place=productoffers&perks=yandex_plus&hyperid=100",
                "place=actual_delivery&perks=yandex_plus&offers-list=Sku1Price5-IiLVm1Goleg:1",
            ]
        ):
            self.report.request_json(
                request
                + "&rids=212&regset=2&pickup-options=grouped&rearr-factors=market_nordstream_relevance=0"
                + color
                + "&utm_source={}".format(utm)
                + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
            )
            self.show_log.expect(
                utm_source=utm,
                url_type=6,
                ware_md5="Sku1Price5-IiLVm1Goleg",
                rgb="BLUE",
                delivered_to=212,
                warehouse_id=145,
                courier_day_to=2,
                courier_service_id=157,
                courier_price=0,
                courier_shop_price=10,
                courier_free_reason="yandex_plus",
                pickup_day_to_50p=5,
                pickup_day_to_80p=6,
                pickup_service_id=157,
                pickup_price_50p=0,
                pickup_price_80p=0,
                pickup_free_reason="yandex_plus",
                post_day_to=8,
                post_price=0,
            ).once()

    def test_debug_delivery_only_when_outlet_id_is_specified(self):
        '''
        Проверка того, что выводим доставочно-дебажную выдачу только для аутлетов из CGI-параметра outlets
        '''
        expected_logic_trace = {"logicTrace": [r"\[ME\].*? CalculateServiceDeliveryVirtual\(\): Shipment day = 0"]}

        common_suffix = (
            "&rgb=blue&rids=212&regset=2&pickup-options=grouped&debug=1&rearr-factors=market_nordstream_relevance=0"
            + "&rearr-factors=disable_delivery_calculator_call_for_blue_offers=0"
        )
        for place in (
            "place=prime&offerid=Sku1Price5-IiLVm1Goleg",
            "place=offerinfo&offerid=Sku1Price5-IiLVm1Goleg",
            "place=sku_offers&market-sku=1",
            "place=productoffers&hyperid=100",
        ):
            request = place + common_suffix + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
            self.assertFragmentNotIn(self.report.request_json(request), expected_logic_trace, use_regex=True)
            self.assertFragmentIn(
                self.report.request_json(request + "&outlets=9011"), expected_logic_trace, use_regex=True
            )

        # Для actual_delivery отображается расчёт курьерской доставки, независимо от указания аутлета
        actual_delivery_request = "place=actual_delivery&offers-list=Sku1Price5-IiLVm1Goleg:1" + common_suffix
        for suffix in ('', '&outlets=9011'):
            self.assertFragmentIn(
                self.report.request_json(actual_delivery_request + suffix), expected_logic_trace, use_regex=True
            )


if __name__ == "__main__":
    main()
