#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

import runner  # noqa

from core.testcase import TestCase, main
from core.types import PromoCheapestAsGift
from core.types.dynamic_filters import DynamicBlueGenericBundlesPromos
from core.types.offer_promo import (
    Promo,
    PromoType,
    make_generic_bundle_content,
    PromoDirectDiscount,
    PromoRestrictions,
)
from core.types.shop import Shop
from core.types.sku import MarketSku, BlueOffer
from core.matcher import Absent, ElementCount


class _Shops(object):
    third_party = Shop(
        fesh=777, datafeed_id=777, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL
    )


def make_offer_for_promo(id, promo=None, blue_promo_key=None):
    offerid = 'blue.offer.{}'.format(id)
    return BlueOffer(
        price=500,
        offerid=offerid,
        feedid=_Shops.third_party.datafeed_id,
        fesh=_Shops.third_party.fesh,
        waremd5='Sku{}____-vm1Goleg'.format(id),
        promo=promo,
        blue_promo_key=blue_promo_key,
    )


class _Offers(object):
    offer_for_cheapest_as_gift = make_offer_for_promo('222222')
    offer_for_generic_bundle = make_offer_for_promo('333333')
    offer_secondary = make_offer_for_promo('444444')
    offer_for_discount_perks = make_offer_for_promo('555555')
    offer_for_direct_discount = make_offer_for_promo("888888")


def make_msku(id, offers):
    return MarketSku(sku=id, hyperid=2, blue_offers=offers)


class _Mskus(object):
    msku_for_cheapest_as_gift = make_msku(222222, [_Offers.offer_for_cheapest_as_gift])
    msku_for_generic_bundle = make_msku(333333, [_Offers.offer_for_generic_bundle])
    msku_for_secondary = make_msku(444444, [_Offers.offer_secondary])
    msku_for_discount_perks = make_msku(555555, [_Offers.offer_for_discount_perks])
    msku_for_direct_discount = make_msku(888888, [_Offers.offer_for_direct_discount])


class _Promos(object):
    promo_cheapest_as_gift = Promo(
        promo_type=PromoType.CHEAPEST_AS_GIFT,
        feed_id=_Shops.third_party.datafeed_id,
        key='Promo_cheapest_as_gift',
        url='http://localhost.ru/',
        cheapest_as_gift=PromoCheapestAsGift(
            offer_ids=[
                (_Shops.third_party.datafeed_id, _Offers.offer_for_cheapest_as_gift.offerid),
            ],
            count=3,
            promo_url='url',
            link_text='text',
            allow_berubonus=False,
            allow_promocode=False,
        ),
    )

    promo_generic_bundle = Promo(
        promo_type=PromoType.GENERIC_BUNDLE,
        feed_id=_Shops.third_party.datafeed_id,
        key="GENERIC_BUNDLE_ID",
        url='http://beru.ru/generic_bundle',
        generic_bundles_content=[
            make_generic_bundle_content(_Offers.offer_for_generic_bundle.offerid, _Offers.offer_secondary.offerid, 100),
        ],
        restrictions=PromoRestrictions(restricted_promo_types=[PromoType.PROMO_CODE]),
    )

    promo_direct_discount = Promo(
        promo_type=PromoType.DIRECT_DISCOUNT,
        feed_id=_Shops.third_party.datafeed_id,
        key='DIRECT_DISCOUNT_PROMO',
        url='http://direct_discount.com/',
        direct_discount=PromoDirectDiscount(
            items=[
                {
                    'feed_id': _Shops.third_party.datafeed_id,
                    'offer_id': _Offers.offer_for_direct_discount.offerid,
                    'discount_price': {
                        'value': _Offers.offer_for_direct_discount.price - 10,
                        'currency': 'RUR',
                    },
                    'old_price': {
                        'value': 12345,
                        'currency': 'RUR',
                    },
                },
            ],
            allow_berubonus=True,
            allow_promocode=True,
        ),
    )

    promo_code = Promo(
        promo_type=PromoType.PROMO_CODE,
        promo_code='promocode_1_text',
        description='promocode_1_description',
        discount_value=30,
        discount_currency='RUR',
        feed_id=_Shops.third_party.datafeed_id,
        key='PROMO_CODE',
        url='http://promocode_1.com/',
        shop_promo_id='promo_code',
        conditions='buy at least 300321',
        restrictions=PromoRestrictions(restricted_promo_types=[PromoType.DIRECT_DISCOUNT, PromoType.BLUE_SET]),
    )

    promo_discount_perks = Promo(
        promo_type=PromoType.DIRECT_DISCOUNT,
        feed_id=_Shops.third_party.datafeed_id,
        key='DIRECT_DISCOUNT_PERKS',
        url='http://direct_discount.com/perks',
        direct_discount=PromoDirectDiscount(
            items=[
                {
                    'feed_id': _Shops.third_party.datafeed_id,
                    'offer_id': _Offers.offer_for_discount_perks.offerid,
                    'discount_price': {
                        'value': 400,
                        'currency': 'RUR',
                    },
                    'old_price': {
                        'value': 600,
                        'currency': 'RUR',
                    },
                },
            ],
        ),
        restrictions=PromoRestrictions(predicates=[{'perks': ['yandex_plus']}]),
    )


class T(TestCase):
    """
    Проверяем что синие акций нормально применяются на оффер вместе с blue-bashback.
    А именно на оферах с blue-bashback + oneof(generic-bundle, blue-flash, direct-discount, blue-set, cheapest-as-gift, secret-sale)
    И blue-bashback И другая акция показывается на выдаче в блоке promos, а в блоке promo только НЕ blue-bashback
    """

    @classmethod
    def prepare(self):
        self.settings.default_search_experiment_flags += [
            'enable_fast_promo_matcher=0;enable_fast_promo_matcher_test=1'
        ]

        _Offers.offer_for_cheapest_as_gift.promo = [
            _Promos.promo_cheapest_as_gift,
            _Promos.promo_code,
        ]
        _Offers.offer_for_direct_discount.promo = [
            _Promos.promo_direct_discount,
            _Promos.promo_code,
        ]
        _Offers.offer_for_generic_bundle.promo = [
            _Promos.promo_generic_bundle,
            _Promos.promo_code,
        ]
        _Offers.offer_for_discount_perks.promo = [
            _Promos.promo_discount_perks,
        ]

        self.index.shops += [_Shops.third_party]

        self.index.mskus += [
            _Mskus.msku_for_cheapest_as_gift,
            _Mskus.msku_for_generic_bundle,
            _Mskus.msku_for_secondary,
            _Mskus.msku_for_direct_discount,
            _Mskus.msku_for_discount_perks,
        ]

        self.index.promos += [
            _Promos.promo_generic_bundle,
            _Promos.promo_cheapest_as_gift,
            _Promos.promo_direct_discount,
            _Promos.promo_discount_perks,
        ]
        self.settings.loyalty_enabled = True
        self.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(
                whitelist=[
                    _Promos.promo_cheapest_as_gift.key,
                    _Promos.promo_generic_bundle.key,
                    _Promos.promo_direct_discount.key,
                    _Promos.promo_discount_perks.key,
                ]
            )
        ]

    def test_allow_promo_code(self):
        '''
        У оффера _Offers.offer_cheapest_as_gift запрещено пересечение с промокодом через флаг allow_promocode. Из ответа пропадает менее приоритетное промо
        '''

        request_tmpl = 'place=prime&rgb=blue&market-sku={msku}&numdoc=100&rids=0&regset=1&pp=18&yandexuid=1'

        response = self.report.request_json(request_tmpl.format(msku=_Mskus.msku_for_cheapest_as_gift.sku))
        self.assertFragmentIn(
            response,
            [{'entity': 'offer', 'wareId': _Offers.offer_for_cheapest_as_gift.waremd5, 'promos': ElementCount(1)}],
        )

    def test_restriction_in_generic_bundle(self):
        '''
        У оффера _Offers.offer_generic_bundle запрещено пересечение с промокодом через restricted_promo_types. Из ответа пропадает менее приоритетное промо
        проверяем сначала старую логику, потом новую с market_promo_mutually_restrictions_enable=1
        '''

        request_tmpl = 'place=prime&rgb=blue&market-sku={msku}&numdoc=100&rids=0&regset=1&pp=18&yandexuid=1&rearr-factors=market_promo_mutually_restrictions_enable=0;'

        response = self.report.request_json(request_tmpl.format(msku=_Mskus.msku_for_generic_bundle.sku))
        self.assertFragmentIn(
            response,
            [{'entity': 'offer', 'wareId': _Offers.offer_for_generic_bundle.waremd5, 'promos': ElementCount(1)}],
        )

        request_tmpl = 'place=prime&rgb=blue&market-sku={msku}&numdoc=100&rids=0&regset=1&pp=18&yandexuid=1&rearr-factors=market_promo_mutually_restrictions_enable=1;'

        response = self.report.request_json(request_tmpl.format(msku=_Mskus.msku_for_generic_bundle.sku))
        self.assertFragmentIn(
            response,
            [{'entity': 'offer', 'wareId': _Offers.offer_for_generic_bundle.waremd5, 'promos': ElementCount(2)}],
        )

    def test_restriction_in_generic_promocode(self):
        '''
        У оффера _Offers.offer_direct_discount запрещено пересечение с прямой скидкой через restricted_promo_types у пормокода. Из ответа пропадает менее приоритетное промо
        проверяем сначала старую логику, потом новую с market_promo_mutually_restrictions_enable=1
        '''

        request_tmpl = 'place=prime&rgb=blue&market-sku={msku}&numdoc=100&rids=0&regset=1&pp=18&yandexuid=1&rearr-factors=market_promo_mutually_restrictions_enable=0;'

        response = self.report.request_json(request_tmpl.format(msku=_Mskus.msku_for_direct_discount.sku))
        self.assertFragmentIn(
            response,
            [{'entity': 'offer', 'wareId': _Offers.offer_for_direct_discount.waremd5, 'promos': ElementCount(1)}],
        )

        request_tmpl = 'place=prime&rgb=blue&market-sku={msku}&numdoc=100&rids=0&regset=1&pp=18&yandexuid=1&rearr-factors=market_promo_mutually_restrictions_enable=1;'

        response = self.report.request_json(request_tmpl.format(msku=_Mskus.msku_for_direct_discount.sku))
        self.assertFragmentIn(
            response,
            [{'entity': 'offer', 'wareId': _Offers.offer_for_direct_discount.waremd5, 'promos': ElementCount(2)}],
        )

    def test_restriction_perks(self):
        '''
        Проверяем, что промо с ограничением по перкам появляется только, если в запросе указан нужный перк
        '''

        request_tmpl = 'place={place}&rgb=blue&market-sku={msku}&numdoc=100&rids=0&regset=1&pp=18&yandexuid=1{perks}'
        for place in ['prime', 'sku_offers']:
            response = self.report.request_json(
                request_tmpl.format(place=place, msku=_Mskus.msku_for_discount_perks.sku, perks='')
            )
            self.assertFragmentIn(
                response,
                [
                    {
                        'entity': 'offer',
                        'wareId': _Offers.offer_for_discount_perks.waremd5,
                        'prices': {
                            'value': str(_Offers.offer_for_discount_perks.price),
                        },
                        'promos': Absent(),
                    }
                ],
                allow_different_len=False,
            )

            response = self.report.request_json(
                request_tmpl.format(place=place, msku=_Mskus.msku_for_discount_perks.sku, perks='&perks=yandex_plus')
            )
            self.assertFragmentIn(
                response,
                [
                    {
                        'entity': 'offer',
                        'wareId': _Offers.offer_for_discount_perks.waremd5,
                        'prices': {
                            'value': '400',
                        },
                        'promos': [
                            {
                                'key': _Promos.promo_discount_perks.key,
                                'type': PromoType.DIRECT_DISCOUNT,
                            },
                        ],
                    }
                ],
                allow_different_len=False,
            )


if __name__ == '__main__':
    main()
