# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import mock
import pytest

from requests import ConnectionError, Timeout, HTTPError

from travel.avia.library.python.shared_flights_client.client import OperatingFlight
from travel.avia.ticket_daemon_api.jsonrpc.lib.operating_checker import OperatingFlightChecker, \
    OperatingFlightCheckException

AEROFLOT_AIRLINE_ID = 26
POBEDA_AIRLINE_ID = 9144


def raiser(exc_type):
    raise exc_type


class UnexpectedError(Exception):
    pass


@pytest.mark.parametrize(
    ['segment', 'airline_id', 'operating_flight', 'heuristics', 'expected'],
    [
        pytest.param(
            {'number': 'SU 1000'},
            POBEDA_AIRLINE_ID,
            lambda *args, **kwargs: OperatingFlight(POBEDA_AIRLINE_ID, '1000', 'SU 1000'),
            lambda *args, **kwargs: False,
            True,
            id='flight provider: OK, airline id: match => True'
        ),
        pytest.param(
            {'number': 'SU 1000'},
            POBEDA_AIRLINE_ID,
            lambda *args, **kwargs: OperatingFlight(AEROFLOT_AIRLINE_ID, '1000', 'SU 1000'),
            lambda *args, **kwargs: False,
            False,
            id='flight provider: OK, airline id: not match => False'
        ),
        pytest.param(
            {'number': 'SU 1000'},
            POBEDA_AIRLINE_ID,
            lambda *args, **kwargs: None,
            lambda *args, **kwargs: False,
            False,
            id='flight provider: None, heuristics: False => False'
        ),
        pytest.param(
            {'number': 'SU 1000'},
            POBEDA_AIRLINE_ID,
            lambda *args, **kwargs: None,
            lambda *args, **kwargs: True,
            True,
            id='flight provider: None, heuristics: True => True'
        ),
        pytest.param(
            {'number': 'SU 1000'},
            POBEDA_AIRLINE_ID,
            lambda *args, **kwargs: raiser(ConnectionError),
            lambda *args, **kwargs: False,
            False,
            id='flight provider: ConnectionError, heuristics: False => False'
        ),
        pytest.param(
            {'number': 'SU 1000'},
            POBEDA_AIRLINE_ID,
            lambda *args, **kwargs: raiser(Timeout),
            lambda *args, **kwargs: False,
            False,
            id='flight provider: Timeout, heuristics: False => False'
        ),
        pytest.param(
            {'number': 'SU 1000'},
            POBEDA_AIRLINE_ID,
            lambda *args, **kwargs: raiser(HTTPError),
            lambda *args, **kwargs: False,
            False,
            id='flight provider: HTTPError, heuristics: False => False'
        ),
        pytest.param(
            {'number': 'SU 1000'},
            POBEDA_AIRLINE_ID,
            lambda *args, **kwargs: raiser(ConnectionError),
            lambda *args, **kwargs: True,
            True,
            id='flight provider: ConnectionError, heuristics: True => True'
        ),
        pytest.param(
            {'number': 'SU 1000'},
            POBEDA_AIRLINE_ID,
            lambda *args, **kwargs: raiser(Timeout),
            lambda *args, **kwargs: True,
            True,
            id='flight provider: Timeout, heuristics: True => True'
        ),
        pytest.param(
            {'number': 'SU 1000'},
            POBEDA_AIRLINE_ID,
            lambda *args, **kwargs: raiser(HTTPError),
            lambda *args, **kwargs: True,
            True,
            id='flight provider: HTTPError, heuristics: True => True'
        ),
        pytest.param(
            {'number': 'SU 1000'},
            POBEDA_AIRLINE_ID,
            lambda *args, **kwargs: raiser(UnexpectedError),
            lambda *args, **kwargs: True,
            OperatingFlightCheckException,
            id='Any other exception in flight provider is unexpected => OperatingFlightCheckException'
        ),
        pytest.param(
            {'number': 'SU 1000'},
            POBEDA_AIRLINE_ID,
            lambda *args, **kwargs: None,
            lambda *args, **kwargs: raiser(UnexpectedError),
            OperatingFlightCheckException,
            id='Any other exception is unexpected => OperatingFlightCheckException'
        ),
    ]
)
def test_operating_checker(segment, operating_flight, airline_id, heuristics, expected):
    """
    :param segment: segment under test
    :param operating_flight: result from operating flight provider
    :param airline_id: airline id to check with result from operating flight provider
    :param heuristics: function to apply if operating flight provider returned None or failed
    :param expected: expected value to be returned from operating flight checker
    :return:
    """
    mock_flight_provider = mock.Mock()
    mock_flight_provider.map_segment_to_params.return_value = (mock.Mock(), mock.Mock(), mock.Mock())
    mock_flight_provider.get_operating_flight.side_effect = operating_flight
    ofc = OperatingFlightChecker(
        operating_flight_provider=mock_flight_provider,
        airline_id=airline_id,
        heuristics=heuristics
    )
    if type(expected) == type and issubclass(expected, Exception) or isinstance(expected, Exception):
        with pytest.raises(expected):
            ofc.is_operating(segment)
    else:
        assert expected == ofc.is_operating(segment)
