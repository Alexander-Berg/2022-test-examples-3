#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Const, HybridAuctionParam, MnPlace, Model, Offer, Shop
from core.testcase import TestCase, main


class T(TestCase):
    """
    MALISA-563
    """

    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(fesh=1, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=2, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=3, priority_region=213, cpa=Shop.CPA_REAL),
        ]

        cls.index.models += [
            Model(hyperid=101, hid=100, title="glass sphere"),
        ]

        cls.index.hybrid_auction_settings += [HybridAuctionParam(category=Const.ROOT_HID, cpc_ctr_for_cpc=0.033)]

        cls.index.offers += [
            Offer(
                hyperid=101, fesh=1, price=10000, bid=10, ts=101001, waremd5="AAAAAAAAAAAAAAAAAAAAAA"
            ),  # default; CPM = 100000 * 10 * 0.02 ~ 20000
            Offer(
                hyperid=101, fesh=2, price=10000, bid=50, ts=101002, waremd5="BBBBBBBBBBBBBBBBBBBBBA"
            ),  # premium & top-1; CPM = 100000 * 50 * 0.01 ~ 50000
            Offer(
                hyperid=101, fesh=3, price=10000, bid=30, ts=101003, waremd5="CCCCCCCCCCCCCCCCCCCCCC"
            ),  # CPM = 100000 * 30 * 0.01 ~ 30000
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101001).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101002).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101003).respond(0.025)

    def test_search(self):
        # слово sphere встречается и в названии модели, проверим что вернётся дефолтный офер
        response = self.report.request_json('alice=1&place=prime&pp=420&rids=213&text=sphere&use-default-offers=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'entity': 'product',
                            'id': 101,
                            "offers": {"items": [{"entity": "offer", "wareId": "BBBBBBBBBBBBBBBBBBBBBA"}]},
                        }
                    ],
                }
            },
            preserve_order=False,
        )

    def test_default_offers_click_price_in_case_minbid_more_then_threshold_bid(self):
        """Если документ преодолевает порог - но ставка необходимая для преодоления ниже минставки то списывается минставка"""
        _ = self.report.request_json('alice=1&place=prime&pp=420&rids=213&text=sphere&use-default-offers=1&cpmdo=1')
        self.click_log.expect(ware_md5='BBBBBBBBBBBBBBBBBBBBBA', min_bid=13, cb=13, cp=13)

    def test_default_offer_click_price_in_case_offer_under_threshold(self):
        """Если документ не преодолевает порог - то списывается минставка"""
        _ = self.report.request_json(
            'alice=1&place=prime&pp=420&rids=213&text=sphere&use-default-offers=1&cpmdo=1'
            '&rearr-factors=market_premium_offer_threshold_mult=1000'
        )
        self.click_log.expect(ware_md5='BBBBBBBBBBBBBBBBBBBBBA', min_bid=13, cb=13, cp=13)


if __name__ == '__main__':
    main()
