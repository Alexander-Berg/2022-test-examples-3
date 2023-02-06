# -*- coding: utf-8 -*-

import pytest

import flask

from hamcrest import (
    assert_that,
    contains,
    all_of,
    has_entry,
)


from mock import patch

from market.idx.api.backend.idxapi import create_flask_app
from market.idx.api.backend.marketindexer.storage.storage import Storage

from utils import (
    is_success_response,
    is_response
)


@pytest.fixture(scope="module")
def test_app():
    return create_flask_app(Storage())


@pytest.fixture(scope='module', params=[None, 'blue'])
def mode(request):
    return 'mode=' + request.param if request.param else ''


def modify_request(request, param):
    if not param:
        return request
    return request + '&' + param if request.find('?') >= 0 else request + '?' + param


def test_get_master(test_app, mode):
    with patch('market.idx.api.backend.blueprints.master.current_master', auto_spec=True, return_value='fender'),\
            test_app.test_client() as client:
        resp = client.get(modify_request('/v1/master', mode))
        data = flask.json.loads(resp.data)

        assert_that(
            (resp.status_code, data),
            contains(
                200,
                all_of(
                    has_entry('current_master', 'fender'),
                    has_entry('self_url', 'http://localhost:29334/v1/master'),
                )
            )
        )


@pytest.mark.parametrize('format_param', ['json', 'xml'])
def test_parse_format(test_app, format_param):
    with test_app.test_request_context('/v1/master?format=' + format_param):
        assert_that(
            (
                flask.request.path,
                flask.request.args['format']
            ),
            contains(
                '/v1/master',
                format_param,
            )
        )


def test_parse_mode(test_app):
    with test_app.test_request_context('/v1/master?mode=blue'):
        assert_that(
            (
                flask.request.path,
                flask.request.args['mode']
            ),
            contains(
                '/v1/master',
                'blue'
            )
        )


XML_ANSWER = (
    '<?xml version="1.0" encoding="utf-8"?>'
    '<master>'
    '<current_master>fender</current_master>'
    '<self_url>http://localhost:29334/v1/master</self_url>'
    '</master>')


testdata = [
    ('', 200, 'application/json; charset=utf-8', '{"current_master":"fender","self_url":"http://localhost:29334/v1/master"}\n'),
    ('?format=json', 200, 'application/json; charset=utf-8', '{"current_master":"fender","self_url":"http://localhost:29334/v1/master"}\n'),
    ('?format=xml', 200, 'application/xml; charset=utf-8', XML_ANSWER),
    ('?format=text', 200, 'text/plain; charset=utf-8', 'fender'),
    ('?format=someformat', 406, 'text/plain; charset=utf-8', '406 Not Acceptable\nrequest mime type is not implemented: someformat'),
]


@pytest.mark.parametrize("format_param, status_code, content_type, data", testdata, ids=['defalt', 'json', 'xml', 'text', 'someformat'])
def test_response_headers(test_app, mode, format_param, status_code, content_type, data):
    with patch('market.idx.api.backend.blueprints.master.current_master', auto_spec=True, return_value='fender'),\
            test_app.test_client() as client:
        resp = client.get(modify_request('/v1/master' + format_param, mode))

        assert_that(
            resp,
            is_response(data, status_code, content_type=content_type)
        )


def test_response_sync_masters(test_app):
    with patch('market.idx.api.backend.blueprints.master.current_master', auto_spec=True, return_value='fender'),\
            test_app.test_client() as client:

        resp = client.get('/v1/master/masters_are_sync')

        assert_that(resp, is_success_response('{"value":true}\n'))


def test_response_async_masters(test_app):
    with patch('market.idx.api.backend.blueprints.master.current_master', auto_spec=True, side_effect=['fender', 'ibanez']),\
            test_app.test_client() as client:

        resp = client.get('/v1/master/masters_are_sync')

        assert_that(resp, is_success_response('{"value":false}\n'))
