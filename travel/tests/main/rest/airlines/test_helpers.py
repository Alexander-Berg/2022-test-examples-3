# coding=utf-8
from __future__ import absolute_import

from functools import partial

from mock import Mock
from typing import cast

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.main.rest.airlines.helpers import AbcAirlineProvider
from travel.avia.backend.tests.factories import create_airline_model
from travel.avia.backend.repository.airlines import AirlineRepository
from travel.avia.backend.repository.translations import TranslatedTitleRepository


class AbcAirlineProviderTest(TestCase):
    def setUp(self):
        self._fake_airline_repository = Mock()
        self._translated_title_repository = TranslatedTitleRepository()
        self._provider = AbcAirlineProvider(
            airline_repository=cast(AirlineRepository, self._fake_airline_repository)
        )

    def test_build_abc_index(self):
        airlines = map(partial(
            create_airline_model,
            translated_title_repository=self._translated_title_repository
        ),
            [u'б', u'ба', u'д', u'ДД', u'f', u'ff', u'z', u'zz']
        )
        self._fake_airline_repository.get_all = Mock(return_value=airlines)

        airlines_by_letter = self._provider.get_airlines_by_letter('ru')
        assert airlines_by_letter[u'б'] == airlines[:2]
        assert airlines_by_letter[u'д'] == airlines[2:4]
        assert airlines_by_letter[u'ф'] == []

        assert airlines_by_letter[u'f'] == airlines[4:6]
        assert airlines_by_letter[u'z'] == airlines[6:8]
        assert airlines_by_letter[u'r'] == []
