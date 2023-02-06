# -*- coding: utf-8 -*-

import pytest

import flask

from hamcrest import assert_that

from market.idx.api.backend.idxapi import create_flask_app
from market.idx.api.backend.marketindexer.storage.storage import Storage

from utils import is_success_response


@pytest.fixture(scope="module")
def test_app():
    return create_flask_app(Storage())


def test_response(test_app):
    with test_app.test_client() as client:
        resp = client.get('/admin/bin_version')
        assert_that(resp, is_success_response())


def test_parse_format_json(test_app):
    with test_app.test_request_context('/admin/bin_version?format=json'):
        assert flask.request.path == '/admin/bin_version'
        assert flask.request.args['format'] == 'json'


def test_response_headers_format_json(test_app):
    with test_app.test_client() as client:
        resp = client.get('/admin/bin_version?format=json')
        assert_that(resp, is_success_response())
