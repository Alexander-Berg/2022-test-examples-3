#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import HyperCategory, Model, Offer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.hypertree = [
            HyperCategory(
                hid=1,
                children=[
                    HyperCategory(hid=2, children=[HyperCategory(hid=3)]),
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=1, hid=1),
            Model(hyperid=2, hid=2),
        ]

        cls.index.offers += [
            Offer(title='offer hid 1', hyperid=1, waremd5='OfferHid1____________g'),
            Offer(title='offer hid 2', hyperid=2, waremd5='OfferHid2____________g'),
            Offer(
                title='offer hid 3 without model', hid=3, waremd5='OfferHid3____________g', auto_creating_model=False
            ),
        ]

    def test_category_path_rendering(self):
        '''
        С параметром get-category-path=1 выводим весь путь категорий товара
        Путь выводим без самой верхней категории (все товары)
        '''

        response = self.report.request_json('place=prime&hyperid=1&allow-collapsing=1&get-category-path=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1,
                        "categories": [
                            {
                                "entity": "category",
                                "id": 1,
                            },
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json('place=prime&hyperid=2&allow-collapsing=1&get-category-path=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 2,
                        "categories": [
                            {
                                "entity": "category",
                                "id": 1,
                            },
                            {
                                "entity": "category",
                                "id": 2,
                            },
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json('place=prime&hid=3&allow-collapsing=1&get-category-path=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": 'OfferHid3____________g',
                        "categories": [
                            {
                                "entity": "category",
                                "id": 1,
                            },
                            {
                                "entity": "category",
                                "id": 2,
                            },
                            {
                                "entity": "category",
                                "id": 3,
                            },
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_category_path_rendering_negative(self):
        '''
        Без get-category-path или с get-category-path=0 выводим одну категорию, как раньше
        '''

        response = self.report.request_json('place=prime&hyperid=2&allow-collapsing=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 2,
                        "categories": [
                            {
                                "entity": "category",
                                "id": 2,
                            },
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json('place=prime&hid=3&allow-collapsing=1&get-category-path=0')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": 'OfferHid3____________g',
                        "categories": [
                            {
                                "entity": "category",
                                "id": 3,
                            },
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
