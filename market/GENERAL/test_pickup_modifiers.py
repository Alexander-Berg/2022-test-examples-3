#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent
from core.testcase import TestCase, main
from core.types import (
    BucketInfo,
    ComparisonOperation,
    CostModificationRule,
    DeliveryCostCondition,
    DeliveryModifier,
    DeliveryModifierCondition,
    Dimensions,
    ModificationOperation,
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
    RegionsAvailability,
    Shop,
    TimeModificationRule,
)


class T(TestCase):
    @classmethod
    def prepare(cls):
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
            )
        ]

        cls.index.new_pickup_buckets += [
            NewPickupBucket(
                bucket_id=1,
                region_groups=[
                    PickupRegionGroup(options=[NewPickupOption(price=220, day_from=1, day_to=3)], outlets=[1])
                ],
            ),
            NewPickupBucket(
                bucket_id=2,
                region_groups=[
                    PickupRegionGroup(options=[NewPickupOption(price=220, day_from=1, day_to=3)], outlets=[2])
                ],
            ),
        ]

        cls.index.delivery_modifiers += [
            DeliveryModifier(
                modifier_id=1, action=RegionsAvailability(False), condition=DeliveryModifierCondition(regions=[1])
            ),
            DeliveryModifier(
                modifier_id=2, action=RegionsAvailability(True), condition=DeliveryModifierCondition(regions=[213])
            ),
        ]

        cls.index.offers += [
            Offer(
                title='offer_1',
                delivery_info=OfferDeliveryInfo(
                    pickup_buckets=[BucketInfo(bucket_id=1, region_availability_modifiers=[1])]
                ),
                dimensions=OfferDimensions(width=10, height=10, length=10),
                hid=10,
            ),
            Offer(
                title='offer_2',
                delivery_info=OfferDeliveryInfo(
                    pickup_buckets=[BucketInfo(bucket_id=1, region_availability_modifiers=[2, 1])]
                ),
                dimensions=OfferDimensions(width=10, height=10, length=10),
                hid=11,
            ),
        ]

    def test_regions_availability_modifiers(self):
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ('prime', 'geo'):
            response = self.report.request_json(
                'place={place}&text=offer_1&exact-match=1&rids=213&pickup-options=raw'.format(place=place)
                + unified_off_flags
            )
            self.assertFragmentIn(response, {'total': 0})

            response = self.report.request_json(
                'place={place}&text=offer_2&exact-match=1&rids=213&pickup-options=raw'.format(place=place)
                + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    'total': 1,
                    'results': [
                        {
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
                                    }
                                ],
                            },
                            'outlet': {'entity': 'outlet', 'selfDeliveryRule': {'currency': 'RUR', 'cost': '220'}},
                        }
                    ],
                },
            )

        response = self.report.request_json('place=stat_numbers&delivery-stats=1&hid=10&rids=213' + unified_off_flags)
        self.assertFragmentIn(
            response, {'filters': {'DELIVERY': 1}, 'deliverableBy': {'courier': 0, 'pickup': 0, 'post': 0}}
        )

        response = self.report.request_json('place=stat_numbers&delivery-stats=1&hid=11&rids=213' + unified_off_flags)
        self.assertFragmentIn(response, {'filters': {}, 'deliverableBy': {'courier': 0, 'pickup': 1, 'post': 0}})

    @classmethod
    def prepare_cost_modifiers(cls):
        cls.index.delivery_modifiers += [
            DeliveryModifier(
                modifier_id=3, action=CostModificationRule(operation=ModificationOperation.ADD, parameter=50)
            ),
        ]

        cls.index.offers += [
            Offer(
                title='offer_3',
                delivery_info=OfferDeliveryInfo(pickup_buckets=[BucketInfo(bucket_id=1, cost_modifiers=[3])]),
            ),
        ]

    def test_cost_modifiers(self):
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ('prime', 'geo'):
            response = self.report.request_json(
                'place={place}&text=offer_3&exact-match=1&rids=213&pickup-options=raw'.format(place=place)
                + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'delivery': {
                        'hasPickup': True,
                        'pickupPrice': {
                            'currency': 'RUR',
                            'value': '270',
                        },
                        'pickupOptions': [
                            {
                                'price': {'currency': 'RUR', 'value': '270'},
                                'outlet': {'id': '1'},
                                'dayFrom': 1,
                                'dayTo': 3,
                            }
                        ],
                    },
                    'outlet': {'entity': 'outlet', 'selfDeliveryRule': {'currency': 'RUR', 'cost': '270'}},
                },
            )

    @classmethod
    def prepare_unknown_price(cls):
        cls.index.delivery_modifiers += [
            DeliveryModifier(modifier_id=4, action=CostModificationRule(operation=ModificationOperation.UNKNOWN_VALUE))
        ]

        cls.index.offers += [
            Offer(
                title='offer_4',
                delivery_info=OfferDeliveryInfo(pickup_buckets=[BucketInfo(bucket_id=1, cost_modifiers=[4])]),
                pickup_buckets=[1],
            )
        ]

    def test_unknown_price(self):
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ('prime', 'geo'):
            response = self.report.request_json(
                'place={place}&text=offer_4&exact-match=1&rids=213&pickup-options=raw'.format(place=place)
                + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'delivery': {
                        'hasPickup': True,
                        'pickupPrice': Absent(),
                        'pickupOptions': [
                            {
                                'price': Absent(),
                                'outlet': {'id': '1'},
                                'dayFrom': 1,
                                'dayTo': 3,
                            }
                        ],
                    },
                    'outlet': {'entity': 'outlet', 'selfDeliveryRule': {'cost': Absent()}},
                },
            )

    @classmethod
    def prepare_sorting_outlets_by_price(cls):
        cls.index.delivery_modifiers += [
            DeliveryModifier(
                modifier_id=5, action=CostModificationRule(operation=ModificationOperation.ADD, parameter=-100)
            )
        ]

        cls.index.outlets += [
            Outlet(
                point_id=2,
                region=213,
                delivery_option=OutletDeliveryOption(day_from=0, day_to=1, order_before=2, work_in_holiday=False),
                working_days=[i for i in range(10)],
                delivery_service_id=101,
                point_type=Outlet.FOR_PICKUP,
                dimensions=Dimensions(width=100, height=90, length=80),
            )
        ]

        cls.index.pickup_buckets += [
            PickupBucket(bucket_id=1, options=[PickupOption(price=220, day_from=1, day_to=3, outlet_id=2)])
        ]

        cls.index.offers += [
            Offer(
                title='offer_5',
                delivery_info=OfferDeliveryInfo(
                    pickup_buckets=[BucketInfo(bucket_id=1, cost_modifiers=[5]), BucketInfo(bucket_id=2)]
                ),
            ),
            Offer(
                title='offer_6',
                delivery_info=OfferDeliveryInfo(
                    pickup_buckets=[BucketInfo(bucket_id=1, cost_modifiers=[3]), BucketInfo(bucket_id=2)]
                ),
            ),
        ]

    def test_sorting_outlets_by_price(self):
        """Тестируем, что аутлеты сортируются с учетом применений модификаторов"""
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json(
            'place=prime&text=offer_5&exact-match=1&rids=213&pickup-options=raw&use-rids-location=1' + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'outlet': {'entity': 'outlet', 'id': '1'},
                'delivery': {'pickupOptions': [{'outlet': {'id': '1'}}, {'outlet': {'id': '2'}}]},
            },
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=prime&text=offer_6&exact-match=1&rids=213&pickup-options=raw&use-rids-location=1' + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'outlet': {'entity': 'outlet', 'id': '2'},
                'delivery': {'pickupOptions': [{'outlet': {'id': '2'}}, {'outlet': {'id': '1'}}]},
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_time_modifiers(cls):
        cls.index.delivery_modifiers += [
            DeliveryModifier(
                modifier_id=6, action=TimeModificationRule(operation=ModificationOperation.MULTIPLY, parameter=2)
            ),
        ]

        cls.index.offers += [
            Offer(
                title='offer_7',
                delivery_info=OfferDeliveryInfo(pickup_buckets=[BucketInfo(bucket_id=1, time_modifiers=[6])]),
            )
        ]

    def test_time_modifiers(self):
        for place in ('prime', 'geo'):
            response = self.report.request_json(
                'place=prime&text=offer_7&exact-match=1&rids=213&pickup-options=raw&use-rids-location=1'
            )
            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'delivery': {
                        'hasPickup': True,
                        'pickupOptions': [
                            {
                                'outlet': {'id': '1'},
                                'dayFrom': 2,
                                'dayTo': 6,
                            }
                        ],
                    },
                },
            )

    @classmethod
    def prepare_unknown_time(cls):
        cls.index.delivery_modifiers += [
            DeliveryModifier(modifier_id=7, action=TimeModificationRule(operation=ModificationOperation.UNKNOWN_VALUE))
        ]

        cls.index.offers += [
            Offer(
                title='offer_8',
                delivery_info=OfferDeliveryInfo(pickup_buckets=[BucketInfo(bucket_id=1, time_modifiers=[7])]),
            )
        ]

    def test_unknown_time(self):
        for place in ('prime', 'geo'):
            response = self.report.request_json(
                'place=prime&text=offer_8&exact-match=1&rids=213&pickup-options=raw&use-rids-location=1'
            )
            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'delivery': {
                        'hasPickup': True,
                        'pickupOptions': [
                            {
                                'outlet': {'id': '1'},
                                'dayFrom': Absent(),
                                'dayTo': Absent(),
                            }
                        ],
                    },
                },
            )

    @classmethod
    def prepare_cost_condition(cls):
        cls.index.delivery_modifiers += [
            DeliveryModifier(
                modifier_id=8,
                action=TimeModificationRule(operation=ModificationOperation.MULTIPLY, parameter=2),
                condition=DeliveryModifierCondition(
                    delivery_cost_condition=DeliveryCostCondition(
                        percent_from_offer_price=10, comparison_operation=ComparisonOperation.LESS
                    )
                ),
            )
        ]

        cls.index.offers += [
            Offer(
                title='offer_9',
                delivery_info=OfferDeliveryInfo(pickup_buckets=[BucketInfo(bucket_id=1, time_modifiers=[8])]),
                price=400,
            ),
            Offer(
                title='offer_10',
                delivery_info=OfferDeliveryInfo(pickup_buckets=[BucketInfo(bucket_id=1, time_modifiers=[8])]),
                price=40000,
            ),
        ]

    def test_cost_condition(self):
        for place in ('prime', 'geo'):
            response = self.report.request_json(
                'place=prime&text=offer_9&exact-match=1&rids=213&pickup-options=raw&use-rids-location=1'
            )
            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'delivery': {
                        'hasPickup': True,
                        'pickupOptions': [
                            {
                                'outlet': {'id': '1'},
                                'dayFrom': 1,
                                'dayTo': 3,
                            }
                        ],
                    },
                },
            )

            response = self.report.request_json(
                'place=prime&text=offer_10&exact-match=1&rids=213&pickup-options=raw&use-rids-location=1'
            )
            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'delivery': {
                        'hasPickup': True,
                        'pickupOptions': [
                            {
                                'outlet': {'id': '1'},
                                'dayFrom': 2,
                                'dayTo': 6,
                            }
                        ],
                    },
                },
            )


if __name__ == '__main__':
    main()
