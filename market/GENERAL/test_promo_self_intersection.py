#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import (
    main,
    TestCase,
)
from core.types import DynamicBlueGenericBundlesPromos, Promo, PromoType, Shop
from core.types.offer_promo import (
    PromoBlueCashback,
    PromoDirectDiscount,
    PromoBlueSet,
    PromoBlueFlash,
    PromoSaleDetails,
    PromoCheapestAsGift,
    make_generic_bundle_content,
)
from core.types.sku import (
    BlueOffer,
    MarketSku,
)
from market.pylibrary.const.offer_promo import MechanicsPaymentType

from itertools import count


DEFAULT_FEED_ID = 777
DEFAULT_SHOP_ID = 7770
BLUE_CASHBACK = 'blue_cashback'
BLUE_FLASH = 'blue_flash'
SECRET_SALE = 'secret_sale'
CHEAPEST_AS_GIFT = 'cheapest_as_gift'
DIRECT_DISCOUNT = 'direct_discount'
GENERIC_BUNDLE = 'generic_bundle'
BLUE_SET = 'blue_set'
PROMO_CODE = 'promo_code'


def build_ware_md5(offerid):
    return '{}w'.format(offerid.ljust(21, "_"))


def create_blue_offer(offerid, price, promo=None, fesh=DEFAULT_SHOP_ID, feed=DEFAULT_FEED_ID):
    params = dict(
        waremd5=build_ware_md5(offerid),
        price=price,
        fesh=fesh,
        feedid=feed,
        promo=promo,
        offerid='shop_sku_{}'.format(offerid),
    )

    return BlueOffer(**params)


def create_msku(sku, offers_list, hid=None):
    msku_args = dict(sku=sku, hyperid=2, blue_offers=offers_list)

    return MarketSku(**msku_args)


class T(TestCase):
    """
    Проверяем, что количество выдаваемых "promos" равно 1
    """

    Offers = {}
    Promos = {}
    Mskus = {}
    Sku = count(start=100900)

    def build_request(self, place, msku, rearr_flags):
        request = "place={place}&rgb=blue&numdoc=100&regset=0&pp=18&yandexuid=1".format(place=place)
        request += '&perks=yandex_cashback'  # для кэшбека
        request += '&allowed-promos={}_1'.format(SECRET_SALE)  # для закрытых распродаж
        request += '&allowed-promos={}_2'.format(SECRET_SALE)  # для закрытых распродаж
        request += '&rearr-factors={}'.format(";".join("{}={}".format(key, val) for key, val in rearr_flags.items()))
        request += "&market-sku={msku}".format(msku=msku.sku)
        return request

    def blue_offer_checker(self, msku, offer, place, expected_promo_type, rearr_flag):
        req = self.build_request(place, msku, rearr_flag)
        response = self.report.request_json(req)
        """
        Акции были созданны намеренно неразличимы по приоритету для репорта.
        Проверяем, что даже в этом случае репорт будет всегда возвращать только одну акуцию определенного типа в блоке "promos"
        """

        self.assertFragmentIn(
            response,
            {
                'offers': {
                    'items': [
                        {
                            'entity': 'offer',
                            'wareId': offer.waremd5,
                            'promos': [{'type': expected_promo_type}],
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

    def check_promos(self, msku, offer, expected_promo_type):
        flags = {
            "market_enable_multipromo": 1,
        }

        for place in ('prime', 'sku_offers'):
            self.blue_offer_checker(msku, offer, place, expected_promo_type, flags)

    @classmethod
    def prepare(cls):
        shop_pr = Shop(
            fesh=DEFAULT_SHOP_ID,
            datafeed_id=DEFAULT_FEED_ID,
            priority_region=213,
            supplier_type=Shop.THIRD_PARTY,
            blue=Shop.BLUE_REAL,
        )
        cls.index.shops += [shop_pr]
        cls.settings.loyalty_enabled = True
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=0;enable_fast_promo_matcher_test=0']

    @classmethod
    def prepare_cashback_intersection(cls):
        BLUE_CASHBACK_1 = '{}_1'.format(BLUE_CASHBACK)
        BLUE_CASHBACK_2 = '{}_2'.format(BLUE_CASHBACK)
        T.Offers[BLUE_CASHBACK] = create_blue_offer('offer_blue_cashback', price=100)
        T.Promos[BLUE_CASHBACK_1] = Promo(
            promo_type=PromoType.BLUE_CASHBACK,
            description=BLUE_CASHBACK_1,
            url='blue_cashback_url',
            key='blue_cashback_pr',
            blue_cashback=PromoBlueCashback(share=0.2, version=10, priority=1),
        )

        T.Promos[BLUE_CASHBACK_2] = Promo(
            promo_type=PromoType.BLUE_CASHBACK,
            description=BLUE_CASHBACK_2,
            url='blue_another_cashback_url',
            key='blue_another_cashback_pr',
            blue_cashback=PromoBlueCashback(share=0.3, version=10, priority=2),
        )

        T.Offers[BLUE_CASHBACK].promo = [T.Promos[BLUE_CASHBACK_1], T.Promos[BLUE_CASHBACK_2]]

        T.Mskus[BLUE_CASHBACK] = create_msku(next(T.Sku), [T.Offers[BLUE_CASHBACK]])

        cls.index.mskus += [
            T.Mskus[BLUE_CASHBACK],
        ]

    def test_blue_cashback_intersection(self):
        self.check_promos(T.Mskus[BLUE_CASHBACK], T.Offers[BLUE_CASHBACK], PromoType.BLUE_CASHBACK)

    @classmethod
    def prepare_blue_flash_intersection(cls):
        BLUE_FLASH_1 = '{}_1'.format(BLUE_FLASH)
        BLUE_FLASH_2 = '{}_2'.format(BLUE_FLASH)
        T.Offers[BLUE_FLASH] = create_blue_offer(BLUE_FLASH, price=100)
        T.Promos[BLUE_FLASH_1] = Promo(
            promo_type=PromoType.BLUE_FLASH,
            description=BLUE_FLASH_1,
            key=BLUE_FLASH_1,
            url='syrin.com',
            blue_flash=PromoBlueFlash(
                items=[
                    {
                        'feed_id': DEFAULT_FEED_ID,
                        'offer_id': T.Offers[BLUE_FLASH].offerid,
                        'price': {'value': 60, 'currency': 'RUR'},
                    },
                ],
                allow_berubonus=True,
                allow_promocode=True,
            ),
        )
        T.Promos[BLUE_FLASH_2] = Promo(
            promo_type=PromoType.BLUE_FLASH,
            description=BLUE_FLASH_2,
            key=BLUE_FLASH_2,
            url='syrin.com',
            blue_flash=PromoBlueFlash(
                items=[
                    {
                        'feed_id': DEFAULT_FEED_ID,
                        'offer_id': T.Offers[BLUE_FLASH].offerid,
                        'price': {'value': 50, 'currency': 'RUR'},
                    },
                ],
                allow_berubonus=True,
                allow_promocode=True,
            ),
        )
        T.Offers[BLUE_FLASH].promo = [T.Promos[BLUE_FLASH_1], T.Promos[BLUE_FLASH_2]]

        T.Mskus[BLUE_FLASH] = create_msku(next(T.Sku), [T.Offers[BLUE_FLASH]])

        cls.index.mskus += [
            T.Mskus[BLUE_FLASH],
        ]

        cls.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(
                whitelist=[
                    T.Promos[BLUE_FLASH_1].key,
                    T.Promos[BLUE_FLASH_2].key,
                ]
            )
        ]

    def test_blue_flash_intersection(self):
        self.check_promos(T.Mskus[BLUE_FLASH], T.Offers[BLUE_FLASH], PromoType.BLUE_FLASH)

    @classmethod
    def prepare_secret_sale_intersection(cls):
        SECRET_SALE_1 = '{}_1'.format(SECRET_SALE)
        SECRET_SALE_2 = '{}_2'.format(SECRET_SALE)
        T.Offers[SECRET_SALE] = create_blue_offer(SECRET_SALE, price=100)
        T.Mskus[SECRET_SALE] = create_msku(next(T.Sku), [T.Offers[SECRET_SALE]])

        T.Promos[SECRET_SALE_1] = Promo(
            promo_type=PromoType.SECRET_SALE,
            key=SECRET_SALE_1,
            source_promo_id=SECRET_SALE_1,
            secret_sale_details=PromoSaleDetails(msku_list=[int(T.Mskus[SECRET_SALE].sku)], discount_percent_list=[10]),
        )
        T.Promos[SECRET_SALE_2] = Promo(
            promo_type=PromoType.SECRET_SALE,
            key=SECRET_SALE_2,
            source_promo_id=SECRET_SALE_2,
            secret_sale_details=PromoSaleDetails(msku_list=[int(T.Mskus[SECRET_SALE].sku)], discount_percent_list=[12]),
        )
        T.Offers[SECRET_SALE].promo = [T.Promos[SECRET_SALE_1], T.Promos[SECRET_SALE_2]]

        cls.index.mskus += [
            T.Mskus[SECRET_SALE],
        ]

        cls.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(
                whitelist=[
                    T.Promos[SECRET_SALE_1].key,
                    T.Promos[SECRET_SALE_2].key,
                ]
            )
        ]

    def test_secret_sale_intersection(self):
        self.check_promos(T.Mskus[SECRET_SALE], T.Offers[SECRET_SALE], PromoType.SECRET_SALE)

    @classmethod
    def prepare_cheapest_as_gift_intersection(cls):
        CHEAPEST_AS_GIFT_1 = '{}_1'.format(CHEAPEST_AS_GIFT)
        CHEAPEST_AS_GIFT_2 = '{}_2'.format(CHEAPEST_AS_GIFT)
        T.Offers[CHEAPEST_AS_GIFT] = create_blue_offer(CHEAPEST_AS_GIFT, price=100)
        T.Mskus[CHEAPEST_AS_GIFT] = create_msku(next(T.Sku), [T.Offers[CHEAPEST_AS_GIFT]])

        T.Promos[CHEAPEST_AS_GIFT_1] = Promo(
            promo_type=PromoType.CHEAPEST_AS_GIFT,
            feed_id=DEFAULT_FEED_ID,
            key=CHEAPEST_AS_GIFT_1,
            url='witcher.pl',
            cheapest_as_gift=PromoCheapestAsGift(
                offer_ids=[
                    (DEFAULT_FEED_ID, T.Offers[CHEAPEST_AS_GIFT].offerid),
                ],
                count=3,
                promo_url='url',
                link_text='some text',
                allow_berubonus=False,
                allow_promocode=False,
            ),
        )
        T.Promos[CHEAPEST_AS_GIFT_2] = Promo(
            promo_type=PromoType.CHEAPEST_AS_GIFT,
            feed_id=DEFAULT_FEED_ID,
            key=CHEAPEST_AS_GIFT_2,
            url='witcher.pl',
            cheapest_as_gift=PromoCheapestAsGift(
                offer_ids=[
                    (DEFAULT_FEED_ID, T.Offers[CHEAPEST_AS_GIFT].offerid),
                ],
                count=4,
                promo_url='url',
                link_text='some text',
                allow_berubonus=False,
                allow_promocode=False,
            ),
        )
        T.Offers[CHEAPEST_AS_GIFT].promo = [T.Promos[CHEAPEST_AS_GIFT_1], T.Promos[CHEAPEST_AS_GIFT_2]]

        cls.index.mskus += [
            T.Mskus[CHEAPEST_AS_GIFT],
        ]

        cls.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(
                whitelist=[
                    T.Promos[CHEAPEST_AS_GIFT_1].key,
                    T.Promos[CHEAPEST_AS_GIFT_2].key,
                ]
            )
        ]

    def test_cheapest_as_gift_intersection(self):
        self.check_promos(T.Mskus[CHEAPEST_AS_GIFT], T.Offers[CHEAPEST_AS_GIFT], PromoType.CHEAPEST_AS_GIFT)

    @classmethod
    def prepare_direct_discount_intersection(cls):
        DIRECT_DISCOUNT_1 = '{}_1'.format(DIRECT_DISCOUNT)
        DIRECT_DISCOUNT_2 = '{}_2'.format(DIRECT_DISCOUNT)
        T.Offers[DIRECT_DISCOUNT] = create_blue_offer(DIRECT_DISCOUNT, price=100)
        T.Mskus[DIRECT_DISCOUNT] = create_msku(next(T.Sku), [T.Offers[DIRECT_DISCOUNT]])

        T.Promos[DIRECT_DISCOUNT_1] = Promo(
            promo_type=PromoType.DIRECT_DISCOUNT,
            feed_id=888,
            key=DIRECT_DISCOUNT_1,
            url='direct_discount_pr.ru',
            direct_discount=PromoDirectDiscount(
                items=[
                    {
                        'feed_id': DEFAULT_FEED_ID,
                        'offer_id': T.Offers[DIRECT_DISCOUNT].offerid,
                        'discount_price': {
                            'value': 40,
                            'currency': 'RUR',
                        },
                        'old_price': {
                            'value': 100,
                            'currency': 'RUR',
                        },
                    }
                ],
                allow_berubonus=True,
                allow_promocode=True,
            ),
        )
        T.Promos[DIRECT_DISCOUNT_2] = Promo(
            promo_type=PromoType.DIRECT_DISCOUNT,
            feed_id=889,
            key=DIRECT_DISCOUNT_2,
            url='direct_discount_pr.ru',
            direct_discount=PromoDirectDiscount(
                items=[
                    {
                        'feed_id': DEFAULT_FEED_ID,
                        'offer_id': T.Offers[DIRECT_DISCOUNT].offerid,
                        'discount_price': {
                            'value': 30,
                            'currency': 'RUR',
                        },
                        'old_price': {
                            'value': 100,
                            'currency': 'RUR',
                        },
                    }
                ],
                allow_berubonus=True,
                allow_promocode=True,
            ),
        )
        T.Offers[DIRECT_DISCOUNT].promo = [T.Promos[DIRECT_DISCOUNT_1], T.Promos[DIRECT_DISCOUNT_2]]

        cls.index.mskus += [
            T.Mskus[DIRECT_DISCOUNT],
        ]

        cls.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(
                whitelist=[
                    T.Promos[DIRECT_DISCOUNT_1].key,
                    T.Promos[DIRECT_DISCOUNT_2].key,
                ]
            )
        ]

    def test_direct_discount_intersection(self):
        self.check_promos(T.Mskus[DIRECT_DISCOUNT], T.Offers[DIRECT_DISCOUNT], PromoType.DIRECT_DISCOUNT)

    @classmethod
    def prepare_generic_bundle_intersection(cls):
        GENERIC_BUNDLE_SECONDARY = '{}_second'.format(GENERIC_BUNDLE)
        GENERIC_BUNDLE_1 = '{}_1'.format(GENERIC_BUNDLE)
        GENERIC_BUNDLE_2 = '{}_2'.format(GENERIC_BUNDLE)
        T.Offers[GENERIC_BUNDLE] = create_blue_offer(GENERIC_BUNDLE, price=100)
        T.Offers[GENERIC_BUNDLE_SECONDARY] = create_blue_offer(GENERIC_BUNDLE_SECONDARY, price=11)
        T.Mskus[GENERIC_BUNDLE] = create_msku(next(T.Sku), [T.Offers[GENERIC_BUNDLE]])
        T.Mskus[GENERIC_BUNDLE_SECONDARY] = create_msku(next(T.Sku), [T.Offers[GENERIC_BUNDLE_SECONDARY]])

        T.Promos[GENERIC_BUNDLE_1] = Promo(
            promo_type=PromoType.GENERIC_BUNDLE,
            feed_id=DEFAULT_FEED_ID,
            key=GENERIC_BUNDLE_1,
            url='promo_generic_bundle_pr.com',
            generic_bundles_content=[
                make_generic_bundle_content(
                    T.Offers[GENERIC_BUNDLE].offerid, T.Offers[GENERIC_BUNDLE_SECONDARY].offerid, 10
                ),
            ],
        )
        T.Promos[GENERIC_BUNDLE_2] = Promo(
            promo_type=PromoType.GENERIC_BUNDLE,
            feed_id=DEFAULT_FEED_ID,
            key=GENERIC_BUNDLE_2,
            url='promo_generic_bundle_pr.com',
            generic_bundles_content=[
                make_generic_bundle_content(
                    T.Offers[GENERIC_BUNDLE].offerid, T.Offers[GENERIC_BUNDLE_SECONDARY].offerid, 20
                ),
            ],
        )
        T.Offers[GENERIC_BUNDLE].promo = [T.Promos[GENERIC_BUNDLE_1], T.Promos[GENERIC_BUNDLE_2]]

        cls.index.mskus += [
            T.Mskus[GENERIC_BUNDLE],
            T.Mskus[GENERIC_BUNDLE_SECONDARY],
        ]

        cls.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(
                whitelist=[
                    T.Promos[GENERIC_BUNDLE_1].key,
                    T.Promos[GENERIC_BUNDLE_2].key,
                ]
            )
        ]

    def test_generic_bundle_intersection(self):
        self.check_promos(T.Mskus[GENERIC_BUNDLE], T.Offers[GENERIC_BUNDLE], PromoType.GENERIC_BUNDLE)

    @classmethod
    def prepare_blue_set_intersection(cls):
        BLUE_SET_SECONDARY = '{}_second'.format(BLUE_SET)
        BLUE_SET_1 = '{}_1'.format(BLUE_SET)
        BLUE_SET_2 = '{}_2'.format(BLUE_SET)
        T.Offers[BLUE_SET] = create_blue_offer(BLUE_SET, price=100)
        T.Offers[BLUE_SET_SECONDARY] = create_blue_offer(BLUE_SET_SECONDARY, price=11)
        T.Mskus[BLUE_SET] = create_msku(next(T.Sku), [T.Offers[BLUE_SET]])
        T.Mskus[BLUE_SET_SECONDARY] = create_msku(next(T.Sku), [T.Offers[BLUE_SET_SECONDARY]])

        T.Promos[BLUE_SET_1] = Promo(
            promo_type=PromoType.BLUE_SET,
            feed_id=DEFAULT_FEED_ID,
            key=BLUE_SET_1,
            url='blue_set_pr.ru',
            blue_set=PromoBlueSet(
                sets_content=[
                    {
                        'items': [
                            {'offer_id': T.Offers[BLUE_SET].offerid},
                            {'offer_id': T.Offers[BLUE_SET_SECONDARY].offerid},
                        ],
                    }
                ],
            ),
        )
        T.Promos[BLUE_SET_2] = Promo(
            promo_type=PromoType.BLUE_SET,
            feed_id=DEFAULT_FEED_ID,
            key=BLUE_SET_2,
            url='blue_set_pr.ru',
            blue_set=PromoBlueSet(
                sets_content=[
                    {
                        'items': [
                            {'offer_id': T.Offers[BLUE_SET].offerid},
                            {'offer_id': T.Offers[BLUE_SET_SECONDARY].offerid},
                        ],
                    }
                ],
            ),
        )
        T.Offers[BLUE_SET].promo = [T.Promos[BLUE_SET_1], T.Promos[BLUE_SET_2]]
        T.Offers[BLUE_SET_SECONDARY].promo = [T.Promos[BLUE_SET_1], T.Promos[BLUE_SET_2]]

        cls.index.mskus += [
            T.Mskus[BLUE_SET],
            T.Mskus[BLUE_SET_SECONDARY],
        ]

        cls.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(
                whitelist=[
                    T.Promos[BLUE_SET_1].key,
                    T.Promos[BLUE_SET_2].key,
                ]
            )
        ]

    def test_blue_set_intersection(self):
        self.check_promos(T.Mskus[BLUE_SET], T.Offers[BLUE_SET], PromoType.BLUE_SET)

    @classmethod
    def prepare_promo_code_intersection(cls):
        PROMO_CODE_1 = '{}_1'.format(PROMO_CODE)
        PROMO_CODE_2 = '{}_2'.format(PROMO_CODE)
        T.Offers[PROMO_CODE] = create_blue_offer(PROMO_CODE, price=500)
        T.Mskus[PROMO_CODE] = create_msku(next(T.Sku), [T.Offers[PROMO_CODE]])

        T.Promos[PROMO_CODE_1] = Promo(
            promo_type=PromoType.PROMO_CODE,
            promo_code=PROMO_CODE_1,
            discount_value=125,
            discount_currency='RUR',
            key=PROMO_CODE_1,
            url='promocode_pr.ru',
            mechanics_payment_type=MechanicsPaymentType.CPA,
        )
        T.Promos[PROMO_CODE_2] = Promo(
            promo_type=PromoType.PROMO_CODE,
            promo_code=PROMO_CODE_2,
            discount_value=115,
            discount_currency='RUR',
            key=PROMO_CODE_2,
            url='promocode_pr.ru',
            mechanics_payment_type=MechanicsPaymentType.CPA,
        )
        T.Offers[PROMO_CODE].promo = [T.Promos[PROMO_CODE_1], T.Promos[PROMO_CODE_2]]

        cls.index.mskus += [
            T.Mskus[PROMO_CODE],
        ]

        cls.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(
                whitelist=[
                    T.Promos[PROMO_CODE_1].key,
                    T.Promos[PROMO_CODE_2].key,
                ]
            )
        ]

    def test_promo_code_intersection(self):
        self.check_promos(T.Mskus[PROMO_CODE], T.Offers[PROMO_CODE], PromoType.PROMO_CODE)


if __name__ == '__main__':
    main()
