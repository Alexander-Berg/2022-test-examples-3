#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    CategoryRestriction,
    DeliveryBucket,
    DeliveryOption,
    Disclaimer,
    GLParam,
    GLType,
    GLValue,
    GradeDispersionItem,
    HyperCategory,
    MnPlace,
    Model,
    ModelDescriptionTemplates,
    ModelGroup,
    NavCategory,
    Offer,
    Outlet,
    PhotoDataItem,
    PickupBucket,
    PickupOption,
    Region,
    RegionalRestriction,
    ReviewDataItem,
    ReviewFactorItem,
    Shop,
    YamarecPlaceReasonsToBuy,
)
from core.testcase import TestCase, main
from core.types.picture import to_mbo_picture
from core.matcher import Absent, Contains, LikeUrl, NoKey
from core.blackbox import BlackboxUser
from core.report import REQUEST_TIMESTAMP
from core.types.delivery import BlueDeliveryTariff

from datetime import datetime, timedelta
from itertools import count

import json
import urllib

now = datetime.fromtimestamp(REQUEST_TIMESTAMP)
delta = timedelta(days=10)
nummer = count()


class T(TestCase):
    @classmethod
    def prepare_yamarec_data(cls):
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=0;enable_fast_promo_matcher_test=0']

        cls.index.yamarec_places += [
            YamarecPlaceReasonsToBuy()
            .new_partition("split=1")
            .add(
                hyperid=584110,
                reasons=[
                    {
                        "id": "positive_feedback",
                        "type": "consumerFactor",
                        "author_puid": "1001",
                        "text": "Уникальный аппарат, который уверенно смотрит в будущее",
                        "anonymous": False,
                        "value": 5.0,
                        "value_threshold": 4.0,
                    }
                ],
            )
            .add(
                hyperid=3478001,
                reasons=[
                    {
                        "id": "positive_feedback",
                        "type": "consumerfactor",
                        "author_puid": "1001",
                        "text": "text1",
                        "anonymous": "true",  # filtered
                        "value": 5.0,
                        "value_threshold": "4.0",
                    },
                    {  # passed
                        "id": "customers_choice",
                        "type": "consumerfactor",
                        "rating": "4.5",
                        "rating_threshold": "4.0",
                        "value": 0.95,
                        "recommenders_count": "100",
                        "share_threshold": "0.8",
                    },
                ],
            )
        ]

    @classmethod
    def prepare_model_right_incut(cls):

        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=[
                BlueDeliveryTariff(user_price=99, large_size=0, price_to=100),
                BlueDeliveryTariff(user_price=0, large_size=0),
                BlueDeliveryTariff(user_price=399, large_size=1),
            ],
            large_size_weight=20,
        )
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=[
                BlueDeliveryTariff(user_price=99, large_size=0, price_to=100),
                BlueDeliveryTariff(user_price=0, large_size=0),
                BlueDeliveryTariff(user_price=399, large_size=1),
            ],
            regions=[54],
            large_size_weight=20,
        )

        cls.index.hypertree += [
            HyperCategory(hid=1000, name='Root Category', children=[HyperCategory(hid=9900, name='Leaf Category')])
        ]

        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=9900,
                friendlymodel=[
                    ("Цвет", "{T1}"),
                    ("Диагональ", "{T2}"),
                ],
                model=[
                    (
                        "Внешний вид",
                        {
                            "Цвет": "{T1}",
                        },
                    ),
                    (
                        "Размеры",
                        {
                            "Диагональ": "{T2}",
                        },
                    ),
                ],
            )
        ]

        cls.index.gltypes += [
            GLType(
                param_id=600100,
                hid=9900,
                gltype=GLType.ENUM,
                xslname='T1',
                name=u'Цвет',
                values=[GLValue(value_id=1, text='Красный'), GLValue(value_id=2, text='Зеленый')],
            ),
            GLType(
                param_id=600101,
                hid=9900,
                gltype=GLType.ENUM,
                xslname='T2',
                name=u'Диагональ',
                values=[GLValue(value_id=1, text='12 дюймов'), GLValue(value_id=2, text='18 дюймов')],
            ),
        ]

        cls.index.regiontree += [Region(rid=213, name='Москва', genitive='Москвы', locative='Москве', preposition='в')]

        analogs_count = 4

        cls.index.models += [
            Model(
                hyperid=584110,
                hid=9900,
                title='Right Incut Model',
                picinfo='//avatars.mds.yandex.net/get-mpic/model_9900_584110/0/orig#100#100',
                proto_add_pictures=[
                    to_mbo_picture('//avatars.mds.yandex.net/get-mpic/model_9900_584110/1/orig#100#100'),
                    to_mbo_picture('//avatars.mds.yandex.net/get-mpic/model_9900_584110/2/orig#100#100'),
                    to_mbo_picture('//avatars.mds.yandex.net/get-mpic/model_9900_584110/3/orig#100#100'),
                ],
                glparams=[GLParam(param_id=600100, value=1), GLParam(param_id=600101, value=2)],
                analogs=[584111 + i for i in range(analogs_count)],
            ),
            Model(hyperid=585100, hid=9900, title="Banned Model"),
        ]

        cls.index.navtree += [
            NavCategory(nid=19900, hid=9900),
            NavCategory(nid=11000, hid=1000),
        ]

        cls.index.models += [
            Model(
                hyperid=584111 + i,
                hid=9900,
                title='Model Analog {}'.format(i),
                picinfo='//avatars.mds.yandex.net/get-mpic/model_9900_{}/0/orig#100#100'.format(584111 + i),
            )
            for i in range(analogs_count)
        ]

        cls.index.shops += [
            Shop(fesh=1, name="right incut shop", priority_region=213, pickup_buckets=[1001]),
        ]

        cls.index.outlets += [
            Outlet(fesh=1, region=213, point_id=1),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=1001,
                fesh=1,
                carriers=[99],
                options=[PickupOption(outlet_id=1)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            )
        ]

        cls.index.offers += [
            Offer(fesh=1, hyperid=584110, title='Right Incut Offer', price=101, pickup=True),
            Offer(fesh=1, hyperid=585100, title='Filetered Model Offer', price=103, pickup=True),
        ]

        for i in range(analogs_count):
            if i % 2 == 0:
                cls.index.offers.append(
                    Offer(fesh=1, hyperid=584111 + i, title='Right Incut Offer Analog {}'.format(i), price=102)
                )

        cls.index.model_reviews_data += [
            ReviewDataItem(
                review_id=9111,
                model_id=584110,
                author_id=9111,
                region_id=9112,
                cpa=True,
                anonymous=0,
                usage_time=2,
                pro="adv " + "cran" * 30,
                contra="disadv " + "cran" * 30,
                short_text="comment " + "cran" * 30,
                cr_time="2015-10-25T23:09:23",
                rank=0.8,
                agree=3,
                reject=4,
                total_votes=5,
                grade_value=2,
                author_reviews_count=21,
                photos=[
                    PhotoDataItem(group_id="photo_group_id1", image_name="image_name1"),
                    PhotoDataItem(group_id="photo_group_id2", image_name="image_name2"),
                ],
                most_useful=1,
            ),
            ReviewDataItem(
                review_id=9112,
                model_id=584110,
                author_id=9112,
                region_id=9114,
                cpa=True,
                anonymous=1,
                usage_time=1,
                pro="adv " + "ы" * 300,
                contra="disadv " + "ы" * 300,
                short_text="comment " + "ы" * 300,
                cr_time="2015-10-24T23:08:21",
                grade_value=2,
                rank=0.9,
                agree=8,
                reject=4,
                most_useful=1,
            ),
            ReviewDataItem(
                review_id=9113,
                model_id=584110,
                author_id=9113,
                region_id=9112,
                cpa=True,
                anonymous=0,
                usage_time=2,
                pro="adv " + "cran" * 31,
                contra="disadv " + "cran" * 31,
                short_text="comment " + "cran" * 31,
                cr_time="2015-10-25T23:09:25",
                rank=0.7,
                agree=3,
                reject=2,
                total_votes=5,
                grade_value=2,
                author_reviews_count=21,
            ),
        ]
        cls.index.model_grade_dispersion_data += [
            GradeDispersionItem(
                model_id=584110,
                one=1,
                two=2,
                three=3,
                four=4,
                five=5,
                factors=[
                    ReviewFactorItem(factor_id=1, factor_name="first", factor_avg=2.2, count=3),
                    ReviewFactorItem(factor_id=2, factor_name="second", factor_avg=3.3, count=4),
                ],
                recommend_percent=32.543,
            )
        ]

        cls.blackbox.on_request(uids=['9111', '9112', '9113']).respond(
            [
                BlackboxUser(uid='9113', name='', avatar='', public_id=''),
                BlackboxUser(uid='9112', name='name 1', avatar='avatar_1', public_id='public_id_1'),
                BlackboxUser(uid='9111', name='name 2', avatar='avatar_2', public_id='public_id_2'),
            ]
        )
        cls.blackbox.on_request(uids=['9111', '9113']).respond(
            [
                BlackboxUser(uid='9111', name='name 2', avatar='avatar_2', public_id='public_id_2'),
                BlackboxUser(uid='9113', name='', avatar='', public_id=''),
            ]
        )
        cls.blackbox.on_request(uids=['1001']).respond(
            [
                BlackboxUser(uid='1001', name='', avatar='', public_id=''),
            ]
        )

    def test_model_right_incut(self):
        """
        Проверяем правую врезку модельного колдунщика
        https://st.yandex-team.ru/MARKETOUT-26589
        """

        request = "place=parallel&text=Right+Incut&rearr-factors=market_enable_model_wizard_right_incut=1;split=1"
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_model_right_incut": {
                    "url": LikeUrl.of(
                        "//market.yandex.ru/product--right-incut-model/584110?hid=9900&nid=19900&text=right%20incut&lr=0&clid=632"
                    ),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/product--right-incut-model/584110?hid=9900&nid=19900&text=right%20incut&lr=0&clid=704"
                    ),
                    "subtype": "market_model_right_incut",
                    "type": "market_constr",
                    "reportDataRequestUrl": LikeUrl.of(
                        "/search/report_market?text=Right+Incut&market_hyperid=584110&market_hid=9900"
                    ),
                    "title": {"__hl": {"text": "Right Incut Model", "raw": True}},
                    "url_for_category_name": LikeUrl.of(
                        "//market.yandex.ru/catalog--leaf-category/19900?hid=9900&hyperid=584110&modelid=584110&text=right%20incut&lr=0&clid=632"
                    ),
                    "categoryUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/catalog?hid=9900&hyperid=584110&modelid=584110&nid=19900&text=right%20incut&lr=0&clid=704"
                    ),
                    "searchUrl": LikeUrl.of("//market.yandex.ru/search?text=right+incut&clid=632"),
                    "searchUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=right%20incut&lr=0&clid=704"),
                    "reviewsUrl": LikeUrl.of(
                        "//market.yandex.ru/product--right-incut-model/584110/reviews?text=right+incut&clid=632"
                    ),
                    "reviewsUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/product--right-incut-model/584110/reviews?text=right%20incut&lr=0&clid=704"
                    ),
                    "articlesUrl": LikeUrl.of(
                        "//market.yandex.ru/product--right-incut-model/584110/articles?text=right+incut&clid=632"
                    ),
                    "articlesUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/product--right-incut-model/584110/reviews?hid=9900&nid=19900&text=right%20incut&lr=0&clid=704"
                    ),
                    "pictures": [
                        LikeUrl.of("//avatars.mds.yandex.net/get-mpic/model_9900_584110/0/8hq"),
                        LikeUrl.of("//avatars.mds.yandex.net/get-mpic/model_9900_584110/1/8hq"),
                        LikeUrl.of("//avatars.mds.yandex.net/get-mpic/model_9900_584110/2/8hq"),
                        LikeUrl.of("//avatars.mds.yandex.net/get-mpic/model_9900_584110/3/8hq"),
                    ],
                    "categoryPath": [
                        {
                            "name": "Root Category",
                            "url": LikeUrl.of(
                                "//market.yandex.ru/catalog--root-category/11000?hid=1000&hyperid=584110&modelid=584110&text=right%20incut&clid=632&lr=0"
                            ),
                        },
                        {
                            "name": "Leaf Category",
                            "url": LikeUrl.of(
                                "//market.yandex.ru/catalog--leaf-category/19900?hid=9900&hyperid=584110&modelid=584110&text=right%20incut&clid=632&lr=0"
                            ),
                        },
                    ],
                    "reasonsToBuy": [
                        {
                            "author_puid": "1001",
                            "id": "positive_feedback",
                            "text": "Уникальный аппарат, который уверенно смотрит в будущее",
                            "type": "consumerFactor",
                        }
                    ],
                    "parameters": ["Красный", "18 дюймов"],
                    "reviews": [
                        {
                            "author": {"uid": "9112", "reviewsCount": "0", "anonymous": "1"},
                            "reviewData": {
                                "advantages": "adv " + "ы" * 300,
                                "disadvantages": "disadv " + "ы" * 300,
                                "comment": "comment " + "ы" * 300,
                                "rating": "2",
                                "usageTime": "несколько месяцев",
                            },
                            "reactions": {"likesCount": "8", "dislikesCount": "4"},
                            "url": LikeUrl.of(
                                "//market.yandex.ru/product--right-incut-model/584110/reviews?firstReviewId=9112&clid=632"
                            ),
                            "reviewId": "9112",
                            "rank": "0.9",
                            "time": "2015-10-24T20:08:21Z",
                        },
                        {
                            "author": {
                                "uid": "9111",
                                "name": "name 2",
                                "avatar": "avatar_2",
                                "publicId": "public_id_2",
                                "reviewsCount": "21",
                                "anonymous": "0",
                            },
                            "reviewData": {
                                "advantages": "adv " + "cran" * 30,
                                "disadvantages": "disadv " + "cran" * 30,
                                "comment": "comment " + "cran" * 30,
                                "rating": "2",
                                "usageTime": "более года",
                            },
                            "reactions": {"likesCount": "3", "dislikesCount": "4"},
                            "url": LikeUrl.of(
                                "//market.yandex.ru/product--right-incut-model/584110/reviews?firstReviewId=9111&clid=632"
                            ),
                            "reviewId": "9111",
                            "rank": "0.8",
                            "time": "2015-10-25T20:09:23Z",
                        },
                    ],
                    "showcase": {
                        "extra_models": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "Model Analog 0", "raw": True}},
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--model-analog-0/584111?hid=9900&clid=632"
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/product--model-analog-0/584111?hid=9900&nid=19900&lr=0&clid=704"
                                    ),
                                },
                                "price": {"currency": "RUR", "type": "min", "priceMin": "102", "priceMax": Absent()},
                                "thumb": {
                                    "source": LikeUrl.of("//avatars.mds.yandex.net/get-mpic/model_9900_584111/0/3hq"),
                                    "retinaSource": LikeUrl.of(
                                        "//avatars.mds.yandex.net/get-mpic/model_9900_584111/0/5hq"
                                    ),
                                },
                            },
                            {
                                "title": {
                                    "text": {"__hl": {"text": "Model Analog 2", "raw": True}},
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/product--model-analog-2/584113?hid=9900&clid=632"
                                    ),
                                },
                                "price": {"currency": "RUR", "type": "min", "priceMin": "102", "priceMax": Absent()},
                                "thumb": {
                                    "source": LikeUrl.of("//avatars.mds.yandex.net/get-mpic/model_9900_584113/0/3hq"),
                                    "retinaSource": LikeUrl.of(
                                        "//avatars.mds.yandex.net/get-mpic/model_9900_584113/0/5hq"
                                    ),
                                },
                            },
                        ]
                    },
                }
            },
        )
        # check access log
        self.access_log.expect(
            wizard_elements=Contains('market_model_right_incut'), wizards=Contains('market_model_right_incut')
        )

    def test_model_wizard_parameters(self):
        """Проверяем отображение характеристик модели как в КМ в модельном под конктрутором
        https://st.yandex-team.ru/MARKETOUT-29801
        """

        # по умолчанию market_model_wizard_show_parameters=1
        request = "place=parallel&text=Right+Incut&rearr-factors=showcase_universal_model=1"
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "title": {"__hl": {"text": "Right Incut Model", "raw": True}},
                    "parameters": ["Красный", "18 дюймов"],
                }
            },
        )

    def test_model_right_incut_full_params(self):
        """Проверяем наличие полных характеристик модели формируемых по шаблону MODEL
        https://st.yandex-team.ru/MARKETOUT-31418
        """
        request = "place=parallel&text=Right+Incut&rearr-factors=market_enable_model_wizard_right_incut=1"
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_model_right_incut": {
                    "title": {"__hl": {"text": "Right Incut Model", "raw": True}},
                    "fullParameters": [
                        {
                            "specName": "Внешний вид",
                            "params": [
                                {
                                    "paramName": "Цвет",
                                    "paramValue": "Красный",
                                    "paramDescription": "Цвет parameter description",  # В lite сейчас не реализована подстановка своего описания, и, кажется, в этом нет необходимости
                                }
                            ],
                        },
                        {
                            "specName": "Размеры",
                            "params": [
                                {
                                    "paramName": "Диагональ",
                                    "paramValue": "18 дюймов",
                                    "paramDescription": "Диагональ parameter description",  # В lite сейчас не реализована подстановка своего описания, и, кажется, в этом нет необходимости
                                }
                            ],
                        },
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_model_right_incut_filtering(self):
        """Проверка фильтрации модельного справа при отсутствии некоторых блоков
        https://st.yandex-team.ru/MARKETOUT-33104
        """

        request = "place=parallel&text=Banned&rearr-factors=market_enable_model_wizard_right_incut=1"
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(response, {"market_model": {}})
        self.assertFragmentIn(response, {"market_model_right_incut": {}})

        request = "place=parallel&text=Banned&rearr-factors=market_enable_model_wizard_right_incut=1;market_filter_model_wizard_right_incut_without_reviews=1"
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(response, {"market_model": {}})
        self.assertFragmentNotIn(response, {"market_model_right_incut": {}})

    def test_model_wizard_delivery(self):
        """Проверка доставки из других регионов в модельном под конктрутором
        https://st.yandex-team.ru/MARKETOUT-29925
        Проверка наличия поля самовывоз отдельно от другой информации по доставке
        https://st.yandex-team.ru/MARKETOUT-32046
        """

        request = "place=parallel&text=Right+Incut&rearr-factors=showcase_universal_model=1"
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "title": {"__hl": {"text": "Right Incut Model", "raw": True}},
                    "showcase": {
                        "items": [
                            {
                                "delivery": {
                                    "text": "Из Москвы",
                                    "pickup": "Самовывоз",
                                }
                            }
                        ]
                    },
                }
            },
        )

        request = "place=parallel&text=Right+Incut&rids=213&rearr-factors=showcase_universal_model=1"
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "title": {"__hl": {"text": "Right Incut Model", "raw": True}},
                    "showcase": {
                        "items": [
                            {
                                "delivery": {
                                    "currency": "RUR",
                                    "price": "100",
                                    "pickup": "Самовывоз",
                                    "text": NoKey("text"),
                                }
                            }
                        ]
                    },
                }
            },
        )

    @classmethod
    def prepare_models_for_title_no_vendor_no_category(cls):
        cls.index.models += [
            Model(hyperid=100100, title='Смартфон Apple iPhone 7 64GB', title_no_vendor='Смартфон iPhone 7 64GB'),
        ]
        cls.index.offers += [
            Offer(title='apple 1', hyperid=1, price=10),
        ]

    def test_model_title_no_vendor_no_category(self):
        """https://st.yandex-team.ru/MARKETOUT-33455
        Check how works deleting category and vendor from titles in model wizard
        """
        request = 'place=parallel&text=iphone&lr=213&rearr-factors=showcase_universal_model=1;'
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(response, {'market_model': {'title': {'__hl': {'text': 'Смартфон Apple iPhone 7 64GB'}}}})
        response = self.report.request_bs_pb(request + 'market_model_wizard_title_no_category=1;')
        self.assertFragmentIn(response, {'market_model': {'title': {'__hl': {'text': 'Apple iPhone 7 64GB'}}}})
        response = self.report.request_bs_pb(request + 'market_model_wizard_title_no_vendor=1;')
        self.assertFragmentIn(response, {'market_model': {'title': {'__hl': {'text': 'Смартфон iPhone 7 64GB'}}}})
        response = self.report.request_bs_pb(
            request + 'market_model_wizard_title_no_category=1;market_model_wizard_title_no_vendor=1;'
        )
        self.assertFragmentIn(response, {'market_model': {'title': {'__hl': {'text': 'iPhone 7 64GB'}}}})

        request += 'market_enable_model_wizard_right_incut=1;'
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response, {'market_model_right_incut': {'title': {'__hl': {'text': 'Смартфон Apple iPhone 7 64GB'}}}}
        )
        response = self.report.request_bs_pb(request + 'market_model_wizard_right_incut_title_no_category=1;')
        self.assertFragmentIn(
            response, {'market_model_right_incut': {'title': {'__hl': {'text': 'Apple iPhone 7 64GB'}}}}
        )
        response = self.report.request_bs_pb(request + 'market_model_wizard_right_incut_title_no_vendor=1;')
        self.assertFragmentIn(
            response, {'market_model_right_incut': {'title': {'__hl': {'text': 'Смартфон iPhone 7 64GB'}}}}
        )
        response = self.report.request_bs_pb(
            request
            + 'market_model_wizard_right_incut_title_no_category=1;market_model_wizard_right_incut_title_no_vendor=1;'
        )
        self.assertFragmentIn(response, {'market_model_right_incut': {'title': {'__hl': {'text': 'iPhone 7 64GB'}}}})

    def test_clid_in_category_path(self):
        """Проверяем, что в списке категорий в модельном справа правильные клиды в зависимости от платформы
        https://st.yandex-team.ru/MARKETOUT-37960
        """
        request = "place=parallel&text=Right+Incut&rearr-factors=market_enable_model_wizard_right_incut=1;"
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(response, {"market_model_right_incut": {"categoryPath": [{"url": Contains("clid=632")}]}})
        response = self.report.request_bs_pb(request + 'device=touch&touch=1')
        self.assertFragmentIn(response, {"market_model_right_incut": {"categoryPath": [{"url": Contains("clid=704")}]}})

    @classmethod
    def prepare_boost_meta_formula_by_multitoken(cls):
        cls.index.models += [
            Model(hyperid=101, ts=101, title="filter 1"),
            Model(hyperid=102, ts=102, title="filter fz-a60mfe"),
        ]

        cls.index.offers += [
            Offer(hyperid=101),
            Offer(hyperid=102),
        ]

        # Base formula
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 102).respond(0.8)

        # Meta formula
        cls.matrixnet.on_place(MnPlace.MODEL_WIZARD_META, 101).respond(0.6)
        cls.matrixnet.on_place(MnPlace.MODEL_WIZARD_META, 102).respond(0.5)

    def test_boost_meta_formula_by_multitoken(self):
        """Проверяем, что под флагом market_parallel_boost_completely_match_multitoken_coef в модельном колдунщике
        для модели с совпадением по мультитокену бустятся значения базовой и мета формул
        https://st.yandex-team.ru/MARKETOUT-31295
        """
        qtree = {
            'Market': {
                'qtree4market': 'cHic7VS_q9NgFL3nJi3hs0h58qAElFKXIDwIDrUIgnQqovh803uZGtFHF5c3lU6FJ1JExV8Iios_OlrKA8HFwc0x4O7g4Oq_4P2SL2mShgeC4GK23HO_85177knUFdVwqEktarPHPm2QSx06R-fpYlonj3y6XBvUtmmXhjTCY9Ar0FvQEegzSJ6voAi33OdQ1w2dI8c0He9PXGxljJxjpAFpxtHPH42UUbpLnD710L_goEmugB3y4GP7GSdCDlwlxZYuUo926GZd2sg7NXLGPGGHpyBhdt9D7Sl9N2ey6mHXv7N_W2bFlpFWr5D2-sX3D5yKM2eqBF6KBZoGI_KpbUSeUQaoFEoTSmR2EoXctnSXpg0Oa8502tzUl7m_bDX4s1Uds6iPUFdLi0KYNwMVZnyyUz6E6ybY_T05BRdheUmnFcJWTCWj86MoWI1PYxZ7BIaBrdl8UcT1Hocwm5xCXSsHrOvnbEjyhYLuaBWvrr8uvNY_K8fgCtg54adP14-HMImfUCbhC9SNkgQrSVKqwarw7sHLJ9k2rMoQcX8Y-2etEpQ52Fa6KiYl8eHZIsA8MwliYqkDs4Dni3xH3sZi0tZzZ7psXfcoeJNL4cPaX0zht4ptRsscXVUKjzazbUbLqm9xnPwsoqVx8R3vYshxDqUYBxE93sFhIWdr8L3j4btFWOz9n9F_llFb3u6joWw98obtnDzQ4G_fTxRY'  # noqa
            }
        }

        request = "place=parallel&text=filter+fz-a60mfe&wizard-rules={}&trace_wizard=1".format(
            urllib.quote(json.dumps(qtree))
        )

        # 1. Без флага market_parallel_boost_completely_match_multitoken_coef бустинга базовой и мета формул нет
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {"market_model": {"title": {"__hl": {"text": "Filter 1", "raw": True}}}},
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertIn('31 1 Meta MatrixNet value: 0.6', response.get_trace_wizard())  # Meta 0.6

        # 2. Под флагом market_parallel_boost_completely_match_multitoken_coef для модели "filter fz-a60mfe" есть совпадение по мультитокену
        # значения базовой и мета формул бустятся
        response = self.report.request_bs_pb(
            request + '&rearr-factors=market_parallel_boost_completely_match_multitoken_coef=2'
        )
        self.assertFragmentIn(
            response,
            {"market_model": {"title": {"__hl": {"text": "Filter fz-a60mfe", "raw": True}}}},
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertIn('31 1 Meta MatrixNet value: 1', response.get_trace_wizard())  # Meta 0.5*2

    def test_search_url_with_nailed_model(self):
        """Проверяем, что под флагом market_search_url_in_model_wizard=1 ссылка модели ведет на search и содержит параметр &rs с прибитой моделью
        https://st.yandex-team.ru/MARKETOUT-31957
        """
        request = "place=parallel&text=Right+Incut&rids=213&rearr-factors=market_search_url_in_model_wizard=1"

        # Параметр rs содержит одну прибитую модель: 584110
        # python -c 'import base64, sys, zlib; sys.stdout.write(zlib.decompress(base64.urlsafe_b64decode("eJwz4vLiEGIztTAxNDQAAAsDAdo-")))' | protoc --decode_raw
        rs = "eJwz4vLiEGIztTAxNDQAAAsDAdo%2C"
        response = self.report.request_bs_pb(request + ";showcase_universal_model=1")
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "url": LikeUrl.of(
                        "//market.yandex.ru/search?text=right%20incut&rs={}&clid=502&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                            rs
                        ),
                        ignore_len=False,
                    ),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?text=right%20incut&rs={}&clid=704&lr=213&utm_medium=cpc&utm_referrer=wizards".format(
                            rs
                        ),
                        ignore_len=False,
                    ),
                }
            },
        )

    def test_not_onstock_parameter_in_urls(self):
        """Проверяем, что под флагом market_parallel_not_onstock_in_urls=1 в ссылки модельного колдунщика,
        ведущие на search и catalog добавляется параметр &onstock=0
        https://st.yandex-team.ru/MARKETOUT-32130
        """
        request = "place=parallel&text=Right+Incut&rearr-factors=market_parallel_not_onstock_in_urls=1;"
        response = self.report.request_bs_pb(request + "showcase_universal_model=1")
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "url_for_category_name": LikeUrl.of(
                        "//market.yandex.ru/catalog--leaf-category/19900?hid=9900&hyperid=584110&modelid=584110&text=right%20incut&clid=502&onstock=0"
                    ),
                    "searchUrl": LikeUrl.of("//market.yandex.ru/search?text=right%20incut&clid=502&onstock=0"),
                    "searchUrlAdG": LikeUrl.of("//market.yandex.ru/search?text=right%20incut&clid=914&onstock=0"),
                    "adGMoreUrl": LikeUrl.of("//market.yandex.ru/search?text=right%20incut&clid=914&onstock=0"),
                }
            },
        )

    @classmethod
    def prepare_collapsing_trace(cls):
        cls.index.models += [Model(title='Кран', hyperid=301, ts=301), Model(title='Техника', hyperid=302, ts=302)]

        cls.index.offers += [
            Offer(title='Кран offer', hyperid=301, ts=311),
            Offer(title='Кран Техника offer', hyperid=302, ts=312),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 311).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 312).respond(0.9)

        cls.matrixnet.on_place(MnPlace.MODEL_WIZARD_META, 301).respond(0.8)
        cls.matrixnet.on_place(MnPlace.MODEL_WIZARD_META, 302).respond(0.6)

    def test_collapsing_trace(self):
        """Проверяем, что под флагом market_show_collapsing_trace_for_model_wizard=1 в трейс выводятся
        значене формулы и порог для схлопнутой модели, в выдаче остается модель до схлопывания
        https://st.yandex-team.ru/MARKETOUT-33176
        """
        request = (
            'place=parallel&text=Кран&trace_wizard=1&rearr-factors=showcase_universal_model=1;market_show_collapsing_trace_for_model_wizard=1;'
            'market_model_wizard_meta_threshold=0.2;market_model_wizard_collapsing_meta_threshold=0.3'
        )
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "title": {"__hl": {"text": "Кран", "raw": True}},
                }
            },
        )

        trace = response.get_trace_wizard()
        self.assertIn('31 2 Выбрана модель modelId = 301', trace)
        self.assertIn('31 1 Meta MatrixNet value: 0.8', trace)
        self.assertIn('31 1 ModelWizard.ModelMnValue.Meta.Threshold: market_model_wizard_meta_threshold=0.2', trace)

        self.assertIn('31 2 По схлопыванию офферов выбрана модель ModelId: 302', trace)
        self.assertIn('31 1 Meta MatrixNet value: 0.6', trace)
        self.assertIn(
            '31 1 ModelWizard.ModelMnValue.Meta.Threshold: market_model_wizard_collapsing_meta_threshold=0.3', trace
        )

    @classmethod
    def prepare_model_right_incut_statistics(cls):
        cls.index.navtree += [NavCategory(nid=1, hid=1)]

        cls.index.models += [Model(hid=1, title='model with stats', hyperid=501)]

        cls.index.offers += [
            Offer(hid=1, hyperid=501, price=100, fesh=501, ts=501),
            Offer(hid=1, hyperid=501, price=101, fesh=502, ts=502),
            Offer(hid=1, hyperid=501, price=102, fesh=503, ts=503, delivery_options=[DeliveryOption(price=0)]),
            Offer(hid=1, hyperid=501, price=103, fesh=504, ts=504, delivery_options=[DeliveryOption(price=0)]),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 501).respond(0.95)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 502).respond(0.94)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 503).respond(0.93)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 504).respond(0.92)

        cls.index.shops += [
            Shop(fesh=501, name="shop 1", priority_region=1, regions=[213], pickup_buckets=[501]),
            Shop(fesh=502, name="shop 2", priority_region=213, pickup_buckets=[502]),
            Shop(fesh=503, name="shop 3", priority_region=213),
            Shop(fesh=504, name="shop 4", priority_region=1, regions=[213]),
        ]

        cls.index.outlets += [
            Outlet(fesh=501, region=213, point_id=501),
            Outlet(fesh=502, region=213, point_id=502),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=501,
                fesh=501,
                options=[PickupOption(outlet_id=501, price=0)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=502,
                fesh=502,
                options=[PickupOption(outlet_id=502, price=1)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

    def test_model_right_incut_statistics(self):
        """https://st.yandex-team.ru/MARKETOUT-34020"""
        response = self.report.request_bs_pb(
            'place=parallel&text=stats&rids=213&rearr-factors=market_enable_model_wizard_right_incut=1;market_parallel_use_additional_model_statistics=1'
        )
        self.assertFragmentIn(
            response,
            {
                'market_model_right_incut': {
                    'shopOffers': {
                        'totalShopsCount': 4,
                        'priceMin': 100,
                        'priceMax': 103,
                        'currency': 'RUR',
                        'url': LikeUrl.of(
                            '//market.yandex.ru/product--model-with-stats/501/offers?grhow=shop&hid=1&hyperid=501&nid=1&text=stats&lr=213&clid=632'
                        ),
                        'shopNames': [
                            'shop 1',
                            'shop 2',
                            'shop 3',
                            'shop 4',
                        ],
                        'freeDelivery': {
                            'priceMin': 102,
                            'url': LikeUrl.of(
                                '//market.yandex.ru/product--model-with-stats/501/offers?cost-of-delivery=free&hid=1&hyperid=501&local-offers-first=0&nid=1&offer-shipping=delivery&text=stats&lr=213&clid=632&free-delivery=1'  # noqa
                            ),
                            'urlTouch': LikeUrl.of(
                                '//m.market.yandex.ru/product--model-with-stats/501?cost-of-delivery=free&hid=1&local-offers-first=0&nid=1&offer-shipping=delivery&text=stats&lr=213&clid=704&free-delivery=1'  # noqa
                            ),
                        },
                        'freePickup': {
                            'priceMin': 100,
                            'url': LikeUrl.of(
                                '//market.yandex.ru/product--model-with-stats/501/offers?cost-of-delivery=free&hid=1&hyperid=501&local-offers-first=0&nid=1&offer-shipping=pickup&text=stats&lr=213&clid=632&free-delivery=1'  # noqa
                            ),
                            'urlTouch': LikeUrl.of(
                                '//m.market.yandex.ru/product--model-with-stats/501?cost-of-delivery=free&hid=1&local-offers-first=0&nid=1&offer-shipping=pickup&text=stats&lr=213&clid=704&free-delivery=1'  # noqa
                            ),
                        },
                        'localOffers': {
                            'priceMin': 100,
                            'url': LikeUrl.of(
                                '//market.yandex.ru/product--model-with-stats/501/offers?hid=1&hyperid=501&local-offers-first=1&nid=1&text=stats&lr=213&clid=632'
                            ),
                            'urlTouch': LikeUrl.of(
                                '//m.market.yandex.ru/product--model-with-stats/501?hid=1&local-offers-first=1&nid=1&text=stats&lr=213&clid=704'
                            ),
                        },
                    }
                }
            },
        )

        response = self.report.request_bs_pb(
            'place=parallel&text=right+incut&rids=213&rearr-factors=market_enable_model_wizard_right_incut=1;market_parallel_use_additional_model_statistics=1'
        )
        self.assertFragmentIn(
            response,
            {
                'market_model_right_incut': {
                    'shopOffers': {
                        'freeDelivery': NoKey('freeDelivery'),
                        'freePickup': NoKey('freePickup'),
                    }
                }
            },
        )

    @classmethod
    def prepare_model_wizard_offers_url_type(cls):
        cls.index.models += [
            Model(hid=9900, hyperid=601, title='Samsung RB-30'),
        ]
        cls.index.offers += [
            Offer(hyperid=601, title='Samsung RB-30 offer', waremd5='bBngptoS3AIjkxLxUQct3Q'),
        ]

    def test_model_wizard_offers_url_type(self):
        """Проверяем что под флагом market_model_wizard_incut_url_type ссылки офферов
        в модельном колдунщике ведут на разные посадочные страницы
        https://st.yandex-team.ru/MARKETOUT-35053
        """

        def test(wizard, device, url_type, url_for_counter):
            request = "place=parallel&text=Samsung+RB-30&rearr-factors=showcase_universal_model=1;market_enable_model_wizard_right_incut=1;"
            if url_type:
                request += 'market_model_wizard_incut_url_type={};'.format(url_type)
            if device:
                request += 'device={};'.format(device)
            touch = '&touch=1' if device == 'touch' else ''

            response = self.report.request_bs_pb(request + touch)
            self.assertFragmentIn(
                response,
                {
                    wizard: {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {"__hl": {"text": "Samsung RB-30 offer", "raw": True}},
                                        "urlForCounter": url_for_counter,
                                    },
                                    "thumb": {"urlForCounter": url_for_counter},
                                }
                            ]
                        }
                    }
                },
            )

        # Default: External url ссылки ведут в магазин
        test('market_model', 'desktop', None, Contains('//market-click2.yandex.ru/redir/dtype=market'))
        test('market_model', 'touch', None, Contains('//market-click2.yandex.ru/redir/dtype=market'))
        test('market_model_right_incut', 'desktop', None, Contains('//market-click2.yandex.ru/redir/dtype=market'))
        test('market_model_right_incut', 'touch', None, Contains('//market-click2.yandex.ru/redir/dtype=market'))

        # Под флагом market_model_wizard_incut_url_type=OfferCard ссылки ведут на КО
        test('market_model', 'desktop', 'OfferCard', Contains('//market-click2.yandex.ru/redir/dtype=offercard'))
        test('market_model', 'touch', 'OfferCard', Contains('//market-click2.yandex.ru/redir/dtype=offercard'))
        test(
            'market_model_right_incut',
            'desktop',
            'OfferCard',
            Contains('//market-click2.yandex.ru/redir/dtype=offercard'),
        )
        test(
            'market_model_right_incut',
            'touch',
            'OfferCard',
            Contains('//market-click2.yandex.ru/redir/dtype=offercard'),
        )

        # Под флагом market_model_wizard_incut_url_type=NailedInSearch ссылки ведут на страницу поиска с прибитым оффером
        test(
            'market_model',
            'desktop',
            'NailedInSearch',
            LikeUrl.of(
                '//market.yandex.ru/search?text=samsung%20rb-30&cvredirect=0&lr=0&rs=eJwzUvCS4xJLcspLLyjJDzZ29MzKrvCpCA1MLjEOlGBUYNBgAACpMAkq&clid=502'
            ),
        )
        test(
            'market_model',
            'touch',
            'NailedInSearch',
            LikeUrl.of(
                '//m.market.yandex.ru/search?text=samsung%20rb-30&cvredirect=0&lr=0&rs=eJwzUvCS4xJLcspLLyjJDzZ29MzKrvCpCA1MLjEOlGBUYNBgAACpMAkq&clid=704'
            ),
        )
        test(
            'market_model_right_incut',
            'desktop',
            'NailedInSearch',
            LikeUrl.of(
                '//market.yandex.ru/search?text=samsung%20rb-30&cvredirect=0&lr=0&rs=eJwzUvCS4xJLcspLLyjJDzZ29MzKrvCpCA1MLjEOlGBUYNBgAACpMAkq&clid=632'
            ),
        )
        test(
            'market_model_right_incut',
            'touch',
            'NailedInSearch',
            LikeUrl.of(
                '//m.market.yandex.ru/search?text=samsung%20rb-30&cvredirect=0&lr=0&rs=eJwzUvCS4xJLcspLLyjJDzZ29MzKrvCpCA1MLjEOlGBUYNBgAACpMAkq&clid=704'
            ),
        )

        # Под флагом market_model_wizard_incut_url_type=NailedInCatalog ссылки ведут в каталог с прибитым оффером
        test(
            'market_model',
            'desktop',
            'NailedInCatalog',
            LikeUrl.of(
                '//market.yandex.ru/catalog/19900/list?text=samsung%20rb-30&hid=9900&lr=0&rs=eJwzUvCS4xJLcspLLyjJDzZ29MzKrvCpCA1MLjEOlGBUYNBgAACpMAkq&clid=502'
            ),
        )
        test(
            'market_model',
            'touch',
            'NailedInCatalog',
            LikeUrl.of(
                '//m.market.yandex.ru/catalog/19900/list?text=samsung%20rb-30&hid=9900&lr=0&rs=eJwzUvCS4xJLcspLLyjJDzZ29MzKrvCpCA1MLjEOlGBUYNBgAACpMAkq&clid=704'
            ),
        )
        test(
            'market_model_right_incut',
            'desktop',
            'NailedInCatalog',
            LikeUrl.of(
                '//market.yandex.ru/catalog/19900/list?text=samsung%20rb-30&hid=9900&lr=0&rs=eJwzUvCS4xJLcspLLyjJDzZ29MzKrvCpCA1MLjEOlGBUYNBgAACpMAkq&clid=632'
            ),
        )
        test(
            'market_model_right_incut',
            'touch',
            'NailedInCatalog',
            LikeUrl.of(
                '//m.market.yandex.ru/catalog/19900/list?text=samsung%20rb-30&hid=9900&lr=0&rs=eJwzUvCS4xJLcspLLyjJDzZ29MzKrvCpCA1MLjEOlGBUYNBgAACpMAkq&clid=704'
            ),
        )

    @classmethod
    def prepare_medicine_models(cls):
        cls.index.category_restrictions += [
            CategoryRestriction(
                name='medicine',
                hids=[901],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=False,
                        display_only_matched_offers=True,
                        delivery=False,
                        disclaimers=[
                            Disclaimer(
                                name='medicine',
                                text='Есть противопоказания, посоветуйтесь с врачом',
                                short_text='Есть противопоказания, посоветуйтесь с врачом',
                                default_warning=False,
                            ),
                            Disclaimer(
                                name='medicine_recipe',
                                text='Есть противопоказания, посоветуйтесь с врачом. Отпускается по рецепту врача',
                                short_text='Есть противопоказания, посоветуйтесь с врачом. Отпускается по рецепту врача',
                                default_warning=False,
                            ),
                        ],
                    ),
                ],
            )
        ]
        cls.index.hypertree += [
            HyperCategory(hid=901, name='Лекарства'),
        ]
        cls.index.models += [
            Model(hyperid=50, hid=901, title='Лекарство', ts=50, disclaimers_model='medicine'),
            Model(
                hyperid=51,
                hid=901,
                title='Лекарство рецептурное',
                ts=51,
                disclaimers_model=['medicine', 'medicine_recipe'],
            ),  # фильтруется по medicine_recipe
        ]
        cls.index.offers += [
            Offer(hyperid=50, hid=901, title='Лекарство 1', price=100),
            Offer(hyperid=51, hid=901, title='Лекарство рецептурное 1', price=100),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 50).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 51).respond(0.95)

    def test_medicine_prescription_models_filtering(self):
        """Фильтрация рецептурных лекарственных моделей
        https://st.yandex-team.ru/MARKETOUT-35468
        """
        request = 'place=parallel&text=лекарство&trace_wizard=1&rearr-factors=market_parallel_reject_restricted_offers=0;showcase_universal_model=1;'
        response = self.report.request_bs_pb(
            request + 'market_parallel_filter_prescription_offers_on_base_search=0;'
            'market_parallel_filter_prescription_models_on_base_search=0;'
            'market_parallel_filter_prescription_models=0;'
        )
        self.assertFragmentIn(
            response, {"market_model": {"title": {"__hl": {"text": "Лекарство рецептурное", "raw": True}}}}
        )

        response = self.report.request_bs_pb(
            request + 'market_parallel_filter_prescription_offers_on_base_search=0;'
            'market_parallel_filter_prescription_models_on_base_search=0;'
            'market_parallel_filter_prescription_models=1;'
        )
        self.assertFragmentIn(response, {"market_model": {"title": {"__hl": {"text": "Лекарство", "raw": True}}}})
        self.assertFragmentNotIn(
            response, {"market_model": {"title": {"__hl": {"text": "Лекарство рецептурное", "raw": True}}}}
        )

        response = self.report.request_bs_pb(
            request + 'market_parallel_filter_prescription_offers_on_base_search=0;'
            'market_parallel_filter_prescription_models_on_base_search=1;'
            'market_parallel_filter_prescription_models=0;'
        )
        self.assertFragmentIn(response, {"market_model": {"title": {"__hl": {"text": "Лекарство", "raw": True}}}})
        self.assertFragmentNotIn(
            response, {"market_model": {"title": {"__hl": {"text": "Лекарство рецептурное", "raw": True}}}}
        )

    @classmethod
    def prepare_model_group_with_params(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=40,
                name="Ноутбуки",
                has_groups=True,
                range_fields=["ProcType", "OS", "GraphicsCard", "Memory", "HDD"],
                design_group_params='''
                <category name='notebooks'>
                    <b-params name='Тип'>
                        <value>OS</value>
                    </b-params>

                    <b-params name='Процессор'>
                        <value>ProcType</value>
                    </b-params>

                    <b-params name='Память'>
                        <value>Memory</value>
                        <value>HDD</value>
                    </b-params>
                </category>
                ''',
            )
        ]

        cls.index.gltypes += [
            GLType(
                param_id=401,
                hid=40,
                xslname="ProcType",
                name=u"Процессор",
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text="Core i5"),
                    GLValue(value_id=2, text="Core i7"),
                ],
            ),
            GLType(
                param_id=402,
                hid=40,
                xslname="OS",
                name=u"ОС",
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text="MacOS"),
                    GLValue(value_id=2, text="Windows"),
                ],
                has_description=False,
            ),
            GLType(
                param_id=403,
                hid=40,
                xslname="GraphicsCard",
                name=u"Видеокарта",
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text="NVIDIA GeForce"),
                    GLValue(value_id=2, text="ATI Radeon"),
                ],
            ),
            GLType(param_id=404, hid=40, xslname="Memory", name=u"ОЗУ", gltype=GLType.NUMERIC, unit_name="Гб"),
            GLType(param_id=405, hid=40, xslname="HDD", name=u"HDD", gltype=GLType.NUMERIC, unit_name="Gb"),
        ]

        cls.index.model_groups += [
            ModelGroup(
                hyperid=10000,
                title='Group Model',
                hid=40,
            )
        ]

        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=40,
                friendlymodel=[
                    ("Процессор", "процессор: {ProcType}"),
                    ("ОС", "ОС: {OS}"),
                    ("Видеокарта", "Видеокарта: {GraphicsCard}"),
                    ("ОЗУ", "ОЗУ: {Memory} Гб"),
                    ("HDD", "HDD: {HDD} Gb"),
                ],
                model=[
                    ("Процессор", {"Процессор": "{ProcType}"}),
                    ("ОС", {"ОС": "{OS}"}),
                    ("Видеокарта", {"Видеокарта": "{GraphicsCard}"}),
                    ("ОЗУ", {"ОЗУ": "{Memory} Гб"}),
                    ("HDD", {"HDD": "{HDD} Gb"}),
                ],
            )
        ]

        cls.index.models += [
            Model(
                hyperid=20001,
                hid=40,
                title='Single Model 1',
                group_hyperid=10000,
                glparams=[
                    GLParam(param_id=401, value=1),
                    GLParam(param_id=402, value=1),
                    GLParam(param_id=403, value=1),
                    GLParam(param_id=404, value=2),
                    GLParam(param_id=405, value=256),
                ],
            ),
            Model(
                hyperid=20002,
                hid=40,
                title='Single Model 2',
                group_hyperid=10000,
                glparams=[
                    GLParam(param_id=401, value=1),
                    GLParam(param_id=402, value=2),
                    GLParam(param_id=403, value=2),
                    GLParam(param_id=404, value=3),
                    GLParam(param_id=405, value=512),
                ],
            ),
            Model(
                hyperid=20003,
                hid=40,
                title='Single Model 3',
                group_hyperid=10000,
                glparams=[
                    GLParam(param_id=401, value=2),
                    GLParam(param_id=402, value=1),
                    GLParam(param_id=403, value=1),
                    GLParam(param_id=404, value=4),
                    GLParam(param_id=405, value=512),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(fesh=1, hyperid=10000, title='Group Model Offer', price=101, pickup=True),
            Offer(fesh=1, hyperid=20001, title='Single Model 1 Offer', price=101, pickup=True),
            Offer(fesh=1, hyperid=20002, title='Single Model 2 Offer', price=101, pickup=True),
            Offer(fesh=1, hyperid=20003, title='Single Model 3 Offer', price=101, pickup=True),
        ]

    def test_model_wizard_group_paramenters(self):
        """Проверяем, что параметры групповой модели правильно отображаются
        https://st.yandex-team.ru/MARKETOUT-36487
        """
        request = "place=parallel&text=group&rearr-factors=market_enable_model_wizard_right_incut=1;"

        # Incorrect
        response = self.report.request_bs_pb(request + "market_model_wizard_correct_group_model_parameters=0")
        self.assertFragmentIn(
            response,
            {
                'market_model_right_incut': {
                    'parameters': [
                        'Core i5 / Core i7',
                        'MacOS / Windows',
                        '2...4 Гб',
                        'ATI Radeon / NVIDIA GeForce',
                        '256...512 Gb',
                    ]
                }
            },
        )
        # Correct
        response = self.report.request_bs_pb(request + "market_model_wizard_correct_group_model_parameters=1")
        self.assertFragmentIn(
            response,
            {
                'market_model_right_incut': {
                    'parameters': [
                        'Процессор: Core i5 / Core i7',
                        'ОС: MacOS / Windows',
                        'ОЗУ: 2...4 Гб',
                        'Видеокарта: ATI Radeon / NVIDIA GeForce',
                        'HDD: 256...512 Gb',
                    ]
                }
            },
        )


if __name__ == '__main__':
    main()
