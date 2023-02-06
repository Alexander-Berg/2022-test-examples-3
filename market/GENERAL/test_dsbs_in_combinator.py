#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa
import itertools

from datetime import (
    datetime,
    time,
    timedelta,
    date,
)
from core.report import REQUEST_TIMESTAMP
from core.testcase import TestCase, main
from core.matcher import (
    Absent,
    EmptyList,
    EqualToOneOfOrAbsent,
    NotEmpty,
    Regex,
)
from core.types import (
    DeliveryBucket,
    DeliveryOption,
    MarketSku,
    Offer,
    OfferDimensions,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Shop,
)
from core.types.delivery import BlueDeliveryTariff, OutletType
from core.combinator import DeliveryStats, make_offer_id
from core.logs import ErrorCodes

from core.types.combinator import (
    CombinatorOffer,
    create_string_delivery_dates,
    create_delivery_option,
    create_virtual_box,
    DeliveryItem,
    DeliveryType,
    Destination,
    PickupPointGrouped,
    PointTimeInterval,
    RoutePath,
    RoutePoint,
)

from market.combinator.proto.grpc.combinator_pb2 import DeliveryService, Time

from market.pylibrary.const.payment_methods import (
    PaymentMethod,
)

# This date is fixed for all test
TODAY = datetime.fromtimestamp(REQUEST_TIMESTAMP)
DATE_FROM = TODAY + timedelta(days=2)
DATE_TO = TODAY + timedelta(days=4)

MODEL_ID = 1
CATEGORY_ID = 1
MODEL_ID2 = 2
CATEGORY_ID2 = 2
MODEL_ID_KGT = 3
CATEGORY_ID_KGT = 3
MODEL_ID_PICKUP = 4
CATEGORY_ID_PICKUP = 4
DSBS_PICKUP_BUCKET_ID = 1001
DSBS_OUTLET_ID = 217
DSBS_OUTLET_ID2 = 218
DSBS_OUTLET_ID_FROM_COMBI1 = 291901009
DSBS_OUTLET_ID_FROM_COMBI2 = 291901010
DSBS_FESH = 7
DSBS_WAREHOUSE = 75
DSBS_MSKU = 30
DSBS_MSKU2 = 40
DSBS_MSKU_KGT = 50
DSBS_MSKU_PICKUP_1 = 61
DSBS_MSKU_PICKUP_2 = 62

MSK_RIDS = 213


def make_rearr(**kwds):
    kvlist = ['{}={}'.format(key, kwds[key]) for key in kwds]
    kvlist.sort(key=lambda x: x[0])
    return ';'.join(kvlist)


class _Constants:
    DeliveryPrice = 99
    DeliveryServiceId = 157
    MockRouteCost = 50
    MockRouteCostForShop = 50

    class TestVirtualBox:
        Weight = 18000
        Length = 100
        Width = 80
        Height = 50


class _DsbsCombiSelfPickupAvailableFalse:
    offer_wmd5 = Offer.generate_waremd5('Offer-SelfPickup-Off')


class _DsbsCombiSelfPickupAvailableTrue:
    offer_wmd5 = Offer.generate_waremd5('Offer-SelfPickup-On')


class _DsbsFeedOption:
    local_delivery_option = DeliveryOption(price=100, day_from=0, day_to=3, order_before=24)
    local_pickup_option = DeliveryOption(price=350, day_from=1, day_to=7, order_before=10)


class _CombinatorResponseStats:
    courier_stats = DeliveryStats(cost=15, day_from=1, day_to=5)
    pickup_stats = DeliveryStats(cost=20, day_from=2, day_to=5)
    post_stats = DeliveryStats(cost=30, day_from=3, day_to=10)


class _Shops:
    dsbs_shop = Shop(
        fesh=DSBS_FESH,
        datafeed_id=DSBS_FESH,
        business_fesh=DSBS_FESH,
        name="Магазин DSBS",
        regions=[MSK_RIDS],
        cpa=Shop.CPA_REAL,
        priority_region=MSK_RIDS,
        warehouse_id=DSBS_WAREHOUSE,
    )


class _Offers:
    dsbs_offer = Offer(
        title="DSBS Offer",
        offerid="DsbsShopSku",
        hid=CATEGORY_ID,
        hyperid=MODEL_ID,
        shop_category_path='категория 1\\категория 2\\категория 3',
        shop_category_path_ids='1\\2\\3',
        price=500,
        dimensions=OfferDimensions(length=10, width=20, height=30),
        weight=5,
        feedid=DSBS_FESH,
        fesh=DSBS_FESH,
        business_id=DSBS_FESH,
        sku=DSBS_MSKU,
        cpa=Offer.CPA_REAL,
        waremd5='SkuDSBS-------------eg',
        post_term_delivery=True,
        delivery_options=[_DsbsFeedOption.local_delivery_option],
        pickup_option=_DsbsFeedOption.local_pickup_option,
        pickup_buckets=[DSBS_PICKUP_BUCKET_ID],
    )

    dsbs_offer_kgt = Offer(
        title="DSBS Offer KGT",
        offerid="DsbsShopSkuKGT",
        hid=CATEGORY_ID_KGT,
        hyperid=MODEL_ID_KGT,
        shop_category_path='категория 1\\категория 2\\категория 3',
        shop_category_path_ids='1\\2\\3',
        price=1000,
        dimensions=OfferDimensions(length=10, width=20, height=30),
        weight=40,
        feedid=DSBS_FESH,
        fesh=DSBS_FESH,
        business_id=DSBS_FESH,
        sku=DSBS_MSKU_KGT,
        cpa=Offer.CPA_REAL,
        waremd5='SkuDSBS-kgt---------eg',
        post_term_delivery=True,
        delivery_options=[_DsbsFeedOption.local_delivery_option],
        pickup_option=_DsbsFeedOption.local_pickup_option,
        pickup_buckets=[DSBS_PICKUP_BUCKET_ID],
    )

    dsbs_offer_without_dimensions = Offer(
        title="DSBS Offer without dimensions",
        offerid="DsbsShopSkuWithoutDimensions",
        hid=CATEGORY_ID2,
        hyperid=MODEL_ID2,
        price=120,
        feedid=DSBS_FESH,
        fesh=DSBS_FESH,
        business_id=DSBS_FESH,
        sku=DSBS_MSKU2,
        cpa=Offer.CPA_REAL,
        waremd5='DSBSwithout-dimens--eg',
        post_term_delivery=True,
        delivery_options=[_DsbsFeedOption.local_delivery_option],
        pickup_option=_DsbsFeedOption.local_pickup_option,
        pickup_buckets=[DSBS_PICKUP_BUCKET_ID],
    )

    dsbs_offer_selfpickup_off = Offer(
        title="DSBS Offer SelfPickup Off",
        offerid="DsbsOffer_SelfPickup_Off",
        waremd5=_DsbsCombiSelfPickupAvailableFalse.offer_wmd5,
        hyperid=MODEL_ID_PICKUP,
        sku=DSBS_MSKU_PICKUP_1,
        cpa=Offer.CPA_REAL,
        price=500,
        feedid=DSBS_FESH,
        fesh=DSBS_FESH,
        delivery_options=[_DsbsFeedOption.local_delivery_option],
        pickup_option=_DsbsFeedOption.local_pickup_option,
        pickup_buckets=[DSBS_PICKUP_BUCKET_ID],
    )

    dsbs_offer_selfpickup_on = Offer(
        title="DSBS Offer SelfPickup On",
        offerid="DsbsOffer_SelfPickup_On",
        waremd5=_DsbsCombiSelfPickupAvailableTrue.offer_wmd5,
        hyperid=MODEL_ID_PICKUP,
        sku=DSBS_MSKU_PICKUP_2,
        cpa=Offer.CPA_REAL,
        price=500,
        feedid=DSBS_FESH,
        fesh=DSBS_FESH,
        delivery_options=[_DsbsFeedOption.local_delivery_option],
        pickup_option=_DsbsFeedOption.local_pickup_option,
        pickup_buckets=[DSBS_PICKUP_BUCKET_ID],
    )


class _Skus:
    dsbs_sku = MarketSku(title="MSKU DSBS", hid=CATEGORY_ID, hyperid=MODEL_ID, sku=str(DSBS_MSKU))

    dsbs_sku_kgt = MarketSku(title="MSKU DSBS KGT", hid=CATEGORY_ID_KGT, hyperid=MODEL_ID_KGT, sku=str(DSBS_MSKU_KGT))

    dsbs_sku_without_dimensions = MarketSku(
        title="MSKU DSBS WITHOUT DIMENSIONS", hid=CATEGORY_ID2, hyperid=MODEL_ID2, sku=str(DSBS_MSKU2)
    )

    dsbs_sku_selfpickup_off = MarketSku(
        title="MSKU SelfPickupAvailable False",
        hid=CATEGORY_ID_PICKUP,
        hyperid=MODEL_ID_PICKUP,
        sku=DSBS_MSKU_PICKUP_1,
    )

    dsbs_sku_selfpickup_on = MarketSku(
        title="MSKU SelfPickupAvailable True",
        hid=CATEGORY_ID_PICKUP,
        hyperid=MODEL_ID_PICKUP,
        sku=DSBS_MSKU_PICKUP_2,
    )


class Dsbs_Payment(object):
    SCALE = 4

    tarrifs = [
        BlueDeliveryTariff(
            is_dsbs_payment=True,
            large_size=0,
            price_to=550,
            dsbs_payment=110,
            courier_price=111,
            pickup_price=112,
            post_price=113,
        ),
        BlueDeliveryTariff(
            is_dsbs_payment=True,
            large_size=0,
            dsbs_payment=115,
            courier_price=116,
            pickup_price=117,
            post_price=118,
        ),
        BlueDeliveryTariff(
            is_dsbs_payment=True,
            large_size=1,
            dsbs_payment=1010,
            courier_price=1011,
            pickup_price=1012,
            post_price=1013,
        ),
    ]

    tarrifs_default = [
        BlueDeliveryTariff(
            is_dsbs_payment=True,
            large_size=0,
            price_to=200,
            dsbs_payment=10 * SCALE,
            courier_price=11 * SCALE,
            pickup_price=12 * SCALE,
            post_price=13 * SCALE,
        ),
        BlueDeliveryTariff(
            is_dsbs_payment=True,
            large_size=0,
            dsbs_payment=110 * SCALE,
            courier_price=111 * SCALE,
            pickup_price=112 * SCALE,
            post_price=113 * SCALE,
        ),
        BlueDeliveryTariff(
            is_dsbs_payment=True,
            large_size=1,
            dsbs_payment=1010 * SCALE,
            courier_price=1011 * SCALE,
            pickup_price=1012 * SCALE,
            post_price=1013 * SCALE,
        ),
    ]


class _Outlets:
    dsbs_outlet = Outlet(
        point_id=DSBS_OUTLET_ID,
        fesh=DSBS_FESH,
        region=MSK_RIDS,
        point_type=Outlet.MIXED_TYPE,
        delivery_option=OutletDeliveryOption(
            day_from=3,
            day_to=5,
            order_before=16,
            price=500,
            price_to=1000,
            shipper_id=100,
        ),
        working_days=[i for i in range(10)],
    )

    dsbs_outlet_shipper_99 = Outlet(
        point_id=DSBS_OUTLET_ID2,
        fesh=DSBS_FESH,
        region=MSK_RIDS,
        point_type=Outlet.MIXED_TYPE,
        delivery_option=OutletDeliveryOption(
            day_from=3,
            day_to=5,
            order_before=16,
            price=500,
            price_to=1000,
            shipper_id=99,
        ),
        working_days=[i for i in range(10)],
    )


class _PickupBuckets:
    dsbs_bucket = PickupBucket(
        bucket_id=DSBS_PICKUP_BUCKET_ID,
        fesh=DSBS_FESH,
        carriers=[99],
        options=[PickupOption(outlet_id=DSBS_OUTLET_ID, day_from=3, day_to=5, price=500)],
        delivery_program=DeliveryBucket.REGULAR_PROGRAM,
    )


def service_time(hour, minute, day=0):
    return datetime(year=2020, month=8, day=2 + day, hour=hour, minute=minute)


class _Combinator:
    DeliveryDateFrom = date(year=2020, month=8, day=2)
    DeliveryDateTo = date(year=2020, month=8, day=12)
    DropshipWH = RoutePoint(
        point_ids=Destination(partner_id=_Shops.dsbs_shop.warehouse_id),
        segment_id=512101,
        segment_type="warehouse",
        services=(
            (DeliveryService.INTERNAL, "CUTOFF", service_time(13, 25), timedelta()),
            (DeliveryService.INTERNAL, "PROCESSING", service_time(13, 25), timedelta(hours=2)),
            (DeliveryService.OUTBOUND, "SHIPMENT", service_time(15, 25), timedelta(minutes=35)),
        ),
        partner_type="DROPSHIP",
    )
    Movement = RoutePoint(
        point_ids=Destination(partner_id=_Constants.DeliveryServiceId),
        segment_id=512002,
        segment_type="movement",
        services=(
            (DeliveryService.INBOUND, "INBOUND", service_time(16, 0), timedelta(seconds=15)),
            (DeliveryService.INTERNAL, "MOVEMENT", service_time(16, 1), timedelta(minutes=29)),
            (DeliveryService.OUTBOUND, "SHIPMENT", service_time(16, 30), timedelta(hours=1)),
        ),
    )
    Linehaul = RoutePoint(
        point_ids=Destination(partner_id=_Constants.DeliveryServiceId),
        segment_id=512003,
        segment_type="linehaul",
        services=(
            (DeliveryService.INTERNAL, "DELIVERY", service_time(17, 50), timedelta(hours=1, minutes=45)),
            (DeliveryService.INTERNAL, "LAST_MILE", service_time(19, 35), timedelta(minutes=30)),
        ),
    )
    EndPointPickup = RoutePoint(
        point_ids=Destination(logistic_point_id=_Outlets.dsbs_outlet.point_id),
        segment_id=512004,
        segment_type="pickup",
        services=(
            (
                DeliveryService.OUTBOUND,
                "HANDING",
                service_time(20, 5),
                timedelta(minutes=15),
                (Time(hour=10), Time(hour=22, minute=30)),
            ),
        ),
    )
    EndPointRegion = RoutePoint(
        point_ids=Destination(region_id=MSK_RIDS),
        segment_id=512005,
        segment_type="handing",
        services=(
            (
                DeliveryService.OUTBOUND,
                "HANDING",
                service_time(20, 5),
                timedelta(minutes=15),
                (Time(hour=10), Time(hour=22, minute=30)),
            ),
        ),
    )
    SimplePaths = [RoutePath(point_from=i, point_to=i + 1) for i in range(4)]

    Dropship_Offer = CombinatorOffer(
        shop_sku=_Offers.dsbs_offer.offerid,
        shop_id=_Shops.dsbs_shop.fesh,
        partner_id=_Shops.dsbs_shop.warehouse_id,
        available_count=4,
    )


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.shops += [_Shops.dsbs_shop]
        cls.index.offers += [
            _Offers.dsbs_offer,
            _Offers.dsbs_offer_kgt,
            _Offers.dsbs_offer_without_dimensions,
            _Offers.dsbs_offer_selfpickup_off,
            _Offers.dsbs_offer_selfpickup_on,
        ]
        cls.index.mskus += [
            _Skus.dsbs_sku,
            _Skus.dsbs_sku_kgt,
            _Skus.dsbs_sku_without_dimensions,
            _Skus.dsbs_sku_selfpickup_off,
            _Skus.dsbs_sku_selfpickup_on,
        ]
        cls.index.outlets += [_Outlets.dsbs_outlet, _Outlets.dsbs_outlet_shipper_99]
        cls.index.pickup_buckets += [_PickupBuckets.dsbs_bucket]

    @classmethod
    def prepare_unified_dsbs_seller_payment_modifiers(cls):
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=Dsbs_Payment.tarrifs, regions=[MSK_RIDS], is_dsbs_payment=True
        )
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=Dsbs_Payment.tarrifs_default, is_dsbs_payment=True
        )

    @classmethod
    def prepare_combinator(cls):
        cls.settings.check_combinator_errors = True
        report_start_time_in_ms = REQUEST_TIMESTAMP * 10**6
        cls.combinator.set_start_date(microseconds_for_disabled_random=report_start_time_in_ms)
        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_Offers.dsbs_offer, _Shops.dsbs_shop),
            courier_stats=DeliveryStats(*_CombinatorResponseStats.courier_stats),
            external_pickup_stats=DeliveryStats(*_CombinatorResponseStats.pickup_stats),
            post_stats=DeliveryStats(*_CombinatorResponseStats.post_stats),
            # В комбинатор передаются опции доставки из фида
            offer_feed_delivery_option=_DsbsFeedOption.local_delivery_option,
            offer_feed_pickup_option=_DsbsFeedOption.local_pickup_option,
        )
        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_Offers.dsbs_offer_kgt, _Shops.dsbs_shop),
            courier_stats=DeliveryStats(*_CombinatorResponseStats.courier_stats),
            external_pickup_stats=DeliveryStats(*_CombinatorResponseStats.pickup_stats),
            post_stats=DeliveryStats(*_CombinatorResponseStats.post_stats),
            # В комбинатор передаются опции доставки из фида
            offer_feed_delivery_option=_DsbsFeedOption.local_delivery_option,
            offer_feed_pickup_option=_DsbsFeedOption.local_pickup_option,
        )

        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_Offers.dsbs_offer_without_dimensions, _Shops.dsbs_shop),
            courier_stats=DeliveryStats(*_CombinatorResponseStats.courier_stats),
            external_pickup_stats=DeliveryStats(*_CombinatorResponseStats.pickup_stats),
            post_stats=DeliveryStats(*_CombinatorResponseStats.post_stats),
            # В комбинатор передаются опции доставки из фида
            offer_feed_delivery_option=_DsbsFeedOption.local_delivery_option,
            offer_feed_pickup_option=_DsbsFeedOption.local_pickup_option,
        )

        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_Offers.dsbs_offer_selfpickup_off, _Shops.dsbs_shop),
            external_pickup_stats=DeliveryStats(*_CombinatorResponseStats.pickup_stats),
            # В комбинатор передаются опции доставки из фида
            offer_feed_delivery_option=_DsbsFeedOption.local_delivery_option,
            offer_feed_pickup_option=_DsbsFeedOption.local_pickup_option,
            self_pickup_available=False,
        )

        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_Offers.dsbs_offer_selfpickup_on, _Shops.dsbs_shop),
            external_pickup_stats=DeliveryStats(*_CombinatorResponseStats.pickup_stats),
            # В комбинатор передаются опции доставки из фида
            offer_feed_delivery_option=_DsbsFeedOption.local_delivery_option,
            offer_feed_pickup_option=_DsbsFeedOption.local_pickup_option,
            self_pickup_available=True,
        )

        cls.combinator.on_courier_options_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=_Offers.dsbs_offer.weight * 1000,
                    dimensions=[
                        _Offers.dsbs_offer.dimensions.width,
                        _Offers.dsbs_offer.dimensions.height,
                        _Offers.dsbs_offer.dimensions.length,
                    ],
                    cargo_types=[],
                    offers=[
                        CombinatorOffer(
                            shop_sku=_Offers.dsbs_offer.offerid,
                            shop_id=_Shops.dsbs_shop.fesh,
                            partner_id=_Shops.dsbs_shop.warehouse_id,
                            available_count=1,
                            feed_id=_Offers.dsbs_offer.feed_id(),
                            categories=[1, 2, 3],
                            offer_feed_delivery_option=_DsbsFeedOption.local_delivery_option,
                            offer_feed_pickup_option=_DsbsFeedOption.local_pickup_option,
                        ),
                    ],
                    price=_Offers.dsbs_offer.price,
                ),
            ],
            destination=Destination(region_id=MSK_RIDS),
            payment_methods=[],
            total_price=_Offers.dsbs_offer.price,
        ).respond_with_courier_options(
            options=[
                create_delivery_option(
                    cost=15,
                    date_from=DATE_FROM,
                    date_to=DATE_TO,
                    time_from=time(8, 0),
                    time_to=time(21, 0),
                    delivery_service_id=140,
                    payment_methods=[
                        PaymentMethod.PT_YANDEX,
                        PaymentMethod.PT_CARD_ON_DELIVERY,
                    ],
                )
            ],
            virtual_box=create_virtual_box(weight=18000, length=100, width=80, height=50),
        )

        cls.combinator.on_pickup_points_grouped_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=_Offers.dsbs_offer.weight * 1000,
                    dimensions=[
                        _Offers.dsbs_offer.dimensions.width,
                        _Offers.dsbs_offer.dimensions.height,
                        _Offers.dsbs_offer.dimensions.length,
                    ],
                    cargo_types=[],
                    offers=[
                        CombinatorOffer(
                            shop_sku=_Offers.dsbs_offer.offerid,
                            shop_id=_Shops.dsbs_shop.fesh,
                            partner_id=_Shops.dsbs_shop.warehouse_id,
                            available_count=1,
                            feed_id=_Offers.dsbs_offer.feed_id(),
                            categories=[1, 2, 3],
                            offer_feed_delivery_option=_DsbsFeedOption.local_delivery_option,
                            offer_feed_pickup_option=_DsbsFeedOption.local_pickup_option,
                        ),
                    ],
                    price=_Offers.dsbs_offer.price,
                ),
            ],
            destination_regions=[MSK_RIDS],
            point_types=[],
            total_price=_Offers.dsbs_offer.price,
        ).respond_with_grouped_pickup_points(
            groups=[
                PickupPointGrouped(
                    ids_list=[DSBS_OUTLET_ID, DSBS_OUTLET_ID2],
                    outlet_type=OutletType.FOR_PICKUP,
                    service_id=99,
                    cost=100,
                    date_from=DATE_FROM,
                    date_to=DATE_TO,
                    payment_methods=[PaymentMethod.PT_CASH_ON_DELIVERY],
                    external_logistics=True,
                    dsbs_to_market_outlet=False,
                    delivery_intervals=[
                        PointTimeInterval(point_id=1, time_from=time(10, 0), time_to=time(22, 0)),
                    ],
                    dsbs_ids=[str(DSBS_OUTLET_ID_FROM_COMBI1), str(DSBS_OUTLET_ID_FROM_COMBI2)],
                ),
            ],
            virtual_box=create_virtual_box(weight=18000, length=100, width=80, height=50),
        )

        cls.combinator.on_courier_options_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=_Offers.dsbs_offer_kgt.weight * 1000,
                    dimensions=[
                        _Offers.dsbs_offer_kgt.dimensions.width,
                        _Offers.dsbs_offer_kgt.dimensions.height,
                        _Offers.dsbs_offer_kgt.dimensions.length,
                    ],
                    cargo_types=[],
                    offers=[
                        CombinatorOffer(
                            shop_sku=_Offers.dsbs_offer_kgt.offerid,
                            shop_id=_Shops.dsbs_shop.fesh,
                            partner_id=_Shops.dsbs_shop.warehouse_id,
                            available_count=1,
                            feed_id=_Offers.dsbs_offer_kgt.feed_id(),
                            categories=[1, 2, 3],
                            offer_feed_delivery_option=_DsbsFeedOption.local_delivery_option,
                            offer_feed_pickup_option=_DsbsFeedOption.local_pickup_option,
                        ),
                    ],
                    price=_Offers.dsbs_offer_kgt.price,
                ),
            ],
            destination=Destination(region_id=MSK_RIDS),
            payment_methods=[],
            total_price=_Offers.dsbs_offer_kgt.price,
        ).respond_with_courier_options(
            options=[
                create_delivery_option(
                    cost=15,
                    date_from=DATE_FROM,
                    date_to=DATE_TO,
                    time_from=time(8, 0),
                    time_to=time(21, 0),
                    delivery_service_id=140,
                    payment_methods=[
                        PaymentMethod.PT_YANDEX,
                        PaymentMethod.PT_CARD_ON_DELIVERY,
                    ],
                )
            ],
            virtual_box=create_virtual_box(weight=18000, length=100, width=80, height=50),
        )

        cls.combinator.on_pickup_points_grouped_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=_Offers.dsbs_offer_kgt.weight * 1000,
                    dimensions=[
                        _Offers.dsbs_offer_kgt.dimensions.width,
                        _Offers.dsbs_offer_kgt.dimensions.height,
                        _Offers.dsbs_offer_kgt.dimensions.length,
                    ],
                    cargo_types=[],
                    offers=[
                        CombinatorOffer(
                            shop_sku=_Offers.dsbs_offer_kgt.offerid,
                            shop_id=_Shops.dsbs_shop.fesh,
                            partner_id=_Shops.dsbs_shop.warehouse_id,
                            available_count=1,
                            feed_id=_Offers.dsbs_offer_kgt.feed_id(),
                            categories=[1, 2, 3],
                            offer_feed_delivery_option=_DsbsFeedOption.local_delivery_option,
                            offer_feed_pickup_option=_DsbsFeedOption.local_pickup_option,
                        ),
                    ],
                    price=_Offers.dsbs_offer_kgt.price,
                ),
            ],
            destination_regions=[MSK_RIDS],
            point_types=[],
            total_price=_Offers.dsbs_offer_kgt.price,
        ).respond_with_grouped_pickup_points(
            groups=[
                PickupPointGrouped(
                    ids_list=[10002153760],
                    outlet_type=OutletType.FOR_PICKUP,
                    service_id=322,
                    cost=100,
                    date_from=DATE_FROM,
                    date_to=DATE_TO,
                    payment_methods=[PaymentMethod.PT_CASH_ON_DELIVERY],
                    delivery_intervals=[
                        PointTimeInterval(point_id=1, time_from=time(10, 0), time_to=time(22, 0)),
                    ],
                    dsbs_ids=[str(DSBS_OUTLET_ID_FROM_COMBI1)],
                ),
            ],
            virtual_box=create_virtual_box(weight=18000, length=100, width=80, height=50),
        )

        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.PICKUP,
            destination=_Combinator.EndPointPickup,
            delivery_option=create_delivery_option(),
            total_price=_Offers.dsbs_offer.price,
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=_Offers.dsbs_offer.weight * 1000,
                    dimensions=[
                        _Offers.dsbs_offer.dimensions.width,
                        _Offers.dsbs_offer.dimensions.height,
                        _Offers.dsbs_offer.dimensions.length,
                    ],
                    cargo_types=[],
                    offers=[
                        CombinatorOffer(
                            shop_sku=_Offers.dsbs_offer.offerid,
                            shop_id=_Shops.dsbs_shop.fesh,
                            partner_id=_Shops.dsbs_shop.warehouse_id,
                            available_count=1,
                            feed_id=_Offers.dsbs_offer.feed_id(),
                            categories=[1, 2, 3],
                            offer_feed_delivery_option=_DsbsFeedOption.local_delivery_option,
                            offer_feed_pickup_option=_DsbsFeedOption.local_pickup_option,
                        ),
                    ],
                    price=_Offers.dsbs_offer.price,
                ),
            ],
        ).respond_with_delivery_route(
            offers=[_Combinator.Dropship_Offer],
            points=[
                _Combinator.DropshipWH,
                _Combinator.Movement,
                _Combinator.Linehaul,
                _Combinator.EndPointPickup,
            ],
            paths=_Combinator.SimplePaths,
            date_from=_Combinator.DeliveryDateFrom,
            date_to=_Combinator.DeliveryDateTo,
            virtual_box=create_virtual_box(
                weight=_Constants.TestVirtualBox.Weight,
                length=_Constants.TestVirtualBox.Length,
                width=_Constants.TestVirtualBox.Width,
                height=_Constants.TestVirtualBox.Height,
            ),
            string_delivery_dates=create_string_delivery_dates(
                shipment_date="2020-08-02T00:00:00+03:00",
                packaging_time="PT19H30M",
                shipment_by_supplier="2020-08-02T00:00:00+03:00",
                reception_by_warehouse="2020-08-02T00:00:00+03:00",
            ),
            shipment_warehouse=322,
            is_external_logistics=True,
            is_dsbs_to_market_outlet=False,
        )

    def test_dsbs_on_model_card(self):
        "Проверяется коррекный расчет доставки ДСБС через комбинатор"
        request = (
            'place={place}&'
            'offerid=SkuDSBS-------------eg&'
            'rids={rid}&'
            'regset=2&'
            'pickup-options=grouped&'
            'market-sku={msku}&'
            'combinator=1&'
            'pp=18&'
            'rearr-factors={rearr}'
        )

        # Под флагами enable_dsbs_combinator_request и use_dsbs_combinator_response опции для ДСБС офферов расчитываются через комбинатор
        for place, use_dsbs_nordstream in itertools.product(['sku_offers&rgb=blue', 'productoffers'], [0, 1]):
            response = self.report.request_json(
                request.format(
                    place=place,
                    rid=MSK_RIDS,
                    msku=DSBS_MSKU,
                    enable_request=1,
                    rearr=make_rearr(
                        enable_dsbs_combinator_request=1,
                        use_dsbs_combinator_response=1,
                        market_nordstream_dsbs=use_dsbs_nordstream,
                    ),
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 1,
                        "totalOffers": 1,
                    }
                },
            )
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "options": [
                            {
                                "dayFrom": _CombinatorResponseStats.courier_stats.day_from,
                                "dayTo": _CombinatorResponseStats.courier_stats.day_to,
                                "serviceId": "99",
                                "partnerType": "regular",
                            }
                        ],
                        "pickupOptions": [
                            {
                                "serviceId": 99,
                                "dayFrom": _CombinatorResponseStats.pickup_stats.day_from,
                                "dayTo": _CombinatorResponseStats.pickup_stats.day_to,
                            }
                        ],
                        "postStats": {
                            "minDays": _CombinatorResponseStats.post_stats.day_from,
                            "maxDays": _CombinatorResponseStats.post_stats.day_to,
                        },
                    }
                },
            )

        # Флаг enable_dsbs_combinator_request включает обработку ДСБС через комбинатор
        # С помощью флага use_dsbs_combinator_response можно игнорировать ответ комбинатора
        for enable_request, use_response in ([1, 0], [0, 1], [0, 0]):
            for place in ['sku_offers&rgb=blue', 'productoffers']:
                response = self.report.request_json(
                    request.format(
                        place=place,
                        rid=MSK_RIDS,
                        msku=DSBS_MSKU,
                        rearr=make_rearr(
                            enable_dsbs_combinator_request=enable_request,
                            use_dsbs_combinator_response=use_response,
                            market_nordstream_dsbs=use_dsbs_nordstream,
                        ),
                    )
                )
                self.assertFragmentIn(
                    response,
                    {
                        "search": {
                            "total": 1,
                            "totalOffers": 1,
                        }
                    },
                )
                self.assertFragmentIn(
                    response,
                    {
                        "delivery": {
                            "options": [
                                {
                                    "dayFrom": _DsbsFeedOption.local_delivery_option.day_from,
                                    "dayTo": _DsbsFeedOption.local_delivery_option.day_to,
                                    "serviceId": "99",
                                    "partnerType": "regular",
                                }
                            ],
                            "pickupOptions": [
                                {
                                    "serviceId": 100,
                                    "dayFrom": _DsbsFeedOption.local_pickup_option.day_from,
                                    "dayTo": _DsbsFeedOption.local_pickup_option.day_to,
                                    "orderBefore": _DsbsFeedOption.local_pickup_option.order_before,
                                }
                            ],
                            "postStats": Absent(),
                        }
                    },
                )

    def test_dsbs_offer_without_dimensions(self):
        "Проверяется, что для ДСБС офферов не обязятельны весогабариты"
        request = (
            'place={place}&'
            'offerid=DSBSwithout-dimens--eg&'
            'rids={rid}&'
            'regset=2&'
            'pickup-options=grouped&'
            'market-sku={msku}&'
            'combinator=1&'
            'pp=18&'
            'rearr-factors={rearr}'
        )

        for place, use_dsbs_nordstream in itertools.product(['sku_offers&rgb=blue', 'productoffers'], [0, 1]):
            response = self.report.request_json(
                request.format(
                    place=place,
                    rid=MSK_RIDS,
                    msku=DSBS_MSKU2,
                    rearr=make_rearr(
                        enable_dsbs_combinator_request=1,
                        use_dsbs_combinator_response=1,
                        market_nordstream_dsbs=use_dsbs_nordstream,
                    ),
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 1,
                        "totalOffers": 1,
                    }
                },
            )
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "options": [
                            {
                                "dayFrom": _CombinatorResponseStats.courier_stats.day_from,
                                "dayTo": _CombinatorResponseStats.courier_stats.day_to,
                                "serviceId": "99",
                                "partnerType": "regular",
                            }
                        ],
                        "pickupOptions": [
                            {
                                "serviceId": 99,
                                "dayFrom": _CombinatorResponseStats.pickup_stats.day_from,
                                "dayTo": _CombinatorResponseStats.pickup_stats.day_to,
                            }
                        ],
                        "postStats": {
                            "minDays": _CombinatorResponseStats.post_stats.day_from,
                            "maxDays": _CombinatorResponseStats.post_stats.day_to,
                        },
                    }
                },
            )

    def test_actual_delivery(self):
        """Проверяется, что для рассчетов доставки ДСБС используется комбинатор,
        также проверяем корректность расчета стоимости доставки дсбс посылки."""
        request = (
            'place=actual_delivery'
            '&offers-list={offers}:1'
            '&rids=213'
            '&pickup-options=grouped'
            '&pickup-options-extended-grouping=1'
            '&combinator=1&debug=1'
            '&total-price={price}'
            '&rearr-factors={rearr}'
        )
        for use_dsbs_points, use_dsbs_nordstream in itertools.product([0, 1], [0, 1]):
            for total_price in [_Offers.dsbs_offer.price, 700]:
                self.error_log.expect(code=ErrorCodes.FAILED_TO_FIND_DELICALC_GENERATION_NUMBER)
                response = self.report.request_json(
                    request.format(
                        offers=_Offers.dsbs_offer.ware_md5,
                        price=total_price,
                        rearr=make_rearr(
                            enable_dsbs_combinator_request_in_actual_delivery=1,
                            use_dsbs_combinator_response_in_actual_delivery=1,
                            use_dsbs_string_point_ids=use_dsbs_points,
                            market_nordstream_dsbs=use_dsbs_nordstream,
                        ),
                    )
                )
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {
                                "delivery": {
                                    "deliveryPartnerTypes": ["SHOP"],
                                    "options": [
                                        {
                                            "dayFrom": 2,
                                            "dayTo": 4,
                                            "timeIntervals": Absent(),
                                            "paymentMethods": ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"],
                                            "partnerType": "regular",
                                            "supplierPrice": {
                                                "currency": "RUR",
                                                "value": "111",
                                            },
                                            "supplierDiscount": {
                                                "currency": "RUR",
                                                "value": "12",
                                            },
                                            "isExternalLogistics": False,
                                        }
                                    ],
                                    # Здесь произошло расхлопываение группы, которую прислал комбинатор
                                    # Так как у одного аутлета shipperId = 99 мы обнулили доставку в этот аутлет для дсбс посылки,
                                    # у другого же аутлета стоимость доставки не обнулилась
                                    "pickupOptions": [
                                        {
                                            "serviceId": 99,
                                            "partnerType": "regular",
                                            "price": {"currency": "RUR", "value": "0"},
                                            "supplierPrice": {
                                                "currency": "RUR",
                                                "value": "112",
                                            },
                                            "supplierDiscount": {
                                                "currency": "RUR",
                                                "value": "112",
                                            },
                                            "dayFrom": 2,
                                            "dayTo": 4,
                                            "paymentMethods": ["CASH_ON_DELIVERY"],
                                            "isExternalLogistics": True,
                                            "isDsbsToMarketOutlet": False,
                                            "outletIds": [
                                                DSBS_OUTLET_ID_FROM_COMBI2 if use_dsbs_points else DSBS_OUTLET_ID2
                                            ],
                                        },
                                        {
                                            "serviceId": 99,
                                            "partnerType": "regular",
                                            "price": {"currency": "RUR", "value": "99"},
                                            "supplierPrice": {
                                                "currency": "RUR",
                                                "value": "112",
                                            },
                                            "supplierDiscount": {
                                                "currency": "RUR",
                                                "value": "13",
                                            },
                                            "dayFrom": 2,
                                            "dayTo": 4,
                                            "paymentMethods": ["CASH_ON_DELIVERY"],
                                            "isExternalLogistics": True,
                                            "isDsbsToMarketOutlet": False,
                                            "outletIds": [
                                                DSBS_OUTLET_ID_FROM_COMBI1 if use_dsbs_points else DSBS_OUTLET_ID
                                            ],
                                        },
                                    ],
                                }
                            }
                        ]
                    },
                    allow_different_len=False,
                )

    def test_actual_delivery_kgt(self):
        "Проверяется, что для рассчетов доставки ДСБС используется комбинатор"
        request = (
            'place=actual_delivery'
            '&offers-list={offers}:1'
            '&rids=213'
            '&pickup-options=grouped'
            '&pickup-options-extended-grouping=1'
            '&combinator=1&debug=1'
            '&rearr-factors={rearr}'
        )
        for use_dsbs_points, use_dsbs_nordstream in itertools.product([0, 1], [0, 1]):
            self.error_log.expect(code=ErrorCodes.FAILED_TO_FIND_DELICALC_GENERATION_NUMBER)
            response = self.report.request_json(
                request.format(
                    offers=_Offers.dsbs_offer_kgt.ware_md5,
                    rearr=make_rearr(
                        enable_dsbs_combinator_request_in_actual_delivery=1,
                        use_dsbs_combinator_response_in_actual_delivery=1,
                        use_dsbs_string_point_ids=use_dsbs_points,
                        market_nordstream_dsbs=use_dsbs_nordstream,
                    ),
                ),
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "delivery": {
                                "deliveryPartnerTypes": ["SHOP"],
                                "options": [
                                    {
                                        "dayFrom": 2,
                                        "dayTo": 4,
                                        "timeIntervals": Absent(),
                                        "paymentMethods": ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"],
                                        "partnerType": "regular",
                                        "supplierPrice": {
                                            "currency": "RUR",
                                            "value": "1011",
                                        },
                                        "supplierDiscount": {
                                            "currency": "RUR",
                                            "value": "912",
                                        },
                                    }
                                ],
                                "pickupOptions": [
                                    {
                                        "serviceId": 322,
                                        "partnerType": "regular",
                                        "supplierPrice": {
                                            "currency": "RUR",
                                            "value": "1012",
                                        },
                                        "supplierDiscount": {
                                            "currency": "RUR",
                                            "value": "913",
                                        },
                                        "dayFrom": 2,
                                        "dayTo": 4,
                                        "paymentMethods": ["CASH_ON_DELIVERY"],
                                        "outletIds": [DSBS_OUTLET_ID_FROM_COMBI1 if use_dsbs_points else 10002153760],
                                    }
                                ],
                            }
                        }
                    ]
                },
                allow_different_len=False,
            )

    def make_expected_response(self, delivery_type, user_price, shipment_offset=False, nordstream=True):
        def make_expected_points():
            if delivery_type == DeliveryType.PICKUP:
                end_point = _Combinator.EndPointPickup
            else:
                end_point = _Combinator.EndPointRegion
            points = [
                _Combinator.DropshipWH,
                _Combinator.Movement,
                _Combinator.Linehaul,
                end_point,
            ]
            return points

        def make_expected_supplier_processing():
            reception_by_warehouse = "2020-08-02T00:00:00+03:00"
            return {"shipmentBySupplier": reception_by_warehouse, "receptionByWarehouse": reception_by_warehouse}

        tariff_id = 100147

        route_points = make_expected_points()
        route_paths = _Combinator.SimplePaths
        date_from, date_to = _Combinator.DeliveryDateFrom, _Combinator.DeliveryDateTo

        supplier_processing = make_expected_supplier_processing()
        expected_time_intervals = EqualToOneOfOrAbsent([{"from": "10:00", "to": "22:30", "isDefault": True}])

        supplier_price = Absent()
        supplier_discount = Absent()
        if delivery_type == DeliveryType.PICKUP:
            supplier_price = {
                "currency": "RUR",
                "value": "112",
            }
            supplier_discount = {
                "currency": "RUR",
                "value": "13",
            }
        response = {
            "search": {
                "results": [
                    {
                        "entity": "deliveryGroup",
                        "offers": [
                            {
                                "entity": "offer",
                                "wareId": _Offers.dsbs_offer.waremd5,
                                "fulfillmentWarehouse": 322 if nordstream else NotEmpty(),
                            }
                        ],
                        "fakeOffers": EmptyList(),
                        "delivery": {
                            "hasPickup": delivery_type == DeliveryType.PICKUP,
                            "hasPost": False,
                            "route": {
                                "offers": [
                                    {
                                        "partner_id": _Combinator.Dropship_Offer.partner_id,
                                        "available_count": _Combinator.Dropship_Offer.available_count,
                                        "shop_id": _Combinator.Dropship_Offer.shop_id,
                                        "shop_sku": _Combinator.Dropship_Offer.shop_sku,
                                    }
                                ],
                                "route": {
                                    "paths": [
                                        {"point_from": p.point_from, "point_to": p.point_to} for p in route_paths
                                    ],
                                    "points": [
                                        {
                                            "ids": {
                                                "region_id": p.point_ids.region_id
                                                if p.point_ids.region_id is not None
                                                else 0,
                                                "partner_id": p.point_ids.partner_id
                                                if p.point_ids.partner_id is not None
                                                else 0,
                                                "logistic_point_id": p.point_ids.logistic_point_id
                                                if p.point_ids.logistic_point_id is not None
                                                else 0,
                                            },
                                            "segment_id": p.segment_id,
                                        }
                                        for p in route_points
                                    ],
                                    "tariff_id": tariff_id,
                                    "cost": _Constants.MockRouteCost,
                                    "cost_for_shop": _Constants.MockRouteCostForShop,
                                    "delivery_type": DeliveryType.to_string(delivery_type),
                                    "date_from": {
                                        "year": date_from.year,
                                        "month": date_from.month,
                                        "day": date_from.day,
                                    },
                                    "date_to": {"year": date_to.year, "month": date_to.month, "day": date_to.day},
                                },
                            },
                            "option": {
                                "price": {"currency": "RUR", "value": str(user_price)},
                                "supplierPrice": supplier_price,
                                "supplierDiscount": supplier_discount,
                                "tariffId": tariff_id,
                                "supplierProcessing": Absent(),
                                "shipmentBySupplier": supplier_processing['shipmentBySupplier'],
                                "receptionByWarehouse": supplier_processing['receptionByWarehouse'],
                                "timeIntervals": expected_time_intervals,
                                "partnerType": "regular",
                                "packagingTime": "PT19H30M" if not shipment_offset else "PT43H30M",
                                "outletIds": Absent(),
                                "shipmentDay": 0,
                                "isExternalLogistics": True,
                                "isDsbsToMarketOutlet": False,
                            },
                        },
                    }
                ]
            }
        }

        response["search"]["results"][0]["weight"] = str(_Constants.TestVirtualBox.Weight / 1000)
        response["search"]["results"][0]["dimensions"] = [
            str(i)
            for i in [
                _Constants.TestVirtualBox.Length,
                _Constants.TestVirtualBox.Width,
                _Constants.TestVirtualBox.Height,
            ]
        ]

        return response

    def test_delivery_route_dsbs(self):
        "Проверяется коррекный расчет доставки ДСБС через комбинатор"
        for total_price, use_dsbs_nordstream in itertools.product([_Offers.dsbs_offer.price, 700], [0, 1]):
            request = (
                'place=delivery_route'
                '&pp=18'
                '&rids=213'
                '&offers-list={offer}:1'
                '&delivery-type={type}'
                '&{type_param}'
                '&combinator=1&debug=1'
                '&total-price={total_price}'
                '&rearr-factors={rearr}'
            )
            response = self.report.request_json(
                request.format(
                    offer=_Offers.dsbs_offer.ware_md5,
                    type='pickup',
                    total_price=total_price,
                    type_param='point_id={}'.format(_Outlets.dsbs_outlet.point_id),
                    rearr=make_rearr(
                        market_nordstream=1,
                        enable_dsbs_combinator_request_in_actual_delivery=1,
                        use_dsbs_combinator_response_in_actual_delivery=1,
                        use_dsbs_string_point_ids=1,
                        enable_dsbs_combinator_request=1,
                        use_dsbs_combinator_response=1,
                        market_nordstream_dsbs=use_dsbs_nordstream,
                    ),
                )
            )

            self.assertFragmentIn(
                response,
                self.make_expected_response(
                    delivery_type=DeliveryType.PICKUP,
                    user_price=_Constants.DeliveryPrice,
                    nordstream=1,
                ),
            )

    def test_self_pickup_available_flag(self):
        """
        Проверяем, что самовывоз бесплатный при self_pickup_available=1 и при включенном rearr-флаге enable_self_pickup_available_from_combinator
        """

        for enable_flag, use_dsbs_nordstream in itertools.product([0, 1], [0, 1]):
            request = (
                'place=productoffers&'
                'rids={rid}&'
                'regset=2&'
                'hid={hid}&'
                'hyperid={hyperid}&'
                'pickup-options=grouped&'
                'combinator=1&'
                'pp=18&'
                'rearr-factors={rearr}'
            )
            response = self.report.request_json(
                request.format(
                    rid=MSK_RIDS,
                    hid=CATEGORY_ID_PICKUP,
                    hyperid=MODEL_ID_PICKUP,
                    enable_flag=enable_flag,
                    rearr=make_rearr(
                        enable_self_pickup_available_from_combinator=enable_flag,
                        market_nordstream_dsbs=use_dsbs_nordstream,
                    ),
                )
            )
            self.assertFragmentIn(
                response,
                [
                    {
                        'entity': 'offer',
                        'wareId': _DsbsCombiSelfPickupAvailableFalse.offer_wmd5,
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "serviceId": 99,
                                    "price": {
                                        "currency": "RUR",
                                        "value": Regex("[^0]+"),
                                    },
                                    "dayFrom": _CombinatorResponseStats.pickup_stats.day_from,
                                    "dayTo": _CombinatorResponseStats.pickup_stats.day_to,
                                }
                            ],
                        },
                    },
                    {
                        'entity': 'offer',
                        'wareId': _DsbsCombiSelfPickupAvailableTrue.offer_wmd5,
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "serviceId": 99,
                                    "price": {
                                        "currency": "RUR",
                                        "value": "0" if enable_flag else Regex("[^0]+"),
                                    },
                                    "dayFrom": _CombinatorResponseStats.pickup_stats.day_from,
                                    "dayTo": _CombinatorResponseStats.pickup_stats.day_to,
                                }
                            ],
                        },
                    },
                ],
                allow_different_len=False,
            )


if __name__ == '__main__':
    main()
