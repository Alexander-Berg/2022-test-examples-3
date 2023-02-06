# coding: utf-8

import pytest
import uuid
import zlib

from datetime import datetime
from hamcrest import assert_that, not_
from market.idx.datacamp.controllers.scanner.yatf.test_env import make_scanner
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import MARKET_IDX, MARKET_IDX_GENERATION
import market.idx.datacamp.proto.offer.OfferStatus_pb2 as OfferStatus
from market.proto.common.process_log_pb2 import ERROR
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.datacamp.yatf.utils import dict2tskv, create_meta
from market.pylibrary.proto_utils import message_from_data
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC


SHOP_ID = 7777
BUSINESS_ID = 777
WAREHOUSE_ID = 7
PUBLISHED = OfferStatus.PublicationStatus.PUBLISHED
NOT_PUBLISHED = OfferStatus.PublicationStatus.NOT_PUBLISHED

PUBLISHED_ID = "a_published_offer"
NOT_PUBLISHED_ID = "not_published_offer_with_verdict"

NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"


OFFER_STATUS = [
    {
        'offer': [
            # offer status with verdicts
            {
                'identifiers': {
                    'shop_id': SHOP_ID,
                    'offer_id': PUBLISHED_ID,
                    'warehouse_id': 7,
                    'business_id': BUSINESS_ID,
                },
                'status': {
                    'disabled': [],
                    'publication': {
                        'value': PUBLISHED,
                        'meta': {
                            'source': MARKET_IDX,
                            'timestamp': NOW_UTC.strftime(time_pattern),
                        },
                    },
                },
            },
            # offer not published cause of error, verdict is set
            {
                'identifiers': {
                    'shop_id': SHOP_ID,
                    'offer_id': NOT_PUBLISHED_ID,
                    'warehouse_id': WAREHOUSE_ID,
                    'business_id': BUSINESS_ID,
                },
                'status': {
                    'disabled': [],
                    'publication': {
                        'value': NOT_PUBLISHED,
                        'meta': {
                            'source': MARKET_IDX,
                            'timestamp': NOW_UTC.strftime(time_pattern),
                        },
                    },
                },
                'resolution': {
                    'by_source': [{
                        'meta': {
                            'source': MARKET_IDX_GENERATION,
                            'timestamp': NOW_UTC.strftime(time_pattern),
                        },
                        'verdict': [{
                            'results': [
                            {
                                'messages': [{
                                    'code': "490",
                                    'details': "{\"price_limit\":\"1727.4\",\"price\":\"1892\",\"market-sku\":682008601,\"offerId\":\"not_published_offer_with_verdict\",\"code\":\"490\"}",
                                    "level": ERROR,
                                    "namespace": "1",
                                    "params": [],
                                    "text": "The price is too high"
                                }],
                            }]
                        }]
                    }]
                }
            }
        ]
    }
]


@pytest.fixture(scope='module')
def service_offers_table_data(color):
    return [
        offer_to_service_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=business_id,
                    shop_id=shop_id,
                    offer_id=offer_id,
                ),
                meta=create_meta(10, DTC.BLUE)
            )
        )
        for business_id, shop_id, offer_id in [
            (BUSINESS_ID, SHOP_ID, PUBLISHED_ID),
            (BUSINESS_ID, SHOP_ID, NOT_PUBLISHED_ID),
        ]
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
        offer_to_service_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=business_id,
                    shop_id=shop_id,
                    offer_id=offer_id,
                    warehouse_id=warehouse_id,
                ),
                meta=create_meta(10, DTC.BLUE)
            )
        )
        for business_id, shop_id, offer_id, warehouse_id in [
            (BUSINESS_ID, SHOP_ID, PUBLISHED_ID, WAREHOUSE_ID),
            (BUSINESS_ID, SHOP_ID, NOT_PUBLISHED_ID, WAREHOUSE_ID),
        ]
    ]


@pytest.fixture(scope='module')
def offer_status_batched_table_data():
    return [{'batch': zlib.compress(message_from_data(batch, DTC.OffersBatch()).SerializeToString())} for batch in OFFER_STATUS]


@pytest.fixture(scope='module')
def offer_status_batched_table_path():
    return '//home/offer_status_batched' + str(uuid.uuid4()) + '/20210101_0001'


@pytest.fixture(scope='module')
def partners_table_data():
    return [
        {
            'shop_id': SHOP_ID,
            'mbi': '\n\n'.join([
                dict2tskv({
                    'shop_id': SHOP_ID,
                    'business_id': BUSINESS_ID,
                    'blue_status': 'REAL',
                }),
            ]),
            'status': 'publish'
        }
    ]


@pytest.fixture(scope='module')
def scanner(
        yt_server,
        log_broker_stuff,
        scanner_resources,
        color,
):
    with make_scanner(yt_server, log_broker_stuff, color, shopsdat_cacher=True, **scanner_resources) as scanner_env:
        wait_until(lambda: scanner_env.united_offers_processed >= len(OFFER_STATUS), timeout=60)
        yield scanner_env


@pytest.mark.parametrize("expected", [
    (
        PUBLISHED_ID,
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'warehouse_id': WAREHOUSE_ID,
                'offer_id': PUBLISHED_ID
            },
            'status': {
                'publication': {
                    'value': PUBLISHED,
                    'meta': {
                        'source': MARKET_IDX,
                    }
                }
            }
        },
        None
    ),
    (
        NOT_PUBLISHED_ID,
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'warehouse_id': WAREHOUSE_ID,
                'offer_id': NOT_PUBLISHED_ID
            },
            'status': {
                'publication': {
                    'value': NOT_PUBLISHED,
                    'meta': {
                        'source': MARKET_IDX,
                    }
                }
            },
        },
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'offer_id': NOT_PUBLISHED_ID
            },
            'resolution': {
                'by_source': [{
                    'meta': {
                        'source': MARKET_IDX_GENERATION,
                    },
                    'verdict': [{
                        'results': [
                            {
                                'messages': [{
                                    'code': "490",
                                    'details': "{\"price_limit\":\"1727.4\",\"price\":\"1892\",\"market-sku\":682008601,\"offerId\":\"not_published_offer_with_verdict\",\"code\":\"490\"}",
                                    "level": ERROR,
                                    "namespace": "1",
                                    "text": "The price is too high"
                                }],
                            }]
                    }]
                }]
            }
        }
    ),
])
def test_offer_status_batched(scanner, expected):
    """
    Проверяем, что пришедший вердикт с MARKET_IDX_GENERATION модифицирует только actual_service_table
    """
    offer_id, data, resolution = expected

    assert_that(scanner.actual_service_offers_table.data, HasOffers([message_from_data(data, DTC.Offer())]))
    if resolution:
        assert_that(scanner.actual_service_offers_table.data, HasOffers([message_from_data(resolution, DTC.Offer())]))
        # в service_offers_table её нет
        assert_that(scanner.service_offers_table.data, not_(HasOffers([message_from_data(resolution, DTC.Offer())])))
