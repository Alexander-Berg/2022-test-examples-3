# coding: utf-8
from datetime import datetime

from mock import Mock
import pytest

from travel.avia.ticket_daemon.ticket_daemon.api.flights import _flight_key, Flight


@pytest.mark.parametrize('key, flight', [
    ('ДЖ 555.1011T1213', Mock(number='ДЖ 555', local_departure=datetime(2016, 10, 11, 12, 13))),
    ('SU 132.1011T1213', Mock(number=u'SU 132', local_departure=datetime(2016, 10, 11, 12, 13))),
    ('ЮГ 456.0304T0506', Mock(number=u'\u042e\u0413 456', local_departure=datetime(2016, 3, 4, 5, 6))),
], ids=['RU', 'ASCII', 'UNICODE'])
def test_flight_key(key, flight):
    flight_key = _flight_key(flight)
    assert isinstance(flight_key, str)
    assert flight_key == key


@pytest.mark.parametrize('key, flight_number', [
    ('ДЖ 555-None-None-None-None-None', 'ДЖ 555'),
    ('SU 132-None-None-None-None-None', u'SU 132'),
    ('ЮГ 456-None-None-None-None-None', u'\u042e\u0413 456'),
], ids=['RU', 'ASCII', 'UNICODE'])
def test_flight_property_key_flight_number_encoding(key, flight_number):
    flight = Flight()
    flight.number = flight_number
    # flight.company = None
    # flight.station_from = None
    # flight.station_to = None
    flight.local_departure = None
    flight.local_arrival = None

    assert flight.key == key
