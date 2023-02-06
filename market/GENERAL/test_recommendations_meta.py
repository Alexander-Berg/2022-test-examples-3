#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, Offer, YamarecFeaturePartition, YamarecPlace, YamarecSettingPartition
from core.testcase import TestCase, main
from core.bigb import WeightedValue, BigBKeyword
from core.dj import DjModel

DEFAULT_PROFILE = [
    BigBKeyword(
        id=BigBKeyword.GENDER,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.GENDER_MALE, weight=621947),
            WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=375515),
        ],
    ),
]


def _make_yamarec_partition(formula_id, splits, features=None):
    names = keys = ['model_id', 'category_id']
    f = [[101, 201]] if features is None else features
    return YamarecFeaturePartition(
        feature_names=names, feature_keys=keys, features=f, formula_id=formula_id, splits=splits
    )


class T(TestCase):
    @classmethod
    def _reg_ichwill_request(cls, user_id, models, item_count=40):
        cls.recommender.on_request_models_of_interest(
            user_id=user_id, item_count=item_count, with_timestamps=True, version=4
        ).respond({'models': map(str, models), 'timestamps': map(str, list(range(len(models), 0, -1)))})
        cls.bigb.on_request(yandexuid=user_id.replace('yandexuid:', ''), client='merch-machine').respond(counters=[])

    @classmethod
    def prepare_recommendations_meta(cls):
        """ """
        for seq in range(12):
            cls.index.models += [
                Model(hyperid=1888200 + seq, title='model' + str(seq), hid=1888200),
            ]
            cls.index.offers += [
                Offer(hyperid=1888200 + seq, price=100 + seq * 10),
                Offer(hyperid=1888200 + seq, price=2000 + seq * 10),
            ]
        cls.bigb.on_request(yandexuid='001', client='merch-machine').respond(keywords=DEFAULT_PROFILE)

        cls._reg_ichwill_request("yandexuid:001", [1888202, 1888201, 1888202, 1888203, 1888209])
        dj_models = [DjModel(id=modelid) for modelid in [1888202, 1888201, 1888202, 1888203, 1888209]]
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid='001').respond(dj_models)
        cls.recommender.on_request_we_have_cheaper(user_id='yandexuid:001', item_count=100,).respond(
            {
                "we_have_cheaper": [
                    {"model_id": 1888201, "price": 2500.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 1888202, "price": 3500.0, "success_requests_share": 0.8, "timestamp": "1495206745"},
                    {"model_id": 1888203, "price": 4100.0, "success_requests_share": 0.6, "timestamp": "1495206745"},
                    {"model_id": 1888204, "price": 5500.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 1888209, "price": 4100.0, "success_requests_share": 0.6, "timestamp": "1495206745"},
                    {"model_id": 1888210, "price": 5500.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                ]
            }
        )
        cls.index.yamarec_places += [
            YamarecPlace(
                name="better-price",
                kind=YamarecPlace.Type.SETTING,
                partitions=[
                    # явно все параметры
                    YamarecSettingPartition(
                        params={
                            "show-history": "1",
                            "filter-by-price": "0",
                            "filter-by-assistant": "0",
                        },
                        splits=['001'],
                    ),
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.CATEGORY_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=['category_id', 'position'],
                        feature_keys=['category_id'],
                        features=[
                            [1888200, 1],
                            [1888210, 2],
                        ],
                        splits=['001'],
                    ),
                ],
            ),
        ]

    def test_recommendations_meta(self):
        """Запрашиваем по 2 модели на плейсах products_by_history и better_price,.
        Дедеупликацию при этом запрашиваем только для первых двух
        моделей на каждом плейсе
        Проверяем, что первые две модели всех плейсов уникальны, и плейс
        products_by_history имеет приоритет при дедупликации
        """

        response = self.report.request_json(
            '&place=recommendations_meta&recommendation-places=products_by_history:2:3;better_price:2:3&yandexuid=001'
        )
        self.assertFragmentIn(
            response,
            {
                "places": [
                    {
                        "place": "products_by_history",
                        "results": [
                            {"entity": "product", "id": 1888202},
                            {"entity": "product", "id": 1888201},
                            {"entity": "product", "id": 1888203},
                        ],
                    },
                    {
                        "place": "better_price",
                        "results": [
                            {"entity": "product", "id": 1888203},
                            {"entity": "product", "id": 1888204},
                            {"entity": "product", "id": 1888209},
                        ],
                    },
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
