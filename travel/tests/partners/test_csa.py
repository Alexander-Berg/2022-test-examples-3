# -*- coding: utf-8 -*-
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, get_query, assert_variants_equal,
    expected_variants
)
from travel.avia.ticket_daemon.ticket_daemon.partners import csa


@mock.patch('requests.get', return_value=get_mocked_response('csa.xml'))
def test_csa_one_way_query(mocked_request):
    expected = expected_variants('csa_expected.json')
    test_query = get_query()
    variants = next(csa.query(test_query))
    assert_variants_equal(expected, variants)
