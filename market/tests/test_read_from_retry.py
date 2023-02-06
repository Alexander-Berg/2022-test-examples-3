# coding: utf-8

import pytest
from datetime import datetime, timedelta
from hamcrest import assert_that, has_items

from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.utils import create_meta, create_price, dict2tskv
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampServiceOffersTable,
    DataCampPartnersTable
)
from market.pylibrary.proto_utils import message_from_data
import market.idx.datacamp.proto.subscription.subscription_pb2 as SUBS
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC

OFFER_ID = 'offer_id'
BUSINESS_ID = 1
FEED_ID = 11
SHOP_ID = 101

FUTURE_UTC = datetime.utcnow() + timedelta(minutes=45)
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
future_time = FUTURE_UTC.strftime(time_pattern)
ORIGINAL_SOURCE = 'SomeOriginalUnpacker'


UNITED_OFFERS = [
    {
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {'business_id': BUSINESS_ID, 'offer_id': OFFER_ID, 'feed_id': FEED_ID},
                            'content': {
                                'partner': {
                                    'original': {
                                        'weight': {'value_mg': 1000000},
                                        'dimensions': {'width_mkm': 20000, 'length_mkm': 10000, 'height_mkm': 30000},
                                    }
                                },
                            },
                        },
                        'service': {
                            SHOP_ID: {
                                'identifiers': {
                                    'business_id': BUSINESS_ID,
                                    'offer_id': OFFER_ID,
                                    'shop_id': SHOP_ID,
                                    'feed_id': FEED_ID,
                                },
                                'price': {
                                    'basic': {
                                        'binary_price': {
                                            'price': 1100000000, 'rate': 'CBRF', 'id': 'USD'
                                        },
                                        'meta': {
                                            'timestamp': future_time
                                        }
                                    }
                                },
                            }
                        },
                    }
                ]
            }
        ],
        'tech_info': {
            'source': ORIGINAL_SOURCE
        }
    }
]

PARTNERS=[
    {
        'shop_id': SHOP_ID,
        'status': 'publish',
        'mbi': dict2tskv({
            'shop_id': SHOP_ID,
            'business_id': BUSINESS_ID,
            'datafeed_id': FEED_ID,
            'is_push_partner': True,
        })
    }
]


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(
        yt_server,
        config.yt_basic_offers_tablepath,
        data=[
            offer_to_basic_row(
                DTC.Offer(
                    identifiers=DTC.OfferIdentifiers(
                        business_id=BUSINESS_ID, offer_id=OFFER_ID, shop_id=SHOP_ID, feed_id=FEED_ID
                    ),
                    meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
                    price=create_price(100, 1),
                )
            )
        ],
    )


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(
        yt_server,
        config.yt_service_offers_tablepath,
        data=[
            offer_to_service_row(
                DTC.Offer(
                    identifiers=DTC.OfferIdentifiers(
                        business_id=BUSINESS_ID, offer_id=OFFER_ID, shop_id=SHOP_ID, feed_id=FEED_ID
                    ),
                    meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
                    price=create_price(100, 200),
                )
            )
        ],
    )


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(
        yt_server,
        config.yt_actual_service_offers_tablepath,
        data=[
            offer_to_service_row(
                DTC.Offer(
                    identifiers=DTC.OfferIdentifiers(
                        business_id=BUSINESS_ID, offer_id=OFFER_ID, shop_id=SHOP_ID, feed_id=FEED_ID
                    ),
                    meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
                    price=create_price(100, 200),
                )
            )
        ],
    )


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    return DataCampPartnersTable(yt_server, config.yt_partners_tablepath, PARTNERS)


@pytest.fixture(scope='module')
def retry_input_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def subscription_service_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'subscription-service')
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, retry_input_topic, subscription_service_topic):
    cfg = {
        'logbroker': {
            'retry_input_topic': retry_input_topic.topic,
            'subscription_service_topic': subscription_service_topic.topic,
        },
        'general': {
            'color': 'blue',
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    retry_input_topic,
    partners_table,
    subscription_service_topic
):
    resources = {
        'config': config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'retry_input_topic': retry_input_topic,
        'partners_table': partners_table,
        'subscription_service_topic': subscription_service_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_retry_pipeline_update(retry_input_topic, subscription_service_topic, piper):
    united_offers = 0
    for message in UNITED_OFFERS:
        for offers in message['united_offers']:
            united_offers += len(offers)
        retry_input_topic.write(message_from_data(message, DatacampMessage()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= united_offers)

    assert_that(
        piper.basic_offers_table.data,
        HasOffers(
            [message_from_data({'identifiers': {'business_id': BUSINESS_ID, 'offer_id': OFFER_ID}}, DTC.Offer())]
        ),
    )

    assert_that(
        piper.service_offers_table.data,
        HasOffers(
            [
                message_from_data(
                    {
                        'identifiers': {'business_id': BUSINESS_ID, 'offer_id': OFFER_ID, 'shop_id': SHOP_ID},
                        "price": {
                            "basic": {
                                "binary_price": {"price": "1100000000", "id": "USD", "rate": "CBRF"},
                                "meta": {"timestamp": future_time},
                            }
                        },
                    },
                    DTC.Offer(),
                )
            ]
        ),
    )

    data = subscription_service_topic.read(1)
    assert_that(
        data,
        has_items(
            IsSerializedProtobuf(
                SUBS.UnitedOffersSubscriptionMessage,
                {
                    'source': ORIGINAL_SOURCE,
                }
            )
        )
    )
