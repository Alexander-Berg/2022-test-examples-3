# coding: utf-8

from hamcrest import assert_that
import pytest
from datetime import datetime

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.pylibrary.proto_utils import message_from_data

NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = NOW_UTC.strftime(time_pattern)

BUSINESS_ID_1P = 10447296
SHOP_ID_1P = 10264169

EXPECTED_OFFERS = [
    {
        'identifiers': {
            'business_id': BUSINESS_ID_1P,
            'shop_id': SHOP_ID_1P,
            'feed_id': 123,
            'offer_id': 'T1000',
        },
    }
]

MBOC_OFFERS_1P_MESSAGE = {
    'offers': [{
        'offer': [
            {
                'identifiers': {
                    'business_id': BUSINESS_ID_1P,
                    'shop_id': SHOP_ID_1P,
                    'offer_id': 'T1000',
                },
                'content': {
                    'binding': {
                        'approved': {
                            'market_sku_id': 7812201,
                            'meta': {
                                'timestamp': current_time,
                                'source': DTC.MARKET_MBO,
                            },
                        },
                    }
                },
                'meta': {
                    'rgb': DTC.BLUE,
                },
                'status': {
                    'united_catalog': {
                        'flag': True,
                        'meta': {
                            'timestamp': current_time,
                            'source': DTC.MARKET_MBO,
                        },
                    }
                },
            }
        ]
    }]
}


@pytest.fixture(scope='module')
def lbk_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def mboc_offer_1p_new_and_updates(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, lbk_topic, mboc_offer_1p_new_and_updates):
    cfg = {
        'general': {
            'color': 'white',
            'batch_size': 10,
        },
        'logbroker': {
            'offers_topic': lbk_topic.topic,
            'mboc_offer_1p_new_and_updates_topic': mboc_offer_1p_new_and_updates.topic,
        }
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, lbk_topic, mboc_offer_1p_new_and_updates, config):
    resources = {
        'config': config,
        'offers_topic': lbk_topic,
        'mboc_offer_1p_new_and_updates_topic': mboc_offer_1p_new_and_updates,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.fixture(scope='module')
def inserter(piper, lbk_topic, mboc_offer_1p_new_and_updates):
    mboc_offer_1p_new_and_updates.write(message_from_data(MBOC_OFFERS_1P_MESSAGE, DatacampMessage()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= len(MBOC_OFFERS_1P_MESSAGE), timeout=60)


@pytest.fixture()
def expected_basic_offers():
    expected_offers = []
    for offer in EXPECTED_OFFERS:
        expected = message_from_data({
            'identifiers': {
                'business_id': offer['identifiers']['business_id'],
                'offer_id': offer['identifiers']['offer_id'],
            },
            'meta': {
                'scope': DTC.BASIC,
                'rgb': DTC.UNKNOWN_COLOR
            },
            'content': {
                'binding': {
                    'approved': {
                        'market_sku_id': 7812201,
                        'meta': {
                            'timestamp': current_time
                        },
                    },
                }
            },
        }, DTC.Offer())
        expected_offers.append(expected)
    return expected_offers


@pytest.fixture()
def expected_service_offers():
    expected_offers = []
    for offer in EXPECTED_OFFERS:
        expected = message_from_data({
            'identifiers': {
                'business_id': offer['identifiers']['business_id'],
                'offer_id': offer['identifiers']['offer_id'],
                'shop_id': offer['identifiers']['shop_id'],
            },
            'meta': {
                'scope': DTC.SERVICE,
                'rgb': DTC.BLUE,
            },
            'status': {
                'united_catalog': {
                    'flag': True,
                    'meta': {
                        'timestamp': current_time
                    },
                }
            },
        }, DTC.Offer())
        expected_offers.append(expected)
    return expected_offers


def test_existence_of_content_system_status(piper, inserter, expected_basic_offers, expected_service_offers):
    assert len(piper.basic_offers_table.data) == 1
    assert_that(piper.basic_offers_table.data, HasOffers(expected_basic_offers))

    assert len(piper.service_offers_table.data) == 1
    assert_that(piper.service_offers_table.data, HasOffers(expected_service_offers))
