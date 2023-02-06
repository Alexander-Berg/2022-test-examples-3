#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    DynamicBlueGenericBundlesPromos,
    DynamicPromo,
    Promo,
    PromoMSKU,
    PromoSaleDetails,
    PromoType,
    Shop,
)
from core.types import PromoCheapestAsGift
from core.types.offer_promo import (
    PromoBlueFlash,
    PromoBlueSet,
    make_generic_bundle_content,
    PromoDirectDiscount,
    PromoBlueCashback,
)
from core.types.sku import MarketSku, BlueOffer


class _Shops(object):
    third_party = Shop(
        fesh=777, datafeed_id=777, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL
    )


promo_blue_cashback = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key='Promo_blue_cashback',
    url='http://blue_cashback.com/',
    blue_cashback=PromoBlueCashback(share=0.2, version=10, priority=1),
)


def make_offer_for_promo(id, promo=None):
    return BlueOffer(
        price=500,
        offerid='blue.offer.{}'.format(id),
        feedid=_Shops.third_party.datafeed_id,
        fesh=_Shops.third_party.fesh,
        waremd5='Sku{}____-vm1Goleg'.format(id),
        promo=promo,
    )


class _Offers(object):
    offer_for_cheapest_as_gift = make_offer_for_promo('222222')
    offer_for_generic_bundle = make_offer_for_promo('333333')
    offer_secondary = make_offer_for_promo('444444')
    offer_blue_flash = make_offer_for_promo('555555')
    blue_set_1 = make_offer_for_promo('set111')
    blue_set_2 = make_offer_for_promo('set222')
    offer_for_direct_discount = make_offer_for_promo("ddisc6")
    offer_for_blue_cashback = make_offer_for_promo("bcash7", promo_blue_cashback)


def make_msku(id, offers):
    return MarketSku(sku=id, hyperid=2, blue_offers=offers)


class _Mskus(object):
    msku_for_cheapest_as_gift = make_msku(222222, [_Offers.offer_for_cheapest_as_gift])
    msku_for_generic_bundle = make_msku(333333, [_Offers.offer_for_generic_bundle])
    msku_for_secondary = make_msku(444444, [_Offers.offer_secondary])
    msku_for_blue_flash = make_msku(555555, [_Offers.offer_blue_flash])
    msku_for_blue_set_1 = make_msku(666666, [_Offers.blue_set_1])
    msku_for_blue_set_2 = make_msku(777777, [_Offers.blue_set_2])
    msku_for_direct_discount = make_msku(888888, [_Offers.offer_for_direct_discount])
    msku_for_blue_cashback = make_msku(999999, [_Offers.offer_for_blue_cashback])


class _Promos(object):
    secret_sale = Promo(
        promo_type=PromoType.SECRET_SALE,
        key='Secret_sale_promo_key1',
        secret_sale_details=PromoSaleDetails(
            msku_list=[int(_Mskus.msku_for_cheapest_as_gift.sku)], discount_percent_list=[10]
        ),
        source_promo_id='Secret_sale_promo_key1',
    )
    secret_sale2 = Promo(
        promo_type=PromoType.SECRET_SALE,
        key='Secret_sale_promo_key2',
        secret_sale_details=PromoSaleDetails(
            msku_list=[int(_Mskus.msku_for_generic_bundle.sku)], discount_percent_list=[10]
        ),
        source_promo_id='Secret_sale_promo_key2',
    )
    secret_sale3 = Promo(
        promo_type=PromoType.SECRET_SALE,
        key='Secret_sale_promo_key3',
        secret_sale_details=PromoSaleDetails(
            msku_list=[
                int(_Mskus.msku_for_blue_flash.sku),
                int(_Mskus.msku_for_blue_set_1.sku),
            ],
            discount_percent_list=[10, 10],
        ),
        source_promo_id='Secret_sale_promo_key3',
    )
    flash_discount = Promo(
        promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
        key='FLvklxUgdnawSJPG4UhZGA',
        shop_promo_id=1,
        feed_id=_Shops.third_party.datafeed_id,
        mskus=[
            PromoMSKU(msku=str(_Mskus.msku_for_cheapest_as_gift.sku), market_promo_price=500, market_old_price=1000)
        ],
    )
    cheapest_as_gift = Promo(
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
            allow_berubonus=True,
            allow_promocode=True,
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
    )
    promo_blue_flash = Promo(
        promo_type=PromoType.BLUE_FLASH,
        key='BLUE_FLASH_PROMO',
        blue_flash=PromoBlueFlash(
            items=[
                {
                    'feed_id': 777,
                    'offer_id': _Offers.offer_blue_flash.offerid,
                    'price': {'value': _Offers.offer_blue_flash.price - 70, 'currency': 'RUR'},
                },
            ],
            allow_berubonus=True,
            allow_promocode=True,
        ),
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
                }
            ],
        ),
    )
    promo_direct_discount = Promo(
        promo_type=PromoType.DIRECT_DISCOUNT,
        feed_id=_Shops.third_party.datafeed_id,
        key='Promo_direct_discount',
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
                }
            ],
            allow_berubonus=True,
            allow_promocode=True,
        ),
    )


class T(TestCase):
    @classmethod
    def prepare(self):
        self.settings.default_search_experiment_flags += [
            'enable_fast_promo_matcher=0;enable_fast_promo_matcher_test=1'
        ]

        _Offers.offer_for_cheapest_as_gift.promo = [
            _Promos.secret_sale,
            _Promos.flash_discount,
            _Promos.cheapest_as_gift,
        ]
        _Offers.offer_for_generic_bundle.promo = [
            _Promos.secret_sale2,
            _Promos.promo_generic_bundle,
        ]
        _Offers.offer_blue_flash.promo = [
            _Promos.secret_sale3,
        ]
        _Offers.blue_set_1.promo = [
            _Promos.secret_sale3,
            _Promos.promo_blue_set,
        ]
        _Offers.blue_set_2.promo = [
            _Promos.promo_blue_set,
        ]
        _Offers.offer_blue_flash.promo = [
            _Promos.promo_blue_flash,
        ]
        _Offers.offer_for_direct_discount.promo = [
            _Promos.promo_direct_discount,
        ]

        self.index.shops += [_Shops.third_party]

        self.index.mskus += [
            _Mskus.msku_for_cheapest_as_gift,
            _Mskus.msku_for_generic_bundle,
            _Mskus.msku_for_secondary,
            _Mskus.msku_for_blue_flash,
            _Mskus.msku_for_blue_set_1,
            _Mskus.msku_for_blue_set_2,
            _Mskus.msku_for_direct_discount,
            _Mskus.msku_for_blue_cashback,
        ]

        self.index.promos += [
            _Promos.flash_discount,
            _Promos.promo_generic_bundle,
            _Promos.cheapest_as_gift,
            _Promos.secret_sale,
            _Promos.secret_sale2,
            _Promos.secret_sale3,
            _Promos.promo_blue_flash,
            _Promos.promo_blue_set,
            _Promos.promo_direct_discount,
        ]
        self.settings.loyalty_enabled = True
        self.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(
                whitelist=[
                    _Promos.cheapest_as_gift.key,
                    _Promos.promo_generic_bundle.key,
                    _Promos.promo_blue_flash.key,
                    _Promos.promo_blue_set.key,
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
            '{additional_query_params}'
        )

        response = self.report.request_json(request.format(msku=msku, additional_query_params=additional_query_params))
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': offer_waremd5,
                    'promos': [{'type': promo_type}],
                }
            ],
            allow_different_len=False,
        )

    def test_priority_blue_flash(self):
        for (msku, offer_waremd5, secret_sale_promo_key) in [
            (_Mskus.msku_for_blue_flash.sku, _Offers.offer_blue_flash.waremd5, _Promos.secret_sale3.key)
        ]:
            allowed_promos = '&allowed-promos={secret_sale_promo}'.format(secret_sale_promo=secret_sale_promo_key)
            self.__assert_prime_response_has_promo_type(
                msku=msku,
                offer_waremd5=offer_waremd5,
                promo_type=PromoType.SECRET_SALE,
                additional_query_params=allowed_promos,
            )
        self.dynamic.market_dynamic.disabled_promos = [
            DynamicPromo(promo_key=_Promos.secret_sale3.key)
        ]  # отключаем SECRET_SALE, должен появиться BLUE_FLASH
        for (msku, offer_waremd5, blue_flash_promo_key) in [
            (_Mskus.msku_for_blue_flash.sku, _Offers.offer_blue_flash.waremd5, _Promos.promo_blue_flash.key)
        ]:
            allowed_promos = '&allowed-promos={blue_flash_promo}'.format(blue_flash_promo=blue_flash_promo_key)
            self.__assert_prime_response_has_promo_type(
                msku=msku,
                offer_waremd5=offer_waremd5,
                promo_type=PromoType.BLUE_FLASH,
                additional_query_params=allowed_promos,
            )

    def test_secret_sale_priority(self):
        '''
        Проверяем приоритет secret sales
        акция включается через allowed-promos
        '''
        for (msku, offer_waremd5, secret_sale_promo_key) in [
            (_Mskus.msku_for_cheapest_as_gift.sku, _Offers.offer_for_cheapest_as_gift.waremd5, _Promos.secret_sale.key),
            (_Mskus.msku_for_generic_bundle.sku, _Offers.offer_for_generic_bundle.waremd5, _Promos.secret_sale2.key),
        ]:
            allowed_promos = '&allowed-promos={secret_sale_promo}'.format(secret_sale_promo=secret_sale_promo_key)
            self.__assert_prime_response_has_promo_type(
                msku=msku,
                offer_waremd5=offer_waremd5,
                promo_type=PromoType.SECRET_SALE,
                additional_query_params=allowed_promos,
            )

    def test_cheapest_as_gift_priority(self):
        '''
        Проверяем что если через allowed_promos не выставить secret sales,
        то для оффера у которого есть акция CHEAPEST AS GIFT, эта акция пойдет следующим приоритетом
        ВАЖНО!!! не тестируем generic bundle и cheapest as gift на одном оффере, т.к репорт не позволяет этим акциям жить на одном оффере
        '''
        self.__assert_prime_response_has_promo_type(
            msku=_Mskus.msku_for_cheapest_as_gift.sku,
            offer_waremd5=_Offers.offer_for_cheapest_as_gift.waremd5,
            promo_type=PromoType.CHEAPEST_AS_GIFT,
        )

    def test_generic_bundle_priority(self):
        '''
        Проверяем что если через allowed_promos не выставить secret sales,
        то для оффера у которого есть акция Generic Bundle, эта акция пойдет следующим приоритетом
        ВАЖНО!!! не тестируем generic bundle и cheapest as gift на одном оффере, т.к репорт не позволяет этим акциям жить на одном оффере
        '''
        self.__assert_prime_response_has_promo_type(
            msku=_Mskus.msku_for_generic_bundle.sku,
            offer_waremd5=_Offers.offer_for_generic_bundle.waremd5,
            promo_type=PromoType.GENERIC_BUNDLE,
        )

    def test_direct_discount_priority(self):
        '''
        Проверяем что если через allowed_promos не выставить secret sales,
        то для оффера, у которого есть акция DirectDiscount, эта акция пойдет следующим приоритетом
        '''
        self.__assert_prime_response_has_promo_type(
            msku=_Mskus.msku_for_direct_discount.sku,
            offer_waremd5=_Offers.offer_for_direct_discount.waremd5,
            promo_type=PromoType.DIRECT_DISCOUNT,
        )

    def test_blue_cashback_priority(self):
        '''
        Проверяем что если через allowed_promos не выставить secret sales,
        то для оффера, у которого есть акция BlueCashback, эта акция пойдет следующим приоритетом
        '''
        self.__assert_prime_response_has_promo_type(
            msku=_Mskus.msku_for_blue_cashback.sku,
            offer_waremd5=_Offers.offer_for_blue_cashback.waremd5,
            promo_type=PromoType.BLUE_CASHBACK,
            additional_query_params='&perks=yandex_cashback',
        )

    def test_priority_blue_set(self):
        for (msku, offer_waremd5, secret_sale_promo_key) in [
            (_Mskus.msku_for_blue_set_1.sku, _Offers.blue_set_1.waremd5, _Promos.secret_sale3.key)
        ]:
            allowed_promos = '&allowed-promos={secret_sale_promo}'.format(secret_sale_promo=secret_sale_promo_key)
            self.__assert_prime_response_has_promo_type(
                msku=msku,
                offer_waremd5=offer_waremd5,
                promo_type=PromoType.SECRET_SALE,
                additional_query_params=allowed_promos,
            )
        self.dynamic.market_dynamic.disabled_promos = [
            DynamicPromo(promo_key=_Promos.secret_sale3.key)
        ]  # отключаем SECRET_SALE, должен появиться BLUE_SET
        for (msku, offer_waremd5, secret_sale_promo_key) in [
            (_Mskus.msku_for_blue_set_1.sku, _Offers.blue_set_1.waremd5, _Promos.secret_sale3.key)
        ]:
            allowed_promos = '&allowed-promos={secret_sale_promo}'.format(secret_sale_promo=secret_sale_promo_key)
            self.__assert_prime_response_has_promo_type(
                msku=msku,
                offer_waremd5=offer_waremd5,
                promo_type=PromoType.BLUE_SET,
                additional_query_params=allowed_promos,
            )

    def test_flash_discount_priority(self):
        '''
        Проверяем что если через allowed_promos не выставить secret sales и через динамик выключить cheapest as gift,
        то для оффера у которого есть акция FLASH DISCOUNT, эта акция пойдет следующим приоритетом
        ВАЖНО!!! не тестируем flash discount и bundle на одном оффере, т.к репорт не позволяет этим акциям жить на одном оффере одновременно
        '''
        self.dynamic.market_dynamic.disabled_promos = [DynamicPromo(promo_key=_Promos.cheapest_as_gift.key)]
        self.__assert_prime_response_has_promo_type(
            msku=_Mskus.msku_for_cheapest_as_gift.sku,
            offer_waremd5=_Offers.offer_for_cheapest_as_gift.waremd5,
            promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
        )


if __name__ == '__main__':
    main()
