# -*- coding: utf-8 -*-

import pytest

import flask

from hamcrest import (
    assert_that,
    contains_string,
)

from utils import (
    is_error_response,
    is_success_response,
)


from market.idx.api.backend.idxapi import create_flask_app
from market.idx.api.backend.marketindexer.storage.storage import Storage


@pytest.fixture(scope="module")
def test_app():
    return create_flask_app(Storage())


def test_help(test_app):
    with test_app.test_client() as client:
        resp = client.get('/help')
        assert_that(resp, is_success_response())


def test_help_json(test_app):
    with test_app.test_client() as client:
        resp = client.get('/help?format=json')
        assert_that(resp, is_success_response())

        data = flask.json.loads(resp.data)
        assert data
        assert data['self_url'] == 'http://localhost:29334/help'
        assert len(data['docs'])


def test_all_routes_has_doc(test_app):
    with test_app.test_client() as client:
        resp = client.get('/help?format=json')
        assert_that(resp, is_success_response())
        data = flask.json.loads(resp.data)

        assert data
        for help_doc in data['docs']:
            assert help_doc['methods']
            assert help_doc['url']
            assert help_doc['help']


def test_parse_format_json(test_app):
    with test_app.test_request_context('/help?format=json'):
        assert flask.request.path == '/help'
        assert flask.request.args['format'] == 'json'


def test_parse_format_xml(test_app):
    with test_app.test_request_context('/help?format=xml'):
        assert flask.request.path == '/help'
        assert flask.request.args['format'] == 'xml'


def test_response_headers_default_html(test_app):
    with test_app.test_client() as client:
        resp = client.get('/help')
        assert_that(resp, is_success_response(content_type='text/html; charset=utf-8'))


def test_response_headers_format_html(test_app):
    with test_app.test_client() as client:
        resp = client.get('/help?format=html')
        assert_that(resp, is_success_response(content_type='text/html; charset=utf-8'))


def test_response_headers_format_json(test_app):
    with test_app.test_client() as client:
        resp = client.get('/help?format=json')
        assert_that(resp, is_success_response(content_type='application/json; charset=utf-8'))


def test_response_headers_format_xml(test_app):
    with test_app.test_client() as client:
        resp = client.get('/help?format=xml')
        assert_that(
            resp,
            is_success_response(
                data=contains_string('<?xml version="1.0" encoding="utf-8"?>'),
                content_type='application/xml; charset=utf-8'
            )
        )


def test_response_headers_format_undef(test_app):
    with test_app.test_client() as client:
        resp = client.get('/help?format=someformat')
        assert_that(
            resp,
            is_error_response('406 Not Acceptable\nrequest mime type is not implemented: someformat', 406)
        )
