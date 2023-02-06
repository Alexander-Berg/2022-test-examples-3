#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BucketInfo,
    ComparisonOperation,
    CostModificationRule,
    Currency,
    DeliveryBucket,
    DeliveryCostCondition,
    DeliveryModifier,
    DeliveryModifierCondition,
    DeliveryOption,
    ExchangeRate,
    ModificationOperation,
    Offer,
    OfferDeliveryInfo,
    Promo,
    PromoType,
    Region,
    RegionalDelivery,
    RegionsAvailability,
    Shop,
    TimeModificationRule,
    ValueLimiter,
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

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=3, day_to=5)])],
                delivery_program=DeliveryBucket.DAAS,
            ),
            DeliveryBucket(
                bucket_id=2,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=2)])],
                delivery_program=DeliveryBucket.DAAS,
            ),
        ]

        cls.index.delivery_modifiers += [
            DeliveryModifier(
                action=CostModificationRule(operation=ModificationOperation.ADD, parameter=50), modifier_id=2
            ),
            DeliveryModifier(
                action=CostModificationRule(operation=ModificationOperation.ADD, parameter=10, currency=Currency.USD),
                modifier_id=7,
            ),
            DeliveryModifier(action=CostModificationRule(operation=ModificationOperation.UNKNOWN_VALUE), modifier_id=9),
        ]

        cls.index.delivery_modifiers += [
            DeliveryModifier(
                action=TimeModificationRule(operation=ModificationOperation.MULTIPLY, parameter=3), modifier_id=3
            ),
            DeliveryModifier(action=TimeModificationRule(operation=ModificationOperation.UNKNOWN_VALUE), modifier_id=4),
            DeliveryModifier(
                action=TimeModificationRule(
                    operation=ModificationOperation.MULTIPLY, parameter=3, limit=ValueLimiter(10, 14)
                ),
                modifier_id=8,
            ),
        ]

    @classmethod
    def prepare_disabling_delivery(cls):
        cls.index.delivery_modifiers += [
            DeliveryModifier(action=RegionsAvailability(False), modifier_id=1),
            DeliveryModifier(action=RegionsAvailability(True), modifier_id=10),
        ]

        cls.index.offers += [
            Offer(
                title='offer_1',
                delivery_info=OfferDeliveryInfo(
                    courier_buckets=[BucketInfo(bucket_id=1, region_availability_modifiers=[1])]
                ),
                has_delivery_options=False,
            )
        ]

        cls.index.offers += [
            Offer(
                title='offer_16',
                delivery_info=OfferDeliveryInfo(
                    courier_buckets=[BucketInfo(bucket_id=1, region_availability_modifiers=[1, 10])]
                ),
                has_delivery_options=False,
            )
        ]

    def test_disabling_delivery(self):
        response = self.report.request_json('place=prime&text=offer_1&rids=213&exact-match=1')
        self.assertFragmentIn(response, {'search': {'total': 0}})

        response = self.report.request_json('place=prime&text=offer_16&rids=213&exact-match=1')
        self.assertFragmentIn(response, {'search': {'total': 0}})

    @classmethod
    def prepare_cost_modifiers(cls):
        cls.index.currencies += [
            Currency(
                name=Currency.USD,
                exchange_rates=[
                    ExchangeRate(to=Currency.RUR, rate=60.0),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                title='offer_2',
                delivery_info=OfferDeliveryInfo(courier_buckets=[BucketInfo(bucket_id=1, cost_modifiers=[2])]),
            ),
            Offer(
                title='offer_9',
                delivery_info=OfferDeliveryInfo(courier_buckets=[BucketInfo(bucket_id=1, cost_modifiers=[7])]),
            ),
        ]

    def test_cost_modifier(self):
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json('place=prime&text=offer_2&rids=213&exact-match=1' + unified_off_flags)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer_2'},
                            'delivery': {'options': [{'price': {'currency': 'RUR', 'value': '150'}}]},
                        }
                    ],
                }
            },
        )

        response = self.report.request_json('place=prime&text=offer_9&rids=213&exact-match=1' + unified_off_flags)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer_9'},
                            'delivery': {'options': [{'price': {'currency': 'RUR', 'value': '700'}}]},
                        }
                    ],
                }
            },
        )

    @classmethod
    def prepare_time_modifiers(cls):
        cls.index.offers += [
            Offer(
                title='offer_3',
                delivery_info=OfferDeliveryInfo(courier_buckets=[BucketInfo(bucket_id=1, time_modifiers=[3])]),
            ),
            Offer(
                title='offer_7',
                delivery_info=OfferDeliveryInfo(courier_buckets=[BucketInfo(bucket_id=1, time_modifiers=[4])]),
            ),
            Offer(
                title='offer_11',
                delivery_info=OfferDeliveryInfo(courier_buckets=[BucketInfo(bucket_id=1, time_modifiers=[8])]),
            ),
        ]

    def test_time_modifier(self):
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json('place=prime&text=offer_3&rids=213&exact-match=1' + unified_off_flags)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer_3'},
                            'delivery': {'options': [{'dayFrom': 9, 'dayTo': 15}]},
                        }
                    ],
                }
            },
        )

        response = self.report.request_json('place=prime&text=offer_7&rids=213&exact-match=1' + unified_off_flags)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer_7'},
                            'delivery': {
                                'options': [
                                    {
                                        'price': {'currency': 'RUR', 'value': '100'},
                                        'dayFrom': Absent(),
                                        'dayTo': Absent(),
                                    }
                                ]
                            },
                        }
                    ],
                }
            },
        )

        response = self.report.request_json('place=prime&text=offer_11&rids=213&exact-match=1' + unified_off_flags)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer_11'},
                            'delivery': {'options': [{'dayFrom': 10, 'dayTo': 14}]},
                        }
                    ],
                }
            },
        )

    @classmethod
    def prepare_cost_condition(cls):
        cls.index.delivery_modifiers += [
            DeliveryModifier(
                action=CostModificationRule(operation=ModificationOperation.FIX_VALUE, parameter=1500),
                condition=DeliveryModifierCondition(
                    delivery_cost_condition=DeliveryCostCondition(
                        percent_from_offer_price=50, comparison_operation=ComparisonOperation.MORE
                    )
                ),
                modifier_id=5,
            ),
        ]

        cls.index.offers += [
            Offer(
                title='offer_4',
                delivery_info=OfferDeliveryInfo(courier_buckets=[BucketInfo(bucket_id=1, cost_modifiers=[5])]),
                price=100,
            ),
            Offer(
                title='offer_5',
                delivery_info=OfferDeliveryInfo(courier_buckets=[BucketInfo(bucket_id=1, cost_modifiers=[5])]),
                price=300,
            ),
            Offer(
                title='offer_6',
                delivery_info=OfferDeliveryInfo(courier_buckets=[BucketInfo(bucket_id=1, cost_modifiers=[5])]),
                price=400,
                promo_price=100,
                promo=Promo(promo_type=PromoType.FLASH_DISCOUNT, key='xMpCOKC5I4INzFCab3WEm1'),
            ),
        ]

    def test_cost_condition(self):
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'

        response = self.report.request_json('place=prime&text=offer_4&rids=213&exact-match=1' + unified_off_flags)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer_4'},
                            'delivery': {'options': [{'price': {'currency': 'RUR', 'value': '1500'}}]},
                        }
                    ],
                }
            },
        )

        response = self.report.request_json('place=prime&text=offer_5&rids=213&exact-match=1' + unified_off_flags)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer_5'},
                            'delivery': {'options': [{'price': {'currency': 'RUR', 'value': '100'}}]},
                        }
                    ],
                }
            },
        )

        response = self.report.request_json('place=prime&text=offer_6&rids=213&exact-match=1' + unified_off_flags)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer_6'},
                            'delivery': {'options': [{'price': {'currency': 'RUR', 'value': '1500'}}]},
                        }
                    ],
                }
            },
        )

    @classmethod
    def prepare_regions_condition(cls):
        cls.index.delivery_modifiers += [
            DeliveryModifier(
                action=CostModificationRule(operation=ModificationOperation.FIX_VALUE, parameter=1500),
                condition=DeliveryModifierCondition(regions=[216]),
                modifier_id=6,
            )
        ]

        cls.index.offers += [
            Offer(
                title='offer_8',
                delivery_info=OfferDeliveryInfo(courier_buckets=[BucketInfo(bucket_id=1, cost_modifiers=[6])]),
            )
        ]

    def test_regions_condition(self):
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json('place=prime&text=offer_8&rids=213&exact-match=1' + unified_off_flags)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer_8'},
                            'delivery': {'options': [{'price': {'currency': 'RUR', 'value': '100'}}]},
                        }
                    ],
                }
            },
        )

        response = self.report.request_json('place=prime&text=offer_8&rids=216&exact-match=1' + unified_off_flags)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer_8'},
                            'delivery': {'options': [{'price': {'currency': 'RUR', 'value': '1500'}}]},
                        }
                    ],
                }
            },
        )

    @classmethod
    def prepare_multiple_modifiers(cls):
        cls.index.offers += [
            Offer(
                title='offer_12',
                delivery_info=OfferDeliveryInfo(
                    courier_buckets=[BucketInfo(bucket_id=1, cost_modifiers=[7, 2], time_modifiers=[3, 4, 8])]
                ),
            ),
        ]

    def test_multiply_modifiers(self):
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json('place=prime&text=offer_12&rids=216&exact-match=1' + unified_off_flags)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer_12'},
                            'delivery': {
                                'options': [{'price': {'currency': 'RUR', 'value': '700'}, 'dayFrom': 9, 'dayTo': 15}]
                            },
                        }
                    ],
                }
            },
        )

    @classmethod
    def prepare_onstock(cls):
        cls.index.offers += [
            Offer(
                title='offer_13',
                delivery_info=OfferDeliveryInfo(courier_buckets=[BucketInfo(bucket_id=2)]),
            ),
            Offer(
                title='offer_14',
                delivery_info=OfferDeliveryInfo(courier_buckets=[BucketInfo(bucket_id=2, time_modifiers=[3])]),
            ),
        ]

    def test_onstock(self):
        """Тестируем правильность определения onstock"""

        # dayTo <= 2
        response = self.report.request_json('place=prime&text=offer_13&rids=216&exact-match=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer_13'},
                            'delivery': {
                                'options': [
                                    {'dayFrom': 1, 'dayTo': 2},
                                ],
                                'inStock': True,
                            },
                        }
                    ],
                }
            },
        )

        # dayTo > 2
        response = self.report.request_json('place=prime&text=offer_14&rids=216&exact-match=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'offer_14'},
                            'delivery': {'options': [{'dayFrom': 3, 'dayTo': 6}], 'inStock': False},
                        }
                    ],
                }
            },
        )

    @classmethod
    def prepare_stats_number(cls):
        cls.index.offers += [
            Offer(title='offer_15', delivery_info=OfferDeliveryInfo(courier_buckets=[BucketInfo(bucket_id=1)]), hid=10),
            Offer(
                title='offer_17',
                delivery_info=OfferDeliveryInfo(courier_buckets=[BucketInfo(bucket_id=1, cost_modifiers=[7, 2])]),
                hid=10,
            ),
            Offer(
                title='offer_18',
                delivery_info=OfferDeliveryInfo(
                    courier_buckets=[BucketInfo(bucket_id=1, region_availability_modifiers=[1])]
                ),
                hid=10,
            ),
        ]

    def test_stats_number(self):
        response = self.report.request_json('place=stat_numbers&delivery-stats=1&hid=10&rids=216&exact-match=1')
        self.assertFragmentIn(
            response, {'filters': {'DELIVERY': 1}, 'deliverableBy': {'courier': 2, 'pickup': 0, 'post': 0}}
        )

    @classmethod
    def prepare_unknown_delivery_price_is_hidden(cls):
        cls.index.shops += [Shop(fesh=1, regions=[213], priority_region=213)]

        cls.index.offers += [
            Offer(
                title='offer_19',
                delivery_info=OfferDeliveryInfo(courier_buckets=[BucketInfo(bucket_id=1)]),
                fesh=1,
                has_delivery_options=False,
                hyperid=19,
            ),
            Offer(
                title='offer_20',
                delivery_info=OfferDeliveryInfo(courier_buckets=[BucketInfo(bucket_id=1, cost_modifiers=[9])]),
                fesh=1,
                has_delivery_options=False,
                hyperid=20,
            ),
        ]

    def test_unknown_delivery_price_is_hidden(self):
        """Проверяем, что модификаторы цены с операцией UNKNOWN_VALUE, убирают цену из выдачи"""
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ('prime', 'productoffers'):
            response = self.report.request_json(
                'place={place}&hyperid=19&rids=213'.format(place=place) + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 1,
                        'results': [
                            {
                                'entity': 'offer',
                                'titles': {'raw': 'offer_19'},
                                'delivery': {
                                    'options': [{'price': {'currency': 'RUR', 'value': '100'}}],
                                    'price': {'currency': 'RUR', 'value': '100'},
                                },
                            }
                        ],
                    }
                },
            )

            response = self.report.request_json(
                'place={place}&hyperid=20&rids=213'.format(place=place) + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 1,
                        'results': [
                            {
                                'entity': 'offer',
                                'titles': {'raw': 'offer_20'},
                                'delivery': {'options': [{'price': Absent()}], 'price': Absent()},
                            }
                        ],
                    }
                },
            )

    def test_unknown_delivery_price_filtering(self):
        """Проверяем, что неизвестная цена доставки отфильтровывается фильтром free-delivery"""
        for place in ('prime', 'productoffers'):
            response = self.report.request_json('place={place}&hyperid=20&rids=213&free-delivery=1'.format(place=place))
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 0,
                    }
                },
            )

    @classmethod
    def prepare_unknown_delivery_price_options_compare(cls):
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=3,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=3, day_to=5)])],
                delivery_program=DeliveryBucket.DAAS,
            )
        ]
        cls.index.offers += [
            Offer(
                title='offer_21',
                delivery_info=OfferDeliveryInfo(
                    courier_buckets=[BucketInfo(bucket_id=1, cost_modifiers=[9]), BucketInfo(bucket_id=3)]
                ),
                fesh=1,
                has_delivery_options=False,
                hyperid=21,
            )
        ]

    def test_unknown_delivery_price_options_compare(self):
        """Проверяемм, что опция с неизвестной ценой доставки менее приоритетная, чем с фиксированной ценой"""
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ('prime', 'productoffers'):
            response = self.report.request_json(
                'place={place}&hyperid=21&rids=213'.format(place=place) + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 1,
                        'results': [
                            {
                                'entity': 'offer',
                                'titles': {'raw': 'offer_21'},
                                'delivery': {
                                    'options': [{'price': {'currency': 'RUR', 'value': '100'}}],
                                    'price': {'currency': 'RUR', 'value': '100'},
                                },
                            }
                        ],
                    }
                },
            )


if __name__ == '__main__':
    main()
