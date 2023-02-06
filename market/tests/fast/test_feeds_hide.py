# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.SyncHideFeed_pb2 import HideFeedResponse

from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row
from market.idx.datacamp.yatf.utils import create_meta
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers

BUSINESS_ID = 100
BUSINESS_ID_FOR_PAGINATION = 101
SHOP_ID = 200
FEED_ID = 400
TIMESTAMP = '2019-02-15T15:55:55Z'
OFFERS = [
    {'business_id': BUSINESS_ID, 'offer_id': 'o1', 'feed_id': FEED_ID},
    {'business_id': BUSINESS_ID, 'offer_id': 'o2', 'feed_id': FEED_ID},
    {'business_id': BUSINESS_ID, 'offer_id': 'o3', 'feed_id': FEED_ID},
    {'business_id': BUSINESS_ID_FOR_PAGINATION, 'offer_id': 'o1', 'feed_id': FEED_ID},
    {'business_id': BUSINESS_ID_FOR_PAGINATION, 'offer_id': 'o2', 'feed_id': FEED_ID},
    {'business_id': BUSINESS_ID_FOR_PAGINATION, 'offer_id': 'o3', 'feed_id': FEED_ID},
]


def _make_offer_extra_ids(business_id):
    return DTC.OfferExtraIdentifiers(
        recent_business_id=business_id,
        offer_yabs_id=2,
    )


@pytest.fixture(scope='module')
def service_offers():
    return [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=offer['business_id'],
                offer_id=offer['offer_id'],
                shop_id=SHOP_ID,
                real_feed_id=offer['feed_id'],
                extra=_make_offer_extra_ids(offer['business_id']),
            ),
            meta=create_meta(10, scope=DTC.SERVICE),
        )) for offer in OFFERS
    ]


@pytest.fixture(scope='module')
def basic_search_offers():
    return [{
        'business_id': offer['business_id'],
        'shop_sku': offer['offer_id'],
        'removed': False
    } for offer in OFFERS]


@pytest.fixture(scope='module')
def service_search_offers():
    return [{
        'business_id': offer['business_id'],
        'shop_id': SHOP_ID,
        'shop_sku': offer['offer_id'],
        'removed': False,
        'real_feed_id': offer['feed_id'],
    } for offer in OFFERS]


@pytest.yield_fixture(scope='module')
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        service_offers_table,
        basic_search_offers_table,
        service_search_offers_table,
):
    with make_stroller(
            config,
            yt_server,
            log_broker_stuff,
            service_offers_table=service_offers_table,
            basic_search_offers_table=basic_search_offers_table,
            service_search_offers_table=service_search_offers_table,
    ) as stroller_env:
        yield stroller_env


def assert_offers(table, identifiers):
    table.load()
    assert_that(table.data, HasOffers([
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=offer['business_id'],
                offer_id=offer['offer_id'],
                shop_id=SHOP_ID,
                real_feed_id=offer['feed_id'],
                warehouse_id=offer.get('warehouse_id', None),
                extra=_make_offer_extra_ids(offer['business_id']),
            ),
            status=DTC.OfferStatus(
                disabled=[
                    DTC.Flag(
                        flag=True,
                        meta=DTC.UpdateMeta(
                            source=DTC.PUSH_PARTNER_FEED,
                        )
                    )
                ]
            ) if offer.get('disabled', False) else None
        ) for offer in identifiers
    ]))


@pytest.mark.parametrize('search_tables', [0, 1])
def test_disable_all(stroller, service_offers_table, search_tables):
    business_id = BUSINESS_ID
    response = stroller.post(
        '/v1/partners/{}/shops/{}/feeds/{}/hide?search_tables={}'.format(
            business_id, SHOP_ID, FEED_ID, search_tables
        )
    )
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(HideFeedResponse, {
        'next_page_token': None
    }))
    assert_offers(service_offers_table, [
        {'business_id': business_id, 'offer_id': 'o1', 'feed_id': FEED_ID, 'disabled': True},
        {'business_id': business_id, 'offer_id': 'o2', 'feed_id': FEED_ID, 'disabled': True},
        {'business_id': business_id, 'offer_id': 'o3', 'feed_id': FEED_ID, 'disabled': True},
    ])


@pytest.mark.parametrize('search_tables', [0, 1])
def test_disable_with_paging(stroller, service_offers_table, search_tables):
    business_id = BUSINESS_ID_FOR_PAGINATION
    response = stroller.post(
        '/v1/partners/{}/shops/{}/feeds/{}/hide?batch_size=2&search_tables={}'.format(
            business_id, SHOP_ID, FEED_ID, search_tables
        ),
    )
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(HideFeedResponse, {
        'next_page_token': 'CMgBGAAiAm8yOGU'
    }))
    assert_offers(service_offers_table, [
        {'business_id': business_id, 'offer_id': 'o1', 'feed_id': FEED_ID, 'disabled': True},
        {'business_id': business_id, 'offer_id': 'o2', 'feed_id': FEED_ID, 'disabled': True},
        {'business_id': business_id, 'offer_id': 'o3', 'feed_id': FEED_ID, 'disabled': False},
    ])

    response = stroller.post(
        '/v1/partners/{}/shops/{}/feeds/{}/hide?batch_size=2&page_token=CMgBGAAiAm8yOGU&search_tables={}'.format(
            business_id, SHOP_ID, FEED_ID, search_tables
        )
    )
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(HideFeedResponse, {
        'next_page_token': None
    }))
    assert_offers(service_offers_table, [
        {'business_id': business_id, 'offer_id': 'o1', 'feed_id': FEED_ID, 'disabled': True},
        {'business_id': business_id, 'offer_id': 'o2', 'feed_id': FEED_ID, 'disabled': True},
        {'business_id': business_id, 'offer_id': 'o3', 'feed_id': FEED_ID, 'disabled': True},
    ])
