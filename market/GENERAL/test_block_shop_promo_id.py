#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent, NotEmpty
from core.testcase import (
    TestCase,
    main,
)
from core.types import DynamicBlueGenericBundlesPromos, Promo, PromoType
from core.types.autogen import b64url_md5
from core.types.offer_promo import (
    PromoBlueFlash,
    PromoDirectDiscount,
    PromoBlueCashback,
    OffersMatchingRules,
)
from core.types.sku import (
    MarketSku,
    BlueOffer,
)
from market.pylibrary.const.offer_promo import MechanicsPaymentType
from itertools import count


HID_1 = 123000
DEFAULT_FEED_ID = 777

nummer = count()


def get_offer_id(x):
    return 'offer_id: {}'.format(x)


def __blue_offer(offer_id, price=1000, promo=None):
    num = next(nummer)
    return BlueOffer(
        waremd5=b64url_md5(num),
        price=price,
        fesh=DEFAULT_FEED_ID,
        feedid=DEFAULT_FEED_ID,
        offerid=get_offer_id(offer_id),
        promo=promo,
    )


def __msku(offers):
    num = next(nummer)
    return MarketSku(sku=num, hyperid=num, blue_offers=offers if isinstance(offers, list) else [offers])


# Акция blue_cashback
blue_cashback = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key='Promo_blue_cashback',
    url='http://blue_cashback.com/',
    shop_promo_id='blue_cashback',
    blue_cashback=PromoBlueCashback(share=0.2, version=10, priority=1),
    offers_matching_rules=[
        OffersMatchingRules(feed_offer_ids=[[DEFAULT_FEED_ID, get_offer_id(1)]]),
    ],
)

blue_offer_1 = __blue_offer(offer_id=1, price=800, promo=blue_cashback)
msku_1 = __msku(
    [
        blue_offer_1,
    ]
)

# Акция direct_discount
direct_discount = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=DEFAULT_FEED_ID,
    key='Promo_direct_discount',
    url='http://direct_discount.com/',
    shop_promo_id='direct_discount',
    direct_discount=PromoDirectDiscount(
        discounts_by_category=[
            {
                'category_restriction': {
                    'categories': [
                        HID_1,
                    ],
                },
                'discount_percent': 12.3,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    source_reference='http://source_reference.com/direct_discount',
    offers_matching_rules=[
        OffersMatchingRules(feed_offer_ids=[[DEFAULT_FEED_ID, get_offer_id(2)]]),
    ],
)

blue_offer_2 = __blue_offer(offer_id=2, price=810, promo=direct_discount)
msku_2 = __msku(
    [
        blue_offer_2,
    ]
)

# Акция promo code
promo_code = Promo(
    promo_type=PromoType.PROMO_CODE,
    promo_code='promocode_text',
    description='promocode_description',
    discount_value=300,
    discount_currency='RUR',
    feed_id=DEFAULT_FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://promocode.com/',
    landing_url='http://promocode_landing.com/',
    mechanics_payment_type=MechanicsPaymentType.CPA,
    shop_promo_id='promocode',
    conditions='buy at least 300321',
    offers_matching_rules=[
        OffersMatchingRules(feed_offer_ids=[[DEFAULT_FEED_ID, get_offer_id(3)]]),
    ],
)

blue_offer_3 = __blue_offer(offer_id=3, price=830, promo=promo_code)
msku_3 = __msku(
    [
        blue_offer_3,
    ]
)

# Акция blue_flash
blue_flash = Promo(
    promo_type=PromoType.BLUE_FLASH,
    key='BLUE_FLASH_PROMO',
    shop_promo_id='blue_flash',
    url='http://blue_flash.com/',
    blue_flash=PromoBlueFlash(
        items=[
            {'feed_id': 777, 'offer_id': get_offer_id(4), 'price': {'value': 700, 'currency': 'RUR'}},
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    source_reference='http://source_reference.com/blue_flash',
    offers_matching_rules=[
        OffersMatchingRules(feed_offer_ids=[[DEFAULT_FEED_ID, get_offer_id(4)]]),
    ],
)

blue_offer_4 = __blue_offer(offer_id=4, price=840, promo=blue_flash)
msku_4 = __msku(
    [
        blue_offer_4,
    ]
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=1;enable_fast_promo_matcher_test=1']

        cls.index.mskus += [
            msku_1,
            msku_2,
            msku_3,
            msku_4,
        ]

        cls.settings.loyalty_enabled = True
        cls.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(whitelist=[promo.key for promo in [blue_flash]]),
        ]

    def __make_promo_fragment(self, promo, promo_state):
        return {
            'promoKey': promo.key,
            'promoType': promo.type_name,
            'promoState': promo_state,
        }

    def __check_promo_active(self, response, waremd5, additional_query_params, promo):
        # Проверяем что в выдаче есть оффер с корректным блоком 'promos'
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': waremd5,
                    'promos': [
                        {
                            'type': promo.type_name,
                            'key': promo.key,
                            'startDate': NotEmpty() if promo.start_date else Absent(),
                            'endDate': NotEmpty() if promo.start_date else Absent(),
                            'url': promo.url,
                        }
                    ],
                }
            ],
            allow_different_len=False,
        )

        # Проверяем что в oтладочной выдаче есть статус активности акции
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'docs_search_trace': {
                        'traces': [
                            {
                                'promos': [
                                    self.__make_promo_fragment(promo, 'Active'),
                                ],
                            }
                        ],
                    },
                },
            },
        )

    def __check_promo_absent(self, response, waremd5, promo):
        # Проверяем, что блок promos отсутствует
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

        # Проверяем что в oтладочной выдаче есть причина блокировки акции
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'docs_search_trace': {
                        'traces': [
                            {
                                'promos': [
                                    self.__make_promo_fragment(promo, 'DeclinedByBlockShopPromoId'),
                                ],
                            }
                        ],
                    },
                },
            },
        )

    def test_promo_enabled(self):
        '''
        Проверяем что в ответе репорта есть информация об активных промоакциях, в которых участвует оффер
        '''
        offers = [
            (blue_offer_1, '&perks=yandex_cashback'),
            (blue_offer_2, ''),
            (blue_offer_3, ''),
            (blue_offer_4, ''),
        ]
        for offer, query_params in offers:
            waremd5 = offer.waremd5
            additional_query_params = '&debug=1'
            additional_query_params += '&rearr-factors=market_documents_search_trace={}'.format(waremd5)
            additional_query_params += query_params

            request = (
                'place=prime'
                '&rgb=blue'
                '&offerid={waremd5}'
                '&rids=0'
                '&regset=1'
                '&pp=18'
                '{additional_query_params}'
            )

            response = self.report.request_json(
                request.format(waremd5=waremd5, additional_query_params=additional_query_params)
            )

            self.__check_promo_active(
                response, waremd5=waremd5, additional_query_params=additional_query_params, promo=offer.promo
            )

    def test_promo_disabled(self):
        '''
        Проверяем что акции блокируются, если передать в запрос флаг block_shop_promo_id
        '''
        offers = [
            (blue_offer_1, '&perks=yandex_cashback'),
            (blue_offer_2, ''),
            (blue_offer_3, ''),
            (blue_offer_4, ''),
        ]
        for offer, query_params in offers:
            waremd5 = offer.waremd5
            additional_query_params = '&debug=1'
            additional_query_params += '&rearr-factors=market_documents_search_trace={}'.format(waremd5)
            additional_query_params += '&rearr-factors=block_shop_promo_id={}'.format(offer.promo.shop_promo_id)
            additional_query_params += query_params

            request = (
                'place=prime'
                '&rgb=blue'
                '&offerid={waremd5}'
                '&rids=0'
                '&regset=1'
                '&pp=18'
                '{additional_query_params}'
            )

            response = self.report.request_json(
                request.format(waremd5=waremd5, additional_query_params=additional_query_params)
            )

            self.__check_promo_absent(response, waremd5=waremd5, promo=offer.promo)


if __name__ == '__main__':
    main()
