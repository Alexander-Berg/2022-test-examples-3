#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.matcher import NotEmpty, Regex, Absent, Contains
from core.types import (
    GLParam,
    GLType,
    HyperCategory,
    ParametrizatorParam,
    Offer,
)
from core.report import DefaultFlags
from core.testcase import TestCase, main


ParamType = ParametrizatorParam.ParamType


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.hypertree += [
            HyperCategory(hid=1),
        ]

        cls.index.gltypes += [
            GLType(param_id=1, hid=1, gltype=GLType.ENUM, values=[1, 2]),
            GLType(param_id=2, hid=1, gltype=GLType.NUMERIC),
            GLType(param_id=3, hid=1, gltype=GLType.NUMERIC),
        ]

        cls.index.offers += [
            Offer(
                title="red iphone 5 16 GB pretty",
                hid=1,
                glparams=[GLParam(param_id=1, value=2), GLParam(param_id=2, value=5), GLParam(param_id=3, value=16)],
            ),
        ]

        cls.parametrizator.on_request(
            hid=1, query="red iphone 5 16 GB pretty", rearr_factors="market_parametrizator=1"
        ).respond(
            parametrizator_params=[
                ParametrizatorParam(param_id=1, value=2, param_type=ParamType.ENUM, value_positions=(0, 3)),
                ParametrizatorParam(param_id=2, value=5, param_type=ParamType.NUMBER, value_positions=(11, 12)),
                ParametrizatorParam(
                    param_id=3, value=16, param_type=ParamType.NUMBER, value_positions=(13, 15), unit_positions=(16, 18)
                ),
            ]
        )
        cls.parametrizator.on_request(hid=1, query="red iphone 5").respond(
            parametrizator_params=[
                ParametrizatorParam(param_id=1, value=2, param_type=ParamType.ENUM, value_positions=(0, 3)),
            ]
        )
        cls.parametrizator.on_request(
            hid=1, query="iphone 5", rearr_factors="market_parametrizator=1;market_use_formalizer_for_search=1"
        ).respond(
            parametrizator_params=[
                ParametrizatorParam(param_id=2, value=5, param_type=ParamType.NUMBER, value_positions=(7, 8)),
            ]
        )

        cls.parametrizator.on_default_request().respond()

    def test_parametrizator(self):
        """Проверяем, что под флагом market_parametrizator=1 репорт ходит в параметризатор
        https://st.yandex-team.ru/MARKETOUT-47137
        """
        text = 'red iphone 5 16 GB pretty'
        rearr = '&rearr-factors=market_parametrizator=1'

        response = self.report.request_json(
            'place=prime&text={}&cvredirect=1&debug=1'.format(text) + rearr, add_defaults=DefaultFlags.NONE
        )

        glfilters = ["1:2", "2:5,5", "3:16,16"]
        hid = "1"

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["11"],
                        "rs": [NotEmpty()],
                        "hid": [hid],
                        "text": [text],
                        "glfilter": glfilters,
                    },
                }
            },
        )

        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Regex(
                        r"\[ME\].* FormalizeQuery\(\): Making POST-request to parametrizator. Query: \[red iphone 5 16 GB pretty\], hid: 1, rearrFactors: market_parametrizator=1"
                    ),
                    Regex(
                        r"\[ME\].* FormalizeQuery\(\): Parametrizator response: "
                        r"gl_param_values {\n  param {\n    param_id: 1\n    param_type: 3\n  }\n  values {\n    value_id: 2\n    value_num: 0\n    "
                        r"value_index_range {\n      begin: 0\n      end: 3\n    }\n    param_index_range {\n      begin: 0\n      end: 0\n    }\n    "
                        r"unit_index_range {\n      begin: 0\n      end: 0\n    }\n  }\n}\n"
                        r"gl_param_values {\n  param {\n    param_id: 2\n    param_type: 2\n  }\n  values {\n    value_id: 0\n    value_num: 5\n    "
                        r"value_index_range {\n      begin: 11\n      end: 12\n    }\n    param_index_range {\n      begin: 0\n      end: 0\n    }\n    "
                        r"unit_index_range {\n      begin: 0\n      end: 0\n    }\n  }\n}\n"
                        r"gl_param_values {\n  param {\n    param_id: 3\n    param_type: 2\n  }\n  values {\n    value_id: 0\n    value_num: 16\n    "
                        r"value_index_range {\n      begin: 13\n      end: 15\n    }\n    param_index_range {\n      begin: 0\n      end: 0\n    }\n    "
                        r"unit_index_range {\n      begin: 16\n      end: 18\n    }\n  }\n}\n"
                    ),
                ]
            },
        )

        # Без флага market_parametrizator=1 запроса в параметризатор нет
        response = self.report.request_json(
            'place=prime&text={}&cvredirect=1&debug=1'.format(text), add_defaults=DefaultFlags.NONE
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["9"],
                        "rs": [NotEmpty()],
                        "hid": [hid],
                        "text": [text],
                        "glfilter": Absent(),
                    },
                }
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                "logicTrace": [
                    Regex(
                        r"\[ME\].* FormalizeQuery\(\): Making POST-request to parametrizator. Query: \[red iphone 5 16 GB pretty\], hid: 1, rearrFactors: "
                    ),
                    Regex(
                        r"\[ME\].* FormalizeQuery\(\): Parametrizator response: "
                        r"gl_param_values {\n  param {\n    param_id: 1\n    param_type: 3\n  }\n  values {\n    value_id: 2\n    value_num: 0\n    "
                        r"value_index_range {\n      begin: 0\n      end: 3\n    }\n    param_index_range {\n      begin: 0\n      end: 0\n    }\n    "
                        r"unit_index_range {\n      begin: 0\n      end: 0\n    }\n  }\n}\n"
                        r"gl_param_values {\n  param {\n    param_id: 2\n    param_type: 2\n  }\n  values {\n    value_id: 0\n    value_num: 5\n    "
                        r"value_index_range {\n      begin: 11\n      end: 12\n    }\n    param_index_range {\n      begin: 0\n      end: 0\n    }\n    "
                        r"unit_index_range {\n      begin: 0\n      end: 0\n    }\n  }\n}\n"
                        r"gl_param_values {\n  param {\n    param_id: 3\n    param_type: 2\n  }\n  values {\n    value_id: 0\n    value_num: 16\n    "
                        r"value_index_range {\n      begin: 13\n      end: 15\n    }\n    param_index_range {\n      begin: 0\n      end: 0\n    }\n    "
                        r"unit_index_range {\n      begin: 16\n      end: 18\n    }\n  }\n}\n"
                    ),
                ]
            },
        )

    def test_cache_requests_to_parametrizator(self):
        """Проверяем, что под флагом market_parametrizator=1 репорт ходит в параметризатор
        https://st.yandex-team.ru/MARKETOUT-47137
        """
        text = 'red iphone 5'
        rearr = '&rearr-factors=market_parametrizator=1'

        # первым запросом кладём в кеш
        response = self.report.request_json('place=prime&text={}&cvredirect=1&debug=1'.format(text) + rearr)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Regex(r"\[ME\].* Get\(\): Local cache miss for parametrizator, key.*"),
                    Regex(r"\[ME\].*, Set\(\): Saved to local cache for parametrizator.*"),
                ]
            },
        )

        # вторым запросом достаём из кеша
        response = self.report.request_json('place=prime&text={}&cvredirect=1&debug=1'.format(text) + rearr)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Regex(r"\[ME\].* Get\(\): Local cache hit for parametrizator, key.*"),
                ]
            },
        )

    def test_rearr_factors_send_to_parametrizator(self):
        """Проверяем, что все rearr-factors отправляются в параметризатор"""
        text = 'iphone 5'
        rearr = '&rearr-factors=market_parametrizator=1;market_use_formalizer_for_search=1'

        response = self.report.request_json(
            "place=prime&cvredirect=1&text={text}{rearr}&debug=da".format(text=text, rearr=rearr),
            add_defaults=DefaultFlags.NONE,
        )
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(
                        "FormalizeQuery(): Making POST-request to parametrizator. Query: [iphone 5], hid: 1, rearrFactors: market_parametrizator=1;market_use_formalizer_for_search=1"
                    )
                ]
            },
        )


if __name__ == '__main__':
    main()
