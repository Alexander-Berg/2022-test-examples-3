#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from math import exp
from core.types import ClickType, CpaCategory, CpaCategoryType, HyperCategory, HyperCategoryType, Offer, Shop
from core.testcase import TestCase, main
from core.matcher import Contains, Round


class T(TestCase):
    LOW_SC_FESH = 596  # see market/report/data/shop_conversions.csv
    HIGH_SC_FESH = 181298  # see market/report/data/shop_conversins.csv
    CPA_HID = 101
    CPC_HID = 102

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]

        cls.index.shops += [
            Shop(fesh=cls.HIGH_SC_FESH, regions=[213]),
            Shop(fesh=cls.LOW_SC_FESH, regions=[213]),
        ]
        cls.index.cpa_categories += [
            CpaCategory(hid=cls.CPA_HID, fee=100, regions=[213], cpa_type=CpaCategoryType.CPA_NON_GURU),
            CpaCategory(hid=cls.CPC_HID, fee=100, regions=[213], cpa_type=CpaCategoryType.CPC_AND_CPA),
        ]

    @classmethod
    def prepare_formula_with_cpc(cls):
        cls.index.shops += [
            Shop(fesh=95, regions=[213]),  # shop conversion = 0.01833762415
        ]
        cls.index.offers += [
            Offer(fesh=95, title="tv", bid=4),
        ]

    def test_formula_with_cpc(self):
        exp_flags = (
            "market_force_search_auction=ShopConversion;market_tweak_search_auction_params=0.21,0.22,0.23;"
            "market_tweak_search_auction_shop_conversion_params=0.11,0.12,0.13"
        )
        response = self.report.request_json("rids=213&place=prime&text=tv&rearr-factors={}&debug=da".format(exp_flags))

        self.assertFragmentIn(
            response,
            [
                Contains(
                    "Using search auction with parameters:",
                    "cpcAlpha=0.21 cpcBeta=0.22 cpcGamma=0.23 ",
                    "shopConversionAlpha=0.11 shopConversionBeta=0.12 shopConversionGamma=0.13 ",
                    "auction type: 6",
                ),
            ],
        )

        bid = 4
        min_bid = 1
        shop_conversion = 0.01833762415
        cpc_alpha, cpc_beta, cpc_gamma = 0.21, 0.22, 0.23
        sc_alpha, sc_beta, sc_gamma = 0.11, 0.12, 0.13
        multiplier = (
            1.0
            + sc_alpha * (1.0 / (sc_gamma * exp(-sc_beta * shop_conversion) + 1.0) - 1.0 / (sc_gamma + 1.0))
            + cpc_alpha * (1.0 / (cpc_gamma * exp(-cpc_beta * (bid - min_bid)) + 1.0) - 1.0 / (cpc_gamma + 1.0))
        )
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "tv"},
                "debug": {
                    "properties": {
                        "SHOP_CONVERSION": str(shop_conversion),
                        "BID": str(bid),
                        "MIN_BID": str(min_bid),
                        "DOCUMENT_AUCTION_TYPE": "CPC",
                        "AUCTION_MULTIPLIER": Round(multiplier, 2),  # 1.01698130644
                        "CPM": str(int(30000 * multiplier)),  # 30509
                    }
                },
            },
        )

    @classmethod
    def prepare_formula_with_cpa(cls):
        cls.index.cpa_categories += [
            CpaCategory(hid=100, fee=120, regions=[213], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
        ]
        cls.index.hypertree += [
            HyperCategory(hid=100, output_type=HyperCategoryType.GURU),
        ]
        cls.index.shops += [
            Shop(fesh=154, regions=[213]),  # shop conversion = 0.01423709374
        ]
        cls.index.offers += [
            Offer(fesh=154, title="toothbrush", ts=1, hid=100),
        ]

    def test_formula_with_cpa(self):
        exp_flags = (
            "market_force_search_auction=ShopConversion;market_tweak_search_auction_cpa_params=0.41,0.42,0.43;"
            "market_tweak_search_auction_shop_conversion_params=0.31,0.32,0.33"
        )
        response = self.report.request_json(
            "rids=213&place=prime&text=toothbrush&rearr-factors={}&debug=da".format(exp_flags)
        )

        self.assertFragmentIn(
            response,
            [
                Contains(
                    "Using search auction with parameters:",
                    "cpaAlpha=0.41 cpaBeta=0.42 cpaGamma=0.43",
                    "shopConversionAlpha=0.31 shopConversionBeta=0.32 shopConversionGamma=0.33",
                    "auction type: 6",
                ),
            ],
        )

        _ = 150
        _ = 120
        shop_conversion = 0.01423709374
        sc_alpha, sc_beta, sc_gamma = 0.31, 0.32, 0.33
        multiplier = 1.0 + sc_alpha * (
            1.0 / (sc_gamma * exp(-sc_beta * shop_conversion) + 1.0) - 1.0 / (sc_gamma + 1.0)
        )
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "toothbrush"},
                "debug": {
                    "properties": {
                        "SHOP_CONVERSION": str(shop_conversion),
                        "DOCUMENT_AUCTION_TYPE": "CPA",
                        "AUCTION_MULTIPLIER": Round(multiplier, 2),  # 1.010846615
                        "CPM": str(int(30000 * multiplier)),  # 30325
                    }
                },
            },
        )

    @classmethod
    def prepare_auto_broker_policy_cpa(cls):
        cls.index.offers += [
            Offer(title="cpa_auto_broker with low conversion", hid=cls.CPA_HID, bid=200, fesh=cls.LOW_SC_FESH),
            Offer(title="cpa_auto_broker with high conversion", hid=cls.CPA_HID, bid=200, fesh=cls.HIGH_SC_FESH),
        ]

    def test_auto_broker_policy_cpa(self):
        """
        Запршиваем 2 cpa оффера. У них одинаковое всё кроме конверсионности их магазинов.
        Чтобы конверсионность внесла больший вклад в образование fee, указывем market_tweak_search_auction_shop_conversion_params=5,5,5 ,
        а остальные alpha,beta,gamma в 1,1,1.

        В выдаче репорта мы должны увидеть, что оффер с большей конверсионностью находится выше, чем второй оффер,
        и его fee тоже должен быть больше.

        В логах мы должны увидеть, что оба оффера с одинаковыми click_price(cp), а их fee соответсвуют выдаче report'a
        """
        factors = (
            "market_force_search_auction=ShopConversion;market_tweak_search_auction_params=1,1,1;market_tweak_search_auction_cpa_params=1,1,1;"
            "market_tweak_search_auction_shop_conversion_params=5,5,5"
        )
        response = self.report.request_json(
            "rids=213&place=prime&text=cpa&show-urls=cpa,external&debug=da&rearr-factors=" + factors
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "cpa_auto_broker with high conversion"},
                    },
                    {
                        "titles": {"raw": "cpa_auto_broker with low conversion"},
                    },
                ],
            },
            preserve_order=True,
        )
        self.show_log.expect(title="cpa_auto_broker with high conversion", position=1, click_price=2)
        self.show_log.expect(title="cpa_auto_broker with low conversion", position=2, click_price=1)

        self.click_log.expect(ClickType.EXTERNAL, cp=2, position=1)
        self.click_log.expect(ClickType.EXTERNAL, cp=1, position=2)

    @classmethod
    def prepare_auto_broker_policy_cpc(cls):
        cls.index.offers += [
            Offer(title="cpc_auto_broker with low conversion", hid=cls.CPC_HID, bid=200, fesh=cls.LOW_SC_FESH),
            Offer(title="cpc_auto_broker with high conversion", hid=cls.CPC_HID, bid=200, fesh=cls.HIGH_SC_FESH),
        ]

    def test_auto_broker_policy_cpc(self):
        """
        Запршиваем 2 cpc оффера. У них одинаковое всё кроме конверсионности их магазинов.
        Чтобы конверсионность внесла больший вклад в образование click_price, указывем market_tweak_search_auction_shop_conversion_params=5,5,5 ,
        а остальные alpha,beta,gamma в 1,1,1.

        В выдаче репорта мы должны увидеть, что оффер с большей конверсионностью находится выше, чем второй оффер.

        В логах мы должны увидеть, что cp(click_price) оффера с большей конверсионностью, выше чем
        у второго оффера, а их fee одинковые.
        """
        factors = (
            "market_force_search_auction=ShopConversion;market_tweak_search_auction_params=1,1,1;market_tweak_search_auction_cpa_params=1,1,1;"
            "market_tweak_search_auction_shop_conversion_params=5,5,5"
        )
        response = self.report.request_json(
            "rids=213&place=prime&text=cpc&show-urls=cpa,external&debug=da&rearr-factors=" + factors
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "cpc_auto_broker with high conversion"},
                    },
                    {
                        "titles": {"raw": "cpc_auto_broker with low conversion"},
                    },
                ],
            },
            preserve_order=True,
        )
        self.show_log.expect(title="cpc_auto_broker with high conversion", position=1, click_price=2)
        self.show_log.expect(title="cpc_auto_broker with low conversion", position=2, click_price=1)

        self.click_log.expect(ClickType.EXTERNAL, cp=2, position=1)
        self.click_log.expect(ClickType.EXTERNAL, cp=1, position=2)


if __name__ == '__main__':
    main()
