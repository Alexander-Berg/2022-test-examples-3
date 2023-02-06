import pytz
from datetime import date, datetime

import mock
import requests

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.avia_api.avia.lib.flight_info import WizardFlightInfo, WizardFlightStatus
from travel.avia.avia_api.avia.lib.shared_flights import shared_flights


class TestCheckinStartedStatus(TestCase):
    RESPONSE_SU_100 = {
        u'arrivalDay': u'2020-09-22',
        u'number': u'102',
        u'airlineCode': u'SU',
        u'airportToCode': u'JFK',
        u'airportFromCode': u'SVO',
        u'source': u'',
        u'departureUtc': u'2020-09-22 11:15:00',
        u'airportFromID': 9600213,
        u'departureDay': u'2020-09-22',
        u'status': {
            u'status': u'unknown',
            u'arrival': u'',
            u'createdAtUtc': u'2020-09-21 11:16:17',
            u'arrivalTerminal': u'',
            u'baggageCarousels': u'',
            u'arrivalStatus': u'unknown',
            u'arrivalUpdatedAtUtc': u'',
            u'divertedAirportCode': u'',
            u'checkInDesks': u'',
            u'departure': u'2020-09-22 14:26:27',
            u'divertedAirportID': 0,
            u'departureStatus': u'departed',
            u'departureGate': u'25',
            u'departureTerminal': u'D',
            u'departureUpdatedAtUtc': u'2020-09-22 11:43:17',
            u'updatedAtUtc': u'2020-09-22 11:43:17',
            u'diverted': False,
            u'departureSource': u'3',
            u'arrivalGate': u'',
            u'arrivalSource': u'3',
        },
        u'arrivalTerminal': u'1',
        u'airlineID': 26,
        u'departureTerminal': u'D',
        u'arrivalTime': u'17:30:00',
        u'updatedAtUtc': u'2020-09-15 11:51:52',
        u'departureTime': u'14:15:00',
        u'createdAtUtc': u'2020-09-15 11:51:52',
        u'departureTimezone': u'Europe/Moscow',
        u'airportToID': 9600371,
        u'arrivalTimezone': u'America/New_York',
        u'title': u'SU 102',
        u'arrivalUtc': u'2020-09-22 21:30:00',
    }

    @staticmethod
    def mock_response(data):
        # type: (dict) -> requests.Response
        response_json_mock = mock.Mock()
        response_json_mock.return_value = data

        response = requests.Response()
        response.status_code = 200
        response.request = requests.PreparedRequest()
        response.json = response_json_mock

        return response

    def test_flight(self):
        shared_flights._client._session.get = mock.Mock()
        shared_flights._client._session.get.return_value = self.mock_response(self.RESPONSE_SU_100)

        flight = shared_flights.get_flight('', date(2020, 9, 22))
        assert flight is None

        flight = shared_flights.get_flight(None, date(2020, 9, 22))
        assert flight is None

        flight = shared_flights.get_flight('SU 100', date(2020, 9, 22))

        assert isinstance(flight, WizardFlightInfo)

        assert flight.number == '102'
        assert flight.scheduled_departure_datetime == pytz.timezone('Europe/Moscow').localize(
            datetime.strptime('2020-09-22 14:15:00', '%Y-%m-%d %H:%M:%S'),
        )
        assert flight.scheduled_arrival_datetime == pytz.timezone('America/New_York').localize(
            datetime.strptime('2020-09-22 17:30:00', '%Y-%m-%d %H:%M:%S'),
        )
        assert flight.real_departure_datetime == pytz.timezone('Europe/Moscow').localize(
            datetime.strptime('2020-09-22 14:26:27', '%Y-%m-%d %H:%M:%S'),
        )
        assert flight.company_id == 26
        assert flight.from_station_id == 9600213
        assert flight.to_station_id == 9600371
        assert flight.status == WizardFlightStatus.Unknown
