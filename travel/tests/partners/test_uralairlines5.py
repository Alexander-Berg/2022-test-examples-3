# -*- coding: utf-8 -*-
from datetime import date

import mock

from travel.avia.ticket_daemon.ticket_daemon.partners import uralairlines5 as uralairlines5
from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, get_query, assert_variants_equal,
    expected_variants, SettlementMock,
)


@mock.patch('requests.post', return_value=get_mocked_response('uralairlines5_oneway.xml'))
def test_uralairlines5_oneway_query(*mocks):
    expected = expected_variants('uralairlines5_oneway.json')
    test_query = get_query(
        point_to=SettlementMock(iata='LED', code='RU', id=2),
        date_forward=date(2020, 4, 5),
    )
    variants = next(uralairlines5.query(test_query))

    assert_variants_equal(expected, variants)


@mock.patch('requests.post', return_value=get_mocked_response('uralairlines5_return.xml'))
def test_uralairlines5_return_query(*mocks):
    expected = expected_variants('uralairlines5_return.json')
    test_query = get_query(
        point_to=SettlementMock(iata='LED', code='RU', id=2),
        date_forward=date(2020, 4, 5),
        date_backward=date(2019, 5, 10),
    )
    variants = next(uralairlines5.query(test_query))

    assert_variants_equal(expected, variants)
