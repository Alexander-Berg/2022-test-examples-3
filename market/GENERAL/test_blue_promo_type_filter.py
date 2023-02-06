#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.matcher import Absent
from core.types import (
    HyperCategory,
    HyperCategoryType,
)
from core.types.sku import MarketSku, BlueOffer
from core.types.autogen import b64url_md5
from core.types.offer_promo import (
    PromoBlueCashback,
    PromoCheapestAsGift,
    PromoDirectDiscount,
    PromoSpreadDiscountCount,
    PromoSpreadDiscountReceipt,
    PromoBlueSet,
    PromoBlueFlash,
    make_generic_bundle_content,
)
from core.types import (
    DynamicBlueGenericBundlesPromos,
    Promo,
    PromoType,
)

from collections import Counter, defaultdict
from itertools import count

FEED_ID = 777
nummer = count()


def make_blue_offer(hid):
    return BlueOffer(
        feedid=FEED_ID,
        offerid=str(next(nummer)),
        hid=hid,
        price=2000,
    )


def make_msku(hid, offer):
    return MarketSku(
        sku=next(nummer),
        hid=hid,
        blue_offers=[offer],
    )


def make_generic_bundle(offer, gift):
    promo = Promo(
        key=b64url_md5(next(nummer)),
        promo_type=PromoType.GENERIC_BUNDLE,
        feed_id=offer.feedid,
        generic_bundles_content=[
            make_generic_bundle_content(offer.offerid, gift.offerid, 1),
        ],
    )
    offer.promo = [promo]
    return promo


def make_cheapest_as_gift(offer):
    promo = Promo(
        key=b64url_md5(next(nummer)),
        promo_type=PromoType.CHEAPEST_AS_GIFT,
        cheapest_as_gift=PromoCheapestAsGift(
            offer_ids=[
                (offer.feedid, offer.offerid),
            ],
            count=3,
            promo_url='url',
            link_text='text',
        ),
    )
    offer.promo = [promo]
    return promo


def make_blue_cashback(offer):
    promo = Promo(
        key=b64url_md5(next(nummer)),
        promo_type=PromoType.BLUE_CASHBACK,
        blue_cashback=PromoBlueCashback(
            share=0.01,
            version=10,
            priority=1,
        ),
    )
    offer.promo = [promo]
    return promo


def make_direct_discount(offer):
    promo = Promo(
        key=b64url_md5(next(nummer)),
        promo_type=PromoType.DIRECT_DISCOUNT,
        direct_discount=PromoDirectDiscount(
            items=[
                {
                    'feed_id': offer.feedid,
                    'offer_id': offer.offerid,
                    'discount_price': {'value': int(offer.price * 0.9), 'currency': 'RUR'},
                }
            ],
        ),
    )
    offer.promo = [promo]
    return promo


def make_blue_set(offer1, offer2):
    promo = Promo(
        promo_type=PromoType.BLUE_SET,
        key=b64url_md5(next(nummer)),
        blue_set=PromoBlueSet(
            sets_content=[
                {
                    'items': [
                        {'offer_id': offer1.offerid, 'discount': 15},
                        {'offer_id': offer2.offerid},
                    ],
                    'linked': False,
                }
            ],
        ),
    )
    offer1.promo = [promo]
    offer2.promo = [promo]
    return promo


def make_blue_flash(offer):
    promo = Promo(
        key=b64url_md5(next(nummer)),
        promo_type=PromoType.BLUE_FLASH,
        blue_flash=PromoBlueFlash(
            items=[
                {
                    'feed_id': offer.feedid,
                    'offer_id': offer.offerid,
                    'price': {'value': int(offer.price * 0.9), 'currency': 'RUR'},
                }
            ],
        ),
    )
    offer.promo = [promo]
    return promo


def make_promo_code(offer):
    promo = Promo(
        key=b64url_md5(next(nummer)),
        promo_type=PromoType.PROMO_CODE,
        discount_value=10,
    )
    offer.promo = [promo]
    return promo


def make_spread_discount_receipt(offer):
    promo = Promo(
        key=b64url_md5(next(nummer)),
        promo_type=PromoType.SPREAD_DISCOUNT_RECEIPT,
        spread_discount_receipt=PromoSpreadDiscountReceipt(
            receipt_bounds=[
                {'discount_price': {'value': 1000, 'currency': 'RUR'}, 'percent_discount': 10},
                {
                    'discount_price': {'value': 5000, 'currency': 'RUR'},
                    'absolute_discount': {'value': 500, 'currency': 'RUR'},
                },
            ]
        ),
    )
    offer.promo = [promo]
    return promo


def make_spread_discount_count(offer, msku):
    promo = Promo(
        key=b64url_md5(next(nummer)),
        promo_type=PromoType.SPREAD_DISCOUNT_COUNT,
        spread_discount_count=PromoSpreadDiscountCount(
            items={
                msku.sku: [{'count': 3, 'percent_discount': 7}, {'count': 5, 'percent_discount': 13}],
            }
        ),
    )
    offer.promo = [promo]
    return promo


def response_offer(offerid):
    return {
        'entity': 'offer',
        'shop': {'feed': {'offerId': offerid}},
    }


def response_filter(promo_types):
    return {
        'filters': [
            {
                'id': 'promo-type-filter',
                'values': [
                    {
                        'id': promo_type,
                        'found': count,
                        'value': FILTER_VALUES[promo_type],
                    }
                    for promo_type, count in promo_types.items()
                ],
            },
        ]
    }


make_promo = {
    PromoType.GENERIC_BUNDLE: make_generic_bundle,
    PromoType.BLUE_CASHBACK: make_blue_cashback,
    PromoType.CHEAPEST_AS_GIFT: make_cheapest_as_gift,
    PromoType.DIRECT_DISCOUNT: make_direct_discount,
    PromoType.BLUE_SET: make_blue_set,
    PromoType.BLUE_FLASH: make_blue_flash,
    PromoType.PROMO_CODE: make_promo_code,
    PromoType.SPREAD_DISCOUNT_COUNT: make_spread_discount_count,
    PromoType.SPREAD_DISCOUNT_RECEIPT: make_spread_discount_receipt,
}

CASES = {
    10: [
        PromoType.GENERIC_BUNDLE,
        PromoType.BLUE_CASHBACK,
        PromoType.CHEAPEST_AS_GIFT,
        PromoType.DIRECT_DISCOUNT,
        PromoType.BLUE_SET,
        PromoType.BLUE_FLASH,
        PromoType.PROMO_CODE,
        PromoType.SPREAD_DISCOUNT_COUNT,
        # PromoType.SPREAD_DISCOUNT_RECEIPT,        Оставлено на следующий этап
    ],
    11: [
        PromoType.GENERIC_BUNDLE,
        PromoType.GENERIC_BUNDLE,
        PromoType.BLUE_SET,
        PromoType.BLUE_SET,
    ],
}

FILTER_VALUES = {
    PromoType.GENERIC_BUNDLE: "подарки",
    PromoType.BLUE_CASHBACK: "кешбэк баллами Плюса",
    PromoType.CHEAPEST_AS_GIFT: "акция 3=2",
    PromoType.DISCOUNT: "скидки",
    PromoType.BLUE_SET: "есть комплект",
    PromoType.PROMO_CODE: "промокоды",
    PromoType.SPREAD_DISCOUNT_COUNT: "больше - дешевле",
    # PromoType.SPREAD_DISCOUNT_RECEIPT: "больше - дешевле",
}

GROUP_FILTERS = {
    PromoType.DIRECT_DISCOUNT: 'discount',
    PromoType.BLUE_FLASH: 'discount',
}

OFFERS = defaultdict(list)


def expected_promo_types(promo_types):
    return list(map(lambda pt: GROUP_FILTERS.get(pt, pt), promo_types))


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_enable_common_promo_type_filter=1']
        cls.index.hypertree += [HyperCategory(hid=42, uniq_name='Тракторы')]
        cls.settings.loyalty_enabled = True

        for hid, v in CASES.items():
            for promo_type in v:
                offers = [make_blue_offer(hid)]
                if promo_type in [PromoType.GENERIC_BUNDLE, PromoType.BLUE_SET]:
                    offers += [make_blue_offer(hid)]
                mskus = [make_msku(hid, offer) for offer in offers]

                if promo_type in [PromoType.SPREAD_DISCOUNT_COUNT]:
                    promo = make_promo[promo_type](offers[0], mskus[0])
                else:
                    promo = make_promo[promo_type](*offers)

                OFFERS[hid] += [(promo_type, offers[0])]
                if promo_type in [PromoType.BLUE_SET]:
                    OFFERS[hid] += [(promo_type, offers[1])]

                cls.index.mskus += mskus
                cls.index.promos += [promo]
                cls.dynamic.loyalty += [DynamicBlueGenericBundlesPromos(whitelist=[promo.key])]
            cls.index.hypertree += [
                HyperCategory(hid=hid, output_type=HyperCategoryType.GURU),
            ]

    def test_output_filter(self):
        """
        Проверяем корректность вывода блока filters
        """
        for hid, promo_types in CASES.items():
            promo_types_counter = Counter(expected_promo_types(promo_types))
            # Так как в комплектах по 2 оффера, то они оба увеличивают счетчик
            if PromoType.BLUE_SET in promo_types_counter:
                promo_types_counter[PromoType.BLUE_SET] *= 2
            response = self.report.request_json('place=prime&hid={hid}&debug=1&perks=yandex_cashback'.format(hid=hid))

            self.assertFragmentIn(
                response,
                response_filter(promo_types_counter),
                allow_different_len=True,
                preserve_order=False,
            )

            # Количество офферов в счетчиках фильтров должно оставаться неизменным при зажатии фильтра
            for promo_type in promo_types_counter.keys():
                response = self.report.request_json(
                    'place=prime&hid={hid}&debug=1&perks=yandex_cashback&promo-type-filter={promo_type}'.format(
                        hid=hid, promo_type=promo_type
                    )
                )
                self.assertFragmentIn(
                    response,
                    response_filter(promo_types_counter),
                    allow_different_len=True,
                    preserve_order=False,
                )

    def check_offers(self, response, expected, not_expected):
        for offer in expected:
            self.assertFragmentIn(response, response_offer('{}.{}'.format(offer.feedid, offer.offerid)))

        for offer in not_expected:
            self.assertFragmentNotIn(response, response_offer('{}.{}'.format(offer.feedid, offer.offerid)))

    def test_filter(self):
        """
        Проверяем работу фильтрации офферов
        """
        for url in (
            'place=prime&hid=10&numdoc=20&perks=yandex_cashback',
            'place=prime&hid=10&numdoc=20&perks=yandex_cashback&promo-type-filter=',
        ):
            response = self.report.request_json(url)
            self.check_offers(
                response,
                [o[1] for o in OFFERS[10]],
                [],
            )

        for promo_type in set(expected_promo_types(CASES[10])):
            for url in (
                'place=prime&hid=10&perks=yandex_cashback&promo-type-filter={promo_type}',
                'place=prime&hid=10&perks=yandex_cashback&filter=promo-type-filter:{promo_type}',
            ):

                response = self.report.request_json(url.format(promo_type=promo_type))
                self.check_offers(
                    response,
                    [o[1] for o in OFFERS[10] if GROUP_FILTERS.get(o[0], o[0]) == promo_type],
                    [o[1] for o in OFFERS[10] if GROUP_FILTERS.get(o[0], o[0]) != promo_type],
                )

    def test_filter_checked(self):
        """
        Проверяем, что у выбранных фильтров выставлен флажок
        """
        response = self.report.request_json('place=prime&hid=10')
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {
                        'id': 'promo-type-filter',
                        'values': [
                            {
                                'id': PromoType.GENERIC_BUNDLE,
                                'checked': Absent(),
                            }
                        ],
                    }
                ]
            },
        )
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {
                        'id': 'promo-type-filter',
                        'values': [
                            {
                                'id': PromoType.DISCOUNT,
                                'checked': Absent(),
                            }
                        ],
                    }
                ]
            },
        )

        response = self.report.request_json('place=prime&hid=10&promo-type-filter=generic-bundle,discount')
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {
                        'id': 'promo-type-filter',
                        'values': [
                            {
                                'id': PromoType.GENERIC_BUNDLE,
                                'checked': True,
                            }
                        ],
                    }
                ]
            },
        )
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {
                        'id': 'promo-type-filter',
                        'values': [
                            {
                                'id': PromoType.DISCOUNT,
                                'checked': True,
                            }
                        ],
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()
