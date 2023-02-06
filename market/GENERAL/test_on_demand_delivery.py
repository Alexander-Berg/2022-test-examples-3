#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from datetime import datetime, time, timedelta

from core.combinator import DeliveryStats, make_offer_id
from core.logs import ErrorCodes
from core.report import REQUEST_TIMESTAMP
from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
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
    Payment,
    PreorderDates,
    Region,
    RegionalDelivery,
    RtyOffer,
    Shop,
    Tax,
    TimeInfo,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.types.combinator import (
    CombinatorOffer,
    create_delivery_interval_cgi,
    create_delivery_option,
    create_user_info,
    create_virtual_box,
    DeliveryItem,
    DeliverySubtype,
    DeliveryType,
    Destination,
    RoutePoint,
    RoutePath,
    PickupPointGrouped,
)
from core.types.offer import OfferDimensions

from core.types.delivery import (
    OutletType,
)

from market.combinator.proto.grpc.combinator_pb2 import DeliveryService, Time
from market.pylibrary.const.payment_methods import PaymentMethod


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
    courier_bucket_dc_id = 1000
    courier_bucket_id = 2000

    on_demand_delivery_service_id = 101
    on_demand_bucket_dc_id = 1001
    on_demand_bucket_id = 2001

    mvp_lavka_delivery_service_id = 1006419
    mvp_lavka_bucket_dc_id = 1002
    mvp_lavka_bucket_id = 2002

    dc_day_from = 1
    dc_day_to = 1
    dc_delivery_cost = 50

    combinator_delivery_cost = 50
    combinator_day_from = 0
    combinator_day_to = 0
    combinator_date_from = DATETIME_NOW + timedelta(days=combinator_day_from)
    combinator_date_to = DATETIME_NOW + timedelta(days=combinator_day_to)
    combinator_courier_time_from = time(18, 0)
    combinator_courier_time_to = time(22, 0)
    combinator_on_demand_time_from = time(10, 0)
    combinator_on_demand_time_to = time(22, 0)

    preorder_shipment_day = 20
    preorder_day_from = preorder_shipment_day + combinator_day_from
    preorder_day_to = preorder_shipment_day + combinator_day_to

    preorder_shipment_date = DATETIME_NOW + timedelta(days=preorder_shipment_day)
    preorder_date_from = DATETIME_NOW + timedelta(days=preorder_day_from)
    preorder_date_to = DATETIME_NOW + timedelta(days=preorder_day_to)

    user_gps = GpsCoord(lon=41.920925, lat=54.343961)

    virtual_shopping_cart_price = 100001


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
        delivery_buckets=[_Constants.courier_bucket_id, _Constants.mvp_lavka_bucket_id],
    )

    third_party_msku = MarketSku(
        title="Обычный 3P оффер", hyperid=_Constants.category_id, sku=1, blue_offers=[third_party_offer]
    )


class _CombinatorOffers:
    third_party_offer = CombinatorOffer(
        shop_sku=_BlueOffers.third_party_offer.offerid,
        shop_id=_Constants.third_party_fesh,
        partner_id=_Constants.ff_warehouse_id,
        available_count=1,
    )


class _LogisticGraph:
    def _service_time(day, hour, minute):
        return datetime(
            year=DATETIME_NOW.year, month=DATETIME_NOW.month, day=DATETIME_NOW.day + day, hour=hour, minute=minute
        )

    Warehouse = RoutePoint(
        segment_id=512001,
        segment_type='warehouse',
        partner_type='FULFILLMENT',
        point_ids=Destination(partner_id=_Constants.ff_warehouse_id),
        services=[
            (DeliveryService.INTERNAL, 'PROCESSING', _service_time(day=0, hour=10, minute=0), timedelta(hours=2)),
            (DeliveryService.OUTBOUND, 'SHIPMENT', _service_time(day=0, hour=12, minute=0), timedelta(minutes=30)),
        ],
    )

    Movement = RoutePoint(
        segment_id=512002,
        segment_type='movement',
        partner_type='DELIVERY',
        point_ids=Destination(partner_id=_Constants.courier_delivery_service_id),
        services=[
            (DeliveryService.INBOUND, 'INBOUND', _service_time(day=0, hour=12, minute=30), timedelta(minutes=5)),
            (DeliveryService.INTERNAL, 'MOVEMENT', _service_time(day=0, hour=12, minute=5), timedelta(minutes=20)),
            (DeliveryService.OUTBOUND, 'SHIPMENT', _service_time(day=0, hour=12, minute=30), timedelta()),
        ],
    )

    OnDemandWarehouse = RoutePoint(
        segment_id=512006,
        segment_type='warehouse',
        partner_type='DELIVERY',
        point_ids=Destination(partner_id=_Constants.on_demand_delivery_service_id),
        services=[
            (DeliveryService.INBOUND, 'INBOUND', _service_time(day=0, hour=12, minute=30), timedelta(minutes=5)),
            (
                DeliveryService.INTERNAL,
                'SORT',
                _service_time(day=0, hour=12, minute=35),
                timedelta(hours=1, minutes=25),
            ),
            (DeliveryService.INBOUND, 'ON_DEMAND_YANDEX_GO', None, timedelta()),
            (DeliveryService.OUTBOUND, 'SHIPMENT', _service_time(day=0, hour=14, minute=0), timedelta(minutes=30)),
        ],
    )

    OnDemandMovement = RoutePoint(
        segment_id=512007,
        segment_type='movement',
        partner_type='DELIVERY',
        point_ids=Destination(partner_id=_Constants.on_demand_delivery_service_id),
        services=[
            (DeliveryService.INBOUND, 'INBOUND', _service_time(day=0, hour=14, minute=30), timedelta(minutes=5)),
            (DeliveryService.INTERNAL, 'MOVEMENT', _service_time(day=0, hour=14, minute=35), timedelta(minutes=5)),
            (DeliveryService.OUTBOUND, 'SHIPMENT', _service_time(day=0, hour=14, minute=40), timedelta()),
        ],
    )

    OnDemandLinehaul = RoutePoint(
        segment_id=512008,
        segment_type='linehaul',
        partner_type='DELIVERY',
        point_ids=Destination(partner_id=_Constants.on_demand_delivery_service_id),
        services=(
            (
                DeliveryService.INTERNAL,
                'DELIVERY',
                _service_time(day=0, hour=14, minute=40),
                timedelta(hours=1, minutes=20),
            ),
            (DeliveryService.INTERNAL, 'LAST_MILE', _service_time(day=0, hour=16, minute=0), timedelta(minutes=30)),
        ),
    )

    OnDemandEndPoint = RoutePoint(
        segment_id=512009,
        segment_type='handing',
        partner_type='DELIVERY',
        point_ids=Destination(region_id=_Constants.moscow_rids, gps_coords=_Constants.user_gps),
        services=[
            (
                DeliveryService.OUTBOUND,
                'HANDING',
                _service_time(day=0, hour=16, minute=30),
                timedelta(minutes=15),
                (
                    Time(
                        hour=_Constants.combinator_on_demand_time_from.hour,
                        minute=_Constants.combinator_on_demand_time_from.minute,
                    ),
                    Time(
                        hour=_Constants.combinator_on_demand_time_to.hour,
                        minute=_Constants.combinator_on_demand_time_to.minute,
                    ),
                ),
            )
        ],
    )


class _Requests:
    gps_cgi = '&gps=lat:{lat};lon:{lon}'
    mvp_lakva_rear = '&rearr-factors=market_blue_add_delivery_service_options={mvp_lavka}'

    actual_delivery_request = (
        'place=actual_delivery'
        '&pp=18'
        '&rgb=blue'
        '&pickup-options=grouped'
        '&pickup-options-extended-grouping=1'
        '&rids={rids}'
        '&offers-list={offers}'
        '&logged-in={logged_in}'
        '&combinator=1'
    )

    delivery_route_request = (
        'place=delivery_route'
        '&pp=18'
        '&rids={rids}'
        '&rgb=blue'
        '&offers-list={offers}'
        '&delivery-type={type}'
        '&delivery-subtype={subtype}'
        '&delivery-interval={interval}'
        '&logged-in={logged_in}'
    )

    sku_offers_request = (
        'place=sku_offers'
        '&pp=18'
        '&rgb=blue'
        '&allow-collapsing=0'
        '&rids={rids}'
        '&hid={hid}'
        '&market-sku={msku}'
        '&logged-in={logged_in}'
    )

    combine_request = (
        'place=combine'
        '&pp=18'
        '&rgb=blue'
        '&rids={rids}'
        '&offers-list={offers}'
        '&logged-in={logged_in}'
        '&combinator=1'
    )


class T(TestCase):
    @staticmethod
    def _create_courier_delivery_bucket(bucket_id, bucket_dc_id, fesh, delivery_service_id):
        return DeliveryBucket(
            bucket_id=bucket_id,
            dc_bucket_id=bucket_dc_id,
            fesh=fesh,
            carriers=[delivery_service_id],
            regional_options=[
                RegionalDelivery(
                    rid=_Constants.moscow_rids,
                    options=[
                        DeliveryOption(
                            price=_Constants.dc_delivery_cost,
                            day_from=_Constants.dc_day_from,
                            day_to=_Constants.dc_day_to,
                        )
                    ],
                    payment_methods=[Payment.PT_YANDEX, Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
                )
            ],
            delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
        )

    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [Region(rid=_Constants.moscow_rids, name="Москва")]

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [_Shops.virtual_blue_shop, _Shops.third_party_shop]

    @classmethod
    def prepare_delivery_buckets(cls):
        cls.index.delivery_buckets += [
            T._create_courier_delivery_bucket(
                bucket_id=_Constants.courier_bucket_id,
                bucket_dc_id=_Constants.courier_bucket_dc_id,
                fesh=_Constants.third_party_fesh,
                delivery_service_id=_Constants.courier_delivery_service_id,
            ),
            T._create_courier_delivery_bucket(
                bucket_id=_Constants.mvp_lavka_bucket_id,
                bucket_dc_id=_Constants.mvp_lavka_bucket_dc_id,
                fesh=_Constants.third_party_fesh,
                delivery_service_id=_Constants.mvp_lavka_delivery_service_id,
            ),
        ]

    @classmethod
    def prepare_warehouses(cls):
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[_Constants.russia_rids, _Constants.moscow_rids],
                warehouse_with_priority=[WarehouseWithPriority(warehouse_id=_Constants.ff_warehouse_id, priority=1)],
            )
        ]

    @classmethod
    def prepare_lms(cls):
        cls.settings.lms_autogenerate = False

        cls.dynamic.lms += [DynamicDaysSet(key=1, days=[])]

        for wh_id, ds_id, ds_name in [
            (_Constants.ff_warehouse_id, _Constants.courier_delivery_service_id, 'courier_delivery_service'),
            (_Constants.ff_warehouse_id, _Constants.on_demand_delivery_service_id, 'on_demand_delivery_service'),
            (_Constants.ff_warehouse_id, _Constants.mvp_lavka_delivery_service_id, 'mvp_lavka_delivery_service'),
        ]:
            cls.dynamic.lms += [
                DynamicDeliveryServiceInfo(
                    id=ds_id,
                    name=ds_name,
                    region_to_region_info=[
                        DeliveryServiceRegionToRegionInfo(
                            region_from=_Constants.moscow_rids, region_to=_Constants.russia_rids, days_key=1
                        )
                    ],
                ),
                DynamicWarehouseInfo(id=wh_id, home_region=_Constants.moscow_rids),
                DynamicWarehouseAndDeliveryServiceInfo(
                    warehouse_id=wh_id,
                    delivery_service_id=ds_id,
                    operation_time=0,
                    date_switch_time_infos=[
                        DateSwitchTimeAndRegionInfo(
                            date_switch_hour=2,
                            region_to=_Constants.russia_rids,
                            date_switch_time=TimeInfo(19, 0),
                            packaging_time=TimeInfo(3, 30),
                        )
                    ],
                ),
            ]

    @classmethod
    def prepare_combinator(cls):
        report_start_time_in_ms = REQUEST_TIMESTAMP * 10**6
        cls.combinator.set_start_date(microseconds_for_disabled_random=report_start_time_in_ms)
        cls.settings.default_search_experiment_flags += ['enable_dsbs_combinator_request_in_actual_delivery=0']

        # Опции доставки курьеской службой в фиксированный интервал времени (обычная курьерка)
        courier_delivery_option = create_delivery_option(
            cost=_Constants.combinator_delivery_cost,
            date_from=_Constants.combinator_date_from,
            date_to=_Constants.combinator_date_to,
            time_from=_Constants.combinator_courier_time_from,
            time_to=_Constants.combinator_courier_time_to,
            delivery_service_id=_Constants.courier_delivery_service_id,
            delivery_subtype=DeliverySubtype.ORDINARY,
            leave_at_the_door=True,
            do_not_call=True,
            customizers=[
                {
                    "key": "leave_at_the_door",
                    "name": "Оставить у двери",
                    "type": "boolean",
                },
                {
                    "key": "not_call",
                    "name": "Не звонить",
                    "type": "boolean",
                },
            ],
        )
        for gps, logged_in in [(_Constants.user_gps, False), (None, True), (None, False)]:
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
                destination=Destination(region_id=_Constants.moscow_rids, gps_coords=gps),
                payment_methods=[],
                user_info=create_user_info(logged_in=logged_in),
                total_price=_BlueOffers.third_party_offer.price,
            ).respond_with_courier_options(options=[courier_delivery_option])

        # Опции доставки курьеской службой по требованию (on-demand доставка)
        on_demand_delivery_option = create_delivery_option(
            cost=_Constants.combinator_delivery_cost,
            date_from=_Constants.combinator_date_from,
            date_to=_Constants.combinator_date_to,
            time_from=_Constants.combinator_on_demand_time_from,
            time_to=_Constants.combinator_on_demand_time_to,
            delivery_service_id=_Constants.on_demand_delivery_service_id,
            delivery_subtype=DeliverySubtype.ON_DEMAND,
            leave_at_the_door=True,
            do_not_call=True,
            customizers=[
                {
                    "key": "leave_at_the_door",
                    "name": "Оставить у двери",
                    "type": "boolean",
                },
                {
                    "key": "not_call",
                    "name": "Не звонить",
                    "type": "boolean",
                },
            ],
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
            destination=Destination(region_id=_Constants.moscow_rids, gps_coords=_Constants.user_gps),
            payment_methods=[],
            user_info=create_user_info(logged_in=True),
            total_price=_BlueOffers.third_party_offer.price,
        ).respond_with_courier_options(options=[courier_delivery_option, on_demand_delivery_option])

        # Статистика доставляемости на КМ
        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_BlueOffers.third_party_offer, _Shops.third_party_shop),
            courier_stats=DeliveryStats(
                cost=_Constants.combinator_delivery_cost,
                day_from=_Constants.combinator_day_from,
                day_to=_Constants.combinator_day_to,
            ),
            on_demand_stats=DeliveryStats(
                cost=_Constants.combinator_delivery_cost,
                day_from=_Constants.combinator_day_from,
                day_to=_Constants.combinator_day_to,
            ),
        )

        # Маршрут доставки по требованию через Яндекс.Go (сервис 'ON_DEMAND_YANDEX_GO')
        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.COURIER,
            destination=_LogisticGraph.OnDemandEndPoint,
            delivery_option=create_delivery_option(
                date_from=_Constants.combinator_date_from,
                date_to=_Constants.combinator_date_to,
                time_from=_Constants.combinator_on_demand_time_from,
                time_to=_Constants.combinator_on_demand_time_to,
                delivery_subtype=DeliverySubtype.ON_DEMAND,
            ),
            total_price=_BlueOffers.third_party_offer.price,
        ).respond_with_delivery_route(
            offers=[_CombinatorOffers.third_party_offer],
            points=[
                _LogisticGraph.Warehouse,
                _LogisticGraph.Movement,
                _LogisticGraph.OnDemandWarehouse,
                _LogisticGraph.OnDemandMovement,
                _LogisticGraph.OnDemandLinehaul,
                _LogisticGraph.OnDemandEndPoint,
            ],
            paths=[RoutePath(point_from=i, point_to=i + 1) for i in range(0, 5)],
            date_from=_Constants.combinator_date_from,
            date_to=_Constants.combinator_date_to,
        )

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.mskus += [_BlueOffers.third_party_msku]

    def test_actual_delivery(self):
        """
        Проверяем, что в 'place=actual_delivery' Репорта запрос и обработка опций доставки по требованию,
        вычисленных через Комбинатор по ручке 'GetCourierOptions', происходят только если в запросе:
            - пользователь залогинен - CGI-параметр 'logged-in' равен 1
            - присутствуют GPS-координаты пользователя - CGI-параметр 'gps'
        """

        # Проверяем наличие
        request = _Requests.actual_delivery_request + _Requests.gps_cgi
        response = self.report.request_json(
            request.format(
                rids=_Constants.moscow_rids,
                offers='{}:1'.format(_BlueOffers.third_party_offer.waremd5),
                lat=_Constants.user_gps.latitude,
                lon=_Constants.user_gps.longitude,
                logged_in=1,
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
                                    'dayFrom': _Constants.combinator_day_from,
                                    'dayTo': _Constants.combinator_day_to,
                                    'timeIntervals': [{'from': '18:00', 'to': '22:00'}],
                                    'isDefault': True,
                                },
                                {
                                    'dayFrom': _Constants.combinator_day_from,
                                    'dayTo': _Constants.combinator_day_to,
                                    'timeIntervals': [{'from': '10:00', 'to': '22:00'}],
                                    'isDefault': False,
                                    'isOnDemand': True,
                                },
                            ]
                        },
                        'offers': [{'entity': 'offer', 'wareId': _BlueOffers.third_party_offer.waremd5}],
                    }
                ]
            },
        )

        # Проверяем отсутствие
        for gps, logged_in in [
            (True, False),
            (False, True),
            (False, False),
        ]:
            request = _Requests.actual_delivery_request + _Requests.mvp_lakva_rear
            if gps:
                request += _Requests.gps_cgi.format(lat=_Constants.user_gps.latitude, lon=_Constants.user_gps.longitude)

            response = self.report.request_json(
                request.format(
                    rids=_Constants.moscow_rids,
                    offers='{}:1'.format(_BlueOffers.third_party_offer.waremd5),
                    logged_in=int(logged_in),
                    mvp_lavka=0,
                )
            )
            self.assertFragmentNotIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'deliveryGroup',
                            'delivery': {
                                'options': [
                                    {
                                        'dayFrom': _Constants.combinator_day_from,
                                        'dayTo': _Constants.combinator_day_to,
                                        'timeIntervals': [{'from': '10:00', 'to': '22:00'}],
                                        'isDefault': False,
                                        'isOnDemand': True,
                                    }
                                ]
                            },
                            'offers': [{'entity': 'offer', 'wareId': _BlueOffers.third_party_offer.waremd5}],
                        }
                    ]
                },
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
                                        'dayFrom': _Constants.combinator_day_from,
                                        'dayTo': _Constants.combinator_day_to,
                                        'timeIntervals': [{'from': '18:00', 'to': '22:00'}],
                                        'isDefault': True,
                                    }
                                ]
                            },
                            'offers': [{'entity': 'offer', 'wareId': _BlueOffers.third_party_offer.waremd5}],
                        }
                    ]
                },
                allow_different_len=False,
            )

    def test_delivery_route(self):
        """
        Проверяем, что в 'place=delivery_route' Репорта запрос и обработка маршрута доставки по требованию
        происходят только если в запросе:
            - пользователь залогинен - CGI-параметр 'logged-in' равен 1
            - присутствуют GPS-координаты пользователя - CGI-параметр 'gps'
            - указан подтип доставки 'ON_DEMAND'
        """

        # Проверяем наличие
        request = _Requests.delivery_route_request + _Requests.gps_cgi
        response = self.report.request_json(
            request.format(
                rids=_Constants.moscow_rids,
                offers='{}:1'.format(_BlueOffers.third_party_offer.waremd5),
                type='courier',
                subtype='on-demand',
                interval=create_delivery_interval_cgi(
                    date_from=_Constants.combinator_date_from,
                    time_from=_Constants.combinator_on_demand_time_from,
                    date_to=_Constants.combinator_date_to,
                    time_to=_Constants.combinator_on_demand_time_to,
                ),
                logged_in=1,
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
                            'route': {
                                'route': {
                                    'paths': [{'point_from': i, 'point_to': i + 1} for i in range(0, 5)],
                                    'points': [
                                        {'segment_id': s.segment_id}
                                        for s in [
                                            _LogisticGraph.Warehouse,
                                            _LogisticGraph.Movement,
                                            _LogisticGraph.OnDemandWarehouse,
                                            _LogisticGraph.OnDemandMovement,
                                            _LogisticGraph.OnDemandLinehaul,
                                            _LogisticGraph.OnDemandEndPoint,
                                        ]
                                    ],
                                    'delivery_type': DeliveryType.to_string(DeliveryType.COURIER),
                                }
                            },
                            'option': {
                                'dayFrom': _Constants.combinator_day_from,
                                'dayTo': _Constants.combinator_day_to,
                                'timeIntervals': [{'from': '10:00', 'to': '22:00'}],
                                'isDefault': True,
                                'isOnDemand': True,
                            },
                        },
                        'offers': [{'entity': 'offer', 'wareId': _BlueOffers.third_party_offer.waremd5}],
                    }
                ]
            },
        )

        # Проверяем отсутствие
        for gps, logged_in in [
            (True, False),
            (False, True),
            (False, False),
        ]:
            request = _Requests.delivery_route_request
            if gps:
                request += _Requests.gps_cgi.format(lat=_Constants.user_gps.latitude, lon=_Constants.user_gps.longitude)

            response = self.report.request_json(
                request.format(
                    rids=_Constants.moscow_rids,
                    offers='{}:1'.format(_BlueOffers.third_party_offer.waremd5),
                    type='courier',
                    subtype='on-demand',
                    interval=create_delivery_interval_cgi(
                        date_from=_Constants.combinator_date_from,
                        time_from=_Constants.combinator_on_demand_time_from,
                        date_to=_Constants.combinator_date_to,
                        time_to=_Constants.combinator_on_demand_time_to,
                    ),
                    logged_in=int(logged_in),
                )
            )
            self.assertFragmentIn(response, {"error": {"code": "INVALID_USER_CGI"}})
            self.error_log.expect(code=ErrorCodes.RESPONSE_ERROR).times(3)

    def test_mvp_lavka_overriden(self):
        """
        Проверяем, что в 'place=actual_delivery' опции доставки по требованию, не вычисляются репортом
        а отдаются только опции, вычисленные через Комбинатор
        """
        request = (
            _Requests.actual_delivery_request
            + _Requests.gps_cgi
            + _Requests.mvp_lakva_rear
            + '&rearr-factors=market_nordstream_relevance=0'
        )
        # Проверяем отсутствие опций от Репорта при наличии опций от Комбинатора (новая схема)
        response = self.report.request_json(
            request.format(
                rids=_Constants.moscow_rids,
                offers='{}:1'.format(_BlueOffers.third_party_offer.waremd5),
                lat=_Constants.user_gps.latitude,
                lon=_Constants.user_gps.longitude,
                logged_in=1,
                mvp_lavka=1,
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
                                    'dayFrom': _Constants.combinator_day_from,
                                    'dayTo': _Constants.combinator_day_to,
                                    'serviceId': str(_Constants.courier_delivery_service_id),
                                    'isDefault': True,
                                    'timeIntervals': [{'from': '18:00', 'to': '22:00'}],
                                    'leaveAtTheDoor': True,
                                    'notCall': True,
                                    'customizers': [
                                        {
                                            'key': 'leave_at_the_door',
                                            'name': 'Оставить у двери',
                                            'type': 'boolean',
                                        },
                                        {
                                            'key': 'not_call',
                                            'name': 'Не звонить',
                                            'type': 'boolean',
                                        },
                                    ],
                                },
                                {
                                    'dayFrom': _Constants.combinator_day_from,
                                    'dayTo': _Constants.combinator_day_to,
                                    'serviceId': str(_Constants.on_demand_delivery_service_id),
                                    'isDefault': False,
                                    'isOnDemand': True,
                                    'timeIntervals': [{'from': '10:00', 'to': '22:00'}],
                                    'leaveAtTheDoor': True,
                                    'notCall': True,
                                    'customizers': [
                                        {
                                            'key': 'leave_at_the_door',
                                            'name': 'Оставить у двери',
                                            'type': 'boolean',
                                        },
                                        {
                                            'key': 'not_call',
                                            'name': 'Не звонить',
                                            'type': 'boolean',
                                        },
                                    ],
                                },
                            ]
                        },
                        'offers': [{'entity': 'offer', 'wareId': _BlueOffers.third_party_offer.waremd5}],
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_sku_offers(self):
        """
        Проверяем, что в 'place=sku_offers' опции доставки по требованию, вычисленные
        через Комбинатор, присутствуют на выдаче
        """
        for logged_in in [0, 1]:
            response = self.report.request_json(
                _Requests.sku_offers_request.format(
                    rids=_Constants.moscow_rids,
                    hid=_Constants.category_id,
                    msku=_BlueOffers.third_party_msku.sku,
                    logged_in=logged_in,
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'sku',
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'wareId': _BlueOffers.third_party_offer.waremd5,
                                        'delivery': {
                                            'onDemandStats': {
                                                'dayFrom': _Constants.combinator_day_from,
                                                'dayTo': _Constants.combinator_day_to,
                                                'price': {
                                                    'value': '99',
                                                    'currency': 'RUR',
                                                },
                                            }
                                        },
                                    }
                                ]
                            },
                        }
                    ]
                },
                allow_different_len=False,
            )

    def test_combine(self):
        """
        Проверяем, что в 'place=combine' Репорта запрос и обработка опций доставки по требованию,
        вычисленных через Комбинатор по ручке 'GetCourierOptions', происходят только если в запросе:
            - пользователь залогинен - CGI-параметр 'logged-in' равен 1
            - присутствуют GPS-координаты пользователя - CGI-параметр 'gps'
        """

        # Проверяем наличие
        request = _Requests.combine_request + _Requests.gps_cgi
        response = self.report.request_json(
            request.format(
                rids=_Constants.moscow_rids,
                offers='{}:1;msku:{}'.format(_BlueOffers.third_party_offer.waremd5, _BlueOffers.third_party_msku.sku),
                lat=_Constants.user_gps.latitude,
                lon=_Constants.user_gps.longitude,
                logged_in=1,
            )
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'split-strategy',
                        'buckets': [
                            {
                                "warehouseId": 10,
                                "shopId": 1,
                                "isFulfillment": True,
                                "isDigital": False,
                                "hasCourier": True,
                                "hasPickup": True,
                                "hasOnDemand": True,
                                "deliveryDayFrom": 0,
                            }
                        ],
                    }
                ],
                'offers': {'items': [{'entity': 'offer', 'wareId': _BlueOffers.third_party_offer.waremd5}]},
            },
            allow_different_len=False,
        )

        # Проверяем отсутствие
        for gps, logged_in in [
            (True, False),
            (False, True),
            (False, False),
        ]:
            request = _Requests.combine_request
            if gps:
                request += _Requests.gps_cgi.format(lat=_Constants.user_gps.latitude, lon=_Constants.user_gps.longitude)

            response = self.report.request_json(
                request.format(
                    rids=_Constants.moscow_rids,
                    offers='{}:1;msku:{}'.format(
                        _BlueOffers.third_party_offer.waremd5, _BlueOffers.third_party_msku.sku
                    ),
                    logged_in=int(logged_in),
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'split-strategy',
                            'buckets': [
                                {
                                    "warehouseId": 10,
                                    "shopId": 1,
                                    "isFulfillment": True,
                                    "isDigital": False,
                                    "hasCourier": True,
                                    "hasPickup": True,
                                    "hasOnDemand": False,
                                    "deliveryDayFrom": 0,
                                }
                            ],
                        }
                    ],
                    'offers': {'items': [{'entity': 'offer', 'wareId': _BlueOffers.third_party_offer.waremd5}]},
                },
                allow_different_len=False,
            )

    @classmethod
    def prepare_for_test_total_price(cls):
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
            total_price=_Constants.virtual_shopping_cart_price,
        ).respond_with_courier_options(
            options=[
                create_delivery_option(
                    cost=_Constants.combinator_delivery_cost,
                    date_from=_Constants.combinator_date_from,
                    date_to=_Constants.combinator_date_to,
                    time_from=_Constants.combinator_courier_time_from,
                    time_to=_Constants.combinator_courier_time_to,
                    delivery_service_id=_Constants.courier_delivery_service_id,
                    delivery_subtype=DeliverySubtype.ORDINARY,
                )
            ]
        )

        cls.combinator.on_pickup_points_grouped_request(
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
            destination_regions=[_Constants.moscow_rids],
            point_types=[],
            total_price=_BlueOffers.third_party_offer.price,
        ).respond_with_grouped_pickup_points(
            groups=[
                PickupPointGrouped(
                    ids_list=[1],
                    outlet_type=OutletType.FOR_PICKUP,
                    service_id=322,
                    cost=100,
                    date_from=_Constants.combinator_date_from,
                    date_to=_Constants.combinator_date_to,
                    payment_methods=[PaymentMethod.PT_CASH_ON_DELIVERY],
                ),
            ],
            virtual_box=create_virtual_box(5500, 50, 50, 50),
        )

        cls.combinator.on_pickup_points_grouped_request(
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
            destination_regions=[_Constants.moscow_rids],
            point_types=[],
            total_price=_Constants.virtual_shopping_cart_price,
        ).respond_with_grouped_pickup_points(
            groups=[
                PickupPointGrouped(
                    ids_list=[2],
                    outlet_type=OutletType.FOR_PICKUP,
                    service_id=323,
                    cost=100,
                    date_from=_Constants.combinator_date_from,
                    date_to=_Constants.combinator_date_to,
                    payment_methods=[PaymentMethod.PT_CASH_ON_DELIVERY],
                ),
            ],
            virtual_box=create_virtual_box(5500, 50, 50, 50),
        )

    def test_total_price(self):
        """
        Проверяем, что в 'place=actual_delivery' Репорта в комбинатор передается цена как общая стоимость
        товаров в посылке, а не цена, которая приходит в cgi параметре total-price
        """
        request = _Requests.actual_delivery_request + _Requests.gps_cgi + '&total-price={total_price}'
        response = self.report.request_json(
            request.format(
                rids=_Constants.moscow_rids,
                offers='{}:1'.format(_BlueOffers.third_party_offer.waremd5),
                lat=_Constants.user_gps.latitude,
                lon=_Constants.user_gps.longitude,
                logged_in=1,
                total_price=_Constants.virtual_shopping_cart_price,  # передаем цену корзины отличную от цены оффера
            )
        )

        """
        В моке курьерки комбинатора, для цены, переданной в запросе, нет опций лавки. Если в ответе
        пришли опции лавки, значит в комбинатор передалась цена оффера, а не цена из параметра в запросе.

        В случае самовывоза моки комбинатора с разными ценами имеют разные service_id и ids_list.
        """
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'deliveryGroup',
                        'delivery': {
                            'options': [
                                {
                                    'dayFrom': _Constants.combinator_day_from,
                                    'dayTo': _Constants.combinator_day_to,
                                    'timeIntervals': [{'from': '18:00', 'to': '22:00'}],
                                    'isDefault': True,
                                },
                                {
                                    'dayFrom': _Constants.combinator_day_from,
                                    'dayTo': _Constants.combinator_day_to,
                                    'timeIntervals': [{'from': '10:00', 'to': '22:00'}],
                                    'isDefault': False,
                                    'isOnDemand': True,
                                },
                            ],
                            'pickupOptions': [
                                {
                                    "dayFrom": 0,
                                    "dayTo": 0,
                                    "outletIds": [1],
                                    'serviceId': 322,
                                }
                            ],
                        },
                        'offers': [{'entity': 'offer', 'wareId': _BlueOffers.third_party_offer.waremd5}],
                    }
                ]
            },
        )

        """
        Проверяем, что в 'place=delivery_route' Репорта в комбинатор передается цена как общая стоимость
        товаров в посылке, а не цена, которая приходит в cgi параметре total-price
        """
        request = _Requests.delivery_route_request + _Requests.gps_cgi + '&total-price={total_price}'
        response = self.report.request_json(
            request.format(
                rids=_Constants.moscow_rids,
                offers='{}:1'.format(_BlueOffers.third_party_offer.waremd5),
                type='courier',
                subtype='on-demand',
                interval=create_delivery_interval_cgi(
                    date_from=_Constants.combinator_date_from,
                    time_from=_Constants.combinator_on_demand_time_from,
                    date_to=_Constants.combinator_date_to,
                    time_to=_Constants.combinator_on_demand_time_to,
                ),
                logged_in=1,
                lat=_Constants.user_gps.latitude,
                lon=_Constants.user_gps.longitude,
                total_price=_Constants.virtual_shopping_cart_price,  # передаем цену корзины отличную от цены оффера
            )
        )

        """
        Для цены, переданной в запросе, нет мока комбинатора. Если в ответе
        пришли опции лавки, значит в комбинатор передалась цена оффера, а не цена из параметра в запросе.
        """
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'deliveryGroup',
                        'delivery': {
                            'route': {
                                'route': {
                                    'paths': [{'point_from': i, 'point_to': i + 1} for i in range(0, 5)],
                                    'points': [
                                        {'segment_id': s.segment_id}
                                        for s in [
                                            _LogisticGraph.Warehouse,
                                            _LogisticGraph.Movement,
                                            _LogisticGraph.OnDemandWarehouse,
                                            _LogisticGraph.OnDemandMovement,
                                            _LogisticGraph.OnDemandLinehaul,
                                            _LogisticGraph.OnDemandEndPoint,
                                        ]
                                    ],
                                    'delivery_type': DeliveryType.to_string(DeliveryType.COURIER),
                                }
                            },
                            'option': {
                                'dayFrom': _Constants.combinator_day_from,
                                'dayTo': _Constants.combinator_day_to,
                                'timeIntervals': [{'from': '10:00', 'to': '22:00'}],
                                'isDefault': True,
                                'isOnDemand': True,
                            },
                        },
                        'offers': [{'entity': 'offer', 'wareId': _BlueOffers.third_party_offer.waremd5}],
                    }
                ]
            },
        )

    @classmethod
    def prepare_preorder(cls):
        cls.settings.rty_qpipe = True
        cls.settings.report_subrole = 'main'
        cls.index.preorder_dates += [
            PreorderDates(
                feedId=_BlueOffers.third_party_offer.feedid,
                ssku=_BlueOffers.third_party_offer.offerid,
                date_from="2020-12-17",
                date_to="2020-12-18",
                shipment_start=_Constants.preorder_shipment_date.strftime("%Y-%m-%d"),
            ),
        ]

    def test_preorder(self):
        """
        Проверяем, что для ondemand опций доставки, приходящих от комбинатора
        работает смещение из предзаказов
        """
        RSD_ORDER_METHOD = 1 << 0

        PRE_ORDER = 3

        # Добавим возможность предзаказа в rty
        self.rty.offers += [
            RtyOffer(
                feedid=_BlueOffers.third_party_offer.feedid,
                offerid=_BlueOffers.third_party_offer.offerid,
                order_method=PRE_ORDER,
                order_method_ts=1,
            )
        ]
        self.rty.flush()

        # Проверяем наличие
        request = _Requests.actual_delivery_request + _Requests.gps_cgi
        request_suffix = (
            "&show-preorder=1"
            "&rearr-factors=market_enable_preorder_dates_shift=1;"
            "rty_dynamics=1;rty_stock_dynamics={}".format(RSD_ORDER_METHOD)
        )
        request += request_suffix

        response = self.report.request_json(
            request.format(
                rids=_Constants.moscow_rids,
                offers='{}:1'.format(_BlueOffers.third_party_offer.waremd5),
                lat=_Constants.user_gps.latitude,
                lon=_Constants.user_gps.longitude,
                logged_in=1,
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
                                    'dayFrom': _Constants.preorder_day_from,
                                    'dayTo': _Constants.preorder_day_to,
                                    'timeIntervals': [{'from': '18:00', 'to': '22:00'}],
                                    'isDefault': True,
                                },
                                {
                                    'dayFrom': _Constants.preorder_day_from,
                                    'dayTo': _Constants.preorder_day_to,
                                    'timeIntervals': [{'from': '10:00', 'to': '22:00'}],
                                    'isDefault': False,
                                    'isOnDemand': True,
                                },
                            ]
                        },
                        'offers': [
                            {'entity': 'offer', 'wareId': _BlueOffers.third_party_offer.waremd5, 'isPreorder': True}
                        ],
                    }
                ]
            },
        )

        # проверяем не только опции но и onDemand статистики

        response = self.report.request_json(
            _Requests.sku_offers_request.format(
                rids=_Constants.moscow_rids,
                hid=_Constants.category_id,
                msku=_BlueOffers.third_party_msku.sku,
                logged_in=1,
            )
            + request_suffix
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'sku',
                        'offers': {
                            'items': [
                                {
                                    'entity': 'offer',
                                    'wareId': _BlueOffers.third_party_offer.waremd5,
                                    'isPreorder': True,
                                    'delivery': {
                                        'onDemandStats': {
                                            'dayFrom': _Constants.preorder_day_from,
                                            'dayTo': _Constants.preorder_day_to,
                                            'price': {
                                                'value': '99',
                                                'currency': 'RUR',
                                            },
                                        }
                                    },
                                }
                            ]
                        },
                    }
                ]
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
