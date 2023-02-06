import pytest
import re

from requests_mock import ANY

from travel.avia.flight_status_registrar.lib.flights_fetcher import Flight
from travel.avia.flight_status_registrar.oag.lib.registrar import OAGRegistrar, OAGError, OAGRegistrationError


DEPARTURE_ANY = re.compile('.+?/departure/.+?')
ARRIVAL_ANY = re.compile('.+?/arrival/.+?')
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


def test_oag_registration_check_requests(two_recent_requests):
    flight = MOCK_FLIGHT

    registrar = OAGRegistrar('mock', 'mock')

    registrar.register_flight(
        Flight(flight),
    )
    expected_departure = 'https://fnb.flightview.com/register/departure/TSE/IQ/367/2020/04/10/0840'
    expected_arrival = 'https://fnb.flightview.com/register/arrival/KSN/IQ/367/2020/04/10/1005'
    actual_requests = two_recent_requests()

    assert actual_requests[0].json() == actual_requests[1].json()
    assert actual_requests[0].json() == {'RequestParameters': {'appid': 'mock', 'appkey': 'mock'}}

    assert list(map(lambda r: r.url, actual_requests)) == [expected_departure, expected_arrival]


def test_oag_registration_fatal_error_by_body(requests_mock, mock_flight):
    requests_mock.post(ANY, json={
        'Success': False,
        'Error': 'Invalid appid/appkey combination'
    })
    with pytest.raises(RuntimeError):
        registrar = OAGRegistrar('mock', 'mock')
        registrar.register_flight(
            Flight(mock_flight),
        )


@pytest.mark.parametrize('matcher', [DEPARTURE_ANY, ARRIVAL_ANY, ANY])
def test_oag_registration_simple_error_by_body(requests_mock, mock_flight, matcher):
    requests_mock.post(matcher, json={
        'Success': False,
        'Error': 'Flight not Found'
    })
    _assert_oag_error(mock_flight, matcher)


@pytest.mark.parametrize('matcher', [DEPARTURE_ANY, ARRIVAL_ANY, ANY])
def test_oag_registration_bad_response(requests_mock, mock_flight, matcher):
    requests_mock.post(matcher, text='{')
    _assert_oag_error(mock_flight, matcher)

    requests_mock.post(matcher, text='{}')
    _assert_oag_error(mock_flight, matcher)


@pytest.mark.parametrize('status_code', [400, 401, 500, 502])
def test_oag_registration_fatal_status_code(requests_mock, mock_flight, status_code):
    requests_mock.post(ANY, status_code=status_code)
    with pytest.raises(RuntimeError):
        registrar = OAGRegistrar('mock', 'mock')
        registrar.register_flight(
            Flight(mock_flight),
        )


@pytest.mark.parametrize('matcher', [DEPARTURE_ANY, ARRIVAL_ANY, ANY])
def test_oag_registration_simple_error_by_status_code(
    requests_mock, mock_flight, matcher
):
    requests_mock.post(matcher, status_code=408)
    _assert_oag_error(mock_flight, matcher)


@pytest.fixture()
def mock_flight():
    return MOCK_FLIGHT


@pytest.fixture(autouse=True)
def mock_any_url(requests_mock):
    requests_mock.post(ANY, json={'Success': True, 'Error': ''})


@pytest.fixture()
def two_recent_requests(requests_mock):
    history = requests_mock.request_history

    def getter():
        if len(history) > 1:
            return history[-2], history[-1]

        return None, None

    return getter


def _assert_oag_error(mock_flight, matcher):
    try:
        registrar = OAGRegistrar('mock', 'mock')
        registrar.register_flight(
            Flight(mock_flight),
        )
    except OAGError as e:
        if matcher is DEPARTURE_ANY:
            assert isinstance(e.departure_exception, OAGRegistrationError)
            assert e.arrival_exception is None
        elif matcher is ARRIVAL_ANY:
            assert e.departure_exception is None
            assert isinstance(e.arrival_exception, OAGRegistrationError)
        else:
            assert isinstance(e.departure_exception, OAGRegistrationError)
            assert isinstance(e.arrival_exception, OAGRegistrationError)
    else:
        assert False
