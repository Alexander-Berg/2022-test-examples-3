#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    GLParam,
    GLType,
    HyperCategory,
    MarketSku,
    Model,
    NavCategory,
    NavRecipe,
    NavRecipeFilter,
    NavTree,
    NidsRedirector,
    Offer,
    RedirectorRecord,
    Shop,
    Vat,
)
from core.testcase import TestCase, main
from core.matcher import Absent


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        # Numeration rules:
        # - hid = {100, 199}
        # - nid = {300, 399}
        # - hyper = {500, 599}
        # - glparam = {10, 99}
        # - glvalue = {1, 9}
        # - vclusterid = {600, 699}

        # anomaly handling:
        cls.index.hypertree += [HyperCategory(hid=100), HyperCategory(hid=101), HyperCategory(hid=102)]

        cls.index.navtree += [
            NavCategory(
                nid=300,
                children=[
                    NavCategory(
                        nid=301,
                        hid=100,
                        children=[NavCategory(nid=302, hid=101, primary=True), NavCategory(nid=303, hid=102)],
                    )
                ],
            )
        ]

        cls.index.offers += [
            Offer(title='sony', hid=101),
            Offer(title='sony two', hid=102),
            Offer(title='sony three', hid=102),
        ]

        # best nid selection & sorting
        cls.index.navtree += [
            NavCategory(
                nid=312,
                children=[NavCategory(nid=313, hid=108, primary=True), NavCategory(nid=314, hid=108, primary=False)],
            )
        ]

        cls.index.offers += [
            Offer(title='iphone10', hid=108, glparams=[GLParam(param_id=13, value=1)]),
            Offer(title='iphone11', hid=108),
        ]

        # other
        cls.index.navtree += [
            NavCategory(
                nid=304,
                children=[NavCategory(nid=305, hid=103, name='AAAA'), NavCategory(nid=306, hid=104, name='BBBB')],
            ),
            NavCategory(
                nid=307,
                children=[
                    NavCategory(nid=308, hid=105),
                ],
            ),
            NavCategory(nid=310, hid=101, primary=False),
            NavCategory(nid=311, hid=107),
        ]

        cls.index.offers += [
            Offer(title='red shoes', vclusterid=1000000504, hid=103),
            Offer(title='green shoes', vclusterid=1000000505, hid=104),
            Offer(title='yellow shoes', vclusterid=1000000506, hid=104),
            Offer(title='iphone7', hid=105),
            Offer(title='iphone8', hid=106, glparams=[GLParam(param_id=10, value=2)]),
            Offer(title='iphone9', hid=106, glparams=[GLParam(param_id=10, value=3)]),
            Offer(hid=107, vclusterid=1000000500, glparams=[GLParam(param_id=11, value=1, cluster_filter=True)]),
            Offer(hid=107, vclusterid=1000000501, glparams=[GLParam(param_id=11, value=2, cluster_filter=True)]),
            Offer(hid=107, vclusterid=1000000502, glparams=[GLParam(param_id=12, value=1, cluster_filter=True)]),
            Offer(hid=107, vclusterid=1000000503, glparams=[GLParam(param_id=12, value=1, cluster_filter=True)]),
        ]

        # navigation tree inference
        cls.index.navtree += [NavCategory(nid=316, hid=109)]

        # На самом деле всегда было только одно навигационное дерево в лесу.
        # Данный тест оставлен для совместимости на момент перехода на новый лес
        # Когда новый лес будет окончательно введен, нужно привести тесты в норму. MARKETOUT-20919
        cls.index.navforest += [NavTree(tree_id=2, children=[NavCategory(nid=317, hid=109)])]

        cls.index.offers += [Offer(title='multitree', hid=109)]

        # experiment impact
        cls.index.hypertree += [HyperCategory(hid=110, children=[HyperCategory(hid=111), HyperCategory(hid=112)])]

        cls.index.navtree += [
            NavCategory(
                nid=318,
                children=[NavCategory(nid=319, hid=110), NavCategory(nid=320, hid=111), NavCategory(nid=321, hid=112)],
            )
        ]

        cls.index.offers += [Offer(title='kiwi', hid=110), Offer(title='kiwi', hid=111), Offer(title='kiwi', hid=112)]

        # visual search mixed categories
        cls.index.navtree += [NavCategory(nid=322, hid=113), NavCategory(nid=323, hid=114)]

        cls.index.offers += [Offer(title='visual', hid=114, vclusterid=1000000600), Offer(title='non visual', hid=113)]

    def test_hierarchy_anomaly(self):
        response = self.report.request_json('place=prime&nid=300')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "sony"}},
                    {"entity": "offer", "titles": {"raw": "sony two"}},
                    {"entity": "offer", "titles": {"raw": "sony three"}},
                ]
            },
        )

    def test_output_filter_and_bubles_in_mainsearch(self):
        response = self.report.request_json('place=prime&text=sony&rearr-factors=turn_off_nid_intents_on_serp=0')
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {"name": "NID-300", "uniqName": "UNIQ-NID-300", "hid": 0, "nid": 300},
                        "intents": [
                            {
                                "category": {"name": "NID-301", "uniqName": "UNIQ-NID-301", "hid": 100, "nid": 301},
                                "intents": [
                                    {
                                        "category": {
                                            "name": "NID-303",
                                            "uniqName": "UNIQ-NID-303",
                                            "hid": 102,
                                            "nid": 303,
                                        },
                                        "intents": Absent(),
                                    },
                                    {
                                        "category": {
                                            "name": "NID-302",
                                            "uniqName": "UNIQ-NID-302",
                                            "hid": 101,
                                            "nid": 302,
                                        },
                                        "intents": Absent(),
                                    },
                                ],
                            }
                        ],
                    }
                ]
            },
            preserve_order=True,
        )

    def test_alphabetical_order_in_output_filter_in_prime(self):
        response = self.report.request_json('place=prime&text=shoes&rearr-factors=turn_off_nid_intents_on_serp=0')
        self.assertFragmentIn(
            response,
            {
                'intents': [
                    {
                        'ownCount': 2,
                        'category': {'name': "BBBB", 'uniqName': "UNIQ-NID-306", 'hid': 104, 'nid': 306},
                    },
                    {
                        'ownCount': 1,
                        'category': {'name': "AAAA", 'uniqName': "UNIQ-NID-305", 'hid': 103, 'nid': 305},
                    },
                ]
            },
            preserve_order=True,
        )

    def test_output_filters_with_filtering_by_nid(self):
        response = self.report.request_json('place=prime&nid=302')
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {"name": "NID-300", "uniqName": "UNIQ-NID-300", "hid": 0, "nid": 300},
                        "intents": [
                            {
                                "category": {"name": "NID-301", "uniqName": "UNIQ-NID-301", "hid": 100, "nid": 301},
                                "intents": [
                                    {
                                        "category": {
                                            "name": "NID-302",
                                            "uniqName": "UNIQ-NID-302",
                                            "hid": 101,
                                            "nid": 302,
                                        }
                                    }
                                ],
                            }
                        ],
                    }
                ]
            },
            preserve_order=True,
        )

    def test_search_by_virtual_nid(self):
        response = self.report.request_json('place=prime&nid=307')
        self.assertFragmentIn(response, {"entity": "offer", "titles": {"raw": "iphone7"}})

    def test_search_by_primary_nid(self):
        response = self.report.request_json('place=prime&nid=302&debug=1')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "categories": [{"entity": "category", "id": 101}],
                "debug": {"properties": {"NID": "302"}},
            },
        )

    def test_search_by_secondary_nid(self):
        response = self.report.request_json('place=prime&nid=310&debug=1')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "categories": [{"entity": "category", "id": 101}],
                "debug": {"properties": {"NID": "310"}},
            },
        )

    def test_output_filter_with_filtering_by_nid(self):
        response = self.report.request_json('place=prime&nid=311&glfilter=11:2')
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {
                        'id': "11",
                        'name': "GLPARAM-11",
                        'position': 1,
                        'noffers': 3,  # 2 really
                        'values': [
                            {'initialFound': 1, 'found': 1, 'value': "VALUE-1", 'id': "1"},
                            {
                                # 1 offer really was counted twice
                                'initialFound': 2,
                                'checked': True,
                                'found': 2,
                                'value': "VALUE-2",
                                'id': "2",
                            },
                        ],
                    },
                    {
                        'id': "12",
                        'type': "enum",
                        'name': "GLPARAM-12",
                        'position': 1,
                        'values': [{'initialFound': 2, 'found': 0, 'value': "VALUE-1", 'id': "1"}],
                    },
                ]
            },
            preserve_order=True,
        )

    def test_best_nid_selection_without_nid_filter(self):
        response = self.report.request_json('place=prime&text=iphone10&debug=1')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "categories": [{"entity": "category", "id": 108}],
                "debug": {"properties": {"NID": "313"}},
            },
        )

    def test_best_nid_selection_with_nid_filter(self):
        response = self.report.request_json('place=prime&text=iphone10&nid=312&debug=1')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "categories": [{"entity": "category", "id": 108}],
                "debug": {"properties": {"NID": "313"}},
            },
        )

    def test_primary_nid_selection(self):
        response = self.report.request_json('place=prime&nid=312&text=iphone11&debug=1')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "categories": [{"entity": "category", "id": 108}],
                "debug": {"properties": {"NID": "313"}},
            },
        )

    def test_children_from_primary_nid(self):
        response = self.report.request_json('place=prime&nid=301')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "sony"}},
                    {"entity": "offer", "titles": {"raw": "sony two"}},
                    {"entity": "offer", "titles": {"raw": "sony three"}},
                ]
            },
        )

    def test_experiment_impact(self):
        response = self.report.request_json('place=prime&text=kiwi&rearr-factors=turn_off_nid_intents_on_serp=0')
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {"hid": 0, "nid": 318},
                        "intents": [
                            {
                                "category": {"hid": 110, "nid": 319},
                            },
                            {
                                "category": {"hid": 111, "nid": 320},
                            },
                            {
                                "category": {"hid": 112, "nid": 321},
                            },
                        ],
                    }
                ]
            },
            preserve_order=True,
        )

        response = self.report.request_json('place=prime&text=kiwi&nid=318')
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {"hid": 0, "nid": 318},
                        "intents": [
                            {
                                "category": {"hid": 110, "nid": 319},
                            },
                            {
                                "category": {"hid": 111, "nid": 320},
                            },
                            {
                                "category": {"hid": 112, "nid": 321},
                            },
                        ],
                    }
                ]
            },
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=prime&text=kiwi&hid=110&rearr-factors=turn_off_nid_intents_on_serp=0'
        )
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {"hid": 110, "nid": 319},
                        "intents": [
                            {
                                "category": {"hid": 111, "nid": 320},
                            },
                            {
                                "category": {"hid": 112, "nid": 321},
                            },
                        ],
                    }
                ]
            },
            preserve_order=True,
        )

        # Тест на хидовые интенты
        response = self.report.request_json('place=prime&text=kiwi&rearr-factors=market_return_nids_in_intents=0')
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {"hid": 110, "nid": 319},
                        "intents": [
                            {
                                "category": {"hid": 111, "nid": 320},
                            },
                            {
                                "category": {"hid": 112, "nid": 321},
                            },
                        ],
                    }
                ]
            },
            preserve_order=True,
        )

        response = self.report.request_json('place=prime&text=kiwi&hid=110')
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {"hid": 110, "nid": 319},
                        "intents": [
                            {
                                "category": {"hid": 111, "nid": 320},
                            },
                            {
                                "category": {"hid": 112, "nid": 321},
                            },
                        ],
                    }
                ]
            },
            preserve_order=True,
        )

        # Тест на хидовые интенты
        response = self.report.request_json(
            'place=prime&text=kiwi&nid=318&rearr-factors=market_return_nids_in_intents=0'
        )
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {"hid": 110, "nid": 319},
                        "intents": [
                            {
                                "category": {"hid": 111, "nid": 320},
                            },
                            {
                                "category": {"hid": 112, "nid": 321},
                            },
                        ],
                    }
                ]
            },
            preserve_order=True,
        )

    def test_mixed_category_filter(self):
        # Ожидаются хидовые интенты
        response = self.report.request_json('place=prime&text=visual&rearr-factors=market_return_nids_in_intents=0')
        self.assertFragmentIn(
            response,
            {
                'intents': [
                    {'category': {'name': "HID-113", 'hid': 113, 'nid': 322}},
                    {'category': {'name': "HID-114", 'hid': 114, 'nid': 323}},
                ]
            },
            preserve_order=True,
        )

        response = self.report.request_json('place=prime&text=visual&rearr-factors=turn_off_nid_intents_on_serp=0')
        self.assertFragmentIn(
            response,
            {
                'intents': [
                    {'category': {'name': "NID-322", 'hid': 113, 'nid': 322}},
                    {'category': {'name': "NID-323", 'hid': 114, 'nid': 323}},
                ]
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_multi_navigation_trees(cls):
        # Старое бело/синее дерево
        cls.index.navtree += [NavCategory(nid=10001, hid=10001)]

        # Новое синее дерево
        cls.index.navtree_blue += [NavCategory(nid=30001, hid=10001)]

        cls.index.models += [
            Model(hyperid=10001, hid=10001),
        ]

        cls.index.shops += [
            Shop(fesh=1111111, datafeed_id=1111111, priority_region=213, blue="REAL", cpa=Shop.CPA_REAL),
            Shop(
                fesh=2222222, priority_region=213, fulfillment_virtual=True, virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title="blue offer for multi trees",
                hyperid=10001,
                sku="1",
                blue_offers=[
                    BlueOffer(
                        price=10,
                        vat=Vat.VAT_10,
                        feedid=1111111,
                    ),
                ],
            ),
        ]

    def test_multi_navigation_trees(self):
        """
        Что проверяем: работу навигационных деревьев, разделенных по цвету
        """
        request = 'place=prime&hyperid=10001&rids=213'

        # Проверяем, что будет выбрано белое дерево вместо синего
        white_tree_always_tests = [
            (0, 'blue', 10001),  # (use-multi-navigation-trees, color, result_nid)
            (1, 'blue', 10001),
            (0, 'green', 10001),
            (1, 'green', 10001),
        ]
        for multi_tree_flag, color, result_nid in white_tree_always_tests:
            response = self.report.request_json(
                request + '&use-multi-navigation-trees={}&rgb={}'.format(multi_tree_flag, color)
            )
            self.assertFragmentIn(
                response,
                {
                    'navnodes': [
                        {
                            'id': result_nid,
                        }
                    ],
                },
            )

    def test_multi_navigation_nid_search(self):
        """
        Что проверяем: поиск по нидам из леса с несколькими деревьями.
        Поиск по поисковому литералу точно работает по белому
        навигационному дереву с синим поддеревом
        """
        response = self.report.request_json(
            "place=prime&nid=10001&use-multi-navigation-trees=1&rgb=blue&rids=213&rearr-factors=market_use_white_tree_always=1"
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {'raw': 'blue offer for multi trees'},
            },
        )

    @classmethod
    def prepare_not_leaf_hid__search_by_nid(cls):
        cls.index.navtree += [
            NavCategory(
                nid=80001,
                hid=40001,
                children=[
                    NavCategory(nid=80002, hid=40001, primary=False),
                ],
            ),
        ]

        cls.index.navtree_blue += [
            NavCategory(
                nid=40001,
                hid=40001,
                children=[
                    NavCategory(nid=40002, hid=40001, primary=False),
                ],
            ),
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=40001,
                children=[
                    HyperCategory(hid=40002),
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=40002, hid=40002),
        ]

        cls.index.mskus += [
            MarketSku(
                title="hid without nid",
                hyperid=40002,
                sku="2",
                blue_offers=[
                    BlueOffer(
                        price=10,
                        vat=Vat.VAT_10,
                        feedid=1111111,
                    ),
                ],
            ),
        ]

    def test_not_leaf_hid__search_by_nid(self):

        """
        Что проверяем: при поиске по ниду, находится документ с hid (80002), у которых нет соответствия в nid
        При этом учитываются соответствие родительских hid (80001), превращающийся в nid 40002
        """
        response = self.report.request_json('place=prime&nid=80002&rids=213&use-multi-navigation-trees=1&rgb=blue')
        self.assertFragmentIn(
            response,
            {
                'categories': [
                    {
                        'id': 40002,
                    }
                ],
                'navnodes': [
                    {
                        'id': 80002,
                    }
                ],
            },
        )

    P_ENUM_1 = 700
    P_NUM_1 = 701
    P_BOOL_1 = 702
    P_ENUM_2 = 703
    P_NUM_2 = 704
    P_NUM_3 = 705
    P_BOOL_2 = 706

    BEFORE_VALUE = 4
    MIN_VALUE = 5
    MIDDLE_VALUE = 6
    MAX_VALUE = 7.7
    OVER_VALUE = 12

    M_ENUM_1_V1 = 701
    M_ENUM_1_V2 = 702
    M_ENUM_1_V3 = 703

    M_NUM_1_BEFORE = 711
    M_NUM_1_LOW_BORDER = 712
    M_NUM_1_MIDDLE = 713
    M_NUM_1_HIGH_BORDER = 714
    M_NUM_1_OVER = 715

    M_BOOL_1_V1 = 716
    M_BOOL_1_V0 = 717

    M_ENUM_1_NUM_1 = 731
    M_ENUM_0_NUM_1 = 732
    M_ENUM_1_NUM_0 = 733

    M_NUM_1_BOOL_1 = 741
    M_NUM_0_BOOL_1 = 742
    M_NUM_1_BOOL_0 = 743

    NID_ENUM_1_V1 = 701
    NID_ENUM_1_V2 = 702

    NID_NUM_1_MIN_MAX = 711
    NID_NUM_1_MIN_ONLY = 712
    NID_NUM_1_MAX_ONLY = 713

    NID_BOOL_1 = 714

    NID_ENUM_1_MULTI = 721

    NID_ENUM_NUM = 731
    NID_NUM_BOOL = 741

    @classmethod
    def prepare_nids_with_recipe(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=699,
                children=[
                    HyperCategory(hid=700),
                ],
            ),
        ]

        cls.index.navtree += [
            NavCategory(
                nid=700,
                children=[
                    NavCategory(
                        nid=T.NID_ENUM_1_V1,
                        hid=700,
                        primary=False,
                        recipe=NavRecipe(
                            filters=[
                                NavRecipeFilter(
                                    filter_type=NavRecipeFilter.ENUM, param_id=T.P_ENUM_1, enum_values=[15]
                                ),
                            ]
                        ),
                    ),
                    NavCategory(
                        nid=T.NID_ENUM_1_V2,
                        hid=700,
                        recipe=NavRecipe(
                            filters=[
                                NavRecipeFilter(
                                    filter_type=NavRecipeFilter.ENUM, param_id=T.P_ENUM_1, enum_values=[16]
                                ),
                            ]
                        ),
                    ),
                ],
            ),
            # Для проверки разных типов рецептов
            NavCategory(
                nid=710,
                children=[
                    NavCategory(
                        nid=T.NID_NUM_1_MIN_MAX,
                        hid=700,
                        recipe=NavRecipe(
                            filters=[
                                NavRecipeFilter(
                                    filter_type=NavRecipeFilter.NUMBER,
                                    param_id=T.P_NUM_1,
                                    min_value=T.MIN_VALUE,
                                    max_value=T.MAX_VALUE,
                                ),
                            ]
                        ),
                    ),
                    NavCategory(
                        nid=T.NID_NUM_1_MIN_ONLY,
                        hid=700,
                        recipe=NavRecipe(
                            filters=[
                                NavRecipeFilter(
                                    filter_type=NavRecipeFilter.NUMBER, param_id=T.P_NUM_1, min_value=T.MIN_VALUE
                                ),
                            ]
                        ),
                    ),
                    NavCategory(
                        nid=T.NID_NUM_1_MAX_ONLY,
                        hid=700,
                        recipe=NavRecipe(
                            filters=[
                                NavRecipeFilter(
                                    filter_type=NavRecipeFilter.NUMBER, param_id=T.P_NUM_1, max_value=T.MAX_VALUE
                                ),
                            ]
                        ),
                    ),
                    NavCategory(
                        nid=T.NID_BOOL_1,
                        hid=700,
                        recipe=NavRecipe(
                            filters=[
                                NavRecipeFilter(
                                    filter_type=NavRecipeFilter.BOOLEAN, param_id=T.P_BOOL_1, bool_value=True
                                ),
                            ]
                        ),
                    ),
                ],
            ),
            # Несколько енум значений в одном фильтре. Логика ИЛИ
            NavCategory(
                nid=720,
                children=[
                    NavCategory(
                        nid=T.NID_ENUM_1_MULTI,
                        hid=700,
                        recipe=NavRecipe(
                            filters=[
                                NavRecipeFilter(
                                    filter_type=NavRecipeFilter.ENUM, param_id=T.P_ENUM_1, enum_values=[15, 17]
                                ),
                            ]
                        ),
                    ),
                ],
            ),
            # Несколько разных фильтров в рецепте. Логика И
            NavCategory(
                nid=730,
                children=[
                    NavCategory(
                        nid=T.NID_ENUM_NUM,
                        hid=700,
                        recipe=NavRecipe(
                            filters=[
                                NavRecipeFilter(
                                    filter_type=NavRecipeFilter.ENUM, param_id=T.P_ENUM_2, enum_values=[18]
                                ),
                                NavRecipeFilter(
                                    filter_type=NavRecipeFilter.NUMBER, param_id=T.P_NUM_2, min_value=25, max_value=27.7
                                ),
                            ]
                        ),
                    ),
                ],
            ),
            NavCategory(
                nid=740,
                children=[
                    NavCategory(
                        nid=T.NID_NUM_BOOL,
                        hid=700,
                        recipe=NavRecipe(
                            filters=[
                                NavRecipeFilter(filter_type=NavRecipeFilter.NUMBER, param_id=T.P_NUM_3, max_value=18),
                                NavRecipeFilter(
                                    filter_type=NavRecipeFilter.BOOLEAN, param_id=T.P_BOOL_2, bool_value=True
                                ),
                            ]
                        ),
                    ),
                ],
            ),
            NavCategory(nid=6991, hid=699, primary=False),
            NavCategory(nid=6992, hid=699, is_blue=False),
        ]

        cls.index.gltypes += [
            GLType(param_id=T.P_ENUM_1, hid=700, cluster_filter=False, gltype=GLType.ENUM, values=[15, 16, 17, 18, 19]),
            GLType(param_id=T.P_NUM_1, hid=700, cluster_filter=False, gltype=GLType.NUMERIC),
            GLType(param_id=T.P_BOOL_1, hid=700, cluster_filter=False, gltype=GLType.BOOL, hasboolno=True),
            GLType(param_id=T.P_ENUM_2, hid=700, cluster_filter=False, gltype=GLType.ENUM, values=[15, 16, 17, 18, 19]),
            GLType(param_id=T.P_NUM_2, hid=700, cluster_filter=False, gltype=GLType.NUMERIC),
            GLType(param_id=T.P_NUM_3, hid=700, cluster_filter=False, gltype=GLType.NUMERIC),
            GLType(param_id=T.P_BOOL_2, hid=700, cluster_filter=False, gltype=GLType.BOOL, hasboolno=True),
        ]

        models_and_params = {
            # Модели для enum параметра
            T.M_ENUM_1_V1: [GLParam(param_id=T.P_ENUM_1, value=15)],
            T.M_ENUM_1_V2: [GLParam(param_id=T.P_ENUM_1, value=16)],
            T.M_ENUM_1_V3: [GLParam(param_id=T.P_ENUM_1, value=17)],
            # Модели для number параметра
            T.M_NUM_1_BEFORE: [GLParam(param_id=T.P_NUM_1, value=T.BEFORE_VALUE)],
            T.M_NUM_1_LOW_BORDER: [GLParam(param_id=T.P_NUM_1, value=T.MIN_VALUE)],
            T.M_NUM_1_MIDDLE: [GLParam(param_id=T.P_NUM_1, value=T.MIDDLE_VALUE)],
            T.M_NUM_1_HIGH_BORDER: [GLParam(param_id=T.P_NUM_1, value=T.MAX_VALUE)],
            T.M_NUM_1_OVER: [GLParam(param_id=T.P_NUM_1, value=T.OVER_VALUE)],
            # Модели для bool параметра
            T.M_BOOL_1_V1: [GLParam(param_id=T.P_BOOL_1, value=1)],
            T.M_BOOL_1_V0: [GLParam(param_id=T.P_BOOL_1, value=0)],
            # Модели комбинированного параметра enum+number
            T.M_ENUM_1_NUM_1: [GLParam(param_id=T.P_ENUM_2, value=18), GLParam(param_id=T.P_NUM_2, value=25)],
            T.M_ENUM_1_NUM_0: [GLParam(param_id=T.P_ENUM_2, value=18), GLParam(param_id=T.P_NUM_2, value=27.8)],
            T.M_ENUM_0_NUM_1: [GLParam(param_id=T.P_ENUM_2, value=19), GLParam(param_id=T.P_NUM_2, value=25)],
            # Модели комбинированного параметра number+bool
            T.M_NUM_1_BOOL_1: [GLParam(param_id=T.P_NUM_3, value=18), GLParam(param_id=T.P_BOOL_2, value=1)],
            T.M_NUM_1_BOOL_0: [GLParam(param_id=T.P_NUM_3, value=18), GLParam(param_id=T.P_BOOL_2, value=0)],
            T.M_NUM_0_BOOL_1: [GLParam(param_id=T.P_NUM_3, value=19), GLParam(param_id=T.P_BOOL_2, value=1)],
        }

        for model, params in models_and_params.items():
            cls.index.models += [
                Model(title="ModelRecipe-{}".format(model), hyperid=model, hid=700, glparams=params),
            ]
            cls.index.mskus += [
                MarketSku(
                    title="OfferRecipe-{}".format(model),
                    hyperid=model,
                    sku=str(1000 + model),
                    blue_offers=[
                        BlueOffer(
                            price=10,
                            vat=Vat.VAT_10,
                            feedid=1111111,
                        ),
                    ],
                ),
            ]

    def __check_nids_with_recipe(self, request, expect):
        for nid, models in expect.items():
            response = self.report.request_json(request.format(nid))
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'titles': {'raw': 'ModelRecipe-{}'.format(model)},
                            'navnodes': [
                                {
                                    'id': nid,
                                }
                            ],
                            'offers': {
                                'items': [
                                    {
                                        'navnodes': [
                                            {
                                                'id': nid,
                                            }
                                        ],
                                    }
                                ],
                            },
                        }
                        for model in models
                    ]
                },
                allow_different_len=False,
            )

    def test_nids_with_recipe(self):
        '''
        Что проверяем: В категориях с рецептами находится только те модели, которые соответствует этой категории
        '''

        expect = {
            # enum
            T.NID_ENUM_1_V1: [T.M_ENUM_1_V1],  # В каждую листовую категорию попадает только одна модель
            T.NID_ENUM_1_V2: [T.M_ENUM_1_V2],
            T.NID_ENUM_1_MULTI: [
                T.M_ENUM_1_V1,
                T.M_ENUM_1_V3,
            ],  # В категории с фильтром, имеющим несколько значений, попадает две модели по ИЛИ
            # number
            T.NID_NUM_1_MIN_MAX: [T.M_NUM_1_LOW_BORDER, T.M_NUM_1_MIDDLE, T.M_NUM_1_HIGH_BORDER],
            T.NID_NUM_1_MIN_ONLY: [T.M_NUM_1_LOW_BORDER, T.M_NUM_1_MIDDLE, T.M_NUM_1_HIGH_BORDER, T.M_NUM_1_OVER],
            T.NID_NUM_1_MAX_ONLY: [T.M_NUM_1_BEFORE, T.M_NUM_1_LOW_BORDER, T.M_NUM_1_MIDDLE, T.M_NUM_1_HIGH_BORDER],
            # bool
            T.NID_BOOL_1: [T.M_BOOL_1_V1],
            # несколько фильтров накладываются по И
            T.NID_ENUM_NUM: [
                T.M_ENUM_1_NUM_1
            ],  # Модели M_ENUM_0_NUM_1, M_ENUM_1_NUM_0 не попали, т.к. не удовлетворяют одному из условий
            T.NID_NUM_BOOL: [
                T.M_NUM_1_BOOL_1
            ],  # Модели M_NUM_0_BOOL_1, M_NUM_1_BOOL_0 не попали, т.к. не удовлетворяют одному из условий
        }

        # На синем и белом маркете выдача совпадает.
        self.__check_nids_with_recipe(
            'place=prime&nid={}&rids=213&allow-collapsing=1&rgb=blue&rearr-factors=white_recipes_checking=0', expect
        )
        self.__check_nids_with_recipe(
            'place=prime&nid={}&rids=213&allow-collapsing=1&text=ModelRecipe&use-default-offers=1&rgb=green&rearr-factors=white_recipes_checking=0',
            expect,
        )

    def test_nids_with_recipe_virtual(self):
        '''
        Что проверяем: в виртуальной категории 700 вывелись модели, подходящие под каждый дочерний рецепт (701, 702)
        '''
        response = self.report.request_json(
            'place=prime&nid=700&rids=213&allow-collapsing=1&rgb=blue&rearr-factors=white_recipes_checking=0'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'ModelRecipe-{}'.format(nid)},
                        'navnodes': [
                            {
                                'id': nid,
                            }
                        ],
                        'offers': {
                            'items': [
                                {
                                    'navnodes': [
                                        {
                                            'id': nid,
                                        }
                                    ],
                                }
                            ],
                        },
                    }
                    for nid in [T.NID_ENUM_1_V1, T.NID_ENUM_1_V2]
                ]
            },
            allow_different_len=False,
        )

    def __check_nids_with_recipe_text_search(self, request, nid_for_699):
        # Список ожидаемых нидов и их моделей
        #   (model, nid)
        models_and_nids = [
            # Оферы/модели, которые подходят под какой-то рецепт прикрепляются к этому рецепту
            (T.M_ENUM_1_V1, T.NID_ENUM_1_MULTI),  # Был выбран рецепт 721, а не 701, т.к. 701 не приоритетен
            (T.M_ENUM_1_V2, T.NID_ENUM_1_V2),
            (T.M_ENUM_1_V3, T.NID_ENUM_1_MULTI),
            (T.M_NUM_1_BEFORE, T.NID_NUM_1_MAX_ONLY),  # Подходит только под этот рецепт
            (T.M_NUM_1_LOW_BORDER, T.NID_NUM_1_MIN_MAX),  # Первый рецепт, которому подошел
            (T.M_NUM_1_MIDDLE, T.NID_NUM_1_MIN_MAX),
            (T.M_NUM_1_HIGH_BORDER, T.NID_NUM_1_MIN_MAX),
            (T.M_NUM_1_OVER, T.NID_NUM_1_MIN_ONLY),  # Подходит только под этот рецепт
            (T.M_BOOL_1_V1, T.NID_BOOL_1),
            (T.M_ENUM_1_NUM_1, T.NID_ENUM_NUM),
            (T.M_NUM_1_BOOL_1, T.NID_NUM_BOOL),
            # Не подошли ни к одному рецепту. Прикрепились к ниду родительского хида
            (T.M_BOOL_1_V0, nid_for_699),
            (T.M_ENUM_0_NUM_1, nid_for_699),
            (T.M_ENUM_1_NUM_0, nid_for_699),
            (T.M_NUM_0_BOOL_1, nid_for_699),
            (T.M_NUM_1_BOOL_0, nid_for_699),
        ]

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'ModelRecipe-{}'.format(model)},
                        'navnodes': [
                            {
                                'id': nid,
                            }
                        ],
                        'offers': {
                            'items': [
                                {
                                    'navnodes': [
                                        {
                                            'id': nid,
                                        }
                                    ],
                                }
                            ],
                        },
                    }
                    for model, nid in models_and_nids
                ]
            },
            allow_different_len=False,
        )

    def test_nids_with_recipe_text_search(self):
        '''
        Что проверяем: текстовый поиск и определение нида для рецептов
        И на синем и не белом используется одно нидовое дерево: берется основной nid=6992
        '''

        self.__check_nids_with_recipe_text_search(
            'place=prime&rids=213&allow-collapsing=1&rgb=blue&text=OfferRecipe&numdoc=100&rearr-factors=market_metadoc_search=no',
            6992,
        )
        self.__check_nids_with_recipe_text_search(
            'place=prime&rids=213&rgb=green&numdoc=100&allow-collapsing=1&text=OfferRecipe&use-default-offers=1&rearr-factors=market_metadoc_search=no',
            6992,
        )

    def test_nid_intents_recipe(self):
        '''
        Проверяем вывод фильтров в нидовых интентах
        '''
        response = self.report.request_json(
            'place=prime&rids=213&allow-collapsing=1&rgb=blue&text=OfferRecipe&debug=1&rearr-factors=turn_off_nid_intents_on_serp=0'
        )
        self.assertFragmentIn(
            response,
            {
                'intents': [
                    {
                        'category': {
                            'nid': 710,
                            'isLeaf': False,
                            'glfilters': Absent(),
                        },
                        'intents': [
                            {
                                'category': {
                                    'nid': 711,
                                    'isLeaf': True,
                                    'glfilters': ['701:5~7.7'],
                                },
                            },
                            {
                                'category': {
                                    'nid': 712,
                                    'isLeaf': True,
                                    'glfilters': ['701:5~'],
                                },
                            },
                            {
                                'category': {
                                    'nid': 713,
                                    'isLeaf': True,
                                    'glfilters': ['701:~7.7'],
                                },
                            },
                            {
                                'category': {
                                    'nid': 714,
                                    'isLeaf': True,
                                    'glfilters': ['702:1'],
                                },
                            },
                        ],
                    },
                    {
                        'category': {
                            'nid': 6992,
                            'isLeaf': True,
                            'glfilters': Absent(),
                        },
                    },
                    {
                        'category': {
                            'nid': 720,
                            'isLeaf': False,
                        },
                        'intents': [
                            {
                                'category': {
                                    'nid': 721,
                                    'isLeaf': True,
                                    'glfilters': ['700:15,17'],
                                },
                            }
                        ],
                    },
                    {
                        'category': {
                            'nid': 700,
                            'isLeaf': False,
                        },
                        'intents': [
                            {
                                'category': {
                                    'nid': 702,
                                    'isLeaf': True,
                                    'glfilters': ['700:16'],
                                },
                            }
                        ],
                    },
                    {
                        'category': {
                            'nid': 730,
                            'isLeaf': False,
                        },
                        'intents': [
                            {
                                'category': {
                                    'nid': 731,
                                    'isLeaf': True,
                                    'glfilters': ['703:18', '704:25~27.7'],
                                },
                            }
                        ],
                    },
                    {
                        'category': {
                            'nid': 740,
                            'isLeaf': False,
                        },
                        'intents': [
                            {
                                'category': {
                                    'nid': 741,
                                    'isLeaf': True,
                                    'glfilters': ['705:~18', '706:1'],
                                },
                            }
                        ],
                    },
                ]
            },
        )

    @classmethod
    def prepare_redirects(cls):
        cls.index.nidsredirector = [
            NidsRedirector(
                "green",
                [
                    RedirectorRecord(frm=100301, to=301),
                    RedirectorRecord(
                        frm=302,  # Этот нид есть
                        to=222302,  # Этого нида нет. Редиректа в него не будет, но будет редирект в frm
                    ),
                    RedirectorRecord(frm=100305, to=11123344),  # не существует в дереве
                ],
            )
        ]

    def test_revert_redirects(self):
        '''
        Проверяем обратную подмену нида
        Раз исходный нид есть в дереве, значит на него надо делать редирект
        '''
        for nid in [302, 222302]:
            response = self.report.request_json('place=prime&allow-collapsing=1&text=sony&nid={}'.format(nid))
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {
                                'raw': 'sony',
                            },
                            'navnodes': [{'id': 302}],
                        }
                    ]
                },
                allow_different_len=False,
            )

    def test_redirects(self):
        '''
        Проверяем редирект
        '''

        response = self.report.request_json('place=prime&allow-collapsing=1&text=sony&nid=100301')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'titles': {
                            'raw': 'sony three',
                        },
                        'navnodes': [{'id': 303}],
                    },
                    {
                        'entity': 'offer',
                        'titles': {
                            'raw': 'sony',
                        },
                        'navnodes': [{'id': 302}],
                    },
                    {
                        'entity': 'offer',
                        'titles': {
                            'raw': 'sony two',
                        },
                        'navnodes': [{'id': 303}],
                    },
                ]
            },
        )

    def test_missed_nid(self):
        '''
        Проверяем ошибку нахождения нида (3632). Она работает, если нормального редиректа нет
        И 9171 (NAVIGATION_FOREST_CANNOT_FIND_NAVIGATION_NODE_WITH_GIVEN_NID), тк теперь на белом поиск по нидам
        '''
        self.report.request_json(
            'place=prime&allow-collapsing=1&text=sony&nid=100305&rearr-factors=market_use_white_tree_always=0&enable-hard-filters=0'
        )
        self.error_log.expect(code=3632).times(9)
        self.error_log.expect(code=9171).times(6)

        '''
        Проверяем ошибку нахождения нида в мульти дереве. Она работает, если нормального редиректа нет
        '''
        self.report.request_json(
            'place=prime&allow-collapsing=1&text=sony&nid=100305&rearr-factors=market_use_white_tree_always=1&enable-hard-filters=0'
        )
        self.error_log.expect(code=3632).times(2)
        self.error_log.expect(code=9171).times(14)

    @classmethod
    def prepare_white_nid_searching(cls):
        cls.index.hypertree += [
            HyperCategory(hid=1580),
            HyperCategory(hid=1581),
            HyperCategory(hid=1582),
            HyperCategory(hid=1583),
            HyperCategory(hid=1584),
            HyperCategory(hid=1585),
            HyperCategory(hid=1586),
            HyperCategory(hid=1587),
            HyperCategory(hid=1588),
            HyperCategory(hid=1589),
            HyperCategory(hid=1590),
        ]

        cls.index.navtree += [
            NavCategory(
                nid=1251,
                name='Гвинтокарты',
                is_blue=False,
                children=[
                    NavCategory(
                        nid=1252,
                        hid=1580,
                        name='Королевства севера',
                        is_blue=False,
                        children=[
                            NavCategory(
                                nid=1352,
                                name='Редкие карты',
                                is_blue=False,
                                children=[
                                    NavCategory(nid=1452, hid=1581, name='Пехота Темерии', primary=True, is_blue=False),
                                    NavCategory(nid=1462, hid=1582, name='Кавалерия Каэдвена', is_blue=False),
                                ],
                            ),
                            NavCategory(
                                nid=1353,
                                name='Легендарные карты',
                                is_blue=False,
                                children=[
                                    NavCategory(nid=1453, hid=1583, name='Чародейки', primary=True, is_blue=False),
                                    NavCategory(nid=1463, hid=1584, name='Ведьмаки', is_blue=False),
                                ],
                            ),
                        ],
                    ),
                ],
            ),
            NavCategory(
                nid=1354,
                name='Зелья (простой случай с одним ребенком-рецептом)',
                is_blue=False,
                children=[
                    NavCategory(
                        nid=1454,
                        hid=1585,
                        name='Ласточка',
                        is_blue=False,
                        recipe=NavRecipe(
                            filters=[
                                NavRecipeFilter(filter_type=NavRecipeFilter.NUMBER, param_id=133, max_value=100),
                                NavRecipeFilter(filter_type=NavRecipeFilter.BOOLEAN, param_id=144, bool_value=True),
                                NavRecipeFilter(filter_type=NavRecipeFilter.ENUM, param_id=155, enum_values=[10]),
                            ]
                        ),
                    ),
                ],
            ),
            NavCategory(
                nid=1355,
                name='Броня (есть дочерние ноды с рецептами)',
                is_blue=False,
                children=[
                    NavCategory(
                        nid=1455,
                        hid=1586,
                        name='Доспехи',
                        is_blue=False,
                        recipe=NavRecipe(
                            filters=[
                                NavRecipeFilter(filter_type=NavRecipeFilter.BOOLEAN, param_id=200, bool_value=True),
                            ]
                        ),
                    ),
                    NavCategory(
                        nid=1465,
                        hid=1587,
                        name='Перчатки',
                        is_blue=False,
                        recipe=NavRecipe(
                            filters=[
                                NavRecipeFilter(filter_type=NavRecipeFilter.BOOLEAN, param_id=201, bool_value=True),
                            ]
                        ),
                    ),
                    NavCategory(nid=1475, hid=1588, name='Сапоги', is_blue=False),
                    NavCategory(
                        nid=1485,
                        hid=1589,
                        name='Штаны',
                        is_blue=False,
                        children=[
                            NavCategory(
                                nid=1486,
                                hid=1590,
                                name='Штаны - тяжелые',
                                is_blue=False,
                                recipe=NavRecipe(
                                    filters=[
                                        NavRecipeFilter(
                                            filter_type=NavRecipeFilter.BOOLEAN, param_id=202, bool_value=True
                                        ),
                                    ]
                                ),
                            ),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.gltypes += [
            GLType(param_id=133, hid=1585, cluster_filter=False, gltype=GLType.NUMERIC),
            GLType(param_id=144, hid=1585, cluster_filter=False, gltype=GLType.BOOL),
            GLType(param_id=155, hid=1585, cluster_filter=False, gltype=GLType.ENUM, values=[10, 11, 12]),
            GLType(param_id=244, hid=1585, cluster_filter=False, gltype=GLType.BOOL),
            GLType(param_id=200, hid=1586, cluster_filter=False, gltype=GLType.BOOL),
            GLType(param_id=255, hid=1586, cluster_filter=False, gltype=GLType.BOOL),
            GLType(param_id=201, hid=1587, cluster_filter=False, gltype=GLType.BOOL),
            GLType(param_id=202, hid=1590, cluster_filter=False, gltype=GLType.BOOL),
        ]

        cls.index.models += [
            Model(hyperid=100500, hid=1580, title='Фольтест'),  # nid=1251,1252
            Model(hyperid=100501, hid=1582, title='Бурая хоругвь'),  # nid=1251,1252,1352,1462
            Model(hyperid=100502, hid=1583, title='Йеннифэр из Венгерберга'),  # nid=1251,1252,1353,1453
            # Модели для рецептных нод
            Model(
                hyperid=100503,
                hid=1585,
                title='Ласточка : редкая, gl - OK',
                glparams=[
                    GLParam(param_id=133, value=50),
                    GLParam(param_id=144, value=1),
                    GLParam(param_id=155, value=10),
                ],
            ),  # nid=1251,1354,1454
            Model(
                hyperid=100504,
                hid=1585,
                title='Ласточка : редкая, glbool == False',
                glparams=[
                    GLParam(param_id=133, value=50),
                    GLParam(param_id=144, value=0),
                    GLParam(param_id=155, value=10),
                ],
            ),  # nid=1251,1354,1454
            Model(
                hyperid=100505,
                hid=1585,
                title='Ласточка : редкая, glint > maxval',
                glparams=[
                    GLParam(param_id=133, value=120),
                    GLParam(param_id=144, value=1),
                    GLParam(param_id=155, value=10),
                ],
            ),  # nid=1251,1354,1454
            Model(
                hyperid=100506,
                hid=1585,
                title='Ласточка : редкая, glenum != 10',
                glparams=[
                    GLParam(param_id=133, value=50),
                    GLParam(param_id=144, value=1),
                    GLParam(param_id=155, value=11),
                ],
            ),  # nid=1251,1354,1454
        ]

        cls.index.offers += [
            Offer(hid=1580, hyperid=100500),  # nid=1251,1252
            Offer(hid=1582, hyperid=100501),  # nid=1251,1252,1352,1462
            Offer(hid=1583, hyperid=100502),  # nid=1251,1252,1353,1453
            Offer(hid=1584, auto_creating_model=False, title='Геральт из Ривии : Аард'),  # nid=1251,1252,1353,1463
            # Оффера для рецептных нод
            Offer(
                hid=1585,
                auto_creating_model=False,
                title='Ласточка : gl - OK',
                glparams=[
                    GLParam(param_id=133, value=50),
                    GLParam(param_id=144, value=1),
                    GLParam(param_id=155, value=10),
                ],
            ),  # nid=1251,1354,1454
            Offer(
                hid=1585,
                auto_creating_model=False,
                title='Ласточка : glbool == False',
                glparams=[
                    GLParam(param_id=133, value=120),
                    GLParam(param_id=144, value=0),
                    GLParam(param_id=155, value=10),
                ],
            ),  # nid=1251,1354,1454
            Offer(
                hid=1585,
                auto_creating_model=False,
                title='Ласточка : glint > maxval',
                glparams=[
                    GLParam(param_id=133, value=120),
                    GLParam(param_id=144, value=1),
                    GLParam(param_id=155, value=10),
                ],
            ),  # nid=1251,1354,1454
            Offer(
                hid=1585,
                auto_creating_model=False,
                title='Ласточка : glenum != 10',
                glparams=[
                    GLParam(param_id=133, value=50),
                    GLParam(param_id=144, value=1),
                    GLParam(param_id=155, value=11),
                ],
            ),  # nid=1251,1354,1454
            Offer(
                hid=1586,
                auto_creating_model=False,
                title='Доспехи : gl - Ok',
                glparams=[
                    GLParam(param_id=200, value=1),
                ],
            ),  # nid=1251,1355,1455
            Offer(
                hid=1586,
                auto_creating_model=False,
                title='Доспехи : gl == False',
                glparams=[
                    GLParam(param_id=200, value=0),
                ],
            ),  # nid=1251,1355,1455
            Offer(
                hid=1587,
                auto_creating_model=False,
                title='Перчатки : gl - Ok',
                glparams=[
                    GLParam(param_id=201, value=1),
                ],
            ),  # nid=1251,1355,1465
            Offer(
                hid=1587,
                auto_creating_model=False,
                title='Перчатки : gl == False',
                glparams=[
                    GLParam(param_id=201, value=0),
                ],
            ),  # nid=1251,1355,1465
            Offer(hid=1588, auto_creating_model=False, title='Просто сапоги'),  # nid=1251,1355,1475
            Offer(hid=1589, auto_creating_model=False, title='Просто штаны'),  # nid=1251,1355,1485
            Offer(
                hid=1590,
                auto_creating_model=False,
                title='Штаны - тяжелые : gl - Ok',
                glparams=[
                    GLParam(param_id=202, value=1),
                ],
            ),  # nid=1251,1355,1465
            Offer(
                hid=1590,
                auto_creating_model=False,
                title='Штаны - тяжелые : gl == False',
                glparams=[
                    GLParam(param_id=202, value=0),
                ],
            ),  # nid=1251,1355,1465
        ]

        NavCategory(
            nid=1355,
            name='Броня (есть дочерние ноды с рецептами)',
            is_blue=False,
            children=[
                NavCategory(
                    nid=1455,
                    hid=1586,
                    name='Доспехи',
                    is_blue=False,
                    recipe=NavRecipe(
                        filters=[
                            NavRecipeFilter(filter_type=NavRecipeFilter.BOOLEAN, param_id=200, bool_value=True),
                        ]
                    ),
                ),
                NavCategory(
                    nid=1465,
                    hid=1587,
                    name='Перчатки',
                    is_blue=False,
                    recipe=NavRecipe(
                        filters=[
                            NavRecipeFilter(filter_type=NavRecipeFilter.BOOLEAN, param_id=201, bool_value=True),
                        ]
                    ),
                ),
                NavCategory(nid=1475, hid=1588, name='Сапоги', is_blue=False),
                NavCategory(
                    nid=1485,
                    hid=1589,
                    name='Штаны',
                    is_blue=False,
                    children=[
                        NavCategory(
                            nid=1486,
                            hid=1590,
                            name='Штаны - тяжелые',
                            is_blue=False,
                            recipe=NavRecipe(
                                filters=[
                                    NavRecipeFilter(filter_type=NavRecipeFilter.BOOLEAN, param_id=202, bool_value=True),
                                ]
                            ),
                        ),
                    ],
                ),
            ],
        ),

    def test_search_white_by_nid_literal_positive(self):
        """
        Поиск по ниду, nid при этом не расхлапывается в hid-ы
        т.к. теперь на белом поиск по nid-литералу возможен
        """

        request = 'place=prime&allow-collapsing=1&nid=1251&rearr-factors=market_metadoc_search=no'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "titles": {"raw": "Фольтест"},
                        "navnodes": [
                            {
                                "id": 1252,
                            }
                        ],
                    },
                    {
                        "entity": "product",
                        "titles": {"raw": "Бурая хоругвь"},
                        "navnodes": [
                            {
                                "id": 1462,
                            }
                        ],
                    },
                    {
                        "entity": "product",
                        "titles": {"raw": "Йеннифэр из Венгерберга"},
                        "navnodes": [
                            {
                                "id": 1453,
                            }
                        ],
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "Геральт из Ривии : Аард"},
                        "navnodes": [
                            {
                                "id": 1463,
                            }
                        ],
                    },
                ]
            },
            allow_different_len=False,
        )

        request = 'place=prime&allow-collapsing=1&nid=1353&rearr-factors=market_metadoc_search=no'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "titles": {"raw": "Йеннифэр из Венгерберга"},
                        "navnodes": [
                            {
                                "id": 1453,
                            }
                        ],
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "Геральт из Ривии : Аард"},
                        "navnodes": [
                            {
                                "id": 1463,
                            }
                        ],
                    },
                ]
            },
            allow_different_len=False,
        )

        request = 'place=prime&allow-collapsing=1&nid=1463&rearr-factors=market_metadoc_search=no'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "Геральт из Ривии : Аард"},
                        "navnodes": [
                            {
                                "id": 1463,
                            }
                        ],
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_nid_search_recipe_nodes(self):
        """
        К нам пришли с любым нидом
        В таком случае в выдачу попадают и документы из нод-потомков,
        среди которых могут быть ноды с рецептами. Надо для таких ребят достать этот рецепт и примерить
        """

        nid_1354_ans = {
            "results": [
                {
                    "entity": "product",
                    "id": 100503,
                    "titles": {"raw": "Ласточка : редкая, gl - OK"},
                    "navnodes": [
                        {
                            "id": 1454,
                        }
                    ],
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "Ласточка : gl - OK"},
                    "navnodes": [
                        {
                            "id": 1454,
                        }
                    ],
                },
            ]
        }

        # Запрашиваем nid=1354, у этой ноды есть ребенок-рецепт : nid=1454
        # в рецепте фильтры 133, 144, 155. Условия:
        # 1) 133 : param < 100
        # 2) 144 : param == True
        # 3) 155 : param == 10
        # => В выдачу попадут только оффер "Ласточка : gl - OK" и модель 100503
        # Также в данном тесте проверяется, что все типы фильтров работают (enum, int, bool)
        request = 'place=prime&allow-collapsing=1&nid=1354&rearr-factors=white_recipes_checking=0'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, nid_1354_ans, allow_different_len=False)

        # Сейчас на прайме, если указать в запросе gl фильтр, будет вызвана немного другая логика фильтрации:
        # https://a.yandex-team.ru/arc/trunk/arcadia/market/report/src/place/prime/prime.cpp?rev=r8192502#L2234
        # Важно! Cейчас в запросе в репорт из нидовой выдачи параметр &glfilter может пролезть только под флагом market_use_childs_for_nid_to_hid
        # Также в &glfilter могут оказаться только параметры первого прямого потомка с hid-ом
        request = (
            'place=prime&allow-collapsing=1&nid=1354&rearr-factors=market_use_childs_for_nid_to_hid=1&glfilter=244:0'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, nid_1354_ans, allow_different_len=False)

        nid_1355_ans = {
            "results": [
                {
                    "entity": "offer",
                    "titles": {"raw": "Доспехи : gl - Ok"},
                    "navnodes": [
                        {
                            "id": 1455,
                        }
                    ],
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "Перчатки : gl - Ok"},
                    "navnodes": [
                        {
                            "id": 1465,
                        }
                    ],
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "Просто сапоги"},
                    "navnodes": [
                        {
                            "id": 1475,
                        }
                    ],
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "Просто штаны"},
                    "navnodes": [
                        {
                            "id": 1485,
                        }
                    ],
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "Штаны - тяжелые : gl - Ok"},
                    "navnodes": [
                        {
                            "id": 1486,
                        }
                    ],
                },
            ]
        }

        # Более сложный запрос
        # У нида 1355 есть два прямых потомка-рецепта : nid = 1455, 1465
        # А также есть потомок nid=1485, который в свою очередь есть ребенок nid=1486
        # Проверяем, что из nid=1455, 1465, 1486 остались только оффера с "gl - Ok" в названии
        # При этом из нерецептных категорий ничего не должно пропасть
        request = 'place=prime&allow-collapsing=1&nid=1355'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, nid_1355_ans, allow_different_len=False)

        # С логикой предварительной фильтрации тоже должно быть ок
        request = (
            'place=prime&allow-collapsing=1&nid=1355&rearr-factors=market_use_childs_for_nid_to_hid=1&glfilter=255:0'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, nid_1355_ans, allow_different_len=False)


if __name__ == '__main__':
    main()
