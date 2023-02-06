#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import ClickType, Model, Offer, Shop
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        for i in range(1, 10):
            cls.index.shops += [
                Shop(fesh=i, priority_region=213),
            ]

        # threshold equals to 43, min bid equals to 13
        cls.index.offers += [
            Offer(title="iphone 7s 512 gb", hyperid=1000, fesh=1, price=10000, bid=60),
            Offer(title="iphone 7s 512 gb", hyperid=1000, fesh=2, price=10000, bid=55),
            Offer(title="iphone 7s 512 gb", hyperid=1000, fesh=3, price=10000, bid=50),
            Offer(title="iphone 7s 512 gb", hyperid=1000, fesh=4, price=10000, bid=45),
            Offer(title="iphone 7s 512 gb", hyperid=1000, fesh=5, price=10000, bid=40),
            Offer(title="iphone 7s 512 gb", hyperid=1000, fesh=6, price=10000, bid=35),
        ]

        cls.index.models += [Model(title="iphone 7s 512 gb", hyperid=1000)]

    def test_model_card(self):
        response = self.report.request_json('place=productoffers&hyperid=1000&pp=6&show-urls=external&rids=213')
        self.assertEqual(6, response.count({"entity": "offer"}))
        logs = [
            {'shop_id': 1, 'cp': 56},
            {'shop_id': 2, 'cp': 51},
            {'shop_id': 3, 'cp': 46},
            {'shop_id': 4, 'cp': 41},
            {'shop_id': 5, 'cp': 36},
            {'shop_id': 6, 'cp': 13},
        ]

        for item in logs:
            self.click_log.expect(ClickType.EXTERNAL, **item)

    def test_model_wizard(self):
        _ = self.report.request_bs('place=parallel&text=iphone+7s+512+gb&show-urls=external&rids=213')
        logs = [
            {'shop_id': 1, 'cp': 56},
            {'shop_id': 2, 'cp': 51},
            {'shop_id': 3, 'cp': 46},
            {'shop_id': 4, 'cp': 41},
            {'shop_id': 5, 'cp': 36},
        ]

        for item in logs:
            self.click_log.expect(ClickType.EXTERNAL, pp=404, **item)


if __name__ == '__main__':
    main()
