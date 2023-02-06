#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    CategoryRestriction,
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
    Region,
    RegionalRestriction,
    Shop,
    YamarecPlaceReasonsToBuy,
)
from core.testcase import TestCase, main

from core.matcher import ElementCount, LikeUrl, NoKey
from core.blackbox import BlackboxUser

import json
import urllib


class T(TestCase):
    @classmethod
    def prepare(cls):
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

    @classmethod
    def prepare_boost_meta_formula_by_multitoken(cls):
        cls.index.models += [
            Model(hyperid=201, ts=201, title="filter 1"),
            Model(hyperid=202, ts=202, title="filter 2"),
            Model(hyperid=203, ts=203, title="filter 3"),
            Model(hyperid=204, ts=204, title="filter fz-a60mfe"),
        ]

        cls.index.offers += [Offer(hyperid=201), Offer(hyperid=202), Offer(hyperid=203), Offer(hyperid=204)]

        cls.index.mskus += [
            MarketSku(
                sku=2010,
                title="godlike sku 21",
                hyperid=201,
                blue_offers=[BlueOffer(price=100, feedid=3), BlueOffer(price=200, feedid=3)],
            ),
        ]

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

    @classmethod
    def prepare_panasonic_tumix(cls):
        cls.index.gltypes += [
            GLType(param_id=13213456, hid=30, gltype=GLType.ENUM, values=[100, 16358321]),
        ]

        cls.index.models += [
            Model(
                hyperid=301,
                ts=101000,
                hid=30,
                title="panasonic tumix 5000",
                opinion=Opinion(rating=4.5, rating_count=10, total_count=12, precise_rating=4.46),
                picinfo='//avatars.mds.yandex.net/get-mpic/5678/test9901/orig#100#100',
                glparams=[GLParam(param_id=13213456, value=16358321)],
            ),
            Model(
                hyperid=302,
                ts=102000,
                hid=30,
                title="panasonic tumix pt6000",
                opinion=Opinion(rating=2.5, precise_rating=2.73),
                picinfo='//avatars.mds.yandex.net/get-mpic/5678/test99/orig#100#100',
                glparams=[GLParam(param_id=13213456, value=16358321)],
            ),
        ]
        cls.index.offers += [
            Offer(hyperid=301, price=666, fesh=12),
            Offer(hyperid=301, price=700),
            Offer(hyperid=302, price=777),
            Offer(hyperid=302, price=800),
            # offer for testing shop_count
            Offer(title='panasonic tumix', waremd5='obmaDftYV4bsBdqYgc5WmQ'),
        ]

    @classmethod
    def prepare_models_with_equal_model_ids(cls):
        cls.index.models += [
            Model(hyperid=305, ts=101001, hid=30000, title="dfdgfdgd"),
        ]
        cls.index.offers += [
            Offer(hyperid=305, price=13, title="ABACABCABACAB"),
            Offer(hyperid=305, price=12, title="ABACABCABACAB"),
        ]

    def test_boost_meta_formula_by_multitoken(self):
        """Проверяем, что под флагом market_parallel_boost_completely_match_multitoken_coef в колдунщике неявной модели
        для моделей с совпадением по мультитокену бустятся значения базовой и мета формул
        https://st.yandex-team.ru/MARKETOUT-31295
        """
        qtree = {
            'Market': {
                'qtree4market': 'cHic7VS_q9NgFL3nJi3hs0h58qAElFKXIDwIDrUIgnQqovh803uZGtFHF5c3lU6FJ1JExV8Iios_OlrKA8HFwc0x4O7g4Oq_4P2SL2mShgeC4GK23HO_85177knUFdVwqEktarPHPm2QSx06R-fpYlonj3y6XBvUtmmXhjTCY9Ar0FvQEegzSJ6voAi33OdQ1w2dI8c0He9PXGxljJxjpAFpxtHPH42UUbpLnD710L_goEmugB3y4GP7GSdCDlwlxZYuUo926GZd2sg7NXLGPGGHpyBhdt9D7Sl9N2ey6mHXv7N_W2bFlpFWr5D2-sX3D5yKM2eqBF6KBZoGI_KpbUSeUQaoFEoTSmR2EoXctnSXpg0Oa8502tzUl7m_bDX4s1Uds6iPUFdLi0KYNwMVZnyyUz6E6ybY_T05BRdheUmnFcJWTCWj86MoWI1PYxZ7BIaBrdl8UcT1Hocwm5xCXSsHrOvnbEjyhYLuaBWvrr8uvNY_K8fgCtg54adP14-HMImfUCbhC9SNkgQrSVKqwarw7sHLJ9k2rMoQcX8Y-2etEpQ52Fa6KiYl8eHZIsA8MwliYqkDs4Dni3xH3sZi0tZzZ7psXfcoeJNL4cPaX0zht4ptRsscXVUKjzazbUbLqm9xnPwsoqVx8R3vYshxDqUYBxE93sFhIWdr8L3j4btFWOz9n9F_llFb3u6joWw98obtnDzQ4G_fTxRY'  # noqa
            }
        }

        request = "place=parallel&text=filter+fz-a60mfe&wizard-rules={}&trace_wizard=1&rearr-factors=market_enable_reask_gallery_wiz=1;market_enable_reask_gallery_wizard_models=1".format(
            urllib.quote(json.dumps(qtree))
        )

        # 1. Без флага market_parallel_boost_completely_match_multitoken_coef бустинга базовой и мета формул нет
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_reask_gallery": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "filter 1", "raw": True}}},
                                "skuId": "2010",
                            },
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
            request
            + '&rearr-factors=market_parallel_boost_completely_match_multitoken_coef=2;market_enable_reask_gallery_wiz=1;market_enable_reask_gallery_wizard_models=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_reask_gallery": {
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

    def test_mini_gallery_response_grouping(self):
        """
        Проверяем, что мини-галерея всегда отдаётся в отдельной группировке
        https://st.yandex-team.ru/ECOMSERP-28
        """

        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix&g=1.market.10.1.-1...-1.....&rearr-factors=market_enable_reask_gallery_wiz=1;market_enable_reask_gallery_wizard_models=1&modelid=301&ag0=previous_market'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                "market_reask_gallery": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "url": LikeUrl.of(
                                        "https://yandex.ru/search?text=panasonic%20tumix%205000&lr=0&modelid=301",
                                        ignore_len=True,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "https://yandex.ru/search/touch?text=panasonic%20tumix%205000&lr=0&modelid=301",
                                        ignore_len=True,
                                    ),
                                    "text": {"__hl": {"text": "panasonic tumix 5000", "raw": True}},
                                },
                                "selected": True,
                            }
                        ]
                    },
                    "type": "market_constr",
                    "subtype": "market_mini_reask_gallery",
                },
            },
        )
        report = response.get_report()
        assert len(report.Grouping) == 1 and report.Grouping[0].Attr == "previous_market"

        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix&g=1.market.10.1.-1...-1.....&rearr-factors=market_enable_reask_gallery_wiz=1;market_enable_reask_gallery_wizard_models=1&ag0=market'
        )
        report = response.get_report()
        assert len(report.Grouping) == 1 and report.Grouping[0].Attr == "market"

    def test_mark_as_selected_model_in_response(self):
        """
        Для выбранной пользователем модели выставлять "selected": 1
        https://st.yandex-team.ru/ECOMSERP-28
        """

        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix&modelid=301&g=1.market.10.1.-1...-1.....&rearr-factors=market_enable_reask_gallery_wiz=1;market_enable_reask_gallery_wizard_models=1;'
        )
        self.assertFragmentIn(
            response,
            {
                "market_reask_gallery": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "url": LikeUrl.of(
                                        "https://yandex.ru/search?text=panasonic%20tumix%205000&lr=0&modelid=301",
                                        ignore_len=True,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "https://yandex.ru/search/touch?text=panasonic%20tumix%205000&lr=0&modelid=301",
                                        ignore_len=True,
                                    ),
                                    "text": {"__hl": {"text": "panasonic tumix 5000", "raw": True}},
                                },
                                "selected": True,
                            },
                            {
                                "title": {
                                    "url": LikeUrl.of(
                                        "https://yandex.ru/search?text=panasonic%20tumix%20pt6000&lr=0&modelid=302",
                                        ignore_len=True,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "https://yandex.ru/search/touch?text=panasonic%20tumix%20pt6000&lr=0&modelid=302",
                                        ignore_len=True,
                                    ),
                                    "text": {"__hl": {"text": "panasonic tumix pt6000", "raw": True}},
                                },
                                "selected": NoKey("selected"),  # Модель не выбрана
                            },
                        ]
                    }
                }
            },
        )

    def test_get_items_only_from_cgi_reask_modelids(self):
        """
        Модели в items формируются по средством cgi-параметра reask-modelids.
        В items попадают только те, которые указаны в reask-modelids.
        https://st.yandex-team.ru/ECOMSERP-39
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=nerelevanrtn&reask-modelids=301&g=1.market.10.1.-1...-1.....&rearr-factors=market_enable_reask_gallery_wiz=1;market_enable_reask_gallery_wizard_models=1;'
        )
        self.assertFragmentIn(response, {"market_reask_gallery": {"showcase": {"items": ElementCount(1)}}})

        self.assertFragmentIn(
            response,
            {
                "market_reask_gallery": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "url": LikeUrl.of(
                                        "https://yandex.ru/search?text=panasonic%20tumix%205000&lr=0&modelid=301&reask-modelids=301",
                                        ignore_len=True,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "https://yandex.ru/search/touch?text=panasonic%20tumix%205000&lr=0&modelid=301&reask-modelids=301",
                                        ignore_len=True,
                                    ),
                                    "text": {"__hl": {"text": "panasonic tumix 5000", "raw": True}},
                                },
                            }
                        ]
                    }
                }
            },
        )

    def test_get_items_from_cgi_reask_modelids_with_preserved_order(self):
        """
        Модели в items формируются по средством cgi-параметра reask-modelids.
        В items попадают модели точно в таком же порядке, как в reask-modelids.
        https://st.yandex-team.ru/ECOMSERP-39
        """
        response = self.report.request_bs_pb(
            'place=parallel&text=nerelevanrtn&reask-modelids=302;301&g=1.market.10.1.-1...-1.....&rearr-factors=market_enable_reask_gallery_wiz=1;market_enable_reask_gallery_wizard_models=1;'
        )
        self.assertFragmentIn(response, {"market_reask_gallery": {"showcase": {"items": ElementCount(2)}}})

        self.assertFragmentIn(
            response,
            {
                "market_reask_gallery": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "url": LikeUrl.of(
                                        "https://yandex.ru/search?text=panasonic%20tumix%20pt6000&lr=0&modelid=302&reask-modelids=302;301",
                                        ignore_len=True,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "https://yandex.ru/search/touch?text=panasonic%20tumix%20pt6000&lr=0&modelid=302&reask-modelids=302;301",
                                        ignore_len=True,
                                    ),
                                    "text": {"__hl": {"text": "panasonic tumix pt6000", "raw": True}},
                                },
                            },
                            {
                                "title": {
                                    "url": LikeUrl.of(
                                        "https://yandex.ru/search?text=panasonic%20tumix%205000&lr=0&modelid=301&reask-modelids=302;301",
                                        ignore_len=True,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "https://yandex.ru/search/touch?text=panasonic%20tumix%205000&lr=0&modelid=301&reask-modelids=302;301",
                                        ignore_len=True,
                                    ),
                                    "text": {"__hl": {"text": "panasonic tumix 5000", "raw": True}},
                                },
                            },
                        ]
                    }
                },
            },
            preserve_order=True,
        )

    def test_set_reask_modelids_to_url(self):
        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix&g=1.market.10.1.-1...-1.....&rearr-factors=market_enable_reask_gallery_wiz=1;market_enable_reask_gallery_wizard_models=1;'
        )
        ids = "301;302"
        wizards = response.extract_wizards()
        for wizard in wizards:
            if 'market_reask_gallery' in wizard:
                url = wizard["market_reask_gallery"]["showcase"]["items"][0]["title"]["url"]
                ids = "302;301" if url.find("reask-modelids=302;301") != -1 else "301;302"
                break
        self.assertFragmentIn(
            response,
            {
                "market_reask_gallery": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "url": LikeUrl.of(
                                        "https://yandex.ru/search?text=panasonic%20tumix%205000&lr=0&modelid=301&reask-modelids="
                                        + ids,
                                        ignore_len=True,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "https://yandex.ru/search/touch?text=panasonic%20tumix%205000&lr=0&modelid=301&reask-modelids="
                                        + ids,
                                        ignore_len=True,
                                    ),
                                    "text": {"__hl": {"text": "panasonic tumix 5000", "raw": True}},
                                },
                            },
                            {
                                "title": {
                                    "url": LikeUrl.of(
                                        "https://yandex.ru/search?text=panasonic%20tumix%20pt6000&lr=0&modelid=302&reask-modelids="
                                        + ids,
                                        ignore_len=True,
                                    ),
                                    "urlTouch": LikeUrl.of(
                                        "https://yandex.ru/search/touch?text=panasonic%20tumix%20pt6000&lr=0&modelid=302&reask-modelids="
                                        + ids,
                                        ignore_len=True,
                                    ),
                                    "text": {"__hl": {"text": "panasonic tumix pt6000", "raw": True}},
                                },
                            },
                        ]
                    }
                }
            },
        )

    def test_reask_gallery_with_cost_filter(self):
        """
        Формируются ценовые фильтры
        https://st.yandex-team.ru/ECOMSERP-45
        """

        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix&g=1.market.10.1.-1...-1.....&rearr-factors=market_enable_reask_gallery_wiz=1;market_enable_reask_gallery_wizard_filters=1;market_include_reask_gallery_wizard_cost_filters=1;market_reask_gallery_price_bucket_size=100;market_enable_reask_gallery_wizard_models=1'  # noqa
        )

        self.assertFragmentIn(
            response,
            {
                "market_reask_gallery": {
                    "filters": [
                        {
                            "text": "До 200 ₽",
                            "url": LikeUrl.of(
                                "https://yandex.ru/search?lr=0&reask-wizard-state=mcpriceto%3A200&text=panasonic%20tumix"
                            ),
                        },
                        {
                            "text": "200 - 700 ₽",
                            "url": LikeUrl.of(
                                "https://yandex.ru/search?lr=0&reask-wizard-state=mcpricefrom%3A200;mcpriceto%3A700&text=panasonic%20tumix"
                            ),
                        },
                        {
                            "text": "От 700 ₽",
                            "url": LikeUrl.of(
                                "https://yandex.ru/search?lr=0&reask-wizard-state=mcpricefrom%3A700&text=panasonic%20tumix"
                            ),
                        },
                    ],
                    "showcase": {
                        "items": [
                            {"price": {"priceMin": "666"}},
                            {"price": {"priceMin": "777"}},
                        ]
                    },
                }
            },
        )

    def test_filter_models_by_price(self):
        """
        Фильтраця по ценам на парралельном
        https://st.yandex-team.ru/ECOMSERP-45
        """

        response = self.report.request_bs_pb(
            'place=parallel&text=iPhone&g=1.market.10.1.-1...-1.....&rearr-factors=market_enable_reask_gallery_wiz=1;market_enable_reask_gallery_wizard_models=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_reask_gallery": {
                    "showcase": {
                        "items": [
                            {"price": {"priceMin": "10"}},
                            {"price": {"priceMin": "20"}},
                            {"price": {"priceMin": "30"}},
                        ]
                    }
                }
            },
        )

        response = self.report.request_bs_pb(
            'place=parallel&text=iPhone&g=1.market.10.1.-1...-1.....&rearr-factors=market_enable_reask_gallery_wiz=1;market_enable_reask_gallery_wizard_models=1&reask-wizard-state=mcpricefrom%3A20'
        )
        self.assertFragmentIn(
            response,
            {
                "market_reask_gallery": {
                    "showcase": {
                        "items": [
                            {"price": {"priceMin": "20"}},
                            {"price": {"priceMin": "30"}},
                        ]
                    }
                }
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "market_reask_gallery": {
                    "showcase": {
                        "items": [
                            {"price": {"priceMin": "10"}},
                        ]
                    }
                }
            },
        )

        response = self.report.request_bs_pb(
            'place=parallel&text=iPhone&g=1.market.10.1.-1...-1.....&rearr-factors=market_enable_reask_gallery_wiz=1;market_enable_reask_gallery_wizard_models=1&reask-wizard-state=mcpriceto%3A20'
        )
        self.assertFragmentIn(
            response,
            {
                "market_reask_gallery": {
                    "showcase": {
                        "items": [
                            {"price": {"priceMin": "10"}},
                            {"price": {"priceMin": "20"}},
                        ]
                    }
                }
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "market_reask_gallery": {
                    "showcase": {
                        "items": [
                            {"price": {"priceMin": "30"}},
                        ]
                    }
                }
            },
        )

    def test_reask_gallery_with_cost_filter_after_filtration(self):
        """
        После того, как ценовые фильтры уже применены, больше они не предлагаются в поле filters
        https://st.yandex-team.ru/ECOMSERP-45
        """

        response = self.report.request_bs_pb(
            'place=parallel&text=iPhone&g=1.market.10.1.-1...-1.....&rearr-factors=market_enable_reask_gallery_wiz=1;market_enable_reask_gallery_wizard_models=1;market_enable_reask_gallery_wizard_filters=1;market_include_reask_gallery_wizard_cost_filters=1;market_reask_gallery_price_bucket_size=5&reask-wizard-state=mcpriceto%3A20'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                "market_reask_gallery": {
                    "filters": NoKey("filters"),
                }
            },
        )

    def test_reask_gallery_should_contain_models_with_different_ids(self):
        """
        Модели в колдунщика reask models должны быть с разными id
        https://st.yandex-team.ru/ECOMSERP-45
        """

        response = self.report.request_bs_pb(
            'place=parallel&text=ABACABCABACAB&g=1.market.10.1.-1...-1.....&rearr-factors=market_enable_reask_gallery_wiz=1;market_enable_reask_gallery_wizard_models=1'
        )

        self.assertFragmentIn(response, {"market_reask_gallery": {"showcase": {"items": ElementCount(1)}}})
        self.assertFragmentIn(
            response, {"showcase": {"items": [{"title": {"text": {"__hl": {"text": "ABACABCABACAB", "raw": True}}}}]}}
        )

    def test_reask_url_with_empty_host(self):
        response = self.report.request_bs_pb(
            'place=parallel&text=panasonic+tumix&g=1.market.10.1.-1...-1.....&rearr-factors=market_enable_reask_gallery_wiz=1;market_enable_reask_gallery_wizard_models=1;market_reask_gallery_set_empty_host_in_url=1'  # noqa
        )
        ids = "301;302"
        wizards = response.extract_wizards()
        for wizard in wizards:
            if 'market_reask_gallery' in wizard:
                url = wizard["market_reask_gallery"]["showcase"]["items"][0]["title"]["url"]
                ids = "302;301" if url.find("reask-modelids=302;301") != -1 else "301;302"
                break
        self.assertFragmentIn(
            response,
            {
                "market_reask_gallery": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "url": "/search?hyperid=302&lr=0&modelid=302&reask-modelids={}&text=panasonic%20tumix%20pt6000&utm_medium=cpc&utm_referrer=wizards&clid=698".format(
                                        ids
                                    ),
                                    "urlTouch": "/search/touch?hyperid=302&lr=0&modelid=302&reask-modelids={}&text=panasonic%20tumix%20pt6000&utm_medium=cpc&utm_referrer=wizards&clid=721".format(
                                        ids
                                    ),
                                    "text": {"__hl": {"text": "panasonic tumix pt6000", "raw": True}},
                                },
                            }
                        ]
                    }
                }
            },
        )


if __name__ == '__main__':
    main()
