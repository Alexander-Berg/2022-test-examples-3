# coding: utf-8

import pytest
from hamcrest import assert_that, equal_to

import market.idx.datacamp.proto.api.OffersBatch_pb2 as OffersBatch
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.yatf.utils import create_meta
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.pylibrary.proto_utils import message_from_data

BUSINESS_ID = 1
SHOP_ID = 2

BASIC_OFFERS = [
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=BUSINESS_ID,
            offer_id='o1'
        ),
        meta=create_meta(10, scope=DTC.BASIC),
    ))
]

SERVICE_OFFERS = [
    offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=BUSINESS_ID,
            offer_id='o1',
            shop_id=SHOP_ID,
            warehouse_id=0,
        ),
        meta=create_meta(10, scope=DTC.SERVICE, color=DTC.BLUE),
    ))
]

EXPECTED_SERVICE_OFFER = {
    'identifiers': {
        'business_id': BUSINESS_ID,
        'shop_id': SHOP_ID,
        'offer_id': 'o1',
    },
    'status': {
        'result': DTC.OfferStatus.NOT_PUBLISHED_DISABLED_BY_PARTNER
    }
}


@pytest.fixture(scope='module')
def basic_offers():
    return BASIC_OFFERS


@pytest.fixture(scope='module')
def service_offers():
    return SERVICE_OFFERS


@pytest.yield_fixture(scope='module')
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table,
):
    with make_stroller(
            config,
            yt_server,
            log_broker_stuff,
            shopsdat_cacher=True,
            basic_offers_table=basic_offers_table,
            service_offers_table=service_offers_table,
            actual_service_offers_table=actual_service_offers_table,
    ) as stroller_env:
        yield stroller_env


def test_set_service_offer_and_get_batch(stroller):
    message = message_from_data({
        'status': {
            'disabled': [{
                'meta': {
                    'source': DTC.PUSH_PARTNER_API,
                },
                'flag': True
            }]
        }
    }, DTC.Offer())

    response = stroller.post(path='/v1/partners/1/offers/services/2?offer_id=o1', data=message.SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))
    assert_that(response.data, IsSerializedProtobuf(DTC.Offer, EXPECTED_SERVICE_OFFER))

    request = message_from_data({
        'entries': [{
            'method': OffersBatch.RequestMethod.GET,
            'business_id': BUSINESS_ID,
            'offer_id': 'o1'
        }]
    }, OffersBatch.UnitedOffersBatchRequest())

    response = stroller.post('/v1/offers/united/batch', data=request.SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))
    assert_that(response.data, IsSerializedProtobuf(OffersBatch.UnitedOffersBatchResponse, {
        'entries': [{
            'united_offer': {
                'service': IsProtobufMap({
                    SHOP_ID: EXPECTED_SERVICE_OFFER
                }),
            }}
        ]
    }))


def test_set_offers_batch(stroller):
    request = message_from_data({
        'entries': [{
            'method': OffersBatch.RequestMethod.POST,
            'business_id': BUSINESS_ID,
            'shop_id': SHOP_ID,
            'offer_id': 'o1',
            'offer': {
                'service': {
                    SHOP_ID: {
                        'status': {
                            'disabled': [{
                                'meta': {
                                    'source': DTC.PUSH_PARTNER_API,
                                },
                                'flag': True
                            }]
                        }
                    }
                }
            }
        }]
    }, OffersBatch.UnitedOffersBatchRequest())

    response = stroller.post('/v1/offers/united/batch', data=request.SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))
    assert_that(response.data, IsSerializedProtobuf(OffersBatch.UnitedOffersBatchResponse, {
        'entries': [{
            'united_offer': {
                'service': IsProtobufMap({
                    SHOP_ID: EXPECTED_SERVICE_OFFER
                }),
            }}
        ]
    }))
