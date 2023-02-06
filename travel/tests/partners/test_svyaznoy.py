# -*- coding: utf-8 -*-
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal
)
from travel.avia.ticket_daemon.ticket_daemon.partners import svyaznoy4


@mock.patch('requests.post', return_value=get_mocked_response('svyaznoy4.json'))
def test_svyaznoy4_query(mocked_request):
    test_query = get_query()
    expected = expected_variants('svyaznoy4_expected.json')
    variants = next(svyaznoy4.query(test_query))
    assert_variants_equal(expected, variants)
