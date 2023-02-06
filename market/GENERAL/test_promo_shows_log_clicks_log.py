#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import ClickType, DynamicBlueGenericBundlesPromos, Promo, PromoType
from core.types.offer_promo import (
    PromoCheapestAsGift,
    PromoBlueCashback,
)
from core.types.sku import MarketSku, BlueOffer
from core.types.autogen import b64url_md5
from core.types.dynamic_filters import DynamicPromoKeysBlacklist

from market.pylibrary.const.offer_promo import MechanicsPaymentType

from itertools import count


FEED_ID = 777

nummer = count()


def get_offer_id(x):
    return 'offer_id: {}'.format(x)


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


def __msku(offers):
    num = next(nummer)
    return MarketSku(sku=num, hyperid=num, blue_offers=offers if isinstance(offers, list) else [offers])


# Действующая акция promo code. Скидка по промокоду в абсолютной величинe (рублях).
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
)

blue_offer_ff_virtual = __blue_offer(offer_id=1, price=1000, price_old=1000, promo=promo1, is_fulfillment=True)
msku_1 = __msku(
    [
        blue_offer_ff_virtual,
    ]
)


# Действующая акция cheapest_as_gift
promo2 = Promo(
    promo_type=PromoType.CHEAPEST_AS_GIFT,
    description='cag_description',
    key=b64url_md5(next(nummer)),
    url='http://cag.com/',
    cheapest_as_gift=PromoCheapestAsGift(
        offer_ids=[
            (FEED_ID, get_offer_id(2)),
        ],
        count=3,
        promo_url='http://cag_promo_url.com',
        link_text='cag_link_text',
        allow_berubonus=False,
        allow_promocode=False,
    ),
)


# Действующая акция blue_cashback
promo3 = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    description='cashback_description',
    key=b64url_md5(next(nummer)),
    url='http://cashback.com/',
    landing_url='http://cashback_landing.com/',
    shop_promo_id='cashback_promo_id',
    blue_cashback=PromoBlueCashback(share=0.2, version=3, priority=1),
)

blue_offer_ff_virtual_2 = __blue_offer(
    offer_id=2, price=1000, price_old=1000, promo=[promo2, promo3], is_fulfillment=True
)
msku_2 = __msku(
    [
        blue_offer_ff_virtual_2,
    ]
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=0;enable_fast_promo_matcher_test=0']
        cls.index.mskus += [
            msku_1,
            msku_2,
        ]

        cls.index.promos += [
            promo1,
            promo2,
            promo3,
        ]

        cls.settings.loyalty_enabled = True
        cls.dynamic.loyalty += [DynamicBlueGenericBundlesPromos(whitelist=[promo.key for promo in cls.index.promos])]

    def test_single_promo_in_shows_log_and_clicks_log(self):
        '''
        Проверяем, что поля promo_key, promo_type корректно записываются в shows-log и clicks-log репорта для
        случая одного промо. Делаем два запроса в репорт - один, когда промо активно, другой - когда промо
        заблокировано по чёрному списку лоялти. В shows-log и clicks-log попадает только одна запись для случая
        активного промо.
        '''

        request = 'place=prime&rids=0&regset=1&pp=18&offerid={}&rgb=blue'.format(blue_offer_ff_virtual.waremd5)

        _ = self.report.request_json(request)
        self.dynamic.loyalty += [
            DynamicPromoKeysBlacklist(
                blacklist=[
                    promo1.key,
                ]
            ),
        ]
        _ = self.report.request_json(request)

        expected_promo_type = PromoType.MASK_BY_NAME[promo1.type_name]
        self.show_log_tskv.expect(promo_key=promo1.key, promo_type=expected_promo_type).times(1)
        self.click_log.expect(promo_key=promo1.key, promo_type=expected_promo_type, clicktype=ClickType.CPA).times(1)

    def test_multi_promo_in_shows_log_and_clicks_log(self):
        '''
        Проверяем, что поля promo_key, promo_type корректно записываются в shows-log и clicks-log репорта для
        случая нескольких промо.
        '''

        request = 'place=prime&rids=0&regset=1&pp=18&offerid={}&rgb=blue'.format(blue_offer_ff_virtual_2.waremd5)
        request += '&perks=yandex_cashback'

        response = self.report.request_json(request)

        expected_promo_type = PromoType.MASK_BY_NAME[promo2.type_name] | PromoType.MASK_BY_NAME[promo3.type_name]
        expected_promo_key = ','.join(promo.key for promo in [promo3, promo2])

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': blue_offer_ff_virtual_2.waremd5,
                'promos': [
                    {
                        'type': promo2.type_name,
                        'key': promo2.key,
                    },
                    {
                        'type': promo3.type_name,
                        'key': promo3.key,
                    },
                ],
            },
        )

        self.show_log_tskv.expect(promo_key=expected_promo_key, promo_type=expected_promo_type).times(1)
        self.click_log.expect(
            promo_key=expected_promo_key, promo_type=expected_promo_type, clicktype=ClickType.CPA
        ).times(1)


if __name__ == '__main__':
    main()
