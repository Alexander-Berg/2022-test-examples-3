# coding=utf-8
from __future__ import unicode_literals, absolute_import

import ujson
from logging import Logger

from mock import Mock
from typing import cast

from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.library.python.common.models.schedule import CompanySynonym
from travel.avia.backend.main.rest.airlines.airlines_synonyms import AirlinesSynonymsView
from travel.avia.backend.repository.airlines import AirlineRepository
from travel.avia.backend.repository.translations import TranslatedTitleRepository
from travel.avia.library.python.tester.factories import create_company, get_model_factory


create_company_synonym = get_model_factory(CompanySynonym)


class AirlinesByPopularViewTest(TestCase):
    def setUp(self):
        self._translated_title_repository = TranslatedTitleRepository()
        self._airline_repository = AirlineRepository(self._translated_title_repository)
        self._fake_airline_repository = Mock()
        self._view = AirlinesSynonymsView(
            airline_repository=self._airline_repository,
            logger=cast(Logger, Mock())
        )

    def test_aaa(self):
        assert str('aaa') == 'aaa'

    def test_view(self):
        artificial_company_id = 1000000
        create_company(
            id=artificial_company_id,
            title='Amirov Airline',
            iata='AE',
            sirena_id='ДП',
            icao='AER',
            icao_ru='ОТ',
            t_type_id=TransportType.PLANE_ID
        )
        create_company_synonym(company_id=artificial_company_id, synonym='Amirov Airline DE', language='de')
        create_company_synonym(company_id=artificial_company_id, synonym='Amirov Airline EN', language='en')
        create_company_synonym(company_id=artificial_company_id, synonym='Amirov Airline RU', language='ru')
        create_company_synonym(company_id=artificial_company_id, synonym='Amirov Airline TR', language='tr')
        create_company_synonym(company_id=artificial_company_id, synonym='Amirov Airline UK', language='uk')

        self._airline_repository.pre_cache()

        result = self._view._unsafe_process({})

        result_airlines = ujson.loads(result.response[0])['data']['airlines']
        created_company = [airline for airline in result_airlines if airline['id'] == artificial_company_id][0]

        expected_airline = {
            'id': artificial_company_id,
            'iata': 'AE',
            'sirena': 'ДП',
            'icao': 'AER',
            'icao_ru': 'ОТ',
            'titles': {
                'de': {
                    'nom': ['Amirov Airline DE']
                },
                'en': {
                    'nom': ['Amirov Airline EN']
                },
                'ru': {
                    'nom': [
                        'Amirov Airline',
                        'Amirov Airline RU'
                    ]
                },
                'tr': {
                    'nom': ['Amirov Airline TR']
                },
                'uk': {
                    'nom': [
                        'Amirov Airline',
                        'Amirov Airline UK'
                    ]
                }
            }
        }

        self.assertDictEqual(created_company, expected_airline)
