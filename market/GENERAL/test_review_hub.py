#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    GradeDispersionItem,
    HyperCategory,
    HyperCategoryType,
    Model,
    ModelGroup,
    NewShopRating,
    Offer,
    Opinion,
    PhotoDataItem,
    Region,
    ReviewDataItem,
    Shop,
    YamarecFeaturePartition,
    YamarecPlace,
)
from core.testcase import TestCase, main
from core.matcher import NotEmpty


class T(TestCase):
    # MARKETOUT-14808
    @classmethod
    def prepare_model_with_opinions(cls):
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        """Добавляем модели с отзывами в гуру-категорию"""
        cls.index.hypertree += [
            HyperCategory(hid=30, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=31, output_type=HyperCategoryType.GURULIGHT),
        ]

        cls.index.models += [
            Model(hyperid=1480801, hid=30, title='Модель с отзывом'),
            Model(hyperid=1480802, hid=30, title='Модель без отзыва'),
        ]

        cls.index.offers += [
            Offer(hyperid=1480801, title='t0 offer', price=101),
            Offer(hyperid=1480802, title='t1 offer', price=102),
        ]

        cls.index.model_reviews_data += [
            ReviewDataItem(
                review_id=14808001,
                model_id=1480801,
                author_id=12345,
                region_id=213,
                cpa=False,
                anonymous=0,
                usage_time=1,
                pro='Хорошие ботинки',
                contra='Но пахнут гуталином',
                cr_time='1970-01-01T03:00:00',
                short_text='Ходят и ладно',
                agree=5,
                reject=1,
                total_votes=6,
                grade_value=4,
                photos=[PhotoDataItem(group_id=0, image_name="12345"), PhotoDataItem(group_id=1, image_name="67890")],
                most_useful=1,
            )
        ]

        cls.index.model_grade_dispersion_data += [GradeDispersionItem(model_id=1480801)]

    def test_catalog_with_reviews(self):
        """
        Делаем запрос на каталог категории

        У нас должна показаться только одна модель 1480801, т.к. на ней есть отзыв.
        Модель 1480802 должна исключиться, т.к. на ней нет отзывов.

        И проверяем все поля выдачи
        """
        response = self.report.request_json('place=prime&hid=30&show-reviews=1&allow-collapsing=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'totalOffers': 0,
                    'totalModels': 1,
                    'results': [
                        {
                            'entity': 'product',
                            'showReview': True,
                            'review': {
                                'entity': 'review',
                                'id': 14808001,
                                'user': {
                                    'entity': 'user',
                                    'uid': 12345,
                                },
                                'averageGrade': 4,
                                'usage': 1,
                                'pro': 'Хорошие ботинки',
                                'contra': 'Но пахнут гуталином',
                                'comment': 'Ходят и ладно',
                                'created': '1970-01-01T00:00:00Z',
                                'photos': [
                                    {
                                        'groupId': '0',
                                        'imageName': '12345',
                                    },
                                    {
                                        'groupId': '1',
                                        'imageName': '67890',
                                    },
                                ],
                                'anonymous': 0,
                                'votes': {
                                    'agree': 5,
                                    'reject': 1,
                                    'total': 6,
                                },
                                'type': 1,
                            },
                        }
                    ],
                }
            },
            allow_different_len=False,
        )

    # MARKETOUT-14808
    @classmethod
    def prepare_group_model_with_opinions(cls):
        """Добавляем групповые модели с отзывами"""

        cls.index.hypertree += [
            HyperCategory(hid=40, output_type=HyperCategoryType.GURU),
        ]

        cls.index.model_groups += [
            ModelGroup(hyperid=2480801, hid=40, title='Групповая Модель с отзывом'),
            ModelGroup(hyperid=2480804, hid=40, title='Групповая Модель 2 с отзывом'),
        ]

        cls.index.models += [
            Model(hyperid=2480802, hid=40, group_hyperid=2480801, title='модификация без отзыва'),
            Model(hyperid=2480803, hid=40, group_hyperid=2480801, title='модификация без отзыва'),
            Model(hyperid=2480805, hid=40, group_hyperid=2480804, title='модификация без отзыва'),
            Model(hyperid=2480806, hid=40, group_hyperid=2480804, title='модификация без отзыва'),
        ]

        cls.index.model_reviews_data += [
            ReviewDataItem(review_id=24808001, model_id=2480801, most_useful=1),
            ReviewDataItem(review_id=24808004, model_id=2480804, most_useful=1),
        ]

        cls.index.model_grade_dispersion_data += [
            GradeDispersionItem(model_id=2480801),
            GradeDispersionItem(model_id=2480804),
        ]

    def test_group_model_reviews_in_catalog(self):
        """
        Делаем запрос на каталог категории

        У нас должна остаться только групповые модели - модификации скрываются
        """
        response = self.report.request_json('place=prime&hid=40&show-reviews=1&allow-collapsing=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'totalOffers': 0,
                    'totalModels': 2,
                    'results': [
                        {
                            'entity': 'product',
                            'titles': {'raw': 'Групповая Модель с отзывом'},
                            'review': {},
                        },
                        {
                            'entity': 'product',
                            'titles': {'raw': 'Групповая Модель 2 с отзывом'},
                            'review': {},
                        },
                    ],
                }
            },
            allow_different_len=False,
        )

    def test_group_model_reviews_in_catalog_sortings(self):
        """
        Делаем запрос на каталог категории с разными сортировками

        У нас должна остаться только групповая модель - модификация скрывается
        """
        for sort in ['&how=opinions', '&how=quality', '&how=aprice']:
            response = self.report.request_json(
                'place=prime&hid=40&show-reviews=1&allow-collapsing=1&rearr-factors=market_metadoc_search=no' + sort
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 2,
                        'totalOffers': 0,
                        'totalModels': 2,
                        'results': [
                            {
                                'entity': 'product',
                                'titles': {'raw': 'Групповая Модель с отзывом'},
                                'review': {},
                            },
                            {
                                'entity': 'product',
                                'titles': {'raw': 'Групповая Модель 2 с отзывом'},
                                'review': {},
                            },
                        ],
                    }
                },
                allow_different_len=False,
            )

    # MARKETOUT-15516
    @classmethod
    def prepare_model_with_offers(cls):

        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
            Region(rid=214, name='Не Москва'),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=50, output_type=HyperCategoryType.GURU),
        ]

        cls.index.models += [
            Model(hyperid=3480801, hid=50, title='модель с отзывом и оффером'),
            Model(hyperid=3480802, hid=50, title='модель с отзывом но без оффера'),
        ]

        cls.index.model_reviews_data += [
            ReviewDataItem(review_id=34808001, model_id=3480801, most_useful=1),
            ReviewDataItem(review_id=34808002, model_id=3480802, most_useful=1),
        ]

        cls.index.model_grade_dispersion_data += [
            GradeDispersionItem(model_id=3480801),
            GradeDispersionItem(model_id=3480802),
        ]

        # офферы магазина будут находиться, даже если запрос идет с другим регионом
        cls.index.shops += [
            Shop(
                fesh=3480801,
                name='test_shop_1',
                new_shop_rating=NewShopRating(new_rating_total=3.0),
                priority_region=213,
                regions=[213],
            ),
            Shop(
                fesh=3480802,
                name='test_shop_2',
                new_shop_rating=NewShopRating(new_rating_total=3.0),
                priority_region=213,
                regions=[213],
            ),
        ]

        # один оффер для модели, и один без модели
        cls.index.offers += [
            Offer(fesh=34808011, hyperid=3480801, title="offer for model"),
            Offer(fesh=34808011, hid=50),
        ]

    def test_model_contains_offers(self):
        # должен выкинуться только одинокий оффер, поскольку для них нет отзывов. И второй оффер хлопнулся в мдель
        response = self.report.request_json('place=prime&hid=50&show-reviews=1&allow-collapsing=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'totalOffers': 0,
                    'totalModels': 2,
                    'results': [
                        {
                            'entity': 'product',
                            'titles': {'raw': 'модель с отзывом и оффером'},
                        },
                        {
                            'entity': 'product',
                            'titles': {'raw': 'модель с отзывом но без оффера'},
                        },
                    ],
                }
            },
            allow_different_len=False,
        )

    # https://st.yandex-team.ru/MARKETOUT-16508
    def test_models_still_here_if_no_offers_and_onstock_required(self):
        # Без ревью-хаба получаем только 1 модель, поскольку у второй нет офферов
        response = self.report.request_json(
            'place=prime&hid=50&allow-collapsing=1&onstock=1&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'totalOffers': 0,
                    'totalModels': 1,
                }
            },
        )

        # Без ревью-хаба получаем ничего, потому что в этом регионе вообще ни у кого ничего нет
        response = self.report.request_json(
            'place=prime&hid=50&allow-collapsing=1&onstock=1&regset=2&rids=214&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 0,
                    'totalOffers': 0,
                    'totalModels': 0,
                }
            },
        )

        # А на ревью-хабе выкидывается только одинокий оффер. Остались модель без оффера и модель в другом регионе, несмотря на onstock=1 и указание регионов
        response = self.report.request_json(
            'place=prime&hid=50&allow-collapsing=1&onstock=1&regset=2&rids=214&show-reviews=1&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'totalOffers': 0,
                    'totalModels': 2,
                }
            },
        )

    def test_offers_as_result_not_come(self):
        # отзывов на офферы нет, но в моделях должны быть офферы и цены в случае их наличия
        response = self.report.request_json('place=prime&hid=30&show-reviews=1&allow-collapsing=0')
        self.assertFragmentNotIn(response, {'results': [{'entity': 'offer'}]})
        self.assertFragmentIn(
            response, {'results': [{'entity': 'product', 'type': 'model', 'prices': NotEmpty(), 'offers': NotEmpty()}]}
        )

    # https://st.yandex-team.ru/MARKETOUT-15755
    @classmethod
    def prepare_model_with_discount(cls):

        cls.index.hypertree += [
            HyperCategory(hid=15755, output_type=HyperCategoryType.GURU),
        ]

        cls.index.models += [
            Model(hyperid=157551, hid=15755, title='модель с отзывом2'),
            Model(hyperid=157552, hid=15755, title='модель с отзывом1'),
        ]

        cls.index.model_reviews_data += [
            ReviewDataItem(review_id=157555, model_id=157551, most_useful=1),
            ReviewDataItem(review_id=157556, model_id=157552, most_useful=1),
        ]

        cls.index.model_grade_dispersion_data += [
            GradeDispersionItem(model_id=157551),
            GradeDispersionItem(model_id=157552),
        ]

        cls.index.shops += [
            Shop(fesh=157553, name='test_shop_1', new_shop_rating=NewShopRating(new_rating_total=3.0)),
            Shop(fesh=157554, name='test_shop_2', new_shop_rating=NewShopRating(new_rating_total=3.0)),
        ]

        cls.index.offers += [
            Offer(fesh=157553, hyperid=157551, discount=45, title="offer1"),
            Offer(fesh=157554, hyperid=157552, discount=30, title="offer2"),
        ]

    def test_discount_sorting_disappear(self):
        # проверяем, что без show-reviews ревью сортировка на месте
        response = self.report.request_json('place=prime&hid=15755&allow-collapsing=1')
        self.assertFragmentIn(
            response,
            {
                'sorts': [
                    {"text": "по размеру скидки", "options": [{"id": "discount_p"}]},
                ]
            },
        )

        # а с ревью исчезла
        response = self.report.request_json('place=prime&hid=15755&allow-collapsing=1&show-reviews=1')
        self.assertFragmentNotIn(
            response,
            {
                'sorts': [
                    {"text": "по размеру скидки", "options": [{"id": "discount_p"}]},
                ]
            },
        )

    @classmethod
    def prepare_multi_categories(cls):
        """
        Создаем 10 категорий по 2 модели на категорию
        Создаем Yamarec-конфигурацию с рекомендуемыми моделями 4-х категорий
        и настройки внешних сервисов
        """

        # index
        for hid_seq in range(10):
            for hyperid_seq in range(2):
                hyperid = 16851000 + hid_seq * 2 + hyperid_seq
                cls.index.models += [Model(hyperid=hyperid, randx=999 - hyperid % 1000, hid=1685100 + hid_seq)]

                cls.index.offers += [Offer(hyperid=hyperid)]

                cls.index.model_reviews_data += [
                    ReviewDataItem(
                        review_id=hyperid * 10 + 1,
                        model_id=hyperid,
                        author_id=12345,
                        region_id=213,
                        cpa=False,
                        anonymous=0,
                        usage_time=1,
                        pro='Телефон работает',
                        contra='Но медленно',
                        cr_time='1970-01-01T03:00:00',
                        short_text='Можно брать',
                        agree=5,
                        reject=1,
                        total_votes=6,
                        grade_value=4,
                        photos=[
                            PhotoDataItem(group_id=10, image_name="56789"),
                            PhotoDataItem(group_id=11, image_name="54321"),
                        ],
                        most_useful=1,
                    )
                ]

                cls.index.model_grade_dispersion_data += [GradeDispersionItem(model_id=hyperid)]

        model_ids = list(range(16851013, 16851020))

        # external services configs
        cls.recommender.on_request_viewed_models(user_id="yandexuid:10001").respond({"models": map(str, model_ids)})
        crypta_response = map(lambda model: {'id': model}, model_ids)
        cls.crypta.on_request_models('10001').respond(crypta_response)
        cls.crypta.on_default_request().respond(features=[])

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
                ],
            ),
        ]

    def test_multicategory_personal_categs(self):
        """Что тестируем: выдачу отзывов в плейсе multi_category с параметром
        &show-personal=1
        Проверяем, что при запросе с категориями и без них
        выдаются модели рекомендованных категорий, а не тех, что
        указаны в запросе
        """
        for hids in ['', '&hid=1685100&hid=1685101']:
            response = self.report.request_json(
                'place=multi_category&show-personal=1&show-reviews=1&yandexuid=10001&rearr-factors=split=market&allow-collapsing=1'
                + hids
            )
            self.assertFragmentIn(
                response,
                {
                    "total": 8,
                    "results": [
                        {"id": 16851012, "categories": [{"id": 1685106}], "review": {"id": 168510121}},
                        {"id": 16851013, "categories": [{"id": 1685106}], "review": {"id": 168510131}},
                        {"id": 16851014, "categories": [{"id": 1685107}], "review": {"id": 168510141}},
                        {"id": 16851015, "categories": [{"id": 1685107}], "review": {"id": 168510151}},
                        {"id": 16851016, "categories": [{"id": 1685108}], "review": {"id": 168510161}},
                        {"id": 16851017, "categories": [{"id": 1685108}], "review": {"id": 168510171}},
                        {"id": 16851018, "categories": [{"id": 1685109}], "review": {"id": 168510181}},
                        {"id": 16851019, "categories": [{"id": 1685109}], "review": {"id": 168510191}},
                    ],
                },
                allow_different_len=False,
            )

    @classmethod
    def prepare_model_review_rating(cls):
        """
        Создаем 5 моделей:
          - с низким рейтингом и плохой оценкой
          - с низким рейтингом и хорошей оценкой
          - с хорошим рейтингом и плохой оценкой
          - с хорошим рейтингом и хорошей оценкой
          - без рейтинга и хорошей оценкой
        """

        cls.index.models += [
            Model(hyperid=1676410, hid=1676400, opinion=Opinion(total_count=3, rating=2)),
            Model(hyperid=1676411, hid=1676400, opinion=Opinion(total_count=3, rating=3.5)),
            Model(hyperid=1676412, hid=1676400, opinion=Opinion(total_count=3, rating=3.75)),
            Model(hyperid=1676413, hid=1676400, opinion=Opinion(total_count=3, rating=5)),
            Model(hyperid=1676414, hid=1676400),
        ]

        cls.index.model_reviews_data += [
            ReviewDataItem(
                review_id=16764101, model_id=1676410, author_id=12345, region_id=213, grade_value=2, most_useful=1
            ),
            ReviewDataItem(
                review_id=16764111, model_id=1676411, author_id=12345, region_id=213, grade_value=4, most_useful=1
            ),
            ReviewDataItem(
                review_id=16764121, model_id=1676412, author_id=12345, region_id=213, grade_value=3, most_useful=1
            ),
            ReviewDataItem(
                review_id=16764131, model_id=1676413, author_id=12345, region_id=213, grade_value=5, most_useful=1
            ),
            ReviewDataItem(
                review_id=16764141, model_id=1676414, author_id=12345, region_id=213, grade_value=5, most_useful=1
            ),
        ]

        cls.index.model_grade_dispersion_data += [
            GradeDispersionItem(model_id=1676410),
            GradeDispersionItem(model_id=1676411),
            GradeDispersionItem(model_id=1676412),
            GradeDispersionItem(model_id=1676413),
            GradeDispersionItem(model_id=1676414),
        ]

    def test_model_rating_filter(self):
        """Что тестируем: фильтрацию по рейтингу модели на хабе отзывов"""
        response = self.report.request_json(
            'place=prime&show-reviews=1&hid=1676400&model-rating=3.75&allow-collapsing=1'
        )
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "results": [
                    {"id": 1676412},
                    {"id": 1676413},
                ],
            },
            allow_different_len=False,
        )

        def test_review_grade_filter(self):
            """Что тестируем: фильтрацию по оценке в отзыве на модель"""
            response = self.report.request_json(
                'place=prime&show-reviews=1&hid=1676400&review-grade=4&allow-collapsing=1'
            )
            self.assertFragmentIn(
                response,
                {
                    "total": 3,
                    "results": [
                        {"id": 1676411},
                        {"id": 1676413},
                        {"id": 1676414},
                    ],
                },
                allow_different_len=False,
            )

    def test_model_rating_review_grade_filters(self):
        """Что тестируем: фильтрацию по рейтингу модели и по оценке в ее отзыве
        на хабе отзывов
        Фильтрация работает по логике "И"
        """
        response = self.report.request_json(
            'place=prime&show-reviews=1&hid=1676400&model-rating=3.75&review-grade=4&allow-collapsing=1'
        )
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [
                    {"id": 1676413},
                ],
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_several_reviews(cls):
        cls.index.hypertree += [
            HyperCategory(hid=60, output_type=HyperCategoryType.GURU),
        ]

        cls.index.models += [
            Model(hyperid=1680001, hid=60, title='Плюмбус с 2 отзывами'),
        ]

        cls.index.model_reviews_data += [
            ReviewDataItem(
                review_id=16800001,
                model_id=1680001,
                author_id=12345,
                region_id=213,
                cpa=False,
                anonymous=0,
                usage_time=1,
                pro='Помогает на кухне',
                contra='Дорого',
                cr_time='1970-01-01T03:00:00',
                short_text='Других нет',
                agree=5,
                reject=1,
                total_votes=6,
                grade_value=4,
                most_useful=1,
            ),
            ReviewDataItem(
                review_id=16800002,
                model_id=1680001,
                author_id=12345,
                region_id=213,
                cpa=False,
                anonymous=0,
                usage_time=1,
                pro='Помогает в ванной',
                contra='Дешево',
                cr_time='1970-01-01T03:00:00',
                short_text='Других нет',
                agree=5,
                reject=1,
                total_votes=6,
                grade_value=5,
                most_useful=1,
            ),
            ReviewDataItem(review_id=16800003, model_id=1680001, max_grade_useful=1),
            ReviewDataItem(review_id=16800004, model_id=1680001, min_grade_useful=1),
        ]
        cls.index.model_grade_dispersion_data += [GradeDispersionItem(model_id=1680001)]

    def test_several_reviews(self):
        response = self.report.request_json('place=prime&hid=60&show-reviews=2&allow-collapsing=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'totalOffers': 0,
                    'totalModels': 1,
                    'results': [
                        {
                            'entity': 'product',
                            'showReview': True,
                            'review': {
                                'entity': 'review',
                                'id': 16800001,
                            },
                            'reviews': [
                                {
                                    'entity': 'review',
                                    'id': 16800001,
                                },
                                {
                                    'entity': 'review',
                                    'id': 16800002,
                                },
                            ],
                        }
                    ],
                }
            },
            allow_different_len=False,
        )

        response = self.report.request_json('place=prime&hid=60&show-reviews=3&allow-collapsing=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'totalOffers': 0,
                    'totalModels': 1,
                    'results': [
                        {
                            'entity': 'product',
                            'showReview': True,
                            'review': {
                                'entity': 'review',
                                'id': 16800001,
                            },
                            'reviews': [
                                {
                                    'entity': 'review',
                                    'id': 16800001,
                                },
                                {
                                    'entity': 'review',
                                    'id': 16800002,
                                },
                                {
                                    'entity': 'review',
                                    'id': 16800003,
                                },
                            ],
                        }
                    ],
                }
            },
            allow_different_len=False,
        )

        response = self.report.request_json('place=prime&hid=60&show-reviews=4&allow-collapsing=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'totalOffers': 0,
                    'totalModels': 1,
                    'results': [
                        {
                            'entity': 'product',
                            'showReview': True,
                            'review': {
                                'entity': 'review',
                                'id': 16800001,
                            },
                            'reviews': [
                                {
                                    'entity': 'review',
                                    'id': 16800001,
                                },
                                {
                                    'entity': 'review',
                                    'id': 16800002,
                                },
                                {
                                    'entity': 'review',
                                    'id': 16800003,
                                },
                                {
                                    'entity': 'review',
                                    'id': 16800004,
                                },
                            ],
                        }
                    ],
                }
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
