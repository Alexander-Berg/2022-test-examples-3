#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from datetime import datetime, time, timedelta
import calendar
from core.combinator import DeliveryStats, make_offer_id
from core.dj import DjModel
from core.matcher import Absent, EmptyList, NotEmpty, NotEmptyList
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
    ExpressDeliveryService,
    ExpressSupplier,
    HyperCategory,
    MarketSku,
    Model,
    Offer,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    PrescriptionManagementSystem,
    ProhibitedBlueOffers,
    Region,
    RegionalDelivery,
    Shop,
    Tax,
    TimeInfo,
    TimeIntervalByDay,
    TimeIntervalInfo,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.types.offer import OfferDimensions
from core.types.payment_methods import Payment
from core.testcase import TestCase, main
from core.types.combinator import (
    create_delivery_option,
    create_virtual_box,
    DeliveryItem,
    DeliveryType,
    Destination,
    CombinatorOffer,
    RoutePoint,
    RoutePath,
    CombinatorGpsCoords,
    CombinatorExpressWarehouse,
)
from core.report import REQUEST_TIMESTAMP
from market.combinator.proto.grpc.combinator_pb2 import DeliveryService, Time

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

    model_id_for_heavy_and_oversized = 9
    category_id_for_heavy_and_oversized = 8

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

        prescription_drug_fesh = 400
        prescription_drug_feed_id = 401

        prescription_drug_bucket_id = 4002

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
        combinator_delivery_cost = 150


class _Shops:
    virtual_blue_shop = Shop(
        fesh=_Constants.virtual_blue_fesh,
        datafeed_id=_Constants.virtual_blue_feed_id,
        priority_region=_Constants.moscow_rids,
        tax_system=Tax.OSN,
        fulfillment_virtual=True,
        cpa=Shop.CPA_REAL,
        virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
        delivery_service_outlets=[_Constants.post_outlet_id],
    )

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

    express_dropship_wh2_shop = Shop(
        fesh=_Constants._ExpressPartners.dropship_fesh_wh2,
        datafeed_id=_Constants._ExpressPartners.dropship_feed_wh2_id,
        warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id,
        priority_region=_Constants.moscow_rids,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        fulfillment_program=False,
    )

    express_dropship_shop_with_schedule = Shop(
        fesh=_Constants._ExpressPartners.dropship_fesh_with_schedule,
        datafeed_id=_Constants._ExpressPartners.dropship_feed_id_with_schedule,
        warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id,
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
        cpa=Shop.CPA_REAL,
    )

    pickup_shop = Shop(
        fesh=_Constants._Partners.pickup_fesh,
        datafeed_id=_Constants._Partners.pickup_feed_id,
        warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id,
        priority_region=_Constants.moscow_rids,
        regions=[_Constants.moscow_rids],
        cpa=Shop.CPA_REAL,
    )

    express_prescription_drug_shop = Shop(
        fesh=_Constants._ExpressPartners.prescription_drug_fesh,
        datafeed_id=_Constants._ExpressPartners.prescription_drug_feed_id,
        warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id,
        priority_region=_Constants.moscow_rids,
        regions=[_Constants.moscow_rids],
        cpa=Shop.CPA_REAL,
        medicine_courier=True,
        prescription_management_system=PrescriptionManagementSystem.PS_MEDICATA,
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

    dropship_offer_for_buybox_fight = BlueOffer(
        offerid='dropship_sku9',
        waremd5=Offer.generate_waremd5('DropshipWaremd5sku9'),
        price=10,
        feedid=_Constants._Partners.dropship_feed_id,
        weight=5,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._Partners.dropship_fesh,
        delivery_buckets=[_Constants._Partners.courier_bucket_id],
    )

    dropship_msku = MarketSku(
        title="Обычный dropship оффер", hyperid=_Constants.category_id, sku=1, blue_offers=[dropship_offer]
    )

    express_prohibited_offer = BlueOffer(
        offerid='blue_express_prohibited',
        waremd5='ExpressDropshipProhmdw',
        price=700,
        feedid=_Constants._ExpressPartners.dropship_feed_id,
        is_express=True,
        supplier_id=_Constants._ExpressPartners.dropship_fesh,
        delivery_buckets=[_Constants._ExpressPartners.courier_bucket_id],
    )

    not_express_prohibited_offer = BlueOffer(
        offerid='blue_not_exress_prohibited',
        waremd5='NepressDropshipProhmdw',
        price=700,
        feedid=_Constants._ExpressPartners.dropship_feed_id,
        is_express=False,
        supplier_id=_Constants._ExpressPartners.dropship_fesh,
        delivery_buckets=[_Constants._ExpressPartners.courier_bucket_id],
    )

    prohibited_msku = MarketSku(
        title="Prohibited MSKU (blue)",
        hyperid=_Constants.category_id_prohibited,
        sku=_Constants.sku_prohibited_blue,
        blue_offers=[
            express_prohibited_offer,
            not_express_prohibited_offer,
        ],
    )

    dropship_msku_for_buybox_fight = MarketSku(
        title="Обычный dropship оффер боец",
        hyperid=_Constants.category_id_for_buybox_fight,
        sku=_Constants.sku_for_buybox_fight,
        blue_offers=[dropship_offer_for_buybox_fight],
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

    express_dropship_offer_with_wide_slot = BlueOffer(
        offerid='express_dropship_sku_wide',
        waremd5='ExpressDropshipWidemdw',
        price=700,
        feedid=_Constants._ExpressPartners.dropship_feed_id,
        weight=5,
        is_express=True,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._ExpressPartners.dropship_fesh,
        delivery_buckets=[_Constants._ExpressPartners.courier_bucket_id],
    )

    express_dropship_offer_cheap = BlueOffer(
        offerid='express_dropship_cheap_sku2',
        waremd5='ExpressDropshipCheapdw',
        price=70,
        feedid=_Constants._ExpressPartners.dropship_feed_id,
        weight=5,
        is_express=True,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._ExpressPartners.dropship_fesh,
        delivery_buckets=[_Constants._ExpressPartners.courier_bucket_id],
    )

    express_dropship_offer_expensive = BlueOffer(
        offerid='express_dropship_expensive_sku2',
        waremd5='ExpressDropshipExpendw',
        price=70000,
        feedid=_Constants._ExpressPartners.dropship_feed_id,
        weight=5,
        is_express=True,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._ExpressPartners.dropship_fesh,
        delivery_buckets=[_Constants._ExpressPartners.courier_bucket_id],
    )

    express_dropship_offer_heavy = BlueOffer(
        offerid='express_dropship_heavy_sku3',
        waremd5='ExpressDropshipHeavy_w',
        price=700,
        feedid=_Constants._ExpressPartners.dropship_feed_id,
        weight=16,
        is_express=True,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._ExpressPartners.dropship_fesh,
        delivery_buckets=[_Constants._ExpressPartners.courier_bucket_id],
    )

    express_dropship_offer_oversized = BlueOffer(
        offerid='express_dropship_oversized_sku3',
        waremd5='ExpressDropshipBigSz_w',
        price=70,
        feedid=_Constants._ExpressPartners.dropship_feed_id,
        weight=5,
        is_express=True,
        dimensions=OfferDimensions(length=51, width=51, height=51),
        supplier_id=_Constants._ExpressPartners.dropship_fesh,
        delivery_buckets=[_Constants._ExpressPartners.courier_bucket_id],
    )

    express_dropship_offer_for_buybox_fight = BlueOffer(
        offerid='express_dropship_sku9',
        waremd5=Offer.generate_waremd5('ExpressDropshipWarem9'),
        price=60,
        feedid=_Constants._ExpressPartners.dropship_feed_id,
        weight=5,
        is_express=True,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._ExpressPartners.dropship_fesh,
        delivery_buckets=[_Constants._ExpressPartners.courier_bucket_id],
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
    )

    express_dropship_wh2_offer = BlueOffer(
        offerid='express_dropship_wh2_sku1',
        waremd5='ExpressDropshipWh2Warw',
        price=30,
        feedid=_Constants._ExpressPartners.dropship_feed_wh2_id,
        weight=5,
        is_express=True,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._ExpressPartners.dropship_fesh_wh2,
        delivery_buckets=[_Constants._ExpressPartners.courier_bucket_id],
    )

    no_express_dropship_wh2_offer = BlueOffer(
        offerid='no_express_dropship_wh2_sku1',
        waremd5='NoExpreDropshipWh2Warw',
        price=30,
        feedid=_Constants._ExpressPartners.dropship_feed_wh2_id,
        weight=5,
        is_express=False,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._ExpressPartners.dropship_fesh_wh2,
        delivery_buckets=[_Constants._ExpressPartners.courier_bucket_id],
    )

    express_dropship_msku = MarketSku(
        title="Экспресс dropship оффер", hyperid=_Constants.category_id, sku=2, blue_offers=[express_dropship_offer]
    )

    express_dropship_wide_msku = MarketSku(
        title="Экспресс dropship оффер с широким слотом",
        hyperid=_Constants.model_id_for_cheap_and_expensive,
        sku=23,
        blue_offers=[express_dropship_offer_with_wide_slot],
    )

    express_dropship_msku_2 = MarketSku(
        title="Экспресс dropship оффер 2",
        hyperid=_Constants.model_id_for_cheap_and_expensive,
        sku=22,
        feedid=_Constants._ExpressPartners.dropship_feed_id,
        blue_offers=[
            express_dropship_offer_cheap,
            express_dropship_offer_expensive,
        ],
    )

    express_dropship_msku_3 = MarketSku(
        title="Экспресс dropship оффер 3",
        hyperid=_Constants.model_id_for_heavy_and_oversized,
        sku=33,
        blue_offers=[
            express_dropship_offer_heavy,
            express_dropship_offer_oversized,
        ],
    )

    express_dropship_msku_for_buybox_fight = MarketSku(
        title="Экспресс dropship оффер боец",
        hyperid=_Constants.category_id_for_buybox_fight,
        sku=_Constants.sku_for_buybox_fight,
        blue_offers=[express_dropship_offer_for_buybox_fight],
    )

    express_dropship_msku_with_schedule = MarketSku(
        title="Экспресс dropship оффер с расписанием",
        hyperid=_Constants.category_id_with_schedule,
        sku=30,
        blue_offers=[express_dropship_offer_with_schedule],
    )

    express_dropship_wh2_msku = MarketSku(
        title="Экспресс dropship WH2 оффер",
        hyperid=_Constants.category_wh2_id,
        sku=7,
        blue_offers=[express_dropship_wh2_offer],
    )

    no_express_dropship_wh2_msku = MarketSku(
        title="Не-Экспресс dropship WH2 оффер",
        hyperid=_Constants.category_wh2_id,
        sku=8,
        blue_offers=[no_express_dropship_wh2_offer],
    )


class _WhiteOffers:
    prohibited_msku = MarketSku(
        title="Prohibited MSKU (white)",
        hyperid=_Constants.category_id_prohibited,
        sku=_Constants.sku_prohibited_white,
    )

    prohibited_white_offer = Offer(
        offerid='white_express_prohibited',
        waremd5='White__DropshipProhmdw',
        price=30,
        fesh=_Constants._Partners.usual_white_fesh,
        cpa=Offer.CPA_REAL,
        hyperid=_Constants.category_id_prohibited,
        sku=prohibited_msku.sku,
        delivery_buckets=[_Constants._ExpressPartners.courier_bucket_id],
    )

    usual_white_offer = Offer(
        hyperid=_Constants.category_id,
        fesh=_Constants._Partners.usual_white_fesh,
    )

    usual_white_offer_filter = Offer(
        hyperid=_Constants.category_id_filter,
        fesh=_Constants._Partners.usual_white_fesh,
    )

    pickup_msku = MarketSku(title="Pickup msku", hyperid=_Constants.category_pickup_id, sku=5)

    express_prescription_drug_msku = MarketSku(
        title="Express prescription drug msku", hyperid=_Constants.category_id_prescription_drug, sku=6
    )

    pickup_offer = Offer(
        waremd5='pickup_offer____gggggg',
        hyperid=_Constants.category_pickup_id,
        sku=pickup_msku.sku,
        fesh=_Constants._Partners.pickup_fesh,
        price=30,
        weight=5,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        pickup_buckets=[_Constants._Partners.pickup_bucket_id],
        has_delivery_options=False,
        pickup=True,
    )

    express_prescription_drug_offer = Offer(
        waremd5='express_prescription_g',
        hyperid=_Constants.category_id_prescription_drug,
        sku=express_prescription_drug_msku.sku,
        fesh=_Constants._ExpressPartners.prescription_drug_fesh,
        price=30,
        weight=5,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        delivery_buckets=[_Constants._ExpressPartners.prescription_drug_bucket_id],
        is_express=True,
        is_medicine=True,
        is_prescription=True,
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

    express_dropship_offer_with_wide_slot = CombinatorOffer(
        shop_sku=_BlueOffers.express_dropship_offer_with_wide_slot.offerid,
        shop_id=_Constants._ExpressPartners.dropship_fesh,
        partner_id=_Constants._ExpressPartners.dropship_warehouse_id,
        available_count=1,
    )

    express_dropship_offer_cheap = CombinatorOffer(
        shop_sku=_BlueOffers.express_dropship_offer_cheap.offerid,
        shop_id=_Constants._ExpressPartners.dropship_fesh,
        partner_id=_Constants._ExpressPartners.dropship_warehouse_id,
        available_count=1,
    )

    express_dropship_offer_expensive = CombinatorOffer(
        shop_sku=_BlueOffers.express_dropship_offer_expensive.offerid,
        shop_id=_Constants._ExpressPartners.dropship_fesh,
        partner_id=_Constants._ExpressPartners.dropship_warehouse_id,
        available_count=1,
    )

    express_dropship_offer_heavy = CombinatorOffer(
        shop_sku=_BlueOffers.express_dropship_offer_heavy.offerid,
        shop_id=_Constants._ExpressPartners.dropship_fesh,
        partner_id=_Constants._ExpressPartners.dropship_warehouse_id,
        available_count=1,
    )

    express_dropship_offer_oversized = CombinatorOffer(
        shop_sku=_BlueOffers.express_dropship_offer_oversized.offerid,
        shop_id=_Constants._ExpressPartners.dropship_fesh,
        partner_id=_Constants._ExpressPartners.dropship_warehouse_id,
        available_count=1,
    )

    fake_express_dropship_offer = CombinatorOffer(
        shop_sku='',
        # в случае фейкового оффера в комбинатор отправляется
        # эта константа и опции возвращаются, воспроизводим
        shop_id=_Constants.MAX_UINT32,
        partner_id=_Constants._ExpressPartners.dropship_warehouse_id,
        available_count=1,
    )


class _Requests:
    prime_request = 'place=prime&rgb=blue&allow-collapsing=0&rids={rids}&market-sku={msku}'

    white_prime_request = 'place=prime&rgb=white&allow-collapsing=0&rids={rids}&hyperid={hyper_id}'

    white_prime_request_filter = (
        'place=prime' '&pp=18' '&rgb=white' '&allow-collapsing=0' '&rids={rids}' '&hyperid={hyper_id}'
    )

    white_prime_request_express = (
        'place=prime'
        '&pp=18'
        '&rgb=white'
        '&allow-collapsing=0'
        '&rids={rids}'
        '&market-sku={msku}'
        '&rearr-factors=enable_prescription_drugs_delivery={prescription}'
    )

    sku_offers_request = 'place=sku_offers' '&pp=18' '&rgb=blue' '&rids={rids}' '&market-sku={msku}'

    productoffers_request = 'place=productoffers' '&pp=18' '&rgb=blue' '&rids={rids}' '&market-sku={msku}'

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

    delivery_route_request = (
        'place=delivery_route'
        '&pp=18'
        '&rgb=blue'
        '&rids={rids}'
        '&offers-list={offers}'
        '&delivery-type=courier'
        '&{date}'
    )

    shop_info_request = 'place=shop_info' '&rids={rids}' '&fesh={fesh}'

    business_info_request = 'place=business_info' '&rids={rids}' '&fesh={fesh}'


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

    end_point_msk_region_for_wide_express = RoutePoint(
        point_ids=Destination(region_id=_Constants.moscow_rids),
        segment_id=512016,
        segment_type="handing",
        services=(
            (
                DeliveryService.OUTBOUND,
                "HANDING",
                service_time(14, 30),
                timedelta(minutes=15),
                (Time(hour=17, minute=00), Time(hour=18, minute=00)),
            ),
        ),
    )

    express_movement_with_wide_slot = RoutePoint(
        point_ids=Destination(partner_id=_Constants._ExpressPartners.delivery_service_id),
        segment_id=512005,
        segment_type="movement",
        services=(
            (
                DeliveryService.INTERNAL,
                "CALL_COURIER",
                service_time(13, 30),
                timedelta(seconds=15),
                None,
                {"IS_WIDE_EXPRESS": "1"},
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
    Набор тестов для проекта "Экспресс-доставка". Краткое описание:
        -   Экспресс-доставка - суть курьерская доставка в течение 1-2 часов
            с фиксированной повышенной стоимостью для пользователя
        -   В LMS заведена новая виртуальная СД для Яндекс.Go как службы последней мили
        -   В Тарификаторе для нее заведен тариф с dayFrom = dayTo = 0
        -   Есть ограниченный набор поставщиков (supplier_id), предоставляющих свои офферы
            на условиях экспресс-доставки
        -   Список экспресс-поставщиков и экспресс-СД поставляется под Репорт
            через 'express_partners.json' из svn-data
        -   Репорт размечает офферы на базовом поиске бинарным признаком 'isExpress'
            при выполнении всех условий из списка:
                * поставщик из 'epxress_partners.json'
                * СД из 'epxress_partners.json'
                * срок доставки на текущий момент = 0 дней
        -   Экспресс-офферы всегда включаются в выдачу (флаг упразднен см. https://st.yandex-team.ru/MARKETOUT-42913)
        -   Возможна фильтрация по признаку 'isExrepss' через CGI-параметр 'filter-express-delivery=1'
        -   По ум считаем, что вычисление сроков идет по новому пути - через Комбинатор
    См. https://st.yandex-team.ru/MARKETOUT-36256
    """

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
            _Shops.express_dropship_shop_with_schedule,
            _Shops.express_dropship_wh2_shop,
            _Shops.usual_white_shop,
            _Shops.pickup_shop,
            _Shops.express_prescription_drug_shop,
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
                feed_id=_Constants._Partners.pickup_feed_id,
                supplier_id=_Constants._Partners.pickup_fesh,
                warehouse_id=_Constants._ExpressPartners.dropship_warehouse_id,
            )
        ]
        cls.index.express_partners.suppliers += [
            ExpressSupplier(
                feed_id=_Constants._ExpressPartners.prescription_drug_feed_id,
                supplier_id=_Constants._ExpressPartners.prescription_drug_fesh,
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
            DeliveryBucket(
                bucket_id=_Constants._ExpressPartners.prescription_drug_bucket_id,
                fesh=_Constants._ExpressPartners.prescription_drug_fesh,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=_Constants.moscow_rids, options=[DeliveryOption(price=100, day_from=1, day_to=1)]
                    )
                ],
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
            offer_ids=make_offer_id(_BlueOffers.express_dropship_offer_cheap, _Shops.express_dropship_shop),
            courier_stats=DeliveryStats(
                cost=_Constants._ExpressPartners.combinator_delivery_cost,
                day_from=_Constants._ExpressPartners.combinator_day_from,
                day_to=_Constants._ExpressPartners.combinator_day_to,
            ),
        )

        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_BlueOffers.express_dropship_offer_expensive, _Shops.express_dropship_shop),
            courier_stats=DeliveryStats(
                cost=_Constants._ExpressPartners.combinator_delivery_cost,
                day_from=_Constants._ExpressPartners.combinator_day_from,
                day_to=_Constants._ExpressPartners.combinator_day_to,
            ),
        )

        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_BlueOffers.express_dropship_offer_heavy, _Shops.express_dropship_shop),
            courier_stats=DeliveryStats(
                cost=_Constants._ExpressPartners.combinator_delivery_cost,
                day_from=_Constants._ExpressPartners.combinator_day_from,
                day_to=_Constants._ExpressPartners.combinator_day_to,
            ),
        )

        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_BlueOffers.express_dropship_offer_oversized, _Shops.express_dropship_shop),
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
                    date_from=_Constants._Partners.combinator_date_from,
                    date_to=_Constants._Partners.combinator_date_to,
                    time_from=time(10, 0),
                    time_to=time(18, 0),
                    delivery_service_id=_Constants._Partners.delivery_service_id,
                )
            ]
        )

        for rid in [_Constants.moscow_rids, _Constants.ekb_rids]:
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
                destination=Destination(region_id=rid),
                payment_methods=[],
                total_price=_BlueOffers.express_dropship_offer.price,
            ).respond_with_courier_options(
                options=[
                    create_delivery_option(
                        cost=_Constants._ExpressPartners.combinator_delivery_cost,
                        date_from=_Constants._ExpressPartners.combinator_date_from,
                        date_to=_Constants._ExpressPartners.combinator_date_to,
                        time_from=time(10, 0),
                        time_to=time(12, 0),
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
                        offers=[_CombinatorOffers.express_dropship_offer_with_wide_slot],
                        price=_BlueOffers.express_dropship_offer_with_wide_slot.price,
                    )
                ],
                destination=Destination(region_id=rid),
                payment_methods=[],
                total_price=_BlueOffers.express_dropship_offer_with_wide_slot.price,
            ).respond_with_courier_options(
                options=[
                    create_delivery_option(
                        cost=_Constants._ExpressPartners.combinator_delivery_cost,
                        date_from=_Constants._ExpressPartners.combinator_date_from,
                        date_to=_Constants._ExpressPartners.combinator_date_to,
                        time_from=time(10, 0),
                        time_to=time(12, 0),
                        delivery_service_id=_Constants._ExpressPartners.delivery_service_id,
                        is_wide_express=True,
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
                        offers=[_CombinatorOffers.express_dropship_offer_cheap],
                        price=_BlueOffers.express_dropship_offer_cheap.price,
                    ),
                ],
                destination=Destination(region_id=rid),
                payment_methods=[],
                total_price=_BlueOffers.express_dropship_offer_cheap.price,
            ).respond_with_courier_options(
                options=[
                    create_delivery_option(
                        cost=_Constants._ExpressPartners.combinator_delivery_cost,
                        date_from=_Constants._ExpressPartners.combinator_date_from,
                        date_to=_Constants._ExpressPartners.combinator_date_to,
                        time_from=time(10, 0),
                        time_to=time(12, 0),
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
                        offers=[_CombinatorOffers.express_dropship_offer_expensive],
                        price=_BlueOffers.express_dropship_offer_expensive.price,
                    ),
                ],
                destination=Destination(region_id=rid),
                payment_methods=[],
                total_price=_BlueOffers.express_dropship_offer_expensive.price,
            ).respond_with_courier_options(
                options=[
                    create_delivery_option(
                        cost=_Constants._ExpressPartners.combinator_delivery_cost,
                        date_from=_Constants._ExpressPartners.combinator_date_from,
                        date_to=_Constants._ExpressPartners.combinator_date_to,
                        time_from=time(10, 0),
                        time_to=time(12, 0),
                        delivery_service_id=_Constants._ExpressPartners.delivery_service_id,
                    )
                ]
            )

            cls.combinator.on_courier_options_request(
                items=[
                    DeliveryItem(
                        required_count=1,
                        weight=16000,
                        dimensions=[30, 30, 30],
                        cargo_types=[],
                        offers=[_CombinatorOffers.express_dropship_offer_heavy],
                        price=_BlueOffers.express_dropship_offer_heavy.price,
                    ),
                ],
                destination=Destination(region_id=rid),
                payment_methods=[],
                total_price=_BlueOffers.express_dropship_offer_heavy.price,
            ).respond_with_courier_options(
                options=[
                    create_delivery_option(
                        cost=_Constants._ExpressPartners.combinator_delivery_cost,
                        date_from=_Constants._ExpressPartners.combinator_date_from,
                        date_to=_Constants._ExpressPartners.combinator_date_to,
                        time_from=time(10, 0),
                        time_to=time(12, 0),
                        delivery_service_id=_Constants._ExpressPartners.delivery_service_id,
                    )
                ]
            )

            cls.combinator.on_courier_options_request(
                items=[
                    DeliveryItem(
                        required_count=1,
                        weight=5000,
                        dimensions=[51, 51, 51],
                        cargo_types=[],
                        offers=[_CombinatorOffers.express_dropship_offer_oversized],
                        price=_BlueOffers.express_dropship_offer_oversized.price,
                    ),
                ],
                destination=Destination(region_id=rid),
                payment_methods=[],
                total_price=_BlueOffers.express_dropship_offer_oversized.price,
            ).respond_with_courier_options(
                options=[
                    create_delivery_option(
                        cost=_Constants._ExpressPartners.combinator_delivery_cost,
                        date_from=_Constants._ExpressPartners.combinator_date_from,
                        date_to=_Constants._ExpressPartners.combinator_date_to,
                        time_from=time(10, 0),
                        time_to=time(12, 0),
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
                        offers=[_CombinatorOffers.fake_express_dropship_offer],
                        price=_BlueOffers.express_dropship_offer.price,
                    )
                ],
                destination=Destination(region_id=rid),
                payment_methods=[],
                total_price=_BlueOffers.express_dropship_offer.price,
            ).respond_with_courier_options(
                options=[
                    create_delivery_option(
                        cost=_Constants._ExpressPartners.combinator_delivery_cost,
                        date_from=_Constants._ExpressPartners.combinator_date_from,
                        date_to=_Constants._ExpressPartners.combinator_date_to,
                        time_from=time(10, 0),
                        time_to=time(12, 0),
                        delivery_service_id=_Constants._ExpressPartners.delivery_service_id,
                    )
                ]
            )

        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.COURIER,
            destination=_Points.end_point_msk_region,
            delivery_option=create_delivery_option(
                date_from=_Constants._ExpressPartners.combinator_date_from,
                date_to=_Constants._ExpressPartners.combinator_date_to,
                time_from=time(15, 0),
                time_to=time(16, 0),
            ),
            total_price=_BlueOffers.express_dropship_offer.price,
        ).respond_with_delivery_route(
            offers=[_CombinatorOffers.express_dropship_offer],
            points=[
                _Points.express_dropship_warehouse,
                _Points.express_movement,
                _Points.end_point_msk_region,
            ],
            paths=[RoutePath(point_from=0, point_to=1), RoutePath(point_from=1, point_to=2)],
            date_from=_Constants._ExpressPartners.combinator_date_from,
            date_to=_Constants._ExpressPartners.combinator_date_to,
            virtual_box=create_virtual_box(weight=5000, length=30, width=30, height=30),
        )

        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.COURIER,
            destination=_Points.end_point_msk_region_for_wide_express,
            delivery_option=create_delivery_option(
                date_from=_Constants._ExpressPartners.combinator_date_from,
                date_to=_Constants._ExpressPartners.combinator_date_to,
                time_from=time(17, 0),
                time_to=time(18, 0),
            ),
            total_price=_BlueOffers.express_dropship_offer_with_wide_slot.price,
        ).respond_with_delivery_route(
            offers=[_CombinatorOffers.express_dropship_offer_with_wide_slot],
            points=[
                _Points.express_dropship_warehouse,
                _Points.express_movement_with_wide_slot,
                _Points.end_point_msk_region_for_wide_express,
            ],
            paths=[RoutePath(point_from=0, point_to=1), RoutePath(point_from=1, point_to=2)],
            date_from=_Constants._ExpressPartners.combinator_date_from,
            date_to=_Constants._ExpressPartners.combinator_date_to,
            virtual_box=create_virtual_box(weight=5000, length=30, width=30, height=30),
        )

        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.COURIER,
            destination=_Points.end_point_msk_region,
            delivery_option=create_delivery_option(
                date_from=_Constants._ExpressPartners.combinator_date_from,
                date_to=_Constants._ExpressPartners.combinator_date_to,
                time_from=time(15, 0),
                time_to=time(16, 0),
            ),
            total_price=_BlueOffers.express_dropship_offer_cheap.price,
        ).respond_with_delivery_route(
            offers=[_CombinatorOffers.express_dropship_offer_cheap],
            points=[
                _Points.express_dropship_warehouse,
                _Points.express_movement,
                _Points.end_point_msk_region,
            ],
            paths=[RoutePath(point_from=0, point_to=1), RoutePath(point_from=1, point_to=2)],
            date_from=_Constants._ExpressPartners.combinator_date_from,
            date_to=_Constants._ExpressPartners.combinator_date_to,
            virtual_box=create_virtual_box(weight=5000, length=30, width=30, height=30),
        )

        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.COURIER,
            destination=_Points.end_point_msk_region,
            delivery_option=create_delivery_option(
                date_from=_Constants._ExpressPartners.combinator_date_from,
                date_to=_Constants._ExpressPartners.combinator_date_to,
                time_from=time(15, 0),
                time_to=time(16, 0),
            ),
            total_price=_BlueOffers.express_dropship_offer_expensive.price,
        ).respond_with_delivery_route(
            offers=[_CombinatorOffers.express_dropship_offer_expensive],
            points=[
                _Points.express_dropship_warehouse,
                _Points.express_movement,
                _Points.end_point_msk_region,
            ],
            paths=[RoutePath(point_from=0, point_to=1), RoutePath(point_from=1, point_to=2)],
            date_from=_Constants._ExpressPartners.combinator_date_from,
            date_to=_Constants._ExpressPartners.combinator_date_to,
            virtual_box=create_virtual_box(weight=5000, length=30, width=30, height=30),
        )

        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.COURIER,
            destination=_Points.end_point_msk_region,
            delivery_option=create_delivery_option(
                date_from=_Constants._Partners.combinator_date_from,
                date_to=_Constants._Partners.combinator_date_to,
                time_from=time(15, 0),
                time_to=time(16, 0),
            ),
            total_price=_BlueOffers.dropship_offer.price,
        ).respond_with_delivery_route(
            offers=[_CombinatorOffers.dropship_offer],
            points=[
                _Points.dropship_warehouse,
                _Points.movement,
                _Points.end_point_msk_region,
            ],
            paths=[RoutePath(point_from=0, point_to=1), RoutePath(point_from=1, point_to=2)],
            date_from=_Constants._Partners.combinator_date_from,
            date_to=_Constants._Partners.combinator_date_to,
            virtual_box=create_virtual_box(weight=5000, length=30, width=30, height=30),
        )

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.mskus += [
            _BlueOffers.dropship_msku,
            _BlueOffers.dropship_msku_for_buybox_fight,
            _BlueOffers.express_dropship_msku,
            _BlueOffers.express_dropship_wide_msku,
            _BlueOffers.express_dropship_msku_2,
            _BlueOffers.express_dropship_msku_3,
            _BlueOffers.express_dropship_msku_for_buybox_fight,
            _BlueOffers.express_dropship_msku_with_schedule,
            _BlueOffers.express_dropship_wh2_msku,
            _BlueOffers.no_express_dropship_wh2_msku,
            _BlueOffers.prohibited_msku,
            _WhiteOffers.prohibited_msku,
            _WhiteOffers.pickup_msku,
            _WhiteOffers.express_prescription_drug_msku,
        ]

        cls.index.offers += [
            _WhiteOffers.prohibited_white_offer,
            _WhiteOffers.usual_white_offer,
            _WhiteOffers.usual_white_offer_filter,
            _WhiteOffers.pickup_offer,
            _WhiteOffers.express_prescription_drug_offer,
        ]

        cls.index.models += [
            Model(hid=_Constants.drugs_category_id, hyperid=_Constants.category_id_prescription_drug),
            Model(hid=_Constants.category_id_for_buybox_fight, hyperid=_Constants.category_id_for_buybox_fight),
            Model(
                hid=_Constants.category_id_for_cheap_and_expensive, hyperid=_Constants.model_id_for_cheap_and_expensive
            ),
            Model(
                hid=_Constants.category_id_for_heavy_and_oversized, hyperid=_Constants.model_id_for_heavy_and_oversized
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

        cls.index.express_unit_economy.add_static_experiment_split(
            split_name="static-split-1",
            plus_discount_in_rur=60,
            gmv_threshold_in_rur=1200,
            value_below_threshold_in_rur=199,
            value_above_threshold_in_rur=99,
        )

        cls.index.express_unit_economy.add_static_experiment_split(
            split_name="static-split-2",
            plus_discount_in_rur=50,
            gmv_threshold_in_rur=1200,
            value_below_threshold_in_rur=199,
            value_above_threshold_in_rur=99,
        )

        cls.index.express_unit_economy.add_dynamic_experiment_split(
            split_name="dynamic-split-1",
            plus_discount_in_rur=50,
            target_ue_by_category=[(90401, 0.0)],
            min_cost=0,
            max_cost=249,
            min_cost_for_wide_slot=49,
            cost_rounding_step=50,
            support_and_payment_cost=1.7,
            take_rate_by_category=[
                (90401, 8.0),
                (_Constants.category_id_for_cheap_and_expensive_parent, 0.25),
            ],
            logistics_revenue_from_merchant=125,
            min_dynamic_radius_in_km=15.0,
            new_logistics_revenue_from_merchant_relative_value=4.0,
            new_logistics_revenue_from_merchant_min_value=55,
            new_logistics_revenue_from_merchant_max_value=200,
            new_logistics_revenue_from_merchant_weight_threshold_in_kg=15.0,
            new_logistics_revenue_from_merchant_dimensions_sum_threshold_in_cm=150.0,
            new_logistics_revenue_from_merchant_bulk_tariff=350,
        )

        cls.index.express_unit_economy.add_dynamic_experiment_split(
            split_name="dynamic-with-target-ue",
            plus_discount_in_rur=50,
            target_ue_by_category=[
                (90401, 0.0),
                (_Constants.category_id_for_cheap_and_expensive_parent, 11.0),
            ],
            min_cost=49,
            max_cost=499,
            min_cost_for_wide_slot=0,
            cost_rounding_step=50,
            support_and_payment_cost=1.7,
            take_rate_by_category=[
                (90401, 8.0),
                (_Constants.category_id_for_cheap_and_expensive_parent, -9.0),
                (_Constants.category_id, -11.0),
            ],
            logistics_revenue_from_merchant=125,
            min_dynamic_radius_in_km=15.0,
            new_logistics_revenue_from_merchant_relative_value=4.0,
            new_logistics_revenue_from_merchant_min_value=55,
            new_logistics_revenue_from_merchant_max_value=200,
            new_logistics_revenue_from_merchant_weight_threshold_in_kg=15.0,
            new_logistics_revenue_from_merchant_dimensions_sum_threshold_in_cm=150.0,
            new_logistics_revenue_from_merchant_bulk_tariff=350,
        )

        cls.index.express_unit_economy.add_dynamic_experiment_split(
            split_name="dynamic-split-2",
            plus_discount_in_rur=50,
            target_ue_by_category=[(90401, -2.0)],
            min_cost=0,
            max_cost=249,
            min_cost_for_wide_slot=99,
            cost_rounding_step=50,
            support_and_payment_cost=1.7,
            take_rate_by_category=[
                (90401, 8.0),
                (_Constants.category_id_for_cheap_and_expensive_parent, 0.25),
            ],
            logistics_revenue_from_merchant=10,
            min_dynamic_radius_in_km=15.0,
            new_logistics_revenue_from_merchant_relative_value=4.0,
            new_logistics_revenue_from_merchant_min_value=55,
            new_logistics_revenue_from_merchant_max_value=200,
            new_logistics_revenue_from_merchant_weight_threshold_in_kg=15.0,
            new_logistics_revenue_from_merchant_dimensions_sum_threshold_in_cm=150.0,
            new_logistics_revenue_from_merchant_bulk_tariff=350,
        )

    @classmethod
    def prepare_nearest_delivery_from_combinator(cls):
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(1.0, 1.0),
            rear_factors=make_mock_rearr(),
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
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(1.0, 1.0),
            rear_factors=make_mock_rearr(express_nearest_time_interval_enabled=0),
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
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=_Constants.moscow_rids,
            gps_coords=CombinatorGpsCoords(2.0, 2.0),
            rear_factors=make_mock_rearr(market_hyperlocal_context_mmap_version=3, new_delivery_interval_today=True),
        ).respond_with_express_warehouses(
            [
                CombinatorExpressWarehouse(
                    warehouse_id=11,
                    zone_id=1,
                    nearest_delivery_day=_FilterDeliveryInterval.TOMORROW,
                    nearest_delivery_interval=((12, 30), (13, 45)),
                )
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=_Constants.moscow_rids,
            gps_coords=CombinatorGpsCoords(2.0, 2.0),
            rear_factors=make_mock_rearr(
                market_hyperlocal_context_mmap_version=3,
                new_delivery_interval_today=True,
                new_filter_express_delivery_today=False,
            ),
        ).respond_with_express_warehouses(
            [
                CombinatorExpressWarehouse(
                    warehouse_id=11,
                    zone_id=1,
                    nearest_delivery_day=_FilterDeliveryInterval.TOMORROW,
                    nearest_delivery_interval=((12, 30), (13, 45)),
                )
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=_Constants.moscow_rids,
            gps_coords=CombinatorGpsCoords(3.0, 3.0),
            rear_factors=make_mock_rearr(market_hyperlocal_context_mmap_version=3, new_delivery_interval_today=True),
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
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=_Constants.moscow_rids,
            gps_coords=CombinatorGpsCoords(4.0, 4.0),
            rear_factors=make_mock_rearr(
                market_hyperlocal_context_mmap_version=3,
                new_delivery_interval_today=True,
                show_old_filter_express_delivery=True,
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

        cls.combinatorExpress.on_express_warehouses_request(
            region_id=_Constants.moscow_rids,
            gps_coords=CombinatorGpsCoords(5.0, 5.0),
            rear_factors=make_mock_rearr(
                market_hyperlocal_context_mmap_version=3,
                new_delivery_interval_today=True,
                show_old_filter_express_delivery=False,
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

        cls.combinatorExpress.on_express_warehouses_request(
            region_id=_Constants.moscow_rids,
            gps_coords=CombinatorGpsCoords(5.0, 5.0),
            rear_factors=make_mock_rearr(
                market_hyperlocal_context_mmap_version=3,
                new_delivery_interval_today=True,
                show_old_filter_express_delivery=False,
                enable_dsbs_filter_by_delivery_interval=1,
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

        cls.combinatorExpress.on_express_warehouses_request(
            region_id=_Constants.moscow_rids,
            gps_coords=CombinatorGpsCoords(6.0, 6.0),
            rear_factors=make_mock_rearr(
                market_hyperlocal_context_mmap_version=3,
                enable_dsbs_filter_by_delivery_interval=0,
                honest_express_filter=True,
                market_promo_datetime=str(get_custom_date_time(datetime(2021, 3, 4, 12, 0))),
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

        cls.combinatorExpress.on_express_warehouses_request(
            region_id=_Constants.moscow_rids,
            gps_coords=CombinatorGpsCoords(7.0, 7.0),
            rear_factors=make_mock_rearr(
                market_hyperlocal_context_mmap_version=3,
                enable_dsbs_filter_by_delivery_interval=1,
                honest_express_filter=True,
                market_promo_datetime=str(get_custom_date_time(datetime(2021, 3, 4, 12, 0))),
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

        cls.combinatorExpress.on_express_warehouses_request(
            region_id=_Constants.moscow_rids,
            gps_coords=CombinatorGpsCoords(8.0, 8.0),
            rear_factors=make_mock_rearr(
                market_hyperlocal_context_mmap_version=3,
                enable_dsbs_filter_by_delivery_interval=2,
                honest_express_filter=True,
                market_promo_datetime=str(get_custom_date_time(datetime(2021, 3, 4, 12, 0))),
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

        cls.combinatorExpress.on_express_warehouses_request(
            region_id=_Constants.moscow_rids,
            gps_coords=CombinatorGpsCoords(8.0, 8.0),
            rear_factors=make_mock_rearr(
                market_hyperlocal_context_mmap_version=3,
                market_promo_datetime=str(get_custom_date_time(datetime(2021, 3, 1, 11, 0))),
            ),
        ).respond_with_express_warehouses(
            [
                CombinatorExpressWarehouse(
                    warehouse_id=11,
                    zone_id=1,
                    nearest_delivery_day=_FilterDeliveryInterval.TODAY,
                    nearest_delivery_interval=((11, 30), (12, 45)),
                )
            ]
        )

        cls.combinatorExpress.on_express_warehouses_request(
            region_id=_Constants.moscow_rids,
            gps_coords=CombinatorGpsCoords(6.0, 6.0),
            rear_factors=make_mock_rearr(
                market_hyperlocal_context_mmap_version=3,
                enable_dsbs_filter_by_delivery_interval=0,
                honest_express_filter=True,
                fastest_delivery_interval=1,
                market_promo_datetime=str(get_custom_date_time(datetime(2021, 3, 4, 12, 0))),
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

        cls.combinatorExpress.on_express_warehouses_request(
            region_id=_Constants.moscow_rids,
            gps_coords=CombinatorGpsCoords(8.0, 8.0),
            rear_factors=make_mock_rearr(
                market_hyperlocal_context_mmap_version=3,
                enable_dsbs_filter_by_delivery_interval=2,
                honest_express_filter=True,
                fastest_delivery_interval=1,
                market_promo_datetime=str(get_custom_date_time(datetime(2021, 3, 4, 12, 0))),
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

        cls.combinatorExpress.on_express_warehouses_request(
            region_id=_Constants.moscow_rids,
            gps_coords=CombinatorGpsCoords(9.0, 9.0),
            rear_factors=make_mock_rearr(
                market_hyperlocal_context_mmap_version=3,
                honest_express_filter=True,
                market_promo_datetime=str(get_custom_date_time(datetime(2021, 3, 4, 12, 0))),
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

    def date_time_to_string(sekf, dt, tm):
        return '{year}{month}{day}.{hour}{minute}'.format(
            year=dt.year, month=dt.month, day=dt.day, hour=tm.hour, minute=tm.minute
        )

    def delivery_interval_string(self, date_from, time_from, date_to, time_to):
        return 'delivery-interval={}-{}'.format(
            self.date_time_to_string(date_from, time_from), self.date_time_to_string(date_to, time_to)
        )

    def test_filter_express_prescription_offer(self):
        """
        Проверяем, что рецептурные экспресс-офферы попадают в экспресс выдачу
        при разрешении на доставку enable_prescription_drugs_delivery=1,
        а также при подключении поставщика к системе электронного рецепта
        """
        for prescription in [0, 1]:
            response = self.report.request_json(
                _Requests.white_prime_request_express.format(
                    rids=_Constants.moscow_rids,
                    msku=_WhiteOffers.express_prescription_drug_msku.sku,
                    prescription=prescription,
                )
                + '&filter-express-delivery=1'
            )
            if prescription:
                self.assertFragmentIn(
                    response,
                    {
                        'results': [
                            {
                                'entity': 'offer',
                                'wareId': _WhiteOffers.express_prescription_drug_offer.waremd5,
                                'delivery': {'isExpress': True},
                            }
                        ]
                    },
                )
            else:
                self.assertFragmentIn(response, {'results': EmptyList()})

    def test_exp_flag_for_pickup_offer(self):
        """
        Проверяем, что для НЕ рецептурных медицинских товаров с самовывозов
        запрещена экспресс-доставка
        """
        response = self.report.request_json(
            _Requests.white_prime_request_express.format(
                rids=_Constants.moscow_rids, msku=_WhiteOffers.pickup_msku.sku, prescription=0
            )
            + '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'wareId': _WhiteOffers.pickup_offer.waremd5,
                        'delivery': {'isExpress': False, 'options': []},
                    }
                ]
            },
        )

    def test_exp_flag_for_prescription_drug_offer(self):
        """
        Проверяем наличие экспресс признака у экспресс рецептурных медицинских препаратов
        """
        response = self.report.request_json(
            _Requests.white_prime_request_express.format(
                rids=_Constants.moscow_rids, msku=_WhiteOffers.express_prescription_drug_msku.sku, prescription=1
            )
            + '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'wareId': _WhiteOffers.express_prescription_drug_offer.waremd5,
                        'delivery': {'isExpress': True},
                    }
                ]
            },
        )

    def test_exp_flag_filtering_prime(self):
        """
        Проверяем, что экспресс-офферы всегда включаются в выдачу place=prime
        """
        response = self.report.request_json(
            _Requests.prime_request.format(
                rids=_Constants.moscow_rids,
                msku=','.join(str(i) for i in [_BlueOffers.dropship_msku.sku, _BlueOffers.express_dropship_msku.sku]),
            )
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'wareId': offer.waremd5,
                        'delivery': {'isExpress': is_express},
                    }
                    for offer, is_express in [
                        (_BlueOffers.dropship_offer, False),
                        (_BlueOffers.express_dropship_offer, True),
                    ]
                ]
            },
        )

    def test_exp_flag_filtering_sku_offers(self):
        """
        Проверяем, что экспресс-офферы всегда включаются в выдачу place=sku_offers
        """
        response = self.report.request_json(
            _Requests.sku_offers_request.format(
                rids=_Constants.moscow_rids,
                msku=','.join(str(i) for i in [_BlueOffers.dropship_msku.sku, _BlueOffers.express_dropship_msku.sku]),
            )
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'sku',
                        'id': str(msku.sku),
                        'offers': {
                            'items': [
                                {'entity': 'offer', 'wareId': offer.waremd5, 'delivery': {'isExpress': is_express}}
                            ]
                        },
                    }
                    for offer, msku, is_express in [
                        (_BlueOffers.dropship_offer, _BlueOffers.dropship_msku, False),
                        (_BlueOffers.express_dropship_offer, _BlueOffers.express_dropship_msku, True),
                    ]
                ]
            },
        )

    def test_exp_flag_filtering_actual_delivery(self):
        """
        Проверяем, что экспресс-офферы всегда включаются в выдачу place=actual_delivery
        """
        for offer, is_express in [
            (_BlueOffers.dropship_offer, False),
            (_BlueOffers.express_dropship_offer, True),
        ]:
            response = self.report.request_json(
                _Requests.actual_delivery_request.format(
                    rids=_Constants.moscow_rids,
                    offers='{}:1'.format(offer.waremd5),
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'deliveryGroup',
                            'delivery': {'options': [{'isExpress': is_express}]},
                            'offers': [{'entity': 'offer', 'wareId': offer.waremd5}],
                        }
                    ]
                },
            )

    def test_fake_express_offer(self):
        """
        Проверяем, что если для фейкового оффера передать экспресс склад, то
        доставка будет посчитана для него как для экспресс оффера
        """
        fake_offer_param = '3BA3nE89ISkh60sD0j4XNw:1;w:5;d:30x30x30;p:700;wh:{};ff:0;ffWh:{}'

        response = self.report.request_json(
            _Requests.actual_delivery_request.format(
                rids=_Constants.moscow_rids,
                offers=fake_offer_param.format(
                    _Constants._ExpressPartners.dropship_warehouse_id, _Constants._ExpressPartners.dropship_warehouse_id
                ),
            )
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'deliveryGroup',
                        'delivery': {'options': [{'isExpress': True}]},
                        'offers': [],
                        'fakeOffers': [
                            {
                                'entity': 'fakeOffer',
                                'wareId': '3BA3nE89ISkh60sD0j4XNw',
                            }
                        ],
                    }
                ]
            },
        )

    def test_fake_express_offer_delivery_route(self):
        """
        Проверяем, что если для фейкового оффера передать экспресс склад, то
        доставка будет посчитана для него как для экспресс оффера по маршруту delivery_route
        """
        fake_offer_param = '3BA3nE89ISkh60sD0j4XNw:1;w:5;d:30x30x30;p:700;wh:{};ff:0;ffWh:{}'

        response = self.report.request_json(
            _Requests.delivery_route_request.format(
                rids=_Constants.moscow_rids,
                offers=fake_offer_param.format(
                    _Constants._ExpressPartners.dropship_warehouse_id, _Constants._ExpressPartners.dropship_warehouse_id
                ),
                date=self.delivery_interval_string(
                    _Constants._Partners.combinator_date_from,
                    time(15, 0),
                    _Constants._Partners.combinator_date_to,
                    time(16, 0),
                ),
            )
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'deliveryGroup',
                        'delivery': {'option': {'isExpress': True}},
                        'offers': [],
                        'fakeOffers': [
                            {
                                'entity': 'fakeOffer',
                                'wareId': '3BA3nE89ISkh60sD0j4XNw',
                            }
                        ],
                    }
                ]
            },
        )

    def test_delivery_price(self):
        '''
        Проверяем цену доставки
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        # У обычного офера цена доставки 99 рублей
        response = self.report.request_json(
            _Requests.actual_delivery_request.format(
                rids=_Constants.moscow_rids, offers='{}:1'.format(_BlueOffers.dropship_offer.waremd5)
            )
            + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'deliveryGroup',
                        'delivery': {
                            'options': [{'price': {'value': '77'}}],
                        },
                    }
                ]
            },
        )

        for rid, offer_price, delivery_price in [
            # В Москве цена доставки не зависит от цены корзины
            (_Constants.moscow_rids, 1000, 350),
            (_Constants.moscow_rids, 1200, 350),
            # В Екб цена доставки зависит от цены корзины
            (_Constants.ekb_rids, 1000, 150),
            (_Constants.ekb_rids, 1200, 250),
        ]:
            response = self.report.request_json(
                _Requests.actual_delivery_request.format(
                    rids=rid, offers='{}:1'.format(_BlueOffers.express_dropship_offer.waremd5)
                )
                + '&total-price={}'.format(offer_price)
                + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'deliveryGroup',
                            'offers': [
                                {
                                    'entity': 'offer',
                                    'wareId': _BlueOffers.express_dropship_offer.waremd5,
                                }
                            ],
                            'delivery': {'options': [{'price': {'value': str(delivery_price)}}]},
                        }
                    ]
                },
            )

        response = self.report.request_json(
            _Requests.sku_offers_request.format(
                rids=_Constants.moscow_rids,
                msku=','.join(str(i) for i in [_BlueOffers.dropship_msku.sku, _BlueOffers.express_dropship_msku.sku]),
            )
            + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'sku',
                        'id': str(msku.sku),
                        'offers': {
                            'items': [
                                {
                                    'entity': 'offer',
                                    'wareId': offer.waremd5,
                                    'delivery': {'options': [{'price': {'value': str(delivery_price)}}]},
                                }
                            ]
                        },
                    }
                    for offer, msku, delivery_price in [
                        (_BlueOffers.dropship_offer, _BlueOffers.dropship_msku, 77),
                        (_BlueOffers.express_dropship_offer, _BlueOffers.express_dropship_msku, 350),
                    ]
                ]
            },
        )

    # See https://st.yandex-team.ru/MARKETOUT-40954
    # todo: filter-express-delivery deprecated, remove which checks its appearance in 'filters' it should be applied but not rendered
    def test_filter_order(self):
        """
        Проверяем порядок сортировки фильтров
        """

        def get_custom_time(date_time):
            # since we test shop in moscow region - we should convert time to the moscow region time zone
            date_time -= timedelta(hours=3)
            return '&rearr-factors=market_promo_datetime={}'.format(calendar.timegm(date_time.timetuple()))

        show_old_filter_express_delivery_enabled = (
            '&rearr-factors=show_old_filter_express_delivery=1;enable_dsbs_filter_by_delivery_interval=1'
        )

        response = self.report.request_json(
            _Requests.white_prime_request.format(rids=_Constants.moscow_rids, hyper_id=_Constants.category_id)
            + '&filter-express-delivery=1'
            + get_custom_time(datetime(2021, 3, 1, 11, 0))
            + '&glfilters-order=express'
            + show_old_filter_express_delivery_enabled
        )
        self.assertFragmentIn(
            response, {"filters": [{"id": "filter-express-delivery"}, {"id": "glprice"}]}, preserve_order=True
        )

    def test_filter_express_offers(self):
        """
        Проверяем, что экспресс-офферы фильтруются при задании параметра filter-express-delivery
        """

        def get_custom_time(date_time):
            # since we test shop in moscow region - we should convert time to the moscow region time zone
            date_time -= timedelta(hours=3)
            return '&rearr-factors=market_promo_datetime={}'.format(calendar.timegm(date_time.timetuple()))

        filter_express_cgi = '&filter-express-delivery={}'
        show_old_filter_express_delivery_enabled = '&rearr-factors=show_old_filter_express_delivery=1'
        for has_filter in [0, 1]:
            req = (
                _Requests.white_prime_request.format(rids=_Constants.moscow_rids, hyper_id=_Constants.category_id)
                + "&gps=lat:8.0;lon:8.0"
                + filter_express_cgi.format(has_filter)
                + "&rearr-factors="
                + make_rearr(
                    market_hyperlocal_context_mmap_version=3,
                    market_promo_datetime=str(get_custom_date_time(datetime(2021, 3, 1, 11, 0))),
                )
            )

            response = self.report.request_json(req)

            if has_filter:
                # обычный при задании фильтра пропадает
                self.assertFragmentNotIn(
                    response, {'results': [{'entity': 'offer', 'wareId': _BlueOffers.dropship_offer.waremd5}]}
                )
                # CPC оффер тоже должен пропадать
                self.assertFragmentNotIn(
                    response, {'results': [{'entity': 'offer', 'shop': _Constants._Partners.usual_white_fesh}]}
                )
            else:
                self.assertFragmentIn(
                    response, {'results': [{'entity': 'offer', 'wareId': _BlueOffers.dropship_offer.waremd5}]}
                )
                self.assertFragmentIn(
                    response,
                    {
                        "filters": [
                            {
                                "id": "delivery-interval",
                                "type": "boolean",
                                "values": [
                                    {"value": "12", "found": 1},
                                    {"value": "0", "found": 1},
                                    {"value": "1", "found": 1},
                                ],
                            }
                        ],
                        "search": {
                            "results": [
                                {
                                    "delivery": {
                                        "hasPickup": False,
                                    },
                                    "wareId": "ExpressDropshipWaremdw",
                                }
                            ],
                        },
                    },
                    preserve_order=False,
                )
                # Должен быть первым в списке фильтров
                self.assertEqual(response['filters'][0]['id'], 'fastest-delivery-12')

            # оффер с быстрой доставкой в не зависимости от фильтра остается на выдаче
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': _BlueOffers.express_dropship_offer.waremd5,
                            'delivery': {'isExpress': True},
                        }
                    ]
                },
            )

        response = self.report.request_json(
            _Requests.white_prime_request.format(rids=_Constants.moscow_rids, hyper_id=_Constants.category_id)
            + filter_express_cgi.format(1)
            + get_custom_time(datetime(2021, 3, 1, 11, 0))
            + '&rearr-factors=market_metadoc_search=offers'
            + show_old_filter_express_delivery_enabled
        )
        self.assertFragmentIn(response, {'results': NotEmptyList()})

    # todo: filter-express-delivery deprecated, remove which checks its appearance in 'filters' it should be applied but not rendered
    def test_hide_filters_express_delivery(self):
        """
        Проверяем, что экспресс-офферы фильтруются при задании параметра filter-express-delivery-today=1
        """

        # комбинатор отвечает что склад может доставить товар СЕГОДНЯ
        # пользователь не поставил фильтр
        # оффер есть на выдаче, фильтр есть на выдаче (он непустой)
        # также проверяем формат выдачи фильтра
        response = self.report.request_json(
            "place=prime&rids=213&regset=2&offerid=ExpressDropshipWaremdw&gps=lat:4.0;lon:4.0&rearr-factors="
            + make_rearr(
                market_hyperlocal_context_mmap_version=3,
                new_delivery_interval_today=True,
                show_old_filter_express_delivery=True,
                enable_dsbs_filter_by_delivery_interval=1,
            )
        )

        self.assertFragmentNotIn(
            response,
            {
                'filters': [
                    {
                        'id': "filter-express-delivery-today",
                        'type': "boolean",
                        'name': "Express",
                        'values': [
                            {'value': "1", 'found': 1},  # сегодня оферы есть
                            {'value': "0", 'found': 0},  # не-сегодня оферов нет
                        ],
                    },
                ],
                'search': {
                    'results': [
                        {
                            'entity': "offer",
                            'wareId': _BlueOffers.express_dropship_offer.waremd5,
                        },
                    ],
                },
            },
        )

        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {
                        'id': "filter-express-delivery",
                        'type': "boolean",
                        'name': "Express",
                        'values': [
                            {'value': "1", 'found': 1},  # сегодня оферы есть
                            {'value': "0", 'found': 0},  # не-сегодня оферов нет
                        ],
                    },
                ],
                'search': {
                    'results': [
                        {
                            'entity': "offer",
                            'wareId': _BlueOffers.express_dropship_offer.waremd5,
                        },
                    ],
                },
            },
        )

        response = self.report.request_json(
            "place=prime&rids=213&regset=2&offerid=ExpressDropshipWaremdw&gps=lat:5.0;lon:5.0&rearr-factors="
            + make_rearr(
                market_hyperlocal_context_mmap_version=3,
                new_delivery_interval_today=True,
                show_old_filter_express_delivery=False,
                enable_dsbs_filter_by_delivery_interval=1,
            )
        )

        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {
                        'id': "filter-express-delivery-today",
                        'type': "boolean",
                        'name': "Express",
                        'values': [
                            {'value': "1", 'found': 1},  # сегодня оферы есть
                            {'value': "0", 'found': 0},  # не-сегодня оферов нет
                        ],
                    },
                ],
                'search': {
                    'results': [
                        {
                            'entity': "offer",
                            'wareId': _BlueOffers.express_dropship_offer.waremd5,
                        },
                    ],
                },
            },
        )

        self.assertEqual(
            response['filters'][0]['id'], "filter-express-delivery-today"
        )  # должен быть первым в списке фильтров

    def test_filter_express_delivery_today(self):
        """
        Проверяем, что экспресс-офферы фильтруются при задании параметра filter-express-delivery-today=1
        """

        # комбинатор отвечает что склад может доставить товар СЕГОДНЯ
        # пользователь не поставил фильтр
        # оффер есть на выдаче, фильтр есть на выдаче (он непустой)
        # также проверяем формат выдачи фильтра
        response = self.report.request_json(
            "place=prime&rids=213&regset=2&offerid=ExpressDropshipWaremdw&gps=lat:5.0;lon:5.0&rearr-factors="
            + make_rearr(
                market_hyperlocal_context_mmap_version=3,
                new_delivery_interval_today=True,
                show_old_filter_express_delivery=False,
            )
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': "offer",
                            'wareId': _BlueOffers.express_dropship_offer.waremd5,
                        },
                    ],
                },
            },
        )

        # комбинатор отвечает что склад может доставить товар СЕГОДНЯ
        # пользователь поставил фильтр доставка СЕГОДНЯ
        # оффер есть на выдаче, фильтр есть на выдаче и выбран
        response = self.report.request_json(
            "place=prime&rids=213&regset=2&offerid=ExpressDropshipWaremdw&gps=lat:5.0;lon:5.0&filter-express-delivery-today=1&rearr-factors="
            + make_rearr(
                market_hyperlocal_context_mmap_version=3,
                new_delivery_interval_today=True,
                show_old_filter_express_delivery=False,
            )
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': "offer",
                            'wareId': _BlueOffers.express_dropship_offer.waremd5,
                        },
                    ],
                },
            },
        )

        # комбинатор отвечает что склад может доставить товар ЗАВТРА
        # пользователь поставил фильтр доставка СЕГОДНЯ
        # оффера нет на выдаче
        response = self.report.request_json(
            "place=prime&rids=213&regset=2&offerid=ExpressDropshipWaremdw&gps=lat:2.0;lon:2.0&filter-express-delivery-today=1&rearr-factors="
            + make_rearr(market_hyperlocal_context_mmap_version=3, new_delivery_interval_today=True)
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 0,
                },
            },
        )

        # комбинатор отвечает что склад может доставить товар ЗАВТРА
        # пользователь поставил фильтр доставка СЕГОДНЯ
        # фильтр выключен флагом стоп-крана
        # оффер есть на выдаче, фильтра нет на выдаче (он выключен)
        response = self.report.request_json(
            "place=prime&rids=213&regset=2&offerid=ExpressDropshipWaremdw&gps=lat:2.0;lon:2.0&filter-express-delivery-today=1&rearr-factors="
            + make_rearr(
                market_hyperlocal_context_mmap_version=3,
                new_delivery_interval_today=True,
                new_filter_express_delivery_today=False,
            )
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': "offer",
                            'wareId': _BlueOffers.express_dropship_offer.waremd5,
                        },
                    ],
                },
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'filters': [
                    {
                        'id': "filter-express-delivery-today",
                    }
                ],
            },
        )

        # комбинатор отвечает что склад может доставить товар ЗАВТРА
        # пользователь не поставил фильтр
        # оффер есть на выдаче, фильтра нет на выдаче (он пустой)
        response = self.report.request_json(
            "place=prime&rids=213&regset=2&offerid=ExpressDropshipWaremdw&gps=lat:2.0;lon:2.0&rearr-factors="
            + make_rearr(
                market_hyperlocal_context_mmap_version=3,
                new_delivery_interval_today=True,
                show_old_filter_express_delivery=False,
            )
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': "offer",
                            'wareId': _BlueOffers.express_dropship_offer.waremd5,
                        },
                    ],
                },
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'filters': [
                    {
                        'id': "filter-express-delivery-today",
                    }
                ],
            },
        )

    def test_filter_express_offers_without_partners_file(self):
        """
        Проверяем, что экспресс-офферы фильтруются при задании параметра filter-express-delivery
        c помощью фильтра по поисковому литералу is_express
        """

        filter_express_cgi = '&filter-express-delivery={}'
        show_old_filter_express_delivery_enabled = '&rearr-factors=show_old_filter_express_delivery=1'

        for has_filter in [0, 1]:
            response = self.report.request_json(
                _Requests.white_prime_request.format(
                    rids=_Constants.moscow_rids,
                    hyper_id=_Constants.category_wh2_id,
                )
                + filter_express_cgi.format(has_filter)
                + show_old_filter_express_delivery_enabled
            )

            if has_filter:
                # не-экспресс при задании фильтра пропадает
                self.assertFragmentNotIn(
                    response,
                    {
                        'results': [
                            {
                                'entity': 'offer',
                                'wareId': _BlueOffers.no_express_dropship_wh2_offer.waremd5,
                            }
                        ],
                    },
                )
            else:
                self.assertFragmentIn(
                    response,
                    {
                        'results': [
                            {
                                'entity': 'offer',
                                'wareId': _BlueOffers.no_express_dropship_wh2_offer.waremd5,
                                'delivery': {
                                    'isExpress': True,
                                },
                            }
                        ],
                    },
                )

            # оффер с быстрой доставкой в не зависимости от фильтра остается на выдаче
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': _BlueOffers.express_dropship_wh2_offer.waremd5,
                            'delivery': {
                                'isExpress': True,
                            },
                        }
                    ],
                },
            )

    def test_has_filter_express_offers(self):
        """
        Проверка что если есть только найденные офферы со значением фильтра 0 - скроем
        """

        response = self.report.request_json(
            _Requests.white_prime_request_filter.format(
                rids=_Constants.moscow_rids,
                hyper_id=_Constants.category_id_filter,
            )
        )

        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "id": "filter-express-delivery",
                        "type": "boolean",
                    }
                ]
            },
        )

    def test_full_filter_express_offers(self):
        """
        если в запросе указано значение фильтра как выключен filter-express-delivery=0,
        то он должен быть на выдаче, и поле values должно содержать все значения фильтра
        """

        """
        проверка что у фильтра экспресс доставки нет найденных офферов при значении value: "0"
        но это значени и данные по нему отображаются
        """
        show_old_filter_express_delivery_enabled = (
            '&rearr-factors=show_old_filter_express_delivery=1;enable_dsbs_filter_by_delivery_interval=1'
        )

        response = self.report.request_json(
            _Requests.white_prime_request_filter.format(
                rids=_Constants.moscow_rids,
                hyper_id=_Constants.category_id_filter,
            )
            + "&filter-express-delivery=0"
            + show_old_filter_express_delivery_enabled
        )

        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "isExpress": True,
                        }
                    }
                ]
            },
        )

        """
        проверка что у фильтра экспресс доставки нет найденных офферов при значении value: "1"
        но это значени и данные по нему отображаются
        """
        response = self.report.request_json(
            _Requests.white_prime_request_filter.format(
                rids=_Constants.moscow_rids,
                hyper_id=_Constants.category_wh2_id,
            )
            + "&filter-express-delivery=1"
            + show_old_filter_express_delivery_enabled
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "isExpress": True,
                        }
                    }
                ]
            },
        )

    def test_filter_express_offers_hours(self):
        """
        Проверяем что вне окон работы склада наши офера не пролезают на выдачу при market_show_express_out_of_working_hours=0
        Вермя меняем прямо в запросе с помощью флага market_promo_datetime
        """
        EXPRESS_OUT_OF_WORKING_TIME_FLAG = '&rearr-factors=market_show_express_out_of_working_hours=0'

        request_time_to_visibility = {
            # (сегодня НЕ рабочий день магазина)                 НЕТ
            # (сегодня рабочий день магазина) И
            # (сейчас рабочее время)                              ДА
            # (сегодня рабочий день магазина) И
            # (сейчас НЕ рабочее время) И
            # (рабочее время НЕ началось)                         ДА
            # (сегодня рабочий день магазина) И
            # (сейчас НЕ рабочее время) И
            # (рабочее время закончилось) И
            # (завтра будет рабочий день магазина)                ДА
            # (сегодня рабочий день магазина) И
            # (сейчас НЕ рабочее время) И
            # (рабочее время закончилось) И
            # (завтра будет НЕ рабочий день магазина)             НЕТ
            datetime(2021, 3, 7, 12, 12): False,  # sunday
            datetime(2021, 3, 1, 11, 59): True,  # monday
            datetime(2021, 3, 3, 9, 59): True,  # wednesday too wearly
            datetime(2021, 3, 3, 10, 1): True,  # wednesday right after open
            datetime(2021, 3, 5, 18, 1): False,  # friday out of time
            # проверки на границы открытия/закрытия и ночная доставка
            datetime(2021, 3, 3, 18, 59): True,  # wednesday
            datetime(2021, 3, 1, 10, 00): True,  # monday
            datetime(2021, 3, 5, 18, 00): True,  # friday
        }

        for hide_warehouses in [1, 0]:
            for request_time, is_visible in request_time_to_visibility.items():

                def get_custom_time(date_time):
                    # since we test shop in moscow region - we should convert time to the moscow region time zone
                    date_time -= timedelta(hours=3)
                    return '&rearr-factors=market_promo_datetime={};hide_express_offers_if_warehouse_out_of_working_hours={}'.format(
                        calendar.timegm(date_time.timetuple()), hide_warehouses
                    )

                response = self.report.request_json(
                    _Requests.prime_request.format(
                        rids=_Constants.moscow_rids,
                        msku=str(_BlueOffers.express_dropship_msku_with_schedule.sku),
                    )
                    + get_custom_time(request_time)
                    + EXPRESS_OUT_OF_WORKING_TIME_FLAG
                )

                if is_visible or not hide_warehouses:
                    self.assertFragmentIn(
                        response,
                        {
                            'results': [
                                {
                                    'entity': 'offer',
                                    'wareId': _BlueOffers.express_dropship_offer_with_schedule.waremd5,
                                }
                            ]
                        },
                    )
                else:
                    self.assertFragmentNotIn(
                        response,
                        {
                            'results': [
                                {
                                    'entity': 'offer',
                                    'wareId': _BlueOffers.express_dropship_offer_with_schedule.waremd5,
                                }
                            ]
                        },
                    )

    def test_express_offers_delivery_filter(self):

        # комбинатор отвечает что склад может доставить товар СЕГОДНЯ
        # пользователь поставил фильтр доставка СЕГОДНЯ
        # оффер есть на выдаче
        response = self.report.request_json(
            "place=prime&rgb=white&rids=213&regset=2&offerid=ExpressDropshipWaremdw&gps=lat:3.0;lon:3.0&delivery_interval=0&rearr-factors="
            + make_rearr(market_hyperlocal_context_mmap_version=3, new_delivery_interval_today=True)
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'wareId': _BlueOffers.express_dropship_offer.waremd5,
                    }
                ]
            },
        )

        # комбинатор отвечает что склад может доставить товар ЗАВТРА
        # пользователь поставил фильтр доставка СЕГОДНЯ
        # оффера нет на выдаче
        response = self.report.request_json(
            "place=prime&rgb=white&rids=213&regset=2&offerid=ExpressDropshipWaremdw&gps=lat:2.0;lon:2.0&delivery_interval=0&rearr-factors="
            + make_rearr(market_hyperlocal_context_mmap_version=3, new_delivery_interval_today=True)
        )

        self.assertFragmentIn(
            response,
            {
                "total": 0,
                "results": [],
            },
            allow_different_len=False,
        )

        # комбинатор отвечает что склад может доставить товар ЗАВТРА
        # пользователь не указывал сроки доставки
        # оффер есть на выдаче
        response = self.report.request_json(
            "place=prime&rids=213&regset=2&offerid=ExpressDropshipWaremdw&gps=lat:3.0;lon:3.0&rearr-factors="
            + make_rearr(market_hyperlocal_context_mmap_version=3, new_delivery_interval_today=True)
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'wareId': _BlueOffers.express_dropship_offer.waremd5,
                    }
                ]
            },
        )

    def test_filter_express_offers_holdays(self):
        """
        Проверяем что в праздничные дни офферы не пролезают на выдачу
        Вермя меняем прямо в запросе с помощью флага market_promo_datetime
        """
        EXPRESS_OUT_OF_WORKING_TIME_FLAG = '&rearr-factors=market_show_express_out_of_working_hours=0'

        request_time_to_visibility = {
            # на сегодня есть расписание работы
            # но прадничный день
            # значит сегодня не рабочий день
            # до открыти
            datetime(2021, 3, 8, 0, 1): False,  # monday
            # во время работы
            datetime(2021, 3, 8, 12, 0): False,  # monday
            # после закрытия
            datetime(2021, 3, 8, 23, 59): False,  # monday
            # (сегодня рабочий день магазина) И
            # (сейчас НЕ рабочее время) И
            # (рабочее время еще не началось) И
            # (завтра будет праздник)
            datetime(2021, 3, 9, 1, 0): True,  # tuesday
            # (сегодня рабочий день магазина) И
            # (сейчас рабочее время) И
            # (завтра будет праздник)
            datetime(2021, 3, 9, 12, 0): True,  # tuesday
            # (сегодня рабочий день магазина) И
            # (сейчас НЕ рабочее время) И
            # (рабочее время закончилось) И
            # (завтра будет праздник)
            datetime(2021, 3, 9, 23, 0): False,  # tuesday
        }

        for hide_warehouses in [1, 0]:
            for request_time, is_visible in request_time_to_visibility.items():

                def get_custom_time(date_time):
                    # since we test shop in moscow region - we should convert time to the moscow region time zone
                    date_time -= timedelta(hours=3)
                    return '&rearr-factors=market_promo_datetime={};hide_express_offers_if_warehouse_out_of_working_hours={}'.format(
                        calendar.timegm(date_time.timetuple()), hide_warehouses
                    )

                response = self.report.request_json(
                    _Requests.prime_request.format(
                        rids=_Constants.moscow_rids,
                        msku=str(_BlueOffers.express_dropship_msku_with_schedule.sku),
                    )
                    + get_custom_time(request_time)
                    + EXPRESS_OUT_OF_WORKING_TIME_FLAG
                )

                if is_visible or not hide_warehouses:
                    self.assertFragmentIn(
                        response,
                        {
                            'results': [
                                {
                                    'entity': 'offer',
                                    'wareId': _BlueOffers.express_dropship_offer_with_schedule.waremd5,
                                }
                            ]
                        },
                    )
                else:
                    self.assertFragmentNotIn(
                        response,
                        {
                            'results': [
                                {
                                    'entity': 'offer',
                                    'wareId': _BlueOffers.express_dropship_offer_with_schedule.waremd5,
                                }
                            ]
                        },
                    )

    def test_filter_express_offers_hours_with_flag(self):
        """
        Проверяем что при market_show_express_out_of_working_hours=1 экспресс офферы
        всегда присутствуют на выдаче, даже  в нерабочее время склада (для заказа на следующий рабочий день)
        """
        EXPRESS_OUT_OF_WORKING_TIME_FLAG = '&rearr-factors=market_show_express_out_of_working_hours=1'

        request_time_to_visibility = {
            datetime(2021, 3, 1, 11, 0),  # monday
            datetime(2021, 3, 3, 13, 0),  # wednesday
            datetime(2021, 3, 3, 10, 0),  # wednesday right after open
            datetime(2021, 3, 3, 9, 59),  # wednesday too wearly
            datetime(
                2021, 3, 3, 17, 1
            ),  # wednesday, it is working hour but we stop show offers 1 hour early before closing
            datetime(2021, 3, 3, 18, 1),  # wednesday out of time
            datetime(2021, 3, 7, 13, 10),  # sunday
            datetime(2021, 3, 7, 0, 0),  # sunday midnight
        }

        for request_time in request_time_to_visibility:

            def get_custom_time(date_time):
                # since we test shop in moscow region - we should convert time to the moscow region time zone
                date_time -= timedelta(hours=3)
                return '&rearr-factors=market_promo_datetime={}'.format(calendar.timegm(date_time.timetuple()))

            response = self.report.request_json(
                _Requests.prime_request.format(
                    rids=_Constants.moscow_rids,
                    msku=str(_BlueOffers.express_dropship_msku_with_schedule.sku),
                )
                + get_custom_time(request_time)
                + EXPRESS_OUT_OF_WORKING_TIME_FLAG
            )

            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': _BlueOffers.express_dropship_offer_with_schedule.waremd5,
                        }
                    ]
                },
            )

    def test_current_work_schedule(self):
        """
        Проверяем, что в ответе репорта для экпресс поставщиков есть график работы в день запроса
        """

        def get_custom_time(date_time):
            # since we test shop in moscow region - we should convert time to the moscow region time zone
            date_time -= timedelta(hours=3)
            return '&rearr-factors=market_promo_datetime={}'.format(calendar.timegm(date_time.timetuple()))

        response = self.report.request_json(
            _Requests.shop_info_request.format(
                rids=_Constants.moscow_rids, fesh=_Constants._ExpressPartners.dropship_fesh_with_schedule
            )
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'workScheduleList': [
                            {'day': day, 'from': {'hour': 10, 'minute': 0}, 'to': {'hour': 18, 'minute': 0}}
                            for day in range(5)
                        ],
                        'currentWorkSchedule': {'from': {'hour': 10, 'minute': 0}, 'to': {'hour': 18, 'minute': 0}},
                    }
                ],
            },
        )

        response = self.report.request_json(
            _Requests.business_info_request.format(
                rids=_Constants.moscow_rids, fesh=_Constants._ExpressPartners.dropship_fesh_with_schedule
            )
        )

        self.assertFragmentIn(
            response,
            {
                'shop_info': [
                    {
                        'workScheduleList': [
                            {'day': day, 'from': {'hour': 10, 'minute': 0}, 'to': {'hour': 18, 'minute': 0}}
                            for day in range(5)
                        ],
                        'currentWorkSchedule': {'from': {'hour': 10, 'minute': 0}, 'to': {'hour': 18, 'minute': 0}},
                    }
                ],
            },
        )

        response = self.report.request_json(
            _Requests.prime_request.format(
                rids=_Constants.moscow_rids, msku=str(_BlueOffers.express_dropship_msku_with_schedule.sku)
            )
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'supplier': {
                            'workScheduleList': [
                                {'day': day, 'from': {'hour': 10, 'minute': 0}, 'to': {'hour': 18, 'minute': 0}}
                                for day in range(5)
                            ],
                            'currentWorkSchedule': {'from': {'hour': 10, 'minute': 0}, 'to': {'hour': 18, 'minute': 0}},
                        }
                    }
                ]
            },
        )

        request_time_to_visibility = {datetime(2021, 3, 7, 13, 0), datetime(2021, 3, 6, 10, 0)}  # sunday  # saturday
        # Проверяем, что в нерабочие дни магазина расписание не появляется на выдаче
        for request_time in request_time_to_visibility:
            response = self.report.request_json(
                _Requests.shop_info_request.format(
                    rids=_Constants.moscow_rids, fesh=_Constants._ExpressPartners.dropship_fesh_with_schedule
                )
                + get_custom_time(request_time)
            )

            self.assertFragmentNotIn(
                response,
                {
                    'results': [
                        {
                            'workScheduleList': [
                                {'day': day, 'from': {'hour': 10, 'minute': 0}, 'to': {'hour': 18, 'minute': 0}}
                                for day in range(5)
                            ],
                            'currentWorkSchedule': {'from': {'hour': 10, 'minute': 0}, 'to': {'hour': 18, 'minute': 0}},
                        }
                    ],
                },
            )

            response = self.report.request_json(
                _Requests.business_info_request.format(
                    rids=_Constants.moscow_rids, fesh=_Constants._ExpressPartners.dropship_fesh_with_schedule
                )
                + get_custom_time(request_time)
            )

            self.assertFragmentNotIn(
                response,
                {
                    'shop_info': [
                        {
                            'workScheduleList': [
                                {'day': day, 'from': {'hour': 10, 'minute': 0}, 'to': {'hour': 18, 'minute': 0}}
                                for day in range(5)
                            ],
                            'currentWorkSchedule': {'from': {'hour': 10, 'minute': 0}, 'to': {'hour': 18, 'minute': 0}},
                        }
                    ],
                },
            )

            response = self.report.request_json(
                _Requests.prime_request.format(
                    rids=_Constants.moscow_rids,
                    msku=str(_BlueOffers.express_dropship_msku_with_schedule.sku),
                )
                + get_custom_time(request_time)
            )

            self.assertFragmentNotIn(
                response,
                {
                    'results': [
                        {
                            'supplier': {
                                'workScheduleList': [
                                    {'day': day, 'from': {'hour': 10, 'minute': 0}, 'to': {'hour': 18, 'minute': 0}}
                                    for day in range(5)
                                ],
                                'currentWorkSchedule': {
                                    'from': {'hour': 10, 'minute': 0},
                                    'to': {'hour': 18, 'minute': 0},
                                },
                            }
                        }
                    ]
                },
            )

    def test_nearest_delivery_from_combinator(self):
        """
        Проверяем, что без стоп-крана мы начали подклеивать точный интервал экспресса к оферной выдаче.
        Также проверяем, что флаг стоп-крана это отключает
        """

        REARR_SWITCH_OFF = "&rearr-factors=express_nearest_time_interval_enabled=0"

        for request in [
            "place=prime&rgb=white&rids=213&regset=2&offerid=ExpressDropshipWaremdw&gps=lat:1.0;lon:1.0",
            "place=offerinfo&rgb=white&rids=213&regset=2&offerid=ExpressDropshipWaremdw&gps=lat:1.0;lon:1.0",
        ]:
            for switched_off in [True, False]:
                response = self.report.request_json(request + (REARR_SWITCH_OFF if switched_off else ""))

                self.assertFragmentIn(
                    response,
                    {
                        'results': [
                            {
                                'entity': 'offer',
                                'wareId': _BlueOffers.express_dropship_offer.waremd5,
                                'delivery': {
                                    'options': [
                                        {
                                            'isDefault': True,
                                            'timeIntervals': Absent()
                                            if switched_off
                                            else [
                                                {
                                                    'from': "12:30",
                                                    'to': "13:45",
                                                    'isDefault': True,
                                                }
                                            ],
                                        }
                                    ],
                                },
                            }
                        ]
                    },
                )

    def test_exp_flag_filtering_delivery_route(self):
        """
        Проверяем, что поле isExpress появляется в place=delivery_route
        в delivery.option по аналогии с actual_delivery
        """
        response = self.report.request_json(
            _Requests.delivery_route_request.format(
                rids=_Constants.moscow_rids,
                offers='{}:1'.format(_BlueOffers.dropship_offer.waremd5),
                date=self.delivery_interval_string(
                    _Constants._Partners.combinator_date_from,
                    time(15, 0),
                    _Constants._Partners.combinator_date_to,
                    time(16, 0),
                ),
            )
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'deliveryGroup',
                        'delivery': {'option': {'isExpress': False}},
                        'offers': [{'entity': 'offer', 'wareId': _BlueOffers.dropship_offer.waremd5}],
                    }
                ]
            },
        )

        response = self.report.request_json(
            _Requests.delivery_route_request.format(
                rids=_Constants.moscow_rids,
                offers='{}:1'.format(_BlueOffers.express_dropship_offer.waremd5),
                date=self.delivery_interval_string(
                    _Constants._ExpressPartners.combinator_date_from,
                    time(15, 0),
                    _Constants._ExpressPartners.combinator_date_to,
                    time(16, 0),
                ),
            )
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'deliveryGroup',
                        'delivery': {'option': {'isExpress': True}},
                        'offers': [{'entity': 'offer', 'wareId': _BlueOffers.express_dropship_offer.waremd5}],
                    }
                ]
            },
        )

    def test_delivery_partner_types(self):
        """
        Проверяем, что у оффера в службе доствкине пустое поле deliveryPartnerTypes
        """
        response = self.report.request_json(
            _Requests.delivery_route_request.format(
                rids=_Constants.moscow_rids,
                offers='{}:1'.format(_BlueOffers.dropship_offer.waremd5),
                date=self.delivery_interval_string(
                    _Constants._Partners.combinator_date_from,
                    time(15, 0),
                    _Constants._Partners.combinator_date_to,
                    time(16, 0),
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
                            'deliveryPartnerTypes': ["YANDEX_MARKET"],
                        },
                    }
                ]
            },
        )

    def test_payment_methods(self):
        """
        Проверяем, для экспресс офферов отрываюся все способы оплаты, кроме PT_YANDEX
        """
        for place_request, multiply_options in (
            (_Requests.delivery_route_request, False),
            (_Requests.actual_delivery_request, True),
        ):
            response = self.report.request_json(
                place_request.format(
                    rids=_Constants.moscow_rids,
                    offers='{}:1'.format(_BlueOffers.dropship_offer.waremd5),
                    date=self.delivery_interval_string(
                        _Constants._Partners.combinator_date_from,
                        time(15, 0),
                        _Constants._Partners.combinator_date_to,
                        time(16, 0),
                    ),
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'deliveryGroup',
                            'delivery': (
                                {
                                    'options': [
                                        {
                                            'paymentMethods': [
                                                Payment.to_dc_type(pm)
                                                for pm in (
                                                    Payment.PT_YANDEX,
                                                    Payment.PT_CASH_ON_DELIVERY,
                                                    Payment.PT_CARD_ON_DELIVERY,
                                                )
                                            ]
                                        }
                                    ]
                                }
                                if multiply_options
                                else {
                                    'option': {
                                        'paymentMethods': [
                                            Payment.to_dc_type(pm)
                                            for pm in (
                                                Payment.PT_YANDEX,
                                                Payment.PT_CASH_ON_DELIVERY,
                                                Payment.PT_CARD_ON_DELIVERY,
                                            )
                                        ]
                                    }
                                }
                            ),
                            'offers': [{'entity': 'offer', 'wareId': _BlueOffers.dropship_offer.waremd5}],
                        }
                    ]
                },
            )
            response = self.report.request_json(
                place_request.format(
                    rids=_Constants.moscow_rids,
                    offers='{}:1'.format(_BlueOffers.express_dropship_offer.waremd5),
                    date=self.delivery_interval_string(
                        _Constants._ExpressPartners.combinator_date_from,
                        time(15, 0),
                        _Constants._ExpressPartners.combinator_date_to,
                        time(16, 0),
                    ),
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'deliveryGroup',
                            'delivery': (
                                {'options': [{'paymentMethods': [Payment.to_dc_type(Payment.PT_YANDEX)]}]}
                                if multiply_options
                                else {'option': {'paymentMethods': [Payment.to_dc_type(Payment.PT_YANDEX)]}}
                            ),
                            'offers': [{'entity': 'offer', 'wareId': _BlueOffers.express_dropship_offer.waremd5}],
                        }
                    ]
                },
            )

    def test_express_not_filtered_by_delivery_interval(self):
        for value in [0, 1, 2, 3, 5]:
            response = self.report.request_json(
                _Requests.prime_request.format(
                    rids=_Constants.moscow_rids, msku=str(_BlueOffers.express_dropship_msku_2.sku)
                )
                + "&delivery_interval={}".format(value)
                + "&gps=lat:9.0;lon:9.0"
                + "&rearr-factors="
                + make_rearr(
                    market_hyperlocal_context_mmap_version=3,
                    honest_express_filter=True,
                    market_promo_datetime=str(get_custom_date_time(datetime(2021, 3, 4, 12, 0))),
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            'entity': 'offer',
                            'delivery': {'isExpress': True},
                        }
                    ]
                },
            )

    def test_fastest_delievery_interval(self):
        for value in ["0", "1", "5", "12"]:
            response = self.report.request_json(
                _Requests.prime_request.format(
                    rids=_Constants.moscow_rids, msku=str(_BlueOffers.express_dropship_msku_2.sku)
                )
                + "&user_filter=fastest-delivery-{}:1".format(value)
                + "&gps=lat:8.0;lon:8.0"
                + "&rearr-factors="
                + make_rearr(
                    market_hyperlocal_context_mmap_version=3,
                    enable_dsbs_filter_by_delivery_interval=2,
                    honest_express_filter=True,
                    fastest_delivery_interval=1,
                    market_promo_datetime=str(get_custom_date_time(datetime(2021, 3, 4, 12, 0))),
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            'entity': 'offer',
                            'delivery': {'isExpress': True},
                        }
                    ]
                },
            )

            self.assertFragmentIn(
                response,
                {
                    "id": "delivery-interval",
                    "name": "Срок доставки",
                    "values": [{"checked": True, "value": value}],
                },
            )

            self.assertFragmentNotIn(
                response,
                {
                    "id": "fastest-delivery-{}".format(value),
                    "filterOrigin": _FilterOrigin.USER_FILTER,
                },
            )

    def test_express_filter_with_delivery_interval(self):
        # Проверяем взаимодействие фильтра экспресс доставки с фильтром delivery-interval
        # enable_dsbs_filter_by_delivery_interval = 0, фильтр express_delivery отображается
        # enable_dsbs_filter_by_delivery_interval = 1, фильтр express_delivery отображается, в фильтре delivery-interval есть експресс доставка со сначением 12
        # enable_dsbs_filter_by_delivery_interval = 2, фильтр express_delivery НЕ отображается, в фильтре delivery-interval есть експресс доставка со сначением 12
        for flag_value in (0, 1, 2):
            response = self.report.request_json(
                _Requests.prime_request.format(
                    rids=_Constants.moscow_rids, msku=str(_BlueOffers.express_dropship_msku_2.sku)
                )
                + "&gps=lat:{gps};lon:{gps}".format(gps=6 + flag_value)
                + "&rearr-factors="
                + make_rearr(
                    market_hyperlocal_context_mmap_version=3,
                    enable_dsbs_filter_by_delivery_interval=flag_value,
                    honest_express_filter=True,
                    market_promo_datetime=str(get_custom_date_time(datetime(2021, 3, 4, 12, 0))),
                )
            )

            if flag_value == 0:
                self.assertFragmentIn(
                    response,
                    {
                        "id": "filter-express-delivery-today",
                        "values": [
                            {"value": "1", "found": 1},
                        ],
                    },
                )

                self.assertFragmentNotIn(
                    response,
                    {
                        "id": "delivery-interval",
                        "name": "Срок доставки",
                        "values": [{"value": "12", "found": 1}],
                    },
                )

            if flag_value == 1:
                self.assertFragmentIn(
                    response,
                    {
                        "id": "filter-express-delivery-today",
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
                        "values": [{"value": "0", "found": 1}, {"value": "12", "found": 1}],
                    },
                )

            if flag_value == 2:
                self.assertFragmentNotIn(response, {"id": "filter-express-delivery-today"})

                self.assertFragmentIn(
                    response,
                    {
                        "id": "delivery-interval",
                        "name": "Срок доставки",
                        "values": [{"value": "0", "found": 1}, {"value": "12", "found": 1}],
                    },
                )

    def test_fastest_delivery(self):
        # Проверяем отображение фильтра самая быстрая доставка
        # Должен быть такой же как и первый пункт в фильтре срок доставки
        # Меняем за счет флага enable_dsbs_filter_by_delivery_interval
        for flag_value in (0, 2):
            response = self.report.request_json(
                _Requests.prime_request.format(
                    rids=_Constants.moscow_rids, msku=str(_BlueOffers.express_dropship_msku_2.sku)
                )
                + "&gps=lat:{gps};lon:{gps}".format(gps=6 + flag_value)
                + "&rearr-factors="
                + make_rearr(
                    market_hyperlocal_context_mmap_version=3,
                    enable_dsbs_filter_by_delivery_interval=flag_value,
                    honest_express_filter=True,
                    fastest_delivery_interval=1,
                    market_promo_datetime=str(get_custom_date_time(datetime(2021, 3, 4, 12, 0))),
                )
            )

            if flag_value == 0:
                self.assertFragmentIn(
                    response,
                    {
                        "id": "fastest-delivery-0",
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
                            {"value": "0", "found": 1},
                        ],
                    },
                )

            if flag_value == 2:
                self.assertFragmentIn(
                    response,
                    {
                        "id": "fastest-delivery-12",
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
                            {"name": "1-3 часа", "value": "12", "found": 1},
                        ],
                    },
                )

    def test_delivery_price_unified_tariffs(self):
        '''
        Проверяем цену доставки с новыми едиными тарифами
        '''
        unified_tariffs_flag = '&rearr-factors=market_unified_tariffs=1'
        ya_plus_perk = '&perks=yandex_plus'
        # У обычного офера цена доставки 77 рублей, на него флаг и экспресс тарифы не влияют
        response = self.report.request_json(
            _Requests.actual_delivery_request.format(
                rids=_Constants.moscow_rids, offers='{}:1'.format(_BlueOffers.dropship_offer.waremd5)
            )
            + unified_tariffs_flag
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'deliveryGroup',
                        'delivery': {
                            'options': [{'price': {'value': '77'}}],
                        },
                    }
                ]
            },
        )

        for rid, offer_price, delivery_price, has_cheaper_delivery in [
            # В Москве цена доставки зависит от цены корзины
            (_Constants.moscow_rids, 1000, 199, True),
            (_Constants.moscow_rids, 1200, 39, True),
            # В Екб цена доставки не зависит от цены корзины
            (_Constants.ekb_rids, 1000, 350, False),
            (_Constants.ekb_rids, 1200, 350, False),
        ]:
            response = self.report.request_json(
                _Requests.actual_delivery_request.format(
                    rids=rid, offers='{}:1'.format(_BlueOffers.express_dropship_offer.waremd5)
                )
                + '&total-price={}'.format(offer_price)
                + unified_tariffs_flag
                + ya_plus_perk
            )
            remainder = 1200 - offer_price
            self.assertFragmentIn(
                response,
                {
                    'cheaperDeliveryThreshold': {'currency': 'RUR', 'value': '1200'}
                    if has_cheaper_delivery
                    else Absent(),
                    'cheaperDeliveryRemainder': {'currency': 'RUR', 'value': str(remainder) if remainder > 0 else "0"}
                    if has_cheaper_delivery
                    else Absent(),
                    'results': [
                        {
                            'entity': 'deliveryGroup',
                            'offers': [
                                {
                                    'entity': 'offer',
                                    'wareId': _BlueOffers.express_dropship_offer.waremd5,
                                }
                            ],
                            'delivery': {'options': [{'price': {'value': str(delivery_price)}}]},
                        }
                    ],
                },
            )

    def test_delivery_price_dynamic_tariffs(self):
        '''
        ...
        '''
        express_new_logistics_revenue_from_merchant_disabled = (
            "&rearr-factors=express_new_logistics_revenue_from_merchant_enabled=none"
        )
        #   ====================== given =======================, ===================== expected ======================
        for (
            exp_split,
            ware_md5,
            in_ekb,
            parcel_price,
            with_plus,
            delivery_price,
            delivery_threshold,
            delivery_reminder,
        ) in [
            ("static-split-1", "ExpressDropshipWaremdw", True, 1000, False, 199, 1200, 200),  # noqa
            ("static-split-1", "ExpressDropshipWaremdw", True, 1200, False, 99, None, None),  # noqa
            ("static-split-1", "ExpressDropshipWaremdw", True, 1000, True, 139, 1200, 200),  # noqa
            ("static-split-1", "ExpressDropshipWaremdw", True, 1200, True, 39, None, None),  # noqa
            ("default", "ExpressDropshipWaremdw", True, 698, False, 199, 699, 1),  # noqa
            ("default", "ExpressDropshipWaremdw", True, 699, False, 149, None, None),  # noqa
            ("default", "ExpressDropshipWaremdw", True, 698, True, 149, 699, 1),  # noqa
            ("default", "ExpressDropshipWaremdw", True, 699, True, 99, None, None),  # noqa
            (None, "ExpressDropshipWaremdw", True, 698, False, 350, None, None),  # noqa
            (None, "ExpressDropshipWaremdw", True, 1200, False, 350, None, None),  # noqa
            (None, "ExpressDropshipWaremdw", True, 698, True, 350, None, None),  # noqa
            (None, "ExpressDropshipWaremdw", True, 1200, True, 350, None, None),  # noqa
            ("dynamic-default", "ExpressDropshipWaremdw", True, 700, False, 49, None, None),  # noqa
            ("dynamic-split-1", "ExpressDropshipCheapdw", True, 70, False, 99, None, None),  # noqa
            ("dynamic-split-1", "ExpressDropshipExpendw", True, 70000, False, 299, None, None),  # noqa
            ("dynamic-default", "ExpressDropshipWaremdw", True, 700, True, 0, None, None),  # noqa
            ("dynamic-split-1", "ExpressDropshipCheapdw", True, 70, True, 49, None, None),  # noqa
            ("dynamic-split-1", "ExpressDropshipExpendw", True, 70000, True, 249, None, None),  # noqa
            ("dynamic-split-2", "ExpressDropshipWaremdw", True, 700, True, 99, None, None),  # noqa
            ("dynamic-split-2", "ExpressDropshipCheapdw", True, 70, True, 149, None, None),  # noqa
            ("dynamic-split-2", "ExpressDropshipExpendw", True, 70000, True, 0, None, None),  # noqa
            ("dynamic-with-target-ue", "ExpressDropshipCheapdw", True, 70, False, 99, None, None),  # noqa
            ("dynamic-with-target-ue", "ExpressDropshipWaremdw", True, 700, False, 149, None, None),  # noqa
            ("dynamic-with-target-ue", "ExpressDropshipExpendw", True, 70000, False, 549, None, None),  # noqa
            ("dynamic-with-target-ue", "ExpressDropshipCheapdw", True, 70, True, 49, None, None),  # noqa
            ("dynamic-with-target-ue", "ExpressDropshipWaremdw", True, 700, True, 99, None, None),  # noqa
            ("dynamic-with-target-ue", "ExpressDropshipExpendw", True, 70000, True, 499, None, None),  # noqa
        ]:
            response = self.report.request_json(
                _Requests.actual_delivery_request.format(
                    offers="{}:1".format(ware_md5),
                    rids=_Constants.ekb_rids if in_ekb else _Constants.moscow_rids,
                )
                + ("&rearr-factors=express_unit_economy_experiment_split={}".format(exp_split) if exp_split else "")
                + express_new_logistics_revenue_from_merchant_disabled
                + "&total-price={}".format(parcel_price)
                + ("&perks=yandex_plus" if with_plus else "")
            )

            self.assertFragmentIn(
                response,
                {
                    'cheaperDeliveryThreshold': {'currency': 'RUR', 'value': str(delivery_threshold)}
                    if delivery_threshold is not None
                    else Absent(),
                    'cheaperDeliveryRemainder': {'currency': 'RUR', 'value': str(delivery_reminder)}
                    if delivery_reminder is not None
                    else Absent(),
                    'results': [
                        {
                            'entity': "deliveryGroup",
                            'offers': [
                                {
                                    'entity': "offer",
                                    'wareId': ware_md5,
                                }
                            ],
                            'delivery': {'options': [{'price': {'value': str(delivery_price)}}]},
                        }
                    ],
                },
            )

    def test_delivery_price_dynamic_tariffs_delivery_route(self):
        '''
        проверяем что delivery_route выдает такую же стоимость доставки как и actual_delivery
        для экспрессных офферов при расчете стоимости по динамическим тарифам
        '''
        express_new_logistics_revenue_from_merchant_disabled = (
            "&rearr-factors=express_new_logistics_revenue_from_merchant_enabled=none"
        )
        for (exp_split, ware_md5, rid, parcel_price, delivery_price,) in [
            (
                "dynamic-with-target-ue",
                _BlueOffers.express_dropship_offer_cheap.waremd5,
                _Constants.moscow_rids,
                _BlueOffers.express_dropship_offer_cheap.price,
                49,
            ),  # noqa
            (
                "dynamic-with-target-ue",
                _BlueOffers.express_dropship_offer_expensive.waremd5,
                _Constants.ekb_rids,
                _BlueOffers.express_dropship_offer_expensive.price,
                499,
            ),  # noqa
        ]:
            for with_plus in [True, False]:
                response = self.report.request_json(
                    _Requests.actual_delivery_request.format(
                        offers="{}:1".format(ware_md5),
                        rids=rid,
                    )
                    + "&rearr-factors=express_unit_economy_experiment_split={}".format(exp_split)
                    + "&total-price={}".format(parcel_price)
                    + ("&perks=yandex_plus" if with_plus else "")
                    + express_new_logistics_revenue_from_merchant_disabled
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
                                'delivery': {
                                    'options': [{'price': {'value': str(delivery_price + (0 if with_plus else 50))}}]
                                },
                            },
                        ],
                    },
                )

                response = self.report.request_json(
                    _Requests.delivery_route_request.format(
                        offers="{}:1".format(ware_md5),
                        rids=213,
                        date=self.delivery_interval_string(
                            _Constants._Partners.combinator_date_from,
                            time(15, 0),
                            _Constants._Partners.combinator_date_to,
                            time(16, 0),
                        ),
                    )
                    + "&rearr-factors=express_unit_economy_experiment_split={}".format(exp_split)
                    + ("&perks=yandex_plus" if with_plus else "")
                    + express_new_logistics_revenue_from_merchant_disabled
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
                                'delivery': {
                                    'option': {'price': {'value': str(delivery_price + (0 if with_plus else 50))}}
                                },
                            }
                        ],
                    },
                )

    def test_dynamic_tariffs_delivery_route_with_wide_slot(self):
        '''
        Проверяем что delivery_route отдает признак isWideExpress в опции доставки
        для экспрессных офферов с широкими слотмами при расчете стоимости по динамическим тарифам
        https://st.yandex-team.ru/MARKETOUT-47395
        '''
        for (is_wide, ware_md5, delivery_price_with_plus, delivery_price_without_plus, time_from, time_to) in [
            (
                False,
                _BlueOffers.express_dropship_offer.waremd5,
                0,
                49,
                time(15, 0),
                time(16, 0),
            ),
            (
                True,
                _BlueOffers.express_dropship_offer_with_wide_slot.waremd5,
                99,
                149,
                time(17, 0),
                time(18, 0),
            ),
        ]:
            for with_plus in (True, False):
                response = self.report.request_json(
                    _Requests.delivery_route_request.format(
                        offers="{}:1".format(ware_md5),
                        rids=_Constants.moscow_rids,
                        date=self.delivery_interval_string(
                            _Constants._Partners.combinator_date_from,
                            time_from,
                            _Constants._Partners.combinator_date_to,
                            time_to,
                        ),
                    )
                    + "&rearr-factors=express_unit_economy_experiment_split=dynamic-split-2"
                    + ("&perks=yandex_plus" if with_plus else "")
                    + "&rearr-factors=enable_wide_express_courier_options=1"
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
                                'delivery': {
                                    'option': {
                                        'price': {
                                            'value': str(
                                                delivery_price_with_plus if with_plus else delivery_price_without_plus
                                            )
                                        },
                                        'isWideExpress': True if is_wide else False,
                                    }
                                },
                            }
                        ],
                    },
                )

    def test_delivery_price_dynamic_tariffs_with_new_logistics_cost_from_merchant(self):
        '''
        Проверяем работу по расчету динамического тарифа доставки, где тариф мерчанту считается не константой,
        а процентом от стоимости оффера. Все таким образом подсчитанные logistics_revenue_from_merchant
        суммируются для офферов в посылке.

        Статический и быстрый офферы добавлены в тест, чтобы убедиться, что изменения точно не аффектят эти сплиты юнит-экономики.
        https://st.yandex-team.ru/MARKETOUT-45632
        '''
        express_new_logistics_revenue_from_merchant_enabled = (
            "&rearr-factors=express_new_logistics_revenue_from_merchant_enabled=all"
        )
        #   ====================== given =======================, ===================== expected ======================
        for (exp_split, ware_md5, in_ekb, parcel_price, delivery_price, delivery_threshold, delivery_reminder,) in [
            ("static-split-2", "ExpressDropshipWaremdw", True, 1000, 149, 1200, 200),  # noqa
            ("default", "ExpressDropshipWaremdw", True, 698, 149, 699, 1),  # noqa
            ("default", "ExpressDropshipWaremdw", True, 699, 99, None, None),  # noqa
            (None, "ExpressDropshipWaremdw", True, 1200, 350, None, None),  # noqa
            ("dynamic-split-1", "ExpressDropshipCheapdw", True, 70, 99, None, None),  # noqa
            ("dynamic-split-1", "ExpressDropshipExpendw", True, 70000, 249, None, None),  # noqa
            ("dynamic-default", "ExpressDropshipWaremdw", True, 700, 49, None, None),  # noqa
            ("dynamic-split-1", "ExpressDropshipCheapdw", True, 70, 99, None, None),  # noqa
            ("dynamic-split-2", "ExpressDropshipWaremdw", True, 700, 49, None, None),  # noqa
            ("dynamic-split-2", "ExpressDropshipCheapdw", True, 70, 99, None, None),  # noqa
            ("dynamic-split-2", "ExpressDropshipExpendw", True, 70000, 0, None, None),  # noqa
            ("dynamic-with-target-ue", "ExpressDropshipCheapdw", True, 70, 99, None, None),  # noqa
            ("dynamic-with-target-ue", "ExpressDropshipWaremdw", True, 700, 199, None, None),  # noqa
            ("dynamic-with-target-ue", "ExpressDropshipExpendw", True, 70000, 499, None, None),  # noqa
            ("dynamic-default", "ExpressDropshipHeavy_w", True, 700, 0, None, None),  # noqa
            ("dynamic-default", "ExpressDropshipBigSz_w", True, 70, 0, None, None),  # noqa
        ]:
            for with_plus in (True, False):
                response = self.report.request_json(
                    _Requests.actual_delivery_request.format(
                        offers="{}:1".format(ware_md5),
                        rids=_Constants.ekb_rids if in_ekb else _Constants.moscow_rids,
                    )
                    + ("&rearr-factors=express_unit_economy_experiment_split={}".format(exp_split) if exp_split else "")
                    + express_new_logistics_revenue_from_merchant_enabled
                    + "&total-price={}".format(parcel_price)
                    + ("&perks=yandex_plus" if with_plus else "")
                )

                if exp_split is None and not with_plus:
                    with_plus = True

                if not delivery_price and not with_plus:
                    delivery_price -= 1

                self.assertFragmentIn(
                    response,
                    {
                        'cheaperDeliveryThreshold': {'currency': 'RUR', 'value': str(delivery_threshold)}
                        if delivery_threshold is not None
                        else Absent(),
                        'cheaperDeliveryRemainder': {'currency': 'RUR', 'value': str(delivery_reminder)}
                        if delivery_reminder is not None
                        else Absent(),
                        'results': [
                            {
                                'entity': "deliveryGroup",
                                'offers': [
                                    {
                                        'entity': "offer",
                                        'wareId': ware_md5,
                                    }
                                ],
                                'delivery': {
                                    'options': [{'price': {'value': str(delivery_price + (0 if with_plus else 50))}}]
                                },
                            }
                        ],
                    },
                )

    def test_delivery_price_dynamic_tariffs_for_wide_slots(self):
        '''
        Проверяем работу калькулятора по расчету динамического тарифа доставки для экспрессных широких слотов
        https://st.yandex-team.ru/MARKETOUT-47395
        '''
        #   ====================== given =======================, ===================== expected ======================
        for (exp_split, ware_md5, is_wide, delivery_price,) in [
            ("dynamic-default", "ExpressDropshipWaremdw", False, 49),
            ("dynamic-default", "ExpressDropshipWidemdw", True, 49),
            ("dynamic-split-2", "ExpressDropshipWaremdw", False, 49),
            ("dynamic-split-2", "ExpressDropshipWidemdw", True, 99),
        ]:
            for with_plus in (True, False):
                response = self.report.request_json(
                    _Requests.actual_delivery_request.format(
                        offers="{}:1".format(ware_md5),
                        rids=_Constants.ekb_rids,
                    )
                    + "&rearr-factors=express_unit_economy_experiment_split={}".format(exp_split)
                    + "&rearr-factors=enable_wide_express_courier_options=1"
                    + "&total-price={}".format(_BlueOffers.express_dropship_msku.price)
                    + ("&perks=yandex_plus" if with_plus else "")
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
                                'delivery': {
                                    'options': [
                                        {
                                            'price': {'value': str(delivery_price + (0 if with_plus else 50))},
                                            'isWideExpress': True if is_wide else False,
                                        }
                                    ]
                                },
                            }
                        ],
                    },
                )

    def test_express_delivery_dynamic_radius(self):
        '''
        Проверяем, что работает логика скрытия оферов по динамическому радиусу юнит-экономики экспресс-доставки

        ВАЖНО: Временно тестируем "макет" логики:
        Если расстояние от склада до пользователя превышает границу 20 км и выполнены условия флагов,
        то экспресс-оферы отфильтровываются
        https://st.yandex-team.ru/MARKETOUT-43753
        '''

        EXPRESS_SUPPLIER = str(_Constants._ExpressPartners.dropship_fesh) + ","

        EXPRESS_WAREMD5 = _BlueOffers.express_dropship_offer.waremd5
        EXPRESS_MSKU = _BlueOffers.express_dropship_msku.sku

        EXP_70K_WAREMD5 = _BlueOffers.express_dropship_offer_expensive.waremd5
        EXP_70K_MSKU = _BlueOffers.express_dropship_msku_2.sku

        EXTRA_REQUEST_TEMPLATE = (
            "&debug=1"
            "&gps={user_gps}"
            "&rearr-factors="
            "express_dynamic_radius_enabled={rearr_enabled}"
            ";express_dynamic_radius_enabled_for_suppliers={rearr_for_suppliers}"
            ";express_unit_economy_experiment_split={exp_split}"
        )

        def expected_entity(ware_md5, entity_type):
            def expected_offer(ware_md5, with_delivery=True):
                offer_entity = {
                    'entity': 'offer',
                    'wareId': ware_md5,
                }
                if with_delivery:
                    offer_entity['delivery'] = {
                        'isExpress': True,
                    }
                return offer_entity

            ENTITIES = {
                'offer': expected_offer(ware_md5),
                'sku': {
                    'entity': 'sku',
                    'offers': {
                        'items': [
                            expected_offer(ware_md5),
                        ],
                    },
                },
                'deliveryGroup': {
                    'entity': 'deliveryGroup',
                    'offers': [
                        expected_offer(ware_md5, with_delivery=False),
                    ],
                    'delivery': {
                        'isExpress': True,
                    },
                },
            }

            return ENTITIES[entity_type]

        #     ================================================= given =================================================, === expected ==          # noqa
        for (
            exp_split,
            rearr_enabled,
            rearr_for_suppliers,
            msku,
            ware_md5,
            user_gps,
            is_offer_hidden,
        ) in [  # noqa  # warehouse_gps = lat:55.7;lon:37.7
            (
                "dynamic-default",
                "none",
                "",
                EXPRESS_MSKU,
                EXPRESS_WAREMD5,
                "lat:55.50;lon:37.50",
                False,
            ),  # noqa  # rearr off - offer NOT hidden
            (
                "dynamic-default",
                "all",
                "",
                EXPRESS_MSKU,
                EXPRESS_WAREMD5,
                "lat:55.50;lon:37.50",
                True,
            ),  # noqa  # rearr on, all suppliers, ~25 km - offer hidden
            (
                "dynamic-default",
                "all",
                "",
                EXPRESS_MSKU,
                EXPRESS_WAREMD5,
                "lat:55.69;lon:37.69",
                False,
            ),  # noqa  # rearr on, all suppliers, ~1.2 km - offer NOT hidden
            (
                "dynamic-default",
                "all",
                "",
                EXP_70K_MSKU,
                EXP_70K_WAREMD5,
                "lat:54.80;lon:36.80",
                False,
            ),  # noqa  # rearr on, all suppliers, ~118 km - EXPENSIVE NOT hidden (нам выгодно повезти дорогой товар хоть и далеко)
            (
                "dynamic-default",
                "all",
                "",
                EXP_70K_MSKU,
                EXP_70K_WAREMD5,
                "lat:55.50;lon:37.50",
                False,
            ),  # noqa  # rearr on, all suppliers, ~25 km - EXPENSIVE offer NOT hidden
            (
                "dynamic-default",
                "all",
                "",
                EXP_70K_MSKU,
                EXP_70K_WAREMD5,
                "lat:55.60;lon:37.60",
                False,
            ),  # noqa  # rearr on, all suppliers, ~12 km - EXPENSIVE offer NOT hidden
            (
                "dynamic-default",
                "specified",
                "",
                EXPRESS_MSKU,
                EXPRESS_WAREMD5,
                "lat:55.50;lon:37.50",
                False,
            ),  # noqa  # rearr on, empty suppliers - offer NOT hidden
            (
                "dynamic-default",
                "specified",
                EXPRESS_SUPPLIER,
                EXPRESS_MSKU,
                EXPRESS_WAREMD5,
                "lat:55.50;lon:37.50",
                True,
            ),  # noqa  # rearr on, EXPRESS supplier, ~25 km - offer hidden
            (
                "dynamic-default",
                "specified",
                EXPRESS_SUPPLIER,
                EXPRESS_MSKU,
                EXPRESS_WAREMD5,
                "lat:55.69;lon:37.69",
                False,
            ),  # noqa  # rearr on, EXPRESS supplier, ~1.2 km - offer NOT hidden
            (
                "dynamic-default",
                "all",
                EXPRESS_SUPPLIER,
                EXPRESS_MSKU,
                EXPRESS_WAREMD5,
                "lat:55.65;lon:37.60",
                True,
            ),  # noqa  # rearr on, all suppliers, ~12 km - EXPRESS offer hidden when used new_logistics_revenue_from_merchant: https://st.yandex-team.ru/MARKETOUT-45821
        ]:
            for (request, entity_type,) in [  # noqa
                (
                    _Requests.prime_request,
                    'offer',
                ),  # noqa
                (
                    _Requests.sku_offers_request,
                    'sku',
                ),  # noqa
                (
                    _Requests.productoffers_request,
                    'offer',
                ),  # noqa
                (
                    _Requests.actual_delivery_request,
                    'deliveryGroup',
                ),  # noqa
            ]:
                response = self.report.request_json(
                    (request + EXTRA_REQUEST_TEMPLATE).format(
                        rids=_Constants.moscow_rids,
                        msku=msku,
                        offers=(ware_md5 + ":1"),
                        user_gps=user_gps,
                        rearr_enabled=rearr_enabled,
                        rearr_for_suppliers=rearr_for_suppliers,
                        exp_split=exp_split,
                    )
                )

                if is_offer_hidden:
                    self.assertFragmentNotIn(response, expected_entity(ware_md5, entity_type))
                    self.assertFragmentIn(
                        response,
                        {
                            'brief': {
                                'filters': {
                                    'EXPRESS_DYNAMIC_RADIUS_EXCEEDED': NotEmpty(),
                                }
                            }
                        },
                    )
                else:
                    self.assertFragmentIn(response, expected_entity(ware_md5, entity_type))

    @classmethod
    def prepare_collapsed_offer(cls):
        """
        модель с 1 экспресс оффером, и 2 не экспресс офферами
        """
        cls.index.models += [
            Model(hid=1, hyperid=1),
        ]

        """
        модель без экспресс офферов
        """
        cls.index.models += [
            Model(hid=222, hyperid=222),
        ]

        cls.index.shops += [
            Shop(fesh=2, priority_region=213, regions=[225], name="Белый магазин"),
        ]

        cls.index.offers += [
            Offer(hyperid=222, fesh=2, waremd5='BH8EPLtKmdLQhLUasgaOnA', ts=208, sku=15),
            Offer(
                title='подмышник хозяйственный', hyperid=222, fesh=2, waremd5='KXGI8T3GP_pqjgdd7HfoHQ', ts=209, sku=16
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=222,
                sku=17,
                blue_offers=[BlueOffer(price=2100, feedid=11, waremd5='yRgmzyBD4j8r4rkCby6Iuw', ts=210)],
            ),
        ]

    def test_express_model_blender(self):

        flag_collapsed = '&rearr-factors=market_nordstream=0'

        request = (
            'hid={}&place=blender&use-default-offers=1&numdoc=20&'
            'allow-collapsing=1&show-models-specs=msku-friendly,msku-full&'
            'show-urls=productVendorBid%2Cexternal%2Cgeo%2CgeoShipping%2Ccpa%2CshowPhone%2Coffercard%2Ccpa'
        )

        response = self.report.request_json(request.format("1,222") + flag_collapsed)

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 1,
                'hasExpressOffer': True,
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 222,
                'hasExpressOffer': False,
            },
        )

    def test_express_model_modelinfo(self):

        flag_collapsed = '&rearr-factors=market_nordstream=0'

        request = (
            'hyperid={}&place=modelinfo&use-default-offers=1&rids=0&'
            'allow-collapsing=1&show-models-specs=msku-friendly,msku-full'
        )

        response = self.report.request_json(request.format("1,222") + flag_collapsed)

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 1,
                'hasExpressOffer': True,
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 222,
                'hasExpressOffer': False,
            },
        )

    def test_express_model_if_express_offer_lose_buybox_fight(self):
        """
        У модели два оффера - экспресс и обычный. Байбокс выигрывает обычный.
        С реарр-флагом у модели есть признак наличия экспресса, если экспресс оффер прошёл фильтры.
        """
        request = (
            'hid={hid}&place=blender&use-default-offers=1'
            '&rids=213'
            '&rearr-factors=market_blue_buybox_max_price_rel_add_diff=0;dynamic_stats_update_has_express_through_buybox={enable_update}'
        )

        response = self.report.request_json(
            request.format(hid=_Constants.category_id_for_buybox_fight, enable_update=False)
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': _Constants.category_id_for_buybox_fight,
                'hasExpressOffer': False,
            },
        )

        request_with_upper_price = request + '&mcpriceto={}'.format(
            _BlueOffers.express_dropship_offer_for_buybox_fight.price - 1
        )
        response = self.report.request_json(
            request_with_upper_price.format(hid=_Constants.category_id_for_buybox_fight, enable_update=True)
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': _Constants.category_id_for_buybox_fight,
                'hasExpressOffer': False,
            },
        )

        response = self.report.request_json(
            request.format(hid=_Constants.category_id_for_buybox_fight, enable_update=True)
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': _Constants.category_id_for_buybox_fight,
                'hasExpressOffer': True,
            },
        )

    def test_product_offers_express_cpa_benefit(self):

        cheapest = {
            'search': {
                'results': [
                    {
                        'entity': 'offer',
                        'benefit': {
                            'type': 'cheapest',
                        },
                        'wareId': _BlueOffers.dropship_offer.waremd5,
                    }
                ]
            }
        }

        express_cpa = {
            'search': {
                'results': [
                    {
                        'entity': 'offer',
                        'benefit': {
                            'type': 'express-cpa',
                        },
                        'wareId': _BlueOffers.express_dropship_offer.waremd5,
                    }
                ]
            }
        }

        response = self.report.request_json('place=productoffers&rids=213&hyperid=1&pp=6&offers-set=defaultList')
        self.assertFragmentIn(response, cheapest)
        self.assertFragmentIn(response, express_cpa)

        self.click_log.expect(pp=294)
        self.show_log.expect(pp=294)

        response = self.report.request_json(
            'place=productoffers&rids=213&hyperid=1&offers-set=defaultList'
            '&rearr-factors=enable_express_cpa_benefit=1'
        )
        self.assertFragmentIn(response, cheapest)
        self.assertFragmentIn(response, express_cpa)

        response = self.report.request_json(
            'place=productoffers&rids=213&hyperid=1&offers-set=defaultList'
            '&rearr-factors=enable_express_cpa_benefit=0'
        )
        self.assertFragmentIn(response, cheapest)
        self.assertFragmentNotIn(response, express_cpa)

    def test_product_offers_express_cpa_touch_pp(self):
        self.report.request_json('place=productoffers&rids=213&hyperid=1&pp=6&offers-set=defaultList&touch=1')
        self.click_log.expect(pp=694)
        self.show_log.expect(pp=694)

    def test_product_offers_express_cpa_android_pp(self):
        self.report.request_json('place=productoffers&rids=213&hyperid=1&pp=6&offers-set=defaultList&client=ANDROID')
        self.click_log.expect(pp=1794)
        self.show_log.expect(pp=1794)

    def test_product_offers_express_cpa_ios_pp(self):
        self.report.request_json('place=productoffers&rids=213&hyperid=1&pp=6&offers-set=defaultList&client=IOS')
        self.click_log.expect(pp=1894)
        self.show_log.expect(pp=1894)

    def test_collections_if_express_delivery(self):
        """Проверяем какие коллекции запрашиваются при поиске с &filter-express-delivery=1"""

        query = 'place=prime&text=test&filter-express-delivery=1&debug=1'

        # по умолчанию
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'report': {
                        'context': {
                            'collections': {
                                'SHOP': NotEmpty(),
                                'MODEL': Absent(),
                                'BOOK': NotEmpty(),
                            }
                        }
                    }
                }
            },
        )

        # под флагом market_no_models_for_express_delivery=0 ищем в MODEL
        # https://st.yandex-team.ru/MARKETOUT-41042
        response = self.report.request_json(query + '&rearr-factors=market_no_models_for_express_delivery=0')
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'report': {
                        'context': {
                            'collections': {
                                'SHOP': NotEmpty(),
                                'MODEL': NotEmpty(),
                                'BOOK': NotEmpty(),
                            }
                        }
                    }
                }
            },
        )

    @classmethod
    def prepare_dj_for_express(cls):
        cls.settings.set_default_reqid = False

        cls.dj.on_request(
            exp='go_express_retargeting_block', djid='go_express_retargeting_block', rids=213, yandexuid='001'
        ).respond([DjModel(id=(10000 + i)) for i in range(20)])
        cls.index.mskus += [
            MarketSku(
                hyperid=(10000 + i),
                sku=(6782 + i),
                blue_offers=[
                    BlueOffer(
                        price=1000 + i,
                        feedid=_Constants._ExpressPartners.dropship_feed_id,
                        is_express=True,
                        supplier_id=_Constants._ExpressPartners.dropship_fesh,
                        delivery_buckets=[_Constants._ExpressPartners.courier_bucket_id],
                        waremd5='yRgmzyBD4j8r4rkCby6I' + '{:02d}'.format(i),
                    )
                ],  # Add express offer in 213 region for each model
            )
            for i in range(20)
        ]
        cls.index.models += [Model(hyperid=(10000 + i)) for i in range(20)]

    def test_dj_express_delivery_uses_rids_and_numdoc(self):
        response = self.report.request_json(
            "place=dj&dj-place=this_place_must_not_be_used&djid=go_express_retargeting_block&cpa=real&rids=213"
            "&numdoc=12&yandexuid=001&filter-express-delivery=1"
        )

        self.assertFragmentIn(
            response,
            {'results': [{'entity': 'product', 'id': 10000 + i} for i in range(12)]},
            preserve_order=True,
            allow_different_len=False,
        )

    def test_factor(self):
        """Тестируем наличие фактора COURIER_DELIVERY_EXPRESS"""

        response = self.report.request_json('place=prime&allow-collapsing=0&rids=213&hyperid=1&debug=da')
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {'raw': 'Экспресс dropship оффер'},
                'wareId': _BlueOffers.express_dropship_offer.waremd5,
                'debug': {'factors': {'COURIER_DELIVERY_EXPRESS': '1'}},
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {'raw': 'Обычный dropship оффер'},
                'wareId': _BlueOffers.dropship_offer.waremd5,
                'debug': {'factors': {'COURIER_DELIVERY_EXPRESS': Absent()}},
            },
        )

    @classmethod
    def prepare_prohibited_offers(cls):
        '''
        Подготавливаем список запрещенный офферов
        '''
        cls.index.prohibited_blue_offers += [
            ProhibitedBlueOffers(region_id=213, categories=[_Constants.category_id_prohibited]),
        ]

    def test_prohibited_offers(self):
        """Тестируем что белые и экспрессные оффера игнорируют запрещенные категории"""

        response = self.report.request_json(
            'place=prime&allow-collapsing=0&rids=213&hyperid={}&debug=da'.format(_Constants.category_id_prohibited)
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': _BlueOffers.express_prohibited_offer.waremd5,
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'offer',
                'wareId': _BlueOffers.not_express_prohibited_offer.waremd5,
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': _WhiteOffers.prohibited_white_offer.waremd5,
            },
        )

    @classmethod
    def prepare_also_viewed_filtrations(cls):
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_EXPRESS,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(params={'version': 'ANALOGS2_EXPRESS'}, splits=[{}]),
                ],
            )
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=(29461 + i),
                sku=(7638 + i),
                blue_offers=[
                    BlueOffer(
                        price=1000 + i,
                        feedid=_Constants._ExpressPartners.dropship_feed_id,
                        is_express=True,
                        supplier_id=_Constants._ExpressPartners.dropship_fesh,
                        delivery_buckets=[_Constants._ExpressPartners.courier_bucket_id],
                        waremd5='also_viewed_4rkCby6I' + '{:02d}'.format(i),
                    )
                ],  # Add express offer in 213 region for each model
            )
            for i in range(2)
        ]
        cls.index.models += [Model(hyperid=(29461 + i)) for i in range(2)]
        cls.recommender.on_request_accessory_models(
            model_id=29461, item_count=1000, version='ANALOGS2_EXPRESS'
        ).respond({'models': ['29462']})

    def test_also_viewed_express(self):
        response = self.report.request_json(
            'place=also_viewed&hyperid=29461&debug=1&yamarec-place-id=also-viewed-express&filter-express-delivery=1&rids=213'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'entity': 'product',
                            'id': 29462,
                        }
                    ],
                }
            },
        )

    def test_shopdata_is_express(self):
        # Проверяем, что магазни передается признак "IsExpress", Если склад тоже экспрессный
        response = self.report.request_json("place=shop_info&fesh=3&rids=213")

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "isExpress": True,
                    },
                ]
            },
        )


if __name__ == '__main__':
    main()
