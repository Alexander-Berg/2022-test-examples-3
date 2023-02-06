#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Model
from core.svn_data import SvnData
from core.matcher import NotEmpty
from core.blender_bundles import create_blender_bundles, get_supported_incuts_cgi


class BlenderBundleConstPositon:
    BUNDLE_CONST_SEARCH_POSITION = '''
{{
    "incut_places": ["Search"],
    "incut_positions": [{row_position}],
    "incut_viewtypes": ["Gallery"],
    "incut_ids": ["{incut_id}"],
    "result_scores": [
        {{
            "incut_place": "Search",
            "row_position": {row_position},
            "incut_viewtype": "Gallery",
            "incut_id": "{incut_id}",
            "score": 1.0
        }}
    ],
    "calculator_type": "ConstPosition"
}}
'''


class T(TestCase):
    class CgiParams(dict):
        def raw(self, separator='&'):
            if len(self):
                return separator.join("{}={}".format(str(k), str(v)) for (k, v) in self.items())
            return ""

    class RearrFlags(CgiParams):
        def __init__(self, *args, **kwargs):
            super(T.RearrFlags, self).__init__(*args, **kwargs)

        def raw(self):
            if len(self):
                return 'rearr-factors={}'.format(super(T.RearrFlags, self).raw(';'))
            return str()

    @staticmethod
    def create_request(parameters, rearr):
        return '{}{}'.format(parameters.raw(), '&{}'.format(rearr.raw()) if len(rearr) else '')

    @staticmethod
    def create_blender_request(parameters, rearr={}, supported_incuts={}):
        request_params = {
            "place": "prime",
            "blender": 1,
            "columns-in-grid": 3,
            "supported-incuts": supported_incuts if supported_incuts else get_supported_incuts_cgi(),
        }
        if not rearr:
            rearr = T.RearrFlags({})
        rearr["market_blender_use_bundles_config"] = 1
        if "market_ugc_saas_enabled" not in rearr:
            rearr["market_ugc_saas_enabled"] = 1
        parameters.update(request_params)
        return T.create_request(parameters, rearr)

    @classmethod
    def prepare_blender_bundles_config(cls):
        cls.settings.formulas_path = create_blender_bundles(
            cls.meta_paths.testroot,
            None,
            {
                "bundle_fact_ans.json": BlenderBundleConstPositon.BUNDLE_CONST_SEARCH_POSITION.format(
                    row_position=1, incut_id="fact_ans_incut"
                )
            },
        )

    hidArr = [734595, 91616]

    @classmethod
    def prepare_market_report_data_from_access(cls):
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_svn_data = True
        cls.settings.market_access_settings.use_svn_data = True

    @classmethod
    def setup_market_access_resources(cls, access_server, shade_host_port):
        svn_data = SvnData(access_server=access_server, shade_host_port=shade_host_port, meta_paths=cls.meta_paths)

        svn_data.fact_ans_data_arr += [
            ['Категория (hid)', 'Вопрос', 'Фактовый ответ'],
            [
                '734595',
                'На что влияет размер бака увлажнителя?',
                'От размера бака для воды зависит продолжительность беспрерывной работы увлажнителя. Время работы можно рассчитать, если разделить объем бака на расход жидкости за час.',
            ],
            [
                '734595',
                'В чем особенности ультразвукового увлажнителя?',
                'Ульразвуковой увлажнитель: под действием ультразвуковой мембраны вода разбивается на микроскопические капельки и с помощью вентилятора вместе с воздухом выходит наружу.',
            ],
            [
                '91616',
                'Из каких материалов делают стенки и дверцы?',
                'Оргстекло — самый долговечный и приемлемый по цене и качеству материал; полистирол — бюджетный, но служит недолго; закаленное стекло — долговечный, но подвержен царапинам.',
            ],
        ]

        svn_data.create_version()

    @classmethod
    def prepare_fact_ans_incut(cls):
        cls.index.models += [
            Model(title='some model #' + str(i), hyperid=101010 + i, hid=cls.hidArr[i]) for i in range(len(cls.hidArr))
        ]

    def test_fact_ans_incut(self):
        request_params = self.CgiParams(
            {
                "hid": "734595",
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "fact_ans_incut_enabled": 1,
                "market_blender_bundles_for_inclid": "22:bundle_fact_ans",
                "market_blender_bundles_row_position_format": 1,
            }
        )
        response = self.report.request_json(
            self.create_blender_request(
                request_params, rearr_factors, get_supported_incuts_cgi({1: range(1, 8), 2: range(1, 8)})
            )
        )
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "inClid": 22,
                            "incutId": "fact_ans_incut",
                            "items": [
                                {"id": NotEmpty(), "entity": "fact", "question": NotEmpty(), "text": NotEmpty()},
                                {"id": NotEmpty(), "entity": "fact", "question": NotEmpty(), "text": NotEmpty()},
                            ],
                        }
                    ]
                },
            },
        )


if __name__ == '__main__':
    main()
