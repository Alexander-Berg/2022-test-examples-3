# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import httpretty
import mock
import pytest
import requests

from travel.rasp.library.python.api_clients.sandbox_proxy import SandboxProxyClient

HOST = 'http://test-sandbox-proxy.ru/'
OAUTH_TOKEN = 'oauth_token'


def get_sandbox_proxy_client():
    return SandboxProxyClient(
        host=HOST,
        oauth_token=OAUTH_TOKEN,
        disable_retry_config=True,
        disable_timeout=True,
        disable_circuit_breaker_config=True
    )


def test_get_last_resource():
    with mock.patch.object(SandboxProxyClient, 'get_sandbox_proxy_response', return_value=None) as m_get_sandbox_proxy_response:
        get_sandbox_proxy_client().get_last_resource('SOME_RESOURCE')
        m_get_sandbox_proxy_response.assert_called_with('last/SOME_RESOURCE')


@httpretty.activate
def test_get_sandbox_proxy_response():
    sandbox_proxy_client = get_sandbox_proxy_client()

    httpretty.register_uri(
        httpretty.GET, '{}SOME_RESOURCE'.format(HOST), status=200,
        body='[{"title": "test"}]', content_type="application/json"
    )
    response = sandbox_proxy_client.get_sandbox_proxy_response('SOME_RESOURCE')
    assert response.json() == [{'title': 'test'}]

    httpretty.register_uri(httpretty.GET, '{}SOME_RESOURCE?par=value'.format(HOST), status=200, body='{"status": "OK"}')
    res = sandbox_proxy_client.get_sandbox_proxy_response('SOME_RESOURCE', params={'par': 'value'}, timeout=5)
    assert res.json().get("status") == "OK"

    httpretty.register_uri(httpretty.GET, '{}SOME_RESOURCE_400'.format(HOST), status=400)
    with pytest.raises(requests.HTTPError):
        sandbox_proxy_client.get_sandbox_proxy_response('SOME_RESOURCE_400')

    httpretty.register_uri(httpretty.GET, '{}SOME_RESOURCE_500'.format(HOST), status=500)
    with pytest.raises(requests.HTTPError):
        sandbox_proxy_client.get_sandbox_proxy_response('SOME_RESOURCE_500')
