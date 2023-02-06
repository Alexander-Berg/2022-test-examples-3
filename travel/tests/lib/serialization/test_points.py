# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest
from rest_framework import serializers

from common.tester.factories import create_settlement
from geosearch.models import NameSearchIndex
from travel.rasp.wizards.suburban_wizard_api.lib.serialization.points import parse_points

pytestmark = pytest.mark.dbuser


@pytest.mark.parametrize('departure_query', (
    {'departure_point_key': 'c1'},
    {'departure_settlement_geoid': '2'},
    {'departure_point_name': 'отправление'}
))
@pytest.mark.parametrize('arrival_query', (
    {'arrival_point_key': 'c3'},
    {'arrival_settlement_geoid': '4'},
    {'arrival_point_name': 'прибытие'}
))
def test_parse_points(departure_query, arrival_query):
    departure_settlement = create_settlement(id=1, _geo_id=2, title_ru='отправление')
    arrival_settlement = create_settlement(id=3, _geo_id=4, title_ru='прибытие')
    NameSearchIndex.objects.bulk_create(NameSearchIndex.get_records())

    assert parse_points(dict(departure_query, **arrival_query)) == (departure_settlement, arrival_settlement)


@pytest.mark.parametrize('query_params, expected_message', (
    (
        {},
        'one of arguments have to be specified: '
        'departure_point_key, departure_settlement_geoid or departure_point_name'
    ),
    (
        {'departure_point_key': 'c1'},
        'one of arguments have to be specified: '
        'arrival_point_key, arrival_settlement_geoid or arrival_point_name'
    ),
    (
        {'departure_point_key': 'c1', 'departure_point_name': 'отправление', 'arrival_point_key': 'c3'},
        'only one of arguments have to be specified: '
        'departure_point_key, departure_settlement_geoid or departure_point_name'
    ),
    (
        {'departure_point_key': 'c1', 'arrival_point_key': 'c3', 'arrival_point_name': 'прибытие'},
        'only one of arguments have to be specified: '
        'arrival_point_key, arrival_settlement_geoid or arrival_point_name'
    ),
    (
        {'departure_point_name': 'foo'},
        'invalid point name departure_point_name: no suitable points found for term u\'foo\''
    ),
    (
        {'departure_point_key': 'c1', 'arrival_point_name': 'bar'},
        'invalid point name arrival_point_name: no suitable points found for term u\'bar\''
    ),
    (
        {'departure_point_key': 'c1', 'arrival_point_key': 'c1'},
        'departure and arrival points are same'
    ),
))
def test_parse_points_validation(query_params, expected_message):
    create_settlement(id=1)
    create_settlement(id=3)

    with pytest.raises(serializers.ValidationError) as excinfo:
        parse_points(query_params)
    assert excinfo.value.detail == [expected_message]
