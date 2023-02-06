# coding=utf-8
from __future__ import unicode_literals

import logging
from datetime import date

from travel.avia.shared_flights.diff_builder.codeshares_map import CodesharesMap
from travel.avia.shared_flights.diff_builder.flights_builder import FlightsBuilder, FlightPatternKey, FlightBaseIdEntry
from travel.avia.shared_flights.lib.python.consts.consts import MIN_SIRENA_FLIGHT_PATTERN_ID
from travel.avia.shared_flights.lib.python.consts.consts import MIN_AMADEUS_RETAINED_FLIGHT_PATTERN_ID
from travel.avia.shared_flights.lib.python.consts.consts import MIN_SIRENA_RETAINED_FLIGHT_BASE_ID
from travel.avia.shared_flights.lib.python.consts.consts import MIN_SIRENA_RETAINED_FLIGHT_PATTERN_ID
from travel.proto.shared_flights.ssim.flights_pb2 import TFlightPattern, EFlightBaseSource

MIN_TEST_FLIGHT_PATTERN_ID = 101

logger = logging.getLogger(__name__)


class TestFlightsBuilder:
    def test_generate_flight_patterns_from_flyouts(self):

        flights_builder = FlightsBuilder(logger, date(2020, 9, 1))

        flyouts = [
            (
                FlightPatternKey(
                    26,
                    '1403',
                    1,
                    EFlightBaseSource.TYPE_AMADEUS,
                    100,
                    'SU',
                    False,
                    0,
                    0,
                    0,
                    False,
                ),
                ['2020-09-01', '2020-09-02', '2020-09-03', '2020-09-04', '2020-09-05', '2020-09-06', '2020-09-09'],
            )
        ]
        flight_bases_data = {}
        flight_bases_data[100] = FlightBaseIdEntry(10, '26.1403.1')
        codeshares_map = CodesharesMap(MIN_TEST_FLIGHT_PATTERN_ID, logger)
        fp1 = TFlightPattern()
        fp1.Id = 1
        fp1.FlightId = 14
        fp1.FlightLegKey = 'SU.1403.1. 01'
        fp1.BucketKey = '26.1403.1'
        fp1.OperatingFromDate = '2020-09-02'
        fp1.OperatingUntilDate = '2020-09-04'
        fp1.OperatingOnDays = 345
        fp1.MarketingCarrier = 26
        fp1.MarketingCarrierIata = 'SU'
        fp1.MarketingFlightNumber = '1403'
        fp1.IsCodeshare = False
        fp1.LegSeqNumber = 1
        codeshares_map.add_flight(fp1)

        expectedFp1 = TFlightPattern()
        expectedFp1.Id = MIN_AMADEUS_RETAINED_FLIGHT_PATTERN_ID + 1
        expectedFp1.FlightId = 10
        expectedFp1.FlightLegKey = '26.1403.1.1'
        expectedFp1.BucketKey = '26.1403.1'
        expectedFp1.OperatingFromDate = '2020-09-01'
        expectedFp1.OperatingUntilDate = '2020-09-07'
        expectedFp1.OperatingOnDays = 267
        expectedFp1.MarketingCarrier = 26
        expectedFp1.MarketingCarrierIata = 'SU'
        expectedFp1.MarketingFlightNumber = '1403'
        expectedFp1.IsCodeshare = False
        expectedFp1.LegSeqNumber = 1

        expectedFp2 = TFlightPattern()
        expectedFp2.Id = MIN_AMADEUS_RETAINED_FLIGHT_PATTERN_ID + 2
        expectedFp2.FlightId = 10
        expectedFp2.FlightLegKey = '26.1403.1.1'
        expectedFp2.BucketKey = '26.1403.1'
        expectedFp2.OperatingFromDate = '2020-09-09'
        expectedFp2.OperatingUntilDate = '2020-09-09'
        expectedFp2.OperatingOnDays = 3
        expectedFp2.MarketingCarrier = 26
        expectedFp2.MarketingCarrierIata = 'SU'
        expectedFp2.MarketingFlightNumber = '1403'
        expectedFp2.IsCodeshare = False
        expectedFp2.LegSeqNumber = 1

        result = list(flights_builder.generate_flight_patterns_from_flyouts(flyouts, flight_bases_data, codeshares_map))
        assert [expectedFp1, expectedFp2] == result

        # try the same thing with a Sirena flight
        flyouts = [
            (
                FlightPatternKey(
                    26,
                    '1403',
                    1,
                    EFlightBaseSource.TYPE_SIRENA,
                    100,
                    'SU',
                    False,
                    0,
                    0,
                    0,
                    False,
                ),
                ['2020-09-01', '2020-09-02', '2020-09-03', '2020-09-04', '2020-09-05', '2020-09-06', '2020-09-09'],
            )
        ]
        flight_bases_data[100] = FlightBaseIdEntry(MIN_SIRENA_RETAINED_FLIGHT_BASE_ID + 1, '26.1403.1')
        fp1.Id = MIN_SIRENA_FLIGHT_PATTERN_ID + 1
        codeshares_map.add_flight(fp1)
        expectedFp1.Id = MIN_SIRENA_RETAINED_FLIGHT_PATTERN_ID + 1
        expectedFp1.FlightId = MIN_SIRENA_RETAINED_FLIGHT_BASE_ID + 1
        expectedFp2.Id = MIN_SIRENA_RETAINED_FLIGHT_PATTERN_ID + 2
        expectedFp2.FlightId = MIN_SIRENA_RETAINED_FLIGHT_BASE_ID + 1

        result = list(flights_builder.generate_flight_patterns_from_flyouts(flyouts, flight_bases_data, codeshares_map))
        assert [expectedFp1, expectedFp2] == result
