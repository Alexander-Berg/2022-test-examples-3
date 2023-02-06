#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, Offer, Shop
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        _ = 100

        cls.index.models += [
            Model(hyperid=1, title='title#1', hid=100, model_clicks=100),
            Model(hyperid=2, title='title#2', hid=100, model_clicks=100),
            Model(hyperid=3, title='title#3', hid=100, model_clicks=100),
        ]

        cls.index.offers += [
            Offer(title='query1 #1', hyperid=1, descr='a', fesh=1, discount=1),
            Offer(title='query1 #2', hyperid=2, descr='|a|b', fesh=1, discount=1),
            Offer(
                title='query1 #3',
                hyperid=1,
                fesh=1,
                discount=1,
                descr='|Тип поверхности - варочная | Способ подключения - электрическая | Установка - независимая | Габариты (ШхГ) - 57.6 x 50.7 см | Панель конфорок - стеклокерамика',
            ),
        ]

        cls.index.shops += [Shop(fesh=1, regions=[1])]

    def test_offer_description_field(self):
        response = self.report.request_json('place=prime&text=query1')
        self.assertFragmentIn(response, {"results": [{'description': 'a'}]})
        self.assertFragmentIn(response, {"results": [{'description': '|a|b'}]})
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'description': '|Тип поверхности - варочная | Способ подключения - электрическая | Установка - независимая | Габариты (ШхГ) - 57.6 x 50.7 см | Панель конфорок - стеклокерамика'
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()
