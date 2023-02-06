# -*- coding: utf-8 -*-
from __future__ import absolute_import

import ujson
from logging import Logger
from mock import Mock
from typing import cast

from travel.avia.backend.tests.main.rest.transport_model.planes.base_test_case import PlaneViewTest
from travel.avia.backend.main.rest.transport_model.planes.info import PlaneInfoView


class PlaneInfoViewTest(PlaneViewTest):
    def setUp(self):
        super(PlaneInfoViewTest, self).setUp()
        self._view = PlaneInfoView(self._repository, logger=cast(Logger, Mock()))

    def test_view_null(self):
        result = self._view._unsafe_process({
            'code_en': 'CR2',
        })
        response = ujson.loads(result.response[0])

        assert response[u'status'] == u'ok'
        assert response[u'data'] is None

    def test_view(self):
        result = self._view._unsafe_process({
            'code_en': 'CE2',
        })
        response = ujson.loads(result.response[0])

        assert response[u'status'] == u'ok'
        assert response[u'data']['code_en'] == 'CE2'
