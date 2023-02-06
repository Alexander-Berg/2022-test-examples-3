# -*- coding: utf-8 -*-
from datetime import date

import mock

from travel.avia.ticket_daemon.ticket_daemon.partners import azimuth
from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, get_query, assert_variants_equal,
    expected_variants, SettlementMock
)


@mock.patch('requests.post', return_value=get_mocked_response('azimuth_one_way.xml'))
def test_azimuth_one_way_query(*mocks):
    expected = expected_variants('azimuth_oneway.json')
    test_query = get_query(
        point_to=SettlementMock(iata='ROV', code='RU', id=39),
        date_forward=date(2019, 11, 9),
    )
    variants = next(azimuth.query(test_query))

    assert_variants_equal(expected, variants)


@mock.patch('requests.post', return_value=get_mocked_response('azimuth_return.xml'))
def test_azimuth_return_query(*mocks):
    expected = expected_variants('azimuth_return.json')
    test_query = get_query(
        point_to=SettlementMock(iata='ROV', code='RU', id=39),
        date_forward=date(2019, 11, 9),
        date_backward=date(2019, 11, 19)
    )
    variants = next(azimuth.query(test_query))

    assert_variants_equal(expected, variants)
