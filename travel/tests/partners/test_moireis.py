# -*- coding: utf-8 -*-
import datetime
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal,
    SettlementMock
)
from travel.avia.ticket_daemon.ticket_daemon.partners import moireis


@mock.patch('requests.post', return_value=get_mocked_response('moireis.xml'))
def test_moireis_query(mocked_request):
    expected = expected_variants('moireis.json')
    test_query = get_query(
        point_from=SettlementMock(iata='SVX', code='RU', id=2),
        point_to=SettlementMock(iata='JFK', code='RU', id=2),
        date_forward=datetime.date(2018, 3, 10),
        date_backward=datetime.date(2018, 3, 24),
    )
    variants = next(moireis.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch('requests.post', return_value=get_mocked_response('moireis_no_baggage.xml'))
def test_moireis_query_1(mocked_request):
    expected = expected_variants('moireis_no_baggage.json')
    test_query = get_query(
        point_from=SettlementMock(iata='SVX', code='RU', id=2),
        point_to=SettlementMock(iata='JFK', code='RU', id=2),
        date_forward=datetime.date(2018, 3, 10),
        date_backward=datetime.date(2018, 3, 24),
    )
    variants = next(moireis.query(test_query))
    assert_variants_equal(expected, variants)
