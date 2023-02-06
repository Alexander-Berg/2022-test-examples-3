# -*- coding: utf-8 -*-
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal
)
from travel.avia.ticket_daemon.ticket_daemon.partners import biletdv


@mock.patch('requests.post', return_value=get_mocked_response('biletdv.xml'))
def test_biletdv_query(mocked_request):
    expected = expected_variants('biletdv.json')
    variants = next(biletdv.query(get_query()))
    assert_variants_equal(expected, variants)


@mock.patch('requests.post', return_value=get_mocked_response('biletdv_baggage.xml'))
def test_biletdv_baggage_query(mocked_request):
    expected = expected_variants('biletdv_baggage.json')
    variants = next(biletdv.query(get_query()))
    assert_variants_equal(expected, variants)
