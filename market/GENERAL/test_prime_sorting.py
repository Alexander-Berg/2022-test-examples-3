#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from simple_testcase import get_timestamp
from core.testcase import TestCase, main
from core.types import (
    GLParam,
    GLType,
    HyperCategory,
    HyperCategoryType,
    MnPlace,
    Model,
    ModelGroup,
    NewShopRating,
    Offer,
    Opinion,
    RegionalModel,
    Shop,
    VCluster,
)
from core.matcher import Absent, Round


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=4']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.hypertree += [HyperCategory(hid=1, visual=True)]
        cls.index.vclusters += [
            VCluster(
                hid=1,
                vclusterid=1000000101,
                model_clicks=500,
                title="vcluster",
                opinion=Opinion(total_count=11, rating=4.5),
                randx=1000,
            ),
            VCluster(
                hid=1,
                vclusterid=1000000102,
                title="vcluster",
                opinion=Opinion(total_count=7, rating=3.5),
                randx=200,
            ),
            VCluster(hid=1, vclusterid=1000000103, model_clicks=900, opinion=Opinion(total_count=15), randx=200),
        ]

        cls.index.offers += [
            Offer(title='offerdoc', hid=1, fesh=10, hyperid=301, randx=1),
            Offer(title='offerdoc', hid=1, fesh=10, hyperid=301, randx=1),
            Offer(title='offerdoc', hid=1, fesh=10, hyperid=302, randx=1),
            Offer(title='offerdoc', hid=1, fesh=10, hyperid=302, randx=1),
            Offer(title='offerdoc', hid=1, fesh=10, hyperid=303, randx=1),
            Offer(title='offerdoc', hid=1, fesh=10, hyperid=304, randx=1),
            Offer(title='offerdoc', hid=1, fesh=10, hyperid=305, randx=1),
            Offer(title='offerdoc', hid=1, fesh=10, hyperid=306, randx=1),
            Offer(title='offerdoc', hid=1, fesh=10, hyperid=307, randx=1),
            Offer(title='offerdoc', hid=1, fesh=10, randx=1),
            Offer(title='offerdoc', hid=1, fesh=10, randx=1),
            Offer(title='offerdoc', hid=1, fesh=10, vclusterid=1000000101, randx=2),
            Offer(title='offerdoc', hid=1, fesh=10, vclusterid=1000000102, randx=2),
            Offer(title='offerdoc', hid=1, fesh=10, vclusterid=1000000103, randx=2),
        ]
        cls.index.models += [
            Model(
                title='model doc',
                hyperid=301,
                hid=1,
                model_clicks=1000,
                opinion=Opinion(total_count=10, rating=5.0, precise_rating=4.9),
                randx=101,
            ),
            Model(
                title='model doc',
                hyperid=302,
                hid=1,
                model_clicks=3000,
                opinion=Opinion(total_count=14, rating=5.0, precise_rating=4.8),
                randx=102,
            ),
            Model(
                title='model doc',
                hyperid=303,
                hid=1,
                model_clicks=800,
                opinion=Opinion(total_count=7, rating=4.0, precise_rating=4.2),
                randx=100,
            ),
            Model(
                title='model doc',
                hyperid=304,
                hid=1,
                opinion=Opinion(total_count=9, rating=3, precise_rating=2.9),
                randx=1000,
            ),
            Model(title='model doc', hyperid=305, hid=1, model_clicks=5, opinion=Opinion(total_count=3), randx=13),
            Model(
                title='model doc',
                hyperid=306,
                hid=1,
                model_clicks=100,
                opinion=Opinion(total_count=40, rating=1.5, precise_rating=1.7),
                randx=14,
            ),
            Model(title='model doc', hyperid=307, hid=1, model_clicks=300, randx=999),
            Model(title='model doc', hyperid=308, hid=1, model_clicks=5000, randx=12),
        ]

        cls.index.gltypes += [GLType(hid=2, param_id=1, values=[1, 2], cluster_filter=True)]

        cls.index.model_groups += [
            ModelGroup(hid=2, hyperid=4001, title="model_group", opinion=Opinion(total_count=10, rating=5.0), randx=0),
            ModelGroup(hid=2, hyperid=4002, title="model_group", opinion=Opinion(total_count=20, rating=4.0), randx=1),
            ModelGroup(hid=2, hyperid=4003, title="model_group", opinion=Opinion(total_count=30, rating=3.0), randx=2),
            ModelGroup(hid=2, hyperid=4004, title="model_group", opinion=Opinion(total_count=40, rating=2.0), randx=3),
            ModelGroup(hid=2, hyperid=4005, title="model_group", opinion=Opinion(total_count=50, rating=1.0), randx=4),
        ]

        cls.index.models += [
            Model(hid=2, hyperid=40011, group_hyperid=4001, title="modification without opinion", randx=200),
            Model(
                hid=2,
                hyperid=40012,
                group_hyperid=4001,
                title="modification with opinion",
                opinion=Opinion(total_count=3, rating=3.5),
                randx=100,
            ),
        ]

        for hyper in list(range(4001, 4005 + 1)) + [40011, 40012]:
            cls.index.offers += [Offer(hid=2, hyperid=hyper, glparams=[GLParam(param_id=1, value=1)], randx=1)]

        cls.index.shops += [
            Shop(fesh=10, new_shop_rating=NewShopRating(new_rating_total=1.0)),
            Shop(fesh=222, priority_region=213, regions=[213]),
        ]

        # test_sort_by_date
        for i in range(5):
            cls.index.models += [
                Model(title='newnessmodel', hyperid=400 + i, hid=10, created_ts=get_timestamp(2016, 5, i + 1))
            ]
            cls.index.vclusters += [
                VCluster(
                    title='newnesscluster', vclusterid=1000000400 + i, hid=20, created_ts=get_timestamp(2016, 5, i + 1)
                )
            ]
            cls.index.model_groups += [
                ModelGroup(
                    title='newnessgroup',
                    hyperid=500 + i,
                    hid=30,
                    created_ts=get_timestamp(2016, 5, i + 1),
                    group_size=3,
                )
            ]
            cls.index.offers += [Offer(title='newnessoffer', hid=10)]
            cls.index.offers += [Offer(fesh=222, vclusterid=1000000400 + i)]

    @classmethod
    def prepare_sort_quality_opinions(cls):
        cls.index.hypertree += [
            HyperCategory(hid=5, visual=True),
        ]

        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[213], new_shop_rating=NewShopRating(new_rating_total=4.0)),
            Shop(fesh=2, priority_region=213, regions=[213], new_shop_rating=NewShopRating(new_rating_total=5.0)),
        ]

        cls.index.gltypes += [GLType(hid=5, param_id=1, values=[1, 2], cluster_filter=True)]

        cls.index.model_groups += [
            ModelGroup(
                hid=5,
                hyperid=5100,
                ts=5100,
                randx=20,
                opinion=Opinion(total_count=120, rating=4.75),
                title="test quality opinions: group model 5100 with rating 4.75 (~5.0)",
            ),
        ]

        cls.index.models += [
            Model(
                hid=5,
                hyperid=5000,
                ts=5000,
                randx=10,
                group_hyperid=5100,  # рейтинг берется от групповой модели
                title="test quality opinions: modification of model 5100 with rating 4.75 (~5.0)",
            ),
            Model(
                hid=5,
                hyperid=5001,
                ts=5001,
                randx=11,
                opinion=Opinion(total_count=110, rating=4.74),
                title="test quality opinions: model with rating 4.74 (~4.5)",
            ),
            Model(
                hid=5,
                hyperid=5002,
                ts=5002,
                randx=12,
                opinion=Opinion(total_count=10, rating=4.76),
                title="test quality opinions: model with rating 4.76 (~5.0)",
            ),
            Model(
                hid=5,
                hyperid=5003,
                ts=5003,
                randx=13,
                opinion=Opinion(total_count=20, rating=3.2),
                title="test quality opinions: model with rating 3.2 (~3.0) with one offer",
            ),
            Model(
                hid=5,
                hyperid=5004,
                ts=5004,
                randx=14,
                opinion=Opinion(total_count=20, rating=2.9),
                title="test quality opinions: model with rating 2.9 (~3.0) with many offers",
            ),
            Model(hid=5, hyperid=5005, ts=5005, randx=15, title="test quality opinions: model without opinions"),
        ]

        cls.index.vclusters += [
            VCluster(
                hid=5,
                vclusterid=1000005000,
                ts=5006,
                randx=16,
                opinion=Opinion(total_count=1, rating=4.25),
                title="test quality opinions: model (cluster) with rating 4.25 (~4.5)",
            )
        ]

        for hyper in [5000, 5003, 5004, 5005, 5100]:
            cls.index.offers += [Offer(hid=5, fesh=1, hyperid=hyper, randx=1, glparams=[GLParam(param_id=1, value=1)])]

        cls.index.offers += [
            Offer(
                hid=5,
                ts=50000,
                fesh=1,
                randx=2,
                title="test quality opinions: offer not related with model from shop 1 (rating 4)",
                glparams=[GLParam(param_id=1, value=1)],
            ),
            Offer(
                hid=5,
                hyperid=5001,
                ts=50001,
                fesh=1,
                randx=3,
                title="test quality opinions: offer of hyperid 5001 (~4.5) from shop 1 (rating 4)",
                glparams=[GLParam(param_id=1, value=1)],
            ),
            Offer(
                hid=5,
                hyperid=5002,
                ts=50002,
                fesh=2,
                randx=4,
                title="test quality opinions: offer of hyperid 5002 (~5.0) from shop 2 (rating 5)",
                glparams=[GLParam(param_id=1, value=1)],
            ),
            Offer(
                hid=5,
                hyperid=1000005000,
                ts=50006,
                fesh=2,
                randx=5,
                title="test quality opinions: offer of cluster with (~4.5) from shop 2 (rating 5)",
                glparams=[GLParam(param_id=1, value=1)],
            ),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=5000, offers=200),
            RegionalModel(hyperid=5001, offers=40),
            RegionalModel(hyperid=5002, offers=160),
            RegionalModel(hyperid=5003, offers=1),
            # задаем статистики в разных регионах чтобы проверить какая именно применится
            RegionalModel(hyperid=5004, offers=1000, rids=[191]),
            RegionalModel(hyperid=5004, offers=2000, rids=[213]),
            RegionalModel(hyperid=5004, offers=3000, rids=[-1]),  # должна использоваться именно эта статистика
            RegionalModel(hyperid=1000005000, offers=15),
        ]

    def test_guru_popularity(self):
        def model(id, guru_popularity):
            return {
                "id": id,
                "entity": "product",
                "debug": {
                    "rank": [
                        {"name": "HAS_PICTURE", "value": "1"},
                        {
                            "name": "DELIVERY_TYPE",
                            "value": "1",
                        },  # delivery_type = Exists for models because we use rids=0
                        {"name": "IS_MODEL", "value": "1"},
                        {"name": "GURU_POPULARITY", "value": str(guru_popularity)},
                        {"name": "ONSTOCK"},
                        {"name": "RANDX"},
                    ]
                },
            }

        response = self.report.request_json('place=prime&hid=1&numdoc=12&how=guru_popularity&debug=da')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    model(id=302, guru_popularity=60),
                    model(id=301, guru_popularity=20),
                    model(id=1000000103, guru_popularity=9),
                    model(id=303, guru_popularity=8),
                    model(id=1000000101, guru_popularity=5),
                    model(id=307, guru_popularity=3),
                    model(id=306, guru_popularity=1),
                    model(id=304, guru_popularity=0),  # no model_clicks
                    model(id=1000000102, guru_popularity=0),  # too low guru_popularity
                    model(id=305, guru_popularity=0),  # too low guru_popularity
                    model(id=308, guru_popularity=0),  # no offers
                    {"entity": "offer"},
                ]
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_pessimize_no_picture(cls):
        cls.index.hypertree += [
            HyperCategory(hid=200200, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=200201, output_type=HyperCategoryType.CLUSTERS),
            HyperCategory(hid=200202, output_type=HyperCategoryType.SIMPLE),
        ]

        cls.index.models += [Model(hid=200200, title="has_not_picture", no_picture=True, no_add_picture=True)]
        cls.index.models += [Model(hid=200200, title="has_picture", no_picture=False)]

        cls.index.models += [Model(hid=200201, title="has_not_picture", no_picture=True, no_add_picture=True)]
        cls.index.models += [Model(hid=200201, title="has_picture", no_picture=False)]

    def test_pessimize_no_picture(self):
        response = self.report.request_json("place=prime&hid=200200&debug=da")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "titles": {"raw": "has_picture"}},
                    {"entity": "product", "titles": {"raw": "has_not_picture"}},
                ]
            },
            preserve_order=True,
        )
        self.assertFragmentIn(
            response,
            {
                "name": "HAS_PICTURE",
                "width": "1",
            },
            preserve_order=True,
        )

        response = self.report.request_json("place=prime&hid=200201&debug=da")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "titles": {"raw": "has_picture"}},
                    {"entity": "product", "titles": {"raw": "has_not_picture"}},
                ]
            },
            preserve_order=True,
        )
        self.assertFragmentIn(
            response,
            {
                "name": "HAS_PICTURE",
                "width": "1",
            },
            preserve_order=True,
        )

    def test_do_not_pessimize_no_picture(self):
        response = self.report.request_json("place=prime&hid=200200&debug=da&pessimize-offers-without-picture=0")
        self.assertFragmentNotIn(
            response,
            {
                "name": "HAS_PICTURE",
                "width": "1",
            },
            preserve_order=True,
        )

        response = self.report.request_json("place=prime&hid=200201&debug=da&pessimize-offers-without-picture=0")
        self.assertFragmentNotIn(
            response,
            {
                "name": "HAS_PICTURE",
                "width": "1",
            },
            preserve_order=True,
        )

    def test_opinions(self):
        # сортировка по количеству отзывов

        def rank(opinions):
            return {
                "rank": [
                    {"name": "DELIVERY_TYPE", "value": "1"},
                    {"name": "OPINIONS", "value": str(opinions)},
                    {"name": "ONSTOCK", "value": "1"},
                    {"name": "RANDX"},
                ]
            }

        response = self.report.request_json('place=prime&hid=1&numdoc=18&how=opinions&debug=da')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 306, "entity": "product", "debug": rank(opinions=40)},
                    {"entity": "offer", "model": {"id": 306}, "debug": rank(opinions=40)},
                    {"id": 1000000103, "entity": "product", "debug": rank(opinions=15)},
                    {"entity": "offer", "model": {"id": 1000000103}, "debug": rank(opinions=15)},
                    {"id": 302, "entity": "product", "debug": rank(opinions=14)},
                    {"entity": "offer", "model": {"id": 302}, "debug": rank(opinions=14)},
                    {"entity": "offer", "model": {"id": 302}, "debug": rank(opinions=14)},
                    {"id": 1000000101, "entity": "product", "debug": rank(opinions=11)},
                    {"entity": "offer", "model": {"id": 1000000101}, "debug": rank(opinions=11)},
                    {"id": 301, "entity": "product", "debug": rank(opinions=10)},
                    {"entity": "offer", "model": {"id": 301}, "debug": rank(opinions=10)},
                    {"entity": "offer", "model": {"id": 301}, "debug": rank(opinions=10)},
                    {"id": 304, "entity": "product", "debug": rank(opinions=9)},
                    {"entity": "offer", "model": {"id": 304}, "debug": rank(opinions=9)},
                    {"id": 1000000102, "entity": "product", "debug": rank(opinions=7)},
                    {"id": 303, "entity": "product", "debug": rank(opinions=7)},
                    {"entity": "offer", "model": {"id": 1000000102}, "debug": rank(opinions=7)},
                    {"entity": "offer", "model": {"id": 303}, "debug": rank(opinions=7)},
                ]
            },
            preserve_order=True,
        )

    def test_quality_by_precise_rating(self):
        response = self.report.request_json('place=prime&hid=1&numdoc=12&how=quality&allow-collapsing=1')
        self.assertFragmentIn(
            response,
            [
                {"id": 301, "entity": "product", "rating": 5, "preciseRating": 4.9},
                {"id": 302, "entity": "product", "rating": 5, "preciseRating": 4.8},
                {"id": 303, "entity": "product", "rating": 4, "preciseRating": 4.2},
                {"id": 304, "entity": "product", "rating": 3, "preciseRating": 2.9},
                {"id": 306, "entity": "product", "rating": 1.5, "preciseRating": 1.7},
            ],
            preserve_order=True,
        )

    def test_quality_opinions(self):
        """Сотрировка по отзывам покупателей"""

        response = self.report.request_json(
            'place=prime&text=test+quality+opinions&how=quality_opinions&rids=213&numdoc=20&debug=da&debug-doc-count=20&local-offers-first=0&rearr-factors=disable_panther_quorum=0&glfilter=1:1&hid=5'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        # модели и кластера
                        {
                            "titles": {
                                "raw": "test quality opinions: modification of model 5100 with rating 4.75 (~5.0)"
                            },
                            "id": 5000,
                        },
                        {
                            "titles": {"raw": "test quality opinions: group model 5100 with rating 4.75 (~5.0)"},
                            "id": 5100,
                        },
                        {"titles": {"raw": "test quality opinions: model with rating 4.76 (~5.0)"}, "id": 5002},
                        # оффер - рейтинг документа = рейтинг магазина
                        {
                            "titles": {
                                "raw": "test quality opinions: offer of hyperid 5002 (~5.0) from shop 2 (rating 5)"
                            }
                        },
                        {
                            "titles": {
                                "raw": "test quality opinions: offer of cluster with (~4.5) from shop 2 (rating 5)"
                            }
                        },
                        {"titles": {"raw": "test quality opinions: model with rating 4.74 (~4.5)"}, "id": 5001},
                        {
                            "titles": {"raw": "test quality opinions: model (cluster) with rating 4.25 (~4.5)"},
                            "id": 1000005000,
                        },
                        # офферы - рейтинг документа = рейтинг магазина
                        {
                            "titles": {
                                "raw": "test quality opinions: offer of hyperid 5001 (~4.5) from shop 1 (rating 4)"
                            }
                        },
                        {
                            "titles": {
                                "raw": "test quality opinions: offer not related with model from shop 1 (rating 4)"
                            }
                        },
                        {
                            "titles": {"raw": "test quality opinions: model with rating 2.9 (~3.0) with many offers"},
                            "id": 5004,
                        },
                        {
                            "titles": {"raw": "test quality opinions: model with rating 3.2 (~3.0) with one offer"},
                            "id": 5003,
                        },
                        {"titles": {"raw": "test quality opinions: model without opinions"}, "id": 5005},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "rank": [
                    {"name": "DELIVERY_TYPE"},
                    {"name": "DOCUMENT_RATING_10"},
                    {"name": "OPINIONS_WITH_PREVALENCE"},
                    {"name": "ONSTOCK"},
                    {"name": "GURU_POPULARITY"},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # Модели 5100 (group) 5000 (modification) и 5002 c рейтингом ~5.0 (DOCUMENT_RATING_10 = 10)

        self.assertFragmentIn(
            response,
            {
                "properties": {
                    "TS": "5000",
                    "TOTAL_OFFER_COUNT": "201",
                    "OPINIONS": "120",  # отзывы берутся от групповой модели
                    "PREVALENCE": "1",
                },
                "rank": [
                    {"name": "DOCUMENT_RATING_10", "value": "10"},
                    {"name": "OPINIONS_WITH_PREVALENCE", "value": "12100"},  # (120+1)*100*PREVALENCE
                ],
            },
        )

        self.assertFragmentIn(
            response,
            {
                "properties": {
                    "TS": "5100",
                    "TOTAL_OFFER_COUNT": "202",
                    "OPINIONS": "120",
                    "PREVALENCE": "1",
                },
                "rank": [
                    {"name": "DOCUMENT_RATING_10", "value": "10"},
                    {"name": "OPINIONS_WITH_PREVALENCE", "value": "12100"},  # (120+1)*100*PREVALENCE
                ],
            },
        )

        self.assertFragmentIn(
            response,
            {
                "properties": {
                    "TS": "5002",
                    "TOTAL_OFFER_COUNT": "161",
                    "OPINIONS": "10",
                    "PREVALENCE": Round(0.9999),
                },
                "rank": [
                    {"name": "DOCUMENT_RATING_10", "value": "10"},
                    {"name": "OPINIONS_WITH_PREVALENCE", "value": "1099"},  # [(10+1)*100*0.9999] = 10
                ],
            },
        )

        # Модель 5001 и кластер 1000005000 с рейтингом ~4.5 (DOCUMENT_RATING_10 = 9)
        self.assertFragmentIn(
            response,
            {
                "properties": {
                    "TS": "5001",
                    "TOTAL_OFFER_COUNT": "41",
                    "OPINIONS": "110",
                    "PREVALENCE": Round(0.8578),
                },
                "rank": [
                    {"name": "DOCUMENT_RATING_10", "value": "9"},
                    {"name": "OPINIONS_WITH_PREVALENCE", "value": "9521"},  # [(110+1)*100*0.8578] = 95
                ],
            },
        )

        self.assertFragmentIn(
            response,
            {
                "properties": {
                    "TS": "5006",
                    "TOTAL_OFFER_COUNT": "16",
                    "OPINIONS": "1",
                    "PREVALENCE": Round(0.3312),
                },
                "rank": [
                    {"name": "DOCUMENT_RATING_10", "value": "9"},
                    {"name": "OPINIONS_WITH_PREVALENCE", "value": "66"},
                ],
            },
        )

        # Модели 5003, 5004, с рейтингом ~3.0 (DOCUMENT_RATING_10 = 6)
        self.assertFragmentIn(
            response,
            {
                "properties": {
                    "TS": "5004",
                    "TOTAL_OFFER_COUNT": "3000",  # используется статистика по всем регионам
                    "OPINIONS": "20",
                    "PREVALENCE": "1",
                },
                "rank": [
                    {"name": "DOCUMENT_RATING_10", "value": "6"},
                    {"name": "OPINIONS_WITH_PREVALENCE", "value": "2100"},
                ],
            },
        )

        # prevalence = e ^ (x / 10) / (e ^ (x / 10) + 10) = [ x = TOTAL_OFFER_COUNT = 2] = Round(0.1088)
        self.assertFragmentIn(
            response,
            {
                "properties": {"TS": "5003", "TOTAL_OFFER_COUNT": "2", "OPINIONS": "20", "PREVALENCE": Round(0.1088)},
                "rank": [
                    {"name": "DOCUMENT_RATING_10", "value": "6"},
                    {"name": "OPINIONS_WITH_PREVALENCE", "value": "228"},
                ],
            },
        )

        # Модель 5005 без отзывов и рейтинга
        # prevalence = e ^ (x / 10) / (e ^ (x / 10) + 10) = [ x = TOTAL_OFFER_COUNT = 1] = Round(0.0995)
        self.assertFragmentIn(
            response,
            {
                "properties": {"TS": "5005", "PREVALENCE": Round(0.0995)},
                "rank": [
                    {"name": "DOCUMENT_RATING_10", "value": "0"},
                    {"name": "OPINIONS_WITH_PREVALENCE", "value": "9"},  # (0+1)*100*0.0909
                ],
            },
        )

        # Офферы
        self.assertFragmentIn(
            response,
            {
                "properties": {
                    "TS": "50001",
                    "TOTAL_OFFER_COUNT": "1",
                    "OPINIONS": "110",  # количество отзывов о связанной модели 5001
                    "PREVALENCE": Round(0.0995),
                },
                "rank": [
                    {"name": "DOCUMENT_RATING_10", "value": "8"},  # рейтинг магазина 4.0
                    {"name": "OPINIONS_WITH_PREVALENCE", "value": "1104"},
                ],
            },
        )

        # оффер без модели
        self.assertFragmentIn(
            response,
            {
                "properties": {
                    "TS": "50000",
                    "TOTAL_OFFER_COUNT": "1",
                    "OPINIONS": "0",  # количество отзывов о связанной модели 5001
                    "PREVALENCE": Round(0.0995),
                },
                "rank": [
                    {"name": "DOCUMENT_RATING_10", "value": "8"},  # рейтинг магазина 4.0
                    {"name": "OPINIONS_WITH_PREVALENCE", "value": "9"},  # (0+1)*100*0.0995
                ],
            },
        )

    def test_quality_opinions_sorting_params(self):
        """Проверяем что коэффициенты y и z задаваемые флагом market_quality_opinions_sorting_params=y,z
        влияют не вычисление PREVALENCE по формуле k = (e^(x/z))/((e^(x/z))+y) где x = TOTAL_OFFER_COUNT
        дефолтные значения y=10, z=10
        """
        request = (
            'place=prime&text=quality+opinions&how=quality_opinions&rids=213&numdoc=20&debug=da&debug-doc-count=20'
        )
        responseDefault = self.report.request_json(request)
        responseFactor = self.report.request_json(
            request + '&rearr-factor=market_quality_opinions_sorting_params=100.0,5.0'
        )
        # y = 100.0 z = 5.0
        self.assertFragmentIn(
            responseDefault,
            {
                "properties": {
                    "TS": "5001",
                    "TOTAL_OFFER_COUNT": "41",
                    "PREVALENCE": Round(0.8578)
                    # x/z = 41/10 = 4.1  exp(4.1) ~ 60.34  prevalence = 60.34/(60.34 + 10) ~ 0.8578
                }
            },
        )
        self.assertFragmentIn(
            responseFactor,
            {
                "properties": {
                    "TS": "5001",
                    "TOTAL_OFFER_COUNT": "41",
                    # TODO uncomment this after MARKETOUT-13898
                    # "PREVALENCE": Round(0.9732)
                    # x/z = 41/5 = 8.2  exp(8.2) ~ 3640.95  prevalence = 3640.95/(3640.95 + 100) ~ 0.9732
                }
            },
        )

    def test_quality_group_models_and_modifications(self):
        """Для модификаций данные об отзывах/рейтингах берутся из групповой модели"""
        response = self.report.request_json('place=prime&hid=2&how=quality&glfilter=1:1&debug=da&entities=product')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 40011, "rating": 5, "titles": {"raw": "modification without opinion"}},
                    {"id": 40012, "rating": 5, "titles": {"raw": "modification with opinion"}},
                    {"id": 4001, "rating": 5},
                    {"id": 4002, "rating": 4},
                    {"id": 4003, "rating": 3},
                    {"id": 4004, "rating": 2},
                    {"id": 4005, "rating": 1},
                ]
            },
            preserve_order=True,
        )

    def test_default_sorting_name(self):
        response = self.report.request_json('place=prime&hid=1')
        self.assertFragmentIn(
            response, {"sorts": [{"text": u"по популярности"}]}, preserve_order=True, allow_different_len=True
        )

        response = self.report.request_json('place=prime&hid=1&text=doc')
        self.assertFragmentIn(
            response, {"sorts": [{"text": u"по популярности"}]}, preserve_order=True, allow_different_len=True
        )

    def test_sorts_with_models_and_no_hid(self):
        '''Тестируем, что если смешанная выдача содержит модели, то есть сортировка по умолчанию
        Сортировка по цене показывается только под флагом market_render_price_sort_on_search
        WARNING: этот тест падает при выкатке новой формулы, т.к. модели нет на первой странице
                 при запросе place=prime&text=doc
                 однако, если выставить numdoc=20 то модель оказыается на первой позиции
                 у нас какие-то проблемы с пересортировкой на мете - модели нет на первой странице,
                 а когда пользователь переходит на вторую страницу - модель оказывается внезапно на первой
                 @see https://st.yandex-team.ru/MARKETOUT-12471
        '''
        response = self.report.request_json('place=prime&text=doc&numdoc=20')
        self.assertFragmentIn(response, {"entity": "product"}, preserve_order=True)
        self.assertFragmentIn(
            response,
            {
                "sorts": [
                    {"text": u"по популярности"},
                    {
                        "text": u"по цене",
                        "options": [{"id": "aprice", "type": "asc"}, {"id": "dprice", "type": "desc"}],
                    },
                ]
            },
            preserve_order=True,
        )

    def test_sorts_with_models_and_hid(self):
        response = self.report.request_json('place=prime&text=doc&hid=1')
        self.assertFragmentIn(response, {"entity": "product"}, preserve_order=True)
        self.assertFragmentIn(
            response,
            {
                "sorts": [
                    {"text": u"по популярности"},
                    {
                        "text": u"по цене",
                        "options": [{"id": "aprice", "type": "asc"}, {"id": "dprice", "type": "desc"}],
                    },
                    {"text": u"по рейтингу", "options": [{"id": "quality"}]},
                    {"text": u"по отзывам", "options": [{"id": "opinions"}]},
                ]
            },
            preserve_order=True,
        )

    def test_sorts_with_offers_only(self):
        response = self.report.request_json('place=prime&text=offerdoc')
        self.assertFragmentNotIn(response, {"entity": "product"}, preserve_order=True)
        self.assertFragmentIn(
            response,
            {
                "sorts": [
                    {"text": u"по популярности"},
                    {
                        "text": u"по цене",
                        "options": [{"id": "aprice", "type": "asc"}, {"id": "dprice", "type": "desc"}],
                    },
                    {"text": u"по рейтингу", "options": [{"id": "rorp"}]},
                ]
            },
            preserve_order=True,
        )

    def test_sorts_with_group_models(self):
        response = self.report.request_json('place=prime&text=model_group&hid=2')
        self.assertFragmentIn(
            response,
            {
                "sorts": [
                    {"text": u"по рейтингу", "options": [{"id": "quality"}]},
                    {"text": u"по отзывам", "options": [{"id": "opinions"}]},
                ]
            },
            preserve_order=False,
        )

    def test_sorts_order(self):
        """Флаг market_sortings_order_in_prime=sort1,sort2,...,sortn влияет на порядок и наличие сортировок в prime"""
        response = self.report.request_json('place=prime&text=doc&hid=1')
        self.assertFragmentIn(
            response,
            {
                "sorts": [
                    {"text": u"по популярности"},
                    {"text": u"по цене"},
                    {"text": u"по рейтингу"},
                    {"text": u"по отзывам"},
                    {"text": u"по размеру скидки"},
                    {"text": u"по новизне"},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        order = '&rearr-factors=market_sortings_order_in_prime=opinions,quality,rorp,aprice,default,discount_p,ddate'
        response = self.report.request_json('place=prime&text=doc&hid=1' + order)
        self.assertFragmentIn(
            response,
            {
                "sorts": [
                    {"text": u"по отзывам"},
                    {"text": u"по рейтингу"},
                    {"text": u"по цене"},
                    {"text": u"по популярности"},
                    {"text": u"по размеру скидки"},
                    {"text": u"по новизне"},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        order = '&rearr-factors=market_sortings_order_in_prime=default,quality_opinions'
        response = self.report.request_json('place=prime&text=doc&hid=1' + order)
        self.assertFragmentIn(
            response,
            {"sorts": [{"text": u"по популярности"}, {"text": u"по отзывам покупателей"}]},
            preserve_order=True,
            allow_different_len=False,
        )

    def test_model_group_quality(self):
        response = self.report.request_json('place=prime&hid=2&numdoc=9&how=quality&entities=product')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 4001, "entity": "product"},
                    {"id": 4002, "entity": "product"},
                    {"id": 4003, "entity": "product"},
                    {"id": 4004, "entity": "product"},
                    {"id": 4005, "entity": "product"},
                ]
            },
            preserve_order=True,
        )

    def test_model_group_opinions(self):
        response = self.report.request_json('place=prime&hid=2&numdoc=9&how=opinions&entities=product')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 4005, "entity": "product"},
                    {"id": 4004, "entity": "product"},
                    {"id": 4003, "entity": "product"},
                    {"id": 4002, "entity": "product"},
                    {"id": 4001, "entity": "product"},
                ]
            },
            preserve_order=True,
        )

    def test_vcluster_dups(self):
        response = self.report.request_json('place=prime&text=vcluster')
        self.assertEqual(response.count({"id": 1000000101}), 1)

    def test_sort_by_date(self):
        model_response = self.report.request_json('place=prime&text=newnessmodel&how=ddate&hid=10&debug=da')
        self.assertFragmentIn(
            model_response,
            {
                "results": [
                    {"id": 404, "entity": "product"},
                    {"id": 403, "entity": "product"},
                    {"id": 402, "entity": "product"},
                    {"id": 401, "entity": "product"},
                    {"id": 400, "entity": "product"},
                ]
            },
            preserve_order=True,
        )

        self.assertFragmentIn(
            model_response,
            {
                "rank": [
                    {"name": "DELIVERY_TYPE"},
                    {"name": "IS_MODEL"},
                    {"name": "DATE"},
                    {"name": "GURU_POPULARITY"},
                    {"name": "ONSTOCK"},
                    {"name": "RANDX"},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.assertFragmentIn(
            model_response, {"sorts": [{"text": "по новизне", "options": [{"id": "ddate", "isActive": True}]}]}
        )

        cluster_response = self.report.request_json('place=prime&text=newnesscluster&how=ddate&hid=20')
        self.assertFragmentIn(
            cluster_response,
            {
                "results": [
                    {"id": 1000000404, "entity": "product"},
                    {"id": 1000000403, "entity": "product"},
                    {"id": 1000000402, "entity": "product"},
                    {"id": 1000000401, "entity": "product"},
                    {"id": 1000000400, "entity": "product"},
                ]
            },
            preserve_order=True,
        )

        self.assertFragmentIn(
            cluster_response, {"sorts": [{"text": "по новизне", "options": [{"id": "ddate", "isActive": True}]}]}
        )

        modelgroup_response = self.report.request_json('place=prime&text=newnessgroup&how=ddate&hid=30')
        self.assertFragmentIn(
            modelgroup_response,
            {
                "results": [
                    {"id": 504, "entity": "product"},
                    {"id": 503, "entity": "product"},
                    {"id": 502, "entity": "product"},
                    {"id": 501, "entity": "product"},
                    {"id": 500, "entity": "product"},
                ]
            },
            preserve_order=True,
        )

        self.assertFragmentIn(
            modelgroup_response, {"sorts": [{"text": "по новизне", "options": [{"id": "ddate", "isActive": True}]}]}
        )

    def test_sort_by_date_visible(self):
        model_response = self.report.request_json('place=prime&text=newnessmodel&hid=10')
        self.assertFragmentIn(
            model_response, {"sorts": [{"text": "по новизне", "options": [{"id": "ddate", "isActive": Absent()}]}]}
        )

    def test_sort_blue_market(self):
        """
        Что тестируем:
        1. на синем маркете скрыта сортировка "по новизне" (ddate)
        2. доступны сортировки "по отзывам", "по рейтингу"
        3. срыта сортировка rorp (по рейтингу и цене), т.к. рейтинг магазина (у нас один магазин)
        """
        model_response = self.report.request_json('place=prime&text=newnessmodel&hid=10&rgb=blue')

        self.assertFragmentNotIn(
            model_response,
            {
                "sorts": [
                    {
                        "options": [
                            {
                                "id": "ddate",
                            }
                        ]
                    }
                ]
            },
        )

        self.assertFragmentNotIn(
            model_response,
            {
                "sorts": [
                    {
                        "options": [
                            {
                                "id": "rorp",
                            }
                        ]
                    }
                ]
            },
        )

        self.assertFragmentIn(
            model_response,
            {
                "sorts": [
                    {
                        "options": [
                            {"id": "opinions"},
                        ]
                    },
                    {
                        "options": [
                            {"id": "quality"},
                        ]
                    },
                ]
            },
        )

    def test_sort_by_date_hidden(self):
        without_hid_model_response = self.report.request_json('place=prime&text=newness_model')
        self.assertFragmentNotIn(without_hid_model_response, {"sorts": [{"options": [{"id": "ddate"}]}]})

        offer_only_response = self.report.request_json('place=prime&text=newness_offer&hid=10')
        self.assertFragmentNotIn(offer_only_response, {"sorts": [{"options": [{"id": "ddate"}]}]})

    def test_price_sort_visability(self):
        # По умолчанию на безкатегорийном текстовом поиске сортировку показываем
        response = self.report.request_json("place=prime&text=doc")
        self.assertFragmentIn(
            response,
            {
                "sorts": [
                    {"text": "по цене"},
                ],
            },
        )

        # На всякий случай проверяю явное отключение сортировки
        response = self.report.request_json("place=prime&text=doc&rearr-factors=market_render_price_sort_on_search=0")
        self.assertFragmentNotIn(
            response,
            {
                "sorts": [
                    {"text": "по цене"},
                ],
            },
        )

        # Под флагом на безкатегорийном текстовом поиске сортировку показываем
        response = self.report.request_json("place=prime&text=doc&rearr-factors=market_render_price_sort_on_search=1")
        self.assertFragmentIn(
            response,
            {
                "sorts": [
                    {"text": "по цене"},
                ],
            },
        )

        # Если ищем по категории, то сортировку по цене показываем
        for query_params in ("hid=1", "hid=1&text=doc"):
            request = "platform=desktop&place=prime&" + query_params
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "sorts": [
                        {"text": "по цене"},
                    ],
                },
            )

            # Если ищем по категории, то флаг на отображение сортировки не влияет
            for flag_param in (
                "&rearr-factors=market_render_price_sort_on_search=1",
                "&rearr-factors=market_render_price_sort_on_search=0",
            ):
                response = self.report.request_json(request + flag_param)
                self.assertFragmentIn(
                    response,
                    {
                        "sorts": [
                            {"text": "по цене"},
                        ],
                    },
                )

    @classmethod
    def prepare_test_sort_by_offers_count(cls):
        """
        Создаем модели и групповую модель с разным количеством офферов + создаем оффер, чтобы
        охватить все сущности
        """
        cls.index.hypertree += [HyperCategory(hid=100, output_type=HyperCategoryType.GURU)]

        cls.index.models += [
            Model(hid=100, hyperid=1001),
            Model(hid=100, hyperid=1002),
            Model(hid=100, hyperid=1003),
        ]

        cls.index.model_groups += [ModelGroup(hid=100, hyperid=1000)]

        cls.index.regional_models += [
            RegionalModel(hyperid=1000, offers=2, local_offers=1, rids=[213]),
            RegionalModel(hyperid=1001, offers=3, local_offers=1, rids=[213]),
            RegionalModel(hyperid=1002, offers=4, local_offers=1, rids=[213]),
            RegionalModel(hyperid=1003, offers=5, local_offers=1, rids=[213]),
        ]

        cls.index.offers += [Offer(hid=100, hyperid=1000, fesh=1) for _ in range(2)]
        cls.index.offers += [Offer(hid=100, hyperid=1001, fesh=1) for _ in range(3)]
        cls.index.offers += [Offer(hid=100, hyperid=1002, fesh=1) for _ in range(4)]
        cls.index.offers += [Offer(hid=100, hyperid=1003, fesh=1) for _ in range(5)]

    def test_sort_by_offers_count(self):
        # Делаем запрос в категорию с сортировкой по количеству офферов. Ожидаем:
        # 1. Модели и групповые модели отсортированы по убыванию количества офферов
        # 2. Оффер является последним элементом в этой сортировке

        response = self.report.request_json(
            'place=prime&hid=100&how=dnoffers&rids=213&allow-collapsing=1&debug=da'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 1003},
                    {"id": 1002},
                    {"id": 1001},
                    {"id": 1000},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "rank": [
                    {"name": "IS_MODEL"},
                    {"name": "DELIVERY_TYPE"},
                    {"name": "NOFFERS"},
                    {"name": "RANDX"},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_bestseller(cls):
        cls.index.hypertree += [HyperCategory(hid=9, output_type=HyperCategoryType.GURU)]
        cls.index.offers += [
            Offer(title='offerdoc1', hid=9, fesh=90, hyperid=901),
            Offer(title='offerdoc2', hid=9, fesh=90, hyperid=901),
            Offer(title='offerdoc3', hid=9, fesh=90, hyperid=902),
            Offer(title='offerdoc4', hid=9, fesh=90, hyperid=902),
            Offer(title='offerdoc5', hid=9, fesh=90, hyperid=903),
            Offer(title='offerdoc6', hid=9, fesh=90, hyperid=903),
        ]
        cls.index.models += [
            Model(
                title='modeldoc1',
                hyperid=901,
                hid=9,
                model_clicks=1000,
                opinion=Opinion(total_count=10, rating=5.0),
                ts=501,
            ),
            Model(
                title='modeldoc2',
                hyperid=902,
                hid=9,
                model_clicks=1000,
                opinion=Opinion(total_count=10, rating=4.8),
                ts=502,
            ),
            Model(
                title='modeldoc3', hyperid=903, hid=9, model_clicks=3000, opinion=Opinion(total_count=14, rating=4.5)
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 501).respond(0.60)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 502).respond(0.80)

    def test_bestseller(self):
        response = self.report.request_json('place=prime&hid=9&numdoc=12&how=bestseller')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 903, "entity": "product"},
                    {"id": 901, "entity": "product"},
                    {"id": 902, "entity": "product"},
                    {"entity": "offer"},
                    {"entity": "offer"},
                    {"entity": "offer"},
                    {"entity": "offer"},
                    {"entity": "offer"},
                    {"entity": "offer"},
                ]
            },
            preserve_order=True,
        )

        response = self.report.request_json('place=prime&hid=9&numdoc=12&how=bestseller&use-relevance-in-bestsellers=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 903, "entity": "product"},
                    {"id": 902, "entity": "product"},
                    {"id": 901, "entity": "product"},
                    {"entity": "offer"},
                    {"entity": "offer"},
                    {"entity": "offer"},
                    {"entity": "offer"},
                    {"entity": "offer"},
                    {"entity": "offer"},
                ]
            },
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
