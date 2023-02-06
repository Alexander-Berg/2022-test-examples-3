# -*- coding: utf-8 -*-
from datetime import date

import mock

from travel.avia.ticket_daemon.ticket_daemon.partners import azimuth2
from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, get_query, assert_variants_equal,
    expected_variants, SettlementMock
)


@mock.patch('requests.post', return_value=get_mocked_response('azimuth2_one_way.xml'))
def test_azimuth2_one_way_query(*mocks):
    expected = expected_variants('azimuth2_one_way.json')
    test_query = get_query(
        point_to=SettlementMock(iata='ROV', code='RU', id=39),
        date_forward=date(2021, 11, 19),
    )
    variants = next(azimuth2.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch('requests.post', return_value=get_mocked_response('azimuth2_return.xml'))
def test_azimuth2_return_query(*mocks):
    expected = expected_variants('azimuth2_return.json')
    test_query = get_query(
        point_to=SettlementMock(iata='ROV', code='RU', id=39),
        date_forward=date(2021, 10, 24),
        date_backward=date(2021, 10, 27),
    )
    variants = next(azimuth2.query(test_query))

    assert_variants_equal(expected, variants)


@mock.patch('requests.post', return_value=get_mocked_response('azimuth2_return_connection.xml'))
def test_azimuth2_return_connection_query(*mocks):
    expected = expected_variants('azimuth2_return_connection.json')
    test_query = get_query(
        point_from=SettlementMock(iata='MOW', code='RU', id=213),
        point_to=SettlementMock(iata='SVX', code='RU', id=54),
        date_forward=date(2021, 11, 25),
        date_backward=date(2021, 12, 22),
    )
    variants = next(azimuth2.query(test_query))

    assert_variants_equal(expected, variants)


@mock.patch('requests.post', return_value=get_mocked_response('azimuth2_multi_round.xml'))
def test_azimuth2_multi_round_query(*mocks):
    expected = expected_variants('azimuth2_multi_round.json')
    test_query = get_query(
        point_from=SettlementMock(iata='MOW', code='RU', id=213),
        point_to=SettlementMock(iata='SVX', code='RU', id=54),
        date_forward=date(2021, 11, 25),
        date_backward=date(2021, 12, 22),
    )
    variants = next(azimuth2.query(test_query))

    assert_variants_equal(expected, variants)


@mock.patch('requests.post', return_value=get_mocked_response('azimuth2_bad_child_only.xml'))
def test_azimuth2_bad_child_only_query(*mocks):
    expected = expected_variants('azimuth2_bad_child_only.json')
    test_query = get_query(
        point_from=SettlementMock(iata='MOW', code='RU', id=213),
        point_to=SettlementMock(iata='SVX', code='RU', id=54),
        date_forward=date(2021, 11, 25),
        date_backward=date(2021, 12, 22),
    )
    variants = next(azimuth2.query(test_query))

    assert_variants_equal(expected, variants)


@mock.patch('requests.post', return_value=get_mocked_response('azimuth2_bad_no_fares.xml'))
def test_azimuth2_bad_no_fares(*mocks):
    expected = expected_variants('azimuth2_bad_no_fares.json')
    test_query = get_query(
        point_from=SettlementMock(iata='MOW', code='RU', id=213),
        point_to=SettlementMock(iata='SVX', code='RU', id=54),
        date_forward=date(2021, 11, 25),
        date_backward=date(2021, 12, 22),
    )
    variants = next(azimuth2.query(test_query))

    assert_variants_equal(expected, variants)


@mock.patch('requests.post', return_value=get_mocked_response('azimuth2_bad_fares_structure.xml'))
def test_azimuth2_bad_fares_structure(*mocks):
    expected = expected_variants('azimuth2_bad_fares_structure.json')
    test_query = get_query(
        point_from=SettlementMock(iata='MOW', code='RU', id=213),
        point_to=SettlementMock(iata='SVX', code='RU', id=54),
        date_forward=date(2021, 11, 25),
        date_backward=date(2021, 12, 22),
    )
    variants = next(azimuth2.query(test_query))

    assert_variants_equal(expected, variants)
