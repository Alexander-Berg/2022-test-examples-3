#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

"""
Проверяет работу фильтра filter-delivery-perks-eligible
"""

from core.types import Currency, HyperCategory, Model, Offer, Shop
from core.testcase import TestCase, main

from core.types.delivery import BlueDeliveryTariff
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax


HUGE_PRICE = 1000000
YANDEX_PLUS_FDT = 55
PRIME_FDT = YANDEX_PLUS_FDT + 5

REQUEST_PREFIX = 'base=default.market-exp-prestable.yandex.ru&'

# магазины для синего и белого фидов
BLUE_VIRTUAL_SHOP = Shop(
    fesh=3,
    datafeed_id=3,
    priority_region=2,
    name='blue_shop_1',
    currency=Currency.RUR,
    tax_system=Tax.OSN,
    fulfillment_virtual=True,
    virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
)

BLUE_SUPPLIER_SHOP = Shop(
    fesh=100,
    datafeed_id=100,
    priority_region=213,
    name='supplier_blue_shop_2',
    currency=Currency.RUR,
    tax_system=Tax.OSN,
    supplier_type=Shop.FIRST_PARTY,
    blue=Shop.BLUE_REAL,
)

GREEN_SHOP = Shop(
    fesh=1,
    datafeed_id=1,
    priority_region=2,
    name='green_shop_1',
    currency=Currency.RUR,
)

# SKU1 будет использоваться для проверки ситуации,
# когда 1 оффер MSKU проходит фильтр, а 1 - нет
SKU1_OFFER1 = BlueOffer(
    price=PRIME_FDT,
    price_old=8,
    vat=Vat.VAT_10,
    feedid=100,
    offerid='blue.offer.1.1',
    waremd5='Sku1Price5-IiLVm1Goleg',
)
SKU1_OFFER2 = BlueOffer(
    price=PRIME_FDT,
    vat=Vat.VAT_10,
    feedid=100,
    offerid='blue.offer.1.2',
    waremd5='Sku1Price50-iLVm1Goleg',
)

# SKU2 будет использоваться для проверки ситуации,
# когда единственный оффер MSKU проходит или не проходит фильтр
SKU2_OFFER1 = BlueOffer(
    price=YANDEX_PLUS_FDT + 1,
    vat=Vat.VAT_10,
    feedid=100,
    offerid='blue.offer.2.1',
    waremd5='Sku2Price55-iLVm1Goleg',
)

# SKU3 будет попадать под оба перка
SKU3_OFFER1 = BlueOffer(
    price=PRIME_FDT,
    vat=Vat.VAT_10,
    feedid=100,
    offerid='blue.offer.3.1',
    waremd5='Sku3Price55-iLVm1Goleg',
)

# SKU4 не будет попадать ни под что
SKU4_OFFER1 = BlueOffer(
    price=min(PRIME_FDT, YANDEX_PLUS_FDT) - 1,
    vat=Vat.VAT_10,
    feedid=100,
    offerid='blue.offer.4.1',
    waremd5='Sku4Price55-iLVm1Goleg',
)

# зеленый оффер попадает под условия прайма но не должен приводить к включениюя фильтра
GREEN_OFFER = Offer(
    title='green',
    fesh=1,
    price=max(PRIME_FDT, YANDEX_PLUS_FDT) + 1,
    hyperid=1,
    waremd5='GreenOffer1_1_gggggggg',
    randx=100,
)

SKU1 = MarketSku(
    title='blue offer sku1 lennon',
    hyperid=1,
    sku=1,
    waremd5='Sku1-wdDXWsIiLVm1goleg',
    blue_offers=[SKU1_OFFER1, SKU1_OFFER2],
    randx=1,
)

SKU2 = MarketSku(
    title='blue offer sku2 mccartney',
    hyperid=2,
    sku=2,
    waremd5='Sku2-wdDXWsIiLVm1goleg',
    blue_offers=[SKU2_OFFER1],
    randx=1,
)

SKU3 = MarketSku(
    title='blue offer sku3 harrison',
    hyperid=3,
    sku=3,
    waremd5='Sku3-wdDXWsIiLVm1goleg',
    blue_offers=[SKU3_OFFER1],
    randx=1,
)

SKU4 = MarketSku(
    title='blue offer sku4 ringo',
    hyperid=4,
    sku=4,
    waremd5='Sku4-wdDXWsIiLVm1goleg',
    blue_offers=[SKU4_OFFER1],
    randx=1,
)

MODEL1 = Model(
    hyperid=1,
    hid=1,
    title='blue and green model one',
)

MODEL2 = Model(
    hyperid=2,
    hid=1,
    title='blue and green model two',
)

MODEL3 = Model(
    hyperid=3,
    hid=1,
    title='blue and green model three',
)

MODEL4 = Model(
    hyperid=4,
    hid=1,
    title='blue and green model four',
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.blue_market_free_delivery_threshold = HUGE_PRICE
        cls.settings.blue_market_prime_free_delivery_threshold = PRIME_FDT
        cls.settings.blue_market_yandex_plus_free_delivery_threshold = YANDEX_PLUS_FDT
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=[BlueDeliveryTariff(user_price=99)], ya_plus_threshold=YANDEX_PLUS_FDT
        )

        cls.index.hypertree += [
            HyperCategory(hid=1, fee=123),
        ]

        cls.index.shops += [
            BLUE_VIRTUAL_SHOP,
            BLUE_SUPPLIER_SHOP,
            GREEN_SHOP,
        ]

        cls.index.models += [
            MODEL1,
            MODEL2,
            MODEL3,
            MODEL4,
        ]

        cls.index.mskus += [
            SKU1,
            SKU2,
            SKU3,
            SKU4,
        ]

        cls.index.offers += [
            GREEN_OFFER,
        ]

    def test_visibility_white(self):
        """
        Что проверяем:
        * на белом маркете не показывается фильтр
        * это не зависит от переданных перков
        """
        for perks in [
            '&perks=prime',
            '&perks=yandex_plus',
            '&perks=beru_plus',
            '&perks=prime,yandex_plus',
            '&perks=yandex_plus,beru_plus',
            '&perks=prime,yandex_plus,beru_plus',
            '',
        ]:
            for rgb_type in ['&rgb=green_with_blue', '']:
                response = self.report.request_json(REQUEST_PREFIX + 'place=prime&text=green' + rgb_type + perks)
                self.assertFragmentNotIn(
                    response,
                    {
                        "filters": [
                            {
                                "id": "filter-delivery-perks-eligible",
                            },
                        ],
                    },
                )

    def test_visibility_blue_has_results(self):
        """
        Что проверяем:
        * на синем маркете фильтр показывается, если есть подходящие результаты
          * но только если переданы правильные перки
        """
        for perks in [
            '&perks=prime',
            '&perks=yandex_plus',
            '&perks=prime,yandex_plus',
            '',
        ]:
            response = self.report.request_json(
                # lennon = sku1
                REQUEST_PREFIX
                + 'place=prime&text=lennon&rgb=blue'
                + perks
            )
            assertion = self.assertFragmentIn if perks else self.assertFragmentNotIn
            assertion(
                response,
                {
                    "filters": [
                        {
                            "id": "filter-delivery-perks-eligible",
                        },
                    ],
                },
            )

    def test_visibility_blue_no_results(self):
        """
        Что проверяем:
        * на синем маркете фильтр не показывается, если нет подходящих результатов
          * перки (кроме beru_plus) это не аффектят
        """
        for perks in [
            '&perks=prime',
            '&perks=yandex_plus',
            '&perks=beru_plus',
            '&perks=prime,yandex_plus',
            '&perks=yandex_plus,beru_plus',
            '&perks=prime,yandex_plus,beru_plus',
            '',
        ]:
            response = self.report.request_json(
                # ringo = sku4
                REQUEST_PREFIX
                + 'place=prime&text=ringo&rgb=blue'
                + perks
            )
            if 'beru_plus' in perks:
                self.assertFragmentIn(
                    response,
                    {
                        "filters": [
                            {
                                "id": "filter-delivery-perks-eligible",
                            },
                        ],
                    },
                )
            else:
                self.assertFragmentNotIn(
                    response,
                    {
                        "filters": [
                            {
                                "id": "filter-delivery-perks-eligible",
                            },
                        ],
                    },
                )

    def test_filter_action(self):
        """
        Что проверяет:
        * на синем маркете фильтр работает: на выдачу попадают только MSKU с подходящими под условия офферами
        """
        sku1 = SKU1.title
        sku2 = SKU2.title
        sku3 = SKU3.title
        sku4 = SKU4.title

        for perks, present_skus, absent_skus in [
            ('&perks=prime', [sku1, sku3], [sku2, sku4]),
            ('&perks=yandex_plus', [sku1, sku2, sku3], [sku4]),
            ('&perks=prime,yandex_plus', [sku1, sku2, sku3], [sku4]),
            ('&perks=beru_plus', [sku1, sku2, sku3, sku4], []),
            ('&perks=beru_plus,yandex_plus', [sku1, sku2, sku3, sku4], []),
            ('&perks=prime,beru_plus,yandex_plus', [sku1, sku2, sku3, sku4], []),
        ]:
            response = self.report.request_json('place=prime&hid=1&rgb=blue&filter-delivery-perks-eligible=1' + perks)

            for sku in present_skus:
                self.assertFragmentIn(response, sku)

            for sku in absent_skus:
                self.assertFragmentNotIn(response, sku)


if __name__ == '__main__':
    main()
