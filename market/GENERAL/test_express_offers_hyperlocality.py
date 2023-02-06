#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from datetime import datetime, time, timedelta

from core.logs import ErrorCodes
from itertools import product

import calendar
from core.combinator import DeliveryStats, make_offer_id
from core.matcher import Regex, ListMatcher, Not
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
    ExpressDeliveryService,
    ExpressSupplier,
    MarketSku,
    Offer,
    Region,
    RegionalDelivery,
    Shop,
    Tax,
    TimeInfo,
    TimeIntervalByDay,
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
)
from core.report import REQUEST_TIMESTAMP
from market.combinator.proto.grpc.combinator_pb2 import DeliveryService, Time

from core.types.delivery import BlueDeliveryTariff

from core.types.combinator import CombinatorGpsCoords, CombinatorExpressWarehouse


DATETIME_NOW = datetime.fromtimestamp(REQUEST_TIMESTAMP)


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


class _Gps:
    _lat = 1.0
    _lon = 1.0

    location_combinator = CombinatorGpsCoords(_lat, _lon)
    location_str = 'lat:{lat};lon:{lon}'.format(lat=_lat, lon=_lon)


class _Gps1:
    _lat = 2.0
    _lon = 2.0

    location_combinator = CombinatorGpsCoords(_lat, _lon)
    location_str = 'lat:{lat};lon:{lon}'.format(lat=_lat, lon=_lon)


class _Gps2:
    _lat = 3.0
    _lon = 3.0

    location_combinator = CombinatorGpsCoords(_lat, _lon)
    location_str = 'lat:{lat};lon:{lon}'.format(lat=_lat, lon=_lon)


class _GpsAll:
    _lat = 4.0
    _lon = 4.0

    location_combinator = CombinatorGpsCoords(_lat, _lon)
    location_str = 'lat:{lat};lon:{lon}'.format(lat=_lat, lon=_lon)


class _Constants:
    russia_rids = 225
    moscow_rids = 213
    ekb_rids = 54

    model_id = 1
    category_id = 1
    category_id_with_schedule = 2
    category_id_filter = 3

    virtual_blue_fesh = 1
    virtual_blue_feed_id = 1

    class _Partners:
        dropship_fesh = 2
        dropship_business_id = 2
        dropship_feed_id = 2
        dropship_warehouse_id = 9

        usual_white_fesh = 6

        delivery_service_id = 100

        courier_bucket_dc_id = 1000
        courier_bucket_id = 2000

        dc_day_from = 1
        dc_day_to = 1
        dc_delivery_cost = 50

        combinator_day_from = 1
        combinator_day_to = 1
        combinator_date_from = DATETIME_NOW + timedelta(days=combinator_day_from)
        combinator_date_to = DATETIME_NOW + timedelta(days=combinator_day_to)
        combinator_delivery_cost = 50

    class _ExpressPartners:
        dropship_fesh_with_schedule = 3
        dropship_feed_id_with_schedule = 3
        dropship_warehouse_id_with_schedule = 10

        dropship_fesh = 4
        dropship_business_id = 100400
        dropship_feed_id = 4
        dropship_warehouse_id = 11

        dropship_fesh_1 = 5
        dropship_feed_id_1 = 5
        dropship_warehouse_id_1 = 12

        dropship_fesh_2 = 6
        dropship_feed_id_2 = 6
        dropship_warehouse_id_2 = 13

        delivery_service_id = 101

        courier_bucket_dc_id = 1001
        courier_bucket_id = 2001

        dc_day_from = 0
        dc_day_to = 0
        dc_delivery_cost = 50

        combinator_day_from = 1
        combinator_day_to = 1
        combinator_date_from = DATETIME_NOW + timedelta(days=combinator_day_from)
        combinator_date_to = DATETIME_NOW + timedelta(days=combinator_day_to)
        combinator_delivery_cost = 50


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

    dropship_shop = Shop(
        fesh=_Constants._Partners.dropship_fesh,
        business_fesh=_Constants._Partners.dropship_business_id,
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
        business_fesh=_Constants._ExpressPartners.dropship_business_id,
        datafeed_id=_Constants._ExpressPartners.dropship_feed_id,
        warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id,
        priority_region=_Constants.moscow_rids,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        fulfillment_program=False,
    )

    """
    копии для тестирования многоскладовости
    """
    express_dropship_shop_1 = Shop(
        fesh=_Constants._ExpressPartners.dropship_fesh,  # умышленно делаем магазин с 2мя складами
        business_fesh=_Constants._ExpressPartners.dropship_business_id,
        datafeed_id=_Constants._ExpressPartners.dropship_feed_id_1,
        warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id_1,
        priority_region=_Constants.moscow_rids,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        fulfillment_program=False,
    )

    express_dropship_shop_2 = Shop(
        fesh=_Constants._ExpressPartners.dropship_fesh_2,
        datafeed_id=_Constants._ExpressPartners.dropship_feed_id_2,
        warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id_2,
        priority_region=_Constants.moscow_rids,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        fulfillment_program=False,
    )

    express_dropship_shop_with_schedule = Shop(
        fesh=_Constants._ExpressPartners.dropship_fesh_with_schedule,
        datafeed_id=_Constants._ExpressPartners.dropship_feed_id_with_schedule,
        warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id_with_schedule,
        priority_region=_Constants.moscow_rids,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        fulfillment_program=False,
    )

    usual_white_shop = Shop(
        fesh=_Constants._Partners.usual_white_fesh,
        priority_region=_Constants.moscow_rids,
        regions=[_Constants.moscow_rids],
        cpa=Shop.CPA_NO,
    )


class _BlueOffers:
    """
    копии для тестирования многоскладовости
    """

    express_dropship_offer = BlueOffer(
        offerid='express_dropship_sku1',
        price=30,
        feedid=_Constants._ExpressPartners.dropship_feed_id,
        weight=5,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._ExpressPartners.dropship_fesh,
        delivery_buckets=[_Constants._ExpressPartners.courier_bucket_id],
        waremd5='original_____________g',
        is_express=True,
    )

    express_dropship_offer_1 = BlueOffer(
        offerid='express_dropship_sku1',
        price=30,
        feedid=_Constants._ExpressPartners.dropship_feed_id_1,
        weight=5,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._ExpressPartners.dropship_fesh,  # умышленно делаем магазин с 2мя складами
        delivery_buckets=[_Constants._ExpressPartners.courier_bucket_id],
        waremd5='original_1___________g',
        is_express=True,
    )

    express_dropship_offer_2 = BlueOffer(
        offerid='express_dropship_sku1',
        price=30,
        feedid=_Constants._ExpressPartners.dropship_feed_id_2,
        weight=5,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._ExpressPartners.dropship_fesh_2,
        delivery_buckets=[_Constants._ExpressPartners.courier_bucket_id],
        waremd5='original_2___________g',
        is_express=True,
        title='duplicate',
    )

    dropship_offer = BlueOffer(
        offerid='dropship_sku1',
        waremd5='DropshipWaremd5______w',
        price=30,
        feedid=_Constants._Partners.dropship_feed_id,
        weight=5,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._Partners.dropship_fesh,
        delivery_buckets=[_Constants._Partners.courier_bucket_id],
        title='duplicate',
        is_express=False,
    )

    express_dropship_offer_with_schedule = BlueOffer(
        offerid='express_dropship_with_schedule_sku1',
        waremd5='ExpressDroSchedWaremdw',
        price=30,
        feedid=_Constants._ExpressPartners.dropship_feed_id_with_schedule,
        weight=5,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._ExpressPartners.dropship_fesh_with_schedule,
        delivery_buckets=[_Constants._ExpressPartners.courier_bucket_id],
        is_express=True,
        title='express_dropship_with_schedule',
    )

    express_dropship_msku = MarketSku(
        title="duplicate", hyperid=_Constants.category_id, sku=1, blue_offers=[express_dropship_offer]
    )

    express_dropship_1_msku = MarketSku(
        title="duplicate", hyperid=_Constants.category_id, sku=4, blue_offers=[express_dropship_offer_1]
    )

    express_dropship_2_msku = MarketSku(
        title="duplicate", hyperid=_Constants.category_id, sku=5, blue_offers=[express_dropship_offer_2]
    )

    dropship_msku = MarketSku(title="duplicate", hyperid=_Constants.category_id, sku=6, blue_offers=[dropship_offer])

    express_dropship_msku_with_schedule = MarketSku(
        title="duplicate",
        hyperid=_Constants.category_id_with_schedule,
        sku=2,
        blue_offers=[express_dropship_offer_with_schedule],
    )


class _WhiteOffers:
    usual_white_offer = Offer(
        hyperid=_Constants.category_id,
        fesh=_Constants._Partners.usual_white_fesh,
    )

    usual_white_offer_filter = Offer(
        hyperid=_Constants.category_id_filter,
        fesh=_Constants._Partners.usual_white_fesh,
    )


class _CombinatorOffers:
    dropship_offer = CombinatorOffer(
        shop_sku=_BlueOffers.dropship_offer.offerid,
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

    express_dropship_offer_1 = CombinatorOffer(
        shop_sku=_BlueOffers.express_dropship_offer_1.offerid,
        shop_id=_Constants._ExpressPartners.dropship_fesh,  # умышленно делаем магазин с 2мя складами
        partner_id=_Constants._ExpressPartners.dropship_warehouse_id_1,
        available_count=1,
    )

    express_dropship_offer_2 = CombinatorOffer(
        shop_sku=_BlueOffers.express_dropship_offer_2.offerid,
        shop_id=_Constants._ExpressPartners.dropship_fesh_2,
        partner_id=_Constants._ExpressPartners.dropship_warehouse_id_2,
        available_count=1,
    )


class _Points:
    def service_time(hour, minute, day=0):
        return datetime(year=2020, month=8, day=2 + day, hour=hour, minute=minute)

    end_point_msk_region = RoutePoint(
        point_ids=Destination(region_id=_Constants.moscow_rids),
        segment_id=512015,
        segment_type="handing",
        services=(
            (
                DeliveryService.OUTBOUND,
                "HANDING",
                service_time(14, 30),
                timedelta(minutes=15),
                (Time(hour=15, minute=00), Time(hour=16, minute=00)),
            ),
        ),
    )

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
    Набор тестов для проекта гиперлокальности
    """

    @classmethod
    def prepare_flags(cls):
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [Region(rid=_Constants.moscow_rids, name="Москва", tz_offset=10800)]

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [
            _Shops.virtual_blue_shop,
            _Shops.dropship_shop,
            _Shops.express_dropship_shop,
            _Shops.express_dropship_shop_1,
            _Shops.express_dropship_shop_2,
            _Shops.express_dropship_shop_with_schedule,
            _Shops.usual_white_shop,
        ]

    @classmethod
    def prepare_express(cls):
        # Оказывается, что бизнес_ид от комбинатора может отличаться от бизнес ид в репорте.
        # Специально портим этот идентификатор, чтобы проверить, что это не влияет на выдачу
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=_Gps.location_combinator,
            rear_factors=make_mock_rearr(),
        ).respond_with_express_warehouses(
            [
                CombinatorExpressWarehouse(
                    warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id,
                    zone_id=1,
                    business_id=_Constants._ExpressPartners.dropship_business_id + 100500,
                ),
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=_Gps1.location_combinator,
            rear_factors=make_mock_rearr(),
        ).respond_with_express_warehouses(
            [
                CombinatorExpressWarehouse(
                    warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id_1,
                    zone_id=2,
                ),
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=_Gps1.location_combinator,
            rear_factors=make_mock_rearr(
                express_offers_hyperlocality=1,
                market_metadoc_search="no",
            ),
        ).respond_with_express_warehouses(
            [
                CombinatorExpressWarehouse(
                    warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id_1,
                    zone_id=2,
                ),
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=_Gps1.location_combinator,
            rear_factors=make_mock_rearr(
                express_offers_hyperlocality=1,
                market_metadoc_search="offers",
            ),
        ).respond_with_express_warehouses(
            [
                CombinatorExpressWarehouse(
                    warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id_1,
                    zone_id=2,
                ),
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=_Gps1.location_combinator,
            rear_factors=make_mock_rearr(
                express_offers_hyperlocality=1,
            ),
        ).respond_with_express_warehouses(
            [
                CombinatorExpressWarehouse(
                    warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id_1,
                    zone_id=2,
                ),
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=_Gps2.location_combinator,
            rear_factors=make_mock_rearr(),
        ).respond_with_express_warehouses(
            [
                CombinatorExpressWarehouse(
                    warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id_2,
                    zone_id=3,
                ),
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=_GpsAll.location_combinator,
            rear_factors=make_mock_rearr(),
        ).respond_with_express_warehouses(
            [
                CombinatorExpressWarehouse(
                    warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id,
                    zone_id=1,
                    business_id=_Constants._ExpressPartners.dropship_business_id,
                ),
                CombinatorExpressWarehouse(
                    warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id_1,
                    zone_id=1,
                ),
                CombinatorExpressWarehouse(
                    warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id_2,
                    zone_id=1,
                ),
            ]
        )

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
                warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id_with_schedule,
                working_schedule=[TimeIntervalByDay(day=i, time_from='10:00', time_to='18:00') for i in range(0, 5)],
            )
        ]
        cls.index.express_partners.suppliers += [
            ExpressSupplier(
                feed_id=_Constants._ExpressPartners.dropship_feed_id_1,
                supplier_id=_Constants._ExpressPartners.dropship_fesh,
                warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id_1,
            )
        ]
        cls.index.express_partners.suppliers += [
            ExpressSupplier(
                feed_id=_Constants._ExpressPartners.dropship_feed_id_2,
                supplier_id=_Constants._ExpressPartners.dropship_fesh_2,
                warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id_2,
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
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=_Constants._Partners.courier_bucket_id,
                dc_bucket_id=_Constants._Partners.courier_bucket_dc_id,
                fesh=_Constants._Partners.dropship_fesh,
                carriers=[_Constants._Partners.delivery_service_id],
                regional_options=[
                    RegionalDelivery(
                        rid=_Constants.moscow_rids,
                        options=[
                            DeliveryOption(
                                price=_Constants._Partners.dc_delivery_cost,
                                day_from=_Constants._Partners.dc_day_from,
                                day_to=_Constants._Partners.dc_day_to,
                            )
                        ],
                        payment_methods=[Payment.PT_YANDEX, Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
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
                    RegionalDelivery(
                        rid=_Constants.ekb_rids,
                        options=[
                            DeliveryOption(
                                price=_Constants._ExpressPartners.dc_delivery_cost,
                                day_from=_Constants._ExpressPartners.dc_day_from + 1,
                                day_to=_Constants._ExpressPartners.dc_day_to + 1,
                            )
                        ],
                        payment_methods=[Payment.PT_YANDEX, Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
                    ),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.delivery_calc.on_request_offer_buckets(
            weight=10, height=64, length=32, width=32, warehouse_id=_Constants._Partners.dropship_warehouse_id
        ).respond([_Constants._ExpressPartners.courier_bucket_dc_id, _Constants._Partners.courier_bucket_dc_id], [], [])
        cls.delivery_calc.on_request_offer_buckets(
            weight=10, height=64, length=32, width=32, warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id
        ).respond([_Constants._ExpressPartners.courier_bucket_dc_id, _Constants._Partners.courier_bucket_dc_id], [], [])
        cls.delivery_calc.on_request_offer_buckets(
            weight=10, height=64, length=32, width=32, warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id_1
        ).respond([_Constants._ExpressPartners.courier_bucket_dc_id, _Constants._Partners.courier_bucket_dc_id], [], [])
        cls.delivery_calc.on_request_offer_buckets(
            weight=10, height=64, length=32, width=32, warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id_2
        ).respond([_Constants._ExpressPartners.courier_bucket_dc_id, _Constants._Partners.courier_bucket_dc_id], [], [])
        cls.delivery_calc.on_request_offer_buckets(
            weight=5, height=30, length=30, width=30, warehouse_id=_Constants._Partners.dropship_warehouse_id
        ).respond([_Constants._ExpressPartners.courier_bucket_dc_id, _Constants._Partners.courier_bucket_dc_id], [], [])
        cls.delivery_calc.on_request_offer_buckets(
            weight=5, height=30, length=30, width=30, warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id
        ).respond([_Constants._ExpressPartners.courier_bucket_dc_id, _Constants._Partners.courier_bucket_dc_id], [], [])
        cls.delivery_calc.on_request_offer_buckets(
            weight=5,
            height=30,
            length=30,
            width=30,
            warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id_with_schedule,
        ).respond([_Constants._ExpressPartners.courier_bucket_dc_id, _Constants._Partners.courier_bucket_dc_id], [], [])
        cls.delivery_calc.on_request_offer_buckets(
            weight=5, height=30, length=30, width=30, warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id_1
        ).respond([_Constants._ExpressPartners.courier_bucket_dc_id, _Constants._Partners.courier_bucket_dc_id], [], [])
        cls.delivery_calc.on_request_offer_buckets(
            weight=5, height=30, length=30, width=30, warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id_2
        ).respond([_Constants._ExpressPartners.courier_bucket_dc_id, _Constants._Partners.courier_bucket_dc_id], [], [])

    @classmethod
    def prepare_warehouses(cls):
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[_Constants.russia_rids, _Constants.moscow_rids],
                warehouse_with_priority=[
                    WarehouseWithPriority(warehouse_id=_Constants._Partners.dropship_warehouse_id, priority=3),
                    WarehouseWithPriority(warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id_1, priority=2),
                    WarehouseWithPriority(warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id_2, priority=1),
                ],
            )
        ]

    @classmethod
    def prepare_lms(cls):
        cls.settings.lms_autogenerate = False

        cls.dynamic.lms += [DynamicDaysSet(key=1, days=[])]

        for ds_id, ds_name in [
            (_Constants._Partners.delivery_service_id, 'ordinary_delivery_serivce'),
            (_Constants._ExpressPartners.delivery_service_id, 'express_delivery_service'),
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
            ]

        for is_express_wh, wh_id, ds_id in [
            (False, _Constants._Partners.dropship_warehouse_id, _Constants._Partners.delivery_service_id),
            (True, _Constants._ExpressPartners.dropship_warehouse_id, _Constants._ExpressPartners.delivery_service_id),
            (
                True,
                _Constants._ExpressPartners.dropship_warehouse_id_with_schedule,
                _Constants._ExpressPartners.delivery_service_id,
            ),
            (
                True,
                _Constants._ExpressPartners.dropship_warehouse_id_1,
                _Constants._ExpressPartners.delivery_service_id,
            ),
            (
                True,
                _Constants._ExpressPartners.dropship_warehouse_id_2,
                _Constants._ExpressPartners.delivery_service_id,
            ),
        ]:
            cls.dynamic.lms += [
                DynamicWarehouseInfo(
                    id=wh_id,
                    home_region=_Constants.moscow_rids,
                    is_express=is_express_wh,
                ),
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
                day_from=_Constants._Partners.combinator_day_from,
                day_to=_Constants._Partners.combinator_day_to,
            ),
        )

        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_BlueOffers.express_dropship_offer, _Shops.express_dropship_shop),
            courier_stats=DeliveryStats(
                cost=_Constants._ExpressPartners.combinator_delivery_cost,
                day_from=_Constants._ExpressPartners.combinator_day_from,
                day_to=_Constants._ExpressPartners.combinator_day_to,
            ),
        )

        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_BlueOffers.express_dropship_offer_1, _Shops.express_dropship_shop_1),
            courier_stats=DeliveryStats(
                cost=_Constants._ExpressPartners.combinator_delivery_cost,
                day_from=_Constants._ExpressPartners.combinator_day_from,
                day_to=_Constants._ExpressPartners.combinator_day_to,
            ),
        )

        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_BlueOffers.express_dropship_offer_2, _Shops.express_dropship_shop_2),
            courier_stats=DeliveryStats(
                cost=_Constants._ExpressPartners.combinator_delivery_cost,
                day_from=_Constants._ExpressPartners.combinator_day_from,
                day_to=_Constants._ExpressPartners.combinator_day_to,
            ),
        )

        cls.combinator.on_courier_options_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=5000,
                    dimensions=[30, 30, 30],
                    cargo_types=[],
                    offers=[
                        _CombinatorOffers.dropship_offer,
                        _CombinatorOffers.express_dropship_offer,
                        _CombinatorOffers.express_dropship_offer_1,
                        _CombinatorOffers.express_dropship_offer_2,
                    ],
                    price=_BlueOffers.dropship_offer.price,
                )
            ],
            destination=Destination(region_id=_Constants.moscow_rids),
            payment_methods=[],
            total_price=50,
        ).respond_with_courier_options(
            options=[
                create_delivery_option(
                    cost=_Constants._Partners.combinator_delivery_cost,
                    date_from=_Constants._Partners.combinator_date_from,
                    date_to=_Constants._Partners.combinator_date_to,
                    time_from=time(10, 0),
                    time_to=time(18, 0),
                    delivery_service_id=_Constants._Partners.delivery_service_id,
                )
            ]
        )

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.mskus += [
            _BlueOffers.dropship_msku,
            _BlueOffers.express_dropship_msku,
            _BlueOffers.express_dropship_1_msku,
            _BlueOffers.express_dropship_2_msku,
            _BlueOffers.express_dropship_msku_with_schedule,
        ]

        cls.index.offers += [
            _WhiteOffers.usual_white_offer,
            _WhiteOffers.usual_white_offer_filter,
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

        DEFAULT_FAST_TARIFFS = [BlueDeliveryTariff(user_price=350)]

        DEFAULT_TARIFFS = [BlueDeliveryTariff(user_price=77)]

        # Тариф быстрых оферов в конкретный город
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=EKB_FAST_TARIFFS, regions=[_Constants.ekb_rids], fast_delivery=True
        )

        # Тариф быстрых оферов везде
        cls.index.blue_delivery_modifiers.set_default_modifier(tariffs=DEFAULT_FAST_TARIFFS, fast_delivery=True)

        # Тариф обычных оферов везде
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=DEFAULT_TARIFFS,
        )

    def date_time_to_string(sekf, dt, tm):
        return '{year}{month}{day}.{hour}{minute}'.format(
            year=dt.year, month=dt.month, day=dt.day, hour=tm.hour, minute=tm.minute
        )

    def delivery_interval_string(self, date_from, time_from, date_to, time_to):
        return 'delivery-interval={}-{}'.format(
            self.date_time_to_string(date_from, time_from), self.date_time_to_string(date_to, time_to)
        )

    def test_offerinfo_non_recurring_hypelocality_offer(self):
        offer_ids = 'DropshipWaremd5______w,original_____________g,original_1___________g,original_2___________g'
        request = 'place=offerinfo&express-warehouses=11&offerid={}&rids=213&regset=1&rearr-factors=express_offers_hyperlocality=1'.format(
            offer_ids
        )
        requestCombinatorWarehouses = 'place=offerinfo&gps={gps}&offerid={offerids}&rids={region}&regset=1'.format(
            gps=_Gps.location_str,
            offerids=','.join(
                ['DropshipWaremd5______w', 'original_____________g', 'original_1___________g', 'original_2___________g']
            ),
            region=213,
        )
        for req in [request, requestCombinatorWarehouses]:
            response = self.report.request_json(req)
            self.error_log.ignore(
                code=ErrorCodes.BASE_PROPS_FILE_INVALID
            )  # TODO: https://st.yandex-team.ru/MARKETOUT-39026

            responseJsonWhith = {
                "search": {
                    "total": 2,
                    "results": [
                        {
                            "entity": "offer",
                            "wareId": "original_____________g",
                            "delivery": {
                                "hasPickup": False,
                            },
                        },
                        {
                            "entity": "offer",
                            "wareId": "DropshipWaremd5______w",
                            "delivery": {
                                "hasPickup": False,
                            },
                        },
                    ],
                }
            }

            responseJsonWhithOut = {"entity": "offer", "wareId": "original_1___________g"}

            self.assertFragmentNotIn(response, responseJsonWhithOut)

            responseJsonWhithOut = {"entity": "offer", "wareId": "original_2___________g"}

            self.assertFragmentNotIn(response, responseJsonWhithOut)

            self.assertFragmentIn(response, responseJsonWhith, allow_different_len=False)

    def test_prime_non_recurring_hypelocality_offer(self):
        warehouses = [
            '&express-warehouses=12',
            '&gps=' + _Gps1.location_str,
        ]

        rearrs = [
            # С фильтрацией на базовых работает и обычный поиск и метапоиск
            make_rearr(express_offers_hyperlocality=1, market_metadoc_search="no"),
            make_rearr(express_offers_hyperlocality=1, market_metadoc_search="offers"),
            make_rearr(express_offers_hyperlocality=1),
        ]

        request = "place=prime" "&rids=213" "{warehouses}" "&text=duplicate" "&regset=2" "&rearr-factors={rearr}"

        for wh, r in product(warehouses, rearrs):
            response = self.report.request_json(request.format(warehouses=wh, rearr=r))

            responseJsonWhith = {"entity": "offer", "wareId": "original_1___________g"}
            self.assertFragmentIn(response, responseJsonWhith, allow_different_len=False)

            responseJsonWhithOut = {"entity": "offer", "wareId": "original_____________g"}
            self.assertFragmentNotIn(response, responseJsonWhithOut)

            responseJsonWhithOut = {"entity": "offer", "wareId": "original_2___________g"}
            self.assertFragmentNotIn(response, responseJsonWhithOut)

    def test_prime_subrequests(self):
        request = (
            'place=prime&rids=213&express-warehouses=12&text=duplicate&regset=1&rearr-factors=express_offers_hyperlocality=1'
            '&cart=original_____________g&debug=1&rgb=blue'
        )

        business_offer_rearr = '&rearr-factors=market_use_business_offer={}'

        # В режиме мультисклада или в режиме фильтрации на базовых, ограничение через список складов
        regex = Regex("hyperlocal_warehouses_context_mms")

        how_shop = {
            'how': [
                {
                    'collections': ListMatcher(
                        expected=['SHOP', 'SHOP_UPDATE', 'SHOP_FRESH'],
                        unexpected=['MODEL', 'BOOK', 'CARD', 'CATALOG', 'PREVIEW_MODEL'],
                    ),
                    'args': regex,
                }
            ]
        }

        # На модельные индексы склада не отправляются
        how_model = {
            'how': [
                {
                    'collections': ListMatcher(expected=['MODEL']),
                    'args': Not(regex),
                }
            ]
        }
        how_book = {
            'how': [
                {
                    'collections': ListMatcher(expected=['BOOK', 'PREVIEW_MODEL']),
                    'args': Not(regex),
                }
            ]
        }

        for flag in [business_offer_rearr.format(2), '']:
            response = self.report.request_json(request + flag)
            self.assertFragmentIn(
                response,
                {
                    'debug': {
                        'report': how_shop,
                        'metasearch': {
                            'subrequests': [
                                "debug",
                                {'report': how_shop},
                            ],
                        },
                    },
                },
            )
            self.assertFragmentIn(
                response,
                {
                    'report': how_model,
                },
            )
            self.assertFragmentIn(
                response,
                {
                    'report': how_book,
                },
            )

    def test_sku_offers_non_recurring_hypelocality_offer(self):
        request = 'place=sku_offers&market-sku=5&rids=213&express-warehouses=13&regset=1&rearr-factors=express_offers_hyperlocality=1'
        requestCombinatorWarehouses = (
            'place=sku_offers&market-sku={msku}&rids={region}&gps={gps}&regset=1&rearr-factors={rearr}'.format(
                region=213,
                msku=5,
                gps=_Gps2.location_str,
                rearr='express_offers_hyperlocality=1',
            )
        )
        for req in [request, requestCombinatorWarehouses]:
            response = self.report.request_json(req)
            responseJsonWhith = {"entity": "offer", "wareId": "original_2___________g"}

            self.assertFragmentIn(response, responseJsonWhith, allow_different_len=False)

            responseJsonWhithOut = {"entity": "offer", "wareId": "original_____________g"}

            self.assertFragmentNotIn(response, responseJsonWhithOut)

            responseJsonWhithOut = {"entity": "offer", "wareId": "original_1___________g"}

            self.assertFragmentNotIn(response, responseJsonWhithOut)

    def test_with_express_user_filter(self):
        """
        Тестируем комбинации пользовательского фильтра по экспрессу и режимов фильтрации по гиперлокальности
        И то и другое работает по поисковому литералу is_express
        """

        REQUEST_TEMPLATE = (
            'place=prime&regset=1&rids=213&allow-collapsing=0&text=duplicate'
            '&rearr-factors=express_offers_hyperlocality={hyperlocality_flag}'
            '&rearr-factors=market_metadoc_search=no'
            '&rearr-factors=show_old_filter_express_delivery=1'
            '&express-warehouses={hyperlocality_filter}'
            '{user_filter}'
        )

        USER_FILTER_TEMPLATE = '&filter-express-delivery={user_filter}'

        NONE = []  # no offers in response
        NON_EXPRESS = ['DropshipWaremd5______w']  # non-express offer only
        ALL_EXPRESS = [
            'original_____________g',
            'original_1___________g',
            'original_2___________g',
        ]  # all express offers
        EXPRESS_WAREHOUSE_12 = ['original_1___________g']  # express offer for warehouse_id == 12 only

        TEST_CASES = [
            # hyperlocality_flag, hyperlocality_filter, user_filter, expected_offers #
            (0, None, None, ALL_EXPRESS + NON_EXPRESS),
            (0, None, 1, ALL_EXPRESS),
            (1, None, None, ALL_EXPRESS + NON_EXPRESS),
            (1, None, 1, ALL_EXPRESS),
            (2, None, None, NON_EXPRESS),
            (2, None, 1, NONE),  # error expected
            (1, 0, None, NON_EXPRESS),
            (1, 0, 1, NONE),  # error expected
            (1, 12, None, EXPRESS_WAREHOUSE_12 + NON_EXPRESS),
            (1, 12, 1, EXPRESS_WAREHOUSE_12),
        ]

        for (
            hyperlocality_flag,
            hyperlocality_filter,
            user_filter,
            expected_offers,
        ) in TEST_CASES:
            request = REQUEST_TEMPLATE.format(
                hyperlocality_flag=hyperlocality_flag,
                hyperlocality_filter=hyperlocality_filter if hyperlocality_filter is not None else "",
                user_filter=USER_FILTER_TEMPLATE.format(
                    user_filter=user_filter,
                )
                if user_filter is not None
                else "",
            )
            response = self.report.request_json(request)
            self.error_log.ignore(
                code=ErrorCodes.BASE_PROPS_FILE_INVALID
            )  # TODO: https://st.yandex-team.ru/MARKETOUT-39026

            if len(expected_offers) == 0:
                self.error_log.ignore(code=ErrorCodes.UNEXPECTED_EXPRESS_FILTER_WITH_NO_HYPERLOCALITY)

            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {
                                'entity': "offer",
                                'wareId': expected_offer,
                            }
                            for expected_offer in expected_offers
                        ]
                    },
                },
                allow_different_len=False,
            )

    def test_combine_offers_non_recurring_hypelocality_offer(self):
        offers = (
            (
                _BlueOffers.express_dropship_offer_with_schedule.waremd5,
                _BlueOffers.express_dropship_msku_with_schedule.sku,
            ),
            (_BlueOffers.dropship_offer.waremd5, _BlueOffers.dropship_msku.sku),
            (_BlueOffers.express_dropship_offer.waremd5, _BlueOffers.express_dropship_msku.sku),
        )
        request = 'place=combine&rids=213&express-warehouses=10&regset=1&rearr-factors=express_offers_hyperlocality=1&offers-list={}'.format(
            ','.join('{}:1;msku:{}'.format(offer_id, msku_id) for offer_id, msku_id in offers),
        )
        response = self.report.request_json(request)

        # проверяем, что в выдаче есть список офферов
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'offers': {
                        'items': [
                            {
                                'entity': 'offer',
                                'wareId': _BlueOffers.express_dropship_offer_with_schedule.waremd5,
                            },
                            {
                                'entity': 'offer',
                                'wareId': _BlueOffers.dropship_offer.waremd5,  # так как офер не эксперсс, на него фильтр не влияет
                            },
                        ],
                    },
                },
            },
            allow_different_len=False,
        )

        self.assertFragmentNotIn(
            response,
            {
                'search': {
                    'offers': {
                        'items': [
                            {
                                'entity': 'offer',
                                'wareId': _BlueOffers.express_dropship_offer.waremd5,
                            }
                        ],
                    },
                },
            },
        )

    def test_combine_offers_with_multi_warehouses(self):
        offers = (
            (
                _BlueOffers.express_dropship_offer_with_schedule.waremd5,
                _BlueOffers.express_dropship_msku_with_schedule.sku,
            ),
            (_BlueOffers.dropship_offer.waremd5, _BlueOffers.dropship_msku.sku),
            (_BlueOffers.express_dropship_offer.waremd5, _BlueOffers.express_dropship_msku.sku),
        )
        request = 'place=combine&rids=213&express-warehouses=10,11&regset=1&rearr-factors=express_offers_hyperlocality=1&offers-list={}'.format(
            ','.join('{}:1;msku:{}'.format(offer_id, msku_id) for offer_id, msku_id in offers),
        )
        response = self.report.request_json(request)

        # проверяем, что в выдаче есть список офферов
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'offers': {
                        'items': [
                            {
                                'entity': 'offer',
                                'wareId': _BlueOffers.express_dropship_offer_with_schedule.waremd5,
                            },
                            {
                                'entity': 'offer',
                                'wareId': _BlueOffers.express_dropship_offer.waremd5,
                            },
                            {
                                'entity': 'offer',
                                'wareId': _BlueOffers.dropship_offer.waremd5,  # так как офер не эксперсс, на него фильтр не влияет
                            },
                        ],
                    },
                },
            },
            allow_different_len=False,
        )

    def test_product_offers_non_recurring_hypelocality_offer(self):
        request = 'place=productoffers&express-warehouses=11,10&rids=213&hyperid=1,2&rearr-factors=express_offers_hyperlocality=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': _BlueOffers.express_dropship_offer.waremd5,
                        },
                        {
                            'entity': 'offer',
                            'wareId': _BlueOffers.dropship_offer.waremd5,
                        },
                        {
                            'entity': 'offer',
                            'wareId': _BlueOffers.express_dropship_offer_with_schedule.waremd5,
                        },
                        {
                            'entity': 'offer',
                            'wareId': _WhiteOffers.usual_white_offer.waremd5,
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_product_offers_blue_only(self):
        request = 'place=productoffers&rgb=blue&express-warehouses=11,10&rids=213&hyperid=1,2&rearr-factors=express_offers_hyperlocality=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': _BlueOffers.express_dropship_offer.waremd5,
                        },
                        {
                            'entity': 'offer',
                            'wareId': _BlueOffers.dropship_offer.waremd5,
                        },
                        {
                            'entity': 'offer',
                            'wareId': _BlueOffers.express_dropship_offer_with_schedule.waremd5,
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_empty_request(self):
        request = "place=prime&rids=213&rearr-factors=express_offers_hyperlocality=1;market_metadoc_search=no&express-warehouses="
        for warehouse in ['', '0']:
            response = self.report.request_json(request + warehouse, strict=False)
            self.assertEqual(response.code, 400)
            self.assertFragmentIn(
                response,
                {
                    'error': {'code': 'EMPTY_REQUEST'},
                },
            )

    def test_warehouse_list_filtered_by_shop(self):
        '''
        Проверяем, что список складов ограничивается списком магазинов
        '''
        request = (
            'place=productoffers&hyperid={model_id}&rids=213&fesh={shop_id}&gps={gps}&debug=1&supplier-id={supplier_id}'
        )

        def __check(incoming, filtered, shops, suppliers, gps):
            response = self.report.request_json(
                request.format(
                    model_id=_Constants.category_id,
                    shop_id=','.join([str(shop) for shop in shops]),
                    supplier_id=','.join([str(supplier) for supplier in suppliers]),
                    gps=gps,
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'logicTrace': [
                        Regex(
                            'Hyperlocal warehouses count: incoming: {}, after filtering: {}'.format(incoming, filtered)
                        ),
                    ]
                },
            )

        for incoming, filtered, shops, gps in [
            # Запросили оферы одного магазина, а склады другого
            (
                1,
                0,
                [_Constants._ExpressPartners.dropship_fesh_2],
                _Gps.location_str,
            ),
            # Указали бизнес и склад
            (
                1,
                1,
                [_Constants._ExpressPartners.dropship_business_id],
                _Gps.location_str,
            ),
            # Указали магазин и склад
            (
                1,
                1,
                [_Constants._ExpressPartners.dropship_fesh],
                _Gps.location_str,
            ),
            # Указали бизнес и другой магазин и склад
            (
                1,
                1,
                [_Constants._ExpressPartners.dropship_business_id, _Constants._ExpressPartners.dropship_fesh_2],
                _Gps.location_str,
            ),
            # Указали бизнес и несколько складов
            (3, 2, [_Constants._ExpressPartners.dropship_business_id], _GpsAll.location_str),
            # Указали бизнес и другой магазин и несколько складов
            (
                3,
                3,
                [_Constants._ExpressPartners.dropship_business_id, _Constants._ExpressPartners.dropship_fesh_2],
                _GpsAll.location_str,
            ),
            # Указали бизнес и не экспресс магазин
            (
                3,
                2,
                [_Constants._ExpressPartners.dropship_business_id, _Constants._Partners.dropship_fesh],
                _GpsAll.location_str,
            ),
        ]:
            # Фильтр по fesh
            __check(incoming, filtered, shops, [], gps)
            # Фильтр по supplier-id
            __check(incoming, filtered, [], shops, gps)

        # Есть исключение из магазинов. Фильтрации складов нет (даже если это не экспресс магазин)
        __check(
            3,
            3,
            [_Constants._ExpressPartners.dropship_business_id, -_Constants._Partners.dropship_fesh],
            [],
            _GpsAll.location_str,
        )
        # Отрицательных постащиков не может быть

    def test_express_warehouse_id_param(self):
        request = 'place=prime&rids=213&hyperid=1,2&gps={}'.format(_GpsAll.location_str)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': _WhiteOffers.usual_white_offer.waremd5,
                        },
                        {
                            'entity': 'offer',
                            'wareId': _BlueOffers.express_dropship_offer.waremd5,
                        },
                        {
                            'entity': 'offer',
                            'wareId': _BlueOffers.express_dropship_offer_1.waremd5,
                        },
                        {
                            'entity': 'offer',
                            'wareId': _BlueOffers.express_dropship_offer_2.waremd5,
                        },
                        {
                            'entity': 'offer',
                            'wareId': _BlueOffers.dropship_offer.waremd5,
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

        # После добавления express-warehouse-id=dropship_warehouse_id_2 на выдаче остаются все не эксперсс офферы из базового запроса и экспресс с указанного склада
        warehouseFilter = _Constants._ExpressPartners.dropship_warehouse_id_1
        request = 'place=prime&rids=213&hyperid=1,2&gps={}&express-warehouse-id={}'.format(
            _GpsAll.location_str, warehouseFilter
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': _WhiteOffers.usual_white_offer.waremd5,
                        },
                        {
                            'entity': 'offer',
                            'wareId': _BlueOffers.express_dropship_offer_1.waremd5,
                        },
                        {
                            'entity': 'offer',
                            'wareId': _BlueOffers.dropship_offer.waremd5,
                        },
                    ],
                },
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
