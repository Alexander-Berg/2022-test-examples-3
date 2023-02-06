# -*- coding: utf-8 -*-
import mock

from travel.avia.ticket_daemon.ticket_daemon.partners import agent3
from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, get_query, assert_variants_equal,
    expected_variants
)


@mock.patch('requests.post', return_value=get_mocked_response('agent3_oneway.xml'))
def test_agent3_query_one_way(mocked_request):
    expected = expected_variants('agent3_oneway.json')
    test_query = get_query()
    variants = next(agent3.query(test_query))

    assert_variants_equal(expected, variants)


@mock.patch('requests.post', return_value=get_mocked_response('agent3_twoway.xml'))
def test_agent3_query_two_way(mocked_request):
    expected = expected_variants('agent3_twoway.json')
    test_query = get_query()
    variants = next(agent3.query(test_query))

    assert_variants_equal(expected, variants)
