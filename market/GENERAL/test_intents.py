#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    GLParam,
    GLType,
    HyperCategory,
    MnPlace,
    Model,
    NavCategory,
    NavRecipe,
    NavRecipeFilter,
    Offer,
    Picture,
    PictureMbo,
    Shop,
    VCluster,
    ViewType,
    MarketSku,
    BlueOffer,
)
from core.matcher import (
    Absent,
    NotEmpty,
    NotEmptyList,
    EmptyList,
    NotEmptyDict,
    EmptyDict,
    NoKey,
    Contains,
    Greater,
    Round,
)
from core.types.navcategory import ModelList, HidList

# Tests intents (also known as пупыри).
# See https://st.yandex-team.ru/MARKETOUT-7492.

# Результат должен быть тот же при использовании альтернативного метода
DIRECT_SNIPPET_FLAG = ';market_direct_snippet_intents=1'


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.hypertree = [
            HyperCategory(
                hid=42,
                name='ROOT',
                children=[
                    HyperCategory(
                        hid=1,
                        name='A',
                        view_type=ViewType.GRID,
                        children=[
                            HyperCategory(hid=2, name='AA', view_type=ViewType.GRID),
                            HyperCategory(
                                hid=3,
                                name='AB',
                                view_type=ViewType.LIST,
                                children=[HyperCategory(hid=4, name='ABA', view_type=ViewType.LIST)],
                            ),
                        ],
                    ),
                    HyperCategory(
                        hid=5,
                        name='B',
                        children=[
                            HyperCategory(hid=6, name='BA'),
                            HyperCategory(hid=7, name='BB'),
                        ],
                    ),
                    HyperCategory(hid=8, name='C'),
                ],
            ),
            HyperCategory(hid=15754673, name='Medicine'),
        ]

        # Some leaf categories from the category tree become non-leaf in the navigation tree and vice versa:
        #   * BB is now a child of BA (and renamed to BAA), which means BA becomes non-leaf;
        #   * ABA is now connected directly to A (and renamed to AC), which means AB becomes leaf.
        cls.index.navtree = [
            NavCategory(
                hid=0,
                nid=1000,
                name="N",
                children=[
                    NavCategory(
                        hid=1,
                        nid=1001,
                        name='NA',
                        children=[
                            NavCategory(hid=2, nid=1002, name='NAA'),
                            NavCategory(hid=3, nid=1003, name='NAB'),
                            NavCategory(hid=4, nid=1004, name='NAC'),
                        ],
                    ),
                    NavCategory(
                        hid=5,
                        nid=1005,
                        name='NB',
                        children=[
                            NavCategory(
                                hid=6,
                                nid=1006,
                                name='NBA',
                                children=[
                                    NavCategory(hid=7, nid=1007, name='NBAA'),
                                ],
                            ),
                        ],
                    ),
                    NavCategory(hid=8, nid=1008, name='NC'),
                ],
            )
        ]

        cls.index.models += [Model(hid=1, hyperid=100, ts=10000)]

        # Add offer 'pepyaka' to each category except C.
        cls.index.offers += [
            Offer(title='pepyaka', hid=1, hyperid=100, ts=10001),
            Offer(title='pepyaka', hid=1, hyperid=100, ts=10002),
            Offer(title='pepyaka', hid=2, ts=10003),
            Offer(title='pepyaka', hid=3, ts=10004),
            Offer(title='pepyaka', hid=4, ts=10005),
            Offer(title='pepyaka', hid=5, ts=10006),
            Offer(title='pepyaka', hid=6, ts=10007),
            Offer(title='pepyaka', hid=7, ts=10008),
            Offer(title='medicineoffer', hid=15754673, ts=10009),
        ]

        for i in range(0, 10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10000 + i).respond(0.3 + 0.001 * i)

    def check_intents_equal(self, response, response_direct_snippet):
        self.assertEqual(response['intents'], response_direct_snippet['intents'])

    def test_kinds_medicine(self):
        response = self.report.request_json('place=prime&hid=15754673')
        self.assertFragmentIn(response, {"intents": [{"category": {"kinds": ["medicine"]}}]})

    def expected_hid_intents(self, intent_names=None):
        def filter_intents(root, names):
            if "intents" in root:
                root["intents"] = [item for item in root["intents"] if item["category"]["name"] in names]
                if len(root["intents"]) == 0:
                    del root["intents"]
                else:
                    for item in root["intents"]:
                        filter_intents(item, names)
            return root

        intents = {
            "intents": [
                {
                    "intents": [
                        {"category": {"name": "AA"}, "defaultOrder": NotEmpty()},
                        {
                            "intents": [{"category": {"name": "ABA"}, "defaultOrder": NotEmpty()}],
                            "category": {"name": "AB"},
                            "defaultOrder": NotEmpty(),
                        },
                    ],
                    "category": {"name": "A"},
                    "defaultOrder": NotEmpty(),
                },
                {
                    "intents": [
                        {
                            "category": {"name": "BA"},
                            "defaultOrder": NotEmpty(),
                        },
                        {
                            "category": {"name": "BB"},
                            "defaultOrder": NotEmpty(),
                        },
                    ],
                    "category": {"name": "B"},
                    "defaultOrder": NotEmpty(),
                },
            ]
        }

        if intent_names:
            filter_intents(intents, intent_names)

        return intents

    def expected_nid_intents(self):
        return {
            "intents": [
                {
                    "intents": [
                        {"category": {"name": "NAA"}, "defaultOrder": NotEmpty()},
                        {"category": {"name": "NAB"}, "defaultOrder": NotEmpty()},
                        {"category": {"name": "NAC"}, "defaultOrder": NotEmpty()},
                    ],
                    "category": {"name": "NA"},
                    "defaultOrder": NotEmpty(),
                },
                {
                    "intents": [
                        {
                            "intents": [{"category": {"name": "NBAA"}, "defaultOrder": NotEmpty()}],
                            "category": {"name": "NBA"},
                            "defaultOrder": NotEmpty(),
                        }
                    ],
                    "category": {"name": "NB"},
                    "defaultOrder": NotEmpty(),
                },
            ]
        }

    def test_hid_intents_on_prime(self):
        response = self.report.request_json('place=prime&text=pepyaka&rearr-factors=market_return_nids_in_intents=0')
        self.assertFragmentIn(response, self.expected_hid_intents())

        not_expected = {"intents": [{"category": {"name": "C"}}]}
        self.assertFragmentNotIn(response, not_expected)

    def test_truncate_hid_intents_on_prime(self):
        expected_intents = self.expected_hid_intents(set(["A", "AA", "AB", "B", "BA", "BB"]))
        not_expected_intents = {"intents": [{"category": {"name": "ABA"}}]}
        response = self.report.request_json(
            'place=prime&text=pepyaka&rearr-factors=market_return_nids_in_intents=0&intents-height={height}'.format(
                height=2
            )
        )
        self.assertFragmentIn(response, expected_intents, allow_different_len=False)
        self.assertFragmentNotIn(response, not_expected_intents)

        response = self.report.request_json(
            'place=prime&text=pepyaka&rearr-factors=market_return_nids_in_intents=0&hid={hid}&intents-height={height}'.format(
                hid=1, height=1
            )
        )
        expected_intents = self.expected_hid_intents(set(["A", "AA", "AB"]))
        self.assertFragmentIn(response, expected_intents, allow_different_len=False)
        self.assertFragmentNotIn(response, not_expected_intents)

        response = self.report.request_json(
            'place=prime&text=pepyaka&rearr-factors=market_return_nids_in_intents=0&nid={nid}&intents-height={height}'.format(
                nid=1001, height=1
            )
        )
        expected_intents = self.expected_hid_intents(set(["A", "AA", "AB"]))
        self.assertFragmentIn(response, expected_intents, allow_different_len=False)
        self.assertFragmentNotIn(response, not_expected_intents)

    def test_nid_intents_on_prime(self):
        response = self.report.request_json(
            'place=prime&text=pepyaka&rearr-factors=market_return_nids_in_intents=1;turn_off_nid_intents_on_serp=0'
        )
        self.assertFragmentIn(response, self.expected_nid_intents())

        not_expected = {"intents": [{"category": {"name": "NC"}}]}
        self.assertFragmentNotIn(response, not_expected)

    def test_hid_intents_on_prime_without_nid_in_request(self):
        # Под временным флагом turn_off_nid_intents_on_serp (по-умолчанию вкл) нидовые интенты будут появляться только, если в запросе есть nid
        # То есть в каталоге
        response = self.report.request_json(
            'place=prime&text=pepyaka&rearr-factors=market_return_nids_in_intents=1;turn_off_nid_intents_on_serp=1'
        )
        self.assertFragmentIn(response, self.expected_hid_intents())

        response = self.report.request_json(
            'place=prime&nid=12345&text=pepyaka&rearr-factors=market_return_nids_in_intents=1;turn_off_nid_intents_on_serp=1'
        )
        self.assertFragmentIn(response, self.expected_nid_intents())

    def test_intents_with_limited_candidates(self):
        """Тестрируем что интенты отдаются более или менее корректно если количество кандидатов для ранжирования по формуле ограниченно"""
        r = '&rearr-factors=market_categories_ranking_candidates_count=1'
        request_nid_intents = 'place=prime&nid=1000&text=pepyaka&debug=da&rearr-factors=market_return_nids_in_intents=1;market_write_category_redirect_features=20'

        response = self.report.request_json(request_nid_intents + r)
        self.assertFragmentIn(response, self.expected_nid_intents())
        self.assertFragmentIn(
            response,
            'TCategoryRanking(): Calculate categories ranking: rootId: 1000 categoryStatsName: niddocuments categoryStatsForOutput:  categoryVClusterStatsName: nidvclusters count categories: 8 count candidates: 4',  # noqa
        )
        self.assertFragmentIn(
            response, 'TCategoryRanking(): Calculate categories ranking: [1001, 1003, 12345, 1000]'
        )  # 1 листовой узел и его родители

        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'navnodes_ranking_json': [{'id': 1003, 'relevanceValue': Round(-1.9875), 'factors': NotEmptyDict()}]
                }
            },
        )
        self.assertFragmentIn(
            response,
            {'debug': {'navnodes_ranking_json': [{'id': 1005, 'relevanceValue': '-inf', 'factors': EmptyDict()}]}},
        )

        self.assertFragmentIn(
            response,
            'TCategoryRanking(): Calculate categories ranking: rootId: 90401 categoryStatsName: categ categoryStatsForOutput: categ categoryVClusterStatsName: categvcluster count categories: 8 count candidates: 6',  # noqa
        )
        self.assertFragmentIn(
            response, 'TCategoryRanking(): Calculate categories ranking: [42, 7, 1, 2, 90401, 5]'
        )  # 1 листовая по порядку, одна по количеству и их родители

        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'categories_ranking_json': [{'id': 1, 'relevanceValue': Round(-1.9875), 'factors': NotEmptyDict()}]
                }
            },
        )
        self.assertFragmentIn(
            response,
            {'debug': {'categories_ranking_json': [{'id': 3, 'relevanceValue': '-inf', 'factors': EmptyDict()}]}},
        )

    def test_write_category_redirect_features_limit_output_factors(self):
        """Флаг market_write_category_redirect_features задает сколько категорий будет записано в вывод"""

        request_nid_intents = (
            'place=prime&nid=1000&text=pepyaka&debug=da&rearr-factors=market_write_category_redirect_features=20'
        )
        response = self.report.request_json(request_nid_intents)
        self.assertFragmentIn(response, {'categories_ranking_json': [{} for _ in range(8)]}, allow_different_len=False)

        request_nid_intents = (
            'place=prime&nid=1000&text=pepyaka&debug=da&rearr-factors=market_write_category_redirect_features=3'
        )
        response = self.report.request_json(request_nid_intents)
        self.assertFragmentIn(response, {'categories_ranking_json': [{} for _ in range(3)]}, allow_different_len=False)

    def test_view_type_of_category_intents(self):
        """Для категорий выводится информация о виде отображения документов в категории"""
        response = self.report.request_json('place=prime&text=pepyaka&rearr-factors=turn_off_nid_intents_on_serp=0')
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {"name": "NA", "view": "grid"},
                        "intents": [
                            {"category": {"name": "NAA", "view": "grid"}},
                            {"category": {"name": "NAB", "view": "list"}},
                            {"category": {"name": "NAC", "view": "list"}},
                        ],
                    },
                    {
                        "category": {"name": "NB", "view": "list"},
                        "intents": [
                            {
                                "category": {"name": "NBA", "view": "list"},
                                "intents": [
                                    {"category": {"name": "NBAA", "view": "list"}},
                                ],
                            }
                        ],
                    },
                ]
            },
        )

        # Под временным флагом turn_off_nid_intents_on_serp (по-умолчанию вкл) нидовые интенты будут появляться только, если в запросе есть nid
        # То есть в каталоге
        response = self.report.request_json(
            'place=prime&text=pepyaka&rearr-factors=market_return_nids_in_intents=1;turn_off_nid_intents_on_serp=1'
        )
        expected = {
            "intents": [
                {
                    "intents": [
                        {"category": {"name": "AA"}, "defaultOrder": NotEmpty()},
                        {
                            "intents": [{"category": {"name": "ABA"}, "defaultOrder": NotEmpty()}],
                            "category": {"name": "AB"},
                            "defaultOrder": NotEmpty(),
                        },
                    ],
                    "category": {"name": "A"},
                    "defaultOrder": NotEmpty(),
                },
                {
                    "intents": [
                        {
                            "category": {"name": "BA"},
                            "defaultOrder": NotEmpty(),
                        },
                        {
                            "category": {"name": "BB"},
                            "defaultOrder": NotEmpty(),
                        },
                    ],
                    "category": {"name": "B"},
                    "defaultOrder": NotEmpty(),
                },
            ]
        }
        self.assertFragmentIn(response, expected)

    @classmethod
    def prepare_hypertree_relevance(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=10,
                children=[
                    HyperCategory(
                        hid=101,
                        name='PRESENTED101',
                        children=[
                            HyperCategory(hid=1011, name='REL1011'),
                            HyperCategory(hid=1012, name='IRR1012'),
                        ],
                    ),
                    HyperCategory(
                        hid=102,
                        name='PRESENTED102',
                        children=[
                            HyperCategory(hid=1021, name='IRR1021'),
                            HyperCategory(hid=1022, name='IRR1022'),
                        ],
                    ),
                ],
            ),
            HyperCategory(
                hid=11,
                name='SAVED',
                children=[
                    HyperCategory(hid=111, name='REL NON LIST', children=[HyperCategory(hid=1111, name='IRR1111')])
                ],
            ),
            HyperCategory(
                hid=12,
                name='REMOVED',
                children=[HyperCategory(hid=121, children=[HyperCategory(hid=1211, name='IRR1211')])],
            ),
        ]

        cls.index.navtree += [
            NavCategory(
                nid=10,
                hid=10,
                children=[
                    NavCategory(
                        nid=101,
                        hid=101,
                        name='PRESENTED101',
                        children=[
                            NavCategory(nid=1011, hid=1011, name='REL1011'),
                            NavCategory(nid=1012, hid=1012, name='IRR1012'),
                        ],
                    ),
                    NavCategory(
                        nid=102,
                        hid=102,
                        name='PRESENTED102',
                        children=[
                            NavCategory(nid=1021, hid=1021, name='IRR1021'),
                            NavCategory(nid=1022, hid=1022, name='IRR1022'),
                        ],
                    ),
                ],
            ),
            NavCategory(
                nid=11,
                hid=11,
                name='SAVED',
                children=[
                    NavCategory(
                        nid=111,
                        hid=111,
                        name='REL NON LIST',
                        children=[NavCategory(nid=1111, hid=1111, name='IRR1111')],
                    )
                ],
            ),
            NavCategory(
                nid=12,
                hid=12,
                name='REMOVED',
                children=[NavCategory(nid=121, hid=121, children=[NavCategory(nid=1211, hid=1211, name='IRR1211')])],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 10).respond(0.1)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 101).respond(0.1)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 1011).respond(0.5)  # one list relevant category
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 1012).respond(0.1)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 102).respond(0.1)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 1021).respond(0.1)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 1022).respond(0.1)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 11).respond(0.1)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 111).respond(0.45)  # non list relevant category
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 1111).respond(0.1)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 12).respond(0.1)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 121).respond(0.1)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 1211).respond(0.1)

        cls.index.offers += [
            Offer(title='cat', hid=1011),
            Offer(title='cat', hid=1012),
            Offer(title='cat', hid=1021),
            Offer(title='cat', hid=1022),
            Offer(title='cat', hid=1022),
            Offer(title='cat', hid=1111),
            Offer(title='cat', hid=1211),
        ]

    def test_hid_relevance_threshold(self):
        """Под флагом market_hid_relevance_threshold
        из выдачи пропадают офферы из листовых категорий имеющих релевантность меньше порога
        в интентах у таких категорий исчезает defaultOrder
        Но! это не работает если в cgi уже задан hid листовой категории
        """

        response = self.report.request_json('place=prime&text=cat&rearr-factors=market_return_nids_in_intents=0')
        self.assertFragmentIn(
            response,
            {
                'search': {'total': 7},  # все 7 офферов на месте
                'intents': [
                    {
                        'category': {'name': 'HID-10'},
                        'defaultOrder': NotEmpty(),
                        'intents': [
                            {
                                'category': {'name': 'PRESENTED101'},
                                'defaultOrder': NotEmpty(),
                                'intents': [
                                    {'category': {'name': 'REL1011'}, 'defaultOrder': NotEmpty()},
                                    {'category': {'name': 'IRR1012'}, 'defaultOrder': NotEmpty()},
                                ],
                            },
                            {
                                'category': {'name': 'PRESENTED102'},
                                'defaultOrder': NotEmpty(),
                                'intents': [
                                    {'category': {'name': 'IRR1021'}, 'defaultOrder': NotEmpty()},
                                    {'category': {'name': 'IRR1022'}, 'defaultOrder': NotEmpty()},
                                ],
                            },
                        ],
                    },
                    {
                        'category': {'name': 'SAVED'},
                        'defaultOrder': NotEmpty(),
                        'intents': [
                            {
                                'category': {'name': 'REL NON LIST'},
                                'defaultOrder': NotEmpty(),
                                'intents': [{'category': {'name': 'IRR1111'}, 'defaultOrder': NotEmpty()}],
                            },
                        ],
                    },
                    {
                        'category': {'name': 'REMOVED'},
                        'defaultOrder': NotEmpty(),
                        'intents': [
                            {
                                'category': {'name': 'HID-121'},
                                'defaultOrder': NotEmpty(),
                                'intents': [{'category': {'name': 'IRR1211'}, 'defaultOrder': NotEmpty()}],
                            },
                        ],
                    },
                ],
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=prime&text=cat&rearr-factors=market_hid_relevance_threshold=0.4&rearr-factors=market_return_nids_in_intents=0'
        )
        self.assertFragmentIn(
            response,
            {
                # остался только один оффер из релевантной категории
                'search': {'total': 1, 'results': [{'entity': 'offer', 'categories': [{'id': 1011}]}]},
                # и только интены относящиеся к этой релевантной категории
                'intents': [
                    {
                        'category': {'name': 'HID-10'},
                        'defaultOrder': NoKey('defaultOrder'),
                        'intents': [
                            {
                                'category': {'name': 'PRESENTED101'},
                                'defaultOrder': NoKey('defaultOrder'),
                                'intents': [
                                    {'category': {'name': 'REL1011'}, 'defaultOrder': 0},
                                ],
                            },
                        ],
                    },
                ],
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=prime&text=cat&hid=1021&rearr-factors=market_hid_relevance_threshold=0.4&rearr-factors=market_return_nids_in_intents=0'
        )
        self.assertFragmentIn(
            response,
            {
                # остался только один оффер из релевантной категории
                'search': {'total': 1, 'results': [{'entity': 'offer', 'categories': [{'id': 1021}]}]},
                'intents': [
                    {
                        'category': {'name': 'HID-10'},
                        'defaultOrder': NotEmpty(),
                        'intents': [
                            {
                                'category': {'name': 'PRESENTED102'},
                                'defaultOrder': NotEmpty(),
                                'intents': [{'category': {'name': 'IRR1021'}, 'defaultOrder': NotEmpty()}],
                            }
                        ],
                    },
                ],
            },
            allow_different_len=False,
        )

    def test_relevance_value(self):
        response = self.report.request_json('place=prime&text=cat&rearr-factors=turn_off_nid_intents_on_serp=0')
        self.assertFragmentIn(
            response,
            {
                'intents': [
                    {
                        'category': {'name': 'NID-10'},
                        'relevance': 0.1,
                        'intents': [
                            {
                                'category': {'name': 'PRESENTED101'},
                                'relevance': 0.1,
                                'intents': [
                                    {'category': {'name': 'REL1011'}, 'relevance': 0.5},
                                    {'category': {'name': 'IRR1012'}, 'relevance': 0.1},
                                ],
                            },
                            {
                                'category': {'name': 'PRESENTED102'},
                                'relevance': 0.1,
                                'intents': [
                                    {'category': {'name': 'IRR1021'}, 'relevance': 0.1},
                                    {'category': {'name': 'IRR1022'}, 'relevance': 0.1},
                                ],
                            },
                        ],
                    },
                    {
                        'category': {'name': 'SAVED'},
                        'relevance': 0.1,
                        'intents': [
                            {
                                'category': {'name': 'REL NON LIST'},
                                'relevance': 0.45,
                                'intents': [{'category': {'name': 'IRR1111'}, 'relevance': 0.1}],
                            },
                        ],
                    },
                    {
                        'category': {'name': 'REMOVED'},
                        'relevance': 0.1,
                        'intents': [
                            {
                                'category': {'name': 'NID-121'},
                                'relevance': 0.1,
                                'intents': [{'category': {'name': 'IRR1211'}, 'relevance': 0.1}],
                            },
                        ],
                    },
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_intents_pictures(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=201,
                name='Электроника',
                children=[
                    HyperCategory(
                        hid=202,
                        name='Телефоны',
                        children=[
                            HyperCategory(hid=203, name='Мобильные телефоны'),
                            HyperCategory(
                                hid=204,
                                name='Аксессуары для телефонов',
                                children=[
                                    HyperCategory(hid=205, name='Чехлы'),
                                    HyperCategory(hid=206, name='Защитные плёнки и стёкла'),
                                ],
                            ),
                            HyperCategory(
                                hid=207,
                                name='Запасные части',
                                children=[
                                    HyperCategory(hid=208, name='Корпусные детали', visual=True),
                                ],
                            ),
                        ],
                    ),
                    HyperCategory(
                        hid=209,
                        name='Компьютерная техника',
                        children=[
                            HyperCategory(
                                hid=210,
                                name='Программное обеспечение',
                                children=[
                                    HyperCategory(hid=211, name='Программы'),
                                ],
                            ),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.navtree += [
            NavCategory(
                nid=201,
                hid=201,
                name='Электроника',
                children=[
                    NavCategory(
                        nid=202,
                        hid=202,
                        name='Телефоны',
                        children=[
                            NavCategory(nid=203, hid=203, name='Мобильные телефоны'),
                            NavCategory(
                                nid=204,
                                hid=204,
                                name='Аксессуары для телефонов',
                                children=[
                                    NavCategory(nid=205, hid=205, name='Чехлы'),
                                    NavCategory(nid=206, hid=206, name='Защитные плёнки и стёкла'),
                                ],
                            ),
                            NavCategory(
                                nid=207,
                                hid=207,
                                name='Запасные части',
                                children=[
                                    NavCategory(nid=208, hid=208, name='Корпусные детали'),
                                ],
                            ),
                        ],
                    ),
                    NavCategory(
                        nid=209,
                        hid=209,
                        name='Компьютерная техника',
                        children=[
                            NavCategory(
                                nid=210,
                                hid=210,
                                name='Программное обеспечение',
                                children=[
                                    NavCategory(nid=211, hid=211, name='Программы'),
                                ],
                            ),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=1, priority_region=2),
        ]

        for ts in range(15, 40):
            cls.index.offers += [
                Offer(
                    title='электрон',
                    hid=203,
                    ts=ts,
                    fesh=1,
                    picture=Picture(
                        picture_id='Yie8Uh3j_{hid}{ts}_tU2HziP'.format(hid=203, ts=ts),
                        width=200,
                        height=200,
                        group_id=1234,
                    ),
                ),
            ]
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(0.35)

        cls.index.offers += [
            Offer(
                title='электрон',
                hid=203,
                ts=1,
                picture_flags=1,
                fesh=1,
                picture=Picture(picture_id='iyC4nHsl_20301_ygVAHeA', width=200, height=200, group_id=1234),
            ),
            Offer(
                title='гаджет',
                hid=203,
                ts=2,
                picture_flags=2,
                fesh=1,
                picture=Picture(picture_id='iyC4nHsl_20302_ygVAHeA', width=200, height=200, group_id=1234),
            ),
            Offer(
                title='электрон',
                hid=205,
                ts=3,
                picture_flags=3,
                fesh=1,
                picture=Picture(picture_id='iyC4nHsl_20503_ygVAHeA', width=200, height=200, group_id=1234),
            ),
            Offer(
                title='гаджет',
                hid=205,
                ts=4,
                picture_flags=4,
                fesh=1,
                picture=Picture(picture_id='iyC4nHsl_20504_ygVAHeA', width=200, height=200, group_id=1234),
            ),
            Offer(
                title='электрон',
                hid=206,
                ts=5,
                picture_flags=5,
                fesh=1,
                picture=Picture(picture_id='iyC4nHsl_20605_ygVAHeA', width=200, height=200, group_id=1234),
            ),
            Offer(
                title='гаджет',
                hid=206,
                ts=6,
                picture_flags=6,
                fesh=1,
                picture=Picture(picture_id='iyC4nHsl_20606_ygVAHeA', width=200, height=200, group_id=1234),
            ),
            Offer(hid=208, title='гаджет', vclusterid=1000000001, fesh=1),
            Offer(hid=208, title='гаджет', vclusterid=1000000002, fesh=1),
        ]

        cls.index.vclusters += [
            VCluster(
                title='электрон',
                hid=208,
                ts=7,
                vclusterid=1000000001,
                pictures=[
                    Picture(picture_id="uS6z5i755_807_Xx1CKyOQ", width=500, height=600, group_id=6789),
                ],
            ),
            VCluster(
                title='гаджет',
                hid=208,
                ts=8,
                vclusterid=1000000002,
                pictures=[
                    Picture(picture_id="uS6z5i755_808_Xx1CKyOQ", width=500, height=600, group_id=6790),
                ],
            ),
        ]

        cls.index.models += [
            Model(
                title='электрон',
                hid=211,
                ts=9,
                proto_picture=PictureMbo('//avatars.mds.yandex.net/get-mpic/9/img_21109/orig', width=500, height=600),
            ),
            Model(
                title='гаджет',
                hid=211,
                ts=10,
                proto_picture=PictureMbo('//avatars.mds.yandex.net/get-mpic/10/img_21110/orig', width=500, height=600),
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(1.0)

        for ts in (3, 5, 7, 9):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(0.4 + ts * 0.001)

        for ts in (4, 6, 8, 10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(0.6 + ts * 0.001)

        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 203).respond(89)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 211).respond(87)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 208).respond(85)
        for hid in (201, 202, 204, 205, 206, 207, 209, 210):
            cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, hid).respond(83)

    def test_intents_pictures(self):
        """
        Проверяем, что с нужным cgi-параметром в листовые интенты добавляются
        картинки самых релевантных документов (офферов, кластеров, моделей)
        из категории. С флагом market_use_found_pic_in_first_intent всё то же,
        но для первого интента документ выбирается из найденных по запросу
        """

        base = 'place=prime&text=электрон&rids=2'
        add_pics = '&additional_entities=intents_pictures&rearr-factors=no_pictures_in_hid_intents=0'
        # Пока что отключаем market_rearrange_intents_by_hids_on_text/textless,
        # т.к. в тестах используется ручное управление релевантностью категорий для интентов
        flag = (
            '&rearr-factors=market_use_found_pic_in_first_intent=1;market_return_nids_in_intents=0;'
            'market_rearrange_intents_by_hids_on_text=0;market_rearrange_intents_by_hids_on_textless=0'
        )
        # Сейчас везде включены нидовые интенты
        # У нидовых интентов другая логика отображения картинок
        # На это есть свои тесты
        hid_intents_flag = '&rearr-factors=market_return_nids_in_intents=0'

        # На все запросы (с параметром и без, с флагом и без) находятся интенты

        def check_picture(key, new_format=False):
            return {'original': {'url': Contains(key)}} if not new_format else {'original': {'key': Contains(key)}}

        for request in (base, base + add_pics, base + flag, base + add_pics + flag):

            response = self.report.request_json(request)

            self.assertFragmentIn(
                response,
                {
                    'intents': [
                        {
                            'category': {
                                'hid': 201,
                            },
                            'intents': [
                                {
                                    'category': {
                                        'hid': 202,
                                    },
                                    'intents': [
                                        {
                                            'category': {
                                                'hid': 203,
                                            },
                                            'intents': Absent(),
                                        },
                                        {
                                            'category': {
                                                'hid': 204,
                                            },
                                            'intents': [
                                                {
                                                    'category': {
                                                        'hid': 205,
                                                    },
                                                    'intents': Absent(),
                                                },
                                                {
                                                    'category': {
                                                        'hid': 206,
                                                    },
                                                    'intents': Absent(),
                                                },
                                            ],
                                        },
                                        {
                                            'category': {
                                                'hid': 207,
                                            },
                                            'intents': [
                                                {
                                                    'category': {
                                                        'hid': 208,
                                                    },
                                                    'intents': Absent(),
                                                }
                                            ],
                                        },
                                    ],
                                },
                                {
                                    'category': {
                                        'hid': 209,
                                    },
                                    'intents': [
                                        {
                                            'category': {
                                                'hid': 210,
                                            },
                                            'intents': [
                                                {
                                                    'category': {
                                                        'hid': 211,
                                                    },
                                                    'intents': Absent(),
                                                }
                                            ],
                                        }
                                    ],
                                },
                            ],
                        }
                    ]
                },
                allow_different_len=False,
            )

        # Без параметра &additional_entities=intents_pictures у интентов нет
        # картинок
        for hid in range(201, 211 + 1):
            for request in (base, base + flag):
                response = self.report.request_json(request)
                self.assertFragmentIn(
                    response,
                    {
                        'intents': [
                            {
                                'category': {
                                    'hid': hid,
                                },
                                'pictures': Absent(),
                            }
                        ]
                    },
                )

        # Для всех категорий, кроме самой релевантной из листовых (она будет
        # первым интентом, 203), с параметром и независимо от флага картинка
        # листовых интентов берётся из самого релевантного документа в категории
        # (не по запросу), в нелистовых картинок по-прежнему нет
        for new_pictures_format in ['', '&new-picture-format=1']:
            for request in [
                base + add_pics + hid_intents_flag + new_pictures_format,
                base + add_pics + flag + new_pictures_format,
            ]:
                response = self.report.request_json(request)

                def category_has_picture(hid, picture_key):
                    return {
                        'intents': [
                            {
                                'category': {'hid': hid},
                                'pictures': Absent()
                                if picture_key is None
                                else [check_picture(picture_key, new_format=new_pictures_format != '')],
                            }
                        ]
                    }

                self.assertFragmentIn(
                    response,
                    {
                        'search': {
                            'results': [
                                {
                                    'categories': [{'id': 203}],
                                    'pictures': [check_picture('_20301_', new_format=new_pictures_format != '')],
                                },
                                {
                                    'categories': [{'id': 206}],
                                    'pictures': [check_picture('_20605_', new_format=new_pictures_format != '')],
                                },
                                {
                                    'categories': [{'id': 205}],
                                    'pictures': [check_picture('_20503_', new_format=new_pictures_format != '')],
                                },
                                {
                                    'categories': [{'id': 208}],
                                    'pictures': [check_picture('_807_', new_format=new_pictures_format != '')],
                                },
                                {
                                    'categories': [{'id': 203}],
                                    'pictures': [check_picture('_203', new_format=new_pictures_format != '')],
                                },
                                {
                                    'categories': [{'id': 203}],
                                    'pictures': [check_picture('_203', new_format=new_pictures_format != '')],
                                },
                                {
                                    'categories': [{'id': 203}],
                                    'pictures': [check_picture('_203', new_format=new_pictures_format != '')],
                                },
                                {
                                    'categories': [{'id': 203}],
                                    'pictures': [check_picture('_203', new_format=new_pictures_format != '')],
                                },
                                {
                                    'categories': [{'id': 203}],
                                    'pictures': [check_picture('_203', new_format=new_pictures_format != '')],
                                },
                                {
                                    'categories': [{'id': 203}],
                                    'pictures': [check_picture('_203', new_format=new_pictures_format != '')],
                                },
                            ]
                        }
                    },
                    preserve_order=True,
                )

                self.assertFragmentIn(response, category_has_picture(201, '_20301_'))
                self.assertFragmentIn(response, category_has_picture(202, '_20301_'))
                self.assertFragmentIn(response, category_has_picture(203, '_20301_'))
                self.assertFragmentIn(response, category_has_picture(204, '_20605_'))
                self.assertFragmentIn(response, category_has_picture(205, '_20503_'))
                self.assertFragmentIn(response, category_has_picture(206, '_20605_'))
                self.assertFragmentIn(
                    response, category_has_picture(207, None)
                )  # нужно аггрегировать не просто лучши оффер а лучшй документ (оффер или модель)
                self.assertFragmentIn(
                    response, category_has_picture(208, None)
                )  # но для популярных категорий проблем не будет
                self.assertFragmentIn(response, category_has_picture(209, None))
                self.assertFragmentIn(response, category_has_picture(210, None))
                self.assertFragmentIn(response, category_has_picture(211, None))

    @classmethod
    def prepare_non_leaf_intents_pictures(cls):
        cls.index.hypertree += [HyperCategory(hid=301, name='Одежда', children=[HyperCategory(hid=302, name='Платья')])]

        cls.index.offers += [
            Offer(
                title='одежонка',
                hid=301,
                picture_flags=1,
                ts=11,
                picture=Picture(picture_id='iyC4nHsl_30101_ygVAHeA', width=200, height=200, group_id=1234),
            ),
            Offer(
                title='одежонка платьишко',
                hid=302,
                picture_flags=1,
                ts=12,
                picture=Picture(picture_id='iyC4nHsl_30201_ygVAHeA', width=200, height=200, group_id=1234),
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 11).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 12).respond(0.6)

    def test_non_leaf_intents_pictures(self):
        """
        Проверяем, что картинки прорастают в нелистовой интент, если у него
        есть собственный оффер
        """

        request = 'place=prime&text=одежонка&additional_entities=intents_pictures&rearr-factors=market_return_nids_in_intents=0;no_pictures_in_hid_intents=0'

        response = self.report.request_json(request)
        response_direct_snippet = self.report.request_json(request + DIRECT_SNIPPET_FLAG)
        self.check_intents_equal(response, response_direct_snippet)

        self.assertFragmentIn(
            response,
            {
                'intents': [
                    {
                        'category': {
                            'hid': 301,
                        },
                        'pictures': [{'original': {'url': Contains('_30101_')}}],
                        'intents': [
                            {
                                'category': {
                                    'hid': 302,
                                },
                                'pictures': [{'original': {'url': Contains('_30201_')}}],
                            }
                        ],
                    }
                ]
            },
        )

    @classmethod
    def prepare_many_intents(cls):
        for hid in range(401, 401 + 20):
            cls.index.hypertree += [HyperCategory(hid=hid)]

            cls.index.offers += [
                Offer(
                    title='крендель',
                    hid=hid,
                    picture_flags=1,
                    picture=Picture(picture_id='iyC4nHsl_%d01_ygVAHeA' % hid, width=200, height=200, group_id=1234),
                ),
            ]

            cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, hid).respond(1.0 - (hid - 400) / 25.0)

    @classmethod
    def prepare_adult_categories(cls):

        cls.index.hypertree += [
            HyperCategory(
                hid=91172, name='Средства гигиены', children=[HyperCategory(hid=13744375, name='Презервативы')]
            ),
            HyperCategory(hid=6091783, name='Интим-товары', children=[HyperCategory(hid=6290268, name='Вибраторы')]),
            HyperCategory(
                hid=16440100, name='Табак', children=[HyperCategory(hid=16440108, name='Системы нагревания')]
            ),
            HyperCategory(hid=16155476, name='Пиво'),
            HyperCategory(hid=11100111, name='Взрослые проблемы'),
        ]

        cls.index.navtree += [
            NavCategory(
                nid=91172,
                hid=91172,
                name='Средства гигиены',
                children=[NavCategory(nid=13744375, hid=13744375, name='Презервативы')],
            ),
            NavCategory(
                nid=6091783,
                hid=6091783,
                name='Интим-товары',
                children=[NavCategory(nid=6290268, hid=6290268, name='Вибраторы')],
            ),
            NavCategory(
                nid=16440100,
                hid=16440100,
                name='Табак',
                children=[NavCategory(nid=16440108, hid=16440108, name='Системы нагревания')],
            ),
            NavCategory(nid=16155476, hid=16155476, name='Пиво'),
            NavCategory(nid=11100111, hid=11100111, name='Взрослые проблемы'),
        ]

        cls.index.offers += [
            Offer(hid=13744375, title='презервативы для взрослых', adult=True),
            Offer(hid=6290268, title='вибраторы для взрослых', adult=True),
            Offer(hid=16440108, title='электронные сигареты для взрослых', adult=True),
            Offer(hid=16155476, title='пиво для взрослых', adult=True),
            Offer(hid=11100111, title='проблемы взрослых', adult=True),
        ]

    def test_adult_categories_in_intents(self):
        """Проверяем что в интентах в параметре kinkds появляются метки для взрослых категорий и алкоголя"""

        response = self.report.request_json('place=prime&text=для+взрослых&adult=1')
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {'category': {'name': 'Интим-товары', 'kinds': ["adult"]}},
                    {'category': {'name': 'Средства гигиены', 'kinds': []}},
                    {'category': {'name': 'Пиво', 'kinds': ["adult", "alco"]}},
                    {'category': {'name': 'Табак', 'kinds': ["adult"]}},
                    {'category': {'name': 'Взрослые проблемы', 'kinds': []}},
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

        # Проверяем, что в adult категориях фозвращаются картинки
        request = 'place=prime&text=для+взрослых&adult=1&additional_entities=intents_pictures&new-picture-format=1&rearr-factors=no_pictures_in_hid_intents=0'
        response = self.report.request_json(request)
        response_direct_snippet = self.report.request_json(request + DIRECT_SNIPPET_FLAG)
        self.check_intents_equal(response, response_direct_snippet)

        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {
                            "hid": 91172,
                            "name": "Средства гигиены",
                            "nid": 91172,
                        },
                        "intents": [
                            {
                                "category": {
                                    "hid": 13744375,
                                    "name": "Презервативы",
                                    "nid": 13744375,
                                },
                                "pictures": NotEmpty(),
                            }
                        ],
                    },
                    {
                        "category": {
                            "hid": 6091783,
                            "name": "Интим-товары",
                            "nid": 6091783,
                        },
                        "intents": [
                            {
                                "category": {
                                    "hid": 6290268,
                                    "name": "Вибраторы",
                                    "nid": 6290268,
                                },
                                "pictures": NotEmpty(),
                            }
                        ],
                    },
                    {
                        "category": {
                            "hid": 11100111,
                            "name": "Взрослые проблемы",
                            "nid": 11100111,
                        },
                        "pictures": NotEmpty(),
                    },
                    {
                        "category": {
                            "hid": 16155476,
                            "name": "Пиво",
                            "nid": 16155476,
                        },
                        "pictures": NotEmpty(),
                    },
                    {
                        "category": {
                            "hid": 16440100,
                            "name": "Табак",
                            "nid": 16440100,
                        },
                        "intents": [
                            {
                                "category": {
                                    "hid": 16440108,
                                    "name": "Системы нагревания",
                                    "nid": 16440108,
                                },
                                "pictures": NotEmpty(),
                            }
                        ],
                    },
                ],
            },
            allow_different_len=False,
            preserve_order=False,
        )

    @classmethod
    def prepare_white_nid_intents(cls):

        cls.index.hypertree += [
            HyperCategory(
                hid=1580,
                name='Темерия, Редания, Каэдвен, Аэдирн и другие',
                children=[
                    HyperCategory(hid=1581, name='Пехота'),
                    HyperCategory(hid=1582, name='Кавалерия'),
                    HyperCategory(hid=1583, name='Чародеи и чародейки'),
                    HyperCategory(hid=1584, name='Ведьмаки'),
                ],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 1580).respond(0.4)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 1581).respond(0.2)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 1582).respond(0.3)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 1583).respond(0.8)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 1584).respond(0.5)

        cls.index.navtree += [
            NavCategory(
                nid=1251,
                name='Гвинтокарты',
                children=[
                    NavCategory(
                        nid=1252,
                        hid=1580,
                        name='Королевства севера',
                        children=[
                            NavCategory(
                                nid=1352,
                                name='Редкие карты',
                                children=[
                                    NavCategory(nid=1452, hid=1581, name='Пехота Темерии', primary=True),
                                    NavCategory(nid=1462, hid=1582, name='Кавалерия Каэдвена'),
                                ],
                            ),
                            NavCategory(
                                nid=1353,
                                name='Легендарные карты',
                                children=[
                                    NavCategory(nid=1453, hid=1583, name='Чародейки', primary=True),
                                    NavCategory(nid=1463, hid=1584, name='Ведьмаки школы волка'),
                                ],
                            ),
                        ],
                    )
                ],
            ),
            NavCategory(
                nid=2251,
                name='TES',
                children=[
                    NavCategory(
                        nid=2252,
                        hid=2580,
                        name='Даэдра',
                        children=[
                            NavCategory(
                                nid=2352,
                                name='Боэтия',
                                hide_inner_nodes=True,
                                children=[
                                    NavCategory(
                                        nid=2452,
                                        hid=2581,
                                        name='Кинжалы Боэтии',
                                        primary=True,
                                        children=[NavCategory(nid=2454, hid=2584, name='Правые кинжалы', primary=True)],
                                    ),
                                ],
                            ),
                            NavCategory(
                                nid=2353,
                                name='Азура',
                                children=[
                                    NavCategory(nid=2453, hid=2582, name='Мечи Азуры', primary=True),
                                ],
                            ),
                        ],
                    ),
                    NavCategory(
                        nid=2253,
                        name='Аэдра',
                        hide_inner_nodes=True,
                        children=[
                            NavCategory(nid=2354, hid=2583, name='Аркей', primary=True),
                        ],
                    ),
                ],
            ),
            NavCategory(
                nid=3251,
                name='TES',
                children=[
                    NavCategory(
                        nid=3252,
                        hid=3580,
                        name='Даэдра',
                        children=[
                            NavCategory(
                                nid=3352,
                                name='Боэтия',
                                is_hidden=True,
                                children=[
                                    NavCategory(
                                        nid=3452,
                                        hid=3581,
                                        name='Кинжалы Боэтии',
                                        primary=True,
                                        children=[NavCategory(nid=3454, hid=3584, name='Правые кинжалы')],
                                    ),
                                ],
                            ),
                            NavCategory(
                                nid=3353,
                                name='Азура',
                                is_hidden_app=True,
                                children=[
                                    NavCategory(nid=3453, hid=3582, name='Мечи Азуры', primary=True),
                                ],
                            ),
                        ],
                    ),
                    NavCategory(
                        nid=3253,
                        name='Аэдра',
                        is_hidden_touch=True,
                        children=[
                            NavCategory(nid=3354, hid=3583, name='Аркей', primary=True),
                        ],
                    ),
                ],
            ),
            NavCategory(
                nid=4251,
                children=[
                    NavCategory(
                        nid=4252,
                        hid=4580,
                        children=[
                            NavCategory(
                                nid=4352,
                                children=[
                                    NavCategory(nid=4452, hid=4581, name='HID 4581 - 0', primary=True),
                                ],
                            ),
                            NavCategory(
                                nid=4353,
                                children=[
                                    NavCategory(nid=4453, hid=4581, name='HID 4581 - 1', primary=True),
                                ],
                            ),
                        ],
                    ),
                    NavCategory(
                        nid=4253,
                        children=[
                            NavCategory(nid=4354, hid=4581, name='HID 4581 - 2', primary=True),
                        ],
                    ),
                    NavCategory(
                        nid=4254,
                        children=[
                            NavCategory(nid=4356, hid=4582, name='HID 4582 - 0', primary=True, is_hidden=True),
                            NavCategory(nid=4357, hid=4582, name='HID 4582 - 1', primary=True),
                            NavCategory(nid=4358, hid=4582, name='HID 4582 - 2', primary=True, is_hidden=True),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=100500, hid=1580, title='Фольтест', ts=100500),  # nid=1251,1252
            Model(hyperid=100501, hid=1582, title='Бурая хоругвь', ts=100501),  # nid=1251,1252,1352,1462
            Model(hyperid=100502, hid=1583, title='Йеннифэр из Венгерберга', ts=100502),  # nid=1251,1252,1353,1453
            Model(hyperid=100503, hid=2584, ts=100503),
            Model(hyperid=100504, hid=2582, ts=100504),
            Model(hyperid=100505, hid=2583, ts=100505),
            Model(hyperid=100506, hid=3584, ts=100506),
            Model(hyperid=100507, hid=3582, ts=100507),
            Model(hyperid=100508, hid=3583, ts=100508),
            Model(hyperid=100509, hid=4581, ts=100509),
            Model(hyperid=100510, hid=4582, ts=1005010),
        ]

        cls.index.offers += [
            Offer(hid=1580, hyperid=100500, cpa=Offer.CPA_REAL, ts=1005000),  # nid=1251,1252
            Offer(hid=1582, hyperid=100501, ts=1005010),  # nid=1251,1252,1352,1462
            Offer(hid=1583, hyperid=100502, ts=1005020),  # nid=1251,1252,1353,1453
            Offer(
                hid=1584, auto_creating_model=False, title='Геральт из Ривии : Аард', ts=1005001
            ),  # nid=1251,1252,1353,1463
            Offer(hid=2584, hyperid=100503, ts=1005030),  # nid=2251,2252,2352,2452,2454
            Offer(hid=2582, hyperid=100504, ts=1005040),  # nid=2251,2252,2353,2453
            Offer(hid=2583, hyperid=100505, ts=1005050),  # nid=2251,2253,2354
            Offer(hid=3584, hyperid=100506, ts=1005060),  # nid=3251,3252,3352,3452,3454
            Offer(hid=3582, hyperid=100507, ts=1005070),  # nid=3251,3252,3353,3453
            Offer(hid=3583, hyperid=100508, ts=1005080),  # nid=3251,3253,3354
            Offer(hid=4581, hyperid=100509, ts=1005090),  # nid=4251,4252,4352,4452,4353,4453,4253,4354
            Offer(hid=4582, hyperid=100510, ts=1005100),  # nid=4251,4254,4356,4357
        ]

        for i in range(0, 11):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100500 + i).respond(0.3 + 0.01 + i)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1005000 + i * 10).respond(0.3 + 0.01 + i)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1005001).respond(0.325)

    def test_hid_relevance_in_nid_intents(self):
        """
        Проверяем, что под флагом hid_relevance_in_nid_intents в нидовых интентах в факторах используются их хиды
        У виртуальных хидом считается первый встреченный дочерний хид.
        Порядок (defaultOrder) для пупырей не проверяем, за это отвечает переранжирование "market_rearrange_intents_by_hids_on_text/textless=1",
        которое включено по умолчанию.
        """
        flags = '&rearr-factors=market_return_nids_in_intents=1;use_intents_mbo_hiding=1;market_uniq_nid_intents_by_hid=1;market_write_category_redirect_features=20;'
        request = 'place=prime&allow-collapsing=1&nid=1252&debug=da'
        response = self.report.request_json(request + flags + '&rearr-factors=hid_relevance_in_nid_intents=0')
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {"hid": 0, "nid": 1251, "name": "Гвинтокарты"},
                        "intents": [
                            {
                                "category": {"hid": 1580, "nid": 1252, "name": "Королевства севера"},
                                "intents": [
                                    {
                                        "category": {"hid": 0, "nid": 1353, "name": "Легендарные карты"},
                                        "intents": [
                                            {"category": {"hid": 1583, "nid": 1453, "name": "Чародейки"}},
                                            {"category": {"hid": 1584, "nid": 1463, "name": "Ведьмаки школы волка"}},
                                        ],
                                    },
                                    {
                                        "category": {"hid": 0, "nid": 1352, "name": "Редкие карты"},
                                        "intents": [
                                            {"category": {"hid": 1582, "nid": 1462, "name": "Кавалерия Каэдвена"}}
                                        ],
                                    },
                                ],
                            }
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'categories_ranking_json': [
                        {
                            'id': 1582,
                            'factors': {'MATRIXNET_VALUE_Q25': NotEmpty(), 'POSITION_OF_FIRST_DOCUMENT_IN_TOP': 2},
                        },
                        {
                            'id': 1583,
                            'factors': {'MATRIXNET_VALUE_Q25': NotEmpty(), 'POSITION_OF_FIRST_DOCUMENT_IN_TOP': 1},
                        },
                        {
                            'id': 1584,
                            'factors': {'MATRIXNET_VALUE_Q25': NotEmpty(), 'POSITION_OF_FIRST_DOCUMENT_IN_TOP': 3},
                        },
                    ],
                    'navnodes_ranking_json': [
                        {
                            'id': 1353,
                            'factors': {
                                'MATRIXNET_VALUE_Q25': Absent(),
                                'POSITION_OF_FIRST_DOCUMENT_IN_TOP': Greater(10005000),
                            },
                        },
                        {
                            'id': 1462,
                            'factors': {
                                'MATRIXNET_VALUE_Q25': Absent(),
                                'POSITION_OF_FIRST_DOCUMENT_IN_TOP': Greater(10005000),
                            },
                        },
                    ],
                }
            },
        )

        # Под флагом hid_relevance_in_nid_intents факторы начинают считаться с учетом хида
        response = self.report.request_json(request + flags + '&rearr-factors=hid_relevance_in_nid_intents=1')
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'categories_ranking_json': [
                        {
                            'id': 1582,
                            'factors': {'MATRIXNET_VALUE_Q25': NotEmpty(), 'POSITION_OF_FIRST_DOCUMENT_IN_TOP': 2},
                        },
                        {
                            'id': 1583,
                            'factors': {'MATRIXNET_VALUE_Q25': NotEmpty(), 'POSITION_OF_FIRST_DOCUMENT_IN_TOP': 1},
                        },
                        {
                            'id': 1584,
                            'factors': {'MATRIXNET_VALUE_Q25': NotEmpty(), 'POSITION_OF_FIRST_DOCUMENT_IN_TOP': 3},
                        },
                    ],
                    'navnodes_ranking_json': [
                        {
                            'id': 1353,
                            'factors': {'MATRIXNET_VALUE_Q25': NotEmpty(), 'POSITION_OF_FIRST_DOCUMENT_IN_TOP': 1},
                        },  # hid=1583
                        {
                            'id': 1462,
                            'factors': {'MATRIXNET_VALUE_Q25': NotEmpty(), 'POSITION_OF_FIRST_DOCUMENT_IN_TOP': 2},
                        },  # hid=1582
                    ],
                }
            },
        )

    def test_fast_dssm_calculation(self):
        """Выкатился расчет факторов без фактического применения дссм
        Также выкатили расчет факторв DSSM_CATEGORY_HARD и DSSM_IS_STUPID"""

        flags = '&rearr-factors=market_write_category_redirect_features=20;'
        request = 'place=prime&allow-collapsing=1&nid=1252&debug=da'

        # В быстром варианте модели умножаются скалярным произведением и получается
        response = self.report.request_json(request + flags)
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'categories_ranking_json': [
                        {
                            'id': 1582,
                            'factors': {
                                'DSSM_BY_QUERIES': Round(0.75),
                                # 'DSSM_BY_TITLES': NotEmpty(),
                                'DSSM_BY_UNIQNAME': Round(0.666),
                                'DSSM_BY_UNIQNAME_BLUE': Round(0.666),
                                # 'DSSM_CATEGORY_HARD': Absent(),
                                'DSSM_IS_STUPID': NotEmpty(),
                            },
                        }
                    ]
                }
            },
        )

        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'navnodes_ranking_json': [
                        {
                            'id': 1463,
                            'factors': {
                                'DSSM_BY_QUERIES': Round(0.75),
                                # 'DSSM_BY_TITLES': NotEmpty(), # не загружается в lite
                                'DSSM_BY_UNIQNAME': Round(0.666),
                                'DSSM_BY_UNIQNAME_BLUE': Round(0.666),
                                # 'DSSM_CATEGORY_HARD': NotEmpty(), # не загружается в lite
                                'DSSM_IS_STUPID': NotEmpty(),
                            },
                        }
                    ]
                }
            },
        )

    def test_white_nid_intents_positive(self):
        """
        Под флагом market_return_nids_in_intents=1 в intents возвращаем ниды
        """

        def check_big_response(response):
            self.assertFragmentIn(
                response,
                {
                    "intents": [
                        {
                            "category": {
                                "hid": 0,
                                "nid": 1251,
                                "name": "Гвинтокарты",
                            },
                            "intents": [
                                {
                                    "category": {
                                        "hid": 1580,
                                        "nid": 1252,
                                        "name": "Королевства севера",
                                    },
                                    "intents": [
                                        {
                                            "category": {
                                                "hid": 0,
                                                "nid": 1353,
                                                "name": "Легендарные карты",
                                            },
                                            "intents": [
                                                {
                                                    "category": {
                                                        "hid": 1583,
                                                        "nid": 1453,
                                                        "name": "Чародейки",
                                                    },
                                                },
                                                {
                                                    "category": {
                                                        "hid": 1584,
                                                        "nid": 1463,
                                                        "name": "Ведьмаки школы волка",
                                                    },
                                                },
                                            ],
                                        },
                                        {
                                            "category": {
                                                "hid": 0,
                                                "nid": 1352,
                                                "name": "Редкие карты",
                                            },
                                            "intents": [
                                                {
                                                    "category": {
                                                        "hid": 1582,
                                                        "nid": 1462,
                                                        "name": "Кавалерия Каэдвена",
                                                    },
                                                }
                                            ],
                                        },
                                    ],
                                }
                            ],
                        }
                    ],
                },
                allow_different_len=False,
            )

        def check_small_response(response):
            self.assertFragmentIn(
                response,
                {
                    "intents": [
                        {
                            "category": {
                                "hid": 0,
                                "nid": 1251,
                                "name": "Гвинтокарты",
                            },
                            "intents": [
                                {
                                    "category": {
                                        "hid": 1580,
                                        "nid": 1252,
                                        "name": "Королевства севера",
                                    },
                                    "intents": [
                                        {
                                            "category": {
                                                "hid": 0,
                                                "nid": 1353,
                                                "name": "Легендарные карты",
                                            },
                                            "intents": [
                                                {
                                                    "category": {
                                                        "hid": 1583,
                                                        "nid": 1453,
                                                        "name": "Чародейки",
                                                    },
                                                },
                                            ],
                                        }
                                    ],
                                }
                            ],
                        }
                    ],
                },
                allow_different_len=False,
            )

        request = 'place=prime&allow-collapsing=1&hid=1580&nid=1252&rearr-factors=market_return_nids_in_intents=1'
        response = self.report.request_json(request)
        check_big_response(response)

        # Если запросим только nid, ответ не должен отличаться
        request = 'place=prime&allow-collapsing=1&nid=1252&rearr-factors=market_return_nids_in_intents=1'
        response = self.report.request_json(request)
        check_big_response(response)

        request = 'place=prime&allow-collapsing=1&hid=1583&nid=1453&rearr-factors=market_return_nids_in_intents=1'
        response = self.report.request_json(request)
        check_small_response(response)

        # Если запросим только nid, ответ не должен отличаться
        request = 'place=prime&allow-collapsing=1&nid=1453&rearr-factors=market_return_nids_in_intents=1'
        response = self.report.request_json(request)
        check_small_response(response)

    def test_white_nid_intents_negative(self):
        """
        Без флага market_return_nids_in_intents в intents только хиды
        На синем флаг market_return_nids_in_intents=0 тоже возвращает хидовые интенты
        """

        # При market_return_nids_in_intents=0
        request = 'place=prime&allow-collapsing=1&hid=1580&nid=1252&rearr-factors=market_return_nids_in_intents=0'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {
                            "hid": 1580,
                            "nid": 1252,
                            "name": "Темерия, Редания, Каэдвен, Аэдирн и другие",
                        },
                        "intents": [
                            {
                                "category": {
                                    "hid": 1582,
                                    "nid": 1462,
                                    "name": "Кавалерия",
                                },
                            },
                            {
                                "category": {
                                    "hid": 1583,
                                    "nid": 1453,
                                    "name": "Чародеи и чародейки",
                                },
                            },
                            {
                                "category": {
                                    "hid": 1584,
                                    "nid": 1463,
                                    "name": "Ведьмаки",
                                },
                            },
                        ],
                    }
                ],
            },
            allow_different_len=False,
        )

    def test_nid_intents_pictures_on_white(self):
        """
        Проверяем, что возвращаем картинки самых релевантных доков во всех интентах
        (только с &additional_entities=intents_pictures)
        """

        def check_pictures_rendering(request, need_pictures):
            response = self.report.request_json(request)
            pictures_resp = NotEmpty() if need_pictures else Absent()
            self.assertFragmentIn(
                response,
                {
                    "intents": [
                        {
                            "category": {
                                "hid": 0,
                                "nid": 1251,
                                "name": "Гвинтокарты",
                            },
                            "pictures": pictures_resp,
                            "intents": [
                                {
                                    "category": {
                                        "hid": 1580,
                                        "nid": 1252,
                                        "name": "Королевства севера",
                                    },
                                    "pictures": pictures_resp,
                                    "intents": [
                                        {
                                            "category": {
                                                "hid": 0,
                                                "nid": 1353,
                                                "name": "Легендарные карты",
                                            },
                                            "pictures": pictures_resp,
                                            "intents": [
                                                {
                                                    "category": {
                                                        "hid": 1583,
                                                        "nid": 1453,
                                                        "name": "Чародейки",
                                                    },
                                                    "pictures": pictures_resp,
                                                },
                                            ],
                                        }
                                    ],
                                }
                            ],
                        }
                    ],
                },
                allow_different_len=False,
            )

        request = 'place=prime&allow-collapsing=1&nid=1453&additional_entities=intents_pictures&rearr-factors=market_return_nids_in_intents=1'
        check_pictures_rendering(request, need_pictures=True)
        check_pictures_rendering(request + DIRECT_SNIPPET_FLAG, need_pictures=True)

        # Тот же функционал должен работать под флагом nid_intents_pictures=1
        request = 'place=prime&allow-collapsing=1&nid=1453&rearr-factors=market_return_nids_in_intents=1;nid_intents_pictures=1'
        check_pictures_rendering(request, need_pictures=True)
        check_pictures_rendering(request + DIRECT_SNIPPET_FLAG, need_pictures=True)

        # Без &additional_entities=intents_pictures картинок не будет
        request = 'place=prime&allow-collapsing=1&nid=1453&rearr-factors=market_return_nids_in_intents=1'
        check_pictures_rendering(request, need_pictures=False)
        check_pictures_rendering(request + DIRECT_SNIPPET_FLAG, need_pictures=False)

    def test_hide_inner_nodes(self):
        """
        У нод с флагом hide_inner_nodes=True не должны присутствовать дети в интентах
        Работает под флагами use_intents_mbo_hiding + market_return_nids_in_intents
        """

        # У ноды с нидами : {2352, 2253} присутствует флаг hide_inner_nodes, их детей не должно быть в выдаче
        request = 'place=prime&allow-collapsing=1&nid=2251&rearr-factors=market_return_nids_in_intents=1;use_intents_mbo_hiding=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {
                            "hid": 0,
                            "nid": 2251,
                            "name": "TES",
                        },
                        "intents": [
                            {
                                "category": {
                                    "hid": 2580,
                                    "nid": 2252,
                                    "name": "Даэдра",
                                },
                                "intents": [
                                    {
                                        "category": {"hid": 0, "nid": 2352, "name": "Боэтия", "isLeaf": True},
                                        "intents": Absent(),  # hide_inner_nodes == True
                                    },
                                    {
                                        "category": {
                                            "hid": 0,
                                            "nid": 2353,
                                            "name": "Азура",
                                        },
                                        "intents": [
                                            {
                                                "category": {
                                                    "hid": 2582,
                                                    "nid": 2453,
                                                    "name": "Мечи Азуры",
                                                },
                                                "intents": Absent(),
                                            }
                                        ],
                                    },
                                ],
                            },
                            {
                                "category": {"hid": 0, "nid": 2253, "name": "Аэдра", "isLeaf": True},
                                "intents": Absent(),  # hide_inner_nodes == True
                            },
                        ],
                    }
                ],
            },
            allow_different_len=False,
        )

        # Но, на пути к запрошенной ноде такие скрытия не должны учитываться
        # У 2452 родитель(2352) имеет флажок hide_inner_nodes
        # При запросе 2452 или 2454 это должно игнорироваться
        right_ans = {
            "intents": [
                {
                    "category": {
                        "hid": 0,
                        "nid": 2251,
                        "name": "TES",
                    },
                    "intents": [
                        {
                            "category": {
                                "hid": 2580,
                                "nid": 2252,
                                "name": "Даэдра",
                            },
                            "intents": [
                                {
                                    "category": {"hid": 0, "nid": 2352, "name": "Боэтия", "isLeaf": False},
                                    "intents": [
                                        {
                                            # Не скрываем
                                            "category": {
                                                "hid": 2581,
                                                "nid": 2452,
                                                "name": "Кинжалы Боэтии",
                                            },
                                            "intents": [
                                                {
                                                    "category": {
                                                        "hid": 2584,
                                                        "nid": 2454,
                                                        "name": "Правые кинжалы",
                                                    },
                                                    "intents": Absent(),
                                                }
                                            ],
                                        },
                                    ],
                                }
                            ],
                        }
                    ],
                }
            ],
        }
        for nid in [2452, 2454]:
            request = 'place=prime&allow-collapsing=1&nid={}&rearr-factors=nid_search=1;market_return_nids_in_intents=1;use_intents_mbo_hiding=1'.format(
                nid
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(response, right_ans, allow_different_len=False)

        # Без флага use_intents_mbo_hiding это не должно работать
        request = 'place=prime&allow-collapsing=1&nid=2251&rearr-factors=market_return_nids_in_intents=1;use_intents_mbo_hiding=0'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {
                            "hid": 0,
                            "nid": 2251,
                            "name": "TES",
                        },
                        "intents": [
                            {
                                "category": {
                                    "hid": 2580,
                                    "nid": 2252,
                                    "name": "Даэдра",
                                },
                                "intents": [
                                    {
                                        "category": {
                                            "hid": 0,
                                            "nid": 2352,
                                            "name": "Боэтия",
                                        },
                                        "intents": [
                                            {
                                                "category": {"hid": 2581, "nid": 2452},
                                                "intents": [
                                                    {"category": {"hid": 2584, "nid": 2454}, "intent": Absent()}
                                                ],
                                            }
                                        ],
                                    },
                                    {
                                        "category": {
                                            "hid": 0,
                                            "nid": 2353,
                                            "name": "Азура",
                                        },
                                        "intents": [
                                            {
                                                "category": {
                                                    "hid": 2582,
                                                    "nid": 2453,
                                                    "name": "Мечи Азуры",
                                                },
                                                "intents": Absent(),
                                            }
                                        ],
                                    },
                                ],
                            },
                            {
                                "category": {
                                    "hid": 0,
                                    "nid": 2253,
                                    "name": "Аэдра",
                                },
                                "intents": [
                                    {
                                        "category": {
                                            "hid": 2583,
                                            "nid": 2354,
                                        },
                                        "intents": Absent(),
                                    }
                                ],
                            },
                        ],
                    }
                ],
            },
            allow_different_len=False,
        )

    def test_hidden_nodes(self):
        """
        Под флагом market_return_nids_in_intents=1 в интентах выводится нидовое дерево
        А под флагом use_intents_mbo_hiding=1
        в интентах работают флаги скрытия is_hidden, is_hidden_app, is_hidden_touch
        Но! Мы не должны скрывать путь до запрошенной ноды. То есть все родители нида в запросе не должны быть скрыты

        Скрываем по той же логике, что каталогер https://a.yandex-team.ru/arc/trunk/arcadia/market/cataloger/src/navigation/navigation_info.cpp?rev=r8330959#L93-105
        """

        # Будет скрыта нода 3352 (is_hidden == True)
        # Обычный запрос без конкретной платформы
        rearr = '&rearr-factors=market_return_nids_in_intents=1;use_intents_mbo_hiding=1'
        request = 'place=prime&allow-collapsing=1&nid=3251' + rearr
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {
                            "hid": 0,
                            "nid": 3251,
                            "name": "TES",
                        },
                        "intents": [
                            {
                                "category": {
                                    "hid": 3580,
                                    "nid": 3252,
                                    "name": "Даэдра",
                                },
                                "intents": [
                                    {
                                        "category": {
                                            "hid": 0,
                                            "nid": 3353,
                                            "name": "Азура",
                                        },
                                        "intents": [
                                            {
                                                "category": {
                                                    "hid": 3582,
                                                    "nid": 3453,
                                                    "name": "Мечи Азуры",
                                                },
                                                "intents": Absent(),
                                            }
                                        ],
                                    }
                                ],
                            },
                            {
                                "category": {
                                    "hid": 0,
                                    "nid": 3253,
                                    "name": "Аэдра",
                                },
                                "intents": [
                                    {
                                        "category": {
                                            "hid": 3583,
                                            "nid": 3354,
                                            "name": "Аркей",
                                        },
                                        "intents": Absent(),
                                    }
                                ],
                            },
                        ],
                    }
                ],
            },
            allow_different_len=False,
        )

        # Будет скрыта нода 3253 (is_hidden_touch == True)
        # Запрос для тача

        request = 'place=prime&allow-collapsing=1&nid=3251&platform=touch' + rearr
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {
                            "hid": 0,
                            "nid": 3251,
                            "name": "TES",
                        },
                        "intents": [
                            {
                                "category": {
                                    "hid": 3580,
                                    "nid": 3252,
                                    "name": "Даэдра",
                                },
                                "intents": [
                                    {
                                        "category": {
                                            "hid": 0,
                                            "nid": 3352,
                                            "name": "Боэтия",
                                        },
                                        "intents": [
                                            {
                                                "category": {
                                                    "hid": 3581,
                                                    "nid": 3452,
                                                    "name": "Кинжалы Боэтии",
                                                },
                                                "intents": [
                                                    {
                                                        "category": {
                                                            "hid": 3584,
                                                            "nid": 3454,
                                                            "name": "Правые кинжалы",
                                                        },
                                                        "intents": Absent(),
                                                    }
                                                ],
                                            }
                                        ],
                                    },
                                    {
                                        "category": {
                                            "hid": 0,
                                            "nid": 3353,
                                            "name": "Азура",
                                        },
                                        "intents": [
                                            {
                                                "category": {
                                                    "hid": 3582,
                                                    "nid": 3453,
                                                    "name": "Мечи Азуры",
                                                },
                                                "intents": Absent(),
                                            }
                                        ],
                                    },
                                ],
                            }
                        ],
                    }
                ],
            },
            allow_different_len=False,
        )

        # Будет скрыта нода 3353 (is_hidden_app == True)
        # Запрос для приложения
        for client in ['IOS', 'ANDROID']:
            request = 'place=prime&allow-collapsing=1&nid=3251&client={}'.format(client) + rearr
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "intents": [
                        {
                            "category": {
                                "hid": 0,
                                "nid": 3251,
                                "name": "TES",
                            },
                            "intents": [
                                {
                                    "category": {
                                        "hid": 3580,
                                        "nid": 3252,
                                        "name": "Даэдра",
                                    },
                                    "intents": [
                                        {
                                            "category": {
                                                "hid": 0,
                                                "nid": 3352,
                                                "name": "Боэтия",
                                            },
                                            "intents": [
                                                {
                                                    "category": {
                                                        "hid": 3581,
                                                        "nid": 3452,
                                                        "name": "Кинжалы Боэтии",
                                                    },
                                                    "intents": [
                                                        {
                                                            "category": {
                                                                "hid": 3584,
                                                                "nid": 3454,
                                                                "name": "Правые кинжалы",
                                                            },
                                                            "intents": Absent(),
                                                        }
                                                    ],
                                                }
                                            ],
                                        }
                                    ],
                                },
                                {
                                    "category": {
                                        "hid": 0,
                                        "nid": 3253,
                                        "name": "Аэдра",
                                    },
                                    "intents": [
                                        {
                                            "category": {
                                                "hid": 3583,
                                                "nid": 3354,
                                                "name": "Аркей",
                                            },
                                            "intents": Absent(),
                                        }
                                    ],
                                },
                            ],
                        }
                    ],
                },
                allow_different_len=False,
            )

        # Запрашиваем 3352, 3452 и 3454 в путях до них есть скрытая 3352
        # Нода 3352 не должна быть скрыта
        # А весь путь до 3454 не должен быть скрыт, тк больше нет скрытых нод
        for nid in [3352, 3452, 3454]:
            request = 'place=prime&allow-collapsing=1&nid={}'.format(nid) + rearr
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "intents": [
                        {
                            "category": {
                                "hid": 0,
                                "nid": 3251,
                                "name": "TES",
                            },
                            "intents": [
                                {
                                    "category": {
                                        "hid": 3580,
                                        "nid": 3252,
                                        "name": "Даэдра",
                                    },
                                    "intents": [
                                        {
                                            "category": {
                                                "hid": 0,
                                                "nid": 3352,
                                                "name": "Боэтия",
                                            },
                                            "intents": [
                                                {
                                                    "category": {
                                                        "hid": 3581,
                                                        "nid": 3452,
                                                        "name": "Кинжалы Боэтии",
                                                    },
                                                    "intents": [
                                                        {
                                                            "category": {
                                                                "hid": 3584,
                                                                "nid": 3454,
                                                                "name": "Правые кинжалы",
                                                            },
                                                            "intents": Absent(),
                                                        }
                                                    ],
                                                }
                                            ],
                                        }
                                    ],
                                }
                            ],
                        }
                    ],
                },
                allow_different_len=False,
            )

            # Без флага use_intents_mbo_hiding скрытия не должны работать
            response = self.report.request_json(
                'place=prime&allow-collapsing=1&nid=3251&rearr-factors=market_return_nids_in_intents=1;use_intents_mbo_hiding=0'
            )
            self.assertFragmentIn(
                response,
                {
                    "intents": [
                        {
                            "category": {
                                "hid": 0,
                                "nid": 3251,
                                "name": "TES",
                            },
                            "intents": [
                                {
                                    "category": {
                                        "hid": 3580,
                                        "nid": 3252,
                                        "name": "Даэдра",
                                    },
                                    "intents": [
                                        {
                                            "category": {
                                                "hid": 0,
                                                "nid": 3352,
                                                "name": "Боэтия",
                                            },
                                            "intents": [
                                                {
                                                    "category": {
                                                        "hid": 3581,
                                                        "nid": 3452,
                                                        "name": "Кинжалы Боэтии",
                                                    },
                                                    "intents": [
                                                        {
                                                            "category": {
                                                                "hid": 3584,
                                                                "nid": 3454,
                                                                "name": "Правые кинжалы",
                                                            },
                                                            "intents": Absent(),
                                                        }
                                                    ],
                                                }
                                            ],
                                        },
                                        {
                                            "category": {
                                                "hid": 0,
                                                "nid": 3353,
                                                "name": "Азура",
                                            },
                                            "intents": [
                                                {
                                                    "category": {
                                                        "hid": 3582,
                                                        "nid": 3453,
                                                        "name": "Мечи Азуры",
                                                    },
                                                    "intents": Absent(),
                                                }
                                            ],
                                        },
                                    ],
                                },
                                {
                                    "category": {
                                        "hid": 0,
                                        "nid": 3253,
                                        "name": "Аэдра",
                                    },
                                    "intents": [
                                        {
                                            "category": {
                                                "hid": 3583,
                                                "nid": 3354,
                                                "name": "Аркей",
                                            },
                                            "intents": Absent(),
                                        }
                                    ],
                                },
                            ],
                        }
                    ],
                },
                allow_different_len=False,
            )

    def test_unique_intents(self):
        """
        В навигационном дереве может присутствовать несколько нод с одинаковым hid-ом
        Не нужно возвращать в интентах дубликаты (не распространяется на рецепты)
        связка флагов market_return_nids_in_intents=1 и market_uniq_nid_intents_by_hid=1
        """

        # флаг use_intents_mbo_hiding добавлен, тк проверяется в связке со скрытиями
        rearr = (
            '&rearr-factors=market_return_nids_in_intents=1;market_uniq_nid_intents_by_hid=1;use_intents_mbo_hiding=1'
        )
        request = 'place=prime&allow-collapsing=1&nid=4251' + rearr
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {
                            "hid": 0,
                            "nid": 4251,
                        },
                        "intents": [
                            {
                                "category": {
                                    "hid": 4580,
                                    "nid": 4252,
                                },
                                "intents": [
                                    {
                                        "category": {
                                            "hid": 0,
                                            "nid": 4352,
                                        },
                                        "intents": Absent(),  # тк 4452 дубликат ноды 4354
                                    },
                                    {
                                        "category": {
                                            "hid": 0,
                                            "nid": 4353,
                                        },
                                        "intents": Absent(),  # тк 4453 дубликат ноды 4354
                                    },
                                ],
                            },
                            {
                                "category": {
                                    "hid": 0,
                                    "nid": 4253,
                                },
                                "intents": [
                                    {
                                        "category": {"hid": 4581, "nid": 4354, "name": "HID 4581 - 2"},
                                        "intents": Absent(),
                                    }
                                ],
                            },
                            {
                                "category": {
                                    "hid": 0,
                                    "nid": 4254,
                                },
                                "intents": [
                                    {
                                        # 4356, 4357, 4358 имеют одинаковый хид 4582
                                        # + 4356 и 4358 скрыты
                                        "category": {"hid": 4582, "nid": 4357, "name": "HID 4582 - 1"},
                                        "intents": Absent(),
                                    }
                                ],
                            },
                        ],
                    }
                ],
            },
            allow_different_len=False,
        )

        # Без флага market_uniq_nid_intents_by_hid работать не должно
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&nid=4251&rearr-factors=market_return_nids_in_intents=1;use_intents_mbo_hiding=1;market_uniq_nid_intents_by_hid=0'
        )
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {
                            "hid": 0,
                            "nid": 4251,
                        },
                        "intents": [
                            {
                                "category": {
                                    "hid": 4580,
                                    "nid": 4252,
                                },
                                "intents": [
                                    {
                                        "category": {
                                            "hid": 0,
                                            "nid": 4352,
                                        },
                                        "intents": [
                                            {
                                                "category": {"hid": 4581, "nid": 4452, "name": "HID 4581 - 0"},
                                                "intents": Absent(),
                                            }
                                        ],
                                    },
                                    {
                                        "category": {
                                            "hid": 0,
                                            "nid": 4353,
                                        },
                                        "intents": [
                                            {
                                                "category": {"hid": 4581, "nid": 4453, "name": "HID 4581 - 1"},
                                                "intents": Absent(),
                                            }
                                        ],
                                    },
                                ],
                            },
                            {
                                "category": {
                                    "hid": 0,
                                    "nid": 4253,
                                },
                                "intents": [
                                    {
                                        "category": {"hid": 4581, "nid": 4354, "name": "HID 4581 - 2"},
                                        "intents": Absent(),
                                    }
                                ],
                            },
                            {
                                "category": {
                                    "hid": 0,
                                    "nid": 4254,
                                },
                                "intents": [
                                    {
                                        # 4356, 4357, 4358 имеют одинаковый хид 4582
                                        # + 4356 и 4358 скрыты
                                        "category": {"hid": 4582, "nid": 4357, "name": "HID 4582 - 1"},
                                        "intents": Absent(),
                                    }
                                ],
                            },
                        ],
                    }
                ],
            },
            allow_different_len=False,
        )

    def test_blue_intents(self):
        """На синем интенты по умолчанию строятся по навигационному дереву"""

        request = (
            'place=prime&allow-collapsing=1&hid=1580&nid=1252&rgb=blue&additional_entities=intents_pictures'
            '&rearr-factors=market_white_cpa_on_blue=2'
        )
        response = self.report.request_json(request)
        response_direct_snippet = self.report.request_json(request + DIRECT_SNIPPET_FLAG)
        self.check_intents_equal(response, response_direct_snippet)

        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {
                            "name": "Гвинтокарты",
                            "hid": 0,
                            "nid": 1251,
                        },
                        "pictures": NotEmptyList(),
                        "intents": [
                            {
                                "category": {
                                    "name": "Королевства севера",
                                    "hid": 1580,
                                    "nid": 1252,
                                },
                                "pictures": NotEmptyList(),
                            }
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

        # если просто выключить интенты на синем - у них не будет картинок
        request = (
            'place=prime&allow-collapsing=1&hid=1580&nid=1252&rgb=blue&additional_entities=intents_pictures'
            '&rearr-factors=market_white_cpa_on_blue=2;market_return_nids_in_intents=0'
        )

        response = self.report.request_json(request)
        response_direct_snippet = self.report.request_json(request + DIRECT_SNIPPET_FLAG)
        self.check_intents_equal(response, response_direct_snippet)

        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {"name": "Темерия, Редания, Каэдвен, Аэдирн и другие", "hid": 1580, "nid": 1252},
                        "pictures": NotEmptyList(),
                    }
                ]
            },
            allow_different_len=False,
        )

        # переключаем интенты на белый - все работает как на белом (с картинками)
        request = (
            'place=prime&allow-collapsing=1&hid=1580&nid=1252&rgb=blue&additional_entities=intents_pictures'
            '&rearr-factors=market_white_cpa_on_blue=2;market_force_white_on=27;market_return_nids_in_intents=0'
        )

        response = self.report.request_json(request)
        response_direct_snippet = self.report.request_json(request + DIRECT_SNIPPET_FLAG)
        self.check_intents_equal(response, response_direct_snippet)

        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {"name": "Темерия, Редания, Каэдвен, Аэдирн и другие", "hid": 1580, "nid": 1252},
                        "pictures": NotEmptyList(),
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_use_nid_for_search_intents(self):
        """
        Параметр use-nid-for-search прсдставляет собой аналог для market_return_nids_in_intents=1 + market_uniq_nid_intents_by_hid=1 + use_intents_mbo_hiding=1
        """

        # проверяем поведение при market_return_nids_in_intents=1, даже если сам флаг равен 0
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&hid=1583&nid=1453&use-nid-for-search=1'
            '&rearr-factors=market_return_nids_in_intents=0'
        )
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {
                            "hid": 0,
                            "nid": 1251,
                            "name": "Гвинтокарты",
                        },
                        "intents": [
                            {
                                "category": {
                                    "hid": 1580,
                                    "nid": 1252,
                                    "name": "Королевства севера",
                                },
                                "intents": [
                                    {
                                        "category": {
                                            "hid": 0,
                                            "nid": 1353,
                                            "name": "Легендарные карты",
                                        },
                                        "intents": [
                                            {
                                                "category": {
                                                    "hid": 1583,
                                                    "nid": 1453,
                                                    "name": "Чародейки",
                                                },
                                            },
                                        ],
                                    }
                                ],
                            }
                        ],
                    }
                ],
            },
            allow_different_len=False,
        )

        # проверяем поведение при market_uniq_nid_intents_by_hid=1, даже если сам флаг равен 0
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&nid=4251&use-nid-for-search=1'
            '&rearr-factors=market_return_nids_in_intents=0;market_uniq_nid_intents_by_hid=0'
        )
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {
                            "hid": 0,
                            "nid": 4251,
                        },
                        "intents": [
                            {
                                "category": {
                                    "hid": 4580,
                                    "nid": 4252,
                                },
                                "intents": [
                                    {
                                        "category": {
                                            "hid": 0,
                                            "nid": 4352,
                                        },
                                        "intents": Absent(),
                                    },
                                    {
                                        "category": {
                                            "hid": 0,
                                            "nid": 4353,
                                        },
                                        "intents": Absent(),
                                    },
                                ],
                            },
                            {
                                "category": {
                                    "hid": 0,
                                    "nid": 4253,
                                },
                                "intents": [
                                    {
                                        "category": {"hid": 4581, "nid": 4354, "name": "HID 4581 - 2"},
                                        "intents": Absent(),
                                    }
                                ],
                            },
                            {
                                "category": {
                                    "hid": 0,
                                    "nid": 4254,
                                },
                                "intents": [
                                    {
                                        "category": {"hid": 4582, "nid": 4357, "name": "HID 4582 - 1"},
                                        "intents": Absent(),
                                    }
                                ],
                            },
                        ],
                    }
                ],
            },
            allow_different_len=False,
        )

    def test_market_force_white_on_navtree_intents(self):
        """Проверяем что при market_force_white_on=27 (NAVTREE_INTENTS) на синем начинают отдаваться интенты как в белом"""

    @classmethod
    def prepare_nids_with_recipes(cls):
        cls.index.navtree += [
            NavCategory(
                nid=5251,
                hid=5579,
                children=[
                    NavCategory(
                        nid=5254,
                        hid=5581,
                        children=[
                            NavCategory(nid=5255, hid=5580),
                            NavCategory(
                                nid=5258,
                                hid=5580,
                                recipe=NavRecipe(
                                    filters=[
                                        NavRecipeFilter(
                                            filter_type=NavRecipeFilter.ENUM, param_id=100, enum_values=[1, 2]
                                        ),
                                        NavRecipeFilter(
                                            filter_type=NavRecipeFilter.ENUM, param_id=300, enum_values=[1]
                                        ),
                                        NavRecipeFilter(
                                            filter_type=NavRecipeFilter.ENUM, param_id=200, enum_values=[1]
                                        ),
                                    ]
                                ),
                            ),
                        ],
                    ),
                    NavCategory(
                        nid=5256,
                        hid=5582,
                        children=[
                            NavCategory(nid=5257, hid=5580),
                            NavCategory(
                                nid=5259,  # уйдет
                                hid=5580,
                                recipe=NavRecipe(
                                    filters=[
                                        NavRecipeFilter(
                                            filter_type=NavRecipeFilter.ENUM, param_id=200, enum_values=[1]
                                        ),
                                        NavRecipeFilter(
                                            filter_type=NavRecipeFilter.ENUM, param_id=100, enum_values=[2, 1]
                                        ),
                                        NavRecipeFilter(
                                            filter_type=NavRecipeFilter.ENUM, param_id=300, enum_values=[1]
                                        ),
                                    ]
                                ),
                            ),
                            NavCategory(
                                nid=5260,
                                hid=5580,
                                recipe=NavRecipe(
                                    filters=[
                                        NavRecipeFilter(
                                            filter_type=NavRecipeFilter.BOOLEAN, param_id=400, bool_value=True
                                        ),
                                    ]
                                ),
                            ),
                            NavCategory(
                                nid=5261,  # уйдет
                                hid=5580,
                                recipe=NavRecipe(
                                    filters=[
                                        NavRecipeFilter(
                                            filter_type=NavRecipeFilter.BOOLEAN, param_id=400, bool_value=True
                                        ),
                                    ]
                                ),
                            ),
                        ],
                    ),
                    NavCategory(
                        nid=5252,
                        hid=5580,
                        recipe=NavRecipe(
                            filters=[
                                NavRecipeFilter(filter_type=NavRecipeFilter.ENUM, param_id=100, enum_values=[1]),
                            ]
                        ),
                    ),
                    NavCategory(
                        nid=5253,
                        hid=5580,
                        recipe=NavRecipe(
                            filters=[NavRecipeFilter(filter_type=NavRecipeFilter.ENUM, param_id=100, enum_values=[2])]
                        ),
                    ),
                ],
            )
        ]

        cls.index.gltypes += [
            GLType(param_id=100, hid=5580, gltype=GLType.ENUM, values=[1, 2, 3]),
            GLType(param_id=200, hid=5580, gltype=GLType.ENUM, values=[1, 2]),
            GLType(param_id=300, hid=5580, gltype=GLType.ENUM, values=[1]),
            GLType(param_id=400, hid=5580, gltype=GLType.BOOL),
        ]

        cls.index.offers += [
            Offer(
                hid=5580,
                hyperid=25590,
                glparams=[
                    GLParam(param_id=100, value=1),
                    GLParam(param_id=200, value=1),
                    GLParam(param_id=300, value=1),
                    GLParam(param_id=400, value=1),
                ],
            ),  # nid=5255,5252,5251
            Offer(
                hid=5580,
                hyperid=25590,
                glparams=[
                    GLParam(param_id=100, value=2),
                    GLParam(param_id=200, value=1),
                    GLParam(param_id=300, value=1),
                    GLParam(param_id=400, value=1),
                ],
            ),  # nid=5255,5252,5251
            Offer(hid=5581, hyperid=15581),  # nid=5253,5251
            Offer(hid=5582, hyperid=15582),  # nid=5255,5251
            Offer(hid=5583, hyperid=15583),  # nid=5255,5251
        ]

    def test_recipe_nid_intent(self):
        """
        Проверяем, что уникальность листьев работает по связке hid + список рецептов
        Под флагом market_uniq_nid_intents_by_hid=1
        """
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&nid=5251&rearr-factors=market_return_nids_in_intents=1;market_uniq_nid_intents_by_hid=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {
                            "hid": 5579,
                            "nid": 5251,
                        },
                        "intents": [
                            {
                                "category": {
                                    "hid": 5581,
                                    "nid": 5254,
                                },
                                "intents": [
                                    {
                                        "category": {
                                            # категория 5580
                                            "hid": 5580,
                                            "nid": 5255,
                                        },
                                        "intents": Absent(),
                                    },
                                    {
                                        "category": {
                                            # рецепт неуникален == nid-у 5259
                                            "glfilters": ["100:1,2", "200:1", "300:1"],
                                            "hid": 5580,
                                            "nid": 5258,
                                        },
                                        "intents": Absent(),
                                    },
                                ],
                            },
                            {
                                "category": {
                                    "hid": 5582,
                                    "nid": 5256,
                                },
                                "intents": [
                                    # отсда пропадут ниды 5257 == 5255, 5259 == 5258 и 5261 == 5260
                                    {
                                        "category": {
                                            # рецепт неуникален == nid-у 5261
                                            "glfilters": ["400:1"],
                                            "hid": 5580,
                                            "nid": 5260,
                                        },
                                        "intents": Absent(),
                                    }
                                ],
                            },
                            {
                                "category": {
                                    # рецепт категории 5580 - уникален
                                    "glfilters": ["100:1"],
                                    "hid": 5580,
                                    "nid": 5252,
                                },
                                "intents": Absent(),
                            },
                            {
                                "category": {
                                    # рецепт категории 5580 - уникален
                                    "glfilters": ["100:2"],
                                    "hid": 5580,
                                    "nid": 5253,
                                },
                                "intents": Absent(),
                            },
                        ],
                    }
                ],
            },
            allow_different_len=False,
        )

        # запрос без унификации листьев
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&nid=5251&rearr-factors=market_return_nids_in_intents=1;market_uniq_nid_intents_by_hid=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {
                            "hid": 5579,
                            "nid": 5251,
                        },
                        "intents": [
                            {
                                "category": {
                                    "hid": 5581,
                                    "nid": 5254,
                                },
                                "intents": [
                                    {
                                        "category": {
                                            # категория 5580
                                            "hid": 5580,
                                            "nid": 5255,
                                        },
                                        "intents": Absent(),
                                    },
                                    {
                                        "category": {
                                            # рецепт неуникален == nid-у 5259
                                            "glfilters": ["100:1,2", "200:1", "300:1"],
                                            "hid": 5580,
                                            "nid": 5258,
                                        },
                                        "intents": Absent(),
                                    },
                                ],
                            },
                            {
                                "category": {
                                    "hid": 5582,
                                    "nid": 5256,
                                },
                                "intents": [
                                    {
                                        "category": {
                                            "hid": 5580,
                                            "nid": 5257,
                                        },
                                        "intents": Absent(),
                                    },
                                    {
                                        "category": {
                                            "glfilters": ["100:2,1", "200:1", "300:1"],
                                            "hid": 5580,
                                            "nid": 5259,
                                        },
                                        "intents": Absent(),
                                    },
                                    {
                                        "category": {
                                            # рецепт неуникален == nid-у 5261
                                            "glfilters": ["400:1"],
                                            "hid": 5580,
                                            "nid": 5260,
                                        },
                                        "intents": Absent(),
                                    },
                                    {
                                        "category": {
                                            "glfilters": ["400:1"],
                                            "hid": 5580,
                                            "nid": 5261,
                                        },
                                        "intents": Absent(),
                                    },
                                ],
                            },
                            {
                                "category": {
                                    # рецепт категории 5580 - уникален
                                    "glfilters": ["100:1"],
                                    "hid": 5580,
                                    "nid": 5252,
                                },
                                "intents": Absent(),
                            },
                            {
                                "category": {
                                    # рецепт категории 5580 - уникален
                                    "glfilters": ["100:2"],
                                    "hid": 5580,
                                    "nid": 5253,
                                },
                                "intents": Absent(),
                            },
                        ],
                    }
                ],
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_pictures(cls):
        cls.index.navtree += [
            NavCategory(
                nid=90,
                hid=90,
                children=[
                    NavCategory(
                        nid=91,
                        hid=90,
                        recipe=NavRecipe(
                            filters=[
                                NavRecipeFilter(filter_type=NavRecipeFilter.ENUM, param_id=1000, enum_values=[1]),
                            ]
                        ),
                        primary=False,
                    ),
                    NavCategory(
                        nid=92,
                        hid=90,
                        recipe=NavRecipe(
                            filters=[
                                NavRecipeFilter(filter_type=NavRecipeFilter.ENUM, param_id=1000, enum_values=[2]),
                            ]
                        ),
                        primary=False,
                    ),
                    NavCategory(
                        nid=93,
                        hid=90,
                        children=[
                            NavCategory(
                                nid=94,
                                hid=90,
                            )
                        ],
                        recipe=NavRecipe(
                            filters=[
                                NavRecipeFilter(filter_type=NavRecipeFilter.ENUM, param_id=1000, enum_values=[3]),
                            ]
                        ),
                        hide_inner_nodes=True,
                        primary=False,
                    ),
                    NavCategory(
                        nid=95,
                        hid=90,
                        children=[
                            NavCategory(
                                nid=96,
                                hid=90,
                                is_hidden=True,
                            ),
                            NavCategory(
                                nid=97,
                                hid=90,
                                is_hidden=True,
                            ),
                        ],
                        recipe=NavRecipe(
                            filters=[
                                NavRecipeFilter(filter_type=NavRecipeFilter.ENUM, param_id=1000, enum_values=[4]),
                            ]
                        ),
                        primary=False,
                    ),
                    NavCategory(
                        nid=98,
                        hid=90,
                        recipe=NavRecipe(
                            filters=[
                                NavRecipeFilter(filter_type=NavRecipeFilter.ENUM, param_id=1000, enum_values=[5]),
                            ]
                        ),
                        primary=True,
                    ),
                    NavCategory(
                        # Список моделей
                        nid=99,
                        model_list=ModelList(list_id=1, models=[99]),
                        hid_list=HidList(list_id=1, hids=[90]),
                        node_type=NavCategory.MODEL_LIST,
                        primary=False,
                    ),
                    NavCategory(
                        # Виртуальный узел, у которого все дети скрыты
                        nid=999,
                        hid=0,
                        children=[
                            NavCategory(nid=9991, hid=1991, is_hidden=True),
                            NavCategory(nid=9992, hid=1992, is_hidden=True),
                        ],
                        node_type=NavCategory.VIRTUAL,
                        primary=False,
                    ),
                ],
            ),
        ]

        cls.index.gltypes += [
            GLType(param_id=1000, hid=90, gltype=GLType.ENUM, values=[1, 2, 3, 4, 5]),
        ]

        cls.index.offers += [
            Offer(
                hid=90,
                hyperid=111,
                glparams=[GLParam(param_id=1000, value=1)],
                picture=Picture(picture_id='0______________ygVAHeA', width=200, height=200, group_id=1234),
            ),
            Offer(
                hid=90,
                hyperid=111,
                glparams=[GLParam(param_id=1000, value=2)],
                picture=Picture(picture_id='1______________ygVAHeA', width=200, height=200, group_id=1234),
            ),
            Offer(
                hid=90,
                hyperid=111,
                glparams=[GLParam(param_id=1000, value=3)],
                picture=Picture(picture_id='2______________ygVAHeA', width=200, height=200, group_id=1234),
            ),
            Offer(
                hid=90,
                hyperid=111,
                glparams=[GLParam(param_id=1000, value=4)],
                picture=Picture(picture_id='3______________ygVAHeA', width=200, height=200, group_id=1234),
            ),
            Offer(
                hid=90,
                hyperid=111,
                glparams=[GLParam(param_id=1000, value=5)],
                picture=Picture(picture_id='4______________ygVAHeA', width=200, height=200, group_id=1234),
            ),
            Offer(
                hid=90,
                hyperid=99,  # оффер из списка моделей
                picture=Picture(picture_id='5______________ygVAHeA', width=200, height=200, group_id=1234),
            ),
            Offer(
                title='222',
                hid=1991,
                hyperid=222,
                picture=Picture(picture_id='6______________ygVAHeA', width=200, height=200, group_id=1234),
            ),
            Offer(
                title='333',
                hid=1992,
                hyperid=333,
                picture=Picture(picture_id='7______________ygVAHeA', width=200, height=200, group_id=1234),
            ),
        ]

    def test_nid_pictures(self):
        """
        Проверям, что мы возвращаем картинки в НЕ ОСНОВНЫХ интентах, а не только в первом подходящем основном
        Нужно для дедубликации изображений: MARKETOUT-42573

        Пока под одним из флагов проекта - hid_relevance_in_nid_intents
        """
        flags = '&rearr-factors=market_return_nids_in_intents=1;use_intents_mbo_hiding=1;market_uniq_nid_intents_by_hid=1;market_make_glfilters_from_configs=1;hid_relevance_in_nid_intents=1'
        request = 'place=prime&allow-collapsing=1&nid=90&additional_entities=intents_pictures' + flags

        response = self.report.request_json(request)
        response_direct_snippet = self.report.request_json(request + DIRECT_SNIPPET_FLAG)
        self.check_intents_equal(response, response_direct_snippet)

        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {
                            "hid": 90,
                            "nid": 90,
                        },
                        "pictures": NotEmpty(),
                        "intents": [
                            {
                                "category": {
                                    "hid": 90,
                                    "nid": 91,
                                },
                                "intents": Absent(),
                                "pictures": [
                                    {
                                        "entity": "picture",
                                        "original": {
                                            "containerHeight": 200,
                                            "containerWidth": 200,
                                            "height": 200,
                                            "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_0______________ygVAHeA/orig",
                                            "width": 200,
                                        },
                                        "thumbnails": NotEmpty(),
                                    }
                                ],
                            },
                            {
                                "category": {
                                    "hid": 90,
                                    "nid": 92,
                                },
                                "intents": Absent(),
                                "pictures": [
                                    {
                                        "entity": "picture",
                                        "original": {
                                            "containerHeight": 200,
                                            "containerWidth": 200,
                                            "height": 200,
                                            "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_1______________ygVAHeA/orig",
                                            "width": 200,
                                        },
                                        "thumbnails": NotEmpty(),
                                    }
                                ],
                            },
                            {
                                "category": {
                                    "hid": 90,
                                    "nid": 93,
                                },
                                "intents": Absent(),  # тк hide_inner_nodes
                                "pictures": [
                                    {
                                        "entity": "picture",
                                        "original": {
                                            "containerHeight": 200,
                                            "containerWidth": 200,
                                            "height": 200,
                                            "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_2______________ygVAHeA/orig",
                                            "width": 200,
                                        },
                                        "thumbnails": NotEmpty(),
                                    }
                                ],
                            },
                            {
                                "category": {
                                    "hid": 90,
                                    "nid": 95,
                                },
                                "intents": Absent(),  # тк все дети скрыты
                                "pictures": [
                                    {
                                        "entity": "picture",
                                        "original": {
                                            "containerHeight": 200,
                                            "containerWidth": 200,
                                            "height": 200,
                                            "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_3______________ygVAHeA/orig",
                                            "width": 200,
                                        },
                                        "thumbnails": NotEmpty(),
                                    }
                                ],
                            },
                            {
                                # primary рецепт
                                "category": {
                                    "hid": 90,
                                    "nid": 98,
                                },
                                "intents": Absent(),
                                "pictures": [
                                    {
                                        "entity": "picture",
                                        "original": {
                                            "containerHeight": 200,
                                            "containerWidth": 200,
                                            "height": 200,
                                            "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_4______________ygVAHeA/orig",
                                            "width": 200,
                                        },
                                        "thumbnails": NotEmpty(),
                                    }
                                ],
                            },
                            {
                                # список моделей
                                "category": {
                                    "nid": 99,
                                },
                                "intents": Absent(),
                                "pictures": [
                                    {
                                        "entity": "picture",
                                        "original": {
                                            "containerHeight": 200,
                                            "containerWidth": 200,
                                            "height": 200,
                                            "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_5______________ygVAHeA/orig",
                                            "width": 200,
                                        },
                                        "thumbnails": NotEmpty(),
                                    }
                                ],
                            },
                            {
                                # Виртуальный узел со скрытыми детьми
                                "category": {
                                    "nid": 999,
                                },
                                "intents": Absent(),
                                "pictures": [
                                    {
                                        "entity": "picture",
                                        "original": {
                                            "containerHeight": 200,
                                            "containerWidth": 200,
                                            "height": 200,
                                            "width": 200,
                                        },
                                        "thumbnails": NotEmpty(),
                                    }
                                ],
                            },
                        ],
                    }
                ],
            },
            allow_different_len=False,
        )

        # Проверяем, что работает новый формат картинок
        request = (
            'place=prime&allow-collapsing=1&nid=90&additional_entities=intents_pictures&new-picture-format=1' + flags
        )

        response = self.report.request_json(request)
        response_direct_snippet = self.report.request_json(request + DIRECT_SNIPPET_FLAG)
        self.check_intents_equal(response, response_direct_snippet)

        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {
                            "hid": 90,
                            "nid": 90,
                        },
                        "pictures": NotEmpty(),
                        "intents": [
                            {
                                "category": {
                                    "hid": 90,
                                    "nid": 91,
                                },
                                "intents": Absent(),
                                "pictures": [
                                    {
                                        "entity": "picture",
                                        "original": {
                                            "groupId": 1234,
                                            "height": 200,
                                            "key": "market_0______________ygVAHeA",
                                            "namespace": "marketpic",
                                            "width": 200,
                                        },
                                        "signatures": [],
                                    }
                                ],
                            },
                            {
                                "category": {
                                    "hid": 90,
                                    "nid": 92,
                                },
                                "intents": Absent(),
                                "pictures": [
                                    {
                                        "entity": "picture",
                                        "original": {
                                            "groupId": 1234,
                                            "height": 200,
                                            "key": "market_1______________ygVAHeA",
                                            "namespace": "marketpic",
                                            "width": 200,
                                        },
                                        "signatures": [],
                                    }
                                ],
                            },
                            {
                                "category": {
                                    "hid": 90,
                                    "nid": 93,
                                },
                                "intents": Absent(),  # тк hide_inner_nodes
                                "pictures": [
                                    {
                                        "entity": "picture",
                                        "original": {
                                            "groupId": 1234,
                                            "height": 200,
                                            "key": "market_2______________ygVAHeA",
                                            "namespace": "marketpic",
                                            "width": 200,
                                        },
                                        "signatures": [],
                                    }
                                ],
                            },
                            {
                                "category": {
                                    "hid": 90,
                                    "nid": 95,
                                },
                                "intents": Absent(),  # тк все дети скрыты
                                "pictures": [
                                    {
                                        "entity": "picture",
                                        "original": {
                                            "groupId": 1234,
                                            "height": 200,
                                            "key": "market_3______________ygVAHeA",
                                            "namespace": "marketpic",
                                            "width": 200,
                                        },
                                        "signatures": [],
                                    }
                                ],
                            },
                            {
                                # primary рецепт
                                "category": {
                                    "hid": 90,
                                    "nid": 98,
                                },
                                "intents": Absent(),
                                "pictures": [
                                    {
                                        "entity": "picture",
                                        "original": {
                                            "groupId": 1234,
                                            "height": 200,
                                            "key": "market_4______________ygVAHeA",
                                            "namespace": "marketpic",
                                            "width": 200,
                                        },
                                        "signatures": [],
                                    }
                                ],
                            },
                            {
                                # список моделей
                                "category": {
                                    "nid": 99,
                                },
                                "intents": Absent(),
                                "pictures": [
                                    {
                                        "entity": "picture",
                                        "original": {
                                            "groupId": 1234,
                                            "height": 200,
                                            "key": "market_5______________ygVAHeA",
                                            "namespace": "marketpic",
                                            "width": 200,
                                        },
                                        "signatures": [],
                                    }
                                ],
                            },
                            {
                                # Виртуальный узел со скрытыми детьми
                                "category": {
                                    "nid": 999,
                                },
                                "intents": Absent(),
                                "pictures": [
                                    {
                                        "entity": "picture",
                                        "original": {
                                            "groupId": 1234,
                                            "height": 200,
                                            "namespace": "marketpic",
                                            "width": 200,
                                        },
                                        "signatures": [],
                                    }
                                ],
                            },
                        ],
                    }
                ],
            },
            allow_different_len=False,
        )

        # Без флага статистика для НЕ ОСНОВНЫХ рецептов-листов не будет подсчитана
        flags = '&rearr-factors=market_return_nids_in_intents=1;use_intents_mbo_hiding=1;market_uniq_nid_intents_by_hid=1;market_make_glfilters_from_configs=1;hid_relevance_in_nid_intents=0'
        request = 'place=prime&allow-collapsing=1&nid=90&additional_entities=intents_pictures' + flags

        response = self.report.request_json(request)
        response_direct_snippet = self.report.request_json(request + DIRECT_SNIPPET_FLAG)
        self.check_intents_equal(response, response_direct_snippet)

        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {
                            "hid": 90,
                            "nid": 90,
                        },
                        "pictures": NotEmpty(),
                        "intents": [
                            {
                                "category": {
                                    "hid": 90,
                                    "nid": 91,
                                },
                                "intents": Absent(),
                                "pictures": Absent(),
                            },
                            {
                                "category": {
                                    "hid": 90,
                                    "nid": 92,
                                },
                                "intents": Absent(),
                                "pictures": Absent(),
                            },
                            {
                                "category": {
                                    "hid": 90,
                                    "nid": 93,
                                },
                                "intents": Absent(),  # тк hide_inner_nodes
                                "pictures": Absent(),
                            },
                            {
                                "category": {
                                    "hid": 90,
                                    "nid": 95,
                                },
                                "intents": Absent(),  # тк все дети - скрыты
                                "pictures": Absent(),
                            },
                            {
                                # тк primary рецепт - то изображения останутся
                                "category": {
                                    "hid": 90,
                                    "nid": 98,
                                },
                                "intents": Absent(),
                                "pictures": [
                                    {
                                        "entity": "picture",
                                        "original": {
                                            "containerHeight": 200,
                                            "containerWidth": 200,
                                            "height": 200,
                                            "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_4______________ygVAHeA/orig",
                                            "width": 200,
                                        },
                                    }
                                ],
                            },
                            {
                                # список моделей
                                "category": {
                                    "nid": 99,
                                },
                                "intents": Absent(),
                                "pictures": Absent(),
                            },
                            {
                                # Виртуальный узел со скрытыми детьми
                                "category": {
                                    "nid": 999,
                                },
                                "intents": Absent(),
                                "pictures": Absent(),
                            },
                        ],
                    }
                ],
            },
            allow_different_len=False,
        )

        # проверяем логику для вирт узлов
        # Он идет в статистиках у всех документов с хидами его дочерних узлов
        flags = '&rearr-factors=market_return_nids_in_intents=1;use_intents_mbo_hiding=1;market_uniq_nid_intents_by_hid=1;market_make_glfilters_from_configs=1;hid_relevance_in_nid_intents=1'
        request = (
            'place=prime&allow-collapsing=1&nid=90&text=222&additional_entities=intents_pictures&new-picture-format=1'
            + flags
        )

        response = self.report.request_json(request)
        response_direct_snippet = self.report.request_json(request + DIRECT_SNIPPET_FLAG)
        self.check_intents_equal(response, response_direct_snippet)

        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {
                            "hid": 90,
                            "nid": 90,
                        },
                        "pictures": NotEmpty(),
                        "intents": [
                            {
                                # Виртуальный узел со скрытыми детьми
                                "category": {
                                    "nid": 999,
                                },
                                "intents": Absent(),
                                "pictures": [
                                    {
                                        "entity": "picture",
                                        "original": {
                                            "groupId": 1234,
                                            "height": 200,
                                            "key": "market_6______________ygVAHeA",  # картинка документа с hid=1991
                                            "namespace": "marketpic",
                                            "width": 200,
                                        },
                                        "signatures": [],
                                    }
                                ],
                            },
                        ],
                    }
                ],
            },
            allow_different_len=False,
        )

        request = (
            'place=prime&allow-collapsing=1&nid=90&text=333&additional_entities=intents_pictures&new-picture-format=1'
            + flags
        )

        response = self.report.request_json(request)
        response_direct_snippet = self.report.request_json(request + DIRECT_SNIPPET_FLAG)
        self.check_intents_equal(response, response_direct_snippet)

        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {
                            "hid": 90,
                            "nid": 90,
                        },
                        "pictures": NotEmpty(),
                        "intents": [
                            {
                                # Виртуальный узел со скрытыми детьми
                                "category": {
                                    "nid": 999,
                                },
                                "intents": Absent(),
                                "pictures": [
                                    {
                                        "entity": "picture",
                                        "original": {
                                            "groupId": 1234,
                                            "height": 200,
                                            "key": "market_7______________ygVAHeA",  # картинка документа с hid=1997
                                            "namespace": "marketpic",
                                            "width": 200,
                                        },
                                        "signatures": [],
                                    }
                                ],
                            },
                        ],
                    }
                ],
            },
            allow_different_len=False,
        )

    @classmethod
    def flatten_intents(self, intent_list, result):
        if not intent_list:
            return
        for intent in intent_list:
            result.append(intent)
            self.flatten_intents(intent.get('intents'), result)

    @classmethod
    def extract_intent_hids(self, response):
        result = []
        flat_intents = []
        self.flatten_intents(response.root.get('intents'), flat_intents)
        for intent in flat_intents:
            result.append(intent['category']['hid'])
        return result

    def test_unchanging_rearranged_intent_order(self):
        # Переранжирование топа интентов не должно влиять на порядок интентов в дереве intents

        req_clean = 'place=prime&text=pepyaka'
        intents_clean = self.extract_intent_hids(self.report.request_json(req_clean))

        req_rearr = req_clean + '&rearr-factors=market_rearrange_intents_by_hids_on_text=1'
        intents_rearr = self.extract_intent_hids(self.report.request_json(req_rearr))

        assert len(intents_clean) > 1
        assert intents_clean == intents_rearr

    @classmethod
    def get_docs_hid_order(self, response):
        hid_order_docs = []
        docs = response['search']['results']
        for doc in docs:
            hid = doc['categories'][0]['id']
            if hid not in hid_order_docs:
                hid_order_docs.append(hid)
        return hid_order_docs

    @classmethod
    def get_intents_hid_order(self, response):
        hid_order_intents = []
        flat_intents = []
        self.flatten_intents(response.root.get('intents'), flat_intents)
        flat_intents.sort(key=lambda i: i.get('defaultOrder') or -1)
        for intent in flat_intents:
            hid = intent['category']['hid']
            if (
                hid not in hid_order_intents and hid != 42
            ):  # hid=42 is a hierarchy root and doesn't belong to any offers
                hid_order_intents.append(hid)
        return hid_order_intents

    def test_hid_intents_rearrange_by_docs(self):
        # Включаем переранжирование интентов для текстового поиска.
        # У интентов меняется поле defaultOrder, по которому упорядочиваются листовые интенты над выдачей.
        # Порядок hid-ов интентов под флагом должен совпадать с порядком hid-ов в выдаче
        req = 'place=prime&text=pepyaka&rearr-factors=market_rearrange_intents_by_hids_on_text=1'
        response = self.report.request_json(req)
        assert self.get_docs_hid_order(response) == self.get_intents_hid_order(response)

    def test_hid_intents_rearrange_preserve_relevance(self):
        # При переранжировании должны смотреть на порядок первой страницы документов,
        # дальше порядок должен совпадать с тем, что был без флага market_rearrange_intents_by_hids_on_text
        req = 'place=prime&text=pepyaka&rearr-factors=market_rearrange_intents_by_hids_on_text=1&numdoc=4&rearr-factors=market_metadoc_search=no'
        response = self.report.request_json(req)
        first_page_hids = self.get_docs_hid_order(response)
        num_hids = len(first_page_hids)
        assert num_hids >= 3

        rearranged_intent_hids = self.get_intents_hid_order(response)

        req = 'place=prime&text=pepyaka&rearr-factors=market_rearrange_intents_by_hids_on_text=0&numdoc=4&rearr-factors=market_metadoc_search=no'
        response = self.report.request_json(req)
        original_intent_hids = self.get_intents_hid_order(response)
        assert sorted(rearranged_intent_hids) == sorted(original_intent_hids)

        rearranged_tail_hids = rearranged_intent_hids[num_hids:]

        idx = 0
        for hid in original_intent_hids:
            if hid in rearranged_tail_hids:
                assert rearranged_tail_hids.index(hid) == idx
                idx += 1

    def test_hid_intents_rearrange_stability(self):
        # При переранжировании должны смотреть на порядок документов с первой страницы, даже если запрашиваем вторую и т.д.
        req = 'place=prime&text=pepyaka&rearr-factors=market_rearrange_intents_by_hids_on_text=1&numdoc=4&rearr-factors=market_metadoc_search=no'
        response = self.report.request_json(req)
        first_page_hids = self.get_docs_hid_order(response)
        num_hids = len(first_page_hids)
        assert num_hids >= 3

        intent_hids = self.get_intents_hid_order(response)[:num_hids]
        assert first_page_hids == intent_hids

        response = self.report.request_json(req + '&page=2')
        intent_hids = self.get_intents_hid_order(response)[:num_hids]
        assert first_page_hids == intent_hids

    def test_hid_intents_rearrange_by_docs_textless(self):
        # Включаем переранжирование интентов для текстового поиска.
        # У интентов меняется поле defaultOrder, по которому упорядочиваются листовые интенты над выдачей.
        # Порядок hid-ов интентов под флагом должен совпадать с порядком hid-ов в выдаче
        req = 'place=prime&hid=42&rearr-factors=market_rearrange_intents_by_hids_on_textless=1'
        response = self.report.request_json(req)
        assert self.get_docs_hid_order(response) == self.get_intents_hid_order(response)

    def test_nid_intents_rearrange_by_docs(self):
        # Аналогично предыдущему тесту, но интенты строятся из навигационного дерева.
        # Цель - проверка совместимости с market_return_nids_in_intents=1
        req = 'place=prime&text=pepyaka&rearr-factors=market_rearrange_intents_by_hids_on_text=1;market_return_nids_in_intents=1'
        response = self.report.request_json(req)
        assert self.get_docs_hid_order(response) == self.get_intents_hid_order(response)

    def test_no_intents(self):
        """Проверяем что при наличии флагов
        no-search-filters=1
        no-intents=1
        market_write_meta_factors=0;market_write_category_redirect_features=0;market_enable_meta_head_rearrange=0
        cvredirect=0
        ранжирование категорий не нужно и оно не вызывается
        """
        response = self.report.request_json(
            'place=prime&hid=5582&no-search-filters=1&debug=da'
            '&rearr-factors=market_write_meta_factors=0;market_write_category_redirect_features=0;market_enable_meta_head_rearrange=0'
        )
        self.assertFragmentIn(response, {'intents': NotEmptyList()}, allow_different_len=False)
        self.assertFragmentIn(response, 'Calculate categories ranking')

        response = self.report.request_json(
            'place=prime&hid=5582&no-search-filters=1&no-intents=1&debug=da'
            '&rearr-factors=market_write_meta_factors=0;market_write_category_redirect_features=0;market_enable_meta_head_rearrange=0'
        )
        self.assertFragmentIn(response, {'intents': []}, allow_different_len=False)
        self.assertFragmentNotIn(response, 'Calculate categories ranking')

    def test_nid_intents_in_deparments(self):
        for flags in [
            '&rearr-factors=turn_off_nid_intents_on_departments_web=1',
            '&client=ANDROID&rearr-factors=turn_off_nid_intents_on_departments_app=1',
        ]:
            # департамент
            response = self.report.request_json('place=prime&text=pepyaka&nid=1000' + flags)
            self.assertFragmentIn(response, self.expected_hid_intents())

    @classmethod
    def prepare_web_like_app_intents(cls):
        cls.index.hypertree += [
            HyperCategory(hid=103),
        ]

        cls.index.navtree += [
            NavCategory(
                nid=1444,
                hid=103,
                name='Smartphones',
                is_hidden_app=True,
            ),
        ]

        cls.index.models += [
            Model(hid=103, title="pixel 1", hyperid=1231),
        ]

        cls.index.mskus += [
            MarketSku(
                sku=123,
                title='pixel 1',
                hyperid=1231,
                blue_offers=[
                    BlueOffer(
                        price=10,
                        offerid='Shop777_sku12300',
                    )
                ],
            ),
        ]

    def test_web_like_app_intents(self):
        '''
        Проверяем что под флагом market_web_like_app_intents на поиске в приложении хидовые интенты
        https://st.yandex-team.ru/MARKETOUT-43264
        '''

        # Запрос из приложения без флага. Интенты нидовые и отфильтровываются из-за is_hidden_app флага
        # флаг market_web_like_app_intents расскатан, проставляем его в 0
        response = self.report.request_json(
            'place=prime&text=pixel&pp=18&debug=0&rgb=blue&client=ANDROID&rearr-factors=market_web_like_app_intents=0'
        )
        self.assertFragmentIn(response, {"intents": EmptyList()})

        # Запрос из приложения c флагом. Интенты хидовые и не отфильтровываются
        response = self.report.request_json(
            'place=prime&text=pixel&pp=18&debug=0&rgb=blue&client=ANDROID&rearr-factors=market_web_like_app_intents=1'
        )
        self.assertFragmentIn(response, {"intents": [{"category": {"hid": 103, "nid": 1444}}]})


if __name__ == '__main__':
    main()
