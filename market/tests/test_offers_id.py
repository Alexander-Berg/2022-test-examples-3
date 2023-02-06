# -*- coding: utf-8 -*-

import base64
import flask
import pytest
import six

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
from market.proto.feedparser.deprecated import OffersData_pb2


class StorageMock(Storage):
    def get_published_offer_by_id(self, feed_id, offer_id):
        if feed_id != '1069' or offer_id != '1':
            return None

        offer_info = OffersData_pb2.Offer()
        offer_info.Comment = 'some text comment'
        offer_info.DeliveryOptions.add()
        offer_info.DeliveryOptions[0].Cost = 300.0
        offer_info.DeliveryOptions[0].DaysMax = 2
        offer_info.shop_name = 'Я Тестовый шоп 4 edit 21^39 090117'
        offer_info.price_expression = '3.000000 1 0 RUR RUR'
        offer_info.yx_shop_name = 'Я Тестовый шоп 4 edit 21^39 090117'
        offer_info.yx_shop_offer_id = '1'

        return {
            "offer_id": "1",
            "data:offer": offer_info,
            "data:recs": base64.b64decode('rNsdaX+EX24IWdr6XBR9T0uYfQ89NYu8a3iDGxIOxxxY7+zLCWcWinI5I0EdnJxcz3QCgx0YRcLrpC1EciwRriYT8Vuju/aR8k3Iz7tOMTY='),
        }


@pytest.fixture(scope='module')
def test_app():
    return create_flask_app(StorageMock())


def test_unversioned_redirect(test_app):
    with test_app.test_client() as client:
        resp = client.get('/feeds/1069/sessions/published/offers/1')
        assert_that(resp, is_error_response(code=404))


def test_offer_id_not_found_wrong_feed_id(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/SOMEFEED/sessions/published/offers/1')
        assert_that(resp, is_error_response('404 Not Found', 404))


def test_offer_id_not_found_wrong_session_id(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/sessions/SOMESESSION/offers/1')
        assert_that(resp, is_error_response(code=404))


def test_offer_id_not_found_wrong_offer_id(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/sessions/published/offers/SOMEOFFER')
        assert_that(resp, is_error_response('404 Not Found', 404))


def test_get_offer_id(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/sessions/published/offers/1')
        assert_that(resp, is_success_response())

        data = flask.json.loads(resp.data)
        assert data['offer_id'] == '1'
        assert data['self_url'] == 'http://localhost:29334/v1/feeds/1069/sessions/published/offers/1'
        assert data['delivery_url'] == 'http://localhost:29334/v1/feeds/1069/sessions/published/offers/1/delivery'

        assert data['data:offer']
        offer_info = data['data:offer']
        offer_info['Comment'] = 'some text comment'
        assert len(offer_info['DeliveryOptions']) == 1
        assert offer_info['DeliveryOptions'][0]['Cost'] == 300.0
        assert offer_info['DeliveryOptions'][0]['DaysMax'] == 2
        assert offer_info['shop_name'] == six.ensure_text('Я Тестовый шоп 4 edit 21^39 090117')
        assert offer_info['price_expression'] == '3.000000 1 0 RUR RUR'
        assert offer_info['yx_shop_name'] == six.ensure_text('Я Тестовый шоп 4 edit 21^39 090117')
        assert offer_info['yx_shop_offer_id'] == '1'


def test_get_offer_id_published_session(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/sessions/published/offers/1')
        assert_that(resp, is_success_response())

        data = flask.json.loads(resp.data)
        assert data['offer_id'] == '1'
        assert data['self_url'] == 'http://localhost:29334/v1/feeds/1069/sessions/published/offers/1'

        assert data['data:offer']
        offer_info = data['data:offer']
        offer_info['Comment'] = 'some text comment'
        assert len(offer_info['DeliveryOptions']) == 1
        assert offer_info['DeliveryOptions'][0]['Cost'] == 300.0
        assert offer_info['DeliveryOptions'][0]['DaysMax'] == 2
        assert offer_info['shop_name'] == six.ensure_text('Я Тестовый шоп 4 edit 21^39 090117')
        assert offer_info['price_expression'] == '3.000000 1 0 RUR RUR'
        assert offer_info['yx_shop_name'] == six.ensure_text('Я Тестовый шоп 4 edit 21^39 090117')
        assert offer_info['yx_shop_offer_id'] == '1'


def test_get_offer_id_published_session_not_found(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1070/sessions/published/offers/1')
        assert_that(resp, is_error_response('404 Not Found', 404))


def test_data_recs(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/sessions/published/offers/1')
        assert_that(resp, is_success_response())

        data = flask.json.loads(resp.data)
        assert data['data:recs']
        assert data['data:recs']['rec_ware_md5_base64']
        recs = data['data:recs']['rec_ware_md5_base64']
        assert len(recs) == 5
        expected_recs = [
            'rNsdaX-EX24IWdr6XBR9Tw',
            'S5h9Dz01i7xreIMbEg7HHA',
            'WO_sywlnFopyOSNBHZycXA',
            'z3QCgx0YRcLrpC1EciwRrg',
            'JhPxW6O79pHyTcjPu04xNg',
        ]
        assert recs == expected_recs


def test_parse_format_json(test_app):
    with test_app.test_request_context('/v1/feeds/1069/sessions/published/offers/1?format=json'):
        assert flask.request.path == '/v1/feeds/1069/sessions/published/offers/1'
        assert flask.request.args['format'] == 'json'


def test_parse_format_xml(test_app):
    with test_app.test_request_context('/v1/feeds/1069/sessions/published/offers/1?format=xml'):
        assert flask.request.path == '/v1/feeds/1069/sessions/published/offers/1'
        assert flask.request.args['format'] == 'xml'


def test_response_headers_default_json(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/sessions/published/offers/1')
        assert_that(
            resp,
            is_success_response(content_type='application/json; charset=utf-8')
        )


def test_response_headers_format_json(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/sessions/published/offers/1?format=json')
        assert_that(
            resp,
            is_success_response(content_type='application/json; charset=utf-8')
        )


def test_response_headers_format_xml(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/sessions/published/offers/1?format=xml')
        assert_that(
            resp,
            is_success_response(
                content_type='application/xml; charset=utf-8',
                data=contains_string('<?xml version="1.0" encoding="utf-8"?>')
            )
        )


def test_response_headers_format_undef(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/sessions/published/offers/1?format=someformat')
        assert_that(resp, is_error_response('406 Not Acceptable\nrequest mime type is not implemented: someformat', 406))
