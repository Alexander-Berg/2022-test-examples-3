# coding: utf-8

from hamcrest import assert_that
import pytest
from datetime import datetime, timedelta

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.pylibrary.proto_utils import message_from_data
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.yatf.utils.utils import create_timestamp_from_json

NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = NOW_UTC.strftime(time_pattern)

FUTURE_UTC = NOW_UTC + timedelta(minutes=45)
future_time = FUTURE_UTC.strftime(time_pattern)
future_ts = create_timestamp_from_json(future_time)


OFFERS = [
    {
        'identifiers': {
            'business_id': 1,
            'shop_id': 10,
            'feed_id': 123,
            'offer_id': 'T1000',
        },
        'status': {
            'disabled': [
                {
                    'flag': False,
                    'meta': {
                        'timestamp': current_time,
                        'source': DTC.PUSH_PARTNER_FEED,
                    },
                },
            ],
        },
        'meta': {
            'ts_created': current_time
        },
    }
]


MBOC_OFFERS_MESSAGE = {
    'offers': [{
        'offer': [
            {
                'identifiers': {
                    'business_id': 1,
                    'shop_id': 10,
                    'offer_id': 'T1000',
                },
                'content': {
                    'status': {
                        'content_system_status': {
                            'meta': {
                                'timestamp': future_time
                            },
                            'cpc_state': DTC.OfferContentCpcState.CPC_CONTENT_NEED_MAPPING
                        }
                    }
                }
            }
        ]
    }]
}


@pytest.fixture(scope='module')
def lbk_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def mboc_offer_state_updates(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, lbk_topic, mboc_offer_state_updates):
    cfg = {
        'general': {
            'color': 'white',
            'batch_size': 10,
        },
        'logbroker': {
            'offers_topic': lbk_topic.topic,
            'mboc_offer_state_updates_topic': mboc_offer_state_updates.topic,
        }
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, lbk_topic, mboc_offer_state_updates, config):
    resources = {
        'config': config,
        'offers_topic': lbk_topic,
        'mboc_offer_state_updates_topic': mboc_offer_state_updates,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.fixture(scope='module')
def inserter(piper, lbk_topic, mboc_offer_state_updates):
    for offer in OFFERS:
        lbk_topic.write(message_from_data(offer, DTC.Offer()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= len(OFFERS))

    mboc_offer_state_updates.write(message_from_data(MBOC_OFFERS_MESSAGE, DatacampMessage()).SerializeToString())


@pytest.fixture()
def expected_basic_offers():
    expected_offers = []
    for offer in OFFERS:
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
                'status': {
                    'content_system_status': {
                        'meta': {
                            'timestamp': future_time
                        },
                        'cpc_state': DTC.OfferContentCpcState.CPC_CONTENT_NEED_MAPPING
                    }
                }
            },
        }, DTC.Offer())
        expected_offers.append(expected)
    return expected_offers


def test_existence_of_content_system_status(piper, inserter, expected_basic_offers):
    assert len(piper.basic_offers_table.data) == 1
    assert_that(piper.basic_offers_table.data, HasOffers(expected_basic_offers))
