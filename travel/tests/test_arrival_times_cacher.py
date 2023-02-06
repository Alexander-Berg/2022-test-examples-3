# coding=utf-8
from __future__ import unicode_literals

import logging

from travel.avia.shared_flights.tasks.amadeus_parser.flight_arr_times_cacher import FlightArrivalTimesCacher
from travel.avia.shared_flights.tasks.amadeus_parser.flight_segment import FlightSegmentFactory

logger = logging.getLogger(__name__)


class TestFlightArrivalTimesCacher:

    def test_check_range(self):
        arr_times_cacher = FlightArrivalTimesCacher()
        flight_segment_factory = FlightSegmentFactory(
            'operating_carrier^operating_flight_code^stops^arrival_date^arrival_time^board_airport^leg_sequence_number'.split('^'),
        )
        flight_segment = flight_segment_factory.parse_flight_segment('OA^359^0^2021-05-29^1835^JTR^1')
        arr_times_cacher.cache_arrival_time(flight_segment)
        assert 1835 == arr_times_cacher.get_arrival(
            'OA',
            '359',
            '2021-05-29',
            1,
        )

        assert -1 == arr_times_cacher.get_arrival(
            'OA',
            '359',
            '2021-05-30',
            1,
        )
