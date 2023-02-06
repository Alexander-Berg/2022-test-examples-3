#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.matcher import Absent, NotEmpty
from core.report import REQUEST_TIMESTAMP
from core.testcase import TestCase, main
from core.types import DynamicBlueGenericBundlesPromos
from core.types.offer import Offer
from core.types.offer_promo import (
    Promo,
    PromoType,
    PromoBlueFlash,
    PromoDirectDiscount,
    PromoRestrictions,
    calc_discount_percent,
    OffersMatchingRules,
    source_type_name,
)
from core.types.sku import MarketSku, BlueOffer
from core.types.autogen import b64url_md5
from core.types.dynamic_filters import DynamicPromoKeysBlacklist

from market.pylibrary.const.offer_promo import MechanicsPaymentType
from market.proto.common.promo_pb2 import ESourceType

from datetime import datetime, timedelta
from itertools import count
from math import floor


now = datetime.fromtimestamp(REQUEST_TIMESTAMP)
delta_big = timedelta(days=1)
delta_small = timedelta(hours=5)


FEED_ID = 777
WHITE_FEED = 888
DEFAULT_HID = 8001
HID_1 = 8002
HID_2 = 8003
PERSONAL_PERK = 'minus_424242'

nummer = count()


def get_offer_id(x):
    return 'offer_id_{}'.format(x)


def __blue_offer(offer_id, price=1000, price_old=1000, promo=None, is_fulfillment=True):
    num = next(nummer)
    promo_key = [p.shop_promo_id for p in promo] if isinstance(promo, list) else promo.shop_promo_id
    return BlueOffer(
        waremd5=b64url_md5(num),
        price=price,
        price_old=price_old,
        fesh=FEED_ID,
        feedid=FEED_ID,
        offerid=get_offer_id(offer_id),
        promo=promo,
        is_fulfillment=is_fulfillment,
        blue_promo_key=promo_key,
    )


def __msku(offers, hid):
    num = next(nummer)
    return MarketSku(sku=num, hyperid=num, hid=hid, blue_offers=offers if isinstance(offers, list) else [offers])


# Действующая акция (корзинный промокод, так как поле order_min_price непусто). Скидка по промокоду в абсолютной величинe (рублях). Не от TRADE_MARKETING
promo1 = Promo(
    promo_type=PromoType.PROMO_CODE,
    promo_code='promocode_1_text',
    description='promocode_1_description',
    discount_value=300,
    discount_currency='RUR',
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://promocode_1.com/',
    landing_url='http://promocode_1_landing.com/',
    mechanics_payment_type=MechanicsPaymentType.CPA,
    shop_promo_id='promocode_1',
    conditions='buy at least 300321',
    promo_internal_priority=3,
    restrictions=PromoRestrictions(
        order_min_price={
            'value': 4000,
            'currency': 'RUR',
        }
    ),
    source_type=ESourceType.CATEGORYIFACE,
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(1)],
                [FEED_ID, get_offer_id(2)],
                [WHITE_FEED, get_offer_id(3)],
                [FEED_ID, get_offer_id(8)],
            ]
        )
    ],
    market_division="TRADE_MARKETING",
)

# Действующая акция (корзинный промокод, так как поле order_min_price непусто). Скидка по промокоду в абсолютной величинe (рублях). От TRADE_MARKETING
promo1_trade_marketing = promo1


blue_offer_ff_virtual = __blue_offer(offer_id=1, price=890, price_old=1000, promo=promo1, is_fulfillment=True)
msku_1 = __msku(
    [
        blue_offer_ff_virtual,
    ],
    DEFAULT_HID,
)

blue_offer_no_ff_virtual = __blue_offer(offer_id=2, price=890, price_old=1000, promo=promo1, is_fulfillment=False)
msku_2 = __msku(
    [
        blue_offer_no_ff_virtual,
    ],
    DEFAULT_HID,
)

white_offer_1 = Offer(
    feedid=WHITE_FEED,
    title='white_offer_1',
    waremd5='white_offer_1--------g',
    price=890,
    promo=promo1,
    offerid=get_offer_id(3),
)

# Действующая акция. Скидка по промокоду в процентах
promo2 = Promo(
    promo_type=PromoType.PROMO_CODE,
    promo_code='promocode_2_text',
    description='promocode_2_description',
    discount_value=15,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://promocode_2.com/',
    landing_url='http://promocode_2_landing.com/',
    mechanics_payment_type=MechanicsPaymentType.CPA,
    shop_promo_id='promocode_2',
    conditions='conditions to buy',
    promo_internal_priority=4,
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(4)],
                [FEED_ID, get_offer_id(5)],
                [FEED_ID, get_offer_id(7)],
            ]
        )
    ],
)

blue_offer_ff_virtual_2 = __blue_offer(offer_id=4, price=167, price_old=500, promo=promo2, is_fulfillment=True)
msku_3 = __msku(
    [
        blue_offer_ff_virtual_2,
    ],
    DEFAULT_HID,
)

# Действующая акция blue_flash
promo3 = Promo(
    promo_type=PromoType.BLUE_FLASH,
    description='blue_flash_1_description',
    key=b64url_md5(next(nummer)),
    url='http://blue_flash_1.com/',
    blue_flash=PromoBlueFlash(
        items=[
            {'feed_id': FEED_ID, 'offer_id': get_offer_id(5), 'price': {'value': 750, 'currency': 'RUR'}},
        ],
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(5)],
            ]
        )
    ],
)

blue_offer_ff_virtual_3 = __blue_offer(
    offer_id=5, price=861, price_old=1100, promo=[promo2, promo3], is_fulfillment=True
)
msku_4 = __msku(
    [
        blue_offer_ff_virtual_3,
    ],
    DEFAULT_HID,
)

promo4 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    description='direct_discount_1_description',
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_1.com/',
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(7),
                'discount_price': {'value': 800, 'currency': 'RUR'},
                'old_price': {'value': 1100, 'currency': 'RUR'},
            }
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(7)],
            ]
        )
    ],
)

blue_offer_ff_virtual_5 = __blue_offer(
    offer_id=7, price=870, price_old=1100, promo=[promo2, promo4], is_fulfillment=True
)
msku_6 = __msku(
    [
        blue_offer_ff_virtual_5,
    ],
    DEFAULT_HID,
)

# Действующая акция (офферный промокод, так как поле conditions пусто). Скидка по промокоду в абсолютной величинe (рублях).
promo5 = Promo(
    promo_type=PromoType.PROMO_CODE,
    promo_code='promocode_5_text',
    description='promocode_5_description',
    discount_value=350,
    discount_currency='RUR',
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://promocode_5.com/',
    landing_url='http://promocode_5_landing.com/',
    mechanics_payment_type=MechanicsPaymentType.CPA,
    shop_promo_id='promocode_5',
    promo_internal_priority=3,
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(9)],
            ]
        )
    ],
)

# Действующая акция (офферный промокод, так как поле conditions пусто). Скидка по промокоду болльше стоимости товара (рублях).
promo6 = Promo(
    promo_type=PromoType.PROMO_CODE,
    promo_code='promocode_6_text',
    description='promocode_6_description',
    discount_value=550,
    discount_currency='RUR',
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://promocode_6.com/',
    landing_url='http://promocode_6_landing.com/',
    mechanics_payment_type=MechanicsPaymentType.CPA,
    shop_promo_id='promocode_6',
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(13)],
            ]
        )
    ],
)

promo_priora10 = Promo(
    promo_type=PromoType.PROMO_CODE,
    promo_code='PRIORITY10',
    description='PRIORITY10',
    discount_value=310,
    discount_currency='RUR',
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://promocode_10.com/',
    landing_url='http://promocode_10_landing.com/',
    mechanics_payment_type=MechanicsPaymentType.CPA,
    shop_promo_id='promocode_priora10',
    promo_internal_priority=10,
    source_type=ESourceType.LOYALTY,
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(12)],
            ]
        )
    ],
)

promo_priora50 = Promo(
    promo_type=PromoType.PROMO_CODE,
    promo_code='PRIORITY50',
    description='PRIORITY50',
    discount_value=350,
    discount_currency='RUR',
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://promocode_50.com/',
    landing_url='http://promocode_50_landing.com/',
    mechanics_payment_type=MechanicsPaymentType.CPA,
    shop_promo_id='promocode_priora50',
    promo_internal_priority=50,
    source_type=ESourceType.LOYALTY,
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(12)],
            ]
        )
    ],
)

promo_priora40 = Promo(
    promo_type=PromoType.PROMO_CODE,
    promo_code='PRIORITY40',
    description='PRIORITY40',
    discount_value=350,
    discount_currency='RUR',
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://promocode_40.com/',
    landing_url='http://promocode_40_landing.com/',
    mechanics_payment_type=MechanicsPaymentType.CPA,
    shop_promo_id='promocode_priora40',
    promo_internal_priority=40,
    source_type=ESourceType.LOYALTY,
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(12)],
            ]
        )
    ],
)

# "Персональное" промо (пока без перков)
promo_personal = Promo(
    promo_type=PromoType.PROMO_CODE,
    promo_code='promo_personal',
    discount_value=15,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://promo_personal.ru/',
    landing_url='http://promo_personal.ru/land',
    mechanics_payment_type=MechanicsPaymentType.CPA,
    shop_promo_id='promo_personal',
    conditions='conditions to buy',
    promo_internal_priority=4,
    source_type=ESourceType.DCO_PERSONAL,
    restrictions=PromoRestrictions(
        predicates=[
            {
                'perks': [PERSONAL_PERK],
            }
        ]
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(14)],
            ]
        )
    ],
)

# Офферы для проверки сортировки по цене
# Оффер с корзинным промокодом. При сортировке по цене на выдаче не учитываем скидку по промокоду
blue_offer_ff_virtual_6 = __blue_offer(offer_id=8, price=1100, price_old=1100, promo=[promo1], is_fulfillment=True)
msku_7 = __msku(
    [
        blue_offer_ff_virtual_6,
    ],
    HID_1,
)

# Оффер с офферным промокодом. При сортировке по цене на выдаче учитываем скидку по промокоду
blue_offer_ff_virtual_7 = __blue_offer(offer_id=9, price=1330, price_old=1330, promo=[promo5], is_fulfillment=True)
msku_8 = __msku(
    [
        blue_offer_ff_virtual_7,
    ],
    HID_1,
)

# Оффер, не участвующий в акциях. Для проверки сортировки по цене
blue_offer_ff_virtual_8 = __blue_offer(offer_id=10, price=1250, price_old=1250, promo=[], is_fulfillment=True)
msku_9 = __msku(
    [
        blue_offer_ff_virtual_8,
    ],
    HID_1,
)

# Оффер, не участвующий в акциях. Для проверки сортировки по цене
blue_offer_ff_virtual_9 = __blue_offer(offer_id=11, price=1000, price_old=1000, promo=[], is_fulfillment=True)
msku_10 = __msku(
    [
        blue_offer_ff_virtual_9,
    ],
    HID_1,
)

blue_offer_ff_virtual_10 = __blue_offer(
    offer_id=12, price=1100, price_old=1100, promo=[promo_priora10, promo_priora50, promo_priora40], is_fulfillment=True
)
msku_11 = __msku(
    [
        blue_offer_ff_virtual_10,
    ],
    HID_1,
)

# Оффер с ценой меньше абсолютной скидки промокода
blue_offer_ff_virtual_11 = __blue_offer(offer_id=13, price=500, price_old=500, promo=[promo6], is_fulfillment=True)
msku_12 = __msku(
    [
        blue_offer_ff_virtual_11,
    ],
    HID_1,
)

# оффер для персонального промо
blue_offer_personal = __blue_offer(offer_id=14, price=500, price_old=500, promo=[promo_personal], is_fulfillment=True)
msku_14 = __msku(
    [
        blue_offer_personal,
    ],
    HID_1,
)

# Действующая акция. Скидка по промокоду в процентах.
# Офферный промокод, так как поле conditions пусто. Поле order_max_price непусто.
promo_with_order_max_price_1 = Promo(
    promo_type=PromoType.PROMO_CODE,
    promo_code='promocode_with_order_max_price_1_text',
    description='promocode_with_order_max_price_1_description',
    discount_value=12,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://promocode_with_order_max_price_1.com/',
    landing_url='http://promocode_with_order_max_price_1_landing.com/',
    mechanics_payment_type=MechanicsPaymentType.CPA,
    shop_promo_id='promocode_with_order_max_price_1',
    promo_internal_priority=4,
    restrictions=PromoRestrictions(
        order_max_price={
            'value': 500,
            'currency': 'RUR',
        }
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(15)],
            ]
        )
    ],
)

# Действующая акция. Скидка по промокоду в процентах.
# Офферный промокод, так как поле conditions пусто. Поле order_max_price непусто.
promo_with_order_max_price_2 = Promo(
    promo_type=PromoType.PROMO_CODE,
    promo_code='promocode_with_order_max_price_2_text',
    description='promocode_with_order_max_price_2_description',
    discount_value=10,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://promocode_with_order_max_price_2.com/',
    landing_url='http://promocode_with_order_max_price_2_landing.com/',
    mechanics_payment_type=MechanicsPaymentType.CPA,
    shop_promo_id='promocode_with_order_max_price_2',
    promo_internal_priority=3,
    restrictions=PromoRestrictions(
        order_max_price={
            'value': 600,
            'currency': 'RUR',
        },
        bind_only_once=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(16)],
            ]
        )
    ],
)
# Величина order_max_price для промо меньше, чем цена оффера. Поэтому скидка по промокоду
# рассчитывается от order_max_price, а не от цены оффера.
blue_offer_ff_virtual_12 = __blue_offer(
    offer_id=15, price=550, price_old=600, promo=[promo_with_order_max_price_1], is_fulfillment=True
)
msku_15 = __msku(
    [
        blue_offer_ff_virtual_12,
    ],
    HID_2,
)

# Величина order_max_price для промо больше, чем цена оффера. Поэтому скидка по промокоду
# рассчитывается от цены оффера.
blue_offer_ff_virtual_13 = __blue_offer(
    offer_id=16, price=550, price_old=600, promo=[promo_with_order_max_price_2], is_fulfillment=True
)
msku_16 = __msku(
    [
        blue_offer_ff_virtual_13,
    ],
    DEFAULT_HID,
)

# Оффер, не участвующий в акциях. Для проверки сортировки по цене, в которой не должно учитываться поле order_max_price
blue_offer_ff_virtual_14 = __blue_offer(offer_id=17, price=491, price_old=700, promo=[], is_fulfillment=True)
msku_17 = __msku(
    [
        blue_offer_ff_virtual_14,
    ],
    HID_2,
)

# Действующая акция. Должна скрываться, если приходит в disabled_user_threasholds
promo_disabled_threashold = Promo(
    promo_type=PromoType.PROMO_CODE,
    promo_code='USEDPROMOCODE',
    description='used promocode',
    discount_value=350,
    discount_currency='RUR',
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://used_promocode.com/',
    landing_url='http://used_promocode_landing.com/',
    mechanics_payment_type=MechanicsPaymentType.CPA,
    shop_promo_id='used_promocode',
    source_type=ESourceType.LOYALTY,
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(18)],
            ]
        )
    ],
)

# оффер для скрытия использованных промокодов
blue_offer_disabled_threashold = __blue_offer(
    offer_id=18, price=500, price_old=500, promo=[promo_disabled_threashold], is_fulfillment=True
)
msku_18 = __msku(
    [
        blue_offer_disabled_threashold,
    ],
    HID_1,
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=1;enable_fast_promo_matcher_test=1']

        cls.index.offers += [
            white_offer_1,
        ]

        cls.index.mskus += [
            msku_1,
            msku_2,
            msku_3,
            msku_4,
            msku_6,
            msku_7,
            msku_8,
            msku_9,
            msku_10,
            msku_11,
            msku_12,
            msku_14,
            msku_15,
            msku_16,
            msku_17,
            msku_18,
        ]

        cls.index.promos += [
            promo1,
            promo2,
            promo3,
            promo4,
            promo5,
            promo6,
            promo_personal,
            promo_with_order_max_price_1,
            promo_with_order_max_price_2,
            promo_disabled_threashold,
        ]

        cls.settings.loyalty_enabled = True
        cls.dynamic.loyalty += [DynamicBlueGenericBundlesPromos(whitelist=[promo.key for promo in cls.index.promos])]

    def __calc_discount_price(self, offer, promo):
        if promo.discount_percent is None:
            return offer.price - promo.discount_value
        else:
            # round discount price according to TFixedPointNumber::roundValue function
            if (promo.restrictions is not None) and (promo.restrictions.order_max_price is not None):
                val = min(offer.price, promo.restrictions.order_max_price['value'])
                return offer.price - int(round(val * promo.discount_percent / 100))
            else:
                return int(round(offer.price * (100.0 - promo.discount_percent) / 100))

    def __check_present_promo_fragment(self, response, promo, offer, waremd5):
        def is_absolute_promocode(promo):
            if promo.discount_currency is not None:
                return True
            if (promo.restrictions is not None) and (promo.restrictions.order_max_price is not None):
                return offer.price > promo.restrictions.order_max_price['value']
            return False

        is_absolute = is_absolute_promocode(promo)
        discount_price = self.__calc_discount_price(offer, promo)
        discount_value = promo.discount_value if not is_absolute else offer.price - discount_price
        old_price = offer.price_old if offer.price_old else offer.price

        order_min_price = Absent()
        if (promo.restrictions is not None) and (promo.restrictions.order_min_price is not None):
            order_min_price = {
                'value': str(promo.restrictions.order_min_price['value']),
                'currency': promo.restrictions.order_min_price['currency'],
            }

        order_max_price = Absent()
        order_max_discount = Absent()
        if (promo.restrictions is not None) and (promo.restrictions.order_max_price is not None):
            order_max_price = {
                'value': str(promo.restrictions.order_max_price['value']),
                'currency': promo.restrictions.order_max_price['currency'],
            }
            if promo.discount_percent:
                order_max_discount = {
                    'value': str(
                        int(round(promo.restrictions.order_max_price['value'] * promo.discount_percent / 100.0))
                    ),
                    'currency': promo.restrictions.order_max_price['currency'],
                }

        # Проверяем что в выдаче есть оффер с корректным блоком 'promos'
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': waremd5,
                    'prices': {
                        'value': str(offer.price),
                        'currency': 'RUR',
                    },
                    'promos': [
                        {
                            'type': promo.type_name,
                            'key': promo.key,
                            'startDate': NotEmpty() if promo.start_date else Absent(),
                            'endDate': NotEmpty() if promo.end_date else Absent(),
                            'url': promo.url,
                            'landingUrl': promo.landing_url,
                            'mechanicsPaymentType': promo.mechanics_payment_type_as_string,
                            'promoCode': promo.promo_code,
                            'conditions': Absent() if promo.conditions is None else promo.conditions,
                            'isPersonal': True if promo.source_type == ESourceType.DCO_PERSONAL else Absent(),
                            'sourceType': source_type_name(promo.source_type),
                            'discount': {
                                'value': discount_value,
                                'currency': 'RUR' if is_absolute else Absent(),
                            },
                            'itemsInfo': {
                                'promoCode': promo.promo_code,
                                'discountType': 'absolute' if is_absolute else 'percent',
                                'conditions': Absent() if promo.conditions is None else promo.conditions,
                                'orderMinPrice': order_min_price,
                                'orderMaxPrice': order_max_price,
                                'orderMaxDiscount': order_max_discount,
                                'bindOnlyOnce': promo.restrictions.bind_only_once
                                if promo.restrictions is not None
                                else Absent(),
                                'promoPrice': {
                                    'currency': 'RUR',
                                    'value': str(discount_price),
                                    'discount': {
                                        'oldMin': str(offer.price),
                                        'percent': int(floor((1 - 1.0 * discount_price / offer.price) * 100 + 0.5)),
                                        'absolute': str(offer.price - discount_price),
                                    },
                                },
                                'promoPriceWithTotalDiscount': {
                                    'currency': 'RUR',
                                    'value': str(discount_price),
                                    'discount': {
                                        'oldMin': str(old_price),
                                        'percent': int(floor((1 - 1.0 * discount_price / old_price) * 100 + 0.5)),
                                        'absolute': str(old_price - discount_price),
                                    },
                                },
                            },
                        }
                    ],
                }
            ],
            allow_different_len=False,
        )

    def __check_absent_promo_fragment(self, response, waremd5):
        # Проверяем, что блок промо отсутствует
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

    # Проверяем, что в ответе репорта есть блок promo
    def check_promo(self, promo, msku, offer, check_present, rearr_flags=None, add_url=""):
        for rgb in ('blue', 'green', 'green_with_blue'):
            for place in ('sku_offers', 'prime'):
                for rearr_flag in rearr_flags or (None,):
                    request = 'place={place}&rids=0&regset=1&pp=18&market-sku={msku}&rgb={rgb}'
                    request = request.format(place=place, msku=msku, rgb=rgb)
                    request += add_url
                    response = self.report.request_json(request)

                    if check_present:
                        self.__check_present_promo_fragment(response, promo, offer, offer.waremd5)
                    else:
                        self.__check_absent_promo_fragment(response, offer.waremd5)

    def test_promocode_active(self):

        ACTIVE_PROMO_DATA = [
            (promo1, msku_1, blue_offer_ff_virtual),
            (promo1, msku_2, blue_offer_no_ff_virtual),
            (promo2, msku_3, blue_offer_ff_virtual_2),
            (promo1, msku_7, blue_offer_ff_virtual_6),
            (promo5, msku_8, blue_offer_ff_virtual_7),
            (promo_with_order_max_price_1, msku_15, blue_offer_ff_virtual_12),
            (promo_with_order_max_price_2, msku_16, blue_offer_ff_virtual_13),
        ]

        for promo, msku, blue_offer in ACTIVE_PROMO_DATA:
            # Флага market_promo_blue_promocode нет, промо всегда есть на выдаче
            self.check_promo(promo, msku.sku, blue_offer, True, [None, 1])

    def test_promocode_blacklist(self):
        """
        Проверяем, что при включении промо в черный список он исчезает из выдачи
        """
        self.check_promo(promo1, msku_1.sku, blue_offer_ff_virtual, True, [None, 1])
        self.dynamic.loyalty += [DynamicPromoKeysBlacklist(blacklist=[promo1.key])]  # promo_key
        self.check_promo(promo1, msku_1.sku, blue_offer_ff_virtual, False, [None, 1])

    def test_disable_promo_in_expirement(self):
        """
        Пропускаю пока не добавим MarketDivision в лайты
        Проверяем, что при включении промо в эксперименте он исчезает из выдачи
        """
        self.check_promo(
            promo1_trade_marketing, msku_1.sku, blue_offer_ff_virtual, True, [None, 1]
        )  # no expiriment -> has promo
        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            False,
            [None, 1],
            '&rearr-factors=market_promo_disabling_experiment=1;market_promo_disabling_discount_limit_if_no_puid=0',
        )  # in expiriment -> no promo
        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            True,
            [None, 1],
            '&rearr-factors=market_promo_disabling_experiment=1;market_promo_disabling_discount_limit_if_no_puid=0;market_promo_disabling_discount_from=50',
        )  # in expiriment out of interval -> has promo
        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            False,
            [None, 1],
            '&rearr-factors=market_promo_disabling_experiment=1;market_promo_disabling_discount_limit_if_no_puid=0;market_promo_disabling_discount_from=10;market_promo_disabling_discount_to=50',
        )  # in expiriment in interval -> no promo

        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            True,
            [None, 1],
            '&rearr-factors=market_promo_disabling_experiment=1&off-promo-disabling-experiment=1',
        )  # in expiriment with disabling param -> has promo
        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            True,
            [None, 1],
            '&rearr-factors=market_promo_disabling_experiment=1;market_promo_disabling_always_if_no_puid=1&off-promo-disabling-experiment=1',
        )  # in expiriment with disabling param -> has promo

        # puid tests
        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            False,
            [None, 1],
            '&rearr-factors=market_promo_disabling_experiment=1&puid=456',
        )  # default: with puid -> no promo
        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            False,
            [None, 1],
            '&rearr-factors=market_promo_disabling_experiment=1',
        )  # default: without puid -> no promo
        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            False,
            [None, 1],
            '&rearr-factors=market_promo_disabling_always_if_no_puid=1',
        )  # default: without puid not in split with flag -> no promo
        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            True,
            [None, 1],
            '&rearr-factors=market_promo_disabling_always_if_no_puid=1&puid=456',
        )  # default: with puid not in split with flag -> has promo
        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            True,
            [None, 1],
            '&rearr-factors=market_promo_disabling_experiment=1;market_promo_disabling_only_if_no_puid=1&puid=456',
        )  # with puid after disabling -> has promo

        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            True,
            [None, 1],
            '&rearr-factors=market_promo_disabling_experiment=1;market_promo_disabling_discount_from=50&puid=456',
        )  # with puid in expiriment out of interval -> has promo
        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            False,
            [None, 1],
            '&rearr-factors=market_promo_disabling_experiment=1;market_promo_disabling_discount_from=50',
        )  # without puid in expiriment out of interval -> no promo

        # размер промо в процентах
        # self.check_promo(promo1_discount_in_percent, msku_1.sku, blue_offer_ff_virtual, False, [None, 1], '&rearr-factors=market_promo_disabling_experiment=1;market_promo_disabling_discount_limit_if_no_puid=0;market_promo_disabling_discount_from=10;market_promo_disabling_discount_to=50')  # noqa

        # без market_division promo не отрывается
        # self.check_promo(promo1, msku_1.sku, blue_offer_ff_virtual, True, [None, 1], '&rearr-factors=market_promo_disabling_experiment=1&puid=456') # default: with puid -> no promo, but this promo is not from trade_marketing  # noqa

    def test_white_offer_promocode(self):
        '''
        Проверяем, что промокод для белого оффера остаётся на выдаче при различных значениях эксп-флага
        '''
        for rgb in ('green', 'green_with_blue'):
            for rearr_flag in (None, 0, 1):
                request = 'place=prime&rids=0&regset=1&pp=18&offerid={}&rgb={}'.format(white_offer_1.ware_md5, rgb)
                response = self.report.request_json(request)
                self.__check_present_promo_fragment(response, promo1, white_offer_1, white_offer_1.ware_md5)

    def test_blue_offer_promocode_priority_lower(self):
        '''
        Проверяем, что если на синем оффере одновременно два промо PromoCode и BlueFlash, то, согласно приоритетам
        на репорте, BlueFlash выигрывает.
        '''

        offer = blue_offer_ff_virtual_3
        request = 'place=prime&rids=0&regset=1&pp=18&offerid={}&rgb=blue'.format(offer.waremd5)

        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': offer.waremd5,
                    'prices': {
                        'value': '750',
                        'currency': 'RUR',
                    },
                    'promos': [
                        {
                            'type': promo3.type_name,
                            'key': promo3.key,
                            'startDate': NotEmpty() if promo3.start_date else Absent(),
                            'endDate': NotEmpty() if promo3.end_date else Absent(),
                            'url': promo3.url,
                            'itemsInfo': {
                                'promoPrice': {
                                    'currency': 'RUR',
                                    'value': '750',
                                },
                                'discount': {
                                    'oldMin': '1100',
                                    'percent': calc_discount_percent(750, offer.price_old),
                                },
                                'constraints': {
                                    'allow_berubonus': promo3.blue_flash.allow_berubonus,
                                    'allow_promocode': promo3.blue_flash.allow_promocode,
                                },
                            },
                        }
                    ],
                }
            ],
            allow_different_len=False,
        )

    def test_blue_offer_promocode_and_direct_discount(self):
        '''
        Проверяем, что если на синем оффере одновременно два промо PromoCode и BlueFlash, то они оба возвращаются в выдаче.
        '''

        offer = blue_offer_ff_virtual_5
        request = 'place=prime&rids=0&regset=1&pp=18&offerid={}&rgb=blue'.format(offer.waremd5)

        discount_price = promo4.direct_discount.items[0]['discount_price']['value']
        old_price = promo4.direct_discount.items[0]['old_price']['value']
        discount_percent = calc_discount_percent(discount_price, offer.price_old)

        # PromoCode применяется на цену, полученную после применения flash акции
        promocode_price = int(discount_price * (100 - promo2.discount_value) / 100)

        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': offer.waremd5,
                    'prices': {
                        'value': str(discount_price),
                        'currency': 'RUR',
                    },
                    'promos': [
                        {
                            'type': promo4.type_name,
                            'key': promo4.key,
                            'startDate': NotEmpty() if promo4.start_date else Absent(),
                            'endDate': NotEmpty() if promo4.end_date else Absent(),
                            'url': promo4.url,
                            'itemsInfo': {
                                'price': {
                                    'currency': 'RUR',
                                    'value': str(discount_price),
                                    'discount': {
                                        'absolute': str(old_price - discount_price),
                                        'oldMin': str(old_price),
                                        'percent': discount_percent,
                                    },
                                },
                                'constraints': {
                                    'allow_berubonus': promo4.direct_discount.allow_berubonus,
                                    'allow_promocode': promo4.direct_discount.allow_promocode,
                                },
                            },
                        },
                        {
                            'type': promo2.type_name,
                            'key': promo2.key,
                            'startDate': NotEmpty() if promo2.start_date else Absent(),
                            'endDate': NotEmpty() if promo2.end_date else Absent(),
                            'url': promo2.url,
                            'itemsInfo': {
                                'promoPrice': {
                                    'currency': 'RUR',
                                    'value': str(promocode_price),
                                    'discount': {
                                        'absolute': str(discount_price - promocode_price),
                                        'oldMin': str(discount_price),
                                        'percent': promo2.discount_value,
                                    },
                                },
                            },
                        },
                    ],
                }
            ],
            allow_different_len=False,
        )

    def test_promocode_landing(self):
        '''
        Проверяем, что promocode попадает в автолендиг
        '''
        request = 'place=prime&rids=0&pp=18&shop-promo-id={}'.format(promo1.shop_promo_id)
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'model': {'id': model_id},
                    'promos': [
                        {
                            'type': promo1.type_name,
                            'key': promo1.key,
                        }
                    ],
                }
                for model_id in [2, 4, 21]
            ],
            allow_different_len=False,
        )

    def test_multi_promocodes_priority(self):
        '''
        Проверяем, что если на офере есть несколько акций типа промокод, то они возвращается в "promos" та акция, у которой priority больше
        '''
        offer = blue_offer_ff_virtual_10
        request = 'place=prime&rids=0&regset=1&pp=18&offerid={}&rgb=blue'.format(offer.waremd5)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': offer.waremd5,
                    'promos': [
                        {
                            'type': promo_priora50.type_name,
                            'key': promo_priora50.key,
                        }
                    ],
                }
            ],
            allow_different_len=False,
        )

    def __make_offer_fragment(self, offer):
        return {
            'entity': 'product',
            'offers': {
                'items': [
                    {'entity': 'offer', 'wareId': offer.waremd5, 'prices': {'value': str(offer.price)}},
                ],
            },
        }

    def test_price_sorting(self):
        '''
        Проверяем, что при сортировке по цене скидка по корзинному промокоду не учитывается в цене,
        а скидка по офферному промокоду - учитывается.
        '''
        request = 'place=prime&pp=18&hid={}&how=aprice&rgb=blue&use-default-offers=1'.format(HID_1)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    self.__make_offer_fragment(blue_offer_ff_virtual_7),
                    self.__make_offer_fragment(blue_offer_ff_virtual_9),
                    self.__make_offer_fragment(blue_offer_ff_virtual_6),
                    self.__make_offer_fragment(blue_offer_ff_virtual_8),
                ],
            },
            preserve_order=True,
        )

    def test_price_sorting_order_max_price(self):
        '''
        Проверяем, что при сортировке по цене скидка по офферному промокоду не учитывается в цене,
        если в акции задано ограничение order_max_price.
        '''
        request = 'place=prime&pp=18&hid={}&how=aprice&rgb=green_with_blue&use-default-offers=1'.format(HID_2)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    self.__make_offer_fragment(blue_offer_ff_virtual_14),
                    self.__make_offer_fragment(blue_offer_ff_virtual_12),
                ],
            },
            preserve_order=True,
        )

    def test_negative_price(self):
        '''
        Проверяем, что цена товара считается корректно, если промокод не является корзинным и номинал промокода больше стоимости товара.
        Поле promoPrice в itemsInfo должно быть равно 1.
        '''
        offer = blue_offer_ff_virtual_11
        request = 'place=prime&rids=0&regset=1&pp=18&offerid={}&rgb=blue'.format(offer.waremd5)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': offer.waremd5,
                    'promos': Absent(),
                    'prices': {'value': str(offer.price)},
                }
            ],
            allow_different_len=False,
        )

    def test_personal_promo(self):
        cases = (
            (0, None, False),
            (0, PERSONAL_PERK, False),
            (1, None, False),
            (1, PERSONAL_PERK, True),
        )
        for rearr, perk, should in cases:
            request = 'place=prime&regset=0&pp=18&offerid={waremd5}&rgb=blue&rearr-factors=personal_promo_enabled={rearr}'.format(
                waremd5=blue_offer_personal.waremd5, rearr=rearr
            )
            if perk:
                request += '&perks={perk}'.format(perk=perk)
            response = self.report.request_json(request)
            if should:
                self.__check_present_promo_fragment(
                    response, promo_personal, blue_offer_personal, blue_offer_personal.waremd5
                )
            else:
                self.__check_absent_promo_fragment(response, blue_offer_personal.waremd5)

    def test_disabled_promo_thresholds(self):
        '''
        Проверяем, что при указании cgi параметра disabled-promo-thresholds с ключем акции акция пропадает с выдачи.
        '''
        request = 'place=prime&regset=0&pp=18&offerid={waremd5}'.format(waremd5=blue_offer_disabled_threashold.waremd5)
        response = self.report.request_json(request)
        self.__check_present_promo_fragment(
            response,
            promo_disabled_threashold,
            blue_offer_disabled_threashold,
            blue_offer_disabled_threashold.waremd5,
        )

        blockpromokey = 'blockpromo_{}_{}'.format(
            promo_disabled_threashold.shop_promo_id, promo_disabled_threashold.source_type
        )
        request = 'place=prime&regset=0&pp=18&offerid={waremd5}&disabled-promo-thresholds=something_else,{blockpromokey}'.format(
            waremd5=blue_offer_disabled_threashold.waremd5, blockpromokey=blockpromokey
        )
        response = self.report.request_json(request)
        self.__check_absent_promo_fragment(response, blue_offer_disabled_threashold.waremd5)


if __name__ == '__main__':
    main()
