# coding: utf-8

import pytest
import uuid

from datetime import datetime
from hamcrest import assert_that
from market.idx.datacamp.controllers.scanner.yatf.test_env import make_scanner
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import MARKET_SCM
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row, offer_to_basic_row
from market.idx.datacamp.yatf.utils import dict2tskv, create_meta
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.pylibrary.proto_utils import message_from_data
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC


SHOP_ID = 1
BUSINESS_ID = 1000
NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"


MBO_DATA = [
    # offer hidden by MBO
    {
        'identifiers': {
            'shop_id': SHOP_ID,
            'offer_id': 'mbo_hidden',
            'warehouse_id': 5,
            'feed_id': SHOP_ID * 100,
        },
        'status': {
            'disabled': [
                {
                    'flag': True,
                    'meta': {
                        'source': MARKET_SCM,
                        'timestamp': NOW_UTC.strftime(time_pattern),
                    },
                },
            ],
        },
        'resolution': {
            'by_source': [{
                'meta': {
                    'source': MARKET_SCM,
                    'timestamp': NOW_UTC.strftime(time_pattern),
                },
                'verdict': [{
                    'results': [{
                        'messages': [{
                            'code': 'some_code',
                        }]
                    }]
                }]
            }]
        }
    },
    # offer not hidden by MBO
    {
        'identifiers': {
            'shop_id': SHOP_ID,
            'offer_id': 'mbo_not_hidden',
            'warehouse_id': 5,
            'feed_id': SHOP_ID * 100,
        },
        'status': {
            'disabled': [
                {
                    'flag': False,
                    'meta': {
                        'source': MARKET_SCM,
                        'timestamp': NOW_UTC.strftime(time_pattern),
                    },
                },
            ],
        }
    }
]


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
def basic_offers_table_data():
    return [
        offer_to_basic_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID,
                    offer_id=offer_id,
                ),
                meta=create_meta(10)
            )
        )
        for offer_id in ['mbo_hidden', 'mbo_not_hidden']
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        offer_to_service_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID,
                    shop_id=SHOP_ID,
                    offer_id=offer_id,
                ),
                meta=create_meta(10, DTC.BLUE)
            )
        )
        for offer_id in ['mbo_hidden', 'mbo_not_hidden']
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
        offer_to_service_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID,
                    shop_id=SHOP_ID,
                    warehouse_id=5,
                    offer_id=offer_id,
                ),
                meta=create_meta(10, DTC.BLUE)
            )
        )
        for offer_id in ['mbo_hidden', 'mbo_not_hidden']
    ]


@pytest.fixture(scope='module')
def mbo_hidings_table_data():
    return [{'data': message_from_data(offer, DTC.Offer()).SerializeToString()} for offer in MBO_DATA]


@pytest.fixture(scope='module')
def mbo_hidings_table_path():
    return '//home/mbo_hiding' + str(uuid.uuid4()) + '/20200101_0101'


@pytest.fixture(scope='module')
def scanner(
    yt_server,
    log_broker_stuff,
    scanner_resources,
    color,
):
    with make_scanner(yt_server, log_broker_stuff, color, shopsdat_cacher=True, **scanner_resources) as scanner_env:
        wait_until(lambda: scanner_env.united_offers_processed == len(MBO_DATA), timeout=60)
        yield scanner_env


@pytest.mark.parametrize("expected", [
    (
        'mbo_hidden',
        {
            'identifiers': {
                'offer_id': 'mbo_hidden',
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'warehouse_id': 5
            },
            'status': {
                'disabled': [{
                    'flag': True,
                    'meta': {
                        'source': DTC.MARKET_SCM,
                    }
                }],
                'publish': DTC.HIDDEN,
                'publish_by_partner': None,
            },
            'resolution': {
                'by_source': [{
                    'meta': {
                        'source': MARKET_SCM,
                    },
                    'verdict': [{
                        'results': [{
                            'messages': [{
                                'code': 'some_code',
                            }]
                        }]
                    }]
                }]
            }
        }
    ),
    (
        'mbo_not_hidden',
        {
            'identifiers': {
                'offer_id': 'mbo_not_hidden',
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'warehouse_id': 5
            },
            'status': {
                'disabled': [{
                    'flag': False,
                    'meta': {
                        'source': DTC.MARKET_SCM,
                    }
                }],
                'publish_by_partner': None,
            }
        }
    ),
])
def test_mbo_hiding(scanner, expected):
    """
    Проверяем, что scanner только модифицирует таблицы
    """
    shop_sku, data = expected
    assert_that(scanner.actual_service_offers_table.data, HasOffers([message_from_data(data, DTC.Offer())]))
    assert_that(scanner.service_offers_table.data, HasOffers([message_from_data({
        'identifiers': {
            'business_id': BUSINESS_ID,
            'shop_id': SHOP_ID,
            'offer_id': shop_sku,
        },
    }, DTC.Offer())]))
    assert_that(scanner.basic_offers_table.data, HasOffers([message_from_data({
        'identifiers': {
            'business_id': BUSINESS_ID,
            'offer_id': shop_sku,
        },
    }, DTC.Offer())]))
