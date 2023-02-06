# -*- coding: utf-8 -*-
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response,
    get_query, assert_variants_equal
)
from travel.avia.ticket_daemon.ticket_daemon.partners import aerotur


@mock.patch('requests.post', return_value=get_mocked_response('uzairways3.xml'))
def test_aerotur_query(mocked_request):
    expected = expected_variants('uzairways3.json')
    test_query = get_query()
    variants = next(aerotur.query(test_query))

    assert_variants_equal(expected, variants)
