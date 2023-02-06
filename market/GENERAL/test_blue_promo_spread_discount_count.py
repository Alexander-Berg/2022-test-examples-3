#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent, NotEmpty
from core.report import REQUEST_TIMESTAMP
from core.testcase import TestCase, main
from core.types import DynamicBlueGenericBundlesPromos, DynamicPromo, Promo, PromoType
from core.types.offer_promo import (
    PromoDirectDiscount,
    PromoSpreadDiscountCount,
    PromoCheapestAsGift,
)
from core.types.sku import MarketSku, BlueOffer
from core.types.autogen import b64url_md5
from core.types.dynamic_filters import DynamicPromoKeysBlacklist

from datetime import datetime, timedelta
from itertools import count
from math import floor


now = datetime.fromtimestamp(REQUEST_TIMESTAMP)
delta_big = timedelta(days=1)
delta_small = timedelta(hours=5)


FEED_ID = 777
DEFAULT_HID = 8001
HID_1 = 8002

nummer = count()


def get_offer_id(x):
    return 'offer_id_{}'.format(x)


def __blue_offer(offer_id, price=1000, price_old=1000, is_fulfillment=True):
    num = next(nummer)
    return BlueOffer(
        waremd5=b64url_md5(num),
        price=price,
        price_old=price_old,
        fesh=FEED_ID,
        feedid=FEED_ID,
        offerid=get_offer_id(offer_id),
        is_fulfillment=is_fulfillment,
    )


def __msku(offers, hid):
    num = next(nummer)
    return MarketSku(sku=num, hyperid=num, hid=hid, blue_offers=offers if isinstance(offers, list) else [offers])


###############################################################################################################
blue_offer_for_one_promo = __blue_offer(offer_id=next(nummer), price=890, price_old=1000, is_fulfillment=True)
msku_for_single_promo = __msku(
    [
        blue_offer_for_one_promo,
    ],
    DEFAULT_HID,
)

# Действующая акция с двумя порогами скидки.
promo_sdc_single = Promo(
    promo_type=PromoType.SPREAD_DISCOUNT_COUNT,
    description='spread discount normal',
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://spdc_1.com/',
    landing_url='http://spdc_1_landing.com/',
    shop_promo_id='spread_discount_count_single',
    spread_discount_count=PromoSpreadDiscountCount(
        items={
            msku_for_single_promo.sku: [{'count': 3, 'percent_discount': 7}, {'count': 5, 'percent_discount': 13}],
        }
    ),
)
blue_offer_for_one_promo.promo = promo_sdc_single


###############################################################################################################
blue_offer_for_two_promos = __blue_offer(offer_id=next(nummer), price=890, price_old=1000, is_fulfillment=False)
msku_for_two_promos = __msku(
    [
        blue_offer_for_two_promos,
    ],
    DEFAULT_HID,
)

# Действующая акция одним порого скидки. Скидка должна складываться с "прямой скидкой".
promo_sdc_two_promos = Promo(
    promo_type=PromoType.SPREAD_DISCOUNT_COUNT,
    description='spread discount with direct discount',
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://spdc_2.com/',
    landing_url='http://spdc_2_landing.com/',
    shop_promo_id='spread_discount_count_two_promos',
    spread_discount_count=PromoSpreadDiscountCount(
        items={
            msku_for_two_promos.sku: [{'count': 3, 'percent_discount': 7}, {'count': 5, 'percent_discount': 13}],
        }
    ),
)

promo_dd_two_promos = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    description='direct discount with spread discount',
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_1.com/',
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': blue_offer_for_two_promos.offerid,
                'discount_price': {'value': 800, 'currency': 'RUR'},
            }
        ],
    ),
)
blue_offer_for_two_promos.promo = [promo_sdc_two_promos, promo_dd_two_promos]


###############################################################################################################
blue_offer_for_too_low_discount = __blue_offer(offer_id=next(nummer), price=90, is_fulfillment=True)
msku_for_too_low_discount = __msku(
    [
        blue_offer_for_too_low_discount,
    ],
    DEFAULT_HID,
)

# Действующая акция маленькой скидкой
promo_sdc_too_low_discount = Promo(
    promo_type=PromoType.SPREAD_DISCOUNT_COUNT,
    description='spread discount with low discount',
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://spdc_3.com/',
    landing_url='http://spdc_3_landing.com/',
    shop_promo_id='spread_discount_count_too_low_discount',
    spread_discount_count=PromoSpreadDiscountCount(
        items={
            msku_for_too_low_discount.sku: [{'count': 11, 'percent_discount': 1}],
        }
    ),
)
blue_offer_for_too_low_discount.promo = promo_sdc_too_low_discount


###############################################################################################################
blue_offer_loose_for_cg = __blue_offer(offer_id=next(nummer), price=900, is_fulfillment=True)
msku_loose_for_cg = __msku(
    [
        blue_offer_loose_for_cg,
    ],
    DEFAULT_HID,
)

# Действующая акция spread discount count
promo_sdc_loose_for_cg = Promo(
    promo_type=PromoType.SPREAD_DISCOUNT_COUNT,
    description='spread discount loosing cheapest as gift',
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://spdc_4.com/',
    landing_url='http://spdc_4_landing.com/',
    shop_promo_id='spread_discount_count_loose_for_cg',
    spread_discount_count=PromoSpreadDiscountCount(
        items={
            msku_loose_for_cg.sku: [{'count': 11, 'percent_discount': 6}],
        }
    ),
)

# Действующая акция cheapest as gift
promo_cg_win = Promo(
    promo_type=PromoType.CHEAPEST_AS_GIFT,
    key=b64url_md5(next(nummer)),
    url='http://localhost.ru/',
    cheapest_as_gift=PromoCheapestAsGift(
        offer_ids=[
            (FEED_ID, blue_offer_loose_for_cg.offerid),
        ],
        count=3,
        promo_url='',
        link_text='text',
    ),
)
blue_offer_loose_for_cg.promo = [promo_sdc_loose_for_cg, promo_cg_win]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=0;enable_fast_promo_matcher_test=1']

        cls.index.mskus += [msku_for_single_promo, msku_for_two_promos, msku_for_too_low_discount, msku_loose_for_cg]

        cls.index.promos += [
            promo_sdc_single,
            promo_sdc_two_promos,
            promo_dd_two_promos,
            promo_sdc_too_low_discount,
            promo_sdc_loose_for_cg,
            promo_cg_win,
        ]

        cls.settings.loyalty_enabled = True
        cls.dynamic.loyalty += [DynamicBlueGenericBundlesPromos(whitelist=[promo.key for promo in cls.index.promos])]

    def __calc_discount_percent(self, old_price, new_price):
        return int(floor((1 - 1.0 * new_price / old_price) * 100.0 + 0.5))

    def __check_present_promo_fragment(self, response, promo, msku, offer):

        # Проверяем что в выдаче есть оффер с корректным блоком 'promos'
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': offer.waremd5,
                    'prices': {
                        'value': str(offer.price),
                        'currency': 'RUR',
                    },
                    'promos': [promo.spread_discount_count.promo_fragment(offer, msku)],
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
    def check_promo(self, check_present, promo, msku, offer, rearr_flags=None, add_url=""):
        for rgb in ('blue', 'green', 'green_with_blue'):
            for place in ('sku_offers', 'prime'):
                for rearr_flag in rearr_flags or (None,):
                    request = 'place={place}&rids=0&regset=1&pp=18&market-sku={msku}&rgb={rgb}'
                    if rearr_flag is not None:
                        request += '&rearr-factors=market_promo_spread_discount_count={}'.format(rearr_flag)
                    request = request.format(place=place, msku=msku, rgb=rgb)
                    request += add_url
                    response = self.report.request_json(request)

                    if check_present:
                        self.__check_present_promo_fragment(response, promo, msku, offer)
                    else:
                        self.__check_absent_promo_fragment(response, offer.waremd5)

    def test_spread_discount_count_active(self):
        """
        Проверяем стандартный сценарий наличия прогрессирующей скидки от количества
        """

        # Без флага и с флагом market_promo_blue_spread_discount_count=1 промо есть на выдаче
        self.check_promo(
            True, promo_sdc_single, msku_for_single_promo.sku, blue_offer_for_one_promo, rearr_flags=[None, 1]
        )
        # C флагом market_promo_spread_discount_count=0 промо нет на выдаче
        self.check_promo(False, promo_sdc_single, msku_for_single_promo.sku, blue_offer_for_one_promo, rearr_flags=[0])

    def test_spread_discount_count_whitelist(self):
        """
        Проверяем, что без включения промо в белый список он не показывается на выдаче
        """
        self.check_promo(
            True, promo_sdc_single, msku_for_single_promo.sku, blue_offer_for_one_promo, rearr_flags=[None, 1]
        )
        self.dynamic.loyalty -= [DynamicBlueGenericBundlesPromos(whitelist=[promo_sdc_single.key])]
        self.check_promo(
            False, promo_sdc_single, msku_for_single_promo.sku, blue_offer_for_one_promo, rearr_flags=[None, 1]
        )
        self.dynamic.loyalty += [DynamicBlueGenericBundlesPromos(whitelist=[promo_sdc_single.key])]

    def test_spread_discount_count_blacklist(self):
        """
        Проверяем, что при включении промо в черный список он исчезает из выдачи
        """
        self.check_promo(
            True, promo_sdc_single, msku_for_single_promo.sku, blue_offer_for_one_promo, rearr_flags=[None, 1]
        )
        self.dynamic.loyalty += [DynamicPromoKeysBlacklist(blacklist=[promo_sdc_single.key])]
        self.check_promo(
            False, promo_sdc_single, msku_for_single_promo.sku, blue_offer_for_one_promo, rearr_flags=[None, 1]
        )
        self.dynamic.loyalty += [DynamicPromoKeysBlacklist(blacklist=[])]

    def test_spread_discount_count_low_discount(self):
        """
        Проверяем, что промо не попадает в выдачу при маленькой скидке
        """

        self.check_promo(
            False,
            promo_sdc_too_low_discount,
            msku_for_too_low_discount.sku,
            blue_offer_for_too_low_discount,
            rearr_flags=[None, 0, 1],
        )

    def test_spread_discount_count_priority_lower(self):
        """
        Проверяем, что если на синем оффере одновременно два промо SpreadDiscountCount и CheapestAsGift, то, согласно приоритетам
        на репорте, CheapestAsGift выигрывает.
        """

        offer = blue_offer_loose_for_cg
        promo = promo_cg_win
        request = 'place=prime&rids=0&regset=1&pp=18&offerid={}&rgb=blue'.format(offer.waremd5)
        request += '&rearr-factors=market_promo_spread_discount_count=1'

        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': offer.waremd5,
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
                            'itemsInfo': {
                                'count': promo.cheapest_as_gift.count,
                                'promo_url': promo.cheapest_as_gift.promo_url,
                                'link_text': promo.cheapest_as_gift.link_text,
                                'constraints': {
                                    'allow_berubonus': promo.cheapest_as_gift.allow_berubonus,
                                    'allow_promocode': promo.cheapest_as_gift.allow_promocode,
                                },
                            },
                        }
                    ],
                }
            ],
            allow_different_len=False,
        )
        self.dynamic.market_dynamic.disabled_promos = [DynamicPromo(promo_key=promo.key)]
        self.check_promo(
            True, promo_sdc_loose_for_cg, msku_loose_for_cg.sku, blue_offer_loose_for_cg, rearr_flags=[None, 1]
        )
        self.dynamic.market_dynamic.disabled_promos = []

    def test_spread_discount_count_and_direct_discount(self):
        """
        Проверяем, что если на синем оффере одновременно два промо SpreadDiscountCount и DirectDiscount, то они оба возвращаются в выдаче.
        При этом promoPriceWithTotalDiscount в SpreadDiscountCount должен учитывать скидку DirectDiscount
        """

        offer = blue_offer_for_two_promos
        msku = msku_for_two_promos.sku
        dd_item = promo_dd_two_promos.direct_discount.items[0]
        request = 'place=prime&rids=0&regset=1&pp=18&market-sku={}&rgb=blue'.format(msku)
        request += '&rearr-factors=market_promo_spread_discount_count=1'

        discount_price = dd_item['discount_price']['value']
        offer_old_price = offer.price_old if offer.price_old else offer.price
        old_price = dd_item['old_price']['value'] if 'old_price' in dd_item else offer_old_price
        discount_percent = self.__calc_discount_percent(old_price, discount_price)

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
                            'type': promo_dd_two_promos.type_name,
                            'key': promo_dd_two_promos.key,
                            'startDate': NotEmpty() if promo_dd_two_promos.start_date else Absent(),
                            'endDate': NotEmpty() if promo_dd_two_promos.end_date else Absent(),
                            'url': promo_dd_two_promos.url,
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
                                    'allow_berubonus': promo_dd_two_promos.direct_discount.allow_berubonus,
                                    'allow_promocode': promo_dd_two_promos.direct_discount.allow_promocode,
                                },
                            },
                        },
                        promo_sdc_two_promos.spread_discount_count.promo_fragment(
                            offer, msku, current_price=discount_price
                        ),
                    ],
                }
            ],
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
