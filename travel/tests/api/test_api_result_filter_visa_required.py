# coding: utf-8
import pytest

from travel.avia.library.python.tester.factories import create_settlement, create_station

from travel.avia.ticket_daemon.ticket_daemon.api.flights import Variant, Segment, IATAFlight
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon.ticket_daemon.api.result.filtering import visa_required_for_inner_flights


def fill_segments(variant_flights, country_id_list):
    for i, _ in enumerate(variant_flights.segments):
        create_settlement(id=country_id_list[i + 1])
        variant_flights.segments[i]._flight.station_to = create_station(settlement_id=country_id_list[i + 1])
        variant_flights.segments[i]._flight.station_to.country_id = country_id_list[i + 1]
        create_settlement(id=country_id_list[i])
        variant_flights.segments[i]._flight.station_from = create_station(settlement_id=country_id_list[i])
        variant_flights.segments[i]._flight.station_from.country_id = country_id_list[i]


@pytest.mark.dbuser
@pytest.mark.parametrize('stations_country_id,expected', [
    (([225, 168, 149, 225], [225, 225]), False),
    (([225, 149, 183, 225], [225, 225]), True),
    (([225, 149, 168], [225, 225]), False),
    (([225, 225], [225, 225]), False),
    (([225, 168, 149, 225], [225, 1004, 225]), True),
    (([225, 225], []), False)
])
def test_filter_visa_required(stations_country_id, expected):
    reset_all_caches()

    variant = Variant()
    variant.forward.segments = [Segment(IATAFlight()) for _ in range(len(stations_country_id[0]) - 1)]
    variant.backward.segments = [Segment(IATAFlight()) for _ in range(len(stations_country_id[1]) - 1)]

    fill_segments(variant.forward, stations_country_id[0])
    fill_segments(variant.backward, stations_country_id[1])

    actual = visa_required_for_inner_flights(variant)

    assert actual == expected
