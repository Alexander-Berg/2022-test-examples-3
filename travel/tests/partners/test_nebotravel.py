# -*- coding: utf-8 -*-
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, get_query, assert_variants_equal,
    expected_variants
)
from travel.avia.ticket_daemon.ticket_daemon.partners import nebotravel


@mock.patch('requests.get', return_value=get_mocked_response('nebotravel.xml'))
@mock.patch('travel.avia.ticket_daemon.ticket_daemon.partners.nebotravel.build_search_params', return_value={})
def test_nebotravel_one_way_query(mocked_request, mocked_build_search_params):
    expected = expected_variants('nebotravel_expected.json')
    test_query = get_query()
    variants = next(nebotravel.query(test_query))
    assert_variants_equal(expected, variants)
