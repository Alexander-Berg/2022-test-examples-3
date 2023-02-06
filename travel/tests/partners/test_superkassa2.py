# -*- coding: utf-8 -*-
import datetime
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, get_query, assert_variants_equal,
    SettlementMock, expected_variants
)
from travel.avia.ticket_daemon.ticket_daemon.partners import superkassa2


@mock.patch('requests.get', return_value=get_mocked_response('superkassa2.xml'))
def test_superkassa_query(mocked_request):
    expected = expected_variants('superkassa2.json')
    test_query = get_query(
        point_to=SettlementMock(iata='LED', code='RU', id=2),
        date_forward=datetime.date(2017, 3, 21),
        date_backward=datetime.date(2017, 3, 24),
    )
    variants = next(superkassa2.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch('requests.get', return_value=get_mocked_response('superkassa2_bad_baggage.xml'))
def test_superkassa_query_bad_baggage(mocked_request):
    expected = expected_variants('superkassa2_bad_baggage.json')
    test_query = get_query(
        point_to=SettlementMock(iata='LED', code='RU', id=2),
        date_forward=datetime.date(2017, 3, 21),
        date_backward=datetime.date(2017, 3, 24),
    )
    variants = next(superkassa2.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch('requests.get', return_value=get_mocked_response('superkassa2_fare_codes.xml'))
def test_superkassa_query_fare_codes(mocked_request):
    expected = expected_variants('superkassa2_fare_codes_expected.json')
    test_query = get_query(
        point_to=SettlementMock(iata='LED', code='RU', id=2),
        date_forward=datetime.date(2019, 12, 29),
        date_backward=datetime.date(2020, 1, 12),
    )
    variants = next(superkassa2.query(test_query))
    assert_variants_equal(expected, variants)
