# -*- coding: utf-8 -*-
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants,
    get_mocked_response, get_query, assert_variants_equal,

)
from travel.avia.ticket_daemon.ticket_daemon.partners import s_seven_testing


@mock.patch('requests.post', return_value=get_mocked_response('s_seven_testing_one_way.xml'))
def test_s_seven_testing_one_way_query(mocked_request):
    expected = expected_variants('s_seven_testing_oneway_result.json')
    test_query = get_query(date_backward=None, klass='business')
    variants = next(s_seven_testing.query(test_query))

    assert_variants_equal(expected, variants)


@mock.patch('requests.post', return_value=get_mocked_response('s_seven_testing_one_way.xml'))
def test_s_seven_testing_different_national_versions(mocked_request):
    expected = expected_variants('s_seven_testing_diff_cur_KZT_oneway_result.json')

    test_query = get_query(national_version='kz', date_backward=None, klass='business')
    variants = next(s_seven_testing.query(test_query))

    assert_variants_equal(expected, variants)
