#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    HyperCategory,
    HyperCategoryType,
    Model,
    YamarecFeaturePartition,
    YamarecMatchingPartition,
    YamarecPlace,
)
from core.testcase import TestCase, main
from core.matcher import ElementCount, Contains, Not
from core.bigb import WeightedValue, BigBKeyword
import random
import itertools


class T(TestCase):
    """
    Набор тестов для персональных категорий синего маркета
    """

    @classmethod
    def prepare(cls):
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ACCESSORY_CATEGORIES,
                kind=YamarecPlace.Type.MATCHING,
                partitions=[
                    YamarecMatchingPartition(matching=ACCESSORY_CATEGORIES, splits=['*']),
                ],
            )
        ]

        # categories with accessories
        cls.index.hypertree += [
            HyperCategory(
                hid=90853,
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(hid=1001393, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=6203660, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=1001410, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=15561854, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=1001664, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=90941, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=91003, output_type=HyperCategoryType.GURU),
                ],
            ),
        ]

        # categories without accessories
        cls.index.hypertree += [
            HyperCategory(
                hid=9000,
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(hid=9001, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=9002, output_type=HyperCategoryType.CLUSTERS),
                    HyperCategory(
                        hid=9003,
                        output_type=HyperCategoryType.GURU,
                        children=[
                            HyperCategory(hid=9004, output_type=HyperCategoryType.GURU),
                            HyperCategory(hid=9005, output_type=HyperCategoryType.GURU),
                            HyperCategory(hid=9006, output_type=HyperCategoryType.CLUSTERS),
                        ],
                    ),
                ],
            ),
        ]
        cls.index.models += [
            Model(hyperid=1, hid=9001),
            Model(hyperid=2, hid=9002),
            Model(hyperid=3, hid=9003),
            Model(hyperid=4, hid=9004),
            Model(hyperid=5, hid=9005),
            Model(hyperid=6, hid=9006),
        ]

        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1001').respond({'models': ['2']})

        random.seed(0)

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.CATEGORY_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    # no config for split=no-config,
                    # some data
                    YamarecFeaturePartition(
                        feature_keys=['category_id'],
                        feature_names=['category_id', CATEGORY_FEATURE_MALE, 'feature1'],
                        features=[
                            [9001, 0.0, 50000.0],
                            [9002, 0.0, 50000.0],
                        ],
                        splits=[{'split': 'noisy_features'}],
                    ),
                    # some data
                    YamarecFeaturePartition(
                        feature_keys=['category_id'],
                        feature_names=['category_id', CATEGORY_FEATURE_MALE],
                        features=[
                            [9001, 0.0],
                            [9002, 0.0],
                        ],
                        splits=[{'split': '1'}],
                    ),
                    # test_history_data
                    YamarecFeaturePartition(
                        feature_keys=['category_id'],
                        feature_names=['category_id', CATEGORY_FEATURE_MALE],
                        features=[
                            [1001393, 0.0],  # category 0
                            [6203660, 0.0],  # accessory for category 0
                            [1001410, 0.0],  # category 1
                            [15561854, 0.0],  # accessory for category 1
                            [1001664, 0.0],  # category 2
                            [90853, 0.0],  # accessory for category 2
                        ],
                        splits=[{'split': 'history_data'}],
                    ),
                    # categories with null user feature values
                    YamarecFeaturePartition(
                        feature_keys=['category_id'],
                        feature_names=['category_id'] + CATEGORY_USER_FEATURES,
                        features=[
                            [9001] + [0.0] * len(CATEGORY_USER_FEATURES),
                            [9002] + [0.0] * len(CATEGORY_USER_FEATURES),
                            [9003] + [0.0] * len(CATEGORY_USER_FEATURES),
                            [9004] + [0.0] * len(CATEGORY_USER_FEATURES),
                        ],
                        splits=[{'split': 'null_features'}],
                    ),
                    # categories with random user feature values
                    YamarecFeaturePartition(
                        feature_keys=['category_id'],
                        feature_names=['category_id'] + CATEGORY_USER_FEATURES,
                        features=[
                            [9001] + [random.uniform(1, 10 ^ 6) for i in range(len(CATEGORY_USER_FEATURES))],
                            [9002] + [random.uniform(1, 10 ^ 6) for i in range(len(CATEGORY_USER_FEATURES))],
                            [9003] + [random.uniform(1, 10 ^ 6) for i in range(len(CATEGORY_USER_FEATURES))],
                            [9004] + [random.uniform(1, 10 ^ 6) for i in range(len(CATEGORY_USER_FEATURES))],
                        ],
                        splits=[{'split': 'random_features'}],
                    ),
                    # categories with equal user feature values
                    YamarecFeaturePartition(
                        feature_keys=['category_id'],
                        feature_names=['category_id'] + CATEGORY_USER_FEATURES,
                        features=[
                            [9006] + [555 + i for i in range(len(CATEGORY_USER_FEATURES))],
                            [9001] + [555 + i for i in range(len(CATEGORY_USER_FEATURES))],
                            [9003] + [555 + i for i in range(len(CATEGORY_USER_FEATURES))],
                            [9002] + [555 + i for i in range(len(CATEGORY_USER_FEATURES))],
                            [9004] + [555 + i for i in range(len(CATEGORY_USER_FEATURES))],
                        ],
                        splits=[{'split': 'equal_features'}],
                    ),
                    # missing of user features
                    YamarecFeaturePartition(
                        feature_keys=['category_id'],
                        feature_names=['category_id', CATEGORY_FEATURE_MALE],
                        features=[
                            [9006, 500000.0],
                            [9004, 500000.0],
                        ],
                        splits=[{'split': 'missing_features'}],
                    ),
                    # equal user feature values except one feature
                    YamarecFeaturePartition(
                        feature_keys=['category_id'],
                        feature_names=['category_id'] + CATEGORY_USER_FEATURES,
                        features=[
                            [9001, 700000.0, 300000.0, 2, 2, 2, 2, 2, 3, 3, 3],
                            [9002, 0.0, 1000000.0, 2, 2, 2, 2, 2, 3, 3, 3],
                            [9003, 500000.0, 500000.0, 2, 2, 2, 2, 2, 3, 3, 3],
                            [9004, 1000000.0, 0.0, 2, 2, 2, 2, 2, 3, 3, 3],
                        ],
                        splits=[{'split': 'rank_1_features'}],
                    ),
                    # different user feature vectors equally similar to vector with uniform buckets
                    #   like UserProfile[0.25, 0.25, 0.25, 0.25]
                    YamarecFeaturePartition(
                        feature_keys=['category_id'],
                        feature_names=['category_id']
                        + [
                            CATEGORY_FEATURE_MALE,
                            CATEGORY_FEATURE_FEMALE,
                            CATEGORY_FEATURE_INCOME_A,
                            CATEGORY_FEATURE_INCOME_B,
                        ],
                        features=[
                            [9003, 0, 1, 0, 1],
                            [9002, 1, 0, 1, 0],
                        ],
                        splits=[{'split': 'orthogonal_features'}],
                    ),
                    # case with features equally similar to UserProfile[0.5, 0.5, 0.5, 0.5],
                    #   extended with other features leading to specific ordering
                    YamarecFeaturePartition(
                        feature_keys=['category_id'],
                        feature_names=['category_id']
                        + [
                            CATEGORY_FEATURE_MALE,
                            CATEGORY_FEATURE_FEMALE,
                            CATEGORY_FEATURE_AGE_0_17,
                            CATEGORY_FEATURE_AGE_18_24,
                            CATEGORY_FEATURE_INCOME_A,
                            CATEGORY_FEATURE_INCOME_B,
                        ],
                        features=[
                            [9002, 0, 1, 0.0, 100000.0, 200000.0, 800000.0],
                            [9003, 1, 0, 100000.0, 0.0, 500000.0, 500000.0],
                        ],
                        splits=[{'split': 'orthogonal_and_ranging_features'}],
                    ),
                    # simple case
                    YamarecFeaturePartition(
                        feature_keys=['category_id'],
                        feature_names=['category_id'] + CATEGORY_USER_FEATURES,
                        features=[
                            [9991] + [1 for i in range(len(CATEGORY_USER_FEATURES))],
                            [9992] + [1 for i in range(len(CATEGORY_USER_FEATURES))],
                            [9993] + [1 for i in range(len(CATEGORY_USER_FEATURES))],
                        ],
                        splits=[{'split': 'simple'}],
                    ),
                ],
            ),
        ]
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:').respond({'models': []})

    def test_no_config(self):
        """
        Случай, когда нет фичей категорий для сплита конфигурации
        Проверяем работу без ошибок

        1. Нет истории пользователя, ожидаем пустую выдачу
        """
        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                'place=personal_categories&{}&rearr-factors=split=no-config&numdoc=10'.format(param)
            )
            self.assertFragmentIn(response, {'search': {'total': 0, 'results': ElementCount(0)}})
            self.error_log.ignore('Request to ICHWILL_MODELS_OF_INTEREST failed')
        self.error_log.expect('Personal category config is not available for user .').times(2)

        """
        2. То же самое, но история есть
        """
        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                'place=personal_categories&{}&rearr-factors=split=no-config&yandexuid=1001&numdoc=10'.format(param)
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 1,
                        'results': [
                            {'link': {'params': {'hid': '9002'}}},
                        ],
                    }
                },
            )
        self.error_log.expect('Personal category config is not available for user 1001').times(2)

    def test_noisy_features(self):
        """
        Случай, когда присутствуют не только целевые фичи.
        Проверяем работу отбора фич и сравнения по профилю без ошибок
        """
        for param in ['rgb=blue', 'cpa=real']:
            _ = self.report.request_json(
                'place=personal_categories&{}&rearr-factors=split=noisy_features&hid=9003&numdoc=10'.format(param)
            )
            self.error_log.ignore('Request to ICHWILL_MODELS_OF_INTEREST failed')

    def test_mismatch(self):
        """
        Случай, когда запрошенные категории не соответствуют категориям из истории и конфгурации
        Проверяем работу без ошибок

        1. Нет истории пользователя, ожидаем пустую выдачу
        """
        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                'place=personal_categories&{}&rearr-factors=split=1&hid=9003&numdoc=10'.format(param)
            )
            self.assertFragmentIn(response, {'search': {'total': 0, 'results': ElementCount(0)}})
            self.error_log.ignore('Request to ICHWILL_MODELS_OF_INTEREST failed')
        """
        2. То же самое, но история есть
        """
        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                'place=personal_categories&{}&rearr-factors=split=1&hid=9003&yandexuid=1001&numdoc=10'.format(param)
            )
            self.assertFragmentIn(response, {'search': {'total': 0, 'results': ElementCount(0)}})
            self.error_log.ignore('BigB: can not get profile for user yandexUid=1001')
            self.error_log.ignore('Request to BIGB_PROFILE failed')

    @classmethod
    def prepare_history_data(cls):
        """
        Модели из истории из нескольких категорий, имеющих аксессуарные категории,
        а также из категорий без аксессуарных категорий
        """
        cls.index.models += [
            Model(hyperid=101, hid=1001393),
            Model(hyperid=102, hid=6203660),
            Model(hyperid=103, hid=1001410),
            Model(hyperid=104, hid=15561854),
            Model(hyperid=105, hid=1001664),
            Model(hyperid=106, hid=90853),
            Model(hyperid=1061, hid=90941),
            Model(hyperid=1051, hid=91003),
        ]

        cls.recommender.on_request_models_of_interest(user_id='yandexuid:history_data').respond(
            {'models': map(str, [106, 105, 101, 102, 103, 104])}
        )

    def test_history_data(self):
        """
        Для yandexuid=history_data непустая история
        Ожидаем, что выдача упорядочена как соответствующие модели в истории пользователя
        (не считая вставок из аксессуарных категорий)
        """
        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                'place=personal_categories&{}&rearr-factors=split=history_data&yandexuid=history_data&hid=90853&numdoc=8'.format(
                    param
                )
            )
            self.assertFragmentIn(
                response,
                [
                    {'link': {'params': {'hid': '90853'}}},  # 106
                    {'link': {'params': {'hid': '90941'}}},  # accessory
                    {'link': {'params': {'hid': '1001664'}}},  # 105
                    # accessory 90853, skip duplilcate
                    {'link': {'params': {'hid': '91003'}}},  # second accessory
                    {'link': {'params': {'hid': '1001393'}}},  # 101
                    {'link': {'params': {'hid': '6203660'}}},  # accessory for 1001393
                    # 102 - 6203660, skip duplilcate
                    {'link': {'params': {'hid': '1001410'}}},  # 103
                    {'link': {'params': {'hid': '15561854'}}},  # accessory for 1001410,
                    # 104 - 1001410, skip duplilcate
                ],
                preserve_order=True,
            )

    def test_bigb_no_data(self):
        """
        Smoke test для случая, когда доходит дело до вычисления близости по профилю,
            а данных bigb нет
        Данные и конфигурация для нескольких категорий на основе истории
        ожидаем из prepare_history_data
        """
        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                'place=personal_categories&{}&rearr-factors=split=history_data&yandexuid=&hid=90853&numdoc=100500'.format(
                    param
                )
            )
            self.assertFragmentIn(response, {'search': {'total': 6}})
            self.error_log.ignore('BigB: can not get profile for user yandexUid=history_data')
            self.error_log.ignore('Request to BIGB_PROFILE failed')
            self.error_log.ignore('Request to ICHWILL_MODELS_OF_INTEREST failed')

    @classmethod
    def prepare_null_features(cls):
        """
        Данные пользовательского профиля с нулевыми значениями и ненулевыми
        """
        null_profile = create_bigb_profile(gender_buckets=[0] * 2, age_buckets=[0] * 5, revenue_buckets=[0] * 3)
        cls.bigb.on_request(yandexuid='null_features', client='merch-machine').respond(keywords=null_profile)
        profile = create_bigb_profile(gender_buckets=[10000] * 2, age_buckets=[10000] * 5, revenue_buckets=[10000] * 3)
        cls.bigb.on_request(yandexuid='random_features', client='merch-machine').respond(keywords=profile)

        for yandexuid in ['null_features', 'random_features']:
            cls.recommender.on_request_models_of_interest(user_id='yandexuid:{}'.format(yandexuid)).respond(
                {'models': ['1', '2']}
            )

    def test_null_features(self):
        """
        Проверяем корректность работы в случае, когда фичи из профиля или данных категорийной формулы нулевые
        """
        for yandexuid, split in [('random_features', 'null_features'), ('null_features', 'random_features')]:
            for param in ['rgb=blue', 'cpa=real']:
                response = self.report.request_json(
                    'place=personal_categories&{param}&rearr-factors=split={split}&yandexuid={yandexuid}&hid=9000&numdoc=100500'.format(
                        param=param, split=split, yandexuid=yandexuid
                    )
                )
                self.assertFragmentIn(
                    response,
                    [
                        {'link': {'params': {'hid': '9001'}}},
                        {'link': {'params': {'hid': '9002'}}},
                        {'link': {'params': {'hid': '9003'}}},
                        {'link': {'params': {'hid': '9004'}}},
                    ],
                    preserve_order=False,
                )

    def test_crypta_default_order(self):
        """
        Проверяем, что на этапе ранжирования по профилю
            категории упорядочиваются, как в данных категорийной формулы,
            если пользовательские фичи категорий идентичны
        Ожидаем первые две категории 9001, 9002 из истории с ранжированием из истории
            и еще две категории 9006, 9004 отранжированы, как в данных формулы
        В yamarec-конфигурации ожидаем категории с равными пользовательскими фичами в split=equal_features
        Ожидаем данные для профиля и истории пользователя yandexuid=random_features из prepare_similarity_null_features
        """
        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                'place=personal_categories&{}&rearr-factors=split=equal_features&yandexuid=random_features&hid=9000&numdoc=100500'.format(
                    param
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 5,
                        'results': [
                            {'link': {'params': {'hid': '9001'}}},  # history based
                            {'link': {'params': {'hid': '9002'}}},  # history based
                            {'link': {'params': {'hid': '9006'}}},  # similarity based
                            {'link': {'params': {'hid': '9003'}}},  # similarity based
                            {'link': {'params': {'hid': '9004'}}},  # similarity based
                        ],
                    }
                },
                preserve_order=True,
            )

    def test_missing_feature(self):
        """
        Проверка корректной работы при услови отсутствия у категорий
            фичи, которая есть в профиле пользователя
        В yamarec-конфигурации ожидаем категории 9004, 9006 с неполным набором пользовательских фич в split=missing_features
        Эти категории попадают под ранжирование по близости профилей
        Ожидаем данные для профиля пользователя yandexuid=random_features из prepare_similarity_null_features
        """
        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                'place=personal_categories&{}&rearr-factors=split=missing_features&yandexuid=random_features&hid=9000&numdoc=100500'.format(
                    param
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 4,
                        'results': [
                            {'link': {'params': {'hid': '9001'}}},  # history based
                            {'link': {'params': {'hid': '9002'}}},  # history based
                            {'link': {'params': {'hid': '9006'}}},  # similarity based
                            {'link': {'params': {'hid': '9004'}}},  # similarity based
                        ],
                    }
                },
                preserve_order=True,
            )

    @classmethod
    def prepare_rank_1_features(cls):
        hids = [9004, 9001, 9003, 9002]
        models = [hid * 10 + 1 for hid in hids]
        cls.index.models += [Model(hyperid=hyperid, hid=hid) for hyperid, hid in zip(models, hids)]
        profile = create_bigb_profile(
            gender_buckets=[1000000, 0], age_buckets=[10000] * 5, revenue_buckets=[333333] * 3
        )
        cls.bigb.on_request(yandexuid='rank_1_features', client='merch-machine').respond(keywords=profile)
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:rank_1_features').respond(
            {'models': map(str, models)}
        )

    def test_rank_1_features(self):
        """
        Проверяем ранжирование по профилю, когда две категории различаются только в одной фиче
        Данные категорий в split=rank_1_feautre отличаются в одной фиче (gender: male/female)
        По разности значений в этой фиче с пользователем yandexuid=rank_1_features категории сортируются так:
           9004, 9001, 9003, 9002
        """

        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                'place=personal_categories&rearr-factors=split=rank_1_features&yandexuid=rank_1_features&hid=9000&numdoc=100500&{}'.format(
                    param
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 4,
                        'results': [
                            {'link': {'params': {'hid': '9004'}}},
                            {'link': {'params': {'hid': '9001'}}},
                            {'link': {'params': {'hid': '9003'}}},
                            {'link': {'params': {'hid': '9002'}}},
                        ],
                    }
                },
                preserve_order=True,
            )

    @classmethod
    def prepare_orthogonal_features(cls):
        """
        Неполный профиль пользователя, равноудалённый от категорий split=orthogonal_features
        """
        profile = create_bigb_profile(
            gender_buckets=[500000] * 2, age_buckets=[100000] * 5, revenue_buckets=[333333] * 3
        )
        cls.bigb.on_request(yandexuid='orthogonal_features', client='merch-machine').respond(keywords=profile)
        hids = [9003, 9002]
        models = [hid * 10 + 2 for hid in hids]
        cls.index.models += [Model(hyperid=hyperid, hid=hid) for hyperid, hid in zip(models, hids)]
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:orthogonal_features').respond(
            {'models': map(str, models)}
        )

    def test_orthogonal_features(self):
        """
        1. Проверяем ранжирование по профилю, когда категории "ортогональны" и равноудалены от пользователя
        Ожидаем дефолтное ранжирование - как в данных категорий, не зависимо от порядка категорий в запросе
        """
        for hids in '9002,9003', '9003,9002':
            for param in ['rgb=blue', 'cpa=real']:
                response = self.report.request_json(
                    'place=personal_categories&{}&rearr-factors=split=orthogonal_features&yandexuid=orthogonal_features&hid={}&numdoc=100500'.format(
                        param, hids
                    )
                )
                self.assertFragmentIn(
                    response,
                    {
                        'search': {
                            'total': 2,
                            'results': [
                                {'link': {'params': {'hid': '9003'}}},
                                {'link': {'params': {'hid': '9002'}}},
                            ],
                        }
                    },
                    preserve_order=True,
                )
        self.error_log.ignore('Request to ICHWILL_MODELS_OF_INTEREST failed')
        """
        2. Проверяем ранжирование по профилю, когда категории "ортогональны" и равноудалены от пользователя,
        если смотреть по одному подмножеству фич, но ранжируются по добавочному подмножеству фич
        """
        for hids in '9002,9003', '9003,9002':
            for param in ['rgb=blue', 'cpa=real']:
                response = self.report.request_json(
                    'place=personal_categories&{}&rearr-factors=split=orthogonal_and_ranging_features&yandexuid=orthogonal_features&hid={}&numdoc=100500'.format(
                        param, hids
                    )
                )
                self.assertFragmentIn(
                    response,
                    {
                        'search': {
                            'total': 2,
                            'results': [
                                {'link': {'params': {'hid': '9003'}}},
                                {'link': {'params': {'hid': '9002'}}},
                            ],
                        }
                    },
                    preserve_order=True,
                )

    @classmethod
    def prepare_numdoc(cls):
        """
        Задаём историю пользователя и BigB-профиль для yandexuid=regular
        Ожидаем фичи для трех категорий в split=simple,
          и аксессуарные категорий для истории пользователя
        """
        cls.index.hypertree += [
            HyperCategory(
                hid=9999,
                output_type=HyperCategoryType.GURU,
                children=[
                    # categories with accessories
                    HyperCategory(hid=hid, output_type=HyperCategoryType.GURU)
                    for hid in SAMPLE_CATEGORY_HISTORY[:3]
                ]
                + [
                    # accessories
                    HyperCategory(hid=ACCESSORY_CATEGORIES[hid][0], output_type=HyperCategoryType.GURU)
                    for hid in SAMPLE_CATEGORY_HISTORY[:3]
                ]
                + [
                    # categories without accessories
                    HyperCategory(hid=9991, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=9992, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=9993, output_type=HyperCategoryType.GURU),
                ],
            ),
        ]
        model_ids = [10000 + i for i in range(3)]
        cls.index.models += [
            Model(hyperid=hyperid, hid=hid) for hyperid, hid in zip(model_ids, SAMPLE_CATEGORY_HISTORY[:3])
        ]
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:regular').respond(
            {'models': map(str, model_ids)}
        )
        profile = create_bigb_profile(
            gender_buckets=[500000] * 2, age_buckets=[100000] * 5, revenue_buckets=[333333] * 3
        )
        cls.bigb.on_request(yandexuid='regular', client='merch-machine').respond(keywords=profile)

    def test_numdoc(self):
        """
        Проверка обработки параметра numdoc
        Результат должен содержать не больше numdoc категорий, и ровно numdoc, если есть достаточно данных
        """
        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                'place=personal_categories&{}&rearr-factors=split=simple&yandexuid=regular&hid=9999&numdoc=0'.format(
                    param
                )
            )
            self.assertFragmentIn(response, {'search': {'total': 0, 'results': ElementCount(0)}})

        """
        Есть история пользователя. Проверяем случай, когда не требуется BigB:
        Ожидаем чередование: hid из истории - accessory hid
        """
        top_hid = SAMPLE_CATEGORY_HISTORY[0]
        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                'place=personal_categories&{}&rearr-factors=split=simple&yandexuid=regular&hid=9999&numdoc=1'.format(
                    param
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 1,
                        'results': [
                            {'link': {'params': {'hid': str(top_hid)}}},
                        ],
                    }
                },
            )

        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                'place=personal_categories&{}&rearr-factors=split=simple&yandexuid=regular&hid=9999&numdoc=2'.format(
                    param
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 2,
                        'results': [
                            {'link': {'params': {'hid': str(top_hid)}}},
                            {'link': {'params': {'hid': str(ACCESSORY_CATEGORIES[top_hid][0])}}},
                        ],
                    }
                },
                preserve_order=True,
            )

        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                'place=personal_categories&{}&rearr-factors=split=simple&yandexuid=regular&hid=9999&numdoc=3'.format(
                    param
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 3,
                        'results': [
                            {'link': {'params': {'hid': str(top_hid)}}},
                            {'link': {'params': {'hid': str(ACCESSORY_CATEGORIES[top_hid][0])}}},
                            {'link': {'params': {'hid': str(SAMPLE_CATEGORY_HISTORY[1])}}},
                        ],
                    }
                },
                preserve_order=True,
            )

        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                'place=personal_categories&{}&rearr-factors=split=simple&yandexuid=regular&hid=9999&numdoc=6'.format(
                    param
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 6,
                        'results': list(
                            itertools.chain.from_iterable(
                                [
                                    [
                                        {'link': {'params': {'hid': str(hid)}}},
                                        {'link': {'params': {'hid': str(ACCESSORY_CATEGORIES[hid][0])}}},
                                    ]
                                    for hid in SAMPLE_CATEGORY_HISTORY[:3]
                                ]
                            )
                        ),
                    }
                },
                preserve_order=True,
            )

        """
        Случай, когда истории недостаточно и требуется BigB
        """
        for param in ['rgb=blue', 'cpa=real']:
            response = self.report.request_json(
                'place=personal_categories&{}&rearr-factors=split=simple&yandexuid=regular&hid=9999&numdoc=7'.format(
                    param
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 7,
                        'results': list(
                            itertools.chain.from_iterable(
                                [
                                    [
                                        {'link': {'params': {'hid': str(hid)}}},
                                        {'link': {'params': {'hid': str(ACCESSORY_CATEGORIES[hid][0])}}},
                                    ]
                                    for hid in SAMPLE_CATEGORY_HISTORY[:3]
                                ]
                                + [
                                    [
                                        {'link': {'params': {'hid': '9991'}}},
                                    ],
                                ]
                            )
                        ),
                    }
                },
                preserve_order=True,
            )

        """
        Нет истории (yandexuid='')
        """
        _ = [9991, 9992, 9993]
        for numdoc in range(5):
            for param in ['rgb=blue', 'cpa=real']:
                response = self.report.request_json(
                    'place=personal_categories&{param}&rearr-factors=split=simple&yandexuid=&hid=9999&numdoc={numdoc}'.format(
                        param=param, numdoc=numdoc
                    )
                )
                self.assertFragmentIn(
                    response, {'search': {'total': min(3, numdoc), 'results': ElementCount(min(3, numdoc))}}
                )
        self.error_log.ignore('Request to ICHWILL_MODELS_OF_INTEREST failed')

    def check_market_req_id_forwarded_value(self, contains_value, not_contains_value, external_service_logs):
        for i, log in enumerate(external_service_logs):
            log.expect(headers=Contains("x-market-req-id={}/{}".format(contains_value, i).lower()))
            log.expect(headers=Not(Contains("x-market-req-id={}/{}".format(not_contains_value, i).lower())))

    def check_market_req_id_forwarded(self, test_request, header_value, cgi_value, external_service_logs):
        market_req_id_header = {'X-Market-Req-ID': header_value}

        self.report.request_json(test_request, headers=market_req_id_header)
        self.check_market_req_id_forwarded_value(header_value, cgi_value, external_service_logs)

        self.report.request_json(test_request + "&market-req-id={}".format(cgi_value), headers=market_req_id_header)
        self.check_market_req_id_forwarded_value(header_value, cgi_value, external_service_logs)

        self.report.request_json(test_request + "&market-req-id={}".format(cgi_value))
        self.check_market_req_id_forwarded_value(cgi_value, header_value, external_service_logs)

    def test_market_req_id_forwarded(self):
        for param in ['rgb=blue', 'cpa=real']:
            test_request = "place=personal_categories&{}&rearr-factors=split=equal_features&yandexuid=random_features&hid=9000&numdoc=100500".format(
                param
            )

            '''
            Проверяем, что репорт пробрасывает заголовок X-Market-Req-ID или (в отсутствие заголовка) CGI параметр market-req-id в сервис bigb
            Сначала для alphanum значения, потом для numerical
            '''
            self.check_market_req_id_forwarded(test_request, "abc123", "def456", [self.bigb_log, self.recommender_log])
            self.check_market_req_id_forwarded(test_request, 987654321, 12345678, [self.bigb_log, self.recommender_log])


CATEGORY_FEATURE_MALE = '174:0_avg'
CATEGORY_FEATURE_FEMALE = '174:1_avg'
CATEGORY_FEATURE_AGE_0_17 = '175:0_avg'
CATEGORY_FEATURE_AGE_18_24 = '175:1_avg'
CATEGORY_FEATURE_AGE_25_34 = '175:2_avg'
CATEGORY_FEATURE_AGE_35_44 = '175:3_avg'
CATEGORY_FEATURE_AGE_45_99 = '175:4_avg'
CATEGORY_FEATURE_INCOME_A = '176:0_avg'
CATEGORY_FEATURE_INCOME_B = '176:1_avg'
CATEGORY_FEATURE_INCOME_C = '176:2_avg'

CATEGORY_USER_FEATURES = [
    CATEGORY_FEATURE_MALE,
    CATEGORY_FEATURE_FEMALE,
    CATEGORY_FEATURE_AGE_0_17,
    CATEGORY_FEATURE_AGE_18_24,
    CATEGORY_FEATURE_AGE_25_34,
    CATEGORY_FEATURE_AGE_35_44,
    CATEGORY_FEATURE_AGE_45_99,
    CATEGORY_FEATURE_INCOME_A,
    CATEGORY_FEATURE_INCOME_B,
    CATEGORY_FEATURE_INCOME_C,
]

# fragment of hardcoded accessory categories file
ACCESSORY_CATEGORIES = {
    1001393: [6203660, 90569, 91161, 454690, 13239041, 90567, 90586, 6203657, 766157, 90570],
    1001410: [15561854, 90906, 90987, 90976, 90996, 90928, 90854, 90983, 90967, 90905],
    1001664: [90853, 91003, 90979, 90971, 15561854, 90991, 989393, 90987, 90938, 90984],
    1003092: [11911273, 989040, 1003093, 6196807, 10752691, 12894143, 7286126, 7286125, 90675, 10755995],
    1003093: [1003092, 90675, 6196807, 7286537, 7286125, 1003105, 6280628, 90710, 90681, 989040],
    1003105: [7286537, 90675, 6280628, 90678, 90684, 14739825, 1003093, 7296416, 1003092, 10785221],
    1005464: [10683245, 6527202, 10682618, 1006240, 10683227, 10682629, 7851711, 10682647, 10683255, 14419921],
    1005898: [90568, 90564, 90567, 90566, 242704, 1564519, 1005910, 90572, 278341, 242699],
    1005910: [90568, 90564, 1564519, 90586, 12802914, 90588, 765280, 90569, 91161, 90590],
    1006240: [14304975, 7851706, 10683245, 14288720, 91590, 12785133, 233108, 396902, 91541, 6527202],
    1006244: [12785133, 15315708, 1009483, 14419921, 1001393, 12501719, 6269371, 91498, 14288720, 91526],
    90853: [90941, 1001664, 90985, 15561854, 90987, 90863, 90860, 90856, 90854, 90857],
}

SAMPLE_CATEGORY_HISTORY = [1005910, 1006240, 1006244]


def create_bigb_profile(gender_buckets, age_buckets, revenue_buckets):
    return [
        BigBKeyword(
            id=BigBKeyword.GENDER,
            weighted_uint_values=[
                WeightedValue(value=BigBKeyword.GENDER_MALE, weight=gender_buckets[0]),
                WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=gender_buckets[1]),
            ],
        ),
        BigBKeyword(
            id=BigBKeyword.AGE,
            weighted_uint_values=[
                WeightedValue(value=BigBKeyword.AGE_0_17, weight=age_buckets[0]),
                WeightedValue(value=BigBKeyword.AGE_18_24, weight=age_buckets[1]),
                WeightedValue(value=BigBKeyword.AGE_25_34, weight=age_buckets[2]),
                WeightedValue(value=BigBKeyword.AGE_35_44, weight=age_buckets[3]),
                WeightedValue(value=BigBKeyword.AGE_45_54, weight=age_buckets[4]),
            ],
        ),
        BigBKeyword(
            id=BigBKeyword.REVENUE,
            weighted_uint_values=[
                WeightedValue(value=BigBKeyword.REVENUE_LOW, weight=revenue_buckets[0]),
                WeightedValue(value=BigBKeyword.REVENUE_MED, weight=revenue_buckets[1]),
                WeightedValue(value=BigBKeyword.REVENUE_HIGH, weight=revenue_buckets[2]),
            ],
        ),
    ]


if __name__ == '__main__':
    main()
