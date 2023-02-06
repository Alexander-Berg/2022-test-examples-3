# coding: utf-8

import base64
import pytest
import six
from datetime import datetime, timedelta
from hamcrest import assert_that, equal_to

from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampPartnersTable,
    DataCampServiceOffersTable,
)
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.pylibrary.proto_utils import message_from_data
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.utils.utils import create_timestamp_from_json
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.idx.pylibrary.datacamp.utils import wait_until

NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
PAST_UTC = NOW_UTC - timedelta(minutes=45)
FUTURE_UTC = NOW_UTC + timedelta(minutes=45)
time_pattern = "%Y-%m-%dT%H:%M:%SZ"

current_time = NOW_UTC.strftime(time_pattern)
current_ts = create_timestamp_from_json(current_time)

past_time = PAST_UTC.strftime(time_pattern)
past_ts = create_timestamp_from_json(past_time)

future_time = FUTURE_UTC.strftime(time_pattern)
future_ts = create_timestamp_from_json(future_time)

PARTNERS = [
    {
        'shop_id': 1,
        'status': 'publish',
        'mbi': dict2tskv(
            {
                'shop_id': 1,
                'business_id': 10,
                'datafeed_id': 1234,
                'warehouse_id': 54321,
                'is_push_partner': True,
            }
        ),
    },
]

BASIC_OFFERS = [
    offer_to_basic_row(
        message_from_data({
            'identifiers': {
                'business_id': business_id,
                'offer_id': shop_sku
            },
            'meta': {
                'ts_first_added': past_time
            },
        }, DTC.Offer()))
    for business_id, shop_sku in [
        (10, 'TestAmoreRtyUpdate'),
    ]
]

SERVICE_OFFERS = [
    offer_to_service_row(
        message_from_data(
            {
                'identifiers': {
                    'business_id': business_id,
                    'offer_id': shop_sku,
                    'shop_id': shop_id,
                    'warehouse_id': 0,
                },
                'meta': {
                    'rgb': DTC.BLUE,
                    'scope': DTC.SERVICE,
                },
                'bids': {
                    'amore_beru_vendor_data': {
                        'meta': {
                            'timestamp': past_time,
                        },
                        'amore_pl_gen_create': 1,
                    }
                },
            },
            DTC.Offer(),
        )
    )
    for business_id, shop_id, shop_sku in [
        (10, 1, 'TestAmoreRtyUpdate'),
    ]
]

ACTUAL_SERVICE_OFFERS = [
    offer_to_service_row(
        message_from_data(
            {
                'identifiers': {
                    'business_id': business_id,
                    'offer_id': shop_sku,
                    'shop_id': shop_id,
                    'warehouse_id': warehouse_id,
                    'feed_id': feed_id,
                },
                'meta': {
                    'rgb': DTC.BLUE,
                    'scope': DTC.SERVICE,
                },
            },
            DTC.Offer(),
        )
    )
    for business_id, shop_id, shop_sku, warehouse_id, feed_id in [
        (10, 1, 'TestAmoreRtyUpdate', 54321, 1234),
        (10, 1, 'TestAmoreRtyUpdate', 145, 1235),
        (10, 1, 'TestAmoreRtyUpdate', 147, 1236),
    ]
]


@pytest.fixture(scope='module')
def amore_united_topic_data():
    return {
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': 10,
                                'offer_id': 'TestAmoreRtyUpdate',
                            },
                        },
                        'service': {
                            1: {
                                'identifiers': {'business_id': 10, 'shop_id': 1, 'offer_id': 'TestAmoreRtyUpdate'},
                                'bids': {
                                    'amore_data': {
                                        'meta': {'timestamp': future_time},
                                        'value': six.ensure_str(base64.b64encode(six.ensure_binary('by new'))),
                                    }
                                },
                            }
                        },
                    }
                ]
            }
        ],
    }


@pytest.fixture(scope='module')
def amore_united_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'amore_united_topic')
    return topic


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    return DataCampPartnersTable(yt_server, config.yt_partners_tablepath, PARTNERS)


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=BASIC_OFFERS)


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=SERVICE_OFFERS)


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath, data=ACTUAL_SERVICE_OFFERS)


@pytest.fixture(scope='module')
def rty_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'rty_topic')
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, amore_united_topic, rty_topic):
    cfg = {
        'logbroker': {
            'rty_topic': rty_topic.topic,
            'amore_united_topic': amore_united_topic.topic,
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
    amore_united_topic,
    rty_topic,
    partners_table,
):
    resources = {
        'config': config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'partners_table': partners_table,
        'amore_united_topic': amore_united_topic,
        'rty_topic': rty_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.yield_fixture(scope='module')
def rty_united_topic_data(piper, amore_united_topic, amore_united_topic_data, rty_topic):
    amore_united_topic.write(message_from_data(amore_united_topic_data, DatacampMessage()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= 1)
    result = rty_topic.read(count=3)  # три актуальных части создают три документа rty
    assert_that(rty_topic, HasNoUnreadData())
    return result


def test_amore_united_update(rty_united_topic_data):
    assert_that(len(rty_united_topic_data), equal_to(3))
