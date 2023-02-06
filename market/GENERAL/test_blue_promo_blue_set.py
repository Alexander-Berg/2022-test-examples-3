#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent, NotEmpty
from core.report import REQUEST_TIMESTAMP
from core.testcase import TestCase, main
from core.types import DynamicBlueGenericBundlesPromos, DynamicSkuOffer, Promo, PromoType, Shop
from core.types.dynamic_filters import DynamicBluePromosBlacklist, DynamicPromoSecondaries
from core.types.sku import MarketSku, BlueOffer
from core.types.offer_promo import OffersMatchingRules, PromoBlueSet
from core.types.autogen import b64url_md5

from datetime import datetime, timedelta
from itertools import count
from math import floor


now = datetime.fromtimestamp(REQUEST_TIMESTAMP)
delta_big = timedelta(days=1)
delta_small = timedelta(hours=5)  # похоже что лайт-тесты криво работают с временной зоной…


FEED = 777

BLUE = 'blue'
GREEN = 'green'


nummer = count()


def __blue_offer(price=1000, old_price=1000, min_quantity=None):
    num = next(nummer)
    return BlueOffer(
        waremd5=b64url_md5(num),
        price=price,
        fesh=FEED,
        feedid=FEED,
        offerid='ССКУ_{}'.format(num),
        min_quantity=min_quantity,
    )


# участвует в акции 1 (с двумя вторичными офферами)
blue_offer_1 = __blue_offer(price=1215, old_price=2000)
blue_offer_2 = __blue_offer(price=2400)
blue_offer_3 = __blue_offer(price=4242)
blue_offer_1_1 = __blue_offer()
blue_offer_1_2 = __blue_offer()

# участвует в акции 2
blue_offer_4 = __blue_offer()

# участвует в акции 3
blue_offer_5 = __blue_offer()

# участвует в акции которая не в белом списке
blue_offer_6 = __blue_offer()

# в акции для проверки связанности
blue_offer_7 = __blue_offer()
blue_offer_8 = __blue_offer()

# цена <1 после скидки
blue_offer_9 = __blue_offer(price=1)
blue_offer_A = __blue_offer()

# вторичный оффер в связанном комклекте
blue_offer_B_1 = __blue_offer()
blue_offer_B_2 = __blue_offer()

# оффера для проверки вариаций
blue_offer_variations_1 = __blue_offer(price=1000)
blue_offer_variations_1_1 = __blue_offer(price=1100)
blue_offer_variations_1_2 = __blue_offer(price=1200)
blue_offer_no_variations_1 = __blue_offer(price=1000)
blue_offer_no_variations_1_1 = __blue_offer(price=1100)

blue_offer_variations_2 = __blue_offer(price=2000)
blue_offer_variations_2_1 = __blue_offer(price=2100)
blue_offer_variations_2_2 = __blue_offer(price=2200)
blue_offer_variations_2_3 = __blue_offer(price=2300)

blue_offer_variations_3 = __blue_offer(price=3000)
blue_offer_variations_3_1 = __blue_offer(price=3100)
blue_offer_variations_3_2 = __blue_offer(price=3200)
blue_offer_variations_3_3 = __blue_offer(price=3300)

blue_offer_variations_missing_1 = __blue_offer(price=1000)
blue_offer_variations_missing_1_1 = __blue_offer(price=1100)
blue_offer_variations_missing_1_2 = __blue_offer(price=1200)
blue_offer_variations_missing_1_3 = __blue_offer(price=1300)

blue_offer_variations_quant_secondary_1 = __blue_offer(price=1000, min_quantity=3)
blue_offer_variations_quant_secondary_1_1 = __blue_offer(price=1100)
blue_offer_variations_quant_secondary_1_2 = __blue_offer(price=1200)
blue_offer_variations_quant_secondary_1_3 = __blue_offer(price=1300, min_quantity=5)

blue_offer_multiple_secondary_main_1 = __blue_offer(price=1000)
blue_offer_multiple_secondary_main_2 = __blue_offer(price=1000)
blue_offer_multiple_secondary_main_3 = __blue_offer(price=1000)

blue_offer_multiple_secondary_1 = __blue_offer(price=1000)
blue_offer_multiple_secondary_2 = __blue_offer(price=1000)

# оффера для проверки ошибки связанной с дробными числами (Когда процент)
blue_offer_float_discount_1 = __blue_offer(price=1)
blue_offer_float_discount_1_1 = __blue_offer(price=1)
blue_offer_float_discount_1_2 = __blue_offer(price=1)

blue_offer_float_discount_2 = __blue_offer(price=10)
blue_offer_float_discount_2_1 = __blue_offer(price=10)
blue_offer_float_discount_2_2 = __blue_offer(price=10)

blue_offer_float_discount_3 = __blue_offer(price=100)
blue_offer_float_discount_3_1 = __blue_offer(price=100)
blue_offer_float_discount_3_2 = __blue_offer(price=100)

blue_offer_float_discount_4 = __blue_offer(price=1000)
blue_offer_float_discount_4_1 = __blue_offer(price=1000)
blue_offer_float_discount_4_2 = __blue_offer(price=1000)

blue_offer_float_discount_5 = __blue_offer(price=10000)
blue_offer_float_discount_5_1 = __blue_offer(price=10000)
blue_offer_float_discount_5_2 = __blue_offer(price=10000)

blue_offer_float_discount_6 = __blue_offer(price=100000)
blue_offer_float_discount_6_1 = __blue_offer(price=100000)
blue_offer_float_discount_6_2 = __blue_offer(price=100000)


def __msku(offer):
    num = next(nummer)
    return MarketSku(sku=num, hyperid=num, blue_offers=[offer])


msku_1 = __msku(blue_offer_1)
msku_2 = __msku(blue_offer_2)
msku_3 = __msku(blue_offer_3)
msku_1_1 = __msku(blue_offer_1_1)
msku_1_2 = __msku(blue_offer_1_2)
msku_4 = __msku(blue_offer_4)
msku_5 = __msku(blue_offer_5)
msku_6 = __msku(blue_offer_6)
msku_7 = __msku(blue_offer_7)
msku_8 = __msku(blue_offer_8)
msku_9 = __msku(blue_offer_9)
msku_A = __msku(blue_offer_A)
msku_B_1 = __msku(blue_offer_B_1)
msku_B_2 = __msku(blue_offer_B_2)
msku_variations_1 = __msku(blue_offer_variations_1)
msku_variations_1_1 = __msku(blue_offer_variations_1_1)
msku_variations_1_2 = __msku(blue_offer_variations_1_2)
msku_no_variations_1 = __msku(blue_offer_no_variations_1)
msku_no_variations_1_1 = __msku(blue_offer_no_variations_1_1)
msku_variations_2 = __msku(blue_offer_variations_2)
msku_variations_2_1 = __msku(blue_offer_variations_2_1)
msku_variations_2_2 = __msku(blue_offer_variations_2_2)
msku_variations_2_3 = __msku(blue_offer_variations_2_3)
msku_variations_3 = __msku(blue_offer_variations_3)
msku_variations_3_1 = __msku(blue_offer_variations_3_1)
msku_variations_3_2 = __msku(blue_offer_variations_3_2)
msku_variations_3_3 = __msku(blue_offer_variations_3_3)
msku_variations_missing_1 = __msku(blue_offer_variations_missing_1)
msku_variations_missing_1_1 = __msku(blue_offer_variations_missing_1_1)
msku_variations_missing_1_2 = __msku(blue_offer_variations_missing_1_2)
msku_variations_quant_secondary_1 = __msku(blue_offer_variations_quant_secondary_1)
msku_variations_quant_secondary_1_1 = __msku(blue_offer_variations_quant_secondary_1_1)
msku_variations_quant_secondary_1_2 = __msku(blue_offer_variations_quant_secondary_1_2)
msku_variations_quant_secondary_1_3 = __msku(blue_offer_variations_quant_secondary_1_3)
msku_multiple_secondary_main_1 = __msku(blue_offer_multiple_secondary_main_1)
msku_multiple_secondary_main_2 = __msku(blue_offer_multiple_secondary_main_2)
msku_multiple_secondary_main_3 = __msku(blue_offer_multiple_secondary_main_3)
msku_multiple_secondary_1 = __msku(blue_offer_multiple_secondary_1)
msku_multiple_secondary_2 = __msku(blue_offer_multiple_secondary_2)
msku_float_discount_1 = __msku(blue_offer_float_discount_1)
msku_float_discount_1_1 = __msku(blue_offer_float_discount_1_1)
msku_float_discount_1_2 = __msku(blue_offer_float_discount_1_2)
msku_float_discount_2 = __msku(blue_offer_float_discount_2)
msku_float_discount_2_1 = __msku(blue_offer_float_discount_2_1)
msku_float_discount_2_2 = __msku(blue_offer_float_discount_2_2)
msku_float_discount_3 = __msku(blue_offer_float_discount_3)
msku_float_discount_3_1 = __msku(blue_offer_float_discount_3_1)
msku_float_discount_3_2 = __msku(blue_offer_float_discount_3_2)
msku_float_discount_4 = __msku(blue_offer_float_discount_4)
msku_float_discount_4_1 = __msku(blue_offer_float_discount_4_1)
msku_float_discount_4_2 = __msku(blue_offer_float_discount_4_2)
msku_float_discount_5 = __msku(blue_offer_float_discount_5)
msku_float_discount_5_1 = __msku(blue_offer_float_discount_5_1)
msku_float_discount_5_2 = __msku(blue_offer_float_discount_5_2)
msku_float_discount_6 = __msku(blue_offer_float_discount_6)
msku_float_discount_6_1 = __msku(blue_offer_float_discount_6_1)
msku_float_discount_6_2 = __msku(blue_offer_float_discount_6_2)


# действующая акция
promo1 = Promo(
    promo_type=PromoType.BLUE_SET,
    feed_id=FEED,
    key=b64url_md5(next(nummer)),
    url='http://яндекс.рф/',
    shop_promo_id='акция номер один',
    blue_set=PromoBlueSet(
        sets_content=[
            {
                'items': [
                    {'offer_id': blue_offer_1.offerid, 'discount': 5},
                    {'offer_id': blue_offer_2.offerid, 'discount': 10},
                    {'offer_id': blue_offer_3.offerid, 'discount': 15},
                ],
                'linked': True,
            },
            {
                'items': [
                    {'offer_id': blue_offer_1_1.offerid},
                    {'offer_id': blue_offer_1_2.offerid},
                ],
            },
        ],
        restrict_refund=True,
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[OffersMatchingRules(mskus=[msku_1, msku_1_1, msku_1_2, msku_2, msku_3])],
)

# ещё не началась
promo2 = Promo(
    promo_type=PromoType.BLUE_SET,
    feed_id=FEED,
    key=b64url_md5(next(nummer)),
    start_date=now + delta_small,
    blue_set=PromoBlueSet(
        sets_content=[
            {
                'items': [
                    {'offer_id': blue_offer_4.offerid},
                ],
            }
        ],
    ),
    offers_matching_rules=[OffersMatchingRules(mskus=[msku_4])],
)

# уже закончилась
promo3 = Promo(
    promo_type=PromoType.BLUE_SET,
    feed_id=FEED,
    key=b64url_md5(next(nummer)),
    end_date=now - delta_small,
    blue_set=PromoBlueSet(
        sets_content=[
            {
                'items': [
                    {'offer_id': blue_offer_5.offerid},
                ],
            }
        ],
    ),
    offers_matching_rules=[OffersMatchingRules(mskus=[msku_5])],
)

# акция не включена в белом списке лоялти
promo4 = Promo(
    promo_type=PromoType.BLUE_SET,
    feed_id=FEED,
    key=b64url_md5(next(nummer)),
    blue_set=PromoBlueSet(
        sets_content=[
            {
                'items': [
                    {'offer_id': blue_offer_6.offerid},
                ],
            }
        ],
    ),
    offers_matching_rules=[OffersMatchingRules(mskus=[msku_6])],
)

# тест флажка связности
promo5 = Promo(
    promo_type=PromoType.BLUE_SET,
    feed_id=FEED,
    key=b64url_md5(next(nummer)),
    url='http://яндекс.рф/',
    blue_set=PromoBlueSet(
        sets_content=[
            {
                'items': [
                    {'offer_id': blue_offer_7.offerid, 'discount': 15},
                    {'offer_id': blue_offer_8.offerid},
                ],
                'linked': False,
            }
        ],
    ),
    offers_matching_rules=[OffersMatchingRules(mskus=[msku_7, msku_8])],
)


# цена <1 после скидки
promo6 = Promo(
    promo_type=PromoType.BLUE_SET,
    feed_id=FEED,
    key=b64url_md5(next(nummer)),
    url='http://яндекс.рф/',
    blue_set=PromoBlueSet(
        sets_content=[
            {
                'items': [
                    {'offer_id': blue_offer_9.offerid, 'discount': 99},
                    {'offer_id': blue_offer_A.offerid},
                ],
            }
        ],
    ),
    offers_matching_rules=[OffersMatchingRules(mskus=[msku_9, msku_A])],
)

# тест флажка связности по вторичному офферу
promo7 = Promo(
    promo_type=PromoType.BLUE_SET,
    feed_id=FEED,
    key=b64url_md5(next(nummer)),
    url='http://яндекс.рф/',
    blue_set=PromoBlueSet(
        sets_content=[
            {
                'items': [
                    {'offer_id': blue_offer_B_1.offerid, 'discount': 15},
                    {'offer_id': blue_offer_B_2.offerid},
                ],
                'linked': True,
            }
        ],
    ),
    offers_matching_rules=[OffersMatchingRules(mskus=[msku_B_1, msku_B_2])],
)

promo_variations_1_additional_offer = Promo(
    promo_type=PromoType.BLUE_SET,
    feed_id=FEED,
    key=b64url_md5(nummer.next()),
    url='http://яндекс.рф/',
    shop_promo_id='акция номер вариация один',
    blue_set=PromoBlueSet(
        sets_content=[
            {
                'items': [
                    {'offer_id': blue_offer_variations_1.offerid, 'discount': 5},
                    {'offer_id': blue_offer_variations_1_1.offerid, 'discount': 10},
                ],
                'linked': False,
            },
            {
                'items': [
                    {'offer_id': blue_offer_variations_1.offerid, 'discount': 5},
                    {'offer_id': blue_offer_variations_1_2.offerid, 'discount': 10},
                ],
                'linked': False,
            },
            {
                'items': [
                    {'offer_id': blue_offer_no_variations_1.offerid, 'discount': 5},
                    {'offer_id': blue_offer_no_variations_1_1.offerid, 'discount': 10},
                ],
                'linked': False,
            },
        ],
        restrict_refund=True,
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            mskus=[
                msku_variations_1,
                msku_variations_1_1,
                msku_variations_1_2,
                msku_no_variations_1,
                msku_no_variations_1_1,
            ]
        )
    ],
)

promo_variations_2_additional_offers = Promo(
    promo_type=PromoType.BLUE_SET,
    feed_id=FEED,
    key=b64url_md5(nummer.next()),
    url='http://яндекс.рф/',
    shop_promo_id='акция номер вариация два',
    blue_set=PromoBlueSet(
        sets_content=[
            {
                'items': [
                    {'offer_id': blue_offer_variations_2.offerid, 'discount': 5},
                    {'offer_id': blue_offer_variations_2_1.offerid, 'discount': 10},
                    {'offer_id': blue_offer_variations_2_2.offerid, 'discount': 10},
                ],
                'linked': False,
            },
            {
                'items': [
                    {'offer_id': blue_offer_variations_2.offerid, 'discount': 5},
                    {'offer_id': blue_offer_variations_2_1.offerid, 'discount': 10},
                    {'offer_id': blue_offer_variations_2_3.offerid, 'discount': 10},
                ],
                'linked': False,
            },
            {
                'items': [
                    {'offer_id': blue_offer_variations_2.offerid, 'discount': 5},
                    {'offer_id': blue_offer_variations_2_2.offerid, 'discount': 10},
                    {'offer_id': blue_offer_variations_2_3.offerid, 'discount': 10},
                ],
                'linked': False,
            },
        ],
        restrict_refund=True,
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_variations_2, msku_variations_2_1, msku_variations_2_2, msku_variations_2_3])
    ],
)

promo_variations_missing_sendaries = Promo(
    promo_type=PromoType.BLUE_SET,
    feed_id=FEED,
    key=b64url_md5(nummer.next()),
    url='http://яндекс.рф/',
    shop_promo_id='акция тест отсутсвующих дополнительных товаров',
    blue_set=PromoBlueSet(
        sets_content=[
            {
                'items': [
                    {'offer_id': blue_offer_variations_missing_1.offerid, 'discount': 5},
                    {'offer_id': blue_offer_variations_missing_1_1.offerid, 'discount': 10},
                    {'offer_id': blue_offer_variations_missing_1_2.offerid, 'discount': 10},
                ],
                'linked': False,
            },
            {
                'items': [
                    {'offer_id': blue_offer_variations_missing_1.offerid, 'discount': 5},
                    {'offer_id': blue_offer_variations_missing_1_1.offerid, 'discount': 10},
                    {'offer_id': blue_offer_variations_missing_1_3.offerid, 'discount': 10},
                ],
                'linked': False,
            },
            {
                'items': [
                    {'offer_id': blue_offer_variations_missing_1.offerid, 'discount': 5},
                    {'offer_id': blue_offer_variations_missing_1_2.offerid, 'discount': 10},
                    {'offer_id': blue_offer_variations_missing_1_3.offerid, 'discount': 10},
                ],
                'linked': False,
            },
        ],
        restrict_refund=True,
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_variations_missing_1, msku_variations_missing_1_1, msku_variations_missing_1_2])
    ],
)

promo_variations_quant_secondary = Promo(
    promo_type=PromoType.BLUE_SET,
    feed_id=FEED,
    key=b64url_md5(nummer.next()),
    url='http://яндекс.рф/',
    shop_promo_id='акция тест невалидного (квантового) дополнительного оффера',
    blue_set=PromoBlueSet(
        sets_content=[
            {
                'items': [
                    {'offer_id': blue_offer_variations_quant_secondary_1.offerid, 'discount': 5},
                    {'offer_id': blue_offer_variations_quant_secondary_1_1.offerid, 'discount': 10},
                    {'offer_id': blue_offer_variations_quant_secondary_1_2.offerid, 'discount': 10},
                ],
                'linked': False,
            },
            {
                'items': [
                    {'offer_id': blue_offer_variations_quant_secondary_1.offerid, 'discount': 5},
                    {'offer_id': blue_offer_variations_quant_secondary_1_1.offerid, 'discount': 10},
                    {'offer_id': blue_offer_variations_quant_secondary_1_3.offerid, 'discount': 10},
                ],
                'linked': False,
            },
            {
                'items': [
                    {'offer_id': blue_offer_variations_quant_secondary_1.offerid, 'discount': 5},
                    {'offer_id': blue_offer_variations_quant_secondary_1_2.offerid, 'discount': 10},
                    {'offer_id': blue_offer_variations_quant_secondary_1_3.offerid, 'discount': 10},
                ],
                'linked': False,
            },
        ],
        restrict_refund=True,
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            mskus=[
                msku_variations_quant_secondary_1,
                msku_variations_quant_secondary_1_1,
                msku_variations_quant_secondary_1_2,
                msku_variations_quant_secondary_1_3,
            ]
        )
    ],
)

promo_variations_frozen_item = Promo(
    promo_type=PromoType.BLUE_SET,
    feed_id=FEED,
    key=b64url_md5(nummer.next()),
    url='http://яндекс.рф/',
    shop_promo_id='акция номер вариация три',
    blue_set=PromoBlueSet(
        sets_content=[
            {
                'items': [
                    {'offer_id': blue_offer_variations_3.offerid, 'discount': 5},
                    {'offer_id': blue_offer_variations_3_1.offerid, 'discount': 10},
                    {'offer_id': blue_offer_variations_3_2.offerid, 'discount': 10},
                ],
                'linked': False,
            },
            {
                'items': [
                    {'offer_id': blue_offer_variations_3.offerid, 'discount': 5},
                    {'offer_id': blue_offer_variations_3_1.offerid, 'discount': 10},
                    {'offer_id': blue_offer_variations_3_3.offerid, 'discount': 10},
                ],
                'linked': False,
            },
        ],
        restrict_refund=True,
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(mskus=[msku_variations_3, msku_variations_3_1, msku_variations_3_2, msku_variations_3_3])
    ],
)

promo_float_discount = Promo(
    promo_type=PromoType.BLUE_SET,
    feed_id=FEED,
    key=b64url_md5(nummer.next()),
    url='http://яндекс.рф/',
    shop_promo_id='акция тест дробных чисел в скидке',
    blue_set=PromoBlueSet(
        sets_content=[
            {
                'items': [
                    {'offer_id': blue_offer_float_discount_1.offerid, 'discount': 0},
                    {'offer_id': blue_offer_float_discount_1_1.offerid, 'discount': 0},
                    {'offer_id': blue_offer_float_discount_1_2.offerid, 'discount': 0},
                ],
                'linked': False,
            },
            {
                'items': [
                    {'offer_id': blue_offer_float_discount_2.offerid, 'discount': 0},
                    {'offer_id': blue_offer_float_discount_2_1.offerid, 'discount': 0},
                    {'offer_id': blue_offer_float_discount_2_2.offerid, 'discount': 0},
                ],
                'linked': False,
            },
            {
                'items': [
                    {'offer_id': blue_offer_float_discount_3.offerid, 'discount': 0},
                    {'offer_id': blue_offer_float_discount_3_1.offerid, 'discount': 0},
                    {'offer_id': blue_offer_float_discount_3_2.offerid, 'discount': 0},
                ],
                'linked': False,
            },
            {
                'items': [
                    {'offer_id': blue_offer_float_discount_4.offerid, 'discount': 0},
                    {'offer_id': blue_offer_float_discount_4_1.offerid, 'discount': 0},
                    {'offer_id': blue_offer_float_discount_4_2.offerid, 'discount': 0},
                ],
                'linked': False,
            },
            {
                'items': [
                    {'offer_id': blue_offer_float_discount_5.offerid, 'discount': 0},
                    {'offer_id': blue_offer_float_discount_5_1.offerid, 'discount': 0},
                    {'offer_id': blue_offer_float_discount_5_2.offerid, 'discount': 0},
                ],
                'linked': False,
            },
            {
                'items': [
                    {'offer_id': blue_offer_float_discount_6.offerid, 'discount': 0},
                    {'offer_id': blue_offer_float_discount_6_1.offerid, 'discount': 0},
                    {'offer_id': blue_offer_float_discount_6_2.offerid, 'discount': 0},
                ],
                'linked': False,
            },
        ],
        restrict_refund=True,
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            mskus=[
                msku_float_discount_1,
                msku_float_discount_1_1,
                msku_float_discount_1_2,
                msku_float_discount_2,
                msku_float_discount_2_1,
                msku_float_discount_2_2,
                msku_float_discount_3,
                msku_float_discount_3_1,
                msku_float_discount_3_2,
                msku_float_discount_4,
                msku_float_discount_4_1,
                msku_float_discount_4_2,
                msku_float_discount_5,
                msku_float_discount_5_1,
                msku_float_discount_5_2,
                msku_float_discount_6,
                msku_float_discount_6_1,
                msku_float_discount_6_2,
            ]
        )
    ],
)

promo_multiple_secondary_1 = Promo(
    promo_type=PromoType.BLUE_SET,
    feed_id=FEED,
    key=b64url_md5(nummer.next()),
    url='http://яндекс.рф/',
    shop_promo_id='акция номер multiple blue-set-secondary один',
    blue_set=PromoBlueSet(
        sets_content=[
            {
                'items': [
                    {'offer_id': blue_offer_multiple_secondary_main_1.offerid, 'discount': 5},
                    {'offer_id': blue_offer_multiple_secondary_1.offerid, 'discount': 10},
                ],
                'linked': False,
            },
            {
                'items': [
                    {'offer_id': blue_offer_multiple_secondary_main_1.offerid, 'discount': 5},
                    {'offer_id': blue_offer_multiple_secondary_2.offerid, 'discount': 10},
                ],
                'linked': False,
            },
        ],
        restrict_refund=True,
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            mskus=[msku_multiple_secondary_main_1, msku_multiple_secondary_1, msku_multiple_secondary_2]
        )
    ],
)

promo_multiple_secondary_2 = Promo(
    promo_type=PromoType.BLUE_SET,
    feed_id=FEED,
    key=b64url_md5(nummer.next()),
    url='http://яндекс.рф/',
    shop_promo_id='акция номер multiple blue-set-secondary два',
    blue_set=PromoBlueSet(
        sets_content=[
            {
                'items': [
                    {'offer_id': blue_offer_multiple_secondary_main_2.offerid, 'discount': 5},
                    {'offer_id': blue_offer_multiple_secondary_1.offerid, 'discount': 10},
                ],
                'linked': False,
            },
            {
                'items': [
                    {'offer_id': blue_offer_multiple_secondary_main_2.offerid, 'discount': 5},
                    {'offer_id': blue_offer_multiple_secondary_2.offerid, 'discount': 10},
                ],
                'linked': False,
            },
        ],
        restrict_refund=True,
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            mskus=[msku_multiple_secondary_main_2, msku_multiple_secondary_1, msku_multiple_secondary_2]
        )
    ],
)

promo_multiple_secondary_3 = Promo(
    promo_type=PromoType.BLUE_SET,
    feed_id=FEED,
    key=b64url_md5(nummer.next()),
    url='http://яндекс.рф/',
    shop_promo_id='акция номер multiple blue-set-secondary три',
    blue_set=PromoBlueSet(
        sets_content=[
            {
                'items': [
                    {'offer_id': blue_offer_multiple_secondary_main_3.offerid, 'discount': 5},
                    {'offer_id': blue_offer_multiple_secondary_1.offerid, 'discount': 10},
                ],
                'linked': False,
            },
            {
                'items': [
                    {'offer_id': blue_offer_multiple_secondary_main_3.offerid, 'discount': 5},
                    {'offer_id': blue_offer_multiple_secondary_2.offerid, 'discount': 10},
                ],
                'linked': False,
            },
        ],
        restrict_refund=True,
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            mskus=[msku_multiple_secondary_main_3, msku_multiple_secondary_1, msku_multiple_secondary_2]
        )
    ],
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=1;enable_fast_promo_matcher_test=1']

        blue_offer_1.promo = [promo1]
        blue_offer_2.promo = [promo1]
        blue_offer_3.promo = [promo1]
        blue_offer_1_1.promo = [promo1]
        blue_offer_1_2.promo = [promo1]
        blue_offer_4.promo = [promo2]
        blue_offer_5.promo = [promo3]
        blue_offer_6.promo = [promo4]
        blue_offer_7.promo = [promo5]
        blue_offer_8.promo = [promo5]
        blue_offer_9.promo = [promo6]
        blue_offer_A.promo = [promo6]
        blue_offer_B_1.promo = [promo7]
        blue_offer_B_2.promo = [promo7]
        blue_offer_variations_1.promo = [promo_variations_1_additional_offer]
        blue_offer_variations_1_1.promo = [promo_variations_1_additional_offer]
        blue_offer_variations_1_2.promo = [promo_variations_1_additional_offer]
        blue_offer_no_variations_1.promo = [promo_variations_1_additional_offer]
        blue_offer_no_variations_1_1.promo = [promo_variations_1_additional_offer]
        blue_offer_variations_2.promo = [promo_variations_2_additional_offers]
        blue_offer_variations_2_1.promo = [promo_variations_2_additional_offers]
        blue_offer_variations_2_2.promo = [promo_variations_2_additional_offers]
        blue_offer_variations_2_3.promo = [promo_variations_2_additional_offers]
        blue_offer_variations_3.promo = [promo_variations_frozen_item]
        blue_offer_variations_3_1.promo = [promo_variations_frozen_item]
        blue_offer_variations_3_2.promo = [promo_variations_frozen_item]
        blue_offer_variations_3_3.promo = [promo_variations_frozen_item]
        blue_offer_variations_missing_1.promo = [promo_variations_missing_sendaries]
        blue_offer_variations_missing_1_1.promo = [promo_variations_missing_sendaries]
        blue_offer_variations_missing_1_2.promo = [promo_variations_missing_sendaries]
        blue_offer_variations_missing_1_3.promo = [promo_variations_missing_sendaries]
        blue_offer_variations_quant_secondary_1.promo = [promo_variations_quant_secondary]
        blue_offer_variations_quant_secondary_1_1.promo = [promo_variations_quant_secondary]
        blue_offer_variations_quant_secondary_1_2.promo = [promo_variations_quant_secondary]
        blue_offer_variations_quant_secondary_1_3.promo = [promo_variations_quant_secondary]
        blue_offer_multiple_secondary_main_1.promo = [promo_multiple_secondary_1]
        blue_offer_multiple_secondary_main_2.promo = [promo_multiple_secondary_2]
        blue_offer_multiple_secondary_main_3.promo = [promo_multiple_secondary_3]
        blue_offer_multiple_secondary_1.promo = [
            promo_multiple_secondary_1,
            promo_multiple_secondary_2,
            promo_multiple_secondary_3,
        ]
        blue_offer_multiple_secondary_2.promo = [
            promo_multiple_secondary_1,
            promo_multiple_secondary_2,
            promo_multiple_secondary_3,
        ]
        blue_offer_float_discount_1.promo = [promo_float_discount]
        blue_offer_float_discount_1_1.promo = [promo_float_discount]
        blue_offer_float_discount_1_2.promo = [promo_float_discount]
        blue_offer_float_discount_2.promo = [promo_float_discount]
        blue_offer_float_discount_2_1.promo = [promo_float_discount]
        blue_offer_float_discount_2_2.promo = [promo_float_discount]
        blue_offer_float_discount_3.promo = [promo_float_discount]
        blue_offer_float_discount_3_1.promo = [promo_float_discount]
        blue_offer_float_discount_3_2.promo = [promo_float_discount]
        blue_offer_float_discount_4.promo = [promo_float_discount]
        blue_offer_float_discount_4_1.promo = [promo_float_discount]
        blue_offer_float_discount_4_2.promo = [promo_float_discount]
        blue_offer_float_discount_5.promo = [promo_float_discount]
        blue_offer_float_discount_5_1.promo = [promo_float_discount]
        blue_offer_float_discount_5_2.promo = [promo_float_discount]
        blue_offer_float_discount_6.promo = [promo_float_discount]
        blue_offer_float_discount_6_1.promo = [promo_float_discount]
        blue_offer_float_discount_6_2.promo = [promo_float_discount]
        cls.index.shops += [
            Shop(fesh=777, datafeed_id=777, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
        ]

        cls.index.mskus += [
            msku_1,
            msku_2,
            msku_3,
            msku_1_1,
            msku_1_2,
            msku_4,
            msku_5,
            msku_6,
            msku_7,
            msku_8,
            msku_9,
            msku_A,
            msku_B_1,
            msku_B_2,
            msku_variations_1,
            msku_variations_1_1,
            msku_variations_1_2,
            msku_no_variations_1,
            msku_no_variations_1_1,
            msku_variations_2,
            msku_variations_2_1,
            msku_variations_2_2,
            msku_variations_2_3,
            msku_variations_3,
            msku_variations_3_1,
            msku_variations_3_2,
            msku_variations_3_3,
            msku_variations_missing_1,
            msku_variations_missing_1_1,
            msku_variations_missing_1_2,
            msku_variations_quant_secondary_1,
            msku_variations_quant_secondary_1_1,
            msku_variations_quant_secondary_1_2,
            msku_variations_quant_secondary_1_3,
            msku_multiple_secondary_main_1,
            msku_multiple_secondary_main_2,
            msku_multiple_secondary_main_3,
            msku_multiple_secondary_1,
            msku_multiple_secondary_2,
            msku_float_discount_1,
            msku_float_discount_1_1,
            msku_float_discount_1_2,
            msku_float_discount_2,
            msku_float_discount_2_1,
            msku_float_discount_2_2,
            msku_float_discount_3,
            msku_float_discount_3_1,
            msku_float_discount_3_2,
            msku_float_discount_4,
            msku_float_discount_4_1,
            msku_float_discount_4_2,
            msku_float_discount_5,
            msku_float_discount_5_1,
            msku_float_discount_5_2,
            msku_float_discount_6,
            msku_float_discount_6_1,
            msku_float_discount_6_2,
        ]

        cls.index.promos += [
            promo1,
            promo2,
            promo3,
            promo4,
            promo5,
            promo6,
            promo7,
            promo_variations_1_additional_offer,
            promo_variations_2_additional_offers,
            promo_variations_missing_sendaries,
            promo_variations_quant_secondary,
            promo_variations_frozen_item,
            promo_multiple_secondary_1,
            promo_multiple_secondary_2,
            promo_multiple_secondary_3,
            promo_float_discount,
        ]
        cls.dynamic.promo_secondaries += [
            DynamicPromoSecondaries(promos=cls.index.promos),
        ]

        cls.settings.loyalty_enabled = True
        cls.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(
                whitelist=[
                    promo1.key,
                    promo2.key,
                    promo3.key,
                    promo5.key,
                    promo6.key,
                    promo7.key,
                    promo_variations_1_additional_offer.key,
                    promo_variations_2_additional_offers.key,
                    promo_variations_missing_sendaries.key,
                    promo_variations_quant_secondary.key,
                    promo_variations_frozen_item.key,
                    promo_multiple_secondary_1.key,
                    promo_multiple_secondary_2.key,
                    promo_multiple_secondary_3.key,
                    promo_float_discount.key,
                ]
            )
        ]

    def __should(self, promo, msku, main_offer, secondary_offers, extra_rearr=''):
        def __get_promo_price(promo, offer, ignore_discount=False):
            for content in promo.blue_set.sets_content:
                for item in content.get('items'):
                    if item.get('offer_id') == offer.offerid:
                        if ignore_discount:
                            return offer.price
                        discount = item.get('discount') or 0
                        return int(offer.price * ((100 - discount) / 100.0))
            return -1

        def __calc_discount_percent(price, oldprice):
            return int(floor((1.0 - price * 1.0 / oldprice) * 100 + 0.5))

        main_price = main_offer.price
        main_promo_price = __get_promo_price(promo, main_offer)
        main_discount_percent = __calc_discount_percent(main_promo_price, main_price)
        is_items_multi = isinstance(secondary_offers[0], list)

        def __get_items_info_response(offers):
            total_price = main_promo_price + sum(map(lambda x: __get_promo_price(promo, x), offers))
            total_old_price = main_price + sum(map(lambda x: x.price, offers))
            total_discount_percent = __calc_discount_percent(total_price, total_old_price)
            offer_discount_percent = dict()

            for offer in offers:
                offer_discount_percent[offer.waremd5] = __calc_discount_percent(
                    __get_promo_price(promo, offer),
                    __get_promo_price(promo, offer, True),
                )

            items_info_response = {
                'constraints': {
                    'allow_promocode': promo.blue_set.allow_promocode,
                    'allow_berubonus': promo.blue_set.allow_berubonus,
                },
                'totalPrice': {
                    'value': str(total_price),
                    'currency': 'RUR',
                    'discount': {
                        'percent': total_discount_percent,
                        'oldMin': str(total_old_price),
                        'absolute': str(total_old_price - total_price if total_discount_percent != 0 else 0),
                    },
                },
                'primaryPrice': {
                    'value': str(main_promo_price),
                    'currency': 'RUR',
                    'discount': {
                        'percent': main_discount_percent,
                        'oldMin': str(main_price),
                        'absolute': str(main_price - main_promo_price if main_discount_percent != 0 else 0),
                    },
                },
                'additionalOffers': [
                    {
                        'urls': {
                            'direct': NotEmpty(),
                            'cpa': NotEmpty(),
                            'encrypted': NotEmpty(),
                        },
                        'offerId': offer.waremd5,
                        'shopSku': offer.offerid,
                        'showUid': NotEmpty(),
                        'feeShow': NotEmpty(),
                        'promoPrice': {
                            'value': str(__get_promo_price(promo, offer)),
                            'currency': 'RUR',
                            'discount': {
                                'percent': offer_discount_percent[offer.waremd5],
                                'oldMin': str(__get_promo_price(promo, offer, True)),
                                'absolute': str(
                                    __get_promo_price(promo, offer, True) - __get_promo_price(promo, offer)
                                    if offer_discount_percent[offer.waremd5] != 0
                                    else 0
                                ),
                            },
                        },
                    }
                    for offer in offers
                ],
            }

            return items_info_response

        for rgb in ('blue', 'green', 'green_with_blue'):
            for place in ('sku_offers', 'prime', 'offerinfo'):
                # выбор байбокса нужен детерминированный, для этого фиксируем yandexuid (MARKETOUT-16443)
                params = 'place={place}&rids=0&regset=1&pp=18&market-sku={msku}&rgb={rgb}&rearr-factors={extra_rearr}&yandexuid=1&debug=1&show-min-quantity=1'
                response = self.report.request_json(
                    params.format(place=place, msku=msku.sku, rgb=rgb, extra_rearr=extra_rearr)
                )

                if is_items_multi:
                    items_info_response = [__get_items_info_response(offers) for offers in secondary_offers]
                else:
                    items_info_response = __get_items_info_response(secondary_offers)

                self.assertFragmentIn(
                    response,
                    [
                        {
                            'entity': 'offer',
                            'wareId': main_offer.waremd5,
                            'promos': [
                                {
                                    'type': promo.type_name,
                                    'key': promo.key,
                                    'url': promo.url,
                                    'startDate': NotEmpty() if promo.start_date >= 0 else Absent(),
                                    'endDate': NotEmpty() if promo.end_date >= 0 else Absent(),
                                    'itemsInfo': items_info_response if not is_items_multi else Absent(),
                                    'itemsInfoMulti': items_info_response if is_items_multi else Absent(),
                                }
                            ],
                        }
                    ],
                    allow_different_len=False,
                )

    def __should_not(self, msku, waremd5, extra_rearr=''):
        for rgb in ('blue', 'green', 'green_with_blue'):
            for place in ('sku_offers', 'prime', 'offerinfo'):
                params = 'place={place}&rids=0&regset=1&pp=18&market-sku={msku}&rgb={rgb}&rearr-factors={extra_rearr}&yandexuid=1'
                response = self.report.request_json(
                    params.format(place=place, msku=msku, rgb=rgb, extra_rearr=extra_rearr)
                )
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

    def test_blue_set(self):
        self.__should(promo1, msku_1, blue_offer_1, [blue_offer_2, blue_offer_3])
        self.__should(promo1, msku_2, blue_offer_2, [blue_offer_1, blue_offer_3])
        self.__should(promo1, msku_3, blue_offer_3, [blue_offer_1, blue_offer_2])

        block_shop_promo_id = 'block_shop_promo_id={},{}'.format(promo1.shop_promo_id, 'ЯNKNOWN_OTHER_SHOPPROMOID')
        self.__should_not(msku_3.sku, blue_offer_3.waremd5, block_shop_promo_id)

        self.dynamic.disabled_sku_offers += [
            DynamicSkuOffer(shop_id=FEED, sku=blue_offer_3.offerid),
        ]
        self.__should_not(msku_1.sku, blue_offer_1.waremd5)

        # вовращаем стоки, но отключаем через чёрный список лоялти
        self.dynamic.disabled_sku_offers.clear()
        self.__should(promo1, msku_1, blue_offer_1, [blue_offer_2, blue_offer_3])
        self.__should(promo1, msku_1_1, blue_offer_1_1, [blue_offer_1_2])
        self.dynamic.loyalty += [
            DynamicBluePromosBlacklist(
                blacklist=[
                    (FEED, blue_offer_3.offerid),
                ]
            )
        ]
        self.__should_not(msku_1.sku, blue_offer_1.waremd5)
        # в чёрном списке только 1 комплект из promo1, второй должен работать
        self.__should(promo1, msku_1_1, blue_offer_1_1, [blue_offer_1_2])

    def test_promo_inactive(self):
        # проверяем отключение акций по времени
        self.__should_not(msku_4.sku, blue_offer_4.waremd5)
        self.__should_not(msku_5.sku, blue_offer_5.waremd5)
        # проверяем акция не работает без белого списка
        self.__should_not(msku_6.sku, blue_offer_6.waremd5)

    def test_unlinked(self):
        # промо-5 не связано, акция только на основном оффере
        self.__should(promo5, msku_7, blue_offer_7, [blue_offer_8])

        for place, rgb in [
            ('sku_offers', BLUE),
            ('prime', BLUE),
            ('offerinfo', BLUE),
            ('offerinfo', GREEN),
        ]:
            params = 'place={place}&rids=0&regset=1&pp=18&market-sku={msku}&rgb={rgb}&yandexuid=1'
            response = self.report.request_json(params.format(place=place, msku=msku_8.sku, rgb=rgb))
            # в промо должна быть псевдо-промо с ключом основной акции
            self.assertFragmentIn(
                response,
                [
                    {
                        'entity': 'offer',
                        'wareId': blue_offer_8.waremd5,
                        'promos': [
                            {
                                'type': PromoType.BLUE_SET_SECONDARY,
                                'key': promo5.key,
                                'startDate': NotEmpty() if promo5.start_date >= 0 else Absent(),
                                'endDate': NotEmpty() if promo5.end_date >= 0 else Absent(),
                                'itemsInfo': Absent(),
                            }
                        ],
                    }
                ],
            )

    def test_price_not_zero(self):
        # если цена после скидки <1 рубля, промо блокируется
        self.__should_not(msku_9.sku, blue_offer_9.waremd5)

    def test_linked_secondary(self):
        """
        Проверяем что для связанных комплектов при запросе в плейс productoffers на вторичных товарах промка активна
        """
        request = 'place=productoffers&rids=0&regset=1&pp=18&hyperid={hyperid}&market_sku={msku}'
        response = self.report.request_json(request.format(hyperid=msku_B_1.hyperid, msku=msku_B_1.sku))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': blue_offer_B_1.waremd5,
                            'promos': [
                                {
                                    'type': PromoType.BLUE_SET,
                                    'key': promo7.key,
                                }
                            ],
                        },
                    ]
                },
                'offers': [
                    {
                        'entity': 'offer',
                        'wareId': blue_offer_B_2.waremd5,
                        'promos': [
                            {
                                'type': PromoType.BLUE_SET,
                                'key': promo7.key,
                            }
                        ],
                    }
                ],
            },
        )
        response = self.report.request_json(request.format(hyperid=msku_B_2.hyperid, msku=msku_B_2.sku))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': blue_offer_B_2.waremd5,
                            'promos': [
                                {
                                    'type': PromoType.BLUE_SET,
                                    'key': promo7.key,
                                }
                            ],
                        },
                    ]
                },
                'offers': [
                    {
                        'entity': 'offer',
                        'wareId': blue_offer_B_1.waremd5,
                        'promos': [
                            {
                                'type': PromoType.BLUE_SET,
                                'key': promo7.key,
                            }
                        ],
                    }
                ],
            },
        )

    def test_blue_set_variations(self):
        self.__should(
            promo_variations_1_additional_offer,
            msku_variations_1,
            blue_offer_variations_1,
            [[blue_offer_variations_1_1], [blue_offer_variations_1_2]],
        )
        self.__should(
            promo_variations_1_additional_offer,
            msku_no_variations_1,
            blue_offer_no_variations_1,
            [blue_offer_no_variations_1_1],
        )

        self.__should(
            promo_variations_2_additional_offers,
            msku_variations_2,
            blue_offer_variations_2,
            [
                [blue_offer_variations_2_1, blue_offer_variations_2_2],
                [blue_offer_variations_2_1, blue_offer_variations_2_3],
                [blue_offer_variations_2_2, blue_offer_variations_2_1],
                [blue_offer_variations_2_2, blue_offer_variations_2_3],
                [blue_offer_variations_2_3, blue_offer_variations_2_1],
                [blue_offer_variations_2_3, blue_offer_variations_2_2],
            ],
        )

        self.__should(
            promo_variations_missing_sendaries,
            msku_variations_missing_1,
            blue_offer_variations_missing_1,
            [blue_offer_variations_missing_1_1, blue_offer_variations_missing_1_2],
        )

        self.__should(
            promo_variations_quant_secondary,
            msku_variations_quant_secondary_1,
            blue_offer_variations_quant_secondary_1,
            [blue_offer_variations_quant_secondary_1_1, blue_offer_variations_quant_secondary_1_2],
        )

        self.__should(
            promo_variations_quant_secondary,
            msku_variations_quant_secondary_1,
            blue_offer_variations_quant_secondary_1,
            [blue_offer_variations_quant_secondary_1_1, blue_offer_variations_quant_secondary_1_2],
            'market_hide_blue_sets_with_quantity_limited_secondaries=1',
        )

        self.__should(
            promo_variations_quant_secondary,
            msku_variations_quant_secondary_1,
            blue_offer_variations_quant_secondary_1,
            [
                [blue_offer_variations_quant_secondary_1_1, blue_offer_variations_quant_secondary_1_2],
                [blue_offer_variations_quant_secondary_1_1, blue_offer_variations_quant_secondary_1_3],
                [blue_offer_variations_quant_secondary_1_2, blue_offer_variations_quant_secondary_1_1],
                [blue_offer_variations_quant_secondary_1_2, blue_offer_variations_quant_secondary_1_3],
                [blue_offer_variations_quant_secondary_1_3, blue_offer_variations_quant_secondary_1_1],
                [blue_offer_variations_quant_secondary_1_3, blue_offer_variations_quant_secondary_1_2],
            ],
            'market_hide_blue_sets_with_quantity_limited_secondaries=0',
        )

        self.__should(
            promo_variations_frozen_item,
            msku_variations_3,
            blue_offer_variations_3,
            [
                [blue_offer_variations_3_1, blue_offer_variations_3_2],
                [blue_offer_variations_3_1, blue_offer_variations_3_3],
            ],
        )

    def test_blue_set_multiple_secondary(self):
        self.__should(
            promo_multiple_secondary_1,
            msku_multiple_secondary_main_1,
            blue_offer_multiple_secondary_main_1,
            [[blue_offer_multiple_secondary_1], [blue_offer_multiple_secondary_2]],
        )

        self.__should(
            promo_multiple_secondary_2,
            msku_multiple_secondary_main_2,
            blue_offer_multiple_secondary_main_2,
            [[blue_offer_multiple_secondary_1], [blue_offer_multiple_secondary_2]],
        )

        self.__should(
            promo_multiple_secondary_3,
            msku_multiple_secondary_main_3,
            blue_offer_multiple_secondary_main_3,
            [[blue_offer_multiple_secondary_1], [blue_offer_multiple_secondary_2]],
        )

        for place, rgb in [
            ('sku_offers', BLUE),
            ('prime', BLUE),
            ('offerinfo', BLUE),
            ('offerinfo', GREEN),
        ]:
            params = 'place={place}&rids=0&regset=1&pp=18&market-sku={msku}&rgb={rgb}&yandexuid=1'
            for current_msku, current_waremd5 in [
                (msku_multiple_secondary_1.sku, blue_offer_multiple_secondary_1.waremd5),
                (msku_multiple_secondary_2.sku, blue_offer_multiple_secondary_2.waremd5),
            ]:
                response = self.report.request_json(params.format(place=place, msku=current_msku, rgb=rgb))
                self.assertFragmentIn(
                    response,
                    [
                        {
                            'entity': 'offer',
                            'wareId': current_waremd5,
                            'promos': [
                                {
                                    'type': PromoType.BLUE_SET_SECONDARY,
                                    'key': promo_multiple_secondary_1.key,
                                },
                                {
                                    'type': PromoType.BLUE_SET_SECONDARY,
                                    'key': promo_multiple_secondary_2.key,
                                },
                                {
                                    'type': PromoType.BLUE_SET_SECONDARY,
                                    'key': promo_multiple_secondary_3.key,
                                },
                            ],
                        }
                    ],
                )

    def test_blue_set_float_discount(self):
        self.__should(
            promo_float_discount,
            msku_float_discount_1,
            blue_offer_float_discount_1,
            [blue_offer_float_discount_1_1, blue_offer_float_discount_1_2],
        )
        self.__should(
            promo_float_discount,
            msku_float_discount_2,
            blue_offer_float_discount_2,
            [blue_offer_float_discount_2_1, blue_offer_float_discount_2_2],
        )
        self.__should(
            promo_float_discount,
            msku_float_discount_3,
            blue_offer_float_discount_3,
            [blue_offer_float_discount_3_1, blue_offer_float_discount_3_2],
        )
        self.__should(
            promo_float_discount,
            msku_float_discount_4,
            blue_offer_float_discount_4,
            [blue_offer_float_discount_4_1, blue_offer_float_discount_4_2],
        )
        self.__should(
            promo_float_discount,
            msku_float_discount_5,
            blue_offer_float_discount_5,
            [blue_offer_float_discount_5_1, blue_offer_float_discount_5_2],
        )
        self.__should(
            promo_float_discount,
            msku_float_discount_6,
            blue_offer_float_discount_6,
            [blue_offer_float_discount_6_1, blue_offer_float_discount_6_2],
        )


if __name__ == '__main__':
    main()
