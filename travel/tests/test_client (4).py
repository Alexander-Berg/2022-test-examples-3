# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from dateutil import parser
from json import dumps as json_dumps

import httpretty

from travel.library.python.base_http_client import CircuitBreakerConfig, RetryConfig
from travel.rasp.library.python.api_clients.baris import BarisClient

HOST = 'http://test-baris.ru/'
TEST_TABLO_BODY = {'tablo': 123}
TEST_STATION_ID = '101'
TEST_NATIONAL_VERSION = 'ru'
TEST_FLIGHT_NUMBER = 'SU-1'


def _register_baris_handler(url_path, response=None, params=None, status_code=200):
    response = response or TEST_TABLO_BODY
    params = params or {}

    def request_callback(request, uri, response_headers):
        for param_name, param_value in params.items():
            assert param_value in request.querystring[param_name]
        return [status_code, response_headers, json_dumps(response)]

    httpretty.register_uri(
        httpretty.GET, '{}api/v1/{}'.format(HOST, url_path),
        status=status_code, content_type='application/json',
        body=request_callback
    )


def _get_baris_client():
    return BarisClient(
        HOST,
        timeout=2,
        circuit_breaker_config=CircuitBreakerConfig(fail_max=3, reset_timeout=20),
        retry_config=RetryConfig(total=2)
    )


@httpretty.activate
def test_flight_board():
    after_dt = parser.parse('2020-02-01T00:00:00+03:00')
    before_dt = parser.parse('2020-02-02T00:00:00+03:00')

    _register_baris_handler(
        'flight-board/{}/'.format(TEST_STATION_ID), params={
            'direction': 'departure',
            'after': '2020-02-01T00:00:00',
            'before': '2020-02-02T00:00:00'
        }
    )

    result = _get_baris_client().flight_board(TEST_STATION_ID, direction='departure', after=after_dt, before=before_dt)
    assert result == TEST_TABLO_BODY


@httpretty.activate
def test_flight_board_schedule():
    _register_baris_handler(
        'flight-board/{}/schedule/'.format(TEST_STATION_ID), params={'direction': 'departure'}
    )

    result = _get_baris_client().flight_board_schedule(TEST_STATION_ID, direction='departure')
    assert result == TEST_TABLO_BODY


@httpretty.activate
def test_flight_p2p():
    station_from_ids = [TEST_STATION_ID]
    station_to_ids = [TEST_STATION_ID]
    before_dt = parser.parse('2020-02-02T00:00:00+03:00')
    after_dt = parser.parse('2020-02-01T00:00:00+03:00')

    _register_baris_handler(
        'flight-p2p', params={
            'from': TEST_STATION_ID,
            'to': TEST_STATION_ID,
            'national_version': TEST_NATIONAL_VERSION,
            'show_banned': 'True',
            'before': '2020-02-02T00:00:00 03:00',
            'after': '2020-02-01T00:00:00 03:00'
        }
    )
    result = _get_baris_client().flight_p2p(
        station_from_ids, station_to_ids, TEST_NATIONAL_VERSION, True, before_dt, after_dt
    )
    assert result == TEST_TABLO_BODY


@httpretty.activate
def test_flight_p2p_with_numbers():
    station_from_ids = [TEST_STATION_ID]
    station_to_ids = [TEST_STATION_ID]
    flight_numbers = [TEST_FLIGHT_NUMBER]
    before_dt = parser.parse('2020-02-02T00:00:00+03:00')
    after_dt = parser.parse('2020-02-01T00:00:00+03:00')

    _register_baris_handler(
        'flight-p2p', params={
            'from': TEST_STATION_ID,
            'to': TEST_STATION_ID,
            'national_version': TEST_NATIONAL_VERSION,
            'show_banned': 'True',
            'flight': TEST_FLIGHT_NUMBER,
            'before': '2020-02-02T00:00:00 03:00',
            'after': '2020-02-01T00:00:00 03:00'
        }
    )
    result = _get_baris_client().flight_p2p_with_numbers(
        station_from_ids, station_to_ids, TEST_NATIONAL_VERSION, True, flight_numbers, before_dt, after_dt
    )
    assert result == TEST_TABLO_BODY


@httpretty.activate
def test_flight_p2p_schedule():
    station_from_ids = [TEST_STATION_ID]
    station_to_ids = [TEST_STATION_ID]

    _register_baris_handler(
        'flight-p2p-schedule', params={
            'from': TEST_STATION_ID,
            'to': TEST_STATION_ID,
            'national_version': TEST_NATIONAL_VERSION,
            'show_banned': 'True'
        }
    )
    result = _get_baris_client().flight_p2p_schedule(
        station_from_ids, station_to_ids, national_version=TEST_NATIONAL_VERSION, show_banned=True
    )
    assert result == TEST_TABLO_BODY


@httpretty.activate
def test_flight_schedule():
    _register_baris_handler('flight-schedule/{}'.format(TEST_FLIGHT_NUMBER.replace('-', '/')))
    result = _get_baris_client().flight_schedule(TEST_FLIGHT_NUMBER)
    assert result == TEST_TABLO_BODY


@httpretty.activate
def test_delayed_flights():
    station_from_ids = [TEST_STATION_ID]
    _register_baris_handler('delayed-flights', params={
        'station': TEST_STATION_ID
    })
    result = _get_baris_client().delayed_flights(station_from_ids)
    assert result == TEST_TABLO_BODY


@httpretty.activate
def test_flight_p2p_summary():
    _register_baris_handler('flight-p2p-summary')
    result = _get_baris_client().flight_p2p_summary()
    assert result == TEST_TABLO_BODY
