#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import GLParam, GLType, GLValue, Offer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare_hide_filter_type(cls):
        cls.index.gltypes += [
            GLType(
                param_id=1,
                hid=1,
                gltype=GLType.ENUM,
                values=[GLValue(value_id=1), GLValue(value_id=2)],
                xslname='type_marketing',
            )
        ]

        cls.index.offers += [
            Offer(title='title 1', price=42, hid=1, glparams=[GLParam(param_id=1, value=1)]),
            Offer(title='title 2', price=42, hid=1, glparams=[GLParam(param_id=1, value=1)]),
        ]

    def test_hide_filter_type(self):
        response = self.report.request_json("place=prime&hid=1")
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'filters': [{'xslname': 'type_marketing', 'values': [{'value': 'VALUE-1', 'id': '1'}]}]},
                        {'filters': [{'xslname': 'type_marketing', 'values': [{'value': 'VALUE-1', 'id': '1'}]}]},
                    ]
                }
            },
        )
        self.assertFragmentNotIn(response, {'search': {}, 'filters': [{'xslname': 'type_marketing'}]})

    @classmethod
    def prepare_nonhide_filter_type(cls):
        cls.index.gltypes += [
            GLType(
                param_id=2,
                hid=2,
                gltype=GLType.ENUM,
                values=[GLValue(value_id=1), GLValue(value_id=2)],
                xslname='type_marketing',
            )
        ]

        cls.index.offers += [
            Offer(title='title 1', price=42, hid=2, glparams=[GLParam(param_id=2, value=1)]),
            Offer(title='title 2', price=42, hid=2, glparams=[GLParam(param_id=2, value=2)]),
        ]

    def test_nonhide_filter_type(self):
        response = self.report.request_json("place=prime&hid=2")
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'filters': [{'xslname': 'type_marketing', 'values': [{'value': 'VALUE-1', 'id': '1'}]}]},
                        {'filters': [{'xslname': 'type_marketing', 'values': [{'value': 'VALUE-2', 'id': '2'}]}]},
                    ]
                },
                'filters': [
                    {
                        'xslname': 'type_marketing',
                        'values': [
                            {'value': 'VALUE-1', 'id': '1'},
                            {'value': 'VALUE-2', 'id': '2'},
                        ],
                    }
                ],
            },
        )


if __name__ == '__main__':
    main()
