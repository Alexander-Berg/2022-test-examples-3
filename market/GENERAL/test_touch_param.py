#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import ClickType, Model, Offer, Shop, VCluster
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[213], cpa=Shop.CPA_REAL),
            Shop(fesh=2, priority_region=213, regions=[213]),
            Shop(fesh=3, priority_region=213, regions=[213], cpa=Shop.CPA_REAL),
            Shop(fesh=4, priority_region=213, regions=[213]),
        ]
        cls.index.models += [
            Model(hyperid=101),
        ]
        cls.index.vclusters += [
            VCluster(vclusterid=1000000002),
        ]
        cls.index.offers += [
            Offer(title='iphone red', hyperid=101, cpa=Offer.CPA_REAL, fesh=1),
            Offer(title='iphone black', hyperid=101, cpa=Offer.CPA_NO, fesh=2),
            Offer(title='iphone red case', vclusterid=1000000002, cpa=Offer.CPA_REAL, fesh=3),
            Offer(title='iphone black case', vclusterid=1000000002, cpa=Offer.CPA_NO, fesh=4),
        ]

    def test_prime_touch_0(self):
        self.report.request_json(
            'place=prime&text=iphone&rids=213&show-urls=encrypted,cpa,phone,showPhone&phone=1&touch=0'
        )
        self.click_log.expect(ClickType.EXTERNAL, touch=0).times(4)
        self.click_log.expect(ClickType.CPA, touch=0).times(2)
        self.click_log.expect(ClickType.PHONE, touch=0).times(4)
        self.click_log.expect(ClickType.SHOW_PHONE, touch=0).times(4)
        self.show_log.expect(touch=0).times(14)

    def test_prime_touch_1(self):
        self.report.request_json(
            'place=prime&text=iphone&rids=213&show-urls=encrypted,cpa,phone,showPhone'
            '&phone=1&touch=1&allow-collapsing=0'
        )
        self.click_log.expect(ClickType.EXTERNAL, touch=1).times(4)
        self.click_log.expect(ClickType.CPA, touch=1).times(2)
        self.click_log.expect(ClickType.PHONE, touch=1).times(4)
        self.click_log.expect(ClickType.SHOW_PHONE, touch=1).times(4)
        self.show_log.expect(touch=1).times(14)

    def test_productoffers_touch_0(self):
        self.report.request_json(
            'place=productoffers&pp=6&hyperid=101&rids=213&touch=0&show-urls=cpa,external,showPhone,signed'
        )
        self.click_log.expect(ClickType.CPA, touch=0).times(1)
        self.click_log.expect(ClickType.EXTERNAL, touch=0).times(2)
        self.click_log.expect(ClickType.SHOW_PHONE, touch=0).times(2)
        self.show_log.expect(touch=0).times(5)

    def test_productoffers_touch_1(self):
        self.report.request_json(
            'place=productoffers&pp=6&hyperid=101&rids=213&touch=1&show-urls=cpa,external,showPhone,signed'
        )
        self.click_log.expect(ClickType.CPA, touch=1).times(1)
        self.click_log.expect(ClickType.EXTERNAL, touch=1).times(2)
        self.click_log.expect(ClickType.SHOW_PHONE, touch=1).times(2)
        self.show_log.expect(touch=1).times(5)


if __name__ == '__main__':
    main()
