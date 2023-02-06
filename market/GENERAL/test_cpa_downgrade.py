#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Offer, Shop
from core.testcase import TestCase, main
from core.matcher import Absent


class T(TestCase):
    @classmethod
    def prepare(cls):
        pass

    # MARKETOUT-12195
    @classmethod
    def prepare_sandbox(cls):
        cls.index.shops += [
            Shop(fesh=1219501, cpa=Shop.CPA_REAL),
            Shop(fesh=1219503, cpa=Shop.CPA_NO),
        ]

        cls.index.offers += [
            Offer(hyperid=121950101, fesh=1219501, cpa=Offer.CPA_NO, price=100),
            Offer(hyperid=121950103, fesh=1219501, cpa=Offer.CPA_REAL, price=300),
            Offer(hyperid=121950301, fesh=1219503, cpa=Offer.CPA_NO, price=100),
            Offer(hyperid=121950303, fesh=1219503, cpa=Offer.CPA_REAL, price=300, override_cpa_check=True),
        ]

    def test_shop_cpa_real_offer_cpa_no(self):
        """
        Запрашиваем офферы для магазина 1219501 (cpa=real), оффер с cpa=no
        cpa-шность оффера должна остаться той же
        """
        response = self.report.request_json('place=productoffers&hyperid=121950101')
        self.assertFragmentIn(response, {'entity': 'offer', 'model': {'id': 121950101}, 'cpa': Absent()})

    def test_shop_cpa_real_offer_cpa_real(self):
        """
        Запрашиваем офферы для магазина 1219501 (cpa=real), оффер с cpa=real
        cpa-шность оффера должна остаться той же
        """
        response = self.report.request_json('place=productoffers&hyperid=121950103')
        self.assertFragmentIn(response, {'entity': 'offer', 'model': {'id': 121950103}, 'cpa': 'real'})

    def test_shop_cpa_no_offer_cpa_no(self):
        """
        Запрашиваем офферы для магазина 1219503 (cpa=no), оффер с cpa=no
        cpa-шность оффера должна остаться той же
        """
        response = self.report.request_json('place=productoffers&hyperid=121950301')
        self.assertFragmentIn(response, {'entity': 'offer', 'model': {'id': 121950301}, 'cpa': Absent()})

    def test_shop_cpa_no_offer_cpa_real_allow_cpc_pessimization(self):
        """
        Запрашиваем офферы для магазина 1219503 (cpa=no), оффер с cpa=real
        cpa-шность оффера должна стать no

        Дополнительно проверяется причина пессимизации CPA
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=121950303&debug=1&rearr-factors=market_filter_cpa_to_cpc_degradation=0'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'model': {'id': 121950303},
                'cpa': Absent(),
                "debug": {"properties": {"CPA_PESSIMIZATION_BY_SHOP_SETTINGS": "1"}},
            },
        )

    def test_shop_cpa_no_offer_cpa_real_forbid_cpc_pessimization(self):
        """
        Запрашиваем офферы для магазина 1219503 (cpa=no), оффер с cpa=real
        скроется из-за пессимизации CPA.
        """
        response = self.report.request_json('place=productoffers&hyperid=121950303&debug=1')
        self.assertFragmentIn(response, {"filters": {"HIDE_CPA_PESSIMIZATION_BY_SHOP_SETTINGS": 1}})


if __name__ == '__main__':
    main()
