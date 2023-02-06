# -*- coding: utf-8 -*-

import os
import time
import pytest

from hamcrest import (
    assert_that,
    equal_to,
)

from lxml import etree

from yatest.common import source_path

import flask
from market.proto.indexer import GenerationLog_pb2
from market.proto.feedparser.deprecated.OffersData_pb2 import Offer

from market.idx.api.backend.idxapi import create_flask_app
from market.idx.api.backend.marketindexer.storage.builders import OfferBuilder
from market.idx.api.backend.marketindexer.storage.builders import SessionBuilder
from market.idx.api.backend.marketindexer.storage.resources import SimpleResource
from market.idx.api.backend.marketindexer.storage.meta_storage import MetaStorage
from market.idx.api.backend.marketindexer.storage.real_storage import RealStorage
from market.idx.api.backend.marketindexer.storage.utils import ts_to_session_id
from market.idx.api.backend.marketindexer import exchange

from utils import (
    is_success_response,
)

# Время, относительно которого проверяются все утверждения
NOW_TIME = time.time()

# Параметры несуществующего документа
NOT_FOUND_FEED_ID = 777
NOT_FOUND_OFFER_ID = '111'


def make_saas_kv_offer_data(feed_id, offer_id):
    data = {
        'qoffer_{}/{}'.format(feed_id, offer_id): {
            'b64:qdata': GenerationLog_pb2.Record()
        },
        'offer_{}/{}'.format(feed_id, offer_id): {
            'b64:offer': Offer(title='offer_titile_{}'.format(offer_id))
        }
    }
    return data


def make_saas_kv_feed_session(feed_id, timestamp, quick_timestamp):
    saas_kv = {
        'qfeed_{}'.format(feed_id): {
            'modification_time': quick_timestamp,
            'check_time': quick_timestamp
        },
        'feed_{}'.format(feed_id): {
            'modification_time': timestamp,
            'check_time': timestamp
        }
    }

    legacy_qsession = {
        'session_id': '{}_{}'.format(feed_id, ts_to_session_id(quick_timestamp)),
        'data:session': ts_to_session_id(quick_timestamp),
    }

    return {
        'saas_kv': saas_kv,
        'legacy_qsession': legacy_qsession
    }


def make_legacy_storage_data(feed_id, offer_id, price, timestamp):
    data = {
        'offer_id': offer_id,
        'feed_id': feed_id,
        'session': ts_to_session_id(timestamp),
        'title': 'offer_title_{}'.format(offer_id),
        'ru_price': price,
        'shop_price': price,
        'shop_currency': 'RUR',
    }
    return data


def make_saas_hub_price(price):
    data = {
        "plus": 0,
        "value": price,
        "currency": "RUR",
        "ref_currency": "RUR",
        "rate": "1"
    }
    return data


FEED_IDS = [1069]
OFFER_IDS = ['00', '11', '22', '33', '44', '55', '66']
FEEDS = {
    FEED_IDS[0]: {
        # Информация сессии фида
        'session': make_saas_kv_feed_session(FEED_IDS[0], NOW_TIME - 10, NOW_TIME - 20),
        # Тестовые данные офферов фида
        'offers': {
            # Оффер с разными значениями api и feed цен в saas-hub (источник "api")
            OFFER_IDS[0]: {
                # Описание состояния оффера в saas-hub (через ручку doc_state)
                'doc_state': {
                    "feed_prices": {
                        "price": make_saas_hub_price(5000000)
                    },
                    "api_prices": {
                        "price": make_saas_hub_price(5000000)
                    },
                    "prices": {
                        "source": "api",
                        "price": make_saas_hub_price(5000000)
                    }
                },
                # Данные об офере в saas-kv
                'saas_kv': make_saas_kv_offer_data(FEED_IDS[0], OFFER_IDS[0]),
                # Представление данных оффера в старом формате
                'legacy_offer': make_legacy_storage_data(FEED_IDS[0], OFFER_IDS[0], 4000.1, NOW_TIME - 10)
            },
            # Оффер с разными значениями api и feed цен в saas-hub (источник "feed")
            OFFER_IDS[1]: {
                'doc_state': {
                    "feed_prices": {
                        "price": make_saas_hub_price(6000000)
                    },
                    "api_prices": {
                        "price": make_saas_hub_price(6000000)
                    },
                    "prices": {
                        "source": "feed",
                        "price": make_saas_hub_price(6000000)
                    }
                },
                'saas_kv': make_saas_kv_offer_data(FEED_IDS[0], OFFER_IDS[1])
            },
            # Оффер с ценой только из feed
            OFFER_IDS[2]: {
                'doc_state': {
                    "feed_prices": {
                        "price": make_saas_hub_price(7000000)
                    },
                    "prices": {
                        "source": "feed",
                        "price": make_saas_hub_price(7000000)
                    }
                },
                'saas_kv': make_saas_kv_offer_data(FEED_IDS[0], OFFER_IDS[2])
            },
            # Оффер с ценой только из api
            OFFER_IDS[3]: {
                'doc_state': {
                    "api_prices": {
                        "price": make_saas_hub_price(8000000)
                    },
                    "prices": {
                        "source": "api",
                        "price": make_saas_hub_price(8000000)
                    }
                },
                'saas_kv': make_saas_kv_offer_data(FEED_IDS[0], OFFER_IDS[3])
            },
            # Оффер, у которого не указан источник цены
            OFFER_IDS[4]: {
                'doc_state': {
                    "prices": {
                        "price": make_saas_hub_price(9000000)
                    }
                },
                'saas_kv': make_saas_kv_offer_data(FEED_IDS[0], OFFER_IDS[4])
            },
            # Оффер, у которого не указано поле price в api_prices
            OFFER_IDS[5]: {
                'doc_state': {
                    "api_prices": {
                        "ts": NOW_TIME,
                        "deleted": True
                    },
                    "prices": {
                        "source": "feed",
                        "price": make_saas_hub_price(10000000)
                    }
                },
                'saas_kv': make_saas_kv_offer_data(FEED_IDS[0], OFFER_IDS[5])
            },
            # Оффер, у которого не указано поле price в feed_prices
            OFFER_IDS[6]: {
                'doc_state': {
                    "feed_prices": {
                        "ts": NOW_TIME,
                        "deleted": True
                    },
                    "prices": {
                        "source": "api",
                        "price": make_saas_hub_price(11000000)
                    }
                },
                'saas_kv': make_saas_kv_offer_data(FEED_IDS[0], OFFER_IDS[6])
            }
        }
    }
}


def check_feed_offer_id(feeds, feed_id, offer_id):
    if feed_id not in feeds or offer_id not in feeds[feed_id]['offers']:
        raise Exception('Offer {}/{} not found: {}'.format(feed_id, offer_id, feeds))


def get_offer(feeds, feed_id, offer_id):
    return feeds[feed_id]['offers'][offer_id]


class MockStorage(RealStorage):
    def __init__(self, crates, feeds):
        RealStorage.__init__(self, crates)
        self._feeds = feeds

    def get_offer(self, feed_id, offer_id, *_):
        check_feed_offer_id(self._feeds, feed_id, offer_id)
        return get_offer(self._feeds, feed_id, offer_id)['legacy_offer']

    def get_feed_qsession(self, feed_id):
        feed_id = int(feed_id)
        if feed_id not in self._feeds:
            raise Exception('Feed {} not found: {}'.format(feed_id, self._feeds))
        return self._feeds[feed_id]['session']['legacy_qsession']


class MockRealStorage(object):
    def __init__(self, feeds):
        self._feeds = feeds

    def get_offer(self, feed_id, offer_id):
        check_feed_offer_id(self._feeds, feed_id, offer_id)
        return get_offer(self._feeds, feed_id, offer_id)['saas_kv']

    def get_session(self, feed_id, offer_id=None):
        check_feed_offer_id(self._feeds, feed_id, offer_id)
        return self._feeds[feed_id]['session']['saas_kv']


class MockVirtualStorage(object):
    def get_offer(self, feed_id, offer_id):
        raise Exception('Unexpected call to VirtualStorage')

    def get_session(self, feed_id, offer_id=None):
        raise Exception('Unexpected call to VirtualStorage')


class MockSaashubStorage(object):
    def __init__(self, feeds):
        self._feeds = feeds

    def get_doc_state(self, feed_id, offer_id):
        # Если оффера в Saas-hub нет, то ошибка 404, по которой get_doc_state возвращает None
        if feed_id == NOT_FOUND_FEED_ID and offer_id == NOT_FOUND_OFFER_ID:
            return None

        check_feed_offer_id(self._feeds, feed_id, offer_id)
        return get_offer(self._feeds, feed_id, offer_id)['doc_state']


@pytest.fixture(scope="module")
def test_app():
    cratespath = os.path.abspath(source_path('market/idx/api/tests/currency_rates.xml'))
    crates = SimpleResource(cratespath, exchange.load).get()

    meta_storage = MetaStorage(
        default_offer_builder=OfferBuilder(crates, {'price_from_saashub': True}),
        default_session_builder=SessionBuilder(),
        real_kv_storage=MockRealStorage(FEEDS),
        virtual_kv_storage=MockVirtualStorage(),
        saashub_storage=MockSaashubStorage(FEEDS),
    )
    return create_flask_app(MockStorage(crates, FEEDS), meta_storage)


def assert_doc_state_price(price, doc_state_price):
    assert price * (10000000) == doc_state_price['price']['value']


def get_xml_tag_text(root_etree, xml_tag):
    xpath = etree.XPath('//atom:{}'.format(xml_tag), namespaces={'atom': 'http://www.w3.org/2005/Atom'})
    return [tag.text for tag in xpath(root_etree)]


def test_price_source_not_supported_in_idxapi_v1(test_app):
    """ В IDXAPI v1 не поддерживается источник текущей цены из saas-hub """
    feed_id = FEED_IDS[0]
    offer_id = OFFER_IDS[0]

    with test_app.test_client() as client:
        resp = client.get('/v1/smart/offer?feed_id={}&offer_id={}&format=json'.format(feed_id, offer_id))
        assert_that(resp, is_success_response())

        offer = get_offer(FEEDS, feed_id, offer_id)

        data = flask.json.loads(resp.data)
        assert data['ru_price'] == offer['legacy_offer']['ru_price']
        assert 'price_source' not in data
        assert 'api_price' not in data
        assert 'feed_price' not in data


@pytest.mark.parametrize("offer_id", [OFFER_IDS[0], OFFER_IDS[1]])
def test_several_price_sources(test_app, offer_id):
    """ Если в doc_state в saas-hub для оффера присутствуют оба источника цены, то возвращаем обе цены и источник
    текущей """
    feed_id = FEED_IDS[0]

    with test_app.test_client() as client:
        resp = client.get('/v2/smart/offer?feed_id={}&offer_id={}&format=json'.format(feed_id, offer_id))
        assert_that(resp, is_success_response())

        offer = get_offer(FEEDS, feed_id, offer_id)

        data = flask.json.loads(resp.data)
        assert data['feed_id'] == feed_id
        assert data['offer_id'] == offer_id
        assert data['shop_currency'] == offer['doc_state']['prices']['price']['currency']
        assert data['price_source'] == offer['doc_state']['prices']['source']
        assert_doc_state_price(data['ru_price'], offer['doc_state']['prices'])
        assert_doc_state_price(data['shop_price'], offer['doc_state']['prices'])
        assert_doc_state_price(data['api_price'], offer['doc_state']['api_prices'])
        assert_doc_state_price(data['feed_price'], offer['doc_state']['feed_prices'])

    with test_app.test_client() as client:
        resp = client.get('/v2/smart/offer?feed_id={}&offer_id={}&format=text'.format(feed_id, offer_id))
        assert_that(resp, is_success_response())

        offer = get_offer(FEEDS, feed_id, offer_id)

        root_etree = etree.fromstring(resp.data)
        actual = get_xml_tag_text(root_etree, 'price_source')
        assert_that(len(actual), equal_to(1))
        assert_that(actual[0], equal_to(offer['doc_state']['prices']['source']))
        actual = get_xml_tag_text(root_etree, 'api_price')
        assert_that(len(actual), equal_to(1))
        assert_doc_state_price(float(actual[0]), offer['doc_state']['api_prices'])
        actual = get_xml_tag_text(root_etree, 'feed_price')
        assert_that(len(actual), equal_to(1))
        assert_doc_state_price(float(actual[0]), offer['doc_state']['feed_prices'])


@pytest.mark.parametrize("offer_id", [OFFER_IDS[2], OFFER_IDS[3]])
def test_single_price_source(test_app, offer_id):
    """ Если в doc_state в saas-hub у оффера указана только одна цена (либо api, либо feed), то указывается только
    источник """
    feed_id = FEED_IDS[0]

    with test_app.test_client() as client:
        resp = client.get('/v2/smart/offer?feed_id={}&offer_id={}&format=json'.format(feed_id, offer_id))
        assert_that(resp, is_success_response())

        offer = get_offer(FEEDS, feed_id, offer_id)

        data = flask.json.loads(resp.data)
        assert data['feed_id'] == feed_id
        assert data['offer_id'] == offer_id
        assert data['shop_currency'] == offer['doc_state']['prices']['price']['currency']
        assert data['price_source'] == offer['doc_state']['prices']['source']
        assert_doc_state_price(data['ru_price'], offer['doc_state']['prices'])
        assert_doc_state_price(data['shop_price'], offer['doc_state']['prices'])
        assert 'api_price' not in data
        assert 'feed_price' not in data

    with test_app.test_client() as client:
        resp = client.get('/v2/smart/offer?feed_id={}&offer_id={}&format=text'.format(feed_id, offer_id))
        assert_that(resp, is_success_response())

        offer = get_offer(FEEDS, feed_id, offer_id)

        root_etree = etree.fromstring(resp.data)
        actual = get_xml_tag_text(root_etree, 'price_source')
        assert_that(len(actual), equal_to(1))
        assert_that(actual[0], equal_to(offer['doc_state']['prices']['source']))
        actual = get_xml_tag_text(root_etree, 'api_price')
        assert_that(len(actual), equal_to(0))
        actual = get_xml_tag_text(root_etree, 'feed_price')
        assert_that(len(actual), equal_to(0))


@pytest.mark.parametrize("offer_id", [OFFER_IDS[4]])
def test_undefined_price_source(test_app, offer_id):
    """ Если в doc_state в saas-hub у оффера не указан источник цены, то idxapi вернет поле price_source == 'undefined'
    """
    feed_id = FEED_IDS[0]

    with test_app.test_client() as client:
        resp = client.get('/v2/smart/offer?feed_id={}&offer_id={}&format=json'.format(feed_id, offer_id))
        assert_that(resp, is_success_response())

        offer = get_offer(FEEDS, feed_id, offer_id)

        data = flask.json.loads(resp.data)
        assert data['feed_id'] == feed_id
        assert data['offer_id'] == offer_id
        assert data['shop_currency'] == offer['doc_state']['prices']['price']['currency']
        assert_doc_state_price(data['ru_price'], offer['doc_state']['prices'])
        assert_doc_state_price(data['shop_price'], offer['doc_state']['prices'])
        assert data['price_source'] == 'undefined'
        assert 'api_price' not in data
        assert 'feed_price' not in data

    with test_app.test_client() as client:
        resp = client.get('/v2/smart/offer?feed_id={}&offer_id={}&format=text'.format(feed_id, offer_id))
        assert_that(resp, is_success_response())

        root_etree = etree.fromstring(resp.data)
        actual = get_xml_tag_text(root_etree, 'price_source')
        assert_that(len(actual), equal_to(1))
        assert_that(actual[0], equal_to('undefined'))
        actual = get_xml_tag_text(root_etree, 'api_price')
        assert_that(len(actual), equal_to(0))
        actual = get_xml_tag_text(root_etree, 'feed_price')
        assert_that(len(actual), equal_to(0))


@pytest.mark.parametrize("offer_id", [OFFER_IDS[5], OFFER_IDS[6]])
def test_empty_api_feed_price(test_app, offer_id):
    """ Если в doc_state в saas-hub у оффера нет поля price в api_prices или feed_prices, мы не пятисотим """
    feed_id = FEED_IDS[0]

    with test_app.test_client() as client:
        resp = client.get('/v2/smart/offer?feed_id={}&offer_id={}&format=json'.format(feed_id, offer_id))
        assert_that(resp, is_success_response())

        offer = get_offer(FEEDS, feed_id, offer_id)

        data = flask.json.loads(resp.data)
        assert data['feed_id'] == feed_id
        assert data['offer_id'] == offer_id
        assert data['shop_currency'] == offer['doc_state']['prices']['price']['currency']
        assert_doc_state_price(data['ru_price'], offer['doc_state']['prices'])
        assert_doc_state_price(data['shop_price'], offer['doc_state']['prices'])
        assert data['price_source'] == offer['doc_state']['prices']['source']
        assert 'api_price' not in data
        assert 'feed_price' not in data

    with test_app.test_client() as client:
        resp = client.get('/v2/smart/offer?feed_id={}&offer_id={}&format=text'.format(feed_id, offer_id))
        assert_that(resp, is_success_response())

        root_etree = etree.fromstring(resp.data)
        actual = get_xml_tag_text(root_etree, 'price_source')
        assert_that(len(actual), equal_to(1))
        assert_that(actual[0], offer['doc_state']['prices']['source'])


@pytest.mark.parametrize("req_format", ['json', 'text'])
def test_not_found_doc_json(test_app, req_format):
    """ Если в doc_state в saas-hub нет оффера, мы не пятисотим, а возвраащем 404 """
    with test_app.test_client() as client:
        resp = client.get('/v2/smart/offer?feed_id={}&offer_id={}&format={}'.format(NOT_FOUND_FEED_ID,
                                                                                    NOT_FOUND_OFFER_ID,
                                                                                    req_format))
        assert resp.status_code == 404
