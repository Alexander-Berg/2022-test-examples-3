# -*- coding: utf-8 -*-
import mock

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response,
    get_query, assert_variants_equal
)
from travel.avia.ticket_daemon.ticket_daemon.partners import rusline5


def create_post_mock(nemo_response, sig_response):
    def post_mock(url, *args, **kwargs):
        if 'nemo' in url:
            return nemo_response
        return sig_response
    return post_mock


@mock.patch(
    'requests.post',
    side_effect=create_post_mock(
        get_mocked_response('rusline3_nemo.xml'),
        get_mocked_response('rusline4_sig_empty.xml'),
    ),
)
def test_rusline5_query_nemo(mocked_request):
    expected = expected_variants('rusline3_nemo.json')
    test_query = get_query()
    variants = next(rusline5.query(test_query))

    assert_variants_equal(expected, variants)


@mock.patch(
    'requests.post',
    side_effect=create_post_mock(
        get_mocked_response('rusline3_nemo_without_fare_code.xml'),
        get_mocked_response('rusline4_sig_empty.xml'),
    ),
)
def test_rusline5_query_without_fare_code(mocked_request):
    expected = expected_variants('rusline3_nemo_without_fare_code.json')
    test_query = get_query()
    variants = next(rusline5.query(test_query))

    assert_variants_equal(expected, variants)


@mock.patch(
    'requests.post',
    side_effect=create_post_mock(
        get_mocked_response('rusline3_nemo_empty.xml'),
        get_mocked_response('rusline4_sig.xml'),
    ),
)
def test_rusline5_query_sig(mocked_request):
    expected = expected_variants('rusline4_sig.json')
    test_query = get_query()
    variants = next(rusline5.query(test_query))

    assert_variants_equal(expected, variants)


@mock.patch(
    'requests.post',
    side_effect=create_post_mock(
        get_mocked_response('rusline3_nemo.xml'),
        get_mocked_response('rusline4_sig.xml'),
    ),
)
def test_rusline5_variants_from_sig_and_nemo(mocked_request):
    expected = expected_variants('rusline4_sig.json')
    test_query = get_query()
    variants = next(rusline5.query(test_query))

    assert_variants_equal(expected, variants)
