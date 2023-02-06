# -*- coding: utf-8 -*-
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response,
    get_query, assert_variants_equal
)
from travel.avia.ticket_daemon.ticket_daemon.partners import rusline3


@mock.patch('requests.post', return_value=get_mocked_response('rusline3_nemo.xml'))
def test_rusline3_query(mocked_request):
    expected = expected_variants('rusline3_nemo.json')
    test_query = get_query()
    variants = next(rusline3.query(test_query))

    assert_variants_equal(expected, variants)


@mock.patch('requests.post', return_value=get_mocked_response('rusline3_nemo_without_fare_code.xml'))
def test_rusline3_query_without_fare_code(mocked_request):
    expected = expected_variants('rusline3_nemo_without_fare_code.json')
    test_query = get_query()
    variants = next(rusline3.query(test_query))

    assert_variants_equal(expected, variants)
