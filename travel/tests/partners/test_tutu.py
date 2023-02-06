# -*- coding: utf-8 -*-
import mock
import pytest
from six.moves.urllib.parse import urlparse, parse_qsl

from travel.avia.library.python.tester.factories import create_partner
from travel.avia.ticket_daemon.tests.partners.helper import (
    expected_variants, get_mocked_response, get_query, assert_variants_equal
)
from travel.avia.ticket_daemon.ticket_daemon.api.redirect import fetch_redirect_data
from travel.avia.ticket_daemon.ticket_daemon.partners import tutu


@mock.patch('requests.get', return_value=get_mocked_response('tutu.json'))
def test_query(mocked_request):
    expected = expected_variants('tutu_expected.json')
    variants = next(tutu.query(get_query()))
    assert_variants_equal(expected, variants)


url = 'https://avia.tutu.ru/yandex-offer/?p=10382&o=eyJzIjp7'


def test_book_with_brand():
    order_data = {
        'url': url,
        'avia_brand': 'tutu'
    }

    expected_redir_data = tutu.book(order_data)
    url_parse = urlparse(expected_redir_data)
    query = dict(parse_qsl(url_parse.query, keep_blank_values=True))

    assert query.get('unisearchquery') == 'brand'


def test_book_without_brand():
    order_data = {
        'url': url,
    }

    expected_redir_data = tutu.book(order_data)
    url_parse = urlparse(expected_redir_data)
    query = dict(parse_qsl(url_parse.query, keep_blank_values=True))

    assert query.get('unisearchquery') is None


@pytest.mark.dbuser
def test_redirect_data_with_brand():
    order_data = {
        'url': url,
        'avia_brand': 'tutu'
    }

    partner = create_partner(code='tutu', query_module_name='tutu', marker='redirect-id')
    redir_data = fetch_redirect_data(partner, order_data)
    url_parse = urlparse(redir_data['url'])
    query = dict(parse_qsl(url_parse.query, keep_blank_values=True))

    assert query.get('unisearchquery') == 'brand'
