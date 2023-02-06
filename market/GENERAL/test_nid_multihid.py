#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main
from core.types import GLParam, GLType, NavCategory, NavRecipe, NavRecipeFilter, Offer
from core.types.navcategory import FilterConfig
from core.matcher import Absent


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.navtree += [
            NavCategory(
                nid=1,
                display_style='GRID',
                children=[
                    NavCategory(nid=2, hid=1, name='kot-nid-1', display_style='LIST'),
                    NavCategory(nid=3, hid=2, name='kot-nid-2'),
                ],
            )
        ]
        cls.index.gltypes += [
            GLType(param_id=1, hid=1, gltype=GLType.ENUM, hidden=False, values=[1, 2], name="kot-param-1"),
            GLType(param_id=2, hid=2, gltype=GLType.ENUM, hidden=False, values=[1, 2], name="kot-param-2"),
        ]
        cls.index.offers += [
            Offer(hid=1, title='kot1', glparams=[GLParam(param_id=1, value=1)]),
            Offer(hid=2, title='kot2', glparams=[GLParam(param_id=2, value=1)]),
        ]

    def test_nid_view_type(self):
        """проверяем, что с включенными флагами фильтры берутся от 1го ребенка виртуального нида"""
        response = self.report.request_json('place=prime&nid=1&rearr-factors=market_use_childs_for_nid_to_hid=1')
        self.assertFragmentIn(response, {"search": {"view": "grid"}})
        response = self.report.request_json('place=prime&nid=2&rearr-factors=market_use_childs_for_nid_to_hid=1')
        self.assertFragmentIn(response, {"search": {"view": "list"}})
        response = self.report.request_json('place=prime&nid=3&rearr-factors=market_use_childs_for_nid_to_hid=1')
        self.assertFragmentIn(response, {"search": {"view": "list"}})

    @classmethod
    def prepare_multinid_filters(cls):
        cls.index.navtree += [
            NavCategory(
                nid=1101,
                hid=2101,
                children=[
                    NavCategory(
                        nid=1201,
                        hid=0,
                        children=[
                            NavCategory(nid=1301, hid=2301, name='kot-nid-1301'),
                            NavCategory(nid=1302, hid=2302, name='kot-nid-1302'),
                            NavCategory(nid=1303, hid=2303, name='kot-nid-1303'),
                            NavCategory(
                                nid=1304,
                                hid=2304,
                                name='kot-nid-1304-recipe',
                                recipe=NavRecipe(
                                    filters=[
                                        NavRecipeFilter(
                                            filter_type=NavRecipeFilter.ENUM, param_id=903, enum_values=[10]
                                        ),
                                    ]
                                ),
                            ),
                        ],
                        node_type=NavCategory.VIRTUAL,
                    )
                ],
            ),
            NavCategory(
                nid=11010,
                hid=21010,
                filter_config=FilterConfig(conf_id=55, filters=[103, 104], advanced_filters=[105, 103]),
                children=[
                    NavCategory(
                        nid=12010,
                        hid=0,
                        filter_config=FilterConfig(
                            conf_id=77, filters=[101, 100], advanced_filters=[102, 100, 101, 12312]
                        ),
                        children=[
                            NavCategory(nid=13010, hid=23010, name='kot-nid-13010'),
                            NavCategory(nid=13020, hid=23020, name='kot-nid-13020'),
                            NavCategory(nid=13030, hid=23030, name='kot-nid-13030'),
                        ],
                        node_type=NavCategory.VIRTUAL,
                    )
                ],
            ),
            NavCategory(
                nid=555,
                filter_config=FilterConfig(conf_id=88, filters=[750, 850, 950], advanced_filters=[750]),
                children=[
                    NavCategory(nid=777, hid=1777, name='kot-nid-777'),
                    NavCategory(nid=888, hid=1888, name='kot-nid-888'),
                    NavCategory(nid=999, hid=1999, name='kot-nid-999'),
                ],
                node_type=NavCategory.VIRTUAL,
            ),
            NavCategory(nid=1111, hid=9111, hide_gl_filters=True),
            NavCategory(
                nid=3101,
                hid=4101,
                children=[
                    NavCategory(
                        nid=3201,
                        hid=0,
                        children=[
                            NavCategory(nid=3301, hid=4301, name='kot-nid-3301'),
                            NavCategory(nid=3302, hid=4302, name='kot-nid-3302'),
                        ],
                        node_type=NavCategory.VIRTUAL,
                    )
                ],
            ),
        ]

        cls.index.gltypes += [
            GLType(param_id=903, hid=2301, gltype=GLType.ENUM, values=[41, 42, 47], name="kot-param-903(2301)"),
            GLType(param_id=903, hid=2302, gltype=GLType.ENUM, values=[41, 42, 43, 44], name="kot-param-903(2302)"),
            GLType(param_id=903, hid=2303, gltype=GLType.ENUM, values=[43, 44, 45, 46], name="kot-param-903(2303)"),
            GLType(param_id=903, hid=2304, gltype=GLType.ENUM, values=[10, 20, 30], name="kot-param-903(2304)"),
            GLType(param_id=100, hid=23010, gltype=GLType.ENUM, values=[10, 20, 30], name="kot-param-100"),
            GLType(param_id=101, hid=23020, gltype=GLType.BOOL, name="kot-param-101"),
            GLType(param_id=102, hid=23030, gltype=GLType.NUMERIC, name="kot-param-102"),
            GLType(param_id=103, hid=21010, gltype=GLType.ENUM, values=[99], name="kot-param-103", hidden=True),
            GLType(param_id=104, hid=21010, gltype=GLType.BOOL, name="kot-param-104"),
            GLType(param_id=105, hid=21010, gltype=GLType.NUMERIC, name="kot-param-105"),
            GLType(param_id=750, hid=1777, gltype=GLType.ENUM, values=[1, 2, 3], name="kot-param-750(1777)"),
            GLType(param_id=750, hid=1888, gltype=GLType.ENUM, values=[1, 2], name="kot-param-750(1888)"),
            GLType(param_id=750, hid=1999, gltype=GLType.ENUM, values=[4], name="kot-param-750(1999)"),
            GLType(param_id=850, hid=1777, gltype=GLType.BOOL, name="kot-param-850(1777)"),
            GLType(param_id=850, hid=1888, gltype=GLType.BOOL, name="kot-param-850(1888)"),
            GLType(param_id=850, hid=1999, gltype=GLType.BOOL, name="kot-param-850(1999)"),
            GLType(param_id=950, hid=1777, gltype=GLType.NUMERIC, name="kot-param-950(1777)"),
            GLType(param_id=950, hid=1888, gltype=GLType.NUMERIC, name="kot-param-950(1888)"),
            GLType(param_id=950, hid=1999, gltype=GLType.NUMERIC, name="kot-param-950(1999)"),
            GLType(param_id=555, hid=9111, gltype=GLType.ENUM, values=[1, 2], name="kot-param-555(9111)"),
            GLType(param_id=556, hid=9111, gltype=GLType.BOOL, name="kot-param-556(9111)"),
            GLType(
                param_id=933,
                hid=4301,
                vendor=True,
                short_enum_count=3,
                gltype=GLType.ENUM,
                values=[param_value for param_value in range(41, 50)],
                name="kot-param-933(4301)",
            ),
            GLType(
                param_id=933,
                hid=4302,
                vendor=True,
                short_enum_count=3,
                gltype=GLType.ENUM,
                values=[param_value for param_value in range(51, 70)],
                name="kot-param-933(4302)",
            ),
            GLType(
                param_id=935,
                hid=4301,
                vendor=True,
                short_enum_count=3,
                gltype=GLType.ENUM,
                values=[91],
                name="kot-param-935(4301)",
            ),
            GLType(
                param_id=935,
                hid=4302,
                vendor=True,
                short_enum_count=3,
                gltype=GLType.ENUM,
                values=[92],
                name="kot-param-935(4302)",
            ),
            GLType(
                param_id=936,
                hid=4301,
                vendor=True,
                short_enum_count=3,
                gltype=GLType.ENUM,
                values=[93, 94],
                name="kot-param-936(4301)",
            ),
            GLType(
                param_id=936,
                hid=4302,
                vendor=True,
                short_enum_count=3,
                gltype=GLType.ENUM,
                values=[95, 96],
                name="kot-param-936(4302)",
            ),
        ]

        cls.index.offers += [
            Offer(hid=2301, title='kot1', glparams=[GLParam(param_id=903, value=41)]),
            Offer(hid=2302, title='kot2', glparams=[GLParam(param_id=903, value=42)]),
            Offer(hid=2302, title='kot3', glparams=[GLParam(param_id=903, value=43)]),
            Offer(hid=2303, title='kot4', glparams=[GLParam(param_id=903, value=44)]),
            Offer(hid=2303, title='kot5', glparams=[GLParam(param_id=903, value=46)]),
            Offer(hid=2304, title='kot6', glparams=[GLParam(param_id=903, value=10)]),
            Offer(hid=2304, title='kot7', glparams=[GLParam(param_id=903, value=20)]),
            Offer(hid=2301, title='kot8', glparams=[GLParam(param_id=903, value=42)]),
            Offer(hid=23010, title='kotik1-23010', glparams=[GLParam(param_id=100, value=10)]),
            Offer(hid=23010, title='kotik2-23010', glparams=[GLParam(param_id=100, value=20)]),
            Offer(hid=23020, title='kotik3-23020', glparams=[GLParam(param_id=100, value=30)]),
            Offer(hid=23020, title='kotik4-23020', glparams=[GLParam(param_id=101, value=1)]),
            Offer(hid=23030, title='kotik5-23030', glparams=[GLParam(param_id=102, value=100500)]),
            Offer(hid=21010, title='kotik6-21010', glparams=[GLParam(param_id=103, value=99)]),
            Offer(hid=21010, title='kotik7-21010', glparams=[GLParam(param_id=104, value=1)]),
            Offer(hid=21010, title='kotik8-21010', glparams=[GLParam(param_id=105, value=200500)]),
            Offer(
                hid=1777, title='kotik9-1777', glparams=[GLParam(param_id=750, value=1), GLParam(param_id=850, value=1)]
            ),
            Offer(
                hid=1777,
                title='kotik10-1777',
                glparams=[
                    GLParam(param_id=750, value=3),
                    GLParam(param_id=850, value=0),
                    GLParam(param_id=950, value=100),
                ],
            ),
            Offer(
                hid=1888,
                title='kotik11-1888',
                glparams=[GLParam(param_id=750, value=1), GLParam(param_id=850, value=1)],
            ),
            Offer(
                hid=1888,
                title='kotik12-1888',
                glparams=[
                    GLParam(param_id=750, value=2),
                    GLParam(param_id=850, value=0),
                    GLParam(param_id=950, value=200),
                ],
            ),
            Offer(hid=1999, title='kotik13-1999', glparams=[GLParam(param_id=750, value=4)]),
            Offer(
                hid=1999,
                title='kotik14-1999',
                glparams=[GLParam(param_id=850, value=1), GLParam(param_id=950, value=300)],
            ),
            Offer(
                hid=9111,
                title='kotik15-9111',
                glparams=[GLParam(param_id=555, value=1), GLParam(param_id=556, value=1)],
            ),
            Offer(
                hid=9111,
                title='kotik16-9111',
                glparams=[GLParam(param_id=555, value=2), GLParam(param_id=556, value=0)],
            ),
            Offer(hid=4301, title='kotyara91', glparams=[GLParam(param_id=935, value=91)]),
            Offer(hid=4302, title='kotyara92', glparams=[GLParam(param_id=935, value=92)]),
            Offer(hid=4301, title='kotyara93', glparams=[GLParam(param_id=936, value=93)]),
            Offer(hid=4301, title='kotyara94', glparams=[GLParam(param_id=936, value=94)]),
            Offer(hid=4302, title='kotyara95', glparams=[GLParam(param_id=936, value=95)]),
            Offer(hid=4302, title='kotyara96', glparams=[GLParam(param_id=936, value=96)]),
        ]

        for param_value in range(51, 70):
            for i in range(1, 71 - param_value):
                cls.index.offers.append(
                    Offer(
                        hid=4302,
                        title=str(i) + 'kotyara' + str(param_value),
                        glparams=[GLParam(param_id=933, value=param_value)],
                    )
                )
        for param_value in range(41, 50):
            for i in range(1, 71 - param_value):
                cls.index.offers.append(
                    Offer(
                        hid=4301,
                        title=str(i) + 'kotyara' + str(param_value),
                        glparams=[GLParam(param_id=933, value=param_value)],
                    )
                )

    def test_multinid_filters_count(self):
        """проверяем, что при обогащении мультинида не превышаются лимиты на количетво значений enum-параметров"""

        query = 'place=prime&nid=3201&showVendors=top&rearr-factors=market_use_childs_for_nid_to_hid=1;market_enrich_multinid_filters=1'
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                "id": "933",
                "valuesCount": 28,
                "values": [
                    {"id": "41"},
                    {"id": "42"},
                    {"id": "43"},
                    {"id": "44"},
                    {"id": "45"},
                    {"id": "46"},
                    {"id": "47"},
                    {"id": "48"},
                    {"id": "49"},
                    {"id": "51"},
                    {"id": "52"},
                    {"id": "53"},
                ],
                "valuesGroups": [
                    {"type": "top", "valuesIds": ["41", "42", "43"]},
                    {
                        "type": "all",
                        "valuesIds": ["41", "42", "43", "44", "45", "46", "47", "48", "49", "51", "52", "53"],
                    },
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.assertFragmentIn(
            response,
            {
                "id": "935",
                "valuesCount": 2,
                "values": [{"id": "91"}, {"id": "92"}],
                "valuesGroups": [{"type": "all", "valuesIds": ["91", "92"]}],
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.assertFragmentIn(
            response,
            {
                "id": "936",
                "valuesCount": 4,
                "values": [{"id": "93"}, {"id": "94"}, {"id": "95"}, {"id": "96"}],
                "valuesGroups": [
                    {"type": "all", "valuesIds": ["93", "94", "95", "96"]},
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # Вариант, когда top не удалялся, если размер top и размер all отличались меньше чем на 3 элемента
        response = self.report.request_json(query + "&rearr-factors=market_sort_by_dimension_filter_items=0")
        self.assertFragmentIn(
            response,
            {
                "id": "936",
                "valuesCount": 4,
                "values": [{"id": "93"}, {"id": "94"}, {"id": "95"}, {"id": "96"}],
                "valuesGroups": [
                    {"type": "top", "valuesIds": ["93", "94", "95"]},
                    {"type": "all", "valuesIds": ["93", "94", "95", "96"]},
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_multinid_filters_all(self):
        """проверяем, что при флаге showVendors=all игнорируются лимиты на количетво значений enum-параметров"""

        query = 'place=prime&nid=3201&showVendors=all&rearr-factors=market_use_childs_for_nid_to_hid=1;market_enrich_multinid_filters=1'
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                "id": "933",
                "valuesCount": 28,
                "values": [
                    {"id": "41"},
                    {"id": "42"},
                    {"id": "43"},
                    {"id": "44"},
                    {"id": "45"},
                    {"id": "46"},
                    {"id": "47"},
                    {"id": "48"},
                    {"id": "49"},
                    {"id": "51"},
                    {"id": "52"},
                    {"id": "53"},
                    {"id": "54"},
                    {"id": "55"},
                    {"id": "56"},
                    {"id": "57"},
                    {"id": "58"},
                    {"id": "59"},
                    {"id": "60"},
                    {"id": "61"},
                    {"id": "62"},
                    {"id": "63"},
                    {"id": "64"},
                    {"id": "65"},
                    {"id": "66"},
                    {"id": "67"},
                    {"id": "68"},
                    {"id": "69"},
                ],
                "valuesGroups": [
                    {"type": "top", "valuesIds": ["41", "42", "43"]},
                    {
                        "type": "all",
                        "valuesIds": [
                            "41",
                            "42",
                            "43",
                            "44",
                            "45",
                            "46",
                            "47",
                            "48",
                            "49",
                            "51",
                            "52",
                            "53",
                            "54",
                            "55",
                            "56",
                            "57",
                            "58",
                            "59",
                            "60",
                            "61",
                            "62",
                            "63",
                            "64",
                            "65",
                            "66",
                            "67",
                            "68",
                            "69",
                        ],
                    },
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.assertFragmentIn(
            response,
            {
                "id": "935",
                "valuesCount": 2,
                "values": [{"id": "91"}, {"id": "92"}],
                "valuesGroups": [{"type": "all", "valuesIds": ["91", "92"]}],
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.assertFragmentIn(
            response,
            {
                "id": "936",
                "valuesCount": 4,
                "values": [{"id": "93"}, {"id": "94"}, {"id": "95"}, {"id": "96"}],
                "valuesGroups": [
                    {"type": "all", "valuesIds": ["93", "94", "95", "96"]},
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # Вариант, когда top не удалялся, если размер top и размер all отличались меньше чем на 3 элемента
        response = self.report.request_json(query + "&rearr-factors=market_sort_by_dimension_filter_items=0")
        self.assertFragmentIn(
            response,
            {
                "id": "936",
                "valuesCount": 4,
                "values": [{"id": "93"}, {"id": "94"}, {"id": "95"}, {"id": "96"}],
                "valuesGroups": [
                    {"type": "top", "valuesIds": ["93", "94", "95"]},
                    {"type": "all", "valuesIds": ["93", "94", "95", "96"]},
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_multinid_filters_all_checked(self):
        """проверяем, что при флаге showVendors=all игнорируются лимиты на количетво значений enum-параметров и соблюдается логика для checked-значений"""

        query = 'place=prime&nid=3201&glfilter=933:49,69;935:91;936:94,95&showVendors=all&rearr-factors=market_use_childs_for_nid_to_hid=1;market_enrich_multinid_filters=1'
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                "id": "933",
                "valuesCount": 28,
                "values": [
                    {"id": "49"},
                    {"id": "41"},
                    {"id": "42"},
                    {"id": "43"},
                    {"id": "44"},
                    {"id": "45"},
                    {"id": "46"},
                    {"id": "47"},
                    {"id": "48"},
                    {"id": "69"},
                    {"id": "51"},
                    {"id": "52"},
                    {"id": "53"},
                    {"id": "54"},
                    {"id": "55"},
                    {"id": "56"},
                    {"id": "57"},
                    {"id": "58"},
                    {"id": "59"},
                    {"id": "60"},
                    {"id": "61"},
                    {"id": "62"},
                    {"id": "63"},
                    {"id": "64"},
                    {"id": "65"},
                    {"id": "66"},
                    {"id": "67"},
                    {"id": "68"},
                ],
                "valuesGroups": [
                    {"type": "top", "valuesIds": ["49", "69", "41", "42", "43"]},
                    {
                        "type": "all",
                        "valuesIds": [
                            "49",
                            "41",
                            "42",
                            "43",
                            "44",
                            "45",
                            "46",
                            "47",
                            "48",
                            "69",
                            "51",
                            "52",
                            "53",
                            "54",
                            "55",
                            "56",
                            "57",
                            "58",
                            "59",
                            "60",
                            "61",
                            "62",
                            "63",
                            "64",
                            "65",
                            "66",
                            "67",
                            "68",
                        ],
                    },
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.assertFragmentIn(
            response,
            {
                "id": "935",
                "valuesCount": 2,
                "values": [{"id": "91"}, {"id": "92"}],
                "valuesGroups": [{"type": "all", "valuesIds": ["91", "92"]}],
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.assertFragmentIn(
            response,
            {
                "id": "936",
                "valuesCount": 4,
                "values": [{"id": "94"}, {"id": "93"}, {"id": "95"}, {"id": "96"}],
                "valuesGroups": [{"type": "all", "valuesIds": ["94", "93", "95", "96"]}],
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # Вариант, когда при удалении top, его содержимое замещало содержимое списка all (здесь checked-значения смещены к началу списка)
        response = self.report.request_json(query + "&rearr-factors=market_sort_by_dimension_filter_items=0")
        self.assertFragmentIn(
            response,
            {
                "id": "936",
                "valuesCount": 4,
                "values": [{"id": "94"}, {"id": "93"}, {"id": "95"}, {"id": "96"}],
                "valuesGroups": [{"type": "all", "valuesIds": ["94", "95", "93", "96"]}],
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_multinid_filters_count_checked(self):
        """проверяем, что при обогащении мультинида не превышаются лимиты на количетво значений enum-параметров
        при этом top может увеличиться на количество зажатых значений"""

        query = 'place=prime&nid=3201&glfilter=933:49,69;935:91;936:94,95&showVendors=top&rearr-factors=market_use_childs_for_nid_to_hid=1;market_enrich_multinid_filters=1'
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                "id": "933",
                "valuesCount": 28,
                "values": [
                    {"id": "49"},
                    {"id": "41"},
                    {"id": "42"},
                    {"id": "43"},
                    {"id": "44"},
                    {"id": "45"},
                    {"id": "46"},
                    {"id": "47"},
                    {"id": "48"},
                    {"id": "69"},
                    {"id": "51"},
                    {"id": "52"},
                ],
                "valuesGroups": [
                    {"type": "top", "valuesIds": ["49", "69", "41", "42", "43"]},
                    {
                        "type": "all",
                        "valuesIds": ["49", "41", "42", "43", "44", "45", "46", "47", "48", "69", "51", "52"],
                    },
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.assertFragmentIn(
            response,
            {
                "id": "935",
                "valuesCount": 2,
                "values": [{"id": "91"}, {"id": "92"}],
                "valuesGroups": [{"type": "all", "valuesIds": ["91", "92"]}],
            },
            allow_different_len=False,
            preserve_order=True,
        )
        # "top" удалён, т.к. его размер совпал с "all"
        self.assertFragmentIn(
            response,
            {
                "id": "936",
                "valuesCount": 4,
                "values": [{"id": "94"}, {"id": "93"}, {"id": "95"}, {"id": "96"}],
                "valuesGroups": [{"type": "all", "valuesIds": ["94", "93", "95", "96"]}],
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # Вариант, когда при удалении top, его содержимое замещало содержимое списка all (здесь checked-значения смещены к началу списка)
        response = self.report.request_json(query + "&rearr-factors=market_sort_by_dimension_filter_items=0")
        # "top" удалён, т.к. его размер совпал с "all"
        self.assertFragmentIn(
            response,
            {
                "id": "936",
                "valuesCount": 4,
                "values": [{"id": "94"}, {"id": "93"}, {"id": "95"}, {"id": "96"}],
                "valuesGroups": [{"type": "all", "valuesIds": ["94", "95", "93", "96"]}],
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_multinid_filters(self):
        """проверяем, что в фильтры мультинида попадают значения фильтров от всех дочерних категорий, но не должно быть дублирования значений"""

        # Важно, что из ноды 1304(рецепта) добавится только значение "10", тк только оно прописано в рецепте
        response = self.report.request_json(
            'place=prime&nid=1201&rearr-factors=market_use_childs_for_nid_to_hid=1;market_enrich_multinid_filters=1'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "903",
                        "name": "kot-param-903(2301)",
                        "type": "enum",
                        "valuesCount": 6,
                        "values": [{"id": "41"}, {"id": "42"}, {"id": "43"}, {"id": "44"}, {"id": "46"}, {"id": "10"}],
                    },
                ],
                "search": {"total": 7},
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "values": [
                            {
                                "id": "45",
                            },
                        ]
                    }
                ]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "values": [
                            {
                                "id": "47",
                            },
                        ]
                    }
                ]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "values": [
                            {
                                "id": "20",
                            },
                        ]
                    }
                ]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "values": [
                            {
                                "id": "30",
                            },
                        ]
                    }
                ]
            },
        )

    def test_multinid_filters_null_flag(self):
        """при выставленном флаге market_enrich_multinid_filters=0 в фильтры мультинида не должны попасть значения фильтров от дочерних категорий"""
        response = self.report.request_json(
            'place=prime&nid=1201&rearr-factors=market_use_childs_for_nid_to_hid=1;market_enrich_multinid_filters=0'
        )
        self.assertFragmentIn(
            response,
            {
                "id": "903",
                "name": "kot-param-903(2301)",
                "type": "enum",
                "valuesCount": 2,
                "values": [
                    {
                        "id": "41",
                    },
                    {
                        "id": "42",
                    },
                ],
            },
            allow_different_len=False,
        )
        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "values": [
                            {
                                "id": "45",
                            },
                        ]
                    }
                ]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "values": [
                            {
                                "id": "47",
                            },
                        ]
                    }
                ]
            },
        )

    def test_filter_configs(self):
        """
        В новом нав дереве для ноды можно задать конфиг фильтров
        В нем указаны фильтры, которые должны прийти на выдачу и их порядок
        Проверяем, что этот механизм работает (под флагом market_make_glfilters_from_configs)
        """
        # У ноды 11010 есть конфиг gl фильтров
        #
        # Запрос без market_make_glfilters_from_configs, должны вернуться все фильтры кроме kot-param-103, тк он скрыт
        response = self.report.request_json('place=prime&nid=11010&rearr-factors=market_make_glfilters_from_configs=0')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "104",
                        "name": "kot-param-104",
                        "type": "boolean",
                        "values": [{"id": "1", "value": "1", "found": 1}, {"id": "0", "value": "0", "found": 7}],
                    },
                    {
                        "id": "105",
                        "name": "kot-param-105",
                        "type": "number",
                        "values": [{"min": "200500", "max": "200500"}],
                    },
                ],
                "search": {"total": 8},
            },
        )
        # kot-param-103 должен отсутствовать
        self.assertFragmentNotIn(
            response,
            {
                "id": "103",
                "name": "kot-param-103",
                "type": "enum",
                "valuesCount": 1,
                "values": [{"found": 1, "id": "99"}],
            },
        )

        # Запрос с флагом market_make_glfilters_from_configs, должны вернуться только фильтры из списка filters у конфига
        # То есть kot-param-103, kot-param-104 (скрытие для kot-param-103 в этом случае игнорируется)
        # + важен порядок
        response = self.report.request_json('place=prime&nid=11010&rearr-factors=market_make_glfilters_from_configs=1')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "103",
                        "name": "kot-param-103",
                        "type": "enum",
                        "valuesCount": 1,
                        "values": [{"found": 1, "id": "99"}],
                    },
                    {
                        "id": "104",
                        "name": "kot-param-104",
                        "type": "boolean",
                        "values": [{"id": "1", "value": "1", "found": 1}, {"id": "0", "value": "0", "found": 7}],
                    },
                ],
                "search": {"total": 8},
            },
            preserve_order=True,
        )
        # kot-param-105 должен отсутствовать
        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "id": "105",
                        "name": "kot-param-105",
                    }
                ],
                "search": {"total": 8},
            },
        )

        # Запрос с флагом market_make_glfilters_from_configs + при &filterList=all должен возвращаться другой список фильтров - advanced_filters
        # То есть kot-param-105, kot-param-103
        # + важен порядок
        response = self.report.request_json(
            'place=prime&nid=11010&rearr-factors=market_make_glfilters_from_configs=1&filterList=all'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "105",
                        "name": "kot-param-105",
                        "type": "number",
                        "values": [{"min": "200500", "max": "200500"}],
                    },
                    {
                        "id": "103",
                        "name": "kot-param-103",
                        "type": "enum",
                        "valuesCount": 1,
                        "values": [{"found": 1, "id": "99"}],
                    },
                ],
                "search": {"total": 8},
            },
            preserve_order=True,
        )
        # kot-param-104 должен отсутствовать
        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "id": "104",
                        "name": "kot-param-104",
                    }
                ],
                "search": {"total": 8},
            },
        )

    def test_filter_configs_multinid(self):
        """
        В новом нав дереве для ноды можно задать конфиг фильтров
        В нем указаны фильтры, которые должны прийти на выдачу и их порядок
        Проверяем, что этот механизм работает (под флагом market_make_glfilters_from_configs)

        Сейчас этот механизм вообще не смотрит на категории и собирает статистики непосредственно по значениям из выдачи
        Поэтому в мультиниде должны прорастать все значения, которые есть у документов этой ноды на выдаче
        """

        # У ноды 12010 есть конфиг gl фильтров
        #
        # Запрос без market_make_glfilters_from_configs, должны вернуться все фильтры первого потомка + значения из всех детей
        # Вернется только kot-param-100, значения 10 и 20 возьмутся из 23010 (первый потомок), а 30 из 23020
        response = self.report.request_json(
            'place=prime&nid=12010&rearr-factors=market_enrich_multinid_filters=1;market_make_glfilters_from_configs=0'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "100",
                        "name": "kot-param-100",
                        "type": "enum",
                        "valuesCount": 3,
                        "values": [
                            {"found": 1, "initialFound": 1, "id": "10"},
                            {"found": 1, "initialFound": 1, "id": "20"},
                            {"found": 1, "initialFound": 1, "id": "30"},
                        ],
                    }
                ],
                "search": {"total": 5},
            },
        )
        # kot-param-101, kot-param-102 должены отсутствовать
        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "id": "101",
                        "name": "kot-param-102",
                    },
                    {
                        "id": "102",
                        "name": "kot-param-102",
                    },
                ],
                "search": {"total": 5},
            },
        )

        # Запрос с флагом market_make_glfilters_from_configs, должны вернуться только фильтры из списка filters у конфига
        # То есть kot-param-101, kot-param-100
        # + важен порядок
        response = self.report.request_json(
            'place=prime&nid=12010&rearr-factors=market_enrich_multinid_filters=1;market_make_glfilters_from_configs=1'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "101",
                        "name": "kot-param-101",
                        "values": [
                            {"found": 1, "initialFound": 1, "id": "1", "value": "1"},
                            {"found": 4, "initialFound": 4, "id": "0", "value": "0"},
                        ],
                    },
                    {
                        "id": "100",
                        "name": "kot-param-100",
                        "type": "enum",
                        "valuesCount": 3,
                        "values": [
                            {"found": 1, "initialFound": 1, "id": "10"},
                            {
                                "found": 1,
                                "initialFound": 1,
                                "id": "20",
                            },
                            {"found": 1, "initialFound": 1, "id": "30"},
                        ],
                    },
                ],
                "search": {"total": 5},
            },
            preserve_order=True,
        )
        # kot-param-102 должен отсутствовать
        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "id": "102",
                        "name": "kot-param-102",
                    }
                ],
                "search": {"total": 5},
            },
        )

        # Запрос с флагом market_make_glfilters_from_configs + при &filterList=all должен возвращаться другой список фильтров - advanced_filters
        # То есть kot-param-102, kot-param-100, kot-param-101
        # + важен порядок
        response = self.report.request_json(
            'place=prime&nid=12010&rearr-factors=market_enrich_multinid_filters=1;market_make_glfilters_from_configs=1&filterList=all'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": "102", "name": "kot-param-102"},
                    {
                        "id": "100",
                        "name": "kot-param-100",
                        "type": "enum",
                        "valuesCount": 3,
                        "values": [
                            {"found": 1, "initialFound": 1, "id": "10"},
                            {"found": 1, "initialFound": 1, "id": "20"},
                            {"found": 1, "initialFound": 1, "id": "30"},
                        ],
                    },
                    {
                        "id": "101",
                        "name": "kot-param-101",
                        "values": [
                            {"found": 1, "initialFound": 1, "id": "1", "value": "1"},
                            {"found": 4, "initialFound": 4, "id": "0", "value": "0"},
                        ],
                    },
                ],
                "search": {"total": 5},
            },
            preserve_order=True,
        )

        # Проверяем на виртуальном узле nid=555
        # Должны быть фильтры 750, 850, 950
        response = self.report.request_json('place=prime&nid=555&rearr-factors=market_make_glfilters_from_configs=1')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "750",
                        "name": "kot-param-750(1999)",
                        "type": "enum",
                        "valuesCount": 4,
                        "values": [
                            {
                                "found": 1,
                                "initialFound": 1,
                                "id": "4",
                            },
                            {
                                "found": 2,
                                "initialFound": 2,
                                "id": "1",
                            },
                            {
                                "found": 1,
                                "initialFound": 1,
                                "id": "2",
                            },
                            {
                                "found": 1,
                                "initialFound": 1,
                                "id": "3",
                            },
                        ],
                    },
                    {
                        "id": "850",
                        "name": "kot-param-850(1999)",
                        "values": [
                            {"found": 3, "initialFound": 3, "id": "1", "value": "1"},
                            {"found": 3, "initialFound": 3, "id": "0", "value": "0"},
                        ],
                    },
                    {
                        "id": "950",
                        "name": "kot-param-950(1777)",
                        "type": "number",
                        "values": [
                            {"id": "found", "initialMax": "300", "initialMin": "100", "max": "300", "min": "100"}
                        ],
                    },
                ],
                "search": {"total": 6},
            },
            preserve_order=True,
        )

    def test_filter_configs_use_filters(self):
        """
        Проверям, что фильтры конфигов не привязаны к конкретной категории и работают корректно
        """
        # Единственное, почему-то документы, которые прошли фильтр удваиваются в статистике
        # Но я проверил в отдельной ветке без изменений: для самых обычных фильтров это работает аналогично
        # Судя по дебагу, почему-то идет второй запрос на базовые, мб это какая-то особенность лайтов и имитация шардов ¯\_(ツ)_/¯
        #
        # Однако тенденции самих изменений верны

        # Запрос с флагом market_make_glfilters_from_configs, должны вернуться только фильтры из списка filters у конфига
        # То есть kot-param-101, kot-param-100
        # + важен порядок
        # Тут зажат фильтр glfilter=100:1
        response = self.report.request_json(
            'place=prime&nid=12010&glfilter=100:10&rearr-factors=market_make_glfilters_from_configs=1'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "101",
                        "name": "kot-param-101",
                        "values": [
                            {
                                "found": 0,  # тк единственный оффер (kotik4-23020) с true отсеялся
                                "initialFound": 1,
                                "id": "1",
                                "value": "1",
                            },
                            {"found": 2, "initialFound": 5, "id": "0", "value": "0"},  # 1  # 4
                        ],
                    },
                    {
                        "id": "100",
                        "name": "kot-param-100",
                        "type": "enum",
                        "valuesCount": 3,
                        "values": [
                            {"found": 2, "initialFound": 2, "id": "10"},  # 1  # 1
                            {"found": 1, "initialFound": 1, "id": "20"},  # тк параметр енамовый, found не изменяется
                            {"found": 1, "initialFound": 1, "id": "30"},  # тк параметр енамовый, found не изменяется
                        ],
                    },
                ],
                "search": {"total": 1},
            },
            preserve_order=True,
        )

        # На выдаче только kotik4-23020, поэтому все значения kot-param-100 занулятся
        response = self.report.request_json(
            'place=prime&nid=12010&glfilter=101:1&rearr-factors=market_make_glfilters_from_configs=1'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "101",
                        "name": "kot-param-101",
                        "values": [
                            {"found": 2, "initialFound": 2, "id": "1", "value": "1"},  # 1  # 1
                            {"found": 4, "initialFound": 4, "id": "0", "value": "0"},
                        ],
                    },
                    {
                        "id": "100",
                        "name": "kot-param-100",
                        "type": "enum",
                        "valuesCount": 3,
                        "values": [
                            {"found": 0, "initialFound": 1, "id": "10"},
                            {"found": 0, "initialFound": 1, "id": "20"},
                            {"found": 0, "initialFound": 1, "id": "30"},
                        ],
                    },
                ],
                "search": {"total": 1},
            },
            preserve_order=True,
        )

        # Зажимаем &glfilter=750:1
        # На выдаче должно статься два документа + в фильтре 850 должно остаться только значение True
        # А нумерики вообще не найтись, тк у этих доков он не задан
        response = self.report.request_json(
            'place=prime&nid=555&glfilter=750:1&rearr-factors=market_make_glfilters_from_configs=1'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "750",
                        "name": "kot-param-750(1999)",
                        "type": "enum",
                        "valuesCount": 4,
                        "values": [
                            {
                                "found": 1,
                                "initialFound": 1,
                                "id": "4",
                            },
                            {
                                "found": 4,  # 2
                                "initialFound": 4,  # 2
                                "id": "1",
                            },
                            {
                                "found": 1,
                                "initialFound": 1,
                                "id": "2",
                            },
                            {
                                "found": 1,
                                "initialFound": 1,
                                "id": "3",
                            },
                        ],
                    },
                    {
                        "id": "850",
                        "name": "kot-param-850(1999)",
                        "values": [
                            {"found": 4, "initialFound": 5, "id": "1", "value": "1"},
                            {"found": 0, "initialFound": 3, "id": "0", "value": "0"},
                        ],
                    },
                    {
                        "id": "950",
                        "name": "kot-param-950(1777)",
                        "type": "number",
                        "values": [
                            {
                                "id": "found",
                                "initialMax": "300",
                                "initialMin": "100",
                                "max": Absent(),  # тк у найденных доков не задан этот фильтр
                                "min": Absent(),
                            }
                        ],
                    },
                ],
                "search": {"total": 2},
            },
            preserve_order=True,
        )

        # Зажимаем булевый фильтр &glfilter=850:0
        # На выдаче должны остаться только 3
        # И уйдут доки с glfilter=750:1
        # Нумерики станут от 100 до 200
        response = self.report.request_json(
            'place=prime&nid=555&glfilter=850:0&rearr-factors=market_make_glfilters_from_configs=1'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "750",
                        "name": "kot-param-750(1999)",
                        "type": "enum",
                        "valuesCount": 4,
                        "values": [
                            {
                                "found": 2,
                                "initialFound": 2,
                                "id": "4",
                            },
                            {
                                "found": 0,
                                "initialFound": 2,
                                "id": "1",
                            },
                            {
                                "found": 2,
                                "initialFound": 2,
                                "id": "2",
                            },
                            {
                                "found": 2,
                                "initialFound": 2,
                                "id": "3",
                            },
                        ],
                    },
                    {
                        "id": "850",
                        "name": "kot-param-850(1999)",
                        "values": [
                            {"found": 3, "initialFound": 3, "id": "1", "value": "1"},
                            {"found": 6, "initialFound": 6, "id": "0", "value": "0"},
                        ],
                    },
                    {
                        "id": "950",
                        "name": "kot-param-950(1777)",
                        "type": "number",
                        "values": [
                            {"id": "found", "initialMax": "300", "initialMin": "100", "max": "200", "min": "100"}
                        ],
                    },
                ],
                "search": {"total": 3},
            },
            preserve_order=True,
        )

        # Зажимаем фильтр &glfilter=850:0 и &glfilter=750:4
        # Таких доков нет
        response = self.report.request_json(
            'place=prime&nid=555&glfilter=850:1&glfilter=750:4&rearr-factors=market_make_glfilters_from_configs=1'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "750",
                        "name": "kot-param-750(1999)",
                        "type": "enum",
                        "valuesCount": 4,
                        "values": [
                            {
                                "found": 0,
                                "initialFound": 1,
                                "id": "4",
                            },
                            {
                                "found": 2,
                                "initialFound": 2,
                                "id": "1",
                            },
                            {
                                "found": 0,
                                "initialFound": 1,
                                "id": "2",
                            },
                            {
                                "found": 0,
                                "initialFound": 1,
                                "id": "3",
                            },
                        ],
                    },
                    {
                        "id": "850",
                        "name": "kot-param-850(1999)",
                        "values": [
                            {"found": 0, "initialFound": 3, "id": "1", "value": "1"},
                            {"found": 3, "initialFound": 3, "id": "0", "value": "0"},
                        ],
                    },
                    {
                        "id": "950",
                        "name": "kot-param-950(1777)",
                        "type": "number",
                        "values": [
                            {"id": "found", "initialMax": "300", "initialMin": "100", "max": Absent(), "min": Absent()}
                        ],
                    },
                ],
                "search": {"total": 0},
            },
            preserve_order=True,
        )

        # Зажимаем нумерик фильтр glfilter=950:~150
        # Подходящий документ всего один
        response = self.report.request_json(
            'place=prime&nid=555&glfilter=950:~150&rearr-factors=market_make_glfilters_from_configs=1'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "750",
                        "name": "kot-param-750(1999)",
                        "type": "enum",
                        "valuesCount": 4,
                        "values": [
                            {
                                "found": 0,
                                "initialFound": 1,
                                "id": "4",
                            },
                            {
                                "found": 0,
                                "initialFound": 2,
                                "id": "1",
                            },
                            {
                                "found": 0,
                                "initialFound": 1,
                                "id": "2",
                            },
                            {
                                "found": 2,
                                "initialFound": 2,
                                "id": "3",
                            },
                        ],
                    },
                    {
                        "id": "850",
                        "name": "kot-param-850(1999)",
                        "values": [
                            {"found": 0, "initialFound": 3, "id": "1", "value": "1"},
                            {"found": 2, "initialFound": 4, "id": "0", "value": "0"},
                        ],
                    },
                    {
                        "id": "950",
                        "name": "kot-param-950(1777)",
                        "type": "number",
                        "values": [
                            {"id": "found", "initialMax": "300", "initialMin": "100", "max": "300", "min": "100"}
                        ],
                    },
                ],
                "search": {"total": 1},
            },
            preserve_order=True,
        )

    def test_hiding_all_filters(self):
        """
        В нав дереве у ноды может быть настроено скрытие всех gl фильтров, проверяем, что это работает
        """
        # У ноды 1111 hide_gl_filters == True
        response = self.report.request_json('place=prime&nid=1111')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "glprice",
                    },
                    {
                        "id": "manufacturer_warranty",
                    },
                    {
                        "id": "onstock",
                    },
                    {
                        "id": "qrfrom",
                    },
                    {
                        "id": "offer-shipping",
                    },
                    {
                        "id": "fesh",
                    },
                ],
                "search": {"total": 2},
            },
        )
        # А гл фильтры 555, 556 должны отстутствовать
        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "id": "555",
                    },
                    {
                        "id": "556",
                    },
                ],
            },
        )


if __name__ == '__main__':
    main()
