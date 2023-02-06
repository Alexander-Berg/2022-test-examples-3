#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa
from core.types.express_partners import ExpressSupplier
from market.proto.common.common_pb2 import ESupplierFlag
from core.matcher import Absent, NotEmpty
from core.testcase import TestCase, main
from core.types.autogen import b64url_md5
from core.types.dynamic_filters import DynamicBlueGenericBundlesPromos, DynamicQPromos, DynamicWarehouseInfo
from core.types.model import Model
from core.types.offer_promo import (
    calc_discount_percent,
    PromoCheapestAsGift,
    OffersMatchingRules,
    Promo,
    PromoBlueFlash,
    PromoBlueSet,
    PromoType,
    PromoDirectDiscount,
    PromoRestrictions,
)
from core.types.shop import Shop
from core.types.sku import MarketSku, BlueOffer
from core.types.offer import Offer
from core.types.taxes import Tax
from core.types.region import Region

from itertools import count
from unittest import skip


FEED = 777
WHITE_FEED = 888
nummer = count()


def __blue_offer(price=1000, feed=FEED, vendor_id=None, supplier_id=None, is_express=False):
    num = next(nummer)
    return BlueOffer(
        waremd5=b64url_md5(num),
        price=price,
        feedid=feed,
        offerid='ССКУ_{}'.format(num),
        vendor_id=vendor_id,
        supplier_id=supplier_id,
        is_express=is_express,
    )


def __white_offer(msku, supplier_id=None, price=1000, feed=WHITE_FEED, is_express=False, cpa=Offer.CPA_NO):
    num = next(nummer)
    return Offer(
        waremd5=b64url_md5(num),
        price=price,
        feedid=feed,
        fesh=supplier_id,
        hyperid=msku.hyperid if isinstance(msku, MarketSku) else None,
        sku=msku.sku if isinstance(msku, MarketSku) else msku,
        title=msku.title if isinstance(msku, MarketSku) else 'title4msku {}'.format(msku),
        offerid='offer_id_{}'.format(num),
        is_express=is_express,
        cpa=cpa,
    )


blue_offer_1 = __blue_offer()  # тест довоза быстрого промо (не было - стало)
blue_offer_2 = __blue_offer(vendor_id=2)  # тест изменения 1, на оффере не было промо, в изменённом - стало
blue_offer_3 = __blue_offer(vendor_id=3)  # тест изменения 2, на оффере другое промо приехало, цена поменялась
blue_offer_4 = __blue_offer(vendor_id=4)  # тест изменения 3, на оффере было промо, приехало быстрое - и не стало
blue_offer_5 = __blue_offer()  # тест отключения промо, в индексе было, быстро промо - приехало и отключило
blue_offer_6 = __blue_offer()  # тест обновления промо с совпадающим promo_key
blue_offer_7 = __blue_offer(vendor_id=7)  # тест комплектной акции
blue_offer_8 = __blue_offer(vendor_id=8)  # тест комплектной акции (вторичка)
blue_offer_9 = __blue_offer(vendor_id=9)  # тест комплектной акции (быстрая)
blue_offer_A = __blue_offer(vendor_id=10)  # тест комплектной акции (быстрая, вторичка)


def __msku(offers, msku=None, hid=None):
    num = next(nummer)
    return MarketSku(sku=msku or num, hyperid=hid or num, blue_offers=offers if isinstance(offers, list) else [offers])


msku_1 = __msku(blue_offer_1, msku=123456789012)
msku_2 = __msku(blue_offer_2)
msku_3 = __msku(blue_offer_3)
msku_4 = __msku(blue_offer_4)
msku_5 = __msku(blue_offer_5)
msku_6 = __msku(blue_offer_6)
msku_7 = __msku(blue_offer_7)
msku_8 = __msku(blue_offer_8)
msku_9 = __msku(blue_offer_9)
msku_A = __msku(blue_offer_A)


# довоз нового быстро-промо
promo1_fast = Promo(
    promo_type=PromoType.BLUE_FLASH,
    key=b64url_md5(next(nummer)),
    feed_id=FEED,
    shop_promo_id='promo1',
    url='http://яндекс.рф/',
    blue_flash=PromoBlueFlash(
        items=[
            {'feed_id': FEED, 'offer_id': blue_offer_1.offerid, 'price': {'value': 777, 'currency': 'RUR'}},
        ],
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_1]),
    ],
    generation_ts=1,
)


# изменение быстро-промо, исходное
promo2 = Promo(
    promo_type=PromoType.BLUE_FLASH,
    key=b64url_md5(next(nummer)),
    feed_id=FEED,
    shop_promo_id='promo2',
    url='http://яндекс.рф/',
    blue_flash=PromoBlueFlash(
        items=[
            {'feed_id': FEED, 'offer_id': blue_offer_3.offerid, 'price': {'value': 888, 'currency': 'RUR'}},
            {'feed_id': FEED, 'offer_id': blue_offer_4.offerid, 'price': {'value': 777, 'currency': 'RUR'}},
        ],
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED, blue_offer_3.offerid],
                [FEED, blue_offer_4.offerid],
            ]
        ),
    ],
)

# быстрое
promo2_fast = Promo(
    promo_type=PromoType.BLUE_FLASH,
    key=b64url_md5(next(nummer)),
    feed_id=FEED,
    shop_promo_id='promo2',
    url='http://яндекс.рф/',
    blue_flash=PromoBlueFlash(
        items=[
            {
                'feed_id': FEED,
                'offer_id': blue_offer_2.offerid,
                'price': {'value': 777, 'currency': 'RUR'},
            },  # новая строка
            {
                'feed_id': FEED,
                'offer_id': blue_offer_3.offerid,
                'price': {'value': 777, 'currency': 'RUR'},
            },  # поменялась цена
            # строка { feed_id=FEED, offer_id=blue_offer_4.offerid } удалена
        ],
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_2, msku_3]),
    ],
    generation_ts=1,
)


# отключение через быстро-промо, исходное
promo3 = Promo(
    promo_type=PromoType.BLUE_FLASH,
    key=b64url_md5(next(nummer)),
    feed_id=FEED,
    shop_promo_id='promo3',
    url='http://яндекс.рф/',
    blue_flash=PromoBlueFlash(
        items=[
            {'feed_id': FEED, 'offer_id': blue_offer_5.offerid, 'price': {'value': 888, 'currency': 'RUR'}},
        ],
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED, blue_offer_5.offerid],
            ]
        ),
    ],
)

# быстрое
promo3_fast = Promo(
    promo_type=PromoType.BLUE_FLASH,
    key=b64url_md5(next(nummer)),
    feed_id=FEED,
    shop_promo_id='promo3',
    url='http://яндекс.рф/',
    blue_flash=PromoBlueFlash(
        items=[
            {'feed_id': FEED, 'offer_id': blue_offer_5.offerid, 'price': {'value': 888, 'currency': 'RUR'}},
        ],
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_5]),
    ],
    disabled_by_default=True,  # отключено
    generation_ts=1,
)

# один промо-ключ (в индексе)
promo4 = Promo(
    promo_type=PromoType.BLUE_FLASH,
    key="same_promo_key",
    feed_id=FEED,
    shop_promo_id='promo4',
    anaplan_id='promo4',
    url='http://яндекс.рф/',
    blue_flash=PromoBlueFlash(
        items=[
            {'feed_id': FEED, 'offer_id': blue_offer_6.offerid, 'price': {'value': 888, 'currency': 'RUR'}},
        ],
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED, blue_offer_6.offerid],
            ]
        ),
    ],
)

# один промо-ключ (быстрое)
promo4_fast = Promo(
    promo_type=PromoType.BLUE_FLASH,
    key="same_promo_key",
    feed_id=FEED,
    shop_promo_id='promo4',
    anaplan_id='promo4_fast',
    url='http://яндекс.рф/',
    blue_flash=PromoBlueFlash(
        items=[
            {'feed_id': FEED, 'offer_id': blue_offer_6.offerid, 'price': {'value': 333, 'currency': 'RUR'}},
        ],
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_6]),
    ],
    generation_ts=1,
)

# комплектная акция (индекс)
promo5 = Promo(
    promo_type=PromoType.BLUE_SET,
    feed_id=FEED,
    key=b64url_md5(next(nummer)),
    url='http://яндекс.рф/',
    shop_promo_id='BLUE_SET1',
    blue_set=PromoBlueSet(
        sets_content=[
            {
                'items': [
                    {'offer_id': blue_offer_7.offerid, 'discount': 5},
                    {'offer_id': blue_offer_8.offerid, 'discount': 10},
                ],
                'linked': False,
            },
        ],
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED, blue_offer_7.offerid],
                [FEED, blue_offer_8.offerid],
            ]
        ),
    ],
)

# комплектная акция (быстрая)
promo5_fast = Promo(
    promo_type=PromoType.BLUE_SET,
    feed_id=FEED,
    key=b64url_md5(next(nummer)),
    url='http://яндекс.рф/',
    shop_promo_id='BLUE_SET1',
    blue_set=PromoBlueSet(
        sets_content=[
            {
                'items': [
                    {'offer_id': blue_offer_9.offerid, 'discount': 5},
                    {'offer_id': blue_offer_A.offerid, 'discount': 10},
                ],
                'linked': False,
            },
        ],
    ),
    offers_matching_rules=[OffersMatchingRules(mskus=[msku_9])],
    generation_ts=1,
)

# быстрые промо для тестирования быстрых лендингов
HID_100 = 100
HID_101 = 101
SHOP2 = 888
FEED2 = 889
FEED3 = 890
VENDOR = 903
WARE1 = 42

blue_offer_100 = __blue_offer()
blue_offer_101 = __blue_offer()
blue_offer_102 = __blue_offer()
blue_offer_103 = __blue_offer(feed=FEED2)
blue_offer_104 = __blue_offer(vendor_id=VENDOR)
blue_offer_105 = __blue_offer(feed=FEED3)
blue_offer_106 = __blue_offer(feed=FEED3)

msku_100 = __msku(blue_offer_100, hid=HID_100)
msku_101 = __msku(blue_offer_101, hid=HID_101)
msku_102 = __msku(blue_offer_102)
msku_103 = __msku(blue_offer_103)
msku_104 = __msku(blue_offer_104)
msku_105 = __msku(blue_offer_105)
msku_106 = __msku(blue_offer_106)

promo6_fast = Promo(
    promo_type=PromoType.BLUE_FLASH,
    key=b64url_md5(next(nummer)),
    feed_id=FEED,
    shop_promo_id='promo6',
    url='http://яндекс.рф/',
    blue_flash=PromoBlueFlash(
        items=[
            {'feed_id': FEED, 'offer_id': blue_offer_100.offerid, 'price': {'value': 777, 'currency': 'RUR'}},
            {'feed_id': FEED, 'offer_id': blue_offer_101.offerid, 'price': {'value': 777, 'currency': 'RUR'}},
            {'feed_id': FEED, 'offer_id': blue_offer_102.offerid, 'price': {'value': 777, 'currency': 'RUR'}},
            {'feed_id': FEED2, 'offer_id': blue_offer_103.offerid, 'price': {'value': 777, 'currency': 'RUR'}},
            {'feed_id': FEED, 'offer_id': blue_offer_104.offerid, 'price': {'value': 777, 'currency': 'RUR'}},
            {'feed_id': FEED3, 'offer_id': blue_offer_105.offerid, 'price': {'value': 777, 'currency': 'RUR'}},
            {'feed_id': FEED3, 'offer_id': blue_offer_106.offerid, 'price': {'value': 777, 'currency': 'RUR'}},
        ],
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(categories=[HID_100], mskus=[msku_100], feed_offer_ids=[[FEED, blue_offer_100.offerid]]),
        OffersMatchingRules(mskus=[msku_101]),
        OffersMatchingRules(feed_offer_ids=[[FEED, blue_offer_102.offerid]]),
        OffersMatchingRules(shops=[FEED2]),
        OffersMatchingRules(vendors=[VENDOR]),
        OffersMatchingRules(warehouses=[WARE1], excluded_mskus=[msku_106]),
    ],
    generation_ts=1,
)

HID_200 = 200
HID_201 = 201

blue_offer_200 = __blue_offer()
blue_offer_201 = __blue_offer()
blue_offer_202 = __blue_offer()

msku_200 = __msku(blue_offer_200, hid=HID_200)
msku_201 = __msku(blue_offer_201, hid=HID_200)
msku_202 = __msku(blue_offer_202, hid=HID_201)

# нужно хотя бы одно промо такого типа в индексе (для срабатывания лендинга по промо-типу)
promo7 = Promo(
    promo_type=PromoType.CHEAPEST_AS_GIFT,
    key=b64url_md5(next(nummer)),
    feed_id=FEED,
    shop_promo_id='promo7_fake',
    url='http://яндекс.рф/',
    cheapest_as_gift=PromoCheapestAsGift(
        offer_ids=[
            (0, 'fake'),
        ],
        count=3,
        promo_url='',
        link_text='',
        allow_berubonus=False,
        allow_promocode=False,
    ),
)

promo7_fast = Promo(
    promo_type=PromoType.CHEAPEST_AS_GIFT,
    key=b64url_md5(next(nummer)),
    feed_id=FEED,
    shop_promo_id='promo7',
    url='http://яндекс.рф/',
    cheapest_as_gift=PromoCheapestAsGift(
        offer_ids=[
            (FEED, blue_offer_200.offerid),
            (FEED, blue_offer_201.offerid),
            (FEED, blue_offer_202.offerid),
        ],
        count=3,
        promo_url='',
        link_text='',
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_200, msku_201, msku_202]),
    ],
    generation_ts=1,
)


# неприменение быстропромо, если его нет в белом списке лоялти
blue_offer_300 = __blue_offer()
msku_300 = __msku(blue_offer_300)
# исходное
promo8 = Promo(
    promo_type=PromoType.BLUE_FLASH,
    key=b64url_md5(next(nummer)),
    feed_id=FEED,
    shop_promo_id='promo8',
    url='http://яндекс.рф/',
    blue_flash=PromoBlueFlash(
        items=[
            {'feed_id': FEED, 'offer_id': blue_offer_300.offerid, 'price': {'value': 777, 'currency': 'RUR'}},
        ],
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED, blue_offer_300.offerid],
            ]
        ),
    ],
)
# быстрое
promo8_fast = Promo(
    promo_type=PromoType.BLUE_FLASH,
    key=b64url_md5(next(nummer)),
    feed_id=FEED,
    shop_promo_id='promo8',
    url='http://яндекс.рф/',
    blue_flash=PromoBlueFlash(
        items=[
            {'feed_id': FEED, 'offer_id': blue_offer_300.offerid, 'price': {'value': 888, 'currency': 'RUR'}},
        ],
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_300]),
    ],
    generation_ts=1,
)


# длинные лендинги
blue_offers_many = [__blue_offer() for _ in range(2000)]
mskus_many = [__msku(offer) for offer in blue_offers_many]
promo9_fast = Promo(
    promo_type=PromoType.BLUE_FLASH,
    key=b64url_md5(next(nummer)),
    feed_id=FEED,
    shop_promo_id='promo9',
    url='http://яндекс.рф/',
    blue_flash=PromoBlueFlash(
        items=[
            {'feed_id': FEED, 'offer_id': offer.offerid, 'price': {'value': 888, 'currency': 'RUR'}}
            for offer in blue_offers_many
        ],
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(feed_offer_ids=[[FEED, offer.offerid] for offer in blue_offers_many]),
    ],
    generation_ts=1,
)

# Тестирования признака экспресса
HID_300 = 300
HID_301 = 301
FESH_BLUE = 1
FESH_BLUE_EX = 2
FESH_DSBS = 3
FESH_DSBS_EX = 4
FESH_WHITE = 5
FEED_4_EX = 900
FEED_5_NO_EX = 901
FEED_6_DSBS = 902
FEED_7_DSBS_EX = 903
FEED_8_WHITE = 904
WAREHOUSE = 10
WAREHOUSE_EX = 11
WAREHOUSE_DSBS = 12
RIDS_MSK = 213

# Поставщик для синего оффера
dropship_shop = Shop(
    fesh=FESH_BLUE,
    datafeed_id=FEED_5_NO_EX,
    warehouse_id=WAREHOUSE,
    priority_region=RIDS_MSK,
    regions=[RIDS_MSK],
    tax_system=Tax.OSN,
    supplier_type=Shop.THIRD_PARTY,
    blue=Shop.BLUE_REAL,
    fulfillment_program=False,
)

# Экспресс-поставщик для синего оффера (добавляется в express_partners)
dropship_express_shop = Shop(
    fesh=FESH_BLUE_EX,
    datafeed_id=FEED_4_EX,
    warehouse_id=WAREHOUSE_EX,
    priority_region=RIDS_MSK,
    regions=[RIDS_MSK],
    tax_system=Tax.OSN,
    supplier_type=Shop.THIRD_PARTY,
    blue=Shop.BLUE_REAL,
    fulfillment_program=False,
    with_express_warehouse=True,
)
# просто магазин для DSBS
dsbs_shop = Shop(
    fesh=FESH_DSBS,
    datafeed_id=FEED_6_DSBS,
    warehouse_id=WAREHOUSE_DSBS,
    priority_region=RIDS_MSK,
    regions=[RIDS_MSK],
    cpa=Shop.CPA_REAL,
    is_dsbs=True,
)

# просто магазин для белого оффера
white_shop = Shop(
    fesh=FESH_WHITE,
    datafeed_id=FEED_8_WHITE,
    priority_region=RIDS_MSK,
    warehouse_id=None,
    regions=[RIDS_MSK],
    cpa=Shop.CPA_NO,
    cpc=Shop.CPC_REAL,
)

blue_offer_express = __blue_offer(
    feed=FEED_4_EX, supplier_id=FESH_BLUE_EX, is_express=True
)  # синий оффер с экспресс доставкой
blue_offer_no_express = __blue_offer(feed=FEED_5_NO_EX, supplier_id=FESH_BLUE)  # синий оффер без экспресс доставки

msku_blue_express = __msku(blue_offer_express, hid=HID_300)
msku_blue_no_express = __msku(blue_offer_no_express, hid=HID_300)
msku_white_no_express = MarketSku(
    title='Обычный белый оффер',
    hyperid=HID_300,
    sku=next(nummer),
)

msku_dsbs_no_express = MarketSku(
    title='DSBS неэкспресс оффер',
    hyperid=HID_300,
    sku=next(nummer),
)
dsbs_offer_no_express = __white_offer(
    msku=msku_dsbs_no_express, feed=FEED_6_DSBS, supplier_id=FESH_DSBS, is_express=False, cpa=Offer.CPA_REAL
)
white_offer_no_express = __white_offer(
    msku=msku_white_no_express, feed=FEED_8_WHITE, supplier_id=FESH_WHITE, is_express=False
)  # белый оффер без экспресс доставки

promo10_fast_express = Promo(
    promo_type=PromoType.PROMO_CODE,
    promo_code='promocode_express_text',
    description='promocode_express_description',
    discount_value=15,
    feed_id=FEED,
    key=b64url_md5(next(nummer)),
    url='http://promocode_express.com/',
    landing_url='http://promocode_express_landing.com/',
    shop_promo_id='promocode_express',
    conditions='conditions to buy',
    promo_internal_priority=4,
    offers_matching_rules=[
        OffersMatchingRules(
            categories=[
                HID_300,
            ],
            supplier_flags=ESupplierFlag.EXPRESS_WAREHOUSE,
        )
    ],
    generation_ts=1,
)

promo11_fast_no_express = Promo(
    promo_type=PromoType.PROMO_CODE,
    promo_code='promocode_no_express_text',
    description='promocode_no_express_description',
    discount_value=15,
    feed_id=FEED,
    key=b64url_md5(next(nummer)),
    url='http://promocode_no_express.com/',
    landing_url='http://promocode_no_express_landing.com/',
    shop_promo_id='promocode_no_express',
    conditions='conditions to buy',
    promo_internal_priority=4,
    offers_matching_rules=[
        OffersMatchingRules(
            categories=[
                HID_300,
            ],
            excluded_supplier_flags=ESupplierFlag.EXPRESS_WAREHOUSE,
        )
    ],
    generation_ts=1,
)

# Тестирования признака DSBS
promo12_fast_include_dsbs = Promo(
    promo_type=PromoType.PROMO_CODE,
    promo_code='promocode_dsbs_text',
    description='promocode_dsbs_description',
    discount_value=15,
    feed_id=FEED,
    key=b64url_md5(next(nummer)),
    url='http://promocode_dsbs.com/',
    landing_url='http://promocode_dsbs_landing.com/',
    shop_promo_id='promocode_dsbs',
    conditions='conditions to buy',
    promo_internal_priority=4,
    offers_matching_rules=[
        OffersMatchingRules(
            categories=[
                HID_301,
            ],
            supplier_flags=ESupplierFlag.DSBS_SUPPLIER,
        )
    ],
    generation_ts=1,
)

promo13_fast_exclude_dsbs = Promo(
    promo_type=PromoType.PROMO_CODE,
    promo_code='promocode_no_dsbs_text',
    description='promocode_no_dsbs_description',
    discount_value=15,
    feed_id=FEED,
    key=b64url_md5(next(nummer)),
    url='http://promocode_no_dsbs.com/',
    landing_url='http://promocode_no_dsbs_landing.com/',
    shop_promo_id='promocode_no_dsbs',
    conditions='conditions to buy',
    promo_internal_priority=4,
    offers_matching_rules=[
        OffersMatchingRules(
            categories=[
                HID_301,
            ],
            excluded_supplier_flags=ESupplierFlag.DSBS_SUPPLIER,
        )
    ],
    generation_ts=1,
)

msku_include_dsbs = MarketSku(
    title='DSBS оффер',
    hyperid=HID_301,
    sku=next(nummer),
)
msku_exclude_dsbs = MarketSku(
    title='Не DSBS оффер',
    hyperid=HID_301,
    sku=next(nummer),
)
offer_include_dsbs = __white_offer(msku=msku_include_dsbs, feed=FEED_6_DSBS, supplier_id=FESH_DSBS, cpa=Offer.CPA_REAL)
offer_exclude_dsbs = __white_offer(msku=msku_exclude_dsbs, feed=FEED_8_WHITE, supplier_id=FESH_WHITE, cpa=Offer.CPA_NO)


# DSBS без MSKU
WHITE_MSKU_NO_MSKU = 1234567
white_offer_no_msku = __white_offer(
    msku=WHITE_MSKU_NO_MSKU, feed=FEED_8_WHITE, supplier_id=FESH_WHITE, cpa=Offer.CPA_REAL
)
promo14_fast = Promo(
    promo_type=PromoType.PROMO_CODE,
    promo_code='promocode_dsbs_text',
    discount_value=15,
    feed_id=FEED,
    key=b64url_md5(next(nummer)),
    url='http://promocode_dsbs.com/',
    landing_url='http://promocode_dsbs_landing.com/',
    shop_promo_id='promocode_dsbs_no_msku',
    offers_matching_rules=[
        OffersMatchingRules(mskus=[WHITE_MSKU_NO_MSKU]),
    ],
    generation_ts=1,
)


# Персональное быстрое промо

blue_offer_1151 = __blue_offer(price=1000)
msku_1151 = __msku(blue_offer_1151, msku=1151)
blue_offer_1152 = __blue_offer(price=2000)
msku_1152 = __msku(blue_offer_1152, msku=1152)

promo15_fast = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED,
    key=b64url_md5(next(nummer)),
    url='http://personal_fast_promo_15.com/',
    shop_promo_id='promo15',
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED,
                'offer_id': blue_offer_1151.offerid,
                'discount_percent': 7,
            },
            {
                'feed_id': FEED,
                'offer_id': blue_offer_1152.offerid,
                'discount_price': {'value': 1500, 'currency': 'RUR'},
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(feed_offer_ids=[[FEED, blue_offer_1151.offerid], [FEED, blue_offer_1152.offerid]])
    ],
    restrictions=PromoRestrictions(
        predicates=[
            {
                'perks': ['perk1', 'perk2', 'perk3', '!perk4', 'yalogin'],
            }
        ]
    ),
    generation_ts=1650014367,
)

promo15_fast_off = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED,
    key=b64url_md5(next(nummer)),
    url='http://personal_fast_promo_15.com/',
    shop_promo_id='promo15',
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED,
                'offer_id': blue_offer_1151.offerid,
                'discount_percent': 7,
            },
            {
                'feed_id': FEED,
                'offer_id': blue_offer_1152.offerid,
                'discount_price': {'value': 1500, 'currency': 'RUR'},
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(feed_offer_ids=[[FEED, blue_offer_1151.offerid], [FEED, blue_offer_1152.offerid]])
    ],
    restrictions=PromoRestrictions(
        predicates=[
            {
                'perks': ['perk1', 'perk2', 'perk3', '!perk4', 'yalogin'],
            }
        ]
    ),
    generation_ts=1650014368,
    force_disabled=True,
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += [
            'enable_fast_promo_matcher=1',
            'enable_fast_promo_matcher_test=1',
            'market_new_cpm_iterator=0',
        ]

        blue_offer_3.promo = [promo2]
        blue_offer_4.promo = [promo2]
        blue_offer_5.promo = [promo3]
        blue_offer_6.promo = [promo4]
        blue_offer_7.promo = [promo5]
        blue_offer_8.promo = [promo5]
        blue_offer_300.promo = [promo8]

        cls.index.regiontree += [Region(rid=RIDS_MSK, name="Москва", tz_offset=10800)]

        cls.index.shops += [
            Shop(
                fesh=FEED,
                datafeed_id=FEED,
                priority_region=RIDS_MSK,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
            ),
            Shop(
                fesh=FEED2,
                datafeed_id=FEED2,
                priority_region=RIDS_MSK,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
            ),
            Shop(
                fesh=FEED3,
                datafeed_id=FEED3,
                priority_region=213,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=WARE1,
            ),
            dropship_shop,
            dropship_express_shop,
            dsbs_shop,
            white_shop,
        ]

        cls.index.express_partners.suppliers += [
            ExpressSupplier(
                feed_id=FEED_4_EX,
                supplier_id=FESH_BLUE_EX,
                warehouse_id=WAREHOUSE_EX,
            ),
            ExpressSupplier(
                feed_id=FEED_7_DSBS_EX,
                supplier_id=FESH_DSBS_EX,
                warehouse_id=WAREHOUSE_EX,
            ),
        ]

        cls.index.mskus += [
            msku_1,
            msku_2,
            msku_3,
            msku_4,
            msku_5,
            msku_6,
            msku_7,
            msku_8,
            msku_9,
            msku_A,
            msku_100,
            msku_101,
            msku_102,
            msku_103,
            msku_104,
            msku_105,
            msku_106,
            msku_1151,
            msku_1152,
            msku_200,
            msku_201,
            msku_202,
            msku_300,
            msku_blue_express,
            msku_blue_no_express,
            msku_white_no_express,
            msku_dsbs_no_express,
            msku_include_dsbs,
            msku_exclude_dsbs,
        ] + mskus_many

        cls.index.offers += [
            dsbs_offer_no_express,
            white_offer_no_express,
            offer_include_dsbs,
            offer_exclude_dsbs,
            white_offer_no_msku,
        ]

        cls.index.models += [
            Model(hyperid=HID_100, hid=HID_100),
            Model(hyperid=HID_101, hid=HID_101),
            Model(hyperid=HID_200, hid=HID_200),
            Model(hyperid=HID_201, hid=HID_201),
            Model(hyperid=HID_300, hid=HID_300),
            Model(hyperid=HID_301, hid=HID_301),
        ]

        cls.index.promos += [
            promo2,
            promo3,
            promo4,
            promo5,
            promo7,
            promo8,
        ]

        cls.settings.loyalty_enabled = True
        cls.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(
                whitelist=[
                    # несмотря на то, что детальки едут через быстрый динамик, лоялти должен эти промки обелить! - для синхронизации (сначала промо должен загрузить лоялти)
                    promo1_fast.key,
                    promo2.key,
                    promo2_fast.key,
                    promo3.key,
                    promo3_fast.key,
                    promo4.key,
                    promo4_fast.key,
                    promo5.key,
                    promo5_fast.key,
                    promo6_fast.key,
                    promo7.key,
                    promo7_fast.key,
                    promo8.key,  # promo8_fast.key НЕ добавляется!
                    promo9_fast.key,
                    promo15_fast.key,
                    promo15_fast_off.key,
                ]
            )
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=WARE1, home_region=213),
            DynamicWarehouseInfo(id=WAREHOUSE, home_region=213),
            DynamicWarehouseInfo(id=WAREHOUSE_EX, home_region=213, is_express=True),
        ]

    def __should(self, msku, blue_offer, promo, promo_price, extra_rearr=None):
        for rgb in ('blue', 'green', 'green_with_blue'):
            for place in ('prime',):
                # выбор байбокса нужен детерминированный, для этого фиксируем yandexuid (MARKETOUT-16443)
                params = 'place={place}&rids=0&regset=1&pp=18&market-sku={msku}&rgb={rgb}&yandexuid=1'
                # params += '&debug=1&rearr-factors=market_documents_search_trace={}'.format(blue_offer.waremd5)  # для отладки
                if extra_rearr:
                    params += '&rearr-factors={}'.format(extra_rearr)
                response = self.report.request_json(params.format(place=place, msku=msku.sku, rgb=rgb))

                # проверяем что в выдаче есть оффер с корректным блоком "promo"
                self.assertFragmentIn(
                    response,
                    [
                        {
                            'entity': 'offer',
                            'wareId': blue_offer.waremd5,
                            'prices': {
                                'value': str(promo_price),
                                'discount': {
                                    'percent': calc_discount_percent(promo_price, blue_offer.price),
                                    'oldMin': str(blue_offer.price),
                                },
                            },
                            'promos': [
                                {
                                    'type': promo.type_name,
                                    'key': promo.key,
                                    'url': promo.url or Absent(),
                                    'startDate': NotEmpty() if promo.start_date else Absent(),
                                    'endDate': NotEmpty() if promo.start_date else Absent(),
                                    'itemsInfo': {
                                        'promoPrice': {
                                            'value': str(promo_price),
                                            'currency': 'RUR',
                                        },
                                        'discount': {
                                            'percent': calc_discount_percent(promo_price, blue_offer.price),
                                            'oldMin': str(blue_offer.price),
                                        },
                                        'constraints': {
                                            'allow_berubonus': promo.blue_flash.allow_berubonus,
                                            'allow_promocode': promo.blue_flash.allow_promocode,
                                        },
                                    },
                                }
                            ],
                        }
                    ],
                    allow_different_len=False,
                )

    def __should_not(self, msku, waremd5, extra_param='', rids=0, trace_promo=None):
        for place in ('prime',):
            params = 'place={place}&rids={rids}&regset=1&pp=18&market-sku={msku}&yandexuid=1' + extra_param
            if trace_promo:
                params += '&debug=1&rearr-factors=market_documents_search_trace={}'.format(waremd5)  # для отладки
            response = self.report.request_json(params.format(place=place, msku=msku, rids=rids))
            # блок промо должен отсутстовать
            self.assertFragmentIn(
                response,
                [
                    {
                        'entity': 'offer',
                        'wareId': waremd5,
                        'promos': Absent(),
                    }
                ],
            )

            if trace_promo:
                self.assertFragmentIn(
                    response,
                    {
                        'debug': {
                            'docs_search_trace': {
                                'traces': [
                                    {
                                        'promos': [
                                            {
                                                'promoKey': trace_promo.key,
                                                'promoType': trace_promo.type_name,
                                                'promoState': 'DeclinedByFastPromoNotInIndex',
                                                'isFastPromo': 'true',
                                            }
                                        ]
                                    }
                                ],
                            },
                        },
                    },
                    allow_different_len=True,
                )

    def __should_be_promocode(self, msku, offer, promo):
        request = 'place=prime&rids=0&regset=1&pp=18&yandexuid=1&rids={rids}&rearr-factors=enable_fast_promo_new_promos=1'.format(
            rids=RIDS_MSK
        )
        request += '&market-sku={msku}'.format(msku=msku) if msku else '&offerid={ware}'.format(ware=offer.waremd5)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': offer.waremd5,
                'promos': [
                    {
                        'type': PromoType.PROMO_CODE,
                        'key': promo.key,
                    }
                ],
            },
            allow_different_len=False,
        )

    def __should_be_direct_discount(self, msku, blue_offer, promo, promo_price, extra_rearr=None):
        for rgb in ('blue', 'green', 'green_with_blue'):
            for place in ('prime',):
                # выбор байбокса нужен детерминированный, для этого фиксируем yandexuid (MARKETOUT-16443)
                params = 'place={place}&rids=0&regset=1&pp=18&market-sku={msku}&rgb={rgb}&yandexuid=1'
                # params += '&debug=1&rearr-factors=market_documents_search_trace={}'.format(blue_offer.waremd5)  # для отладки
                if extra_rearr:
                    params += '&rearr-factors={}'.format(extra_rearr)
                response = self.report.request_json(params.format(place=place, msku=msku.sku, rgb=rgb))

                # проверяем что в выдаче есть оффер с корректным блоком "promo"
                self.assertFragmentIn(
                    response,
                    [
                        {
                            'entity': 'offer',
                            'wareId': blue_offer.waremd5,
                            'prices': {
                                'value': str(promo_price),
                                'discount': {
                                    'percent': calc_discount_percent(promo_price, blue_offer.price),
                                    'oldMin': str(blue_offer.price),
                                },
                            },
                            'promos': [
                                {
                                    'type': promo.type_name,
                                    'key': promo.key,
                                    'url': promo.url or Absent(),
                                    'startDate': NotEmpty() if promo.start_date else Absent(),
                                    'endDate': NotEmpty() if promo.start_date else Absent(),
                                    'itemsInfo': {
                                        'price': {
                                            'discount': {
                                                'percent': calc_discount_percent(promo_price, blue_offer.price),
                                                'oldMin': str(blue_offer.price),
                                            }
                                        },
                                        'constraints': {
                                            'allow_berubonus': promo.direct_discount.allow_berubonus,
                                            'allow_promocode': promo.direct_discount.allow_promocode,
                                        },
                                    },
                                }
                            ],
                        }
                    ],
                    allow_different_len=False,
                )

    def test_fast_promo_new_promo(self):
        # изначально нет промо на оффере
        self.__should_not(msku_1.sku, blue_offer_1.waremd5)

        # довозим быстрое промо
        self.dynamic.qpromos += [DynamicQPromos([promo1_fast])]

        # без флага быстропромо для новых промо - быстропромо не работает
        self.__should_not(
            msku_1.sku,
            blue_offer_1.waremd5,
            extra_param='&rearr-factors=enable_fast_promo_new_promos=0',
            trace_promo=promo1_fast,
        )

        # с флагом - промо должно быть активно
        self.__should(
            msku_1,
            blue_offer_1,
            promo1_fast,
            promo1_fast.blue_flash.items[0]['price']['value'],
            extra_rearr='enable_fast_promo_new_promos=1',
        )

    def test_fast_promo_change(self):
        self.__should_not(msku_2.sku, blue_offer_2.waremd5)
        self.__should(msku_3, blue_offer_3, promo2, promo2.blue_flash.items[0]['price']['value'])
        self.__should(msku_4, blue_offer_4, promo2, promo2.blue_flash.items[1]['price']['value'])

        # довозим быстрое промо
        self.dynamic.qpromos += [DynamicQPromos([promo2_fast])]

        self.__should(msku_2, blue_offer_2, promo2_fast, promo2_fast.blue_flash.items[0]['price']['value'])
        self.__should(msku_3, blue_offer_3, promo2_fast, promo2_fast.blue_flash.items[1]['price']['value'])
        self.__should_not(msku_4.sku, blue_offer_4.waremd5)

    def test_fast_promo_switch_off(self):
        # изначально есть промо
        self.__should(msku_5, blue_offer_5, promo3, promo3.blue_flash.items[0]['price']['value'])

        # довозим быстрое промо
        self.dynamic.qpromos += [DynamicQPromos([promo3_fast])]

        # промо должно отключиться
        self.__should_not(msku_5.sku, blue_offer_5.waremd5)

    def test_fast_promo_same_promo_key(self):
        # изначально есть промо
        self.__should(msku_6, blue_offer_6, promo4, promo4.blue_flash.items[0]['price']['value'])

        # довозим быстрое промо
        self.dynamic.qpromos += [DynamicQPromos([promo4_fast])]

        # промо должно быть заменено
        self.__should(msku_6, blue_offer_6, promo4_fast, promo4_fast.blue_flash.items[0]['price']['value'])

    def test_secondaries_with_fast_promos(self):
        def __check_collect_promos_secondary(feed, offers):
            # выдача должна работать независимо от rearr флагов, для проверки сбрасываем все акционные флаги
            response = self.report.request_json("place=collect_promo_secondary_items")
            self.assertFragmentIn(
                response,
                {
                    "result": [
                        {
                            "feed": feed,
                            "offers": offers,
                        }
                    ],
                },
                allow_different_len=False,
            )

        # изначально есть вторичка есть на индексных промо
        __check_collect_promos_secondary(FEED, [blue_offer_8.offerid])

        # довозим быстрое промо
        self.dynamic.qpromos += [DynamicQPromos([promo5_fast])]

        # индексная вторичка должна остаться, быстрая вторичка должна появиться
        __check_collect_promos_secondary(FEED, [blue_offer_8.offerid, blue_offer_A.offerid])

        # проверка того, что вторичные промо-типы проставляются для быстрых промо
        request = 'place=prime&regset=0&pp=18&offerid={}'.format(blue_offer_A.waremd5)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': blue_offer_A.waremd5,
                    'promos': [
                        {
                            'type': PromoType.BLUE_SET_SECONDARY,
                            'key': promo5_fast.key,
                        }
                    ],
                }
            ],
            allow_different_len=False,
        )

    @skip("эмуляция быстро-лендингов удалена как нерабочая")
    def test_fast_landing(self):
        def __make_response(waremd5, promo_key=None):
            return {
                'entity': 'offer',
                'wareId': waremd5,
                'promos': Absent() if promo_key is None else [{'key': promo_key}],
            }

        # довозим быстрые промо
        self.dynamic.qpromos += [DynamicQPromos([promo6_fast, promo7_fast, promo9_fast])]

        # проверяем разные способы привязки
        flag_land = '&rearr-factors=enable_fast_promo_landing={}'
        flag_new_promos = '&rearr-factors=enable_fast_promo_new_promos={}'
        request = 'place=prime&regset=0&pp=18&shop-promo-id={}'.format(promo6_fast.shop_promo_id)

        for land, new_promos, n_promos in ((0, 0, 0), (0, 1, 0), (1, 0, 0), (1, 1, 6)):
            response = self.report.request_json(request + flag_land.format(land) + flag_new_promos.format(new_promos))
            self.assertFragmentIn(response, {'search': {'totalOffers': n_promos}})

            if land and new_promos:
                for waremd5 in (
                    blue_offer_100.waremd5,  # hid + msku + foid
                    blue_offer_101.waremd5,  # msku
                    blue_offer_102.waremd5,  # foid
                    blue_offer_103.waremd5,  # supplier
                    blue_offer_104.waremd5,  # vendor
                    blue_offer_105.waremd5,  # warehouse
                    # blue_offer_106.waremd5,  # warehouse, но запрет по MSKU!
                ):
                    self.assertFragmentIn(response, __make_response(waremd5, promo6_fast.key))

        # проверяем разные способы привязки
        request = 'place=prime&regset=0&pp=18&shop-promo-id={}&hid={}'.format(promo7_fast.shop_promo_id, HID_200)
        response = self.report.request_json(request + flag_land.format(1) + flag_new_promos.format(1))
        self.assertFragmentIn(response, {'search': {'totalOffers': 2}})
        for waremd5 in (
            blue_offer_200.waremd5,  # только офферы категории HID_200
            blue_offer_201.waremd5,
        ):
            self.assertFragmentIn(response, __make_response(waremd5, promo7_fast.key))

        # лендинг "длинного" промо - выборка по промо должна сработать
        request = 'place=prime&regset=0&pp=18&shop-promo-id={}&puid=77'.format(promo9_fast.shop_promo_id)
        response = self.report.request_json(request + flag_land.format(1) + flag_new_promos.format(1))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'totalOffersBeforeFilters': 900,  # литералов по feed_offer_id - 900
                    'totalOffers': 60,  # прюнинг по кол-ву офферов - 60
                    'results': [__make_response(NotEmpty(), promo9_fast.key)],
                }
            },
        )

        # проверяем ограничение - быстро-лендинги работают только для одного shop_promo_id
        request = 'place=prime&regset=0&pp=18&shop-promo-id={},{}'.format(
            promo6_fast.shop_promo_id, promo7_fast.shop_promo_id
        )
        response = self.report.request_json(request + flag_land.format(1) + flag_new_promos.format(1))
        self.assertFragmentIn(response, {'search': {'totalOffers': 0}})

        # проверяем ограничение - быстро-лендинги НЕ работают для выборки по промо-типу
        request = 'place=prime&regset=0&pp=18&promo-type={}'.format(PromoType.CHEAPEST_AS_GIFT)
        response = self.report.request_json(request + flag_land.format(1) + flag_new_promos.format(1))
        self.assertFragmentIn(response, {'search': {'totalOffers': 0}})

    def test_no_fast_promo_without_loaylty_whitelist(self):
        self.__should(msku_300, blue_offer_300, promo8, promo8.blue_flash.items[0]['price']['value'])

        # довозим быстрое промо
        self.dynamic.qpromos += [DynamicQPromos([promo8_fast])]

        # промо не должно поменяться
        self.__should(msku_300, blue_offer_300, promo8, promo8.blue_flash.items[0]['price']['value'])

    def test_fast_promo_on_express(self):
        """
        Тест на работу быстрых промо с признаком экспресс доставки
        """

        # изначально нет промо на оффере
        self.__should_not(msku_blue_express.sku, blue_offer_express.waremd5, rids=RIDS_MSK)
        self.__should_not(msku_blue_no_express.sku, blue_offer_no_express.waremd5, rids=RIDS_MSK)
        self.__should_not(msku_dsbs_no_express.sku, dsbs_offer_no_express.waremd5, rids=RIDS_MSK)
        self.__should_not(msku_white_no_express.sku, white_offer_no_express.waremd5, rids=RIDS_MSK)

        # довозим быстрое промо
        self.dynamic.qpromos += [DynamicQPromos([promo10_fast_express, promo11_fast_no_express])]

        # с флагом - промо должно быть активно
        self.__should_be_promocode(msku_blue_express.sku, blue_offer_express, promo10_fast_express)
        self.__should_be_promocode(msku_blue_no_express.sku, blue_offer_no_express, promo11_fast_no_express)
        self.__should_be_promocode(msku_dsbs_no_express.sku, dsbs_offer_no_express, promo11_fast_no_express)
        self.__should_not(msku_white_no_express.sku, white_offer_no_express.waremd5)

    def test_fast_promo_on_dsbs(self):
        """
        Тест на работу быстрых промо с признаком DSBS поставщика
        """

        # изначально нет промо на оффере
        self.__should_not(msku_include_dsbs.sku, offer_include_dsbs.waremd5, rids=RIDS_MSK)
        self.__should_not(msku_exclude_dsbs.sku, offer_exclude_dsbs.waremd5, rids=RIDS_MSK)

        # довозим быстрое промо
        self.dynamic.qpromos += [DynamicQPromos([promo12_fast_include_dsbs, promo13_fast_exclude_dsbs, promo14_fast])]

        # с флагом - промо должно быть активно
        self.__should_be_promocode(msku_include_dsbs.sku, offer_include_dsbs, promo12_fast_include_dsbs)
        self.__should_not(msku_exclude_dsbs.sku, offer_exclude_dsbs.waremd5, rids=RIDS_MSK)

    def test_fast_personal_promo(self):
        self.__should_not(msku_1151.sku, blue_offer_1151.waremd5)
        self.__should_not(msku_1152.sku, blue_offer_1152.waremd5)

        self.dynamic.qpromos += [DynamicQPromos([promo15_fast])]

        self.__should_be_direct_discount(
            msku_1151,
            blue_offer_1151,
            promo15_fast,
            930,
            'enable_fast_promo_new_promos=1;exp_perks=perk1,perk2,perk3;exp_perks=yalogin',
        )
        self.__should_be_direct_discount(
            msku_1152,
            blue_offer_1152,
            promo15_fast,
            1500,
            'enable_fast_promo_new_promos=1;exp_perks=perk1,perk2,perk3;exp_perks=yalogin',
        )

        self.dynamic.qpromos += [DynamicQPromos([promo15_fast_off])]

        self.__should_not(msku_1151.sku, blue_offer_1151.waremd5)
        self.__should_not(msku_1152.sku, blue_offer_1152.waremd5)


if __name__ == '__main__':
    main()
