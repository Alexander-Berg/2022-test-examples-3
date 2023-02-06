#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, MarketSku, MnPlace, Model, Offer, Shop
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.01)

        cls.index.shops += [
            Shop(fesh=10, priority_region=213, blue=Shop.BLUE_REAL),
            Shop(fesh=11, priority_region=213, blue=Shop.BLUE_REAL),
            Shop(fesh=20, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=30, priority_region=213),
            Shop(fesh=40, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, online=True, tariff="CLICKS"),
            Shop(fesh=50, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, online=True, tariff="CLICKS"),
        ]

        cls.index.models += [Model(hyperid=101, hid=101), Model(hyperid=307, hid=1, title='model unmatched', ts=103000)]

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
            Offer(fesh=20, title='paid dsbs unmatched', price=7000, bid=10, ts=107000, cpa=Offer.CPA_REAL),
            Offer(fesh=30, title='paid cpc unmatched', price=6000, bid=10, ts=106000),
            Offer(fesh=40, title='free dsbs unmatched', price=5000, bid=10, cbid=10, ts=105000, cpa=Offer.CPA_REAL),
            Offer(fesh=50, title='free cpc unmatched', price=4000, bid=10, cbid=10, ts=104000),
        ]

        cls.index.offers += [
            Offer(fesh=20, title='paid dsbs', hyperid=101, price=7000, bid=10, ts=107000, cpa=Offer.CPA_REAL),
            Offer(fesh=30, title='paid cpc', hyperid=101, price=6000, bid=10, ts=106000),
            Offer(fesh=40, title='free dsbs', hyperid=101, price=5000, bid=10, cbid=10, ts=105000, cpa=Offer.CPA_REAL),
            Offer(fesh=50, title='free cpc', hyperid=101, price=4000, bid=10, cbid=10, ts=104000),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 107000).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 106000).respond(0.04)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 105000).respond(0.05)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 104000).respond(0.06)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 103000).respond(0.07)

    def test_search_output_without_flag_search(self):
        response = self.report.request_json('place=prime&rids=213&text=unmatched&debug=da&show-urls=cpa,external')

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "free cpc unmatched"},
                    },
                    {
                        "titles": {"raw": "free dsbs unmatched"},
                    },
                    {
                        "titles": {"raw": "paid cpc unmatched"},
                    },
                    {"titles": {"raw": "paid dsbs unmatched"}},
                ]
            },
            preserve_order=True,
        )

        self.assertFragmentIn(
            response,
            {"results": [{"titles": {"raw": "model unmatched"}, "debug": {"metaProperties": {"CPM": "7000"}}}]},
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
