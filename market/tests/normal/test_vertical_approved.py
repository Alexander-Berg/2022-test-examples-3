# coding: utf-8

import pytest
import uuid
import datetime

from hamcrest import assert_that, equal_to
from market.idx.datacamp.controllers.scanner.yatf.test_env import make_scanner
from market.idx.datacamp.yatf.utils import (
    create_meta, create_flag, dict2tskv
)
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.pylibrary.proto_utils import message_from_data
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row

NOW = datetime.datetime.now()
YEAR_2077 = datetime.datetime(2077, 1, 1)
YEAR_1984 = datetime.datetime(1984, 1, 1)
BUSINESS_ID = 1000
SHOP_ID = 1
FEED_ID = 123
time_pattern = "%Y-%m-%dT%H:%M:%SZ"


@pytest.fixture(scope='module')
def partners_table_data():
    return [
        {
            'shop_id': SHOP_ID,
            'mbi': '\n\n'.join([
                dict2tskv({
                    'shop_id': SHOP_ID,
                    'business_id': BUSINESS_ID,
                }),
            ]),
            'status': 'publish'
        }
    ]


def get_offers():
    return [
        offer_to_service_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID,
                    shop_id=SHOP_ID,
                    offer_id=offer_id,
                    warehouse_id=0,
                    feed_id=FEED_ID
                ),
                meta=create_meta(10, DTC.BLUE, vertical_approved_flag=create_flag(flag=va_flag, ts=get_ts_seconds(va_flag_ts)) if va_flag is not None else None),
            )
        )
        for offer_id, va_flag, va_flag_ts in [
            ('111', False, YEAR_1984),
            ('222', False, YEAR_2077),
            ('333', True, YEAR_1984),
            ('444', None, None),
        ]
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return get_offers()


@pytest.fixture(scope='module')
def service_offers_table_data():
    return get_offers()


@pytest.fixture(scope='module')
def vertical_approved_table_path():
    return '//home/vertical_approved' + str(uuid.uuid4()) + '/{}'.format(NOW.strftime(time_pattern))


@pytest.fixture(scope='module')
def vertical_approved_table_data():
    return [{
        'business_id': BUSINESS_ID,
        'shop_id': SHOP_ID,
        'offer_id': '111',
        'timestamp': get_ts_seconds(NOW) * 1000,
        'vertical_approved': True
    }, {
        'business_id': BUSINESS_ID,
        'shop_id': SHOP_ID,
        'offer_id': '222',
        'timestamp': get_ts_seconds(YEAR_1984) * 1000,
        'vertical_approved': True
    }, {
        'business_id': BUSINESS_ID,
        'shop_id': SHOP_ID,
        'offer_id': '333',
        'timestamp': get_ts_seconds(NOW) * 1000,
        'vertical_approved': False
    }, {
        'business_id': BUSINESS_ID,
        'shop_id': SHOP_ID,
        'offer_id': '444',
        'timestamp': get_ts_seconds(NOW) * 1000,
        'vertical_approved': True
    }]


@pytest.fixture(scope='module')
def scanner(
    log_broker_stuff,
    yt_server,
    scanner_resources,
    color,
):
    with make_scanner(yt_server, log_broker_stuff, color, shopsdat_cacher=True, **scanner_resources) as scanner_env:
        wait_until(lambda: scanner_env.united_offers_processed == 4, timeout=60)
        yield scanner_env


def get_ts_seconds(ts):
    return int((ts - datetime.datetime(1970, 1, 1)).total_seconds())


@pytest.mark.parametrize("expected", [
    (
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'offer_id': '111',
            },
            'meta': {
                'vertical_approved_flag': {
                    'meta': {
                        'timestamp': NOW.strftime(time_pattern),
                    },
                    'flag': True
                }
            }
        }
    ),
    (
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'offer_id': '222',
            },
            'meta': {
                'vertical_approved_flag': {
                    'meta': {
                        'timestamp': YEAR_2077.strftime(time_pattern),
                    },
                    'flag': False
                }
            }
        }
    ),
    (
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'offer_id': '333',
            },
            'meta': {
                'vertical_approved_flag': {
                    'meta': {
                        'timestamp': NOW.strftime(time_pattern),
                    },
                    'flag': False
                }
            }
        }
    ),
    (
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'offer_id': '444',
            },
            'meta': {
                'vertical_approved_flag': {
                    'meta': {
                        'timestamp': NOW.strftime(time_pattern),
                    },
                    'flag': True
                }
            }
        }
    )
])
def test_vertical_approved(scanner, expected):
    assert_that(scanner.service_offers_table.data, HasOffers([message_from_data(expected, DTC.Offer())]))
    assert_that(scanner.united_offers_processed, equal_to(4))
