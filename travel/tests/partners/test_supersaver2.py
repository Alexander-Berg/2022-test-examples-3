# -*- coding: utf-8 -*-
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, get_query, assert_variants_equal,
    expected_variants
)
from travel.avia.ticket_daemon.ticket_daemon.partners import supersaver2


@mock.patch('requests.get', return_value=get_mocked_response('supersaver2_one_way.json'))
def test_supersaver2_one_way_query(mocked_request):
    expected = expected_variants('supersaver2_one_way_expected.json')
    test_query = get_query(date_backward=None)
    variants = next(supersaver2.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch('requests.get', return_value=get_mocked_response('supersaver2_round_trip.json'))
def test_supersaver2_return_query(mocked_request):
    expected = expected_variants('supersaver2_round_trip_expected.json')
    test_query = get_query()
    variants = next(supersaver2.query(test_query))
    assert_variants_equal(expected, variants)
