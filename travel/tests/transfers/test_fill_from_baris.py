# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date

import pytest
from dateutil import parser

from common.tester.factories import create_station, create_company, create_transport_model
from common.data_api.baris.test_helpers import mock_baris_response
from travel.rasp.library.python.common23.date import environment
from route_search.transfers.fill_from_baris import fill_baris_segments
from route_search.transfers.transfer_segment import BarisTransferSegment


pytestmark = [pytest.mark.dbuser]


def test_make_baris_segments():
    station1 = create_station(id=101)
    station2 = create_station(id=102)
    station3 = create_station(id=103)
    company = create_company(id=301)
    transport_model = create_transport_model(id=201)

    baris_flights = []
    pathfinder_segments = []

    def add_segment_and_flight(
        is_baris_segment, station_from, station_to, flight_number, departure_dt, arrival_dt, msk_start_date
    ):
        flight = None if not is_baris_segment else {
            'airlineID': 301,
            'title': flight_number,
            'departureDatetime': departure_dt,
            'departureTerminal': 'A',
            'departureStation': station_from.id,
            'arrivalDatetime': arrival_dt,
            'arrivalTerminal': '',
            'arrivalStation': station_to.id,
            'transportModelID': 201,
            'route': [station_from.id, station_to.id],
        }
        segment = PathfinderSegmentStub(
            is_baris_segment, station_from, station_to, flight_number, departure_dt, arrival_dt, msk_start_date
        )
        pathfinder_segments.append(segment)
        if flight:
            baris_flights.append(flight)

    def check_transfer_segment(pathfinder_segment):
        assert hasattr(pathfinder_segment, 'transfer_segment')
        segment = pathfinder_segment.transfer_segment
        assert isinstance(segment, BarisTransferSegment)

        assert segment.msk_departure == pathfinder_segment.msk_departure
        assert segment.msk_arrival == pathfinder_segment.msk_arrival
        assert segment.msk_start_date == pathfinder_segment.msk_start_date
        assert segment.station_from == pathfinder_segment.station_from
        assert segment.station_to == pathfinder_segment.station_to
        assert segment.number == pathfinder_segment.flight_number.replace('-', ' ')
        assert segment.thread.number == pathfinder_segment.flight_number.replace('-', ' ')
        assert segment.company == company
        assert segment.transport.model == transport_model

    add_segment_and_flight(
        True, station1, station2, 'SU 1',
        '2020-10-03 01:10:00+03:00', '2020-10-03 02:20:00+03:00', date(2020, 10, 3)
    )
    add_segment_and_flight(
        True, station1, station2, 'SU 1',
        '2020-10-03 01:10:00+03:00', '2020-10-03 02:20:00+03:00', date(2020, 10, 3)
    )
    add_segment_and_flight(
        True, station1, station2, 'TU 1',
        '2020-10-03 03:30:00+03:00', '2020-10-03 04:50:00+03:00', date(2020, 10, 3)
    )
    add_segment_and_flight(
        True, station1, station3, 'SU 1',
        '2020-10-03 01:10:00+03:00', '2020-10-03 07:00:00+03:00', date(2020, 10, 3)
    )
    add_segment_and_flight(
        True, station1, station2, 'SU 1',
        '2020-10-04 01:10:00+03:00', '2020-10-04 02:20:00+03:00', date(2020, 10, 4)
    )
    add_segment_and_flight(
        True, station2, station3, 'SU 1',
        '2020-10-03 05:50:00+03:00', '2020-10-03 07:00:00+03:00', date(2020, 10, 3)
    )
    add_segment_and_flight(
        False, station1, station2, 'BU 1',
        '2020-10-03 10:00:00+03:00', '2020-10-03 11:00:00+03:00', date(2020, 10, 3)
    )

    now = environment.now_aware()
    baris_response = {
        'departureStations': [101, 102, 103],
        'arrivalStations': [102, 103],
        'flights': baris_flights
    }

    with mock_baris_response(baris_response) as m_get_p2p_search:
        fill_baris_segments(pathfinder_segments, now)

        assert len(m_get_p2p_search.mock_calls) == 2

    for pathfinder_segment in pathfinder_segments[:-1]:
        check_transfer_segment(pathfinder_segment)

    assert hasattr(pathfinder_segments[-1], 'transfer_segment') is False


class PathfinderSegmentStub():
    def __init__(self, is_baris_segment, station_from, station_to, flight_number,
                 departure_dt, arrival_dt, msk_start_date):
        self.is_baris_segment = is_baris_segment
        self.station_from = station_from
        self.station_to = station_to
        self.flight_number = flight_number.replace(' ', '-')
        self.msk_departure = parser.parse(departure_dt)
        self.msk_arrival = parser.parse(arrival_dt)
        self.msk_start_date = msk_start_date
        self.convenience = 1
