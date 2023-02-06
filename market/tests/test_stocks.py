# -*- coding: utf-8 -*-

import json
import pytest
import flask
import six

from collections import Counter
from hamcrest import assert_that
from mock import patch
from six.moves.urllib.parse import urlencode
from yatest.common import test_output_path

from market.idx.api.backend.idxapi import create_flask_app
from market.idx.api.backend.marketindexer.storage.storage import Storage
from market.idx.api.yatf.resources.idxapi_cfg import IdxApiConf
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import Offer
from market.idx.yatf.resources.stroller_server import StrollerServer

from utils import (
    is_error_response,
    is_success_response,
    is_redirection_response,
    is_bad_response,
)


WAREHOUSE_ID = 145
BAD_WAREHOUSE_ID = 146
SHOP_ID = 10101010


TEST_OFFERS = (
    {'feed': 1, 'offer': 'first', 'count': 25, 'timestamp': 1563147306},
    {'feed': 1, 'offer': 'first-a', 'count': 0, 'timestamp': 1563172785},
    {'feed': 2, 'offer': '2', 'count': 10, 'timestamp': 1563172785},
    {'feed': 3, 'offer': 'third', 'count': 35},
    {'feed': 600, 'offer': six.ensure_text('Проверочный оффер (.,\\/[]-=.)'), 'count': 111, 'timestamp': 1563172884}
)


# Больше не ходим в saas-kv, все запросы обрабатываются строллером
class EmptyStorage(Storage):
    def __init__(self):
        self.__offers = {}
        self.__feeds = {}
        self.__datacamp_offers = {}

    def get_published_offer_by_id(self, feed_id, offer_id):
        return None

    def get_datacamp_offer_by_feed_offer_id(self, feed_id, offer_id):
        return None

    def get_published_session(self, feed_id):
        return None


def offers_params(offers):
    feed_offers = [
        '{}-{}'.format(
            offer['feed'],
            six.ensure_str(offer['offer'])
        )
        for offer in offers
    ]
    return '&'.join(urlencode({'offer': offer}) for offer in feed_offers)


def offers_get_contents(offers):
    feed_offers = [
        '{}-{}'.format(
            offer['feed'],
            six.ensure_str(offer['offer'])
        )
        for offer in offers
    ]
    return json.dumps(dict(offers=feed_offers))


def verify_response(response, expected_offers):
    def as_string(offer, plus_one):
        fields = []
        for k, v in list(offer.items()):
            if k == 'count':
                fields.append(six.ensure_text('=').join(('count', str(v + (0 if not plus_one else 1)))))
                continue
            value = v if k == 'offer' else str(v)
            fields.append(six.ensure_text('=').join((k, value)))
        return six.ensure_text('|').join(sorted(fields))

    def offers_counter(offer_list, plus_one):
        return Counter([as_string(offer, plus_one) for offer in offer_list])

    assert offers_counter(response, False) == offers_counter(expected_offers, True)


@pytest.fixture(scope='module')
def stroller_server():
    data = {}
    for o in TEST_OFFERS:
        offer = Offer()
        offer.identifiers.offer_id = o['offer']
        offer.identifiers.shop_id = SHOP_ID
        offer.identifiers.warehouse_id = WAREHOUSE_ID
        offer.identifiers.feed_id = o['feed']
        offer.stock_info.partner_stocks.count = o['count'] + 1
        if 'timestamp' in o:
            offer.stock_info.partner_stocks.meta.timestamp.FromSeconds(o['timestamp'])

        if SHOP_ID not in data:
            data[SHOP_ID] = {}
        if WAREHOUSE_ID not in data[SHOP_ID]:
            data[SHOP_ID][WAREHOUSE_ID] = {}
        data[SHOP_ID][WAREHOUSE_ID][six.ensure_str(o['offer'])] = offer

    return StrollerServer(data).init()


@pytest.fixture(scope='module')
def idxapi_config_path():
    return test_output_path('config.ini')


@pytest.fixture(scope='module')
def idxapi_config(stroller_server, idxapi_config_path):
    config_params = {
        'stroller.host': stroller_server.host,
        'stroller.port': stroller_server.port,
    }

    return IdxApiConf(**config_params).dump(idxapi_config_path)


@pytest.fixture(scope='module')
def test_app(idxapi_config, idxapi_config_path):
    with patch('market.idx.api.backend.config.get_config_paths', autospec=True, return_value=[idxapi_config_path]):
        yield create_flask_app(EmptyStorage())


def test_single_offer_request(test_app):
    with test_app.test_client() as client:
        for offer in TEST_OFFERS:
            resp = client.get(
                '/v1/stocks?shop_id={}&warehouse_id={}&'.format(SHOP_ID, WAREHOUSE_ID) +
                offers_params([offer])
            )
            assert_that(resp, is_success_response())
            data = flask.json.loads(resp.data)
            verify_response(data, [offer] if 'timestamp' in offer else [])


def test_multi_offer_request(test_app):
    with test_app.test_client() as client:
        resp = client.get(
            '/v1/stocks?shop_id={}&warehouse_id={}&'.format(SHOP_ID, WAREHOUSE_ID) +
            offers_params(TEST_OFFERS)
        )
        assert_that(resp, is_success_response())
        data = flask.json.loads(resp.data)
        valid_offers = [o for o in TEST_OFFERS if 'timestamp' in o]
        verify_response(data, valid_offers)


def test_multi_offer_request_in_body(test_app):
    with test_app.test_client() as client:
        resp = client.get(
            '/v1/stocks?shop_id={}&warehouse_id={}&'.format(SHOP_ID, WAREHOUSE_ID),
            data=offers_get_contents(TEST_OFFERS),
            content_type='application/json'
        )
        assert_that(resp, is_success_response())
        data = flask.json.loads(resp.data)
        valid_offers = [o for o in TEST_OFFERS if 'timestamp' in o]
        verify_response(data, valid_offers)


def test_multi_offer_request_in_body_invalid_json_redirect(test_app):
    invalid_json = json.dumps(dict(someField='123'))

    with test_app.test_client() as client:
        resp = client.get(
            '/v1/stocks?shop_id={}&warehouse_id={}&'.format(SHOP_ID, WAREHOUSE_ID),
            json=invalid_json,
            content_type='application/json'
        )
        assert_that(resp, is_redirection_response('http://localhost:29334/help'))


def test_response_headers(test_app):
    with test_app.test_client() as client:
        resp = client.get(
            '/v1/stocks?shop_id={}&warehouse_id={}&offer=1-first'.format(SHOP_ID, WAREHOUSE_ID)
        )
        assert_that(resp, is_success_response(content_type='application/json; charset=utf-8'))


def test_no_params(test_app):
    with test_app.test_client() as client:
        for request in ('/stocks', '/v1/stocks'):
            response = client.get(request)
            assert_that(response, is_redirection_response('http://localhost:29334/help'))


def test_without_shop_id(test_app):
    """Проверяем, что если пришел запрос без shop_id, то вернётся 400"""
    with test_app.test_client() as client:
        resp = client.get('/v1/stocks?warehouse_id={}&'.format(WAREHOUSE_ID) + offers_params(TEST_OFFERS))
        assert_that(resp, is_bad_response())


def test_without_warehouse_id(test_app):
    """Проверяем, что если пришел запрос без warehouse_id, то вернётся 400"""
    with test_app.test_client() as client:
        resp = client.get('/v1/stocks?shop_id={}&'.format(SHOP_ID) + offers_params(TEST_OFFERS))
        assert_that(resp, is_bad_response())


def test_with_incorrect_warehouse_id(test_app):
    """Проверяем, что если пришел запрос c несуществующим warehouse_id, то вернётся 404"""
    with test_app.test_client() as client:
        resp = client.get(
            '/v1/stocks?shop_id={}&warehouse_id={}&'.format(SHOP_ID, BAD_WAREHOUSE_ID) +
            offers_params(TEST_OFFERS)
        )
        assert_that(resp, is_error_response(code=404))
