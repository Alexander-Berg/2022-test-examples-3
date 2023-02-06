# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import date, datetime, time, timedelta
from urllib import urlencode

import mock
import pytest
import pytz
from dateutil import parser
from django.test import Client
from hamcrest import has_entries, assert_that, contains

import travel.rasp.morda_backend.morda_backend.thread.data_layer
from common.apps.suburban_events.factories import EventStateFactory, ThreadStationKeyFactory, ThreadStationStateFactory
from common.apps.suburban_events.models import SuburbanKey
from common.apps.suburban_events.utils import EventStateType, get_rtstation_key
from common.data_api.platforms.instance import platforms as platforms_client
from common.data_api.platforms.serialization import PlatformData, PlatformKey, PlatformRecord
from common.models.geo import Country
from common.models.schedule import RThreadType, RTStation, TrainPurchaseNumber, DeLuxeTrain
from common.models.transport import TransportType
from common.tester.factories import (
    create_thread, create_station, create_company, create_settlement, create_supplier, create_deluxe_train,
    create_transport_subtype
)
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting


pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]


def get_thread_map_response(query, lang='ru'):
    qs = urlencode(query)
    response = Client().get('/{lang}/thread/map/?{qs}'.format(
        lang=lang,
        qs=qs
    ))
    return response.status_code, json.loads(response.content)


@replace_now('2018-11-20')
@replace_setting('PRELOAD_MAPPING_GEOMETRY', True)
@pytest.mark.parametrize('map_data, expected_status_code, expected_errors', (
    ({'stations': [], 'segments': [], 'position': None}, 200, {}),
    ({}, 404, ['Cannot draw a map for the requested thread']),
))
def test_thread_map_view(map_data, expected_status_code, expected_errors):
    thread_tzinfo = pytz.timezone('Etc/GMT-4')
    station_from = create_station(title='from', id=111, time_zone=thread_tzinfo)
    station_to = create_station(title='to', id=222, time_zone=thread_tzinfo)
    departure_date = datetime(2018, 11, 20)
    thread_start_time = time(1, 5)
    departure_shift = 11
    thread = create_thread(
        uid='123',
        t_type=TransportType.SUBURBAN_ID,
        year_days=[datetime(2018, 11, 5), departure_date],
        tz_start_time=thread_start_time,
        schedule_v1=[
            [None, 0, station_from, {'time_zone': thread_tzinfo}],
            [departure_shift, None, station_to, {'time_zone': thread_tzinfo}],
        ]
    )

    if map_data:
        mock_draw_path_result = mock.Mock()
        mock_draw_path_result.__json__ = mock.Mock(return_value=map_data)
    else:
        mock_draw_path_result = map_data

    with mock.patch.object(
        travel.rasp.morda_backend.morda_backend.thread.data_layer, 'draw_path',
        return_value=mock_draw_path_result
    ) as mock_draw_path:
        status_code, response = get_thread_map_response(query={
            'uid': thread.uid,
            'departure': departure_date.date(),
            'station_from': station_from.id,
            'station_to': station_to.id,
            'time_zone': 'Etc/GMT-3'
        })

    assert status_code == expected_status_code
    assert response['errors'] == expected_errors

    mock_draw_path.assert_called_once()
    draw_path_kwargs = mock_draw_path.call_args_list[0][1]
    assert_that(draw_path_kwargs, has_entries({
        'thread': thread,
        'thread_start_dt': datetime.combine(departure_date.date(), thread_start_time),
        'path': list(thread.path),
        'first': station_from,
        'last': station_to,
    }))


@replace_now('2019-01-01')
@replace_setting('PRELOAD_MAPPING_GEOMETRY', False)
def test_thread_map_bad_responses():
    status_code, response = get_thread_map_response(query={
        'uid': '123',
        'departure': '2019-01-01',
    })

    assert status_code == 404
    assert_that(response, has_entries({
        'errors': ['This instance is not for geometry requests']
    }))

    status_code, response = get_thread_map_response(query={
        'mixed_uid': 'tr_1',
        'departure_from': '2018-11-20',
    })

    assert status_code == 400
    assert_that(response, has_entries({
        'errors': {
            'departure_from': [u'station_from should be set with departure_from together'],
            'station_from': [u'station_from should be set with departure_from together']
        }
    }))


def get_thread_response(query, code=200, lang='ru'):
    qs = urlencode(query)
    response = Client().get('/{lang}/thread/?{qs}'.format(
        lang=lang,
        qs=qs
    ))
    assert response.status_code == code

    return json.loads(response.content)


@replace_now('2018-11-20')
def test_thread_view():
    station_from = create_station(title='from', id=666)
    station_to = create_station(title='to', id=667)

    create_thread(
        id=1,
        uid='123',
        number='456',
        t_type=TransportType.WATER_ID,
        year_days=[datetime(2018, 11, 5), datetime(2018, 11, 20)],
        schedule_v1=[
            [None, 0, station_from],
            [10, None, station_to],
        ]
    )

    response = get_thread_response(query={'uid': '123', 'country': 'RU'})

    assert_that(response['result'], has_entries({
        'thread': has_entries({
            'runDays': {
                '2018': {
                    '11': [0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
                }
            },
            'daysText': 'только 5, 20 ноября'
        }),
        'rtstations': contains(
            has_entries({
                'title': 'from',
                'departureLocalDt': '2018-11-20T00:00:00+03:00',
                'isStationFrom': True,
                'isStationTo': False,
                'capitalTimeOffset': '+00:00',
            }),
            has_entries({
                'title': 'to',
                'arrivalLocalDt': '2018-11-20T00:10:00+03:00',
                'isStationFrom': False,
                'isStationTo': True,
                'capitalTimeOffset': '+00:00',
            })
        )
    }))


@replace_now('2018-11-20')
@pytest.mark.parametrize('query, expected_status_code, expected_errors', (
    (
        {'departure': '2018-11-20'},
        400, {
            'mixed_uid': [u'uid or mixed_uid should be in the request params'],
            'uid': [u'uid or mixed_uid should be in the request params']
        }
    ),
    (
        {'mixed_uid': 'tr_1', 'departure_from': '2018-11-20'},
        400, {
            'departure_from': [u'station_from should be set with departure_from together'],
            'station_from': [u'station_from should be set with departure_from together']
        }
    ),
    (
        {'mixed_uid': 'tr_xxx', 'departure': '2018-11-20'},
        404, ['Нитки с uid tr_xxx нет в базе']
    ),
    (
        {'mixed_uid': 'tr_1', 'station_from': 201, 'departure_from': '2018-11-20'},
        404, ['Нитка с uid tr_1 не подходит под параметры запроса']
    ),
    (
        {'mixed_uid': 'tr_1', 'station_from': 100, 'station_to': 201, 'departure_from': '2018-11-20'},
        404, ['Нитка с uid tr_1 не подходит под параметры запроса']
    ),
    (
        {'mixed_uid': 'R_1', 'departure': '2018-11-20'},
        404, ['Ниток с canonical_uid R_1 нет в базе']
    ),
    (
        {'mixed_uid': 'tr_1', 'station_from': 100, 'station_to': 201, 'departure_from': '2018-05-20'},
        404, ['В БД нет информации об отправлениях на дату: 2018-05-20']
    ),
    (
        {'mixed_uid': 'tr_1', 'departure': '2020-05-20'},
        404, ['В БД нет информации об отправлениях на дату: 2020-05-20']
    ),
))
def test_thread_view_not_found(query, expected_status_code, expected_errors):
    stations = [create_station(title='station_{}'.format(i), id=i) for i in [100, 200, 201, 900]]

    def st_by_id(station_id):
        return next(st for st in stations if st.id == station_id)

    create_thread(
        id=1,
        uid='tr_1',
        number='456',
        t_type=TransportType.TRAIN_ID,
        year_days=[datetime(2018, 11, 5), datetime(2018, 11, 20)],
        schedule_v1=[
            [None, 0, st_by_id(100)],
            [60, 75, st_by_id(200)],
            [120, None, st_by_id(900)],
        ]
    )

    response = get_thread_response(query, expected_status_code)

    assert response['errors'] == expected_errors


@replace_now('2018-11-20')
def test_thread_view_departure():
    station_from = create_station(title='from', id=666, slug='s666', t_type='water', type_choices='schedule')
    station_middle = create_station(title='middle', id=667, slug='s667', t_type='water', type_choices='schedule')
    station_to = create_station(title='to', id=668, slug='s668', t_type='water', type_choices='schedule')
    company = create_company(title='company', address='address', url='/url/', email='@@@', phone='777', strange=True)

    thread = create_thread(
        id=1,
        uid='123',
        canonical_uid='T_123',
        number='456',
        title='вода',
        company=company,
        t_type=TransportType.WATER_ID,
        year_days=[datetime(2018, 11, 5), datetime(2018, 11, 20)],
        schedule_v1=[
            [None, 0, station_from],
            [10, 15, station_middle],
            [30, None, station_to],
        ]
    )

    rts_from = thread.path[0]
    rts_from.platform = 'платформа'
    rts_from.save()

    platforms_client.update([
        PlatformRecord(
            key=PlatformKey(date=date(2018, 11, 5), station_id=station_middle.id, train_number=thread.number),
            data=PlatformData(departure_platform='platform_mid_d', arrival_platform='platform_mid_a')
        ),
        PlatformRecord(
            key=PlatformKey(date=date(2018, 11, 5), station_id=station_to.id, train_number=thread.number),
            data=PlatformData(departure_platform='platform_to_d', arrival_platform='platform_to_a')
        ),
    ])

    response = get_thread_response(query={'uid': '123', 'departure': '2018-11-05'})

    assert_that(response['result'], has_entries({
        'thread': has_entries({
            'transportType': 'water',
            'uid': '123',
            'canonicalUid': 'T_123',
            'fullTitle': 'Теплоход 456 вода',
            'title': 'вода',
            'company': has_entries({
                'url': '/url/',
                'phone': '777',
                'title': 'company',
                'email': '@@@',
                'address': 'address',
                'description': None,
                'hidden': False,
                'strange': True
            }),
            'number': '456',
            'capitalSlug': 'Moscow',
            'capitalTimezone': 'Europe/Moscow',
            'runDays': {
                '2018': {
                    '11': [0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
                }
            },
            'daysText': 'только 5, 20 ноября',
            'isInterval': False,
            'isNoChangeWagon': False,
            'comment': '',
            'fromStationDepartureLocalDt': '2018-11-05T00:00:00+03:00',
            'capitals': [],
        }),
        'rtstations': contains(
            {
                'title': 'from',
                'url': '/station/666',
                'departureLocalDt': '2018-11-05T00:00:00+03:00',
                'platform': 'платформа',
                'id': 666,
                'slug': 's666',
                'timezone': 'Europe/Moscow',
                'isFuzzy': False,
                'isTechnicalStop': False,
                'hidden': False,
                'isNoStop': False,
                'isCombined': False,
                'settlement': {},
                'country': {},
                'isStationFrom': True,
                'isStationTo': False,
                'capitalTimeOffset': '+00:00',
                'pageType': 'water',
                'mainSubtype': 'schedule'
            },
            {
                'title': 'middle',
                'url': '/station/667',
                'arrivalLocalDt': '2018-11-05T00:10:00+03:00',
                'departureLocalDt': '2018-11-05T00:15:00+03:00',
                'isFuzzy': False,
                'platform': 'platform_mid_d',
                'id': 667,
                'slug': 's667',
                'timezone': 'Europe/Moscow',
                'isTechnicalStop': False,
                'hidden': False,
                'isNoStop': False,
                'isCombined': False,
                'settlement': {},
                'country': {},
                'isStationFrom': False,
                'isStationTo': False,
                'capitalTimeOffset': '+00:00',
                'pageType': 'water',
                'mainSubtype': 'schedule'
            },
            {
                'title': 'to',
                'url': '/station/668',
                'arrivalLocalDt': '2018-11-05T00:30:00+03:00',
                'isFuzzy': False,
                'platform': 'platform_to_a',
                'id': 668,
                'slug': 's668',
                'timezone': 'Europe/Moscow',
                'isTechnicalStop': False,
                'hidden': False,
                'isNoStop': False,
                'isCombined': False,
                'settlement': {},
                'country': {},
                'isStationFrom': False,
                'isStationTo': True,
                'capitalTimeOffset': '+00:00',
                'pageType': 'water',
                'mainSubtype': 'schedule'
            }
        )
    }))


@replace_now('2018-11-20')
def test_thread_view_departure_from():
    stations = [create_station(title='station_{}'.format(i), id=i) for i in range(666, 669)]

    create_thread(
        id=1,
        uid='123',
        number='456',
        t_type=TransportType.WATER_ID,
        tz_start_time=time(23),
        year_days=[datetime(2018, 11, 5), datetime(2018, 11, 20)],
        schedule_v1=[
            [None, 0, stations[0]],
            [120, 130, stations[1]],
            [180, None, stations[2]],
        ]
    )

    response = get_thread_response(query={'uid': '123', 'departure': '2018-11-05',
                                          'departure_from': '2018-11-20T23:00', 'station_from': stations[0].id,
                                          'country': 'RU'})

    assert_that(response['result'], has_entries({
        'thread': has_entries({
            'runDays': {
                '2018': {
                    '11': [0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
                }
            },
            'daysText': 'только 5, 20 ноября',
            'fromStationDepartureLocalDt': '2018-11-20T23:00:00+03:00',
        }),
        'rtstations': contains(
            has_entries({
                'title': 'station_666',
                'departureLocalDt': '2018-11-20T23:00:00+03:00',
                'isStationFrom': True,
                'isStationTo': False,
            }),
            has_entries({
                'title': 'station_667',
                'arrivalLocalDt': '2018-11-21T01:00:00+03:00',
                'departureLocalDt': '2018-11-21T01:10:00+03:00',
                'isStationFrom': False,
                'isStationTo': False,
            }),
            has_entries({
                'title': 'station_668',
                'arrivalLocalDt': '2018-11-21T02:00:00+03:00',
                'isStationFrom': False,
                'isStationTo': True,
            })
        )
    }))

    response = get_thread_response(query={'uid': '123', 'departure': '2018-11-05', 'departure_from': '2018-11-21T01:10',
                                          'country': 'RU', 'station_from': '667'})

    assert_that(response['result'], has_entries({
        'thread': has_entries({
            'runDays': {
                '2018': {
                    '11': [0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0]
                }
            },
            'daysText': 'только 6, 21 ноября',
            'fromStationDepartureLocalDt': '2018-11-21T01:10:00+03:00',
        }),
        'rtstations': contains(
            has_entries({
                'title': 'station_666',
                'departureLocalDt': '2018-11-20T23:00:00+03:00',
                'isStationFrom': False,
                'isStationTo': False,
            }),
            has_entries({
                'title': 'station_667',
                'arrivalLocalDt': '2018-11-21T01:00:00+03:00',
                'departureLocalDt': '2018-11-21T01:10:00+03:00',
                'isStationFrom': True,
                'isStationTo': False,
            }),
            has_entries({
                'title': 'station_668',
                'arrivalLocalDt': '2018-11-21T02:00:00+03:00',
                'isStationFrom': False,
                'isStationTo': True,
            })
        )
    }))


@replace_now('2018-11-20')
@pytest.mark.parametrize(
    'query, expected_start, expected_run_days', [
        [  # first thread, default start = today
            {'uid': 't1'},
            '2018-11-20',
            {'2018': {
                '11': [0, 0, 0, 0, 1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 2, 0, 0, 1, 0, 2, 0, 0, 0]}}
        ],
        [  # second thread, default start = the day after tomorrow
            {'uid': 't2'},
            '2018-11-22',
            {'2018': {
                '11': [0, 0, 0, 0, 1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 2, 0, 0, 1, 0, 2, 0, 0, 0]}}
        ],
        [  # route (all the threads), default start = today
            {'mixed_uid': 'R_canonical_uid'},
            '2018-11-20',
            {'2018': {
                '11': [0, 0, 0, 0, 1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 2, 0, 0, 1, 0, 2, 0, 0, 0]}}
        ],
        [  # first thread, default start = today, but calendar for the route (all the threads)
            {'mixed_uid': 't1'},
            '2018-11-20',
            {'2018': {
                '11': [0, 0, 0, 0, 1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 2, 0, 0, 1, 0, 2, 0, 0, 0]}}
        ],
        [  # first thread, for the second station, exact departure
            {'uid': 't1', 'station_from': 101, 'departure_from': '2018-11-21T23:30:00'},
            '2018-11-20',
            {'2018': {
                '11': [0, 0, 0, 0, 0, 1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 2, 0, 0, 1, 0, 2, 0, 0]}}
        ],
        [  # route (all the threads), for the second station, exact departure
            {'mixed_uid': 'R_canonical_uid', 'station_from': 101, 'departure_from': '2018-11-21T23:30:00'},
            '2018-11-20',
            {'2018': {
                '11': [0, 0, 0, 0, 0, 1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 2, 0, 0, 1, 0, 2, 0, 0]}}
        ],
        [  # route (all the threads), for the second station, departure day
            {'mixed_uid': 'R_canonical_uid', 'station_from': 101, 'departure_from': '2018-11-21'},
            '2018-11-20',
            {'2018': {
                '11': [0, 0, 0, 0, 0, 1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 2, 0, 0, 1, 0, 2, 0, 0]}}
        ],
        [  # first thread, for the second station, departure day
            {'mixed_uid': 't1', 'station_from': 101, 'departure_from': '2018-11-21'},
            '2018-11-20',
            {'2018': {
                '11': [0, 0, 0, 0, 0, 1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 2, 0, 0, 1, 0, 2, 0, 0]}}
        ],
        [  # route (all the threads), for the second station, departure day in future
            {'mixed_uid': 'R_canonical_uid', 'station_from': 101, 'departure_from': '2018-11-26'},
            '2018-11-25',
            {'2018': {
                '11': [0, 0, 0, 0, 0, 1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 2, 0, 0, 1, 0, 2, 0, 0]}}
        ],
        [  # first thread, for the second station, departure day in future
            {'mixed_uid': 't1', 'station_from': 101, 'departure_from': '2018-11-26'},
            '2018-11-25',
            {'2018': {
                '11': [0, 0, 0, 0, 0, 1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 2, 0, 0, 1, 0, 2, 0, 0]}}
        ],
    ]
)
def test_thread_view_departure_from_date(query, expected_start, expected_run_days):
    stations = [create_station(title='station_{}'.format(i), id=i) for i in range(100, 104)]
    station_shifts = [1440 * i for i in range(0, 4)]

    tz_start_time = time(23)
    stop_duration = 30
    create_thread(
        id=1,
        uid='t1',
        canonical_uid='R_canonical_uid',
        number='N456_1',
        t_type=TransportType.SUBURBAN_ID,
        tz_start_time=tz_start_time,
        year_days=[datetime(2018, 11, 5), datetime(2018, 11, 20), datetime(2018, 11, 25)],
        schedule_v1=[
            [None, station_shifts[0], stations[0]],
            [station_shifts[1], station_shifts[1] + stop_duration, stations[1]],
            [station_shifts[2], station_shifts[2] + stop_duration, stations[2]],
            [station_shifts[3], None, stations[3]],
        ]
    )
    create_thread(
        id=2,
        uid='t2',
        canonical_uid='R_canonical_uid',
        number='N456_2',
        t_type=TransportType.SUBURBAN_ID,
        tz_start_time=tz_start_time,
        year_days=[datetime(2018, 11, 7), datetime(2018, 11, 22), datetime(2018, 11, 27)],
        schedule_v1=[
            [None, station_shifts[0], stations[0]],
            [station_shifts[1], station_shifts[1] + stop_duration, stations[1]],
            [station_shifts[2], station_shifts[2] + stop_duration, stations[2]],
            [station_shifts[3], None, stations[3]],
        ]
    )

    response = get_thread_response(query=query)

    expected_start_date = parser.parse(expected_start)
    expected_dates = [(expected_start_date + timedelta(days=i)).date() for i in range(0, 4)]
    assert_that(response['result'], has_entries({
        'thread': has_entries({
            'runDays': has_entries(expected_run_days)
        }),
        'rtstations': contains(
            has_entries({
                'title': 'station_100',
                'departureLocalDt': '{}T23:00:00+03:00'.format(expected_dates[0]),
            }),
            has_entries({
                'title': 'station_101',
                'arrivalLocalDt': '{}T23:00:00+03:00'.format(expected_dates[1]),
                'departureLocalDt': '{}T23:30:00+03:00'.format(expected_dates[1]),
            }),
            has_entries({
                'title': 'station_102',
                'arrivalLocalDt': '{}T23:00:00+03:00'.format(expected_dates[2]),
                'departureLocalDt': '{}T23:30:00+03:00'.format(expected_dates[2]),
            }),
            has_entries({
                'title': 'station_103',
                'arrivalLocalDt': '{}T23:00:00+03:00'.format(expected_dates[3]),
            })
        )
    }))


@replace_now('2018-11-20')
def test_bus_thread_view():
    settlement_from = create_settlement(id=66, title='settlement_66')
    russia = Country.objects.get(id=Country.RUSSIA_ID)
    station_from = create_station(title='from', id=666, settlement=settlement_from, country=russia)
    station_to = create_station(title='to', id=667)

    create_thread(
        uid='123',
        number='456',
        t_type=TransportType.BUS_ID,
        year_days=[datetime(2018, 11, 5), datetime(2018, 11, 20)],
        schedule_v1=[
            [None, 0, station_from],
            [10, None, station_to],
        ]
    )

    response = get_thread_response(query={'uid': '123'})

    assert_that(response['result'], has_entries({
        'thread': has_entries({
            'transportType': 'bus',
            'tariffsKeys': [
                'ybus s666 00:00 s667 456',
                'ybus s666 00:00 s667',
                'static 666 667 123'
            ],
            'isSuburbanBus': False
        }),
        'rtstations': contains(
            has_entries({
                'settlement': {
                    'id': 66,
                    'title': 'settlement_66'
                },
                'country': {
                    'id': 225,
                    'code': 'RU',
                    'title': 'Россия'
                }
            }),
            has_entries()
        )
    }))


@replace_now('2018-11-20')
def test_bus_thread_departure_from_station_from_view():
    station_from = create_station(title='from', id=666)
    station_to = create_station(title='to', id=667)
    supplier = create_supplier(id=456)

    create_thread(
        supplier=supplier,
        uid='123',
        number='456',
        t_type=TransportType.BUS_ID,
        tz_start_time=time(23),
        year_days=[datetime(2018, 11, 5), datetime(2018, 11, 20)],
        schedule_v1=[
            [None, 0, station_from],
            [10, None, station_to],
        ]
    )

    with replace_setting('SUBURBAN_BUS_SUPPLIER', {456}):
        response = get_thread_response(query={
            'uid': '123', 'departure_from': '2018-11-20T23:00', 'station_from': '666'
        })

    assert_that(response['result'], has_entries({
        'thread': has_entries({
            'transportType': 'bus',
            'tariffsKeys': [
                'ybus s666 2018-11-20T23:00 s667 456',
                'ybus s666 2018-11-20T23:00 s667',
                'static 666 667 123 1120'
            ],
            'isSuburbanBus': True
        })
    }))


@replace_now('2018-11-20')
def test_interval_thread_view():
    station_from = create_station(title='from', id=666)
    station_to = create_station(title='to', id=667)

    uid = '123'
    begin_time = time(7, 30)
    end_time = time(23, 15)
    density = 'нло раз в 11 минут'
    comment = 'с интегрированной машиной времени'

    create_thread(
        uid=uid,
        type=RThreadType.INTERVAL_ID,
        number='456',
        t_type=TransportType.BUS_ID,
        year_days=[datetime(2018, 11, 5), datetime(2018, 11, 20)],
        comment=comment,
        begin_time=begin_time,
        end_time=end_time,
        density=density,
        schedule_v1=[
            [None, 0, station_from, {'is_fuzzy': False}],
            [10, None, station_to, {'is_fuzzy': True}],
        ]
    )

    response = get_thread_response(query={'uid': uid})

    assert_that(response['result'], has_entries({
        'thread': has_entries({
            'transportType': 'bus',
            'isSuburbanBus': False,
            'comment': comment,
            'isInterval': True,
            'beginTime': begin_time.isoformat(),
            'endTime': end_time.isoformat(),
            'density': density,
        }),
        'rtstations': contains(
            has_entries({
                'isFuzzy': False
            }),
            has_entries({
                'isFuzzy': True,
            })
        )
    }))


@replace_now('2018-11-20')
def test_train_thread_view():
    station_from = create_station(id=666)
    station_to = create_station(id=667)
    create_thread(
        uid='123',
        number='456',
        t_type=TransportType.TRAIN_ID,
        tz_start_time=time(16),
        year_days=[datetime(2018, 11, 20)],
        schedule_v1=[
            [None, 0, station_from],
            [10, None, station_to],
        ]
    )
    create_deluxe_train(deluxe=True, title_ru='Самый-самый', numbers='456')
    DeLuxeTrain._number2deluxe_train = {}

    response = get_thread_response(query={'uid': '123'})
    assert_that(response['result'], has_entries({
        'thread': has_entries({
            'transportType': 'train',
            'deluxeTrain': has_entries({
                'isDeluxe': True,
                'title': 'Самый-самый',
                'shortTitle': 'фирменный «Самый-самый»'
            }),
            'tariffsKeys': [
                'static 666 667 123',
                'train 456 20181120_1600',
                'train 455 20181120_1600'
            ]
        })
    }))

    for query in [
        {'departure': '2018-11-20'},
        {'departure_from': '2018-11-20', 'station_from': 666},
        {'departure_from': '2018-11-20T16:00:00', 'station_from': 666}
    ]:
        query['uid'] = '123'
        response = get_thread_response(query=query)
        assert_that(response['result'], has_entries({
            'thread': has_entries({
                'transportType': 'train',
                'tariffsKeys': [
                    'static 666 667 123 1120',
                    'train 456 20181120_1600',
                    'train 455 20181120_1600'
                ]
            })
        }))


@replace_now('2019-05-01')
def test_train_thread_view_selector():
    st1 = create_station()
    st2 = create_station()

    create_thread(
        id=1, uid='1',
        canonical_uid='R_1',
        title='thread',
        t_type=TransportType.TRAIN_ID,
        tz_start_time=time(0, 0),
        year_days=[datetime(2019, 5, 1), datetime(2019, 5, 3)],
        type=RThreadType.BASIC_ID,
        schedule_v1=[
            [None, 0, st1],
            [60, None, st2],
        ]
    )
    create_thread(
        id=2, uid='2',
        canonical_uid='R_1',
        title='thread',
        t_type=TransportType.TRAIN_ID,
        tz_start_time=time(0, 0),
        year_days=[datetime(2019, 5, 2)],
        type=RThreadType.BASIC_ID,
        schedule_v1=[
            [None, 0, st1],
            [60, None, st2],
        ]
    )
    create_thread(
        id=3, uid='3',
        canonical_uid='R_1',
        title='thread',
        t_type=TransportType.TRAIN_ID,
        tz_start_time=time(0, 0),
        year_days=[datetime(2019, 5, 4)],
        translated_manual_days_texts='{"0": {"ru": "четвертого числа"}}',
        type=RThreadType.BASIC_ID,
        schedule_v1=[
            [None, 0, st1],
            [60, None, st2],
        ]
    )

    response = get_thread_response(query={'mixed_uid': 'R_1'})

    expected_run_days = {
        '2019': {'5': [1, 1, 1, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]}
    }

    assert_that(response['result'], has_entries({
        'thread': has_entries({
            'runDays': has_entries(expected_run_days),
            'daysText': 'только 1, 3 мая'
        }),
        'threads': has_entries({
            '1': has_entries({
                'uid': '1',
                'type': 'basic',
                'startDepartureTime': '00:00',
                'stopArrivalTime': '01:00',
                'firstDeparture': '2019-05-01',
                'daysText': 'только 1, 2, 3 мая',
            }),
            '3': has_entries({
                'uid': '3',
                'type': 'basic',
                'startDepartureTime': '00:00',
                'stopArrivalTime': '01:00',
                'firstDeparture': '2019-05-04',
                'daysText': 'четвертого числа',
            })
        }),
        'relatedThreads': []
    }))


@replace_now('2018-11-20')
@pytest.mark.parametrize(
    'query, expected_run_days', [
        [  # by uid, default start = today
            {'mixed_uid': 't1'},
            [0, 0, 0, 0, 4, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 3, 0, 0, 1, 0, 4, 0, 0, 0],
        ],
        [  # by canonical uid, default start = today
            {'mixed_uid': 'R_canonical'},
            [0, 0, 0, 0, 4, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 3, 0, 0, 1, 0, 4, 0, 0, 0],
        ],
        [  # by canonical uid, departure - day 22
            {'mixed_uid': 'R_canonical', 'departure': '2018-11-22'},
            [0, 0, 0, 0, 4, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 3, 0, 0, 1, 0, 4, 0, 0, 0],
        ],
        [  # by canonical uid, departure from second station - 25
            {'mixed_uid': 'R_canonical', 'departure_from': '2018-11-25', 'station_from': '101', 'station_to': '103'},
            [0, 0, 0, 0, 4, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 3, 0, 0, 1, 0, 4, 0, 0, 0],
        ],
        [  # by canonical uid, departure from third station - tomorrow
            {'mixed_uid': 'R_canonical', 'departure_from': '2018-11-21', 'station_from': '102'},
            [0, 0, 0, 0, 0, 4, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 3, 0, 0, 1, 0, 4, 0, 0],
        ],
    ]
)
def test_suburban_thread_view(query, expected_run_days):
    stations = [create_station(title='station_{}'.format(i), id=i) for i in range(100, 104)]

    def create_test_thread(thread_id, thread_type, tz_start_time, year_days):
        return create_thread(
            id=thread_id,
            uid='t{}'.format(thread_id),
            canonical_uid='R_canonical',
            number='N456_{}'.format(thread_id),
            t_type=TransportType.SUBURBAN_ID,
            type=thread_type,
            tz_start_time=tz_start_time,
            year_days=year_days,
            express_type='express',
            schedule_v1=[
                [None, 0, stations[0]],
                [55, 60, stations[1]],
                [115, 120, stations[2]],
                [175, None, stations[3]],
            ]
        )

    def on_day(day_number):
        return datetime(2018, 11, day_number)

    create_test_thread(1, RThreadType.BASIC_ID, time(22, 15), [on_day(5), on_day(20), on_day(25)]),
    create_test_thread(2, RThreadType.ASSIGNMENT_ID, time(22, 15), [on_day(6)]),
    create_test_thread(3, RThreadType.CHANGE_ID, time(22, 30), [on_day(7), on_day(22), on_day(27)]),
    create_test_thread(4, RThreadType.CANCEL_ID, time(22, 15), [on_day(5), on_day(27)]),

    response = get_thread_response(query=query)

    assert_that(response['result'], has_entries({
        'thread': has_entries({
            'runDays': has_entries({'2018': {'11': expected_run_days}}),
            'isExpress': True,
            'isAeroExpress': False
        }),
        'threads': has_entries({
            '1': has_entries({
                'uid': 't1',
                'type': 'basic',
                'startDepartureTime': '22:15',
                'stopArrivalTime': '01:10',
                'firstDeparture': '2018-11-20',
                'daysText': 'только 5, 20, 25 ноября',
            }),
            '2': has_entries({
                'uid': 't2',
                'type': 'assignment',
                'startDepartureTime': '22:15',
                'stopArrivalTime': '01:10',
                'firstDeparture': '2018-11-06',
                'daysText': 'только 6 ноября',
            }),
            '3': has_entries({
                'uid': 't3',
                'type': 'change',
                'startDepartureTime': '22:30',
                'stopArrivalTime': '01:25',
                'firstDeparture': '2018-11-22',
            }),
            '4': has_entries({
                'uid': 't4',
                'type': 'cancel',
                'firstDeparture': '2018-11-27',
                'daysText': '5, 27 ноября',
            }),
        })
    }))


@replace_now(datetime(2018, 11, 20, 16))
def test_suburban_thread_state_with_api():
    station_1 = create_station(id=601)
    station_2 = create_station(id=602)
    station_3 = create_station(id=603)

    thread_start_dt = datetime(2018, 11, 20, 16)
    thread_uid = '123'
    thread = create_thread(
        uid=thread_uid,
        number='456',
        t_type=TransportType.SUBURBAN_ID,
        tz_start_time=thread_start_dt.time(),
        year_days=[datetime(2018, 11, 20)],
        schedule_v1=[
            [None, 0, station_1],
            [10, 11, station_2],
            [20, None, station_3],
        ]
    )
    sub_key = SuburbanKey.objects.create(thread=thread, key='thread_key')

    rts_1 = RTStation.objects.get(thread=thread, station=station_1)
    ThreadStationStateFactory(**{
        'key': ThreadStationKeyFactory(**{
            'thread_key': sub_key.key,
            'thread_start_date': thread_start_dt,
            'station_key': get_rtstation_key(rts_1),
            'arrival': rts_1.tz_arrival,
            'departure': rts_1.tz_departure
        }),
        'departure_state': EventStateFactory(**{
            'dt': datetime(2018, 11, 20, 16),
            'type': EventStateType.FACT,
            'thread_uid': thread_uid,
            'minutes_from': 0,
            'minutes_to': 0
        }),
        'tz': rts_1.station.time_zone
    })

    rts_2 = RTStation.objects.get(thread=thread, station=station_2)
    ThreadStationStateFactory(**{
        'key': ThreadStationKeyFactory(**{
            'thread_key': sub_key.key,
            'thread_start_date': thread_start_dt,
            'station_key': get_rtstation_key(rts_2),
            'arrival': rts_2.tz_arrival,
            'departure': rts_2.tz_departure
        }),
        'arrival_state': EventStateFactory(**{
            'dt': datetime(2018, 11, 20, 16, 14),
            'type': EventStateType.FACT,
            'thread_uid': thread_uid,
            'minutes_from': 4,
            'minutes_to': 4
        }),
        'departure_state': EventStateFactory(**{
            'type': EventStateType.POSSIBLE_DELAY,
            'thread_uid': thread_uid,
            'minutes_from': -2,
            'minutes_to': 5
        }),
        'tz': rts_2.station.time_zone
    })

    response = get_thread_response(query={'uid': '123', 'departure': '2018-11-20'})
    assert 'result' in response
    result = response['result']
    assert 'thread' in result
    assert_that(result['thread'], has_entries({
        'transportType': 'suburban',
        'uid': '123',
    }))

    assert 'rtstations' in result
    path = result['rtstations']
    assert len(path) == 3

    assert path[0]['id'] == 601
    state = path[0]['state']
    assert 'arrival' not in state
    assert 'departure' in state
    assert state['departure'] == {
        'factTime': '2018-11-20T16:00:00+03:00',
        'type': EventStateType.FACT,
        'minutesFrom': 0,
        'minutesTo': 0,
    }

    assert path[1]['id'] == 602
    state = path[1]['state']
    assert 'arrival' in state
    assert 'departure' in state
    assert state['arrival'] == {
        'factTime': '2018-11-20T16:14:00+03:00',
        'type': EventStateType.FACT,
        'minutesFrom': 4,
        'minutesTo': 4,
    }
    assert state['departure'] == {
        'type': EventStateType.POSSIBLE_DELAY,
        'minutesFrom': -2,
        'minutesTo': 5,
    }

    assert path[2]['id'] == 603
    assert 'state' not in path[2]


@replace_now(datetime(2020, 4, 26, 16))
def test_suburban_thread_state_with_api_cancels():
    station_1 = create_station(id=601)
    station_2 = create_station(id=602)
    station_3 = create_station(id=603)

    thread_start_dt = datetime(2020, 4, 26, 16)
    thread_uid = '123'
    thread = create_thread(
        uid=thread_uid,
        number='456',
        t_type=TransportType.SUBURBAN_ID,
        tz_start_time=thread_start_dt.time(),
        year_days=[datetime(2020, 4, 26)],
        schedule_v1=[
            [None, 0, station_1],
            [10, 15, station_2],
            [20, None, station_3],
        ]
    )
    sub_key = SuburbanKey.objects.create(thread=thread, key='thread_key')

    rts_1 = RTStation.objects.get(thread=thread, station=station_1)
    ThreadStationStateFactory(**{
        'key': ThreadStationKeyFactory(**{
            'thread_key': sub_key.key,
            'thread_start_date': thread_start_dt,
            'station_key': get_rtstation_key(rts_1),
            'arrival': rts_1.tz_arrival,
            'departure': rts_1.tz_departure
        }),
        'departure_state': EventStateFactory(**{
            'dt': thread_start_dt,
            'type': EventStateType.FACT,
            'thread_uid': thread_uid,
            'minutes_from': 0,
            'minutes_to': 0
        }),
        'tz': rts_1.station.time_zone
    })

    rts_2 = RTStation.objects.get(thread=thread, station=station_2)
    ThreadStationStateFactory(**{
        'key': ThreadStationKeyFactory(**{
            'thread_key': sub_key.key,
            'thread_start_date': thread_start_dt,
            'station_key': get_rtstation_key(rts_2),
            'arrival': rts_2.tz_arrival,
            'departure': rts_2.tz_departure
        }),
        'arrival_state': EventStateFactory(**{
            'dt': thread_start_dt + timedelta(minutes=14),
            'type': EventStateType.FACT,
            'thread_uid': thread_uid,
            'minutes_from': 4,
            'minutes_to': 4
        }),
        'departure_state': EventStateFactory(**{
            'dt': thread_start_dt + timedelta(minutes=15),
            'type': EventStateType.CANCELLED,
            'thread_uid': thread_uid,
            'minutes_from': None,
            'minutes_to': None
        }),
        'tz': rts_2.station.time_zone
    })

    rts_3 = RTStation.objects.get(thread=thread, station=station_3)
    ThreadStationStateFactory(**{
        'key': ThreadStationKeyFactory(**{
            'thread_key': sub_key.key,
            'thread_start_date': thread_start_dt,
            'station_key': get_rtstation_key(rts_3),
            'arrival': rts_3.tz_arrival,
            'departure': rts_3.tz_departure
        }),
        'arrival_state': EventStateFactory(**{
            'dt': thread_start_dt + timedelta(minutes=20),
            'type': EventStateType.CANCELLED,
            'thread_uid': thread_uid,
            'minutes_from': None,
            'minutes_to': None
        }),
        'tz': rts_3.station.time_zone
    })

    response = get_thread_response(query={'uid': '123', 'departure': '2020-04-26'})
    assert 'result' in response
    result = response['result']
    assert 'thread' in result
    assert_that(result['thread'], has_entries({
        'transportType': 'suburban',
        'uid': '123',
    }))

    assert 'rtstations' in result
    path = result['rtstations']
    assert len(path) == 3

    assert path[0]['id'] == 601
    state = path[0]['state']
    assert 'arrival' not in state
    assert 'departure' in state
    assert state['departure'] == {
        'factTime': '2020-04-26T16:00:00+03:00',
        'type': EventStateType.FACT,
        'minutesFrom': 0,
        'minutesTo': 0,
    }

    assert path[1]['id'] == 602
    state = path[1]['state']
    assert 'arrival' in state
    assert 'departure' in state
    assert state['arrival'] == {
        'factTime': '2020-04-26T16:14:00+03:00',
        'type': EventStateType.FACT,
        'minutesFrom': 4,
        'minutesTo': 4,
    }
    assert state['departure'] == {
        'type': EventStateType.CANCELLED,
        'minutesFrom': None,
        'minutesTo': None,
    }

    assert path[2]['id'] == 603
    state = path[2]['state']
    assert 'arrival' in state
    assert state['arrival'] == {
        'type': EventStateType.CANCELLED,
        'minutesFrom': None,
        'minutesTo': None,
    }

    response = get_thread_response(query={'uid': '123', 'departure': '2020-04-26'})
    assert 'result' in response
    result = response['result']
    assert 'thread' in result
    assert_that(result['thread'], has_entries({
        'transportType': 'suburban',
        'uid': '123',
    }))

    assert 'rtstations' in result
    path = result['rtstations']
    assert len(path) == 3

    assert path[0]['id'] == 601
    state = path[0]['state']
    assert 'arrival' not in state
    assert 'departure' in state
    assert state['departure'] == {
        'factTime': '2020-04-26T16:00:00+03:00',
        'type': EventStateType.FACT,
        'minutesFrom': 0,
        'minutesTo': 0,
    }

    assert path[1]['id'] == 602
    state = path[1]['state']
    assert 'arrival' in state
    assert 'departure' in state
    assert state['arrival'] == {
        'factTime': '2020-04-26T16:14:00+03:00',
        'type': EventStateType.FACT,
        'minutesFrom': 4,
        'minutesTo': 4,
    }
    assert state['departure'] == {
        'type': EventStateType.CANCELLED,
        'minutesFrom': None,
        'minutesTo': None,
    }

    assert path[2]['id'] == 603
    state = path[2]['state']
    assert 'arrival' in state
    assert state['arrival'] == {
        'type': EventStateType.CANCELLED,
        'minutesFrom': None,
        'minutesTo': None,
    }


@replace_now('2020-09-25')
def test_suburban_with_train_tariffs_view():
    station_from = create_station(title='from', id=666)
    station_to = create_station(title='to', id=667)

    t_subtype = create_transport_subtype(code='sub', t_type_id=TransportType.SUBURBAN_ID, has_train_tariffs=True)

    suburban_thread = create_thread(
        id='1',
        uid='t1',
        canonical_uid='R_canonical',
        number='7330',
        t_type=TransportType.SUBURBAN_ID,
        t_subtype=t_subtype,
        schedule_v1=[
            [None, 0, station_from],
            [175, None, station_to]
        ]
    )

    create_thread(
        id='2',
        uid='t2',
        canonical_uid='RT_canonical',
        number='882М',
        t_type=TransportType.TRAIN_ID,
        t_subtype=t_subtype,
        schedule_v1=[
            [None, 0, station_from],
            [175, None, station_to]
        ]
    )

    TrainPurchaseNumber.objects.create(thread=suburban_thread, number='882М')

    response = get_thread_response(query={'uid': 't1'})
    assert_that(response['result'], has_entries({
        'thread': has_entries({
            'number': '7330',
            'transportType': 'suburban',
            'tariffsKeys': [
                'static 666 667 t1',
                'suburban 666 667 1',
                '882М',
                '881М',
                'train 882М 20200925_0000',
                'train 881М 20200925_0000'
            ]
        })
    }))
