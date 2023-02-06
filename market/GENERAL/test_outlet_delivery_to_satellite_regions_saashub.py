#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Currency,
    DeliveryBucket,
    DeliveryCalcFeedInfo,
    GpsCoord,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Region,
    Shop,
)
from core.testcase import TestCase, main
from core.types.offer import OfferDimensions
from core.matcher import Absent, EmptyList, NotEmptyList
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax


NORMAL_OFFER = 'Sku1Price5-IiLVm1Goleg'
OFFER_FOR_MOSCOW_ONLY_NO_DELIVERY_TEST = 'Sku2Price5-IiLVm1Goleg'

"""
Tile A contains an outlet 2013 for region 213
Tile B contains an outlet 2014 for region 214
Tile C contains an outlet 2016 for region 216
"""
OUTLET_TILES = {
    2013: {"coord": {"x": 154, "y": 80, "zoom": 8}, "outlets": [{"id": "2013", "type": "pickup"}]},  # Tile A
    2014: {"coord": {"x": 155, "y": 81, "zoom": 8}, "outlets": [{"id": "2014", "type": "pickup"}]},  # Tile B
    2016: {"coord": {"x": 153, "y": 80, "zoom": 8}, "outlets": [{"id": "2016", "type": "pickup"}]},  # Tile C
}


class T(TestCase):
    use_saashub_with_satellite_regions = True

    @classmethod
    def prepare(cls):
        cls.settings.use_saashub_delivery = True
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']

        cls.index.regiontree += [
            Region(
                rid=1,
                name="Москва и Московская область",
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(
                        rid=213,
                        name="Москва",
                        region_type=Region.CITY,
                        children=[
                            Region(rid=216, name="Зеленоград", region_type=Region.CITY),
                            Region(
                                rid=20279,
                                name="Центральный административный округ",
                                region_type=Region.CITY_DISTRICT,
                                children=[
                                    Region(rid=120538, name="Пресненский район", region_type=Region.SECONDARY_DISTRICT),
                                ],
                            ),
                        ],
                    ),
                    Region(
                        rid=120999,
                        name="Городской округ Долгопрудный",
                        region_type=Region.SUBJECT_FEDERATION_DISTRICT,
                        children=[
                            Region(
                                rid=214,
                                name="Долгопрудный",
                                region_type=Region.CITY,
                                children=[
                                    Region(rid=214066, name="Шереметьевский", region_type=Region.CITY_DISTRICT),
                                ],
                            ),
                        ],
                    ),
                    Region(
                        rid=98611,
                        name="Солнечногорский район",
                        region_type=Region.SUBJECT_FEDERATION_DISTRICT,
                        children=[
                            Region(
                                rid=214048,
                                name="Городское поселение Солнечногорск",
                                region_type=Region.SETTLEMENT,
                                children=[
                                    Region(rid=10755, name="Солнечногорск", region_type=Region.CITY),
                                    Region(rid=119895, name="Сенеж", region_type=Region.VILLAGE),
                                ],
                            ),
                        ],
                    ),
                ],
            ),
            Region(
                rid=11095,
                name="Пензенская область",
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(
                        rid=120962,
                        name="Городской округ Пенза",
                        region_type=Region.SUBJECT_FEDERATION_DISTRICT,
                        children=[
                            Region(rid=49, name="Пенза", region_type=Region.CITY),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.shops += [
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
                delivery_service_outlets=[2013, 2014, 2015, 2016, 2049],
            ),
            Shop(
                fesh=2,
                datafeed_id=2,
                priority_region=213,
                name='blue_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue='REAL',
            ),
        ]

        cls.index.outlets += [
            # Pickup outlets below
            Outlet(
                point_id=2013,
                delivery_service_id=123,
                region=213,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(shipper_id=123, day_from=1, day_to=1, price=400),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),  # in tile A: &tile=154%2C80 &zoom=8
            ),
            Outlet(
                point_id=2014,
                delivery_service_id=123,
                region=214,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(shipper_id=123, day_from=1, day_to=1, price=400),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(38.12, 54.67),  # in tile B: &tile=155%2C81 &zoom=8
            ),
            Outlet(
                point_id=2015,
                delivery_service_id=123,
                region=215,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(shipper_id=123, day_from=1, day_to=1, price=400),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.7, 55.7),
            ),
            Outlet(
                point_id=2016,
                delivery_service_id=123,
                region=216,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(shipper_id=123, day_from=1, day_to=1, price=400),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(36.13, 55.45),  # in tile C: &tile=153%2C80 &zoom=8
            ),
            Outlet(
                point_id=2049,
                delivery_service_id=123,
                region=49,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(shipper_id=123, day_from=1, day_to=1, price=400),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.7, 55.7),
            ),
            # Post outlets below
            Outlet(
                point_id=3013,
                delivery_service_id=123,
                region=213,
                point_type=Outlet.FOR_POST,
                delivery_option=OutletDeliveryOption(shipper_id=123, day_from=1, day_to=1, price=400),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.7, 55.7),
            ),
            Outlet(
                point_id=3014,
                delivery_service_id=123,
                region=214,
                point_type=Outlet.FOR_POST,
                delivery_option=OutletDeliveryOption(shipper_id=123, day_from=1, day_to=1, price=400),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.7, 55.7),
            ),
        ]

        cls.index.pickup_buckets += [
            # Pickup bucket
            PickupBucket(
                bucket_id=5001,
                dc_bucket_id=5001,
                fesh=1,
                carriers=[123],
                options=[
                    PickupOption(outlet_id=2013),
                    PickupOption(outlet_id=2014),
                    # Dubna outlet 2015 is missing intentionally
                    PickupOption(outlet_id=2016),
                    PickupOption(outlet_id=2049),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            # Post bucket
            PickupBucket(
                bucket_id=5002,
                dc_bucket_id=5002,
                fesh=1,
                carriers=[123],
                options=[
                    PickupOption(outlet_id=3013),
                    PickupOption(outlet_id=3014),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            # Bucket for Moscow-only crash bug
            PickupBucket(
                bucket_id=5003,
                dc_bucket_id=5003,
                fesh=1,
                carriers=[123],
                options=[
                    PickupOption(outlet_id=2013),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.delivery_calc_feed_info += [
            DeliveryCalcFeedInfo(feed_id=1, pickupBuckets=[5001, 5002, 5003]),
        ]

        cls.index.mskus += [
            MarketSku(
                title="blue offer sku 1",
                sku=101010,
                hyperid=1,
                waremd5='Sku1-wdDXWsIiLVm1goleg',
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=2,
                        offerid='blue.offer.1',
                        waremd5=NORMAL_OFFER,
                        weight=5,
                        dimensions=OfferDimensions(length=20, width=30, height=10),
                    )
                ],
                pickup_buckets=[5001],
                post_buckets=[5002],
                post_term_delivery=True,
            ),
            # SKU for Moscow-only no delivery test
            MarketSku(
                title="blue offer sku 2",
                sku=202020,
                hyperid=1,
                waremd5='Sku2-wdDXWsIiLVm1goleg',
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=2,
                        offerid='blue.offer.2',
                        waremd5=OFFER_FOR_MOSCOW_ONLY_NO_DELIVERY_TEST,
                        weight=5,
                        dimensions=OfferDimensions(length=20, width=30, height=10),
                    )
                ],
                pickup_buckets=[5003],
                post_term_delivery=True,
            ),
        ]

        cls.delivery_calc.on_request_offer_buckets(weight=5, width=30, height=10, length=20).respond([], [5001], [5002])

        cls.index.delivery_buckets_saashub += cls.index.delivery_buckets
        cls.index.pickup_buckets_saashub += cls.index.pickup_buckets
        cls.index.new_pickup_buckets_saashub += cls.index.new_pickup_buckets

    def check_outlet_delivery_to_satellite_regions(
        self,
        enable_satellites,
        region,
        expected_outlets_bulks,
        use_post_as_pickup=False,
        testing_post_delivery=False,
        offer=NORMAL_OFFER,
    ):
        """
        Check if Moscow city outlets are enriched with outlets from all Moscow region and
        Moscow region outlets are enriched with Moscow city outlets
        This is done under experiment flag and only for Moscow specifically for now
        """
        request_template = (
            'place=actual_delivery&rgb=blue&offers-list={offer}:1&rids={rids}&regset=1&pickup-options=grouped&pickup-options-extended-grouping=1'
            '&rearr-factors=satellite_region_outlets_enabled={enable_satellites};rty_delivery_cart_with_satellite_regions=1;market_use_post_as_pickup={use_post_as_pickup}'
            '&force-use-delivery-calc={force_use_delivery_calc}'
        )

        for force_use_delivery_calc in (0, 1):
            request = request_template.format(
                offer=offer,
                rids=region,
                enable_satellites=1 if enable_satellites else 0,
                use_post_as_pickup=1 if use_post_as_pickup else 0,
                force_use_delivery_calc=force_use_delivery_calc,
            )
            if force_use_delivery_calc == 0:
                request += "&combinator=0"
            response = self.report.request_json(request)
            result_options_name = "postOptions" if testing_post_delivery and not use_post_as_pickup else "pickupOptions"
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "delivery": {
                                result_options_name: [
                                    {"outletIds": expected_outlets, "groupCount": len(expected_outlets)}
                                    for expected_outlets in expected_outlets_bulks
                                ]
                                if expected_outlets_bulks
                                else Absent(),
                            },
                        }
                    ],
                },
                allow_different_len=False,
            )

    def test_outlet_delivery_to_satellite_regions(self):
        """
        Check if outlets are enriched below only under experiment flag
        """

        # The following regions belong geographically Moscow city,
        # so they are enriched with Moscow region outlets (only Dolgoprudny that has its outlets)
        # 1) Moscow city
        self.check_outlet_delivery_to_satellite_regions(False, 213, [[2013]])
        self.check_outlet_delivery_to_satellite_regions(True, 213, [[2013, 2014]])
        # 2) Moscow city district TSAO
        self.check_outlet_delivery_to_satellite_regions(False, 20279, [[2013]])
        self.check_outlet_delivery_to_satellite_regions(True, 20279, [[2013, 2014]])
        # 3) Moscow secondary district Presnenskiy
        self.check_outlet_delivery_to_satellite_regions(False, 120538, [[2013]])
        self.check_outlet_delivery_to_satellite_regions(True, 120538, [[2013, 2014]])

        # The following regions belong geographically to Moscow region,
        # so they are enriched with Moscow city outlets: Moscow city itself
        # and Zelenograd (TODO: geographically it is in Moscow region https://st.yandex-team.ru/MARKETOUT-32346)
        # 1) Dolgoprudny subject district
        self.check_outlet_delivery_to_satellite_regions(False, 120999, [[2014]])
        self.check_outlet_delivery_to_satellite_regions(True, 120999, [[2014, 2013, 2016]])
        # 2) Dolgoprudny city
        self.check_outlet_delivery_to_satellite_regions(False, 214, [[2014]])
        self.check_outlet_delivery_to_satellite_regions(True, 214, [[2014, 2013, 2016]])
        # 3) Sheremet'evskiy city district
        self.check_outlet_delivery_to_satellite_regions(False, 214066, [[2014]])
        self.check_outlet_delivery_to_satellite_regions(True, 214066, [[2014, 2013, 2016]])
        # 4) Solnechnogorks subject district (doesn't have its own outlets)
        self.check_outlet_delivery_to_satellite_regions(False, 98611, None)
        self.check_outlet_delivery_to_satellite_regions(True, 98611, [[2013, 2016]])
        # 5) Solnechnogorks city settlement (doesn't have its own outlets)
        self.check_outlet_delivery_to_satellite_regions(False, 214048, None)
        self.check_outlet_delivery_to_satellite_regions(True, 214048, [[2013, 2016]])
        # 6) Solnechnogorks city (doesn't have its own outlets)
        self.check_outlet_delivery_to_satellite_regions(False, 10755, None)
        self.check_outlet_delivery_to_satellite_regions(True, 10755, [[2013, 2016]])
        # 7) Senezh village (doesn't have its own outlets)
        self.check_outlet_delivery_to_satellite_regions(False, 119895, None)
        self.check_outlet_delivery_to_satellite_regions(True, 119895, [[2013, 2016]])

        # Zelenograd is considered a region city, not a part of Moscow, so it is enriched with Moscow city outlets
        self.check_outlet_delivery_to_satellite_regions(False, 216, [[2016, 2013]])
        self.check_outlet_delivery_to_satellite_regions(True, 216, [[2016, 2013, 2014]])

        # Penza city outlets are not enriched
        self.check_outlet_delivery_to_satellite_regions(False, 49, [[2049]])
        self.check_outlet_delivery_to_satellite_regions(True, 49, [[2049]])

        # Post outlets in Dolgoprudny city are not enriched
        self.check_outlet_delivery_to_satellite_regions(
            False, 214, [[3014]], use_post_as_pickup=False, testing_post_delivery=True
        )
        self.check_outlet_delivery_to_satellite_regions(
            True, 214, [[3014]], use_post_as_pickup=False, testing_post_delivery=True
        )

        # Post outlets as pickup in Dolgoprudny city are not enriched
        self.check_outlet_delivery_to_satellite_regions(
            False, 214, [[2014], [3014]], use_post_as_pickup=True, testing_post_delivery=True
        )
        self.check_outlet_delivery_to_satellite_regions(
            True, 214, [[2016, 2013, 2014], [3014]], use_post_as_pickup=True, testing_post_delivery=True
        )

    def check_prime_has_results(
        self, enable_satellites, region, has_results, offer=OFFER_FOR_MOSCOW_ONLY_NO_DELIVERY_TEST
    ):
        """
        Check if place=prime returns results only with pickupOptions set
        """
        request = (
            'place=prime&pickup-options=grouped&pickup-options-extended-grouping=1&rgb=blue&offerid={}&rids={}'
            '&rearr-factors=satellite_region_outlets_enabled={}'.format(offer, region, 1 if enable_satellites else 0)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "offers": {
                            "items": [
                                {
                                    "delivery": {
                                        "hasPickup": True,
                                        "pickupOptions": NotEmptyList(),
                                    }
                                }
                            ]
                        }
                    }
                ]
                if has_results
                else EmptyList()
            },
        )

    def check_satellite_outlets_on_geo_place(self, enable_satellites, region, expected_outlets):
        """
        Check if place=geo response include outlets in appropriate tiles
        """
        request = (
            'place=geo&rids={}&require-geo-coords=1&fesh=1&rgb=blue'
            '&zoom=8&ontile=3000&show-outlet=tiles'
            '&rearr-factors=satellite_region_outlets_enabled={}'
            '&tile=154%2C80'
            '&tile=155%2C81'
            '&tile=153%2C80'.format(region, 1 if enable_satellites else 0)
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response, {"search": {"tiles": [OUTLET_TILES[expected_outlet] for expected_outlet in expected_outlets]}}
        )


if __name__ == '__main__':
    main()


if __name__ == '__main__':
    main()
