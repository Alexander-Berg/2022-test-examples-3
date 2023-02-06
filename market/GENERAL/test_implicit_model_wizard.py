#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    CategoryRestriction,
    DeliveryBucket,
    DeliveryOption,
    Disclaimer,
    GLParam,
    GLType,
    GLValue,
    HyperCategory,
    MarketSku,
    MnPlace,
    Model,
    ModelDescriptionTemplates,
    NavCategory,
    Offer,
    Opinion,
    Promo,
    PromoType,
    Region,
    RegionalDelivery,
    RegionalRestriction,
    Shop,
    UrlType,
    YamarecPlaceReasonsToBuy,
)
from core.testcase import TestCase, main
from core.types.autogen import b64url_md5
from core.matcher import Absent, Contains, ElementCount, LikeUrl, NoKey, NotEmptyList
from core.blackbox import BlackboxUser
from core.report import REQUEST_TIMESTAMP
from core.types.offer_promo import PromoBlueCashback, PromoRestrictions

from datetime import datetime, timedelta
from itertools import count

import json
import urllib

now = datetime.fromtimestamp(REQUEST_TIMESTAMP)
delta = timedelta(days=10)
nummer = count()


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=0;enable_fast_promo_matcher_test=0']

        cls.index.regiontree += [
            Region(rid=213, name='Москва', genitive='Москвы', locative='Москве', preposition='в', en_name='Moscow')
        ]

        cls.index.models += [
            Model(hyperid=101, ts=101, title="reviewModelFiltering 1", opinion=Opinion(total_count=10)),
            Model(hyperid=102, ts=102, title="reviewModelFiltering 2"),
            Model(hyperid=103, ts=103, title="reviewModelFiltering 3", opinion=Opinion(total_count=2)),
            Model(hyperid=104, ts=104, title="reviewModelFiltering 4"),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 102).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 103).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 104).respond(0.6)

        cls.index.offers += [Offer(hyperid=101), Offer(hyperid=102), Offer(hyperid=103), Offer(hyperid=104)]

        cls.index.shops += [
            Shop(fesh=502, priority_region=213),
        ]

        cls.index.models += [
            Model(hyperid=501, ts=501, title="newModel 1", new=True),
            Model(hyperid=502, ts=502, title="newModel 2"),
            Model(hyperid=503, ts=503, title="newModel 3"),
            Model(hyperid=504, ts=504, title="newModel 4"),
            Model(hyperid=505, ts=505, title="newModel 5"),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 501).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 502).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 503).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 504).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 505).respond(0.5)

        cls.index.offers += [
            Offer(hyperid=502, fesh=502),
            Offer(hyperid=503, fesh=502),
            Offer(hyperid=504, fesh=502),
            Offer(hyperid=505, fesh=502),
        ]

        cls.index.yamarec_places += [
            YamarecPlaceReasonsToBuy()
            .new_partition("split=1")
            .add(
                hyperid=101,
                reasons=[
                    {
                        "id": "positive_feedback",
                        "type": "consumerFactor",
                        "author_puid": "1001",
                        "text": "Уникальный аппарат, который уверенно смотрит в будущее",
                    }
                ],
            )
            .add(
                hyperid=102,
                reasons=[
                    {
                        "id": "positive_feedback",
                        "type": "consumerFactor",
                        "author_puid": "1002",
                        "text": "Мощный процессор",
                    },
                ],
            )
            .add(
                hyperid=103,
                reasons=[
                    {
                        "type": "consumerFactor",
                        "id": "positive_feedback",
                        "author_puid": "1003",
                        "text": "Очень хорошо держит аккумулятор",
                    }
                ],
            )
            .add(
                hyperid=104,
                reasons=[
                    {
                        "type": "consumerFactor",
                        "factor_name": "Объем памяти",
                        "factor_priority": "2",
                    }
                ],
            )
        ]

        cls.blackbox.on_request(uids=['1001', '1002', '1003']).respond(
            [
                BlackboxUser(uid='1001', name='name 1001', avatar='avatar_1001'),
                BlackboxUser(uid='1003', name='name 1003', avatar='avatar_1003'),
            ]
        )

    def test_implicit_log_features_rate(self):
        """Проверяем, флаг для изменения количества логируемых запросов для неявного колдунщика
        https://st.yandex-team.ru/MARKETOUT-37376
        https://st.yandex-team.ru/MARKETOUT-39058
        """

        model_count = 4
        # Под флагом market_parallel_feature_log_rate указываем количество логируемых запросов
        # Значение 0 отключает логирование
        for i in range(model_count):
            self.report.request_bs_pb(
                'place=parallel&text=reviewModelFiltering&reqid=123'
                '&rearr-factors=market_disable_model_wiz=1;'
                'market_parallel_feature_log_rate={}'.format(0)
            )
            model_id = 101 + i
            self.feature_log.expect(req_id=123, model_id=model_id, document_type=2).times(0)

        # Под флагом market_parallel_feature_log_rate=1 логируем модели для каждого запроса
        for i in range(model_count):
            req_id = 765 + i
            self.report.request_bs_pb(
                'place=parallel&text=reviewModelFiltering&reqid={}'
                '&rearr-factors=market_disable_model_wiz=1;market_parallel_feature_log_rate=1'.format(req_id)
            )
            self.feature_log.expect(document_type=2, req_id=req_id).times(model_count)

    @classmethod
    def prepare_models_for_count_flags(cls):
        # Создаем шаблон описания модели
        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=201 + i,
                friendlymodel=[
                    ("spec1", "{spec1}"),
                    ("spec2", "{spec2}"),
                    ("spec3", "{spec3}"),
                ],
            )
            for i in range(3)
        ]

        # Создаем фильтры, на которые ссылаемся в шаблоне
        for i in range(3):
            cls.index.gltypes += [
                GLType(hid=201 + i, param_id=100, xslname="spec1", gltype=GLType.STRING),
                GLType(hid=201 + i, param_id=101, xslname="spec2", gltype=GLType.NUMERIC),
                GLType(
                    hid=201 + i,
                    param_id=102,
                    xslname="spec3",
                    gltype=GLType.ENUM,
                    values=[GLValue(value_id=1, text='iOS'), GLValue(value_id=2, text='Android')],
                ),
            ]

        cls.index.hypertree += [
            HyperCategory(
                hid=200,
                name='godlike category',
                children=[
                    HyperCategory(hid=201, name='godlike category 1'),
                    HyperCategory(hid=202, name='godlike category 2'),
                    HyperCategory(hid=203, name='godlike category 3'),
                ],
            ),
        ]

        cls.index.navtree += [
            NavCategory(nid=1200, hid=200),
            NavCategory(nid=1201, hid=201),
            NavCategory(nid=1202, hid=202),
            NavCategory(nid=1203, hid=203),
        ]

        cls.index.models += [
            Model(
                hyperid=1001 + i,
                hid=201 + i // 3,
                title='godlike model {}'.format(1 + i),
                has_blue_offers=True,
                ts=1001 + i,
                glparams=[
                    GLParam(param_id=100, string_value="model {} spec1".format(1 + i)),
                    GLParam(param_id=101, value=4 + i),
                    GLParam(param_id=102, value=1 + i % 2),
                ],
                opinion=Opinion(rating=4.5),
            )
            for i in range(6)
        ]

        for i in range(6):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1001 + i).respond(0.5 - 0.01 * i)

        for i in range(10):
            cls.index.offers += [
                Offer(title='godlike offer 1', hid=201),
                Offer(title='godlike offer 2', hid=202),
                Offer(title='godlike offer 3', hid=202),
                Offer(title='godlike offer 4', hid=203),
            ]

        for i in range(10):
            cls.index.mskus += [
                MarketSku(
                    sku=71 + i,
                    title="godlike sku 1",
                    hyperid=1001 + i % 2,
                    blue_offers=[BlueOffer(price=100, feedid=3), BlueOffer(price=200, feedid=3)],
                ),
                MarketSku(
                    sku=81 + i,
                    title="godlike sku 2",
                    hyperid=1003 + i % 2,
                    blue_offers=[BlueOffer(price=105, feedid=3), BlueOffer(price=205, feedid=3)],
                ),
                MarketSku(
                    sku=91 + i,
                    title="godlike sku 3",
                    hyperid=1005 + i % 2,
                    blue_offers=[BlueOffer(price=110, feedid=3), BlueOffer(price=210, feedid=3)],
                ),
            ]

    def test_implicit_model_documents_count_flags(self):
        """https://st.yandex-team.ru/MARKETOUT-27591"""

        request = 'place=parallel&text=godlike&rearr-factors=market_implicit_blue_model_wizard_meta_threshold=0;'

        def do_test(flags, name, modelsCount, blueIncutCount):
            response = self.report.request_bs_pb(request + flags)
            items = [
                {
                    'title': {'text': {'__hl': {'text': 'godlike model {}'.format(1 + i)}}},
                    'is_blue': NoKey('is_blue'),
                }
                for i in range(modelsCount)
            ]
            items += [
                {
                    'title': {'text': {'__hl': {'text': 'godlike model {}'.format(1 + i)}}},
                    'is_blue': '1',
                }
                for i in range(blueIncutCount)
            ]
            self.assertFragmentIn(
                response, {name: {'showcase': {'items': items}}}, allow_different_len=False, preserve_order=True
            )

        maxFlagsForEveryIncut = 'market_implicit_wizard_top_models_count=6;market_implicit_blue_incut_model_count=6;'  # there is no more than 12 models in index
        zeroFlagsForEveryIncut = 'market_implicit_wizard_top_models_count=0;market_implicit_blue_incut_model_count=0;'
        enableCenterIncut = 'market_enable_implicit_model_wiz_center_incut=1;'
        enableAdGIncut = 'market_enable_implicit_model_adg_wiz=1;'

        # default IMPLICIT_MODEL_WIZARD_MODELS_COUNT=4
        moreThanDefault = 5
        lessThanDefault = 3

        tests = [
            (
                'market_implicit_model_wizard_models_right_incut_count=5;market_implicit_model_wizard_blue_models_right_incut_count=3;',
                'market_implicit_model',
                moreThanDefault,
                lessThanDefault,
            ),
            (
                'market_implicit_model_wizard_models_right_incut_count=3;market_implicit_model_wizard_blue_models_right_incut_count=5;',
                'market_implicit_model',
                lessThanDefault,
                moreThanDefault,
            ),
            (
                enableCenterIncut
                + 'market_implicit_model_wizard_models_center_incut_count=3;market_implicit_model_wizard_blue_models_center_incut_count=5;',
                'market_implicit_model_center_incut',
                lessThanDefault,
                moreThanDefault,
            ),
            (
                enableCenterIncut
                + 'market_implicit_model_wizard_models_center_incut_count=5;market_implicit_model_wizard_blue_models_center_incut_count=3;',
                'market_implicit_model_center_incut',
                moreThanDefault,
                lessThanDefault,
            ),
            (
                enableAdGIncut + 'market_implicit_model_wizard_models_adg_count=3;',
                'market_implicit_model_adg_wizard',
                lessThanDefault,
                0,
            ),  # adg VT does not contain blue incut
            (
                enableAdGIncut + 'market_implicit_model_wizard_models_adg_count=5;',
                'market_implicit_model_adg_wizard',
                moreThanDefault,
                0,
            ),
            (
                'device=touch;market_implicit_model_wizard_models_touch_count=5;market_implicit_model_wizard_blue_models_touch_count=3;',
                'market_implicit_model',
                moreThanDefault,
                lessThanDefault,
            ),
            (
                'device=touch;market_implicit_model_wizard_models_touch_count=3;market_implicit_model_wizard_blue_models_touch_count=5;',
                'market_implicit_model',
                lessThanDefault,
                moreThanDefault,
            ),
        ]

        # priority of exact flags is more than of common flags
        for flags, name, modelsCount, blueIncutCount in list(tests):
            tests.append([flags + maxFlagsForEveryIncut, name, modelsCount, blueIncutCount])
            tests.append([flags + zeroFlagsForEveryIncut, name, modelsCount, blueIncutCount])

        tests += [
            (zeroFlagsForEveryIncut, "market_implicit_model", 0, 0),
            (maxFlagsForEveryIncut, "market_implicit_model", 6, 6),
        ]

        for flags, name, modelsCount, blueIncutCount in tests:
            do_test(flags, name, modelsCount, blueIncutCount)

    def test_implicit_model_text_param_in_categories(self):
        """https://st.yandex-team.ru/MARKETOUT-27963
        Проверяем, что под флагом market_implicit_model_wizard_category_without_request_text
        в ссылках на категории в неявном колдунщике нет параметра text
        """
        old_sitelinks_rearr = ';market_implicit_model_sitelink_reviews=0;market_implicit_wiz_without_offers_categories=0;market_implicit_model_sitelink_map=0;market_implicit_model_sitelink_next_day_delivery=0;market_implicit_model_sitelink_categories=1'  # noqa
        response = self.report.request_bs_pb(
            'place=parallel&text=godlike&rearr-factors=market_implicit_model_wizard_category_without_request_text=1'
            + old_sitelinks_rearr
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "sitelinks": [
                        {
                            "text": "godlike category 1",
                            "url": LikeUrl.of("//market.yandex.ru/search?hid=201&clid=698", no_params=['text']),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?hid=201&clid=721", no_params=['text']),
                        },
                        {
                            "text": "godlike category 2",
                            "url": LikeUrl.of("//market.yandex.ru/search?hid=202&clid=698", no_params=['text']),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?hid=202&clid=721", no_params=['text']),
                        },
                        {
                            "text": "godlike category 3",
                            "url": LikeUrl.of("//market.yandex.ru/search?hid=203&clid=698", no_params=['text']),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?hid=203&clid=721", no_params=['text']),
                        },
                    ]
                }
            },
        )

    def test_implicit_model_show_categories(self):
        """https://st.yandex-team.ru/MARKETOUT-28157
        Проверяем, что по умолчанию категории в неявном колдунщике показываются
        и что при market_top_categories_in_implicit_model_wizard=0 катекории не показываются
        """
        # По умолчанию market_top_categories_in_implicit_model_wizard=1
        response = self.report.request_bs_pb('place=parallel&text=godlike')
        self.assertFragmentIn(response, {"market_implicit_model": {"sitelinks": NotEmptyList()}})

        # Под флагом market_top_categories_in_implicit_model_wizard категории показываются
        response = self.report.request_bs_pb(
            'place=parallel&text=godlike&rearr-factors=market_top_categories_in_implicit_model_wizard=1'
        )
        self.assertFragmentIn(response, {"market_implicit_model": {"sitelinks": NotEmptyList()}})

        # Без флага market_top_categories_in_implicit_model_wizard категории не показываются
        old_sitelinks_rearr = ';market_implicit_model_sitelink_reviews=0;market_implicit_wiz_without_offers_categories=0;market_implicit_model_sitelink_map=0;market_implicit_model_sitelink_next_day_delivery=0;market_implicit_model_sitelink_categories=1'  # noqa
        response = self.report.request_bs_pb(
            'place=parallel&text=godlike&rearr-factors=market_top_categories_in_implicit_model_wizard=0'
            + old_sitelinks_rearr
        )
        self.assertFragmentIn(response, {"market_implicit_model": {"sitelinks": Absent()}})

    def test_implicit_model_without_offers_categories(self):
        """https://st.yandex-team.ru/MARKETOUT-28053
        По умолчанию если меньше трех категорий моделей, то до трех догоняется категориями офферов
        под флагом market_implicit_wiz_without_offers_categories=1 не должен брать категории от офферов
        """
        old_sitelinks_rearr = '&rearr-factors=market_implicit_model_sitelink_reviews=0;market_implicit_wiz_without_offers_categories=0;market_implicit_model_sitelink_map=0;market_implicit_model_sitelink_next_day_delivery=0;market_implicit_model_sitelink_categories=1'  # noqa
        # По умолчанию market_implicit_wiz_without_offers_categories=0
        response = self.report.request_bs_pb('place=parallel&text=godlike' + old_sitelinks_rearr)
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "sitelinks": [
                        {
                            "text": "godlike category 1",
                        },
                        {
                            "text": "godlike category 2",
                        },
                        {
                            "text": "godlike category 3",
                        },
                    ]
                }
            },
            allow_different_len=False,
        )

        old_sitelinks_rearr_categories = ';market_implicit_model_sitelink_reviews=0;market_implicit_model_sitelink_map=0;market_implicit_model_sitelink_next_day_delivery=0;market_implicit_model_sitelink_categories=1'  # noqa
        # Под флагом market_implicit_wiz_without_offers_categories=1
        response = self.report.request_bs_pb(
            'place=parallel&text=godlike&rearr-factors=market_implicit_wiz_without_offers_categories=1'
            + old_sitelinks_rearr_categories
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "sitelinks": [
                        {
                            "text": "godlike category 1",
                        },
                        {
                            "text": "godlike category 2",
                        },
                    ]
                }
            },
            allow_different_len=False,
        )

    def test_implicit_model_sitelinks(self):
        """Проверяем разные сайтлинки неявного колдунщика под разными флагами
        https://st.yandex-team.ru/MARKETOUT-31594
        """
        old_sitelinks_rearr_categories = ';market_implicit_model_sitelink_reviews=0;market_implicit_wiz_without_offers_categories=0;market_implicit_model_sitelink_map=0;market_implicit_model_sitelink_next_day_delivery=0'  # noqa

        # Без сайтлинков, по умолчанию market_implicit_model_sitelink_categories=1
        response = self.report.request_bs_pb(
            'place=parallel&text=godlike&rearr-factors=market_implicit_model_sitelink_categories=0'
            + old_sitelinks_rearr_categories
        )
        self.assertFragmentIn(response, {"market_implicit_model": {"sitelinks": Absent()}}, allow_different_len=False)

        old_sitelinks_rearr = '&rearr-factors=market_implicit_model_sitelink_reviews=0;market_implicit_wiz_without_offers_categories=0;market_implicit_model_sitelink_map=0;market_implicit_model_sitelink_next_day_delivery=0;market_implicit_model_sitelink_categories=1'  # noqa
        # Только сайтлинки категории, по умолчанию market_implicit_model_sitelink_categories=1
        response = self.report.request_bs_pb('place=parallel&text=godlike' + old_sitelinks_rearr)
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "sitelinks": [
                        {
                            "text": "godlike category 1",
                        },
                        {
                            "text": "godlike category 2",
                        },
                        {
                            "text": "godlike category 3",
                        },
                    ]
                }
            },
            allow_different_len=False,
        )

        old_sitelinks_rearr_qa = ';market_implicit_model_sitelink_reviews=0;market_implicit_wiz_without_offers_categories=0;market_implicit_model_sitelink_map=0;market_implicit_model_sitelink_next_day_delivery=0'  # noqa
        # Только вопросы и ответы к модельным категориям
        # Вопросы и ответы показываются только к тем категориям, модели которых показаны во врезке
        response = self.report.request_bs_pb(
            'place=parallel&text=godlike&rearr-factors=market_implicit_model_sitelink_categories=0;market_implicit_model_sitelink_categories_qa=1'
            + old_sitelinks_rearr_qa
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "sitelinks": [
                        {
                            "text": "godlike category 1 - вопросы и ответы",
                        },
                        {
                            "text": "godlike category 2 - вопросы и ответы",
                        },
                    ]
                }
            },
            allow_different_len=False,
        )

        old_sitelinks_rearr_map = ';market_implicit_model_sitelink_reviews=0;market_implicit_wiz_without_offers_categories=0;market_implicit_model_sitelink_next_day_delivery=0'
        # Только ссылка на карту с магазинами
        response = self.report.request_bs_pb(
            'place=parallel&text=godlike&rearr-factors=market_implicit_model_sitelink_categories=0;market_implicit_model_sitelink_map=1'
            + old_sitelinks_rearr_map
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "sitelinks": [
                        {
                            "text": "На карте",
                            "hint": Absent(),
                        }
                    ]
                }
            },
            allow_different_len=False,
        )
        # На таче сайтлинка на карту быть не должно
        response = self.report.request_bs_pb(
            'place=parallel&text=godlike&touch=1&rearr-factors=device=touch;market_implicit_model_sitelink_categories=0;market_implicit_model_sitelink_map=1'
            + old_sitelinks_rearr_map
        )
        self.assertFragmentIn(response, {"market_implicit_model": {"sitelinks": Absent()}}, allow_different_len=False)

        old_sitelinks_reviews = ';market_implicit_wiz_without_offers_categories=0;market_implicit_model_sitelink_map=0;market_implicit_model_sitelink_next_day_delivery=0'
        # Только ссылка на поисковую выдачу с отзывами
        response = self.report.request_bs_pb(
            'place=parallel&text=godlike&rearr-factors=market_implicit_model_sitelink_categories=0;market_implicit_model_sitelink_reviews=1'
            + old_sitelinks_reviews
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "sitelinks": [
                        {
                            "text": "Отзывы",
                            "hint": Absent(),
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

        old_sitelinks_rearr_delivery = ';market_implicit_model_sitelink_reviews=0;market_implicit_wiz_without_offers_categories=0;market_implicit_model_sitelink_map=0'
        # Только ссылка на поисковую выдачу с отзывами
        response = self.report.request_bs_pb(
            'place=parallel&text=godlike&rearr-factors=market_implicit_model_sitelink_categories=0;market_implicit_model_sitelink_next_day_delivery=1'
            + old_sitelinks_rearr_delivery
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "sitelinks": [
                        {
                            "text": "С доставкой завтра",
                            "hint": Absent(),
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

        old_sitelinks_rearr_all = ';market_implicit_wiz_without_offers_categories=0'
        # Все сайтлинки в порядке: отзывы-карта-QA-категории
        response = self.report.request_bs_pb(
            'place=parallel&text=godlike&rearr-factors='
            'market_implicit_model_sitelink_categories=1;'
            'market_implicit_model_sitelink_reviews=1;'
            'market_implicit_model_sitelink_map=1;'
            'market_implicit_model_sitelink_next_day_delivery=1;'
            'market_implicit_model_sitelink_categories_qa=1' + old_sitelinks_rearr_all
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "sitelinks": [
                        {
                            "text": "Отзывы",
                            "url": LikeUrl.of("//market.yandex.ru/search?show-reviews=1&text=godlike&clid=698"),
                        },
                        {
                            "text": "На карте",
                            "url": LikeUrl.of("//market.yandex.ru/geo?text=godlike&clid=698"),
                        },
                        {
                            "text": "С доставкой завтра",
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?delivery-interval=1&text=godlike&lr=0&clid=698"
                            ),
                        },
                        {
                            "text": "godlike category 1 - вопросы и ответы",
                            "url": LikeUrl.of(
                                "//market.yandex.ru/catalog--godlike-category-1/1201/questions?hid=201&clid=698"
                            ),
                        },
                        {
                            "text": "godlike category 2 - вопросы и ответы",
                            "url": LikeUrl.of(
                                "//market.yandex.ru/catalog--godlike-category-2/1202/questions?hid=202&clid=698"
                            ),
                        },
                        {
                            "text": "godlike category 1",
                            "url": LikeUrl.of("//market.yandex.ru/search?hid=201&nid=1201&text=godlike&clid=698"),
                        },
                        {
                            "text": "godlike category 2",
                            "url": LikeUrl.of("//market.yandex.ru/search?hid=202&nid=1202&text=godlike&clid=698"),
                        },
                        {
                            "text": "godlike category 3",
                            "url": LikeUrl.of("//market.yandex.ru/search?hid=203&nid=1203&text=godlike&clid=698"),
                        },
                    ]
                }
            },
            allow_different_len=False,
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

    def test_delivery_sitelink_if_medicine_models(self):
        """https://st.yandex-team.ru/MARKETOUT-35468"""
        response = self.report.request_bs_pb(
            'place=parallel&text=лекарство&rearr-factors=market_implicit_model_sitelink_next_day_delivery=1;market_implicit_model_delivery_sitelink_if_medicine=0'
        )
        self.assertFragmentNotIn(
            response,
            {
                "market_implicit_model": {
                    "sitelinks": [
                        {
                            "text": "С доставкой завтра",
                        }
                    ]
                }
            },
        )

    def test_implicit_model_image_in_text_wizard(self):
        """https://st.yandex-team.ru/MARKETOUT-27932
        При market_implicit_model_wizard_show_image_in_text=1 должна возвращаться картинка для колдунщика взятая из первой модели
        если есть модели с картинкой
        """
        for flag in ['device=desktop;', 'device=tablet;', 'market_implicit_model_wizard_show_image_in_text=1']:
            response = self.report.request_bs_pb("place=parallel&text=godlike&rearr-factors=" + flag)
            self.assertFragmentIn(
                response, {"market_implicit_model": {"pictures": [LikeUrl.of(Model.DEFAULT_PIC_URL)], "rating": "4.5"}}
            )

    def test_implicit_model_blue_meta_formula(self):
        defaultFormula = "MNA_fml_formula_442417"

        response = self.report.request_bs(
            "place=parallel&text=godlike&rearr-factors=device=desktop;market_implicit_blue_incut_model_count=6&debug=1"
        )
        self.assertFragmentIn(response, {'market_implicit_model': []})
        self.assertFragmentIn(response, 'Using implicit blue model wizard meta MatrixNet formula: ' + defaultFormula)

    def test_implicit_model_parameters(self):
        """https://st.yandex-team.ru/MARKETOUT-29504
        При market_implicit_model_wizard_show_model_parameters=1
        у моделей в неявном должны появляться parameters с характеристиками модели
        """
        response = self.report.request_bs_pb(
            "place=parallel&text=godlike&rearr-factors=market_implicit_model_wizard_show_model_parameters=1"
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                'title': {'text': {'__hl': {'text': "godlike model 1"}}},
                                "parameters": ["model 1 spec1", "4", "iOS"],
                            }
                        ]
                    }
                }
            },
        )

    @classmethod
    def prepare_models_for_title_no_vendor_no_category(cls):
        cls.index.models += [
            Model(hyperid=1, title=' Смартфон Apple iPhone 7 64GB', title_no_vendor='Смартфон iPhone 7 64GB'),
            Model(
                hyperid=2, title='xxxxxx yyyxxx yyyyyy iPhone 8 64GB ', title_no_vendor=' xxxxxx yyyyyy iPhone 8 64GB '
            ),
            Model(hyperid=3, title='Смартфон Apple iPhone 9 64GB'),
        ]
        cls.index.offers += [
            Offer(title='apple 1', hyperid=1, price=10),
            Offer(title='apple 2', hyperid=2, price=20),
            Offer(title='apple 3', hyperid=3, price=30),
        ]

    def test_implicit_model_title_no_vendor_no_category(self):
        """https://st.yandex-team.ru/MARKETOUT-29030
        Check how works deleting category and vendor from titles in imlicit model wizard
        """

        # Проставляем market_implicit_model_title_no_category, так как он по умолчанию включен
        response = self.report.request_bs_pb(
            "place=parallel&text=iphone&rearr-factors=market_implicit_model_title_no_category=0"
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "Смартфон Apple iPhone 7 64GB", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "xxxxxx yyyxxx yyyyyy iPhone 8 64GB", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "Смартфон Apple iPhone 9 64GB", "raw": True}}}},
                        ]
                    }
                }
            },
        )

        response = self.report.request_bs_pb(
            "place=parallel&text=iphone&rearr-factors=market_implicit_model_title_no_category=1"
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "Apple iPhone 7 64GB", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "yyyxxx yyyyyy iPhone 8 64GB", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "Смартфон Apple iPhone 9 64GB", "raw": True}}}},
                        ]
                    }
                }
            },
        )

        # Проставляем market_implicit_model_title_no_category, так как он по умолчанию включен
        response = self.report.request_bs_pb(
            "place=parallel&text=iphone&rearr-factors=market_implicit_model_title_no_vendor=1&rearr-factors=market_implicit_model_title_no_category=0"
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "Смартфон iPhone 7 64GB", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "xxxxxx yyyyyy iPhone 8 64GB", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "Смартфон Apple iPhone 9 64GB", "raw": True}}}},
                        ]
                    }
                }
            },
        )

        response = self.report.request_bs_pb(
            "place=parallel&text=iphone&rearr-factors=market_implicit_model_title_no_category=1;market_implicit_model_title_no_vendor=1"
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "iPhone 7 64GB", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "yyyyyy iPhone 8 64GB", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "Смартфон Apple iPhone 9 64GB", "raw": True}}}},
                        ]
                    }
                }
            },
        )

    def test_filtering_models_without_reviews_in_implicit_model(self):
        """Проверяем, что под флагом market_implicit_model_with_reviews_only=1 в колдунщике неявной модели фильтруются модели без отзывов
        https://st.yandex-team.ru/MARKETOUT-31383
        """
        request = (
            'place=parallel&text=reviewModelFiltering&trace_wizard=1'
            '&rearr-factors=split=1;market_implicit_model_wizard_author_info=1'
        )

        # Без флага market_implicit_model_with_reviews_only=1 в выдаче 4 модели
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "model_count": "4",
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "reviewModelFiltering 1", "raw": True}}},
                                "reasonsToBuy": [
                                    {
                                        "id": "positive_feedback",
                                        "type": "consumerFactor",
                                        "text": "Уникальный аппарат, который уверенно смотрит в будущее",
                                        "author_puid": "1001",
                                        "author_name": "name 1001",
                                        "author_avatar": "avatar_1001",
                                    }
                                ],
                                "reviews": {"count": "10", "authorAvatars": ["avatar_1001"]},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "reviewModelFiltering 2", "raw": True}}},
                                "reasonsToBuy": [
                                    {
                                        "id": "positive_feedback",
                                        "type": "consumerFactor",
                                        "text": "Мощный процессор",
                                        "author_puid": "1002",
                                    }
                                ],
                            },
                            {
                                "title": {"text": {"__hl": {"text": "reviewModelFiltering 3", "raw": True}}},
                                "reasonsToBuy": [
                                    {
                                        "id": "positive_feedback",
                                        "type": "consumerFactor",
                                        "text": "Очень хорошо держит аккумулятор",
                                        "author_puid": "1003",
                                        "author_name": "name 1003",
                                        "author_avatar": "avatar_1003",
                                    }
                                ],
                                "reviews": {"count": "2", "authorAvatars": ["avatar_1003"]},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "reviewModelFiltering 4", "raw": True}}},
                                "reasonsToBuy": [
                                    {"type": "consumerFactor", "factor_name": "Объем памяти", "factor_priority": "2"}
                                ],
                            },
                        ]
                    },
                }
            },
        )

        # Под флагом market_implicit_model_with_reviews_only=1 в выдаче 2 модели
        # для модели 102 нет имени автора и аватарки, для модели 104 нет отзывов
        response = self.report.request_bs_pb(request + ';market_implicit_model_with_reviews_only=1')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "model_count": "2",
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "reviewModelFiltering 1", "raw": True}}},
                                "reasonsToBuy": [
                                    {
                                        "id": "positive_feedback",
                                        "type": "consumerFactor",
                                        "text": "Уникальный аппарат, который уверенно смотрит в будущее",
                                        "author_puid": "1001",
                                        "author_name": "name 1001",
                                        "author_avatar": "avatar_1001",
                                    }
                                ],
                                "reviews": {"count": "10", "authorAvatars": ["avatar_1001"]},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "reviewModelFiltering 3", "raw": True}}},
                                "reasonsToBuy": [
                                    {
                                        "id": "positive_feedback",
                                        "type": "consumerFactor",
                                        "text": "Очень хорошо держит аккумулятор",
                                        "author_puid": "1003",
                                        "author_name": "name 1003",
                                        "author_avatar": "avatar_1003",
                                    }
                                ],
                                "reviews": {"count": "2", "authorAvatars": ["avatar_1003"]},
                            },
                        ]
                    },
                }
            },
        )
        self.assertIn('30 1 Пропущена модель modelId = 102 т.к. у нее нет отзывов', response.get_trace_wizard())
        self.assertIn('30 1 Пропущена модель modelId = 104 т.к. у нее нет отзывов', response.get_trace_wizard())

    def test_review_avatars_in_implicit_model(self):
        """Проверяем количество аватарок для отзывов в колдунщике неявной модели
        https://st.yandex-team.ru/MARKETOUT-31791
        https://st.yandex-team.ru/MARKETOUT-37483
        """
        request = (
            'place=parallel&text=reviewModelFiltering&rearr-factors=split=1;market_implicit_model_wizard_author_info=1'
        )

        # Без флага market_implicit_model_with_reviews_only=1 в выдаче 4 модели
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "model_count": "4",
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "reviewModelFiltering 1", "raw": True}}},
                                "reviews": {"count": "10", "authorAvatars": ElementCount(1)},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "reviewModelFiltering 2", "raw": True}}},
                                "reviews": NoKey('reviews'),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "reviewModelFiltering 3", "raw": True}}},
                                "reviews": {"count": "2", "authorAvatars": ElementCount(1)},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "reviewModelFiltering 4", "raw": True}}},
                                "reviews": NoKey('reviews'),
                            },
                        ]
                    },
                }
            },
            allow_different_len=False,
        )

        # Под флагом market_implicit_model_with_reviews_only=1 в выдаче 2 модели
        # для модели 102 нет имени автора и аватарки, для модели 104 нет отзывов
        response = self.report.request_bs_pb(request + ';market_implicit_model_with_reviews_only=1')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "model_count": "2",
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "reviewModelFiltering 1", "raw": True}}},
                                "reviews": {"count": "10", "authorAvatars": ElementCount(1)},
                            },
                            {
                                "title": {"text": {"__hl": {"text": "reviewModelFiltering 3", "raw": True}}},
                                "reviews": {"count": "2", "authorAvatars": ElementCount(1)},
                            },
                        ]
                    },
                }
            },
            allow_different_len=False,
        )

    def test_review_random_avatar_count_in_implicit_model(self):
        """Проверяем что флаг market_implicit_model_wizard_random_avatar_count задает количество рандомных аватарок
        для отзывов в колдунщике неявной модели
        https://st.yandex-team.ru/MARKETOUT-35669
        """
        request = (
            'place=parallel&text=reviewModelFiltering&rearr-factors=split=1;market_implicit_model_wizard_author_info=1;'
        )

        for flag, avatars_count in [
            ('market_implicit_model_wizard_random_avatar_count=0;', 1),
            ('market_implicit_model_wizard_random_avatar_count=1;', 2),
            ('market_implicit_model_wizard_random_avatar_count=2;', 3),
            ('', 1),
        ]:
            response = self.report.request_bs_pb(request + flag)
            self.assertFragmentIn(
                response,
                {
                    "market_implicit_model": {
                        "showcase": {
                            "items": [
                                {
                                    "title": {"text": {"__hl": {"text": "reviewModelFiltering 1", "raw": True}}},
                                    "reviews": {"authorAvatars": ElementCount(avatars_count)},
                                }
                            ]
                        }
                    }
                },
            )

    def test_review_request_tag(self):
        """Проверяем добавление признака "reviewRequest" для отзывных запросов
        Флаг market_implicit_model_review_tag_model_count задает количество моделей с отзывами,
        необходимое для добавления признака.
        https://st.yandex-team.ru/MARKETOUT-32056
        """
        request = 'place=parallel&text=reviewModelFiltering&rearr-factors=split=1;'
        review_request = 'place=parallel&text=отзывы+reviewModelFiltering&rearr-factors=split=1;'

        for i in (0, 1, 2):
            # Флаг market_implicit_model_review_tag_model_count задает количество моделей с отзывами,
            # необходимое для добавления признака "reviewRequest"
            # При market_implicit_model_review_tag_model_count=0 признак добавляется при любом количестве моделей с отзывами
            response = self.report.request_bs_pb(
                review_request + 'market_implicit_model_review_tag_model_count={};'.format(i)
            )
            self.assertFragmentIn(response, {"market_implicit_model": {"reviewRequest": "1"}})

            # Запрос не отзывной - признака нет
            response = self.report.request_bs_pb(request + 'market_implicit_model_review_tag_model_count={};'.format(i))
            self.assertFragmentIn(response, {"market_implicit_model": {"reviewRequest": NoKey("reviewRequest")}})

        # При market_implicit_model_review_tag_model_count=3 признак не добавляется, так как всего 2 модели содержат отзывы
        response = self.report.request_bs_pb(review_request + 'market_implicit_model_review_tag_model_count=3;')
        self.assertFragmentIn(response, {"market_implicit_model": {"reviewRequest": NoKey("reviewRequest")}})

    @classmethod
    def prepare_boost_meta_formula_by_multitoken(cls):
        cls.index.models += [
            Model(hyperid=201, ts=201, title="filter 1"),
            Model(hyperid=202, ts=202, title="filter 2"),
            Model(hyperid=203, ts=203, title="filter 3"),
            Model(hyperid=204, ts=204, title="filter fz-a60mfe"),
        ]

        cls.index.offers += [Offer(hyperid=201), Offer(hyperid=202), Offer(hyperid=203), Offer(hyperid=204)]

        # Base formula
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 201).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 202).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 203).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 204).respond(0.6)

        # Meta formula
        cls.matrixnet.on_place(MnPlace.IMPLICIT_MODEL_WIZARD_META, 201).respond(0.8)
        cls.matrixnet.on_place(MnPlace.IMPLICIT_MODEL_WIZARD_META, 202).respond(0.7)
        cls.matrixnet.on_place(MnPlace.IMPLICIT_MODEL_WIZARD_META, 203).respond(0.6)
        cls.matrixnet.on_place(MnPlace.IMPLICIT_MODEL_WIZARD_META, 204).respond(0.5)

    def test_boost_meta_formula_by_multitoken(self):
        """Проверяем, что под флагом market_parallel_boost_completely_match_multitoken_coef в колдунщике неявной модели
        для моделей с совпадением по мультитокену бустятся значения базовой и мета формул
        https://st.yandex-team.ru/MARKETOUT-31295
        """
        qtree = {
            'Market': {
                'qtree4market': '\
cHic7VS_q9NgFL3nJi3hs0h58qAElFKXIDwIDrUIgnQqovh803uZGtFHF5c3lU\
6FJ1JExV8Iios_OlrKA8HFwc0x4O7g4Oq_4P2SL2mShgeC4GK23HO_85177knU\
FdVwqEktarPHPm2QSx06R-fpYlonj3y6XBvUtmmXhjTCY9Ar0FvQEegzSJ6voA\
i33OdQ1w2dI8c0He9PXGxljJxjpAFpxtHPH42UUbpLnD710L_goEmugB3y4GP7\
GSdCDlwlxZYuUo926GZd2sg7NXLGPGGHpyBhdt9D7Sl9N2ey6mHXv7N_W2bFlp\
FWr5D2-sX3D5yKM2eqBF6KBZoGI_KpbUSeUQaoFEoTSmR2EoXctnSXpg0Oa850\
2tzUl7m_bDX4s1Uds6iPUFdLi0KYNwMVZnyyUz6E6ybY_T05BRdheUmnFcJWTC\
Wj86MoWI1PYxZ7BIaBrdl8UcT1Hocwm5xCXSsHrOvnbEjyhYLuaBWvrr8uvNY_\
K8fgCtg54adP14-HMImfUCbhC9SNkgQrSVKqwarw7sHLJ9k2rMoQcX8Y-2etEp\
Q52Fa6KiYl8eHZIsA8MwliYqkDs4Dni3xH3sZi0tZzZ7psXfcoeJNL4cPaX0zh\
t4ptRsscXVUKjzazbUbLqm9xnPwsoqVx8R3vYshxDqUYBxE93sFhIWdr8L3j4b\
tFWOz9n9F_llFb3u6joWw98obtnDzQ4G_fTxRY'
            }
        }

        request = "place=parallel&text=filter+fz-a60mfe&wizard-rules={}&trace_wizard=1".format(
            urllib.quote(json.dumps(qtree))
        )

        # 1. Без флага market_parallel_boost_completely_match_multitoken_coef бустинга базовой и мета формул нет
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "filter 1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "filter 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "filter 3", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "filter fz-a60mfe", "raw": True}}}},
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertIn('29 1 Top 3 meta MatrixNet sum: 2.1', response.get_trace_wizard())  # Meta 0.8 + 0.7 + 0.6

        # 2. Под флагом market_parallel_boost_completely_match_multitoken_coef для модели "filter fz-a60mfe" есть совпадение по мультитокену
        # значения базовой и мета формул бустятся
        response = self.report.request_bs_pb(
            request + '&rearr-factors=market_parallel_boost_completely_match_multitoken_coef=2'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "filter fz-a60mfe", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "filter 1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "filter 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "filter 3", "raw": True}}}},
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertIn('29 1 Top 3 meta MatrixNet sum: 2.5', response.get_trace_wizard())  # Meta 0.8 + 0.7 + 0.5*2

    def test_search_url_with_nailed_model(self):
        """Проверяем, что под флагом market_search_url_in_implicit_model_wizard=1 ссылки моделей ведут на search и содержат параметр &rs с прибитой моделью
        https://st.yandex-team.ru/MARKETOUT-31957
        """
        request = "place=parallel&text=iphone&rearr-factors=market_search_url_in_implicit_model_wizard=1"

        # Параметр rs содержит одну прибитую модель
        # python -c 'import base64, sys, zlib; sys.stdout.write(zlib.decompress(base64.urlsafe_b64decode("eJwzYvViFmI0BAADagDJ")))' | protoc --decode_raw
        rs = ["eJwzYvViFmI0BAADagDJ", "eJwzYvViFmI0AgADawDK", "eJwzYvViFmI0BgADbADL"]

        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/search?text=iphone&rs={}&clid=698&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                            rs[0]
                                        ),
                                        ignore_len=False,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/search?text=iphone&rs={}&clid=721&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                            rs[0]
                                        ),
                                        ignore_len=False,
                                    ),
                                },
                                "thumb": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/search?text=iphone&rs={}&clid=698&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                            rs[0]
                                        ),
                                        ignore_len=False,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/search?text=iphone&rs={}&clid=721&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                            rs[0]
                                        ),
                                        ignore_len=False,
                                    ),
                                },
                            },
                            {
                                "title": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/search?text=iphone&rs={}&clid=698&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                            rs[1]
                                        ),
                                        ignore_len=False,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/search?text=iphone&rs={}&clid=721&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                            rs[1]
                                        ),
                                        ignore_len=False,
                                    ),
                                },
                                "thumb": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/search?text=iphone&rs={}&clid=698&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                            rs[1]
                                        ),
                                        ignore_len=False,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/search?text=iphone&rs={}&clid=721&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                            rs[1]
                                        ),
                                        ignore_len=False,
                                    ),
                                },
                            },
                            {
                                "title": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/search?text=iphone&rs={}&clid=698&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                            rs[2]
                                        ),
                                        ignore_len=False,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/search?text=iphone&rs={}&clid=721&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                            rs[2]
                                        ),
                                        ignore_len=False,
                                    ),
                                },
                                "thumb": {
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/search?text=iphone&rs={}&clid=698&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                            rs[2]
                                        ),
                                        ignore_len=False,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/search?text=iphone&rs={}&clid=721&lr=0&utm_medium=cpc&utm_referrer=wizards".format(
                                            rs[2]
                                        ),
                                        ignore_len=False,
                                    ),
                                },
                            },
                        ]
                    }
                }
            },
        )

    def test_not_onstock_parameter_in_urls(self):
        """Проверяем, что под флагом market_parallel_not_onstock_in_urls=1 в ссылки колдунщика неявной модели,
        ведущие на search добавляется параметр &onstock=0
        https://st.yandex-team.ru/MARKETOUT-32130
        """
        request = (
            "place=parallel&text=iphone&rearr-factors=market_parallel_not_onstock_in_urls=1;"
            "market_search_url_in_implicit_model_wizard=1;market_implicit_model_sitelink_reviews=1;market_implicit_model_sitelink_map=1"
        )
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&clid=698&onstock=0"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&clid=721&onstock=0"),
                    "adGUrl": LikeUrl.of("//market.yandex.ru/search?text=iphone&clid=915&onstock=0"),
                    "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&clid=921&onstock=0"),
                    "reviewsUrl": LikeUrl.of("//market.yandex.ru/search?show-reviews=1&text=iphone&clid=698&onstock=0"),
                    "reviewsUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?show-reviews=1&text=iphone&clid=721&onstock=0"
                    ),
                    "reviewsAdGUrl": LikeUrl.of(
                        "//market.yandex.ru/search?show-reviews=1&text=iphone&clid=915&onstock=0"
                    ),
                    "reviewsAdGUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/search?show-reviews=1&text=iphone&clid=921&onstock=0"
                    ),
                    "greenUrl": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&clid=698&onstock=0"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&clid=721&onstock=0"),
                            "adGUrl": LikeUrl.of("//market.yandex.ru/search?text=iphone&clid=915&onstock=0"),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&clid=921&onstock=0"),
                        }
                    ],
                    "button": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&clid=698&onstock=0"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&clid=721&onstock=0"),
                            "adGUrl": LikeUrl.of("//market.yandex.ru/search?text=iphone&clid=915&onstock=0"),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&clid=921&onstock=0"),
                        }
                    ],
                    "showcase": {
                        "items": [
                            {
                                "thumb": {
                                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&clid=698&onstock=0"),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/search?text=iphone&clid=721&onstock=0"
                                    ),
                                    "adGUrl": LikeUrl.of("//market.yandex.ru/search?text=iphone&clid=915&onstock=0"),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/search?text=iphone&clid=921&onstock=0"
                                    ),
                                },
                                "title": {
                                    "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&clid=698&onstock=0"),
                                    "urlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/search?text=iphone&clid=721&onstock=0"
                                    ),
                                    "adGUrl": LikeUrl.of("//market.yandex.ru/search?text=iphone&clid=915&onstock=0"),
                                    "adGUrlTouch": LikeUrl.of(
                                        "//m.market.yandex.ru/search?text=iphone&clid=921&onstock=0"
                                    ),
                                },
                            }
                        ]
                    },
                    "sitelinks": [
                        {
                            "text": "Отзывы",
                            "url": LikeUrl.of(
                                "//market.yandex.ru/search?show-reviews=1&text=iphone&clid=698&onstock=0"
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?show-reviews=1&text=iphone&clid=721&onstock=0"
                            ),
                            "adGUrl": LikeUrl.of(
                                "//market.yandex.ru/search?show-reviews=1&text=iphone&clid=915&onstock=0"
                            ),
                            "adGUrlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/search?show-reviews=1&text=iphone&clid=921&onstock=0"
                            ),
                        },
                        {
                            "text": "На карте",
                            "url": LikeUrl.of("//market.yandex.ru/geo?text=iphone&clid=698&onstock=0"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/geo?text=iphone&clid=721&onstock=0"),
                            "adGUrl": LikeUrl.of("//market.yandex.ru/geo?text=iphone&clid=915&onstock=0"),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/geo?text=iphone&clid=921&onstock=0"),
                        },
                        {
                            "url": LikeUrl.of("//market.yandex.ru/search?text=iphone&clid=698&onstock=0"),
                            "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&clid=721&onstock=0"),
                            "adGUrl": LikeUrl.of("//market.yandex.ru/search?text=iphone&clid=915&onstock=0"),
                            "adGUrlTouch": LikeUrl.of("//m.market.yandex.ru/search?text=iphone&clid=921&onstock=0"),
                        },
                    ],
                }
            },
        )

    @classmethod
    def prepare_collapsing_trace(cls):
        cls.index.models += [
            Model(title='Кран 1', hyperid=301, ts=301),
            Model(title='Кран 2', hyperid=302, ts=302),
            Model(title='Кран 3', hyperid=303, ts=303),
            Model(title='Кран 4', hyperid=304, ts=304),
            Model(title='Техника 1', hyperid=401, ts=401),
        ]

        cls.index.offers += [
            Offer(title='Кран offer 1', hyperid=301),
            Offer(title='Кран offer 2', hyperid=302),
            Offer(title='Кран offer 3', hyperid=303),
            Offer(title='Кран offer 4', hyperid=304),
            Offer(title='Кран Техника 1', hyperid=401, ts=411),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 301).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 302).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 303).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 304).respond(0.5)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 411).respond(0.9)

        cls.matrixnet.on_place(MnPlace.IMPLICIT_MODEL_WIZARD_META, 301).respond(0.5)
        cls.matrixnet.on_place(MnPlace.IMPLICIT_MODEL_WIZARD_META, 302).respond(0.4)
        cls.matrixnet.on_place(MnPlace.IMPLICIT_MODEL_WIZARD_META, 303).respond(0.3)
        cls.matrixnet.on_place(MnPlace.IMPLICIT_MODEL_WIZARD_META, 304).respond(0.2)

        cls.matrixnet.on_place(MnPlace.IMPLICIT_MODEL_WIZARD_META, 401).respond(0.6)

    def test_collapsing_trace(self):
        """Проверяем, что под флагом market_show_collapsing_trace_for_implicit_model_wizard=1 в трейс выводятся
        значения формул и пороги для схлопнутых моделей, в выдаче остаются модели до схлопывания
        https://st.yandex-team.ru/MARKETOUT-33176
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=Кран&trace_wizard=1&rearr-factors=market_show_collapsing_trace_for_implicit_model_wizard=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "Кран 1", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "Кран 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "Кран 3", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "Кран 4", "raw": True}}}},
                        ]
                    }
                }
            },
            allow_different_len=False,
        )

        trace = response.get_trace_wizard()
        self.assertIn(
            '29 1 ImplicitModel.TopCollapsingModelsMnValue.Meta.Value: 3', trace
        )  # Сумма по базовой формуле 0.9+0.8+0.7+0.6 (модели 401, 301, 302, 303)
        self.assertIn(
            '29 1 ImplicitModel.TopCollapsingModelsMnValue.Meta.Threshold: market_implicit_model_wizard_collapsing_top_models_threshold=-100',
            trace,
        )

        self.assertIn(
            '29 1 Top 3 meta MatrixNet sum: 1.5', trace
        )  # Сумма по метаформуле для схлопывания 0.6+0.5+0.4 (модели 401, 301, 302)
        self.assertIn(
            '29 1 ImplicitModel.TopCollapsingModelsMnValue.Meta.Threshold: market_implicit_model_wizard_collapsing_meta_threshold=0.3',
            trace,
        )

    def test_show_new_models(self):
        """Проверяем, что в колдунщик неявной модели добавляются модели-новинки
        https://st.yandex-team.ru/MARKETOUT-34158
        """
        request = 'place=parallel&text=newModel&rids=213'

        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "newModel 1", "raw": True}}},
                                "isNew": True,
                                "offersCount": Absent(),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "newModel 2", "raw": True}}},
                                "isNew": Absent(),
                                "offersCount": 1,
                            },
                        ]
                    }
                }
            },
        )

    def test_medicine_prescription_models_filtering(self):
        """Фильтрация рецептурных лекарственных моделей
        https://st.yandex-team.ru/MARKETOUT-35468
        """
        request = (
            'place=parallel&text=лекарство&trace_wizard=1&rearr-factors=market_parallel_reject_restricted_offers=0;'
        )
        response = self.report.request_bs_pb(
            request + 'market_parallel_filter_prescription_offers_on_base_search=0;'
            'market_parallel_filter_prescription_models_on_base_search=0;'
            'market_parallel_filter_prescription_models=0;'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "Лекарство", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "Лекарство рецептурное", "raw": True}}}},
                        ]
                    }
                }
            },
        )

        response = self.report.request_bs_pb(
            request + 'market_parallel_filter_prescription_offers_on_base_search=0;'
            'market_parallel_filter_prescription_models_on_base_search=0;'
            'market_parallel_filter_prescription_models=1;'
        )
        self.assertFragmentNotIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [{"title": {"text": {"__hl": {"text": "Лекарство рецептурное", "raw": True}}}}]
                    }
                }
            },
        )

        # проверяем, как фильтруются рецептурные модели на базовом, отдельно от схлопывания
        response = self.report.request_bs_pb(
            request + 'market_parallel_filter_prescription_offers_on_base_search=0;'
            'market_parallel_filter_prescription_models_on_base_search=1;'
            'market_parallel_filter_prescription_models=0;'
            'market_parallel_use_collapsing=0;'
        )
        self.assertFragmentNotIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [{"title": {"text": {"__hl": {"text": "Лекарство рецептурное", "raw": True}}}}]
                    }
                }
            },
        )

        # проверяем, как фильтруются рецептурные офферы на базовом при схлопывании
        response = self.report.request_bs_pb(
            request + 'market_parallel_filter_prescription_offers_on_base_search=1;'
            'market_parallel_filter_prescription_models_on_base_search=0;'
            'market_parallel_filter_prescription_models=0;'
            'market_parallel_reject_medicine_models=1'
        )
        self.assertFragmentNotIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [{"title": {"text": {"__hl": {"text": "Лекарство рецептурное", "raw": True}}}}]
                    }
                }
            },
        )

    @classmethod
    def prepare_cpa_offers_in_models(cls):

        cls.index.shops += [
            Shop(fesh=3605511, priority_region=213),
            Shop(fesh=3605512, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(
                fesh=431782,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                priority_region=213,
                name='Яндекс.Маркет',
            ),
            Shop(fesh=431783, priority_region=213, name='Синий.Маркет', warehouse_id=145),
        ]

        cls.index.models += [
            Model(hyperid=3605501, ts=3605501, title="cpa 1"),
            Model(hyperid=3605502, ts=3605502, title="cpa 2"),
            Model(hyperid=3605503, ts=3605503, title="cpa 3"),
            Model(hyperid=3605504, ts=3605504, title="cpa 4"),
            Model(hyperid=3605505, ts=3605505, title="cpa 5"),
        ]

        cls.index.offers += [
            Offer(hyperid=3605501, waremd5='1sFnEBncNV6VLkT9w4BajQ', ts=3605511, fesh=3605511),
            Offer(hyperid=3605502, waremd5='2sFnEBncNV6VLkT9w4BajQ', ts=3605512, fesh=3605511),
            Offer(hyperid=3605503, waremd5='3sFnEBncNV6VLkT9w4BajQ', ts=3605513, fesh=3605511),
            Offer(hyperid=3605504, waremd5='4sFnEBncNV6VLkT9w4BajQ', ts=3605514, fesh=3605511),
            Offer(
                hyperid=3605502,
                waremd5='5sFnEBncNV6VLkT9w4BajQ',
                title='dsbs 1',
                ts=3605515,
                fesh=3605512,
                discount=50,
                has_url=False,
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=3605504,
                waremd5='6sFnEBncNV6VLkT9w4BajQ',
                title='dsbs 2',
                ts=3605516,
                fesh=3605512,
                has_url=False,
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=3605505,
                waremd5='7sFnEBncNV6VLkT9w4BajQ',
                title='dsbs 3',
                ts=3605519,
                sku=0,
                fesh=3605512,
                has_url=False,
                cpa=Offer.CPA_REAL,
            ),
        ]

        blue_cashback_1 = Promo(
            promo_type=PromoType.BLUE_CASHBACK,
            key=b64url_md5(next(nummer)),
            start_date=now - delta,
            end_date=now + delta,
            blue_cashback=PromoBlueCashback(share=0.2, version=1, priority=1),
        )

        blue_cashback_2 = Promo(
            promo_type=PromoType.BLUE_CASHBACK,
            key=b64url_md5(next(nummer)),
            start_date=now - delta,
            end_date=now + delta,
            blue_cashback=PromoBlueCashback(
                share=0.5,
                version=1,
                priority=1,
            ),
            restrictions=PromoRestrictions(predicates=[{'perks': ['yandex_extra_cashback']}]),
        )

        cls.index.mskus += [
            MarketSku(
                sku=36055030,
                hyperid=3605503,
                blue_offers=[
                    BlueOffer(
                        ts=3605517,
                        title='blue 1',
                        waremd5='m3FnEBncNV6VLkT9w4BajQ',
                        feedid=431782,
                        delivery_buckets=[101],
                        price=100,
                        discount=20,
                        promo=[blue_cashback_1, blue_cashback_2],
                    )
                ],
            ),
            MarketSku(
                sku=36055040,
                hyperid=3605504,
                blue_offers=[
                    BlueOffer(
                        ts=3605518,
                        title='blue 2',
                        waremd5='m4FnEBncNV6VLkT9w4BajQ',
                        feedid=431782,
                        delivery_buckets=[101],
                        price=100,
                        promo=[blue_cashback_1, blue_cashback_2],
                    )
                ],
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=101,
                fesh=431782,
                carriers=[1, 3],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=500, day_from=1, day_to=4)])],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3605501).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3605502).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3605503).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3605504).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3605511).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3605512).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3605513).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3605514).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3605515).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3605516).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3605517).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3605518).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3605519).respond(0.55)

    def test_cpa_offers_in_models(self):
        """Проверяем, что под флагом market_implicit_model_cpa_offer
        в моделях колдунщика неявной приходит покупочный оффер
        """
        rearrs = [
            'market_implicit_model_cpa_offer=1',
            'market_show_virtual_msku_id=1',
            'market_cards_everywhere_sku_offers=1',
            'market_white_cpa_on_blue=1',
            'market_dsbs_tariffs=0',
            'market_unified_tariffs=0',
        ]
        request = 'place=parallel&text=cpa&rids=213&reqid=1234578adcdef&rearr-factors=' + ';'.join(rearrs) + ';'
        response = self.report.request_bs_pb(request + 'market_extra_cashback_on_parallel=0;')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "modelId": "3605501",
                                "cpaOffer": Absent(),
                            },
                            {
                                "modelId": "3605502",
                                "cpaOffer": {
                                    "title": "dsbs 1",
                                    "url": LikeUrl.of(
                                        '//market.yandex.ru/product/3605502?do-waremd5=5sFnEBncNV6VLkT9w4BajQ&clid=698&wprid=1234578adcdef'
                                    ),
                                    "offerId": "5sFnEBncNV6VLkT9w4BajQ",
                                    "skuId": Absent(),
                                    "price": "100",
                                    "currency": "RUR",
                                    "discountPercent": 50,
                                    "oldPrice": "200",
                                    "deliveryOption": {"dayFrom": 0, "dayTo": 2, "price": 100},
                                },
                            },
                            {
                                "modelId": "3605503",
                                "skuId": "36055030",
                                "cpaOffer": {
                                    "title": "blue 1",
                                    "url": LikeUrl.of(
                                        '//market.yandex.ru/product/3605503?do-waremd5=m3FnEBncNV6VLkT9w4BajQ&clid=698&wprid=1234578adcdef'
                                    ),
                                    "offerId": "m3FnEBncNV6VLkT9w4BajQ",
                                    "skuId": "36055030",
                                    "price": "100",
                                    "currency": "RUR",
                                    "cashback": "20",
                                },
                            },
                            {
                                "modelId": "3605504",
                                "skuId": "36055040",
                                "cpaOffer": {
                                    "title": "blue 2",
                                    "url": LikeUrl.of(
                                        '//market.yandex.ru/product/3605504?do-waremd5=m4FnEBncNV6VLkT9w4BajQ&clid=698&wprid=1234578adcdef'
                                    ),
                                    "offerId": "m4FnEBncNV6VLkT9w4BajQ",
                                    "skuId": "36055040",
                                    "price": "100",
                                    "currency": "RUR",
                                    "cashback": "20",
                                },
                            },
                            {
                                "modelId": "3605505",
                                "cpaOffer": {
                                    "title": "dsbs 3",
                                    "url": LikeUrl.of(
                                        '//market.yandex.ru/product/3605505?do-waremd5=7sFnEBncNV6VLkT9w4BajQ&clid=698&wprid=1234578adcdef'
                                    ),
                                    "offerId": "7sFnEBncNV6VLkT9w4BajQ",
                                    "skuId": Absent(),
                                    "price": "100",
                                    "currency": "RUR",
                                },
                            },
                        ]
                    }
                }
            },
            allow_different_len=False,
        )

        # проверяем повышенный кэшбек
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {"cpaOffer": Absent()},
                            {"cpaOffer": {"cashback": Absent()}},
                            {"cpaOffer": {"cashback": "50"}},
                            {"cpaOffer": {"cashback": "50"}},
                            {"cpaOffer": {"cashback": Absent()}},
                        ]
                    }
                }
            },
            allow_different_len=False,
        )

        # проверяем ссылку на КО
        response = self.report.request_bs_pb(request + 'market_implicit_model_cpa_offer_url_type=OfferCard;')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {"cpaOffer": Absent()},
                            {"cpaOffer": {"url": Contains('market-click2.yandex.ru/redir')}},
                            {"cpaOffer": {"url": Contains('market-click2.yandex.ru/redir')}},
                            {"cpaOffer": {"url": Contains('market-click2.yandex.ru/redir')}},
                            {"cpaOffer": {"url": Contains('//market-click2.yandex.ru/redir')}},
                        ]
                    }
                }
            },
            allow_different_len=False,
        )

        self.show_log.expect(
            ware_md5='5sFnEBncNV6VLkT9w4BajQ',
            url_type=UrlType.OFFERCARD,
            url=LikeUrl.of('//market.yandex.ru/offer/5sFnEBncNV6VLkT9w4BajQ?clid=698&wprid=1234578adcdef'),
        )

        self.show_log.expect(
            ware_md5='m3FnEBncNV6VLkT9w4BajQ',
            url_type=UrlType.OFFERCARD,
            url=LikeUrl.of('//market.yandex.ru/offer/m3FnEBncNV6VLkT9w4BajQ?clid=698&wprid=1234578adcdef'),
        )

        self.show_log.expect(
            ware_md5='m4FnEBncNV6VLkT9w4BajQ',
            url_type=UrlType.OFFERCARD,
            url=LikeUrl.of('//market.yandex.ru/offer/m4FnEBncNV6VLkT9w4BajQ?clid=698&wprid=1234578adcdef'),
        )

    def test_cpa_offers_in_models_touch(self):
        """Проверяем, что под флагом market_implicit_model_cpa_offer
        в моделях колдунщика неявной на таче приходит покупочный оффер
        """
        rearrs = [
            'market_implicit_model_cpa_offer=1',
            'device=touch',
            'market_show_virtual_msku_id=1',
            'market_cards_everywhere_sku_offers=1',
            'market_white_cpa_on_blue=1',
            'market_dsbs_tariffs=0',
            'market_unified_tariffs=0',
        ]
        request = 'place=parallel&text=cpa&rids=213&reqid=1234578adcdef&rearr-factors=' + ';'.join(rearrs) + ';'
        response = self.report.request_bs_pb(request + 'market_extra_cashback_on_parallel=0;')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {
                                "modelId": "3605501",
                                "cpaOffer": Absent(),
                            },
                            {
                                "modelId": "3605502",
                                "cpaOffer": {
                                    "title": "dsbs 1",
                                    "url": LikeUrl.of(
                                        '//market.yandex.ru/product/3605502?do-waremd5=5sFnEBncNV6VLkT9w4BajQ&clid=721&wprid=1234578adcdef'
                                    ),
                                    "offerId": "5sFnEBncNV6VLkT9w4BajQ",
                                    "skuId": Absent(),
                                    "price": "100",
                                    "currency": "RUR",
                                    "deliveryOption": {"dayFrom": 0, "dayTo": 2, "price": 100},
                                },
                            },
                            {
                                "modelId": "3605503",
                                "cpaOffer": {
                                    "title": "blue 1",
                                    "url": LikeUrl.of(
                                        '//market.yandex.ru/product/3605503?do-waremd5=m3FnEBncNV6VLkT9w4BajQ&clid=721&wprid=1234578adcdef'
                                    ),
                                    "offerId": "m3FnEBncNV6VLkT9w4BajQ",
                                    "skuId": "36055030",
                                    "price": "100",
                                    "oldPrice": "125",
                                    "discountPercent": 20,
                                    "currency": "RUR",
                                    "cashback": "20",
                                },
                            },
                            {
                                "modelId": "3605504",
                                "cpaOffer": {
                                    "title": "blue 2",
                                    "url": LikeUrl.of(
                                        '//market.yandex.ru/product/3605504?do-waremd5=m4FnEBncNV6VLkT9w4BajQ&clid=721&wprid=1234578adcdef'
                                    ),
                                    "offerId": "m4FnEBncNV6VLkT9w4BajQ",
                                    "skuId": "36055040",
                                    "price": "100",
                                    "currency": "RUR",
                                    "cashback": "20",
                                },
                            },
                            {
                                "modelId": "3605505",
                                "cpaOffer": {
                                    "title": "dsbs 3",
                                    "url": LikeUrl.of(
                                        '//market.yandex.ru/product/3605505?do-waremd5=7sFnEBncNV6VLkT9w4BajQ&clid=721&wprid=1234578adcdef'
                                    ),
                                    "offerId": "7sFnEBncNV6VLkT9w4BajQ",
                                    "skuId": Absent(),
                                    "price": "100",
                                    "currency": "RUR",
                                },
                            },
                        ]
                    }
                }
            },
            allow_different_len=False,
        )

        # проверяем повышенный кэшбек
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {"cpaOffer": Absent()},
                            {"cpaOffer": {"cashback": Absent()}},
                            {"cpaOffer": {"cashback": "50"}},
                            {"cpaOffer": {"cashback": "50"}},
                            {"cpaOffer": {"cashback": Absent()}},
                        ]
                    }
                }
            },
            allow_different_len=False,
        )

        # проверяем ссылку на КО
        response = self.report.request_bs_pb(request + 'market_implicit_model_cpa_offer_url_type=OfferCard;')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {"cpaOffer": Absent()},
                            {"cpaOffer": {"url": Contains('//market-click2.yandex.ru/redir')}},
                            {"cpaOffer": {"url": Contains('//market-click2.yandex.ru/redir')}},
                            {"cpaOffer": {"url": Contains('//market-click2.yandex.ru/redir')}},
                            {"cpaOffer": {"url": Contains('//market-click2.yandex.ru/redir')}},
                        ]
                    }
                }
            },
            allow_different_len=False,
        )

        self.show_log.expect(
            ware_md5='5sFnEBncNV6VLkT9w4BajQ',
            url_type=UrlType.OFFERCARD,
            url=LikeUrl.of('//market.yandex.ru/offer/5sFnEBncNV6VLkT9w4BajQ?clid=721&wprid=1234578adcdef'),
        )

        self.show_log.expect(
            ware_md5='m3FnEBncNV6VLkT9w4BajQ',
            url_type=UrlType.OFFERCARD,
            url=LikeUrl.of('//market.yandex.ru/offer/m3FnEBncNV6VLkT9w4BajQ?clid=721&wprid=1234578adcdef'),
        )

        self.show_log.expect(
            ware_md5='m4FnEBncNV6VLkT9w4BajQ',
            url_type=UrlType.OFFERCARD,
            url=LikeUrl.of('//market.yandex.ru/offer/m4FnEBncNV6VLkT9w4BajQ?clid=721&wprid=1234578adcdef'),
        )

    def test_cpa_filter_in_title_url(self):
        """Проверяем что под флагом market_cpa_filter_in_wizard_title_urls=1 в ссылку тайтла добавляется фильтр CPA
        https://st.yandex-team.ru/MARKETOUT-36793
        https://st.yandex-team.ru/MARKETOUT-37552
        """
        query = (
            'place=parallel&text=iphone&rearr-factors=market_enable_implicit_model_wiz_center_incut=1;market_enable_implicit_model_wiz_without_incut=1;'
            'market_cpa_filter_in_wizard_title_urls=1;'
        )

        # Под флагом market_cpa_filter_in_wizard_title_urls=1 CPA фильтр добавляется во все колдунщики
        response = self.report.request_bs_pb(query)
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of("//market.yandex.ru/search?lr=0&text=iphone&clid=698&cpa=1"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?lr=0&text=iphone&clid=721&cpa=1"),
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model_center_incut": {
                    "url": LikeUrl.of("//market.yandex.ru/search?lr=0&text=iphone&clid=836&cpa=1"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?lr=0&text=iphone&clid=837&cpa=1"),
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model_without_incut": {
                    "url": LikeUrl.of("//market.yandex.ru/search?lr=0&text=iphone&clid=834&cpa=1"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?lr=0&text=iphone&clid=835&cpa=1"),
                }
            },
        )

        # Под флагом market_cpa_filter_in_wizard_title_urls_vt=market_implicit_model,market_implicit_model_center_incut
        # CPA фильтр добавляется только в колдунщики market_implicit_model и market_implicit_model_center_incut
        response = self.report.request_bs_pb(
            query + 'market_cpa_filter_in_wizard_title_urls_vt=market_implicit_model,market_implicit_model_center_incut'
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "url": LikeUrl.of("//market.yandex.ru/search?lr=0&text=iphone&clid=698&cpa=1"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?lr=0&text=iphone&clid=721&cpa=1"),
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model_center_incut": {
                    "url": LikeUrl.of("//market.yandex.ru/search?lr=0&text=iphone&clid=836&cpa=1"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?lr=0&text=iphone&clid=837&cpa=1"),
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model_without_incut": {
                    "url": LikeUrl.of("//market.yandex.ru/search?lr=0&text=iphone&clid=834", no_params=['cpa']),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/search?lr=0&text=iphone&clid=835", no_params=['cpa']),
                }
            },
        )

    def test_category_id_field(self):
        """Проверяем наличие поля categoryId у модели
        https://st.yandex-team.ru/MARKETOUT-37684
        """

        response = self.report.request_bs_pb("place=parallel&text=godlike")
        self.assertFragmentIn(response, {"market_implicit_model": {"showcase": {"items": [{"categoryId": 201}]}}})


if __name__ == '__main__':
    main()
