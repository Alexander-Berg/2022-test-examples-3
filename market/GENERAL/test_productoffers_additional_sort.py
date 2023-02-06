#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import MinBidsCategory, MnPlace, Model, Offer, Shop
from core.testcase import TestCase, main


# Tests for MARKETOUT-18229: additional sort (after user sort) as by default

# list of available user sortings
USER_SORTS = [
    "guru_popularity",
    "aprice",
    "rorp",
    "discount_p",
]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(fesh=1, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=2, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=3, priority_region=213, cpa=Shop.CPA_REAL),
        ]

        cls.index.models += [
            Model(hyperid=102, hid=102000),
        ]

        cls.index.offers += [
            Offer(hyperid=102, fesh=1, price=10000, bid=10, randx=3, ts=102001),
            Offer(hyperid=102, fesh=2, price=10000, bid=10, randx=2, ts=102002),
            Offer(hyperid=102, fesh=3, price=10000, bid=10, randx=1, ts=102003),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 102001).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 102002).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 102003).respond(0.03)

        cls.index.min_bids_category_stats += [
            MinBidsCategory(
                category_id=102000,
                geo_group_id=0,
                price_group_id=0,
                drr=0.22,
                search_conversion=0.01,
                card_conversion=0.02,
                full_card_conversion=0.03,
            ),
        ]

    def getOffersDebugInfo(self, response, field):
        return [offer["debug"][field] for offer in response.root["search"]["results"]]

    def test_sort(self):
        """
        Test if offers with equal user relevances are in expected order
        """

        def getDebugStringValues(response, section, field):
            return [str(offer["debug"][section][field]) for offer in response.root["search"]["results"]]

        for how in USER_SORTS:
            request = "debug=1&place=productoffers&hyperid=102&rids=213&how=" + how
            response = self.report.request_json(request)

            # check if offers are in expected order
            self.assertFragmentIn(
                response,
                {"results": [{"shop": {"id": 3}}, {"shop": {"id": 2}}, {"shop": {"id": 1}}]},
                preserve_order=True,
            )

            # check if actual bids are used
            self.assertEqual(
                getDebugStringValues(response, "properties", "BID"), getDebugStringValues(response, "sale", "bid")
            )

            # check if actual fees are used
            self.assertEqual(
                getDebugStringValues(response, "properties", "FEE"),
                getDebugStringValues(response, "sale", "brokeredFee"),
            )

    def test_dont_ignore_ctr_pow_rearr_factor(self):
        """
        Check if 'market_ha_ctr_pow' rearr-factor matters for user sort
        """

        request = "debug=1&place=productoffers&hyperid=102&rids=213&rearr-factors="
        factor = "market_ha_ctr_pow=0.1;"

        # default sort - check that 'market_ha_ctr_pow' affects
        response1 = self.report.request_json(request)
        response2 = self.report.request_json(request + factor)
        self.assertNotEqual(
            self.getOffersDebugInfo(response1, "properties"), self.getOffersDebugInfo(response2, "properties")
        )

        # user sort - check that 'market_ha_ctr_pow' doesn't affect
        for how in USER_SORTS:
            userSortRequest = "how=" + how + "&" + request
            response1 = self.report.request_json(userSortRequest)
            response2 = self.report.request_json(userSortRequest + factor)
            self.assertNotEqual(
                self.getOffersDebugInfo(response1, "properties"), self.getOffersDebugInfo(response2, "properties")
            )


if __name__ == '__main__':
    main()
