#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from datetime import datetime, time, timedelta
import calendar
from core.combinator import DeliveryStats, make_offer_id
from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicCalendarMetaInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicTimeIntervalsSet,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    ExpressDeliveryService,
    ExpressSupplier,
    HyperCategory,
    MarketSku,
    Model,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    Shop,
    Tax,
    TimeInfo,
    TimeIntervalByDay,
    TimeIntervalInfo,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.types.offer import OfferDimensions
from core.types.payment_methods import Payment
from core.testcase import TestCase, main
from core.types.combinator import (
    create_delivery_option,
    DeliveryItem,
    Destination,
    CombinatorOffer,
    RoutePoint,
    CombinatorGpsCoords,
    CombinatorExpressWarehouse,
)
from core.report import REQUEST_TIMESTAMP
from market.combinator.proto.grpc.combinator_pb2 import DeliveryService

from core.types.delivery import BlueDeliveryTariff


def make_mock_rearr(**kwds):
    suffix = 'parallel_smm=1.0;ext_snippet=1;no_snippet_arc=1;market_enable_sins_offers_wizard=1'
    rearr = make_rearr(**kwds)
    if rearr != '':
        rearr += ';'
    return rearr + suffix


def make_rearr(**kwds):
    kvlist = ['{}={}'.format(key, kwds[key]) for key in kwds]
    kvlist.sort(key=lambda x: x[0])
    return ';'.join(kvlist)


def get_custom_date_time(date_time):
    # since we test shop in moscow region - we should convert time to the moscow region time zone
    date_time -= timedelta(hours=3)
    return calendar.timegm(date_time.timetuple())


DATETIME_NOW = datetime.fromtimestamp(REQUEST_TIMESTAMP)
DATE_FROM = DATETIME_NOW + timedelta(days=2)
DATE_TO = DATETIME_NOW + timedelta(days=4)


def add_days_to_today(days):
    return datetime(2021, 3, 2, 16, 40) - timedelta(hours=3) + timedelta(days=days)


class _TimeIntervals:
    _TimeFrom = [
        TimeInfo(10, 0),
    ]
    _TimeTo = [
        TimeInfo(18, 00),
    ]
    _TimeIntervalInfo = [
        TimeIntervalInfo(_TimeFrom[0], _TimeTo[0]),
    ]


class _FilterOrigin:
    GL_FILTER = "gl_filter"
    NON_GL_FILTER = "non_gl_filter"
    USER_FILTER = "user_filter"


class _FilterDeliveryInterval:
    TODAY = 0
    TOMORROW = 1
    UP_TO_FIVE_DAYS = 2


class _WarehousesShipmentSchedule:
    shipment_schedule1 = [
        DynamicTimeIntervalsSet(key=1, intervals=[_TimeIntervals._TimeIntervalInfo[0]]),
        DynamicTimeIntervalsSet(key=2, intervals=[_TimeIntervals._TimeIntervalInfo[0]]),
        DynamicTimeIntervalsSet(key=3, intervals=[_TimeIntervals._TimeIntervalInfo[0]]),
        DynamicTimeIntervalsSet(key=4, intervals=[_TimeIntervals._TimeIntervalInfo[0]]),
        DynamicTimeIntervalsSet(key=5, intervals=[_TimeIntervals._TimeIntervalInfo[0]]),
    ]


class _LMSDataForHolidays:
    start_time = DynamicCalendarMetaInfo("27.02.2021", 21)
    days_set = DynamicDaysSet(2, [2, 4])


class _Constants:
    empty_rids = 50
    russia_rids = 225
    moscow_rids = 213
    ekb_rids = 54

    post_outlet_id = 10000
    post_service_id = 10001

    drugs_category_id = 15758037

    model_id = 1
    category_id = 1
    category_id_with_schedule = 2
    category_id_filter = 3
    empty_category_id_filter = 10
    category_id_prohibited = 118

    category_pickup_id = 4
    category_id_prescription_drug = 5

    category_wh2_id = 6

    category_id_for_buybox_fight = 7
    sku_for_buybox_fight = 9
    sku_prohibited_blue = 12
    sku_prohibited_white = 13

    model_id_for_cheap_and_expensive = 8
    category_id_for_cheap_and_expensive = 8
    category_id_for_cheap_and_expensive_parent = 9

    virtual_blue_fesh = 1
    virtual_blue_feed_id = 1

    MAX_UINT32 = 4294967295

    class _Partners:
        dropship_fesh = 2
        dropship_feed_id = 2
        dropship_warehouse_id = 10

        usual_white_fesh = 6

        pickup_fesh = 300
        pickup_feed_id = 301

        delivery_service_id = 100

        courier_bucket_dc_id = 1000
        courier_bucket_id = 2000
        courier_bucket_id_2 = 2002
        courier_bucket_id_3 = 2003
        courier_bucket_id_4 = 2004
        pickup_bucket_dc_id = 3000
        pickup_bucket_id = 4000

        dc_day_from = 1
        dc_day_to = 1
        dc_delivery_cost = 50

        combinator_day_from = 1
        combinator_day_to = 1
        combinator_date_from = DATETIME_NOW + timedelta(days=combinator_day_from)
        combinator_date_to = DATETIME_NOW + timedelta(days=combinator_day_to)
        combinator_delivery_cost = 50

    class _ExpressPartners:
        dropship_fesh = 3
        dropship_feed_id = 3

        dropship_fesh_with_schedule = 4
        dropship_feed_id_with_schedule = 4

        dropship_fesh_wh2 = 5
        dropship_feed_wh2_id = 5

        dropship_warehouse_id = 11

        delivery_service_id = 101

        courier_bucket_dc_id = 1001
        courier_bucket_id = 2001

        dc_day_from = 2
        dc_day_to = 4
        dc_delivery_cost = 50

        combinator_day_from = 2
        combinator_day_to = 4
        combinator_date_from = DATETIME_NOW + timedelta(days=combinator_day_from)
        combinator_date_to = DATETIME_NOW + timedelta(days=combinator_day_to)
        combinator_delivery_cost = 150


class _Shops:
    dropship_shop = Shop(
        fesh=_Constants._Partners.dropship_fesh,
        datafeed_id=_Constants._Partners.dropship_feed_id,
        warehouse_id=_Constants._Partners.dropship_warehouse_id,
        priority_region=_Constants.moscow_rids,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        fulfillment_program=False,
    )

    express_dropship_shop = Shop(
        fesh=_Constants._ExpressPartners.dropship_fesh,
        datafeed_id=_Constants._ExpressPartners.dropship_feed_id,
        warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id,
        priority_region=_Constants.moscow_rids,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        fulfillment_program=False,
        cpa=Shop.CPA_REAL,
    )


class _BlueOffers:
    dropship_offer = BlueOffer(
        offerid='dropship_sku1',
        waremd5='DropshipWaremd5______w',
        price=25,
        feedid=_Constants._Partners.dropship_feed_id,
        weight=5,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._Partners.dropship_fesh,
        delivery_buckets=[_Constants._Partners.courier_bucket_id],
    )

    dropship_msku = MarketSku(
        title="Обычный dropship оффер",
        hyperid=_Constants.category_id_for_cheap_and_expensive,
        sku=111,
        blue_offers=[dropship_offer],
    )

    dropship_offer_2 = BlueOffer(
        offerid='dropship_sku2',
        waremd5='DropshipWaremd5_____2w',
        price=25,
        feedid=_Constants._Partners.dropship_feed_id,
        weight=5,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._Partners.dropship_fesh,
        delivery_buckets=[_Constants._Partners.courier_bucket_id_2],
    )

    dropship_msku_2 = MarketSku(
        title="Обычный dropship оффер 2",
        hyperid=_Constants.category_id_for_cheap_and_expensive,
        sku=222,
        blue_offers=[dropship_offer_2],
    )

    dropship_offer_3 = BlueOffer(
        offerid='dropship_sku3',
        waremd5='DropshipWaremd5_____3w',
        price=25,
        feedid=_Constants._Partners.dropship_feed_id,
        weight=5,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._Partners.dropship_fesh,
        delivery_buckets=[_Constants._Partners.courier_bucket_id_3],
    )

    dropship_msku_3 = MarketSku(
        title="Обычный dropship оффер 3",
        hyperid=_Constants.category_id_for_cheap_and_expensive,
        sku=333,
        blue_offers=[dropship_offer_3],
    )

    dropship_offer_4 = BlueOffer(
        offerid='dropship_sku4',
        waremd5='DropshipWaremd5_____4w',
        price=25,
        feedid=_Constants._Partners.dropship_feed_id,
        weight=5,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._Partners.dropship_fesh,
        delivery_buckets=[_Constants._Partners.courier_bucket_id_4],
    )

    dropship_msku_4 = MarketSku(
        title="Обычный dropship оффер 4", hyperid=_Constants.category_id, sku=444, blue_offers=[dropship_offer_4]
    )

    dropship_offer_5 = BlueOffer(
        offerid='dropship_sku5',
        waremd5='DropshipWaremd5_____5w',
        price=25,
        feedid=_Constants._Partners.dropship_feed_id,
        weight=5,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._Partners.dropship_fesh,
        delivery_buckets=[_Constants._Partners.courier_bucket_id],
    )

    dropship_msku_5 = MarketSku(
        title="Обычный dropship оффер 5", hyperid=_Constants.category_id, sku=555, blue_offers=[dropship_offer_5]
    )

    dropship_offer_6 = BlueOffer(
        offerid='dropship_sku6',
        waremd5='DropshipWaremd5_____6w',
        price=25,
        feedid=_Constants._Partners.dropship_feed_id,
        weight=5,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._Partners.dropship_fesh,
        delivery_buckets=[_Constants._Partners.courier_bucket_id],
    )

    dropship_msku_6 = MarketSku(
        title="Обычный dropship оффер 6", hyperid=_Constants.category_id, sku=666, blue_offers=[dropship_offer_6]
    )

    dropship_offer_7 = BlueOffer(
        offerid='dropship_sku7',
        waremd5='DropshipWaremd5_____7w',
        price=25,
        feedid=_Constants._Partners.dropship_feed_id,
        weight=5,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._Partners.dropship_fesh,
        delivery_buckets=[_Constants._Partners.courier_bucket_id],
    )

    dropship_msku_7 = MarketSku(
        title="Обычный dropship оффер 7", hyperid=_Constants.category_id, sku=777, blue_offers=[dropship_offer_7]
    )

    express_dropship_offer = BlueOffer(
        offerid='express_dropship_sku1',
        waremd5='ExpressDropshipWaremdw',
        price=700,
        feedid=_Constants._ExpressPartners.dropship_feed_id,
        weight=5,
        is_express=True,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._ExpressPartners.dropship_fesh,
        delivery_buckets=[_Constants._ExpressPartners.courier_bucket_id],
    )

    express_dropship_msku = MarketSku(
        title="Экспресс dropship оффер", hyperid=_Constants.category_id, sku=2, blue_offers=[express_dropship_offer]
    )

    express_dropship_offer_2 = BlueOffer(
        offerid='express_dropship_sku2',
        waremd5='ExpressDropshipWarem2w',
        price=700,
        feedid=_Constants._ExpressPartners.dropship_feed_id,
        weight=5,
        is_express=True,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._ExpressPartners.dropship_fesh,
        delivery_buckets=[_Constants._ExpressPartners.courier_bucket_id],
    )

    express_dropship_msku_2 = MarketSku(
        title="Экспресс dropship оффер 2",
        hyperid=_Constants.category_id,
        sku=22,
        blue_offers=[express_dropship_offer_2],
    )

    express_dropship_offer_3 = BlueOffer(
        offerid='express_dropship_sku3',
        waremd5='ExpressDropshipWarem3w',
        price=700,
        feedid=_Constants._ExpressPartners.dropship_feed_id,
        weight=5,
        is_express=True,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._ExpressPartners.dropship_fesh,
        delivery_buckets=[_Constants._ExpressPartners.courier_bucket_id],
    )

    express_dropship_msku_3 = MarketSku(
        title="Экспресс dropship оффер 3",
        hyperid=_Constants.category_id_for_cheap_and_expensive,
        sku=33,
        blue_offers=[express_dropship_offer_3],
    )


class _CombinatorOffers:
    dropship_offer = CombinatorOffer(
        shop_sku=_BlueOffers.dropship_offer.offerid,
        shop_id=_Constants._Partners.dropship_fesh,
        partner_id=_Constants._Partners.dropship_warehouse_id,
        available_count=1,
    )

    dropship_offer_2 = CombinatorOffer(
        shop_sku=_BlueOffers.dropship_offer_2.offerid,
        shop_id=_Constants._Partners.dropship_fesh,
        partner_id=_Constants._Partners.dropship_warehouse_id,
        available_count=1,
    )

    dropship_offer_3 = CombinatorOffer(
        shop_sku=_BlueOffers.dropship_offer_3.offerid,
        shop_id=_Constants._Partners.dropship_fesh,
        partner_id=_Constants._Partners.dropship_warehouse_id,
        available_count=1,
    )

    dropship_offer_4 = CombinatorOffer(
        shop_sku=_BlueOffers.dropship_offer_4.offerid,
        shop_id=_Constants._Partners.dropship_fesh,
        partner_id=_Constants._Partners.dropship_warehouse_id,
        available_count=1,
    )

    dropship_offer_5 = CombinatorOffer(
        shop_sku=_BlueOffers.dropship_offer_5.offerid,
        shop_id=_Constants._Partners.dropship_fesh,
        partner_id=_Constants._Partners.dropship_warehouse_id,
        available_count=1,
    )

    dropship_offer_6 = CombinatorOffer(
        shop_sku=_BlueOffers.dropship_offer_6.offerid,
        shop_id=_Constants._Partners.dropship_fesh,
        partner_id=_Constants._Partners.dropship_warehouse_id,
        available_count=1,
    )

    dropship_offer_7 = CombinatorOffer(
        shop_sku=_BlueOffers.dropship_offer_7.offerid,
        shop_id=_Constants._Partners.dropship_fesh,
        partner_id=_Constants._Partners.dropship_warehouse_id,
        available_count=1,
    )

    express_dropship_offer = CombinatorOffer(
        shop_sku=_BlueOffers.express_dropship_offer.offerid,
        shop_id=_Constants._ExpressPartners.dropship_fesh,
        partner_id=_Constants._ExpressPartners.dropship_warehouse_id,
        available_count=1,
    )

    express_dropship_offer_2 = CombinatorOffer(
        shop_sku=_BlueOffers.express_dropship_offer_2.offerid,
        shop_id=_Constants._ExpressPartners.dropship_fesh,
        partner_id=_Constants._ExpressPartners.dropship_warehouse_id,
        available_count=1,
    )

    express_dropship_offer_3 = CombinatorOffer(
        shop_sku=_BlueOffers.express_dropship_offer_3.offerid,
        shop_id=_Constants._ExpressPartners.dropship_fesh,
        partner_id=_Constants._ExpressPartners.dropship_warehouse_id,
        available_count=1,
    )


class _Requests:
    prime_request = 'place=prime&rgb=blue&allow-collapsing=0&rids={rids}&hid={hid}'
    prime_request_msku = 'place=prime&rgb=blue&allow-collapsing=0&rids={rids}&market-sku={msku}'

    actual_delivery_request = (
        'place=actual_delivery'
        '&pp=18'
        '&rgb=blue'
        '&pickup-options=grouped'
        '&pickup-options-extended-grouping=1'
        '&rids={rids}'
        '&offers-list={offers}'
        '&combinator=1'
        '&no-delivery-discount=1'
        '&rearr-factors=disable_delivery_calculator_call_for_blue_offers=0'
    )


class _Points:
    def service_time(hour, minute, day=0):
        return datetime(year=2020, month=8, day=2 + day, hour=hour, minute=minute)

    express_movement = RoutePoint(
        point_ids=Destination(partner_id=_Constants._ExpressPartners.delivery_service_id),
        segment_id=512002,
        segment_type="movement",
        services=(
            (DeliveryService.INBOUND, "INBOUND", service_time(13, 0), timedelta(seconds=15)),
            (DeliveryService.INTERNAL, "MOVEMENT", service_time(13, 1), timedelta(minutes=29)),
            (DeliveryService.OUTBOUND, "SHIPMENT", service_time(13, 30), timedelta(hours=1)),
        ),
    )

    express_dropship_warehouse = RoutePoint(
        point_ids=Destination(partner_id=_Constants._ExpressPartners.dropship_warehouse_id),
        segment_id=512101,
        segment_type="warehouse",
        services=(
            (DeliveryService.INTERNAL, "CUTOFF", service_time(10, 25), timedelta()),
            (DeliveryService.INTERNAL, "PROCESSING", service_time(10, 25), timedelta(hours=2)),
            (DeliveryService.OUTBOUND, "SHIPMENT", service_time(12, 25), timedelta(minutes=35)),
        ),
        partner_type="DROPSHIP",
    )

    movement = RoutePoint(
        point_ids=Destination(partner_id=_Constants._Partners.delivery_service_id),
        segment_id=512002,
        segment_type="movement",
        services=(
            (DeliveryService.INBOUND, "INBOUND", service_time(13, 0), timedelta(seconds=15)),
            (DeliveryService.INTERNAL, "MOVEMENT", service_time(13, 1), timedelta(minutes=29)),
            (DeliveryService.OUTBOUND, "SHIPMENT", service_time(13, 30), timedelta(hours=1)),
        ),
    )

    dropship_warehouse = RoutePoint(
        point_ids=Destination(partner_id=_Constants._Partners.dropship_warehouse_id),
        segment_id=512104,
        segment_type="warehouse",
        services=(
            (DeliveryService.INTERNAL, "CUTOFF", service_time(10, 25), timedelta()),
            (DeliveryService.INTERNAL, "PROCESSING", service_time(10, 25), timedelta(hours=2)),
            (DeliveryService.OUTBOUND, "SHIPMENT", service_time(12, 25), timedelta(minutes=35)),
        ),
        partner_type="DROPSHIP",
    )


class T(TestCase):
    """
    Набор тестов для проекта "Самый быстрый фильтр". Краткое описание:
    """

    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [Region(rid=_Constants.moscow_rids, name="Москва", tz_offset=10800)]

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [
            _Shops.dropship_shop,
            _Shops.express_dropship_shop,
        ]

    @classmethod
    def prepare_express_partners(cls):
        cls.index.express_partners.suppliers += [
            ExpressSupplier(
                feed_id=_Constants._ExpressPartners.dropship_feed_id,
                supplier_id=_Constants._ExpressPartners.dropship_fesh,
                warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id,
            )
        ]
        cls.index.express_partners.suppliers += [
            ExpressSupplier(
                feed_id=_Constants._ExpressPartners.dropship_feed_id_with_schedule,
                supplier_id=_Constants._ExpressPartners.dropship_fesh_with_schedule,
                warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id,
                working_schedule=[TimeIntervalByDay(day=i, time_from='10:00', time_to='18:00') for i in range(0, 5)],
            )
        ]
        cls.index.express_partners.delivery_services += [
            ExpressDeliveryService(
                delivery_service_id=_Constants._ExpressPartners.delivery_service_id, delivery_price_for_user=350
            )
        ]

        cls.settings.disable_random = 1
        # set for all requests Tuesday Mar 02 2021 16:40:00 GMT+0300
        # since we test shop in moscow region - we should convert time to the moscow region time zone
        tuesday_afternoon = datetime(2021, 3, 2, 16, 40) - timedelta(hours=3)
        cls.settings.microseconds_for_disabled_random = calendar.timegm(tuesday_afternoon.timetuple()) * 1000000

    @classmethod
    def prepare_delivery_buckets(cls):
        for bucket_id, day_from, day_to in (
            (_Constants._Partners.courier_bucket_id, 0, 0),
            (_Constants._Partners.courier_bucket_id_2, 1, 1),
            (_Constants._Partners.courier_bucket_id_3, 4, 4),
            (_Constants._Partners.courier_bucket_id_4, 12, 12),
        ):
            cls.index.delivery_buckets += [
                DeliveryBucket(
                    bucket_id=bucket_id,
                    dc_bucket_id=_Constants._Partners.courier_bucket_dc_id,
                    fesh=_Constants._Partners.dropship_fesh,
                    carriers=[_Constants._Partners.delivery_service_id],
                    regional_options=[
                        RegionalDelivery(
                            rid=_Constants.moscow_rids,
                            options=[
                                DeliveryOption(
                                    price=_Constants._Partners.dc_delivery_cost,
                                    day_from=day_from,
                                    day_to=day_to,
                                )
                            ],
                            payment_methods=[
                                Payment.PT_YANDEX,
                                Payment.PT_CASH_ON_DELIVERY,
                                Payment.PT_CARD_ON_DELIVERY,
                            ],
                        )
                    ],
                    delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                ),
            ]
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=_Constants._ExpressPartners.courier_bucket_id,
                dc_bucket_id=_Constants._ExpressPartners.courier_bucket_dc_id,
                fesh=_Constants._ExpressPartners.dropship_fesh,
                carriers=[_Constants._ExpressPartners.delivery_service_id],
                regional_options=[
                    RegionalDelivery(
                        rid=_Constants.moscow_rids,
                        options=[
                            DeliveryOption(
                                price=_Constants._ExpressPartners.dc_delivery_cost,
                                day_from=_Constants._ExpressPartners.dc_day_from,
                                day_to=_Constants._ExpressPartners.dc_day_to,
                            )
                        ],
                        payment_methods=[Payment.PT_YANDEX, Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
                    ),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

    @classmethod
    def prepare_outlets(cls):
        cls.index.outlets += [
            Outlet(
                point_id=_Constants.post_outlet_id,
                delivery_service_id=_Constants.post_service_id,
                region=_Constants.moscow_rids,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=_Constants.post_service_id,
                    day_from=1,
                    day_to=1,
                    order_before=2,
                    work_in_holiday=True,
                    price=100,
                ),
                working_days=[i for i in range(10)],
            )
        ]

    @classmethod
    def prepare_pickup_buckets(cls):
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=_Constants._Partners.pickup_bucket_id,
                dc_bucket_id=_Constants._Partners.pickup_bucket_dc_id,
                fesh=_Constants._Partners.pickup_fesh,
                carriers=[_Constants.post_service_id],
                options=[
                    PickupOption(
                        outlet_id=_Constants.post_outlet_id,
                        price=_Constants._Partners.dc_delivery_cost,
                        day_from=_Constants._Partners.dc_day_from,
                        day_to=_Constants._Partners.dc_day_to,
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            )
        ]

    @classmethod
    def prepare_warehouses(cls):
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[_Constants.russia_rids, _Constants.moscow_rids],
                warehouse_with_priority=[
                    WarehouseWithPriority(warehouse_id=_Constants._Partners.dropship_warehouse_id, priority=1),
                    WarehouseWithPriority(warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id, priority=1),
                ],
            )
        ]

        cls.index.express_warehouses.add(
            _Constants._ExpressPartners.dropship_warehouse_id, region_id=213, latitude=55.7, longitude=37.7
        )

    @classmethod
    def prepare_lms(cls):
        cls.settings.lms_autogenerate = False

        cls.dynamic.lms += [DynamicDaysSet(key=1, days=[])]

        cls.dynamic.lms += [_LMSDataForHolidays.start_time, _LMSDataForHolidays.days_set]

        for wh_id, ds_id, ds_name in [
            (
                _Constants._Partners.dropship_warehouse_id,
                _Constants._Partners.delivery_service_id,
                'ordinary_delivery_serivce',
            ),
            (
                _Constants._ExpressPartners.dropship_warehouse_id,
                _Constants._ExpressPartners.delivery_service_id,
                'express_delivery_service',
            ),
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
                DynamicWarehouseInfo(
                    id=wh_id,
                    home_region=_Constants.moscow_rids,
                    is_express=False,
                    shipment_schedule=_WarehousesShipmentSchedule.shipment_schedule1,
                    holidays_days_set_key=2,
                ),
                DynamicWarehouseToWarehouseInfo(warehouse_from=wh_id, warehouse_to=wh_id),
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

        cls.dynamic.lms += [
            DynamicWarehouseInfo(
                id=_Constants._ExpressPartners.dropship_warehouse_id,
                home_region=_Constants.moscow_rids,
                is_express=True,
                shipment_schedule=_WarehousesShipmentSchedule.shipment_schedule1,
                holidays_days_set_key=2,
            ),
        ]

    @classmethod
    def prepare_nordstream(cls):
        cls.settings.nordstream_types = [
            0,
        ]  # only courier

    @classmethod
    def prepare_combinator(cls):
        report_start_time_in_ms = REQUEST_TIMESTAMP * 10**6
        cls.combinator.set_start_date(microseconds_for_disabled_random=report_start_time_in_ms)
        cls.settings.default_search_experiment_flags += ['enable_dsbs_combinator_request_in_actual_delivery=0']

        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_BlueOffers.dropship_offer, _Shops.dropship_shop),
            courier_stats=DeliveryStats(
                cost=_Constants._Partners.combinator_delivery_cost,
                day_from=0,
                day_to=0,
            ),
        )

        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_BlueOffers.dropship_offer_2, _Shops.dropship_shop),
            courier_stats=DeliveryStats(
                cost=_Constants._Partners.combinator_delivery_cost,
                day_from=1,
                day_to=1,
            ),
        )

        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_BlueOffers.dropship_offer_3, _Shops.dropship_shop),
            courier_stats=DeliveryStats(
                cost=_Constants._Partners.combinator_delivery_cost,
                day_from=2,
                day_to=2,
            ),
        )

        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_BlueOffers.dropship_offer_4, _Shops.dropship_shop),
            courier_stats=DeliveryStats(
                cost=_Constants._Partners.combinator_delivery_cost,
                day_from=3,
                day_to=3,
            ),
        )
        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_BlueOffers.dropship_offer_5, _Shops.dropship_shop),
            courier_stats=DeliveryStats(
                cost=_Constants._Partners.combinator_delivery_cost,
                day_from=4,
                day_to=4,
            ),
        )

        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_BlueOffers.dropship_offer_6, _Shops.dropship_shop),
            courier_stats=DeliveryStats(
                cost=_Constants._Partners.combinator_delivery_cost,
                day_from=5,
                day_to=5,
            ),
        )

        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_BlueOffers.dropship_offer_7, _Shops.dropship_shop),
            courier_stats=DeliveryStats(
                cost=_Constants._Partners.combinator_delivery_cost,
                day_from=6,
                day_to=6,
            ),
        )

        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_BlueOffers.express_dropship_offer, _Shops.express_dropship_shop),
            courier_stats=DeliveryStats(
                cost=_Constants._ExpressPartners.combinator_delivery_cost,
                day_from=0,
                day_to=6,
            ),
        )

        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_BlueOffers.express_dropship_offer_2, _Shops.express_dropship_shop),
            courier_stats=DeliveryStats(
                cost=_Constants._ExpressPartners.combinator_delivery_cost,
                day_from=0,
                day_to=6,
            ),
        )

        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_BlueOffers.express_dropship_offer_3, _Shops.express_dropship_shop),
            courier_stats=DeliveryStats(
                cost=_Constants._ExpressPartners.combinator_delivery_cost,
                day_from=0,
                day_to=4,
            ),
        )

        cls.combinator.on_courier_options_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=5000,
                    dimensions=[30, 30, 30],
                    cargo_types=[],
                    offers=[_CombinatorOffers.dropship_offer],
                    price=_BlueOffers.dropship_offer.price,
                )
            ],
            destination=Destination(region_id=_Constants.moscow_rids),
            payment_methods=[],
            total_price=_BlueOffers.dropship_offer.price,
        ).respond_with_courier_options(
            options=[
                create_delivery_option(
                    cost=_Constants._Partners.combinator_delivery_cost,
                    date_from=add_days_to_today(0),
                    date_to=add_days_to_today(0),
                    time_from=time(10, 0),
                    time_to=time(18, 0),
                    delivery_service_id=_Constants._Partners.delivery_service_id,
                )
            ]
        )

        cls.combinator.on_courier_options_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=5000,
                    dimensions=[30, 30, 30],
                    cargo_types=[],
                    offers=[_CombinatorOffers.dropship_offer_2],
                    price=_BlueOffers.dropship_offer_2.price,
                )
            ],
            destination=Destination(region_id=_Constants.moscow_rids),
            payment_methods=[],
            total_price=_BlueOffers.dropship_offer_2.price,
        ).respond_with_courier_options(
            options=[
                create_delivery_option(
                    cost=_Constants._Partners.combinator_delivery_cost,
                    date_from=add_days_to_today(1),
                    date_to=add_days_to_today(1),
                    time_from=time(10, 0),
                    time_to=time(18, 0),
                    delivery_service_id=_Constants._Partners.delivery_service_id,
                )
            ]
        )

        cls.combinator.on_courier_options_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=5000,
                    dimensions=[30, 30, 30],
                    cargo_types=[],
                    offers=[_CombinatorOffers.dropship_offer_2],
                    price=_BlueOffers.dropship_offer_2.price,
                )
            ],
            destination=Destination(region_id=_Constants.moscow_rids),
            payment_methods=[],
            total_price=_BlueOffers.dropship_offer_2.price,
        ).respond_with_courier_options(
            options=[
                create_delivery_option(
                    cost=_Constants._Partners.combinator_delivery_cost,
                    date_from=add_days_to_today(1),
                    date_to=add_days_to_today(1),
                    time_from=time(10, 0),
                    time_to=time(18, 0),
                    delivery_service_id=_Constants._Partners.delivery_service_id,
                )
            ]
        )

        cls.combinator.on_courier_options_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=5000,
                    dimensions=[30, 30, 30],
                    cargo_types=[],
                    offers=[_CombinatorOffers.dropship_offer_3],
                    price=_BlueOffers.dropship_offer_3.price,
                )
            ],
            destination=Destination(region_id=_Constants.moscow_rids),
            payment_methods=[],
            total_price=_BlueOffers.dropship_offer_3.price,
        ).respond_with_courier_options(
            options=[
                create_delivery_option(
                    cost=_Constants._Partners.combinator_delivery_cost,
                    date_from=add_days_to_today(2),
                    date_to=add_days_to_today(2),
                    time_from=time(10, 0),
                    time_to=time(18, 0),
                    delivery_service_id=_Constants._Partners.delivery_service_id,
                )
            ]
        )

        cls.combinator.on_courier_options_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=5000,
                    dimensions=[30, 30, 30],
                    cargo_types=[],
                    offers=[_CombinatorOffers.dropship_offer_4],
                    price=_BlueOffers.dropship_offer_4.price,
                )
            ],
            destination=Destination(region_id=_Constants.moscow_rids),
            payment_methods=[],
            total_price=_BlueOffers.dropship_offer_4.price,
        ).respond_with_courier_options(
            options=[
                create_delivery_option(
                    cost=_Constants._Partners.combinator_delivery_cost,
                    date_from=add_days_to_today(3),
                    date_to=add_days_to_today(3),
                    time_from=time(10, 0),
                    time_to=time(18, 0),
                    delivery_service_id=_Constants._Partners.delivery_service_id,
                )
            ]
        )

        cls.combinator.on_courier_options_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=5000,
                    dimensions=[30, 30, 30],
                    cargo_types=[],
                    offers=[_CombinatorOffers.dropship_offer_5],
                    price=_BlueOffers.dropship_offer_5.price,
                )
            ],
            destination=Destination(region_id=_Constants.moscow_rids),
            payment_methods=[],
            total_price=_BlueOffers.dropship_offer_5.price,
        ).respond_with_courier_options(
            options=[
                create_delivery_option(
                    cost=_Constants._Partners.combinator_delivery_cost,
                    date_from=add_days_to_today(4),
                    date_to=add_days_to_today(4),
                    time_from=time(10, 0),
                    time_to=time(18, 0),
                    delivery_service_id=_Constants._Partners.delivery_service_id,
                )
            ]
        )

        cls.combinator.on_courier_options_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=5000,
                    dimensions=[30, 30, 30],
                    cargo_types=[],
                    offers=[_CombinatorOffers.dropship_offer_6],
                    price=_BlueOffers.dropship_offer_6.price,
                )
            ],
            destination=Destination(region_id=_Constants.moscow_rids),
            payment_methods=[],
            total_price=_BlueOffers.dropship_offer_6.price,
        ).respond_with_courier_options(
            options=[
                create_delivery_option(
                    cost=_Constants._Partners.combinator_delivery_cost,
                    date_from=add_days_to_today(5),
                    date_to=add_days_to_today(5),
                    time_from=time(10, 0),
                    time_to=time(18, 0),
                    delivery_service_id=_Constants._Partners.delivery_service_id,
                )
            ]
        )

        cls.combinator.on_courier_options_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=5000,
                    dimensions=[30, 30, 30],
                    cargo_types=[],
                    offers=[_CombinatorOffers.dropship_offer_7],
                    price=_BlueOffers.dropship_offer_7.price,
                )
            ],
            destination=Destination(region_id=_Constants.moscow_rids),
            payment_methods=[],
            total_price=_BlueOffers.dropship_offer_7.price,
        ).respond_with_courier_options(
            options=[
                create_delivery_option(
                    cost=_Constants._Partners.combinator_delivery_cost,
                    date_from=add_days_to_today(6),
                    date_to=add_days_to_today(6),
                    time_from=time(10, 0),
                    time_to=time(18, 0),
                    delivery_service_id=_Constants._Partners.delivery_service_id,
                )
            ]
        )

        cls.combinator.on_courier_options_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=5000,
                    dimensions=[30, 30, 30],
                    cargo_types=[],
                    offers=[_CombinatorOffers.express_dropship_offer],
                    price=_BlueOffers.express_dropship_offer.price,
                )
            ],
            destination=Destination(region_id=_Constants.moscow_rids),
            payment_methods=[],
            total_price=_BlueOffers.express_dropship_offer.price,
        ).respond_with_courier_options(
            options=[
                create_delivery_option(
                    cost=_Constants._ExpressPartners.combinator_delivery_cost,
                    date_from=add_days_to_today(0),
                    date_to=add_days_to_today(6),
                    time_from=time(22, 0),
                    time_to=time(23, 0),
                    delivery_service_id=_Constants._ExpressPartners.delivery_service_id,
                )
            ]
        )

        cls.combinator.on_courier_options_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=5000,
                    dimensions=[30, 30, 30],
                    cargo_types=[],
                    offers=[_CombinatorOffers.express_dropship_offer_2],
                    price=_BlueOffers.express_dropship_offer_2.price,
                )
            ],
            destination=Destination(region_id=_Constants.moscow_rids),
            payment_methods=[],
            total_price=_BlueOffers.express_dropship_offer_2.price,
        ).respond_with_courier_options(
            options=[
                create_delivery_option(
                    cost=_Constants._ExpressPartners.combinator_delivery_cost,
                    date_from=add_days_to_today(0),
                    date_to=add_days_to_today(6),
                    time_from=time(0, 30),
                    time_to=time(1, 30),
                    delivery_service_id=_Constants._ExpressPartners.delivery_service_id,
                )
            ]
        )

        cls.combinator.on_courier_options_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=5000,
                    dimensions=[30, 30, 30],
                    cargo_types=[],
                    offers=[_CombinatorOffers.express_dropship_offer_3],
                    price=_BlueOffers.express_dropship_offer_3.price,
                )
            ],
            destination=Destination(region_id=_Constants.moscow_rids),
            payment_methods=[],
            total_price=_BlueOffers.express_dropship_offer_3.price,
        ).respond_with_courier_options(
            options=[
                create_delivery_option(
                    cost=_Constants._ExpressPartners.combinator_delivery_cost,
                    date_from=add_days_to_today(0),
                    date_to=add_days_to_today(4),
                    time_from=time(8, 0),
                    time_to=time(18, 0),
                    delivery_service_id=_Constants._ExpressPartners.delivery_service_id,
                )
            ]
        )

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.mskus += [
            _BlueOffers.dropship_msku,
            _BlueOffers.dropship_msku_2,
            _BlueOffers.dropship_msku_3,
            _BlueOffers.dropship_msku_4,
            _BlueOffers.dropship_msku_5,
            _BlueOffers.dropship_msku_6,
            _BlueOffers.dropship_msku_7,
            _BlueOffers.express_dropship_msku,
            _BlueOffers.express_dropship_msku_2,
            _BlueOffers.express_dropship_msku_3,
        ]

        cls.index.models += [
            Model(hid=_Constants.drugs_category_id, hyperid=_Constants.category_id_prescription_drug),
            Model(hid=_Constants.category_id_for_buybox_fight, hyperid=_Constants.category_id_for_buybox_fight),
            Model(
                hid=_Constants.category_id_for_cheap_and_expensive, hyperid=_Constants.model_id_for_cheap_and_expensive
            ),
            Model(hid=_Constants.category_id_prohibited, hyperid=_Constants.category_id_prohibited),
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=_Constants.category_id_for_cheap_and_expensive_parent,
                children=[
                    HyperCategory(hid=_Constants.category_id_for_cheap_and_expensive),
                ],
            ),
            HyperCategory(hid=_Constants.category_id_prohibited),
        ]

    @classmethod
    def prepare_tariff_fast(cls):
        '''
        Подготавливаем параметры цены доставки быстрых оферов
        '''
        EKB_FAST_TARIFFS = [
            BlueDeliveryTariff(user_price=150, large_size=0, price_to=1200),
            BlueDeliveryTariff(user_price=250),
        ]

        EXP_UNIFIED_TARIFFS = "unified"
        UNIFIED_FAST_TARIFFS = [
            BlueDeliveryTariff(user_price=199, large_size=0, price_to=1200),
            BlueDeliveryTariff(user_price=99, large_size=0, for_plus=1, price_to=1200),
            BlueDeliveryTariff(user_price=39, large_size=0, for_plus=1),
            BlueDeliveryTariff(user_price=99),
        ]

        DEFAULT_FAST_TARIFFS = [BlueDeliveryTariff(user_price=350)]

        DEFAULT_TARIFFS = [BlueDeliveryTariff(user_price=77)]

        # Тариф быстрых оферов в конкретный город
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=EKB_FAST_TARIFFS, regions=[_Constants.ekb_rids], fast_delivery=True
        )

        # Эксперимент с едиными тарифами, тариф экспресс оферов в конкретном городе
        cls.index.blue_delivery_modifiers.add_modifier(
            exp_name=EXP_UNIFIED_TARIFFS,
            tariffs=UNIFIED_FAST_TARIFFS,
            regions=[_Constants.moscow_rids],
            is_express=True,
        )

        # Тариф быстрых (fast) оферов везде
        cls.index.blue_delivery_modifiers.set_default_modifier(tariffs=DEFAULT_FAST_TARIFFS, fast_delivery=True)
        # Эксперимент с едиными тарифами, тариф экспресс (express) оферов везде
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=DEFAULT_FAST_TARIFFS, exp_name=EXP_UNIFIED_TARIFFS, is_express=True
        )
        # Тариф обычных оферов везде
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=DEFAULT_TARIFFS,
        )
        # Эксперимент с едиными тарифами, тариф обычных офферов оферов везде
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=DEFAULT_TARIFFS,
            exp_name=EXP_UNIFIED_TARIFFS,
        )

    @classmethod
    def prepare_nearest_delivery_from_combinator(cls):
        for gps, nearest_day, nearest_interval, in (
            (1.0, _FilterDeliveryInterval.TODAY, ((12, 30), (13, 45))),
            (2.0, _FilterDeliveryInterval.TODAY, ((16, 15), (17, 30))),
            (5.0, _FilterDeliveryInterval.TOMORROW, ((20, 0), (21, 15))),
            (6.0, _FilterDeliveryInterval.UP_TO_FIVE_DAYS, ((12, 30), (13, 45))),
        ):
            cls.combinatorExpress.on_express_warehouses_request(
                region_id=_Constants.moscow_rids,
                gps_coords=CombinatorGpsCoords(gps, gps),
                rear_factors=make_mock_rearr(
                    market_hyperlocal_context_mmap_version=5,
                    enable_dsbs_filter_by_delivery_interval=0,
                    honest_express_filter=True,
                    fastest_delivery_interval=1,
                    market_blue_prime_without_delivery=0,
                    market_promo_datetime=str(get_custom_date_time(datetime(2021, 3, 4, 12, 0))),
                ),
            ).respond_with_express_warehouses(
                [
                    CombinatorExpressWarehouse(
                        warehouse_id=11,
                        zone_id=1,
                        nearest_delivery_day=nearest_day,
                        nearest_delivery_interval=nearest_interval,
                    )
                ]
            )

            cls.combinatorExpress.on_express_warehouses_request(
                region_id=_Constants.moscow_rids,
                gps_coords=CombinatorGpsCoords(gps + 2.0, gps + 2.0),
                rear_factors=make_mock_rearr(
                    market_hyperlocal_context_mmap_version=5,
                    enable_dsbs_filter_by_delivery_interval=2,
                    honest_express_filter=True,
                    fastest_delivery_interval=1,
                    market_blue_prime_without_delivery=0,
                    market_promo_datetime=str(get_custom_date_time(datetime(2021, 3, 4, 12, 0))),
                ),
            ).respond_with_express_warehouses(
                [
                    CombinatorExpressWarehouse(
                        warehouse_id=11,
                        zone_id=1,
                        nearest_delivery_day=nearest_day,
                        nearest_delivery_interval=nearest_interval,
                    )
                ]
            )

        cls.combinatorExpress.on_express_warehouses_request(
            region_id=_Constants.moscow_rids,
            gps_coords=CombinatorGpsCoords(6.0, 6.0),
            rear_factors=make_mock_rearr(
                market_hyperlocal_context_mmap_version=3,
                enable_dsbs_filter_by_delivery_interval=2,
                honest_express_filter=True,
                fastest_delivery_interval=1,
                market_promo_datetime=str(get_custom_date_time(datetime(2021, 3, 4, 12, 0))),
                get_rid_of_direct_shipping=0,
            ),
        ).respond_with_express_warehouses(
            [
                CombinatorExpressWarehouse(
                    warehouse_id=11,
                    zone_id=1,
                    nearest_delivery_day=_FilterDeliveryInterval.TODAY,
                    nearest_delivery_interval=((12, 30), (13, 45)),
                )
            ]
        )

    def date_time_to_string(self, dt, tm):
        return '{year}{month}{day}.{hour}{minute}'.format(
            year=dt.year, month=dt.month, day=dt.day, hour=tm.hour, minute=tm.minute
        )

    def delivery_interval_string(self, date_from, time_from, date_to, time_to):
        return 'delivery-interval={}-{}'.format(
            self.date_time_to_string(date_from, time_from), self.date_time_to_string(date_to, time_to)
        )

    def test_actual_deliverty_courier_options(self):
        '''
        Проверяем курьерские опции доставки у офферов
        https://st.yandex-team.ru/MARKETOUT-46392
        '''

        offer_to_delivery_days = {
            'ExpressDropshipWaremdw': (0, 6),  # оффер доставляется каждый день, начиная с сегодняшнего
            'ExpressDropshipWarem2w': (0, 6),
            'ExpressDropshipWarem3w': (0, 4),
            'DropshipWaremd5______w': (0, 0),  # оффер доставляется только сегодня
            'DropshipWaremd5_____2w': (1, 1),  # оффер доставляется только завтра
            'DropshipWaremd5_____3w': (2, 2),  # оффер доставляется только послезавтра
            'DropshipWaremd5_____4w': (3, 3),
            'DropshipWaremd5_____5w': (4, 4),
            'DropshipWaremd5_____6w': (5, 5),
            'DropshipWaremd5_____7w': (6, 6),
        }

        for ware_md5, day_from_day_to in offer_to_delivery_days.items():
            response = self.report.request_json(
                _Requests.actual_delivery_request.format(
                    offers="{}:1".format(ware_md5),
                    rids=_Constants.moscow_rids,
                )
            )

            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': "deliveryGroup",
                            'offers': [
                                {
                                    'entity': "offer",
                                    'wareId': ware_md5,
                                }
                            ],
                            'delivery': {'options': [{'dayFrom': day_from_day_to[0], 'dayTo': day_from_day_to[1]}]},
                        }
                    ],
                },
            )

    def test_delivery_interval_with_1_2h_option(self):
        '''
        Проверяем правильный подсчет пункта "found" фильтра delivery_inerval после фикса,
        т.к. до исправления к "found" пункта 1-2ч фильтра "Срок доставки" суммировались
        "found" всех остальных пунктов (0 - сегодня, 1 - завтра, 5 - до 5 дней), что вкорне неверно.

        В запросе используется категория, ктр содержит по одному офферу на каждый пункт фильтра.
        Проверяем, что значения в "found" будут расти на 1 при переходе от текущего интервала к более позднему,
        т.к. каждый следующий интервал должен включать офферы из предыдущего + офферы только для данного интервала.
        https://st.yandex-team.ru/MARKETOUT-46880
        '''

        response = self.report.request_json(
            _Requests.prime_request.format(
                rids=_Constants.moscow_rids, hid=str(_Constants.category_id_for_cheap_and_expensive)
            )
            + "&gps=lat:{gps};lon:{gps}".format(gps=6.0)
            + "&rearr-factors="
            + make_rearr(
                market_hyperlocal_context_mmap_version=3,
                enable_dsbs_filter_by_delivery_interval=2,
                honest_express_filter=True,
                fastest_delivery_interval=1,
                market_promo_datetime=str(get_custom_date_time(datetime(2021, 3, 4, 12, 0))),
                get_rid_of_direct_shipping=0,
            )
        )

        self.assertFragmentIn(
            response,
            {
                "id": "delivery-interval",
                "name": "Срок доставки",
                "filterOrigin": _FilterOrigin.NON_GL_FILTER,
                "values": [
                    {"value": "12", "found": 1},
                    {"value": "0", "found": 2},
                    {"value": "1", "found": 3},
                    {"value": "5", "found": 4},
                ],
            },
        )

    def test_hide_fastest_delivery_filter(self):
        '''
        Проверяем работу hide-filter для всех фильтров самой быстрой доставки
        https://st.yandex-team.ru/MARKETOUT-47174
        '''
        hide_filter_map = {
            '1-2h': '&hide-filter=fastest-delivery-12',
            'today': '&hide-filter=fastest-delivery-0',
            'tomorrow': '&hide-filter=fastest-delivery-1',
            'up_to_5_days': '&hide-filter=fastest-delivery-5',
        }

        for hide_filter_options, expected_filter_id in (
            ("", "12"),  # no hiding
            (hide_filter_map['1-2h'], "0"),
            (hide_filter_map['1-2h'] + hide_filter_map['today'], "1"),
            (hide_filter_map['1-2h'] + hide_filter_map['today'] + hide_filter_map['tomorrow'], "5"),
            (
                hide_filter_map['1-2h']
                + hide_filter_map['today']
                + hide_filter_map['tomorrow']
                + hide_filter_map['up_to_5_days'],
                "",
            ),
        ):
            response = self.report.request_json(
                _Requests.prime_request.format(
                    rids=_Constants.moscow_rids, hid=str(_Constants.category_id_for_cheap_and_expensive)
                )
                + "&gps=lat:{gps};lon:{gps}".format(gps=6.0)
                + hide_filter_options
                + "&rearr-factors="
                + make_rearr(
                    market_hyperlocal_context_mmap_version=3,
                    enable_dsbs_filter_by_delivery_interval=2,
                    honest_express_filter=True,
                    fastest_delivery_interval=1,
                    market_promo_datetime=str(get_custom_date_time(datetime(2021, 3, 4, 12, 0))),
                    get_rid_of_direct_shipping=0,
                )
            )

            if (
                hide_filter_options
                != hide_filter_map['1-2h']
                + hide_filter_map['today']
                + hide_filter_map['tomorrow']
                + hide_filter_map['up_to_5_days']
            ):
                self.assertFragmentIn(
                    response,
                    {
                        "id": "fastest-delivery-{}".format(expected_filter_id),
                    },
                )
            else:  # no fastest filters
                for filter_id in ("12", "0", "1", "5"):
                    self.assertFragmentNotIn(
                        response,
                        {
                            "id": "fastest-delivery-{}".format(filter_id),
                        },
                    )

    def test_fastest_delivery_express(self):
        '''
        Проверяем отображение фильтра самая быстрая доставка для экспрессных офферов
        Должен быть такой же как и первый пункт в фильтре срок доставки
        Меняем за счет флага enable_dsbs_filter_by_delivery_interval
        '''
        for gps, fastest_filter_id in (
            (1.0, "12"),
            (2.0, "0"),
            (5.0, "1"),
            (6.0, "5"),
        ):
            for flag_value in (0, 2):
                response = self.report.request_json(
                    _Requests.prime_request_msku.format(
                        rids=_Constants.moscow_rids, msku=str(_BlueOffers.express_dropship_msku.sku)
                    )
                    + "&gps=lat:{gps};lon:{gps}".format(gps=gps + flag_value)
                    + "&rearr-factors="
                    + make_rearr(
                        market_hyperlocal_context_mmap_version=5,
                        enable_dsbs_filter_by_delivery_interval=flag_value,
                        honest_express_filter=True,
                        fastest_delivery_interval=1,
                        market_blue_prime_without_delivery=0,
                        market_promo_datetime=str(get_custom_date_time(datetime(2021, 3, 4, 12, 0))),
                    )
                )

                if flag_value == 0:
                    if fastest_filter_id in ("12", "0"):
                        self.assertFragmentIn(
                            response,
                            {
                                "id": "filter-express-delivery-today",
                                "values": [
                                    {"value": "1", "found": 1},
                                ],
                            },
                        )

                expected_fastest_filter_id = fastest_filter_id

                if fastest_filter_id == "12" and flag_value == 0:
                    expected_fastest_filter_id = "0"

                self.assertFragmentIn(
                    response,
                    {
                        "id": "fastest-delivery-{}".format(expected_fastest_filter_id),
                        "filterOrigin": _FilterOrigin.USER_FILTER,
                        "values": [
                            {"value": "1", "found": 1},
                        ],
                    },
                )

                self.assertFragmentIn(
                    response,
                    {
                        "id": "delivery-interval",
                        "name": "Срок доставки",
                        "filterOrigin": _FilterOrigin.NON_GL_FILTER,
                        "values": [
                            {
                                "value": "{}".format(expected_fastest_filter_id),
                                "found": 1,
                            },
                        ],
                    },
                )

    def test_delivery_interval_with_checked_1_2h_option(self):
        '''
        Проверяем, что при выборе пункта пункта 1-2ч фильтра "Срок доставки"
        в "found" опций фильтра "Сегодня" и "Сегодня или завтра" фильтра посчитаются экспрессные офферы,
        доставляемые за 1-2ч, тем самым пункты гарантированно появятся на выдаче фронту
        https://st.yandex-team.ru/MARKETOUT-47446
        '''

        response = self.report.request_json(
            _Requests.prime_request_msku.format(
                rids=_Constants.moscow_rids, msku=str(_BlueOffers.express_dropship_msku.sku)
            )
            + "&gps=lat:{gps};lon:{gps}".format(gps=3.0)
            + "&delivery_interval=12"
            + "&rearr-factors="
            + make_rearr(
                market_hyperlocal_context_mmap_version=5,
                enable_dsbs_filter_by_delivery_interval=2,
                honest_express_filter=True,
                fastest_delivery_interval=1,
                market_blue_prime_without_delivery=0,
                market_promo_datetime=str(get_custom_date_time(datetime(2021, 3, 4, 12, 0))),
            )
        )

        self.assertFragmentIn(
            response,
            {
                "id": "delivery-interval",
                "name": "Срок доставки",
                "filterOrigin": _FilterOrigin.NON_GL_FILTER,
                "values": [
                    {"value": "12", "found": 1},
                    {"value": "0", "found": 1},
                    {"value": "1", "found": 1},
                ],
            },
        )

    def test_delivery_interval_stats_for_12d(self):
        '''
        Проверяем, что оффер с доставкой 12 дней больше не посчитается в Found пункта 1-2ч фильтра Срок доставки
        https://st.yandex-team.ru/MARKETOUT-47503
        '''

        response = self.report.request_json(
            _Requests.prime_request_msku.format(rids=_Constants.moscow_rids, msku=str(_BlueOffers.dropship_msku_4.sku))
            + "&rearr-factors="
            + make_rearr(
                enable_dsbs_filter_by_delivery_interval=2,
                fastest_delivery_interval=1,
                market_promo_datetime=str(get_custom_date_time(datetime(2021, 3, 4, 12, 0))),
                get_rid_of_direct_shipping=0,
            )
        )

        self.assertFragmentNotIn(
            response,
            {
                "id": "delivery-interval",
                "name": "Срок доставки",
                "filterOrigin": _FilterOrigin.NON_GL_FILTER,
                "values": [
                    {"value": "12", "found": 1},
                ],
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                "id": "fastest-delivery-12",
                "filterOrigin": _FilterOrigin.USER_FILTER,
            },
        )


if __name__ == '__main__':
    main()
