#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.matcher import NoKey
from core.testcase import TestCase, main
from core.types import GLParam, GLType, Offer


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.gltypes += [
            GLType(param_id=201, hid=1, gltype=GLType.ENUM, hidden=False, values=[1, 2]),
        ]
        cls.index.offers += [
            Offer(hid=1, title='iphone', glparams=[GLParam(param_id=201, value=1)], manufacturer_warranty=True),
            Offer(hid=1, title='iphone 2', glparams=[GLParam(param_id=201, value=2)], manufacturer_warranty=False),
        ]

    def test_gurulight_flag(self):
        response = self.report.request_json('place=prime&hid=1')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": "201", "isGuruLight": True},
                    {"id": "manufacturer_warranty", "isGuruLight": NoKey("isGuruLight")},
                ]
            },
        )


if __name__ == '__main__':
    main()
