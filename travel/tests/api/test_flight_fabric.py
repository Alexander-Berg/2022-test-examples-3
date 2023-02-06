# -*- coding: utf-8 -*-
from datetime import datetime

from travel.avia.ticket_daemon.ticket_daemon.api.flights import FlightFabric


def test_flight_number():
    flight_fabric = FlightFabric()

    data = [
        ['SU', '0036', u'SU 0036'],
        ['S7', '56', u'S7 56'],
        ['SU', '1122', u'SU 1122'],
        [u'ДЖ', '106', u'ДЖ 106'],
    ]
    flight_date = datetime(2017, 9, 1)

    for company_iata, pure_number, expected_number in data:
        flight = flight_fabric.create(
            company_iata=company_iata,
            pure_number=pure_number,
            station_from_iata='MOW',
            station_to_iata='SVX',
            local_departure=flight_date,
            local_arrival=flight_date,
        )

        assert flight.number == expected_number


def test_uniq_flights():
    flight_fabric = FlightFabric()

    company_iata = 'DP'
    pure_number = 404
    station_from_iata = 'MOW'
    station_to_iata = 'SVX'
    flight_date = datetime(2017, 9, 1)
    fare_code = 'FARE_CODE'
    another_fare_code = 'ANOTHER_FARE_CODE'

    segment = flight_fabric.create(
        company_iata=company_iata,
        pure_number=pure_number,
        station_from_iata=station_from_iata,
        station_to_iata=station_to_iata,
        local_departure=flight_date,
        local_arrival=flight_date,
        fare_code=fare_code,
    )
    same_segment = flight_fabric.create(
        company_iata=company_iata,
        pure_number=pure_number,
        station_from_iata=station_from_iata,
        station_to_iata=station_to_iata,
        local_departure=flight_date,
        local_arrival=flight_date,
        fare_code=fare_code,
    )
    another_segment = flight_fabric.create(
        company_iata=company_iata,
        pure_number=pure_number,
        station_from_iata=station_from_iata,
        station_to_iata=station_to_iata,
        local_departure=flight_date,
        local_arrival=flight_date,
        fare_code=another_fare_code,
    )

    assert segment._flight == same_segment._flight
    assert segment._flight != another_segment._flight
    assert segment.fare_code == same_segment.fare_code == fare_code
    assert another_segment.fare_code == another_fare_code
