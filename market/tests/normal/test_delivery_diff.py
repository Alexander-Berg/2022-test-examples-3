# coding: utf-8

import pytest
import datetime

from hamcrest import assert_that, equal_to
from market.idx.datacamp.controllers.scanner.yatf.test_env import make_scanner
from market.idx.datacamp.yatf.utils import (
    create_meta, dict2tskv, create_update_meta,
)
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
import market.idx.datacamp.proto.offer.UnitedOffer_pb2 as DTC
from market.pylibrary.proto_utils import message_from_data
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row

NOW = datetime.datetime.now()
BUSINESS_ID = 1000
SHOP_ID = 1
WAREHOUSE_ID = 147


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


@pytest.fixture(scope='module')
def service_offers_table_data(color):
    return [
        offer_to_service_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID,
                    shop_id=SHOP_ID,
                    offer_id='no_delivery',
                ),
                meta=create_meta(20)
            )
        )
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data(color):
    return [
        offer_to_service_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID,
                    shop_id=SHOP_ID,
                    offer_id='no_delivery',
                    warehouse_id=WAREHOUSE_ID,
                ),
                meta=create_meta(20)
            )
        )
    ]


@pytest.fixture(scope='module')
def delivery_diff_table_data():
    return [{
        'business_id': BUSINESS_ID,
        'offer_id': 'no_delivery',
        'shop_id': SHOP_ID,
        'warehouse_id': WAREHOUSE_ID,
        'offer': DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id='no_delivery',
                shop_id=SHOP_ID,
                warehouse_id=WAREHOUSE_ID
            ),
            resolution=DTC.Resolution(
                by_source=[
                    DTC.Verdicts(
                        meta=create_update_meta(10, DTC.MARKET_NORDSTREAM),
                        verdict=[DTC.Verdict(results=[DTC.ValidationResult(
                            messages=[DTC.Explanation(code='39A')]
                        )])]
                    )
                ]
            )
        ).SerializeToString()
    }]


@pytest.fixture(scope='module')
def scanner(
    log_broker_stuff,
    yt_server,
    scanner_resources,
    color,
):
    with make_scanner(yt_server, log_broker_stuff, color, shopsdat_cacher=True, **scanner_resources) as scanner_env:
        wait_until(lambda: scanner_env.united_offers_processed == 1, timeout=60)
        yield scanner_env


@pytest.mark.parametrize("expected", [{
    'identifiers': {
        'business_id': BUSINESS_ID,
        'shop_id': SHOP_ID,
        'offer_id': 'no_delivery',
        'warehouse_id': WAREHOUSE_ID
    },
    'resolution': {
        'by_source': [{
            'meta': {
                'source': DTC.MARKET_NORDSTREAM,
            },
            'verdict': [{
                'results': [{
                    'messages': [{
                        'code': '39A'
                    }]
                }]
            }]
        }]
    },
}])
def test_applied_nodelivery_verdict(scanner, expected):
    """ ??????????????????:
        - ???????????????????? ???????????????? ?? ???????????????????? ?????????????????? ??????????
    """
    assert_that(scanner.actual_service_offers_table.data, HasOffers([message_from_data(expected, DTC.Offer())]))
    assert_that(scanner.united_offers_processed, equal_to(1))
