#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import GLParam, GLType, Model


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.gltypes += [
            GLType(
                param_id=202,
                hid=1,
                gltype=GLType.ENUM,
                values=list(range(40, 51)),
                unit_name="Length",
            ),
        ]

        cls.index.models += [
            Model(title="dress 44", hid=1, hyperid=309, ts=3009, glparams=[GLParam(param_id=202, value=44)]),
            Model(title="dress 46", hid=1, hyperid=310, ts=3010, glparams=[GLParam(param_id=202, value=46)]),
        ]

    def test_models_gl_filters(self):
        response = self.report.request_bs('place=parallel&text=dress&hid=1&glfilter=202:44')
        self.assertFragmentIn(response, {"text": "dress 44"}, preserve_order=True)
        self.assertFragmentNotIn(response, {"text": "dress 46"}, preserve_order=True)

        response = self.report.request_bs('place=parallel&text=dress&hid=1&glfilter=202:46')
        self.assertFragmentIn(response, {"text": "dress 46"}, preserve_order=True)
        self.assertFragmentNotIn(response, {"text": "dress 44"}, preserve_order=True)

        response = self.report.request_bs('place=parallel&text=dress&hid=1&glfilter=202:50')
        self.assertFragmentNotIn(response, {"text": "dress 46"}, preserve_order=True)
        self.assertFragmentNotIn(response, {"text": "dress 44"}, preserve_order=True)


if __name__ == '__main__':
    main()
