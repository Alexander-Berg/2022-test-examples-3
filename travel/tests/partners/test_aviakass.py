# -*- coding: utf-8 -*-
import mock
import pytest

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal
)
from travel.avia.ticket_daemon.ticket_daemon.daemon.utils import BadPartnerResponse, PartnerErrorTypes
from travel.avia.ticket_daemon.ticket_daemon.partners import aviakass5


@mock.patch('requests.post', return_value=get_mocked_response('aviakass5.xml'))
def test_aviakass5_query(mocked_request):
    expected = expected_variants('aviakass5.json')
    test_query = get_query()
    variants = next(aviakass5.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch('requests.post', return_value=get_mocked_response('aviakass5_empty_variant.xml'))
def test_aviakass5_empty_variant(mocked_request):
    test_query = get_query()
    variants = next(aviakass5.query(test_query))
    assert_variants_equal([], variants)


@mock.patch('requests.post', return_value=get_mocked_response('aviakass5_past_date_error.xml'))
def test_aviakass5_past_date_raises_error(mocked_request):
    test_query = get_query()
    with pytest.raises(BadPartnerResponse) as excinfo:
        next(aviakass5.query(test_query))

    assert excinfo.value.errors == PartnerErrorTypes.DATE_IN_THE_PAST


@mock.patch('requests.post', return_value=get_mocked_response('aviakass5_bad_class_error.xml'))
def test_aviakass5_validation_error(mocked_request):
    test_query = get_query()
    with pytest.raises(BadPartnerResponse) as excinfo:
        next(aviakass5.query(test_query))

    assert excinfo.value.errors == PartnerErrorTypes.IVALID_REQUEST_STRUCTURE
