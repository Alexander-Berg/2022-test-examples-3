# -*- coding: utf-8 -*-
import mock

from travel.avia.ticket_daemon.ticket_daemon.partners import tez_tour
from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, get_query, assert_variants_equal,
    expected_variants
)


@mock.patch('requests.post', return_value=get_mocked_response('tez_tour-one-way.xml'))
def test_tez_tour_query_one_way(mocked_request):
    expected = expected_variants('tez_tour-one-way.json')
    test_query = get_query()
    variants = next(tez_tour.query(test_query))

    assert_variants_equal(expected, variants)


@mock.patch('requests.post', return_value=get_mocked_response('tez_tour-two-way.xml'))
def test_tez_tour_query_two_way(mocked_request):
    expected = expected_variants('tez_tour-two-way.json')
    test_query = get_query()
    variants = next(tez_tour.query(test_query))

    assert_variants_equal(expected, variants)
