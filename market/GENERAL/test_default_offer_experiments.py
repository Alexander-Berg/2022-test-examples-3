#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, Currency, MarketSku, MnPlace, Offer, Shop, Tax, Vat
from core.testcase import TestCase, main
from unittest import skip


class T(TestCase):
    @classmethod
    def prepare_random(cls):
        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.01)

        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[225]),  # Common shop
            Shop(fesh=2, priority_region=213, regions=[225]),  # Common shop
            Shop(fesh=3, priority_region=213, regions=[225]),  # Common shop
            Shop(
                # Виртуальный магазин синего маркета
                fesh=11,
                datafeed_id=1,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=3, fesh=1, price=100, ts=3001),
            Offer(hyperid=3, fesh=2, price=200, ts=3002),
            Offer(hyperid=3, fesh=2, price=1000, ts=3003),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3002).respond(0.03)

    def test_random(self):
        """
        Проверяем, что при разных yandexuid в ДО выбираются разные офферы, независимо от формулы.
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=default&hyperid=3&rids=213&rgb=green_with_blue'
            '&rearr-factors=market_default_offer_by_random=1'
            '&yandexuid=1'
        )
        self.assertFragmentIn(response, {'shop': {'id': 1}})
        response = self.report.request_json(
            'place=productoffers&offers-set=default&hyperid=3&rids=213&rgb=green_with_blue'
            '&rearr-factors=market_default_offer_by_random=1'
            '&yandexuid=10'
        )
        self.assertFragmentIn(response, {'shop': {'id': 2}})

    @classmethod
    def prepare_force_cheap_beru_to_default_offer(cls):
        cls.index.offers += [
            Offer(hyperid=4, fesh=1, price=100, ts=4001),
            Offer(hyperid=4, fesh=2, price=200, ts=4002),
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=4,
                sku='4',
                blue_offers=[
                    BlueOffer(price=100.5, vat=Vat.VAT_18, offerid='4000', feedid=4000, ts=4011),
                ],
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4001).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4002).respond(0.04)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4011).respond(0.01)

        cls.index.offers += [
            Offer(hyperid=5, fesh=1, price=100, ts=5001),
            Offer(hyperid=5, fesh=2, price=200, ts=5002),
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=5,
                sku='5',
                blue_offers=[
                    BlueOffer(price=101, vat=Vat.VAT_18, offerid='5000', feedid=5000, ts=5011),
                ],
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5001).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5002).respond(0.04)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5011).respond(0.01)

    @skip('deprecated due to blue priority')
    def test_force_cheap_beru_to_default_offer(self):
        """
        Проверяем, что при флаге force_cheap_beru_to_default_offer в ДО попадает Беру, если у него одна из самых низких цен на карточке.

        Тест утстарел, так как на в ДО безусловный приоритет синих
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=default&hyperid=4&rids=213'
            '&rearr-factors=market_money_force_cheap_beru_to_default_offer=1;'
        )
        self.assertFragmentIn(response, {'shop': {'id': 11}})
        response = self.report.request_json(
            'place=productoffers&offers-set=default&hyperid=5&rids=213'
            '&rearr-factors=market_money_force_cheap_beru_to_default_offer=1'
        )
        self.assertFragmentIn(response, {'shop': {'id': 2}})


if __name__ == '__main__':
    main()
