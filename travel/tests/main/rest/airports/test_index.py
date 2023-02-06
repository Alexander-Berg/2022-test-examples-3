# -*- coding: utf-8 -*-
from __future__ import absolute_import

import ujson
from logging import Logger
from mock import Mock
from typing import cast

from travel.avia.backend.tests.main.rest.airports.base_test_case import AirportViewTest
from travel.avia.backend.main.rest.airports.index import AirportIndexView, AirportIndexForm
from travel.avia.backend.repository.settlement import SettlementRepository
from travel.avia.backend.repository.country import CountryRepository
from travel.avia.backend.repository.station_type import StationTypeRepository


class AirportIndexViewTest(AirportViewTest):
    def setUp(self):
        super(AirportIndexViewTest, self).setUp()
        self._view = AirportIndexView(
            form=AirportIndexForm(),
            repository=self._repository,
            settlement_repository=cast(SettlementRepository, Mock()),
            country_repository=cast(CountryRepository, Mock()),
            station_type_repository=cast(StationTypeRepository, Mock()),
            logger=cast(Logger, Mock())
        )

    def test_view(self):
        result = self._view._unsafe_process({
            'id': None,
        })
        response = ujson.loads(result.response[0])

        assert response[u'status'] == u'ok'
        assert len(response[u'data']) == 2
        for d in response[u'data']:
            assert d[u'iataCode'] in ('SVX', 'JFK')

    def test_view_one_id(self):
        result = self._view._unsafe_process({
            'id': '9600370',
        })
        response = ujson.loads(result.response[0])

        assert response[u'status'] == u'ok'
        assert len(response[u'data']) == 1
        for d in response[u'data']:
            assert d[u'id'] == 9600370

    def test_view_two_id(self):
        result = self._view._unsafe_process({
            'id': '9600370,9600371',
        })
        response = ujson.loads(result.response[0])

        assert response[u'status'] == u'ok'
        assert len(response[u'data']) == 2
        for d in response[u'data']:
            assert d[u'id'] in (9600370, 9600371)

    def test_view_not_found_id(self):
        result = self._view._unsafe_process({
            'id': '1',
        })
        response = ujson.loads(result.response[0])

        assert response[u'status'] == u'ok'
        assert len(response[u'data']) == 0
