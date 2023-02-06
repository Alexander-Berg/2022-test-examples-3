# coding: utf-8

import pytest
import uuid
import datetime

from hamcrest import assert_that, equal_to, is_not
from market.idx.datacamp.controllers.scanner.yatf.test_env import make_scanner
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.api.ExportMessage_pb2 as EM
import market.idx.datacamp.proto.external.Offer_pb2 as ExternalOffer
from market.idx.datacamp.proto.common.Consumer_pb2 import Platform
from market.idx.datacamp.proto.offer.OfferMeta_pb2 import MarketColor
from google.protobuf.timestamp_pb2 import Timestamp
from market.pylibrary.proto_utils import message_from_data
from market.idx.pylibrary.datacamp.utils import wait_until

NOW = datetime.datetime.now()
YEAR_2077 = datetime.datetime(2077, 1, 1)
YEAR_1984 = datetime.datetime(1984, 1, 1)
BUSINESS_ID = 1000
SHOP_ID = 1
FEED_ID = 123
time_pattern = "%Y-%m-%dT%H:%M:%SZ"


def get_ts_seconds(ts):
    return int((ts - datetime.datetime(1970, 1, 1)).total_seconds())


def get_offers():
    return []


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return get_offers()


@pytest.fixture(scope='module')
def service_offers_table_data():
    return get_offers()


@pytest.fixture(scope='module')
def sortdc_updates_table_path():
    return '//home/sortdc_updates' + str(uuid.uuid4())


@pytest.fixture(scope='module')
def sortdc_updates_table_data():
    return [
        {
            'BusinessId': BUSINESS_ID,
            'OfferId': '111',
            'OfferYabsId': 0,
            'ShopId': SHOP_ID,
            'WarehouseId': 0,
            'UrlHashFirst': 0,
            'UrlHashSecond': 0,
            'Rank': 0.5,
            'Data': EM.ExportMessage(
                offer=ExternalOffer.Offer(
                    timestamp=Timestamp(
                        seconds=get_ts_seconds(NOW)
                    ),
                    service=ExternalOffer.Service(
                        platform=Platform.VERTICAL_GOODS,
                        product=[ExternalOffer.VERTICAL_GOODS_ADS],
                        vertical_approved=True
                    ),
                    business_id=BUSINESS_ID,
                    offer_id='111',
                    offer_yabs_id=0,
                    shop_id=SHOP_ID
                )
            ).SerializeToString()
        },
        {
            'BusinessId': BUSINESS_ID,
            'OfferId': '111',
            'OfferYabsId': 0,
            'ShopId': SHOP_ID,
            'WarehouseId': 0,
            'UrlHashFirst': 0,
            'UrlHashSecond': 0,
            'Rank': 0.5,
            'Data': EM.ExportMessage(
                offer=ExternalOffer.Offer(
                    timestamp=Timestamp(
                        seconds=get_ts_seconds(NOW)
                    ),
                    service=ExternalOffer.Service(
                        platform=Platform.DIRECT,
                        product=[ExternalOffer.DIRECT_GOODS_ADS],
                        vertical_approved=True
                    ),
                    business_id=BUSINESS_ID,
                    offer_id='112',
                    offer_yabs_id=0,
                    shop_id=SHOP_ID
                )
            ).SerializeToString()
        },
    ]


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


def test_sortdc_updates(scanner):
    assert_that(scanner.united_offers_processed, equal_to(2))
    assert_that(
        scanner.service_offers_table.data,
        HasOffers([
            message_from_data(
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
                        },
                        'platforms': {
                            MarketColor.VERTICAL_GOODS_ADS: True
                        },
                        'rgb': MarketColor.VERTICAL_GOODS_ADS
                    }
                },
                DTC.Offer()
            )
        ])
    )
    # sort-dc может создавать только чисто-тв офферы
    assert_that(
        scanner.service_offers_table.data,
        is_not(HasOffers([
            message_from_data(
                {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'shop_id': SHOP_ID,
                        'offer_id': '112',
                    },
                    'meta': {
                        'vertical_approved_flag': {
                            'meta': {
                                'timestamp': NOW.strftime(time_pattern),
                            },
                            'flag': True
                        },
                        'rgb': MarketColor.DIRECT_GOODS_ADS
                    }
                },
                DTC.Offer()
            )
        ]))
    )
