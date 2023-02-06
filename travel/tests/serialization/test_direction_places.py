# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date

import pytest
from rest_framework import serializers

from common.tester.factories import create_settlement, create_station
from common.tester.utils.datetime import replace_now
from travel.rasp.wizards.train_wizard_api.serialization.direction_places import DirectionPlacesQuery, dump_query, load_query
from travel.rasp.wizards.wizard_lib.experiment_flags import ExperimentFlag

pytestmark = pytest.mark.dbuser


@replace_now('2000-01-01')
def test_load_query():
    departure_station = create_station()
    arrival_station = create_station()

    assert load_query({
        'departure_point_key': departure_station.point_key,
        'arrival_point_key': arrival_station.point_key,
        'min_departure_date': '2000-06-01',
        'days': '7',
        'language': 'uk',
        'tld': 'ru',
        'exp_flags': 'RASPWIZARDS-557'
    }) == DirectionPlacesQuery(
        departure_point=departure_station,
        arrival_point=arrival_station,
        min_departure_date=date(2000, 6, 1),
        days=7,
        language='uk',
        experiment_flags=frozenset([ExperimentFlag.EXPERIMENTAL_SEARCH]),
        tld='ru'
    )


@replace_now('2000-01-01')
def test_load_query_defaults():
    departure_station = create_station()
    arrival_station = create_station()

    assert load_query({
        'departure_point_key': departure_station.point_key,
        'arrival_point_key': arrival_station.point_key,
        'tld': 'ru',
    }) == DirectionPlacesQuery(
        departure_point=departure_station,
        arrival_point=arrival_station,
        min_departure_date=date(2000, 1, 1),
        days=5,
        language='ru',
        tld='ru',
        experiment_flags=frozenset()
    )


def test_load_query_settlement_geoids():
    station = create_station()
    departure_settlement = create_settlement(_geo_id=1)
    arrival_settlement = create_settlement(_geo_id=2)

    query = load_query({
        'departure_point_key': station.point_key,
        'arrival_settlement_geoid': '2',
        'tld': 'ru',
    })
    assert query.departure_point == station
    assert query.arrival_point == arrival_settlement

    query = load_query({
        'departure_settlement_geoid': '1',
        'arrival_point_key': station.point_key,
        'tld': 'ru',
    })
    assert query.departure_point == departure_settlement
    assert query.arrival_point == station

    query = load_query({
        'departure_settlement_geoid': '1',
        'arrival_settlement_geoid': '2',
        'tld': 'ru',
    })
    assert query.departure_point == departure_settlement
    assert query.arrival_point == arrival_settlement

    with pytest.raises(serializers.ValidationError):
        load_query({
            'departure_point_key': station.point_key,
            'departure_settlement_geoid': '1',
            'arrival_settlement_geoid': '2',
            'tld': 'ru',
        })

    with pytest.raises(serializers.ValidationError):
        load_query({
            'departure_settlement_geoid': '1',
            'arrival_point_key': station.point_key,
            'arrival_settlement_geoid': '2',
            'tld': 'ru',
        })

    with pytest.raises(serializers.ValidationError):
        load_query({})


@replace_now('2000-01-01')
@pytest.mark.parametrize('days', ('xxx', '0', '-1'))
def test_load_query_days_validation(days):
    departure_station = create_station()
    arrival_station = create_station()

    with pytest.raises(serializers.ValidationError) as excinfo:
        load_query({
            'departure_point_key': departure_station.point_key,
            'arrival_point_key': arrival_station.point_key,
            'days': days,
            'tld': 'ru',
        })
    assert excinfo.value.detail == ['invalid days value: it should be positive integer']


def test_dump_query():
    departure_station = create_station()
    arrival_station = create_station()

    assert dump_query(DirectionPlacesQuery(
        departure_point=departure_station,
        arrival_point=arrival_station,
        min_departure_date=date(2000, 1, 1),
        days=5,
        language='ru',
        experiment_flags=frozenset(),
        tld='ru',
    )) == {
        'query': {
            'departure_point': {
                'key': departure_station.point_key,
                'title': departure_station.L_popular_title()
            },
            'arrival_point': {
                'key': arrival_station.point_key,
                'title': arrival_station.L_popular_title()
            },
            'min_departure_date': '2000-01-01',
            'language': 'ru',
            'days': 5
        }
    }
