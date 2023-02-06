# -*- coding: utf-8 -*-
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal
)
from travel.avia.ticket_daemon.ticket_daemon.partners import tinkoff, tinkoff2


@mock.patch('requests.get', return_value=get_mocked_response('tinkoff.xml'))
def test_tinkoff_query(mocked_request):
    expected = expected_variants('tinkoff.json')
    test_query = get_query()
    variants = next(tinkoff.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch('requests.get', return_value=get_mocked_response('tinkoff2_oneway.xml'))
def test_tinkoff2_oneway_query(mocked_request):
    expected = expected_variants('tinkoff2_oneway_expected.json')
    test_query = get_query()
    variants = next(tinkoff2.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch('requests.get', return_value=get_mocked_response('tinkoff2_return.xml'))
def test_tinkoff2_return_query(mocked_request):
    expected = expected_variants('tinkoff2_return_expected.json')
    test_query = get_query()
    variants = next(tinkoff2.query(test_query))
    assert_variants_equal(expected, variants)
