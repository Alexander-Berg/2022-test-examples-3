#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    GLParam,
    GLType,
    Model,
    NavCategory,
    NavFiltersPreset,
    NavFilterCondition,
    HyperCategory,
)
from core.matcher import NoKey


class T(TestCase):
    @classmethod
    def prepare(cls):
        pass

    @classmethod
    def prepare_hide_recipe_filters(cls):
        cls.index.navtree += [
            NavCategory(
                hid=31,
                nid=41,
                name="NavCategory 41",
                is_blue=False,
                filters_presets=[
                    NavFiltersPreset(
                        id=1,
                        name="Пресет фильтров рут (2)",
                        position=1,
                        filters=[
                            NavFilterCondition(
                                filter_type=NavFilterCondition.ENUM, param_id=502, enum_values=[1, 2, 3]
                            ),
                        ],
                    ),
                    NavFiltersPreset(
                        id=2,
                        name="Пресет фильтров one undefined gl param",
                        position=2,
                        filters=[
                            NavFilterCondition(filter_type=NavFilterCondition.ENUM, param_id=502, enum_values=[4]),
                            NavFilterCondition(filter_type=NavFilterCondition.BOOLEAN, param_id=501, bool_value=True),
                        ],
                    ),
                    NavFiltersPreset(
                        id=3,
                        name="Пресет фильтров valid gl param",
                        position=2,
                        filters=[
                            NavFilterCondition(filter_type=NavFilterCondition.ENUM, param_id=502, enum_values=[4]),
                        ],
                    ),
                ],
                children=[
                    NavCategory(
                        hid=51,
                        nid=61,
                        name="NavCategory 61",
                        is_blue=False,
                        filters_presets=[
                            NavFiltersPreset(
                                id=1,
                                name="Пресет фильтров",
                                position=2,
                                filters=[
                                    NavFilterCondition(
                                        filter_type=NavFilterCondition.BOOLEAN, param_id=501, bool_value=True
                                    ),
                                    NavFilterCondition(
                                        filter_type=NavFilterCondition.ENUM, param_id=502, enum_values=[3, 6]
                                    ),
                                    NavFilterCondition(
                                        filter_type=NavFilterCondition.ENUM, param_id=503, enum_values=[12]
                                    ),
                                    NavFilterCondition(
                                        filter_type=NavFilterCondition.NUMERIC, param_id=504, max_value=100
                                    ),
                                    NavFilterCondition(
                                        filter_type=NavFilterCondition.NUMERIC, param_id=505, min_value=55, max_value=55
                                    ),
                                ],
                            ),
                            NavFiltersPreset(
                                id=2,
                                name="Пресет фильтров 2",
                                position=3,
                                filters=[
                                    NavFilterCondition(
                                        filter_type=NavFilterCondition.ENUM, param_id=502, enum_values=[1, 2, 3]
                                    ),
                                ],
                            ),
                            NavFiltersPreset(
                                id=3,
                                name="Пресет фильтров 3",
                                position=1,
                                filters=[
                                    NavFilterCondition(
                                        filter_type=NavFilterCondition.ENUM, param_id=502, enum_values=[7, 8]
                                    ),
                                ],
                            ),
                        ],
                    ),
                    NavCategory(
                        hid=52,
                        nid=62,
                        name="NavCategory 62",
                        is_blue=False,
                        filters_presets=[
                            NavFiltersPreset(
                                id=42,
                                name="Пресет фильтров гипер",
                                position=1,
                                filters=[
                                    NavFilterCondition(
                                        filter_type=NavFilterCondition.ENUM, param_id=503, enum_values=[11, 12]
                                    ),
                                ],
                            ),
                        ],
                        children=[
                            NavCategory(
                                hid=71,
                                nid=81,
                                name="NavCategory 81",
                                is_blue=False,
                            )
                        ],
                    ),
                ],
            ),
        ]

        cls.index.hypertree += [HyperCategory(hid=71, children=[HyperCategory(hid=91)])]

        cls.index.gltypes += [
            GLType(param_id=501, hid=51, cluster_filter=False, gltype=GLType.BOOL),
            GLType(param_id=502, hid=51, gltype=GLType.ENUM, values=list(range(10))),
            GLType(param_id=503, hid=51, gltype=GLType.ENUM, values=list(range(11, 15))),
            GLType(param_id=504, hid=51, gltype=GLType.NUMERIC),
            GLType(param_id=505, hid=51, gltype=GLType.NUMERIC),
            GLType(param_id=502, hid=71, gltype=GLType.ENUM, values=list(range(10))),
            GLType(param_id=502, hid=31, gltype=GLType.ENUM, values=list(range(10))),
            GLType(param_id=503, hid=91, gltype=GLType.ENUM, values=list(range(11, 15))),
            GLType(param_id=503, hid=52, gltype=GLType.ENUM, values=list(range(11, 15))),
        ]

        cls.index.models += [
            Model(
                title="dress",
                hid=51,
                glparams=[
                    GLParam(param_id=501, value=1),
                    GLParam(param_id=502, value=6),
                    GLParam(param_id=503, value=12),
                    GLParam(param_id=504, value=35),
                    GLParam(param_id=505, value=55),
                ],
            ),
            Model(
                title="dress502-1",
                hid=51,
                glparams=[
                    GLParam(param_id=501, value=1),
                    GLParam(param_id=502, value=1),
                ],
            ),
            Model(
                title="socks502-4",
                hid=31,
                glparams=[
                    GLParam(param_id=502, value=4),
                ],
            ),
            Model(
                title="dress502-3",
                hid=51,
                glparams=[
                    GLParam(param_id=501, value=1),
                    GLParam(param_id=502, value=3),
                ],
            ),
            Model(
                title="dress502-2",
                hid=51,
                glparams=[
                    GLParam(param_id=501, value=0),
                    GLParam(param_id=502, value=2),
                ],
            ),
            Model(
                title="dress502-7",
                hid=51,
                glparams=[
                    GLParam(param_id=501, value=0),
                    GLParam(param_id=502, value=7),
                ],
            ),
            Model(
                title="dress502-8",
                hid=51,
                glparams=[
                    GLParam(param_id=501, value=0),
                    GLParam(param_id=502, value=8),
                ],
            ),
            Model(
                title="dress502-deep",
                hid=71,
                glparams=[
                    GLParam(param_id=502, value=1),
                ],
            ),
            Model(
                title="dress502-hyperchild",
                hid=91,
                glparams=[
                    GLParam(param_id=503, value=11),
                ],
            ),
        ]

    def test_filters_presets_render(self):
        """
        Должны скрываться рецептные фильтры типов ENUM и NUMERIC, имеющие одно значение
        """
        response = self.report.request_json('place=prime&nid=61')
        self.assertFragmentIn(
            response,
            {"filters_presets": NoKey("filters_presets")},
            allow_different_len=False,
        )

        response = self.report.request_json('place=prime&nid=61&rearr-factors=market_report_use_filters_presets=1')
        self.assertFragmentIn(
            response,
            {
                "filters_presets": [
                    {
                        "id": 3,
                        "initialFound": 2,
                        "found": 2,
                        "name": "Пресет фильтров 3",
                        "position": 1,
                        "filters": ["502:7,8"],
                    },
                    {
                        "id": 1,
                        "name": "Пресет фильтров",
                        "checked": False,
                        "initialFound": 1,
                        "found": 1,
                        "position": 2,
                        "filters": ["501:1", "502:3,6", "503:12", "504:~100", "505:55~55"],
                    },
                    {
                        "id": 2,
                        "initialFound": 3,
                        "found": 3,
                        "name": "Пресет фильтров 2",
                        "position": 3,
                        "filters": ["502:1,2,3"],
                    },
                ],
            },
            allow_different_len=False,
        )

    def test_filters_presets_filtered_by_bool(self):
        """
        Применим булевый фильтр, один Пресет фильтров должен засериться
        """
        response = self.report.request_json(
            'place=prime&nid=61&rearr-factors=market_report_use_filters_presets=1&glfilter=501:1'
        )
        self.assertFragmentIn(
            response,
            {
                "filters_presets": [
                    {
                        "id": 3,
                        "initialFound": 2,
                        "found": 0,
                    },
                    {
                        "id": 1,
                        "initialFound": 2,
                        "found": 2,
                    },
                    {
                        "id": 2,
                        "initialFound": 5,  # Из-за дозапроса фильтров статистики суммируются, какой-то костыль, но править дорого и опасно - MARKETOUT-33903
                        "found": 4,
                    },
                ],
            },
            allow_different_len=False,
        )

    def test_filters_presets_disable_all(self):
        """
        Применим енам фильтр исключающий все модели
        """
        response = self.report.request_json(
            'place=prime&nid=61&rearr-factors=market_report_use_filters_presets=1&glfilter=503:14'
        )
        self.assertFragmentIn(
            response,
            {
                "filters_presets": [
                    {
                        "id": 3,
                        "initialFound": 2,
                        "found": 0,
                    },
                    {
                        "id": 1,
                        "initialFound": 1,
                        "found": 0,
                    },
                    {
                        "id": 2,
                        "initialFound": 3,
                        "found": 0,
                    },
                ],
            },
            allow_different_len=False,
        )

    def test_filters_presets_in_root(self):
        """
        В руте есть Пресет фильтров, статистики должны считатсья для всех дочерних в том числе
        """
        response = self.report.request_json(
            'place=prime&nid=41&rearr-factors=market_report_use_filters_presets=1&glfilter=502:1'
        )
        self.assertFragmentIn(
            response,
            {
                "filters_presets": [
                    {
                        "id": 1,
                        "initialFound": 6,
                        "found": 4,
                    },
                    {
                        "id": 3,
                        "initialFound": 1,
                        "found": 0,
                    },
                ],
            },
            allow_different_len=False,
        )

    def test_filters_presets_hyper_child(self):
        """
        Товар из подкатегории ребенка должен быть учтен
        """
        response = self.report.request_json('place=prime&nid=62&rearr-factors=market_report_use_filters_presets=1')
        self.assertFragmentIn(
            response,
            {
                "filters_presets": [
                    {
                        "id": 42,
                        "initialFound": 1,
                        "found": 1,
                    },
                ],
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=prime&nid=62&rearr-factors=market_report_use_filters_presets=1&glfilter=503:13'
        )
        self.assertFragmentIn(
            response,
            {
                "filters_presets": [
                    {
                        "id": 42,
                        "initialFound": 1,
                        "found": 0,
                    },
                ],
            },
            allow_different_len=False,
        )

    def test_filters_presets_checked(self):
        """
        Проверяем что поле checked проставляется при передаче быстрого фильтра в параметре
        """

        request = 'place=prime&nid=62&rearr-factors=market_report_use_filters_presets=1'

        def validate(response, checked):
            self.assertFragmentIn(
                response,
                {
                    "filters_presets": [
                        {
                            "id": 42,
                            "checked": checked,
                        },
                    ],
                },
                allow_different_len=False,
            )

        validate(self.report.request_json(request), False)

        validate(self.report.request_json(request + '&glfilter=503:11,12&filters-preset-id=42'), True)

        validate(self.report.request_json(request + '&glfilter=503:12,11&filters-preset-id=42'), True)

        validate(self.report.request_json(request + '&glfilter=503:11,12,13&filters-preset-id=42'), False)

        validate(self.report.request_json(request + '&glfilter=503:11,12'), False)


if __name__ == '__main__':
    main()
