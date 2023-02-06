#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BucketInfo,
    DeliveryBucket,
    Dimensions,
    GpsCoord,
    NewPickupBucket,
    NewPickupOption,
    Offer,
    OfferDeliveryInfo,
    OfferDimensions,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    PickupRegionGroup,
    Region,
    Shop,
    ShopBuckets,
)
from core.testcase import TestCase, main
from core.matcher import Absent


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                tz_offset=10800,
                children=[
                    Region(
                        rid=213,
                        name='Москва',
                        tz_offset=10800,
                        children=[
                            Region(rid=216, name='Зеленоград', tz_offset=10800),
                            Region(
                                rid=114619,
                                name='Новомосковский административный округ',
                                region_type=Region.FEDERAL_DISTRICT,
                                children=[
                                    Region(rid=10720, name='Внуково', region_type=Region.VILLAGE),
                                    Region(rid=21624, name='Щербинка', region_type=Region.CITY),
                                ],
                            ),
                        ],
                    ),
                    Region(rid=10758, name='Химки', tz_offset=10800),
                ],
            ),
        ]

        cls.index.shops += [Shop(fesh=1, delivery_service_outlets=[1])]

        cls.index.outlets += [
            Outlet(
                point_id=1,
                dimensions=Dimensions(width=100, height=90, length=80),
                region=213,
                delivery_option=OutletDeliveryOption(day_from=0, day_to=1, order_before=2, work_in_holiday=False),
                working_days=[i for i in range(10)],
                delivery_service_id=101,
                point_type=Outlet.FOR_PICKUP,
            ),
            Outlet(
                point_id=4,
                dimensions=Dimensions(width=100, height=90, length=80, dim_sum=150),
                region=213,
                delivery_option=OutletDeliveryOption(day_from=0, day_to=1, order_before=2, work_in_holiday=False),
                working_days=[i for i in range(10)],
                delivery_service_id=101,
                point_type=Outlet.FOR_PICKUP,
            ),
            Outlet(
                point_id=5,
                dimensions=Dimensions(width=100, height=90, length=80),
                region=213,
                delivery_option=OutletDeliveryOption(day_from=0, day_to=1, order_before=2, work_in_holiday=False),
                working_days=[i for i in range(10)],
                delivery_service_id=101,
                point_type=Outlet.FOR_POST,
            ),
        ]

        cls.index.new_pickup_buckets += [
            NewPickupBucket(
                bucket_id=1,
                region_groups=[
                    PickupRegionGroup(options=[NewPickupOption(price=220, day_from=1, day_to=3)], outlets=[1, 4])
                ],
            ),
            NewPickupBucket(
                bucket_id=3,
                region_groups=[
                    PickupRegionGroup(options=[NewPickupOption(price=220, day_from=1, day_to=3)], outlets=[5])
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                title='offer_1',
                delivery_info=OfferDeliveryInfo(pickup_buckets=[BucketInfo(bucket_id=1)]),
                dimensions=OfferDimensions(width=100, height=80, length=70),
                hyperid=101,
                hid=1,
            ),
            Offer(
                title='offer_2',
                delivery_info=OfferDeliveryInfo(pickup_buckets=[BucketInfo(bucket_id=1)]),
                dimensions=OfferDimensions(width=101, height=80, length=70),
                hyperid=102,
                hid=2,
            ),
            Offer(
                title='offer_4',
                delivery_info=OfferDeliveryInfo(post_buckets=[BucketInfo(bucket_id=3)]),
                dimensions=OfferDimensions(width=100, height=80, length=70),
                hyperid=103,
                hid=3,
            ),
            Offer(
                title='offer_5',
                delivery_info=OfferDeliveryInfo(post_buckets=[BucketInfo(bucket_id=3)]),
                dimensions=OfferDimensions(width=100, height=91, length=70),
                hyperid=104,
                hid=4,
            ),
        ]

    def test_filtering_by_dimensions(self):
        offer1_fragment = {
            'entity': 'offer',
            'delivery': {
                'hasPickup': True,
                'pickupPrice': {
                    'currency': 'RUR',
                    'value': '220',
                },
                'pickupOptions': [
                    {
                        'price': {'currency': 'RUR', 'value': '220'},
                        'outlet': {'id': '1'},
                        'dayFrom': 1,
                        'dayTo': 3,
                        'partnerType': 'daas',
                    }
                ],
            },
            'outlet': {'selfDeliveryRule': {'currency': 'RUR', 'cost': '220', 'partnerType': 'daas'}},
            'debug': {
                'factors': {
                    'ALL_OUTLETS': '1',
                    'ALL_SHIPPING_OUTLETS': '1',
                    'DEPOT_OUTLETS': '1',
                    'PICKUP_OUTLETS': '1',
                    'PICKUP_PRICE': '220',
                }
            },
        }

        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ('prime', 'geo'):
            response = self.report.request_json(
                'place={place}&text=offer_1&rids=213&exact-match=1&pickup-options=raw&debug=1'.format(place=place)
                + unified_off_flags
            )
            self.assertFragmentIn(response, {'search': {'total': 1, 'results': [offer1_fragment]}})
            self.assertFragmentNotIn(response, {'delivery': {'pickupOptions': [{'outlet': {'id': '2'}}]}})

            response = self.report.request_json(
                'place={place}&text=offer_2&rids=213&exact-match=1&pickup-options=raw'.format(place=place)
            )
            self.assertFragmentIn(response, {'search': {'total': 0}})

        response = self.report.request_json(
            'place=productoffers&hyperid=101&rids=213&pickup-options=raw&debug=1' + unified_off_flags
        )
        self.assertFragmentIn(response, {'search': {'total': 1, 'shopOutlets': 1, 'results': [offer1_fragment]}})

        response = self.report.request_json(
            'place=productoffers&hyperid=102&rids=213&pickup-options=raw' + unified_off_flags
        )
        self.assertFragmentIn(response, {'search': {'total': 0}})

    @classmethod
    def prepare_tiles(cls):
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
        #           |     *(2)     |    *(3)       |              |
        #           |              |               |              |
        #           |              |               |              |
        # 55.4(323) |--------------|---------------|--------------|---------------
        #           |              |               |              |
        #           |              |               |              |
        #           |              |               |              |
        #           |              |               |              |
        # 55.2(324) |--------------|---------------|--------------|---------------
        #           |              |               |              |
        #           |              |               |              |
        #           |              |               |              |
        #           |              |               |              |
        # 55.0(325) |--------------|---------------|--------------|---------------

        cls.index.outlets += [
            Outlet(
                point_id=2,
                region=213,
                gps_coord=GpsCoord(37.1, 55.5),
                dimensions=Dimensions(width=100, height=90, length=80),
            ),
            Outlet(
                point_id=3,
                region=213,
                gps_coord=GpsCoord(37.3, 55.5),
                dimensions=Dimensions(width=50, height=90, length=80),
            ),
        ]

        cls.index.new_pickup_buckets += [
            NewPickupBucket(
                bucket_id=2,
                region_groups=[
                    PickupRegionGroup(options=[NewPickupOption(price=220, day_from=1, day_to=3)], outlets=[2, 3])
                ],
            )
        ]

        cls.index.offers += [
            Offer(
                title='offer_3',
                delivery_info=OfferDeliveryInfo(pickup_buckets=[BucketInfo(bucket_id=2)]),
                dimensions=OfferDimensions(width=100, height=80, length=70),
            ),
        ]

    def test_tiles(self):
        response = self.report.request_json(
            'place=geo&text=offer_3&exact-match=1&tile=617,322&zoom=10&show-outlet=tiles&rids=213'
        )
        self.assertFragmentIn(
            response,
            {
                'shopOutlets': 1,
                'tiles': [
                    {
                        'entity': 'tile',
                        'coord': {'x': 617, 'y': 322, 'zoom': 10},
                        'outlets': [
                            {'entity': 'outlet', 'id': '2', 'gpsCoord': {'longitude': '37.1', 'latitude': '55.5'}}
                        ],
                    }
                ],
            },
            allow_different_len=False,
        )

    def test_stats_number(self):
        response = self.report.request_json('place=stat_numbers&delivery-stats=1&hid=1&rids=213')
        self.assertFragmentIn(
            response, {'filters': {'DELIVERY': Absent()}, 'deliverableBy': {'courier': 0, 'pickup': 1, 'post': 0}}
        )

        response = self.report.request_json('place=stat_numbers&delivery-stats=1&hid=2&rids=213')
        self.assertFragmentIn(
            response, {'filters': {'DELIVERY': 1}, 'deliverableBy': {'courier': 0, 'pickup': 0, 'post': 0}}
        )

        response = self.report.request_json('place=stat_numbers&delivery-stats=1&hid=3&rids=213')
        self.assertFragmentIn(
            response, {'filters': {'DELIVERY': Absent()}, 'deliverableBy': {'courier': 0, 'pickup': 0, 'post': 1}}
        )

        response = self.report.request_json('place=stat_numbers&delivery-stats=1&hid=4&rids=213')
        self.assertFragmentIn(
            response, {'filters': {'DELIVERY': 1}, 'deliverableBy': {'courier': 0, 'pickup': 0, 'post': 0}}
        )

    @classmethod
    def prepare_shop_outlets(cls):
        cls.index.shops += [Shop(fesh=2)]
        cls.index.shop_buckets += [ShopBuckets(shop_id=2, pickup_buckets=[1])]

    def test_shop_outlets(self):
        response = self.report.request_json('place=shop_info&fesh=2&shop-delivery=1')
        self.assertFragmentIn(
            response,
            {
                'outletCounts': {
                    'all': 2,
                    'pickup': 2,
                    'depot': 2,
                }
            },
        )

    @classmethod
    def prepare_not_daas_buckets_in_flatbuf(cls):
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=4,
                options=[PickupOption(day_from=1, day_to=3, outlet_id=1)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5,
                options=[PickupOption(day_from=1, day_to=2, outlet_id=2)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=6,
                options=[PickupOption(day_from=1, day_to=3, outlet_id=2)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [
            Offer(
                title='offer_6',
                delivery_info=OfferDeliveryInfo(pickup_buckets=[BucketInfo(bucket_id=4)]),
                pickup_buckets=[5],
            ),
            Offer(title='offer_7', delivery_info=OfferDeliveryInfo(pickup_buckets=[BucketInfo(bucket_id=6)]), hid=5),
        ]

    def test_not_daas_buckets_in_flatbuf(self):
        for place in ('prime', 'geo'):
            response = self.report.request_json(
                'place={place}&text=offer_6&rids=213&exact-match=1&pickup-options=raw'.format(place=place)
            )
            self.assertFragmentIn(
                response, {'entity': 'offer', 'delivery': {'pickupOptions': [{'outlet': {'id': '1'}}]}}
            )

        response = self.report.request_json(
            'place=geo&text=offer_7&exact-match=1&tile=617,322&zoom=10&show-outlet=tiles&rids=213'
        )
        self.assertFragmentIn(
            response,
            {
                'shopOutlets': 1,
                'tiles': [
                    {
                        'entity': 'tile',
                        'coord': {'x': 617, 'y': 322, 'zoom': 10},
                        'outlets': [
                            {'entity': 'outlet', 'id': '2', 'gpsCoord': {'longitude': '37.1', 'latitude': '55.5'}}
                        ],
                    }
                ],
            },
            allow_different_len=False,
        )

        response = self.report.request_json('place=stat_numbers&delivery-stats=1&hid=5&rids=213')
        self.assertFragmentIn(response, {'deliverableBy': {'courier': 0, 'pickup': 1, 'post': 0}})


if __name__ == '__main__':
    main()
