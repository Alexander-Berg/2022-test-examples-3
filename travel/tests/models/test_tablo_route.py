# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta

import pytest
from django.utils import translation
from freezegun import freeze_time

from common.utils.date import FuzzyDateTime, UTC_TZ
from common.tester.factories import create_settlement
from common.tester.utils.datetime import replace_now
from stationschedule.models import TabloRoute
from stationschedule.tester.factories import create_ztablo


DEFAULT_DT = datetime(2000, 1, 1)


def make_departure_route(z_tablo=None):
    return TabloRoute(create_ztablo(z_tablo or {}), 'departure')


@pytest.mark.dbuser
def test_factory():
    with translation.override('ru'):
        tablo_route = make_departure_route()

    assert tablo_route.event == 'departure'
    assert tablo_route.status_code == 'nodata'
    assert tablo_route.status == 'нет данных'
    assert tablo_route.start_date is None

    # проверка start_date
    assert make_departure_route({'start_datetime': DEFAULT_DT}).start_date == DEFAULT_DT.date()


@pytest.mark.parametrize('z_tablo, expected', ((
    {},
    None
), (
    {
        'number': 'z_tablo number'
    },
    'z_tablo number'
), (
    {
        'number': 'z_tablo number',
        'thread': {'number': 'thread number'}
    },
    'thread number'
), (
    {
        'number': 'z_tablo number',
        'thread': {'number': 'thread number'},
        'merged_flight': {'number': 'merged_z_tablo number'},
    },
    'z_tablo number/merged_z_tablo number'
)))
@pytest.mark.dbuser
def test_factory_number(z_tablo, expected):
    tablo_route = make_departure_route(z_tablo)
    assert tablo_route.number == expected


COMPANY_ID = 1000


@pytest.mark.parametrize('z_tablo, expected', ((
    {},
    None
), (
    {
        'company': {'id': COMPANY_ID}
    },
    COMPANY_ID
), (
    {
        'company': {'id': COMPANY_ID},
        'thread': {'company': {}}
    },
    COMPANY_ID
), (
    {
        'thread': {}
    },
    None
), (
    {
        'thread': {'company': {'id': COMPANY_ID}}
    },
    COMPANY_ID
)))
@pytest.mark.dbuser
def test_factory_company(z_tablo, expected):
    tablo_route = make_departure_route(z_tablo)
    company_id = tablo_route.company and tablo_route.company.id

    assert company_id == expected


T_MODEL_ID = 1000


@pytest.mark.parametrize('z_tablo, expected', ((
    {},
    None
), (
    {
        'rtstation': {'thread': {}, 'station': {}}
    },
    None
), (
    {
        'thread': {}
    },
    None
), (
    {
        'rtstation': {'thread': {}, 'station': {}, 'departure_t_model': {'id': T_MODEL_ID}}
    },
    T_MODEL_ID
), (
    {
        'thread': {'t_model': {'id': T_MODEL_ID}}
    },
    T_MODEL_ID
), (
    {
        'rtstation': {'thread': {}, 'station': {}, 'departure_t_model': {'id': T_MODEL_ID}},
        'thread': {'t_model': {}},
    },
    T_MODEL_ID
)))
@pytest.mark.dbuser
def test_factory_t_model(z_tablo, expected):
    tablo_route = make_departure_route(z_tablo)
    t_model_id = tablo_route.t_model and tablo_route.t_model.id

    assert t_model_id == expected


@pytest.mark.dbuser
def test_factory_settlement():
    tablo_route = make_departure_route({'station': {'settlement': None}})

    assert tablo_route.settlement is None

    settlement = create_settlement()
    tablo_route = make_departure_route({'station': {'settlement': settlement}, 'title': ''})

    assert tablo_route.settlement == settlement


@pytest.mark.parametrize('z_tablo, expected', ((
    {},
    (None, None)
), (
    {
        'station': {'settlement': {'title': 'Дубаи'}},
        'title': 'Сидней - Лондон',
    },
    ('Сидней', 'Лондон')
), (
    {
        'station': {'settlement': {'title': 'Лондон'}},
        'title': 'Сидней - Лондон',
    },
    ('Сидней', None)
), (
    {
        'station': {'settlement': {'title': 'Сидней'}},
        'title': 'Сидней - Лондон',
    },
    (None, 'Лондон')
), (
    {
        'station': {'settlement': {'title': 'Дубаи'}},
        'title': 'Сидней - Дубаи - Лондон',
    },
    ('Сидней', None)
), (
    {
        'station': {'settlement': {'title': 'Дубаи'}},
        'title': 'Дубаи',
    },
    (None, None)
)))
@pytest.mark.dbuser
def test_factory_cities(z_tablo, expected):
    tablo_route = make_departure_route(z_tablo)

    assert (tablo_route.prev_city, tablo_route.next_city) == expected


@pytest.mark.dbuser
def test_factory_datetimes():
    tablo_route = make_departure_route({
        'station': {'time_zone': 'UTC'},
        'start_datetime': datetime(2000, 1, 1, 1),
        'arrival': datetime(2000, 1, 1, 2),
        'departure': datetime(2000, 1, 1, 3),
        'real_arrival': datetime(2000, 1, 1, 4),
        'real_departure': datetime(2000, 1, 1, 5),
    })

    assert tablo_route.naive_start_dt == datetime(2000, 1, 1, 1)
    assert tablo_route.arrival_planned_dt == datetime(2000, 1, 1, 2, tzinfo=UTC_TZ)
    assert tablo_route.event_planned_dt == tablo_route.departure_planned_dt == datetime(2000, 1, 1, 3, tzinfo=UTC_TZ)
    assert tablo_route.arrival_dt == datetime(2000, 1, 1, 4, tzinfo=UTC_TZ)
    assert tablo_route.event_dt == tablo_route.departure_dt == datetime(2000, 1, 1, 5, tzinfo=UTC_TZ)


@pytest.mark.dbuser
def test_factory_datetimes_fuzzy():
    tablo_route = make_departure_route({
        'station': {'time_zone': 'UTC'},
        'start_datetime': datetime(2000, 1, 1, 1),
        'arrival': datetime(2000, 1, 1, 2),
        'departure': datetime(2000, 1, 1, 3),
        'real_arrival': datetime(2000, 1, 1, 4),
        'real_departure': datetime(2000, 1, 1, 5),
        'is_fuzzy': True
    })

    assert tablo_route.arrival_planned_dt == FuzzyDateTime(datetime(2000, 1, 1, 2, tzinfo=UTC_TZ))
    assert tablo_route.event_planned_dt == tablo_route.departure_planned_dt == FuzzyDateTime(datetime(2000, 1, 1, 3, tzinfo=UTC_TZ))
    assert tablo_route.arrival_dt == FuzzyDateTime(datetime(2000, 1, 1, 4, tzinfo=UTC_TZ))
    assert tablo_route.event_dt == tablo_route.departure_dt == FuzzyDateTime(datetime(2000, 1, 1, 5, tzinfo=UTC_TZ))


@pytest.mark.parametrize('z_tablo, now, expected', ((
    {},
    DEFAULT_DT,
    False
), (
    {'departure': DEFAULT_DT, 'real_departure': DEFAULT_DT},
    DEFAULT_DT,
    True
), (
    {'departure': DEFAULT_DT, 'real_departure': DEFAULT_DT},
    DEFAULT_DT - timedelta(minutes=31),
    False
), (
    {'departure': DEFAULT_DT, 'real_departure': DEFAULT_DT + timedelta(minutes=1)},
    DEFAULT_DT - timedelta(minutes=31),
    True
)))
@pytest.mark.dbuser
def test_has_actual_dt(z_tablo, now, expected):
    with freeze_time(now):  # это нужно из-за auto_now=True у lmt
        tablo_route = make_departure_route(z_tablo)

    with replace_now(DEFAULT_DT):
        assert tablo_route.has_actual_dt() == expected
