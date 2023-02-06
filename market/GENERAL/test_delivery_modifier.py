#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import itertools

from core.types import (
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicTimeIntervalsSet,
    DynamicWarehouseAndDeliveryServiceInfo,
    Model,
    Offer,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    RegionalDelivery,
    Shop,
    TimeInfo,
    TimeIntervalInfo,
)

from core.matcher import NotEmpty, Absent
from core.testcase import TestCase, main
from core.types.offer import OfferDimensions
from core.types.payment_methods import Payment
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax
from core.types.delivery import BlueDeliveryTariff, DeliveryServiceModifier


USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"

EXP_NAME = 'old_delivery'
EXP_YA_PLUS = 'ya_plus_threshold'
EXP_UNIFIED_TARIFFS = 'unified'

EXP_REARR = '&rearr-factors=market_blue_tariffs_exp=' + EXP_NAME
EXP_YA_PLUS_REARR = '&rearr-factors=market_blue_tariffs_exp=' + EXP_YA_PLUS

CNC_SHOP = 1000
DS_SELF_DELIVERY = 99


def OUTLET(id, ds, rid, days, price, fesh=None, type=Outlet.FOR_PICKUP, bool_props=[]):
    return (
        Outlet(
            point_id=id,
            delivery_service_id=ds,
            fesh=fesh,
            region=rid,
            point_type=type,
            delivery_option=OutletDeliveryOption(
                shipper_id=ds,
                day_from=days,
                day_to=days,
                order_before=23,
                work_in_holiday=True,
                price=price + 2,  # Эта цена нигде не должна появляться. Сделаем ее отличной от цены в бакете
            ),
            working_days=[i for i in range(10)],
            bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"] + bool_props,
        ),
        PickupBucket(
            bucket_id=id,
            dc_bucket_id=id,
            carriers=[ds if ds else DS_SELF_DELIVERY],
            options=[PickupOption(outlet_id=id, day_from=days, day_to=days, price=price)],
            delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
        ),
    )


def POST_OUTLET(**kwargs):
    return OUTLET(type=Outlet.FOR_POST, **kwargs)


def COURIER_BUCKET(id, ds, rid, days, price):
    return DeliveryBucket(
        bucket_id=id,
        dc_bucket_id=id,
        # fesh=1,
        carriers=[ds],
        regional_options=[
            RegionalDelivery(
                rid=rid,
                options=[DeliveryOption(price=price, day_from=days, day_to=days, shop_delivery_price=5)],
                payment_methods=[Payment.PT_YANDEX, Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
            )
        ],
        delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
    )


PITER_RID = 2
MOSCOW_RID = 213
EKB_RID = 54
VLADIMIR_RID = 97
KGT_RID = 98
KGT_RID_PROGRESS = 37
KGT_RID_PROGRESS_DIM = 38
UNIFIED_RID = 109

"""
По цене тарифы разбиваются на группы:
 * В Питере (до 700, до 2000, более 2000)
 * В Москве (до 900, более 900)
 * В Екб не разбивается

Поэтому возьмем оферы:
 1. До 700
 2. От 700 до 900
 3. От 900 до 2000
 4. Более 2000
"""

T_PRICE_1 = 700
T_PRICE_2 = 900
T_PRICE_3 = 2000

THRESHOLD_MARKET_BRANDED = 755
THRESHOLD_MARKET_POST_TERM = 770
THRESHOLD_MARKET_PARTNER = 790

TO_MARKET_BRANDED = THRESHOLD_MARKET_BRANDED - 1
FROM_MARKET_BRANDED = THRESHOLD_MARKET_BRANDED

THRESHOLD_PRICE = T_PRICE_3

TO_PRICE_1 = T_PRICE_1 - 1
FROM_PRICE_1 = T_PRICE_1
TO_PRICE_2 = T_PRICE_2 - 1
FROM_PRICE_2 = T_PRICE_2
TO_PRICE_3 = T_PRICE_3 - 1
FROM_PRICE_3 = T_PRICE_3

FROM_THRESHOLD = THRESHOLD_PRICE
FROM_THRESHOLD_1 = THRESHOLD_PRICE + 100

PITER_YA_PLUS_THRESHOLD = T_PRICE_1
MOSCOW_YA_PLUS_THRESHOLD = T_PRICE_2

ALL_PRICES = [TO_PRICE_1, FROM_PRICE_1, TO_PRICE_2, FROM_PRICE_2, TO_PRICE_3, FROM_PRICE_3]


DS_EXPENSIVE = 100
DS_CHEAP = 101


class PITER(object):
    # ПВЗ в Питере
    OUTLET_EXPENSIVE, P_BUCKET_EXPENSIVE = OUTLET(id=1000, ds=DS_EXPENSIVE, rid=PITER_RID, days=1, price=500)
    OUTLET_CHEAP, P_BUCKET_CHEAP = OUTLET(id=1001, ds=DS_CHEAP, rid=PITER_RID, days=2, price=50)
    POST_OUTLET, P_BUCKET_POST = POST_OUTLET(id=1003, ds=102, rid=PITER_RID, days=5, price=150)

    CNC_DELIVERY_PRICE = 17
    CNC_OUTLET, CNC_BUCKET = OUTLET(id=1004, ds=None, rid=PITER_RID, days=2, price=17, fesh=CNC_SHOP)

    # Быстрый но дорогой тариф
    C_BUCKET_EXPENSIVE = COURIER_BUCKET(id=1100, ds=DS_EXPENSIVE, rid=PITER_RID, days=1, price=500)
    # Дешевый но долгий тариф
    C_BUCKET_CHEAP = COURIER_BUCKET(id=1101, ds=DS_CHEAP, rid=PITER_RID, days=2, price=50)

    USER_PRICE_1 = 89
    USER_PRICE_2 = 49
    USER_PRICE_3 = 0
    USER_PRICE_HEAVY = 555

    TARIFFS = [
        BlueDeliveryTariff(user_price=USER_PRICE_1, large_size=0, price_to=T_PRICE_1),
        BlueDeliveryTariff(user_price=USER_PRICE_2, large_size=0, price_to=T_PRICE_3),
        BlueDeliveryTariff(user_price=USER_PRICE_3, large_size=0),
        BlueDeliveryTariff(user_price=USER_PRICE_HEAVY),
    ]

    EXP_TARIFFS = [BlueDeliveryTariff(user_price=88, large_size=0), BlueDeliveryTariff(user_price=99)]


class MOSCOW(object):

    USUAL_DS_ID = 200
    SPECIAL_DS_ID = 203
    PICKUP_OUTLET, P_BUCKET = OUTLET(id=2000, ds=USUAL_DS_ID, rid=MOSCOW_RID, days=1, price=600)
    PICKUP_OUTLET2, P_BUCKET2 = OUTLET(id=2003, ds=SPECIAL_DS_ID, rid=MOSCOW_RID, days=1, price=600)
    POST_OUTLET, P_BUCKET_POST = POST_OUTLET(id=2001, ds=201, rid=MOSCOW_RID, days=5, price=160)

    # Брэндированные ПВЗ маркета. Пока что определяются по СД
    PICKUP_OUTLET_BRANDED, P_BUCKET_BRANDED = OUTLET(
        id=2002, ds=1005483, rid=MOSCOW_RID, days=2, price=640, bool_props=['isMarketBranded']
    )

    # Брэндированные постаматы маркета.
    PICKUP_POST_TERM, P_BUCKET_POST_TERM = OUTLET(
        id=2005, ds=1005484, rid=MOSCOW_RID, days=2, price=640, bool_props=['isMarketPostTerm']
    )

    # ПВЗ партнеров маркета.
    PICKUP_OUTLET_PARTNER, P_BUCKET_PARTNER = OUTLET(
        id=2006, ds=1005485, rid=MOSCOW_RID, days=2, price=640, bool_props=['isMarketPartner']
    )

    CNC_DELIVERY_PRICE = 18
    CNC_OUTLET, CNC_BUCKET = OUTLET(id=2004, ds=None, rid=MOSCOW_RID, days=2, price=18, fesh=CNC_SHOP)

    C_BUCKET = COURIER_BUCKET(id=2100, ds=210, rid=MOSCOW_RID, days=1, price=620)

    # тариф в Москве
    USER_PRICE_1 = 399
    USER_PRICE_2 = 149

    USER_PRICE_HEAVY_1 = 666
    USER_PRICE_HEAVY_2 = 999

    TARIFFS = [
        BlueDeliveryTariff(user_price=USER_PRICE_1, large_size=0, price_to=T_PRICE_2),
        BlueDeliveryTariff(user_price=USER_PRICE_2, large_size=0),
        BlueDeliveryTariff(user_price=USER_PRICE_HEAVY_1, large_size=1, price_to=T_PRICE_2),
        BlueDeliveryTariff(user_price=USER_PRICE_HEAVY_2, large_size=1),
    ]

    SPECIAL_DS_PRICE_1 = 780
    SPECIAL_DS_PRICE_2 = 480

    DELIVERY_SERVICE_MODIFIER = [
        DeliveryServiceModifier(
            service_ids=[SPECIAL_DS_ID],
            tariffs=[
                BlueDeliveryTariff(user_price=SPECIAL_DS_PRICE_1, price_to=T_PRICE_2),
                BlueDeliveryTariff(user_price=SPECIAL_DS_PRICE_2),
            ],
        )
    ]

    EXP_TARIFFS = [BlueDeliveryTariff(user_price=77, large_size=0), BlueDeliveryTariff(user_price=66)]


class EKB(object):
    PICKUP_OUTLET, P_BUCKET = OUTLET(id=3000, ds=300, rid=EKB_RID, days=1, price=700)
    POST_OUTLET, P_BUCKET_POST = POST_OUTLET(id=3001, ds=301, rid=EKB_RID, days=5, price=170)

    CNC_DELIVERY_PRICE = 19
    CNC_OUTLET, CNC_BUCKET = OUTLET(id=3004, ds=None, rid=EKB_RID, days=2, price=19, fesh=CNC_SHOP)

    C_BUCKET = COURIER_BUCKET(id=3100, ds=310, rid=EKB_RID, days=1, price=720)

    # Тариф для всех посылок
    USER_PRICE = 1234

    TARIFFS = [
        BlueDeliveryTariff(user_price=USER_PRICE),
    ]

    EXP_TARIFFS = [BlueDeliveryTariff(user_price=55, large_size=0), BlueDeliveryTariff(user_price=44)]


# Тарифы с разными ценами для разных типов доставки
class VLADIMIR(object):
    PICKUP_OUTLET, P_BUCKET = OUTLET(id=4000, ds=400, rid=VLADIMIR_RID, days=1, price=400)
    POST_OUTLET, P_BUCKET_POST = POST_OUTLET(id=4001, ds=401, rid=VLADIMIR_RID, days=5, price=470)

    C_BUCKET = COURIER_BUCKET(id=4100, ds=410, rid=VLADIMIR_RID, days=1, price=420)

    # Тариф для всех посылок
    USER_PRICE = 1333
    COURIER_PRICE = 1335
    PICKUP_PRICE = 1336
    POST_PRICE = 1337

    TARIFFS = [
        BlueDeliveryTariff(
            large_size=0,
            user_price=USER_PRICE,
            courier_price=COURIER_PRICE,
            pickup_price=PICKUP_PRICE,
            post_price=POST_PRICE,
        ),
        BlueDeliveryTariff(
            large_size=1,
            user_price=USER_PRICE,
            courier_price=COURIER_PRICE,
            # Цена доставки в ПВЗ не задана. Будет использоваться user_price
            post_price=POST_PRICE,
        ),
    ]


# Настройки КГТ через тарифы
class KGT_REGION(object):
    PICKUP_OUTLET, P_BUCKET = OUTLET(id=5000, ds=500, rid=KGT_RID, days=1, price=400)
    POST_OUTLET, P_BUCKET_POST = POST_OUTLET(id=5001, ds=501, rid=KGT_RID, days=5, price=470)

    C_BUCKET = COURIER_BUCKET(id=5100, ds=510, rid=KGT_RID, days=1, price=420)

    # Тариф для всех посылок
    USER_PRICE = 333
    USER_PRICE_HEAVY = 334

    LIGHT_FROM_CONFIG = 29.9
    HEAVY_FROM_CONFIG = 30.0

    TARIFFS = [
        BlueDeliveryTariff(
            large_size=0,
            user_price=USER_PRICE,
        ),
        BlueDeliveryTariff(
            large_size=1,
            user_price=USER_PRICE_HEAVY,
        ),
    ]


class UNIFIED_REGION(object):
    PICKUP_OUTLET, P_BUCKET = OUTLET(id=6000, ds=600, rid=UNIFIED_RID, days=1, price=400)
    POST_OUTLET, P_BUCKET_POST = POST_OUTLET(id=6001, ds=601, rid=UNIFIED_RID, days=5, price=470)

    C_BUCKET = COURIER_BUCKET(id=6100, ds=610, rid=UNIFIED_RID, days=1, price=420)

    # Тариф для всех посылок
    USER_PRICE = 277
    USER_PRICE_HEAVY = 477
    USER_PRICE_PLUS = 1
    USER_PRICE_FOR_EXPENSIVE = 120
    USER_PRICE_FOR_EXPENSIVE_PLUS = 0

    EXPENSIVE_THRESHOLD = 1000
    VERY_EXPENSIVE_THRESHOLD = 2000

    VERY_LIGHT_FROM_CONFIG = 1.0
    LIGHT_FROM_CONFIG = 29.9
    HEAVY_FROM_CONFIG = 30.0

    TARIFFS = [
        BlueDeliveryTariff(large_size=0, for_plus=1, user_price=USER_PRICE_PLUS, price_to=EXPENSIVE_THRESHOLD),
        BlueDeliveryTariff(large_size=0, for_plus=1, user_price=USER_PRICE_FOR_EXPENSIVE_PLUS),
        BlueDeliveryTariff(large_size=0, for_plus=0, user_price=USER_PRICE, price_to=EXPENSIVE_THRESHOLD),
        BlueDeliveryTariff(
            large_size=0, for_plus=0, user_price=USER_PRICE_FOR_EXPENSIVE, price_to=VERY_EXPENSIVE_THRESHOLD
        ),
        BlueDeliveryTariff(
            large_size=0,
            for_plus=0,
            user_price=10,
        ),
        BlueDeliveryTariff(large_size=1, user_price=USER_PRICE_HEAVY),
    ]


class KGT_REGION_PROGRESSIVE(object):
    PICKUP_OUTLET, P_BUCKET = OUTLET(id=7000, ds=700, rid=KGT_RID_PROGRESS, days=1, price=400)
    POST_OUTLET, P_BUCKET_POST = POST_OUTLET(id=7001, ds=701, rid=KGT_RID_PROGRESS, days=5, price=470)

    C_BUCKET = COURIER_BUCKET(id=7100, ds=710, rid=KGT_RID_PROGRESS, days=1, price=420)
    HEAVY_FROM_CONFIG = 21.1

    NON_KGT_TARIFFS = [
        BlueDeliveryTariff(
            large_size=0,
            user_price=49,
        ),
    ]

    NON_WEIGHT_KGT_TARIFFS = [
        BlueDeliveryTariff(
            large_size=1,
            user_price=549,
        ),
    ]

    WEIGHT_TARIFFS = [
        BlueDeliveryTariff(
            large_size=1,
            weight_threshold=10000,
            user_price=11999,
        ),
        BlueDeliveryTariff(
            large_size=1,
            weight_threshold=5000,
            user_price=8999,
        ),
        BlueDeliveryTariff(
            large_size=1,
            weight_threshold=3000,
            user_price=5499,
        ),
        BlueDeliveryTariff(
            large_size=1,
            weight_threshold=1500,
            user_price=3999,
        ),
        BlueDeliveryTariff(
            large_size=1,
            weight_threshold=500,
            user_price=2199,
        ),
        BlueDeliveryTariff(
            large_size=1,
            weight_threshold=200,
            user_price=1199,
        ),
        BlueDeliveryTariff(
            large_size=1,
            weight_threshold=100,
            user_price=899,
        ),
        BlueDeliveryTariff(
            large_size=1,
            weight_threshold=0,
            user_price=549,
        ),
    ]

    TARIFFS = NON_KGT_TARIFFS + WEIGHT_TARIFFS + NON_WEIGHT_KGT_TARIFFS

    DSBS_TARIFFS = [
        BlueDeliveryTariff(
            large_size=tariff.large_size,
            weight_threshold=tariff.weight_threshold,
            dsbs_payment=tariff.user_price + 1,
            is_dsbs_payment=True,
        )
        for tariff in TARIFFS
    ]


class KGT_REGION_PROGRESSIVE_WITH_DIM(object):
    HEAVY_FROM_CONFIG = 21.1

    NON_KGT_TARIFFS = [
        BlueDeliveryTariff(
            large_size=0,
            user_price=49,
        ),
    ]

    NON_DIM_KGT_TARIFFS = [
        BlueDeliveryTariff(
            large_size=1,
            user_price=549,
        ),
    ]

    DIM_TARIFFS = [
        BlueDeliveryTariff(
            large_size=1,
            weight_threshold=10000,
            volume_threshold=50,
            max_item_dim_threshold=7.5,
            user_price=11999,
        ),
        BlueDeliveryTariff(
            large_size=1,
            weight_threshold=5000,
            volume_threshold=24,
            max_item_dim_threshold=5,
            user_price=8999,
        ),
        BlueDeliveryTariff(
            large_size=1,
            weight_threshold=1500,
            volume_threshold=15,
            max_item_dim_threshold=4,
            user_price=5499,
        ),
        BlueDeliveryTariff(
            large_size=1,
            weight_threshold=1500,
            volume_threshold=7,
            max_item_dim_threshold=3,
            user_price=3999,
        ),
        BlueDeliveryTariff(
            large_size=1,
            weight_threshold=500,
            volume_threshold=2,
            max_item_dim_threshold=2,
            user_price=2199,
        ),
        BlueDeliveryTariff(
            large_size=1,
            weight_threshold=200,
            volume_threshold=1,
            max_item_dim_threshold=1.5,
            user_price=1199,
        ),
        BlueDeliveryTariff(
            large_size=1,
            weight_threshold=100,
            volume_threshold=0.5,
            user_price=899,
        ),
        BlueDeliveryTariff(
            large_size=1,
            weight_threshold=0,
            user_price=549,
        ),
    ]

    TARIFFS = NON_KGT_TARIFFS + DIM_TARIFFS + NON_DIM_KGT_TARIFFS

    DSBS_TARIFFS = [
        BlueDeliveryTariff(
            large_size=tariff.large_size,
            weight_threshold=tariff.weight_threshold,
            volume_threshold=tariff.volume_threshold,
            max_item_dim_threshold=tariff.max_item_dim_threshold,
            dsbs_payment=tariff.user_price + 1,
            is_dsbs_payment=True,
        )
        for tariff in TARIFFS
    ]


def BLUE_OFFER(id, w, price, **kwargs):
    return BlueOffer(
        price=price,
        vat=Vat.VAT_10,
        offerid=id,
        waremd5=(id + "_" * 21)[:21] + 'g',
        weight=w,
        blue_weight=w,
        dimensions=OfferDimensions(length=20, width=30, height=10),
        blue_dimensions=OfferDimensions(length=20, width=30, height=10),
        cargo_types=[44, 55, 66],
        delivery_buckets=[
            PITER.C_BUCKET_EXPENSIVE.bucket_id,
            PITER.C_BUCKET_CHEAP.bucket_id,
            MOSCOW.C_BUCKET.bucket_id,
            EKB.C_BUCKET.bucket_id,
            VLADIMIR.C_BUCKET.bucket_id,
            KGT_REGION.C_BUCKET.bucket_id,
            UNIFIED_REGION.C_BUCKET.bucket_id,
        ],
        pickup_buckets=[
            PITER.P_BUCKET_EXPENSIVE.bucket_id,
            PITER.P_BUCKET_CHEAP.bucket_id,
            MOSCOW.P_BUCKET.bucket_id,
            MOSCOW.P_BUCKET2.bucket_id,
            MOSCOW.P_BUCKET_BRANDED.bucket_id,
            MOSCOW.P_BUCKET_POST_TERM.bucket_id,
            MOSCOW.P_BUCKET_PARTNER.bucket_id,
            EKB.P_BUCKET.bucket_id,
            VLADIMIR.P_BUCKET.bucket_id,
            KGT_REGION.P_BUCKET.bucket_id,
            UNIFIED_REGION.P_BUCKET.bucket_id,
        ],
        post_term_delivery=True,
        post_buckets=[
            PITER.P_BUCKET_POST.bucket_id,
            MOSCOW.P_BUCKET_POST.bucket_id,
            EKB.P_BUCKET_POST.bucket_id,
            VLADIMIR.P_BUCKET_POST.bucket_id,
            KGT_REGION.P_BUCKET_POST.bucket_id,
            UNIFIED_REGION.P_BUCKET_POST.bucket_id,
        ],
        **kwargs
    )


def CNC_BLUE_OFFER(id, w, price, **kwargs):
    return BlueOffer(
        price=price,
        vat=Vat.VAT_10,
        feedid=CNC_SHOP,
        offerid=id,
        is_fulfillment=False,
        waremd5=(id + "_" * 21)[:21] + 'g',
        weight=w,
        dimensions=OfferDimensions(length=20, width=30, height=10),
        cargo_types=[44, 55, 66],
        pickup_buckets=[PITER.CNC_BUCKET.bucket_id, MOSCOW.CNC_BUCKET.bucket_id, EKB.CNC_BUCKET.bucket_id],
        post_term_delivery=False,
    )


LIGHT = 19.9
HEAVY = 20.0


class OFFERS(object):
    OFFER_TO_1 = BLUE_OFFER(id='offer_to_1', w=LIGHT, price=TO_PRICE_1)
    OFFER_FROM_1 = BLUE_OFFER(id='offer_from_1', w=LIGHT, price=FROM_PRICE_1)
    OFFER_TO_2 = BLUE_OFFER(id='offer_to_2', w=LIGHT, price=TO_PRICE_2)
    OFFER_FROM_2 = BLUE_OFFER(id='offer_from_2', w=LIGHT, price=FROM_PRICE_2)
    OFFER_TO_3 = BLUE_OFFER(id='offer_to_3', w=LIGHT, price=TO_PRICE_3)
    OFFER_FROM_3 = BLUE_OFFER(id='offer_from_3', w=LIGHT, price=FROM_PRICE_3)

    OFFER_THRESHOLD = BLUE_OFFER(id='offer_threshold', w=LIGHT, price=FROM_THRESHOLD)
    OFFER_THRESHOLD_1 = BLUE_OFFER(id='offer_threshold_1', w=LIGHT, price=FROM_THRESHOLD_1)

    HEAVY_OFFER_TO_1 = BLUE_OFFER(id='h_offer_to_1', w=HEAVY, price=TO_PRICE_1)
    HEAVY_OFFER_FROM_1 = BLUE_OFFER(id='h_offer_from_1', w=HEAVY, price=FROM_PRICE_1)
    HEAVY_OFFER_TO_2 = BLUE_OFFER(id='h_offer_to_2', w=HEAVY, price=TO_PRICE_2)
    HEAVY_OFFER_FROM_2 = BLUE_OFFER(id='h_offer_from_2', w=HEAVY, price=FROM_PRICE_2)
    HEAVY_OFFER_TO_3 = BLUE_OFFER(id='h_offer_to_3', w=HEAVY, price=TO_PRICE_3)
    HEAVY_OFFER_FROM_3 = BLUE_OFFER(id='h_offer_from_3', w=HEAVY, price=FROM_PRICE_3)

    HEAVY_OFFER_THRESHOLD = BLUE_OFFER(id='h_offer_threshold', w=HEAVY, price=FROM_THRESHOLD)

    CNC_OFFER_TO_1 = CNC_BLUE_OFFER(id='cnc_offer_to_1', w=LIGHT, price=TO_PRICE_1)
    CNC_OFFER_FROM_3 = CNC_BLUE_OFFER(id='cnc_offer_from_3', w=HEAVY, price=FROM_PRICE_3)

    KGT_OFFER_LIGHT = BLUE_OFFER(id='kgt_offer_light', w=KGT_REGION.LIGHT_FROM_CONFIG, price=100500)
    KGT_OFFER_HEAVY = BLUE_OFFER(id='kgt_offer_heavy', w=KGT_REGION.HEAVY_FROM_CONFIG, price=100500)

    UNIFIED_TEST_OFFER_VERY_LIGHT = BLUE_OFFER(
        id='unified_offer_very_light', w=UNIFIED_REGION.VERY_LIGHT_FROM_CONFIG, price=2500
    )
    UNIFIED_TEST_OFFER_LIGHT = BLUE_OFFER(id='unified_offer_light', w=UNIFIED_REGION.LIGHT_FROM_CONFIG, price=100)
    UNIFIED_TEST_OFFER_HEAVY = BLUE_OFFER(id='unified_offer_heavy', w=UNIFIED_REGION.HEAVY_FROM_CONFIG, price=100)

    OFFER_FREE_DELIVERY_WITH_PLUS = BLUE_OFFER(id='plus_free_delivery', w=UNIFIED_REGION.LIGHT_FROM_CONFIG, price=1500)
    OFFER_NEVER_FREE_DELIVERY = BLUE_OFFER(id='not_free_delivery', w=UNIFIED_REGION.LIGHT_FROM_CONFIG, price=150)

    ALL = [
        OFFER_TO_1,
        OFFER_FROM_1,
        OFFER_TO_2,
        OFFER_FROM_2,
        OFFER_TO_3,
        OFFER_FROM_3,
        HEAVY_OFFER_TO_1,
        HEAVY_OFFER_FROM_1,
        HEAVY_OFFER_TO_2,
        HEAVY_OFFER_FROM_2,
        HEAVY_OFFER_TO_3,
        HEAVY_OFFER_FROM_3,
        CNC_OFFER_TO_1,
        CNC_OFFER_FROM_3,
        OFFER_THRESHOLD,
        OFFER_THRESHOLD_1,
        HEAVY_OFFER_THRESHOLD,
        KGT_OFFER_LIGHT,
        KGT_OFFER_HEAVY,
        UNIFIED_TEST_OFFER_VERY_LIGHT,
        UNIFIED_TEST_OFFER_LIGHT,
        UNIFIED_TEST_OFFER_HEAVY,
        OFFER_FREE_DELIVERY_WITH_PLUS,
        OFFER_NEVER_FREE_DELIVERY,
    ]

    WHITE_OFFERS = [
        Offer(
            fesh=703,
            title='kgt_progressive_light',
            delivery_buckets=[1706],
            weight=1,
            price=150,
            blue_weight=1,
            dimensions=OfferDimensions(length=1.3, width=2.1, height=3.2),
            blue_dimensions=OfferDimensions(length=1.3, width=2.1, height=3.2),
            waremd5=Offer.generate_waremd5("kgt_w_light"),
            cpa=Offer.CPA_REAL,
            delivery_options=[DeliveryOption(price=300, day_from=1, day_to=1)],
        ),
    ] + [
        Offer(
            fesh=703,
            title='w_gt_' + str(tariff.weight_threshold),
            weight=tariff.weight_threshold + 35,
            blue_weight=tariff.weight_threshold + 35,
            delivery_buckets=[1706],
            price=150,
            dimensions=OfferDimensions(length=1.3, width=2.1, height=3.2),
            blue_dimensions=OfferDimensions(length=1.3, width=2.1, height=3.2),
            waremd5=Offer.generate_waremd5("kgt_w" + str(tariff.weight_threshold)),
            cpa=Offer.CPA_REAL,
            delivery_options=[DeliveryOption(price=300, day_from=1, day_to=1)],
        )
        for tariff in KGT_REGION_PROGRESSIVE.WEIGHT_TARIFFS
    ]  # Порядок офферов важен для тестов!

    WHITE_OFFERS_WITH_DIM = []

    for tariff in KGT_REGION_PROGRESSIVE_WITH_DIM.DIM_TARIFFS[:-1]:
        cb = (tariff.volume_threshold * 1000000 + 10) ** (1.0 / 3.0)  # объём делаем через "куб" чтобы точно
        # не было конфликта между объёмным трешхолдом
        # и трешхолдом по максимальной размерности
        # оффер с объёмом выше трешхолда тарифа
        WHITE_OFFERS_WITH_DIM += [
            Offer(
                fesh=703,
                title=("dim_vol_d" + str(tariff.max_item_dim_threshold) + "_v" + str(tariff.volume_threshold)).replace(
                    ".", "_"
                ),
                weight=1,
                blue_weight=1,
                delivery_buckets=[1706],
                price=150,
                dimensions=OfferDimensions(length=cb, width=cb, height=cb),
                blue_dimensions=OfferDimensions(length=cb, width=cb, height=cb),
                waremd5=Offer.generate_waremd5(
                    ("dim_vol_d" + str(tariff.max_item_dim_threshold) + "_v" + str(tariff.volume_threshold)).replace(
                        ".", "_"
                    )
                ),
                cpa=Offer.CPA_REAL,
                delivery_options=[DeliveryOption(price=300, day_from=1, day_to=1)],
            ),
        ]
        # оффер с максимальной размерностью выше трешхолда тарифа
        # т.к. в текущей сетке тарифов не всегда определяется трешхолд по максимальной размерности
        # не для всех тарифов заводим оффера по трешхолду максимальной размерности
        if tariff.max_item_dim_threshold is None:
            continue
        WHITE_OFFERS_WITH_DIM += [
            Offer(
                fesh=703,
                title=("md_gt_d" + str(tariff.max_item_dim_threshold) + "_v" + str(tariff.volume_threshold)).replace(
                    ".", "_"
                ),
                weight=1,
                blue_weight=1,
                delivery_buckets=[1706],
                price=150,
                dimensions=OfferDimensions(length=1, width=1, height=(tariff.max_item_dim_threshold * 100 + 1)),
                blue_dimensions=OfferDimensions(length=1, width=1, height=(tariff.max_item_dim_threshold * 100 + 1)),
                waremd5=Offer.generate_waremd5(
                    ("md_gt_d" + str(tariff.max_item_dim_threshold) + "_v" + str(tariff.volume_threshold)).replace(
                        ".", "_"
                    )
                ),
                cpa=Offer.CPA_REAL,
                delivery_options=[DeliveryOption(price=300, day_from=1, day_to=1)],
            )
        ]  # Порядок офферов важен для тестов!


# ##########################################################
# Ниже перечислены группы значений:
#    * Регион доставки
#    * Офер (его вес и цена заданы в самом офере)
#    * Ожидаемая цена доставки по тарифу
# ##########################################################
piter_light_part = [
    # ##########################################################
    # Питер
    # Цены до T_PRICE_1
    (PITER_RID, OFFERS.OFFER_TO_1, PITER.USER_PRICE_1),
    # Между T_PRICE_1 и T_PRICE_3
    (PITER_RID, OFFERS.OFFER_FROM_1, PITER.USER_PRICE_2),
    (PITER_RID, OFFERS.OFFER_TO_2, PITER.USER_PRICE_2),
    (PITER_RID, OFFERS.OFFER_FROM_2, PITER.USER_PRICE_2),
    (PITER_RID, OFFERS.OFFER_TO_3, PITER.USER_PRICE_2),
    # После T_PRICE_3
    (PITER_RID, OFFERS.OFFER_FROM_3, PITER.USER_PRICE_3),
    # Бесплатная доставка
    (PITER_RID, OFFERS.OFFER_THRESHOLD, 0),
]
moscow_light_part = [
    # ##########################################################
    # Москва
    # Цены до T_PRICE_2
    (MOSCOW_RID, OFFERS.OFFER_TO_1, MOSCOW.USER_PRICE_1),
    (MOSCOW_RID, OFFERS.OFFER_FROM_1, MOSCOW.USER_PRICE_1),
    (MOSCOW_RID, OFFERS.OFFER_TO_2, MOSCOW.USER_PRICE_1),
    # После T_PRICE_2
    (MOSCOW_RID, OFFERS.OFFER_FROM_2, MOSCOW.USER_PRICE_2),
    (MOSCOW_RID, OFFERS.OFFER_TO_3, MOSCOW.USER_PRICE_2),
    (MOSCOW_RID, OFFERS.OFFER_FROM_3, MOSCOW.USER_PRICE_2),
    # Бесплатной доставки нет
]
ekb_light_part = [
    # ##########################################################
    # Екб
    # Единный тариф на все
    (EKB_RID, OFFERS.OFFER_TO_1, EKB.USER_PRICE),
    (EKB_RID, OFFERS.OFFER_FROM_1, EKB.USER_PRICE),
    (EKB_RID, OFFERS.OFFER_TO_2, EKB.USER_PRICE),
    (EKB_RID, OFFERS.OFFER_FROM_2, EKB.USER_PRICE),
    (EKB_RID, OFFERS.OFFER_TO_3, EKB.USER_PRICE),
    (EKB_RID, OFFERS.OFFER_FROM_3, EKB.USER_PRICE),
    # Бесплатной доставки нет
]

piter_heavy_part = [
    # КГТ в Питере имеет одну цену
    (PITER_RID, OFFERS.HEAVY_OFFER_TO_1, PITER.USER_PRICE_HEAVY),
    (PITER_RID, OFFERS.HEAVY_OFFER_FROM_1, PITER.USER_PRICE_HEAVY),
    (PITER_RID, OFFERS.HEAVY_OFFER_TO_2, PITER.USER_PRICE_HEAVY),
    (PITER_RID, OFFERS.HEAVY_OFFER_FROM_2, PITER.USER_PRICE_HEAVY),
    (PITER_RID, OFFERS.HEAVY_OFFER_TO_3, PITER.USER_PRICE_HEAVY),
    (PITER_RID, OFFERS.HEAVY_OFFER_FROM_3, PITER.USER_PRICE_HEAVY),
    # Бесплатная доставка не работает для тяжелых оферов
    (PITER_RID, OFFERS.HEAVY_OFFER_THRESHOLD, PITER.USER_PRICE_HEAVY),
]
moscow_heavy_part = [
    # КГТ в Москве отличается по цене посылки
    # Цена до T_PRICE_2
    (MOSCOW_RID, OFFERS.HEAVY_OFFER_TO_1, MOSCOW.USER_PRICE_HEAVY_1),
    (MOSCOW_RID, OFFERS.HEAVY_OFFER_FROM_1, MOSCOW.USER_PRICE_HEAVY_1),
    (MOSCOW_RID, OFFERS.HEAVY_OFFER_TO_2, MOSCOW.USER_PRICE_HEAVY_1),
    # Цена после T_PRICE_2
    (MOSCOW_RID, OFFERS.HEAVY_OFFER_FROM_2, MOSCOW.USER_PRICE_HEAVY_2),
    (MOSCOW_RID, OFFERS.HEAVY_OFFER_TO_3, MOSCOW.USER_PRICE_HEAVY_2),
    (MOSCOW_RID, OFFERS.HEAVY_OFFER_FROM_3, MOSCOW.USER_PRICE_HEAVY_2),
    # Бесплатная доставка не работает для тяжелых оферов
    (MOSCOW_RID, OFFERS.HEAVY_OFFER_THRESHOLD, MOSCOW.USER_PRICE_HEAVY_2),
]
ekb_heavy_part = [
    # Даже КГТ в Екб имеет ту же цену, что и обычные посылки
    (EKB_RID, OFFERS.HEAVY_OFFER_TO_1, EKB.USER_PRICE),
    (EKB_RID, OFFERS.HEAVY_OFFER_FROM_1, EKB.USER_PRICE),
    (EKB_RID, OFFERS.HEAVY_OFFER_TO_2, EKB.USER_PRICE),
    (EKB_RID, OFFERS.HEAVY_OFFER_FROM_2, EKB.USER_PRICE),
    (EKB_RID, OFFERS.HEAVY_OFFER_TO_3, EKB.USER_PRICE),
    (EKB_RID, OFFERS.HEAVY_OFFER_FROM_3, EKB.USER_PRICE),
    # Бесплатная доставка не работает для тяжелых оферов
    (EKB_RID, OFFERS.HEAVY_OFFER_THRESHOLD, EKB.USER_PRICE),
]

regions_and_prices = (
    piter_light_part + moscow_light_part + ekb_light_part + piter_heavy_part + moscow_heavy_part + ekb_heavy_part
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.blue_market_free_delivery_threshold = THRESHOLD_PRICE
        cls.settings.nordstream_autogenerate = False
        cls.settings.default_search_experiment_flags += ['market_nordstream=0']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.outlets += [
            PITER.OUTLET_EXPENSIVE,
            PITER.OUTLET_CHEAP,
            PITER.POST_OUTLET,
            MOSCOW.PICKUP_OUTLET,
            MOSCOW.PICKUP_OUTLET2,
            MOSCOW.POST_OUTLET,
            MOSCOW.PICKUP_OUTLET_BRANDED,
            MOSCOW.PICKUP_POST_TERM,
            MOSCOW.PICKUP_OUTLET_PARTNER,
            EKB.PICKUP_OUTLET,
            EKB.POST_OUTLET,
            VLADIMIR.PICKUP_OUTLET,
            VLADIMIR.POST_OUTLET,
            KGT_REGION.PICKUP_OUTLET,
            KGT_REGION.POST_OUTLET,
            UNIFIED_REGION.PICKUP_OUTLET,
            UNIFIED_REGION.POST_OUTLET,
            KGT_REGION_PROGRESSIVE.POST_OUTLET,
            KGT_REGION_PROGRESSIVE.PICKUP_OUTLET,
        ]

        cls.index.pickup_buckets += [
            PITER.P_BUCKET_EXPENSIVE,
            PITER.P_BUCKET_CHEAP,
            PITER.P_BUCKET_POST,
            MOSCOW.P_BUCKET,
            MOSCOW.P_BUCKET2,
            MOSCOW.P_BUCKET_POST,
            MOSCOW.P_BUCKET_BRANDED,
            MOSCOW.P_BUCKET_POST_TERM,
            MOSCOW.P_BUCKET_PARTNER,
            EKB.P_BUCKET,
            EKB.P_BUCKET_POST,
            VLADIMIR.P_BUCKET,
            VLADIMIR.P_BUCKET_POST,
            KGT_REGION.P_BUCKET,
            KGT_REGION.P_BUCKET_POST,
            UNIFIED_REGION.P_BUCKET,
            UNIFIED_REGION.P_BUCKET_POST,
            KGT_REGION_PROGRESSIVE.P_BUCKET,
            KGT_REGION_PROGRESSIVE.P_BUCKET_POST,
        ]

        cls.index.delivery_buckets += [
            PITER.C_BUCKET_EXPENSIVE,
            PITER.C_BUCKET_CHEAP,
            MOSCOW.C_BUCKET,
            EKB.C_BUCKET,
            VLADIMIR.C_BUCKET,
            KGT_REGION.C_BUCKET,
            UNIFIED_REGION.C_BUCKET,
            KGT_REGION_PROGRESSIVE.C_BUCKET,
        ]

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                cpa=Shop.CPA_REAL,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                delivery_service_outlets=[
                    PITER.OUTLET_EXPENSIVE.point_id,
                    PITER.OUTLET_CHEAP.point_id,
                    PITER.POST_OUTLET.point_id,
                    MOSCOW.PICKUP_OUTLET.point_id,
                    MOSCOW.PICKUP_OUTLET2.point_id,
                    MOSCOW.POST_OUTLET.point_id,
                    MOSCOW.PICKUP_OUTLET_BRANDED.point_id,
                    MOSCOW.PICKUP_POST_TERM.point_id,
                    MOSCOW.PICKUP_OUTLET_PARTNER.point_id,
                    EKB.PICKUP_OUTLET.point_id,
                    EKB.POST_OUTLET.point_id,
                    VLADIMIR.PICKUP_OUTLET.point_id,
                    VLADIMIR.POST_OUTLET.point_id,
                    KGT_REGION.PICKUP_OUTLET.point_id,
                    KGT_REGION.POST_OUTLET.point_id,
                    UNIFIED_REGION.PICKUP_OUTLET.point_id,
                    UNIFIED_REGION.POST_OUTLET.point_id,
                    KGT_REGION_PROGRESSIVE.POST_OUTLET.point_id,
                    KGT_REGION_PROGRESSIVE.PICKUP_OUTLET.point_id,
                ],
            )
        ]

        cls.index.mskus += [MarketSku(sku=index, blue_offers=[offer]) for index, offer in enumerate(OFFERS.ALL)]
        cls.index.offers += OFFERS.WHITE_OFFERS
        cls.index.offers += OFFERS.WHITE_OFFERS_WITH_DIM

    @classmethod
    def prepare_blue_delivery_modifiers(cls):
        '''
        Подготавливаем параметры для изменения цены доставки для пользователя
        '''
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=PITER.TARIFFS,
            regions=[PITER_RID],
        )
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=MOSCOW.TARIFFS,
            regions=[MOSCOW_RID],
            market_outlet_threshold=THRESHOLD_MARKET_BRANDED,
            market_post_term_threshold=THRESHOLD_MARKET_POST_TERM,
            market_partner_outlet_threshold=THRESHOLD_MARKET_PARTNER,
            delivery_service_modifiers=MOSCOW.DELIVERY_SERVICE_MODIFIER,
        )
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=VLADIMIR.TARIFFS,
            regions=[VLADIMIR_RID],
        )
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=KGT_REGION.TARIFFS,
            regions=[KGT_RID],
            large_size_weight=KGT_REGION.HEAVY_FROM_CONFIG,
        )
        cls.index.blue_delivery_modifiers.add_modifier(
            exp_name=EXP_UNIFIED_TARIFFS,
            tariffs=KGT_REGION_PROGRESSIVE.TARIFFS,
            regions=[KGT_RID_PROGRESS],
            large_size_weight=KGT_REGION.HEAVY_FROM_CONFIG,
        )
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=KGT_REGION_PROGRESSIVE.DSBS_TARIFFS,
            regions=[KGT_RID_PROGRESS],
            large_size_weight=KGT_REGION.HEAVY_FROM_CONFIG,
            is_dsbs_payment=True,
        )
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=KGT_REGION_PROGRESSIVE.DSBS_TARIFFS,
            large_size_weight=KGT_REGION.HEAVY_FROM_CONFIG,
            is_dsbs_payment=True,
        )
        cls.index.blue_delivery_modifiers.add_modifier(
            exp_name=EXP_UNIFIED_TARIFFS,
            tariffs=KGT_REGION_PROGRESSIVE_WITH_DIM.TARIFFS,
            regions=[KGT_RID_PROGRESS_DIM],
            large_size_weight=KGT_REGION.HEAVY_FROM_CONFIG,
            large_size_volume=0.5,
            large_size_max_item_dimension=1.5,
        )
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=KGT_REGION_PROGRESSIVE_WITH_DIM.DSBS_TARIFFS,
            regions=[KGT_RID_PROGRESS_DIM],
            large_size_weight=KGT_REGION.HEAVY_FROM_CONFIG,
            large_size_volume=0.5,
            large_size_max_item_dimension=1.5,
            is_dsbs_payment=True,
        )
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=EKB.TARIFFS,
        )

    @classmethod
    def prepare_blue_delivery_modifiers_experiment(cls):
        '''
        Подготавливаем параметры для изменения цены доставки для пользователя в эксперименте
        '''
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=PITER.EXP_TARIFFS, regions=[PITER_RID], exp_name=EXP_NAME
        )
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=MOSCOW.EXP_TARIFFS, regions=[MOSCOW_RID], exp_name=EXP_NAME
        )
        cls.index.blue_delivery_modifiers.set_default_modifier(tariffs=EKB.EXP_TARIFFS, exp_name=EXP_NAME)

    @classmethod
    def prepare_blue_delivery_modifiers_ya_plus_experiment(cls):
        '''
        Подготавливаем параметры для изменения цены доставки для пользователя в эксперименте
        '''
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=PITER.EXP_TARIFFS,
            regions=[PITER_RID],
            exp_name=EXP_YA_PLUS,
            ya_plus_threshold=PITER_YA_PLUS_THRESHOLD,
        )
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=MOSCOW.EXP_TARIFFS,
            regions=[MOSCOW_RID],
            exp_name=EXP_YA_PLUS,
            ya_plus_threshold=MOSCOW_YA_PLUS_THRESHOLD,
        )
        cls.index.blue_delivery_modifiers.set_default_modifier(tariffs=EKB.EXP_TARIFFS, exp_name=EXP_YA_PLUS)

    def test_choose_fast_courier(self):
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        request = (
            'place=actual_delivery&offers-list={}:1&rids={}&regset=1&no-delivery-discount=1&rgb=blue&combinator=0'
            + unified_off_flags
            + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )
        response = self.report.request_json(request.format(OFFERS.OFFER_TO_1.waremd5, PITER_RID))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "serviceId": str(DS_EXPENSIVE),
                                    "isDefault": True,
                                    "price": {
                                        "currency": "RUR",
                                        "value": str(PITER.USER_PRICE_1),
                                    },
                                    "dayFrom": 1,
                                    "dayTo": 1,
                                }
                            ]
                        }
                    }
                ]
            },
        )

    def test_user_price_in_regions(self):
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        request_actual_delivery = (
            'place=actual_delivery&offers-list={}:1&rids={}&regset=1&no-delivery-discount=1&pickup-options=grouped&pickup-options-extended-grouping=1&rgb=blue&combinator=0'
            + unified_off_flags
            + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )

        def check_actual_delivery_tariffs(offers, total_price, delivery_price, check_large_size=None):
            disable_post_as_pickup_rearr = '&rearr-factors=market_use_post_as_pickup=0'
            # Проверяем тарифную сетку в place=actual_delivery
            pickup_options = [{"price": {"currency": "RUR", "value": str(delivery_price)}}]
            post_options = [{"price": {"currency": "RUR", "value": str(delivery_price)}}]
            for region, offer_id, _ in offers:
                for disable_post_as_pickup in ['', disable_post_as_pickup_rearr]:
                    response = self.report.request_json(
                        request_actual_delivery.format(offer_id.waremd5, region)
                        + '&total-price={}'.format(total_price)
                        + disable_post_as_pickup
                    )
                    self.assertFragmentIn(
                        response,
                        {
                            "offersTotalPrice": {
                                "value": str(total_price),
                            },
                            "results": [
                                {
                                    "largeSize": NotEmpty() if check_large_size is None else check_large_size,
                                    "delivery": {
                                        "options": [
                                            {
                                                "isDefault": True,
                                                "price": {
                                                    "currency": "RUR",
                                                    "value": str(delivery_price),
                                                },
                                            }
                                        ],
                                        "pickupOptions": pickup_options
                                        if disable_post_as_pickup
                                        else pickup_options + post_options,
                                        "postOptions": post_options if disable_post_as_pickup else Absent(),
                                        "postStats": {
                                            "minPrice": {
                                                "currency": "RUR",
                                                "value": str(delivery_price),
                                            },
                                            "maxPrice": {
                                                "currency": "RUR",
                                                "value": str(delivery_price),
                                            },
                                        },
                                    },
                                }
                            ],
                        },
                    )

        # Проверяем цену доставки в Питер. Она зависит только от total-price, переданного в запросе
        # В Питере два порога: PRICE_1 и PRICE_3
        check_actual_delivery_tariffs(
            offers=piter_light_part, total_price=TO_PRICE_1, delivery_price=PITER.USER_PRICE_1, check_large_size=False
        )
        check_actual_delivery_tariffs(
            offers=piter_light_part, total_price=FROM_PRICE_1, delivery_price=PITER.USER_PRICE_2
        )
        check_actual_delivery_tariffs(
            offers=piter_light_part, total_price=TO_PRICE_2, delivery_price=PITER.USER_PRICE_2
        )
        check_actual_delivery_tariffs(
            offers=piter_light_part, total_price=FROM_PRICE_2, delivery_price=PITER.USER_PRICE_2
        )
        check_actual_delivery_tariffs(
            offers=piter_light_part, total_price=TO_PRICE_3, delivery_price=PITER.USER_PRICE_2
        )
        check_actual_delivery_tariffs(
            offers=piter_light_part, total_price=FROM_PRICE_3, delivery_price=PITER.USER_PRICE_3
        )
        # Тяжелые посылки в Питере имеют одну цену
        for price in ALL_PRICES:
            check_actual_delivery_tariffs(
                offers=piter_heavy_part, total_price=price, delivery_price=PITER.USER_PRICE_HEAVY, check_large_size=True
            )

        # Проверяем цену доставки в Питер. Она зависит только от total-price, переданного в запросе
        # В Москве один порог: PRICE_2, но для легких и тяжелых посылок
        check_actual_delivery_tariffs(
            offers=moscow_light_part, total_price=TO_PRICE_1, delivery_price=MOSCOW.USER_PRICE_1
        )
        check_actual_delivery_tariffs(
            offers=moscow_light_part, total_price=FROM_PRICE_1, delivery_price=MOSCOW.USER_PRICE_1
        )
        check_actual_delivery_tariffs(
            offers=moscow_light_part, total_price=TO_PRICE_2, delivery_price=MOSCOW.USER_PRICE_1
        )
        check_actual_delivery_tariffs(
            offers=moscow_light_part, total_price=FROM_PRICE_2, delivery_price=MOSCOW.USER_PRICE_2
        )
        check_actual_delivery_tariffs(
            offers=moscow_light_part, total_price=TO_PRICE_3, delivery_price=MOSCOW.USER_PRICE_2
        )
        check_actual_delivery_tariffs(
            offers=moscow_light_part, total_price=FROM_PRICE_3, delivery_price=MOSCOW.USER_PRICE_2
        )

        check_actual_delivery_tariffs(
            offers=moscow_heavy_part, total_price=TO_PRICE_1, delivery_price=MOSCOW.USER_PRICE_HEAVY_1
        )
        check_actual_delivery_tariffs(
            offers=moscow_heavy_part, total_price=FROM_PRICE_1, delivery_price=MOSCOW.USER_PRICE_HEAVY_1
        )
        check_actual_delivery_tariffs(
            offers=moscow_heavy_part, total_price=TO_PRICE_2, delivery_price=MOSCOW.USER_PRICE_HEAVY_1
        )
        check_actual_delivery_tariffs(
            offers=moscow_heavy_part, total_price=FROM_PRICE_2, delivery_price=MOSCOW.USER_PRICE_HEAVY_2
        )
        check_actual_delivery_tariffs(
            offers=moscow_heavy_part, total_price=TO_PRICE_3, delivery_price=MOSCOW.USER_PRICE_HEAVY_2
        )
        check_actual_delivery_tariffs(
            offers=moscow_heavy_part, total_price=FROM_PRICE_3, delivery_price=MOSCOW.USER_PRICE_HEAVY_2
        )

        # В Екб единный тариф
        for price in ALL_PRICES:
            check_actual_delivery_tariffs(offers=ekb_light_part, total_price=price, delivery_price=EKB.USER_PRICE)
            check_actual_delivery_tariffs(offers=ekb_heavy_part, total_price=price, delivery_price=EKB.USER_PRICE)

        # Проверяем настройки веса КГТ в разных регионах через тарифы
        # Тяжелый товар по-дефолту был 20кг. В регионе КГТ - это легкий офер.
        ANY_PRICE = 1234  # В регионе КГТ цена товара не влияет на цену доставки
        check_actual_delivery_tariffs(
            offers=[(KGT_RID, OFFERS.HEAVY_OFFER_TO_1, None), (KGT_RID, OFFERS.KGT_OFFER_LIGHT, None)],
            total_price=ANY_PRICE,
            delivery_price=KGT_REGION.USER_PRICE,
            check_large_size=False,
        )
        check_actual_delivery_tariffs(
            offers=[(KGT_RID, OFFERS.KGT_OFFER_HEAVY, None)],
            total_price=ANY_PRICE,
            delivery_price=KGT_REGION.USER_PRICE_HEAVY,
            check_large_size=True,
        )

        # Проверяем другие плэйса в разных цветах
        request_place = (
            'place={}&offerid={}&rids={}&regset=1&pickup-options=grouped&pickup-options-extended-grouping=1&allow-collapsing=0&combinator=0'
            + unified_off_flags
            + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )
        for place in ['prime&rgb=blue', 'offerinfo&rgb=blue', 'prime', 'offerinfo']:
            for region, offer_id, user_price in regions_and_prices:
                response = self.report.request_json(request_place.format(place, offer_id.waremd5, region))
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {
                                "entity": "offer",
                                "delivery": {
                                    "options": [
                                        {
                                            "isDefault": True,
                                            "price": {
                                                "currency": "RUR",
                                                "value": str(user_price),
                                            },
                                        }
                                    ],
                                    "pickupOptions": [
                                        {
                                            "price": {
                                                "value": str(user_price),
                                            }
                                        }
                                    ],
                                    "postStats": {
                                        "minPrice": {
                                            "value": str(user_price),
                                        },
                                        "maxPrice": {
                                            "value": str(user_price),
                                        },
                                    },
                                    "pickupPrice": {
                                        "value": str(user_price),
                                    },
                                },
                            }
                        ]
                    },
                )

    def test_check_delivery_price_by_delivery_service(self):
        # Проверяется возможность установки отдельных тарифов для служб доставки
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        request_actual_delivery = (
            'place=actual_delivery&offers-list={}:1&rids={}&regset=1&no-delivery-discount=1&pickup-options=grouped&pickup-options-extended-grouping=1&rgb=blue&combinator=0'
            + unified_off_flags
            + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )

        def check_pickup_tariffs(offers, total_price, delivery_service_prices, check_large_size=None):
            # Проверяем тарифную сетку в place=actual_delivery
            for region, offer_id, _ in offers:
                response = self.report.request_json(
                    request_actual_delivery.format(offer_id.waremd5, region) + '&total-price={}'.format(total_price)
                )
                self.assertFragmentIn(
                    response,
                    {
                        "offersTotalPrice": {
                            "value": str(total_price),
                        },
                        "results": [
                            {
                                "largeSize": NotEmpty() if check_large_size is None else check_large_size,
                                "delivery": {
                                    "pickupOptions": [
                                        {
                                            "price": {
                                                "currency": "RUR",
                                                "value": str(price),
                                            },
                                            "serviceId": service_id,
                                        }
                                        for service_id, price in delivery_service_prices
                                    ],
                                },
                            }
                        ],
                    },
                )

        check_pickup_tariffs(
            offers=moscow_light_part,
            total_price=TO_PRICE_1,
            delivery_service_prices=[
                (MOSCOW.USUAL_DS_ID, MOSCOW.USER_PRICE_1),
                (MOSCOW.SPECIAL_DS_ID, MOSCOW.SPECIAL_DS_PRICE_1),
            ],
        )
        check_pickup_tariffs(
            offers=moscow_light_part,
            total_price=FROM_PRICE_1,
            delivery_service_prices=[
                (MOSCOW.USUAL_DS_ID, MOSCOW.USER_PRICE_1),
                (MOSCOW.SPECIAL_DS_ID, MOSCOW.SPECIAL_DS_PRICE_1),
            ],
        )
        check_pickup_tariffs(
            offers=moscow_light_part,
            total_price=TO_PRICE_2,
            delivery_service_prices=[
                (MOSCOW.USUAL_DS_ID, MOSCOW.USER_PRICE_1),
                (MOSCOW.SPECIAL_DS_ID, MOSCOW.SPECIAL_DS_PRICE_1),
            ],
        )
        check_pickup_tariffs(
            offers=moscow_light_part,
            total_price=FROM_PRICE_2,
            delivery_service_prices=[
                (MOSCOW.USUAL_DS_ID, MOSCOW.USER_PRICE_2),
                (MOSCOW.SPECIAL_DS_ID, MOSCOW.SPECIAL_DS_PRICE_2),
            ],
        )
        check_pickup_tariffs(
            offers=moscow_light_part,
            total_price=TO_PRICE_3,
            delivery_service_prices=[
                (MOSCOW.USUAL_DS_ID, MOSCOW.USER_PRICE_2),
                (MOSCOW.SPECIAL_DS_ID, MOSCOW.SPECIAL_DS_PRICE_2),
            ],
        )
        check_pickup_tariffs(
            offers=moscow_light_part,
            total_price=FROM_PRICE_3,
            delivery_service_prices=[
                (MOSCOW.USUAL_DS_ID, MOSCOW.USER_PRICE_2),
                (MOSCOW.SPECIAL_DS_ID, MOSCOW.SPECIAL_DS_PRICE_2),
            ],
        )

    @classmethod
    def prepare_click_n_collect(cls):
        cls.index.shops += [
            Shop(
                fesh=CNC_SHOP,
                datafeed_id=CNC_SHOP,
                warehouse_id=CNC_SHOP,
                priority_region=213,
                name='c&c_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                fulfillment_program=False,
                ignore_stocks=True,
            )
        ]

        cls.index.outlets += [
            PITER.CNC_OUTLET,
            MOSCOW.CNC_OUTLET,
            EKB.CNC_OUTLET,
        ]

        cls.index.pickup_buckets += [
            PITER.CNC_BUCKET,
            MOSCOW.CNC_BUCKET,
            EKB.CNC_BUCKET,
        ]

        cls.dynamic.lms += [
            DynamicDeliveryServiceInfo(id=DS_SELF_DELIVERY, name='self-delivery'),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=CNC_SHOP,
                delivery_service_id=DS_SELF_DELIVERY,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=20, region_to=225)],
            ),
            DynamicWarehouseInfo(id=CNC_SHOP, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=CNC_SHOP, warehouse_to=CNC_SHOP),
            DynamicTimeIntervalsSet(
                key=0,
                intervals=[
                    TimeIntervalInfo(TimeInfo(19, 15), TimeInfo(23, 45)),
                    TimeIntervalInfo(TimeInfo(10, 0), TimeInfo(18, 30)),
                ],
            ),
        ]

    def test_click_n_collect(self):
        """
        Проверяем расчет цены доставки для Click&Collect оферов.
        Для них цена доставки задается в бакетах, т.к. это самостоятельная доставка магазина.
        """
        request_actual_delivery = 'place=actual_delivery&rgb=blue&offers-list={}:1&rids={}&regset=1&no-delivery-discount=1&pickup-options=grouped&pickup-options-extended-grouping=1'

        for rid, delivery_price in [
            (PITER_RID, PITER.CNC_DELIVERY_PRICE),
            (MOSCOW_RID, MOSCOW.CNC_DELIVERY_PRICE),
            (EKB_RID, EKB.CNC_DELIVERY_PRICE),
        ]:
            # Вес офере, его цена, цена всей корзины не влияют на цену доставки
            for offer, total_price in itertools.product([OFFERS.CNC_OFFER_TO_1, OFFERS.CNC_OFFER_FROM_3], ALL_PRICES):
                response = self.report.request_json(
                    request_actual_delivery.format(offer.waremd5, rid) + '&total-price={}'.format(total_price)
                )
                self.assertFragmentIn(
                    response,
                    {
                        "offersTotalPrice": {
                            "value": str(total_price),
                        },
                        "results": [
                            {
                                "delivery": {
                                    "options": Absent(),
                                    "pickupOptions": [
                                        {
                                            "price": {
                                                "currency": "RUR",
                                                "value": str(delivery_price),
                                            },
                                        }
                                    ],
                                    "postOptions": Absent(),
                                }
                            }
                        ],
                    },
                )

    @classmethod
    def prepare_white(cls):
        '''
        Настройка белого офера.
        '''
        cls.index.shops += [
            Shop(fesh=703, priority_region=213, cpa=Shop.CPA_REAL),
        ]

        cls.index.models += [Model(hyperid=737373)]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1703,
                fesh=703,
                carriers=[77],
                regional_options=[RegionalDelivery(rid=75, options=[DeliveryOption(price=200, day_from=3, day_to=6)])],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=1704,
                fesh=703,
                carriers=[78],
                regional_options=[
                    RegionalDelivery(rid=75, options=[DeliveryOption(price=50, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=2, day_to=3)]),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_WHITE_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=1705,
                fesh=703,
                carriers=[79],
                regional_options=[
                    RegionalDelivery(rid=75, options=[DeliveryOption(price=20, day_from=2, day_to=3)]),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=1706,
                fesh=703,
                carriers=[80],
                regional_options=[
                    RegionalDelivery(rid=KGT_RID_PROGRESS, options=[DeliveryOption(price=20, day_from=2, day_to=3)]),
                    RegionalDelivery(
                        rid=KGT_RID_PROGRESS_DIM, options=[DeliveryOption(price=20, day_from=2, day_to=3)]
                    ),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [
            Offer(
                fesh=703,
                title='only old bucket',
                delivery_buckets=[1703],
                hyperid=737373,
                cpa=Offer.CPA_REAL,
                delivery_options=[DeliveryOption(price=300, day_from=1, day_to=1)],
            ),
            # if regular program exists, market delivery white is not shown in options, only in availableServices
            # (https://st.yandex-team.ru/MARKETOUT-28741)
            Offer(
                fesh=703,
                title='old and new bucket',
                delivery_buckets=[1703, 1704],
                hyperid=737373,
                cpa=Offer.CPA_REAL,
                delivery_options=[DeliveryOption(price=300, day_from=1, day_to=1)],
            ),
            Offer(
                fesh=703,
                title='local delivery and new bucket',
                has_delivery_options=True,
                delivery_options=[DeliveryOption(price=300, day_from=1, day_to=1)],
                delivery_buckets=[1704],
                hyperid=737373,
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                fesh=703,
                title='only local delivery',
                delivery_options=[DeliveryOption(price=300, day_from=1, day_to=1)],
                hyperid=737373,
                cpa=Offer.CPA_REAL,
            ),
        ]

    def test_white(self):
        """
        Тарифы синих оферов не должны влиять на цену доставки белых оферов без флага market_dsbs_tariffs
        С флагом тарифы общие для всех офферов
        """
        for dsbs_tariffs in (0, 1):
            request = 'place=prime&fesh=703&rids=75&pp=7&rearr-factors=market_dsbs_tariffs={flag};market_unified_tariffs=0'.format(
                flag=dsbs_tariffs
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {"entity": "regionalDelimiter"},
                        {
                            "entity": "offer",
                            "titles": {"raw": "only old bucket"},
                            "delivery": {
                                "options": [
                                    {
                                        "price": {"value": str(EKB.USER_PRICE) if dsbs_tariffs else "200"},
                                        "isDefault": True,
                                        "partnerType": "regular",
                                    }
                                ]
                            },
                        },
                        {
                            "entity": "offer",
                            "titles": {"raw": "old and new bucket"},
                            "delivery": {
                                "availableServices": [{"serviceId": 77}, {"serviceId": 78}],
                                "options": [
                                    {
                                        "price": {
                                            "value": str(EKB.USER_PRICE) if dsbs_tariffs else "200",
                                        },
                                        "isDefault": True,
                                        "partnerType": "regular",
                                    }
                                ],
                            },
                        },
                        {
                            "entity": "offer",
                            "titles": {"raw": "local delivery and new bucket"},
                            "delivery": {
                                "options": [
                                    {
                                        "price": {
                                            "value": str(EKB.USER_PRICE) if dsbs_tariffs else "50",
                                        },
                                        "isDefault": True,
                                        "partnerType": "regular",
                                    }
                                ]
                            },
                        },
                    ]
                },
                allow_different_len=False,
            )

    def test_price_with_delivery(self):
        '''
        Проверяем вычисление цены офера вместе с ценой доставки
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        hide_off_flags = ';market_hide_included_in_price_filter=0'

        def check_price_with_delivery(rid, offer, delivery_price, is_delivery_included=False, is_pickup_included=False):
            flag = (
                '&included-in-price=pickup'
                if is_pickup_included
                else '&included-in-price=delivery'
                if is_delivery_included
                else ''
            )
            request_prime = (
                'place=prime&offerid={}&rids={}&regset=1&platform=desktop&pickup-options=grouped&pickup-options-extended-grouping=1&allow-collapsing=0'
                + unified_off_flags
                + hide_off_flags
                + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
            )
            response = self.report.request_json(request_prime.format(offer.waremd5, rid) + flag)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "offer",
                            "prices": {
                                "value": str(offer.price + delivery_price),
                                "isDeliveryIncluded": is_delivery_included,
                                "isPickupIncluded": is_pickup_included,
                            },
                        }
                    ]
                },
            )

        for rid, offer, delivery_price in regions_and_prices:
            check_price_with_delivery(rid, offer, 0)  # Цена доставки не включается в цену офера
            check_price_with_delivery(rid, offer, delivery_price, is_delivery_included=True)
            check_price_with_delivery(rid, offer, delivery_price, is_pickup_included=True)

    def test_price_with_delivery_price_hidden(self):
        '''
        Проверяем, что cgi-параметр included-in-price не влияет на цену при значении по умолчанию для rearr-флага market_hide_included_in_price_filter
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'

        def check_price_with_delivery(rid, offer, is_delivery_included=False, is_pickup_included=False):
            flag = (
                '&included-in-price=pickup'
                if is_pickup_included
                else '&included-in-price=delivery'
                if is_delivery_included
                else ''
            )
            request_prime = (
                'place=prime&offerid={}&rids={}&regset=1&platform=desktop&pickup-options=grouped&pickup-options-extended-grouping=1&allow-collapsing=0'
                + unified_off_flags
            )
            response = self.report.request_json(request_prime.format(offer.waremd5, rid) + flag)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "offer",
                            "prices": {
                                "value": str(offer.price),
                                "isDeliveryIncluded": False,
                                "isPickupIncluded": False,
                            },
                        }
                    ]
                },
            )

        for rid, offer, delivery_price in regions_and_prices:
            check_price_with_delivery(rid, offer)
            check_price_with_delivery(rid, offer, is_delivery_included=True)
            check_price_with_delivery(rid, offer, is_pickup_included=True)

    def test_tariff_with_experiment(self):
        '''
        Проверяем выбор другого тарифа доставки, если пользователь участвует в эксперименте
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        cases = [(PITER_RID, 88, 99), (MOSCOW_RID, 77, 66), (EKB_RID, 55, 44)]

        def get_sample_options(delivery_price):
            return [
                {
                    "price": {
                        "currency": "RUR",
                        "value": str(delivery_price),
                    },
                }
            ]

        def check_actual_delivery(rid, offer, delivery_price):
            request_actual_delivery = (
                'place=actual_delivery'
                '&rgb=blue'
                '&offers-list={}:1'
                '&rids={}'
                '&regset=1'
                '&no-delivery-discount=1'
                '&pickup-options=grouped'
                '&pickup-options-extended-grouping=1'
                '&combinator=0' + unified_off_flags + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
            )
            disable_post_as_pickup_rearr = '&rearr-factors=market_use_post_as_pickup=0'
            for disable_post_as_pickup in ['', disable_post_as_pickup_rearr]:
                response = self.report.request_json(
                    request_actual_delivery.format(offer.waremd5, rid)
                    + '&total-price={}'.format(offer.price)
                    + EXP_REARR
                    + disable_post_as_pickup
                )
                pickup_options = get_sample_options(delivery_price)
                post_options = get_sample_options(delivery_price)
                self.assertFragmentIn(
                    response,
                    {
                        "offersTotalPrice": {
                            "value": str(offer.price),
                        },
                        "results": [
                            {
                                "delivery": {
                                    "options": get_sample_options(delivery_price),
                                    "pickupOptions": pickup_options
                                    if disable_post_as_pickup
                                    else pickup_options + post_options,
                                    "postOptions": post_options if disable_post_as_pickup else Absent(),
                                }
                            }
                        ],
                    },
                )

        def check_prime(rid, offer, delivery_price):
            request_prime = (
                'place=prime&offerid={}&rids={}&regset=1&platform=desktop&pickup-options=grouped&pickup-options-extended-grouping=1&allow-collapsing=0'
                + unified_off_flags
                + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
            )
            response = self.report.request_json(request_prime.format(offer.waremd5, rid) + EXP_REARR)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "offer",
                            "delivery": {
                                "options": get_sample_options(delivery_price),
                                "pickupOptions": get_sample_options(delivery_price),
                                "postStats": {
                                    "minPrice": {
                                        "value": str(delivery_price),
                                    },
                                    "maxPrice": {
                                        "value": str(delivery_price),
                                    },
                                },
                            },
                        }
                    ]
                },
            )

        for rid, light_price, heavy_price in cases:
            check_actual_delivery(rid=rid, offer=OFFERS.OFFER_TO_1, delivery_price=light_price)
            check_actual_delivery(rid=rid, offer=OFFERS.HEAVY_OFFER_TO_1, delivery_price=heavy_price)

            check_prime(rid=rid, offer=OFFERS.OFFER_TO_1, delivery_price=light_price)
            check_prime(rid=rid, offer=OFFERS.HEAVY_OFFER_TO_1, delivery_price=heavy_price)

    def test_price_by_delivery_type(self):
        '''
        Проверяем использование разных цен для разных типов доставки
        Такие тарифы настроены только во Владимире
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'

        def get_sample_options(delivery_price):
            return [
                {
                    "price": {
                        "currency": "RUR",
                        "value": str(delivery_price),
                    },
                }
            ]

        def check_actual_delivery(offer, courier_price, pickup_price, post_price):
            request_actual_delivery = (
                'place=actual_delivery'
                '&rgb=blue'
                '&offers-list={}:1'
                '&rids={}'
                '&regset=1'
                '&no-delivery-discount=1'
                '&pickup-options=grouped'
                '&pickup-options-extended-grouping=1'
                '&combinator=0' + unified_off_flags + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
            )
            disable_post_as_pickup_rearr = '&rearr-factors=market_use_post_as_pickup=0'
            pickup_options = get_sample_options(pickup_price)
            post_options = get_sample_options(post_price)
            for disable_post_as_pickup in ['', disable_post_as_pickup_rearr]:
                response = self.report.request_json(
                    request_actual_delivery.format(offer.waremd5, VLADIMIR_RID)
                    + '&total-price={}'.format(offer.price)
                    + disable_post_as_pickup
                )
                self.assertFragmentIn(
                    response,
                    {
                        "offersTotalPrice": {
                            "value": str(offer.price),
                        },
                        "results": [
                            {
                                "delivery": {
                                    "options": get_sample_options(courier_price),
                                    "pickupOptions": pickup_options
                                    if disable_post_as_pickup
                                    else pickup_options + post_options,
                                    "postOptions": post_options if disable_post_as_pickup else Absent(),
                                }
                            }
                        ],
                    },
                )

        # Для легких посылок каждый тип доставки имеет свою цену
        check_actual_delivery(
            offer=OFFERS.OFFER_TO_1,
            courier_price=VLADIMIR.COURIER_PRICE,
            pickup_price=VLADIMIR.PICKUP_PRICE,
            post_price=VLADIMIR.POST_PRICE,
        )
        # Для тяжелых посылок цена доставки задана для курьерки и почты. ПВЗ будет иметь общую цену доставки
        check_actual_delivery(
            offer=OFFERS.HEAVY_OFFER_TO_1,
            courier_price=VLADIMIR.COURIER_PRICE,
            pickup_price=VLADIMIR.USER_PRICE,
            post_price=VLADIMIR.POST_PRICE,
        )

    def test_free_delivery_threshold(self):
        '''
        Проверяем остаток суммы до бесплатной доставки
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'

        def check_actual_delivery(rid, offer, offer_price, price_threshold, delivery_price):
            request_actual_delivery = (
                'place=actual_delivery&rgb=blue&offers-list={}:1&rids={}&regset=1&no-delivery-discount=1&pickup-options=grouped&pickup-options-extended-grouping=1&combinator=0'
                + unified_off_flags
                + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
            )
            response = self.report.request_json(request_actual_delivery.format(offer.waremd5, rid))
            expected = {
                "offersTotalPrice": {
                    "value": str(offer_price),
                },
                "results": [
                    {
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "outletIds": [PITER.OUTLET_EXPENSIVE.point_id],
                                    "price": {
                                        "currency": "RUR",
                                        "value": str(delivery_price),
                                    },
                                },
                                {
                                    "outletIds": [PITER.OUTLET_CHEAP.point_id],
                                    "price": {
                                        "currency": "RUR",
                                        "value": str(delivery_price),
                                    },
                                },
                            ]
                        }
                    }
                ],
            }
            if price_threshold:
                expected.update(
                    {
                        "freeDeliveryThreshold": {"currency": "RUR", "value": str(price_threshold)},
                        "freeDeliveryRemainder": {
                            "currency": "RUR",
                            "value": str(max(price_threshold - offer_price, 0)),
                        },
                    }
                )
            else:
                expected.update({"freeDeliveryThreshold": Absent(), "freeDeliveryRemainder": Absent()})
            self.assertFragmentIn(response, expected)

        # дешевле порога. Цена доставки в ПВЗ
        check_actual_delivery(
            rid=PITER_RID,
            offer=OFFERS.OFFER_TO_1,
            offer_price=TO_PRICE_1,
            price_threshold=FROM_THRESHOLD,
            delivery_price=PITER.USER_PRICE_1,
        )

        # дороже порога. Цена доставки обнулилась
        check_actual_delivery(
            rid=PITER_RID,
            offer=OFFERS.OFFER_THRESHOLD_1,
            offer_price=FROM_THRESHOLD_1,
            price_threshold=FROM_THRESHOLD,
            delivery_price=0,
        )

        # для КГТ порог не существует
        check_actual_delivery(
            rid=PITER_RID,
            offer=OFFERS.HEAVY_OFFER_TO_1,
            offer_price=TO_PRICE_1,
            price_threshold=None,
            delivery_price=PITER.USER_PRICE_HEAVY,
        ),

    def test_market_branded_actual_delivery(self):
        '''
        Проверяем обнуление цены доставки в брэндироваанные ПВЗ
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'

        def check_actual_delivery(rid, offer, total_price, pickup_price, branded_price, rearr=''):
            request_actual_delivery = (
                'place=actual_delivery&rgb=blue&offers-list={}:1&rids={}&regset=1&no-delivery-discount=1&pickup-options=grouped&pickup-options-extended-grouping=1&combinator=0'
                + unified_off_flags
                + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
            )
            response = self.report.request_json(
                request_actual_delivery.format(offer.waremd5, rid) + '&total-price={}'.format(total_price) + rearr
            )
            self.assertFragmentIn(
                response,
                {
                    "offersTotalPrice": {
                        "value": str(total_price),
                    },
                    "results": [
                        {
                            "delivery": {
                                "pickupOptions": [
                                    {
                                        "outletIds": [MOSCOW.PICKUP_OUTLET.point_id],
                                        "isMarketBranded": Absent(),
                                        "price": {
                                            "currency": "RUR",
                                            "value": str(pickup_price),
                                        },
                                    },
                                    {
                                        "outletIds": [MOSCOW.PICKUP_OUTLET_BRANDED.point_id],
                                        "isMarketBranded": True,
                                        "price": {
                                            "currency": "RUR",
                                            "value": str(branded_price),
                                        },
                                    },
                                ]
                            }
                        }
                    ],
                },
            )

        rearr = '&rearr-factors=market_branded_outlets_from_lms=1'

        # дешевле порога. Цена доставки в ПВЗ
        check_actual_delivery(
            rid=MOSCOW_RID,
            offer=OFFERS.OFFER_TO_1,
            total_price=TO_MARKET_BRANDED,
            pickup_price=MOSCOW.USER_PRICE_1,
            branded_price=MOSCOW.USER_PRICE_1,
        )

        # дороже порога. Цена доставки обнулилась
        check_actual_delivery(
            rid=MOSCOW_RID,
            offer=OFFERS.OFFER_TO_1,
            total_price=FROM_MARKET_BRANDED,
            pickup_price=MOSCOW.USER_PRICE_1,
            branded_price=0,
        )

        # Получение признака брэнированности из настроек оутлета
        check_actual_delivery(
            rid=MOSCOW_RID,
            offer=OFFERS.OFFER_TO_1,
            total_price=FROM_MARKET_BRANDED,
            pickup_price=MOSCOW.USER_PRICE_1,
            branded_price=0,
            rearr=rearr,
        )

    def test_market_outlet_flags(self):
        '''
        Проверяем обнуление цены доставки в маркетные постаматы и партнерские ПВЗ
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'

        def check_outlet_delivery_price(
            rid, offer, outlet, total_parcel_price, usual_delivery_price, outlet_delivery_price
        ):
            request_actual_delivery = (
                'place=actual_delivery&rgb=blue&offers-list={}:1&rids={}&regset=1&no-delivery-discount=1&pickup-options=grouped&pickup-options-extended-grouping=1&combinator=0'
                + unified_off_flags
                + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
            )
            response = self.report.request_json(
                request_actual_delivery.format(offer.waremd5, rid) + '&total-price={}'.format(total_parcel_price)
            )
            self.assertFragmentIn(
                response,
                {
                    "offersTotalPrice": {
                        "value": str(total_parcel_price),
                    },
                    "results": [
                        {
                            "delivery": {
                                "pickupOptions": [
                                    {
                                        "serviceId": MOSCOW.PICKUP_OUTLET.delivery_service_id(),
                                        "outletIds": [MOSCOW.PICKUP_OUTLET.point_id],
                                        "price": {
                                            "currency": "RUR",
                                            "value": str(usual_delivery_price),
                                        },
                                    },
                                    {
                                        "serviceId": outlet.delivery_service_id(),
                                        "outletIds": [outlet.point_id],
                                        "price": {
                                            "currency": "RUR",
                                            "value": str(outlet_delivery_price),
                                        },
                                    },
                                ]
                            }
                        }
                    ],
                },
            )

        for outlet, free_delivery_threshold in (
            (MOSCOW.PICKUP_POST_TERM, THRESHOLD_MARKET_POST_TERM),
            (MOSCOW.PICKUP_OUTLET_PARTNER, THRESHOLD_MARKET_PARTNER),
        ):
            # Если стоимость покупки ниже порога, цена доставки в проверяемый аутлет равна цене доставки в обычный ПВЗ
            check_outlet_delivery_price(
                rid=MOSCOW_RID,
                offer=OFFERS.OFFER_TO_1,
                outlet=outlet,
                total_parcel_price=free_delivery_threshold - 1,
                usual_delivery_price=MOSCOW.USER_PRICE_1,
                outlet_delivery_price=MOSCOW.USER_PRICE_1,
            )

            # Если стоимость покупки равна порогу, цена доставки в аутлет обнуляется
            check_outlet_delivery_price(
                rid=MOSCOW_RID,
                offer=OFFERS.OFFER_TO_1,
                outlet=outlet,
                total_parcel_price=free_delivery_threshold,
                usual_delivery_price=MOSCOW.USER_PRICE_1,
                outlet_delivery_price=0,
            )

    def test_blue_delivery_modifiers_ya_plus_experiment(self):
        '''
        Проверяем выбор другого тарифа доставки, если пользователь участвует в эксперименте
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        cases = [
            (
                PITER_RID,
                {
                    # Без перка ожидаются цены согласно тарифам
                    None: {
                        # (тестируемый оффер, кгт оффер) : (ожидаемая цена доставки, ожидаемая цена доставки кгт оффера)
                        (OFFERS.OFFER_TO_1, OFFERS.HEAVY_OFFER_TO_1): (88, 99),
                        (OFFERS.OFFER_FROM_1, OFFERS.HEAVY_OFFER_FROM_1): (88, 99),
                    },
                    # C перком при цене ниже трешхолда будет цена согласно тарифам
                    # при цене выше трешхолда цена обнуляется для НЕ кгт товаров
                    "yandex_plus": {
                        (OFFERS.OFFER_TO_1, OFFERS.HEAVY_OFFER_TO_1): (88, 99),
                        (OFFERS.OFFER_FROM_1, OFFERS.HEAVY_OFFER_FROM_1): ((0, 88), 99),
                    },
                },
            ),
            (
                MOSCOW_RID,
                {
                    # То же самое, только сумма трешхолда другая
                    None: {
                        (OFFERS.OFFER_TO_2, OFFERS.HEAVY_OFFER_TO_2): (77, 66),
                        (OFFERS.OFFER_FROM_2, OFFERS.HEAVY_OFFER_FROM_2): (77, 66),
                    },
                    "yandex_plus": {
                        (OFFERS.OFFER_TO_2, OFFERS.HEAVY_OFFER_TO_2): (77, 66),
                        (OFFERS.OFFER_FROM_2, OFFERS.HEAVY_OFFER_FROM_2): ((0, 77), 66),
                    },
                },
            ),
            (
                EKB_RID,
                {
                    # Трешхолда для плюсовиков в третьем тире нет поэтому нигде не ожидаем обнуления
                    None: {
                        (OFFERS.OFFER_TO_1, OFFERS.HEAVY_OFFER_TO_1): (55, 44),
                        (OFFERS.OFFER_FROM_1, OFFERS.HEAVY_OFFER_FROM_1): (55, 44),
                    },
                    "yandex_plus": {
                        (OFFERS.OFFER_TO_1, OFFERS.HEAVY_OFFER_TO_1): (55, 44),
                        (OFFERS.OFFER_FROM_1, OFFERS.HEAVY_OFFER_FROM_1): (55, 44),
                    },
                },
            ),
        ]

        def get_sample_options(delivery_price, discount_type):
            return [
                {
                    "price": {
                        "currency": "RUR",
                        "value": str(delivery_price),
                    },
                    "discount": Absent() if discount_type is None else {"discountType": discount_type},
                }
            ]

        disable_config_ya_plus_threshold = ';market_conf_ya_plus_delivery_threshold_enabled=0'

        def check_actual_delivery(rid, offer, delivery_price, perk, discount_type):
            request_actual_delivery = (
                'place=actual_delivery'
                '&rgb=blue'
                '&offers-list={}:1'
                '&rids={}'
                '&regset=1'
                '&no-delivery-discount=1'
                '&pickup-options=grouped'
                '&pickup-options-extended-grouping=1'
                '&combinator=0' + unified_off_flags + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
            )
            if perk is not None:
                request_actual_delivery += '&perks={}'.format(perk)
            rearr = EXP_YA_PLUS_REARR + disable_config_ya_plus_threshold
            disable_post_as_pickup_rearr = '&rearr-factors=market_use_post_as_pickup=0'
            pickup_options = get_sample_options(delivery_price, discount_type)
            post_options = get_sample_options(delivery_price, discount_type)
            for disable_post_as_pickup in ['', disable_post_as_pickup_rearr]:
                response = self.report.request_json(
                    request_actual_delivery.format(offer.waremd5, rid)
                    + '&total-price={}'.format(offer.price)
                    + rearr
                    + disable_post_as_pickup
                )
                self.assertFragmentIn(
                    response,
                    {
                        "offersTotalPrice": {
                            "value": str(offer.price),
                        },
                        "results": [
                            {
                                "delivery": {
                                    "options": get_sample_options(delivery_price, discount_type),
                                    "pickupOptions": pickup_options
                                    if disable_post_as_pickup
                                    else pickup_options + post_options,
                                    "postOptions": post_options if disable_post_as_pickup else Absent(),
                                }
                            }
                        ],
                    },
                )

        def check_prime(rid, offer, delivery_price, perk, discount_type):
            request_prime = (
                'place=prime&offerid={}&rids={}&rgb=blue&regset=1&platform=desktop&pickup-options=grouped&pickup-options-extended-grouping=1&allow-collapsing=0'
                + unified_off_flags
                + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
            )
            if perk is not None:
                request_prime += '&perks={}'.format(perk)
            rearr = EXP_YA_PLUS_REARR + disable_config_ya_plus_threshold
            response = self.report.request_json(request_prime.format(offer.waremd5, rid) + rearr)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "offer",
                            "delivery": {
                                "options": get_sample_options(delivery_price, discount_type),
                                "pickupOptions": get_sample_options(delivery_price, discount_type),
                                "postStats": {
                                    "minPrice": {
                                        "value": str(delivery_price),
                                    },
                                    "maxPrice": {
                                        "value": str(delivery_price),
                                    },
                                },
                            },
                        }
                    ]
                },
            )

        for rid, expected in cases:
            for perk, price_expect in expected.items():
                for offers, delivery_prices in price_expect.items():
                    light_price, heavy_price = delivery_prices
                    discount_type = None
                    light_price_before_discount = light_price

                    if isinstance(light_price, tuple):
                        light_price, light_price_before_discount = light_price
                        discount_type = "yandex_plus"

                    offer, heavy_offer = offers

                    check_actual_delivery(
                        rid=rid, offer=offer, delivery_price=light_price_before_discount, perk=perk, discount_type=None
                    )
                    check_actual_delivery(
                        rid=rid, offer=heavy_offer, delivery_price=heavy_price, perk=perk, discount_type=None
                    )

                    check_prime(
                        rid=rid, offer=offer, delivery_price=light_price, perk=perk, discount_type=discount_type
                    )
                    check_prime(rid=rid, offer=heavy_offer, delivery_price=heavy_price, perk=perk, discount_type=None)

    @classmethod
    def prepare_unified_delivery_modifiers(cls):
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=UNIFIED_REGION.TARIFFS,
            regions=[UNIFIED_RID],
            exp_name=EXP_UNIFIED_TARIFFS,
            # large_size_weight=UNIFIED_REGION.HEAVY_FROM_CONFIG
            # намеренно не указываем КГТ вес, он должен быть по дефолту для
            # unified эксперимента равен 30 кг
        )
        cls.index.blue_delivery_modifiers.set_default_modifier(tariffs=EKB.EXP_TARIFFS, exp_name=EXP_UNIFIED_TARIFFS)

    def test_unified_delivery_modifier_ya_plus(self):
        def get_price(delivery_price):
            return [
                {
                    "price": {
                        "currency": "RUR",
                        "value": str(delivery_price),
                    },
                }
            ]

        def check_prime_tariffs(rid, offer, delivery_price, perk, rearr):
            request_prime = (
                'place=prime&offerid={}&rids={}&rgb=blue&regset=1&platform=desktop&pickup-options=grouped&'
                'pickup-options-extended-grouping=1&allow-collapsing=0'
            )
            if perk is not None:
                request_prime += '&perks={}'.format(perk)
            rearr += (
                '&rearr-factors=market_conf_ya_plus_delivery_threshold_enabled=0' + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
            )
            response = self.report.request_json(request_prime.format(offer.waremd5, rid) + rearr)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "offer",
                            "delivery": {
                                "options": get_price(delivery_price),
                                "pickupOptions": get_price(delivery_price),
                                "postStats": {
                                    "minPrice": {
                                        "value": str(delivery_price),
                                    },
                                    "maxPrice": {
                                        "value": str(delivery_price),
                                    },
                                },
                            },
                        }
                    ]
                },
            )

        def check_actual_delivery_tariffs(rid, offer, delivery_price, perk, rearr):
            request_actual_delivery = (
                'place=actual_delivery&rgb=blue&offers-list={}:1&rids={}&regset=1'
                '&no-delivery-discount=1&pickup-options=grouped&pickup-options-extended-grouping=1&combinator=0'
            )
            if perk is not None:
                request_actual_delivery += '&perks={}'.format(perk)
            rearr += '&rearr-factors=market_conf_ya_plus_delivery_threshold_enabled=0'
            disable_post_as_pickup_rearr = '&rearr-factors=market_use_post_as_pickup=0'
            pickup_options = get_price(delivery_price)
            post_options = get_price(delivery_price)
            for disable_post_as_pickup in ['', disable_post_as_pickup_rearr]:
                response = self.report.request_json(
                    request_actual_delivery.format(offer.waremd5, rid)
                    + '&total-price={}'.format(offer.price)
                    + rearr
                    + disable_post_as_pickup
                    + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
                )
                self.assertFragmentIn(
                    response,
                    {
                        "offersTotalPrice": {
                            "value": str(offer.price),
                        },
                        "results": [
                            {
                                "delivery": {
                                    "options": get_price(delivery_price),
                                    "pickupOptions": pickup_options
                                    if disable_post_as_pickup
                                    else pickup_options + post_options,
                                    "postOptions": post_options if disable_post_as_pickup else Absent(),
                                }
                            }
                        ],
                    },
                )

        UNIFIED_TARIFFS_REARR = '&rearr-factors=market_unified_tariffs=1'
        for perk in ('yandex_plus', None):
            # КГТ посылка в не зависимости от того, плюсовик ли пользователь, имеет одну цену
            # + проверяем,что оффер весом 30 кг - КГТ
            check_prime_tariffs(
                rid=UNIFIED_RID,
                offer=OFFERS.UNIFIED_TEST_OFFER_HEAVY,
                delivery_price=UNIFIED_REGION.USER_PRICE_HEAVY,
                perk=perk,
                rearr=UNIFIED_TARIFFS_REARR,
            )
            check_actual_delivery_tariffs(
                rid=UNIFIED_RID,
                offer=OFFERS.UNIFIED_TEST_OFFER_HEAVY,
                delivery_price=UNIFIED_REGION.USER_PRICE_HEAVY,
                perk=perk,
                rearr=UNIFIED_TARIFFS_REARR,
            )
            # цена обычной посылки зависит от того, плюсовик ли пользователь
            # + проверяем,что оффер весом 29.9 кг - не КГТ
            check_prime_tariffs(
                rid=UNIFIED_RID,
                offer=OFFERS.UNIFIED_TEST_OFFER_LIGHT,
                delivery_price=UNIFIED_REGION.USER_PRICE_PLUS if perk else UNIFIED_REGION.USER_PRICE,
                perk=perk,
                rearr=UNIFIED_TARIFFS_REARR,
            )
            check_actual_delivery_tariffs(
                rid=UNIFIED_RID,
                offer=OFFERS.UNIFIED_TEST_OFFER_LIGHT,
                delivery_price=UNIFIED_REGION.USER_PRICE_PLUS if perk else UNIFIED_REGION.USER_PRICE,
                perk=perk,
                rearr=UNIFIED_TARIFFS_REARR,
            )

    def test_better_with_plus_prime(self):
        request_actual_delivery = (
            'place=prime&pp=18&regset=2&rids='
            + str(UNIFIED_RID)
            + '&offerid={}&show-urls=direct'
            + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )
        perk = '&perks=yandex_plus'
        offers = [
            (OFFERS.OFFER_FREE_DELIVERY_WITH_PLUS, True),
            (OFFERS.OFFER_NEVER_FREE_DELIVERY, False),
            (OFFERS.UNIFIED_TEST_OFFER_HEAVY, False),
        ]

        for offer, better_with_plus in offers:
            response = self.report.request_json(request_actual_delivery.format(offer.waremd5))
            self.assertFragmentIn(response, {"betterWithPlus": better_with_plus})

        for offer, _ in offers:
            response = self.report.request_json(request_actual_delivery.format(offer.waremd5) + perk)
            self.assertFragmentIn(response, {"betterWithPlus": False})

    def test_is_better_with_plus_and_thresholds(self):
        def get_price(price):
            return {"currency": "RUR", "value": str(price)}

        def check_actual_delivery_tariffs(
            rid,
            offer,
            total_price,
            perk,
            rearr,
            threshold=None,
            is_better_with_plus=None,
            check_free_threshold_absense=False,
        ):
            request_actual_delivery = (
                'place=actual_delivery&rgb=blue&offers-list={}:1&rids={}&regset=1'
                '&no-delivery-discount=1&pickup-options=grouped&pickup-options-extended-grouping=1&combinator=0'
            )
            if perk is not None:
                request_actual_delivery += '&perks={}'.format(perk)
            rearr += '&rearr-factors=market_conf_ya_plus_delivery_threshold_enabled=0'
            response = self.report.request_json(
                request_actual_delivery.format(offer.waremd5, rid) + '&total-price={}'.format(total_price) + rearr
            )
            self.assertFragmentIn(
                response,
                {
                    "cheaperDeliveryThreshold": get_price(threshold) if threshold else Absent(),
                    "cheaperDeliveryRemainder": get_price(threshold - total_price if threshold > total_price else 0)
                    if threshold
                    else Absent(),
                    "betterWithPlus": is_better_with_plus if is_better_with_plus is not None else Absent(),
                },
            )

            if check_free_threshold_absense:
                self.assertFragmentNotIn(response, {"freeDeliveryThreshold", "freeDeliveryRemainder"})

        # КГТ при любой цене нет тарифов дешевле
        UNIFIED_TARIFFS_REARR = '&rearr-factors=market_unified_tariffs=1'
        for total_price in (10, 1010, 2010):
            for perk in ('yandex_plus', None):
                check_actual_delivery_tariffs(
                    rid=UNIFIED_RID,
                    offer=OFFERS.UNIFIED_TEST_OFFER_HEAVY,
                    total_price=total_price,
                    perk=perk,
                    rearr=UNIFIED_TARIFFS_REARR,
                )

        # Проверяем, что при наличии перка плюса betterWithPlus всегда false
        for total_price in (10, 1010, 2010):
            check_actual_delivery_tariffs(
                rid=UNIFIED_RID,
                offer=OFFERS.UNIFIED_TEST_OFFER_LIGHT,
                total_price=total_price,
                perk='yandex_plus',
                rearr=UNIFIED_TARIFFS_REARR,
                threshold=None,
                is_better_with_plus=False,
            )

        # Дешевая посылка в 10 рублей, доставка с плюсом лучше, тариф дешевле начинается с 1000 рублей
        check_actual_delivery_tariffs(
            rid=UNIFIED_RID,
            offer=OFFERS.UNIFIED_TEST_OFFER_LIGHT,
            total_price=10,
            perk=None,
            rearr=UNIFIED_TARIFFS_REARR,
            threshold=1000,
            is_better_with_plus=True,
        )
        # Посылка стоимостью 1010, следующий тариф дешевле - с 2000 рублей
        check_actual_delivery_tariffs(
            rid=UNIFIED_RID,
            offer=OFFERS.UNIFIED_TEST_OFFER_LIGHT,
            total_price=1010,
            perk=None,
            rearr=UNIFIED_TARIFFS_REARR,
            threshold=2000,
            is_better_with_plus=True,
        )
        # Передаем перк плюса, тарифов дешевле нет, бесплатная доставка от 1000
        check_actual_delivery_tariffs(
            rid=UNIFIED_RID,
            offer=OFFERS.UNIFIED_TEST_OFFER_LIGHT,
            total_price=10,
            perk='yandex_plus',
            rearr=UNIFIED_TARIFFS_REARR,
        )

        # Если неплюсовик перескочил трешхолд, нам нужно вернуть трешхолд ПЛЮСА! в cheaperDeliveryThreshold,
        # чтобы отрисовать "бесплататная доставка с плюса с <трешхолд с плюсом>"
        #
        # 2500 (цена) > 2000 (трешхолд для 'неплюса')
        check_actual_delivery_tariffs(
            rid=UNIFIED_RID,
            offer=OFFERS.UNIFIED_TEST_OFFER_VERY_LIGHT,
            threshold=1000,
            total_price=999,
            perk=None,
            is_better_with_plus=True,
            rearr=UNIFIED_TARIFFS_REARR,
        )

        # Проверяем, что трешхолды не приходят для click&collect
        check_actual_delivery_tariffs(
            rid=UNIFIED_RID,
            offer=OFFERS.CNC_OFFER_TO_1,
            total_price=100,
            perk=None,
            rearr=UNIFIED_TARIFFS_REARR,
            threshold=None,
            is_better_with_plus=False,
            check_free_threshold_absense=True,
        )

    def test_new_kgt_progressive_logic(self):
        '''
        Проверяем новую логику, которая должна проверять допустимо ли применение тарифа по весу
        (т.е. не меньше ли вес оффера/корзины трешхолда, заданного в тарифе)
        '''
        NEW_KGT_REARR_TEMPLATE = "&rearr-factors=new_kgt_calculation={}"

        def get_price(price):
            return {"currency": "RUR", "value": str(price)}

        def get_sample_options(delivery_price, sullplier_price):
            return [{"price": get_price(delivery_price), "supplierPrice": get_price(sullplier_price)}]

        def check_actual_delivery_tariffs(
            rid,
            offer,
            total_price,
            perk,
            rearr,
            delivery_price,
            supplier_price,
            threshold=None,
            is_better_with_plus=None,
            check_free_threshold_absense=False,
            offers_count=1,
        ):
            request_actual_delivery = (
                'place=actual_delivery&rgb=blue&offers-list={offerid}:{offerscount}&rids={rid}&regset=1'
                '&no-delivery-discount=1&pickup-options=grouped&pickup-options-extended-grouping=1&combinator=0'
            )
            if perk is not None:
                request_actual_delivery += '&perks={}'.format(perk)
            rearr += '&rearr-factors=market_conf_ya_plus_delivery_threshold_enabled=0;market_unified_tariffs=1;market_dsbs_tariffs=1'
            response = self.report.request_json(
                request_actual_delivery.format(offerid=offer.waremd5, offerscount=offers_count, rid=rid)
                + '&total-price={}'.format(total_price)
                + rearr
            )
            self.assertFragmentIn(
                response,
                {
                    "options": get_sample_options(delivery_price, supplier_price),
                    "cheaperDeliveryThreshold": get_price(threshold) if threshold else Absent(),
                    "cheaperDeliveryRemainder": get_price(threshold - total_price if threshold > total_price else 0)
                    if threshold
                    else Absent(),
                    "betterWithPlus": is_better_with_plus if is_better_with_plus is not None else Absent(),
                },
            )

            if check_free_threshold_absense:
                self.assertFragmentNotIn(response, {"freeDeliveryThreshold", "freeDeliveryRemainder"})

        # Флаг выключен - все тарифы с трешхолдом по весу должны быть отброшены
        # Берём или не кгт тариф, или кгт тариф без трешхолда по весу
        for offer, tariff in zip(OFFERS.WHITE_OFFERS, KGT_REGION_PROGRESSIVE.TARIFFS):
            expected_user_price = (
                KGT_REGION_PROGRESSIVE.NON_WEIGHT_KGT_TARIFFS[0].user_price
                if tariff.large_size
                else KGT_REGION_PROGRESSIVE.NON_KGT_TARIFFS[0].user_price
            )
            expected_dsbs_payment = (
                KGT_REGION_PROGRESSIVE.DSBS_TARIFFS[-1].dsbs_payment
                if tariff.large_size
                else KGT_REGION_PROGRESSIVE.DSBS_TARIFFS[0].dsbs_payment
            )
            check_actual_delivery_tariffs(
                rid=KGT_RID_PROGRESS,
                offer=offer,
                total_price=1,
                perk=None,
                rearr=NEW_KGT_REARR_TEMPLATE.format(0),
                delivery_price=expected_user_price,
                supplier_price=expected_dsbs_payment,
                threshold=None,
                is_better_with_plus=False,
            )

        # Флаг включен - выбираем тариф с трешхолдом
        for offer, tariff, dsbs_tariff in zip(
            OFFERS.WHITE_OFFERS, KGT_REGION_PROGRESSIVE.TARIFFS, KGT_REGION_PROGRESSIVE.DSBS_TARIFFS
        ):
            check_actual_delivery_tariffs(
                rid=KGT_RID_PROGRESS,
                offer=offer,
                total_price=1,
                perk=None,
                rearr=NEW_KGT_REARR_TEMPLATE.format(1),
                delivery_price=tariff.user_price,
                supplier_price=dsbs_tariff.dsbs_payment,
                threshold=None,
                is_better_with_plus=False,
            )

        # Проверяем, что несколько офферов, берут тариф соответствующий
        # сумме их весов
        offer = OFFERS.WHITE_OFFERS[2]
        tariff = KGT_REGION_PROGRESSIVE.WEIGHT_TARIFFS[0]
        dsbs_tariff = KGT_REGION_PROGRESSIVE.DSBS_TARIFFS[1]
        check_actual_delivery_tariffs(
            rid=KGT_RID_PROGRESS,
            offer=offer,
            total_price=1,
            perk=None,
            rearr=NEW_KGT_REARR_TEMPLATE.format(1),
            delivery_price=tariff.user_price,
            supplier_price=dsbs_tariff.dsbs_payment,
            threshold=None,
            is_better_with_plus=False,
            offers_count=2,
        )

    def test_new_kgt_progressive_logic_dimensional(self):
        '''
        Проверяем новую логику, которая должна проверять допустимо ли применение тарифа по объёму/максимальной размерности
        (т.е. не меньше ли объёму/максимальной размерности оффера/корзины трешхолда, заданного в тарифе)
        '''
        DIM_USAGE_REARR_TEMPLATE = (
            "&rearr-factors=kgt_volume_calculation=1;market_use_dim_thresholds_in_delivery_price={}"
        )

        def get_price(price):
            return {"currency": "RUR", "value": str(price)}

        def get_sample_options(delivery_price, sullplier_price):
            return [{"price": get_price(delivery_price), "supplierPrice": get_price(sullplier_price)}]

        def check_actual_delivery_tariffs(
            rid,
            offer,
            total_price,
            perk,
            rearr,
            delivery_price,
            supplier_price,
            threshold=None,
            is_better_with_plus=None,
            check_free_threshold_absense=False,
            offers_count=1,
        ):
            request_actual_delivery = (
                'place=actual_delivery&rgb=blue&offers-list={offerid}:{offerscount}&rids={rid}&regset=1'
                '&no-delivery-discount=1&pickup-options=grouped&pickup-options-extended-grouping=1&combinator=0'
            )
            if perk is not None:
                request_actual_delivery += '&perks={}'.format(perk)
            rearr += '&rearr-factors=market_conf_ya_plus_delivery_threshold_enabled=0;market_unified_tariffs=1;market_dsbs_tariffs=1'
            response = self.report.request_json(
                request_actual_delivery.format(offerid=offer.waremd5, offerscount=offers_count, rid=rid)
                + '&total-price={}'.format(total_price)
                + rearr
            )
            self.assertFragmentIn(
                response,
                {
                    "options": get_sample_options(delivery_price, supplier_price),
                    "cheaperDeliveryThreshold": get_price(threshold) if threshold else Absent(),
                    "cheaperDeliveryRemainder": get_price(threshold - total_price if threshold > total_price else 0)
                    if threshold
                    else Absent(),
                    "betterWithPlus": is_better_with_plus if is_better_with_plus is not None else Absent(),
                },
            )

            if check_free_threshold_absense:
                self.assertFragmentNotIn(response, {"freeDeliveryThreshold", "freeDeliveryRemainder"})

        # Флаг выключен - все тарифы с трешхолдом по весу должны быть отброшены
        # Берём или не кгт тариф, или кгт тариф без трешхолда по весу
        for offer_index, tariff in enumerate(KGT_REGION_PROGRESSIVE_WITH_DIM.DIM_TARIFFS[:-1]):
            expected_user_price = (
                KGT_REGION_PROGRESSIVE_WITH_DIM.NON_DIM_KGT_TARIFFS[0].user_price
                if tariff.large_size
                else KGT_REGION_PROGRESSIVE_WITH_DIM.NON_KGT_TARIFFS[0].user_price
            )
            expected_dsbs_payment = (
                KGT_REGION_PROGRESSIVE_WITH_DIM.DSBS_TARIFFS[-1].dsbs_payment
                if tariff.large_size
                else KGT_REGION_PROGRESSIVE_WITH_DIM.DSBS_TARIFFS[0].dsbs_payment
            )
            # Проверяем что по пробитию объёма выбирается первый доступный тариф
            check_actual_delivery_tariffs(
                rid=KGT_RID_PROGRESS_DIM,
                offer=OFFERS.WHITE_OFFERS_WITH_DIM[offer_index * 2],
                total_price=1,
                perk=None,
                rearr=DIM_USAGE_REARR_TEMPLATE.format(0),
                delivery_price=expected_user_price,
                supplier_price=expected_dsbs_payment,
                threshold=None,
                is_better_with_plus=False,
            )
            # Проверяем что по пробитию максимального измерения выбирается первый доступный тариф
            if tariff.max_item_dim_threshold is None:
                continue
            check_actual_delivery_tariffs(
                rid=KGT_RID_PROGRESS_DIM,
                offer=OFFERS.WHITE_OFFERS_WITH_DIM[offer_index * 2 + 1],
                total_price=1,
                perk=None,
                rearr=DIM_USAGE_REARR_TEMPLATE.format(0),
                delivery_price=expected_user_price,
                supplier_price=expected_dsbs_payment,
                threshold=None,
                is_better_with_plus=False,
            )

        # Флаг включен - выбираем тариф с трешхолдом
        for offer_index, tariff in enumerate(KGT_REGION_PROGRESSIVE_WITH_DIM.DIM_TARIFFS[:-1]):

            dsbs_tariff = KGT_REGION_PROGRESSIVE_WITH_DIM.DSBS_TARIFFS[offer_index + 1]
            check_actual_delivery_tariffs(
                rid=KGT_RID_PROGRESS_DIM,
                offer=OFFERS.WHITE_OFFERS_WITH_DIM[offer_index * 2],
                total_price=1,
                perk=None,
                rearr=DIM_USAGE_REARR_TEMPLATE.format(1),
                delivery_price=tariff.user_price,
                supplier_price=dsbs_tariff.dsbs_payment,
                threshold=None,
                is_better_with_plus=False,
            )
            if tariff.max_item_dim_threshold is None:
                continue
            check_actual_delivery_tariffs(
                rid=KGT_RID_PROGRESS_DIM,
                offer=OFFERS.WHITE_OFFERS_WITH_DIM[offer_index * 2 + 1],
                total_price=1,
                perk=None,
                rearr=DIM_USAGE_REARR_TEMPLATE.format(1),
                delivery_price=tariff.user_price,
                supplier_price=dsbs_tariff.dsbs_payment,
                threshold=None,
                is_better_with_plus=False,
            )

        # Проверяем, что несколько офферов, берут тариф соответствующий
        # сумме их объёмов
        offer = OFFERS.WHITE_OFFERS_WITH_DIM[-1]
        tariff = KGT_REGION_PROGRESSIVE_WITH_DIM.DIM_TARIFFS[-3]
        dsbs_tariff = KGT_REGION_PROGRESSIVE_WITH_DIM.DSBS_TARIFFS[-4]
        check_actual_delivery_tariffs(
            rid=KGT_RID_PROGRESS_DIM,
            offer=offer,
            total_price=1,
            perk=None,
            rearr=DIM_USAGE_REARR_TEMPLATE.format(1),
            delivery_price=tariff.user_price,
            supplier_price=dsbs_tariff.dsbs_payment,
            threshold=None,
            is_better_with_plus=False,
            offers_count=3,
        )


if __name__ == '__main__':
    main()
