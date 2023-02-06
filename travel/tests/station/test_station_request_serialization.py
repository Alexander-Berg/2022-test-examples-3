# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import time

from hamcrest import assert_that, contains_inanyorder

from travel.rasp.morda_backend.morda_backend.station.request_serialization import (
    StationQuickContextQuerySchema, StationStopsContextQuerySchema, StationByIdQuerySchema,
    StationContextQuerySchema, PopularDirectionsQuerySchema
)


def test_station_by_id_request():
    get_params = {'station_id': '1111'}
    context, _ = StationByIdQuerySchema().load(get_params)
    assert context['station_id'] == 1111


def test_station_quick_request():
    get_params = {'station_id': '1111', 'subtype': 'suburban'}
    context, _ = StationQuickContextQuerySchema().load(get_params)
    assert context.station_id == 1111
    assert context.subtype == 'suburban'

    get_params = {'station_id': '1112'}
    context, _ = StationQuickContextQuerySchema().load(get_params)
    assert context.station_id == 1112
    assert context.subtype is None


def test_station_request():
    get_params = {'station_id': '1111'}
    context, _ = StationContextQuerySchema().load(get_params)
    assert context.station_id == 1111
    assert context.subtype is None
    assert context.is_mobile is False
    assert context.event == 'departure'
    assert context.direction is None
    assert context.date == 'today'
    assert context.time_after is None
    assert context.time_before is None
    assert context.country == 'ru'

    get_params = {
        'station_id': '1112',
        'subtype': 'suburban',
        'is_mobile': 'true',
        'event': 'arrival',
        'direction': 'направление',
        'date': 'all-days',
    }
    context, _ = StationContextQuerySchema().load(get_params)
    assert context.station_id == 1112
    assert context.subtype == 'suburban'
    assert context.is_mobile is True
    assert context.event == 'arrival'
    assert context.direction == 'направление'
    assert context.date == 'all-days'
    assert context.country == 'ru'

    get_params = {
        'station_id': '1113',
        'subtype': 'plane',
        'is_mobile': 'false',
        'event': 'arrival',
        'date': 'tomorrow',
        'time_after': '08:00',
        'time_before': '12:30',
        'country': 'ua'
    }
    context, _ = StationContextQuerySchema().load(get_params)
    assert context.station_id == 1113
    assert context.subtype == 'plane'
    assert context.is_mobile is False
    assert context.event == 'arrival'
    assert context.date == 'tomorrow'
    assert context.time_after == time(8)
    assert context.time_before == time(12, 30)
    assert context.country == 'ua'


def test_station_stops_request():
    get_params = {
        'station_id': '111',
        'subtype': 'bus',
        'is_mobile': 'false',
        'event': 'departure',
        'date': 'all-days',
        'return_for_types': 'bus,water'
    }

    context, _ = StationStopsContextQuerySchema().load(get_params)
    assert context.station_id == 111
    assert context.subtype == 'bus'
    assert context.is_mobile is False
    assert context.event == 'departure'
    assert context.date == 'all-days'
    assert context.country == 'ru'
    assert_that(context.return_for_types, contains_inanyorder('bus', 'water'))

    get_params = {'station_id': '111'}
    context, _ = StationStopsContextQuerySchema().load(get_params)

    assert context.station_id == 111
    assert len(context.return_for_types) == 0


def test_station_popular_directions_request():
    get_params = {'station_id': '1111', 'subtype': 'suburban'}
    context, _ = PopularDirectionsQuerySchema().load(get_params)
    assert context.station_id == 1111
    assert context.subtype == 'suburban'
    assert context.limit == 2

    get_params = {'station_id': '1112'}
    context, _ = PopularDirectionsQuerySchema().load(get_params)
    assert context.station_id == 1112
    assert context.subtype is None
    assert context.limit == 2

    get_params = {'station_id': 1113, 'subtype': 'train', 'limit': 5}
    context, _ = PopularDirectionsQuerySchema().load(get_params)
    assert context.station_id == 1113
    assert context.subtype == 'train'
    assert context.limit == 5
