#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Model, GLParam
from core.svn_data import SvnData
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
                "bundle_rich_filters.json": BlenderBundleConstPositon.BUNDLE_CONST_SEARCH_POSITION.format(
                    row_position=1, incut_id="rich_filters_incut"
                )
            },
        )

    hidFiltersArr = [
        (15450081, 14805336),
        (18022709, 18021511),
        (18022709, 18021511),
        (18022709, 18027211),
        (18022709, 18027211),
        (18022709, 18027211),
        (18022709, 18024903),
        (18022709, 18024903),
        (1569931, 14770084),
        (1569931, 14770084),
        (1569931, 14770084),
        (1569931, 14770084),
    ]

    @classmethod
    def prepare_market_report_data_from_access(cls):
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_svn_data = True
        cls.settings.market_access_settings.use_svn_data = True

    @classmethod
    def setup_market_access_resources(cls, access_server, shade_host_port):
        svn_data = SvnData(access_server=access_server, shade_host_port=shade_host_port, meta_paths=cls.meta_paths)

        svn_data.rich_filters_data_arr += [
            [
                'Hid',
                'Название категории',
                'id фильтра',
                'Название фильтра',
                'Название для врезки',
                'id фильтра',
                'Название фильтра',
                'Название для врезки',
                'id фильтра',
                'Название фильтра',
                'Название для врезки',
            ],
            [
                '15450081',
                'Холодильники',
                '14805336',
                'Высота',
                'Высота',
                '15463693',
                'Размораживание морозильной камеры',
                'Размораживание морозильной камеры',
                '15463701',
                'Размораживание холодильной камеры',
                'Размораживание холодильной камеры',
                '15463731',
                'Кол-во компрессоров',
                'Кол-во компрессоров',
            ],
            [
                '18022709',
                'Бытовые стерилизаторы',
                '18024903',
                'Назначение',
                'Назначение',
                '18021511',
                'Подходит для мелкой портативной техники',
                'Подходит для мелкой портативной техники',
                '18027211',
                'Подходит для крупной непортативной техники',
                'Подходит для крупной непортативной техники',
            ],
            [
                '1569931',
                'Измельчители пищевых отходов',
                '14770084',
                'Объем перемалывающей камеры',
                'Объем перемалывающей камеры',
                '14770096',
                'Максимальные обороты двигателя',
                'Максимальные обороты двигателя',
            ],
        ]

        svn_data.create_version()

    @classmethod
    def prepare_rich_filters_incut(cls):
        cls.index.models += [
            Model(
                title='some model #' + str(i),
                hyperid=14111997 + i,
                hid=cls.hidFiltersArr[i][0],
                glparams=[GLParam(param_id=cls.hidFiltersArr[i][1], value=i)],
            )
            for i in range(len(cls.hidFiltersArr))
        ]

    def test_rich_filters_incut(self):
        request_params = self.CgiParams(
            {
                "hid": "18022709",
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "rich_filters_incut_enabled": 1,
                "market_blender_bundles_for_inclid": "19:bundle_rich_filters",
                "market_blender_media_adv_incut_enabled": 0,
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
                            "inClid": 19,
                            "incutId": "rich_filters_incut",
                            "filter": {"id": "18024903"},
                        }
                    ]
                },
            },
        )


if __name__ == '__main__':
    main()
