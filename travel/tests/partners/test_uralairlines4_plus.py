# -*- coding: utf-8 -*-
from datetime import date

import mock

from travel.avia.ticket_daemon.ticket_daemon.partners import uralairlines4_plus as uralairlines4
from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, get_query, assert_variants_equal,
    expected_variants, SettlementMock,
)


@mock.patch('requests.post', return_value=get_mocked_response('uralairlines4_oneway.xml'))
def test_uralairlines4_oneway_query(*mocks):
    expected = expected_variants('uralairlines4_oneway_plus.json')
    test_query = get_query(
        point_to=SettlementMock(iata='LED', code='RU', id=2),
        date_forward=date(2020, 4, 5),
    )
    variants = next(uralairlines4.query(test_query))

    assert_variants_equal(expected, variants)


@mock.patch('requests.post', return_value=get_mocked_response('uralairlines4_return_plus.xml'))
def test_uralairlines4_return_query(*mocks):
    expected = expected_variants('uralairlines4_return_plus.json')
    test_query = get_query(
        point_to=SettlementMock(iata='LED', code='RU', id=2),
        date_forward=date(2020, 4, 5),
        date_backward=date(2019, 5, 10),
    )
    variants = next(uralairlines4.query(test_query))

    assert_variants_equal(expected, variants)
