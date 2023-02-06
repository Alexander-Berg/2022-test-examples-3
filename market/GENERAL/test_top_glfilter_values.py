#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import GLParam, GLType, GLValue, Offer, Shop
from core.testcase import TestCase, main
from core.matcher import Equal


class T(TestCase):
    @classmethod
    def prepare_manual_sorting_tops_and_all(cls):
        cls.index.gltypes += [
            GLType(
                param_id=203,
                hid=888,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1),
                    GLValue(value_id=2),
                    GLValue(value_id=3, short_enum_position=2),
                    GLValue(value_id=4, short_enum_position=1),
                    GLValue(value_id=5),
                    GLValue(value_id=6),
                    GLValue(value_id=7, short_enum_position=3),
                ],
                cluster_filter=True,
                short_enum_sort_type=GLType.EnumFieldSortingType.MANUAL,
                short_enum_count=3,
            ),
        ]

        for gl_val in range(1, 8):
            cls.index.offers += [
                Offer(
                    title='title %d' % gl_val,
                    price=42,
                    hid=888,
                    glparams=[
                        GLParam(param_id=203, value=gl_val),
                    ],
                ),
            ]

    def test_manual_sorting_tops_and_all(self):
        """
        Проверяем, что при установленной ручной сортировке и установленном количестве топов меньше,
        чем общее количество значений в гл фильтре
        выводятся поля "valuesGroups" c top - в заданном порядке
                                      c all - в обычном порядке по алфавиту

        """
        response = self.report.request_json('place=prime&hid=888')
        self.assertFragmentIn(
            response,
            {
                "id": "203",
                "valuesGroups": [
                    {"type": "top", "valuesIds": Equal(["4", "3", "7"])},
                    {"type": "all", "valuesIds": Equal(["1", "2", "3", "4", "5", "6", "7"])},
                ],
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_manual_sorting_just_top(cls):
        cls.index.gltypes += [
            GLType(
                param_id=204,
                hid=889,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, short_enum_position=4),
                    GLValue(value_id=2, short_enum_position=3),
                    GLValue(value_id=3, short_enum_position=2),
                    GLValue(value_id=4, short_enum_position=1),
                ],
                cluster_filter=True,
                short_enum_sort_type=GLType.EnumFieldSortingType.MANUAL,
                short_enum_count=4,
            ),
        ]

        for gl_val in range(1, 5):
            cls.index.offers += [
                Offer(
                    title='title %d' % gl_val,
                    price=42,
                    hid=889,
                    glparams=[
                        GLParam(param_id=204, value=gl_val),
                    ],
                ),
            ]

    def test_manual_sorting_no_just_top(self):
        """
        Проверяем, что при установленной ручной сортировке и установленном количестве топов меньше,
        чем общее количество значений в гл фильтре
        выводятся поля "valuesGroups" c top - нет ничего
                                      c all - в заданном для top порядке

        """
        response = self.report.request_json('place=prime&hid=889')
        self.assertFragmentIn(
            response,
            {"id": "204", "valuesGroups": [{"type": "all", "valuesIds": Equal(["4", "3", "2", "1"])}]},
            preserve_order=True,
        )

        self.assertFragmentNotIn(response, {"id": "204", "valuesGroups": [{"type": "top"}]})

    @classmethod
    def prepare_offercount_sorting_tops_and_all(cls):
        cls.index.gltypes += [
            GLType(
                param_id=205,
                hid=890,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1),
                    GLValue(value_id=2),
                    GLValue(value_id=3),
                    GLValue(value_id=4),
                    GLValue(value_id=5),
                    GLValue(value_id=6),
                    GLValue(value_id=7),
                ],
                cluster_filter=True,
                short_enum_sort_type=GLType.EnumFieldSortingType.OFFERS_COUNT,
                short_enum_count=3,
            ),
        ]

        for gl_val in range(1, 8):
            for i in range(gl_val):
                cls.index.offers += [
                    Offer(
                        title='title %d-%d' % (gl_val, i),
                        price=42,
                        hid=890,
                        glparams=[
                            GLParam(param_id=205, value=gl_val),
                        ],
                    ),
                ]

    def test_offercount_sorting_tops_and_all(self):
        """
        Проверяем, что при установленной сортировке по офферам и установленном количестве топов меньше,
        чем общее количество значений в гл фильтре
        выводятся поля "valuesGroups" c top - в порядке по количеству офферов
                                      c all - в обычном порядке по алфавиту

        """
        response = self.report.request_json('place=prime&hid=890')
        self.assertFragmentIn(
            response,
            {
                "id": "205",
                "valuesGroups": [
                    {"type": "top", "valuesIds": Equal(["5", "6", "7"])},
                    {"type": "all", "valuesIds": Equal(["1", "2", "3", "4", "5", "6", "7"])},
                ],
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_offercount_sorting_just_tops(cls):
        cls.index.gltypes += [
            GLType(
                param_id=206,
                hid=891,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1),
                    GLValue(value_id=2),
                    GLValue(value_id=3),
                ],
                cluster_filter=True,
                short_enum_sort_type=GLType.EnumFieldSortingType.OFFERS_COUNT,
                short_enum_count=4,
            ),
        ]

        for gl_val in range(1, 4):
            for i in range(gl_val):
                cls.index.offers += [
                    Offer(
                        title='title %d-%d' % (gl_val, i),
                        price=42,
                        hid=891,
                        glparams=[
                            GLParam(param_id=206, value=gl_val),
                        ],
                    ),
                ]

    def test_offercount_sorting_just_tops(self):
        """
        Проверяем, что при установленной сортировке по офферам и установленном количестве топов меньше,
        чем общее количество значений в гл фильтре
        выводятся поля "valuesGroups" c top - нет ничего
                                      c all - по количеству офферов
        """
        response = self.report.request_json('place=prime&hid=891')
        self.assertFragmentIn(
            response,
            {"id": "206", "valuesGroups": [{"type": "all", "valuesIds": Equal(["1", "2", "3"])}]},
            preserve_order=True,
        )

        self.assertFragmentNotIn(response, {"id": "206", "valuesGroups": [{"type": "top"}]})

    @classmethod
    def prepare_alphabetical_sort_tops_and_all(cls):
        cls.index.gltypes += [
            GLType(
                param_id=207,
                hid=892,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text="bizon"),
                    GLValue(value_id=2, text="arcadia"),
                    GLValue(value_id=3, text="zoo"),
                    GLValue(value_id=4, text="cat"),
                    GLValue(value_id=5, text="parrot"),
                    GLValue(value_id=6, text="fate"),
                    GLValue(value_id=7, text="Zzyzx"),
                ],
                cluster_filter=True,
                short_enum_sort_type=GLType.EnumFieldSortingType.ALPHABETICAL,
                short_enum_count=3,
            ),
        ]

        for gl_val in range(1, 8):
            cls.index.offers += [
                Offer(
                    title='title %d' % gl_val,
                    price=42,
                    hid=892,
                    glparams=[
                        GLParam(param_id=207, value=gl_val),
                    ],
                ),
            ]

    def test_alphabetical_sort_tops_and_all(self):
        """
        проверяем, что при установленной сортировке топов по алфавиту.
        в топах и в all выводятся значения по алфавиту
        """
        response = self.report.request_json('place=prime&hid=892')
        self.assertFragmentIn(
            response,
            {
                "id": "207",
                "valuesGroups": [
                    {"type": "top", "valuesIds": Equal(["2", "1", "4"])},
                    {"type": "all", "valuesIds": Equal(["2", "1", "4", "6", "5", "3", "7"])},
                ],
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_offercount_sorting_tops_with_manual_intop(cls):
        cls.index.gltypes += [
            GLType(
                param_id=208,
                hid=893,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, short_enum_intop=True),
                    GLValue(value_id=2),
                    GLValue(value_id=3, short_enum_intop=True),
                    GLValue(value_id=4),
                    GLValue(value_id=5),
                    GLValue(value_id=6),
                    GLValue(value_id=7),
                    GLValue(value_id=8),
                ],
                cluster_filter=True,
                short_enum_sort_type=GLType.EnumFieldSortingType.OFFERS_COUNT,
                short_enum_count=4,
            ),
        ]

        for gl_val in range(1, 9):
            for i in range(gl_val):
                cls.index.offers += [
                    Offer(
                        title='title %d-%d' % (gl_val, i),
                        price=42,
                        hid=893,
                        glparams=[
                            GLParam(param_id=208, value=gl_val),
                        ],
                    ),
                ]

    def test_offercount_sorting_tops_with_manual_intop(self):
        """
        Проверяем, что при установленной сортировке параметров по офферам и частью офферов, помеченных флагом InTop,
        выводятся поля "valuesGroups" c top - в порядке по количеству офферов, но параметры 3 и 1 попадают обязательно,
                                              т.к. помечены InTop
                                      c all - в обычном порядке по алфавиту

        """
        response = self.report.request_json('place=prime&hid=893')
        self.assertFragmentIn(
            response,
            {
                "id": "208",
                "valuesGroups": [
                    {"type": "top", "valuesIds": Equal(["1", "3", "7", "8"])},
                    {"type": "all", "valuesIds": Equal(["1", "2", "3", "4", "5", "6", "7", "8"])},
                ],
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_default_sorting_count_5(cls):
        cls.index.gltypes += [
            GLType(
                param_id=209,
                hid=894,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1),
                    GLValue(value_id=2),
                    GLValue(value_id=3),
                    GLValue(value_id=4),
                    GLValue(value_id=5),
                ],
                cluster_filter=True,
            ),
        ]

        for gl_val in range(1, 6):
            for i in range(gl_val):
                cls.index.offers += [
                    Offer(
                        title='title %d-%d' % (gl_val, i),
                        price=42,
                        hid=894,
                        glparams=[
                            GLParam(param_id=209, value=gl_val),
                        ],
                    ),
                ]

    def test_default_sorting_count_less_5(self):
        """
        Проверяем, что если для значения не установлена дефолтная сортировка и количество элементов в топе,
        а число элементов <= 5, то в выдаче есть только all, отсортированный по количеству офферов,
        и нет top
        """
        response = self.report.request_json('place=prime&hid=894')
        self.assertFragmentIn(
            response,
            {
                "id": "209",
                "valuesGroups": [
                    {
                        "type": "all",
                        "valuesIds": Equal(
                            [
                                "1",
                                "2",
                                "3",
                                "4",
                                "5",
                            ]
                        ),
                    }
                ],
            },
            preserve_order=True,
        )

        self.assertFragmentNotIn(response, {"id": "209", "valuesGroups": [{"type": "top"}]})

    @classmethod
    def prepare_default_sorting_count_more_than_5(cls):
        cls.index.gltypes += [
            GLType(
                param_id=210,
                hid=895,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1),
                    GLValue(value_id=2),
                    GLValue(value_id=3),
                    GLValue(value_id=4),
                    GLValue(value_id=5),
                    GLValue(value_id=6),
                    GLValue(value_id=7),
                    GLValue(value_id=8),
                    GLValue(value_id=9),
                ],
                cluster_filter=True,
            ),
        ]

        for gl_val in range(1, 10):
            for i in range(gl_val):
                cls.index.offers += [
                    Offer(
                        title='title %d-%d' % (gl_val, i),
                        price=42,
                        hid=895,
                        glparams=[
                            GLParam(param_id=210, value=gl_val),
                        ],
                    ),
                ]

    def test_default_sorting_count_more_than_5(self):
        """
        Проверяем, что если для фильтра не установлена дефолтная сортировка и количество элементов в топе,
        а число элементов > 5 более чем на 3, то в выдаче есть all, отсортированный по алфавиту,
        и top c 5-ю элементами, отоортированный по количеству офферов
        """
        response = self.report.request_json('place=prime&hid=895')
        self.assertFragmentIn(
            response,
            {
                "id": "210",
                "valuesGroups": [
                    {
                        "type": "top",
                        "valuesIds": Equal(
                            [
                                "5",
                                "6",
                                "7",
                                "8",
                                "9",
                            ]
                        ),
                    },
                    {
                        "type": "all",
                        "valuesIds": Equal(
                            [
                                "1",
                                "2",
                                "3",
                                "4",
                                "5",
                                "6",
                                "7",
                                "8",
                                "9",
                            ]
                        ),
                    },
                ],
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_default_sorting_count_with_sorting_type_defined(cls):
        cls.index.gltypes += [
            GLType(
                param_id=211,
                hid=896,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1),
                    GLValue(value_id=2),
                    GLValue(value_id=3),
                    GLValue(value_id=4),
                    GLValue(value_id=5),
                    GLValue(value_id=6),
                    GLValue(value_id=7),
                    GLValue(value_id=8),
                    GLValue(value_id=9),
                ],
                cluster_filter=True,
                short_enum_sort_type=GLType.EnumFieldSortingType.ALPHABETICAL,
            ),
        ]

        for gl_val in range(1, 10):
            for i in range(gl_val):
                cls.index.offers += [
                    Offer(
                        title='title %d-%d' % (gl_val, i),
                        price=42,
                        hid=896,
                        glparams=[
                            GLParam(param_id=211, value=gl_val),
                        ],
                    ),
                ]

    def test_default_sorting_count_with_sorting_type_defined(self):
        """
        Проверяем, что если для фильтра установлена дефолтная сортировка и не установлено количество элементов в топе,
        количество элементов становится 5
        """
        response = self.report.request_json('place=prime&hid=896')
        self.assertFragmentIn(
            response,
            {
                "id": "211",
                "valuesGroups": [
                    {
                        "type": "top",
                        "valuesIds": Equal(
                            [
                                "1",
                                "2",
                                "3",
                                "4",
                                "5",
                            ]
                        ),
                    },
                    {
                        "type": "all",
                        "valuesIds": Equal(
                            [
                                "1",
                                "2",
                                "3",
                                "4",
                                "5",
                                "6",
                                "7",
                                "8",
                                "9",
                            ]
                        ),
                    },
                ],
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_default_sorting_type(cls):
        cls.index.gltypes += [
            GLType(
                param_id=212,
                hid=897,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1),
                    GLValue(value_id=2),
                    GLValue(value_id=3),
                    GLValue(value_id=4),
                    GLValue(value_id=5),
                    GLValue(value_id=6),
                    GLValue(value_id=7),
                ],
                cluster_filter=True,
                short_enum_count=3,
            ),
        ]

        for gl_val in range(1, 8):
            for i in range(gl_val):
                cls.index.offers += [
                    Offer(
                        title='title %d-%d' % (gl_val, i),
                        price=42,
                        hid=897,
                        glparams=[
                            GLParam(param_id=212, value=gl_val),
                        ],
                    ),
                ]

    def test_default_sorting_type(self):
        """
        Проверяем, что если для фильтра не установлена никакая сортировка,
        но установлено число элементов в топе,
        то автоматически используется сортировка
        по количеству офферов
        """
        response = self.report.request_json('place=prime&hid=897')
        self.assertFragmentIn(
            response,
            {
                "id": "212",
                "valuesGroups": [
                    {
                        "type": "top",
                        "valuesIds": Equal(
                            [
                                "5",
                                "6",
                                "7",
                            ]
                        ),
                    },
                    {
                        "type": "all",
                        "valuesIds": Equal(
                            [
                                "1",
                                "2",
                                "3",
                                "4",
                                "5",
                                "6",
                                "7",
                            ]
                        ),
                    },
                ],
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_vendor_sorting(cls):
        """
        Создаем gl-параметр с семью возможными значениями, из которых 3
        наиболее популярных входят в топ

        Создаем два набора офферов для каждого из значений gl-параметра,
        причем с возрастанием id увеличивается популярность
        """
        cls.index.gltypes += [
            GLType(
                param_id=213,
                hid=898,
                vendor=True,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1),
                    GLValue(value_id=2),
                    GLValue(value_id=3),
                    GLValue(value_id=4),
                    GLValue(value_id=5),
                    GLValue(value_id=6),
                    GLValue(value_id=7),
                    GLValue(value_id=8),
                    GLValue(value_id=9),
                ],
                short_enum_sort_type=GLType.EnumFieldSortingType.OFFERS_COUNT,
                short_enum_count=3,
            ),
        ]

        for gl_val in range(1, 10):
            for i in range(gl_val):
                cls.index.offers += [
                    Offer(
                        title='vendor-big-set test %d-%d' % (gl_val, i),
                        hid=898,
                        glparams=[GLParam(param_id=213, value=gl_val)],
                    ),
                ]

        for gl_val in range(2, 5):
            for i in range(gl_val):
                cls.index.offers += [
                    Offer(
                        title='VendorSmallSet test %d-%d' % (gl_val, i),
                        hid=898,
                        glparams=[GLParam(param_id=213, value=gl_val)],
                    ),
                ]

    def test_vendor_sorting(self):
        """
        Делаем запрос проверяем, что вернулся только top с тремя id.
        Проверяем, что в values тоже только три значения

        Делаем запрос c showVendors=all, проверяем, что
        вернулся top с тремя id и all со всеми семью id. Проверяем, что в
        values присутствуют все семь возможных значений
        """
        for query in [
            'place=prime&hid=898&text=vendor-big-set&showVendors=top',
            'place=prime&hid=898&text=vendor-big-set&showVendors=all',
        ]:
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {
                    "id": "213",
                    "valuesGroups": [
                        {
                            "type": "top",
                            "valuesIds": [
                                "9",
                                "8",
                                "7",
                            ],
                        },
                        {
                            "type": "all",
                            "valuesIds": [
                                "1",
                                "2",
                                "3",
                                "4",
                                "5",
                                "6",
                                "7",
                                "8",
                                "9",
                            ],
                        },
                    ],
                    "values": [
                        {"id": "1"},
                        {"id": "2"},
                        {"id": "3"},
                        {"id": "4"},
                        {"id": "5"},
                        {"id": "6"},
                        {"id": "7"},
                        {"id": "8"},
                        {"id": "9"},
                    ],
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_vendor_sorting_checked(self):
        """
        Что тестируем: в случае &use-best-top=1 для параметра вендора с
        указанием выбранных фильтров возвращаются значения из топа и
        выбранные значения

        Делаем запрос с и glfilters 1 и 5. Проверяем,
        что вернулся top с пятью id (для трех элементов были найдены значения
        и еще два пришли как выбранные). Проверяем, что в values пять значений

        """

        for query in [
            'place=prime&hid=898&text=vendor-big-set&glfilter=213:1,2&showVendors=top',
            'place=prime&hid=898&text=vendor-big-set&glfilter=213:1,2&showVendors=all',
        ]:
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {
                    "id": "213",
                    "valuesGroups": [
                        {
                            "type": "top",
                            "valuesIds": [
                                "2",
                                "1",
                                "9",
                                "8",
                                "7",
                            ],
                        },
                        {
                            "type": "all",
                            "valuesIds": [
                                "1",
                                "2",
                                "3",
                                "4",
                                "5",
                                "6",
                                "7",
                                "8",
                                "9",
                            ],
                        },
                    ],
                    "values": [
                        {"id": "1"},
                        {"id": "2"},
                        {"id": "3"},
                        {"id": "4"},
                        {"id": "5"},
                        {"id": "6"},
                        {"id": "7"},
                        {"id": "8"},
                        {"id": "9"},
                    ],
                },
                preserve_order=True,
                allow_different_len=False,
            )

    @classmethod
    def prepare_shop_sorting(cls):
        """
        Создаем 18 магазинов и офферы в них, причем с увеличением
        id магазина число офферов возрастает
        """
        for shop_val in range(1, 19):
            Shop(fesh=300 + shop_val, priority_region=213)

            for i in range(shop_val):
                cls.index.offers += [Offer(title='ShopBigSet test %d-%d' % (shop_val, i), hid=899, fesh=300 + shop_val)]

    def test_shop_sorting(self):
        """
        Фиксируем как работает сортировка фильтров по магазину
        (без  дозапроса за фильтрами)

        Делаем запрос, проверяем, что вернулся только top
        с двенадцатью id. Проверяем, что в values тоже 12 значений

        Делаем запрос с &show-shops=all, проверяем, что
        вернулся all со восемнадцатью id. Проверяем, что в
        values присутствуют 18 значений
        """
        response = self.report.request_json('place=prime&hid=899&text=ShopBigSet&show-shops=top')

        self.assertFragmentIn(
            response,
            {
                "id": "fesh",
                "values": [
                    {"id": "307"},
                    {"id": "308"},
                    {"id": "309"},
                    {"id": "310"},
                    {"id": "311"},
                    {"id": "312"},
                    {"id": "313"},
                    {"id": "314"},
                    {"id": "315"},
                    {"id": "316"},
                    {"id": "317"},
                    {"id": "318"},
                ],
            },
            allow_different_len=False,
        )

    def test_shop_sorting_checked(self):
        """
        Фиксируем как сортируются магазины при выбранном фильтре fesh
        (делается отдельный дозапрос за фильтрами и результаты сливаются)

        Делаем запрос с fesh 302, 303 и 304. Проверяем,
        что вернулся top с пятнадцатью id (12 элементов в топе по-умолчанию
        и еще 3 пришли как выбранные). Проверяем, что в values 15 значений

        """
        response = self.report.request_json('place=prime&hid=899&text=ShopBigSet&fesh=302,303,304')

        self.assertFragmentIn(
            response,
            {
                "id": "fesh",
                "values": [
                    {"id": "304"},
                    {"id": "302"},
                    {"id": "303"},
                    {"id": "308"},
                    {"id": "309"},
                    {"id": "310"},
                    {"id": "311"},
                    {"id": "312"},
                    {"id": "313"},
                    {"id": "314"},
                    {"id": "315"},
                    {"id": "316"},
                    {"id": "317"},
                    {"id": "318"},
                    {"id": "301"},
                    {"id": "305"},
                    {"id": "306"},
                    {"id": "307"},
                ],
            },
            allow_different_len=False,
        )

    def test_shop_sorting_checked_all(self):
        """
        Делаем запрос с &show-shops=all и fesh 301,302,303,304,305,306. Проверяем,
        что вернулся all с восемнадцатью id (12 элементов в топе по-умолчанию
        и еще 6 пришли как выбранные). Проверяем, что в values 12 значений

        """
        response = self.report.request_json(
            'place=prime&hid=899&text=ShopBigSet&show-shops=all&fesh=301,302,303,304,305,306'
        )

        self.assertFragmentIn(
            response,
            {
                "id": "fesh",
                "values": [
                    {"id": "305"},
                    {"id": "306"},
                    {"id": "301"},
                    {"id": "302"},
                    {"id": "303"},
                    {"id": "304"},
                    {"id": "309"},
                    {"id": "310"},
                    {"id": "311"},
                    {"id": "312"},
                    {"id": "313"},
                    {"id": "314"},
                    {"id": "315"},
                    {"id": "316"},
                    {"id": "317"},
                    {"id": "318"},
                    {"id": "307"},
                    {"id": "308"},
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
