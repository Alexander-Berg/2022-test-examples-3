#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import HyperCategory, HyperCategoryType, Offer, Shop
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare_search_auction(cls):
        # disable MN mocks
        cls.matrixnet.set_defaults = False

        cls.index.hypertree += [
            HyperCategory(hid=1000, output_type=HyperCategoryType.GURULIGHT),
        ]
        for i in range(1, 20):
            cls.index.offers.append(
                Offer(title="The offer title", price=100 + 5 * i, hid=1000, fesh=i, feedid=i, offerid=i)
            )
            cls.index.shops.append(Shop(fesh=i, priority_region=213))

    def get_prime_mn_value(self, fesh, rearr=""):
        response = self.report.request_json(
            "debug=1&place=prime&rids=213&fesh={}&text=The+offer+title".format(fesh) + rearr
        )
        return response.root["search"]["results"][0]["debug"]["fullFormulaInfo"][0]["value"]

    def check_formula_value_on_market_search(self, formula_name):
        rearr = "&rearr-factors=market_search_mn_algo={};allow_panther=0".format(formula_name)
        fesh = 1

        mn_value = self.get_prime_mn_value(fesh, rearr)
        response = self.report.request_xml(
            "debug=1&place=bids_recommender&rids=213&type=market_search&fesh={0}&feed_shoffer_id={0}-{0}&text=The+offer+title".format(
                fesh
            )
            + rearr
        )
        self.assertFragmentIn(
            response, '<target-offer><offer-debug-info matrixnet="{}"/></target-offer>'.format(mn_value)
        )

    def test_market_search__MNA_CommonThreshold_v2_251519_m10_x_245752_044(self):
        self.check_formula_value_on_market_search("MNA_CommonThreshold_v2_251519_m10_x_245752_044")

    def test_market_search__MNA_CommonThreshold_v2_251519_m10_x_287136_0495(self):
        self.check_formula_value_on_market_search("MNA_CommonThreshold_v2_251519_m10_x_287136_0495")

    def test_market_search__MNA_CommonThreshold_v2_251519_m10_x_293520_050(self):
        self.check_formula_value_on_market_search("MNA_CommonThreshold_v2_251519_m10_x_293520_050")


if __name__ == '__main__':
    main()
