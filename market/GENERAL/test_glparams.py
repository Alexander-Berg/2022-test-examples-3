#!/usr/bin/env python
# -*- coding: utf-8 -*-
import runner  # noqa

from core.matcher import NotEmpty, NoKey, Absent, Greater, ElementCount

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    DeliveryBucket,
    DeliveryOption,
    FiltersPopularity,
    GLParam,
    GLType,
    GLSizeChart,
    GLValue,
    HyperCategory,
    ImagePickerData,
    MarketSku,
    MnPlace,
    Model,
    NavCategory,
    NewShopRating,
    Offer,
    Outlet,
    ParameterValue,
    PickupBucket,
    PickupOption,
    Promo,
    PromoType,
    Region,
    RegionalModel,
    Shop,
    VCluster,
    Vendor,
)
from core.types.autogen import Const
from unittest import skip

from datetime import datetime
import itertools


DRUGS_HID = 13077405
NON_DRUGS_HID = 13077406

PHONES_HID = 91491

SILVER = 10
ROSE_GOLD = 11
JET_BLACK = 12

NOT_THROUGH_PARAM_ID = 1123
THROUGH_PARAM_ID = 17354681
THROUGH_PARAMS = [17354681, 17354854, 17354844, 17354840, 17354871, 10833154]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.use_delivery_statistics = True
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.gltypes += [
            GLType(param_id=201, hid=1, gltype=GLType.ENUM, hidden=True, values=[1, 2]),
            GLType(param_id=202, hid=1, gltype=GLType.ENUM, hidden=False, values=[3, 4]),
            GLType(param_id=203, hid=1, gltype=GLType.NUMERIC, hidden=True),
        ]

        cls.index.offers += [
            Offer(
                hid=1,
                title='good iphone',
                glparams=[
                    GLParam(param_id=201, value=1),
                    GLParam(param_id=202, value=3),
                    GLParam(param_id=203, value=42),
                ],
            ),
            Offer(
                hid=1, title='bad iphone 1', glparams=[GLParam(param_id=201, value=2), GLParam(param_id=203, value=42)]
            ),
            Offer(
                hid=1, title='bad iphone 2', glparams=[GLParam(param_id=201, value=1), GLParam(param_id=203, value=123)]
            ),
            Offer(
                hid=1, title='bad iphone 3', glparams=[GLParam(param_id=201, value=2), GLParam(param_id=203, value=123)]
            ),
        ]

        # test_vendor_filter
        cls.index.gltypes += [
            GLType(param_id=301, vendor=True, cluster_filter=True, hid=DRUGS_HID, gltype=GLType.ENUM, values=[1, 2, 3]),
            GLType(
                param_id=302, vendor=True, cluster_filter=True, hid=NON_DRUGS_HID, gltype=GLType.ENUM, values=[1, 2, 3]
            ),
        ]
        cls.index.offers += [
            Offer(title="without_vendor_filter_response", hid=DRUGS_HID, glparams=[GLParam(param_id=301, value=1)]),
            Offer(title="with_vendor_filter_response", hid=NON_DRUGS_HID, glparams=[GLParam(param_id=302, value=1)]),
        ]

        # test_hidden_values
        cls.index.gltypes += [
            GLType(param_id=401, hid=2, gltype=GLType.NUMERIC, hidden=False),
            GLType(param_id=402, hid=2, gltype=GLType.NUMERIC, hidden=True),
        ]

        cls.index.models += [
            Model(
                hyperid=1001,
                hid=2,
                glparams=[
                    GLParam(param_id=401, value=1),
                    GLParam(param_id=402, value=1),
                ],
            ),
        ]

        cls.index.regiontree += [
            Region(rid=213, name='region for place=modelinfo'),
        ]

    # MARKETOUT-9826
    def test_vendor_filter_for_drugs(self):
        # when hid != 13077405 everything is visible everywhere
        response = self.report.request_json('place=prime&hid={}'.format(NON_DRUGS_HID))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "offer",
                            "vendor": {"entity": "vendor", "id": 1, "filter": "302:1"},
                            "filters": [
                                {
                                    "id": "302",
                                }
                            ],
                        }
                    ]
                },
                "filters": [
                    {
                        "id": "302",
                    }
                ],
            },
        )

        # when hid == 13077405 'vendor' filter is absent in filters block and only on prime
        response = self.report.request_json('place=prime&hid={}'.format(DRUGS_HID))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "vendor": {"entity": "vendor", "id": 1, "filter": "301:1"},
                        "filters": [
                            {
                                "id": "301",
                            }
                        ],
                    }
                ]
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                "search": NotEmpty(),
                "filters": [
                    {
                        "id": "301",
                    }
                ],
            },
        )

    # See https://st.yandex-team.ru/MARKETOUT-7879.
    def test_hidden_values(self):
        response = self.report.request_json('place=prime&text=iphone&hid=1')
        # Hidden filters should not be shown...
        self.assertFragmentIn(response, {"filters": [{"id": "202"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "201"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "203"}]})

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "good iphone"}},
                    {"titles": {"raw": "bad iphone 1"}},
                    {"titles": {"raw": "bad iphone 2"}},
                    {"titles": {"raw": "bad iphone 3"}},
                ]
            },
        )

        response = self.report.request_json('place=prime&text=iphone&glfilter=201:1&glfilter=203:42~43&hid=1')
        # ...unless they are explicitly checked...
        self.assertFragmentIn(response, {"filters": [{"id": "201"}, {"id": "202"}, {"id": "203"}]})

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "good iphone"}},
                ]
            },
        )

        self.assertFragmentNotIn(response, {"titles": {"raw": "bad iphone 1"}})
        self.assertFragmentNotIn(response, {"titles": {"raw": "bad iphone 2"}})
        self.assertFragmentNotIn(response, {"titles": {"raw": "bad iphone 3"}})

        response = self.report.request_json('place=prime&text=iphone&filterList=all&hid=1')
        # ...or the client wants the full filter list.
        self.assertFragmentIn(response, {"filters": [{"id": "201"}, {"id": "202"}, {"id": "203"}]})

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "good iphone"}},
                    {"titles": {"raw": "bad iphone 1"}},
                    {"titles": {"raw": "bad iphone 2"}},
                    {"titles": {"raw": "bad iphone 3"}},
                ]
            },
        )

        # See https://st.yandex-team.ru/MARKETOUT-9904
        response = self.report.request_xml('place=modelinfo&hyperid=1001&rids=213')
        # there's only not hidden filter without filterList=all
        self.assertFragmentIn(
            response,
            '''
            <gl_filters>
                <filter id="401"/>
            </gl_filters>
        ''',
        )
        self.assertFragmentNotIn(
            response,
            '''
            <gl_filters>
                <filter id="402"/>
            </gl_filters>
        ''',
        )

        response = self.report.request_xml('place=modelinfo&hyperid=1001&rids=213&filterList=all')
        # hidden filter appears with filterList=all
        self.assertFragmentIn(
            response,
            '''
            <gl_filters>
                <filter id="401"/>
                <filter id="402"/>
            </gl_filters>
        ''',
        )

    # MARKETOUT-9209 Свзязь линейки и вендора
    @classmethod
    def prepare_vendor_lines(cls):

        """Вендоры с именем"""
        cls.index.vendors += [
            Vendor(vendor_id=1, name="Vendor1"),
            Vendor(vendor_id=2, name="Vendor2"),
        ]

        """Заведем фильтры с линейками"""
        cls.index.gltypes += [
            GLType(param_id=500, vendor=True, hid=PHONES_HID, gltype=GLType.ENUM, values=[1, 2]),
            GLType(
                param_id=501,
                name=u"Линейка",
                vendor_param_id=500,
                hid=PHONES_HID,
                gltype=GLType.ENUM,
                values=[GLValue(10, vendor_id=1), GLValue(11, vendor_id=1), GLValue(12, vendor_id=2)],
            ),
        ]

        """Модель с параметром линейка и каким-то другим параметром"""
        cls.index.models += [
            Model(
                hyperid=2001,
                title="Model Phone_with_line",
                hid=PHONES_HID,
                glparams=[GLParam(param_id=501, value=10), GLParam(param_id=502, value=11)],
            ),
        ]

        """Оффер с параметром линейка и каким-то другим параметром"""
        cls.index.offers += [
            Offer(
                title="Offer Phone_with_line_1",
                hid=PHONES_HID,
                glparams=[GLParam(param_id=501, value=10), GLParam(param_id=502, value=11)],
            ),
            Offer(
                title="Offer Phone_with_line_2",
                hid=PHONES_HID,
                glparams=[
                    GLParam(param_id=501, value=11),
                    GLParam(param_id=501, value=12),
                    GLParam(param_id=502, value=11),
                ],
            ),
        ]

    def test_vendor_lines(self):
        response = self.report.request_json('place=prime&hid={}'.format(PHONES_HID))

        """
        Проверим, что у фильтров с линейками появился vendorId (и он правильный), а у нелинеек не появился
        """
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "product",
                            "titles": {"raw": "Model Phone_with_line"},
                            "filters": [
                                {
                                    "id": "501",
                                    "values": [
                                        {
                                            "id": "10",
                                            "vendor": {
                                                "entity": "vendor",
                                                "id": 1,
                                                "name": "Vendor1",
                                            },
                                        }
                                    ],
                                },
                                {"id": "502", "values": [{"id": "11", "vendor": NoKey("vendor")}]},
                            ],
                        },
                        {
                            "entity": "offer",
                            "titles": {"raw": "Offer Phone_with_line_1"},
                            "filters": [
                                {
                                    "id": "501",
                                    "values": [
                                        {
                                            "id": "10",
                                            "vendor": {
                                                "entity": "vendor",
                                                "id": 1,
                                                "name": "Vendor1",
                                            },
                                        }
                                    ],
                                },
                                {"id": "502", "values": [{"id": "11", "vendor": NoKey("vendor")}]},
                            ],
                        },
                    ]
                },
                "filters": [
                    {
                        "id": "501",
                        "values": [
                            {
                                "id": "10",
                                "vendor": {
                                    "entity": "vendor",
                                    "id": 1,
                                    "name": "Vendor1",
                                },
                            },
                            {
                                "id": "11",
                                "vendor": {
                                    "entity": "vendor",
                                    "id": 1,
                                    "name": "Vendor1",
                                },
                            },
                            {
                                "id": "12",
                                "vendor": {
                                    "entity": "vendor",
                                    "id": 2,
                                    "name": "Vendor2",
                                },
                            },
                        ],
                    },
                    {"id": "502", "values": [{"id": "11", "vendor": NoKey("vendor")}]},
                ],
            },
        )

    @classmethod
    def prepare_initial_found_test(cls):
        """
        Создать 2 набора gl параметров типов (enum, bool, numeric)
        Создать документы с параметрами из первого набора
        По второму набору документов не будет
        """

        cls.index.gltypes += [
            GLType(param_id=501, hid=3, gltype=GLType.ENUM, cluster_filter=True, name='ENUM cluster', values=[1, 2]),
            GLType(
                param_id=502,
                hid=3,
                gltype=GLType.ENUM,
                cluster_filter=True,
                name='ENUM (no documents) cluster',
                values=[1, 2],
            ),
            GLType(param_id=503, hid=3, gltype=GLType.BOOL, cluster_filter=True, name='BOOL cluster'),
            GLType(param_id=504, hid=3, gltype=GLType.BOOL, cluster_filter=True, name='BOOL (no documents) cluster'),
            GLType(param_id=505, hid=3, gltype=GLType.NUMERIC, cluster_filter=True, name='NUMERIC cluster'),
            GLType(
                param_id=506, hid=3, gltype=GLType.NUMERIC, cluster_filter=True, name='NUMERIC (no documents) cluster'
            ),
            GLType(param_id=601, hid=4, gltype=GLType.ENUM, cluster_filter=True, name='ENUM', values=[1, 2]),
            GLType(
                param_id=602, hid=4, gltype=GLType.ENUM, cluster_filter=True, name='ENUM (no documents)', values=[1, 2]
            ),
            GLType(param_id=603, hid=4, gltype=GLType.BOOL, cluster_filter=True, name='BOOL'),
            GLType(param_id=604, hid=4, gltype=GLType.BOOL, cluster_filter=True, name='BOOL (no documents)'),
            GLType(param_id=605, hid=4, gltype=GLType.NUMERIC, cluster_filter=True, name='NUMERIC'),
            GLType(param_id=606, hid=4, gltype=GLType.NUMERIC, cluster_filter=True, name='NUMERIC (no documents)'),
        ]

        cls.index.shops += [Shop(fesh=17173, priority_region=213)]

        cls.index.models += [
            Model(
                title="phone 1",
                hid=4,
                hyperid=4001,
                glparams=[GLParam(param_id=601, value=2), GLParam(param_id=603, value=1)],
            ),
            Model(
                title="phone 2",
                hid=4,
                hyperid=4002,
                glparams=[GLParam(param_id=601, value=1), GLParam(param_id=603, value=0)],
            ),
        ]

        cls.index.offers += [
            Offer(
                title="phone 1 offer 1",
                hid=4,
                hyperid=4001,
                glparams=[GLParam(param_id=603, value=0), GLParam(param_id=605, value=111)],
            ),
            Offer(
                title="phone 2 offer 1",
                hid=4,
                hyperid=4002,
                fesh=17173,
                glparams=[GLParam(param_id=603, value=1), GLParam(param_id=605, value=128)],
            ),
        ]

        cls.index.vclusters += [
            VCluster(
                title='phone case 1',
                hid=3,
                vclusterid=1000000001,
                glparams=[GLParam(param_id=501, value=1), GLParam(param_id=503, value=1)],
            ),
            VCluster(
                title='phone case 2',
                hid=3,
                vclusterid=1000000002,
                glparams=[GLParam(param_id=501, value=2), GLParam(param_id=503, value=0)],
            ),
        ]

        cls.index.offers += [
            Offer(
                title="phone case 1 offer 1",
                vclusterid=1000000001,
                hid=3,
                glparams=[GLParam(param_id=503, value=0), GLParam(param_id=505, value=0.000001233)],
            ),
            Offer(
                title="phone case 2 offer 2",
                vclusterid=1000000002,
                hid=3,
                glparams=[GLParam(param_id=503, value=1), GLParam(param_id=505, value=456)],
            ),
        ]

    def test_initial_found_prime(self):
        """
        Проверки:
         - значение initialFound считается по всем документам выдачи (модели/кластера и офферы)
         - если в выдаче отсутвуют документы с каким-то gl-параметром -> initialFound это фильтра в значении 0
        """
        response = self.report.request_json('place=prime&hid=4&text=phone')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "601",
                        "type": "enum",
                        "subType": "",
                        "noffers": 4,
                        "values": [
                            {
                                "initialFound": 2,
                                "value": "VALUE-1",
                            },
                            {
                                "initialFound": 2,
                                "value": "VALUE-2",
                            },
                        ],
                    },
                    {
                        "id": "603",
                        "type": "boolean",
                        "noffers": 4,
                        "values": [
                            {
                                "initialFound": 2,
                                "value": "0",
                            },
                            {
                                "initialFound": 2,
                                "value": "1",
                            },
                        ],
                    },
                    {
                        "id": "605",
                        "type": "number",
                        "noffers": 2,
                        "values": [
                            {
                                "initialMax": "128",
                                "initialMin": "111",
                            }
                        ],
                    },
                ]
            },
        )
        self.assertFragmentNotIn(response, {"filters": [{"id": "606"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "602"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "604"}]})

        response = self.report.request_json('place=prime&hid=3&text=phone')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "501",
                        "type": "enum",
                        "noffers": 4,
                        "values": [
                            {
                                "initialFound": 2,
                                "value": "VALUE-1",
                            },
                            {
                                "initialFound": 2,
                                "value": "VALUE-2",
                            },
                        ],
                    },
                    {
                        "id": "503",
                        "type": "boolean",
                        "noffers": 4,
                        "values": [
                            {
                                "initialFound": 2,
                                "value": "0",
                            },
                            {
                                "initialFound": 2,
                                "value": "1",
                            },
                        ],
                    },
                    {
                        "id": "505",
                        "type": "number",
                        "noffers": 2,
                        "values": [
                            {
                                "initialMax": "456",
                                "initialMin": "0",
                            }
                        ],
                    },
                ]
            },
        )
        self.assertFragmentNotIn(response, {"filters": [{"id": "502"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "504"}]})

        response = self.report.request_json(
            'place=prime&hid=4&text=phone&use-default-offers=1&fesh=17173&market-force-business-id=1'
        )
        self.assertFragmentNotIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "id": "603",
                    },
                ],
            },
        )

        response = self.report.request_json('place=prime&hid=4&text=phone&use-default-offers=1&fesh=17173')
        self.assertFragmentIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "id": "603",
                    },
                ],
            },
        )

    def test_hideglfilters(self):
        """
        Проверки:
         - с cgi-параметром hideglfilters отключаются gl-фильтры
        """
        response = self.report.request_json('place=prime&hid=3&text=phone')
        self.assertFragmentIn(response, {"filters": [{"id": "501"}]})
        self.assertFragmentIn(response, {"filters": [{"id": "503"}]})
        self.assertFragmentIn(response, {"filters": [{"id": "505"}]})

        response = self.report.request_json('place=prime&hid=3&text=phone&hideglfilters=1')
        self.assertFragmentNotIn(response, {"filters": [{"id": "501"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "503"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "505"}]})

        for flag in [0, 1]:
            for client in ["IOS", "ANDROID"]:
                response = self.report.request_json(
                    'place=prime&hid=3&text=phone&on-page=1&page=2&client={}&rearr-factors=disable_filter_calculating_on_far_page={}'.format(
                        client, flag
                    )
                )
                if flag:
                    self.assertFragmentNotIn(response, {"filters": [{"id": "501"}]})
                    self.assertFragmentNotIn(response, {"filters": [{"id": "503"}]})
                else:
                    self.assertFragmentIn(response, {"filters": [{"id": "501"}]})
                    self.assertFragmentIn(response, {"filters": [{"id": "503"}]})

    @classmethod
    def prepare_missed_vendor_filter(cls):
        """
        Подготовка данных для проверки корректного отображения фильтра по вендору, а так же имени вендора, если самого вендора нет в vendors-info.xml
        Запрещаем запись аттрибута vendor_id в файл gl_mbo.pbuf.sn
        Запрещаем запись вендора 111 в файл vendors-info.xml
        """

        cls.index.models += [
            Model(hyperid=23456, hid=12345, vendor_id=111, glparams=[GLParam(param_id=303, value=111)])
        ]

        cls.index.gltypes += [
            GLType(
                param_id=303,
                vendor=True,
                cluster_filter=True,
                hid=12345,
                gltype=GLType.ENUM,
                values=[GLValue(111, save_vendor_id_attribute=False)],
            )
        ]

        cls.index.offers += [
            Offer(
                title="with_vendor_filter_response",
                hyperid=23456,
                hid=12345,
                glparams=[GLParam(param_id=303, value=111)],
            )
        ]

        cls.index.vendors += [Vendor(111, save_to_file=False)]

    def test_missed_vendor_filter(self):
        """
        Делаем запрос и ожидаем, что в результате будет запись вендора с корректным значением фильтра и именем вендора
        Проверка для оферов и для моделей
        """

        response = self.report.request_json('place=prime&hid=12345')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'offer',
                            'vendor': {'entity': 'vendor', 'id': 111, 'filter': '303:111', 'name': 'VENDOR-111'},
                        }
                    ]
                },
            },
        )

        response = self.report.request_json('place=modelinfo&hyperid=23456&bsformat=2&rids=213')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'product',
                            'vendor': {'entity': 'vendor', 'id': 111, 'filter': '303:111', 'name': 'VENDOR-111'},
                        }
                    ]
                },
            },
        )

    @classmethod
    def prepare_overflow_found(cls):
        """
        Создаем модель с произвольным параметром
        Регистрируем офферный параметр из той же категории
        """
        cls.index.gltypes += [
            GLType(hid=23, param_id=10, cluster_filter=True, values=[1, 0]),
        ]

        cls.index.models += [
            Model(hid=23, hyperid=20, glparams=[GLParam(param_id=30, value=1, gltype=GLType.BOOL)]),
        ]

    def test_overflow_found(self):
        """
        Ищем по офферному фильтру (ну и что, что ничего не найдем)
        Ожидаем в собранной статистике по фильтров по категории получить все по нулям, а не переполнение, как было раньше
        Подробнее см.: https://st.yandex-team.ru/MARKETOUT-11883
        """
        response = self.report.request_json('place=prime&hid=23&glfilter=10:0')
        self.assertFragmentIn(
            response,
            {
                "id": "30",
                "values": [
                    {"initialFound": 0, "found": 0, "value": "0", "id": "0"},
                    {"initialFound": 1, "found": 0, "value": "1", "id": "1"},
                ],
            },
        )

    @classmethod
    def prepare_test_inconsistent_glparams(cls):
        """
        Создаем модельный параметр, который будет присутствовать у сущностей, но не будет выгружаться
        в gl_mbo. Создаем модель с этим параметр и оффер, приматченный к ней
        """
        cls.index.gltypes += [
            GLType(param_id=421, hid=40, gltype=GLType.BOOL, dump_to_glmbo=False),
            GLType(param_id=422, hid=40, gltype=GLType.BOOL),
        ]

        cls.index.models += [
            Model(hyperid=4000, hid=40, glparams=[GLParam(param_id=421, value=1)]),
        ]

        cls.index.offers += [Offer(hyperid=4000)]

    def test_inconsistent_glparams(self):
        """
        Проверяем, что сущности с несогласованными параметрами находятся  на выдаче
        """
        response = self.report.request_json('place=prime&hid=40')
        self.assertFragmentIn(
            response, {'results': [{"entity": "product"}, {"entity": "offer"}]}, allow_different_len=False
        )

    @classmethod
    def prepare_test_xslname_output(cls):
        """
        Создаем фильтр с xslname, создаем модель с этим фильтром для того, чтобы он показался на выдаче
        """
        cls.index.gltypes += [
            GLType(hid=50, param_id=501, xslname="FILTER_XSLNAME"),
        ]

        cls.index.models += [
            Model(
                hid=50,
                glparams=[
                    GLParam(param_id=501, value=1),
                ],
            )
        ]

    def test_xslname_output(self):
        # Ожидаем:
        # 1. Xslname рендериться у фильтра c xslname
        # 2. Xslname отсутствует у фильтра без xslname (например, glprice)

        response = self.report.request_json('place=prime&hid=50')
        self.assertFragmentIn(
            response, {'filters': [{'id': '501', 'xslname': 'FILTER_XSLNAME'}, {'id': 'glprice', 'xslname': Absent()}]}
        )

    @classmethod
    def prepare_specified_for_offer(cls):
        """
        Создаем не групповую модель.
        """
        cls.index.gltypes += [
            # Параметры первого типа
            GLType(param_id=211, hid=232, gltype=GLType.ENUM, values=[11, 12]),
            GLType(param_id=212, hid=232, gltype=GLType.NUMERIC),
            # Параметры второго типа
            GLType(param_id=221, hid=232, gltype=GLType.ENUM, values=[1, 2], cluster_filter=True),
            GLType(param_id=222, hid=232, gltype=GLType.NUMERIC, cluster_filter=True),
        ]

        # Фильтры для модели
        filters_for_model = [
            GLParam(param_id=211, value=11),
            GLParam(param_id=212, value=100),
        ]
        # Фильтры для оферов
        filters_for_offer = [
            GLParam(param_id=221, value=1),
            GLParam(param_id=222, value=102),
        ]

        cls.index.models += [
            # Не групповая модель
            Model(
                title="single_model",
                hyperid=20024,
                hid=232,
                glparams=filters_for_model,
            ),
        ]

        cls.index.offers += [
            Offer(
                hyperid=20024,
                hid=232,
                title='non_group_offer',
                waremd5='RcSMzi4tf73qGvxRx8atJg',
                glparams=filters_for_model + filters_for_offer,
            ),
        ]

    def test_specified_for_offer(self):
        '''
        Проверяем маркировку фильтров спецефичных для офера.
        '''

        # Для офера не групповой модели отметку будут иметь все параметры второго рода
        response = self.report.request_json(
            'place=offerinfo&offerid=RcSMzi4tf73qGvxRx8atJg&rids=213&show-urls=external&regset=1'
        )
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {'id': '211', 'marks': NoKey('marks')},
                    {'id': '212', 'marks': NoKey('marks')},
                    {'id': '221', 'marks': {'specifiedForOffer': True}},
                    {'id': '222', 'marks': {'specifiedForOffer': True}},
                ]
            },
        )

    @classmethod
    def prepare_hasboolno_param(cls):
        """
        Создаем два booleangl-параметра, с hasBoolNo=True и без
        Создаем модель с этими параметрами
        """
        cls.index.gltypes += [
            GLType(param_id=241, hid=242, gltype=GLType.BOOL, hasboolno=True),
            GLType(param_id=242, hid=242, gltype=GLType.BOOL),
        ]

        cls.index.models += [
            Model(
                hyperid=2421,
                hid=242,
                glparams=[
                    GLParam(param_id=241, value=1),
                    GLParam(param_id=242, value=1),
                ],
            ),
        ]

    def check_hasboolno_param(self, response):
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {
                        'id': '241',
                        'hasBoolNo': True,
                    },
                    {
                        'id': '242',
                        'hasBoolNo': Absent(),
                    },
                ]
            },
        )

    def test_hasboolno_param(self):
        """
        Что тестируем: параметр hasBoolNo пробрасывается на
        выдачу в зависимости от данных MBO на плейсах prime и
        modelinfo
        Для параметра 241 hasBoolNo должен быть равен True,
        для 242 его не должно быть на выдаче
        """
        response = self.report.request_json('place=prime&hid=242')
        self.check_hasboolno_param(response)

        response = self.report.request_json('place=modelinfo&rids=0&hid=242&hyperid=2421')
        self.check_hasboolno_param(response)

    @classmethod
    def prepare_image_picker_subtype(cls):
        """
        Создаем офферный параметр с типом "картинка-пикер"
        Создаем модель и офферы с этим параметром, приматченные к ней
        """
        cls.index.gltypes += [
            GLType(param_id=241, hid=70, gltype=GLType.ENUM, subtype='image_picker', cluster_filter=True, hidden=True)
        ]

        cls.index.models += [
            Model(hyperid=4100, hid=70),
        ]

        cls.index.offers += [
            Offer(hyperid=4100, glparams=[GLParam(param_id=241, value=SILVER)]),
            Offer(hyperid=4100, glparams=[GLParam(param_id=241, value=ROSE_GOLD)]),
            Offer(hyperid=4100, glparams=[GLParam(param_id=241, value=JET_BLACK)]),
        ]

    def check_image_picker_subtype(self, response):
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {
                        "id": "241",
                        "type": "enum",
                        "subType": "image_picker",
                        "kind": 2,
                        "noffers": 3,
                        "values": [
                            {"initialFound": 1, "found": 1, "id": "10"},
                            {"initialFound": 1, "found": 1, "id": "11"},
                            {"initialFound": 1, "found": 1, "id": "12"},
                        ],
                    }
                ]
            },
        )

    def test_image_picker_subtype(self):
        """Что тестируем: офферный параметр с типом "картинка-пикер" выводится
        на плейсе productoffers как при запросе с параметром &filterList=all,
        так и при запросе без него. Параметр не выводится на плейсах modelinfo и prime
        """
        response = self.report.request_json('place=productoffers&hyperid=4100&filterList=all&hid=70')
        self.check_image_picker_subtype(response)

        # Параметр выводится и при запросе с указанием gl-фильтров
        response = self.report.request_json('place=productoffers&hyperid=4100&filterList=all&glfilter=241:10&hid=70')
        self.check_image_picker_subtype(response)

        response = self.report.request_json('place=productoffers&hyperid=4100&hid=70')
        self.check_image_picker_subtype(response)

        response = self.report.request_json('place=modelinfo&hyperid=4100&hid=70')
        self.assertFragmentNotIn(response, {'filters': [{"id": "241"}]})

        response = self.report.request_json('place=prime&hid=70')
        self.assertFragmentNotIn(response, {'filters': [{"id": "241"}]})

        self.error_log.expect(code=3043)

    @classmethod
    def prepare_checked_params(cls):
        """
        Создаем офферные параметры 2-го рода с тремя значениями
        Создаем две модели и два оффера к первой модели и один ко второй
        Каждый оффер с соответственно первым, вторым и третьим значением
        каждого из параметров
        """
        cls.index.gltypes += [
            GLType(param_id=242, hid=71, gltype=GLType.ENUM, cluster_filter=True, values=[1, 2, 3]),
            GLType(param_id=243, hid=71, gltype=GLType.ENUM, cluster_filter=True, values=[4, 5, 6]),
        ]

        cls.index.models += [
            Model(hyperid=4101, hid=71),
            Model(hyperid=4102, hid=71),
        ]

        cls.index.offers += [
            # Оффер, подходящий под фильтры
            Offer(
                hyperid=4101,
                fesh=1,
                glparams=[
                    GLParam(param_id=242, value=1),
                    GLParam(param_id=243, value=4),
                ],
            ),
            # Оффер, не подходящий под фильтры, но учитываемый в initial-found
            Offer(
                hyperid=4101,
                fesh=2,
                glparams=[
                    GLParam(param_id=242, value=2),
                    GLParam(param_id=243, value=5),
                ],
            ),
            # Оффер, не подходящий под запрос
            Offer(
                hyperid=4102,
                fesh=3,
                glparams=[
                    GLParam(param_id=242, value=3),
                    GLParam(param_id=243, value=6),
                ],
            ),
        ]

    def test_productoffers_checked_filters(self):
        """Что тестируем: выбранные значения gl-фильтра и фильтра по магазину
        не отображаются на productoffers, если по запросу нет
        предложений с этими значениями, независимо от прочих выбранных фильтров
        (initial-found=0)
        Делаем запрос за моделью 4101 с фильтрами 242:1,2,3 и 243:4,
        под которые попадает только один оффер.
        Проверяем, что в filters выводятся значения параметра этого и еще
        одного оффера этой модели
        Проверяем, что значение параметра другой модели не выводится в filters
        """
        response = self.report.request_json('place=productoffers&hyperid=4101&glfilter=242:1,2,3&glfilter=243:4&hid=71')
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {
                        "id": "242",
                        "values": [
                            {"id": "1"},
                            {"id": "2"},
                        ],
                    }
                ]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'filters': [
                    {
                        "id": "241",
                        "values": [
                            {"id": "3"},
                        ],
                    }
                ]
            },
        )

    @classmethod
    def prepare_image_picker_url(cls):
        """
        Создаем офферный параметр с типом "картинка-пикер" и значениями
        с урлом картинки и без него
        Создаем модель и офферы с этим параметром, приматченные к ней
        """
        cls.index.gltypes += [
            GLType(
                param_id=244,
                hid=72,
                gltype=GLType.ENUM,
                subtype='image_picker',
                cluster_filter=True,
                hidden=True,
                values=[
                    GLValue(
                        1,
                        image=ImagePickerData(
                            url='//avatars.mds.yandex.net/get-mpic/466729/img_id7441026923613423143/orig',
                            namespace="get-mpic",
                            group_id="466729",
                            image_name="img_id7441026923613423143",
                        ),
                    ),
                    GLValue(2),
                ],
            )
        ]

        cls.index.models += [
            Model(hyperid=4103, hid=72),
        ]

        cls.index.offers += [
            Offer(hyperid=4103, glparams=[GLParam(param_id=244, value=1)]),
            Offer(hyperid=4103, glparams=[GLParam(param_id=244, value=2)]),
        ]

    def check_image_picker_url(self, response):
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {
                        "id": "244",
                        "type": "enum",
                        "subType": "image_picker",
                        "kind": 2,
                        "noffers": 2,
                        "values": [
                            {
                                "initialFound": 1,
                                "found": 1,
                                "image": "//avatars.mds.yandex.net/get-mpic/466729/img_id7441026923613423143/orig",
                                "id": "1",
                                "picker": {
                                    "groupId": "466729",
                                    "entity": "photo",
                                    "imageName": "img_id7441026923613423143",
                                    "namespace": "get-mpic",
                                },
                            },
                            {"id": "2"},
                        ],
                    }
                ]
            },
        )

    def test_image_picker_url(self):
        """Что тестируем: урл картинки выводится на плейсе productoffers
        для параметра с типом "картинка-пикер"
        Запрашиваем productoffers с gl-фильтрами и без них
        Проверяем, что url есть на выдаче
        """
        response = self.report.request_json('place=productoffers&hyperid=4103&filterList=all&hid=72')
        self.check_image_picker_url(response)

        # Параметр выводится и при запросе с указанием gl-фильтров
        response = self.report.request_json('place=productoffers&hyperid=4103&filterList=all&glfilter=244:1&hid=72')
        self.check_image_picker_url(response)

    @classmethod
    def prepare_image_picker_url_from_model(cls):
        """
        Создаем офферный параметр с типом "картинка-пикер" и значениями
        с урлом картинки и без него
        Создаем модель и офферы с этим параметром, приматченные к ней
        """
        cls.index.gltypes += [
            GLType(
                param_id=344,
                hid=772,
                gltype=GLType.ENUM,
                subtype='image_picker',
                cluster_filter=True,
                hidden=True,
                values=[
                    GLValue(
                        1,
                        image=ImagePickerData(
                            '//avatars.mds.yandex.net/get-mpic/466729/img_id1/orig', 'get-mpic', '466729', 'img_id1'
                        ),
                    ),
                    GLValue(
                        2,
                        image=ImagePickerData(
                            '//avatars.mds.yandex.net/get-mpic/466729/img_id2/orig', 'get-mpic', '466729', 'img_id2'
                        ),
                    ),
                    GLValue(
                        3,
                        image=ImagePickerData(
                            '//avatars.mds.yandex.net/get-mpic/466729/img_id3/orig', 'get-mpic', '466729', 'img_id3'
                        ),
                    ),
                ],
            )
        ]

        cls.index.models += [
            Model(
                hyperid=6101,
                hid=772,
                parameter_value_links=[
                    ParameterValue(
                        344,
                        1,
                        ImagePickerData(
                            url='//avatars.mds.yandex.net/get-mpic/466729/img_model1/orig',
                            namespace="get-mpic",
                            group_id='466729',
                            image_name='img_model1',
                        ),
                    ),
                    ParameterValue(
                        344,
                        2,
                        ImagePickerData(
                            url='//avatars.mds.yandex.net/get-mpic/466729/img_model2/orig',
                            namespace="get-mpic",
                            group_id='466729',
                            image_name='img_model2',
                        ),
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=6101, glparams=[GLParam(param_id=344, value=1)]),
            Offer(hyperid=6101, glparams=[GLParam(param_id=344, value=2)]),
            Offer(hyperid=6101, glparams=[GLParam(param_id=344, value=3)]),
        ]

    def test_image_picker_url_from_model(self):
        """Что тестируем: урл картинки выводится на плейсе productoffers
        для параметра с типом "картинка-пикер" из атриббута модели, если он в ней есть.
        Если нет - то выводится значение с категории
        """
        response = self.report.request_json('place=productoffers&hyperid=6101&filterList=all&hid=772')
        self.assertFragmentIn(
            response,
            {
                "id": "344",
                "type": "enum",
                "subType": "image_picker",
                "kind": 2,
                "noffers": 3,
                "values": [
                    {
                        "image": "//avatars.mds.yandex.net/get-mpic/466729/img_model1/orig",
                        "id": "1",
                        "picker": {
                            "groupId": "466729",
                            "entity": "photo",
                            "imageName": "img_model1",
                            "namespace": "get-mpic",
                        },
                    },
                    {
                        "image": "//avatars.mds.yandex.net/get-mpic/466729/img_model2/orig",
                        "id": "2",
                        "picker": {
                            "groupId": "466729",
                            "entity": "photo",
                            "imageName": "img_model2",
                            "namespace": "get-mpic",
                        },
                    },
                    {
                        "image": "//avatars.mds.yandex.net/get-mpic/466729/img_id3/orig",
                        "id": "3",
                        "picker": {
                            "groupId": "466729",
                            "entity": "photo",
                            "imageName": "img_id3",
                            "namespace": "get-mpic",
                        },
                    },
                ],
            },
        )

    @classmethod
    def prepare_glfilter_positions(cls):
        """
        Создаем параметры с разными позициями для модельной и обычной выдачи.
        Проверяем, что в product_offers используется позиция model_filter_index
        Проверяем, что в prime используется позиция position
        Проверяем, что фильтр без model_filter_index в модельной выдаче не отображается,
        независимо от значения position
        """
        cls.index.gltypes += [
            GLType(param_id=591, hid=79, gltype=GLType.NUMERIC, position=1, model_filter_index=2, cluster_filter=True),
            GLType(param_id=592, hid=79, gltype=GLType.NUMERIC, position=2, model_filter_index=1, cluster_filter=True),
            GLType(
                param_id=593,
                hid=79,
                gltype=GLType.NUMERIC,
                position=3,
                has_model_filter_index=False,
                cluster_filter=True,
            ),
            GLType(param_id=594, hid=79, gltype=GLType.NUMERIC, position=-1, model_filter_index=3, cluster_filter=True),
        ]

        cls.index.models += [
            Model(hyperid=6100, hid=79),
        ]

        cls.index.offers += [
            Offer(
                hyperid=6100,
                glparams=[
                    GLParam(param_id=591, value=100),
                    GLParam(param_id=592, value=20),
                    GLParam(param_id=593, value=2),
                    GLParam(param_id=594, value=1),
                ],
            ),
            Offer(
                hyperid=6100,
                glparams=[
                    GLParam(param_id=591, value=130),
                    GLParam(param_id=592, value=25),
                    GLParam(param_id=593, value=5),
                    GLParam(param_id=594, value=3),
                ],
            ),
        ]

    def test_glfilter_positions(self):
        """
        Проверяем, что на productoffers параметры упорядочены по model_filter_index и
        параметр с position=-1 есть на выдаче
        """
        response = self.report.request_json('place=productoffers&hyperid=6100&hid=79')
        self.assertFragmentIn(
            response,
            {
                'search': {},
                'filters': [
                    {"id": "592"},
                    {"id": "591"},
                    {"id": "594"},
                ],
            },
            preserve_order=True,
        )

        self.assertFragmentNotIn(response, {'filters': {'id': 593}})

        # Проверяем, что на prime фильтры упорядочены по position
        response = self.report.request_json('place=prime&hyperid=6100&hid=79')
        self.assertFragmentIn(
            response, {'search': {}, 'filters': [{"id": "591"}, {"id": "592"}, {"id": "593"}]}, preserve_order=True
        )
        self.assertFragmentNotIn(response, {'filters': {'id': 594}})

    @classmethod
    def prepare_glfilter_positions_model_info(cls):
        """
        Проверяем, что в model_info используется model_filter_index, и офферы с position=-1 отображаются
        """
        cls.index.gltypes += [
            GLType(param_id=891, hid=89, gltype=GLType.NUMERIC, position=1, model_filter_index=2),
            GLType(param_id=892, hid=89, gltype=GLType.NUMERIC, position=2, model_filter_index=1),
            GLType(param_id=893, hid=89, gltype=GLType.NUMERIC, position=3, has_model_filter_index=False),
        ]

        cls.index.models += [
            Model(
                hyperid=9100,
                hid=89,
                glparams=[
                    GLParam(param_id=891, value=100),
                    GLParam(param_id=892, value=20),
                    GLParam(param_id=893, value=2),
                ],
            ),
        ]

    def test_glfilter_positions_model_info(self):
        response = self.report.request_json('place=modelinfo&hyperid=9100&hid=89&bsformat=2&rids=0')
        self.assertFragmentIn(response, {'filters': [{"id": "891"}, {"id": "892"}, {"id": "893"}]}, preserve_order=True)

    @classmethod
    def prepare_glfilter_hide_not_popular(cls):
        """
        Проверяем, что фильтры, помеченные hidden не вылезают под экспериментами market_hide_empty_filters, market_hide_initially_empty_filters,
        а показываются только, если задан filterList=all
        """
        cls.index.gltypes += [
            GLType(param_id=901, hid=1301, gltype=GLType.ENUM, position=1, hidden=False, values=[100, 200]),
            GLType(param_id=902, hid=1301, gltype=GLType.ENUM, position=2, hidden=True, values=[20, 30]),
        ]

        cls.index.models += [
            Model(hyperid=700, hid=1301),
        ]

        cls.index.offers += [
            Offer(hyperid=700, glparams=[GLParam(param_id=901, value=100), GLParam(param_id=902, value=20)])
        ]

    def test_glfilter_hide_non_popular_on_exp_hide_found_0(self):
        response = self.report.request_json("place=prime&hid=1301")
        self.assertFragmentIn(response, {'filters': [{"id": "901"}]})
        self.assertFragmentNotIn(response, {'filters': [{"id": "902"}]})
        response = self.report.request_json("place=prime&hid=1301&filterList=all")
        self.assertFragmentIn(response, {'filters': [{"id": "901"}, {"id": "902"}]})

    @classmethod
    def prepare_onstock_initial_found(cls):
        """
        проверяем, что initialFound зависит от фильтра onstock под экспериментом market_hide_initially_empty_filters
        """
        cls.index.gltypes += [
            GLType(param_id=1202, hid=400, gltype=GLType.ENUM, values=[GLValue(1), GLValue(2)]),
        ]

        cls.index.models += [
            Model(hyperid=302, hid=400, glparams=[GLParam(param_id=1202, value=1)]),  # model w/o offers
            Model(hyperid=303, hid=400, glparams=[GLParam(param_id=1202, value=2)]),  # model w/ onstock offers
        ]

        # статистика посчитанная индексатором
        cls.index.regional_models += [
            RegionalModel(hyperid=303, onstock=3, offers=10),
        ]

        cls.index.shops += [Shop(fesh=50, priority_region=213)]

        cls.index.offers += [Offer(hyperid=303, fesh=50)]

    def test_onstock_initial_found_market_hide_initially_empty_filters(self):
        response = self.report.request_json('place=prime&hid=400')
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {
                        'id': '1202',
                        'values': [
                            {'id': '1', 'initialFound': 1},
                            {'id': '2', 'initialFound': 2},
                        ],
                    }
                ]
            },
        )

        response = self.report.request_json('place=prime&hid=400&onstock=1')
        self.assertFragmentIn(response, {'filters': [{'id': '1202', 'values': [{'id': '2', 'initialFound': 2}]}]})
        self.assertFragmentNotIn(response, {'filters': [{'id': '1202', 'values': [{'id': '1'}]}]})

    @classmethod
    def prepare_initial_found_hide_exp(cls):
        """
        проверяем, что при initialFound=0 скрываем не гл фильтры, если офферов для них нет
        """
        cls.index.models += [
            Model(hyperid=311, hid=401),  # model
            Model(hyperid=312, hid=402),  # model w/o offers
        ]

        cls.index.shops += [Shop(fesh=51, priority_region=213)]

        cls.index.offers += [
            Offer(
                hyperid=311,
                fesh=51,
                manufacturer_warranty=False,
                delivery_options=[DeliveryOption(price=100, day_to=1)],
            )
        ]

    def check_initial_found_hide_bool(self, hid, filter_name):
        response = self.report.request_json('place=prime&hid=%d&rids=213&extra-filters=filter-promo' % hid)
        self.assertFragmentNotIn(response, {"filters": [{"id": filter_name}]})

    def test_manufacturer_warranty_initial_found_hide_exp(self):
        self.check_initial_found_hide_bool(401, "manufacturer_warranty")

    def test_free_delivery_initial_found_hide_exp(self):
        self.check_initial_found_hide_bool(401, "free-delivery")

    @classmethod
    def prepare_qr_hide(cls):
        """
        проверяем, что при initialFound=0 скрываем рейтинг и магазины (значения без найденных офферов и сам рейтинг)
        """

        cls.index.models += [
            Model(hyperid=331, hid=421),  # hid 3 ratings
            Model(hyperid=332, hid=422),  # hid 2 ratings
            Model(hyperid=333, hid=423),  # hid 1 rating
        ]

        cls.index.shops += [
            Shop(fesh=71, priority_region=213, new_shop_rating=NewShopRating(new_rating_total=2.0)),
            Shop(fesh=72, priority_region=213, new_shop_rating=NewShopRating(new_rating_total=3.0)),
            Shop(fesh=73, priority_region=213, new_shop_rating=NewShopRating(new_rating_total=4.0)),
            Shop(fesh=74, priority_region=213, new_shop_rating=NewShopRating(new_rating_total=1.0)),
        ]

        cls.index.offers += [
            Offer(hyperid=331, fesh=71, manufacturer_warranty=True),
            Offer(hyperid=331, fesh=72, manufacturer_warranty=True),
            Offer(hyperid=331, fesh=73, manufacturer_warranty=False),
            Offer(hyperid=332, fesh=71),
            Offer(hyperid=332, fesh=72),
            Offer(hyperid=333, fesh=74),
        ]

    def test_qr_initial_found_hide(self):
        # категория с 3 видами рейтингов
        response = self.report.request_json('place=prime&hid=421&rids=213')
        self.assertFragmentIn(response, {"id": "qrfrom", "values": [{"value": "2"}, {"value": "3"}, {"value": "4"}]})

        # категория с 2 видами рейтингов
        response = self.report.request_json('place=prime&hid=422&rids=213')
        self.assertFragmentIn(response, {"id": "qrfrom", "values": [{"value": "2"}, {"value": "3"}]})

        self.assertFragmentNotIn(response, {"id": "qrfrom", "values": [{"value": "4"}]})

        # категория с рейтингом 1
        response = self.report.request_json('place=prime&hid=423&rids=213')
        self.assertFragmentNotIn(response, {"filters": [{"id": "qrfrom"}]})

    @classmethod
    def prepare_offer_shipping_hide(cls):
        cls.index.models += [
            Model(hyperid=341, hid=431),  # hid 3 ratings
            Model(hyperid=342, hid=432),  # hid 2 ratings
            Model(hyperid=343, hid=433),  # hid 1 rating
        ]

        cls.index.shops += [
            Shop(fesh=81, priority_region=213, pickup_buckets=[5001]),
            Shop(fesh=82, priority_region=213, pickup_buckets=[5002]),
            Shop(fesh=83, priority_region=213),
        ]

        cls.index.outlets += [
            Outlet(fesh=81, region=213, point_id=101),
            Outlet(fesh=82, region=213, point_id=102, point_type=Outlet.FOR_PICKUP),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=81,
                carriers=[99],
                options=[PickupOption(outlet_id=101)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5002,
                fesh=82,
                carriers=[99],
                options=[PickupOption(outlet_id=102)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [
            Offer(
                hyperid=341,
                fesh=81,
                delivery_options=[DeliveryOption(price=100, day_to=1)],
                pickup=True,
                store=True,
                manufacturer_warranty=False,
            ),
            Offer(
                hyperid=342, fesh=82, delivery_options=[DeliveryOption(price=100, day_to=1)], pickup=True, store=False
            ),
            Offer(
                hyperid=343, fesh=83, delivery_options=[DeliveryOption(price=100, day_to=1)], pickup=False, store=False
            ),
            Offer(
                hyperid=341,
                fesh=81,
                delivery_options=[DeliveryOption(price=100, day_to=1)],
                pickup=False,
                store=False,
                manufacturer_warranty=True,
            ),
        ]

    def test_offer_shipping_initial_found_hide(self):
        # проверяем все 3 способа есть
        response = self.report.request_json('place=prime&hid=431&rids=213')
        self.assertFragmentIn(
            response,
            {"id": "offer-shipping", "values": [{"value": "delivery"}, {"value": "pickup"}, {"value": "store"}]},
        )

        # проверяем 2 способа есть
        response = self.report.request_json('place=prime&hid=432&rids=213')
        self.assertFragmentIn(
            response, {"id": "offer-shipping", "values": [{"value": "delivery"}, {"value": "pickup"}]}
        )
        self.assertFragmentNotIn(response, {"id": "offer-shipping", "values": [{"value": "store"}]})

        # проверяем 1 способа есть
        response = self.report.request_json('place=prime&hid=433&rids=213')
        self.assertFragmentIn(response, {"id": "offer-shipping", "values": [{"value": "delivery"}]})
        self.assertFragmentNotIn(response, {"id": "offer-shipping", "values": [{"value": "pickup"}]})
        self.assertFragmentNotIn(response, {"id": "offer-shipping", "values": [{"value": "store"}]})

        # проверяем ничего нет
        response = self.report.request_json('place=prime&hid=431&rids=213&text=ooooo')
        self.assertFragmentNotIn(response, {"id": "offer-shipping"})

    @classmethod
    def prepare_delivery_interval_hide(cls):
        cls.index.models += [
            Model(hyperid=351, hid=441),  # hid 3 ratings
            Model(hyperid=352, hid=442),  # hid 2 ratings
            Model(hyperid=353, hid=443),  # hid 1 rating
        ]

        cls.index.shops += [
            Shop(fesh=91, priority_region=213),
        ]
        cls.index.offers += [
            Offer(
                hyperid=351,
                fesh=91,
                delivery_options=[DeliveryOption(price=100, day_to=0)],
                manufacturer_warranty=False,
            ),
            Offer(
                hyperid=351, fesh=91, delivery_options=[DeliveryOption(price=0, day_to=1)], manufacturer_warranty=False
            ),
            Offer(
                hyperid=351,
                fesh=91,
                delivery_options=[DeliveryOption(price=100, day_to=2, day_from=2)],
                manufacturer_warranty=True,
            ),
            Offer(hyperid=352, fesh=91, delivery_options=[DeliveryOption(price=100, day_to=1)]),
            Offer(hyperid=352, fesh=91, delivery_options=[DeliveryOption(price=0, day_to=2)]),
            Offer(hyperid=353, fesh=91, delivery_options=[DeliveryOption(price=100, day_to=1)]),
        ]

    def test_delivery_interval_initial_found_hide(self):
        # проверяем все 3 вида
        response = self.report.request_json('place=prime&hid=441&rids=213')
        self.assertFragmentIn(
            response, {"id": "delivery-interval", "values": [{"value": "0"}, {"value": "1"}, {"value": "5"}]}
        )

        # проверяем 2 способа есть
        response = self.report.request_json('place=prime&hid=442&rids=213')
        self.assertFragmentIn(response, {"id": "delivery-interval", "values": [{"value": "1"}, {"value": "5"}]})
        self.assertFragmentNotIn(response, {"id": "delivery-interval", "values": [{"value": "0"}]})

        # проверяем 1 способа есть
        response = self.report.request_json('place=prime&hid=443&rids=213')
        self.assertFragmentIn(response, {"id": "delivery-interval", "values": [{"value": "1"}]})
        self.assertFragmentNotIn(response, {"id": "delivery-interval", "values": [{"value": "0"}]})
        self.assertFragmentNotIn(response, {"id": "delivery-interval", "values": [{"value": "5"}]})

        # проверяем ничего нет
        response = self.report.request_json('place=prime&hid=441&rids=213&text=ooooo')
        self.assertFragmentNotIn(response, {"id": "delivery-interval"})

    @classmethod
    def prepare_discount_only_hide_exp(cls):
        cls.index.models += [
            Model(hyperid=371, hid=461),  # with discount
            Model(hyperid=372, hid=462),  # without discount
        ]

        cls.index.shops += [
            Shop(fesh=111, priority_region=213),
        ]

        cls.index.offers += [
            Offer(hyperid=371, fesh=111, discount=15, manufacturer_warranty=False),
            Offer(hyperid=371, fesh=111, manufacturer_warranty=True),
            Offer(hyperid=372, fesh=111),
        ]

    def test_discount_only_hide_exp(self):
        self.check_initial_found_hide_bool(462, "filter-discount-only")

    @classmethod
    def prepare_promo_hide_exp(cls):
        cls.index.models += [
            Model(hyperid=381, hid=471),  # with discount
            Model(hyperid=382, hid=472),  # without discount
        ]

        cls.index.shops += [
            Shop(fesh=121, priority_region=213),
        ]

        cls.index.offers += [
            Offer(
                title='promo offer',
                fesh=121,
                hyperid=381,
                waremd5='Z9_iwgA17yd3FCI0LRhC_w',
                price=20,
                manufacturer_warranty=False,
                promo=Promo(
                    promo_type=PromoType.N_PLUS_ONE,
                    key='zMpCOKC5I4INzFCab3WEmw',
                    start_date=datetime(1984, 1, 1),
                    required_quantity=3,
                    free_quantity=34,
                ),
            ),
            Offer(hyperid=381, fesh=121, manufacturer_warranty=True),
            Offer(hyperid=382, fesh=121),
        ]

    @classmethod
    def prepare_hide_boolean_gl_filters(cls):
        """
        Проверяем, что скрываются boolean gl фильтры, для которых есть офферы только для значение false
        """
        cls.index.models += [
            Model(hyperid=391, hid=481),  # with boolean param = true
            Model(hyperid=392, hid=482),  # with boolean param = false
        ]

        cls.index.gltypes += [
            GLType(param_id=2001, hid=481, gltype=GLType.BOOL),
            GLType(param_id=2002, hid=482, gltype=GLType.BOOL),
        ]

        cls.index.shops += [
            Shop(fesh=131, priority_region=213),
        ]

        cls.index.offers += [
            Offer(hyperid=391, fesh=131, manufacturer_warranty=False, glparams=[GLParam(param_id=2001, value=1)]),
            Offer(hyperid=391, fesh=131, manufacturer_warranty=True, glparams=[GLParam(param_id=2001, value=0)]),
            Offer(hyperid=392, fesh=131, manufacturer_warranty=True, glparams=[GLParam(param_id=2001, value=0)]),
        ]

    def test_hide_boolean_gl_filters(self):
        self.check_initial_found_hide_bool(482, "2002")

    @classmethod
    def prepare_hidden_filter_with_model_index(cls):
        """
        Создаем параметры с model_filter_index, скрытые на поисковой выдаче
        Создаем офферы с каждым из параметров
        """
        cls.index.gltypes += [
            GLType(
                param_id=595,
                hid=80,
                gltype=GLType.NUMERIC,
                position=1,
                model_filter_index=1,
                hidden=True,
                cluster_filter=False,
            ),
            GLType(
                param_id=596,
                hid=80,
                gltype=GLType.NUMERIC,
                position=2,
                model_filter_index=2,
                hidden=True,
                cluster_filter=True,
            ),
        ]

        cls.index.models += [
            Model(hyperid=6200, hid=80),
        ]

        cls.index.offers += [
            Offer(hyperid=6200, glparams=[GLParam(param_id=595, value=100), GLParam(param_id=596, value=130)]),
            Offer(hyperid=6200, glparams=[GLParam(param_id=596, value=100)]),
        ]

    def test_hidden_filter_with_model_index(self):
        """
        Проверяем, что на productoffers в списке фильтров выводится скрытый параметр
            с model_filter_index и cluster_filter=True, а такой же параметр с
            cluster_filter=False не выводится
        Проверяем, что на prime по дефолту эти фильтры не выводятся
        Проверяем, что на prime с filterList=all выводятся оба параметра
        """
        response = self.report.request_json('place=productoffers&hyperid=6200&hid=80')
        self.assertFragmentIn(
            response,
            {
                'search': {},
                'filters': [
                    {"id": "596"},
                ],
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'search': {},
                'filters': [
                    {"id": "595"},
                ],
            },
        )

        response = self.report.request_json('place=prime&hid=80')
        self.assertFragmentNotIn(
            response,
            {
                'search': {},
                'filters': [
                    {"id": "595"},
                ],
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'search': {},
                'filters': [
                    {"id": "596"},
                ],
            },
        )

        response = self.report.request_json('place=prime&hid=80&filterList=all')
        self.assertFragmentIn(
            response,
            {
                'search': {},
                'filters': [
                    {"id": "595"},
                    {"id": "596"},
                ],
            },
        )

    @classmethod
    def prepare_offerinfo_glfilter_positions(cls):
        """
        Создаем оффер с параметром, не отображаемым на КМ
        """
        cls.index.offers += [
            Offer(
                hid=79,
                waremd5='HtFQXGWTsf-zCQLA1DpAmn',
                glparams=[
                    GLParam(param_id=591, value=100),
                    GLParam(param_id=592, value=20),
                    GLParam(param_id=593, value=2),
                    GLParam(param_id=594, value=1),
                ],
            ),
        ]

    def test_offerinfo_glfilter_positions(self):
        """
        Проверяем, что на offerinfo c &show-model-card-params=1 выводятся параметры
        с model_filter_index != -1 и они упорядочены по model_filter_index,
        а параметр с model_filter_index=-1 не выводится
        """
        response = self.report.request_json(
            'place=offerinfo&show-model-card-params=1&offerid=HtFQXGWTsf-zCQLA1DpAmn&rids=0&regset=2&show-urls=external'
        )
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {"id": "592"},
                    {"id": "591"},
                    {"id": "594"},
                ]
            },
            preserve_order=True,
        )

        self.assertFragmentNotIn(response, {'filters': {'id': 593}})

        # Запрос без флага
        response = self.report.request_json(
            'place=offerinfo&offerid=HtFQXGWTsf-zCQLA1DpAmn&rids=0&regset=2&show-urls=external'
        )
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {"id": "591"},
                    {"id": "592"},
                    {"id": "593"},
                ]
            },
            preserve_order=True,
        )

        self.assertFragmentNotIn(response, {'filters': {'id': 594}})

    @classmethod
    def prepare_grouped_category_model_param_snippet(cls):
        """
        Создаем групповую категорию, в ней модель и матчим к модели два оффера
        У модели задан параметр первого рода, у офферов задан параметр второго рода типа "цвет"
        """
        cls.index.hypertree += [HyperCategory(hid=80, has_groups=True)]

        cls.index.gltypes += [
            GLType(param_id=601, hid=80, subtype='color', cluster_filter=True, gltype=GLType.ENUM, values=[1, 2, 3]),
            GLType(param_id=603, hid=80, gltype=GLType.ENUM, values=[100, 200, 300]),
        ]

        cls.index.models += [Model(hid=80, hyperid=8000, glparams=[GLParam(param_id=603, value=200)])]

        cls.index.offers += [
            Offer(hyperid=8000, glparams=[GLParam(param_id=601, value=1)]),
            Offer(hyperid=8000, glparams=[GLParam(param_id=601, value=3)]),
        ]

    def test_grouped_category_model_param_snippet(self):
        # Ожидаем, что в сниппете параметров модели будут параметры второго рода типа "цвет" из ее офферов и
        # параметр первого рода

        response = self.report.request_json('place=modelinfo&hyperid=8000&bsformat=2&rids=0')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'filters': [
                            {'id': '601', 'values': [{'id': '1'}, {'id': '3'}]},
                            {'id': '603', 'values': [{'id': '200'}]},
                        ]
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_popular_glparams_onsearch_mix_diff(cls):
        cls.index.hypertree += [HyperCategory(hid=898001), HyperCategory(hid=898002)]

        cls.index.gltypes += [
            GLType(param_id=600601, hid=898001, gltype=GLType.ENUM, cluster_filter=True, values=[100, 200, 300]),
            GLType(
                param_id=600602,
                hid=898001,
                gltype=GLType.ENUM,
                cluster_filter=True,
                values=[GLValue(100), GLValue(200), GLValue(300)],
            ),
            GLType(
                param_id=600603,
                hid=898001,
                gltype=GLType.ENUM,
                cluster_filter=True,
                values=[GLValue(100), GLValue(200), GLValue(300)],
            ),
        ]

        cls.index.gltypes += [
            GLType(
                param_id=600611,
                hid=898002,
                gltype=GLType.ENUM,
                unit_name="2-Length",
                cluster_filter=True,
                values=[100, 200, 300],
            ),
            GLType(
                param_id=600612,
                hid=898002,
                gltype=GLType.ENUM,
                unit_name="2-Length2",
                cluster_filter=True,
                values=[100, 200, 300],
            ),
            GLType(
                param_id=600613,
                hid=898002,
                gltype=GLType.ENUM,
                unit_name="2-Length3",
                cluster_filter=True,
                values=[100, 200, 300],
            ),
        ]

        cls.index.offers += [
            Offer(
                hid=898001,
                title="BloodyMary-1.1",
                glparams=[
                    GLParam(param_id=600601, value=100),
                    GLParam(param_id=600602, value=100),
                    GLParam(param_id=600603, value=100),
                ],
            ),
            Offer(
                hid=898001,
                title="BloodyMary-1.2",
                glparams=[
                    GLParam(param_id=600601, value=200),
                    GLParam(param_id=600602, value=200),
                    GLParam(param_id=600603, value=200),
                ],
            ),
            Offer(
                hid=898002,
                title="BloodyMary-2.1",
                glparams=[
                    GLParam(param_id=600611, value=100),
                    GLParam(param_id=600612, value=100),
                    GLParam(param_id=600613, value=100),
                ],
            ),
            Offer(
                hid=898002,
                title="BloodyMary-2.1",
                glparams=[
                    GLParam(param_id=600611, value=300),
                    GLParam(param_id=600612, value=300),
                    GLParam(param_id=600613, value=300),
                ],
            ),
        ]

    def test_popular_glparams_onsearch_mix_diff(self):
        """
        Проверяем, что выводятся по 2 фильтра из 2 разных найденныйх категорий
        Проверяем, что установился редирект на категорию из которой фильтр (аттрибут hid)
        """
        for pop_filters_params in ('rearr-factors=market_popular_gl_filters_on_search=1', 'popular-filters=1'):
            response = self.report.request_json(
                "place=prime&text=BloodyMary&{}&enable-hard-filters=0".format(pop_filters_params)
            )
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "600601",
                            "isRedirect": True,
                            "values": [
                                {"initialFound": 1, "found": 1, "id": "100", "hid": 898001},
                                {"initialFound": 1, "found": 1, "id": "200", "hid": 898001},
                            ],
                        },
                        {
                            "id": "600602",
                            "isRedirect": True,
                            "values": [
                                {"initialFound": 1, "found": 1, "id": "100", "hid": 898001},
                                {"initialFound": 1, "found": 1, "id": "200", "hid": 898001},
                            ],
                        },
                        {
                            "id": "600611",
                            "isRedirect": True,
                            "values": [
                                {"initialFound": 1, "found": 1, "id": "100", "hid": 898002},
                                {"initialFound": 1, "found": 1, "id": "300", "hid": 898002},
                            ],
                        },
                        {
                            "id": "600612",
                            "isRedirect": True,
                            "values": [
                                {"initialFound": 1, "found": 1, "id": "100", "hid": 898002},
                                {"initialFound": 1, "found": 1, "id": "300", "hid": 898002},
                            ],
                        },
                    ]
                },
            )

    def testRedirectFlag_inside_category(self):
        """
        key "isRedirect" does not exist on category search
        """
        for pop_filters_params in ('rearr-factors=market_popular_gl_filters_on_search=1', 'popular-filters=1'):
            response = self.report.request_json("place=prime&text=BloodyMary&{}&hid=898001".format(pop_filters_params))
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {"id": "600601", "isRedirect": NoKey("isRedirect")},
                        {"id": "600602", "isRedirect": NoKey("isRedirect")},
                    ]
                },
            )

    @classmethod
    def prepare_popular_glparams_onsearch_mix_union(cls):
        cls.index.hypertree += [
            HyperCategory(hid=898101, uniq_name="BestOfLuck"),
            HyperCategory(hid=898102, uniq_name="HardDay"),
        ]

        cls.index.gltypes += [
            GLType(param_id=600701, hid=898101, gltype=GLType.ENUM, cluster_filter=True, values=[100, 200]),
            GLType(
                param_id=600702,
                hid=898101,
                gltype=GLType.ENUM,
                cluster_filter=True,
                values=[GLValue(100), GLValue(200), GLValue(300)],
            ),
            GLType(
                param_id=600703,
                hid=898101,
                gltype=GLType.ENUM,
                cluster_filter=True,
                values=[GLValue(100), GLValue(200), GLValue(300)],
            ),
        ]

        cls.index.gltypes += [
            GLType(
                param_id=600701,
                hid=898102,
                gltype=GLType.ENUM,
                unit_name="2-Length",
                cluster_filter=True,
                values=[200, 300],
                position=1,
            ),
            GLType(
                param_id=600712,
                hid=898102,
                gltype=GLType.ENUM,
                unit_name="2-Length2",
                cluster_filter=True,
                values=[100, 200, 300],
                position=2,
            ),
            GLType(
                param_id=600713,
                hid=898102,
                gltype=GLType.ENUM,
                unit_name="2-Length3",
                cluster_filter=True,
                values=[100, 200, 300],
                position=3,
            ),
        ]

        cls.index.offers += [
            Offer(
                hid=898101,
                title="BlueLagoon-1.1",
                glparams=[
                    GLParam(param_id=600701, value=100),
                    GLParam(param_id=600702, value=100),
                    GLParam(param_id=600703, value=100),
                ],
            ),
            Offer(
                hid=898101,
                title="BlueLagoon-1.2",
                glparams=[
                    GLParam(param_id=600701, value=200),
                    GLParam(param_id=600702, value=200),
                    GLParam(param_id=600703, value=200),
                ],
            ),
            Offer(
                hid=898101,
                title="BlueLagoon-1.3",
                glparams=[
                    GLParam(param_id=600701, value=200),
                    GLParam(param_id=600702, value=200),
                    GLParam(param_id=600703, value=200),
                ],
            ),
            Offer(
                hid=898102,
                title="BlueLagoon-2.1",
                glparams=[
                    GLParam(param_id=600701, value=200),
                    GLParam(param_id=600712, value=100),
                    GLParam(param_id=600713, value=100),
                ],
            ),
            Offer(
                hid=898102,
                title="BlueLagoon-2.1",
                glparams=[
                    GLParam(param_id=600701, value=300),
                    GLParam(param_id=600712, value=300),
                    GLParam(param_id=600713, value=300),
                ],
            ),
        ]

    def test_popular_glparams_onsearch_mix_union(self):
        """
        Проверяем, что выводятся по 1 глобальный фильтр и по 1 из разных найденныйх категорий
        Проверяем, что для редиректа проставилась более популярная категория для фильтра 600701 у значения 200
        """
        for pop_filters_params in ('rearr-factors=market_popular_gl_filters_on_search=1', 'popular-filters=1'):
            response = self.report.request_json(
                "place=prime&text=BlueLagoon&{}&enable-hard-filters=0".format(pop_filters_params)
            )
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "600701",
                            "categoryName": NoKey("categoryName"),
                            "isRedirect": True,
                            "values": [
                                {"initialFound": 1, "found": 1, "id": "100", "hid": 898101},
                                {"initialFound": 3, "found": 3, "id": "200", "hid": 898101},
                                {"initialFound": 1, "found": 1, "id": "300", "hid": 898102},
                            ],
                        },
                        {
                            "id": "600702",
                            "categoryName": "BestOfLuck",
                            "isRedirect": True,
                            "values": [
                                {"initialFound": 1, "found": 1, "id": "100", "hid": 898101},
                                {"initialFound": 2, "found": 2, "id": "200", "hid": 898101},
                            ],
                        },
                        {
                            "id": "600712",
                            "categoryName": "HardDay",
                            "isRedirect": True,
                            "values": [
                                {"initialFound": 1, "found": 1, "id": "100", "hid": 898102},
                                {"initialFound": 1, "found": 1, "id": "300", "hid": 898102},
                            ],
                        },
                    ]
                },
            )

    def test_popular_glparams_onsearch_non_search(self):
        """
        Проверяем, что при обычной категорийной выдаче поля hid в ответе нет.
        Проверяем, что все три фильтра категории 898101 на месте
        """
        for pop_filters_params in ('rearr-factors=market_popular_gl_filters_on_search=1', 'popular-filters=1'):
            response = self.report.request_json("place=prime&text=BlueLagoon&hid=898101&{}".format(pop_filters_params))
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "600701",
                            "categoryName": NoKey("categoryName"),
                            "values": [
                                {"initialFound": 1, "found": 1, "id": "100", "hid": NoKey("hid")},
                                {"initialFound": 2, "found": 2, "id": "200", "hid": NoKey("hid")},
                            ],
                        },
                        {
                            "id": "600702",
                            "categoryName": NoKey("categoryName"),
                            "values": [
                                {"initialFound": 1, "found": 1, "id": "100", "hid": NoKey("hid")},
                                {"initialFound": 2, "found": 2, "id": "200", "hid": NoKey("hid")},
                            ],
                        },
                        {
                            "id": "600703",
                            "categoryName": NoKey("categoryName"),
                            "values": [
                                {"initialFound": 1, "found": 1, "id": "100", "hid": NoKey("hid")},
                                {"initialFound": 2, "found": 2, "id": "200", "hid": NoKey("hid")},
                            ],
                        },
                    ]
                },
            )

    def check_filter_in_response(self, response, param_id, hid, nid, value_ids):
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": str(param_id),
                        "values": [{"id": str(value_id), "hid": hid, "nid": nid} for value_id in value_ids],
                    }
                ]
            },
        )

    def check_filter_not_in_response(self, response, param_id):
        self.assertFragmentNotIn(response, {"filters": [{"id": str(param_id)}]})

    @classmethod
    def prepare_popular_glparams_onsearch_ignore_numeric(cls):
        cls.index.hypertree += [HyperCategory(hid=898201), HyperCategory(hid=898202)]

        cls.index.navtree += [
            NavCategory(nid=900090, hid=898201, name="Test cat 1"),
            NavCategory(nid=900091, hid=898202, name="Test cat 2"),
        ]

        cls.index.gltypes += [
            GLType(param_id=600801, hid=898201, gltype=GLType.NUMERIC, cluster_filter=True),
            GLType(
                param_id=600802,
                hid=898201,
                gltype=GLType.ENUM,
                cluster_filter=True,
                values=[GLValue(100), GLValue(200), GLValue(300)],
            ),
            GLType(
                param_id=600803,
                hid=898201,
                gltype=GLType.ENUM,
                cluster_filter=True,
                values=[GLValue(100), GLValue(200), GLValue(300)],
            ),
        ]

        cls.index.gltypes += [
            GLType(param_id=600811, hid=898202, gltype=GLType.BOOL, unit_name="2-Length", cluster_filter=True),
            GLType(
                param_id=600812,
                hid=898202,
                gltype=GLType.ENUM,
                unit_name="2-Length2",
                cluster_filter=True,
                values=[100, 200, 300],
            ),
            GLType(
                param_id=600813,
                hid=898202,
                gltype=GLType.ENUM,
                unit_name="2-Length3",
                cluster_filter=True,
                values=[100, 200, 300],
            ),
        ]

        cls.index.offers += [
            Offer(
                hid=898201,
                title="GrMojito-1.1",
                glparams=[
                    GLParam(param_id=600801, value=100),
                    GLParam(param_id=600802, value=100),
                    GLParam(param_id=600803, value=100),
                ],
            ),
            Offer(
                hid=898201,
                title="GrMojito-1.2",
                glparams=[
                    GLParam(param_id=600801, value=200),
                    GLParam(param_id=600802, value=200),
                    GLParam(param_id=600803, value=200),
                ],
            ),
            Offer(
                hid=898202,
                title="GrMojito-2.1",
                glparams=[
                    GLParam(param_id=600811, value=1),
                    GLParam(param_id=600812, value=100),
                    GLParam(param_id=600813, value=100),
                ],
            ),
            Offer(
                hid=898202,
                title="GrMojito-2.1",
                glparams=[
                    GLParam(param_id=600811, value=0),
                    GLParam(param_id=600812, value=300),
                    GLParam(param_id=600813, value=200),
                ],
            ),
        ]

    def test_popular_glparams_onsearch_numeric_boolean(self):
        """
        Проверяем, что нумерик и бул фильтры не выводятся,
        а enum выводятся
        """
        for pop_filters_params in ('rearr-factors=market_popular_gl_filters_on_search=1', 'popular-filters=1'):
            response = self.report.request_json(
                "place=prime&text=GrMojito&{}&enable-hard-filters=0".format(pop_filters_params)
            )
            self.check_filter_in_response(response, 600802, 898201, 900090, [100, 200])
            self.check_filter_in_response(response, 600803, 898201, 900090, [100, 200])
            self.check_filter_in_response(response, 600812, 898202, 900091, [100, 300])
            self.check_filter_in_response(response, 600813, 898202, 900091, [100, 200])
            self.check_filter_not_in_response(response, 600801)
            self.check_filter_not_in_response(response, 600811)

    @classmethod
    def prepare_popular_glparams_vendor_color_size(cls):
        cls.index.hypertree += [HyperCategory(hid=898301), HyperCategory(hid=898302)]

        cls.index.gltypes += [
            GLType(
                param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID,
                hid=898301,
                gltype=GLType.ENUM,
                cluster_filter=True,
                values=[100, 200, 300, 400, 500, 600, 700, 800],
                vendor=True,
            ),
            GLType(
                param_id=13887626,
                hid=898301,
                gltype=GLType.ENUM,
                cluster_filter=True,
                values=[0, 1, 2, 3, 4, 5, 6],
                subtype="color",
            ),
        ]

        cls.index.gltypes += [
            GLType(
                param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID,
                hid=898302,
                gltype=GLType.ENUM,
                cluster_filter=True,
                values=[600, 700, 800, 900, 1000, 1100, 1200, 1300, 1400, 1500, 1600],
                vendor=True,
            ),
            GLType(
                param_id=600103,
                hid=898302,
                position=4,
                cluster_filter=1,
                gltype=GLType.ENUM,
                subtype='size',
                size_charts=[
                    GLSizeChart(option_id=1, default=True),
                    GLSizeChart(option_id=2),
                ],
                name=u'Размер',
                unit_param_id=600104,
                values=[
                    GLValue(value_id=1, text='42', unit_value_id=1, filter_value=False),
                    GLValue(value_id=2, text='44', unit_value_id=1, filter_value=False),
                    GLValue(value_id=3, text='46', unit_value_id=1, filter_value=False),
                    GLValue(value_id=4, text='36', unit_value_id=2, filter_value=False),
                    GLValue(value_id=5, text='38', unit_value_id=2, filter_value=False),
                    GLValue(value_id=6, text='40', unit_value_id=2, filter_value=False),
                ],
            ),
            GLType(
                param_id=600104,
                hid=898302,
                position=None,
                gltype=GLType.ENUM,
                values=[GLValue(value_id=1, text='RU'), GLValue(value_id=2, text='EU', default=True)],
            ),
        ]

        cls.index.offers += [
            Offer(
                hid=898301,
                title="PinaColada-1",
                glparams=[
                    GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=param_value),
                    GLParam(param_id=13887626, value=param_value % 7),
                ],
            )
            for param_value in [100, 200, 300, 400, 500, 600, 700, 800]
        ]

        cls.index.offers += [
            Offer(
                hid=898302,
                title="PinaColada-2",
                glparams=[
                    GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=param_value),
                    GLParam(param_id=600103, value=param_value % 6 + 1),
                ],
            )
            for param_value in [600, 700, 800, 900, 1000, 1100, 1200, 1300, 1400, 1500, 1600]
        ]

    def test_popular_glparams_vendor_no_showmore(self):
        """
        1. Проверяем, что в фильтре производитель 12 значений
         (это также значит, что значения смержились, но на смерживание у нас есть другой тест).
        2. Проверяем, что valuesCount == 12, в этом случае фронт не должен показать кнопку "Показать еще".
        """
        for pop_filters_params in ('rearr-factors=market_popular_gl_filters_on_search=1', 'popular-filters=1'):
            response = self.report.request_json("place=prime&text=PinaColada&{}".format(pop_filters_params))
            self.assertFragmentIn(
                response,
                {
                    "id": "7893318",
                    "valuesCount": 12,
                    "values": [
                        {"id": "100"},
                        {"id": "1000"},
                        {"id": "1100"},
                        {"id": "1200"},
                        {"id": "1300"},
                        {"id": "1400"},
                        {"id": "1500"},
                        {"id": "1600"},
                        {"id": "200"},
                        {"id": "600"},
                        {"id": "700"},
                        {"id": "800"},
                    ],
                },
                allow_different_len=False,
            )

    def test_show_all_button_by_default(self):
        """
        1. Проверяем, что valuesCount == 16, в этом случае фронт должен показать кнопку "Показать еще".
        2. Проверяем, что все значения пришли
        """
        response = self.report.request_json("place=prime&text=PinaColada&showVendors=top")
        self.assertFragmentIn(
            response,
            {
                "id": "7893318",
                "valuesCount": 16,
                "values": ElementCount(12),
            },
            allow_different_len=False,
        )
        response = self.report.request_json("place=prime&text=PinaColada&showVendors=all")
        self.assertFragmentIn(
            response,
            {
                "id": "7893318",
                "valuesCount": 16,
                "values": ElementCount(16),
            },
            allow_different_len=False,
        )

    def test_no_color_no_size(self):
        """
        Проверяем, что в фильтрах нет размеров и цветов
        """
        for pop_filters_params in ('rearr-factors=market_popular_gl_filters_on_search=1', 'popular-filters=1'):
            response = self.report.request_json("place=prime&text=PinaColada&{}".format(pop_filters_params))
            self.assertFragmentNotIn(response, {"filters": [{"id": "13887626"}]})
            self.assertFragmentNotIn(response, {"filters": [{"id": "600103"}]})

    @classmethod
    def prepare_popular_glparams_max_categ(cls):

        for category_id in range(898401, 898410):
            cls.index.hypertree += [
                HyperCategory(hid=category_id),
            ]

            param1 = 600110 + (category_id % 898401)
            param2 = 600120 + (category_id % 898401)
            cls.index.gltypes += [
                GLType(
                    param_id=param1,
                    hid=category_id,
                    gltype=GLType.ENUM,
                    cluster_filter=True,
                    values=[100, 200, 300, 400, 500, 600, 700, 800],
                    vendor=True,
                ),
                GLType(
                    param_id=param2,
                    hid=category_id,
                    gltype=GLType.ENUM,
                    cluster_filter=True,
                    values=[100, 200, 300, 400, 500, 600, 700, 800],
                    vendor=True,
                ),
            ]

            cls.index.offers += [
                Offer(
                    hid=category_id,
                    title="CubaLibre{}".format("+" if category_id < 898406 else "Plus"),
                    glparams=[GLParam(param_id=param1, value=param_value), GLParam(param_id=param2, value=param_value)],
                )
                for param_value in [100, 200]
            ]

    @skip('MARKETOUT-42703 флакает')
    def test_popular_glparams_max_categ(self):
        """
        Проверяем, что гл-параметры выводятся только для 5 категорий
        """
        for pop_filters_params in ('rearr-factors=market_popular_gl_filters_on_search=1', 'popular-filters=1'):
            response = self.report.request_json("place=prime&text=CubaLibre&{}".format(pop_filters_params))
            self.assertFragmentIn(
                response, {"filters": [{"values": [{"hid": category_id}]} for category_id in range(898401, 898406)]}
            )

            for category_id in range(898406, 898409):
                self.assertFragmentNotIn(response, {"filters": [{"values": [{"hid": category_id}]}]})

    def test_popular_initial_found(self):
        """
        Проверяем, что initialFound заполнен с фильтрами в т.ч.
        """
        for pop_filters_params in ('rearr-factors=market_popular_gl_filters_on_search=1', 'popular-filters=1'):
            for query in [
                "place=prime&text=CubaLibre&{}".format(pop_filters_params),
                "place=prime&text=CubaLibre&{}&onstock=1".format(pop_filters_params),
            ]:
                response = self.report.request_json(query + "&enable-hard-filters=0")
                self.assertFragmentIn(
                    response,
                    {"filters": [{"id": "600124", "values": [{"initialFound": 1, "found": 1, "hid": 898405}]}]},
                )

    @classmethod
    def prepare_popular_filters_top(cls):
        cls.index.hypertree += [HyperCategory(hid=898601), HyperCategory(hid=898602)]

        cls.index.gltypes += [
            GLType(
                param_id=70001,
                hid=898601,
                gltype=GLType.ENUM,
                cluster_filter=True,
                values=[
                    GLValue(100, text="Novosibirsk"),
                    GLValue(200, text="Bolear"),
                    GLValue(300, text="Rokoko"),
                    GLValue(400, text="Mllan"),
                    GLValue(500, text="Roma"),
                    GLValue(600, text="Paris"),
                    GLValue(700, text="New York"),
                    GLValue(800, text="Moscow"),
                ],
                short_enum_sort_type=GLType.EnumFieldSortingType.ALPHABETICAL,
                short_enum_count=3,
            ),
            GLType(
                param_id=70001,
                hid=898602,
                gltype=GLType.ENUM,
                cluster_filter=True,
                values=[
                    GLValue(1, text="Russia"),
                    GLValue(2, text="USA"),
                    GLValue(3, text="Austria"),
                    GLValue(4, text="Belgium"),
                    GLValue(5, text="Ireland"),
                    GLValue(6, text="India"),
                ],
                short_enum_sort_type=GLType.EnumFieldSortingType.OFFERS_COUNT,
                short_enum_count=5,
            ),
        ]

        cls.index.offers += [
            Offer(hid=898601, title="BuffaloBill-1", glparams=[GLParam(param_id=70001, value=param_value)])
            for param_value in [100, 200, 300, 400, 500, 600, 700, 800, 800]
        ]

        cls.index.offers += [
            Offer(hid=898602, title="BuffaloBill-2", glparams=[GLParam(param_id=70001, value=param_value)])
            for param_value in [1, 2, 3, 4, 5, 6]
        ]

    def test_popular_filters_top(self):
        """
        Проверяем, что в смерженном фильтре подцепились топы с более популярной категории: сортировка по цене
        + 3 штуки в topValues
        """
        for pop_filters_params in ('rearr-factors=market_popular_gl_filters_on_search=1', 'popular-filters=1'):
            response = self.report.request_json(
                "place=prime&text=BuffaloBill&{}&enable-hard-filters=0".format(pop_filters_params)
            )

            self.assertFragmentIn(
                response,
                {
                    "id": "70001",
                    "valuesCount": 14,
                    "valuesGroups": [
                        {"type": "top", "valuesIds": ["3", "4", "200"]},  # Austria  # Belgium  # Bolear
                        {
                            "type": "all",
                            "valuesIds": [
                                "3",
                                "200",
                                "400",
                                "800",
                                "700",
                                "100",
                                "600",
                                "300",
                                "500",
                                "1",
                                "2",
                                "4",
                                "5",
                                "6",
                            ],
                        },
                    ],
                },
                allow_different_len=False,
            )

    @classmethod
    def prepare_model_card_linked_filters(cls):
        """
        Создаем параметры с model_filter_index, скрытые на поисковой выдаче
        и связанные с ними параметры с model_filter_index=-1
        Создаем офферы с каждым из этих параметров
        """
        cls.index.gltypes += [
            GLType(
                param_id=16507001,
                hid=1650700,
                gltype=GLType.ENUM,
                position=1,
                has_model_filter_index=False,
                cluster_filter=True,
            ),
            GLType(
                param_id=16507002,
                hid=1650700,
                gltype=GLType.ENUM,
                model_filter_index=1,
                hidden=True,
                cluster_filter=True,
            ),
            GLType(
                param_id=16507003,
                hid=1650700,
                gltype=GLType.NUMERIC,
                position=2,
                has_model_filter_index=False,
                cluster_filter=True,
            ),
            GLType(
                param_id=16507004,
                hid=1650700,
                gltype=GLType.NUMERIC,
                model_filter_index=2,
                hidden=True,
                cluster_filter=True,
            ),
            GLType(
                param_id=16507005,
                hid=1650700,
                gltype=GLType.BOOL,
                position=3,
                has_model_filter_index=False,
                cluster_filter=True,
            ),
            GLType(
                param_id=16507006,
                hid=1650700,
                gltype=GLType.BOOL,
                position=3,
                model_filter_index=3,
                hidden=True,
                cluster_filter=True,
            ),
        ]

        cls.index.models += [
            Model(hyperid=1650701, hid=1650700),
            Model(hyperid=1650702, hid=1650700),
            Model(hyperid=1650703, hid=1650700),
        ]

        cls.index.offers += [
            Offer(
                hyperid=1650701,
                randx=1,
                title='global enum param',
                waremd5='OfferToCheckGlParams1w',
                glparams=[
                    GLParam(param_id=16507001, value=1),
                ],
            ),
            Offer(
                hyperid=1650701,
                randx=10,
                title='global and model enum params',
                waremd5='OfferToCheckGlParams2w',
                glparams=[GLParam(param_id=16507001, value=2), GLParam(param_id=16507002, value=2)],
            ),
            Offer(
                hyperid=1650701,
                randx=100,
                title='model enum param',
                waremd5='OfferToCheckGlParams3w',
                glparams=[
                    GLParam(param_id=16507002, value=3),
                ],
            ),
            Offer(
                hyperid=1650702,
                randx=1,
                title='global numeric param',
                waremd5='OfferToCheckGlParams4w',
                glparams=[
                    GLParam(param_id=16507003, value=50),
                ],
            ),
            Offer(
                hyperid=1650702,
                randx=10,
                title='global and model numeric params',
                waremd5='OfferToCheckGlParams5w',
                glparams=[
                    GLParam(param_id=16507003, value=100),
                    GLParam(param_id=16507004, value=100),
                ],
            ),
            Offer(
                hyperid=1650702,
                randx=100,
                title='model numeric param',
                waremd5='OfferToCheckGlParams6w',
                glparams=[
                    GLParam(param_id=16507004, value=150),
                ],
            ),
            Offer(
                hyperid=1650703,
                randx=1,
                title='global bool param',
                waremd5='OfferToCheckGlParams7w',
                glparams=[
                    GLParam(param_id=16507005, value=0),
                ],
            ),
            Offer(
                hyperid=1650703,
                randx=10,
                title='global and model bool params',
                waremd5='OfferToCheckGlParams8w',
                glparams=[
                    GLParam(param_id=16507005, value=1),
                    GLParam(param_id=16507006, value=1),
                ],
            ),
            Offer(
                hyperid=1650703,
                randx=100,
                title='model bool param',
                waremd5='OfferToCheckGlParams9w',
                glparams=[
                    GLParam(param_id=16507006, value=1),
                ],
            ),
        ]

    def test_model_card_linked_filters_on_prime(self):
        """Проверяем, что на prime один оффер и одна модель подходят
        под каждое значение каждого фильтра
        """
        for value in [1, 2]:
            response = self.report.request_json('place=prime&hid=1650700&glfilter=16507001:{}'.format(value))
            self.assertFragmentIn(
                response, {'results': [{'entity': 'product'}, {'entity': 'offer'}]}, allow_different_len=False
            )

        for value in ['50,50', '100,100']:
            response = self.report.request_json('place=prime&hid=1650700&glfilter=16507003:{}'.format(value))
            self.assertFragmentIn(
                response, {'results': [{'entity': 'product'}, {'entity': 'offer'}]}, allow_different_len=False
            )

        for value in [1]:
            response = self.report.request_json('place=prime&hid=1650700&glfilter=16507005:{}'.format(value))
            self.assertFragmentIn(
                response, {'results': [{'entity': 'product'}, {'entity': 'offer'}]}, allow_different_len=False
            )

    def test_enum_model_card_linked_filters(self):
        """
        Проверяем фильтрацию и вывод enum-параметров на productoffers
        """
        # Запрос без фильтров - на выдаче офферы с параметрами для КМ
        response = self.report.request_json('place=productoffers&hyperid=1650701&hid=1650700')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "global enum param"}, "filters": Absent()},
                    {"titles": {"raw": "global and model enum params"}, "filters": [{"id": "16507002"}]},
                    {'titles': {'raw': 'model enum param'}, "filters": [{"id": "16507002"}]},
                ]
            },
            allow_different_len=False,
        )

        # Запрос за оффером без параметра для КМ
        response = self.report.request_json(
            'place=productoffers&hyperid=1650701&hid=1650700&glfilter=16507001:1&offers-set=default'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "global enum param"}, "filters": Absent()},
                ]
            },
            allow_different_len=False,
        )

        # Запрос за оффером с параметрами для выдачи и КМ
        response = self.report.request_json(
            'place=productoffers&hyperid=1650701&hid=1650700&glfilter=16507001:2&offers-set=default'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        "titles": {"raw": "global and model enum params"},
                        "filters": [
                            {
                                "id": "16507002",
                                "type": "enum",
                                "kind": 2,
                                "position": 1,
                                "values": [{"initialFound": 1, "found": 1, "id": "2"}],
                            },
                        ],
                    },
                ]
            },
            allow_different_len=False,
        )

        # Запрос за оффером без параметра для выдачи
        # Фильтрация не проходит и возвращается ДО запроса без фильтров (по макс. randx)
        response = self.report.request_json(
            'place=productoffers&hyperid=1650701&hid=1650700&glfilter=16507001:3&offers-set=default'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        "titles": {"raw": "model enum param"},
                        "filters": [
                            {
                                "id": "16507002",
                                "type": "enum",
                                "kind": 2,
                                "position": 1,
                                "values": [{"initialFound": 1, "found": 1, "id": "3"}],
                            },
                        ],
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_numeric_model_card_linked_filters(self):
        """
        Проверяем фильтрацию и вывод numeric-параметров на productoffers
        """
        # Запрос без фильтров - на выдаче офферы с параметрами для КМ
        response = self.report.request_json('place=productoffers&hyperid=1650702&hid=1650700')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "global numeric param"}, "filters": Absent()},
                    {"titles": {"raw": "global and model numeric params"}, "filters": [{"id": "16507004"}]},
                    {'titles': {'raw': 'model numeric param'}, "filters": [{"id": "16507004"}]},
                ]
            },
            allow_different_len=False,
        )

        # Запрос за оффером без параметра для КМ
        response = self.report.request_json(
            'place=productoffers&hyperid=1650702&hid=1650700&glfilter=16507003:50,50&offers-set=default'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "global numeric param"}, "filters": Absent()},
                ]
            },
            allow_different_len=False,
        )

        # Запрос за оффером с параметрами для выдачи и КМ
        response = self.report.request_json(
            'place=productoffers&hyperid=1650702&hid=1650700&glfilter=16507003:100,100&offers-set=default'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        "titles": {"raw": "global and model numeric params"},
                        "filters": [
                            {
                                "id": "16507004",
                                "type": "number",
                                "kind": 2,
                                "position": 2,
                                "values": [
                                    {
                                        "id": "found",
                                        "max": "100",
                                        "min": "100",
                                    }
                                ],
                            },
                        ],
                    },
                ]
            },
            allow_different_len=False,
        )

        # Запрос за оффером без фильтра для выдачи
        # Фильтрация не проходит и возвращается ДО запроса без фильтров (по макс. randx)
        response = self.report.request_json(
            'place=productoffers&hyperid=1650702&hid=1650700&glfilter=16507003:150,150&offers-set=default'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        "titles": {"raw": "model numeric param"},
                        "filters": [
                            {
                                "id": "16507004",
                                "type": "number",
                                "kind": 2,
                                "position": 2,
                                "values": [
                                    {
                                        "id": "found",
                                        "max": "150",
                                        "min": "150",
                                    }
                                ],
                            },
                        ],
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_bool_model_card_linked_filters(self):
        """
        Проверяем фильтрацию и вывод boolean-параметров на productoffers
        """
        # Запрос без фильтров - на выдаче офферы с фильтрами для КМ
        response = self.report.request_json('place=productoffers&hyperid=1650703&hid=1650700')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {"titles": {"raw": "global bool param"}, "filters": Absent()},
                    {"titles": {"raw": "global and model bool params"}, "filters": [{"id": "16507006"}]},
                    {'titles': {'raw': 'model bool param'}, "filters": [{"id": "16507006"}]},
                ]
            },
            allow_different_len=False,
        )

        # Запрос с фильтром False - на выдаче только оффер БЕЗ параметра 16507005
        response = self.report.request_json(
            'place=productoffers&hyperid=1650703&hid=1650700&glfilter=16507005:0&offers-set=default'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        "titles": {"raw": "model bool param"},
                        "filters": [
                            {
                                "id": "16507006",
                                "type": "boolean",
                                "kind": 2,
                                "position": 3,
                                "values": [
                                    {"initialFound": 1, "found": 1, "id": "1"},
                                    {"initialFound": 0, "found": 0, "id": "0"},
                                ],
                            },
                        ],
                    },
                ]
            },
            allow_different_len=False,
        )

        # Запрос с фильтром True, на выдаче старший оффер по randx
        response = self.report.request_json(
            'place=productoffers&hyperid=1650703&hid=1650700&glfilter=16507005:1&offers-set=default'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        "titles": {"raw": "global and model bool params"},
                        "filters": [
                            {
                                "id": "16507006",
                                "type": "boolean",
                                "kind": 2,
                                "position": 3,
                                "values": [
                                    {"initialFound": 1, "found": 1, "id": "1"},
                                    {"initialFound": 0, "found": 0, "id": "0"},
                                ],
                            },
                        ],
                    },
                ]
            },
            allow_different_len=False,
        )

    def check_offerinfo_offer_param(self, ware_md5, is_model_card, filters):
        request_url = 'place=offerinfo&rids=0&regset=2&offerid={}'.format(ware_md5)
        if is_model_card:
            request_url += '&show-model-card-params=1'
        response = self.report.request_json(request_url)
        self.assertFragmentIn(response, {'results': [{'wareId': ware_md5, 'filters': filters}]})

    def get_enum_offer_filter(self, param_id, param_value):
        return [
            {
                "id": str(param_id),
                "type": "enum",
                "kind": 2,
                "values": [{"initialFound": 1, "found": 1, "id": str(param_value)}],
            }
        ]

    def get_numeric_offer_filter(self, param_id, param_value):
        return [
            {
                "id": str(param_id),
                "type": "number",
                "kind": 2,
                "values": [
                    {
                        "id": "found",
                        "max": str(param_value),
                        "min": str(param_value),
                    }
                ],
            }
        ]

    def get_bool_offer_filter(self, param_id, param_value):
        return [{"id": str(param_id), "type": "boolean", "kind": 2, "values": [{"id": str(param_value)}]}]

    def test_offerinfo_model_card_linked_enum_filters(self):
        """Проверяем вывод связанных enum-параметров на offerinfo
        Проверяем соотвествие параметров оффера в индексе и на выдаче,
        в т.ч. наличие/отсутствие параметра
        """
        self.check_offerinfo_offer_param('OfferToCheckGlParams1w', False, self.get_enum_offer_filter(16507001, 1))
        self.check_offerinfo_offer_param('OfferToCheckGlParams1w', True, Absent())
        self.check_offerinfo_offer_param('OfferToCheckGlParams2w', False, self.get_enum_offer_filter(16507001, 2))
        self.check_offerinfo_offer_param('OfferToCheckGlParams2w', True, self.get_enum_offer_filter(16507002, 2))
        self.check_offerinfo_offer_param('OfferToCheckGlParams3w', False, Absent())
        self.check_offerinfo_offer_param('OfferToCheckGlParams3w', True, self.get_enum_offer_filter(16507002, 3))

    def test_offerinfo_model_card_linked_numeric_filters(self):
        """Проверяем вывод связанных numeric-параметров на offerinfo
        Проверяем соотвествие параметров оффера в индексе и на выдаче,
        в т.ч. наличие/отсутствие параметра
        """
        self.check_offerinfo_offer_param('OfferToCheckGlParams4w', False, self.get_numeric_offer_filter(16507003, 50))
        self.check_offerinfo_offer_param('OfferToCheckGlParams4w', True, Absent())
        self.check_offerinfo_offer_param('OfferToCheckGlParams5w', False, self.get_numeric_offer_filter(16507003, 100))
        self.check_offerinfo_offer_param('OfferToCheckGlParams5w', True, self.get_numeric_offer_filter(16507004, 100))
        self.check_offerinfo_offer_param('OfferToCheckGlParams6w', False, Absent())
        self.check_offerinfo_offer_param('OfferToCheckGlParams6w', True, self.get_numeric_offer_filter(16507004, 150))

    def test_offerinfo_model_card_linked_bool_filters(self):
        """Проверяем вывод связанных bool-параметров на offerinfo
        Проверяем соотвествие параметров оффера в индексе и на выдаче,
        в т.ч. наличие/отсутствие параметра
        """
        self.check_offerinfo_offer_param('OfferToCheckGlParams7w', False, self.get_bool_offer_filter(16507005, 0))
        self.check_offerinfo_offer_param('OfferToCheckGlParams7w', True, Absent())
        self.check_offerinfo_offer_param('OfferToCheckGlParams8w', False, self.get_bool_offer_filter(16507005, 1))
        self.check_offerinfo_offer_param('OfferToCheckGlParams8w', True, self.get_bool_offer_filter(16507006, 1))
        self.check_offerinfo_offer_param('OfferToCheckGlParams9w', False, Absent())
        self.check_offerinfo_offer_param('OfferToCheckGlParams9w', True, self.get_bool_offer_filter(16507006, 1))

    @classmethod
    def prepare_offer_bool_filter(cls):
        cls.index.gltypes += [
            GLType(param_id=703, hid=7, gltype=GLType.BOOL, cluster_filter=True),
        ]

        cls.index.models += [
            Model(hid=7, hyperid=4007),
        ]

        cls.index.offers += [
            Offer(title="offer_1", hid=7, hyperid=4007, glparams=[GLParam(param_id=703, value=0)]),
            Offer(title="offer_2", hid=7, hyperid=4007, glparams=[GLParam(param_id=703, value=1)]),
        ]

    def test_offer_bool_filter(self):
        """Проверяется, что для офферов правильно вычисляется initialFound и found для false знаяения bool фильтра"""
        response = self.report.request_json('place=prime&hid=7')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "offer_2"},
                    "filters": [
                        {
                            "id": "703",
                            "type": "boolean",
                            "values": [
                                {"initialFound": 1, "found": 1, "id": "1"},
                                {"initialFound": 0, "found": 0, "id": "0"},
                            ],
                        }
                    ],
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "offer_1"},
                    "filters": [
                        {
                            "id": "703",
                            "type": "boolean",
                            "values": [
                                {"initialFound": 0, "found": 0, "id": "1"},
                                {"initialFound": 1, "found": 1, "id": "0"},
                            ],
                        }
                    ],
                },
            ],
        )

    @classmethod
    def prepare_hide_single_filter_values(cls):
        cls.index.gltypes += [
            GLType(param_id=901, hid=187, gltype=GLType.ENUM, cluster_filter=True, values=[1, 2, 3]),
        ]
        cls.index.models += [
            Model(hid=187, hyperid=12001),
        ]

        cls.index.offers += [
            Offer(title="super magic", hid=187, hyperid=12001, glparams=[GLParam(param_id=901, value=1)]),
            Offer(title="super power", hid=187, hyperid=12001, glparams=[GLParam(param_id=901, value=2)]),
        ]

    def test_hide_single_filter_values(self):
        """
        Проверяем, что фильтра в выдаче нет, когда только одно значение должно быть
        """
        response = self.report.request_json(
            'place=prime&hid=187&text=super&rearr-factors=market_hide_single_enum_filter_values=1'
        )
        self.assertFragmentIn(
            response, {"search": {}, "filters": [{"id": "901", "values": [{"id": "1"}, {"id": "2"}]}]}
        )

        response = self.report.request_json(
            'place=prime&hid=187&text=magic&rearr-factors=market_hide_single_enum_filter_values=1'
        )
        self.assertFragmentNotIn(response, {"search": {}, "filters": [{"id": "901"}]})

        # без экспа - есть фильтр
        response = self.report.request_json('place=prime&hid=187&text=magic')
        self.assertFragmentIn(response, {"search": {}, "filters": [{"id": "901", "values": [{"id": "1"}]}]})

    @classmethod
    def prepare_through_filters(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=5000,
                name='A',
                children=[
                    HyperCategory(hid=5010, name='AA'),
                    HyperCategory(hid=5020, name='AB'),
                ],
            )
        ]

        cls.index.navtree += [
            NavCategory(
                nid=5000,
                name='Virtual A',
                is_blue=True,
                children=[
                    NavCategory(nid=5010, name='NAA', hid=5010, is_blue=True),
                    NavCategory(nid=5020, name='NAB', hid=5020, is_blue=True),
                ],
            )
        ]

        cls.index.models += [
            Model(hyperid=5000, hid=5000),
            Model(hyperid=5001, hid=5000),
            Model(hyperid=5010, hid=5010),
            Model(hyperid=5011, hid=5010),
            Model(hyperid=5020, hid=5020),
            Model(hyperid=5021, hid=5020),
            Model(hyperid=5022, hid=5020),
        ]

        cls.index.mskus += [
            MarketSku(
                title='vendortest ' + additional_text,
                hyperid=id,
                sku=id,
                blue_offers=[BlueOffer(price=100)],
                glparams=[
                    GLParam(param_id=THROUGH_PARAM_ID, value=filter_value),
                    GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=filter_value),
                    GLParam(param_id=NOT_THROUGH_PARAM_ID, value=filter_value),
                ],
            )
            for id, filter_value, additional_text in [
                (5010, 1, "mashina"),
                (5011, 2, "kraska"),
                (5020, 2, "mashina"),
                (5021, 3, "kraska"),
                (5022, 4, "mashina"),
            ]
        ]

        cls.index.gltypes += [
            GLType(
                param_id=param_id,
                vendor=param_id == Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID,
                hid=5000,
                values=[1, 2, 3, 4],
                through=True,
            )
            for param_id in [Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, THROUGH_PARAM_ID]
        ]

        cls.index.gltypes += [
            GLType(
                param_id=param_id,
                vendor=param_id == Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID,
                hid=5010,
                values=[1, 2],
                through=True,
            )
            for param_id in [Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, THROUGH_PARAM_ID]
        ]

        cls.index.gltypes += [
            GLType(
                param_id=param_id,
                vendor=param_id == Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID,
                hid=5020,
                values=[2, 3, 4],
                through=True,
            )
            for param_id in [Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, THROUGH_PARAM_ID]
        ]

        # Добавляем параметр, который не хотим показывать в текстовом поиске
        cls.index.gltypes += [
            GLType(param_id=NOT_THROUGH_PARAM_ID, hid=hid, values=[1, 2, 3, 4, 5]) for hid in [5000, 5010, 5020]
        ]

    def check_filter_exists(self, response, param_id, values_and_found=None):
        # Проверяем, что сквозной параметр был показан при текстовом поиске
        self.assertFragmentIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "id": str(param_id),
                        "values": [
                            {
                                "id": value,
                                "found": found,
                                "initialFound": found,
                            }
                            for value, found in (values_and_found.items() if values_and_found else [])
                        ],
                    }
                ],
            },
        )

    def check_through_filters(self, response, values_and_found):
        # Проверяем, что сквозной параметр был показан при текстовом поиске
        for param_id in [Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, THROUGH_PARAM_ID]:
            self.check_filter_exists(response, param_id, values_and_found)

    def check_filter_vendor(self, response, values_and_found):
        self.check_filter_exists(response, Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, values_and_found)

    def check_filter_missed(self, response, param_id):
        self.assertFragmentNotIn(response, {"search": {}, "filters": [{"id": str(param_id)}]})

    def check_not_through_filter_missed(self, response):
        # Проверяем, что другие gl параметры найденных оферов не были показаны
        self.check_filter_missed(response, NOT_THROUGH_PARAM_ID)

    def check_filter_checked(self, response, param_id, checked_value, values):
        self.assertFragmentIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        'id': str(param_id),
                        'values': [
                            {'id': str(value), 'checked': True if value == checked_value else Absent()}
                            for value in values
                        ],
                    }
                ],
            },
        )

    def check_through_filter_checked(self, response, checked_value=None):
        self.check_filter_checked(response, THROUGH_PARAM_ID, checked_value, [1, 2, 3])

    def test_through_filters(self):
        '''
        Проверяем, что сквозные параметры показываются, если задан флаг market_through_gl_filters_on_search=1
        '''
        response = self.report.request_json(
            'place=prime&text=vendortest&rearr-factors=market_through_gl_filters_on_search=1&enable-hard-filters=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.check_through_filters(response, {"1": 1, "2": 2, "3": 1, "4": 1})
        self.check_not_through_filter_missed(response)

        response = self.report.request_json(
            'place=prime&text=mashina&rearr-factors=market_through_gl_filters_on_search=1&enable-hard-filters=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.check_through_filters(response, {"1": 1, "2": 1})
        self.check_not_through_filter_missed(response)

        response = self.report.request_json(
            'place=prime&text=kraska&rearr-factors=market_through_gl_filters_on_search=1&enable-hard-filters=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.check_through_filters(response, {"2": 1, "3": 1})
        self.check_not_through_filter_missed(response)

        # Без флага фильтр не выводится
        response = self.report.request_json(
            'place=prime&text=vendortest&rearr-factors=market_metadoc_search=no&enable-hard-filters=0'
        )
        for param_id in [THROUGH_PARAM_ID, Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID]:
            self.assertFragmentNotIn(response, {"id": str(param_id)})
        self.check_not_through_filter_missed(response)

        # Если задать категорию, то выводятся все фильтры из категории
        response = self.report.request_json(
            'place=prime&text=vendortest&hid=5000&rearr-factors=market_through_gl_filters_on_search=1&enable-hard-filters=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.check_through_filters(response, {"1": 1, "2": 2, "3": 1, "4": 1})
        self.assertFragmentIn(response, {"search": {}, "filters": [{"id": str(NOT_THROUGH_PARAM_ID)}]})

        # Если выставить значение фильтра в url запроса то это значение помечается как checked в filters
        response = self.report.request_json(
            'place=prime&text=vendortest&rearr-factors=market_through_gl_filters_on_search=1&enable-hard-filters=0&glfilter={}:2'.format(
                THROUGH_PARAM_ID
            )
            + '&rearr-factors=market_metadoc_search=no'
        )
        self.check_through_filter_checked(response, checked_value=2)

        # Если значение фильтра не выставлено в url запроса то свойство checked отсуствует в значениях фильтрах
        response = self.report.request_json(
            'place=prime&text=vendortest&rearr-factors=market_through_gl_filters_on_search=1&enable-hard-filters=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.check_through_filter_checked(response, checked_value=None)

    def test_through_filters_department(self):
        for w in ['vendortest', 'mashina', 'kraska']:
            response = self.report.request_json(
                'place=prime&text={}&rearr-factors=market_through_gl_filters_on_search=1&enable-hard-filters=0'.format(
                    w
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "17354681",
                            "meta": {
                                "department": {
                                    "entity": "category",
                                    "id": 5000,
                                    "name": "A",
                                    "isLeaf": False,
                                }
                            },
                        },
                        {
                            "id": "7893318",
                            "meta": {
                                "department": {
                                    "entity": "category",
                                    "id": 5000,
                                    "name": "A",
                                    "isLeaf": False,
                                }
                            },
                        },
                    ]
                },
            )

    def test_through_filter_vendor(self):
        '''
        Проверяем что только вендор показываются, если задан флаг market_through_gl_filters_on_search=vendor
        '''

        response = self.report.request_json(
            'place=prime&text=vendortest&rearr-factors=market_through_gl_filters_on_search=vendor'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.check_filter_vendor(response, {"1": 1, "2": 2, "3": 1, "4": 1})
        self.check_filter_missed(response, THROUGH_PARAM_ID)
        self.check_filter_missed(response, NOT_THROUGH_PARAM_ID)

        response = self.report.request_json(
            'place=prime&text=mashina&rearr-factors=market_through_gl_filters_on_search=vendor'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.check_filter_vendor(response, {"1": 1, "2": 1})
        self.check_filter_missed(response, THROUGH_PARAM_ID)
        self.check_filter_missed(response, NOT_THROUGH_PARAM_ID)

        response = self.report.request_json(
            'place=prime&text=kraska&rearr-factors=market_through_gl_filters_on_search=1&enable-hard-filters=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.check_through_filters(response, {"2": 1, "3": 1})
        self.check_filter_missed(response, NOT_THROUGH_PARAM_ID)

        # Фильтр выводится также на бестекстовом поиске по nid
        # Сейчас у виртуальных узлов фильтры берутся из первого ребенка
        response = self.report.request_json(
            'place=prime&nid=5000&rgb=blue&rearr-factors=market_through_gl_filters_on_search_blue=vendor'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.check_filter_vendor(response, {"1": 1, "2": 2})

        # Без флага фильтр не выводится
        response = self.report.request_json('place=prime&text=vendortest&enable-hard-filters=0')
        self.check_filter_missed(response, Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID)
        self.check_filter_missed(response, THROUGH_PARAM_ID)
        self.check_filter_missed(response, NOT_THROUGH_PARAM_ID)

        # Если задать категорию, то выводятся все фильтры из категории
        response = self.report.request_json(
            'place=prime&text=vendortest&hid=5000&rearr-factors=market_through_gl_filters_on_search=vendor'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.check_through_filters(response, {"1": 1, "2": 2, "3": 1, "4": 1})
        self.check_filter_vendor(response, {"1": 1, "2": 2, "3": 1, "4": 1})
        self.check_filter_exists(response, NOT_THROUGH_PARAM_ID)

        # Если выставить значение фильтра в url запроса то это значение помечается как checked в filters
        response = self.report.request_json(
            'place=prime&text=vendortest&rearr-factors=market_through_gl_filters_on_search=vendor&glfilter={}:2'.format(
                Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID
            )
            + '&rearr-factors=market_metadoc_search=no'
        )
        self.check_filter_checked(
            response, Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, checked_value=2, values=[1, 2, 3, 4]
        )

        # Если значение фильтра не выставлено в url запроса то свойство checked отсуствует в значениях фильтрах
        response = self.report.request_json(
            'place=prime&text=vendortest&rearr-factors=market_through_gl_filters_on_search=vendor'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.check_filter_checked(
            response, Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, checked_value=None, values=[1, 2, 3, 4]
        )

    def test_through_filter_all(self):
        '''
        Проверяем что все сквозные показываются, если задан флаг market_through_gl_filters_on_search=all
        Даже, если это не просто текстовый поиск
        '''

        response = self.report.request_json(
            'place=prime&text=vendortest&rearr-factors=market_through_gl_filters_on_search=all&enable-hard-filters=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.check_through_filters(response, {"1": 1, "2": 2, "3": 1, "4": 1})
        self.check_filter_missed(response, NOT_THROUGH_PARAM_ID)

        response = self.report.request_json(
            'place=prime&text=mashina&rearr-factors=market_through_gl_filters_on_search=all&enable-hard-filters=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.check_through_filters(response, {"1": 1, "2": 1})
        self.check_filter_missed(response, NOT_THROUGH_PARAM_ID)

        response = self.report.request_json(
            'place=prime&text=kraska&rearr-factors=market_through_gl_filters_on_search=1&enable-hard-filters=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.check_through_filters(response, {"2": 1, "3": 1})
        self.check_filter_missed(response, NOT_THROUGH_PARAM_ID)

        # Фильтр выводится также на бестекстовом поиске по nid
        # Сейчас у виртуальных узлов фильтры берутся из первого ребенка
        response = self.report.request_json(
            'place=prime&nid=5000&rgb=blue&rearr-factors=market_through_gl_filters_on_search_blue=all&enable-hard-filters=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.check_through_filters(response, {"1": 1, "2": 2})

        # Без флага фильтр не выводится
        response = self.report.request_json('place=prime&text=vendortest&enable-hard-filters=0')
        self.check_filter_missed(response, Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID)
        self.check_filter_missed(response, THROUGH_PARAM_ID)
        self.check_filter_missed(response, NOT_THROUGH_PARAM_ID)

        # Если задать категорию, то выводятся все фильтры из категории
        response = self.report.request_json(
            'place=prime&text=vendortest&hid=5000&rearr-factors=market_through_gl_filters_on_search=all&enable-hard-filters=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.check_through_filters(response, {"1": 1, "2": 2, "3": 1, "4": 1})
        self.check_filter_exists(response, NOT_THROUGH_PARAM_ID)

        # Если выставить значение фильтра в url запроса то это значение помечается как checked в filters
        # Другой фильтр остается невыбранным
        response = self.report.request_json(
            'place=prime&text=vendortest&rearr-factors=market_through_gl_filters_on_search=all&enable-hard-filters=0&glfilter={}:2'.format(
                Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID
            )
            + '&rearr-factors=market_metadoc_search=no'
        )
        self.check_filter_checked(
            response, Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, checked_value=2, values=[1, 2, 3, 4]
        )
        self.check_filter_checked(response, THROUGH_PARAM_ID, checked_value=None, values=[1, 2, 3, 4])

        response = self.report.request_json(
            'place=prime&text=vendortest&rearr-factors=market_through_gl_filters_on_search=all&enable-hard-filters=0&glfilter={}:2'.format(
                THROUGH_PARAM_ID
            )
            + '&rearr-factors=market_metadoc_search=no'
        )
        self.check_filter_checked(response, THROUGH_PARAM_ID, checked_value=2, values=[1, 2, 3, 4])
        self.check_filter_checked(
            response, Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, checked_value=None, values=[1, 2, 3, 4]
        )

        # Если значение фильтра не выставлено в url запроса то свойство checked отсуствует в значениях фильтрах
        response = self.report.request_json(
            'place=prime&text=vendortest&rearr-factors=market_through_gl_filters_on_search=all&enable-hard-filters=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.check_filter_checked(
            response, Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, checked_value=None, values=[1, 2, 3, 4]
        )
        self.check_filter_checked(response, THROUGH_PARAM_ID, checked_value=None, values=[1, 2, 3, 4])

    @classmethod
    def prepare_through_filters_for_order_test(cls):
        paramsPositions = {
            THROUGH_PARAMS[0]: 10,
            THROUGH_PARAMS[1]: 11,
            THROUGH_PARAMS[2]: 12,
            THROUGH_PARAMS[3]: 13,
            THROUGH_PARAMS[4]: 14,
            THROUGH_PARAMS[5]: 15,
        }
        hids = [
            {
                "hid": 5050,
                "children": [
                    {
                        "hid": 5051,
                        "relevance": 95,
                        "mskus": 10,
                        "params": [
                            THROUGH_PARAMS[1],
                        ],
                    }
                ],
            },
            {
                "hid": 5060,
                "children": [
                    {
                        "hid": 5061,
                        "relevance": 81,
                        "mskus": 4,
                        "params": [THROUGH_PARAMS[1], THROUGH_PARAMS[2], 50615061],
                    },
                    {
                        "hid": 5062,
                        "relevance": 83,
                        "mskus": 4,
                        "params": [THROUGH_PARAMS[2], THROUGH_PARAMS[3], THROUGH_PARAMS[0]],
                    },
                ],
            },
            {
                "hid": 5070,
                "children": [
                    {"hid": 5071, "relevance": 96, "mskus": 4, "params": [THROUGH_PARAMS[5]]},
                    {
                        "hid": 5072,
                        "relevance": 89,
                        "children": [
                            {
                                "hid": 5073,
                                "relevance": 89,
                                "mskus": 4,
                                "params": [THROUGH_PARAMS[3], THROUGH_PARAMS[4]],
                            },
                        ],
                    },
                ],
            },
        ]

        def leafs(hids):
            result = []
            for hidinfo in hids:
                if "children" not in hidinfo:
                    result.append(hidinfo)
                else:
                    result += leafs(hidinfo["children"])
            return result

        categoryLeafs = leafs(hids)
        cls.index.models += [Model(hyperid=leaf["hid"], hid=leaf["hid"]) for leaf in categoryLeafs]

        def traverseHids(hids, executor):
            result = []
            for hidinfo in hids:
                children = None
                if "children" in hidinfo:
                    children = traverseHids(hidinfo["children"], executor)
                result.append(executor(hidinfo, children))
            return result

        def getHyperCategories(hidinfo, children):
            return HyperCategory(hid=hidinfo["hid"], name='category-{}'.format(str(hidinfo["hid"])), children=children)

        def getNavigation(hidinfo, children):
            name = 'navigation-{}'.format(str(hidinfo["hid"]))
            return NavCategory(
                nid=hidinfo["hid"] * 10, hid=hidinfo["hid"], name=name, uniq_name=name, children=children
            )

        cls.index.hypertree += traverseHids(hids, getHyperCategories)
        cls.index.navtree += traverseHids(hids, getNavigation)

        for leaf in categoryLeafs:
            cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, leaf["hid"]).respond(leaf["relevance"])

        cls.index.mskus += [
            MarketSku(
                title='ordertest ' + str(leafInfo["hid"]),
                hyperid=leafInfo["hid"],
                sku=leafInfo["hid"] * 10 + i,
                blue_offers=[BlueOffer(offerid="order_test_offer" + str(leafInfo["hid"]) + "_" + str(i))],
                glparams=[GLParam(param_id=param_id, value=i % 3 + 1) for param_id in leafInfo["params"]],
            )
            for leafInfo in categoryLeafs
            for i in range(leafInfo["mskus"])
        ]

        cls.index.gltypes += [
            GLType(
                param_id=param_id,
                hid=leafInfo["hid"],
                values=[1, 2, 3],
                position=paramsPositions[param_id],
                through=True,
            )
            for param_id, leafInfo in itertools.product(THROUGH_PARAMS, categoryLeafs)
        ]

        cls.index.gltypes += [
            GLType(
                param_id=50615061,
                hid=5061,
                values=[1, 2, 3],
                position=9,
            )
        ]

    def check_filters_in_order(self, response, expected_order):
        self.assertFragmentIn(
            response,
            {"search": {}, "filters": [{"id": str(id), "meta": department} for id, department in expected_order]},
            preserve_order=True,
        )

    def test_non_through_filters(self):
        '''
        проверяем, что включение редиректов на синем не ломает вывод фильтров в поиске
        '''
        rearr = '&rearr-factors=market_through_gl_filters_on_search=1;market_blue_through_single_nid_redirect=1'
        for rearr in ['', rearr]:
            response = self.report.request_json('place=prime&hid=5061' + rearr)
            self.assertFragmentIn(
                response,
                {
                    "search": {},
                    "filters": [
                        {
                            "id": "50615061",
                        },
                        {
                            "id": "17354854",
                        },
                        {
                            "id": "17354844",
                        },
                    ],
                },
            )

    def request_through_filters(self, redirects):
        req = 'place=prime&text=ordertest&enable-hard-filters=0&rearr-factors=market_through_gl_filters_on_search=1;turn_off_nid_intents_on_serp=0'
        if redirects:
            req += ';market_blue_through_single_nid_redirect=1'
        return self.report.request_json(req)

    def test_through_filters_order(self):
        '''
        Проверяем порядок выдачи сквозных фильтров
        фильтры сортируются по следующим признакам (в порядке приоритета в сравнении)
        1. наличие фильтра в нескольких департаментах
        2. приоритет категории (для фильтра из нескольких категорий берётся самый большой приоритет из всех его категорий)
        3. приоритет фильтра в категории
        '''
        response = self.request_through_filters(False)
        self.check_filters_in_order(
            response,
            [
                (THROUGH_PARAMS[index[0]], index[1])
                for index in [
                    # описание вида [вес департамента|вес категории|позиция фильтра]
                    (1, {}),  # --|95|13
                    (3, {}),  # --|90|11
                    (5, {"department": {"id": 5070}}),  # 96|96|15
                    (4, {"department": {"id": 5070}}),  # 96|90|14
                    (0, {"department": {"id": 5060}}),  # 83|83|10
                    (2, {"department": {"id": 5060}}),  # 83|83|12
                ]
            ],
        )

    def check_filters_redirects(self, response, expected_order):
        self.assertFragmentIn(
            response, {"filters": [{"id": str(id), "redirect": redirect} for id, redirect in expected_order]}
        )

    def test_throught_filters_redirect(self):
        '''
        Проверяем редирект в фильтрах
        Для фильтров участвующих только в одной категории
        необходимо добавить редиректы на nid и hid
        в запросах добавлен rearr флаг
        market_blue_through_single_nid_redirect=1
        '''

        def get_redirect_description(hid, nid):
            return {
                "nid": {
                    "id": nid[0],
                    "slug": nid[1],
                },
                "hid": {
                    "id": hid[0],
                    "slug": hid[1],
                },
            }

        response = self.request_through_filters(True)
        self.check_filters_redirects(
            response,
            [
                (THROUGH_PARAMS[index[0]], index[1])
                for index in [
                    (1, Absent()),  # фильтр в нескольких департаментах
                    (3, Absent()),  # фильтр в нескольких департаментах
                    (5, get_redirect_description((5071, "navigation-5071"), (50710, "navigation-5071"))),
                    (4, get_redirect_description((5073, "navigation-5073"), (50730, "navigation-5073"))),
                    (0, get_redirect_description((5062, "navigation-5062"), (50620, "navigation-5062"))),
                    (2, Absent()),  # фильтр в нескольких категориях
                ]
            ],
        )

    def test_throught_filters_redirect_disabled(self):
        '''
        по дефолту редиректов в сквозных фильтрах быть не должно
        '''

        response = self.request_through_filters(False)
        self.check_filters_redirects(response, [(param, Absent()) for param in THROUGH_PARAMS])

    @classmethod
    def prepare_active_only_filters(cls):
        def create_msku(msku_id, params_and_values, discount):
            return MarketSku(
                title='offertitle' + str(msku_id),
                hyperid=6000 + msku_id,
                sku=msku_id,
                blue_offers=[BlueOffer(price=100, price_old=120 if discount else None)],
                glparams=[GLParam(param_id=param_id, value=value_id) for param_id, value_id in params_and_values],
            )

        cls.index.mskus += [
            create_msku(1, [[201, 1], [202, 1], [203, 0], [204, 0]], discount=True),
            create_msku(2, [[201, 1], [202, 2], [203, 1], [204, 1]], discount=True),
            create_msku(3, [[201, 2], [202, 3], [203, 1], [204, 1]], discount=False),
            create_msku(4, [[201, 3]], discount=False),
            create_msku(5, [[201, 4], [203, 0], [204, 0]], discount=True),
        ]

        cls.index.gltypes += [
            GLType(gltype=GLType.ENUM, param_id=param_id, hid=6000, values=[1, 2, 3, 4, 5]) for param_id in [201, 202]
        ]

        # Параметр, имеющий значение 0 и 1
        cls.index.gltypes += [GLType(gltype=GLType.BOOL, param_id=203, hid=6000, hasboolno=True)]

        # Фильтр, который может иметь только значение 1
        cls.index.gltypes += [GLType(gltype=GLType.BOOL, param_id=204, hid=6000, hasboolno=False)]

        cls.index.models += [Model(hyperid=6000 + id, hid=6000) for id in range(0, 6)]

    def check_has_options(self, response, filter_id, values_with_initial_found):
        self.assertFragmentIn(
            response,
            {
                'id': str(filter_id),
                'values': [
                    {
                        'id': value,
                        'initialFound': Greater(0),
                    }
                    for value in values_with_initial_found
                ],
            },
            allow_different_len=False,
        )

    def check_initial_found_calculates(self, response):
        self.check_has_options(response, 201, ['1', '2', '3', '4'])
        self.check_has_options(response, 202, ['1', '2', '3'])
        self.check_has_options(response, 203, ['0', '1'])
        self.check_has_options(response, 204, ['0', '1'])

    def check_found_zero(self, response, filter_id, values):
        self.assertFragmentIn(
            response,
            {
                'id': str(filter_id),
                'values': [
                    {
                        'id': value,
                        'found': 0,
                    }
                    for value in values
                ],
            },
        )

    def check_found_greater_than_zero(self, response, filter_id, values):
        self.assertFragmentIn(
            response,
            {
                'id': str(filter_id),
                'values': [
                    {
                        'id': value,
                        'found': Greater(0),
                    }
                    for value in values
                ],
            },
            allow_different_len=False,
        )

    def check_bool_found_greater_than_zero(self, response, filter_id, values):
        self.assertFragmentIn(
            response,
            {
                'id': str(filter_id),
                'values': [
                    {
                        'value': value,
                        'found': Greater(0),
                    }
                    for value in values
                ],
            },
            allow_different_len=False,
        )

    def check_bool_filter(self, response, filter_id, values):
        self.assertFragmentIn(
            response,
            {
                'id': str(filter_id),
                'values': [
                    {
                        'value': value,
                        'found': found,
                    }
                    for value, found in values.items()
                ],
            },
            allow_different_len=False,
        )

    def check_filter_is_hidden(self, response, filter_id):
        self.assertFragmentNotIn(
            response,
            {
                "search": NotEmpty(),
                "filters": [
                    {
                        "id": str(filter_id),
                    }
                ],
            },
        )

    @classmethod
    def prepare_through_filters_with_units(cls):
        cls.index.models += [Model(hyperid=5110, hid=5110), Model(hyperid=5120, hid=5120)]

        cls.index.hypertree += [
            HyperCategory(
                hid=5100,
                name='A',
                children=[HyperCategory(hid=5110 + id * 10, name='A' + chr(ord('A') + id)) for id in range(2)],
            )
        ]

        cls.index.mskus += [
            MarketSku(
                title='testunitsonthroughfilters ' + str(hid),
                hyperid=hid,
                sku=hid + i,
                blue_offers=[BlueOffer(offerid="through_units_offer" + str(hid) + str(i))],
                glparams=[GLParam(param_id=THROUGH_PARAMS[1], value=values[i])],
            )
            for values, hid in [([1, 2, 3, 4], 5110), ([4, 5, 6, 7], 5120)]
            for i in range(len(values))
        ]

        for values, hid, units in [([1, 2, 3, 4], 5110, [1, 2]), ([4, 5, 6, 7], 5120, [2, 3])]:
            cls.index.gltypes += [
                GLType(
                    param_id=THROUGH_PARAMS[1],
                    hid=hid,
                    gltype=GLType.ENUM,
                    subtype='size',
                    name=u'Размер',
                    unit_param_id=600104,
                    through=True,
                    values=[
                        GLValue(value_id=value, text=str(value), unit_value_id=int((value - 1) / 3) + 1)
                        for value in values
                    ],
                ),
                GLType(
                    param_id=600104,
                    hid=hid,
                    gltype=GLType.ENUM,
                    values=[GLValue(value_id=unit, text='unit' + str(unit), default=unit == 1) for unit in units],
                ),
            ]

    def test_through_filter_with_units(self):
        response = self.report.request_json(
            'place=prime&text=testunitsonthroughfilters&rearr-factors=market_through_gl_filters_on_search=1&enable-hard-filters=0'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "id": str(THROUGH_PARAMS[1]),
                        "units": [
                            {"values": [{"value": str(value)} for value in values]}
                            for values in [[1, 2, 3], [4, 5, 6], [7]]
                        ],
                    }
                ],
            },
        )

    @classmethod
    def prepare_glfilters_popularity(cls):
        cls.index.gltypes += [
            GLType(param_id=3003, hid=1, gltype=GLType.ENUM, hidden=False, values=[3, 4]),
            GLType(param_id=4004, hid=4, gltype=GLType.ENUM, hidden=False, values=[5, 6]),
        ]

        cls.index.offers += [
            Offer(
                hid=1,
                title='good offer',
                glparams=[
                    GLParam(param_id=3003, value=3),
                ],
            ),
            Offer(
                hid=4,
                title='even better offer',
                glparams=[
                    GLParam(param_id=4004, value=5),
                ],
            ),
        ]

        cls.index.filters_popularity += [FiltersPopularity(hid=1, key=3003, value=3, popularity=42)]

    def test_glfilters_popularity(self):
        response = self.report.request_json('place=prime&hid=1')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "3003",
                        "values": [{"popularity": 42, "id": "3"}],
                    }
                ]
            },
        )

        response = self.report.request_json('place=prime&hid=4')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "4004",
                        "values": [{"popularity": Absent(), "id": "5"}],
                    }
                ]
            },
        )

    @classmethod
    def prepare_non_enum_through_filters(cls):
        cls.index.models += [
            Model(hyperid=2000, hid=5, title='filternumeric', glparams=[GLParam(param_id=17354858, value=1)]),
            Model(hyperid=9009, hid=5, title='filterboolean', glparams=[GLParam(param_id=17354859, value=1)]),
        ]

        cls.index.gltypes += [
            GLType(param_id=17354858, hid=5, gltype=GLType.NUMERIC, hidden=False, through=True),
            GLType(param_id=17354859, hid=5, gltype=GLType.BOOL, hidden=False, through=True),
        ]

    def test_non_enum_through_filters(self):
        response = self.report.request_json(
            'place=prime&text=filternumeric&rearr-factors=market_through_gl_filters_on_search=1&enable-hard-filters=0'
        )
        self.assertFragmentIn(response, {"filters": [{"id": "17354858"}]})

        response = self.report.request_json(
            'place=prime&text=filterboolean&rearr-factors=market_through_gl_filters_on_search=1&enable-hard-filters=0'
        )
        self.assertFragmentIn(response, {"filters": [{"id": "17354859"}]})

    @classmethod
    def prepare_preserve_mbo_order_when_sort_type_offers_count(cls):
        cls.index.hypertree += [HyperCategory(hid=898702)]

        cls.index.gltypes += [
            GLType(
                param_id=80001,
                hid=898702,
                gltype=GLType.ENUM,
                cluster_filter=True,
                values=[
                    GLValue(value_id=100, text="90 x 210"),
                    GLValue(value_id=200, text="110 x 140"),
                    GLValue(value_id=300, text="140 x 205"),
                    GLValue(value_id=400, text="175 x 205"),
                    GLValue(value_id=500, text="200 x 250"),
                    GLValue(value_id=600, text="220 x 250"),
                    GLValue(value_id=700, text="280 x 280"),
                ],
                short_enum_sort_type=GLType.EnumFieldSortingType.OFFERS_COUNT,
                short_enum_count=3,
            )
        ]

        cls.index.offers += [
            Offer(hid=898702, title="Bedsheet", glparams=[GLParam(param_id=80001, value=param_value)])
            for param_value in [100, 200, 200, 300, 400, 400, 500, 500, 500, 600, 700]
        ]

    def test_preserve_mbo_order_when_sort_type_offers_count(self):
        response = self.report.request_json("place=prime&hid=898702")

        self.assertFragmentIn(
            response,
            {
                "id": "80001",
                "valuesCount": 7,
                "valuesGroups": [
                    {"type": "top", "valuesIds": ["200", "400", "500"]},
                    {"type": "all", "valuesIds": ["100", "200", "300", "400", "500", "600", "700"]},
                ],
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_gl_filters_reduction(cls):
        cls.index.gltypes += [
            GLType(
                param_id=801,
                hid=901,
                gltype=GLType.ENUM,
                cluster_filter=True,
                subtype='size',
            ),
            GLType(
                param_id=802,
                hid=901,
                gltype=GLType.ENUM,
                cluster_filter=True,
            ),
            GLType(
                param_id=803,
                hid=901,
                gltype=GLType.ENUM,
                cluster_filter=True,
                subtype='color',
            ),
            GLType(
                param_id=804,
                hid=901,
                gltype=GLType.ENUM,
                cluster_filter=True,
            ),
            GLType(
                param_id=805,
                hid=901,
                gltype=GLType.ENUM,
                cluster_filter=True,
            ),
            GLType(
                param_id=806,
                hid=901,
                gltype=GLType.ENUM,
                cluster_filter=True,
                subtype='color',
            ),
            GLType(
                param_id=807,
                hid=901,
                gltype=GLType.ENUM,
                cluster_filter=True,
            ),
            GLType(
                param_id=808,
                hid=901,
                gltype=GLType.ENUM,
                cluster_filter=True,
                subtype='size',
            ),
            GLType(
                param_id=809,
                hid=901,
                gltype=GLType.ENUM,
                cluster_filter=True,
            ),
            GLType(
                param_id=810,
                hid=901,
                gltype=GLType.ENUM,
                cluster_filter=True,
            ),
        ]

        cls.index.offers += [
            Offer(
                title='model offer',
                hid=901,
                hyperid=28,
                glparams=[
                    GLParam(param_id=801, value=1),
                    GLParam(param_id=802, value=1),
                    GLParam(param_id=803, value=1),
                    GLParam(param_id=804, value=1),
                    GLParam(param_id=805, value=1),
                    GLParam(param_id=806, value=1),
                    GLParam(param_id=807, value=1),
                    GLParam(param_id=808, value=1),
                    GLParam(param_id=809, value=1),
                    GLParam(param_id=810, value=1),
                ],
            )
        ]

    def test_gl_filters_reduction(self):
        response = self.report.request_json('place=prime&hid=901')

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "product",
                            "filters": [
                                {"id": "801"},
                                {"id": "802"},
                                {"id": "803"},
                                {"id": "804"},
                                {"id": "805"},
                                {"id": "806"},
                                {"id": "807"},
                                {"id": "808"},
                                {"id": "809"},
                                {"id": "810"},
                            ],
                        },
                        {
                            "entity": "offer",
                            "filters": [
                                {"id": "801"},
                                {"id": "802"},
                                {"id": "803"},
                                {"id": "804"},
                                {"id": "805"},
                                {"id": "806"},
                                {"id": "807"},
                                {"id": "808"},
                                {"id": "809"},
                                {"id": "810"},
                            ],
                        },
                    ]
                }
            },
            allow_different_len=False,
        )

        response = self.report.request_json('place=prime&hid=901&minimize-output=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "product",
                            "filters": [
                                {"id": "801"},
                                {"id": "802"},
                                {"id": "804"},
                                {"id": "805"},
                                {"id": "807"},
                                {"id": "808"},
                            ],
                        },
                        {
                            "entity": "offer",
                            "filters": [
                                {"id": "801"},
                                {"id": "802"},
                                {"id": "804"},
                                {"id": "805"},
                                {"id": "807"},
                                {"id": "808"},
                                {"id": "809"},
                            ],
                        },
                    ]
                }
            },
            allow_different_len=False,
        )

    def test_gl_filters_size_chart(self):
        """
        Проверяем, что получим default и фильтрующие значения значение из size_chart
        не смотря на то, что параметр сеток 600104 имеет другую дефолтную опцию и значения в параметре 600103 filter false
        """
        response = self.report.request_json('place=prime&hid=898302')

        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "defaultUnit": "RU",
                        "id": "600103",
                        "originalSubType": "size",
                        "position": 4,
                        "subType": "size",
                        "type": "enum",
                        "valuesCount": 3,
                        "units": [
                            {"id": "2", "unitId": "EU", "values": [{"unit": "EU", "value": "38"}]},
                            {
                                "id": "1",
                                "unitId": "RU",
                                "values": [{"unit": "RU", "value": "42"}, {"unit": "RU", "value": "46"}],
                            },
                        ],
                    }
                ],
            },
        )


if __name__ == '__main__':
    main()
