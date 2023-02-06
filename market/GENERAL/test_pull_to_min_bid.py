#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Offer, Shop
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.shops += [
            Shop(fesh=1, priority_region=213),
            Shop(fesh=2, priority_region=213),
        ]
        cls.index.offers += [
            Offer(hyperid=1, hid=1, fesh=1, price=10000, bid=1, pull_to_min_bid=True),
            Offer(hyperid=1, hid=1, fesh=2, price=10000, bid=1, pull_to_min_bid=False, ts=1),
        ]

    def test_productoffers(self):
        """
        Проверяем работу фильтрации по ставке на productoffers
        """
        response = self.report.request_json('place=productoffers&hyperid=1&debug=1&debug-doc-count=5')
        # Только один оффер прошёл фильтрацию
        self.assertFragmentIn(
            response,
            [
                {'entity': 'offer'},
            ],
            allow_different_len=False,
        )
        # Оффер отвалился из-за слишком низкой ставки
        self.assertFragmentIn(
            response,
            {
                'properties': {
                    'TS': '1',
                    'DROP_REASON': 'TOO_LOW_BID',
                }
            },
        )

    def test_prime(self):
        """
        Проверяем работу фильтрации по ставке на prime
        """
        response = self.report.request_json('place=prime&hid=1&debug=1&debug-doc-count=5')
        # Только один оффер прошёл фильтрацию
        self.assertFragmentIn(
            response,
            [
                {'entity': 'product'},
                {'entity': 'offer'},
            ],
            allow_different_len=False,
        )
        # Оффер отвалился из-за слишком низкой ставки
        self.assertFragmentIn(
            response,
            {
                'properties': {
                    'TS': '1',
                    'DROP_REASON': 'TOO_LOW_BID',
                }
            },
        )

    def test_prime_low_bid_filter_disabled(self):
        """
        Проверяем отключение фильтрации по ставке на prime
        """
        response = self.report.request_json(
            'place=prime&hid=1&debug=1&debug-doc-count=5&rearr-factors=filter_by_low_bid_disabled=1'
        )
        # Все офферы вернулись
        self.assertFragmentIn(
            response,
            [
                {'entity': 'product'},
                {'entity': 'offer'},
                {'entity': 'offer'},
            ],
            allow_different_len=False,
        )

    def test_shop_offers(self):
        """
        Проверяем, что на place=shopoffers фильтрация НЕ работает
        """
        response = self.report.request_xml('place=shopoffers&fesh=2&shop-offers-chunk=1')
        # Оффер есть в выдаче, не смотря на низкую ставку
        self.assertFragmentIn(
            response,
            '''
            <offer />
        ''',
        )

    def test_bids_recommender_partner_api(self):
        """
        Проверяем, что на place=bids_recommender с выставленным &api=partner фильтрация НЕ работает
        """
        response = self.report.request_xml('place=bids_recommender&fesh=2&hyperid=1&rids=213&api=partner')
        # Оффер есть в выдаче, не смотря на низкую ставку
        self.assertFragmentIn(
            response,
            '''
            <offer />
        ''',
        )

    def test_bids_recommender_partner_interface(self):
        """
        Проверяем, что на place=bids_recommender с выставленным &client=partnerinterface фильтрация НЕ работает
        """
        response = self.report.request_xml('place=bids_recommender&fesh=2&hyperid=1&rids=213&client=partnerinterface')
        # Оффер есть в выдаче, не смотря на низкую ставку
        self.assertFragmentIn(
            response,
            '''
            <offer />
        ''',
        )


if __name__ == '__main__':
    main()
