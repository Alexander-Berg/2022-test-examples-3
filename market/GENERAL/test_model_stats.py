#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, Offer, RegionalModel, Shop
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(fesh=1, priority_region=225, regions=[54]),
        ]

        cls.index.models += [
            Model(hyperid=101),
        ]
        cls.index.offers += [
            Offer(hyperid=101, fesh=1, price=200),
            Offer(hyperid=101, fesh=1, price=300),
            Offer(hyperid=101, fesh=1, price=400),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=102, rids=[54], offers=7, price_min=100, price_max=200, price_med=150),
            RegionalModel(hyperid=102, rids=[56], offers=5, price_min=100, price_max=300, price_med=175),
            RegionalModel(hyperid=102, rids=[-1], offers=12, price_min=100, price_max=300, price_med=160),
        ]

    def test_model_stats_format(self):
        '''Проверяем, что корректно отображаются данные из RegionalModels:
        - для модели данные занеслись по всем регионам магазина, 0 - для любого региона;
        - для захардкоженных записей в файл статистики все данные отображаются
        '''
        response = self.report.request_json('place=model_stats&hyperid=101')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 3,
                    "results": [
                        {
                            "region": 0,
                            "offerCount": 3,
                            "minPrice": "200",
                            "maxPrice": "400",
                            "medPrice": "300",
                        },
                        {
                            "region": 54,
                            "offerCount": 3,
                            "minPrice": "200",
                            "maxPrice": "400",
                            "medPrice": "300",
                        },
                        {
                            "region": 225,
                            "offerCount": 3,
                            "minPrice": "200",
                            "maxPrice": "400",
                            "medPrice": "300",
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json('place=model_stats&hyperid=102')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 3,
                    "results": [
                        {
                            "region": 0,
                            "offerCount": 12,
                            "minPrice": "100",
                            "maxPrice": "300",
                            "medPrice": "160",
                        },
                        {
                            "region": 54,
                            "offerCount": 7,
                            "minPrice": "100",
                            "maxPrice": "200",
                            "medPrice": "150",
                        },
                        {
                            "region": 56,
                            "offerCount": 5,
                            "minPrice": "100",
                            "maxPrice": "300",
                            "medPrice": "175",
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        """Проверяется, что общее количество для показа = total"""
        self.access_log.expect(total_renderable='3').times(2)

    def test_hyperid_count(self):
        '''Проверяем ровно один hyperid'''
        response = self.report.request_json('place=model_stats')
        self.assertFragmentIn(response, {"message": "One and only one hyper_id should be specified"})
        self.error_log.expect(code=3043)

        response = self.report.request_json('place=model_stats&hyperid=101&hyperid=102')
        self.assertFragmentIn(response, {"message": "One and only one hyper_id should be specified"})
        self.error_log.expect(code=3043)


if __name__ == '__main__':
    main()
