#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import math
import datetime
from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryRestriction,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseDelivery,
    DynamicWarehouseInfo,
    DynamicWarehouseLink,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    DynamicDeliveryTariff,
    GpsCoord,
    MarketSku,
    Model,
    OfferDimensions,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    Shop,
    Tax,
    TimeInfo,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.types.combinator import CombinatorGpsCoords, CombinatorExpressWarehouse
from core.logs import ErrorCodes
from core.matcher import Absent, Contains, ElementCount, EmptyList, NotEmpty, EqualToOneOfOrAbsent
from core.testcase import (
    TestCase,
    main,
)
from core.types.combinator import (
    create_string_delivery_dates,
    create_delivery_option,
    create_virtual_box,
    DeliveryType,
    Destination,
    CombinatorOffer,
    RoutePoint,
    RoutePath,
)
from market.combinator.proto.grpc.combinator_pb2 import DeliveryService, Time
from core.types.delivery import OutletWorkingTime
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


class _Gps:
    _lat = 15.1234
    _lon = 13.4321

    location = GpsCoord(_lon, _lat)
    location_combinator = CombinatorGpsCoords(_lat, _lon)
    location_str = 'lat:{lat};lon:{lon}'.format(lat=_lat, lon=_lon)


class _Requests:
    BaseRequestTemplate = (
        'place=delivery_route'
        '&pp=18'
        '&rids={rids}'
        '&rgb=blue'
        '&offers-list={offers}'
        '&delivery-type={type}'
        '&{type_param}'
        '&rearr-factors='
        'market_nordstream={nordstream}'
    )


class _Constants:
    DeliveryServiceId = 157
    PostTermDeliveryServiceId = 268
    SortingCenterId = 54321
    MskRids = 213
    SpbRids = 2
    OtherRids = 322
    OtherRids2 = 323
    BibirevoRids = 20379
    Today = 76  # current day index
    DeliveryPriceBeforeThreshold = 70
    DeliveryPriceAfterThreshold = 20
    DeliveryPriceKgt = 100
    MockRouteCost = 50
    MockRouteCostForShop = 50

    class TestVirtualBox:
        Weight = 18000
        Length = 100
        Width = 80
        Height = 50


class _Outlets:
    @staticmethod
    def working_times():
        return [
            OutletWorkingTime(
                days_from=OutletWorkingTime.MONDAY,
                days_till=OutletWorkingTime.SUNDAY,
                hours_from='09:00',
                hours_till='20:00',
            )
        ]

    @staticmethod
    def pickup_option():
        return OutletDeliveryOption(
            shipper_id=_Constants.DeliveryServiceId, day_from=2, day_to=3, work_in_holiday=True, price=10
        )

    @staticmethod
    def post_option():
        return OutletDeliveryOption(
            shipper_id=_Constants.DeliveryServiceId,
            day_from=6,
            day_to=10,
            order_before=2,
            work_in_holiday=True,
            price=150,
        )

    @staticmethod
    def post_term_option():
        return OutletDeliveryOption(
            shipper_id=_Constants.PostTermDeliveryServiceId,
            day_from=2,
            day_to=3,
            order_before=2,
            work_in_holiday=True,
            price=150,
        )

    PickupOutlet = Outlet(
        point_id=100001,
        delivery_service_id=_Constants.DeliveryServiceId,
        region=_Constants.MskRids,
        point_type=Outlet.FOR_PICKUP,
        working_days=list(range(15)),
        working_times=working_times.__func__(),
        gps_coord=GpsCoord(37.12, 55.32),
        delivery_option=pickup_option.__func__(),
    )

    PickupOutletMarketBranded = Outlet(
        point_id=100003,
        delivery_service_id=_Constants.DeliveryServiceId,
        region=_Constants.MskRids,
        point_type=Outlet.FOR_PICKUP,
        working_days=list(range(15)),
        working_times=working_times.__func__(),
        gps_coord=GpsCoord(37.12, 55.32),
        delivery_option=pickup_option.__func__(),
        bool_props=["isMarketBranded"],
    )

    PostOutlet = Outlet(
        point_id=2001,
        delivery_service_id=_Constants.DeliveryServiceId,
        region=_Constants.MskRids,
        point_type=Outlet.FOR_POST,
        post_code=123123,
        delivery_option=post_option.__func__(),
        working_days=list(range(20)),
        gps_coord=GpsCoord(36.87, 55.11),
    )
    PostTermOutlet = Outlet(
        point_id=3001,
        delivery_service_id=_Constants.PostTermDeliveryServiceId,
        region=_Constants.MskRids,
        point_type=Outlet.FOR_POST_TERM,
        post_code=123124,
        delivery_option=post_term_option.__func__(),
        working_days=list(range(20)),
        working_times=working_times.__func__(),
        gps_coord=GpsCoord(36.86, 55.12),
    )

    # copies for testing invalid route
    PickupOutlet_2 = Outlet(
        point_id=100002,
        delivery_service_id=_Constants.DeliveryServiceId,
        region=_Constants.MskRids,
        point_type=Outlet.FOR_PICKUP,
        working_days=list(range(15)),
        working_times=working_times.__func__(),
        gps_coord=GpsCoord(37.12, 55.32),
        delivery_option=pickup_option.__func__(),
    )
    PostOutlet_2 = Outlet(
        point_id=2002,
        delivery_service_id=_Constants.DeliveryServiceId,
        region=_Constants.MskRids,
        point_type=Outlet.FOR_POST,
        post_code=123124,
        delivery_option=post_option.__func__(),
        working_days=list(range(20)),
        gps_coord=GpsCoord(36.87, 55.11),
    )


class _Shops:
    VirtualBlue = Shop(
        fesh=1,
        datafeed_id=1,
        priority_region=_Constants.MskRids,
        tax_system=Tax.OSN,
        fulfillment_virtual=True,
        cpa=Shop.CPA_REAL,
        virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
        delivery_service_outlets=[
            _Outlets.PickupOutlet.point_id,
            _Outlets.PickupOutlet_2.point_id,
            _Outlets.PickupOutletMarketBranded.point_id,
        ],
    )
    Shop_3P = Shop(
        fesh=2,
        client_id=2,
        datafeed_id=2,
        warehouse_id=222,
        priority_region=_Constants.MskRids,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        cpa=Shop.CPA_REAL,
        fulfillment_program=True,
    )
    DropshipShop = Shop(
        fesh=4,
        datafeed_id=4,
        warehouse_id=444,
        priority_region=_Constants.MskRids,
        name='Dropship',
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        fulfillment_program=False,
        delivery_service_outlets=[3001],
    )
    ExpressShop = Shop(
        fesh=5,
        datafeed_id=5,
        priority_region=_Constants.MskRids,
        name='express_shop_blue_1',
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        warehouse_id=5,
        with_express_warehouse=True,
    )
    all_supplier_ids = (
        Shop_3P.warehouse_id,
        DropshipShop.warehouse_id,
        _Constants.SortingCenterId,
    )


class _Offers:
    Fulfillment_3P_Offer = BlueOffer(
        offerid="ff3p",
        price=10,
        feedid=_Shops.Shop_3P.datafeed_id,
        weight=2.4,
        blue_weight=2.4,
        waremd5='Fulfillment3P________g',
        dimensions=OfferDimensions(length=10.1, width=24.7, height=30),
        blue_dimensions=OfferDimensions(length=10.1, width=24.7, height=30),
        supplier_id=_Shops.Shop_3P.fesh,
        delivery_buckets=[800],
        pickup_buckets=[5000],
        post_buckets=[5001],
    )

    Fulfillment_3P_Offer_2 = BlueOffer(
        offerid="ff3p_2",
        waremd5='Fulfillment3P_2______g',
        price=20,
        feedid=_Shops.Shop_3P.datafeed_id,
        weight=4.1,
        blue_weight=4.1,
        dimensions=OfferDimensions(length=1.6, width=4.8, height=10),
        blue_dimensions=OfferDimensions(length=1.6, width=4.8, height=10),
        supplier_id=_Shops.Shop_3P.fesh,
        pickup_buckets=[5000],
        post_buckets=[5001],
    )

    Fulfillment_3P_Offer_Expensive = BlueOffer(
        offerid="ff3p_3",
        waremd5='Fulfillment3P_3______g',
        price=750,
        feedid=_Shops.Shop_3P.datafeed_id,
        weight=19,
        blue_weight=19,
        dimensions=OfferDimensions(length=1.8, width=5.8, height=10),
        blue_dimensions=OfferDimensions(length=1.8, width=4.8, height=10),
        supplier_id=_Shops.Shop_3P.fesh,
        pickup_buckets=[5000],
        post_buckets=[5001],
    )

    LargeSize_Fulfillment_3P_Offer_Expensive = BlueOffer(
        offerid="LargeSizeff3p_3",
        waremd5='LargeSizeFF3P_3______g',
        price=750,
        feedid=_Shops.Shop_3P.datafeed_id,
        weight=30,
        blue_weight=30,
        dimensions=OfferDimensions(length=1.8, width=5.8, height=10),
        blue_dimensions=OfferDimensions(length=1.8, width=4.8, height=10),
        supplier_id=_Shops.Shop_3P.fesh,
        pickup_buckets=[5000],
        post_buckets=[5001],
    )

    Dropship_Offer = BlueOffer(
        offerid="dropship",
        waremd5='DropshipOffer________g',
        price=30,
        feedid=_Shops.DropshipShop.datafeed_id,
        weight=1.3,
        blue_weight=1.3,
        dimensions=OfferDimensions(length=1.3, width=2.1, height=3.2),
        blue_dimensions=OfferDimensions(length=1.3, width=2.1, height=3.2),
        supplier_id=_Shops.DropshipShop.fesh,
        delivery_buckets=[801],
        pickup_buckets=[5002],
        post_term_delivery=True,
    )
    DropshipOfferNoPostTermFlag = BlueOffer(
        offerid="dropship_no_post_term",
        waremd5='DropshipNoPostTerm___g',
        price=30,
        feedid=_Shops.DropshipShop.datafeed_id,
        weight=1.3,
        blue_weight=1.3,
        dimensions=OfferDimensions(length=1.3, width=2.1, height=3.2),
        blue_dimensions=OfferDimensions(length=1.3, width=2.1, height=3.2),
        supplier_id=_Shops.DropshipShop.fesh,
        delivery_buckets=[801],
        pickup_buckets=[5002],
    )

    UltraLightOffer = BlueOffer(
        offerid="ultra_light",
        price=10,
        feedid=_Shops.Shop_3P.datafeed_id,
        weight=0.00023,
        blue_weight=0.00023,
        waremd5='Ultralight___________g',
        dimensions=OfferDimensions(length=1.1, width=3.2, height=0.5),
        blue_dimensions=OfferDimensions(length=1.1, width=3.2, height=0.5),
        supplier_id=_Shops.Shop_3P.fesh,
        delivery_buckets=[800],
        pickup_buckets=[5000],
        post_buckets=[5001],
    )

    ExpressOffer = BlueOffer(
        price=5,
        waremd5='Exp1Price5-IiLVm1Goleg',
        weight=5,
        dimensions=OfferDimensions(length=20, width=30, height=10),
        fesh=_Shops.ExpressShop.fesh,
        feedid=_Shops.ExpressShop.fesh,
        offerid='express',
        is_express=True,
        post_term_delivery=True,
        delivery_buckets=[801],
    )

    AviaOffer = BlueOffer(
        offerid="Avia_light",
        price=15,
        feedid=_Shops.Shop_3P.datafeed_id,
        weight=2.0,
        blue_weight=2.0,
        waremd5='AviaOffer____________g',
        dimensions=OfferDimensions(length=1.1, width=3.2, height=0.5),
        blue_dimensions=OfferDimensions(length=1.1, width=3.2, height=0.5),
        supplier_id=_Shops.Shop_3P.fesh,
        delivery_buckets=[801],
        pickup_buckets=[5002],
    )


def service_time(hour, minute, day=0):
    return datetime.datetime(year=2020, month=8, day=2 + day, hour=hour, minute=minute)


class _Combinator:
    DeliveryDateFrom = datetime.date(year=2020, month=8, day=2)
    LongMovementDateFrom = datetime.date(year=2020, month=8, day=3)
    DeliveryDateTo = datetime.date(year=2020, month=8, day=12)
    DeliveryTimeFrom = datetime.time(hour=10, minute=0)
    DeliveryTimeTo = datetime.time(hour=22, minute=30)
    ConsolidatedDeliveryTimeFrom = datetime.time(hour=11, minute=0)
    ConsolidatedDeliveryTimeTo = datetime.time(hour=18, minute=30)

    Warehouse = RoutePoint(
        point_ids=Destination(partner_id=_Shops.Shop_3P.warehouse_id),
        segment_id=512001,
        segment_type="warehouse",
        services=(
            (DeliveryService.INTERNAL, "PROCESSING", service_time(13, 25), datetime.timedelta(hours=2)),
            (DeliveryService.OUTBOUND, "SHIPMENT", service_time(15, 25), datetime.timedelta(minutes=35)),
        ),
        partner_type="FULFILLMENT",
    )
    Movement = RoutePoint(
        point_ids=Destination(partner_id=_Constants.DeliveryServiceId),
        segment_id=512002,
        segment_type="movement",
        services=(
            (DeliveryService.INBOUND, "INBOUND", service_time(16, 0), datetime.timedelta(seconds=15)),
            (DeliveryService.INTERNAL, "MOVEMENT", service_time(16, 1), datetime.timedelta(minutes=29)),
            (DeliveryService.OUTBOUND, "SHIPMENT", service_time(16, 30), datetime.timedelta(hours=1)),
        ),
    )
    ExpressMovement = RoutePoint(
        point_ids=Destination(partner_id=_Constants.DeliveryServiceId),
        segment_id=512020,
        segment_type="movement",
        services=(
            (
                DeliveryService.INTERNAL,
                "CALL_COURIER",
                service_time(16, 0),
                datetime.timedelta(seconds=15),
                None,
                {"IS_WIDE_EXPRESS": "1", "IS_FASTEST_EXPRESS": "1"},
            ),
        ),
    )
    SortingCenter = RoutePoint(
        point_ids=Destination(partner_id=999),
        segment_id=512099,
        segment_type="warehouse",
        services=(
            (DeliveryService.INTERNAL, "SORT", service_time(17, 40), datetime.timedelta(minutes=5)),
            (DeliveryService.OUTBOUND, "SHIPMENT", service_time(17, 45), datetime.timedelta()),
        ),
        partner_type="SORTING_CENTER",
    )
    SecondMovement = RoutePoint(
        point_ids=Destination(partner_id=_Constants.DeliveryServiceId),
        segment_id=512100,
        segment_type="movement",
        services=(
            (DeliveryService.INBOUND, "INBOUND", service_time(17, 45), datetime.timedelta(minutes=2)),
            (DeliveryService.INTERNAL, "MOVEMENT", service_time(17, 47), datetime.timedelta(minutes=3)),
            (DeliveryService.OUTBOUND, "SHIPMENT", service_time(17, 50), datetime.timedelta()),
        ),
    )
    DropshipWH = RoutePoint(
        point_ids=Destination(partner_id=_Shops.DropshipShop.warehouse_id),
        segment_id=512101,
        segment_type="warehouse",
        services=(
            (DeliveryService.INTERNAL, "CUTOFF", service_time(13, 25), datetime.timedelta()),
            (DeliveryService.INTERNAL, "PROCESSING", service_time(13, 25), datetime.timedelta(hours=2)),
            (DeliveryService.OUTBOUND, "SHIPMENT", service_time(15, 25), datetime.timedelta(minutes=35)),
        ),
        partner_type="DROPSHIP",
    )
    SelfMovement = RoutePoint(
        point_ids=Destination(partner_id=_Shops.DropshipShop.warehouse_id),
        segment_id=512102,
        segment_type="movement",
        services=(
            (DeliveryService.INBOUND, "INBOUND", service_time(16, 0), datetime.timedelta(seconds=15)),
            (DeliveryService.INTERNAL, "MOVEMENT", service_time(16, 1), datetime.timedelta(minutes=29)),
            (DeliveryService.OUTBOUND, "SHIPMENT", service_time(16, 30), datetime.timedelta(hours=1)),
        ),
    )
    Linehaul = RoutePoint(
        point_ids=Destination(partner_id=_Constants.DeliveryServiceId),
        segment_id=512003,
        segment_type="linehaul",
        services=(
            (DeliveryService.INTERNAL, "DELIVERY", service_time(17, 50), datetime.timedelta(hours=1, minutes=45)),
            (DeliveryService.INTERNAL, "LAST_MILE", service_time(19, 35), datetime.timedelta(minutes=30)),
        ),
    )
    EndPointPickup = RoutePoint(
        point_ids=Destination(logistic_point_id=_Outlets.PickupOutlet.point_id),
        segment_id=512004,
        segment_type="pickup",
        services=(
            (
                DeliveryService.OUTBOUND,
                "HANDING",
                service_time(20, 5),
                datetime.timedelta(minutes=15),
                (Time(hour=10), Time(hour=22, minute=30)),
            ),
        ),
    )
    EndPointPickupMarketBranded = RoutePoint(
        point_ids=Destination(logistic_point_id=_Outlets.PickupOutletMarketBranded.point_id),
        segment_id=512009,
        segment_type="pickup",
        services=(
            (
                DeliveryService.OUTBOUND,
                "HANDING",
                service_time(20, 5),
                datetime.timedelta(minutes=15),
                (Time(hour=10), Time(hour=22, minute=30)),
            ),
        ),
    )
    EndPointPickup_2 = RoutePoint(
        point_ids=Destination(logistic_point_id=_Outlets.PickupOutlet_2.point_id),
        segment_id=513004,
        segment_type="pickup",
        services=((DeliveryService.OUTBOUND, "HANDING", service_time(20, 5), datetime.timedelta(minutes=15)),),
    )
    EndPointRegion = RoutePoint(
        point_ids=Destination(region_id=_Constants.MskRids),
        segment_id=512005,
        segment_type="handing",
        services=(
            (
                DeliveryService.OUTBOUND,
                "HANDING",
                service_time(20, 5),
                datetime.timedelta(minutes=15),
                (Time(hour=10), Time(hour=22, minute=30)),
            ),
        ),
    )
    EndPointRegionConsolidated = RoutePoint(
        point_ids=Destination(region_id=_Constants.MskRids),
        segment_id=512015,
        segment_type="handing",
        services=(
            (
                DeliveryService.OUTBOUND,
                "HANDING",
                service_time(20, 5),
                datetime.timedelta(minutes=15),
                (Time(hour=11), Time(hour=18, minute=30)),
            ),
        ),
    )
    EndPointRegionBibirevo = RoutePoint(
        point_ids=Destination(region_id=_Constants.BibirevoRids),
        segment_id=512016,
        segment_type="handing",
        services=(
            (
                DeliveryService.OUTBOUND,
                "HANDING",
                service_time(20, 5),
                datetime.timedelta(minutes=15),
                (Time(hour=11), Time(hour=18, minute=30)),
            ),
        ),
    )
    EndPointPostReq = RoutePoint(
        point_ids=Destination(post_code=_Outlets.PostOutlet.post_code),
        segment_id=512008,
        segment_type="pickup",
        services=((DeliveryService.OUTBOUND, "HANDING", service_time(20, 5), datetime.timedelta(minutes=15)),),
    )
    EndPointPostReq_2 = RoutePoint(
        point_ids=Destination(post_code=_Outlets.PostOutlet_2.post_code),
        segment_id=513008,
        segment_type="pickup",
        services=((DeliveryService.OUTBOUND, "HANDING", service_time(20, 5), datetime.timedelta(minutes=15)),),
    )
    EndPointPost = RoutePoint(
        point_ids=Destination(post_code=_Outlets.PostOutlet.post_code, logistic_point_id=_Outlets.PostOutlet.point_id),
        segment_id=512008,
        segment_type="pickup",
        services=((DeliveryService.OUTBOUND, "HANDING", service_time(20, 5), datetime.timedelta(minutes=15)),),
    )
    LongMovement = RoutePoint(
        point_ids=Destination(partner_id=_Constants.DeliveryServiceId),
        segment_id=512021,
        segment_type="movement",
        services=(
            (DeliveryService.INBOUND, "INBOUND", service_time(16, 0), datetime.timedelta(hours=15)),
            (DeliveryService.INTERNAL, "MOVEMENT", service_time(7, 0, day=1), datetime.timedelta(minutes=30)),
        ),
    )
    LongMovementEndPoint = RoutePoint(
        point_ids=Destination(region_id=_Constants.SpbRids),
        segment_id=512022,
        segment_type="handing",
        services=(
            (
                DeliveryService.OUTBOUND,
                "HANDING",
                service_time(9, 0, day=1),
                datetime.timedelta(minutes=30),
                (Time(hour=10), Time(hour=18, minute=30)),
            ),
        ),
    )
    DropshipIntakeEndPoint = RoutePoint(
        point_ids=Destination(region_id=_Constants.OtherRids),
        segment_id=512023,
        segment_type="handing",
        services=(
            (
                DeliveryService.OUTBOUND,
                "HANDING",
                service_time(9, 0, day=1),
                datetime.timedelta(minutes=30),
                (Time(hour=10), Time(hour=18, minute=30)),
            ),
        ),
    )
    DropshipSelfshipmentEndPoint = RoutePoint(
        point_ids=Destination(region_id=_Constants.OtherRids2),
        segment_id=512024,
        segment_type="handing",
        services=(
            (
                DeliveryService.OUTBOUND,
                "HANDING",
                service_time(9, 0, day=1),
                datetime.timedelta(minutes=30),
                (Time(hour=10), Time(hour=18, minute=30)),
            ),
        ),
    )

    Dropship_Offer = CombinatorOffer(
        shop_sku=_Offers.Dropship_Offer.offerid,
        shop_id=_Shops.DropshipShop.fesh,
        partner_id=_Shops.DropshipShop.warehouse_id,
        available_count=4,
    )

    Fulfillment_3P_Offer = CombinatorOffer(
        shop_sku=_Offers.Fulfillment_3P_Offer.offerid,
        shop_id=_Shops.Shop_3P.fesh,
        partner_id=_Shops.Shop_3P.warehouse_id,
        available_count=3,
    )
    Fulfillment_3P_Offer_2 = CombinatorOffer(
        shop_sku=_Offers.Fulfillment_3P_Offer_2.offerid,
        shop_id=_Shops.Shop_3P.fesh,
        partner_id=_Shops.Shop_3P.warehouse_id,
        available_count=2,
    )
    Fulfillment_3P_Offer_Expensive = CombinatorOffer(
        shop_sku=_Offers.Fulfillment_3P_Offer_Expensive.offerid,
        shop_id=_Shops.Shop_3P.fesh,
        partner_id=_Shops.Shop_3P.warehouse_id,
        available_count=2,
    )
    LargeSize_Fulfillment_3P_Offer_Expensive = CombinatorOffer(
        shop_sku=_Offers.LargeSize_Fulfillment_3P_Offer_Expensive.offerid,
        shop_id=_Shops.Shop_3P.fesh,
        partner_id=_Shops.Shop_3P.warehouse_id,
        available_count=1,
    )
    UltraLightOffer = CombinatorOffer(
        shop_sku=_Offers.UltraLightOffer.offerid,
        shop_id=_Shops.Shop_3P.fesh,
        partner_id=_Shops.Shop_3P.warehouse_id,
        available_count=1,
    )

    ExpressOffer = CombinatorOffer(
        shop_sku=_Offers.ExpressOffer.offerid,
        shop_id=_Shops.ExpressShop.fesh,
        partner_id=_Shops.ExpressShop.warehouse_id,
        available_count=1,
    )

    AviaOffer = CombinatorOffer(
        shop_sku=_Offers.AviaOffer.offerid,
        shop_id=_Shops.Shop_3P.fesh,
        partner_id=_Shops.Shop_3P.warehouse_id,
        available_count=1,
    )

    SimplePaths = [RoutePath(point_from=i, point_to=i + 1) for i in range(4)]


class T(TestCase):
    @classmethod
    def beforePrepare(cls):
        cls.settings.delivery_calendar_start_date = datetime.date(day=18, month=5, year=2020)
        cls.settings.nordstream_autogenerate = False

    @classmethod
    def prepare(cls):
        cls.settings.microseconds_for_disabled_random = 1589820813000000  # 18/05/2020 @ 19:53 (MSK)
        cls.settings.report_subrole = 'blue-main'
        cls.settings.lms_autogenerate = True
        cls.index.regiontree += [
            Region(
                rid=_Constants.MskRids,
                name='Москва',
                tz_offset=10800,
                children=[Region(rid=_Constants.BibirevoRids, name='Бибирево', region_type=Region.CITY)],
            ),
            Region(
                rid=_Constants.SpbRids, name='Санкт-Петербург', tz_offset=10800, region_type=Region.FEDERATIVE_SUBJECT
            ),
            Region(
                rid=_Constants.OtherRids, name='ДругойРегион', tz_offset=10800, region_type=Region.FEDERATIVE_SUBJECT
            ),
            Region(
                rid=_Constants.OtherRids2, name='ДругойРегион2', tz_offset=10800, region_type=Region.FEDERATIVE_SUBJECT
            ),
        ]

        cls.index.models += [Model(hyperid=1, hid=1)]
        cls.index.mskus += [
            MarketSku(title="Combination", hyperid=1, sku=1, blue_offers=[_Offers.Fulfillment_3P_Offer]),
            MarketSku(title="Variation", hyperid=2, sku=2, blue_offers=[_Offers.Fulfillment_3P_Offer_2]),
            MarketSku(title="DeliveryPrice", hyperid=6, sku=6, blue_offers=[_Offers.Fulfillment_3P_Offer_Expensive]),
            MarketSku(
                title="LargeSize", hyperid=6, sku=6, blue_offers=[_Offers.LargeSize_Fulfillment_3P_Offer_Expensive]
            ),
            MarketSku(
                title="Dropship",
                hyperid=4,
                sku=4,
                is_fulfillment=False,
                blue_offers=[_Offers.Dropship_Offer, _Offers.DropshipOfferNoPostTermFlag],
            ),
            MarketSku(title="Lighter", hyperid=5, sku=5, blue_offers=[_Offers.UltraLightOffer]),
            MarketSku(title="Avia", hyperid=7, sku=7, blue_offers=[_Offers.AviaOffer]),
            MarketSku(title="Express", hyperid=7, sku=8, blue_offers=[_Offers.ExpressOffer]),
        ]
        cls.index.shops += [
            _Shops.VirtualBlue,
            _Shops.Shop_3P,
            _Shops.DropshipShop,
            _Shops.ExpressShop,
        ]
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[225],
                warehouse_with_priority=[
                    WarehouseWithPriority(warehouse, 100) for warehouse in _Shops.all_supplier_ids
                ],
            )
        ]

        cls.index.outlets += [
            _Outlets.PickupOutlet,
            _Outlets.PickupOutletMarketBranded,
            _Outlets.PickupOutlet_2,
            _Outlets.PostOutlet,
            _Outlets.PostOutlet_2,
            _Outlets.PostTermOutlet,
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=800,
                dc_bucket_id=800,
                fesh=_Shops.VirtualBlue.fesh,
                carriers=[_Constants.DeliveryServiceId],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=_Constants.MskRids, options=[DeliveryOption(price=5, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=_Constants.SpbRids, options=[DeliveryOption(price=50, day_from=2, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=801,
                dc_bucket_id=801,
                fesh=_Shops.DropshipShop.fesh,
                carriers=[_Constants.PostTermDeliveryServiceId],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=_Constants.MskRids, options=[DeliveryOption(price=10, day_from=2, day_to=3)]),
                    RegionalDelivery(
                        rid=_Constants.OtherRids, options=[DeliveryOption(price=57, day_from=2, day_to=3)]
                    ),
                    RegionalDelivery(
                        rid=_Constants.OtherRids2, options=[DeliveryOption(price=57, day_from=2, day_to=3)]
                    ),
                ],
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5000,
                dc_bucket_id=5000,
                fesh=_Shops.VirtualBlue.fesh,
                carriers=[_Constants.DeliveryServiceId],
                options=[
                    PickupOption(outlet_id=o.point_id, day_from=2, day_to=3, price=10)
                    for o in (_Outlets.PickupOutlet, _Outlets.PickupOutlet_2)
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5001,
                dc_bucket_id=5001,
                fesh=_Shops.Shop_3P.fesh,
                carriers=[_Constants.DeliveryServiceId],
                options=[PickupOption(outlet_id=_Outlets.PostOutlet.point_id, day_from=6, day_to=15, price=20)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5002,
                dc_bucket_id=5002,
                fesh=_Shops.DropshipShop.fesh,
                carriers=[_Constants.PostTermDeliveryServiceId],
                options=[PickupOption(outlet_id=_Outlets.PostTermOutlet.point_id, day_from=2, day_to=3, price=20)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]
        for wh_id in _Shops.all_supplier_ids:
            cls.dynamic.lms.append(DynamicWarehouseInfo(wh_id, home_region=213))
        cls.dynamic.lms += [
            DynamicDaysSet(key=1, days=[]),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=_Shops.all_supplier_ids),
            DynamicDeliveryServiceInfo(
                _Constants.DeliveryServiceId,
                "SomeService",
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(region_from=213, region_to=225, days_key=1),
                ],
            ),
            DynamicDeliveryServiceInfo(
                _Constants.PostTermDeliveryServiceId,
                "PostTermService",
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(region_from=213, region_to=225, days_key=1),
                ],
            ),
            DynamicWarehouseToWarehouseInfo(
                warehouse_from=_Shops.Shop_3P.warehouse_id, warehouse_to=_Shops.Shop_3P.warehouse_id
            ),
            DynamicWarehouseToWarehouseInfo(
                warehouse_from=_Shops.DropshipShop.warehouse_id,
                warehouse_to=_Constants.SortingCenterId,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=12, region_to=225, packaging_time=TimeInfo(1))
                ],
                inbound_time=TimeInfo(0, 20),
                transfer_time=TimeInfo(2),
                operation_time=0,
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=_Constants.SortingCenterId,
                delivery_service_id=_Constants.PostTermDeliveryServiceId,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=17, region_to=225)],
            ),
            DynamicWarehouseInfo(id=322, home_region=213),
        ]
        cls.dynamic.lms += [
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=shop.warehouse_id,
                delivery_service_id=_Constants.DeliveryServiceId,
                operation_time=0,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=19, region_to=213, packaging_time=TimeInfo(3))
                ],
            )
            for shop in (_Shops.Shop_3P,)
        ]

    @classmethod
    def prepare_nordstream(cls):
        cls.dynamic.nordstream += [
            DynamicWarehouseLink(_Shops.Shop_3P.warehouse_id, [_Shops.Shop_3P.warehouse_id]),
            DynamicWarehouseLink(_Shops.DropshipShop.warehouse_id, [_Constants.SortingCenterId]),
            DynamicWarehouseLink(_Constants.SortingCenterId, [_Constants.SortingCenterId]),
        ]
        regional_restrictions = [
            DynamicDeliveryRestriction(
                max_phys_weight=20000,
                max_dim_sum=150,
                max_dimensions=[50, 50, 50],
                prohibited_cargo_types=[2],
                max_payment_weight=50,
                density=10,
                min_days=1,
                max_days=3,
            ),
            DynamicDeliveryRestriction(
                max_phys_weight=40000, max_dim_sum=250, max_dimensions=[100, 100, 100], min_days=3, max_days=4
            ),
        ]
        cls.dynamic.nordstream += [
            DynamicWarehouseDelivery(
                warehouse_id,
                {
                    _Constants.MskRids: regional_restrictions,
                    _Constants.SpbRids: regional_restrictions,
                    _Constants.BibirevoRids: regional_restrictions,
                    _Constants.OtherRids: regional_restrictions,
                    _Constants.OtherRids: regional_restrictions,
                    225: [
                        DynamicDeliveryRestriction(
                            max_phys_weight=50000, max_dim_sum=220, max_dimensions=[80, 80, 80], min_days=5, max_days=6
                        )
                    ],
                },
            )
            for warehouse_id in (_Shops.Shop_3P.warehouse_id, _Constants.SortingCenterId)
        ]

    @classmethod
    def prepare_combinator(cls):
        for is_return, cnt in [(True, 1), (False, 1), (True, 3), (False, 3)]:
            cls.combinator.on_delivery_route_request(
                delivery_type=DeliveryType.PICKUP,
                destination=_Combinator.EndPointPickup,
                delivery_option=create_delivery_option(),
                total_price=_Offers.Fulfillment_3P_Offer.price * cnt,
                is_return=is_return,
            ).respond_with_delivery_route(
                offers=[_Combinator.Fulfillment_3P_Offer],
                points=[
                    _Combinator.Warehouse,
                    _Combinator.Movement,
                    _Combinator.SortingCenter,
                    _Combinator.SecondMovement,
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
                shipment_warehouse=323 if is_return else 322,
                is_return=is_return,
            )

        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.PICKUP,
            destination=_Combinator.EndPointPickupMarketBranded,
            delivery_option=create_delivery_option(),
            total_price=_Offers.Fulfillment_3P_Offer_2.price,
        ).respond_with_delivery_route(
            offers=[_Combinator.Fulfillment_3P_Offer_2],
            points=[
                _Combinator.Warehouse,
                _Combinator.Movement,
                _Combinator.SortingCenter,
                _Combinator.SecondMovement,
                _Combinator.Linehaul,
                _Combinator.EndPointPickupMarketBranded,
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
        )

        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.PICKUP,
            destination=_Combinator.EndPointPostReq,
            delivery_option=create_delivery_option(),
            total_price=_Offers.Fulfillment_3P_Offer.price,
        ).respond_with_delivery_route(
            offers=[_Combinator.Fulfillment_3P_Offer],
            points=[
                _Combinator.Warehouse,
                _Combinator.Movement,
                _Combinator.SortingCenter,
                _Combinator.SecondMovement,
                _Combinator.Linehaul,
                _Combinator.EndPointPost,
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
        )

        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.COURIER,
            destination=_Combinator.EndPointRegion,
            delivery_option=create_delivery_option(
                date_from=_Combinator.DeliveryDateFrom,
                date_to=_Combinator.DeliveryDateTo,
                time_from=_Combinator.DeliveryTimeFrom,
                time_to=_Combinator.DeliveryTimeTo,
            ),
            total_price=_Offers.Fulfillment_3P_Offer.price + _Offers.Fulfillment_3P_Offer_2.price,
        ).respond_with_delivery_route(
            offers=[_Combinator.Fulfillment_3P_Offer, _Combinator.Fulfillment_3P_Offer_2],
            points=[
                _Combinator.Warehouse,
                _Combinator.Movement,
                _Combinator.SortingCenter,
                _Combinator.SecondMovement,
                _Combinator.Linehaul,
                _Combinator.EndPointRegion,
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
                shipment_date_offset={"warehouse_position": 0, "offset": -1},
            ),
            shipment_warehouse=322,
            route_id=None,
        )

        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.COURIER,
            destination=_Combinator.EndPointRegion,
            delivery_option=create_delivery_option(
                date_from=_Combinator.DeliveryDateFrom,
                date_to=_Combinator.DeliveryDateTo,
                time_from=_Combinator.DeliveryTimeFrom,
                time_to=_Combinator.DeliveryTimeTo,
            ),
            total_price=_Offers.UltraLightOffer.price,
        ).respond_with_delivery_route(
            offers=[
                _Combinator.UltraLightOffer,
            ],
            points=[
                _Combinator.Warehouse,
                _Combinator.Movement,
                _Combinator.SortingCenter,
                _Combinator.SecondMovement,
                _Combinator.Linehaul,
                _Combinator.EndPointRegion,
            ],
            paths=_Combinator.SimplePaths,
            date_from=_Combinator.DeliveryDateFrom,
            date_to=_Combinator.DeliveryDateTo,
        )

        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.COURIER,
            destination=_Combinator.EndPointRegion,
            delivery_option=create_delivery_option(
                date_from=_Combinator.DeliveryDateFrom,
                date_to=_Combinator.DeliveryDateTo,
                time_from=_Combinator.DeliveryTimeFrom,
                time_to=_Combinator.DeliveryTimeTo,
            ),
            total_price=_Offers.AviaOffer.price,
        ).respond_with_delivery_route(
            offers=[
                _Combinator.AviaOffer,
            ],
            points=[
                _Combinator.Warehouse,
                _Combinator.Movement,
                _Combinator.SortingCenter,
                _Combinator.SecondMovement,
                _Combinator.Linehaul,
                _Combinator.EndPointRegion,
            ],
            paths=_Combinator.SimplePaths,
            date_from=_Combinator.DeliveryDateFrom,
            date_to=_Combinator.DeliveryDateTo,
            is_airship_delivery=True,
        )

    @classmethod
    def prepare_tariffs(cls):
        # дефолтный прямой тариф
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=[
                BlueDeliveryTariff(
                    for_plus=1, user_price=_Constants.DeliveryPriceBeforeThreshold, large_size=0, price_to=700
                ),
                BlueDeliveryTariff(for_plus=1, user_price=0, large_size=0),
                BlueDeliveryTariff(
                    for_plus=0, user_price=_Constants.DeliveryPriceBeforeThreshold, large_size=0, price_to=700
                ),
                BlueDeliveryTariff(for_plus=0, user_price=_Constants.DeliveryPriceAfterThreshold, large_size=0),
                BlueDeliveryTariff(user_price=_Constants.DeliveryPriceKgt, large_size=1),
            ],
            large_size_weight=20,
        )

        AVIA_TARIFF = [
            BlueDeliveryTariff(user_price=150, for_plus=1),
            BlueDeliveryTariff(user_price=200, for_plus=0),
        ]

        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=AVIA_TARIFF,
            is_avia=True,
        )

    def make_expected_response(
        self,
        delivery_type,
        user_price,
        is_post=False,
        shipment_offset=False,
        nordstream=True,
        with_route_id=True,
    ):
        def make_expected_offers():
            combinator_offers = [_Combinator.Fulfillment_3P_Offer]
            if delivery_type == DeliveryType.COURIER:
                combinator_offers += [_Combinator.Fulfillment_3P_Offer_2]
            return combinator_offers

        def make_expected_points():
            if delivery_type == DeliveryType.PICKUP:
                end_point = _Combinator.EndPointPost if is_post else _Combinator.EndPointPickup
            else:
                end_point = _Combinator.EndPointRegion
            points = [
                _Combinator.Warehouse,
                _Combinator.Movement,
                _Combinator.Linehaul,
                end_point,
            ]
            return points

        def make_expected_supplier_processing():
            reception_by_warehouse = "2020-08-02T00:00:00+03:00"

            return [{"shipmentBySupplier": reception_by_warehouse, "receptionByWarehouse": reception_by_warehouse}]

        tariff_id = 100147

        offers = [_Offers.Fulfillment_3P_Offer]

        if delivery_type == DeliveryType.COURIER:
            offers.append(_Offers.Fulfillment_3P_Offer_2)

        combinator_offers = make_expected_offers()
        route_points = make_expected_points()
        route_paths = _Combinator.SimplePaths
        date_from, date_to = _Combinator.DeliveryDateFrom, _Combinator.DeliveryDateTo

        expected_outlets = Absent()
        if is_post:
            expected_outlets = [route_points[-1].point_ids.logistic_point_id]
        supplier_processing = make_expected_supplier_processing()
        if delivery_type == DeliveryType.COURIER:
            hour_from, minute_from = (10, 0)
            hour_to, minute_to = (22, 30)
            expected_time_intervals = [
                {
                    'from': "{:02}:{:02}".format(hour_from, minute_from),
                    'to': "{:02}:{:02}".format(hour_to, minute_to),
                    'isDefault': True,
                }
            ]
        else:
            expected_time_intervals = EqualToOneOfOrAbsent([{"from": "10:00", "to": "22:30", "isDefault": True}])

        response = {
            "search": {
                "results": [
                    {
                        "entity": "deliveryGroup",
                        "offers": [
                            {
                                "entity": "offer",
                                "wareId": o.waremd5,
                                "fulfillmentWarehouse": 322 if nordstream else NotEmpty(),
                            }
                            for o in offers
                        ],
                        "fakeOffers": EmptyList(),
                        "delivery": {
                            "hasPickup": delivery_type == DeliveryType.PICKUP and not is_post,
                            "hasPost": delivery_type == DeliveryType.PICKUP and is_post,
                            "route": {
                                "offers": [
                                    {
                                        "partner_id": co.partner_id,
                                        "available_count": co.available_count,
                                        "shop_id": co.shop_id,
                                        "shop_sku": co.shop_sku,
                                    }
                                    for co in combinator_offers
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
                                "supplierPrice": Absent(),
                                "supplierDiscount": Absent(),
                                "tariffId": tariff_id,
                                "supplierProcessing": Absent(),
                                "shipmentBySupplier": supplier_processing[0]['shipmentBySupplier'],
                                "receptionByWarehouse": supplier_processing[0]['receptionByWarehouse'],
                                "timeIntervals": expected_time_intervals,
                                "partnerType": "market_delivery",
                                "packagingTime": "PT19H30M" if not shipment_offset else "PT43H30M",
                                "outletIds": expected_outlets,
                                "shipmentDay": 0,
                                "isExternalLogistics": False,
                                "isDsbsToMarketOutlet": False,
                            },
                        },
                    }
                ]
            }
        }
        if with_route_id:
            response["search"]["results"][0]["delivery"]["routeID"] = "aaaa-bbbb-cccc-dddd-8888"

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

    def test_delivery_route_pickup(self):
        for nordstream in [1, 0]:
            response = self.report.request_json(
                _Requests.BaseRequestTemplate.format(
                    rids=_Constants.MskRids,
                    offers='{}:1'.format(_Offers.Fulfillment_3P_Offer.waremd5),
                    type='pickup',
                    type_param='point_id={}'.format(_Outlets.PickupOutlet.point_id),
                    nordstream=nordstream,
                )
            )
            self.assertFragmentIn(
                response,
                self.make_expected_response(
                    delivery_type=DeliveryType.PICKUP,
                    user_price=_Constants.DeliveryPriceBeforeThreshold,
                    nordstream=nordstream,
                ),
            )

    def test_delivery_route_from_route_storage(self):
        for use_route_storage in (True, False):
            # Проверка, что есть/нет поход в RouteStorage при включеном флаге
            response = self.report.request_json(
                _Requests.BaseRequestTemplate.format(
                    rids=_Constants.MskRids,
                    offers='{}:1'.format(_Offers.Fulfillment_3P_Offer.waremd5),
                    type='pickup',
                    type_param='point_id={}'.format(_Outlets.PickupOutlet.point_id),
                    nordstream=1,
                )
                + '&rearr-factors=market_use_route_storage_instead_combinator={};&debug=da'.format(
                    '1' if use_route_storage else '0'
                )
            )
            use_route_storage_log_msg = 'GetDeliveryRoute(): RouteStorage request'
            use_combinator_log_msg = 'GetDeliveryRoute(): Combinator request'
            self.assertFragmentIn(response, use_route_storage_log_msg if use_route_storage else use_combinator_log_msg)
            self.assertFragmentNotIn(
                response, use_combinator_log_msg if use_route_storage else use_route_storage_log_msg
            )

    def date_time_to_string(self, dt, tm):
        return '{year}{month}{day}.{hour}{minute}'.format(
            year=dt.year, month=dt.month, day=dt.day, hour=tm.hour, minute=tm.minute
        )

    def delivery_interval_string(self, date_from, time_from, date_to, time_to):
        return 'delivery-interval={}-{}'.format(
            self.date_time_to_string(date_from, time_from), self.date_time_to_string(date_to, time_to)
        )

    def test_delivery_route_courier(self):
        for nordstream in [1, 0]:
            response = self.report.request_json(
                _Requests.BaseRequestTemplate.format(
                    rids=_Constants.MskRids,
                    offers=','.join(
                        [
                            '{}:1'.format(o.waremd5)
                            for o in [_Offers.Fulfillment_3P_Offer, _Offers.Fulfillment_3P_Offer_2]
                        ]
                    ),
                    type='courier',
                    type_param=self.delivery_interval_string(
                        _Combinator.DeliveryDateFrom,
                        _Combinator.DeliveryTimeFrom,
                        _Combinator.DeliveryDateTo,
                        _Combinator.DeliveryTimeTo,
                    ),
                    nordstream=nordstream,
                )
            )
            self.assertFragmentIn(
                response,
                self.make_expected_response(
                    delivery_type=DeliveryType.COURIER,
                    user_price=_Constants.DeliveryPriceBeforeThreshold,
                    nordstream=nordstream,
                    with_route_id=False,
                ),
            )

    def test_no_valid_route(self):
        '''
        В этом тесте у Комбинатора нет маршрутов с такой датой доставки,
        поэтому возвращается ошибка no suitable route, и поле route не заполняется
        '''
        for client in ('', 'client=stats_collector&'):
            response = self.report.request_json(
                _Requests.BaseRequestTemplate.format(
                    rids=_Constants.MskRids,
                    offers=','.join(
                        [
                            '{}:1'.format(o.waremd5)
                            for o in [_Offers.Fulfillment_3P_Offer, _Offers.Fulfillment_3P_Offer_2]
                        ]
                    ),
                    type='courier',
                    type_param=client
                    + self.delivery_interval_string(
                        _Combinator.DeliveryDateFrom + datetime.timedelta(days=1),
                        _Combinator.DeliveryTimeFrom,
                        _Combinator.DeliveryDateTo,
                        _Combinator.DeliveryTimeTo,
                    ),
                    nordstream=1,
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "commonProblems": ["COMBINATOR_ROUTE_UNAVAILABLE"],
                        "results": [{"entity": "deliveryGroup", "delivery": {"route": Absent()}}],
                    }
                },
            )
        self.error_log.expect(code=ErrorCodes.COMBINATOR_ROUTE_UNAVAILABLE)

    def test_delivery_route_post(self):
        response = self.report.request_json(
            _Requests.BaseRequestTemplate.format(
                rids=_Constants.MskRids,
                offers='{}:1'.format(_Offers.Fulfillment_3P_Offer.waremd5),
                type='post',
                type_param='post-index={}'.format(_Outlets.PostOutlet.post_code),
                nordstream=1,
            )
        )
        self.assertFragmentIn(
            response,
            self.make_expected_response(
                delivery_type=DeliveryType.PICKUP,
                user_price=_Constants.DeliveryPriceBeforeThreshold,
                is_post=True,
            ),
        )

    @classmethod
    def prepare_delivery_route_extra_charge_params(cls):
        cls.index.dynamic_delivery_tariffs += [
            DynamicDeliveryTariff(
                delivery_type=DynamicDeliveryTariff.DeliveryType.COURIER_PARTHER, is_large_size=False
            ),
            DynamicDeliveryTariff(delivery_type=DynamicDeliveryTariff.DeliveryType.COURIER_PARTHER, is_large_size=True),
        ]

    def test_delivery_route_extra_charge_params(self):
        response = self.report.request_json(
            _Requests.BaseRequestTemplate.format(
                rids=_Constants.MskRids,
                offers='{}:1'.format(_Offers.Fulfillment_3P_Offer.waremd5),
                type='post',
                type_param='post-index={}'.format(_Outlets.PostOutlet.post_code),
                nordstream=1,
            )
            + '&rearr-factors=market_dynamic_delivery_tariffs=1'
        )

        self.assertFragmentIn(
            response,
            {
                "extraChargeParameters": {
                    "chargeQuant": "50",
                    "maxCharge": "100",
                    "minCharge": "199",
                    "minChargeOfGmv": "0.005",
                    "vatMultiplier": "1.2",
                    "version": 42,
                },
            },
        )

    def test_delivery_route_extra_charge_ya_plus(self):
        def get_request(offer, plus):
            base = _Requests.BaseRequestTemplate.format(
                rids=_Constants.MskRids,
                offers='{}:1'.format(offer),
                type='courier',
                type_param=self.delivery_interval_string(
                    _Combinator.DeliveryDateFrom,
                    _Combinator.DeliveryTimeFrom,
                    _Combinator.DeliveryDateTo,
                    _Combinator.DeliveryTimeTo,
                ),
                nordstream=1,
            )
            base += '&rearr-factors=market_dynamic_delivery_tariffs=1'
            base += '&debug=da'
            base += '&total-price=3490'
            if plus:
                base += '&perks=yandex_plus'
            return base

        free_delivery_response = {
            "search": {
                "results": [
                    {
                        "delivery": {
                            "option": {
                                "price": {"currency": "RUR", "value": "0"},
                            }
                        }
                    }
                ]
            }
        }

        def extra_response(ue, val, price):
            return {
                "search": {
                    "results": [
                        {
                            "delivery": {
                                "option": {
                                    "price": {"currency": "RUR", "value": str(price)},
                                    "extraCharge": {"unitEconomyValue": str(ue), "value": str(val)},
                                }
                            }
                        }
                    ]
                }
            }

        response = self.report.request_json(get_request(offer=_Offers.Fulfillment_3P_Offer.waremd5, plus=True))

        self.assertFragmentIn(
            response, {"logicTrace": [Contains('Skip extra charge calculation, exceed market plus threshold')]}
        )
        self.assertFragmentIn(
            response,
            free_delivery_response,
        )

        response = self.report.request_json(get_request(offer=_Offers.Fulfillment_3P_Offer.waremd5, plus=False))

        self.assertFragmentIn(
            response,
            extra_response(ue=-109, val=89, price=_Constants.DeliveryPriceAfterThreshold),
        )

        response = self.report.request_json(
            get_request(offer=_Offers.LargeSize_Fulfillment_3P_Offer_Expensive.waremd5, plus=True)
        )

        self.assertFragmentIn(
            response,
            extra_response(ue=-98, val=0, price=_Constants.DeliveryPriceKgt),
        )

    @classmethod
    def prepare_routes_different_from_request_filters(cls):
        vbox = create_virtual_box(
            weight=_Constants.TestVirtualBox.Weight,
            length=_Constants.TestVirtualBox.Length,
            width=_Constants.TestVirtualBox.Width,
            height=_Constants.TestVirtualBox.Height,
        )
        points = [_Combinator.Warehouse, _Combinator.Movement, _Combinator.Linehaul, _Combinator.EndPointRegion]
        paths = [RoutePath(point_from=i, point_to=i + 1) for i in range(len(points) - 1)]

        def delivery_date(day_delta):
            return _Combinator.DeliveryDateFrom + datetime.timedelta(days=day_delta)

        def pickup_delivery_option(case_idx):
            return create_delivery_option()

        def courier_delivery_option(case_idx, replace_time=False):
            ddate = delivery_date(10 + case_idx)
            tm_from = _Combinator.DeliveryTimeFrom.replace(hour=11) if replace_time else _Combinator.DeliveryTimeFrom
            return create_delivery_option(
                date_from=ddate, date_to=ddate, time_from=tm_from, time_to=_Combinator.DeliveryTimeTo
            )

        # [case = 1] delivery_type mismatch
        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.COURIER,
            destination=_Combinator.EndPointRegion,
            delivery_option=courier_delivery_option(1),
            total_price=_Offers.Fulfillment_3P_Offer.price,
        ).respond_with_delivery_route(
            offers=[_Combinator.Fulfillment_3P_Offer],
            points=points,
            paths=paths,
            date_from=delivery_date(11),
            date_to=delivery_date(11),
            virtual_box=vbox,
            delivery_type=DeliveryType.PICKUP,
            shipment_warehouse=322,
        )

        # [cases = 2-4] destination mismatch
        for case_idx, courier, request_destination, route_destination in (
            (2, True, _Combinator.EndPointRegion, Destination(region_id=_Constants.SpbRids)),
            (3, False, _Combinator.EndPointPickup_2, _Combinator.EndPointPickup),
            (4, False, _Combinator.EndPointPostReq_2, _Combinator.EndPointPostReq),
        ):
            route_endpoint = (
                route_destination
                if isinstance(route_destination, RoutePoint)
                else RoutePoint(
                    route_destination,
                    request_destination.segment_id,
                    request_destination.segment_type,
                    [service.as_tuple() for service in request_destination.services],
                )
            )
            cls.combinator.on_delivery_route_request(
                delivery_type=DeliveryType.COURIER if courier else DeliveryType.PICKUP,
                destination=request_destination,
                delivery_option=courier_delivery_option(case_idx) if courier else pickup_delivery_option(case_idx),
                total_price=_Offers.Fulfillment_3P_Offer.price,
            ).respond_with_delivery_route(
                offers=[_Combinator.Fulfillment_3P_Offer],
                points=points[:-1] + [route_endpoint],
                paths=paths,
                date_from=delivery_date(10 + case_idx),
                date_to=delivery_date(10 + case_idx),
                virtual_box=vbox,
                shipment_warehouse=322,
            )

        # [cases = 5-6] delivery_interval mismatch (5 - wrong date, 6 - wrong interval)
        for case_idx in (5, 6):
            cls.combinator.on_delivery_route_request(
                delivery_type=DeliveryType.COURIER,
                destination=_Combinator.EndPointRegion,
                delivery_option=courier_delivery_option(case_idx, replace_time=(case_idx == 6)),
                total_price=_Offers.Fulfillment_3P_Offer.price,
            ).respond_with_delivery_route(
                offers=[_Combinator.Fulfillment_3P_Offer],
                points=points,
                paths=paths,
                date_from=delivery_date(16),
                date_to=delivery_date(16),
                virtual_box=vbox,
                shipment_warehouse=322,
            )

        # case = 7, Bibirevo is Moscow's subregion
        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.COURIER,
            destination=_Combinator.EndPointRegionBibirevo,
            delivery_option=courier_delivery_option(7),
            total_price=_Offers.Fulfillment_3P_Offer.price,
        ).respond_with_delivery_route(
            offers=[_Combinator.Fulfillment_3P_Offer],
            points=points,
            paths=paths,
            date_from=delivery_date(17),
            date_to=delivery_date(17),
            virtual_box=vbox,
            shipment_warehouse=322,
        )

    def check_mismatch_route(self, response, delivery_type):
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "commonProblems": [
                        "COMBINATOR_ROUTE_DOESNT_MEET_SPECIFIED_CONDITIONS",
                        "INVALID_COMBINATOR_ROUTE",
                    ],
                    "results": [
                        {
                            "entity": "deliveryGroup",
                            "offers": [
                                {
                                    "entity": "offer",
                                    "wareId": _Offers.Fulfillment_3P_Offer.waremd5,
                                    "fulfillmentWarehouse": 322,
                                }
                            ],
                            "fakeOffers": EmptyList(),
                            "delivery": {
                                "route": Absent(),  # no need in route if it is invalid
                            },
                        }
                    ],
                }
            },
        )

    def test_route_delivery_type_mismatch(self):
        ddate = _Combinator.DeliveryDateFrom + datetime.timedelta(11)
        self.error_log.expect(code=ErrorCodes.COMBINATOR_ROUTE_FILTERS_MISMATCH)
        self.error_log.expect(code=ErrorCodes.COMBINATOR_INVALID_ROUTE)
        for client in ('', 'client=stats_collector&'):
            response = self.report.request_json(
                _Requests.BaseRequestTemplate.format(
                    rids=_Constants.MskRids,
                    offers='{}:1'.format(_Offers.Fulfillment_3P_Offer.waremd5),
                    type_param=client
                    + self.delivery_interval_string(
                        ddate, _Combinator.DeliveryTimeFrom, ddate, _Combinator.DeliveryTimeTo
                    ),
                    nordstream=1,
                    type='courier',
                )
            )
            self.check_mismatch_route(response, DeliveryType.to_string(DeliveryType.PICKUP))

    def test_route_delivery_interval_mismatch(self):
        self.error_log.expect(code=ErrorCodes.COMBINATOR_ROUTE_FILTERS_MISMATCH).times(2)
        self.error_log.expect(code=ErrorCodes.COMBINATOR_INVALID_ROUTE).times(2)
        for case_idx in (5, 6):
            ddate = _Combinator.DeliveryDateFrom + datetime.timedelta(10 + case_idx)
            tm_from = _Combinator.DeliveryTimeFrom.replace(hour=11) if case_idx == 6 else _Combinator.DeliveryTimeFrom
            for client in ('', 'client=stats_collector&'):
                response = self.report.request_json(
                    _Requests.BaseRequestTemplate.format(
                        rids=_Constants.MskRids,
                        offers='{}:1'.format(_Offers.Fulfillment_3P_Offer.waremd5),
                        type_param=client
                        + self.delivery_interval_string(ddate, tm_from, ddate, _Combinator.DeliveryTimeTo),
                        type='courier',
                        nordstream=1,
                    )
                )
                self.check_mismatch_route(response, DeliveryType.to_string(DeliveryType.COURIER))

    def test_nonexistent_offer(self):
        """Передаём в запрос несуществующий waremd5"""
        invalid_waremd5 = 'NonexistentOffer_____g'
        for client in ('', 'client=stats_collector&'):
            response = self.report.request_json(
                _Requests.BaseRequestTemplate.format(
                    rids=_Constants.MskRids,
                    offers='{}:1'.format(invalid_waremd5),
                    type='courier',
                    type_param=client
                    + self.delivery_interval_string(
                        _Combinator.DeliveryDateFrom,
                        _Combinator.DeliveryTimeFrom,
                        _Combinator.DeliveryDateTo,
                        _Combinator.DeliveryTimeTo,
                    ),
                    nordstream=1,
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 0,
                        "offerProblems": [{"wareId": invalid_waremd5, "problems": ["NONEXISTENT_OFFER"]}],
                        "results": EmptyList(),
                    }
                },
                allow_different_len=False,
            )
        self.error_log.expect(code=ErrorCodes.ACD_NONEXISTENT_OFFER).once()

    def test_route_destination_is_route_destination_subregion(self):
        """
        Если комбинатор возвращает в качестве последнего региона одного из родителей запрошенного региона,
        это не ошибка, валидация должна такой маршрут считать корректным
        """
        courier_date = _Combinator.DeliveryDateFrom + datetime.timedelta(17)
        request_intervals = self.delivery_interval_string(
            courier_date, _Combinator.DeliveryTimeFrom, courier_date, _Combinator.DeliveryTimeTo
        )
        response = self.report.request_json(
            _Requests.BaseRequestTemplate.format(
                rids=_Constants.BibirevoRids,
                offers='{}:1'.format(_Offers.Fulfillment_3P_Offer.waremd5),
                type_param=request_intervals,
                type='courier',
                nordstream=1,
            )
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "commonProblems": EmptyList(),
                    "results": [
                        {
                            "entity": "deliveryGroup",
                            "offers": [
                                {
                                    "entity": "offer",
                                    "wareId": _Offers.Fulfillment_3P_Offer.waremd5,
                                    "fulfillmentWarehouse": 322,
                                }
                            ],
                            "fakeOffers": EmptyList(),
                            "delivery": {
                                "route": {
                                    "route": {
                                        "delivery_type": 'COURIER',
                                        "points": [
                                            {"ids": {"partner_id": 222}},
                                            {"ids": {"partner_id": 157}},
                                            {"ids": {"partner_id": 157}},
                                            {"ids": {"region_id": _Constants.MskRids}},
                                        ],
                                    }
                                }
                            },
                        }
                    ],
                }
            },
        )
        self.error_log.expect(code=ErrorCodes.COMBINATOR_ROUTE_FILTERS_MISMATCH).never()

    def test_offer_without_post_term_flag(self):
        """Проверка того, что delivery_route умеет отдавать доставку для офферов, которые фильтруются в actual_delivery"""
        delivery_template = (
            'place=actual_delivery'
            '&rids=213'
            '&offers-list={}:1'
            '&pickup-options=grouped'
            '&pickup-options-extended-grouping=1'
            '&combinator=0'
        )

        for offer, has_post_term_delivery in (
            (_Offers.Dropship_Offer, True),
            (_Offers.DropshipOfferNoPostTermFlag, False),
        ):
            # без указания конкретной точки в параметрах получаем доставку как обычно
            response = self.report.request_json(delivery_template.format(offer.waremd5))
            self.assertFragmentIn(
                response,
                {
                    "commonProblems": EmptyList(),
                    "results": [
                        {
                            "entity": "deliveryGroup",
                            "offers": [
                                {
                                    "entity": "offer",
                                    "wareId": offer.waremd5,
                                }
                            ],
                            "fakeOffers": EmptyList(),
                            "delivery": {
                                "options": [{"serviceId": str(_Constants.PostTermDeliveryServiceId)}],
                                "pickupOptions": [
                                    {
                                        "serviceId": _Constants.PostTermDeliveryServiceId,
                                        "outletIds": [_Outlets.PostTermOutlet.point_id],
                                    }
                                ],
                            },
                        }
                    ],
                },
            )

            # при указании конкретной точки оффер без флага post_term перестаёт находиться репортом (NONEXISTENT_OFFER)
            pt = '&point_id=' + str(_Outlets.PostTermOutlet.point_id)
            response = self.report.request_json(delivery_template.format(offer.waremd5) + pt)
            self.assertFragmentIn(
                response,
                {
                    "offerProblems": EmptyList()
                    if has_post_term_delivery
                    else [{"wareId": offer.waremd5, "problems": ["NONEXISTENT_OFFER"]}],
                    "results": EmptyList()
                    if not has_post_term_delivery
                    else [
                        {
                            "entity": "deliveryGroup",
                            "offers": [
                                {
                                    "entity": "offer",
                                    "wareId": offer.waremd5,
                                }
                            ],
                            "fakeOffers": EmptyList(),
                            "delivery": {
                                "pickupOptions": [
                                    {
                                        "serviceId": _Constants.PostTermDeliveryServiceId,
                                        "outletIds": [_Outlets.PostTermOutlet.point_id],
                                    }
                                ]
                            },
                        }
                    ],
                },
            )

            # delivery_route видит оба оффера при указании точки, но не находит маршрут (т.к. его нет в моке)
            route_template = (
                'place=delivery_route' '&delivery-type=pickup' '&rids=213' '&client=stats_collector' '&offers-list={}:1'
            )

            response = self.report.request_json(route_template.format(offer.waremd5) + pt)
            self.assertFragmentIn(
                response,
                {
                    "offerProblems": EmptyList(),
                    "commonProblems": ["COMBINATOR_ROUTE_UNAVAILABLE"],
                    "results": [
                        {
                            "entity": "deliveryGroup",
                            "offers": [{"entity": "offer", "wareId": offer.waremd5}],
                            "fakeOffers": EmptyList(),
                            "delivery": {"pickupOptions": Absent()},
                        }
                    ],
                },
            )

    @classmethod
    def prepare_long_movement(cls):
        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.COURIER,
            destination=_Combinator.LongMovementEndPoint,
            delivery_option=create_delivery_option(
                date_from=_Combinator.LongMovementDateFrom,
                date_to=_Combinator.DeliveryDateTo,
                time_from=_Combinator.DeliveryTimeFrom,
                time_to=_Combinator.ConsolidatedDeliveryTimeTo,
            ),
            total_price=_Offers.Fulfillment_3P_Offer.price,
        ).respond_with_delivery_route(
            offers=[_Combinator.Fulfillment_3P_Offer],
            points=[_Combinator.Warehouse, _Combinator.LongMovement, _Combinator.LongMovementEndPoint],
            paths=[RoutePath(point_from=i, point_to=i + 1) for i in range(2)],
            date_from=_Combinator.LongMovementDateFrom,
            date_to=_Combinator.DeliveryDateTo,
            virtual_box=create_virtual_box(
                weight=_Constants.TestVirtualBox.Weight,
                length=_Constants.TestVirtualBox.Length,
                width=_Constants.TestVirtualBox.Width,
                height=_Constants.TestVirtualBox.Height,
            ),
            string_delivery_dates=create_string_delivery_dates(
                shipment_date="2020-08-03T00:00:00+03:00",
                packaging_time="PT10H30M",
                shipment_by_supplier="2020-08-03T00:00:00+03:00",
                reception_by_warehouse="2020-08-03T00:00:00+03:00",
            ),
            shipment_warehouse=322,
        )

    def test_long_movement(self):
        response = self.report.request_json(
            _Requests.BaseRequestTemplate.format(
                rids=_Constants.SpbRids,
                offers='{}:1'.format(_Offers.Fulfillment_3P_Offer.waremd5),
                type='courier',
                type_param=self.delivery_interval_string(
                    _Combinator.LongMovementDateFrom,
                    _Combinator.DeliveryTimeFrom,
                    _Combinator.DeliveryDateTo,
                    _Combinator.ConsolidatedDeliveryTimeTo,
                ),
                nordstream=1,
            )
        )
        shipmentTime = '2020-08-03T00:00:00+03:00'
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "deliveryGroup",
                            "offers": [
                                {
                                    "entity": "offer",
                                    "wareId": _Offers.Fulfillment_3P_Offer.waremd5,
                                    "fulfillmentWarehouse": 322,
                                }
                            ],
                            "fakeOffers": EmptyList(),
                            "delivery": {
                                "route": {"route": {"delivery_type": 'COURIER', "points": ElementCount(3)}},
                                "option": {
                                    "shipmentBySupplier": shipmentTime,
                                    "receptionByWarehouse": shipmentTime,
                                    "shipmentDay": 0,
                                    "dayFrom": _Constants.Today + 1,
                                    "dayTo": _Constants.Today + 10,
                                    "packagingTime": "PT10H30M",
                                },
                            },
                        }
                    ]
                }
            },
        )

    @classmethod
    def prepare_dropship_via_sc_intake(cls):
        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.COURIER,
            destination=_Combinator.DropshipIntakeEndPoint,
            delivery_option=create_delivery_option(
                date_from=_Combinator.DeliveryDateFrom,
                date_to=_Combinator.DeliveryDateTo,
                time_from=_Combinator.DeliveryTimeFrom,
                time_to=_Combinator.ConsolidatedDeliveryTimeTo,
            ),
            total_price=_Offers.Dropship_Offer.price,
        ).respond_with_delivery_route(
            offers=[_Combinator.Dropship_Offer],
            points=[
                _Combinator.DropshipWH,
                _Combinator.Movement,
                _Combinator.SortingCenter,
                _Combinator.SecondMovement,
                _Combinator.Linehaul,
                _Combinator.DropshipIntakeEndPoint,
            ],
            paths=[RoutePath(point_from=i, point_to=i + 1) for i in range(5)],
            date_from=_Combinator.DeliveryDateFrom,
            date_to=_Combinator.DeliveryDateTo,
            virtual_box=create_virtual_box(
                weight=_Constants.TestVirtualBox.Weight,
                length=_Constants.TestVirtualBox.Length,
                width=_Constants.TestVirtualBox.Width,
                height=_Constants.TestVirtualBox.Height,
            ),
            string_delivery_dates=create_string_delivery_dates(
                shipment_date="2020-08-02T20:40:00+03:00",
                packaging_time="PT20H50M",
                shipment_by_supplier="2020-08-02T19:00:00+03:00",
                reception_by_warehouse="2020-08-02T20:40:00+03:00",
                supplier_processing=[
                    {
                        "warehouse_id": _Shops.DropshipShop.warehouse_id,
                        "processing_start_time": "2020-08-02T16:25:00+03:00",
                        "shipment_by_supplier": "2020-08-02T19:00:00+03:00",
                        "shipment_date_time": "2020-08-02T20:40:00+03:00",
                        "reception_by_warehouse": "2020-08-02T20:40:00+03:00",
                    }
                ],
            ),
            shipment_warehouse=322,
        )

    def test_dropship_via_sc_intake(self):
        '''
        Схема дропшип через СЦ случай забора, т.е.
        мы отвечаем за доставку товара со склада поставщика на наш склад (иди СЦ)
        '''
        response = self.report.request_json(
            _Requests.BaseRequestTemplate.format(
                rids=_Constants.OtherRids,
                offers='{}:1'.format(_Offers.Dropship_Offer.waremd5),
                type='courier',
                type_param=self.delivery_interval_string(
                    _Combinator.DeliveryDateFrom,
                    _Combinator.DeliveryTimeFrom,
                    _Combinator.DeliveryDateTo,
                    _Combinator.ConsolidatedDeliveryTimeTo,
                ),
                nordstream=1,
            )
        )
        startDateTime = '2020-08-02T16:25:00+03:00'
        # End of shipment service in warehouse segment (supplier warehouse)
        shipmentBySupplier = '2020-08-02T19:00:00+03:00'
        # Processing start_time for shipment warehouse (here for SC)
        receptionByWarehouse = '2020-08-02T20:40:00+03:00'
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "deliveryGroup",
                            "offers": [
                                {
                                    "entity": "offer",
                                    "wareId": _Offers.Dropship_Offer.waremd5,
                                    "fulfillmentWarehouse": 322,
                                }
                            ],
                            "fakeOffers": EmptyList(),
                            "delivery": {
                                "route": {"route": {"delivery_type": 'COURIER', "points": ElementCount(6)}},
                                "option": {
                                    "supplierProcessing": [
                                        {
                                            "startDateTime": startDateTime,
                                            "shipmentBySupplier": shipmentBySupplier,
                                            "receptionByWarehouse": receptionByWarehouse,
                                            "shipmentDateTime": receptionByWarehouse,  # (deprecated) equal to receptionByWarehouse
                                            "warehouseId": _Shops.DropshipShop.warehouse_id,
                                        }
                                    ],
                                    "shipmentBySupplier": shipmentBySupplier,
                                    "receptionByWarehouse": receptionByWarehouse,
                                    "shipmentDay": 0,
                                    "dayFrom": _Constants.Today,
                                    "dayTo": _Constants.Today + 10,
                                    "packagingTime": "PT20H50M",
                                },
                            },
                        }
                    ]
                }
            },
        )

    @classmethod
    def prepare_dropship_via_sc_selfshipment(cls):
        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.COURIER,
            destination=_Combinator.DropshipSelfshipmentEndPoint,
            delivery_option=create_delivery_option(
                date_from=_Combinator.DeliveryDateFrom,
                date_to=_Combinator.DeliveryDateTo,
                time_from=_Combinator.DeliveryTimeFrom,
                time_to=_Combinator.ConsolidatedDeliveryTimeTo,
            ),
            total_price=_Offers.Dropship_Offer.price,
        ).respond_with_delivery_route(
            offers=[_Combinator.Dropship_Offer],
            points=[
                _Combinator.DropshipWH,
                _Combinator.SelfMovement,
                _Combinator.SortingCenter,
                _Combinator.SecondMovement,
                _Combinator.Linehaul,
                _Combinator.DropshipSelfshipmentEndPoint,
            ],
            paths=[RoutePath(point_from=i, point_to=i + 1) for i in range(5)],
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
                packaging_time="PT20H50M",
                shipment_by_supplier="2020-08-02T20:30:00+03:00",
                reception_by_warehouse="2020-08-02T20:40:00+03:00",
                supplier_processing=[
                    {
                        "warehouse_id": _Shops.DropshipShop.warehouse_id,
                        "processing_start_time": "2020-08-02T16:25:00+03:00",
                        "shipment_by_supplier": "2020-08-02T20:30:00+03:00",
                        "shipment_date_time": "2020-08-02T20:40:00+03:00",
                        "reception_by_warehouse": "2020-08-02T20:40:00+03:00",
                    }
                ],
            ),
            shipment_warehouse=322,
        )

    def test_dropship_via_sc_selfshipment(self):
        '''
        Схема дропшип через СЦ случай самопривоза, т.е.
        партнёр сам отвечает за доставку товара со склада поставщика на наш склад (или СЦ)
        '''
        response = self.report.request_json(
            _Requests.BaseRequestTemplate.format(
                rids=_Constants.OtherRids2,
                offers='{}:1'.format(_Offers.Dropship_Offer.waremd5),
                type='courier',
                type_param=self.delivery_interval_string(
                    _Combinator.DeliveryDateFrom,
                    _Combinator.DeliveryTimeFrom,
                    _Combinator.DeliveryDateTo,
                    _Combinator.ConsolidatedDeliveryTimeTo,
                ),
                nordstream=1,
            )
        )
        startDateTime = '2020-08-02T16:25:00+03:00'
        # End of shipment service in movement segment (after supplier warehouse)
        shipmentBySupplier = '2020-08-02T20:30:00+03:00'
        # Processing start_time for shipment warehouse (here for SC)
        receptionByWarehouse = '2020-08-02T20:40:00+03:00'
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "deliveryGroup",
                            "offers": [
                                {
                                    "entity": "offer",
                                    "wareId": _Offers.Dropship_Offer.waremd5,
                                    "fulfillmentWarehouse": 322,
                                }
                            ],
                            "fakeOffers": EmptyList(),
                            "delivery": {
                                "route": {"route": {"delivery_type": 'COURIER', "points": ElementCount(6)}},
                                "option": {
                                    "supplierProcessing": [
                                        {
                                            "startDateTime": startDateTime,
                                            "shipmentBySupplier": shipmentBySupplier,
                                            "receptionByWarehouse": receptionByWarehouse,
                                            "shipmentDateTime": receptionByWarehouse,  # (deprecated) equal to receptionByWarehouse
                                            "warehouseId": _Shops.DropshipShop.warehouse_id,
                                        }
                                    ],
                                    "shipmentBySupplier": shipmentBySupplier,
                                    "receptionByWarehouse": receptionByWarehouse,
                                    "shipmentDay": 0,
                                    "dayFrom": _Constants.Today,
                                    "dayTo": _Constants.Today + 10,
                                    "packagingTime": "PT20H50M",
                                },
                            },
                        }
                    ]
                }
            },
        )

    def test_ultralight_item(self):
        test_request = _Requests.BaseRequestTemplate.format(
            rids=_Constants.MskRids,
            offers='{}:1'.format(_Offers.UltraLightOffer.waremd5),
            type='courier',
            type_param=self.delivery_interval_string(
                _Combinator.DeliveryDateFrom,
                _Combinator.DeliveryTimeFrom,
                _Combinator.DeliveryDateTo,
                _Combinator.DeliveryTimeTo,
            ),
            nordstream=1,
        )
        test_request += '&debug=1'

        response = self.report.request_json(test_request)
        weightRounded = "{:.3f}".format(math.ceil(_Offers.UltraLightOffer.weight * 1000) / 1000)
        # включить после переключения mmap версий
        self.assertFragmentIn(response, {"search": {"results": [{"offers": [{"weight": weightRounded}]}]}})
        self.assertFragmentIn(response, {"logicTrace": [Contains("weight: 1")]})

    def test_force_delivery_id(self):
        '''
        Проверка пробрасывания параметра force-delivery-id в комбинатор
        '''
        request = _Requests.BaseRequestTemplate.format(
            rids=_Constants.MskRids,
            type='courier',
            offers='{}:1'.format(_Offers.Fulfillment_3P_Offer.waremd5),
            type_param=self.delivery_interval_string(
                _Combinator.DeliveryDateFrom,
                _Combinator.DeliveryTimeFrom,
                _Combinator.DeliveryDateTo,
                _Combinator.DeliveryTimeTo,
            ),
            nordstream=1,
        )
        request += '&force-delivery-id={}'.format(_Constants.DeliveryServiceId)
        request += '&debug=1'

        expected_trace = {
            "logicTrace": [
                r"\[ME\].*? GetDeliveryRoute\(\): Combinator request:.*? option \{.*? "
                + r"delivery_service_id: {}".format(_Constants.DeliveryServiceId)
                + r".*?\}"
            ]
        }
        self.error_log.expect(code=ErrorCodes.COMBINATOR_ROUTE_UNAVAILABLE)
        self.assertFragmentIn(self.report.request_json(request), expected_trace, use_regex=True)

    def test_rearr_factors(self):
        '''
        Проверка пробрасывания rearr-factors в комбинатор
        '''
        request = _Requests.BaseRequestTemplate.format(
            rids=_Constants.MskRids,
            offers='{}:1'.format(_Offers.UltraLightOffer.waremd5),
            type='courier',
            type_param=self.delivery_interval_string(
                _Combinator.DeliveryDateFrom,
                _Combinator.DeliveryTimeFrom,
                _Combinator.DeliveryDateTo,
                _Combinator.DeliveryTimeTo,
            ),
            nordstream=1,
        )
        request += '&debug=1'

        expected_trace = {
            "logicTrace": [
                r"\[ME\].*? GetDeliveryRoute\(\): Combinator request:.*? option \{.*?rearr_factors:.*"
                r"market_nordstream=1;"
                r"parallel_smm=1.0;"
                r"ext_snippet=1;"
                r"no_snippet_arc=1"
            ]
        }
        self.assertFragmentIn(self.report.request_json(request), expected_trace, use_regex=True)

    @classmethod
    def prepare_test_delivery_price(cls):
        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.COURIER,
            destination=_Combinator.EndPointRegion,
            delivery_option=create_delivery_option(
                date_from=_Combinator.DeliveryDateFrom,
                date_to=_Combinator.DeliveryDateTo,
                time_from=_Combinator.DeliveryTimeFrom,
                time_to=_Combinator.DeliveryTimeTo,
            ),
            total_price=_Offers.Fulfillment_3P_Offer_Expensive.price,
        ).respond_with_delivery_route(
            offers=[_Combinator.Fulfillment_3P_Offer_Expensive],
            points=[
                _Combinator.Warehouse,
                _Combinator.Movement,
                _Combinator.SortingCenter,
                _Combinator.SecondMovement,
                _Combinator.Linehaul,
                _Combinator.EndPointRegion,
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
                reception_by_warehouse="2020-08-01T00:00:00+03:00",
                shipment_date_offset={"warehouse_position": 0, "offset": -1},
            ),
            shipment_warehouse=322,
        )

    def test_delivery_price(self):
        '''
        Проверяется, что цена доставки после превышения порогового значения,
        установленного в тарифах, уменьшается
        '''
        response = self.report.request_json(
            _Requests.BaseRequestTemplate.format(
                rids=_Constants.MskRids,
                offers=','.join(['{}:1'.format(o.waremd5) for o in [_Offers.Fulfillment_3P_Offer_Expensive]]),
                type='courier',
                type_param=self.delivery_interval_string(
                    _Combinator.DeliveryDateFrom,
                    _Combinator.DeliveryTimeFrom,
                    _Combinator.DeliveryDateTo,
                    _Combinator.DeliveryTimeTo,
                ),
                nordstream=1,
            )
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "delivery": {
                                "option": {
                                    "price": {"currency": "RUR", "value": str(_Constants.DeliveryPriceAfterThreshold)},
                                }
                            }
                        }
                    ]
                }
            },
        )

    @classmethod
    def prepare_test_kgt_delivery_price(cls):
        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.COURIER,
            destination=_Combinator.EndPointRegion,
            delivery_option=create_delivery_option(
                date_from=_Combinator.DeliveryDateFrom,
                date_to=_Combinator.DeliveryDateTo,
                time_from=_Combinator.DeliveryTimeFrom,
                time_to=_Combinator.DeliveryTimeTo,
            ),
            total_price=_Offers.Fulfillment_3P_Offer_Expensive.price + _Offers.Fulfillment_3P_Offer_2.price,
        ).respond_with_delivery_route(
            offers=[_Combinator.Fulfillment_3P_Offer_Expensive, _Combinator.Fulfillment_3P_Offer_2],
            points=[
                _Combinator.Warehouse,
                _Combinator.Movement,
                _Combinator.SortingCenter,
                _Combinator.SecondMovement,
                _Combinator.Linehaul,
                _Combinator.EndPointRegion,
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
                reception_by_warehouse="2020-08-01T00:00:00+03:00",
                shipment_date_offset={"warehouse_position": 0, "offset": -1},
            ),
            shipment_warehouse=322,
        )

    def test_kgt_delivery_price(self):
        '''
        Проверяется, что несмотря на превышения порогового значения цены,
        цена доставки вычисляется по тарифам КГТ
        '''
        response = self.report.request_json(
            _Requests.BaseRequestTemplate.format(
                rids=_Constants.MskRids,
                offers=','.join(
                    [
                        '{}:1'.format(o.waremd5)
                        for o in [_Offers.Fulfillment_3P_Offer_Expensive, _Offers.Fulfillment_3P_Offer_2]
                    ]
                ),
                type='courier',
                type_param=self.delivery_interval_string(
                    _Combinator.DeliveryDateFrom,
                    _Combinator.DeliveryTimeFrom,
                    _Combinator.DeliveryDateTo,
                    _Combinator.DeliveryTimeTo,
                ),
                nordstream=1,
            )
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "delivery": {
                                "option": {
                                    "price": {"currency": "RUR", "value": str(_Constants.DeliveryPriceKgt)},
                                }
                            }
                        }
                    ]
                }
            },
        )

    @classmethod
    def prepare_test_delivery_group_large_size(cls):
        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.COURIER,
            destination=_Combinator.EndPointRegion,
            delivery_option=create_delivery_option(
                date_from=_Combinator.DeliveryDateFrom,
                date_to=_Combinator.DeliveryDateTo,
                time_from=_Combinator.DeliveryTimeFrom,
                time_to=_Combinator.DeliveryTimeTo,
            ),
            total_price=_Offers.LargeSize_Fulfillment_3P_Offer_Expensive.price,
        ).respond_with_delivery_route(
            offers=[_Combinator.LargeSize_Fulfillment_3P_Offer_Expensive],
            points=[
                _Combinator.Warehouse,
                _Combinator.Movement,
                _Combinator.SortingCenter,
                _Combinator.SecondMovement,
                _Combinator.Linehaul,
                _Combinator.EndPointRegion,
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
            shipment_warehouse=322,
        )

        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.COURIER,
            destination=_Combinator.EndPointRegion,
            delivery_option=create_delivery_option(
                date_from=_Combinator.DeliveryDateFrom,
                date_to=_Combinator.DeliveryDateTo,
                time_from=_Combinator.DeliveryTimeFrom,
                time_to=_Combinator.DeliveryTimeTo,
            ),
            total_price=_Offers.Fulfillment_3P_Offer_Expensive.price * 2,
        ).respond_with_delivery_route(
            offers=[_Combinator.Fulfillment_3P_Offer_Expensive] * 2,
            points=[
                _Combinator.Warehouse,
                _Combinator.Movement,
                _Combinator.SortingCenter,
                _Combinator.SecondMovement,
                _Combinator.Linehaul,
                _Combinator.EndPointRegion,
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
            shipment_warehouse=322,
        )

    def test_delivery_group_large_size(self):
        """
        Place delivery_route:
        для КГТ посылки (объект deliveryGroup в ответе) корректно устанавливается значение largeSize
        """

        def check_large_size(response, is_parcel_large_size, is_offer_large_size):
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [{"largeSize": is_parcel_large_size, "offers": [{"largeSize": is_offer_large_size}]}]
                    }
                },
            )

        # КГТ
        response = self.report.request_json(
            _Requests.BaseRequestTemplate.format(
                rids=_Constants.MskRids,
                offers=_Offers.LargeSize_Fulfillment_3P_Offer_Expensive.waremd5 + ':1',
                type='courier',
                type_param=self.delivery_interval_string(
                    _Combinator.DeliveryDateFrom,
                    _Combinator.DeliveryTimeFrom,
                    _Combinator.DeliveryDateTo,
                    _Combinator.DeliveryTimeTo,
                ),
                nordstream=1,
            )
        )
        check_large_size(response, True, True)
        # Не КГТ
        response = self.report.request_json(
            _Requests.BaseRequestTemplate.format(
                rids=_Constants.MskRids,
                offers=_Offers.Fulfillment_3P_Offer_Expensive.waremd5 + ':1',
                type='courier',
                type_param=self.delivery_interval_string(
                    _Combinator.DeliveryDateFrom,
                    _Combinator.DeliveryTimeFrom,
                    _Combinator.DeliveryDateTo,
                    _Combinator.DeliveryTimeTo,
                ),
                nordstream=1,
            )
        )
        check_large_size(response, False, False)
        # Не КГТ оффер, но посылка - КГТ, т.к. суммарно вес превышает порог
        response = self.report.request_json(
            _Requests.BaseRequestTemplate.format(
                rids=_Constants.MskRids,
                offers=_Offers.Fulfillment_3P_Offer_Expensive.waremd5 + ':2',
                type='courier',
                type_param=self.delivery_interval_string(
                    _Combinator.DeliveryDateFrom,
                    _Combinator.DeliveryTimeFrom,
                    _Combinator.DeliveryDateTo,
                    _Combinator.DeliveryTimeTo,
                ),
                nordstream=1,
            )
        )
        check_large_size(response, True, False)

    def test_pickup_time_interval(self):
        '''
        Проверяется, что под флагом enable_outlet_time_interval возвращается интервал доставки в аутлет
        '''
        for enable_interval in [1, 0]:
            response = self.report.request_json(
                _Requests.BaseRequestTemplate.format(
                    rids=_Constants.MskRids,
                    offers='{}:1'.format(_Offers.Fulfillment_3P_Offer.waremd5),
                    type='pickup',
                    type_param='point_id={}'.format(_Outlets.PickupOutlet.point_id),
                    nordstream=1,
                )
                + ('&rearr-factors=enable_outlet_time_interval={}'.format(enable_interval))
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "delivery": {
                                    "option": {
                                        "timeIntervals": [
                                            {
                                                "from": "10:00",
                                                "to": "22:30",
                                            }
                                        ]
                                        if enable_interval == 1
                                        else Absent()
                                    }
                                }
                            }
                        ]
                    }
                },
            )

    def test_delivery_route_return(self):
        '''
        Проверяется, что с параметром is-return и под флагом enable_dropship_return_through_market_outlets
        репорт ходит в возвратную ручку комбинатора
        '''
        for is_return in [0, 1]:
            for flag in [0, 1]:
                response = self.report.request_json(
                    _Requests.BaseRequestTemplate.format(
                        rids=_Constants.MskRids,
                        offers='{}:1'.format(_Offers.Fulfillment_3P_Offer.waremd5),
                        type='pickup',
                        type_param='point_id={}'.format(_Outlets.PickupOutlet.point_id),
                        nordstream=1,
                    )
                    + (
                        '&is-return={}&rearr-factors=enable_dropship_return_through_market_outlets={}'.format(
                            is_return, flag
                        )
                    )
                )
                self.assertFragmentIn(
                    response,
                    {
                        "search": {
                            "results": [
                                {
                                    "entity": "deliveryGroup",
                                    "offers": [
                                        {
                                            "entity": "offer",
                                            "wareId": _Offers.Fulfillment_3P_Offer.waremd5,
                                            "fulfillmentWarehouse": 323
                                            if is_return and flag
                                            else 322,  # мок возвратной ручки отличается от мока обычной номером склада
                                        }
                                    ],
                                }
                            ],
                        }
                    },
                )

    def test_is_market_courier_and_is_market_branded(self):
        '''
        Проверяется корректное прокидывание флага собственной курьевки isMarketCourier от комбинатора до выдачи,
        а также проставление флага брендированности аутлета isMarketBranded
        '''
        for offer, outlet, is_market_courier_delivery, is_market_branded in [
            (_Offers.Fulfillment_3P_Offer, _Outlets.PickupOutlet, False, False),
            (_Offers.Fulfillment_3P_Offer_2, _Outlets.PickupOutletMarketBranded, True, True),
        ]:
            response = self.report.request_json(
                _Requests.BaseRequestTemplate.format(
                    rids=_Constants.MskRids,
                    offers='{}:1'.format(offer.waremd5),
                    type='pickup',
                    type_param='point_id={}'.format(outlet.point_id),
                    nordstream=1,
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "entity": "deliveryGroup",
                                "delivery": {
                                    "option": {
                                        "isMarketBranded": is_market_branded,
                                    }
                                },
                            }
                        ],
                    }
                },
            )

    @classmethod
    def prepare_partial_delivery_enabled(cls):
        for disable_partial_delivery in [False, True]:
            cls.combinator.on_delivery_route_request(
                delivery_type=DeliveryType.COURIER,
                destination=_Combinator.EndPointRegion,
                delivery_option=create_delivery_option(
                    date_from=_Combinator.DeliveryDateFrom,
                    date_to=_Combinator.DeliveryDateTo,
                    time_from=_Combinator.DeliveryTimeFrom,
                    time_to=_Combinator.DeliveryTimeTo,
                ),
                total_price=_Offers.UltraLightOffer.price,
                disable_partial_delivery=disable_partial_delivery,
            ).respond_with_delivery_route(
                offers=[_Combinator.UltraLightOffer],
                points=[
                    _Combinator.Warehouse,
                    _Combinator.Movement,
                    _Combinator.SortingCenter,
                    _Combinator.EndPointRegion,
                ]
                if disable_partial_delivery
                else [
                    _Combinator.Warehouse,
                    _Combinator.EndPointRegion,
                ],
                paths=_Combinator.SimplePaths if disable_partial_delivery else [RoutePath(point_from=0, point_to=1)],
                date_from=_Combinator.DeliveryDateFrom,
                date_to=_Combinator.DeliveryDateTo,
                virtual_box=create_virtual_box(
                    weight=_Constants.TestVirtualBox.Weight,
                    length=_Constants.TestVirtualBox.Length,
                    width=_Constants.TestVirtualBox.Width,
                    height=_Constants.TestVirtualBox.Height,
                ),
                shipment_warehouse=300 if disable_partial_delivery else 333,
            )

    def test_partial_delivery_enabled(self):
        '''
        Проверяем что cgi-параметр partial-delivery-enabled
        передается из репорта в ручку комбинатора GetDeliveryRoute
        '''
        offer = _Offers.UltraLightOffer
        for partial_delivery_enabled in [0, 1]:
            request_template = (
                'place=delivery_route'
                '&delivery-type=courier'
                '&rids={}'
                '&client=stats_collector'
                '&offers-list={}:1'
                '&partial-delivery-enabled={}'
                '&rearr-factors='
                '&delivery-interval=2020802.1000-2020812.2230'
            )
            response = self.report.request_json(
                request_template.format(_Constants.MskRids, offer.waremd5, partial_delivery_enabled)
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "entity": "deliveryGroup",
                                "offers": [
                                    {
                                        "entity": "offer",
                                        "wareId": offer.waremd5,
                                        "fulfillmentWarehouse": 333 if partial_delivery_enabled else 300,
                                    }
                                ],
                                "fakeOffers": EmptyList(),
                                "delivery": {
                                    "route": {
                                        "route": {
                                            "delivery_type": 'COURIER',
                                            "points": ElementCount(2 if partial_delivery_enabled else 4),
                                        }
                                    }
                                },
                            }
                        ],
                    }
                },
            )

    def test_avia_delivery(self):
        '''
        Проверяем корректный расчет стоимости авиадоставки
        '''
        for avia_delivery in [0, 1]:
            for ya_plus_perk in [True, False]:
                request = _Requests.BaseRequestTemplate.format(
                    rids=_Constants.MskRids,
                    offers='{}:1'.format(_Offers.AviaOffer.waremd5),
                    type='courier',
                    type_param=self.delivery_interval_string(
                        _Combinator.DeliveryDateFrom,
                        _Combinator.DeliveryTimeFrom,
                        _Combinator.DeliveryDateTo,
                        _Combinator.DeliveryTimeTo,
                    ),
                    nordstream=1,
                )
                request += '&debug=1&rearr-factors=avia_delivery={}&perks={}'.format(
                    avia_delivery, "yandex_plus" if ya_plus_perk else ""
                )
                response = self.report.request_json(request)
                if avia_delivery:
                    self.assertFragmentIn(
                        response,
                        {
                            "search": {
                                "results": [
                                    {
                                        "entity": "deliveryGroup",
                                        "offers": [
                                            {
                                                "entity": "offer",
                                                "wareId": _Offers.AviaOffer.waremd5,
                                            }
                                        ],
                                        "fakeOffers": EmptyList(),
                                        "delivery": {
                                            "option": {
                                                "price": {
                                                    "currency": "RUR",
                                                    "value": "150" if ya_plus_perk else "200",
                                                },
                                            },
                                        },
                                    }
                                ],
                            }
                        },
                    )
                else:
                    # не отдаем путь, если нет экспериментального флага, но от комбинатора пришла опция авиа,
                    self.error_log.expect(
                        code=ErrorCodes.COMBINATOR_INVALID_ROUTE,
                        message="Error: Courier delivery price is not calculated!",
                    )

    @classmethod
    def prepare_express_delivery_with_wide_courier_option(cls):
        cls.dynamic.lms += [
            DynamicWarehousesPriorityInRegion(
                region=_Constants.MskRids,
                warehouses=[_Shops.ExpressShop.warehouse_id],
            ),
            DynamicWarehouseInfo(id=_Shops.ExpressShop.warehouse_id, home_region=_Constants.MskRids, is_express=True),
        ]

        cls.dynamic.nordstream += [
            DynamicWarehouseLink(_Shops.ExpressShop.warehouse_id, [_Shops.ExpressShop.warehouse_id]),
            DynamicWarehouseDelivery(
                _Shops.ExpressShop.warehouse_id,
                {
                    _Constants.MskRids: [
                        DynamicDeliveryRestriction(
                            max_phys_weight=100000,
                            max_dim_sum=300,
                            max_dimensions=[100, 100, 100],
                            min_days=0,
                            max_days=4,
                        ),
                    ],
                },
            ),
        ]

        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.COURIER,
            destination=RoutePoint(
                point_ids=Destination(region_id=_Constants.MskRids, gps_coords=_Gps.location),
                segment_id=512005,
                segment_type="handing",
                services=(
                    (
                        DeliveryService.OUTBOUND,
                        "HANDING",
                        service_time(20, 5),
                        datetime.timedelta(minutes=15),
                        (Time(hour=10), Time(hour=22, minute=30)),
                    ),
                ),
            ),
            delivery_option=create_delivery_option(
                date_from=_Combinator.DeliveryDateFrom,
                date_to=_Combinator.DeliveryDateTo,
                time_from=_Combinator.DeliveryTimeFrom,
                time_to=_Combinator.DeliveryTimeTo,
            ),
            total_price=_Offers.ExpressOffer.price,
        ).respond_with_delivery_route(
            offers=[_Combinator.ExpressOffer],
            points=[
                _Combinator.Warehouse,
                _Combinator.ExpressMovement,  # Этот сегмент содержит мета информацию о широком слоте
                _Combinator.Linehaul,
                _Combinator.EndPointRegion,
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
            shipment_warehouse=_Shops.ExpressShop.warehouse_id,
        )

        cls.combinatorExpress.on_express_warehouses_request(
            region_id=_Constants.MskRids,
            gps_coords=_Gps.location_combinator,
            rear_factors="market_nordstream=1;parallel_smm=1.0;ext_snippet=1;no_snippet_arc=1;market_enable_sins_offers_wizard=1",
        ).respond_with_express_warehouses(
            [
                CombinatorExpressWarehouse(
                    warehouse_id=_Shops.ExpressShop.warehouse_id,
                    zone_id=1,
                    business_id=_Shops.ExpressShop.fesh,
                    nearest_delivery_day=1,
                    nearest_delivery_interval=((0, 0), (23, 59)),
                ),
            ]
        )

    def test_express_delivery_with_wide_courier_option(self):
        '''
        Проверяется, что при использовани экспресс-оффера, возможно возвращение опции с широким слотом
        '''
        response = self.report.request_json(
            _Requests.BaseRequestTemplate.format(
                rids=_Constants.MskRids,
                offers=_Offers.ExpressOffer.waremd5 + ':1',
                type='courier',
                type_param=self.delivery_interval_string(
                    _Combinator.DeliveryDateFrom,
                    _Combinator.DeliveryTimeFrom,
                    _Combinator.DeliveryDateTo,
                    _Combinator.DeliveryTimeTo,
                ),
                nordstream=1,
            )
            + "&gps={}".format(_Gps.location_str)
        )

        self.assertFragmentIn(
            response,
            {
                "search": {"results": [{"delivery": {"option": {"isWideExpress": True, "isFastestExpress": True}}}]}
            },  # <- Проверяем именно это
        )

    @classmethod
    def prepare_extra_charge_options_fields(cls):
        cls.index.dynamic_delivery_tariffs += [
            DynamicDeliveryTariff(delivery_type=DynamicDeliveryTariff.DeliveryType.PICKUP_PARTHER),
        ]

    def test_extra_charge_options_fields(self):
        """
        Проверяется, что при наличии флага вычисления дополнительной стоимости доставки,
        необходимые поля поставляются в ответе
        """
        for dynamic_delivery_tariff in [1, 0]:
            response = self.report.request_json(
                _Requests.BaseRequestTemplate.format(
                    rids=_Constants.MskRids,
                    offers='{}:3'.format(_Offers.Fulfillment_3P_Offer.waremd5),
                    type='pickup',
                    type_param='point_id={}'.format(_Outlets.PickupOutlet.point_id),
                    nordstream=1,
                )
                + ('&rearr-factors=market_dynamic_delivery_tariffs={}'.format(dynamic_delivery_tariff))
                + "&debug=1"
            )
            if dynamic_delivery_tariff == 1:
                self.assertFragmentIn(response, {"logicTrace": [Contains("ItemCount: 3")]})

            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "delivery": {
                                    "option": {
                                        "extraCharge": {
                                            "value": "68",  # delivery cost = 70, - (-138) - 70 = 39 extra charge
                                            "currency": "RUR",
                                            "unitEconomyValue": "-138",
                                            "reasonCodes": [],
                                        }
                                        if dynamic_delivery_tariff
                                        else Absent(),
                                    }
                                }
                            }
                        ]
                    }
                },
            )


if __name__ == '__main__':
    main()
