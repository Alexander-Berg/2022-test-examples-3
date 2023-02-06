# -*- coding: utf-8 -*-
import logging
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    get_content, get_query, SettlementMock, assert_variants_equal, expected_variants
)
from travel.avia.ticket_daemon.ticket_daemon.partners import redwings

log = logging.getLogger(__name__)


def get_mock_sirena_client_class(data):
    class MockSirenaClientClass:
        def __init__(self, *args, **kwargs):
            log.info('mock_sirena_client_class.__init__ (args: %s, kwargs: %s)', args, kwargs)

        def get(self, *args, **kwargs):
            log.info('mock_sirena_client_class.get (args: %s, kwargs: %s)', args, kwargs)
            return data

        def get_company_routes(self, *args, **kwargs):
            log.info('mock_sirena_client_class.get_company_routes (args: %s, kwargs: %s)', args, kwargs)
            return data

        def close(self):
            pass

    def factory(*args, **kwargs):
        def create_sirena_client():
            return MockSirenaClientClass(*args, **kwargs)

        return create_sirena_client

    return factory


@mock.patch.object(redwings, 'sirena_client_factory',
                   get_mock_sirena_client_class(get_content('redwings_ow_response.xml')))
def test_redwings_one_way_query():
    expected = expected_variants('redwings_ow.json')
    test_query = get_query(
        date_backward=None,
        passengers={'adults': 1, 'children': 0, 'infants': 0},
        point_to=SettlementMock(iata='AER', code='RU', id=239),
    )
    variants = next(redwings.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch.object(redwings, 'sirena_client_factory',
                   get_mock_sirena_client_class(get_content('redwings_return_321_response.xml')))
def test_redwings_return_321_query():
    '''
    Multi-passenger round-trip test
    '''
    expected = expected_variants('redwings_return_321.json')
    test_query = get_query(
        passengers={'adults': 3, 'children': 2, 'infants': 1},
        point_from=SettlementMock(iata='MOW', code='RU', id=213),
        point_to=SettlementMock(iata='AAQ', code='RU', id=1107),
    )
    variants = next(redwings.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch.object(redwings, 'sirena_client_factory',
                   get_mock_sirena_client_class(get_content('redwings_return_rtw_response.xml')))
def test_redwings_return_rtw_query():
    '''
    Tests verifies that we sort things out (origins, destinations) properly,
    even when data in the response is sorted by segment's price
    '''
    expected = expected_variants('redwings_return_rtw.json')
    test_query = get_query(
        passengers={'adults': 1, 'children': 0, 'infants': 0},
        point_from=SettlementMock(iata='SVX', code='RU', id=54),
        point_to=SettlementMock(iata='RTW', code='RU', id=194),
    )
    variants = next(redwings.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch.object(redwings, 'sirena_client_factory',
                   get_mock_sirena_client_class(get_content('redwings_return_svx_response.xml')))
def test_redwings_return_svx_query():
    '''
    Round-trip response has an extra segment in the "Econom" brand that should be disposed,
    since there's no corresponding "Econom" segment on the return slice.
    '''
    expected = expected_variants('redwings_return_svx.json')
    test_query = get_query(
        passengers={'adults': 1, 'children': 0, 'infants': 0},
        point_from=SettlementMock(iata='RTW', code='RU', id=194),
        point_to=SettlementMock(iata='SVX', code='RU', id=54),
    )
    variants = next(redwings.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch.object(redwings, 'sirena_client_factory',
                   get_mock_sirena_client_class(get_content('redwings_empty_response.xml')))
def test_redwings_empty_response():
    test_query = get_query(
        passengers={'adults': 3, 'children': 2, 'infants': 1},
        point_from=SettlementMock(iata='SVX', code='RU', id=54),
        point_to=SettlementMock(iata='AER', code='RU', id=239),
    )
    variants = list(next(redwings.query(test_query)).variants)
    assert len(variants) == 0
