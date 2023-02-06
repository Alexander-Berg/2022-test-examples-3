#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import HyperCategory, HyperCategoryType, Model, Offer, Shop
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[225]),
            Shop(fesh=2, priority_region=213, regions=[225]),
        ]

        cls.index.hypertree += [HyperCategory(hid=1, output_type=HyperCategoryType.GURU)]

        cls.index.offers += [
            Offer(title='offer 1', hid=1, fesh=1, hyperid=100),
            Offer(title='offer 2', hid=1, fesh=1, hyperid=101),
            Offer(title='offer 3', hid=1, fesh=2, hyperid=101),
            Offer(title='offer 4', hid=1, fesh=2, hyperid=100),
            Offer(title='offer 5 webmaster', hid=1, fesh=1, hyperid=100, from_webmaster=True),
            Offer(title='offer 6 webmaster', hid=1, fesh=1, hyperid=102, from_webmaster=True),
            Offer(title='offer 7 webmaster', hid=1, fesh=2, hyperid=102, from_webmaster=True),
        ]

        cls.index.models += [
            Model(title='model100', hyperid=100, hid=1),
            Model(title='model101', hyperid=101, hid=1),
            Model(title='model102', hyperid=102, hid=1),
        ]

    def test_filter_webmaster_offers(self):
        """Проверяем, что по умолчанию не показываем офферы от вебмастера"""
        response = self.report.request_json('place=prime&text=offer&pp=18&rids=213')

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'offer', 'titles': {'raw': 'offer 1'}},
                    {'entity': 'offer', 'titles': {'raw': 'offer 2'}},
                    {'entity': 'offer', 'titles': {'raw': 'offer 3'}},
                    {'entity': 'offer', 'titles': {'raw': 'offer 4'}},
                ]
            },
            allow_different_len=False,
        )

    def test_show_webmaster_offers(self):
        """Проверяем, офферы от вебмастера показываются по флажку эксперимента"""
        response = self.report.request_json(
            'place=prime&text=offer&pp=18&rids=213&rearr-factors=market_show_webmaster_offers=1'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'offer', 'titles': {'raw': 'offer 1'}},
                    {'entity': 'offer', 'titles': {'raw': 'offer 2'}},
                    {'entity': 'offer', 'titles': {'raw': 'offer 3'}},
                    {'entity': 'offer', 'titles': {'raw': 'offer 4'}},
                    {'entity': 'offer', 'titles': {'raw': 'offer 5 webmaster'}},
                    {'entity': 'offer', 'titles': {'raw': 'offer 6 webmaster'}},
                    {'entity': 'offer', 'titles': {'raw': 'offer 7 webmaster'}},
                ]
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
