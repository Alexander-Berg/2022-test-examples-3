# -*- coding: utf-8 -*-
from datetime import date
import mock
import requests
from six.moves.urllib.parse import urlparse, parse_qsl

import pytest

from travel.avia.library.python.tester.factories import create_partner
from travel.avia.ticket_daemon.ticket_daemon.api.redirect import fetch_redirect_data
from travel.avia.ticket_daemon.ticket_daemon.partners import smartavia2
from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, get_query, assert_variants_equal, expected_variants, SettlementMock
)


@mock.patch('requests.post', return_value=get_mocked_response('smartavia_oneway.json'))
@mock.patch('requests.get', return_value=get_mocked_response('smartavia_oneway.json'))
def test_smartavia_one_way_query(*mocks):
    expected = expected_variants('smartavia_oneway_expected.json')
    test_query = get_query(
        point_from=SettlementMock(iata='KIV', code='MD', id=10313),
        point_to=SettlementMock(iata='VKO', code='RU', id=213),
        date_forward=date(2021, 6, 18),
        date_backward=None,
    )
    variants = next(smartavia2.query(test_query))

    assert_variants_equal(expected, variants)


@mock.patch('requests.post', return_value=get_mocked_response('smartavia_roundtrip.json'))
@mock.patch('requests.get', return_value=get_mocked_response('smartavia_roundtrip.json'))
def test_smartavia_return_query(*mocks):
    expected = expected_variants('smartavia_roundtrip_expected.json')
    test_query = get_query(
        point_from=SettlementMock(iata='VKO', code='RU', id=213),
        point_to=SettlementMock(iata='CDG', code='FR', id=11111),
        date_forward=date(2021, 6, 18),
        date_backward=date(2021, 6, 28)
    )
    variants = next(smartavia2.query(test_query))

    assert_variants_equal(expected, variants)


def get_mocked_redirect_response(location):
    class MockRedirectResponse(requests.Response):
        def __init__(self, status_code=302):
            super(MockRedirectResponse, self).__init__()

            self.headers['content-type'] = 'json'
            self.headers['Location'] = location
            self.status_code = status_code
            self._content = 'sample content'

    return MockRedirectResponse()


@mock.patch('requests.post', return_value=get_mocked_redirect_response('https://flysmartavia.com/best-offer'))
def test_book(*mocks):
    order_data = {
        'qid': 'test_book_qid',
        'params': {
            'direct-only': '1',
            'origin-city-code': ['ARH'],
        },
    }

    redir_data = smartavia2.book(order_data)
    assert redir_data['url'] == 'https://ya1-api.flysmartavia.com/booking-meta-redirect'
    assert 'ARH' == redir_data['post_data']['origin-city-code[0]']


@mock.patch('requests.post', return_value=get_mocked_redirect_response('https://flysmartavia.com/best-offer'))
@mock.patch('travel.avia.ticket_daemon.ticket_daemon.api.yaclid.YaClid.dumps', return_value='demo_yaclid')
@mock.patch('travel.avia.ticket_daemon.ticket_daemon.api.redirect.generate_marker', return_value='testmarker')
@pytest.mark.dbuser
def test_complete_book(mock_marker, mock_yaclid, mock_post):
    mock_marker.__name__ = 'trivial_marker_generator'

    order_data = {
        'qid': 'test_book_qid',
        'params': {
            'direct-only': '1',
            'origin-city-code': ['ARH'],
        },
    }

    partner = create_partner(code='smartavia', query_module_name='smartavia', marker='redirect-id')
    redir_data = fetch_redirect_data(partner, order_data)

    assert 'testmarker' == redir_data['post_data']['redirect-id']

    url = urlparse(redir_data['url'])
    assert 'https' == url.scheme
    assert 'ya1-api.flysmartavia.com' == url.netloc
    assert '/booking-meta-redirect' == url.path

    query = dict(parse_qsl(url.query, keep_blank_values=True))
    assert query == {
        'yaclid': 'demo_yaclid',
        'redirect-id': 'testmarker',
    }

    assert 'ARH' == redir_data['post_data']['origin-city-code[0]']
