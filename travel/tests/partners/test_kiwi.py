# -*- coding: utf-8 -*-
import mock
import pytest

from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal
)
from travel.avia.ticket_daemon.ticket_daemon.partners import kiwi


@mock.patch('requests.get', return_value=get_mocked_response('kiwi_one_way.json'))
def test_kiwi_one_way_query(mocked_request):
    expected = expected_variants('kiwi_one_way_expected.json')
    test_query = get_query()
    variants = next(kiwi.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch('requests.get', return_value=get_mocked_response('kiwi_return.json'))
def test_kiwi_return_query(mocked_request):
    expected = expected_variants('kiwi_return_expected.json')
    test_query = get_query()
    variants = next(kiwi.query(test_query))
    assert_variants_equal(expected, variants)


@mock.patch('requests.get', return_value=get_mocked_response('kiwi_selfconnect.json'))
def test_kiwi_selfconnect_query(mocked_request):
    expected = expected_variants('kiwi_selfconnect_expected.json')
    test_query = get_query()
    variants = next(kiwi.query(test_query))
    assert_variants_equal(expected, variants)


@pytest.mark.parametrize('national_version, expected_currency', [
    ('ru', 'RUB'),
    ('by', 'RUB'),
    ('kz', 'KZT'),
])
def test_rule__national_version_to_currency(national_version, expected_currency):
    query = get_query(national_version=national_version)
    params = kiwi.search_params(query)
    assert params['curr'] == expected_currency


@pytest.mark.parametrize('national_version, expected_result', [
    ('ru', 'ru'),
    ('tr', 'tr'),
    ('ua', 'ua'),
    ('com', 'ie'),
    ('kz', 'kz'),
])
def test_rule__national_version_to_partner_market(national_version, expected_result):
    query = get_query(national_version=national_version)
    params = kiwi.search_params(query)
    assert params['partner_market'] == expected_result
