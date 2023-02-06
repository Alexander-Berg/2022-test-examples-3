# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import httpretty
import mock
import pytest

from common.data_api.travelers.client import TravelersClient


class MockTvmFactory(object):
    def get_provider(self):
        return mock.Mock(get_ticket=mock.Mock(
            side_effect=lambda tvm_id: 'tvmtoken42' if tvm_id == 42 else None
        ))


class TestTravelersClient(object):
    def _get_client(self):
        return TravelersClient(
            host='https://myhost',
            tvm_travelers_id=42,
            tvm_factory=MockTvmFactory(),
        )

    def test_call(self):
        def request_callback(request, uri, response_headers):
            assert request.headers.get('X-Ya-Service-Ticket') == 'tvmtoken42'
            assert request.headers.get('X-Ya-User-Ticket') == 'userticket42'
            assert request.querystring['a'][0] == '1'
            assert request.parsed_body == {'b': 1}

            return [200, response_headers, json.dumps({"hello": "world"})]

        httpretty.register_uri(httpretty.POST, 'https://myhost/some_url', body=request_callback)

        client = self._get_client()
        result = client.call('POST', 'some_url', user_ticket='userticket42', params={'a': 1}, data={'b': 1})
        assert result == {'hello': 'world'}

    def test_create_traveler(self):
        def request_callback(request, uri, response_headers):
            assert request.headers.get('X-Ya-Service-Ticket') == 'tvmtoken42'
            assert request.headers.get('X-Ya-User-Ticket') == 'userticket42'
            assert request.parsed_body == {'agree': True, 'email': 'a@yandex.ru', 'phone': '+79991234567'}

            return [200, response_headers, json.dumps({"hello": "world"})]

        httpretty.register_uri(httpretty.POST, 'https://myhost/v1/travelers/uid123', body=request_callback)
        client = self._get_client()
        response = client.create_traveler('uid123', 'userticket42', 'a@yandex.ru', '+79991234567')
        assert response == {"hello": "world"}

    def test_get_traveler(self):
        httpretty.register_uri(httpretty.GET, 'https://myhost/v1/travelers/uid123', json.dumps({'email': 123}))
        client = self._get_client()
        response = client.get_traveler('uid123', 'userticket42')
        assert response == {'email': 123}

        # при 404 возвращается None
        httpretty.reset()
        httpretty.register_uri(httpretty.GET, 'https://myhost/v1/travelers/uid123', status=404)
        response = client.get_traveler('uid123', 'userticket42')
        assert response is None

        # при других ошибках - бросатеся исключение
        httpretty.reset()
        httpretty.register_uri(httpretty.GET, 'https://myhost/v1/travelers/uid123', status=500)
        with pytest.raises(Exception):
            client.get_traveler('uid123', 'userticket42')
