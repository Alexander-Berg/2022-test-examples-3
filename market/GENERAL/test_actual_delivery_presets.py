#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Shop, Outlet, Offer, Region, DynamicWarehouseInfo, DynamicWarehouseToWarehouseInfo

from core.logs import ErrorCodes
from core.matcher import Absent, Contains, EmptyList
from core.testcase import TestCase, main
from core.types.offer import OfferDimensions
from core.types.payment_methods import Payment, PaymentMethod
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax
from core.types.combinator import (
    CombinatorOffer,
    create_delivery_option,
    create_virtual_box,
    DeliveryItem,
    PickupPointGrouped,
    Destination,
)
from core.types import DynamicDeliveryTariff
from core.types.delivery import (
    DeliveryBucket,
    DeliveryOption,
    OutletType,
    PickupBucket,
    PickupOption,
    RegionalDelivery,
)

import datetime


TODAY = datetime.datetime.today()
DATE_FROM = TODAY + datetime.timedelta(days=2)

# ========== From C++ ==========


class EDeliveryAvailability:
    """
    see arcadia/market/report/src/output/info/delivery_address.h
    """

    Available = "AVAILABLE"
    Unavailable = "UNAVAILABLE"
    OutletMissed = "OUTLET_MISSED"


class TAddressPreset:
    class EType:
        """
        see arcadia/market/report/library/cgi/types.h
        """

        Courier = "DELIVERY"
        Pickup = "PICKUP"
        Post = "POST"


class TSearchExperimentFlags:
    class EActualDeliveryPresetOptions:
        """
        see arcadia/market/report/library/search_experiment_flags/experiments.h
        """

        ADAPO_STOP = 0
        ADAPO_USE_PRESETS = 1 << 0
        ADAPO_USE_SIMPLE_FILTERS = 1 << 1
        ADAPO_USE_COMBINATOR_PICKUP = 1 << 2
        ADAPO_USE_COMBINATOR_COURIER = 1 << 3
        ADAPO_USE_COMBINATOR_POST = 1 << 4
        ADAPO_USE_ALL = (
            ADAPO_USE_PRESETS
            | ADAPO_USE_SIMPLE_FILTERS
            | ADAPO_USE_COMBINATOR_PICKUP
            | ADAPO_USE_COMBINATOR_COURIER
            | ADAPO_USE_COMBINATOR_POST
        )


# ========== Вспомогательные генераторы ==========


def id_generator(init_value=1):
    counter = init_value
    while True:
        yield counter
        counter += 1


SHOP_ID_GENERATOR = id_generator()
BUCKET_ID_GENERATOR = id_generator()
OUTLET_ID_GENERATOR = id_generator(100)

# ========== Регионы ==========

PITER_RID = 111
VASYA_OSTROV_RID = 112
PALACE_RID = 113

MOSCOW_RID = 121
HAMOVNIKY_RID = 122
CENTRAL_RID = 123

# ========== Доставка ==========

DELIVERY_SERVICE_ID_PITER = 121
DELIVERY_SERVICE_ID_MOSCOW = 123

# ========== Магазины ==========


class _Shops:
    class _Blue:
        B_1_ID = next(SHOP_ID_GENERATOR)
        B_1 = Shop(
            fesh=B_1_ID,
            datafeed_id=B_1_ID,
            priority_region=MOSCOW_RID,
            name='blue_shop_1',
            tax_system=Tax.OSN,
            supplier_type=Shop.THIRD_PARTY,
            blue=Shop.BLUE_REAL,
            warehouse_id=145,
        )

        B_2_ID = next(SHOP_ID_GENERATOR)
        B_2 = Shop(
            fesh=B_2_ID,
            datafeed_id=B_2_ID,
            priority_region=MOSCOW_RID,
            name='blue_shop_2',
            tax_system=Tax.OSN,
            supplier_type=Shop.THIRD_PARTY,
            blue=Shop.BLUE_REAL,
            warehouse_id=145,
        )

        B_PITER_1_ID = next(SHOP_ID_GENERATOR)
        B_PITER_1 = Shop(
            fesh=B_PITER_1_ID,
            datafeed_id=B_PITER_1_ID,
            priority_region=PITER_RID,
            name='blue_shop_piter_1',
            tax_system=Tax.OSN,
            supplier_type=Shop.THIRD_PARTY,
            blue=Shop.BLUE_REAL,
            warehouse_id=145,
        )

        CLICK_N_COLLECT_1_ID = next(SHOP_ID_GENERATOR)
        CLICK_N_COLLECT_1 = Shop(
            fesh=CLICK_N_COLLECT_1_ID,
            datafeed_id=CLICK_N_COLLECT_1_ID,
            client_id=1,
            warehouse_id=1,
            supplier_type=Shop.THIRD_PARTY,
            blue=Shop.BLUE_REAL,
            priority_region=MOSCOW_RID,
            fulfillment_program=False,
            ignore_stocks=True,
            name='shop_blue_click_n_collect_1',
            tax_system=Tax.OSN,
        )

        CLICK_N_COLLECT_2_ID = next(SHOP_ID_GENERATOR)
        CLICK_N_COLLECT_2 = Shop(
            fesh=CLICK_N_COLLECT_2_ID,
            datafeed_id=CLICK_N_COLLECT_2_ID,
            client_id=1,
            warehouse_id=1,
            supplier_type=Shop.THIRD_PARTY,
            blue=Shop.BLUE_REAL,
            priority_region=MOSCOW_RID,
            fulfillment_program=False,
            ignore_stocks=True,
            name='shop_blue_click_n_collect_2',
            tax_system=Tax.OSN,
        )

        CLICK_N_COLLECT_3_ID = next(SHOP_ID_GENERATOR)
        CLICK_N_COLLECT_3 = Shop(
            fesh=CLICK_N_COLLECT_3_ID,
            datafeed_id=CLICK_N_COLLECT_3_ID,
            client_id=1,
            warehouse_id=1,
            supplier_type=Shop.THIRD_PARTY,
            blue=Shop.BLUE_REAL,
            priority_region=MOSCOW_RID,
            fulfillment_program=False,
            ignore_stocks=False,  # это не попадает под условие CnC на репорте
            name='shop_blue_click_n_collect_3',
            tax_system=Tax.OSN,
            click_and_collect=True,  # А вот это отдельный флаг MBI для CnC
        )

    class _White:
        DSBS_1_ID = next(SHOP_ID_GENERATOR)
        DSBS_1 = Shop(
            fesh=DSBS_1_ID,
            datafeed_id=DSBS_1_ID,
            priority_region=MOSCOW_RID,
            name='shop_white_dsbs_1',
            tax_system=Tax.OSN,
            cpa=Shop.CPA_REAL,
        )

        DSBS_2_ID = next(SHOP_ID_GENERATOR)
        DSBS_2 = Shop(
            fesh=DSBS_2_ID,
            datafeed_id=DSBS_2_ID,
            client_id=1,
            warehouse_id=1,
            supplier_type=Shop.THIRD_PARTY,
            blue=Shop.BLUE_NO,
            priority_region=MOSCOW_RID,
            fulfillment_program=False,
            ignore_stocks=True,
            name='shop_white_dsbs_2',
            tax_system=Tax.OSN,
            cpa=Shop.CPA_REAL,
        )


# ========== Оутлеты ==========


def create_pickup_outlet(outlet_id, rid, fesh=None, delivery_service_id=DELIVERY_SERVICE_ID_MOSCOW):
    return Outlet(
        fesh=fesh,
        point_id=outlet_id,
        post_code=outlet_id,
        delivery_service_id=delivery_service_id if fesh is None else None,
        region=rid,
        point_type=Outlet.FOR_PICKUP,
        working_days=[i for i in range(30)],
        bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
    )


def create_post_outlet(post_code, rid, fesh=None, delivery_service_id=DELIVERY_SERVICE_ID_MOSCOW):
    return Outlet(
        point_id=post_code,
        fesh=fesh,
        post_code=post_code,
        delivery_service_id=delivery_service_id if fesh is None else None,
        region=rid,
        point_type=Outlet.FOR_POST,
        working_days=[i for i in range(30)],
        bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
    )


class _Outlets:
    class _Pickup:
        MOSCOW_1_ID = next(OUTLET_ID_GENERATOR)
        MOSCOW_1 = create_pickup_outlet(MOSCOW_1_ID, MOSCOW_RID)

        MOSCOW_2_ID = next(OUTLET_ID_GENERATOR)
        MOSCOW_2 = create_pickup_outlet(MOSCOW_2_ID, MOSCOW_RID)

        BLUE_SHOP_1_MOSCOW_1_ID = next(OUTLET_ID_GENERATOR)
        SHOP_B_1_MOSCOW_1 = create_pickup_outlet(BLUE_SHOP_1_MOSCOW_1_ID, MOSCOW_RID, _Shops._Blue.B_1_ID)

        BLUE_SHOP_1_MOSCOW_2_ID = next(OUTLET_ID_GENERATOR)
        SHOP_B_1_MOSCOW_2 = create_pickup_outlet(BLUE_SHOP_1_MOSCOW_2_ID, MOSCOW_RID, _Shops._Blue.B_1_ID)

        BLUE_SHOP_2_MOSCOW_1_ID = next(OUTLET_ID_GENERATOR)
        SHOP_B_2_MOSCOW_1 = create_pickup_outlet(BLUE_SHOP_2_MOSCOW_1_ID, MOSCOW_RID, _Shops._Blue.B_2_ID)

        BLUE_SHOP_2_MOSCOW_2_ID = next(OUTLET_ID_GENERATOR)
        SHOP_B_2_MOSCOW_2 = create_pickup_outlet(BLUE_SHOP_2_MOSCOW_2_ID, MOSCOW_RID, _Shops._Blue.B_2_ID)

        BLUE_SHOP_2_MOSCOW_3_ID = next(OUTLET_ID_GENERATOR)
        SHOP_B_2_MOSCOW_3 = create_pickup_outlet(BLUE_SHOP_2_MOSCOW_3_ID, MOSCOW_RID, _Shops._Blue.B_2_ID)

        BLUE_SHOP_1_PITER_1_ID = next(OUTLET_ID_GENERATOR)
        BLUE_SHOP_1_PITER_1 = create_pickup_outlet(
            BLUE_SHOP_1_PITER_1_ID, PITER_RID, _Shops._Blue.B_PITER_1_ID, DELIVERY_SERVICE_ID_PITER
        )

        PITER_1_ID = next(OUTLET_ID_GENERATOR)
        PITER_1 = create_pickup_outlet(PITER_1_ID, PITER_RID)

        PITER_2_ID = next(OUTLET_ID_GENERATOR)
        PITER_2 = create_pickup_outlet(PITER_2_ID, PITER_RID)

        DSBS_1_MOSCOW_1_ID = next(OUTLET_ID_GENERATOR)
        SHOP_DSBS_1_MOSCOW_1 = create_pickup_outlet(DSBS_1_MOSCOW_1_ID, MOSCOW_RID, _Shops._White.DSBS_1_ID)

        SHOP_CLICK_N_COLLECT_1_MOSCOW_1_ID = next(OUTLET_ID_GENERATOR)
        SHOP_CLICK_N_COLLECT_1_MOSCOW_1 = create_pickup_outlet(
            SHOP_CLICK_N_COLLECT_1_MOSCOW_1_ID, MOSCOW_RID, _Shops._Blue.CLICK_N_COLLECT_1_ID
        )

        SHOP_CLICK_N_COLLECT_2_MOSCOW_1_ID = next(OUTLET_ID_GENERATOR)
        SHOP_CLICK_N_COLLECT_2_MOSCOW_1 = create_pickup_outlet(
            SHOP_CLICK_N_COLLECT_2_MOSCOW_1_ID, MOSCOW_RID, _Shops._Blue.CLICK_N_COLLECT_2_ID
        )

        SHOP_CLICK_N_COLLECT_3_MOSCOW_1_ID = next(OUTLET_ID_GENERATOR)
        SHOP_CLICK_N_COLLECT_3_MOSCOW_1 = create_pickup_outlet(
            SHOP_CLICK_N_COLLECT_3_MOSCOW_1_ID, MOSCOW_RID, _Shops._Blue.CLICK_N_COLLECT_3_ID
        )

        DSBS_2_MOSCOW_1_ID = next(OUTLET_ID_GENERATOR)
        DSBS_2_MOSCOW_1 = create_pickup_outlet(DSBS_2_MOSCOW_1_ID, MOSCOW_RID, _Shops._White.DSBS_2_ID)

    class _Post:
        MOSCOW_1_ID = next(OUTLET_ID_GENERATOR)
        MOSCOW_1 = create_post_outlet(MOSCOW_1_ID, MOSCOW_RID)

        PITER_1_ID = next(OUTLET_ID_GENERATOR)
        PITER_1 = create_post_outlet(PITER_1_ID, PITER_RID, _Shops._Blue.B_PITER_1_ID)


# ========== Виртуальный магазин ==========

VIRTUAL_SHOP_BLUE_ID = next(SHOP_ID_GENERATOR)
VIRTUAL_SHOP_BLUE = Shop(
    fesh=VIRTUAL_SHOP_BLUE_ID,
    datafeed_id=VIRTUAL_SHOP_BLUE_ID,
    priority_region=MOSCOW_RID,
    name='virtual_shop',
    tax_system=Tax.OSN,
    fulfillment_virtual=True,
    cpa=Shop.CPA_REAL,
    virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
    delivery_service_outlets=[_Outlets._Post.MOSCOW_1_ID],
)

# ========== Бакеты ==========


def create_pickup_option(outlet_id):
    return PickupOption(outlet_id=outlet_id, day_from=1, day_to=20)


def create_courier_options(rid):
    return RegionalDelivery(
        rid=rid,
        options=[DeliveryOption(day_from=1, day_to=20, shop_delivery_price=5)],
        payment_methods=[Payment.PT_YANDEX, Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
    )


class _Buckets:
    class _Delivery:
        # Обычные посылки доставляют и в Питер, и в Москву
        BLUE_1_ID = next(BUCKET_ID_GENERATOR)
        BLUE_1 = DeliveryBucket(
            bucket_id=BLUE_1_ID,
            dc_bucket_id=BLUE_1_ID,
            carriers=[DELIVERY_SERVICE_ID_MOSCOW],
            regional_options=[
                create_courier_options(PITER_RID),
                create_courier_options(MOSCOW_RID),
            ],
            delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
        )

        # Тяжелая посылка доставляется только в некоторые районы города
        BLUE_HEAVY_1_ID = next(BUCKET_ID_GENERATOR)
        BLUE_HEAVY_1 = DeliveryBucket(
            bucket_id=BLUE_HEAVY_1_ID,
            dc_bucket_id=BLUE_HEAVY_1_ID,
            carriers=[DELIVERY_SERVICE_ID_MOSCOW],
            regional_options=[
                create_courier_options(VASYA_OSTROV_RID),
                create_courier_options(HAMOVNIKY_RID),
            ],
            delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
        )

        BLUE_2_ID = next(BUCKET_ID_GENERATOR)
        BLUE_2 = DeliveryBucket(
            bucket_id=BLUE_2_ID,
            dc_bucket_id=BLUE_2_ID,
            carriers=[DELIVERY_SERVICE_ID_MOSCOW],
            regional_options=[
                create_courier_options(MOSCOW_RID),
            ],
            delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
        )

        BLUE_PITER_1_ID = next(BUCKET_ID_GENERATOR)
        BLUE_PITER_1 = DeliveryBucket(
            bucket_id=BLUE_PITER_1_ID,
            dc_bucket_id=BLUE_PITER_1_ID,
            carriers=[DELIVERY_SERVICE_ID_PITER],
            regional_options=[
                create_courier_options(PITER_RID),
            ],
            delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
        )

    class _Pickup:
        BLUE_1_ID = next(BUCKET_ID_GENERATOR)
        BLUE_1 = PickupBucket(
            bucket_id=BLUE_1_ID,
            dc_bucket_id=BLUE_1_ID,
            carriers=[DELIVERY_SERVICE_ID_MOSCOW],
            options=[
                create_pickup_option(_Outlets._Pickup.MOSCOW_1_ID),
                create_pickup_option(_Outlets._Pickup.MOSCOW_2_ID),
                create_pickup_option(_Outlets._Pickup.PITER_1_ID),
                # Специально не добавил PITER_2
            ],
            delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
        )

        BLUE_HEAVY_1_ID = next(BUCKET_ID_GENERATOR)
        BLUE_HEAVY_1 = PickupBucket(
            bucket_id=BLUE_HEAVY_1_ID,
            dc_bucket_id=BLUE_1_ID,
            carriers=[DELIVERY_SERVICE_ID_MOSCOW],
            options=[
                create_pickup_option(_Outlets._Pickup.MOSCOW_1_ID),
                create_pickup_option(_Outlets._Pickup.PITER_2_ID),
            ],
            delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
        )

        BLUE_2_ID = next(BUCKET_ID_GENERATOR)
        BLUE_2 = PickupBucket(
            bucket_id=BLUE_2_ID,
            dc_bucket_id=BLUE_2_ID,
            carriers=[DELIVERY_SERVICE_ID_MOSCOW],
            fesh=_Shops._Blue.B_1_ID,
            options=[
                create_pickup_option(_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID),
                create_pickup_option(_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_2_ID),
            ],
            delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
        )

        BLUE_3_ID = next(BUCKET_ID_GENERATOR)
        BLUE_3 = PickupBucket(
            bucket_id=BLUE_3_ID,
            dc_bucket_id=BLUE_3_ID,
            fesh=_Shops._Blue.B_1_ID,
            carriers=[DELIVERY_SERVICE_ID_MOSCOW],
            options=[create_pickup_option(_Outlets._Pickup.BLUE_SHOP_2_MOSCOW_1_ID)],
            delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
        )

        DSBS_1_ID = next(BUCKET_ID_GENERATOR)
        DSBS_1 = PickupBucket(
            bucket_id=DSBS_1_ID,
            dc_bucket_id=DSBS_1_ID,
            fesh=_Shops._White.DSBS_1_ID,
            carriers=[DELIVERY_SERVICE_ID_MOSCOW],
            options=[create_pickup_option(_Outlets._Pickup.DSBS_1_MOSCOW_1_ID)],
        )

        CLICK_N_COLLECT_1_ID = next(BUCKET_ID_GENERATOR)
        CLICK_N_COLLECT_1 = PickupBucket(
            bucket_id=CLICK_N_COLLECT_1_ID,
            dc_bucket_id=CLICK_N_COLLECT_1_ID,
            fesh=_Shops._Blue.CLICK_N_COLLECT_1_ID,
            carriers=[DELIVERY_SERVICE_ID_MOSCOW],
            options=[create_pickup_option(_Outlets._Pickup.SHOP_CLICK_N_COLLECT_1_MOSCOW_1_ID)],
        )

        CLICK_N_COLLECT_2_ID = next(BUCKET_ID_GENERATOR)
        CLICK_N_COLLECT_2 = PickupBucket(
            bucket_id=CLICK_N_COLLECT_2_ID,
            dc_bucket_id=CLICK_N_COLLECT_2_ID,
            fesh=_Shops._Blue.CLICK_N_COLLECT_2_ID,
            carriers=[DELIVERY_SERVICE_ID_MOSCOW],
            options=[create_pickup_option(_Outlets._Pickup.SHOP_CLICK_N_COLLECT_2_MOSCOW_1_ID)],
        )

        CLICK_N_COLLECT_3_ID = next(BUCKET_ID_GENERATOR)
        CLICK_N_COLLECT_3 = PickupBucket(
            bucket_id=CLICK_N_COLLECT_3_ID,
            dc_bucket_id=CLICK_N_COLLECT_3_ID,
            fesh=_Shops._Blue.CLICK_N_COLLECT_3_ID,
            carriers=[DELIVERY_SERVICE_ID_MOSCOW],
            options=[create_pickup_option(_Outlets._Pickup.SHOP_CLICK_N_COLLECT_3_MOSCOW_1_ID)],
        )

        DSBS_2_ID = next(BUCKET_ID_GENERATOR)
        DSBS_2 = PickupBucket(
            bucket_id=DSBS_2_ID,
            dc_bucket_id=DSBS_2_ID,
            fesh=_Shops._White.DSBS_2_ID,
            carriers=[DELIVERY_SERVICE_ID_MOSCOW],
            options=[create_pickup_option(_Outlets._Pickup.DSBS_2_MOSCOW_1_ID)],
        )

    class _Post:
        BLUE_1_ID = next(BUCKET_ID_GENERATOR)
        BLUE_1 = PickupBucket(
            bucket_id=BLUE_1_ID,
            dc_bucket_id=BLUE_1_ID,
            fesh=_Shops._Blue.B_1_ID,
            carriers=[DELIVERY_SERVICE_ID_MOSCOW],
            options=[create_pickup_option(_Outlets._Pickup.MOSCOW_1_ID)],
            delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
        )


# ========== Офферы ==========


class _Offers:
    class _Blue:
        B_1 = BlueOffer(
            price=5,
            vat=Vat.VAT_10,
            waremd5='Sku1Price5-IiLVm1Goleg',
            weight=5,
            dimensions=OfferDimensions(length=20, width=30, height=10),
            delivery_buckets=[_Buckets._Delivery.BLUE_1_ID],
            pickup_buckets=[_Buckets._Pickup.BLUE_1_ID],
            post_buckets=[_Buckets._Post.BLUE_1_ID],
            post_term_delivery=True,
        )

        B_2 = BlueOffer(
            price=1,
            vat=Vat.VAT_10,
            waremd5='Sku2Price1-IiLVm1Goleg',
            weight=5,
            offerid='blue.offer.2.1',
            fesh=_Shops._Blue.B_1_ID,
            feedid=_Shops._Blue.B_1_ID,
            dimensions=OfferDimensions(length=100, width=100, height=100),
            delivery_buckets=[_Buckets._Delivery.BLUE_2_ID],
            pickup_buckets=[_Buckets._Pickup.BLUE_2_ID],
            post_buckets=[_Buckets._Post.BLUE_1_ID],
            post_term_delivery=True,
        )

        B_2_3 = BlueOffer(
            price=1,
            vat=Vat.VAT_10,
            waremd5='Sku2Price3-IiLVm1Goleg',
            weight=5,
            offerid='blue.offer.2.2',
            fesh=_Shops._Blue.B_1_ID,
            feedid=_Shops._Blue.B_1_ID,
            dimensions=OfferDimensions(length=100, width=100, height=100),
            delivery_buckets=[_Buckets._Delivery.BLUE_2_ID],
            pickup_buckets=[_Buckets._Pickup.BLUE_2_ID],
            post_buckets=[_Buckets._Post.BLUE_1_ID],
            post_term_delivery=True,
        )

        B_3 = BlueOffer(
            price=2,
            vat=Vat.VAT_10,
            waremd5='Sku2Price2-IiLVm1Goleg',
            weight=5,
            offerid='blue.offer.3.1',
            fesh=_Shops._Blue.B_2_ID,
            feedid=_Shops._Blue.B_2_ID,
            dimensions=OfferDimensions(length=100, width=100, height=100),
            delivery_buckets=[_Buckets._Delivery.BLUE_2_ID],
            pickup_buckets=[_Buckets._Pickup.BLUE_3_ID],
            post_buckets=[_Buckets._Post.BLUE_1_ID],
            post_term_delivery=True,
        )

        B_4_PITER = BlueOffer(
            price=2,
            vat=Vat.VAT_10,
            waremd5='Sku3Price2-IiLVm1Goleg',
            weight=5,
            offerid='blue.offer.4.1',
            fesh=_Shops._Blue.B_PITER_1_ID,
            feedid=_Shops._Blue.B_PITER_1_ID,
            dimensions=OfferDimensions(length=100, width=100, height=100),
            delivery_buckets=[_Buckets._Delivery.BLUE_PITER_1_ID],
            pickup_buckets=[_Buckets._Pickup.BLUE_3_ID],
            post_buckets=[_Buckets._Post.BLUE_1_ID],
            post_term_delivery=True,
        )

        CLICK_N_COLLECT_1 = BlueOffer(
            offerid="click_n_collect_shop_sku_1",
            fesh=_Shops._Blue.CLICK_N_COLLECT_1_ID,
            feedid=_Shops._Blue.CLICK_N_COLLECT_1_ID,
            waremd5='22222222222223ggggMODg',
            weight=5,
            dimensions=OfferDimensions(length=100, width=100, height=100),
            pickup_buckets=[_Buckets._Pickup.CLICK_N_COLLECT_1_ID],
            post_term_delivery=True,
        )

        CLICK_N_COLLECT_2 = BlueOffer(
            offerid="click_n_collect_shop_sku_1",
            fesh=_Shops._Blue.CLICK_N_COLLECT_2_ID,
            feedid=_Shops._Blue.CLICK_N_COLLECT_2_ID,
            waremd5='22222222222221ggggMODg',
            weight=5,
            dimensions=OfferDimensions(length=100, width=100, height=100),
            pickup_buckets=[_Buckets._Pickup.CLICK_N_COLLECT_2_ID],
            post_term_delivery=True,
        )

        CLICK_N_COLLECT_3 = BlueOffer(
            offerid="click_n_collect_shop_sku_3",
            fesh=_Shops._Blue.CLICK_N_COLLECT_3_ID,
            feedid=_Shops._Blue.CLICK_N_COLLECT_3_ID,
            waremd5='22222222222220ggggMODg',
            weight=5,
            dimensions=OfferDimensions(length=100, width=100, height=100),
            pickup_buckets=[_Buckets._Pickup.CLICK_N_COLLECT_3_ID],
            post_term_delivery=True,
        )

    class _White:
        DSBS_1 = Offer(
            price=2,
            fesh=_Shops._White.DSBS_1_ID,
            feedid=_Shops._White.DSBS_1_ID,
            waremd5='22222222222222ggggMODg',
            weight=5,
            dimensions=OfferDimensions(length=20, width=30, height=10),
            cargo_types=[256, 10],
            cpa=Offer.CPA_REAL,
        )

        DSBS_2 = Offer(
            offerid="click_n_collect_shop_sku_1",
            fesh=_Shops._White.DSBS_2_ID,
            feedid=_Shops._White.DSBS_2_ID,
            waremd5='22222222222224ggggMODg',
            weight=5,
            dimensions=OfferDimensions(length=100, width=100, height=100),
            pickup_buckets=[_Buckets._Pickup.DSBS_2_ID],
            cargo_types=[256, 10],
            post_term_delivery=True,
            cpa=Offer.CPA_REAL,
        )


# ========== Представления офферов для mock-запросов в комбинатор ==========


class _CombinatorOffers:
    class _Blue:
        B_2 = CombinatorOffer(
            shop_sku='blue.offer.2.1',
            shop_id=_Shops._Blue.B_1_ID,
            partner_id=145,
            available_count=1,
        )

        B_2_3 = CombinatorOffer(
            shop_sku='blue.offer.2.2',
            shop_id=_Shops._Blue.B_1_ID,
            partner_id=145,
            available_count=3,
        )

        B_3 = CombinatorOffer(
            shop_sku='blue.offer.3.1',
            shop_id=_Shops._Blue.B_2_ID,
            partner_id=145,
            available_count=1,
        )

        B_4_PITER = CombinatorOffer(
            shop_sku='blue.offer.4.1',
            shop_id=_Shops._Blue.B_PITER_1_ID,
            partner_id=145,
            available_count=1,
        )


B_2_delivery_item = DeliveryItem(
    required_count=1,
    weight=5000,
    dimensions=[100, 100, 100],
    cargo_types=[],
    offers=[_CombinatorOffers._Blue.B_2],
    price=_Offers._Blue.B_2.price,
)
B_2_delivery_item_3 = DeliveryItem(
    required_count=3,
    weight=5000,
    dimensions=[100, 100, 100],
    cargo_types=[],
    offers=[_CombinatorOffers._Blue.B_2_3],
    price=_Offers._Blue.B_2_3.price,
)
B_3_delivery_item = DeliveryItem(
    required_count=1,
    weight=5000,
    dimensions=[100, 100, 100],
    cargo_types=[],
    offers=[_CombinatorOffers._Blue.B_3],
    price=_Offers._Blue.B_3.price,
)
B_4_PITER_delivery_item = DeliveryItem(
    required_count=1,
    weight=5000,
    dimensions=[100, 100, 100],
    cargo_types=[],
    offers=[_CombinatorOffers._Blue.B_4_PITER],
    price=_Offers._Blue.B_4_PITER.price,
)
DELIVERY_SERVICE_ID_MOSCOW_delivery_option = create_delivery_option(
    cost=1,
    date_from=DATE_FROM,
    date_to=DATE_FROM,
    time_from=datetime.time(10, 0),
    time_to=datetime.time(22, 0),
    delivery_service_id=DELIVERY_SERVICE_ID_MOSCOW,
    payment_methods=[
        PaymentMethod.PT_YANDEX,
        PaymentMethod.PT_CARD_ON_DELIVERY,
    ],
    trying_available=True,
)
DELIVERY_SERVICE_ID_PITER_delivery_option = create_delivery_option(
    cost=2,
    date_from=DATE_FROM,
    date_to=DATE_FROM,
    time_from=datetime.time(9, 0),
    time_to=datetime.time(23, 0),
    delivery_service_id=DELIVERY_SERVICE_ID_PITER,
    payment_methods=[
        PaymentMethod.PT_YANDEX,
        PaymentMethod.PT_CARD_ON_DELIVERY,
        PaymentMethod.PT_CASH_ON_DELIVERY,
    ],
)

VIRTUAL_BOX = create_virtual_box(weight=5000, length=100, width=100, height=100)


def create_post_point_grouped(post_ids):
    return PickupPointGrouped(
        ids_list=post_ids,
        post_ids=post_ids,
        outlet_type=OutletType.FOR_POST,
        service_id=DELIVERY_SERVICE_ID_MOSCOW,
        cost=1,
        date_from=DATE_FROM,
        date_to=DATE_FROM,
        payment_methods=[
            PaymentMethod.PT_YANDEX,
            PaymentMethod.PT_CASH_ON_DELIVERY,
        ],
    )


def create_pickup_point_grouped(point_ids):
    return PickupPointGrouped(
        ids_list=point_ids,
        post_ids=point_ids,
        outlet_type=OutletType.FOR_PICKUP,
        service_id=DELIVERY_SERVICE_ID_MOSCOW,
        cost=1,
        date_from=DATE_FROM,
        date_to=DATE_FROM,
        payment_methods=[
            PaymentMethod.PT_YANDEX,
            PaymentMethod.PT_CASH_ON_DELIVERY,
        ],
        trying_available=True,
    )


# ========== Пресеты ==========


class _Presets:
    @staticmethod
    def preset_to_request(preset):
        def preset_value_to_string(key, value):
            if key == 'coord':
                return 'lat:{};lon:{}'.format(value['lat'], value['lon'])
            return '{}:{}'.format(key, value)

        fields = [preset_value_to_string(k, v) for (k, v) in preset.items()]
        return '&address=' + ';'.join(fields)

    @staticmethod
    def presets_to_request(presets):
        return ''.join(_Presets.preset_to_request(preset) for preset in presets)

    @staticmethod
    def preset_to_response(preset, parcel_info):
        """
        Привести формат пресета из формата запроса в формат ответа
        post_code (int) -> postCode (str)
        outlet (int) -> outletId (str)
        """
        result = {}
        for k, v in preset.items():
            if k == 'post_code':
                result.update({'postCode': v})
            elif k == 'outlet':
                result.update({'outletId': v})
            else:
                result.update({k: v})
        result.update(parcel_info)
        return result

    @staticmethod
    def manual_presets_to_response(presets_with_parcel_info):
        """
        presets_with_parcel_info_or_presets: List[Tuple[Dict, dict]]
        """
        return [_Presets.preset_to_response(preset, parcel_info) for preset, parcel_info in presets_with_parcel_info]

    @staticmethod
    def presets_to_response(presets_with_availability):
        """
        presets_with_parcel_info_or_presets: Union[List[Dict], List[Tuple[Dict, EDeliveryAvailability]]]
        """
        # Значение по умолчанию информации о посылках, вкладываемая в ответе по пересетам
        DEFAULT_PARCEL_INFO = {
            "parcels": [
                {
                    "parcelIndex": 0,
                    "deliveryAvailable": "AVAILABLE",
                }
            ],
        }

        def prepare_value(value):
            if isinstance(value, dict):
                return (value, DEFAULT_PARCEL_INFO)
            elif isinstance(value, tuple):
                return (
                    value[0],
                    {
                        "parcels": [{"parcelIndex": 0, "deliveryAvailable": value[1]}],
                    },
                )

        return _Presets.manual_presets_to_response([prepare_value(item) for item in presets_with_availability])

    @staticmethod
    def available(preset):
        return (preset, EDeliveryAvailability.Available)

    @staticmethod
    def unavailable(preset):
        return (preset, EDeliveryAvailability.Unavailable)

    @staticmethod
    def outlet_missed(preset):
        return (preset, EDeliveryAvailability.OutletMissed)

    class _Bad:
        WRONG_TYPE = {
            "type": "wrong",
            "id": "123",
            "rid": 17,
            "coord": {
                "lat": 1.1,
                "lon": 2.7,
            },
        }
        WRONG_ID = {
            "type": "courier",
            "id_missed": 123,
            "rid": 17,
            "coord": {
                "lat": 1.1,
                "lon": 2.7,
            },
        }
        PRESET_MOSCOW_TYPE_LOWER_CASE = {
            "type": TAddressPreset.EType.Pickup.lower(),
            "id": "124-moscow",
            "outlet": _Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID,
        }
        MISSED = {
            "type": TAddressPreset.EType.Pickup,
            "id": "1-missed",
            "outlet": next(OUTLET_ID_GENERATOR),
        }

    class _Courier:
        PRESET_VASYA = {
            "type": TAddressPreset.EType.Courier,
            "id": "123-vasya",
            "rid": VASYA_OSTROV_RID,
            "coord": {"lat": 17.5, "lon": 65.82},
        }
        PRESET_PALACE = {"type": TAddressPreset.EType.Courier, "id": "123-palace", "rid": PALACE_RID}
        PRESET_PITER = {"type": TAddressPreset.EType.Courier, "id": "121-piter", "rid": PITER_RID}
        PRESET_MOSCOW = {"type": TAddressPreset.EType.Courier, "id": "123-moscow", "rid": MOSCOW_RID}

    class _Pickup:
        PRESET_MOSCOW = {
            "type": TAddressPreset.EType.Pickup,
            "id": "124-moscow",
            "outlet": _Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID,
        }
        PRESET_MOSCOW_1_2 = {
            "type": TAddressPreset.EType.Pickup,
            "id": "124-moscow",
            "outlet": _Outlets._Pickup.BLUE_SHOP_1_MOSCOW_2_ID,
        }
        PRESET_MOSCOW_2 = {
            "type": TAddressPreset.EType.Pickup,
            "id": "125-moscow",
            "outlet": _Outlets._Pickup.BLUE_SHOP_2_MOSCOW_1_ID,
        }
        PRESET_PITER_1_1 = {
            "type": TAddressPreset.EType.Pickup,
            "id": "111-piter",
            "outlet": _Outlets._Pickup.BLUE_SHOP_1_PITER_1_ID,
        }
        PRESET_MOSCOW_3 = {
            "type": TAddressPreset.EType.Pickup,
            "id": "125-moscow",
            "outlet": _Outlets._Pickup.BLUE_SHOP_2_MOSCOW_3_ID,
        }
        DSBS_OFFER_1_MOSCOW_1 = {
            "type": TAddressPreset.EType.Pickup,
            "id": "127-moscow",
            "outlet": _Outlets._Pickup.DSBS_1_MOSCOW_1_ID,
        }
        CLICK_N_COLLECT_OFFER_1_MOSCOW_1 = {
            "type": TAddressPreset.EType.Pickup,
            "id": "128-moscow",
            "outlet": _Outlets._Pickup.SHOP_CLICK_N_COLLECT_1_MOSCOW_1_ID,
        }
        CLICK_N_COLLECT_OFFER_2_MOSCOW_1 = {
            "type": TAddressPreset.EType.Pickup,
            "id": "129-moscow",
            "outlet": _Outlets._Pickup.SHOP_CLICK_N_COLLECT_2_MOSCOW_1_ID,
        }
        DSBS_DELIVERY_SERVICE_OFFER_1_MOSCOW_1 = {
            "type": TAddressPreset.EType.Pickup,
            "id": "130-moscow",
            "outlet": _Outlets._Pickup.DSBS_2_MOSCOW_1_ID,
        }
        CLICK_N_COLLECT_OFFER_3_MOSCOW_1 = {
            "type": TAddressPreset.EType.Pickup,
            "id": "131-moscow",
            "outlet": _Outlets._Pickup.SHOP_CLICK_N_COLLECT_3_MOSCOW_1_ID,
        }

    class _Post:
        PRESET_MOSCOW = {"type": TAddressPreset.EType.Post, "id": "126-moscow", "post_code": _Outlets._Post.MOSCOW_1_ID}
        PRESET_MOSCOW_AS_PICKUP = {
            "type": TAddressPreset.EType.Pickup,
            "id": "126-moscow-post-as-pickup",
            "outlet": _Outlets._Post.MOSCOW_1_ID,
        }

        # Специальный пресет, у которого почтовый код совпадает с почтовым кодом ПВЗ
        PRESET_MOSCOW_2 = {
            "type": TAddressPreset.EType.Post,
            "id": "128-moscow",
            "post_code": _Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID,
        }

        PRESET_PITER = {"type": TAddressPreset.EType.Post, "id": "127-piter", "post_code": _Outlets._Post.PITER_1_ID}


# ============================


def make_base_request(offers):
    return "place=actual_delivery" + "&offers-list={}".format(
        ",".join("{}:1".format(offer.waremd5) for offer in offers)
    )


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1",
                hyperid=1,
                sku=1,
                blue_offers=[_Offers._Blue.B_1],
                post_term_delivery=True,
            ),
            MarketSku(
                title="blue offer sku2",
                weight=5,
                dimensions=OfferDimensions(length=20, width=30, height=10),
                hyperid=2,
                sku=2,
                blue_offers=[_Offers._Blue.B_2, _Offers._Blue.B_2_3],
                post_term_delivery=True,
            ),
            MarketSku(
                title="blue offer sku3",
                weight=5,
                dimensions=OfferDimensions(length=20, width=40, height=10),
                hyperid=3,
                sku=3,
                blue_offers=[_Offers._Blue.B_3],
                post_term_delivery=True,
            ),
            MarketSku(
                title="blue offer sku4",
                weight=5,
                hyperid=3,
                sku=3,
                blue_offers=[_Offers._Blue.B_4_PITER],
                post_term_delivery=True,
            ),
            MarketSku(
                hid=1,
                title="click and collect sku4",
                hyperid=4,
                sku=str(4),
                blue_offers=[_Offers._Blue.CLICK_N_COLLECT_1],
                post_term_delivery=True,
            ),
            MarketSku(
                hid=1,
                title="click and collect sku5",
                hyperid=5,
                sku=str(5),
                blue_offers=[_Offers._Blue.CLICK_N_COLLECT_2],
                post_term_delivery=True,
            ),
            MarketSku(
                hid=1,
                title="click and collect sku6",
                hyperid=6,
                sku=str(6),
                blue_offers=[_Offers._Blue.CLICK_N_COLLECT_3],
                post_term_delivery=True,
            ),
        ]

        cls.index.offers += [
            _Offers._White.DSBS_1,
            _Offers._White.DSBS_2,
        ]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=MOSCOW_RID),
            DynamicWarehouseInfo(id=1, home_region=MOSCOW_RID),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
            DynamicWarehouseToWarehouseInfo(warehouse_from=1, warehouse_to=1),
        ]

    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [
            Region(
                rid=PITER_RID,
                region_type=Region.CITY,
                children=[
                    Region(rid=VASYA_OSTROV_RID, region_type=Region.CITY_DISTRICT),
                    Region(rid=PALACE_RID, region_type=Region.CITY_DISTRICT),
                ],
            ),
            Region(
                rid=MOSCOW_RID,
                region_type=Region.CITY,
                children=[
                    Region(rid=HAMOVNIKY_RID, region_type=Region.CITY_DISTRICT),
                    Region(rid=CENTRAL_RID, region_type=Region.CITY_DISTRICT),
                ],
            ),
        ]

    @classmethod
    def prepare_buckets(cls):
        cls.index.delivery_buckets += [
            _Buckets._Delivery.BLUE_1,
            _Buckets._Delivery.BLUE_HEAVY_1,
            _Buckets._Delivery.BLUE_2,
            _Buckets._Delivery.BLUE_PITER_1,
        ]

        cls.index.pickup_buckets += [
            _Buckets._Pickup.BLUE_1,
            _Buckets._Pickup.BLUE_HEAVY_1,
            _Buckets._Pickup.BLUE_2,
            _Buckets._Pickup.BLUE_3,
            _Buckets._Pickup.DSBS_1,
            _Buckets._Pickup.CLICK_N_COLLECT_1,
            _Buckets._Pickup.CLICK_N_COLLECT_2,
            _Buckets._Pickup.CLICK_N_COLLECT_3,
            _Buckets._Pickup.DSBS_2,
        ]

        cls.index.pickup_buckets += [
            _Buckets._Post.BLUE_1,
        ]

    @classmethod
    def prepare_outlets(cls):
        cls.index.outlets += [
            _Outlets._Pickup.MOSCOW_1,
            _Outlets._Pickup.MOSCOW_2,
            _Outlets._Pickup.SHOP_B_1_MOSCOW_1,
            _Outlets._Pickup.SHOP_B_1_MOSCOW_2,
            _Outlets._Pickup.SHOP_B_2_MOSCOW_1,
            _Outlets._Pickup.SHOP_B_2_MOSCOW_2,
            _Outlets._Pickup.SHOP_B_2_MOSCOW_3,
            _Outlets._Pickup.BLUE_SHOP_1_PITER_1,
            _Outlets._Pickup.PITER_1,
            _Outlets._Pickup.PITER_2,
            _Outlets._Pickup.SHOP_DSBS_1_MOSCOW_1,
            _Outlets._Pickup.SHOP_CLICK_N_COLLECT_1_MOSCOW_1,
            _Outlets._Pickup.SHOP_CLICK_N_COLLECT_2_MOSCOW_1,
            _Outlets._Pickup.SHOP_CLICK_N_COLLECT_3_MOSCOW_1,
            _Outlets._Pickup.DSBS_2_MOSCOW_1,
            _Outlets._Post.MOSCOW_1,
        ]

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [
            _Shops._Blue.B_1,
            _Shops._Blue.B_2,
            _Shops._Blue.B_PITER_1,
            _Shops._Blue.CLICK_N_COLLECT_1,
            _Shops._Blue.CLICK_N_COLLECT_2,
            _Shops._Blue.CLICK_N_COLLECT_3,
            _Shops._White.DSBS_1,
            _Shops._White.DSBS_2,
            VIRTUAL_SHOP_BLUE,
        ]

    @classmethod
    def prepare_combinator(cls):
        cls.settings.check_combinator_errors = False
        cls.settings.default_search_experiment_flags += ['enable_dsbs_combinator_request_in_actual_delivery=0']

    def test_presets_error_wrong_type(self):
        '''
        Проверяем генерацию ошибок, если входной формат пресетов невозможно распарсить
        Неизвестный тип пресета
        '''
        request = (
            make_base_request([_Offers._Blue.B_1])
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.preset_to_request(_Presets._Bad.WRONG_TYPE)
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "addressPresets": EmptyList(),
                },
            },
        )

        self.error_log.expect(code=ErrorCodes.CGI_ADDRESS_PRESET_ERROR).times(1)

    def test_presets_error_id_missed(self):
        '''
        Проверяем генерацию ошибок, если входной формат пресетов невозможно распарсить
        Нет идентификатора пресета
        '''
        request = (
            make_base_request([_Offers._Blue.B_1])
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.preset_to_request(_Presets._Bad.WRONG_ID)
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "addressPresets": EmptyList(),
                },
            },
        )

        self.error_log.expect(code=ErrorCodes.CGI_ADDRESS_PRESET_ERROR).times(1)

    def test_presets_input(self):
        '''
        Проверяем, что actual_delivery может принимать список любимых адресов и выводить их
        '''
        for rid in [MOSCOW_RID, PITER_RID, HAMOVNIKY_RID]:

            request = make_base_request([_Offers._Blue.B_1]) + "&rids={}".format(MOSCOW_RID)
            response = self.report.request_json(request)

            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "addressPresets": EmptyList(),
                    },
                },
            )

            request = (
                make_base_request([_Offers._Blue.B_1])
                + "&rids={}".format(MOSCOW_RID)
                + _Presets.preset_to_request(_Presets._Courier.PRESET_MOSCOW)
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "addressPresets": [
                            _Presets._Courier.PRESET_MOSCOW,
                        ],
                    },
                },
                allow_different_len=False,
            )

            request = (
                make_base_request([_Offers._Blue.B_1])
                + "&rids={}".format(MOSCOW_RID)
                + _Presets.preset_to_request(_Presets._Courier.PRESET_MOSCOW)
                + "&rearr-factors=market_actual_delivery_address_presets={}".format(
                    TSearchExperimentFlags.EActualDeliveryPresetOptions.ADAPO_STOP
                )
            )
            # пресеты не выводятся, если заблокированы флагом экспа
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "addressPresets": EmptyList(),
                    },
                },
                allow_different_len=False,
            )

    @classmethod
    def prepare_combinator_response_group_type(cls):
        cls.combinator.on_pickup_points_grouped_request(
            items=[B_2_delivery_item],
            destination_regions=[MOSCOW_RID],
            point_types=[],
            total_price=_Offers._Blue.B_2.price,
            post_codes=[],
        ).respond_with_grouped_pickup_points(
            groups=[
                create_pickup_point_grouped([_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID]),
            ],
            virtual_box=VIRTUAL_BOX,
        )

    def test_combinator_response_group_type(self):
        """
        Проверка разницы обработки почтовых кодов от ПВЗ и от почтовых групп
        В рамках запроса данных по основной логике отдается ответ с ПВЗ, имеющей почтовый код
        При этом почтовый пресет с тем же кодом должен остаться недоступным
        Например, это ответ комбинатора группы почты для пресетов (тип POST_OFFICE):
        groups { points { logistic_point_id: 102 region_id: 213 post_code: 102 } type: POST_OFFICE }
        А это уже просто ПВЗ:
        groups { points { logistic_point_id: 102 region_id: 213 post_code: 102 } }
        Проверим, что код почты из "просто ПВЗ" не будет воздействовать на почтовые пресеты
        """
        input_presets = [
            _Presets._Post.PRESET_MOSCOW_2,
        ]
        expeceted_output_presets = [
            _Presets.unavailable(_Presets._Post.PRESET_MOSCOW_2),
        ]
        request = (
            make_base_request([_Offers._Blue.B_2])
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.presets_to_request(input_presets)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_preset_type_non_case_sensivity(cls):
        cls.combinator.on_pickup_points_cart_request(
            items=[B_2_delivery_item],
            point_types=[],
            total_price=_Offers._Blue.B_2.price,
            post_codes=[],
            logistic_point_ids=[_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID],
        ).respond_with_pickup_points_cart(
            groups=[
                create_pickup_point_grouped([_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID]),
            ],
            virtual_box=VIRTUAL_BOX,
        )

    def test_preset_type_non_case_sensivity(self):
        """
        Проверка нечувствительности поля type в пресетах к регистру
        """
        input_presets = [
            _Presets._Bad.PRESET_MOSCOW_TYPE_LOWER_CASE,
        ]
        output_presets = [
            _Presets.available(_Presets._Pickup.PRESET_MOSCOW),
        ]
        request = (
            make_base_request([_Offers._Blue.B_2])
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.presets_to_request(input_presets)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(output_presets)},
            },
            allow_different_len=False,
        )

    def test_preset_outlet_missed(self):
        input_presets = [_Presets._Bad.MISSED]
        expeceted_output_presets = [_Presets.outlet_missed(_Presets._Bad.MISSED)]
        request = (
            make_base_request([_Offers._Blue.B_2])
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.presets_to_request(input_presets)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_blue_offer_presets_filter(cls):
        cls.combinator.on_courier_options_request(
            items=[B_2_delivery_item],
            destination=Destination(region_id=MOSCOW_RID),
            payment_methods=[],
            total_price=_Offers._Blue.B_2.price,
        ).respond_with_courier_options(
            options=[DELIVERY_SERVICE_ID_MOSCOW_delivery_option],
            virtual_box=VIRTUAL_BOX,
        )
        cls.combinator.on_pickup_points_cart_request(
            items=[B_2_delivery_item],
            point_types=[],
            total_price=_Offers._Blue.B_2.price,
            post_codes=[_Outlets._Post.MOSCOW_1_ID],
            logistic_point_ids=[_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID],
        ).respond_with_pickup_points_cart(
            groups=[
                create_pickup_point_grouped([_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID]),
                create_post_point_grouped([_Outlets._Post.MOSCOW_1_ID]),
            ],
            virtual_box=VIRTUAL_BOX,
        )
        cls.combinator.on_pickup_points_cart_request(
            items=[B_2_delivery_item],
            point_types=[],
            total_price=_Offers._Blue.B_2.price,
            post_codes=[_Outlets._Post.MOSCOW_1_ID],
            logistic_point_ids=[
                _Outlets._Pickup.DSBS_1_MOSCOW_1_ID,
                _Outlets._Pickup.BLUE_SHOP_2_MOSCOW_1_ID,
                _Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID,
            ],
        ).respond_with_pickup_points_cart(
            groups=[
                create_pickup_point_grouped(
                    [
                        _Outlets._Pickup.DSBS_1_MOSCOW_1_ID,
                        _Outlets._Pickup.BLUE_SHOP_2_MOSCOW_1_ID,
                        _Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID,
                    ]
                ),
                create_post_point_grouped([_Outlets._Post.MOSCOW_1_ID]),
            ],
            virtual_box=VIRTUAL_BOX,
        )

    def test_blue_offer_presets_filter(self):
        """
        Проверяем пресеты для одиночного синего оффера:
            - пресеты ПВЗ, курьерской доставки и почты проходят фильтры
            - пресеты ПВЗ других магазинов не проходят фильтры
        """
        # Пресеты в запросе и в ответе совпадают
        input_presets = [
            _Presets._Courier.PRESET_PITER,
            _Presets._Courier.PRESET_MOSCOW,
            _Presets._Pickup.PRESET_MOSCOW,
            _Presets._Post.PRESET_MOSCOW,
        ]
        expeceted_output_presets = [
            _Presets.unavailable(_Presets._Courier.PRESET_PITER),
            _Presets.available(_Presets._Courier.PRESET_MOSCOW),
            _Presets.available(_Presets._Pickup.PRESET_MOSCOW),
            _Presets.available(_Presets._Post.PRESET_MOSCOW),
        ]
        request = (
            make_base_request([_Offers._Blue.B_2])
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.presets_to_request(input_presets)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )

        input_presets = [
            _Presets._Courier.PRESET_MOSCOW,
            _Presets._Pickup.DSBS_OFFER_1_MOSCOW_1,  # <- не пройдет фильтр, т.к. не является аутлетом "нашего" магазина
            _Presets._Pickup.PRESET_MOSCOW_2,  # <- не пройдет фильтр, т.к. не является аутлетом "нашего" магазина
            _Presets._Pickup.PRESET_MOSCOW,
            _Presets._Post.PRESET_MOSCOW,
        ]
        expeceted_output_presets = [
            _Presets.available(_Presets._Courier.PRESET_MOSCOW),
            _Presets.unavailable(_Presets._Pickup.DSBS_OFFER_1_MOSCOW_1),
            _Presets.unavailable(_Presets._Pickup.PRESET_MOSCOW_2),
            _Presets.available(_Presets._Pickup.PRESET_MOSCOW),
            _Presets.available(_Presets._Post.PRESET_MOSCOW),
        ]
        request = (
            make_base_request([_Offers._Blue.B_2])
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.presets_to_request(input_presets)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_use_post_as_pickup_presets(cls):
        cls.combinator.on_pickup_points_cart_request(
            items=[B_2_delivery_item],
            point_types=[],
            total_price=_Offers._Blue.B_2.price,
            post_codes=[],
            logistic_point_ids=[_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID, _Outlets._Post.MOSCOW_1_ID],
        ).respond_with_pickup_points_cart(
            groups=[
                create_pickup_point_grouped(
                    [
                        _Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID,
                    ]
                ),
                create_post_point_grouped([_Outlets._Post.MOSCOW_1_ID]),
            ],
            virtual_box=VIRTUAL_BOX,
        )

    def test_use_post_as_pickup_presets(self):
        """
        Проверка работы почты как ПВЗ под флагом.
        При этом:
            - почтовый ПВЗ запрашивается в комбинаторе с pickup-пресетами (передается id оутлета, а не почтовый код)
            - почтовый ПВЗ возвращается в отдельной почтовой группы в ответе комбинатора
        """
        input_presets = [
            _Presets._Pickup.PRESET_MOSCOW,
            _Presets._Post.PRESET_MOSCOW_AS_PICKUP,
        ]
        # Почта как ПВЗ ВКЛ: пресет pickup-почты будет передан и обработан именно как пресет обычного ПВЗ
        expeceted_output_presets = [
            _Presets.available(_Presets._Pickup.PRESET_MOSCOW),
            _Presets.available(_Presets._Post.PRESET_MOSCOW_AS_PICKUP),
        ]
        request = (
            make_base_request([_Offers._Blue.B_2])
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.presets_to_request(input_presets)
            + "&rearr-factors=market_use_post_as_pickup=1"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )
        # Почта как ПВЗ ВЫКЛ: пресет pickup-почты будет недоступен, т.к. передается как pickup
        expeceted_output_presets = [
            _Presets.available(_Presets._Pickup.PRESET_MOSCOW),
            _Presets.unavailable(_Presets._Post.PRESET_MOSCOW_AS_PICKUP),
        ]
        request = (
            make_base_request([_Offers._Blue.B_2])
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.presets_to_request(input_presets)
            + "&rearr-factors=market_use_post_as_pickup=0"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_blue_offers_courier_combinator_presets_filter(cls):
        cls.combinator.on_courier_options_request(
            items=[B_4_PITER_delivery_item],
            destination=Destination(region_id=PITER_RID),
            payment_methods=[],
            total_price=_Offers._Blue.B_4_PITER.price,
        ).respond_with_courier_options(
            options=[DELIVERY_SERVICE_ID_MOSCOW_delivery_option],
            virtual_box=VIRTUAL_BOX,
        )
        cls.combinator.on_courier_options_request(
            items=[B_4_PITER_delivery_item],
            destination=Destination(region_id=MOSCOW_RID),
            payment_methods=[],
            total_price=_Offers._Blue.B_4_PITER.price,
        ).respond_with_courier_options(
            options=[DELIVERY_SERVICE_ID_MOSCOW_delivery_option],
            virtual_box=VIRTUAL_BOX,
        )
        cls.combinator.on_pickup_points_cart_request(
            items=[B_4_PITER_delivery_item],
            point_types=[],
            total_price=_Offers._Blue.B_4_PITER.price,
            post_codes=[_Outlets._Post.MOSCOW_1_ID],
            logistic_point_ids=[_Outlets._Pickup.BLUE_SHOP_1_PITER_1_ID],
        ).respond_with_pickup_points_cart(
            groups=[
                create_pickup_point_grouped([_Outlets._Pickup.BLUE_SHOP_1_PITER_1_ID]),
                create_post_point_grouped([_Outlets._Post.MOSCOW_1_ID]),
            ],
            virtual_box=VIRTUAL_BOX,
        )

    def test_blue_offers_courier_combinator_presets_filter(self):
        """
        Проверяем пресеты для одиночного синего оффера:
            - пресеты ПВЗ, курьерской доставки и почты проходят простой фильтр
            - пресеты ПВЗ и курьерской доставки проходят фильтр, т.к. присутствуют в выдаче комбинатора

        """
        input_presets = [
            _Presets._Courier.PRESET_PITER,
            _Presets._Courier.PRESET_MOSCOW,
            _Presets._Pickup.PRESET_PITER_1_1,
            _Presets._Post.PRESET_MOSCOW,
        ]
        expeceted_output_presets = [
            _Presets.available(_Presets._Courier.PRESET_PITER),
            _Presets.available(_Presets._Courier.PRESET_MOSCOW),
            _Presets.available(_Presets._Pickup.PRESET_PITER_1_1),
            _Presets.available(_Presets._Post.PRESET_MOSCOW),
        ]
        request = (
            make_base_request([_Offers._Blue.B_4_PITER])
            + "&rids={}".format(PITER_RID)
            + _Presets.presets_to_request(input_presets)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_blue_offers_only_courier_combinator_presets_filter(cls):
        cls.combinator.on_courier_options_request(
            items=[B_4_PITER_delivery_item],
            destination=Destination(region_id=PITER_RID),
            payment_methods=[],
            total_price=_Offers._Blue.B_4_PITER.price,
        ).respond_with_courier_options(
            options=[DELIVERY_SERVICE_ID_MOSCOW_delivery_option],
            virtual_box=VIRTUAL_BOX,
        )

    def test_blue_offers_only_courier_combinator_presets_filter(self):
        """
        Проверяем пресет только курьерки для одиночного синего оффера:
            - Пресет пройдет и простой фильтр, и фильтр комбинатора, т.к. присутствует в выдаче комбинатора
        """
        input_presets = [
            _Presets._Courier.PRESET_PITER,
        ]
        expeceted_output_presets = [
            _Presets.available(_Presets._Courier.PRESET_PITER),
        ]
        request = (
            make_base_request([_Offers._Blue.B_4_PITER])
            + "&rids={}".format(PITER_RID)
            + _Presets.presets_to_request(input_presets)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_blue_offers_only_pickup_combinator_presets_filter(cls):
        cls.combinator.on_pickup_points_cart_request(
            items=[B_4_PITER_delivery_item],
            point_types=[],
            total_price=_Offers._Blue.B_4_PITER.price,
            post_codes=[],
            logistic_point_ids=[_Outlets._Pickup.BLUE_SHOP_1_PITER_1_ID],
        ).respond_with_pickup_points_cart(
            groups=[create_pickup_point_grouped([_Outlets._Pickup.BLUE_SHOP_1_PITER_1_ID])],
            virtual_box=VIRTUAL_BOX,
        )

    def test_blue_offers_only_pickup_combinator_presets_filter(self):
        """
        Проверяем пресет только ПВЗ для одиночного синего оффера:
            - Пресет пройдет и простой фильтр, и фильтр комбинатора, т.к. присутствует в выдаче комбинатора
        """
        input_presets = [
            _Presets._Pickup.PRESET_PITER_1_1,
        ]
        expeceted_output_presets = [
            _Presets.available(_Presets._Pickup.PRESET_PITER_1_1),
        ]
        request = (
            make_base_request([_Offers._Blue.B_4_PITER])
            + "&rids={}".format(PITER_RID)
            + _Presets.presets_to_request(input_presets)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_blue_offers_only_post_combinator_presets_filter(cls):
        cls.combinator.on_pickup_points_grouped_request(
            items=[B_4_PITER_delivery_item],
            destination_regions=[PITER_RID],
            point_types=[],
            total_price=_Offers._Blue.B_4_PITER.price,
            post_codes=[_Outlets._Post.MOSCOW_1_ID],
        ).respond_with_grouped_pickup_points(
            groups=[
                create_post_point_grouped([_Outlets._Post.MOSCOW_1_ID]),
            ],
            virtual_box=VIRTUAL_BOX,
        )

        cls.combinator.on_pickup_points_cart_request(
            items=[B_4_PITER_delivery_item],
            point_types=[],
            total_price=_Offers._Blue.B_4_PITER.price,
            post_codes=[_Outlets._Post.MOSCOW_1_ID, _Outlets._Post.PITER_1_ID],
            logistic_point_ids=[],
        ).respond_with_pickup_points_cart(
            groups=[
                create_post_point_grouped([_Outlets._Post.MOSCOW_1_ID]),
            ],
            virtual_box=VIRTUAL_BOX,
        )

    def test_blue_offers_only_post_combinator_presets_filter(self):
        """
        Проверяем пресет только почту для одиночного синего оффера:
            - Один пресет проходит фильтр, т.к. присутствует в выдаче комбинатора, а второй - нет,
              показатели фильтрации не зависият от наличия параметра post-index
        """
        input_presets = [
            _Presets._Post.PRESET_MOSCOW,
            _Presets._Post.PRESET_PITER,  # <- не пройдет фильтр, т.к. отсутствует в выдаче комбинатора
        ]
        expeceted_output_presets = [
            _Presets.available(_Presets._Post.PRESET_MOSCOW),
            _Presets.unavailable(_Presets._Post.PRESET_PITER),
        ]
        request = (
            make_base_request([_Offers._Blue.B_4_PITER])
            + "&rids={}".format(PITER_RID)
            + _Presets.presets_to_request(input_presets)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )

        request += "&post-index={}".format(_Outlets._Post.MOSCOW_1_ID)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_blue_offer_presets_combinator_filter(cls):
        cls.combinator.on_courier_options_request(
            items=[B_2_delivery_item],
            destination=Destination(region_id=MOSCOW_RID),
            payment_methods=[],
            total_price=_Offers._Blue.B_2.price,
        ).respond_with_courier_options(
            options=[DELIVERY_SERVICE_ID_MOSCOW_delivery_option],
            virtual_box=VIRTUAL_BOX,
        )
        cls.combinator.on_pickup_points_cart_request(
            items=[B_2_delivery_item],
            point_types=[],
            total_price=_Offers._Blue.B_2.price,
            post_codes=[_Outlets._Post.MOSCOW_1_ID],
            logistic_point_ids=[
                _Outlets._Pickup.DSBS_1_MOSCOW_1_ID,
                _Outlets._Pickup.BLUE_SHOP_2_MOSCOW_1_ID,
                _Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID,
                _Outlets._Pickup.BLUE_SHOP_1_MOSCOW_2_ID,
            ],
        ).respond_with_pickup_points_cart(
            groups=[
                create_pickup_point_grouped([_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID]),
                create_post_point_grouped([_Outlets._Post.MOSCOW_1_ID]),
            ],
            virtual_box=VIRTUAL_BOX,
        )

    def test_blue_offer_presets_combinator_filter(self):
        """
        Проверяем пресеты для одиночного синего оффера:
            - пресеты ПВЗ, курьерской доставки и почты проходят простой фильтр
            - пресеты ПВЗ других магазинов не проходят простой фильтр
            - один пресет курьерской доставки не проходит фильтр комбинатора, т.к. данные отсутствуют в комбинаторе
            - один пресет ПВЗ (из двух, которые прошли простой фильтр) не проходит фильтр комбинатора, т.к. данные отсутствуют в комбинаторе
        """
        input_presets = [
            _Presets._Courier.PRESET_PITER,  # <- не пройдет фильтр, т.к. отсутствует в выдаче комбинатора
            _Presets._Courier.PRESET_MOSCOW,
            _Presets._Pickup.DSBS_OFFER_1_MOSCOW_1,  # <- не пройдет фильтр, т.к. не является аутлетом "нашего" магазина
            _Presets._Pickup.PRESET_MOSCOW_2,  # <- не пройдет фильтр, т.к. не является аутлетом "нашего" магазина
            _Presets._Pickup.PRESET_MOSCOW,
            _Presets._Pickup.PRESET_MOSCOW_1_2,  # <- не пройдет фильтр, т.к. отсутствует на выдаче комбинатора
            _Presets._Post.PRESET_MOSCOW,
        ]
        expeceted_output_presets = [
            _Presets.unavailable(_Presets._Courier.PRESET_PITER),
            _Presets.available(_Presets._Courier.PRESET_MOSCOW),
            _Presets.unavailable(_Presets._Pickup.DSBS_OFFER_1_MOSCOW_1),
            _Presets.unavailable(_Presets._Pickup.PRESET_MOSCOW_2),
            _Presets.available(_Presets._Pickup.PRESET_MOSCOW),
            _Presets.unavailable(_Presets._Pickup.PRESET_MOSCOW_1_2),
            _Presets.available(_Presets._Post.PRESET_MOSCOW),
        ]
        request = (
            make_base_request([_Offers._Blue.B_2])
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.presets_to_request(input_presets)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )

    def test_dsbs_offer_presets_filter(self):
        """
        Проверяем пресеты для одиночного ДСБС оффера:
            - пресеты ПВЗ, курьерской доставки и почты проходят фильтры
            - пресеты ПВЗ других магазинов не проходят фильтры
        """
        input_presets = [
            _Presets._Courier.PRESET_MOSCOW,
            _Presets._Pickup.PRESET_MOSCOW,  # <- не пройдет фильтр, т.к. не является аутлетом "нашего" магазина
            _Presets._Pickup.CLICK_N_COLLECT_OFFER_1_MOSCOW_1,  # <- не пройдет фильтр, т.к. не является аутлетом "нашего" магазина
            _Presets._Pickup.DSBS_OFFER_1_MOSCOW_1,
            _Presets._Post.PRESET_MOSCOW,
        ]
        expeceted_output_presets = [
            _Presets.available(_Presets._Courier.PRESET_MOSCOW),
            _Presets.unavailable(_Presets._Pickup.PRESET_MOSCOW),
            _Presets.unavailable(_Presets._Pickup.CLICK_N_COLLECT_OFFER_1_MOSCOW_1),
            _Presets.available(_Presets._Pickup.DSBS_OFFER_1_MOSCOW_1),
            _Presets.available(_Presets._Post.PRESET_MOSCOW),
        ]
        request = (
            make_base_request([_Offers._White.DSBS_1])
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.presets_to_request(input_presets)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )

    def test_click_and_collect_offer_presets_filter(self):
        """
        Проверяем пресеты для одиночного ClickAndCollect оффера:
            - пресеты ПВЗ магазина проходят фильтры
            - пресеты для курьерской доставки и почты не проходят фильтры (покупка и вывоз происходит непосредственно из магазина)
            - пресеты ПВЗ других магазинов не проходят фильтры
        """
        input_presets = [
            _Presets._Courier.PRESET_MOSCOW,  # <- не пройдет фильтр, т.к. ClickAndCollect-оффер
            _Presets._Pickup.PRESET_MOSCOW,  # <- не пройдет фильтр, т.к. не является аутлетом "нашего" магазина
            _Presets._Pickup.DSBS_OFFER_1_MOSCOW_1,  # <- не пройдет фильтр, т.к. не является аутлетом "нашего" магазина
            _Presets._Pickup.CLICK_N_COLLECT_OFFER_1_MOSCOW_1,
            _Presets._Post.PRESET_MOSCOW,  # <- не пройдет фильтр, т.к. ClickAndCollect-оффер
        ]
        expeceted_output_presets = [
            _Presets.unavailable(_Presets._Courier.PRESET_MOSCOW),
            _Presets.unavailable(_Presets._Pickup.PRESET_MOSCOW),
            _Presets.unavailable(_Presets._Pickup.DSBS_OFFER_1_MOSCOW_1),
            _Presets.available(_Presets._Pickup.CLICK_N_COLLECT_OFFER_1_MOSCOW_1),
            _Presets.unavailable(_Presets._Post.PRESET_MOSCOW),
        ]
        # Т.к. это алкоголь, надо добавить show-alcohol=1
        request = (
            make_base_request([_Offers._Blue.CLICK_N_COLLECT_1])
            + '&show-alcohol=1'
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.presets_to_request(input_presets)
        )
        response = self.report.request_json(request, headers={'X-Market-Req-ID': "1"})
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )

    def test_dsbs_offer_delivery_service_presets_filter(self):
        """
        Проверяем пресеты для одиночного DSBS оффера с признаками fulfillment_program=False и
        ignore_stocks=True - при наличии таких признаков синий оффер является ClickAndCollect
            - Аутлеты почты, ПВЗ и курьерской доставки будут доступны, т.к. это DSBS
        """
        input_presets = [
            _Presets._Courier.PRESET_MOSCOW,
            _Presets._Pickup.DSBS_DELIVERY_SERVICE_OFFER_1_MOSCOW_1,
            _Presets._Post.PRESET_MOSCOW,
        ]
        expeceted_output_presets = [
            _Presets.available(_Presets._Courier.PRESET_MOSCOW),
            _Presets.available(_Presets._Pickup.DSBS_DELIVERY_SERVICE_OFFER_1_MOSCOW_1),
            _Presets.available(_Presets._Post.PRESET_MOSCOW),
        ]

        # Т.к. это алкоголь, надо добавить show-alcohol=1
        request = (
            make_base_request([_Offers._White.DSBS_2])
            + '&show-alcohol=1'
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.presets_to_request(input_presets)
        )
        response = self.report.request_json(request, headers={'X-Market-Req-ID': "1"})
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )

    def test_click_and_collect_offer_2_presets_filter(self):
        """
        Проверяем пресеты для одиночного ClickAndCollect оффера
        """
        input_presets = [
            _Presets._Courier.PRESET_MOSCOW,  # <- не пройдет фильтр, т.к. для C'n'C недоступна курьерка
            _Presets._Pickup.CLICK_N_COLLECT_OFFER_2_MOSCOW_1,
            _Presets._Post.PRESET_MOSCOW,  # <- не пройдет фильтр, т.к. для C'n'C недоступна доставка почтой
        ]
        expeceted_output_presets = [
            _Presets.unavailable(_Presets._Courier.PRESET_MOSCOW),
            _Presets.available(_Presets._Pickup.CLICK_N_COLLECT_OFFER_2_MOSCOW_1),
            _Presets.unavailable(_Presets._Post.PRESET_MOSCOW),
        ]
        # Т.к. это алкоголь, надо добавить show-alcohol=1
        request = (
            make_base_request([_Offers._Blue.CLICK_N_COLLECT_2])
            + '&show-alcohol=1'
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.presets_to_request(input_presets)
        )
        response = self.report.request_json(request, headers={'X-Market-Req-ID': "1"})
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_click_and_collect_mbi_flag_preset(cls):
        cls.delivery_calc.on_request_offer_buckets(
            weight=5, length=100, width=100, height=100, warehouse_id=145
        ).respond([], [_Buckets._Pickup.CLICK_N_COLLECT_3_ID], [])

    def test_click_and_collect_mbi_flag_preset(self):
        """
        Проверяем, что для ClickAndCollect при наличии флага MBI click_and_collect отрабатывает
        логика похода в калькулятор доставки
        """
        input_presets = [
            _Presets._Courier.PRESET_MOSCOW,  # <- не пройдет простой фильтр, т.к. CnC
            _Presets._Pickup.PRESET_MOSCOW,  # <- не пройдет простой фильтр, т.к. аутлет другого магазина
            _Presets._Pickup.CLICK_N_COLLECT_OFFER_3_MOSCOW_1,
            _Presets._Post.PRESET_MOSCOW,  # <- не пройдет простой фильтр, т.к. CnC
        ]
        expeceted_output_presets = [
            _Presets.unavailable(_Presets._Courier.PRESET_MOSCOW),
            _Presets.unavailable(_Presets._Pickup.PRESET_MOSCOW),
            _Presets.available(_Presets._Pickup.CLICK_N_COLLECT_OFFER_3_MOSCOW_1),
            _Presets.unavailable(_Presets._Post.PRESET_MOSCOW),
        ]
        request = (
            make_base_request([_Offers._Blue.CLICK_N_COLLECT_3])
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.presets_to_request(input_presets)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_blue_offers_from_different_shops_presets_filter(cls):
        cls.delivery_calc.on_request_offer_buckets(
            weight=10, length=106, width=106, height=212, warehouse_id=145
        ).respond(
            [_Buckets._Delivery.BLUE_2_ID],
            [_Buckets._Pickup.BLUE_2_ID, _Buckets._Pickup.BLUE_3_ID],
            [_Buckets._Post.BLUE_1_ID],
        )

        cls.combinator.on_courier_options_request(
            items=[
                B_2_delivery_item,
                B_3_delivery_item,
            ],
            destination=Destination(region_id=MOSCOW_RID),
            payment_methods=[],
            total_price=_Offers._Blue.B_2.price + _Offers._Blue.B_3.price,
        ).respond_with_courier_options(
            options=[DELIVERY_SERVICE_ID_MOSCOW_delivery_option],
            virtual_box=VIRTUAL_BOX,
        )
        cls.combinator.on_pickup_points_cart_request(
            items=[
                B_2_delivery_item,
                B_3_delivery_item,
            ],
            point_types=[],
            total_price=_Offers._Blue.B_2.price + _Offers._Blue.B_3.price,
            post_codes=[_Outlets._Post.MOSCOW_1_ID],
            logistic_point_ids=[
                _Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID,
                _Outlets._Pickup.BLUE_SHOP_2_MOSCOW_1_ID,
            ],
        ).respond_with_pickup_points_cart(
            groups=[
                create_pickup_point_grouped(
                    [_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID, _Outlets._Pickup.BLUE_SHOP_2_MOSCOW_1_ID]
                ),
                create_post_point_grouped([_Outlets._Post.MOSCOW_1_ID]),
            ],
            virtual_box=VIRTUAL_BOX,
        )

    def test_blue_offers_from_different_shops_presets_filter(self):
        """
        Проверяем пресеты для двух синих офферов из двух разных магазинов:
            - пресеты курьерской доставки и почты проходят фильтры
            - пресеты ПВЗ магазинов не проходят фильтры (т.к. это два разных ПВЗ, одновременно получить из них товары нельзя)
        """
        input_presets = [
            _Presets._Courier.PRESET_MOSCOW,
            _Presets._Pickup.PRESET_MOSCOW,  # <- не пройдет простой фильтр, т.к. для двух офферов ПВЗ будет недоступен
            _Presets._Pickup.PRESET_MOSCOW_2,  # <- не пройдет простой фильтр, т.к. т.к. для двух офферов ПВЗ будет недоступен
            _Presets._Post.PRESET_MOSCOW,
        ]
        expeceted_output_presets = [
            _Presets.available(_Presets._Courier.PRESET_MOSCOW),
            _Presets.unavailable(_Presets._Pickup.PRESET_MOSCOW),
            _Presets.unavailable(_Presets._Pickup.PRESET_MOSCOW_2),
            _Presets.available(_Presets._Post.PRESET_MOSCOW),
        ]
        request = (
            make_base_request([_Offers._Blue.B_2, _Offers._Blue.B_3])
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.presets_to_request(input_presets)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_presets_rearr_factors(cls):
        cls.combinator.on_courier_options_request(
            items=[B_2_delivery_item],
            destination=Destination(region_id=MOSCOW_RID),
            payment_methods=[],
            total_price=_Offers._Blue.B_2.price,
        ).respond_with_courier_options(
            options=[DELIVERY_SERVICE_ID_MOSCOW_delivery_option],
            virtual_box=VIRTUAL_BOX,
        )
        # запрос к комбинатору только по ПВЗ
        cls.combinator.on_pickup_points_cart_request(
            items=[B_2_delivery_item],
            point_types=[],
            total_price=_Offers._Blue.B_2.price,
            post_codes=[],
            logistic_point_ids=[
                _Outlets._Pickup.DSBS_1_MOSCOW_1_ID,
                _Outlets._Pickup.BLUE_SHOP_2_MOSCOW_1_ID,
                _Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID,
                _Outlets._Pickup.BLUE_SHOP_1_MOSCOW_2_ID,
            ],
        ).respond_with_pickup_points_cart(
            groups=[
                create_pickup_point_grouped([_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID]),
            ],
            virtual_box=VIRTUAL_BOX,
        )
        # запрос к комбинатору только по почте
        cls.combinator.on_pickup_points_cart_request(
            items=[B_2_delivery_item],
            point_types=[],
            total_price=_Offers._Blue.B_2.price,
            post_codes=[_Outlets._Post.MOSCOW_1_ID, _Outlets._Post.PITER_1_ID],
            logistic_point_ids=[],
        ).respond_with_pickup_points_cart(
            groups=[create_post_point_grouped([_Outlets._Post.MOSCOW_1_ID])],
            virtual_box=VIRTUAL_BOX,
        )
        # запрос к комбинатору по ПВЗ и по почте
        cls.combinator.on_pickup_points_cart_request(
            items=[B_2_delivery_item],
            point_types=[],
            total_price=_Offers._Blue.B_2.price,
            post_codes=[
                _Outlets._Post.MOSCOW_1_ID,
                _Outlets._Post.PITER_1_ID,
            ],
            logistic_point_ids=[
                _Outlets._Pickup.DSBS_1_MOSCOW_1_ID,
                _Outlets._Pickup.BLUE_SHOP_2_MOSCOW_1_ID,
                _Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID,
                _Outlets._Pickup.BLUE_SHOP_1_MOSCOW_2_ID,
            ],
        ).respond_with_pickup_points_cart(
            groups=[
                create_pickup_point_grouped([_Outlets._Pickup.BLUE_SHOP_1_MOSCOW_1_ID]),
                create_post_point_grouped([_Outlets._Post.MOSCOW_1_ID]),
            ],
            virtual_box=VIRTUAL_BOX,
        )

    def test_presets_rearr_factors(self):
        """
        Тестируем различные варианты флагов:
         - эксперимент отключен
         - включен только базовый функционал пресетов(работает только базовая фильтрация)
         - включена работа с комбинатором по ПВЗ по пресетам
         - включена работа с комбинатором по курьерке по пресетам
         - включена работа с комбинатором по почте по пресетам
         - базовый функционал отключен, но включена работа с комбинатором (эффект аналогичный отключенному эксперименту)
         - включено все
        """
        input_presets = [
            _Presets._Courier.PRESET_MOSCOW,
            _Presets._Pickup.DSBS_OFFER_1_MOSCOW_1,
            _Presets._Pickup.PRESET_MOSCOW_2,
            _Presets._Pickup.PRESET_MOSCOW,
            _Presets._Pickup.PRESET_MOSCOW_1_2,
            _Presets._Post.PRESET_MOSCOW,
            _Presets._Post.PRESET_PITER,
        ]
        # эксперимент отключен
        expeceted_output_presets = []
        request = (
            make_base_request([_Offers._Blue.B_2])
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.presets_to_request(input_presets)
            + "&rearr-factors=market_actual_delivery_address_presets={}".format(
                TSearchExperimentFlags.EActualDeliveryPresetOptions.ADAPO_STOP
            )
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": EmptyList()},
            },
            allow_different_len=False,
        )
        # включен только базовый функционал (все пресеты автоматически AVAILBALE)
        expeceted_output_presets = [
            _Presets.available(_Presets._Courier.PRESET_MOSCOW),
            _Presets.available(_Presets._Pickup.DSBS_OFFER_1_MOSCOW_1),
            _Presets.available(_Presets._Pickup.PRESET_MOSCOW_2),
            _Presets.available(_Presets._Pickup.PRESET_MOSCOW),
            _Presets.available(_Presets._Pickup.PRESET_MOSCOW_1_2),
            _Presets.available(_Presets._Post.PRESET_MOSCOW),
            _Presets.available(_Presets._Post.PRESET_PITER),
        ]
        request = (
            make_base_request([_Offers._Blue.B_2])
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.presets_to_request(input_presets)
            + "&rearr-factors=market_actual_delivery_address_presets={}".format(
                TSearchExperimentFlags.EActualDeliveryPresetOptions.ADAPO_USE_PRESETS
            )
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )
        # включена только базовая фильтрация
        expeceted_output_presets = [
            _Presets.available(_Presets._Courier.PRESET_MOSCOW),
            _Presets.unavailable(_Presets._Pickup.DSBS_OFFER_1_MOSCOW_1),
            _Presets.unavailable(_Presets._Pickup.PRESET_MOSCOW_2),
            _Presets.available(_Presets._Pickup.PRESET_MOSCOW),
            _Presets.available(_Presets._Pickup.PRESET_MOSCOW_1_2),
            _Presets.available(_Presets._Post.PRESET_MOSCOW),
            _Presets.available(_Presets._Post.PRESET_PITER),
        ]
        request = (
            make_base_request([_Offers._Blue.B_2])
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.presets_to_request(input_presets)
            + "&rearr-factors=market_actual_delivery_address_presets={}".format(
                TSearchExperimentFlags.EActualDeliveryPresetOptions.ADAPO_USE_PRESETS
                | TSearchExperimentFlags.EActualDeliveryPresetOptions.ADAPO_USE_SIMPLE_FILTERS
            )
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )
        # включена работа с комбинатором и простыми фильтрами по ПВЗ по пресетам
        expeceted_output_presets = [
            _Presets.available(_Presets._Courier.PRESET_MOSCOW),
            _Presets.unavailable(_Presets._Pickup.DSBS_OFFER_1_MOSCOW_1),
            _Presets.unavailable(_Presets._Pickup.PRESET_MOSCOW_2),
            _Presets.available(_Presets._Pickup.PRESET_MOSCOW),
            _Presets.unavailable(_Presets._Pickup.PRESET_MOSCOW_1_2),
            _Presets.available(_Presets._Post.PRESET_MOSCOW),
            _Presets.available(_Presets._Post.PRESET_PITER),
        ]
        request = (
            make_base_request([_Offers._Blue.B_2])
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.presets_to_request(input_presets)
            + "&rearr-factors=market_actual_delivery_address_presets={}".format(
                TSearchExperimentFlags.EActualDeliveryPresetOptions.ADAPO_USE_PRESETS
                | TSearchExperimentFlags.EActualDeliveryPresetOptions.ADAPO_USE_SIMPLE_FILTERS
                | TSearchExperimentFlags.EActualDeliveryPresetOptions.ADAPO_USE_COMBINATOR_PICKUP
            )
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )
        # включена работа с комбинатором и простыми фильтрами по курьерке по пресетам
        expeceted_output_presets = [
            _Presets.available(_Presets._Courier.PRESET_MOSCOW),
            _Presets.unavailable(_Presets._Pickup.DSBS_OFFER_1_MOSCOW_1),
            _Presets.unavailable(_Presets._Pickup.PRESET_MOSCOW_2),
            _Presets.available(_Presets._Pickup.PRESET_MOSCOW),
            _Presets.available(_Presets._Pickup.PRESET_MOSCOW_1_2),
            _Presets.available(_Presets._Post.PRESET_MOSCOW),
            _Presets.available(_Presets._Post.PRESET_PITER),
        ]
        request = (
            make_base_request([_Offers._Blue.B_2])
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.presets_to_request(input_presets)
            + "&rearr-factors=market_actual_delivery_address_presets={}".format(
                TSearchExperimentFlags.EActualDeliveryPresetOptions.ADAPO_USE_PRESETS
                | TSearchExperimentFlags.EActualDeliveryPresetOptions.ADAPO_USE_SIMPLE_FILTERS
                | TSearchExperimentFlags.EActualDeliveryPresetOptions.ADAPO_USE_COMBINATOR_COURIER
            )
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )
        # включена работа с комбинатором и простыми фильтрами по почте по пресетам
        expeceted_output_presets = [
            _Presets.available(_Presets._Courier.PRESET_MOSCOW),
            _Presets.unavailable(_Presets._Pickup.DSBS_OFFER_1_MOSCOW_1),
            _Presets.unavailable(_Presets._Pickup.PRESET_MOSCOW_2),
            _Presets.available(_Presets._Pickup.PRESET_MOSCOW),
            _Presets.available(_Presets._Pickup.PRESET_MOSCOW_1_2),
            _Presets.available(_Presets._Post.PRESET_MOSCOW),
            _Presets.unavailable(_Presets._Post.PRESET_PITER),
        ]
        request = (
            make_base_request([_Offers._Blue.B_2])
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.presets_to_request(input_presets)
            + "&rearr-factors=market_actual_delivery_address_presets={}".format(
                TSearchExperimentFlags.EActualDeliveryPresetOptions.ADAPO_USE_PRESETS
                | TSearchExperimentFlags.EActualDeliveryPresetOptions.ADAPO_USE_SIMPLE_FILTERS
                | TSearchExperimentFlags.EActualDeliveryPresetOptions.ADAPO_USE_COMBINATOR_POST
            )
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )
        # базовый функционал отключен, но включена работа с комбинатором (эффект аналогичный отключенному эксперименту)
        expeceted_output_presets = []
        request = (
            make_base_request([_Offers._Blue.B_2])
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.presets_to_request(input_presets)
            + "&rearr-factors=market_actual_delivery_address_presets={}".format(
                TSearchExperimentFlags.EActualDeliveryPresetOptions.ADAPO_USE_COMBINATOR_PICKUP
            )
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": EmptyList()},
            },
            allow_different_len=False,
        )
        # включены все (по умолчанию)
        expeceted_output_presets = [
            _Presets.available(_Presets._Courier.PRESET_MOSCOW),
            _Presets.unavailable(_Presets._Pickup.DSBS_OFFER_1_MOSCOW_1),
            _Presets.unavailable(_Presets._Pickup.PRESET_MOSCOW_2),
            _Presets.available(_Presets._Pickup.PRESET_MOSCOW),
            _Presets.unavailable(_Presets._Pickup.PRESET_MOSCOW_1_2),
            _Presets.available(_Presets._Post.PRESET_MOSCOW),
            _Presets.unavailable(_Presets._Post.PRESET_PITER),
        ]
        request = (
            make_base_request([_Offers._Blue.B_2])
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.presets_to_request(input_presets)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {"addressPresets": _Presets.presets_to_response(expeceted_output_presets)},
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_partial_delivery_enabled(cls):
        pickup = create_pickup_point_grouped([_Outlets._Pickup.BLUE_SHOP_1_PITER_1_ID])
        post = create_post_point_grouped([_Outlets._Post.PITER_1_ID])
        for disable_partial_delivery in [False, True]:
            cls.combinator.on_pickup_points_grouped_request(
                items=[B_3_delivery_item],
                destination_regions=[PITER_RID],
                point_types=[],
                total_price=_Offers._Blue.B_3.price,
                post_codes=[],
                disable_partial_delivery=disable_partial_delivery,
            ).respond_with_grouped_pickup_points(
                groups=[pickup] if disable_partial_delivery else [pickup, post],
                virtual_box=VIRTUAL_BOX,
            )
            cls.combinator.on_courier_options_request(
                items=[B_3_delivery_item],
                destination=Destination(region_id=PITER_RID),
                payment_methods=[],
                total_price=_Offers._Blue.B_3.price,
                disable_partial_delivery=disable_partial_delivery,
            ).respond_with_courier_options(
                options=[
                    DELIVERY_SERVICE_ID_PITER_delivery_option
                    if disable_partial_delivery
                    else DELIVERY_SERVICE_ID_MOSCOW_delivery_option
                ],
                virtual_box=VIRTUAL_BOX,
            )

    def test_partial_delivery_enabled(self):
        '''
        Проверяем что cgi-параметр partial-delivery-enabled
        передается из репорта в ручки комбинатора GetCourierOptions и GetPickupPointsGrouped
        '''
        for partial_delivery_enabled in [0, 1]:
            request = (
                make_base_request([_Offers._Blue.B_3])
                + "&rids={}".format(PITER_RID)
                + "&partial-delivery-enabled={}".format(partial_delivery_enabled)
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "delivery": {
                                    "hasPost": bool(partial_delivery_enabled),
                                    "options": [
                                        {
                                            "serviceId": "123" if partial_delivery_enabled else "121",
                                            "timeIntervals": [
                                                {
                                                    "from": "10:00" if partial_delivery_enabled else "09:00",
                                                    "to": "22:00" if partial_delivery_enabled else "23:00",
                                                }
                                            ],
                                        }
                                    ],
                                }
                            }
                        ]
                    },
                },
                allow_different_len=False,
            )

    def test_presets_trying_available(self):
        '''
        Проверяем, что у опций с доступностью примерки присутствует признак TRYAIBLE,
        а там где не доступна присутствует признак UNTRYAIBLE
        '''
        input_presets = [
            _Presets._Courier.PRESET_PITER,
            _Presets._Courier.PRESET_MOSCOW,
            _Presets._Pickup.PRESET_MOSCOW,
            _Presets._Post.PRESET_MOSCOW,
        ]
        request = (
            make_base_request([_Offers._Blue.B_2])
            + "&rids={}".format(MOSCOW_RID)
            + _Presets.presets_to_request(input_presets)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "addressPresets": [
                        {
                            "id": _Presets._Courier.PRESET_PITER["id"],
                            "parcels": [{"deliveryAvailable": "UNAVAILABLE", "tryingAvailable": "UNTRYAIBLE"}],
                            "rid": _Presets._Courier.PRESET_PITER["rid"],
                            "type": "DELIVERY",
                        },
                        {
                            "id": _Presets._Courier.PRESET_MOSCOW["id"],
                            "parcels": [{"deliveryAvailable": "AVAILABLE", "tryingAvailable": "TRYAIBLE"}],
                            "rid": _Presets._Courier.PRESET_MOSCOW["rid"],
                            "type": "DELIVERY",
                        },
                        {
                            "id": _Presets._Pickup.PRESET_MOSCOW["id"],
                            "parcels": [{"deliveryAvailable": "AVAILABLE", "tryingAvailable": "TRYAIBLE"}],
                            "type": "PICKUP",
                        },
                        {
                            "id": _Presets._Post.PRESET_MOSCOW["id"],
                            "parcels": [{"deliveryAvailable": "AVAILABLE", "tryingAvailable": "UNTRYAIBLE"}],
                            "type": "POST",
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_extra_charge_options_fields(cls):
        cls.combinator.on_courier_options_request(
            items=[B_2_delivery_item_3],
            destination=Destination(region_id=MOSCOW_RID),
            payment_methods=[],
            total_price=_Offers._Blue.B_2_3.price * 3,
        ).respond_with_courier_options(
            options=[DELIVERY_SERVICE_ID_MOSCOW_delivery_option],
            virtual_box=VIRTUAL_BOX,
        )

        cls.index.dynamic_delivery_tariffs += [
            DynamicDeliveryTariff(constant_coef=-110, delivery_type=DynamicDeliveryTariff.DeliveryType.COURIER_PARTHER),
        ]

    def test_extra_charge_options_fields(self):
        """
        Проверяется, что при наличии флага вычисления дополнительной стоимости доставки,
        необходимые поля поставляются в ответе
        """
        base_request = (
            make_base_request([_Offers._Blue.B_2_3] * 3)
            + "&rids={}&".format(MOSCOW_RID)
            + "combinator=1&"
            + "debug=da&"
            + "rearr-factors=market_dynamic_delivery_tariffs={}"
        )
        for dynamic_delivery_tariff in [1, 0]:
            response = self.report.request_json(base_request.format(dynamic_delivery_tariff))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "deliveryGroup",
                            "delivery": {
                                "options": [
                                    {
                                        "extraCharge": {
                                            "value": "867",
                                            "currency": "RUR",
                                            "unitEconomyValue": "-966",
                                            "reasonCodes": [],
                                        }
                                        if dynamic_delivery_tariff
                                        else Absent(),
                                    }
                                ],
                            },
                        }
                    ]
                },
                allow_different_len=True,
            )
            if dynamic_delivery_tariff == 1:
                self.assertFragmentIn(response, {"logicTrace": [Contains("ItemCount: 3, ParcelPrice: 3")]})


if __name__ == '__main__':
    main()
