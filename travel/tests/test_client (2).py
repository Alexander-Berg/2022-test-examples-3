# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
import logging
from random import Random
from string import ascii_letters
from typing import AnyStr

import httpretty

from travel.library.python.base_http_client import (
    BaseHttpClient, CircuitBreakerConfig, CustomHeadersCreator, RetryConfig
)

HOST = 'http://abc.xyz/'


def string_generator(rnd, length=12):
    # type: (Random, int) -> AnyStr
    return ''.join(rnd.choice(ascii_letters) for _ in range(length))


class RandomAuth(CustomHeadersCreator):
    def __init__(self, seed):
        self._rnd = Random(seed)

    def get_headers(self):
        return {'random_token': string_generator(self._rnd)}


class StaticAuth(CustomHeadersCreator):
    def get_headers(self):
        return {'static_token': 'TOKEN'}


class TestClientCreation:
    def test_client_creation(self):
        class TestClient(BaseHttpClient):
            HTTP_CLIENT_NAME = 'Test client'
            RETRY_CONFIG = RetryConfig(total=10)
            CIRCUIT_BREAKER_CONFIG = CircuitBreakerConfig(fail_max=2, reset_timeout=5)
            TIMEOUT = 1

        test_client = TestClient(host=HOST)

        assert test_client._host == HOST
        assert test_client.http_client_name == 'Test client'
        retry = test_client._instance_scope.get_retry()
        assert retry.total == 10
        circuit_breaker = test_client._instance_scope.get_circuit_breaker()
        assert circuit_breaker.fail_max == 2
        assert circuit_breaker.reset_timeout == 5

        test_instance = TestClient(
            host=HOST,
            retry_config=RetryConfig(connect=10, redirect=3),
            circuit_breaker_config=CircuitBreakerConfig(fail_max=7, reset_timeout=15)
        )
        assert test_instance._host == HOST
        assert test_instance.http_client_name == 'Test client'
        retry = test_instance._instance_scope.get_retry()
        assert retry.connect == 10
        assert retry.redirect == 3
        circuit_breaker = test_instance._instance_scope.get_circuit_breaker()
        assert circuit_breaker.fail_max == 7
        assert circuit_breaker.reset_timeout == 15


class CustomClient(BaseHttpClient):
    RETRY_CONFIG = RetryConfig(total=3)
    CIRCUIT_BREAKER_CONFIG = CircuitBreakerConfig(fail_max=2, reset_timeout=5)
    TIMEOUT = 1
    MASKED_PARAMS = {'key1'}

    def get_plain_request(self):
        return self.make_request('GET', 'get_plain_request', params={'some_param': 123}).text

    def get_json_request(self):
        return self.get('get_json_request', {'some_param': 123}).json()


class TestClientRequests:
    def test_get_plain_request(self, caplog):
        client = CustomClient(HOST, masked_params={'key2'})
        caplog.set_level(logging.INFO)
        with httpretty.enabled():
            httpretty.register_uri(
                httpretty.GET,
                uri=HOST + 'get_plain_request',
                body='123'
            )
            result = client.get_plain_request()
        assert result == '123'

        with httpretty.enabled():
            httpretty.register_uri(httpretty.GET, uri=HOST)
            client.get('', params={'key1': 'value1'})
            client.get('', params={'key2': 'value2'})
            client.get('', params={'key3': 'value3'}, masked_params={'key3'})

        log_records_messages = [record.message for record in caplog.records]
        log_msg = '[CustomClient (GET http://abc.xyz/get_plain_request with params {"some_param": 123})] call started'
        assert log_msg in log_records_messages
        log_msg = '[CustomClient (GET http://abc.xyz/ with params {"key1": "*****"})] call started'
        assert log_msg in log_records_messages
        log_msg = '[CustomClient (GET http://abc.xyz/ with params {"key2": "*****"})] call started'
        assert log_msg in log_records_messages
        log_msg = '[CustomClient (GET http://abc.xyz/ with params {"key3": "*****"})] call started'
        assert log_msg in log_records_messages

    def test_get_json_request(self):
        client = CustomClient(HOST)
        with httpretty.enabled():
            httpretty.register_uri(
                httpretty.GET,
                uri=HOST + 'get_json_request',
                body=json.dumps({'result': 'ok'}),
                content_type='application/json'
            )
            json_response = client.get_json_request()
        assert json_response['result'] == 'ok'

    def test_make_params_for_log(self):
        client = CustomClient(HOST)
        params = json.loads(client._make_params_for_log(
            {'key1': 'value1', 'key2': 'value2'}, {'key1'}
        ))
        assert params == {'key1': '*****', 'key2': 'value2'}

        params = json.loads(client._make_params_for_log(
            {'key1': 'value1', 'key2': 'value2'}, None
        ))
        assert params == {'key1': 'value1', 'key2': 'value2'}

        params = json.loads(client._make_params_for_log(
            None, set()
        ))
        assert params == {}
