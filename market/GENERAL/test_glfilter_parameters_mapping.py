#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main
from core.matcher import Contains
from core.types import GLParam, GLType, Offer, OptionMigration, ParameterMigration, TovarCategory


class T(TestCase):
    @classmethod
    def prepare(cls):

        cls.index.gltypes += [
            GLType(param_id=100, hid=1, gltype=GLType.ENUM, hidden=False, values=[1, 2]),
            GLType(param_id=1000, hid=1, gltype=GLType.ENUM, hidden=False, values=[11, 22]),
            GLType(param_id=200, hid=2, gltype=GLType.ENUM, hidden=False, values=[40, 50]),
            GLType(param_id=2000, hid=2, gltype=GLType.ENUM, hidden=False, values=[40, 50]),
        ]
        cls.index.offers += [
            Offer(hid=1, title='iphone', glparams=[GLParam(param_id=100, value=1), GLParam(param_id=100, value=2)]),
            Offer(
                hid=1,
                title='iphone_remapped',
                glparams=[GLParam(param_id=1000, value=11), GLParam(param_id=1000, value=22)],
            ),
            Offer(hid=2, title='iphone_2', glparams=[GLParam(param_id=2000, value=40)]),
            Offer(hid=2, title='iphone_2_remapped_parameter', glparams=[GLParam(param_id=2000, value=40)]),
        ]

        cls.index.parameters_mapping += [
            TovarCategory(
                hid=1,
                parameters_mapping=[
                    ParameterMigration(
                        source_param_id=100,
                        target_param_id=1000,
                        options_migration=[
                            OptionMigration(source_option_id=1, target_option_id=11),
                            OptionMigration(source_option_id=2, target_option_id=22),
                        ],
                    ),
                ],
            ),
            TovarCategory(
                hid=2,
                parameters_mapping=[
                    ParameterMigration(source_param_id=200, target_param_id=2000),
                ],
            ),
        ]

    def test_parameters_mapping_on_off(self):
        # проверяем что при включенном ремапинге выбирается только офер, с измененными параметрами фильтров,
        # при отключенной - только тот, где параметры не менялись
        response = self.report.request_json('place=prime&hid=1&debug=1&glfilter=100:1')
        self.assertFragmentIn(
            response, {"results": [{"titles": {"raw": "iphone_remapped"}}]}, allow_different_len=False
        )

        self.assertFragmentIn(
            response,
            {
                'logicTrace': [
                    Contains("Parameter remapping [100 -> 1000] applied, category: 1"),
                    Contains("Option remapping [1 -> 11] applied, category: 1, parameter: 1000"),
                ]
            },
        )

        response = self.report.request_json(
            'place=prime&hid=1&debug=1&glfilter=100:1&rearr-factors=market_glfilter_parameters_mapping_enabled=0'
        )
        self.assertFragmentIn(response, {"results": [{"titles": {"raw": "iphone"}}]}, allow_different_len=False)

    def test_parameters_mapping_with_options(self):
        # проверяем что ремапинг происходит для всех фильтров
        response = self.report.request_json('place=prime&hid=1&debug=1&glfilter=100:1&glfilter=100:2')
        self.assertFragmentIn(
            response,
            {
                'logicTrace': [
                    Contains("Parameter remapping [100 -> 1000] applied, category: 1"),
                    Contains("Option remapping [1 -> 11] applied, category: 1, parameter: 1000"),
                    Contains("Parameter remapping [100 -> 1000] applied, category: 1"),
                    Contains("Option remapping [2 -> 22] applied, category: 1, parameter: 1000"),
                ]
            },
        )

    def test_parameters_mapping_no_option(self):
        # ремаппинг только для параметра, значение опции прежнее
        response = self.report.request_json('place=prime&hid=2&debug=1&glfilter=200:40')
        self.assertFragmentIn(
            response,
            {
                'logicTrace': [
                    Contains("Parameter remapping [200 -> 2000] applied, category: 2"),
                ]
            },
        )
        self.assertFragmentIn(
            response,
            {"results": [{"titles": {"raw": "iphone_2_remapped_parameter"}}, {"titles": {"raw": "iphone_2"}}]},
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
