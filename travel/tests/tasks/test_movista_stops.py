# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
import pytest
import httpretty

from django.conf import settings
from hamcrest import assert_that, contains_inanyorder, has_properties

from common.tester.factories import create_station_code, create_station
from common.models.geo import CodeSystem

from travel.rasp.suburban_selling.selling.movista.factories import MovistaStationsFactory
from travel.rasp.suburban_selling.selling.movista.models import MovistaStations
from travel.rasp.suburban_selling.selling.tasks.movista_stops import load_stops_data


pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def _register_api_url(response_json):
    httpretty.register_uri(
        httpretty.POST, '{}api/v1/stops'.format(settings.MOVISTA_API_HOST),
        content_type='application/json', body=json.dumps(response_json)
    )


@httpretty.activate
def test_load_movista_stops_data():
    station_1 = create_station(id=10)
    station_2 = create_station(id=20)
    station_3 = create_station(id=30)
    code_system = CodeSystem.objects.get(id=CodeSystem.EXPRESS_ID)
    create_station_code(code='100', station=station_1, system=code_system)
    create_station_code(code='200', station=station_2, system=code_system)
    create_station_code(code='300', station=station_3, system=code_system)

    MovistaStations.objects().delete()
    MovistaStationsFactory(station_id=10, has_wicket=True, wicket_type='PA2validatorTutorial')
    MovistaStationsFactory(station_id=20, has_wicket=False, wicket_type=None)

    _register_api_url([
        {'id': 100, 'hasWicket': True, 'tutorialType' : 'MID2Tutorial'},
        {'id': 200, 'hasWicket': True, 'tutorialType' : 'PA2validatorTutorial'},
        {'id': 300, 'hasWicket': False},
    ])

    load_stops_data()
    stations = MovistaStations.objects()

    assert_that(stations, contains_inanyorder(
        has_properties({'station_id': 10, 'has_wicket': True, 'wicket_type': 'MID2Tutorial'}),
        has_properties({'station_id': 20, 'has_wicket': True, 'wicket_type': 'PA2validatorTutorial'}),
        has_properties({'station_id': 30, 'has_wicket': False, 'wicket_type': None}),
    ))
