# coding: utf-8

import pytest

from hamcrest import assert_that, equal_to
from market.idx.datacamp.controllers.scanner.yatf.test_env import make_scanner
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row, offer_to_basic_row
from market.idx.datacamp.yatf.utils import create_meta, create_update_meta
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.resources.yt_table_resource import YtTableResource
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.offer.OfferMarketSpecificContent_pb2 import MarketSpecificContent, ShopModelRating
from market.pylibrary.proto_utils import message_from_data

BUSINESS_ID = 1000
OFFER_ID = 'offer_id'
SHOP_ID = 1


@pytest.fixture(scope='module')
def parameters_globalization_table(yt_server, color):
    return YtTableResource(
        yt_server,
        '//home/mbo_params_globalization',
        attributes=dict(
            dynamic=False,
            external=False,
            schema=[
                dict(name='business_id', type='int64'),
                dict(name='shop_sku', type='string'),
                dict(name='market_specific_content', type='string'),
            ]
        ),
        data=[{
            'business_id': BUSINESS_ID,
            'shop_sku': OFFER_ID,
            'market_specific_content': MarketSpecificContent(
                rating=ShopModelRating(
                    current_rating=10,
                    expected_rating=20,
                    meta=create_update_meta(20),
                )
            ).SerializeToString()
        }]
    )


@pytest.fixture(scope='module')
def basic_offers_table_data(color):
    return [
        offer_to_basic_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID,
                    offer_id=OFFER_ID,
                ),
                meta=create_meta(10)
            )
        )
    ]


@pytest.fixture(scope='module')
def service_offers_table_data(color):
    return [
        offer_to_service_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID,
                    shop_id=SHOP_ID,
                    offer_id=OFFER_ID,
                ),
                meta=create_meta(10, DTC.BLUE if color == 'blue' else DTC.WHITE),
            )
        )
    ]


@pytest.fixture(scope='module')
def scanner(
    log_broker_stuff,
    yt_server,
    scanner_resources,
    parameters_globalization_table,
    color,
):
    with make_scanner(
        yt_server,
        log_broker_stuff,
        color,
        shopsdat_cacher=True,
        parameters_globalization_table=parameters_globalization_table,
        **scanner_resources
    ) as scanner_env:
        wait_until(lambda: scanner_env.united_offers_processed > 0, timeout=60)
        yield scanner_env


def test_mbo_params_globalization_processing(scanner):
    assert_that(len(scanner.basic_offers_table.data), equal_to(1))

    expected_basic_offers = [
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': OFFER_ID,
            },
            'content': {
                'partner': {
                    'market_specific_content': {
                        'rating': {
                            'current_rating': 10,
                            'expected_rating': 20,
                        }
                    }
                }
            }
        },
    ]
    expected_basic_offers = [message_from_data(row, DTC.Offer()) for row in expected_basic_offers]
    assert_that(scanner.basic_offers_table.data, HasOffers(expected_basic_offers))
