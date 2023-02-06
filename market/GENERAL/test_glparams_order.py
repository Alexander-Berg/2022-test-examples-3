#!/usr/bin/env python
# -*- coding: utf-8 -*-
import runner  # noqa

from core.testcase import TestCase, main
from core.types import GLParam, GLType, Offer


DRUGS_HID = 13077405
NON_DRUGS_HID = 13077406


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.use_delivery_statistics = True

        cls.index.gltypes += [
            GLType(param_id=201, hid=1, gltype=GLType.ENUM, hidden=True, values=[1, 2]),
            GLType(param_id=202, hid=1, gltype=GLType.ENUM, hidden=False, values=[3, 4]),
            GLType(param_id=203, hid=1, gltype=GLType.NUMERIC, hidden=True),
        ]

        cls.index.offers += [
            Offer(
                hid=1,
                title='good iphone',
                glparams=[
                    GLParam(param_id=201, value=1),
                    GLParam(param_id=202, value=3),
                    GLParam(param_id=203, value=42),
                ],
            ),
            Offer(
                hid=1, title='bad iphone 1', glparams=[GLParam(param_id=201, value=2), GLParam(param_id=203, value=42)]
            ),
            Offer(
                hid=1, title='bad iphone 2', glparams=[GLParam(param_id=201, value=1), GLParam(param_id=203, value=123)]
            ),
            Offer(
                hid=1, title='bad iphone 3', glparams=[GLParam(param_id=201, value=2), GLParam(param_id=203, value=123)]
            ),
        ]

    # See https://st.yandex-team.ru/MARKETOUT-40946
    def test_glfilter_order__checked_first(self):
        response = self.report.request_json('place=prime&text=iphone&glfilter=201:1&glfilter=203:42~43&hid=1')
        self.assertFragmentIn(response, {"filters": [{"id": "201"}, {"id": "202"}, {"id": "203"}]}, preserve_order=True)

        response = self.report.request_json(
            'place=prime&text=iphone&glfilter=201:1&glfilter=203:42~43&hid=1&glfilters-order=checked_first'
        )
        self.assertFragmentIn(response, {"filters": [{"id": "201"}, {"id": "203"}, {"id": "202"}]}, preserve_order=True)

    # See https://st.yandex-team.ru/MARKETOUT-40954
    def test_glfilter_order__cpa(self):
        response = self.report.request_json('place=prime&text=iphone&glfilter=201:1&glfilter=203:42~43&hid=1&cpa=real')
        self.assertFragmentIn(response, {"filters": [{"id": "201"}, {"id": "203"}, {"id": "cpa"}]}, preserve_order=True)

        response = self.report.request_json(
            'place=prime&text=iphone&glfilter=201:1&glfilter=203:42~43&hid=1&cpa=real&glfilters-order=cpa'
        )
        self.assertFragmentIn(response, {"filters": [{"id": "cpa"}, {"id": "201"}, {"id": "203"}]}, preserve_order=True)

    def test_glfilter_order__multiply(self):
        response = self.report.request_json(
            'place=prime&text=iphone&glfilter=203:42~43&hid=1&cpa=real&glfilters-order=cpa,glprice,checked_first'
        )
        self.assertFragmentIn(
            response,
            {"filters": [{"id": "cpa"}, {"id": "glprice"}, {"id": "203"}, {"id": "onstock"}]},
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
