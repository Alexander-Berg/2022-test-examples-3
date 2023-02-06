# -*- coding: utf-8 -*-

from mail.swat.integration_tests.lib import environments
import library.python.resource as resource
import ticket_parser2.api.v1 as tp2
from sendr_utils import utcnow


import datetime
import json
import re
import requests
import traceback
import unicodedata


def _make_url(path, env, request_type):
    path_str = path[env] if type(path) is dict else path
    url = environments.URL[env][request_type]
    return '{}://{}/{}/{}'.format(url['scheme'], url['host'], url['path'], path_str)


def _get_headers(env, userticket_dbg=None):
    headers = {'Host': environments.URL[env]['client']['host'],
            'User-Agent': 'Integration Testing Tool',
            'X-Yandex-Internal-Request': '1',
            'X-Forwarded-Host': environments.URL[env]['client']['host']}
    if userticket_dbg:
        headers.update({'X-Ya-User-Ticket-Debug': str(userticket_dbg)})
    return headers


def _get_service_ticket(env, self_tvm, secret):
    return tp2.TvmClient(tp2.TvmApiClientSettings(
        self_client_id=int(self_tvm),
        self_secret=secret,
        dsts={'payments-api': environments.TVM[env]['api_client_id']},
    )).get_service_ticket_for('payments-api')


def _is_contains(expected, actual, path):
    if isinstance(expected, list):
        assert type(actual) == list, f'In {path} expected type `list`, but was type `{type(actual)}`'
        assert len(expected) <= len(actual), f'In {path} expected {len(expected)} elements, but was {len(actual)} elements'
        for idx, item in enumerate(expected):
            _is_contains(item, actual[idx], path + [idx])

    elif type(expected) == dict:
        assert type(actual) == dict, f'In {path} expected type `dict`, but was type `{type(actual)}`'
        for key, value in expected.items():
            assert key in actual, f'In {path + [key]} expected `{value}`, but was nothing'
            _is_contains(value, actual[key], path + [key])

    elif type(expected) == str and expected.startswith('regex'):
        assert re.match(expected[6:], str(actual)), f'In {path} expected `{expected}`, but was `{actual}`'

    elif type(expected) == str:
        assert type(actual) == str, f'In {path} expected type `str`, but was type `{type(actual)}`'
        nexpected = unicodedata.normalize('NFKC', expected)
        nactual = unicodedata.normalize('NFKC', actual)
        assert nexpected == nactual, f'In {path} expected `{nexpected}`, but was `{nactual}`'

    else:
        assert type(expected) == type(actual), f'In {path} expected type `{type(expected)}`, but was type `{type(actual)}`'
        assert expected == actual, f'In {path} expected `{expected}`, but was `{actual}`'


class HttpRequestContext:
    def __init__(self):
        self.order_id = None


class HttpRequest:
    def __init__(self):
        self.method = 'get'
        self.req_type = 'internal'
        self.data = {}
        self.params = {}
        self.headers = {}

        self.api_key = ''
        self.csrf_token = False
        self.cookie = ''
        self.oauth = ''
        self.need_auth = True
        self.reference = None
        self.status_codes = [200]
        self.service_ticket = True

    def fill_ctx(self, response, ctx: HttpRequestContext):
        pass

    def __run(self, ctx: HttpRequestContext):
        self.headers.update(_get_headers(self.env))
        if self.service_ticket:
            self.headers['X-Ya-Service-Ticket'] = _get_service_ticket(self.env, self.self_tvm, self.tvm_secret)

        if self.need_auth:
            # assert self.cookie or self.oauth, f'Authorization data is absent, cookie and oauth is empty' ToDo: internal scenario if no cookie
            if self.cookie:
                self.headers['Cookie'] = self.cookie
            if self.oauth:
                self.headers['Authorization'] = f'OAuth {self.oauth}'

        url = _make_url(self.path, self.env, self.req_type)
        response = requests.request(self.method, url, headers=self.headers, params=self.params, json=self.data)

        if self.debug:
            print(f'\t→ {response.request.method} {response.request.path_url}\n\t{response.request.headers}\n\t{response.request.body}')
            print(f'\t← {response.status_code} {response.url}\n\t{response.text}')
        

        assert response.status_code in self.status_codes, f'Request failed, waited for status codes {self.status_codes}, got: {response.status_code}, {response.text}'

        actual = response.json()
        if self.reference:
            reference_file = resource.find(f'{self.env}/{self.reference}')
            assert reference_file, f'JSON of expected result not found {self.reference}'

            expected = json.loads(reference_file.decode('utf-8'))
            if self.debug:
                print(f'\texpected {expected}\n\tactual {actual}')
            _is_contains(expected, actual, [])


        self.fill_ctx(actual, ctx)

    def run(self, ctx: HttpRequestContext):
        try:
            self.__run(ctx)
            print(f'{utcnow()}\t[SUCCESS]\t{self.name}')
        except requests.exceptions.ConnectionError as err:
            print(f'{utcnow()}\t[CON_ERROR]\t{self.name}: {err}')
            if self.debug:
                print(traceback.format_exc())
        except Exception as err:
            print(f'{utcnow()}\t[FAILED]\t{self.name}: {err}')
            if self.debug:
                print(traceback.format_exc())