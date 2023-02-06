#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main
from core.types import GLParam, GLType, NavCategory, Offer


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.navtree += [
            NavCategory(
                nid=1,
                children=[NavCategory(nid=2, hid=1, name='kot-nid-1'), NavCategory(nid=3, hid=2, name='kot-nid-2')],
            )
        ]
        cls.index.gltypes += [
            GLType(param_id=1, hid=1, gltype=GLType.ENUM, hidden=False, values=[1, 2], name="kot-param-1"),
            GLType(param_id=2, hid=2, gltype=GLType.ENUM, hidden=False, values=[1, 2], name="kot-param-2"),
        ]
        cls.index.offers += [
            Offer(hid=1, title='kot1', glparams=[GLParam(param_id=1, value=1)]),
            Offer(hid=2, title='kot2', glparams=[GLParam(param_id=2, value=1)]),
        ]

    def test_nid_via_hids(self):
        """проверяем, что с включенными флагами фильтры берутся от 1го ребенка виртуального нида"""
        response = self.report.request_json('place=prime&nid=1')
        self.assertFragmentIn(
            response,
            {
                "search": {},
                "filters": [
                    {"name": "kot-param-1"},
                ],
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "search": {},
                "filters": [
                    {"name": "kot-param-2"},
                ],
            },
        )


if __name__ == '__main__':
    main()
