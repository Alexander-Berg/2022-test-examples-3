# coding=utf-8
from __future__ import print_function
import logging
import mock

import travel.avia.shared_flights.tasks.ssim_parser.flights as flights
import travel.library.python.dicts.file_util as file_util
from travel.proto.shared_flights.ssim.flights_pb2 import TFlightBase

logger = logging.getLogger(__name__)


def write_binary_string(_, binary_string):
    TestFlightsParser.current_binary_string = binary_string


class TestFlightsParser:

    current_binary_string = None

    @staticmethod
    def setup_method(self):
        TestFlightsParser.current_binary_string = None

    @mock.patch.object(file_util, 'write_binary_string', side_effect=write_binary_string)
    def test_parse_operating_flight(self, _):
        """ Test that SSIM Record2 + Record3 produce an operating flight
            plus a flight pattern that references that flight.
        """
        flights_importer = flights.FlightsImporter(flight_bases_file=None, logger=logger)
        flights_importer.process(
            '2LSU      H   01OCT1900XXX0001OCT19INNOVATA SSIM                01OCT19CCREATED BY SABRE AIRFLITE')
        flights_importer.process(
            '3 SU 14140101J20AUG1926OCT191234567 SVO17301730+0300B SVX22002200+0500  73HXX                       XX' +
            '                 DD                                                                       00206970')
        flights_importer.flush()
        flights_importer.post_process()
        result = list(flights_importer.flight_patterns())
        assert len(result) == 1
        flight_pattern = result[0]
        assert flight_pattern.FlightLegKey == 'SU.1414.1. 01'
        assert flight_pattern.OperatingFromDate == '2019-08-20'
        assert flight_pattern.OperatingUntilDate == '2019-10-26'
        assert flight_pattern.OperatingOnDays == 1234567
        assert flight_pattern.MarketingCarrierIata == 'SU'
        assert flight_pattern.MarketingFlightNumber == '1414'
        assert flight_pattern.IsCodeshare is False

        assert TestFlightsParser.current_binary_string is not None

        flight_base = TFlightBase()
        flight_base.ParseFromString(TestFlightsParser.current_binary_string)

        assert flight_base.OperatingCarrierIata == 'SU'
        assert flight_base.OperatingFlightNumber == '1414'
        assert flight_base.ItineraryVariationIdentifier == '1'
        assert flight_base.LegSeqNumber == 1
        assert flight_base.DepartureStationIata == 'SVO'
        assert flight_base.ScheduledDepartureTime == 1730
        assert flight_base.DepartureTerminal == 'B'
        assert flight_base.ArrivalStationIata == 'SVX'
        assert flight_base.ScheduledArrivalTime == 2200
        assert flight_base.ArrivalTerminal == ''
        assert flight_base.AircraftModel == '73H'
        assert flight_base.IntlDomesticStatus == 'DD'
        assert flight_base.Season == 'H '

    @mock.patch.object(file_util, 'write_binary_string', side_effect=write_binary_string)
    def test_parse_marketing_flight(self, _):
        """ Test that SSIM Record3 + Record4 (with modifier 050) produce a code-shared marketing flight
            that refers the same flight base, as the corresponding operating flight.
        """
        flights_importer = flights.FlightsImporter(flight_bases_file=None, logger=logger)
        flights_importer.process(
            '3 SU 14140101J20AUG1926OCT191234567 SVO17301730+0300B SVX22002200+0500  73HXX                       XX' +
            '                 DD                                                                       00206970')
        flights_importer.process(
            '3 KL 28710101J20AUG1926OCT191234567 SVO17301730+0300B SVX22002200+0500  73HXX                       XX' +
            '                 DD       SU                  LG                      C999M999            00788133')
        flights_importer.process(
            '4 KL 28710101J              AB050SVOSVXSU 1414')
        flights_importer.post_process()
        result = flights_importer.flight_patterns_by_leg_key()
        assert len(flights_importer.postponed_legs()) == 0
        assert len(result) == 2
        flight_pattern = result['KL.2871.1. 01']
        assert flight_pattern.FlightLegKey == 'KL.2871.1. 01'
        assert flight_pattern.OperatingFromDate == '2019-08-20'
        assert flight_pattern.OperatingUntilDate == '2019-10-26'
        assert flight_pattern.OperatingOnDays == 1234567
        assert flight_pattern.MarketingCarrierIata == 'KL'
        assert flight_pattern.MarketingFlightNumber == '2871'
        assert flight_pattern.IsCodeshare is True

        operating_flight_pattern = result['SU.1414.1. 01']
        assert flight_pattern.FlightId == operating_flight_pattern.FlightId

    @mock.patch.object(file_util, 'write_binary_string', side_effect=write_binary_string)
    def test_parse_codeshares_transitively(self, _):
        """
        Test that if flight A is a codeshare of flight B and B is a codeshare of flight C,
        then we properly show that both A and B are codeshares of C
        """
        flights_importer = flights.FlightsImporter(flight_bases_file=None, logger=logger)
        flights_importer.process(
            '3 SU 14140101J20AUG1926OCT191234567 SVO17301730+0300B SVX22002200+0500  73HXX                       XX' +
            '                 DD                                                                       00206970')
        flights_importer.process(
            '3 KL 28710101J20AUG1926OCT191234567 SVO17301730+0300B SVX22002200+0500  73HXX                       XX' +
            '                 DD       SU                  LG                      C999M999            00788133')
        flights_importer.process(
            '3 AF 49460101J20AUG1926OCT191234567 SVO17301730+0300B SVX22002200+0500  73HXX                       XX' +
            '                 DD       SU SU SU            LG                      C999Y999            00373996')
        flights_importer.process(
            '4 KL 28710101J              AB050SVOSVXAF 4946')
        flights_importer.process(
            '4 AF 49460101J              AB050SVOSVXSU 1414')
        flights_importer.post_process()
        result = flights_importer.flight_patterns_by_leg_key()
        assert len(result) == 3

        operating_flight_pattern = result['SU.1414.1. 01']
        assert operating_flight_pattern.BucketKey == 'SU.1414.1'

        carriers = set()
        for fp in result.values():
            carriers.add(fp.MarketingCarrierIata)
            if fp.MarketingCarrierIata == 'SU':
                continue
            if fp.MarketingCarrierIata in ['KL', 'AF']:
                assert fp.IsCodeshare is True
                assert fp.BucketKey == operating_flight_pattern.BucketKey

        assert 'SU' in carriers
        assert 'KL' in carriers
        assert 'AF' in carriers

    @mock.patch.object(file_util, 'write_binary_string', side_effect=write_binary_string)
    def test_parse_codeshares_from_rec4_010(self, _):
        """
        Test that if flight A is a operating and B is a codeshare of A, filed only through rec4 / 010,
        then we properly show that flight B exists and that it is a codeshare of A
        """
        flights_importer = flights.FlightsImporter(flight_bases_file=None, logger=logger)
        flights_importer.process(
            '3 SU 14140101J20AUG1926OCT191234567 SVO17301730+0300B SVX22002200+0500  73HXX                       XX' +
            '                 DD                                                                       00206970')
        flights_importer.process(
            '4 SU 14140101J              AB010SVOSVXAF 4946 /KL 2871')
        flights_importer.post_process()
        result = flights_importer.flight_patterns_by_leg_key()
        assert len(result) == 3

        operating_flight_pattern = result['SU.1414.1. 01']
        assert operating_flight_pattern.BucketKey == 'SU.1414.1'

        carriers = set()
        for fp in result.values():
            carriers.add(fp.MarketingCarrierIata)
            if fp.MarketingCarrierIata == 'SU':
                continue
            if fp.MarketingCarrierIata in ['KL', 'AF']:
                assert fp.IsCodeshare is True
                assert fp.BucketKey == operating_flight_pattern.BucketKey

        assert 'SU' in carriers
        assert 'KL' in carriers
        assert 'AF' in carriers
