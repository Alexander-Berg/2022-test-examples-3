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

FEED_ID = 1069
OFFER_ID = '1'


@pytest.fixture(scope='module')
def crates():
    crates_path = os.path.abspath(source_path('market/idx/api/tests/currency_rates.xml'))
    return SimpleResource(crates_path, exchange.load)


@pytest.fixture(scope='module')
def real_storage():
    pe = PriceExpression(price=1230000000,
                         rate='1',
                         plus=0,
                         id='RUR',
                         ref_id='RUR')
    offer = Offer(title='iPhone', ru_price=123, price_expression='123.000000 1 0 RUR RUR')
    qoffer = Record(binary_price=pe)
    real_storage_offer = {'qoffer_1069/1': {'b64:qdata': qoffer},
                          'offer_1069/1': {'b64:offer': offer}}

    class RealStorageMock(object):
        @staticmethod
        def get_offer(feed_id, offer_id):
            return real_storage_offer

        @staticmethod
        def get_session(feed_id, offer_id):
            return {'session': {}}

    return RealStorageMock()


@pytest.fixture(scope='module')
def virtual_storage():
    class VirtualStorageMock(object):
        def get_offer(self, feed_id, offer_id):
            raise Exception('Unexpected call to virtual storage.')

        @staticmethod
        def get_session(feed_id, offer_id):
            return {'session': {}}

    return VirtualStorageMock()


@pytest.fixture(scope='module')
def context(crates, real_storage, virtual_storage):
    return {'crates': crates,
            'real_storage': real_storage,
            'virtual_storage': virtual_storage}


def create_saashub_storage(offer_disabled, feed_flags_value, offer_has_gone_reasons):
    offer_template = {}

    feed_flags = {
        'value': feed_flags_value,
        'ts': 0,
        'time': '',
    }
    if offer_has_gone_reasons:
        feed_flags['has_gone_reasons'] = offer_has_gone_reasons

    offer_template['feed_flags'] = feed_flags
    offer_template['offer_disabled'] = offer_disabled

    class SaasHubStorageMock(object):
        @staticmethod
        def get_doc_state(feed_id, offer_id):
            return offer_template

    return SaasHubStorageMock()


def _create_meta_storage(context, saashub_storage):
    options = {'fields_from_saashub': 'offer_disabled, offer_disabled_reasons, offer_has_gone, offer_has_gone_reasons'}
    offer_builder = OfferBuilder(context['crates'], options)
    return MetaStorage(
        default_offer_builder=offer_builder,
        saashub_storage=saashub_storage,
        real_kv_storage=context['real_storage'],
        virtual_kv_storage=context['virtual_storage']
    )


def create_meta_storage(context, offer_disabled, feed_flags_value, offer_has_gone_reasons):
    saashub_storage = create_saashub_storage(offer_disabled, feed_flags_value, offer_has_gone_reasons)
    return _create_meta_storage(context, saashub_storage)


def assert_offers_field_values(offers, field_name, field_value):
    def assert_offer_field_value(offer):
        if isinstance(offer[field_name], list) and isinstance(field_value, list):
            assert sorted(offer[field_name]) == sorted(field_value)
        else:
            assert offer[field_name] == field_value

    assert_offer_field_value(offers['offer'])
    assert_offer_field_value(offers['qoffer'])


# offer_has_gone должен быть True iff feed_flags_value & (1 << 11) != 0
@pytest.mark.parametrize('feed_flags_value, offer_has_gone_reasons, expected_offer_has_gone, expected_offer_has_gone_reasons', [
    (0, [], False, []),
    (int('1100000000000', 2), ['OHGR_DELETED_FROM_FEED'], True, ['OHGR_DELETED_FROM_FEED']),
    (int('11010000000000', 2), [], False, [])
])
def test_offer_has_gone(
        feed_flags_value,
        offer_has_gone_reasons,
        expected_offer_has_gone,
        expected_offer_has_gone_reasons,
        context):

    meta_storage = create_meta_storage(
        context=context,
        offer_disabled=None,
        feed_flags_value=feed_flags_value,
        offer_has_gone_reasons=offer_has_gone_reasons)

    offers, _ = meta_storage.get_offer_and_session(FEED_ID, OFFER_ID)

    assert_offers_field_values(offers, 'offer_has_gone', expected_offer_has_gone)
    assert_offers_field_values(offers, 'offer_has_gone_reasons', expected_offer_has_gone_reasons)


@pytest.mark.parametrize('offer_disabled, expected_offer_disabled_value, expected_offer_disabled_reasons_value, ', [
    (None, False, []),   # Дефолтное значение для offer_disabled
    ({'api': {'value': False, 'ts': 0, 'time': ''}}, False, []),  # False для offer_disabled
    ({'api': {'value': True, 'ts': 0, 'time': ''}}, True, ['api']),  # True для offer_disabled
    # Несколько источников
    ({'api': {'value': False, 'ts': 0, 'time': ''}, 'someOtherSource': {'value': True, 'ts': 0, 'time': 0}}, True, ['someOtherSource']),
    ({'api': {'value': True, 'ts': 0, 'time': ''}, 'someOtherSource': {'value': True, 'ts': 0, 'time': 0}}, True, ['someOtherSource', 'api'])
])
def test_offer_disabled(offer_disabled, expected_offer_disabled_value, expected_offer_disabled_reasons_value, context):
    meta_storage = create_meta_storage(
        context=context,
        offer_disabled=offer_disabled,
        feed_flags_value=0,
        offer_has_gone_reasons=None)
    offers, _ = meta_storage.get_offer_and_session(FEED_ID, OFFER_ID)

    assert_offers_field_values(offers, 'offer_disabled', expected_offer_disabled_value)
    assert_offers_field_values(offers, 'offer_disabled_reasons', expected_offer_disabled_reasons_value)
