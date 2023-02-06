#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa
from datetime import (
    datetime,
    time,
    timedelta,
)

from core.report import REQUEST_TIMESTAMP
from core.testcase import (
    TestCase,
    main,
)
from core.types import BlueOffer, GpsCoord, MarketSku, Region, Shop, Tax
from core.types.combinator import (
    CombinatorOffer,
    create_delivery_option,
    create_user_info,
    DeliveryItem,
    DeliverySubtype,
    Destination,
)
from core.types.offer import OfferDimensions


DATETIME_NOW = datetime.fromtimestamp(REQUEST_TIMESTAMP)


class _Constants:
    moscow_rids = 213

    model_id = 1
    category_id = 1

    virtual_blue_fesh = 1
    virtual_blue_feed_id = 1

    third_party_fesh = 2
    third_party_feed_id = 2

    ff_warehouse_id = 10

    courier_delivery_service_id = 100
    on_demand_delivery_service_id = 101

    delivery_cost = 50

    first_day_from = 0
    first_day_to = 0
    first_date_from = DATETIME_NOW + timedelta(days=first_day_from)
    first_date_to = DATETIME_NOW + timedelta(days=first_day_to)

    second_day_from = 1
    second_day_to = 1
    second_date_from = DATETIME_NOW + timedelta(days=second_day_from)
    second_date_to = DATETIME_NOW + timedelta(days=second_day_to)

    morning_time_from = time(10, 0)
    morning_time_to = time(12, 0)

    midday_time_from = time(13, 0)
    midday_time_to = time(15, 0)

    evening_time_from = time(16, 0)
    evening_time_to = time(18, 0)

    user_gps = GpsCoord(lon=41.920925, lat=54.343961)


class _Requests:
    actual_delivery_request = (
        'place=actual_delivery'
        '&pp=18'
        '&rgb=blue'
        '&pickup-options=grouped'
        '&pickup-options-extended-grouping=1'
        '&combinator=1'
        '&logged-in=1'
        '&gps=lat:{lat};lon:{lon}'
        '&rids={rids}'
        '&offers-list={offers}'
        '&rearr-factors=enable_flat_courier_options={flag}'
    )


class _Shops:
    virtual_blue_shop = Shop(
        fesh=_Constants.virtual_blue_fesh,
        datafeed_id=_Constants.virtual_blue_feed_id,
        priority_region=_Constants.moscow_rids,
        tax_system=Tax.OSN,
        fulfillment_virtual=True,
        cpa=Shop.CPA_REAL,
        virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
    )

    third_party_shop = Shop(
        fesh=_Constants.third_party_fesh,
        datafeed_id=_Constants.third_party_feed_id,
        warehouse_id=_Constants.ff_warehouse_id,
        priority_region=_Constants.moscow_rids,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        fulfillment_program=True,
        direct_shipping=True,
    )


class _BlueOffers:
    third_party_offer = BlueOffer(
        offerid='third_party_sku1',
        waremd5='ThirdPartyWaremd5____w',
        price=30,
        feedid=_Constants.third_party_feed_id,
        weight=5,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants.third_party_fesh,
    )

    third_party_msku = MarketSku(
        title="Обычный 3P оффер",
        hyperid=_Constants.category_id,
        sku=1,
        blue_offers=[third_party_offer],
    )


class _CombinatorOffers:
    third_party_offer = CombinatorOffer(
        shop_sku=_BlueOffers.third_party_offer.offerid,
        shop_id=_Constants.third_party_fesh,
        partner_id=_Constants.ff_warehouse_id,
        available_count=1,
    )


class T(TestCase):
    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [
            Region(
                rid=_Constants.moscow_rids,
                name="Москва",
            )
        ]

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [
            _Shops.virtual_blue_shop,
            _Shops.third_party_shop,
        ]

    @classmethod
    def prepare_combinator(cls):
        report_start_time_in_ms = REQUEST_TIMESTAMP * 10**6
        cls.combinator.set_start_date(microseconds_for_disabled_random=report_start_time_in_ms)
        cls.settings.default_search_experiment_flags += ['enable_dsbs_combinator_request_in_actual_delivery=0']

        cls.combinator.on_courier_options_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=5000,
                    dimensions=[30, 30, 30],
                    cargo_types=[],
                    offers=[_CombinatorOffers.third_party_offer],
                    price=_BlueOffers.third_party_offer.price,
                )
            ],
            destination=Destination(region_id=_Constants.moscow_rids, gps_coords=_Constants.user_gps),
            payment_methods=[],
            user_info=create_user_info(logged_in=True),
            total_price=_BlueOffers.third_party_offer.price,
        ).respond_with_courier_options(
            options=[
                # опции доставки обычной курьерской службой (DelivertSubtype.ORDINARY)
                create_delivery_option(
                    cost=_Constants.delivery_cost,
                    date_from=_Constants.first_date_from,
                    date_to=_Constants.first_date_to,
                    time_from=_Constants.morning_time_from,
                    time_to=_Constants.morning_time_to,
                    delivery_service_id=_Constants.courier_delivery_service_id,
                ),
                create_delivery_option(
                    cost=_Constants.delivery_cost,
                    date_from=_Constants.first_date_from,
                    date_to=_Constants.first_date_to,
                    time_from=_Constants.midday_time_from,
                    time_to=_Constants.midday_time_to,
                    delivery_service_id=_Constants.courier_delivery_service_id,
                ),
                create_delivery_option(
                    cost=_Constants.delivery_cost,
                    date_from=_Constants.second_date_from,
                    date_to=_Constants.second_date_to,
                    time_from=_Constants.evening_time_from,
                    time_to=_Constants.evening_time_to,
                    delivery_service_id=_Constants.courier_delivery_service_id,
                ),
                # опции доставки по клику (DelivertSubtype.ON_DEMAND)
                create_delivery_option(
                    cost=_Constants.delivery_cost,
                    date_from=_Constants.first_date_from,
                    date_to=_Constants.first_date_to,
                    time_from=_Constants.morning_time_from,
                    time_to=_Constants.morning_time_to,
                    delivery_service_id=_Constants.on_demand_delivery_service_id,
                    delivery_subtype=DeliverySubtype.ON_DEMAND,
                ),
                create_delivery_option(
                    cost=_Constants.delivery_cost,
                    date_from=_Constants.first_date_from,
                    date_to=_Constants.first_date_to,
                    time_from=_Constants.midday_time_from,
                    time_to=_Constants.midday_time_to,
                    delivery_service_id=_Constants.on_demand_delivery_service_id,
                    delivery_subtype=DeliverySubtype.ON_DEMAND,
                ),
                create_delivery_option(
                    cost=_Constants.delivery_cost,
                    date_from=_Constants.second_date_from,
                    date_to=_Constants.second_date_to,
                    time_from=_Constants.midday_time_from,
                    time_to=_Constants.midday_time_to,
                    delivery_service_id=_Constants.on_demand_delivery_service_id,
                    delivery_subtype=DeliverySubtype.ON_DEMAND,
                ),
                create_delivery_option(
                    cost=_Constants.delivery_cost,
                    date_from=_Constants.second_date_from,
                    date_to=_Constants.second_date_to,
                    time_from=_Constants.evening_time_from,
                    time_to=_Constants.evening_time_to,
                    delivery_service_id=_Constants.on_demand_delivery_service_id,
                    delivery_subtype=DeliverySubtype.ON_DEMAND,
                ),
            ]
        )

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.mskus += [_BlueOffers.third_party_msku]

    def test_actual_delivery(self):
        """
        Проверям, что в 'place=actual_delivery' Репорта группировка по 'date_from' курьерских
        опций доставки не происходит при включенном rearr-флаге 'enable_flat_courier_options'
        """

        # группировка есть по умолчанию
        response = self.report.request_json(
            _Requests.actual_delivery_request.format(
                lat=_Constants.user_gps.latitude,
                lon=_Constants.user_gps.longitude,
                rids=_Constants.moscow_rids,
                offers='{}:1'.format(_BlueOffers.third_party_offer.waremd5),
                flag=0,
            )
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'deliveryGroup',
                        'delivery': {
                            'options': [
                                {
                                    'dayFrom': _Constants.first_day_from,
                                    'dayTo': _Constants.first_day_to,
                                    'timeIntervals': [
                                        {
                                            'from': '10:00',
                                            'to': '12:00',
                                            'isDefault': True,
                                        },
                                        {
                                            'from': '13:00',
                                            'to': '15:00',
                                            'isDefault': False,
                                        },
                                    ],
                                    'isDefault': True,
                                },
                                {
                                    'dayFrom': _Constants.second_day_from,
                                    'dayTo': _Constants.second_day_to,
                                    'timeIntervals': [
                                        {
                                            'from': '16:00',
                                            'to': '18:00',
                                            'isDefault': True,
                                        }
                                    ],
                                    'isDefault': False,
                                },
                                {
                                    'dayFrom': _Constants.first_day_from,
                                    'dayTo': _Constants.first_day_to,
                                    'timeIntervals': [
                                        {
                                            'from': '10:00',
                                            'to': '12:00',
                                            'isDefault': True,
                                        },
                                        {
                                            'from': '13:00',
                                            'to': '15:00',
                                            'isDefault': False,
                                        },
                                    ],
                                    'isDefault': False,
                                    'isOnDemand': True,
                                },
                                {
                                    'dayFrom': _Constants.second_day_from,
                                    'dayTo': _Constants.second_day_to,
                                    'timeIntervals': [
                                        {
                                            'from': '13:00',
                                            'to': '15:00',
                                            'isDefault': True,
                                        },
                                        {
                                            'from': '16:00',
                                            'to': '18:00',
                                            'isDefault': False,
                                        },
                                    ],
                                    'isDefault': False,
                                    'isOnDemand': True,
                                },
                            ],
                        },
                        'offers': [
                            {
                                'entity': 'offer',
                                'wareId': _BlueOffers.third_party_offer.waremd5,
                            }
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

        # группировки нет при включенном флаге
        response = self.report.request_json(
            _Requests.actual_delivery_request.format(
                lat=_Constants.user_gps.latitude,
                lon=_Constants.user_gps.longitude,
                rids=_Constants.moscow_rids,
                offers='{}:1'.format(_BlueOffers.third_party_offer.waremd5),
                flag=1,
            )
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'deliveryGroup',
                        'delivery': {
                            'options': [
                                {
                                    'dayFrom': _Constants.first_day_from,
                                    'dayTo': _Constants.first_day_to,
                                    'timeIntervals': [
                                        {
                                            'from': '10:00',
                                            'to': '12:00',
                                            'isDefault': True,
                                        }
                                    ],
                                    'isDefault': True,
                                },
                                {
                                    'dayFrom': _Constants.first_day_from,
                                    'dayTo': _Constants.first_day_to,
                                    'timeIntervals': [
                                        {
                                            'from': '13:00',
                                            'to': '15:00',
                                            'isDefault': False,
                                        }
                                    ],
                                    'isDefault': True,
                                },
                                {
                                    'dayFrom': _Constants.second_day_from,
                                    'dayTo': _Constants.second_day_to,
                                    'timeIntervals': [
                                        {
                                            'from': '16:00',
                                            'to': '18:00',
                                            'isDefault': True,
                                        }
                                    ],
                                    'isDefault': False,
                                },
                                {
                                    'dayFrom': _Constants.first_day_from,
                                    'dayTo': _Constants.first_day_to,
                                    'timeIntervals': [
                                        {
                                            'from': '10:00',
                                            'to': '12:00',
                                            'isDefault': True,
                                        }
                                    ],
                                    'isDefault': False,
                                    'isOnDemand': True,
                                },
                                {
                                    'dayFrom': _Constants.first_day_from,
                                    'dayTo': _Constants.first_day_to,
                                    'timeIntervals': [
                                        {
                                            'from': '13:00',
                                            'to': '15:00',
                                            'isDefault': False,
                                        }
                                    ],
                                    'isDefault': False,
                                    'isOnDemand': True,
                                },
                                {
                                    'dayFrom': _Constants.second_day_from,
                                    'dayTo': _Constants.second_day_to,
                                    'timeIntervals': [
                                        {
                                            'from': '13:00',
                                            'to': '15:00',
                                            'isDefault': True,
                                        }
                                    ],
                                    'isDefault': False,
                                    'isOnDemand': True,
                                },
                                {
                                    'dayFrom': _Constants.second_day_from,
                                    'dayTo': _Constants.second_day_to,
                                    'timeIntervals': [
                                        {
                                            'from': '16:00',
                                            'to': '18:00',
                                            'isDefault': False,
                                        }
                                    ],
                                    'isDefault': False,
                                    'isOnDemand': True,
                                },
                            ],
                        },
                        'offers': [
                            {
                                'entity': 'offer',
                                'wareId': _BlueOffers.third_party_offer.waremd5,
                            }
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
