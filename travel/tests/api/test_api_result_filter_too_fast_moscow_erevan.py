# coding: utf-8
from datetime import datetime

import pytest

from travel.avia.library.python.common.utils.date import UTC_TZ
from travel.avia.library.python.tester.factories import create_station
from travel.avia.ticket_daemon.ticket_daemon.api.flights import Variant, Segment, IATAFlight
from travel.avia.ticket_daemon.ticket_daemon.api.result.filters.too_fast_moscow_erevan import TooFastMoscowErevanFilter


def _fill_segments(variant_flights, flight_params):
    if not flight_params:
        return

    for segment, flight_param in zip(variant_flights.segments, flight_params):
        flight = segment._flight
        flight.station_from = flight_param['from']
        flight.station_to = flight_param['to']
        flight.local_arrival = flight_param['arrival']
        flight.arrival = flight_param['arrival']
        flight.local_departure = flight_param['departure']
        flight.departure = flight_param['departure']


def _create_variant(forward, backward):
    variant = Variant()
    variant.forward.segments = [Segment(IATAFlight()) for _ in range(len(forward))]
    variant.backward.segments = [Segment(IATAFlight()) for _ in range(len(backward))]

    _fill_segments(variant.forward, forward)
    _fill_segments(variant.backward, backward)

    return variant


@pytest.mark.dbuser
def test_filter_too_fast():
    not_our_station = create_station(id=100500)
    erevan_station = create_station(id=TooFastMoscowErevanFilter.ZVARTNOTS_ID)
    moscow_station = create_station(id=TooFastMoscowErevanFilter.DOMODEDOVO_ID)

    unaffected = {
        'from': not_our_station,
        'departure': UTC_TZ.localize(datetime(2020, 2, 2, 14)),
        'to': moscow_station,
        'arrival': UTC_TZ.localize(datetime(2020, 2, 2, 15)),
    }

    erevan_moscow_fast = {
        'from': erevan_station,
        'departure': UTC_TZ.localize(datetime(2020, 2, 2, 14)),
        'to': moscow_station,
        'arrival': UTC_TZ.localize(datetime(2020, 2, 2, 15)),
    }

    erevan_moscow_slow = {
        'from': erevan_station,
        'departure': UTC_TZ.localize(datetime(2020, 2, 2, 14)),
        'to': moscow_station,
        'arrival': UTC_TZ.localize(datetime(2020, 2, 2, 17, 30)),
    }

    filter_ = TooFastMoscowErevanFilter().is_too_fast_variant

    assert filter_(_create_variant([unaffected], [])) is False
    assert filter_(_create_variant([unaffected, erevan_moscow_slow], [])) is False
    assert filter_(_create_variant([unaffected, unaffected], [unaffected, erevan_moscow_fast])) is True
    assert filter_(_create_variant([unaffected], [erevan_moscow_fast])) is True
    assert filter_(_create_variant([erevan_moscow_fast], [])) is True
