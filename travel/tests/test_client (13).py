# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import httpretty
import pytest
import requests
from requests.exceptions import RetryError

from travel.library.python.base_http_client import CircuitBreakerConfig, RetryConfig
from travel.rasp.library.python.api_clients.redir import RedirClient


HOST = 'http://test-redir.ru/'


def _get_client():
    return RedirClient(
        host=HOST,
        timeout=3,
        circuit_breaker_config=CircuitBreakerConfig(fail_max=5, reset_timeout=10),
        retry_config=RetryConfig(
            total=4,
            backoff_factor=0.5,
            status_forcelist=[413, 429, 500, 502, 503, 504]
        )
    )


def _register_label(url_path, label, status_code, body=''):
    def request_callback(request, uri, response_headers):
        return [status_code, response_headers, body]

    httpretty.register_uri(
        httpretty.GET, uri='{}{}'.format(HOST, url_path), params={'LabelParams': label},
        status=status_code, body=request_callback
    )


@httpretty.activate
def test_suburban_label_to_hash_200():
    _register_label('suburban/label_to_hash/', 'label_200', 200, 'some_hash')
    label_hash = _get_client().suburban_label_to_hash('label_200')
    assert label_hash == 'some_hash'


@httpretty.activate
def test_suburban_label_to_hash_500():
    _register_label('suburban/label_to_hash/', 'label_500', 500)
    with pytest.raises(RetryError):
        _get_client().suburban_label_to_hash('label_500')


@httpretty.activate
def test_suburban_label_to_hash_400():
    _register_label('suburban/label_to_hash/', 'label_400', 400)
    with pytest.raises(requests.HTTPError) as ex:
        _get_client().suburban_label_to_hash('label_400')
    assert ex.value.response.status_code == 400
