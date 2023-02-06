#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    GradeDispersionItem,
    Model,
    NavCategory,
    Opinion,
    ReviewDataItem,
    YamarecFeaturePartition,
    YamarecPlace,
)
from core.testcase import TestCase, main


def gen_department(hid):
    """
    каждый хид в дереве будет встречаться по два раза: как департамент, и как сын департамента.
    Для хидов, кратных 20, главным узлом будет департамент, для остальных - лист
    """
    primary_dep = hid % 20 == 0
    children = [NavCategory(nid=(3 * hid), hid=hid, primary=(not primary_dep))]
    return NavCategory(nid=(2 * hid), hid=hid, primary=primary_dep, children=children)


class T(TestCase):
    """
    Набор тестов для place=categories_and_models_by_history
    """

    @classmethod
    def prepare(cls):
        """
        Yamarec-конфигурация и настройки внешних сервисов для всех тестов
        а также некоторые общие данные в индекс
        """

        # index
        model_ids = list(range(1, 9))
        hids = [1000 + 10 * hyperid for hyperid in model_ids]
        # пару категорий добавляем из "дефолтовых категорий",
        # которые используются при отсутствии или недостатке категорий в истории пользователя
        hids.append(90564)
        hids.append(4954975)
        model_ids.append(9)
        model_ids.append(10)
        hid_map = dict(zip(model_ids, hids))
        random_ts = [9, 10, 4, 1, 2, 5, 7, 3, 6, 8]
        ts_map = dict(zip(model_ids, random_ts))

        departments = [gen_department(hid) for hid in hids]
        cls.index.navtree += [NavCategory(nid=100500, children=departments)]

        opinions = dict()
        opinions[1] = Opinion(reviews=5)
        opinions[2] = Opinion(total_count=3)
        opinions[3] = Opinion(reviews=0)
        opinions[4] = Opinion(reviews=1)
        opinions[5] = Opinion(rating=5)
        opinions[6] = Opinion(reviews=1)

        cls.index.models += [
            Model(hyperid=hyperid, ts=ts_map[hyperid], hid=hid_map[hyperid], opinion=opinions.get(hyperid, None))
            for hyperid in model_ids
        ]

        cls.index.model_reviews_data += [
            ReviewDataItem(review_id=100, model_id=1, short_text='super model', most_useful=1),
            ReviewDataItem(review_id=101, model_id=4, pro='short pro', most_useful=1),
            ReviewDataItem(review_id=102, model_id=6, pro='a' * 81, most_useful=1),
        ]
        cls.index.model_grade_dispersion_data += [
            GradeDispersionItem(model_id=1, five=1),
            GradeDispersionItem(model_id=4, five=4),
            GradeDispersionItem(model_id=6, five=6),
        ]
        # external services configs
        cls.crypta.on_default_request().respond(features=[])
        cls.recommender.on_request_viewed_models(user_id="yandexuid:10001").respond({"models": map(str, model_ids)})
        crypta_response = map(lambda model: {'id': model}, model_ids)
        cls.crypta.on_request_models('10001').respond(crypta_response)

        # external services configs for new user with only 3 different models(category)
        short_model_list = model_ids[:3]
        cls.recommender.on_request_viewed_models(user_id="yandexuid:10003").respond(
            {"models": map(str, short_model_list)}
        )
        crypta_response = map(lambda model: {'id': model}, short_model_list)
        cls.crypta.on_request_models('10003').respond(crypta_response)

        # external services configs for new user with only 2 different models(category), 1 from default category
        short_model_with_default_list = [1, 9]
        cls.recommender.on_request_viewed_models(user_id="yandexuid:10004").respond(
            {"models": map(str, short_model_with_default_list)}
        )
        crypta_response = map(lambda model: {'id': model}, short_model_with_default_list)
        cls.crypta.on_request_models('10004').respond(crypta_response)

        # external services configs for new user with only 4 non-default category to check append limit from default.
        nondefault_models = model_ids[:4]
        cls.recommender.on_request_viewed_models(user_id="yandexuid:10005").respond(
            {"models": map(str, nondefault_models)}
        )
        crypta_response = map(lambda model: {'id': model}, nondefault_models)
        cls.crypta.on_request_models('10005').respond(crypta_response)

        # recommendations config
        names = keys = ['model_id', 'category_id']
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.PRODUCTS_BY_HISTORY,
                kind=YamarecPlace.Type.FORMULA,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=names,
                        feature_keys=keys,
                        features=[],
                        formula_id='market',
                        splits=[{'split': 'market'}],
                    ),
                    YamarecFeaturePartition(
                        feature_names=names,
                        feature_keys=keys,
                        features=[],
                        formula_id='crypta',
                        splits=[{'split': 'crypta'}],
                    ),
                    YamarecFeaturePartition(
                        feature_names=names,
                        feature_keys=keys,
                        features=[],
                        formula_id='market+crypta',
                        splits=[{'split': 'market_crypta'}],
                    ),
                    YamarecFeaturePartition(
                        feature_names=names,
                        feature_keys=keys,
                        features=[],
                        formula_id='crypta+market',
                        splits=[{'split': 'crypta_market'}],
                    ),
                    YamarecFeaturePartition(
                        feature_names=names,
                        feature_keys=keys,
                        features=[],
                        formula_id='market+market',
                        splits=[{'split': 'market_market'}],
                    ),
                    YamarecFeaturePartition(
                        feature_names=names,
                        feature_keys=keys,
                        features=[],
                        formula_id='crypta+crypta',
                        splits=[{'split': 'crypta_crypta'}],
                    ),
                ],
            ),
        ]

    def test_output_contains_category_and_models(self):
        """
        порядок выдачи не регламентирован (и категории и модели даже могут идти в перемешку).
        Если есть история - выдаются все найденные категории,
        и все существующие в индексе документы по этим категориям.
        """
        for split in ['crypta', 'market']:
            response = self.report.request_json(
                'place=categories_and_models_by_history&yandexuid=10001&reviews-only-with-comment=0&rearr-factors=split={split}'.format(
                    split=split
                )
            )
            # для хидов, кратных 20, primary_nid == hid * 2, для остальных hid * 3
            self.assertFragmentIn(
                response,
                {
                    "categories": [
                        {"entity": "category", "id": 1010, "nid": 3030},
                        {"entity": "category", "id": 1020, "nid": 2040},
                        {"entity": "category", "id": 1030, "nid": 3090},
                        {"entity": "category", "id": 1040, "nid": 2080},
                        {"entity": "category", "id": 1050, "nid": 3150},
                        {"entity": "category", "id": 1060, "nid": 2120},
                        {"entity": "category", "id": 1070, "nid": 3210},
                        {"entity": "category", "id": 1080, "nid": 2160},
                        {"entity": "category", "id": 90564, "nid": 271692},
                        {"entity": "category", "id": 4954975, "nid": 14864925},
                    ],
                    "search": {
                        "total": 10,
                        "results": [
                            {"entity": "product", "id": 1, "categories": [{"id": 1010}]},
                            {"entity": "product", "id": 2, "categories": [{"id": 1020}]},
                            {"entity": "product", "id": 3, "categories": [{"id": 1030}]},
                            {"entity": "product", "id": 4, "categories": [{"id": 1040}]},
                            {"entity": "product", "id": 5, "categories": [{"id": 1050}]},
                            {"entity": "product", "id": 6, "categories": [{"id": 1060}]},
                            {"entity": "product", "id": 7, "categories": [{"id": 1070}]},
                            {"entity": "product", "id": 8, "categories": [{"id": 1080}]},
                            {"entity": "product", "id": 9, "categories": [{"id": 90564}]},
                            {"entity": "product", "id": 10, "categories": [{"id": 4954975}]},
                        ],
                    },
                },
                allow_different_len=False,
            )

    def test_use_default_category_if_no_history_data(self):
        """
        Если у пользователя нет истории, берем дефолтовые категории (в нашем случае в индексе таких две - 4954975, 90564)
        и запрашиваем документы для них (порядок также не регламентирован)
        """
        self.error_log.ignore()
        for split in ['crypta', 'market']:
            response = self.report.request_json(
                'place=categories_and_models_by_history&yandexuid=10002&reviews-only-with-comment=0&rearr-factors=split={split}'.format(
                    split=split
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "categories": [
                        {"entity": "category", "id": 90564},
                        {"entity": "category", "id": 4954975},
                    ],
                    "search": {
                        "total": 2,
                        "results": [
                            {"entity": "product", "id": 10, "categories": [{"id": 4954975}]},
                            {"entity": "product", "id": 9, "categories": [{"id": 90564}]},
                        ],
                    },
                },
                allow_different_len=False,
            )

    def test_default_category_append_to_little_history(self):
        """
        Если у пользователя короткая история, добираем дефолтных до минимального количества (когда делалось - было 5)
        """
        self.error_log.ignore()
        for split in ['crypta', 'market']:
            response = self.report.request_json(
                'place=categories_and_models_by_history&yandexuid=10003&reviews-only-with-comment=0&reviews-only-with-comment=0&rearr-factors=split={split}'.format(
                    split=split
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "categories": [
                        {"entity": "category", "id": 1010},
                        {"entity": "category", "id": 1020},
                        {"entity": "category", "id": 1030},
                        {"entity": "category", "id": 90564},
                        {"entity": "category", "id": 4954975},
                    ],
                    "search": {
                        "total": 5,
                        "results": [
                            {"entity": "product", "id": 1, "categories": [{"id": 1010}]},
                            {"entity": "product", "id": 2, "categories": [{"id": 1020}]},
                            {"entity": "product", "id": 3, "categories": [{"id": 1030}]},
                            {"entity": "product", "id": 10, "categories": [{"id": 4954975}]},
                            {"entity": "product", "id": 9, "categories": [{"id": 90564}]},
                        ],
                    },
                },
                allow_different_len=False,
            )

    def test_default_category_append_not_duplicate_history(self):
        """
        Если история пользователя дублируется с дефолтовыми, мы не дублируем категории в выдаче.
        """
        self.error_log.ignore()
        for split in ['crypta', 'market']:
            response = self.report.request_json(
                'place=categories_and_models_by_history&yandexuid=10004&reviews-only-with-comment=0&rearr-factors=split={split}'.format(
                    split=split
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "categories": [
                        {"entity": "category", "id": 1010},
                        {"entity": "category", "id": 90564},
                        {"entity": "category", "id": 4954975},
                    ],
                    "search": {
                        "total": 3,
                        "results": [
                            {"entity": "product", "id": 1, "categories": [{"id": 1010}]},
                            {"entity": "product", "id": 10, "categories": [{"id": 4954975}]},
                            {"entity": "product", "id": 9, "categories": [{"id": 90564}]},
                        ],
                    },
                },
                allow_different_len=False,
            )

    def test_default_append_limit(self):
        """
        Если история есть, но мало - проверяем что не насуем дефолтовых больше, чем требуется.
        """
        self.error_log.ignore()
        for split in ['crypta', 'market']:
            response = self.report.request_json(
                'place=categories_and_models_by_history&yandexuid=10005&reviews-only-with-comment=0&rearr-factors=split={split}'.format(
                    split=split
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "categories": [
                        {"entity": "category", "id": 1010},
                        {"entity": "category", "id": 1020},
                        {"entity": "category", "id": 1030},
                        {"entity": "category", "id": 1040},
                        {"entity": "category"},
                    ],
                    "search": {
                        "total": 5,
                        "results": [
                            {"entity": "product", "id": 1, "categories": [{"id": 1010}]},
                            {"entity": "product", "id": 2, "categories": [{"id": 1020}]},
                            {"entity": "product", "id": 3, "categories": [{"id": 1030}]},
                            {"entity": "product", "id": 4, "categories": [{"id": 1040}]},
                            {"entity": "product", "categories": [{}]},
                        ],
                    },
                },
                allow_different_len=False,
            )

    def test_only_with_reviews(self):
        """Отзывы есть только у моделей 1, 4 и 6. При наличии categories-only-with-reviews=1
        не должны показываться категории, в которых нет ни одной модели с отзывами.
        """
        for split in ['crypta', 'market']:
            response = self.report.request_json(
                'place=categories_and_models_by_history&yandexuid=10001&categories-only-with-reviews=1&reviews-only-with-comment=0&rearr-factors=split={split}'.format(
                    split=split
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "categories": [
                        {"entity": "category", "id": 1010},
                        {"entity": "category", "id": 1040},
                        {"entity": "category", "id": 1060},
                    ],
                    "search": {
                        "total": 10,
                        "results": [
                            {"entity": "product", "id": 1, "categories": [{"id": 1010}]},
                            {"entity": "product", "id": 2, "categories": [{"id": 1020}]},
                            {"entity": "product", "id": 3, "categories": [{"id": 1030}]},
                            {"entity": "product", "id": 4, "categories": [{"id": 1040}]},
                            {"entity": "product", "id": 5, "categories": [{"id": 1050}]},
                            {"entity": "product", "id": 6, "categories": [{"id": 1060}]},
                            {"entity": "product", "id": 7, "categories": [{"id": 1070}]},
                            {"entity": "product", "id": 8, "categories": [{"id": 1080}]},
                            {"entity": "product", "id": 9, "categories": [{"id": 90564}]},
                            {"entity": "product", "id": 10, "categories": [{"id": 4954975}]},
                        ],
                    },
                },
                allow_different_len=False,
            )

    def test_reviews_with_comment(self):
        """Без reviews-only-with-comment=0 должен показываться отзыв, у которого или заполненно поле Comment,
        или поле Pro длиннее 80 символов.
        """
        req = 'place=categories_and_models_by_history&yandexuid=10001&rearr-factors=split=market'
        # всего 10 моделей
        response = self.report.request_json(req + '&reviews-only-with-comment=0')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalModels": 10,
                }
            },
            allow_different_len=False,
        )

        # а с подходящими комментариями всего 2: у модели 1 есть комментарий,
        # у модели 6 нет комментария, но есть длинный список достоинств
        response = self.report.request_json(req)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalModels": 2,
                    "results": [
                        {"entity": "product", "id": 1},
                        {"entity": "product", "id": 6},
                    ],
                }
            },
            allow_different_len=False,
        )

    def test_default_categories_shuffling(self):
        self.error_log.ignore()
        response = self.report.request_json(
            'place=categories_and_models_by_history&yandexuid=10002&no-random=1&rearr-factors=split=crypta&debug=1'
        )
        self.assertFragmentIn(response, "default categories not shuffled")

        response = self.report.request_json(
            'place=categories_and_models_by_history&yandexuid=10002&rearr-factors=split=crypta&debug=1'
        )
        self.assertFragmentIn(response, "default categories shuffled")

        response = self.report.request_json(
            'place=categories_and_models_by_history&yandexuid=10002&no-random=0&rearr-factors=split=crypta&debug=1'
        )
        self.assertFragmentIn(response, "default categories shuffled")

    def test_forced_pruning(self):
        """
        Проверяем, что эксперимент market_categories_and_models_by_history_prun_count понижает pruning,
        но количество найденных документов остается прежним
        """
        for split in ['crypta', 'market']:
            response = self.report.request_json(
                'place=categories_and_models_by_history&yandexuid=10001&reviews-only-with-comment=0&'
                'rearr-factors=split={split};market_categories_and_models_by_history_prun_count=5000&debug=da'.format(
                    split=split
                )
            )

            self.assertFragmentIn(response, {"search": {"total": 10}})

            self.assertFragmentIn(response, {'pron': ['pruncount3334']})

            self.assertFragmentNotIn(response, {'pron': ['pruncount66667']})

            response = self.report.request_json(
                'place=categories_and_models_by_history&yandexuid=10001&reviews-only-with-comment=0&'
                'rearr-factors=split={split}&debug=da'.format(split=split)
            )

            self.assertFragmentIn(response, {"search": {"total": 10}})

            self.assertFragmentIn(response, {'pron': ['pruncount66667']})

            self.assertFragmentNotIn(response, {'pron': ['pruncount3334']})


if __name__ == '__main__':
    main()
