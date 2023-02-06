#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Offer, Region, Shop
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.downgrade_cpa_offers_by_shop_settings = False

        cls.index.regiontree += [
            Region(rid=213, name='Москва', tz_offset=10800),
            Region(rid=2, name='Санкт-Петербург', tz_offset=10800),
        ]

        cls.index.shops += [Shop(fesh=1219503, cpa=Shop.CPA_NO)]

        cls.index.offers += [
            Offer(hyperid=121950303, fesh=1219503, cpa=Offer.CPA_REAL, price=300, override_cpa_check=True),
        ]

    def test_shop_cpa_no_offer_cpa_real(self):
        """
        Запрашиваем офферы для магазина 1219503 (cpa=no), оффер с cpa=real
        cpa-шность оффера не должна стать no, т.к. downgrade запрещен
        """
        response = self.report.request_json('place=productoffers&hyperid=121950303')
        self.assertFragmentIn(response, {'entity': 'offer', 'model': {'id': 121950303}, 'cpa': 'real'})

    @classmethod
    def prepare_cpa_partner(cls):
        cls.index.shops += [
            Shop(fesh=1508902, priority_region=213, regions=[2], cpa=Shop.CPA_NO),
        ]

        cls.index.offers += [
            Offer(hyperid=150890101, fesh=1508901, cpa=Offer.CPA_REAL, price=300, override_cpa_check=True),
            Offer(hyperid=150890203, fesh=1508902, cpa=Offer.CPA_REAL, price=300, override_cpa_check=True),
        ]

    def test_cpa_partner_shop_cpa_no_offer_cpa_real(self):
        """
        Запрашиваем офферы для ПИ-магазина 1508902 (cpa=no), оффер с cpa=real в разных регионах
        cpa-шность оффера не должна стать no, т.к. downgrade запрещен
        """
        for region in [213, 2]:
            response = self.report.request_json('place=productoffers&hyperid=150890203&rids={}'.format(region))
            self.assertFragmentIn(response, {'entity': 'offer', 'model': {'id': 150890203}, 'cpa': 'real'})

    @classmethod
    def prepare_cpc_no(cls):
        cls.index.shops += [
            Shop(fesh=1652602, priority_region=213, regions=[2], cpa=Shop.CPA_NO, cpc=Shop.CPC_NO),
        ]

        cls.index.offers += [
            Offer(hyperid=165260203, fesh=1652602, cpa=Offer.CPA_REAL, price=300, override_cpa_check=True),
        ]

    def test_shop_cpc_no_cpa_no_offer_cpa_real(self):
        """
        Запрашиваем офферы для CPA-only ПИ-магазина 1652602 (cpa=no), оффер с cpa=real в разных регионах
        cpa-шность оффера не должна стать no, т.к. downgrade запрещен
        """
        for region in [213, 2]:
            response = self.report.request_json('place=productoffers&hyperid=165260203&rids={}'.format(region))
            self.assertFragmentIn(response, {'entity': 'offer', 'model': {'id': 165260203}, 'cpa': 'real'})


if __name__ == '__main__':
    main()
