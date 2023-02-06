# -*- coding: utf-8 -*-
import mock
import pytest

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants,
    get_mocked_response, get_query, assert_variants_equal,

)
from travel.avia.ticket_daemon.ticket_daemon.partners import s_seven6
from travel.avia.ticket_daemon.ticket_daemon.daemon.utils import BadPartnerResponse, PartnerErrorTypes


@mock.patch('requests.post', return_value=get_mocked_response('s_seven6_one_way.xml'))
def test_s_seven6_one_way_query(mocked_request):
    expected = expected_variants('s_seven6_oneway_result.json')
    test_query = get_query(date_backward=None)
    variants = next(s_seven6.query(test_query))

    assert_variants_equal(expected, variants)


@mock.patch('requests.post', return_value=get_mocked_response('s_seven6_one_way.xml'))
def test_s_seven6_different_national_versions(mocked_request):
    expected = expected_variants('s_seven6_diff_cur_KZT_oneway_result.json')

    test_query = get_query(national_version='kz', date_backward=None)
    variants = next(s_seven6.query(test_query))

    assert_variants_equal(expected, variants)


@mock.patch('requests.post', return_value=get_mocked_response('s_seven6_far_date.xml'))
def test_s_seven6_far_date_raises_error(mocked_request):
    test_query = get_query(date_backward=None)
    with pytest.raises(BadPartnerResponse) as excinfo:
        next(s_seven6.query(test_query))

    assert excinfo.value.errors == PartnerErrorTypes.SYSTEM_ERROR


@mock.patch('requests.post', return_value=get_mocked_response('s_seven6_past_date.xml'))
def test_s_seven6_past_date_raises_error(mocked_request):
    test_query = get_query(date_backward=None)
    with pytest.raises(BadPartnerResponse) as excinfo:
        next(s_seven6.query(test_query))

    assert excinfo.value.errors == PartnerErrorTypes.DATE_IN_THE_PAST


@mock.patch('requests.post', return_value=get_mocked_response('s_seven6_cyrillic_code.xml'))
def test_s_seven6_cyrillic_code_raises_error(mocked_request):
    test_query = get_query(date_backward=None)
    with pytest.raises(BadPartnerResponse) as excinfo:
        next(s_seven6.query(test_query))

    assert excinfo.value.errors == PartnerErrorTypes.CYRILLIC_CODE


@mock.patch('requests.post', return_value=get_mocked_response('s_seven6_unknown_error.xml'))
def test_s_seven6_unknown_error_raises_error(mocked_request):
    test_query = get_query(date_backward=None)
    with pytest.raises(BadPartnerResponse) as excinfo:
        next(s_seven6.query(test_query))

    assert excinfo.value.errors == PartnerErrorTypes.ERROR
