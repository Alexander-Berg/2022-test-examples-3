# -*- coding: utf-8 -*-
from datetime import date

import mock

from travel.avia.ticket_daemon.ticket_daemon.partners import uralairlines3
from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, get_query, assert_variants_equal,
    expected_variants, SettlementMock,
)


@mock.patch('requests.post', return_value=get_mocked_response('uralairlines3_oneway.xml'))
def test_uralairlines3_oneway_query(*mocks):
    expected = expected_variants('uralairlines3_oneway.json')
    test_query = get_query(
        point_to=SettlementMock(iata='LED', code='RU', id=2),
        date_forward=date(2020, 4, 5),
    )
    variants = next(uralairlines3.query(test_query))

    assert_variants_equal(expected, variants)


@mock.patch('requests.post', return_value=get_mocked_response('uralairlines3_return.xml'))
def test_uralairlines3_return_query(*mocks):
    expected = expected_variants('uralairlines3_return.json')
    test_query = get_query(
        point_to=SettlementMock(iata='LED', code='RU', id=2),
        date_forward=date(2020, 4, 5),
        date_backward=date(2019, 5, 10),
    )
    variants = next(uralairlines3.query(test_query))

    assert_variants_equal(expected, variants)
