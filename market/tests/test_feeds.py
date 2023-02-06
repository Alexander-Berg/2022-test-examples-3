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


class StorageMock(Storage):
    def get_feeds(self):
        return [1069, 9997, 10002]


@pytest.fixture(scope="module")
def test_app():
    return create_flask_app(StorageMock())


def test_unversioned_redirect(test_app):
    with test_app.test_client() as client:
        resp = client.get('/feeds')
        assert_that(resp, is_error_response(code=404))


def test_get_all_feeds(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds')
        assert_that(resp, is_success_response())

        data = flask.json.loads(resp.data)
        assert data['feeds']
        assert len(data['feeds']) == 3

        assert data['feeds'][0] == 'http://localhost:29334/v1/feeds/1069'
        assert data['feeds'][1] == 'http://localhost:29334/v1/feeds/9997'
        assert data['feeds'][2] == 'http://localhost:29334/v1/feeds/10002'

        assert data['self_url'] == 'http://localhost:29334/v1/feeds'


def test_parse_format_json(test_app):
    with test_app.test_request_context('/v1/feeds?format=json'):
        assert flask.request.path == '/v1/feeds'
        assert flask.request.args['format'] == 'json'


def test_parse_format_xml(test_app):
    with test_app.test_request_context('/v1/feeds?format=xml'):
        assert flask.request.path == '/v1/feeds'
        assert flask.request.args['format'] == 'xml'


def test_response_headers_default_json(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds')
        assert_that(resp, is_success_response(content_type='application/json; charset=utf-8'))


def test_response_headers_format_json(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds?format=json')
        assert_that(resp, is_success_response(content_type='application/json; charset=utf-8'))


def test_response_headers_format_xml(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds?format=xml')
        assert_that(
            resp,
            is_success_response(
                data=contains_string('<?xml version="1.0" encoding="utf-8"?>'),
                content_type='application/xml; charset=utf-8'
            )
        )


def test_response_headers_format_undef(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds?format=someformat')
        assert_that(
            resp,
            is_error_response(
                '406 Not Acceptable\nrequest mime type is not implemented: someformat',
                406
            )
        )
