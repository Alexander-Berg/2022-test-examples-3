# coding=utf-8
from __future__ import unicode_literals

import logging

from travel.avia.shared_flights.diff_builder.codeshares_map import CodesharesMap
from travel.avia.shared_flights.lib.python.consts.consts import MIN_APM_FLIGHT_PATTERN_ID, MIN_SIRENA_FLIGHT_PATTERN_ID
from travel.proto.shared_flights.ssim.flights_pb2 import TFlightPattern

MIN_TEST_FLIGHT_PATTERN_ID = 101

logger = logging.getLogger(__name__)


class TestCodesharesMapParser:
    def test_add_flight(self):
        fp1 = TFlightPattern()
        fp1.Id = 50
        fp1.FlightLegKey = 'SU.1414.1. 01'
        fp1.BucketKey = '26.1414.1'
        fp1.OperatingFromDate = '2019-08-20'
        fp1.OperatingUntilDate = '2019-10-26'
        fp1.OperatingOnDays = 1234567
        fp1.MarketingCarrier = 26
        fp1.MarketingCarrierIata = 'SU'
        fp1.MarketingFlightNumber = '1414'
        fp1.IsCodeshare = False
        fp1.LegSeqNumber = 1

        codeshares_map = CodesharesMap(MIN_TEST_FLIGHT_PATTERN_ID, logger)

        codeshares_map.add_flight(fp1)
        result = list(codeshares_map.generate_flights_to_write())
        assert result == [fp1]

        fp2 = TFlightPattern()
        fp2.Id = 51
        fp2.FlightLegKey = 'SU.1414.1. 01'
        fp2.BucketKey = '26.1414.1'
        fp2.OperatingFromDate = '2019-11-20'
        fp2.OperatingUntilDate = '2019-11-26'
        fp2.OperatingOnDays = 1234567
        fp2.MarketingCarrier = 26
        fp2.MarketingCarrierIata = 'SU'
        fp2.MarketingFlightNumber = '1414'
        fp2.IsCodeshare = False
        fp2.LegSeqNumber = 1

        codeshares_map.add_flight(fp2)
        result = list(codeshares_map.generate_flights_to_write())
        assert result == [fp1, fp2]

        fp3 = TFlightPattern()
        fp3.MergeFrom(fp1)
        fp3.Id = MIN_SIRENA_FLIGHT_PATTERN_ID + 1
        codeshares_map.add_flight(fp3)
        result = list(codeshares_map.generate_flights_to_write())
        assert result == [fp3]

        fp4 = TFlightPattern()
        fp4.MergeFrom(fp2)
        fp4.Id = MIN_SIRENA_FLIGHT_PATTERN_ID + 2
        codeshares_map.add_flight(fp4)
        result = list(codeshares_map.generate_flights_to_write())
        assert result == [fp3, fp4]

        # adding a codeshare flight for a Sirena flight
        fp5 = TFlightPattern()
        fp5.MergeFrom(fp1)
        fp5.Id = 53
        fp5.MarketingCarrier = 7
        fp5.MarketingCarrierIata = 'B2'
        fp5.MarketingFlightNumber = '3434'
        fp5.IsCodeshare = True
        codeshares_map.add_flight(fp5)
        result = list(codeshares_map.generate_flights_to_write())
        fp5.Id = 101
        fp5.OperatingFlightPatternId = fp3.Id
        assert result == [fp5, fp3, fp4]

    def test_dates_intersect(self):
        fp1 = TFlightPattern()
        fp1.Id = 50
        fp1.FlightLegKey = 'SU.1414.1. 01'
        fp1.BucketKey = '26.1414.1'
        fp1.OperatingFromDate = '2019-08-20'
        fp1.OperatingUntilDate = '2019-10-26'
        fp1.OperatingOnDays = 1234567
        fp1.MarketingCarrier = 26
        fp1.MarketingCarrierIata = 'SU'
        fp1.MarketingFlightNumber = '1414'
        fp1.IsCodeshare = False
        fp1.LegSeqNumber = 1

        codeshares_map = CodesharesMap(MIN_TEST_FLIGHT_PATTERN_ID, logger)

        variation_early = TFlightPattern()
        variation_early.OperatingFromDate = '2019-08-01'
        variation_early.OperatingUntilDate = '2019-08-19'
        variation_early.OperatingOnDays = 1234567
        assert list(codeshares_map.generate_overlaps(fp1, [variation_early])) == []

        variation_late = TFlightPattern()
        variation_late.OperatingFromDate = '2019-10-28'
        variation_late.OperatingUntilDate = '2019-11-01'
        variation_late.OperatingOnDays = 1234567
        assert list(codeshares_map.generate_overlaps(fp1, [variation_late])) == []

        variation_left = TFlightPattern()
        variation_left.OperatingFromDate = '2019-08-01'
        variation_left.OperatingUntilDate = '2019-09-01'
        variation_left.OperatingOnDays = 1234567
        variation_left.BucketKey = 'B2.3434.1'
        expected = TFlightPattern()
        expected.MergeFrom(fp1)
        expected.Id = MIN_TEST_FLIGHT_PATTERN_ID
        expected.OperatingUntilDate = variation_left.OperatingUntilDate
        expected.BucketKey = variation_left.BucketKey
        expected.IsCodeshare = True
        assert list(codeshares_map.generate_overlaps(fp1, [variation_left])) == [expected]

        variation_right = TFlightPattern()
        variation_right.OperatingFromDate = '2019-09-28'
        variation_right.OperatingUntilDate = '2019-10-26'
        variation_right.OperatingOnDays = 1234567
        variation_right.BucketKey = 'B2.3434.1'
        expected = TFlightPattern()
        expected.MergeFrom(fp1)
        expected.Id = MIN_TEST_FLIGHT_PATTERN_ID + 1
        expected.OperatingFromDate = variation_right.OperatingFromDate
        expected.BucketKey = variation_right.BucketKey
        expected.IsCodeshare = True
        assert list(codeshares_map.generate_overlaps(fp1, [variation_right])) == [expected]

        variation_middle = TFlightPattern()
        variation_middle.OperatingFromDate = '2019-10-01'
        variation_middle.OperatingUntilDate = '2019-10-05'
        variation_middle.OperatingOnDays = 1234567
        variation_middle.BucketKey = 'B2.3434.1'
        expected = TFlightPattern()
        expected.MergeFrom(fp1)
        expected.Id = MIN_TEST_FLIGHT_PATTERN_ID + 2
        expected.OperatingFromDate = variation_middle.OperatingFromDate
        expected.OperatingUntilDate = variation_middle.OperatingUntilDate
        expected.BucketKey = variation_middle.BucketKey
        expected.IsCodeshare = True
        assert list(codeshares_map.generate_overlaps(fp1, [variation_middle])) == [expected]

        variation_around = TFlightPattern()
        variation_around.OperatingFromDate = '2018-10-01'
        variation_around.OperatingUntilDate = '2020-10-05'
        variation_around.OperatingOnDays = 1234567
        variation_around.BucketKey = 'B2.3434.1'
        expected = TFlightPattern()
        expected.MergeFrom(fp1)
        expected.Id = MIN_TEST_FLIGHT_PATTERN_ID + 3
        expected.BucketKey = variation_around.BucketKey
        expected.IsCodeshare = True
        assert list(codeshares_map.generate_overlaps(fp1, [variation_around])) == [expected]

    def test_generate_flights_by_dates(self):
        fp = TFlightPattern()
        fp.Id = 50
        fp.FlightLegKey = 'SU.1414.1. 01'
        fp.BucketKey = '26.1414.1'
        fp.OperatingFromDate = '2019-08-20'
        fp.OperatingUntilDate = '2019-10-26'
        fp.OperatingOnDays = 1234567
        fp.MarketingCarrier = 26
        fp.MarketingCarrierIata = 'SU'
        fp.MarketingFlightNumber = '1414'
        fp.IsCodeshare = False
        fp.LegSeqNumber = 1

        codeshares_map = CodesharesMap(MIN_TEST_FLIGHT_PATTERN_ID, logger)

        variation_early = TFlightPattern()
        variation_early.OperatingFromDate = '2019-08-01'
        variation_early.OperatingUntilDate = '2019-08-19'
        variation_early.OperatingOnDays = 1234567
        variation_early.FlightId = 11
        variation_early.BucketKey = '26.1414.1'

        variation_middle = TFlightPattern()
        variation_middle.OperatingFromDate = '2019-10-01'
        variation_middle.OperatingUntilDate = '2019-10-05'
        variation_middle.OperatingOnDays = 123457  # No Saturday, so the last common day with fp is 2019-10-04
        variation_middle.FlightId = 12
        variation_middle.BucketKey = '26.1414.1'

        variations = [variation_early, variation_middle]

        expected = TFlightPattern()
        expected.Id = MIN_TEST_FLIGHT_PATTERN_ID
        expected.FlightLegKey = 'SU.1414.1. 01'
        expected.FlightId = 12
        expected.BucketKey = '26.1414.1'
        expected.OperatingFromDate = '2019-10-01'
        expected.OperatingUntilDate = '2019-10-04'
        expected.OperatingOnDays = 123457
        expected.MarketingCarrier = 26
        expected.MarketingCarrierIata = 'SU'
        expected.MarketingFlightNumber = '1414'
        expected.IsCodeshare = True
        expected.LegSeqNumber = 1

        assert list(codeshares_map.generate_overlaps(fp, variations)) == [expected]

    def test_generate_flights_to_write_basic(self):
        fp1 = TFlightPattern()
        fp1.Id = 50
        fp1.FlightLegKey = 'SU.1414.1. 01'
        fp1.BucketKey = '26.1414.1'
        fp1.OperatingFromDate = '2019-08-20'
        fp1.OperatingUntilDate = '2019-10-26'
        fp1.OperatingOnDays = 1234567
        fp1.MarketingCarrier = 26
        fp1.MarketingCarrierIata = 'SU'
        fp1.MarketingFlightNumber = '1414'
        fp1.IsCodeshare = False
        fp1.LegSeqNumber = 1

        codeshares_map = CodesharesMap(MIN_TEST_FLIGHT_PATTERN_ID, logger)
        codeshares_map.add_flight(fp1)
        # for operating flight the result is just the flight itself
        assert list(codeshares_map.generate_flights_to_write()) == [fp1]

        fp2 = TFlightPattern()
        fp2.MergeFrom(fp1)
        fp2.Id = MIN_SIRENA_FLIGHT_PATTERN_ID + 1
        codeshares_map.add_flight(fp2)
        # for operating sirena flight the result is just the flight itself as well
        assert list(codeshares_map.generate_flights_to_write()) == [fp2]

    def test_generate_flights_to_write_apm(self):
        codeshares_map = CodesharesMap(MIN_TEST_FLIGHT_PATTERN_ID, logger)

        fp1 = TFlightPattern()
        fp1.Id = MIN_SIRENA_FLIGHT_PATTERN_ID + 1
        fp1.FlightLegKey = 'SU.1414.1. 01'
        fp1.BucketKey = '26.1414.1'
        fp1.OperatingFromDate = '2019-08-20'
        fp1.OperatingUntilDate = '2019-10-26'
        fp1.OperatingOnDays = 1234567
        fp1.MarketingCarrier = 26
        fp1.MarketingCarrierIata = 'SU'
        fp1.MarketingFlightNumber = '1414'
        fp1.IsCodeshare = False
        fp1.LegSeqNumber = 1
        codeshares_map.add_flight(fp1)

        fp2 = TFlightPattern()
        fp2.MergeFrom(fp1)
        fp2.Id = MIN_APM_FLIGHT_PATTERN_ID + 1
        codeshares_map.add_flight(fp2)

        fp3 = TFlightPattern()
        fp3.MergeFrom(fp1)
        fp3.Id = MIN_TEST_FLIGHT_PATTERN_ID + 1
        codeshares_map.add_flight(fp3)

        # APM flights have higher priority than Sirena/Amadeus flights
        assert list(codeshares_map.generate_flights_to_write()) == [fp2]

    def test_amadeus_codeshare_flight(self):
        fp1 = TFlightPattern()
        fp1.Id = 50
        fp1.FlightLegKey = 'SU.1414.1. 01'
        fp1.BucketKey = '26.1414.1'
        fp1.FlightId = 11
        fp1.OperatingFromDate = '2019-08-20'
        fp1.OperatingUntilDate = '2019-10-26'
        fp1.OperatingOnDays = 1234567
        fp1.MarketingCarrier = 26
        fp1.MarketingCarrierIata = 'SU'
        fp1.MarketingFlightNumber = '1414'
        fp1.IsCodeshare = False
        fp1.LegSeqNumber = 1

        fp2 = TFlightPattern()
        fp2.Id = 60
        fp2.FlightLegKey = 'AZ.2828.1. 05'
        fp2.FlightId = 15
        fp2.BucketKey = '26.1414.1'
        fp2.OperatingFromDate = '2019-08-20'
        fp2.OperatingUntilDate = '2019-10-26'
        fp2.OperatingOnDays = 1234567
        fp2.MarketingCarrier = 58
        fp2.MarketingCarrierIata = 'AZ'
        fp2.MarketingFlightNumber = '2828'
        fp2.IsCodeshare = True
        fp2.LegSeqNumber = 1

        codeshares_map = CodesharesMap(MIN_TEST_FLIGHT_PATTERN_ID, logger)
        codeshares_map.add_flight(fp1)
        codeshares_map.add_flight(fp2)

        # 'r' is for 'result'
        fp2r = TFlightPattern()
        fp2r.MergeFrom(fp2)
        fp2r.Id = 101
        fp2r.FlightId = 11
        fp2r.OperatingFlightPatternId = 50

        assert list(codeshares_map.generate_flights_to_write()) == [fp1, fp2r]

    def test_sirena_codeshare_flight_referencing_amadeus(self):
        fp1 = TFlightPattern()
        fp1.Id = 50
        fp1.FlightLegKey = 'SU.1414.1. 01'
        fp1.BucketKey = '26.1414.1'
        fp1.FlightId = 11
        fp1.OperatingFromDate = '2019-08-20'
        fp1.OperatingUntilDate = '2019-10-26'
        fp1.OperatingOnDays = 1234567
        fp1.MarketingCarrier = 26
        fp1.MarketingCarrierIata = 'SU'
        fp1.MarketingFlightNumber = '1414'
        fp1.IsCodeshare = False
        fp1.LegSeqNumber = 1

        fp2 = TFlightPattern()
        fp2.Id = 60
        fp2.FlightLegKey = 'AZ.2828.1. 14'
        fp2.BucketKey = '26.1414.1'
        fp2.FlightId = 19
        fp2.OperatingFromDate = '2019-08-20'
        fp2.OperatingUntilDate = '2019-10-26'
        fp2.OperatingOnDays = 1234567
        fp2.MarketingCarrier = 58
        fp2.MarketingCarrierIata = 'AZ'
        fp2.MarketingFlightNumber = '2828'
        fp2.IsCodeshare = True
        fp2.LegSeqNumber = 1

        fp3 = TFlightPattern()
        fp3.Id = MIN_SIRENA_FLIGHT_PATTERN_ID + 1
        fp3.FlightLegKey = 'AZ.2828.1. 05'
        fp3.FlightId = 11
        fp3.BucketKey = '26.1414.1'
        fp3.OperatingFromDate = '2019-08-20'
        fp3.OperatingUntilDate = '2019-10-26'
        fp3.OperatingOnDays = 1234567
        fp3.MarketingCarrier = 58
        fp3.MarketingCarrierIata = 'AZ'
        fp3.MarketingFlightNumber = '2828'
        fp3.IsCodeshare = False
        fp3.LegSeqNumber = 1

        codeshares_map = CodesharesMap(MIN_TEST_FLIGHT_PATTERN_ID, logger)
        codeshares_map.add_flight(fp1)
        codeshares_map.add_flight(fp2)
        codeshares_map.add_flight(fp3)

        # nothing changes for the operating flight
        # for the amadeus codeshare flight we get nothing: it's replaced by sirena's flight
        # for the codeshare flight we shall get new flight pattern
        fp3expected = TFlightPattern()
        fp3expected.Id = 101
        fp3expected.FlightLegKey = 'AZ.2828.1. 05'
        fp3expected.FlightId = 11
        fp3expected.BucketKey = '26.1414.1'
        fp3expected.OperatingFromDate = '2019-08-20'
        fp3expected.OperatingUntilDate = '2019-10-26'
        fp3expected.OperatingOnDays = 1234567
        fp3expected.MarketingCarrier = 58
        fp3expected.MarketingCarrierIata = 'AZ'
        fp3expected.MarketingFlightNumber = '2828'
        fp3expected.IsCodeshare = True
        fp3expected.OperatingFlightPatternId = 50
        fp3expected.LegSeqNumber = 1
        assert list(codeshares_map.generate_flights_to_write()) == [fp1, fp3expected]

    def test_codeshare_flight_in_both_sirena_and_amadeus(self):
        fp1 = TFlightPattern()
        fp1.Id = 50
        fp1.FlightLegKey = 'B2.955.1. 01'
        fp1.BucketKey = '7.955.1'
        fp1.FlightId = 11
        fp1.OperatingFromDate = '2019-08-20'
        fp1.OperatingUntilDate = '2019-10-26'
        fp1.OperatingOnDays = 1234567
        fp1.MarketingCarrier = 7
        fp1.MarketingCarrierIata = 'B2'
        fp1.MarketingFlightNumber = '955'
        fp1.IsCodeshare = False
        fp1.LegSeqNumber = 1

        fp2 = TFlightPattern()
        fp2.Id = 60
        fp2.FlightLegKey = 'S7.4438.1. 01'
        fp2.BucketKey = '7.955.1'
        fp2.FlightId = 11
        fp2.OperatingFromDate = '2019-08-20'
        fp2.OperatingUntilDate = '2019-10-26'
        fp2.OperatingOnDays = 1234567
        fp2.MarketingCarrier = 23
        fp2.MarketingCarrierIata = 'S7'
        fp2.MarketingFlightNumber = '4438'
        fp2.IsCodeshare = True
        fp2.LegSeqNumber = 1

        fp3 = TFlightPattern()
        fp3.Id = MIN_SIRENA_FLIGHT_PATTERN_ID + 1
        fp3.FlightLegKey = 'B2.955.1. 101'
        fp3.FlightId = 21
        fp3.BucketKey = '7.955.1'
        fp3.OperatingFromDate = '2019-08-21'
        fp3.OperatingUntilDate = '2019-10-25'
        fp3.OperatingOnDays = 1234567
        fp3.MarketingCarrier = 7
        fp3.MarketingCarrierIata = 'B2'
        fp3.MarketingFlightNumber = '955'
        fp3.IsCodeshare = False
        fp3.LegSeqNumber = 1

        fp4 = TFlightPattern()
        fp4.Id = MIN_SIRENA_FLIGHT_PATTERN_ID + 2
        fp4.FlightLegKey = 'S7.4438.1. 102'
        fp4.FlightId = 22
        fp4.BucketKey = '23.4438.1'
        fp4.OperatingFromDate = '2019-08-20'
        fp4.OperatingUntilDate = '2019-10-26'
        fp4.OperatingOnDays = 1234567
        fp4.MarketingCarrier = 23
        fp4.MarketingCarrierIata = 'S7'
        fp4.MarketingFlightNumber = '4438'
        fp4.IsCodeshare = False
        fp4.LegSeqNumber = 1

        codeshares_map = CodesharesMap(MIN_TEST_FLIGHT_PATTERN_ID, logger)
        codeshares_map.add_flight(fp1)
        codeshares_map.add_flight(fp2)
        codeshares_map.add_flight(fp3)
        codeshares_map.add_flight(fp4)

        # operating flight from Amadeus is replaced by the one from Sirena,
        # codeshare flight from Amadeus is replaced by sirena's flight as well,
        fp4expected = TFlightPattern()
        fp4expected.Id = MIN_TEST_FLIGHT_PATTERN_ID
        fp4expected.FlightLegKey = 'S7.4438.1. 102'
        fp4expected.FlightId = 21
        fp4expected.BucketKey = '7.955.1'
        fp4expected.OperatingFromDate = '2019-08-21'
        fp4expected.OperatingUntilDate = '2019-10-25'
        fp4expected.OperatingOnDays = 1234567
        fp4expected.MarketingCarrier = 23
        fp4expected.MarketingCarrierIata = 'S7'
        fp4expected.MarketingFlightNumber = '4438'
        fp4expected.IsCodeshare = True
        fp4expected.OperatingFlightPatternId = MIN_SIRENA_FLIGHT_PATTERN_ID + 1
        fp4expected.LegSeqNumber = 1
        assert list(codeshares_map.generate_flights_to_write()) == [fp3, fp4expected]

    def test_derivative_flight_in_both_sirena_and_amadeus(self):
        fp1 = TFlightPattern()
        fp1.Id = 50
        fp1.FlightLegKey = 'B2.955.1. 01'
        fp1.BucketKey = '7.955.1'
        fp1.FlightId = 11
        fp1.OperatingFromDate = '2019-08-20'
        fp1.OperatingUntilDate = '2019-10-26'
        fp1.OperatingOnDays = 1234567
        fp1.MarketingCarrier = 7
        fp1.MarketingCarrierIata = 'B2'
        fp1.MarketingFlightNumber = '955'
        fp1.IsCodeshare = False
        fp1.LegSeqNumber = 1

        fp2 = TFlightPattern()
        fp2.Id = 60
        fp2.FlightLegKey = 'S7.4438.1. 01'
        fp2.BucketKey = '7.955.1'
        fp2.FlightId = 11
        fp2.OperatingFromDate = '2019-08-20'
        fp2.OperatingUntilDate = '2019-10-26'
        fp2.OperatingOnDays = 1234567
        fp2.MarketingCarrier = 23
        fp2.MarketingCarrierIata = 'S7'
        fp2.MarketingFlightNumber = '4438'
        fp2.IsCodeshare = True
        fp2.LegSeqNumber = 1

        fp3 = TFlightPattern()
        fp3.Id = MIN_SIRENA_FLIGHT_PATTERN_ID + 1
        fp3.FlightLegKey = 'B2.955.1. 101'
        fp3.FlightId = 21
        fp3.BucketKey = '7.955.1'
        fp3.OperatingFromDate = '2019-08-21'
        fp3.OperatingUntilDate = '2019-10-25'
        fp3.OperatingOnDays = 1234567
        fp3.MarketingCarrier = 7
        fp3.MarketingCarrierIata = 'B2'
        fp3.MarketingFlightNumber = '955'
        fp3.IsCodeshare = False
        fp3.IsDerivative = True
        fp3.LegSeqNumber = 1

        fp4 = TFlightPattern()
        fp4.Id = MIN_SIRENA_FLIGHT_PATTERN_ID + 2
        fp4.FlightLegKey = 'S7.4438.1. 102'
        fp4.FlightId = 22
        fp4.BucketKey = '7.955.1'
        fp4.OperatingFromDate = '2019-08-19'
        fp4.OperatingUntilDate = '2019-10-26'
        fp4.OperatingOnDays = 1234567
        fp4.MarketingCarrier = 23
        fp4.MarketingCarrierIata = 'S7'
        fp4.MarketingFlightNumber = '4438'
        fp4.IsCodeshare = True
        fp4.LegSeqNumber = 1

        codeshares_map = CodesharesMap(MIN_TEST_FLIGHT_PATTERN_ID, logger)
        codeshares_map.add_flight(fp1)
        codeshares_map.add_flight(fp2)
        codeshares_map.add_flight(fp3)
        codeshares_map.add_flight(fp4)

        # operating flight from Amadeus replaces the derivative one from Sirena,
        # while the codeshare flight from Amadeus is replaced by the Sirena's flight (Sirena's priority is higher)
        fp4expected = TFlightPattern()
        fp4expected.Id = MIN_TEST_FLIGHT_PATTERN_ID
        fp4expected.FlightLegKey = 'S7.4438.1. 102'
        fp4expected.FlightId = 11
        fp4expected.BucketKey = '7.955.1'
        fp4expected.OperatingFromDate = '2019-08-20'
        fp4expected.OperatingUntilDate = '2019-10-26'
        fp4expected.OperatingOnDays = 1234567
        fp4expected.MarketingCarrier = 23
        fp4expected.MarketingCarrierIata = 'S7'
        fp4expected.MarketingFlightNumber = '4438'
        fp4expected.IsCodeshare = True
        fp4expected.OperatingFlightPatternId = 50
        fp4expected.LegSeqNumber = 1
        result = list(codeshares_map.generate_flights_to_write())
        assert result == [fp1, fp4expected]

    def test_contradictory_derivative_flight_type_between_sirena_and_amadeus(self):
        # If Amadeus believes some derivative flight is operating and Sirena believes its a codeshare
        # for now we take tha data from Amadeus
        fp1 = TFlightPattern()
        fp1.Id = 50
        fp1.FlightLegKey = 'B2.955.1. 01'
        fp1.BucketKey = '7.955.1'
        fp1.FlightId = 11
        fp1.OperatingFromDate = '2019-08-20'
        fp1.OperatingUntilDate = '2019-10-26'
        fp1.OperatingOnDays = 1234567
        fp1.MarketingCarrier = 7
        fp1.MarketingCarrierIata = 'B2'
        fp1.MarketingFlightNumber = '955'
        fp1.IsCodeshare = False
        fp1.LegSeqNumber = 1

        # This is dropped since it contradicts non-Sirena operating flight received from Amadeus
        fp2 = TFlightPattern()
        fp2.Id = MIN_SIRENA_FLIGHT_PATTERN_ID + 1
        fp2.FlightLegKey = 'B2.955.1. 101'
        fp2.BucketKey = '23.955.1'
        fp2.FlightId = 21
        fp2.OperatingFromDate = '2019-08-21'
        fp2.OperatingUntilDate = '2019-10-25'
        fp2.OperatingOnDays = 1234567
        fp2.MarketingCarrier = 7
        fp2.MarketingCarrierIata = 'B2'
        fp2.MarketingFlightNumber = '955'
        fp2.IsCodeshare = True
        fp2.LegSeqNumber = 1

        # This is dropped as derivative operating Sirena's flight also seen in Amadeus
        fp3 = TFlightPattern()
        fp3.Id = MIN_SIRENA_FLIGHT_PATTERN_ID + 2
        fp3.FlightLegKey = 'B2.955.1. 102'
        fp3.BucketKey = '7.955.1'
        fp3.FlightId = 21
        fp3.OperatingFromDate = '2019-11-21'
        fp3.OperatingUntilDate = '2019-11-25'
        fp3.OperatingOnDays = 1234567
        fp3.MarketingCarrier = 7
        fp3.MarketingCarrierIata = 'B2'
        fp3.MarketingFlightNumber = '955'
        fp3.IsDerivative = True
        fp3.IsCodeshare = False
        fp2.LegSeqNumber = 1

        fp4 = TFlightPattern()
        fp4.Id = MIN_SIRENA_FLIGHT_PATTERN_ID + 3
        fp4.FlightLegKey = 'S7.4438.1. 103'
        fp4.FlightId = 22
        fp4.BucketKey = '23.955.1'
        fp4.OperatingFromDate = '2019-08-19'
        fp4.OperatingUntilDate = '2019-10-26'
        fp4.OperatingOnDays = 1234567
        fp4.MarketingCarrier = 23
        fp4.MarketingCarrierIata = 'S7'
        fp4.MarketingFlightNumber = '4438'
        fp4.IsCodeshare = False
        fp4.LegSeqNumber = 1

        codeshares_map = CodesharesMap(MIN_TEST_FLIGHT_PATTERN_ID, logger)
        codeshares_map.add_flight(fp1)
        codeshares_map.add_flight(fp2)
        codeshares_map.add_flight(fp3)
        codeshares_map.add_flight(fp4)

        result = list(codeshares_map.generate_flights_to_write())
        assert result == [fp1, fp4]

    def test_sirena_codeshare_flight_referencing_sirena(self):
        fp1 = TFlightPattern()
        fp1.Id = 50
        fp1.FlightLegKey = 'SU.1414.1. 01'
        fp1.BucketKey = '26.1414.1'
        fp1.FlightId = 11
        fp1.OperatingFromDate = '2019-08-20'
        fp1.OperatingUntilDate = '2019-10-26'
        fp1.OperatingOnDays = 1234567
        fp1.MarketingCarrier = 26
        fp1.MarketingCarrierIata = 'SU'
        fp1.MarketingFlightNumber = '1414'
        fp1.IsCodeshare = False
        fp1.LegSeqNumber = 1

        fp2 = TFlightPattern()
        fp2.Id = 60
        fp2.FlightLegKey = 'AZ.2828.1. 14'
        fp2.BucketKey = '26.1414.1'
        fp2.FlightId = 19
        fp2.OperatingFromDate = '2019-08-20'
        fp2.OperatingUntilDate = '2019-10-26'
        fp2.OperatingOnDays = 1234567
        fp2.MarketingCarrier = 58
        fp2.MarketingCarrierIata = 'AZ'
        fp2.MarketingFlightNumber = '2828'
        fp2.IsCodeshare = True
        fp2.LegSeqNumber = 1

        fp3 = TFlightPattern()
        fp3.Id = MIN_SIRENA_FLIGHT_PATTERN_ID + 1
        fp3.FlightLegKey = 'SU.1414.1. 09'
        fp3.BucketKey = '26.1414.1'
        fp3.FlightId = 51
        fp3.OperatingFromDate = '2019-08-21'
        fp3.OperatingUntilDate = '2019-10-25'
        fp3.OperatingOnDays = 123457
        fp3.MarketingCarrier = 26
        fp3.MarketingCarrierIata = 'SU'
        fp3.MarketingFlightNumber = '1414'
        fp3.IsCodeshare = False
        fp3.LegSeqNumber = 1

        fp4 = TFlightPattern()
        fp4.Id = MIN_SIRENA_FLIGHT_PATTERN_ID + 2
        fp4.FlightLegKey = 'AZ.2828.1. 18'
        fp4.FlightId = 55
        fp4.BucketKey = '58.2828.1'
        fp4.OperatingFromDate = '2019-08-20'
        fp4.OperatingUntilDate = '2019-10-26'
        fp4.OperatingOnDays = 123457
        fp4.MarketingCarrier = 58
        fp4.MarketingCarrierIata = 'AZ'
        fp4.MarketingFlightNumber = '2828'
        fp4.IsCodeshare = False
        fp4.LegSeqNumber = 1

        codeshares_map = CodesharesMap(MIN_TEST_FLIGHT_PATTERN_ID, logger)
        codeshares_map.add_flight(fp1)
        codeshares_map.add_flight(fp2)
        codeshares_map.add_flight(fp3)
        codeshares_map.add_flight(fp4)

        # we shall get nothing for the Amadeus flight, since it's replaced by the sirena flight,
        # nothing changes for the operating sirena flight,
        # and for the Amadeus codeshare flight we get nothing: it's replaced by sirena's flight,
        # for sirena the codeshare flight we shall get the flight pattern referencing sirena's flight_base
        fp4expected = TFlightPattern()
        fp4expected.Id = MIN_TEST_FLIGHT_PATTERN_ID
        fp4expected.FlightLegKey = 'AZ.2828.1. 18'
        fp4expected.FlightId = 51
        fp4expected.BucketKey = '26.1414.1'
        fp4expected.OperatingFromDate = '2019-08-21'
        fp4expected.OperatingUntilDate = '2019-10-25'
        fp4expected.OperatingOnDays = 123457
        fp4expected.MarketingCarrier = 58
        fp4expected.MarketingCarrierIata = 'AZ'
        fp4expected.MarketingFlightNumber = '2828'
        fp4expected.IsCodeshare = True
        fp4expected.OperatingFlightPatternId = 1000000001
        fp4expected.LegSeqNumber = 1
        assert list(codeshares_map.generate_flights_to_write()) == [fp3, fp4expected]

    def test_sirena_operating_flight_with_codeshare_in_amadeus(self):
        fp1 = TFlightPattern()
        fp1.Id = MIN_SIRENA_FLIGHT_PATTERN_ID + 1
        fp1.FlightLegKey = 'S7.2047.1. 05'
        fp1.FlightId = 15
        fp1.BucketKey = '23.2047.1'
        fp1.OperatingFromDate = '2019-08-31'
        fp1.OperatingUntilDate = '2019-10-01'
        fp1.OperatingOnDays = 1234567
        fp1.MarketingCarrier = 23
        fp1.MarketingCarrierIata = 'S7'
        fp1.MarketingFlightNumber = '2047'
        fp1.IsCodeshare = False
        fp1.LegSeqNumber = 1

        fp2 = TFlightPattern()
        fp2.Id = 60
        fp2.FlightLegKey = 'EK.7891.1. 14'
        fp2.BucketKey = '23.2047.1'
        fp2.FlightId = 15
        fp2.OperatingFromDate = '2019-08-20'
        fp2.OperatingUntilDate = '2019-10-26'
        fp2.OperatingOnDays = 1234567
        fp2.MarketingCarrier = 1373
        fp2.MarketingCarrierIata = 'EK'
        fp2.MarketingFlightNumber = '7891'
        fp2.IsCodeshare = True
        fp2.LegSeqNumber = 1

        codeshares_map = CodesharesMap(MIN_TEST_FLIGHT_PATTERN_ID, logger)
        codeshares_map.add_flight(fp2)
        codeshares_map.add_flight(fp1)

        # nothing changes for the operating flight
        # for the Amadeus codeshare flight we should get just the new leg calculated via overlaps
        fp2expected = TFlightPattern()
        fp2expected.MergeFrom(fp2)
        fp2expected.Id = MIN_TEST_FLIGHT_PATTERN_ID
        fp2expected.OperatingFromDate = '2019-08-31'
        fp2expected.OperatingUntilDate = '2019-10-01'
        fp2expected.OperatingFlightPatternId = 1000000001
        assert list(codeshares_map.generate_flights_to_write()) == [fp2expected, fp1]

    def test_codeshare_flight_split_by_dates(self):
        fp1 = TFlightPattern()
        fp1.Id = MIN_SIRENA_FLIGHT_PATTERN_ID + 1
        fp1.FlightLegKey = 'SU.1414.1. 01'
        fp1.BucketKey = '26.1414.1'
        fp1.FlightId = 11
        fp1.OperatingFromDate = '2019-08-20'
        fp1.OperatingUntilDate = '2019-10-26'
        fp1.OperatingOnDays = 1234567
        fp1.MarketingCarrier = 26
        fp1.MarketingCarrierIata = 'SU'
        fp1.MarketingFlightNumber = '1414'
        fp1.IsCodeshare = False
        fp1.LegSeqNumber = 1

        fp2 = TFlightPattern()
        fp2.Id = MIN_SIRENA_FLIGHT_PATTERN_ID + 2
        fp2.FlightLegKey = 'SU.1414.1. 02'
        fp2.BucketKey = '26.1414.1'
        fp2.FlightId = 12
        fp2.OperatingFromDate = '2019-10-27'
        fp2.OperatingUntilDate = '2019-11-14'
        fp2.OperatingOnDays = 1234
        fp2.MarketingCarrier = 26
        fp2.MarketingCarrierIata = 'SU'
        fp2.MarketingFlightNumber = '1414'
        fp2.IsCodeshare = False
        fp2.LegSeqNumber = 1

        fp3 = TFlightPattern()
        fp3.Id = 60
        fp3.FlightLegKey = 'AZ.2828.1. 08'
        fp3.FlightId = 15
        fp3.BucketKey = '26.1414.1'
        fp3.OperatingFromDate = '2019-08-20'
        fp3.OperatingUntilDate = '2019-11-15'
        fp3.OperatingOnDays = 1257
        fp3.MarketingCarrier = 58
        fp3.MarketingCarrierIata = 'AZ'
        fp3.MarketingFlightNumber = '2828'
        fp3.IsCodeshare = True
        fp3.LegSeqNumber = 1

        codeshares_map = CodesharesMap(MIN_TEST_FLIGHT_PATTERN_ID, logger)
        codeshares_map.add_flight(fp1)
        codeshares_map.add_flight(fp2)
        codeshares_map.add_flight(fp3)

        # nothing changes for the operating flights
        # for the codeshare flight we shall get two new flight patterns
        expected1 = TFlightPattern()
        expected1.Id = MIN_TEST_FLIGHT_PATTERN_ID
        expected1.FlightLegKey = 'AZ.2828.1. 08'
        expected1.FlightId = 11
        expected1.BucketKey = '26.1414.1'
        expected1.OperatingFromDate = '2019-08-20'
        expected1.OperatingUntilDate = '2019-10-25'
        expected1.OperatingOnDays = 1257
        expected1.MarketingCarrier = 58
        expected1.MarketingCarrierIata = 'AZ'
        expected1.MarketingFlightNumber = '2828'
        expected1.IsCodeshare = True
        expected1.OperatingFlightPatternId = 1000000001
        expected1.LegSeqNumber = 1

        expected2 = TFlightPattern()
        expected2.Id = MIN_TEST_FLIGHT_PATTERN_ID + 1
        expected2.FlightLegKey = 'AZ.2828.1. 08'
        expected2.FlightId = 12
        expected2.BucketKey = '26.1414.1'
        expected2.OperatingFromDate = '2019-10-28'
        expected2.OperatingUntilDate = '2019-11-12'
        expected2.OperatingOnDays = 12  # 1234 of the operating leg intersected with 1257 of the codeshare leg
        expected2.MarketingCarrier = 58
        expected2.MarketingCarrierIata = 'AZ'
        expected2.MarketingFlightNumber = '2828'
        expected2.IsCodeshare = True
        expected2.OperatingFlightPatternId = 1000000002
        expected2.LegSeqNumber = 1

        assert list(codeshares_map.generate_flights_to_write()) == [expected1, expected2, fp1, fp2]
