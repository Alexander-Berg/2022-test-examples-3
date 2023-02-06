from travel.avia.flight_status_registrar.lib.flights_fetcher import Flight
from travel.avia.flight_status_registrar.variflight.lib.pipeline import variflight_pipeline
from travel.avia.flight_status_registrar.variflight.lib.registrar import VariFlightRegistrationError

FETCHER_METHOD = 'travel.avia.flight_status_registrar.lib.flights_fetcher.PopularFlightsFetcher.get_flights'

REGISTER_METHOD = 'travel.avia.flight_status_registrar.variflight.lib.registrar.VariFlightRegistrar.register_flight'


def create_flight(**kwargs):
    data = {
        'airlineCode': '',
        'number': '',
        'airportFromID': '',
        'airportFromCode': '',
        'airportToID': '',
        'airportToCode': '',
        'departureDay': '',
        'departureTime': '',
        'arrivalDay': '',
        'arrivalTime': '',
        'operating': None,
    }
    data.update(kwargs)

    return Flight(data)


def test_run_pipeline_abort(mocker):
    flight1 = create_flight(number=1)
    flight2 = create_flight(number=2)
    flight3 = create_flight(number=3)
    flight4 = create_flight(number=4)
    mocker.patch(FETCHER_METHOD, return_value=[
        flight1, flight2,
        flight3, flight4,
    ])
    mocked_registrar = mocker.patch(
        REGISTER_METHOD,
        side_effect=(None, None, RuntimeError)
    )

    try:
        variflight_pipeline('appid', 'appkey', 'proxy',
                            None, None, None, None,
                            None, None, None, 5)
    except RuntimeError:
        pass

    assert mocked_registrar.call_args_list == [
        mocker.call(flight1),
        mocker.call(flight2),
        mocker.call(flight3),
    ]


def test_run_pipeline_skip_not_fatal_error(mocker):
    flight1 = create_flight(number=1)
    flight2 = create_flight(number=2)
    flight3 = create_flight(number=3)
    flight4 = create_flight(number=4)
    mocker.patch(FETCHER_METHOD, return_value=[
        flight1, flight2,
        flight3, flight4,
    ])
    mocked_registrar = mocker.patch(
        REGISTER_METHOD,
        side_effect=(None, None, VariFlightRegistrationError, None)
    )
    variflight_pipeline('appid', 'appkey', 'proxy',
                        None, None, None, None,
                        None, None, None, 5)
    assert mocked_registrar.call_args_list == [
        mocker.call(flight1),
        mocker.call(flight2),
        mocker.call(flight3),
        mocker.call(flight4),
    ]


def test_run_pipeline_flights_limit(mocker):
    flight1 = create_flight(number=1)
    flight2 = create_flight(number=2)
    flight3 = create_flight(number=3)
    flight4 = create_flight(number=4)
    mocker.patch(FETCHER_METHOD, return_value=[
        flight1, flight2,
        flight3, flight4,
    ])
    mocked_registrar = mocker.patch(
        REGISTER_METHOD,
        side_effect=(None, VariFlightRegistrationError, None, None)
    )
    variflight_pipeline('appid', 'appkey', 'proxy',
                        None, None, None, None,
                        None, None, None, 2)
    assert mocked_registrar.call_args_list == [
        mocker.call(flight1),
        mocker.call(flight2),
        mocker.call(flight3),
    ]
