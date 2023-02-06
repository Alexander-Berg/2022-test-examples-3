#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent
from core.testcase import (
    main,
    TestCase,
)
from core.types import DynamicBlueGenericBundlesPromos, DynamicPromo, Promo, PromoType, Shop
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


DEFAULT_FEED_ID = 777
DEFAULT_SHOP_ID = 7770
DEFAULT_PROMO_QUANTITY_LIMIT = 4


def build_ware_md5(offerid):
    insert = offerid.ljust(14, "_")
    return "Sku{}Goleg".format(insert)


class _Shops(object):
    shop_pr = Shop(
        fesh=DEFAULT_SHOP_ID,
        datafeed_id=DEFAULT_FEED_ID,
        priority_region=213,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
    )


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


class _Offers(object):
    blue_offer_pr = create_blue_offer('offer1', price=100)
    blue_offer_pr_secondary = create_blue_offer('offer2', price=100)


def create_msku(sku, offers_list, hid=None):
    msku_args = dict(sku=sku, hyperid=2, blue_offers=offers_list)

    return MarketSku(**msku_args)


class _Mskus(object):
    msku_pr = create_msku(100900, [_Offers.blue_offer_pr])
    msku_pr1 = create_msku(10123, [_Offers.blue_offer_pr_secondary])


class _Promos(object):

    blue_cashback_pr = Promo(
        promo_type=PromoType.BLUE_CASHBACK,
        description='blue_cashback',
        url='blue_cashback_url',
        key='blue_cashback_pr',
        blue_cashback=PromoBlueCashback(share=0.2, version=10, priority=1),
    )

    blue_another_cashback_pr = Promo(
        promo_type=PromoType.BLUE_CASHBACK,
        description='another_blue_cashback',
        url='blue_another_cashback_url',
        key='blue_another_cashback_pr',
        blue_cashback=PromoBlueCashback(share=0.3, version=10, priority=2),
    )

    blue_flash_pr = Promo(
        promo_type=PromoType.BLUE_FLASH,
        description='blue_flash_discount',
        key='blue_flash_pr',
        url='syrin.com',
        blue_flash=PromoBlueFlash(
            items=[
                {
                    'feed_id': DEFAULT_FEED_ID,
                    'offer_id': _Offers.blue_offer_pr.offerid,
                    'price': {'value': 60, 'currency': 'RUR'},
                },
            ],
            allow_berubonus=True,
            allow_promocode=True,
        ),
    )

    secret_sale_pr = Promo(
        promo_type=PromoType.SECRET_SALE,
        key='secret_sale_pr',
        source_promo_id='secret_sale_pr',
        secret_sale_details=PromoSaleDetails(msku_list=[int(_Mskus.msku_pr.sku)], discount_percent_list=[10]),
    )

    cheapest_as_gift_pr = Promo(
        promo_type=PromoType.CHEAPEST_AS_GIFT,
        feed_id=DEFAULT_FEED_ID,
        key='cheapest_as_gift_pr',
        url='witcher.pl',
        cheapest_as_gift=PromoCheapestAsGift(
            offer_ids=[
                (_Shops.shop_pr.datafeed_id, _Offers.blue_offer_pr.offerid),
            ],
            count=3,
            promo_url='url',
            link_text='some text',
            allow_berubonus=True,
            allow_promocode=True,
        ),
    )

    promo_direct_discount_pr = Promo(
        promo_type=PromoType.DIRECT_DISCOUNT,
        feed_id=888,
        key='direct_discount_pr',
        url='direct_discount_pr.ru',
        direct_discount=PromoDirectDiscount(
            items=[
                {
                    'feed_id': DEFAULT_FEED_ID,
                    'offer_id': _Offers.blue_offer_pr.offerid,
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

    promo_generic_bundle_pr = Promo(
        promo_type=PromoType.GENERIC_BUNDLE,
        feed_id=DEFAULT_FEED_ID,
        key="promo_generic_bundle_pr",
        url='promo_generic_bundle_pr.com',
        generic_bundles_content=[
            make_generic_bundle_content(_Offers.blue_offer_pr.offerid, _Offers.blue_offer_pr_secondary.offerid, 10),
        ],
    )

    promo_blue_set_pr = Promo(
        promo_type=PromoType.BLUE_SET,
        feed_id=DEFAULT_FEED_ID,
        key='blue_set_pr',
        url='blue_set_pr.ru',
        blue_set=PromoBlueSet(
            sets_content=[
                {
                    'items': [
                        {'offer_id': _Offers.blue_offer_pr.offerid},
                        {'offer_id': _Offers.blue_offer_pr_secondary.offerid},
                    ],
                }
            ],
        ),
    )

    promo_blue_promocode_pr = Promo(
        promo_type=PromoType.PROMO_CODE,
        promo_code='promocode_1_text',
        discount_value=25,
        discount_currency='RUR',
        key='promocode_pr',
        url='promocode_pr.ru',
        mechanics_payment_type=MechanicsPaymentType.CPA,
    )


# Список промок отсортированный по уменьшению приоритета, кроме кэшбека, который совместим со всеми в этом списке
priority_list = [
    _Promos.secret_sale_pr,
    _Promos.blue_flash_pr,
    _Promos.cheapest_as_gift_pr,
    _Promos.promo_blue_promocode_pr,
    _Promos.promo_generic_bundle_pr,
    _Promos.promo_direct_discount_pr,
    _Promos.promo_blue_set_pr,
]

multi_priority_list = [
    _Promos.secret_sale_pr,
    _Promos.blue_flash_pr,
    _Promos.promo_direct_discount_pr,
    _Promos.promo_blue_promocode_pr,
    _Promos.cheapest_as_gift_pr,
    _Promos.promo_generic_bundle_pr,
    _Promos.promo_blue_set_pr,
]


promo_compability = {
    PromoType.PROMO_CODE: set(
        [
            PromoType.BLUE_CASHBACK,
            PromoType.CART_DISCOUNT,
            PromoType.BLUE_SET,
            PromoType.DIRECT_DISCOUNT,
            PromoType.GENERIC_BUNDLE,
            PromoType.CART_DISCOUNT,
            PromoType.CHEAPEST_AS_GIFT,
        ]
    ),
    PromoType.DIRECT_DISCOUNT: set(
        [
            PromoType.BLUE_CASHBACK,
            PromoType.BLUE_SET,
            PromoType.CHEAPEST_AS_GIFT,
            PromoType.PROMO_CODE,
            PromoType.GENERIC_BUNDLE,
            PromoType.CART_DISCOUNT,
        ]
    ),
    PromoType.CART_DISCOUNT: set(
        [PromoType.BLUE_CASHBACK, PromoType.BLUE_SET, PromoType.PROMO_CODE, PromoType.DIRECT_DISCOUNT]
    ),
    PromoType.BLUE_SET: set(
        [PromoType.BLUE_CASHBACK, PromoType.DIRECT_DISCOUNT, PromoType.CART_DISCOUNT, PromoType.PROMO_CODE]
    ),
    PromoType.GENERIC_BUNDLE: set([PromoType.BLUE_CASHBACK, PromoType.DIRECT_DISCOUNT, PromoType.PROMO_CODE]),
    PromoType.CHEAPEST_AS_GIFT: set([PromoType.BLUE_CASHBACK, PromoType.DIRECT_DISCOUNT, PromoType.PROMO_CODE]),
    PromoType.SECRET_SALE: set([PromoType.BLUE_CASHBACK]),
    PromoType.BLUE_FLASH: set([PromoType.BLUE_CASHBACK]),
    PromoType.BLUE_CASHBACK: set(
        [
            PromoType.PROMO_CODE,
            PromoType.DIRECT_DISCOUNT,
            PromoType.CART_DISCOUNT,
            PromoType.BLUE_SET,
            PromoType.GENERIC_BUNDLE,
            PromoType.CHEAPEST_AS_GIFT,
            PromoType.SECRET_SALE,
            PromoType.BLUE_FLASH,
        ]
    ),
}


def promos_combination(available_promos, promo_quantity_limit=None):
    if promo_quantity_limit is None:
        promo_quantity_limit = DEFAULT_PROMO_QUANTITY_LIMIT
    comb = []
    for p in available_promos:
        if promo_quantity_limit >= 0 and len(comb) >= promo_quantity_limit:
            break
        if len(comb) == 0 or all(p.type_name in promo_compability[cp.type_name] for cp in comb):
            comb.append(p)
    return comb


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=0;enable_fast_promo_matcher_test=0']

        _Offers.blue_offer_pr.promo = priority_list + [_Promos.blue_cashback_pr, _Promos.blue_another_cashback_pr]

        cls.index.shops += [
            _Shops.shop_pr,
        ]

        cls.index.mskus += [
            _Mskus.msku_pr,
            _Mskus.msku_pr1,
        ]

        cls.settings.loyalty_enabled = True
        cls.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(
                whitelist=[
                    _Promos.blue_flash_pr.key,
                    _Promos.cheapest_as_gift_pr.key,
                    _Promos.promo_generic_bundle_pr.key,
                    _Promos.promo_blue_set_pr.key,
                    _Promos.promo_direct_discount_pr.key,
                    _Promos.secret_sale_pr.key,
                ]
            )
        ]

    def build_request(self, place, msku, rearr_flags):
        request = "place={place}&rgb=blue&numdoc=100&regset=0&pp=18&yandexuid=1".format(place=place)
        request += '&perks=yandex_cashback'  # для кэшбека
        request += '&&allowed-promos={}'.format(_Promos.secret_sale_pr.key)  # для закрытых распродаж
        request += '&rearr-factors={}'.format(";".join("{}={}".format(key, val) for key, val in rearr_flags.items()))
        request += "&market-sku={msku}".format(msku=msku.sku)
        return request

    def blue_offer_checker(self, msku, offer, place, expected_promos, rearr_flag):
        req = self.build_request(place, msku, rearr_flag)
        response = self.report.request_json(req)
        res_promos = (
            list(map(lambda p: {'key': p.key, 'type': p.type_name}, expected_promos)) if expected_promos else Absent()
        )

        self.assertFragmentIn(
            response,
            {
                'offers': {
                    'items': [
                        {
                            'entity': 'offer',
                            'wareId': offer.waremd5,
                            'promos': res_promos,
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

    def check_promos(self, msku, offer, expected_promos, promo_quantity_limit=None):
        flags = {
            "market_enable_multipromo": 1,
        }
        if promo_quantity_limit is not None:
            flags["market_promo_quantity_limit"] = promo_quantity_limit

        for place in ('prime', 'sku_offers'):
            self.blue_offer_checker(msku, offer, place, expected_promos, flags)

    def test_multipromo_priority(self):
        """
        blue_offer_pr - оффер со всеми доступными акциями
        Будем проверять доступные акции и отключать их по одной начиная с самой приоритетной на данный момент,
        тогда следующей активной должна стать акции с приоритетом на 1 ниже
        В поле promo будет лежать акция из оставшихся с наибольшим приоритетом
        А в поле promos должны лежать все акции совместимые с топ приоритетной акцией
        """

        # Текущие приоритеты:
        # cashback
        # secret_sale
        # blue_flash
        # direct_discount
        # promocode
        # cheapest_as_gift
        # generic_bundle
        # blue_set
        # cart_discount

        self.dynamic.market_dynamic.disabled_promos = []
        available_promos = [_Promos.blue_cashback_pr] + multi_priority_list[:]
        while len(available_promos) > 0:
            promos = promos_combination(available_promos)
            self.check_promos(_Mskus.msku_pr, _Offers.blue_offer_pr, promos)
            self.dynamic.market_dynamic.disabled_promos.append(DynamicPromo(promo_key=available_promos[0].key))
            # Так как кэшбек имеет высший приоритет, то, после удаления основной акции, на ее месте должен быть другой кэшбек
            if available_promos[0].key == _Promos.blue_cashback_pr.key:
                available_promos[0] = _Promos.blue_another_cashback_pr
            else:
                available_promos = available_promos[1:]

        # Если все акции выключены, то в полях promo и promos будет пусто
        self.check_promos(_Mskus.msku_pr, _Offers.blue_offer_pr, [])

    def test_multipromo_combinations(self):
        """
        Проверяем все возможные пересечения акций с включенной мультиакционностью
        """
        self.dynamic.market_dynamic.disabled_promos = []
        all_promos = [_Promos.blue_cashback_pr] + multi_priority_list[:]
        for i in range(pow(2, len(all_promos))):
            available_promos = []
            disabled_promos = [_Promos.blue_another_cashback_pr]
            for num in range(len(all_promos)):
                if bool(i & (1 << num)):
                    available_promos.append(all_promos[num])
                else:
                    disabled_promos.append(all_promos[num])
            expected_promos = promos_combination(available_promos)
            self.dynamic.market_dynamic.disabled_promos = [DynamicPromo(promo_key=p.key) for p in disabled_promos]
            self.check_promos(_Mskus.msku_pr, _Offers.blue_offer_pr, expected_promos)

    def test_multipromo_quantity_limit(self):
        """
        Проверяем, что количество выдачаемых "promos" ограничено флагом эксперимента market_promo_quantity_limit
        Если флаг не задан, то репорт должен отдавать до 3 акций по умолчанию
        """

        available_promos = [
            _Promos.blue_cashback_pr,
            _Promos.promo_direct_discount_pr,
            _Promos.promo_blue_promocode_pr,
            _Promos.cheapest_as_gift_pr,
        ]
        self.dynamic.market_dynamic.disabled_promos = [
            DynamicPromo(promo_key=p.key) for p in multi_priority_list if p not in available_promos
        ]
        for promo_quantity_limit in [None, 0, 1, 2, 3, 4, 5, 999]:
            promos = promos_combination(available_promos, promo_quantity_limit)
            self.check_promos(_Mskus.msku_pr, _Offers.blue_offer_pr, promos, promo_quantity_limit)


if __name__ == '__main__':
    main()
