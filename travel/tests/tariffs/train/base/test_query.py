# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import datetime, date, timedelta

import pytest

from common.models.geo import Country
from common.tester.factories import create_station
from travel.rasp.train_api.tariffs.train.base.query import make_train_queries, make_train_local_queries

pytestmark = pytest.mark.dbuser


@pytest.mark.parametrize('tz, expected_dates', [
    ['Asia/Yekaterinburg', [date(2015, 1, 9), date(2015, 1, 10)]],
    ['Europe/Moscow', [date(2015, 1, 10)]],
])
def test_make_queries_railway_tz(tz, expected_dates):
    station_from = create_station(country=Country.RUSSIA_ID, time_zone=tz, settlement=None)
    station_to = create_station()

    dt_from = station_from.pytz.localize(datetime(2015, 1, 10, 0))
    dt_to = dt_from + timedelta(1)

    assert expected_dates == [
        query.departure_date for query
        in make_train_queries(station_from, station_to, dt_from, dt_to,
                              False, None, None, {})
    ]


@pytest.mark.parametrize('tz, expected_dates', [
    ['Asia/Yekaterinburg', [date(2015, 1, 10)]],
    ['Europe/Moscow', [date(2015, 1, 10)]],
])
def test_make_queries_local_tz(tz, expected_dates):
    station_from = create_station(country=Country.RUSSIA_ID, time_zone=tz, settlement=None)
    station_to = create_station()

    dt_from = station_from.pytz.localize(datetime(2015, 1, 10, 0))
    dt_to = dt_from + timedelta(1)

    assert expected_dates == [
        query.departure_date for query
        in make_train_local_queries(station_from, station_to, dt_from, dt_to,
                                    True, None, None, {})
    ]


@pytest.mark.parametrize('departure_min_dt, departure_max_dt, expected_dates', (
    (datetime(2000, 1, 1), datetime(2000, 1, 1, 12), [date(2000, 1, 1)]),
    (datetime(2000, 1, 1, 12), datetime(2000, 1, 3), [date(2000, 1, 1), date(2000, 1, 2)]),
    (datetime(2000, 1, 1, 12), datetime(2000, 1, 3, 10), [date(2000, 1, 1), date(2000, 1, 2), date(2000, 1, 3)]),
    (datetime(2000, 1, 1, 12), datetime(2000, 1, 1, 12), []),
    (datetime(2000, 1, 2), datetime(2000, 1, 1), [])
))
def test_make_queries_dates(departure_min_dt, departure_max_dt, expected_dates):
    departure_point = create_station(country=Country.RUSSIA_ID)
    arrival_point = create_station()
    queries = make_train_queries(
        departure_point,
        arrival_point,
        departure_point.pytz.localize(departure_min_dt),
        departure_point.pytz.localize(departure_max_dt),
        False, None, None, {}
    )

    assert [query.departure_date for query in queries] == expected_dates


@pytest.mark.parametrize('departure_min_dt, departure_max_dt, expected_dates', (
    (datetime(2000, 1, 1), datetime(2000, 1, 1, 12), [date(2000, 1, 1)]),
    (datetime(2000, 1, 1, 12), datetime(2000, 1, 3), [date(2000, 1, 1), date(2000, 1, 2)]),
    (datetime(2000, 1, 1, 12), datetime(2000, 1, 3, 10), [date(2000, 1, 1), date(2000, 1, 2), date(2000, 1, 3)]),
    (datetime(2000, 1, 1, 12), datetime(2000, 1, 1, 12), []),
    (datetime(2000, 1, 2), datetime(2000, 1, 1), [])
))
def test_make_local_queries_dates(departure_min_dt, departure_max_dt, expected_dates):
    departure_point = create_station(country=Country.RUSSIA_ID)
    arrival_point = create_station()
    queries = make_train_local_queries(
        departure_point,
        arrival_point,
        departure_point.pytz.localize(departure_min_dt),
        departure_point.pytz.localize(departure_max_dt),
        True, None, None, {}
    )

    assert [query.departure_date for query in queries] == expected_dates
