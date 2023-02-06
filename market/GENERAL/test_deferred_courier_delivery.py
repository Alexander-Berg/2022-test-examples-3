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
    create_delivery_interval_cgi,
    create_delivery_option,
    create_user_info,
    DeliveryItem,
    DeliverySubtype,
    DeliveryType,
    Destination,
    RoutePoint,
    RoutePath,
)
from core.types.offer import OfferDimensions

from market.combinator.proto.grpc.combinator_pb2 import (
    DeliveryService,
    Time,
)


DATETIME_NOW = datetime.fromtimestamp(REQUEST_TIMESTAMP)


class _Constants:
    russia_rids = 225
    moscow_rids = 213

    model_id = 1
    category_id = 1

    virtual_blue_fesh = 1
    virtual_blue_feed_id = 1

    third_party_fesh = 2
    third_party_feed_id = 2

    ff_warehouse_id = 10

    courier_delivery_service_id = 100
    deferred_courier_delivery_service_id = 101

    delivery_cost = 50
    day_from = 0
    day_to = 0
    date_from = DATETIME_NOW + timedelta(days=day_from)
    date_to = DATETIME_NOW + timedelta(days=day_to)
    courier_time_from = time(18, 0)
    courier_time_to = time(22, 0)
    deferred_courier_time_from = time(13, 0)
    deferred_courier_time_to = time(14, 0)

    user_gps = GpsCoord(lon=41.920925, lat=54.343961)


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

    another_third_party_offer = BlueOffer(
        offerid='third_party_sku2',
        waremd5='ThirdPartyWaremd52___w',
        price=40,
        feedid=_Constants.third_party_feed_id,
        weight=5,
        dimensions=OfferDimensions(length=40, width=40, height=40),
        supplier_id=_Constants.third_party_fesh,
    )

    another_third_party_msku = MarketSku(
        title="Еще один обычный 3P оффер",
        hyperid=_Constants.category_id,
        sku=2,
        blue_offers=[another_third_party_offer],
    )


class _CombinatorOffers:
    third_party_offer = CombinatorOffer(
        shop_sku=_BlueOffers.third_party_offer.offerid,
        shop_id=_Constants.third_party_fesh,
        partner_id=_Constants.ff_warehouse_id,
        available_count=1,
    )

    another_third_party_offer = CombinatorOffer(
        shop_sku=_BlueOffers.another_third_party_offer.offerid,
        shop_id=_Constants.third_party_fesh,
        partner_id=_Constants.ff_warehouse_id,
        available_count=1,
    )


class _Requests:
    actual_delivery_request = (
        'place=actual_delivery'
        '&pp=18'
        '&rgb=blue'
        '&pickup-options=grouped'
        '&pickup-options-extended-grouping=1'
        '&combinator=1'
        '&logged-in=1'
        '&rids={rids}'
        '&offers-list={offers}'
        '&gps=lat:{lat};lon:{lon}'
    )

    delivery_route_request = (
        'place=delivery_route'
        '&pp=18'
        '&rgb=blue'
        '&logged-in=1'
        '&rids={rids}'
        '&offers-list={offers}'
        '&delivery-type={type}'
        '&delivery-subtype={subtype}'
        '&cost={cost}'
        '&delivery-interval={interval}'
    )


class _LogisticGraph:
    def _service_time(hour, minute):
        return datetime(
            year=DATETIME_NOW.year,
            month=DATETIME_NOW.month,
            day=DATETIME_NOW.day - 1,
            hour=hour,
            minute=minute,
        )

    Warehouse = RoutePoint(
        point_ids=Destination(partner_id=_Constants.ff_warehouse_id),
        segment_id=512001,
        segment_type='warehouse',
        services=(
            (DeliveryService.INTERNAL, 'PROCESSING', _service_time(13, 25), timedelta(hours=2)),
            (DeliveryService.OUTBOUND, 'SHIPMENT', _service_time(15, 25), timedelta(minutes=35)),
        ),
        partner_type='FULFILLMENT',
    )

    Movement = RoutePoint(
        point_ids=Destination(partner_id=_Constants.courier_delivery_service_id),
        segment_id=512002,
        segment_type='movement',
        services=[
            (DeliveryService.INBOUND, 'INBOUND', _service_time(17, 45), timedelta(minutes=2)),
            (DeliveryService.INTERNAL, 'MOVEMENT', _service_time(17, 47), timedelta(minutes=3)),
            (DeliveryService.OUTBOUND, 'SHIPMENT', _service_time(17, 50), timedelta()),
        ],
    )

    Linehaul = RoutePoint(
        point_ids=Destination(partner_id=_Constants.courier_delivery_service_id),
        segment_id=512003,
        segment_type='linehaul',
        services=(
            (DeliveryService.INTERNAL, 'DELIVERY', _service_time(17, 50), timedelta(hours=1, minutes=45)),
            (DeliveryService.INTERNAL, 'LAST_MILE', _service_time(19, 35), timedelta(minutes=30)),
        ),
    )

    Pickup = RoutePoint(
        point_ids=Destination(logistic_point_id=10000000000),
        segment_id=513004,
        segment_type='pickup',
        services=[
            (DeliveryService.OUTBOUND, 'HANDING', _service_time(20, 5), timedelta(minutes=15)),
        ],
    )

    GoPlatform = RoutePoint(
        point_ids=Destination(region_id=_Constants.moscow_rids),
        segment_id=512005,
        segment_type='go_platform',
        services=[
            (
                DeliveryService.OUTBOUND,
                'HANDING',
                _service_time(20, 5),
                timedelta(minutes=15),
                (
                    Time(
                        hour=_Constants.deferred_courier_time_from.hour,
                        minute=_Constants.deferred_courier_time_from.minute,
                    ),
                    Time(
                        hour=_Constants.deferred_courier_time_to.hour,
                        minute=_Constants.deferred_courier_time_to.minute,
                    ),
                ),
            )
        ],
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

        # Опции доставки курьеской службой в фиксированный интервал времени (обычная курьерка)
        courier_delivery_option = create_delivery_option(
            cost=_Constants.delivery_cost,
            date_from=_Constants.date_from,
            date_to=_Constants.date_to,
            time_from=_Constants.courier_time_from,
            time_to=_Constants.courier_time_to,
            delivery_service_id=_Constants.courier_delivery_service_id,
            delivery_subtype=DeliverySubtype.ORDINARY,
        )

        # Опции доставки в часовые слоты через Яндекс.Go (deferred_courier)
        deferred_courier_delivery_option = create_delivery_option(
            cost=_Constants.delivery_cost,
            date_from=_Constants.date_from,
            date_to=_Constants.date_to,
            time_from=_Constants.deferred_courier_time_from,
            time_to=_Constants.deferred_courier_time_to,
            delivery_service_id=_Constants.deferred_courier_delivery_service_id,
            delivery_subtype=DeliverySubtype.DEFERRED_COURIER,
        )

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
            destination=Destination(
                region_id=_Constants.moscow_rids,
                gps_coords=_Constants.user_gps,
            ),
            payment_methods=[],
            user_info=create_user_info(logged_in=True),
            total_price=_BlueOffers.third_party_offer.price,
        ).respond_with_courier_options(options=[courier_delivery_option, deferred_courier_delivery_option])

        cls.combinator.on_courier_options_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=5000,
                    dimensions=[40, 40, 40],
                    cargo_types=[],
                    offers=[_CombinatorOffers.another_third_party_offer],
                    price=_BlueOffers.another_third_party_offer.price,
                )
            ],
            destination=Destination(
                region_id=_Constants.moscow_rids,
                gps_coords=_Constants.user_gps,
            ),
            payment_methods=[],
            user_info=create_user_info(logged_in=True),
            total_price=_BlueOffers.another_third_party_offer.price,
        ).respond_with_courier_options(options=[courier_delivery_option])

        # Маршрут доставки службой с поддержкой часовых слотов
        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.COURIER,
            destination=_LogisticGraph.GoPlatform,
            delivery_option=create_delivery_option(
                cost=_Constants.delivery_cost,
                date_from=_Constants.date_from,
                date_to=_Constants.date_to,
                time_from=_Constants.deferred_courier_time_from,
                time_to=_Constants.deferred_courier_time_to,
                delivery_subtype=DeliverySubtype.DEFERRED_COURIER,
            ),
            total_price=_BlueOffers.third_party_offer.price,
        ).respond_with_delivery_route(
            offers=[_CombinatorOffers.third_party_offer],
            points=[
                _LogisticGraph.Warehouse,
                _LogisticGraph.Movement,
                _LogisticGraph.Linehaul,
                _LogisticGraph.Pickup,
                _LogisticGraph.GoPlatform,
            ],
            paths=[RoutePath(point_from=i, point_to=i + 1) for i in range(0, 4)],
            date_from=_Constants.date_from,
            date_to=_Constants.date_to,
            delivery_subtype=DeliverySubtype.DEFERRED_COURIER,
        )

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.mskus += [
            _BlueOffers.third_party_msku,
            _BlueOffers.another_third_party_msku,
        ]

    def test_actual_delivery(self):
        """
        Проверям, что в 'place=actual_delivery' Репорта запрос и обработка опций доставки
        в часовые слоты, вычисленные через Комбинатор по gRPC-методу 'GetCourierOptions',
        происходит корректно
        """

        # Проверяем наличие - оффер с доставкой в часовые слоты
        response = self.report.request_json(
            _Requests.actual_delivery_request.format(
                rids=_Constants.moscow_rids,
                offers='{}:1'.format(_BlueOffers.third_party_offer.waremd5),
                lat=_Constants.user_gps.latitude,
                lon=_Constants.user_gps.longitude,
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
                                    'dayFrom': _Constants.day_from,
                                    'dayTo': _Constants.day_to,
                                    'timeIntervals': [
                                        {
                                            'from': '18:00',
                                            'to': '22:00',
                                        }
                                    ],
                                    'isDefault': True,
                                },
                                {
                                    'dayFrom': _Constants.day_from,
                                    'dayTo': _Constants.day_to,
                                    'timeIntervals': [
                                        {
                                            'from': '13:00',
                                            'to': '14:00',
                                        }
                                    ],
                                    'isDefault': False,
                                    'isDeferredCourier': True,
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

        # Проверяем отсутствие - оффер без доставки в часовые слоты
        response = self.report.request_json(
            _Requests.actual_delivery_request.format(
                rids=_Constants.moscow_rids,
                offers='{}:1'.format(_BlueOffers.another_third_party_offer.waremd5),
                lat=_Constants.user_gps.latitude,
                lon=_Constants.user_gps.longitude,
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
                                    'dayFrom': _Constants.day_from,
                                    'dayTo': _Constants.day_to,
                                    'timeIntervals': [
                                        {
                                            'from': '18:00',
                                            'to': '22:00',
                                        }
                                    ],
                                    'isDefault': True,
                                }
                            ],
                        },
                        'offers': [
                            {
                                'entity': 'offer',
                                'wareId': _BlueOffers.another_third_party_offer.waremd5,
                            }
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_delivery_route(self):
        """
        Проверям, что в 'place=delivery_route' Репорта запрос и обработка маршрута доставки
        в часовые слоты происходит корректно
        """
        response = self.report.request_json(
            _Requests.delivery_route_request.format(
                rids=_Constants.moscow_rids,
                offers='{}:1'.format(_BlueOffers.third_party_offer.waremd5),
                type='courier',
                subtype='deferred-courier',
                cost=_Constants.delivery_cost,
                interval=create_delivery_interval_cgi(
                    date_from=_Constants.date_from,
                    time_from=_Constants.deferred_courier_time_from,
                    date_to=_Constants.date_to,
                    time_to=_Constants.deferred_courier_time_to,
                ),
            )
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'deliveryGroup',
                        'delivery': {
                            'route': {
                                'route': {
                                    'points': [
                                        {'segment_id': s.segment_id}
                                        for s in [
                                            _LogisticGraph.Warehouse,
                                            _LogisticGraph.Movement,
                                            _LogisticGraph.Linehaul,
                                            _LogisticGraph.Pickup,
                                            _LogisticGraph.GoPlatform,
                                        ]
                                    ],
                                    'paths': [{'point_from': i, 'point_to': i + 1} for i in range(0, 4)],
                                    'delivery_type': DeliveryType.to_string(DeliveryType.COURIER),
                                },
                            },
                            'option': {
                                'dayFrom': _Constants.day_from,
                                'dayTo': _Constants.day_to,
                                'timeIntervals': [
                                    {
                                        'from': '13:00',
                                        'to': '14:00',
                                    }
                                ],
                                'isDefault': True,
                                'isDeferredCourier': True,
                            },
                        },
                        'offers': [{'entity': 'offer', 'wareId': _BlueOffers.third_party_offer.waremd5}],
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()
