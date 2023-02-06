# coding: utf-8
from __future__ import unicode_literals

import pytest
from datetime import date

from requests import HTTPError

from travel.avia.library.python.shared_flights_client.client import OperatingFlight
from travel.avia.library.python.ticket_daemon.caches.operating_flights.cache import (
    OperatingFlightProvider,
    _operating_flight_deserializer,
    _operating_flight_serializer,
)

AEROFLOT_ID = 26


def test_params_mapper():
    segment = {'number': 'SU 5010', 'departure': {'local': '2020-01-01T01:02:03Z'}}
    assert ('SU', '5010', date(2020, 1, 1)) == OperatingFlightProvider.map_segment_to_params(segment)


@pytest.mark.parametrize(
    "original",
    [
        pytest.param(OperatingFlight(airlineID=AEROFLOT_ID, number='1234', title='SU 1234'), id="filled"),
        pytest.param(OperatingFlight(airlineID=None, number='1234', title='SU 1234'), id="empty airline ID"),
        pytest.param(OperatingFlight(airlineID=AEROFLOT_ID, number=None, title='SU 1234'), id="empty number"),
        pytest.param(OperatingFlight(airlineID=AEROFLOT_ID, number='1234', title=None), id="empty title"),
        pytest.param(None, id="None value"),
    ],
)
def test_operating_flight_serializer_deserializer(original):
    assert original == _operating_flight_deserializer(_operating_flight_serializer(original))


def raiser(exc_type):
    def fn(*args, **kwargs):
        raise exc_type

    return fn


class AnyError(Exception):
    pass


@pytest.mark.parametrize(
    ["call_params", "get_operating_flight_result_fn", "expected", "exc"],
    [
        pytest.param(
            ("SU", "1000", date(2021, 1, 1),),
            lambda *args, **kwargs: OperatingFlight(26, "1000", "SU 1000"),
            OperatingFlight(26, "1000", "SU 1000"),
            None,
            id="SF:OK,operating"
        ),
        pytest.param(
            ("SU", "1001", date(2021, 1, 1),),
            lambda *args, **kwargs: OperatingFlight(6511, "2000", "DP 2000"),
            OperatingFlight(6511, "2000", "DP 2000"),
            None,
            id="SF:OK,codeshare"
        ),
        pytest.param(
            ("SU", "1002", date(2021, 1, 1),),
            raiser(HTTPError),
            None,
            HTTPError,
            id="SF:failed;passthrough exception 1"
        ),
        pytest.param(
            ("SU", "1003", date(2021, 1, 1),),
            raiser(AnyError),
            None,
            AnyError,
            id="SF:failed;passthrough exception 2"
        ),
    ]
)
def test_operating_flight_provider(call_params, get_operating_flight_result_fn, expected, exc):
    daemon_cache_dict = {}

    class MockSharedFlightsClient(object):
        def get_operating_flight(self, *args, **kwargs):
            return get_operating_flight_result_fn(*args, **kwargs)

    class MockDaemonCache(object):
        def set(self, key, value, timeout=None):
            daemon_cache_dict[key] = value

        def get(self, key):
            return daemon_cache_dict[key]

    ofp = OperatingFlightProvider(MockSharedFlightsClient(), MockDaemonCache())
    if exc:
        with pytest.raises(exc):
            ofp.get_operating_flight(*call_params)
    else:
        operating_flight = ofp.get_operating_flight(*call_params)
        assert expected == operating_flight


def test_operating_flight_provided_cache():
    class MockSharedFlightsClient(object):
        client_call_counter = 0

        def get_operating_flight(self, *args, **kwargs):
            MockSharedFlightsClient.client_call_counter += 1
            return {
                ("SU", "1234", date(2021, 1, 1)): OperatingFlight(1, 2, 3)
            }.get(args)

    daemon_cache_dict = {}

    class MockDaemonCache(object):
        def set(self, key, value, timeout=None):
            daemon_cache_dict[key] = value

        def get(self, key):
            return daemon_cache_dict[key]

    ofp = OperatingFlightProvider(MockSharedFlightsClient(), MockDaemonCache())

    assert MockSharedFlightsClient.client_call_counter == 0
    assert len(daemon_cache_dict) == 0

    # lets request something and check client call counter
    operating_flight = ofp.get_operating_flight("SU", "1234", date(2021, 1, 1))
    assert MockSharedFlightsClient.client_call_counter == 1
    # check result just to make sure all OK
    assert OperatingFlight(1, 2, 3) == operating_flight
    # lets also check daemon cache dict size
    assert len(daemon_cache_dict) == 1

    # now lets request it again
    operating_flight = ofp.get_operating_flight("SU", "1234", date(2021, 1, 1))
    # should not have called client directly
    assert MockSharedFlightsClient.client_call_counter == 1
    assert OperatingFlight(1, 2, 3) == operating_flight
    assert len(daemon_cache_dict) == 1

    # lets also check that memoize has priority by clearing "remote" cache
    daemon_cache_dict.clear()
    assert len(daemon_cache_dict) == 0
    operating_flight = ofp.get_operating_flight("SU", "1234", date(2021, 1, 1))
    assert MockSharedFlightsClient.client_call_counter == 1
    assert OperatingFlight(1, 2, 3) == operating_flight
