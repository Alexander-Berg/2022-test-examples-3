#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, Offer, RegionalModel, Shop, YamarecFeaturePartition, YamarecPlace
from core.testcase import TestCase, main
from core.matcher import ElementCount


class T(TestCase):
    """
    Набор тестов для place=categories_by_history
    """

    @classmethod
    def prepare(cls):
        """
        Yamarec-конфигурация и настройки внешних сервисов для всех тестов
        а также некоторые общие данные в индекс
        """

        # index
        model_ids = list(range(1, 11))
        hids = [1000 + 10 * hyperid for hyperid in model_ids]
        hid_map = dict(zip(model_ids, hids))
        random_ts = [9, 10, 4, 1, 2, 5, 7, 3, 6, 8]
        ts_map = dict(zip(model_ids, random_ts))
        cls.index.models += [Model(hyperid=hyperid, ts=ts_map[hyperid], hid=hid_map[hyperid]) for hyperid in model_ids]
        # external services configs
        cls.recommender.on_request_viewed_models(user_id="yandexuid:10001").respond({"models": map(str, model_ids)})
        crypta_response = map(lambda model: {'id': model}, model_ids)
        cls.crypta.on_request_models('10001').respond(crypta_response)

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

    def test_empty_config(self):
        response = self.report.request_json('place=categories_by_history')
        self.assertFragmentIn(response, {"total": 0})
        self.error_log.expect('Place 6 config is not available for user').once()

    def test_total_renderable(self):
        """
        Проверка total_renderable = total:
        При неизменных существенных параметрах (независимо от размера страницы),
        поле total в выдаче и total_renderable в логе равны
        """

        response = self.report.request_json('place=categories_by_history&yandexuid=10001&rearr-factors=split=market')
        self.assertFragmentIn(response, {"total": 10, "results": ElementCount(10)})

        self.access_log.expect(total_renderable='10').times(1)

    def test_ordering(self):
        """
        Выдача должна быть в порядке, соответствующем истории
        В индексе модели лежат в порядке, отличном от истории
        """
        for split in ['crypta', 'market']:
            response = self.report.request_json(
                'place=categories_by_history&yandexuid=10001&rearr-factors=split={split}'.format(split=split)
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {"entity": "category", "id": 1010},
                        {"entity": "category", "id": 1020},
                        {"entity": "category", "id": 1030},
                        {"entity": "category", "id": 1040},
                        {"entity": "category", "id": 1050},
                        {"entity": "category", "id": 1060},
                        {"entity": "category", "id": 1070},
                        {"entity": "category", "id": 1080},
                        {"entity": "category", "id": 1090},
                        {"entity": "category", "id": 1100},
                    ]
                },
                preserve_order=True,
            )

    @classmethod
    def prepare_duplicates(cls):
        """
        Конфигурация внешних сервисов для выдачи с повторениями
        Плюс данные для кейса нескольких моделей в одной категории
        """
        cls.index.models += [
            Model(hyperid=11, hid=1010),
            Model(hyperid=12, hid=1010),
        ]
        # external services configs
        model_ids = [1, 1, 1, 2, 2, 3, 3, 11, 12]
        cls.recommender.on_request_viewed_models(user_id='yandexuid:10002').respond({'models': map(str, model_ids)})
        crypta_response = map(lambda model: {'id': model}, model_ids)
        cls.crypta.on_request_models('10002').respond(crypta_response)

    def test_duplicates(self):
        """
        Проверка отсутствия дубликатов в выдаче
        """
        for split in ['crypta', 'market', 'crypta_market', 'market_crypta', 'crypta_crypta', 'market_market']:
            response = self.report.request_json(
                "place=categories_by_history&yandexuid=10002&rearr-factors=split={split}".format(split=split)
            )
            self.assertFragmentIn(
                response,
                {
                    "total": 3,
                    "results": [
                        {"entity": "category", "id": 1010},
                        {"entity": "category", "id": 1020},
                        {"entity": "category", "id": 1030},
                    ],
                },
                preserve_order=True,
            )

    """
    Тесты, проверяющие соответствие алгоритмов ранжирования
    products_by_history и categories_by_history
    содержат проверки, похожие на test_products_by_history.py,
    чтоб любые расхождения не остались незамеченными
    """

    @classmethod
    def _reg_ichwill_request(cls, user_id, models, item_count=40):
        cls.recommender.on_request_models_of_interest(
            user_id=user_id, item_count=item_count, with_timestamps=True, version=4
        ).respond({'models': map(str, models), 'timestamps': map(str, list(range(len(models), 0, -1)))})
        cls.bigb.on_request(yandexuid=user_id.replace('yandexuid:', ''), client='merch-machine').respond(counters=[])

    @classmethod
    def prepare_ichwill_uid_priority(cls):
        # index
        # model-analogs for products_by_history
        cls.index.models += [
            Model(hyperid=21, hid=1010),
            Model(hyperid=22, hid=1010),
            Model(hyperid=23, hid=1010),
            Model(hyperid=24, hid=1020),
            Model(hyperid=25, hid=1020),
            Model(hyperid=26, hid=1020),
            Model(hyperid=27, hid=1030),
            Model(hyperid=28, hid=1030),
            Model(hyperid=29, hid=1030),
        ]
        cls.index.shops += [
            Shop(fesh=1, regions=[1001]),
        ]
        cls.index.offers += [Offer(hyperid=hyperid, fesh=1) for hyperid in list(range(1, 11)) + list(range(21, 30))]

        cls.index.regional_models += [
            RegionalModel(hyperid=hyperid, offers=11, rids=[1001])
            for hyperid in list(range(1, 11)) + list(range(21, 30))
        ]

        # responses of external services
        cls._reg_ichwill_request("yandexuid:10011", [1, 21, 22, 23])
        cls._reg_ichwill_request("puid:50011", [2, 24, 25, 26])
        cls._reg_ichwill_request("uuid:60011", [3, 27, 28, 29])
        cls.recommender.on_request_viewed_models(user_id="yandexuid:10011").respond({"models": ["1"]})
        cls.recommender.on_request_viewed_models(user_id="puid:50011").respond({"models": ["2"]})
        cls.recommender.on_request_viewed_models(user_id="uuid:60011").respond({"models": ["3"]})

    def test_ichwill_uid_priority(self):
        """
        Проверка приоритета puid при определении юзера
        """
        models_response = self.report.request_json(
            'place=products_by_history&rids=1001&yandexuid=10011&puid=50011&uuid=60011'
            '&rearr-factors=market_disable_dj_for_recent_findings%3D1'
        )
        self.assertFragmentIn(
            models_response,
            {
                "total": 4,
                "results": [
                    {"categories": [{"entity": "category", "id": 1020}]},
                ]
                * 4,
            },
        )

        cats_response = self.report.request_json(
            'place=categories_by_history&yandexuid=10011&puid=50011&uuid=60011&rearr-factors=split=market'
        )
        self.assertFragmentIn(cats_response, {"total": 1, "results": [{"entity": "category", "id": 1020}]})


if __name__ == '__main__':
    main()
