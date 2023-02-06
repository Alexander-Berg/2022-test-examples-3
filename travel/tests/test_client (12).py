# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import datetime

import pytest
import httpretty
import mock
import requests

from travel.library.python.base_http_client import CircuitBreakerConfig
from travel.rasp.library.python.api_clients.movista.client import MovistaClient


MOVISTA_HOST = 'https://movistahost.ru/'


def register_movista_handler(api_method, response, api_host=MOVISTA_HOST, api_token='token42', status_code=200):
    def request_callback(request, uri, response_headers):
        assert request.headers['token'] == api_token
        return [status_code, response_headers, json.dumps(response)]

    httpretty.register_uri(
        httpretty.POST, '{}api/v1/{}'.format(api_host, api_method),
        status=status_code, content_type='application/json',
        body=request_callback
    )


def check_last_movista_call(expected):
    if expected:
        assert json.loads(httpretty.last_request().body) == expected
    else:
        assert httpretty.last_request().body == b''


@httpretty.activate
class TestMovistaClient(object):
    def get_movista_client(self, **kwargs):
        timeout = kwargs.pop('timeout', 1)
        return MovistaClient(
            host=MOVISTA_HOST,
            api_token='token42',
            circuit_breaker_config=CircuitBreakerConfig(fail_max=2, reset_timeout=5),
            timeout=timeout,
            **kwargs
        )

    def test_call_api_http_404(self):
        movista_client = self.get_movista_client()
        register_movista_handler('some_method', {}, status_code=404)
        result = movista_client.call_and_parse('some_method', json={'mystuff': 44})

        assert result is None
        check_last_movista_call({'mystuff': 44})

    def test_call_api_http_500(self):
        movista_client = self.get_movista_client()
        register_movista_handler('some_method', {}, status_code=500)
        with pytest.raises(requests.HTTPError):
            movista_client.call_and_parse('some_method', json={'mystuff': 50})

    def test_call_api_timeout(self):
        movista_client = self.get_movista_client(timeout=11)

        with mock.patch.object(requests.Session, 'request', autospec=True) as m_post:
            movista_client.call_and_parse('some_method', json={'mystuff': '1'}, timeout=42)
            post_call = m_post.call_args_list[-1]
            assert post_call[1]['timeout'] == 42

            movista_client.call_and_parse('some_method', json={'mystuff': '1'})
            post_call = m_post.call_args_list[-1]
            assert post_call[1]['timeout'] == 11

    def test_fares(self):
        movista_client = self.get_movista_client()
        register_movista_handler('fares', {'some_data': 'doesntmatter'})

        movista_client.fares(datetime(2020, 10, 12), '123', '456')

        check_last_movista_call({
            'date': '2020-10-12',
            'fromExpressId': '123',
            'toExpressId': '456',
        })

    def test_fares_404(self):
        movista_client = self.get_movista_client()
        register_movista_handler('fares', {'some_data': 'doesntmatter'}, status_code=404)

        result = movista_client.fares(datetime(2020, 10, 12), '123', '456')
        assert result == {'fares': [], 'sale': False}

    def test_timetable(self):
        movista_client = self.get_movista_client()
        register_movista_handler('timetable', {'some_data': 'doesntmatter'})

        dt = datetime(2020, 10, 12)
        movista_client.timetable(dt, '123', '456')

        check_last_movista_call({
            'date': '2020-10-12',
            'fromExpressId': '123',
            'toExpressId': '456',
        })

    def test_timetable_404(self):
        movista_client = self.get_movista_client()
        register_movista_handler('timetable', {'some_data': 'doesntmatter'}, status_code=404)

        dt = datetime(2020, 10, 12)
        result = movista_client.timetable(dt, '123', '456')

        assert result == {}

    def test_stops(self):
        movista_client = self.get_movista_client()
        register_movista_handler('stops', [{'some_data': 'doesntmatter'}])

        movista_client.stops()
        check_last_movista_call(None)

    def test_activation(self):
        movista_client = self.get_movista_client()
        register_movista_handler('activation', [{'some_data': 'doesntmatter'}])

        movista_client.activation(111, datetime(2021, 3, 8, 12, 0), True, 'QR', 'ticket')
        check_last_movista_call({
            'orderId': 111,
            'date': '2021-03-08T12:00:00',
            'success': True,
            'qrBody': 'QR',
            'ticketBody': 'ticket'
        })

    def test_cancels(self):
        movista_client = self.get_movista_client()
        register_movista_handler('timetable/cancels', [{'some_data': 'doesntmatter'}])

        movista_client.cancels(datetime(2021, 4, 1, 12, 0))
        check_last_movista_call({'date': '2021-04-01'})

    def test_report(self):
        movista_client = self.get_movista_client()
        register_movista_handler('report', [{'some_data': 'doesntmatter'}])

        movista_client.report(datetime(2021, 8, 13))
        check_last_movista_call({'date': '2021-08-13'})

        movista_client.report(datetime(2021, 8, 13), status='confirmed')
        check_last_movista_call({'date': '2021-08-13', 'status': 'confirmed'})

        movista_client.report(datetime(2021, 8, 13, 1, 10), datetime(2021, 8, 13, 2, 20))
        check_last_movista_call({'date': '2021-08-13T01:10:00', 'date2': '2021-08-13T02:20:00'})

        movista_client.report(datetime(2021, 8, 13, 1, 10), datetime(2021, 8, 13, 2, 20), status='confirmed')
        check_last_movista_call({'date': '2021-08-13T01:10:00', 'date2': '2021-08-13T02:20:00', 'status': 'confirmed'})
