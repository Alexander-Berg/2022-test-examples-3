#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import BlueOffer, Currency, MarketSku, Model, Shop, Tax
from core.matcher import Absent, Contains


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                cpa=Shop.CPA_REAL,
            ),
        ]

    def _test_valid_discount(self, wareid, color, discount):
        params = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}&rgb={}'
        response = self.report.request_json(params.format(wareid, color))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'totalOffers': 1,
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': wareid,
                            'prices': {
                                'discount': {
                                    'percent': discount,
                                }
                            },
                        },
                    ],
                }
            },
        )

    def _test_invalid_discount(self, wareid, color, discount_skip_reason=None):
        params = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}&rgb={}&debug=1'
        response = self.report.request_json(params.format(wareid, color))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'totalOffers': 1,
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': wareid,
                            'prices': {
                                'discount': Absent(),
                            },
                        },
                    ],
                }
            },
        )

        if discount_skip_reason:
            self.assertFragmentIn(response, {"logicTrace": [Contains(discount_skip_reason)]})

    @classmethod
    def prepare_blue_offer(cls):
        cls.index.shops += [
            Shop(fesh=888, datafeed_id=888, priority_region=213, supplier_type=Shop.FIRST_PARTY, blue=Shop.BLUE_REAL),
        ]
        # blue offer with valid discount and valid history price
        blue_offer_1 = BlueOffer(
            waremd5='BlueOffer1-ValidHist-w',
            price=1000,
            price_old=1500,
            price_history=1500,
            feedid=888,
            offerid='shop_sku_1',
            history_price_is_valid=True,
        )

        # blue offer with valid discount and invalid history price
        blue_offer_2 = BlueOffer(
            waremd5='BlueOff1-InvalidHist-w',
            price=1000,
            price_old=1500,
            price_history=1500,
            feedid=888,
            offerid='shop_sku_2',
            history_price_is_valid=False,
        )

        cls.index.mskus += [
            MarketSku(
                title='blue market sku1p',
                hyperid=112,
                sku=11200001,
                waremd5='MarketSku1-IiLVm1goleg',
                blue_offers=[blue_offer_1, blue_offer_2],
            ),
        ]

        cls.index.models += [
            Model(hyperid=112, hid=112, title='blue model 1'),
        ]

    def test_blue_offer_with_valid_history_on_blue(self):
        """Историческая цена для синих офферов на Синем маркете не проходит проверку,
        скидка для оффера с валидной исторической ценой должна быть."""
        wareid = 'BlueOffer1-ValidHist-w'
        self._test_valid_discount(wareid, 'BLUE', 33)

    def test_blue_offer_with_invalid_history_on_blue(self):
        """Историческая цена для синих офферов на Синем маркете не проходит проверку,
        скидка для оффера с невалидной исторической ценой должна быть."""
        wareid = 'BlueOff1-InvalidHist-w'
        self._test_valid_discount(wareid, 'BLUE', 33)

    def test_blue_offer_with_valid_history_on_white(self):
        """Историческая цена для синих офферов на Белом маркете проходит проверку,
        скидка для оффера с валидной исторической ценой должна быть."""
        wareid = 'BlueOffer1-ValidHist-w'
        self._test_valid_discount(wareid, 'GREEN_WITH_BLUE', 33)

    def test_blue_offer_with_invalid_history_on_white(self):
        """Историческая цена для синих офферов на Белом маркете не проходит проверку,
        скидкa для оффера с невалидной исторической ценой должнa быть."""
        wareid = 'BlueOff1-InvalidHist-w'
        self._test_valid_discount(wareid, 'GREEN_WITH_BLUE', 33)


if __name__ == '__main__':
    main()
