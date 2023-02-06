# coding=utf-8
from __future__ import absolute_import

import ujson
from logging import Logger

from mock import Mock
from typing import cast

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.tests.factories import create_airline_model
from travel.avia.backend.main.rest.airlines.flights_to_city import FlightsToCityAirlinesView
from travel.avia.backend.repository.airlines import AirlineRepository
from travel.avia.backend.repository.helpers import NationalBox
from travel.avia.backend.repository.flights_to_city_airlines import FlightsToCityAirlinesRepository
from travel.avia.backend.repository.translations import TranslatedTitleRepository


class SimilarAirlinesViewTest(TestCase):
    SETTLEMENT_ID = 1

    def setUp(self):
        self.translations = TranslatedTitleRepository()
        self.airline_repository = AirlineRepository(self.translations)
        self.flights_to_city_airlines_repository = FlightsToCityAirlinesRepository()
        self._view = FlightsToCityAirlinesView(
            airline_repository=cast(AirlineRepository, self.airline_repository),
            flights_to_city_repository=cast(FlightsToCityAirlinesRepository, self.flights_to_city_airlines_repository),
            logger=cast(Logger, Mock()),
        )

    def test_no_airline_view(self):
        expected = {
            'status': 'ok',
            'data': []
        }
        self.flights_to_city_airlines_repository.get = Mock(return_value=[])
        result = self._view._unsafe_process({
            'national_version': u'ru',
            'lang': u'ru',
            'settlement_id': self.SETTLEMENT_ID,
        })
        actual = ujson.loads(result.response[0])
        assert actual == expected

    def _create_airline_model(self, pk):
        return create_airline_model(
            pk, pk=pk, iata='XX', logo=u'logo_%d' % pk,
            translated_title_repository=self.translations,
            popular_score_by_national_version=NationalBox({'ru': 1}),
        )

    def test_view(self):
        expected = {
            u'status': u'ok',
            u'data': [
                {u'score': 99, u'id': 1, u'slug': u'', u'logo': u'logo_1', u'title': u'1 (XX)'},
                {u'score': 98, u'id': 2, u'slug': u'', u'logo': u'logo_2', u'title': u'2 (XX)'},
                {u'score': 97, u'id': 3, u'slug': u'', u'logo': u'logo_3', u'title': u'3 (XX)'}]
        }

        airlines = {}
        flights_to_city = []
        default = {
            'national_version__code': 'ru',
            'settlement_to_id': self.SETTLEMENT_ID,
        }
        for pk in range(1, 4):
            airlines[pk] = self._create_airline_model(pk)
            flights_to_city.append(dict(default, airline_id=pk, score=100-pk))

        self.airline_repository.get = Mock(side_effect=lambda x: airlines.get(x))
        self.flights_to_city_airlines_repository.get = Mock(return_value=flights_to_city)
        result = self._view._unsafe_process({
            'national_version': u'ru',
            'lang': u'ru',
            'settlement_id': self.SETTLEMENT_ID,
        })
        actual = ujson.loads(result.response[0])
        print actual
        assert actual == expected
