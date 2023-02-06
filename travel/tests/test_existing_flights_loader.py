# coding=utf-8
from __future__ import unicode_literals

import logging
from collections import namedtuple
from datetime import date

from travel.avia.shared_flights.data_importer.existing_flights_loader import ExistingFlightsLoader, FlyoutKey
from travel.avia.shared_flights.lib.python.db_models.flight_base import FlightBase
from travel.avia.shared_flights.lib.python.db_models.flight_pattern import FlightPattern
from travel.proto.shared_flights.ssim.flights_pb2 import EFlightBaseSource


logger = logging.getLogger(__name__)
YYYYMMDD = '%Y-%m-%d'

ResultSegment = namedtuple('ResultSegment', ['departure_day', 'marketing_flight_number', 'flight_base_id'])


class TestExistingFlightsLoader:

    def test_get_flights_to_keep_and_renumber(self):
        start_date = date(2020, 5, 14)
        # Dates range to keep: '2020-05-09' - '2020-05-13'
        loader = ExistingFlightsLoader(logger)
        loader.set_date_mask_matcher(start_date, 5)

        # all the way into the past
        fp1 = FlightPattern()
        fp1.id = 1
        fp1.marketing_flight_number = '11'
        fp1.flight_base_id = 11
        fp1.operating_from = date(2020, 4, 14)
        fp1.operating_until = date(2020, 5, 8)
        fp1.operating_on_days = 1234567

        # crosses the start date
        fp2 = FlightPattern()
        fp2.id = 2
        fp2.marketing_flight_number = '22'
        fp2.flight_base_id = 22
        fp2.marketing_carrier = 22
        fp2.leg_seq_number = 1
        fp2.operating_from = date(2020, 4, 20)
        fp2.operating_until = date(2020, 5, 11)
        fp2.operating_on_days = 123457

        # this entry flies all the time
        fp3 = FlightPattern()
        fp3.id = 3
        fp3.flight_base_id = 33
        fp3.marketing_carrier = 33
        fp3.marketing_flight_number = '33'
        fp3.leg_seq_number = 1
        fp3.flight_leg_key = 'fp3'
        fp3.operating_from = date(2005, 4, 21)
        fp3.operating_until = date(2033, 5, 20)
        fp3.operating_on_days = 134567

        # this flight has been only scheduled in the future
        fp4 = FlightPattern()
        fp4.id = 4
        fp4.marketing_flight_number = '44'
        fp4.flight_base_id = 44
        fp4.operating_from = date(2020, 6, 16)
        fp4.operating_until = date(2020, 7, 20)
        fp4.operating_on_days = 123567

        flight_patterns = [fp1, fp2, fp3, fp4]

        fps_to_keep, fbs_ids = loader.get_flights_to_keep_internal(flight_patterns, set(), EFlightBaseSource.TYPE_AMADEUS)
        result = [
            ResultSegment(
                fp.flight_departure_day.strftime(YYYYMMDD),
                fp.marketing_flight_number,
                fp.flight_base_id
            ) for fp in fps_to_keep
        ]
        expected = [
            ResultSegment('2020-05-10', '22', 22),
            ResultSegment('2020-05-11', '22', 22),
            ResultSegment('2020-05-09', '33', 33),
            ResultSegment('2020-05-10', '33', 33),
            ResultSegment('2020-05-11', '33', 33),
            ResultSegment('2020-05-13', '33', 33),
        ]

        assert expected == result
        result_fbs = list(fbs_ids)
        result_fbs.sort()
        assert result_fbs == [22, 33]

        fb22 = FlightBase()
        fb22.id = 22
        fb22.itinerary_variation = 'fb22'
        fb22.leg_seq_number = 1
        fb22.bucket_key = 'fb22'
        fb22.operating_carrier = 22
        fb22.operating_flight_number = '22'
        fb22.departure_station = 22
        fb22.arrival_station = 222
        fb22.scheduled_departure_time = 0
        fb22.scheduled_arrival_time = 0

        fb33 = FlightBase()
        fb33.id = 33
        fb33.itinerary_variation = 'fb33'
        fb33.leg_seq_number = 1
        fb33.bucket_key = 'fb33'
        fb33.operating_carrier = 33
        fb33.operating_flight_number = '33'
        fb33.departure_station = 33
        fb33.arrival_station = 333
        fb33.scheduled_departure_time = 0
        fb33.scheduled_arrival_time = 0

        fps, fbs = loader.renumber(fps_to_keep, [fb22, fb33], 100, EFlightBaseSource.TYPE_AMADEUS)

        fps_result = [
            ResultSegment(
                fp.flight_departure_day.strftime(YYYYMMDD),
                fp.marketing_flight_number,
                fp.flight_base_id
            ) for fp in fps
        ]
        # Same as above except flight_base_ids: 22 -> 101, 33 -> 102
        expected = [
            ResultSegment('2020-05-10', '22', 101),
            ResultSegment('2020-05-11', '22', 101),
            ResultSegment('2020-05-09', '33', 102),
            ResultSegment('2020-05-10', '33', 102),
            ResultSegment('2020-05-11', '33', 102),
            ResultSegment('2020-05-13', '33', 102),
        ]
        assert expected == fps_result

        assert [22, 33] == list(fbs.keys())
        assert fbs[22].id == 101
        assert fbs[22].leg_seq_number == 1
        assert fbs[22].operating_carrier == 22
        assert fbs[22].operating_flight_number == '22'
        assert fbs[22].departure_station == 22
        assert fbs[22].arrival_station == 222
        assert fbs[22].scheduled_departure_time == 0
        assert fbs[22].scheduled_arrival_time == 0
        assert fbs[22].source == EFlightBaseSource.TYPE_AMADEUS
        assert fbs[22].created_at == start_date

        assert fbs[33].id == 102
        assert fbs[33].leg_seq_number == 1
        assert fbs[33].operating_carrier == 33
        assert fbs[33].operating_flight_number == '33'
        assert fbs[33].departure_station == 33
        assert fbs[33].arrival_station == 333
        assert fbs[33].scheduled_departure_time == 0
        assert fbs[33].scheduled_arrival_time == 0
        assert fbs[33].source == EFlightBaseSource.TYPE_AMADEUS
        assert fbs[33].created_at == start_date

        # Test that we do not keep segments which are stored already
        existing_segments = set()
        existing_segments.add(FlyoutKey('2020-05-10', 22, '22', 1))
        existing_segments.add(FlyoutKey('2020-05-11', 22, '22', 1))
        existing_segments.add(FlyoutKey('2020-05-09', 33, '33', 1))
        existing_segments.add(FlyoutKey('2020-05-11', 33, '33', 1))
        fps_to_keep, fbs_ids = loader.get_flights_to_keep_internal(flight_patterns, existing_segments, EFlightBaseSource.TYPE_AMADEUS)
        result = [
            ResultSegment(
                fp.flight_departure_day.strftime(YYYYMMDD),
                fp.marketing_flight_number,
                fp.flight_base_id
            ) for fp in fps_to_keep
        ]
        expected = [
            ResultSegment('2020-05-10', '33', 33),
            ResultSegment('2020-05-13', '33', 33),
        ]
        assert expected == result
        result_fbs = list(fbs_ids)
        assert result_fbs == [33]
