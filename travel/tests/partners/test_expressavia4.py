# -*- coding: utf-8 -*-
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal
)
from travel.avia.ticket_daemon.ticket_daemon.partners import expressavia4


@mock.patch('requests.post', return_value=get_mocked_response('expressavia4.xml'))
def test_expressavia3_query(mocked_request):
    expected = expected_variants('expressavia4.json')
    test_query = get_query()
    variants = next(expressavia4.query(test_query))

    assert_variants_equal(expected, variants)
