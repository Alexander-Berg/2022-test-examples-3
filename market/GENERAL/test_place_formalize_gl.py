#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import NotEmpty
from core.types import (
    ConsequentParam,
    DependentParamValue,
    FormalizedParam,
    GLType,
    HyperCategory,
    HyperCategoryType,
    MainParamValue,
)
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare_get_formalized_gl_filters(cls):
        cls.index.hypertree += [
            HyperCategory(hid=1, output_type=HyperCategoryType.GURU),
        ]

        cls.index.gltypes += [
            # 1 kind params
            GLType(param_id=11, hid=1, gltype=GLType.ENUM, values=[1, 3], cluster_filter=False),
            GLType(param_id=12, hid=1, gltype=GLType.NUMERIC, cluster_filter=False),
            GLType(param_id=13, hid=1, gltype=GLType.BOOL, cluster_filter=False),
            # 2 kind params
            GLType(param_id=14, hid=1, gltype=GLType.ENUM, values=[1, 2, 3, 4], cluster_filter=True),
            # color
            GLType(param_id=16, hid=1, gltype=GLType.ENUM, values=[1, 2, 3]),
        ]

        cls.formalizer.on_request(hid=1, query="some query", return_glued_params=False).respond(
            formalized_params=[
                FormalizedParam(param_id=11, value=1, is_numeric=False),
                FormalizedParam(param_id=11, value=3, is_numeric=False),
                FormalizedParam(param_id=12, value=5, is_numeric=True),
                FormalizedParam(param_id=13, value=1, is_numeric=False),
                FormalizedParam(param_id=14, value=4, is_numeric=False),
                # Должен проигнорироваться, так как отсутствует в фильтрах репорта
                FormalizedParam(param_id=15, value=1, is_numeric=False),
                FormalizedParam(param_id=16, value=1, is_numeric=False, param_xsl_name='color_glob', rule_id=100500),
                # Парный consequent-параметр для param_id=16
                # При формализации цветов, он присутствует в выдаче формализатора всегда
                FormalizedParam(param_id=17, value=234, is_numeric=False, param_xsl_name='color_vendor'),
            ],
            consequent_params=[
                ConsequentParam(
                    main_param_id=17,
                    main_values=[
                        MainParamValue(
                            main_param_value_id=234,
                            dependent_param_values=[DependentParamValue(param_id=16, value_ids=[1])],
                        )
                    ],
                )
            ],
        )

    def test_get_formalized_gl_filters(self):
        """
        Делаем запрос на формализацию фильтров.
        Формализатор возвращает параметры:
        id=11 type=enum values=[1,3]
        id=12 type=num  values=5
        id=13 type=bool values=true
        id=14 type=enum values=[4]
        id=15 type=enum values=[1]
        id=16 type=enum values=[1]
        id=17 type=enum values=[234]

        Плейс должен вернуть параметры
        11, 12, 13 - потому что мы должны возращать параметры 1го рода
        14 - потому что мы должны возращать параметры 2го рода

        Плейс не должен вернуть параметры 15, 17, потому что их нет в gl_mbo.pbuf.sn
        """
        response = self.report.request_json('place=formalize_gl&text=some+query&hid=1&debug=da')
        self.validateResponse(response)

    @classmethod
    def prepare_no_error_on_timeout(cls):
        cls.formalizer.on_request(hid=1, query='timeouted query', return_glued_params=False).return_code(418)
        cls.formalizer.on_request(hid=1, query='server error query', return_glued_params=False).return_code(500)

    def test_no_error_on_timeout(self):
        """
        Проверяем отсутствие ошибки при таймауте
        """
        self.report.request_json('place=formalize_gl&text=timeouted+query&hid=1')
        self.report.request_json('place=formalize_gl&text=server+error+query&hid=1')

    def validateResponse(self, response):
        self.assertFragmentIn(
            response,
            {
                "filters": {
                    "11": {
                        "type": "enum",
                        "values": [
                            {"id": 1, "num": 0},
                            {"id": 3, "num": 0},
                        ],
                    },
                    "12": {
                        "type": "number",
                        "values": [
                            {"id": 0, "num": 5},
                        ],
                    },
                    "13": {
                        "type": "boolean",
                        "values": [
                            {"id": 1, "num": 0},
                        ],
                    },
                    "14": {
                        "type": "enum",
                        "values": [
                            {"id": 4, "num": 0},
                        ],
                    },
                }
            },
        )
        self.assertFragmentNotIn(response, {"filters": {"15": NotEmpty()}})
        self.assertFragmentNotIn(response, {"filters": {"17": NotEmpty()}})

        # check debug output
        self.assertFragmentIn(response, {"debug": {"report": {"logicTrace": NotEmpty()}}})


if __name__ == '__main__':
    main()
