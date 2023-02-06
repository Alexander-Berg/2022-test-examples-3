# -*- coding: utf-8 -*-

import flask
import os
import pytest
import struct
import time

from yatest.common import source_path
from hamcrest import (
    assert_that,
    contains_string,
)

from utils import (
    is_bad_response,
    is_error_response,
    is_not_found_response,
    is_redirection_response,
    is_success_response,
)

from market.idx.api.backend.blueprints.offer import toxml
from market.idx.api.backend.idxapi import create_flask_app
from market.idx.api.backend.marketindexer import exchange
from market.idx.api.backend.marketindexer import price_expression
from market.idx.api.backend.marketindexer.storage.real_storage import RealStorage
from market.idx.api.backend.marketindexer.storage.resources import SimpleResource
from market.proto.feedparser.deprecated import OffersData_pb2
from market.proto.indexer import GenerationLog_pb2
from market.pylibrary.marketprices import price_expression as pe


def get_crates():
    cratespath = os.path.abspath(source_path('market/idx/api/tests/currency_rates.xml'))
    crates = SimpleResource(cratespath, exchange.load).get()
    return crates


def get_test_offer(feed_id):
    delivery = OffersData_pb2.Offer()
    delivery.DeliveryOptions.add()
    delivery.DeliveryOptions[0].Cost = 600
    delivery.DeliveryOptions[0].DaysMax = 3
    delivery.DeliveryOptions[0].DaysMin = 2
    delivery.DeliveryOptions[0].OrderBeforeHour = 12

    offer = {
        'downloaded': time.time(),
        'feed_id': feed_id,
        'last_checked': 'last_checked',
        'session': '20170610_2100',
        'title': 'привет',
        'description': 'мир',
        'ru_price': 1234,
        'shop_price': 1234,
        'shop_currency': 'RUR',
        'available': 1,
        'URL': 'http://',
        'HasDelivery': True,
        'DeliveryCurrency': 'RUR',
        'DeliveryOptions': delivery.DeliveryOptions
    }
    return offer


class MockStorage(RealStorage):
    def __init__(self, crates):
        self._cexchange = crates

    def _deserialize_qoffer(self, qoffer, feed_id):
        def pack_unsigned_int(ui):
            return struct.pack('=I', ui)

        qoffer['offer_id'] = qoffer['offer_id'].replace(str(pack_unsigned_int(int(feed_id))), '')

        if qoffer.get('F:A') is not None:
            a_proto = GenerationLog_pb2.Record()
            a_proto.ParseFromString(qoffer['F:A'])
            qoffer['F:A'] = a_proto

        if qoffer.get('F:Q') is not None:
            q_proto = GenerationLog_pb2.Record()
            q_proto.ParseFromString(qoffer['F:Q'])
            qoffer['F:Q'] = q_proto

            qoffer['price_expression'] = pe.get_price_expression_from_binary_price(qoffer['F:Q'].binary_price)
            price = price_expression.calculate(qoffer['price_expression'], self._cexchange)
            (qoffer['shop_price'], qoffer['shop_currency']) = price
            qoffer['ru_price'] = price_expression.PriceCurrency(
                qoffer['shop_price'], qoffer['shop_currency']
            ).get_price_by_currency('RUR', self._cexchange)

        return qoffer

    def get_offer(self, feed_id, offer_id, logger=None):
        if feed_id is None:
            feed_id = '1069'
        return get_test_offer(int(feed_id))

    def get_feed_qsession(self, feed_id):
        if feed_id == '1069':
            return {
                'session_id': '1069_00000000_0000',
                'data:session': '20170613_2000',
            }
        if feed_id == '1070':
            return {
                'session_id': '1070_00000000_0000',
                'data:session': '20170608_2100',
            }

    def get_feed_qoffer_by_id(self, feed_id, offer_id):
        if offer_id == '1':
            fq = GenerationLog_pb2.Record()
            fq.binary_price.id = "USD"
            fq.binary_price.plus = 1
            fq.binary_price.price = 10000000
            fq.binary_price.rate = "CBRF"
            qoffer = {
                'offer_id': '1',
                'F:Q': fq.SerializeToString(),
            }
            return self._deserialize_qoffer(qoffer, feed_id)

        if offer_id == '3':
            fq = GenerationLog_pb2.Record()
            fq.binary_price.price = 1009900000
            qoffer = {
                'offer_id': '3',
                'F:Q': fq.SerializeToString(),
            }
            return self._deserialize_qoffer(qoffer, feed_id)

        if offer_id == '5':
            fq = GenerationLog_pb2.Record()
            fq.regional_delivery.add()
            fq.regional_delivery[0].Currency = 'USD'
            fq.regional_delivery[0].DeliveryOptions.add()
            fq.regional_delivery[0].DeliveryOptions[0].price = 100
            fq.regional_delivery[0].DeliveryOptions[0].min_days = 3
            fq.regional_delivery[0].DeliveryOptions[0].max_days = 5
            fq.regional_delivery[0].DeliveryOptions[0].order_before = 1

            fq.regional_delivery[0].DeliveryOptions.add()
            fq.regional_delivery[0].DeliveryOptions[1].price = 10000
            fq.regional_delivery[0].DeliveryOptions[1].min_days = 1
            fq.regional_delivery[0].DeliveryOptions[1].max_days = 1
            fq.regional_delivery[0].DeliveryOptions[1].order_before = 360

            qoffer = {
                'offer_id': '3',
                'F:Q': fq.SerializeToString(),
            }
            return self._deserialize_qoffer(qoffer, feed_id)


@pytest.fixture(scope="module")
def test_app():
    return create_flask_app(MockStorage(get_crates()))


def test_price_was_not_update_with_qsessions_not_found(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/smart/offer?feed_id=1071&offer_id=1&format=json')
        assert_that(resp, is_success_response())

        data = flask.json.loads(resp.data)
        assert data['feed_id'] == 1071
        assert data['ru_price'] == 1234
        assert data['shop_currency'] == 'RUR'
        assert data['shop_price'] == 1234


def test_price_was_not_updated_with_older_qsession(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/smart/offer?feed_id=1070&offer_id=1&format=json')
        assert_that(resp, is_success_response())

        data = flask.json.loads(resp.data)
        assert data['feed_id'] == 1070
        assert data['ru_price'] == 1234
        assert data['shop_currency'] == 'RUR'
        assert data['shop_price'] == 1234


def test_price_was_delete_cause_qoffer_not_found(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/smart/offer?feed_id=1069&offer_id=SOMEOFFER&format=json')
        assert_that(
            resp,
            is_not_found_response('404 Not Found')
        )


def test_price_was_update_with_qoffer_simple(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/smart/offer?feed_id=1069&offer_id=3&format=json')
        assert_that(resp, is_success_response())

        data = flask.json.loads(resp.data)
        assert data['feed_id'] == 1069
        assert data['ru_price'] == 100.99
        assert data['shop_currency'] == 'RUR'
        assert data['shop_price'] == 100.99


def test_price_was_update_with_qoffer_currency(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/smart/offer?feed_id=1069&offer_id=1&format=json')
        assert_that(resp, is_success_response())

        data = flask.json.loads(resp.data)
        assert data['feed_id'] == 1069
        assert data['ru_price'] == 29.578759
        assert data['shop_currency'] == 'RUR'
        assert data['shop_price'] == 29.578759


def test_delivery_was_not_update(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/smart/offer?feed_id=1069&offer_id=1&format=json')
        assert_that(resp, is_success_response())

        data = flask.json.loads(resp.data)
        assert data['feed_id'] == 1069
        assert data['HasDelivery'] is True
        assert data['DeliveryCurrency'] == 'RUR'
        assert data['DeliveryOptions']
        assert len(data['DeliveryOptions']) == 1

        assert data['DeliveryOptions'][0]['Cost'] == 600
        assert data['DeliveryOptions'][0]['DaysMax'] == 3
        assert data['DeliveryOptions'][0]['DaysMin'] == 2
        assert data['DeliveryOptions'][0]['OrderBeforeHour'] == 12


def test_delivery_was_update_change(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/smart/offer?feed_id=1069&offer_id=5&format=json')
        assert_that(resp, is_success_response())

        data = flask.json.loads(resp.data)
        assert data['feed_id'] == 1069
        assert data['HasDelivery'] is True
        assert data['DeliveryCurrency'] == 'USD'
        assert data['DeliveryOptions']
        assert len(data['DeliveryOptions']) == 2

        assert data['DeliveryOptions'][0]['Cost'] == 100
        assert data['DeliveryOptions'][0]['DaysMax'] == 5
        assert data['DeliveryOptions'][0]['DaysMin'] == 3
        assert data['DeliveryOptions'][0]['OrderBeforeHour'] == 1

        assert data['DeliveryOptions'][1]['Cost'] == 10000
        assert data['DeliveryOptions'][1]['DaysMax'] == 1
        assert data['DeliveryOptions'][1]['DaysMin'] == 1
        assert data['DeliveryOptions'][1]['OrderBeforeHour'] == 360


def test_toxml():
    offer = get_test_offer(1)
    assert toxml(offer)


def test_unversioned_redirect(test_app):
    with test_app.test_client() as client:
        resp = client.get('/offer')
        assert_that(
            resp,
            is_redirection_response(
                'http://localhost:29334/v1/smart/offer?'
                'format=text'
            )
        )


def test_unversioned_redirect_with_params(test_app):
    with test_app.test_client() as client:
        resp = client.get('/offer?feed_id=1069&offer_id=1')
        assert_that(
            resp,
            is_redirection_response(
                'http://localhost:29334/v1/smart/offer?'
                'feed_id=1069&'
                'format=text&'
                'offer_id=1'
            )
        )


def test_no_offer_id(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/smart/offer?feed_id=1069')
        assert_that(resp, is_bad_response('400 Bad Request\nNo offer_id'))


def test_parse_format_json(test_app):
    with test_app.test_request_context('/v1/smart/offer?feed_id=1069&offer_id=1&format=json'):
        assert flask.request.path == '/v1/smart/offer'
        assert flask.request.args['format'] == 'json'
        assert flask.request.args['feed_id'] == '1069'
        assert flask.request.args['offer_id'] == '1'


def test_parse_format_xml(test_app):
    with test_app.test_request_context('/v1/smart/offer?feed_id=1069&offer_id=1&format=xml'):
        assert flask.request.path == '/v1/smart/offer'
        assert flask.request.args['format'] == 'xml'
        assert flask.request.args['feed_id'] == '1069'
        assert flask.request.args['offer_id'] == '1'


def test_parse_format_text(test_app):
    with test_app.test_request_context('/v1/smart/offer?feed_id=1069&offer_id=1&format=text'):
        assert flask.request.path == '/v1/smart/offer'
        assert flask.request.args['format'] == 'text'
        assert flask.request.args['feed_id'] == '1069'
        assert flask.request.args['offer_id'] == '1'


def test_response_headers_default_json(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/smart/offer?feed_id=1069&offer_id=1')
        assert_that(
            resp,
            is_success_response(
                content_type='application/json; charset=utf-8'
            )
        )


def test_response_headers_format_json(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/smart/offer?feed_id=1069&offer_id=1&format=json')

        assert_that(
            resp,
            is_success_response(
                content_type='application/json; charset=utf-8'
            )
        )


def test_response_headers_format_xml(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/smart/offer?feed_id=1069&offer_id=1&format=xml')

        assert_that(
            resp,
            is_success_response(
                data=contains_string('<?xml version="1.0" encoding="utf-8"?>'),
                content_type='application/xml; charset=utf-8'
            )
        )


def test_response_headers_format_text(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/smart/offer?feed_id=1069&offer_id=1&format=text')

        assert_that(
            resp,
            is_success_response(
                data=contains_string("<?xml version='1.0' encoding='utf-8'?>"),
                content_type='text/plain; charset=utf-8'
            )
        )


def test_response_headers_format_undef(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/smart/offer?feed_id=1069&offer_id=1&format=someformat')
        assert_that(
            resp,
            is_error_response('406 Not Acceptable\nrequest mime type is not implemented: someformat', 406)
        )
