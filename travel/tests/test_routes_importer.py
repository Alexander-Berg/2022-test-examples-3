# coding=utf-8
from __future__ import unicode_literals

import six

from travel.avia.shared_flights.tasks.sirena_parser.routes_importer import RoutesImporter, TIME_NOT_SPECIFIED

input_string = '''<?xml version="1.0" encoding="UTF-8"?>
<sirena>
    <answer pult="ЯНРСП1" msgid="1" time="12:03:00 20.12.2019" instance="ГРУ">
        <get_schedule2>
            <f c="ЛЫ" n="19Д">
                <pe s="09.10.2012" e="31.12.2049" f="2367" t="74Я" c="ЭБП">
                    <p c="ТЛЯ" term="3" dt="0100" />
                    <p c="NYC" p="JFK" term="Д" at="0640" dt="0835" do="1" />
                    <p c="БКК" at="1440" ao="1" />
                </pe>
            </f>
        </get_schedule2>
    </answer>
</sirena>
'''


class TestRoutesImporter:

    def test_parse_routes(self):
        importer = RoutesImporter()

        # non-existing carrier
        carriers_dict = {}
        carriers_dict[six.ensure_str('ЙН')] = six.ensure_str(input_string)
        routes = importer.parse(carriers_dict, {})
        assert len(routes) == 0

        # correct carrier
        carriers_dict = {}
        carriers_dict[six.ensure_str('ЛЫ')] = six.ensure_str(input_string)
        routes = importer.parse(carriers_dict, {six.ensure_str('ЛЫ'): {'AccountCode': '123'}})

        assert len(routes) == 1
        route = routes[0]
        assert route.CarrierCode == six.ensure_str('ЛЫ')
        assert route.FlightNumber == six.ensure_str('19Д')

        assert len(route.FlightPatterns) == 1
        flight_pattern = route.FlightPatterns[0]
        assert flight_pattern.OperatingFromDate == 20121009
        assert flight_pattern.OperatingUntilDate == 20491231
        assert flight_pattern.OperatingOnDays == 2367
        assert flight_pattern.AircraftModel == six.ensure_str('74Я')

        assert len(flight_pattern.StopPoints) == 3
        stop_point1 = flight_pattern.StopPoints[0]
        assert stop_point1.StationCode == ''
        assert stop_point1.CityCode == six.ensure_str('ТЛЯ')
        assert stop_point1.DepartureTime == 100
        assert stop_point1.DepartureDayShift == 0
        assert stop_point1.ArrivalTime == TIME_NOT_SPECIFIED
        assert stop_point1.ArrivalDayShift == 0
        assert stop_point1.Terminal == '3'

        stop_point2 = flight_pattern.StopPoints[1]
        assert stop_point2.StationCode == 'JFK'
        assert stop_point2.CityCode == 'NYC'
        assert stop_point2.DepartureTime == 835
        assert stop_point2.DepartureDayShift == 1
        assert stop_point2.ArrivalTime == 640
        assert stop_point2.ArrivalDayShift == 0
        assert stop_point2.Terminal == six.ensure_str('Д')

        stop_point3 = flight_pattern.StopPoints[2]
        assert stop_point3.CityCode == six.ensure_str('БКК')
        assert stop_point3.DepartureTime == TIME_NOT_SPECIFIED
        assert stop_point3.DepartureDayShift == 0
        assert stop_point3.ArrivalTime == 1440
        assert stop_point3.ArrivalDayShift == 1
        assert stop_point3.Terminal == ''
