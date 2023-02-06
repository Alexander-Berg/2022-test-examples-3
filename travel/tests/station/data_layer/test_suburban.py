# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import date, time

import mock
import pytest
import pytz
from hamcrest import has_entries, assert_that, contains

from common.models.transport import TransportType
from common.tester.factories import (
    create_station, create_thread, create_transport_subtype, create_direction, create_train_schedule_plan
)
from common.tester.utils.datetime import replace_now
from travel.rasp.library.python.common23.date import environment
from stationschedule.views import station_schedule

from travel.rasp.morda_backend.morda_backend.station.data_layer.suburban import (
    SuburbanStationThread, SuburbanStationForPage
)
from travel.rasp.morda_backend.morda_backend.station.data_layer.page_context import StationPageContext
from travel.rasp.morda_backend.morda_backend.station.data_layer.page_type import StationPageType


pytestmark = [pytest.mark.dbuser]
create_thread = create_thread.mutate(__={'calculate_noderoute': True}, t_type=TransportType.SUBURBAN_ID)
create_station = create_station.mutate(t_type=TransportType.SUBURBAN_ID)


@replace_now('2020-02-01')
def test_suburban_station_thread():
    station0 = create_station(id=300, t_type=TransportType.TRAIN_ID)
    station1 = create_station(id=301, t_type=TransportType.TRAIN_ID)
    station2 = create_station(id=302, t_type=TransportType.TRAIN_ID)
    next_plan = create_train_schedule_plan()

    create_thread(
        canonical_uid='canonicalUid',
        title='Станция1 - Станция2',
        number='333',
        year_days=[date(2020, 1, 29), date(2020, 2, 1), date(2020, 2, 3)],
        express_type='express',
        schedule_v1=[
            [None, 0, station0],
            [20, 30, station1, {'platform': 'пл.3 путь 5'}],
            [60, None, station2]
        ]
    )

    schedule = station_schedule(station1, event='departure', t_type_code='suburban')
    route = list(schedule.schedule_routes)[0]
    route.schedule.stops_translations = json.dumps({'ru': 'остановки'})
    st_for_page = mock.Mock(
        page_context=mock.Mock(event='departure', is_all_days=True),
        language='ru', country='ru'
    )
    thread = SuburbanStationThread(route, st_for_page, next_plan)

    assert thread.canonical_uid == 'canonicalUid'
    assert thread.departure_from.isoformat() == '2020-02-01T00:30:00+03:00'
    assert thread.event_date_and_time['time'] == '00:30'
    assert thread.t_type == 'suburban'
    assert thread.title == 'Станция1 - Станция2'
    assert thread.days_text == 'только 29 января, 1, 3 февраля'
    assert thread.run_days_text == 'только 29 января, 1, 3 февраля'
    assert not hasattr(thread, 'except_days_text')
    assert not hasattr(thread, 'transport_subtype')
    assert not hasattr(thread, 'company')

    assert thread.platform == 'пл.3 путь 5'
    assert thread.is_express is True
    assert thread.is_aeroexpress is False
    assert_that(thread.stops, has_entries({
        'type': 'stops',
        'text': 'остановки'
    }))

    route.schedule.stops_translations = json.dumps({'ru': 'везде'})
    thread = SuburbanStationThread(route, st_for_page, next_plan)

    assert thread.stops == {'type': 'everywhere'}

    route.schedule.stops_translations = json.dumps({'ru': 'без остановок'})
    thread = SuburbanStationThread(route, st_for_page, next_plan)

    assert thread.stops == {'type': 'nonstop'}

    schedule = station_schedule(station1, event='arrival', t_type_code='suburban')
    route = list(schedule.schedule_routes)[0]
    st_for_page = mock.Mock(
        page_context=mock.Mock(event='arrival', is_all_days=True),
        language='ru', country='ru'
    )
    thread = SuburbanStationThread(route, st_for_page, next_plan)

    assert thread.canonical_uid == 'canonicalUid'
    assert thread.departure == date(2020, 2, 1)
    assert thread.event_date_and_time['time'] == '00:20'
    assert not hasattr(thread, 'stops')


def _make_threads(station, date, event, direction=None):
    page_type = StationPageType(station, 'suburban')
    page_context = StationPageContext(station, date, event, environment.now_aware())
    st_for_page = SuburbanStationForPage(page_type, 'ru')
    st_for_page.load_threads(page_context, direction)
    return st_for_page


@replace_now('2020-02-01')
def test_suburban_load_threads():
    ekb_tz = pytz.timezone('Etc/GMT-5')
    station0 = create_station(id=500, time_zone=ekb_tz)
    station1 = create_station(id=501, type_choices='suburban', time_zone=ekb_tz)
    station2 = create_station(id=502, time_zone=ekb_tz)

    from_subtype = create_transport_subtype(t_type=TransportType.TRAIN_ID, code='from', title_ru='Издалека')
    to_subtype = create_transport_subtype(t_type=TransportType.TRAIN_ID, code='to', title_ru='Далеко')

    create_thread(
        canonical_uid='cLong',
        year_days=[date(2020, 2, 1)],
        time_zone='Etc/GMT-3',
        tz_start_time=time(1, 0),
        schedule_v1=[
            [None, 0, station0],
            [60, 70, station1, {'platform': '1', 'time_zone': ekb_tz}],
            [120, None, station2]
        ]
    )

    create_thread(
        canonical_uid='cOther',
        express_type='express',
        year_days=[date(2020, 2, 1)],
        time_zone='Etc/GMT-3',
        tz_start_time=time(22, 0),
        schedule_v1=[
            [None, 0, station0],
            [70, 80, station1, {'time_zone': ekb_tz}],
            [120, None, station2]
        ]
    )

    create_thread(
        canonical_uid='cNext',
        express_type='aeroexpress',
        year_days=[date(2020, 2, 1)],
        time_zone='Etc/GMT-3',
        tz_start_time=time(23, 0),
        schedule_v1=[
            [None, 0, station0],
            [70, 80, station1, {'time_zone': ekb_tz}],
            [120, None, station2]
        ]
    )

    create_thread(
        canonical_uid='cTo',
        t_subtype=to_subtype,
        year_days=[date(2020, 2, 1)],
        time_zone='Etc/GMT-3',
        tz_start_time=time(10, 0),
        schedule_v1=[
            [None, 0, station1, {'platform': '2', 'time_zone': ekb_tz}],
            [60, None, station2]
        ]
    )

    create_thread(
        canonical_uid='cFrom',
        t_subtype=from_subtype,
        year_days=[date(2020, 2, 1)],
        time_zone='Etc/GMT-3',
        tz_start_time=time(5, 0),
        schedule_v1=[
            [None, 0, station0],
            [60, None, station1, {'platform': '3', 'time_zone': ekb_tz}]
        ]
    )

    create_thread(
        canonical_uid='cExpress',
        year_days=[date(2020, 2, 1)],
        time_zone='Etc/GMT-3',
        tz_start_time=time(12, 0),
        schedule_v1=[[None, 0, station0], [120, None, station2]]
    )

    # Один день, отправление

    st_for_page = _make_threads(station1, '2020-02-01', 'departure')

    assert len(st_for_page.threads) == 3

    thread = st_for_page.threads[0]
    assert thread.canonical_uid == 'cLong'
    assert thread.t_type == 'suburban'
    assert thread.platform == '1'
    assert not hasattr(thread, 'transport_subtype')
    assert thread.is_express is False
    assert thread.is_aeroexpress is False
    assert thread.departure_from.isoformat() == '2020-02-01T02:10:00+05:00'
    assert thread.event_date_and_time['time'] == '02:10'
    assert thread.event_date_and_time['datetime'].isoformat() == '2020-02-01T02:10:00+05:00'

    thread = st_for_page.threads[1]
    assert thread.canonical_uid == 'cTo'
    assert thread.platform == '2'
    assert_that(thread.transport_subtype, has_entries({
        'code': 'to',
        'title': 'Далеко'
    }))
    assert thread.is_express is False
    assert thread.is_aeroexpress is False
    assert thread.departure_from.isoformat() == '2020-02-01T10:00:00+05:00'
    assert thread.event_date_and_time['time'] == '10:00'
    assert thread.event_date_and_time['datetime'].isoformat() == '2020-02-01T10:00:00+05:00'

    thread = st_for_page.threads[2]
    assert thread.canonical_uid == 'cOther'
    assert thread.platform == ''
    assert not hasattr(thread, 'transport_subtype')
    assert thread.is_express is True
    assert thread.is_aeroexpress is False
    assert thread.departure_from.isoformat() == '2020-02-01T23:20:00+05:00'
    assert thread.event_date_and_time['time'] == '23:20'
    assert thread.event_date_and_time['datetime'].isoformat() == '2020-02-01T23:20:00+05:00'

    st_for_page = _make_threads(station1, '2020-02-02', 'departure')

    assert len(st_for_page.threads) == 1

    thread = st_for_page.threads[0]
    assert thread.canonical_uid == 'cNext'
    assert thread.platform == ''
    assert not hasattr(thread, 'transport_subtype')
    assert thread.is_express is False
    assert thread.is_aeroexpress is True
    assert thread.departure_from.isoformat() == '2020-02-02T00:20:00+05:00'
    assert thread.event_date_and_time['time'] == '00:20'
    assert thread.event_date_and_time['datetime'].isoformat() == '2020-02-02T00:20:00+05:00'

    st_for_page = _make_threads(station1, '2020-02-03', 'departure')

    assert len(st_for_page.threads) == 0

    # Один день, прибытие

    st_for_page = _make_threads(station1, '2020-02-01', 'arrival')

    assert len(st_for_page.threads) == 3

    thread = st_for_page.threads[0]
    assert thread.canonical_uid == 'cLong'
    assert thread.platform == '1'
    assert not hasattr(thread, 'transport_subtype')
    assert thread.is_express is False
    assert thread.is_aeroexpress is False
    assert thread.departure == date(2020, 2, 1)
    assert thread.event_date_and_time['time'] == '02:00'
    assert thread.event_date_and_time['datetime'].isoformat() == '2020-02-01T02:00:00+05:00'

    thread = st_for_page.threads[1]
    assert thread.canonical_uid == 'cFrom'
    assert thread.platform == '3'
    assert_that(thread.transport_subtype, has_entries({
        'code': 'from',
        'title': 'Издалека'
    }))
    assert thread.is_express is False
    assert thread.is_aeroexpress is False
    assert thread.departure == date(2020, 2, 1)
    assert thread.event_date_and_time['time'] == '06:00'
    assert thread.event_date_and_time['datetime'].isoformat() == '2020-02-01T06:00:00+05:00'

    thread = st_for_page.threads[2]
    assert thread.canonical_uid == 'cOther'
    assert thread.platform == ''
    assert not hasattr(thread, 'transport_subtype')
    assert thread.is_express is True
    assert thread.is_aeroexpress is False
    assert thread.departure == date(2020, 2, 1)
    assert thread.event_date_and_time['time'] == '23:10'
    assert thread.event_date_and_time['datetime'].isoformat() == '2020-02-01T23:10:00+05:00'

    st_for_page = _make_threads(station1, '2020-02-02', 'arrival')

    assert len(st_for_page.threads) == 1

    thread = st_for_page.threads[0]
    assert thread.canonical_uid == 'cNext'
    assert thread.platform == ''
    assert not hasattr(thread, 'transport_subtype')
    assert thread.is_express is False
    assert thread.is_aeroexpress is True
    assert thread.departure == date(2020, 2, 1)
    assert thread.event_date_and_time['time'] == '00:10'
    assert thread.event_date_and_time['datetime'].isoformat() == '2020-02-02T00:10:00+05:00'

    st_for_page = _make_threads(station1, '2020-02-03', 'arrival')

    assert len(st_for_page.threads) == 0

    # Все дни, отправление

    st_for_page = _make_threads(station1, 'all-days', 'departure')

    assert len(st_for_page.threads) == 4

    thread = st_for_page.threads[0]
    assert thread.canonical_uid == 'cNext'
    assert thread.platform == ''
    assert not hasattr(thread, 'transport_subtype')
    assert thread.is_express is False
    assert thread.is_aeroexpress is True
    assert thread.departure_from.isoformat() == '2020-02-02T00:20:00+05:00'
    assert thread.event_date_and_time['time'] == '00:20'
    assert thread.days_text == 'только 2 февраля'
    assert thread.run_days_text == 'только 2 февраля'
    assert not hasattr(thread, 'except_days_text')

    thread = st_for_page.threads[1]
    assert thread.canonical_uid == 'cLong'
    assert thread.platform == '1'
    assert not hasattr(thread, 'transport_subtype')
    assert thread.is_express is False
    assert thread.is_aeroexpress is False
    assert thread.departure_from.isoformat() == '2020-02-01T02:10:00+05:00'
    assert thread.event_date_and_time['time'] == '02:10'
    assert thread.days_text == 'только 1 февраля'
    assert thread.run_days_text == 'только 1 февраля'
    assert not hasattr(thread, 'except_days_text')

    thread = st_for_page.threads[2]
    assert thread.canonical_uid == 'cTo'
    assert thread.platform == '2'
    assert_that(thread.transport_subtype, has_entries({
        'code': 'to',
        'title': 'Далеко'
    }))
    assert thread.is_express is False
    assert thread.is_aeroexpress is False
    assert thread.departure_from.isoformat() == '2020-02-01T10:00:00+05:00'
    assert thread.event_date_and_time['time'] == '10:00'
    assert thread.days_text == 'только 1 февраля'
    assert thread.run_days_text == 'только 1 февраля'
    assert not hasattr(thread, 'except_days_text')

    thread = st_for_page.threads[3]
    assert thread.canonical_uid == 'cOther'
    assert thread.platform == ''
    assert not hasattr(thread, 'transport_subtype')
    assert thread.is_express is True
    assert thread.is_aeroexpress is False
    assert thread.departure_from.isoformat() == '2020-02-01T23:20:00+05:00'
    assert thread.event_date_and_time['time'] == '23:20'
    assert thread.days_text == 'только 1 февраля'
    assert thread.run_days_text == 'только 1 февраля'
    assert not hasattr(thread, 'except_days_text')

    # Все дни, прибытие

    st_for_page = _make_threads(station1, 'all-days', 'arrival')

    assert len(st_for_page.threads) == 4

    thread = st_for_page.threads[0]
    assert thread.canonical_uid == 'cNext'
    assert thread.platform == ''
    assert not hasattr(thread, 'transport_subtype')
    assert thread.is_express is False
    assert thread.is_aeroexpress is True
    assert thread.departure == date(2020, 2, 1)
    assert thread.event_date_and_time['time'] == '00:10'
    assert thread.days_text == 'только 2 февраля'
    assert thread.run_days_text == 'только 2 февраля'
    assert not hasattr(thread, 'except_days_text')

    thread = st_for_page.threads[1]
    assert thread.canonical_uid == 'cLong'
    assert thread.platform == '1'
    assert not hasattr(thread, 'transport_subtype')
    assert thread.is_express is False
    assert thread.is_aeroexpress is False
    assert thread.departure == date(2020, 2, 1)
    assert thread.event_date_and_time['time'] == '02:00'
    assert thread.days_text == 'только 1 февраля'
    assert thread.run_days_text == 'только 1 февраля'
    assert not hasattr(thread, 'except_days_text')

    thread = st_for_page.threads[2]
    assert thread.canonical_uid == 'cFrom'
    assert thread.platform == '3'
    assert_that(thread.transport_subtype, has_entries({
        'code': 'from',
        'title': 'Издалека'
    }))
    assert thread.is_express is False
    assert thread.is_aeroexpress is False
    assert thread.departure == date(2020, 2, 1)
    assert thread.event_date_and_time['time'] == '06:00'
    assert thread.days_text == 'только 1 февраля'
    assert thread.run_days_text == 'только 1 февраля'
    assert not hasattr(thread, 'except_days_text')

    thread = st_for_page.threads[3]
    assert thread.canonical_uid == 'cOther'
    assert thread.platform == ''
    assert not hasattr(thread, 'transport_subtype')
    assert thread.is_express is True
    assert thread.is_aeroexpress is False
    assert thread.departure == date(2020, 2, 1)
    assert thread.event_date_and_time['time'] == '23:10'
    assert thread.days_text == 'только 1 февраля'
    assert thread.run_days_text == 'только 1 февраля'
    assert not hasattr(thread, 'except_days_text')


@replace_now('2020-02-01')
def test_suburban_directions():
    dir_vpered = {
        'departure_subdir': 'vpered',
        'departure_direction': create_direction(code='vpered')
    }
    dir_nazad = {
        'departure_subdir': 'nazad',
        'departure_direction': create_direction(code='nazad')
    }
    station1 = create_station(id=601)
    station2 = create_station(id=602)

    create_thread(
        year_days=[date(2020, 2, 1)],
        tz_start_time=time(2, 0),
        schedule_v1=[
            [None, 0, station1, dir_vpered],
            [120, None, station2]
        ]
    )

    create_thread(
        year_days=[date(2020, 2, 1)],
        tz_start_time=time(1, 0),
        schedule_v1=[
            [None, 0, station1, dir_vpered],
            [120, None, station2]
        ]
    )

    create_thread(
        year_days=[date(2020, 2, 1), date(2020, 2, 2)],
        tz_start_time=time(3, 0),
        schedule_v1=[
            [None, 0, station1, dir_nazad],
            [120, None, station2]
        ]
    )

    create_thread(
        year_days=[date(2020, 2, 2)],
        tz_start_time=time(5, 0),
        schedule_v1=[
            [None, 0, station2],
            [120, None, station1, dir_nazad]
        ]
    )

    create_thread(
        year_days=[date(2020, 2, 1)],
        tz_start_time=time(4, 0),
        schedule_v1=[
            [None, 0, station2],
            [120, None, station1, dir_vpered]
        ]
    )

    st_for_page = _make_threads(station1, '2020-02-01', 'departure', 'all')

    assert st_for_page.page_context.direction_code == 'all'
    _check_directions_list(st_for_page)
    assert len(st_for_page.threads) == 3
    assert st_for_page.threads[0].event_date_and_time['time'] == '01:00'
    assert st_for_page.threads[1].event_date_and_time['time'] == '02:00'
    assert st_for_page.threads[2].event_date_and_time['time'] == '03:00'

    st_for_page = _make_threads(station1, '2020-02-01', 'departure', 'vpered')

    assert st_for_page.page_context.direction_code == 'vpered'
    _check_directions_list(st_for_page)
    assert len(st_for_page.threads) == 2
    assert st_for_page.threads[0].event_date_and_time['time'] == '01:00'
    assert st_for_page.threads[1].event_date_and_time['time'] == '02:00'

    st_for_page = _make_threads(station1, '2020-02-01', 'departure', 'nazad')

    assert st_for_page.page_context.direction_code == 'nazad'
    _check_directions_list(st_for_page)
    assert len(st_for_page.threads) == 1
    assert st_for_page.threads[0].event_date_and_time['time'] == '03:00'

    st_for_page = _make_threads(station1, '2020-02-01', 'departure')

    assert st_for_page.page_context.direction_code == 'vpered'
    _check_directions_list(st_for_page)
    assert len(st_for_page.threads) == 2
    assert st_for_page.threads[0].event_date_and_time['time'] == '01:00'
    assert st_for_page.threads[1].event_date_and_time['time'] == '02:00'

    st_for_page = _make_threads(station1, '2020-02-02', 'departure', 'all')

    assert st_for_page.page_context.direction_code == 'all'
    _check_directions_list(st_for_page)
    assert len(st_for_page.threads) == 1
    assert st_for_page.threads[0].event_date_and_time['time'] == '03:00'

    st_for_page = _make_threads(station1, '2020-02-02', 'departure', 'nazad')

    assert st_for_page.page_context.direction_code == 'nazad'
    _check_directions_list(st_for_page)
    assert len(st_for_page.threads) == 1
    assert st_for_page.threads[0].event_date_and_time['time'] == '03:00'

    st_for_page = _make_threads(station1, '2020-02-02', 'departure', 'vpered')

    assert st_for_page.page_context.direction_code == 'vpered'
    _check_directions_list(st_for_page)
    assert len(st_for_page.threads) == 0

    st_for_page = _make_threads(station1, '2020-02-02', 'departure')

    assert st_for_page.page_context.direction_code == 'vpered'
    _check_directions_list(st_for_page)
    assert len(st_for_page.threads) == 0

    st_for_page = _make_threads(station1, 'all-days', 'departure', 'all')

    assert st_for_page.page_context.direction_code == 'all'
    _check_directions_list(st_for_page)
    assert len(st_for_page.threads) == 3
    assert st_for_page.threads[0].event_date_and_time['time'] == '01:00'
    assert st_for_page.threads[1].event_date_and_time['time'] == '02:00'
    assert st_for_page.threads[2].event_date_and_time['time'] == '03:00'

    st_for_page = _make_threads(station1, 'all-days', 'departure', 'vpered')

    assert st_for_page.page_context.direction_code == 'vpered'
    _check_directions_list(st_for_page)
    assert len(st_for_page.threads) == 2
    assert st_for_page.threads[0].event_date_and_time['time'] == '01:00'
    assert st_for_page.threads[1].event_date_and_time['time'] == '02:00'

    st_for_page = _make_threads(station1, 'all-days', 'departure', 'nazad')

    assert st_for_page.page_context.direction_code == 'nazad'
    _check_directions_list(st_for_page)
    assert len(st_for_page.threads) == 1
    assert st_for_page.threads[0].event_date_and_time['time'] == '03:00'

    st_for_page = _make_threads(station1, 'all-days', 'departure')

    assert st_for_page.page_context.direction_code == 'vpered'
    _check_directions_list(st_for_page)
    assert len(st_for_page.threads) == 2
    assert st_for_page.threads[0].event_date_and_time['time'] == '01:00'
    assert st_for_page.threads[1].event_date_and_time['time'] == '02:00'

    st_for_page = _make_threads(station1, '2020-02-01', 'arrival')

    assert not hasattr(st_for_page.page_context, 'direction_code')
    _check_directions_list(st_for_page)
    assert len(st_for_page.threads) == 1
    assert st_for_page.threads[0].event_date_and_time['time'] == '06:00'

    st_for_page = _make_threads(station1, 'all-days', 'arrival')

    assert not hasattr(st_for_page.page_context, 'direction_code')
    _check_directions_list(st_for_page)
    assert len(st_for_page.threads) == 2
    assert st_for_page.threads[0].event_date_and_time['time'] == '06:00'
    assert st_for_page.threads[1].event_date_and_time['time'] == '07:00'


def _check_directions_list(st_for_page):
    assert_that(st_for_page.station_properties['directions'], contains(
        {
            'code': 'all',
            'title': 'все направления'
        },
        {
            'code': 'vpered',
            'title': 'vpered'
        },
        {
            'code': 'nazad',
            'title': 'nazad'
        }
    ))
