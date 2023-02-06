#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.matcher import Absent, NotEmpty
from core.report import REQUEST_TIMESTAMP
from core.testcase import TestCase, main
from core.types import DynamicBlueGenericBundlesPromos
from core.types.offer_promo import (
    Promo,
    PromoType,
    PromoRestrictions,
    OffersMatchingRules,
)
from core.types.sku import MarketSku, BlueOffer
from core.types.autogen import b64url_md5

from market.pylibrary.const.offer_promo import MechanicsPaymentType
from market.proto.common.promo_pb2 import ESourceType

from datetime import datetime, timedelta
from itertools import count


now = datetime.fromtimestamp(REQUEST_TIMESTAMP)
delta_big = timedelta(days=1)
delta_small = timedelta(hours=5)


FEED_ID = 777
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


# Действующая акция (корзинный промокод, так как поле order_min_price непусто). Скидка по промокоду в абсолютной величинe (рублях). Не от TRADE_MARKETING, скидка в процентах
promo1_trade_marketing = Promo(
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
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(1)],
            ]
        )
    ],
)


blue_offer_ff_virtual = __blue_offer(
    offer_id=1, price=890, price_old=1000, promo=promo1_trade_marketing, is_fulfillment=True
)
msku_1 = __msku(
    [
        blue_offer_ff_virtual,
    ],
    DEFAULT_HID,
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=1']

        cls.index.mskus += [
            msku_1,
        ]

        cls.index.promos += [
            promo1_trade_marketing,
        ]

        cls.settings.loyalty_enabled = True
        cls.dynamic.loyalty += [DynamicBlueGenericBundlesPromos(whitelist=[promo.key for promo in cls.index.promos])]

    def __calc_discount_price(self, offer, promo, is_absolute):
        if is_absolute:
            return offer.price - promo.discount_value
        else:
            # round discount price according to TFixedPointNumber::roundValue function
            if (promo.restrictions is not None) and (promo.restrictions.order_max_price is not None):
                val = min(offer.price, promo.restrictions.order_max_price['value'])
                return offer.price - int(round(val * promo.discount_value / 100))
            else:
                return int(round(offer.price * (100.0 - promo.discount_value) / 100))

    def __check_present_promo_fragment(self, response, promo, offer, waremd5, is_absolute):

        _ = self.__calc_discount_price(offer, promo, is_absolute)
        _ = offer.price_old if offer.price_old else offer.price

        order_min_price = Absent()
        if (promo.restrictions is not None) and (promo.restrictions.order_min_price is not None):
            order_min_price = {
                'value': str(promo.restrictions.order_min_price['value']),
                'currency': promo.restrictions.order_min_price['currency'],
            }

        order_max_price = Absent()
        if (promo.restrictions is not None) and (promo.restrictions.order_max_price is not None):
            order_max_price = {
                'value': str(promo.restrictions.order_max_price['value']),
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
                            'discount': {
                                'value': promo.discount_value,
                                'currency': 'RUR' if promo.discount_currency is not None else Absent(),
                            },
                            'itemsInfo': {
                                'promoCode': promo.promo_code,
                                'discountType': 'absolute' if is_absolute else 'percent',
                                'conditions': Absent() if promo.conditions is None else promo.conditions,
                                'orderMinPrice': order_min_price,
                                'orderMaxPrice': order_max_price,
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
    def check_promo(self, promo, msku, offer, is_absolute, check_present, rearr_flags=None, add_url=""):
        for rgb in ('blue', 'green', 'green_with_blue'):
            for place in ('sku_offers', 'prime'):
                for rearr_flag in rearr_flags or (None,):
                    request = 'place={place}&rids=0&regset=1&pp=18&market-sku={msku}&rgb={rgb}'
                    request = request.format(place=place, msku=msku, rgb=rgb)
                    request += add_url
                    response = self.report.request_json(request)

                    if check_present:
                        self.__check_present_promo_fragment(response, promo, offer, offer.waremd5, is_absolute)
                    else:
                        self.__check_absent_promo_fragment(response, offer.waremd5)

    def test_disable_promo_in_expirement(self):
        """
        Пропускаю пока не добавим MarketDivision в лайты
        Проверяем, что при включении промо в эксперименте он исчезает из выдачи
        """
        self.check_promo(
            promo1_trade_marketing, msku_1.sku, blue_offer_ff_virtual, True, True, [None, 1]
        )  # no expiriment -> has promo
        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            True,
            True,
            [None, 1],
            '&rearr-factors=market_promo_disabling_experiment=1;market_promo_disabling_discount_limit_if_no_puid=0',
        )  # in expiriment -> no promo
        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            True,
            True,
            [None, 1],
            '&rearr-factors=market_promo_disabling_experiment=1;market_promo_disabling_discount_limit_if_no_puid=0;market_promo_disabling_discount_from=50',
        )  # in expiriment out of interval -> has promo
        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            True,
            True,
            [None, 1],
            '&rearr-factors=market_promo_disabling_experiment=1;market_promo_disabling_discount_limit_if_no_puid=0;market_promo_disabling_discount_from=10;market_promo_disabling_discount_to=50',
        )  # in expiriment in interval -> no promo

        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            True,
            True,
            [None, 1],
            '&rearr-factors=market_promo_disabling_experiment=1&off-promo-disabling-experiment=1',
        )  # in expiriment with disabling param -> has promo
        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            True,
            True,
            [None, 1],
            '&rearr-factors=market_promo_disabling_experiment=1;market_promo_disabling_always_if_no_puid=1&off-promo-disabling-experiment=1',
        )  # in expiriment with disabling param -> has promo

        # puid tests
        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            True,
            True,
            [None, 1],
            '&rearr-factors=market_promo_disabling_experiment=1&puid=456',
        )  # default: with puid -> no promo
        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            True,
            True,
            [None, 1],
            '&rearr-factors=market_promo_disabling_experiment=1',
        )  # default: without puid -> no promo
        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            True,
            True,
            [None, 1],
            '&rearr-factors=market_promo_disabling_always_if_no_puid=1',
        )  # default: without puid not in split with flag -> no promo
        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            True,
            True,
            [None, 1],
            '&rearr-factors=market_promo_disabling_always_if_no_puid=1&puid=456',
        )  # default: with puid not in split with flag -> has promo
        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            True,
            True,
            [None, 1],
            '&rearr-factors=market_promo_disabling_experiment=1;market_promo_disabling_only_if_no_puid=1&puid=456',
        )  # with puid after disabling -> has promo

        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            True,
            True,
            [None, 1],
            '&rearr-factors=market_promo_disabling_experiment=1;market_promo_disabling_discount_from=50&puid=456',
        )  # with puid in expiriment out of interval -> has promo
        self.check_promo(
            promo1_trade_marketing,
            msku_1.sku,
            blue_offer_ff_virtual,
            True,
            True,
            [None, 1],
            '&rearr-factors=market_promo_disabling_experiment=1;market_promo_disabling_discount_from=50',
        )  # without puid in expiriment out of interval -> no promo


if __name__ == '__main__':
    main()
