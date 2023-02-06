# coding: utf-8
from datetime import datetime

import pytest

from travel.avia.ticket_daemon.ticket_daemon.api.flights import IATAFlight, Variant


@pytest.mark.parametrize('first_data, second_data', [
    (
        [
            ("EL 909", datetime(2000, 1, 1)),
            ("EL 100", datetime(2000, 1, 1)),
        ],
        [
            ("EL 909", datetime(2000, 1, 1)),
            ("EL 100", datetime(2000, 1, 2)),
        ]
    ),
    (
        [
            ("EL 909", datetime(2000, 1, 1)),
            ("EL 100", datetime(2000, 1, 1)),
        ],
        [
            ("EL 909", datetime(2000, 1, 1)),
            ("EL 101", datetime(2000, 1, 1)),
        ]
    ),
])
def test_by_flights_key(first_data, second_data):
    def create_variant(segments):
        variant = Variant()
        variant.forward.segments = []

        variant.forward.segments = []
        for segment in segments:
            flight = IATAFlight()
            flight.number = segment[0]
            flight.local_departure = segment[1]
            variant.forward.segments.append(flight)

        return variant

    first_variant = create_variant(first_data)
    second_variant = create_variant(second_data)

    assert not first_variant.by_flights_key == second_variant.by_flights_key
    assert first_variant.by_flights_key == tuple((f.number, f.local_departure) for f in first_variant.all_segments)
    assert second_variant.by_flights_key == tuple((f.number, f.local_departure) for f in second_variant.all_segments)
