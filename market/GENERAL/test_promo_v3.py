#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    Currency,
    DynamicShop,
    ExchangeRate,
    HyperCategory,
    Offer,
    Promo,
    PromoPurchase,
    PromoType,
    RtyOffer,
)
from core.matcher import Absent, NotEmpty

from datetime import datetime


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.settings.rty_qpipe = True

        cls.index.hypertree += [HyperCategory(hid=42, uniq_name='Тракторы')]

        cls.index.currencies += [
            Currency(name=Currency.UAH, exchange_rates=[ExchangeRate(fr=Currency.RUR, rate=0.0333333)])
        ]

        cls.index.offers += [
            Offer(
                title='offer with gift',
                price=5,
                promo=Promo(
                    promo_type=PromoType.GIFT_WITH_PURCHASE,
                    start_date=datetime(1980, 1, 1),
                    end_date=datetime(2050, 1, 1),
                    key="xMpQQQC5I4INzCFab3ZZZZ",
                    url='http://my.url',
                    required_quantity=2,
                    feed_id=42,
                    gift_offers=[1, 2],
                    gift_gifts=[3, 4],
                ),
            ),
            Offer(
                title='promocode 1 offer',
                fesh=11,
                hid=42,
                price=500,
                promo=Promo(
                    promo_type=PromoType.PROMO_CODE,
                    start_date=datetime(1980, 1, 1),
                    end_date=datetime(2050, 1, 1),
                    key='xMpCOKC5I4INzFCab3WEmw',
                    url='http://my.url',
                    promo_code="my promo code",
                    discount_value=300,
                    discount_currency='UAH',
                    required_quantity=2,
                    purchases=[
                        PromoPurchase(category_id=42),
                    ],
                ),
            ),
            Offer(
                title='promocode 2 offer',
                fesh=11,
                hid=7,
                offerid=77,
                hyperid=301,
                price=500,
                promo=Promo(
                    promo_type=PromoType.PROMO_CODE,
                    start_date=datetime(1980, 1, 1),
                    end_date=datetime(2050, 1, 1),
                    key='xMpQQQC5I4INzFCab3WEmw',
                    url='http://my.url',
                    promo_code="my promo code",
                    discount_value=300,
                    discount_currency='RUR',
                    purchases=[
                        PromoPurchase(offer_id=77),
                    ],
                ),
            ),
            Offer(
                title='promocode 3 offer',
                fesh=11,
                hid=7,
                price=300,
                promo=Promo(
                    promo_type=PromoType.PROMO_CODE,
                    start_date=datetime(1980, 1, 1),
                    end_date=datetime(2050, 1, 1),
                    key='xMpQQQC5I4INzQQab3WEmw',
                    url='http://my.url',
                    promo_code="my promo code",
                    discount_value=30,
                ),
            ),
            Offer(
                title='no promocode 4 offer',
                fesh=11,
                hid=7,
                price=2,
                promo=Promo(
                    promo_type=PromoType.PROMO_CODE,
                    start_date=datetime(1980, 1, 1),
                    end_date=datetime(2050, 1, 1),
                    key='xMpQQQC5I4INzQQab3wemw',
                    url='http://my.url',
                    promo_code="my promo code",
                    discount_value=10,  # less than 1 rub
                ),
            ),
            Offer(
                title='no promocode 5 offer',
                fesh=11,
                hid=7,
                price=100000,
                promo=Promo(
                    promo_type=PromoType.PROMO_CODE,
                    start_date=datetime(1980, 1, 1),
                    end_date=datetime(2050, 1, 1),
                    key='xMpTTTC5I4INzQQab3wemw',
                    url='http://my.url',
                    promo_code="my promo code",
                    discount_value=300,
                    discount_currency='RUR',  # less than 5% or 500rur
                ),
            ),
            Offer(
                title='no promocode 6 offer',
                fesh=11,
                hid=7,
                price=100,
                promo=Promo(
                    promo_type=PromoType.PROMO_CODE,
                    start_date=datetime(1980, 1, 1),
                    end_date=datetime(2050, 1, 1),
                    key='xMpYYYC5I4INzQQab3wemw',
                    url='http://my.url',
                    promo_code="my promo code",
                    discount_value=4,  # less than 5% or 500rur
                ),
            ),
            Offer(
                title='flashdiscount with explicit offer promo price',
                fesh=11,
                hid=42,
                offerid=1043,
                price=150,
                price_old=200,
                price_history=200,
                promo_price=49,
                promo=Promo(
                    promo_type=PromoType.FLASH_DISCOUNT,
                    start_date=datetime(1985, 6, 20),
                    end_date=datetime(1985, 6, 26),
                    key='somePromoKey',
                    url='http://my.url',
                ),
            ),
            Offer(
                title='flashdiscount with explicit offer promo price and vector',
                fesh=11,
                hid=42,
                offerid=1044,
                price=150,
                price_old=200,
                price_history=200,
                promo_price=49,
                promo=Promo(
                    promo_type=PromoType.FLASH_DISCOUNT,
                    start_date=datetime(1985, 6, 20),
                    end_date=datetime(1985, 6, 26),
                    key='somePromoKey2',
                    url='http://my.url',
                    purchases=[  # ignored
                        PromoPurchase(offer_id=1044, discount_value=30, discount_currency="RUR"),
                    ],
                ),
            ),
            Offer(
                title='flashdiscount with no history price',
                fesh=11,
                hid=42,
                offerid=1143,
                price=150,
                promo_price=49,
                promo=Promo(
                    promo_type=PromoType.FLASH_DISCOUNT,
                    start_date=datetime(1985, 6, 20),
                    end_date=datetime(1985, 6, 26),
                    key='somePromoKey4',
                    url='http://my.url',
                ),
            ),
            Offer(
                title='flashdiscount with low history price',
                fesh=11,
                hid=42,
                offerid=1144,
                price=150,
                price_history=30,
                promo_price=49,
                promo=Promo(
                    promo_type=PromoType.FLASH_DISCOUNT,
                    start_date=datetime(1985, 6, 20),
                    end_date=datetime(1985, 6, 26),
                    key='somePromoKey3',
                    url='http://my.url',
                ),
            ),
            Offer(
                title='too long flashdiscount offer',
                fesh=11,
                hid=42,
                offerid=1045,
                price=150,
                price_old=200,
                price_history=200,
                promo=Promo(
                    promo_type=PromoType.FLASH_DISCOUNT,
                    start_date=datetime(1985, 6, 20),
                    end_date=datetime(1985, 7, 26),
                    key='aApCOKC7I7INzQQab3WEmw',
                    url='http://my.url',
                    purchases=[
                        PromoPurchase(offer_id=1045, discount_value=49, discount_currency="RUR"),
                    ],
                ),
            ),
            Offer(
                feedid=1,
                title='flashdiscount with rty changed price',
                fesh=11,
                hid=42,
                offerid=1046,
                price=150,
                price_old=200,
                price_history=200,
                promo_price=100,
                promo=[
                    Promo(
                        promo_type=PromoType.FLASH_DISCOUNT,
                        start_date=datetime(1985, 6, 20),
                        end_date=datetime(1985, 6, 26),
                        key='someOtherPromoKey',
                        url='http://my.url',
                    ),
                    Promo(
                        promo_type=PromoType.N_PLUS_ONE,
                        key='uuuuuuuuuuuuuuuuuuuuu',
                        required_quantity=3,
                        free_quantity=34,
                        purchases=[
                            PromoPurchase(offer_id=1046),
                        ],
                    ),
                ],
            ),
            Offer(
                title='nplusm 1 offer',
                fesh=11,
                hid=42,
                offerid=777,
                promo=[
                    Promo(
                        promo_type=PromoType.N_PLUS_ONE,
                        key='yMpCOKC5I4INzFCab3WEmw',
                        required_quantity=3,
                        free_quantity=34,
                        purchases=[
                            PromoPurchase(offer_id=777),
                        ],
                    ),
                    Promo(
                        promo_type=PromoType.FLASH_DISCOUNT,
                        start_date=datetime(1985, 6, 20),
                        end_date=datetime(1985, 6, 26),
                        key='aaaaaaaaaaaaaaaaaaaaaa',
                        url='http://my.url',
                    ),
                ],
            ),
            Offer(
                title='bonuscard1',
                fesh=11,
                hid=90509,
                offerid=10001,
                price=150,
                promo_price=49,
                promo=Promo(
                    promo_type=PromoType.BONUS_CARD,
                    start_date=datetime(1985, 6, 20),
                    end_date=datetime(1985, 6, 26),
                    key='somePromoKeybonus',
                    url='http://my.url',
                ),
            ),
            Offer(
                title='bonuscard2',
                fesh=11,
                hid=42,
                offerid=1000112,
                price=150,
                promo_price=49,
                promo=Promo(
                    promo_type=PromoType.BONUS_CARD,
                    start_date=datetime(1985, 6, 20),
                    end_date=datetime(1985, 6, 26),
                    key='somePromoKeybonus12',
                    url='http://my.url',
                ),
            ),
            Offer(
                title='nobonus',
                fesh=11,
                hid=90509,
                offerid=1000113,
                price=150,
                promo_price=150,
                promo=Promo(
                    promo_type=PromoType.BONUS_CARD,
                    start_date=datetime(1985, 6, 20),
                    end_date=datetime(1985, 6, 26),
                    key='somePromoKeybonus13',
                    url='http://my.url',
                ),
            ),
            Offer(title='offernopromo 1 offer', fesh=11, hid=42, price=150, price_old=200),
        ]

    def test_on_stock(self):
        response = self.report.request_json('place=prime&text=promocode&promo-type=promo-code')
        self.assertFragmentIn(
            response,
            {"id": "onstock", "type": "boolean", "values": [{"value": "0"}, {"checked": True, "value": "1"}]},
        )

    def test_promo_code(self):
        response = self.report.request_json('place=prime&text=promocode')
        # с категорией и валютой
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "promocode 1 offer"},
                    "promos": [
                        {
                            "type": "promo-code",
                            "key": "xMpCOKC5I4INzFCab3WEmw",
                            "url": "http://my.url",
                            "startDate": "1980-01-01T00:00:00Z",
                            "endDate": "2050-01-01T00:00:00Z",
                            "promoCode": "my promo code",
                            "discount": {"value": 9000, "currency": "RUR"},
                            "onlyPromoCategory": {"hid": 42, "name": "Тракторы"},
                            "parameters": {"requiredQuantity": 2},
                        }
                    ],
                }
            ],
        )

        # с валютой, c оффером
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "promocode 2 offer"},
                    "promos": [
                        {
                            "type": "promo-code",
                            "key": "xMpQQQC5I4INzFCab3WEmw",
                            "url": "http://my.url",
                            "startDate": "1980-01-01T00:00:00Z",
                            "endDate": "2050-01-01T00:00:00Z",
                            "promoCode": "my promo code",
                            "discount": {"value": 300, "currency": "RUR"},
                            "onlyPromoOffer": True,
                        }
                    ],
                }
            ],
        )

        # без категории, без оффера, без валюты
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "promocode 3 offer"},
                    "promos": [
                        {
                            "type": "promo-code",
                            "key": "xMpQQQC5I4INzQQab3WEmw",
                            "url": "http://my.url",
                            "startDate": "1980-01-01T00:00:00Z",
                            "endDate": "2050-01-01T00:00:00Z",
                            "promoCode": "my promo code",
                            "discount": {"value": 30},
                        }
                    ],
                }
            ],
        )

        # промокод меньше рубля снимается
        self.assertFragmentIn(
            response, [{"entity": "offer", "titles": {"raw": "no promocode 4 offer"}, "promos": Absent()}]
        )

        self.assertFragmentIn(
            response, [{"entity": "offer", "titles": {"raw": "no promocode 5 offer"}, "promos": Absent()}]
        )

        self.assertFragmentIn(
            response, [{"entity": "offer", "titles": {"raw": "no promocode 6 offer"}, "promos": Absent()}]
        )

    def test_bonus_card(self):
        response = self.report.request_json('place=prime&text=bonuscard')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "bonuscard1"},
                    "prices": {
                        "value": "150",
                    },
                    "promos": [{"type": "bonus-card", "bonusPrice": 49}],
                }
            ],
        )

        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "bonuscard2"},
                    "prices": {
                        "value": "150",
                    },
                    "promos": [{"type": "bonus-card", "bonusPrice": 49}],
                }
            ],
        )

        response = self.report.request_json('place=prime&text=bonuscard&rearr-factors=market_bonus_card_ban=1')

        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "bonuscard1"},
                    "prices": {
                        "value": "150",
                    },
                    "promos": [{"type": "bonus-card", "bonusPrice": 49}],
                }
            ],
        )

        self.assertFragmentNotIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "bonuscard2"},
                    "prices": {
                        "value": "150",
                    },
                    "promos": [{"type": "bonus-card", "bonusPrice": 49}],
                }
            ],
        )

        response = self.report.request_json('place=prime&text=nobonus')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "nobonus"},
                    "prices": {
                        "value": "150",
                    },
                    "promos": Absent(),
                }
            ],
        )

    def test_flash_discount(self):
        """
        Проверяем наличие нужных полей в выдаче, и то, что ценовые фильтры учитывают промо-цену
        """
        response = self.report.request_json('place=prime&text=flashdiscount&mcpriceto=49')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "flashdiscount with explicit offer promo price"},
                    "prices": {
                        "currency": "RUR",
                        "value": "49",
                        "discount": {
                            "oldMin": "150",
                            "percent": 67,
                        },
                    },
                    "promos": [
                        {
                            "type": "flash-discount",
                            "key": "somePromoKey",
                            "url": "http://my.url",
                            "startDate": "1985-06-20T00:00:00Z",
                            "endDate": "1985-06-26T00:00:00Z",
                        }
                    ],
                }
            ],
        )

        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "flashdiscount with explicit offer promo price and vector"},
                    "prices": {
                        "currency": "RUR",
                        "value": "49",
                        "discount": {
                            "oldMin": "150",
                            "percent": 67,
                        },
                    },
                    "promos": [
                        {
                            "type": "flash-discount",
                            "key": "somePromoKey2",
                            "url": "http://my.url",
                            "startDate": "1985-06-20T00:00:00Z",
                            "endDate": "1985-06-26T00:00:00Z",
                        }
                    ],
                }
            ],
        )

        self.assertFragmentNotIn(response, [{"entity": "offer", "titles": {"raw": "too long flashdiscount offer"}}])

        self.assertFragmentNotIn(
            response, [{"entity": "offer", "titles": {"raw": "flashdiscount with low history price"}}]
        )

        response = self.report.request_json('place=prime&text=flashdiscount&mcpriceto=48')
        self.assertFragmentNotIn(response, [{"entity": "offer"}])

    def test_flash_discount_rty(self):
        """MARKETOUT-25563 Проверяем что если цена офера изменилась через RTY то блок с flash-discount убирается с офера"""

        # without changing price through rty "promos" block is present ...
        response = self.report.request_json(
            'place=prime&text=flashdiscount with rty changed price&rearr-factors=rty_qpipe=1'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "flashdiscount with rty changed price"},
                    "promos": [
                        {
                            "type": "flash-discount",
                            "key": "someOtherPromoKey",
                        }
                    ],
                }
            ],
        )

        # ... after decreasing price till promo_price "promos" block should gone
        self.rty.offers += [
            RtyOffer(
                feedid=1,
                offerid=1046,
                price=100.0,
            )
        ]
        response = self.report.request_json(
            'place=prime&text=flashdiscount with rty changed price&rearr-factors=rty_qpipe=1'
        )
        self.assertFragmentNotIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "flashdiscount with rty changed price"},
                    "promos": [
                        {
                            "type": "flash-discount",
                            "key": "someOtherPromoKey",
                        }
                    ],
                }
            ],
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "flashdiscount with rty changed price"},
                    "promos": [
                        {
                            "type": "n-plus-m",
                            "key": "uuuuuuuuuuuuuuuuuuuuu",
                        }
                    ],
                }
            ],
        )

    def test_n_plus_m(self):
        response = self.report.request_json('place=prime&text=nplusm')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "nplusm 1 offer"},
                    "promos": [
                        {
                            "type": "n-plus-m",
                            "key": "yMpCOKC5I4INzFCab3WEmw",
                            "parameters": {"requiredQuantity": 3, "freeQuantity": 34},
                            "onlyPromoOffer": True,
                        }
                    ],
                }
            ],
        )

    def test_filters(self):
        response = self.report.request_json('place=prime&text=offer&rearr-factors=market_do_not_split_promo_filter=1')
        self.assertFragmentIn(response, {"filters": [{"id": "filter-promo-or-discount", "name": "Скидки и акции"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "filter-promo"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "filter-discount-only"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "promo-type-filter"}]})

        response = self.report.request_json('place=prime&text=offer')
        self.assertFragmentNotIn(response, {"filters": [{"id": "filter-promo-or-discount", "name": "Скидки и акции"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "filter-promo"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "filter-discount-only"}]})
        self.assertFragmentIn(response, {"filters": [{"id": "promo-type-filter"}]})

    def test_filter_by_promo_id(self):
        response = self.report.request_json('place=prime&text=offer&filter-by-promo-id=xMpQQQC5I4INzFCab3WEmw')
        self.assertFragmentIn(response, {"totalOffers": 1})
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "promos": [
                        {
                            "key": "xMpQQQC5I4INzFCab3WEmw",
                        }
                    ],
                }
            ],
        )

    def test_has_promo(self):
        response = self.report.request_json('place=prime&text=offer&has-promo=0')
        self.assertFragmentIn(response, {"totalOffers": 12})

        response = self.report.request_json('place=prime&text=offer&has-promo=1')
        self.assertFragmentIn(response, {"totalOffers": 11})

    def test_filter_by_promo_id_2(self):
        response = self.report.request_json('place=prime&text=offer&filter-by-promo-id=xMpQQQC5I4INzFCab3WEmw')
        self.assertFragmentIn(response, {"totalOffers": 1})
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "promos": [
                        {
                            "key": "xMpQQQC5I4INzFCab3WEmw",
                        }
                    ],
                }
            ],
        )

    def test_filter_nonverifiable_promo(self):
        response = self.report.request_json('place=prime&text=offer&numdoc=20')
        self.assertFragmentIn(response, {"totalOffers": 12})
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "promos": [
                        {
                            "type": "gift-with-purchase",
                        }
                    ],
                }
            ],
        )

        response = self.report.request_json('place=prime&text=offer&filter-nonverifiable-promos=1&numdoc=20')
        self.assertFragmentIn(response, {"totalOffers": 11})
        self.assertFragmentNotIn(
            response,
            [
                {
                    "entity": "offer",
                    "promos": [
                        {
                            "type": "gift-with-purchase",
                        }
                    ],
                }
            ],
        )

    def test_disable_by_dynamic(self):
        self.dynamic.market_dynamic.disabled_shop_promos.clear()

        # сначала находится
        response = self.report.request_json('place=prime&text=nplusm')
        self.assertFragmentIn(response, [{"promos": NotEmpty()}])

        # добавляем в динамик - больше не находится
        self.dynamic.market_dynamic.disabled_shop_promos += [DynamicShop(11)]

        response = self.report.request_json('place=prime&text=nplusm')
        self.assertFragmentNotIn(response, [{"promos": NotEmpty()}])

        # удаляем из динамика - снова находится
        self.dynamic.market_dynamic.disabled_shop_promos.clear()
        response = self.report.request_json('place=prime&text=nplusm')
        self.assertFragmentIn(response, [{"promos": NotEmpty()}])


if __name__ == '__main__':
    main()
