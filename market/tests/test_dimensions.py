# -*- coding: utf-8 -*-

import flask
import pytest
import six

from collections import Counter
from google.protobuf.json_format import MessageToDict
from hamcrest import assert_that
from mock import patch
from six import BytesIO as StringIO
from six.moves.urllib.parse import urlencode
from yatest.common import test_output_path

from market.idx.api.backend.idxapi import create_flask_app
from market.idx.api.backend.marketindexer.storage.real_storage import RealStorage
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import Offer
from market.proto.idxapi import OfferDimensions_pb2
from market.pylibrary.snappy_protostream import pbsn_reader
from market.idx.yatf.resources.stroller_server import StrollerServer
from market.idx.api.yatf.resources.idxapi_cfg import IdxApiConf

from utils import (
    is_bad_response,
    is_success_response,
    is_error_response,
    is_redirection_response,
)


WAREHOUSE_ID = 145
BAD_WAREHOUSE_ID = 146
SHOP_ID = 10101010


DATACAMP_OFFERS = [
    {
        'identifiers': {
            'offer_id': 'offer1',
            'feed_id': 1,
        },
        'content': {
            'partner': {
                'actual': {
                    'dimensions': {
                        'height_mkm': 120980,
                        'width_mkm': 110000,
                        'length_mkm': 0,
                    },
                    'weight': {
                        'grams': 500,
                    },
                },
            },
        },
    },
    {
        'identifiers': {
            'offer_id': 'offer2',
            'feed_id': 2,
        },
        'content': {
            'partner': {
                'actual': {
                    'dimensions': {
                        'height_mkm': 98000,
                        'width_mkm': 1010000,
                        'length_mkm': 150000,
                    },
                    'weight': {
                        'grams': 12300,
                    },
                },
            },
        },
    },
    {
        'identifiers': {
            'offer_id': 'offer3',
            'feed_id': 3,
        },
        'content': {
            'partner': {
                'original': {
                    'dimensions': {
                        'height_mkm': 99000,
                        'width_mkm': 1020000,
                        'length_mkm': 160000,
                    },
                    'weight': {
                        'grams': 12340,
                    },
                },
            },
        },
    },
]

DATACAMP_OFFERS_WITH_MISSING_PARAMS = [
    # offer without param "dimensions.height_mkm"
    {
        'identifiers': {
            'offer_id': 'empty_height',
            'feed_id': 1,
        },
        'content': {
            'partner': {
                'original': {
                    'dimensions': {
                        'width_mkm': 1020000,
                        'length_mkm': 160000,
                    },
                    'weight': {
                        'grams': 12340,
                    },
                },
            },
        },
    },
    # offer without param "weight"
    {
        'identifiers': {
            'offer_id': 'empty_weight',
            'feed_id': 1,
        },
        'content': {
            'partner': {
                'original': {
                    'dimensions': {
                        'height_mkm': 99000,
                        'width_mkm': 1020000,
                        'length_mkm': 160000,
                    },
                },
            },
        },
    },
]


def datacamp_offers_params(offers):
    feed_offers = []
    for o in offers:
        feed_offers.append(
            str(o['identifiers']['feed_id']) +
            '-' +
            six.ensure_str(o['identifiers']['offer_id'])
    )
    return '&'.join(urlencode({'offer': o}) for o in feed_offers)


def verify_response(response, expected_offers):
    def as_string(offer):
        is_proto_offer = isinstance(offer, OfferDimensions_pb2.OfferDimensions)
        offer_dict = MessageToDict(offer, True, True) if is_proto_offer else offer
        fields = []
        for k, v in list(offer_dict.items()):
            if k == 'yx_shop_offer_id':
                continue
            if k == 'identifiers':
                fields.append(six.ensure_text('=').join(('offer_id', v['offer_id'])))
                fields.append(six.ensure_text('=').join(('feed_id', str(v['feed_id']))))
                continue
            if k == 'content':
                specification = v['partner']['original'] if 'original' in v['partner'] else v['partner']['actual']
                dimensions = specification['dimensions']
                for k_dim, v_dim in list(dimensions.items()):
                    k = k_dim.split('_')[0]
                    fields.append(six.ensure_text('=').join((k, str(float(v_dim + 1)/10000))))
                weight = specification['weight']
                fields.append(six.ensure_text('=').join(('weight', str(float(weight['grams'] + 1)/1000))))
                continue
            key = 'weight' if k == 'weight_gross' else k
            if k == 'offer_id':
                value = six.ensure_text(v) if is_proto_offer else v
            else:
                value = str(v)
            fields.append(six.ensure_text('=').join((key, value)))
        return six.ensure_text('|').join(sorted(fields))

    def offers_counter(offer_list):
        return Counter([as_string(offer) for offer in offer_list])

    assert offers_counter(response) == offers_counter(expected_offers)


def assert_is_empty(response):
    assert_that(response, is_success_response())
    data = flask.json.loads(response.data)
    assert not data


# Больше не ходим в saas-kv, все запросы обрабатываются строллером
class EmptyStorage(RealStorage):
    def __init__(self):
        self.all_offers = {}
        self.all_datacamp_offers = {}

    def get_published_offer_by_id(self, feed_id, offer_id):
        return None

    def get_datacamp_offer_by_feed_offer_id(self, feed_id, offer_id):
        return None


@pytest.fixture(scope='module')
def stroller_server():
    data = {}
    for o in DATACAMP_OFFERS + DATACAMP_OFFERS_WITH_MISSING_PARAMS:
        offer = Offer()
        offer.identifiers.offer_id = o['identifiers']['offer_id']
        offer.identifiers.shop_id = SHOP_ID
        offer.identifiers.warehouse_id = WAREHOUSE_ID
        offer.identifiers.feed_id = o['identifiers']['feed_id']

        src_specification = {}
        dst_specification = {}
        if 'original' in o['content']['partner']:
            src_specification = o['content']['partner']['original']
            dst_specification = offer.content.partner.original
        else:
            src_specification = o['content']['partner']['actual']
            dst_specification = offer.content.partner.actual

        if 'height_mkm' in src_specification['dimensions']:
            dst_specification.dimensions.height_mkm = src_specification['dimensions']['height_mkm'] + 1
        dst_specification.dimensions.width_mkm = src_specification['dimensions']['width_mkm'] + 1
        dst_specification.dimensions.length_mkm = src_specification['dimensions']['length_mkm'] + 1
        if 'weight' in src_specification:
            weight = src_specification['weight']
            dst_specification.weight.grams = weight['grams'] + 1

        if SHOP_ID not in data:
            data[SHOP_ID] = {}
        if WAREHOUSE_ID not in data[SHOP_ID]:
            data[SHOP_ID][WAREHOUSE_ID] = {}
        data[SHOP_ID][WAREHOUSE_ID][o['identifiers']['offer_id']] = offer

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


@pytest.fixture(scope="module")
def test_app(idxapi_config, idxapi_config_path):
    with patch('market.idx.api.backend.config.get_config_paths', autospec=True, return_value=[idxapi_config_path]):
        yield create_flask_app(EmptyStorage())


def test_valid_offers(test_app):
    with test_app.test_client() as client:
        resp = client.get(
            '/v1/dimensions?shop_id={}&warehouse_id={}&'.format(SHOP_ID, WAREHOUSE_ID) +
            datacamp_offers_params(DATACAMP_OFFERS)
        )
        assert_that(resp, is_success_response())
        data = flask.json.loads(resp.data)
        verify_response(data, DATACAMP_OFFERS)


def est_valid_offers_pbsn(test_app):
    DIMENSIONS_MAGIC = 'DIMS'

    with test_app.test_client() as client:
        resp = client.get(
            '/v1/dimensions?shop_id={}&warehouse_id={}&'.format(SHOP_ID, WAREHOUSE_ID) +
            datacamp_offers_params(DATACAMP_OFFERS) + '&format=pbsn'
        )
        assert_that(resp, is_success_response())
        offers_generator = pbsn_reader(StringIO(resp.data), DIMENSIONS_MAGIC, OfferDimensions_pb2.Offers)
        for offers in offers_generator:
            verify_response(offers.offer, DATACAMP_OFFERS)


def test_missing_offers(test_app):
    with test_app.test_client() as client:
        resp = client.get(
            '/v1/dimensions?shop_id={}&warehouse_id={}&'.format(SHOP_ID, WAREHOUSE_ID) +
            datacamp_offers_params([{
                'identifiers': {
                    'offer_id': '0987644-missing',
                    'feed_id': 1,
                },
            }])
        )
        assert_is_empty(resp)


def test_invalid_offers(test_app):
    with test_app.test_client() as client:
        resp = client.get(
            '/v1/dimensions?shop_id={}&warehouse_id={}&'.format(SHOP_ID, WAREHOUSE_ID) +
            datacamp_offers_params(DATACAMP_OFFERS_WITH_MISSING_PARAMS)
        )
        assert_is_empty(resp)


def test_mixed_offers(test_app):
    with test_app.test_client() as client:
        offers = DATACAMP_OFFERS + DATACAMP_OFFERS_WITH_MISSING_PARAMS
        resp = client.get(
            '/v1/dimensions?shop_id={}&warehouse_id={}&'.format(SHOP_ID, WAREHOUSE_ID) +
            datacamp_offers_params(offers)
        )
        assert_that(resp, is_success_response())
        data = flask.json.loads(resp.data)
        verify_response(data, DATACAMP_OFFERS)


def test_no_params(test_app):
    with test_app.test_client() as client:
        for request in ('/dimensions', '/v1/dimensions'):
            response = client.get(request)
            assert_that(
                response,
                is_redirection_response(
                    'http://localhost:29334/help',
                    code=302
                )
            )


def test_without_shop_id(test_app):
    """Проверяем, что если пришел запрос без shop_id, то вернётся 400"""
    with test_app.test_client() as client:
        resp = client.get('/v1/dimensions?warehouse_id={}&'.format(WAREHOUSE_ID) + datacamp_offers_params(DATACAMP_OFFERS))
        assert_that(resp, is_bad_response())


def test_without_warehouse_id(test_app):
    """Проверяем, что если пришел запрос без warehouse_id, то вернётся 400"""
    with test_app.test_client() as client:
        resp = client.get('/v1/dimensions?shop_id={}&'.format(SHOP_ID) + datacamp_offers_params(DATACAMP_OFFERS))
        assert_that(resp, is_bad_response())


def test_with_incorrect_warehouse_id(test_app):
    """Проверяем, что если пришел запрос с несуществующим warehouse_id, то вернётся 404"""
    with test_app.test_client() as client:
        resp = client.get(
            '/v1/dimensions?shop_id={}&warehouse_id={}&'.format(SHOP_ID, BAD_WAREHOUSE_ID) +
            datacamp_offers_params(DATACAMP_OFFERS)
        )
        assert_that(resp, is_error_response(code=404))


def test_parse_format_json(test_app):
    with test_app.test_request_context(
        '/v1/dimensions?shop_id={}&warehouse_id={}&offer=123-321&format=json'
        .format(SHOP_ID, WAREHOUSE_ID)
    ):
        assert flask.request.path == '/v1/dimensions'
        assert flask.request.args['format'] == 'json'
        assert flask.request.args.get('shop_id', type=int) == SHOP_ID
        assert flask.request.args.get('warehouse_id', type=int) == WAREHOUSE_ID
        assert flask.request.args['offer'] == '123-321'


def test_parse_format_pbsn(test_app):
    with test_app.test_request_context(
        '/v1/dimensions?shop_id={}&warehouse_id={}&offer=123-321&format=pbsn'
        .format(SHOP_ID, WAREHOUSE_ID)
    ):
        assert flask.request.path == '/v1/dimensions'
        assert flask.request.args['format'] == 'pbsn'
        assert flask.request.args.get('shop_id', type=int) == SHOP_ID
        assert flask.request.args.get('warehouse_id', type=int) == WAREHOUSE_ID
        assert flask.request.args['offer'] == '123-321'


def test_response_headers_default_json(test_app):
    with test_app.test_client() as client:
        resp = client.get(
            '/v1/dimensions?shop_id={}&warehouse_id={}&offer=123-321'.format(SHOP_ID, WAREHOUSE_ID)
        )
        assert_that(resp, is_success_response(content_type='application/json; charset=utf-8'))


def test_response_headers_format_json(test_app):
    with test_app.test_client() as client:
        resp = client.get(
            '/v1/dimensions?shop_id={}&warehouse_id={}&offer=123-321&format=json'.format(SHOP_ID, WAREHOUSE_ID)
        )
        assert_that(resp, is_success_response(content_type='application/json; charset=utf-8'))


def test_response_headers_format_pbsn(test_app):
    with test_app.test_client() as client:
        resp = client.get(
            '/v1/dimensions?shop_id={}&warehouse_id={}&offer=123-321&format=pbsn'.format(SHOP_ID, WAREHOUSE_ID)
        )
        assert_that(resp, is_success_response(content_type='application/octet-stream; charset=utf-8'))


def test_response_headers_format_undef(test_app):
    with test_app.test_client() as client:
        resp = client.get(
            '/v1/dimensions?shop_id={}&warehouse_id={}&offer=123-321&format=weird_format'.format(SHOP_ID, WAREHOUSE_ID)
        )
        assert_that(
            resp,
            is_error_response(
                code=406,
                data='406 Not Acceptable\nrequest mime type is not implemented: weird_format'
            )
        )
