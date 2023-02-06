from travel.avia.flight_status_registrar.lib.flights_fetcher import Flight
from travel.avia.flight_status_registrar.oag.lib.pipeline import oag_pipeline
from travel.avia.flight_status_registrar.oag.lib.registrar import OAGError

FETCHER_METHOD = 'travel.avia.flight_status_registrar.lib.flights_fetcher.PopularFlightsFetcher.get_flights'

REGISTER_METHOD = 'travel.avia.flight_status_registrar.oag.lib.registrar.OAGRegistrar.register_flight'


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
    flight2 = create_flight(number=1)
    flight3 = create_flight(number=1)
    flight4 = create_flight(number=1)
    mocker.patch(FETCHER_METHOD, return_value=[
        flight1, flight2,
        flight3, flight4,
    ])
    mocked_registrar = mocker.patch(
        REGISTER_METHOD,
        side_effect=(None, None, RuntimeError)
    )

    try:
        oag_pipeline('appid', 'appkey',
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
    flight2 = create_flight(number=1)
    flight3 = create_flight(number=1)
    flight4 = create_flight(number=1)
    mocker.patch(FETCHER_METHOD, return_value=[
        flight1, flight2,
        flight3, flight4,
    ])
    mocked_registrar = mocker.patch(
        REGISTER_METHOD,
        side_effect=(None, None, OAGError, None)
    )
    oag_pipeline('appid', 'appkey',
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
    flight2 = create_flight(number=1)
    flight3 = create_flight(number=1)
    flight4 = create_flight(number=1)
    mocker.patch(FETCHER_METHOD, return_value=[
        flight1, flight2,
        flight3, flight4,
    ])
    mocked_registrar = mocker.patch(
        REGISTER_METHOD,
        side_effect=(None, OAGError, None, None)
    )
    oag_pipeline('appid', 'appkey',
                 None, None, None, None,
                 None, None, None, 2)
    assert mocked_registrar.call_args_list == [
        mocker.call(flight1),
        mocker.call(flight2),
        mocker.call(flight3),
    ]


def test_operating_flight():
    flight1 = create_flight(number='1', airlineCode='AA')
    flight2 = create_flight(number='3', airlineCode='BB', operating={'title': 'AA 1'})
    assert repr(flight1) == repr(flight2)
