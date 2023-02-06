#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.matcher import Absent, NotEmpty
from core.report import REQUEST_TIMESTAMP
from core.testcase import main, TestCase
from core.types import (
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryCalcFeedInfo,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    Offer,
    Outlet,
    OutletDeliveryOption,
    Payment,
    PickupBucket,
    PickupOption,
    Promo,
    PromoType,
    RegionalDelivery,
    Tax,
    TimeInfo,
    TimeIntervalsForDaysInfo,
    TimeIntervalsForRegion,
    ShopPaymentMethods,
    PaymentRegionalGroup,
    HyperCategory,
    HyperCategoryType,
)
from core.types.offer_promo import PromoBlueCashback, PromoRestrictions, OffersMatchingRules, PartnerInfo, Threshold
from core.types.shop import Shop
from core.types.sku import BlueOffer, MarketSku
from core.types.autogen import b64url_md5

from market.proto.common.promo_pb2 import ESourceType, EPaymentMethods
from market.proto.feedparser.Promo_pb2 import UserDeviceType

from datetime import datetime, timedelta
from itertools import count
from math import ceil
from copy import deepcopy
from itertools import product


now = datetime.fromtimestamp(REQUEST_TIMESTAMP)
delta_big = timedelta(days=10)
delta_small = timedelta(days=1)

nummer = count()

cashback_without_yandex_plus = [
    'yandex_cashback',
]
cashback_enable_perks = [
    'yandex_plus',
    'yandex_cashback',
]
cashback_disable_perks = []
cashback_extra_enable_perks = [
    'yandex_plus',
    'yandex_cashback',
    'yandex_extra_cashback',
]
cashback_extra_employee_enable_perks = [
    'yandex_cashback',
    'yandex_employee_extra_cashback',
]
cashback_extra_disable_perks = [
    'yandex_cashback',
]
cashback_all_perks = [
    'yandex_plus',
    'yandex_cashback',
    'yandex_extra_cashback',
    'yandex_employee_extra_cashback',
]

BLUE_PLACES = (
    'prime',
    'sku_offers',
)
WHITE_PLACES = (
    'prime',
    'multi_category',
)
WHITE_OFFERS_PLACE = (
    'productoffers',
    'offerinfo',
)

BLUE_COLOR = 'blue'
GREEN_COLOR = 'green'
GREEN_WITH_BLUE_COLOR = 'green_with_blue'

DEFAULT_FEED_ID = 777
DEFAULT_SHOP_ID = 7770

DSBS_SKU = 10010
DSBS_HID = 10011001

DSBS_CEHAC_SKU = 10011
DSBS_CEHAC_HID = 10011011

DSBS_DIY_SKU = 10012
DSBS_DIY_HID = 10011021

DSBS_DEFAULT_SKU = 10013
DSBS_DEFAULT_HID = 10011031

DSBS_WITH_TWO_CASHBACKS_SKU = 10014
DSBS_WITH_TWO_CASHBACKS_HID = 10011041

DSBS_WITHOUT_PREPAYMENT_SKU = 20020
DSBS_WITHOUT_PREPAYMENT_HID = 20022002

DSBS_WITH_PREPAYMENT_SKU = 30030
DSBS_WITH_PREPAYMENT_HID = 30033003

DSBS_FOR_DETAILS_TEST_SKU = 40040
DSBS_FOR_DETAILS_TEST_HID = 40044004

DSBS_FOR_DETAILS_SORTING_TEST_SKU = 50050
DSBS_FOR_DETAILS_SORTING_TEST_HID = 50055005

USUAL_FEED_ID = 888
USUAL_SHOP_ID = 8880

FEED_ID_WITH_SHOP_CARRIER = 777
SHOP_ID_WITH_SHOP_CARRIER = 7770

FEED_ID_DSBS = 11111
SHOP_ID_DSBS = 111110

FEED_ID_DSBS_WITHOUT_PREPAYMENT = 22222
SHOP_ID_DSBS_WITHOUT_PREPAYMENT = 222220

FEED_ID_DSBS_WITH_PREPAYMENT = 33333
SHOP_ID_DSBS_WITH_PREPAYMENT = 333330

FEED_ID_CNC = 11112
SHOP_ID_CNC = 111120
WARE_ID_CNC = 123321
CNC_OUTLET = 3030

ALCO_HID = 1615546
MEDICNE_HID = 15756525

CNC_ALCO_MSKU = 10020
CNC_MEDICINE_MSKU = 10022


# магазины
virtual_shop = Shop(
    fesh=1,
    datafeed_id=1,
    priority_region=213,
    name='ВиртуальныйМагазин',
    fulfillment_virtual=True,
    supplier_type=Shop.FIRST_PARTY,
    virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
)

shop_1 = Shop(
    fesh=DEFAULT_SHOP_ID,
    datafeed_id=DEFAULT_FEED_ID,
    priority_region=213,
    supplier_type=Shop.THIRD_PARTY,
    blue=Shop.BLUE_REAL,
    warehouse_id=145,
    fulfillment_virtual_shop_id=virtual_shop.fesh,
)

crossdock_shop = Shop(
    fesh=8,
    datafeed_id=8,
    priority_region=213,
    name='blue_shop_crossdock',
    currency=Currency.RUR,
    tax_system=Tax.OSN,
    supplier_type=Shop.THIRD_PARTY,
    blue=Shop.BLUE_REAL,
    warehouse_id=1213,
    fulfillment_program=True,
    direct_shipping=False,
)

usual_3p_shop = Shop(
    fesh=USUAL_SHOP_ID,
    datafeed_id=USUAL_FEED_ID,
    priority_region=213,
    fulfillment_program=False,
    supplier_type=Shop.THIRD_PARTY,
    blue=Shop.BLUE_REAL,
    warehouse_id=123,
    name='usual_shop',
)

shop_with_shop_carrier = Shop(
    fesh=FEED_ID_WITH_SHOP_CARRIER,
    datafeed_id=SHOP_ID_WITH_SHOP_CARRIER,
    priority_region=213,
    fulfillment_program=False,
    supplier_type=Shop.THIRD_PARTY,
    blue=Shop.BLUE_REAL,
    warehouse_id=124,
    name='shop_with_shop_carrier',
)

crossdock_shop_with_direct = Shop(
    fesh=8,
    datafeed_id=99,
    priority_region=213,
    name='blue_shop_crossdock_with_shop',
    currency=Currency.RUR,
    tax_system=Tax.OSN,
    supplier_type=Shop.THIRD_PARTY,
    blue=Shop.BLUE_REAL,
    warehouse_id=1214,
    fulfillment_program=True,
    direct_shipping=True,
)

dsbs_shop = Shop(
    fesh=SHOP_ID_DSBS,
    datafeed_id=FEED_ID_DSBS,
    priority_region=213,
    name='DSBS shop',
    cpa=Shop.CPA_REAL,
    warehouse_id=35984,
)

dsbs_shop_without_prepayment = Shop(
    fesh=SHOP_ID_DSBS_WITHOUT_PREPAYMENT,
    datafeed_id=FEED_ID_DSBS_WITHOUT_PREPAYMENT,
    priority_region=213,
    name='DSBS shop without prepayment',
    cpa=Shop.CPA_REAL,
    warehouse_id=35989,
)

dsbs_shop_with_prepayment = Shop(
    fesh=SHOP_ID_DSBS_WITH_PREPAYMENT,
    datafeed_id=FEED_ID_DSBS_WITH_PREPAYMENT,
    priority_region=213,
    name='DSBS shop with prepayment',
    cpa=Shop.CPA_REAL,
    warehouse_id=35567,
)

# click-n-collect shop
cnc_shop = Shop(
    fesh=SHOP_ID_CNC,
    datafeed_id=FEED_ID_CNC,
    warehouse_id=WARE_ID_CNC,
    fulfillment_program=False,
    ignore_stocks=True,
    name="Click & collect",
    priority_region=213,
    tax_system=Tax.OSN,
    supplier_type=Shop.THIRD_PARTY,
    blue=Shop.BLUE_REAL,
    client_id=4,
    delivery_service_outlets=[CNC_OUTLET],
)


# офферы
def create_blue_offer(
    price,
    fesh=DEFAULT_SHOP_ID,
    feed=DEFAULT_FEED_ID,
    delivery_buckets=None,
    pickup_buckets=None,
    is_fulfillment=True,
    name=None,
    post_term_delivery=None,
    is_baa=False,
):
    def build_ware_md5(id):
        return id.ljust(21, "_") + "w"

    if name is not None:
        offerid = name
        waremd5 = build_ware_md5(name)
    else:
        num = next(nummer)
        offerid = 'offerid_{}'.format(num)
        waremd5 = b64url_md5(num)

    return BlueOffer(
        waremd5=waremd5,
        price=price,
        fesh=fesh,
        feedid=feed,
        offerid=offerid,
        delivery_buckets=delivery_buckets,
        pickup_buckets=pickup_buckets,
        is_fulfillment=is_fulfillment,
        post_term_delivery=post_term_delivery,
        is_baa=is_baa,
    )


def create_white_offer(price, fesh, hyperid, sku, delivery_buckets=None, is_dsbs=False, promos=[]):
    num = next(nummer)
    return Offer(
        fesh=fesh,
        waremd5=b64url_md5(num),
        hyperid=hyperid,
        sku=sku,
        price=price,
        promo=promos,
        blue_promo_key=[promo.shop_promo_id for promo in promos],
        cpa=Offer.CPA_REAL if is_dsbs else Offer.CPA_NO,
        delivery_buckets=delivery_buckets,
    )


blue_offer_cb_no_predicates = create_blue_offer(price=1000, name="no_predicates")
blue_offer_1 = create_blue_offer(price=1001, name="offer_1")
blue_offer_2 = create_blue_offer(price=2001, name="offer_2")
blue_offer_3 = create_blue_offer(price=3001, name="offer_3")
blue_offer_4 = create_blue_offer(price=4001, name="offer_4")
blue_offer_4_employees_cashback = create_blue_offer(price=4001, name="4_emp_cashback")
blue_offer_6 = create_blue_offer(price=7001, name="offer_6")
blue_offer_7 = create_blue_offer(price=10001, name="offer_7")
blue_offer_8 = create_blue_offer(price=100, name="offer_8")
blue_offer_crossdock = create_blue_offer(
    price=6001, feed=crossdock_shop.datafeed_id, is_fulfillment=False, name="crossdock_offer"
)
blue_offer_with_extra_cb = create_blue_offer(
    price=1000, feed=usual_3p_shop.datafeed_id, delivery_buckets=[777], is_fulfillment=False, name="extra_cb"
)
blue_offer_with_extra_cb_shop_carrier = create_blue_offer(
    price=1000,
    feed=shop_with_shop_carrier.datafeed_id,
    delivery_buckets=[888],
    is_fulfillment=False,
    name="extra_cb_sh_c",
)
blue_offer_with_extra_and_normal_cb = create_blue_offer(
    price=1000, feed=usual_3p_shop.datafeed_id, delivery_buckets=[777], is_fulfillment=False, name="extra_n_norm"
)
blue_offer_with_extra_and_normal_cb_shop_carrier = create_blue_offer(
    price=1000,
    feed=shop_with_shop_carrier.datafeed_id,
    delivery_buckets=[888],
    is_fulfillment=False,
    name="extra_n_norm_sh_c",
)
blue_offer_with_max_offer_cashback = create_blue_offer(
    price=1000, feed=usual_3p_shop.datafeed_id, is_fulfillment=False, name="max_offer_cb"
)
blue_offer_with_max_offer_cashback_cheap = create_blue_offer(
    price=100, feed=usual_3p_shop.datafeed_id, is_fulfillment=False, name="max_offer_cb_cheap"
)
blue_offer_with_extra_3p_cb = create_blue_offer(
    name="extra_3p_cb", price=1000, feed=usual_3p_shop.datafeed_id, delivery_buckets=[777], is_fulfillment=False
)
blue_ff_offer_with_extra_3p_cb_and_plain = create_blue_offer(name="extra_3p_cb_n_p", price=1000, delivery_buckets=[999])
blue_offer_with_extra_cb_shop_carrier_3p_cb = create_blue_offer(
    name="extra_3p_cb_sc",
    price=1000,
    feed=shop_with_shop_carrier.datafeed_id,
    delivery_buckets=[888],
    is_fulfillment=False,
)

# Повышенный кешбэк только для офферов с доставкой магазина и обычный кешбэк для офферов только с маркетной доставкой
# по умолчанию повышенный кешбэк привязывается только к маркетной доставке, а обычный кешбэк не смотрит на доставку
blue_offer_extra_cb_market_dt = create_blue_offer(
    name="extra_mdt", price=1000, feed=usual_3p_shop.datafeed_id, delivery_buckets=[777], is_fulfillment=False
)
blue_offer_cb_shop_dt = create_blue_offer(
    name="extra_sdt", price=1000, feed=shop_with_shop_carrier.datafeed_id, delivery_buckets=[888], is_fulfillment=False
)

blue_offer_extra_cb_market_all_dt = create_blue_offer(
    name="extra_all_dt", price=1000, feed=usual_3p_shop.datafeed_id, delivery_buckets=[777], is_fulfillment=False
)
blue_offer_cb_shop_all_dt = create_blue_offer(
    name="extra_s_all_dt",
    price=1000,
    feed=shop_with_shop_carrier.datafeed_id,
    delivery_buckets=[888],
    is_fulfillment=False,
)

blue_offer_cb_shop_all = create_blue_offer(
    name="extra_s_all",
    price=1000,
    feed=crossdock_shop_with_direct.datafeed_id,
    delivery_buckets=[777, 888],
    is_fulfillment=True,
)
blue_offer_cb_shop_dt_with_all = create_blue_offer(
    name="extra_s_w_all",
    price=1000,
    feed=crossdock_shop_with_direct.datafeed_id,
    delivery_buckets=[777, 888],
    is_fulfillment=True,
)
blue_offer_cb_market_dt_with_all = create_blue_offer(
    name="extra_m_w_all",
    price=1000,
    feed=crossdock_shop_with_direct.datafeed_id,
    delivery_buckets=[777, 888],
    is_fulfillment=True,
)


# кешбэк для dsbs
def create_cashback_for_dsbs(
    name,
    share,
    tags=[],
    mskus=[],
    bucket_name='default',
    details_group_key='',
    details_group_name='',
    source_type=ESourceType.UNKNOWN,
):
    return Promo(
        promo_type=PromoType.BLUE_CASHBACK,
        key=b64url_md5(next(nummer)),
        shop_promo_id='cashback_for_dsbs_' + name,
        blue_cashback=PromoBlueCashback(
            share=share,
            version=6,
            priority=2,
            details_group_key=details_group_key,
            details_group_name=details_group_name,
        ),
        tags=tags,
        promo_bucket_name=bucket_name,
        restrictions=PromoRestrictions(
            predicates=[
                {
                    'delivery_partner_types': [
                        1,
                    ],
                }
            ]
        ),
        offers_matching_rules=[
            OffersMatchingRules(mskus=mskus),
        ],
        source_type=source_type,
    )


blue_cashback_for_dsbs_with_details_1 = create_cashback_for_dsbs(
    name='unusual',
    share=0.19,
    mskus=[DSBS_FOR_DETAILS_SORTING_TEST_SKU],
    details_group_key='A_some_group',
    details_group_name='Нестандартный кешбек',
    bucket_name='default',
)
blue_cashback_for_dsbs_with_details_2 = create_cashback_for_dsbs(
    name='very_unusual',
    share=0.29,
    mskus=[DSBS_FOR_DETAILS_SORTING_TEST_SKU],
    details_group_key='B_some_group',
    details_group_name='Очень не стандартный кешбек',
    bucket_name='extra',
)
blue_cashback_for_dsbs = create_cashback_for_dsbs(
    name='usual',
    share=0.09,
    mskus=[CNC_MEDICINE_MSKU, CNC_ALCO_MSKU, DSBS_SKU, DSBS_FOR_DETAILS_TEST_SKU, DSBS_FOR_DETAILS_SORTING_TEST_SKU],
    details_group_key='default',
    details_group_name='Стандартный кешбек',
    bucket_name='blabla_bucket_id',
)
small_cashback_for_dsbs_cehac = create_cashback_for_dsbs(
    name='small_cehac',
    share=0.01,
    tags=['extra-cashback'],
    mskus=[DSBS_CEHAC_SKU],
    bucket_name='default',
)
big_cashback_for_dsbs_cehac = create_cashback_for_dsbs(
    name='big_cehac',
    share=0.03,
    tags=[],
    mskus=[DSBS_CEHAC_SKU],
    bucket_name='extra',
)
small_cashback_for_dsbs_diy = create_cashback_for_dsbs(
    name='small_diy',
    share=0.003,
    tags=['extra-cashback'],
    mskus=[DSBS_DIY_SKU],
    bucket_name='default',
)
big_cashback_for_dsbs_diy = create_cashback_for_dsbs(
    name='big_diy',
    share=0.06,
    tags=[],
    mskus=[DSBS_DIY_SKU],
    bucket_name='extra',
)
small_cashback_for_dsbs_default = create_cashback_for_dsbs(
    name='small_default',
    share=0.03,
    tags=['extra-cashback'],
    mskus=[DSBS_DEFAULT_SKU, DSBS_FOR_DETAILS_TEST_SKU],
    bucket_name='default',
    source_type=ESourceType.PARTNER_SOURCE,
)
big_cashback_for_dsbs_default = create_cashback_for_dsbs(
    name='big_default',
    share=0.3,
    tags=[],
    mskus=[DSBS_DEFAULT_SKU, DSBS_FOR_DETAILS_TEST_SKU],
    bucket_name='extra',
    source_type=ESourceType.PARTNER_SOURCE,
)
small_cashback_1 = create_cashback_for_dsbs(
    name='small_cashback_1',
    share=0.05,
    tags=[],
    mskus=[DSBS_WITH_TWO_CASHBACKS_SKU],
    bucket_name='default',
)
small_cashback_2 = create_cashback_for_dsbs(
    name='small_cashback_2',
    share=0.05,
    tags=[],
    mskus=[DSBS_WITH_TWO_CASHBACKS_SKU],
    bucket_name='extra',
)
blue_offer_cnc_from_medicine_hid = create_blue_offer(
    name="cnc_medicine",
    price=1000,
    feed=cnc_shop.datafeed_id,
    pickup_buckets=[14040],
    is_fulfillment=False,
    post_term_delivery=True,
    is_baa=True,
)
blue_offer_cnc_from_alco_hid = create_blue_offer(
    name="cnc_alco",
    price=1000,
    feed=cnc_shop.datafeed_id,
    pickup_buckets=[14040],
    is_fulfillment=False,
    post_term_delivery=True,
)
offer_dsbs = create_white_offer(
    price=1000,
    hyperid=DSBS_SKU,
    sku=DSBS_SKU,
    fesh=dsbs_shop.fesh,
    is_dsbs=True,
    delivery_buckets=[4240],
    promos=[blue_cashback_for_dsbs],
)

offer_dsbs_cehac_assortment_group = create_white_offer(
    price=1000,
    hyperid=DSBS_CEHAC_SKU,
    sku=DSBS_CEHAC_SKU,
    fesh=dsbs_shop.fesh,
    is_dsbs=True,
    delivery_buckets=[4240],
    promos=[small_cashback_for_dsbs_cehac, big_cashback_for_dsbs_cehac],
)

offer_dsbs_diy_assortment_group = create_white_offer(
    price=3000,
    hyperid=DSBS_DIY_SKU,
    sku=DSBS_DIY_SKU,
    fesh=dsbs_shop.fesh,
    is_dsbs=True,
    delivery_buckets=[4240],
    promos=[small_cashback_for_dsbs_diy, big_cashback_for_dsbs_diy],
)

offer_dsbs_default_assortment_group = create_white_offer(
    price=4000,
    hyperid=DSBS_DEFAULT_SKU,
    sku=DSBS_DEFAULT_SKU,
    fesh=dsbs_shop.fesh,
    is_dsbs=True,
    delivery_buckets=[4240],
    promos=[small_cashback_for_dsbs_default, big_cashback_for_dsbs_default],
)

offer_with_two_small_cashbacks = create_white_offer(
    price=4000,
    hyperid=DSBS_WITH_TWO_CASHBACKS_SKU,
    sku=DSBS_WITH_TWO_CASHBACKS_SKU,
    fesh=dsbs_shop.fesh,
    is_dsbs=True,
    delivery_buckets=[4240],
    promos=[small_cashback_1, small_cashback_2],
)

offer_for_detail_groups_test = create_white_offer(
    price=4000,
    hyperid=DSBS_FOR_DETAILS_TEST_SKU,
    sku=DSBS_FOR_DETAILS_TEST_SKU,
    fesh=dsbs_shop.fesh,
    is_dsbs=True,
    delivery_buckets=[4240],
    promos=[small_cashback_for_dsbs_default, big_cashback_for_dsbs_default, blue_cashback_for_dsbs],
)

offer_for_detail_groups_sorting_test = create_white_offer(
    price=4000,
    hyperid=DSBS_FOR_DETAILS_SORTING_TEST_SKU,
    sku=DSBS_FOR_DETAILS_SORTING_TEST_SKU,
    fesh=dsbs_shop.fesh,
    is_dsbs=True,
    delivery_buckets=[4240],
    promos=[blue_cashback_for_dsbs_with_details_1, blue_cashback_for_dsbs_with_details_2, blue_cashback_for_dsbs],
)

# для проверки корректности счётчика по типу акции, этот оффер не должен посчитаться
blue_offer_no_promo = create_blue_offer(price=1000)

blue_offer_personal_cb = create_blue_offer(price=1000)

blue_offer_cashback_value = create_blue_offer(price=1000)

blue_offer_cashback_value_with_max_thresholds = create_blue_offer(price=10000, delivery_buckets=[777, 888])

blue_offer_personal_compensation = create_blue_offer(price=1000)


# Market SKU
def create_msku(offers_list=None, hid=None, num=None):
    num = num or next(nummer)
    msku_args = dict(
        sku=num,
        hyperid=num,
    )
    if offers_list is not None:
        assert isinstance(offers_list, list)
        msku_args["blue_offers"] = offers_list
    if hid is not None:
        msku_args.update(hid=hid)
    return MarketSku(**msku_args)


msku_cb_no_predicates = create_msku([blue_offer_cb_no_predicates])
msku_1 = create_msku([blue_offer_1])
msku_2 = create_msku([blue_offer_2])
msku_3 = create_msku([blue_offer_3])
msku_4 = create_msku([blue_offer_4])
msku_4_employees_cashback = create_msku([blue_offer_4_employees_cashback])
msku_6 = create_msku([blue_offer_6])
msku_7 = create_msku([blue_offer_7])
msku_8 = create_msku([blue_offer_8])
msku_crossdock = create_msku([blue_offer_crossdock])
msku_with_extra_cb = create_msku([blue_offer_with_extra_cb])
msku_with_extra_cb_shop_carrier = create_msku([blue_offer_with_extra_cb_shop_carrier])
msku_with_extra_and_normal_cb = create_msku([blue_offer_with_extra_and_normal_cb])
msku_with_extra_and_normal_cb_shop_carrier = create_msku([blue_offer_with_extra_and_normal_cb_shop_carrier])
msku_with_max_offer_cashback = create_msku([blue_offer_with_max_offer_cashback])
msku_with_max_offer_cashback_cheap = create_msku([blue_offer_with_max_offer_cashback_cheap])
msku_with_extra_3p_cb = create_msku(
    [
        blue_offer_with_extra_3p_cb,
    ]
)
msku_ff_with_extra_3p_cb_and_plain = create_msku([blue_ff_offer_with_extra_3p_cb_and_plain])
msku_with_extra_cb_shop_carrier_3p_cb = create_msku([blue_offer_with_extra_cb_shop_carrier_3p_cb])
msku_extra_cb_market_dt = create_msku(
    [
        blue_offer_extra_cb_market_dt,
    ]
)
msku_cb_shop_dt = create_msku(
    [
        blue_offer_cb_shop_dt,
    ]
)
msku_cb_all_dt = create_msku(
    [
        blue_offer_cb_shop_all,
    ]
)
msku_cb_shop_dt_with_all = create_msku(
    [
        blue_offer_cb_shop_dt_with_all,
    ]
)
msku_cb_market_dt_with_all = create_msku(
    [
        blue_offer_cb_market_dt_with_all,
    ]
)
msku_extra_cb_market_all_dt = create_msku(
    [
        blue_offer_extra_cb_market_all_dt,
    ]
)
msku_cb_shop_all_dt = create_msku(
    [
        blue_offer_cb_shop_all_dt,
    ]
)
msku_cnc_medicine = create_msku([blue_offer_cnc_from_medicine_hid], num=CNC_MEDICINE_MSKU, hid=MEDICNE_HID)

msku_cnc_alco = create_msku([blue_offer_cnc_from_alco_hid], num=CNC_ALCO_MSKU, hid=ALCO_HID)

msku_dsbs = create_msku(num=DSBS_SKU, hid=DSBS_HID)

msku_dsbs_cehac = create_msku(num=DSBS_CEHAC_SKU, hid=DSBS_CEHAC_HID)

msku_dsbs_diy = create_msku(num=DSBS_DIY_SKU, hid=DSBS_DIY_HID)

msku_dsbs_default = create_msku(num=DSBS_DEFAULT_SKU, hid=DSBS_DEFAULT_HID)

msku_without_prepayment_dsbs = create_msku(num=DSBS_WITHOUT_PREPAYMENT_SKU, hid=DSBS_WITHOUT_PREPAYMENT_HID)

msku_with_prepayment_dsbs = create_msku(num=DSBS_WITH_PREPAYMENT_SKU, hid=DSBS_WITH_PREPAYMENT_HID)

msku_for_detail_groups_test = create_msku(num=DSBS_FOR_DETAILS_TEST_SKU, hid=DSBS_FOR_DETAILS_TEST_HID)

msku_for_detail_groups_sorting_test = create_msku(
    num=DSBS_FOR_DETAILS_SORTING_TEST_SKU, hid=DSBS_FOR_DETAILS_SORTING_TEST_HID
)

msku_with_two_small_cashbacks = create_msku(num=DSBS_WITH_TWO_CASHBACKS_SKU, hid=DSBS_WITH_TWO_CASHBACKS_HID)

msku_no_promo = create_msku([blue_offer_no_promo])

msku_personal_cb = create_msku([blue_offer_personal_cb])

msku_personal_compensation = create_msku([blue_offer_personal_compensation])

msku_cashback_value = create_msku([blue_offer_cashback_value])

msku_cashback_value_with_max_thresholds = create_msku([blue_offer_cashback_value_with_max_thresholds])

# Акции
# Действующая акция
blue_cashback_1 = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    description='blue_cashback_1_description',
    key=b64url_md5(next(nummer)),
    shop_promo_id='blue_cashback_1',
    blue_cashback=PromoBlueCashback(
        share=0.02,
        version=10,
        priority=3,
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_1, msku_crossdock]),
    ],
)


# Акция уже закoнчилась
blue_cashback_2 = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    description='blue_cashback_2_description',
    key=b64url_md5(next(nummer)),
    shop_promo_id='blue_cashback_2',
    end_date=now - delta_small,
    blue_cashback=PromoBlueCashback(
        share=0.25,
        version=11,
        priority=3,
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_2]),
    ],
)


# Акция ещё не началaсь
blue_cashback_3 = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    description='blue_cashback_3_description',
    key=b64url_md5(next(nummer)),
    shop_promo_id='blue_cashback_3',
    start_date=now + delta_small,
    blue_cashback=PromoBlueCashback(
        share=0.26,
        version=12,
        priority=3,
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_3]),
    ],
)


# Акция с низким внутренним приоритетом кэшбэка
blue_cashback_4 = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    description='blue_cashback_4_description',
    key=b64url_md5(next(nummer)),
    shop_promo_id='blue_cashback_4',
    blue_cashback=PromoBlueCashback(
        share=0.27,
        version=13,
        priority=6,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            mskus=[
                msku_4,
                msku_with_extra_and_normal_cb_shop_carrier,
                msku_with_extra_and_normal_cb,
                msku_with_extra_and_normal_cb_shop_carrier,
            ]
        ),
    ],
)


# Акция с высоким внутренним приоритетом кэшбэка - ограничена по времени
blue_cashback_5 = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    description='blue_cashback_5_description',
    key=b64url_md5(next(nummer)),
    shop_promo_id='blue_cashback_5',
    start_date=now + delta_small,
    end_date=now + delta_big,
    blue_cashback=PromoBlueCashback(
        share=0.28,
        version=13,
        priority=3,
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_4]),
    ],
)


# кешбек с предикатами - условиями применимости кешбека
# в данном случае - перкой повышенного кешбека
blue_cashback_7 = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key=b64url_md5(next(nummer)),
    shop_promo_id='blue_cashback_7',
    blue_cashback=PromoBlueCashback(
        share=0.33,
        version=6,
        priority=2,
    ),
    restrictions=PromoRestrictions(
        predicates=[
            {
                'perks': ['yandex_extra_cashback'],
                'delivery_partner_types': [
                    2,
                ],
            }
        ],
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            mskus=[
                msku_with_extra_cb,
                msku_with_extra_and_normal_cb,
                msku_with_extra_cb_shop_carrier,
                msku_with_extra_3p_cb,
                msku_ff_with_extra_3p_cb_and_plain,
                msku_with_extra_and_normal_cb_shop_carrier,
            ]
        ),
    ],
)


# кешбэк для сотрудников
blue_cashback_for_employees = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key=b64url_md5(next(nummer)),
    shop_promo_id='blue_cashback_4_employees',
    blue_cashback=PromoBlueCashback(
        share=0.5,
        version=6,
        priority=1,
    ),
    restrictions=PromoRestrictions(
        predicates=[
            {
                'perks': ['yandex_employee_extra_cashback'],
            }
        ]
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_4_employees_cashback]),
    ],
)

# кешбек с ограничением максимальной цены кешбека
blue_cashback_8 = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key=b64url_md5(next(nummer)),
    shop_promo_id='blue_cashback_8',
    blue_cashback=PromoBlueCashback(
        share=0.33,
        version=6,
        priority=1,
        max_offer_cashback=30000,  # ограничение в 300 руб
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_with_max_offer_cashback, msku_with_max_offer_cashback_cheap]),
    ],
    source_type=ESourceType.PARTNER_SOURCE,
)

# повышенный кешбэк для 3p партнёров
blue_cashback_9 = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key=b64url_md5(next(nummer)),
    shop_promo_id='blue_cashback_9',
    tags=[
        "extra-cashback",
    ],
    blue_cashback=PromoBlueCashback(
        share=0.5,
        version=6,
        priority=1,
    ),
    restrictions=PromoRestrictions(
        predicates=[
            {
                'perks': ['yandex_extra_cashback'],
                'at_supplier_warehouse': True,
                'delivery_partner_types': [
                    2,
                ],
            }
        ],
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            mskus=[
                msku_with_extra_cb_shop_carrier_3p_cb,
                msku_with_extra_3p_cb,
                msku_ff_with_extra_3p_cb_and_plain,
            ]
        ),
    ],
)


# повышенный кешбэк с проверкой типа партнёра в предикате, не соответствующей дефолтному поведению (только для MARKET)
blue_cashback_extra_no_delivery_partner_type = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key=b64url_md5(next(nummer)),
    shop_promo_id='blue_extra_cb_with_dpt_check',
    tags=[
        "extra-cashback",
    ],
    blue_cashback=PromoBlueCashback(
        share=0.5,
        version=6,
        priority=1,
    ),
    restrictions=PromoRestrictions(
        predicates=[
            {
                'perks': ['yandex_extra_cashback'],
                'delivery_partner_types': [
                    1,
                ],
            }
        ]
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_cb_shop_dt, msku_cb_shop_dt_with_all, msku_extra_cb_market_dt]),
    ],
)


# обычный кешбэк с проверкой типа партнёра в предикате, не соответствующей дефолтному поведению (отсутствие проверки)
blue_cashback_with_delivery_partner_type = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key=b64url_md5(next(nummer)),
    shop_promo_id='blue_cb_with_dpt_check',
    blue_cashback=PromoBlueCashback(
        share=0.3,
        version=6,
        priority=3,
    ),
    restrictions=PromoRestrictions(
        predicates=[
            {
                'delivery_partner_types': [
                    2,
                ],
            }
        ]
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_cb_shop_dt, msku_extra_cb_market_dt, msku_cb_market_dt_with_all]),
    ],
)


# кешбэк, который требует соответствия магазина обоим типам
blue_cashback_with_all_delivery_partner_types = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key=b64url_md5(next(nummer)),
    shop_promo_id='blue_cb_with_dpt_all_check',
    blue_cashback=PromoBlueCashback(
        share=0.3,
        version=6,
        priority=2,
    ),
    restrictions=PromoRestrictions(
        predicates=[
            {
                'delivery_partner_types': [
                    1,
                    2,
                ],
            }
        ]
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_cb_all_dt, msku_extra_cb_market_all_dt, msku_cb_shop_all_dt]),
    ],
)


# кешбэк с пустыми предикатами
blue_cb_no_predicates = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key=b64url_md5(next(nummer)),
    shop_promo_id='cashback_no_predicates',
    blue_cashback=PromoBlueCashback(
        share=0.3,
        version=6,
        priority=2,
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_cb_no_predicates]),
    ],
)


# кешбэки для теста перс.промо
blue_cashback_norm = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key=b64url_md5(next(nummer)),
    shop_promo_id='blue_cashback_norm',
    description='blue_cashback_norm',
    blue_cashback=PromoBlueCashback(
        share=0.3,
        version=1,
        priority=30,
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_personal_cb]),
    ],
)
blue_cashback_extra = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key=b64url_md5(next(nummer)),
    shop_promo_id='blue_cashback_extra',
    description='blue_cashback_extra',
    blue_cashback=PromoBlueCashback(
        share=0.3,
        version=1,
        priority=10,
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_personal_cb]),
    ],
)
blue_cashback_personal = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key=b64url_md5(next(nummer)),
    shop_promo_id='blue_cashback_personal',
    description='blue_cashback_personal',
    blue_cashback=PromoBlueCashback(
        share=0.3,
        version=1,
        priority=None,
    ),
    promo_internal_priority=-20,
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_personal_cb]),
    ],
    source_type=ESourceType.DCO_PERSONAL,
)
# Кешбэк для теста компенсации
blue_cashback_personal_compensation = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key=b64url_md5(next(nummer)),
    shop_promo_id='blue_cashback_personal_compensation',
    description='blue_cashback_personal_compensation',
    blue_cashback=PromoBlueCashback(
        share=0.3,
        version=1,
        priority=None,
    ),
    promo_internal_priority=-20,
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_personal_compensation]),
    ],
    restrictions=PromoRestrictions(
        predicates=[
            {
                'perks': ['!someperk'],
            }
        ]
    ),
    source_type=ESourceType.DCO_PERSONAL,
)
blue_cashback_with_client_restriction = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key=b64url_md5(next(nummer)),
    shop_promo_id='blue_cashback_with_client_restriction',
    description='blue_cashback_with_client_restriction',
    blue_cashback=PromoBlueCashback(
        share=0.3,
        version=1,
        priority=10,
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_8]),
    ],
    promo_bucket_name='default',
    user_device_types=[UserDeviceType.MARKET_GO, UserDeviceType.DESKTOP],
)


def create_cashback_for_cashback_value_tests(
    shop_promo_id, priority, promo_bucket_name, tags, required_buckets=[], cms_id=None
):
    return Promo(
        promo_type=PromoType.BLUE_CASHBACK,
        description='blue_cashback_description',
        key=b64url_md5(next(nummer)),
        shop_promo_id=shop_promo_id,
        blue_cashback=PromoBlueCashback(
            share=0.3,
            version=10,
            priority=priority,
        ),
        tags=tags,
        offers_matching_rules=[
            OffersMatchingRules(mskus=[msku_cashback_value]),
        ],
        promo_bucket_name=promo_bucket_name,
        required_buckets=required_buckets,
        cms_description_semantic_id=cms_id,
    )


default_cashback_low_priority_1 = create_cashback_for_cashback_value_tests(
    'default_cashback_low_priority_1', 15, 'default', ['some_tag_1']
)
default_cashback_low_priority_2 = create_cashback_for_cashback_value_tests(
    'default_cashback_low_priority_2', 12, 'default', ['some_tag_2']
)
default_cashback_high_priority = create_cashback_for_cashback_value_tests(
    'default_cashback_high_priority', 4, 'default', ['some_tag_3'], cms_id='default-cashback-id'
)
extra_cashback = create_cashback_for_cashback_value_tests(
    'extra_cashback_priority', 1, 'extra', ['extra-cashback'], ['default'], cms_id='extra-cashback-id'
)
cashback_with_empty_bucket = create_cashback_for_cashback_value_tests(
    'cashback_with_empty_bucket', 3, '', ['empty_tag']
)


def create_cashback_with_rearr_flag(shop_promo_id, promo_bucket_name, rearr_flags=None, priority=10):
    return Promo(
        promo_type=PromoType.BLUE_CASHBACK,
        description='blue_cashback_description',
        key=b64url_md5(nummer.next()),
        shop_promo_id=shop_promo_id,
        blue_cashback=PromoBlueCashback(
            share=0.3,
            version=10,
            priority=priority,
            predicates=[
                {
                    'experiment_rearr_flags': rearr_flags,
                },
            ],
        ),
        tags=[],
        offers_matching_rules=[
            OffersMatchingRules(mskus=[msku_8]),
        ],
        promo_bucket_name=promo_bucket_name,
        required_buckets=[],
    )


cashback_instead_of_under_flag1 = create_cashback_with_rearr_flag(
    'cashback_instead_of_under_flag1',
    'default',
    [],
    # у кешбека обратный приоритет, это промо менее приоритетно, чем cashback_under_flag1
    priority=100,
)
cashback_under_flag1 = create_cashback_with_rearr_flag(
    'cashback_under_flag1',
    'default',
    [
        'flag1=1',
    ],
)
cashback_under_flag2 = create_cashback_with_rearr_flag(
    'cashback_under_flag2',
    'extra',
    [
        'flag2=some_value',
    ],
)
cashback_under_flag3 = create_cashback_with_rearr_flag('cashback_under_flag3', 'some_bucket', [])

cashback_from_partner_source = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    description='blue_cashback_description',
    key=b64url_md5(next(nummer)),
    shop_promo_id='cashback_from_partner_source',
    blue_cashback=PromoBlueCashback(
        share=0.3,
        version=10,
        priority=-100,
    ),
    source_type=ESourceType.PARTNER_SOURCE,
    tags=['partner_source_tag'],
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_cashback_value]),
    ],
    promo_bucket_name='extra',
)

# Кешбеки для теста поля PartnerInfo и трешхолдов
cashback_with_threshholds = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key=b64url_md5(next(nummer)),
    shop_promo_id='cashback_with_threshholds',
    blue_cashback=PromoBlueCashback(
        share=0.33,
        version=6,
        priority=3,
        partner_info=PartnerInfo(partner_id=123, tariff_version_id=0),
        max_offer_thresholds=[
            Threshold(code='first', value=1),
            Threshold(code='second', value=2),
            Threshold(code='third', value=3),
        ],
        min_order_thresholds=[
            Threshold(code='bla', value=10),
            Threshold(code='blabla', value=20),
            Threshold(code='blablabla', value=300),
        ],
    ),
    promo_bucket_name='default',
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_6]),
    ],
)

# Кешбеки для теста учета максимального значения кешбека,
# Одно из значений в максимальных трешхолдах меньше
# чем максимальное значение кешбека, берем его
cashback_with_max_value_from_threshold = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key=b64url_md5(next(nummer)),
    shop_promo_id='cashback_with_max_value_from_threshold',
    blue_cashback=PromoBlueCashback(
        share=0.9,
        version=6,
        priority=3,
        max_offer_cashback=30000,  # ограничение в 300 руб
        max_offer_thresholds=[
            Threshold(code='first', value=199),
            Threshold(code='second', value=299),
            Threshold(code='third', value=499),
        ],
    ),
    promo_bucket_name='extra',
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_7]),
    ],
)

# Максимальное значие кешбека в промо меньше,
# чем все значения в трешхолдах, берем его
cashback_with_max_value_from_promo = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key=b64url_md5(next(nummer)),
    shop_promo_id='cashback_with_max_value_from_promo',
    blue_cashback=PromoBlueCashback(
        share=0.9,
        version=6,
        priority=3,
        max_offer_cashback=30000,  # ограничение в 300 руб
        max_offer_thresholds=[
            Threshold(code='first', value=1000),
            Threshold(code='second', value=2000),
            Threshold(code='third', value=3000),
        ],
    ),
    promo_bucket_name='default',
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_7]),
    ],
)

# Кешбеки для проверки суммирования значения кешбеков в метапромо
cashback_with_max_value_1 = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key=b64url_md5(next(nummer)),
    shop_promo_id='cashback_with_max_value_1',
    blue_cashback=PromoBlueCashback(
        share=0.2,
        version=6,
        priority=3,
        max_offer_thresholds=[
            Threshold(code='first', value=500),
            Threshold(code='second', value=700),
            Threshold(code='third', value=3000),
        ],
    ),
    promo_bucket_name='default',
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_cashback_value_with_max_thresholds]),
    ],
)
cashback_with_max_value_2 = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key=b64url_md5(next(nummer)),
    shop_promo_id='cashback_with_max_value_2',
    blue_cashback=PromoBlueCashback(
        share=0.1,
        version=6,
        priority=3,
        max_offer_thresholds=[Threshold(code='first', value=7000), Threshold(code='second', value=10000)],
    ),
    promo_bucket_name='extra',
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_cashback_value_with_max_thresholds]),
    ],
)
cashback_pharma = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key=b64url_md5(next(nummer)),
    shop_promo_id='cashback_pharma',
    blue_cashback=PromoBlueCashback(
        share=0.2,
        version=6,
        priority=3,
    ),
    promo_bucket_name='some_bucket',
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_cashback_value_with_max_thresholds]),
    ],
)
cashback_mastercard_low_priority = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key=b64url_md5(next(nummer)),
    shop_promo_id='cashback_mastercard_low_priority',
    blue_cashback=PromoBlueCashback(
        share=0.15,
        version=6,
        priority=30,
        allowed_payment_methods=[EPaymentMethods.YANDEX],
    ),
    promo_bucket_name='payment_system',
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_cashback_value_with_max_thresholds]),
    ],
)

cashback_mastercard_high_priority = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key=b64url_md5(next(nummer)),
    shop_promo_id='cashback_mastercard_high_priority',
    blue_cashback=PromoBlueCashback(
        share=0.16,
        version=6,
        priority=-30,
        allowed_payment_methods=[EPaymentMethods.YANDEX],
    ),
    promo_bucket_name='payment_system',
    offers_matching_rules=[
        OffersMatchingRules(
            mskus=[
                msku_cashback_value_with_max_thresholds,
                msku_without_prepayment_dsbs,
                msku_with_prepayment_dsbs,
            ]
        ),
    ],
)

offer_dsbs_without_prepayment = create_white_offer(
    price=1000,
    hyperid=DSBS_WITHOUT_PREPAYMENT_SKU,
    sku=DSBS_WITHOUT_PREPAYMENT_SKU,
    fesh=dsbs_shop_without_prepayment.fesh,
    is_dsbs=True,
    delivery_buckets=[4242],
    promos=[cashback_mastercard_high_priority],
)

offer_dsbs_with_prepayment = create_white_offer(
    price=1000,
    hyperid=DSBS_WITH_PREPAYMENT_SKU,
    sku=DSBS_WITH_PREPAYMENT_SKU,
    fesh=dsbs_shop_with_prepayment.fesh,
    is_dsbs=True,
    delivery_buckets=[4042],
    promos=[cashback_mastercard_high_priority],
)


# ручная привязка промо к офферам
def bind_promo(offer, promos):
    assert isinstance(promos, list)
    offer.promo = promos
    offer.blue_promo_key = [promo.shop_promo_id for promo in promos]


bind_promo(blue_offer_cb_no_predicates, [blue_cb_no_predicates])
bind_promo(blue_offer_1, [blue_cashback_1])
bind_promo(blue_offer_2, [blue_cashback_2])
bind_promo(blue_offer_3, [blue_cashback_3])
bind_promo(blue_offer_4, [blue_cashback_4, blue_cashback_5])
bind_promo(blue_offer_4_employees_cashback, [blue_cashback_for_employees])
bind_promo(blue_offer_crossdock, [blue_cashback_1])
bind_promo(blue_offer_with_extra_cb, [blue_cashback_7])
bind_promo(blue_offer_with_extra_cb_shop_carrier, [blue_cashback_7])
bind_promo(blue_offer_with_extra_and_normal_cb, [blue_cashback_4, blue_cashback_7])
bind_promo(blue_offer_with_extra_and_normal_cb_shop_carrier, [blue_cashback_4, blue_cashback_7])
bind_promo(blue_offer_with_max_offer_cashback, [blue_cashback_8])
bind_promo(blue_offer_with_max_offer_cashback_cheap, [blue_cashback_8])
bind_promo(blue_offer_with_extra_3p_cb, [blue_cashback_9, blue_cashback_7])
bind_promo(blue_ff_offer_with_extra_3p_cb_and_plain, [blue_cashback_9, blue_cashback_7])
bind_promo(blue_offer_with_extra_cb_shop_carrier_3p_cb, [blue_cashback_9])
bind_promo(
    blue_offer_extra_cb_market_dt,
    [blue_cashback_extra_no_delivery_partner_type, blue_cashback_with_delivery_partner_type],
)
bind_promo(
    blue_offer_cb_shop_dt, [blue_cashback_extra_no_delivery_partner_type, blue_cashback_with_delivery_partner_type]
)
bind_promo(blue_offer_extra_cb_market_all_dt, [blue_cashback_with_all_delivery_partner_types])
bind_promo(blue_offer_cb_shop_all_dt, [blue_cashback_with_all_delivery_partner_types])
bind_promo(blue_offer_cb_shop_all, [blue_cashback_with_all_delivery_partner_types])
bind_promo(blue_offer_cb_shop_dt_with_all, [blue_cashback_extra_no_delivery_partner_type])
bind_promo(blue_offer_cb_market_dt_with_all, [blue_cashback_with_delivery_partner_type])
bind_promo(blue_offer_cnc_from_medicine_hid, [blue_cashback_for_dsbs])
bind_promo(blue_offer_cnc_from_alco_hid, [blue_cashback_for_dsbs])
bind_promo(offer_dsbs, [blue_cashback_for_dsbs])
bind_promo(offer_dsbs_cehac_assortment_group, [small_cashback_for_dsbs_cehac, big_cashback_for_dsbs_cehac])
bind_promo(offer_dsbs_diy_assortment_group, [small_cashback_for_dsbs_diy, big_cashback_for_dsbs_diy])
bind_promo(offer_dsbs_default_assortment_group, [small_cashback_for_dsbs_default, big_cashback_for_dsbs_default])
bind_promo(offer_dsbs_with_prepayment, [cashback_mastercard_high_priority])
bind_promo(offer_dsbs_without_prepayment, [cashback_mastercard_high_priority])
bind_promo(offer_with_two_small_cashbacks, [small_cashback_1, small_cashback_2])
bind_promo(blue_offer_personal_cb, [blue_cashback_norm, blue_cashback_extra, blue_cashback_personal])
bind_promo(blue_offer_6, [cashback_with_threshholds])
bind_promo(blue_offer_7, [cashback_with_max_value_from_threshold, cashback_with_max_value_from_promo])
bind_promo(
    blue_offer_8,
    [
        cashback_under_flag1,
        cashback_instead_of_under_flag1,
        cashback_under_flag2,
        cashback_under_flag3,
        blue_cashback_with_client_restriction,
    ],
)
bind_promo(
    blue_offer_cashback_value,
    [
        default_cashback_low_priority_1,
        default_cashback_low_priority_2,
        default_cashback_high_priority,
        extra_cashback,
        cashback_with_empty_bucket,
        cashback_from_partner_source,
    ],
)
bind_promo(
    blue_offer_cashback_value_with_max_thresholds,
    [
        cashback_with_max_value_1,
        cashback_with_max_value_2,
        cashback_mastercard_high_priority,
        cashback_mastercard_low_priority,
        cashback_pharma,
    ],
)
bind_promo(blue_offer_personal_compensation, [blue_cashback_personal_compensation])
bind_promo(
    offer_for_detail_groups_test,
    [
        small_cashback_for_dsbs_default,
        big_cashback_for_dsbs_default,
        blue_cashback_for_dsbs,
    ],
)
bind_promo(
    offer_for_detail_groups_sorting_test,
    [
        blue_cashback_for_dsbs_with_details_1,
        blue_cashback_for_dsbs_with_details_2,
        blue_cashback_for_dsbs,
    ],
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.nordstream_autogenerate = False
        cls.settings.default_search_experiment_flags += ['market_nordstream=0', 'market_nordstream_dsbs=0']
        cls.index.shops += [
            virtual_shop,
            shop_1,
            crossdock_shop,
            usual_3p_shop,
            shop_with_shop_carrier,
            crossdock_shop_with_direct,
            dsbs_shop,
            dsbs_shop_without_prepayment,
            dsbs_shop_with_prepayment,
            cnc_shop,
        ]

        cls.index.mskus += [
            msku_cb_no_predicates,
            msku_1,
            msku_2,
            msku_3,
            msku_4,
            msku_6,
            msku_7,
            msku_8,
            msku_crossdock,
            msku_with_extra_cb,
            msku_with_extra_cb_shop_carrier,
            msku_with_extra_and_normal_cb,
            msku_with_extra_and_normal_cb_shop_carrier,
            msku_with_max_offer_cashback,
            msku_with_max_offer_cashback_cheap,
            msku_with_extra_3p_cb,
            msku_ff_with_extra_3p_cb_and_plain,
            msku_with_extra_cb_shop_carrier_3p_cb,
            msku_extra_cb_market_dt,
            msku_cb_shop_dt,
            msku_cb_all_dt,
            msku_cb_shop_dt_with_all,
            msku_cb_market_dt_with_all,
            msku_extra_cb_market_all_dt,
            msku_cb_shop_all_dt,
            msku_no_promo,
            msku_personal_cb,
            msku_personal_compensation,
            msku_cashback_value,
            msku_4_employees_cashback,
            msku_cnc_medicine,
            msku_cnc_alco,
            msku_dsbs,
            msku_dsbs_cehac,
            msku_dsbs_diy,
            msku_dsbs_default,
            msku_without_prepayment_dsbs,
            msku_with_prepayment_dsbs,
            msku_with_two_small_cashbacks,
            msku_cashback_value_with_max_thresholds,
            msku_for_detail_groups_test,
            msku_for_detail_groups_sorting_test,
        ]

        cls.index.offers += [
            offer_dsbs,
            offer_with_two_small_cashbacks,
            offer_dsbs_without_prepayment,
            offer_dsbs_with_prepayment,
            offer_dsbs_cehac_assortment_group,
            offer_dsbs_diy_assortment_group,
            offer_dsbs_default_assortment_group,
            offer_for_detail_groups_test,
            offer_for_detail_groups_sorting_test,
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=123, home_region=213, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=145, home_region=213, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=124, home_region=213),
            DynamicWarehouseInfo(id=1214, home_region=213),
            DynamicWarehouseInfo(id=dsbs_shop.warehouse_id, home_region=213),
            DynamicWarehouseInfo(id=dsbs_shop_with_prepayment.warehouse_id, home_region=213),
            DynamicWarehouseInfo(id=dsbs_shop_without_prepayment.warehouse_id, home_region=213),
            DynamicWarehouseInfo(id=cnc_shop.warehouse_id, home_region=213, holidays_days_set_key=1),
            DynamicWarehouseToWarehouseInfo(warehouse_from=123, warehouse_to=123),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
            DynamicWarehouseToWarehouseInfo(warehouse_from=124, warehouse_to=124),
            DynamicWarehouseToWarehouseInfo(warehouse_from=1214, warehouse_to=1214),
            DynamicWarehouseToWarehouseInfo(warehouse_from=dsbs_shop.warehouse_id, warehouse_to=dsbs_shop.warehouse_id),
            DynamicWarehouseToWarehouseInfo(
                warehouse_from=dsbs_shop_with_prepayment.warehouse_id,
                warehouse_to=dsbs_shop_with_prepayment.warehouse_id,
            ),
            DynamicWarehouseToWarehouseInfo(
                warehouse_from=dsbs_shop_without_prepayment.warehouse_id,
                warehouse_to=dsbs_shop_without_prepayment.warehouse_id,
            ),
            DynamicWarehouseToWarehouseInfo(warehouse_from=cnc_shop.warehouse_id, warehouse_to=cnc_shop.warehouse_id),
            DynamicWarehousesPriorityInRegion(region=213, warehouses=[123, 124, 145, 1214, cnc_shop.warehouse_id]),
            DynamicDeliveryServiceInfo(
                99,
                "self-delivery",
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=213, region_to=225, days_key=1)],
                time_intervals=[
                    TimeIntervalsForRegion(
                        region=213, intervals=[TimeIntervalsForDaysInfo(intervals_key=1, days_key=1)]
                    )
                ],
            ),
            DynamicDeliveryServiceInfo(id=100, name='yandex_market'),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=123,
                delivery_service_id=100,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=19, region_to=213)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=100,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=19, region_to=213)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=1214,
                delivery_service_id=100,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=19, region_to=213)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=124,
                delivery_service_id=DeliveryOption.SHOP_CARRIER_ID,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=19, region_to=213)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=1214,
                delivery_service_id=DeliveryOption.SHOP_CARRIER_ID,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=19, region_to=213)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=cnc_shop.warehouse_id,
                delivery_service_id=99,
                operation_time=0,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=12, region_to=225, packaging_time=TimeInfo(1))
                ],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=dsbs_shop.warehouse_id,
                delivery_service_id=99,
                operation_time=0,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=12, region_to=213, packaging_time=TimeInfo(1))
                ],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=dsbs_shop_without_prepayment.warehouse_id,
                delivery_service_id=99,
                operation_time=0,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=12, region_to=213, packaging_time=TimeInfo(1))
                ],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=dsbs_shop_with_prepayment.warehouse_id,
                delivery_service_id=99,
                operation_time=0,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=12, region_to=213, packaging_time=TimeInfo(1))
                ],
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=777,
                dc_bucket_id=777,
                fesh=DEFAULT_SHOP_ID,
                carriers=[100],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[DeliveryOption(price=20, day_from=10, day_to=20, shop_delivery_price=5)],
                        payment_methods=[Payment.PT_CASH_ON_DELIVERY],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=888,
                dc_bucket_id=888,
                fesh=DEFAULT_SHOP_ID,
                carriers=[DeliveryOption.SHOP_CARRIER_ID],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[DeliveryOption(price=20, day_from=10, day_to=20, shop_delivery_price=5)],
                        payment_methods=[Payment.PT_CASH_ON_DELIVERY],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=999,
                dc_bucket_id=999,
                fesh=DEFAULT_SHOP_ID,
                carriers=[100],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[DeliveryOption(price=20, day_from=10, day_to=20, shop_delivery_price=5)],
                        payment_methods=[Payment.PT_CASH_ON_DELIVERY],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=4240,
                fesh=dsbs_shop.fesh,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
            DeliveryBucket(
                bucket_id=4242,
                fesh=dsbs_shop_without_prepayment.fesh,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
            DeliveryBucket(
                bucket_id=4042,
                fesh=dsbs_shop_with_prepayment.fesh,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=CNC_OUTLET,
                delivery_service_id=99,
                region=213,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(shipper_id=99, day_from=1, day_to=2, price=100),
                working_days=list(range(10)),
            )
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=14040,
                dc_bucket_id=80,
                fesh=cnc_shop.fesh,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                options=[PickupOption(outlet_id=CNC_OUTLET)],
            )
        ]

        cls.index.shops_payment_methods += [
            ShopPaymentMethods(
                fesh=dsbs_shop_without_prepayment.fesh,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[213],
                        payment_methods=[Payment.PT_CARD_ON_DELIVERY, Payment.PT_CASH_ON_DELIVERY],
                    ),
                ],
            ),
            ShopPaymentMethods(
                fesh=dsbs_shop_with_prepayment.fesh,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[213],
                        payment_methods=[Payment.PT_PREPAYMENT_CARD, Payment.PT_PREPAYMENT_OTHER],
                    ),
                ],
            ),
            ShopPaymentMethods(
                fesh=virtual_shop.fesh,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[213],
                        payment_methods=[Payment.PT_PREPAYMENT_CARD, Payment.PT_PREPAYMENT_OTHER],
                    ),
                ],
            ),
            ShopPaymentMethods(
                fesh=dsbs_shop.fesh,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[213],
                        payment_methods=[Payment.PT_PREPAYMENT_CARD, Payment.PT_PREPAYMENT_OTHER],
                    ),
                ],
            ),
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=90402,
                name='Большая категория из DIY',
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(
                        hid=DSBS_DIY_HID, name='Категория поменьше из DIY', output_type=HyperCategoryType.GURU
                    ),
                ],
            ),
            HyperCategory(
                hid=91009,
                name='Большая категория из CEHAC',
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(
                        hid=DSBS_CEHAC_HID, name='Категория поменьше из CEHAC', output_type=HyperCategoryType.GURU
                    ),
                ],
            ),
        ]

        cls.index.delivery_calc_feed_info += [DeliveryCalcFeedInfo(feed_id=cnc_shop.datafeed_id, pickupBuckets=[14040])]

    def build_common_request_part(self, place, rgb, rids, regset, perks, rearr_flags, additional_params):
        result = "place={place}&rgb={rgb}&rids={rids}&regset={regset}&adult=1".format(
            place=place, rgb=rgb, rids=rids, regset=regset
        )
        if perks:
            result += "&perks={}".format(",".join(perks))
        if rearr_flags:
            result += '&rearr-factors={}'.format(";".join("{}={}".format(key, val) for key, val in rearr_flags.items()))
        if additional_params:
            result += additional_params
        return result

    def build_blue_request(self, place, msku, offer, color, rids, regset, perk, rearr_flags, additional_params):
        request = self.build_common_request_part(
            place=place,
            rgb=color,
            rids=rids,
            regset=regset,
            perks=perk,
            rearr_flags=rearr_flags,
            additional_params=additional_params,
        )
        request += "&market-sku={msku}".format(msku=msku.sku)
        return request

    @staticmethod
    def calc_cashback_value(offer_price, cashback_share, max_offer_cashback=None):
        if max_offer_cashback:
            return min(int(ceil(offer_price * cashback_share)), max_offer_cashback)
        else:
            return int(ceil(offer_price * cashback_share))

    @staticmethod
    def calc_max_cashback_for_promo(cashback):
        result = cashback.max_offer_cashback / 100 if cashback.max_offer_cashback else None
        for threshold in cashback.max_offer_thresholds:
            if result and threshold.value:
                result = min(result, threshold.value)
            elif threshold.value:
                result = threshold.value

        return result

    @staticmethod
    def calc_cms_description_semantic_id(promo, is_extra_cashback):
        if promo.source_type == ESourceType.PARTNER_SOURCE:
            return 'partner-extra-cashback' if is_extra_cashback else 'partner-default-cashback'
        else:
            return promo.cms_description_semantic_id if promo.cms_description_semantic_id is not None else Absent()

    @staticmethod
    def calc_cashback_tags(promo_tags, cashback, assortment_group):
        if promo_tags is None:
            promo_tags = []
        if 'extra-cashback' in promo_tags:
            promo_tags.remove('extra-cashback')

        if assortment_group == 'cehac':
            return promo_tags + ['extra-cashback'] if cashback.share >= 0.02 else promo_tags
        if assortment_group == 'diy':
            return promo_tags + ['extra-cashback'] if cashback.share >= 0.04 else promo_tags

        return promo_tags + ['extra-cashback'] if cashback.share >= 0.06 else promo_tags

    def create_promos_for_checkers(self, promos, offer, assortment_group):
        def get_value_or_absent_if_none(value):
            return value if value else Absent()

        return [
            {
                'type': promo.type_name,
                'key': promo.key,
                'description': get_value_or_absent_if_none(promo.description),
                'startDate': NotEmpty() if promo.start_date else Absent(),
                'endDate': NotEmpty() if promo.end_date else Absent(),
                'share': promo.blue_cashback.share,
                'version': get_value_or_absent_if_none(promo.blue_cashback.version),
                # 'priority': promo.blue_cashback.priority, # TODO: вернуть после MARKETOUT-40507
                'value': T.calc_cashback_value(
                    offer.price,
                    promo.blue_cashback.share,
                    self.calc_max_cashback_for_promo(promo.blue_cashback),
                ),
                'tags': T.calc_cashback_tags(promo.tags, promo.blue_cashback, assortment_group),
                'partnerId': promo.blue_cashback.partner_info.partner_id
                if promo.blue_cashback.partner_info and promo.blue_cashback.partner_info.partner_id
                else Absent(),
                'marketTariffsVersionId': promo.blue_cashback.partner_info.tariff_version_id
                if promo.blue_cashback.partner_info and promo.blue_cashback.partner_info.partner_id
                else Absent(),
                'maxOfferCashbackThresholds': [
                    {
                        'code': threshold.code,
                        'value': threshold.value,
                    }
                    for threshold in promo.blue_cashback.max_offer_thresholds
                ]
                if promo.blue_cashback.max_offer_thresholds
                else Absent(),
                'minOrderTotalThresholds': [
                    {
                        'code': threshold.code,
                        'value': threshold.value,
                    }
                    for threshold in promo.blue_cashback.min_order_thresholds
                ]
                if promo.blue_cashback.min_order_thresholds
                else Absent(),
                'cmsDescriptionSemanticId': self.calc_cms_description_semantic_id(
                    promo,
                    'extra-cashback' in T.calc_cashback_tags(promo.tags, promo.blue_cashback, assortment_group),
                ),
            }
            for promo in promos
        ]

    def create_meta_promos_for_checkers(self, meta_promos):
        return {
            'promoCollections': [
                {
                    'id': meta_promo['id'],
                    'promoKeys': meta_promo['promoKeys'],
                    'info': {
                        'value': meta_promo['value'],
                        'tags': meta_promo['tags'],
                        'cmsDescriptionSemanticId': meta_promo['cms_description_semantic_id']
                        if 'cms_description_semantic_id' in meta_promo
                        else Absent(),
                    },
                }
                for meta_promo in meta_promos
            ]
        }

    def create_cashback_detail_groups(self, cashback_detail_groups):
        return {
            'cashbackDetails': [
                {
                    'groupId': cashback_detail_group['group_id'],
                    'groupName': cashback_detail_group['group_name'],
                    'promoKeys': cashback_detail_group['promo_keys'],
                    'value': cashback_detail_group['value'],
                    'tags': cashback_detail_group['tags'],
                    'cmsDescriptionSemanticId': cashback_detail_group['cms_description_semantic_id']
                    if 'cms_description_semantic_id' in cashback_detail_group
                    else Absent(),
                }
                for cashback_detail_group in cashback_detail_groups
            ]
        }

    def blue_checker_promo_enabled(
        self,
        promos,
        msku,
        offer,
        place,
        color,
        rids,
        regset,
        perk,
        rearr_flag,
        opt_fields=[],
        meta_promos=[],
        assortment_group='default',
        cashback_detail_groups=[],
        additional_params=None,
    ):
        response = self.report.request_json(
            self.build_blue_request(place, msku, offer, color, rids, regset, perk, rearr_flag, additional_params)
        )
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': offer.waremd5,
                    'promos': self.create_promos_for_checkers(promos, offer, assortment_group) if promos else Absent(),
                }
            ],
            allow_different_len=False,
        )
        if meta_promos:
            self.assertFragmentIn(
                response, self.create_meta_promos_for_checkers(meta_promos), allow_different_len=False
            )
        if cashback_detail_groups:
            self.assertFragmentIn(
                response,
                self.create_cashback_detail_groups(cashback_detail_groups),
                preserve_order=True,
                allow_different_len=False,
            )

    def blue_checker_promo_disabled(
        self, promos, msku, offer, place, color, rids, regset, perk, rearr_flag, additional_params
    ):
        response = self.report.request_json(
            self.build_blue_request(place, msku, offer, color, rids, regset, perk, rearr_flag, additional_params)
        )
        # Блок promo должен отсутствовать
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': offer.waremd5,
                    'promos': Absent(),
                }
            ],
        )

    def build_white_request(self, place, msku, offer, color, rids, regset, perk, rearr_flags, additional_params):
        request = self.build_common_request_part(
            place=place,
            rgb=color,
            rids=rids,
            regset=regset,
            perks=perk,
            rearr_flags=rearr_flags,
            additional_params=additional_params,
        )
        if place == 'offerinfo':
            request += '&offerid={}'.format(offer.waremd5)
        if place == 'productoffers':
            request += '&market-sku={}'.format(msku.sku)
        if place == 'prime':
            request += '&hyperid={}'.format(str(msku.sku))
        if place == 'multi_category':
            request += '&hid={}'.format(msku.category)
        request += '&use-default-offers=1&pp=28&rearr-factors=market_hide_regional_delimiter=1'
        return request

    def white_checker_promo_enabled(
        self,
        promos,
        msku,
        offer,
        place,
        color,
        rids,
        regset,
        perk,
        rearr_flag,
        opt_fields=[],
        meta_promos=[],
        assortment_group='default',
        cashback_detail_groups=[],
        additional_params=None,
    ):
        response = self.report.request_json(
            self.build_white_request(place, msku, offer, color, rids, regset, perk, rearr_flag, additional_params)
        )
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'product',
                    'id': int(msku.sku),
                    'offers': {
                        'items': [
                            {
                                'entity': 'offer',
                                'wareId': offer.waremd5,
                                'promos': self.create_promos_for_checkers(promos, offer, assortment_group)
                                if promos
                                else Absent(),
                            }
                        ]
                    },
                }
            ],
            allow_different_len=False,
        )
        if meta_promos:
            self.assertFragmentIn(response, self.create_meta_promos_for_checkers(meta_promos))
        if cashback_detail_groups:
            self.assertFragmentIn(response, self.create_cashback_detail_groups(cashback_detail_groups))

    def white_checker_promo_disabled(
        self, promos, msku, offer, place, color, rids, regset, perk, rearr_flag, additional_params
    ):
        response = self.report.request_json(
            self.build_white_request(place, msku, offer, color, rids, regset, perk, rearr_flag, additional_params)
        )
        # Блок promo должен отсутствовать
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'product',
                    'id': int(msku.sku),
                    'offers': {
                        'items': [
                            {
                                'entity': 'offer',
                                'wareId': offer.waremd5,
                                'promos': Absent(),
                            }
                        ]
                    },
                }
            ],
        )

    def white_offer_checker_promo_enabled(
        self,
        promos,
        msku,
        offer,
        place,
        color,
        rids,
        regset,
        perk,
        rearr_flag,
        opt_fields=[],
        meta_promos=[],
        assortment_group='default',
        cashback_detail_groups=[],
        additional_params=None,
    ):
        response = self.report.request_json(
            self.build_white_request(place, msku, offer, color, rids, regset, perk, rearr_flag, additional_params)
        )
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': offer.waremd5,
                    'promos': self.create_promos_for_checkers(promos, offer, assortment_group) if promos else Absent(),
                }
            ],
            allow_different_len=False,
        )
        if meta_promos:
            self.assertFragmentIn(response, self.create_meta_promos_for_checkers(meta_promos))
        if cashback_detail_groups:
            self.assertFragmentIn(response, self.create_cashback_detail_groups(cashback_detail_groups))

    def white_offer_checker_promo_disabled(
        self, promos, msku, offer, place, color, rids, regset, perk, rearr_flag, additional_params
    ):
        response = self.report.request_json(
            self.build_white_request(place, msku, offer, color, rids, regset, perk, rearr_flag, additional_params)
        )
        # Блок promo должен отсутствовать
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': offer.waremd5,
                    'promos': Absent(),
                }
            ],
        )

    def check_promo_impl(
        self,
        promos,
        msku,
        offer,
        checker,
        places,
        extra_rearrs,
        color,
        rids,
        regset,
        rearr_flags,
        perks,
        time,
        additional_params,
    ):
        combined_flags = [
            [(flag_name, flag_value) for flag_value in values] for flag_name, values in rearr_flags.items()
        ]
        for place in places:
            for perk, flags_combination in [
                (perk, flags_combination) for flags_combination in product(*combined_flags) for perk in perks
            ]:
                request_rearr_flags = {key: value for key, value in flags_combination if value is not None}
                request_rearr_flags.update(request_rearr_flags)
                if time is not None:
                    request_rearr_flags["market_promo_datetime"] = time
                if extra_rearrs:
                    request_rearr_flags.update(extra_rearrs)

                url_params = dict(
                    perk=perk,
                    rearr_flag=request_rearr_flags,
                    color=color,
                    place=place,
                    rids=rids,
                    regset=regset,
                    additional_params=additional_params,
                )
                checker(promos, msku, offer, url_params)

    def build_url_params(
        self,
        colors=(BLUE_COLOR, GREEN_COLOR, GREEN_WITH_BLUE_COLOR),
        perks=[
            cashback_enable_perks,
        ],
        rearr_flags={},
        time=None,
        extra_rearrs=None,
        rids=0,
        regset=2,
        additional_params=None,
    ):
        rearr_flags['market_metadoc_search'] = ['no']
        return dict(
            colors=colors,
            rearr_flags=rearr_flags,
            perks=perks,
            time=time,
            extra_rearrs=extra_rearrs,
            rids=rids,
            regset=regset,
            additional_params=additional_params,
        )

    def check_promo(
        self,
        promos,
        msku,
        offer,
        url_params,
        opt_fields={"tags": None},
        meta_promos=[],
        assortment_group='default',
        cashback_detail_groups=[],
    ):
        # проверка синей выдачи
        def checker(promos, msku, offer, url_params):
            return self.blue_checker_promo_enabled(
                promos,
                msku,
                offer,
                opt_fields=opt_fields,
                meta_promos=meta_promos,
                assortment_group=assortment_group,
                cashback_detail_groups=cashback_detail_groups,
                **url_params
            )

        # проверка белой помодельной выдачи
        def checker_white(promos, msku, offer, url_params):
            return self.white_checker_promo_enabled(
                promos,
                msku,
                offer,
                opt_fields=opt_fields,
                meta_promos=meta_promos,
                assortment_group=assortment_group,
                cashback_detail_groups=cashback_detail_groups,
                **url_params
            )

        # проверка белой поофферной выдачи
        def checker_white_offer(promos, msku, offer, url_params):
            return self.white_offer_checker_promo_enabled(
                promos,
                msku,
                offer,
                opt_fields=opt_fields,
                meta_promos=meta_promos,
                assortment_group=assortment_group,
                cashback_detail_groups=cashback_detail_groups,
                **url_params
            )

        self._check_promo_applier(promos, msku, offer, url_params, checker, checker_white, checker_white_offer)

    def check_promo_absent(self, promo, msku, offer, url_params):
        # проверка синей выдачи
        def checker(promo, msku, offer, url_params):
            return self.blue_checker_promo_disabled(promo, msku, offer, **url_params)

        # проверка белой помодельной выдачи
        def checker_white(promo, msku, offer, url_params):
            return self.white_checker_promo_disabled(promo, msku, offer, **url_params)

        # проверка белой поофферной выдачи
        def checker_white_offer(promo, msku, offer, url_params):
            return self.white_offer_checker_promo_disabled(promo, msku, offer, **url_params)

        self._check_promo_applier(promo, msku, offer, url_params, checker, checker_white, checker_white_offer)

    def _check_promo_applier(self, promo, msku, offer, urlparams, checker, checker_white, checker_white_offer):
        url_params = deepcopy(urlparams)
        colors = url_params["colors"]
        del url_params["colors"]
        for color in colors:
            url_params["color"] = color
            if color == BLUE_COLOR:
                self.check_promo_impl(promo, msku, offer, checker, BLUE_PLACES, **url_params)
                continue
            for (checker, places) in [(checker_white, WHITE_PLACES), (checker_white_offer, WHITE_OFFERS_PLACE)]:
                self.check_promo_impl(promo, msku, offer, checker, places, **url_params)

    def test_blue_cashback_active_flags_blue(self):
        params = self.build_url_params()
        self.check_promo([blue_cashback_1], msku_1, blue_offer_1, params)

    def test_blue_cashback_active_flags_white(self):
        params = self.build_url_params()
        self.check_promo([blue_cashback_1], msku_1, blue_offer_1, params)

    def test_blue_cashback_inactive(self):
        # обнуляем перки, даже с активным флагом в таком случае кэшбэк не должен быть показан
        params = self.build_url_params(
            perks=[
                cashback_disable_perks,
            ]
        )
        self.check_promo_absent([blue_cashback_1], msku_1, blue_offer_1, params)

        # Проверяем отключение акции по времени
        params = self.build_url_params(perks=[cashback_disable_perks, cashback_enable_perks])
        # акция уже закончилась
        self.check_promo_absent([blue_cashback_2], msku_2, blue_offer_2, params)
        # акция ещё не началась
        params = self.build_url_params(perks=[cashback_disable_perks, cashback_enable_perks])
        self.check_promo_absent([blue_cashback_3], msku_3, blue_offer_3, params)

    def test_cashback_priority(self):
        '''
        Проверяем, что, когда к офферу привязано несколько кэшбэков с разными приоритетами (собственными приоритетами кэшбэка)
        будет выбран кэшбэк с наименьшим значением приоритета
        '''
        # акция с маленьким приоритетом: начинается раньше
        params = self.build_url_params()
        self.check_promo([blue_cashback_4], msku_4, blue_offer_4, params)
        # проверяем, что когда акция с бОльшим приоритетом активна - будет выбрана именно она
        high_priority_cashback_promo_started = now + 2 * delta_small
        params = self.build_url_params(time=high_priority_cashback_promo_started.isoformat())
        self.check_promo([blue_cashback_5], msku_4, blue_offer_4, params)
        # проверяем, что когда акция с бОльшим приоритетом закончится - на выдаче снова появится акция с меньшим приоритетом
        high_priority_cashback_promo_ended = now + delta_big + delta_small
        params = self.build_url_params(time=high_priority_cashback_promo_ended.isoformat())
        self.check_promo([blue_cashback_4], msku_4, blue_offer_4, params)

    def test_predicates(self):
        '''
        Проверяем что акции кешбека с предикатами фильтруются как надо под флагом
        Сейчас у нас есть только 1 способ фильтрации: по перкам
        '''

        # оффер blue_offer_with_extra_cb лежит на складе 123, и к нему привязан бакет 777
        # со склада 123 идет доставка только службой 100
        # следовательно наш оффер имеет "deliveryPartnerTypes" == ["YANDEX_MARKET"] и кб мы ему показываем
        # если остальные условия соблюдены

        # есть перк и флаг - есть повышенный КБ
        params = self.build_url_params(
            perks=[
                cashback_extra_enable_perks,
            ],
            rids=213,
        )
        self.check_promo([blue_cashback_7], msku_with_extra_cb, blue_offer_with_extra_cb, params)

        # нет перки но есть флаг - нет повышенного КБ
        params = self.build_url_params(
            perks=[
                cashback_extra_disable_perks,
            ],
            rids=213,
        )
        self.check_promo_absent([blue_cashback_7], msku_with_extra_cb, blue_offer_with_extra_cb, params)

    def test_cashback_for_employees(self):
        '''
        Проверяем что значения перка повышенного кешбэка для сотрудников парсятся и учитываются.
        Сама механика перков проверяется в test_predicates
        '''

        # перк повышенного кешбэка для сотрудников есть
        # промка должна быть показана
        params = self.build_url_params(
            perks=[
                cashback_extra_employee_enable_perks,
            ],
        )
        self.check_promo(
            [blue_cashback_for_employees], msku_4_employees_cashback, blue_offer_4_employees_cashback, params
        )

        # перка повышенного кешбэка для сотрудников нет
        # промка не должна быть показана
        params = self.build_url_params(
            perks=[cashback_extra_enable_perks, cashback_extra_disable_perks],
        )
        self.check_promo_absent(
            [blue_cashback_for_employees], msku_4_employees_cashback, blue_offer_4_employees_cashback, params
        )

    def test_cashback_max_cashback_price(self):
        '''
        Проверяем что офера с кешбеком у которого есть поле max_offer_cashback, рисуют кешбек не больше чем max_offer_cashback
        '''

        # у офера blue_offer_with_max_offer_cashback должен насчитаться по процентам кешбек 330 рублей
        # но так как ограничение кб 300 рублей - мы нарисовать должны 300 рублей
        params = self.build_url_params(
            perks=[
                cashback_enable_perks,
            ],
        )
        self.check_promo([blue_cashback_8], msku_with_max_offer_cashback, blue_offer_with_max_offer_cashback, params)

        # у офера blue_offer_with_max_offer_cashback_cheap должен насчитаться по процентам кешбек 33 рубля
        # но тут все ок - это меньше чем ограничение в 300 рублей
        params = self.build_url_params(
            perks=[
                cashback_enable_perks,
            ],
        )
        self.check_promo(
            [blue_cashback_8], msku_with_max_offer_cashback_cheap, blue_offer_with_max_offer_cashback_cheap, params
        )

    def test_extra_cashback_filter(self):
        '''
        Проверяем что КБ с перкой 'yandex_extra_cashback' не показывается на оферах у которых нет опций
        доставки не 99 (SHOP_CARRIER_ID) партнером
        '''

        # оффер blue_offer_with_extra_cb_shop_carrier лежит на складе 124, и к нему привязан бакет 888
        # со склада 124 идет доставка только службой 99 aka DeliveryOption.SHOP_CARRIER_ID
        # следовательно наш оффер имеет "deliveryPartnerTypes" == ["SHOP"] и кб мы ему не показываем
        params = self.build_url_params(
            perks=[
                cashback_extra_enable_perks,
            ],
            rids=213,
        )
        self.check_promo_absent(
            [blue_cashback_7], msku_with_extra_cb_shop_carrier, blue_offer_with_extra_cb_shop_carrier, params
        )

    def test_extra_cashback_priority(self):
        '''
        Проверяем что КБ с перкой 'yandex_extra_cashback' имеет приортет выше чем обычный 'yandex_cashback'
        и хорошо дружит с фильтрами
        '''

        # оффер blue_offer_with_extra_and_normal_cb_shop_carrier имеет "deliveryPartnerTypes" == ["SHOP"] и
        # два кешбека: обычный blue_cashback_4 и повышенный blue_cashback_7
        # повышенный кб мы ему не должны показывать так что покажем обычный
        params = self.build_url_params(
            perks=[
                cashback_extra_enable_perks,
            ],
            rids=213,
        )
        self.check_promo(
            [blue_cashback_4],
            msku_with_extra_and_normal_cb_shop_carrier,
            blue_offer_with_extra_and_normal_cb_shop_carrier,
            params,
        )

        # оффер blue_offer_with_extra_and_normal_cb имеет "deliveryPartnerTypes" == ["YANDEX_MARKET"] и
        # два кешбека: обычный blue_cashback_4 и повышенный blue_cashback_7
        # мы ему должны показать повышенный потому что у него приоритет выше (1 vs 2)
        params = self.build_url_params(
            perks=[
                cashback_extra_enable_perks,
            ],
            rids=213,
        )
        self.check_promo([blue_cashback_7], msku_with_extra_and_normal_cb, blue_offer_with_extra_and_normal_cb, params)

        # оффер blue_offer_with_extra_and_normal_cb имеет "deliveryPartnerTypes" == ["YANDEX_MARKET"] и
        # два кешбека: обычный blue_cashback_4 и повышенный blue_cashback_7
        # мы ему должны показать пониженный потому что перки юзера не позволяют показать повышенный
        params = self.build_url_params(
            perks=[
                cashback_enable_perks,
            ],
            rids=213,
        )
        self.check_promo([blue_cashback_4], msku_with_extra_and_normal_cb, blue_offer_with_extra_and_normal_cb, params)

    def test_personal_cashback_priority(self):
        # оффер blue_offer_personal_cb имеет три акции (приоритеты перевёрнуты, пока лоялти не станет выгружать по нормальной шкале):
        # * blue_cashback_norm - простой кэшбек, приоритет -30, в блоке blue_cashback
        # * blue_cashback_personal - персональный, приоритет -20, задан в same_type_priority
        # * blue_cashback_extra - повышенный, приоритет -10, в блоке blue_cashback
        for top_promo, need_block in (
            (blue_cashback_extra, []),  # по-умолчанию выигрывает повышенный (prio -10)
            (blue_cashback_personal, [blue_cashback_extra]),  # если отключить повышенный, то выиграет персональный
            (
                blue_cashback_norm,
                [blue_cashback_extra, blue_cashback_personal],
            ),  # если отключить повышенный и персональный, то останется только обычный
        ):
            params = self.build_url_params(
                perks=[
                    cashback_enable_perks,
                ],
                extra_rearrs={
                    'block_shop_promo_id': ','.join([str(promo.shop_promo_id) for promo in need_block]),
                    'personal_promo_enabled': 1,
                },
            )
            self.check_promo([top_promo], msku_personal_cb, blue_offer_personal_cb, params)

    def test_3p_extra_cashback(self):
        '''
        Проверяем что КБ для 3p партнёров:
        * имеет приоритет выше чем обычный повышенный кешбэк
        * не назначается FF товарам
        * не назначается товарам партнёров не участвующих в программе лояльности
        * не приходит без extra_cashback

        '''

        # оффер blue_offer_with_extra_cb_shop_carrier_3p_cb имеет "deliveryPartnerTypes" == ["SHOP"] поэтому
        # повышенный кешбэк неприменим
        params = self.build_url_params(
            perks=[
                cashback_extra_enable_perks,
            ],
            extra_rearrs={'market_cashback_predicates_enable': '1'},
            rids=213,
        )
        self.check_promo_absent(
            [blue_cashback_9],
            msku_with_extra_cb_shop_carrier_3p_cb,
            blue_offer_with_extra_cb_shop_carrier_3p_cb,
            params,
        )

        # оффер msku_with_extra_3p_cb имеет "deliveryPartnerTypes" == ["YANDEX_MARKET"] и
        # два кешбека: повышенный и повышенный для 3p  и обычный повышенный кешбэк
        # должен быть выбран кешбэк для 3p
        params = self.build_url_params(
            perks=[
                cashback_extra_enable_perks,
            ],
            extra_rearrs={'market_cashback_predicates_enable': '1'},
            rids=213,
        )
        self.check_promo([blue_cashback_9], msku_with_extra_3p_cb, blue_offer_with_extra_3p_cb, params)

        # оффер msku_ff_with_extra_3p_cb_and_plain не соответствует условию atSupplierWarehouse
        # поэтом кешбэк должен быть отброшен
        params = self.build_url_params(
            perks=[
                cashback_extra_enable_perks,
            ],
            extra_rearrs={'market_cashback_predicates_enable': '1', 'get_rid_of_direct_shipping': '0'},
            rids=213,
        )
        self.check_promo(
            [blue_cashback_7], msku_ff_with_extra_3p_cb_and_plain, blue_ff_offer_with_extra_3p_cb_and_plain, params
        )

    def test_promo_type_counter(self):
        # всего в тесте 42 оффера
        # +1 фейковый оффер из CPC коллекции
        req = 'place=stat_numbers&pp=18&regset=0'
        response = self.report.request_json(req)
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'offersCount': 42,
                },
            },
            allow_different_len=False,
        )

        # если включить выборку офферов по промо-типу BLUE_CASHBACK, то отвалятся ещё 3 оффера
        #   blue_offer_2 - закончившаяся акция
        #   blue_offer_3 - ещё не начавшаяся акция
        #   blue_offer_no_promo - отбрасывается, т.к. на нём нет промо типа BLUE_CASHBACK
        # если фильтровать по поисковому литералу, то статусы акций не учитываются и офферы попадут в stat_numbers
        req = 'place=stat_numbers&pp=18&regset=0&promo-type={}&rearr-factors=enable_promo_type_literal={}'
        for promo_type_literal in (0, 1):
            expected_offers_count = 38 if promo_type_literal == 0 else 40
            response = self.report.request_json(req.format(PromoType.BLUE_CASHBACK, promo_type_literal))
            self.assertFragmentIn(
                response,
                {
                    'result': {
                        'offersCount': expected_offers_count,
                    },
                },
                allow_different_len=False,
            )

    def test_get_cashback_offers_by_promo_type(self):
        '''
        проверяем возможность получить оффера с кешбэком по литералам
        '''
        # если у вас упал этот тест - проверьте должен ли ваш оффер с кешбэком попадать в поиск по
        # кешбэчному литералу и если должен добавьте его в один из списков (или в оба, в зависимости от условий доставки)
        req = 'place=prime&pp=18&promo-type=blue-cashback&rgb=blue&numdoc=100&perks=' + ','.join(cashback_all_perks)

        def template(offers, offer):
            return {
                "results": [
                    {
                        "offers": {
                            "items": [
                                {
                                    "entity": "offer",
                                    "wareId": offer.waremd5,
                                    "promos": [
                                        {
                                            "type": "blue-cashback",
                                        }
                                    ],
                                }
                            ]
                        }
                    }
                ]
            }

        # список составляется из позитивных кейсов разных тестов
        # оффера из негативных кейсов кешбэка на выдаче не имеют -> не встречаются на выдаче
        cashback_offers = [
            # test_blue_cashback_active_flags_blue
            blue_offer_1,
            # test_cashback_priority
            blue_offer_4,
            blue_offer_6,
            blue_offer_7,
            blue_offer_8,
            # test_cashback_for_employees
            blue_offer_4_employees_cashback,
            # test_cashback_max_cashback_price
            blue_offer_with_max_offer_cashback,
            blue_offer_with_max_offer_cashback_cheap,
            blue_offer_crossdock,
            # test_dsbs_cashback
            offer_dsbs,
            # test_cashback_without_default_flag
            blue_offer_cb_no_predicates,
            # test_extra_cashback_priority
            blue_offer_with_extra_and_normal_cb,
            blue_offer_with_extra_and_normal_cb_shop_carrier,
            # test_personal_cashback_priority
            blue_offer_personal_cb,
            blue_offer_cashback_value,
            blue_offer_cashback_value_with_max_thresholds,
            # test_personal_compensation
            blue_offer_personal_compensation,
            offer_dsbs_diy_assortment_group,
            offer_dsbs_cehac_assortment_group,
            offer_dsbs_default_assortment_group,
            offer_with_two_small_cashbacks,
            offer_for_detail_groups_test,
            offer_for_detail_groups_sorting_test,
        ]
        response = self.report.request_json(req)
        for offer in cashback_offers:
            self.assertFragmentIn(response, template(cashback_offers, offer))
        self.assertFragmentIn(response, {"total": len(cashback_offers)})

        cashback_offers_rids = [
            # test_dsbs_cashback
            offer_dsbs,
            offer_dsbs_with_prepayment,
            # test_predicate_enhancement
            blue_offer_cb_shop_all,
            blue_offer_cb_shop_dt,
            blue_ff_offer_with_extra_3p_cb_and_plain,
            blue_offer_cb_shop_dt_with_all,
            blue_offer_cb_market_dt_with_all,
            blue_offer_extra_cb_market_dt,
            # test_predicates
            blue_offer_with_extra_cb,
            # test_3p_extra_cashback
            blue_offer_with_extra_3p_cb,
            # test_extra_cashback_priority
            blue_offer_with_extra_and_normal_cb,
            blue_offer_with_extra_and_normal_cb_shop_carrier,
            blue_offer_cashback_value_with_max_thresholds,
            # test_click_n_collect_cashback
            blue_offer_cnc_from_medicine_hid,
            offer_dsbs_diy_assortment_group,
            offer_dsbs_cehac_assortment_group,
            offer_dsbs_default_assortment_group,
            offer_with_two_small_cashbacks,
            offer_for_detail_groups_test,
            offer_for_detail_groups_sorting_test,
        ]
        response = self.report.request_json(req + "&rids=213")
        for offer in cashback_offers_rids:
            self.assertFragmentIn(response, template(cashback_offers_rids, offer))
        self.assertFragmentIn(response, {"total": len(cashback_offers_rids)})

    def test_predicate_enhancement(self):
        '''
        Проверяем два оффера с "противоположыми" дефолтам условиям привязки.
        Экстра кешбэк по-умолчанию требует delivery_partner_type MARKET (2). В тесте у повышенного кешбэка перегружаем на 1
        Обычный кешбэк на delivery_partner_type оффера не смотрит. В тесте у него перегружаем на требование delivery_partner_type MARKET 2
        '''

        values = {
            "perks": [
                cashback_extra_enable_perks,
            ],
            "rids": 213,
        }

        # проверяем, что при определённом поведении (с подгрузкой delivery_partner_type предиката) extra_cashback
        # НЕ будет применяться к офферу с delivery_partner_type=MARKET, будет показан обычный кешбэк
        params = self.build_url_params(**values)
        self.check_promo(
            [blue_cashback_with_delivery_partner_type], msku_extra_cb_market_dt, blue_offer_extra_cb_market_dt, params
        )

        # проверяем, что при определённом поведении (с подгрузкой delivery_partner_type предиката) обычный кешбэк
        # будет применяться к офферу с delivery_partner_type=SHOP (extra_cashback будет отфильтрован по дефолтному поведению)
        params = self.build_url_params(**values)
        self.check_promo([blue_cashback_extra_no_delivery_partner_type], msku_cb_shop_dt, blue_offer_cb_shop_dt, params)

        # проверяем, что при определённом поведении (с подгрузкой delivery_partner_type предиката)
        # Промо требующее SHOP|MARKET в delivery_partner_types будет привязано к офферу с SHOP|MARKET
        params = self.build_url_params(**values)
        self.check_promo(
            [blue_cashback_with_all_delivery_partner_types], msku_cb_all_dt, blue_offer_cb_shop_all, params
        )

        # проверяем, что при определённом поведении (с подгрузкой delivery_partner_type предиката)
        # Промо требующе SHOP в delivery_partner_types будет привязано к офферу с SHOP|MARKET
        params = self.build_url_params(**values)
        self.check_promo(
            [blue_cashback_extra_no_delivery_partner_type],
            msku_cb_shop_dt_with_all,
            blue_offer_cb_shop_dt_with_all,
            params,
        )

        # проверяем, что при определённом поведении (с подгрузкой delivery_partner_type предиката)
        # Промо требующе MARKET в delivery_partner_types будет привязано к офферу с SHOP|MARKET
        params = self.build_url_params(**values)
        self.check_promo(
            [blue_cashback_with_delivery_partner_type],
            msku_cb_market_dt_with_all,
            blue_offer_cb_market_dt_with_all,
            params,
        )

        # проверяем, что промо требующе SHOP|MARKET в delivery_partner_types
        # НЕ будет применяться к офферу с delivery_partner_type=MARKET
        params = self.build_url_params(**values)
        self.check_promo_absent(
            [blue_cashback_with_all_delivery_partner_types],
            msku_extra_cb_market_all_dt,
            blue_offer_extra_cb_market_all_dt,
            params,
        )

        # проверяем, что промо требующе SHOP|MARKET в delivery_partner_types
        # НЕ будет применяться к офферу с delivery_partner_type=shop
        params = self.build_url_params(**values)
        self.check_promo_absent(
            [blue_cashback_with_all_delivery_partner_types], msku_cb_shop_all_dt, blue_offer_cb_shop_all_dt, params
        )

    def test_dsbs_cashback(self):
        # проверяем кешбэк на dsbs офферах
        values = {
            "perks": [
                cashback_extra_enable_perks,
            ],
            "extra_rearrs": {'market_cashback_predicates_enable': '1'},
        }
        params = self.build_url_params(**values)
        self.check_promo([blue_cashback_for_dsbs], msku_dsbs, offer_dsbs, params)

    def test_click_n_collect_cashback(self):
        # проверяем кешбэк на click-n-collect офферах
        values = {
            "perks": [
                cashback_extra_enable_perks,
            ],
            "extra_rearrs": {'market_cashback_predicates_enable': '1'},
            "rids": "213",
        }
        # на медицинских товарах кешбек появляется
        params = self.build_url_params(**values)
        self.check_promo([blue_cashback_for_dsbs], msku_cnc_medicine, blue_offer_cnc_from_medicine_hid, params)

        # на click-n-collect алкоголе кешбека быть не должно
        params = self.build_url_params(**values)
        self.check_promo_absent([blue_cashback_for_dsbs], msku_cnc_alco, blue_offer_cnc_from_alco_hid, params)

    def test_cashback_without_default_flag(self):
        '''
        проверяем, что флаг market_cashback_without_default_perk убирает дефолтное требование на
        перк PERK_YANDEX_CASHBACK для кешбэка
        '''
        values = {
            "rearr_flags": {
                "market_cashback_without_default_perk": [0, None],
            },
            "perks": [
                cashback_disable_perks,
            ],
        }
        params = self.build_url_params(**values)
        self.check_promo_absent([blue_cashback_1], msku_1, blue_offer_1, params)
        self.check_promo_absent([blue_cb_no_predicates], msku_cb_no_predicates, blue_offer_cb_no_predicates, params)
        values["rearr_flags"]["market_cashback_without_default_perk"] = [1]
        values["perks"] = [cashback_disable_perks, cashback_enable_perks]
        params = self.build_url_params(**values)
        self.check_promo([blue_cashback_1], msku_1, blue_offer_1, params)
        self.check_promo([blue_cb_no_predicates], msku_cb_no_predicates, blue_offer_cb_no_predicates, params)

    def test_promo_cashback_meta_promo(self):
        '''
        проверяем, что из кешбеков с одинаковым promo_bucket_name выбирается самый приоритетный,
        остальные откидываются
        после этого все оставшиеся на оффере кешбеки, у которых есть promo_bucket_name, обьединяются в метапромо
        '''
        values = {
            "rearr_flags": {"block_cashback_from_partner_source": [1]},
            "perks": [
                cashback_extra_enable_perks,
            ],
        }
        params = self.build_url_params(**values)
        # в метапромо будет default_cashback_high_priority от бакета default и extra_cashback от бакета extra
        # кешбек cashback_with_empty_bucket не будет на выдаче
        meta_promo = {
            'id': 'cashback-value',
            'promoKeys': [default_cashback_high_priority.key, extra_cashback.key],
            'value': self.calc_cashback_value(
                blue_offer_cashback_value.price,
                default_cashback_high_priority.blue_cashback.share + extra_cashback.blue_cashback.share,
            ),
            'tags': list(
                set(
                    self.calc_cashback_tags(
                        default_cashback_high_priority.tags, default_cashback_high_priority.blue_cashback, 'default'
                    )
                    + self.calc_cashback_tags(extra_cashback.tags, extra_cashback.blue_cashback, 'default')
                )
            ),
            'cms_description_semantic_id': 'extra-cashback-id',
        }
        self.check_promo(
            [default_cashback_high_priority, extra_cashback],
            msku_cashback_value,
            blue_offer_cashback_value,
            params,
            meta_promos=[meta_promo],
        )

    def test_promo_cashback_meta_promo_with_blocked_promo(self):
        '''
        проверяем, что мета-промо считается корректно при использовании флага
        block_shop_promo_id для самого приоритетного промо
        '''
        values = {
            "rearr_flags": {
                "block_cashback_from_partner_source": [1],
                "block_shop_promo_id": [default_cashback_high_priority.shop_promo_id],
            },
            "perks": [
                cashback_extra_enable_perks,
            ],
        }
        params = self.build_url_params(**values)
        # в метапромо не будет default_cashback_high_priority от бакета default, тк он заблокирован
        # будет взят следующий по приоритету - default_cashback_low_priority_2 и extra_cashback от бакета extra
        meta_promo = {
            'id': 'cashback-value',
            'promoKeys': [default_cashback_low_priority_2.key, extra_cashback.key],
            'value': self.calc_cashback_value(
                blue_offer_cashback_value.price,
                default_cashback_low_priority_2.blue_cashback.share + extra_cashback.blue_cashback.share,
            ),
            'tags': list(
                set(
                    self.calc_cashback_tags(
                        default_cashback_low_priority_2.tags, default_cashback_low_priority_2.blue_cashback, 'default'
                    )
                    + self.calc_cashback_tags(extra_cashback.tags, extra_cashback.blue_cashback, 'default')
                )
            ),
            'cms_description_semantic_id': 'extra-cashback-id',
        }
        self.check_promo(
            [default_cashback_low_priority_2, extra_cashback],
            msku_cashback_value,
            blue_offer_cashback_value,
            params,
            meta_promos=[meta_promo],
        )

    def test_random_promo_bucket_name_in_meta_promo(self):
        '''
        Проверяем, что весь кешбек с любым значением promo_bucket_name,
        кроме кешбека от платежных систем просуммируется в мета промо cashback-value
        '''
        values = {
            "perks": [
                cashback_extra_enable_perks,
            ],
        }
        meta_promo = {
            'id': 'cashback-value',
            'promoKeys': [
                blue_cashback_for_dsbs.key,
            ],
            'value': self.calc_cashback_value(offer_dsbs.price, blue_cashback_for_dsbs.blue_cashback.share),
            'tags': list(
                self.calc_cashback_tags(blue_cashback_for_dsbs.tags, blue_cashback_for_dsbs.blue_cashback, 'default')
            ),
        }
        params = self.build_url_params(**values)
        self.check_promo([blue_cashback_for_dsbs], msku_dsbs, offer_dsbs, params, meta_promos=[meta_promo])

    def test_block_cashback_from_partner_source(self):
        '''
        проверяем, что кешбек считается корректно при использовании флага
        block_cashback_from_partner_source, кешбек из ПИ при нем не применяется

        также проверяем, что cms_description_semantic_id прокидывается на выдачу из PromoDetails как есть
        для всех кешбеков, кроме партнерских, для них:
            для повышенных кешбеков будет значение partner-extra-cashback
            для обычных кешбеков будет значение partner-default-cashback

        для мета промо значение выбирается как значение самого максимального по номиналу кешбека,
        входящего в это мета промо
        '''
        values = {
            "rearr_flags": {
                "block_cashback_from_partner_source": [1],
            },
            "perks": [
                cashback_extra_enable_perks,
            ],
        }
        params = self.build_url_params(**values)
        # в метапромо не будет cashback_from_partner_source от бакета extra, тк он заблокирован
        # будет взят следующий по приоритету - extra_cashback
        meta_promo = {
            'id': 'cashback-value',
            'promoKeys': [default_cashback_high_priority.key, extra_cashback.key],
            'value': self.calc_cashback_value(
                blue_offer_cashback_value.price,
                default_cashback_high_priority.blue_cashback.share + extra_cashback.blue_cashback.share,
            ),
            'tags': list(
                set(
                    self.calc_cashback_tags(
                        default_cashback_high_priority.tags, default_cashback_high_priority.blue_cashback, 'default'
                    )
                    + self.calc_cashback_tags(extra_cashback.tags, extra_cashback.blue_cashback, 'default')
                )
            ),
            # в метапромо будет значение extra-cashback-id от extra_cashback
            'cms_description_semantic_id': 'extra-cashback-id',
        }
        self.check_promo(
            [default_cashback_high_priority, extra_cashback],
            msku_cashback_value,
            blue_offer_cashback_value,
            params,
            meta_promos=[meta_promo],
        )

        values['rearr_flags']['block_cashback_from_partner_source'] = [0]
        # без флага начинает применяться cashback_from_partner_source
        params = self.build_url_params(**values)
        meta_promo = {
            'id': 'cashback-value',
            'promoKeys': [default_cashback_high_priority.key, cashback_from_partner_source.key],
            'value': self.calc_cashback_value(
                blue_offer_cashback_value.price,
                default_cashback_high_priority.blue_cashback.share + cashback_from_partner_source.blue_cashback.share,
            ),
            'tags': list(
                set(
                    self.calc_cashback_tags(
                        default_cashback_high_priority.tags, default_cashback_high_priority.blue_cashback, 'default'
                    )
                    + self.calc_cashback_tags(
                        cashback_from_partner_source.tags, cashback_from_partner_source.blue_cashback, 'default'
                    )
                )
            ),
            # в метапромо будет значение partner-extra-cashback от cashback_from_partner_source
            'cms_description_semantic_id': 'partner-extra-cashback',
        }
        self.check_promo(
            [default_cashback_high_priority, cashback_from_partner_source],
            msku_cashback_value,
            blue_offer_cashback_value,
            params,
            meta_promos=[meta_promo],
        )

    def test_block_cashback_from_partner_source_without_yandex_plus(self):
        '''
        проверяем, что кешбек из ПИ не применяется без перка yandex_plus,
        если включен флаг block_cashback_from_partner_source_without_yandex_plus
        '''
        test_data = [
            (
                {
                    "block_cashback_from_partner_source_without_yandex_plus": [1],
                },
                cashback_without_yandex_plus,
                False,
            ),
            (
                {
                    "block_cashback_from_partner_source_without_yandex_plus": [1],
                },
                cashback_all_perks,
                True,
            ),
            (
                {
                    "exp_perks": ["yandex_plus"],
                    "block_cashback_from_partner_source_without_yandex_plus": [1],
                },
                cashback_without_yandex_plus,
                True,
            ),
            (
                {
                    "block_cashback_from_partner_source_without_yandex_plus": [0],
                },
                cashback_without_yandex_plus,
                True,
            ),
            (
                {
                    "block_cashback_from_partner_source_without_yandex_plus": [0],
                },
                cashback_all_perks,
                True,
            ),
        ]

        for rearr_flags, perks, enable_cashback_prom_ps in test_data:
            values = {
                "rearr_flags": rearr_flags,
                "perks": [perks],
            }
            params = self.build_url_params(**values)
            if enable_cashback_prom_ps:
                self.check_promo(
                    [default_cashback_high_priority, cashback_from_partner_source],
                    msku_cashback_value,
                    blue_offer_cashback_value,
                    params,
                )

            else:
                self.check_promo(
                    [default_cashback_high_priority, extra_cashback],
                    msku_cashback_value,
                    blue_offer_cashback_value,
                    params,
                )

    def test_promo_cashback_required_buckets(self):
        '''
        проверяем, что при отсуствии кешбека сегмента default
        кешбек сегмента extra не появляется
        '''
        all_default_cashback = [
            default_cashback_high_priority,
            default_cashback_low_priority_1,
            default_cashback_low_priority_2,
        ]
        values = {
            "rearr_flags": {"block_cashback_from_partner_source": [1]},
            "perks": [
                cashback_extra_enable_perks,
            ],
        }
        params = self.build_url_params(**values)
        meta_promo = {
            'id': 'cashback-value',
            'promoKeys': [default_cashback_high_priority.key, extra_cashback.key],
            'value': self.calc_cashback_value(
                blue_offer_cashback_value.price,
                default_cashback_high_priority.blue_cashback.share + extra_cashback.blue_cashback.share,
            ),
            'tags': list(
                set(
                    self.calc_cashback_tags(
                        default_cashback_high_priority.tags, default_cashback_high_priority.blue_cashback, 'default'
                    )
                    + self.calc_cashback_tags(extra_cashback.tags, extra_cashback.blue_cashback, 'default')
                )
            ),
            'cms_description_semantic_id': 'extra-cashback-id',
        }
        # с активным кешбеком сегмента default есть кешбек из сегмента extra
        self.check_promo(
            [default_cashback_high_priority, extra_cashback],
            msku_cashback_value,
            blue_offer_cashback_value,
            params,
            meta_promos=[meta_promo],
        )
        # выключаем все кешбеки сегмента default
        values["extra_rearrs"] = {
            'block_shop_promo_id': ','.join([str(promo.shop_promo_id) for promo in all_default_cashback])
        }
        params = self.build_url_params(**values)
        # на оффере вообще не остается промо и нет метапромо
        self.check_promo_absent([extra_cashback], msku_cashback_value, blue_offer_cashback_value, params)

    def test_cashback_partner_id_and_thresholds(self):
        '''
        проверяем, что поля partner_info и трешхолды заполняются корректно
        '''
        values = {
            "perks": [
                cashback_enable_perks,
            ],
        }
        meta_promo = {
            'id': 'cashback-value',
            'promoKeys': [cashback_with_threshholds.key],
            'value': 1,  # для cashback_with_threshholds минимальное значение порога - 1
            'tags': ['extra-cashback'],
        }
        params = self.build_url_params(**values)
        self.check_promo([cashback_with_threshholds], msku_6, blue_offer_6, params, meta_promos=[meta_promo])

    def test_max_cashback_value(self):
        '''
        проверяем, что максимально значение для кешбека берется как миниум из
        явно заданного значения в промо и всех значений в max_offer_thresholds
        '''
        values = {
            "perks": [
                cashback_enable_perks,
            ],
        }
        meta_promo = {
            'id': 'cashback-value',
            'promoKeys': [cashback_with_max_value_from_promo.key, cashback_with_max_value_from_threshold.key],
            'value': 499,
            # для cashback_with_max_value_from_threshold минимальное значение порога - 199 +
            # для cashback_with_max_value_from_promo минимальное значение порога - 300
            'tags': ['extra-cashback'],
        }
        params = self.build_url_params(**values)
        self.check_promo(
            [cashback_with_max_value_from_promo, cashback_with_max_value_from_threshold],
            msku_7,
            blue_offer_7,
            params,
            meta_promos=[meta_promo],
        )

    def test_max_cashback_value_meta_promo(self):
        '''
        проверяем, что для метапромо корректно считается значение
        стоимость товара - 10000

        cashback_with_max_value_1 с share 20% и максимальным кешбеком 500 принесет 500 баллов
        cashback_with_max_value_2 с share 10% и максимальным кешбеком 7000 принесет 1000 баллов
        cashback_pharma c share 20% принесет 2000 баллов

        если суммировать share, результат будет 40% - 4000, но в сумме будут учтены максимальные значения
        для обоих кешбеков и сумма будет 3500

        '''
        values = {
            "rearr_flags": {"market_promo_meta_payment_system_cashback": [0]},
            "perks": [
                cashback_enable_perks,
            ],
            "rids": 213,
        }
        params = self.build_url_params(**values)
        meta_promo = {
            'id': 'cashback-value',
            'promoKeys': [cashback_with_max_value_1.key, cashback_with_max_value_2.key, cashback_pharma.key],
            'value': 3500,
            'tags': ['extra-cashback'],
        }
        # это не относится к проверяемому мета промо, но нужно для проверки всех кешбеков на товаре
        meta_promo_mastercard = {
            'id': 'cashback-payment-system-value',
            'promoKeys': [cashback_mastercard_high_priority.key],
            'value': 1600,
            'tags': ['extra-cashback'],
        }

        self.check_promo(
            [cashback_with_max_value_1, cashback_with_max_value_2, cashback_mastercard_high_priority, cashback_pharma],
            msku_cashback_value_with_max_thresholds,
            blue_offer_cashback_value_with_max_thresholds,
            params,
            meta_promos=[meta_promo, meta_promo_mastercard],
        )

    def test_personal_compensation(self):
        values = {
            "rearr_flags": {
                "personal_promo_direct_discount_enabled": [1],
            },
            "perks": [cashback_enable_perks],
        }
        params = self.build_url_params(**values)

        self.check_promo(
            [blue_cashback_personal_compensation], msku_personal_compensation, blue_offer_personal_compensation, params
        )

        values["perks"][0] += ['someperk']
        params = self.build_url_params(**values)

        self.check_promo_absent(
            [blue_cashback_personal_compensation], msku_personal_compensation, blue_offer_personal_compensation, params
        )

    def test_cashback_payment_systems(self):
        '''
        проверяем, что кешбек от мастеркарда с сегментом payment_system не суммируется в айтем 'cashback-value',
        а выделяется в promoCollections в отдельный айтем 'cashback-payment-system-value'
        '''
        values = {
            "perks": [
                cashback_enable_perks,
            ],
            "rids": 213,
        }

        meta_promo_mastercard = {
            'id': 'cashback-payment-system-value',
            'promoKeys': [cashback_mastercard_high_priority.key],
            'value': 1600,
            'tags': ['extra-cashback'],
        }
        meta_promo = {
            'id': 'cashback-value',
            'promoKeys': [cashback_with_max_value_1.key, cashback_with_max_value_2.key, cashback_pharma.key],
            'value': 3500,
            'tags': ['extra-cashback'],
        }
        params = self.build_url_params(**values)
        self.check_promo(
            [cashback_with_max_value_1, cashback_with_max_value_2, cashback_mastercard_high_priority, cashback_pharma],
            msku_cashback_value_with_max_thresholds,
            blue_offer_cashback_value_with_max_thresholds,
            params,
            meta_promos=[meta_promo, meta_promo_mastercard],
        )

    def test_cashback_payment_systems_with_cash_only_shops(self):
        '''
        проверяем, что кешбек от платежных систем применяется на оффер только если у него есть
        возможность предоплаты
        '''
        values = {
            "perks": [
                cashback_enable_perks,
            ],
            "rids": 213,
            "regset": 1,
        }
        params = self.build_url_params(**values)
        # у офферов магазина с предоплатой кешбек от мастеркарда есть
        self.check_promo(
            [cashback_mastercard_high_priority],
            msku_with_prepayment_dsbs,
            offer_dsbs_with_prepayment,
            params,
        )
        # у офферов магазина без предоплаты кешбека от мастеркарда нет
        self.check_promo_absent(
            [cashback_mastercard_high_priority], msku_without_prepayment_dsbs, offer_dsbs_without_prepayment, params
        )
        # у синих товаров тоже есть предоплата и кешбек от мастеркарда, потому что для виртуального магазина
        # настроена предоплата (в бакетах нет)
        self.check_promo(
            [cashback_with_max_value_1, cashback_with_max_value_2, cashback_mastercard_high_priority, cashback_pharma],
            msku_cashback_value_with_max_thresholds,
            blue_offer_cashback_value_with_max_thresholds,
            params,
        )

    def test_extra_cashback_tag(self):
        '''
        проверяем, что тег повышенного кешбека 'extra-cashback'
        генерируется по категории оффера и проценту кешбека и игнорируется из промо детали
        '''
        values = {
            "perks": [
                cashback_enable_perks,
            ],
            "rids": 213,
        }
        test_data = [
            (
                small_cashback_for_dsbs_cehac,
                big_cashback_for_dsbs_cehac,
                offer_dsbs_cehac_assortment_group,
                msku_dsbs_cehac,
                'cehac',
            ),
            (
                small_cashback_for_dsbs_diy,
                big_cashback_for_dsbs_diy,
                offer_dsbs_diy_assortment_group,
                msku_dsbs_diy,
                'diy',
            ),
            (
                small_cashback_for_dsbs_default,
                big_cashback_for_dsbs_default,
                offer_dsbs_default_assortment_group,
                msku_dsbs_default,
                'default',
            ),
        ]
        params = self.build_url_params(**values)
        for small_cashback, big_cashback, offer, msku, assortment_group in test_data:
            meta_promo = {
                'id': 'cashback-value',
                'promoKeys': [small_cashback.key, big_cashback.key],
                'value': self.calc_cashback_value(
                    offer.price,
                    small_cashback.blue_cashback.share + big_cashback.blue_cashback.share,
                ),
                'tags': ['extra-cashback'],
            }
            if assortment_group == 'default':
                meta_promo['cms_description_semantic_id'] = 'partner-extra-cashback'
            params = self.build_url_params(**values)
            self.check_promo(
                [small_cashback, big_cashback],
                msku,
                offer,
                params,
                meta_promos=[meta_promo],
                assortment_group=assortment_group,
            )

    def test_extra_cashback_tag_meta_promo(self):
        '''
        проверяем, что тег повышенного кешбека 'extra-cashback'
        появляется у мета промо на оффере, если суммарные кешбеки пробили трешхолд "повышенности"
        даже если кешбеки, входящие в сумму - не повышенные
        '''
        values = {
            "perks": [
                cashback_enable_perks,
            ],
            "rids": 213,
        }
        params = self.build_url_params(**values)
        meta_promo = {
            'id': 'cashback-value',
            'promoKeys': [small_cashback_1.key, small_cashback_2.key],
            'value': self.calc_cashback_value(
                offer_with_two_small_cashbacks.price,
                small_cashback_1.blue_cashback.share + small_cashback_2.blue_cashback.share,
            ),
            'tags': ['extra-cashback'],
        }
        params = self.build_url_params(**values)
        self.check_promo(
            [small_cashback_1, small_cashback_2],
            msku_with_two_small_cashbacks,
            offer_with_two_small_cashbacks,
            params,
            meta_promos=[meta_promo],
        )

    def test_extra_cashback_offers(self):
        '''
        проверка фильтрации офферов с повышенным кешбеком параметром extra-cashback-offers

        при флаге extra_cashback_filter_only_from_partner_source=1 с параметром extra-cashback-offers=1
        на выдаче будут только офферы с повыщшенным кешбеком из Партнерского Интерфейса,
        при флаге extra_cashback_filter_only_from_partner_source=0 с параметром extra-cashback-offers=1
        на выдаче будут все офферы с повышенным кешбеком, в том числе не из ПИ
        '''
        req = 'place=prime&pp=18&promo-type=blue-cashback&numdoc=100&perks=' + ','.join(cashback_all_perks)
        extra_cashback_param = '&extra-cashback-offers={}'
        from_partner_source_flag = '&rearr-factors=extra_cashback_filter_only_from_partner_source={}'
        test_data = [
            (
                1,  # список офферов взят из теста test_get_cashback_offers_by_promo_type
                [
                    # blue_offer_1 - кешбек меньше 6%
                    (blue_offer_4, False),
                    (blue_offer_6, False),
                    (blue_offer_7, False),
                    (blue_offer_8, False),
                    (blue_offer_4_employees_cashback, False),
                    (blue_offer_with_max_offer_cashback, True),
                    (blue_offer_with_max_offer_cashback_cheap, True),
                    (offer_dsbs, False),
                    (blue_offer_cb_no_predicates, False),
                    (blue_offer_with_extra_and_normal_cb, False),
                    (blue_offer_with_extra_and_normal_cb_shop_carrier, False),
                    (blue_offer_personal_cb, False),
                    (blue_offer_cashback_value, True),
                    (blue_offer_cashback_value_with_max_thresholds, False),
                    (blue_offer_personal_compensation, False),
                    (offer_dsbs_diy_assortment_group, False),
                    (offer_dsbs_cehac_assortment_group, False),
                    (offer_dsbs_default_assortment_group, True),
                    (offer_for_detail_groups_test, True),
                    (offer_for_detail_groups_sorting_test, False),
                ],
            ),
            (
                0,
                [
                    blue_offer_1,
                    blue_offer_4,
                    blue_offer_6,
                    blue_offer_7,
                    blue_offer_8,
                    blue_offer_crossdock,
                    blue_offer_4_employees_cashback,
                    blue_offer_with_max_offer_cashback,
                    blue_offer_with_max_offer_cashback_cheap,
                    offer_dsbs,
                    blue_offer_cb_no_predicates,
                    blue_offer_with_extra_and_normal_cb,
                    blue_offer_with_extra_and_normal_cb_shop_carrier,
                    blue_offer_personal_cb,
                    blue_offer_cashback_value,
                    blue_offer_cashback_value_with_max_thresholds,
                    blue_offer_personal_compensation,
                    offer_dsbs_diy_assortment_group,
                    offer_dsbs_cehac_assortment_group,
                    offer_dsbs_default_assortment_group,
                    offer_with_two_small_cashbacks,
                    offer_for_detail_groups_test,
                    offer_for_detail_groups_sorting_test,
                ],
            ),
        ]

        for param_value, offers in test_data:
            for flag in (0, 1):

                def template(offer, extra_tag):
                    return {
                        "results": [
                            {
                                "entity": "offer",
                                "wareId": offer.waremd5,
                                "promos": [
                                    {
                                        "type": "blue-cashback",
                                        "tags": ["extra-cashback"] if extra_tag else [],
                                    }
                                ],
                            }
                        ]
                    }

                response = self.report.request_json(
                    req + extra_cashback_param.format(param_value) + from_partner_source_flag.format(flag)
                )

                if param_value:
                    # проверяем, что в выдаче есть нужные оффера
                    for offer, from_partner_source in offers:
                        if from_partner_source:
                            self.assertFragmentIn(response, template(offer, param_value))
                    # проверяем, что в выдаче только нужные оффера
                    self.assertFragmentIn(
                        response,
                        {"total": sum(1 for offer, from_partner_source in offers if not flag or from_partner_source)},
                    )
                else:
                    # проверяем, что в выдаче есть нужные оффера
                    for offer in offers:
                        self.assertFragmentIn(response, template(offer, param_value))
                    # проверяем, что в выдаче только нужные оффера
                    self.assertFragmentIn(response, {"total": len(offers)})

    def test_promo_experimental_cashback(self):
        '''
        проверяем, что кешбек, у которого в PromoDetails заполнено поле experiment_rearr_flags
        попадает на выдачу только при наличии в запросе требуемых флагов
        '''
        test_data = [
            # кешбек cashback_under_flag3 всегда будет на выдаче с флагами и без них,
            # потому что у него не заполнены флаги в PromoDetails
            (
                {
                    'flag1': [0],
                    'flag2': [0],
                },
                # кешбека под флагом flag1 нет, вместо него будет cashback_instead_of_under_flag1
                [cashback_instead_of_under_flag1, cashback_under_flag3],
            ),
            (
                {
                    'flag1': [1],
                    'flag2': [1],
                },
                [cashback_under_flag1, cashback_under_flag3],
            ),
            (
                {
                    'flag1': [0],
                    'flag2': ['some_value'],
                },
                [cashback_instead_of_under_flag1, cashback_under_flag2, cashback_under_flag3],
            ),
            (
                {
                    'flag1': [1],
                    'flag2': ['some_value'],
                },
                [cashback_under_flag1, cashback_under_flag2, cashback_under_flag3],
            ),
        ]

        for rearr_factors, offers_list in test_data:
            values = {
                "rearr_flags": rearr_factors,
                "perks": [
                    cashback_extra_enable_perks,
                ],
            }
            params = self.build_url_params(**values)

            self.check_promo(
                offers_list,
                msku_8,
                blue_offer_8,
                params,
            )

    def test_user_device_type(self):
        '''
        проверяем, что кешбек, у которого в PromoDetails заполнено поле user_device_types
        фильтруются на выдаче при параметре client не из списка
        '''
        test_data = [
            (
                'ANDROID',
                None,
                [
                    # cashback_instead_of_under_flag1 менее приоритетный чем blue_cashback_with_client_restriction
                    # и появляется, когда его нет
                    cashback_instead_of_under_flag1,
                    cashback_under_flag3,
                ],
            ),
            (
                'IOS',
                None,
                [
                    cashback_instead_of_under_flag1,
                    cashback_under_flag3,
                ],
            ),
            (
                'go_android',
                None,
                [
                    blue_cashback_with_client_restriction,
                    cashback_under_flag3,
                ],
            ),
            (
                'go_ios',
                None,
                [
                    blue_cashback_with_client_restriction,
                    cashback_under_flag3,
                ],
            ),
            (
                'frontend',
                'desktop',
                [
                    blue_cashback_with_client_restriction,
                    cashback_under_flag3,
                ],
            ),
            (
                'frontend',
                'touch',
                [
                    cashback_instead_of_under_flag1,
                    cashback_under_flag3,
                ],
            ),
        ]
        client_param = '&client={}'
        platform_param = '&platform={}'
        for client, platform, offers_list in test_data:
            additional_params = client_param.format(client) + (platform_param.format(platform) if platform else '')
            values = {
                "additional_params": additional_params,
                "perks": [
                    cashback_extra_enable_perks,
                ],
            }
            params = self.build_url_params(**values)

            self.check_promo(
                offers_list,
                msku_8,
                blue_offer_8,
                params,
            )

    def test_cashback_detail_groups_sorting(self):
        '''
        проверяем, что в детализации кешбека группы сортируются по id группы,
        исключение составляет дефолтная группа со стандартным кешбеком - она всегда первая
        '''
        values = {
            'perks': [
                cashback_extra_enable_perks,
            ],
        }
        params = self.build_url_params(**values)
        self.check_promo(
            [blue_cashback_for_dsbs_with_details_1, blue_cashback_for_dsbs_with_details_2, blue_cashback_for_dsbs],
            msku_for_detail_groups_sorting_test,
            offer_for_detail_groups_sorting_test,
            params,
            assortment_group='default',
            cashback_detail_groups=[
                {
                    'group_id': 'default',
                    'group_name': 'Стандартный кешбек',
                    'promo_keys': [blue_cashback_for_dsbs.key],
                    'value': self.calc_cashback_value(
                        offer_for_detail_groups_sorting_test.price, blue_cashback_for_dsbs.blue_cashback.share
                    ),
                    'tags': ['extra-cashback'],
                },
                {
                    'group_id': 'A_some_group',
                    'group_name': 'Нестандартный кешбек',
                    'promo_keys': [blue_cashback_for_dsbs_with_details_1.key],
                    'value': self.calc_cashback_value(
                        offer_for_detail_groups_test.price, blue_cashback_for_dsbs_with_details_1.blue_cashback.share
                    ),
                    'tags': ['extra-cashback'],
                },
                {
                    'group_id': 'B_some_group',
                    'group_name': 'Очень не стандартный кешбек',
                    'promo_keys': [blue_cashback_for_dsbs_with_details_2.key],
                    'value': self.calc_cashback_value(
                        offer_for_detail_groups_test.price, blue_cashback_for_dsbs_with_details_2.blue_cashback.share
                    ),
                    'tags': ['extra-cashback'],
                },
            ],
        )

    def test_cashback_detail_groups(self):
        '''
        проверяем, что кешбек корректно группируется по идентификатору группы детализации
        '''
        values = {
            'perks': [
                cashback_extra_enable_perks,
            ],
        }
        params = self.build_url_params(**values)
        self.check_promo(
            [blue_cashback_for_dsbs, small_cashback_for_dsbs_default, big_cashback_for_dsbs_default],
            msku_for_detail_groups_test,
            offer_for_detail_groups_test,
            params,
            assortment_group='default',
            cashback_detail_groups=[
                {
                    'group_id': 'default',
                    'group_name': 'Стандартный кешбэк',
                    # small_cashback_for_dsbs_default попал в этому группу, потому что
                    # это кешбек от поставщика + он не повышенный
                    # blue_cashback_for_dsbs попал в эту группу, потому что она прописана в его конфигурации
                    'promo_keys': [small_cashback_for_dsbs_default.key, blue_cashback_for_dsbs.key],
                    'value': self.calc_cashback_value(
                        offer_for_detail_groups_test.price,
                        blue_cashback_for_dsbs.blue_cashback.share
                        + small_cashback_for_dsbs_default.blue_cashback.share,
                    ),
                    'tags': ['extra-cashback'],
                    'cms_description_semantic_id': 'partner-default-cashback',
                },
                {
                    'group_id': 'partner_extra',
                    'group_name': 'Повышенный кешбэк от продавца',
                    # big_cashback_for_dsbs_default попал в этому группу, потому что
                    # это кешбек от поставщика + он повышенный
                    'promo_keys': [big_cashback_for_dsbs_default.key],
                    'value': self.calc_cashback_value(
                        offer_for_detail_groups_test.price, big_cashback_for_dsbs_default.blue_cashback.share
                    ),
                    'tags': ['extra-cashback'],
                    'cms_description_semantic_id': 'partner-extra-cashback',
                },
            ],
        )

    def test_ya_card_cashback(self):
        '''
        проверяем, что при передаче всех параметров для кешбека от яндекс карты, он появляется
        в мета промо и детализации кешбека
        '''
        ya_card_promo_id = 'blabla'
        values = {
            'additional_params': '&ya-card-share=0.5&ya-card-max-cashback=100&ya-card-promo-id={}'.format(
                ya_card_promo_id
            ),
            'perks': [
                cashback_extra_enable_perks,
            ],
            "rids": 213,
        }
        # промо, которые всегда есть на оффере без кешбека от яндекс карты
        cashback_details_groups_without_ya_card = [
            {
                'group_id': 'default',
                'group_name': 'Стандартный кешбэк',
                'promo_keys': [small_cashback_for_dsbs_default.key, blue_cashback_for_dsbs.key],
                'value': self.calc_cashback_value(
                    offer_for_detail_groups_test.price,
                    blue_cashback_for_dsbs.blue_cashback.share + small_cashback_for_dsbs_default.blue_cashback.share,
                ),
                'tags': ['extra-cashback'],
                'cms_description_semantic_id': 'partner-default-cashback',
            },
            {
                'group_id': 'partner_extra',
                'group_name': 'Повышенный кешбэк от продавца',
                'promo_keys': [big_cashback_for_dsbs_default.key],
                'value': self.calc_cashback_value(
                    offer_for_detail_groups_test.price, big_cashback_for_dsbs_default.blue_cashback.share
                ),
                'tags': ['extra-cashback'],
                'cms_description_semantic_id': 'partner-extra-cashback',
            },
        ]
        meta_promo_without_ya_card = [
            {
                'id': 'cashback-value',
                'promoKeys': [
                    small_cashback_for_dsbs_default.key,
                    blue_cashback_for_dsbs.key,
                    big_cashback_for_dsbs_default.key,
                ],
                'value': self.calc_cashback_value(
                    offer_for_detail_groups_test.price,
                    small_cashback_for_dsbs_default.blue_cashback.share
                    + blue_cashback_for_dsbs.blue_cashback.share
                    + big_cashback_for_dsbs_default.blue_cashback.share,
                ),
                'tags': ['extra-cashback'],
                'cms_description_semantic_id': 'partner-extra-cashback',
            },
        ]
        for report_params, has_ya_card_cashback, has_max_restriction in (
            ('&ya-card-share=0.5&ya-card-max-cashback=100&ya-card-promo-id={}'.format(ya_card_promo_id), True, True),
            ('&ya-card-share=0.5&ya-card-promo-id={}'.format(ya_card_promo_id), True, False),
            (
                '&ya-card-share=0.5&ya-card-promo-id={}'.format(ya_card_promo_id) + '&ya-card-max-offer-price=4001',
                True,
                False,
            ),
            (
                '&ya-card-share=0.5&ya-card-promo-id={}'.format(ya_card_promo_id) + '&ya-card-max-offer-price=3999',
                False,
                False,
            ),
        ):
            values['additional_params'] = report_params
            params = self.build_url_params(**values)
            meta_promo = {
                'id': 'cashback-ya-card',
                'promoKeys': [ya_card_promo_id],
                'value': 100 if has_max_restriction else 2000,
                'tags': ['extra-cashback'],
                'cms_description_semantic_id': 'yandex-bank-card',
            }
            self.check_promo(
                [blue_cashback_for_dsbs, small_cashback_for_dsbs_default, big_cashback_for_dsbs_default],
                msku_for_detail_groups_test,
                offer_for_detail_groups_test,
                params,
                assortment_group='default',
                meta_promos=meta_promo_without_ya_card + [meta_promo] if has_ya_card_cashback else [],
                cashback_detail_groups=cashback_details_groups_without_ya_card
                + [
                    {
                        'group_id': 'yandex-bank-card',
                        'group_name': 'При оплате со счёта в Яндексе',
                        'promo_keys': [
                            ya_card_promo_id,
                        ],
                        'value': 100 if has_max_restriction else 2000,
                        'tags': ['extra-cashback'],
                        'cms_description_semantic_id': 'yandex-bank-card',
                    },
                ]
                if has_ya_card_cashback
                else [],
            )

    def test_ya_card_cashback_without_prepayment(self):
        '''
        проверяем, что кешбек от яндекс карты не появится на товарах без предоплаты
        '''
        ya_card_promo_id = 'blabla'
        values = {
            'perks': [
                cashback_disable_perks,
            ],
            'rids': 213,
            'regset': 1,
            'additional_params': '&ya-card-share=0.5&ya-card-max-cashback=100&ya-card-promo-id={}'.format(
                ya_card_promo_id
            ),
        }
        params = self.build_url_params(**values)
        self.check_promo(
            [],
            msku_without_prepayment_dsbs,
            offer_dsbs_without_prepayment,
            params,
            assortment_group='default',
            meta_promos=[],
            cashback_detail_groups=[],
        )

    def test_ya_card_cashback_without_other_promo(self):
        '''
        проверяем, что при передаче всех параметров для кешбека от яндекс карты, он появляется
        в мета промо и детализации кешбека даже на товарах, на которых нет других промо
        '''
        ya_card_promo_id = 'blabla'
        values = {
            'additional_params': '&ya-card-share=0.5&ya-card-max-cashback=10000&ya-card-promo-id={}'.format(
                ya_card_promo_id
            ),
            'perks': [
                cashback_disable_perks,
            ],
            'rids': 213,
            'regset': 1,
        }
        params = self.build_url_params(**values)
        meta_promo = {
            'id': 'cashback-ya-card',
            'promoKeys': [ya_card_promo_id],
            'value': 500,
            'tags': ['extra-cashback'],
            'cms_description_semantic_id': 'yandex-bank-card',
        }
        self.check_promo(
            [],
            msku_no_promo,
            blue_offer_no_promo,
            params,
            assortment_group='default',
            meta_promos=[meta_promo],
            cashback_detail_groups=[
                {
                    'group_id': 'yandex-bank-card',
                    'group_name': 'При оплате со счёта в Яндексе',
                    'promo_keys': [
                        ya_card_promo_id,
                    ],
                    'value': 500,
                    'tags': ['extra-cashback'],
                    'cms_description_semantic_id': 'yandex-bank-card',
                },
            ],
        )


if __name__ == '__main__':
    main()
