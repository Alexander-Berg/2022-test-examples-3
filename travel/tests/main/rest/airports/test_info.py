# -*- coding: utf-8 -*-
from __future__ import absolute_import

import ujson
from logging import Logger
from mock import Mock
from typing import cast

from travel.avia.backend.tests.main.rest.airports.base_test_case import AirportViewTest
from travel.avia.backend.main.rest.airports.info import AirportInfoView, AirportInfoForm
from travel.avia.backend.repository.settlement import SettlementRepository
from travel.avia.backend.repository.country import CountryRepository
from travel.avia.backend.repository.station_type import StationTypeRepository


class AirportInfoViewTest(AirportViewTest):
    def setUp(self):
        super(AirportInfoViewTest, self).setUp()
        self._view = AirportInfoView(
            form=AirportInfoForm(),
            repository=self._repository,
            settlement_repository=cast(SettlementRepository, Mock()),
            country_repository=cast(CountryRepository, Mock()),
            station_type_repository=cast(StationTypeRepository, Mock()),
            logger=cast(Logger, Mock())
        )

    def test_view(self):
        id = 9600370
        result = self._view._unsafe_process({
            'station_id': id,
        })
        response = ujson.loads(result.response[0])

        assert response[u'status'] == u'ok'
        assert response[u'data']['id'] == id

    def test_view_not_found(self):
        id = 1
        result = self._view._unsafe_process({
            'station_id': id,
        })
        response = ujson.loads(result.response[0])

        assert response[u'status'] == u'ok'
        assert response[u'data'] is None
