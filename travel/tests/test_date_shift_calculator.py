# coding=utf-8
from __future__ import unicode_literals

import logging
from travel.avia.shared_flights.data_importer.date_shift_calculator import DateShiftCalculator
from travel.proto.shared_flights.ssim.flights_pb2 import TFlightBase, TFlightPattern
from travel.proto.shared_flights.snapshots.station_with_codes_pb2 import TStationWithCodes

logger = logging.getLogger(__name__)


class TestDateShiftCalculator:

    def test_close_tz(self):
        date_shift_calculator = self.prepare_dates_shift_calculator()

        flight_base = TFlightBase()
        flight_base.DepartureStation = 1
        flight_base.ScheduledDepartureTime = 2100
        flight_base.ArrivalStation = 2
        flight_base.ScheduledArrivalTime = 100

        flight_pattern = TFlightPattern()
        flight_pattern.OperatingFromDate = '2020-01-01'

        # MOW to SVX
        assert date_shift_calculator.calculate_arrival_day_shift(flight_pattern, flight_base) == 1

    def test_far_away_tz_west_to_east(self):
        date_shift_calculator = self.prepare_dates_shift_calculator()

        flight_base = TFlightBase()
        flight_base.DepartureStation = 3
        flight_base.ScheduledDepartureTime = 1700
        flight_base.ArrivalStation = 2
        flight_base.ScheduledArrivalTime = 100

        flight_pattern = TFlightPattern()
        flight_pattern.OperatingFromDate = '2020-01-01'

        # NYC to SVX
        assert date_shift_calculator.calculate_arrival_day_shift(flight_pattern, flight_base) == 2

    def test_far_away_tz_east_to_west(self):
        date_shift_calculator = self.prepare_dates_shift_calculator()

        flight_base = TFlightBase()
        flight_base.DepartureStation = 2
        flight_base.ScheduledDepartureTime = 1700
        flight_base.ArrivalStation = 3
        flight_base.ScheduledArrivalTime = 1000

        flight_pattern = TFlightPattern()
        flight_pattern.OperatingFromDate = '2020-01-01'

        # SVX to NYC
        assert date_shift_calculator.calculate_arrival_day_shift(flight_pattern, flight_base) == 0

    def test_even_farther_away_tz_east_to_west(self):
        date_shift_calculator = self.prepare_dates_shift_calculator()

        flight_base = TFlightBase()
        flight_base.DepartureStation = 4
        flight_base.ScheduledDepartureTime = 45
        flight_base.ArrivalStation = 5
        flight_base.ScheduledArrivalTime = 2245

        flight_pattern = TFlightPattern()
        flight_pattern.OperatingFromDate = '2020-02-02'

        # PEK to SFO
        assert date_shift_calculator.calculate_arrival_day_shift(flight_pattern, flight_base) == -1

    def prepare_dates_shift_calculator(self):
        station1 = TStationWithCodes()
        station1.Station.Id = 1
        station1.Station.TimeZoneId = 1

        station2 = TStationWithCodes()
        station2.Station.Id = 2
        station2.Station.TimeZoneId = 2

        station3 = TStationWithCodes()
        station3.Station.Id = 3
        station3.Station.TimeZoneId = 3

        station3 = TStationWithCodes()
        station3.Station.Id = 3
        station3.Station.TimeZoneId = 3

        stationPEK = TStationWithCodes()
        stationPEK.Station.Id = 4
        stationPEK.Station.TimeZoneId = 4

        stationSFO = TStationWithCodes()
        stationSFO.Station.Id = 5
        stationSFO.Station.TimeZoneId = 5

        stations = {
            station1.Station.Id: station1,
            station2.Station.Id: station2,
            station3.Station.Id: station3,
            stationPEK.Station.Id: stationPEK,
            stationSFO.Station.Id: stationSFO,
        }

        timezones = {
            1: 'Europe/Moscow',
            2: 'Asia/Yekaterinburg',
            3: 'America/New_York',
            4: 'Asia/Shanghai',
            5: 'America/Los_Angeles',
        }

        return DateShiftCalculator(stations, timezones, logger)
