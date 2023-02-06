# -*- coding: utf-8 -*-

import base64
import flask
import pytest

from hamcrest import (
    assert_that,
    contains_string,
)

from utils import (
    is_success_response,
    is_error_response,
)

from market.idx.api.backend.idxapi import create_flask_app
from market.idx.api.backend.marketindexer.storage.storage import Storage
from market.proto.indexer import GenerationLog_pb2


class StorageMock(Storage):
    def get_feed_qoffer_by_id(self, feed_id, offer_id):
        if feed_id != '1069' or offer_id != '1':
            return None
        fa = GenerationLog_pb2.Record()
        fa.binary_ware_md5 = base64.b64decode('4bMJfsb7zTtV7gLLUBANyg==')
        fa.offers_robot_session = 1482938100

        fq = GenerationLog_pb2.Record()
        fq.binary_price.id = "USD"
        fq.binary_price.plus = 1
        fq.binary_price.price = 14971039390000000
        fq.binary_price.rate = "CBRF"

        return {
            'offer_id': '1',
            'F:A': fa,
            'F:Q': fq,
        }


@pytest.fixture(scope='module')
def test_app():
    return create_flask_app(StorageMock())


def test_qoffer_id_not_found_wrong_feed_id(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/SOMEFEED/quick/session/offers/1')
        assert_that(resp, is_error_response('404 Not Found', 404))


def test_qoffer_id_not_found_wrong_offer_id(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/quick/session/offers/SOMEOFFER')
        assert_that(resp, is_error_response('404 Not Found', 404))


def test_get_qoffer_id(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/quick/session/offers/1')
        assert_that(resp, is_success_response())

        data = flask.json.loads(resp.data)
        assert data['offer_id'] == '1'
        assert data['F:A']
        assert data['F:A']['binary_ware_md5'] == '4bMJfsb7zTtV7gLLUBANyg=='
        assert data['F:A']['offers_robot_session'] == 1482938100
        assert data['F:Q']
        assert data['F:Q']['binary_price']
        assert data['F:Q']['binary_price']['id'] == "USD"
        assert data['F:Q']['binary_price']['plus'] == 1
        assert data['F:Q']['binary_price']['price'] == '14971039390000000'
        assert data['F:Q']['binary_price']['rate'] == "CBRF"
        assert data['self_url'] == 'http://localhost:29334/v1/feeds/1069/quick/session/offers/1'


def test_parse_format_json(test_app):
    with test_app.test_request_context('/v1/feeds/1069/quick/session/offers/1?format=json'):
        assert flask.request.path == '/v1/feeds/1069/quick/session/offers/1'
        assert flask.request.args['format'] == 'json'


def test_parse_format_xml(test_app):
    with test_app.test_request_context('/v1/feeds/1069/quick/session/offers/1?format=xml'):
        assert flask.request.path == '/v1/feeds/1069/quick/session/offers/1'
        assert flask.request.args['format'] == 'xml'


def test_response_headers_default_json(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/quick/session/offers/1')
        assert_that(resp, is_success_response(content_type='application/json; charset=utf-8'))


def test_response_headers_format_json(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/quick/session/offers/1?format=json')
        assert_that(resp, is_success_response(content_type='application/json; charset=utf-8'))


def test_response_headers_format_xml(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/quick/session/offers/1?format=xml')
        assert_that(
            resp,
            is_success_response(
                data=contains_string('<?xml version="1.0" encoding="utf-8"?>'),
                content_type='application/xml; charset=utf-8'
            )
        )


def test_response_headers_format_undef(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/quick/session/offers/1?format=someformat')
        assert_that(resp, is_error_response('406 Not Acceptable\nrequest mime type is not implemented: someformat', 406))
