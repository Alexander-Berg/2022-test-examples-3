import logging

from travel.avia.shared_flights.admin.app.parsers.apm_parser import ApmParser, Stoppoint, subtract_five_mins
from travel.avia.shared_flights.lib.python.db_models.flight_base import ApmFlightBase
from travel.library.python.safexml.safe_xml_parser import safe_xml_fromstring
from travel.proto.shared_flights.snapshots.station_with_codes_pb2 import TStationWithCodes


logger = logging.getLogger(__name__)


class TestApmParser:

    def test_subtract_five_mins(self):
        assert subtract_five_mins(0) == 0
        assert subtract_five_mins(4) == 0
        assert subtract_five_mins(6) == 1
        assert subtract_five_mins(1026) == 1021
        assert subtract_five_mins(1004) == 959

    def test_parse_stop_points(self):
        text='''
         <stoppoints>
            <stoppoint station_code="1" departure_time="13:00"/>
            <stoppoint station_code="1" departure_time="14:00"/>
            <stoppoint station_code="1" departure_time="00:00"/>
            <stoppoint station_code="1" departure_time="00:02"/>
            <stoppoint station_code="1" departure_time="09:09"/>
            <stoppoint station_code="1" arrival_time="14:50"/>
        </stoppoints>
        '''
        stoppoints_el = safe_xml_fromstring((text))
        stations = {'1' : TStationWithCodes()}
        messages = []
        parser = ApmParser()
        stoppoints, error = parser.get_stoppoints(stoppoints_el, '1', 'FL 1', stations, messages)
        assert not error, 'Error: {}'.format(messages)
        assert [s.__dict__ for s in stoppoints] == [
            Stoppoint('1', '', 1300, -1).__dict__,
            Stoppoint('1', '', 1400, 1355).__dict__,
            Stoppoint('1', '', 0, 0).__dict__,
            Stoppoint('1', '', 2, 0).__dict__,
            Stoppoint('1', '', 909, 904).__dict__,
            Stoppoint('1', '', -1, 1450).__dict__,
        ]

    def test_verify_flight_base(self):
        # valid flight base
        fb = ApmFlightBase()
        fb.scheduled_departure_time = 100
        fb.scheduled_arrival_time = 200
        assert ApmParser.verify_flight_base(fb, '123', 1) == []

        # invalid departure time
        fb = ApmFlightBase()
        fb.scheduled_departure_time = -1
        fb.scheduled_arrival_time = 200
        assert ApmParser.verify_flight_base(fb, '123', 1) == [
            'Error: Invalid or missing departure time for flight 123 segment 1',
        ]

        # invalid arrival time
        fb = ApmFlightBase()
        fb.scheduled_departure_time = 100
        fb.scheduled_arrival_time = 2500
        assert ApmParser.verify_flight_base(fb, '123', 1) == [
            'Error: Invalid or missing arrival time for flight 123 segment 1',
        ]

        # both departure and arrival times are invalid
        fb = ApmFlightBase()
        fb.scheduled_departure_time = 160
        fb.scheduled_arrival_time = 2400
        assert ApmParser.verify_flight_base(fb, '123', 1) == [
            'Error: Invalid or missing departure time for flight 123 segment 1',
            'Error: Invalid or missing arrival time for flight 123 segment 1',
        ]
