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


class StorageMock(Storage):
    def get_published_session(self, feed_id):
        if feed_id == '1069':
            return {
                'feed': 1069,
                'published': 1516183380,
                'pub_meta': None,
                'finished': 1516180020,
                'fin_meta': None,
            }

        if feed_id == '1070':
            return {
                'feed': 1070,
                'published': 1516183380,
                'pub_meta': None,
                'finished': None,
                'fin_meta': None,
            }

        return {}

    def get_finished_session(self, feed_id):
        return self.get_published_session(feed_id)


@pytest.fixture(scope='module')
def test_app():
    return create_flask_app(StorageMock())


def test_sessions_not_found(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/somefeed/sessions')
        assert resp.status_code == 404


def test_sessions_ok(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/sessions')
        data = flask.json.loads(resp.data)

        assert data['sessions'][0] == "http://localhost:29334/v1/feeds/1069/sessions/published"
        assert data['sessions'][1] == "http://localhost:29334/v1/feeds/1069/sessions/finished"


def test_unversioned_get_published_session_redirect(test_app):
    with test_app.test_client() as client:
        resp = client.get('/feeds/1069/sessions/published')
        assert resp.status_code == 404  # route removed


def test_get_published_session(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/sessions/published')
        data = flask.json.loads(resp.data)

        assert data['feed_id'] == 1069
        assert data['session_id'] == 1516183380
        assert data['self_url'] == 'http://localhost:29334/v1/feeds/1069/sessions/published'


def test_get_published_session_for_unknown_feed(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/111/sessions/published')
        assert resp.status_code == 404


def test_get_finished_session(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/sessions/finished')
        data = flask.json.loads(resp.data)

        assert data['feed_id'] == 1069
        assert data['session_id'] == 1516180020
        assert data['self_url'] == 'http://localhost:29334/v1/feeds/1069/sessions/finished'


def test_get_empty_finished_session(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1070/sessions/finished')
        assert resp.status_code == 404


def test_parse_format_json(test_app):
    with test_app.test_request_context('/v1/feeds/1069/sessions/published?format=json'):
        assert flask.request.path == '/v1/feeds/1069/sessions/published'
        assert flask.request.args['format'] == 'json'


def test_parse_format_xml(test_app):
    with test_app.test_request_context('/v1/feeds/1069/sessions/published?format=xml'):
        assert flask.request.path == '/v1/feeds/1069/sessions/published'
        assert flask.request.args['format'] == 'xml'


def test_response_headers_default_json(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/sessions/published')
        assert_that(resp, is_success_response(content_type='application/json; charset=utf-8'))


def test_response_headers_format_json(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/sessions/published?format=json')
        assert_that(resp, is_success_response(content_type='application/json; charset=utf-8'))


def test_response_headers_format_xml(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/sessions/published?format=xml')
        assert_that(
            resp,
            is_success_response(
                data=contains_string('<?xml version="1.0" encoding="utf-8"?>'),
                content_type='application/xml; charset=utf-8'
            )
        )


def test_response_headers_format_undef(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/sessions/published?format=someformat')
        assert_that(resp, is_error_response('406 Not Acceptable\nrequest mime type is not implemented: someformat', 406))
