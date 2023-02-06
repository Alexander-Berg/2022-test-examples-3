# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import httpretty
import pytest
import requests
import six

from travel.library.python.base_http_client import CustomHeadersCreator, RetryConfig
from travel.rasp.library.python.api_clients.travel_api.client import TravelApiClient, TravelApiUserIdent

HOST = 'http://test_travel_api.ru/'


class FakeTvmHeaderCreator(CustomHeadersCreator):
    def __init__(self, token):
        self._token = token

    def get_headers(self):
        return {'X-Ya-Service-Ticket': self._token}


def _make_travel_api_client():
    return TravelApiClient(
        host=HOST,
        tvm_header_creator=FakeTvmHeaderCreator('tvm_ticket'),
        timeout=2,
        retry_config=RetryConfig(total=3)
    )


def test_make_headers():
    client = _make_travel_api_client()

    user_ident = TravelApiUserIdent()
    headers = client.make_headers(user_ident)

    assert headers['Content-Type'] == 'application/json'
    assert 'X-Ya-Session-Key' not in headers
    assert 'X-Ya-YandexUid' not in headers
    assert 'X-Ya-User-Ticket' not in headers
    assert 'X-Ya-User-Agent' not in headers
    assert 'X-Ya-User-Ip' not in headers

    user_ident = TravelApiUserIdent(session_id='session')
    headers = client.make_headers(user_ident)
    assert headers['X-Ya-Session-Key'] == 'session'

    user_ident = TravelApiUserIdent(yandex_uid='yandex')
    headers = client.make_headers(user_ident)
    assert headers['X-Ya-YandexUid'] == 'yandex'

    user_ident = TravelApiUserIdent(user_ticket='user_ticket')
    headers = client.make_headers(user_ident)
    assert headers['X-Ya-User-Ticket'] == 'user_ticket'

    user_ident = TravelApiUserIdent(blackbox_user_uid='user_uid')
    headers = client.make_headers(user_ident)
    assert headers['X-Ya-PassportId'] == 'user_uid'

    user_ident = TravelApiUserIdent(user_agent='user_agent')
    headers = client.make_headers(user_ident)
    assert headers['X-Ya-User-Agent'] == 'user_agent'

    user_ident = TravelApiUserIdent(user_ip='user_ip')
    headers = client.make_headers(user_ident)
    assert headers['X-Ya-User-Ip'] == 'user_ip'


def _register_travel_api_url(
    url_suffix, params=None, request_content=None, response_json=None, status_code=200
):
    def request_callback(request, uri, response_headers):
        if request_content:
            assert json.loads(request.body) == request_content

        assert request.headers.get('X-Ya-Service-Ticket') == 'tvm_ticket'
        assert request.headers.get('X-Ya-Session-Key') == 'session'
        assert request.headers.get('X-Ya-YandexUid') == 'yandex'
        assert request.headers.get('X-Ya-User-Ticket') == 'user_ticket'
        assert request.headers.get('X-Ya-PassportId') == 'user_uid'
        assert request.headers.get('X-Ya-User-Agent') == 'user_agent'
        assert request.headers.get('X-Ya-User-Ip') == 'user_ip'

        if response_json:
            assert request.headers.get('Content-Type') == 'application/json'
            response_body = json.dumps(response_json)
        else:
            response_body = 'OK'

        return [status_code, response_headers, response_body]

    method = httpretty.POST if request_content else httpretty.GET
    httpretty.register_uri(
        method, '{}{}'.format(HOST, url_suffix), status=status_code,
        params=params, body=request_callback, content_type='application/json'
    )


def _make_user_indent():
    return TravelApiUserIdent(
        session_id='session',
        yandex_uid='yandex',
        user_ticket='user_ticket',
        blackbox_user_uid='user_uid',
        user_agent='user_agent',
        user_ip='user_ip'
    )


def _make_client_and_ident():
    return _make_travel_api_client(), _make_user_indent()


@httpretty.activate
def test_call_api():
    user_ident = _make_user_indent()

    _register_travel_api_url(
        'post_method', request_content={'request': 'test'},
        response_json={'response': 'test'}, status_code=200
    )
    result = _make_travel_api_client().call_and_parse('post_method', user_ident, request_content={'request': 'test'})
    assert result == {'response': 'test'}

    _register_travel_api_url(
        'get_method', params={'par': 'x'},
        response_json={'response': 'test'}, status_code=200
    )
    result = _make_travel_api_client().call_and_parse('get_method', user_ident, params={'par': 'x'})
    assert result == {'response': 'test'}

    _register_travel_api_url(
        'post_method', request_content={'request': 'test'},
        response_json={'response': 'test'}, status_code=404
    )
    with pytest.raises(requests.HTTPError):
        _make_travel_api_client().call_and_parse('post_method', user_ident, request_content={'request': 'test'})

    _register_travel_api_url(
        'get_method', params={'par': 'x'},
        response_json={'response': 'test'}, status_code=404
    )
    with pytest.raises(requests.HTTPError):
        _make_travel_api_client().call_and_parse('get_method', user_ident, params={'par': 'x'})

    _register_travel_api_url(
        'post_method', status_code=400
    )
    with pytest.raises(requests.HTTPError):
        _make_travel_api_client().call_and_parse('post_method', user_ident)

    _register_travel_api_url(
        'post_method_json', status_code=400, response_json={'result': 'any'}
    )
    with pytest.raises(requests.HTTPError):
        _make_travel_api_client().call_and_parse('post_method', user_ident)

    _register_travel_api_url('get_method', params={'par': 'x'}, status_code=500)
    with pytest.raises(requests.HTTPError):
        _make_travel_api_client().call_and_parse('get_method', user_ident, params={'par': 'x'})


@httpretty.activate
def test_create_order():
    api_client, user_ident = _make_client_and_ident()
    _register_travel_api_url(
        url_suffix='generic_booking_flow/v1/create_order',
        request_content={'request': 'test'},
        response_json={'response': 'test'},
    )

    result = api_client.create_order(user_ident, {'request': 'test'})
    assert result == {'response': 'test'}


@httpretty.activate
def test_create_order_retries():
    def timeout_callback(request, uri, response_headers):
        raise requests.Timeout('.')

    httpretty.register_uri(
        httpretty.POST, '{}{}'.format(HOST, 'generic_booking_flow/v1/create_order'), body=timeout_callback, status=200
    )

    api_client, user_ident = _make_client_and_ident()
    with pytest.raises(requests.ConnectionError):
        api_client.create_order(user_ident, {'request': 'test'})

    if six.PY2:
        assert len(httpretty.latest_requests()) == 4
    else:  # httpretty bug with duplicated POST requests: https://github.com/gabrielfalcao/HTTPretty/issues/425
        assert len(httpretty.latest_requests()) == 8


@httpretty.activate
def test_get_order_state():
    api_client, user_ident = _make_client_and_ident()
    _register_travel_api_url(
        url_suffix='generic_booking_flow/v1/get_order_state?order_id=order_uid',
        response_json={'state': 'ZASHIBIS'},
    )

    result = api_client.get_order_state(user_ident, 'order_uid')
    assert result['state'] == 'ZASHIBIS'


@httpretty.activate
def test_get_order_state_batch():
    api_client, user_ident = _make_client_and_ident()
    _register_travel_api_url(
        url_suffix='generic_booking_flow/v1/get_order_state_batch',
        request_content={'request': 'test'},
        response_json={'response': 'test'},
    )

    result = api_client.get_order_state_batch(user_ident, {'request': 'test'})
    assert result == {'response': 'test'}


@httpretty.activate
def test_get_order():
    api_client, user_ident = _make_client_and_ident()
    _register_travel_api_url(
        url_suffix='generic_booking_flow/v1/get_order?order_id=order_uid',
        response_json={
            'state': 'CONFIRMED', 'ticket': 'BILET'
        },
    )

    result = api_client.get_order(user_ident, 'order_uid')
    assert result == {'state': 'CONFIRMED', 'ticket': 'BILET'}


@httpretty.activate
def test_start_payment():
    api_client, user_ident = _make_client_and_ident()
    _register_travel_api_url(
        url_suffix='generic_booking_flow/v1/start_payment',
        request_content={'request': 'test'}
    )

    result = api_client.start_payment(user_ident, {'request': 'test'})
    assert result is None


@httpretty.activate
def test_get_hotel_city_static_page():
    api_client, user_ident = _make_client_and_ident()

    response = {
        'seo_info': {'title': 'Отели в Москве'},
        'blocks': [{
            'type': 'IHotelsBlock',
            'hotel': {}
        }]
    }
    _register_travel_api_url(
        url_suffix='hotels_portal/v1/city_static_page',
        response_json=response
    )
    result = api_client.get_hotel_city_static_page(user_ident, slug='moscow')
    assert result == response

    _register_travel_api_url(
        url_suffix='hotels_portal/v1/city_static_page',
        response_json='{}',
        status_code=404
    )
    with pytest.raises(requests.HTTPError):
        api_client.get_hotel_city_static_page(user_ident, slug='revda')
