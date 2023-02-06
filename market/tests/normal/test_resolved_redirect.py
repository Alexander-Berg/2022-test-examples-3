# coding: utf-8

import pytest
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC

from hamcrest import all_of, assert_that, has_length
from market.idx.datacamp.controllers.scanner.yatf.test_env import make_scanner
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.utils import create_meta, create_update_meta
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.pylibrary.proto_utils import message_from_data


BUSINESS_ID = 1
ONLY_BASIC_OFFER_ID = 'only_basic_offer'
WITH_SERVICE_OFFER_ID = 'with_service_offer'
SHOP_ID = 100


@pytest.fixture(scope='module')
def basic_offers_table_data(color):
    return [
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=ONLY_BASIC_OFFER_ID,
            ),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        name=DTC.StringValue(
                            value="Name",
                            meta=create_update_meta(10)
                        ),
                    )
                )
            ),
            meta=create_meta(10, DTC.BASIC),
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=WITH_SERVICE_OFFER_ID,
            ),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        name=DTC.StringValue(
                            value="Name",
                            meta=create_update_meta(10)
                        ),
                    )
                )
            ),
            meta=create_meta(10, DTC.BASIC),
        ))
    ]


@pytest.fixture(scope='module')
def service_offers_table_data(color):
    return [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=WITH_SERVICE_OFFER_ID,
                shop_id=SHOP_ID,
            ),
            meta=create_meta(10, DTC.SERVICE),
        ))
    ]


@pytest.fixture(scope='module')
def resolved_redirect_table_data():
    meta = create_meta(ts=10, scope=DTC.SERVICE)
    meta.redirect_resolved_flag.meta.timestamp.FromSeconds(10)
    meta.redirect_resolved_flag.flag = True
    return [{
        'offer': DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=ONLY_BASIC_OFFER_ID,
                shop_id=SHOP_ID,
            ),
            meta=meta
        ).SerializeToString()
    }, {
        'offer': DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=WITH_SERVICE_OFFER_ID,
                shop_id=SHOP_ID,
            ),
            meta=meta
        ).SerializeToString()
    }]


@pytest.fixture(scope='module')
def scanner(yt_server, color, log_broker_stuff, scanner_resources):
    with make_scanner(
        yt_server,
        log_broker_stuff,
        color,
        shopsdat_cacher=True,
        **scanner_resources
    ) as scanner_env:
        wait_until(lambda: scanner_env.united_offers_processed == 2, timeout=60)
        yield scanner_env


def test_resolved_redirect_reader(scanner):
    assert_that(scanner.service_offers_table.data, all_of(
        has_length(1),
        HasOffers([
            message_from_data({
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'offer_id': WITH_SERVICE_OFFER_ID,
                    'shop_id': SHOP_ID,
                },
                'meta': {
                    'redirect_resolved_flag': {
                        'flag': True
                    }
                }
            }, DTC.Offer())
        ])
    ))
