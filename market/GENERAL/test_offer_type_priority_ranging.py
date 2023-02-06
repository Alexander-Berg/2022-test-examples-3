#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, MarketSku, MnPlace, Model, Offer, Shop
from core.testcase import TestCase, main
from unittest import skip


base_rearr_flags_dict = {
    "ready_for_intervals_merging": 1,
    "enable_flat_courier_options": 1,
    "market_write_meta_factors": 1,
    "market_white_cpa_on_blue": 2,
    "market_white_cpa_on_blue_no_delivery": 1,
    "show_credits_on_white": 1,
    "market_uncollapse_supplier": 1,
    "enable_business_id": 1,
    "use_offer_type_priority_as_main_factor_in_top": 1,
    "use_offer_type_priority_as_main_factor_in_do": 1,
}


def dict_to_rearr(rearr_flags):
    result = ""
    for key in rearr_flags.keys():
        result += str(key) + "=" + str(rearr_flags[key]) + ";"

    return result


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.01)

        cls.index.shops += [
            Shop(fesh=10, priority_region=213, blue=Shop.BLUE_REAL),
            Shop(fesh=11, priority_region=213, blue=Shop.BLUE_REAL),
            Shop(fesh=20, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=30, priority_region=213),
            Shop(fesh=40, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, online=True, tariff="FREE"),
            Shop(fesh=50, priority_region=213, regions=[225], online=True, tariff="FREE"),
        ]

        cls.index.models += [
            Model(hyperid=101, hid=101),
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=101,
                sku=101,
                blue_offers=[
                    BlueOffer(price=9000, feedid=10, waremd5='H10-S10-p9000-F10-OOOO', ts=109000),
                    BlueOffer(price=8000, feedid=11, waremd5='H10-S10-p8000-F11-OOOO', ts=109000),
                ],
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 109000).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 108000).respond(0.02)

        cls.index.offers += [
            Offer(fesh=20, title='paid dsbs', hyperid=101, price=7000, bid=10, ts=107000, cpa=Offer.CPA_REAL),
            Offer(fesh=30, title='paid cpc', hyperid=101, price=6000, bid=10, ts=106000),
            Offer(fesh=40, title='free dsbs', hyperid=101, price=5000, bid=0, cbid=0, ts=105000, cpa=Offer.CPA_REAL),
            Offer(fesh=50, title='free cpc', hyperid=101, price=4000, bid=0, cbid=0, ts=104000),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 107000).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 106000).respond(0.04)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 105000).respond(0.05)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 104000).respond(0.06)

        cls.index.models += [
            Model(hyperid=201, hid=201),
        ]
        cls.index.offers += [
            Offer(fesh=20, title='paid dsbs', hyperid=201, price=7000, bid=10, ts=207000, cpa=Offer.CPA_REAL),
            Offer(fesh=30, title='paid cpc', hyperid=201, price=6000, bid=10, ts=206000),
            Offer(fesh=40, title='free dsbs', hyperid=201, price=5000, bid=0, cbid=0, ts=205000, cpa=Offer.CPA_REAL),
            Offer(fesh=50, title='free cpc', hyperid=201, price=4000, bid=0, cbid=0, ts=204000),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 207000).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 206000).respond(0.04)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 205000).respond(0.05)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 204000).respond(0.06)

        cls.index.models += [
            Model(hyperid=301, hid=301),
        ]
        cls.index.offers += [
            Offer(fesh=30, title='paid cpc', hyperid=301, price=6000, bid=10, ts=306000),
            Offer(fesh=40, title='free dsbs', hyperid=301, price=5000, bid=0, cbid=0, ts=305000, cpa=Offer.CPA_REAL),
            Offer(fesh=50, title='free cpc', hyperid=301, price=4000, bid=0, cbid=0, ts=304000),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 306000).respond(0.04)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 305000).respond(0.05)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 304000).respond(0.06)

        cls.index.models += [
            Model(hyperid=401, hid=401),
        ]
        cls.index.offers += [
            Offer(fesh=40, title='free dsbs', hyperid=401, price=5000, bid=0, cbid=0, ts=405000, cpa=Offer.CPA_REAL),
            Offer(fesh=50, title='free cpc', hyperid=401, price=4000, bid=0, cbid=0, ts=404000),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 405000).respond(0.05)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 404000).respond(0.06)

        cls.index.models += [
            Model(hyperid=501, hid=501),
        ]
        cls.index.offers += [
            Offer(fesh=50, title='free cpc', hyperid=501, price=4000, bid=0, cbid=0, ts=504000),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 504000).respond(0.06)

        cls.index.models += [
            Model(hyperid=601, hid=601),
        ]
        cls.index.offers += [
            Offer(fesh=30, title='paid cpc', hyperid=601, price=6000, bid=10, ts=606000),
            Offer(fesh=50, title='free cpc', hyperid=601, price=4000, bid=0, cbid=0, ts=604000),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 606000).respond(0.06)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 604000).respond(0.06)

    def test_cheapest_offer_wins_without_flag(self):
        response = self.report.request_json(
            "place=productoffers&hyperid=101&offers-set=default&rearr-factors=prefer_do_with_sku=0;use_offer_type_priority_as_main_factor_in_do=0"
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "benefit": {"type": "default"},
                "prices": {"currency": "RUR", "value": "4000"},
            },
        )

    def test_market_delivery_offer_wins_with_flag(self):
        response = self.report.request_json(
            "place=productoffers&hyperid=101&offers-set=default&rearr-factors=market_blue_buybox_dsbs_conversion_coef=1;prefer_do_with_sku=0;use_offer_type_priority_as_main_factor_in_do=1"
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "benefit": {"type": "default"},
                "prices": {"currency": "RUR", "value": "5000"},  # 8000 if market_ranging_blue_offer_priority_eq_dsbs=0
            },
        )

    def test_free_dsbs_offer_wins_with_flag(self):
        response = self.report.request_json(
            "place=productoffers&hyperid=401&offers-set=default&rearr-factors=use_offer_type_priority_as_main_factor_in_do=1"
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "benefit": {"type": "default"},
                "prices": {"currency": "RUR", "value": "5000"},
            },
        )

    def test_free_cpc_offer_wins_with_flag(self):
        response = self.report.request_json(
            "place=productoffers&hyperid=501&offers-set=default&rearr-factors=use_offer_type_priority_as_main_factor_in_do=1"
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "benefit": {"type": "default"},
                "prices": {"currency": "RUR", "value": "4000"},
            },
        )

    def test_top_without_flag(self):
        response = self.report.request_json(
            "place=productoffers&hyperid=101&offers-set=top&rearr-factors=market_ranging_cpa_by_ue_in_top=0;use_offer_type_priority_as_main_factor_in_top=0;market_uncollapse_supplier=0"
        )

        self.assertFragmentIn(
            response,
            [
                {"prices": {"currency": "RUR", "value": "4000"}},
                {"prices": {"currency": "RUR", "value": "5000"}},
                {"prices": {"currency": "RUR", "value": "6000"}},
                {"prices": {"currency": "RUR", "value": "7000"}},
                {"prices": {"currency": "RUR", "value": "8000"}},
            ],
            preserve_order=True,
        )

    def test_delivery_offer_wins_top_with_flag(self):
        response = self.report.request_json(
            "place=productoffers&hyperid=101&offers-set=top&rearr-factors=use_offer_type_priority_as_main_factor_in_top=1;market_uncollapse_supplier=0;market_ranging_blue_offer_priority_eq_dsbs=0"
        )

        self.assertFragmentIn(
            response,
            [{"prices": {"currency": "RUR", "value": "8000"}}, {"prices": {"currency": "RUR", "value": "4000"}}],
            preserve_order=True,
        )

        self.assertFragmentIn(
            response,
            [{"prices": {"currency": "RUR", "value": "8000"}}, {"prices": {"currency": "RUR", "value": "5000"}}],
            preserve_order=True,
        )

        self.assertFragmentIn(
            response,
            [{"prices": {"currency": "RUR", "value": "8000"}}, {"prices": {"currency": "RUR", "value": "6000"}}],
            preserve_order=True,
        )

        self.assertFragmentIn(
            response,
            [{"prices": {"currency": "RUR", "value": "8000"}}, {"prices": {"currency": "RUR", "value": "7000"}}],
            preserve_order=True,
        )

    @skip('ситуация без флага market_no_cpc_mode_if_cpa_real пока не воспроизведена')
    def test_top_with_cpa_real__market_no_cpc_mode_if_cpa_real_0(self):
        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_no_cpc_mode_if_cpa_real"] = 0
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=productoffers&hyperid=601&puid=173712710&offers-set=defaultList%2ClistCpa&grhow=supplier&cpa=real&pp=6&rgb=green_with_blue&rearr-factors='
            + rearr_flags_str
        )

        self.assertFragmentIn(
            response,
            [
                {"prices": {"currency": "RUR", "value": "6000"}},
            ],
            preserve_order=False,
        )

    def test_top_with_cpa_real__market_no_cpc_mode_if_cpa_real_1__no_cpa(self):
        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_no_cpc_mode_if_cpa_real"] = 1
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=productoffers&hyperid=601&puid=173712710&offers-set=defaultList%2ClistCpa&grhow=supplier&cpa=real&pp=6&rgb=green_with_blue&rearr-factors='
            + rearr_flags_str
        )

        self.assertFragmentNotIn(
            response, "prices"
        )  # проверяем что оффера при включенном market_no_cpc_mode_if_cpa_real не берутся

    def test_top_with_cpa_real__market_no_cpc_mode_if_cpa_real_1__has_cpa(self):
        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_no_cpc_mode_if_cpa_real"] = 1
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=productoffers&hyperid=101&puid=173712710&offers-set=defaultList%2ClistCpa&grhow=supplier&cpa=real&pp=6&rgb=green_with_blue&rearr-factors='
            + rearr_flags_str
        )

        self.assertFragmentIn(
            response,
            [
                {"prices": {"currency": "RUR", "value": "5000"}},
                {"prices": {"currency": "RUR", "value": "7000"}},
                {"prices": {"currency": "RUR", "value": "8000"}},
            ],
            preserve_order=False,
        )

        self.assertFragmentNotIn(
            response,
            [
                {"prices": {"currency": "RUR", "value": "6000"}},
            ],
            preserve_order=False,
        )


if __name__ == '__main__':
    main()
