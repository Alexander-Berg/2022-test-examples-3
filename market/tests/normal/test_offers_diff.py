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
                meta=create_meta(20)
            )
        )
        for business_id, shop_id, offer_id in [
            (BUSINESS_ID, SHOP_ID, 'peresorted'),
            (BUSINESS_ID, SHOP_ID, 'disabled_by_gutgin'),
        ]
    ]


@pytest.fixture(scope='module')
def offers_diff_table_data():
    return [{
        'business_id': BUSINESS_ID,
        'offer_id': 'peresorted',
        'offer': DTC.UnitedOffer(
            basic=DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID,
                    offer_id='peresorted',
                ),
                meta=create_meta(20)
            ),
            service={
                SHOP_ID: DTC.Offer(
                    identifiers=DTC.OfferIdentifiers(
                        business_id=BUSINESS_ID,
                        offer_id='peresorted',
                        shop_id=SHOP_ID
                    ),
                    meta=create_meta(10, scope=DTC.SERVICE),
                    status=DTC.OfferStatus(
                        disabled=[
                            DTC.Flag(
                                flag=True,
                                meta=create_update_meta(10, DTC.MARKET_IDX_PERESORT)
                            ),
                        ]
                    ),
                    resolution=DTC.Resolution(
                        by_source=[
                            DTC.Verdicts(
                                meta=create_update_meta(10, DTC.MARKET_IDX_PERESORT),
                                verdict=[DTC.Verdict(results=[DTC.ValidationResult(
                                    is_banned=True,
                                    messages=[DTC.Explanation(code='49a')]
                                )])]
                            )
                        ]
                    )
                )
            }
        ).SerializeToString()
    }, {
        'business_id': BUSINESS_ID,
        'offer_id': 'disabled_by_gutgin',
        'offer': DTC.UnitedOffer(
            basic=DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID,
                    offer_id='disabled_by_gutgin',
                ),
                meta=create_meta(20)
            ),
            service={
                SHOP_ID: DTC.Offer(
                    identifiers=DTC.OfferIdentifiers(
                        business_id=BUSINESS_ID,
                        offer_id='disabled_by_gutgin',
                        shop_id=SHOP_ID
                    ),
                    meta=create_meta(10, scope=DTC.SERVICE),
                    status=DTC.OfferStatus(
                        disabled=[
                            DTC.Flag(
                                flag=True,
                                meta=create_update_meta(10, DTC.MARKET_GUTGIN)
                            ),
                        ]
                    ),
                )
            }
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
        wait_until(lambda: scanner_env.united_offers_processed == 2, timeout=60)
        yield scanner_env


@pytest.mark.parametrize("expected", [{
    'identifiers': {
        'business_id': BUSINESS_ID,
        'shop_id': SHOP_ID,
        'offer_id': 'peresorted',
    },
    'resolution': {
        'by_source': [{
            'meta': {
                'source': DTC.MARKET_IDX_PERESORT,
            },
            'verdict': [{
                'results': [{
                    'is_banned': True,
                    'messages': [{
                        'code': '49a'
                    }]
                }]
            }]
        }]
    },
}, {
    'identifiers': {
        'business_id': BUSINESS_ID,
        'shop_id': SHOP_ID,
        'offer_id': 'disabled_by_gutgin',
    },
    'status': {
        'disabled': [
            {
                'flag': True,
                'meta': {
                    'source': DTC.MARKET_GUTGIN
                }
            }
        ],
    },
}])
def test_peresort(scanner, expected):
    """ Проверяем:
        - применение вердиктов и скрытий по пересорту к сервисной части
        - применение скрытий от GUTGIN к сервисной части
    """
    assert_that(scanner.service_offers_table.data, HasOffers([message_from_data(expected, DTC.Offer())]))
    assert_that(scanner.united_offers_processed, equal_to(2))
