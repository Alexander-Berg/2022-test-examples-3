#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, ModelTransition, Offer, Shop
from core.testcase import TestCase, main
from core.matcher import NoKey


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.models += [
            Model(hyperid=10),
            Model(hyperid=20),
            Model(hyperid=30, hid=100500),
            Model(hyperid=40, hid=100500),
            Model(hyperid=50, hid=100500),
        ]

        cls.index.shops += [Shop(fesh=1, priority_region=213)]

        cls.index.offers += [
            Offer(hyperid=10, fesh=1, price=10, title='10-1'),
            Offer(hyperid=10, fesh=1, price=10, title='10-2'),
            Offer(hyperid=20, fesh=1, price=20, title='20-1'),
            Offer(hyperid=20, fesh=1, price=20, title='20-2'),
            Offer(hyperid=20, fesh=1, price=20, title='20-3'),
        ]

        cls.index.model_transitions += [
            ModelTransition(old_id=1, primary_id=10),
            ModelTransition(old_id=2, primary_id=20),
            ModelTransition(old_id=3, primary_id=30),
        ]

    def test_modelinfo_enabled(self):
        response = self.report.request_json('place=modelinfo&hyperid=1&with-rebuilt-model=1&rids=0')
        self.assertFragmentIn(
            response,
            {
                'type': 'model',
                'id': 10,
                'deletedId': 1,
            },
        )

    def test_modelinfo_disabled(self):
        response = self.report.request_json('place=modelinfo&hyperid=1&with-rebuilt-model=0&rids=0')
        self.assertFragmentNotIn(
            response,
            {
                'type': 'model',
                'id': 10,
                'deletedId': 1,
            },
        )

    def test_modelinfo_multiple_models(self):
        response = self.report.request_json('place=modelinfo&hyperid=1,2&with-rebuilt-model=1&rids=0')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'type': 'model',
                            'id': 10,
                            'deletedId': 1,
                        },
                        {
                            'type': 'model',
                            'id': 20,
                            'deletedId': 2,
                        },
                    ]
                }
            },
        )

    def test_productoffers(self):
        response = self.report.request_json('place=productoffers&hyperid=1&with-rebuilt-model=1&rids=0')
        self.assertFragmentIn(response, {'search': {'modelId': 10, 'deletedModelId': 1}})

        self.assertEqual(2, response.count({"entity": "offer"}))
        for n in range(1, 3):
            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "model": {"id": 10},
                    "titles": {"raw": "10-{}".format(n)},
                },
            )

        response = self.report.request_json('place=productoffers&hyperid=2&with-rebuilt-model=1&rids=0')
        self.assertFragmentIn(response, {'search': {'modelId': 20, 'deletedModelId': 2}})

        self.assertEqual(3, response.count({"entity": "offer"}))
        for n in range(1, 4):
            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "model": {"id": 20},
                    "titles": {"raw": "20-{}".format(n)},
                },
            )

        response = self.report.request_json('place=productoffers&hyperid=20&with-rebuilt-model=1&rids=0')
        self.assertFragmentIn(response, {'search': {'deletedModelId': NoKey('deletedModelId')}})

    def test_productoffers_multiple_hyperid(self):
        response = self.report.request_json(
            'place=productoffers&hyperid=1,2&with-rebuilt-model=1&rids=0&use_multiple_hyperid=1'
        )
        self.assertFragmentIn(response, {'modelIdWithOffers': [{'model_id': 10, 'deletedModelId': 1}]})

        self.assertFragmentIn(response, {'modelIdWithOffers': [{'model_id': 20, 'deletedModelId': 2}]})

        response = self.report.request_json(
            'place=productoffers&hyperid=1,20&with-rebuilt-model=1&rids=0&use_multiple_hyperid=1'
        )
        self.assertFragmentIn(response, {'modelIdWithOffers': [{'model_id': 10, 'deletedModelId': 1}]})

        self.assertFragmentIn(
            response, {'modelIdWithOffers': [{'model_id': 20, 'deletedModelId': NoKey('deletedModelId')}]}
        )

    def test_compare_products_enabled(self):
        response = self.report.request_json(
            'place=compare_products&with-rebuilt-model=1&hid=100500&hyperid=3&hyperid=40&hyperid=50'
        )
        self.assertFragmentIn(
            response,
            {
                'comparedParameters': {
                    'comparedIds': [
                        {'id': '30'},
                        {'id': '40'},
                        {'id': '50'},
                    ],
                }
            },
        )

    def test_compare_products_disabled(self):
        response = self.report.request_json(
            'place=compare_products&with-rebuilt-model=0&hid=100500&hyperid=3&hyperid=40&hyperid=50'
        )
        self.assertFragmentIn(
            response,
            {
                'comparedParameters': {
                    'comparedIds': [
                        {'id': '40'},
                        {'id': '50'},
                    ],
                }
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
