# coding=utf-8
from __future__ import absolute_import

import ujson
from logging import Logger

from mock import Mock
from typing import cast

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.tests.factories import create_airline_model
from travel.avia.backend.main.rest.airlines.similar_airlines import SimilarAirlinesView
from travel.avia.backend.repository.airlines import AirlineRepository
from travel.avia.backend.repository.helpers import NationalBox
from travel.avia.backend.repository.similar_airlines import SimilarAirlinesRepository
from travel.avia.backend.repository.translations import TranslatedTitleRepository


class SimilarAirlinesViewTest(TestCase):
    def setUp(self):
        self.translations = TranslatedTitleRepository()
        self._fake_airline_repository = AirlineRepository(self.translations)
        self._fake_similar_airlines_repository = Mock()
        self._view = SimilarAirlinesView(
            airline_repository=cast(AirlineRepository, self._fake_airline_repository),
            similar_airlines_repository=cast(SimilarAirlinesRepository, self._fake_similar_airlines_repository),
            logger=cast(Logger, Mock()),
        )

    def test_no_airline_view(self):
        expected = {
            'status': 'ok',
            'data': []
        }
        self._fake_airline_repository.get = Mock(return_value=None)
        result = self._view._unsafe_process({
            'national_version': u'ru',
            'lang': u'ru',
            'company_id': 1,
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
                {u'score': 0.5, u'id': 2, u'slug': u'', u'logo': u'logo_2', u'title': u'2 (XX)'},
                {u'score': 0, u'id': 3, u'slug': u'', u'logo': u'logo_3', u'title': u'3 (XX)'}]
        }

        airlines = {}
        for pk in range(1, 4):
            airlines[pk] = self._create_airline_model(pk)
        self._fake_airline_repository.get = Mock(side_effect=lambda x: airlines.get(x))
        self._fake_airline_repository.get_all = Mock(side_effect=lambda: airlines.values())
        self._fake_similar_airlines_repository.get = Mock(return_value=[{'company_id': 2, 'score': 0.5}])
        result = self._view._unsafe_process({
            'national_version': u'ru',
            'lang': u'ru',
            'company_id': 1,
        })
        actual = ujson.loads(result.response[0])
        assert actual == expected
