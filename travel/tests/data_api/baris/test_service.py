# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import copy
import pytz
from datetime import datetime
from dateutil import parser

import requests
import mock
from hamcrest import has_entries, assert_that, contains, contains_inanyorder

from common.data_api.baris.instance import baris
from common.tester.utils.datetime import replace_now

from travel.rasp.library.python.api_clients.baris import BarisClient


ONE_DAY_TABLO_BARIS_RESPONSE = {
    'direction': 'departure',
    'station': 101,
    'flights': [
        {
            'airlineID': 301,
            'transportModelID': 201,
            'title': 'SU 1',
            'datetime': '2020-02-01T01:30:00+03:00',
            'terminal': 'A',
            'codeshares': [{
                'airlineID': 302,
                'title': 'SV 1'
            }],
            'route': [101, 102, 103],
            'source': 'flight-board',
            'status': {
                'departure': '2020-02-01 02:30:00',
                'departureStatus': 'delayed',
                'departureGate': '1',
                'departureTerminal': 'B',
                'checkInDesks': '2',
                'baggageCarousels': '3',
                'diverted': True,
                'divertedAirportID': 104
            }
        }
    ]
}

ALL_DAYS_TABLO_BARIS_RESPONSE = {
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
                    'startTime': '22:00',
                    'startDayShift': -1,
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
                    'route': [101, 103],
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
            'transportModelID': 201,
            'title': 'SV 1',
            'schedules': [
                {
                    'transportModelID': 201,
                    'time': '11:00',
                    'terminal': 'A',
                    'route': [101, 102, 104],
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

ONE_DAY_P2P_BARIS_RESPONSE = {
    'departureStations': [101, 102],
    'arrivalStations': [103],
    'flights': [
        {
            'airlineID': 301,
            'title': 'SU 1',
            'departureDatetime': '2020-06-01T01:30:00+03:00',
            'departureTerminal': 'A',
            'departureStation': 101,
            'arrivalDatetime': '2020-06-01T05:00:00+05:00',
            'arrivalTerminal': '',
            'arrivalStation': 103,
            'transportModelID': 201,
            'codeshares': [{
                'airlineID': 302,
                'title': 'SV 1'
            }],
            'route': [101, 103, 102],
            'source': 'flight-board',
        }
    ]
}

ALL_DAYS_P2P_BARIS_RESPONSE = {
    'departureStations': [101, 102],
    'arrivalStations': [103],
    'flights': [
        {
            'airlineID': 301,
            'title': 'SU 1',
            'departureTime': '01:30',
            'departureTimezone': '+0300',
            'departureTerminal': 'A',
            'departureStation': 101,
            'arrivalTime': '05:00',
            'arrivalTimezone': '+0500',
            'arrivalTerminal': '',
            'arrivalStation': 103,
            'arrivalDayShift': 0,
            'startTime': '22:30',
            'startDayShift': -1,
            'transportModelID': 201,
            'codeshares': [{
                'airlineID': 302,
                'title': 'SV 1'
            }],
            'route': [101, 103, 102],
            'source': 'flight-board',
            'masks': [
                {
                    'from': '2020-06-01',
                    'until': '2020-06-25',
                    'on': 34
                }
            ]
        }
    ]
}

FLIGHT_SCHEDULE_BARIS_RESPONSE = {
    'title': 'SU 1',
    'airlineID': 301,
    'schedules': [
        {
            'transportModelID': 201,
            'route': [
                {
                    'airportID': 101,
                    'departureTime': '22:30:00'
                },
                {
                    'airportID': 102,
                    'arrivalTime': '01:10:00',
                    'arrivalDayShift': 1,
                    'departureTime': '13:30:00',
                    'departureDayShift': 1,
                },
                {
                    'airportID': 103,
                    'arrivalTime': '01:20:00',
                    'arrivalDayShift': 2,
                }
            ],
            'masks': [
                {
                    'from': '2020-11-23',
                    'until': '2020-12-01',
                    'on': 12
                }
            ]
        }
    ]
}

DELAYED_FLIGHTS_BARIS_RESPONSE = {
    'stations': [
        {'id': 101, 'cancelled': 3, 'delayed': 2},
        {'id': 102, 'cancelled': 0, 'delayed': 0},
        {'id': 103, 'cancelled': 4},
        {'id': 104, 'delayed': 5},
    ]
}

P2P_SUMMARY_BARIS_RESPONSE = {
    "flights": [
        {
            'departureStation': 211,
            'arrivalStation': 212,
            'flightsCount': 1,
            'totalFlightsCount': 1
        },
        {
            'departureStation': 212,
            'arrivalStation': 213,
            'flightsCount': 3,
            'dopFlightsCount': 2,
            'totalFlightsCount': 4
        }
    ]
}


@replace_now('2020-02-01')
def test_get_station_tablo():
    with mock.patch.object(BarisClient, 'flight_board', return_value=copy.deepcopy(ONE_DAY_TABLO_BARIS_RESPONSE)):
        after_dt = parser.parse('2020-02-01T00:00:00+03:00')
        before_dt = parser.parse('2020-02-02T00:00:00+03:00')
        tablo = baris.get_station_tablo(101, after=after_dt, before=before_dt)

        assert tablo.stations_ids == {101, 102, 103, 104}
        assert tablo.companies_ids == {301, 302}

        assert len(tablo.flights) == 1
        assert_that(tablo.flights[0], has_entries({
            'airlineID': 301,
            'transportModelID': 201,
            'title': 'SU 1',
            'datetime': '2020-02-01T01:30:00+03:00',
            'terminal': 'A',
            'codeshares': contains(has_entries({
                'airlineID': 302,
                'title': 'SV 1'
            })),
            'route': contains(101, 102, 103),
            'source': 'flight-board',
            'status': has_entries({
                'departure': '2020-02-01 02:30:00',
                'departureStatus': 'delayed',
                'departureGate': '1',
                'departureTerminal': 'B',
                'checkInDesks': '2',
                'baggageCarousels': '3',
                'diverted': True,
                'divertedAirportID': 104
            })
        }))


@replace_now('2020-02-01')
def test_get_station_all_days_tablo():
    with mock.patch.object(BarisClient, 'flight_board_schedule', return_value=copy.deepcopy(ALL_DAYS_TABLO_BARIS_RESPONSE)):
        tablo = baris.get_station_all_days_tablo(101, 'Etc/GMT-3', 'ru')

        assert tablo.stations_ids == {101, 102, 103, 104}
        assert tablo.companies_ids == {301, 302}
        assert tablo.transport_models_ids == {201}

        assert len(tablo.flights) == 3
        assert_that(tablo.flights, contains(
            has_entries({
                'airlineID': 301,
                'transportModelID': 201,
                'title': 'SU 1',
                'terminal': 'A',
                'route': [101, 102],
                'time': '10:00',
                'daysText': 'ежедневно по 01.04',
                'nearestDatetime': pytz.timezone('Etc/GMT-3').localize(datetime(2020, 2, 1, 10, 0, 0)),
                'naiveStart': datetime(2020, 1, 31, 22, 0, 0),
            }),
            has_entries({
                'airlineID': 301,
                'transportModelID': 201,
                'title': 'SU 1',
                'terminal': 'B',
                'route': [101, 103],
                'time': '10:10',
                'daysText': 'ежедневно по 01.04',
                'nearestDatetime': pytz.timezone('Etc/GMT-3').localize(datetime(2020, 2, 1, 10, 10, 0)),
            }),
            has_entries({
                'airlineID': 302,
                'transportModelID': 201,
                'title': 'SV 1',
                'terminal': 'A',
                'route': [101, 102, 104],
                'time': '11:00',
                'daysText': 'ежедневно по 01.04',
                'nearestDatetime': pytz.timezone('Etc/GMT-3').localize(datetime(2020, 2, 1, 11, 0, 0)),
            }),
        ))


def test_get_baris_404_station_tablo():
    mock_response = mock.Mock()
    mock_response.status_code = 404
    error = requests.HTTPError(response=mock_response)
    with mock.patch.object(BarisClient, 'flight_board', side_effect=error):
        tablo = baris.get_station_tablo(101)

        assert tablo.stations_ids == {101}
        assert len(tablo.flights) == 0

    with mock.patch.object(BarisClient, 'flight_board_schedule', side_effect=error):
        tablo = baris.get_station_all_days_tablo(101, 'Etc/GMT-3', 'ru')

        assert tablo.stations_ids == {101}
        assert len(tablo.flights) == 0


@replace_now('2020-06-01')
def test_get_p2p_search():
    with mock.patch.object(BarisClient, 'flight_p2p', return_value=copy.deepcopy(ONE_DAY_P2P_BARIS_RESPONSE)):
        search_response = baris.get_p2p_search(
            [101, 102], [103],
            after=datetime(2020, 6, 1, 0, 0, 0), before=datetime(2020, 6, 1, 4, 0, 0)
        )

        assert search_response.stations_ids == {101, 102, 103}
        assert search_response.companies_ids == {301, 302}
        assert search_response.transport_models_ids == {201}

        assert len(search_response.flights) == 1
        assert_that(search_response.flights[0], has_entries({
            'airlineID': 301,
            'title': 'SU 1',
            'departureDatetime': '2020-06-01T01:30:00+03:00',
            'departureTerminal': 'A',
            'departureStation': 101,
            'arrivalDatetime': '2020-06-01T05:00:00+05:00',
            'arrivalTerminal': '',
            'arrivalStation': 103,
            'transportModelID': 201,
            'codeshares': contains(has_entries({
                'airlineID': 302,
                'title': 'SV 1'
            })),
            'route': contains(101, 103, 102),
            'source': 'flight-board',
        }))

    search_response = baris.get_p2p_search([], [101])
    assert search_response.flights == []
    search_response = baris.get_p2p_search([101], [])
    assert search_response.flights == []


@replace_now('2020-06-01')
def test_get_p2p_all_days_search():
    with mock.patch.object(BarisClient, 'flight_p2p_schedule', return_value=copy.deepcopy(ALL_DAYS_P2P_BARIS_RESPONSE)):
        search_response = baris.get_p2p_all_days_search(
            lang='ru', station_from_ids=[101, 102], station_to_ids=[103],
            from_timezone='Etc/GMT-3', to_timezone='Etc/GMT-5'
        )

        assert search_response.stations_ids == {101, 102, 103}
        assert search_response.companies_ids == {301, 302}
        assert search_response.transport_models_ids == {201}

        assert len(search_response.flights) == 1
        assert_that(search_response.flights[0], has_entries({
            'airlineID': 301,
            'title': 'SU 1',
            'departureTime': '01:30',
            'departureTimezone': '+0300',
            'departureTerminal': 'A',
            'departureStation': 101,
            'arrivalTime': '05:00',
            'arrivalTimezone': '+0500',
            'arrivalTerminal': '',
            'arrivalStation': 103,
            'arrivalDayShift': 0,
            'startTime': '22:30',
            'startDayShift': -1,
            'transportModelID': 201,
            'codeshares': contains(has_entries({
                'airlineID': 302,
                'title': 'SV 1'
            })),
            'route': contains(101, 103, 102),
            'source': 'flight-board',
            'daysText': 'ср, чт по 25.06',
            'runDays': {'2020': {'6': [
                0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0
            ]}}
        }))
        assert search_response.flights[0]['departure'].isoformat() == '2020-06-03T01:30:00+03:00'
        assert search_response.flights[0]['arrival'].isoformat() == '2020-06-03T05:00:00+05:00'
        assert search_response.flights[0]['naiveStart'] == datetime(2020, 6, 2, 22, 30, 0)

    # Использование параметра result_timezone, без сдвига даты
    with mock.patch.object(BarisClient, 'flight_p2p_schedule', return_value=copy.deepcopy(ALL_DAYS_P2P_BARIS_RESPONSE)):
        search_response = baris.get_p2p_all_days_search(
            lang='ru', station_from_ids=[101, 102], station_to_ids=[103],
            from_timezone='Etc/GMT-3', to_timezone='Etc/GMT-5',
            result_timezone='Etc/GMT-3',
        )

        assert_that(search_response.flights[0], has_entries({
            'departureTime': '01:30',
            'daysText': 'ср, чт по 25.06',
            'runDays': {'2020': {'6': [
                0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0
            ]}},
            'tzDaysText': 'ср, чт по 25.06',
            'tzRunDays': {'2020': {'6': [
                0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0
            ]}}
        }))

    # Использование параметра result_timezone, сдвиг даты
    with mock.patch.object(BarisClient, 'flight_p2p_schedule', return_value=copy.deepcopy(ALL_DAYS_P2P_BARIS_RESPONSE)):
        search_response = baris.get_p2p_all_days_search(
            lang='ru', station_from_ids=[101, 102], station_to_ids=[103],
            from_timezone='Etc/GMT-3', to_timezone='Etc/GMT-5',
            result_timezone='Etc/GMT-1',
        )

        assert_that(search_response.flights[0], has_entries({
            'departureTime': '01:30',
            'daysText': 'ср, чт по 25.06',
            'runDays': {'2020': {'6': [
                0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0
            ]}},
            'tzDaysText': 'вт, ср по 24.06',
            'tzRunDays': {'2020': {'6': [
                0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0
            ]}}
        }))

    # Без дней хождения
    with mock.patch.object(BarisClient, 'flight_p2p_schedule', return_value=copy.deepcopy(ALL_DAYS_P2P_BARIS_RESPONSE)):
        search_response = baris.get_p2p_all_days_search(
            lang='ru', station_from_ids=[101, 102], station_to_ids=[103],
            from_timezone='Etc/GMT-3', to_timezone='Etc/GMT-5',
            result_timezone='Etc/GMT-1', add_days_text=False, add_run_days=False
        )

        assert 'daysText' not in search_response.flights[0]
        assert 'tzDaysText' not in search_response.flights[0]
        assert 'runDays' not in search_response.flights[0]
        assert 'tzRunDays' not in search_response.flights[0]

    # Без указания станций прилета или отлета
    search_response = baris.get_p2p_all_days_search('ru', [], [101], 'Etc/GMT-3', 'Etc/GMT-3')
    assert search_response.flights == []
    search_response = baris.get_p2p_all_days_search('ru', [101], [], 'Etc/GMT-3', 'Etc/GMT-3')
    assert search_response.flights == []

    # Без указания таймзоны вылета или прилета
    search_response = baris.get_p2p_all_days_search('ru', [101], [102], None, 'Etc/GMT-3')
    assert search_response.flights == []
    search_response = baris.get_p2p_all_days_search('ru', [101], [102], 'Etc/GMT-3', None)
    assert search_response.flights == []


def test_get_flight_schedule():
    with mock.patch.object(BarisClient, 'flight_schedule', return_value=copy.deepcopy(FLIGHT_SCHEDULE_BARIS_RESPONSE)):
        flight_data = baris.get_flight_schedule('SU-1')

        assert flight_data.stations_ids == {101, 102, 103}
        assert flight_data.companies_ids == {301}
        assert flight_data.transport_models_ids == {201}

        assert len(flight_data.flights) == 1
        assert_that(flight_data.flights[0], has_entries({
            'title': 'SU 1',
            'airlineID': 301,
            'schedules': contains(
                has_entries({
                    'transportModelID': 201,
                    'route': contains(
                        has_entries({
                            'airportID': 101,
                            'departureTime': '22:30:00'
                        }),
                        has_entries({
                            'airportID': 102,
                            'arrivalTime': '01:10:00',
                            'arrivalDayShift': 1,
                            'departureTime': '13:30:00',
                            'departureDayShift': 1,
                        }),
                        has_entries({
                            'airportID': 103,
                            'arrivalTime': '01:20:00',
                            'arrivalDayShift': 2,
                        })
                    ),
                    'masks': contains(
                        has_entries({
                            'from': '2020-11-23',
                            'until': '2020-12-01',
                            'on': 12
                        })
                    )
                })
            )
        }))


def test_get_delayed_flights():
    with mock.patch.object(BarisClient, 'delayed_flights', return_value=copy.deepcopy(DELAYED_FLIGHTS_BARIS_RESPONSE)):
        delayed_fligths = baris.get_delayed_flights([101, 102, 103, 104])
        assert_that(delayed_fligths, has_entries({
            101: has_entries({'cancelled': 3, 'delayed': 2}),
            102: has_entries({'cancelled': 0, 'delayed': 0}),
            103: has_entries({'cancelled': 4, 'delayed': 0}),
            104: has_entries({'cancelled': 0, 'delayed': 5})
        }))


def test_p2p_summary():
    with mock.patch.object(BarisClient, 'flight_p2p_summary', return_value=copy.deepcopy(P2P_SUMMARY_BARIS_RESPONSE)):
        station_pairs = baris.get_p2p_summary()

        assert_that(station_pairs, contains_inanyorder((211, 212), (212, 213)))


def test_station_summary():
    with mock.patch.object(BarisClient, 'flight_p2p_summary', return_value=copy.deepcopy(P2P_SUMMARY_BARIS_RESPONSE)):
        departure_stations, arrival_stations = baris.get_station_summary()

        assert_that(departure_stations, contains_inanyorder(211, 212))
        assert_that(arrival_stations, contains_inanyorder(212, 213))
