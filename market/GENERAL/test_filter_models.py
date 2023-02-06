#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import GLParam, GLType, Model, Offer, RegionalModel
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.models += [
            Model(hyperid=301, hid=1, title='model_A', glparams=[GLParam(param_id=201, value=1)]),
            Model(hyperid=302, hid=1, title='model_B', glparams=[GLParam(param_id=201, value=2)]),
            Model(hyperid=303, hid=1, title='model_C'),
            Model(hyperid=304, hid=1, title='model_E'),
            Model(hyperid=305, hid=1, title='model_F'),
            Model(hyperid=306, hid=1, title='model_G'),
            Model(hyperid=307, hid=1, title='model_H'),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=304, offers=1, cut_price_count=0),
            RegionalModel(hyperid=305, offers=0, cut_price_count=1),
            RegionalModel(hyperid=306, offers=1, cut_price_count=1),
            RegionalModel(hyperid=307, offers=0, cut_price_count=0),
        ]

        cls.index.offers += [
            Offer(price=1000, hyperid=301),
            Offer(price=1500, hyperid=301),
            Offer(price=2000, hyperid=302),
        ]

        cls.index.gltypes += [
            GLType(param_id=201, hid=1, gltype=GLType.ENUM, values=[1, 2]),
            GLType(param_id=202, hid=1, gltype=GLType.ENUM, values=[3, 4]),
        ]

    def test_format(self):
        response = self.report.request_json('place=filter_models&hid=1&modelid=301,302')
        self.assertFragmentIn(
            response, {'model_count': 2, 'models': [{'id': 301, 'name': 'model_A'}, {'id': 302, 'name': 'model_B'}]}
        )

    def test_glfilter(self):
        response = self.report.request_json('place=filter_models&hid=1&modelid=301,302&glfilter=201:1')
        self.assertFragmentIn(response, {'model_count': 1, 'models': [{'id': 301, 'name': 'model_A'}]})

    def test_price_filter(self):
        response = self.report.request_json('place=filter_models&hid=1&modelid=301,302&mcpricefrom=1000&mcpriceto=1300')
        self.assertFragmentIn(response, {'model_count': 1, 'models': [{'id': 301, 'name': 'model_A'}]})

        response = self.report.request_json('place=filter_models&hid=1&modelid=301,302&mcpricefrom=1000&mcpriceto=2000')
        self.assertFragmentIn(
            response, {'model_count': 2, 'models': [{'id': 301, 'name': 'model_A'}, {'id': 302, 'name': 'model_B'}]}
        )

    def test_onstock_filter(self):
        response = self.report.request_json('place=filter_models&hid=1&modelid=302,303&onstock=1')
        self.assertFragmentIn(response, {'model_count': 1, 'models': [{'id': 302, 'name': 'model_B'}]})

    def test_good_state(self):
        response = self.report.request_json('place=filter_models&hid=1&modelid=304,305,306,307')
        self.assertFragmentIn(
            response, {'model_count': 4, 'models': [{'id': 304}, {'id': 305}, {'id': 306}, {'id': 307}]}
        )

        response = self.report.request_json(
            'place=filter_models&hid=1&modelid=304,305,306,307&show-cutprice=1&good-state=cutprice'
        )
        self.assertFragmentIn(response, {'model_count': 2, 'models': [{'id': 305}, {'id': 306}]})

        response = self.report.request_json(
            'place=filter_models&hid=1&modelid=304,305,306,307&show-cutprice=1&good-state=new'
        )
        self.assertFragmentIn(
            response,
            {
                'model_count': 2,
                'models': [
                    {
                        'id': 304,
                    },
                    {
                        'id': 306,
                    },
                ],
            },
        )


if __name__ == '__main__':
    main()
