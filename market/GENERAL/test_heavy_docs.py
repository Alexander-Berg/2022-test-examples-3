#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Book, HyperCategory, Model, Offer, VCluster
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare_model_only_search(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.hypertree += [
            HyperCategory(hid=1),
            HyperCategory(hid=2, visual=True),
        ]

        cls.index.models += [
            Model(hid=1, hyperid=11, title='ёлочка'),
        ]

        cls.index.vclusters += [
            VCluster(hid=2, vclusterid=1000000012, title='ёлочка визуальная'),
        ]

        cls.index.books += [
            Book(hid=1, hyperid=13, title='книга ёлочка', author='Дед Мороз'),
        ]

        cls.index.offers += [
            Offer(hyperid=11, title='ёлочка оффер'),
            Offer(hyperid=1000000012, title='ёлочка визуальная оффер'),
            Offer(hyperid=13, title='книга ёлочка оффер'),
            Offer(title='ёлочка оффер без модели'),
        ]

    def test_model_only_search(self):
        """
        Проверяем, что под флагом выключается поиск по офферам, но при этом
        всё равно дозапрашиваются ДО
        """

        request = (
            'place=prime&text=ёлочка&rearr-factors='
            'market_models_only_search=%d&use-default-offers=1&rearr-factors=market_metadoc_search=no'
        )

        request_no_flag = request % 0
        response = self.report.request_json(request_no_flag)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': t}}
                    for t in [
                        'ёлочка',
                        'ёлочка визуальная',
                        'Дед Мороз "книга ёлочка"',
                        'ёлочка оффер',
                        'ёлочка визуальная оффер',
                        'книга ёлочка оффер',
                        'ёлочка оффер без модели',
                    ]
                ]
            },
            allow_different_len=False,
        )

        request_no_flag_coll = request % 0 + '&allow-collapsing=1'
        response = self.report.request_json(request_no_flag_coll)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': t}}
                    for t in [
                        'ёлочка',
                        'ёлочка визуальная',
                        'Дед Мороз "книга ёлочка"',
                        'ёлочка оффер без модели',
                    ]
                ]
            },
            allow_different_len=False,
        )

        request_flag = request % 1
        request_flag_coll = request_flag + '&allow-collapsing=1'
        for r in (request_flag, request_flag_coll):
            response = self.report.request_json(r)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {'titles': {'raw': t}}
                        for t in [
                            'ёлочка',
                            'ёлочка визуальная',
                            'Дед Мороз "книга ёлочка"',
                        ]
                    ]
                },
                allow_different_len=False,
            )

        for r in (request_no_flag, request_no_flag_coll, request_flag, request_flag_coll):
            response = self.report.request_json(r)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {'id': i, 'offers': {'items': [{'titles': {'raw': t}}]}}
                        for i, t in [
                            (11, 'ёлочка оффер'),
                            (1000000012, 'ёлочка визуальная оффер'),
                            (13, 'книга ёлочка оффер'),
                        ]
                    ]
                },
            )


if __name__ == '__main__':
    main()
