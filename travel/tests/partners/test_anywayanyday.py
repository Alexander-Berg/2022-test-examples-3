# -*- coding: utf-8 -*-
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal
)
from travel.avia.ticket_daemon.ticket_daemon.partners import anywayanyday


@mock.patch('requests.get', return_value=get_mocked_response('anywayanyday_oneway.json'))
def test_anywayanyday_oneway_query(mocked_request):
    test_query = get_query(
        date_backward=None,
        klass='business',
    )
    expected = expected_variants('anywayanyday_oneway_expected.json')
    variants = next(anywayanyday.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch('requests.get', return_value=get_mocked_response('anywayanyday_return.json'))
def test_anywayanyday_return_query(mocked_request):
    test_query = get_query(klass='business')
    expected = expected_variants('anywayanyday_return_expected.json')
    variants = next(anywayanyday.query(test_query))
    assert_variants_equal(expected, variants)
