# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import datetime

import pytest
import httpretty
import requests

from travel.library.python.base_http_client import RetryConfig, CircuitBreakerConfig
from travel.rasp.library.python.api_clients.im.client import ImClient


IM_HOST = 'https://imhost.ru/'


def register_im_handler(endpoint, response, api_host=IM_HOST, status_code=200):
    def request_callback(request, uri, response_headers):
        return [status_code, response_headers, json.dumps(response)]

    httpretty.register_uri(
        httpretty.POST, '{}Railway/V1/{}'.format(api_host, endpoint),
        status=status_code, content_type='application/json',
        body=request_callback
    )


def check_last_im_call(expected):
    assert json.loads(httpretty.last_request().body) == expected


@httpretty.activate
class TestImClient(object):
    def get_im_client(self):
        breaker_config = CircuitBreakerConfig(fail_max=2, reset_timeout=1)
        retry_config = RetryConfig(total=2)
        return ImClient(
            host=IM_HOST,
            username='user',
            password='pass',
            pos='pos',
            circuit_breaker_config=breaker_config,
            retry_config=retry_config,
            timeout=2
        )

    def test_train_pricing(self):
        im_client = self.get_im_client()

        register_im_handler('Search/TrainPricing', {'some_data': 'some_value'})
        result = im_client.train_pricing('123', '456', datetime(2021, 7, 10))

        assert result == {'some_data': 'some_value'}
        check_last_im_call({
            'CarGrouping': 'Group',
            'Origin': '123',
            'Destination': '456',
            'DepartureDate': '2021-07-10T00:00:00',
        })

        register_im_handler('Search/TrainPricing', {'some_data': 'some_value'}, status_code=404)
        result = im_client.train_pricing('123', '456', datetime(2021, 7, 10))

        assert result == {'Trains': []}

        register_im_handler('Search/TrainPricing', {'some_data': 'some_value'}, status_code=500)
        with pytest.raises(requests.HTTPError):
            im_client.train_pricing('123', '456', datetime(2021, 7, 10))
