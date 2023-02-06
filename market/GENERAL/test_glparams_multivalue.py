#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from unittest import skip

from core.types import GLParam, GLType, Model, Offer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.gltypes = [
            GLType(param_id=201, hid=1, gltype=GLType.ENUM),
            GLType(param_id=202, hid=1, gltype=GLType.ENUM),
        ]

        cls.index.models = [
            Model(
                title='modeloffer',
                hyperid=500,
                hid=1,
                glparams=[
                    GLParam(param_id=201, value=302),
                    GLParam(param_id=201, value=303),
                ],
            ),
            Model(
                title='onlymodel',
                hid=1,
                hyperid=600,
                glparams=[
                    GLParam(param_id=201, value=301),
                    GLParam(param_id=201, value=302),
                    GLParam(param_id=202, value=401),
                ],
            ),
        ]

        cls.index.offers = [
            Offer(
                title='onlyoffer',
                hid=1,
                glparams=[
                    GLParam(param_id=201, value=301),
                    GLParam(param_id=201, value=302),
                    GLParam(param_id=202, value=401),
                ],
            ),
            Offer(
                title='modeloffer',
                hid=1,
                hyperid=500,
                glparams=[
                    GLParam(param_id=201, value=301),
                    GLParam(param_id=201, value=302),
                ],
            ),
        ]

    def test_onlyoffer(self):
        # no filters, OK
        response = self._query('onlyoffer', '')
        self.assertFragmentIn(response, {"results": [{"entity": "offer"}]}, allow_different_len=False)

        # filter by one or more values that are present: OK
        for filters in ['201:301', '201:302', '201:301,302', '201:301;201:302']:
            response = self._query('onlyoffer', filters)
            self.assertFragmentIn(response, {"results": [{"entity": "offer"}]}, allow_different_len=False)

        # filter by a missing value: nothing found
        response = self._query('onlyoffer', '201:303')
        self.assertFragmentNotIn(response, {"entity": "offer"})

        # filter by an OR of values, one of which is present: OK
        response = self._query('onlyoffer', '202:401,402')
        self.assertFragmentIn(response, {"results": [{"entity": "offer"}]}, allow_different_len=False)

    @skip('MARKETINDEXER-11552: a possible future feature: AND of enum glparam values')
    def test_onlyoffer_conjunction(self):
        # filter by an AND of values, one of which is present: nothing SHOULD BE found
        # REALITY: the second offer overrides the first, so only the last value is searched
        response = self._query('onlyoffer', '202:402;202:401')
        self.assertFragmentNotIn(response, {"entity": "offer"})

        response = self._query('onlyoffer', '202:401;202:402')
        self.assertFragmentNotIn(response, {"entity": "offer"})

    def test_modeloffer_overrides(self):
        # parameter values from the model override the ones from the offer completely
        response = self._query('modeloffer', '201:301')
        self.assertFragmentNotIn(response, {"entity": "product"})
        self.assertFragmentNotIn(response, {"entity": "offer"})

        response = self._query('modeloffer', '201:302')
        self.assertFragmentIn(response, {"entity": "product"})
        self.assertFragmentIn(response, {"entity": "offer"})

        response = self._query('modeloffer', '201:303')
        self.assertFragmentIn(response, {"entity": "product"})
        self.assertFragmentIn(response, {"entity": "offer"})

    def test_onlymodel(self):
        # no filters, OK
        response = self._query('onlymodel', '')
        self.assertFragmentIn(response, {"results": [{"entity": "product"}]}, allow_different_len=False)

        # filter by one or more values that are present: OK
        for filters in ['201:301', '201:302', '201:301,302', '201:301;201:302']:
            response = self._query('onlymodel', filters)
            self.assertFragmentIn(response, {"results": [{"entity": "product"}]}, allow_different_len=False)

        # filter by a missing value: nothing found
        response = self._query('onlymodel', '201:303')
        self.assertFragmentNotIn(response, {"entity": "offer"})

        # filter by an OR of values, one of which is present: OK
        response = self._query('onlymodel', '202:401,402')
        self.assertFragmentIn(response, {"results": [{"entity": "product"}]}, allow_different_len=False)

    @skip('MARKETINDEXER-11552: a possible future feature: AND of enum glparam values')
    def test_onlymodel_conjunction(self):
        # filter by an AND of values, one of which is present: nothing SHOULD BE found
        # REALITY: the second offer overrides the first, so only the last value is searched
        response = self._query('onlymodel', '202:402;202:401')
        self.assertFragmentNotIn(response, {"entity": "product"})

        response = self._query('onlymodel', '202:401;202:402')
        self.assertFragmentNotIn(response, {"entity": "product"})

    def _query(self, text, filters):
        return self.report.request_json('place=prime&text={}&hid=1&glfilter={}'.format(text, filters))


if __name__ == '__main__':
    main()
