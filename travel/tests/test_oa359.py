# coding=utf-8
from __future__ import unicode_literals

import logging

import travel.avia.shared_flights.tasks.amadeus_parser.flights_parser as flights_parser

logger = logging.getLogger(__name__)


class TestFlightsParser:

    def test_codeshare(self):
        """ Parse single csv line with codeshares for test
        """
        flights_importer = flights_parser.AmadeusFlightsParser(logger=logger)
        flights_importer._parse_flights(iter([
            'operating_carrier^operating_flight_code^stops^dep_date^dep_time^dep_weekday^dep_week^arrival_date^arrival_time^'+
            'arrival_weekday^arrival_week^board_airport^board_city^board_country^board_region^board_terminal^off_airport^off_city^'+
            'off_country^off_region^off_terminal^direct_distance^total_capacity^first_total_capacity^business_total_capacity^'+
            'economy_total_capacity^premiumeco_total_capacity^codeshare_flights^aircraft_type^aircraft_family^route^leg_sequence_number',
            'OA^359^0^2021-05-29^1835^6^202121^2021-05-29^1920^6^202121^JTR^JTR^GR^EUR^A^ATH^ATH^GR^EUR^B^218^174^0^0^174^0^A3 7359^320^32S^JTR-ATH^1',
            'OA^359^0^2021-05-30^1835^7^202121^2021-05-30^1920^7^202121^JTR^JTR^GR^EUR^A^ATH^ATH^GR^EUR^B^218^174^0^0^174^0^A3 7359^320^32S^JTR-ATH^1',
            'OA^359^0^2021-05-31^1900^1^202122^2021-05-31^2000^1^202122^JTR^JTR^GR^EUR^A^ATH^ATH^GR^EUR^B^218^174^0^0^174^0^^320^32S^JTR-ATH^1',
        ]
        ))

        flight_bases = []
        flight_patterns = []
        codeshares = []

        flights_importer.list_flights(
            on_flight_base=lambda fb: flight_bases.append(str(fb)),
            on_flight_pattern=lambda fp: flight_patterns.append(TestFlightsParser.remove_id(str(fp))),
            on_codeshare=lambda cs: codeshares.append(str(cs)),
        )

        flight_bases.sort()
        flight_patterns.sort()
        codeshares.sort()

        assert flight_bases == [
            'Id: 1\nOperatingCarrierIata: "OA"\nOperatingFlightNumber: "359"\nLegSeqNumber: 1\nDepartureStationIata: "JTR"\n'+
                'ScheduledDepartureTime: 1835\nDepartureTerminal: "A"\nArrivalStationIata: "ATH"\nScheduledArrivalTime: 1920\n'+
                'ArrivalTerminal: "B"\nAircraftModel: "320"\n',
            'Id: 2\nOperatingCarrierIata: "OA"\nOperatingFlightNumber: "359"\nLegSeqNumber: 1\nDepartureStationIata: "JTR"\n'+
                'ScheduledDepartureTime: 1900\nDepartureTerminal: "A"\nArrivalStationIata: "ATH"\nScheduledArrivalTime: 2000\n'+
                'ArrivalTerminal: "B"\nAircraftModel: "320"\n',
        ]
        assert flight_patterns == [
            'FlightId: 1; FlightLegKey: "A3.7359.1.1"; OperatingFromDate: "2021-05-29"; OperatingUntilDate: "2021-06-04"; '+
                'OperatingOnDays: 67; MarketingCarrierIata: "A3"; MarketingFlightNumber: "7359"; IsCodeshare: true; BucketKey: "OA.359.1"; LegSeqNumber: 1; ',
            'FlightId: 1; FlightLegKey: "OA.359.1.1"; OperatingFromDate: "2021-05-29"; OperatingUntilDate: "2021-06-04"; '+
                'OperatingOnDays: 67; MarketingCarrierIata: "OA"; MarketingFlightNumber: "359"; BucketKey: "OA.359.1"; LegSeqNumber: 1; ',
            'FlightId: 2; FlightLegKey: "OA.359.1.2"; OperatingFromDate: "2021-05-31"; OperatingUntilDate: "2021-05-31"; '+
                'OperatingOnDays: 1; MarketingCarrierIata: "OA"; MarketingFlightNumber: "359"; BucketKey: "OA.359.1"; LegSeqNumber: 1; ',
        ]
        assert codeshares == [
            'MarketingCarrierCode: "A3"\nMarketingFlightNumber: "7359"\nMarketingLegNumber: 1\n'+
            'OperatingCarrierCode: "OA"\nOperatingFlightNumber: "359"\nOperatingLegNumber: 1\n'+
                'FromDate: "2021-05-29"\nUntilDate: "2021-06-04"\n',
        ]

    def test_multi_segment(self):
        """ Parse single multi-segment flight for test
        """
        flights_importer = flights_parser.AmadeusFlightsParser(logger=logger)
        lines = [
            'operating_carrier^operating_flight_code^stops^dep_date^dep_time^dep_weekday^dep_week^arrival_date^arrival_time^'+
            'arrival_weekday^arrival_week^board_airport^board_city^board_country^board_region^board_terminal^off_airport^off_city^'+
            'off_country^off_region^off_terminal^direct_distance^total_capacity^first_total_capacity^business_total_capacity^'+
            'economy_total_capacity^premiumeco_total_capacity^codeshare_flights^aircraft_type^aircraft_family^route^leg_sequence_number',
            'GL^502^0^2022-03-02^940^3^202209^2022-03-03^1025^3^202209^SFJ^SFJ^GL^NOA^^JAV^JAV^GL^NOA^^248^29^0^0^29^0^^DH2^DH8^SFJ-JAV^2',
            'GL^502^0^2022-03-02^800^3^202209^2022-03-02^855^3^202209^GOH^GOH^GL^NOA^^SFJ^SFJ^GL^NOA^^317^29^0^0^29^0^^DH2^DH8^GOH-SFJ^1',
            'GL^502^1^2022-03-02^800^3^202209^2022-03-03^1025^3^202209^GOH^GOH^GL^NOA^^JAV^JAV^GL^NOA^^562^29^0^0^29^0^^DH2^DH8^GOH-SFJ-JAV^1',
        ]
        flights_importer._cache_segments(iter(lines))
        flights_importer._parse_flights(iter(lines))

        flight_bases = []
        flight_patterns = []
        codeshares = []

        flights_importer.list_flights(
            on_flight_base=lambda fb: flight_bases.append(str(fb)),
            on_flight_pattern=lambda fp: flight_patterns.append(TestFlightsParser.remove_id(str(fp))),
            on_codeshare=lambda cs: codeshares.append(str(cs)),
        )

        flight_bases.sort()
        flight_patterns.sort()
        codeshares.sort()

        assert flight_bases == [
            'Id: 1\nOperatingCarrierIata: "GL"\nOperatingFlightNumber: "502"\nLegSeqNumber: 2\nDepartureStationIata: "SFJ"\n'+
                'ScheduledDepartureTime: 940\nArrivalStationIata: "JAV"\nScheduledArrivalTime: 1025\nAircraftModel: "DH2"\n',
            'Id: 2\nOperatingCarrierIata: "GL"\nOperatingFlightNumber: "502"\nLegSeqNumber: 1\nDepartureStationIata: "GOH"\n'+
                'ScheduledDepartureTime: 800\nArrivalStationIata: "SFJ"\nScheduledArrivalTime: 855\nAircraftModel: "DH2"\n',
        ]
        assert flight_patterns == [
            'FlightId: 1; FlightLegKey: "GL.502.2.1"; OperatingFromDate: "2022-03-02"; OperatingUntilDate: "2022-03-02"; '+
                'OperatingOnDays: 3; MarketingCarrierIata: "GL"; MarketingFlightNumber: "502"; BucketKey: "GL.502.2"; ArrivalDayShift: 1; '+
                'LegSeqNumber: 2; ',
            'FlightId: 2; FlightLegKey: "GL.502.1.2"; OperatingFromDate: "2022-03-02"; OperatingUntilDate: "2022-03-02"; '+
                'OperatingOnDays: 3; MarketingCarrierIata: "GL"; MarketingFlightNumber: "502"; BucketKey: "GL.502.1"; LegSeqNumber: 1; ',
        ]
        assert codeshares == []

    def test_departure_day_shift(self):
        """ Parse single multi-segment flight with departure day shift for test
        """
        flights_importer = flights_parser.AmadeusFlightsParser(logger=logger)
        lines = [
            'operating_carrier^operating_flight_code^stops^dep_date^dep_time^dep_weekday^dep_week^arrival_date^arrival_time^'+
            'arrival_weekday^arrival_week^board_airport^board_city^board_country^board_region^board_terminal^off_airport^off_city^'+
            'off_country^off_region^off_terminal^direct_distance^total_capacity^first_total_capacity^business_total_capacity^'+
            'economy_total_capacity^premiumeco_total_capacity^codeshare_flights^aircraft_type^aircraft_family^route^leg_sequence_number',
            'GL^502^0^2022-03-03^740^3^202209^2022-03-03^1025^3^202209^SFJ^SFJ^GL^NOA^^JAV^JAV^GL^NOA^^248^29^0^0^29^0^^DH2^DH8^SFJ-JAV^2',
            'GL^502^0^2022-03-02^800^3^202209^2022-03-02^855^3^202209^GOH^GOH^GL^NOA^^SFJ^SFJ^GL^NOA^^317^29^0^0^29^0^^DH2^DH8^GOH-SFJ^1',
            'GL^502^1^2022-03-02^800^3^202209^2022-03-03^1025^3^202209^GOH^GOH^GL^NOA^^JAV^JAV^GL^NOA^^562^29^0^0^29^0^^DH2^DH8^GOH-SFJ-JAV^1',
        ]
        flights_importer._cache_segments(iter(lines))
        flights_importer._parse_flights(iter(lines))

        flight_bases = []
        flight_patterns = []
        codeshares = []

        flights_importer.list_flights(
            on_flight_base=lambda fb: flight_bases.append(str(fb)),
            on_flight_pattern=lambda fp: flight_patterns.append(TestFlightsParser.remove_id(str(fp))),
            on_codeshare=lambda cs: codeshares.append(str(cs)),
        )

        flight_bases.sort()
        flight_patterns.sort()
        codeshares.sort()

        assert flight_bases == [
            'Id: 1\nOperatingCarrierIata: "GL"\nOperatingFlightNumber: "502"\nLegSeqNumber: 2\nDepartureStationIata: "SFJ"\n'+
                'ScheduledDepartureTime: 740\nArrivalStationIata: "JAV"\nScheduledArrivalTime: 1025\nAircraftModel: "DH2"\n',
            'Id: 2\nOperatingCarrierIata: "GL"\nOperatingFlightNumber: "502"\nLegSeqNumber: 1\nDepartureStationIata: "GOH"\n'+
                'ScheduledDepartureTime: 800\nArrivalStationIata: "SFJ"\nScheduledArrivalTime: 855\nAircraftModel: "DH2"\n',
        ]
        assert flight_patterns == [
            'FlightId: 1; FlightLegKey: "GL.502.2.1"; OperatingFromDate: "2022-03-03"; OperatingUntilDate: "2022-03-03"; '+
                'OperatingOnDays: 4; MarketingCarrierIata: "GL"; MarketingFlightNumber: "502"; BucketKey: "GL.502.2"; LegSeqNumber: 2; '+
                'DepartureDayShift: 1; ',
            'FlightId: 2; FlightLegKey: "GL.502.1.2"; OperatingFromDate: "2022-03-02"; OperatingUntilDate: "2022-03-02"; '+
                'OperatingOnDays: 3; MarketingCarrierIata: "GL"; MarketingFlightNumber: "502"; BucketKey: "GL.502.1"; LegSeqNumber: 1; ',
        ]
        assert codeshares == []

    @staticmethod
    def remove_id(text):
        if not text:
            return text
        return '; '.join(text.split('\n')[1:])
