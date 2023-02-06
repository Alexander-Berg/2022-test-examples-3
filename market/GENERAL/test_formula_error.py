#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.matcher import Contains, Regex


class T(TestCase):
    @classmethod
    def prepare(cls):
        pass

    def test_formula_error_in_debug(self):
        """
        Проверяем, что при передаче флага с отсутствующей формулой появляется запись в logicTrace в дебаге
        """
        response = self.report.request_json(
            'place=prime&text=test&rearr-factors=market_meta_formula_type=meta_fml_formula_35001154&debug=da'
        )
        self.error_log.expect(code=1011, url_hash=Regex("[0-9a-f]{32}"))
        self.error_log.expect(code=3030)
        self.base_logs_storage.error_log.expect(code=3030)
        self.assertFragmentIn(
            response,
            {
                'logicTrace': [
                    Contains(
                        'Can\'t parse experimental flag "market_meta_formula_type" Meta mn algorithm "meta_fml_formula_35001154" doesn\'t exist'
                    )
                ]
            },
        )


if __name__ == '__main__':
    main()
