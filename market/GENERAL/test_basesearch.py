#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import DynamicShop, Model, Offer, Shop
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        for i in range(100):
            cls.index.models += [
                Model(hid=1, hyperid=i, randx=i),
            ]
            cls.index.offers += [
                Offer(hyperid=i, fesh=i % 3 + 1, randx=i),
            ]

        for i in range(3):
            cls.index.shops += [
                Shop(fesh=i + 1, priority_region=213),
            ]

        cls.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(2)]
        cls.disable_randx_randomize()

    def test_sorting(self):
        response = self.report.request_json('place=prime&hid=1&onstock=1&debug=1&how=aprice')
        self.assertFragmentIn(response, {'total': 61})


if __name__ == '__main__':
    main()
