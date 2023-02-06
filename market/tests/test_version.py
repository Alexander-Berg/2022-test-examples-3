# -*- coding: utf-8 -*-

import pytest

import flask

from hamcrest import (
    assert_that,
    contains_string,
)

from market.idx.api.backend.idxapi import create_flask_app
from market.idx.api.backend.marketindexer.storage.storage import Storage

from utils import (
    is_success_response,
    is_error_response,
)


@pytest.fixture(scope="module")
def test_app():
    return create_flask_app(Storage())


def test_get_version(test_app):
    with test_app.test_client() as client:
        resp = client.get('/version')
        assert_that(resp, is_success_response())

        app_version = str(test_app.config['VERSION'])
        assert resp.headers['X-Market-IDXAPI'] == app_version

        data = flask.json.loads(resp.data)
        assert data['current_version'] == app_version
        assert data['self_url'] == 'http://localhost:29334/version'


def test_parse_format_json(test_app):
    with test_app.test_request_context('/version?format=json'):
        assert flask.request.path == '/version'
        assert flask.request.args['format'] == 'json'


def test_parse_format_xml(test_app):
    with test_app.test_request_context('/version?format=xml'):
        assert flask.request.path == '/version'
        assert flask.request.args['format'] == 'xml'


def test_response_headers_default_json(test_app):
    with test_app.test_client() as client:
        resp = client.get('/version')
        assert_that(resp, is_success_response(content_type='application/json; charset=utf-8'))


def test_response_headers_format_json(test_app):
    with test_app.test_client() as client:
        resp = client.get('/version?format=json')
        assert_that(resp, is_success_response(content_type='application/json; charset=utf-8'))


def test_response_headers_format_xml(test_app):
    with test_app.test_client() as client:
        resp = client.get('/version?format=xml')
        assert_that(
            resp,
            is_success_response(
                data=contains_string('<?xml version="1.0" encoding="utf-8"?'),
                content_type='application/xml; charset=utf-8'
            )
        )


def test_response_headers_format_undef(test_app):
    with test_app.test_client() as client:
        resp = client.get('/version?format=someformat')
        assert_that(resp, is_error_response('406 Not Acceptable\nrequest mime type is not implemented: someformat', 406))
