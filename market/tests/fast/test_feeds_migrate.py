# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.MigrateFeedOffers_pb2 import MigrateFeedOffersRequest, MigrateFeedOffersResponse

from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row
from market.idx.datacamp.yatf.utils import create_meta
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.pylibrary.proto_utils import message_from_data

BUSINESS_ID = 100
BUSINESS_ID_FOR_PAGINATION = 101
BUSINESS_ID_SHOP_MIGRATION = 102
SHOP_ID = 200
SRC_FEED_ID = 400
TARGET_FEED_ID = 500
EXTRA_FEED_ID = 600
ACTUAL_FEED_ID = 700
TARGET_ACTUAL_FEED_ID = 701
TIMESTAMP = '2019-02-15T15:55:55Z'


@pytest.fixture(scope='module')
def service_offers():
    return [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=offer['business_id'],
                offer_id=offer['offer_id'],
                shop_id=SHOP_ID,
                real_feed_id=offer['feed_id'],
            ),
            meta=create_meta(10, scope=DTC.SERVICE),
        )) for offer in [
            {'business_id': BUSINESS_ID, 'offer_id': 'o1', 'feed_id': SRC_FEED_ID},
            {'business_id': BUSINESS_ID, 'offer_id': 'o2', 'feed_id': SRC_FEED_ID},
            {'business_id': BUSINESS_ID, 'offer_id': 'o3', 'feed_id': SRC_FEED_ID},
            {'business_id': BUSINESS_ID, 'offer_id': 'o9', 'feed_id': EXTRA_FEED_ID},
            {'business_id': BUSINESS_ID_FOR_PAGINATION, 'offer_id': 'o1', 'feed_id': SRC_FEED_ID},
            {'business_id': BUSINESS_ID_FOR_PAGINATION, 'offer_id': 'o2', 'feed_id': SRC_FEED_ID},
            {'business_id': BUSINESS_ID_FOR_PAGINATION, 'offer_id': 'o3', 'feed_id': SRC_FEED_ID},
            {'business_id': BUSINESS_ID_FOR_PAGINATION, 'offer_id': 'o9', 'feed_id': EXTRA_FEED_ID},
        ]
    ] + [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=offer['business_id'],
                offer_id=offer['offer_id'],
                shop_id=SHOP_ID,
                real_feed_id=offer['feed_id']+1,
            ),
            meta=create_meta(10, scope=DTC.SERVICE),
        )) for offer in [
            # эмулируем ситуацию с разъехавшимися feed_id & real_feed_id
            {'business_id': BUSINESS_ID_SHOP_MIGRATION, 'offer_id': 'o1', 'feed_id': SRC_FEED_ID},
            {'business_id': BUSINESS_ID_SHOP_MIGRATION, 'offer_id': 'o2', 'feed_id': SRC_FEED_ID + 1},
            {'business_id': BUSINESS_ID_SHOP_MIGRATION, 'offer_id': 'o3', 'feed_id': SRC_FEED_ID + 2},
            {'business_id': BUSINESS_ID_SHOP_MIGRATION, 'offer_id': 'o9', 'feed_id': TARGET_FEED_ID},
        ]
    ]


@pytest.fixture(scope='module')
def actual_service_offers():
    return [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=offer['business_id'],
                offer_id=offer['offer_id'],
                shop_id=SHOP_ID,
                feed_id=offer['feed_id'],
                warehouse_id=offer.get('warehouse_id', 0)
            ),
            meta=create_meta(10, scope=DTC.SERVICE),
        )) for offer in [
            {'business_id': BUSINESS_ID, 'offer_id': 'o1', 'feed_id': SRC_FEED_ID, 'warehouse_id': 147},
            {'business_id': BUSINESS_ID, 'offer_id': 'o2', 'feed_id': SRC_FEED_ID, 'warehouse_id': 147},
            {'business_id': BUSINESS_ID, 'offer_id': 'o3', 'feed_id': SRC_FEED_ID, 'warehouse_id': 147},
            {'business_id': BUSINESS_ID, 'offer_id': 'o3', 'feed_id': ACTUAL_FEED_ID, 'warehouse_id': 145},
            {'business_id': BUSINESS_ID, 'offer_id': 'o9', 'feed_id': EXTRA_FEED_ID, 'warehouse_id': 147},
            {'business_id': BUSINESS_ID_FOR_PAGINATION, 'offer_id': 'o1', 'feed_id': SRC_FEED_ID, 'warehouse_id': 147},
            {'business_id': BUSINESS_ID_FOR_PAGINATION, 'offer_id': 'o2', 'feed_id': SRC_FEED_ID, 'warehouse_id': 147},
            {'business_id': BUSINESS_ID_FOR_PAGINATION, 'offer_id': 'o3', 'feed_id': SRC_FEED_ID, 'warehouse_id': 147},
            {'business_id': BUSINESS_ID_FOR_PAGINATION, 'offer_id': 'o3', 'feed_id': ACTUAL_FEED_ID,
             'warehouse_id': 145},
            {'business_id': BUSINESS_ID_FOR_PAGINATION, 'offer_id': 'o9', 'feed_id': EXTRA_FEED_ID,
             'warehouse_id': 147},
            {'business_id': BUSINESS_ID_SHOP_MIGRATION, 'offer_id': 'o1', 'feed_id': SRC_FEED_ID, 'warehouse_id': 145},
            {'business_id': BUSINESS_ID_SHOP_MIGRATION, 'offer_id': 'o2', 'feed_id': SRC_FEED_ID + 1, 'warehouse_id': 145},
            {'business_id': BUSINESS_ID_SHOP_MIGRATION, 'offer_id': 'o3', 'feed_id': SRC_FEED_ID + 2,
             'warehouse_id': 145},
            {'business_id': BUSINESS_ID_SHOP_MIGRATION, 'offer_id': 'o3', 'feed_id': SRC_FEED_ID + 2,
             'warehouse_id': 147},
            {'business_id': BUSINESS_ID_SHOP_MIGRATION, 'offer_id': 'o9', 'feed_id': TARGET_ACTUAL_FEED_ID,
             'warehouse_id': 147},
        ]
    ]


@pytest.yield_fixture(scope='module')
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        service_offers_table,
        actual_service_offers_table
):
    with make_stroller(
            config,
            yt_server,
            log_broker_stuff,
            service_offers_table=service_offers_table,
            actual_service_offers_table=actual_service_offers_table
    ) as stroller_env:
        yield stroller_env


def assert_offers(table, service, identifiers):
    table.load()
    assert_that(table.data, HasOffers([
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=offer['business_id'],
                offer_id=offer['offer_id'],
                shop_id=SHOP_ID,
                real_feed_id=offer['feed_id'] if service else None,
                feed_id=offer['feed_id'] if not service else None,
                warehouse_id=offer.get('warehouse_id', None)
            )
        ) for offer in identifiers
    ]))


def test_migrate_all(stroller, service_offers_table, actual_service_offers_table):
    business_id = BUSINESS_ID
    response = stroller.post(
        '/v1/partners/{}/shops/{}/feeds/{}/migrate?target_feed_id={}'.format(
            business_id, SHOP_ID, SRC_FEED_ID, TARGET_FEED_ID
        ),
        data=message_from_data({
            'actual_feeds': {
                ACTUAL_FEED_ID: TARGET_ACTUAL_FEED_ID,
            },
        }, MigrateFeedOffersRequest()).SerializeToString()
    )
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(MigrateFeedOffersResponse, {
        'next_page_token': None
    }))
    assert_offers(service_offers_table, True, [
        {'business_id': business_id, 'offer_id': 'o1', 'feed_id': TARGET_FEED_ID},
        {'business_id': business_id, 'offer_id': 'o2', 'feed_id': TARGET_FEED_ID},
        {'business_id': business_id, 'offer_id': 'o3', 'feed_id': TARGET_FEED_ID},
        {'business_id': business_id, 'offer_id': 'o9', 'feed_id': EXTRA_FEED_ID},
    ])
    assert_offers(actual_service_offers_table, False, [
        {'business_id': business_id, 'offer_id': 'o1', 'feed_id': TARGET_FEED_ID, 'warehouse_id': 147},
        {'business_id': business_id, 'offer_id': 'o2', 'feed_id': TARGET_FEED_ID, 'warehouse_id': 147},
        {'business_id': business_id, 'offer_id': 'o3', 'feed_id': TARGET_FEED_ID, 'warehouse_id': 147},
        {'business_id': business_id, 'offer_id': 'o3', 'feed_id': TARGET_ACTUAL_FEED_ID, 'warehouse_id': 145},
        {'business_id': business_id, 'offer_id': 'o9', 'feed_id': EXTRA_FEED_ID, 'warehouse_id': 147},
    ])


def test_migrate_with_paging(stroller, service_offers_table, actual_service_offers_table):
    business_id = BUSINESS_ID_FOR_PAGINATION
    response = stroller.post(
        '/v1/partners/{}/shops/{}/feeds/{}/migrate?target_feed_id={}&batch_size=2'.format(
            business_id, SHOP_ID, SRC_FEED_ID, TARGET_FEED_ID
        ),
        data=message_from_data({
            'actual_feeds': {
                ACTUAL_FEED_ID: TARGET_ACTUAL_FEED_ID,
            },
        }, MigrateFeedOffersRequest()).SerializeToString()
    )
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(MigrateFeedOffersResponse, {
        'next_page_token': 'CMgBGAAiAm8yOGU'
    }))
    assert_offers(service_offers_table, True, [
        {'business_id': business_id, 'offer_id': 'o1', 'feed_id': TARGET_FEED_ID},
        {'business_id': business_id, 'offer_id': 'o2', 'feed_id': TARGET_FEED_ID},
        {'business_id': business_id, 'offer_id': 'o3', 'feed_id': SRC_FEED_ID},
        {'business_id': business_id, 'offer_id': 'o9', 'feed_id': EXTRA_FEED_ID},
    ])
    assert_offers(actual_service_offers_table, False, [
        {'business_id': business_id, 'offer_id': 'o1', 'feed_id': TARGET_FEED_ID, 'warehouse_id': 147},
        {'business_id': business_id, 'offer_id': 'o2', 'feed_id': TARGET_FEED_ID, 'warehouse_id': 147},
        {'business_id': business_id, 'offer_id': 'o3', 'feed_id': SRC_FEED_ID, 'warehouse_id': 147},
        {'business_id': business_id, 'offer_id': 'o3', 'feed_id': ACTUAL_FEED_ID, 'warehouse_id': 145},
        {'business_id': business_id, 'offer_id': 'o9', 'feed_id': EXTRA_FEED_ID, 'warehouse_id': 147},
    ])

    response = stroller.post(
        '/v1/partners/{}/shops/{}/feeds/{}/migrate?target_feed_id={}&batch_size=2&page_token=CMgBGAAiAm8yOGU'.format(
            business_id, SHOP_ID, SRC_FEED_ID, TARGET_FEED_ID
        ),
        data=message_from_data({
            'actual_feeds': {
                ACTUAL_FEED_ID: TARGET_ACTUAL_FEED_ID,
            },
        }, MigrateFeedOffersRequest()).SerializeToString()
    )
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(MigrateFeedOffersResponse, {
        'next_page_token': 'CMgBGAAiAm85OGU'
    }))
    assert_offers(service_offers_table, True, [
        {'business_id': business_id, 'offer_id': 'o1', 'feed_id': TARGET_FEED_ID},
        {'business_id': business_id, 'offer_id': 'o2', 'feed_id': TARGET_FEED_ID},
        {'business_id': business_id, 'offer_id': 'o3', 'feed_id': TARGET_FEED_ID},
        {'business_id': business_id, 'offer_id': 'o9', 'feed_id': EXTRA_FEED_ID},
    ])
    assert_offers(actual_service_offers_table, False, [
        {'business_id': business_id, 'offer_id': 'o1', 'feed_id': TARGET_FEED_ID, 'warehouse_id': 147},
        {'business_id': business_id, 'offer_id': 'o2', 'feed_id': TARGET_FEED_ID, 'warehouse_id': 147},
        {'business_id': business_id, 'offer_id': 'o3', 'feed_id': TARGET_FEED_ID, 'warehouse_id': 147},
        {'business_id': business_id, 'offer_id': 'o3', 'feed_id': TARGET_ACTUAL_FEED_ID, 'warehouse_id': 145},
        {'business_id': business_id, 'offer_id': 'o9', 'feed_id': EXTRA_FEED_ID, 'warehouse_id': 147},
    ])

    response = stroller.post(
        '/v1/partners/{}/shops/{}/feeds/{}/migrate?target_feed_id={}&batch_size=2&page_token=CMgBGAAiAm85OGU'.format(
            business_id, SHOP_ID, SRC_FEED_ID, TARGET_FEED_ID
        ),
        data=message_from_data({
            'actual_feeds': {
                ACTUAL_FEED_ID: TARGET_ACTUAL_FEED_ID,
            },
        }, MigrateFeedOffersRequest()).SerializeToString()
    )
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(MigrateFeedOffersResponse, {
        'next_page_token': None
    }))


def test_migrate_full(stroller, service_offers_table, actual_service_offers_table):
    # миграция всех офферов всех фидов магазина под один, делается с заданием магического source feed_id=0
    business_id = BUSINESS_ID_SHOP_MIGRATION
    response = stroller.post(
        '/v1/partners/{}/shops/{}/feeds/{}/migrate?target_feed_id={}'.format(
            business_id, SHOP_ID, 0, TARGET_FEED_ID
        ),
        data=message_from_data({
            'actual_feeds': {
                0: TARGET_ACTUAL_FEED_ID,
            },
        }, MigrateFeedOffersRequest()).SerializeToString()
    )
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(MigrateFeedOffersResponse, {
        'next_page_token': None
    }))
    assert_offers(service_offers_table, True, [
        {'business_id': business_id, 'offer_id': 'o1', 'feed_id': TARGET_FEED_ID},
        {'business_id': business_id, 'offer_id': 'o2', 'feed_id': TARGET_FEED_ID},
        {'business_id': business_id, 'offer_id': 'o3', 'feed_id': TARGET_FEED_ID},
        {'business_id': business_id, 'offer_id': 'o9', 'feed_id': TARGET_FEED_ID},
    ])
    assert_offers(actual_service_offers_table, False, [
        {'business_id': business_id, 'offer_id': 'o1', 'feed_id': TARGET_ACTUAL_FEED_ID, 'warehouse_id': 145},
        {'business_id': business_id, 'offer_id': 'o2', 'feed_id': TARGET_ACTUAL_FEED_ID, 'warehouse_id': 145},
        {'business_id': business_id, 'offer_id': 'o3', 'feed_id': TARGET_ACTUAL_FEED_ID, 'warehouse_id': 145},
        {'business_id': business_id, 'offer_id': 'o3', 'feed_id': TARGET_ACTUAL_FEED_ID, 'warehouse_id': 147},
        {'business_id': business_id, 'offer_id': 'o9', 'feed_id': TARGET_ACTUAL_FEED_ID, 'warehouse_id': 147},
    ])

    # Случай, когда target_feed_id=0 => режим "перевесь магазин полностью"
    # Фиксирую поведение в случае, когда тело запроса не содержит соответствия из 0 в целевой фид -
    # перевесим на target_feed_id из урла
    new_target_feed_id = TARGET_FEED_ID + 1
    response = stroller.post(
        '/v1/partners/{}/shops/{}/feeds/{}/migrate?target_feed_id={}'.format(
            business_id, SHOP_ID, 0, new_target_feed_id
        ),
        data=message_from_data({
            'actual_feeds': {
                SRC_FEED_ID: TARGET_ACTUAL_FEED_ID,
            },
        }, MigrateFeedOffersRequest()).SerializeToString()
    )
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(MigrateFeedOffersResponse, {
        'next_page_token': None
    }))
    assert_offers(service_offers_table, True, [
        {'business_id': business_id, 'offer_id': 'o1', 'feed_id': new_target_feed_id},
        {'business_id': business_id, 'offer_id': 'o2', 'feed_id': new_target_feed_id},
        {'business_id': business_id, 'offer_id': 'o3', 'feed_id': new_target_feed_id},
        {'business_id': business_id, 'offer_id': 'o9', 'feed_id': new_target_feed_id},
    ])
    assert_offers(actual_service_offers_table, False, [
        {'business_id': business_id, 'offer_id': 'o1', 'feed_id': new_target_feed_id, 'warehouse_id': 145},
        {'business_id': business_id, 'offer_id': 'o2', 'feed_id': new_target_feed_id, 'warehouse_id': 145},
        {'business_id': business_id, 'offer_id': 'o3', 'feed_id': new_target_feed_id, 'warehouse_id': 145},
        {'business_id': business_id, 'offer_id': 'o3', 'feed_id': new_target_feed_id, 'warehouse_id': 147},
        {'business_id': business_id, 'offer_id': 'o9', 'feed_id': new_target_feed_id, 'warehouse_id': 147},
    ])
