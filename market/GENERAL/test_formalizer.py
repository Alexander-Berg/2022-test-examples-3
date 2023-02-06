#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.matcher import Absent, Contains
from core.svn_data import SvnData
from core.testcase import TestCase, main
from core.types import FormalizedParam, GLParam, GLType, Offer
from core.types.autogen import Const
from core.types.formalizer_blacklist import FormalizerBlacklistRecord


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_svn_data = True
        cls.settings.market_access_settings.use_svn_data = True

        cls.index.gltypes += [
            GLType(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, hid=1, gltype=GLType.ENUM, values=[7701962]),
        ]

        cls.formalizer.on_request(hid=1, query='xiaomi смартфон').respond(
            formalized_params=[
                FormalizedParam(
                    param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID,
                    value=7701962,
                    is_numeric=False,
                    param_positions=(0, 6),
                ),
            ]
        )
        cls.formalizer.on_request(hid=13, query="летние сапоги чулки").respond(
            formalized_params=[
                FormalizedParam(
                    param_id=Const.DEFAULT_FASHION_WEATHER_SEASON_PARAM_ID,
                    value=28575659,
                    is_numeric=False,
                    param_positions=(0, 6),
                ),
                FormalizedParam(
                    param_id=Const.DEFAULT_FASHION_PRODUCT_TYPE_PARAM_ID,
                    value=28575453,
                    is_numeric=False,
                    param_positions=(7, 12),
                ),
                FormalizedParam(
                    param_id=Const.DEFAULT_FASHION_PRODUCT_TYPE_PARAM_ID,
                    value=28575009,
                    is_numeric=False,
                    param_positions=(13, 19),
                ),
            ]
        )

        cls.index.offers += [
            Offer(
                hid=1,
                title='xiaomi смартфон',
                glparams=[
                    GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=7701962),
                ],
            ),
            Offer(
                hid=13,
                title="летние сапоги чулки",
                glparams=[
                    GLParam(param_id=Const.DEFAULT_FASHION_WEATHER_SEASON_PARAM_ID, value=28575659),
                    GLParam(param_id=Const.DEFAULT_FASHION_PRODUCT_TYPE_PARAM_ID, value=28575453),
                    GLParam(param_id=Const.DEFAULT_FASHION_PRODUCT_TYPE_PARAM_ID, value=28575009),
                ],
            ),
        ]

    @classmethod
    def setup_market_access_resources(cls, access_server, shade_host_port):
        svn_data = SvnData(access_server=access_server, shade_host_port=shade_host_port, meta_paths=cls.meta_paths)

        svn_data.formalizer_blacklist += [
            FormalizerBlacklistRecord(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value_id=7701962),
        ]

        svn_data.create_version()

    def test_formalizer_blacklist(self):
        """Проверяем, что флаг market_enable_formalizer_blacklist=1 включает фильтрацию ответа формализатора
        https://st.yandex-team.ru/MARKETOUT-46924
        """
        request = 'place=prime&text=xiaomi+смартфон&cvredirect=1&debug=1'

        response = self.report.request_json(request + '&rearr-factors=market_enable_formalizer_blacklist=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["9"],
                        "text": ["xiaomi смартфон"],
                        "glfilter": Absent(),
                    },
                    "target": "search",
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains("Formalized param (param_id=7893318, value_id=7701962) was skipped: filtered by blacklist")
                ]
            },
        )

        # Без флага blacklist выключен
        response = self.report.request_json(request + '&rearr-factors=market_enable_formalizer_blacklist=0')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["11"],
                        "text": ["xiaomi смартфон"],
                        "glfilter": ["7893318:7701962"],
                    },
                    "target": "search",
                }
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "logicTrace": [
                    Contains("Formalized param (param_id=7893318, value_id=7701962) was skipped: filtered by blacklist")
                ]
            },
        )

    def test_formalizer_with_single_or_without_product_types(self):
        """Проверяем, что тип фильтра не устанавливается, если их было распознано несколько"""
        request = 'place=prime&text=летние+сапоги+чулки&cvredirect=1&debug=1'

        # фильтруем тип продукта в ответе формализатора
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["11"],
                        "text": ["летние сапоги чулки"],
                        "glfilter": ["27142893:28575659"],
                    },
                    "target": "search",
                }
            },
        )
        self.assertFragmentIn(
            response,
            {"logicTrace": [Contains("Formalizer response has many product types: remove all of them")]},
        )


if __name__ == '__main__':
    main()
