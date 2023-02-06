# -*- coding: utf-8 -*-
from datetime import datetime

import pytest

from travel.avia.library.python.common.models.geo import CodeSystem
from travel.avia.library.python.tester.factories import create_station
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon.ticket_daemon.api.flights import IATAFlight
from travel.avia.ticket_daemon.ticket_daemon.daemon.utils import fill


def create_airport(iata, **kwargs):
    iata_system = CodeSystem.objects.get(code='iata')
    return create_station(t_type='plane', __={'codes': {iata_system: iata}}, **kwargs)


def _create_flight(flight_number, **kwargs):
    return fill(
        IATAFlight(),
        company_iata=flight_number.split()[0],
        number=flight_number,
        station_from_iata=None,
        station_to_iata=None,
        **kwargs
    )


@pytest.mark.dbuser
def test_complete_flights_stations():
    airport_dme = create_airport(iata='DME')
    airport_svx = create_airport(iata='SVX')

    flight = fill(
        IATAFlight(),
        company_iata='SU', number='SU 123',
        station_from_iata=airport_dme.iata,
        station_to_iata=airport_svx.iata,
        local_departure=datetime.now(),
        local_arrival=datetime.now(),
    )
    reset_all_caches()

    flight.complete()
    assert flight.station_from
    assert flight.station_from.id == airport_dme.id
    assert flight.station_to
    assert flight.station_to.id == airport_svx.id
    assert flight.departure
    assert flight.arrival


def stations_eq(one, two):
    __tracebackhide__ = True
    return one and two and one.id == two.id
