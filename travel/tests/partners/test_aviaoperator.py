# -*- coding: utf-8 -*-
import datetime
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal,
    SettlementMock
)
from travel.avia.ticket_daemon.ticket_daemon.partners import aviaoperator2


@mock.patch('requests.get', return_value=get_mocked_response('aviaoperator2.xml'))
def test_aviaoperator2_query(mocked_request):
    expected = expected_variants('aviaoperator2.json')
    test_query = get_query(
        point_to=SettlementMock(iata='POP', code=None, id=26683),
        date_forward=datetime.date(2017, 1, 24),
        date_backward=None,
    )
    variants = next(aviaoperator2.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch('requests.get', return_value=get_mocked_response('aviaoperator2_baggage.xml'))
def test_aviaoperator2_baggage_query(mocked_request):
    expected = expected_variants('aviaoperator2_baggage.json')
    variants = next(aviaoperator2.query(get_query()))
    assert_variants_equal(expected, variants)
