# coding: utf-8

import pytest

from hamcrest import assert_that, equal_to
from market.idx.datacamp.controllers.scanner.yatf.test_env import make_scanner
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row, offer_to_basic_row
from market.idx.datacamp.yatf.utils import create_meta, create_update_meta
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.resources.yt_table_resource import YtTableResource
import market.idx.datacamp.proto.offer.UnitedOffer_pb2 as DTC
from market.pylibrary.proto_utils import message_from_data

BUSINESS_ID = 1000
OFFER_ID = 'offer_id'
FIRST_SHOP_ID = 1
SECOND_SHOP_ID = 2


@pytest.fixture(scope='module')
def united_offers_table(yt_server, color):
    return YtTableResource(
        yt_server,
        '//home/united_offers',
        attributes=dict(
            dynamic=False,
            external=False,
            schema=[
                dict(name='united_offer', type='string')
            ]
        ),
        data=[{
            'united_offer': DTC.UnitedOffer(
                basic=DTC.Offer(
                    identifiers=DTC.OfferIdentifiers(
                        business_id=BUSINESS_ID,
                        offer_id=OFFER_ID,
                    ),
                    meta=create_meta(20)
                ),
                service={
                    FIRST_SHOP_ID: DTC.Offer(
                        identifiers=DTC.OfferIdentifiers(
                            business_id=BUSINESS_ID,
                            offer_id=OFFER_ID,
                            shop_id=FIRST_SHOP_ID
                        ),
                        status=DTC.OfferStatus(
                            original_cpa=DTC.Flag(
                                flag=True,
                                meta=create_update_meta(20),
                            ),
                        ),
                        meta=create_meta(20, DTC.BLUE if color == 'blue' else DTC.WHITE),
                    ),
                    SECOND_SHOP_ID: DTC.Offer(
                        identifiers=DTC.OfferIdentifiers(
                            business_id=BUSINESS_ID,
                            offer_id=OFFER_ID,
                            shop_id=SECOND_SHOP_ID
                        ),
                        meta=create_meta(20, DTC.BLUE if color == 'blue' else DTC.WHITE),
                    ),
                }
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
                    shop_id=shop_id,
                    offer_id=OFFER_ID,
                ),
                meta=create_meta(10, DTC.BLUE if color == 'blue' else DTC.WHITE),
            )
        )
        for shop_id in [FIRST_SHOP_ID, SECOND_SHOP_ID]
    ]


@pytest.fixture(scope='module')
def scanner(
    log_broker_stuff,
    yt_server,
    scanner_resources,
    united_offers_table,
    color,
):
    with make_scanner(
        yt_server,
        log_broker_stuff,
        color,
        shopsdat_cacher=True,
        test_configs=['test_united_offers_unpacker.cfg', 'test_united_offers_unpacker_links.cfg'],
        united_offers_table=united_offers_table,
        **scanner_resources
    ) as scanner_env:
        wait_until(lambda: scanner_env.united_offers_processed > 0, timeout=60)
        yield scanner_env


def test_united_offers_processing(scanner):
    assert_that(len(scanner.service_offers_table.data), equal_to(2))

    expected_service_offers = [
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': OFFER_ID,
                'shop_id': FIRST_SHOP_ID,
            },
            'status': {
                'original_cpa': {
                    'flag': True
                }
            }
        },
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': OFFER_ID,
                'shop_id': SECOND_SHOP_ID,
            },
        },
    ]
    expected_service_offers = [message_from_data(row, DTC.Offer()) for row in expected_service_offers]
    assert_that(scanner.service_offers_table.data, HasOffers(expected_service_offers))
