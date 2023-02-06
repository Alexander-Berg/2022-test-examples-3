# -*- coding: utf-8 -*-

import os
import pytest
from yatest.common import source_path
from market.idx.api.backend.marketindexer.storage.builders import OfferBuilder
from market.idx.api.backend.marketindexer.storage.resources import SimpleResource
from market.idx.api.backend.marketindexer.storage.meta_storage import MetaStorage
from market.idx.api.backend.marketindexer import exchange
from market.proto.feedparser.deprecated.OffersData_pb2 import Offer
from market.proto.indexer.GenerationLog_pb2 import Record
from market.proto.common.common_pb2 import PriceExpression

FEED_ID = '1069'
OFFER_ID = '1'
PRICES = {
    # Цена офера из idxapi-kv
    'Saas': {
        'input': {
            'price': 1230000000,
            'rate': '1',
            'plus': 0,
            'id': 'RUR',
            'ref_id': 'RUR',
            'price_expression': '123.000000 1 0 RUR RUR',
            'ru_price': 123},
        'output': {
            'price_expression': '123.000000 1 0 RUR RUR',
            'ru_price': 123,
            'shop_price': 123,
            'shop_currency': 'RUR'},
    },

    # Цена офера из saashub
    'SaasHub': {
        'input': {
            'value': 5550000000,
            'rate': '1',
            'plus': 0,
            'currency': 'RUR',
            'ref_currency': 'RUR'},
        'output': {
            'price_expression': '555.000000 1 0 RUR RUR',
            'ru_price': 555,
            'shop_price': 555,
            'shop_currency': 'RUR'
        }
    }
}


def get_crates():
    crates_path = os.path.abspath(source_path('market/idx/api/tests/currency_rates.xml'))
    return SimpleResource(crates_path, exchange.load)


def get_saas_hub_response(include_only_fields=None):
    saashub_price = PRICES['SaasHub']['input']
    offer_template = {
        "prices": {
            "ts": 1548267480,
            "source": "api",
            "time": "Wed, 23 Jan 2019 21:18:00 MSK",
            "price": {
                "plus": saashub_price['plus'],
                "value": saashub_price['value'],
                "currency": saashub_price['currency'],
                "ref_currency": saashub_price['ref_currency'],
                "rate": saashub_price['rate']
            }
        }
    }

    if include_only_fields is not None:
        offer_price = offer_template['prices']['price']
        price_copy = offer_price.copy()
        offer_price.clear()
        for field in include_only_fields:
            offer_price[field] = price_copy[field]

    return offer_template


class MockRealStorage(object):
    def get_offer(self, feed_id, offer_id):
        self.check_offer_id(feed_id, offer_id)
        return self._get_1069_offer_response()

    def get_session(self, feed_id, offer_id):
        return {'session': {}}

    @staticmethod
    def check_offer_id(feed_id, offer_id):
        if feed_id != FEED_ID or offer_id != OFFER_ID:
            raise Exception('Unexpected input data.')
        pass

    @staticmethod
    def _get_1069_offer_response():
        saas_price = PRICES['Saas']['input']
        pe = PriceExpression(price=saas_price['price'],
                             rate=saas_price['rate'],
                             plus=saas_price['plus'],
                             id=saas_price['id'],
                             ref_id=saas_price['ref_id'])
        offer = Offer(title='iPhone', ru_price=saas_price['ru_price'], price_expression=saas_price['price_expression'])

        qoffer = Record(binary_price=pe)

        return {'qoffer_1069/1': {'b64:qdata': qoffer},
                'offer_1069/1': {'b64:offer': offer}}


class MockVirtualStorage(object):

    def get_session(self, feed_id, offer_id):
        return {'session': {}}

    def get_offer(self, feed_id, offer_id):
        # Если RealStorage отдаёт данные, то VirtualStorage не должен вызываться
        raise Exception('Unexpected call to VirtualStorage.')


class MockSaashubStorage(object):
    def __init__(self, exception=None, include_only_fields=None):
        self._exception = exception
        self._include_only_fields = include_only_fields

    def get_doc_state(self, feed_id, offer_id):
        if self._exception is not None:
            raise self._exception

        if feed_id != FEED_ID or offer_id != OFFER_ID:
            raise Exception('Offer not found.')

        return get_saas_hub_response(self._include_only_fields)


def create_meta_storage(real_storage, virtual_storage, saashub_storage, price_from_saashub):
    options = {'price_from_saashub': price_from_saashub}
    offer_builder = OfferBuilder(get_crates(), options)
    return MetaStorage(
        default_offer_builder=offer_builder,
        saashub_storage=saashub_storage,
        real_kv_storage=real_storage,
        virtual_kv_storage=virtual_storage
    )


def assert_meta_storage_price_response(offers, prices):
    def assert_offer_prices(offer, price):
        assert offer['price_expression'] == price['price_expression']
        assert offer['ru_price'] == price['ru_price']
        assert offer['shop_currency'] == price['shop_currency']
        assert offer['shop_price'] == price['shop_price']
    assert_offer_prices(offers['offer'], prices)
    assert_offer_prices(offers['qoffer'], prices)


# Тест проверяет, что при выключенном параметре offer_price_from_saashub цена берётся из хранилища
# idxapi-kv (RealStorage, VirtualStorage).
def test_price_from_saashub_off_price_not_changed():
    saashub_storage = MockSaashubStorage()
    meta_storage = create_meta_storage(MockRealStorage(), MockVirtualStorage(), saashub_storage,
                                       price_from_saashub=False)

    offers, _ = meta_storage.get_offer_and_session(FEED_ID, OFFER_ID)

    assert_meta_storage_price_response(offers, PRICES['Saas']['output'])


# Тест проверяет, что при включённом параметре offer_price_from_saashub цена берётся из SaasHub
def test_price_from_saashub_on_price_from_saashub():
    saashub_storage = MockSaashubStorage()
    meta_storage = create_meta_storage(MockRealStorage(), MockVirtualStorage(), saashub_storage,
                                       price_from_saashub=True)

    offers, _ = meta_storage.get_offer_and_session(FEED_ID, OFFER_ID)

    assert_meta_storage_price_response(offers, PRICES['SaasHub']['output'])


# Тест проверяет, что при включённом параметре offer_price_from_saashub и исключении при работе с SaasHub будет
# выброшено исключение
def test_price_from_saashub_on_exception_rethrown():
    saashub_storage = MockSaashubStorage(exception=Exception('SaasHub test exception'))
    meta_storage = create_meta_storage(MockRealStorage(), MockVirtualStorage(), saashub_storage,
                                       price_from_saashub=True)
    with pytest.raises(Exception):
        meta_storage.get_offer_and_session(FEED_ID, OFFER_ID)


# Тест проверяет, что при отсутствии обязательных полей в ответе от SaasHub будет выброшено ислючение
def test_price_from_saashub_on_exception_if_no_required_field():
    required_fields = {'value', 'plus'}
    optional_fields = {'currency', 'ref_currency', 'rate'}
    all_fields = required_fields | optional_fields

    for required_field in required_fields:
        include_fields = all_fields.difference({required_field})
        saashub_storage = MockSaashubStorage(include_only_fields=include_fields)

        meta_storage = create_meta_storage(MockRealStorage(), MockVirtualStorage(), saashub_storage,
                                           price_from_saashub=True)
        with pytest.raises(Exception):
            meta_storage.get_offer_and_session(FEED_ID, OFFER_ID)


# Тест проверяет, что при отсутствии необязательных полей в ответе от SaasHub цена будет взята из SaasHub
def test_price_from_saashub_on_optional_fields():
    required_fields = {'value', 'plus'}
    saashub_storage = MockSaashubStorage(include_only_fields=required_fields)

    meta_storage = create_meta_storage(MockRealStorage(), MockVirtualStorage(), saashub_storage,
                                       price_from_saashub=True)
    offers, _ = meta_storage.get_offer_and_session(FEED_ID, OFFER_ID)

    assert_meta_storage_price_response(offers, PRICES['SaasHub']['output'])
