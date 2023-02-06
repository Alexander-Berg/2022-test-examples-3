#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import MnPlace, Offer, Shop
from core.testcase import TestCase, main
from unittest import skip


class T(TestCase):
    """
    Experimental tests for experimental features.
    Feel free to skip any broken tests, just inform richard@
    """

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.001)
        cls.index.shops += [
            Shop(fesh=101, priority_region=213),
            Shop(fesh=102, priority_region=213),
            Shop(fesh=103, priority_region=213),
            Shop(fesh=104, priority_region=213),
            Shop(fesh=105, priority_region=213),
        ]
        cls.index.offers += [
            Offer(hyperid=1, fesh=101, price=10000, bid=100),
            Offer(hyperid=1, fesh=102, price=10000, bid=90),
            Offer(hyperid=1, fesh=103, price=10000, bid=70),
            Offer(hyperid=1, fesh=104, price=10000, bid=50),
            Offer(hyperid=1, fesh=105, price=10000, bid=10),
        ]

    def test_without_threshold(self):
        """ """
        _ = self.report.request_json('place=productoffers&hyperid=1&numdoc=3&rids=213')

        for pos, cp in [[1, 62], [2, 55], [3, 50]]:
            self.show_log.expect(click_price_vcg=cp, position=pos)

    @skip('Threshold is not implemented for VCG now')
    def test_with_threshold(self):
        """ """
        _ = self.report.request_json(
            'place=productoffers&hyperid=1&numdoc=6&pp=6&offers-set=listCpa&rids=213&rearr-factors=market_ranging_cpa_by_ue_in_top_cpa_multiplier=1'
        )

        for pos, cp in [[1, 53], [2, 44], [3, 35], [4, 32]]:
            self.show_log.expect(click_price_vcg=cp, position=pos)


if __name__ == '__main__':
    main()
