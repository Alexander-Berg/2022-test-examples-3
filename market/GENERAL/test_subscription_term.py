#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main
from core.types import MarketSku, Offer, Region
from core.matcher import Absent


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.regiontree += [
            Region(rid=777, name='region for place=modelinfo'),
        ]
        cls.index.offers += [
            Offer(title="has subscription term", hyperid=1001, subscription_term=12),
            Offer(title="has no subscription term", hyperid=1000),
        ]
        cls.index.mskus += [
            MarketSku(title="has subscription term", hyperid=1001, sku=50, subscription_term=12),
            MarketSku(title="has no subscription term", hyperid=1000, sku=51),
        ]

    # Тесты для офферов
    def test_offer_has_subscription_term(self):
        response = self.report.request_json('place=prime&hyperid=1001')
        self.assertFragmentIn(response, {"results": [{"entity": "offer", "subscriptionTerm": 12}]})

    def test_offer_has_no_subscription_term(self):
        response = self.report.request_json('place=prime&hyperid=1000')
        self.assertFragmentIn(response, {"results": [{"entity": "offer", "subscriptionTerm": Absent()}]})

    # Тесты для sku
    def test_sku_has_subscription_term(self):
        response = self.report.request_json('place=modelinfo&hyperid=1001&rids=777&market-sku=50')
        self.assertFragmentIn(response, {"results": [{"entity": "sku", "subscriptionTerm": 12}]})

    def test_sku_has_no_subscription_term(self):
        response = self.report.request_json('place=modelinfo&hyperid=1000&rids=777&market-sku=51')
        self.assertFragmentIn(response, {"results": [{"entity": "sku", "subscriptionTerm": Absent()}]})


if __name__ == '__main__':
    main()
