#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    GLParam,
    GLValue,
    GLType,
    HyperCategory,
    Model,
    Const,
    Offer,
)

from core.matcher import (
    EmptyList,
)


from core.testcase import (
    TestCase,
    main,
)

virtual_model_id_range_start = int(2 * 1e12)


class HyperCategoriesData:
    ROOT = 10
    DEPARTMENT_WITH_PARAM = 1010
    CATEGORY_WITH_PARAM = 101010
    CATEGORY_WITH_HIDDEN_PARAM = 101020

    DEPARTMENT_WITHOUT_PARAM = 1020
    CATEGORY_WITHOUT_PARAM_DEPARTMENT_WITH_PARAM = 102010
    CATEGORY_WITHOUT_PARAM_DEPARTMENT_NO_PARAM = 102020

    DEPARTMENT_WITH_PARAM_IN_MID = 1030
    CATEGORY_WITH_PARAM_IN_MID_MID = 103010
    CATEGORY_WITH_PARAM_IN_MID_LST = 10301010

    categories = [
        HyperCategory(
            hid=DEPARTMENT_WITH_PARAM,
            children=[
                HyperCategory(
                    hid=CATEGORY_WITH_HIDDEN_PARAM,
                ),
                HyperCategory(
                    hid=CATEGORY_WITH_PARAM,
                ),
            ],
        ),
        HyperCategory(
            hid=DEPARTMENT_WITHOUT_PARAM,
            children=[
                HyperCategory(
                    hid=CATEGORY_WITHOUT_PARAM_DEPARTMENT_WITH_PARAM,
                ),
                HyperCategory(
                    hid=CATEGORY_WITHOUT_PARAM_DEPARTMENT_NO_PARAM,
                ),
            ],
        ),
        HyperCategory(
            hid=DEPARTMENT_WITH_PARAM_IN_MID,
            children=[
                HyperCategory(
                    hid=CATEGORY_WITH_PARAM_IN_MID_MID,
                    children=[
                        HyperCategory(
                            hid=CATEGORY_WITH_PARAM_IN_MID_LST,
                        ),
                    ],
                ),
            ],
        ),
    ]


class GlParamData:
    PARAM_ID = 12782797
    VALUE_ONE_ID = 1
    VALUE_TWO_ID = 2

    XSL_NAME = "vendor_line"

    SOME_PARAM_ID = 12782798
    LIST_XSL_NAME = "not_line"

    values = [
        GLValue(value_id=VALUE_ONE_ID, text="first"),
        GLValue(value_id=VALUE_TWO_ID, text="second"),
    ]

    gltypes = [
        GLType(
            param_id=PARAM_ID,
            hid=HyperCategoriesData.DEPARTMENT_WITH_PARAM,
            gltype=GLType.ENUM,
            values=values,
            short_enum_sort_type=GLType.EnumFieldSortingType.ALPHABETICAL,
            xslname=XSL_NAME,
        ),
        GLType(
            param_id=PARAM_ID,
            hid=HyperCategoriesData.CATEGORY_WITH_PARAM,
            gltype=GLType.ENUM,
            values=values,
            short_enum_sort_type=GLType.EnumFieldSortingType.ALPHABETICAL,
            xslname=XSL_NAME,
        ),
        GLType(
            param_id=PARAM_ID,
            hid=HyperCategoriesData.CATEGORY_WITH_HIDDEN_PARAM,
            gltype=GLType.ENUM,
            values=values,
            hidden=True,
            short_enum_sort_type=GLType.EnumFieldSortingType.ALPHABETICAL,
            xslname=XSL_NAME,
        ),
        GLType(
            param_id=PARAM_ID,
            hid=HyperCategoriesData.CATEGORY_WITHOUT_PARAM_DEPARTMENT_WITH_PARAM,
            gltype=GLType.ENUM,
            values=values,
            short_enum_sort_type=GLType.EnumFieldSortingType.ALPHABETICAL,
            xslname=XSL_NAME,
        ),
        GLType(
            param_id=PARAM_ID,
            hid=HyperCategoriesData.CATEGORY_WITH_PARAM_IN_MID_MID,
            gltype=GLType.ENUM,
            values=values,
            short_enum_sort_type=GLType.EnumFieldSortingType.ALPHABETICAL,
            xslname=XSL_NAME,
        ),
        GLType(
            param_id=PARAM_ID,
            hid=HyperCategoriesData.CATEGORY_WITH_PARAM_IN_MID_LST,
            gltype=GLType.ENUM,
            values=values,
            short_enum_sort_type=GLType.EnumFieldSortingType.ALPHABETICAL,
            xslname=XSL_NAME,
        ),
        GLType(
            param_id=SOME_PARAM_ID,
            hid=HyperCategoriesData.CATEGORY_WITHOUT_PARAM_DEPARTMENT_NO_PARAM,
            gltype=GLType.ENUM,
            values=values,
            short_enum_sort_type=GLType.EnumFieldSortingType.ALPHABETICAL,
            xslname=LIST_XSL_NAME,
        ),
        GLType(
            param_id=SOME_PARAM_ID,
            hid=HyperCategoriesData.DEPARTMENT_WITHOUT_PARAM,
            gltype=GLType.ENUM,
            values=values,
            short_enum_sort_type=GLType.EnumFieldSortingType.ALPHABETICAL,
            xslname=LIST_XSL_NAME,
        ),
        GLType(
            param_id=SOME_PARAM_ID,
            hid=Const.ROOT_HID,
            gltype=GLType.ENUM,
            values=values,
            short_enum_sort_type=GLType.EnumFieldSortingType.ALPHABETICAL,
            xslname=LIST_XSL_NAME,
        ),
    ]


def calc_hyperid(hid, value_id, virtual=False):
    return int(str(int(hid) % 29) + str(value_id)) + (virtual_model_id_range_start if virtual else 0)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.hypertree = HyperCategoriesData.categories
        cls.index.gltypes += GlParamData.gltypes
        cls.index.models += [
            Model(
                hyperid=calc_hyperid(hid, value.get_option_id()),
                title="{hid}.{value}".format(hid=hid, value=value.get_option_id()),
                hid=int(hid),
                glparams=[
                    GLParam(param_id=param.id, value=value.get_option_id())
                    for param in GlParamData.gltypes
                    if int(param.hid) == hid
                ],
            )
            # создаём модели в каждой листовой категории
            # со всеми параметрами, с одним значением внутри оффера
            for hid in [
                HyperCategoriesData.CATEGORY_WITH_PARAM,
                HyperCategoriesData.CATEGORY_WITH_HIDDEN_PARAM,
                HyperCategoriesData.CATEGORY_WITHOUT_PARAM_DEPARTMENT_WITH_PARAM,
                HyperCategoriesData.CATEGORY_WITHOUT_PARAM_DEPARTMENT_NO_PARAM,
                HyperCategoriesData.CATEGORY_WITH_PARAM_IN_MID_LST,
            ]
            for value in GlParamData.values
        ]

        cls.index.offers += [
            Offer(
                virtual_model_id=calc_hyperid(hid, value.get_option_id(), True),
                title="{hid}.{value}".format(hid=hid, value=value.get_option_id()),
                hid=int(hid),
                glparams=[
                    GLParam(param_id=param.id, value=value.get_option_id())
                    for param in GlParamData.gltypes
                    if int(param.hid) == hid
                ],
            )
            # создаём модели в каждой листовой категории
            # со всеми параметрами, с одним значением внутри оффера
            for hid in [
                HyperCategoriesData.CATEGORY_WITH_PARAM,
                HyperCategoriesData.CATEGORY_WITH_HIDDEN_PARAM,
                HyperCategoriesData.CATEGORY_WITHOUT_PARAM_DEPARTMENT_WITH_PARAM,
                HyperCategoriesData.CATEGORY_WITHOUT_PARAM_DEPARTMENT_NO_PARAM,
                HyperCategoriesData.CATEGORY_WITH_PARAM_IN_MID_LST,
            ]
            for value in GlParamData.values
        ]

    def test_entity_link(self):
        '''
        Поднимаясь от категории оффера ищем такое максимальное
        поддерево без ROOT категории , где путь от корня этого поддерева до категории
        оффера содержал бы запрошенный параметр в каждом узле.
        Проверяем случай, когда такой путь есть до департамента
        '''
        for virtual in [True, False]:
            request = "place=modelinfo&hyperid={}&rids=0&filter-links={}".format(
                calc_hyperid(HyperCategoriesData.CATEGORY_WITH_PARAM, GlParamData.VALUE_ONE_ID, virtual),
                GlParamData.XSL_NAME,
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "product",
                            "links": [
                                {
                                    "type": "filter",
                                    "xslname": GlParamData.XSL_NAME,
                                    "hid": str(HyperCategoriesData.DEPARTMENT_WITH_PARAM),
                                    "filter": str(GlParamData.PARAM_ID),
                                    "values": [
                                        str(GlParamData.VALUE_ONE_ID),
                                    ],
                                }
                            ],
                        },
                    ]
                },
            )

    def test_entity_list_link(self):
        '''
        Поднимаясь от категории оффера ищем такое максимальное
        поддерево без ROOT категории , где путь от корня этого поддерева до категории
        оффера содержал бы запрошенный параметр в каждом узле.
        Проверяем путь из одной вершины (категории оффера)
        '''
        for virtual in [True, False]:
            request = "place=modelinfo&hyperid={}&rids=0&filter-links={}".format(
                calc_hyperid(
                    HyperCategoriesData.CATEGORY_WITHOUT_PARAM_DEPARTMENT_WITH_PARAM, GlParamData.VALUE_ONE_ID, virtual
                ),
                GlParamData.XSL_NAME,
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "product",
                            "links": [
                                {
                                    "type": "filter",
                                    "xslname": GlParamData.XSL_NAME,
                                    "hid": str(HyperCategoriesData.CATEGORY_WITHOUT_PARAM_DEPARTMENT_WITH_PARAM),
                                    "filter": str(GlParamData.PARAM_ID),
                                    "values": [
                                        str(GlParamData.VALUE_ONE_ID),
                                    ],
                                }
                            ],
                        },
                    ]
                },
            )

    def test_entity_no_link(self):
        '''
        Поднимаясь от категории оффера ищем такое максимальное
        поддерево без ROOT категории , где путь от корня этого поддерева до категории
        оффера содержал бы запрошенный параметр в каждом узле.
        Проверяем отсутсвие параметра у оффера.
        '''
        for virtual in [True, False]:
            request = "place=modelinfo&hyperid={}&rids=0&filter-links={}".format(
                calc_hyperid(
                    HyperCategoriesData.CATEGORY_WITHOUT_PARAM_DEPARTMENT_NO_PARAM, GlParamData.VALUE_ONE_ID, virtual
                ),
                GlParamData.XSL_NAME,
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {"entity": "product", "links": EmptyList()},
                    ]
                },
            )

    def test_entity_mid_link(self):
        '''
        Поднимаясь от категории оффера ищем такое максимальное
        поддерево без ROOT категории , где путь от корня этого поддерева до категории
        оффера содержал бы запрошенный параметр в каждом узле.
        Проверяем ссылку на недепартаментную категорию .
        '''
        for virtual in [True, False]:
            request = "place=modelinfo&hyperid={}&rids=0&filter-links={}".format(
                calc_hyperid(HyperCategoriesData.CATEGORY_WITH_PARAM_IN_MID_LST, GlParamData.VALUE_TWO_ID, virtual),
                GlParamData.XSL_NAME,
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "product",
                            "links": [
                                {
                                    "type": "filter",
                                    "xslname": GlParamData.XSL_NAME,
                                    "hid": str(HyperCategoriesData.CATEGORY_WITH_PARAM_IN_MID_MID),
                                    "filter": str(GlParamData.PARAM_ID),
                                    "values": [
                                        str(GlParamData.VALUE_TWO_ID),
                                    ],
                                }
                            ],
                        },
                    ]
                },
            )

    def test_entity_not_root_link(self):
        '''
        Поднимаясь от категории оффера ищем такое максимальное
        поддерево без ROOT категории , где путь от корня этого поддерева до категории
        оффера содержал бы запрошенный параметр в каждом узле.
        Проверяем, что root не попадает ссылки.
        '''
        for virtual in [True, False]:
            request = "place=modelinfo&hyperid={}&rids=0&filter-links={}".format(
                calc_hyperid(
                    HyperCategoriesData.CATEGORY_WITHOUT_PARAM_DEPARTMENT_NO_PARAM, GlParamData.VALUE_TWO_ID, virtual
                ),
                GlParamData.LIST_XSL_NAME,
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "product",
                            "links": [
                                {
                                    "type": "filter",
                                    "xslname": GlParamData.LIST_XSL_NAME,
                                    "hid": str(HyperCategoriesData.DEPARTMENT_WITHOUT_PARAM),
                                    "filter": str(GlParamData.SOME_PARAM_ID),
                                    "values": [
                                        str(GlParamData.VALUE_TWO_ID),
                                    ],
                                }
                            ],
                        },
                    ]
                },
            )


if __name__ == '__main__':
    main()
