#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import HyperCategory, HyperCategoryType, MnPlace, Model, ModelGroup, Offer, Shop
from core.testcase import TestCase, main
from core.matcher import Absent


MAX_VBID = 8400
MAX_POSITIONS = 96  # 2 страницы по 48

OK = 0
UNREACHABLE_BY_BID = 2
UNREACHABLE_BY_PRIORITY = 3


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']

        cls.index.shops += [
            Shop(fesh=10, priority_region=213, regions=[213]),
            Shop(fesh=11, priority_region=2, regions=[225]),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=100, children=[HyperCategory(hid=1, output_type=HyperCategoryType.GURU)]),
            HyperCategory(hid=2, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=3, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=4, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=5, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=6, output_type=HyperCategoryType.GURU),
        ]

        cls.index.offers += [
            Offer(title='offer301', hid=1, fesh=10, hyperid=301, price=3000),
            Offer(title='offer302', hid=1, fesh=10, hyperid=302, price=1000),
            Offer(title='offer303', hid=1, fesh=10, hyperid=303, price=3000),
            Offer(title='offer304', hid=1, fesh=10, hyperid=304, price=1000),
            Offer(title='offer305', hid=1, fesh=10, hyperid=305, price=3000),
            Offer(title='offer306', hid=1, fesh=11, hyperid=306, price=3000),
            Offer(title='offer307', hid=1, fesh=11, hyperid=307, price=4000),
            Offer(title='offer601', hid=6, fesh=10, hyperid=601, price=3000),
            Offer(title='offer602', hid=6, fesh=10, hyperid=602, price=3000),
        ]

        cls.index.models += [
            Model(title='model301_msk', ts=301, hyperid=301, hid=1, vendor_id=1, vbid=4, datasource_id=1),
            Model(title='model302_msk', ts=302, hyperid=302, hid=1, vendor_id=2, vbid=5, datasource_id=2),
            Model(title='model303_msk', ts=303, hyperid=303, hid=1, vendor_id=3, vbid=0, datasource_id=3),
            Model(title='model304_msk', ts=304, hyperid=304, hid=1, vendor_id=1, vbid=4, datasource_id=1),
            Model(title='model305_msk', ts=305, hyperid=305, hid=1, vendor_id=2, vbid=0, datasource_id=2),
            Model(title='model306_spb', ts=306, hyperid=306, hid=1, vendor_id=3, vbid=9, datasource_id=3),
            Model(title='model307_spb', ts=307, hyperid=307, hid=1, vendor_id=1, vbid=3, datasource_id=1),
            Model(title='model308_no_offers', ts=308, hyperid=308, hid=1, vendor_id=2, vbid=0, datasource_id=2),
            Model(title='model309_no_offers', ts=309, hyperid=309, hid=1, vendor_id=3, vbid=10, datasource_id=3),
            Model(title='model310_no_offers', ts=310, hyperid=310, hid=1, vendor_id=4, vbid=1, datasource_id=4),
            Model(title='model601_msk', ts=601, hyperid=601, hid=6, vendor_id=1, vbid=4, datasource_id=1),
            Model(title='model602_msk', ts=602, hyperid=602, hid=6, vendor_id=1, vbid=4, datasource_id=2),
        ]

        # Для наглядности, чтобы только ставки влияли на позицию
        for ts in range(301, 310):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(0.3)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 601).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 602).respond(0.29)

        # Уменьшим ctr_matrix_net модели 303 для тестирования ситуаций, что данной модели не хватит ставки,
        # чтобы улучшить позицию, а другие модели этой же группы будут обгонять 303-ю даже имея нулевую ставку
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 303).respond(0.2)

        # Для тестирования рекомендация для групповых моделей и модификаций
        # Создадим групповые модели, их модификации с различными параметрами и обычные модели
        # Некоторые из моделей будут с доставкой в регион, некоторыен без офферов
        cls.index.model_groups += [
            ModelGroup(title='model_group400', hyperid=400, ts=400, hid=2, vendor_id=1),
            ModelGroup(title='model_group401', hyperid=401, ts=401, hid=2, vendor_id=2),
        ]

        cls.index.models += [
            Model(
                title='model_modification402',
                ts=402,
                hyperid=402,
                hid=2,
                vendor_id=1,
                group_hyperid=400,
                vbid=9,
                datasource_id=1,
            ),
            Model(
                title='model_modification403',
                ts=403,
                hyperid=403,
                hid=2,
                vendor_id=2,
                group_hyperid=401,
                vbid=8,
                datasource_id=2,
            ),
            Model(title='model404', ts=404, hyperid=404, hid=2, vendor_id=3, vbid=5, datasource_id=3),
            Model(title='model405', ts=405, hyperid=405, hid=2, vendor_id=1, vbid=4, datasource_id=1),
            Model(
                title='model_modification406',
                ts=406,
                hyperid=406,
                hid=2,
                vendor_id=1,
                group_hyperid=400,
                vbid=4,
                datasource_id=1,
            ),
            Model(
                title='model_modification407',
                ts=407,
                hyperid=407,
                hid=2,
                vendor_id=3,
                group_hyperid=401,
                vbid=6,
                datasource_id=1,
            ),
            Model(
                title='model_modification408',
                ts=408,
                hyperid=408,
                hid=2,
                vendor_id=4,
                group_hyperid=400,
                vbid=8,
                datasource_id=1,
            ),
        ]

        cls.index.offers += [
            Offer(title='offer402', hid=2, fesh=10, hyperid=402, price=1000),
            Offer(title='offer404', hid=2, fesh=10, hyperid=404, price=1000),
        ]

    def assert_fragment_in_both(self, res1, res2, frag, preserve_order=False, allow_different_len=True):
        self.assertFragmentIn(res1, frag, preserve_order=preserve_order, allow_different_len=allow_different_len)
        self.assertFragmentIn(res2, frag, preserve_order=preserve_order, allow_different_len=allow_different_len)

    def test_prime_output_hid1_msk(self):
        """Проверим, что выдает прайм для запроса c hid=1,чтобы понимать,
        что рекомендатор имитирует работу прайма верно
        Схлопывание должно быть включено.
        Так же включаем эксперимент c использованием старых мин ставок на мете, т.к. рекомендатор работает именно так
        """
        response = self.report.request_json(
            'place=prime&hid=1&rids=213&allow-collapsing=1&rearr-factors=market_use_old_min_bids_on_meta=1;&debug=da'
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 302,
                        "debug": {"properties": {"VBID": "5", "MIN_VBID": "2", "DELIVERY_TYPE": "3"}},
                    },
                    {
                        "entity": "product",
                        "id": 304,
                        "debug": {"properties": {"VBID": "4", "MIN_VBID": "2", "DELIVERY_TYPE": "3"}},
                    },
                    {
                        "entity": "product",
                        "id": 301,
                        "debug": {"properties": {"VBID": "4", "MIN_VBID": "4", "DELIVERY_TYPE": "3"}},
                    },
                    {
                        "entity": "product",
                        "id": 305,
                        "debug": {"properties": {"VBID": "0", "MIN_VBID": "4", "DELIVERY_TYPE": "3"}},
                    },
                    {
                        "entity": "product",
                        "id": 303,
                        "debug": {"properties": {"VBID": "0", "MIN_VBID": "4", "DELIVERY_TYPE": "3"}},
                    },
                    {"entity": "regionalDelimiter"},
                    {
                        "entity": "product",
                        "id": 306,
                        "debug": {"properties": {"VBID": "9", "MIN_VBID": "4", "DELIVERY_TYPE": "2"}},
                    },
                    {
                        "entity": "product",
                        "id": 307,
                        "debug": {"properties": {"VBID": "4", "MIN_VBID": "4", "DELIVERY_TYPE": "2"}},
                    },
                    {
                        "entity": "product",
                        "id": 309,
                        "debug": {"properties": {"VBID": "10", "MIN_VBID": "1", "DELIVERY_TYPE": "1"}},
                    },
                    {
                        "entity": "product",
                        "id": 310,
                        "debug": {"properties": {"VBID": "1", "MIN_VBID": "1", "DELIVERY_TYPE": "1"}},
                    },
                    {
                        "entity": "product",
                        "id": 308,
                        "debug": {"properties": {"VBID": "0", "MIN_VBID": "1", "DELIVERY_TYPE": "1"}},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_recommendator_301_msk(self):
        """Проверяем:
        - модель 301 при текущей ставке 3-я; она может обогнать 304 и 302 при соотв. ставках;
        - у модели 305 ставка в расчете рекомендатора поднята до новой минимальной
        (но, т.к. она меньше поисковой минимальной, все равно не дает прироста cpm, это вообще норм?)
        - 301 обгоняет 305 с минимальной ставкой (новой) (+тут проверяется подпорка в рекомендаторе, когда новая минставка < старой)
        - 301 обгоняет 303 (из той же группы по доставке) даже с нулевой ставкой (т.к. у 303 снижен ctr);
        получив рекомнедацию с 0 ставкой дальше не считаем, т.к. очевидно, что для всех нижних позиций тоже достаточно 0 ставки
        """
        response = self.report.request_json('place=model_bids_recommender&rids=213&hyperid=301&debug=da')
        response_old = self.report.request_json('place=model_bids_recommender&rids=213&hyperid=301&hid=1&debug=da')

        # Проверим так же секцию с выводом информации по модели, для которой считаем рекомендации
        self.assert_fragment_in_both(response, response_old, {"total": 1})
        self.assert_fragment_in_both(response, response_old, {"model": {"id": 301, "titles": {"raw": "model301_msk"}}})

        self.assert_fragment_in_both(
            response,
            response_old,
            {
                "catalog_search": {
                    "estimatePosition": 3,
                    "debugDocument": {"id": 301, "vbid": 4, "minVbid": 3, "searchRankMinVbid": 4},
                }
            },
        )

        self.assert_fragment_in_both(
            response,
            response_old,
            {
                "positions": [
                    {
                        "position": 1,
                        "vbid": 8,
                        "code": OK,
                        "debugDocument": {"id": 302, "vbid": 5, "minVbid": 1, "searchRankMinVbid": 2},
                    },
                    {
                        "position": 2,
                        "vbid": 7,
                        "code": OK,
                        "debugDocument": {"id": 304, "vbid": 4, "minVbid": 1, "searchRankMinVbid": 2},
                    },
                    {
                        "position": 3,
                        "vbid": 3,
                        "code": OK,
                        "debugDocument": {"id": 305, "vbid": 0, "minVbid": 3, "searchRankMinVbid": 4},
                    },
                    {
                        "position": 4,
                        "vbid": 0,
                        "code": OK,
                        "debugDocument": {"id": 303, "vbid": 0, "minVbid": 3, "searchRankMinVbid": 4, "ctr": 0.2},
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_recommendator_303_msk(self):
        """Проверяем:
        - модель 303 имеет сниженный ctr, потому даже при макс. ставке не может обогнать другие модели ее группы (delivery=Priority)
        - модель 303 даже с нулевой ставкой обгоняет модели более низкоприоритетной группы (delivery=Exists). Выводи
        Выводим только первую нудевую рекомнедацию
        """
        response = self.report.request_json('place=model_bids_recommender&rids=213&hyperid=303&debug=da')
        response_old = self.report.request_json('place=model_bids_recommender&rids=213&hyperid=303&hid=1&debug=da')

        self.assert_fragment_in_both(
            response,
            response_old,
            {
                "catalog_search": {
                    "estimatePosition": 5,
                    "debugDocument": {"id": 303, "vbid": 0, "minVbid": 3, "searchRankMinVbid": 4, "ctr": 0.2},
                }
            },
        )
        self.assert_fragment_in_both(
            response,
            response_old,
            {
                "positions": [
                    {
                        "position": 1,
                        "vbid": MAX_VBID,
                        "code": UNREACHABLE_BY_BID,
                        "debugDocument": {"id": 302, "ctr": 0.3},
                    },
                    {
                        "position": 2,
                        "vbid": MAX_VBID,
                        "code": UNREACHABLE_BY_BID,
                        "debugDocument": {"id": 304, "ctr": 0.3},
                    },
                    {
                        "position": 3,
                        "vbid": MAX_VBID,
                        "code": UNREACHABLE_BY_BID,
                        "debugDocument": {"id": 301, "ctr": 0.3},
                    },
                    {
                        "position": 4,
                        "vbid": MAX_VBID,
                        "code": UNREACHABLE_BY_BID,
                        "debugDocument": {"id": 305, "ctr": 0.3},
                    },
                    {"position": 5, "vbid": 0, "code": OK, "debugDocument": {"id": 306, "ctr": 0.3}},
                ]
            },
            allow_different_len=False,
        )

    def test_recommendator_306_msk(self):
        """Проверяем:
        - модель 306 (deliveryPriority=Country) не может за счет ставки опередить модели с deliveryPriority=Priority
        - может обогнать 307 модель той же группы
        - в любом случае (т.е. даже с 0 ставкой) обгоняет модели с deliveryPriority=Exists
        """
        response = self.report.request_json('place=model_bids_recommender&rids=213&hyperid=306&debug=da')
        response_old = self.report.request_json('place=model_bids_recommender&rids=213&hyperid=306&hid=1&debug=da')

        self.assert_fragment_in_both(
            response,
            response_old,
            {
                "catalog_search": {
                    "estimatePosition": 6,
                    "debugDocument": {"id": 306, "vbid": 9, "minVbid": 3, "searchRankMinVbid": 4},
                }
            },
        )
        self.assert_fragment_in_both(
            response,
            response_old,
            {
                "positions": [
                    {"position": 1, "vbid": 0, "code": UNREACHABLE_BY_PRIORITY, "debugDocument": {"id": 302}},
                    {"position": 2, "vbid": 0, "code": UNREACHABLE_BY_PRIORITY, "debugDocument": {"id": 304}},
                    {"position": 3, "vbid": 0, "code": UNREACHABLE_BY_PRIORITY, "debugDocument": {"id": 301}},
                    {"position": 4, "vbid": 0, "code": UNREACHABLE_BY_PRIORITY, "debugDocument": {"id": 305}},
                    {"position": 5, "vbid": 0, "code": UNREACHABLE_BY_PRIORITY, "debugDocument": {"id": 303}},
                    {
                        "position": 6,
                        "vbid": 5,
                        "code": OK,
                        "debugDocument": {"id": 307, "vbid": 4, "minVbid": 4, "searchRankMinVbid": 4},
                    },
                    {"position": 7, "vbid": 0, "code": OK, "debugDocument": {"id": 309}},
                ]
            },
            allow_different_len=False,
        )

    def test_recommendator_308_msk(self):
        """Проверяем:
        - модель 308 (deliveryPriority=Exists) не может за счет ставки опередить модели с deliveryPriority=Priority/Country
        - может обогнать 309, 310 модели той же группы
        - в любом случае (т.е. даже с 0 ставкой) обгоняет модели с deliveryPriority=Exists
        """
        response = self.report.request_json('place=model_bids_recommender&rids=213&hyperid=308&debug=da')
        response_old = self.report.request_json('place=model_bids_recommender&rids=213&hyperid=308&hid=1&debug=da')

        self.assert_fragment_in_both(
            response,
            response_old,
            {
                "catalog_search": {
                    "estimatePosition": 10,
                    "debugDocument": {"id": 308, "vbid": 0, "minVbid": 1, "searchRankMinVbid": 1},
                }
            },
        )
        self.assert_fragment_in_both(
            response,
            response_old,
            {
                "positions": [
                    {"position": 1, "vbid": 0, "code": UNREACHABLE_BY_PRIORITY, "debugDocument": {"id": 302}},
                    {"position": 2, "vbid": 0, "code": UNREACHABLE_BY_PRIORITY, "debugDocument": {"id": 304}},
                    {"position": 3, "vbid": 0, "code": UNREACHABLE_BY_PRIORITY, "debugDocument": {"id": 301}},
                    {"position": 4, "vbid": 0, "code": UNREACHABLE_BY_PRIORITY, "debugDocument": {"id": 305}},
                    {"position": 5, "vbid": 0, "code": UNREACHABLE_BY_PRIORITY, "debugDocument": {"id": 303}},
                    {"position": 6, "vbid": 0, "code": UNREACHABLE_BY_PRIORITY, "debugDocument": {"id": 306}},
                    {"position": 7, "vbid": 0, "code": UNREACHABLE_BY_PRIORITY, "debugDocument": {"id": 307}},
                    {
                        "position": 8,
                        "vbid": 11,
                        "code": OK,
                        "debugDocument": {"id": 309, "vbid": 10, "minVbid": 1, "searchRankMinVbid": 1},
                    },
                    {
                        "position": 9,
                        "vbid": 2,
                        "code": OK,
                        "debugDocument": {"id": 310, "vbid": 1, "minVbid": 1, "searchRankMinVbid": 1},
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_recommendator_parent_categ(self):
        """Проверяем: считаем рекомендации, если задана родительская категория целевой модели"""
        response = self.report.request_json('place=model_bids_recommender&rids=213&hyperid=301&debug=da')
        response_old = self.report.request_json('place=model_bids_recommender&rids=213&hyperid=301&hid=100&debug=da')

        self.assert_fragment_in_both(response, response_old, {"total": 1})
        self.assert_fragment_in_both(response, response_old, {"model": {"id": 301, "titles": {"raw": "model301_msk"}}})

        self.assert_fragment_in_both(
            response,
            response_old,
            {
                "catalog_search": {
                    "estimatePosition": 3,
                    "debugDocument": {"id": 301, "vbid": 4, "minVbid": 3, "searchRankMinVbid": 4},
                }
            },
        )

        self.assert_fragment_in_both(
            response,
            response_old,
            {
                "positions": [
                    {
                        "position": 1,
                        "vbid": 8,
                        "code": OK,
                        "debugDocument": {"id": 302, "vbid": 5, "minVbid": 1, "searchRankMinVbid": 2},
                    },
                    {
                        "position": 2,
                        "vbid": 7,
                        "code": OK,
                        "debugDocument": {"id": 304, "vbid": 4, "minVbid": 1, "searchRankMinVbid": 2},
                    },
                    {
                        "position": 3,
                        "vbid": 3,
                        "code": OK,
                        "debugDocument": {"id": 305, "vbid": 0, "minVbid": 3, "searchRankMinVbid": 4},
                    },
                    {
                        "position": 4,
                        "vbid": 0,
                        "code": OK,
                        "debugDocument": {"id": 303, "vbid": 0, "minVbid": 3, "searchRankMinVbid": 4, "ctr": 0.2},
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_prime_output_hid2_msk(self):
        """Проверим, что выдает прайм для запроса c hid=2,чтобы понимать,
        что рекомендатор имитирует работу прайма верно
        Схлопывание должно быть включено.
        Так же включаем эксперимент c использованием старых мин ставок на мете, т.к. рекомендатор работает именно так
        """
        response = self.report.request_json(
            'place=prime&hid=2&rids=213&allow-collapsing=1&rearr-factors=market_use_old_min_bids_on_meta=1;&debug=da'
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 404,
                        "debug": {"properties": {"VBID": "5", "MIN_VBID": "2", "DELIVERY_TYPE": "3"}},
                    },
                    {
                        "entity": "product",
                        "id": 400,
                        "debug": {"properties": {"VBID": "0", "MIN_VBID": "2", "DELIVERY_TYPE": "3"}},
                    },
                    {"entity": "regionalDelimiter"},
                    {
                        "entity": "product",
                        "id": 405,
                        "debug": {"properties": {"VBID": "4", "MIN_VBID": "1", "DELIVERY_TYPE": "1"}},
                    },
                    {
                        "entity": "product",
                        "id": 401,
                        "debug": {"properties": {"VBID": "0", "MIN_VBID": "1", "DELIVERY_TYPE": "1"}},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_group_models_recommendation(self):
        """Проверяем выдачу рекомендаций для групповой модели 401 (которая без офферов)
        - все модели модификации фильтруются, как и на прайме, т.к. не задан gl-фильтр.
        - 400 имеет приоритет с доставкой за счет оффера своей модели-модификации, но проигрывает 404 по ставке
        - от отфильтрованной 402 в выдаче остается оффер, он выше моделек без доставки
        - 401 не догоняет модели с доставкой, может опередить модель без офферов при соотв. ставке
        """
        response = self.report.request_json('place=model_bids_recommender&rids=213&hyperid=401&debug=da')
        response_old = self.report.request_json('place=model_bids_recommender&rids=213&hyperid=401&hid=2&debug=da')

        self.assert_fragment_in_both(
            response,
            response_old,
            {
                "catalog_search": {
                    "estimatePosition": 4,
                    "debugDocument": {"id": 401, "vbid": 0, "minVbid": 1, "searchRankMinVbid": 1},
                }
            },
        )
        self.assert_fragment_in_both(
            response,
            response_old,
            {
                "positions": [
                    {
                        "position": 1,
                        "vbid": 0,
                        "code": UNREACHABLE_BY_PRIORITY,
                        "debugDocument": {"id": 404, "vbid": 5, "minVbid": 1, "searchRankMinVbid": 2},
                    },
                    {
                        "position": 2,
                        "vbid": 0,
                        "code": UNREACHABLE_BY_PRIORITY,
                        "debugDocument": {"id": 400, "vbid": 0, "minVbid": 1, "searchRankMinVbid": 2},
                    },
                    {
                        "position": 3,
                        "vbid": 5,
                        "code": OK,
                        "debugDocument": {"id": 405, "vbid": 4, "minVbid": 1, "searchRankMinVbid": 1},
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_models_modifications_recommendation1(self):
        """Проверяем: т.к. на прайме отфильтровываются модификации, если в запросе нет gl-фильтра,
        то в данных обстоятельствах не можем дать рекомендации по ставкам на модель-модификацию - возвращаем пустой рез-т
        """
        response = self.report.request_json('place=model_bids_recommender&rids=213&hyperid=402')
        response_old = self.report.request_json('place=model_bids_recommender&rids=213&hyperid=402&hid=2')

        self.assert_fragment_in_both(response, response_old, {"total": 0, "results": []})

    @classmethod
    def prepare_max_docs_minus_1(cls):
        """Для тестирования ситуаций, когда число документов для расчета рекомендаций = макс. чило рекомнедаций на плейсе -1
        Пусть все модели будут одного приоритета - без офферов
        """
        for ts in range(500, 500 + MAX_POSITIONS):  # +1 - НЕ МОЖЕМ ЗНАТЬ ТЕК. ПОЗИЦИЮ
            cls.index.models += [
                Model(ts=ts, hyperid=ts, hid=3, vendor_id=1, vbid=(ts - 500), datasource_id=1),
            ]

    def test_max_docs_minus_1(self):
        """Проверяем, что в ответе MAX_POSITIONS-1 рекомендаций и известна прогнозируемая
        позиция целевой модели (наименьшая из доступных - у целевой модели совсем нет ставки)
        """
        response = self.report.request_json('place=model_bids_recommender&rids=213&hyperid=500')
        response_old = self.report.request_json('place=model_bids_recommender&rids=213&hyperid=500&hid=3')

        self.assert_fragment_in_both(
            response,
            response_old,
            {
                "catalog_search": {
                    "estimatePosition": MAX_POSITIONS,
                }
            },
        )

        for pos in range(1, MAX_POSITIONS - 1):
            self.assert_fragment_in_both(
                response,
                response_old,
                {
                    "position": pos,
                },
            )

    @classmethod
    def prepare_max_docs(cls):
        """Для тестирования ситуаций, когда число документов для расчета рекомендаций = макс. чило рекомнедаций на плейсе
        Пусть все модели будут одного приоритета - без офферов
        """
        for ts in range(700, 850):
            cls.index.models += [
                Model(ts=ts, hyperid=ts, hid=4, vendor_id=1, vbid=(ts - 700), datasource_id=1),
            ]

    def test_max_docs(self):
        """Проверяем, что в ответе MAX_POSITIONS рекомендаций и не известна прогнозируемая
        позиция целевой модели, т.к. она где-то ниже обработанного рекомендатором числа документов
        """
        response = self.report.request_json('place=model_bids_recommender&rids=213&hyperid=700')
        response_old = self.report.request_json('place=model_bids_recommender&rids=213&hyperid=700&hid=1')

        self.assert_fragment_in_both(
            response,
            response_old,
            {
                "catalog_search": {
                    "estimatePosition": Absent(),
                }
            },
        )

        for pos in range(1, MAX_POSITIONS):
            self.assert_fragment_in_both(
                response,
                response_old,
                {
                    "position": pos,
                },
            )

    @classmethod
    def prepare_cpm_rounding(cls):
        """Зададим модели с большими ставками, на этом уровне разница в 1 у.е. незначительна"""
        cls.index.models += [
            Model(ts=900, hyperid=900, hid=5, vendor_id=1, vbid=0, datasource_id=1),
            Model(ts=901, hyperid=901, hid=5, vendor_id=2, vbid=200, datasource_id=2, randx=1),
            Model(ts=902, hyperid=902, hid=5, vendor_id=3, vbid=201, datasource_id=3, randx=2),
        ]

    def test_cpm_rounding(self):
        """Проверим, что CPM при расчете рекомендаций округяется так же, как и при расчете релевантности
        Косвенно это можно проверить тем, что рекомендуемая ставка для достижения обоих моделей одинакова
        (без округления, рекомнедуемая ставка для 2-ой позиции была бы больше, чем для первой)
        """
        response = self.report.request_json('place=model_bids_recommender&rids=213&hyperid=900')
        response_old = self.report.request_json('place=model_bids_recommender&rids=213&hyperid=900&hid=5')

        self.assert_fragment_in_both(
            response,
            response_old,
            {
                "positions": [
                    {"position": 1, "vbid": 202, "code": OK},
                    {"position": 2, "vbid": 202, "code": OK},
                ]
            },
            allow_different_len=False,
        )

    def test_batch_recommendator(self):
        """Проверяем:
        Запрос со многими hyperid без передачи хидов.
        """
        # many hyperIds from different categories
        hyperIds = [301, 302, 303, 304, 305, 306, 307, 308, 400, 401, 402, 403, 404, 405]

        response = self.report.request_json(
            'place=model_bids_recommender&rids=213&hyperid=' + ','.join([str(x) for x in hyperIds])
        )

        expected_response = {
            "search": {
                "total": 12,
                "results": [
                    {
                        "model": {"id": 308, "titles": {"raw": "model308_no_offers"}},
                        "recommendations": {
                            "catalog_search": {
                                "vbid": 0,
                                "minVbid": 1,
                                "estimatePosition": 10,
                                "positions": [
                                    {"position": 1, "vbid": 0, "code": 3},
                                    {"position": 2, "vbid": 0, "code": 3},
                                    {"position": 3, "vbid": 0, "code": 3},
                                    {"position": 4, "vbid": 0, "code": 3},
                                    {"position": 5, "vbid": 0, "code": 3},
                                    {"position": 6, "vbid": 0, "code": 3},
                                    {"position": 7, "vbid": 0, "code": 3},
                                    {"position": 8, "vbid": 11, "code": 0},
                                    {"position": 9, "vbid": 2, "code": 0},
                                ],
                            }
                        },
                    },
                    {
                        "model": {"id": 307, "titles": {"raw": "model307_spb"}},
                        "recommendations": {
                            "catalog_search": {
                                "vbid": 4,
                                "minVbid": 4,
                                "estimatePosition": 7,
                                "positions": [
                                    {"position": 1, "vbid": 0, "code": 3},
                                    {"position": 2, "vbid": 0, "code": 3},
                                    {"position": 3, "vbid": 0, "code": 3},
                                    {"position": 4, "vbid": 0, "code": 3},
                                    {"position": 5, "vbid": 0, "code": 3},
                                    {"position": 6, "vbid": 10, "code": 0},
                                    {"position": 7, "vbid": 0, "code": 0},
                                ],
                            }
                        },
                    },
                    {
                        "model": {"id": 306, "titles": {"raw": "model306_spb"}},
                        "recommendations": {
                            "catalog_search": {
                                "vbid": 9,
                                "minVbid": 3,
                                "estimatePosition": 6,
                                "positions": [
                                    {"position": 1, "vbid": 0, "code": 3},
                                    {"position": 2, "vbid": 0, "code": 3},
                                    {"position": 3, "vbid": 0, "code": 3},
                                    {"position": 4, "vbid": 0, "code": 3},
                                    {"position": 5, "vbid": 0, "code": 3},
                                    {"position": 6, "vbid": 5, "code": 0},
                                    {"position": 7, "vbid": 0, "code": 0},
                                ],
                            }
                        },
                    },
                    {
                        "model": {"id": 305, "titles": {"raw": "model305_msk"}},
                        "recommendations": {
                            "catalog_search": {
                                "vbid": 0,
                                "minVbid": 3,
                                "estimatePosition": 4,
                                "positions": [
                                    {"position": 1, "vbid": 8, "code": 0},
                                    {"position": 2, "vbid": 7, "code": 0},
                                    {"position": 3, "vbid": 5, "code": 0},
                                    {"position": 4, "vbid": 0, "code": 0},
                                ],
                            }
                        },
                    },
                    {
                        "model": {"id": 304, "titles": {"raw": "model304_msk"}},
                        "recommendations": {
                            "catalog_search": {
                                "vbid": 4,
                                "minVbid": 1,
                                "estimatePosition": 2,
                                "positions": [
                                    {"position": 1, "vbid": 6, "code": 0},
                                    {"position": 2, "vbid": 3, "code": 0},
                                    {"position": 3, "vbid": 1, "code": 0},
                                    {"position": 4, "vbid": 0, "code": 0},
                                ],
                            }
                        },
                    },
                    {
                        "model": {"id": 303, "titles": {"raw": "model303_msk"}},
                        "recommendations": {
                            "catalog_search": {
                                "vbid": 0,
                                "minVbid": 3,
                                "estimatePosition": 5,
                                "positions": [
                                    {"position": 1, "vbid": 8400, "code": 2},
                                    {"position": 2, "vbid": 8400, "code": 2},
                                    {"position": 3, "vbid": 8400, "code": 2},
                                    {"position": 4, "vbid": 8400, "code": 2},
                                    {"position": 5, "vbid": 0, "code": 0},
                                ],
                            }
                        },
                    },
                    {
                        "model": {"id": 302, "titles": {"raw": "model302_msk"}},
                        "recommendations": {
                            "catalog_search": {
                                "vbid": 5,
                                "minVbid": 1,
                                "estimatePosition": 1,
                                "positions": [
                                    {"position": 1, "vbid": 5, "code": 0},
                                    {"position": 2, "vbid": 3, "code": 0},
                                    {"position": 3, "vbid": 1, "code": 0},
                                    {"position": 4, "vbid": 0, "code": 0},
                                ],
                            }
                        },
                    },
                    {
                        "model": {"id": 301, "titles": {"raw": "model301_msk"}},
                        "recommendations": {
                            "catalog_search": {
                                "vbid": 4,
                                "minVbid": 3,
                                "estimatePosition": 3,
                                "positions": [
                                    {"position": 1, "vbid": 8, "code": 0},
                                    {"position": 2, "vbid": 7, "code": 0},
                                    {"position": 3, "vbid": 3, "code": 0},
                                    {"position": 4, "vbid": 0, "code": 0},
                                ],
                            }
                        },
                    },
                    {
                        "model": {"id": 405, "titles": {"raw": "model405"}},
                        "recommendations": {
                            "catalog_search": {
                                "vbid": 4,
                                "minVbid": 1,
                                "estimatePosition": 3,
                                "positions": [
                                    {"position": 1, "vbid": 0, "code": 3},
                                    {"position": 2, "vbid": 0, "code": 3},
                                    {"position": 3, "vbid": 1, "code": 0},
                                ],
                            }
                        },
                    },
                    {
                        "model": {"id": 404, "titles": {"raw": "model404"}},
                        "recommendations": {
                            "catalog_search": {
                                "vbid": 5,
                                "minVbid": 1,
                                "estimatePosition": 1,
                                "positions": [
                                    {"position": 1, "vbid": 1, "code": 0},
                                    {"position": 2, "vbid": 0, "code": 0},
                                ],
                            }
                        },
                    },
                    {
                        "model": {"id": 401, "titles": {"raw": "model_group401"}},
                        "recommendations": {
                            "catalog_search": {
                                "vbid": 0,
                                "minVbid": 1,
                                "estimatePosition": 4,
                                "positions": [
                                    {"position": 1, "vbid": 0, "code": 3},
                                    {"position": 2, "vbid": 0, "code": 3},
                                    {"position": 3, "vbid": 5, "code": 0},
                                ],
                            }
                        },
                    },
                    {
                        "model": {"id": 400, "titles": {"raw": "model_group400"}},
                        "recommendations": {
                            "catalog_search": {
                                "vbid": 0,
                                "minVbid": 1,
                                "estimatePosition": 2,
                                "positions": [
                                    {"position": 1, "vbid": 6, "code": 0},
                                    {"position": 2, "vbid": 0, "code": 0},
                                ],
                            }
                        },
                    },
                ],
            }
        }

        self.assertFragmentIn(response, expected_response)

    def test_2_hypers(self):
        """Проверяем:
        Случай когда в категории всего две модели.
        """
        response = self.report.request_json('place=model_bids_recommender&rids=213&hyperid=601,602')
        expected_response = {
            "search": {
                "total": 2,
                "results": [
                    {
                        "model": {"id": 601, "titles": {"raw": "model601_msk"}},
                        "recommendations": {
                            "catalog_search": {
                                "vbid": 4,
                                "minVbid": 3,
                                "estimatePosition": 1,
                                "positions": [{"position": 1, "vbid": 3, "code": 0}],
                            }
                        },
                    },
                    {
                        "model": {"id": 602, "titles": {"raw": "model602_msk"}},
                        "recommendations": {
                            "catalog_search": {
                                "vbid": 4,
                                "minVbid": 3,
                                "estimatePosition": 2,
                                "positions": [{"position": 1, "vbid": 12, "code": 0}],
                            }
                        },
                    },
                ],
            }
        }

        self.assertFragmentIn(response, expected_response)


if __name__ == '__main__':
    main()
