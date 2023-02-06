# coding=utf-8
from __future__ import unicode_literals

import logging
from datetime import date
from travel.avia.shared_flights.data_importer.storage.missing_data import MissingDataStorage

from travel.avia.shared_flights.data_importer.sirena_flights_parser import SirenaFlightsParser
from travel.avia.shared_flights.lib.python.consts.consts import MIN_SIRENA_FLIGHT_BASE_ID, MIN_SIRENA_FLIGHT_PATTERN_ID
from travel.proto.dicts.rasp.carrier_pb2 import TCarrier
from travel.proto.shared_flights.sirena.routes_pb2 import TRoute, TSirenaFlightPattern, TStopPoint
from travel.proto.shared_flights.snapshots.station_with_codes_pb2 import TStationWithCodes
from travel.proto.shared_flights.ssim.flights_pb2 import TFlightBase, TFlightPattern

logger = logging.getLogger(__name__)


class TestSirenaFlightsParser:

    def test_parse_data(self):
        carrier1 = TCarrier()
        carrier1.Id = 26
        carrier1.Iata = 'SU'
        carrier1.SirenaId = 'СУ'

        carrier2 = TCarrier()
        carrier2.Id = 27
        carrier2.Iata = ''
        carrier2.SirenaId = 'Ж8'

        carriers = [carrier1, carrier2]

        station1 = TStationWithCodes()
        station1.Station.Id = 11
        station1.Station.SettlementId = 12
        station1.IataCode = 'SVX'
        station1.SirenaCode = 'КЛЦ'
        station1.Station.TimeZoneId = 1

        station2 = TStationWithCodes()
        station2.Station.Id = 21
        station2.Station.SettlementId = 22
        station2.IataCode = ''
        station2.SirenaCode = 'АЕК'
        station2.Station.TimeZoneId = 1

        station3 = TStationWithCodes()
        station3.Station.Id = 31
        station3.Station.SettlementId = 32
        station3.IataCode = 'LED'
        station3.SirenaCode = 'СПБ'
        station3.Station.TimeZoneId = 1

        stations = {
            station1.Station.Id: station1,
            station2.Station.Id: station2,
            station3.Station.Id: station3,
        }

        timezones = {
            1: 'Europe/Moscow',
        }

        route1 = TRoute()
        route1.CarrierCode = 'SU'
        route1.FlightNumber = '1'

        fp1point1 = TStopPoint()
        fp1point1.StationCode = 'SVX'
        fp1point1.DepartureTime = 1100
        fp1point1.ArrivalTime = 1155
        fp1point1.Terminal = 'Д'

        fp1point2 = TStopPoint()
        fp1point2.CityCode = 'АЕК'
        fp1point2.DepartureTime = 1200
        fp1point2.ArrivalTime = 1255
        fp1point2.Terminal = 'Z'

        fp1point3 = TStopPoint()
        fp1point3.StationCode = 'СПБ'
        fp1point3.DepartureTime = 1300
        fp1point3.ArrivalTime = 1355
        fp1point3.Terminal = 'Я'

        r1flight_pattern1 = TSirenaFlightPattern()
        r1flight_pattern1.OperatingFromDate = 20191201
        r1flight_pattern1.OperatingUntilDate = 20191215
        r1flight_pattern1.OperatingOnDays = 1237
        r1flight_pattern1.AircraftModel = '737'
        r1flight_pattern1.StopPoints.extend([fp1point1, fp1point2, fp1point3])
        route1.FlightPatterns.extend([r1flight_pattern1])

        fp2point1 = TStopPoint()
        fp2point1.StationCode = 'КЛЦ'
        fp2point1.DepartureTime = 1400
        fp2point1.ArrivalTime = 1455
        fp2point1.Terminal = 'Д'

        fp2point2 = TStopPoint()
        fp2point2.CityCode = 'АЕК'
        fp2point2.DepartureTime = 1500
        fp2point2.ArrivalTime = 1555
        fp2point2.Terminal = 'Z'

        r1flight_pattern2 = TSirenaFlightPattern()
        r1flight_pattern2.OperatingFromDate = 20191216
        r1flight_pattern2.OperatingUntilDate = 20191230
        r1flight_pattern2.OperatingOnDays = 2367
        r1flight_pattern2.AircraftModel = '737'
        r1flight_pattern2.StopPoints.extend([fp2point1, fp2point2])
        route1.FlightPatterns.extend([r1flight_pattern2])

        route2 = TRoute()
        route2.CarrierCode = 'Ж8'
        route2.FlightNumber = '2'
        route2.FlightPatterns.extend([r1flight_pattern1])

        route3 = TRoute()
        route3.CarrierCode = 'СУ'
        route3.FlightNumber = '3'
        route3.FlightPatterns.extend([r1flight_pattern2])

        routes = [route1, route2, route3]

        missing_data = MissingDataStorage()
        sirena_flight_parser = SirenaFlightsParser(
            logger,
            carriers,
            stations,
            timezones,
            missing_data,
            date(2019, 12, 1),
        )
        flight_bases, flight_patterns, _, = sirena_flight_parser.parse_data([], routes)

        assert len(flight_bases) == 6
        assert len(flight_patterns) == 5

        fb0 = flight_bases[MIN_SIRENA_FLIGHT_BASE_ID + 1]
        assert fb0.Id == MIN_SIRENA_FLIGHT_BASE_ID + 1
        assert fb0.OperatingCarrier == 26
        assert fb0.OperatingCarrierIata == 'SU'
        assert fb0.OperatingFlightNumber == '1'
        assert fb0.ItineraryVariationIdentifier == '1'
        assert fb0.LegSeqNumber == 1
        assert fb0.DepartureStation == 11
        assert fb0.DepartureStationIata == 'SVX'
        assert fb0.ScheduledDepartureTime == 1100
        assert fb0.DepartureTerminal == 'Д'
        assert fb0.ArrivalStation == 21
        assert fb0.ArrivalStationIata == 'АЕК'
        assert fb0.ScheduledArrivalTime == 1255
        assert fb0.ArrivalTerminal == 'Z'
        assert fb0.AircraftModel == '737'
        assert fb0.BucketKey == '26.1.1'

        fp0_legs = flight_patterns[fb0.BucketKey]
        assert len(fp0_legs) == 2
        fp0 = fp0_legs[0]
        assert fp0.Id == MIN_SIRENA_FLIGHT_PATTERN_ID + 1
        assert fp0.FlightId == fb0.Id
        assert fp0.FlightLegKey == 'SU.1.1.1'
        assert fp0.OperatingFromDate == '2019-12-01'
        assert fp0.OperatingUntilDate == '2019-12-15'
        assert fp0.OperatingOnDays == 1237
        assert fp0.MarketingCarrier == 26
        assert fp0.MarketingCarrierIata == 'SU'
        assert fp0.MarketingFlightNumber == '1'
        assert fp0.BucketKey == '26.1.1'

        fb1 = flight_bases[MIN_SIRENA_FLIGHT_BASE_ID + 2]
        assert fb1.Id == MIN_SIRENA_FLIGHT_BASE_ID + 2
        assert fb1.OperatingCarrier == 26
        assert fb1.OperatingCarrierIata == 'SU'
        assert fb1.OperatingFlightNumber == '1'
        assert fb1.ItineraryVariationIdentifier == '2'
        assert fb1.LegSeqNumber == 2
        assert fb1.DepartureStation == 21
        assert fb1.DepartureStationIata == 'АЕК'
        assert fb1.ScheduledDepartureTime == 1200
        assert fb1.DepartureTerminal == 'Z'
        assert fb1.ArrivalStation == 31
        assert fb1.ArrivalStationIata == 'LED'
        assert fb1.ScheduledArrivalTime == 1355
        assert fb1.ArrivalTerminal == 'Я'
        assert fb1.AircraftModel == '737'
        assert fb1.BucketKey == '26.1.2'

        fb2 = flight_bases[MIN_SIRENA_FLIGHT_BASE_ID + 3]
        assert fb2.Id == MIN_SIRENA_FLIGHT_BASE_ID + 3
        assert fb2.OperatingCarrier == 26
        assert fb2.OperatingCarrierIata == 'SU'
        assert fb2.OperatingFlightNumber == '1'
        assert fb2.ItineraryVariationIdentifier == '3'
        assert fb2.LegSeqNumber == 1
        assert fb2.DepartureStation == 11
        assert fb2.DepartureStationIata == 'SVX'
        assert fb2.ScheduledDepartureTime == 1400
        assert fb2.DepartureTerminal == 'Д'
        assert fb2.ArrivalStation == 21
        assert fb2.ArrivalStationIata == 'АЕК'
        assert fb2.ScheduledArrivalTime == 1555
        assert fb2.ArrivalTerminal == 'Z'
        assert fb2.AircraftModel == '737'
        assert fb2.BucketKey == '26.1.1'

        fp1 = fp0_legs[1]
        # FP IDs order is non-sequential because they are re-grouped by the bucket_key
        assert fp1.Id == MIN_SIRENA_FLIGHT_PATTERN_ID + 3
        assert fp1.FlightId == fb2.Id
        assert fp1.FlightLegKey == 'SU.1.1.3'
        assert fp1.OperatingFromDate == '2019-12-16'
        assert fp1.OperatingUntilDate == '2019-12-30'
        assert fp1.OperatingOnDays == 2367
        assert fp1.MarketingCarrier == 26
        assert fp1.MarketingCarrierIata == 'SU'
        assert fp1.MarketingFlightNumber == '1'
        assert fp1.BucketKey == '26.1.1'

        fp2_legs = flight_patterns[fb1.BucketKey]
        assert len(fp2_legs) == 1
        fp2 = fp2_legs[0]
        assert fp2.Id == MIN_SIRENA_FLIGHT_PATTERN_ID + 2
        assert fp2.FlightId == fb1.Id
        assert fp2.FlightLegKey == 'SU.1.2.2'
        assert fp2.OperatingFromDate == '2019-12-01'
        assert fp2.OperatingUntilDate == '2019-12-15'
        assert fp2.OperatingOnDays == 1237
        assert fp2.MarketingCarrier == 26
        assert fp2.MarketingCarrierIata == 'SU'
        assert fp2.MarketingFlightNumber == '1'
        assert fp2.BucketKey == '26.1.2'

        # Last two cases are only for carrier codes testing, multi-leg and single-leg
        fb3 = flight_bases[MIN_SIRENA_FLIGHT_BASE_ID + 4]
        assert fb3.Id == MIN_SIRENA_FLIGHT_BASE_ID + 4
        assert fb3.OperatingCarrier == 27
        assert fb3.OperatingCarrierIata == 'Ж8'
        assert fb3.OperatingFlightNumber == '2'
        assert fb3.ItineraryVariationIdentifier == '4'
        assert fb3.LegSeqNumber == 1
        assert fb3.BucketKey == '27.2.1'

        fp3_legs = flight_patterns[fb3.BucketKey]
        assert len(fp3_legs) == 1
        fp3 = fp3_legs[0]
        assert fp3.Id == MIN_SIRENA_FLIGHT_PATTERN_ID + 4
        assert fp3.FlightId == fb3.Id
        assert fp3.FlightLegKey == '27.2.1.4'
        assert fp3.OperatingFromDate == '2019-12-01'
        assert fp3.OperatingUntilDate == '2019-12-15'
        assert fp3.OperatingOnDays == 1237
        assert fp3.MarketingCarrier == 27
        assert fp3.MarketingCarrierIata == 'Ж8'
        assert fp3.MarketingFlightNumber == '2'
        assert fp3.BucketKey == '27.2.1'

        fb4 = flight_bases[MIN_SIRENA_FLIGHT_BASE_ID + 5]
        assert fb4.Id == MIN_SIRENA_FLIGHT_BASE_ID + 5
        assert fb4.OperatingCarrier == 27
        assert fb4.OperatingCarrierIata == 'Ж8'
        assert fb4.OperatingFlightNumber == '2'
        assert fb4.ItineraryVariationIdentifier == '5'
        assert fb4.LegSeqNumber == 2
        assert fb4.BucketKey == '27.2.2'

        fp4_legs = flight_patterns[fb4.BucketKey]
        assert len(fp4_legs) == 1
        fp4 = fp4_legs[0]
        assert fp4.Id == MIN_SIRENA_FLIGHT_PATTERN_ID + 5
        assert fp4.FlightId == fb4.Id
        assert fp4.FlightLegKey == '27.2.2.5'
        assert fp4.OperatingFromDate == '2019-12-01'
        assert fp4.OperatingUntilDate == '2019-12-15'
        assert fp4.OperatingOnDays == 1237
        assert fp4.MarketingCarrier == 27
        assert fp4.MarketingCarrierIata == 'Ж8'
        assert fp4.MarketingFlightNumber == '2'
        assert fp4.BucketKey == '27.2.2'

        fb5 = flight_bases[MIN_SIRENA_FLIGHT_BASE_ID + 6]
        assert fb5.Id == MIN_SIRENA_FLIGHT_BASE_ID + 6
        assert fb5.OperatingCarrier == 26
        assert fb5.OperatingCarrierIata == 'SU'
        assert fb5.OperatingFlightNumber == '3'
        assert fb5.ItineraryVariationIdentifier == '6'
        assert fb5.LegSeqNumber == 1
        assert fb5.BucketKey == '26.3.1'

        fp5_legs = flight_patterns[fb5.BucketKey]
        assert len(fp5_legs) == 1
        fp5 = fp5_legs[0]
        assert fp5.Id == MIN_SIRENA_FLIGHT_PATTERN_ID + 6
        assert fp5.FlightId == fb5.Id
        assert fp5.FlightLegKey == 'SU.3.1.6'
        assert fp5.OperatingFromDate == '2019-12-16'
        assert fp5.OperatingUntilDate == '2019-12-30'
        assert fp5.OperatingOnDays == 2367
        assert fp5.MarketingCarrier == 26
        assert fp5.MarketingCarrierIata == 'SU'
        assert fp5.MarketingFlightNumber == '3'
        assert fp5.BucketKey == '26.3.1'

    def test_parse_codeshares(self):
        carrier1 = TCarrier()
        carrier1.Id = 1
        carrier1.Iata = '5N'
        carrier1.SirenaId = '5N'

        carrier2 = TCarrier()
        carrier2.Id = 2
        carrier2.Iata = 'WZ'
        carrier2.SirenaId = 'WZ'

        carrier3 = TCarrier()
        carrier3.Id = 3
        carrier3.Iata = 'S7'
        carrier3.SirenaId = 'S7'

        carriers = [carrier1, carrier2, carrier3]

        station1 = TStationWithCodes()
        station1.Station.Id = 11
        station1.Station.SettlementId = 12
        station1.IataCode = 'DME'
        station1.SirenaCode = 'ДМД'
        station1.Station.TimeZoneId = 1

        station2 = TStationWithCodes()
        station2.Station.Id = 21
        station2.Station.SettlementId = 22
        station2.IataCode = ''
        station2.SirenaCode = 'АХГ'
        station2.Station.TimeZoneId = 1

        stations = {
            station1.Station.Id: station1,
            station2.Station.Id: station2,
        }

        timezones = {
            1: 'Europe/Moscow',
        }

        route1 = TRoute()
        route1.CarrierCode = '5N'
        route1.FlightNumber = '100'

        fp1point1 = TStopPoint()
        fp1point1.StationCode = 'ДМД'
        fp1point1.DepartureTime = 1100
        fp1point1.Terminal = 'Д'

        fp1point2 = TStopPoint()
        fp1point2.CityCode = 'АХГ'
        fp1point2.DepartureTime = 1200
        fp1point2.ArrivalTime = 1255
        fp1point2.Terminal = 'Z'

        flight_pattern = TSirenaFlightPattern()
        flight_pattern.OperatingFromDate = 20191201
        flight_pattern.OperatingUntilDate = 20191230
        flight_pattern.OperatingOnDays = 1237
        flight_pattern.StopPoints.extend([fp1point1, fp1point2])

        flight_pattern.IsCodeshare = True
        flight_pattern.OperatingFlight.CarrierCode = 'WZ'
        flight_pattern.OperatingFlight.FlightNumber = '10'

        route1.FlightPatterns.extend([flight_pattern])

        route2 = TRoute()
        route2.CarrierCode = 'S7'
        route2.FlightNumber = '1000'

        fp2 = TSirenaFlightPattern()
        fp2.OperatingFromDate = 20191201
        fp2.OperatingUntilDate = 20191230
        fp2.OperatingOnDays = 1237
        fp2.StopPoints.extend([fp1point1, fp1point2])

        fp2.IsCodeshare = True
        fp2.OperatingFlight.CarrierCode = 'WZ'
        fp2.OperatingFlight.FlightNumber = '100'

        # same as fp2 but with no intersection on dates
        fp2x = TSirenaFlightPattern()
        fp2x.OperatingFromDate = 20191201
        fp2x.OperatingUntilDate = 20191230
        fp2x.OperatingOnDays = 46
        fp2x.StopPoints.extend([fp1point1, fp1point2])

        fp2x.IsCodeshare = True
        fp2x.OperatingFlight.CarrierCode = 'WZ'
        fp2x.OperatingFlight.FlightNumber = '100'

        route2.FlightPatterns.extend([fp2, fp2x])

        route3 = TRoute()
        route3.CarrierCode = 'WZ'
        route3.FlightNumber = '100'

        fp3 = TSirenaFlightPattern()
        fp3.OperatingFromDate = 20191201
        fp3.OperatingUntilDate = 20191230
        fp3.OperatingOnDays = 1237
        fp3.StopPoints.extend([fp1point1, fp1point2])
        fp3.IsCodeshare = False

        route3.FlightPatterns.extend([fp3])

        # Please route3 before route2 to make sure order is irrelevant
        routes = [route1, route3, route2]

        start_date = date(2019, 12, 15)
        sirena_flight_parser = SirenaFlightsParser(
            logger, carriers, stations, timezones, MissingDataStorage(), start_date,
        )
        flight_bases, flight_patterns, _, = sirena_flight_parser.parse_data([], routes)

        assert len(flight_bases) == 3
        assert len(flight_patterns) == 4

        fb = flight_bases[MIN_SIRENA_FLIGHT_BASE_ID + 1]
        assert fb.Id == MIN_SIRENA_FLIGHT_BASE_ID + 1
        assert fb.OperatingCarrier == 2
        assert fb.OperatingCarrierIata == 'WZ'
        assert fb.OperatingFlightNumber == '100'
        assert fb.LegSeqNumber == 1
        assert fb.DepartureStation == 11
        assert fb.DepartureStationIata == 'DME'
        assert fb.ArrivalStation == 21
        assert fb.ArrivalStationIata == 'АХГ'
        assert fb.ItineraryVariationIdentifier == '1'
        assert fb.ScheduledDepartureTime == 1100
        assert fb.DepartureTerminal == 'Д'
        assert fb.ScheduledArrivalTime == 1255
        assert fb.ArrivalTerminal == 'Z'
        assert fb.BucketKey == '2.100.1'

        fb = flight_bases[MIN_SIRENA_FLIGHT_BASE_ID + 2]
        assert fb.Id == MIN_SIRENA_FLIGHT_BASE_ID + 2
        assert fb.OperatingCarrier == 2
        assert fb.OperatingCarrierIata == 'WZ'
        assert fb.OperatingFlightNumber == '10'
        assert fb.LegSeqNumber == 1
        assert fb.DepartureStation == 11
        assert fb.DepartureStationIata == 'DME'
        assert fb.ArrivalStation == 21
        assert fb.ArrivalStationIata == 'АХГ'
        assert fb.ItineraryVariationIdentifier == '2'
        assert fb.ScheduledDepartureTime == 1100
        assert fb.DepartureTerminal == 'Д'
        assert fb.ScheduledArrivalTime == 1255
        assert fb.ArrivalTerminal == 'Z'
        assert fb.BucketKey == '2.10.1'

        # MIN_SIRENA_FLIGHT_BASE_ID + 3 is skipped as it falls on a known codeshare
        fb = flight_bases[MIN_SIRENA_FLIGHT_BASE_ID + 4]
        assert fb.Id == MIN_SIRENA_FLIGHT_BASE_ID + 4
        assert fb.OperatingCarrier == 2
        assert fb.OperatingCarrierIata == 'WZ'
        assert fb.OperatingFlightNumber == '100'
        assert fb.LegSeqNumber == 1
        assert fb.DepartureStation == 11
        assert fb.DepartureStationIata == 'DME'
        assert fb.ArrivalStation == 21
        assert fb.ArrivalStationIata == 'АХГ'
        assert fb.ItineraryVariationIdentifier == '4'
        assert fb.ScheduledDepartureTime == 1100
        assert fb.DepartureTerminal == 'Д'
        assert fb.ScheduledArrivalTime == 1255
        assert fb.ArrivalTerminal == 'Z'
        assert fb.BucketKey == '2.100.1'

        fp1_list = flight_patterns['2.10.1']
        assert len(fp1_list) == 1
        fp1 = fp1_list[0]
        assert fp1.MarketingCarrier == 2
        assert fp1.MarketingCarrierIata == 'WZ'
        assert fp1.MarketingFlightNumber == '10'
        assert fp1.BucketKey == '2.10.1'
        assert fp1.IsCodeshare is False
        assert fp1.FlightLegKey == 'WZ.10.1.2'

        fp2_list = flight_patterns['1.100.1']
        assert len(fp2_list) == 1
        fp2 = fp2_list[0]
        assert fp2.MarketingCarrier == 1
        assert fp2.MarketingCarrierIata == '5N'
        assert fp2.MarketingFlightNumber == '100'
        assert fp2.BucketKey == '2.10.1'
        assert fp2.IsCodeshare is True
        assert fp2.FlightLegKey == '5N.100.1.2'

        for fp in [fp1, fp2]:
            assert fp.OperatingFromDate == '2019-12-01'
            assert fp.OperatingUntilDate == '2019-12-30'
            assert fp.OperatingOnDays == 1237
            assert fp.ArrivalDayShift == 0

        fp3_list = flight_patterns['2.100.1']
        assert len(fp3_list) == 2
        fp3 = fp3_list[0]
        assert fp3.MarketingCarrier == 2
        assert fp3.MarketingCarrierIata == 'WZ'
        assert fp3.MarketingFlightNumber == '100'
        assert fp3.BucketKey == '2.100.1'
        assert fp3.IsCodeshare is False
        assert fp3.FlightLegKey == 'WZ.100.1.1'
        assert fp3.OperatingOnDays == 1237

        fp3 = fp3_list[1]
        assert fp3.MarketingCarrier == 2
        assert fp3.MarketingCarrierIata == 'WZ'
        assert fp3.MarketingFlightNumber == '100'
        assert fp3.BucketKey == '2.100.1'
        assert fp3.IsCodeshare is False
        assert fp3.FlightLegKey == 'WZ.100.1.4'
        assert fp3.OperatingOnDays == 46

        fp4_list = flight_patterns['3.1000.1']
        assert len(fp4_list) == 2
        fp4 = fp4_list[0]
        assert fp4.MarketingCarrier == 3
        assert fp4.MarketingCarrierIata == 'S7'
        assert fp4.MarketingFlightNumber == '1000'
        assert fp4.BucketKey == '2.100.1'
        assert fp4.IsCodeshare is True
        assert fp4.FlightLegKey == 'S7.1000.1.3'
        assert fp4.OperatingOnDays == 1237

        fp4x = fp4_list[1]
        assert fp4x.MarketingCarrier == 3
        assert fp4x.MarketingCarrierIata == 'S7'
        assert fp4x.MarketingFlightNumber == '1000'
        assert fp4x.BucketKey == '2.100.1'
        assert fp4x.IsCodeshare is True
        assert fp4x.FlightLegKey == 'S7.1000.1.4'
        assert fp4x.OperatingOnDays == 46

    def test_parse_derivative_flight(self):
        carrier1 = TCarrier()
        carrier1.Id = 1
        carrier1.Iata = '5N'
        carrier1.SirenaId = '5N'

        carrier2 = TCarrier()
        carrier2.Id = 2
        carrier2.Iata = 'WZ'
        carrier2.SirenaId = 'WZ'

        carriers = [carrier1, carrier2]

        station1 = TStationWithCodes()
        station1.Station.Id = 11
        station1.Station.SettlementId = 12
        station1.IataCode = 'DME'
        station1.SirenaCode = 'ДМД'
        station1.Station.TimeZoneId = 1

        station2 = TStationWithCodes()
        station2.Station.Id = 21
        station2.Station.SettlementId = 22
        station2.IataCode = ''
        station2.SirenaCode = 'PAR'
        station2.Station.TimeZoneId = 1

        stations = {
            station1.Station.Id: station1,
            station2.Station.Id: station2,
        }

        timezones = {
            1: 'Europe/Moscow',
        }

        route = TRoute()
        route.CarrierCode = '5N'
        route.FlightNumber = '100'

        fp1point1 = TStopPoint()
        fp1point1.StationCode = 'DME'
        fp1point1.DepartureTime = 1100

        fp1point2 = TStopPoint()
        fp1point2.CityCode = 'PAR'
        fp1point2.DepartureTime = 1200
        fp1point2.ArrivalTime = 1255

        flight_pattern = TSirenaFlightPattern()
        flight_pattern.OperatingFromDate = 20191201
        flight_pattern.OperatingUntilDate = 20191230
        flight_pattern.OperatingOnDays = 1237
        flight_pattern.StopPoints.extend([fp1point1, fp1point2])

        flight_pattern.IsCodeshare = True
        flight_pattern.OperatingFlight.CarrierCode = 'WZ'
        flight_pattern.OperatingFlight.FlightNumber = '10'

        route.FlightPatterns.extend([flight_pattern])

        routes = [route]

        start_date = date(2019, 12, 15)
        sirena_flight_parser = SirenaFlightsParser(
            logger, carriers, stations, timezones, MissingDataStorage(), start_date,
        )
        flight_bases, flight_patterns, _, = sirena_flight_parser.parse_data([], routes)

        expected_fb = TFlightBase()
        expected_fb.Id = 1000000001
        expected_fb.OperatingCarrier = 2
        expected_fb.OperatingCarrierIata = 'WZ'
        expected_fb.OperatingFlightNumber = '10'
        expected_fb.ItineraryVariationIdentifier = '1'
        expected_fb.LegSeqNumber = 1
        expected_fb.DepartureStation = 11
        expected_fb.DepartureStationIata = 'DME'
        expected_fb.ScheduledDepartureTime = 1100
        expected_fb.ArrivalStation = 21
        expected_fb.ArrivalStationIata = 'PAR'
        expected_fb.ScheduledArrivalTime = 1255
        expected_fb.ServiceType = 'J'
        expected_fb.BucketKey = '2.10.1'

        assert flight_bases == {expected_fb.Id : expected_fb}

        expected_fp1 = TFlightPattern()
        expected_fp1.Id = 1000000001
        expected_fp1.FlightId = 1000000001
        expected_fp1.FlightLegKey = '5N.100.1.1'
        expected_fp1.OperatingFromDate = '2019-12-01'
        expected_fp1.OperatingUntilDate = '2019-12-30'
        expected_fp1.OperatingOnDays = 1237
        expected_fp1.MarketingCarrier = 1
        expected_fp1.MarketingCarrierIata = '5N'
        expected_fp1.MarketingFlightNumber = '100'
        expected_fp1.IsCodeshare = True
        expected_fp1.BucketKey = '2.10.1'
        expected_fp1.LegSeqNumber = 1

        expected_fp2 = TFlightPattern()
        expected_fp2.Id = 1000000002
        expected_fp2.FlightId = 1000000001
        expected_fp2.FlightLegKey = 'WZ.10.1.1'
        expected_fp2.OperatingFromDate = '2019-12-01'
        expected_fp2.OperatingUntilDate = '2019-12-30'
        expected_fp2.OperatingOnDays = 1237
        expected_fp2.MarketingCarrier = 2
        expected_fp2.MarketingCarrierIata = 'WZ'
        expected_fp2.MarketingFlightNumber = '10'
        expected_fp2.BucketKey = '2.10.1'
        expected_fp2.LegSeqNumber = 1
        expected_fp2.IsDerivative = True

        assert flight_patterns == {
            '1.100.1': [expected_fp1],
            '2.10.1': [expected_fp2],
        }

    def test_shift_date(self):
        assert SirenaFlightsParser.shift_date(20200331, 1) == '2020-04-01'
