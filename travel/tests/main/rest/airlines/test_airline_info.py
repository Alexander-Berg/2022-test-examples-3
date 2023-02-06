# coding=utf-8
from __future__ import absolute_import

from logging import Logger

import pytest
from mock import Mock, patch
from typing import cast

from travel.avia.library.python.avia_data.models import TransportType
from travel.avia.library.python.tester.factories import create_aviacompany, create_company
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.main.rest.airlines.airline_info import (
    AirlineInfoView, AirlinesInfoView, AirlineRepository, AirlineRatingRepository
)
from travel.avia.backend.repository import airline_repository
from travel.avia.backend.repository.translations import TranslatedTitleRepository
from travel.avia.backend.tests.factories import create_airline_model

RETURN_FIELDS = (
    'rating', 'alliance', 'seo_description_i18n_key', 'color', 'iata', 'sirena',
    'icao', 'logoSvg', 'baggage_rules', 'id', 'default_tariff', 'title', 'url',
    'baggage_rules_url', 'slug', 'registration_url', 'registration_phone',
    'hidden', 'carryon', 'baggage',
)


class AirlineInfoViewTest(TestCase):
    def setUp(self):
        self.view = AirlineInfoView(
            AirlineRepository(cast(TranslatedTitleRepository, Mock())),
            cast(AirlineRatingRepository, Mock()),
            cast(Logger, Mock())
        )

    def test_view(self):
        company_id = 1
        airline_repository = Mock()
        airline_rating_repository = Mock()
        view = AirlineInfoView(
            cast(AirlineRepository, airline_repository),
            cast(AirlineRatingRepository, airline_rating_repository),
            cast(Logger, Mock())
        )

        view._unsafe_process({
            'company_id': company_id,
        })
        airline_rating_repository.get.assert_called_once_with(company_id)
        airline_repository.get.assert_called_once_with(company_id)

    @patch.object(AirlineRepository, 'get')
    def test_airline_info_view(self, airline_repo_get):
        expected_airline = {
            'alliance_id': 1,
            'iata': 'AA',
            'pk': 1,
        }
        airline_repo_get.return_value = create_airline_model(
            'test_airline', **expected_airline
        )

        actual = self.view._process({
            'company_id': 1,
            'lang': u'ru',
            'fields': {},
        })
        assert actual['alliance'] == expected_airline['alliance_id']
        assert actual['iata'] == expected_airline['iata']
        assert actual['id'] == expected_airline['pk']
        assert len(actual) == len(RETURN_FIELDS)
        assert all(map(actual.__contains__, RETURN_FIELDS))

    @patch.object(AirlineRepository, 'get')
    def test_airline_info_view_with_fields(self, airline_repo_get):
        expected_airline = {
            'alliance_id': 1,
            'iata': 'AA',
            'pk': 1,
        }
        airline_repo_get.return_value = create_airline_model(
            'test_airline', **expected_airline
        )

        actual = self.view._process({
            'company_id': 1,
            'lang': u'ru',
            'fields': {'id'},
        })
        assert actual['id'] == expected_airline['pk']
        assert len(actual) == 1


class AirlinesInfoViewTest(TestCase):
    def setUp(self):
        self.view = AirlinesInfoView(
            AirlineRepository(cast(TranslatedTitleRepository, Mock())),
            cast(AirlineRatingRepository, Mock()),
            cast(Logger, Mock())
        )

    @patch.object(AirlineRepository, 'get_all')
    def test_airline_info_view_with_fields(self, get_all):
        expected_airlines = [{
            'alliance_id': 1,
            'iata': 'AA',
            'pk': 1,
        }, {
            'alliance_id': 2,
            'iata': 'AB',
            'pk': 2,
        },
        ]
        airlines = [
            create_airline_model('test_airline', **expected_airline)
            for expected_airline in expected_airlines
        ]
        get_all.return_value = airlines

        with patch.object(AirlineRepository, 'get',
                          side_effect=lambda company_id: airlines[company_id-1]):
            actual = self.view._process({
                'lang': u'ru',
                'fields': {'id', 'iata', 'alliance'},
            })

        assert len(actual) == len(expected_airlines)
        for expected, actual in zip(expected_airlines, sorted(actual.items())):
            assert actual[0] == expected['pk']
            assert actual[1]['id'] == expected['pk']
            assert actual[1]['iata'] == expected['iata']
            assert actual[1]['alliance'] == expected['alliance_id']
            assert len(actual[1]) == 3


@pytest.mark.dbuser
@pytest.mark.parametrize('baggage_rules_are_valid', (True, False))
def test_airline_baggage(client, baggage_rules_are_valid):
    aviacompany = create_aviacompany(
        baggage_rules='some baggage rules text',
        baggage_rules_are_valid=baggage_rules_are_valid,
        rasp_company=create_company(t_type=TransportType.PLANE_ID),
    )
    airline_repository.pre_cache()

    try:
        response = client.get('/rest/airlines/airline_info/{}'.format(aviacompany.rasp_company_id))
    finally:
        airline_repository.reset()

    assert response.status_code == 200
    if aviacompany.baggage_rules_are_valid:
        assert response.json['data']['baggage_rules'] == aviacompany.baggage_rules
    else:
        assert not response.json['data']['baggage_rules']
