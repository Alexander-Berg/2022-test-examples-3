#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    HyperCategory,
    HyperCategoryType,
    Model,
    NewShopRating,
    Offer,
    RegionalModel,
    Shop,
    YamarecFeaturePartition,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.testcase import TestCase, main
from core.matcher import EqualToOneOf
from core.bigb import WeightedValue, BigBKeyword
from core.dj import DjModel


class T(TestCase):
    @classmethod
    def _reg_ichwill_request(cls, user_id, models, item_count=40):
        cls.recommender.on_request_models_of_interest(
            user_id=user_id, item_count=item_count, with_timestamps=True, version=4
        ).respond({'models': map(str, models), 'timestamps': map(str, list(range(len(models), 0, -1)))})

    @classmethod
    def _reg_dj_request(cls, user_id, models):
        cls.dj.on_request(yandexuid=user_id).respond([DjModel(id=modelid) for modelid in models])

    @classmethod
    def prepare(cls):
        """
        Id values:
            hyperid: [100:499]
            hid: [1100:1499]
        """
        cls.settings.set_default_reqid = False

        # popular models
        popular_models_ids = list(range(100, 200))
        cls.index.models += [Model(hyperid=model_id, hid=model_id + 1000) for model_id in popular_models_ids]
        cls.index.regional_models += [
            RegionalModel(hyperid=model_id, offers=1, rids=[-1]) for model_id in popular_models_ids
        ]
        cls.index.offers += [Offer(hyperid=model_id) for model_id in popular_models_ids]
        for yandexuid in ('004', '1002'):
            cls.recommender.on_request_models_of_interest(user_id='yandexuid:{}'.format(yandexuid)).respond(
                {'models': []}
            )
            cls._reg_dj_request(yandexuid, [])
        for yandexuid in ('001', '1001'):
            cls.recommender.on_request_models_of_interest(user_id='yandexuid:{}'.format(yandexuid)).respond(
                {'models': map(str, popular_models_ids)}
            )
            cls._reg_dj_request(yandexuid, popular_models_ids)

        # recommendations by history models
        history_models_ids = list(range(300, 400))
        cls.index.models += [Model(hyperid=model_id, hid=model_id + 1000) for model_id in history_models_ids]
        cls.index.regional_models += [
            RegionalModel(hyperid=model_id, offers=1, rids=[-1]) for model_id in history_models_ids
        ]
        cls.index.offers += [Offer(hyperid=model_id) for model_id in history_models_ids]

        for yandexuid in ['004', '1002']:
            cls._reg_ichwill_request('yandexuid:{}'.format(yandexuid), [])
            cls._reg_dj_request(yandexuid, [])
        for yandexuid in ["001", "004", "1002"]:
            cls.recommender.on_request_we_have_cheaper(user_id="yandexuid:" + yandexuid, item_count=100).respond(
                {"we_have_cheaper": []}
            )
        response = '{{"models": [{}]}}'.format(",".join(['"{}"'.format(m) for m in history_models_ids]))
        cls.recommender.on_request_viewed_models(user_id='yandexuid:001').respond(response)
        cls.recommender.on_request_viewed_models(user_id='yandexuid:1001', item_count=40).respond(response)
        cls._reg_ichwill_request('yandexuid:001', history_models_ids)
        cls._reg_ichwill_request('yandexuid:1001', history_models_ids)
        cls._reg_dj_request('001', history_models_ids)
        cls._reg_dj_request('1001', history_models_ids)
        cls.crypta.on_request_profile(yandexuid='001').respond(features=[])
        cls.crypta.on_request_profile(yandexuid='1001').respond(features=[])
        cls.crypta.on_request_profile(yandexuid='004').respond(features=[])
        cls.crypta.on_request_profile(yandexuid='3001').respond(features=[])

        # bestdeals
        bestdeals_models_ids = list(range(400, 500))
        cls.index.models += [Model(hyperid=model_id, hid=model_id + 1000) for model_id in bestdeals_models_ids]
        cls.index.offers += [Offer(hyperid=model_id, discount=10) for model_id in bestdeals_models_ids]
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.CATEGORY_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        splits=['1', '2'],
                        feature_keys=['category_id'],
                        feature_names=['category_id', 'position'],
                        features=[],
                    ),
                ],
            )
        ]
        cls.index.hypertree += [
            HyperCategory(
                hid=90000,
                children=[
                    HyperCategory(hid=hyperid + 1000, output_type=HyperCategoryType.GURU) for hyperid in range(100, 500)
                ],
            ),
        ]

        for yandexuid in ['001', '004', '1001', '3001']:
            omm_options = {
                'yandexuid': yandexuid,
                'exp': 'white_attractive_models_personal_recommendations',
            }
            cls.bigb.on_request(yandexuid=yandexuid, client='merch-machine').respond(
                keywords=DEFAULT_PROFILE, counters=[]
            )
            cls.dj.on_request(**omm_options).respond(
                models=[
                    DjModel(id=111111, title='some model #1'),
                    DjModel(id=111112, title='some model #2'),
                    DjModel(id=111113, title='some model #3'),
                    DjModel(id=111114, title='some model #4'),
                ]
            )

        # default stuff
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ENDLESS_MAINPAGE,
                kind=YamarecPlace.Type.SETTING,
                partitions=[YamarecSettingPartition()],
            )
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ARTICLE_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=['article_id', 'category_id'],
                        feature_keys=['article_id', 'category_id'],
                        features=[[0, 1101]],
                    )
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.COLLECTION_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=['collection_id', 'category_id'],
                        feature_keys=['collection_id', 'category_id'],
                        features=[[0, 1101]],
                    )
                ],
            ),
        ]

    def test_config(self):
        response = self.report.request_json(
            'place=personal_recommendations&yandexuid=004&hid=90000&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'
        )
        self.assertFragmentIn(response, {"total": 0})
        self.error_log.expect('Personal category config is not available for user 004').once()

        response = self.report.request_json(
            'place=personal_recommendations&yandexuid=001&hid=90000&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'
        )
        self.assertFragmentIn(response, {"total": 30})
        self.assertFragmentIn(response, ["bestseller"])
        self.assertFragmentIn(response, ["suggested"])

        self.error_log.ignore("Cannot find suitable personal categories. Fallback to all categories")
        self.error_log.ignore("Category filtration is turned off and vendor filtration is turned off")

    def test_numdoc(self):
        response = self.report.request_json('place=personal_recommendations&yandexuid=001')
        self.assertFragmentIn(response, {"total": 30})

        response = self.report.request_json('place=personal_recommendations&yandexuid=001&numdoc=100')
        self.assertFragmentIn(response, {"total": 100})

        response = self.report.request_json('place=personal_recommendations&yandexuid=001&numdoc=10')
        self.assertFragmentIn(response, {"total": 10})

        response = self.report.request_json('place=personal_recommendations&yandexuid=001&numdoc=1')
        self.assertFragmentIn(response, {"total": 1})

        response = self.report.request_json('place=personal_recommendations&yandexuid=001&numdoc=0')
        self.assertFragmentIn(response, {"total": 0})

        self.error_log.ignore("Cannot find suitable personal categories. Fallback to all categories")
        self.error_log.ignore("Category filtration is turned off and vendor filtration is turned off")

        """Проверяется, что общее количество для показа = total"""
        self.access_log.expect(total_renderable='30')
        self.access_log.expect(total_renderable='100')
        self.access_log.expect(total_renderable='10')
        self.access_log.expect(total_renderable='1')
        self.access_log.expect(total_renderable='0')

    def test_proportions(self):
        response = self.report.request_json('place=personal_recommendations&yandexuid=001&numdoc=10')
        self.assertFragmentIn(response, [{"tags": ["bestseller"]}] * 3)
        self.assertFragmentIn(response, [{"tags": ["suggested"]}] * 3)

        response = self.report.request_json('place=personal_recommendations&yandexuid=001&numdoc=30')
        self.assertFragmentIn(response, [{"tags": ["bestseller"]}] * 8)
        self.assertFragmentIn(response, [{"tags": ["suggested"]}] * 10)

        self.error_log.ignore("Cannot find suitable personal categories. Fallback to all categories")
        self.error_log.ignore("Category filtration is turned off and vendor filtration is turned off")

    def test_sort(self):
        response = self.report.request_json(
            'place=personal_recommendations&yandexuid=001&hid=90000&numdoc=10'
            '&rearr-factors=market_disable_dj_for_recent_findings%3D1'
        )
        self.assertFragmentIn(
            response,
            [
                {"subEntity": {"id": 100}},
                {"subEntity": {"id": 101}},
                {"subEntity": {"id": 102}},
            ],
            preserve_order=True,
        )
        self.assertFragmentIn(
            response,
            [
                {"subEntity": {"id": 300}},
                {"subEntity": {"id": 304}},
                {"subEntity": {"id": 308}},
            ],
            preserve_order=True,
        )

        self.error_log.ignore("Cannot find suitable personal categories. Fallback to all categories")
        self.error_log.ignore("Category filtration is turned off and vendor filtration is turned off")

    def test_sort_dj(self):
        response = self.report.request_json('place=personal_recommendations&yandexuid=001&hid=90000&numdoc=10')
        self.assertFragmentIn(
            response,
            [
                {"subEntity": {"id": 100}},
                {"subEntity": {"id": 101}},
                {"subEntity": {"id": 102}},
            ],
            preserve_order=True,
        )
        self.error_log.ignore("Cannot find suitable personal categories. Fallback to all categories")
        self.error_log.ignore("Category filtration is turned off and vendor filtration is turned off")

    @classmethod
    def prepare_deduplicate_better_price(cls):
        """
        Конфигурация для непустой выдачи "У нас дешевле"
        """
        cls.index.yamarec_places += [
            YamarecPlace(
                name="better-price",
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            "show-history": "1",
                        },
                        splits=[{"split": "1"}],
                    ),
                    YamarecSettingPartition(params={}, splits=[{}]),
                ],
            ),
        ]

        cls.recommender.on_request_we_have_cheaper(user_id="yandexuid:1001", item_count=100).respond(
            {
                "we_have_cheaper": [
                    {"model_id": 100, "price": 600.0, "success_requests_share": 0.9, "timestamp": "1495206745"},
                ]
            }
        )

        Shop(fesh=99, priority_region=213, new_shop_rating=NewShopRating(new_rating_total=4.5), cpa=Shop.CPA_REAL),
        cls.index.offers += [
            Offer(
                waremd5="BH8EPLtKmdLQhLUasgaOnA",
                fesh=99,
                hyperid=100,
                price=500,
                bid=1,
                cpa=Offer.CPA_REAL,
                override_cpa_check=True,
            ),
        ]

    def test_deduplicate_better_price(self):
        """
        Проверяем, что отфильтровываются модели, попадающие в выдачу better_price.
        С помощью rearr-флага включаем better_price конфигурацию, дающую модель 100 и ожидаем,
        что из выдачи personal_recommendations эта модель исключается
        """

        response = self.report.request_json('place=personal_recommendations&yandexuid=001&numdoc=10')
        self.assertFragmentIn(
            response,
            [
                {"subEntity": {"id": 100}},
            ],
        )
        response = self.report.request_json(
            'place=personal_recommendations&yandexuid=1001&numdoc=10&rearr-factors=split=1&rearr-factors=products_by_history_with_bigb_and_sovetnik%3D0'
        )
        self.assertFragmentNotIn(
            response,
            [
                {"subEntity": {"id": 100}},
            ],
        )

        self.error_log.ignore("Cannot find suitable personal categories. Fallback to all categories")
        self.error_log.ignore("Category filtration is turned off and vendor filtration is turned off")

    @classmethod
    def prepare_omm(cls):
        models_ids = list(range(500, 600))
        cls.index.models += [Model(hyperid=model_id, hid=model_id + 1000) for model_id in models_ids]
        cls.index.offers += [Offer(hyperid=model_id) for model_id in models_ids]
        yandexuid = '1002'
        omm_options = {
            'yandexuid': yandexuid,
            'exp': 'white_attractive_models_personal_recommendations',
        }
        cls.bigb.on_request(yandexuid=yandexuid, client='merch-machine').respond(keywords=DEFAULT_PROFILE, counters=[])
        cls.dj.on_request(**omm_options).respond(
            models=[DjModel(id=hyperid, title='model #{}'.format(hyperid)) for hyperid in models_ids]
        )

    @classmethod
    def prepare_dj(cls):
        cls.dj.on_request(yandexuid='1002', exp='dj_personal_recommendation').respond(
            [DjModel(id=512, title='2 в 9й'), DjModel(id=555, title='три пятёрки')]
        )

    def test_dj(self):
        """
        Проверяем, что под флагом market_dj_exp_for_personal_recommendations плейс personal_recommendations ходит в dj
        и сохраняет на выдаче порядок моделей, полученных из dj-плейса
        """
        response = self.report.request_json(
            'place=personal_recommendations&yandexuid=1002&numdoc=10&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'
        )
        self.assertFragmentIn(
            response,
            [
                {'tags': ['attractive'], "subEntity": {"id": EqualToOneOf(*list(range(500, 600)))}},
            ],
        )
        self.error_log.ignore("Category filtration is turned off and vendor filtration is turned off")

        response = self.report.request_json(
            'place=personal_recommendations&yandexuid=1002&'
            'rearr-factors=market_dj_exp_for_personal_recommendations=dj_personal_recommendation;switch_popular_products_to_dj_no_nid_check=0'
        )
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "results": [
                    {'tags': ['attractive'], "subEntity": {"entity": "product", "id": 512}},
                    {'tags': ['attractive'], "subEntity": {"entity": "product", "id": 555}},
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.error_log.ignore("Category filtration is turned off and vendor filtration is turned off")

    @classmethod
    def prepare_null_better_price_models(cls):
        cls.recommender.on_request_we_have_cheaper(user_id="yandexuid:3001", item_count=100).respond(
            {
                "we_have_cheaper": [
                    {"model_id": 701, "price": 600.0, "success_requests_share": 0.9, "timestamp": "1495206745"},
                    {"model_id": None, "price": 600.0, "success_requests_share": 0.9, "timestamp": "1495206746"},
                ]
            }
        )

        cls.index.offers += [
            Offer(
                waremd5="CH8EPLtKmdLQhLUasgaOnA",
                fesh=99,
                hyperid=701,
                price=700,
                bid=1,
                cpa=Offer.CPA_REAL,
                override_cpa_check=True,
            ),
        ]

        cls.index.models += [
            Model(hyperid=701, hid=1201),
        ]

        cls._reg_ichwill_request('yandexuid:3001', (701,))
        cls._reg_dj_request('3001', [701])

        cls.recommender.on_request_models_of_interest(user_id='yandexuid:3001').respond({'models': map(str, (701,))})

        response = '{{"models": [{}]}}'.format(",".join(['"{}"'.format(m) for m in (701,)]))
        cls.recommender.on_request_viewed_models(user_id='yandexuid:3001', item_count=40).respond(response)
        cls.recommender.on_request_viewed_models(user_id='yandexuid:3001', item_count=100).respond(response)

        cls.index.yamarec_places += [
            YamarecPlace(
                name="better-price",
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            "show-history": "1",
                            "filter-by-price": "1",
                        },
                        splits=[{"split": "1"}],
                    )
                ],
            ),
        ]

    def test_null_better_price_models(self):
        """
        Проверяем, что при возврате null-моделей ручкой we_have_cheaper, нулевые айдишники не просачиваются
        на базовый поиск
        """
        response = self.report.request_json(
            'place=personal_recommendations&yandexuid=3001&debug=1&numdoc=20&rearr-factors=split=1&rearr-factors=products_by_history_with_bigb_and_sovetnik%3D0'
        )

        self.assertFragmentNotIn(
            response,
            {'reqwizardText': 'hyper_id:"0"'},
        )

        self.error_log.ignore("Cannot find suitable personal categories. Fallback to all categories")
        self.error_log.ignore("Category filtration is turned off and vendor filtration is turned off")


DEFAULT_PROFILE = [
    BigBKeyword(
        id=BigBKeyword.GENDER,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.GENDER_MALE, weight=621947),
            WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=375515),
        ],
    ),
]

if __name__ == '__main__':
    main()
