#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    HyperCategory,
    HyperCategoryType,
    Model,
    Offer,
    RegionalModel,
    YamarecCategoryBanList,
    YamarecFeaturePartition,
    YamarecPlace,
)
from core.testcase import TestCase, main
from core.matcher import ElementCount, Wildcard

from core.report import DefaultFlags


class T(TestCase):
    @classmethod
    def prepare(cls):
        """
        Id values:
            hyperid: [101:108]
            hid: [201:206]
            fesh: [301:302]
        """

        # data for best_deals
        cls.index.models += [
            Model(hyperid=101, title='model_title1', hid=201, model_clicks=100),
            Model(hyperid=102, title='model_title2', hid=202, model_clicks=100),
            Model(hyperid=103, title='model_title3', hid=203, model_clicks=100),
            Model(hyperid=104, title='model_title4', hid=204, model_clicks=100),
            Model(hyperid=105, title='model_title5', hid=205, model_clicks=100),
            Model(hyperid=106, title='model_title6', hid=206, model_clicks=100),
            Model(hyperid=107, title='model_title7', hid=201, model_clicks=50),
            Model(hyperid=108, title='model_title8', hid=202, model_clicks=50),
            Model(hyperid=109, title='model_title9', hid=204, model_clicks=50),
            Model(hyperid=110, title='model_title10', hid=206, model_clicks=50),
            Model(hyperid=111, title='model_title11', hid=201, model_clicks=10),
            Model(hyperid=112, title='model_title12', hid=206, model_clicks=10),
            Model(hyperid=121, title='model_title21', hid=221, model_clicks=10),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=101, offers=100),
            RegionalModel(hyperid=102, offers=100),
            RegionalModel(hyperid=103, offers=100),
            RegionalModel(hyperid=104, offers=100),
            RegionalModel(hyperid=105, offers=100),
            RegionalModel(hyperid=106, offers=100),
            RegionalModel(hyperid=107, offers=50),
            RegionalModel(hyperid=108, offers=50),
            RegionalModel(hyperid=109, offers=50),
            RegionalModel(hyperid=110, offers=50),
            RegionalModel(hyperid=111, offers=10),
            RegionalModel(hyperid=112, offers=10),
            RegionalModel(hyperid=121, offers=0),
        ]

        cls.index.offers += [Offer(hyperid=hyperid) for hyperid in range(101, 113)]

        cls.index.hypertree += [
            HyperCategory(hid=201, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=202, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=203, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=204, output_type=HyperCategoryType.GURU),
            HyperCategory(
                hid=207,
                children=[
                    HyperCategory(hid=205, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=206, output_type=HyperCategoryType.GURU),
                ],
            ),
            HyperCategory(hid=221, output_type=HyperCategoryType.GURU),
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.CATEGORY_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=['category_id', 'position'],
                        feature_keys=['category_id'],
                        features=[
                            [201, 1],
                            [202, 2],
                            [203, 3],
                            [204, 4],
                            [205, 5],
                            [206, 6],
                        ],
                        splits=['1'],
                    ),
                    YamarecFeaturePartition(
                        feature_names=['category_id', 'position'],
                        feature_keys=['category_id'],
                        features=[
                            [201, 1],
                            [202, 2],
                            [203, 3],
                        ],
                        splits=['2'],
                    ),
                    YamarecFeaturePartition(
                        feature_names=['category_id', 'position'],
                        feature_keys=['category_id'],
                        features=[
                            [204, 1],
                            [205, 2],
                            [206, 3],
                        ],
                        splits=['3'],
                    ),
                    YamarecFeaturePartition(
                        feature_names=['category_id', 'position'],
                        feature_keys=['category_id'],
                        features=[
                            [221, 1],
                            [201, 2],
                        ],
                        splits=['9'],
                    ),
                ],
            )
        ]
        cls.recommender.on_request_models_of_interest(user_id="yandexuid:001", item_count=1000).respond(
            {"models": map(str, list(range(101, 107)))}
        )
        cls.recommender.on_request_models_of_interest(user_id="yandexuid:002", item_count=1000).respond(
            {"models": map(str, list(range(101, 104)))}
        )
        cls.recommender.on_request_models_of_interest(user_id="yandexuid:003", item_count=1000).respond(
            {"models": map(str, list(range(104, 107)))}
        )
        cls.recommender.on_request_models_of_interest(user_id="yandexuid:004", item_count=1000).respond({"models": []})

    @staticmethod
    def get_category_fragment(id):
        return {"entity": "category", "id": id}

    def test_config(self):
        response = self.report.request_json('place=personalcategorymodels&yandexuid=004&debug=1')
        self.error_log.expect('Personal category config is not available for user 004.').once()
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "report": {
                        "logicTrace": [Wildcard('*Looking for * largest categories matching hid filter (fallback)')]
                    }
                }
            },
        )

        request = 'place=personalcategorymodels&yandexuid=001'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"total": 9}, preserve_order=True)

        self.assertFragmentIn(response, T.get_category_fragment(id=201), preserve_order=True)
        self.assertFragmentIn(response, T.get_category_fragment(id=202), preserve_order=True)
        self.assertFragmentIn(response, T.get_category_fragment(id=203), preserve_order=True)
        self.assertFragmentIn(response, T.get_category_fragment(id=204), preserve_order=True)
        self.assertFragmentIn(response, T.get_category_fragment(id=205), preserve_order=True)
        self.assertFragmentNotIn(response, T.get_category_fragment(id=206), preserve_order=True)
        """Проверяется, что общее количество для показа = total"""
        self.access_log.expect(total_renderable='9')

        response = self.report.request_json('place=personalcategorymodels&yandexuid=002')
        self.assertFragmentIn(response, T.get_category_fragment(id=201), preserve_order=True)
        self.assertFragmentIn(response, T.get_category_fragment(id=202), preserve_order=True)
        self.assertFragmentIn(response, T.get_category_fragment(id=203), preserve_order=True)
        self.assertFragmentNotIn(response, T.get_category_fragment(id=204), preserve_order=True)
        self.assertFragmentNotIn(response, T.get_category_fragment(id=205), preserve_order=True)
        self.assertFragmentNotIn(response, T.get_category_fragment(id=206), preserve_order=True)

        response = self.report.request_json('place=personalcategorymodels&yandexuid=003')
        self.assertFragmentNotIn(response, T.get_category_fragment(id=201), preserve_order=True)
        self.assertFragmentNotIn(response, T.get_category_fragment(id=202), preserve_order=True)
        self.assertFragmentNotIn(response, T.get_category_fragment(id=203), preserve_order=True)
        self.assertFragmentIn(response, T.get_category_fragment(id=204), preserve_order=True)
        self.assertFragmentIn(response, T.get_category_fragment(id=205), preserve_order=True)
        self.assertFragmentIn(response, T.get_category_fragment(id=206), preserve_order=True)
        """
        Проверка поля url_hash в show log
        """
        self.show_log_tskv.expect(url_hash=ElementCount(32))

    def test_sort(self):
        response = self.report.request_json('place=personalcategorymodels&yandexuid=001')
        self.assertFragmentIn(
            response,
            [
                {"type": "model", "id": 101},
                {"type": "model", "id": 102},
                {"type": "model", "id": 103},
                {"type": "model", "id": 104},
                {"type": "model", "id": 105},
                {"type": "model", "id": 107},
                {"type": "model", "id": 108},
                {"type": "model", "id": 109},
                {"type": "model", "id": 111},
            ],
            preserve_order=True,
        )

        response = self.report.request_json('place=personalcategorymodels&yandexuid=002')
        self.assertFragmentIn(
            response,
            [
                {"type": "model", "id": 101},
                {"type": "model", "id": 102},
                {"type": "model", "id": 103},
                {"type": "model", "id": 107},
                {"type": "model", "id": 108},
                {"type": "model", "id": 111},
            ],
            preserve_order=True,
        )

        response = self.report.request_json('place=personalcategorymodels&yandexuid=003')
        self.assertFragmentIn(
            response,
            [
                {"type": "model", "id": 104},
                {"type": "model", "id": 105},
                {"type": "model", "id": 106},
                {"type": "model", "id": 109},
                {"type": "model", "id": 110},
                {"type": "model", "id": 112},
            ],
            preserve_order=True,
        )

    def test_param(self):
        response = self.report.request_json('place=personalcategorymodels&yandexuid=001&numdoc=5')
        self.assertFragmentIn(response, {"total": 9}, preserve_order=True)

        response = self.report.request_json('place=personalcategorymodels&yandexuid=001&numdoc=20')
        self.assertFragmentIn(response, {"total": 9}, preserve_order=True)

        response = self.report.request_json('place=personalcategorymodels&yandexuid=001&hid=207')
        self.assertFragmentIn(response, {"total": 4}, preserve_order=True)
        self.assertFragmentIn(
            response,
            [
                {"type": "model", "id": 105},
                {"type": "model", "id": 106},
                {"type": "model", "id": 110},
                {"type": "model", "id": 112},
            ],
            preserve_order=True,
        )

    def test_missing_pp(self):
        response = self.report.request_json(
            'place=personalcategorymodels&yandexuid=001&ip=127.0.0.1', strict=False, add_defaults=DefaultFlags.BS_FORMAT
        )
        self.error_log.expect('Some client has not set PP value. Find and punish him violently').once()
        self.assertEqual(500, response.code)

    @classmethod
    def prepare_no_offers(cls):
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:009', with_timestamps=False).respond(
            {'models': []}
        )

    def test_no_offers(self):
        """
        Проверка учёта наличия оферов для модели:
        модель 221 входит в колдстарт-рекомендации по yamarec config (режим матчинга по фичам), но для неё нет оферов,
        поэтому она не должна проходить в выдачу
        """
        response = self.report.request_json("place=personalcategorymodels&yandexuid=009")
        self.assertFragmentNotIn(response, T.get_category_fragment(id=221))
        self.assertFragmentIn(response, T.get_category_fragment(id=201))

    @classmethod
    def prepare_new_personal_categories(cls):
        cls.index.hypertree += [
            HyperCategory(hid=777, output_type=HyperCategoryType.GURU),
        ]
        cls.index.models += [
            Model(hyperid=7777, hid=777),
        ]
        cls.index.offers += [
            Offer(hyperid=7777),
        ]
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:9999', with_timestamps=False).respond(
            {'models': ['7777']}
        )

    def test_new_personal_categories(self):
        response = self.report.request_json(
            'place=personalcategorymodels&yandexuid=9999&rearr-factors=market_use_recommender=1'
        )
        self.assertFragmentIn(
            response,
            [
                {'entity': 'product', 'id': 7777},
            ],
            preserve_order=True,
        )

    @classmethod
    def prepare_empty_personal_categories_fallback(cls):
        cls.index.models += [
            Model(hyperid=130, hid=91399),
            Model(hyperid=131, hid=231),
        ]
        cls.index.offers += [
            Offer(hyperid=130),
            Offer(hyperid=131),
        ]
        cls.index.hypertree += [
            HyperCategory(
                hid=230,
                children=[
                    HyperCategory(hid=91399, output_type=HyperCategoryType.GURU),  # Сигареты
                    HyperCategory(hid=231, output_type=HyperCategoryType.GURU),
                ],
            ),
        ]
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.CATEGORY_BAN_LIST,
                kind=YamarecPlace.Type.CATEGORY_BAN_LIST,
                partitions=[YamarecCategoryBanList(ban_list={91399: 'hard'})],  # Сигареты
            )
        ]

    def test_empty_personal_categories_fallback(self):
        """
        Если после применения фильтра по категории к персонализированному ответу
        результат оказался пустым, возвращаем категорию с наибольшим количеством офферов.
        """
        response = self.report.request_json('place=personalcategorymodels&yandexuid=9999&hid=207')
        self.assertFragmentIn(
            response,
            [
                {"type": "model", "id": 105},
                {"type": "model", "id": 106},
                {"type": "model", "id": 110},
                {"type": "model", "id": 112},
            ],
        )

        """
        Забаненные товары не должны попасть в выборку.
        """
        response = self.report.request_json('place=personalcategorymodels&yandexuid=9999&hid=230&debug=1')
        self.assertFragmentNotIn(response, T.get_category_fragment(id=91399))
        self.assertFragmentIn(response, T.get_category_fragment(id=231))
        self.assertFragmentIn(
            response, {"debug": {"report": {"logicTrace": [Wildcard('*Largest categories are 231')]}}}
        )


if __name__ == '__main__':
    main()
