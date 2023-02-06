#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Offer, Shop


class T(TestCase):
    @classmethod
    def prepare(cls):
        pass

    # MARKETOUT-13207
    @classmethod
    def prepare_offers_with_and_without_cpc_links(cls):
        """
        1320701 - CPA-магазин с CPC-ссылками
        1320702 - CPA-only магазин (без CPC-ссылок)
        """
        cls.index.shops += [
            Shop(fesh=1320701, cpa=Shop.CPA_REAL),
            Shop(fesh=1320702, cpc=Shop.CPC_NO, cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(hyperid=1320701, fesh=1320701, price=10000, cpa=Offer.CPA_REAL),
            Offer(hyperid=1320702, fesh=1320702, price=10000, cpa=Offer.CPA_REAL),
        ]

    def test_offer_with_cpc_link(self):
        """
        Проверяем, что у CPA+CPC магазина оффер остался
        """
        response = self.report.request_json('place=productoffers&hyperid=1320701&hide-offers-without-cpc-link=1')
        self.assertFragmentIn(response, {"entity": "offer", "model": {"id": 1320701}})

    def test_offer_without_cpc_link(self):
        """
        Проверяем, что у CPA-only магазина оффера нет
        """
        response = self.report.request_json('place=productoffers&hyperid=1320702&hide-offers-without-cpc-link=1')
        self.assertFragmentNotIn(response, {"entity": "offer", "model": {"id": 1320702}})

    def test_offer_without_cpc_link_regress_param_is_equal_to_zero(self):
        """
        Регресс:
        Проверяем, что у CPA-only магазина оффер есть, если указывать параметр hide-offers-without-cpc-link=0
        """
        response = self.report.request_json('place=productoffers&hyperid=1320702&hide-offers-without-cpc-link=0')
        self.assertFragmentIn(response, {"entity": "offer", "model": {"id": 1320702}})

    def test_offer_without_cpc_link_regress_param_is_absent(self):
        """
        Регресс:
        Проверяем, что у CPA-only магазина оффер есть, если вообще не указывать параметр hide-offers-without-cpc-link
        """
        response = self.report.request_json('place=productoffers&hyperid=1320702')
        self.assertFragmentIn(response, {"entity": "offer", "model": {"id": 1320702}})


if __name__ == '__main__':
    main()
