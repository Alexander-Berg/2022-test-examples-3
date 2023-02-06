# -*- coding: utf-8 -*-
import datetime
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal,
    SettlementMock
)
from travel.avia.ticket_daemon.ticket_daemon.partners import kupibilet


@mock.patch('requests.get', return_value=get_mocked_response('kupibilet.xml'))
def test_kupibilet_query(mocked_request):
    expected = expected_variants('kupibilet.json')
    test_query = get_query(
        point_to=SettlementMock(iata='SVO', code='RU', id=2),
        date_forward=datetime.date(2017, 10, 10),
        date_backward=datetime.date(2017, 3, 24),
    )
    variants = next(kupibilet.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch('requests.get', return_value=get_mocked_response('kupibilet_bad_baggage.xml'))
def test_kupibilet_query_bad_baggage(mocked_request):
    expected = expected_variants('kupibilet_bad_baggage_expected.json')
    test_query = get_query(
        point_to=SettlementMock(iata='SVO', code='RU', id=2),
        date_forward=datetime.date(2017, 10, 10),
        date_backward=datetime.date(2017, 3, 24),
    )
    variants = next(kupibilet.query(test_query))
    assert_variants_equal(expected, variants)
