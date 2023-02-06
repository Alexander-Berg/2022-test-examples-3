import pytest

from requests import HTTPError
from requests_mock import ANY

from travel.avia.flight_status_registrar.lib.flights_fetcher import Flight
from travel.avia.flight_status_registrar.variflight.lib.registrar import (
    VariFlightRegistrar, VariFlightRegistrationError,
)


MOCK_FLIGHT = {
    u'departureTimezone': u'Asia/Almaty',
    u'airportFromCode': u'TSE',
    u'arrivalTime': u'10:05:00',
    u'createdAtUtc': u'2019-12-31 07:20:28',
    u'arrivalTimezone': u'Asia/Almaty',
    u'number': u'367',
    u'arrivalUtc': u'2020-04-10 04:05:00',
    u'airportToID': 9623914,
    u'updatedAtUtc': u'2020-04-07 10:32:01',
    u'segments': [],
    u'departureTime': u'08:40:00',
    u'departureDay': u'2020-04-10',
    u'airportFromID': 9623556, u'airline_id': 57734,
    u'airportToCode': u'KSN',
    u'departureUtc': u'2020-04-10 02:40:00',
    u'airlineCode': u'IQ',
    u'arrivalDay': u'2020-04-10',
    u'operating': None,
}


def test_variflight_registration_check_requests(requests_mock):
    flight = MOCK_FLIGHT
    registrar = VariFlightRegistrar('http://mock', 'mock', 'mock')
    registrar.register_flight(
        Flight(flight),
    )

    history = requests_mock.request_history

    assert len(history) == 1
    assert history[0].url.startswith(
        'http://mock/api/addflightpush/?appid=mock'
        '&arr=KSN&date=2020-04-10&dep=TSE&fnum=IQ367'
        '&lang=en'
        '&token='
    )


@pytest.mark.parametrize('error_code', (0, 1, 2, 3, 4, 7, 11))
def test_variflight_fatal_error(requests_mock, mock_flight, error_code):
    requests_mock.get(ANY, json={
        'error_code': error_code,
        'error': '',
    })
    with pytest.raises(RuntimeError):
        registrar = VariFlightRegistrar('http://mock', 'mock', 'mock')
        registrar.register_flight(
            Flight(mock_flight),
        )


@pytest.mark.parametrize('error_code', (10, 5))
def test_variflight_registration_error(requests_mock, mock_flight, error_code):
    requests_mock.get(ANY, json={
        'error_code': error_code,
        'error': '',
    })
    with pytest.raises(VariFlightRegistrationError):
        registrar = VariFlightRegistrar('http://mock', 'mock', 'mock')
        registrar.register_flight(
            Flight(mock_flight),
        )


@pytest.mark.parametrize('error_code', (6, 8))
def test_variflight_success_code(requests_mock, mock_flight, error_code):
    requests_mock.get(ANY, json={
        'error_code': error_code,
        'error': '',
    })
    registrar = VariFlightRegistrar('http://mock', 'mock', 'mock')
    registrar.register_flight(
        Flight(mock_flight),
    )


@pytest.mark.parametrize('status_code', [400, 401, 500, 502])
def test_variflight_registration_fatal_status_code(requests_mock, mock_flight, status_code):
    requests_mock.get(ANY, status_code=status_code)
    with pytest.raises(HTTPError):
        registrar = VariFlightRegistrar('http://mock', 'mock', 'mock')
        registrar.register_flight(
            Flight(mock_flight),
        )


@pytest.fixture()
def mock_flight():
    return MOCK_FLIGHT


@pytest.fixture(autouse=True)
def mock_any_url(requests_mock):
    requests_mock.get(ANY, json={'error_code': 8, 'error': ''})
