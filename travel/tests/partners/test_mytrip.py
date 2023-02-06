# -*- coding: utf-8 -*-
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, get_query, assert_variants_equal,
    expected_variants
)
from travel.avia.ticket_daemon.ticket_daemon.partners import mytrip


@mock.patch('requests.get', return_value=get_mocked_response('mytrip.json'))
def test_go_to_gate_one_way_query(mocked_request):
    expected = expected_variants('mytrip_one_way_expected.json')
    test_query = get_query(date_backward=None)
    variants = next(mytrip.query(test_query))
    assert_variants_equal(expected, variants)
