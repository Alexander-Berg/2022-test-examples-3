#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    DeliveryBucket,
    DeliveryOption,
    GLParam,
    GLType,
    MarketSku,
    Model,
    NavCategory,
    Offer,
    RegionalDelivery,
    Shop,
)
from core.types.navcategory import ModelList, FilterConfig, HidList
from core.matcher import Absent


class T(TestCase):

    shop_dsbs = Shop(
        fesh=42,
        datafeed_id=4240,
        priority_region=213,
        client_id=11,
        cpa=Shop.CPA_REAL,
    )

    shop_blue = Shop(
        fesh=43,
        datafeed_id=3232,
        priority_region=213,
        client_id=12,
        cpa=Shop.CPA_REAL,
        blue=Shop.BLUE_REAL,
    )

    @classmethod
    def prepare_modellists(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.navtree += [
            NavCategory(
                nid=1,
                hid=1,
                children=[
                    NavCategory(
                        nid=100,
                        hid=100,
                        children=[
                            NavCategory(
                                nid=200,
                                name='Список моделей - 200',
                                model_list=ModelList(list_id=1, models=[322, 3220, 100500]),
                                node_type=NavCategory.MODEL_LIST,
                            )
                        ],
                    ),
                    NavCategory(
                        nid=300,
                        hid=300,
                        children=[
                            NavCategory(
                                nid=400,
                                name='Список моделей - 400',
                                model_list=ModelList(list_id=2, models=[322, 3221]),
                                node_type=NavCategory.MODEL_LIST,
                            )
                        ],
                    ),
                ],
            )
        ]

        cls.index.offers += [
            Offer(hid=300, hyperid=322, title='offer-300-322-0'),
            Offer(hid=300, hyperid=322, title='offer-300-322-1'),
            Offer(hid=300, hyperid=3220, title='offer-300-3220'),
            Offer(hid=100, hyperid=3221, title='offer-100-3221'),
            Offer(hid=100, hyperid=555, title='offer-100-555'),
            Offer(
                hid=300,
                fesh=T.shop_dsbs.fesh,
                sku=100500,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[4242],
                title="offer-300-100500(msku)-cpa",
                waremd5='CpaMsku100500________g',
            ),
            Offer(hid=300, sku=100500, title="offer-300-100500(msku)-cpc", waremd5='CpcMsku100500________g'),
        ]

        cls.index.models += [
            Model(hyperid=322, hid=300),
            Model(hyperid=3220, hid=300),
            Model(hyperid=3221, hid=100),
            Model(hyperid=555, hid=100),
        ]

        cls.index.mskus += [
            MarketSku(
                title="msku 100500",
                sku=100500,
                hid=300,
                blue_offers=[
                    BlueOffer(
                        hid=300,
                        fesh=T.shop_blue.fesh,
                        feedid=T.shop_blue.datafeed_id,
                        offerid='ShopBlue_sku_',
                        price=400,
                        delivery_buckets=[3232],
                        waremd5='Blue101______________Q',
                    )
                ],
            )
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=4242,
                fesh=T.shop_dsbs.fesh,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=3)])],
            ),
            DeliveryBucket(
                bucket_id=3232,
                fesh=T.shop_blue.fesh,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=3)])],
            ),
        ]

    def test_model_list_search(self):
        """
        Ниды ноды-списков моделей обогащают оффера и модельки с model_id из своего списка
        Также таким офферам и моделям проставляются все ниды от корня до списка (как и для обычных категорийных нод)
        + в списке могут присутствовать мску
        """

        # Запрашиваем конкретно список моделей
        # Должны присутствовать документы с model_id in [322, 3220] или msku=100500
        response = self.report.request_json(
            'place=prime&nid=200&allow-collapsing=1&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 5,
                    "results": [
                        {
                            "entity": "product",
                            "id": 3220,
                            "offers": {
                                "count": 1,
                            },
                        },
                        {
                            "entity": "product",
                            "id": 322,
                            "offers": {
                                "count": 2,
                            },
                        },
                        {
                            "entity": "offer",
                            "wareId": "CpaMsku100500________g",
                        },
                        {
                            "entity": "offer",
                            "wareId": "CpcMsku100500________g",
                        },
                        {
                            "entity": "offer",
                            "wareId": "Blue101______________Q",
                        },
                    ],
                }
            },
            allow_different_len=False,
        )

        # Должны присутствовать документы с model_id in [322, 3221]
        response = self.report.request_json(
            'place=prime&nid=400&allow-collapsing=1&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {
                            "entity": "product",
                            "id": 3221,
                            "offers": {
                                "count": 1,
                            },
                        },
                        {
                            "entity": "product",
                            "id": 322,
                            "offers": {
                                "count": 2,
                            },
                        },
                    ],
                }
            },
            allow_different_len=False,
        )

        # Запрашиваем родителя списка nid=200
        # Должны присутствовать документы с хидом 100
        # И с model_id in [322, 3220] и с мску=100500
        response = self.report.request_json(
            'place=prime&nid=100&allow-collapsing=1&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 7,
                    "results": [
                        {
                            "entity": "product",
                            "id": 3220,
                            "offers": {
                                "count": 1,
                            },
                        },
                        {
                            "entity": "product",
                            "id": 322,
                            "offers": {
                                "count": 2,
                            },
                        },
                        {
                            "entity": "product",
                            "id": 3221,
                            "offers": {
                                "count": 1,
                            },
                        },
                        {
                            "entity": "product",
                            "id": 555,
                            "offers": {
                                "count": 1,
                            },
                        },
                        {
                            "entity": "offer",
                            "wareId": "CpaMsku100500________g",
                        },
                        {
                            "entity": "offer",
                            "wareId": "CpcMsku100500________g",
                        },
                        {
                            "entity": "offer",
                            "wareId": "Blue101______________Q",
                        },
                    ],
                }
            },
            allow_different_len=False,
        )

        # Запрашиваем родителя списка nid=400
        # Должны присутствовать документы с хидом 300
        # И с model_id in [322, 3221]
        response = self.report.request_json(
            'place=prime&nid=300&allow-collapsing=1&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 6,
                    "results": [
                        {
                            "entity": "product",
                            "id": 3220,
                            "offers": {
                                "count": 1,
                            },
                        },
                        {
                            "entity": "product",
                            "id": 322,
                            "offers": {
                                "count": 2,
                            },
                        },
                        {
                            "entity": "product",
                            "id": 3221,
                            "offers": {
                                "count": 1,
                            },
                        },
                        {
                            "entity": "offer",
                            "wareId": "CpaMsku100500________g",
                        },
                        {
                            "entity": "offer",
                            "wareId": "CpcMsku100500________g",
                        },
                        {
                            "entity": "offer",
                            "wareId": "Blue101______________Q",
                        },
                    ],
                }
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_modellist_filters_config(cls):
        cls.index.navtree += [
            NavCategory(
                nid=111,
                filter_config=FilterConfig(conf_id=11, filters=[750, 850, 950], advanced_filters=[750]),
                children=[
                    NavCategory(
                        nid=222,
                        name='Список моделей - 222',
                        model_list=ModelList(list_id=3, models=[450, 451]),
                        hid_list=HidList(list_id=1, hids=[202, 203]),
                        node_type=NavCategory.MODEL_LIST,
                    ),
                    NavCategory(
                        nid=333,
                        name='Список моделей - 333',
                        model_list=ModelList(list_id=4, models=[450, 452]),
                        hid_list=HidList(list_id=2, hids=[202, 204]),
                        filter_config=FilterConfig(conf_id=22, filters=[750, 850], advanced_filters=[850]),
                        node_type=NavCategory.MODEL_LIST,
                    ),
                ],
                node_type=NavCategory.VIRTUAL,
            )
        ]

        cls.index.gltypes += [
            GLType(param_id=750, hid=202, gltype=GLType.ENUM, values=[1, 2, 3]),
            GLType(param_id=750, hid=203, gltype=GLType.ENUM, values=[1, 2]),
            GLType(param_id=750, hid=204, gltype=GLType.ENUM, values=[4]),
            GLType(param_id=850, hid=202, gltype=GLType.BOOL),
            GLType(param_id=850, hid=203, gltype=GLType.BOOL),
            GLType(param_id=850, hid=204, gltype=GLType.BOOL),
            GLType(param_id=950, hid=202, gltype=GLType.NUMERIC),
            GLType(param_id=950, hid=203, gltype=GLType.NUMERIC),
            GLType(param_id=950, hid=204, gltype=GLType.NUMERIC),
        ]

        cls.index.offers += [
            Offer(
                hid=202,
                hyperid=450,
                title='offer-202-450-0',
                glparams=[GLParam(param_id=750, value=1), GLParam(param_id=850, value=1)],
            ),
            Offer(
                hid=202,
                hyperid=450,
                title='offer-202-450-1',
                glparams=[
                    GLParam(param_id=750, value=3),
                    GLParam(param_id=850, value=0),
                    GLParam(param_id=950, value=100),
                ],
            ),
            Offer(
                hid=203,
                hyperid=451,
                title='offer-203-451-0',
                glparams=[GLParam(param_id=750, value=1), GLParam(param_id=850, value=1)],
            ),
            Offer(
                hid=203,
                hyperid=451,
                title='offer-203-451-1',
                glparams=[
                    GLParam(param_id=750, value=2),
                    GLParam(param_id=850, value=0),
                    GLParam(param_id=950, value=200),
                ],
            ),
            Offer(hid=204, hyperid=452, title='offer-204-452-0', glparams=[GLParam(param_id=750, value=4)]),
            Offer(
                hid=204,
                hyperid=452,
                title='offer-204-452-1',
                glparams=[GLParam(param_id=850, value=1), GLParam(param_id=950, value=300)],
            ),
        ]

        cls.index.models += [
            Model(hyperid=450, hid=202),
            Model(hyperid=451, hid=203),
            Model(hyperid=452, hid=204),
        ]

    def test_model_list_filter_config(self):
        """
        Проверяем настройку фильтров у списков моделей
        Для применения этих фильтров нужны категории, для списков моделей они передаются в нав дереве в блоке models-hids-list

        Должно работать под флагом market_make_glfilters_from_configs
        """

        # Запрос без зажатых фильтров
        # Должны быть фильтры из конфига
        response = self.report.request_json(
            'place=prime&nid=333&allow-collapsing=1&rearr-factors=market_make_glfilters_from_configs=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "750",
                        "type": "enum",
                        "valuesCount": 3,
                        "values": [
                            {"found": 1, "initialFound": 1, "id": "4"},
                            {"found": 1, "initialFound": 1, "id": "1"},
                            {"found": 1, "initialFound": 1, "id": "3"},
                        ],
                    },
                    {
                        "id": "850",
                        "type": "boolean",
                        "values": [
                            {"found": 2, "initialFound": 2, "id": "1"},
                            {"found": 4, "initialFound": 4, "id": "0"},
                        ],
                    },
                ],
                "search": {
                    "total": 2,
                    "results": [
                        {
                            "entity": "product",
                            "id": 452,
                            "offers": {
                                "count": 2,
                            },
                        },
                        {
                            "entity": "product",
                            "id": 450,
                            "offers": {
                                "count": 2,
                            },
                        },
                    ],
                },
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "id": "950",
                        "name": "kot-param-102",
                    }
                ]
            },
        )

        # Зажимаем enum фильтр &glfilter=750:1
        # Под условие попадает только оффер offer-202-450-0 + в фильтре 850 должно остаться только значение True
        response = self.report.request_json(
            'place=prime&nid=333&allow-collapsing=1&glfilter=750:1&rearr-factors=market_make_glfilters_from_configs=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "750",
                        "type": "enum",
                        "valuesCount": 3,
                        "values": [
                            {"found": 1, "initialFound": 1, "id": "4"},
                            {"found": 2, "initialFound": 2, "id": "1"},
                            {"found": 1, "initialFound": 1, "id": "3"},
                        ],
                    },
                    {
                        "id": "850",
                        "type": "boolean",
                        "values": [
                            {"found": 2, "initialFound": 3, "id": "1"},
                            {"found": 0, "initialFound": 4, "id": "0"},
                        ],
                    },
                ],
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "entity": "product",
                            "id": 450,
                            "offers": {
                                "count": 1,
                            },
                        },
                    ],
                },
            },
        )

        # Зажимаем enum фильтр &glfilter=850:0
        # Под условие попадает только оффер offer-202-450-1 и offer-204-452-0
        response = self.report.request_json(
            'place=prime&nid=333&allow-collapsing=1&glfilter=850:0&rearr-factors=market_make_glfilters_from_configs=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "750",
                        "type": "enum",
                        "valuesCount": 3,
                        "values": [
                            {"found": 2, "initialFound": 2, "id": "4"},
                            {"found": 0, "initialFound": 1, "id": "1"},
                            {"found": 2, "initialFound": 2, "id": "3"},
                        ],
                    },
                    {
                        "id": "850",
                        "type": "boolean",
                        "values": [
                            {"found": 2, "initialFound": 2, "id": "1"},
                            {"found": 8, "initialFound": 8, "id": "0"},
                        ],
                    },
                ],
                "search": {
                    "total": 2,
                    "results": [
                        {
                            "entity": "product",
                            "id": 450,
                            "offers": {
                                "count": 1,
                            },
                        },
                        {
                            "entity": "product",
                            "id": 452,
                            "offers": {
                                "count": 1,
                            },
                        },
                    ],
                },
            },
        )

    def test_virtual_nid_filter_config(self):
        """
        Теперь проверим для виртуальной ноды nid=111 у которой все дети - списки моделей
        """

        response = self.report.request_json(
            'place=prime&nid=111&allow-collapsing=1&rearr-factors=market_make_glfilters_from_configs=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "750",
                        "type": "enum",
                        "valuesCount": 4,
                        "values": [
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
                                "id": "4",
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
                        "values": [
                            {"found": 3, "initialFound": 3, "id": "1", "value": "1"},
                            {"found": 6, "initialFound": 6, "id": "0", "value": "0"},
                        ],
                    },
                    {
                        "id": "950",
                        "type": "number",
                        "values": [
                            {"id": "found", "initialMax": "300", "initialMin": "100", "max": "300", "min": "100"}
                        ],
                    },
                ],
                "search": {
                    "total": 3,
                    "results": [
                        {
                            "entity": "product",
                            "id": 450,
                            "offers": {
                                "count": 2,
                            },
                        },
                        {
                            "entity": "product",
                            "id": 451,
                            "offers": {
                                "count": 2,
                            },
                        },
                        {
                            "entity": "product",
                            "id": 452,
                            "offers": {
                                "count": 2,
                            },
                        },
                    ],
                },
            },
        )

        # Зажимаем &glfilter=750:1
        # На выдаче должно статься два оффера + в фильтре 850 должно остаться только значение True
        # А нумерики вообще не найтись, тк у этих доков он не задан
        response = self.report.request_json(
            'place=prime&nid=111&allow-collapsing=1&glfilter=750:1&rearr-factors=market_make_glfilters_from_configs=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "750",
                        "type": "enum",
                        "valuesCount": 4,
                        "values": [
                            {
                                "found": 4,
                                "initialFound": 4,
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
                                "id": "4",
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
                        "values": [
                            {"found": 4, "initialFound": 5, "id": "1", "value": "1"},
                            {"found": 0, "initialFound": 6, "id": "0", "value": "0"},
                        ],
                    },
                    {
                        "id": "950",
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
                "search": {
                    "total": 2,
                    "results": [
                        {
                            "entity": "product",
                            "id": 450,
                            "offers": {
                                "count": 1,
                            },
                        },
                        {
                            "entity": "product",
                            "id": 451,
                            "offers": {
                                "count": 1,
                            },
                        },
                    ],
                },
            },
        )

        # Зажимаем булевый фильтр &glfilter=850:0
        # На выдаче должно остаться только 3 оффера
        # И уйдут доки с glfilter=750:1
        # Нумерики станут от 100 до 200
        response = self.report.request_json(
            'place=prime&nid=111&allow-collapsing=1&glfilter=850:0&rearr-factors=market_make_glfilters_from_configs=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "750",
                        "type": "enum",
                        "valuesCount": 4,
                        "values": [
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
                                "id": "4",
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
                        "values": [
                            {"found": 3, "initialFound": 3, "id": "1", "value": "1"},
                            {"found": 12, "initialFound": 12, "id": "0", "value": "0"},
                        ],
                    },
                    {
                        "id": "950",
                        "type": "number",
                        "values": [
                            {"id": "found", "initialMax": "300", "initialMin": "100", "max": "200", "min": "100"}
                        ],
                    },
                ],
                "search": {
                    "total": 3,
                    "results": [
                        {
                            "entity": "product",
                            "id": 450,
                            "offers": {
                                "count": 1,
                            },
                        },
                        {
                            "entity": "product",
                            "id": 451,
                            "offers": {
                                "count": 1,
                            },
                        },
                        {
                            "entity": "product",
                            "id": 452,
                            "offers": {
                                "count": 1,
                            },
                        },
                    ],
                },
            },
        )

        # Зажимаем фильтр &glfilter=850:0 и &glfilter=750:4
        # Таких доков нет
        response = self.report.request_json(
            'place=prime&nid=111&allow-collapsing=1&glfilter=850:1&glfilter=750:4&rearr-factors=market_make_glfilters_from_configs=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "750",
                        "type": "enum",
                        "valuesCount": 4,
                        "values": [
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
                                "id": "4",
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
                        "values": [
                            {"found": 0, "initialFound": 3, "id": "1", "value": "1"},
                            {"found": 6, "initialFound": 6, "id": "0", "value": "0"},
                        ],
                    },
                    {
                        "id": "950",
                        "type": "number",
                        "values": [
                            {"id": "found", "initialMax": "300", "initialMin": "100", "max": Absent(), "min": Absent()}
                        ],
                    },
                ],
                "search": {"total": 0},
            },
        )

        # Зажимаем нумерик фильтр glfilter=950:~150
        # Подходящий документ всего один
        response = self.report.request_json(
            'place=prime&nid=111&allow-collapsing=1&glfilter=950:~150&rearr-factors=market_make_glfilters_from_configs=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "750",
                        "type": "enum",
                        "valuesCount": 4,
                        "values": [
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
                                "found": 0,
                                "initialFound": 1,
                                "id": "4",
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
                        "values": [
                            {"found": 0, "initialFound": 3, "id": "1", "value": "1"},
                            {"found": 2, "initialFound": 7, "id": "0", "value": "0"},
                        ],
                    },
                    {
                        "id": "950",
                        "type": "number",
                        "values": [
                            {"id": "found", "initialMax": "300", "initialMin": "100", "max": "300", "min": "100"}
                        ],
                    },
                ],
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "entity": "product",
                            "id": 450,
                            "offers": {
                                "count": 1,
                            },
                        }
                    ],
                },
            },
        )

    def test_modellist_intents(self):
        """
        Проверяем, что списки моделей отображаются в нидовых интентах
        """
        # Запрашиваем список
        # В интентах будет путь до него: nid=111, nid=333
        response = self.report.request_json(
            'place=prime&nid=333&allow-collapsing=1&rearr-factors=market_make_glfilters_from_configs=1;market_return_nids_in_intents=1'
        )
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {
                            "hid": 0,
                            "isLeaf": False,
                            "nid": 111,
                        },
                        "intents": [
                            {
                                "category": {
                                    "hid": 0,
                                    "isLeaf": True,
                                    "name": "Список моделей - 333",
                                    "nid": 333,
                                }
                            }
                        ],
                    }
                ]
            },
        )

        # Запрашиваем виртуальный узел nid=111
        # У него два дочерних узла - списки моделей
        response = self.report.request_json(
            'place=prime&nid=111&allow-collapsing=1&rearr-factors=market_make_glfilters_from_configs=1;market_return_nids_in_intents=1'
        )
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {
                            "hid": 0,
                            "isLeaf": False,
                            "nid": 111,
                        },
                        "intents": [
                            {
                                "category": {
                                    "hid": 0,
                                    "isLeaf": True,
                                    "name": "Список моделей - 222",
                                    "nid": 222,
                                }
                            },
                            {
                                "category": {
                                    "hid": 0,
                                    "isLeaf": True,
                                    "name": "Список моделей - 333",
                                    "nid": 333,
                                }
                            },
                        ],
                    }
                ]
            },
        )

        # Запрашиваем nid=1
        # В навигации должны быть списки
        response = self.report.request_json(
            'place=prime&nid=1&allow-collapsing=1&rearr-factors=market_make_glfilters_from_configs=1;market_return_nids_in_intents=1'
        )
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {
                            "hid": 1,
                            "nid": 1,
                        },
                        "intents": [
                            {
                                "category": {
                                    "hid": 100,
                                    "nid": 100,
                                },
                                "intents": [
                                    {
                                        "category": {
                                            "hid": 0,
                                            "nid": 200,
                                            "name": "Список моделей - 200",
                                        }
                                    }
                                ],
                            },
                            {
                                "category": {
                                    "hid": 300,
                                    "nid": 300,
                                },
                                "intents": [
                                    {
                                        "category": {
                                            "hid": 0,
                                            "nid": 400,
                                            "name": "Список моделей - 400",
                                        }
                                    }
                                ],
                            },
                        ],
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()
