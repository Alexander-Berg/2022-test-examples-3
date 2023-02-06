# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import json
from copy import deepcopy
from datetime import date, datetime

import mock
from django.conf import settings

from common.utils.date import MSK_TZ, UTC_TZ
from travel.rasp.wizards.wizard_lib.cache import Translations


EMPTY_BARIS_FLIGHT = {
    'airlineID': 0,
    'number': '?',
    'title': '?',
    'datetime': '2000-01-01T00:00:00+00:00',
    'terminal': '',
    'codeshares': None,
    'status': {
        'status': 'unknown',
        'departure': '',
        'arrival': '',
        'departureStatus': 'unknown',
        'arrivalStatus': 'unknown',
        'arrivalGate': '',
        'arrivalTerminal': '',
        'departureGate': '',
        'departureTerminal': '',
        'departureSource': '',
        'arrivalSource': '',
        'checkInDesks': '',
        'baggageCarousels': '',
        'createdAtUtc': '2000-01-01 00:00:00',
        'updatedAtUtc': '2000-01-01 00:00:00',
        'departureUpdatedAtUtc': '',
        'arrivalUpdatedAtUtc': '',
        'diverted': False,
        'divertedAirportCode': '',
        'divertedAirportID': 0
    },
    'route': []
}


def make_baris_flight(station_from, station_to, flight_params):
    flight = deepcopy(EMPTY_BARIS_FLIGHT)
    flight['route'] = [station_from.id, station_to.id]
    for name, value in flight_params.items():
        if name == 'status':
            for status_name, status_value in flight_params[name].items():
                flight[name][status_name] = status_value
        elif name in flight:
            flight[name] = value
    return flight


def make_dummy_baris_tablo_response(station, direction, flight_params_list):
    flights = [
        make_baris_flight(flight_params['station_from'], flight_params['station_to'], flight_params)
        for flight_params
        in flight_params_list
    ]

    response = {
        'station': station.id,
        'direction': direction,
        'flights': flights,
    }
    return json.dumps(response)


def make_empty_baris_tablo_response(station, direction):
    response = {
        'station': station.id,
        'direction': direction,
        'flights': [],
    }
    return json.dumps(response)


def msk_dt(*args):
    return MSK_TZ.localize(datetime(*args))


def utc_dt(*args):
    return datetime(*args, tzinfo=UTC_TZ)


def make_dummy_raw_segment(departure_station, arrival_station, transport_subtype_id=None):
    return mock.Mock(**{
        'arrival_dt': utc_dt(2000, 1, 2),
        'arrival_station': arrival_station,
        'departure_dt': utc_dt(2000, 1, 1),
        'departure_station': departure_station,
        'event_stop.platform': None,
        'event_stop.stops_text': None,
        'thread.express_type': None,
        'thread.number': 'some_number',
        'thread.t_subtype_id': transport_subtype_id,
        'thread.title': Translations(**{lang: 'some_title' for lang in settings.MODEL_LANGUAGES}),
        'thread.uid': 'some_thread_uid',
        'thread_start_dt': utc_dt(2000, 1, 1),
    })


def make_dummy_segment(
    departure_station, arrival_station, price=None, transport_subtype_id=None, thread_express_type=None
):
    return mock.Mock(
        departure_local_dt=msk_dt(2000, 1, 1),
        arrival_local_dt=msk_dt(2000, 1, 2),
        departure_station=departure_station,
        arrival_station=arrival_station,
        thread_express_type=thread_express_type,
        thread_start_date=date(2000, 1, 1),
        thread_transport_subtype_id=transport_subtype_id,
        thread_uid='some_thread_uid',
        train_number='some_number',
        train_title=Translations(**{lang: 'some_title' for lang in settings.MODEL_LANGUAGES}),
        price=price
    )
