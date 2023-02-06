#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent, ElementCount
from core.types import (
    BlueOffer,
    Book,
    CategoryCompareParams,
    DeliveryBucket,
    DeliveryOption,
    GLParam,
    GLType,
    GLValue,
    HyperCategory,
    MarketSku,
    Model,
    ModelDescriptionTemplates,
    ModelGroup,
    Offer,
    RegionalDelivery,
    Shop,
    VCluster,
    VClusterTransition,
)
from core.testcase import TestCase, main
from core.types.autogen import Const

SILVER = 10
ROSE_GOLD = 11


class T(TestCase):
    @classmethod
    def prepare_non_group_category(cls):
        cls.index.hypertree += [
            # Due to the way LITE works now, a category should be visual if it has at least one cluster
            # (even though it has models, too).
            HyperCategory(hid=100, visual=True)
        ]

        cls.index.gltypes += [
            GLType(param_id=201, hid=100, position=1, gltype=GLType.BOOL),
            GLType(
                param_id=202,
                hid=100,
                position=2,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=300, text='DVD-RW'),
                    GLValue(value_id=301, text='DVD'),
                    GLValue(value_id=302, text='CD'),
                ],
            ),
            GLType(param_id=203, hid=100, position=3, gltype=GLType.NUMERIC),
            GLType(
                param_id=204,
                hid=100,
                position=4,
                subtype='color',
                gltype=GLType.ENUM,
                cluster_filter=1,
                values=[
                    GLValue(400, code='#FF0000', tag='red', text='crimson'),
                    GLValue(401, code='#85D6FF', tag='blue', text='cyan'),
                    GLValue(402, code='#FF00BB', tag='pink', text='magenta'),
                ],
            ),
            GLType(
                param_id=205,
                hid=100,
                position=5,
                subtype='size',
                gltype=GLType.ENUM,
                cluster_filter=1,
                unit_param_id=210,
                values=[
                    GLValue(value_id=1, text='32', unit_value_id=1),
                    GLValue(value_id=2, text='36', unit_value_id=1),
                    GLValue(value_id=3, text='S', unit_value_id=2),
                    GLValue(value_id=4, text='M', unit_value_id=2),
                ],
            ),
            GLType(
                param_id=210,
                hid=100,
                gltype=GLType.ENUM,
                name='size_units',
                position=None,
                values=[GLValue(value_id=1, text='Digits', default=True), GLValue(value_id=2, text='Letters')],
            ),
            # Never set in the models we use for comparison.
            GLType(param_id=206, hid=100, gltype=GLType.BOOL),
            # GlTypes for books
            GLType(param_id=633, hid=90829, position=1, xslname='Rating', gltype=GLType.NUMERIC),
            GLType(
                param_id=634,
                hid=90829,
                position=2,
                xslname='Theme',
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text='Животные'),
                    GLValue(value_id=2, text='IT'),
                ],
            ),
            GLType(
                param_id=635,
                hid=90829,
                position=3,
                xslname='Description',
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text='Узнай все про кошечек'),
                    GLValue(value_id=2, text='Узнай все про песиков'),
                ],
            ),
        ]

        cls.index.models += [
            # This model is never used in comparisons, therefore the 206 parameter should never be in the results.
            Model(hyperid=100500, hid=100, glparams=[GLParam(param_id=206, value=1)]),
            # We know nothing about this model.
            Model(hyperid=500, hid=100, glparams=[]),
            # This one has at most one value per param.
            Model(
                hyperid=501,
                hid=100,
                glparams=[
                    GLParam(param_id=201, value=0),
                    GLParam(param_id=202, value=300),
                    GLParam(param_id=203, value=42),
                ],
            ),
            # A supermodel having every possible value.
            Model(
                hyperid=502,
                hid=100,
                glparams=[
                    GLParam(param_id=201, value=1),
                    GLParam(param_id=202, value=300),
                    GLParam(param_id=202, value=301),
                    GLParam(param_id=202, value=302),
                    GLParam(param_id=203, value=42),
                ],
            ),
            # A full duplicate of model 501.
            Model(
                hyperid=503,
                hid=100,
                glparams=[
                    GLParam(param_id=201, value=0),
                    GLParam(param_id=202, value=300),
                    GLParam(param_id=203, value=42),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=500,
                sku=123,
                glparams=[
                    GLParam(param_id=201, value=0),
                    GLParam(param_id=202, value=300),
                    GLParam(param_id=203, value=42),
                ],
            ),
            MarketSku(
                hyperid=500,
                sku=124,
                glparams=[
                    GLParam(param_id=201, value=1),
                    GLParam(param_id=202, value=300),
                    GLParam(param_id=202, value=301),
                    GLParam(param_id=202, value=302),
                    GLParam(param_id=203, value=42),
                ],
                blue_offers=[BlueOffer(title="offer-124")],
            ),
        ]

        cls.index.books += [
            Book(
                title='Книжка про кошек',
                hyperid=100521,
                hid=90829,
                glparams=[
                    GLParam(param_id=633, value=5),
                    GLParam(param_id=634, value=1),
                    GLParam(param_id=635, value=1),
                ],
            ),
            Book(
                title='Книжка про собак',
                hyperid=100522,
                hid=90829,
                glparams=[
                    GLParam(param_id=633, value=4),
                    GLParam(param_id=634, value=1),
                    GLParam(param_id=635, value=2),
                ],
            ),
            Book(
                title='Книжка про собак от другого автора',
                hyperid=100523,
                hid=90829,
                glparams=[
                    GLParam(param_id=633, value=4),
                    GLParam(param_id=634, value=1),
                    GLParam(param_id=635, value=2),
                ],
            ),
        ]

        cls.index.vclusters += [
            VCluster(
                vclusterid=1000000001,
                hid=100,
                glparams=[
                    GLParam(param_id=201, value=0),
                ],
            ),
            VCluster(
                vclusterid=1000000002,
                hid=100,
                glparams=[
                    GLParam(param_id=201, value=1),
                ],
            ),
        ]

        cls.index.vcluster_transitions += [
            VClusterTransition(src_id=1000000000, strong_id=1000000001),
            VClusterTransition(src_id=1000000004, strong_id=1000000005),
        ]

        # To attach second kind parameters to models.
        cls.index.offers += [
            Offer(
                hyperid=501,
                glparams=[
                    GLParam(param_id=204, value=400),
                    GLParam(param_id=205, value=1),
                ],
            ),
            Offer(
                hyperid=502,
                glparams=[
                    GLParam(param_id=204, value=400),
                ],
            ),
            Offer(
                hyperid=502,
                glparams=[
                    GLParam(param_id=204, value=401),
                    GLParam(param_id=205, value=1),
                ],
            ),
            Offer(
                hyperid=502,
                glparams=[
                    GLParam(param_id=204, value=402),
                    GLParam(param_id=205, value=2),
                ],
            ),
            Offer(
                hyperid=503,
                glparams=[
                    GLParam(param_id=204, value=400),
                    GLParam(param_id=205, value=1),
                ],
            ),
        ]

    @classmethod
    def prepare_group_category(cls):
        cls.index.gltypes += [
            # Group params.
            GLType(
                param_id=701,
                hid=101,
                position=1,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=801, text='macOS'),
                    GLValue(value_id=802, text='Windows'),
                ],
            ),
            GLType(param_id=702, hid=101, position=2, gltype=GLType.NUMERIC),
            # Modification params.
            GLType(
                param_id=703,
                hid=101,
                position=3,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=803, text='Core i3'),
                    GLValue(value_id=804, text='Core i5'),
                    GLValue(value_id=805, text='Core i7'),
                ],
            ),
            GLType(param_id=704, hid=101, position=4, gltype=GLType.NUMERIC),
        ]

        cls.index.model_groups += [
            # A group with no modifications.
            ModelGroup(
                hyperid=900,
                hid=101,
                glparams=[
                    GLParam(param_id=701, value=801),
                    GLParam(param_id=702, value=42),
                ],
            ),
            # A group with modifications.
            ModelGroup(
                hyperid=901,
                hid=101,
                glparams=[
                    GLParam(param_id=701, value=801),
                    GLParam(param_id=701, value=802),
                    GLParam(param_id=702, value=43),
                ],
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=902,
                group_hyperid=901,
                hid=101,
                glparams=[
                    # Params inherited from the group.
                    GLParam(param_id=701, value=802),
                    GLParam(param_id=702, value=43),
                    # Modification params.
                    GLParam(param_id=703, value=803),
                ],
            ),
            Model(
                hyperid=903,
                group_hyperid=901,
                hid=101,
                glparams=[
                    # Params inherited from the group.
                    GLParam(param_id=701, value=802),
                    GLParam(param_id=702, value=43),
                    # Modification params.
                    GLParam(param_id=703, value=804),
                    GLParam(param_id=704, value=10),
                ],
            ),
            Model(
                hyperid=904,
                group_hyperid=901,
                hid=101,
                glparams=[
                    # Params inherited from the group.
                    GLParam(param_id=701, value=802),
                    GLParam(param_id=702, value=43),
                    # Modification params.
                    GLParam(param_id=704, value=20),
                ],
            ),
        ]

    @classmethod
    def prepare_category_with_model_card(cls):
        cls.index.gltypes += [
            GLType(param_id=1001, hid=102, position=1, xslname='Group1Param1', gltype=GLType.BOOL),
            GLType(param_id=1002, hid=102, position=2, xslname='NoGroupParam3', gltype=GLType.NUMERIC, subtype='color'),
            GLType(param_id=1003, hid=102, position=3, xslname='Group2Param3', gltype=GLType.ENUM, values=[1100, 1101]),
            GLType(param_id=1004, hid=102, position=4, xslname='NoGroupParam1', gltype=GLType.BOOL, subtype='size'),
            GLType(param_id=1005, hid=102, position=5, xslname='Group1Param3', gltype=GLType.NUMERIC),
            GLType(param_id=1006, hid=102, position=6, xslname='Group1Param2', gltype=GLType.ENUM, values=[1200, 1201]),
            GLType(param_id=1007, hid=102, position=7, xslname='Group2Param1', gltype=GLType.BOOL),
            GLType(param_id=1008, hid=102, position=8, xslname='NoGroupParam2', gltype=GLType.NUMERIC),
            GLType(param_id=1009, hid=102, position=9, xslname='Group2Param2', gltype=GLType.ENUM, values=[1300, 1301]),
        ]

        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=102,
                friendlymodel=[],
                model=[
                    (
                        "GROUP 1",
                        {
                            "G1P1": "{Group1Param1:Group1Param1}{if (($Group1Param1)&&($Group1Param2)) return '/'; else return '';#exec}{Group1Param2:Group1Param2}",
                            "G1P2": "{Group1Param3#ifnz}/Group1Param3{#endif}",
                        },
                    ),
                    (
                        "GROUP 2",
                        {
                            "G2P1": "{*comma-on}{Group2Param1:test}{Group2Param2:test2}{*comma-off}",
                            "G2P3": "{Group2Param3}",
                        },
                    ),
                ],
            )
        ]

        cls.index.models += [
            Model(
                hyperid=1500,
                hid=102,
                glparams=[
                    GLParam(param_id=1001, value=0),
                    GLParam(param_id=1002, value=42),
                    GLParam(param_id=1003, value=1100),
                    GLParam(param_id=1004, value=0),
                    GLParam(param_id=1005, value=42),
                    GLParam(param_id=1006, value=1200),
                    GLParam(param_id=1007, value=0),
                    GLParam(param_id=1008, value=42),
                    GLParam(param_id=1009, value=1300),
                ],
            ),
            Model(
                hyperid=1501,
                hid=102,
                glparams=[
                    GLParam(param_id=1001, value=1),
                    GLParam(param_id=1002, value=43),
                    GLParam(param_id=1003, value=1101),
                    GLParam(param_id=1004, value=1),
                    GLParam(param_id=1005, value=43),
                    GLParam(param_id=1006, value=1201),
                    GLParam(param_id=1007, value=1),
                    GLParam(param_id=1008, value=43),
                    GLParam(param_id=1009, value=1301),
                ],
            ),
            Model(
                hyperid=1502,
                hid=102,
                glparams=[
                    GLParam(param_id=1001, value=1),
                    GLParam(param_id=1005, value=50),
                ],
            ),
            Model(
                hyperid=1503,
                hid=102,
                glparams=[
                    GLParam(param_id=1005, value=50),
                    GLParam(param_id=1006, value=1200),
                ],
            ),
            Model(
                hyperid=1504,
                hid=102,
                glparams=[
                    GLParam(param_id=1001, value=1),
                ],
            ),
            Model(
                hyperid=1505,
                hid=102,
                glparams=[
                    GLParam(param_id=1005, value=51),
                ],
            ),
            Model(
                hyperid=1506,
                hid=102,
                glparams=[
                    GLParam(param_id=1001, value=1),
                    GLParam(param_id=1005, value=42),
                    GLParam(param_id=1006, value=1200),
                    GLParam(param_id=1003, value=1100),
                    GLParam(param_id=1007, value=123),
                    GLParam(param_id=1009, value=1300),
                ],
            ),
            Model(
                hyperid=1507,
                hid=102,
                glparams=[
                    GLParam(param_id=1001, value=1),
                    GLParam(param_id=1005, value=42),
                    GLParam(param_id=1006, value=1200),
                    GLParam(param_id=1003, value=1101),
                    GLParam(param_id=1007, value=123),
                    GLParam(param_id=1009, value=1300),
                ],
            ),
        ]

    def test_books(self):
        '''Проверяем, что плейс работает с книжками'''

        def check_book_param(first_id, second_id, param_id, first_value, second_value):
            is_enum = True
            first_res_values = {"value": first_value}
            second_res_values = {"value": second_value}

            if type(first_value) == int:
                is_enum = False
                first_res_values = {"max": str(first_value), "min": str(first_value)}
                second_res_values = {"max": str(second_value), "min": str(second_value)}

            return {
                "areValuesEqual": first_value == second_value,
                "comparedItems": [
                    {
                        "entity": "product",
                        "id": first_id,
                        "values": [
                            first_res_values,
                        ],
                    },
                    {
                        "entity": "product",
                        "id": second_id,
                        "values": [
                            second_res_values,
                        ],
                    },
                ],
                "entity": "parameter",
                "id": param_id,
                "type": "enum" if is_enum else "number",
            }

        response = self.report.request_json('place=compare_products&hid=90829&hyperid=100521,100522')
        self.assertFragmentIn(
            response,
            {
                "comparedParameters": {
                    "comparedIds": [{"entity": "product", "id": "100521"}, {"entity": "product", "id": "100522"}],
                    "groups": [
                        {
                            "areValuesEqual": False,
                            "params": [
                                check_book_param(
                                    first_id='100521',
                                    second_id='100522',
                                    param_id='633',
                                    first_value=5,
                                    second_value=4,
                                ),
                                check_book_param(
                                    first_id='100521',
                                    second_id='100522',
                                    param_id='634',
                                    first_value='Животные',
                                    second_value='Животные',
                                ),
                                check_book_param(
                                    first_id='100521',
                                    second_id='100522',
                                    param_id='635',
                                    first_value='Узнай все про кошечек',
                                    second_value='Узнай все про песиков',
                                ),
                            ],
                        }
                    ],
                }
            },
            allow_different_len=False,
        )

        # С одинаковыми книжками тоже должно быть ок
        response = self.report.request_json('place=compare_products&hid=90829&hyperid=100522,100523')
        self.assertFragmentIn(
            response,
            {
                "comparedParameters": {
                    "comparedIds": [{"entity": "product", "id": "100522"}, {"entity": "product", "id": "100523"}],
                    "groups": [
                        {
                            "areValuesEqual": True,
                        }
                    ],
                }
            },
            allow_different_len=False,
        )

    def test_compare_product_and_sku(self):
        response = self.report.request_json('place=compare_products&hid=100&hyperid=500,501,502&market-sku=123')
        self.assertFragmentIn(
            response,
            {
                "comparedParameters": {
                    "comparedIds": [
                        {"entity": "product", "id": "500"},
                        {"entity": "product", "id": "501"},
                        {"entity": "product", "id": "502"},
                        {"entity": "sku", "id": "123"},
                    ],
                    "groups": [
                        {
                            "areValuesEqual": False,
                            "params": [
                                {
                                    "areValuesEqual": False,
                                    "isAggregated": False,
                                    "type": "boolean",
                                    "subType": "",
                                    "id": "201",
                                    "comparedItems": [
                                        {"values": [], "entity": "product", "id": "500"},
                                        {"values": [{"value": "0"}], "entity": "product", "id": "501"},
                                        {"values": [{"value": "1"}], "entity": "product", "id": "502"},
                                        {"values": [{"value": "0"}], "entity": "sku", "id": "123"},
                                    ],
                                    "entity": "parameter",
                                },
                                {
                                    "areValuesEqual": False,
                                    "isAggregated": False,
                                    "type": "enum",
                                    "subType": "",
                                    "id": "202",
                                    "comparedItems": [
                                        {"values": [], "entity": "product", "id": "500"},
                                        {
                                            "values": [{"value": "DVD-RW", "id": "300"}],
                                            "entity": "product",
                                            "id": "501",
                                        },
                                        {
                                            "values": [
                                                {"value": "CD", "id": "302"},
                                                {"value": "DVD", "id": "301"},
                                                {"value": "DVD-RW", "id": "300"},
                                            ],
                                            "entity": "product",
                                            "id": "502",
                                        },
                                        {"values": [{"value": "DVD-RW", "id": "300"}], "entity": "sku", "id": "123"},
                                    ],
                                    "entity": "parameter",
                                },
                                {
                                    "areValuesEqual": False,
                                    "isAggregated": False,
                                    "type": "number",
                                    "subType": "",
                                    "id": "203",
                                    "comparedItems": [
                                        {"values": [], "entity": "product", "id": "500"},
                                        {"values": [{"max": "42", "min": "42"}], "entity": "product", "id": "501"},
                                        {"values": [{"max": "42", "min": "42"}], "entity": "product", "id": "502"},
                                        {"values": [{"max": "42", "min": "42"}], "entity": "sku", "id": "123"},
                                    ],
                                    "entity": "parameter",
                                },
                                {
                                    "areValuesEqual": False,
                                    "isAggregated": False,
                                    "type": "enum",
                                    "subType": "color",
                                    "id": "204",
                                    "comparedItems": [
                                        {"values": [], "entity": "product", "id": "500"},
                                        {
                                            "values": [
                                                {"group": "red", "value": "crimson", "code": "#FF0000", "id": "400"}
                                            ],
                                            "entity": "product",
                                            "id": "501",
                                        },
                                        {
                                            "values": [
                                                {"group": "red", "value": "crimson", "code": "#FF0000", "id": "400"},
                                                {"group": "blue", "value": "cyan", "code": "#85D6FF", "id": "401"},
                                                {"group": "pink", "value": "magenta", "code": "#FF00BB", "id": "402"},
                                            ],
                                            "entity": "product",
                                            "id": "502",
                                        },
                                        {"values": [], "entity": "sku", "id": "123"},
                                    ],
                                    "entity": "parameter",
                                },
                                {
                                    "areValuesEqual": False,
                                    "isAggregated": False,
                                    "type": "enum",
                                    "defaultUnit": "Digits",
                                    "subType": "size",
                                    "id": "205",
                                    "comparedItems": [
                                        {"values": [], "entity": "product", "id": "500"},
                                        {"values": [{"value": "32", "id": "1"}], "entity": "product", "id": "501"},
                                        {
                                            "values": [{"value": "32", "id": "1"}, {"value": "36", "id": "2"}],
                                            "entity": "product",
                                            "id": "502",
                                        },
                                        {"values": [], "entity": "sku", "id": "123"},
                                    ],
                                    "entity": "parameter",
                                },
                            ],
                        }
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json('place=compare_products&hid=100&market-sku=123')
        self.assertFragmentIn(
            response,
            {
                "comparedParameters": {
                    "comparedIds": [{"entity": "sku", "id": "123"}],
                    "groups": [
                        {
                            "areValuesEqual": True,
                            "params": [
                                {
                                    "areValuesEqual": True,
                                    "isAggregated": False,
                                    "type": "boolean",
                                    "subType": "",
                                    "id": "201",
                                    "comparedItems": [{"values": [{"value": "0"}], "entity": "sku", "id": "123"}],
                                    "entity": "parameter",
                                },
                                {
                                    "areValuesEqual": True,
                                    "isAggregated": False,
                                    "type": "enum",
                                    "subType": "",
                                    "id": "202",
                                    "comparedItems": [
                                        {"values": [{"value": "DVD-RW", "id": "300"}], "entity": "sku", "id": "123"}
                                    ],
                                    "entity": "parameter",
                                },
                                {
                                    "areValuesEqual": True,
                                    "isAggregated": False,
                                    "type": "number",
                                    "subType": "",
                                    "id": "203",
                                    "comparedItems": [
                                        {"values": [{"max": "42", "min": "42"}], "entity": "sku", "id": "123"}
                                    ],
                                    "entity": "parameter",
                                },
                            ],
                        }
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json('place=compare_products&hid=100&market-sku=123,124')
        self.assertFragmentIn(
            response,
            {
                "comparedParameters": {
                    "comparedIds": [{"entity": "sku", "id": "123"}, {"entity": "sku", "id": "124"}],
                    "groups": [
                        {
                            "areValuesEqual": False,
                            "params": [
                                {
                                    "areValuesEqual": False,
                                    "isAggregated": False,
                                    "type": "boolean",
                                    "subType": "",
                                    "id": "201",
                                    "comparedItems": [
                                        {"values": [{"value": "0"}], "entity": "sku", "id": "123"},
                                        {"values": [{"value": "1"}], "entity": "sku", "id": "124"},
                                    ],
                                    "entity": "parameter",
                                },
                                {
                                    "areValuesEqual": False,
                                    "isAggregated": False,
                                    "type": "enum",
                                    "subType": "",
                                    "id": "202",
                                    "comparedItems": [
                                        {"values": [{"value": "DVD-RW", "id": "300"}], "entity": "sku", "id": "123"},
                                        {
                                            "values": [
                                                {"value": "CD", "id": "302"},
                                                {"value": "DVD", "id": "301"},
                                                {"value": "DVD-RW", "id": "300"},
                                            ],
                                            "entity": "sku",
                                            "id": "124",
                                        },
                                    ],
                                    "entity": "parameter",
                                },
                                {
                                    "areValuesEqual": True,
                                    "isAggregated": False,
                                    "type": "number",
                                    "subType": "",
                                    "id": "203",
                                    "comparedItems": [
                                        {"values": [{"max": "42", "min": "42"}], "entity": "sku", "id": "123"},
                                        {"values": [{"max": "42", "min": "42"}], "entity": "sku", "id": "124"},
                                    ],
                                    "entity": "parameter",
                                },
                            ],
                        }
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_non_group_category_different_models(self):
        """
        Compare 3 models that have different parameters.
        Check the following:
          * parameter rendering for all possible parameter types (see https://github.yandex-team.ru/market/microformats/pull/157);
          * areValuesEqual rendering logic (should be false everywhere since the values are different);
          * that we don't show valueless parameters (https://st.yandex-team.ru/MARKETOUT-10710#1479892658000).
        """
        response = self.report.request_json('place=compare_products&hid=100&hyperid=500&hyperid=501&hyperid=502')
        # Implicitly test that param 206 is not in the results (by setting allow_different_len/preserve_order).
        self.assertFragmentIn(
            response,
            {
                "comparedParameters": {
                    "comparedIds": [
                        {"id": "500"},
                        {"id": "501"},
                        {"id": "502"},
                    ],
                    "groups": [
                        {
                            "params": [
                                {
                                    "areValuesEqual": False,
                                    "id": "201",
                                    "type": "boolean",
                                    "comparedItems": [
                                        {"id": "500", "values": []},
                                        {"id": "501", "values": [{"value": "0"}]},
                                        {"id": "502", "values": [{"value": "1"}]},
                                    ],
                                },
                                {
                                    "areValuesEqual": False,
                                    "id": "202",
                                    "type": "enum",
                                    "comparedItems": [
                                        {"id": "500", "values": []},
                                        {"id": "501", "values": [{"value": "DVD-RW"}]},
                                        {
                                            "id": "502",
                                            "values": [
                                                {"value": "CD"},
                                                {"value": "DVD"},
                                                {"value": "DVD-RW"},
                                            ],
                                        },
                                    ],
                                },
                                {
                                    "areValuesEqual": False,
                                    "id": "203",
                                    "type": "number",
                                    "comparedItems": [
                                        {"id": "500", "values": []},
                                        {
                                            "id": "501",
                                            "values": [
                                                {
                                                    "min": "42",
                                                    "max": "42",
                                                }
                                            ],
                                        },
                                        {
                                            "id": "502",
                                            "values": [
                                                {
                                                    "min": "42",
                                                    "max": "42",
                                                }
                                            ],
                                        },
                                    ],
                                },
                                {
                                    "areValuesEqual": False,
                                    "id": "204",
                                    "type": "enum",
                                    "subType": "color",
                                    "comparedItems": [
                                        {"id": "500", "values": []},
                                        {
                                            "id": "501",
                                            "values": [
                                                {
                                                    "group": "red",
                                                    "value": "crimson",
                                                    "code": "#FF0000",
                                                }
                                            ],
                                        },
                                        {
                                            "id": "502",
                                            "values": [
                                                {
                                                    "group": "red",
                                                    "value": "crimson",
                                                    "code": "#FF0000",
                                                },
                                                {
                                                    "group": "blue",
                                                    "value": "cyan",
                                                    "code": "#85D6FF",
                                                },
                                                {
                                                    "group": "pink",
                                                    "value": "magenta",
                                                    "code": "#FF00BB",
                                                },
                                            ],
                                        },
                                    ],
                                },
                                {
                                    "areValuesEqual": False,
                                    "id": "205",
                                    "type": "enum",
                                    "subType": "size",
                                    "defaultUnit": "Digits",
                                    "comparedItems": [
                                        {"id": "500", "values": []},
                                        {
                                            "id": "501",
                                            "values": [
                                                {
                                                    "value": "32",
                                                }
                                            ],
                                        },
                                        {
                                            "id": "502",
                                            "values": [
                                                {
                                                    "value": "32",
                                                },
                                                {
                                                    "value": "36",
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ]
                        }
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_non_group_category_similar_models(self):
        """
        Compare identical models and check that areValuesEqual is always true.
        """
        response = self.report.request_json('place=compare_products&hid=100&hyperid=501&hyperid=503')
        self.assertFragmentIn(
            response,
            {
                "params": [
                    {
                        "areValuesEqual": True,
                        "id": "201",
                    },
                    {
                        "areValuesEqual": True,
                        "id": "202",
                    },
                    {
                        "areValuesEqual": True,
                        "id": "203",
                    },
                    {
                        "areValuesEqual": True,
                        "id": "204",
                    },
                    {
                        "areValuesEqual": True,
                        "id": "205",
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_group_category(self):
        """
        Compare a group with modification, a group with no modifications and a modification.
        Modification-level parameters (see params 703 and 704) should be aggregated for groups.
        """
        response = self.report.request_json('place=compare_products&hid=101&hyperid=900&hyperid=901&hyperid=902')
        self.assertFragmentIn(
            response,
            {
                "comparedParameters": {
                    "comparedIds": [
                        {"id": "900"},
                        {"id": "901"},
                        {"id": "902"},
                    ],
                    "groups": [
                        {
                            "params": [
                                {
                                    "areValuesEqual": False,
                                    "id": "701",
                                    "type": "enum",
                                    "isAggregated": False,
                                    "comparedItems": [
                                        {"id": "900", "values": [{"value": "macOS"}]},
                                        {"id": "901", "values": [{"value": "macOS"}, {"value": "Windows"}]},
                                        {"id": "902", "values": [{"value": "Windows"}]},
                                    ],
                                },
                                {
                                    "areValuesEqual": False,
                                    "id": "702",
                                    "type": "number",
                                    "isAggregated": False,
                                    "comparedItems": [
                                        {"id": "900", "values": [{"max": "42", "min": "42"}]},
                                        {"id": "901", "values": [{"max": "43", "min": "43"}]},
                                        {"id": "902", "values": [{"max": "43", "min": "43"}]},
                                    ],
                                },
                                {
                                    "areValuesEqual": False,
                                    "id": "703",
                                    "type": "enum",
                                    "isAggregated": True,
                                    "comparedItems": [
                                        {"id": "900", "values": []},
                                        {
                                            "id": "901",
                                            "values": [
                                                {"value": "Core i3", "id": "803"},
                                                {"value": "Core i5", "id": "804"},
                                            ],
                                        },
                                        {"id": "902", "values": [{"value": "Core i3", "id": "803"}]},
                                    ],
                                },
                                {
                                    "areValuesEqual": False,
                                    "id": "704",
                                    "type": "number",
                                    "isAggregated": True,
                                    "comparedItems": [
                                        {"id": "900", "values": []},
                                        {"id": "901", "values": [{"max": "20", "min": "10"}]},
                                        {"id": "902", "values": []},
                                    ],
                                },
                            ]
                        }
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_non_existent_model(self):
        response = self.report.request_json('place=compare_products&hid=100&hyperid=501&hyperid=12345')
        self.assertFragmentIn(
            response,
            {
                "comparedParameters": {
                    "comparedIds": [
                        {"id": "501"},
                    ],
                    "groups": [
                        {
                            "params": [
                                {
                                    "areValuesEqual": True,
                                    "id": "201",
                                    "comparedItems": [
                                        {"id": "501", "values": [{"value": "0"}]},
                                    ],
                                },
                                {
                                    "areValuesEqual": True,
                                    "id": "202",
                                    "comparedItems": [
                                        {"id": "501", "values": [{"value": "DVD-RW"}]},
                                    ],
                                },
                                {
                                    "areValuesEqual": True,
                                    "id": "203",
                                    "comparedItems": [
                                        {
                                            "id": "501",
                                            "values": [
                                                {
                                                    "min": "42",
                                                    "max": "42",
                                                }
                                            ],
                                        },
                                    ],
                                },
                                {
                                    "areValuesEqual": True,
                                    "id": "204",
                                    "comparedItems": [
                                        {
                                            "id": "501",
                                            "values": [
                                                {
                                                    "group": "red",
                                                    "value": "crimson",
                                                    "code": "#FF0000",
                                                }
                                            ],
                                        },
                                    ],
                                },
                                {
                                    "areValuesEqual": True,
                                    "id": "205",
                                    "comparedItems": [
                                        {
                                            "id": "501",
                                            "values": [
                                                {
                                                    "value": "32",
                                                }
                                            ],
                                        },
                                    ],
                                },
                            ]
                        }
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_model_transitions(self):
        # Even if there is a new strong cluster (1000000001) for a non-existent one (1000000000), it should not be
        # in the results unless &with-rebuilt-model=1 is set.
        response = self.report.request_json('place=compare_products&hid=100&hyperid=1000000000&hyperid=1000000002')
        self.assertFragmentIn(
            response,
            {
                "comparedParameters": {
                    "comparedIds": [
                        {"id": "1000000002"},
                    ],
                    "groups": [
                        {
                            "params": [
                                {
                                    "areValuesEqual": True,
                                    "id": "201",
                                    "type": "boolean",
                                    "comparedItems": [
                                        {"id": "1000000002", "values": [{"value": "1"}]},
                                    ],
                                },
                            ]
                        }
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # &with-rebuilt-model=1 turns on model transitions.
        # Make sure that we get a strong new cluster (1000000001) when we try to include an old one (1000000000)
        # in comparison.
        response = self.report.request_json(
            'place=compare_products&hid=100&hyperid=1000000000&hyperid=1000000002&with-rebuilt-model=1'
        )
        self.assertFragmentIn(
            response,
            {
                "comparedParameters": {
                    "comparedIds": [
                        {
                            "id": "1000000001",
                            "deletedId": "1000000000",
                        },
                        {"id": "1000000002"},
                    ],
                    "groups": [
                        {
                            "params": [
                                {
                                    "areValuesEqual": False,
                                    "id": "201",
                                    "type": "boolean",
                                    "comparedItems": [
                                        {"id": "1000000001", "values": [{"value": "0"}]},
                                        {"id": "1000000002", "values": [{"value": "1"}]},
                                    ],
                                },
                            ]
                        }
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # The original cluster (1000000004) is dead but the new one (1000000005) is also dead.
        # Everybody is dead and life does not make any sense.
        response = self.report.request_json(
            'place=compare_products&hid=100&hyperid=1000000004&hyperid=1000000002&with-rebuilt-model=1'
        )
        self.assertFragmentIn(
            response,
            {
                "comparedParameters": {
                    "comparedIds": [
                        {"id": "1000000002"},
                    ],
                    "groups": [
                        {
                            "params": [
                                {
                                    "areValuesEqual": True,
                                    "id": "201",
                                    "type": "boolean",
                                    "comparedItems": [
                                        {"id": "1000000002", "values": [{"value": "1"}]},
                                    ],
                                },
                            ]
                        }
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_category_with_parameter_groups(self):
        """
        Compare models in category 102 that has parameter groups (they are extracted from the model description template).
        Parameters 100{1..3} should be in group 1, 100{4..6} should be in group 2.
        100{7..9} do not belong to any group and are not output.
        Note that parameter groups are COMPLETELY unrelated to model groups (which are tested in test_group_category).
        """
        response = self.report.request_json('place=compare_products&hid=102&hyperid=1500&hyperid=1501')
        self.assertFragmentIn(
            response,
            {
                "groups": [
                    {
                        # Note that parameters in a group should be ordered by their position (which roughly
                        # corresponds to their importance).
                        "title": "GROUP 1",
                        "areValuesEqual": False,
                        "params": [
                            {"id": "1001"},
                            {"id": "1005"},
                            {"id": "1006"},
                        ],
                    },
                    {
                        "title": "GROUP 2",
                        "areValuesEqual": False,
                        "params": [
                            {"id": "1003"},
                            {"id": "1007"},
                            {"id": "1009"},
                        ],
                    },
                    {
                        # Color/size parameters should be in the unnamed group, everything else should not.
                        # See MARKETOUT-11176
                        "title": Absent(),
                        "params": [
                            {"id": "1002"},
                            {"id": "1004"},
                        ],
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Make sure we still output the parameters that are present only in one model (param 1001 in model 1502
        # and param 1006 in model 1503).
        response = self.report.request_json('place=compare_products&hid=102&hyperid=1502&hyperid=1503')
        self.assertFragmentIn(
            response,
            {
                "groups": [
                    {
                        "title": "GROUP 1",
                        "areValuesEqual": False,
                        "params": [
                            {"id": "1001"},
                            {"id": "1005"},
                            {"id": "1006"},
                        ],
                    }
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Taking this to extremes: no parameters with values for all models.
        response = self.report.request_json('place=compare_products&hid=102&hyperid=1504&hyperid=1505')
        self.assertFragmentIn(
            response,
            {
                "groups": [
                    {
                        "title": "GROUP 1",
                        "areValuesEqual": False,
                        "params": [
                            {"id": "1001"},
                            {"id": "1005"},
                        ],
                    }
                ]
            },
        )

        # Check the areValuesEqual flag for groups. Models 1506 and 1507 are identical except for parameter
        # 1003. Therefore, the group that has param 1003 should have allValuesEqual = False and the other
        # group should have allValuesEqual = True.
        response = self.report.request_json('place=compare_products&hid=102&hyperid=1506&hyperid=1507')
        self.assertFragmentIn(
            response,
            {
                "groups": [
                    {
                        "title": "GROUP 1",
                        "areValuesEqual": True,
                        "params": [
                            {"id": "1001", "areValuesEqual": True},
                            {"id": "1005", "areValuesEqual": True},
                            {"id": "1006", "areValuesEqual": True},
                        ],
                    },
                    {
                        "title": "GROUP 2",
                        "areValuesEqual": False,
                        "params": [
                            {"id": "1003", "areValuesEqual": False},
                            {"id": "1007", "areValuesEqual": True},
                            {"id": "1009", "areValuesEqual": True},
                        ],
                    },
                ]
            },
        )

    @classmethod
    def prepare_numeric_filter_calculated(cls):
        """
        Тест проверяет корректное выполнение GUMOFUL-скрипта в случае, когда он явно работает с числом
        и производит над ним операции.
        Скрипт, на котором происходило падение, взят с прода.
        """

        cls.index.gltypes += [
            GLType(param_id=3031, hid=105, position=1, xslname='RAMVol', gltype=GLType.NUMERIC, unit_name="MB"),
            GLType(param_id=3032, hid=105, position=2, xslname='SimpleVol', gltype=GLType.NUMERIC),
        ]

        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=105,
                friendlymodel=[],
                model=[
                    (
                        "GROUP 1",
                        {
                            "RAMVol": '{if ($RAMVol>1023) return $RAMVol/1024 +"ГБ"; if (($RAMVol<1024)&&($RAMVol>0)) return (string)$RAMVol+"МБ"; else return ""; #exec}',
                            "SimpleVol": "{SimpleVol#ifnz}/SimpleVol{#endif}",
                        },
                    ),
                    (
                        "GROUP 2",
                        {
                            "SimpleVol": "{SimpleVol}",
                        },
                    ),
                ],
            )
        ]

        cls.index.models += [
            Model(
                hyperid=4501,
                hid=105,
                glparams=[
                    GLParam(param_id=3031, value=1200),
                    GLParam(param_id=3032, value=6),
                ],
            ),
            Model(
                hyperid=4502,
                hid=105,
                glparams=[
                    GLParam(param_id=3031, value=2200),
                    GLParam(param_id=3032, value=7),
                ],
            ),
        ]

    def test_numeric_filter_calculated(self):
        response = self.report.request_json('place=compare_products&hid=105&hyperid=4501&hyperid=4502')
        self.assertFragmentIn(
            response,
            {
                "groups": [
                    {
                        "title": "GROUP 1",
                        "areValuesEqual": False,
                        "params": [
                            {"id": "3031"},
                            {"id": "3032"},
                        ],
                    }
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_image_picker_compare(cls):
        """
        Создаем офферный параметры с типом "картинка-пикер" в группе и не в группе параметров
        Создаем модели и офферы с этими параметрами, приматченные к ним
        """
        cls.index.gltypes += [
            # Параметр не в группе параметров
            GLType(param_id=241, hid=70, gltype=GLType.ENUM, subtype='image_picker', cluster_filter=True, hidden=True),
            # Параметр в группе параметров
            GLType(
                param_id=242,
                hid=70,
                gltype=GLType.ENUM,
                subtype='image_picker',
                xslname='Group1Param1',
                cluster_filter=True,
                hidden=True,
            ),
        ]
        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=70,
                friendlymodel=[],
                model=[
                    (
                        "GROUP 1",
                        {
                            "G1P1": "{Group1Param1}",
                        },
                    ),
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=4101, hid=70),
            Model(hyperid=4102, hid=70),
            Model(hyperid=4103, hid=70),
            Model(hyperid=4104, hid=70),
        ]

        cls.index.offers += [
            Offer(hyperid=4101, glparams=[GLParam(param_id=241, value=SILVER)]),
            Offer(hyperid=4102, glparams=[GLParam(param_id=241, value=ROSE_GOLD)]),
            Offer(hyperid=4103, glparams=[GLParam(param_id=242, value=0)]),
            Offer(hyperid=4104, glparams=[GLParam(param_id=242, value=1)]),
        ]

    def test_image_picker_compare(self):
        """Что тестируем: офферный параметр с типом "картинка-пикер" не участвует
        в сравнении товаров, будучи в группе параметров или не в группе
        """
        response = self.report.request_json('place=compare_products&hid=70&hyperid=4101&hyperid=4102')
        self.assertFragmentNotIn(response, {"groups": [{"params": [{"id": "241"}]}]})

        response = self.report.request_json('place=compare_products&hid=70&hyperid=4103&hyperid=4104')
        self.assertFragmentNotIn(response, {"groups": [{"params": [{"id": "242"}]}]})

    @classmethod
    def prepare_short_params_list(cls):
        cls.index.gltypes += [
            GLType(param_id=2001, hid=200, position=1, gltype=GLType.NUMERIC),
            GLType(
                param_id=2002,
                hid=200,
                position=2,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=20021),
                    GLValue(value_id=20022),
                ],
            ),
            GLType(param_id=2003, hid=200, position=2, xslname='NotPopularParam', gltype=GLType.NUMERIC),
            GLType(param_id=3001, hid=300, position=1, gltype=GLType.ENUM),
            GLType(param_id=3002, hid=300, position=2, subtype='image_picker', cluster_filter=True, hidden=True),
        ]

        cls.index.categories_compare_params += [
            CategoryCompareParams(hid=200, params=['2002', '100500', 'abc', '2001']),
            CategoryCompareParams(hid=300, params=['3002', '3001']),
        ]

        cls.index.models += [
            Model(
                hyperid=200001,
                hid=200,
                glparams=[GLParam(param_id=2001, value=10), GLParam(param_id=2002, value=20021)],
            ),
            Model(
                hyperid=200002,
                hid=200,
                glparams=[GLParam(param_id=2001, value=35), GLParam(param_id=2002, value=20022)],
            ),
            Model(
                hyperid=300001,
                hid=300,
                glparams=[GLParam(param_id=3001, value=25), GLParam(param_id=3002, value=SILVER)],
            ),
        ]

    def test_short_params_list(self):
        '''Проверяем, что при наличии short-params-list=1 показываем выбранный список параметров из файла categories_compare_params.csv'''

        # несуществующие и невалидные параметры '100500' и 'abc' проигнорились,
        # а существующие 2001 и 2002 следуют в порядке из categories_compare_params, а не согласно position
        response = self.report.request_json(
            'place=compare_products&hid=200&hyperid=200001&hyperid=200002&short-params-list=1'
        )
        self.assertFragmentIn(
            response,
            {
                "groups": [
                    {
                        'params': [
                            {
                                'id': '2002',
                                'comparedItems': [
                                    {'id': '200001', 'values': [{'id': '20021'}]},
                                    {'id': '200002', 'values': [{'id': '20022'}]},
                                ],
                            },
                            {
                                'id': '2001',
                                'comparedItems': [
                                    {'id': '200001', 'values': [{'min': '10'}]},
                                    {'id': '200002', 'values': [{'min': '35'}]},
                                ],
                            },
                        ]
                    }
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # image_picker не показывается, даже если указан в categories_compare_params
        response = self.report.request_json('place=compare_products&hid=300&hyperid=300001&short-params-list=1')
        self.assertFragmentIn(
            response,
            {'comparedIds': [{'id': '300001'}], 'groups': [{'params': [{'id': '3001'}]}]},
            allow_different_len=False,
            preserve_order=True,
        )

        # для хидов, не указанных в categories_compare_params, с параметром short-params-list=1 выдача пустая
        req = 'place=compare_products&hid=100&hyperid=500,501'
        response = self.report.request_json(req + '&short-params-list=1')
        self.assertFragmentIn(
            response,
            {
                'comparedIds': ElementCount(
                    0
                ),  # при short-params-list=1 если хид в файле не найден, то плейс сразу завершается
                'groups': ElementCount(0),
            },
        )
        response = self.report.request_json(req)
        self.assertFragmentIn(response, {'comparedIds': ElementCount(2), 'groups': ElementCount(1)})

    @classmethod
    def prepare_virtual_cards_compare(cls):

        cls.index.hypertree += [HyperCategory(hid=1252, name="Королевства севера")]

        cls.index.gltypes += [
            GLType(param_id=1100, hid=1252, position=1, gltype=GLType.BOOL, name='Захвачена ли Нильфгаардом'),
            GLType(param_id=1101, hid=1252, position=2, gltype=GLType.NUMERIC, name='Год основания'),
            GLType(
                param_id=1102,
                hid=1252,
                position=3,
                gltype=GLType.ENUM,
                name='Столица',
                values=[
                    GLValue(value_id=10, text='Вызима'),
                    GLValue(value_id=11, text='Третогор'),
                    GLValue(value_id=12, text='Венгерберг'),
                    GLValue(value_id=13, text='Ард Каррайг'),
                ],
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=11252,
                hid=1252,
                title='Темерия',
                glparams=[
                    GLParam(param_id=1100, value=1),
                    GLParam(param_id=1101, value=100),
                    GLParam(param_id=1102, value=10),
                ],
            ),
            Model(hyperid=11253, hid=1252, title='Аэдирн'),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=11253,
                sku=33253,
                glparams=[
                    GLParam(param_id=1100, value=1),
                    GLParam(param_id=1101, value=101),
                    GLParam(param_id=1102, value=12),
                ],
            ),
        ]

        # Cpa оффера для виртуальных моделек
        cls.index.offers += [
            Offer(
                virtual_model_id=Const.VMID_START + 10,
                title='Редания',
                hid=1252,
                cpa=Offer.CPA_REAL,
                glparams=[
                    GLParam(param_id=1100, value=0),
                    GLParam(param_id=1101, value=102),
                    GLParam(param_id=1102, value=11),
                ],
                waremd5='OfferCpa0____________g',
            ),
            Offer(
                virtual_model_id=Const.VMID_START + 11,
                title='Каэдвен',
                hid=1252,
                cpa=Offer.CPA_REAL,
                glparams=[
                    GLParam(param_id=1100, value=1),
                    GLParam(param_id=1101, value=103),
                    GLParam(param_id=1102, value=13),
                ],
                waremd5='OfferCpa1____________g',
            ),
            Offer(
                virtual_model_id=Const.VMID_START + 12,
                title='Каэдвен_1',
                hid=1252,
                cpa=Offer.CPA_REAL,
                glparams=[
                    GLParam(param_id=1100, value=1),
                    GLParam(param_id=1101, value=103),
                    GLParam(param_id=1102, value=13),
                ],
                waremd5='OfferCpa2____________g',
            ),
        ]

    def test_virtual_cards_compare(self):
        """
        Кейсы для тестов:
        1) Без мску:
            - только vmid-ы
            - vmid-ы + обычные модельки
        2) С мску:
            - только виртульные
            - виртуальные и обычные

        TODO: вероятно при запросе виртуального msku надо помечать его как sku в comparedIds, а не как виртуальный product
        """

        # Флаг быстрых карточек не должен ломать старые виртуальные
        for flags in ['', '&rearr-factors=use_fast_cards=1']:
            request = 'place=compare_products&hyperid={}&hyperid={}&hid=1252{}'.format(
                Const.VMID_START + 10, Const.VMID_START + 11, flags
            )
            response = self.report.request_json(request)

            only_virtual_resp = {
                "comparedParameters": {
                    "comparedIds": [
                        {"entity": "product", "id": "2000000000010"},
                        {"entity": "product", "id": "2000000000011"},
                    ],
                    "groups": [
                        {
                            "areValuesEqual": False,
                            "params": [
                                {
                                    "areValuesEqual": False,
                                    "comparedItems": [
                                        {"entity": "product", "id": "2000000000010", "values": [{"value": "0"}]},
                                        {"entity": "product", "id": "2000000000011", "values": [{"value": "1"}]},
                                    ],
                                    "id": "1100",
                                    "name": "Захвачена ли Нильфгаардом",
                                },
                                {
                                    # Этот параметр нужен офферам для вирт карточек, сейчас в лайтах он тоже выдается
                                    # MARKETOUT-38207
                                    "areValuesEqual": True,
                                    "id": "16695477",
                                },
                                {
                                    "areValuesEqual": False,
                                    "comparedItems": [
                                        {
                                            "entity": "product",
                                            "id": "2000000000010",
                                            "values": [{"max": "102", "min": "102"}],
                                        },
                                        {
                                            "entity": "product",
                                            "id": "2000000000011",
                                            "values": [{"max": "103", "min": "103"}],
                                        },
                                    ],
                                    "id": "1101",
                                    "name": "Год основания",
                                },
                                {
                                    "areValuesEqual": False,
                                    "comparedItems": [
                                        {
                                            "entity": "product",
                                            "id": "2000000000010",
                                            "values": [{"id": "11", "value": "Третогор"}],
                                        },
                                        {
                                            "entity": "product",
                                            "id": "2000000000011",
                                            "values": [{"id": "13", "value": "Ард Каррайг"}],
                                        },
                                    ],
                                    "id": "1102",
                                    "name": "Столица",
                                },
                            ],
                        }
                    ],
                }
            }

            self.assertFragmentIn(response, only_virtual_resp, allow_different_len=False, preserve_order=True)

            # Запрашиваем два виртуальных айдишника, у офферов которых все параметры одинаковые
            request = 'place=compare_products&hyperid={}&hyperid={}&hid=1252{}'.format(
                Const.VMID_START + 11, Const.VMID_START + 12, flags
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "comparedParameters": {
                        "comparedIds": [
                            {"entity": "product", "id": "2000000000011"},
                            {"entity": "product", "id": "2000000000012"},
                        ],
                        "groups": [
                            {
                                "areValuesEqual": True,
                                "params": [
                                    {
                                        "areValuesEqual": True,
                                        "id": "1100",
                                        "name": "Захвачена ли Нильфгаардом",
                                    },
                                    {
                                        # Этот параметр нужен офферам для вирт карточек, сейчас в лайтах он тоже выдается
                                        # MARKETOUT-38207
                                        "areValuesEqual": True,
                                        "id": "16695477",
                                    },
                                    {
                                        "areValuesEqual": True,
                                        "id": "1101",
                                        "name": "Год основания",
                                    },
                                    {
                                        "areValuesEqual": True,
                                        "id": "1102",
                                        "name": "Столица",
                                    },
                                ],
                            }
                        ],
                    }
                },
                allow_different_len=False,
                preserve_order=True,
            )

            # Запрашиваем виртуальную и обычную модельку
            request = 'place=compare_products&hyperid={}&hyperid={}&hid=1252{}'.format(
                Const.VMID_START + 11, 11252, flags
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "comparedParameters": {
                        "comparedIds": [
                            {"entity": "product", "id": "2000000000011"},
                            {"entity": "product", "id": "11252"},
                        ],
                        "groups": [
                            {
                                "areValuesEqual": False,
                                "params": [
                                    {
                                        "areValuesEqual": True,
                                        "comparedItems": [
                                            {"entity": "product", "id": "2000000000011", "values": [{"value": "1"}]},
                                            {"entity": "product", "id": "11252", "values": [{"value": "1"}]},
                                        ],
                                        "id": "1100",
                                        "name": "Захвачена ли Нильфгаардом",
                                    },
                                    {
                                        # Этот параметр нужен офферам для вирт карточек, сейчас в лайтах он тоже выдается
                                        # MARKETOUT-38207
                                        "areValuesEqual": False,
                                        "comparedItems": [
                                            {
                                                "entity": "product",
                                                "id": "2000000000011",
                                                "values": [{"id": "16695478", "value": "VALUE-16695478"}],
                                            },
                                            {"entity": "product", "id": "11252", "values": []},
                                        ],
                                        "id": "16695477",
                                    },
                                    {
                                        "areValuesEqual": False,
                                        "comparedItems": [
                                            {
                                                "entity": "product",
                                                "id": "2000000000011",
                                                "values": [{"max": "103", "min": "103"}],
                                            },
                                            {
                                                "entity": "product",
                                                "id": "11252",
                                                "values": [{"max": "100", "min": "100"}],
                                            },
                                        ],
                                        "id": "1101",
                                        "name": "Год основания",
                                    },
                                    {
                                        "areValuesEqual": False,
                                        "comparedItems": [
                                            {
                                                "entity": "product",
                                                "id": "2000000000011",
                                                "values": [{"id": "13", "value": "Ард Каррайг"}],
                                            },
                                            {
                                                "entity": "product",
                                                "id": "11252",
                                                "values": [{"id": "10", "value": "Вызима"}],
                                            },
                                        ],
                                        "id": "1102",
                                        "name": "Столица",
                                    },
                                ],
                            }
                        ],
                    }
                },
                allow_different_len=False,
                preserve_order=True,
            )

            # Запрашиваем два виртуальных мску, но на выходе должны быть виртуальные продукты
            request = 'place=compare_products&market-sku={}&market-sku={}&hid=1252{}'.format(
                Const.VMID_START + 10, Const.VMID_START + 11, flags
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(response, only_virtual_resp, allow_different_len=False, preserve_order=True)

            # Запрашиваем виртуальный + обычный мску -> на выходе вирт продукт + ску
            request = 'place=compare_products&market-sku={}&market-sku={}&hid=1252{}'.format(
                Const.VMID_START + 11, 33253, flags
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "comparedParameters": {
                        "comparedIds": [{"entity": "product", "id": "2000000000011"}, {"entity": "sku", "id": "33253"}],
                        "groups": [
                            {
                                "areValuesEqual": False,
                                "params": [
                                    {
                                        "areValuesEqual": True,
                                        "comparedItems": [
                                            {"entity": "product", "id": "2000000000011", "values": [{"value": "1"}]},
                                            {"entity": "sku", "id": "33253", "values": [{"value": "1"}]},
                                        ],
                                        "id": "1100",
                                        "name": "Захвачена ли Нильфгаардом",
                                    },
                                    {
                                        # Этот параметр нужен офферам для вирт карточек, сейчас в лайтах он тоже выдается
                                        # MARKETOUT-38207
                                        "areValuesEqual": False,
                                        "comparedItems": [
                                            {
                                                "entity": "product",
                                                "id": "2000000000011",
                                                "values": [{"id": "16695478", "value": "VALUE-16695478"}],
                                            },
                                            {"entity": "sku", "id": "33253", "values": []},
                                        ],
                                        "id": "16695477",
                                    },
                                    {
                                        "areValuesEqual": False,
                                        "comparedItems": [
                                            {
                                                "entity": "product",
                                                "id": "2000000000011",
                                                "values": [{"max": "103", "min": "103"}],
                                            },
                                            {"entity": "sku", "id": "33253", "values": [{"max": "101", "min": "101"}]},
                                        ],
                                        "id": "1101",
                                        "name": "Год основания",
                                    },
                                    {
                                        "areValuesEqual": False,
                                        "comparedItems": [
                                            {
                                                "entity": "product",
                                                "id": "2000000000011",
                                                "values": [{"id": "13", "value": "Ард Каррайг"}],
                                            },
                                            {
                                                "entity": "sku",
                                                "id": "33253",
                                                "values": [{"id": "12", "value": "Венгерберг"}],
                                            },
                                        ],
                                        "id": "1102",
                                        "name": "Столица",
                                    },
                                ],
                            }
                        ],
                    }
                },
                allow_different_len=False,
                preserve_order=True,
            )

            # Запрашиваем 1 вирт hyperId + 1 обычный hyperId + 1 вирт мску + 1 обычный мску
            # Важно, что порядок в comparedIds следующий: сначала все hyperid в том же порядке, что в запросе затем аналогично msku
            request = 'place=compare_products&market-sku={}&market-sku={}&hyperid={}&hyperid={}&hid=1252{}'.format(
                Const.VMID_START + 11, 33253, Const.VMID_START + 10, 11252, flags
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "comparedParameters": {
                        "comparedIds": [
                            {"entity": "product", "id": "2000000000010"},
                            {"entity": "product", "id": "11252"},
                            {"entity": "product", "id": "2000000000011"},
                            {"entity": "sku", "id": "33253"},
                        ],
                        "groups": [
                            {
                                "areValuesEqual": False,
                                "params": [
                                    {
                                        "areValuesEqual": False,
                                        "comparedItems": [
                                            {"entity": "product", "id": "2000000000010", "values": [{"value": "0"}]},
                                            {"entity": "product", "id": "11252", "values": [{"value": "1"}]},
                                            {"entity": "product", "id": "2000000000011", "values": [{"value": "1"}]},
                                            {"entity": "sku", "id": "33253", "values": [{"value": "1"}]},
                                        ],
                                        "id": "1100",
                                        "name": "Захвачена ли Нильфгаардом",
                                    },
                                    {
                                        "areValuesEqual": False,
                                        "comparedItems": [
                                            {
                                                "entity": "product",
                                                "id": "2000000000010",
                                                "values": [{"id": "16695478", "value": "VALUE-16695478"}],
                                            },
                                            {"entity": "product", "id": "11252", "values": []},
                                            {
                                                "entity": "product",
                                                "id": "2000000000011",
                                                "values": [{"id": "16695478", "value": "VALUE-16695478"}],
                                            },
                                            {"entity": "sku", "id": "33253", "values": []},
                                        ],
                                        "id": "16695477",
                                        "name": "GLPARAM-16695477",
                                    },
                                    {
                                        "areValuesEqual": False,
                                        "comparedItems": [
                                            {
                                                "entity": "product",
                                                "id": "2000000000010",
                                                "values": [{"max": "102", "min": "102"}],
                                            },
                                            {
                                                "entity": "product",
                                                "id": "11252",
                                                "values": [{"max": "100", "min": "100"}],
                                            },
                                            {
                                                "entity": "product",
                                                "id": "2000000000011",
                                                "values": [{"max": "103", "min": "103"}],
                                            },
                                            {"entity": "sku", "id": "33253", "values": [{"max": "101", "min": "101"}]},
                                        ],
                                        "id": "1101",
                                        "name": "Год основания",
                                    },
                                    {
                                        "areValuesEqual": False,
                                        "comparedItems": [
                                            {
                                                "entity": "product",
                                                "id": "2000000000010",
                                                "values": [{"id": "11", "value": "Третогор"}],
                                            },
                                            {
                                                "entity": "product",
                                                "id": "11252",
                                                "values": [{"id": "10", "value": "Вызима"}],
                                            },
                                            {
                                                "entity": "product",
                                                "id": "2000000000011",
                                                "values": [{"id": "13", "value": "Ард Каррайг"}],
                                            },
                                            {
                                                "entity": "sku",
                                                "id": "33253",
                                                "values": [{"id": "12", "value": "Венгерберг"}],
                                            },
                                        ],
                                        "id": "1102",
                                        "name": "Столица",
                                    },
                                ],
                            }
                        ],
                    }
                },
                allow_different_len=False,
                preserve_order=True,
            )

    @classmethod
    def prepare_fast_cards(cls):
        cls.index.shops += [
            Shop(fesh=12708, priority_region=213),
            Shop(fesh=12709, priority_region=213),
            Shop(fesh=12710, datafeed_id=14240, priority_region=213, regions=[213], client_id=12, cpa=Shop.CPA_REAL),
            Shop(
                fesh=12711,
                datafeed_id=14241,
                priority_region=213,
                regions=[213],
                client_id=13,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.index.offers += [
            Offer(
                title='Редания - БК - 1580',
                hid=1252,
                fesh=12708,
                price=165,
                waremd5='offer_cpc_vmid_fc0__mQ',
                sku=1580,
                virtual_model_id=1580,
                vmid_is_literal=False,
                glparams=[
                    GLParam(param_id=1100, value=0),
                    GLParam(param_id=1101, value=101),
                    GLParam(param_id=1102, value=11),
                ],
            ),
            Offer(
                title='Редания - БК v2 - 1580',
                hid=1252,
                fesh=12709,
                price=175,
                waremd5='offer_cpc_vmid_fc1__mQ',
                sku=1580,
                virtual_model_id=1580,
                vmid_is_literal=False,
                glparams=[
                    GLParam(param_id=1100, value=0),
                    GLParam(param_id=1101, value=102),
                    GLParam(param_id=1102, value=11),
                ],
            ),
            Offer(
                title='Каэдвен - БК - 1581',
                hid=1252,
                fesh=12710,
                waremd5='offer_cpa_vmid_fc0__mQ',
                price=150,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[14240],
                sku=1581,
                virtual_model_id=1581,
                vmid_is_literal=False,
                glparams=[
                    GLParam(param_id=1100, value=1),
                    GLParam(param_id=1101, value=103),
                    GLParam(param_id=1102, value=13),
                ],
            ),
            Offer(
                title='Каэдвен_1 - БК - 1582',
                hid=1252,
                fesh=12711,
                waremd5='offer_blue_vmid_fc0_mQ',
                price=250,
                delivery_buckets=[14241],
                sku=1582,
                virtual_model_id=1582,
                blue_without_real_sku=True,
                vmid_is_literal=False,
                glparams=[
                    GLParam(param_id=1100, value=1),
                    GLParam(param_id=1101, value=103),
                    GLParam(param_id=1102, value=13),
                ],
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=14240,
                fesh=12710,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
            DeliveryBucket(
                bucket_id=14241,
                fesh=12711,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
        ]

    def test_fast_cards_compare_products(self):
        '''
        Появились новые типы виртуальных карточек - Быстрые карточки
        Их айдишник неотличим от sku и лежит в нем же
        Но у офферов с скюшкой быстрых карточек в extraData лежит VirtualModelId == sku
        А литерала vmid нет

        Проверяем, что для них работает сравнение

        Под флагом: use_fast_cards
        '''

        # БК + БК
        flags = '&rearr-factors=use_fast_cards=1'
        request = 'place=compare_products&hyperid={}&hyperid={}&hid=1252'.format(1580, 1581) + flags
        response = self.report.request_json(request)

        only_bk_resp = {
            "comparedParameters": {
                "comparedIds": [
                    {"entity": "product", "id": "1580"},
                    {"entity": "product", "id": "1581"},
                ],
                "groups": [
                    {
                        "areValuesEqual": False,
                        "params": [
                            {
                                "areValuesEqual": False,
                                "comparedItems": [
                                    {"entity": "product", "id": "1580", "values": [{"value": "0"}]},
                                    {"entity": "product", "id": "1581", "values": [{"value": "1"}]},
                                ],
                                "id": "1100",
                                "name": "Захвачена ли Нильфгаардом",
                            },
                            {
                                # Этот параметр нужен офферам для вирт карточек, сейчас в лайтах он тоже выдается
                                # MARKETOUT-38207
                                "areValuesEqual": True,
                                "id": "16695477",
                            },
                            {
                                "areValuesEqual": False,
                                "comparedItems": [
                                    {
                                        "entity": "product",
                                        "id": "1580",
                                        # Значение из оффера с минимальным offset, именно по такому офферу дожна строиться быстрая карточка
                                        "values": [{"max": "101", "min": "101"}],
                                    },
                                    {
                                        "entity": "product",
                                        "id": "1581",
                                        "values": [{"max": "103", "min": "103"}],
                                    },
                                ],
                                "id": "1101",
                                "name": "Год основания",
                            },
                            {
                                "areValuesEqual": False,
                                "comparedItems": [
                                    {
                                        "entity": "product",
                                        "id": "1580",
                                        "values": [{"id": "11", "value": "Третогор"}],
                                    },
                                    {
                                        "entity": "product",
                                        "id": "1581",
                                        "values": [{"id": "13", "value": "Ард Каррайг"}],
                                    },
                                ],
                                "id": "1102",
                                "name": "Столица",
                            },
                        ],
                    }
                ],
            }
        }
        self.assertFragmentIn(response, only_bk_resp, allow_different_len=False, preserve_order=True)

        # Запрашиваем два БК-шных айдишника, у офферов которых все параметры одинаковые
        request = 'place=compare_products&hyperid={}&hyperid={}&hid=1252'.format(1581, 1582) + flags
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "comparedParameters": {
                    "comparedIds": [
                        {"entity": "product", "id": "1581"},
                        {"entity": "product", "id": "1582"},
                    ],
                    "groups": [
                        {
                            "areValuesEqual": True,
                            "params": [
                                {
                                    "areValuesEqual": True,
                                    "id": "1100",
                                    "name": "Захвачена ли Нильфгаардом",
                                },
                                {
                                    # Этот параметр нужен офферам для вирт карточек, сейчас в лайтах он тоже выдается
                                    # MARKETOUT-38207
                                    "areValuesEqual": True,
                                    "id": "16695477",
                                },
                                {
                                    "areValuesEqual": True,
                                    "id": "1101",
                                    "name": "Год основания",
                                },
                                {
                                    "areValuesEqual": True,
                                    "id": "1102",
                                    "name": "Столица",
                                },
                            ],
                        }
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # Запрашиваем БК и обычную модельку
        request = 'place=compare_products&hyperid={}&hyperid={}&hid=1252'.format(1581, 11252) + flags
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "comparedParameters": {
                    "comparedIds": [{"entity": "product", "id": "1581"}, {"entity": "product", "id": "11252"}],
                    "groups": [
                        {
                            "areValuesEqual": False,
                            "params": [
                                {
                                    "areValuesEqual": True,
                                    "comparedItems": [
                                        {"entity": "product", "id": "1581", "values": [{"value": "1"}]},
                                        {"entity": "product", "id": "11252", "values": [{"value": "1"}]},
                                    ],
                                    "id": "1100",
                                    "name": "Захвачена ли Нильфгаардом",
                                },
                                {
                                    # Этот параметр нужен офферам для вирт карточек, сейчас в лайтах он тоже выдается
                                    # MARKETOUT-38207
                                    "areValuesEqual": False,
                                    "comparedItems": [
                                        {
                                            "entity": "product",
                                            "id": "1581",
                                            "values": [{"id": "16695478", "value": "VALUE-16695478"}],
                                        },
                                        {"entity": "product", "id": "11252", "values": []},
                                    ],
                                    "id": "16695477",
                                },
                                {
                                    "areValuesEqual": False,
                                    "comparedItems": [
                                        {
                                            "entity": "product",
                                            "id": "1581",
                                            "values": [{"max": "103", "min": "103"}],
                                        },
                                        {"entity": "product", "id": "11252", "values": [{"max": "100", "min": "100"}]},
                                    ],
                                    "id": "1101",
                                    "name": "Год основания",
                                },
                                {
                                    "areValuesEqual": False,
                                    "comparedItems": [
                                        {
                                            "entity": "product",
                                            "id": "1581",
                                            "values": [{"id": "13", "value": "Ард Каррайг"}],
                                        },
                                        {
                                            "entity": "product",
                                            "id": "11252",
                                            "values": [{"id": "10", "value": "Вызима"}],
                                        },
                                    ],
                                    "id": "1102",
                                    "name": "Столица",
                                },
                            ],
                        }
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # Запрашиваем два БК-шных мску, но на выходе должны быть виртуальные продукты
        request = 'place=compare_products&market-sku={}&market-sku={}&hid=1252'.format(1580, 1581) + flags
        response = self.report.request_json(request)
        self.assertFragmentIn(response, only_bk_resp, allow_different_len=False, preserve_order=True)

        # Запрашиваем БК + обычный мску -> на выходе вирт продукт + ску
        request = 'place=compare_products&market-sku={}&market-sku={}&hid=1252'.format(1581, 33253) + flags
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "comparedParameters": {
                    "comparedIds": [{"entity": "product", "id": "1581"}, {"entity": "sku", "id": "33253"}],
                    "groups": [
                        {
                            "areValuesEqual": False,
                            "params": [
                                {
                                    "areValuesEqual": True,
                                    "comparedItems": [
                                        {"entity": "product", "id": "1581", "values": [{"value": "1"}]},
                                        {"entity": "sku", "id": "33253", "values": [{"value": "1"}]},
                                    ],
                                    "id": "1100",
                                    "name": "Захвачена ли Нильфгаардом",
                                },
                                {
                                    # Этот параметр нужен офферам для вирт карточек, сейчас в лайтах он тоже выдается
                                    # MARKETOUT-38207
                                    "areValuesEqual": False,
                                    "comparedItems": [
                                        {
                                            "entity": "product",
                                            "id": "1581",
                                            "values": [{"id": "16695478", "value": "VALUE-16695478"}],
                                        },
                                        {"entity": "sku", "id": "33253", "values": []},
                                    ],
                                    "id": "16695477",
                                },
                                {
                                    "areValuesEqual": False,
                                    "comparedItems": [
                                        {
                                            "entity": "product",
                                            "id": "1581",
                                            "values": [{"max": "103", "min": "103"}],
                                        },
                                        {"entity": "sku", "id": "33253", "values": [{"max": "101", "min": "101"}]},
                                    ],
                                    "id": "1101",
                                    "name": "Год основания",
                                },
                                {
                                    "areValuesEqual": False,
                                    "comparedItems": [
                                        {
                                            "entity": "product",
                                            "id": "1581",
                                            "values": [{"id": "13", "value": "Ард Каррайг"}],
                                        },
                                        {
                                            "entity": "sku",
                                            "id": "33253",
                                            "values": [{"id": "12", "value": "Венгерберг"}],
                                        },
                                    ],
                                    "id": "1102",
                                    "name": "Столица",
                                },
                            ],
                        }
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # Запрашиваем 1 БК hyperId + 1 обычный hyperId + 1 БК мску + 1 обычный мску + 1 вирт модель + 1 вирт скю
        # Важно, что порядок в comparedIds следующий: сначала все hyperid в том же порядке, что в запросе затем аналогично msku
        request = (
            'place=compare_products&market-sku={}&market-sku={}&market-sku={}&hyperid={}&hyperid={}&hyperid={}&hid=1252'.format(
                1581, 33253, 2000000000010, 1580, 11252, 2000000000011
            )
            + flags
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "comparedParameters": {
                    "comparedIds": [
                        {"entity": "product", "id": "1580"},
                        {"entity": "product", "id": "11252"},
                        {"entity": "product", "id": "2000000000011"},
                        {"entity": "product", "id": "1581"},
                        {"entity": "sku", "id": "33253"},
                        {"entity": "product", "id": "2000000000010"},
                    ],
                    "groups": [
                        {
                            "areValuesEqual": False,
                            "params": [
                                {
                                    "areValuesEqual": False,
                                    "comparedItems": [
                                        {"entity": "product", "id": "1580", "values": [{"value": "0"}]},
                                        {"entity": "product", "id": "11252", "values": [{"value": "1"}]},
                                        {"entity": "product", "id": "2000000000011", "values": [{"value": "1"}]},
                                        {"entity": "product", "id": "1581", "values": [{"value": "1"}]},
                                        {"entity": "sku", "id": "33253", "values": [{"value": "1"}]},
                                        {"entity": "product", "id": "2000000000010", "values": [{"value": "0"}]},
                                    ],
                                    "id": "1100",
                                    "name": "Захвачена ли Нильфгаардом",
                                },
                                {
                                    "areValuesEqual": False,
                                    "comparedItems": [
                                        {
                                            "entity": "product",
                                            "id": "1580",
                                            "values": [{"id": "16695478", "value": "VALUE-16695478"}],
                                        },
                                        {"entity": "product", "id": "11252", "values": []},
                                        {
                                            "entity": "product",
                                            "id": "2000000000011",
                                            "values": [{"id": "16695478", "value": "VALUE-16695478"}],
                                        },
                                        {
                                            "entity": "product",
                                            "id": "1581",
                                            "values": [{"id": "16695478", "value": "VALUE-16695478"}],
                                        },
                                        {"entity": "sku", "id": "33253", "values": []},
                                        {
                                            "entity": "product",
                                            "id": "2000000000010",
                                            "values": [{"id": "16695478", "value": "VALUE-16695478"}],
                                        },
                                    ],
                                    "id": "16695477",
                                    "name": "GLPARAM-16695477",
                                },
                                {
                                    "areValuesEqual": False,
                                    "comparedItems": [
                                        {
                                            "entity": "product",
                                            "id": "1580",
                                            "values": [{"max": "101", "min": "101"}],
                                        },
                                        {"entity": "product", "id": "11252", "values": [{"max": "100", "min": "100"}]},
                                        {
                                            "entity": "product",
                                            "id": "2000000000011",
                                            "values": [{"max": "103", "min": "103"}],
                                        },
                                        {
                                            "entity": "product",
                                            "id": "1581",
                                            "values": [{"max": "103", "min": "103"}],
                                        },
                                        {"entity": "sku", "id": "33253", "values": [{"max": "101", "min": "101"}]},
                                        {
                                            "entity": "product",
                                            "id": "2000000000010",
                                            "values": [{"max": "102", "min": "102"}],
                                        },
                                    ],
                                    "id": "1101",
                                    "name": "Год основания",
                                },
                                {
                                    "areValuesEqual": False,
                                    "comparedItems": [
                                        {
                                            "entity": "product",
                                            "id": "1580",
                                            "values": [{"id": "11", "value": "Третогор"}],
                                        },
                                        {
                                            "entity": "product",
                                            "id": "11252",
                                            "values": [{"id": "10", "value": "Вызима"}],
                                        },
                                        {
                                            "entity": "product",
                                            "id": "2000000000011",
                                            "values": [{"id": "13", "value": "Ард Каррайг"}],
                                        },
                                        {
                                            "entity": "product",
                                            "id": "1581",
                                            "values": [{"id": "13", "value": "Ард Каррайг"}],
                                        },
                                        {
                                            "entity": "sku",
                                            "id": "33253",
                                            "values": [{"id": "12", "value": "Венгерберг"}],
                                        },
                                        {
                                            "entity": "product",
                                            "id": "2000000000010",
                                            "values": [{"id": "11", "value": "Третогор"}],
                                        },
                                    ],
                                    "id": "1102",
                                    "name": "Столица",
                                },
                            ],
                        }
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
