#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import DynamicBlueGenericBundlesPromos, Promo, PromoMSKU, PromoSaleDetails, PromoType, Shop
from core.types import PromoCheapestAsGift
from core.types.offer_promo import (
    PromoBlueFlash,
    PromoBlueSet,
    make_generic_bundle_content,
    PromoDirectDiscount,
    PromoBlueCashback,
    OffersMatchingRules,
)
from core.types.sku import MarketSku, BlueOffer
from core.matcher import Absent, ElementCount


class _Shops(object):
    third_party = Shop(
        fesh=777, datafeed_id=777, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL
    )


def create_offer_id(id):
    return 'blue.offer.{}'.format(id)


promo_blue_cashback = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key='Promo_blue_cashback',
    url='http://blue_cashback.com/',
    blue_cashback=PromoBlueCashback(
        share=0.2,
        version=10,
        priority=1,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [_Shops.third_party.datafeed_id, offerid]
                for offerid in [
                    create_offer_id('111111'),
                    create_offer_id('222222'),
                    create_offer_id('333333'),
                    create_offer_id('555555'),
                    create_offer_id('555556'),
                    create_offer_id('888888'),
                    create_offer_id('888889'),
                    create_offer_id('999999'),
                    create_offer_id('100001'),
                    create_offer_id('100003'),
                    create_offer_id('set111'),
                    create_offer_id('set222'),
                    create_offer_id('set333'),
                ]
            ]
        )
    ],
)


def make_offer_for_promo(id, promo=None, blue_promo_key=None):
    offerid = create_offer_id(id)
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
    offer_for_cheapest_as_gift = make_offer_for_promo('222222', promo_blue_cashback)
    offer_for_generic_bundle = make_offer_for_promo('333333', promo_blue_cashback)
    offer_secondary = make_offer_for_promo('444444')
    offer_blue_flash = make_offer_for_promo('555555', promo_blue_cashback)
    offer_3p_flash_discount = make_offer_for_promo(
        '555556', promo_blue_cashback, blue_promo_key='BLUE_3P_FLASH_DISCOUNT'
    )
    blue_set_1 = make_offer_for_promo('set111', promo_blue_cashback)
    blue_set_2 = make_offer_for_promo('set222', promo_blue_cashback)
    offer_for_direct_discount = make_offer_for_promo("888888", promo_blue_cashback)
    offer_for_secret_sale = make_offer_for_promo("999999", promo_blue_cashback)
    offer_with_two_promos = make_offer_for_promo("999998")
    offer_gb2_primary = make_offer_for_promo("100000")
    offer_gb2_secondary = make_offer_for_promo("100001")
    offer_bs2_primary = make_offer_for_promo("100002")
    offer_bs2_secondary = make_offer_for_promo("100003")


def make_msku(id, offers):
    return MarketSku(sku=id, hyperid=2, blue_offers=offers)


class _Mskus(object):
    msku_for_cheapest_as_gift = make_msku(222222, [_Offers.offer_for_cheapest_as_gift])
    msku_for_generic_bundle = make_msku(333333, [_Offers.offer_for_generic_bundle])
    msku_for_secondary = make_msku(444444, [_Offers.offer_secondary])
    msku_for_blue_flash = make_msku(555555, [_Offers.offer_blue_flash])
    msku_for_3p_flash_discount = make_msku(555556, [_Offers.offer_3p_flash_discount])
    msku_for_blue_set_1 = make_msku(666666, [_Offers.blue_set_1])
    msku_for_blue_set_2 = make_msku(777777, [_Offers.blue_set_2])
    msku_for_direct_discount = make_msku(888888, [_Offers.offer_for_direct_discount])
    msku_for_secret_sale = make_msku(999999, [_Offers.offer_for_secret_sale])
    msku_for_two_promos = make_msku(999998, [_Offers.offer_with_two_promos])
    msku_for_gb2_primary = make_msku(100000, [_Offers.offer_gb2_primary])
    msku_for_gb2_secondary = make_msku(100001, [_Offers.offer_gb2_secondary])
    msku_for_bs2_primary = make_msku(100002, [_Offers.offer_bs2_primary])
    msku_for_bs2_secondary = make_msku(100003, [_Offers.offer_bs2_secondary])


class _Promos(object):
    secret_sale = Promo(
        promo_type=PromoType.SECRET_SALE,
        key='Secret_sale_promo_key1',
        secret_sale_details=PromoSaleDetails(
            msku_list=[
                int(_Mskus.msku_for_secret_sale.sku),
                int(_Mskus.msku_for_gb2_secondary.sku),
                int(_Mskus.msku_for_bs2_secondary.sku),
            ],
            discount_percent_list=[10, 10, 10],
        ),
        source_promo_id='Secret_sale_promo_key1',
    )

    cheapest_as_gift = Promo(
        promo_type=PromoType.CHEAPEST_AS_GIFT,
        feed_id=_Shops.third_party.datafeed_id,
        key='Promo_cheapest_as_gift',
        url='http://localhost.ru/',
        cheapest_as_gift=PromoCheapestAsGift(
            offer_ids=[
                (_Shops.third_party.datafeed_id, _Offers.offer_for_cheapest_as_gift.offerid),
                (_Shops.third_party.datafeed_id, _Offers.offer_with_two_promos.offerid),
                (_Shops.third_party.datafeed_id, _Offers.offer_bs2_secondary.offerid),
            ],
            count=3,
            promo_url='url',
            link_text='text',
            allow_berubonus=False,
            allow_promocode=False,
        ),
        offers_matching_rules=[
            OffersMatchingRules(
                feed_offer_ids=[
                    [_Shops.third_party.datafeed_id, offerid]
                    for offerid in [
                        _Offers.offer_for_cheapest_as_gift.offerid,
                        _Offers.offer_with_two_promos.offerid,
                        _Offers.offer_gb2_secondary.offerid,
                        _Offers.offer_bs2_secondary.offerid,
                    ]
                ]
            )
        ],
    )

    promo_generic_bundle = Promo(
        promo_type=PromoType.GENERIC_BUNDLE,
        feed_id=_Shops.third_party.datafeed_id,
        key="GENERIC_BUNDLE_ID",
        url='http://beru.ru/generic_bundle',
        generic_bundles_content=[
            make_generic_bundle_content(_Offers.offer_for_generic_bundle.offerid, _Offers.offer_secondary.offerid, 100),
            make_generic_bundle_content(_Offers.offer_gb2_primary.offerid, _Offers.offer_gb2_secondary.offerid, 100),
        ],
        offers_matching_rules=[
            OffersMatchingRules(
                feed_offer_ids=[
                    [_Shops.third_party.datafeed_id, offerid]
                    for offerid in [
                        _Offers.offer_for_generic_bundle.offerid,
                        _Offers.offer_gb2_primary.offerid,
                        _Offers.offer_gb2_secondary.offerid,
                        _Offers.offer_bs2_secondary.offerid,
                    ]
                ]
            )
        ],
    )

    promo_blue_3p_flash = Promo(
        promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
        feed_id=777,
        key='BLUE_3P_FLASH_DISCOUNT',
        mskus=[
            PromoMSKU(msku=str(_Mskus.msku_for_3p_flash_discount.sku), market_promo_price=500, market_old_price=700),
            PromoMSKU(msku=str(_Mskus.msku_for_gb2_secondary.sku), market_promo_price=500, market_old_price=700),
            PromoMSKU(msku=str(_Mskus.msku_for_bs2_secondary.sku), market_promo_price=500, market_old_price=700),
        ],
    )

    promo_blue_flash = Promo(
        promo_type=PromoType.BLUE_FLASH,
        key='BLUE_FLASH_PROMO',
        blue_flash=PromoBlueFlash(
            items=[
                {
                    'feed_id': _Shops.third_party.datafeed_id,
                    'offer_id': _Offers.offer_blue_flash.offerid,
                    'price': {'value': _Offers.offer_blue_flash.price - 90, 'currency': 'RUR'},
                },
                {
                    'feed_id': _Shops.third_party.datafeed_id,
                    'offer_id': _Offers.offer_with_two_promos.offerid,
                    'price': {'value': _Offers.offer_with_two_promos.price - 80, 'currency': 'RUR'},
                },
                {
                    'feed_id': _Shops.third_party.datafeed_id,
                    'offer_id': _Offers.offer_gb2_secondary.offerid,
                    'price': {'value': _Offers.offer_gb2_secondary.price - 90, 'currency': 'RUR'},
                },
                {
                    'feed_id': _Shops.third_party.datafeed_id,
                    'offer_id': _Offers.offer_bs2_secondary.offerid,
                    'price': {'value': _Offers.offer_bs2_secondary.price - 90, 'currency': 'RUR'},
                },
            ],
            allow_berubonus=True,
            allow_promocode=True,
        ),
        offers_matching_rules=[
            OffersMatchingRules(
                feed_offer_ids=[
                    [_Shops.third_party.datafeed_id, offerid]
                    for offerid in [
                        _Offers.offer_blue_flash.offerid,
                        _Offers.offer_with_two_promos.offerid,
                        _Offers.offer_gb2_secondary.offerid,
                        _Offers.offer_bs2_secondary.offerid,
                    ]
                ]
            )
        ],
    )

    promo_blue_set = Promo(
        promo_type=PromoType.BLUE_SET,
        feed_id=777,
        key='BLUE_SET_PROMO',
        url='http://яндекс.рф/',
        blue_set=PromoBlueSet(
            sets_content=[
                {
                    'items': [
                        {'offer_id': _Offers.blue_set_1.offerid},
                        {'offer_id': _Offers.blue_set_2.offerid},
                    ],
                },
                {
                    'items': [
                        {'offer_id': _Offers.offer_bs2_primary.offerid},
                        {'offer_id': _Offers.offer_bs2_secondary.offerid},
                    ],
                },
            ],
        ),
        offers_matching_rules=[
            OffersMatchingRules(
                feed_offer_ids=[
                    [_Shops.third_party.datafeed_id, offerid]
                    for offerid in [
                        _Offers.blue_set_1.offerid,
                        _Offers.offer_bs2_primary.offerid,
                        _Offers.offer_gb2_secondary.offerid,
                    ]
                ]
            )
        ],
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
                {
                    'feed_id': _Shops.third_party.datafeed_id,
                    'offer_id': _Offers.offer_bs2_secondary.offerid,
                    'discount_price': {
                        'value': _Offers.offer_bs2_secondary.price - 10,
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
        offers_matching_rules=[
            OffersMatchingRules(
                feed_offer_ids=[
                    [_Shops.third_party.datafeed_id, offerid]
                    for offerid in [
                        _Offers.offer_for_direct_discount.offerid,
                        _Offers.offer_bs2_secondary.offerid,
                        _Offers.offer_gb2_secondary.offerid,
                    ]
                ]
            )
        ],
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
        # mechanics_payment_type = MechanicsPaymentType.CPA,
        shop_promo_id='promo_code',
        conditions='buy at least 300321',
        offers_matching_rules=[
            OffersMatchingRules(
                feed_offer_ids=[
                    [_Shops.third_party.datafeed_id, offerid]
                    for offerid in [
                        create_offer_id('100001'),
                        create_offer_id('100003'),
                    ]
                ]
            )
        ],
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
            'enable_fast_promo_matcher=1;enable_fast_promo_matcher_test=1'
        ]

        _Offers.offer_for_cheapest_as_gift.promo = [
            _Promos.cheapest_as_gift,
            promo_blue_cashback,
        ]
        _Offers.offer_for_direct_discount.promo = [
            _Promos.promo_direct_discount,
            promo_blue_cashback,
        ]
        _Offers.offer_for_generic_bundle.promo = [
            _Promos.promo_generic_bundle,
            promo_blue_cashback,
        ]
        _Offers.offer_blue_flash.promo = [
            _Promos.promo_blue_flash,
            promo_blue_cashback,
        ]
        _Offers.blue_set_1.promo = [
            _Promos.promo_blue_set,
            promo_blue_cashback,
        ]

        _Offers.offer_with_two_promos.promo = [
            _Promos.cheapest_as_gift,
            _Promos.promo_blue_flash,
        ]

        _Offers.offer_gb2_secondary.promo = [
            _Promos.promo_direct_discount,
            _Promos.promo_code,
            _Promos.cheapest_as_gift,
            _Promos.promo_blue_flash,
            _Promos.promo_blue_3p_flash,
            _Promos.promo_blue_set,
            _Promos.promo_generic_bundle,
            promo_blue_cashback,
        ]

        _Offers.offer_bs2_secondary.promo = [
            _Promos.promo_direct_discount,
            _Promos.promo_code,
            _Promos.cheapest_as_gift,
            _Promos.promo_blue_flash,
            _Promos.promo_blue_3p_flash,
            _Promos.promo_generic_bundle,
            promo_blue_cashback,
        ]

        self.index.shops += [_Shops.third_party]

        self.index.mskus += [
            _Mskus.msku_for_cheapest_as_gift,
            _Mskus.msku_for_generic_bundle,
            _Mskus.msku_for_secondary,
            _Mskus.msku_for_blue_flash,
            _Mskus.msku_for_3p_flash_discount,
            _Mskus.msku_for_blue_set_1,
            _Mskus.msku_for_blue_set_2,
            _Mskus.msku_for_direct_discount,
            _Mskus.msku_for_secret_sale,
            _Mskus.msku_for_two_promos,
            _Mskus.msku_for_gb2_primary,
            _Mskus.msku_for_gb2_secondary,
            _Mskus.msku_for_bs2_primary,
            _Mskus.msku_for_bs2_secondary,
        ]

        self.index.promos += [
            _Promos.secret_sale,
            _Promos.promo_generic_bundle,
            _Promos.cheapest_as_gift,
            _Promos.promo_blue_flash,
            _Promos.promo_blue_3p_flash,
            _Promos.promo_blue_set,
            _Promos.promo_direct_discount,
            promo_blue_cashback,
        ]
        self.settings.loyalty_enabled = True
        self.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(
                whitelist=[
                    _Promos.cheapest_as_gift.key,
                    _Promos.promo_generic_bundle.key,
                    _Promos.promo_blue_flash.key,
                    _Promos.promo_blue_set.key,
                    promo_blue_cashback.key,
                ]
            )
        ]

    def __assert_prime_response_has_promo_type(self, msku, offer_waremd5, promo_type, additional_query_params=''):
        request = (
            'place=prime'
            '&rgb=blue'
            '&market-sku={msku}'
            '&numdoc=100'
            '&rids=0&regset=1&pp=18'
            '&yandexuid=1'
            '&perks=yandex_cashback'
            '&rearr-factors=market_enable_single_promo={single_promo}'
            '{additional_query_params}'
        )

        for enable_single_promo in [0, 1]:
            response = self.report.request_json(
                request.format(
                    msku=msku, single_promo=enable_single_promo, additional_query_params=additional_query_params
                )
            )
            single_promo_fragment = {'type': PromoType.BLUE_CASHBACK} if enable_single_promo == 1 else Absent()
            self.assertFragmentIn(
                response,
                [
                    {
                        'entity': 'offer',
                        'wareId': offer_waremd5,
                        'promo': single_promo_fragment,
                        'promos': [
                            {'type': PromoType.BLUE_CASHBACK},
                            {'type': promo_type},
                        ],
                    }
                ],
                allow_different_len=False,
            )

    def test_cheapest_as_gift(self):
        '''
        Проверяем что у оффера _Offers.offer_for_cheapest_as_gift в ответе в поле "promos" лежат 2 промо: blue-bashback + cheapest-as-gift
        а в поле "promo" только 1: cheapest-as-gift
        '''
        self.__assert_prime_response_has_promo_type(
            msku=_Mskus.msku_for_cheapest_as_gift.sku,
            offer_waremd5=_Offers.offer_for_cheapest_as_gift.waremd5,
            promo_type=PromoType.CHEAPEST_AS_GIFT,
        )

    def test_generic_bundle(self):
        '''
        Проверяем что у оффера _Offers.offer_for_generic_bundle в ответе в поле "promos" лежат 2 промо: blue-bashback + generic-bundle
        а в поле "promo" только 1: generic-bundle
        '''
        self.__assert_prime_response_has_promo_type(
            msku=_Mskus.msku_for_generic_bundle.sku,
            offer_waremd5=_Offers.offer_for_generic_bundle.waremd5,
            promo_type=PromoType.GENERIC_BUNDLE,
        )

    def test_blue_flash(self):
        '''
        Проверяем что у оффера _Offers.offer_blue_flash в ответе в поле "promos" лежат 2 промо: blue-bashback + blue-flash
        а в поле "promo" только 1: blue-flash
        '''
        self.__assert_prime_response_has_promo_type(
            msku=_Mskus.msku_for_blue_flash.sku,
            offer_waremd5=_Offers.offer_blue_flash.waremd5,
            promo_type=PromoType.BLUE_FLASH,
        )

    def test_blue_3p_flash_discount(self):
        '''
        Проверяем что у оффера _Offers.offer_3p_flash_discount в ответе в поле "promos" лежат 2 промо: blue-bashback + blue-3p-flash-discount
        а в поле "promo" только 1: blue-3p-flash-discount
        '''
        self.__assert_prime_response_has_promo_type(
            msku=_Mskus.msku_for_3p_flash_discount.sku,
            offer_waremd5=_Offers.offer_3p_flash_discount.waremd5,
            promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
        )

    def test_direct_discount(self):
        '''
        Проверяем что у оффера _Offers.offer_for_direct_discount в ответе в поле "promos" лежат 2 промо: blue-bashback + direct-discount
        а в поле "promo" только 1: direct-discount
        '''
        self.__assert_prime_response_has_promo_type(
            msku=_Mskus.msku_for_direct_discount.sku,
            offer_waremd5=_Offers.offer_for_direct_discount.waremd5,
            promo_type=PromoType.DIRECT_DISCOUNT,
        )

    def test_secret_sale(self):
        '''
        Проверяем что у оффера _Offers.offer_for_secret_sale в ответе в поле "promos" лежат 2 промо: blue-bashback + secret-sale
        а в поле "promo" только 1: secret-sale
        '''
        allowed_promos = '&allowed-promos={secret_sale_promo}'.format(secret_sale_promo=_Promos.secret_sale.key)
        self.__assert_prime_response_has_promo_type(
            msku=_Mskus.msku_for_secret_sale.sku,
            offer_waremd5=_Offers.offer_for_secret_sale.waremd5,
            promo_type=PromoType.SECRET_SALE,
            additional_query_params=allowed_promos,
        )

    def test_blue_set(self):
        '''
        Проверяем что у оффера _Offers.blue_set_1 в ответе в поле "promos" лежат 2 промо: blue-bashback + blue-set
        а в поле "promo" только 1: blue-set
        '''
        allowed_promos = '&allowed-promos={secret_sale_promo}'.format(secret_sale_promo=_Promos.promo_blue_set.key)
        self.__assert_prime_response_has_promo_type(
            msku=_Mskus.msku_for_blue_set_1.sku,
            offer_waremd5=_Offers.blue_set_1.waremd5,
            promo_type=PromoType.BLUE_SET,
            additional_query_params=allowed_promos,
        )

    def test_two_promos(self):
        '''
        Проверяем что у оффера _Offers.offer_with_two_promos в ответе в поле "promos" лежит 1 промо: blue-flash
        Второая промо cheapest-as-gift пролезть не должна, так как у нее приоритет ниже.
        '''

        request_tmpl = 'place=prime&rgb=blue&market-sku={msku}&numdoc=100&rids=0&regset=1&pp=18&yandexuid=1'

        response = self.report.request_json(request_tmpl.format(msku=_Mskus.msku_for_two_promos.sku))
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': _Offers.offer_with_two_promos.waremd5,
                    'promos': [
                        {'type': PromoType.BLUE_FLASH},
                    ],
                }
            ],
            allow_different_len=False,
        )
        self.assertFragmentIn(response, [{'promos': ElementCount(1)}])

    def test_generic_bundle_secondaries(self):
        '''
        MARKETOUT-37201, совместимость промо на вторичных офферах комплектных акций
        '''

        promo_types_on_generic_bundle_secondaries = (
            PromoType.DIRECT_DISCOUNT,
            PromoType.PROMO_CODE,
            PromoType.BLUE_CASHBACK,
            PromoType.GENERIC_BUNDLE_SECONDARY,
        )

        request_tmpl = 'place=prime&rgb=blue&market-sku={msku}&rids=0&regset=1&pp=18&yandexuid=1&perks=yandex_cashback&rearr-factors=market_promo_quantity_limit=4'

        response = self.report.request_json(request_tmpl.format(msku=_Mskus.msku_for_gb2_secondary.sku))
        for promo_type in promo_types_on_generic_bundle_secondaries:
            self.assertFragmentIn(
                response,
                [
                    {
                        'entity': 'offer',
                        'wareId': _Offers.offer_gb2_secondary.waremd5,
                        'promos': [
                            {'type': promo_type},
                        ],
                    }
                ],
            )

    def test_blue_set_secondaries(self):

        promo_types_on_blue_set_secondaries = (
            PromoType.DIRECT_DISCOUNT,
            PromoType.PROMO_CODE,
            PromoType.BLUE_CASHBACK,
            PromoType.BLUE_SET_SECONDARY,
        )

        request_tmpl = 'place=prime&rgb=blue&market-sku={msku}&rids=0&regset=1&pp=18&yandexuid=1&perks=yandex_cashback&rearr-factors=market_promo_quantity_limit=4'

        response = self.report.request_json(request_tmpl.format(msku=_Mskus.msku_for_bs2_secondary.sku))
        for promo_type in promo_types_on_blue_set_secondaries:
            self.assertFragmentIn(
                response,
                [
                    {
                        'entity': 'offer',
                        'wareId': _Offers.offer_bs2_secondary.waremd5,
                        'promos': [
                            {'type': promo_type},
                        ],
                    }
                ],
            )


if __name__ == '__main__':
    main()
