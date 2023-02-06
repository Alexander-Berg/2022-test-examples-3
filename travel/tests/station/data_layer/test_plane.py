# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from datetime import time
from hamcrest import has_entries, assert_that, contains
from dateutil import parser

from common.data_api.baris.test_helpers import mock_baris_response
from common.models.geo import Country, CodeSystem
from common.models.transport import TransportType
from common.tester.utils.datetime import replace_now
from travel.rasp.library.python.common23.date import environment
from common.tester.factories import (
    create_settlement, create_station, create_station_code, create_company
)
from travel.rasp.morda_backend.morda_backend.station.data_layer.plane import (
    PlaneStationForPage, BasePlaneStationThread, OneDayPlaneStationThread, AllDaysPlaneStationThread
)
from travel.rasp.morda_backend.morda_backend.station.data_layer.page_type import StationPageType, get_station_by_id
from travel.rasp.morda_backend.morda_backend.station.data_layer.page_context import StationPageContext


pytestmark = [pytest.mark.dbuser]
create_station = create_station.mutate(t_type=TransportType.PLANE_ID, country=Country.RUSSIA_ID)


def test_airport_codes():
    iata_code_system = CodeSystem.objects.get(code='iata')
    icao_code_system = CodeSystem.objects.get(code='icao')
    sirena_code_system = CodeSystem.objects.get(code='sirena')
    station = create_station()
    create_station_code(station=station, system=iata_code_system, code='IATAC')
    create_station_code(station=station, system=icao_code_system, code='ICAOC')
    create_station_code(station=station, system=sirena_code_system, code='SIRENAC')

    station = get_station_by_id(station.id)
    page_type = StationPageType(station, 'plane')
    st_for_page = PlaneStationForPage(page_type, 'ru')
    st_for_page._make_airport_codes()

    assert st_for_page.iata_code == 'IATAC'
    assert st_for_page.station_properties['iata_code'] == 'IATAC'
    assert st_for_page.icao_code == 'ICAOC'
    assert st_for_page.sirena_code == 'SIRENAC'


def _create_stations(trusted=False):
    iata_code_system = CodeSystem.objects.get(code='iata')
    settlement1 = create_settlement(title='Первый город')
    settlement2 = create_settlement(title='Второй город')
    station1 = create_station(id=101, title='Первый', settlement=settlement1, tablo_state='real' if trusted else '')
    station2 = create_station(id=102, title='Второй', settlement=settlement2)
    station3 = create_station(id=103, title='Третий')
    create_station_code(station=station1, system=iata_code_system, code='CODE1')
    create_station_code(station=station2, system=iata_code_system, code='CODE2')
    create_station_code(station=station3, system=iata_code_system, code='CODE3')
    create_company(
        id=301, short_title='Компания', title='Авиакомпания', url='url.ru', svg_logo='icon.svg', hidden=False
    )
    create_company(
        id=302, short_title='Другая', title='Другая компания', url='url2.ru', svg_logo='icon2.svg', hidden=False
    )

    return {101: station1, 102: station2, 103: station3}


def _make_st_for_page_mock(
    stations, event='departure', is_all_days=False, trusted=True,
    language='ru', country='ru', sirena_code='', icao_code=''
):
    iata_code = '' if sirena_code or icao_code else 'CODE1'
    return mock.Mock(
        page_context=mock.Mock(event=event, is_all_days=is_all_days),
        language=language, country=country, station=stations[101],
        stations_by_ids=stations, trusted=trusted,
        iata_code=iata_code, sirena_code=sirena_code, icao_code=icao_code
    )


@replace_now('2020-02-01')
def test_base_plane_station_thread():
    stations = _create_stations()

    st_for_page = _make_st_for_page_mock(stations)

    flight = {
        'airlineID': 301,
        'title': 'SU 1',
        'terminal': 'B',
        'codeshares': [{
            'airlineID': 302,
            'title': 'SV 1'
        }],
        'route': [101, 102, 103]
    }

    thread = BasePlaneStationThread(flight, st_for_page)

    assert thread.flight == flight
    assert thread.t_type == 'plane'
    assert thread.terminal == 'B'

    assert thread.number == 'SU 1'
    assert thread.company_id == 301
    assert_that(thread.codeshares, contains(
        {
            'company_id': 302,
            'number': 'SV 1'
        }
    ))
    assert len(thread.route_stations) == 2
    assert_that(thread.route_stations, contains(
        has_entries({
            'settlement': 'Второй город',
            'iata_code': 'CODE2',
            'title': 'Второй'
        }),
        has_entries({
            'iata_code': 'CODE3',
            'title': 'Третий'
        })
    ))

    flight = {
        'airlineID': 301,
        'title': 'SU 2',
        'terminal': '',
        'route': [101, 102],
        'codeshares': None
    }

    thread = BasePlaneStationThread(flight, st_for_page)

    assert thread.flight == flight
    assert thread.t_type == 'plane'
    assert thread.terminal == ''

    assert thread.number == 'SU 2'
    assert thread.company_id == 301
    assert not hasattr(thread, 'codeshares')
    assert len(thread.route_stations) == 1
    assert_that(thread.route_stations[0], has_entries({
        'settlement': 'Второй город',
        'iata_code': 'CODE2',
        'title': 'Второй'
    }))

    flight = {
        'airlineID': 301,
        'title': 'SU 3',
        'terminal': '',
        'route': [102, 101, 103],
        'codeshares': None
    }

    thread = BasePlaneStationThread(flight, st_for_page)

    assert thread.flight == flight
    assert thread.t_type == 'plane'
    assert thread.terminal == ''

    assert thread.number == 'SU 3'
    assert thread.company_id == 301
    assert not hasattr(thread, 'codeshares')
    assert len(thread.route_stations) == 1
    assert_that(thread.route_stations[0], has_entries({
        'iata_code': 'CODE3',
        'title': 'Третий'
    }))

    st_for_page = _make_st_for_page_mock(stations, 'arrival')

    flight = {
        'airlineID': 301,
        'title': 'SU 4',
        'terminal': 'A',
        'codeshares': [{
            'airlineID': 302,
            'title': 'SV 4'
        }],
        'route': [102, 101, 103]
    }

    thread = BasePlaneStationThread(flight, st_for_page)

    assert thread.flight == flight
    assert thread.t_type == 'plane'
    assert thread.terminal == 'A'

    assert thread.number == 'SU 4'
    assert thread.company_id == 301
    assert_that(thread.codeshares, contains(
        {
            'company_id': 302,
            'number': 'SV 4'
        }
    ))
    assert len(thread.route_stations) == 1
    assert_that(thread.route_stations[0], has_entries({
        'settlement': 'Второй город',
        'iata_code': 'CODE2',
        'title': 'Второй'
    }))

    flight = {
        'airlineID': 301,
        'title': 'SU 4',
        'terminal': 'A',
        'route': [104, 101]
    }

    thread = BasePlaneStationThread(flight, st_for_page)

    assert thread.is_valid is False


@replace_now('2020-02-01')
def test_one_day_plane_station_thread():
    stations = _create_stations()

    st_for_page = _make_st_for_page_mock(stations)

    flight = {
        'airlineID': 301,
        'title': 'SU 1',
        'terminal': 'B',
        'datetime': '2020-02-01T12:10:00+03:00',
        'route': [101, 102],
        'codeshares': None,
        'source': 'flight-board',
        'status': {
            'departureStatus': 'unknown',
            'departure': '',
            'departureTerminal': '',
            'departureGate': '',
            'checkInDesks': '',
            'baggageCarousels': '',
            'diverted': False
        }
    }

    thread = OneDayPlaneStationThread(flight, st_for_page)

    assert thread.flight == flight
    assert thread.t_type == 'plane'
    assert thread.terminal == 'B'
    assert thread.number == 'SU 1'
    assert thread.company_id == 301
    assert thread.is_supplement is True
    assert len(thread.route_stations) == 1
    assert_that(thread.route_stations[0], has_entries({
        'settlement': 'Второй город',
        'iata_code': 'CODE2',
        'title': 'Второй'
    }))

    assert thread.event_date_and_time['time'] == '12:10'
    assert thread.event_date_and_time['datetime'].isoformat() == '2020-02-01T12:10:00+03:00'
    assert thread.avia_link == 'https://travel-test.yandex.ru/avia/flights/SU-1/?lang=ru&when=2020-02-01&from=CODE1'

    assert_that(thread.status, has_entries({
        'status': 'unknown',
        'actual_dt': '',
        'actual_terminal': '',
        'gate': '',
        'check_in_desks': '',
        'baggage_carousels': ''
    }))
    assert not hasattr(thread.status, 'diverted')

    flight = {
        'airlineID': 301,
        'title': 'SU 2',
        'terminal': 'B',
        'datetime': '2020-02-01T12:20:00+03:00',
        'route': [101, 102],
        'codeshares': None,
        'source': '',
        'status': {
            'departureStatus': 'delayed',
            'departure': '2020-02-01 13:20:00',
            'departureTerminal': 'A',
            'departureGate': '20',
            'checkInDesks': '5-6',
            'baggageCarousels': '',
            'diverted': True,
            'divertedAirportID': 104
        }
    }

    thread = OneDayPlaneStationThread(flight, st_for_page)

    assert thread.is_supplement is False
    assert thread.event_date_and_time['time'] == '12:20'
    assert thread.event_date_and_time['datetime'].isoformat() == '2020-02-01T12:20:00+03:00'
    assert thread.avia_link == 'https://travel-test.yandex.ru/avia/flights/SU-2/?lang=ru&when=2020-02-01&from=CODE1'

    assert_that(thread.status, has_entries({
        'status': 'delayed',
        'actual_dt': '2020-02-01T13:20:00+03:00',
        'actual_terminal': 'A',
        'gate': '20',
        'check_in_desks': '5-6',
        'baggage_carousels': '',
    }))
    assert not hasattr(thread.status, 'diverted')

    st_for_page = _make_st_for_page_mock(stations, 'arrival')

    flight = {
        'airlineID': 301,
        'title': 'SU 3Ц',
        'terminal': 'B',
        'datetime': '2020-02-01T12:30:00+03:00',
        'route': [102, 101],
        'codeshares': None,
        'source': '',
        'status': {
            'arrivalStatus': 'on_time',
            'arrival': '2020-02-01 12:30:00',
            'arrivalTerminal': 'A',
            'arrivalGate': '30',
            'checkInDesks': '',
            'baggageCarousels': '3-4',
            'diverted': True,
            'divertedAirportID': 103
        }
    }

    thread = OneDayPlaneStationThread(flight, st_for_page)

    assert thread.event_date_and_time['time'] == '12:30'
    assert thread.event_date_and_time['datetime'].isoformat() == '2020-02-01T12:30:00+03:00'
    assert thread.avia_link == 'https://travel-test.yandex.ru/avia/flights/SU-3%D0%A6/?lang=ru&when=2020-02-01&to=CODE1'

    assert_that(thread.status, has_entries({
        'status': 'on_time',
        'actual_dt': '2020-02-01T12:30:00+03:00',
        'actual_terminal': 'A',
        'gate': '30',
        'check_in_desks': '',
        'baggage_carousels': '3-4',
        'diverted': has_entries({
            'iata_code': 'CODE3',
            'title': 'Третий'
        })
    }))

    flight = {
        'airlineID': 301,
        'title': 'SU 4',
        'terminal': '',
        'datetime': '2020-02-01T12:40:00+03:00',
        'route': [101, 102],
        'codeshares': None,
        'source': '',
    }

    st_for_page = _make_st_for_page_mock(stations, language='ua', trusted=False)
    thread = OneDayPlaneStationThread(flight, st_for_page)
    assert thread.avia_link == 'https://travel-test.yandex.ru/avia/flights/SU-4/?lang=ua&when=2020-02-01&from=CODE1'

    st_for_page = _make_st_for_page_mock(stations, country='ua', trusted=False)
    thread = OneDayPlaneStationThread(flight, st_for_page)
    assert thread.avia_link == 'https://travel-test.yandex.ru/avia/flights/SU-4/?lang=ru&when=2020-02-01&from=CODE1'

    st_for_page = _make_st_for_page_mock(stations, trusted=False, sirena_code='SIRENA1')
    thread = OneDayPlaneStationThread(flight, st_for_page)
    assert thread.avia_link == 'https://travel-test.yandex.ru/avia/flights/SU-4/?lang=ru&when=2020-02-01&from=SIRENA1'

    st_for_page = _make_st_for_page_mock(stations, trusted=False, sirena_code='ICAO1')
    thread = OneDayPlaneStationThread(flight, st_for_page)
    assert thread.avia_link == 'https://travel-test.yandex.ru/avia/flights/SU-4/?lang=ru&when=2020-02-01&from=ICAO1'

    st_for_page = _make_st_for_page_mock(stations, trusted=False)
    flight = {
        'airlineID': 301,
        'title': 'SU 4',
        'terminal': '',
        'datetime': '',
        'route': [101, 102],
        'codeshares': None,
        'source': '',
    }

    thread = OneDayPlaneStationThread(flight, st_for_page)
    assert thread.is_valid is False

    flight = {
        'airlineID': 301,
        'title': 'SU 4',
        'terminal': '',
        'datetime': '2020-02-02T12:40:00+03:00',
        'route': [101, 102],
        'codeshares': None,
        'source': '',
    }

    thread = OneDayPlaneStationThread(flight, st_for_page, dt_before=parser.parse('2020-02-02T00:00:00+03:00'))
    assert thread.is_valid is False

    thread = OneDayPlaneStationThread(flight, st_for_page, dt_after=parser.parse('2020-02-03T00:00:00+03:00'))
    assert thread.is_valid is False


@replace_now('2020-02-01')
def test_all_days_plane_station_thread():
    stations = _create_stations()
    st_for_page = _make_st_for_page_mock(stations, is_all_days=True)

    flight = {
        'airlineID': 301,
        'title': 'SU 5',
        'terminal': 'B',
        'time': '12:50',
        'route': [101, 102],
        'daysText': 'дни хождения'
    }

    thread = AllDaysPlaneStationThread(flight, st_for_page)

    assert thread.flight == flight
    assert thread.t_type == 'plane'
    assert thread.terminal == 'B'
    assert thread.number == 'SU 5'
    assert thread.company_id == 301
    assert len(thread.route_stations) == 1
    assert_that(thread.route_stations[0], has_entries({
        'settlement': 'Второй город',
        'iata_code': 'CODE2',
        'title': 'Второй'
    }))

    assert thread.event_date_and_time['time'] == '12:50'
    assert 'datetime' not in thread.event_date_and_time
    assert thread.avia_link == 'https://travel-test.yandex.ru/avia/flights/SU-5/?lang=ru'
    assert thread.days_text == 'дни хождения'


def _fill_st_for_page(stations, date, event):
    station = get_station_by_id(101)
    page_type = StationPageType(station, 'plane')
    page_context = StationPageContext(stations[101], date, event, environment.now_aware())
    st_for_page = PlaneStationForPage(page_type, 'ru')
    if date == 'all-days':
        st_for_page.load_threads(page_context, None, None)
    else:
        st_for_page.load_threads(page_context, time(0), time(22))
    return st_for_page


def _check_companies_and_stations_by_ids(st_for_page, no_flights):
    if no_flights:
        assert len(st_for_page.companies_by_ids) == 0
        assert len(st_for_page.stations_by_ids) == 1
        return

    assert len(st_for_page.companies_by_ids) == 2
    assert st_for_page.companies_by_ids[301].id == 301
    assert st_for_page.companies_by_ids[302].id == 302

    assert len(st_for_page.stations_by_ids) == 2
    assert st_for_page.stations_by_ids[101].id == 101
    assert st_for_page.stations_by_ids[102].id == 102


def _check_one_day_st_for_page(stations, flights_count, trusted, special, date_str, event):
    flights = []
    for _ in range(flights_count):
        flight = {
            'airlineID': 301,
            'title': 'SU 1',
            'datetime': '{}T01:30:00+03:00'.format(date_str),
            'terminal': 'A',
            'codeshares': [{
                'airlineID': 302,
                'title': 'SV 1'
            }],
            'route': [101, 102] if event == 'departure' else [102, 101],
            'source': '',
        }
        if trusted:
            flight['status'] = {
                event: '{} 01:30:00'.format(date_str),
                '{}Status'.format(event): 'on_time',
                '{}Gate'.format(event): '1',
                '{}Terminal'.format(event): 'A',
                'checkInDesks': '2',
                'baggageCarousels': '3',
                'diverted': True,
                'divertedAirportID': 102
            }
        flights.append(flight)

    with mock_baris_response({'direction': event, 'station': 101, 'flights': flights}):
        st_for_page = _fill_st_for_page(stations, special, event)
        _check_companies_and_stations_by_ids(st_for_page, flights_count == 0)

        assert st_for_page.page_context.event == event
        assert st_for_page.page_context.date.isoformat() == date_str
        assert st_for_page.page_context.time_after == time(0)
        assert st_for_page.page_context.time_before == time(22)
        assert st_for_page.page_context.dt_after.isoformat() == '{}T00:00:00+03:00'.format(date_str)
        assert st_for_page.page_context.dt_before.isoformat() == '{}T22:00:00+03:00'.format(date_str)
        assert st_for_page.page_context.dt_now.isoformat() == '2020-02-01T00:00:00+03:00'

        assert len(st_for_page.threads) == flights_count
        for thread in st_for_page.threads:
            assert thread.t_type == 'plane'
            assert thread.number == 'SU 1'
            assert thread.company_id == 301
            assert thread.terminal == 'A'
            assert thread.event_date_and_time['time'] == '01:30'
            assert thread.event_date_and_time['datetime'].isoformat() == '{}T01:30:00+03:00'.format(date_str)
            assert thread.avia_link == 'https://travel-test.yandex.ru/avia/flights/SU-1/?lang=ru&when={}&{}=CODE1'.format(
                date_str, 'to' if event == 'arrival' else 'from'
            )

            assert len(thread.route_stations) == 1
            assert_that(thread.route_stations[0], has_entries({
                'settlement': 'Второй город',
                'iata_code': 'CODE2',
                'title': 'Второй'
            }))
            assert len(thread.codeshares) == 1
            assert_that(thread.codeshares[0], has_entries({
                'company_id': 302,
                'number': 'SV 1'
            }))

            if not trusted:
                assert not hasattr(thread, 'status')
            else:
                assert_that(thread.status, has_entries({
                    'status': 'on_time',
                    'actual_terminal': 'A',
                    'gate': '1',
                    'check_in_desks': '2',
                    'baggage_carousels': '3',
                    'actual_dt': '{}T01:30:00+03:00'.format(date_str),
                    'diverted': has_entries({
                        'settlement': 'Второй город',
                        'iata_code': 'CODE2',
                        'title': 'Второй'
                    })
                }))


@replace_now('2020-02-01')
def test_load_one_day_threads():
    stations = _create_stations()
    _check_one_day_st_for_page(stations, 0, False, 'today', '2020-02-01', 'departure')
    _check_one_day_st_for_page(stations, 1, False, 'today', '2020-02-01', 'departure')
    _check_one_day_st_for_page(stations, 1, False, 'today', '2020-02-01', 'arrival')
    _check_one_day_st_for_page(stations, 2, False, 'today', '2020-02-01', 'departure')
    _check_one_day_st_for_page(stations, 3, False, 'today', '2020-02-01', 'arrival')


@replace_now('2020-02-01')
def test_load_one_day_threads_trusted():
    stations = _create_stations(trusted=True)
    _check_one_day_st_for_page(stations, 0, True, 'tomorrow', '2020-02-02', 'arrival')

    _check_one_day_st_for_page(stations, 1, True, 'today', '2020-02-01', 'departure')
    _check_one_day_st_for_page(stations, 1, True, 'tomorrow', '2020-02-02', 'departure')
    _check_one_day_st_for_page(stations, 1, True, '2020-02-05', '2020-02-05', 'departure')
    _check_one_day_st_for_page(stations, 1, True, 'today', '2020-02-01', 'arrival')
    _check_one_day_st_for_page(stations, 1, True, 'tomorrow', '2020-02-02', 'arrival')
    _check_one_day_st_for_page(stations, 1, True, '2020-02-05', '2020-02-05', 'arrival')

    _check_one_day_st_for_page(stations, 3, True, 'today', '2020-02-01', 'departure')
    _check_one_day_st_for_page(stations, 2, True, 'tomorrow', '2020-02-02', 'departure')
    _check_one_day_st_for_page(stations, 3, True, '2020-02-05', '2020-02-05', 'departure')
    _check_one_day_st_for_page(stations, 2, True, 'today', '2020-02-01', 'arrival')
    _check_one_day_st_for_page(stations, 3, True, 'tomorrow', '2020-02-02', 'arrival')
    _check_one_day_st_for_page(stations, 2, True, '2020-02-05', '2020-02-05', 'arrival')


BARIS_RESPONSE_1 = {'direction': 'departure', 'station': 101}
BARIS_RESPONSE_2 = {
    'direction': 'arrival',
    'station': 101,
    'flights': [
        {
            'airlineID': 301,
            'title': 'SU 1',
            'schedules': [
                {
                    'transportModelID': 201,
                    'time': '10:00',
                    'terminal': 'A',
                    'route': [102, 101],
                    'masks': [
                        {
                            'from': '2020-01-01',
                            'until': '2020-03-10',
                            'on': 1
                        }
                    ]
                }
            ]
        }
    ]
}
BARIS_RESPONSE_3 = {
    'direction': 'departure',
    'station': 101,
    'flights': [
        {
            'airlineID': 301,
            'title': 'SU 1',
            'schedules': [
                {
                    'transportModelID': 201,
                    'time': '10:00',
                    'terminal': 'A',
                    'route': [101, 102],
                    'masks': [
                        {
                            'from': '2020-01-01',
                            'until': '2020-04-01',
                            'on': 1234567
                        }
                    ]
                },
                {
                    'transportModelID': 201,
                    'time': '10:10',
                    'terminal': 'B',
                    'route': [101, 102],
                    'masks': [
                        {
                            'from': '2020-01-01',
                            'until': '2020-04-01',
                            'on': 1234567
                        }
                    ]
                }
            ]
        },
        {
            'airlineID': 302,
            'title': 'SV 1Ц',
            'schedules': [
                {
                    'transportModelID': 201,
                    'time': '11:00',
                    'terminal': 'A',
                    'route': [101, 102],
                    'masks': [
                        {
                            'from': '2020-01-01',
                            'until': '2020-04-01',
                            'on': 1234567
                        }
                    ]
                }
            ]
        }
    ]
}


@replace_now('2020-02-01')
def test_load_all_days_threads():
    stations = _create_stations()

    with mock_baris_response(BARIS_RESPONSE_1):
        st_for_page = _fill_st_for_page(stations, 'all-days', 'departure')

        assert len(st_for_page.companies_by_ids) == 0
        assert len(st_for_page.stations_by_ids) == 1
        assert st_for_page.page_context.event == 'departure'
        assert st_for_page.page_context.special == 'all-days'
        assert len(st_for_page.threads) == 0

    with mock_baris_response(BARIS_RESPONSE_2):
        st_for_page = _fill_st_for_page(stations, 'all-days', 'arrival')

        assert len(st_for_page.companies_by_ids) == 1
        assert len(st_for_page.stations_by_ids) == 2
        assert st_for_page.companies_by_ids[301].id == 301
        assert st_for_page.stations_by_ids[101].id == 101
        assert st_for_page.stations_by_ids[102].id == 102
        assert st_for_page.page_context.event == 'arrival'
        assert st_for_page.page_context.special == 'all-days'

        assert len(st_for_page.threads) == 1
        thread = st_for_page.threads[0]
        assert thread.t_type == 'plane'
        assert thread.number == 'SU 1'
        assert thread.company_id == 301
        assert thread.terminal == 'A'
        assert thread.event_date_and_time['time'] == '10:00'
        assert thread.avia_link == 'https://travel-test.yandex.ru/avia/flights/SU-1/?lang=ru'
        assert thread.days_text == '27 января, 3, 10, 17, 24 февраля, 2, 9 марта'

        assert len(thread.route_stations) == 1
        assert_that(thread.route_stations[0], has_entries({
            'settlement': 'Второй город',
            'iata_code': 'CODE2',
            'title': 'Второй'
        }))

    with mock_baris_response(BARIS_RESPONSE_3):
        st_for_page = _fill_st_for_page(stations, 'all-days', 'departure')

        _check_companies_and_stations_by_ids(st_for_page, False)
        assert st_for_page.page_context.event == 'departure'
        assert st_for_page.page_context.special == 'all-days'
        assert len(st_for_page.threads) == 3

        thread = st_for_page.threads[0]
        assert thread.t_type == 'plane'
        assert thread.number == 'SU 1'
        assert thread.company_id == 301
        assert thread.terminal == 'A'
        assert thread.event_date_and_time['time'] == '10:00'
        assert thread.avia_link == 'https://travel-test.yandex.ru/avia/flights/SU-1/?lang=ru'
        assert thread.days_text == 'ежедневно по 01.04'
        assert len(thread.route_stations) == 1
        assert_that(thread.route_stations[0], has_entries({
            'settlement': 'Второй город',
            'iata_code': 'CODE2',
            'title': 'Второй'
        }))

        thread = st_for_page.threads[1]
        assert thread.t_type == 'plane'
        assert thread.number == 'SU 1'
        assert thread.company_id == 301
        assert thread.terminal == 'B'
        assert thread.event_date_and_time['time'] == '10:10'
        assert thread.avia_link == 'https://travel-test.yandex.ru/avia/flights/SU-1/?lang=ru'
        assert thread.days_text == 'ежедневно по 01.04'
        assert len(thread.route_stations) == 1
        assert_that(thread.route_stations[0], has_entries({
            'settlement': 'Второй город',
            'iata_code': 'CODE2',
            'title': 'Второй'
        }))

        thread = st_for_page.threads[2]
        assert thread.t_type == 'plane'
        assert thread.number == 'SV 1Ц'
        assert thread.company_id == 302
        assert thread.terminal == 'A'
        assert thread.event_date_and_time['time'] == '11:00'
        assert thread.avia_link == 'https://travel-test.yandex.ru/avia/flights/SV-1%D0%A6/?lang=ru'
        assert thread.days_text == 'ежедневно по 01.04'
        assert len(thread.route_stations) == 1
        assert_that(thread.route_stations[0], has_entries({
            'settlement': 'Второй город',
            'iata_code': 'CODE2',
            'title': 'Второй'
        }))
