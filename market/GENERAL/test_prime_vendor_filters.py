#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    GLParam,
    GLType,
    HyperCategory,
    Model,
    Offer,
    Vendor,
    FormalizedParam,
    MnPlace,
    RedirectWhiteListRecord,
    FiltersPopularity,
)
from core.testcase import TestCase, main
from core.types.autogen import Const
from core.matcher import NotEmpty, Contains, Absent, LikeUrl
from core.types.fashion_parameters import FashionCategory


TOP_VALUES_COUNT = 12


class _Hids:
    fashion = 7812062
    not_fashion = 1234567


class _VendorIds:
    base = 100000
    fashion_min = base + 7
    fashion_max = base + 31
    not_fashion_min = fashion_max
    not_fashion_max = fashion_max + (fashion_max - fashion_min)
    fashion_no_popularity = base + 100


class T(TestCase):
    @classmethod
    def prepare_multicategory_vendor(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        # Заводим поддерево категорий с несколькими уровнями
        # и общим корнем
        cls.index.hypertree += [
            HyperCategory(
                hid=101,
                children=[
                    HyperCategory(hid=102, children=[HyperCategory(hid=103)]),
                    HyperCategory(hid=104, children=[HyperCategory(hid=105)]),
                ],
            ),
        ]

        # Раскидываем по листам офферы, некоторые
        # приматчиваем к вендору
        cls.index.offers += [
            Offer(hid=103, vendor_id=1),
            Offer(hid=103),
            Offer(hid=105, vendor_id=1),
            Offer(hid=105),
        ]

        # То же самое для моделей
        cls.index.models += [
            Model(hid=103, vendor_id=1),
            Model(hid=103),
            Model(hid=105, vendor_id=1),
            Model(hid=105),
        ]

    def test_multicategory_vendor(self):
        """
        Проверяем, что работает фильтрация по вендору при указании
        листовых и нелистовых категорий в запросе, одной или нескольких
        """

        # Делаем запрос без фильтрации по вендору в категорию, в которую
        # входят все заведённые документы. Убеждаемся, что все в выдаче.
        response = self.report.request_json('place=prime&hid=101')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 8,
                }
            },
        )

        # Делаем запрос с фильтрацией по вендору в листовую категорию. Убеждаемся,
        # что в выдаче все заведённые документы данного вендора
        response = self.report.request_json('place=prime&hid=103&vendor_id=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {"entity": "product", "vendor": {"id": 1}},
                        {"entity": "offer", "vendor": {"id": 1}},
                    ],
                }
            },
            preserve_order=False,
        )

        expected_entities = {
            "search": {
                "total": 4,
                "results": [
                    {"entity": "product", "vendor": {"id": 1}},
                    {"entity": "product", "vendor": {"id": 1}},
                    {"entity": "offer", "vendor": {"id": 1}},
                    {"entity": "offer", "vendor": {"id": 1}},
                ],
            },
        }

        # Делаем запрос с фильтрацией по вендору в нелистовую категорию, в которую
        # входят все заведённые документы. Убеждаемся, что все документы данного вендора в выдаче.
        response = self.report.request_json('place=prime&hid=101&vendor_id=1')
        self.assertFragmentIn(response, expected_entities, preserve_order=False)

        # Делаем запрос с фильтрацией по вендору в несколько категорий, в которые
        # входят все заведённые документы. Убеждаемся, что все документы данного вендора в выдаче.
        response = self.report.request_json('place=prime&hid=103&hid=105&vendor_id=1')
        self.assertFragmentIn(response, expected_entities, preserve_order=False)

    @classmethod
    def prepare_test_multicategory_vendor_filter(cls):
        """
        Заводим поддерево категорий с несколькими уровнями и общим корнем.
        """
        cls.index.hypertree += [
            HyperCategory(
                hid=201,
                children=[
                    HyperCategory(hid=202, children=[HyperCategory(hid=203)]),
                    HyperCategory(hid=204, children=[HyperCategory(hid=205)]),
                ],
            ),
        ]

        # У листовых и корневой категорий заводим стандартный вендорный фильтр
        cls.index.gltypes += [
            GLType(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, vendor=True, hid=201),
            GLType(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, vendor=True, hid=203),
            GLType(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, vendor=True, hid=205),
        ]

        # Создаем по модели в каждой листовой категории, привязываем вендора
        cls.index.models += [
            Model(hid=203, glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=1)]),
            Model(hid=205, glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=2)]),
        ]

        # Создаем оффер, который привязан к корневой категории, привязываем вендора
        cls.index.offers += [
            Offer(hid=201, glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=3)])
        ]

    def test_multicategory_vendor_filter(self):
        # Делаем запрос к нелистовым категориям. Ожидаем увидеть фильтр "Производитель", собранный из ее детей
        response = self.report.request_json('place=prime&hid=202')
        self.assertFragmentIn(
            response,
            {
                'search': NotEmpty(),
                'filters': [{"id": str(Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID), "values": [{"id": "1"}]}],
            },
        )

        # Запрос к корневой категории. Ожидаем:
        # 1. Значения вендорного фильтра составлены из собственных значений фильтра категории + значений ее детей
        # 2. Вендорный фильтр не дублируется на выдаче, а схлопывается в один
        response = self.report.request_json('place=prime&hid=201')
        self.assertFragmentIn(
            response,
            {
                'search': NotEmpty(),
                'filters': [
                    {
                        "id": str(Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID),
                        "values": [{"id": "1"}, {"id": "2"}, {"id": "3"}],
                    }
                ],
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'search': NotEmpty(),
                'filters': [
                    {
                        "id": str(Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID),
                    },
                    {"id": str(Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID)},
                ],
            },
        )

    @classmethod
    def prepare_singlecategory_vendor(cls):
        cls.index.hypertree += [
            HyperCategory(hid=1001),
        ]

        cls.index.gltypes += [
            GLType(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, vendor=True, hid=1001),
        ]

        for i in range(20):
            hyperid = 100 + i
            vendor_id = 100 + i

            cls.index.vendors += [
                Vendor(vendor_id=vendor_id, name="vendor-%.3d" % vendor_id),
            ]

            cls.index.models += [
                Model(
                    hid=1001,
                    vendor_id=vendor_id,
                    hyperid=hyperid,
                    glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=vendor_id)],
                    model_clicks=20 - i,
                ),
            ]

            for j in range(i / 2):
                cls.index.offers += [
                    Offer(
                        hid=1001,
                        vendor_id=vendor_id,
                        hyperid=hyperid,
                        glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=vendor_id)],
                    ),
                ]

    def test_single_category_vendor_filter(self):
        # если вендоров отсортировать по кол-ву офферов (в частности, если не доступны клики и популярность,
        # то получится range(108, 120)
        # у нас популярность есть, поэтому набор другой (достаточно произвольный)

        expected_ids = [104, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116]
        response = self.report.request_json('place=prime&hid=1001')
        self.assertFragmentIn(
            response,
            {
                'search': NotEmpty(),
                'filters': [
                    {
                        "id": str(Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID),
                        "values": [{"id": str(i)} for i in expected_ids],
                    }
                ],
            },
            preserve_order=True,
        )

        # проверяем, что набор не меняется, если выбрали один из выводимых айдишников
        # но выбрнный переносится наверх
        expected_ids.remove(116)
        expected_ids = [116] + expected_ids
        response = self.report.request_json('place=prime&hid=1001&glfilter=7893318%3A116')
        self.assertFragmentIn(
            response,
            {
                'search': NotEmpty(),
                'filters': [
                    {
                        "id": str(Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID),
                        "values": [{"id": str(i)} for i in expected_ids],
                    }
                ],
            },
            preserve_order=True,
        )

        # если запросить скрытый айдишник, он добавится в набор
        # причём выбранные сортируются не по алфавиту (баг или фича - не важно)
        expected_ids = [116, 101] + expected_ids[1:-1]
        response = self.report.request_json('place=prime&hid=1001&glfilter=7893318%3A101%2C116')
        self.assertFragmentIn(
            response,
            {
                'search': NotEmpty(),
                'filters': [
                    {
                        "id": str(Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID),
                        "values": [{"id": str(i)} for i in expected_ids],
                    }
                ],
            },
            preserve_order=True,
        )

    def test_vendor_filter_in_search_literals(self):
        """Под флагом market_use_vendor_literal производитель из glfilter поппадает в литералы"""
        response = self.report.request_json(
            'place=prime&hid=1001&glfilter=7893318:101,116&debug=da' '&rearr-factors=market_use_vendor_literal=1'
        )
        self.assertFragmentIn(
            response,
            {
                "report": {
                    "context": {
                        "collections": {
                            "SHOP": {
                                "text": [Contains('hyper_categ_id:"1001"', '(vendor_id:"101" | vendor_id:"116")')]
                            },
                            "MODEL": {
                                "text": [Contains('hyper_categ_id:"1001"', '(vendor_id:"101" | vendor_id:"116")')]
                            },
                        }
                    }
                }
            },
        )

        self.assertFragmentIn(
            response,
            {
                "id": "7893318",
                "valuesCount": 20,
                "values":
                # кроме выбранных 101 и 116 в фильтр попадают также и другие вендоры
                [{"id": "101"}, {"id": "116"}, {"id": "104"}, {"id": "107"}],
            },
            allow_different_len=True,
        )

    def test_market_use_vendor_literal_on_blue(self):
        """На синем литерал по вендору тоже добавляется в запрос"""

        response = self.report.request_json(
            'place=prime&hid=1001&glfilter=7893318:101,116&debug=da&rgb=blue'
            '&rearr-factors=market_use_vendor_literal=1'
        )
        self.assertFragmentIn(
            response,
            {
                "report": {
                    "context": {
                        "collections": {
                            "SHOP": {"text": [Contains('hyper_categ_id:"1001"', '(vendor_id:"101" | vendor_id:"116")')]}
                        }
                    }
                }
            },
        )

    @classmethod
    def prepare_top_in_all_alphabetical_sorting_type_for_top(cls):
        cls.index.hypertree += [
            HyperCategory(hid=1002),
        ]

        cls.index.gltypes += [
            GLType(
                param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID,
                vendor=True,
                hid=1002,
                short_enum_sort_type=GLType.EnumFieldSortingType.ALPHABETICAL,
                short_enum_count=8,
            ),
        ]
        vendor_count = 20

        for i in range(vendor_count):
            hyperid = 200 + i
            vendor_id = i

            cls.index.vendors += [
                Vendor(vendor_id=vendor_id, name="vendor-%d" % vendor_id),
            ]

            for j in range(vendor_count - i):
                cls.index.offers += [
                    Offer(
                        hid=1002,
                        vendor_id=vendor_id,
                        hyperid=hyperid,
                        glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=vendor_id)],
                    ),
                ]

    def test_top_in_all(self):
        """
        проверяем, что набор top является подмножеством all
        """

        def assert_vendor_filter(response):
            self.assertFragmentIn(
                response,
                {
                    'filters': [
                        {
                            'id': str(Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID),
                            'valuesGroups': [{'type': 'all', 'valuesIds': []}, {'type': 'top', 'valuesIds': []}],
                        }
                    ]
                },
            )

        def find_vendor_filter(response):
            for f in response.root['filters']:
                if f.get('id') == str(Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID):
                    return f
            return None

        def get_type(filt, typ):
            for f in filt['valuesGroups']:
                if f.get('type') == typ:
                    return f
            return None

        def top_in_all(response):
            assert_vendor_filter(response)
            filt = find_vendor_filter(response)
            top = get_type(filt, "top")
            al = get_type(filt, "all")
            return set(top['valuesIds']) <= set(al['valuesIds'])

        response = self.report.request_json('place=prime&hid=1001')
        assert top_in_all(response)

        response = self.report.request_json('place=prime&hid=1001&glfilter=7893318%3A116')
        assert top_in_all(response)

        response = self.report.request_json('place=prime&hid=1001&glfilter=7893318%3A101%2C116')
        assert top_in_all(response)

        response = self.report.request_json('place=prime&hid=1002')
        assert top_in_all(response)

    @classmethod
    def prepare_search_by_brand_name(cls):
        cls.index.hypertree += [
            HyperCategory(hid=2001),
            HyperCategory(hid=2002),
            HyperCategory(hid=2003),
        ]

        for hid in [Const.ROOT_HID, 2001, 2002, 2003]:
            cls.index.gltypes += [
                GLType(
                    param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID,
                    vendor=True,
                    through=True,
                    hid=hid,
                ),
            ]

        cls.index.vendors += [
            Vendor(vendor_id=10784987, name="Рот Фронт"),
            Vendor(vendor_id=10784992, name="Красный Октябрь"),
        ]

        cls.index.offers += [
            Offer(
                title='Карамель Рот Фронт Гусиные лапки',
                hid=2001,
                vendor_id=10784987,
                glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=10784987)],
                waremd5='vqJ03ZwpokiSjM8hG1J1Dw',
            ),
            Offer(
                title='Конфеты Птичье молоко',
                hid=2002,
                vendor_id=10784987,
                glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=10784987)],
                waremd5='F0fR-oWZ0r0QLOHouO4jmQ',
            ),
            Offer(
                title='Пряники Рот Фронт',
                hid=2003,
                vendor_id=10784987,
                glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=10784987)],
                waremd5='G4mi6YByTLiZdFn625B3Eg',
            ),
            Offer(title="Промо оффер", cms_promo_literal="123"),
        ]

        cls.formalizer.on_request(hid=Const.ROOT_HID, query="Рот Фронт").respond(
            formalized_params=[
                FormalizedParam(
                    param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=10784987, param_positions=(0, 9)
                ),
            ]
        )
        cls.formalizer.on_request(hid=Const.ROOT_HID, query="Карамель Рот Фронт").respond(
            formalized_params=[
                FormalizedParam(
                    param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=10784987, param_positions=(9, 18)
                ),
            ]
        )

        # forbid category redirect
        cls.matrixnet.on_place(MnPlace.CATEGORY_REDIRECT, 2001).respond(-4)
        cls.matrixnet.on_place(MnPlace.CATEGORY_REDIRECT, 2002).respond(-4)
        cls.matrixnet.on_place(MnPlace.CATEGORY_REDIRECT, 2003).respond(-4)

        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='Красный Октябрь', url='/search?glfilter=7893318:10784992')
        ]

        cls.index.offers += [
            Offer(
                title='Конфеты Красный Октябрь',
                hid=2001,
                vendor_id=10784992,
                glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=10784992)],
            ),
            Offer(
                title='Конфеты вендора 10784992',
                hid=2001,
                vendor_id=10784992,
                glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=10784992)],
            ),
        ]

    def test_search_by_brand_name(self):
        """Проверяем, что под флагом market_use_vendor_literal_textless
        находятся все товары производителя из gl-фильтра
        https://st.yandex-team.ru/MARKETOUT-44588
        Флаг market_use_vendor_literal_textless=1 раскатан по дефолту
        https://st.yandex-team.ru/MARKETOUT-45607
        """
        glfilter = '7893318:10784987'
        rearr_factors = (
            '&rearr-factors=market_enable_parametric_cut_text=1;'
            'market_through_gl_filters_redirect=1;market_through_gl_filters_on_search=vendor'
        )

        # 1. Текст запроса формализован полностью - находятся все товары вендора
        text = 'Рот Фронт'
        response = self.report.request_json('place=prime&text={}&cvredirect=1'.format(text) + rearr_factors)
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["11"],
                        "rs": [NotEmpty()],
                        "hid": Absent(),
                        "text": Absent(),
                        "suggest_text": [text],
                        "glfilter": [glfilter],
                    },
                }
            },
        )

        rs = response.root['redirect']['params']['rs'][0]

        response = self.report.request_json('place=prime&glfilter={}&rs={}'.format(glfilter, rs) + rearr_factors)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 3,
                    "results": [
                        {
                            "entity": "offer",
                            "titles": {"raw": "Карамель Рот Фронт Гусиные лапки"},
                            "wareId": "vqJ03ZwpokiSjM8hG1J1Dw",
                            "vendor": {"id": 10784987, "name": "Рот Фронт"},
                        },
                        {
                            "entity": "offer",
                            "titles": {"raw": "Конфеты Птичье молоко"},
                            "wareId": "F0fR-oWZ0r0QLOHouO4jmQ",
                            "vendor": {"id": 10784987, "name": "Рот Фронт"},
                        },
                        {
                            "entity": "offer",
                            "titles": {"raw": "Пряники Рот Фронт"},
                            "wareId": "G4mi6YByTLiZdFn625B3Eg",
                            "vendor": {"id": 10784987, "name": "Рот Фронт"},
                        },
                    ],
                }
            },
            preserve_order=False,
        )

        # 2. Запрос с отжатым gl-фильтром
        for request in [
            'place=prime&rs={}'.format(rs) + rearr_factors,
            'place=prime&suggest_text={}'.format(text) + rearr_factors,
            'place=prime&suggest_text={}&rs={}'.format(text, rs) + rearr_factors,
        ]:
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 2,
                        "results": [
                            {
                                "entity": "offer",
                                "titles": {"raw": "Карамель Рот Фронт Гусиные лапки"},
                                "wareId": "vqJ03ZwpokiSjM8hG1J1Dw",
                                "vendor": {"id": 10784987, "name": "Рот Фронт"},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "Пряники Рот Фронт"},
                                "wareId": "G4mi6YByTLiZdFn625B3Eg",
                                "vendor": {"id": 10784987, "name": "Рот Фронт"},
                            },
                        ],
                    }
                },
                preserve_order=False,
            )

        # 3. Текст запроса формализован частично
        text = 'Карамель Рот Фронт'
        response = self.report.request_json('place=prime&text={}&cvredirect=1'.format(text) + rearr_factors)
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["11"],
                        "rs": [NotEmpty()],
                        "hid": Absent(),
                        "text": [text],
                        "suggest_text": Absent(),
                        "glfilter": [glfilter],
                    },
                }
            },
        )

        rs = response.root['redirect']['params']['rs'][0]

        response = self.report.request_json(
            'place=prime&text={}&glfilter={}&rs={}'.format(text, glfilter, rs) + rearr_factors
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {
                            "entity": "offer",
                            "titles": {"raw": "Карамель Рот Фронт Гусиные лапки"},
                            "wareId": "vqJ03ZwpokiSjM8hG1J1Dw",
                            "vendor": {"id": 10784987, "name": "Рот Фронт"},
                        },
                        {
                            "entity": "offer",
                            "titles": {"raw": "Пряники Рот Фронт"},
                            "wareId": "G4mi6YByTLiZdFn625B3Eg",
                            "vendor": {"id": 10784987, "name": "Рот Фронт"},
                        },
                    ],
                }
            },
            preserve_order=False,
        )

        # 4. Если в запросе есть другие фильтры, то запрос остается бестекстовым
        response = self.report.request_json('place=prime&suggest_text=Рот Фронт&filter-by-cms-promo=123')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "entity": "offer",
                            "titles": {"raw": "Промо оффер"},
                        },
                    ],
                }
            },
            allow_different_len=False,
        )

    def test_redirects_on_search_by_brand_name(self):
        """Проверяем, что после редиректа в мультакатегорию на бестекстовом поиске находятся все товары производителя из gl-фильтра
        https://st.yandex-team.ru/MARKETOUT-45607
        """

        # 1. Бестекстовый поиск - находятся все товары вендора
        response = self.report.request_json('place=prime&text=Красный+Октябрь&cvredirect=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of(
                        "/search?glfilter=7893318:10784992&suggest_text=Красный+Октябрь&was_redir=1&rt=10"
                    )
                }
            },
        )

        response = self.report.request_json('place=prime&suggest_text=Красный+Октябрь&glfilter=7893318:10784992')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {
                            "entity": "offer",
                            "titles": {"raw": "Конфеты Красный Октябрь"},
                            "vendor": {"id": 10784992, "name": "Красный Октябрь"},
                        },
                        {
                            "entity": "offer",
                            "titles": {"raw": "Конфеты вендора 10784992"},
                            "vendor": {"id": 10784992, "name": "Красный Октябрь"},
                        },
                    ],
                }
            },
        )

        # 2. Текстовый поиск - находятся не все товары вендора
        response = self.report.request_json('place=prime&text=Красный+Октябрь&glfilter=7893318:10784992')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "entity": "offer",
                            "titles": {"raw": "Конфеты Красный Октябрь"},
                            "vendor": {"id": 10784992, "name": "Красный Октябрь"},
                        },
                    ],
                }
            },
        )

    @classmethod
    def prepare_test_fashion_vendor_sorting(cls):
        """
        Создаем иерархию категорий фешн
        """
        cls.index.fashion_categories += [
            FashionCategory("FASHION_CATEGORY", _Hids.fashion),
        ]

        """
        Заводим категории fashion и not fashion.
        """
        cls.index.hypertree += [
            HyperCategory(hid=_Hids.fashion),
            HyperCategory(hid=_Hids.not_fashion),
        ]

        # Заводим вендорные фильтры
        cls.index.gltypes += [
            GLType(
                param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID,
                gltype=GLType.ENUM,
                vendor=True,
                hid=_Hids.fashion,
                values=list(range(_VendorIds.fashion_min, _VendorIds.fashion_max)) + [_VendorIds.fashion_no_popularity],
            ),
            GLType(
                param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID,
                gltype=GLType.ENUM,
                vendor=True,
                hid=_Hids.not_fashion,
                values=list(range(_VendorIds.not_fashion_min, _VendorIds.not_fashion_max)),
            ),
        ]

        for i in range(_VendorIds.fashion_min, _VendorIds.fashion_max):
            # Создаем fashion офферы, привязываем вендоров
            cls.index.offers += [
                Offer(
                    hid=_Hids.fashion,
                    vendor_id=i,
                    glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=i)],
                ),
            ]

        # Создаем fashion оффер вендора без популярности
        cls.index.offers += [
            Offer(
                hid=_Hids.fashion,
                vendor_id=_VendorIds.fashion_no_popularity,
                glparams=[
                    GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=_VendorIds.fashion_no_popularity)
                ],
            ),
        ]

        for i in range(_VendorIds.not_fashion_min, _VendorIds.not_fashion_max):
            # Создаем not fashion офферы, привязываем вендоров
            cls.index.offers += [
                Offer(
                    hid=_Hids.not_fashion,
                    vendor_id=i,
                    glparams=[GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=i)],
                ),
            ]

        fashion_vendors_count = _VendorIds.fashion_max - _VendorIds.fashion_min
        not_fashion_vendors_count = _VendorIds.not_fashion_max - _VendorIds.not_fashion_min

        # Заполняем популярность вендоров
        # Чем больше vendor_id тем меньше порядковый номер (параметр popularity) в списке популярных вендоров, тем выше популярность
        # Поэтому сортировка по убыванию популярности это сортировка по убыванию vendor_id
        cls.index.filters_popularity += [
            FiltersPopularity(
                hid=_Hids.fashion,
                key=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID,
                value=vendor_id,
                popularity=fashion_vendors_count - idx,
            )
            for idx, vendor_id in enumerate(range(_VendorIds.fashion_min, _VendorIds.fashion_max))
        ]

        cls.index.filters_popularity += [
            FiltersPopularity(
                hid=_Hids.not_fashion,
                key=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID,
                value=vendor_id,
                popularity=not_fashion_vendors_count - idx,
            )
            for idx, vendor_id in enumerate(range(_VendorIds.not_fashion_min, _VendorIds.not_fashion_max))
        ]

    def test_vendor_filter_sorting(self):
        # Проверяем что:
        # В случае если market_sort_fashion_vendors_by_popularity=1 и запрашиваются товары фешн категории, сортировка вендоров будет по популярности
        # (все офферы без заданной популярности, в конце списка)
        # В случае если market_sort_fashion_vendors_by_popularity=0 или товары нефешн категории, сортировка вендоров будет по алфавиту

        request = 'place=prime&hid={}&showVendors={}&debug=1&rearr-factors=market_sort_fashion_vendors_by_popularity={}'

        # Если сортировка по популярности на выдаче:
        # в случае 'top' 12 самых популярных вендоров в порядке убывания популярности
        # в случае 'all' все вендоры c заданной популярностью по убыванию популярности + вендор без популярности в конце списка
        fashion_vendors_sorted_by_popularity = {
            'top': [
                {"id": str(id)}
                for id in reversed(
                    range(
                        _VendorIds.fashion_max,
                        max(_VendorIds.fashion_max - TOP_VALUES_COUNT - 1, _VendorIds.fashion_min),
                    )
                )
            ],
            'all': [{"id": str(id)} for id in reversed(range(_VendorIds.fashion_max, _VendorIds.fashion_min))]
            + [{"id": str(_VendorIds.fashion_no_popularity)}],
        }

        # Если сортировка по алфавиту:
        # в случае 'top' - 12 первых по алфавиту вендоров
        # в случае 'all' - полный список вендоров, отсортированный по алфавиту
        fashion_vendors_sorted_alphabetically = {
            'top': [
                {"id": str(id)}
                for id in range(
                    _VendorIds.fashion_min, min(_VendorIds.fashion_min + TOP_VALUES_COUNT, _VendorIds.fashion_max)
                )
            ],
            'all': [{"id": str(id)} for id in range(_VendorIds.fashion_min, _VendorIds.fashion_max)],
        }

        not_fashion_vendors_sorted_alphabetically = {
            'top': [
                {"id": str(id)}
                for id in range(
                    _VendorIds.not_fashion_min,
                    min(_VendorIds.not_fashion_min + TOP_VALUES_COUNT, _VendorIds.not_fashion_max),
                )
            ],
            'all': [{"id": str(id)} for id in range(_VendorIds.not_fashion_min, _VendorIds.not_fashion_max)],
        }

        # Проверяем выдачу фильтра вендоров категории фешн
        for sort_vendors_by_popularity in [0, 1]:
            for show_vendors in ['top', 'all']:
                response = self.report.request_json(
                    request.format(_Hids.fashion, show_vendors, sort_vendors_by_popularity)
                )
                self.assertFragmentIn(
                    response,
                    {
                        'filters': [
                            {
                                "id": str(Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID),
                                "values": fashion_vendors_sorted_by_popularity[show_vendors]
                                if bool(sort_vendors_by_popularity)
                                else fashion_vendors_sorted_alphabetically[show_vendors],
                            },
                        ]
                    },
                    preserve_order=True,
                )

        # Проверяем выдачу фильтра вендоров категории нефешн
        for sort_vendors_by_popularity in [0, 1]:
            for show_vendors in ['top', 'all']:
                response = self.report.request_json(
                    request.format(_Hids.not_fashion, show_vendors, sort_vendors_by_popularity)
                )
                self.assertFragmentIn(
                    response,
                    {
                        'filters': [
                            {
                                "id": str(Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID),
                                "values": not_fashion_vendors_sorted_alphabetically[show_vendors],
                            },
                        ]
                    },
                    preserve_order=True,
                )


if __name__ == '__main__':
    main()
