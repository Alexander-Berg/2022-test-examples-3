#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import HyperCategory, Offer, RequestCategoryClassification, RequestCategoryClassificationFeature
from core.testcase import TestCase, main
from core.matcher import Round, NotEmptyList


class T(TestCase):
    def check_categories_ranking_factors(self, response):
        """Проверяем что вывод в categoreies_ranking и в categories_ranking_json совпадает"""
        factors = []

        for category_ranking in response.root['debug']['categories_ranking']:
            category_ranking_props = {}
            for prop in category_ranking.split('\t'):
                prop_pair = prop.split('=')
                if len(prop_pair) != 2:
                    continue
                category_ranking_props[prop_pair[0]] = prop_pair[1]

            if (
                'hid' in category_ranking_props
                and 'factors' in category_ranking_props
                and 'factors_names' in category_ranking_props
            ):
                hid = category_ranking_props['hid']
                factors_values = category_ranking_props['factors'].split()
                factors_names = category_ranking_props['factors_names'].split()

                assert len(factors_values) == len(factors_names)
                factors.append(
                    {
                        'id': int(hid),
                        'factors': dict(
                            [
                                (name.upper(), Round(float(value), 5))
                                for name, value in zip(factors_names, factors_values)
                                if float(value) != 0
                            ]
                        ),
                    }
                )

        self.assertFragmentIn(response, {'debug': {'categories_ranking_json': factors}}, allow_different_len=False)

    @classmethod
    def prepare(cls):
        cls.request_categories_classificator.on_default_request().respond()
        cls.settings.default_search_experiment_flags += ['market_write_category_redirect_features=20']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

    @classmethod
    def prepare_request_category_classification_factors(cls):
        # готовим офферы из двух разных категорий
        cls.index.hypertree += [
            HyperCategory(hid=156151, name='vegetables'),
            HyperCategory(hid=156152, name='weapon'),
        ]

        cls.index.offers += [
            Offer(title="luk repchatyy", hid=156151),
            Offer(title="luk dugoobraznyy", hid=156152),
        ]

        # ответ классификатора с категориями, соответствующими подготовленным офферам
        cls.request_categories_classificator.on_request("luk").respond(
            found_categories_classification=[
                RequestCategoryClassification(
                    category_id=156151,
                    probability=0.432,
                    rank=1,
                    features=[
                        RequestCategoryClassificationFeature(name='bCategoryL2CBPMN', value=0.1102239266037941),
                        RequestCategoryClassificationFeature(name='bmFullTBPMN', value=0.11022398975871),
                    ],
                ),
                RequestCategoryClassification(category_id=156152, probability=0.764, rank=3.0, features=[]),
            ]
        )

    def test_request_category_classification_base_factors(self):
        """Базовые факторы из классификатора запроса по категориям на place=prime.
        https://st.yandex-team.ru/MARKETOUT-15615
        https://st.yandex-team.ru/MARKETOUT-16729
        """

        # Задаем запрос, на который матчатся офферы из разных категорий
        request = 'place=prime&text=luk&cvredirect=0&debug=da'
        response = self.report.request_json(request)
        self.check_categories_ranking_factors(response)

        # Проверяем, что у офферов появляются соотствующие их категориям факторы
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "titles": {"raw": "luk repchatyy"},
                            "debug": {
                                "factors": {
                                    "CATEGORY_CLSSFY_PROBABILITY": "0.4320000112",
                                    "CATEGORY_CLSSFY_RANK": "1",
                                    "CATEGORY_CLSSFY_B_CATEGORY_L2CBPMN": "0.1102239266",
                                    "CATEGORY_CLSSFY_BM_FULL_TBPMN": "0.1102239862",
                                }
                            },
                        },
                        {
                            "titles": {"raw": "luk dugoobraznyy"},
                            "debug": {
                                "factors": {"CATEGORY_CLSSFY_PROBABILITY": "0.7639999986", "CATEGORY_CLSSFY_RANK": "3"}
                            },
                        },
                    ]
                }
            },
        )

        # На запросы из советника (pp=1002) факторы не должны считаться
        # https://st.yandex-team.ru/MARKETOUT-16910
        response = self.report.request_json(request + '&client=sovetnik')
        self.assertFragmentNotIn(
            response,
            {
                "debug": {
                    "factors": {
                        "CATEGORY_CLSSFY_PROBABILITY": "0.4320000112",
                    }
                }
            },
        )

    def test_request_category_classification_ranking_factors(self):
        """Факторы из классификатора запроса по категориям в ранжировании категорий.
        https://st.yandex-team.ru/MARKETOUT-12334
        """

        # Задаем запрос, на который матчатся офферы из разных категорий
        request = 'place=prime&text=luk&cvredirect=0&debug=da'
        response = self.report.request_json(request)
        self.check_categories_ranking_factors(response)

        self.assertFragmentIn(
            response,
            {
                'categories_ranking_json': [
                    {
                        'id': 156151,
                        'factors': {
                            "CLSSFY_PROBABILITY": Round(0.432),
                            "CLSSFY_RANK": 1,
                            "CLSSFY_B_CATEGORY_L2CBPMN": Round(0.110224, 5),
                            "CLSSFY_BM_FULL_TBPMN": Round(0.110224, 5),
                        },
                    },
                    {'id': 156152, 'factors': {"CLSSFY_PROBABILITY": Round(0.764), "CLSSFY_RANK": 3}},
                ]
            },
        )

    @classmethod
    def prepare_collapsing(cls):

        cls.index.offers += [
            Offer(hid=100, title='замок железный', hyperid=101),
            Offer(hid=100, title='замок электронный', hyperid=101),
            Offer(hid=200, title='замок каменный', hyperid=201),
            Offer(hid=200, title='замок средневековый', hyperid=202),
            Offer(hid=300, title='замок деревянный', hyperid=300),
        ]

    def test_collapse_statistics_factors(self):
        """Статистика по схлапыванию работает не правильно
        Учитываются все офферы, даже от одной и той же модели
        Правильную статистику можно получить если использовать
        market_use_collapsed_statistic_for_category_ranking=1
        """

        response = self.report.request_json('place=prime&text=замок&debug=da')
        self.assertFragmentIn(
            response,
            {
                'categories_ranking_json': [
                    {'id': 100, 'factors': {"OFFERS_COUNT_RATIO": Round(0.4)}},
                    {'id': 200, 'factors': {"OFFERS_COUNT_RATIO": Round(0.4)}},
                    {'id': 300, 'factors': {"OFFERS_COUNT_RATIO": Round(0.2)}},
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json('place=prime&text=замок&debug=da&allow-collapsing=1')
        self.assertFragmentIn(
            response,
            {
                'categories_ranking_json': [
                    {'id': 100, 'factors': {"OFFERS_COUNT_RATIO": Round(0.4)}},
                    {'id': 200, 'factors': {"OFFERS_COUNT_RATIO": Round(0.4)}},
                    {'id': 300, 'factors': {"OFFERS_COUNT_RATIO": Round(0.2)}},
                ]
            },
            allow_different_len=False,
        )

    def test_factors_for_navnodes_ranking(self):
        """Кроме факторов по категории пишутся также факторы по навигационным узлам"""

        response = self.report.request_json('place=prime&text=замок&debug=da&allow-collapsing=1')
        self.assertFragmentIn(
            response, {'debug': {'navnodes_ranking': NotEmptyList(), 'navnodes_ranking_json': NotEmptyList()}}
        )


if __name__ == '__main__':
    main()
