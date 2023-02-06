# -*- coding: utf-8 -*-
from __future__ import absolute_import

import ujson
from logging import Logger
from mock import Mock
from typing import cast

from travel.avia.backend.tests.main.rest.transport_model.planes.base_test_case import PlaneViewTest
from travel.avia.backend.main.rest.transport_model.planes.index import PlaneIndexView


class PlainIndexViewTest(PlaneViewTest):
    def setUp(self):
        super(PlainIndexViewTest, self).setUp()
        self._view = PlaneIndexView(self._repository, logger=cast(Logger, Mock()))

    def test_view(self):
        result = self._view._unsafe_process({})
        response = ujson.loads(result.response[0])

        assert response[u'status'] == u'ok'
        assert len(response[u'data']) == 2
        for d in response[u'data']:
            assert d[u'code_en'] in ('CE1', 'CE2')
            assert d[u'code'] in ('CR1', 'CR2')
