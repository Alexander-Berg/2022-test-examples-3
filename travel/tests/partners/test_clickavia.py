# -*- coding: utf-8 -*-
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal
)
from travel.avia.ticket_daemon.ticket_daemon.partners import clickavia2 as clickavia


@mock.patch('requests.get', return_value=get_mocked_response('clickavia.xml'))
def test_clickavia_query(mocked_request):
    expected = expected_variants('clickavia.json')
    variants = next(clickavia.query(get_query()))
    assert_variants_equal(expected, variants)


@mock.patch('requests.get', return_value=get_mocked_response('clickavia_transfers.xml'))
def test_clickavia_with_transfers_query(mocked_request):
    expected = expected_variants('clickavia_transfers.json')
    variants = next(clickavia.query(get_query()))
    assert_variants_equal(expected, variants)


@mock.patch('requests.get', return_value=get_mocked_response('clickavia_selfconnect.xml'))
def test_clickavia_with_selfconnect_query(mocked_request):
    expected = expected_variants('clickavia_selfconnect.json')
    variants = next(clickavia.query(get_query()))
    assert_variants_equal(expected, variants)
