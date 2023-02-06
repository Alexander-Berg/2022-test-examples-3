# coding=utf-8
from __future__ import unicode_literals

import logging
from datetime import date

from travel.avia.shared_flights.diff_builder.p2p_cache import FlightKey, P2PCache, RouteKey
from travel.proto.shared_flights.ssim.flights_pb2 import TFlightPattern


logger = logging.getLogger(__name__)


class TestP2PCache:
    def test_print_cache(self):
        p2p_cache = P2PCache(logger, date(2020, 10, 1))

        fp1 = TFlightPattern()
        fp1.Id = 1
        fp1.FlightId = 14
        fp1.FlightLegKey = 'SU.1403.1. 01'
        fp1.BucketKey = '26.1403.1'
        fp1.OperatingFromDate = '2020-10-06'
        fp1.OperatingUntilDate = '2020-10-08'
        fp1.OperatingOnDays = 345
        fp1.MarketingCarrier = 26
        fp1.MarketingCarrierIata = 'SU'
        fp1.MarketingFlightNumber = '1403'
        fp1.IsCodeshare = False
        fp1.LegSeqNumber = 1

        p2p_cache.add_segment(fp1, 1, 2)

        expected = '\n'.join(
            [
                'Flight: FlightKey(carrier=26, flight_number=\'1403\')',
                '  Leg: 1',
                '    Route: RouteKey(station_from=1, station_to=2)',
                '      Arr shift: 0',
                '      Dep shift: 0',
                '      Is codeshare: False',
                '      Date: 20201007',
                '      Date: 20201008',
            ]
        )
        result = p2p_cache.print_cache()
        assert expected == result

        assert [
            (RouteKey(station_from=1, station_to=2), [FlightKey(carrier=26, flight_number='1403')]),
        ] == list(p2p_cache.generate_p2p_cache())

        # add another flight
        fp2 = TFlightPattern()
        fp2.Id = 2
        fp2.OperatingFromDate = '2020-10-06'
        fp2.OperatingUntilDate = '2020-10-08'
        fp2.OperatingOnDays = 345
        fp2.MarketingCarrier = 26
        fp2.MarketingFlightNumber = '1404'
        fp2.LegSeqNumber = 1

        p2p_cache.add_segment(fp2, 3, 4)

        assert [
            (RouteKey(station_from=1, station_to=2), [FlightKey(carrier=26, flight_number='1403')]),
            (RouteKey(station_from=3, station_to=4), [FlightKey(carrier=26, flight_number='1404')]),
        ] == list(p2p_cache.generate_p2p_cache())

        # add another flight to the already-existing route
        fp3 = TFlightPattern()
        fp3.Id = 3
        fp3.OperatingFromDate = '2020-10-06'
        fp3.OperatingUntilDate = '2020-10-08'
        fp3.OperatingOnDays = 345
        fp3.MarketingCarrier = 26
        fp3.MarketingFlightNumber = '1405'
        fp3.LegSeqNumber = 1

        p2p_cache.add_segment(fp3, 1, 2)
        assert [
            (
                RouteKey(station_from=1, station_to=2),
                [
                    FlightKey(carrier=26, flight_number='1403'),
                    FlightKey(carrier=26, flight_number='1405'),
                ],
            ),
            (RouteKey(station_from=3, station_to=4), [FlightKey(carrier=26, flight_number='1404')]),
        ] == list(p2p_cache.generate_p2p_cache())

    def test_multisegment(self):
        p2p_cache = P2PCache(logger, date(2020, 10, 1))

        fp1 = TFlightPattern()
        fp1.Id = 1
        fp1.OperatingFromDate = '2020-10-06'
        fp1.OperatingUntilDate = '2020-10-08'
        fp1.OperatingOnDays = 345
        fp1.MarketingCarrier = 26
        fp1.MarketingFlightNumber = '1404'
        fp1.LegSeqNumber = 1

        fp2a = TFlightPattern()
        fp2a.Id = 2
        fp2a.OperatingFromDate = '2020-10-06'
        fp2a.OperatingUntilDate = '2020-10-08'
        fp2a.OperatingOnDays = 345
        fp2a.MarketingCarrier = 26
        fp2a.MarketingFlightNumber = '1404'
        fp2a.LegSeqNumber = 2

        fp2b = TFlightPattern()
        fp2b.Id = 2
        fp2b.OperatingFromDate = '2020-10-06'
        fp2b.OperatingUntilDate = '2020-10-08'
        fp2b.OperatingOnDays = 345
        fp2b.MarketingCarrier = 26
        fp2b.MarketingFlightNumber = '1404'
        fp2b.LegSeqNumber = 2

        p2p_cache.add_segment(fp2a, 2, 3)
        p2p_cache.add_segment(fp1, 1, 2)

        assert [
            (RouteKey(station_from=1, station_to=2), [FlightKey(carrier=26, flight_number='1404')]),
            (RouteKey(station_from=1, station_to=3), [FlightKey(carrier=26, flight_number='1404')]),
            (RouteKey(station_from=2, station_to=3), [FlightKey(carrier=26, flight_number='1404')]),
        ] == list(p2p_cache.generate_p2p_cache())

        p2p_cache.add_segment(fp2b, 2, 4)

        assert [
            (RouteKey(station_from=1, station_to=2), [FlightKey(carrier=26, flight_number='1404')]),
            (RouteKey(station_from=1, station_to=3), [FlightKey(carrier=26, flight_number='1404')]),
            (RouteKey(station_from=1, station_to=4), [FlightKey(carrier=26, flight_number='1404')]),
            (RouteKey(station_from=2, station_to=3), [FlightKey(carrier=26, flight_number='1404')]),
            (RouteKey(station_from=2, station_to=4), [FlightKey(carrier=26, flight_number='1404')]),
        ] == sorted(list(p2p_cache.generate_p2p_cache()))

    def test_shift_dates(self):
        p2p_cache = P2PCache(logger, date(2020, 10, 1))

        fp1 = TFlightPattern()
        fp1.Id = 1
        fp1.OperatingFromDate = '2020-10-07'
        fp1.OperatingUntilDate = '2020-10-07'
        fp1.OperatingOnDays = 3
        fp1.MarketingCarrier = 26
        fp1.MarketingFlightNumber = '1404'
        fp1.LegSeqNumber = 1
        fp1.ArrivalDayShift = 1

        fp2 = TFlightPattern()
        fp2.Id = 2
        fp2.OperatingFromDate = '2020-10-08'
        fp2.OperatingUntilDate = '2020-10-08'
        fp2.OperatingOnDays = 4
        fp2.MarketingCarrier = 26
        fp2.MarketingFlightNumber = '1404'
        fp2.LegSeqNumber = 2

        p2p_cache.add_segment(fp1, 1, 2)
        p2p_cache.add_segment(fp2, 2, 3)

        assert [
            (RouteKey(station_from=1, station_to=2), [FlightKey(carrier=26, flight_number='1404')]),
            (RouteKey(station_from=1, station_to=3), [FlightKey(carrier=26, flight_number='1404')]),
            (RouteKey(station_from=2, station_to=3), [FlightKey(carrier=26, flight_number='1404')]),
        ] == list(p2p_cache.generate_p2p_cache())

        fp3 = TFlightPattern()
        fp3.Id = 2
        fp3.OperatingFromDate = '2020-10-09'
        fp3.OperatingUntilDate = '2020-10-09'
        fp3.OperatingOnDays = 5
        fp3.MarketingCarrier = 26
        fp3.MarketingFlightNumber = '1404'
        fp3.LegSeqNumber = 3
        fp3.DepartureDayShift = 1

        expected = [
            (RouteKey(station_from=1, station_to=2), [FlightKey(carrier=26, flight_number='1404')]),
            (RouteKey(station_from=1, station_to=3), [FlightKey(carrier=26, flight_number='1404')]),
            (RouteKey(station_from=1, station_to=4), [FlightKey(carrier=26, flight_number='1404')]),
            (RouteKey(station_from=2, station_to=3), [FlightKey(carrier=26, flight_number='1404')]),
            (RouteKey(station_from=2, station_to=4), [FlightKey(carrier=26, flight_number='1404')]),
            (RouteKey(station_from=3, station_to=4), [FlightKey(carrier=26, flight_number='1404')]),
        ]
        p2p_cache.add_segment(fp3, 3, 4)
        assert expected == sorted(p2p_cache.generate_p2p_cache())

    def test_negative_cases(self):
        p2p_cache = P2PCache(logger, date(2020, 10, 1))

        fp1 = TFlightPattern()
        fp1.Id = 1
        fp1.OperatingFromDate = '2020-10-07'
        fp1.OperatingUntilDate = '2020-10-07'
        fp1.OperatingOnDays = 3
        fp1.MarketingCarrier = 26
        fp1.MarketingFlightNumber = '1404'
        fp1.LegSeqNumber = 1
        fp1.ArrivalDayShift = 1

        fp2 = TFlightPattern()
        fp2.Id = 2
        fp2.OperatingFromDate = '2020-10-08'
        fp2.OperatingUntilDate = '2020-10-08'
        fp2.OperatingOnDays = 4
        fp2.MarketingCarrier = 26
        fp2.MarketingFlightNumber = '1404'
        fp2.LegSeqNumber = 2

        p2p_cache.add_segment(fp1, 1, 2)
        p2p_cache.add_segment(fp2, 2, 3)

        # only contains two legs
        expected = [
            (RouteKey(station_from=1, station_to=2), [FlightKey(carrier=26, flight_number='1404')]),
            (RouteKey(station_from=1, station_to=3), [FlightKey(carrier=26, flight_number='1404')]),
            (RouteKey(station_from=2, station_to=3), [FlightKey(carrier=26, flight_number='1404')]),
        ]

        # skips the leg number - should not be added to the route
        fp3 = TFlightPattern()
        fp3.Id = 3
        fp3.OperatingFromDate = '2020-10-08'
        fp3.OperatingUntilDate = '2020-10-08'
        fp3.OperatingOnDays = 4
        fp3.MarketingCarrier = 26
        fp3.MarketingFlightNumber = '1404'
        fp3.LegSeqNumber = 4

        p2p_cache.add_segment(fp3, 3, 4)

        assert expected == list(p2p_cache.generate_p2p_cache())

        # departs from wrong station
        fp4 = TFlightPattern()
        fp4.Id = 3
        fp4.OperatingFromDate = '2020-10-08'
        fp4.OperatingUntilDate = '2020-10-08'
        fp4.OperatingOnDays = 4
        fp4.MarketingCarrier = 26
        fp4.MarketingFlightNumber = '1404'
        fp4.LegSeqNumber = 3

        p2p_cache.add_segment(fp4, 2, 4)

        assert expected == list(p2p_cache.generate_p2p_cache())

        # departs on wrong date
        fp5 = TFlightPattern()
        fp5.Id = 3
        fp5.OperatingFromDate = '2020-10-09'
        fp5.OperatingUntilDate = '2020-10-09'
        fp5.OperatingOnDays = 5
        fp5.MarketingCarrier = 26
        fp5.MarketingFlightNumber = '1404'
        fp5.LegSeqNumber = 3

        p2p_cache.add_segment(fp5, 3, 4)

        assert expected == list(p2p_cache.generate_p2p_cache())
