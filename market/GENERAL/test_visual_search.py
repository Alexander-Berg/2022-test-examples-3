#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    GLParam,
    GLType,
    HyperCategory,
    MarketSku,
    Model,
    NavCategory,
    Offer,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.dj import DjModel
from core.testcase import TestCase, main
from core.blender_bundles import create_blender_bundles, get_supported_incuts_cgi
from core.matcher import Contains

PARENT_HID_FOR_VISUAL_SEARCH = 1
CHILD_HID_FOR_VISUAL_SEARCH = 2
PARENT_HID_FOR_ANALOGUE_SEARCH = 3
CHILD_HID_FOR_ANALOGUE_SEARCH = 4

VISUAL_SIMILAR_COUNT_DEFAULT = 2
VISUAL_SIMILAR_COUNT = 4

MODEL_IDS = list(range(1, 30))
MSKU_SHIFT = 1000

MSKU_FOR_VISUAL_SEARCH = 8 + MSKU_SHIFT
RESPONSE_MODEL_IDS_FOR_VISUAL_SEARCH = [5, 6, 7, 8, 9, 10, 11, 12]
GL_201_6_MODELS = [5, 6, 8, 10, 11, 12, 13, 14, 16]
GL_202_8_MODELS = [6, 7, 8, 10, 11, 12, 14, 15, 16]
VISUAL_MODEL_IDS_PASSED_FILTERS = [
    model_id
    for model_id in RESPONSE_MODEL_IDS_FOR_VISUAL_SEARCH
    if model_id in GL_201_6_MODELS and model_id in GL_202_8_MODELS
]
MODEL_ID_SHOWN = 6
NON_SHOWN_MODEL_IDS = [model_id for model_id in RESPONSE_MODEL_IDS_FOR_VISUAL_SEARCH if model_id != MODEL_ID_SHOWN]

MSKU_FOR_NOT_ENOUGH_MODELS = 7 + MSKU_SHIFT

MODEL_ID_FOR_ANALOGUE_SEARCH = 9
RESPONSE_MODEL_IDS_FOR_ANALOGUE_SEARCH = [10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22]
RESPONSE_MODEL_IDS_FOR_ANALOGUE_SEARCH_AS_STR = [str(model_id) for model_id in RESPONSE_MODEL_IDS_FOR_ANALOGUE_SEARCH]
ANALOGUE_MODEL_IDS_PASSED_FILTERS = [
    model_id
    for model_id in RESPONSE_MODEL_IDS_FOR_ANALOGUE_SEARCH
    if model_id in GL_201_6_MODELS and model_id in GL_202_8_MODELS
]
ANALOGUE_MODEL_IDS_NOT_PASSED_BOTH_FILTERS = [
    model_id
    for model_id in RESPONSE_MODEL_IDS_FOR_ANALOGUE_SEARCH
    if model_id not in GL_201_6_MODELS and model_id not in GL_202_8_MODELS
]
MSKU_FOR_ANALOGUE_SEARCH = MODEL_ID_FOR_ANALOGUE_SEARCH + MSKU_SHIFT

MODEL_ID_FOR_VISUAL_AND_ANALOGUE_SEARCH = 10
MSKU_FOR_VISUAL_AND_ANALOGUE_SEARCH = MODEL_ID_FOR_VISUAL_AND_ANALOGUE_SEARCH + MSKU_SHIFT


class BlenderBundleConstPositon:
    BUNDLE_CONST_SEARCH_POSITION = '''
{{
    "incut_places": ["Search"],
    "incut_positions": [{row_position}],
    "incut_viewtypes": ["GridWidedModelContainer"],
    "incut_ids": ["{incut_id}"],
    "result_scores": [
        {{
            "incut_place": "Search",
            "row_position": {row_position},
            "incut_viewtype": "GridWidedModelContainer",
            "incut_id": "{incut_id}",
            "score": 1.0
        }}
    ],
    "calculator_type": "ConstPosition"
}}
'''


class T(TestCase):
    """
    Набор тестов для place=visual_search
    """

    @classmethod
    def prepare(cls):
        """
        Данные для also_viewed, product_analogs
        """
        """чтобы работал place=dj"""
        cls.settings.set_default_reqid = False
        """чтобы в дереве категорий были категории, по которым мы решаем, делать ли визуальный поиск/поиск аналогов"""
        cls.index.hypertree += [
            HyperCategory(
                hid=PARENT_HID_FOR_VISUAL_SEARCH,
                name='визпоиск папа',
                children=[HyperCategory(hid=CHILD_HID_FOR_VISUAL_SEARCH, name='визпоиск доча')],
            )
        ]
        cls.index.hypertree += [
            HyperCategory(
                hid=PARENT_HID_FOR_ANALOGUE_SEARCH,
                name='аналоги мама',
                children=[HyperCategory(hid=CHILD_HID_FOR_ANALOGUE_SEARCH, name='аналоги сына')],
            )
        ]
        cls.index.navtree = [
            NavCategory(
                hid=PARENT_HID_FOR_VISUAL_SEARCH,
                nid=PARENT_HID_FOR_VISUAL_SEARCH,
                is_blue=True,
                name='визпоиск папа',
                children=[
                    NavCategory(
                        hid=CHILD_HID_FOR_VISUAL_SEARCH,
                        nid=CHILD_HID_FOR_VISUAL_SEARCH,
                        is_blue=True,
                        name='визпоиск доча',
                    ),
                ],
            ),
        ]
        """если нет модели, то выдача dj будет пустой"""
        cls.index.models += [
            Model(
                hyperid=model_id,
                hid=CHILD_HID_FOR_VISUAL_SEARCH,
                glparams=[
                    GLParam(param_id=201, value=6 if model_id in GL_201_6_MODELS else 1),
                    GLParam(param_id=202, value=8 if model_id in GL_202_8_MODELS else 1),
                ],
            )
            for model_id in MODEL_IDS
        ]
        cls.index.mskus += [MarketSku(hyperid=model_id, sku=MSKU_SHIFT + model_id) for model_id in MODEL_IDS]
        cls.index.offers += [
            Offer(
                hyperid=model_id,
                glparams=[
                    GLParam(param_id=301, value=6 if model_id in GL_201_6_MODELS else 1),
                    GLParam(param_id=302, value=8 if model_id in GL_202_8_MODELS else 1),
                ],
            )
            for model_id in MODEL_IDS
        ]
        cls.index.gltypes += [
            GLType(param_id=201, hid=CHILD_HID_FOR_VISUAL_SEARCH, gltype=GLType.ENUM, values=list(range(1, 10))),
            GLType(param_id=202, hid=CHILD_HID_FOR_VISUAL_SEARCH, gltype=GLType.ENUM, values=list(range(1, 10))),
            GLType(param_id=301, hid=CHILD_HID_FOR_VISUAL_SEARCH, gltype=GLType.ENUM, values=list(range(1, 10))),
            GLType(param_id=302, hid=CHILD_HID_FOR_VISUAL_SEARCH, gltype=GLType.ENUM, values=list(range(1, 10))),
        ]

    @classmethod
    def prepare_visual_search_dj_place(cls):
        cls.dj.on_request(market_sku=MSKU_FOR_VISUAL_SEARCH).respond(
            [DjModel(id=model_id) for model_id in RESPONSE_MODEL_IDS_FOR_VISUAL_SEARCH]
        )

    def test_visual_search_dj_place_gl_filters(self):
        """
        Проверяем, что place=visual_search работает с фильтрами
        """
        request = (
            'place=visual_search'
            + '&rearr-factors=dj_show_filters=1'
            + '&rearr-factors=market_visual_search_with_gl_filters_enabled=1'
            + '&market-sku=%d' % MSKU_FOR_VISUAL_SEARCH
            + '&hid=%d' % CHILD_HID_FOR_VISUAL_SEARCH
            + '&nid=%d' % CHILD_HID_FOR_VISUAL_SEARCH
            + '&rearr-factors=market_visual_search_departments=%d' % PARENT_HID_FOR_VISUAL_SEARCH
            + '&visual-similar-count=%d' % VISUAL_SIMILAR_COUNT
            + '&glfilter=201:6&glfilter=202:8'
        )
        response = self.report.request_json(request)
        expected_results = [
            {
                'entity': 'product',
                'id': model_id,
                'filters': [
                    {'id': '201', 'values': [{'id': '6'}]},
                    {'id': '202', 'values': [{'id': '8'}]},
                ],
            }
            for model_id in VISUAL_MODEL_IDS_PASSED_FILTERS[:VISUAL_SIMILAR_COUNT]
        ]
        self.assertFragmentIn(response, {'results': expected_results}, preserve_order=True, allow_different_len=False)

    def test_visual_search_dj_place_gl_filters_several_values(self):
        """
        Проверяем, что в place=visual_search несколько значений фильтра работают как логическое И
        """
        base_request = (
            'place=visual_search'
            + '&rearr-factors=dj_show_filters=1'
            + '&rearr-factors=market_visual_search_with_gl_filters_enabled=1'
            + '&market-sku=%d' % MSKU_FOR_VISUAL_SEARCH
            + '&hid=%d' % CHILD_HID_FOR_VISUAL_SEARCH
            + '&nid=%d' % CHILD_HID_FOR_VISUAL_SEARCH
            + '&rearr-factors=market_visual_search_departments=%d' % PARENT_HID_FOR_VISUAL_SEARCH
            + '&visual-similar-count=%d' % VISUAL_SIMILAR_COUNT_DEFAULT
        )
        for (request, models, value) in [
            [
                base_request + '&glfilter=201:1,2,3,7',
                [model_id for model_id in RESPONSE_MODEL_IDS_FOR_VISUAL_SEARCH if model_id not in GL_201_6_MODELS],
                '1',
            ],
            [
                base_request + '&glfilter=201:6,2,3',
                [model_id for model_id in GL_201_6_MODELS],
                '6',
            ],
        ]:
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'product',
                            'id': model_id,
                            'filters': [
                                {'id': '201', 'values': [{'id': value}]},
                            ],
                        }
                        for model_id in models[:VISUAL_SIMILAR_COUNT_DEFAULT]
                    ]
                },
                preserve_order=True,
            )

    def test_visual_search_dj_place(self):
        """
        Проверяем, что place=visual_search работает корректно: выдаёт ровно VISUAL_SIMILAR_COUNT моделей в ответ
        """
        base_request = (
            'place=visual_search'
            + '&market-sku=%d' % MSKU_FOR_VISUAL_SEARCH
            + '&hid=%d' % CHILD_HID_FOR_VISUAL_SEARCH
            + '&rearr-factors=market_visual_search_departments=%d' % PARENT_HID_FOR_VISUAL_SEARCH
        )
        for request in [
            base_request + '&rearr-factors=market_visual_similar_count=%d' % VISUAL_SIMILAR_COUNT,
            base_request + '&visual-similar-count=%d' % VISUAL_SIMILAR_COUNT,
        ]:
            response = self.report.request_json(request)
            expected_results = [
                {'entity': 'product', 'id': model_id}
                for model_id in RESPONSE_MODEL_IDS_FOR_VISUAL_SEARCH[:VISUAL_SIMILAR_COUNT]
            ]
            self.assertFragmentIn(
                response, {'results': expected_results}, preserve_order=True, allow_different_len=False
            )
            self.assertFragmentIn(response, {'visualSearchResultType': 'visual_similar'})

    @classmethod
    def prepare_visual_search_dj_place_with_filtration(cls):
        cls.dj.on_request(market_sku=MSKU_FOR_VISUAL_SEARCH, hyperid=MODEL_ID_SHOWN).respond(
            [DjModel(id=model_id) for model_id in NON_SHOWN_MODEL_IDS]
        )

    def test_visual_search_dj_place_with_filtration(self):
        """
        Проверяем, что place=visual_search не выдаёт в ответе модель, указанную в hyperid
        """
        request = (
            'place=visual_search'
            + '&rearr-factors=market_visual_similar_count=%d' % VISUAL_SIMILAR_COUNT
            + '&market-sku=%d' % MSKU_FOR_VISUAL_SEARCH
            + '&hid=%d' % CHILD_HID_FOR_VISUAL_SEARCH
            + '&rearr-factors=market_visual_search_departments=%d' % PARENT_HID_FOR_VISUAL_SEARCH
            + '&hyperid=%d' % MODEL_ID_SHOWN
        )
        response = self.report.request_json(request)
        expected_results = [
            {'entity': 'product', 'id': model_id} for model_id in NON_SHOWN_MODEL_IDS[:VISUAL_SIMILAR_COUNT]
        ]
        self.assertFragmentIn(response, {'results': expected_results}, preserve_order=True, allow_different_len=False)
        self.assertFragmentIn(response, {'visualSearchResultType': 'visual_similar'})

    @classmethod
    def prepare_visual_search_dj_place_not_enough_models(cls):
        """
        Пришлось делать отдельную market_sku, потому что при передаче
        rearr-factors=market_visual_similar_count=len(MODEL_IDS) в метод on_request
        все нижние подчеркивания заменяются на дефисы и меняется значение rearr-factor-а
        """
        cls.dj.on_request(market_sku=MSKU_FOR_NOT_ENOUGH_MODELS).respond([])

    def test_visual_search_dj_place_not_enough_models(self):
        """
        Проверяем, что place=visual_search выдает пустой ответ, если VISUAL_SIMILAR_COUNT слишком большой
        """
        request = (
            'place=visual_search'
            + '&rearr-factors=market_visual_similar_count=%d' % len(MODEL_IDS)
            + '&market-sku=%d' % MSKU_FOR_NOT_ENOUGH_MODELS
            + '&hid=%d' % CHILD_HID_FOR_VISUAL_SEARCH
            + '&rearr-factors=market_visual_search_departments=%d' % PARENT_HID_FOR_VISUAL_SEARCH
            + '&rearr-factors=market_visual_search_analogue_replacement_enabled=0'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {'search': {'total': 0, 'results': []}}, preserve_order=False)

    def test_pass_visual_similar_count_param(self):
        """
        Проверяем, что в DJ пробрасывается visual-similar-count
        """
        base_request = (
            'place=visual_search&debug=1'
            + '&market-sku=%d' % MSKU_FOR_VISUAL_SEARCH
            + '&hid=%d' % CHILD_HID_FOR_VISUAL_SEARCH
            + '&rearr-factors=market_visual_search_departments=%d' % PARENT_HID_FOR_VISUAL_SEARCH
        )
        for request in [
            base_request + '&rearr-factors=market_visual_similar_count=%d' % VISUAL_SIMILAR_COUNT,
            base_request + '&visual-similar-count=%d' % VISUAL_SIMILAR_COUNT,
        ]:
            response = self.report.request_json(request)
            self.error_log.not_expect(code=3787)
            self.assertFragmentIn(
                response,
                {"logicTrace": [Contains('Set param for DJ: visual-similar-count=' + str(VISUAL_SIMILAR_COUNT))]},
            )

    @classmethod
    def prepare_blender_bundles_config(cls):
        cls.settings.formulas_path = create_blender_bundles(
            cls.meta_paths.testroot,
            """
            {
                "INCLID_VISUALLY_SIMILAR" : {
                    "1": { "bundle_name": "bundle_visually_similar_page_2.json" }
                }
            }
            """,
            {
                "bundle_visually_similar_page_2.json": BlenderBundleConstPositon.BUNDLE_CONST_SEARCH_POSITION.format(
                    row_position=5, incut_id="visually_similar_page_2"
                ),
            },
        )

    def test_visual_search_blender_place(self):
        """
        Проверяем, врезка в place=blender работает корректно: выдаёт ровно market_blender_visually_similar_numdoc моделей в ответ
        """
        request = (
            'place=prime'
            + '&blender=1&debug=da'
            + '&columns-in-grid=2'
            + '&viewtype=grid'
            + '&client=frontend'
            + '&platform=touch'
            + '&supported-incuts='
            + get_supported_incuts_cgi({"1": ["19"]})
            + '&page=2'
            + '&numdoc=8'
            + '&rearr-factors=market_visual_search_departments=%d' % PARENT_HID_FOR_VISUAL_SEARCH
            + '&vs-liked-msku-ids=%d:%d' % (CHILD_HID_FOR_VISUAL_SEARCH, MSKU_FOR_VISUAL_SEARCH)
            + '&hid=%d' % CHILD_HID_FOR_VISUAL_SEARCH
        )
        response = self.report.request_json(
            request + '&rearr-factors=market_blender_visually_similar_numdoc=%d' % VISUAL_SIMILAR_COUNT
        )
        expected_results = [
            {'entity': 'product', 'id': model_id}
            for model_id in RESPONSE_MODEL_IDS_FOR_VISUAL_SEARCH[:VISUAL_SIMILAR_COUNT]
        ]
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'items': expected_results,
                            'typeId': 19,
                            'position': 1,
                            'inClid': 25,
                            'incutId': 'visually_similar_page_2',
                        }
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        """
        Проверяем, что если market_blender_visually_similar_numdoc встречается с market_visual_similar_count,
        он всегда имеет над ним приоритет
        """

        response = self.report.request_json(
            request
            + '&rearr-factors=market_visual_similar_count=2;'
            + 'market_blender_visually_similar_numdoc=%d' % VISUAL_SIMILAR_COUNT
        )
        expected_results = [
            {'entity': 'product', 'id': model_id}
            for model_id in RESPONSE_MODEL_IDS_FOR_VISUAL_SEARCH[:VISUAL_SIMILAR_COUNT]
        ]
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'items': expected_results,
                            'typeId': 19,
                            'position': 1,
                            'inClid': 25,
                            'incutId': 'visually_similar_page_2',
                        }
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            request
            + '&rearr-factors=market_blender_visually_similar_numdoc=%d;' % VISUAL_SIMILAR_COUNT
            + 'market_visual_similar_count=2'
        )
        expected_results = [
            {'entity': 'product', 'id': model_id}
            for model_id in RESPONSE_MODEL_IDS_FOR_VISUAL_SEARCH[:VISUAL_SIMILAR_COUNT]
        ]
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'items': expected_results,
                            'typeId': 19,
                            'position': 1,
                            'inClid': 25,
                            'incutId': 'visually_similar_page_2',
                        }
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            request
            + '&rearr-factors=market_blender_visually_similar_numdoc=%d' % VISUAL_SIMILAR_COUNT
            + '&rearr-factors=market_visual_similar_count=2'
        )
        expected_results = [
            {'entity': 'product', 'id': model_id}
            for model_id in RESPONSE_MODEL_IDS_FOR_VISUAL_SEARCH[:VISUAL_SIMILAR_COUNT]
        ]
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'items': expected_results,
                            'typeId': 19,
                            'position': 1,
                            'inClid': 25,
                            'incutId': 'visually_similar_page_2',
                        }
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_analogue_search(cls):
        """also_viewed"""
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_PRODUCTS,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(params={'version': 'COVIEWS1'}, splits=[{}]),
                ],
            ),
        ]
        cls.recommender.on_request_accessory_models(
            model_id=MODEL_ID_FOR_ANALOGUE_SEARCH, item_count=1000, version='COVIEWS1'
        ).respond({'models': RESPONSE_MODEL_IDS_FOR_ANALOGUE_SEARCH_AS_STR})

    def test_analogue_search(self):
        """
        Проверяем, что из остальных категорий вызывается place=also_viewed и выдает корректный ответ
        """
        base_request = (
            'place=visual_search'
            + '&modelid=%d' % MODEL_ID_FOR_ANALOGUE_SEARCH
            + '&hid=%d' % CHILD_HID_FOR_ANALOGUE_SEARCH
        )
        for [request, visual_similar_count] in [
            [
                base_request + '&rearr-factors=market_visual_similar_count=%d' % VISUAL_SIMILAR_COUNT,
                VISUAL_SIMILAR_COUNT,
            ],
            [base_request + '&visual-similar-count=%d' % VISUAL_SIMILAR_COUNT, VISUAL_SIMILAR_COUNT],
            [base_request, VISUAL_SIMILAR_COUNT_DEFAULT],
        ]:
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': visual_similar_count,
                        'results': [
                            {'entity': 'product', 'id': model_id, 'meta': {'place': 'also_viewed'}}
                            for model_id in RESPONSE_MODEL_IDS_FOR_ANALOGUE_SEARCH[:visual_similar_count]
                        ],
                        'visualSearchResultType': 'analogue',
                    }
                },
                preserve_order=False,
            )

    def test_analogue_search_filtration(self):
        """
        Проверяем, что при походе в place=also_viewed в ответе не выдаётся модель, указанная в hyperid
        """
        request = (
            'place=visual_search'
            + '&modelid=%d' % MODEL_ID_FOR_ANALOGUE_SEARCH
            + '&hid=%d' % CHILD_HID_FOR_ANALOGUE_SEARCH
            + '&hyperid=11'
        )
        response = self.report.request_json(request)
        self.assertFragmentNotIn(response, {'entity': 'product', 'id': 11})
        self.assertFragmentIn(response, {'search': {'total': 2}})
        self.assertFragmentIn(response, {'visualSearchResultType': 'analogue'})

    @classmethod
    def prepare_analogue_search_replacement_enabled(cls):
        """
        Для MSKU для поиска аналогов в DJ нет рекомендаций и возвращается пустой ответ
        """
        cls.dj.on_request(market_sku=MSKU_FOR_ANALOGUE_SEARCH).respond([])

    def test_analogue_search_replacement_enabled(self):
        """
        Проверяем, что при включенном флаге и пустом ответе визуально похожих вызывается place=also_viewed и выдает корректный ответ
        """
        request = (
            'place=visual_search'
            '&rearr-factors=market_visual_search_analogue_replacement_enabled=1'
            + '&rearr-factors=market_visual_similar_count=%d' % VISUAL_SIMILAR_COUNT
            + '&modelid=%d' % MODEL_ID_FOR_ANALOGUE_SEARCH
            + '&hid=%d' % CHILD_HID_FOR_VISUAL_SEARCH
            + '&market-sku=%d' % MSKU_FOR_ANALOGUE_SEARCH
            + '&rearr-factors=market_visual_search_departments=%d' % PARENT_HID_FOR_VISUAL_SEARCH
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': VISUAL_SIMILAR_COUNT,
                    'results': [
                        {'entity': 'product', 'id': model_id, 'meta': {'place': 'also_viewed'}}
                        for model_id in RESPONSE_MODEL_IDS_FOR_ANALOGUE_SEARCH[:VISUAL_SIMILAR_COUNT]
                    ],
                    'visualSearchResultType': 'analogue',
                }
            },
            preserve_order=False,
        )

    @classmethod
    def prepare_analogue_search_replacement_disabled(cls):
        """
        Для MSKU для поиска аналогов в DJ нет рекомендаций и возвращается пустой ответ
        """
        cls.dj.on_request(market_sku=MSKU_FOR_ANALOGUE_SEARCH).respond([])

    def test_analogue_search_replacement_disabled(self):
        """
        Проверяем, что при ВЫКЛЮЧЕННОМ флаге и пустом ответе визуально ответ остаётся пустым
        """
        request = (
            'place=visual_search'
            '&rearr-factors=market_visual_search_analogue_replacement_enabled=0'
            + '&modelid=%d' % MODEL_ID_FOR_ANALOGUE_SEARCH
            + '&hid=%d' % CHILD_HID_FOR_VISUAL_SEARCH
            + '&market-sku=%d' % MSKU_FOR_ANALOGUE_SEARCH
            + '&rearr-factors=market_visual_search_departments=%d' % PARENT_HID_FOR_VISUAL_SEARCH
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {'search': {'total': 0, 'results': []}}, preserve_order=False)

    def test_analogue_search_gl_filters(self):
        """
        Проверяем, что place=visual_search при замене визуально похожих на аналоги применяет фильтры
        """
        request = (
            'place=visual_search'
            + '&rearr-factors=dj_show_filters=1'
            + '&rearr-factors=market_visual_search_with_gl_filters_enabled=1'
            + '&rearr-factors=market_visual_search_analogue_replacement_enabled=1'
            + '&modelid=%d' % MODEL_ID_FOR_ANALOGUE_SEARCH
            + '&market-sku=%d' % MSKU_FOR_ANALOGUE_SEARCH
            + '&hid=%d' % CHILD_HID_FOR_VISUAL_SEARCH
            + '&nid=%d' % CHILD_HID_FOR_VISUAL_SEARCH
            + '&rearr-factors=market_visual_search_departments=%d' % PARENT_HID_FOR_VISUAL_SEARCH
            + '&visual-similar-count=%d' % VISUAL_SIMILAR_COUNT
            + '&glfilter=201:6&glfilter=202:8'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': VISUAL_SIMILAR_COUNT,
                    'results': [
                        {
                            'entity': 'product',
                            'id': model_id,
                            'filters': [
                                {'id': '201', 'values': [{'id': '6'}]},
                                {'id': '202', 'values': [{'id': '8'}]},
                            ],
                        }
                        for model_id in ANALOGUE_MODEL_IDS_PASSED_FILTERS[:VISUAL_SIMILAR_COUNT]
                    ],
                    'visualSearchResultType': 'analogue',
                }
            },
            preserve_order=False,
        )

    @classmethod
    def prepare_visual_and_analogue_search_gl_filters(cls):
        """
        DJ отдаёт в ответе много моделей, как при обычном поиске, но после применения фильтров остаётся всего 1 модель
        """
        cls.dj.on_request(market_sku=MSKU_FOR_VISUAL_AND_ANALOGUE_SEARCH).respond(
            [DjModel(id=model_id) for model_id in RESPONSE_MODEL_IDS_FOR_VISUAL_SEARCH]
        )
        cls.recommender.on_request_accessory_models(
            model_id=MODEL_ID_FOR_VISUAL_AND_ANALOGUE_SEARCH, item_count=1000, version='COVIEWS1'
        ).respond({'models': RESPONSE_MODEL_IDS_FOR_ANALOGUE_SEARCH_AS_STR})

    def test_visual_and_analogue_search_gl_filters(self):
        """
        Проверяем, что place=visual_search переходит на аналоги, если с DJ пришло слишком мало моделей, подходящих под фильтры
        (в начале файлы задаётся, какие фильтры у моделей и среди RESPONSE_MODEL_IDS_FOR_VISUAL_SEARCH лишь у одной оба фильтра равны 1)
        """
        request = (
            'place=visual_search'
            + '&rearr-factors=dj_show_filters=1'
            + '&rearr-factors=market_visual_search_with_gl_filters_enabled=1'
            + '&rearr-factors=market_visual_search_analogue_replacement_enabled=1'
            + '&modelid=%d' % MODEL_ID_FOR_VISUAL_AND_ANALOGUE_SEARCH
            + '&market-sku=%d' % MSKU_FOR_VISUAL_AND_ANALOGUE_SEARCH
            + '&hid=%d' % CHILD_HID_FOR_VISUAL_SEARCH
            + '&nid=%d' % CHILD_HID_FOR_VISUAL_SEARCH
            + '&rearr-factors=market_visual_search_departments=%d' % PARENT_HID_FOR_VISUAL_SEARCH
            + '&visual-similar-count=%d' % VISUAL_SIMILAR_COUNT
            + '&glfilter=201:1&glfilter=202:1'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': VISUAL_SIMILAR_COUNT,
                    'results': [
                        {
                            'entity': 'product',
                            'id': model_id,
                            'filters': [
                                {'id': '201', 'values': [{'id': '1'}]},
                                {'id': '202', 'values': [{'id': '1'}]},
                            ],
                        }
                        for model_id in ANALOGUE_MODEL_IDS_NOT_PASSED_BOTH_FILTERS[:VISUAL_SIMILAR_COUNT]
                    ],
                    'visualSearchResultType': 'analogue',
                }
            },
            preserve_order=False,
        )


if __name__ == '__main__':
    main()
